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

public class RegisterFrame extends JFrame {

    // Constructor
    public RegisterFrame() {
        // Set frame properties
        setTitle("Register Page");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create panel to hold components
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2));

        // Create components
        JLabel userLabel = new JLabel("Username:");
        final JTextField userText = new JTextField(20);

        JLabel passLabel = new JLabel("Password:");
        final JPasswordField passText = new JPasswordField(20);

        JButton registerButton = new JButton("Register");

        // Add components to panel
        panel.add(userLabel);
        panel.add(userText);
        panel.add(passLabel);
        panel.add(passText);
        panel.add(registerButton);

        // Add panel to frame
        add(panel, BorderLayout.CENTER);

        // Add action listener to register button
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = userText.getText();
                String password = new String(passText.getPassword());
                try {
                    String response = sendRegisterRequest(username, password);
                    JSONObject jsonResponse = new JSONObject(response);
                    int status = jsonResponse.getInt("status");
                    if (status == 200) {
                        JOptionPane.showMessageDialog(null, "Registration successful!");
                        dispose(); // Close the registration frame
                    } else {
                        JOptionPane.showMessageDialog(null, "Registration failed: " + jsonResponse.getString("error"));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error connecting to server.");
                }
            }
        });
    }

    private String sendRegisterRequest(String username, String password) throws Exception {
        String url = "http://localhost:8000/api/registerUser";
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
}
