
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import javax.swing.*;
import javax.swing.border.*;

public class Client extends JFrame {

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private volatile boolean isConnected = false;

    // UI Components
    private JPanel mainPanel;
    private JPanel chatArea;
    private JScrollPane chatScroll;
    private ModernUI.ModernTextField messageField;
    private ModernUI.ModernButton sendButton;
    private JLabel statusLabel;
    private ModernUI.ModernTextField serverField, portField, usernameField;
    private ModernUI.ModernButton connectButton;
    private ModernUI.ModernButton closeButton, minimizeButton;

    // Translucent Colors
    private final Color TRANSLUCENT_BG = new Color(18, 18, 18, 220);
    private final Color TRANSLUCENT_SURFACE = new Color(28, 28, 28, 200);
    private final Color GLOW_COLOR = new Color(88, 86, 214, 150);

    public Client() {
        initializeGUI();
    }

    private void initializeGUI() {
        // Setup frame for translucency
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setTitle("Glowing Client");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Main container with rounded corners and glow
        mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TRANSLUCENT_BG);
                g2.fillRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 20, 20);
                g2.dispose();
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new ModernUI.GlowBorder(GLOW_COLOR, 5, 20));

        // Header (Draggable)
        JPanel headerPanel = createHeaderPanel();
        ModernUI.makeDraggable(this, headerPanel);

        // Content
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

        // Connection Bar
        JPanel connectionPanel = createConnectionPanel();

        // Chat Area
        createChatArea();

        // Input Area
        JPanel inputPanel = createInputPanel();

        contentPanel.add(connectionPanel, BorderLayout.NORTH);
        contentPanel.add(chatScroll, BorderLayout.CENTER);
        contentPanel.add(inputPanel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(15, 20, 5, 20));

        JLabel titleLabel = new JLabel("✨ Glowing Client");
        titleLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 16));
        titleLabel.setForeground(ModernUI.ThemeColors.TEXT_PRIMARY);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        controls.setOpaque(false);

        minimizeButton = new ModernUI.ModernButton("_", new Color(255, 255, 255, 20));
        minimizeButton.setPreferredSize(new Dimension(30, 30));
        minimizeButton.addActionListener(e -> setState(Frame.ICONIFIED));

        closeButton = new ModernUI.ModernButton("X", new Color(255, 59, 48, 180));
        closeButton.setPreferredSize(new Dimension(30, 30));
        closeButton.addActionListener(e -> System.exit(0));

        controls.add(minimizeButton);
        controls.add(closeButton);

        header.add(titleLabel, BorderLayout.WEST);
        header.add(controls, BorderLayout.EAST);
        return header;
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));

        serverField = new ModernUI.ModernTextField("localhost");
        serverField.setPreferredSize(new Dimension(120, 35));

        portField = new ModernUI.ModernTextField("12345");
        portField.setPreferredSize(new Dimension(80, 35));

        usernameField = new ModernUI.ModernTextField("User");
        usernameField.setPreferredSize(new Dimension(100, 35));

        connectButton = new ModernUI.ModernButton("Connect", ModernUI.ThemeColors.PRIMARY);
        connectButton.setPreferredSize(new Dimension(100, 35));
        connectButton.addActionListener(e -> toggleConnection());

        statusLabel = new JLabel("⚫ Offline");
        statusLabel.setForeground(ModernUI.ThemeColors.TEXT_SECONDARY);
        statusLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 12));

        panel.add(serverField);
        panel.add(portField);
        panel.add(usernameField);
        panel.add(connectButton);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(statusLabel);

        return panel;
    }

    private void createChatArea() {
        chatArea = new JPanel();
        chatArea.setLayout(new BoxLayout(chatArea, BoxLayout.Y_AXIS));
        chatArea.setOpaque(false);
        chatArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        chatScroll = new JScrollPane(chatArea);
        chatScroll.setOpaque(false);
        chatScroll.getViewport().setOpaque(false);
        chatScroll.setBorder(new ModernUI.GlowBorder(new Color(255, 255, 255, 30), 1, 10));
        chatScroll.getVerticalScrollBar().setUI(new ModernUI.ModernScrollBarUI());
        chatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);

        messageField = new ModernUI.ModernTextField("Type a message...");
        messageField.addActionListener(e -> sendMessage());

        sendButton = new ModernUI.ModernButton("➤", ModernUI.ThemeColors.PRIMARY);
        sendButton.setPreferredSize(new Dimension(50, 40));
        sendButton.addActionListener(e -> sendMessage());

        panel.add(messageField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        return panel;
    }

    private void toggleConnection() {
        if (isConnected) {
            disconnect();
        } else {
            connect();
        }
    }

    private void connect() {
        new Thread(() -> {
            try {
                String server = serverField.getText();
                int port = Integer.parseInt(portField.getText());
                String username = usernameField.getText();

                socket = new Socket(server, port);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                output.println("USERNAME|" + username);

                isConnected = true;
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("🟢 Connected");
                    statusLabel.setForeground(ModernUI.ThemeColors.SUCCESS);
                    connectButton.setText("Disconnect");
                    connectButton.setColors(ModernUI.ThemeColors.ERROR, ModernUI.ThemeColors.ERROR, ModernUI.ThemeColors.ERROR);
                    addSystemMessage("Connected to server as " + username);
                });

                listenForMessages();

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Connection failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private void disconnect() {
        isConnected = false;
        try {
            if (output != null) {
                output.println("/quit");
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("⚫ Offline");
            statusLabel.setForeground(ModernUI.ThemeColors.TEXT_SECONDARY);
            connectButton.setText("Connect");
            connectButton.setColors(ModernUI.ThemeColors.PRIMARY, ModernUI.ThemeColors.PRIMARY_HOVER, ModernUI.ThemeColors.PRIMARY_PRESSED);
            addSystemMessage("Disconnected from server");
        });
    }

    private void listenForMessages() {
        try {
            String message;
            while (isConnected && (message = input.readLine()) != null) {
                String finalMsg = message;
                SwingUtilities.invokeLater(() -> handleMessage(finalMsg));
            }
        } catch (IOException e) {
            if (isConnected) {
                disconnect();
            }
        }
    }

    private void handleMessage(String message) {
        if (message.startsWith("PING")) {
            output.println("PONG");
            return;
        }

        String[] parts = message.split("\\|", 2);
        if (parts.length < 2) {
            return;
        }

        String type = parts[0];
        String content = parts[1];

        if (type.equals("CHAT")) {
            String[] chatParts = content.split("\\|", 3);
            if (chatParts.length >= 3) {
                addMessage(chatParts[1], chatParts[2], false); // sender, content
            }
        } else if (type.equals("JOIN")) {
            addSystemMessage(content.split("\\|")[1] + " joined");
        } else if (type.equals("LEAVE")) {
            addSystemMessage(content.split("\\|")[1] + " left");
        }
    }

    private void sendMessage() {
        if (!isConnected) {
            return;
        }
        String text = messageField.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        output.println(text);
        addMessage("Me", text, true);
        messageField.setText("");
    }

    private void addMessage(String sender, String text, boolean isOwn) {
        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setOpaque(false);

        JPanel content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isOwn ? new Color(88, 86, 214, 180) : new Color(60, 60, 60, 180));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
            }
        };
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(8, 12, 8, 12));
        content.setLayout(new BorderLayout());

        JLabel nameLabel = new JLabel(sender);
        nameLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.BOLD, 11));
        nameLabel.setForeground(new Color(255, 255, 255, 150));

        JLabel textLabel = new JLabel("<html><body style='width: 250px'>" + text + "</body></html>");
        textLabel.setFont(ModernUI.getEmojiCompatibleFont(Font.PLAIN, 13));
        textLabel.setForeground(Color.WHITE);

        content.add(nameLabel, BorderLayout.NORTH);
        content.add(textLabel, BorderLayout.CENTER);

        JPanel row = new JPanel(new FlowLayout(isOwn ? FlowLayout.RIGHT : FlowLayout.LEFT));
        row.setOpaque(false);
        row.add(content);

        chatArea.add(row);
        chatArea.add(Box.createVerticalStrut(5));

        // Auto scroll
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScroll.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });

        chatArea.revalidate();
        chatArea.repaint();
    }

    private void addSystemMessage(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ModernUI.getEmojiCompatibleFont(Font.ITALIC, 11));
        label.setForeground(new Color(255, 255, 255, 100));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER));
        row.setOpaque(false);
        row.add(label);

        chatArea.add(row);
        chatArea.add(Box.createVerticalStrut(5));
        chatArea.revalidate();
        chatArea.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Client().setVisible(true);
        });
    }
}
