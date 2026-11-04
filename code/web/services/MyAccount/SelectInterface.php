<?php

class MyAccount_SelectInterface extends Action {
	public $selectInterfaceMessage;

	public static function getLibraryBaseUrl(Library $selectedLibrary): string {
		if (!empty($selectedLibrary->baseUrl)) {
			return rtrim($selectedLibrary->baseUrl, '/');
		}

		global $configArray;
		$baseUrl = $configArray['Site']['url'];
		$urlPortions = explode('://', $baseUrl);
		$subdomain = $selectedLibrary->subdomain;
		// Site.url can be scoped to the catalog currently being viewed. Remove
		// that catalog's subdomain before adding the selected library's one.
		$activeLibrary = Library::getActiveLibrary();
		if ($activeLibrary && !empty($activeLibrary->subdomain)) {
			$urlPortions[1] = preg_replace('/^' . preg_quote($activeLibrary->subdomain, '/') . '\\./', '', $urlPortions[1]);
		}
		if (strpos($urlPortions[1], 'opac2') !== false) {
			$urlPortions[1] = str_replace('opac2.', '', $urlPortions[1]);
			$subdomain .= '2';
		}
		$urlPortions[1] = str_replace('opac.', '', $urlPortions[1]);
		return rtrim($urlPortions[0] . '://' . $subdomain . '.' . $urlPortions[1], '/');
	}

	function launch() {
		global $interface;
		global $logger;

		$libraries = [];
		$library = new Library();
		$library->createSearchInterface = 1;
		$library->showInSelectInterface = 1;
		$library->orderBy('displayName');
		$library->find();
		while ($library->fetch()) {
			$libraries[$library->libraryId] = [
				'id' => $library->libraryId,
				'displayName' => $library->displayName,
				'library' => clone $library,
				'isLibrary' => true,
			];
		}
		$location = new Location();
		$location->createSearchInterface = 1;
		$location->showInSelectInterface = 1;
		$location->orderBy('displayName');
		$location->find();
		while ($location->fetch()) {
			// Only include locations that have a valid parent library.
			$parentLibrary = $location->getParentLibrary();
			if (!empty($parentLibrary)) {
				$libraries['location' . $location->locationId] = [
					'id' => $location->locationId,
					'displayName' => $location->displayName,
					'location' => clone $location,
					'isLibrary' => false,
				];
			}
		}
		$sortLibraries = function ($library1, $library2) {
			return strcasecmp($library1['displayName'], $library2['displayName']);
		};
		usort($libraries, $sortLibraries);
		$interface->assign('libraries', $libraries);

		/** @var Location $locationSingleton */ //global $locationSingleton;
		//$physicalLocation = $locationSingleton->getIPLocation();

		$goToPath = null;
		$gotoModule = null;
		$gotoAction = null;
		if (isset($_REQUEST['goToPath']) && is_string($_REQUEST['goToPath'])) {
			// Only allow internal paths as destinations.
			$requestedPath = $_REQUEST['goToPath'];
			if (str_starts_with($requestedPath, '/') && !str_starts_with($requestedPath, '//')) {
				$goToPath = $requestedPath;
				$interface->assign('goToPath', $goToPath);
			}
		}
		if (isset($_REQUEST['gotoModule'])) {
			$gotoModule = $_REQUEST['gotoModule'];
			$interface->assign('gotoModule', $gotoModule);
		}
		if (isset($_REQUEST['gotoAction'])) {
			$gotoAction = $_REQUEST['gotoAction'];
			$interface->assign('gotoAction', $gotoAction);
		}

		$redirectLibrary = null;
		if (!array_key_exists('noRememberThis', $_REQUEST)) {
			$user = UserAccount::getLoggedInUser();
			if (isset($_REQUEST['library'])) {
				$redirectLibrary = $_REQUEST['library'];
				//}elseif (!is_null($physicalLocation)){
				//	$redirectLibrary = $physicalLocation->libraryId;
			} elseif ($user && isset($user->preferredLibraryInterface) && is_numeric($user->preferredLibraryInterface)) {
				$redirectLibrary = $user->preferredLibraryInterface;
			} elseif (isset($_COOKIE['PreferredLibrarySystem'])) {
				$redirectLibrary = $_COOKIE['PreferredLibrarySystem'];
			}
			$interface->assign('noRememberThis', false);
		} else {
			$interface->assign('noRememberThis', true);
		}

		if ($redirectLibrary != null && array_key_exists($redirectLibrary, $libraries)) {
			$logger->log("Selected library $redirectLibrary", Logger::LOG_DEBUG);

			if ($libraries[$redirectLibrary]['isLibrary']) {
				/** @var Library $selectedLibrary */
				$selectedLibrary = $libraries[$redirectLibrary]['library'];
				$baseUrl = self::getLibraryBaseUrl($selectedLibrary);
				if (empty($goToPath)) {
					$baseUrl .= '?branch=';
				}
			} else {
				/** @var Location $selectedLocation */
				$selectedLocation = $libraries[$redirectLibrary]['location'];
				global $configArray;
				$baseUrl = $configArray['Site']['url'];
				$branch = '';
				$urlPortions = explode('://', $baseUrl);
				//Get rid of extra portions of the url
				$subdomain = $selectedLocation->subdomain;
				$buildUrl = true;
				if (empty($subdomain)) {
					$library = $selectedLocation->getParentLibrary();
					if (!empty($library->baseUrl)) {
						$baseUrl = $library->baseUrl;
						$branch = $selectedLocation->code;
						$buildUrl = false;
					} else {
						$subdomain = $selectedLocation->getParentLibrary()->subdomain;
						$branch = $selectedLocation->code;
					}
				}
				if ($buildUrl) {
					if (strpos($urlPortions[1], 'opac2') !== false) {
						$urlPortions[1] = str_replace('opac2.', '', $urlPortions[1]);
						$subdomain .= '2';
					}
					$urlPortions[1] = str_replace('opac.', '', $urlPortions[1]);
					$baseUrl = $urlPortions[0] . '://' . $subdomain . '.' . $urlPortions[1];
				}
				if (!empty($branch) && empty($goToPath)) {
					$baseUrl .= '?branch=' . $branch;
				} elseif (empty($goToPath)) {
					$baseUrl .= '?branch=';
				}
			}
			if (!empty($goToPath)) {
				$baseUrl = rtrim($baseUrl, '/') . $goToPath;
			} else {
				if (!empty($gotoModule)) {
					$baseUrl .= '/' . $gotoModule;
				}
				if (!empty($gotoAction)) {
					$baseUrl .= '/' . $gotoAction;
				}
			}
			if (isset($_REQUEST['rememberThis']) && isset($_REQUEST['submit'])) {
				if ($user) {
					$user->preferredLibraryInterface = $redirectLibrary;
					$user->update();
					$_SESSION['userinfo'] = serialize($user);
				}
				//Set a cookie to remember the location when not logged in
				//Remember for a year
				setcookie('PreferredLibrarySystem', $redirectLibrary, time() + 60 * 60 * 24 * 365, '/');
			}

			header('Location:' . $baseUrl);
			die();
		}

        if (!empty($this->selectInterfaceMessage)) {
            $interface->assign('selectInterfaceMessage', $this->selectInterfaceMessage);
        }

		// Display Page
		$this->display('selectInterface.tpl', 'Select Library Catalog', '');
	}

	function getBreadcrumbs(): array {
		return [];
	}
}
