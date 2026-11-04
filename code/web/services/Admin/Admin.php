<?php

abstract class Admin_Admin extends Action {
	protected $db;

	function __construct($isStandalonePage = false) {
		parent::__construct($isStandalonePage);

		$_SESSION['returnToModule'] = 'Admin';
		$_SESSION['returnToAction'] = 'Home';

		global $updatingSearchIndex;
		if ($updatingSearchIndex) {
			return;
		}
		$user = UserAccount::getLoggedInUser();

		//If the user isn't logged in, take them to the login page
		if (!$user) {
			require_once ROOT_DIR . '/services/MyAccount/Login.php';
			$myAccountAction = new MyAccount_Login($isStandalonePage);
			$myAccountAction->launch();
			exit();
		}

		header("Cache-Control: no-cache, no-store, must-revalidate");
		header("Pragma: no-cache");
		header("Expires: 0");

		//Make sure the user has permission to access the page
		$userCanAccess = $this->canView();

		if (!$userCanAccess) {
			$this->display(ROOT_DIR . '/interface/themes/responsive/Admin/noPermission.tpl', 'Access Error');
			exit();
		}

		global $interface;
		$adminActions = UserAccount::getActiveUserObj()->getAdminActions();
		$interface->assign('adminActions', $adminActions);
		$interface->assign('activeAdminSection', $this->getActiveAdminSection());
		$interface->assign('activeMenuOption', 'admin');
		$interface->assign('showContentAsFullWidth', true);
	}

	public function display($mainContentTemplate, $pageTitle, $sidebarTemplate = 'Admin/admin-sidebar.tpl', $translateTitle = true) {
		parent::display($mainContentTemplate, $pageTitle, $sidebarTemplate, $translateTitle);
	}

	abstract function canView(): bool;

	/** @noinspection PhpUnused */
	function getInitializationJs(): string {
		return 'return AspenDiscovery.CollectionSpotlights.updateSpotlightFields();';
	}

	abstract function getActiveAdminSection(): string;

}