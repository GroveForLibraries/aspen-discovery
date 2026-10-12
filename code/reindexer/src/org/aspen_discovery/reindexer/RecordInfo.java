package org.aspen_discovery.reindexer;

import com.turning_leaf_technologies.dates.DateUtils;
import com.turning_leaf_technologies.indexing.GroupedWorkDisplaySettings;
import com.turning_leaf_technologies.indexing.Scope;
import org.apache.solr.common.SolrInputDocument;

import java.util.*;
import java.util.regex.Pattern;

public class RecordInfo {
	private long databaseId;
	private String source;
	private String subSource;
	private String recordIdentifier;

	//Formats exist at both the item and record level because
	//Various systems define them in both ways.
	private HashSet<String> formats = new HashSet<>();
	// A record can have multiple format categories if the items within it have different formats when
	// format is defined based on properties for the item rather than being defined at the bib record
	// When this happens,
	private HashSet<String> formatCategories = new HashSet<>();
	private long formatBoost = 1;

	private String edition;
	// The raw audience from the 521a
	private String audience;

	private HashSet<String> targetAudience = new HashSet<>();
	private HashSet<String> targetAudienceFull = new HashSet<>();
	private String primaryLanguage;
	protected Long languageBoost = 1L;
	protected Long languageBoostSpanish = 1L;
	private String publisher;
	private String publicationDate;
	private String placeOfPublication;
	private String physicalDescription;
	private Integer duration;
	private boolean isClosedCaptioned;

	private boolean hasParentRecord;
	private boolean hasChildRecord;
	private boolean notForLoan;

	private final ArrayList<ItemInfo> relatedItems = new ArrayList<>();

	public RecordInfo(String source, String recordIdentifier){
		this.source = source;
		this.recordIdentifier = recordIdentifier;
	}

	public String getSource(){
		return this.source;
	}

	void setSubSource(String subSource) {
		this.subSource = subSource;
	}

	public String getSubSource(){
		return this.subSource;
	}

	public long getFormatBoost() {
		return formatBoost;
	}

	public void setFormatBoost(long formatBoost) {
		if (formatBoost > this.formatBoost) {
			this.formatBoost = formatBoost;
		}
	}

	void setEdition(String edition) {
		this.edition = edition;
	}

	public String getEdition(){
		return edition;
	}

	void setAudience(String audience) {
		this.audience = audience;
	}

	public String getAudience() {
		return audience;
	}

	void addTargetAudiences(HashSet<String> targetAudiences) {
		this.targetAudience.addAll(targetAudiences);
	}

	void addTargetAudience(String targetAudiences) {
		this.targetAudience.add(targetAudiences);
	}

	public String getTargetAudience() {
		return String.join(", ", targetAudience);
	}

	void addTargetAudiencesFull(HashSet<String> targetAudiencesFull) {
		this.targetAudienceFull.addAll(targetAudiencesFull);
	}

	void addTargetAudienceFull(String targetAudiencesFull) {
		this.targetAudienceFull.add(targetAudiencesFull);
	}

	public String getTargetAudienceFull() {
		return String.join(", ", targetAudienceFull);
	}

	void setPrimaryLanguage(String primaryLanguage) {
		this.primaryLanguage = primaryLanguage;
	}

	String getPrimaryLanguage(){
		return primaryLanguage;
	}

	void setLanguageBoost(Long languageBoost) {
		if (languageBoost > this.languageBoost) {
			this.languageBoost = languageBoost;
		}
	}

	void setLanguageBoostSpanish(Long languageBoostSpanish) {
		if (languageBoostSpanish > this.languageBoostSpanish) {
			this.languageBoostSpanish = languageBoostSpanish;
		}
	}

	public void addLanguage(String language) {
		if (this.primaryLanguage == null) {
			this.setPrimaryLanguage(language);
		}
	}

	void setLanguages(HashSet<String> languages) {
		if (this.primaryLanguage == null) {
			setPrimaryLanguage(languages.iterator().next());
		}
	}

	void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public String getPublisher() {
		return this.publisher;
	}

	void setPlaceOfPublication(String placeOfPublication) {
		this.placeOfPublication = placeOfPublication;
	}

	public String getPlaceOfPublication() {
		return this.placeOfPublication;
	}

	void setPublicationDate(String publicationDate) {
		this.publicationDate = publicationDate;
	}

	public String getPublicationDate() {
		return this.publicationDate;
	}

	void setPhysicalDescription(String physicalDescription) {
		this.physicalDescription = physicalDescription;
	}

	void setDuration(Integer duration) {
		this.duration = duration;
	}

	ArrayList<ItemInfo> getRelatedItems() {
		return relatedItems;
	}

	void setRecordIdentifier(String source, String recordIdentifier) {
		this.source = source;
		this.recordIdentifier = recordIdentifier;
	}

	public String getRecordIdentifier() {
		return recordIdentifier;
	}

	private String recordDetails = null;
	String getDetails() {
		if (recordDetails == null) {
			//None of this changes by scope, so we can just form it once and then return the previous value
			recordDetails = this.getFullIdentifier() + "|" +
				getPrimaryFormat() + "|" +
				getPrimaryFormatCategory() + "|" +
				Util.getCleanDetailValue(edition) + "|" +
				Util.getCleanDetailValue(audience) + "|" +
				Util.getCleanDetailValue(primaryLanguage) + "|" +
				Util.getCleanDetailValue(publisher) + "|" +
				Util.getCleanDetailValue(placeOfPublication) + "|" +
				Util.getCleanDetailValue(publicationDate) + "|" +
				Util.getCleanDetailValue(physicalDescription)
			;
		}
		return recordDetails;
	}

	private String primaryFormat = null;
	private String primaryFormatCategory = null;
	String getPrimaryFormat() {
		if (primaryFormat == null){
			HashMap<String, Integer> relatedFormats = new HashMap<>();
			HashMap<String, String> formatToFormatCategory = new HashMap<>();
			for (ItemInfo curItem : relatedItems){
				if (curItem.getFormat() != null && !curItem.getFormat().isEmpty()) {
					if (relatedFormats.containsKey(curItem.getFormat())){
						relatedFormats.merge(curItem.getFormat(), 1, Integer::sum);
					}else{
						relatedFormats.put(curItem.getFormat(), relatedFormats.getOrDefault(curItem.getFormat(), 1));
					}
					formatToFormatCategory.put(curItem.getFormat(), curItem.getFormatCategory());
				}
			}

			HashMap.Entry<String, Integer> FormatCounter = null; //need to sort through both string and integer to compare things properly
			String mostUsedFormat = null; //Set most used format to null before iterating through the hashmap

			//for each entry set in relatedFormats
			for (HashMap.Entry<String, Integer> curItem : relatedFormats.entrySet())
			{
				//if current item format count is greater than FormatCounter, set this as mostUsedFormat
				if (FormatCounter == null || curItem.getValue().compareTo(FormatCounter.getValue()) > 0)
				{
					FormatCounter = curItem;
					mostUsedFormat = curItem.getKey(); //get and set the most used format from entrySet with getKey()
				}
			}

			if (mostUsedFormat == null){
				//If we have formats for the record, use that. We only get here if we have no item formats.
				if (!formats.isEmpty()) {
					primaryFormat = formats.iterator().next();
				}

				//This might not be correct if we have multiple formats since the format category could be different
				//for each.
				if (!formatCategories.isEmpty()){
					primaryFormatCategory = formatCategories.iterator().next();
				}
			}else{
				primaryFormat = mostUsedFormat;
				primaryFormatCategory = formatToFormatCategory.get(mostUsedFormat);
			}
		}

		return primaryFormat;
	}

	public String getPrimaryFormatCategory() {
		if (primaryFormatCategory == null) {
			HashMap<String, Integer> relatedFormatCategories = new HashMap<>();
			for (String formatCategory : formatCategories) {
				relatedFormatCategories.put(formatCategory, 1);
			}
			for (ItemInfo curItem : relatedItems) {
				if (curItem.getFormatCategory() != null) {
					relatedFormatCategories.put(curItem.getFormatCategory(), relatedFormatCategories.getOrDefault(curItem.getFormatCategory(), 0) + 1);
				}
			}
			int timesUsed = 0;
			String mostUsedFormat = null;
			for (String curFormat : relatedFormatCategories.keySet()) {
				if (relatedFormatCategories.get(curFormat) > timesUsed) {
					mostUsedFormat = curFormat;
					timesUsed = relatedFormatCategories.get(curFormat);
				}
			}
			primaryFormatCategory = Objects.requireNonNullElse(mostUsedFormat, "Unknown");
		}
		return primaryFormatCategory;
	}

	public void addItem(ItemInfo itemInfo) {
		relatedItems.add(itemInfo);
		itemInfo.setRecordInfo(this);
	}

	HashSet<String> getFormats() {
		return formats;
	}

	String getFirstFormat() {
		if (!formats.isEmpty()) {
			for (String format : formats) {
				return format;
			}
		}
		return null;
	}

	HashSet<String> getFormatCategories() {
		return formatCategories;
	}

	String getFirstFormatCategory() {
		if (!formatCategories.isEmpty()) {
			for (String formatCategory : formatCategories) {
				return formatCategory;
			}
		}
		return null;
	}

	int getNumCopiesOnOrder() {
		int numOrders = 0;
		for (ItemInfo curItem : relatedItems){
			if (curItem.isOrderItem()){
				numOrders += curItem.getNumCopies();
			}
		}
		return numOrders;
	}

	String getFullIdentifier() {
		String fullIdentifier;
		if (subSource != null && !subSource.isEmpty()){
			fullIdentifier = source + ":" + subSource + ":" + recordIdentifier;
		}else{
			fullIdentifier = source + ":" + recordIdentifier;
		}
		return fullIdentifier;
	}

	int getNumPrintCopies() {
		int numPrintCopies = 0;
		for (ItemInfo curItem : relatedItems){
			if (!curItem.isOrderItem() && !curItem.isEContent()){
				numPrintCopies += curItem.getNumCopies();
			}
		}
		return numPrintCopies;
	}

	int getNumEContentCopies() {
		int numEContentCopies = 0;
		for (ItemInfo curItem : relatedItems){
			if (curItem.isEContent()){
				numEContentCopies += curItem.getNumCopies();
			}
		}
		return numEContentCopies;
	}

	public int getNumVirtualItems() {
		int numVirtualItems = 0;
		for (ItemInfo curItem : relatedItems){
			if (curItem.isVirtual()){
				numVirtualItems++;
			}
		}
		return numVirtualItems;
	}

	HashSet<String> getAllEContentSources() {
		HashSet<String> values = new HashSet<>();
		for (ItemInfo curItem : relatedItems){
			values.add(curItem.geteContentSource());
		}
		return values;
	}

	HashSet<String> getAllCallNumbers(){
		HashSet<String> values = new HashSet<>();
		for (ItemInfo curItem : relatedItems){
			values.add(curItem.getCallNumber());
		}
		return values;
	}

	void addFormats(HashSet<String> translatedFormats) {
		this.formats.addAll(translatedFormats);
	}

	void addFormat(String translatedFormat){
		this.formats.add(translatedFormat);
	}

	void addFormatCategories(HashSet<String> translatedFormatCategories) {
		this.formatCategories.addAll(translatedFormatCategories);
	}

	void addFormatCategory(String translatedFormatCategory){
		this.formatCategories.add(translatedFormatCategory);
	}

	boolean hasItemFormats() {
		for (ItemInfo curItem : relatedItems){
			if (curItem.getFormat() != null){
				return true;
			}
		}
		return false;
	}

	void getAutoReindexTimes(HashSet<Long> autoReindexTimes) {
		for (ItemInfo curItem : relatedItems){
			if (curItem.getAutoReindexTime() != null){
				autoReindexTimes.add(curItem.getAutoReindexTime());
			}
		}
	}

	void copyFrom(RecordInfo recordInfo){
		//noinspection unchecked
		this.formats = (HashSet<String>) recordInfo.formats.clone();
		//noinspection unchecked
		this.formatCategories = (HashSet<String>)recordInfo.formatCategories.clone();
		this.formatBoost = recordInfo.formatBoost;
		this.edition = recordInfo.edition;
		this.audience = recordInfo.audience;
		//noinspection unchecked
		this.targetAudience = (HashSet<String>)recordInfo.targetAudience.clone();
		//noinspection unchecked
		this.targetAudienceFull = (HashSet<String>)recordInfo.targetAudienceFull.clone();
		this.primaryLanguage = recordInfo.primaryLanguage;
		this.publisher = recordInfo.publisher;
		this.placeOfPublication = recordInfo.placeOfPublication;
		this.publicationDate = recordInfo.publicationDate;
		this.physicalDescription = recordInfo.physicalDescription;
		this.duration = recordInfo.duration;
		this.isClosedCaptioned = recordInfo.isClosedCaptioned;
		for (ItemInfo itemInfo : recordInfo.relatedItems) {
			ItemInfo clonedItem = new ItemInfo();
			addItem(clonedItem);
			clonedItem.copyFrom(itemInfo);
		}
	}

	public String getPhysicalDescription() {
		return physicalDescription;
	}

	public Integer getDuration() {
		if (duration != null) {
			return duration;
		}
		return 0;
	}

	public boolean isClosedCaptioned() {
		return isClosedCaptioned;
	}

	public void setClosedCaptioned(boolean closedCaptioned) {
		isClosedCaptioned = closedCaptioned;
	}

	public boolean allItemsHaveFormats() {
		for (ItemInfo curItem : relatedItems){
			if (curItem.getFormat() == null){
				return false;
			}
		}
		return true;
	}

	public HashSet<String> getUniqueItemFormats() {
		HashSet<String> uniqueItemFormats = new HashSet<>();
		for (ItemInfo curItem : relatedItems){
			if (curItem.getFormat() != null){
				uniqueItemFormats.add(curItem.getFormat());
			}
		}
		return uniqueItemFormats;
	}

	public HashSet<String> getUniqueItemFormatsCategories() {
		HashSet<String> uniqueItemFormatCategories = new HashSet<>();
		for (ItemInfo curItem : relatedItems){
			if (curItem.getFormatCategory() != null){
				uniqueItemFormatCategories.add(curItem.getFormatCategory());
			}
		}
		return uniqueItemFormatCategories;
	}

	public boolean hasParentRecord() {
		return hasParentRecord;
	}

	public void setHasParentRecord(boolean hasParentRecord) {
		this.hasParentRecord = hasParentRecord;
	}

	public boolean hasChildRecord() {
		return hasChildRecord;
	}

	public void setHasChildRecord(boolean hasChildRecord) {
		this.hasChildRecord = hasChildRecord;
	}

	/**
	 * Checks if the record has a not-for-loan status; that is, it is not on-shelf or checked-out, where
	 * if it is checked-out, that means it is loanable.
	 *
	 * @return {@code true} if the item is on order, {@code false} otherwise.
	 */
	public boolean hasNotForLoanStatus() {
		return notForLoan;
	}

	/**
	 * Sets the on-order status of the record.
	 *
	 * @param notForLoan {@code true} to indicate the item is on order, {@code false} otherwise.
	 */
	public void setNotForLoan(boolean notForLoan) {
		this.notForLoan = notForLoan;
	}

	@SuppressWarnings("DuplicatedCode")
	public ArrayList<SolrInputDocument> getRecordScopeSolrDocuments(GroupedWorkIndexer groupedWorkIndexer, AbstractGroupedWorkSolr parentWork, Long daysAddedSincePubDate) {
		if (databaseId == -1) {
			//This did not save for some reason
			return null;
		}

		HashSet<String> relatedScopes = getRelatedScopes();
		if (relatedScopes.isEmpty()) {
			return null;
		}

		Map<RecordScopeData, RecordScopeDocumentGroup> groupedScopeDocuments = new HashMap<>();
		HashSet<String> formatsForRecord = getFormats();
		HashSet<String> formatCategoriesForRecord = getFormatCategories();

		HashSet<String> filteredTargetAudiences = groupedWorkIndexer.cleanTargetAudiences(targetAudience);
		HashSet<String> filteredTargetAudiencesFull = groupedWorkIndexer.cleanTargetAudiences(targetAudienceFull);
		if (parentWork.isDebugEnabled()) {parentWork.addDebugMessage("Final target audience for " + this.recordIdentifier + " is " + targetAudience, 1);}
		if (parentWork.isDebugEnabled()) {parentWork.addDebugMessage("Final full target audience " + this.recordIdentifier + " is " + targetAudienceFull, 1);}


		TreeSet<String> sortedRelatedScopes = new TreeSet<>(relatedScopes);

		for (String scopeName : sortedRelatedScopes) {
			Scope curScope = groupedWorkIndexer.getScopes().get(scopeName);
			if (curScope == null) {
				continue;
			}
			RecordScopeData scopeData = new RecordScopeData();
			AvailabilityToggleInfo availabilityToggleValuesForScope = new AvailabilityToggleInfo();

			String scopeFacetLabel = curScope.getFacetLabel();
			GroupedWorkDisplaySettings scopeDisplaySettings = curScope.getGroupedWorkDisplaySettings();

			HashSet<String> targetAudiencesForScopeByItem = new HashSet<>();

			for (ItemInfo curItem : relatedItems) {
				ScopingInfo scopingInfo = curItem.getScopingInfo().get(scopeName);
				if (scopingInfo == null) {
					continue;
				}
				String trimmedIType = curItem.getTrimmedIType();
				String itemFormat = curItem.getFormat();
				String itemFormatCategory = curItem.getFormatCategory();
				String itemTargetAudience = curItem.getTargetAudience();

				if (itemFormat != null && !formatsForRecord.contains(itemFormat)) {
					scopeData.additionalFormats.add(itemFormat);
				}
				if (itemFormatCategory != null && !formatCategoriesForRecord.contains(itemFormatCategory)) {
					scopeData.additionalFormatCategories.add(itemFormatCategory);
				}
				if (itemTargetAudience != null) {
					targetAudiencesForScopeByItem.add(itemTargetAudience);
				}

				boolean addAllOwningLocations = false;
				boolean addAllOwningLocationsToAvailableAt = false;
				boolean locallyOwned = scopingInfo.isLocallyOwned();
				boolean libraryOwned = scopingInfo.isLibraryOwned();
				boolean isAvailable = curItem.isAvailable();
				boolean isEContent = curItem.isEContent();

				if (isEContent) {
					String trimmedEContentSource = curItem.getTrimmedEContentSource();
					if (trimmedEContentSource == null) {
						trimmedEContentSource = "Unknown";
					}
					if (trimmedEContentSource.equals("overdrive")) {
						trimmedEContentSource = "Libby";
					}
					availabilityToggleValuesForScope.updateToggleValues(locallyOwned || libraryOwned, scopeDisplaySettings.isIncludeOnlineMaterialsInAvailableToggle() && isAvailable, isAvailable);
					scopeData.owningLibraries.add(trimmedEContentSource);
					if (isAvailable) {
						scopeData.availableAt.add(trimmedEContentSource);
					}
					scopeData.econtentSources.add(trimmedEContentSource);
				} else { //physical materials
					if (locallyOwned) {
						availabilityToggleValuesForScope.updateToggleValues(locallyOwned, isAvailable, false);
						if (isAvailable) {
							scopeData.availableAt.add(scopeFacetLabel);
						}
						scopeData.owningLocations.add(scopeFacetLabel);
						scopeData.owningLibraries.add(curScope.isLibraryScope() ? scopeFacetLabel : curScope.getLibraryScope().getFacetLabel());
						if (curScope.isIncludeAllLibraryBranchesInFacets()) {
							//Include other branches of this library that own the title within the owning locations
							//isIncludeAllLibraryBranchesInFacets is only a setting at the location level
							addAllOwningLocations = true;
						}
					}
					if (libraryOwned) {
						if (curScope.isLibraryScope() || (curScope.isLocationScope() && !scopeDisplaySettings.isBaseAvailabilityToggleOnLocalHoldingsOnly())) {
							availabilityToggleValuesForScope.updateToggleValues(libraryOwned, isAvailable, false);
						}
						if (isAvailable) {
							addAllOwningLocationsToAvailableAt = true;
						}
						scopeData.owningLibraries.add(scopeFacetLabel);
						addAllOwningLocations = true;
					}
					if (!locallyOwned && !libraryOwned && !scopeDisplaySettings.isBaseAvailabilityToggleOnLocalHoldingsOnly()) {
						availabilityToggleValuesForScope.updateToggleValues(false, isAvailable, false);
						if (isAvailable) {
							addAllOwningLocationsToAvailableAt = true;
						}
					}
					if (isAvailable && curScope.getAdditionalLocationsToShowAvailabilityForPattern() != null && curItem.getLocationCode() != null) {
						//We might include the item in the owning and availability facets if it matched the available locations
						if (curScope.getAdditionalLocationsToShowAvailabilityForPattern().matcher(curItem.getLocationCode()).matches()) {
							addAllOwningLocationsToAvailableAt = true;
						}
					}

					if (!curScope.isRestrictOwningLibraryAndLocationFacets() || curScope.isConsortialCatalog()) {
						scopeData.owningLibraries.addAll(curItem.getLibraryOwnedNames());
						addAllOwningLocations = true;
					}
				}

				if (addAllOwningLocations) {
					scopeData.owningLocations.addAll(curItem.getLocationOwnedNames());
				}
				if (addAllOwningLocationsToAvailableAt) {
					scopeData.availableAt.addAll(curItem.getLocationOwnedNames());
				}

				if (locallyOwned || libraryOwned || scopeDisplaySettings.isIncludeAllRecordsInShelvingFacets()) {
					String collection = curItem.getCollection();
					if (collection != null && !collection.isEmpty()) {
						scopeData.collections.add(collection);
					}
					String detailedLocation = curItem.getDetailedLocation();
					if (detailedLocation != null && !detailedLocation.isEmpty()) {
						scopeData.detailedLocations.add(detailedLocation);
					}
					String shelfLocation = curItem.getShelfLocation();
					if (shelfLocation != null && !shelfLocation.isEmpty() && (scopeDisplaySettings.includeEContentInShelvingLocations() || !isEContent)) {
						scopeData.shelfLocations.add(shelfLocation);
					}
				}

				if (isEContent || locallyOwned || libraryOwned || scopeDisplaySettings.isIncludeAllRecordsInDateAddedFacets()) {
					Long daysSinceAdded = curItem.getDaysSinceAdded(daysAddedSincePubDate);
					if (scopeData.daysSinceAdded == null || daysSinceAdded > scopeData.daysSinceAdded) {
						scopeData.daysSinceAdded = daysSinceAdded;
					}
				}

				if (locallyOwned || libraryOwned) {
					if (isAvailable) {
						if (scopeData.libraryBoost < groupedWorkIndexer.availableAtBoostValue) {
							scopeData.libraryBoost = groupedWorkIndexer.availableAtBoostValue;
						}
					} else {
						if (scopeData.libraryBoost < groupedWorkIndexer.ownedByBoostValue) {
							scopeData.libraryBoost = groupedWorkIndexer.ownedByBoostValue;
						}
					}
				}

				if (trimmedIType != null) {
					scopeData.itemTypes.add(trimmedIType);
				}

				if (locallyOwned || libraryOwned || !scopingInfo.getScope().isRestrictOwningLibraryAndLocationFacets()) {
					String callNumber = curItem.getCallNumber();
					if (callNumber != null) {
						scopeData.localCallNumbers.add(callNumber);
					}
				}
			} // End looping through items

			if (formatsForRecord.contains("eAudiobook") || scopeData.additionalFormats.contains("eAudiobook")) {
				if (!formatCategoriesForRecord.contains("eBook")) {
					scopeData.additionalFormatCategories.add("eBook");
				}
			}
			if (formatsForRecord.contains("CD + Book") || scopeData.additionalFormats.contains("CD + Book")) {
				if (!formatCategoriesForRecord.contains("Books")) {
					scopeData.additionalFormatCategories.add("Books");
				}
				if (!formatCategoriesForRecord.contains("Audio Books")) {
					scopeData.additionalFormatCategories.add("Audio Books");
				}
			}
			if (formatsForRecord.contains("VOX Books") || scopeData.additionalFormats.contains("VOX Books")) {
				if (!formatCategoriesForRecord.contains("Books")) {
					scopeData.additionalFormatCategories.add("Books");
				}
				if (!formatCategoriesForRecord.contains("Audio Books")) {
					scopeData.additionalFormatCategories.add("Audio Books");
				}
			}

			if (targetAudiencesForScopeByItem.isEmpty()) {
				scopeData.targetAudiences.addAll(filteredTargetAudiences);
				scopeData.targetAudiencesFull.addAll(filteredTargetAudiencesFull);
			}else{
				HashSet<String> cleanedAudiencesForScopeByItem = groupedWorkIndexer.cleanTargetAudiences(targetAudiencesForScopeByItem);
				scopeData.targetAudiences.addAll(cleanedAudiencesForScopeByItem);
				scopeData.targetAudiencesFull.addAll(cleanedAudiencesForScopeByItem);
			}

			scopeData.availabilityToggles.addAll(availabilityToggleValuesForScope.getValues());
			if (curScope.getLocationsToExcludeAvailabilityForPattern() != null) {
				Pattern locationsToExclude = curScope.getLocationsToExcludeAvailabilityForPattern();
				scopeData.availableAt.removeIf(location -> locationsToExclude.matcher(location).matches());
			}

			RecordScopeDocumentGroup documentGroup = groupedScopeDocuments.computeIfAbsent(
				scopeData,
				RecordScopeDocumentGroup::new
			);

			documentGroup.scopes.add(scopeName);
		}

		ArrayList<RecordScopeDocumentGroup> sortedDocumentGroups =
			new ArrayList<>(groupedScopeDocuments.values());

		sortedDocumentGroups.sort(
			(group1, group2) ->
				group1.scopes.first().compareTo(group2.scopes.first())
		);

		ArrayList<SolrInputDocument> recordSolrScopeDocuments =
			new ArrayList<>(sortedDocumentGroups.size());

		int scopeGroupNumber = 1;
		for (RecordScopeDocumentGroup documentGroup : sortedDocumentGroups) {
			recordSolrScopeDocuments.add(createRecordScopeSolrDocument(
				documentGroup.scopeData,
				documentGroup.scopes,
				scopeGroupNumber++,
				formatsForRecord,
				formatCategoriesForRecord
			));
		}

		if (parentWork.isDebugEnabled()) {
			parentWork.addDebugMessage(
				"Combined " + relatedScopes.size()
					+ " scope documents into "
					+ recordSolrScopeDocuments.size()
					+ " record_scoping documents for "
					+ recordIdentifier,
				1
			);
		}

		return recordSolrScopeDocuments;
	}

	private SolrInputDocument createRecordScopeSolrDocument(
		RecordScopeData scopeData,
		TreeSet<String> scopes,
		int scopeGroupNumber,
		Set<String> formatsForRecord,
		Set<String> formatCategoriesForRecord
	) {
		SolrInputDocument recordDoc = new SolrInputDocument();
		recordDoc.setField("id", "record_" + databaseId + "_" + scopeGroupNumber);
		recordDoc.setField("recordtype", "record_scoping");
		recordDoc.setField("scope", new ArrayList<>(scopes));

		HashSet<String> finalFormats = new HashSet<>(formatsForRecord);
		finalFormats.addAll(scopeData.additionalFormats);
		recordDoc.setField("format", finalFormats);

		HashSet<String> finalFormatCategories = new HashSet<>(formatCategoriesForRecord);
		finalFormatCategories.addAll(scopeData.additionalFormatCategories);
		recordDoc.setField("format_category", finalFormatCategories);

		recordDoc.setField("target_audience", scopeData.targetAudiences);
		recordDoc.setField("target_audience_full", scopeData.targetAudiencesFull);
		setFieldIfNotEmpty(recordDoc, "availability_toggle", scopeData.availabilityToggles);
		setFieldIfNotEmpty(recordDoc, "owning_library", scopeData.owningLibraries);
		setFieldIfNotEmpty(recordDoc, "owning_location", scopeData.owningLocations);
		setFieldIfNotEmpty(recordDoc, "available_at", scopeData.availableAt);
		setFieldIfNotEmpty(recordDoc, "econtent_source", scopeData.econtentSources);
		setFieldIfNotEmpty(recordDoc, "collection", scopeData.collections);
		setFieldIfNotEmpty(recordDoc, "detailed_location", scopeData.detailedLocations);
		setFieldIfNotEmpty(recordDoc, "shelf_location", scopeData.shelfLocations);
		setFieldIfNotEmpty(recordDoc, "itype", scopeData.itemTypes);
		setFieldIfNotEmpty(recordDoc, "local_callnumber", scopeData.localCallNumbers);

		if (scopeData.daysSinceAdded != null) {
			recordDoc.setField("local_days_since_added", scopeData.daysSinceAdded);
			recordDoc.setField("local_time_since_added", DateUtils.getTimeSinceAdded(scopeData.daysSinceAdded));
		}
		recordDoc.setField("lib_boost", scopeData.libraryBoost);

		return recordDoc;
	}

	private static void setFieldIfNotEmpty(
		SolrInputDocument document,
		String fieldName,
		Collection<String> values
	) {
		if (!values.isEmpty()) {
			document.setField(fieldName, values);
		}
	}


	public void setDatabaseId(long recordId) {
		this.databaseId = recordId;
	}

	public HashSet<String> getRelatedScopes() {
		HashSet<String> relatedScopes = new HashSet<>();
		for (ItemInfo itemInfo : relatedItems) {
			relatedScopes.addAll(itemInfo.getScopingInfo().keySet());
		}
		return relatedScopes;
	}

	/**
	 * The scope-dependent values used to build a record_scoping document.
	 *
	 * Instances are mutable while a scope is being processed, but must not be
	 * modified after being inserted into groupedScopeDocuments because they are
	 * used as HashMap keys.
	 */
	private static class RecordScopeData {
		private final HashSet<String> additionalFormats = new HashSet<>();
		private final HashSet<String> additionalFormatCategories = new HashSet<>();
		private final HashSet<String> targetAudiences = new HashSet<>();
		private final HashSet<String> targetAudiencesFull = new HashSet<>();
		private final HashSet<String> availabilityToggles = new HashSet<>();
		private final HashSet<String> owningLibraries = new HashSet<>();
		private final HashSet<String> owningLocations = new HashSet<>();
		private final HashSet<String> availableAt = new HashSet<>();
		private final HashSet<String> econtentSources = new HashSet<>();
		private final HashSet<String> collections = new HashSet<>();
		private final HashSet<String> detailedLocations = new HashSet<>();
		private final HashSet<String> shelfLocations = new HashSet<>();
		private final HashSet<String> itemTypes = new HashSet<>();
		private final HashSet<String> localCallNumbers = new HashSet<>();
		private Long daysSinceAdded;
		private int libraryBoost;

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof RecordScopeData)) {
				return false;
			}

			RecordScopeData other = (RecordScopeData)object;
			return libraryBoost == other.libraryBoost
				&& Objects.equals(additionalFormats, other.additionalFormats)
				&& Objects.equals(additionalFormatCategories, other.additionalFormatCategories)
				&& Objects.equals(targetAudiences, other.targetAudiences)
				&& Objects.equals(targetAudiencesFull, other.targetAudiencesFull)
				&& Objects.equals(availabilityToggles, other.availabilityToggles)
				&& Objects.equals(owningLibraries, other.owningLibraries)
				&& Objects.equals(owningLocations, other.owningLocations)
				&& Objects.equals(availableAt, other.availableAt)
				&& Objects.equals(econtentSources, other.econtentSources)
				&& Objects.equals(collections, other.collections)
				&& Objects.equals(detailedLocations, other.detailedLocations)
				&& Objects.equals(shelfLocations, other.shelfLocations)
				&& Objects.equals(itemTypes, other.itemTypes)
				&& Objects.equals(localCallNumbers, other.localCallNumbers)
				&& Objects.equals(daysSinceAdded, other.daysSinceAdded);
		}

		@Override
		public int hashCode() {
			int result = additionalFormats.hashCode();
			result = 31 * result + additionalFormatCategories.hashCode();
			result = 31 * result + targetAudiences.hashCode();
			result = 31 * result + targetAudiencesFull.hashCode();
			result = 31 * result + availabilityToggles.hashCode();
			result = 31 * result + owningLibraries.hashCode();
			result = 31 * result + owningLocations.hashCode();
			result = 31 * result + availableAt.hashCode();
			result = 31 * result + econtentSources.hashCode();
			result = 31 * result + collections.hashCode();
			result = 31 * result + detailedLocations.hashCode();
			result = 31 * result + shelfLocations.hashCode();
			result = 31 * result + itemTypes.hashCode();
			result = 31 * result + localCallNumbers.hashCode();
			result = 31 * result + Objects.hashCode(daysSinceAdded);
			result = 31 * result + libraryBoost;
			return result;
		}
	}

	/**
	 * A unique record_scoping data set and every scope that can share it.
	 */
	private static class RecordScopeDocumentGroup {
		private final RecordScopeData scopeData;
		private final TreeSet<String> scopes = new TreeSet<>();

		private RecordScopeDocumentGroup(RecordScopeData scopeData) {
			this.scopeData = scopeData;
		}
	}
}
