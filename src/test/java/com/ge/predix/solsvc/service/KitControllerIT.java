/*
 * Copyright (c) 2017 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.predix.solsvc.service;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.solsvc.ext.util.JsonMapper;
import com.ge.predix.solsvc.kitservice.error.DeviceRegistrationError;
import com.ge.predix.solsvc.kitservice.manager.DeviceManager;
import com.ge.predix.solsvc.kitservice.model.GeoLocation;
import com.ge.predix.solsvc.kitservice.model.RegisterDevice;
import com.ge.predix.solsvc.restclient.config.IOauthRestConfig;
import com.ge.predix.solsvc.restclient.impl.RestClient;

/**
 * 
 * @author 212421693 -
 */
public class KitControllerIT extends AbstractBaseControllerIT {

	private static final Logger log = LoggerFactory.getLogger(KitControllerIT.class);

	@Value("${local.server.port}")
	private int localServerPort;

	private URL base;
	private RestTemplate template;

	@Autowired
	private RestClient restClient;

	@Autowired
	@Qualifier("defaultOauthRestConfig")
	private IOauthRestConfig restConfig;

	@Autowired
	private JsonMapper jsonMapper;
	private ObjectMapper objectMapper;

	private @Value("${kit.test.webapp.user:null}") String appUser;

	@Value("${kit.test.webapp.user.password:null}")
	private String appUserPassword;

	@Value("${kit.test.predix.oauth.clientId:null}")
	private String clientCreds;

	@Autowired
	private DeviceManager deviceManager;

	/**
	 * -
	 */
	@Before
	public void setUp() {
		this.template = new TestRestTemplate();
		this.template.getMessageConverters().add(new FormHttpMessageConverter());
		this.template.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * @throws MalformedURLException
	 *             -
	 */
	@SuppressWarnings("nls")
	@Test
	public void registerDeviceWithAuthenticationError() throws MalformedURLException {
		this.base = new URL("http://localhost:" + this.localServerPort + "/device/register"); //$NON-NLS-2$
		ResponseEntity<String> response = this.template.getForEntity(this.base.toString(), String.class);
		assertThat(response.getBody(), containsString("unauthorized"));
	}

	/**
	 * @throws IOException
	 *             -
	 * @throws ParseException
	 *             -
	 */
	@SuppressWarnings("nls")
	@Test
	public void registerDevice() throws ParseException, IOException {

		String url = "http://localhost:" + this.localServerPort + "/device/register"; //$NON-NLS-2$
		RegisterDevice device = getRegisterDevice();

		String req = this.jsonMapper.toJson(device);
		log.debug("Register Device Json req is " + this.jsonMapper.toJson(device));
		List<Header> headers = new ArrayList<Header>();
		String userToken = getUserToken(this.appUser, this.appUserPassword);
		headers.add(new BasicHeader("Authorization", userToken));
		headers.add(new BasicHeader("Content-Type", "application/json")); //$NON-NLS-2$

		CloseableHttpResponse response = null;
		try {
			response = this.restClient.post(url, req, headers, this.restConfig.getDefaultConnectionTimeout(),
					this.restConfig.getDefaultSocketTimeout());
			Assert.assertNotNull(response);
			String body = EntityUtils.toString(response.getEntity());
			Assert.assertTrue(response.toString() + body, response.toString().contains("HTTP/1.1 200 OK"));
			assertThat(body, containsString("uri"));
			RegisterDevice registeredDevice = this.objectMapper.readValue(body, RegisterDevice.class);
			assertTrue(registeredDevice.getUri() != null);
			String kitDeviceUrl = (String) registeredDevice.getDeviceConfig().get("artifactoryConfigUrl");
			assertTrue(kitDeviceUrl != null && kitDeviceUrl.startsWith("https"));

		} finally {
			if (response != null)
				response.close();
		}
	}

	@SuppressWarnings("nls")
	// @Test
	public void registerDevice2() throws ParseException, IOException {

		String url = "http://localhost:" + this.localServerPort + "/device/register"; //$NON-NLS-2$
		RegisterDevice device = getRegisterDevice2();

		String req = this.jsonMapper.toJson(device);
		log.debug("Register Device Json req is " + this.jsonMapper.toJson(device));
		List<Header> headers = new ArrayList<Header>();
		String userToken = getUserToken("app_admin_1", "app_admin_1"); //$NON-NLS-2$
		// String userToken = getUserToken(this.appUser, this.appUserPassword);
		headers.add(new BasicHeader("Authorization", userToken));
		headers.add(new BasicHeader("Content-Type", "application/json")); //$NON-NLS-2$

		CloseableHttpResponse response = null;
		try {
			response = this.restClient.post(url, req, headers, this.restConfig.getDefaultConnectionTimeout(),
					this.restConfig.getDefaultSocketTimeout());
			Assert.assertNotNull(response);
			String body = EntityUtils.toString(response.getEntity());
			Assert.assertTrue(response.toString() + body, response.toString().contains("HTTP/1.1 200 OK"));
			assertThat(body, containsString("uri"));
			RegisterDevice registeredDevice = this.objectMapper.readValue(body, RegisterDevice.class);
			assertTrue(registeredDevice.getUri() != null);
			String kitDeviceUrl = (String) registeredDevice.getDeviceConfig().get("artifactoryConfigUrl");
			assertTrue(kitDeviceUrl != null && kitDeviceUrl.startsWith("https"));

		} finally {
			if (response != null)
				response.close();
		}
	}

	/**
	 * @return -
	 */
	@SuppressWarnings("nls")
	private RegisterDevice getRegisterDevicebyId(List<Header> headers, String url) {
		RegisterDevice registeredDevice = null;
		CloseableHttpResponse response = null;
		try {
			response = this.restClient.get(url, headers);
			Assert.assertNotNull(response);
			String body = EntityUtils.toString(response.getEntity());
			Assert.assertTrue(response.toString() + body, response.toString().contains("HTTP/1.1 200 OK"));
			registeredDevice = this.objectMapper.readValue(body, RegisterDevice.class);
			assertTrue(registeredDevice.getUri() != null);

		} catch (ParseException | IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (response != null)
				try {
					response.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
		}
		return registeredDevice;
	}

	/**
	 * @throws IOException
	 *             -
	 * @throws ParseException
	 *             - -
	 */
	@SuppressWarnings("nls")
	@Test
	public void registerDeviceValidationFailure() throws ParseException, IOException {

		String url = "http://localhost:" + this.localServerPort + "/device/register"; //$NON-NLS-2$
		RegisterDevice device = getRegisterDevice();
		// device.setUserId("");
		// device.setDeviceAddress("");
		device.setDeviceName("!!!!");
		String req = this.jsonMapper.toJson(device);
		log.debug("Register Device Json req is " + this.jsonMapper.toJson(device));
		List<Header> headers = new ArrayList<Header>();
		String userToken = getUserToken(this.appUser, this.appUserPassword);
		headers.add(new BasicHeader("Authorization", userToken));
		headers.add(new BasicHeader("Content-Type", "application/json")); //$NON-NLS-2$

		CloseableHttpResponse response = null;
		try {
			response = this.restClient.post(url, req, headers, this.restConfig.getDefaultConnectionTimeout(),
					this.restConfig.getDefaultSocketTimeout());
			Assert.assertNotNull(response);
			String body = EntityUtils.toString(response.getEntity());
			Assert.assertTrue(response.toString() + body, response.toString().contains("HTTP/1.1 400"));
			assertThat(body, containsString("error"));

		} finally {
			if (response != null)
				response.close();
		}
	}

	/**
	 * 
	 * @throws IOException
	 *             -
	 * @throws ParseException
	 *             -
	 */
	@SuppressWarnings("nls")
	@Test
	public void getDevice() throws ParseException, IOException {
		RegisterDevice device = getRegisterDevice();
		String url = "http://localhost:" + this.localServerPort + "/device/" + device.getDeviceAddress(); //$NON-NLS-2$
		List<Header> headers = new ArrayList<Header>();
		String userToken = getUserToken(this.appUser, this.appUserPassword);
		headers.add(new BasicHeader("Authorization", userToken));
		headers.add(new BasicHeader("Content-Type", "application/json")); //$NON-NLS-2$

		CloseableHttpResponse response = null;
		try {
			response = this.restClient.get(url, headers);
			Assert.assertNotNull(response);
			String body = EntityUtils.toString(response.getEntity());
			Assert.assertTrue(response.toString() + body, response.toString().contains("HTTP/1.1 200 OK"));
			// assertThat(body, containsString("uri"));
			RegisterDevice registeredDevice = this.objectMapper.readValue(body, RegisterDevice.class);
			assertTrue(registeredDevice.getUri() != null);

		} finally {
			if (response != null)
				response.close();
		}
	}

	/**
	 * 
	 * @throws IOException
	 *             -
	 * @throws ParseException
	 *             -
	 */
	@SuppressWarnings("nls")
	@Test
	public void getDeviceValidation() throws ParseException, IOException {
		String url = "http://localhost:" + this.localServerPort + "/device/tinfoil_0481%29"; //$NON-NLS-2$
		List<Header> headers = new ArrayList<Header>();
		String userToken = getUserToken(this.appUser, this.appUserPassword);
		headers.add(new BasicHeader("Authorization", userToken));
		headers.add(new BasicHeader("Content-Type", "application/json")); //$NON-NLS-2$

		CloseableHttpResponse response = null;
		try {
			response = this.restClient.get(url, headers);
			Assert.assertNotNull(response);
			String body = EntityUtils.toString(response.getEntity());
			Assert.assertTrue(response.toString() + body, response.toString().contains("HTTP/1.1 400"));
			assertThat(body, containsString("error"));

		} finally {
			if (response != null)
				response.close();
		}
	}

	/**
	 * 
	 * @throws IOException
	 *             -
	 * @throws ParseException
	 *             -
	 */
	@SuppressWarnings({ "unchecked", "nls" })
	@Test
	public void getAllDevice() throws ParseException, IOException {
		String url = "http://localhost:" + this.localServerPort + "/device/"; //$NON-NLS-2$
		List<Header> headers = new ArrayList<Header>();
		String userToken = getUserToken(this.appUser, this.appUserPassword);
		headers.add(new BasicHeader("Authorization", userToken));
		headers.add(new BasicHeader("Content-Type", "application/json")); //$NON-NLS-2$

		CloseableHttpResponse response = null;
		try {
			response = this.restClient.get(url, headers);
			Assert.assertNotNull(response);
			String body = EntityUtils.toString(response.getEntity());
			Assert.assertTrue(response.toString() + body, response.toString().contains("HTTP/1.1 200 OK"));
			List<RegisterDevice> registeredDevices = this.objectMapper.readValue(body, ArrayList.class);
			assertTrue(registeredDevices != null);
			assertTrue(registeredDevices.size() > 0);

		} finally {
			if (response != null)
				response.close();
		}
	}

	/**
	 * @throws IOException
	 *             -
	 * @throws DeviceRegistrationError
	 *             -
	 */
	@SuppressWarnings("nls")
	@Test
	public void updateDevice() throws IOException, DeviceRegistrationError {
		List<Header> headers = new ArrayList<Header>();
		String userToken = getUserToken(this.appUser, this.appUserPassword);
		headers.add(new BasicHeader("Authorization", userToken));
		headers.add(new BasicHeader("Content-Type", "application/json"));

		RegisterDevice device = getRegisterDevice();
		// we don't get a userGroup when we get back the device.
		String url = "http://localhost:" + this.localServerPort + "/device/" + device.getDeviceAddress(); //$NON-NLS-2$
		// ensure the device hasn't expired
		DateTime twoDaysAgo = new DateTime().minusDays(2);
		device.setActivationDate(String.valueOf(twoDaysAgo.getMillis()));
		Jwt accessToken = JwtHelper.decode(userToken.substring(7));
		String userId = this.deviceManager.getUserId(accessToken);

		// RegisterDevice originalDevice =
		// this.deviceManager.getDevice(device.getDeviceAddress(), userId);
		// if(null !=(originalDevice.getUserGroup()))// it might be null because of
		// reset.
		// device.setUserGroup(originalDevice.getUserGroup());
		// //it is better to set the userGroup here so that we don't keep creating new
		// userGrp in this test when we call createorUpdateDevice

		this.deviceManager.createorUpdateDevice(device, userId);
		RegisterDevice updateDevice = getRegisterDevicebyId(headers, url);
		updateDevice.setDeviceName("UpdateDevice-Test");

		String req = this.jsonMapper.toJson(updateDevice);
		log.debug("update Device Json req is " + this.jsonMapper.toJson(updateDevice));

		CloseableHttpResponse response = null;
		try {
			response = this.restClient.put(url, req, headers, this.restConfig.getDefaultConnectionTimeout(),
					this.restConfig.getDefaultSocketTimeout());
			Assert.assertNotNull(response);
			Assert.assertTrue(response.toString() + EntityUtils.toString(response.getEntity()),
					response.toString().contains("HTTP/1.1 200 OK"));

			RegisterDevice newUpdatedDevice = getRegisterDevicebyId(headers, url);
			// log.info("new updated device name is:" + newUpdatedDevice.getDeviceName() );
			// log.info("updateDevice name is:" + updateDevice.getDeviceName());
			Assert.assertTrue(newUpdatedDevice.getDeviceName().equalsIgnoreCase(updateDevice.getDeviceName()));
		} finally {
			if (response != null)
				response.close();
		}
	}

	/**
	 * @param url
	 * @param device
	 * @param lastWeek
	 * @throws IOException
	 *             -
	 */
	@SuppressWarnings("nls")
	private void callPost(String url, RegisterDevice device) throws IOException {
		String req = this.jsonMapper.toJson(device);
		log.debug("Device Json req is " + this.jsonMapper.toJson(device));
		List<Header> headers = new ArrayList<Header>();
		String userToken = getUserToken(this.appUser, this.appUserPassword);

		headers.add(new BasicHeader("Authorization", userToken));
		headers.add(new BasicHeader("Content-Type", "application/json")); //$NON-NLS-2$

		CloseableHttpResponse response = null;
		try {
			response = this.restClient.post(url, req, headers, this.restConfig.getDefaultConnectionTimeout(),
					this.restConfig.getDefaultSocketTimeout());
			Assert.assertNotNull(response);
			// first time response is 200OK, next time its a validation failure
			// due to expire time check
			Assert.assertTrue(response.toString() + EntityUtils.toString(response.getEntity()),
					response.toString().contains("HTTP/1.1 200 OK")
							|| (response.toString().contains("400 Bad Request")));

		} finally {
			if (response != null)
				response.close();
		}
	}

	/**
	 * @return -
	 */
	@SuppressWarnings("nls")
	private RegisterDevice getRegisterDevice() {
		RegisterDevice device = new RegisterDevice();
		// device.setUri("/device/test-guid");
		// device.setActivationDate("1496181827187");
		device.setActivationDate(String.valueOf(Instant.now().toEpochMilli()));
		// device.setCreatedDate(String.valueOf(Instant.now().toEpochMilli()));
		device.setDeviceName("NUC-WR-IDP-E799-2");
		device.setDeviceAddress("WR-IDP-E799");
		device.setDeviceType("NUC");
		device.setDeviceGroup("/deviceGroup/testcompany-2");
		// device.setUserId("bd9f70a3-8aaa-490b-b2a8-91ba59e58f0f");
		//
		device.setUri("/device/" + device.getDeviceAddress());
		GeoLocation geoLocation = new GeoLocation();
		geoLocation.setLatitude("51.5033640");
		geoLocation.setLongitude("-0.1276250");
		device.setGeoLocation(geoLocation);
		return device;
	}

	@SuppressWarnings("nls")
	private RegisterDevice getRegisterDevice2() {
		RegisterDevice device = new RegisterDevice();
		device.setActivationDate(String.valueOf(Instant.now().toEpochMilli()));
		device.setDeviceName("NUC-WR-IDP-2222");
		device.setDeviceAddress("WR-IDP-2222");
		device.setDeviceType("NUCSushma");
		device.setDeviceGroup("/deviceGroup/testcompany-2");
		device.setUri("/device/" + device.getDeviceAddress());
		GeoLocation geoLocation = new GeoLocation();
		geoLocation.setLatitude("51.5033640");
		geoLocation.setLongitude("-0.1276250");
		device.setGeoLocation(geoLocation);
		return device;
	}

	/**
	 * Returns a OAuth2RestTemplate based on the username password
	 */

	@SuppressWarnings("nls")
	private String getUserToken(String username, String password) {
		// get token here based on username password;
		ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
		resourceDetails.setUsername(username);
		resourceDetails.setPassword(password);

		String url = this.restConfig.getOauthIssuerId();

		resourceDetails.setAccessTokenUri(url);

		String[] clientIds = this.clientCreds.split(":");
		resourceDetails.setClientId(clientIds[0]);
		resourceDetails.setClientSecret(clientIds[1]);

		OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(resourceDetails);
		OAuth2AccessToken token = restTemplate.getAccessToken();

		return token.getTokenType() + " " + token.getValue();
	}

	@SuppressWarnings("nls")
	private String getClientToken(String clientId, String secret) {
		ClientCredentialsResourceDetails clientCredsLocal = new ClientCredentialsResourceDetails();
		String url = this.restConfig.getOauthIssuerId();
		clientCredsLocal.setAccessTokenUri(url);
		clientCredsLocal.setClientId(clientId);
		clientCredsLocal.setClientSecret(secret);
		OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(clientCredsLocal);
		OAuth2AccessToken token = restTemplate.getAccessToken();
		return token.getTokenType() + " " + token.getValue();
	}

	/**
	 * 
	 * @throws IOException
	 *             -
	 * @throws ParseException
	 *             -
	 */
	@SuppressWarnings({ "nls" })
	@Test
	public void checkDeviceActivationExpiry() throws ParseException, IOException {

		String url = "http://localhost:" + this.localServerPort + "/device/register"; //$NON-NLS-2$
		RegisterDevice device = getRegisterDevice();
		device.setUri("/device/testexpiry");
		device.setDeviceGroup("/deviceGroup/testexpiry");
		device.setDeviceName("TEST-EXPIRY");
		device.setDeviceAddress("testexpiry");
		DateTime twoMonthsAgo = new DateTime().minusDays(60 + 1);
		device.setActivationDate(String.valueOf(twoMonthsAgo.getMillis()));
		log.debug("Register Device Json req is " + this.jsonMapper.toJson(device));
		List<Header> headers = new ArrayList<Header>();
		String userToken = getUserToken(this.appUser, this.appUserPassword);

		headers.add(new BasicHeader("Authorization", userToken));
		headers.add(new BasicHeader("Content-Type", "application/json")); //$NON-NLS-2$
		callPost(url, device);

		CloseableHttpResponse response = null;
		try {
			String geturl = "http://localhost:" + this.localServerPort + "/device/" + device.getDeviceAddress(); //$NON-NLS-2$
			response = this.restClient.get(geturl, headers);
			String body = EntityUtils.toString(response.getEntity());
			assertThat(body, containsString("Device has past its activation period."));

		} finally {
			if (response != null)
				response.close();
		}

	}

	/**
	 * @throws IOException
	 *             -
	 * @throws ParseException
	 *             -
	 */
	@SuppressWarnings("nls")
	@Test
	public void getMachineDevice() throws ParseException, IOException {
		RegisterDevice device = getRegisterDevice();
		String url = "http://localhost:" + this.localServerPort + "/device/" + device.getDeviceAddress(); //$NON-NLS-2$
		List<Header> headers = new ArrayList<Header>();
		String clientToken = getClientToken("device_client_id", "secret"); //$NON-NLS-2$
		headers.add(new BasicHeader("Authorization", clientToken));
		headers.add(new BasicHeader("Content-Type", "application/json")); //$NON-NLS-2$

		CloseableHttpResponse response = null;
		try {
			response = this.restClient.get(url, headers);
			Assert.assertNotNull(response);
			String body = EntityUtils.toString(response.getEntity());
			Assert.assertTrue(response.toString() + body, response.toString().contains("HTTP/1.1 200 OK"));
			// assertThat(body, containsString("uri"));
			RegisterDevice registeredDevice = this.objectMapper.readValue(body, RegisterDevice.class);
			assertTrue(registeredDevice.getUri() != null);

		} finally {
			if (response != null)
				response.close();
		}
	}

	/**
	 * @throws IOException
	 *             -
	 */
	@Test
	@SuppressWarnings("nls")
	public void resetDevice() throws IOException, DeviceRegistrationError {
		List<Header> headers = new ArrayList<Header>();
		String userToken = getUserToken(this.appUser, this.appUserPassword);
		headers.add(new BasicHeader("Authorization", userToken));
		headers.add(new BasicHeader("Content-Type", "application/json")); //$NON-NLS-2$

		RegisterDevice device = getRegisterDevice();
		String deviceId = device.getDeviceAddress();
		String url = "http://localhost:" + this.localServerPort + "/device/reset/" + device.getDeviceAddress(); //$NON-NLS-2$
		/**
		 * the method callPut will reset the device by making deviceGroup and userGroup
		 * as null
		 */
		callPut(headers, url, "");
		RegisterDevice myResetDevice = this.deviceManager.getDevice(deviceId, null);
		if (myResetDevice != null) {
			/**
			 * Check/Assert if the reset worked. The activationDate and updateDate should be
			 * the same
			 */
			// log.debug("getting back the reset device now..");
			Assert.assertTrue(org.apache.commons.lang.StringUtils.equalsIgnoreCase(myResetDevice.getActivationDate(),
					myResetDevice.getUpdateDate()));
		}
	}

	/**
	 * @param headers
	 * @param url
	 * @param device
	 * @throws IOException
	 *             -
	 */
	@SuppressWarnings("nls")
	private void callPut(List<Header> headers, String url, String body) throws IOException {
		CloseableHttpResponse response = null;
		try {
			response = this.restClient.put(url, body, headers, this.restConfig.getDefaultConnectionTimeout(),
					this.restConfig.getDefaultSocketTimeout());
			Assert.assertNotNull(response);
			Assert.assertTrue(response.toString() + EntityUtils.toString(response.getEntity()),
					response.toString().contains("HTTP/1.1 200 OK"));
		} finally {
			if (response != null)
				response.close();
		}
	}

	/**
	 * 
	 * @throws IOException
	 *             -
	 * @throws ParseException
	 *             -
	 */
	@SuppressWarnings({ "unchecked", "nls" })
	@Test
	public void getAdminDevice() throws ParseException, IOException {

		String url = "http://localhost:" + this.localServerPort + "/device/"; //$NON-NLS-2$
		List<Header> headers = new ArrayList<Header>();
		String userToken = getUserToken("kit_admin_1", "Kit_Admin_111"); //$NON-NLS-2$
		headers.add(new BasicHeader("Authorization", userToken));
		headers.add(new BasicHeader("Content-Type", "application/json")); //$NON-NLS-2$

		CloseableHttpResponse response = null;
		try {
			response = this.restClient.get(url, headers);
			Assert.assertNotNull(response);
			String body = EntityUtils.toString(response.getEntity());
			Assert.assertTrue(response.toString() + body, response.toString().contains("HTTP/1.1 200 OK"));
			List<RegisterDevice> registeredDevices = this.objectMapper.readValue(body, ArrayList.class);
			log.info("number of devices returned: " + registeredDevices.size());
			log.info("devices returned: " + registeredDevices.toString());
			assertTrue(registeredDevices != null);
			assertTrue(registeredDevices.size() > 0);

		} finally {
			if (response != null)
				response.close();
		}
	}

}
