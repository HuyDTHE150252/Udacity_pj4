package com.example.demo;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.requests.ModifyCartRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


@AutoConfigureMockMvc
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SareetaApplicationTests {

	private static final String createUserUrl = "/api/user/create";

	private static final String loginUrl = "/login";

	private static final String getUserByNameUrl = "/api/user";

	private static final String getUserByIdUrl = "/api/user/id";

	private static final String createItemUrl = "/api/item/create";

	private static final String getAllItemUrl = "/api/item";

	private static final String getItemById = "/api/item";

	private static final String getItemByName = "/api/item/name";

	private static final String addToCartUrl = "/api/cart/addToCart";

	private static final String removeFromCartUrl = "/api/cart/removeFromCart";

	private static final String submitOrder = "/api/order/submit";

	private static final String getOrderForUser = "/api/order/history";

	private static String jwtToken = "";

	@Autowired
	private MockMvc mvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@BeforeAll
	public void createNewUser() throws Exception {
		mockPostRequest(createMockUserData(), createUserUrl);
		MvcResult loginResult = mockPostRequest( createMockUserData(), loginUrl);
		jwtToken = (String) loginResult.getResponse().getHeaderValue("Authorization");
	}


	@Test
	public void test_create_user_successfully() throws Exception {
		MvcResult mvcResult = mockPostRequest(createMockUserData(), createUserUrl);
		MvcResult loginResult = mockPostRequest( createMockUserData(), loginUrl);
		jwtToken = (String) loginResult.getResponse().getHeaderValue("Authorization");
		User user = objectMapper.convertValue(
				convertResponseContentToJson(mvcResult),
				User.class
		);
		assertNotNull(user);
		assertEquals(createMockUserData().getUsername(), user.getUsername());
	}

	@Test
	public void test_create_user_unsuccessfully_due_to_password_is_missing() throws Exception {
		User user = createMockUserData();
		user.setPassword("");
		MvcResult mvcResult = mockPostRequest(user, createUserUrl);
		assertEquals(400, mvcResult.getResponse().getStatus());
	}

	@Test
	public void test_create_user_unsuccessfully_due_to_password_length_is_not_met_min_length() throws Exception {
		User user = createMockUserData();
		user.setPassword("123");
		MvcResult mvcResult = mockPostRequest(user, createUserUrl);
		assertEquals(400, mvcResult.getResponse().getStatus());
	}

	@Test
	public void test_get_user_by_name_successfully() throws Exception {
		mockPostRequest(createMockUserData(), createUserUrl);
		MvcResult mvcResult = mockGetRequest(getUserByNameUrl + "/" + createMockUserData().getUsername());
		User user = objectMapper.convertValue(
				convertResponseContentToJson(mvcResult),
				User.class
		);
		assertNotNull(user);
		assertEquals(createMockUserData().getUsername(), user.getUsername());
	}

	@Test
	public void test_get_user_by_name_unsuccessfully_due_to_unregistered_user() throws Exception {
		mockPostRequest(createMockUserData(), createUserUrl);
		MvcResult mvcResult = mockGetRequest(getUserByNameUrl + "/" + "Test");
		assertEquals(404, mvcResult.getResponse().getStatus());
		assertNull(mvcResult.getResponse().getContentType());
	}

	@Test
	public void test_get_user_by_id_successfully() throws Exception {
		mockPostRequest(createMockUserData(), createUserUrl);
		MvcResult savedUserResult = mockGetRequest(getUserByNameUrl + "/" + createMockUserData().getUsername());
		User user = objectMapper.convertValue(
				convertResponseContentToJson(savedUserResult),
				User.class
		);
		MvcResult mvcResult = mockGetRequest(getUserByIdUrl + "/" + user.getId());
		User userRetrievedById = objectMapper.convertValue(
				convertResponseContentToJson(mvcResult),
				User.class
		);
		assertNotNull(userRetrievedById);
		assertEquals(createMockUserData().getUsername(), userRetrievedById.getUsername());
	}

	@Test
	public void test_get_user_by_id_unsuccessfully_due_to_unregistered_id() throws Exception {
		mockPostRequest(createMockUserData(), createUserUrl);
		MvcResult mvcResult = mockGetRequest(getUserByIdUrl + "/" + "100");
		assertEquals(404, mvcResult.getResponse().getStatus());
		assertNull(mvcResult.getResponse().getContentType());
	}

	@Test
	public void test_create_item_successfully() throws Exception {
		MvcResult mvcResult = mockPostRequest(createMockItemData(), createItemUrl);
		Item createdItem = objectMapper.convertValue(
				convertResponseContentToJson(mvcResult),
				Item.class
		);
		assertEquals(createMockItemData().getPrice(), createdItem.getPrice());
		assertEquals(createMockItemData().getDescription(), createdItem.getDescription());
		assertEquals(createMockItemData().getName(), createdItem.getName());
	}

	@Test
	public void test_create_item_unsuccessfully_due_to_item_name_is_null() throws Exception {
		Item mockedItem = createMockItemData();
		mockedItem.setName(null);
		MvcResult mvcResult = mockPostRequest(mockedItem, createItemUrl);
		assertEquals(400, mvcResult.getResponse().getStatus());
		assertNull(mvcResult.getResponse().getContentType());
	}

	@Test
	public void test_find_all_item_successfully() throws Exception {
		MvcResult mvcResult = mockGetRequest(getAllItemUrl);
		List<Item> itemList  = objectMapper.readValue(
				mvcResult.getResponse().getContentAsByteArray(),
				new TypeReference<List<Item>>(){}
		);
		assertFalse(itemList.isEmpty());
	}

	@Test
	public void test_find_item_by_id_successfully() throws Exception {
		MvcResult mvcResult = mockGetRequest(getItemById + "/1");
		Item retrievedItem = objectMapper.convertValue(
				convertResponseContentToJson(mvcResult),
				Item.class
		);
		assertNotNull(retrievedItem);
		assertEquals(Long.valueOf(1), retrievedItem.getId());
	}

	@Test
	public void test_find_item_by_id_unsuccessfully_due_to_unregistered_item() throws Exception {
		MvcResult mvcResult = mockGetRequest(getItemById + "/100");
		assertEquals(404, mvcResult.getResponse().getStatus());
		assertNull(mvcResult.getResponse().getContentType());
	}

	@Test
	public void test_find_item_by_name_successfully() throws Exception {
		MvcResult mvcResult = mockGetRequest(getItemByName+ "/Round Widget");
		List<Item> itemList  = objectMapper.readValue(
				mvcResult.getResponse().getContentAsByteArray(),
				new TypeReference<List<Item>>(){}
		);
		assertFalse(itemList.isEmpty());
	}

	@Test
	public void test_find_item_by_name_unsuccessfully_due_to_unregistered_name() throws Exception {
		MvcResult mvcResult = mockGetRequest(getItemByName+ "/test");
		assertEquals(404, mvcResult.getResponse().getStatus());
		assertNull(mvcResult.getResponse().getContentType());
	}

	@Test
	public void test_add_to_cart_successfully() throws Exception {
		test_create_item_successfully();
		ModifyCartRequest mockCartRequest = createMockCartRequest();
		MvcResult mvcResult = mockPostRequest(mockCartRequest, addToCartUrl);
		Cart savedCart = objectMapper.convertValue(
				convertResponseContentToJson(mvcResult),
				Cart.class
		);
		assertNotNull(savedCart.getId());
		assertEquals(createMockUserData().getUsername(), savedCart.getUser().getUsername());
		assertNotNull(savedCart.getTotal());
		assertEquals(createMockItemData().getName(), savedCart.getItems().get(0).getName());
		assertEquals(createMockItemData().getDescription(), savedCart.getItems().get(0).getDescription());
		assertEquals(createMockItemData().getPrice().setScale(0, RoundingMode.UNNECESSARY), savedCart.getItems().get(0).getPrice().setScale(0, RoundingMode.UNNECESSARY));
	}

	@Test
	public void test_add_to_cart_unsuccessfully_due_to_unregistered_username() throws Exception {
		ModifyCartRequest mockCartRequest = createMockCartRequest();
		mockCartRequest.setUsername("Not registered name");
		MvcResult mvcResult = mockPostRequest(mockCartRequest, addToCartUrl);
		assertEquals(404, mvcResult.getResponse().getStatus());
		assertNull(mvcResult.getResponse().getContentType());
	}

	@Test
	public void test_add_to_cart_unsuccessfully_due_to_item_id() throws Exception {
		ModifyCartRequest mockCartRequest = createMockCartRequest();
		mockCartRequest.setItemId(100);
		MvcResult mvcResult = mockPostRequest(mockCartRequest, addToCartUrl);
		assertEquals(404, mvcResult.getResponse().getStatus());
		assertNull(mvcResult.getResponse().getContentType());
	}

	@Test
	public void test_remove_from_cart_successfully() throws Exception {
		test_create_item_successfully();
		ModifyCartRequest mockCartRequest = createMockCartRequest();
		MvcResult mvcResult = mockPostRequest(mockCartRequest, removeFromCartUrl);
		Cart savedCart = objectMapper.convertValue(
				convertResponseContentToJson(mvcResult),
				Cart.class
		);
		assertNotNull(savedCart.getId());
		assertEquals(createMockUserData().getUsername(), savedCart.getUser().getUsername());
		assertNotNull(savedCart.getTotal());
		assertTrue(savedCart.getItems().isEmpty());
	}

	@Test
	public void test_remove_from_cart_unsuccessfully_due_to_unregistered_username() throws Exception {
		ModifyCartRequest mockCartRequest = createMockCartRequest();
		mockCartRequest.setUsername("Not registered name");
		MvcResult mvcResult = mockPostRequest(mockCartRequest, removeFromCartUrl);
		assertEquals(404, mvcResult.getResponse().getStatus());
		assertNull(mvcResult.getResponse().getContentType());
	}

	@Test
	public void test_remove_from_cart_unsuccessfully_due_to_unregistered_item_id() throws Exception {
		ModifyCartRequest mockCartRequest = createMockCartRequest();
		mockCartRequest.setItemId(100);
		MvcResult mvcResult = mockPostRequest(mockCartRequest, removeFromCartUrl);
		assertEquals(404, mvcResult.getResponse().getStatus());
		assertNull(mvcResult.getResponse().getContentType());
	}

	@Test
	public void test_submit_order_successfully() throws Exception {
		MvcResult mvcResult = mockPostRequest(null, submitOrder + "/" + createMockUserData().getUsername());
		UserOrder userOrder = objectMapper.convertValue(
				convertResponseContentToJson(mvcResult),
				UserOrder.class
		);
		assertNotNull(userOrder);
	}

	@Test
	public void test_submit_order_unsuccessfully_due_to_unregistered_user() throws Exception {
		MvcResult mvcResult = mockPostRequest(null, submitOrder + "/" + "test");
		assertEquals(404, mvcResult.getResponse().getStatus());
		assertNull( mvcResult.getResponse().getContentType());
	}

	@Test
	public void test_get_order_for_user_successfully() throws Exception {
		test_submit_order_successfully();
		MvcResult mvcResult = mockGetRequest(getOrderForUser + "/" + createMockUserData().getUsername());
		List<UserOrder> userOrderList = objectMapper.readValue(
				mvcResult.getResponse().getContentAsByteArray(),
				new TypeReference<List<UserOrder>>(){}
		);
		assertNotNull(userOrderList);
		assertFalse(userOrderList.isEmpty());
	}

	@Test
	public void test_get_order_for_user_unsuccessfully_due_to_unregistered_user() throws Exception {
		test_submit_order_successfully();
		MvcResult mvcResult = mockGetRequest(getOrderForUser + "/" + "test");
		assertEquals(404, mvcResult.getResponse().getStatus());
		assertNull( mvcResult.getResponse().getContentType());
	}

	/**
	 * mockPostRequest
	 * @param postData postData
	 * @param url url
	 * @return MvcResult
	 * @throws Exception Exception
	 */
	private MvcResult mockPostRequest(Object postData, String url) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", jwtToken);
		RequestBuilder request = post(url)
				.headers(headers)
				.accept(MediaType.ALL_VALUE)
				.locale(Locale.US)
				.content(objectMapper.writeValueAsString(postData))
				.contentType(MediaType.APPLICATION_JSON);
		return mvc
				.perform(request)
				.andDo(print())
				.andReturn();
	}

	/**
	 * mockPutRequest
	 * @param postData postData
	 * @param url url
	 * @return MvcResult
	 * @throws Exception Exception
	 */
	private MvcResult mockPutRequest(Object postData, String url) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", jwtToken);
		RequestBuilder request = put(url)
				.headers(headers)
				.accept(MediaType.ALL_VALUE)
				.locale(Locale.US)
				.content(objectMapper.writeValueAsString(postData))
				.contentType(MediaType.APPLICATION_JSON);
		return mvc
				.perform(request)
				.andDo(print())
				.andReturn();
	}

	/**
	 * mockGetRequest
	 * @param url url
	 * @return MvcResult
	 * @throws Exception Exception
	 */
	private MvcResult mockGetRequest(String url) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", jwtToken);
		RequestBuilder request = get(url)
				.headers(headers)
				.accept(MediaType.ALL_VALUE)
				.locale(Locale.US)
				.contentType(MediaType.APPLICATION_JSON);
		return mvc
				.perform(request)
				.andDo(print())
				.andReturn();
	}

	/**
	 * mockDeleteRequest
	 * @param postData postData
	 * @param url url
	 * @return MvcResult
	 * @throws Exception Exception
	 */
	private MvcResult mockDeleteRequest(Object postData, String url) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", jwtToken);
		RequestBuilder request = delete(url)
				.headers(headers)
				.accept(MediaType.ALL_VALUE)
				.locale(Locale.US)
				.content(objectMapper.writeValueAsString(postData))
				.contentType(MediaType.APPLICATION_JSON);
		return mvc
				.perform(request)
				.andDo(print())
				.andReturn();
	}

	/**
	 * convertResponseContentToJson
	 * @param mvcResult mvcResult
	 * @return JSONObject
	 * @throws UnsupportedEncodingException UnsupportedEncodingException
	 * @throws ParseException ParseException
	 */
	private JSONObject convertResponseContentToJson(MvcResult mvcResult) throws UnsupportedEncodingException, ParseException {
		JSONParser parser = new JSONParser();
		return (JSONObject) parser.parse(mvcResult.getResponse().getContentAsString());
	}

	/**
	 * createMockCartRequest
	 * @return ModifyCartRequest
	 */
	private ModifyCartRequest createMockCartRequest() {
		ModifyCartRequest request = new ModifyCartRequest();
		request.setUsername(createMockUserData().getUsername());
		request.setItemId(3);
		request.setQuantity(1);
		return request;
	}

	/**
	 * createMockCartData
	 * @return Cart
	 */
	private Cart createMockCartData() {
		Cart cart = new Cart();
		cart.setItems(new ArrayList<>());
		cart.setTotal(BigDecimal.ONE);
		cart.getItems().add(createMockItemData());
		return cart;
	}

	/**
	 * createMockItemData
	 * @return Item
	 */
	private Item createMockItemData() {
		Item item = new Item();
		item.setDescription("Test Item");
		item.setName("Test Item Name");
		item.setPrice(new BigDecimal(1000));
		return item;
	}

	/**
	 * createMockOrderData
	 * @return UserOrder
	 */
	private UserOrder createMockOrderData() {
		UserOrder userOrder = new UserOrder();
		userOrder.setUser(createMockUserData());
		return userOrder;
	}

	/**
	 * createMockUserData
	 *
	 * @return User
	 */
	private User createMockUserData() {
		User user = new User();
		user.setPassword("Test123");
		user.setUsername("MockUserName");
		user.setCart(createMockCartData());
		return user;
	}

}
