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
		'add_enable_location_change_lida' => [
			'title' => 'Add setting to allow users to change their active location in Aspen LiDA',
			'description' => 'Add setting to allow users to change their active location in Aspen LiDA.',
			'continueOnError' => false,
			'sql' => [
				"ALTER TABLE aspen_lida_general_settings ADD COLUMN enableLocationChange TINYINT(1) NOT NULL DEFAULT 0"
			]
		],
		//add_enable_location_change_lida

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