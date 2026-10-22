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
		'fields_to_suppress' => [
			'title' => 'Field Suppression',
			'description' => 'Adds column to indexing profile table for field suppression value.',
			'continueOnError' => false,
			'sql' => [
				"ALTER TABLE indexing_profiles ADD COLUMN fieldsToSuppress VARCHAR(600) NOT NULL DEFAULT ''"
			]
		], //fields_to_suppress

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
