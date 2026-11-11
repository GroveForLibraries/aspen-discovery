package com.turning_leaf_technologies.indexing;

import com.turning_leaf_technologies.marc.MarcUtil;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Inclusion Rule based on record type and location code only.
 * These are generally different for each library and location although there is some sharing.
 * By pulling this code out we can optimize checking before proceeding to details
 */
class InclusionRuleLocation {
	private final String recordType;
	private final boolean matchAllLocations;
	private boolean isLocationExactMatch;
	private String locationCodeToMatch;
	private Pattern locationCodePattern;
	private Pattern locationsToExcludePattern = null;
	private final String locationPatternString;

	private static final Pattern isRegexPattern = Pattern.compile("[.*?{}\\\\^\\[\\]|$]");
	InclusionRuleLocation(String recordType, String locationCode, String locationsToExclude){
		this.recordType = recordType;

		//Location & Sublocation Code Inclusion/Exclusion Check
		if (locationCode.isEmpty()){
			locationCode = ".*";
		}
		this.locationPatternString = locationCode;
		matchAllLocations = locationCode.equals(".*");
		if (!matchAllLocations){
			if (isRegexPattern.matcher(locationCode).find()) {
				this.locationCodePattern = Pattern.compile(locationCode, Pattern.CASE_INSENSITIVE);
			}else{
				this.locationCodeToMatch = locationCode;
				isLocationExactMatch = true;
			}
		}

		if (locationsToExclude != null && !locationsToExclude.isEmpty()){
			this.locationsToExcludePattern = Pattern.compile(locationsToExclude, Pattern.CASE_INSENSITIVE);
		}
	}

	//TODO: We can potentially just pass in the ItemInfo object instead of all or most of these parameters
	//		This would likely require creating an interface for ItemInfo under java_shared_libraries.
	boolean isItemIncluded(String itemIdentifier, String recordType, String locationCode, DebugLogger debugLogger){
		//Determine if we have already determined this already
		if (matchAllLocations){
			if (!recordType.equals(this.recordType)){
				if (debugLogger != null && debugLogger.isDebugEnabled()) {
					debugLogger.addDebugMessage("[scoping]Item " + itemIdentifier + " excluded from scope because record type '" + recordType + "' does not match rule record type '" + this.recordType + "'", 3);
				}
				return false;
			}else{
				return true;
			}
		}else{
			if (!this.recordType.equals(recordType)) {
				if (debugLogger != null && debugLogger.isDebugEnabled()) {
					debugLogger.addDebugMessage("[scoping]Item " + itemIdentifier + " excluded from scope because record type '" + recordType + "' does not match rule record type '" + this.recordType + "'", 3);
				}
				return false;
			}

			if (isLocationExactMatch) {
				if (!locationCodeToMatch.equalsIgnoreCase(locationCode)) {
					if (debugLogger != null && debugLogger.isDebugEnabled()) {
						debugLogger.addDebugMessage("[scoping]Item " + itemIdentifier + " excluded from scope because location '" + locationCode + "' does not match required location '" + locationCodeToMatch + "'", 3);
					}
					return false;
				}
			} else {
				if (!locationCodePattern.matcher(locationCode).matches()) {
					if (debugLogger != null && debugLogger.isDebugEnabled()) {
						debugLogger.addDebugMessage("[scoping]Item " + itemIdentifier + " excluded from scope because location '" + locationCode + "' does not match pattern '" + locationPatternString + "'", 3);
					}
					return false;
				}
			}
			if (!locationCode.isEmpty() && locationsToExcludePattern != null) {
				if (locationsToExcludePattern.matcher(locationCode).matches()) {
					if (debugLogger != null && debugLogger.isDebugEnabled()) {
						debugLogger.addDebugMessage("[scoping]Item " + itemIdentifier + " excluded from scope because location '" + locationCode + "' matches exclusion pattern", 3);
					}
					return false;
				}
			}

			return true;
		}
	}
}
