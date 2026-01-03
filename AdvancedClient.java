
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import javax.swing.*;
import javax.swing.border.*;

public class AdvancedClient extends JFrame {

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private volatile boolean isConnected = false;

    // Modern UI Components
    private JPanel chatArea; // Changed to JPanel for Discord-style messages
    private JScrollPane chatScroll;
    private ModernUI.ModernTextField messageField;
    private ModernUI.ModernButton connectButton, sendButton, emojiButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JScrollPane userScroll;
    private ModernUI.ModernPanel connectionPanel, chatPanel, inputPanel;
    private JLabel statusLabel, userCountLabel;
    private JLabel typingLabel;
    private ModernUI.ModernTextField serverField, portField, usernameField;
    private String lastMessageSender = null; // Track last sender for grouping

    private String serverAddress = "localhost";
    private int serverPort = 12345;
    private String username = "Anonymous";

    // Modern Dark Theme Color Scheme (from ModernUI.ThemeColors)
    private final Color PRIMARY_COLOR = ModernUI.ThemeColors.PRIMARY;
    private final Color ACCENT_COLOR = ModernUI.ThemeColors.ERROR;
    private final Color SUCCESS_COLOR = ModernUI.ThemeColors.SUCCESS;
    private final Color BACKGROUND_COLOR = ModernUI.ThemeColors.BACKGROUND;
    private final Color SURFACE_COLOR = ModernUI.ThemeColors.SURFACE;
    private final Color CARD_COLOR = ModernUI.ThemeColors.CARD;
    private final Color TEXT_COLOR = ModernUI.ThemeColors.TEXT_PRIMARY;
    private final Color TEXT_SECONDARY = ModernUI.ThemeColors.TEXT_SECONDARY;
    private final Color BORDER_COLOR = ModernUI.ThemeColors.BORDER;

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
        // Setup modern look and feel
        ModernUI.setupLookAndFeel();

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
        connectionPanel = new ModernUI.ModernPanel(SURFACE_COLOR);
        connectionPanel.setLayout(new BorderLayout(10, 0));
        connectionPanel.setBorder(new EmptyBorder(18, 28, 18, 28));

        // Initialize connection fields with ModernUI.ModernTextField
        serverField = new ModernUI.ModernTextField("localhost");
        serverField.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 13));
        serverField.setPreferredSize(new Dimension(140, 42));
        serverField.setToolTipText("Enter server IP (e.g., 192.168.1.x for local network)");

        portField = new ModernUI.ModernTextField("12345");
        portField.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 13));
        portField.setPreferredSize(new Dimension(100, 42));
        portField.setToolTipText("Server port (default: 12345)");

        usernameField = new ModernUI.ModernTextField("Your name");
        usernameField.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 13));
        usernameField.setPreferredSize(new Dimension(140, 42));
        usernameField.setToolTipText("Your display name (leave empty for 'Anonymous')");

        // Left side - Connection controls with improved alignment
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 12, 0, 12);
        gbc.anchor = GridBagConstraints.WEST;

        // Server field
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel serverLabel = createGlowLabel("🖥️ Server:", TEXT_COLOR);
        serverLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 12));
        leftPanel.add(serverLabel, gbc);
        gbc.gridx = 1;
        leftPanel.add(serverField, gbc);

        // Port field
        gbc.gridx = 2;
        gbc.gridy = 0;
        JLabel portLabelConn = createGlowLabel("🔌 Port:", TEXT_COLOR);
        portLabelConn.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 12));
        leftPanel.add(portLabelConn, gbc);
        gbc.gridx = 3;
        leftPanel.add(portField, gbc);

        // Username field
        gbc.gridx = 4;
        gbc.gridy = 0;
        JLabel usernameLabel = createGlowLabel("👤 Username:", TEXT_COLOR);
        usernameLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 12));
        leftPanel.add(usernameLabel, gbc);
        gbc.gridx = 5;
        leftPanel.add(usernameField, gbc);

        // Connect button
        gbc.gridx = 6;
        gbc.gridy = 0;
        connectButton = new ModernUI.ModernButton("🔗 Connect", SUCCESS_COLOR);
        connectButton.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 13));
        connectButton.addActionListener(e -> toggleConnection());
        leftPanel.add(connectButton, gbc);

        // Right side - Status with improved alignment
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 8));
        rightPanel.setOpaque(false);

        statusLabel = new JLabel("⚫ Offline");
        statusLabel.setForeground(TEXT_COLOR);
        statusLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 16));

        userCountLabel = new JLabel("👥 0 online");
        userCountLabel.setForeground(TEXT_SECONDARY);
        userCountLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 14));

        rightPanel.add(userCountLabel);
        rightPanel.add(statusLabel);

        connectionPanel.add(leftPanel, BorderLayout.WEST);
        connectionPanel.add(rightPanel, BorderLayout.EAST);
    }

    private void createModernChatPanel() {
        chatPanel = new ModernUI.ModernPanel(BACKGROUND_COLOR);
        chatPanel.setLayout(new BorderLayout(15, 12));
        chatPanel.setBorder(new EmptyBorder(18, 28, 18, 28));

        // Discord-style chat area with message bubbles
        chatArea = new JPanel();
        chatArea.setLayout(new BoxLayout(chatArea, BoxLayout.Y_AXIS));
        chatArea.setBackground(CARD_COLOR);
        chatArea.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Modern scroll pane with custom styling
        chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(new CompoundBorder(
                new ModernUI.ModernTitledBorder("💬 Messages"),
                new EmptyBorder(10, 10, 10, 10)));
        chatScroll.setBackground(CARD_COLOR);
        chatScroll.getViewport().setBackground(CARD_COLOR);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Style scrollbar
        styleScrollBar(chatScroll);

        // Modern user list with improved readability
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 14));
        userList.setFixedCellHeight(32);
        userList.setBackground(CARD_COLOR);
        userList.setForeground(TEXT_COLOR);
        userList.setSelectionBackground(ModernUI.ThemeColors.TABLE_SELECTION);
        userList.setSelectionForeground(TEXT_COLOR);
        userList.setBorder(new EmptyBorder(20, 20, 20, 20));

        userScroll = new JScrollPane(userList);
        userScroll.setBorder(new CompoundBorder(
                new ModernUI.ModernTitledBorder("👥 Online (0)"),
                new EmptyBorder(10, 10, 10, 10)));
        userScroll.setPreferredSize(new Dimension(240, 0));
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
        inputPanel = new ModernUI.ModernPanel(BACKGROUND_COLOR);
        inputPanel.setLayout(new BorderLayout(15, 12));
        inputPanel.setBorder(new EmptyBorder(18, 28, 24, 28));

        // Message input container with glassmorphism effect
        JPanel inputContainer = new JPanel(new BorderLayout(15, 0));
        inputContainer.setOpaque(false);

        // Create modern message field
        messageField = new ModernUI.ModernTextField("Type your message...");
        messageField.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 14));
        messageField.setEnabled(false);

        // Modern send button with glow effect
        sendButton = new ModernUI.ModernButton("📤 Send", PRIMARY_COLOR);
        sendButton.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 13));
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> sendMessage());

        // Emoji button for future enhancement
        emojiButton = new ModernUI.ModernButton("😊 Emoji", ACCENT_COLOR);
        emojiButton.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 13));
        emojiButton.setEnabled(false);
        emojiButton.addActionListener(e -> showEmojiPanel());

        // Enter key to send message
        messageField.addActionListener(e -> sendMessage());

        // Advanced typing indicator with animation
        setupAdvancedTypingIndicator();

        // Create input layout with better alignment
        ModernUI.ModernCard messagePanel = new ModernUI.ModernCard(CARD_COLOR, false);
        messagePanel.setLayout(new BorderLayout());
        messagePanel.setBorder(new EmptyBorder(6, 10, 6, 10));
        messagePanel.add(messageField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        buttonPanel.setOpaque(false);
        buttonPanel.add(emojiButton);
        buttonPanel.add(sendButton);

        inputContainer.add(messagePanel, BorderLayout.CENTER);
        inputContainer.add(buttonPanel, BorderLayout.EAST);

        inputPanel.add(inputContainer, BorderLayout.CENTER);

        // Typing indicator with improved readability
        typingLabel = new JLabel(" ");
        typingLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.ITALIC, 13));
        typingLabel.setForeground(TEXT_SECONDARY);
        typingLabel.setBorder(new EmptyBorder(8, 4, 0, 4));
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
        vBar.setUI(new ModernUI.ModernScrollBarUI());
        hBar.setUI(new ModernUI.ModernScrollBarUI());
    }

    // Create Discord-style message component
    private JPanel createMessageBubble(String sender, String content, String timestamp, String messageType) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout(0, 4));
        messagePanel.setOpaque(false);
        messagePanel.setBorder(new EmptyBorder(6, 0, 6, 0));
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // Determine colors based on message type
        Color senderColor = PRIMARY_COLOR;
        Color contentColor = TEXT_COLOR;
        String avatarEmoji = "👤";

        if (messageType.equals("own")) {
            senderColor = ACCENT_COLOR;
            avatarEmoji = "🔷";
        } else if (messageType.equals("system")) {
            senderColor = TEXT_SECONDARY;
            contentColor = TEXT_SECONDARY;
            avatarEmoji = "ℹ️";
        } else if (messageType.equals("private")) {
            senderColor = new Color(180, 100, 200);
            avatarEmoji = "✉️";
        } else {
            // Assign color based on sender name hash for consistency
            int hash = sender.hashCode();
            Color[] userColors = {
                new Color(88, 101, 242),
                new Color(67, 181, 129),
                new Color(250, 166, 26),
                new Color(237, 66, 69),
                new Color(155, 89, 182),
                new Color(26, 188, 156),
                new Color(241, 196, 15)
            };
            senderColor = userColors[Math.abs(hash) % userColors.length];
            avatarEmoji = getAvatarForUser(sender);
        }

        // Header panel (avatar + username + timestamp)
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerPanel.setOpaque(false);

        // Avatar
        JLabel avatarLabel = new JLabel(avatarEmoji);
        avatarLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 24));
        avatarLabel.setPreferredSize(new Dimension(36, 36));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setVerticalAlignment(SwingConstants.TOP);
        headerPanel.add(avatarLabel);

        // Username
        JLabel usernameLabel = new JLabel(sender);
        usernameLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 14));
        usernameLabel.setForeground(senderColor);
        headerPanel.add(usernameLabel);

        // Timestamp
        JLabel timestampLabel = new JLabel(timestamp);
        timestampLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 11));
        timestampLabel.setForeground(TEXT_SECONDARY);
        headerPanel.add(timestampLabel);

        // Content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(0, 44, 0, 0)); // Indent to align with text after avatar

        // Message content with word wrap
        JTextArea contentArea = new JTextArea(content);
        contentArea.setEditable(false);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 14));
        contentArea.setForeground(contentColor);
        contentArea.setBackground(CARD_COLOR);
        contentArea.setBorder(null);
        contentArea.setOpaque(false);
        contentPanel.add(contentArea, BorderLayout.CENTER);

        messagePanel.add(headerPanel, BorderLayout.NORTH);
        messagePanel.add(contentPanel, BorderLayout.CENTER);

        // Add hover effect
        messagePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                messagePanel.setOpaque(true);
                messagePanel.setBackground(new Color(CARD_COLOR.getRed() + 8, CARD_COLOR.getGreen() + 8, CARD_COLOR.getBlue() + 8));
                messagePanel.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                messagePanel.setOpaque(false);
                messagePanel.repaint();
            }
        });

        return messagePanel;
    }

    // Create compact message (when same user sends multiple messages)
    private JPanel createCompactMessage(String content, String messageType) {
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);
        messagePanel.setBorder(new EmptyBorder(2, 44, 2, 0)); // Indent to align with previous message
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        Color contentColor = messageType.equals("system") ? TEXT_SECONDARY : TEXT_COLOR;

        // Message content with word wrap
        JTextArea contentArea = new JTextArea(content);
        contentArea.setEditable(false);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 14));
        contentArea.setForeground(contentColor);
        contentArea.setBackground(CARD_COLOR);
        contentArea.setBorder(null);
        contentArea.setOpaque(false);
        messagePanel.add(contentArea, BorderLayout.CENTER);

        // Add hover effect
        messagePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                messagePanel.setOpaque(true);
                messagePanel.setBackground(new Color(CARD_COLOR.getRed() + 8, CARD_COLOR.getGreen() + 8, CARD_COLOR.getBlue() + 8));
                messagePanel.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                messagePanel.setOpaque(false);
                messagePanel.repaint();
            }
        });

        return messagePanel;
    }

    // Get consistent avatar emoji for each user
    private String getAvatarForUser(String username) {
        String[] avatars = {"👤", "😀", "😎", "🤖", "👻", "🦊", "🐱", "🐶", "🐼", "🦁", "🐯", "🐸"};
        int index = Math.abs(username.hashCode()) % avatars.length;
        return avatars[index];
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
                        SwingUtilities.invokeLater(() -> appendDiscordMessage("System", "Connection lost to server", getCurrentTime(), "system"));
                        disconnectFromServer();
                    }
                } catch (IOException e) {
                    if (isConnected) {
                        SwingUtilities.invokeLater(() -> appendDiscordMessage("System",
                                "Error reading from server: " + e.getMessage(), getCurrentTime(), "system"));
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
            statusLabel.setText("🔄 Connecting...");
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
                statusLabel.setText("🟢 Connected as " + username);
                // statusLabel.stopPulse();
                setTitle("💬 Elite Chat Client - " + username);
                appendDiscordMessage("System", "✨ Welcome! Connected as " + username, getCurrentTime(), "system");
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
                            count[0] % 2 == 0 ? "🟢 Connected! ✨" : "🟢 Connected as " + username);
                });
                celebrationTimer.start();
            });

        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("🔴 Connection failed");
                // statusLabel.stopPulse();
                connectButton.setEnabled(true);
                connectButton.setText("🔗 Connect");

                // Modern error dialog
                JOptionPane.showMessageDialog(this,
                        "Failed to connect to server:\n" + e.getMessage(),
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
            });
        } catch (NumberFormatException e) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("🔴 Invalid port");
                // statusLabel.stopPulse();
                connectButton.setEnabled(true);
                connectButton.setText("🔗 Connect");

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
            statusLabel.setText("⚫ Offline");
            // statusLabel.stopPulse();
            appendDiscordMessage("System", "👋 You left the chat", getCurrentTime(), "system");
            userListModel.clear();
            userCountLabel.setText("👥 0 online");
            updateConnectionStatus();

            // Reset title
            setTitle("💬 Elite Chat Client");
        });
    }

    private void updateConnectionStatus() {
        boolean connected = isConnected;
        // Allow disconnect via the same button
        connectButton.setEnabled(true);
        connectButton.setText(connected ? "🔌 Disconnect" : "🔗 Connect");
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
                            appendDiscordMessage("Server", message, getCurrentTime(), "system");
                            return;
                        }
                        String timestamp = p[0];
                        String sender = p[1];
                        String content = p[2];
                        appendDiscordMessage(sender, content, timestamp, "user");
                    }
                    case "PRIVATE" -> {
                        // PRIVATE|ts|from|to|content
                        String[] p = rest.split("\\|", 4);
                        if (p.length < 4) {
                            appendDiscordMessage("Server", message, getCurrentTime(), "system");
                            return;
                        }
                        String timestamp = p[0];
                        String from = p[1];
                        String to = p[2];
                        String content = p[3];
                        String displayName = to != null && to.equalsIgnoreCase(username) ? from + " (private)" : "You → " + to;
                        appendDiscordMessage(displayName, content, timestamp, "private");
                    }
                    case "JOIN", "LEAVE", "SYSTEM" -> {
                        // TYPE|ts|content
                        String[] p = rest.split("\\|", 2);
                        if (p.length < 2) {
                            appendDiscordMessage("Server", message, getCurrentTime(), "system");
                            return;
                        }
                        String timestamp = p[0];
                        String content = p[1];
                        String displayText = switch (messageType) {
                            case "JOIN" ->
                                content + " joined the chat";
                            case "LEAVE" ->
                                content + " left the chat";
                            default ->
                                content;
                        };
                        appendDiscordMessage("System", displayText, timestamp, "system");
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
                        appendDiscordMessage("Server", message, getCurrentTime(), "system");
                }
            } catch (Exception e) {
                // Fallback for any parsing errors
                appendDiscordMessage("Server", message, getCurrentTime(), "system");
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
        userCountLabel.setText("👥 " + userListModel.size() + " online");

        // Update the user list title with count
        if (userScroll != null && userScroll.getBorder() instanceof CompoundBorder cb) {
            if (cb.getOutsideBorder() instanceof ModernUI.ModernTitledBorder border) {
                border.setTitle("👥 Online (" + userListModel.size() + ")");
                userScroll.repaint();
            }
        }
    }

    private void appendDiscordMessage(String sender, String content, String timestamp, String messageType) {
        SwingUtilities.invokeLater(() -> {
            // Check if we should group with previous message (same sender, within short time)
            boolean groupWithPrevious = sender.equals(lastMessageSender) && !messageType.equals("system");

            JPanel messageComponent;
            if (groupWithPrevious) {
                messageComponent = createCompactMessage(content, messageType);
            } else {
                messageComponent = createMessageBubble(sender, content, timestamp, messageType);
                lastMessageSender = sender;
            }

            chatArea.add(messageComponent);
            chatArea.revalidate();

            // Auto-scroll to bottom
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScroll.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        });
    }

    private String getCurrentTime() {
        java.time.LocalTime now = java.time.LocalTime.now();
        return String.format("%02d:%02d", now.getHour(), now.getMinute());
    }

    private void clearChat() {
        chatArea.removeAll();
        lastMessageSender = null;
        chatArea.revalidate();
        chatArea.repaint();
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
