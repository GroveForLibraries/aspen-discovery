package com.turning_leaf_technologies.indexing;

import com.turning_leaf_technologies.marc.MarcUtil;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Inclusion rule for basic information that can be checked very quickly
 */
class InclusionRuleBasics {
	private final boolean includeHoldableOnly;
	private final boolean includeItemsOnOrder;
	private final boolean includeEContent;

	InclusionRuleBasics(boolean includeHoldableOnly, boolean includeItemsOnOrder, boolean includeEContent){
		this.includeHoldableOnly = includeHoldableOnly;
		this.includeItemsOnOrder = includeItemsOnOrder;
		this.includeEContent = includeEContent;
	}

	//TODO: We can potentially just pass in the ItemInfo object instead of all or most of these parameters
	//		This would likely require creating an interface for ItemInfo under java_shared_libraries.
	boolean isItemIncluded(String itemIdentifier, boolean isHoldable, boolean isOnOrder, boolean isEContent, DebugLogger debugLogger){
		if (!isEContent && (includeHoldableOnly && !isHoldable)) {
			if (debugLogger != null && debugLogger.isDebugEnabled()) {
				debugLogger.addDebugMessage("[scoping]Item " + itemIdentifier + " excluded from scope because 'Include Holdable Only' is enabled but item is not holdable", 3);
			}
			return false;
		} else if (!includeItemsOnOrder && isOnOrder){
			if (debugLogger != null && debugLogger.isDebugEnabled()) {
				debugLogger.addDebugMessage("[scoping]Item " + itemIdentifier + " excluded from scope because 'Include Items On Order' is disabled but item is on order", 3);
			}
			return  false;
		} else if (!includeEContent && isEContent){
			if (debugLogger != null && debugLogger.isDebugEnabled()) {
				debugLogger.addDebugMessage("[scoping]Item " + itemIdentifier + " excluded from scope because 'Include eContent' is disabled but item is eContent", 3);
			}
			return  false;
		}
		return true;
	}
}
