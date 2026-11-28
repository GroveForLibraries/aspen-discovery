{strip}
    <button type="button" id="aspen-events-login-redirect" class="btn btn-primary" onclick="return AspenDiscovery.Account.ajaxLogin(null, function(){ AspenDiscovery.Account.regInfoModal(null, 'Events', '{$eventSourceId|escape}', '{$vendor|escape}', '{$regLink|escape}'); });">
    	{translate text="Login To Register" isPublicFacing=true}
    </button>
{/strip}
