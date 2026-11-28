	<div class="holdsWithSelected{$sectionKey}">
		{if !empty($allowHoldsToBeGrouped)}
			<div class="alert alert-info">
				{translate text='"Group Selected Pending ILS Holds" requires two or more ILS Holds to be selected.' isPublicFacing=true}
			</div>
		{/if}
		{assign var="sectionLabel" value=""}
		{if $sectionKey == "unavailable"}
			{assign var="sectionLabel" value="Pending"}
		{elseif $sectionKey == "cancelled"}
			{assign var="sectionLabel" value="Cancelled"}
		{elseif $sectionKey == "interlibrary_loan"}
			{assign var="sectionLabel" value="ILL"}
		{else}
			{assign var="sectionLabel" value=""}
		{/if}
		{if $sectionKey != 'cancelled'}
			<form id="withSelectedHoldsFormBottom{$sectionKey}" action="{$fullPath}">
				<div class="btn-group">
					<button type="button" onclick="AspenDiscovery.Account.confirmCancelHoldSelected()" class="btn btn-sm btn-default btn-warning" aria-description="{translate text="Click here to cancel selected holds in the $sectionLabel section" isPublicFacing=true inAttribute=true}">{translate text="Cancel Selected $sectionLabel" isPublicFacing=true}</button>
					<button type="button" onclick="AspenDiscovery.Account.confirmCancelHoldAll()" class="btn btn-sm btn-default btn-warning" aria-description="{translate text="Click here to cancel all holds in the $sectionLabel section" isPublicFacing=true inAttribute=true}">{translate text="Cancel All $sectionLabel" isPublicFacing=true}</button>
					{if !empty($allowFreezeAllHolds)}
						<button type="button" onclick="AspenDiscovery.Account.confirmFreezeHoldSelected('', '', '', {if !empty($suspendRequiresReactivationDate)}true{else}false{/if})" class="btn btn-sm btn-default" aria-description="{translate text="Click here to freeze selected holds in the $sectionLabel section" isPublicFacing=true inAttribute=true}">{translate text="Freeze Selected $sectionLabel" isPublicFacing=true}</button>
						<button type="button" onclick="AspenDiscovery.Account.confirmFreezeHoldAll('{$userId}', {if !empty($suspendRequiresReactivationDate)}true{else}false{/if})" class="btn btn-sm btn-default" aria-description="{translate text="CLick here to freeze all holds in the $sectionLabel section" isPublicFacing=true inAttribute=true}">{translate text="Freeze All $sectionLabel" isPublicFacing=true}</button>
						<button type="button" onclick="AspenDiscovery.Account.confirmThawHoldSelected()" class="btn btn-sm btn-default" aria-description="{translate text="Click here to thaw selected holds in the $sectionLabel section" isPublicFacing=true inAttribute=true}">{translate text="Thaw Selected $sectionLabel" isPublicFacing=true}</button>
						<button type="button" onclick="AspenDiscovery.Account.confirmThawHoldAll('{$userId}')" class="btn btn-sm btn-default" aria-description="{translate text="Click here to thaw all holds in the $sectionLabel section" isPublicFacing=true inAttribute=true}">{translate text="Thaw All $sectionLabel" isPublicFacing=true}</button>
					{/if}
					{if $allowHoldsToBeGrouped}
						<button type="button" onclick="AspenDiscovery.Account.groupSelectedPendingHolds('{$source}', $('#unavailableHoldSort_{$source} option:selected').val());" class="btn btn-sm btn-default groupSelectedHoldsButton disabled" aria-description="{translate text="Click here to group selected pending ILS holds" isPublicFacing=true inAttribute=true}">{translate text="Group Selected Pending ILS Holds" isPublicFacing=true}</button>
					{/if}
					{if $allowSelectingHoldsToExport}
						<button type="button" onclick="return AspenDiscovery.Account.exportOnlySelectedHolds('{$source}', $('#{$sectionKey}HoldSort_{$source} option:selected').val()" class="btn btn-sm btn-default" aria-description="{translate text="Click here to export selected holds in the $sectionLabel section to CSV" isPublicFacing=true inAttribute=true}">{translate text="Export Selected $sectionLabel to CSV" isPublicFacing=true}</button>
					{/if}
					<button type="button" onclick="return AspenDiscovery.Account.exportHolds('{$source}', $('#{$sectionKey}HoldSort_{$source} option:selected').val());" class="btn btn-sm btn-default" aria-description="{translate text="Click here to export all holds in the $sectionLabel section to CSV" isPublicFacing=true inAttribute=true}">{translate text="Export All $sectionLabel to CSV" isPublicFacing=true}</button>
					<button type="button" class="btn btn-default btn-sm" onclick="return AspenDiscovery.Account.reloadHolds();" title="{translate text="Refresh" isPublicFacing=true inAttribute=true}">{translate text="Refresh" isPublicFacing=true} <i class="fas fa-sync-alt" role="presentation"></i></button>
				</div>
			</form>
		{elseif $showCancelled}
			<form id="withSelectedHoldsFormBottom{$sectionKey}" action="{$fullPath}">
				<div class="btn-group">
					<button type="button" onclick="return AspenDiscovery.Account.exportHolds('{$source}', $('#{$sectionKey}HoldSort_{$source} option:selected').val());" class="btn btn-sm btn-default" aria-description="{translate text="Click here to export all holds in the $sectionLabel section to CSV" isPublicFacing=true inAttribute=true}">{translate text="Export All $sectionLabel to CSV" isPublicFacing=true}</button>
				</div>
			</form>
		{/if}
	</div>
