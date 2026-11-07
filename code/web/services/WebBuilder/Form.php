<?php
require_once ROOT_DIR . '/recaptcha/recaptchalib.php';

class WebBuilder_Form extends Action {
	private $form;

	function launch() {
		global $interface;

		$id = strip_tags($_REQUEST['id']);
		$_SESSION['returnToId'] = $id;
		$_SESSION['returnToModule'] = 'WebBuilder';
		$_SESSION['returnToAction'] = 'Form';
		$interface->assign('id', $id);

		require_once ROOT_DIR . '/sys/WebBuilder/CustomForm.php';
		$this->form = new CustomForm();
		$this->form->id = $id;
		if (!$this->form->find(true)) {
			global $interface;
			$interface->assign('module', 'Error');
			$interface->assign('action', 'Handle404');
			require_once ROOT_DIR . "/services/Error/Handle404.php";
			$actionClass = new Error_Handle404();
			$actionClass->launch();
			die();
		}
		$interface->assign('title', $this->form->title);

		if ($this->form->requireHomeLibraryInterface) {
			$formUrl = '/WebBuilder/Form?id=' . rawurlencode($id);
			$homeLibrarySelectionComplete = !empty($_REQUEST['homeLibraryInterfaceSelected']);
			if (!UserAccount::isLoggedIn() && !$homeLibrarySelectionComplete) {
				require_once ROOT_DIR . '/services/MyAccount/SelectInterface.php';
				$_REQUEST['goToPath'] = $formUrl . '&homeLibraryInterfaceSelected=1';
				$interface->assign('module', 'MyAccount');
				$interface->assign('action', 'SelectInterface');
				$selectInterfaceAction = new MyAccount_SelectInterface();
				$selectInterfaceAction->selectInterfaceMessage = $this->form->requireHomeLibraryInterfaceMessage;
				$selectInterfaceAction->launch();
				return;
			} else {
				$user = UserAccount::getActiveUserObj();
				$homeLibrary = $user ? $user->getHomeLibrary() : null;
				$activeLibrary = Library::getActiveLibrary();
				if ($homeLibrary && $activeLibrary && $homeLibrary->libraryId != $activeLibrary->libraryId) {
					$interface->assign('homeLibraryMessage', translate([
						'text' => 'Your library card is associated with another library. Please use your home library\'s catalog to submit this form.',
						'isPublicFacing' => true,
					]));
					require_once ROOT_DIR . '/services/MyAccount/SelectInterface.php';
					$homeLibraryUrl = MyAccount_SelectInterface::getLibraryBaseUrl($homeLibrary) . $formUrl;
					$interface->assign('homeLibraryUrl', $homeLibraryUrl);
				}
			}
		}

		if (!UserAccount::isLoggedIn()) {
			if (!$this->form->requireLogin) {
				require_once ROOT_DIR . '/sys/Enrichment/RecaptchaSetting.php';
				$recaptcha = new RecaptchaSetting();
				if ($recaptcha->find(true) && !empty($recaptcha->publicKey)) {
					$captchaCode = recaptcha_get_html($recaptcha->publicKey, $this->form->id);
					$interface->assign('captcha', $captchaCode);
					$interface->assign('captchaKey', $recaptcha->publicKey);
				}
			} else {
				//Display a message that the user must be logged in
				require_once ROOT_DIR . '/services/MyAccount/Login.php';
				$myAccountAction = new MyAccount_Login();
				$interface->assign('followupModule', 'WebBuilder');
				$interface->assign('followupAction', 'Form');
				$myAccountAction->launch();
				exit();
			}
		}

		require_once ROOT_DIR . '/sys/Parsedown/AspenParsedown.php';
		$parsedown = AspenParsedown::instance();
		$parsedown->setBreaksEnabled(true);
		$introText = $parsedown->parse($this->form->introText);

		$interface->assign('introText', $introText);
		$interface->assign('contents', $this->form->getFormattedFields());

		$this->display('customForm.tpl', $this->form->title, '', false);
	}

	function getBreadcrumbs(): array {
		$breadcrumbs = [];
		$breadcrumbs[] = new Breadcrumb('/', 'Home');
		$breadcrumbs[] = new Breadcrumb('', $this->form->title, true);
		if (UserAccount::userHasPermission([
			'Administer All Custom Forms',
			'Administer Library Custom Forms',
		])) {
			$breadcrumbs[] = new Breadcrumb('/WebBuilder/CustomForms?id=' . $this->form->id . '&objectAction=edit', 'Edit', true);
		}
		return $breadcrumbs;
	}
}
