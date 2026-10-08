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
		'disallow_reading_history_return_date_edits' => [
			'title' => 'Disallow reading history return date edits',
			'description' => 'Add setting to library table for disallowing editing of return dates in reading history.',
			'continueOnError' => false,
			'sql' => [
				"ALTER TABLE library ADD COLUMN disallowReturnDateEdits TINYINT(1) NOT NULL DEFAULT 0"
			]
		]

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
