package db.mysql;

import java.util.HashSet;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.TicketMasterAPI;

public class MySQLConnection implements DBConnection {

	private Connection connection;
	
	public MySQLConnection() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();
			connection = DriverManager.getConnection(MySQLDBUtil.URL);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void close() {
		if (connection != null) {
			try {
				connection.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		if (connection == null) {
			System.err.println("DB connection failed");
			return;
		}
		
		try {
			String sql = "INSERT IGNORE INTO history(user_id, item_id) VALUE(?, ?)";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, userId);
			for (String itemId : itemIds) {
				ps.setString(2, itemId);
				ps.execute();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		if (connection == null) {
			System.err.println("DB connection failed");
			return;
		}
		
		try {
			String sql = "DELETE FROM history WHERE user_id = ? AND item_id = ?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, userId);
			for (String itemId : itemIds) {
				ps.setString(2, itemId);
				ps.execute();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		if (connection == null) {
			return new HashSet<>();
		}
		Set<String> favoriteItems = new HashSet<>();
		
		try {
			String sql = "SELECT item_id FROM history WHERE user_id = ?";
			PreparedStatement pStatement = connection.prepareStatement(sql);
			pStatement.setString(1, userId);
			ResultSet rSet = pStatement.executeQuery();
			
			while (rSet.next()) {
				String itemId = rSet.getString("item_id");
				favoriteItems.add(itemId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return favoriteItems;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		if (connection == null) {
			return new HashSet<>();
		}
		Set<Item> favoriteItems = new HashSet<>();
		Set<String> itemIds = getFavoriteItemIds(userId);
		
		try {
			String sql = "SELECT * FROM items WHERE item_id = ?";
			PreparedStatement pStatement = connection.prepareStatement(sql);
			for (String itemId : itemIds) {
				pStatement.setString(1, itemId);
				ResultSet rSet = pStatement.executeQuery();
				ItemBuilder builder = new ItemBuilder();
				while (rSet.next()) {
					builder.setItemId(rSet.getString("item_id"));
					builder.setName(rSet.getString("name"));
					builder.setAddress(rSet.getString("address"));
					builder.setImageUrl(rSet.getString("image_url"));
					builder.setUrl(rSet.getString("url"));
					builder.setCategories(getCategories(itemId));
					builder.setDistance(rSet.getDouble("distance"));
					builder.setRating(rSet.getDouble("rating"));
					
					favoriteItems.add(builder.build());
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return favoriteItems;
		
	}

	@Override
	public Set<String> getCategories(String itemId) {
		if (connection == null) {
			return null;
		}
		Set<String> categories = new HashSet<>();
		try {
			String sql = "SELECT category from categories WHERE item_id = ? ";
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, itemId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				String category = rs.getString("category");
				categories.add(category);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return categories;
		
	}


	@Override
	public List<Item> searchItems(double lat, double lon, String term) {
		TicketMasterAPI ticketMasterAPI = new TicketMasterAPI();
		List<Item> items = ticketMasterAPI.search(lat, lon, term);
		
		for (Item item : items) {
			saveItem(item);
		}
		return items;
	}

	@Override
	public void saveItem(Item item) {
		if (connection == null) {
			System.err.println("DB connection failed");
			return;
		}
		
		try {
			String sql = "INSERT IGNORE INTO items VALUES(?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, item.getItemId());
	   		ps.setString(2, item.getName());
	   		ps.setDouble(3, item.getRating());
	   		ps.setString(4, item.getAddress());
	   		ps.setString(5, item.getImageUrl());
	   		ps.setString(6, item.getUrl());
	   		ps.setDouble(7, item.getDistance());
	   		ps.execute();
	   		
	   		sql = "INSERT IGNORE INTO categories VALUES(?, ?)";
	   		ps = connection.prepareStatement(sql);
	   		ps.setString(1, item.getItemId());
	   		for (String category : item.getCategories()) {
	   			ps.setString(2, category);
	   			ps.execute();
	   		}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public String getFullname(String userId) {
		if (connection == null) {
			return "";
		}
		
		String name = "";
		try {
			String sql = "SELECT first_name, last_name FROM users WHERE user_id = ?";
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rSet = statement.executeQuery();
			if (rSet.next()) {
				name = rSet.getString("first_name") + " " +rSet.getString("last_name");
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			// TODO: handle exception
		}
		

		return name;

	}

	@Override
	public boolean verifyLogin(String userId, String password) {
		if (connection == null) {
			return false;
		}
		
		try {
			String sql = "SELECT user_id FROM users WHERE user_id = ? AND password = ?";
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, userId);
			statement.setString(2, password);
			
			ResultSet rSet = statement.executeQuery();
			if (rSet.next()) {
				return true;
			}
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			// TODO: handle exception
		}
	
		return false;

	}

}
