package org.apereo.cas.pm.web.flow;

import com.google.common.collect.Lists;
import org.apereo.cas.pm.PasswordChangeBean;
import org.apereo.cas.web.flow.AbstractCasWebflowConfigurer;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.webflow.engine.ActionState;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.Transition;
import org.springframework.webflow.engine.ViewState;
import org.springframework.webflow.engine.builder.BinderConfiguration;
import org.springframework.webflow.execution.Action;

/**
 * This is {@link PasswordManagementWebflowConfigurer}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
public class PasswordManagementWebflowConfigurer extends AbstractCasWebflowConfigurer {
    /**
     * Flow id for password reset.
     */
    public static final String FLOW_ID_PASSWORD_RESET = "pswdreset";

    /**
     * Flow id for password reset.
     */
    public static final String FLOW_VAR_ID_PASSWORD = "password";

    private static final String CAS_MUST_CHANGE_PASS_VIEW = "casMustChangePassView";
    private static final String CAS_EXPIRED_PASS_VIEW = "casExpiredPassView";
    private static final String PASSWORD_CHANGE_ACTION = "passwordChangeAction";
    private static final String SEND_PASSWORD_RESET_INSTRUCTIONS_ACTION = "sendInstructions";

    @Autowired
    @Qualifier("initPasswordChangeAction")
    private Action passwordChangeAction;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    protected void doInitialize() throws Exception {
        final Flow flow = getLoginFlow();
        createViewState(flow, "casAuthenticationBlockedView", "casAuthenticationBlockedView");
        createViewState(flow, "casBadWorkstationView", "casBadWorkstationView");
        createViewState(flow, "casBadHoursView", "casBadHoursView");
        createViewState(flow, "casAccountLockedView", "casAccountLockedView");
        createViewState(flow, "casAccountDisabledView", "casAccountDisabledView");
        createEndState(flow, "casPasswordUpdateSuccess", "casPasswordUpdateSuccessView");

        if (casProperties.getAuthn().getPm().isEnabled()) {
            configure(flow, CAS_MUST_CHANGE_PASS_VIEW);
            configure(flow, CAS_EXPIRED_PASS_VIEW);
            configurePasswordReset();
        } else {
            createViewState(flow, CAS_MUST_CHANGE_PASS_VIEW, CAS_MUST_CHANGE_PASS_VIEW);
            createViewState(flow, CAS_EXPIRED_PASS_VIEW, CAS_EXPIRED_PASS_VIEW);
        }
    }

    private void configurePasswordReset() {
        final Flow flow = getLoginFlow();
        final ViewState state = (ViewState) flow.getState(CasWebflowConstants.STATE_ID_VIEW_LOGIN_FORM);
        createTransitionForState(state, CasWebflowConstants.TRANSITION_ID_RESET_PASSWORD,
                CasWebflowConstants.VIEW_ID_SEND_RESET_PASSWORD_ACCT_INFO);
        final ViewState accountInfo = createViewState(flow, CasWebflowConstants.VIEW_ID_SEND_RESET_PASSWORD_ACCT_INFO,
                CasWebflowConstants.VIEW_ID_SEND_RESET_PASSWORD_ACCT_INFO);
        createTransitionForState(accountInfo, "findAccount", SEND_PASSWORD_RESET_INSTRUCTIONS_ACTION);
        final ActionState sendInst = createActionState(flow, SEND_PASSWORD_RESET_INSTRUCTIONS_ACTION,
                createEvaluateAction("sendPasswordResetInstructionsAction"));
        createTransitionForState(sendInst, CasWebflowConstants.TRANSITION_ID_SUCCESS,
                CasWebflowConstants.VIEW_ID_SENT_RESET_PASSWORD_ACCT_INFO);
        createTransitionForState(sendInst, CasWebflowConstants.TRANSITION_ID_ERROR, accountInfo.getId());
        createViewState(flow, CasWebflowConstants.VIEW_ID_SENT_RESET_PASSWORD_ACCT_INFO,
                CasWebflowConstants.VIEW_ID_SENT_RESET_PASSWORD_ACCT_INFO);

        final Flow pswdFlow = buildFlow("classpath:/webflow/pswdreset/pswdreset-webflow.xml", FLOW_ID_PASSWORD_RESET);
        createViewState(pswdFlow, "passwordResetErrorView", "casResetPasswordErrorView");
        createEndState(pswdFlow, "casPasswordUpdateSuccess", "casPasswordUpdateSuccessView");
        configure(pswdFlow, CAS_MUST_CHANGE_PASS_VIEW);
        loginFlowDefinitionRegistry.registerFlowDefinition(pswdFlow);
    }

    private void configure(final Flow flow, final String id) {
        createFlowVariable(flow, FLOW_VAR_ID_PASSWORD, PasswordChangeBean.class);

        final BinderConfiguration binder = createStateBinderConfiguration(Lists.newArrayList(FLOW_VAR_ID_PASSWORD, "confirmedPassword"));
        final ViewState viewState = createViewState(flow, id, id, binder);
        createStateModelBinding(viewState, FLOW_VAR_ID_PASSWORD, PasswordChangeBean.class);

        viewState.getEntryActionList().add(this.passwordChangeAction);
        final Transition transition = createTransitionForState(viewState, CasWebflowConstants.TRANSITION_ID_SUBMIT, PASSWORD_CHANGE_ACTION);
        transition.getAttributes().put("bind", Boolean.TRUE);
        transition.getAttributes().put("validate", Boolean.TRUE);

        createStateDefaultTransition(viewState, id);

        final ActionState pswChangeAction = createActionState(flow, PASSWORD_CHANGE_ACTION, createEvaluateAction(PASSWORD_CHANGE_ACTION));
        pswChangeAction.getTransitionSet().add(
                createTransition(PasswordChangeAction.PASSWORD_UPDATE_SUCCESS, "casPasswordUpdateSuccess"));
        pswChangeAction.getTransitionSet().add(createTransition(CasWebflowConstants.TRANSITION_ID_ERROR, id));
    }
}
