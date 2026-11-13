<?php
require_once ROOT_DIR . '/sys/BaseLogEntry.php';

/***************************************
 * Simple class to retrieve feed of NYT best sellers
 * documentation:
 * http://developer.nytimes.com/docs/read/best_sellers_api
 *
 * Last Updated: 2016-02-26 JN
 ***************************************
 */
class NYTApi {
	private static ?NYTApi $apiSingleton = null;
	const BASE_URI = 'https://api.nytimes.com/svc/books/v3/lists/';
	protected ?string $api_key = null;

	private ?object $rawJSON = null;
	private ?array $allListsInfo = null;
	private ?string $lastUpdateDate = null;
	private string $copyright = '';

	/**
	 * NYTApi constructor.
	 * @param string $key
	 */
	public function __construct(string $key) {
		$this->api_key = $key;
	}

	public static function getNYTApi(string $key) : NYTApi{
		if (NYTApi::$apiSingleton == null) {
			NYTApi::$apiSingleton = new NYTApi($key);
		}
		return NYTApi::$apiSingleton;
	}

	/**
	 * Returns an array of information about all lists from v3 of the New York Times API
	 * @return array
	 */
	public function getListsOverview() : array {
		if (isset($this->allListsInfo)) {
			return $this->allListsInfo;
		}

		$url = self::BASE_URI . 'overview.json?api-key=' . $this->api_key;

		// array of request options
		$curl_opts = [
			// set request url
			CURLOPT_URL => $url,
			// return data
			CURLOPT_RETURNTRANSFER => 1,
			// do not include header in result
			CURLOPT_HEADER => 0,
			// set user agent
			CURLOPT_USERAGENT => 'Aspen Discovery',
		];
		// Get cURL resource
		$curl = curl_init();
		// Set curl options
		curl_setopt_array($curl, $curl_opts);
		// Send the request & save response to $response
		$response = curl_exec($curl);
		//Get response code and errors
		$responseCode = curl_getinfo($curl, CURLINFO_HTTP_CODE);
		$curlError = curl_error($curl);
		// Close request to clear up some resources
		curl_close($curl);
		//Debugging
		if ($responseCode !== 200) {
			global $logger;
			$logger->log("Did not get a good result from NYT", Logger::LOG_WARNING);
			if ($response === false) {
				$logger->log("Curl error: " . $curlError, Logger::LOG_WARNING);
			} else {
				$logger->log($response, Logger::LOG_WARNING);
			}
		}

		$jsonResponse = json_decode($response);
		if ($jsonResponse) {
			$this->rawJSON = $jsonResponse;
			$this->allListsInfo = $jsonResponse->results->lists;
			$this->lastUpdateDate = $jsonResponse->results->published_date;
			$this->copyright = $jsonResponse->copyright ?? '';
		}else{
			$this->allListsInfo = [];
		}

		// return response
		return $this->allListsInfo;
	}

	public function getLastUpdateDate() : ?string {
		return $this->lastUpdateDate;
	}

	public function getCopyright() : string {
		return $this->copyright;
	}
}