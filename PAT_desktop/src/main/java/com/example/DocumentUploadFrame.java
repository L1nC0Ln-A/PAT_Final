package com.example;

import javax.swing.*;

import org.json.JSONObject;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class DocumentUploadFrame extends JFrame {

    private String username;
    private JTextField titleText;
    private JTextArea documentTextArea;

    public DocumentUploadFrame(String username) {
        this.username = username;

        setTitle("New Document");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        titleText = new JTextField(20);
        documentTextArea = new JTextArea(10, 20);

        JButton uploadButton = new JButton("Save");
        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String title = titleText.getText();
                String content = documentTextArea.getText();
                if (title.isEmpty() || content.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Title and document content cannot be empty.");
                } else {
                    try {
                        uploadDocument(title, content);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Error saving document: " + ex.getMessage());
                    }
                }
            }
        });

        panel.add(new JLabel("Title:"), BorderLayout.NORTH);
        panel.add(titleText, BorderLayout.CENTER);
        panel.add(new JScrollPane(documentTextArea), BorderLayout.CENTER);
        panel.add(uploadButton, BorderLayout.SOUTH);

        add(panel);
    }

    private void uploadDocument(String title, String content) throws Exception {
        String url = "http://localhost:8000/api/uploadDocument";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // Set up the connection properties
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");

        // Create the JSON request payload
        JSONObject jsonInput = new JSONObject();
        jsonInput.put("owner", username);
        jsonInput.put("title", title);

        // Write content to a temporary file
        File tempFile = File.createTempFile("document", ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write(content);
        }

        // Send the request with multipart/form-data
        con.setDoOutput(true);
        try (OutputStream os = con.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true)) {

            // Send the title and owner as JSON
            writer.append("--boundary\r\n");
            writer.append("Content-Disposition: form-data; name=\"metadata\"\r\n");
            writer.append("Content-Type: application/json\r\n\r\n");
            writer.append(jsonInput.toString()).append("\r\n");

            // Send the file
            writer.append("--boundary\r\n");
            writer.append("Content-Disposition: form-data; name=\"document\"; filename=\"")
                  .append(tempFile.getName()).append("\"\r\n");
            writer.append("Content-Type: text/plain\r\n\r\n");
            writer.flush();
            try (FileInputStream inputStream = new FileInputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
            writer.append("\r\n--boundary--\r\n");
            writer.flush();
        }

        // Read the response
        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            JSONObject jsonResponse = new JSONObject(response.toString());
            int status = jsonResponse.getInt("status");
            if (status == 200) {
                JOptionPane.showMessageDialog(null, "Document saved successfully!");
                dispose();
            } else {
                JOptionPane.showMessageDialog(null, "Failed to save document: " + jsonResponse.getString("error"));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new DocumentUploadFrame("user123").setVisible(true); // Replace with actual username
            }
        });
    }
}
