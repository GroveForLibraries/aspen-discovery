{strip}
	<h2>{translate text="Your Year" isPublicFacing="true"}</h2>
	<div>
		{if !empty($yearInReviewSummary->totalCheckouts)}
			<div class="row">
				<div class="result-label col-xs-3">{translate text="Yearly Usage" isPublicFacing=true}</div>
				<div class="col-xs-9 result-value">
					{$yearInReviewSummary->totalCheckouts}
				</div>
			</div>
		{/if}
		{if !empty($yearInReviewSummary->yearlyCostSavings)}
			<div class="row">
				<div class="result-label col-xs-3">{translate text="Yearly Cost Savings" isPublicFacing=true}</div>
				<div class="col-xs-9 result-value">
					{$yearInReviewSummary->yearlyCostSavings}
				</div>
			</div>
		{/if}
		{if !empty($yearInReviewSummary->topMonth)}
			<div class="row">
				<div class="result-label col-xs-3">{translate text="Top Month" isPublicFacing=true}</div>
				<div class="col-xs-9 result-value">
					{$yearInReviewSummary->topMonth}
				</div>
			</div>
		{/if}
		{if !empty($yearInReviewSummary->averageCheckouts)}
			<div class="row">
				<div class="result-label col-xs-3">{translate text="Average Checkouts Per Month" isPublicFacing=true}</div>
				<div class="col-xs-9 result-value">
					{$yearInReviewSummary->averageCheckouts}
				</div>
			</div>
		{/if}
		{if !empty($yearInReviewSummary->topFormats)}
			<div class="row">
				<div class="result-label col-xs-3">{translate text="Top Formats" isPublicFacing=true}</div>
				<div class="col-xs-9 result-value">
					{if !empty($yearInReviewSummary->topFormat1)}
						<a href='/Search/Results?filter[]=format%3A"{$yearInReviewSummary->topFormat1}"'>{$yearInReviewSummary->topFormat1}</a>
					{/if}
					{if !empty($yearInReviewSummary->topFormat2)}
						{if !empty($yearInReviewSummary->topFormat3)} and {else}, {/if}
						<a href='/Search/Results?filter[]=format%3A"{$yearInReviewSummary->topFormat2}"'>{$yearInReviewSummary->topFormat2}</a>
					{/if}
					{if !empty($yearInReviewSummary->topFormat3)}
						&nbsp;and <a href='/Search/Results?filter[]=format%3A"{$yearInReviewSummary->topFormat3}"'>{$yearInReviewSummary->topFormat3}</a>
					{/if}
				</div>
			</div>
		{/if}
		{if !empty($yearInReviewSummary->topGenres)}
			<div class="row">
				<div class="result-label col-xs-3">{translate text="Top Genres" isPublicFacing=true}</div>
				<div class="col-xs-9 result-value">
					{if !empty($yearInReviewSummary->topGenre1)}
						<a href='/Search/Results?filter[]=genre_facet%3A"{$yearInReviewSummary->topGenre1}"'>{$yearInReviewSummary->topGenre1}</a>
					{/if}
					{if !empty($yearInReviewSummary->topGenre2)}
						{if !empty($yearInReviewSummary->topGenre3)} and {else}, {/if}
						<a href='/Search/Results?filter[]=genre_facet%3A"{$yearInReviewSummary->topGenre2}"'>{$yearInReviewSummary->topGenre2}</a>
					{/if}
					{if !empty($yearInReviewSummary->topGenre3)}
						&nbsp;and <a href='/Search/Results?filter[]=genre_facet%3A"{$yearInReviewSummary->topGenre3}"'>{$yearInReviewSummary->topGenre3}</a>
					{/if}
				</div>
			</div>
		{/if}
		{if !empty($yearInReviewSummary->topAuthor)}
			<div class="row">
				<div class="result-label col-xs-3">{translate text="Top Author" isPublicFacing=true}</div>
				<div class="col-xs-9 result-value">
					{if !empty($yearInReviewSummary->topAuthor)}
						<a href='/Search/Results?lookfor="{$yearInReviewSummary->topAuthor}"&searchIndex=Author'>{$yearInReviewSummary->topAuthor}</a>
					{/if}
				</div>
			</div>
		{/if}
		{if !empty($yearInReviewSummary->topSeries)}
			<div class="row">
				<div class="result-label col-xs-3">{translate text="Top Series" isPublicFacing=true}</div>
				<div class="col-xs-9 result-value">
					{if !empty($yearInReviewSummary->topSeries1)}
						<a href='/Search/Results?lookfor="{$yearInReviewSummary->topSeries1}"&searchIndex=Series'>{$yearInReviewSummary->topSeries1}</a>
					{/if}
					{if !empty($yearInReviewSummary->topSeries2)}
						{if !empty($yearInReviewSummary->topSeries2)} and {/if}
						<a href='/Search/Results?lookfor="{$yearInReviewSummary->topSeries2}"&searchIndex=Series'>{$yearInReviewSummary->topSeries2}</a>
					{/if}
				</div>
			</div>
		{/if}
	</div>
	{if (!empty($recommendations))}
		<div>
			<h2>{translate text="Recommended Titles" isPublicFacing="true"}</h2>
			{foreach from=$recommendations item=recommendation}
				{$recommendation}
			{/foreach}
		</div>
	{/if}
{/strip}