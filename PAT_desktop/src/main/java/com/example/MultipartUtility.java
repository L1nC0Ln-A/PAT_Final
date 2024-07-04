package com.example;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MultipartUtility {

    private final String boundary;
    private static final String LINE_FEED = "\r\n";
    private HttpURLConnection httpConn;
    private OutputStream outputStream;
    private PrintWriter writer;

    /**
     * Initializes a new HTTP POST request with content type is set to multipart/form-data.
     *
     * @param requestURL The URL for the POST request.
     * @param charset    The charset to be used in the request.
     * @throws IOException If an I/O exception occurs.
     */
    public MultipartUtility(String requestURL, String charset) throws IOException {
        boundary = "===" + UUID.randomUUID().toString() + "===";

        URL url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setDoOutput(true);
        httpConn.setUseCaches(false);
        httpConn.setRequestMethod("POST");
        httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        httpConn.setRequestProperty("User-Agent", "Mozilla/5.0");
        httpConn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        outputStream = httpConn.getOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);
    }

    /**
     * Adds a form field to the request.
     *
     * @param name  Field name.
     * @param value Field value.
     */
    public void addFormField(String name, String value) {
        writer.append("--").append(boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"").append(name).append("\"").append(LINE_FEED);
        writer.append("Content-Type: text/plain; charset=").append(StandardCharsets.UTF_8.name()).append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value).append(LINE_FEED);
        writer.flush();
    }

    /**
     * Adds a file part to the request.
     *
     * @param fieldName   Name attribute in <input type="file" name="..." />.
     * @param uploadFile  The file to be uploaded.
     * @throws IOException If an I/O exception occurs.
     */
    public void addFilePart(String fieldName, File uploadFile) throws IOException {
        String fileName = uploadFile.getName();
        writer.append("--").append(boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"").append(fieldName)
                .append("\"; filename=\"").append(fileName).append("\"").append(LINE_FEED);
        writer.append("Content-Type: ").append(HttpURLConnection.guessContentTypeFromName(fileName)).append(LINE_FEED);
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();

        try (FileInputStream inputStream = new FileInputStream(uploadFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }

        writer.append(LINE_FEED);
        writer.flush();
    }

    /**
     * Completes the request and receives response from the server.
     *
     * @return The server's response as a string.
     * @throws IOException If an I/O exception occurs.
     */
    public String finish() throws IOException {
        StringBuilder response = new StringBuilder();

        writer.append(LINE_FEED).flush();
        writer.append("--").append(boundary).append("--").append(LINE_FEED);
        writer.close();

        int status = httpConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
        } else {
            throw new IOException("Server returned non-OK status: " + status);
        }

        httpConn.disconnect();
        return response.toString();
    }
}
