package com.foodwala.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MenuDataImporter {

	// =========================================================
	// BASE URL
	// =========================================================

	private static final String BASE_URL = "https://raw.githubusercontent.com/asifd1253/food-wala-api/main/menu/";

	// =========================================================
	// ALL 20 MENU JSON FILES
	// =========================================================

	private static final String[] MENU_FILES = {

			"118256.json", "17036.json", "17310.json", "23682.json", "248787.json", "2675.json", "279024.json",
			"28981.json", "29954.json", "334867.json", "350220.json", "36001.json", "376708.json", "382641.json",
			"50467.json", "548400.json", "57283.json", "79462.json", "801279.json", "81642.json" };

	// =========================================================
	// MAIN
	// =========================================================

	public static void main(String[] args) {

		HttpClient client = HttpClient.newHttpClient();

		ObjectMapper mapper = new ObjectMapper();

		int totalRestaurantsImported = 0;
		int totalMenuItemsInserted = 0;

		System.out.println();
		System.out.println("============================================");
		System.out.println("      FOODWALA MENU IMPORT STARTED");
		System.out.println("============================================");

		try {

			// =====================================================
			// PROCESS EACH RESTAURANT FILE
			// =====================================================

			for (String fileName : MENU_FILES) {

				System.out.println();
				System.out.println();
				System.out.println("--------------------------------------------");
				System.out.println("Processing : " + fileName);
				System.out.println("--------------------------------------------");

				String menuUrl = BASE_URL + fileName;

				// -------------------------------------------------
				// DOWNLOAD JSON
				// -------------------------------------------------

				HttpRequest request = HttpRequest.newBuilder().uri(URI.create(menuUrl)).GET().build();

				HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

				System.out.println("HTTP Status : " + response.statusCode());

				if (response.statusCode() != 200) {

					System.out.println("ERROR: Could not download " + fileName);

					continue;
				}

				// -------------------------------------------------
				// PARSE JSON
				// -------------------------------------------------

				JsonNode root = mapper.readTree(response.body());

				JsonNode cards = root.path("data").path("cards");

				// -------------------------------------------------
				// FIND RESTAURANT INFO
				// -------------------------------------------------

				JsonNode restaurantInfo = findRestaurantInfo(cards);

				if (restaurantInfo == null) {

					System.out.println("ERROR: Restaurant information not found.");

					continue;
				}

				String swiggyRestaurantId = restaurantInfo.path("id").asText();

				String restaurantName = restaurantInfo.path("name").asText().trim();

				System.out.println("Swiggy Restaurant ID : " + swiggyRestaurantId);

				System.out.println("Restaurant Name      : " + restaurantName);

				// -------------------------------------------------
				// FIND REGULAR MENU
				// -------------------------------------------------

				JsonNode regularCards = findRegularCards(cards);

				if (regularCards == null) {

					System.out.println("ERROR: Menu cards not found.");

					continue;
				}

				// -------------------------------------------------
				// CONNECT TO DATABASE
				// -------------------------------------------------

				try (Connection connection = DBConnection.getConnection()) {

					connection.setAutoCommit(false);

					try {

						// -----------------------------------------
						// FIND MYSQL RESTAURANT ID
						// -----------------------------------------

						int mysqlRestaurantId = findRestaurantId(connection, restaurantName);

						System.out.println("MySQL Restaurant ID  : " + mysqlRestaurantId);

						// -----------------------------------------
						// INSERT SQL
						// -----------------------------------------

						String insertSql = "INSERT INTO menu " + "(restaurant_id, " + "item_name, " + "description, "
								+ "price, " + "is_available, " + "food_type, " + "category, " + "rating, "
								+ "item_image_url) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

						int totalItemsForRestaurant = 0;

						try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {

							// -------------------------------------
							// PROCESS MENU CATEGORIES
							// -------------------------------------

							for (JsonNode menuCard : regularCards) {

								JsonNode menuData = menuCard.path("card").path("card");

								String type = menuData.path("@type").asText();

								// =================================
								// NORMAL CATEGORY
								// =================================

								if (type.equals("type.googleapis.com/swiggy.presentation.food.v2.ItemCategory")) {

									String category = menuData.path("title").asText().trim();

									JsonNode itemCards = menuData.path("itemCards");

									if (itemCards.isArray()) {

										for (JsonNode itemCard : itemCards) {

											JsonNode item = itemCard.path("card").path("info");

											insertMenuItem(insertStatement, mysqlRestaurantId, category, item);

											totalItemsForRestaurant++;
										}
									}
								}

								// =================================
								// NESTED CATEGORY
								// =================================

								else if (type
										.equals("type.googleapis.com/swiggy.presentation.food.v2.NestedItemCategory")) {

									String parentCategory = menuData.path("title").asText().trim();

									JsonNode categories = menuData.path("categories");

									if (categories.isArray()) {

										for (JsonNode categoryNode : categories) {

											String childCategory = categoryNode.path("title").asText().trim();

											String category = parentCategory + " / " + childCategory;

											JsonNode itemCards = categoryNode.path("itemCards");

											if (itemCards.isArray()) {

												for (JsonNode itemCard : itemCards) {

													JsonNode item = itemCard.path("card").path("info");

													insertMenuItem(insertStatement, mysqlRestaurantId, category, item);

													totalItemsForRestaurant++;
												}
											}
										}
									}
								}
							}

							// -------------------------------------
							// EXECUTE BATCH
							// -------------------------------------

							System.out.println("Executing batch insert...");

							int[] results = insertStatement.executeBatch();

							// -------------------------------------
							// COMMIT
							// -------------------------------------

							connection.commit();

							System.out.println();
							System.out.println("MENU IMPORT SUCCESSFUL");

							System.out.println("Restaurant : " + restaurantName);

							System.out.println("MySQL ID   : " + mysqlRestaurantId);

							System.out.println("Items      : " + totalItemsForRestaurant);

							System.out.println("Inserted   : " + results.length);

							totalRestaurantsImported++;

							totalMenuItemsInserted += results.length;
						}

					} catch (Exception e) {

						connection.rollback();

						System.out.println();
						System.out.println("ERROR importing " + restaurantName);

						System.out.println("Transaction rolled back.");

						e.printStackTrace();
					}
				}
			}

			// =====================================================
			// FINAL RESULT
			// =====================================================

			System.out.println();
			System.out.println();
			System.out.println("============================================");

			System.out.println("       FOODWALA MENU IMPORT COMPLETED");

			System.out.println("============================================");

			System.out.println("Restaurants Imported : " + totalRestaurantsImported);

			System.out.println("Total Menu Items     : " + totalMenuItemsInserted);

			System.out.println("============================================");

		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	// =========================================================
	// FIND RESTAURANT INFO
	// =========================================================

	private static JsonNode findRestaurantInfo(JsonNode cards) {

		for (JsonNode card : cards) {

			JsonNode info = card.path("card").path("card").path("info");

			if (!info.isMissingNode() && !info.isEmpty()) {

				return info;
			}
		}

		return null;
	}

	// =========================================================
	// FIND REGULAR MENU CARDS
	// =========================================================

	private static JsonNode findRegularCards(JsonNode cards) {

		for (JsonNode card : cards) {

			JsonNode regular = card.path("groupedCard").path("cardGroupMap").path("REGULAR").path("cards");

			if (regular.isArray()) {

				return regular;
			}
		}

		return null;
	}

	// =========================================================
	// FIND MYSQL RESTAURANT ID
	// =========================================================

	private static int findRestaurantId(Connection connection, String restaurantName) throws Exception {

		String sql = "SELECT restaurant_id " + "FROM restaurant " + "WHERE restaurant_name = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, restaurantName);

			try (ResultSet resultSet = statement.executeQuery()) {

				if (resultSet.next()) {

					return resultSet.getInt("restaurant_id");
				}
			}
		}

		throw new Exception("Restaurant not found in MySQL: " + restaurantName);
	}

	// =========================================================
	// INSERT MENU ITEM INTO BATCH
	// =========================================================

	private static void insertMenuItem(PreparedStatement preparedStatement, int restaurantId, String category,
			JsonNode item) throws Exception {

		// -----------------------------------------------------
		// ITEM NAME
		// -----------------------------------------------------

		String itemName = item.path("name").asText().trim();

		// -----------------------------------------------------
		// DESCRIPTION
		// -----------------------------------------------------

		String description = null;

		if (item.has("description") && !item.path("description").isNull()) {

			description = item.path("description").asText().trim();
		}

		// -----------------------------------------------------
		// PRICE
		// -----------------------------------------------------

		double price = 0.0;

		if (item.has("price") && !item.path("price").isNull()) {

			price = item.path("price").asDouble() / 100.0;

		} else if (item.has("defaultPrice") && !item.path("defaultPrice").isNull()) {

			price = item.path("defaultPrice").asDouble() / 100.0;
		}

		// -----------------------------------------------------
		// AVAILABILITY
		// -----------------------------------------------------

		int isAvailable = item.path("inStock").asInt();

		// -----------------------------------------------------
		// FOOD TYPE
		// -----------------------------------------------------

		String foodType = "UNKNOWN";

		JsonNode itemAttribute = item.path("itemAttribute");

		if (!itemAttribute.isMissingNode()) {

			String vegClassifier = itemAttribute.path("vegClassifier").asText();

			if (vegClassifier.equalsIgnoreCase("VEG")) {

				foodType = "VEG";

			} else if (vegClassifier.equalsIgnoreCase("NONVEG")) {

				foodType = "NON-VEG";
			}
		}

		// -----------------------------------------------------
		// RATING
		// -----------------------------------------------------

		double rating = 0.0;

		JsonNode ratings = item.path("ratings");

		if (!ratings.isMissingNode()) {

			rating = ratings.path("aggregatedRating").path("rating").asDouble();
		}

		// -----------------------------------------------------
		// IMAGE URL
		// -----------------------------------------------------

		String imageUrl = null;

		String imageId = item.path("imageId").asText();

		if (!imageId.isEmpty()) {

			imageUrl = "https://media-assets.swiggy.com/" + "swiggy/image/upload/" + "fl_lossy,f_auto,q_auto,w_660/"
					+ imageId;
		}

		// -----------------------------------------------------
		// SET VALUES
		// -----------------------------------------------------

		preparedStatement.setInt(1, restaurantId);

		preparedStatement.setString(2, itemName);

		if (description == null || description.isEmpty()) {

			preparedStatement.setNull(3, java.sql.Types.LONGVARCHAR);

		} else {

			preparedStatement.setString(3, description);
		}

		preparedStatement.setDouble(4, price);

		preparedStatement.setInt(5, isAvailable);

		preparedStatement.setString(6, foodType);

		preparedStatement.setString(7, category);

		preparedStatement.setDouble(8, rating);

		if (imageUrl == null) {

			preparedStatement.setNull(9, java.sql.Types.VARCHAR);

		} else {

			preparedStatement.setString(9, imageUrl);
		}

		// -----------------------------------------------------
		// ADD TO BATCH
		// -----------------------------------------------------

		preparedStatement.addBatch();
	}
}
