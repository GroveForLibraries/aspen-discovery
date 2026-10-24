<?php
/** @noinspection SqlDialectInspection */

/** @noinspection PhpUnused */
function getUpdates26_11_00(): array {
	$now = time();

	return [
		/*'name' => [
			 'title' => '',
			 'description' => '',
			 'continueOnError' => false,
			 'sql' => [
				 ''
			 ]
		 ], //name*/

		//mark n

		//kirstien

		//kodi

		//yanjun

		//imani

		//galen

		//chloe
	
		//pedro

		//mark j

		//lucas

		//tomas

		// stephen
		'addRedirectToHomeLibrary' => [
			'title' => 'Add requireHomeLibraryInterface to web_builder_custom_form',
			'description' => 'Add a setting to enable redirecting patrons to their home library catalog for custom forms.',
			'continueOnError' => false,
			'sql' => [
			'ALTER TABLE web_builder_custom_form ADD COLUMN requireHomeLibraryInterface TINYINT(1) NOT NULL DEFAULT 0',
			]
		], //addRequireHomeLibraryInterface
		'addRequireHomeLibraryInterfaceMessage' => [
			'title' => 'Add requireHomeLibraryInterfaceMessage to web_builder_custom_form',
			'description' => 'Optional message when redirecting patrons to home library for custom form',
			'continueOnError' => false,
			'sql' => [
				'ALTER TABLE web_builder_custom_form ADD COLUMN requireHomeLibraryInterfaceMessage VARCHAR(500) NULL',
			]
		], //addRequireHomeLibraryInterfaceMessage

		//jacob - OpenFifth

		//kyle - ByWater

	];
}
