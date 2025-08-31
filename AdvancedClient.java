import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;

// Custom Modern Components
class ModernButton extends JButton {
    private Color bgColor;
    private Color hoverColor;
    private Timer animationTimer;
    private float animationProgress = 0.0f;
    private boolean isHovering = false;

    public ModernButton(String text, Color bgColor) {
        super(text);
        this.bgColor = bgColor;
        this.hoverColor = bgColor.brighter();

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFont(new Font("Segoe UI", Font.BOLD, 12));
        setForeground(Color.WHITE);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(12, 24, 12, 24));

        animationTimer = new Timer(16, e -> {
            if (isHovering && animationProgress < 1.0f) {
                animationProgress = Math.min(1.0f, animationProgress + 0.1f);
                repaint();
            } else if (!isHovering && animationProgress > 0.0f) {
                animationProgress = Math.max(0.0f, animationProgress - 0.1f);
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovering = true;
                animationTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovering = false;
                animationTimer.start();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                animationProgress = 1.0f;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                animationProgress = isHovering ? 0.8f : 0.0f;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Interpolate colors
        Color currentColor = interpolateColor(bgColor, hoverColor, animationProgress);
        g2d.setColor(currentColor);

        // Draw rounded rectangle with shadow
        g2d.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 8, 8);

        // Add subtle glow effect when hovering
        if (animationProgress > 0) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, animationProgress * 0.3f));
            g2d.setColor(Color.WHITE);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        }

        g2d.dispose();
        super.paintComponent(g);
    }

    private Color interpolateColor(Color start, Color end, float factor) {
        int r = (int) (start.getRed() + factor * (end.getRed() - start.getRed()));
        int g = (int) (start.getGreen() + factor * (end.getGreen() - start.getGreen()));
        int b = (int) (start.getBlue() + factor * (end.getBlue() - start.getBlue()));
        return new Color(Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, b)));
    }
}

class ModernTextField extends JTextField {
    private Color focusColor;
    private Color borderColor;
    private boolean isFocused = false;
    private Timer animationTimer;
    private float animationProgress = 0.0f;

    public ModernTextField(String text, int columns, Color focusColor, Color borderColor) {
        super(text, columns);
        this.focusColor = focusColor;
        this.borderColor = borderColor;

        setFont(new Font("Segoe UI", Font.PLAIN, 12));
        setBackground(new Color(48, 48, 48));
        setForeground(new Color(240, 240, 240));
        setCaretColor(focusColor);
        setBorder(new EmptyBorder(8, 12, 8, 12));

        animationTimer = new Timer(16, e -> {
            if (isFocused && animationProgress < 1.0f) {
                animationProgress = Math.min(1.0f, animationProgress + 0.1f);
                repaint();
            } else if (!isFocused && animationProgress > 0.0f) {
                animationProgress = Math.max(0.0f, animationProgress - 0.1f);
                repaint();
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                animationTimer.start();
            }

            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                animationTimer.start();
            }
        });
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color currentColor = interpolateColor(borderColor, focusColor, animationProgress);
        g2d.setColor(currentColor);
        g2d.setStroke(new BasicStroke(1 + animationProgress * 2));
        g2d.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 6, 6);

        g2d.dispose();
    }

    private Color interpolateColor(Color start, Color end, float factor) {
        int r = (int) (start.getRed() + factor * (end.getRed() - start.getRed()));
        int g = (int) (start.getGreen() + factor * (end.getGreen() - start.getGreen()));
        int b = (int) (start.getBlue() + factor * (end.getBlue() - start.getBlue()));
        return new Color(Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, b)));
    }
}

class ModernPanel extends JPanel {
    private Color backgroundColor;
    private int cornerRadius;
    private boolean hasShadow;

    public ModernPanel(LayoutManager layout, Color backgroundColor, int cornerRadius, boolean hasShadow) {
        super(layout);
        this.backgroundColor = backgroundColor;
        this.cornerRadius = cornerRadius;
        this.hasShadow = hasShadow;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (hasShadow) {
            // Draw shadow
            g2d.setColor(new Color(0, 0, 0, 30));
            g2d.fillRoundRect(4, 4, getWidth() - 4, getHeight() - 4, cornerRadius, cornerRadius);
        }

        // Draw main background
        g2d.setColor(backgroundColor);
        g2d.fillRoundRect(0, 0, getWidth() - (hasShadow ? 4 : 0),
                getHeight() - (hasShadow ? 4 : 0), cornerRadius, cornerRadius);

        g2d.dispose();
        super.paintComponent(g);
    }
}

// Modern UI Components - Continued
class AnimatedLabel extends JLabel {
    private Timer pulseTimer;
    private float pulseOpacity = 1.0f;
    private boolean isPulsing = false;

    public AnimatedLabel(String text) {
        super(text);
        pulseTimer = new Timer(50, e -> {
            if (isPulsing) {
                pulseOpacity += 0.1f;
                if (pulseOpacity >= 1.0f) {
                    pulseOpacity = 0.7f;
                }
                repaint();
            }
        });
    }

    public void startPulse() {
        isPulsing = true;
        pulseTimer.start();
    }

    public void stopPulse() {
        isPulsing = false;
        pulseTimer.stop();
        pulseOpacity = 1.0f;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        if (isPulsing) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulseOpacity));
        }
        super.paintComponent(g2d);
        g2d.dispose();
    }
}

class ModernTitledBorder extends AbstractBorder {
    private String title;
    private Color color;
    private Font font;

    public ModernTitledBorder(String title, Color color) {
        this.title = title;
        this.color = color;
        this.font = new Font("Segoe UI", Font.BOLD, 12);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw border
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(x + 1, y + 10, width - 2, height - 12, 8, 8);

        // Draw title background
        FontMetrics fm = g2d.getFontMetrics(font);
        int titleWidth = fm.stringWidth(title) + 16;
        g2d.setColor(new Color(28, 28, 28));
        g2d.fillRoundRect(x + 15, y, titleWidth, 20, 10, 10);

        // Draw title
        g2d.setColor(color);
        g2d.setFont(font);
        g2d.drawString(title, x + 23, y + 14);

        g2d.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(20, 10, 10, 10);
    }
}

class ModernRoundedBorder extends AbstractBorder {
    private Color color;
    private int radius;

    public ModernRoundedBorder(Color color, int radius) {
        this.color = color;
        this.radius = radius;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);
        g2d.fillRoundRect(x, y, width, height, radius, radius);
        g2d.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(2, 2, 2, 2);
    }
}

class ModernListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        setBorder(new EmptyBorder(8, 12, 8, 12));
        setFont(new Font("Segoe UI", Font.PLAIN, 12));

        if (isSelected) {
            setBackground(new Color(88, 86, 214, 150));
            setForeground(Color.WHITE);
        } else {
            setBackground(new Color(38, 38, 38));
            setForeground(new Color(240, 240, 240));
        }

        // Add online indicator
        setText("🟢 " + value.toString());

        return this;
    }
}

class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
    @Override
    protected void configureScrollBarColors() {
        thumbColor = new Color(88, 86, 214, 100);
        thumbDarkShadowColor = new Color(88, 86, 214, 150);
        thumbHighlightColor = new Color(88, 86, 214, 200);
        thumbLightShadowColor = new Color(88, 86, 214, 50);
        trackColor = new Color(28, 28, 28);
        trackHighlightColor = new Color(38, 38, 38);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    private JButton createZeroButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(thumbColor);
        g2d.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                thumbBounds.width - 4, thumbBounds.height - 4, 6, 6);
        g2d.dispose();
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(trackColor);
        g2d.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        g2d.dispose();
    }
}

// Modern UI Components - Continued

public class AdvancedClient extends JFrame {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private boolean isConnected = false;

    // Modern UI Components
    private JTextPane chatArea;
    private ModernTextField messageField;
    private ModernButton connectButton, sendButton, emojiButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private ModernPanel connectionPanel, chatPanel, inputPanel;
    private AnimatedLabel statusLabel, userCountLabel;
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

    private void initializeModernGUI() {
        setTitle("💬 Elite Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 650));

        // Enable modern window decorations
        try {
            setUndecorated(true);
            getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
        } catch (Exception e) {
            // Use default decorations
        }

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

        // Left side - Connection controls with glass morphism effect
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        leftPanel.setOpaque(false);

        // Create modern text fields
        serverField = new ModernTextField(serverAddress, 12, PRIMARY_COLOR, BORDER_COLOR);
        portField = new ModernTextField(String.valueOf(serverPort), 6, PRIMARY_COLOR, BORDER_COLOR);
        usernameField = new ModernTextField(username, 12, PRIMARY_COLOR, BORDER_COLOR);

        // Add labels with glow effect
        leftPanel.add(createGlowLabel("🌐 Server:", TEXT_COLOR));
        leftPanel.add(serverField);
        leftPanel.add(createGlowLabel("🔌 Port:", TEXT_COLOR));
        leftPanel.add(portField);
        leftPanel.add(createGlowLabel("👤 Username:", TEXT_COLOR));
        leftPanel.add(usernameField);

        // Modern connect button with pulse animation
        connectButton = new ModernButton("� Connect", SUCCESS_COLOR);
        connectButton.addActionListener(e -> connectToServer());
        leftPanel.add(connectButton);

        // Right side - Status with animated indicators
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        rightPanel.setOpaque(false);

        statusLabel = new AnimatedLabel("🔴 Disconnected");
        statusLabel.setForeground(TEXT_COLOR);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        userCountLabel = new AnimatedLabel("👥 0 online");
        userCountLabel.setForeground(TEXT_SECONDARY);
        userCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        rightPanel.add(userCountLabel);
        rightPanel.add(statusLabel);

        connectionPanel.add(leftPanel, BorderLayout.WEST);
        connectionPanel.add(rightPanel, BorderLayout.EAST);
    }

    private void createModernChatPanel() {
        chatPanel = new ModernPanel(new BorderLayout(15, 0), BACKGROUND_COLOR, 0, false);
        chatPanel.setBorder(new EmptyBorder(20, 30, 10, 30));

        // Modern chat area with glassmorphism
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setBackground(CARD_COLOR);
        chatArea.setForeground(TEXT_COLOR);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chatArea.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Custom document styling
        StyledDocument doc = chatArea.getStyledDocument();
        addAdvancedStylesToDocument(doc);

        // Modern scroll pane with custom styling
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(new ModernTitledBorder("💬 Chat Messages", PRIMARY_COLOR));
        chatScroll.setBackground(CARD_COLOR);
        chatScroll.getViewport().setBackground(CARD_COLOR);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Style scrollbar
        styleScrollBar(chatScroll);

        // Modern user list with hover effects
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        userList.setBackground(CARD_COLOR);
        userList.setForeground(TEXT_COLOR);
        userList.setSelectionBackground(new Color(88, 86, 214, 100));
        userList.setSelectionForeground(Color.WHITE);
        userList.setBorder(new EmptyBorder(15, 15, 15, 15));
        userList.setCellRenderer(new ModernListCellRenderer());

        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBorder(new ModernTitledBorder("👥 Online Users (0)", ACCENT_COLOR));
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
        messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageField.setEnabled(false);
        messageField.setBorder(new EmptyBorder(12, 15, 12, 15));

        // Modern send button with glow effect
        sendButton = new ModernButton("� Send", PRIMARY_COLOR);
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> sendMessage());

        // Emoji button for future enhancement
        emojiButton = new ModernButton("😊", ACCENT_COLOR);
        emojiButton.setEnabled(false);
        emojiButton.addActionListener(e -> showEmojiPanel());

        // Enter key to send message
        messageField.addActionListener(e -> sendMessage());

        // Advanced typing indicator with animation
        setupAdvancedTypingIndicator();

        // Create input layout
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);
        messagePanel.setBorder(new ModernRoundedBorder(CARD_COLOR, 12));
        messagePanel.add(messageField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(emojiButton);
        buttonPanel.add(sendButton);

        inputContainer.add(messagePanel, BorderLayout.CENTER);
        inputContainer.add(buttonPanel, BorderLayout.EAST);

        inputPanel.add(inputContainer, BorderLayout.CENTER);

        // Typing indicator with pulse animation
        typingLabel = new JLabel(" ");
        typingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
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
            private final javax.swing.Timer typingTimer = new javax.swing.Timer(2000, evt -> sendTyping(false));
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
                statusLabel.stopPulse();
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                if (isConnected) {
                    statusLabel.startPulse();
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

    // Custom ScrollBar UI
    class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(88, 86, 214, 100);
            thumbDarkShadowColor = new Color(88, 86, 214, 150);
            thumbHighlightColor = new Color(88, 86, 214, 200);
            thumbLightShadowColor = new Color(88, 86, 214, 50);
            trackColor = new Color(28, 28, 28);
            trackHighlightColor = new Color(38, 38, 38);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(thumbColor);
            g2d.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                    thumbBounds.width - 4, thumbBounds.height - 4, 6, 6);
            g2d.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(trackColor);
            g2d.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            g2d.dispose();
        }
    }

    // Add advanced document styling
    private void addAdvancedStylesToDocument(StyledDocument doc) {
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontFamily(regular, "Segoe UI");
        StyleConstants.setFontSize(regular, 13);
        StyleConstants.setForeground(regular, TEXT_COLOR);

        Style system = doc.addStyle("system", regular);
        StyleConstants.setForeground(system, TEXT_SECONDARY);
        StyleConstants.setItalic(system, true);

        Style user = doc.addStyle("user", regular);
        StyleConstants.setForeground(user, PRIMARY_COLOR);
        StyleConstants.setBold(user, true);

        Style ownMessage = doc.addStyle("own", regular);
        StyleConstants.setForeground(ownMessage, ACCENT_COLOR);
        StyleConstants.setBold(ownMessage, true);

        Style timestamp = doc.addStyle("timestamp", regular);
        StyleConstants.setForeground(timestamp, TEXT_SECONDARY);
        StyleConstants.setFontSize(timestamp, 11);
    }

    // Animation for user selection
    private void animateSelection() {
        Timer selectionTimer = new Timer(100, null);
        final int[] pulse = { 0 };

        selectionTimer.addActionListener(e -> {
            pulse[0]++;
            if (pulse[0] > 3) {
                ((Timer) e.getSource()).stop();
            }
            userList.repaint();
        });
        selectionTimer.start();
    }

    // Show emoji panel (placeholder for future enhancement)
    private void showEmojiPanel() {
        String[] emojis = { "😊", "😂", "❤️", "👍", "🎉", "😎", "🔥", "💯" };
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
        // Start message reader thread
        Thread messageReader = new Thread(() -> {
            while (true) {
                try {
                    synchronized (this) {
                        while (!isConnected && input == null) {
                            wait(100); // Wait for connection
                        }
                    }

                    if (isConnected && input != null) {
                        String message = input.readLine();
                        if (message != null) {
                            handleIncomingMessage(message);
                        } else {
                            // Connection lost
                            SwingUtilities.invokeLater(() -> {
                                appendToChat("Connection lost to server\n", "system");
                                disconnectFromServer();
                            });
                            break;
                        }
                    }
                } catch (IOException e) {
                    if (isConnected) {
                        SwingUtilities.invokeLater(() -> {
                            appendToChat("Error reading from server: " + e.getMessage() + "\n", "system");
                            disconnectFromServer();
                        });
                    }
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        messageReader.setDaemon(true);
        messageReader.start();
    }

    private void connectToServer() {
        if (isConnected)
            return;

        try {
            // Animate connection process
            connectButton.setEnabled(false);
            statusLabel.setText("🔄 Connecting...");
            statusLabel.startPulse();

            serverAddress = serverField.getText();
            serverPort = Integer.parseInt(portField.getText());
            username = usernameField.getText();

            socket = new Socket(serverAddress, serverPort);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            isConnected = true;

            // Send username first
            output.println("USERNAME|" + username);

            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("🟢 Connected as " + username);
                statusLabel.stopPulse();
                setTitle("💬 Elite Chat Client - " + username);
                appendToChat("🎉 Connected to server as " + username + "\n", "system");
                updateConnectionStatus();

                // Celebration animation
                Timer celebrationTimer = new Timer(100, null);
                final int[] count = { 0 };
                celebrationTimer.addActionListener(e -> {
                    count[0]++;
                    if (count[0] > 5) {
                        ((Timer) e.getSource()).stop();
                    }
                    statusLabel.setText(count[0] % 2 == 0 ? "🟢 Connected! ✨" : "🟢 Connected as " + username);
                });
                celebrationTimer.start();
            });

        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("🔴 Connection failed");
                statusLabel.stopPulse();
                connectButton.setEnabled(true);

                // Modern error dialog
                JOptionPane.showMessageDialog(this,
                        "Failed to connect to server:\n" + e.getMessage(),
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
            });
        } catch (NumberFormatException e) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("🔴 Invalid port");
                statusLabel.stopPulse();
                connectButton.setEnabled(true);

                JOptionPane.showMessageDialog(this,
                        "Please enter a valid port number",
                        "Input Error",
                        JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private void disconnectFromServer() {
        if (!isConnected)
            return;

        isConnected = false;
        try {
            if (output != null) {
                output.println("/quit");
                output.close();
            }
            if (input != null)
                input.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            // Ignore
        }

        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("🔴 Disconnected");
            statusLabel.stopPulse();
            appendToChat("👋 You left the chat\n", "system");
            userListModel.clear();
            userCountLabel.setText("👥 0 online");
            updateConnectionStatus();

            // Reset title
            setTitle("💬 Elite Chat Client");
        });
    }

    private void updateConnectionStatus() {
        boolean connected = isConnected;
        connectButton.setEnabled(!connected);
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
        if (!isConnected || messageField.getText().trim().isEmpty())
            return;

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
                String[] parts = message.split("\\|", 4);
                if (parts.length < 3) {
                    // Fallback for old format
                    appendToChat("Server: " + message + "\n", "regular");
                    return;
                }

                String messageType = parts[0];
                String timestamp = parts[1];
                String content = parts.length > 3 ? parts[3] : parts[2];

                switch (messageType) {
                    case "CHAT" -> {
                        String sender = parts[2];
                        appendToChat("[" + timestamp + "] " + sender + ": " + content + "\n", "user");
                    }
                    case "PRIVATE" -> {
                        String sender = parts[2];
                        // For PRIVATE we expect format PRIVATE|ts|from|to|content
                        String[] privParts = message.split("\\|", 5);
                        String privContent = privParts.length == 5 ? privParts[4] : content;
                        appendToChat("[" + timestamp + "] [PM] " + sender + ": " + privContent + "\n", "own");
                    }
                    case "JOIN" -> appendToChat("[" + timestamp + "] *** " + content + " joined ***\n", "system");
                    case "LEAVE" -> appendToChat("[" + timestamp + "] *** " + content + " left ***\n", "system");
                    case "SYSTEM" -> appendToChat("[" + timestamp + "] * " + content + "\n", "system");
                    case "USERLIST" -> updateUserList(content);
                    case "TYPING" -> {
                        // content expected username|true/false
                        String[] tp = parts[2].split("\\|");
                        if (tp.length == 2) {
                            String who = tp[0];
                            boolean isTyping = Boolean.parseBoolean(tp[1]);
                            if (!who.equals(username)) {
                                typingLabel.setText(isTyping ? (who + " is typing…") : " ");
                            }
                        }
                    }
                    case "PING" -> {
                        // reply PONG immediately
                        if (output != null)
                            output.println("PONG");
                    }
                    default -> appendToChat("[" + timestamp + "] " + message + "\n", "regular");
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
        userCountLabel.setText("👥 " + userListModel.size() + " online");

        // Update the user list title with count
        JScrollPane userScroll = (JScrollPane) chatPanel.getComponent(1);
        if (userScroll.getBorder() instanceof TitledBorder border) {
            border.setTitle("👥 Online Users (" + userListModel.size() + ")");
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