package org.aspen_discovery.reindexer;

import com.turning_leaf_technologies.dates.DateUtils;
import com.turning_leaf_technologies.logging.BaseIndexingLogEntry;
import com.turning_leaf_technologies.strings.AspenStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;

import java.util.*;

/**
 * Represents a grouped work withing Solr
 *
 * Compatible with the grouped_work_v3 schema
 *
 * Utilizes nested documents for better faceting
 */
public class GroupedWorkSolr3 extends AbstractGroupedWorkSolr implements Cloneable {
	public GroupedWorkSolr3(GroupedWorkIndexer groupedWorkIndexer, Logger logger) {
		super(groupedWorkIndexer, logger);
	}

	public GroupedWorkSolr3 clone() throws CloneNotSupportedException {
		GroupedWorkSolr3 clonedWork = (GroupedWorkSolr3) super.clone();
		super.cloneCollectionData(clonedWork);

		return clonedWork;
	}

	void addTargetAudiences(HashSet<String> target_audiences, RecordInfo recordInfo) {
		recordInfo.addTargetAudiences(target_audiences);
	}

	void addTargetAudience(String target_audience, RecordInfo recordInfo) {
		recordInfo.addTargetAudience(target_audience);
	}

	void addTargetAudienceFull(String target_audience_full, RecordInfo recordInfo) {
		recordInfo.addTargetAudienceFull(target_audience_full);
	}

	void addTargetAudiencesFull(HashSet<String> target_audiences_full, RecordInfo recordInfo) {
		recordInfo.addTargetAudiencesFull(target_audiences_full);
	}

	/**
	 * Create a solr document to store the majority of the grouped work data.
	 * There is also a child document that contains information about the scoped records which allows filtering by
	 *
	 *
	 * @param logEntry The log entry used for logging
	 * @return The fully built out document
	 */
	@SuppressWarnings("DuplicatedCode")
	SolrInputDocument getSolrDocument(BaseIndexingLogEntry logEntry) {
		SolrInputDocument groupedWorkDoc = new SolrInputDocument();
		Date indexingDate = new Date();

		Long daysAddedSincePubDate = null;
		if (earliestPublicationDate != null) {
			Calendar publicationDate = GregorianCalendar.getInstance();
			publicationDate.set(earliestPublicationDate.intValue(), Calendar.JANUARY, 1);
			daysAddedSincePubDate = DateUtils.getDaysSinceAddedForDate(publicationDate.getTime());
		}

		int numItems = 0;
		boolean allItemsOnOrder = true;
		boolean allItemsInProcess = true;
		boolean allItemsUnderConsideration = true;
		HashSet<String> uniqueFormatCategories = new HashSet<>();
		HashSet<String> uniqueFormats = new HashSet<>();
		HashSet<String> eContentSources = new HashSet<>();
		HashSet<String> callNumbers = new HashSet<>();
		ArrayList<SolrInputDocument> allScopingDocuments = new ArrayList<>();
		Map<String, RecordInfo.ParentScopeData> parentScopeDataByName = new HashMap<>();

		// Build all item-derived parent values and child documents together. This
		// prevents the parent builder and each helper from rescanning every item.
		for (RecordInfo record : relatedRecords.values()) {
			try {
				RecordInfo.RecordScopeBuildResult buildResult = record.buildRecordScopeDocuments(
					groupedWorkIndexer,
					this,
					daysAddedSincePubDate,
					parentScopeDataByName
				);
				RecordInfo.RecordItemSummary summary = buildResult.getItemSummary();
				if (summary.getItemCount() > 0) {
					numItems += summary.getItemCount();
					allItemsOnOrder &= summary.areAllItemsOnOrder();
					allItemsInProcess &= summary.areAllItemsInProcess();
					allItemsUnderConsideration &= summary.areAllItemsUnderConsideration();
				}
				uniqueFormats.addAll(summary.getItemFormats());
				uniqueFormatCategories.addAll(summary.getItemFormatCategories());
				uniqueFormats.addAll(record.getFormats());
				uniqueFormatCategories.addAll(record.getFormatCategories());
				eContentSources.addAll(summary.getEContentSources());
				callNumbers.addAll(summary.getCallNumbers());
				allScopingDocuments.addAll(buildResult.getDocuments());
			} catch (Exception e) {
				logEntry.incErrors("Error building scoped documents for grouped work " + id, e);
			}
		}

		if (numItems == 0) {
			allItemsOnOrder = false;
			allItemsInProcess = false;
			allItemsUnderConsideration = false;
		}
		//Main identification

		groupedWorkDoc.addField("id", id);
		groupedWorkDoc.addField("last_indexed", indexingDate);
		groupedWorkDoc.addField("alternate_ids", alternateIds);
		groupedWorkDoc.addField("recordtype", "grouped_work");
		try {
			//Title and variations
			String fullTitle = title;
			if (subTitle != null) {
				fullTitle += " " + subTitle;
			}
			groupedWorkDoc.addField("title", fullTitle);
			groupedWorkDoc.addField("title_display", displayTitle);
			//This is set lower now with additional titles added with formats
			//doc.addField("title_full", fullTitles);
			HashSet<String> startOfTitle = new HashSet<>();
			startOfTitle.add(fullTitle);
			String sortableTitle = AspenStringUtils.makeValueSortable(fullTitle);
			startOfTitle.add(sortableTitle);
			startOfTitle.add(titleSort);
			groupedWorkDoc.addField("title_left", startOfTitle);

			groupedWorkDoc.addField("subtitle_display", subTitle);
			groupedWorkDoc.addField("title_short", title);
			groupedWorkDoc.addField("title_sort", titleSort);
			groupedWorkDoc.addField("title_alt", titleAlt);
			groupedWorkDoc.addField("title_new", titleNew);

			//author and variations
			String primaryAuthor = getPrimaryAuthor();
			groupedWorkDoc.addField("auth_author", authAuthor);
			groupedWorkDoc.addField("author", primaryAuthor);
			if (primaryAuthor != null && !primaryAuthor.isEmpty()){ //skip if empty so titles with no author are sorted last
				groupedWorkDoc.addField("author_sort", primaryAuthor.toLowerCase(Locale.ROOT));
			}

			groupedWorkDoc.addField("auth_author2", authAuthor2);
			groupedWorkDoc.addField("author2", author2);
			groupedWorkDoc.addField("author2-role", author2Role);
			groupedWorkDoc.addField("author_additional", authorAdditional);
			groupedWorkDoc.addField("author_display", authorDisplay);

			//title auth
			HashSet<String> titleAuthors = new HashSet<>();
			titleAuthors.add(fullTitle + " " + primaryAuthor);
			groupedWorkDoc.addField("title_author", titleAuthors);

			//format related items at grouped work level
			groupedWorkDoc.addField("grouping_category", groupingCategory);
			groupedWorkDoc.addField("format_boost", getTotalFormatBoost());

			//language related fields
			//Check to see if we have Unknown plus a valid value
			HashSet<String> languagesForIndex = new HashSet<>(languages);
			if (languagesForIndex.size() > 1 || !groupedWorkIndexer.getTreatUnknownLanguageAs().isEmpty()) {
				languagesForIndex.remove("Unknown");
			}
			if (languagesForIndex.isEmpty() && !groupedWorkIndexer.getTreatUnknownLanguageAs().isEmpty()) {
				languagesForIndex.add(groupedWorkIndexer.getTreatUnknownLanguageAs());
			}
			groupedWorkDoc.addField("all_languages", languagesForIndex);
			groupedWorkDoc.addField("language", primaryLanguage);
			groupedWorkDoc.addField("translation", translations);
			groupedWorkDoc.addField("language_boost", languageBoost);
			groupedWorkDoc.addField("language_boost_es", languageBoostSpanish);
			//Publication related fields
			groupedWorkDoc.addField("publisher", publishers);
			groupedWorkDoc.addField("publishDate", publicationDates);
			groupedWorkDoc.addField("placeOfPublication", placesOfPublication);
			//Sorting will use the earliest date published
			groupedWorkDoc.addField("publishDateSort", earliestPublicationDate);

			//faceting and refined searching
			groupedWorkDoc.addField("physical", physicals);
			groupedWorkDoc.addField("duration", durations);
			groupedWorkDoc.addField("edition", editions);

			//Series fields
			ArrayList<SeriesInfo> sortedSeriesWithVolume = new ArrayList<>(series.values());
			sortedSeriesWithVolume.sort(
				Comparator.comparingInt(SeriesInfo::getPriorityScore).reversed()
			);
			if (isDebugEnabled() && !series.isEmpty()) {
				addDebugMessage("Series Priority Values", 1);
			}
			boolean isFirstSeries = true;
			for (SeriesInfo seriesInfo : sortedSeriesWithVolume) {
				if (isDebugEnabled()) {
					addDebugMessage(seriesInfo.getSeriesName() + " priority: " + seriesInfo.getVolumes(), 2);
				}
				groupedWorkDoc.addField("series", seriesInfo.getSeriesName());

				for (String volume : seriesInfo.getVolumes()) {
					groupedWorkDoc.addField("series_with_volume", seriesInfo.getSeriesName() + "|" + volume);
				}
				if (isFirstSeries) {
					groupedWorkDoc.addField("series_author", seriesInfo.getSeriesName() + " " + primaryAuthor);
					isFirstSeries = false;
				}
			}

			//Subject related fields
			groupedWorkDoc.addField("topic", topics);
			groupedWorkDoc.addField("topic_facet", withoutHiddenSubjects(topicFacets));
			groupedWorkDoc.addField("subject_facet", withoutHiddenSubjects(subjects));
			groupedWorkDoc.addField("lc_subject", lcSubjects);
			groupedWorkDoc.addField("bisac_subject", bisacSubjects);
			groupedWorkDoc.addField("genre", genres);
			groupedWorkDoc.addField("genre_facet", withoutHiddenSubjects(genreFacets));
			groupedWorkDoc.addField("geographic", geographic);
			groupedWorkDoc.addField("geographic_facet", withoutHiddenSubjects(geographicFacets));
			groupedWorkDoc.addField("personal_name_facet", withoutHiddenSubjects(personalNameSubjects));
			groupedWorkDoc.addField("corporate_name_facet", withoutHiddenSubjects(corporateNameSubjects));
			groupedWorkDoc.addField("era", withoutHiddenSubjects(eras));

			//Literary Forms
			checkDefaultValue(literaryFormFull, "Not Coded");
			checkDefaultValue(literaryFormFull, "Other");
			checkDefaultValue(literaryFormFull, "Unknown");
			if (this.isDebugEnabled()) {this.addDebugMessage("Total full literary form score: " + this.literaryFormFull, 1);}
			checkInconsistentLiteraryFormsFull();
			checkDefaultValue(literaryForm, "Not Coded");
			checkDefaultValue(literaryForm, "Other");
			checkDefaultValue(literaryForm, "Unknown");
			if (this.isDebugEnabled()) {this.addDebugMessage("Total fiction vs non fiction score: " + this.literaryForm, 1);}
			checkInconsistentLiteraryForms();
			//Check if .isHide
			if (groupedWorkIndexer.isHideUnknownLiteraryForm()) {
				literaryForm.remove("Unknown");
				literaryFormFull.remove("Unknown");
			}
			if (groupedWorkIndexer.isHideNotCodedLiteraryForm()) {
				literaryForm.remove("Not Coded");
				literaryFormFull.remove("Not Coded");
			}
			//Add field
			if (this.debugEnabled) {this.addDebugMessage("Literary form is " + literaryForm.keySet(), 2);}
			if (this.debugEnabled) {this.addDebugMessage("Full literary form is " + literaryFormFull.keySet(), 2);}
			groupedWorkDoc.addField("literary_form_full", literaryFormFull.keySet());
			groupedWorkDoc.addField("literary_form", literaryForm.keySet());

			//Target Audiences - In V3 these are stored at the record level

			//Date added to catalog
			Date dateAdded = getDateAdded();
			groupedWorkDoc.addField("date_added", dateAdded);

			// Item status and formats were collected while the scoped child
			// documents were built above.
			if (!uniqueFormatCategories.isEmpty()) {
				fullTitles.add(fullTitle + " " + StringUtils.join(uniqueFormatCategories, ", "));
				addKeywords(uniqueFormatCategories);
			}
			if (!uniqueFormats.isEmpty()) {
				fullTitles.add(fullTitle + " " + StringUtils.join(uniqueFormats, ", "));
				addKeywords(uniqueFormats);
			}
			groupedWorkDoc.addField("title_full", fullTitles);

			if (allItemsOnOrder) {
				addKeywords("On Order");
				addKeywords("Coming Soon");
				groupedWorkDoc.addField("days_since_added", -1);
				groupedWorkDoc.addField("time_since_added", "On Order");
			} else if (allItemsInProcess) {
				addKeywords("In Processing");
				groupedWorkDoc.addField("days_since_added", -2);
				groupedWorkDoc.addField("time_since_added", "In Processing");
			} else if (allItemsUnderConsideration) {
				addKeywords("Under Consideration");
				groupedWorkDoc.addField("days_since_added", Integer.MAX_VALUE);
				groupedWorkDoc.addField("time_since_added", "Under Consideration");
			} else {
				//Check to see if all items are either on order or
				if (dateAdded == null) {
					//Determine date added based on publication date
					if (earliestPublicationDate != null) {
						//Return number of days since the given year
						Calendar publicationDate = GregorianCalendar.getInstance();
						publicationDate.set(earliestPublicationDate.intValue(), Calendar.JANUARY, 1);

						long indexTime = indexingDate.getTime();
						long publicationTime = publicationDate.getTime().getTime();
						long bibDaysSinceAdded = (indexTime - publicationTime) / (long) (1000 * 60 * 60 * 24);
						groupedWorkDoc.addField("days_since_added", Long.toString(bibDaysSinceAdded));
						groupedWorkDoc.addField("time_since_added", DateUtils.getTimeSinceAddedForDate(publicationDate.getTime()));
					} else {
						groupedWorkDoc.addField("days_since_added", Long.toString(Integer.MAX_VALUE));
					}
				} else {
					groupedWorkDoc.addField("days_since_added", DateUtils.getDaysSinceAddedForDate(dateAdded));
					groupedWorkDoc.addField("time_since_added", DateUtils.getTimeSinceAddedForDate(dateAdded));
				}
			}

			//Awards and ratings
			groupedWorkDoc.addField("content_rating", contentRatings);
			groupedWorkDoc.addField("awards_facet", awards);
			if (lexileScore.isEmpty()) {
				groupedWorkDoc.addField("lexile_score", -1);
			} else {
				groupedWorkDoc.addField("lexile_score", lexileScore);
			}
			if (!lexileCode.isEmpty()) {
				groupedWorkDoc.addField("lexile_code", AspenStringUtils.trimTrailingPunctuation(lexileCode));
			}
			if (!fountasPinnell.isEmpty()) {
				groupedWorkDoc.addField("fountas_pinnell", fountasPinnell);
			}
			groupedWorkDoc.addField("accelerated_reader_interest_level", AspenStringUtils.trimTrailingPunctuation(acceleratedReaderInterestLevel));
			if (AspenStringUtils.isNumeric(acceleratedReaderReadingLevel)) {
				groupedWorkDoc.addField("accelerated_reader_reading_level", acceleratedReaderReadingLevel);
			}
			if (AspenStringUtils.isNumeric(acceleratedReaderPointValue)) {
				groupedWorkDoc.addField("accelerated_reader_point_value", acceleratedReaderPointValue);
			}

			// Add some special values to keywords
			keywords.addAll(eContentSources);

			keywords.addAll(isbns.keySet());
			keywords.addAll(oclcs);
			keywords.addAll(issns);
			keywords.addAll(lccns);
			keywords.addAll(upcs.keySet());

			keywords.addAll(callNumbers);
			groupedWorkDoc.addField("keywords", Util.getCRSeparatedStringFromSet(keywords));

			groupedWorkDoc.addField("table_of_contents", contents);
			//broad search terms
			//identifiers
			groupedWorkDoc.addField("lccn", lccns);
			groupedWorkDoc.addField("oclc", oclcs);
			//Get the primary isbn
			groupedWorkDoc.addField("primary_isbn", primaryIsbn);
			groupedWorkDoc.addField("isbn", isbns.keySet());
			groupedWorkDoc.addField("issn", issns);
			groupedWorkDoc.addField("primary_upc", getPrimaryUpc());
			groupedWorkDoc.addField("upc", upcs.keySet());

			//call numbers
			groupedWorkDoc.addField("callnumber-first", callNumberFirst);
			groupedWorkDoc.addField("callnumber-subject", callNumberSubject);
			//relevance determiners
			groupedWorkDoc.addField("popularity", Long.toString((long) popularity));
			groupedWorkDoc.addField("total_holds", Long.toString(totalHolds));
			groupedWorkDoc.addField("num_holdings", numHoldings);
			//aspen-discovery enrichment
			groupedWorkDoc.addField("rating", rating == -1f ? 2.5 : rating);
			groupedWorkDoc.addField("rating_facet", getRatingFacet(rating));

			//Links to users
			groupedWorkDoc.addField("user_rating_link", userRatingLink);
			groupedWorkDoc.addField("user_not_interested_link", userNotInterestedLink);
			groupedWorkDoc.addField("user_reading_history_link", userReadingHistoryLink);
			groupedWorkDoc.addField("list_link", listLink);
			for (Long listId : listEntryWeights.keySet()) {
				groupedWorkDoc.addField("list_entry_weight_" + listId, listEntryWeights.get(listId));
			}
			for (Long listId : listEntryDatesAdded.keySet()) {
				groupedWorkDoc.addField("list_entry_date_added_" + listId, listEntryDatesAdded.get(listId));
			}

			groupedWorkDoc.addField("description", Util.getCRSeparatedString(description));
			groupedWorkDoc.addField("display_description", displayDescription);
			//This possibly could move to the record, but that would also make the index significantly larger
			groupedWorkDoc.addField("ils_description", ilsDescription);

			for (Integer customFacetNumber : customFacetValues.keySet()) {
				groupedWorkDoc.addField("custom_facet_" + customFacetNumber, customFacetValues.get(customFacetNumber));
			}
		}catch (Exception e){
			logEntry.incErrors("Error creating solr document for grouped work " + id, e);
		}
		try{
			//Save information from scopes
			addScopedFieldsToDocument(groupedWorkDoc, logEntry);
		}catch (Exception e){
			logEntry.incErrors("Error adding scoped fields to grouped work " + id, e);
		}

		groupedWorkDoc.addField("related_scopes", relatedScopes.keySet());

		// These values were accumulated during the same traversal that created the
		// child scope documents. In particular, no early break is used after a Book
		// call number is found, so available-copy counts include every item.
		try {
			for (Map.Entry<String, RecordInfo.ParentScopeData> entry : parentScopeDataByName.entrySet()) {
				String scopeName = entry.getKey();
				RecordInfo.ParentScopeData scopeData = entry.getValue();
				String sortableCallNumberForScope = scopeData.getSortableCallNumber();
				if (sortableCallNumberForScope != null) {
					groupedWorkDoc.addField("callnumber_sort_" + scopeName, sortableCallNumberForScope);
					groupedWorkDoc.addField("callnumber_sort_lowercase_" + scopeName, sortableCallNumberForScope.toLowerCase(Locale.ROOT));
				}
				groupedWorkDoc.addField("available_copies_" + scopeName, scopeData.getAvailableCopies());
			}
		}catch (Exception e) {
			logEntry.incErrors("Error determining call number sort", e);
		}

		groupedWorkDoc.setField("record_scoping", allScopingDocuments);

		return groupedWorkDoc;
	}

	private HashSet<String> withoutHiddenSubjects(Collection<String> values) {
		HashSet<String> filteredValues = new HashSet<>(values);
		filteredValues.removeAll(groupedWorkIndexer.hideSubjects);
		return filteredValues;
	}

	@Override
	protected void addScopedFieldsToDocument(SolrInputDocument doc, BaseIndexingLogEntry logEntry) {
		//This is a no-op for GroupedWork Solr 3, instead it adds child documents
	}
}
