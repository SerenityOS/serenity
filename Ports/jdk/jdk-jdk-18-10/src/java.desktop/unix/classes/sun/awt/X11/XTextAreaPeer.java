/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.awt.X11;

import java.awt.*;
import java.awt.peer.ComponentPeer;
import java.awt.peer.TextAreaPeer;
import java.awt.event.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.JTextArea;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.plaf.ComponentUI;
import com.sun.java.swing.plaf.motif.MotifTextAreaUI;
import javax.swing.plaf.UIResource;
import javax.swing.UIDefaults;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.AbstractBorder;
import javax.swing.JButton;
import javax.swing.JViewport;
import javax.swing.InputMap;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

import javax.swing.plaf.BorderUIResource;
import java.awt.im.InputMethodRequests;
import sun.awt.AWTAccessor;
import sun.awt.SunToolkit;

final class XTextAreaPeer extends XComponentPeer implements TextAreaPeer {

    private final AWTTextPane textPane;
    private final AWTTextArea jtext;
    private final boolean firstChangeSkipped;

    private final JavaMouseEventHandler javaMouseEventHandler =
            new JavaMouseEventHandler(this);

    /**
     * Create a Text area.
     */
    XTextAreaPeer(TextArea target) {
        super(target);

        // some initializations require that target be set even
        // though init(target) has not been called
        this.target = target;

        //ComponentAccessor.enableEvents(target,AWTEvent.MOUSE_WHEEL_EVENT_MASK);

        String text = target.getText();
        jtext = new AWTTextArea(text, this);
        jtext.setWrapStyleWord(true);
        jtext.getDocument().addDocumentListener(jtext);
        XToolkit.specialPeerMap.put(jtext,this);
        textPane = new AWTTextPane(jtext,this, target.getParent());

        setBounds(x, y, width, height, SET_BOUNDS);
        textPane.setVisible(true);
        textPane.validate();

        AWTAccessor.ComponentAccessor compAccessor = AWTAccessor.getComponentAccessor();
        foreground = compAccessor.getForeground(target);
        if (foreground == null)  {
            foreground = SystemColor.textText;
        }
        setForeground(foreground);

        background = compAccessor.getBackground(target);
        if (background == null) {
            if (target.isEditable()) background = SystemColor.text;
            else background = SystemColor.control;
        }
        setBackground(background);

        if (!target.isBackgroundSet()) {
            // This is a way to set the background color of the TextArea
            // without calling setBackground - go through accessor
            compAccessor.setBackground(target, background);
        }
        if (!target.isForegroundSet()) {
            target.setForeground(SystemColor.textText);
        }

        setFont(font);

        // set the text of this object to the text of its target
        setTextImpl(target.getText());  //?? should this be setText

        int start = target.getSelectionStart();
        int end = target.getSelectionEnd();
        // Fix for 5100200
        // Restoring Motif behaviour
        // Since the end position of the selected text can be greater than the length of the text,
        // so we should set caret to max position of the text
        setCaretPosition(Math.min(end, text.length()));
        if (end > start) {
            // Should be called after setText() and setCaretPosition()
            select(start, end);
        }
        setEditable(target.isEditable());
        setScrollBarVisibility();
        // After this line we should not change the component's text
        firstChangeSkipped = true;
        compAccessor.setPeer(textPane, this);
    }

    @Override
    public void dispose() {
        XToolkit.specialPeerMap.remove(jtext);
        // visible caret has a timer thread which must be stopped
        jtext.getCaret().setVisible(false);
        jtext.removeNotify();
        super.dispose();
    }

    /*
     * The method overrides one from XComponentPeer
     * If ignoreSubComponents=={@code true} it calls super.
     * If ignoreSubComponents=={@code false} it uses the XTextArea machinery
     * to change cursor appropriately. In particular it changes the cursor to
     * default if over scrollbars.
     */
    @Override
    public void pSetCursor(Cursor cursor, boolean ignoreSubComponents) {
        if (ignoreSubComponents ||
            javaMouseEventHandler == null) {
            super.pSetCursor(cursor, true);
            return;
        }

        Point cursorPos = new Point();
        ((XGlobalCursorManager)XGlobalCursorManager.getCursorManager()).getCursorPos(cursorPos);

        final Point onScreen = getLocationOnScreen();
        Point localPoint = new Point(cursorPos.x - onScreen.x, cursorPos.y - onScreen.y );

        javaMouseEventHandler.setPointerToUnderPoint(localPoint);
        javaMouseEventHandler.setCursor();
    }

    private void setScrollBarVisibility() {
        int visibility = ((TextArea)target).getScrollbarVisibility();
        jtext.setLineWrap(false);

        if (visibility == TextArea.SCROLLBARS_NONE) {
            textPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            textPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            jtext.setLineWrap(true);
        }
        else if (visibility == TextArea.SCROLLBARS_BOTH) {

            textPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            textPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        }
        else if (visibility == TextArea.SCROLLBARS_VERTICAL_ONLY) {
            textPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            textPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            jtext.setLineWrap(true);
        }
        else if (visibility == TextArea.SCROLLBARS_HORIZONTAL_ONLY) {
            textPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            textPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        }
    }

    /**
     * Compute minimum size.
     */
    @Override
    public Dimension getMinimumSize() {
        return getMinimumSize(10, 60);
    }

    @Override
    public Dimension getPreferredSize(int rows, int cols) {
        return getMinimumSize(rows, cols);
    }

    /**
     * @see java.awt.peer.TextAreaPeer
     */
    @Override
    public Dimension getMinimumSize(int rows, int cols) {
        /*    Dimension d = null;
              if (jtext != null) {
              d = jtext.getMinimumSize(rows,cols);
              }
              return d;
        */

        int vsbwidth=0;
        int hsbheight=0;

        JScrollBar vsb = textPane.getVerticalScrollBar();
        if (vsb != null) {
            vsbwidth = vsb.getMinimumSize().width;
        }

        JScrollBar hsb = textPane.getHorizontalScrollBar();
        if (hsb != null) {
            hsbheight = hsb.getMinimumSize().height;
        }

        Font f = jtext.getFont();
        FontMetrics fm = jtext.getFontMetrics(f);

        return new Dimension(fm.charWidth('0') * cols + /*2*XMARGIN +*/ vsbwidth,
                             fm.getHeight() * rows + /*2*YMARGIN +*/ hsbheight);
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (textPane != null)
            textPane.setVisible(b);
    }

    void repaintText() {
        jtext.repaintNow();
    }

    @Override
    public void focusGained(FocusEvent e) {
        super.focusGained(e);
        jtext.forwardFocusGained(e);
    }

    @Override
    public void focusLost(FocusEvent e) {
        super.focusLost(e);
        jtext.forwardFocusLost(e);
    }

    /**
     * Paint the component
     * this method is called when the repaint instruction has been used
     */
    @Override
    public void repaint() {
        if (textPane  != null)  {
            //textPane.validate();
            textPane.repaint();
        }
    }

    @Override
    void paintPeer(final Graphics g) {
        if (textPane  != null)  {
            textPane.paint(g);
        }
    }

    @Override
    public void setBounds(int x, int y, int width, int height, int op) {
        super.setBounds(x, y, width, height, op);
        if (textPane != null) {
            /*
             * Fixed 6277332, 6198290:
             * the coordinates is coming (to peer): relatively to closest HW parent
             * the coordinates is setting (to textPane): relatively to closest ANY parent
             * the parent of peer is target.getParent()
             * the parent of textPane is the same
             * see 6277332, 6198290 for more information
             */
            int childX = x;
            int childY = y;
            Component parent = target.getParent();
            // we up to heavyweight parent in order to be sure
            // that the coordinates of the text pane is relatively to closest parent
            while (parent.isLightweight()){
                childX -= parent.getX();
                childY -= parent.getY();
                parent = parent.getParent();
            }
            textPane.setBounds(childX,childY,width,height);
            textPane.validate();
        }
    }

    @Override
    void handleJavaKeyEvent(KeyEvent e) {
        AWTAccessor.getComponentAccessor().processEvent(jtext,e);
    }

    @Override
    public boolean handlesWheelScrolling() { return true; }

    @Override
    void handleJavaMouseWheelEvent(MouseWheelEvent e) {
        AWTAccessor.getComponentAccessor().processEvent(textPane, e);
    }

    @Override
    public void handleJavaMouseEvent( MouseEvent e ) {
        super.handleJavaMouseEvent( e );
        javaMouseEventHandler.handle( e );
    }

    @Override
    void handleJavaInputMethodEvent(InputMethodEvent e) {
        if (jtext != null)
            jtext.processInputMethodEventPublic(e);
    }

    /**
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public void select(int s, int e) {
        jtext.select(s, e);
        // Fixed 5100806
        // We must take care that Swing components repainted correctly
        jtext.repaint();
    }

    @Override
    public void setBackground(Color c) {
        super.setBackground(c);
//          synchronized (getStateLock()) {
//              background = c;
//          }
        if (jtext != null) {
            jtext.setBackground(c);
            jtext.setSelectedTextColor(c);
        }
//          repaintText();
    }

    @Override
    public void setForeground(Color c) {
        super.setForeground(c);
//          synchronized (getStateLock()) {
//              foreground = c;
//          }
        if (jtext != null) {
            jtext.setForeground(foreground);
            jtext.setSelectionColor(foreground);
            jtext.setCaretColor(foreground);
        }
//          repaintText();
    }

    @Override
    public void setFont(Font f) {
        super.setFont(f);
//          synchronized (getStateLock()) {
//              font = f;
//          }
        if (jtext != null) {
            jtext.setFont(font);
        }
        textPane.validate();
    }

    /**
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public void setEditable(boolean editable) {
        if (jtext != null) jtext.setEditable(editable);
        repaintText();
    }

    /**
     * @see java.awt.peer.ComponentPeer
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (jtext != null) {
            jtext.setEnabled(enabled);
            jtext.repaint();
        }
    }

    /**
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public InputMethodRequests getInputMethodRequests() {
        if (jtext != null) return jtext.getInputMethodRequests();
        else  return null;
    }

    /**
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public int getSelectionStart() {
        return jtext.getSelectionStart();
    }

    /**
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public int getSelectionEnd() {
        return jtext.getSelectionEnd();
    }

    /**
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public String getText() {
        return jtext.getText();
    }

    /**
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public void setText(String text) {
        setTextImpl(text);
        repaintText();
    }

    private void setTextImpl(String txt) {
        if (jtext != null) {
            // JTextArea.setText() posts two different events (remove & insert).
            // Since we make no differences between text events,
            // the document listener has to be disabled while
            // JTextArea.setText() is called.
            jtext.getDocument().removeDocumentListener(jtext);
            jtext.setText(txt);
            if (firstChangeSkipped) {
                postEvent(new TextEvent(target, TextEvent.TEXT_VALUE_CHANGED));
            }
            jtext.getDocument().addDocumentListener(jtext);
        }
    }

    /**
     * insert the text "txt on position "pos" in the array lines
     * @see java.awt.peer.TextAreaPeer
     */
    @Override
    public void insert(String txt, int p) {
        if (jtext != null) {
            boolean doScroll = (p >= jtext.getDocument().getLength() && jtext.getDocument().getLength() != 0);
            jtext.insert(txt,p);
            textPane.validate();
            if (doScroll) {
                JScrollBar bar = textPane.getVerticalScrollBar();
                if (bar != null) {
                    bar.setValue(bar.getMaximum()-bar.getVisibleAmount());
                }
            }
        }
    }

    /**
     * replace the text between the position "s" and "e" with "txt"
     * @see java.awt.peer.TextAreaPeer
     */
    @Override
    public void replaceRange(String txt, int s, int e) {
        if (jtext != null) {
            // JTextArea.replaceRange() posts two different events.
            // Since we make no differences between text events,
            // the document listener has to be disabled while
            // JTextArea.replaceRange() is called.
            jtext.getDocument().removeDocumentListener(jtext);
            jtext.replaceRange(txt, s, e);
            postEvent(new TextEvent(target, TextEvent.TEXT_VALUE_CHANGED));
            jtext.getDocument().addDocumentListener(jtext);
        }
    }

    /**
     * to be implemented.
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public void setCaretPosition(int position) {
        jtext.setCaretPosition(position);
    }

    /**
     * to be implemented.
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public int getCaretPosition() {
        return jtext.getCaretPosition();
    }

    final class AWTTextAreaUI extends MotifTextAreaUI {

        private JTextArea jta;

        @Override
        protected String getPropertyPrefix() { return "TextArea"; }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);

            jta = (JTextArea) c;

            JTextArea editor = jta;

            UIDefaults uidefaults = XToolkit.getUIDefaults();

            String prefix = getPropertyPrefix();
            Font f = editor.getFont();
            if ((f == null) || (f instanceof UIResource)) {
                editor.setFont(uidefaults.getFont(prefix + ".font"));
            }

            Color bg = editor.getBackground();
            if ((bg == null) || (bg instanceof UIResource)) {
                editor.setBackground(uidefaults.getColor(prefix + ".background"));
            }

            Color fg = editor.getForeground();
            if ((fg == null) || (fg instanceof UIResource)) {
                editor.setForeground(uidefaults.getColor(prefix + ".foreground"));
            }

            Color color = editor.getCaretColor();
            if ((color == null) || (color instanceof UIResource)) {
                editor.setCaretColor(uidefaults.getColor(prefix + ".caretForeground"));
            }

            Color s = editor.getSelectionColor();
            if ((s == null) || (s instanceof UIResource)) {
                editor.setSelectionColor(uidefaults.getColor(prefix + ".selectionBackground"));
            }

            Color sfg = editor.getSelectedTextColor();
            if ((sfg == null) || (sfg instanceof UIResource)) {
                editor.setSelectedTextColor(uidefaults.getColor(prefix + ".selectionForeground"));
            }

            Color dfg = editor.getDisabledTextColor();
            if ((dfg == null) || (dfg instanceof UIResource)) {
                editor.setDisabledTextColor(uidefaults.getColor(prefix + ".inactiveForeground"));
            }

            Border b = new BevelBorder(false,SystemColor.controlDkShadow,SystemColor.controlLtHighlight);
            editor.setBorder(new BorderUIResource.CompoundBorderUIResource(
                b,new EmptyBorder(2, 2, 2, 2)));

            Insets margin = editor.getMargin();
            if (margin == null || margin instanceof UIResource) {
                editor.setMargin(uidefaults.getInsets(prefix + ".margin"));
            }
        }

        @Override
        protected void installKeyboardActions() {
            super.installKeyboardActions();

            JTextComponent comp = getComponent();

            UIDefaults uidefaults = XToolkit.getUIDefaults();

            String prefix = getPropertyPrefix();

            InputMap map = (InputMap)uidefaults.get(prefix + ".focusInputMap");

            if (map != null) {
                SwingUtilities.replaceUIInputMap(comp, JComponent.WHEN_FOCUSED,
                                                 map);
            }
        }

        @Override
        protected Caret createCaret() {
            return new XAWTCaret();
        }
    }

    @SuppressWarnings("serial") // JDK-implementation class
    static final class XAWTCaret extends DefaultCaret {
        @Override
        public void focusGained(FocusEvent e) {
            super.focusGained(e);
            if (getComponent().isEnabled()){
                // Make sure the cursor is visible in case of non-editable TextArea
                super.setVisible(true);
            }
            getComponent().repaint();
        }

        @Override
        public void focusLost(FocusEvent e) {
            super.focusLost(e);
            getComponent().repaint();
        }
    }

    @SuppressWarnings("serial") // JDK-implementation class
    final class XAWTScrollBarButton extends BasicArrowButton {

        private UIDefaults uidefaults = XToolkit.getUIDefaults();
        private Color darkShadow = SystemColor.controlShadow;
        private Color lightShadow = SystemColor.controlLtHighlight;
        private Color buttonBack = uidefaults.getColor("ScrollBar.track");

        XAWTScrollBarButton(int direction) {
            super(direction);

            switch (direction) {
            case NORTH:
            case SOUTH:
            case EAST:
            case WEST:
                this.direction = direction;
                break;
            default:
                throw new IllegalArgumentException("invalid direction");
            }

            setRequestFocusEnabled(false);
            setOpaque(true);
            setBackground(uidefaults.getColor("ScrollBar.thumb"));
            setForeground(uidefaults.getColor("ScrollBar.foreground"));
        }

        @Override
        public Dimension getPreferredSize() {
            switch (direction) {
            case NORTH:
            case SOUTH:
                return new Dimension(11, 12);
            case EAST:
            case WEST:
            default:
                return new Dimension(12, 11);
            }
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        public boolean isFocusTraversable() {
            return false;
        }

        @Override
        public void paint(Graphics g)
        {
            int w = getWidth();
            int h = getHeight();

            if (isOpaque()) {
                g.setColor(buttonBack);
                g.fillRect(0, 0, w, h);
            }

            boolean isPressed = getModel().isPressed();
            Color lead = (isPressed) ? darkShadow : lightShadow;
            Color trail = (isPressed) ? lightShadow : darkShadow;
            Color fill = getBackground();

            int cx = w / 2;
            int cy = h / 2;
            int s = Math.min(w, h);

            switch (direction) {
            case NORTH:
                g.setColor(lead);
                g.drawLine(cx, 0, cx, 0);
                for (int x = cx - 1, y = 1, dx = 1; y <= s - 2; y += 2) {
                    g.setColor(lead);
                    g.drawLine(x, y, x, y);
                    if (y >= (s - 2)) {
                        g.drawLine(x, y + 1, x, y + 1);
                    }
                    g.setColor(fill);
                    g.drawLine(x + 1, y, x + dx, y);
                    if (y < (s - 2)) {
                        g.drawLine(x, y + 1, x + dx + 1, y + 1);
                    }
                    g.setColor(trail);
                    g.drawLine(x + dx + 1, y, x + dx + 1, y);
                    if (y >= (s - 2)) {
                        g.drawLine(x + 1, y + 1, x + dx + 1, y + 1);
                    }
                    dx += 2;
                    x -= 1;
                }
                break;

            case SOUTH:
                g.setColor(trail);
                g.drawLine(cx, s, cx, s);
                for (int x = cx - 1, y = s - 1, dx = 1; y >= 1; y -= 2) {
                    g.setColor(lead);
                    g.drawLine(x, y, x, y);
                    if (y <= 2) {
                        g.drawLine(x, y - 1, x + dx + 1, y - 1);
                    }
                    g.setColor(fill);
                    g.drawLine(x + 1, y, x + dx, y);
                    if (y > 2) {
                        g.drawLine(x, y - 1, x + dx + 1, y - 1);
                    }
                    g.setColor(trail);
                    g.drawLine(x + dx + 1, y, x + dx + 1, y);

                    dx += 2;
                    x -= 1;
                }
                break;

            case EAST:
                g.setColor(lead);
                g.drawLine(s, cy, s, cy);
                for (int y = cy - 1, x = s - 1, dy = 1; x >= 1; x -= 2) {
                    g.setColor(lead);
                    g.drawLine(x, y, x, y);
                    if (x <= 2) {
                        g.drawLine(x - 1, y, x - 1, y + dy + 1);
                    }
                    g.setColor(fill);
                    g.drawLine(x, y + 1, x, y + dy);
                    if (x > 2) {
                        g.drawLine(x - 1, y, x - 1, y + dy + 1);
                    }
                    g.setColor(trail);
                    g.drawLine(x, y + dy + 1, x, y + dy + 1);

                    dy += 2;
                    y -= 1;
                }
                break;

            case WEST:
                g.setColor(trail);
                g.drawLine(0, cy, 0, cy);
                for (int y = cy - 1, x = 1, dy = 1; x <= s - 2; x += 2) {
                    g.setColor(lead);
                    g.drawLine(x, y, x, y);
                    if (x >= (s - 2)) {
                        g.drawLine(x + 1, y, x + 1, y);
                    }
                    g.setColor(fill);
                    g.drawLine(x, y + 1, x, y + dy);
                    if (x < (s - 2)) {
                        g.drawLine(x + 1, y, x + 1, y + dy + 1);
                    }
                    g.setColor(trail);
                    g.drawLine(x, y + dy + 1, x, y + dy + 1);
                    if (x >= (s - 2)) {
                        g.drawLine(x + 1, y + 1, x + 1, y + dy + 1);
                    }
                    dy += 2;
                    y -= 1;
                }
                break;
            }
        }
    }

    final class XAWTScrollBarUI extends BasicScrollBarUI {

        @Override
        protected void installDefaults()
        {
            super.installDefaults();
            scrollbar.setBorder(new BevelBorder(false,SystemColor.controlDkShadow,SystemColor.controlLtHighlight) );
        }

        @Override
        protected void configureScrollBarColors() {
            UIDefaults uidefaults = XToolkit.getUIDefaults();
            Color bg = scrollbar.getBackground();
            if (bg == null || bg instanceof UIResource) {
                scrollbar.setBackground(uidefaults.getColor("ScrollBar.background"));
            }

            Color fg = scrollbar.getForeground();
            if (fg == null || fg instanceof UIResource) {
                scrollbar.setForeground(uidefaults.getColor("ScrollBar.foreground"));
            }

            thumbHighlightColor = uidefaults.getColor("ScrollBar.thumbHighlight");
            thumbLightShadowColor = uidefaults.getColor("ScrollBar.thumbShadow");
            thumbDarkShadowColor = uidefaults.getColor("ScrollBar.thumbDarkShadow");
            thumbColor = uidefaults.getColor("ScrollBar.thumb");
            trackColor = uidefaults.getColor("ScrollBar.track");

            trackHighlightColor = uidefaults.getColor("ScrollBar.trackHighlight");

        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            JButton b = new XAWTScrollBarButton(orientation);
            return b;

        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            JButton b = new XAWTScrollBarButton(orientation);
            return b;
        }

        public JButton getDecreaseButton(){
            return decrButton;
        }

        public JButton getIncreaseButton(){
            return incrButton;
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            paintTrack(g, c, getTrackBounds());
            Rectangle thumbBounds = getThumbBounds();
            paintThumb(g, c, thumbBounds);
        }

        @Override
        public void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds)
        {
            if(!scrollbar.isEnabled()) {
                return;
            }

            if (thumbBounds.isEmpty())
                thumbBounds = getTrackBounds();

            int w = thumbBounds.width;
            int h = thumbBounds.height;

            g.translate(thumbBounds.x, thumbBounds.y);
            g.setColor(thumbColor);
            g.fillRect(0, 0, w-1, h-1);

            g.setColor(thumbHighlightColor);
            g.drawLine(0, 0, 0, h-1);
            g.drawLine(1, 0, w-1, 0);

            g.setColor(thumbLightShadowColor);
            g.drawLine(1, h-1, w-1, h-1);
            g.drawLine(w-1, 1, w-1, h-2);

            g.translate(-thumbBounds.x, -thumbBounds.y);
        }
    }

    @SuppressWarnings("serial") // JDK-implementation class
    final class AWTTextArea extends JTextArea implements DocumentListener {

        private boolean isFocused = false;
        private final XTextAreaPeer peer;

        AWTTextArea(String text, XTextAreaPeer peer) {
            super(text);
            setFocusable(false);
            this.peer = peer;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            if (peer != null) {
                peer.postEvent(new TextEvent(peer.target,
                                             TextEvent.TEXT_VALUE_CHANGED));
            }
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            if (peer != null) {
                peer.postEvent(new TextEvent(peer.target,
                                             TextEvent.TEXT_VALUE_CHANGED));
            }
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            if (peer != null) {
                peer.postEvent(new TextEvent(peer.target,
                                             TextEvent.TEXT_VALUE_CHANGED));
            }
        }

        void forwardFocusGained( FocusEvent e) {
            isFocused = true;
            FocusEvent fe = new FocusEvent(this, e.getID(), e.isTemporary(),
                    e.getOppositeComponent(), e.getCause());
            super.processFocusEvent(fe);
        }


        void forwardFocusLost( FocusEvent e) {
            isFocused = false;
            FocusEvent fe = new FocusEvent(this, e.getID(), e.isTemporary(),
                    e.getOppositeComponent(), e.getCause());
            super.processFocusEvent(fe);
        }

        @Override
        public boolean hasFocus() {
            return isFocused;
        }

        public void repaintNow() {
            paintImmediately(getBounds());
        }

        public void processMouseEventPublic(MouseEvent e) {
            processMouseEvent(e);
        }

        public void processMouseMotionEventPublic(MouseEvent e) {
            processMouseMotionEvent(e);
        }

        public void processInputMethodEventPublic(InputMethodEvent e) {
            processInputMethodEvent(e);
        }

        @Override
        public void updateUI() {
            ComponentUI ui = new AWTTextAreaUI();
            setUI(ui);
        }

        @Override
        public void setTransferHandler(final TransferHandler newHandler) {
            // override the default implementation to avoid loading
            // SystemFlavorMap and associated classes
            Object key = AWTAccessor.getClientPropertyKeyAccessor()
                                    .getJComponent_TRANSFER_HANDLER();
            Object oldHandler = getClientProperty(key);
            putClientProperty(key, newHandler);
            firePropertyChange("transferHandler", oldHandler, newHandler);
        }
    }

    final class XAWTScrollPaneUI extends BasicScrollPaneUI {

        private final Border vsbMarginBorderR = new EmptyBorder(0, 2, 0, 0);
        private final Border vsbMarginBorderL = new EmptyBorder(0, 0, 0, 2);
        private final Border hsbMarginBorder = new EmptyBorder(2, 0, 0, 0);

        private Border vsbBorder;
        private Border hsbBorder;

        private PropertyChangeListener propertyChangeHandler;

        @Override
        protected void installListeners(JScrollPane scrollPane) {
            super.installListeners(scrollPane);
            propertyChangeHandler = createPropertyChangeHandler();
            scrollPane.addPropertyChangeListener(propertyChangeHandler);
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            Border vpBorder = scrollpane.getViewportBorder();
            if (vpBorder != null) {
                Rectangle r = scrollpane.getViewportBorderBounds();
                vpBorder.paintBorder(scrollpane, g, r.x, r.y, r.width, r.height);
            }
        }

        @Override
        protected void uninstallListeners(JComponent scrollPane) {
            super.uninstallListeners(scrollPane);
            scrollPane.removePropertyChangeListener(propertyChangeHandler);
        }

        private PropertyChangeListener createPropertyChangeHandler() {
            return new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent e) {
                        String propertyName = e.getPropertyName();

                        if (propertyName.equals("componentOrientation")) {
                            JScrollPane pane = (JScrollPane)e.getSource();
                            JScrollBar vsb = pane.getVerticalScrollBar();
                            if (vsb != null) {
                                if (isLeftToRight(pane)) {
                                    vsbBorder = new CompoundBorder(new EmptyBorder(0, 4, 0, -4),
                                                                   vsb.getBorder());
                                } else {
                                    vsbBorder = new CompoundBorder(new EmptyBorder(0, -4, 0, 4),
                                                                   vsb.getBorder());
                                }
                                vsb.setBorder(vsbBorder);
                            }
                        }
                    }};
        }

        boolean isLeftToRight( Component c ) {
            return c.getComponentOrientation().isLeftToRight();
        }

        @Override
        protected void installDefaults(JScrollPane scrollpane) {
            Border b = scrollpane.getBorder();
            UIDefaults uidefaults = XToolkit.getUIDefaults();
            scrollpane.setBorder(uidefaults.getBorder("ScrollPane.border"));
            scrollpane.setBackground(uidefaults.getColor("ScrollPane.background"));
            scrollpane.setViewportBorder(uidefaults.getBorder("TextField.border"));
            JScrollBar vsb = scrollpane.getVerticalScrollBar();
            if (vsb != null) {
                if (isLeftToRight(scrollpane)) {
                    vsbBorder = new CompoundBorder(vsbMarginBorderR,
                                                   vsb.getBorder());
                }
                else {
                    vsbBorder = new CompoundBorder(vsbMarginBorderL,
                                                   vsb.getBorder());
                }
                vsb.setBorder(vsbBorder);
            }

            JScrollBar hsb = scrollpane.getHorizontalScrollBar();
            if (hsb != null) {
                hsbBorder = new CompoundBorder(hsbMarginBorder, hsb.getBorder());
                hsb.setBorder(hsbBorder);
            }
        }

        @Override
        protected void uninstallDefaults(JScrollPane c) {
            super.uninstallDefaults(c);

            JScrollBar vsb = scrollpane.getVerticalScrollBar();
            if (vsb != null) {
                if (vsb.getBorder() == vsbBorder) {
                    vsb.setBorder(null);
                }
                vsbBorder = null;
            }

            JScrollBar hsb = scrollpane.getHorizontalScrollBar();
            if (hsb != null) {
                if (hsb.getBorder() == hsbBorder) {
                    hsb.setBorder(null);
                }
                hsbBorder = null;
            }
        }
    }

    @SuppressWarnings("serial") // JDK-implementation class
    private class AWTTextPane extends JScrollPane implements FocusListener {

        private final JTextArea jtext;
        private final XWindow xwin;

        private final Color control = SystemColor.control;
        private final Color focus = SystemColor.activeCaptionBorder;

        AWTTextPane(JTextArea jt, XWindow xwin, Container parent) {
            super(jt);
            this.xwin = xwin;
            setDoubleBuffered(true);
            jt.addFocusListener(this);
            AWTAccessor.getComponentAccessor().setParent(this,parent);
            setViewportBorder(new BevelBorder(false,SystemColor.controlDkShadow,SystemColor.controlLtHighlight) );
            this.jtext = jt;
            setFocusable(false);
            addNotify();
        }

        @Override
        public void invalidate() {
            synchronized (getTreeLock()) {
                final Container parent = getParent();
                AWTAccessor.getComponentAccessor().setParent(this, null);
                try {
                    super.invalidate();
                } finally {
                    AWTAccessor.getComponentAccessor().setParent(this, parent);
                }
            }
        }

        @Override
        public void focusGained(FocusEvent e) {
            Graphics g = getGraphics();
            Rectangle r = getViewportBorderBounds();
            g.setColor(focus);
            g.drawRect(r.x,r.y,r.width,r.height);
            g.dispose();
        }

        @Override
        public void focusLost(FocusEvent e) {
            Graphics g = getGraphics();
            Rectangle r = getViewportBorderBounds();
            g.setColor(control);
            g.drawRect(r.x,r.y,r.width,r.height);
            g.dispose();
        }

        public Window getRealParent() {
            return (Window) xwin.target;
        }

        @Override
        public void updateUI() {
            ComponentUI ui = new XAWTScrollPaneUI();
            setUI(ui);
        }

        @Override
        public JScrollBar createVerticalScrollBar() {
            return new XAWTScrollBar(JScrollBar.VERTICAL);
        }

        @Override
        public JScrollBar createHorizontalScrollBar() {
            return new XAWTScrollBar(JScrollBar.HORIZONTAL);
        }

        public JTextArea getTextArea () {
            return this.jtext;
        }

        @Override
        public Graphics getGraphics() {
            return xwin.getGraphics();
        }

        @SuppressWarnings("serial") // JDK-implementation class
        final class XAWTScrollBar extends ScrollBar {

            XAWTScrollBar(int i) {
                super(i);
                setFocusable(false);
            }

            @Override
            public void updateUI() {
                ComponentUI ui = new XAWTScrollBarUI();
                setUI(ui);
            }
        }
    }

    @SuppressWarnings("serial") // JDK-implementation class
    static class BevelBorder extends AbstractBorder implements UIResource {
        private Color darkShadow = SystemColor.controlDkShadow;
        private Color lightShadow = SystemColor.controlLtHighlight;
        private Color control = SystemColor.controlShadow;
        private boolean isRaised;

        BevelBorder(boolean isRaised, Color darkShadow, Color lightShadow) {
            this.isRaised = isRaised;
            this.darkShadow = darkShadow;
            this.lightShadow = lightShadow;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor((isRaised) ? lightShadow : darkShadow);
            g.drawLine(x, y, x+w-1, y);           // top
            g.drawLine(x, y+h-1, x, y+1);         // left

            g.setColor(control);
            g.drawLine(x+1, y+1, x+w-2, y+1);           // top
            g.drawLine(x+1, y+h-1, x+1, y+1);         // left

            g.setColor((isRaised) ? darkShadow : lightShadow);
            g.drawLine(x+1, y+h-1, x+w-1, y+h-1); // bottom
            g.drawLine(x+w-1, y+h-1, x+w-1, y+1); // right

            g.setColor(control);
            g.drawLine(x+1, y+h-2, x+w-2, y+h-2); // bottom
            g.drawLine(x+w-2, y+h-2, x+w-2, y+1); // right
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return getBorderInsets(c, new Insets(0,0,0,0));
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = insets.left = insets.bottom = insets.right = 2;
            return insets;
        }

        public boolean isOpaque(Component c) {
            return true;
        }
    }


    // This class dispatches 'MouseEvent's to 'XTextAreaPeer''s (hidden)
    // subcomponents, and overrides mouse cursor, e.g. for scrollbars.
    //
    // However, current dispatching is a kind of fake, and is tuned to do only
    // what is necessary/possible. E.g. no additional mouse-exited/entered
    // events are generated, when mouse exits scrollbar and enters viewport
    // with JTextArea inside. Actually, no events are ever generated here (for
    // now). They are only dispatched as correctly as possible/neccessary.
    //
    // In future, it would be better to replace fake-emulation of grab-detection
    // and event-dispatching here, by reusing some common implementation of this
    // functionality. Mouse-cursor setting should also be freed of hacked
    // overloading here.

    private static final class JavaMouseEventHandler {
        private final XTextAreaPeer outer;
        private final Pointer current = new Pointer();
        private boolean grabbed = false;

        JavaMouseEventHandler( XTextAreaPeer outer ) {
            this.outer = outer;
        }


        // 1. We can make grab-tracking emulation here more robust to variations in
        //    in mouse-events order and consistence. E.g. by using such code:
        //    if( grabbed && event.getID()==MouseEvent.MOUSE_MOVED ) grabbed = false;
        //    Or we can also use 'assert'ions.
        // 2. WARNING: Currently, while grab-detection mechanism _here_ says, that
        //    grab is in progress, we do not update 'current'.  In case 'current'
        //    is set to a scrollbar or to a scroll-button, then references to their
        //    'Component'-instances are "remembered". And events are dispatched to
        //    these remembered components, without checking, if XTextAreaPeer has
        //    replaced these instances with another ones. This also aplies to
        //    mouse-drags-from-outside (see comment in 'grabbed_update' method).

        void handle( MouseEvent event ) {
            if ( ! grabbed ) {
                // dispatch() needs up-to-date pointer in ungrabbed case.
                setPointerToUnderPoint( event.getPoint() );
            }
            dispatch( event );
            boolean wasGrabbed = grabbed;
            grabbed_update( event );
            if ( wasGrabbed && ! grabbed ) {
                setPointerToUnderPoint( event.getPoint() );
            }
            setCursor();
        }

        // Following is internally private:

        // Here dispatching is performed, of 'MouseEvent's to (some)
        // 'XTextAreaPeer''s (hidden) subcomponents.
        private void dispatch( MouseEvent event ) {
            switch( current.getType() )
            {
                case TEXT:
                    Point point = toViewportChildLocalSpace(
                        outer.textPane.getViewport(), event.getPoint() );
                    XTextAreaPeer.AWTTextArea jtext = outer.jtext;
                    MouseEvent newEvent = newMouseEvent( jtext, point, event );
                    int id = newEvent.getID();
                    if ( id==MouseEvent.MOUSE_MOVED || id==MouseEvent.MOUSE_DRAGGED ) {
                        jtext.processMouseMotionEventPublic( newEvent );
                    } else {
                        jtext.processMouseEventPublic( newEvent );
                    }
                    break;

                // We perform (additional) dispatching of events to buttons of
                // scrollbar, instead of leaving it to JScrollbar. This is
                // required, because of different listeners in Swing and AWT,
                // which trigger scrolling (ArrowButtonListener vs. TrackListener,
                // accordingly). So we dispatch events to scroll-buttons, to
                // invoke a correct Swing button listener.
                // See CR 6175401 for more information.
                case BAR:
                case BUTTON:
                    Component c = current.getBar();
                    Point p = toLocalSpace( c, event.getPoint() );
                    if ( current.getType()==Pointer.Type.BUTTON ) {
                        c = current.getButton();
                        p = toLocalSpace( c, p );
                    }
                    AWTAccessor.getComponentAccessor().processEvent( c, newMouseEvent( c, p, event ) );
                    break;
            }
        }

        @SuppressWarnings("deprecation")
        private static MouseEvent newMouseEvent(
            Component source, Point point, MouseEvent template )
        {
            MouseEvent e = template;
            MouseEvent nme = new MouseEvent(
                source,
                e.getID(), e.getWhen(),
                e.getModifiersEx() | e.getModifiers(),
                point.x, point.y,
                e.getXOnScreen(), e.getYOnScreen(),
                e.getClickCount(), e.isPopupTrigger(), e.getButton() );
            // Because these MouseEvents are dispatched directly to
            // their target, we need to mark them as being
            // system-generated here
            SunToolkit.setSystemGenerated(nme);
            return nme;
        }

        private void setCursor() {
            if ( current.getType()==Pointer.Type.TEXT ) {
                // 'target.getCursor()' is also applied from elsewhere
                // (at least now), but only when mouse "entered", and
                // before 'XTextAreaPeer.handleJavaMouseEvent' is invoked.
                outer.pSetCursor( outer.target.getCursor(), true );
            }
            else {
                // We can write here a more intelligent cursor selection
                // mechanism, like getting cursor from 'current' component.
                // However, I see no point in doing so now. But if you feel
                // like implementing it, you'll probably need to introduce
                // 'Pointer.Type.PANEL'.
                outer.pSetCursor( outer.textPane.getCursor(), true );
            }
        }


        // Current way of grab-detection causes interesting (but harmless)
        // side-effect. If mouse is draged from outside to inside of TextArea,
        // we will then (in some cases) be asked to dispatch mouse-entered/exited
        // events. But, as at least one mouse-button is down, we will detect
        // grab-mode is on (though the grab isn't ours).
        //
        // Thus, we will not update 'current' (see 'handle' method), and will
        // dispatch events to the last subcomponent, the 'current' was set to.
        // As always, we set cursor in this case also. But, all this seems
        // harmless, because mouse entered/exited events seem to have no effect
        // here, and cursor setting is ignored in case of drags from outside.
        //
        // Grab-detection can be further improved, e.g. by taking into account
        // current event-ID, but I see not point in doing it now.

        private void grabbed_update( MouseEvent event ) {
            final int allButtonsMask
                = MouseEvent.BUTTON1_DOWN_MASK
                | MouseEvent.BUTTON2_DOWN_MASK
                | MouseEvent.BUTTON3_DOWN_MASK;
            grabbed = ( (event.getModifiersEx() & allButtonsMask) != 0 );
        }

        // 'toLocalSpace' and 'toViewportChildLocalSpace' can be "optimized" to
        // 'return' 'void' and use 'Point' input-argument also as output.
        private static Point toLocalSpace( Component local, Point inParentSpace )
        {
            Point p = inParentSpace;
            Point l = local.getLocation();
            return new Point( p.x - l.x, p.y - l.y );
        }
        private static Point toViewportChildLocalSpace( JViewport v, Point inViewportParentSpace )
        {
            Point l = toLocalSpace(v, inViewportParentSpace);
            Point p = v.getViewPosition();
            l.x += p.x;
            l.y += p.y;
            return l;
        }

        private void setPointerToUnderPoint( Point point ) {
            if ( outer.textPane.getViewport().getBounds().contains( point ) ) {
                current.setText();
            }
            else if ( ! setPointerIfPointOverScrollbar(
                outer.textPane.getVerticalScrollBar(), point ) )
            {
                if ( ! setPointerIfPointOverScrollbar(
                    outer.textPane.getHorizontalScrollBar(), point ) )
                {
                    current.setNone();
                }
            }
        }

        private boolean setPointerIfPointOverScrollbar( JScrollBar bar, Point point ) {
            if ( ! bar.getBounds().contains( point ) ) {
                return false;
            }
            current.setBar( bar );
            Point local = toLocalSpace( bar, point );

            XTextAreaPeer.XAWTScrollBarUI ui =
                (XTextAreaPeer.XAWTScrollBarUI) bar.getUI();

            if ( ! setPointerIfPointOverButton( ui.getIncreaseButton(), local ) ) {
                setPointerIfPointOverButton( ui.getDecreaseButton(), local );
            }

            return true;
        }

        private boolean setPointerIfPointOverButton( JButton button, Point point ) {
            if ( ! button.getBounds().contains( point ) ) {
                return false;
            }
            current.setButton( button );
            return true;
        }

        private static final class Pointer {
            static enum Type {
                NONE, TEXT, BAR, BUTTON  // , PANEL
            }
            Type getType() {
                return type;
            }
            boolean isNone() {
                return type==Type.NONE;
            }
            JScrollBar getBar() {
                boolean ok = type==Type.BAR || type==Type.BUTTON;
                assert ok;
                return ok ? bar : null;
            }
            JButton getButton() {
                boolean ok = type==Type.BUTTON;
                assert ok;
                return ok ? button : null;
            }
            void setNone() {
                type = Type.NONE;
            }
            void setText() {
                type = Type.TEXT;
            }
            void setBar( JScrollBar bar ) {
                this.bar=bar;
                type=Type.BAR;
            }
            void setButton( JButton button ) {
                this.button=button;
                type=Type.BUTTON;
            }

            private Type type;
            private JScrollBar bar;
            private JButton button;
        }
    }
}
