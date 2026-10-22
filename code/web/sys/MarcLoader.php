<?php
require_once(ROOT_DIR . '/sys/File/MARC.php');

/**
 * Class MarcLoader
 *
 * Loads a Marc record from the database or file system as appropriate.
 */
class MarcLoader {
	/**
	 * @param ?array $record An array of record data from Solr
	 * @return ?File_MARC_Record
	 */
	public static function loadMarcRecordFromRecord(?array $record) : ?File_MARC_Record {
		if (is_null($record)){
			return null;
		} else {
			if ($record['recordtype'] == 'marc') {
				return MarcLoader::loadMarcRecordByILSId($record['id'], $record['recordtype']);
			} else {
				return null;
			}
		}
	}

	/**
	 * @var $loadedMarcRecords File_MARC_Record[]
	 */
	private static array $loadedMarcRecords = [];

	/**
	 * @param string $id The id of the record within the ils
	 * @param string $recordType The type of the record in the system
	 * @return ?File_MARC_Record
	 */
	public static function loadMarcRecordByILSId(string $id, string $recordType = 'marc', string $omitFields = null) : ?File_MARC_Record {
		if (str_contains($id, ':')) {
			$recordInfo = explode(':', $id);
			$recordType = $recordInfo[0];
			$ilsId = $recordInfo[1];
		} else {
			$ilsId = $id;
		}

		if (array_key_exists($ilsId, MarcLoader::$loadedMarcRecords)) {
			return MarcLoader::$loadedMarcRecords[$ilsId];
		}

		require_once ROOT_DIR . '/sys/Indexing/IlsRecord.php';
		$ilsRecord = IlsRecord::getIlsRecordForId($recordType, $ilsId);
		$marcRecord = null;
		if ($ilsRecord != null) {
			if (!empty($ilsRecord->sourceData)) {
				$marcRecord = new File_MARC_Record();
				if (!$marcRecord->jsonDecode($ilsRecord->sourceData)) {
					AspenError::raiseError(new AspenError('Could not load marc record for record ' . $ilsId));
				} elseif (!empty($omitFields)) {
					MarcLoader::removeSuppressedFields($marcRecord, $omitFields);
				}
			}
		}
		//Make sure not to use too much memory
		global $memoryWatcher;
		if (count(MarcLoader::$loadedMarcRecords) > 50) {
			array_shift(MarcLoader::$loadedMarcRecords);
			$memoryWatcher->logMemory("Removed Cached MARC");
		}
		$memoryWatcher->logMemory("Loaded MARC for $id");
		MarcLoader::$loadedMarcRecords[$id] = $marcRecord;
		global $timer;
		$timer->logTime("Loaded MARC record by ILS ID");
		return $marcRecord;
	}

	private static function removeSuppressedFields(File_MARC_Record $marcRecord, string $omitFields) : void {
		$suppressedFields = array_filter(array_map('trim', explode('|', trim($omitFields, " []"))), 'strlen');

		$wholeTags = [];  // tags to drop entirely
		$omitByTag = [];  // specific subfields to drop

		foreach ($suppressedFields as $suppressedField) {
			if (!preg_match('/^(\d{3})(.)?$/', $suppressedField, $m)) {
				continue; // ignore malformed entries
			}
			if (!isset($m[2])) {
				$wholeTags[$m[1]] = true;
			} else {
				$omitByTag[$m[1]][$m[2]] = true;
			}
		}

		if (empty($wholeTags) && empty($omitByTag)) {
			return;
		}

		$fieldsToDelete = [];  // fields to remove
		$replacements   = [];  // fields to replace

		foreach ($marcRecord->getFields() as $field) {
			$tag = (string)$field->getTag();

			// omit whole tag
			if (isset($wholeTags[$tag])) {
				$fieldsToDelete[] = $field;
				continue;
			}

			// only data fields with rules are handled (no control fields)
			if (!($field instanceof File_MARC_Data_Field) || !isset($omitByTag[$tag])) {
				continue;
			}

			$kept = [];
			$removedAny = false;
			foreach ($field->getSubfields() as $subfield) {
				if (isset($omitByTag[$tag][(string)$subfield->getCode()])) {
					$removedAny = true;
					continue;
				}
				// fresh copy so we don't carry over linked-list node state
				$kept[] = new File_MARC_Subfield($subfield->getCode(), $subfield->getData());
			}

			// nothing matched, leave the field untouched
			if (!$removedAny) {
				continue;
			}

			// nothing left in field, drop the whole field
			if (count($kept) === 0) {
				$fieldsToDelete[] = $field;
				continue;
			}

			// rebuild the field from the surviving subfields
			$replacements[] = [
				'old' => $field,
				'new' => new File_MARC_Data_Field(
					$tag,
					$kept,
					$field->getIndicator(1),
					$field->getIndicator(2)
				),
			];
		}

		// replace fields not omitted entirely
		foreach ($replacements as $r) {
			$marcRecord->insertField($r['new'], $r['old'], true);
			$r['old']->delete();
		}

		// remove fields omitted entirely
		foreach ($fieldsToDelete as $field) {
			$field->delete();
		}
	}

	/**
	 * @param string $id Passed as <type>:<id>
	 * @return int|false
	 */
	public static function lastModificationTimeForIlsId(string $id) : int|false {
		if (str_contains($id, ':')) {
			$recordInfo = explode(':', $id);
			$recordType = $recordInfo[0];
			$ilsId = $recordInfo[1];
		} else {
			//Try to infer the indexing profile from the module
			global $activeRecordProfile;
			if ($activeRecordProfile) {
				$recordType = $activeRecordProfile->name;
			} else {
				$recordType = 'ils';
			}
			$ilsId = $id;
		}

		require_once ROOT_DIR . '/sys/Indexing/IlsRecord.php';
		$ilsRecord = IlsRecord::getIlsRecordForId($recordType, $ilsId);
		if ($ilsRecord != null) {
			return $ilsRecord->lastModified;
		}else{
			return false;
		}
	}

	/**
	 * @param string $id Passed as <type>:<id>
	 * @return boolean
	 */
	public static function marcExistsForILSId(string $id) : bool {
		if (str_contains($id, ':')) {
			$recordInfo = explode(':', $id, 2);
			$recordType = $recordInfo[0];
			$ilsId = $recordInfo[1];
			if ($recordType == 'external_econtent') {
				$recordInfo = explode(':', $ilsId);
				$recordType = $recordInfo[0];
				$ilsId = $recordInfo[1];
			}
		} else {
			//Try to infer the indexing profile from the module
			global $activeRecordProfile;
			if ($activeRecordProfile) {
				$recordType = $activeRecordProfile->name;
			} else {
				$recordType = 'ils';
			}
			$ilsId = $id;
		}

		require_once ROOT_DIR . '/sys/Indexing/IlsRecord.php';
		$ilsRecord = IlsRecord::getIlsRecordForId($recordType, $ilsId);

		if ($ilsRecord != null) {
			$hasMarc = !empty($ilsRecord->sourceData);
		} else {
			$hasMarc = false;
		}
		return $hasMarc;
	}
}