<?php
/** @noinspection SqlDialectInspection */

/** @noinspection PhpUnused */
function getUpdates26_10_00(): array {
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
		'add_oauth2_user_consents_table' => [
			'title' => 'Add OAuth2 User Consents Table',
			'description' => 'Stores remembered OAuth2 authorization consent per user and client.',
			'continueOnError' => false,
			'sql' => [
				"CREATE TABLE IF NOT EXISTS oauth2_user_consents (
					id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
					user_id INT NOT NULL,
					oauth2_client_id INT NOT NULL,
					scopes TEXT,
					dateCreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
					lastUpdated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
					UNIQUE KEY unique_user_client_consent (user_id, oauth2_client_id),
					INDEX idx_user_id (user_id),
					INDEX idx_oauth2_client_id (oauth2_client_id),
					FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
					FOREIGN KEY (oauth2_client_id) REFERENCES oauth2_clients(id) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
			],
		],
		//add_oauth2_user_consents_table

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


	];
}