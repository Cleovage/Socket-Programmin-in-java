
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
    private Timer typingDotsTimer;
    private String typingBaseText = "";
    private boolean soundEnabled = true;
    private boolean notificationsEnabled = true;
    private JMenuBar menuBar;
    private ModernUI.ModernButton clearChatButton, settingsButton;

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

        // Create modern menu bar
        createModernMenuBar();

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

    // Lightweight animated wrapper for fade + slide-in effects
    private static class AnimatedPanel extends JPanel {

        private float alpha = 0f;
        private int translateY = 8;
        private Timer fadeTimer;

        AnimatedPanel(JComponent child) {
            setOpaque(false);
            setLayout(new BorderLayout());
            add(child, BorderLayout.CENTER);
        }

        void playEntrance() {
            alpha = 0f;
            translateY = 8;
            if (fadeTimer != null) {
                fadeTimer.stop();
            }

            fadeTimer = new Timer(16, e -> {
                alpha = Math.min(1f, alpha + 0.08f);
                translateY = Math.max(0, (int) (8 * (1f - alpha)));
                repaint();
                if (alpha >= 1f) {
                    ((Timer) e.getSource()).stop();
                }
            });
            fadeTimer.start();
        }

        @Override
        protected void paintChildren(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(0, translateY);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            super.paintChildren(g2);
            g2.dispose();
        }
    }

    // Pulsing status indicator animation
    private static class PulsingLabel extends JLabel {

        private float pulseAlpha = 1f;
        private boolean pulsing = false;
        private Timer pulseTimer;
        private boolean increasing = false;

        PulsingLabel(String text) {
            super(text);
        }

        void startPulse() {
            if (pulsing) {
                return;
            }
            pulsing = true;
            pulseTimer = new Timer(50, e -> {
                if (increasing) {
                    pulseAlpha = Math.min(1f, pulseAlpha + 0.05f);
                    if (pulseAlpha >= 1f) {
                        increasing = false;
                    }
                } else {
                    pulseAlpha = Math.max(0.4f, pulseAlpha - 0.05f);
                    if (pulseAlpha <= 0.4f) {
                        increasing = true;
                    }
                }
                repaint();
            });
            pulseTimer.start();
        }

        void stopPulse() {
            pulsing = false;
            pulseAlpha = 1f;
            if (pulseTimer != null) {
                pulseTimer.stop();
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulseAlpha));
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    // Create modern menu bar with settings
    private void createModernMenuBar() {
        menuBar = new JMenuBar();
        menuBar.setBackground(SURFACE_COLOR);
        menuBar.setBorder(new EmptyBorder(4, 8, 4, 8));

        // File menu
        JMenu fileMenu = createStyledMenu("📁 File");
        JMenuItem exportChat = createStyledMenuItem("💾 Export Chat");
        exportChat.addActionListener(e -> exportChatToFile());
        JMenuItem exitItem = createStyledMenuItem("🚪 Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exportChat);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Edit menu
        JMenu editMenu = createStyledMenu("✏️ Edit");
        JMenuItem clearChat = createStyledMenuItem("🗑️ Clear Chat");
        clearChat.addActionListener(e -> clearChat());
        JMenuItem copySelected = createStyledMenuItem("📋 Copy Selected");
        copySelected.addActionListener(e -> copySelectedText());
        editMenu.add(clearChat);
        editMenu.add(copySelected);

        // Settings menu
        JMenu settingsMenu = createStyledMenu("⚙️ Settings");
        JCheckBoxMenuItem soundToggle = new JCheckBoxMenuItem("🔊 Sound Notifications", soundEnabled);
        soundToggle.setBackground(SURFACE_COLOR);
        soundToggle.setForeground(TEXT_COLOR);
        soundToggle.addActionListener(e -> soundEnabled = soundToggle.isSelected());
        JCheckBoxMenuItem notifToggle = new JCheckBoxMenuItem("🔔 Desktop Notifications", notificationsEnabled);
        notifToggle.setBackground(SURFACE_COLOR);
        notifToggle.setForeground(TEXT_COLOR);
        notifToggle.addActionListener(e -> notificationsEnabled = notifToggle.isSelected());
        settingsMenu.add(soundToggle);
        settingsMenu.add(notifToggle);

        // Help menu
        JMenu helpMenu = createStyledMenu("❓ Help");
        JMenuItem shortcuts = createStyledMenuItem("⌨️ Keyboard Shortcuts");
        shortcuts.addActionListener(e -> showShortcutsDialog());
        JMenuItem about = createStyledMenuItem("ℹ️ About");
        about.addActionListener(e -> showAboutDialog());
        helpMenu.add(shortcuts);
        helpMenu.add(about);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(settingsMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private JMenu createStyledMenu(String text) {
        JMenu menu = new JMenu(text);
        menu.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 12));
        menu.setForeground(TEXT_COLOR);
        menu.setOpaque(true);
        menu.setBackground(SURFACE_COLOR);
        return menu;
    }

    private JMenuItem createStyledMenuItem(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 12));
        item.setForeground(TEXT_COLOR);
        item.setBackground(SURFACE_COLOR);
        return item;
    }

    // Export chat to file
    private void exportChatToFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Chat");
        chooser.setSelectedFile(new java.io.File("chat_export_" + System.currentTimeMillis() + ".txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(chooser.getSelectedFile())) {
                pw.println("Chat Export - " + java.time.LocalDateTime.now());
                pw.println("=".repeat(50));
                // Export visible messages (simplified)
                pw.println("[Chat history exported]");
                JOptionPane.showMessageDialog(this, "Chat exported successfully!", "Export", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to export: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Copy selected text
    private void copySelectedText() {
        // Copy from message field if focused
        String selected = messageField.getSelectedText();
        if (selected != null && !selected.isEmpty()) {
            java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(selected);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        }
    }

    // Show keyboard shortcuts dialog
    private void showShortcutsDialog() {
        String shortcuts = """
            ⌨️ Keyboard Shortcuts
            
            Enter          - Send message
            Ctrl+Enter     - Send message
            Escape         - Clear message field
            Ctrl+L         - Clear chat
            Double-click user - Start private message
            @username      - Send private message
            """;
        JOptionPane.showMessageDialog(this, shortcuts, "Keyboard Shortcuts", JOptionPane.INFORMATION_MESSAGE);
    }

    // Show about dialog
    private void showAboutDialog() {
        String about = """
            💬 Elite Chat Client v2.0
            
            A modern Discord-style chat application
            Built with Java Swing & ModernUI
            
            Features:
            • Real-time messaging
            • Private messages (@user)
            • Typing indicators
            • Emoji support
            • Dark theme UI
            """;
        JOptionPane.showMessageDialog(this, about, "About Elite Chat", JOptionPane.INFORMATION_MESSAGE);
    }

    // Play notification sound
    private void playNotificationSound() {
        if (!soundEnabled) {
            return;
        }
        Toolkit.getDefaultToolkit().beep();
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
        chatArea.setBorder(new EmptyBorder(4, 4, 4, 4));

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
        inputPanel.setBorder(new EmptyBorder(12, 28, 18, 28));

        // Message input container with glassmorphism effect
        JPanel inputContainer = new JPanel(new BorderLayout(10, 0));
        inputContainer.setOpaque(false);

        // Create modern message field
        messageField = new ModernUI.ModernTextField("Type your message...");
        messageField.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 14));
        messageField.setEnabled(false);

        // Modern send button with glow effect
        sendButton = new ModernUI.ModernButton("📤 Send", PRIMARY_COLOR);
        sendButton.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 13));
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> sendMessageWithAnimation());

        // Emoji button
        emojiButton = new ModernUI.ModernButton("😊", ACCENT_COLOR);
        emojiButton.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 16));
        emojiButton.setPreferredSize(new Dimension(50, 38));
        emojiButton.setEnabled(false);
        emojiButton.addActionListener(e -> showEmojiPanel());

        // Clear chat button
        clearChatButton = new ModernUI.ModernButton("🗑️", new Color(100, 100, 100));
        clearChatButton.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 14));
        clearChatButton.setPreferredSize(new Dimension(50, 38));
        clearChatButton.setToolTipText("Clear chat (Ctrl+L)");
        clearChatButton.addActionListener(e -> clearChatWithAnimation());

        // Enter key to send message
        messageField.addActionListener(e -> sendMessageWithAnimation());

        // Advanced typing indicator with animation
        setupAdvancedTypingIndicator();

        // Create input layout with better alignment
        ModernUI.ModernCard messagePanel = new ModernUI.ModernCard(CARD_COLOR, false);
        messagePanel.setLayout(new BorderLayout());
        messagePanel.setBorder(new EmptyBorder(6, 10, 6, 10));
        messagePanel.add(messageField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttonPanel.setOpaque(false);
        buttonPanel.add(clearChatButton);
        buttonPanel.add(emojiButton);
        buttonPanel.add(sendButton);

        inputContainer.add(messagePanel, BorderLayout.CENTER);
        inputContainer.add(buttonPanel, BorderLayout.EAST);

        inputPanel.add(inputContainer, BorderLayout.CENTER);

        // Typing indicator with improved readability
        typingLabel = new JLabel(" ");
        typingLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.ITALIC, 12));
        typingLabel.setForeground(TEXT_SECONDARY);
        typingLabel.setBorder(new EmptyBorder(6, 4, 0, 4));
        inputPanel.add(typingLabel, BorderLayout.SOUTH);
    }

    // Send message with button animation
    private void sendMessageWithAnimation() {
        if (!isConnected || messageField.getText().trim().isEmpty()) {
            // Shake animation for empty message
            if (messageField.getText().trim().isEmpty() && isConnected) {
                shakeComponent(messageField);
            }
            return;
        }
        sendMessage();
        // Flash send button
        flashComponent(sendButton);
    }

    // Clear chat with fade animation
    private void clearChatWithAnimation() {
        if (chatArea.getComponentCount() == 0) {
            return;
        }

        // Fade out animation
        Timer fadeTimer = new Timer(30, null);
        final float[] alpha = {1f};
        fadeTimer.addActionListener(e -> {
            alpha[0] -= 0.1f;
            if (alpha[0] <= 0) {
                ((Timer) e.getSource()).stop();
                clearChat();
            }
            chatArea.repaint();
        });
        fadeTimer.start();
    }

    // Shake animation for components
    private void shakeComponent(JComponent comp) {
        Point originalLocation = comp.getLocation();
        Timer shakeTimer = new Timer(30, null);
        final int[] count = {0};
        final int[] offset = {5, -5, 4, -4, 3, -3, 2, -2, 1, -1, 0};
        shakeTimer.addActionListener(e -> {
            if (count[0] >= offset.length) {
                comp.setLocation(originalLocation);
                ((Timer) e.getSource()).stop();
                return;
            }
            comp.setLocation(originalLocation.x + offset[count[0]], originalLocation.y);
            count[0]++;
        });
        shakeTimer.start();
    }

    // Flash animation for components
    private void flashComponent(JComponent comp) {
        Color original = comp.getBackground();
        Timer flashTimer = new Timer(50, null);
        final int[] count = {0};
        flashTimer.addActionListener(e -> {
            count[0]++;
            if (count[0] > 4) {
                ((Timer) e.getSource()).stop();
                return;
            }
            // Simple repaint to trigger visual feedback
            comp.repaint();
        });
        flashTimer.start();
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

    // Animated typing indicator (ellipsis pulse)
    private void setTypingIndicator(String baseText, boolean active) {
        typingBaseText = baseText == null ? "" : baseText;
        if (typingDotsTimer != null) {
            typingDotsTimer.stop();
        }

        if (!active || typingBaseText.trim().isEmpty()) {
            typingLabel.setText(" ");
            return;
        }

        typingLabel.setText(typingBaseText + "...");
        typingDotsTimer = new Timer(420, e -> {
            String dots = switch ((int) (System.currentTimeMillis() / 420 % 4)) {
                case 0 ->
                    ".";
                case 1 ->
                    "..";
                case 2 ->
                    "...";
                default ->
                    "";
            };
            typingLabel.setText(typingBaseText + dots);
        });
        typingDotsTimer.start();
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
        // System messages get special centered treatment
        if (messageType.equals("system")) {
            return createSystemMessage(content, timestamp);
        }

        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout(0, 0));
        messagePanel.setOpaque(false);
        messagePanel.setBorder(new EmptyBorder(2, 6, 2, 6));
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        // Determine colors based on message type
        Color senderColor = PRIMARY_COLOR;
        Color contentColor = TEXT_COLOR;
        String avatarEmoji = "👤";

        if (messageType.equals("own")) {
            senderColor = ACCENT_COLOR;
            avatarEmoji = "🔷";
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
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        headerPanel.setOpaque(false);

        // Avatar
        JLabel avatarLabel = new JLabel(avatarEmoji);
        avatarLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 22));
        avatarLabel.setPreferredSize(new Dimension(32, 32));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setVerticalAlignment(SwingConstants.TOP);
        headerPanel.add(avatarLabel);

        // Username
        JLabel usernameLabel = new JLabel(sender);
        usernameLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 15));
        usernameLabel.setForeground(senderColor);
        headerPanel.add(usernameLabel);

        // Timestamp
        JLabel timestampLabel = new JLabel(timestamp);
        timestampLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 11));
        timestampLabel.setForeground(new Color(TEXT_SECONDARY.getRed(), TEXT_SECONDARY.getGreen(), TEXT_SECONDARY.getBlue(), 180));
        headerPanel.add(timestampLabel);

        // Content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(0, 36, 0, 8)); // Indent to align with text after avatar

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
                messagePanel.setBackground(new Color(CARD_COLOR.getRed() + 6, CARD_COLOR.getGreen() + 6, CARD_COLOR.getBlue() + 6));
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

    // Create centered system message with distinct styling
    private JPanel createSystemMessage(String content, String timestamp) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setOpaque(false);
        messagePanel.setBorder(new EmptyBorder(1, 10, 1, 10));
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // Create a centered container
        JPanel centerContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        centerContainer.setOpaque(true);
        centerContainer.setBackground(new Color(SURFACE_COLOR.getRed() + 5, SURFACE_COLOR.getGreen() + 5, SURFACE_COLOR.getBlue() + 5));
        centerContainer.setBorder(new EmptyBorder(3, 8, 3, 8));

        // System icon
        JLabel iconLabel = new JLabel("ℹ️");
        iconLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 12));
        centerContainer.add(iconLabel);

        // Message text with italic styling
        JLabel messageLabel = new JLabel(content);
        messageLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.ITALIC, 12));
        messageLabel.setForeground(new Color(TEXT_SECONDARY.getRed() + 20, TEXT_SECONDARY.getGreen() + 20, TEXT_SECONDARY.getBlue() + 20));
        centerContainer.add(messageLabel);

        // Timestamp
        JLabel timestampLabel = new JLabel(timestamp);
        timestampLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 10));
        timestampLabel.setForeground(new Color(TEXT_SECONDARY.getRed(), TEXT_SECONDARY.getGreen(), TEXT_SECONDARY.getBlue(), 150));
        centerContainer.add(timestampLabel);

        centerContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        messagePanel.add(centerContainer);

        return messagePanel;
    }

    // Create compact message (when same user sends multiple messages)
    private JPanel createCompactMessage(String content, String messageType) {
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);
        messagePanel.setBorder(new EmptyBorder(0, 40, 0, 10)); // Tighter vertical spacing
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        Color contentColor = TEXT_COLOR;

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
                messagePanel.setBackground(new Color(CARD_COLOR.getRed() + 6, CARD_COLOR.getGreen() + 6, CARD_COLOR.getBlue() + 6));
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

    // Modern emoji picker with grid layout
    private void showEmojiPanel() {
        JDialog emojiDialog = new JDialog(this, "😊 Emoji Picker", true);
        emojiDialog.setSize(380, 420);
        emojiDialog.setLocationRelativeTo(this);
        emojiDialog.getContentPane().setBackground(SURFACE_COLOR);

        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBackground(SURFACE_COLOR);
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Category tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(SURFACE_COLOR);
        tabbedPane.setForeground(TEXT_COLOR);
        tabbedPane.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 14));

        // Emoji categories
        String[][] categories = {
            {"😀", "😃", "😄", "😁", "😅", "😂", "🤣", "😊", "😇", "🙂", "😉", "😍", "🥰", "😘", "😋", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩", "🥳", "😏"},
            {"👍", "👎", "👊", "✊", "🤛", "🤜", "🤝", "👏", "🙌", "👐", "🤲", "🤞", "✌️", "🤟", "🤘", "👌", "🤌", "👈", "👉", "👆", "👇", "☝️", "✋", "🤚"},
            {"❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "♥️", "💌", "💋", "🫶", "🥹"},
            {"🔥", "✨", "⭐", "🌟", "💫", "⚡", "💥", "💢", "💦", "💨", "🎉", "🎊", "🎈", "🎁", "🏆", "🥇", "🎯", "💯", "✅", "❌", "⚠️", "🚀", "💡", "🔔"}
        };
        String[] categoryNames = {"😀 Smileys", "👍 Gestures", "❤️ Hearts", "🔥 Symbols"};

        for (int i = 0; i < categories.length; i++) {
            JPanel emojiGrid = new JPanel(new GridLayout(0, 6, 4, 4));
            emojiGrid.setBackground(CARD_COLOR);
            emojiGrid.setBorder(new EmptyBorder(8, 8, 8, 8));

            for (String emoji : categories[i]) {
                JButton emojiBtn = new JButton(emoji);
                emojiBtn.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 22));
                emojiBtn.setBackground(CARD_COLOR);
                emojiBtn.setForeground(TEXT_COLOR);
                emojiBtn.setBorder(new EmptyBorder(6, 6, 6, 6));
                emojiBtn.setFocusPainted(false);
                emojiBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

                // Hover animation
                emojiBtn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        emojiBtn.setBackground(ModernUI.ThemeColors.CARD_HOVER);
                        emojiBtn.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 26));
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        emojiBtn.setBackground(CARD_COLOR);
                        emojiBtn.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 22));
                    }
                });

                emojiBtn.addActionListener(e -> {
                    messageField.setText(messageField.getText() + emoji);
                    emojiDialog.dispose();
                    messageField.requestFocus();
                });
                emojiGrid.add(emojiBtn);
            }

            JScrollPane scrollPane = new JScrollPane(emojiGrid);
            scrollPane.setBorder(null);
            scrollPane.getViewport().setBackground(CARD_COLOR);
            styleScrollBar(scrollPane);
            tabbedPane.addTab(categoryNames[i], scrollPane);
        }

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Recent emojis panel
        JPanel recentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        recentPanel.setBackground(SURFACE_COLOR);
        recentPanel.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(8, 4, 4, 4)
        ));

        JLabel recentLabel = new JLabel("Recent: ");
        recentLabel.setForeground(TEXT_SECONDARY);
        recentLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 11));
        recentPanel.add(recentLabel);

        String[] recent = {"😀", "❤️", "👍", "🔥", "😂"};
        for (String emoji : recent) {
            JButton btn = new JButton(emoji);
            btn.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 16));
            btn.setBackground(SURFACE_COLOR);
            btn.setBorder(new EmptyBorder(2, 4, 2, 4));
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                messageField.setText(messageField.getText() + emoji);
                emojiDialog.dispose();
                messageField.requestFocus();
            });
            recentPanel.add(btn);
        }

        mainPanel.add(recentPanel, BorderLayout.SOUTH);
        emojiDialog.add(mainPanel);
        emojiDialog.setVisible(true);
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

        // Ctrl+L to clear chat
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), "clearChat");
        getRootPane().getActionMap().put("clearChat", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearChat();
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

            setTypingIndicator(" ", false);

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

        if (!connected) {
            setTypingIndicator(" ", false);
        }

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
                        // Play sound for messages from others
                        if (!sender.equals(username)) {
                            playNotificationSound();
                        }
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
                        playNotificationSound(); // Always play for private messages
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
                            setTypingIndicator(who + " is typing", typing);
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

            AnimatedPanel animated = new AnimatedPanel(messageComponent);
            chatArea.add(animated);
            chatArea.revalidate();
            animated.playEntrance();

            // Auto-scroll to bottom with a smooth glide
            SwingUtilities.invokeLater(this::smoothScrollToBottom);
        });
    }

    private void smoothScrollToBottom() {
        JScrollBar vertical = chatScroll.getVerticalScrollBar();
        int start = vertical.getValue();
        int target = vertical.getMaximum();
        int distance = target - start;
        if (distance <= 0) {
            return;
        }

        final int steps = 10;
        final int[] step = {0};
        Timer scrollTimer = new Timer(12, e -> {
            step[0]++;
            float progress = step[0] / (float) steps;
            int value = start + Math.round(distance * progress);
            vertical.setValue(value);
            if (step[0] >= steps) {
                ((Timer) e.getSource()).stop();
            }
        });
        scrollTimer.start();
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
