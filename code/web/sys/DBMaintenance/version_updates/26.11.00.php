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
		'user_list_hide_deleted_option' => [
			'title' => 'Add Library Setting for Hiding List Items No Longer Available',
			'description' => 'Adds a setting to hide items no longer available in user lists.',
			'continueOnError' => false,
			'sql' => [
				"ALTER TABLE library ADD COLUMN hideListItemsNoLongerInCatalog TINYINT(1) NOT NULL DEFAULT 0;"
			],
		],

		//yanjun

		//imani

		//galen

		//chloe
	
		//pedro

		//mark j

		//lucas

		//tomas

		// stephen

		//jacob - OpenFifth

		//kyle - ByWater

	];
}
