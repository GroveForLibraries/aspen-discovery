{strip}
	{if count($listSources) > 1}
		<div class="row">
			<div id="listSources">
				<h3 id="list-source-label" class="sidebar-label">{translate text="Entry Types" isPublicFacing=true}</h3>
				<div id="facet-accordion" class="accordion">
					<div class="facetList">
						<div id="facetDetails_listSource" class="facetDetails" role="region" aria-labelledby="list-source-label">
							{foreach from=$listSources item=source}
								<div class="facetValue">
									<label for="listSource_{$source.value|escapeCSS}">
										<input type="checkbox" {if !empty($source.isApplied)}checked{/if} name="listSource_{$source.value|escapeCSS}" id="listSource_{$source.value|escapeCSS}" onclick="document.location = '{if !empty($source.isApplied)}{$source.removalUrl|escape}{else}{$source.url|escape}{/if}';" onkeypress="document.location = '{if !empty($source.isApplied)}{$source.removalUrl|escape}{else}{$source.url|escape}{/if}';">
										&nbsp;{$source.display}{if $facetCountsToShow == 1 || ($facetCountsToShow == 2 && empty($source.countIsApproximate))}{if !empty($source.count)}&nbsp;({if !empty($source.countIsApproximate)}{/if}{$source.count|number_format}){/if}{/if}
									</label>
								</div>
							{/foreach}
						</div>
					</div>
				</div>
			</div>
		</div>
	{/if}

	{if !empty($showHideItemsNoLongerInCatalog)}
		<div class="row">
			<div id="hideNoLongerInCatalog">
				<h3 id="narrow-search-label" class="sidebar-label">{translate text="Other Filter Options" isPublicFacing=true}</h3>
				<div class="facetValue">
					<label for="hideItemsNoLongerInCatalog">
						<input type="checkbox" {if !empty($hideItemsNoLongerInCatalog)}checked{/if} {if !empty($hideItemsDisabled)}disabled{/if} name="hideItemsNoLongerInCatalog" id="hideItemsNoLongerInCatalog" onclick="document.location = '{$hideItemsToggleUrl|escape}';">
						&nbsp;{translate text="Hide Items No Longer in Catalog" isPublicFacing=true}
					</label>
					{if !empty($hideItemsDisabled)}
						<div class="help-block small">{translate text="Items no longer in the catalog are always hidden while filters are applied." isPublicFacing=true}</div>
					{/if}
				</div>
			</div>
		</div>
	{/if}

	{if !empty($sideFacetSet)}
		<div id="refineSearch">
			{* Narrow Results *}
			<div class="row">
				{include file="Search/Recommend/SideFacets.tpl"}
			</div>
		</div>
	{/if}
{/strip}
