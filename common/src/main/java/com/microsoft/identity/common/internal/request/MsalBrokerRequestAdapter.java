package com.microsoft.identity.common.internal.request;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.authorities.Environment;
import com.microsoft.identity.common.internal.broker.BrokerRequest;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.ui.browser.Browser;
import com.microsoft.identity.common.internal.ui.browser.BrowserSelector;
import com.microsoft.identity.common.internal.util.QueryParamsAdapter;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_HOME_ACCOUNT_ID;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_REDIRECT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.DEFAULT_BROWSER_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ENVIRONMENT;

public class MsalBrokerRequestAdapter implements IBrokerRequestAdapter {

    private static final String TAG = MsalBrokerRequestAdapter.class.getName();

    @Override
    public BrokerRequest brokerRequestFromAcquireTokenParameters(@NonNull final AcquireTokenOperationParameters parameters) {

        Logger.info(TAG, "Constructing result bundle from AcquireTokenOperationParameters.");

        final BrokerRequest brokerRequest = new BrokerRequest.Builder()
                .authority(parameters.getAuthority().getAuthorityURL().toString())
                .scope(TextUtils.join(" ", parameters.getScopes()))
                .redirect(getRedirectUri(parameters))
                .clientId(parameters.getClientId())
                .username(parameters.getLoginHint())
                .extraQueryStringParameter(
                        parameters.getExtraQueryStringParameters() != null ?
                                QueryParamsAdapter._toJson(parameters.getExtraQueryStringParameters())
                                : null
                ).prompt(parameters.getOpenIdConnectPromptParameter().name())
                .claims(parameters.getClaimsRequestJson())
                .forceRefresh(parameters.getForceRefresh())
                .correlationId(DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID))
                .applicationName(parameters.getApplicationName())
                .applicationVersion(parameters.getApplicationVersion())
                .msalVersion(parameters.getSdkVersion())
                .environment(AzureActiveDirectory.getEnvironment().name())
                .multipleCloudsSupported(getMultipleCloudsSupported(parameters))
                .build();

        return brokerRequest;
    }

    @Override
    public BrokerRequest brokerRequestFromSilentOperationParameters(@NonNull final AcquireTokenSilentOperationParameters parameters) {

        Logger.info(TAG, "Constructing result bundle from AcquireTokenSilentOperationParameters.");

        final BrokerRequest brokerRequest = new BrokerRequest.Builder()
                .authority(parameters.getAuthority().getAuthorityURL().toString())
                .scope(TextUtils.join(" ", parameters.getScopes()))
                .redirect(getRedirectUri(parameters))
                .clientId(parameters.getClientId())
                .homeAccountId(parameters.getAccount().getHomeAccountId())
                .localAccountId(parameters.getAccount().getLocalAccountId())
                .username(parameters.getAccount().getUsername())
                .claims(parameters.getClaimsRequestJson())
                .forceRefresh(parameters.getForceRefresh())
                .correlationId(DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID))
                .applicationName(parameters.getApplicationName())
                .applicationVersion(parameters.getApplicationVersion())
                .msalVersion(parameters.getSdkVersion())
                .environment(AzureActiveDirectory.getEnvironment().name())
                .multipleCloudsSupported(getMultipleCloudsSupported(parameters))
                .build();

        return brokerRequest;
    }

    @Override
    public BrokerAcquireTokenOperationParameters brokerInteractiveParametersFromActivity(
            @NonNull final Activity callingActivity) {

        Logger.info(TAG, "Constructing BrokerAcquireTokenOperationParameters from calling activity");

        final BrokerAcquireTokenOperationParameters parameters =
                new BrokerAcquireTokenOperationParameters();

        final Intent intent = callingActivity.getIntent();

        final BrokerRequest brokerRequest = new Gson().fromJson(
                intent.getStringExtra(AuthenticationConstants.Broker.BROKER_REQUEST_V2),
                BrokerRequest.class);

        parameters.setActivity(callingActivity);

        parameters.setAppContext(callingActivity.getApplicationContext());

        parameters.setSdkType(SdkType.MSAL);

        int callingAppUid = intent.getIntExtra(
                AuthenticationConstants.Broker.CALLER_INFO_UID, 0
        );
        parameters.setCallerUId(callingAppUid);

        parameters.setCallerPackageName(brokerRequest.getApplicationName());

        parameters.setCallerAppVersion(brokerRequest.getApplicationVersion());

        List<Pair<String, String>> extraQP = new ArrayList<>();

        if (!TextUtils.isEmpty(brokerRequest.getExtraQueryStringParameter())) {
            extraQP = QueryParamsAdapter._fromJson(brokerRequest.getExtraQueryStringParameter());
            parameters.setExtraQueryStringParameters(extraQP);
        }

        final AzureActiveDirectoryAuthority authority = AdalBrokerRequestAdapter.getRequestAuthorityWithExtraQP(
                brokerRequest.getAuthority(),
                extraQP
        );

        if (authority != null) {
            authority.setMultipleCloudsSupported(brokerRequest.getMultipleCloudsSupported());
            parameters.setAuthority(authority);
        }

        parameters.setScopes(getScopesAsSet(brokerRequest.getScope()));

        parameters.setClientId(brokerRequest.getClientId());

        parameters.setRedirectUri(brokerRequest.getRedirect());

        parameters.setLoginHint(brokerRequest.getUserName());

        String correlationIdString = brokerRequest.getCorrelationId();

        if (TextUtils.isEmpty(correlationIdString)) {
            UUID correlationId = UUID.randomUUID();
            correlationIdString = correlationId.toString();
        }
        parameters.setCorrelationId(correlationIdString);

        parameters.setClaimsRequest(brokerRequest.getClaims());

        parameters.setOpenIdConnectPromptParameter(
                OpenIdConnectPromptParameter.valueOf(brokerRequest.getPrompt())
        );

        parameters.setAuthorizationAgent(AuthorizationAgent.WEBVIEW);

        // Set Global environment variable for instance discovery if present
        if (!TextUtils.isEmpty(brokerRequest.getEnvironment())) {
            AzureActiveDirectory.setEnvironment(
                    Environment.valueOf(brokerRequest.getEnvironment())
            );
        }

        return parameters;

    }

    @Override
    public BrokerAcquireTokenSilentOperationParameters brokerSilentParametersFromBundle(
            @NonNull final Bundle bundle,
            @NonNull final Context context,
            @NonNull final Account account) {

        Logger.info(TAG, "Constructing BrokerAcquireTokenSilentOperationParameters from result bundle");

        final BrokerRequest brokerRequest = new Gson().fromJson(
                bundle.getString(AuthenticationConstants.Broker.BROKER_REQUEST_V2),
                BrokerRequest.class);

        final BrokerAcquireTokenSilentOperationParameters parameters =
                new BrokerAcquireTokenSilentOperationParameters();

        parameters.setAppContext(context);

        parameters.setAccountManagerAccount(account);

        parameters.setSdkType(SdkType.MSAL);

        int callingAppUid = bundle.getInt(
                AuthenticationConstants.Broker.CALLER_INFO_UID
        );
        parameters.setCallerUId(callingAppUid);

        parameters.setCallerPackageName(brokerRequest.getApplicationName());

        parameters.setCallerAppVersion(brokerRequest.getApplicationVersion());

        final Authority authority = Authority.getAuthorityFromAuthorityUrl(
                brokerRequest.getAuthority()
        );

        if (authority instanceof AzureActiveDirectoryAuthority) {
            ((AzureActiveDirectoryAuthority) authority).setMultipleCloudsSupported(brokerRequest.getMultipleCloudsSupported());
        }

        parameters.setAuthority(authority);

        String correlationIdString = bundle.getString(
                brokerRequest.getCorrelationId()
        );
        if (TextUtils.isEmpty(correlationIdString)) {
            UUID correlationId = UUID.randomUUID();
            correlationIdString = correlationId.toString();
        }
        parameters.setCorrelationId(correlationIdString);

        parameters.setScopes(
                getScopesAsSet(brokerRequest.getScope())
        );

        parameters.setRedirectUri(brokerRequest.getRedirect());

        parameters.setClientId(brokerRequest.getClientId());

        parameters.setForceRefresh(brokerRequest.getForceRefresh());

        parameters.setClaimsRequest(brokerRequest.getClaims());

        parameters.setLoginHint(brokerRequest.getUserName());

        parameters.setHomeAccountId(brokerRequest.getHomeAccountId());

        parameters.setLocalAccountId(brokerRequest.getLocalAccountId());

        if (!TextUtils.isEmpty(brokerRequest.getExtraQueryStringParameter())) {
            parameters.setExtraQueryStringParameters(
                    QueryParamsAdapter._fromJson(brokerRequest.getExtraQueryStringParameter())
            );
        }

        // Set Global environment variable for instance discovery if present
        if (!TextUtils.isEmpty(brokerRequest.getEnvironment())) {
            AzureActiveDirectory.setEnvironment(
                    Environment.valueOf(brokerRequest.getEnvironment())
            );
        }

        return parameters;
    }

    /**
     * Helper method to transforn scopes string to Set
     */
    private Set<String> getScopesAsSet(@Nullable final String scopeString) {
        if (TextUtils.isEmpty(scopeString)) {
            return new HashSet<>();
        }
        final String[] scopes = scopeString.split(" ");
        return new HashSet<>(Arrays.asList(scopes));
    }

    /**
     * Helper method to get redirect uri from parameters, calculates from package signature if not available.
     */
    private String getRedirectUri(@NonNull OperationParameters parameters) {
        if (TextUtils.isEmpty(parameters.getRedirectUri())) {
            return BrokerValidator.getBrokerRedirectUri(
                    parameters.getAppContext(),
                    parameters.getApplicationName()
            );
        }
        return parameters.getRedirectUri();
    }

    public Bundle getRequestBundleForHello(@NonNull final OperationParameters parameters) {
        final Bundle requestBundle = new Bundle();
        requestBundle.putString(AuthenticationConstants.Broker.CLIENT_ADVERTISED_MAXIMUM_BP_VERSION_KEY,
                AuthenticationConstants.Broker.BROKER_PROTOCOL_VERSION_CODE);

        if (!StringUtil.isEmpty(parameters.getRequiredBrokerProtocolVersion())) {
            requestBundle.putString(AuthenticationConstants.Broker.CLIENT_CONFIGURED_MINIMUM_BP_VERSION_KEY,
                    parameters.getRequiredBrokerProtocolVersion());
        }

        return requestBundle;
    }

    public Bundle getRequestBundleForAcquireTokenSilent(final AcquireTokenSilentOperationParameters parameters) {
        final MsalBrokerRequestAdapter msalBrokerRequestAdapter = new MsalBrokerRequestAdapter();

        final Bundle requestBundle = new Bundle();
        final BrokerRequest brokerRequest = msalBrokerRequestAdapter.
                brokerRequestFromSilentOperationParameters(parameters);

        requestBundle.putString(
                AuthenticationConstants.Broker.BROKER_REQUEST_V2,
                new Gson().toJson(brokerRequest, BrokerRequest.class)
        );

        requestBundle.putInt(
                AuthenticationConstants.Broker.CALLER_INFO_UID,
                parameters.getAppContext().getApplicationInfo().uid
        );

        return requestBundle;
    }

    public Bundle getRequestBundleForGetAccounts(@NonNull final OperationParameters parameters) {
        final Bundle requestBundle = new Bundle();
        requestBundle.putString(ACCOUNT_CLIENTID_KEY, parameters.getClientId());
        requestBundle.putString(ACCOUNT_REDIRECT, parameters.getRedirectUri());
        //Disable the environment and tenantID. Just return all accounts belong to this clientID.
        return requestBundle;
    }

    public Bundle getRequestBundleForRemoveAccount(@NonNull final OperationParameters parameters) {
        final Bundle requestBundle = new Bundle();
        if (null != parameters.getAccount()) {
            requestBundle.putString(ACCOUNT_CLIENTID_KEY, parameters.getClientId());
            requestBundle.putString(ENVIRONMENT, parameters.getAccount().getEnvironment());
            requestBundle.putString(ACCOUNT_HOME_ACCOUNT_ID, parameters.getAccount().getHomeAccountId());
        }

        return requestBundle;
    }

    public Bundle getRequestBundleForRemoveAccountFromSharedDevice(@NonNull final OperationParameters parameters) {
        final Bundle requestBundle = new Bundle();

        try {
            Browser browser = BrowserSelector.select(parameters.getAppContext(), parameters.getBrowserSafeList());
            requestBundle.putString(DEFAULT_BROWSER_PACKAGE_NAME, browser.getPackageName());
        } catch (ClientException e) {
            // Best effort. If none is passed to broker, then it will let the OS decide.
            Logger.error(TAG, e.getErrorCode(), e);
        }

        return requestBundle;
    }

    private boolean getMultipleCloudsSupported(@NonNull final OperationParameters parameters) {
        if (parameters.getAuthority() instanceof AzureActiveDirectoryAuthority) {
            final AzureActiveDirectoryAuthority authority = (AzureActiveDirectoryAuthority) parameters.getAuthority();
            return authority.getMultipleCloudsSupported();
        } else {
            return false;
        }
    }
}
