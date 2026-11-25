{strip}
	{* User's viewing mode toggle switch *}
	<div class="row">{* browse styling replicated here *}
		<div class="col-xs-6">
			{if !empty($recordCount)}
				{* <span class="sidebar-label">
					   <label for="results-sort">{translate text='Sort'}</label></span> *}
				<label for="results-sort">{translate text='Sort by' isPublicFacing=true}</label>
				<select id="results-sort" name="sort" onchange="document.location.href = this.options[this.selectedIndex].value;" class="input-medium">
					{foreach from=$sortList item=sortData key=sortLabel}
						<option value="{$sortData.sortUrl|escape}"{if !empty($sortData.selected)} selected="selected"{/if}>{translate text=$sortData.desc isPublicFacing=true inAttribute=true}</option>
					{/foreach}
				</select>
			{/if}
		</div>
		<div class="col-xs-6">
			<div id="selected-browse-label">
				<div class="btn-group btn-group-sm">
					<button type="button" id="covers" title="Covers" class="btn btn-sm btn-default{if $displayMode == 'covers'} active{/if}" onclick="AspenDiscovery.Searches.toggleDisplayMode('covers')">
						<span class="thumbnail-icon" role="presentation"></span>
						<span> {translate text=Covers isPublicFacing=true}</span>
					</button>
					<button type="button" id="list" title="Lists" class="btn btn-sm btn-default{if $displayMode == 'list'} active{/if}" onclick="AspenDiscovery.Searches.toggleDisplayMode('list')">
						<span class="list-icon" role="presentation"></span>
						<span> {translate text=List isPublicFacing=true}</span>
					</button>
				</div>
				<div class="btn-group" id="hideSearchCoversSwitch"{if $displayMode != 'list'} style="display: none;"{/if}>
					<label for="hideCovers" class="checkbox{* control-label*}"> {translate text='Hide Covers' isPublicFacing=true}
						<input id="hideCovers" type="checkbox" onclick="AspenDiscovery.Account.toggleShowCovers(!$(this).is(':checked'))" {if $showCovers == false}checked="checked"{/if}>
					</label>
				</div>
			</div>
		</div>
	</div>
{/strip}