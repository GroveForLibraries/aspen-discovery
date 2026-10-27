<?php
require_once ROOT_DIR . '/sys/WebBuilder/WebResource.php';

class WebBuilder_ResourcesList extends Action {

	function launch() {
		global $library;
		global $interface;

		require_once ROOT_DIR . '/RecordDrivers/WebResourceRecordDriver.php';

		$resourcesByCategory = [];
		$featuredResources = [];

		//Decide whether to show the facet sidebar
		$webResourceSettings = $library->getLibraryWebResourceSettings();
		$showFilterOptions = !empty($webResourceSettings) && $webResourceSettings->showFilterOptionsListing == 1;

		//Always query Solr. Facet parameters on the url
		//(i.e. ?category_facet=Arts+and+Music) are applied by init()
		/** @var SearchObject_WebResourcesSearcher $searchObject */
		$searchObject = SearchObjectFactory::initSearchObject('WebResources');
		$searchObject->init(); //must come BEFORE addFilter(), since init() rebuilds the filter list from the URL

		$scopeField = 'scope_has_related_records';
		$hasAnyFilter = !empty($_REQUEST['filter']);

		//Only apply a default scope filter when filter options are shown to the user
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
			} else {
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
						$searchObject->addFilter($scopeField . ':' . $scopeValue);
						$_SESSION[$sessionKey] = true;
					}
				}
			}
		}

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
		//Process recommendations so SideFacets assigns facets to the interface
		$result = $searchObject->processSearch(true, true);

		$solrSearchSucceeded = !($result instanceof AspenError) && empty($result['error']);

		$numLoaded = 0;
		if ($solrSearchSucceeded) {
			foreach ($searchObject->getResultRecordSet() as $solrRecord) {
				$recordDriver = new WebResourceRecordDriver($solrRecord);
				if (!$recordDriver->isValid()) {
					continue;
				}

				$resourceData = [
					'id' => $recordDriver->getNumericId(),
					'title' => $recordDriver->getTitle(),
					'description' => $recordDriver->getDescription(),
					'url' => $recordDriver->getLinkUrl(),
					'bookCoverUrl' => $recordDriver->getBookCoverUrl('medium'),
					'openInNewTab' => $recordDriver->opensInNewTab(),
					'featured' => $recordDriver->isFeatured(),
				];

				if ($resourceData['featured']) {
					$featuredResources[] = $resourceData;
				}

				//Categories come from the indexed category facet field (may be a string or an array)
				$categories = $solrRecord['category_facet'] ?? [];
				if (!is_array($categories)) {
					$categories = [$categories];
				}
				foreach ($categories as $categoryName) {
					if (!array_key_exists($categoryName, $resourcesByCategory)) {
						$resourcesByCategory[$categoryName] = [];
					}
					$resourcesByCategory[$categoryName][] = $resourceData;
				}
				$numLoaded++;
			}
		}
		ksort($resourcesByCategory);

		$interface->assign('resourcesByCategory', $resourcesByCategory);
		$interface->assign('featuredResources', $featuredResources);
		$interface->assign('numLoaded', $numLoaded);
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

		$this->display('resourcesList.tpl', 'Research & Learn', $sidebar);
	}

	function getBreadcrumbs(): array {
		$breadcrumbs = [];
		$breadcrumbs[] = new Breadcrumb('/', 'Home');
		$breadcrumbs[] = new Breadcrumb('', 'Resources', true);
		return $breadcrumbs;
	}
}