{strip}
	<div id="main-content">
		{if !empty($updateMessage)}
			<div class="alert {if !empty($updateMessageIsError)}alert-danger{else}alert-success{/if}">
				{$updateMessage}
			</div>
		{/if}
		<h1>{$SideLoadName}</h1>
		<div class="row">
			<div class="col-xs-12">
				<div class="btn-group">
					<a class="btn btn-default" href='/SideLoads/SideLoads?objectAction=list'><i class="fas fa-arrow-alt-circle-left" role="presentation"></i> {translate text="Return to List" isAdminFacing=true}</a>
				</div>
				<div class="btn-group">
					<a class="btn btn-default" href="/SideLoads/SideLoads?objectAction=edit&amp;id={$id}"><i class="fas fa-edit" role="presentation"></i> {translate text="Edit Side Load" isAdminFacing=true}</a>
				</div>
			</div>
		</div>
		{if !empty($additionalObjectActions)}
			<div class="row">
				<div class="col-xs-12">
					<div class="btn-group-sm">
						{foreach from=$additionalObjectActions item=action}
							{if $smarty.server.REQUEST_URI != $action.url}
								<a class="btn btn-default" href='{$action.url}'>{translate text=$action.text isAdminFacing=true}</a>
							{/if}
						{/foreach}
					</div>
				</div>
			</div>
		{/if}
		
		<div class="row" style="margin-top: 20px;">
			<div class="col-xs-12">
				<table class="table table-striped table-bordered">
			<tr>
				<th>{translate text="File Name" isAdminFacing=true}</th>
				<th>{translate text="Date" isAdminFacing=true}</th>
				<th>{translate text="Size (bytes)" isAdminFacing=true}</th>
				<th></th>
			</tr>
			{foreach from=$files key=file item=fileData}
				<tr id="file{$fileData.index}">
					<td><a href="/SideLoads/DownloadMarc?id={$id}&file={$file|urlencode}">{$file}</a></td>
					<td>{$fileData.date|date_format:"%D %T"}</td>
					<td>{$fileData.size|number_format}</td>
					<td><button type="button" class="btn btn-sm btn-danger" onclick="return AspenDiscovery.SideLoads.deleteMarc('{$id}', '{$file|escape:"javascript"}', {$fileData.index});">{translate text="Delete" isAdminFacing=true}</button></td>
				</tr>
			{foreachelse}
				<tr>
					<td>{translate text="No Marc Files Found" isAdminFacing=true}</td>
				</tr>
			{/foreach}
				</table>
				{if !$sideload->isReadOnly()}
					<a class="btn btn-primary" href="/SideLoads/UploadMarc?id={$id}">
						{translate text="Upload MARC File" isAdminFacing=true}
					</a>
				{/if}
			</div>
		</div>
	</div>
{/strip}
