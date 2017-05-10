package org.apereo.cas.support.oauth.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.BasicCredentialMetaData;
import org.apereo.cas.authentication.BasicIdentifiableCredential;
import org.apereo.cas.authentication.CredentialMetaData;
import org.apereo.cas.authentication.DefaultAuthenticationBuilder;
import org.apereo.cas.authentication.DefaultHandlerResult;
import org.apereo.cas.authentication.HandlerResult;
import org.apereo.cas.authentication.TestUtils;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.support.oauth.OAuthConstants;
import org.apereo.cas.ticket.accesstoken.AccessTokenImpl;
import org.apereo.cas.ticket.accesstoken.AccessTokenFactory;
import org.apereo.cas.ticket.accesstoken.DefaultAccessTokenFactory;
import org.apereo.cas.ticket.support.AlwaysExpiresExpirationPolicy;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * This class tests the {@link OAuth20ProfileController} class.
 *
 * @author Jerome Leleu
 * @since 3.5.2
 */

public class OAuth20ProfileControllerTests extends AbstractOAuth20Tests {

    private static final String CONTEXT = "/oauth2.0/";

    private static final String ID = "1234";

    private static final String NAME = "attributeName";

    private static final String NAME2 = "attributeName2";

    private static final String VALUE = "attributeValue";

    private static final String CONTENT_TYPE = "application/json";

    @Autowired
    private AccessTokenFactory accessTokenFactory;

    @Autowired
    private OAuth20ProfileController oAuth20ProfileController;

    @Test
    public void verifyNoGivenAccessToken() throws Exception {
        final MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", CONTEXT
                + OAuthConstants.PROFILE_URL);
        final MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        final ResponseEntity<String> entity = oAuth20ProfileController.handleRequestInternal(mockRequest, mockResponse);

        assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
        assertEquals(CONTENT_TYPE, mockResponse.getContentType());
        assertTrue(entity.getBody().contains(OAuthConstants.MISSING_ACCESS_TOKEN));
    }

    @Test
    public void verifyNoExistingAccessToken() throws Exception {
        final MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", CONTEXT
                + OAuthConstants.PROFILE_URL);
        mockRequest.setParameter(OAuthConstants.ACCESS_TOKEN, "DOES NOT EXIST");
        final MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        final ResponseEntity<String> entity = oAuth20ProfileController.handleRequestInternal(mockRequest, mockResponse);

        assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
        assertEquals(CONTENT_TYPE, mockResponse.getContentType());
        assertTrue(entity.getBody().contains(OAuthConstants.EXPIRED_ACCESS_TOKEN));
    }

    @Test
    public void verifyExpiredAccessToken() throws Exception {
        final Principal principal = org.apereo.cas.authentication.TestUtils.getPrincipal(ID, new HashMap<>());
        final Authentication authentication = getAuthentication(principal);
        final DefaultAccessTokenFactory expiringAccessTokenFactory = new DefaultAccessTokenFactory();
        expiringAccessTokenFactory.setExpirationPolicy(new AlwaysExpiresExpirationPolicy());
        final AccessTokenImpl accessToken = (AccessTokenImpl) expiringAccessTokenFactory.create(TestUtils.getService(), authentication);
        oAuth20ProfileController.getTicketRegistry().addTicket(accessToken);

        final MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", CONTEXT
                + OAuthConstants.PROFILE_URL);
        mockRequest.setParameter(OAuthConstants.ACCESS_TOKEN, accessToken.getId());
        final MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        final ResponseEntity<String> entity = oAuth20ProfileController.handleRequestInternal(mockRequest, mockResponse);
        assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
        assertEquals(CONTENT_TYPE, mockResponse.getContentType());
        assertTrue(entity.getBody().contains(OAuthConstants.EXPIRED_ACCESS_TOKEN));
    }

    @Test
    public void verifyOK() throws Exception {
        final Map<String, Object> map = new HashMap<>();
        map.put(NAME, VALUE);
        final List<String> list = Lists.newArrayList(VALUE, VALUE);
        map.put(NAME2, list);

        final Principal principal = org.apereo.cas.authentication.TestUtils.getPrincipal(ID, map);
        final Authentication authentication = getAuthentication(principal);
        final AccessTokenImpl accessToken = (AccessTokenImpl) accessTokenFactory.create(TestUtils.getService(), authentication);
        oAuth20ProfileController.getTicketRegistry().addTicket(accessToken);

        final MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", CONTEXT
                + OAuthConstants.PROFILE_URL);
        mockRequest.setParameter(OAuthConstants.ACCESS_TOKEN, accessToken.getId());
        final MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        final ResponseEntity<String> entity = oAuth20ProfileController.handleRequestInternal(mockRequest, mockResponse);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(CONTENT_TYPE, mockResponse.getContentType());

        final ObjectMapper mapper = new ObjectMapper();

        final String expected = "{\"id\":\"" + ID + "\",\"attributes\":[{\"" + NAME + "\":\"" + VALUE + "\"},{\"" + NAME2
                + "\":[\"" + VALUE + "\",\"" + VALUE + "\"]}]}";
        final JsonNode expectedObj = mapper.readTree(expected);
        final JsonNode receivedObj = mapper.readTree(entity.getBody());
        assertEquals(expectedObj.get("id").asText(), receivedObj.get("id").asText());

        final JsonNode expectedAttributes = expectedObj.get("attributes");
        final JsonNode receivedAttributes = receivedObj.get("attributes");

        assertEquals(expectedAttributes.findValue(NAME).asText(), receivedAttributes.findValue(NAME).asText());
        assertEquals(expectedAttributes.findValues(NAME2), receivedAttributes.findValues(NAME2));
    }

    @Test
    public void verifyOKWithAuthorizationHeader() throws Exception {
        final Map<String, Object> map = new HashMap<>();
        map.put(NAME, VALUE);
        final List<String> list = Lists.newArrayList(VALUE, VALUE);
        map.put(NAME2, list);

        final Principal principal = org.apereo.cas.authentication.TestUtils.getPrincipal(ID, map);
        final Authentication authentication = getAuthentication(principal);
        final AccessTokenImpl accessToken = (AccessTokenImpl) accessTokenFactory.create(TestUtils.getService(), authentication);
        oAuth20ProfileController.getTicketRegistry().addTicket(accessToken);

        final MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", CONTEXT
                + OAuthConstants.PROFILE_URL);
        mockRequest.addHeader("Authorization", OAuthConstants.BEARER_TOKEN + ' '
                + accessToken.getId());
        final MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        final ResponseEntity<String> entity = oAuth20ProfileController.handleRequestInternal(mockRequest, mockResponse);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(CONTENT_TYPE, mockResponse.getContentType());

        final ObjectMapper mapper = new ObjectMapper();

        final String expected = "{\"id\":\"" + ID + "\",\"attributes\":[{\"" + NAME + "\":\"" + VALUE + "\"},{\"" + NAME2
                + "\":[\"" + VALUE + "\",\"" + VALUE + "\"]}]}";
        final JsonNode expectedObj = mapper.readTree(expected);
        final JsonNode receivedObj = mapper.readTree(entity.getBody());
        assertEquals(expectedObj.get("id").asText(), receivedObj.get("id").asText());

        final JsonNode expectedAttributes = expectedObj.get("attributes");
        final JsonNode receivedAttributes = receivedObj.get("attributes");

        assertEquals(expectedAttributes.findValue(NAME).asText(), receivedAttributes.findValue(NAME).asText());
        assertEquals(expectedAttributes.findValues(NAME2), receivedAttributes.findValues(NAME2));
    }

    private static Authentication getAuthentication(final Principal principal) {
        final CredentialMetaData metadata = new BasicCredentialMetaData(
                new BasicIdentifiableCredential(principal.getId()));
        final HandlerResult handlerResult = new DefaultHandlerResult(principal.getClass().getCanonicalName(),
                metadata, principal, new ArrayList<>());

        return DefaultAuthenticationBuilder.newInstance()
                .setPrincipal(principal)
                .addCredential(metadata)
                .setAuthenticationDate(ZonedDateTime.now())
                .addSuccess(principal.getClass().getCanonicalName(), handlerResult)
                .build();
    }
}
