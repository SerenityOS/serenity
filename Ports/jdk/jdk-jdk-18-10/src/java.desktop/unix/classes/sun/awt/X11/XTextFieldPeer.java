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
import java.awt.peer.*;
import java.awt.event.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.TextEvent;
import javax.swing.text.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.ComponentUI;
import javax.swing.InputMap;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import java.awt.event.MouseEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

import javax.swing.plaf.UIResource;
import javax.swing.UIDefaults;
import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.border.Border;
import com.sun.java.swing.plaf.motif.*;
import java.awt.im.InputMethodRequests;

import sun.util.logging.PlatformLogger;

import sun.awt.AWTAccessor;

final class XTextFieldPeer extends XComponentPeer implements TextFieldPeer {
    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XTextField");

    private String text;
    private final XAWTTextField xtext;
    private final boolean firstChangeSkipped;

    XTextFieldPeer(TextField target) {
        super(target);
        text = target.getText();
        xtext = new XAWTTextField(text,this, target.getParent());
        xtext.getDocument().addDocumentListener(xtext);
        xtext.setCursor(target.getCursor());
        XToolkit.specialPeerMap.put(xtext,this);

        initTextField();
        setText(target.getText());
        if (target.echoCharIsSet()) {
            setEchoChar(target.getEchoChar());
        }
        else setEchoChar((char)0);

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

        // After this line we should not change the component's text
        firstChangeSkipped = true;
        AWTAccessor.getComponentAccessor().setPeer(xtext, this);
    }

    @Override
    public void dispose() {
        XToolkit.specialPeerMap.remove(xtext);
        // visible caret has a timer thread which must be stopped
        xtext.getCaret().setVisible(false);
        super.dispose();
    }

    void initTextField() {
        setVisible(target.isVisible());

        setBounds(x, y, width, height, SET_BOUNDS);

        AWTAccessor.ComponentAccessor compAccessor = AWTAccessor.getComponentAccessor();
        foreground = compAccessor.getForeground(target);
        if (foreground == null)
            foreground = SystemColor.textText;

        setForeground(foreground);

        background = compAccessor.getBackground(target);
        if (background == null) {
            if (((TextField)target).isEditable()) background = SystemColor.text;
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
    }

    /**
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public void setEditable(boolean editable) {
        if (xtext != null) {
            xtext.setEditable(editable);
            xtext.repaint();
        }
    }

    /**
     * @see java.awt.peer.ComponentPeer
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (xtext != null) {
            xtext.setEnabled(enabled);
            xtext.repaint();
        }
    }

    /**
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public InputMethodRequests getInputMethodRequests() {
        if (xtext != null) return xtext.getInputMethodRequests();
        else  return null;

    }

    @Override
    void handleJavaInputMethodEvent(InputMethodEvent e) {
        if (xtext != null)
            xtext.processInputMethodEventImpl(e);
    }

    /**
     * @see java.awt.peer.TextFieldPeer
     */
    @Override
    public void setEchoChar(char c) {
        if (xtext != null) {
            xtext.setEchoChar(c);
            xtext.putClientProperty("JPasswordField.cutCopyAllowed",
                    xtext.echoCharIsSet() ? Boolean.FALSE : Boolean.TRUE);
        }
    }

    /**
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public int getSelectionStart() {
        return xtext.getSelectionStart();
    }

    /**
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public int getSelectionEnd() {
        return xtext.getSelectionEnd();
    }

    /**
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    @SuppressWarnings("deprecation")
    public String getText() {
        return xtext.getText();
    }

    /**
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public void setText(String text) {
        setXAWTTextField(text);
        repaint();
    }

    private void setXAWTTextField(String txt) {
        text = txt;
        if (xtext != null)  {
            // JTextField.setText() posts two different events (remove & insert).
            // Since we make no differences between text events,
            // the document listener has to be disabled while
            // JTextField.setText() is called.
            xtext.getDocument().removeDocumentListener(xtext);
            xtext.setText(txt);
            if (firstChangeSkipped) {
                postEvent(new TextEvent(target, TextEvent.TEXT_VALUE_CHANGED));
            }
            xtext.getDocument().addDocumentListener(xtext);
            xtext.setCaretPosition(0);
        }
    }

    /**
     * to be implemented.
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public void setCaretPosition(int position) {
        if (xtext != null) xtext.setCaretPosition(position);
    }

    void repaintText() {
        xtext.repaintNow();
    }

    @Override
    public void setBackground(Color c) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("target="+ target + ", old=" + background + ", new=" + c);
        }
        background = c;
        if (xtext != null) {
            if (xtext.getBackground() != c) {
                xtext.setBackground(c);
            }
            xtext.setSelectedTextColor(c);
        }
        repaintText();
    }

    @Override
    public void setForeground(Color c) {
        foreground = c;
        if (xtext != null) {
            if (xtext.getForeground() != c) {
                xtext.setForeground(foreground);
            }
            xtext.setSelectionColor(foreground);
            xtext.setCaretColor(foreground);
        }
        repaintText();
    }

    @Override
    public void setFont(Font f) {
        boolean isChanged = false;
        synchronized (getStateLock()) {
            font = f;
            if (xtext != null && xtext.getFont() != f) {
                xtext.setFont(font);
                isChanged = true;
            }
        }
        if (isChanged)
            xtext.validate();
    }

    /**
     * Deselects the highlighted text.
     */
    public void deselect() {
        int selStart=xtext.getSelectionStart();
        int selEnd=xtext.getSelectionEnd();
        if (selStart != selEnd) {
            xtext.select(selStart,selStart);
        }
    }

    /**
     * to be implemented.
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public int getCaretPosition() {
        return xtext.getCaretPosition();
    }

    /**
     * @see java.awt.peer.TextComponentPeer
     */
    @Override
    public void select(int s, int e) {
        xtext.select(s,e);
        // Fixed 5100806
        // We must take care that Swing components repainted correctly
        xtext.repaint();
    }

    @Override
    public Dimension getMinimumSize() {
        return xtext.getMinimumSize();
    }

    @Override
    public Dimension getPreferredSize() {
        return xtext.getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize(int cols) {
        return getMinimumSize(cols);
    }

    private static final int PADDING = 16;

    @Override
    public Dimension getMinimumSize(int cols) {
        Font f = xtext.getFont();
        FontMetrics fm = xtext.getFontMetrics(f);
        return new Dimension(fm.charWidth('0') * cols + 10,
                             fm.getMaxDescent() + fm.getMaxAscent() + PADDING);
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    // NOTE: This method is called by privileged threads.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    public void action(final long when, final int modifiers) {
        postEvent(new ActionEvent(target, ActionEvent.ACTION_PERFORMED,
                                  text, when,
                                  modifiers));
    }

    protected void disposeImpl() {
    }

    @Override
    public void repaint() {
        if (xtext  != null) xtext.repaint();
    }
    @Override
    void paintPeer(final Graphics g) {
        if (xtext  != null) xtext.paint(g);
    }

    @Override
    public void print(Graphics g) {
        if (xtext != null) {
            xtext.print(g);
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        super.focusLost(e);
        xtext.forwardFocusLost(e);
    }

    @Override
    public void focusGained(FocusEvent e) {
        super.focusGained(e);
        xtext.forwardFocusGained(e);
    }

    @Override
    void handleJavaKeyEvent(KeyEvent e) {
        AWTAccessor.getComponentAccessor().processEvent(xtext,e);
    }


    @Override
    public void handleJavaMouseEvent( MouseEvent mouseEvent ) {
        super.handleJavaMouseEvent(mouseEvent);
        if (xtext != null)  {
            mouseEvent.setSource(xtext);
            int id = mouseEvent.getID();
            if (id == MouseEvent.MOUSE_DRAGGED || id == MouseEvent.MOUSE_MOVED)
                xtext.processMouseMotionEventImpl(mouseEvent);
            else
                xtext.processMouseEventImpl(mouseEvent);
        }
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (xtext != null) xtext.setVisible(b);
    }

    @Override
    public void setBounds(int x, int y, int width, int height, int op) {
        super.setBounds(x, y, width, height, op);
        if (xtext != null) {
            /*
             * Fixed 6277332, 6198290:
             * the coordinates is coming (to peer): relatively to closest HW parent
             * the coordinates is setting (to textField): relatively to closest ANY parent
             * the parent of peer is target.getParent()
             * the parent of textField is the same
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
            xtext.setBounds(childX,childY,width,height);
            xtext.validate();
        }
    }

    final class AWTTextFieldUI extends MotifPasswordFieldUI {

        private JTextField jtf;

        @Override
        protected String getPropertyPrefix() {
            JTextComponent comp = getComponent();
            if (comp instanceof JPasswordField && ((JPasswordField)comp).echoCharIsSet()) {
                return "PasswordField";
            } else {
                return "TextField";
            }
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);

            jtf = (JTextField) c;

            JTextField editor = jtf;

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

            Border b = editor.getBorder();
            if ((b == null) || (b instanceof UIResource)) {
                editor.setBorder(uidefaults.getBorder(prefix + ".border"));
            }

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
            return new XTextAreaPeer.XAWTCaret();
        }
    }

    @SuppressWarnings("serial") // JDK-implementation class
    final class XAWTTextField extends JPasswordField
            implements ActionListener, DocumentListener {

        private boolean isFocused = false;
        private final XComponentPeer xwin;

        XAWTTextField(String text, XComponentPeer xwin, Container parent) {
            super(text);
            this.xwin = xwin;
            setDoubleBuffered(true);
            setFocusable(false);
            AWTAccessor.getComponentAccessor().setParent(this,parent);
            setBackground(xwin.getPeerBackground());
            setForeground(xwin.getPeerForeground());
            setFont(xwin.getPeerFont());
            setCaretPosition(0);
            addActionListener(this);
            addNotify();
        }

        @Override
        @SuppressWarnings("deprecation")
        public void actionPerformed( ActionEvent actionEvent ) {
            xwin.postEvent(
                    new ActionEvent(xwin.target, ActionEvent.ACTION_PERFORMED,
                                    getText(), actionEvent.getWhen(),
                                    actionEvent.getModifiers()));

        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            if (xwin != null) {
                xwin.postEvent(new TextEvent(xwin.target,
                                             TextEvent.TEXT_VALUE_CHANGED));
            }
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            if (xwin != null) {
                xwin.postEvent(new TextEvent(xwin.target,
                                             TextEvent.TEXT_VALUE_CHANGED));
            }
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            if (xwin != null) {
                xwin.postEvent(new TextEvent(xwin.target,
                                             TextEvent.TEXT_VALUE_CHANGED));
            }
        }

        public void repaintNow() {
            paintImmediately(getBounds());
        }

        @Override
        public Graphics getGraphics() {
            return xwin.getGraphics();
        }

        @Override
        public void updateUI() {
            ComponentUI ui = new AWTTextFieldUI();
            setUI(ui);
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

        public void processInputMethodEventImpl(InputMethodEvent e) {
            processInputMethodEvent(e);
        }

        public void processMouseEventImpl(MouseEvent e) {
            processMouseEvent(e);
        }

        public void processMouseMotionEventImpl(MouseEvent e) {
            processMouseMotionEvent(e);
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

        @Override
        public void setEchoChar(char c) {
            super.setEchoChar(c);
            ((AWTTextFieldUI)ui).installKeyboardActions();
        }
    }
}
