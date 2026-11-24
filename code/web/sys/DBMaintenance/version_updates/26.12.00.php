<?php
/** @noinspection SqlDialectInspection */

/** @noinspection PhpUnused */
function getUpdates26_12_00(): array {
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
		'event_field_set_field_weight' => [
			'title' => 'Add weight to event field set fields',
			'description' => 'Allow event fields in a set to be sorted',
			'continueOnError' => false,
			'sql' => [
				'ALTER TABLE event_field_set_field ADD COLUMN weight INT NOT NULL DEFAULT 0',
				'UPDATE event_field_set_field SET weight = id',
			],
		], //event_field_set_field_weight

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
