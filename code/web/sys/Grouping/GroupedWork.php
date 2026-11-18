<?php /** @noinspection PhpMissingFieldTypeInspection */

class GroupedWork extends DataObject {
	public $__table = 'grouped_work';    // table name
	public $id;
	public $permanent_id;
	public $full_title;
	public $author;
	public $grouping_category;
	public $date_updated;
	public $referenceCover;

	/**
	 * @param bool $updatePrimaryIdentifiers Updating primary identifiers will force regrouping and is a bit slower
	 */
	public function forceReindex(bool $updatePrimaryIdentifiers = false) : void {
		require_once ROOT_DIR . '/sys/Indexing/GroupedWorkScheduledWorkIndex.php';
		$scheduledWork = new GroupedWorkScheduledWorkIndex();
		$scheduledWork->permanent_id = $this->permanent_id;
		$scheduledWork->indexAfter = time();
		$scheduledWork->processed = 0;
		$scheduledWork->insert();

		if ($updatePrimaryIdentifiers) {
			require_once ROOT_DIR . '/sys/Grouping/GroupedWorkPrimaryIdentifier.php';
			$groupedWorkPrimaryIdentifier = new GroupedWorkPrimaryIdentifier();
			$groupedWorkPrimaryIdentifier->grouped_work_id = $this->id;
			$groupedWorkPrimaryIdentifier->find();
			while ($groupedWorkPrimaryIdentifier->fetch()) {
				require_once ROOT_DIR . '/sys/Indexing/RecordIdentifiersToReload.php';
				$recordIdentifierToReload = new RecordIdentifiersToReload();
				$recordIdentifierToReload->type = $groupedWorkPrimaryIdentifier->type;
				$recordIdentifierToReload->identifier = $groupedWorkPrimaryIdentifier->identifier;
				$recordIdentifierToReload->insert();
			}
		}
	}

	public function getDebuggingInfo() : GroupedWorkDebugInfo {
		require_once ROOT_DIR . '/sys/Indexing/GroupedWorkDebugInfo.php';
		$indexDebugInfo = new GroupedWorkDebugInfo();
		$indexDebugInfo->permanent_id = $this->permanent_id;
		if ($indexDebugInfo->find(true)) {
			//Record has already been marked for debugging
		}else{
			//Need to create a new record
			$indexDebugInfo->processed = 0;
			$indexDebugInfo->insert();
			$this->forceReindex();
		}
		return $indexDebugInfo;
	}

	public function getParsedDebugInfo() : array {
		$debugInfo = $this->getDebuggingInfo();
		$lines = [];
		$tagCounts = [];

		//Split text into lines with tags for filtering data, we don't want the user to see the [tag] on every line
		foreach (explode('<br/>', $debugInfo->debugInfo) as $entry) {
			if ($entry === '') {
				continue;
			}
			//Match leading indentation (&nbsp; or whitespace), an optional [tag], then the message
			if (preg_match('/^((?:&nbsp;|\s)*)(?:\[([A-Za-z0-9_-]+)\]\s*)?(.*)$/s', $entry, $m)) {
				$indent = $m[1];
				$tag = ($m[2] !== '') ? strtolower($m[2]) : 'other';
				$text = $m[3];
			} else {
				$indent = '';
				$tag = 'other';
				$text = $entry;
			}
			$lines[] = [
				'tag' => $tag,
				'indent' => $indent,
				'text' => htmlspecialchars($text, ENT_QUOTES, 'UTF-8'),
			];
			$tagCounts[$tag] = ($tagCounts[$tag] ?? 0) + 1;
		}
		ksort($tagCounts);

		//Assign tag labels, is mostly just capitalizing the tag, but readerFriendlyLabels can be used instead
		$tagLabels = [];
		$readerFriendlyLabels = [
			'litform' => 'Literary Format',
		];
		foreach (array_keys($tagCounts) as $tag) {
			$tagLabels[$tag] = $readerFriendlyLabels[$tag] ?? ucfirst($tag);
		}

		return ['lines' => $lines, 'tagCounts' => $tagCounts, 'tagLabels' => $tagLabels, 'processed' => $debugInfo->processed, 'debugTime' => $debugInfo->debugTime];
	}

	public function resetDebugging() : GroupedWorkDebugInfo {
		require_once ROOT_DIR . '/sys/Indexing/GroupedWorkDebugInfo.php';
		$indexDebugInfo = new GroupedWorkDebugInfo();
		$indexDebugInfo->permanent_id = $this->permanent_id;
		if ($indexDebugInfo->find(true)) {
			$indexDebugInfo->processed = 0;
			$indexDebugInfo->debugInfo = '';
			$indexDebugInfo->debugTime = null;
			$indexDebugInfo->update();
		}else{
			//Need to create a new record
			$indexDebugInfo->processed = 0;
			$indexDebugInfo->insert();
			$this->forceReindex();
		}
		return $indexDebugInfo;
	}
} 