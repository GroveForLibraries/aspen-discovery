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
		'add_event_highlight_color_options' => [
			'title' => 'Add Option to Set Highlight Colors On Event Type',
			'description' => 'Add ability to choose to add highlight color to events based on event type on calendar view.',
			'sql' => [
				"ALTER TABLE event_type ADD COLUMN eventTypeHighlightColor varchar(7) DEFAULT ''",
				"ALTER TABLE event_type ADD COLUMN eventTypeHighlightColorDefault TINYINT(1) DEFAULT 1",
				"ALTER TABLE event_type ADD COLUMN eventHighlightCustomizable TINYINT(1) DEFAULT 0",
				"ALTER TABLE event ADD COLUMN eventHighlightColor varchar(7) DEFAULT ''",
				"ALTER TABLE event ADD COLUMN eventHighlightColorDefault TINYINT(1) DEFAULT 1",
				"ALTER TABLE user_events_entry ADD COLUMN eventHighlightColor varchar(7) DEFAULT ''"
			]
		], //add_event_highlight_color_options

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
