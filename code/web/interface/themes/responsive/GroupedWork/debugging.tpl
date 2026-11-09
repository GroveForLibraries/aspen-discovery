{strip}
	{if empty($debuggingInfo.processed)}
		<div>{translate text="Diagnostics will be available after the record is indexed next." isAdminFacing=true}</div>
	{else}
		<div>
			<i>Diagnostics performed at {$debuggingInfo.debugTime|date_format:"%D %T"}. Results will be reset after 24 hours.</i>
		</div>
		<br>
		<ul class="nav nav-pills" id="debugFilters" style="display: flex; flex-wrap: nowrap; overflow-x: auto;">
			<li class="active">
				<a href="#" onclick="return AspenDiscovery.GroupedWork.filterDebugging('all', this);">
					{translate text="All" isAdminFacing=true} <span class="badge">{$debugTotalLines}</span>
				</a>
			</li>
			{foreach from=$debugTagCounts key=tag item=count}
				<li>
					<a href="#" onclick="return AspenDiscovery.GroupedWork.filterDebugging('{$tag}', this);">
						{$debugTagLabels.$tag} <span class="badge">{$count}</span>
					</a>
				</li>
			{/foreach}
		</ul>
		<br>
		<div id="debugLines">
			{foreach from=$debugLines item=line}
				<div class="debug-line" data-tag="{$line.tag}">{$line.indent}{$line.text}</div>
			{/foreach}
		</div>
	{/if}
{/strip}
