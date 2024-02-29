package com.domain;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Logger;

import org.mindrot.jbcrypt.BCrypt;

import com.function.QueueMappingRequest;
import com.microsoft.azure.functions.HttpRequestMessage;

public class Authorisation {
    private String username, password;

    private static final Logger logger = Logger.getLogger(Authorisation.class.getName());
    private static final String DB_URL = "jdbc:mysql://leisadb.mysql.database.azure.com:3306/leisa";
    private static final String DB_USER = "lei";
    private static final String DB_PASSWORD = "mahirgamal123#";

    private long id;

    public boolean isAuthorised(HttpRequestMessage<Optional<QueueMappingRequest>> request) {
        // Parse the Authorization header
        final String authHeader = request.getHeaders().get("authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return false;
        }

        // Extract and decode username and password
        String base64Credentials = authHeader.substring("Basic ".length()).trim();
        byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
        String credentials = new String(credDecoded);

        // credentials = username:password
        final String[] values = credentials.split(":", 2);

        if (values.length != 2) {
            return false; // Incorrect format of the header
        }

        username = values[0];
        password = values[1];

        logger.info(username);
        logger.info(password);

        String sql = "SELECT * FROM users WHERE username=?";

        try (java.sql.Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && BCrypt.checkpw(password, rs.getString("password"))) {
                    id = rs.getLong("id");
                    return true;
                } else
                    return false;
            }
        } catch (SQLException e) {
            // Handle exceptions (log it or throw as needed)
            e.printStackTrace();
        }

        return false;

    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public long getId() {
        return id;
    }

}
