import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

class ModernButton extends JButton {
    private Color bgColor;
    private Color hoverColor;
    private javax.swing.Timer animationTimer;
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

        animationTimer = new javax.swing.Timer(16, e -> {
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
    private javax.swing.Timer animationTimer;
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

        animationTimer = new javax.swing.Timer(16, e -> {
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