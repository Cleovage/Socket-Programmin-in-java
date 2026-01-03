import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Modern UI Components Library
 * Premium dark theme components with rounded corners, animations, and consistent styling
 */
public class ModernUI {
    
    // ==================== THEME COLORS ====================
    public static class ThemeColors {
        // Base colors
        public static final Color BACKGROUND = new Color(18, 18, 18);
        public static final Color SURFACE = new Color(28, 28, 28);
        public static final Color CARD = new Color(38, 38, 38);
        public static final Color CARD_HOVER = new Color(48, 48, 48);
        
        // Primary accent
        public static final Color PRIMARY = new Color(88, 86, 214);
        public static final Color PRIMARY_HOVER = new Color(108, 106, 234);
        public static final Color PRIMARY_PRESSED = new Color(68, 66, 194);
        
        // Secondary accent
        public static final Color SECONDARY = new Color(52, 199, 89);
        public static final Color SECONDARY_HOVER = new Color(72, 219, 109);
        
        // Status colors
        public static final Color SUCCESS = new Color(52, 199, 89);
        public static final Color WARNING = new Color(255, 159, 10);
        public static final Color ERROR = new Color(255, 69, 58);
        public static final Color INFO = new Color(10, 132, 255);
        
        // Text colors
        public static final Color TEXT_PRIMARY = new Color(220, 220, 220);
        public static final Color TEXT_SECONDARY = new Color(160, 160, 160);
        public static final Color TEXT_DISABLED = new Color(100, 100, 100);
        public static final Color TEXT_HINT = new Color(120, 120, 120);
        
        // Border colors
        public static final Color BORDER = new Color(60, 60, 60);
        public static final Color BORDER_LIGHT = new Color(70, 70, 70);
        public static final Color BORDER_FOCUS = PRIMARY;
        
        // Scrollbar colors
        public static final Color SCROLLBAR_TRACK = new Color(30, 30, 30);
        public static final Color SCROLLBAR_THUMB = new Color(70, 70, 70);
        public static final Color SCROLLBAR_THUMB_HOVER = new Color(90, 90, 90);
        
        // Table colors
        public static final Color TABLE_HEADER = new Color(35, 35, 35);
        public static final Color TABLE_ROW_ALT = new Color(32, 32, 32);
        public static final Color TABLE_SELECTION = new Color(88, 86, 214, 60);
        
        // Online/Offline
        public static final Color ONLINE = new Color(52, 199, 89);
        public static final Color OFFLINE = new Color(142, 142, 147);
    }
    
    // ==================== CONSTANTS ====================
    public static final int CORNER_RADIUS = 12;
    public static final int CORNER_RADIUS_SMALL = 8;
    public static final int CORNER_RADIUS_LARGE = 16;
    public static final int PADDING = 16;
    public static final int PADDING_SMALL = 8;
    public static final int PADDING_LARGE = 24;
    
    // ==================== FONT HELPER ====================
    public static Font getEmojiCompatibleFont(int style, int size) {
        String[] fontNames = {
            "Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji",
            "Segoe UI Symbol", "Symbola", "Segoe UI", "Arial"
        };
        for (String fontName : fontNames) {
            Font font = new Font(fontName, style, size);
            // Check if font can display emoji (using code point instead of char literal)
            if (font.canDisplay(0x1F600) || fontName.equals("Segoe UI") || fontName.equals("Arial")) {
                return font;
            }
        }
        return new Font("Segoe UI", style, size);
    }
    
    // ==================== MODERN BUTTON ====================
    public static class ModernButton extends JButton {
        private Color normalColor;
        private Color hoverColor;
        private Color pressedColor;
        private Color textColor;
        private int cornerRadius;
        private boolean isHovered = false;
        private boolean isPressed = false;
        private float animationProgress = 0f;
        private Timer animationTimer;
        
        public ModernButton(String text) {
            this(text, ThemeColors.PRIMARY);
        }
        
        public ModernButton(String text, Color color) {
            super(text);
            this.normalColor = color;
            this.hoverColor = brightenColor(color, 20);
            this.pressedColor = darkenColor(color, 20);
            this.textColor = ThemeColors.TEXT_PRIMARY;
            this.cornerRadius = CORNER_RADIUS;
            
            setupButton();
        }
        
        private void setupButton() {
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setFont(getEmojiCompatibleFont(Font.BOLD, 13));
            setForeground(textColor);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(120, 38));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    startAnimation(true);
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    isPressed = false;
                    startAnimation(false);
                }
                
                @Override
                public void mousePressed(MouseEvent e) {
                    isPressed = true;
                    repaint();
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    isPressed = false;
                    repaint();
                }
            });
        }
        
        private void startAnimation(boolean forward) {
            if (animationTimer != null) {
                animationTimer.stop();
            }
            animationTimer = new Timer(16, e -> {
                if (forward) {
                    animationProgress = Math.min(1f, animationProgress + 0.15f);
                } else {
                    animationProgress = Math.max(0f, animationProgress - 0.15f);
                }
                repaint();
                if ((forward && animationProgress >= 1f) || (!forward && animationProgress <= 0f)) {
                    ((Timer) e.getSource()).stop();
                }
            });
            animationTimer.start();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            
            Color bgColor;
            if (isPressed) {
                bgColor = pressedColor;
            } else if (isHovered) {
                bgColor = interpolateColor(normalColor, hoverColor, animationProgress);
            } else {
                bgColor = interpolateColor(hoverColor, normalColor, 1f - animationProgress);
            }
            
            // Draw shadow for depth
            if (!isPressed) {
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fill(new RoundRectangle2D.Float(2, 3, getWidth() - 4, getHeight() - 4, cornerRadius, cornerRadius));
            }
            
            // Draw button background
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius));
            
            // Draw text
            g2.setColor(textColor);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textX = (getWidth() - fm.stringWidth(getText())) / 2;
            int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(getText(), textX, textY);
            
            g2.dispose();
        }
        
        public void setColors(Color normal, Color hover, Color pressed) {
            this.normalColor = normal;
            this.hoverColor = hover;
            this.pressedColor = pressed;
            repaint();
        }
        
        public void setCornerRadius(int radius) {
            this.cornerRadius = radius;
            repaint();
        }
    }
    
    // ==================== MODERN TEXT FIELD ====================
    public static class ModernTextField extends JTextField {
        private String placeholder;
        private Color placeholderColor;
        private Color borderColor;
        private Color focusBorderColor;
        private int cornerRadius;
        private boolean isFocused = false;
        
        public ModernTextField() {
            this("");
        }
        
        public ModernTextField(String placeholder) {
            this.placeholder = placeholder;
            this.placeholderColor = ThemeColors.TEXT_HINT;
            this.borderColor = ThemeColors.BORDER;
            this.focusBorderColor = ThemeColors.PRIMARY;
            this.cornerRadius = CORNER_RADIUS;
            
            setupTextField();
        }
        
        private void setupTextField() {
            setOpaque(false);
            setBackground(ThemeColors.CARD);
            setForeground(ThemeColors.TEXT_PRIMARY);
            setCaretColor(ThemeColors.PRIMARY);
            setFont(getEmojiCompatibleFont(Font.PLAIN, 14));
            setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            setPreferredSize(new Dimension(200, 40));
            
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    isFocused = true;
                    repaint();
                }
                
                @Override
                public void focusLost(FocusEvent e) {
                    isFocused = false;
                    repaint();
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            
            // Draw background
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius));
            
            // Draw border
            g2.setColor(isFocused ? focusBorderColor : borderColor);
            g2.setStroke(new BasicStroke(isFocused ? 2f : 1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 2, getHeight() - 2, cornerRadius, cornerRadius));
            
            g2.dispose();
            
            super.paintComponent(g);
            
            // Draw placeholder
            if (getText().isEmpty() && !isFocused && placeholder != null && !placeholder.isEmpty()) {
                Graphics2D g3 = (Graphics2D) g.create();
                g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                g3.setColor(placeholderColor);
                g3.setFont(getFont());
                Insets insets = getInsets();
                g3.drawString(placeholder, insets.left, getHeight() / 2 + g3.getFontMetrics().getAscent() / 2 - 2);
                g3.dispose();
            }
        }
        
        public void setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
            repaint();
        }
        
        public void setCornerRadius(int radius) {
            this.cornerRadius = radius;
            repaint();
        }
    }
    
    // ==================== MODERN PANEL ====================
    public static class ModernPanel extends JPanel {
        private Color backgroundColor;
        private int cornerRadius;
        private boolean hasBorder;
        private Color borderColor;
        private boolean hasShadow;
        
        public ModernPanel() {
            this(ThemeColors.SURFACE, CORNER_RADIUS, false);
        }
        
        public ModernPanel(Color bgColor) {
            this(bgColor, CORNER_RADIUS, false);
        }
        
        public ModernPanel(Color bgColor, int cornerRadius, boolean hasBorder) {
            this.backgroundColor = bgColor;
            this.cornerRadius = cornerRadius;
            this.hasBorder = hasBorder;
            this.borderColor = ThemeColors.BORDER;
            this.hasShadow = false;
            
            setOpaque(false);
            setBackground(backgroundColor);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int offset = hasShadow ? 2 : 0;
            
            // Draw shadow
            if (hasShadow) {
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fill(new RoundRectangle2D.Float(3, 4, getWidth() - 6, getHeight() - 6, cornerRadius, cornerRadius));
            }
            
            // Draw background
            g2.setColor(backgroundColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1 - offset, getHeight() - 1 - offset, cornerRadius, cornerRadius));
            
            // Draw border
            if (hasBorder) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 2 - offset, getHeight() - 2 - offset, cornerRadius, cornerRadius));
            }
            
            g2.dispose();
            super.paintComponent(g);
        }
        
        public void setCornerRadius(int radius) {
            this.cornerRadius = radius;
            repaint();
        }
        
        public void setHasBorder(boolean hasBorder) {
            this.hasBorder = hasBorder;
            repaint();
        }
        
        public void setBorderColor(Color color) {
            this.borderColor = color;
            repaint();
        }
        
        public void setHasShadow(boolean hasShadow) {
            this.hasShadow = hasShadow;
            repaint();
        }
        
        public void setBackgroundColor(Color color) {
            this.backgroundColor = color;
            setBackground(color);
            repaint();
        }
    }
    
    // ==================== MODERN CARD ====================
    public static class ModernCard extends ModernPanel {
        private boolean isHoverable;
        private boolean isHovered;
        private Color normalColor;
        private Color hoverColor;
        
        public ModernCard() {
            this(ThemeColors.CARD, true);
        }
        
        public ModernCard(Color bgColor, boolean hoverable) {
            super(bgColor, CORNER_RADIUS, true);
            this.normalColor = bgColor;
            this.hoverColor = ThemeColors.CARD_HOVER;
            this.isHoverable = hoverable;
            this.isHovered = false;
            
            setHasShadow(true);
            
            if (hoverable) {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        setBackgroundColor(hoverColor);
                    }
                    
                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        setBackgroundColor(normalColor);
                    }
                });
            }
        }
        
        public void setHoverable(boolean hoverable) {
            this.isHoverable = hoverable;
        }
    }
    
    // ==================== MODERN TOGGLE BUTTON ====================
    public static class ModernToggleButton extends JPanel {
        private boolean isOn;
        private Color onColor;
        private Color offColor;
        private Color thumbColor;
        private float animationProgress;
        private Timer animationTimer;
        private java.util.List<ActionListener> listeners = new java.util.ArrayList<>();
        
        public ModernToggleButton() {
            this(false);
        }
        
        public ModernToggleButton(boolean initialState) {
            this.isOn = initialState;
            this.onColor = ThemeColors.PRIMARY;
            this.offColor = ThemeColors.BORDER;
            this.thumbColor = ThemeColors.TEXT_PRIMARY;
            this.animationProgress = isOn ? 1f : 0f;
            
            setOpaque(false);
            setPreferredSize(new Dimension(50, 26));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    toggle();
                }
            });
        }
        
        public void toggle() {
            isOn = !isOn;
            startAnimation();
            fireActionPerformed();
        }
        
        private void startAnimation() {
            if (animationTimer != null) {
                animationTimer.stop();
            }
            animationTimer = new Timer(16, e -> {
                if (isOn) {
                    animationProgress = Math.min(1f, animationProgress + 0.12f);
                } else {
                    animationProgress = Math.max(0f, animationProgress - 0.12f);
                }
                repaint();
                if ((isOn && animationProgress >= 1f) || (!isOn && animationProgress <= 0f)) {
                    ((Timer) e.getSource()).stop();
                }
            });
            animationTimer.start();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            int trackHeight = 22;
            int thumbSize = 18;
            int padding = 2;
            
            int trackY = (height - trackHeight) / 2;
            
            // Draw track
            Color trackColor = interpolateColor(offColor, onColor, animationProgress);
            g2.setColor(trackColor);
            g2.fill(new RoundRectangle2D.Float(0, trackY, width, trackHeight, trackHeight, trackHeight));
            
            // Draw thumb
            float thumbX = padding + (width - thumbSize - padding * 2) * animationProgress;
            float thumbY = trackY + (trackHeight - thumbSize) / 2f;
            
            g2.setColor(thumbColor);
            g2.fill(new Ellipse2D.Float(thumbX, thumbY, thumbSize, thumbSize));
            
            g2.dispose();
        }
        
        public boolean isOn() {
            return isOn;
        }
        
        public void setOn(boolean on) {
            if (this.isOn != on) {
                this.isOn = on;
                this.animationProgress = on ? 1f : 0f;
                repaint();
            }
        }
        
        public void addActionListener(ActionListener listener) {
            listeners.add(listener);
        }
        
        private void fireActionPerformed() {
            ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, isOn ? "on" : "off");
            for (ActionListener listener : listeners) {
                listener.actionPerformed(event);
            }
        }
    }
    
    // ==================== MODERN PROGRESS BAR ====================
    public static class ModernProgressBar extends JPanel {
        private int value;
        private int maximum;
        private Color trackColor;
        private Color progressColor;
        private boolean indeterminate;
        private float indeterminateOffset;
        private Timer indeterminateTimer;
        
        public ModernProgressBar() {
            this(0, 100);
        }
        
        public ModernProgressBar(int value, int maximum) {
            this.value = value;
            this.maximum = maximum;
            this.trackColor = ThemeColors.SURFACE;
            this.progressColor = ThemeColors.PRIMARY;
            this.indeterminate = false;
            
            setOpaque(false);
            setPreferredSize(new Dimension(200, 8));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            int radius = height;
            
            // Draw track
            g2.setColor(trackColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, width, height, radius, radius));
            
            if (indeterminate) {
                // Draw indeterminate progress
                int barWidth = width / 3;
                float x = (width + barWidth) * indeterminateOffset - barWidth;
                g2.setColor(progressColor);
                g2.setClip(new RoundRectangle2D.Float(0, 0, width, height, radius, radius));
                g2.fill(new RoundRectangle2D.Float(x, 0, barWidth, height, radius, radius));
            } else {
                // Draw determinate progress
                float progressWidth = (float) value / maximum * width;
                if (progressWidth > 0) {
                    g2.setColor(progressColor);
                    g2.fill(new RoundRectangle2D.Float(0, 0, progressWidth, height, radius, radius));
                }
            }
            
            g2.dispose();
        }
        
        public void setValue(int value) {
            this.value = Math.max(0, Math.min(maximum, value));
            repaint();
        }
        
        public int getValue() {
            return value;
        }
        
        public void setMaximum(int maximum) {
            this.maximum = maximum;
            repaint();
        }
        
        public void setIndeterminate(boolean indeterminate) {
            this.indeterminate = indeterminate;
            if (indeterminate) {
                if (indeterminateTimer == null) {
                    indeterminateTimer = new Timer(30, e -> {
                        indeterminateOffset += 0.02f;
                        if (indeterminateOffset > 1f) {
                            indeterminateOffset = 0f;
                        }
                        repaint();
                    });
                }
                indeterminateTimer.start();
            } else if (indeterminateTimer != null) {
                indeterminateTimer.stop();
            }
        }
        
        public void setProgressColor(Color color) {
            this.progressColor = color;
            repaint();
        }
    }
    
    // ==================== MODERN SPINNER ====================
    public static class ModernSpinner extends JSpinner {
        public ModernSpinner(SpinnerModel model) {
            super(model);
            setupSpinner();
        }
        
        private void setupSpinner() {
            setBackground(ThemeColors.CARD);
            setForeground(ThemeColors.TEXT_PRIMARY);
            setFont(getEmojiCompatibleFont(Font.PLAIN, 14));
            
            JComponent editor = getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
                tf.setBackground(ThemeColors.CARD);
                tf.setForeground(ThemeColors.TEXT_PRIMARY);
                tf.setCaretColor(ThemeColors.PRIMARY);
                tf.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            }
            
            setBorder(new RoundedBorder(ThemeColors.BORDER, CORNER_RADIUS_SMALL, 1));
        }
    }
    
    // ==================== MODERN SCROLL BAR UI ====================
    public static class ModernScrollBarUI extends BasicScrollBarUI {
        private int thumbSize = 8;
        
        @Override
        protected void configureScrollBarColors() {
            this.trackColor = ThemeColors.SCROLLBAR_TRACK;
            this.thumbColor = ThemeColors.SCROLLBAR_THUMB;
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
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(trackColor);
            g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            g2.dispose();
        }
        
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            Color color = isDragging ? ThemeColors.SCROLLBAR_THUMB_HOVER : 
                         (isThumbRollover() ? ThemeColors.SCROLLBAR_THUMB_HOVER : thumbColor);
            g2.setColor(color);
            
            int x, y, width, height;
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                int margin = (thumbBounds.width - thumbSize) / 2;
                x = thumbBounds.x + margin;
                y = thumbBounds.y + 2;
                width = thumbSize;
                height = thumbBounds.height - 4;
            } else {
                int margin = (thumbBounds.height - thumbSize) / 2;
                x = thumbBounds.x + 2;
                y = thumbBounds.y + margin;
                width = thumbBounds.width - 4;
                height = thumbSize;
            }
            
            g2.fill(new RoundRectangle2D.Float(x, y, width, height, thumbSize, thumbSize));
            g2.dispose();
        }
        
        @Override
        protected Dimension getMinimumThumbSize() {
            return new Dimension(thumbSize, thumbSize * 3);
        }
    }
    
    // ==================== ROUNDED BORDER ====================
    public static class RoundedBorder extends AbstractBorder {
        private Color color;
        private int radius;
        private int thickness;
        
        public RoundedBorder(Color color, int radius, int thickness) {
            this.color = color;
            this.radius = radius;
            this.thickness = thickness;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, width - 1, height - 1, radius, radius));
            g2.dispose();
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness + 2, thickness + 4, thickness + 2, thickness + 4);
        }
        
        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
    
    // ==================== MODERN TITLED BORDER ====================
    public static class ModernTitledBorder extends TitledBorder {
        private int cornerRadius;
        
        public ModernTitledBorder(String title) {
            super(title);
            this.cornerRadius = CORNER_RADIUS;
            setTitleColor(ThemeColors.TEXT_SECONDARY);
            setTitleFont(getEmojiCompatibleFont(Font.BOLD, 12));
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            
            FontMetrics fm = g2.getFontMetrics(getTitleFont());
            int titleHeight = fm.getHeight();
            int titleY = y + titleHeight;
            
            // Draw rounded border
            g2.setColor(ThemeColors.BORDER);
            g2.setStroke(new BasicStroke(1f));
            int borderY = y + titleHeight / 2;
            g2.draw(new RoundRectangle2D.Float(x + 0.5f, borderY + 0.5f, width - 1, height - borderY - 1, cornerRadius, cornerRadius));
            
            // Draw title background to cover border
            String title = getTitle();
            if (title != null && !title.isEmpty()) {
                int titleWidth = fm.stringWidth(title);
                g2.setColor(c.getBackground());
                g2.fillRect(x + 12, y, titleWidth + 8, titleHeight);
                
                // Draw title
                g2.setColor(getTitleColor());
                g2.setFont(getTitleFont());
                g2.drawString(title, x + 16, y + fm.getAscent());
            }
            
            g2.dispose();
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            FontMetrics fm = c.getFontMetrics(getTitleFont());
            int titleHeight = fm.getHeight();
            return new Insets(titleHeight + 8, 12, 12, 12);
        }
    }
    
    // ==================== TABLE STYLER ====================
    public static class TableStyler {
        public static void styleTable(JTable table) {
            table.setBackground(ThemeColors.SURFACE);
            table.setForeground(ThemeColors.TEXT_PRIMARY);
            table.setSelectionBackground(ThemeColors.TABLE_SELECTION);
            table.setSelectionForeground(ThemeColors.TEXT_PRIMARY);
            table.setGridColor(ThemeColors.BORDER);
            table.setRowHeight(40);
            table.setFont(getEmojiCompatibleFont(Font.PLAIN, 13));
            table.setShowGrid(false);
            table.setIntercellSpacing(new Dimension(0, 1));
            table.setFillsViewportHeight(true);
            
            // Style header
            JTableHeader header = table.getTableHeader();
            header.setBackground(ThemeColors.TABLE_HEADER);
            header.setForeground(ThemeColors.TEXT_SECONDARY);
            header.setFont(getEmojiCompatibleFont(Font.BOLD, 12));
            header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeColors.BORDER));
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, 44));
            
            // Alternating row colors
            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    
                    if (isSelected) {
                        c.setBackground(ThemeColors.TABLE_SELECTION);
                        c.setForeground(ThemeColors.TEXT_PRIMARY);
                    } else {
                        c.setBackground(row % 2 == 0 ? ThemeColors.SURFACE : ThemeColors.TABLE_ROW_ALT);
                        c.setForeground(ThemeColors.TEXT_PRIMARY);
                    }
                    
                    setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                    return c;
                }
            });
        }
        
        public static JScrollPane createStyledScrollPane(JTable table) {
            styleTable(table);
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBackground(ThemeColors.SURFACE);
            scrollPane.getViewport().setBackground(ThemeColors.SURFACE);
            scrollPane.setBorder(new RoundedBorder(ThemeColors.BORDER, CORNER_RADIUS, 1));
            scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
            scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
            return scrollPane;
        }
    }
    
    // ==================== COMBO BOX STYLER ====================
    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(ThemeColors.CARD);
        comboBox.setForeground(ThemeColors.TEXT_PRIMARY);
        comboBox.setFont(getEmojiCompatibleFont(Font.PLAIN, 13));
        comboBox.setBorder(new RoundedBorder(ThemeColors.BORDER, CORNER_RADIUS_SMALL, 1));
        
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? ThemeColors.PRIMARY : ThemeColors.CARD);
                setForeground(ThemeColors.TEXT_PRIMARY);
                setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
                return this;
            }
        });
    }
    
    // ==================== UTILITY METHODS ====================
    public static Color brightenColor(Color color, int amount) {
        return new Color(
            Math.min(255, color.getRed() + amount),
            Math.min(255, color.getGreen() + amount),
            Math.min(255, color.getBlue() + amount),
            color.getAlpha()
        );
    }
    
    public static Color darkenColor(Color color, int amount) {
        return new Color(
            Math.max(0, color.getRed() - amount),
            Math.max(0, color.getGreen() - amount),
            Math.max(0, color.getBlue() - amount),
            color.getAlpha()
        );
    }
    
    public static Color interpolateColor(Color c1, Color c2, float ratio) {
        ratio = Math.max(0, Math.min(1, ratio));
        int red = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * ratio);
        int green = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * ratio);
        int blue = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * ratio);
        int alpha = (int) (c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * ratio);
        return new Color(red, green, blue, alpha);
    }
    
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
    
    // ==================== LOOK AND FEEL SETUP ====================
    public static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            
            // Global UI defaults
            UIManager.put("Panel.background", ThemeColors.BACKGROUND);
            UIManager.put("Label.foreground", ThemeColors.TEXT_PRIMARY);
            UIManager.put("TextField.background", ThemeColors.CARD);
            UIManager.put("TextField.foreground", ThemeColors.TEXT_PRIMARY);
            UIManager.put("TextField.caretForeground", ThemeColors.PRIMARY);
            UIManager.put("TextArea.background", ThemeColors.CARD);
            UIManager.put("TextArea.foreground", ThemeColors.TEXT_PRIMARY);
            UIManager.put("TextArea.caretForeground", ThemeColors.PRIMARY);
            UIManager.put("ScrollPane.background", ThemeColors.SURFACE);
            UIManager.put("ScrollBar.background", ThemeColors.SCROLLBAR_TRACK);
            UIManager.put("ScrollBar.thumb", ThemeColors.SCROLLBAR_THUMB);
            UIManager.put("TabbedPane.background", ThemeColors.SURFACE);
            UIManager.put("TabbedPane.foreground", ThemeColors.TEXT_PRIMARY);
            UIManager.put("TabbedPane.selected", ThemeColors.PRIMARY);
            UIManager.put("ComboBox.background", ThemeColors.CARD);
            UIManager.put("ComboBox.foreground", ThemeColors.TEXT_PRIMARY);
            UIManager.put("List.background", ThemeColors.SURFACE);
            UIManager.put("List.foreground", ThemeColors.TEXT_PRIMARY);
            UIManager.put("List.selectionBackground", ThemeColors.PRIMARY);
            UIManager.put("List.selectionForeground", ThemeColors.TEXT_PRIMARY);
            UIManager.put("Table.background", ThemeColors.SURFACE);
            UIManager.put("Table.foreground", ThemeColors.TEXT_PRIMARY);
            UIManager.put("Table.selectionBackground", ThemeColors.TABLE_SELECTION);
            UIManager.put("Table.selectionForeground", ThemeColors.TEXT_PRIMARY);
            UIManager.put("Table.gridColor", ThemeColors.BORDER);
            UIManager.put("TableHeader.background", ThemeColors.TABLE_HEADER);
            UIManager.put("TableHeader.foreground", ThemeColors.TEXT_SECONDARY);
            UIManager.put("OptionPane.background", ThemeColors.SURFACE);
            UIManager.put("OptionPane.messageForeground", ThemeColors.TEXT_PRIMARY);
            UIManager.put("Button.background", ThemeColors.PRIMARY);
            UIManager.put("Button.foreground", ThemeColors.TEXT_PRIMARY);
            UIManager.put("CheckBox.background", ThemeColors.SURFACE);
            UIManager.put("CheckBox.foreground", ThemeColors.TEXT_PRIMARY);
            UIManager.put("RadioButton.background", ThemeColors.SURFACE);
            UIManager.put("RadioButton.foreground", ThemeColors.TEXT_PRIMARY);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
