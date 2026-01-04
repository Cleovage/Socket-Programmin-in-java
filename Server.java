
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
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
    private JLabel statusLabel, portLabel, uptimeLabel, clientCountLabel, networkIPLabel;
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

    // Modern Dark Theme Colors (from ModernUI.ThemeColors)
    private final Color primaryColor = ModernUI.ThemeColors.PRIMARY;
    private final Color accentColor = ModernUI.ThemeColors.ERROR;
    private final Color successColor = ModernUI.ThemeColors.SUCCESS;
    private final Color backgroundColor = ModernUI.ThemeColors.BACKGROUND;
    private final Color surfaceColor = ModernUI.ThemeColors.SURFACE;
    private final Color cardColor = ModernUI.ThemeColors.CARD;
    private final Color textColor = ModernUI.ThemeColors.TEXT_PRIMARY;
    private final Color textSecondary = ModernUI.ThemeColors.TEXT_SECONDARY;
    private final Color borderColor = ModernUI.ThemeColors.BORDER;

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

    // Get best available font for emoji rendering
    private Font getEmojiCompatibleFont(int style, int size) {
        // Try different fonts that support emojis
        String[] emojiSupportingFonts = {
            "Segoe UI Emoji", // Windows
            "Apple Color Emoji", // macOS
            "Noto Color Emoji", // Linux
            "Segoe UI", // Fallback
            "Arial Unicode MS", // Fallback
            "SansSerif" // Last resort
        };

        for (String fontName : emojiSupportingFonts) {
            Font font = new Font(fontName, style, size);
            if (font.getFamily().equals(fontName)) {
                return font;
            }
        }

        // Fallback to default
        return new Font("SansSerif", style, size);
    }

    private String getLocalIPAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // Skip loopback and inactive interfaces
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // Get IPv4 address only
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        // Skip localhost
                        if (!ip.startsWith("127.")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            return "Unable to detect";
        }
        return "No network found";
    }

    private void initializeGUI() {
        setTitle("🖥️ Advanced Chat Server - Control Panel");
        setUndecorated(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Apply modern look and feel
        ModernUI.setupLookAndFeel();
        getContentPane().setBackground(backgroundColor);

        // Create top toolbar
        createToolbar();

        // Create tabbed interface
        createTabbedInterface();

        // Create status bar
        createStatusBar();

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
        if (isRunning) {
            return;
        }

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
                    statusLabel.setText("🟢 Running");
                    statusLabel.setForeground(successColor);
                    portLabel.setText("🔌 Port: " + selectedPort);

                    String localIP = getLocalIPAddress();
                    if (networkIPLabel != null) {
                        networkIPLabel.setText("🌐 " + localIP + ":" + selectedPort);
                        networkIPLabel.setForeground(successColor);
                    }

                    String startMsg = "[" + LocalDateTime.now().format(timeFormatter) + "] [>] Server started on "
                            + localIP + ":" + selectedPort;
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
        if (!isRunning) {
            return;
        }

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
            clientConnectTimes.clear();
            clientRowIndex.clear();
            clientMessageCounts.clear();
        } catch (IOException e) {
            addActivity("Error stopping server: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            broadcastButton.setEnabled(false);
            statusLabel.setText("⚫ Offline");
            statusLabel.setForeground(textSecondary);
            if (networkIPLabel != null) {
                networkIPLabel.setText("🌐 Offline");
                networkIPLabel.setForeground(textSecondary);
            }
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
            case "JOIN" ->
                formattedMessage = "JOIN|" + timestamp + "|" + message;
            case "LEAVE" ->
                formattedMessage = "LEAVE|" + timestamp + "|" + message;
            case "SYSTEM" ->
                formattedMessage = "SYSTEM|" + timestamp + "|" + message;
            case "TYPING" ->
                formattedMessage = "TYPING|" + timestamp + "|" + message; // message carries
            // username|true/false
            default ->
                formattedMessage = "CHAT|" + timestamp + "|Server|" + message;
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
                input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                // Wait for username from client (tolerate legacy clients)
                String firstLine = input.readLine();
                String pendingFirstChatMessage = null;
                if (firstLine != null && firstLine.startsWith("USERNAME|")) {
                    this.username = firstLine.substring(9).trim();
                    if (this.username.isEmpty()) {
                        this.username = "User" + (connectedClients.size() + 1);
                    }
                } else {
                    // Legacy client: treat the first line as a chat message
                    pendingFirstChatMessage = firstLine;
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

                // If the client didn't send USERNAME first, don't drop their first message
                if (pendingFirstChatMessage != null && !pendingFirstChatMessage.trim().isEmpty()) {
                    broadcastMessage(pendingFirstChatMessage, clientId, "CHAT");
                    incrementMessageCount(clientId);
                }

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
            if (!isConnected) {
                return;
            }

            isConnected = false;
            String leavingUsername = clientUsernames.get(clientId);
            if (leavingUsername != null) {
                broadcastMessage(leavingUsername + " left the chat", clientId, "LEAVE");
            }

            // Clean up
            connectedClients.remove(clientId);
            clientUsernames.remove(clientId);
            clientConnectTimes.remove(clientId);
            clientMessageCounts.remove(clientId);
            clientRowIndex.remove(clientId);

            // Update user list for remaining clients
            broadcastUserList();

            SwingUtilities.invokeLater(() -> {
                updateClientRowOnDisconnect(clientId, leavingUsername);
                // Synchronize dashboard when client disconnects
                synchronizeDashboard();
            });

            try {
                if (input != null) {
                    input.close();
                }
                if (output != null) {
                    output.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void createToolbar() {
        // Create gradient header panel
        JPanel toolbar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(30, 30, 35),
                        0, getHeight(), new Color(22, 22, 26)
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        toolbar.setLayout(new BorderLayout());
        toolbar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 2, 0, primaryColor),
                new EmptyBorder(16, 24, 16, 24)));

        // Left side - Logo and title
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandPanel.setOpaque(false);

        JLabel logoLabel = new JLabel("🖥️");
        logoLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 28));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Chat Server");
        titleLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 18));
        titleLabel.setForeground(textColor);

        JLabel subtitleLabel = new JLabel("Control Panel v2.0");
        subtitleLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 11));
        subtitleLabel.setForeground(textSecondary);

        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);

        brandPanel.add(logoLabel);
        brandPanel.add(titlePanel);

        // Center - Server controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        controlPanel.setOpaque(false);

        // Port selection with label
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        portPanel.setOpaque(false);
        JLabel toolbarPortLabel = new JLabel("🔌 Port:");
        toolbarPortLabel.setForeground(textSecondary);
        toolbarPortLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 12));
        portSpinner = new ModernUI.ModernSpinner(new SpinnerNumberModel(PORT, 1024, 65535, 1));
        portSpinner.setPreferredSize(new Dimension(90, 36));
        portPanel.add(toolbarPortLabel);
        portPanel.add(portSpinner);

        startButton = new ModernUI.ModernButton("▶ Start", successColor);
        startButton.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 13));
        startButton.setPreferredSize(new Dimension(100, 36));

        stopButton = new ModernUI.ModernButton("⏹ Stop", accentColor);
        stopButton.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 13));
        stopButton.setPreferredSize(new Dimension(100, 36));
        stopButton.setEnabled(false);

        clearLogButton = new ModernUI.ModernButton("🗑 Clear", new Color(80, 80, 90));
        clearLogButton.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 12));
        clearLogButton.setPreferredSize(new Dimension(90, 36));

        controlPanel.add(portPanel);
        controlPanel.add(Box.createHorizontalStrut(10));
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(clearLogButton);

        // Right side - Status indicators
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        statusPanel.setOpaque(false);

        // Network IP with copy button
        JPanel networkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        networkPanel.setOpaque(false);
        networkPanel.setBorder(new EmptyBorder(4, 10, 4, 10));
        networkPanel.setBackground(new Color(40, 40, 45));

        networkIPLabel = new JLabel("🌐 Offline");
        networkIPLabel.setForeground(textSecondary);
        networkIPLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 12));
        networkIPLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        networkIPLabel.setToolTipText("Click to copy network address");
        networkIPLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String text = networkIPLabel.getText();
                if (text.contains(":") && !text.contains("Offline")) {
                    String address = text.substring(text.indexOf(" ") + 1);
                    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(address);
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                    JOptionPane.showMessageDialog(Server.this,
                            "✅ Address copied: " + address + "\n\nShare this with clients to connect!",
                            "Network Address Copied",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        networkPanel.add(networkIPLabel);

        // Status indicator with colored dot
        JPanel statusIndicator = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        statusIndicator.setOpaque(false);

        statusLabel = new JLabel("⚫ Offline");
        statusLabel.setForeground(textSecondary);
        statusLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 14));

        statusIndicator.add(statusLabel);

        statusPanel.add(networkPanel);
        statusPanel.add(statusIndicator);

        toolbar.add(brandPanel, BorderLayout.WEST);
        toolbar.add(controlPanel, BorderLayout.CENTER);
        toolbar.add(statusPanel, BorderLayout.EAST);

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
        mainTabs.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 13));

        // Apply dark theme to tabbed pane
        mainTabs.setBackground(backgroundColor);
        mainTabs.setForeground(textColor);
        mainTabs.setBorder(new EmptyBorder(8, 10, 0, 10));

        // Dashboard Tab
        mainTabs.addTab("  📊  Dashboard  ", createDashboardTab());

        // Server Log Tab
        mainTabs.addTab("  📝  Logs  ", createLogTab());

        // Client Management Tab
        mainTabs.addTab("  👥  Clients  ", createClientTab());

        // Broadcast Tab
        mainTabs.addTab("  📢  Broadcast  ", createBroadcastTab());

        // Settings Tab
        mainTabs.addTab("  ⚙️  Settings  ", createSettingsTab());

        add(mainTabs, BorderLayout.CENTER);
    }

    private JPanel createDashboardTab() {
        ModernUI.ModernPanel dashboard = new ModernUI.ModernPanel(backgroundColor);
        dashboard.setLayout(new BorderLayout(18, 18));
        dashboard.setBorder(new EmptyBorder(20, 22, 20, 22));

        // Stats Panel
        ModernUI.ModernPanel statsPanel = new ModernUI.ModernPanel(backgroundColor);
        statsPanel.setLayout(new GridLayout(2, 3, 15, 15));

        // Create stat cards with references stored for updates using new color scheme
        statsPanel.add(createStatCard("👥 Connected", "0", primaryColor, "clients"));
        statsPanel.add(createStatCard("💬 Messages", "0", successColor, "messages"));
        statsPanel.add(createStatCard("⏱️ Uptime", "00:00:00", accentColor, "uptime"));
        statsPanel.add(createStatCard("🔌 Port", String.valueOf(PORT), new Color(200, 140, 60), "port"));
        statsPanel.add(createStatCard("💾 Memory", "0 MB", new Color(200, 100, 100), "memory"));
        statsPanel.add(createStatCard("🔗 Total Connections", "0", new Color(80, 160, 150), "connections"));

        dashboard.add(statsPanel, BorderLayout.NORTH);

        // Real-time activity feed
        ModernUI.ModernCard activityPanel = new ModernUI.ModernCard(cardColor, false);
        activityPanel.setLayout(new BorderLayout(0, 10));
        activityPanel.setBorder(new CompoundBorder(
                new ModernUI.ModernTitledBorder("🔴 Live Activity"),
                new EmptyBorder(12, 12, 12, 12)));
        dashboardActivityFeed = new JTextArea(15, 40);
        dashboardActivityFeed.setEditable(false);
        dashboardActivityFeed.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 13));
        dashboardActivityFeed.setBackground(cardColor);
        dashboardActivityFeed.setForeground(textColor);
        dashboardActivityFeed.setBorder(new EmptyBorder(16, 16, 16, 16));

        JScrollPane activityScroll = new JScrollPane(dashboardActivityFeed);
        activityScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        activityPanel.add(activityScroll, BorderLayout.CENTER);

        dashboard.add(activityPanel, BorderLayout.CENTER);

        return dashboard;
    }

    private JPanel createStatCard(String title, String value, Color color, String type) {
        // Create card with subtle gradient background
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw rounded background with subtle gradient
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(cardColor.getRed() + 5, cardColor.getGreen() + 5, cardColor.getBlue() + 5),
                        0, getHeight(), cardColor
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);

                // Draw accent line at top
                g2.setColor(color);
                g2.fillRoundRect(0, 0, getWidth(), 4, 4, 4);

                // Draw subtle border
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(20, 18, 16, 18));

        // Top section with icon and title
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 13));
        titleLabel.setForeground(textSecondary);
        topPanel.add(titleLabel);

        // Center with large value
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 32));
        valueLabel.setForeground(color);
        centerPanel.add(valueLabel);

        // Bottom with live indicator
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(6, 0, 0, 0));

        JLabel dotLabel = new JLabel("●");
        dotLabel.setFont(new Font("SansSerif", Font.PLAIN, 8));
        dotLabel.setForeground(successColor);

        JLabel liveLabel = new JLabel("Live");
        liveLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 10));
        liveLabel.setForeground(new Color(textSecondary.getRed(), textSecondary.getGreen(), textSecondary.getBlue(), 180));

        bottomPanel.add(dotLabel);
        bottomPanel.add(liveLabel);

        card.add(topPanel, BorderLayout.NORTH);
        card.add(centerPanel, BorderLayout.CENTER);
        card.add(bottomPanel, BorderLayout.SOUTH);

        // Store references
        switch (type) {
            case "clients" ->
                dashboardClientCountLabel = valueLabel;
            case "messages" ->
                totalMessagesStatLabel = valueLabel;
            case "uptime" ->
                dashboardUptimeLabel = valueLabel;
            case "port" ->
                dashboardPortLabel = valueLabel;
            case "memory" ->
                dashboardMemoryLabel = valueLabel;
            case "connections" ->
                dashboardConnectionsLabel = valueLabel;
        }

        return card;
    }

    private JPanel createLogTab() {
        JPanel logPanel = new JPanel(new BorderLayout(0, 12));
        logPanel.setBackground(backgroundColor);
        logPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Log controls
        JPanel logControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        logControls.setBackground(backgroundColor);
        autoScrollToggle = new JToggleButton("Auto-scroll", true);
        autoScrollToggle.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 12));
        autoScrollToggle.setBackground(cardColor);
        autoScrollToggle.setForeground(textColor);
        logControls.add(autoScrollToggle);

        logPanel.add(logControls, BorderLayout.NORTH);

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 14));
        logArea.setBackground(cardColor);
        logArea.setForeground(textColor);
        logArea.setBorder(new EmptyBorder(14, 14, 14, 14));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(new ModernUI.RoundedBorder(borderColor, ModernUI.CORNER_RADIUS, 1));
        logScroll.setBackground(cardColor);
        logScroll.getViewport().setBackground(cardColor);
        logScroll.getVerticalScrollBar().setUI(new ModernUI.ModernScrollBarUI());
        logScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logPanel.add(logScroll, BorderLayout.CENTER);

        return logPanel;
    }

    private JPanel createClientTab() {
        ModernUI.ModernPanel clientPanel = new ModernUI.ModernPanel(backgroundColor);
        clientPanel.setLayout(new BorderLayout(0, 12));
        clientPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Client table with ModernUI styling
        String[] columns = {"Username", "IP Address", "Connect Time", "Status", "Messages Sent"};
        clientTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        clientTable = new JTable(clientTableModel);
        clientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Use TableStyler for consistent styling
        JScrollPane clientScroll = ModernUI.TableStyler.createStyledScrollPane(clientTable);
        clientPanel.add(clientScroll, BorderLayout.CENTER);

        // Client actions
        ModernUI.ModernPanel actionPanel = new ModernUI.ModernPanel(backgroundColor);
        actionPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        actionPanel.setBorder(new EmptyBorder(6, 0, 0, 0));
        banUserButton = new ModernUI.ModernButton("🚫 Kick", ModernUI.ThemeColors.ERROR);
        banUserButton.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 13));
        banUserButton.setEnabled(false);
        actionPanel.add(banUserButton);

        clientPanel.add(actionPanel, BorderLayout.SOUTH);

        // Selection listener to enable kick button
        clientTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            boolean hasSelection = clientTable.getSelectedRow() >= 0;
            banUserButton.setEnabled(hasSelection);
        });

        // Kick selected user
        banUserButton.addActionListener(e -> kickSelectedClient());

        return clientPanel;
    }

    private JPanel createBroadcastTab() {
        ModernUI.ModernPanel broadcastPanel = new ModernUI.ModernPanel(backgroundColor);
        broadcastPanel.setLayout(new BorderLayout(0, 16));
        broadcastPanel.setBorder(new EmptyBorder(20, 22, 20, 22));

        JLabel titleLabel = new JLabel("📢 Broadcast Message");
        titleLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 18));
        titleLabel.setForeground(textColor);
        broadcastPanel.add(titleLabel, BorderLayout.NORTH);

        ModernUI.ModernPanel inputPanel = new ModernUI.ModernPanel(backgroundColor);
        inputPanel.setLayout(new BorderLayout(12, 0));
        inputPanel.setBorder(new EmptyBorder(18, 0, 0, 0));

        ModernUI.ModernTextField modernBroadcastField = new ModernUI.ModernTextField("Type your broadcast message...");
        modernBroadcastField.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 14));
        broadcastField = modernBroadcastField;

        broadcastButton = new ModernUI.ModernButton("📤 Send", primaryColor);
        broadcastButton.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 13));
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
        ModernUI.ModernPanel settingsPanel = new ModernUI.ModernPanel(backgroundColor);
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(new EmptyBorder(20, 22, 20, 22));

        JLabel titleLabel = new JLabel("⚙️ Configuration");
        titleLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 18));
        titleLabel.setForeground(textColor);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(titleLabel);

        settingsPanel.add(Box.createVerticalStrut(18));

        // Log level setting with ModernCard
        ModernUI.ModernCard logLevelCard = new ModernUI.ModernCard(cardColor, false);
        logLevelCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 6));
        logLevelCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        logLevelCard.setMaximumSize(new Dimension(500, 60));
        JLabel logLevelLabel = new JLabel("📊 Log Level:");
        logLevelLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 13));
        logLevelLabel.setForeground(textColor);
        logLevelLabel.setBorder(new EmptyBorder(0, 0, 0, 6));
        logLevelCard.add(logLevelLabel);
        logLevelSlider = new JSlider(1, 5, 3);
        logLevelSlider.setMajorTickSpacing(1);
        logLevelSlider.setPaintTicks(true);
        logLevelSlider.setPaintLabels(true);
        logLevelSlider.setBackground(cardColor);
        logLevelSlider.setForeground(textColor);
        logLevelSlider.setBorder(new EmptyBorder(4, 4, 0, 4));
        logLevelCard.add(logLevelSlider);
        logLevelCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(logLevelCard);

        settingsPanel.add(Box.createVerticalStrut(12));

        // Auto-scroll toggle setting
        ModernUI.ModernCard autoScrollCard = new ModernUI.ModernCard(cardColor, false);
        autoScrollCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 6));
        autoScrollCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        autoScrollCard.setMaximumSize(new Dimension(500, 60));
        JLabel autoScrollLabel = new JLabel("📜 Auto-scroll Logs:");
        autoScrollLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 13));
        autoScrollLabel.setForeground(textColor);
        autoScrollCard.add(autoScrollLabel);
        ModernUI.ModernToggleButton autoScrollToggleBtn = new ModernUI.ModernToggleButton(true);
        autoScrollToggleBtn.addActionListener(e -> autoScrollToggle.setSelected(autoScrollToggleBtn.isOn()));
        autoScrollCard.add(autoScrollToggleBtn);
        autoScrollCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(autoScrollCard);

        settingsPanel.add(Box.createVerticalStrut(12));

        // Max clients setting
        ModernUI.ModernCard maxClientsCard = new ModernUI.ModernCard(cardColor, false);
        maxClientsCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 6));
        maxClientsCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        maxClientsCard.setMaximumSize(new Dimension(500, 60));
        JLabel maxClientsLabel = new JLabel("👥 Max Clients:");
        maxClientsLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 13));
        maxClientsLabel.setForeground(textColor);
        maxClientsCard.add(maxClientsLabel);
        JSpinner maxClientsSpinner = new ModernUI.ModernSpinner(new SpinnerNumberModel(50, 1, 500, 1));
        maxClientsSpinner.setPreferredSize(new Dimension(80, 32));
        maxClientsCard.add(maxClientsSpinner);
        maxClientsCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(maxClientsCard);

        settingsPanel.add(Box.createVerticalStrut(12));

        // Heartbeat interval setting
        ModernUI.ModernCard heartbeatCard = new ModernUI.ModernCard(cardColor, false);
        heartbeatCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 6));
        heartbeatCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        heartbeatCard.setMaximumSize(new Dimension(500, 60));
        JLabel heartbeatLabel = new JLabel("💓 Heartbeat Interval (sec):");
        heartbeatLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 13));
        heartbeatLabel.setForeground(textColor);
        heartbeatCard.add(heartbeatLabel);
        JSpinner heartbeatSpinner = new ModernUI.ModernSpinner(new SpinnerNumberModel(30, 5, 120, 5));
        heartbeatSpinner.setPreferredSize(new Dimension(80, 32));
        heartbeatCard.add(heartbeatSpinner);
        heartbeatCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(heartbeatCard);

        settingsPanel.add(Box.createVerticalStrut(24));

        // Server info section
        JLabel infoTitle = new JLabel("ℹ️ Server Information");
        infoTitle.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 16));
        infoTitle.setForeground(textColor);
        infoTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(infoTitle);

        settingsPanel.add(Box.createVerticalStrut(12));

        ModernUI.ModernCard infoCard = new ModernUI.ModernCard(cardColor, false);
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        infoCard.setMaximumSize(new Dimension(500, 150));

        JLabel versionLabel = new JLabel("🔖 Version: 2.0.0 (Modern UI)");
        versionLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 12));
        versionLabel.setForeground(textSecondary);
        infoCard.add(versionLabel);
        infoCard.add(Box.createVerticalStrut(6));

        JLabel javaLabel = new JLabel("☕ Java: " + System.getProperty("java.version"));
        javaLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 12));
        javaLabel.setForeground(textSecondary);
        infoCard.add(javaLabel);
        infoCard.add(Box.createVerticalStrut(6));

        JLabel osLabel = new JLabel("💻 OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        osLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 12));
        osLabel.setForeground(textSecondary);
        infoCard.add(osLabel);
        infoCard.add(Box.createVerticalStrut(6));

        String localIP = getLocalIPAddress();
        JLabel ipLabel = new JLabel("🌐 Local IP: " + localIP);
        ipLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 12));
        ipLabel.setForeground(textSecondary);
        infoCard.add(ipLabel);

        infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(infoCard);

        return settingsPanel;
    }

    private void createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(surfaceColor); // Surface color
        statusBar.setBorder(new EmptyBorder(5, 10, 5, 10));

        JPanel leftStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftStatus.setOpaque(false);

        portLabel = new JLabel("🔌 " + PORT);
        uptimeLabel = new JLabel("⏱️ 00:00:00");
        clientCountLabel = new JLabel("👥 0");

        portLabel.setForeground(textColor);
        uptimeLabel.setForeground(textColor);
        clientCountLabel.setForeground(textColor);

        // Apply consistent font styling with emoji support
        Font statusFont = ModernUI.getEmojiCompatibleFont(Font.PLAIN, 13);
        portLabel.setFont(statusFont);
        uptimeLabel.setFont(statusFont);
        clientCountLabel.setFont(statusFont);

        leftStatus.add(portLabel);
        leftStatus.add(uptimeLabel);
        leftStatus.add(clientCountLabel);

        // Memory usage
        JPanel rightStatus = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightStatus.setOpaque(false);

        // Use ModernProgressBar for memory display
        ModernUI.ModernProgressBar modernMemoryBar = new ModernUI.ModernProgressBar(0, 100);
        modernMemoryBar.setPreferredSize(new Dimension(150, 12));
        modernMemoryBar.setProgressColor(primaryColor);
        memoryBar = new JProgressBar(0, 100); // Keep reference for compatibility
        memoryBar.setStringPainted(true);
        memoryBar.setString("Memory: 0%");
        memoryBar.setPreferredSize(new Dimension(120, 20));
        memoryBar.setBackground(cardColor);
        memoryBar.setForeground(primaryColor);
        memoryBar.setBorder(new ModernUI.RoundedBorder(borderColor, 6, 1));
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
            uptimeLabel.setText("⏱️ " + uptimeStr);
        } else {
            uptimeLabel.setText("⏱️ 00:00:00");
        }

        // Update dashboard uptime (always synchronized)
        if (dashboardUptimeLabel != null) {
            dashboardUptimeLabel.setText(uptimeStr);
        }

        // Update client count for both status bar and dashboard
        int clientCount = connectedClients.size();
        clientCountLabel.setText("👥 " + clientCount);
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
        memoryBar.setString("💾 " + memoryPercent + "%");

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
            Color clientColor = isRunning ? new Color(88, 86, 214) : new Color(120, 120, 120);
            dashboardClientCountLabel.setForeground(clientColor);
        }

        if (dashboardUptimeLabel != null) {
            Color uptimeColor = isRunning ? new Color(255, 92, 88) : new Color(120, 120, 120);
            dashboardUptimeLabel.setForeground(uptimeColor);
        }

        if (dashboardPortLabel != null) {
            Color portColor = isRunning ? new Color(100, 180, 120) : new Color(200, 140, 60);
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
        Object[] row = {username, ip, connectTime, "Online", 0};
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
        clientCountLabel.setText("👥 " + connectedClients.size());
    }

    private void kickSelectedClient() {
        int row = clientTable.getSelectedRow();
        if (row < 0) {
            return;
        }
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
