import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class Server extends JFrame {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private final ExecutorService threadPool;
    private final Map<String, ClientHandler> connectedClients;
    private final Map<String, String> clientUsernames; // clientId -> username
    private final Map<String, Long> clientConnectTimes; // clientId -> connect epoch
    private final Map<String, Integer> clientRowIndex; // clientId -> row index in table
    private final Map<String, Integer> clientMessageCounts; // clientId -> messages sent

    // UI Components
    private JTextArea logArea;
    private JButton startButton, stopButton, clearLogButton, banUserButton;
    private JLabel statusLabel, portLabel, uptimeLabel, clientCountLabel;
    private JTable clientTable;
    private DefaultTableModel clientTableModel;
    private JProgressBar memoryBar;
    private JTabbedPane mainTabs;
    private JTextField broadcastField;
    private JButton broadcastButton;
    private JSpinner portSpinner;
    private JToggleButton autoScrollToggle;
    private JSlider logLevelSlider;

    // Stats tracking
    private long serverStartTime;
    private int totalConnectionsEver = 0;

    private DateTimeFormatter timeFormatter;
    private javax.swing.Timer uiUpdateTimer;
    private ScheduledExecutorService heartbeatScheduler;
    private JLabel totalMessagesStatLabel;

    public Server() {
        connectedClients = new ConcurrentHashMap<>();
        clientUsernames = new ConcurrentHashMap<>();
        clientConnectTimes = new ConcurrentHashMap<>();
        clientRowIndex = new ConcurrentHashMap<>();
        clientMessageCounts = new ConcurrentHashMap<>();
        threadPool = Executors.newCachedThreadPool();
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        // Set simple look and feel

        initializeGUI();
        startUIUpdateTimer();
    }

    private void initializeGUI() {
        setTitle("🚀 Advanced Chat Server Control Panel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Set custom colors
        Color primaryColor = new Color(25, 118, 210);
        Color backgroundColor = new Color(245, 245, 245);

        getContentPane().setBackground(backgroundColor);

        // Create top toolbar
        createToolbar(primaryColor);

        // Create tabbed interface
        createTabbedInterface();

        // Create status bar
        createStatusBar(primaryColor);

        setSize(1200, 800);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 600));

        // Add window listener for cleanup
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (isRunning) {
                    stopServer();
                }
                if (uiUpdateTimer != null) {
                    uiUpdateTimer.stop();
                }
                System.exit(0);
            }
        });
    }

    private void startServer() {
        if (isRunning)
            return;

        int selectedPort = (Integer) portSpinner.getValue();

        threadPool.execute(() -> {
            try {
                serverSocket = new ServerSocket(selectedPort);
                isRunning = true;
                serverStartTime = System.currentTimeMillis();

                SwingUtilities.invokeLater(() -> {
                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                    broadcastButton.setEnabled(true);
                    statusLabel.setText("✅ Server Running on port " + selectedPort);
                    statusLabel.setForeground(new Color(76, 175, 80));
                    portLabel.setText("🔌 Port: " + selectedPort);

                    String startMsg = "[" + LocalDateTime.now().format(timeFormatter) + "] 🚀 Server started on port "
                            + selectedPort;
                    logArea.append(startMsg + "\n");
                    if (autoScrollToggle.isSelected()) {
                        logArea.setCaretPosition(logArea.getDocument().getLength());
                    }
                });

                // Start heartbeat scheduler
                startHeartbeat();

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    String clientId = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
                    totalConnectionsEver++;

                    SwingUtilities.invokeLater(() -> {
                        String connectMsg = "[" + LocalDateTime.now().format(timeFormatter)
                                + "] 🔗 New client connected: " + clientId;
                        logArea.append(connectMsg + "\n");
                        if (autoScrollToggle.isSelected()) {
                            logArea.setCaretPosition(logArea.getDocument().getLength());
                        }
                    });

                    ClientHandler handler = new ClientHandler(clientSocket, clientId);
                    connectedClients.put(clientId, handler);
                    clientConnectTimes.put(clientId, System.currentTimeMillis());
                    threadPool.execute(handler);
                }
            } catch (IOException e) {
                if (isRunning) {
                    SwingUtilities.invokeLater(() -> {
                        String errorMsg = "[" + LocalDateTime.now().format(timeFormatter) + "] ❌ Server error: "
                                + e.getMessage();
                        logArea.append(errorMsg + "\n");
                        if (autoScrollToggle.isSelected()) {
                            logArea.setCaretPosition(logArea.getDocument().getLength());
                        }
                    });
                }
            }
        });
    }

    private void stopServer() {
        if (!isRunning)
            return;

        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }

            // Notify all clients about server shutdown
            broadcastMessage("Server is shutting down...", "server", "SYSTEM");

            for (ClientHandler handler : connectedClients.values()) {
                handler.disconnect();
            }
            connectedClients.clear();
            clientUsernames.clear();
            clientRowIndex.clear();
            clientMessageCounts.clear();
        } catch (IOException e) {
            logArea.append("Error stopping server: " + e.getMessage() + "\n");
        }

        SwingUtilities.invokeLater(() -> {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            statusLabel.setText("Server Stopped");
            logArea.append("[" + LocalDateTime.now().format(timeFormatter) + "] Server stopped\n");
        });

        stopHeartbeat();
    }

    private void broadcastMessage(String message, String senderId, String messageType) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        String formattedMessage;

        switch (messageType) {
            case "CHAT" -> {
                String username = clientUsernames.getOrDefault(senderId, "Server");
                formattedMessage = "CHAT|" + timestamp + "|" + username + "|" + message;
            }
            case "JOIN" -> formattedMessage = "JOIN|" + timestamp + "|" + message;
            case "LEAVE" -> formattedMessage = "LEAVE|" + timestamp + "|" + message;
            case "SYSTEM" -> formattedMessage = "SYSTEM|" + timestamp + "|" + message;
            case "TYPING" -> formattedMessage = "TYPING|" + timestamp + "|" + message; // message carries
                                                                                       // username|true/false
            default -> formattedMessage = "CHAT|" + timestamp + "|Server|" + message;
        }

        // Send to all connected clients
        for (ClientHandler handler : connectedClients.values()) {
            handler.sendMessage(formattedMessage);
        }
    }

    private void broadcastUserList() {
        String userList = String.join(",", clientUsernames.values());
        String message = "USERLIST|" + LocalDateTime.now().format(timeFormatter) + "|" + userList;

        for (ClientHandler handler : connectedClients.values()) {
            handler.sendMessage(message);
        }
    }

    private void sendPrivateMessage(String toUsername, String fromClientId, String content) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        String fromUser = clientUsernames.getOrDefault(fromClientId, "Unknown");
        ClientHandler toHandler = null;
        String toClientId = null;
        for (Map.Entry<String, String> e : clientUsernames.entrySet()) {
            if (e.getValue().equalsIgnoreCase(toUsername)) {
                toClientId = e.getKey();
                break;
            }
        }
        if (toClientId != null) {
            toHandler = connectedClients.get(toClientId);
        }

        if (toHandler != null) {
            String msg = "PRIVATE|" + timestamp + "|" + fromUser + "|" + toUsername + "|" + content;
            toHandler.sendMessage(msg);
            ClientHandler fromHandler = connectedClients.get(fromClientId);
            if (fromHandler != null) {
                fromHandler.sendMessage(msg); // echo to sender
                incrementMessageCount(fromClientId);
            }
            SwingUtilities.invokeLater(() -> logArea
                    .append("[" + timestamp + "] [PM] " + fromUser + " -> " + toUsername + ": " + content + "\n"));
        } else {
            ClientHandler fromHandler = connectedClients.get(fromClientId);
            if (fromHandler != null) {
                fromHandler.sendMessage("SYSTEM|" + timestamp + "|User '" + toUsername + "' not found");
            }
        }
    }

    // Inner class to handle client connections
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final String clientId;
        private BufferedReader input;
        private PrintWriter output;
        private boolean isConnected = true;
        private String username;
        private long lastPongTime = System.currentTimeMillis();

        public ClientHandler(Socket socket, String clientId) {
            this.socket = socket;
            this.clientId = clientId;
            this.username = "User" + (connectedClients.size() + 1); // Default username
        }

        @Override
        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);

                // Wait for username from client
                String usernameMessage = input.readLine();
                if (usernameMessage != null && usernameMessage.startsWith("USERNAME|")) {
                    this.username = usernameMessage.substring(9);
                }

                // Store username
                clientUsernames.put(clientId, username);
                clientMessageCounts.put(clientId, 0);

                // Add to client table
                SwingUtilities.invokeLater(() -> addClientToTable(clientId, username));

                // Send welcome message and notify others
                sendMessage("SYSTEM|" + LocalDateTime.now().format(timeFormatter) + "|Welcome to the chat, " + username
                        + "!");
                broadcastMessage(username + " joined the chat", clientId, "JOIN");
                broadcastUserList();

                SwingUtilities.invokeLater(() -> {
                    logArea.append("[" + LocalDateTime.now().format(timeFormatter) + "] " + username + " (" + clientId
                            + ") joined\n");
                });

                String message;
                while (isConnected && (message = input.readLine()) != null) {
                    final String finalMessage = message;
                    SwingUtilities.invokeLater(() -> {
                        logArea.append("[" + LocalDateTime.now().format(timeFormatter) + "] [" + username + "]: "
                                + finalMessage + "\n");
                    });

                    if (message.startsWith("PONG")) {
                        lastPongTime = System.currentTimeMillis();
                        continue;
                    } else if (message.startsWith("TYPING|")) {
                        // Relay typing state to others: expected format TYPING|username|true/false
                        String typingPayload = message.substring(7); // username|true
                        broadcastMessage(typingPayload, clientId, "TYPING");
                        continue;
                    } else if (message.startsWith("/w ") || message.startsWith("/pm ")) {
                        // Private message: /w username message
                        String[] parts = message.split(" ", 3);
                        if (parts.length >= 3) {
                            String toUser = parts[1];
                            String content = parts[2];
                            sendPrivateMessage(toUser, clientId, content);
                        } else {
                            sendMessage("SYSTEM|" + LocalDateTime.now().format(timeFormatter)
                                    + "|Usage: /w <username> <message>");
                        }
                        continue;
                    } else if (message.equalsIgnoreCase("/help")) {
                        sendMessage("SYSTEM|" + LocalDateTime.now().format(timeFormatter)
                                + "|Available commands: /help, /list, /quit, /w <user> <msg>, /broadcast <msg>, /file <name>");
                        continue;
                    } else if (message.startsWith("/broadcast ")) {
                        String broadcastMsg = message.substring(11);
                        broadcastMessage(broadcastMsg, clientId, "CHAT");
                        incrementMessageCount(clientId);
                    } else if (message.equalsIgnoreCase("/list")) {
                        sendMessage("SYSTEM|" + LocalDateTime.now().format(timeFormatter) + "|Connected users: "
                                + String.join(", ", clientUsernames.values()));
                    } else if (message.equalsIgnoreCase("/quit")) {
                        break;
                    } else if (message.startsWith("/file ")) {
                        // Handle file transfer
                        broadcastMessage("sent a file: " + message.substring(6), clientId, "SYSTEM");
                        incrementMessageCount(clientId);
                    } else {
                        // Regular chat message
                        broadcastMessage(message, clientId, "CHAT");
                        incrementMessageCount(clientId);
                    }
                }
            } catch (IOException e) {
                if (isConnected) {
                    SwingUtilities.invokeLater(() -> {
                        logArea.append("[" + LocalDateTime.now().format(timeFormatter) + "] Client " + clientId
                                + " error: " + e.getMessage() + "\n");
                    });
                }
            } finally {
                disconnect();
            }
        }

        public void sendMessage(String message) {
            if (output != null) {
                output.println(message);
            }
        }

        public void disconnect() {
            if (!isConnected)
                return;

            isConnected = false;
            String leavingUsername = clientUsernames.get(clientId);
            if (leavingUsername != null) {
                broadcastMessage(leavingUsername + " left the chat", clientId, "LEAVE");
            }

            // Clean up
            connectedClients.remove(clientId);
            clientUsernames.remove(clientId);

            // Update user list for remaining clients
            broadcastUserList();

            SwingUtilities.invokeLater(() -> {
                updateClientRowOnDisconnect(clientId, leavingUsername);
            });

            try {
                if (input != null)
                    input.close();
                if (output != null)
                    output.close();
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void createToolbar(Color primaryColor) {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(primaryColor);
        toolbar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 2, 0, primaryColor.darker()),
                new EmptyBorder(10, 15, 10, 15)));

        // Left side - Server controls
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        startButton = new JButton("▶ Start Server");
        stopButton = new JButton("⏹ Stop Server");
        clearLogButton = new JButton("🗑 Clear Log");

        // Style buttons
        styleToolbarButton(startButton, new Color(76, 175, 80));
        styleToolbarButton(stopButton, new Color(244, 67, 54));
        styleToolbarButton(clearLogButton, new Color(158, 158, 158));

        stopButton.setEnabled(false);

        // Port selection
        portSpinner = new JSpinner(new SpinnerNumberModel(PORT, 1024, 65535, 1));
        portSpinner.setPreferredSize(new Dimension(80, 30));

        leftPanel.add(new JLabel("🌐 Port:"));
        leftPanel.add(portSpinner);
        leftPanel.add(Box.createHorizontalStrut(10));
        leftPanel.add(startButton);
        leftPanel.add(stopButton);
        leftPanel.add(clearLogButton);

        // Right side - Status indicators
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        statusLabel = new JLabel("⚪ Server Stopped");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        rightPanel.add(statusLabel);

        toolbar.add(leftPanel, BorderLayout.WEST);
        toolbar.add(rightPanel, BorderLayout.EAST);

        // Add action listeners
        startButton.addActionListener(_ -> startServer());
        stopButton.addActionListener(_ -> stopServer());
        clearLogButton.addActionListener(_ -> logArea.setText(""));

        add(toolbar, BorderLayout.NORTH);
    }

    private void createTabbedInterface() {
        mainTabs = new JTabbedPane(JTabbedPane.TOP);
        mainTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Dashboard Tab
        mainTabs.addTab("📊 Dashboard", createDashboardTab());

        // Server Log Tab
        mainTabs.addTab("📄 Server Log", createLogTab());

        // Client Management Tab
        mainTabs.addTab("👥 Clients", createClientTab());

        // Broadcast Tab
        mainTabs.addTab("📢 Broadcast", createBroadcastTab());

        // Settings Tab
        mainTabs.addTab("⚙️ Settings", createSettingsTab());

        add(mainTabs, BorderLayout.CENTER);
    }

    private JPanel createDashboardTab() {
        JPanel dashboard = new JPanel(new BorderLayout(15, 15));
        dashboard.setBorder(new EmptyBorder(20, 20, 20, 20));
        dashboard.setBackground(Color.WHITE);

        // Stats Panel
        JPanel statsPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        statsPanel.setBackground(Color.WHITE);

        // Create stat cards
        statsPanel.add(createStatCard("👥 Connected Clients", "0", new Color(33, 150, 243)));
        statsPanel.add(createStatCard("💬 Total Messages", "0", new Color(76, 175, 80)));
        statsPanel.add(createStatCard("⏱ Server Uptime", "00:00:00", new Color(156, 39, 176)));
        statsPanel.add(createStatCard("🔌 Port Status", String.valueOf(PORT), new Color(255, 152, 0)));
        statsPanel.add(createStatCard("📦 Memory Usage", "0 MB", new Color(244, 67, 54)));
        statsPanel.add(createStatCard("🔄 Total Connections", "0", new Color(0, 150, 136)));

        dashboard.add(statsPanel, BorderLayout.NORTH);

        // Real-time activity feed
        JPanel activityPanel = new JPanel(new BorderLayout());
        activityPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(200, 200, 200)),
                "🟢 Live Activity Feed",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14)));
        activityPanel.setBackground(Color.WHITE);

        JTextArea activityFeed = new JTextArea(15, 40);
        activityFeed.setEditable(false);
        activityFeed.setFont(new Font("Consolas", Font.PLAIN, 11));
        activityFeed.setBackground(new Color(248, 249, 250));
        activityFeed.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane activityScroll = new JScrollPane(activityFeed);
        activityScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        activityPanel.add(activityScroll, BorderLayout.CENTER);

        dashboard.add(activityPanel, BorderLayout.CENTER);

        return dashboard;
    }

    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(color, 2),
                new EmptyBorder(15, 15, 15, 15)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(100, 100, 100));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(color);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        if (title.contains("Total Messages")) {
            totalMessagesStatLabel = valueLabel;
        }

        return card;
    }

    private void styleToolbarButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setBorder(new EmptyBorder(8, 15, 8, 15));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });
    }

    private JPanel createLogTab() {
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Log controls
        JPanel logControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        autoScrollToggle = new JToggleButton("🔄 Auto-scroll", true);
        logControls.add(autoScrollToggle);

        logPanel.add(logControls, BorderLayout.NORTH);

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBackground(new Color(248, 249, 250));
        logArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logPanel.add(logScroll, BorderLayout.CENTER);

        return logPanel;
    }

    private JPanel createClientTab() {
        JPanel clientPanel = new JPanel(new BorderLayout());
        clientPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Client table
        String[] columns = { "Username", "IP Address", "Connect Time", "Status", "Messages Sent" };
        clientTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        clientTable = new JTable(clientTableModel);
        clientTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clientTable.setRowHeight(25);
        clientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane clientScroll = new JScrollPane(clientTable);
        clientPanel.add(clientScroll, BorderLayout.CENTER);

        // Client actions
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        banUserButton = new JButton("⛔ Kick User");
        banUserButton.setEnabled(false);
        actionPanel.add(banUserButton);

        clientPanel.add(actionPanel, BorderLayout.SOUTH);

        // Selection listener to enable kick button
        clientTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting())
                return;
            boolean hasSelection = clientTable.getSelectedRow() >= 0;
            banUserButton.setEnabled(hasSelection);
        });

        // Kick selected user
        banUserButton.addActionListener(_ -> kickSelectedClient());

        return clientPanel;
    }

    private JPanel createBroadcastTab() {
        JPanel broadcastPanel = new JPanel(new BorderLayout());
        broadcastPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("📢 Send Message to All Clients");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        broadcastPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        broadcastField = new JTextField();
        broadcastField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        broadcastField.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 200, 200)),
                new EmptyBorder(10, 10, 10, 10)));

        broadcastButton = new JButton("📤 Send Broadcast");
        broadcastButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        broadcastButton.setBackground(new Color(33, 150, 243));
        broadcastButton.setForeground(Color.WHITE);
        broadcastButton.setBorder(new EmptyBorder(10, 20, 10, 20));
        broadcastButton.setEnabled(false);

        inputPanel.add(broadcastField, BorderLayout.CENTER);
        inputPanel.add(broadcastButton, BorderLayout.EAST);

        broadcastPanel.add(inputPanel, BorderLayout.CENTER);

        // Wire broadcast action
        broadcastButton.addActionListener(_ -> {
            String msg = broadcastField.getText().trim();
            if (!msg.isEmpty()) {
                broadcastMessage(msg, "server", "CHAT");
                String ts = LocalDateTime.now().format(timeFormatter);
                logArea.append("[" + ts + "] [Broadcast]: " + msg + "\n");
                broadcastField.setText("");
            }
        });
        // Enter to send
        broadcastField.addActionListener(_ -> broadcastButton.doClick());

        return broadcastPanel;
    }

    private JPanel createSettingsTab() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("⚙️ Server Settings");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(titleLabel);

        settingsPanel.add(Box.createVerticalStrut(20));

        // Log level setting
        JPanel logLevelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        logLevelPanel.add(new JLabel("Log Level:"));
        logLevelSlider = new JSlider(1, 5, 3);
        logLevelSlider.setMajorTickSpacing(1);
        logLevelSlider.setPaintTicks(true);
        logLevelSlider.setPaintLabels(true);
        logLevelPanel.add(logLevelSlider);
        logLevelPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(logLevelPanel);

        return settingsPanel;
    }

    private void createStatusBar(Color primaryColor) {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(primaryColor.darker());
        statusBar.setBorder(new EmptyBorder(5, 10, 5, 10));

        JPanel leftStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftStatus.setOpaque(false);

        portLabel = new JLabel("🔌 Port: " + PORT);
        uptimeLabel = new JLabel("⏱ Uptime: 00:00:00");
        clientCountLabel = new JLabel("👥 Clients: 0");

        portLabel.setForeground(Color.WHITE);
        uptimeLabel.setForeground(Color.WHITE);
        clientCountLabel.setForeground(Color.WHITE);

        leftStatus.add(portLabel);
        leftStatus.add(uptimeLabel);
        leftStatus.add(clientCountLabel);

        // Memory usage
        JPanel rightStatus = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightStatus.setOpaque(false);

        memoryBar = new JProgressBar(0, 100);
        memoryBar.setStringPainted(true);
        memoryBar.setString("Memory: 0%");
        memoryBar.setPreferredSize(new Dimension(120, 20));
        rightStatus.add(memoryBar);

        statusBar.add(leftStatus, BorderLayout.WEST);
        statusBar.add(rightStatus, BorderLayout.EAST);

        add(statusBar, BorderLayout.SOUTH);
    }

    private void startUIUpdateTimer() {
        uiUpdateTimer = new javax.swing.Timer(1000, _ -> updateUIStats());
        uiUpdateTimer.start();
    }

    private void updateUIStats() {
        if (isRunning && serverStartTime > 0) {
            long uptime = System.currentTimeMillis() - serverStartTime;
            long hours = uptime / (1000 * 60 * 60);
            long minutes = (uptime / (1000 * 60)) % 60;
            long seconds = (uptime / 1000) % 60;

            uptimeLabel.setText(String.format("⏱ Uptime: %02d:%02d:%02d", hours, minutes, seconds));
        }

        clientCountLabel.setText("👥 Clients: " + connectedClients.size());

        // Update memory usage
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        int memoryPercent = (int) ((usedMemory * 100) / maxMemory);

        memoryBar.setValue(memoryPercent);
        memoryBar.setString("Memory: " + memoryPercent + "%");
    }

    private void addClientToTable(String clientId, String username) {
        String ip = clientId.split(":")[0];
        String connectTime = LocalDateTime.now().format(timeFormatter);
        Object[] row = { username, ip, connectTime, "Online", 0 };
        int rowIndex = clientTableModel.getRowCount();
        clientTableModel.addRow(row);
        clientRowIndex.put(clientId, rowIndex);
        updateClientCountLabel();
    }

    private void updateClientRowOnDisconnect(String clientId, String username) {
        Integer row = clientRowIndex.get(clientId);
        if (row != null && row >= 0 && row < clientTableModel.getRowCount()) {
            clientTableModel.setValueAt("Offline", row, 3);
        }
        String ts = LocalDateTime.now().format(timeFormatter);
        logArea.append("[" + ts + "] " + (username != null ? username : clientId) + " disconnected\n");
        updateClientCountLabel();
    }

    private void incrementMessageCount(String clientId) {
        clientMessageCounts.merge(clientId, 1, Integer::sum);
        if (totalMessagesStatLabel != null) {
            int sum = 0;
            for (int c : clientMessageCounts.values())
                sum += c;
            totalMessagesStatLabel.setText(String.valueOf(sum));
        }
        Integer row = clientRowIndex.get(clientId);
        if (row != null && row < clientTableModel.getRowCount()) {
            clientTableModel.setValueAt(clientMessageCounts.get(clientId), row, 4);
        }
    }

    private void updateClientCountLabel() {
        clientCountLabel.setText("👥 Clients: " + connectedClients.size());
    }

    private void kickSelectedClient() {
        int row = clientTable.getSelectedRow();
        if (row < 0)
            return;
        String username = (String) clientTableModel.getValueAt(row, 0);
        String clientIdToKick = null;
        for (Map.Entry<String, String> e : clientUsernames.entrySet()) {
            if (e.getValue().equals(username)) {
                clientIdToKick = e.getKey();
                break;
            }
        }
        if (clientIdToKick != null) {
            ClientHandler handler = connectedClients.get(clientIdToKick);
            if (handler != null) {
                handler.sendMessage(
                        "SYSTEM|" + LocalDateTime.now().format(timeFormatter) + "|You were disconnected by the server");
                handler.disconnect();
            }
        }
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                String ts = LocalDateTime.now().format(timeFormatter);
                // Send PING
                for (ClientHandler handler : connectedClients.values()) {
                    handler.sendMessage("PING|" + ts);
                }
                // Check timeouts
                long now = System.currentTimeMillis();
                for (ClientHandler handler : new ArrayList<>(connectedClients.values())) {
                    if (now - handler.lastPongTime > 90_000) { // 90s timeout
                        SwingUtilities.invokeLater(() -> logArea.append("[" + ts + "] No PONG from "
                                + clientUsernames.get(handler.clientId) + ", disconnecting...\n"));
                        handler.disconnect();
                    }
                }
            } catch (Exception ignored) {
            }
        }, 10, 30, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
            heartbeatScheduler = null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Server().setVisible(true);
        });
    }
}