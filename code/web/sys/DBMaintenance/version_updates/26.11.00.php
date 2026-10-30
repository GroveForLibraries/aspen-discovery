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
		'add_show_all_locations_lida' => [
			'title' => 'Add setting to show all locations in Aspen LiDA',
			'description' => 'Add setting to show all locations in Aspen LiDA instead of just the branches for the users library system.',
			'continueOnError' => false,
			'sql' => [
				"ALTER TABLE aspen_lida_general_settings ADD COLUMN showOnlyLocalBranches TINYINT(1) NOT NULL DEFAULT 1"
			]
		],
		//add_show_all_locations_lida

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

		//jacob - OpenFifth

		//kyle - ByWater

	];
}