<?php
require_once __DIR__ . '/../bootstrap.php';
require_once __DIR__ . '/../bootstrap_aspen.php';

require_once ROOT_DIR . '/sys/Updates/ScheduledUpdate.php';
require_once ROOT_DIR . '/sys/Greenhouse/AspenSite.php';

$pendingUpdates = new ScheduledUpdate();
$pendingUpdates->status = 'pending';
$pendingUpdates->whereAdd('dateScheduled <= ' . time()); //Only get things where the scheduled time is before right now
$pendingUpdates->orderBy('dateScheduled asc');
//Load all of them once since we update them
$updatesToRun = $pendingUpdates->fetchAll('id');

global $configArray;
global $serverName;

if (count($updatesToRun) == 0) {
	console_log("no updates to run\n");
}else {
	foreach ($updatesToRun as $id) {
		//Load the actual item
		$scheduledUpdate = new ScheduledUpdate();
		$scheduledUpdate->id = $id;
		if ($scheduledUpdate->find(true)) {
			$versionToUpdateTo = $scheduledUpdate->updateToVersion;
			$currentVersion = getGitBranch();

			if (str_replace('.', '', $versionToUpdateTo) >= str_replace('.', '', $currentVersion,)) {
				if ($scheduledUpdate->updateType === 'complete') {
					$scheduledUpdate->notes .= "FAILED: Complete updates are not supported yet";
				} elseif ($scheduledUpdate->updateType === 'patch') {
					//assume it works and update to false if there are issues.
					$updateSucceeded = true;
					if (strcasecmp($configArray['System']['operatingSystem'], 'windows') == 0) {
						exec("cd c:\web\aspen-discovery; git fetch origin; git reset --hard origin/$currentVersion 2>&1", $resetGitResult);
					} else {
						if (!exec("cd /usr/local/aspen-discovery; git fetch origin", $resetGitResult)) {
							$updateSucceeded = false;
							$scheduledUpdate->notes .= 'FAILED: fetch origin failed';
						}
						if ($updateSucceeded) {
							if (!exec("cd /usr/local/aspen-discovery; git reset --hard origin/$currentVersion 2>&1", $resetGitResult)) {
								$updateSucceeded = false;
								$scheduledUpdate->notes .= 'FAILED: reset hard failed';
							}
						}
					}

					$scheduledUpdate->notes .= "Resetting git to branch $currentVersion\n";
					foreach ($resetGitResult as $result) {
						$scheduledUpdate->notes .= $result . "\n";
					}

					if (strcasecmp($configArray['System']['operatingSystem'], 'windows') == 0) {
						exec("cd c:\web\aspen-discovery; git pull origin $scheduledUpdate->updateToVersion", $gitResult);
					} else {
						exec("cd /usr/local/aspen-discovery; git pull origin $scheduledUpdate->updateToVersion 2>&1", $gitResult);
					}
					$scheduledUpdate->notes .= "Pulling branch $currentVersion\n";
					foreach ($gitResult as $result) {
						$scheduledUpdate->notes .= $result . "\n";
					}

					// run db maintenance
					$scheduledUpdate->notes .= "Running database maintenance $currentVersion\n";
					require_once ROOT_DIR . '/services/API/SystemAPI.php';
					$systemAPI = new SystemAPI();
					$dbMaintenance = $systemAPI->runPendingDatabaseUpdates();
					if (!isset($dbMaintenance['success']) || $dbMaintenance['success'] == false) {
						$message = $dbMaintenance['message'] ?? '';
						$scheduledUpdate->status = 'failed';
						$scheduledUpdate->notes .= $message;
					}
				} else {
					// invalid updateType
					$scheduledUpdate->notes = "FAILED: Invalid update type\n";
				}
			} else {
				$scheduledUpdate->notes = "FAILED: Must update to a version that is the same or newer than the current version of $currentVersion\n";
			}

			$lowerNotes = strtolower($scheduledUpdate->notes);
			if ((strpos($lowerNotes, 'fatal') !== false) || (strpos($lowerNotes, 'failed') !== false) || (strpos($lowerNotes, 'rejected') !== false)) {
				$scheduledUpdate->status = 'failed';
			} else {
				$scheduledUpdate->status = 'complete';
			}
			$scheduledUpdate->dateRun = time();

			$scheduledUpdate->update();

			if (!empty($scheduledUpdate->greenhouseId)) {
				// update greenhouse if the update was scheduled from there
				require_once ROOT_DIR . '/sys/SystemVariables.php';
				$systemVariables = SystemVariables::getSystemVariables();
				if (!empty($systemVariables)) {
					$greenhouseUrl = $systemVariables->greenhouseUrl . '/Greenhouse/UpdateCenter/';
					require_once ROOT_DIR . '/sys/CurlWrapper.php';
					$curl = new CurlWrapper();
					$body = [
						'runType' => $scheduledUpdate->updateType,
						'dateScheduled' => $scheduledUpdate->dateScheduled,
						'updateToVersion' => $scheduledUpdate->updateToVersion,
						'status' => $scheduledUpdate->status,
						'greenhouseId' => $scheduledUpdate->greenhouseId,
						'notes' => $scheduledUpdate->notes,
						'dateRun' => $scheduledUpdate->dateRun,

					];
					$response = $curl->curlPostPage($greenhouseUrl . '/API/GreenhouseAPI?method=updateScheduledUpdate', $body);

					//TODO: temp debugging
					print_r($response);
				}
			}
		}
	}
	console_log("Finished running " . count($updatesToRun) . " updates\n");
}

function console_log($message, $prefix = '') {
	$STDERR = fopen("php://stderr", "w");
	fwrite($STDERR, $prefix.$message."\n");
	fclose($STDERR);
}