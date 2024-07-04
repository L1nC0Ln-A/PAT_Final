package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class UploadFrame extends JFrame {
    private int userId;
    private JTextField titleField;
    private JTextArea contentArea;

    public UploadFrame(int userId) {
        this.userId = userId;

        setTitle("New Document");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Title input
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(BorderFactory.createTitledBorder("Title"));
        titleField = new JTextField();
        titlePanel.add(titleField, BorderLayout.CENTER);
        add(titlePanel, BorderLayout.NORTH);

        // Content input
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createTitledBorder("Content"));
        contentArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(contentArea);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        // Save button
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveDocument();
            }
        });
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(saveButton, BorderLayout.LINE_START);
        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void saveDocument() {
        String title = titleField.getText().trim();
        String content = contentArea.getText().trim();

        if (title.isEmpty() || content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title and content cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Create the document file
            File file = new File(title + ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(content);
            }

            // Upload the document to the server
            uploadDocument(file, title);

        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving the document.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void uploadDocument(File file, String title) {
        try {
            URL url = new URL("http://localhost:8000/api/uploadDocument");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "multipart/form-data;boundary=*****");

            OutputStream os = con.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);

            // Send the owner_id field
            writer.append("--*****\r\n");
            writer.append("Content-Disposition: form-data; name=\"owner_id\"\r\n\r\n");
            writer.append(String.valueOf(userId)).append("\r\n");

            // Send the title field
            writer.append("--*****\r\n");
            writer.append("Content-Disposition: form-data; name=\"title\"\r\n\r\n");
            writer.append(title).append("\r\n");

            // Send the document file
            writer.append("--*****\r\n");
            writer.append("Content-Disposition: form-data; name=\"document\"; filename=\"").append(file.getName()).append("\"\r\n");
            writer.append("Content-Type: text/plain\r\n\r\n");
            writer.flush();

            // Read file and send
            try (FileInputStream inputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            writer.append("\r\n--*****--\r\n");
            writer.close();

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.getInt("status") == 200) {
                    JOptionPane.showMessageDialog(this, "Document uploaded successfully.");
                    dispose();
                } else {
                    String error = jsonResponse.optString("error", "Unknown error");
                    JOptionPane.showMessageDialog(this, "Failed to upload document: " + error, "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Failed to upload document. Server returned HTTP error: " + responseCode, "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error uploading the document.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}