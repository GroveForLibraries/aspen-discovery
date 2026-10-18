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

	void addTargetAudiencesFull(HashSet<String> targetAudiencesFull) {
		this.targetAudienceFull.addAll(targetAudiencesFull);
	}

	void addTargetAudienceFull(String targetAudiencesFull) {
		this.targetAudienceFull.add(targetAudiencesFull);
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
			//None of these change by scope, so we can just form it once and then return the previous value
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
				String itemFormat = curItem.getFormat();
				if (itemFormat != null && !itemFormat.isEmpty()) {
					relatedFormats.merge(itemFormat, 1, Integer::sum);
					formatToFormatCategory.put(itemFormat, curItem.getFormatCategory());
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
			HashMap<String, Integer> relatedFormats = new HashMap<>();
			for (String format : formatCategories) {
				relatedFormats.merge(format, 1, Integer::sum);
			}
			for (ItemInfo curItem : relatedItems) {
				String itemFormatCategory = curItem.getFormatCategory();
				if (itemFormatCategory != null) {
					relatedFormats.merge(itemFormatCategory, 1, Integer::sum);
				}
			}
			int timesUsed = 0;
			String mostUsedFormat = null;
			for (String curFormat : relatedFormats.keySet()) {
				if (relatedFormats.get(curFormat) > timesUsed) {
					mostUsedFormat = curFormat;
					timesUsed = relatedFormats.get(curFormat);
				}
			}
			primaryFormatCategory = Objects.requireNonNullElse(mostUsedFormat, "Unknown");
		}
		return primaryFormatCategory;
	}

	public void addItem(ItemInfo itemInfo) {
		relatedItems.add(itemInfo);
		itemInfo.setRecordInfo(this);
		// These values are derived from the item collection.  Make sure they are
		// recalculated if an item is added after either getter has been called.
		primaryFormat = null;
		primaryFormatCategory = null;
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

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
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

	/**
	 * Builds the record-level summary, parent scope values, and merged child
	 * documents in one pass over the items and their actual scope associations.
	 */
	public RecordScopeBuildResult buildRecordScopeDocuments(
		GroupedWorkIndexer groupedWorkIndexer,
		AbstractGroupedWorkSolr parentWork,
		Long daysAddedSincePubDate,
		Map<String, ParentScopeData> parentScopeDataByName
	) {
		RecordItemSummary itemSummary = new RecordItemSummary();
		Map<String, OptimizedScopeData> scopeDataByName = new HashMap<>();
		Map<String, ScopeBuildContext> scopeContextByName = new HashMap<>();

		for (ItemInfo item : relatedItems) {
			PreparedItemData itemData = new PreparedItemData(item, daysAddedSincePubDate);
			itemSummary.add(item, itemData);

			if (databaseId == -1) {
				continue;
			}

			for (Map.Entry<String, ScopingInfo> scopeEntry : item.getScopingInfo().entrySet()) {
				String scopeName = scopeEntry.getKey();
				ScopeBuildContext scopeContext = scopeContextByName.get(scopeName);
				if (scopeContext == null) {
					Scope scope = groupedWorkIndexer.getScopes().get(scopeName);
					if (scope == null) {
						continue;
					}
					scopeContext = new ScopeBuildContext(scope);
					scopeContextByName.put(scopeName, scopeContext);
				}

				ScopingInfo scopingInfo = scopeEntry.getValue();
				OptimizedScopeData scopeData = scopeDataByName.computeIfAbsent(
					scopeName,
					ignored -> new OptimizedScopeData()
				);
				scopeData.add(itemData, scopingInfo, scopeContext, groupedWorkIndexer);

				ParentScopeData parentScopeData = parentScopeDataByName.computeIfAbsent(
					scopeName,
					ignored -> new ParentScopeData()
				);
				parentScopeData.add(itemData, scopingInfo);
			}
		}

		if (scopeDataByName.isEmpty()) {
			return new RecordScopeBuildResult(itemSummary, new ArrayList<>());
		}

		Set<String> recordFormats = getFormats();
		Set<String> recordFormatCategories = getFormatCategories();
		Set<String> filteredTargetAudiences = groupedWorkIndexer.cleanTargetAudiences(targetAudience);
		Set<String> filteredTargetAudiencesFull = groupedWorkIndexer.cleanTargetAudiences(targetAudienceFull);

		if (parentWork.isDebugEnabled()) {
			parentWork.addDebugMessage("Final target audience for " + recordIdentifier + " is " + targetAudience, 1);
			parentWork.addDebugMessage("Final full target audience " + recordIdentifier + " is " + targetAudienceFull, 1);
		}

		Map<OptimizedScopeData, OptimizedScopeDocumentGroup> groupedDocuments =
			new HashMap<>(mapCapacity(scopeDataByName.size()));

		for (Map.Entry<String, OptimizedScopeData> scopeEntry : scopeDataByName.entrySet()) {
			String scopeName = scopeEntry.getKey();
			OptimizedScopeData scopeData = scopeEntry.getValue();
			ScopeBuildContext scopeContext = scopeContextByName.get(scopeName);
			scopeData.finish(
				recordFormats,
				recordFormatCategories,
				filteredTargetAudiences,
				filteredTargetAudiencesFull,
				scopeContext,
				groupedWorkIndexer
			);

			OptimizedScopeDocumentGroup group = groupedDocuments.computeIfAbsent(
				scopeData,
				OptimizedScopeDocumentGroup::new
			);
			group.addScope(scopeName);
		}

		ArrayList<OptimizedScopeDocumentGroup> sortedGroups =
			new ArrayList<>(groupedDocuments.values());
		sortedGroups.sort(Comparator.comparing(OptimizedScopeDocumentGroup::getPrimaryScope));

		ArrayList<SolrInputDocument> documents = new ArrayList<>(sortedGroups.size());
		for (OptimizedScopeDocumentGroup group : sortedGroups) {
			documents.add(createOptimizedScopeDocument(
				group,
				recordFormats,
				recordFormatCategories
			));
		}

		if (parentWork.isDebugEnabled()) {
			parentWork.addDebugMessage(
				"Combined " + scopeDataByName.size() + " scope documents into "
					+ documents.size() + " record_scoping documents for " + recordIdentifier,
				1
			);
		}

		return new RecordScopeBuildResult(itemSummary, documents);
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

	private SolrInputDocument createOptimizedScopeDocument(
		OptimizedScopeDocumentGroup group,
		Set<String> recordFormats,
		Set<String> recordFormatCategories
	) {
		OptimizedScopeData data = group.scopeData;
		SolrInputDocument document = new SolrInputDocument();
		document.setField("id", "record_" + databaseId + "_" + group.primaryScope);
		document.setField("recordtype", "record_scoping");
		group.scopes.sort(String::compareTo);
		document.setField("scope", group.scopes);

		HashSet<String> finalFormats = new HashSet<>(recordFormats);
		finalFormats.addAll(data.additionalFormats);
		document.setField("format", finalFormats);
		HashSet<String> finalCategories = new HashSet<>(recordFormatCategories);
		finalCategories.addAll(data.additionalFormatCategories);
		document.setField("format_category", finalCategories);

		setFieldIfNotEmpty(document, "target_audience", data.targetAudiences);
		setFieldIfNotEmpty(document, "target_audience_full", data.targetAudiencesFull);
		setFieldIfNotEmpty(document, "availability_toggle", data.availabilityToggles);
		setFieldIfNotEmpty(document, "owning_library", data.owningLibraries);
		setFieldIfNotEmpty(document, "owning_location", data.owningLocations);
		setFieldIfNotEmpty(document, "available_at", data.availableAt);
		setFieldIfNotEmpty(document, "econtent_source", data.econtentSources);
		setFieldIfNotEmpty(document, "collection", data.collections);
		setFieldIfNotEmpty(document, "detailed_location", data.detailedLocations);
		setFieldIfNotEmpty(document, "shelf_location", data.shelfLocations);
		setFieldIfNotEmpty(document, "itype", data.itemTypes);
		setFieldIfNotEmpty(document, "local_callnumber", data.localCallNumbers);
		if (data.daysSinceAdded != null) {
			document.setField("local_days_since_added", data.daysSinceAdded);
			document.setField("local_time_since_added", DateUtils.getTimeSinceAdded(data.daysSinceAdded));
		}
		document.setField("lib_boost", data.libraryBoost);
		return document;
	}

	private static int mapCapacity(int expectedSize) {
		return Math.max(16, (int)Math.ceil(expectedSize / 0.75d) + 1);
	}

	private static Set<String> addValue(Set<String> values, String value) {
		if (value == null || value.isEmpty()) {
			return values;
		}
		if (values.isEmpty()) {
			HashSet<String> result = new HashSet<>(4);
			result.add(value);
			return result;
		}
		values.add(value);
		return values;
	}

	private static Set<String> addValues(Set<String> values, Collection<String> additions) {
		if (additions == null || additions.isEmpty()) {
			return values;
		}
		if (values.isEmpty()) {
			HashSet<String> result = new HashSet<>(mapCapacity(additions.size()));
			for (String addition : additions) {
				if (addition != null && !addition.isEmpty()) {
					result.add(addition);
				}
			}
			return result.isEmpty() ? Collections.emptySet() : result;
		}
		for (String addition : additions) {
			if (addition != null && !addition.isEmpty()) {
				values.add(addition);
			}
		}
		return values;
	}

	public static final class RecordScopeBuildResult {
		private final RecordItemSummary itemSummary;
		private final ArrayList<SolrInputDocument> documents;

		private RecordScopeBuildResult(RecordItemSummary itemSummary, ArrayList<SolrInputDocument> documents) {
			this.itemSummary = itemSummary;
			this.documents = documents;
		}

		public RecordItemSummary getItemSummary() {
			return itemSummary;
		}

		public ArrayList<SolrInputDocument> getDocuments() {
			return documents;
		}
	}

	public static final class RecordItemSummary {
		private int itemCount;
		private boolean allItemsOnOrder = true;
		private boolean allItemsInProcess = true;
		private boolean allItemsUnderConsideration = true;
		private final HashSet<String> itemFormats = new HashSet<>();
		private final HashSet<String> itemFormatCategories = new HashSet<>();
		private final HashSet<String> eContentSources = new HashSet<>();
		private final HashSet<String> callNumbers = new HashSet<>();

		private void add(ItemInfo item, PreparedItemData data) {
			itemCount++;
			String groupedStatus = item.getGroupedStatus();
			String detailedStatus = item.getDetailedStatus();
			allItemsOnOrder &= item.isOrderItem()
				|| "On Order".equals(groupedStatus)
				|| "On Order".equals(detailedStatus)
				|| "Coming Soon".equals(detailedStatus);
			allItemsInProcess &= "In Processing".equals(groupedStatus)
				|| "In-Process".equals(detailedStatus);
			allItemsUnderConsideration &= "Under Consideration".equals(groupedStatus)
				|| "Under Consideration".equals(detailedStatus);
			if (data.format != null) itemFormats.add(data.format);
			if (data.formatCategory != null) itemFormatCategories.add(data.formatCategory);
			if (data.eContentSource != null && !data.eContentSource.isEmpty()) eContentSources.add(data.eContentSource);
			if (data.callNumber != null && !data.callNumber.isEmpty()) callNumbers.add(data.callNumber);
		}

		public int getItemCount() { return itemCount; }
		public boolean areAllItemsOnOrder() { return itemCount > 0 && allItemsOnOrder; }
		public boolean areAllItemsInProcess() { return itemCount > 0 && allItemsInProcess; }
		public boolean areAllItemsUnderConsideration() { return itemCount > 0 && allItemsUnderConsideration; }
		public Set<String> getItemFormats() { return itemFormats; }
		public Set<String> getItemFormatCategories() { return itemFormatCategories; }
		public Set<String> getEContentSources() { return eContentSources; }
		public Set<String> getCallNumbers() { return callNumbers; }
	}

	public static final class ParentScopeData {
		private int availableCopies;
		private String sortableCallNumber;
		private int callNumberPriority;

		private void add(PreparedItemData item, ScopingInfo scopingInfo) {
			if (item.available) availableCopies++;
			if (item.sortableCallNumber == null || item.sortableCallNumber.isEmpty()) return;
			if (!(scopingInfo.isLocallyOwned() || scopingInfo.isLibraryOwned()
				|| !scopingInfo.getScope().isRestrictOwningLibraryAndLocationFacets())) return;

			int priority = 1;
			if ("book".equals(item.primaryFormatLowercase)) priority = 3;
			else if ("large print".equals(item.primaryFormatLowercase)) priority = 2;
			if (sortableCallNumber == null || priority > callNumberPriority) {
				sortableCallNumber = item.sortableCallNumber;
				callNumberPriority = priority;
			}
		}

		public int getAvailableCopies() { return availableCopies; }
		public String getSortableCallNumber() { return sortableCallNumber; }
	}

	private static final class PreparedItemData {
		private final ItemInfo item;
		private final Long fallbackDays;
		private final String format;
		private final String formatCategory;
		private final String targetAudience;
		private final String trimmedItemType;
		private final boolean available;
		private final boolean eContent;
		private final String eContentSource;
		private final String trimmedEContentSource;
		private final String locationCode;
		private final Collection<String> libraryOwnedNames;
		private final Collection<String> locationOwnedNames;
		private final String collection;
		private final String detailedLocation;
		private final String shelfLocation;
		private final String callNumber;
		private final String sortableCallNumber;
		private final String primaryFormatLowercase;
		private Long daysSinceAdded;
		private boolean daysCalculated;

		private PreparedItemData(ItemInfo item, Long fallbackDays) {
			this.item = item;
			this.fallbackDays = fallbackDays;
			format = item.getFormat();
			formatCategory = item.getFormatCategory();
			targetAudience = item.getTargetAudience();
			trimmedItemType = item.getTrimmedIType();
			available = item.isAvailable();
			eContent = item.isEContent();
			eContentSource = item.geteContentSource();
			String source = item.getTrimmedEContentSource();
			trimmedEContentSource = source == null ? "Unknown" : ("overdrive".equals(source) ? "Libby" : source);
			locationCode = item.getLocationCode();
			libraryOwnedNames = item.getLibraryOwnedNames();
			locationOwnedNames = item.getLocationOwnedNames();
			collection = item.getCollection();
			detailedLocation = item.getDetailedLocation();
			shelfLocation = item.getShelfLocation();
			callNumber = item.getCallNumber();
			sortableCallNumber = item.getSortableCallNumber();
			String primaryFormat = item.getPrimaryFormat();
			primaryFormatLowercase = primaryFormat == null ? null : primaryFormat.toLowerCase(Locale.ROOT);
		}

		private Long getDaysSinceAdded() {
			if (!daysCalculated) {
				daysSinceAdded = item.getDaysSinceAdded(fallbackDays);
				daysCalculated = true;
			}
			return daysSinceAdded;
		}
	}

	private static final class ScopeBuildContext {
		private final Scope scope;
		private final GroupedWorkDisplaySettings settings;
		private final String facetLabel;
		private final String libraryFacetLabel;
		private final Pattern additionalAvailabilityPattern;
		private final Pattern excludedAvailabilityPattern;

		private ScopeBuildContext(Scope scope) {
			this.scope = scope;
			settings = scope.getGroupedWorkDisplaySettings();
			facetLabel = scope.getFacetLabel();
			Scope libraryScope = scope.getLibraryScope();
			libraryFacetLabel = scope.isLibraryScope() || libraryScope == null
				? facetLabel
				: libraryScope.getFacetLabel();
			additionalAvailabilityPattern = scope.getAdditionalLocationsToShowAvailabilityForPattern();
			excludedAvailabilityPattern = scope.getLocationsToExcludeAvailabilityForPattern();
		}
	}

	private static final class OptimizedScopeData {
		private Set<String> additionalFormats = Collections.emptySet();
		private Set<String> additionalFormatCategories = Collections.emptySet();
		private Set<String> rawTargetAudiences = Collections.emptySet();
		private Set<String> targetAudiences = Collections.emptySet();
		private Set<String> targetAudiencesFull = Collections.emptySet();
		private Set<String> availabilityToggles = Collections.emptySet();
		private Set<String> owningLibraries = Collections.emptySet();
		private Set<String> owningLocations = Collections.emptySet();
		private Set<String> availableAt = Collections.emptySet();
		private Set<String> econtentSources = Collections.emptySet();
		private Set<String> collections = Collections.emptySet();
		private Set<String> detailedLocations = Collections.emptySet();
		private Set<String> shelfLocations = Collections.emptySet();
		private Set<String> itemTypes = Collections.emptySet();
		private Set<String> localCallNumbers = Collections.emptySet();
		private final AvailabilityToggleInfo toggleInfo = new AvailabilityToggleInfo();
		private Long daysSinceAdded;
		private int libraryBoost;

		private void add(PreparedItemData item, ScopingInfo info, ScopeBuildContext context,
						 GroupedWorkIndexer indexer) {
			additionalFormats = addValue(additionalFormats, item.format);
			additionalFormatCategories = addValue(additionalFormatCategories, item.formatCategory);
			rawTargetAudiences = addValue(rawTargetAudiences, item.targetAudience);

			boolean locallyOwned = info.isLocallyOwned();
			boolean libraryOwned = info.isLibraryOwned();
			boolean addAllOwningLocations = false;
			boolean addAllOwningLocationsToAvailableAt = false;

			if (item.eContent) {
				toggleInfo.updateToggleValues(
					locallyOwned || libraryOwned,
					context.settings.isIncludeOnlineMaterialsInAvailableToggle() && item.available,
					item.available
				);
				if (context.settings.isIncludeEContentInLibrarySystem()) {
					owningLibraries = addValue(owningLibraries, item.trimmedEContentSource);
				}
				if (item.available && context.settings.isIncludeEContentInAvailableAt()) {
					availableAt = addValue(availableAt, item.trimmedEContentSource);
				}
				econtentSources = addValue(econtentSources, item.trimmedEContentSource);
			} else {
				if (locallyOwned) {
					toggleInfo.updateToggleValues(true, item.available, false);
					if (item.available) availableAt = addValue(availableAt, context.facetLabel);
					owningLocations = addValue(owningLocations, context.facetLabel);
					owningLibraries = addValue(owningLibraries, context.libraryFacetLabel);
					addAllOwningLocations = context.scope.isIncludeAllLibraryBranchesInFacets();
				}
				if (libraryOwned) {
					if (context.scope.isLibraryScope() || (context.scope.isLocationScope()
						&& !context.settings.isBaseAvailabilityToggleOnLocalHoldingsOnly())) {
						toggleInfo.updateToggleValues(true, item.available, false);
					}
					if (item.available) addAllOwningLocationsToAvailableAt = true;
					owningLibraries = addValue(owningLibraries, context.facetLabel);
					addAllOwningLocations = true;
				}
				if (!locallyOwned && !libraryOwned
					&& !context.settings.isBaseAvailabilityToggleOnLocalHoldingsOnly()) {
					toggleInfo.updateToggleValues(false, item.available, false);
					if (item.available) addAllOwningLocationsToAvailableAt = true;
				}
				if (item.available && context.additionalAvailabilityPattern != null
					&& item.locationCode != null
					&& context.additionalAvailabilityPattern.matcher(item.locationCode).matches()) {
					addAllOwningLocationsToAvailableAt = true;
				}
				if (!context.scope.isRestrictOwningLibraryAndLocationFacets()
					|| context.scope.isConsortialCatalog()) {
					owningLibraries = addValues(owningLibraries, item.libraryOwnedNames);
					addAllOwningLocations = true;
				}
			}

			if (addAllOwningLocations) owningLocations = addValues(owningLocations, item.locationOwnedNames);
			if (addAllOwningLocationsToAvailableAt) availableAt = addValues(availableAt, item.locationOwnedNames);

			if (locallyOwned || libraryOwned || context.settings.isIncludeAllRecordsInShelvingFacets()) {
				collections = addValue(collections, item.collection);
				detailedLocations = addValue(detailedLocations, item.detailedLocation);
				if (context.settings.includeEContentInShelvingLocations() || !item.eContent) {
					shelfLocations = addValue(shelfLocations, item.shelfLocation);
				}
			}

			if (item.eContent || locallyOwned || libraryOwned
				|| context.settings.isIncludeAllRecordsInDateAddedFacets()) {
				Long itemDays = item.getDaysSinceAdded();
				if (itemDays != null && (daysSinceAdded == null || itemDays > daysSinceAdded)) {
					daysSinceAdded = itemDays;
				}
			}

			if (locallyOwned || libraryOwned) {
				int boost = item.available ? indexer.availableAtBoostValue : indexer.ownedByBoostValue;
				libraryBoost = Math.max(libraryBoost, boost);
			}
			itemTypes = addValue(itemTypes, item.trimmedItemType);
			if (locallyOwned || libraryOwned || !info.getScope().isRestrictOwningLibraryAndLocationFacets()) {
				localCallNumbers = addValue(localCallNumbers, item.callNumber);
			}
		}

		private void finish(Set<String> recordFormats, Set<String> recordCategories,
							Set<String> defaultAudiences, Set<String> defaultAudiencesFull,
							ScopeBuildContext context, GroupedWorkIndexer indexer) {
			if (!additionalFormats.isEmpty()) additionalFormats.removeAll(recordFormats);
			if (!additionalFormatCategories.isEmpty()) additionalFormatCategories.removeAll(recordCategories);

			if (recordFormats.contains("eAudiobook") || additionalFormats.contains("eAudiobook")) {
				if (!recordCategories.contains("eBook")) additionalFormatCategories = addValue(additionalFormatCategories, "eBook");
			}
			if (recordFormats.contains("CD + Book") || additionalFormats.contains("CD + Book")
				|| recordFormats.contains("VOX Books") || additionalFormats.contains("VOX Books")) {
				if (!recordCategories.contains("Books")) additionalFormatCategories = addValue(additionalFormatCategories, "Books");
				if (!recordCategories.contains("Audio Books")) additionalFormatCategories = addValue(additionalFormatCategories, "Audio Books");
			}

			if (rawTargetAudiences.isEmpty()) {
				targetAudiences = addValues(targetAudiences, defaultAudiences);
				targetAudiencesFull = addValues(targetAudiencesFull, defaultAudiencesFull);
			} else {
				Set<String> cleaned = indexer.cleanTargetAudiences(new HashSet<>(rawTargetAudiences));
				targetAudiences = addValues(targetAudiences, cleaned);
				targetAudiencesFull = addValues(targetAudiencesFull, cleaned);
			}
			rawTargetAudiences = Collections.emptySet();
			availabilityToggles = addValues(availabilityToggles, toggleInfo.getValues());
			if (context.excludedAvailabilityPattern != null && !availableAt.isEmpty()) {
				availableAt.removeIf(value -> context.excludedAvailabilityPattern.matcher(value).matches());
			}
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) return true;
			if (!(object instanceof OptimizedScopeData)) return false;
			OptimizedScopeData other = (OptimizedScopeData)object;
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
			return Objects.hash(additionalFormats, additionalFormatCategories, targetAudiences,
				targetAudiencesFull, availabilityToggles, owningLibraries, owningLocations,
				availableAt, econtentSources, collections, detailedLocations, shelfLocations,
				itemTypes, localCallNumbers, daysSinceAdded, libraryBoost);
		}
	}

	private static final class OptimizedScopeDocumentGroup {
		private final OptimizedScopeData scopeData;
		private final ArrayList<String> scopes = new ArrayList<>();
		private String primaryScope;

		private OptimizedScopeDocumentGroup(OptimizedScopeData scopeData) {
			this.scopeData = scopeData;
		}

		private void addScope(String scopeName) {
			scopes.add(scopeName);
			if (primaryScope == null || scopeName.compareTo(primaryScope) < 0) primaryScope = scopeName;
		}

		private String getPrimaryScope() { return primaryScope; }
	}

	public void setDatabaseId(long recordId) {
		this.databaseId = recordId;
	}
}
