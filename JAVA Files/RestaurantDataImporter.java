package com.foodwala.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestaurantDataImporter {

	private static final String JSON_URL = "https://raw.githubusercontent.com/asifd1253/food-wala-api/main/restaurants/restaurants.json";

	private static final int RESTAURANT_ADMIN_ID = 7;

	private static final String INSERT_QUERY = "INSERT INTO restaurant " + "(restaurant_name, cuisine_type, "
			+ "estimated_delivery_time, restaurant_address, " + "restaurant_admin_id, cost_for_two, rating, "
			+ "is_active, restaurant_image_url) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String EXISTING_RESTAURANTS_QUERY = "SELECT restaurant_name FROM restaurant";

	public static void main(String[] args) {

		try {

			// ------------------------------------------------
			// 1. Download JSON
			// ------------------------------------------------

			HttpClient client = HttpClient.newHttpClient();

			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(JSON_URL)).GET().build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			System.out.println("HTTP Status: " + response.statusCode());

			if (response.statusCode() != 200) {

				System.out.println("Failed to download JSON.");

				return;
			}

			System.out.println("JSON downloaded successfully.");

			// ------------------------------------------------
			// 2. Parse JSON
			// ------------------------------------------------

			ObjectMapper objectMapper = new ObjectMapper();

			JsonNode root = objectMapper.readTree(response.body());

			System.out.println("JSON parsed successfully.");

			// ------------------------------------------------
			// 3. Get cards
			// ------------------------------------------------

			JsonNode cards = root.path("data").path("cards");

			System.out.println("Number of cards: " + cards.size());

			// ------------------------------------------------
			// 4. Find FIRST card containing restaurants
			//
			// Same logic as React:
			//
			// cards.find(
			// card =>
			// card?.card?.card?.gridElements
			// ?.infoWithStyle?.restaurants
			// )
			// ------------------------------------------------

			JsonNode restaurants = null;

			for (JsonNode cardWrapper : cards) {

				JsonNode card = cardWrapper.path("card").path("card");

				JsonNode restaurantArray = card.path("gridElements").path("infoWithStyle").path("restaurants");

				if (restaurantArray.isArray() && restaurantArray.size() > 0) {

					restaurants = restaurantArray;

					System.out.println("Restaurant card found!");

					System.out.println("Number of restaurants found: " + restaurants.size());

					break;
				}
			}

			// ------------------------------------------------
			// 5. Check whether restaurants were found
			// ------------------------------------------------

			if (restaurants == null) {

				System.out.println("No restaurant list found in JSON.");

				return;
			}

			// ------------------------------------------------
			// 6. Connect to MySQL
			// ------------------------------------------------

			try (Connection connection = DBConnection.getConnection();

					PreparedStatement insertStatement = connection.prepareStatement(INSERT_QUERY);

					PreparedStatement existingStatement = connection.prepareStatement(EXISTING_RESTAURANTS_QUERY)) {

				System.out.println("MySQL connection successful!");

				// ------------------------------------------------
				// 7. Get existing restaurant names
				// ------------------------------------------------

				Set<String> existingRestaurants = new HashSet<>();

				try (ResultSet resultSet = existingStatement.executeQuery()) {

					while (resultSet.next()) {

						String name = resultSet.getString("restaurant_name");

						if (name != null) {

							existingRestaurants.add(name.trim().toLowerCase());
						}
					}
				}

				System.out.println("Existing restaurants in database: " + existingRestaurants.size());

				// ------------------------------------------------
				// 8. Process restaurants
				// ------------------------------------------------

				int totalRestaurants = restaurants.size();

				int addedCount = 0;

				int skippedCount = 0;

				for (JsonNode restaurantNode : restaurants) {

					JsonNode restaurant = restaurantNode.path("info");

					// --------------------------------------------
					// Restaurant name
					// --------------------------------------------

					String restaurantName = restaurant.path("name").asText("").trim();

					if (restaurantName.isBlank()) {

						System.out.println("Skipping restaurant with no name.");

						continue;
					}

					// --------------------------------------------
					// Check duplicate
					// --------------------------------------------

					String restaurantKey = restaurantName.toLowerCase();

					if (existingRestaurants.contains(restaurantKey)) {

						System.out.println("Already exists - skipping: " + restaurantName);

						skippedCount++;

						continue;
					}

					// --------------------------------------------
					// Cuisine
					// --------------------------------------------

					String cuisineType = "";

					JsonNode cuisines = restaurant.path("cuisines");

					if (cuisines.isArray()) {

						StringBuilder cuisineBuilder = new StringBuilder();

						for (int i = 0; i < cuisines.size(); i++) {

							if (i > 0) {

								cuisineBuilder.append(", ");
							}

							cuisineBuilder.append(cuisines.get(i).asText());
						}

						cuisineType = cuisineBuilder.toString();
					}

					// --------------------------------------------
					// Delivery time
					// --------------------------------------------

					int deliveryTime = restaurant.path("sla").path("deliveryTime").asInt();

					// --------------------------------------------
					// Address
					// --------------------------------------------

					String locality = restaurant.path("locality").asText("");

					String areaName = restaurant.path("areaName").asText("");

					String restaurantAddress;

					if (!locality.isBlank() && !areaName.isBlank() && !locality.equalsIgnoreCase(areaName)) {

						restaurantAddress = locality + ", " + areaName;

					} else if (!areaName.isBlank()) {

						restaurantAddress = areaName;

					} else {

						restaurantAddress = locality;
					}

					// --------------------------------------------
					// Rating
					// --------------------------------------------

					double rating = restaurant.path("avgRating").asDouble();

					// --------------------------------------------
					// Is Open
					// --------------------------------------------

					boolean isOpen = restaurant.path("isOpen").asBoolean();

					// --------------------------------------------
					// Cost for two
					//
					// Example:
					// ₹500 for two
					//
					// Becomes:
					// 500
					// --------------------------------------------

					String costForTwoText = restaurant.path("costForTwo").asText("");

					String costNumber = costForTwoText.replaceAll("[^0-9]", "");

					int costForTwo = 0;

					if (!costNumber.isBlank()) {

						costForTwo = Integer.parseInt(costNumber);
					}

					// --------------------------------------------
					// Image URL
					// --------------------------------------------

					String imageId = restaurant.path("cloudinaryImageId").asText("");

					String imageUrl = null;

					if (!imageId.isBlank()) {

						imageUrl = "https://media-assets.swiggy.com/" + "swiggy/image/upload/"
								+ "fl_lossy,f_auto,q_auto,w_660/" + imageId;
					}

					// --------------------------------------------
					// Set PreparedStatement values
					// --------------------------------------------

					insertStatement.setString(1, restaurantName);

					insertStatement.setString(2, cuisineType);

					insertStatement.setInt(3, deliveryTime);

					insertStatement.setString(4, restaurantAddress);

					insertStatement.setInt(5, RESTAURANT_ADMIN_ID);

					insertStatement.setInt(6, costForTwo);

					insertStatement.setDouble(7, rating);

					insertStatement.setBoolean(8, isOpen);

					insertStatement.setString(9, imageUrl);

					// --------------------------------------------
					// Add to batch
					// --------------------------------------------

					insertStatement.addBatch();

					existingRestaurants.add(restaurantKey);

					addedCount++;

					System.out.println("Added to batch: " + restaurantName);
				}

				// ------------------------------------------------
				// 9. Execute batch
				// ------------------------------------------------

				if (addedCount > 0) {

					int[] result = insertStatement.executeBatch();

					System.out.println("--------------------------------");

					System.out.println("Batch insert completed!");

					System.out.println("Rows inserted: " + result.length);

				} else {

					System.out.println("No new restaurants to insert.");
				}

				// ------------------------------------------------
				// 10. Final summary
				// ------------------------------------------------

				System.out.println("--------------------------------");

				System.out.println("Total restaurants in JSON: " + totalRestaurants);

				System.out.println("Restaurants added: " + addedCount);

				System.out.println("Restaurants skipped: " + skippedCount);
			}

		} catch (IOException | InterruptedException e) {

			e.printStackTrace();

		} catch (SQLException e) {

			e.printStackTrace();
		}
	}
}
