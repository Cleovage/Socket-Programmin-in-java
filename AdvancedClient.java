
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;

public class AdvancedClient extends JFrame {

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private volatile boolean isConnected = false;

    // Modern UI Components
    private JTextPane chatArea;
    private ModernTextField messageField;
    private ModernButton connectButton, sendButton, emojiButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JScrollPane userScroll;
    private ModernPanel connectionPanel, chatPanel, inputPanel;
    private JLabel statusLabel, userCountLabel;
    private JLabel typingLabel;
    private ModernTextField serverField, portField, usernameField;

    private String serverAddress = "localhost";
    private int serverPort = 12345;
    private String username = "Anonymous";

    // Modern Dark Theme Color Scheme
    private final Color PRIMARY_COLOR = new Color(88, 86, 214);
    private final Color ACCENT_COLOR = new Color(255, 92, 88);
    private final Color SUCCESS_COLOR = new Color(72, 187, 120);
    private final Color BACKGROUND_COLOR = new Color(18, 18, 18);
    private final Color SURFACE_COLOR = new Color(28, 28, 28);
    private final Color CARD_COLOR = new Color(38, 38, 38);
    private final Color TEXT_COLOR = new Color(240, 240, 240);
    private final Color TEXT_SECONDARY = new Color(160, 160, 160);
    private final Color BORDER_COLOR = new Color(58, 58, 58);

    public AdvancedClient() {
        initializeModernGUI();
        setupNetworking();
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

    private void initializeModernGUI() {
        setTitle("[CLIENT] Elite Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 650));

        // Set dark theme
        getContentPane().setBackground(BACKGROUND_COLOR);
        setLayout(new BorderLayout(0, 0));

        // Create modern components with animations
        createModernConnectionPanel();
        createModernChatPanel();
        createModernInputPanel();

        // Add components with spacing
        add(connectionPanel, BorderLayout.NORTH);
        add(chatPanel, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // Setup keyboard shortcuts and effects
        setupKeyboardShortcuts();
        setupWindowEffects();
    }

    private void createModernConnectionPanel() {
        connectionPanel = new ModernPanel(new BorderLayout(), SURFACE_COLOR, 0, false);
        connectionPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        // Initialize connection fields
        serverField = new ModernTextField("localhost", 0, PRIMARY_COLOR, BORDER_COLOR);
        serverField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        serverField.setPreferredSize(new Dimension(120, 30));

        portField = new ModernTextField("12345", 0, PRIMARY_COLOR, BORDER_COLOR);
        portField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        portField.setPreferredSize(new Dimension(80, 30));

        usernameField = new ModernTextField("", 0, PRIMARY_COLOR, BORDER_COLOR);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        usernameField.setPreferredSize(new Dimension(120, 30));

        // Left side - Connection controls with improved alignment
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 10, 0, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Server field
        gbc.gridx = 0;
        gbc.gridy = 0;
        leftPanel.add(createGlowLabel("[SERVER] Server:", TEXT_COLOR), gbc);
        gbc.gridx = 1;
        leftPanel.add(serverField, gbc);

        // Port field
        gbc.gridx = 2;
        gbc.gridy = 0;
        leftPanel.add(createGlowLabel("[PORT] Port:", TEXT_COLOR), gbc);
        gbc.gridx = 3;
        leftPanel.add(portField, gbc);

        // Username field
        gbc.gridx = 4;
        gbc.gridy = 0;
        leftPanel.add(createGlowLabel("[USER] Username:", TEXT_COLOR), gbc);
        gbc.gridx = 5;
        leftPanel.add(usernameField, gbc);

        // Connect button
        gbc.gridx = 6;
        gbc.gridy = 0;
        connectButton = new ModernButton("[CONNECT] Connect", SUCCESS_COLOR);
        connectButton.addActionListener(e -> toggleConnection());
        leftPanel.add(connectButton, gbc);

        // Right side - Status with improved alignment
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 25, 5));
        rightPanel.setOpaque(false);

        statusLabel = new JLabel("[OFFLINE] Disconnected");
        statusLabel.setForeground(TEXT_COLOR);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        userCountLabel = new JLabel("[USERS] 0 online");
        userCountLabel.setForeground(TEXT_SECONDARY);
        userCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        rightPanel.add(userCountLabel);
        rightPanel.add(statusLabel);

        connectionPanel.add(leftPanel, BorderLayout.WEST);
        connectionPanel.add(rightPanel, BorderLayout.EAST);
    }

    private void createModernChatPanel() {
        chatPanel = new ModernPanel(new BorderLayout(15, 0), BACKGROUND_COLOR, 0, false);
        chatPanel.setBorder(new EmptyBorder(20, 30, 10, 30));

        // Modern chat area with improved readability
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setBackground(CARD_COLOR);
        chatArea.setForeground(TEXT_COLOR);
        chatArea.setFont(getEmojiCompatibleFont(Font.PLAIN, 15));
        chatArea.setBorder(new EmptyBorder(25, 25, 25, 25));

        // Custom document styling
        StyledDocument doc = chatArea.getStyledDocument();
        addAdvancedStylesToDocument(doc);

        // Modern scroll pane with custom styling
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder("[CHAT] Chat Messages"));
        chatScroll.setBackground(CARD_COLOR);
        chatScroll.getViewport().setBackground(CARD_COLOR);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Style scrollbar
        styleScrollBar(chatScroll);

        // Modern user list with improved readability
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userList.setBackground(CARD_COLOR);
        userList.setForeground(TEXT_COLOR);
        userList.setSelectionBackground(new Color(88, 86, 214, 100));
        userList.setSelectionForeground(Color.WHITE);
        userList.setBorder(new EmptyBorder(20, 20, 20, 20));
        // userList.setCellRenderer(new DefaultListCellRenderer());

        userScroll = new JScrollPane(userList);
        userScroll.setBorder(BorderFactory.createTitledBorder("[USERS] Online Users (0)"));
        userScroll.setPreferredSize(new Dimension(250, 0));
        userScroll.setBackground(CARD_COLOR);
        userScroll.getViewport().setBackground(CARD_COLOR);
        styleScrollBar(userScroll);

        // Add double-click interaction
        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && userList.getSelectedValue() != null) {
                    String user = userList.getSelectedValue();
                    messageField.setText("@" + user + " ");
                    messageField.requestFocusInWindow();
                    // Add selection animation
                    animateSelection();
                }
            }
        });

        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(userScroll, BorderLayout.EAST);
    }

    private void createModernInputPanel() {
        inputPanel = new ModernPanel(new BorderLayout(15, 0), BACKGROUND_COLOR, 0, false);
        inputPanel.setBorder(new EmptyBorder(10, 30, 30, 30));

        // Message input container with glassmorphism effect
        JPanel inputContainer = new JPanel(new BorderLayout(15, 0));
        inputContainer.setOpaque(false);

        // Create modern message field
        messageField = new ModernTextField("", 0, PRIMARY_COLOR, BORDER_COLOR);
        messageField.setFont(getEmojiCompatibleFont(Font.PLAIN, 14));
        messageField.setEnabled(false);
        messageField.setBorder(new EmptyBorder(12, 15, 12, 15));

        // Modern send button with glow effect
        sendButton = new ModernButton("[SEND] Send", PRIMARY_COLOR);
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> sendMessage());

        // Emoji button for future enhancement
        emojiButton = new ModernButton("[EMOJI] :)", ACCENT_COLOR);
        emojiButton.setEnabled(false);
        emojiButton.addActionListener(e -> showEmojiPanel());

        // Enter key to send message
        messageField.addActionListener(e -> sendMessage());

        // Advanced typing indicator with animation
        setupAdvancedTypingIndicator();

        // Create input layout with better alignment
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);
        messagePanel.setBorder(new LineBorder(CARD_COLOR, 2));
        messagePanel.add(messageField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.add(emojiButton);
        buttonPanel.add(sendButton);

        inputContainer.add(messagePanel, BorderLayout.CENTER);
        inputContainer.add(buttonPanel, BorderLayout.EAST);

        inputPanel.add(inputContainer, BorderLayout.CENTER);

        // Typing indicator with improved readability
        typingLabel = new JLabel(" ");
        typingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        typingLabel.setForeground(TEXT_SECONDARY);
        inputPanel.add(typingLabel, BorderLayout.SOUTH);
    }

    // Helper method to create glowing labels
    private JLabel createGlowLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return label;
    }

    // Advanced typing indicator setup
    private void setupAdvancedTypingIndicator() {
        messageField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private final javax.swing.Timer typingTimer = new javax.swing.Timer(2000, e -> sendTyping(false));

            {
                typingTimer.setRepeats(false);
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                onType();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                onType();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                onType();
            }

            private void onType() {
                if (isConnected) {
                    sendTyping(true);
                    typingTimer.restart();
                }
            }
        });
    }

    // Setup window effects
    private void setupWindowEffects() {
        // Add window focus effects
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                // statusLabel.stopPulse();
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                if (isConnected) {
                    // statusLabel.startPulse();
                }
            }
        });
    }

    // Style scroll bars with modern design
    private void styleScrollBar(JScrollPane scrollPane) {
        JScrollBar vBar = scrollPane.getVerticalScrollBar();
        JScrollBar hBar = scrollPane.getHorizontalScrollBar();

        vBar.setBackground(CARD_COLOR);
        hBar.setBackground(CARD_COLOR);
        vBar.setUI(new ModernScrollBarUI());
        hBar.setUI(new ModernScrollBarUI());
    }

    // Add advanced document styling
    private void addAdvancedStylesToDocument(StyledDocument doc) {
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontFamily(regular, "Segoe UI");
        StyleConstants.setFontSize(regular, 15);
        StyleConstants.setForeground(regular, TEXT_COLOR);

        Style system = doc.addStyle("system", regular);
        StyleConstants.setForeground(system, TEXT_SECONDARY);
        StyleConstants.setItalic(system, true);
        StyleConstants.setFontSize(system, 14);

        Style user = doc.addStyle("user", regular);
        StyleConstants.setForeground(user, PRIMARY_COLOR);
        StyleConstants.setBold(user, true);

        Style ownMessage = doc.addStyle("own", regular);
        StyleConstants.setForeground(ownMessage, ACCENT_COLOR);
        StyleConstants.setBold(ownMessage, true);

        Style timestamp = doc.addStyle("timestamp", regular);
        StyleConstants.setForeground(timestamp, TEXT_SECONDARY);
        StyleConstants.setFontSize(timestamp, 13);
    }

    // Animation for user selection
    private void animateSelection() {
        Timer selectionTimer = new Timer(100, null);
        final int[] pulse = {0};

        selectionTimer.addActionListener(e -> {
            pulse[0]++;
            if (pulse[0] > 3) {
                ((Timer) e.getSource()).stop();
            }
            userList.repaint();
        });
        selectionTimer.start();
    }

    // Show emoji panel with Unicode emojis
    private void showEmojiPanel() {
        String[] emojis = {
            "😀", "😂", "😍", "😊", "😎", "😢", "😡", "😭",
            "👍", "👎", "❤️", "💯", "🔥", "✨", "🎉", "👌",
            "🤔", "😴", "🙄", "😘", "🥰", "😋", "😜", "🤗"
        };
        String selected = (String) JOptionPane.showInputDialog(
                this,
                "Choose an emoji:",
                "Emoji Picker",
                JOptionPane.PLAIN_MESSAGE,
                null,
                emojis,
                emojis[0]);

        if (selected != null) {
            messageField.setText(messageField.getText() + selected);
            messageField.requestFocus();
        }
    }

    private void setupKeyboardShortcuts() {
        // Ctrl+Enter to send message
        messageField.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "send");
        messageField.getActionMap().put("send", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // Escape to clear message
        messageField.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clear");
        messageField.getActionMap().put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                messageField.setText("");
            }
        });
    }

    private void setupNetworking() {
        // Start message reader thread (survives disconnect/reconnect)
        Thread messageReader = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (!isConnected || input == null) {
                        Thread.sleep(100);
                        continue;
                    }

                    String message;
                    while (isConnected && input != null && (message = input.readLine()) != null) {
                        handleIncomingMessage(message);
                    }

                    // Connection dropped (readLine returned null)
                    if (isConnected) {
                        SwingUtilities.invokeLater(() -> appendToChat("Connection lost to server\n", "system"));
                        disconnectFromServer();
                    }
                } catch (IOException e) {
                    if (isConnected) {
                        SwingUtilities.invokeLater(() -> appendToChat(
                                "Error reading from server: " + e.getMessage() + "\n", "system"));
                        disconnectFromServer();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        messageReader.setDaemon(true);
        messageReader.start();
    }

    private void toggleConnection() {
        if (isConnected) {
            disconnectFromServer();
        } else {
            connectToServer();
        }
    }

    private void connectToServer() {
        if (isConnected) {
            return;
        }

        try {
            // Animate connection process
            connectButton.setEnabled(false);
            statusLabel.setText("[CONNECTING] Connecting...");
            // statusLabel.startPulse();

            serverAddress = serverField.getText().trim();
            serverPort = Integer.parseInt(portField.getText());
            username = usernameField.getText().trim();
            if (username.isEmpty()) {
                username = "Anonymous";
            }

            socket = new Socket(serverAddress, serverPort);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            isConnected = true;

            // Send username first
            output.println("USERNAME|" + username);

            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("[CONNECTED] Connected as " + username);
                // statusLabel.stopPulse();
                setTitle("[CLIENT] Elite Chat Client - " + username);
                appendToChat("[WELCOME] Connected to server as " + username + "\n", "system");
                updateConnectionStatus();

                // Celebration animation
                Timer celebrationTimer = new Timer(100, null);
                final int[] count = {0};
                celebrationTimer.addActionListener(e -> {
                    count[0]++;
                    if (count[0] > 5) {
                        ((Timer) e.getSource()).stop();
                    }
                    statusLabel.setText(
                            count[0] % 2 == 0 ? "[CONNECTED] Connected! ***" : "[CONNECTED] Connected as " + username);
                });
                celebrationTimer.start();
            });

        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("[ERROR] Connection failed");
                // statusLabel.stopPulse();
                connectButton.setEnabled(true);
                connectButton.setText("[CONNECT] Connect");

                // Modern error dialog
                JOptionPane.showMessageDialog(this,
                        "Failed to connect to server:\n" + e.getMessage(),
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
            });
        } catch (NumberFormatException e) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("[ERROR] Invalid port");
                // statusLabel.stopPulse();
                connectButton.setEnabled(true);
                connectButton.setText("[CONNECT] Connect");

                JOptionPane.showMessageDialog(this,
                        "Please enter a valid port number",
                        "Input Error",
                        JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private void disconnectFromServer() {
        if (!isConnected) {
            return;
        }

        isConnected = false;
        try {
            if (output != null) {
                output.println("/quit");
                output.close();
            }
            if (input != null) {
                input.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore
        }

        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("[OFFLINE] Disconnected");
            // statusLabel.stopPulse();
            appendToChat("[BYE] You left the chat\n", "system");
            userListModel.clear();
            userCountLabel.setText("[USERS] 0 online");
            updateConnectionStatus();

            // Reset title
            setTitle("[CLIENT] Elite Chat Client");
        });
    }

    private void updateConnectionStatus() {
        boolean connected = isConnected;
        // Allow disconnect via the same button
        connectButton.setEnabled(true);
        connectButton.setText(connected ? "[DISCONNECT] Disconnect" : "[CONNECT] Connect");
        sendButton.setEnabled(connected);
        emojiButton.setEnabled(connected);
        messageField.setEnabled(connected);

        // Update field states with animation
        serverField.setEnabled(!connected);
        portField.setEnabled(!connected);
        usernameField.setEnabled(!connected);

        if (connected) {
            messageField.requestFocusInWindow();
        }
    }

    private void sendMessage() {
        if (!isConnected || messageField.getText().trim().isEmpty()) {
            return;
        }

        String message = messageField.getText().trim();

        // If message starts with '@name ...', send as a private message
        if (message.startsWith("@")) {
            int spaceIdx = message.indexOf(' ');
            if (spaceIdx > 1) {
                String toUser = message.substring(1, spaceIdx).trim();
                String content = message.substring(spaceIdx + 1).trim();
                if (!toUser.isEmpty() && !content.isEmpty()) {
                    output.println("/w " + toUser + " " + content);
                }
            } else {
                // No space/content after mention, ignore for now
            }
        } else {
            // Send message to server - don't display locally to avoid duplication
            output.println(message);
        }
        messageField.setText("");
        sendTyping(false);
    }

    private void handleIncomingMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                String[] head = message.split("\\|", 2);
                String messageType = head[0];
                String rest = head.length == 2 ? head[1] : "";

                switch (messageType) {
                    case "PING" -> {
                        // reply PONG immediately
                        if (output != null) {
                            output.println("PONG");
                        }
                    }
                    case "CHAT" -> {
                        // CHAT|ts|sender|content
                        String[] p = rest.split("\\|", 3);
                        if (p.length < 3) {
                            appendToChat("Server: " + message + "\n", "regular");
                            return;
                        }
                        String timestamp = p[0];
                        String sender = p[1];
                        String content = p[2];
                        appendToChat("[" + timestamp + "] " + sender + ": " + content + "\n", "user");
                    }
                    case "PRIVATE" -> {
                        // PRIVATE|ts|from|to|content
                        String[] p = rest.split("\\|", 4);
                        if (p.length < 4) {
                            appendToChat("Server: " + message + "\n", "regular");
                            return;
                        }
                        String timestamp = p[0];
                        String from = p[1];
                        String to = p[2];
                        String content = p[3];
                        String label = to != null && to.equalsIgnoreCase(username) ? "[PM]" : "[PM to " + to + "]";
                        appendToChat("[" + timestamp + "] " + label + " " + from + ": " + content + "\n", "own");
                    }
                    case "JOIN", "LEAVE", "SYSTEM" -> {
                        // TYPE|ts|content
                        String[] p = rest.split("\\|", 2);
                        if (p.length < 2) {
                            appendToChat("Server: " + message + "\n", "regular");
                            return;
                        }
                        String timestamp = p[0];
                        String content = p[1];
                        String prefix = switch (messageType) {
                            case "JOIN" ->
                                "*** ";
                            case "LEAVE" ->
                                "*** ";
                            default ->
                                "* ";
                        };
                        String suffix = switch (messageType) {
                            case "JOIN" ->
                                " joined ***";
                            case "LEAVE" ->
                                " left ***";
                            default ->
                                "";
                        };
                        appendToChat("[" + timestamp + "] " + prefix + content + suffix + "\n", "system");
                    }
                    case "USERLIST" -> {
                        // USERLIST|ts|user1,user2,...
                        String[] p = rest.split("\\|", 2);
                        String content = p.length == 2 ? p[1] : "";
                        updateUserList(content);
                    }
                    case "TYPING" -> {
                        // TYPING|ts|username|true/false
                        String[] p = rest.split("\\|", 3);
                        if (p.length < 3) {
                            return;
                        }
                        String who = p[1];
                        boolean typing = Boolean.parseBoolean(p[2]);
                        if (who != null && !who.equals(username)) {
                            typingLabel.setText(typing ? (who + " is typing…") : " ");
                        }
                    }
                    default ->
                        appendToChat("Server: " + message + "\n", "regular");
                }
            } catch (Exception e) {
                // Fallback for any parsing errors
                appendToChat("Server: " + message + "\n", "regular");
            }
        });
    }

    private void updateUserList(String userListStr) {
        userListModel.clear();
        if (userListStr != null && !userListStr.trim().isEmpty()) {
            String[] users = userListStr.split(",");
            for (String user : users) {
                if (!user.trim().isEmpty()) {
                    userListModel.addElement(user.trim());
                }
            }
        }

        // Update user count
        userCountLabel.setText("[USERS] " + userListModel.size() + " online");

        // Update the user list title with count
        if (userScroll != null && userScroll.getBorder() instanceof TitledBorder border) {
            border.setTitle("[USERS] Online Users (" + userListModel.size() + ")");
            userScroll.repaint();
        }
    }

    private void appendToChat(String text, String style) {
        StyledDocument doc = chatArea.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), text, doc.getStyle(style));
            // Auto scroll to bottom
            chatArea.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            // Ignore
        }
    }

    private void sendTyping(boolean typing) {
        if (isConnected && output != null && username != null) {
            output.println("TYPING|" + username + "|" + typing);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AdvancedClient().setVisible(true);
        });
    }
}
