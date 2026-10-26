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
		'web_resource_filter_settings' => [
			'title' => 'Web Resource Page Filters',
			'description' => 'Add column for storing if a web resource page has filter options.',
			'continueOnError' => false,
			'sql' => [
				"ALTER TABLE web_builder_web_resources_settings ADD COLUMN showFilterOptionsListing TINYINT(1) NOT NULL DEFAULT 0",
				"ALTER TABLE web_builder_web_resources_settings ADD COLUMN showFilterOptionsAtoZ TINYINT(1) NOT NULL DEFAULT 0",
			],
		],
		//web_resource_filter_settings

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
