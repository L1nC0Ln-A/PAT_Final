package com.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

public class AdminFrame extends JFrame {

    private String username;
    private JTable usersTable;
    private DefaultTableModel model;

    public AdminFrame(String username) {
        this.username = username;

        setTitle("Admin Panel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Panel to hold users table and buttons panel
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Table to display users
        usersTable = new JTable(new DefaultTableModel(
            new Object[]{"ID", "Username", "Password", "Is Active", "Banned"}, 0
        ));
        model = (DefaultTableModel) usersTable.getModel();
        JScrollPane scrollPane = new JScrollPane(usersTable);

        // Set font and row height for the table
        Font tableFont = new Font("Arial", Font.PLAIN, 14);
        usersTable.setFont(tableFont);
        usersTable.setRowHeight(25);

        // Add components to main panel
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel for bottom buttons
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        // Ban button
        JButton banButton = new JButton("Ban");
        banButton.setFont(tableFont);
        banButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleBanUser();
            }
        });
        bottomPanel.add(banButton);

        // Unban button
        JButton unbanButton = new JButton("Unban");
        unbanButton.setFont(tableFont);
        unbanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleUnbanUser();
            }
        });
        bottomPanel.add(unbanButton);

        // Edit Password button
        JButton editPasswordButton = new JButton("Edit Password");
        editPasswordButton.setFont(tableFont);
        editPasswordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleEditPassword();
            }
        });
        bottomPanel.add(editPasswordButton);

        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(tableFont);
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fetchUsers();
            }
        });
        bottomPanel.add(refreshButton);

        // Logout button
        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(tableFont);
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogout();
            }
        });
        bottomPanel.add(logoutButton);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Add main panel to frame
        add(mainPanel);

        // Fetch and display users
        fetchUsers();

        setVisible(true);
    }

    private void fetchUsers() {
        try {
            URL url = new URL("http://localhost:8000/api/allUsers");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONArray users = new JSONArray(response.toString());
                model.setRowCount(0); // Clear existing rows

                for (int i = 0; i < users.length(); i++) {
                    JSONObject user = users.getJSONObject(i);
                    String bannedStatus = user.getInt("banned") == 1 ? "Yes" : "No";

                    model.addRow(new Object[]{
                        user.getInt("id"),
                        user.getString("username"),
                        user.getString("password"),
                        user.getInt("is_active") == 1 ? "Yes" : "No",
                        bannedStatus
                    });
                }
            } else {
                JOptionPane.showMessageDialog(this, "Failed to fetch users.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching users: " + e.getMessage());
        }
    }

    private void handleBanUser() {
        String userIdStr = JOptionPane.showInputDialog(this, "Enter user ID to ban:");
        if (userIdStr != null && !userIdStr.trim().isEmpty()) {
            try {
                int userId = Integer.parseInt(userIdStr);
                toggleBanStatus(userId, false);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid user ID.");
            }
        }
    }

    private void handleUnbanUser() {
        String userIdStr = JOptionPane.showInputDialog(this, "Enter user ID to unban:");
        if (userIdStr != null && !userIdStr.trim().isEmpty()) {
            try {
                int userId = Integer.parseInt(userIdStr);
                toggleBanStatus(userId, true);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid user ID.");
            }
        }
    }

    private void handleEditPassword() {
        String userIdStr = JOptionPane.showInputDialog(this, "Enter user ID to edit password:");
        if (userIdStr != null && !userIdStr.trim().isEmpty()) {
            try {
                int userId = Integer.parseInt(userIdStr);
                String newPassword = JOptionPane.showInputDialog(this, "Enter new password:");
                if (newPassword != null && !newPassword.trim().isEmpty()) {
                    editPassword(userId, newPassword);
                } else {
                    JOptionPane.showMessageDialog(this, "Password cannot be empty.");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid user ID.");
            }
        }
    }

    private void editPassword(int userId, String newPassword) {
        try {
            URL url = new URL("http://localhost:8000/api/editPassword");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/json");

            JSONObject payload = new JSONObject();
            payload.put("userId", userId);
            payload.put("newPassword", newPassword);

            String jsonInputString = payload.toString();

            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                JOptionPane.showMessageDialog(this, "Password updated successfully.");
                fetchUsers(); // Refresh the users list
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject errorResponse = new JSONObject(response.toString());
                JOptionPane.showMessageDialog(this, "Error: " + errorResponse.getString("error"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating password: " + e.getMessage());
        }
    }

    private void toggleBanStatus(int userId, boolean isBanned) {
        try {
            String endpoint = isBanned ? "unbanUser" : "banUser";
            URL url = new URL("http://localhost:8000/api/" + endpoint);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");

            JSONObject payload = new JSONObject();
            payload.put("adminId", 1);  // Hardcoded adminId for simplicity
            payload.put("userId", userId);

            String jsonInputString = payload.toString();

            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                fetchUsers(); // Refresh the users list
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject errorResponse = new JSONObject(response.toString());
                JOptionPane.showMessageDialog(this, "Error: " + errorResponse.getString("error"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void handleLogout() {
        try {
            String response = sendLogoutRequest(username);
            JSONObject jsonResponse = new JSONObject(response);
            int status = jsonResponse.getInt("status");
            if (status == 200) {
                JOptionPane.showMessageDialog(null, "Logout successful!");
                dispose(); // Close the admin frame
                new LoginFrame().setVisible(true); // Show the login frame again
            } else {
                JOptionPane.showMessageDialog(null, "Logout failed.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error logging out: " + e.getMessage());
        }
    }

    private String sendLogoutRequest(String username) throws Exception {
        URL url = new URL("http://localhost:8000/api/logout");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");

        String jsonInputString = new JSONObject().put("username", username).toString();

        con.setDoOutput(true);
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }
}
