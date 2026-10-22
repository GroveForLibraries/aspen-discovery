<?php
/**
 * Recalculates cost savings for all reading history entries from the command line
 * First parameter - server name
 * Second parameter - background process ID (optional)
 * Third parameter - format (optional)
 */

require_once __DIR__ . '/../bootstrap.php';
require_once __DIR__ . '/../bootstrap_aspen.php';

set_time_limit(0);
ini_set('memory_limit', '2G');
/** @var MemoryWatcher $memoryWatcher */
global $memoryWatcher;

require_once ROOT_DIR . '/sys/Administration/BackgroundProcess.php';
$backgroundProcess = null;
if ($argc > 2) {
	$backgroundProcessId = $argv[2];
	$backgroundProcess = new BackgroundProcess();
	$backgroundProcess->id = $backgroundProcessId;
	if (!$backgroundProcess->find(true)) {
		$backgroundProcess = null;
		if (is_null($backgroundProcess)) { echo ("Could not find the specified background process\n"); }
		die();
	}else{
		if (!$backgroundProcess->isRunning) {
			$backgroundProcess->endProcess('Error, attempted to restart previously completed background process');
			die();
		}
	}
}
if ($argc > 3) {
	$format = $argv[3];
}

require_once ROOT_DIR . '/sys/ReplacementCost.php';
$replacementCosts = ReplacementCost::getReplacementCostsByFormat();

require_once ROOT_DIR . '/sys/Utils/GroupingUtils.php';
require_once ROOT_DIR . '/sys/ReadingHistoryEntry.php';
$readingHistoryEntry = new ReadingHistoryEntry();
if (!empty($format)) {
	$readingHistoryEntry->format = $format;
}
//Optimize to load by source so we can initialize fewer record drivers
$readingHistoryEntry->orderBy(['source', 'sourceId']);
$numEntriesToUpdate = $readingHistoryEntry->count();
if (!is_null($backgroundProcess)) { $backgroundProcess->addNote("Updating $numEntriesToUpdate reading history entries"); }
if (is_null($backgroundProcess)) { echo(date('Y M d H:i:s') . " There are $numEntriesToUpdate entries to update " . $memoryWatcher->getCurrentMemoryAllocation()); }

$readingHistoryEntry->find();
$numUpdated = 0;
$numProcessed = 0;
$loggedZeroCostFormats = [];

global $indexingProfiles;

//Recalculate all reading history entries
$lastSource = '';
$lastSourceId = '';
$recordDriver = null;
global $aspen_db;
while ($readingHistoryEntry->fetch()) {
	if ($lastSource != $readingHistoryEntry->source || $lastSourceId != $readingHistoryEntry->sourceId) {
		if (array_key_exists($lastSource, $indexingProfiles)) {
			//Clear cache
			RecordDriverFactory::clearCachedDrivers();
			$recordDriver = RecordDriverFactory::initRecordDriverById($readingHistoryEntry->source . ':' . $readingHistoryEntry->sourceId);
		}else{
			$recordDriver = null;
		}
		$lastSource = $readingHistoryEntry->source;
		$lastSourceId = $readingHistoryEntry->sourceId;
	}
	$lowerFormat = strtolower($readingHistoryEntry->format);
	//Update the costSavings for the reading history entry and update the total cost savings for the user

	$useFormatMapLookup = true;
	//We only have replacement costs at the item level for marc records from the ILS so don't bother creating drivers for anything else
	if (array_key_exists($readingHistoryEntry->source, $indexingProfiles)) {
		if ($recordDriver != null) {
			$replacementCost = getReplacementCost($recordDriver, $readingHistoryEntry->format, null, $readingHistoryEntry->barcode);
			if ($replacementCost > 0) {
				$aspen_db->exec("UPDATE user_reading_history_work SET costSavings = $replacementCost where id = $readingHistoryEntry->id");
				$numUpdated++;
				$useFormatMapLookup = false;
			}
			$recordDriver = null;
		}
	}
	if ($useFormatMapLookup) {
		$foundCost = false;
		if (array_key_exists($lowerFormat, $replacementCosts)) {
			if ($replacementCosts[$lowerFormat] > 0) {
				//Update the costSavings for the reading history entry and update the total cost savings for the user
				$replacementCost = $replacementCosts[$lowerFormat];
				$aspen_db->exec("UPDATE user_reading_history_work SET costSavings = $replacementCost where id = $readingHistoryEntry->id");
				$numUpdated++;
				$foundCost = true;
			}
		}
		if (!$foundCost && !array_key_exists($lowerFormat, $loggedZeroCostFormats)) {
			if (!is_null($backgroundProcess)) {
				$backgroundProcess->addNote("Skipping $readingHistoryEntry->format because no replacement cost was specified.");
			}
			$loggedZeroCostFormats[$lowerFormat] = $lowerFormat;
		}
	}

	$numProcessed++;
	if ($numProcessed % 1000 === 0) {
		if (is_null($backgroundProcess)) { echo(date('Y M d H:i:s') . " Processed $numProcessed reading history entries "  . $memoryWatcher->getCurrentMemoryAllocation() . "\n"); }
		if (is_null($backgroundProcess)) { echo(date('Y M d H:i:s') . "  - "  . $memoryWatcher->getCurrentMemoryAllocation() . "\n"); }
		ob_flush();
	}
}

//Now get the total cost savings for users that have checked something out in the format
if (is_null($backgroundProcess)) { echo(date('Y M d H:i:s') . " Updating total cost savings for users\n"); }
ob_flush();
$readingHistoryEntry = new ReadingHistoryEntry();
if (!empty($format)) {
	$readingHistoryEntry->format = $format;
}
$readingHistoryEntry->whereAdd("costSavings > 0");
$readingHistoryEntry->selectAdd();
$readingHistoryEntry->selectAdd('userId');
$readingHistoryEntry->selectAdd("sum(costSavings) as costSavings");
$readingHistoryEntry->groupBy('userId');

$numUsersToUpdate = $readingHistoryEntry->count();
if (is_null($backgroundProcess)) { echo(date('Y M d H:i:s') . " Updating total cost savings for $numUsersToUpdate users\n"); }
ob_flush();

$numUsersUpdated = 0;
$readingHistoryEntry->find();
while ($readingHistoryEntry->fetch()) {
	$aspen_db->exec("UPDATE user SET totalCostSavings = $readingHistoryEntry->costSavings where id = $readingHistoryEntry->userId");
	$numUsersUpdated++;
	if ($numUsersUpdated % 1000 == 0) {
		if (is_null($backgroundProcess)) { echo(date('Y M d H:i:s') . " Updated $numUsersUpdated users\n"); }
		ob_flush();
	}
}

if (is_null($backgroundProcess)) { echo(date('Y M d H:i:s') . " Done!"); }
ob_flush();

if (!is_null($backgroundProcess)) {
	$backgroundProcess->addNote(translate([
		'text' => 'Updated %1% historic cost savings.',
		1 => $numUpdated,
		'isAdminFacing' => true
	]));
	$endNote = translate([
		'text' => 'Updated total cost savings for %1% users.',
		1 => $numUsersUpdated,
		'isAdminFacing' => true
	]);
	$backgroundProcess->endProcess($endNote);
}
