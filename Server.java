import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
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

    // Dashboard stat labels for real-time updates
    private JLabel dashboardClientCountLabel;
    private JLabel totalMessagesStatLabel;
    private JLabel dashboardUptimeLabel;
    private JLabel dashboardPortLabel;
    private JLabel dashboardMemoryLabel;
    private JLabel dashboardConnectionsLabel;
    private JTextArea dashboardActivityFeed;

    // Stats tracking
    private long serverStartTime;
    private int totalConnectionsEver = 0;

    private DateTimeFormatter timeFormatter;
    private javax.swing.Timer uiUpdateTimer;
    private ScheduledExecutorService heartbeatScheduler;

    public Server() {
        threadPool = Executors.newCachedThreadPool();
        connectedClients = new ConcurrentHashMap<>();
        clientUsernames = new ConcurrentHashMap<>();
        clientConnectTimes = new ConcurrentHashMap<>();
        clientRowIndex = new ConcurrentHashMap<>();
        clientMessageCounts = new ConcurrentHashMap<>();
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        initializeGUI();
        startUIUpdateTimer();

        // Initialize dashboard with correct starting values
        synchronizeDashboard();
    }

    private void initializeGUI() {
        setTitle("[SERVER] Advanced Chat Server Control Panel");
        setUndecorated(false);
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
                    statusLabel.setText("[>] Server Running on port " + selectedPort);
                    statusLabel.setForeground(new Color(76, 175, 80));
                    portLabel.setText("[PORT] Port: " + selectedPort);

                    String startMsg = "[" + LocalDateTime.now().format(timeFormatter) + "] [>] Server started on port "
                            + selectedPort;
                    addActivity(startMsg);

                    // Synchronize dashboard after server start
                    synchronizeDashboard();
                });

                // Start heartbeat scheduler
                startHeartbeat();

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    String clientId = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
                    totalConnectionsEver++;

                    SwingUtilities.invokeLater(() -> {
                        String connectMsg = "[" + LocalDateTime.now().format(timeFormatter)
                                + "] [+] New client connected: " + clientId;
                        addActivity(connectMsg);
                    });

                    ClientHandler handler = new ClientHandler(clientSocket, clientId);
                    connectedClients.put(clientId, handler);
                    clientConnectTimes.put(clientId, System.currentTimeMillis());
                    threadPool.execute(handler);
                }
            } catch (IOException e) {
                if (isRunning) {
                    SwingUtilities.invokeLater(() -> {
                        String errorMsg = "[" + LocalDateTime.now().format(timeFormatter) + "] [ERROR] Server error: "
                                + e.getMessage();
                        addActivity(errorMsg);
                    });
                }
            }
        });
    }

    public void stopServer() {
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
            addActivity("Error stopping server: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            statusLabel.setText("[STOPPED] Server Stopped");
            addActivity("[" + LocalDateTime.now().format(timeFormatter) + "] Server stopped");

            // Synchronize dashboard after server stop
            synchronizeDashboard();
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
            SwingUtilities.invokeLater(() -> addActivity(
                    "[" + timestamp + "] [PM] " + fromUser + " -> " + toUsername + ": " + content));
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
                    addActivity("[" + LocalDateTime.now().format(timeFormatter) + "] " + username + " (" + clientId
                            + ") joined");
                    // Synchronize dashboard when client joins
                    synchronizeDashboard();
                });

                String message;
                while (isConnected && (message = input.readLine()) != null) {
                    final String finalMessage = message;
                    SwingUtilities.invokeLater(() -> {
                        addActivity("[" + LocalDateTime.now().format(timeFormatter) + "] [" + username + "]: "
                                + finalMessage);
                    });

                    if (message.startsWith("PONG")) {
                        lastPongTime = System.currentTimeMillis();
                    } else if (message.startsWith("TYPING|")) {
                        // Relay typing state to others: expected format TYPING|username|true/false
                        String typingPayload = message.substring(7); // username|true
                        broadcastMessage(typingPayload, clientId, "TYPING");
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
                    } else if (message.equalsIgnoreCase("/help")) {
                        sendMessage("SYSTEM|" + LocalDateTime.now().format(timeFormatter)
                                + "|Available commands: /help, /list, /quit, /w <user> <msg>, /broadcast <msg>, /file <name>");
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
                        addActivity("[" + LocalDateTime.now().format(timeFormatter) + "] Client " + clientId
                                + " error: " + e.getMessage());
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
                // Synchronize dashboard when client disconnects
                synchronizeDashboard();
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
        ModernPanel toolbar = new ModernPanel(new BorderLayout(), primaryColor, 0, false);
        toolbar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 2, 0, primaryColor.darker()),
                new EmptyBorder(10, 15, 10, 15)));

        // Left side - Server controls
        ModernPanel leftPanel = new ModernPanel(new FlowLayout(FlowLayout.LEFT, 10, 0), primaryColor, 0, false);
        leftPanel.setOpaque(false);

        startButton = new ModernButton("[>] Start Server", new Color(76, 175, 80));
        stopButton = new ModernButton("[STOP] Stop Server", new Color(244, 67, 54));
        clearLogButton = new ModernButton("[CLEAR] Clear Log", new Color(158, 158, 158));

        stopButton.setEnabled(false);

        // Port selection
        portSpinner = new JSpinner(new SpinnerNumberModel(PORT, 1024, 65535, 1));
        portSpinner.setPreferredSize(new Dimension(80, 30));

        leftPanel.add(new JLabel("[PORT] Port:"));
        leftPanel.add(portSpinner);
        leftPanel.add(Box.createHorizontalStrut(10));
        leftPanel.add(startButton);
        leftPanel.add(stopButton);
        leftPanel.add(clearLogButton);

        // Right side - Status indicators
        ModernPanel rightPanel = new ModernPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0), primaryColor, 0, false);
        rightPanel.setOpaque(false);

        statusLabel = new JLabel("[OFFLINE] Server Stopped");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        rightPanel.add(statusLabel);

        toolbar.add(leftPanel, BorderLayout.WEST);
        toolbar.add(rightPanel, BorderLayout.EAST);

        // Add action listeners
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        clearLogButton.addActionListener(e -> {
            logArea.setText("");
            if (dashboardActivityFeed != null) {
                dashboardActivityFeed.setText("");
            }
        });

        add(toolbar, BorderLayout.NORTH);
    }

    private void createTabbedInterface() {
        mainTabs = new JTabbedPane(JTabbedPane.TOP);
        mainTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Dashboard Tab
        mainTabs.addTab("[DASHBOARD] Dashboard", createDashboardTab());

        // Server Log Tab
        mainTabs.addTab("[LOG] Server Log", createLogTab());

        // Client Management Tab
        mainTabs.addTab("[CLIENTS] Clients", createClientTab());

        // Broadcast Tab
        mainTabs.addTab("[BROADCAST] Broadcast", createBroadcastTab());

        // Settings Tab
        mainTabs.addTab("[SETTINGS] Settings", createSettingsTab());

        add(mainTabs, BorderLayout.CENTER);
    }

    private JPanel createDashboardTab() {
        ModernPanel dashboard = new ModernPanel(new BorderLayout(15, 15), Color.WHITE, 0, false);
        dashboard.setBorder(new EmptyBorder(20, 20, 20, 20));
        dashboard.setBackground(Color.WHITE);

        // Stats Panel
        ModernPanel statsPanel = new ModernPanel(new GridLayout(2, 3, 15, 15), Color.WHITE, 0, false);
        statsPanel.setBackground(Color.WHITE);

        // Create stat cards with references stored for updates
        statsPanel.add(createStatCard("[CLIENTS] Connected Clients", "0", new Color(33, 150, 243), "clients"));
        statsPanel.add(createStatCard("[MESSAGES] Total Messages", "0", new Color(76, 175, 80), "messages"));
        statsPanel.add(createStatCard("[UPTIME] Server Uptime", "00:00:00", new Color(156, 39, 176), "uptime"));
        statsPanel.add(createStatCard("[PORT] Port Status", String.valueOf(PORT), new Color(255, 152, 0), "port"));
        statsPanel.add(createStatCard("[MEMORY] Memory Usage", "0 MB", new Color(244, 67, 54), "memory"));
        statsPanel.add(createStatCard("[CONNECTIONS] Total Connections", "0", new Color(0, 150, 136), "connections"));

        dashboard.add(statsPanel, BorderLayout.NORTH);

        // Real-time activity feed
        ModernPanel activityPanel = new ModernPanel(new BorderLayout(), Color.WHITE, 8, true);
        activityPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(200, 200, 200)),
                "[LIVE] Live Activity Feed",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14)));
        activityPanel.setBackground(Color.WHITE);

        dashboardActivityFeed = new JTextArea(15, 40);
        dashboardActivityFeed.setEditable(false);
        dashboardActivityFeed.setFont(new Font("Consolas", Font.PLAIN, 11));
        dashboardActivityFeed.setBackground(new Color(248, 249, 250));
        dashboardActivityFeed.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane activityScroll = new JScrollPane(dashboardActivityFeed);
        activityScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        activityPanel.add(activityScroll, BorderLayout.CENTER);

        dashboard.add(activityPanel, BorderLayout.CENTER);

        return dashboard;
    }

    private JPanel createStatCard(String title, String value, Color color, String type) {
        ModernPanel card = new ModernPanel(new BorderLayout(), Color.WHITE, 8, true);
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

        // Add a small status indicator for real-time sync
        JLabel lastUpdated = new JLabel("Live");
        lastUpdated.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lastUpdated.setForeground(new Color(150, 150, 150));

        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.setOpaque(false);
        labelPanel.add(valueLabel, BorderLayout.CENTER);
        labelPanel.add(lastUpdated, BorderLayout.SOUTH);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(labelPanel, BorderLayout.CENTER);

        // Store references to dashboard stat labels for real-time updates
        switch (type) {
            case "clients" -> dashboardClientCountLabel = valueLabel;
            case "messages" -> totalMessagesStatLabel = valueLabel;
            case "uptime" -> dashboardUptimeLabel = valueLabel;
            case "port" -> dashboardPortLabel = valueLabel;
            case "memory" -> dashboardMemoryLabel = valueLabel;
            case "connections" -> dashboardConnectionsLabel = valueLabel;
        }

        return card;
    }

    private JPanel createLogTab() {
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Log controls
        JPanel logControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        autoScrollToggle = new JToggleButton("[AUTO] Auto-scroll", true);
        logControls.add(autoScrollToggle);

        logPanel.add(logControls, BorderLayout.NORTH);

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        logArea.setBackground(new Color(40, 40, 40));
        logArea.setForeground(Color.WHITE);
        logArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(null);
        logScroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        logScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logPanel.add(logScroll, BorderLayout.CENTER);

        return logPanel;
    }

    private JPanel createClientTab() {
        ModernPanel clientPanel = new ModernPanel(new BorderLayout(), Color.WHITE, 0, false);
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
        ModernPanel actionPanel = new ModernPanel(new FlowLayout(FlowLayout.RIGHT), Color.WHITE, 0, false);
        banUserButton = new ModernButton("[KICK] Kick User", new Color(244, 67, 54));
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
        banUserButton.addActionListener(e -> kickSelectedClient());

        return clientPanel;
    }

    private JPanel createBroadcastTab() {
        ModernPanel broadcastPanel = new ModernPanel(new BorderLayout(), Color.WHITE, 0, false);
        broadcastPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("[BROADCAST] Send Message to All Clients");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        broadcastPanel.add(titleLabel, BorderLayout.NORTH);

        ModernPanel inputPanel = new ModernPanel(new BorderLayout(10, 10), Color.WHITE, 0, false);
        inputPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        broadcastField = new JTextField();
        broadcastField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        broadcastField.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 200, 200)),
                new EmptyBorder(10, 10, 10, 10)));

        broadcastButton = new ModernButton("[SEND] Send Broadcast", new Color(33, 150, 243));
        broadcastButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        broadcastButton.setForeground(Color.WHITE);
        broadcastButton.setBorder(new EmptyBorder(10, 20, 10, 20));
        broadcastButton.setEnabled(false);

        inputPanel.add(broadcastField, BorderLayout.CENTER);
        inputPanel.add(broadcastButton, BorderLayout.EAST);

        broadcastPanel.add(inputPanel, BorderLayout.CENTER);

        // Wire broadcast action
        broadcastButton.addActionListener(e -> {
            String msg = broadcastField.getText().trim();
            if (!msg.isEmpty()) {
                broadcastMessage(msg, "server", "CHAT");
                String ts = LocalDateTime.now().format(timeFormatter);
                addActivity("[" + ts + "] [Broadcast]: " + msg);
                broadcastField.setText("");
            }
        });
        // Enter to send
        broadcastField.addActionListener(e -> broadcastButton.doClick());

        return broadcastPanel;
    }

    private JPanel createSettingsTab() {
        ModernPanel settingsPanel = new ModernPanel(new FlowLayout(FlowLayout.LEFT), Color.WHITE, 0, false);
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("[SETTINGS] Server Settings");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(titleLabel);

        settingsPanel.add(Box.createVerticalStrut(20));

        // Log level setting
        ModernPanel logLevelPanel = new ModernPanel(new FlowLayout(FlowLayout.LEFT), Color.WHITE, 0, false);
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

        portLabel = new JLabel("[PORT] Port: " + PORT);
        uptimeLabel = new JLabel("[UPTIME] Uptime: 00:00:00");
        clientCountLabel = new JLabel("[CLIENTS] Clients: 0");

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
        uiUpdateTimer = new javax.swing.Timer(1000, e -> updateUIStats());
        uiUpdateTimer.start();
    }

    private void updateUIStats() {
        // Update status bar elements and dashboard synchronously

        // Handle uptime for both running and stopped states
        String uptimeStr = "00:00:00";
        if (isRunning && serverStartTime > 0) {
            long uptime = System.currentTimeMillis() - serverStartTime;
            long hours = uptime / (1000 * 60 * 60);
            long minutes = (uptime / (1000 * 60)) % 60;
            long seconds = (uptime / 1000) % 60;
            uptimeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            uptimeLabel.setText(String.format("[UPTIME] Uptime: %s", uptimeStr));
        } else {
            uptimeLabel.setText("[UPTIME] Uptime: 00:00:00");
        }

        // Update dashboard uptime (always synchronized)
        if (dashboardUptimeLabel != null) {
            dashboardUptimeLabel.setText(uptimeStr);
        }

        // Update client count for both status bar and dashboard
        int clientCount = connectedClients.size();
        clientCountLabel.setText("[CLIENTS] Clients: " + clientCount);
        if (dashboardClientCountLabel != null) {
            dashboardClientCountLabel.setText(String.valueOf(clientCount));
        }

        // Update memory usage for both status bar and dashboard (consistent format)
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        int memoryPercent = (int) ((usedMemory * 100) / maxMemory);
        long usedMemoryMB = usedMemory / (1024 * 1024);

        memoryBar.setValue(memoryPercent);
        memoryBar.setString("[MEMORY] Memory: " + memoryPercent + "%");

        // Dashboard shows memory in MB with percentage
        if (dashboardMemoryLabel != null) {
            dashboardMemoryLabel.setText(usedMemoryMB + " MB (" + memoryPercent + "%)");
        }

        // Update dashboard total connections
        if (dashboardConnectionsLabel != null) {
            dashboardConnectionsLabel.setText(String.valueOf(totalConnectionsEver));
        }

        // Update dashboard port (show current selected port, not just when running)
        if (dashboardPortLabel != null) {
            int currentPort = (Integer) portSpinner.getValue();
            String portStatus = isRunning ? currentPort + " (Active)" : currentPort + " (Stopped)";
            dashboardPortLabel.setText(portStatus);
        }

        // Update total messages count (ensure it's current)
        if (totalMessagesStatLabel != null) {
            int totalMessages = 0;
            for (int count : clientMessageCounts.values()) {
                totalMessages += count;
            }
            totalMessagesStatLabel.setText(String.valueOf(totalMessages));
        }

        // Force repaint of dashboard elements to ensure visual updates
        refreshDashboardVisuals();
    }

    // Method to refresh dashboard visual indicators and colors
    private void refreshDashboardVisuals() {
        // Update stat card colors based on server status
        if (dashboardClientCountLabel != null) {
            Color clientColor = isRunning ? new Color(33, 150, 243) : new Color(158, 158, 158);
            dashboardClientCountLabel.setForeground(clientColor);
        }

        if (dashboardUptimeLabel != null) {
            Color uptimeColor = isRunning ? new Color(156, 39, 176) : new Color(158, 158, 158);
            dashboardUptimeLabel.setForeground(uptimeColor);
        }

        if (dashboardPortLabel != null) {
            Color portColor = isRunning ? new Color(76, 175, 80) : new Color(255, 152, 0);
            dashboardPortLabel.setForeground(portColor);
        }
    }

    // Enhanced method to ensure dashboard is fully synchronized when server
    // starts/stops
    private void synchronizeDashboard() {
        SwingUtilities.invokeLater(() -> {
            updateUIStats();

            // Ensure activity feed is synchronized
            if (dashboardActivityFeed != null && logArea != null) {
                String logContent = logArea.getText();
                if (!dashboardActivityFeed.getText().equals(logContent)) {
                    dashboardActivityFeed.setText(logContent);
                    dashboardActivityFeed.setCaretPosition(dashboardActivityFeed.getDocument().getLength());
                }
            }
        });
    }

    // Method to add activity to both server log and dashboard activity feed
    private void addActivity(String message) {
        // Add to server log
        logArea.append(message + "\n");
        if (autoScrollToggle.isSelected()) {
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }

        // Add to dashboard activity feed (synchronized)
        if (dashboardActivityFeed != null) {
            dashboardActivityFeed.append(message + "\n");
            // Auto-scroll activity feed
            dashboardActivityFeed.setCaretPosition(dashboardActivityFeed.getDocument().getLength());

            // Keep activity feed to reasonable size (max 1000 lines)
            String text = dashboardActivityFeed.getText();
            String[] lines = text.split("\n");
            if (lines.length > 1000) {
                StringBuilder trimmed = new StringBuilder();
                for (int i = lines.length - 1000; i < lines.length; i++) {
                    trimmed.append(lines[i]).append("\n");
                }
                dashboardActivityFeed.setText(trimmed.toString());
                dashboardActivityFeed.setCaretPosition(dashboardActivityFeed.getDocument().getLength());
            }
        }
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
        addActivity("[" + ts + "] " + (username != null ? username : clientId) + " disconnected");
        updateClientCountLabel();
    }

    private void incrementMessageCount(String clientId) {
        clientMessageCounts.merge(clientId, 1, Integer::sum);

        // Update dashboard total messages immediately
        if (totalMessagesStatLabel != null) {
            int totalMessages = 0;
            for (int count : clientMessageCounts.values()) {
                totalMessages += count;
            }
            totalMessagesStatLabel.setText(String.valueOf(totalMessages));
        }

        // Update client table
        Integer row = clientRowIndex.get(clientId);
        if (row != null && row < clientTableModel.getRowCount()) {
            clientTableModel.setValueAt(clientMessageCounts.get(clientId), row, 4);
        }
    }

    private void updateClientCountLabel() {
        clientCountLabel.setText("[CLIENTS] Clients: " + connectedClients.size());
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
                        SwingUtilities.invokeLater(() -> addActivity("[" + ts + "] No PONG from "
                                + clientUsernames.get(handler.clientId) + ", disconnecting..."));
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