<?php /** @noinspection PhpMissingFieldTypeInspection */

class IlsRecord extends DataObject {
	public $__table = 'ils_records';    // table name
	public $id;
	public $ilsId;
	/** @noinspection PhpUnused */
	public $checksum;
	/** @noinspection PhpUnused */
	public $dateFirstDetected;
	public $deleted;
	public $dateDeleted;
	/** @noinspection PhpUnused */
	public $suppressedNoMarcAvailable;
	public $source;
	public $sourceData;
	public $lastModified;
	public $suppressed;
	/** @noinspection PhpUnused */
	public $suppressionNotes;

	public function getNumericColumnNames(): array {
		return [
			'suppressed',
			'deleted',
			'dateFirstDetected',
			'dateDeleted',
			'suppressedNoMarcAvailable',
		];
	}

	public function getCompressedColumnNames(): array {
		return ['sourceData'];
	}

	/**
	 * @var IlsRecord[]
	 */
	private static $preloadedIlsRecords = [];
	/**
	 * Preloads ILS Records for a type and identifier using minimal database queries
	 *
	 * @param string $type
	 * @param array $identifiers
	 * @return void
	 */
	static function preloadIlsRecords(string $type, array $identifiers) : void {
		if (!isset(self::$preloadedIlsRecords[$type])) {
			self::$preloadedIlsRecords[$type] = [];
		}
		foreach ($identifiers as $identifier) {
			if (!array_key_exists($identifier, self::$preloadedIlsRecords[$type])) {
				self::$preloadedIlsRecords[$type][$identifier] = null;
			}
		}
		$ilsRecords = new IlsRecord();
		$ilsRecords->source = $type;
		$ilsRecords->whereAddIn('ilsId', $identifiers, true);
		$allRelatedRecords = $ilsRecords->fetchAll();
		foreach ($allRelatedRecords as $relatedRecord) {
			self::$preloadedIlsRecords[$type][$relatedRecord->ilsId] = $relatedRecord;
		}
	}

	/**
	 * @param string $type
	 * @param string $identifier
	 * @return ?IlsRecord
	 */
	static function getIlsRecordForId(string $type, string $identifier) : ?IlsRecord {
		if (!isset(self::$preloadedIlsRecords[$type])) {
			self::$preloadedIlsRecords[$type] = [];
		}
		if (!array_key_exists($identifier, self::$preloadedIlsRecords[$type])) {
			while (count(self::$preloadedIlsRecords[$type]) >= 50) {
				$firstKey = array_key_first(self::$preloadedIlsRecords[$type]);
				unset(self::$preloadedIlsRecords[$type][$firstKey]);
			}

			$ilsRecord = new IlsRecord();
			$ilsRecord->source = $type;
			$ilsRecord->ilsId = $identifier;
			if ($ilsRecord->find(true)) {
				self::$preloadedIlsRecords[$type][$identifier] = $ilsRecord;
			} else {
				self::$preloadedIlsRecords[$type][$identifier] = null;
			}
		}
		return self::$preloadedIlsRecords[$type][$identifier];
	}
}