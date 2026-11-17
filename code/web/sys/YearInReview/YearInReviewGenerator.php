<?php

function generateYearInReview(User $patron) : void {
	require_once ROOT_DIR . '/sys/YearInReview/UserYearInReview.php';
	require_once ROOT_DIR . '/sys/YearInReview/YearInReviewSetting.php';
	require_once ROOT_DIR . '/sys/LibraryLocation/LibraryYearInReview.php';

	if ($patron->hasIlsConnection()) {
		$userIsStaff = $patron->isStaff();

		global $library;
		$yearInReviewSetting  = new YearInReviewSetting();
		if ($userIsStaff) {
			$yearInReviewSetting->whereAdd('staffStartDate <= ' . time());
		}else{
			$yearInReviewSetting->whereAdd('patronStartDate <= ' . time());
		}
		$yearInReviewSetting->whereAdd('endDate >= ' . time());

		$libraryYearInReview = new LibraryYearInReview();
		$libraryYearInReview->libraryId = $library->libraryId;
		$yearInReviewSetting->joinAdd($libraryYearInReview, 'INNER', 'libraryYearInReview', 'id', 'yearInReviewId');

		if ($yearInReviewSetting->find(true)) {
			//We have valid settings
			$userYearInReview = new UserYearInReview();
			$userYearInReview->userId = $patron->id;
			$userYearInReview->settingId = $yearInReviewSetting->id;
			$reloadData = false;
			if (!$userYearInReview->find(true)) {
				$reloadData = true;
			}else{
				if (isset($_REQUEST['reload'])) {
					$reloadData = true;
					$userYearInReview->delete();
				}
			}

			if ($reloadData) {
				//We have not created year in review data for the user
				$readingHistorySize = $patron->getReadingHistorySizeForYear($yearInReviewSetting->year);

				if ($readingHistorySize >= 5) {
					$readingHistorySummary = $patron->getReadingHistorySummaryForYear($yearInReviewSetting->year);

					$userYearInReview->wrappedActive = true;
					$slidesToShow = [];
					//Calculate information for use in the display of Year in Review
					if ($yearInReviewSetting->year >= 2024) {
						$yearInReviewData = new stdClass();
						$yearInReviewData->userData = [];
						$yearInReviewData->activeStyle = $yearInReviewSetting->style;

						/** @noinspection PhpIfWithCommonPartsInspection */
						if (in_array($yearInReviewSetting->style, [0, 2, 3])) {
							//Always show the first slide (intro)
							$slidesToShow[] = 1;
						}else {
							//Always show the two slides (intro)
							$slidesToShow[] = 1;
							$slidesToShow[] = 2;
						}

						//Borrower Experience
						$yearInReviewData->userData['totalCheckouts'] = $readingHistorySummary->totalYearlyCheckouts;
						if (!empty($readingHistorySummary->yearlyCostSavings)) {
							$yearInReviewData->userData['yearlyCostSavings'] = $readingHistorySummary->yearlyCostSavings;
							$slidesToShow[] = match ($yearInReviewSetting->style) {
								0, 2, 3, 4 => 2,
								1 => 3,
							};
						}else{
							//Show the version with just the number of checkouts
							$slidesToShow[] = match ($yearInReviewSetting->style) {
								0, 2, 3 => 3,
								1 => 4
							};
						}

						//Hot Month / Busy Months
						$isSteadyUser = false;
						if ($readingHistorySummary->maxMonthlyCheckouts - $readingHistorySummary->averageCheckouts > 2) {
							foreach ($readingHistorySummary->monthlyCheckouts as $month => $monthlyCheckoutCount) {
								if ($readingHistorySummary->topMonth != $month) {
									if ($readingHistorySummary->maxMonthlyCheckouts - $monthlyCheckoutCount < 2) {
										$isSteadyUser = true;
									}
								}
							}
						}
						if (!$isSteadyUser) {
							$dateObj   = DateTime::createFromFormat('!m', $readingHistorySummary->topMonth);
							$monthName = $dateObj->format('F');
							$yearInReviewData->userData['topMonth'] = $monthName;
							$slidesToShow[] = match ($yearInReviewSetting->style) {
								0, 2, 3, 4 => 4,
								1 => 5,
							};
						}else{
							$yearInReviewData->userData['averageCheckouts'] = $readingHistorySummary->averageCheckouts;
							$slidesToShow[] = match ($yearInReviewSetting->style) {
								0, 2, 3, 4 => 5,
								1 => 6,
							};
						}

						//Top formats
						if (!empty($readingHistorySummary->topFormats) && count($readingHistorySummary->topFormats) >= 3) {
							$yearInReviewData->userData['topFormats'] = join("\n", $readingHistorySummary->topFormats);
							$formatNames = array_values($readingHistorySummary->topFormats);
							$yearInReviewData->userData['topFormat1'] = $formatNames[0];
							$yearInReviewData->userData['topFormat2'] = count($formatNames) > 1 ?  $formatNames[1] : '';
							$yearInReviewData->userData['topFormat3'] = count($formatNames) > 2 ?  $formatNames[2] : '';
							$slidesToShow[] = match ($yearInReviewSetting->style) {
								0, 2, 3, 4 => 6,
								1 => 7,
							};
						}

						//Top genres
						if (!empty($readingHistorySummary->topGenres)) {
							if (count($readingHistorySummary->topGenres) == 1){
								$yearInReviewData->userData['topGenres'] = $readingHistorySummary->topGenres[0];
							}elseif (count($readingHistorySummary->topGenres) == 2){
								$yearInReviewData->userData['topGenres'] = join("\n", [
									$readingHistorySummary->topGenres[0],
									'and',
									$readingHistorySummary->topGenres[1]
								]);
							}elseif(count($readingHistorySummary->topGenres) == 3){
								$yearInReviewData->userData['topGenres'] = join("\n", [
									$readingHistorySummary->topGenres[0] . ', ',
									$readingHistorySummary->topGenres[1]. ', ',
									'and',
									$readingHistorySummary->topGenres[2]
								]);
							}
							$genreNames = array_values($readingHistorySummary->topGenres);
							$yearInReviewData->userData['topGenre1'] = $genreNames[0];
							$yearInReviewData->userData['topGenre2'] = count($genreNames) > 1 ?  $genreNames[1] : '';
							$yearInReviewData->userData['topGenre3'] = count($genreNames) > 2 ?  $genreNames[2] : '';
							$slidesToShow[] = match ($yearInReviewSetting->style) {
								0, 2, 3, 4 => 7,
								1 => 8,
							};
						}

						//Top author
						if (!empty($readingHistorySummary->topAuthor)) {
							$yearInReviewData->userData['topAuthor'] = $readingHistorySummary->topAuthor;
							$slidesToShow[] = match ($yearInReviewSetting->style) {
								0, 2, 3, 4 => 8,
								1 => 9,
							};
						}

						//Top series
						if ($readingHistorySummary->topSeries) {
							$yearInReviewData->userData['topSeries'] = join("\nand\n", $readingHistorySummary->topSeries);
							$seriesNames = array_values($readingHistorySummary->topSeries);
							$yearInReviewData->userData['topSeries1'] = $seriesNames[0];
							$yearInReviewData->userData['topSeries2'] = count($seriesNames) > 1 ?  $seriesNames[1] : '';
							$slidesToShow[] = match ($yearInReviewSetting->style) {
								0, 2, 3,4 => 9,
								1 => 10,
							};
						}

						//Recommendations
						if ($readingHistorySummary->recommendations) {
							$yearInReviewData->userData['recommendations'] = join("\n\n", $readingHistorySummary->recommendations);
							$yearInReviewData->userData['recommendationIds'] = $readingHistorySummary->recommendationIds;
							$slidesToShow[] = match ($yearInReviewSetting->style) {
								0, 2, 3, 4 => 10,
								1 => 11,
							};
						}

						//Always show the last slide
						$slidesToShow[] = match ($yearInReviewSetting->style) {
							0, 2, 3, 4 => 11,
							1 => 12,
						};

						$yearInReviewData->numSlidesToShow = count($slidesToShow);
						$yearInReviewData->slidesToShow = $slidesToShow;
						$userYearInReview->wrappedResults = json_encode($yearInReviewData);
					}
				}else{
					$userYearInReview->wrappedActive = false;
					$userYearInReview->wrappedResults = '';
				}
				$userYearInReview->wrappedViewed = 0;

				$userYearInReview->insert();

				if ($userYearInReview->wrappedActive) {
					//Create a user message for the user
					require_once ROOT_DIR . '/sys/Account/UserMessage.php';
					//First check for and dismiss existing Year in Review messages
					$userMessage = new UserMessage();
					$userMessage->userId = UserAccount::getActiveUserId();
					$userMessage->messageType = 'yearInReview_' . $yearInReviewSetting->year;
					$userMessage->isDismissed = 0;
					$userMessage->find();
					while ($userMessage->fetch()) {
						$userMessage->isDismissed = 1;
						$userMessage->update();
					}
					$userMessage = new UserMessage();
					$userMessage->userId = UserAccount::getActiveUserId();
					$promoMessage = $yearInReviewSetting->getTextBlockTranslation('promoMessage', $patron->interfaceLanguage);
					// Remove paragraph tags if it's not multi-paragraph to improve button wrapping
					if (substr_count($promoMessage, "<p>") == 1) {
						$promoMessage = preg_replace('/(<p>)|(<\/p>)/', '', $promoMessage);
					}
					$userMessage->message = $promoMessage;
					$userMessage->messageType = 'yearInReview_' . $yearInReviewSetting->year;
					$userMessage->relatedObjectId = $userYearInReview->id;
					$userMessage->action1 = "return AspenDiscovery.Account.viewYearInReview(1)";
					$userMessage->action1Title = 'Yes';
					$userMessage->action2Title = 'Dismiss';
					$userMessage->isDismissed = 0;
					$userMessage->insert();
					$userMessage->action2 = "return AspenDiscovery.Account.dismissMessage({$userMessage->id})";
					$userMessage->update();
				}
			}
		}
	}
}
