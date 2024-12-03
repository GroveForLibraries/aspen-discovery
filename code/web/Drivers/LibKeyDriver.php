<?php

class LibKeyDriver {
	public function getLibKeyLink(string $doiUrl): string | null {
		require_once ROOT_DIR . '/sys/LibKey/LibKeySetting.php';
		$activeLibrary = Library::getActiveLibrary();
		$settings = new LibKeySetting();
		$settings->whereAdd("id=$activeLibrary->libKeySettingId");
		if (!$settings->find(true)) {
			return null;
		}
		$curlWrapper = new CurlWrapper;
		$response = $curlWrapper->curlGetPage("https://public-api.thirdiron.com/public/v1/libraries/" . $settings->libraryId  . "/articles/doi/" . $this->extractDoi($doiUrl) . "?access_token=" . $settings->apiKey);
		if (empty($response)) {
			return null;
		}
		return json_decode($response, true)["data"]["bestIntegratorLink"]["bestLink"];
	}
	public function extractDoi(string $url): string {
		$doi = str_replace(["https://doi.org/", "http://"], "", $url);
		return $doi;
	}
}