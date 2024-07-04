package com.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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

public class DocumentDisplayFrame extends JFrame {

    private int userId;
    private JPanel documentPanel;

    public DocumentDisplayFrame(int userId) {
        this.userId = userId;

        // Set Nimbus look and feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("Documents for User ID: " + userId);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        // Panel to hold documents
        documentPanel = new JPanel();
        documentPanel.setLayout(new BoxLayout(documentPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(documentPanel);

        // Fetch and display documents
        fetchDocuments();

        // Add components to frame
        add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new BorderLayout());

        // Logout button
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogout();
            }
        });
        buttonPanel.add(logoutButton, BorderLayout.LINE_END);

        // Upload button
        JButton uploadButton = new JButton("Upload");
        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new UploadFrame(userId).setVisible(true);
            }
        });
        buttonPanel.add(uploadButton, BorderLayout.LINE_START);

        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setPreferredSize(new Dimension(100, 30));
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fetchDocuments();
            }
        });
        buttonPanel.add(refreshButton, BorderLayout.CENTER);

        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void fetchDocuments() {
        documentPanel.removeAll(); // Clear existing documents
        try {
            URL url = new URL("http://localhost:8000/api/documentsByOwner/" + userId);
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

                JSONArray documents = new JSONArray(response.toString());

                for (int i = 0; i < documents.length(); i++) {
                    JSONObject doc = documents.getJSONObject(i);
                    int docId = doc.getInt("doc_id");
                    String title = doc.getString("title");

                    JPanel docPanel = new JPanel(new BorderLayout());
                    JLabel titleLabel = new JLabel(title);
                    titleLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
                    docPanel.add(titleLabel, BorderLayout.CENTER);

                    // Button panel for edit and delete buttons
                    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));

                    // Edit button
                    JButton editButton = new JButton("Edit");
                    editButton.setPreferredSize(new Dimension(70, 30));
                    editButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            fetchAndEditDocument(docId, title);
                        }
                    });
                    buttonPanel.add(editButton);

                    // Delete button
                    JButton deleteButton = new JButton("Delete");
                    deleteButton.setPreferredSize(new Dimension(70, 30));
                    deleteButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            deleteDocument(docId);
                        }
                    });
                    buttonPanel.add(deleteButton);

                    docPanel.add(buttonPanel, BorderLayout.LINE_END);

                    documentPanel.add(docPanel);
                }

                revalidate();
                repaint();
            
            } else if (responseCode != 404) {
                JOptionPane.showMessageDialog(null, "Failed to fetch documents. Server returned HTTP error: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error fetching documents: " + e.getMessage());
        }
    }

    private void fetchAndEditDocument(int docId, String title) {
        try {
            URL url = new URL("http://localhost:8000/api/getDocumentContent/" + docId);
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

                JSONObject jsonResponse = new JSONObject(response.toString());
                String content = jsonResponse.getString("response");

                new EditFrame(userId, docId, title, content).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(null, "Failed to fetch document content. Server returned HTTP error: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error fetching document content: " + e.getMessage());
        }
    }

    private void deleteDocument(int docId) {
        try {
            URL url = new URL("http://localhost:8000/api/deleteDocument/" + docId);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("DELETE");
            con.setRequestProperty("Content-Type", "application/json");

            String jsonInputString = new JSONObject().put("owner_id", userId).toString();

            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                JOptionPane.showMessageDialog(null, "Document deleted successfully!");
                fetchDocuments(); // Refresh the document list
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                String error = jsonResponse.optString("error", "Unknown error");
                JOptionPane.showMessageDialog(null, "Failed to delete document: " + error);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error deleting document: " + e.getMessage());
        }
    }

    private void handleLogout() {
        try {
            String response = sendLogoutRequest(userId);
            JSONObject jsonResponse = new JSONObject(response);
            int status = jsonResponse.getInt("status");
            if (status == 200) {
                JOptionPane.showMessageDialog(null, "Logout successful!");
                dispose(); // Close document display frame
                new LoginFrame().setVisible(true); // Show login frame again
            } else {
                String error = jsonResponse.optString("error", "Unknown error");
                JOptionPane.showMessageDialog(null, "Failed to logout: " + error);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error logging out: " + e.getMessage());
        }
    }

    private String sendLogoutRequest(int userId) throws Exception {
        URL url = new URL("http://localhost:8000/api/logout");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");

        String jsonInputString = new JSONObject().put("userId", userId).toString();

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Example usage
                new DocumentDisplayFrame(1).setVisible(true);
            }
        });
    }
}