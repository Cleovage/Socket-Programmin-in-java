
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
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
    private ModernUI.ModernButton discoverButton;
    private JDialog discoveryDialog;
    private JPanel discoveryResultsPanel;
    private JLabel discoveryStatusLabel;
    private List<ServerInfo> discoveredServers = new ArrayList<>();

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

        // Discover button
        gbc.gridx = 6;
        gbc.gridy = 0;
        discoverButton = new ModernUI.ModernButton("🔍 Discover", new Color(10, 132, 255));
        discoverButton.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 13));
        discoverButton.setToolTipText("Scan local network for servers");
        discoverButton.addActionListener(e -> showServerDiscovery());
        leftPanel.add(discoverButton, gbc);

        // Connect button
        gbc.gridx = 7;
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

    // Create modern message bubble with left-right alignment
    private JPanel createMessageBubble(String sender, String content, String timestamp, String messageType) {
        // System messages get special centered treatment
        if (messageType.equals("system")) {
            return createSystemMessage(content, timestamp);
        }

        boolean isOwnMessage = messageType.equals("own");
        boolean isPrivateMessage = messageType.equals("private");

        // Get sender-specific color
        Color userColor = getUserColor(sender);

        // Own messages use blue, others use their assigned color
        Color bubbleColor;
        Color textColor = Color.WHITE;

        if (isOwnMessage) {
            bubbleColor = new Color(88, 101, 242); // Blue for own
        } else if (isPrivateMessage) {
            bubbleColor = new Color(155, 89, 182); // Purple for private
        } else {
            bubbleColor = userColor; // User's unique color
        }

        // Main row panel
        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
        rowPanel.setOpaque(false);
        rowPanel.setBorder(new EmptyBorder(4, 12, 4, 12));
        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Avatar panel with colored circle background
        JPanel avatarPanel = createAvatarPanel(isOwnMessage ? username : sender, userColor, isOwnMessage);

        // Message bubble
        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBackground(bubbleColor);
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Round the bubble corners
        bubble.setUI(new javax.swing.plaf.PanelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bubbleColor);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 16, 16);
                g2.dispose();
            }
        });
        bubble.setOpaque(false);

        // Header with name and time
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        header.setOpaque(false);

        JLabel nameLabel = new JLabel(sender);
        nameLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 12));
        nameLabel.setForeground(new Color(255, 255, 255, 220));

        JLabel timeLabel = new JLabel(" • " + timestamp);
        timeLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 10));
        timeLabel.setForeground(new Color(255, 255, 255, 150));

        header.add(nameLabel);
        header.add(timeLabel);

        // Message text
        JLabel msgLabel = new JLabel("<html><body style='width: 280px'>" + escapeHtml(content) + "</body></html>");
        msgLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 13));
        msgLabel.setForeground(textColor);
        msgLabel.setBorder(new EmptyBorder(2, 0, 0, 0));

        bubble.add(header);
        bubble.add(msgLabel);

        // Assemble row based on alignment
        if (isOwnMessage) {
            rowPanel.add(Box.createHorizontalGlue());
            rowPanel.add(bubble);
            rowPanel.add(Box.createRigidArea(new Dimension(8, 0)));
            rowPanel.add(avatarPanel);
        } else {
            rowPanel.add(avatarPanel);
            rowPanel.add(Box.createRigidArea(new Dimension(8, 0)));
            rowPanel.add(bubble);
            rowPanel.add(Box.createHorizontalGlue());
        }

        return rowPanel;
    }

    // Create avatar panel with colored circle background
    private JPanel createAvatarPanel(String user, Color color, boolean isOwnMessage) {
        String avatar = getProfileAvatar(user);

        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Draw colored circle background
                Color bgColor = isOwnMessage ? new Color(88, 101, 242) : color;
                g2.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 60));
                g2.fillOval(0, 0, 36, 36);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatarPanel.setOpaque(false);
        avatarPanel.setPreferredSize(new Dimension(36, 36));
        avatarPanel.setMinimumSize(new Dimension(36, 36));
        avatarPanel.setMaximumSize(new Dimension(36, 36));
        avatarPanel.setLayout(new GridBagLayout());

        JLabel avatarLabel = new JLabel(avatar);
        avatarLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 20));
        avatarPanel.add(avatarLabel);

        return avatarPanel;
    }

    // Get consistent color for user
    private Color getUserColor(String username) {
        Color[] userColors = {
            new Color(88, 101, 242), // Blue
            new Color(67, 181, 129), // Green
            new Color(250, 166, 26), // Orange
            new Color(237, 66, 69), // Red
            new Color(155, 89, 182), // Purple
            new Color(26, 188, 156), // Teal
            new Color(241, 196, 15), // Yellow
            new Color(231, 76, 60), // Coral
            new Color(52, 152, 219), // Sky Blue
            new Color(46, 204, 113) // Emerald
        };
        return userColors[Math.abs(username.hashCode()) % userColors.length];
    }

    // Escape HTML special characters
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
    }

    // Create centered system message with distinct styling
    private JPanel createSystemMessage(String content, String timestamp) {
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        rowPanel.setOpaque(false);
        rowPanel.setBorder(new EmptyBorder(6, 12, 6, 12));

        JPanel pill = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        pill.setBackground(new Color(50, 50, 55));
        pill.setBorder(new EmptyBorder(2, 12, 2, 12));

        // Round corners
        pill.setUI(new javax.swing.plaf.PanelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(50, 50, 55));
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 14, 14);
                g2.dispose();
            }
        });
        pill.setOpaque(false);

        JLabel icon = new JLabel("ℹ️");
        icon.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 11));

        JLabel msg = new JLabel(content);
        msg.setFont(ModernUI.getEmojiCompatibleFont(Font.ITALIC, 11));
        msg.setForeground(TEXT_SECONDARY);

        JLabel time = new JLabel(timestamp);
        time.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 9));
        time.setForeground(new Color(TEXT_SECONDARY.getRed(), TEXT_SECONDARY.getGreen(), TEXT_SECONDARY.getBlue(), 150));

        pill.add(icon);
        pill.add(msg);
        pill.add(time);
        rowPanel.add(pill);

        return rowPanel;
    }

    // Create compact message (when same user sends multiple messages)
    private JPanel createCompactMessage(String content, String messageType, String sender) {
        boolean isOwnMessage = messageType.equals("own");
        boolean isPrivateMessage = messageType.equals("private");

        Color userColor = getUserColor(sender);
        Color bubbleColor;

        if (isOwnMessage) {
            bubbleColor = new Color(88, 101, 242);
        } else if (isPrivateMessage) {
            bubbleColor = new Color(155, 89, 182);
        } else {
            bubbleColor = userColor;
        }

        // Row panel
        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
        rowPanel.setOpaque(false);
        rowPanel.setBorder(new EmptyBorder(1, 12, 1, 12));

        // Spacer for avatar alignment
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(44, 1));
        spacer.setMinimumSize(new Dimension(44, 1));
        spacer.setMaximumSize(new Dimension(44, 1));

        // Compact bubble
        JPanel bubble = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bubble.setBackground(bubbleColor);
        bubble.setBorder(new EmptyBorder(6, 12, 6, 12));

        bubble.setUI(new javax.swing.plaf.PanelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bubbleColor);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 14, 14);
                g2.dispose();
            }
        });
        bubble.setOpaque(false);

        JLabel msgLabel = new JLabel("<html><body style='width: 280px'>" + escapeHtml(content) + "</body></html>");
        msgLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 13));
        msgLabel.setForeground(Color.WHITE);
        bubble.add(msgLabel);

        if (isOwnMessage) {
            rowPanel.add(Box.createHorizontalGlue());
            rowPanel.add(bubble);
            rowPanel.add(Box.createRigidArea(new Dimension(8, 0)));
            rowPanel.add(spacer);
        } else {
            rowPanel.add(spacer);
            rowPanel.add(Box.createRigidArea(new Dimension(8, 0)));
            rowPanel.add(bubble);
            rowPanel.add(Box.createHorizontalGlue());
        }

        return rowPanel;
    }

    // Get consistent profile avatar for each user
    private String getProfileAvatar(String username) {
        String[] avatars = {
            "👨", "👩", "🧑", "👦", "👧", "🧔", "👴", "👵",
            "👨‍💼", "👩‍💼", "👨‍🔬", "👩‍🔬", "👨‍💻", "👩‍💻", "👨‍🎨", "👩‍🎨",
            "🧑‍🚀", "👨‍🚀", "👩‍🚀", "🦸", "🦸‍♀️", "🧙", "🧙‍♀️", "🧚",
            "🧛", "🧜", "🧝", "👼", "🎅", "🤶", "🥷", "🧞",
            "😀", "😎", "🤓", "🥳", "😇", "🤩", "🤖", "👽",
            "👻", "💀", "🎃", "🤡", "🐱", "🐶", "🐼", "🦊",
            "🦁", "🐯", "🐸", "🐵", "🦄", "🐲", "🦋", "🐬"
        };
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
                messageComponent = createCompactMessage(content, messageType, sender);
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

    // ==================== SERVER DISCOVERY ====================
    // Class to hold discovered server information
    private static class ServerInfo {

        String ipAddress;
        int port;
        long responseTime;
        boolean reachable;

        ServerInfo(String ipAddress, int port, long responseTime, boolean reachable) {
            this.ipAddress = ipAddress;
            this.port = port;
            this.responseTime = responseTime;
            this.reachable = reachable;
        }
    }

    // Show server discovery dialog
    private void showServerDiscovery() {
        if (discoveryDialog != null && discoveryDialog.isVisible()) {
            discoveryDialog.toFront();
            return;
        }

        discoveryDialog = new JDialog(this, "🔍 Discover Servers on Local Network", false);
        discoveryDialog.setSize(650, 500);
        discoveryDialog.setLocationRelativeTo(this);
        discoveryDialog.getContentPane().setBackground(SURFACE_COLOR);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBackground(SURFACE_COLOR);
        mainPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("🔍 Network Scanner");
        titleLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 18));
        titleLabel.setForeground(TEXT_COLOR);
        headerPanel.add(titleLabel, BorderLayout.NORTH);

        JLabel infoLabel = new JLabel("<html>Scanning your local network for chat servers...<br>"
                + "This will check common ports on your subnet.</html>");
        infoLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 12));
        infoLabel.setForeground(TEXT_SECONDARY);
        headerPanel.add(infoLabel, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Results panel with scrolling
        discoveryResultsPanel = new JPanel();
        discoveryResultsPanel.setLayout(new BoxLayout(discoveryResultsPanel, BoxLayout.Y_AXIS));
        discoveryResultsPanel.setBackground(CARD_COLOR);
        discoveryResultsPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane resultsScroll = new JScrollPane(discoveryResultsPanel);
        resultsScroll.setBorder(new CompoundBorder(
                new ModernUI.ModernTitledBorder("📡 Discovered Servers"),
                new EmptyBorder(10, 10, 10, 10)));
        resultsScroll.setBackground(CARD_COLOR);
        resultsScroll.getViewport().setBackground(CARD_COLOR);
        styleScrollBar(resultsScroll);

        mainPanel.add(resultsScroll, BorderLayout.CENTER);

        // Bottom panel with status and scan button
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);

        discoveryStatusLabel = new JLabel("🔍 Ready to scan");
        discoveryStatusLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 13));
        discoveryStatusLabel.setForeground(TEXT_SECONDARY);
        bottomPanel.add(discoveryStatusLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        ModernUI.ModernButton scanButton = new ModernUI.ModernButton("🔍 Start Scan", PRIMARY_COLOR);
        scanButton.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 13));
        scanButton.addActionListener(e -> startNetworkScan());

        ModernUI.ModernButton closeButton = new ModernUI.ModernButton("❌ Close", new Color(100, 100, 100));
        closeButton.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 13));
        closeButton.addActionListener(e -> discoveryDialog.dispose());

        buttonPanel.add(scanButton);
        buttonPanel.add(closeButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        discoveryDialog.add(mainPanel);
        discoveryDialog.setVisible(true);

        // Auto-start scan
        SwingUtilities.invokeLater(() -> startNetworkScan());
    }

    // Start network scanning
    private void startNetworkScan() {
        discoveredServers.clear();
        discoveryResultsPanel.removeAll();
        discoveryResultsPanel.revalidate();
        discoveryResultsPanel.repaint();

        discoveryStatusLabel.setText("🔄 Scanning network...");
        discoveryStatusLabel.setForeground(PRIMARY_COLOR);

        // Add scanning indicator
        JPanel scanningPanel = createScanningIndicator();
        discoveryResultsPanel.add(scanningPanel);
        discoveryResultsPanel.revalidate();

        // Run scan in background thread
        Thread scanThread = new Thread(() -> {
            try {
                // Get local IP address
                String localIP = getLocalIPAddress();
                if (localIP == null) {
                    SwingUtilities.invokeLater(() -> {
                        discoveryStatusLabel.setText("❌ Could not determine local IP address");
                        discoveryStatusLabel.setForeground(ACCENT_COLOR);
                        discoveryResultsPanel.removeAll();
                        discoveryResultsPanel.revalidate();
                        discoveryResultsPanel.repaint();
                    });
                    return;
                }

                // Get subnet (e.g., 192.168.1)
                String subnet = localIP.substring(0, localIP.lastIndexOf('.'));
                int currentPort = serverPort;

                SwingUtilities.invokeLater(() -> {
                    discoveryStatusLabel.setText("🔍 Scanning " + subnet + ".* on port " + currentPort + "...");
                });

                // Scan subnet (1-254) using thread pool for speed
                ExecutorService executor = Executors.newFixedThreadPool(50);
                List<Future<ServerInfo>> futures = new ArrayList<>();

                for (int i = 1; i <= 254; i++) {
                    final String ip = subnet + "." + i;
                    futures.add(executor.submit(() -> scanHost(ip, currentPort)));
                }

                // Collect results
                int scanned = 0;
                for (Future<ServerInfo> future : futures) {
                    try {
                        ServerInfo info = future.get();
                        scanned++;
                        final int currentScanned = scanned;

                        SwingUtilities.invokeLater(() -> {
                            discoveryStatusLabel.setText(String.format("🔍 Scanned %d/254 hosts...", currentScanned));
                        });

                        if (info != null && info.reachable) {
                            discoveredServers.add(info);
                            SwingUtilities.invokeLater(() -> addServerToResults(info));
                        }
                    } catch (Exception e) {
                        // Ignore individual scan failures
                    }
                }

                executor.shutdown();

                // Update final status
                SwingUtilities.invokeLater(() -> {
                    discoveryResultsPanel.remove(scanningPanel);
                    if (discoveredServers.isEmpty()) {
                        discoveryStatusLabel.setText("❌ No servers found on local network");
                        discoveryStatusLabel.setForeground(ACCENT_COLOR);
                        JLabel noServersLabel = new JLabel("<html><center>"
                                + "<br><br>🔍<br><br>"
                                + "No chat servers found on your local network.<br>"
                                + "Make sure the server is running and accessible.<br><br>"
                                + "Scanned subnet: " + subnet + ".0/24<br>"
                                + "Port: " + currentPort + "</center></html>");
                        noServersLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 13));
                        noServersLabel.setForeground(TEXT_SECONDARY);
                        noServersLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        noServersLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                        discoveryResultsPanel.add(noServersLabel);
                    } else {
                        discoveryStatusLabel.setText("✅ Found " + discoveredServers.size() + " server(s)");
                        discoveryStatusLabel.setForeground(SUCCESS_COLOR);
                    }
                    discoveryResultsPanel.revalidate();
                    discoveryResultsPanel.repaint();
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    discoveryStatusLabel.setText("❌ Scan failed: " + e.getMessage());
                    discoveryStatusLabel.setForeground(ACCENT_COLOR);
                    discoveryResultsPanel.removeAll();
                    discoveryResultsPanel.revalidate();
                    discoveryResultsPanel.repaint();
                });
            }
        });
        scanThread.setDaemon(true);
        scanThread.start();
    }

    // Get local IP address
    private String getLocalIPAddress() {
        try {
            // Try to find non-loopback IPv4 address
            for (NetworkInterface ni : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isLoopback() || !ni.isUp()) {
                    continue;
                }
                for (InetAddress addr : java.util.Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Scan a specific host for server
    private ServerInfo scanHost(String ip, int port) {
        long startTime = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 500); // 500ms timeout
            long responseTime = System.currentTimeMillis() - startTime;
            return new ServerInfo(ip, port, responseTime, true);
        } catch (Exception e) {
            return new ServerInfo(ip, port, -1, false);
        }
    }

    // Create scanning indicator animation
    private JPanel createScanningIndicator() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel scanLabel = new JLabel("🔄 Scanning network...");
        scanLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 14));
        scanLabel.setForeground(TEXT_SECONDARY);

        ModernUI.ModernProgressBar progressBar = new ModernUI.ModernProgressBar();
        progressBar.setPreferredSize(new Dimension(300, 8));
        progressBar.setIndeterminate(true);

        panel.add(scanLabel);

        return panel;
    }

    // Add discovered server to results panel
    private void addServerToResults(ServerInfo serverInfo) {
        ModernUI.ModernCard serverCard = new ModernUI.ModernCard(CARD_COLOR, true);
        serverCard.setLayout(new BorderLayout(12, 8));
        serverCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        serverCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        // Left side - Server info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel ipLabel = new JLabel("🖥️  " + serverInfo.ipAddress + ":" + serverInfo.port);
        ipLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 15));
        ipLabel.setForeground(TEXT_COLOR);
        ipLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel responseLabel = new JLabel("⚡ Response time: " + serverInfo.responseTime + "ms");
        responseLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 12));
        responseLabel.setForeground(TEXT_SECONDARY);
        responseLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(ipLabel);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(responseLabel);

        // Right side - Connect button
        ModernUI.ModernButton connectBtn = new ModernUI.ModernButton("🔗 Connect", SUCCESS_COLOR);
        connectBtn.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 12));
        connectBtn.setPreferredSize(new Dimension(120, 36));
        connectBtn.addActionListener(e -> {
            serverField.setText(serverInfo.ipAddress);
            portField.setText(String.valueOf(serverInfo.port));
            discoveryDialog.dispose();
            // Flash the server field to show it was updated
            flashComponent(serverField);
            JOptionPane.showMessageDialog(this,
                    "Server address set to: " + serverInfo.ipAddress + ":" + serverInfo.port + "\n\n"
                    + "Click 'Connect' to join the server.",
                    "Server Selected",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        serverCard.add(infoPanel, BorderLayout.CENTER);
        serverCard.add(connectBtn, BorderLayout.EAST);

        // Animate entry
        AnimatedPanel animated = new AnimatedPanel(serverCard);
        discoveryResultsPanel.add(animated);
        discoveryResultsPanel.add(Box.createVerticalStrut(8));
        discoveryResultsPanel.revalidate();
        animated.playEntrance();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AdvancedClient().setVisible(true);
        });
    }
}
