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
		'closure_dates_for_calendar' => [
			'title' => 'Events Calendar Closure Dates',
			'description' => 'Add option for showing closure dates on the events calendar.',
			'sql' => [
				"ALTER TABLE calendar_display_settings ADD COLUMN showLibraryClosures TINYINT(1) DEFAULT 0"
			]
		], //closure_dates_for_calendar

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
