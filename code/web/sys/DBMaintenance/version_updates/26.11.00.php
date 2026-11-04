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
		], //web_resource_filter_settings
		'fields_to_suppress' => [
			'title' => 'Field Suppression',
			'description' => 'Adds column to indexing profile table for field suppression value.',
			'continueOnError' => false,
			'sql' => [
				"ALTER TABLE indexing_profiles ADD COLUMN fieldsToSuppress VARCHAR(600) NOT NULL DEFAULT ''"
			]
		], //fields_to_suppress
		'sierra_self_reg_staff_message' => [
			'title' => 'Self Registration Staff Message',
			'description' => 'Message to display to staff in Sierra ILS for self-registered patrons.',
			'continueOnError' => false,
			'sql' => [
				"ALTER TABLE self_registration_form_sierra ADD COLUMN selfRegStaffMessage VARCHAR(250) NOT NULL DEFAULT ''"
			]
		], //sierra_self_reg_staff_message
		'user_list_hide_deleted_option' => [
			'title' => 'Add Library Setting for Hiding List Items No Longer Available',
			'description' => 'Adds a setting to hide items no longer available in user lists.',
			'continueOnError' => false,
			'sql' => [
				"ALTER TABLE library ADD COLUMN hideListItemsNoLongerInCatalog TINYINT(1) NOT NULL DEFAULT 0;"
			],
		], //user_list_hide_deleted_option

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
