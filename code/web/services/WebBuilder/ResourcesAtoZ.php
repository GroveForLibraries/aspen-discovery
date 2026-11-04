<?php

class WebBuilder_ResourcesAtoZ extends Action
{
	function launch() : void
	{
		global $interface;
		global $activeLanguage;

		require_once ROOT_DIR . '/sys/WebBuilder/WebResource.php';
		require_once ROOT_DIR . '/RecordDrivers/WebResourceRecordDriver.php';
		require_once ROOT_DIR . '/sys/WebBuilder/WebResourcesSetting.php';

		global $library;

		$filter = '';
		if (isset($_REQUEST['startsWith']) && ctype_alpha($_REQUEST['startsWith']) && (strlen($_REQUEST['startsWith']) == 1 || $_REQUEST['startsWith'] == "num")) {
			$filter = $_REQUEST['startsWith'];
		}

		$webResourceSetting = new WebResourcesSetting();
		$webResourceSetting->id = $library->webResourcesSettingId;

		//Decide whether to show the facet sidebar
		$webResourceSettings = $library->getLibraryWebResourceSettings();
		$showFilterOptions = !empty($webResourceSettings) && $webResourceSettings->showFilterOptionsAtoZ == 1;

		//Always query Solr, scoped to the active library. Facet parameters on the url
		//(i.e. ?category_facet=Arts+and+Music) are applied by init()
		/** @var SearchObject_WebResourcesSearcher $searchObject */
		$searchObject = SearchObjectFactory::initSearchObject('WebResources');
		$searchObject->init(); //must come BEFORE addFilter(), since init() rebuilds the filter list from the URL

		$scopeField = 'scope_has_related_records';
		$hasAnyFilter = !empty($_REQUEST['filter']);

		if ($showFilterOptions) {
			if (!UserAccount::isLoggedIn()) {
				//Logged-out users: apply the default unless a filter is present or they're navigating within this page
				if (!$hasAnyFilter
					&& !$library->isConsortialCatalog
					&& !empty($library->subdomain)) {

					$cameFromThisPage = false;
					if (!empty($_SERVER['HTTP_REFERER'])) {
						$ref = parse_url($_SERVER['HTTP_REFERER']);
						$sameHost = isset($ref['host']) && strcasecmp($ref['host'], $_SERVER['HTTP_HOST'] ?? '') === 0;
						$refPath = rtrim($ref['path'] ?? '', '/');
						$currentPath = rtrim(parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH) ?? '', '/');
						$cameFromThisPage = $sameHost && strcasecmp($refPath, $currentPath) === 0;
					}

					if (!$cameFromThisPage) {
						$searchObject->addFilter($scopeField . ':' . $library->subdomain);
					}
				}
			}  else {
				//Logged-in users: apply the default once per session so it can be removed
				$userLibrary = Library::getPatronHomeLibrary();
				$scopeLibrary = !empty($userLibrary) ? $userLibrary : $library;
				$scopeValue = $scopeLibrary->subdomain;
				$sessionKey = 'webResourcesDefaultScopeApplied';

				if (!$scopeLibrary->isConsortialCatalog && !empty($scopeValue)) {
					$hasScopeFilter = false;
					foreach ((array)($_REQUEST['filter'] ?? []) as $requestFilter) {
						if (str_starts_with($requestFilter, $scopeField . ':')) {
							$hasScopeFilter = true;
							break;
						}
					}

					if (!$hasScopeFilter && !$hasAnyFilter && empty($_SESSION[$sessionKey])) {
						//First visit with a clean URL: apply the default
						$searchObject->addFilter($scopeField . ':' . $scopeValue);
						$_SESSION[$sessionKey] = true;
					}
					//Otherwise the user removed it (or chose other filters), so leave it off
				}
			}
		}

		$searchObject->init();
		$searchObject->disableSpelling();
		$searchObject->setSearchTerms([
			'lookfor' => '*:*',
			'index' => 'WebsiteKeyword',
		]);
		$searchObject->addHiddenFilter('recordtype', '"WebResource"');
		//Sort alphabetically by title
		$searchObject->setSort('title_sort');
		$searchObject->setPage(1);
		$searchObject->setLimit(1000);
		//Handle filtering by first character
		if (!empty($filter)) {
			$value = ($filter === 'num')
				? '/[0-9].*/'
				: mb_strtolower($filter) . '*';
			$searchObject->addHiddenFilter('title_sort', $value);
		}
		//Process recommendations so SideFacets assigns facets to the interface
		$result = $searchObject->processSearch(true, true);

		$solrSearchSucceeded = !($result instanceof AspenError) && empty($result['error']);

		$validLetters = [];
		$webResources = [];
		if ($solrSearchSucceeded) {
			foreach ($searchObject->getResultRecordSet() as $solrRecord) {
				$webResourceRecordDriver = new WebResourceRecordDriver($solrRecord);
				if (!$webResourceRecordDriver->isValid()) {
					continue;
				}

				$title = $webResourceRecordDriver->getTitle();

				//Work out the A-Z letter for this resource
				$firstLetter = mb_strtoupper(mb_substr($title, 0, 1));
				$letterKey = is_numeric($firstLetter) ? 'num' : $firstLetter;
				$validLetters[$letterKey] = true;

				//Apply the A-Z filter in PHP
				if (!empty($filter) && $letterKey !== mb_strtoupper($filter)) {
					continue;
				}

				$id = $webResourceRecordDriver->getNumericId();
				$webResources[$id] = [
					'id' => $id,
					'title' => $title,
					'description' => $webResourceRecordDriver->getDescription(),
					'url' => $webResourceRecordDriver->getLinkUrl(),
					'bookCoverUrl' => $webResourceRecordDriver->getBookCoverUrl('medium'),
					'openInNewTab' => $webResourceRecordDriver->opensInNewTab(),
				];
			}
		}
		$validLetters = array_keys($validLetters);

		$filterArray = range('A', 'Z');
		$filterArray = ['num', ...$filterArray];

		$filterParams = $searchObject->getFilterParams();
		$atozQuery = empty($filterParams) ? '' : implode('&', $filterParams) . '&';
		$interface->assign('atozQuery', $atozQuery);

		$interface->assign('validLetters', $validLetters);
		$interface->assign('filterArray', $filterArray);
		$interface->assign('webResources', $webResources);
		$interface->assign('description', $webResourceSetting->getTextBlockTranslation('descriptionAtoZ', $activeLanguage->code));
		$interface->assign('searchSourceSpecial', 'WebResources');

		$sidebar = '';
		if ($showFilterOptions) {
			$interface->assign('sideRecommendations', $searchObject->getRecommendationsTemplates('side'));
			if (($searchObject->getResultTotal() > 0) && (count($searchObject->getFacetList()) > 0)) {
				$sidebar = 'Search/results-sidebar.tpl';
			}
		} else {
			$interface->assign('sideRecommendations', []);
		}

		$this->display('resourcesAtoZ.tpl', 'Resources A-Z', $sidebar, false);
	}

	function getBreadcrumbs(): array
	{
		$breadcrumbs = [];
		$breadcrumbs[] = new Breadcrumb('/Home', 'Home');
		return $breadcrumbs;
	}
}
