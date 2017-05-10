package org.apereo.cas.services.support;

import com.google.common.collect.Lists;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceAttributeFilter;
import org.apereo.cas.services.ReturnAllowedAttributeReleasePolicy;
import org.apereo.cas.util.serialization.SerializationUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Misagh Moayyed
 * @since 4.0.0
 */
public class RegisteredServiceRegexAttributeFilterTests {

    private RegisteredServiceAttributeFilter filter;
    private Map<String, Object> givenAttributesMap;

    @Mock
    private RegisteredService registeredService;

    public RegisteredServiceRegexAttributeFilterTests() {

        this.filter = new RegisteredServiceRegexAttributeFilter("^.{5,}$");

        this.givenAttributesMap = new HashMap<>();
        this.givenAttributesMap.put("uid", "loggedInTestUid");
        this.givenAttributesMap.put("phone", "1290");
        this.givenAttributesMap.put("familyName", "Smith");
        this.givenAttributesMap.put("givenName", "John");
        this.givenAttributesMap.put("employeeId", "E1234");
        this.givenAttributesMap.put("memberOf", Lists.newArrayList("math", "science", "chemistry"));
        this.givenAttributesMap.put("arrayAttribute", new String[] {"math", "science", "chemistry"});
        this.givenAttributesMap.put("setAttribute", new HashSet<>(Lists.newArrayList("math", "science", "chemistry")));

        final Map<String, String> mapAttributes = new HashMap<>();
        mapAttributes.put("uid", "loggedInTestUid");
        mapAttributes.put("phone", "890");
        mapAttributes.put("familyName", "Smith");
        this.givenAttributesMap.put("mapAttribute", mapAttributes);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(this.registeredService.getName()).thenReturn("sample test service");
        when(this.registeredService.getServiceId()).thenReturn("https://www.jasig.org");
    }

    @Test
    public void verifyPatternFilter() {

        final Map<String, Object> attrs = this.filter.filter(this.givenAttributesMap);
        assertEquals(attrs.size(), 7);

        assertFalse(attrs.containsKey("phone"));
        assertFalse(attrs.containsKey("givenName"));

        assertTrue(attrs.containsKey("uid"));
        assertTrue(attrs.containsKey("memberOf"));
        assertTrue(attrs.containsKey("mapAttribute"));

        @SuppressWarnings("unchecked")
        final Map<String, String> mapAttributes = (Map<String, String>) attrs.get("mapAttribute");
        assertTrue(mapAttributes.containsKey("uid"));
        assertTrue(mapAttributes.containsKey("familyName"));
        assertFalse(mapAttributes.containsKey("phone"));

        final List<?> obj = (List<?>) attrs.get("memberOf");
        assertEquals(2, obj.size());
    }

    @Test
    public void verifyServiceAttributeFilterAllowedAttributesWithARegexFilter() {
        final ReturnAllowedAttributeReleasePolicy policy = new ReturnAllowedAttributeReleasePolicy();
        policy.setAllowedAttributes(Lists.newArrayList("attr1", "attr3", "another"));
        policy.setAttributeFilter(new RegisteredServiceRegexAttributeFilter("v3"));
        final Principal p = mock(Principal.class);

        final Map<String, Object> map = new HashMap<>();
        map.put("attr1", "value1");
        map.put("attr2", "value2");
        map.put("attr3", Lists.newArrayList("v3", "v4"));

        when(p.getAttributes()).thenReturn(map);
        when(p.getId()).thenReturn("principalId");

        final Map<String, Object> attr = policy.getAttributes(p);
        assertEquals(attr.size(), 1);
        assertTrue(attr.containsKey("attr3"));

        final byte[] data = SerializationUtils.serialize(policy);
        final ReturnAllowedAttributeReleasePolicy p2 =
            SerializationUtils.deserializeAndCheckObject(data, ReturnAllowedAttributeReleasePolicy.class);
        assertNotNull(p2);
        assertEquals(p2.getAllowedAttributes(), policy.getAllowedAttributes());
        assertEquals(p2.getAttributeFilter(), policy.getAttributeFilter());
    }

    @Test
    public void verifySerialization() {
        final byte[] data = SerializationUtils.serialize(this.filter);
        final RegisteredServiceAttributeFilter secondFilter =
            SerializationUtils.deserializeAndCheckObject(data, RegisteredServiceAttributeFilter.class);
        assertEquals(secondFilter, this.filter);
    }
}
