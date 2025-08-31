import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;

public class AdvancedClient extends JFrame {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private boolean isConnected = false;

    // Modern UI Components
    private JTextPane chatArea;
    private JTextField messageField;
    private JButton connectButton, sendButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JPanel connectionPanel, chatPanel, inputPanel;
    private JLabel statusLabel, userCountLabel;
    private JLabel typingLabel;
    private JTextField serverField, portField, usernameField;

    private String serverAddress = "localhost";
    private int serverPort = 12345;
    private String username = "Anonymous";

    // Modern color scheme
    private final Color PRIMARY_COLOR = new Color(63, 81, 181);
    private final Color ACCENT_COLOR = new Color(255, 87, 34);
    private final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private final Color BACKGROUND_COLOR = new Color(250, 250, 250);
    private final Color CHAT_BACKGROUND = new Color(255, 255, 255);

    public AdvancedClient() {
        initializeModernGUI();
        setupNetworking();
        // Ensure window is visible and not minimized
        setVisible(true);
        setExtendedState(JFrame.NORMAL);
        toFront();
    }

    private void initializeModernGUI() {
        setTitle("💬 Modern Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));

        // Set modern look and feel
        try {
            // Use default look and feel
        } catch (Exception e) {
            // Use default
        }

        // Main layout
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_COLOR);

        // Create modern components
        createConnectionPanel();
        createChatPanel();
        createInputPanel();

        // Add components
        add(connectionPanel, BorderLayout.NORTH);
        add(chatPanel, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // Add keyboard shortcuts
        setupKeyboardShortcuts();
    }

    private void createConnectionPanel() {
        connectionPanel = new JPanel(new BorderLayout());
        connectionPanel.setBackground(PRIMARY_COLOR);
        connectionPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // Left side - Connection controls
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setOpaque(false);

        // Server input with modern styling
        serverField = createStyledTextField(serverAddress, 12);
        portField = createStyledTextField(String.valueOf(serverPort), 6);
        usernameField = createStyledTextField(username, 12);

        leftPanel.add(createStyledLabel("🌐 Server:", Color.WHITE));
        leftPanel.add(serverField);
        leftPanel.add(createStyledLabel("🔌 Port:", Color.WHITE));
        leftPanel.add(portField);
        leftPanel.add(createStyledLabel("👤 Username:", Color.WHITE));
        leftPanel.add(usernameField);

        // Connect button with modern styling
        connectButton = createStyledButton("🔗 Connect", SUCCESS_COLOR);
        connectButton.addActionListener(_ -> connectToServer());

        leftPanel.add(connectButton);

        // Right side - Status
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setOpaque(false);

        statusLabel = new JLabel("🔴 Disconnected");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        userCountLabel = new JLabel("👥 0 online");
        userCountLabel.setForeground(Color.WHITE);
        userCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        rightPanel.add(userCountLabel);
        rightPanel.add(statusLabel);

        connectionPanel.add(leftPanel, BorderLayout.WEST);
        connectionPanel.add(rightPanel, BorderLayout.EAST);
    }

    private void createChatPanel() {
        chatPanel = new JPanel(new BorderLayout(10, 0));
        chatPanel.setBackground(BACKGROUND_COLOR);
        chatPanel.setBorder(new EmptyBorder(20, 20, 10, 20));

        // Chat area with modern styling
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setBackground(CHAT_BACKGROUND);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chatArea.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(15, 15, 15, 15)));

        // Add custom document to handle text styling
        StyledDocument doc = chatArea.getStyledDocument();
        addStylesToDocument(doc);

        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(PRIMARY_COLOR, 2),
                "💬 Chat Messages",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14),
                PRIMARY_COLOR));
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // User list with modern styling
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        userList.setBackground(CHAT_BACKGROUND);
        userList.setSelectionBackground(new Color(63, 81, 181, 100));
        userList.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(ACCENT_COLOR, 2),
                "👥 Online Users (0)",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14),
                ACCENT_COLOR));
        userScroll.setPreferredSize(new Dimension(200, 0));

        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(userScroll, BorderLayout.EAST);
        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && userList.getSelectedValue() != null) {
                    String u = userList.getSelectedValue();
                    messageField.setText("@" + u + " ");
                    messageField.requestFocusInWindow();
                }
            }
        });
    }

    private void createInputPanel() {
        inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(BACKGROUND_COLOR);
        inputPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

        // Message input area
        JPanel inputContainer = new JPanel(new BorderLayout(10, 0));
        inputContainer.setBackground(CHAT_BACKGROUND);
        inputContainer.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1),
                new EmptyBorder(10, 10, 10, 10)));

        messageField = new JTextField();
        messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageField.setBorder(new EmptyBorder(5, 5, 5, 5));
        messageField.setEnabled(false);

        sendButton = createStyledButton("📤 Send", PRIMARY_COLOR);
        sendButton.setEnabled(false);
        sendButton.addActionListener(_ -> sendMessage());

        // Enter key to send message
        messageField.addActionListener(_ -> sendMessage());

        // Typing indicator
        messageField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private javax.swing.Timer typingTimer = new javax.swing.Timer(1500, evt -> sendTyping(false));
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

        inputContainer.add(messageField, BorderLayout.CENTER);
        inputContainer.add(sendButton, BorderLayout.EAST);

        inputPanel.add(inputContainer, BorderLayout.CENTER);

        typingLabel = new JLabel(" ");
        typingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        typingLabel.setForeground(new Color(120, 120, 120));
        inputPanel.add(typingLabel, BorderLayout.SOUTH);
    }

    private JTextField createStyledTextField(String text, int columns) {
        JTextField field = new JTextField(text, columns);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1),
                new EmptyBorder(5, 8, 5, 8)));
        field.setBackground(Color.WHITE);
        return field;
    }

    private JLabel createStyledLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return label;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    private void addStylesToDocument(StyledDocument doc) {
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontFamily(def, "Segoe UI");
        StyleConstants.setFontSize(def, 13);

        Style system = doc.addStyle("system", regular);
        StyleConstants.setForeground(system, new Color(100, 100, 100));
        StyleConstants.setItalic(system, true);

        Style user = doc.addStyle("user", regular);
        StyleConstants.setForeground(user, PRIMARY_COLOR);
        StyleConstants.setBold(user, true);

        Style ownMessage = doc.addStyle("own", regular);
        StyleConstants.setForeground(ownMessage, ACCENT_COLOR);
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
                statusLabel.setForeground(Color.WHITE);
                setTitle("💬 Modern Chat Client - " + username);
                appendToChat("Connected to server as " + username + "\n", "system");
                updateConnectionStatus();
            });

        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Failed to connect: " + e.getMessage(),
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
            });
        } catch (NumberFormatException e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Invalid port number", "Error", JOptionPane.ERROR_MESSAGE);
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
            statusLabel.setForeground(Color.WHITE);
            appendToChat("You left the chat\n", "system");
            userListModel.clear();
            userCountLabel.setText("👥 0 online");
            updateConnectionStatus();
        });
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

    private void updateConnectionStatus() {
        boolean connected = isConnected;
        connectButton.setEnabled(!connected);
        sendButton.setEnabled(connected);
        messageField.setEnabled(connected);

        // Update field states
        serverField.setEnabled(!connected);
        portField.setEnabled(!connected);
        usernameField.setEnabled(!connected);
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