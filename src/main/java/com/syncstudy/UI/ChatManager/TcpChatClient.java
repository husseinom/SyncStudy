package com.syncstudy.UI.ChatManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.syncstudy.BL.ChatManager.Message;
import com.syncstudy.BL.FileManager.SharedFile;
import com.syncstudy.BL.StudySessionManager.StudySession;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TcpChatClient with:
 * - TCP_NODELAY (disable Nagle)
 * - BufferedWriter + explicit flush
 * - synchronized send to avoid interleaving
 * - proper disconnect/cleanup
 * - simple file chunking (base64) helper
 */
public class TcpChatClient {
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private final Gson gson;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread readerThread;

    public TcpChatClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    public void connect(ChatController controller) throws IOException {
        if (running.get()) return;
        socket = new Socket(host, port);
        try {
            socket.setTcpNoDelay(true); // disable Nagle for lower latency
        } catch (Exception ignored) {}
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        running.set(true);

        readerThread = new Thread(() -> {
            System.out.println("TcpChatClient reader thread started for " + host + ":" + port);
            try {
                String line;
                while (running.get() && (line = in.readLine()) != null) {
                    System.out.println("Received line: " + line); // <-- ADD THIS
                    final String json = line;
                    try {
                        EventEnvelope env = gson.fromJson(json, EventEnvelope.class);
                        if (env.type != null && env.data != null) {
                            String dataJson = gson.toJson(env.data);
                            switch (env.type) {
                                case "file":
                                case "file-delete":
                                    env.data = gson.fromJson(dataJson, SharedFile.class);
                                    break;
                                case "session":
                                case "session-update":
                                case "session-vote":
                                case "session-register":
                                    env.data = gson.fromJson(dataJson, StudySession.class);
                                    break;
                            }
                        }
                        Platform.runLater(() -> controller.handleRemoteEnvelope(env));
                    } catch (Exception e) {
                        System.err.println("Failed to parse envelope: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                if (running.get()) System.err.println("Read error: " + e.getMessage());
            } finally {
                disconnect();
            }
        }, "TcpChatClient-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * Send an envelope as one JSON line. Synchronized and always flushed.
     */
    public synchronized void sendEvent(EventEnvelope envelope) {
        if (out == null) return;
        try {
            String json = gson.toJson(envelope);
            out.write(json);
            out.write("\n");
            out.flush();
        } catch (IOException e) {
            System.err.println("Send failed: " + e.getMessage());
            disconnect();
        }
    }

    /**
     * Send a file by chunking it into base64 frames. Adjust chunkSize as needed (e.g. 64KB).
     * The server simply broadcasts these envelopes; clients must reassemble.
     */
    public void sendFile(File file, int chunkSizeBytes) throws IOException {
        if (file == null || !file.exists()) throw new FileNotFoundException("File not found");
        try (FileInputStream fis = new FileInputStream(file)) {
            long total = file.length();
            int totalChunks = (int) ((total + chunkSizeBytes - 1) / chunkSizeBytes);
            byte[] buffer = new byte[chunkSizeBytes];
            int idx = 0;
            int read;
            while ((read = fis.read(buffer)) != -1) {
                idx++;
                byte[] actual = buffer;
                if (read != buffer.length) {
                    actual = new byte[read];
                    System.arraycopy(buffer, 0, actual, 0, read);
                }
                String b64 = Base64.getEncoder().encodeToString(actual);
                EventEnvelope env = new EventEnvelope();
                env.type = "file-chunk";
                env.fileName = file.getName();
                env.chunkIndex = idx;
                env.totalChunks = totalChunks;
                env.chunkData = b64;
                // optional: attach a small Message object or metadata
                sendEvent(env);
            }
        }
    }

    public synchronized void disconnect() {
        if (!running.getAndSet(false)) return;
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        in = null;
        out = null;
        socket = null;
        if (readerThread != null && readerThread.isAlive()) {
            readerThread.interrupt();
            readerThread = null;
        }
    }

    // Envelope shape extended for optional file transfer fields
    public static class EventEnvelope {
        public String type; // "new", "edit", "delete", "file-chunk", ...
        public Message message;
        public Long id;
        public Object data;

        // file transfer fields (optional)
        public String fileName;
        public Integer chunkIndex;
        public Integer totalChunks;
        public String chunkData; // base64 payload

        public EventEnvelope() {}
        public EventEnvelope(String type, Message message, Long id) {
            this.type = type;
            this.message = message;
            this.id = id;
        }
    }
}