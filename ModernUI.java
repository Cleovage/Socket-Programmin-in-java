
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;

/**
 * ModernUI.java - Custom modern UI components for the Socket Programming
 * application Provides modern, animated UI components with dark theme support
 */
// Helper method for emoji-compatible font
class FontHelper {

    public static Font getEmojiCompatibleFont(int style, int size) {
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
        return new Font("SansSerif", style, size);
    }
}

class ModernButton extends JButton {

    private final Color bgColor;
    private final Color hoverColor;
    private javax.swing.Timer animationTimer;
    private float animationProgress = 0.0f;
    private boolean isHovering = false;
    private float shadowIntensity = 0.0f;

    public ModernButton(String text, Color bgColor) {
        super(text);
        this.bgColor = bgColor;
        this.hoverColor = bgColor.brighter();

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFont(FontHelper.getEmojiCompatibleFont(Font.BOLD, 13));
        setForeground(Color.WHITE);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(12, 24, 12, 24));

        animationTimer = new javax.swing.Timer(16, e -> {
            boolean changed = false;
            if (isHovering && animationProgress < 1.0f) {
                animationProgress = Math.min(1.0f, animationProgress + 0.15f);
                shadowIntensity = Math.min(1.0f, shadowIntensity + 0.15f);
                changed = true;
            } else if (!isHovering && animationProgress > 0.0f) {
                animationProgress = Math.max(0.0f, animationProgress - 0.15f);
                shadowIntensity = Math.max(0.0f, shadowIntensity - 0.15f);
                changed = true;
            }

            if (changed) {
                repaint();
            } else {
                ((javax.swing.Timer) e.getSource()).stop();
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
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw enhanced shadow when hovering
        if (shadowIntensity > 0) {
            g2d.setColor(new Color(0, 0, 0, (int) (shadowIntensity * 50)));
            g2d.fillRoundRect(3, 4, getWidth() - 6, getHeight() - 6, 10, 10);
        }

        // Interpolate colors with smooth transition
        Color currentColor = interpolateColor(bgColor, hoverColor, animationProgress);
        g2d.setColor(currentColor);

        // Draw main button with rounded corners
        g2d.fillRoundRect(0, 0, getWidth() - (int) (shadowIntensity * 2),
                getHeight() - (int) (shadowIntensity * 2), 10, 10);

        // Add subtle inner glow when hovering
        if (animationProgress > 0) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, animationProgress * 0.2f));
            g2d.setColor(Color.WHITE);
            g2d.fillRoundRect(2, 2, getWidth() - (int) (shadowIntensity * 2) - 4,
                    getHeight() - (int) (shadowIntensity * 2) - 4, 8, 8);
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

    private final Color focusColor;
    private final Color borderColor;
    private boolean isFocused = false;
    private javax.swing.Timer animationTimer;
    private float animationProgress = 0.0f;
    private float glowIntensity = 0.0f;

    public ModernTextField(String text, int columns, Color focusColor, Color borderColor) {
        super(text, columns);
        this.focusColor = focusColor;
        this.borderColor = borderColor;

        setFont(FontHelper.getEmojiCompatibleFont(Font.PLAIN, 14));
        setBackground(new Color(48, 48, 48));
        setForeground(new Color(240, 240, 240));
        setCaretColor(focusColor);
        setBorder(new EmptyBorder(12, 16, 12, 16));

        animationTimer = new javax.swing.Timer(16, e -> {
            boolean changed = false;
            if (isFocused && animationProgress < 1.0f) {
                animationProgress = Math.min(1.0f, animationProgress + 0.12f);
                glowIntensity = Math.min(1.0f, glowIntensity + 0.12f);
                changed = true;
            } else if (!isFocused && animationProgress > 0.0f) {
                animationProgress = Math.max(0.0f, animationProgress - 0.12f);
                glowIntensity = Math.max(0.0f, glowIntensity - 0.12f);
                changed = true;
            }

            if (changed) {
                repaint();
            } else {
                ((javax.swing.Timer) e.getSource()).stop();
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
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw subtle outer glow when focused
        if (glowIntensity > 0) {
            g2d.setColor(new Color(focusColor.getRed(), focusColor.getGreen(),
                    focusColor.getBlue(), (int) (glowIntensity * 40)));
            g2d.setStroke(new BasicStroke(3 + glowIntensity * 2));
            g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        }

        // Draw main border with smooth color transition
        Color currentColor = interpolateColor(borderColor, focusColor, animationProgress);
        g2d.setColor(currentColor);
        g2d.setStroke(new BasicStroke(2 + animationProgress));
        g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 7, 7);

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

    private final Color backgroundColor;
    private final int cornerRadius;
    private final boolean hasShadow;

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

class ModernScrollBarUI extends BasicScrollBarUI {

    // Note: Field hiding warnings are expected as we override parent class fields
    @SuppressWarnings({"FieldHidesField", "UnusedVariable"})
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
