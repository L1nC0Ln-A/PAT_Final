package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class LoginFrame extends JFrame {

    private JTextField userText;
    private JPasswordField passText;
    private JButton logoutButton;
    private int userId;

    public LoginFrame() {
        try {
            // Set Nimbus look and feel
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set frame properties
        setTitle("Login Page");
        setSize(450, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create panel to hold components
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Create components
        JLabel userLabel = new JLabel("Username:");
        userText = new JTextField(25); // Increased width
        JLabel passLabel = new JLabel("Password:");
        passText = new JPasswordField(25); // Increased width

        JButton loginButton = new JButton("Login");
        styleButton(loginButton);
        logoutButton = new JButton("Logout");
        styleButton(logoutButton);
        logoutButton.setEnabled(false);

        JButton registerButton = new JButton("Register");
        styleButton(registerButton);

        // Add components to panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(userLabel, gbc);
        gbc.gridx = 1;
        panel.add(userText, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(passLabel, gbc);
        gbc.gridx = 1;
        panel.add(passText, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(loginButton, gbc);

        gbc.gridy = 3;
        panel.add(logoutButton, gbc);

        gbc.gridy = 4;
        panel.add(registerButton, gbc);

        // Add panel to frame
        add(panel, BorderLayout.CENTER);

        // Add action listener to login button
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = userText.getText();
                String password = new String(passText.getPassword());
                try {
                    String response = sendLoginRequest(username, password);
                    JSONObject jsonResponse = new JSONObject(response);
                    int status = jsonResponse.getInt("status");
                    if (status == 200) {
                        JSONObject responseObject = jsonResponse.getJSONObject("response");
                        userId = responseObject.getInt("userId");
                        JOptionPane.showMessageDialog(null, "Login successful!");
                        logoutButton.setEnabled(true);
                        if (userId == 1) {
                            AdminFrame adminFrame = new AdminFrame(username);
                            adminFrame.setVisible(true);
                        } else {
                            DocumentDisplayFrame documentDisplayFrame = new DocumentDisplayFrame(userId);
                            documentDisplayFrame.setVisible(true);
                        }
                        setVisible(false);
                    } else if (status == 400 && "User is already logged in".equals(jsonResponse.getString("error"))) {
                        JOptionPane.showMessageDialog(null, "User is already logged in.");
                    } else if (status == 405) {
                        JOptionPane.showMessageDialog(null, "Invalid username or password.");
                    } else if (status == 403) {
                        JOptionPane.showMessageDialog(null, "This account is banned");
                    } else {
                        JOptionPane.showMessageDialog(null, "Unknown error.");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Invalid username or password.");
                }
            }
        });

        // Add action listener to logout button
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = userText.getText();
                try {
                    String response = sendLogoutRequest(username);
                    JSONObject jsonResponse = new JSONObject(response);
                    int status = jsonResponse.getInt("status");
                    if (status == 200) {
                        JOptionPane.showMessageDialog(null, "Logout successful!");
                        logoutButton.setEnabled(false);
                        userText.setText("");
                        passText.setText("");
                    } else {
                        JOptionPane.showMessageDialog(null, "Logout failed.");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error connecting to server.");
                }
            }
        });

        // Add action listener to register button
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new RegisterFrame().setVisible(true);
            }
        });
    }

    private String sendLoginRequest(String username, String password) throws Exception {
        String url = "http://localhost:8000/api/login";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // Set up the connection properties
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");

        // Create the JSON request payload
        String jsonInputString = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";

        // Send the request
        con.setDoOutput(true);
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Read the response
        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }

    private String sendLogoutRequest(String username) throws Exception {
        String url = "http://localhost:8000/api/logout";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // Set up the connection properties
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");

        // Create the JSON request payload
        String jsonInputString = "{\"username\": \"" + username + "\"}";

        // Send the request
        con.setDoOutput(true);
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Read the response
        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }

    private void styleButton(JButton button) {
        button.setBackground(new Color(51, 153, 255));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
    }

    public static void main(String[] args) {
        // Create and display the frame
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LoginFrame().setVisible(true);
            }
        });
    }
}
