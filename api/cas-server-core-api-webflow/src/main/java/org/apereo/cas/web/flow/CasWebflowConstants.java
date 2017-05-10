package org.apereo.cas.web.flow;

/**
 * This is {@link CasWebflowConstants}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
public interface CasWebflowConstants {
    /**
     * The transition state 'success'.
     */
    String TRANSITION_ID_SUCCESS = "success";

    /**
     * The state id 'success'.
     */
    String STATE_ID_SUCCESS = "success";

    /**
     * The state id 'verifyTrustedDevice'.
     */
    String STATE_ID_VERIFY_TRUSTED_DEVICE = "verifyTrustedDevice";
    
    /**
     * The view id 'registerDeviceView'.
     */
    String VIEW_ID_REGISTER_DEVICE = "registerDeviceView";

    /**
     * The state id 'registerTrustedDevice'.
     */
    String STATE_ID_REGISTER_TRUSTED_DEVICE = "registerTrustedDevice";

    /**
     * The transition state 'realSubmit'.
     */
    String TRANSITION_ID_REAL_SUBMIT = "realSubmit";

    /**
     * The decision state 'checkRegistrationRequired'.
     */
    String DECISION_STATE_REQUIRE_REGISTRATION = "checkRegistrationRequired";
    
    /**
     * The transition state 'yes'.
     */
    String TRANSITION_ID_YES = "yes";

    /**
     * The transition state 'no'.
     */
    String TRANSITION_ID_NO = "no";

    /**
     * The transition state 'submit'.
     */
    String TRANSITION_ID_SUBMIT = "submit";
    /**
     * The transition state 'error'.
     */
    String TRANSITION_ID_ERROR = "error";

    /**
     * The transition state 'authenticationFailure'.
     */
    String TRANSITION_ID_AUTHENTICATION_FAILURE = "authenticationFailure";

    /**
     * 'gateway' state id.
     */
    String STATE_ID_GATEWAY = "gateway";
    
    /**
     * The transition state 'warn'.
     */
    String TRANSITION_ID_WARN = "warn";

    /**
     * The transition state 'initialAuthenticationRequestValidationCheck'.
     */
    String TRANSITION_ID_INITIAL_AUTHN_REQUEST_VALIDATION_CHECK = "initialAuthenticationRequestValidationCheck";

    /**
     * The transition state 'sendTicketGrantingTicket'.
     */
    String TRANSITION_ID_SEND_TICKET_GRANTING_TICKET = "sendTicketGrantingTicket";

    /**
     * The state 'initializeLoginForm'.
     */
    String STATE_ID_INIT_LOGIN_FORM = "initializeLoginForm";

    /**
     * The state 'viewLoginForm'.
     */
    String STATE_ID_VIEW_LOGIN_FORM = "viewLoginForm";

    /**
     * The state 'serviceAuthorizationCheck'.
     */
    String STATE_ID_SERVICE_AUTHZ_CHECK = "serviceAuthorizationCheck";

    /**
     * The state 'terminateSession'.
     */
    String STATE_ID_TERMINATE_SESSION = "terminateSession";

    /**
     * The state 'gatewayRequestCheck'.
     */
    String STATE_ID_GATEWAY_REQUEST_CHECK = "gatewayRequestCheck";

    /**
     * The state 'gatewayRequestCheck'.
     */
    String STATE_ID_GENERATE_SERVICE_TICKET = "generateServiceTicket";


    /**
     * The state 'gatewayServicesManagementCheck'.
     */
    String STATE_ID_GATEWAY_SERVICES_MGMT_CHECK = "gatewayServicesManagementCheck";

    /**
     * The state 'postView'.
     */
    String STATE_ID_POST_VIEW = "postView";

    /**
     * The state 'showWarningView'.
     */
    String STATE_ID_SHOW_WARNING_VIEW = "showWarningView";

    /**
     * The state 'redirectView'.
     */
    String STATE_ID_REDIR_VIEW = "redirectView";

    /**
     * The state 'viewRedirectToUnauthorizedUrlView'.
     */
    String STATE_ID_VIEW_REDIR_UNAUTHZ_URL = "viewRedirectToUnauthorizedUrlView";

    /**
     * The state 'serviceUnauthorizedCheck'.
     */
    String STATE_ID_SERVICE_UNAUTHZ_CHECK = "serviceUnauthorizedCheck";


    /**
     * The state 'viewGenericLoginSuccess'.
     */
    String STATE_ID_VIEW_GENERIC_LOGIN_SUCCESS = "viewGenericLoginSuccess";

    /**
     * The state 'serviceCheck'.
     */
    String STATE_ID_SERVICE_CHECK = "serviceCheck";

    /**
     * The state 'viewServiceErrorView'.
     */
    String STATE_ID_VIEW_SERVICE_ERROR = "viewServiceErrorView";

    /**
     * The state id 'warn'.
     */
    String STATE_ID_WARN = "warn";

    /**
     * The state id 'renewRequestCheck'.
     */
    String STATE_ID_RENEW_REQUEST_CHECK = "renewRequestCheck";

    /**
     * The state id 'hasServiceCheck'.
     */
    String STATE_ID_HAS_SERVICE_CHECK = "hasServiceCheck";

    /**
     * The state id 'redirect'.
     */
    String STATE_ID_REDIRECT = "redirect";

    /**
     * The state id 'postRedirectDecision'.
     */
    String STATE_ID_POST_REDIR_DECISION = "postRedirectDecision";
    
    /**
     * The view id 'casGenericSuccessView'.
     */
    String VIEW_ID_GENERIC_SUCCESS = "casGenericSuccessView";

    /**
     * The view id 'casConfirmView'.
     */
    String VIEW_ID_CONFIRM = "casConfirmView";

    /**
     * The view id 'casServiceErrorView'.
     */
    String VIEW_ID_SERVICE_ERROR = "casServiceErrorView";

    /**
     * The flow var id 'credential'.
     */
    String VAR_ID_CREDENTIAL = "credential";

    /**
     * View id 'casResetPasswordSendInstructions'.
     */
    String VIEW_ID_SEND_RESET_PASSWORD_ACCT_INFO = "casResetPasswordSendInstructionsView";

    /**
     * View id 'casResetPasswordSentInstructions'.
     */
    String VIEW_ID_SENT_RESET_PASSWORD_ACCT_INFO = "casResetPasswordSentInstructionsView";
    
    /**
     * Transition id 'resetPassword'.
     */
    String TRANSITION_ID_RESET_PASSWORD = "resetPassword";
}
