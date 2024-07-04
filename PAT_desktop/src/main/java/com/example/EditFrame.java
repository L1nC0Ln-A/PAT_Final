package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class EditFrame extends JFrame {
    private int userId;
    private int docId;
    private String title;
    private String content;
    private JTextArea contentArea;
    private JTextField titleField;

    public EditFrame(int userId, int docId, String title, String content) {
        this.userId = userId;
        this.docId = docId;
        this.title = title;
        this.content = content;

        setTitle("Edit Document");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        // Title field
        JLabel titleLabel = new JLabel("Title:");
        titleField = new JTextField(title, 20);

        // Content area
        JLabel contentLabel = new JLabel("Content:");
        contentArea = new JTextArea(content, 10, 30);
        JScrollPane scrollPane = new JScrollPane(contentArea);

        // Save button
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveDocument();
            }
        });

        // Layout
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout());
        titlePanel.add(titleLabel);
        titlePanel.add(titleField);

        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(contentLabel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(saveButton, BorderLayout.SOUTH);

        add(panel);
        setVisible(true);
    }

    private void saveDocument() {
        String newTitle = titleField.getText();
        String newContent = contentArea.getText();

        try {
            URL url = new URL("http://localhost:8000/api/editDocument/" + docId);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            JSONObject jsonInput = new JSONObject();
            jsonInput.put("title", newTitle);
            jsonInput.put("content", newContent);
            jsonInput.put("owner_id", userId);

            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInput.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject responseJson = new JSONObject(response.toString());
                if (responseJson.getInt("status") == 200) {
                    JOptionPane.showMessageDialog(this, "Document saved successfully!");
                    dispose();
                } else {
                    String error = responseJson.optString("error", "Unknown error");
                    JOptionPane.showMessageDialog(this, "Failed to save document: " + error);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save document. Server returned HTTP error: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving document: " + e.getMessage());
        }
    }
}