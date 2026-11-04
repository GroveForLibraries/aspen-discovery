<?php
require_once ROOT_DIR . '/sys/SearchObject/WebsitesSearcher.php';

/**
 * Website searcher used by Web Resources List and A to Z pages.
 * Facet and filter links are built against the list page so users stay on it when filtering instead of going to search results.
 */
class SearchObject_WebResourcesSearcher extends SearchObject_WebsitesSearcher {
	public function __construct() {
		parent::__construct();

		$this->resultsModule = 'WebBuilder';
		$this->resultsAction = $_REQUEST['action'];
	}

	/**
	 * Load filters from simplified url parameters named after the facet (i.e. ?category_facet=Arts+and+Music).
	 * Multiple values for a facet use array syntax (?category_facet[]=Arts+and+Music&category_facet[]=History).
	 * Standard filter[] parameters and locked facets are still handled by the parent.
	 */
	protected function initFilters(): void {
		$hasFacetParameters = false;
		foreach (array_keys($this->getFacetConfig()) as $facetField) {
			if (!isset($_REQUEST[$facetField])) {
				continue;
			}
			$values = is_array($_REQUEST[$facetField]) ? $_REQUEST[$facetField] : [$_REQUEST[$facetField]];
			foreach ($values as $value) {
				if (!is_array($value) && strlen($value) > 0) {
					$this->addFilter([
						$facetField,
						strip_tags($value),
					]);
					$hasFacetParameters = true;
				}
			}
		}
		if (!$hasFacetParameters || isset($_REQUEST['filter'])) {
			parent::initFilters();
		}
	}

	/**
	 * The applied filters as url-encoded key=value strings.
	 */
	public function getFilterParams(): array {
		$params = [];
		foreach ($this->filterList as $field => $values) {
			$parameterName = urlencode($field) . (count($values) > 1 ? '[]' : '');
			foreach ($values as $value) {
				$params[] = $parameterName . '=' . urlencode($value);
			}
		}
		return $params;
	}

	/**
	 * Build a url for the list page containing the applied filters plus the A-Z letter, if any.
	 * Search terms, sort, page, and view are fixed for this page so they are left off.
	 */
	public function renderSearchUrl(bool $includePaginationAndSortParameters = true) : string {
		$params = $this->getFilterParams();

		//startsWith is not a Solr filter, but carry it along so facet links keep the letter
		if (isset($_REQUEST['startsWith']) && is_string($_REQUEST['startsWith']) && ctype_alpha($_REQUEST['startsWith'])
			&& (strlen($_REQUEST['startsWith']) == 1 || $_REQUEST['startsWith'] == 'num')) {
			$params[] = 'startsWith=' . urlencode($_REQUEST['startsWith']);
		}

		$url = "/$this->resultsModule/$this->resultsAction";
		if (!empty($params)) {
			$url .= '?' . implode('&', $params);
		}
		return $url;
	}
}
