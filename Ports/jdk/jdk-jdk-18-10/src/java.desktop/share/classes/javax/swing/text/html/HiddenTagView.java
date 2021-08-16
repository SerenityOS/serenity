/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text.html;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.text.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.util.*;

/**
 * HiddenTagView subclasses EditableView to contain a JTextField showing
 * the element name. When the textfield is edited the element name is
 * reset. As this inherits from EditableView if the JTextComponent is
 * not editable, the textfield will not be visible.
 *
 * @author  Scott Violet
 */
class HiddenTagView extends EditableView implements DocumentListener {
    HiddenTagView(Element e) {
        super(e);
        yAlign = 1;
    }

    protected Component createComponent() {
        JTextField tf = new JTextField(getElement().getName());
        Document doc = getDocument();
        Font font;
        if (doc instanceof StyledDocument) {
            font = ((StyledDocument)doc).getFont(getAttributes());
            tf.setFont(font);
        }
        else {
            font = tf.getFont();
        }
        tf.getDocument().addDocumentListener(this);
        updateYAlign(font);

        // Create a panel to wrap the textfield so that the textfields
        // laf border shows through.
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(null);
        if (isEndTag()) {
            panel.setBorder(EndBorder);
        }
        else {
            panel.setBorder(StartBorder);
        }
        panel.add(tf);
        return panel;
    }

    public float getAlignment(int axis) {
        if (axis == View.Y_AXIS) {
            return yAlign;
        }
        return 0.5f;
    }

    public float getMinimumSpan(int axis) {
        if (axis == View.X_AXIS && isVisible()) {
            // Default to preferred.
            return Math.max(30, super.getPreferredSpan(axis));
        }
        return super.getMinimumSpan(axis);
    }

    public float getPreferredSpan(int axis) {
        if (axis == View.X_AXIS && isVisible()) {
            return Math.max(30, super.getPreferredSpan(axis));
        }
        return super.getPreferredSpan(axis);
    }

    public float getMaximumSpan(int axis) {
        if (axis == View.X_AXIS && isVisible()) {
            // Default to preferred.
            return Math.max(30, super.getMaximumSpan(axis));
        }
        return super.getMaximumSpan(axis);
    }

    // DocumentListener methods
    public void insertUpdate(DocumentEvent e) {
        updateModelFromText();
    }

    public void removeUpdate(DocumentEvent e) {
        updateModelFromText();
    }

    public void changedUpdate(DocumentEvent e) {
        updateModelFromText();
    }

    // View method
    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        if (!isSettingAttributes) {
            setTextFromModel();
        }
    }

    // local methods

    @SuppressWarnings("deprecation")
    void updateYAlign(Font font) {
        Container c = getContainer();
        FontMetrics fm = (c != null) ? c.getFontMetrics(font) :
            Toolkit.getDefaultToolkit().getFontMetrics(font);
        float h = fm.getHeight();
        float d = fm.getDescent();
        yAlign = (h > 0) ? (h - d) / h : 0;
    }

    void resetBorder() {
        Component comp = getComponent();

        if (comp != null) {
            if (isEndTag()) {
                ((JPanel)comp).setBorder(EndBorder);
            }
            else {
                ((JPanel)comp).setBorder(StartBorder);
            }
        }
    }

    /**
     * This resets the text on the text component we created to match
     * that of the AttributeSet for the Element we represent.
     * <p>If this is invoked on the event dispatching thread, this
     * directly invokes <code>_setTextFromModel</code>, otherwise
     * <code>SwingUtilities.invokeLater</code> is used to schedule execution
     * of <code>_setTextFromModel</code>.
     */
    void setTextFromModel() {
        if (SwingUtilities.isEventDispatchThread()) {
            _setTextFromModel();
        }
        else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    _setTextFromModel();
                }
            });
        }
    }

    /**
     * This resets the text on the text component we created to match
     * that of the AttributeSet for the Element we represent.
     */
    void _setTextFromModel() {
        Document doc = getDocument();
        try {
            isSettingAttributes = true;
            if (doc instanceof AbstractDocument) {
                ((AbstractDocument)doc).readLock();
            }
            JTextComponent text = getTextComponent();
            if (text != null) {
                text.setText(getRepresentedText());
                resetBorder();
                Container host = getContainer();
                if (host != null) {
                    preferenceChanged(this, true, true);
                    host.repaint();
                }
            }
        }
        finally {
            isSettingAttributes = false;
            if (doc instanceof AbstractDocument) {
                ((AbstractDocument)doc).readUnlock();
            }
        }
    }

    /**
     * This copies the text from the text component we've created
     * to the Element's AttributeSet we represent.
     * <p>If this is invoked on the event dispatching thread, this
     * directly invokes <code>_updateModelFromText</code>, otherwise
     * <code>SwingUtilities.invokeLater</code> is used to schedule execution
     * of <code>_updateModelFromText</code>.
     */
    void updateModelFromText() {
        if (!isSettingAttributes) {
            if (SwingUtilities.isEventDispatchThread()) {
                _updateModelFromText();
            }
            else {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        _updateModelFromText();
                    }
                });
            }
        }
    }

    /**
     * This copies the text from the text component we've created
     * to the Element's AttributeSet we represent.
     */
    void _updateModelFromText() {
        Document doc = getDocument();
        Object name = getElement().getAttributes().getAttribute
            (StyleConstants.NameAttribute);
        if ((name instanceof HTML.UnknownTag) &&
            (doc instanceof StyledDocument)) {
            SimpleAttributeSet sas = new SimpleAttributeSet();
            JTextComponent textComponent = getTextComponent();
            if (textComponent != null) {
                String text = textComponent.getText();
                isSettingAttributes = true;
                try {
                    sas.addAttribute(StyleConstants.NameAttribute,
                                     new HTML.UnknownTag(text));
                    ((StyledDocument)doc).setCharacterAttributes
                        (getStartOffset(), getEndOffset() -
                         getStartOffset(), sas, false);
                }
                finally {
                    isSettingAttributes = false;
                }
            }
        }
    }

    JTextComponent getTextComponent() {
        Component comp = getComponent();

        return (comp == null) ? null : (JTextComponent)((Container)comp).
                                       getComponent(0);
    }

    String getRepresentedText() {
        String retValue = getElement().getName();
        return (retValue == null) ? "" : retValue;
    }

    boolean isEndTag() {
        AttributeSet as = getElement().getAttributes();
        if (as != null) {
            Object end = as.getAttribute(HTML.Attribute.ENDTAG);
            if (end != null && (end instanceof String) &&
                ((String)end).equals("true")) {
                return true;
            }
        }
        return false;
    }

    /** Alignment along the y axis, based on the font of the textfield. */
    float yAlign;
    /** Set to true when setting attributes. */
    boolean isSettingAttributes;


    // Following are for Borders that used for Unknown tags and comments.
    //
    // Border defines
    static final int circleR = 3;
    static final int circleD = circleR * 2;
    static final int tagSize = 6;
    static final int padding = 3;
    static final Color UnknownTagBorderColor = Color.black;
    static final Border StartBorder = new StartTagBorder();
    static final Border EndBorder = new EndTagBorder();

    @SuppressWarnings("serial") // Same-version serialization only
    static class StartTagBorder implements Border, Serializable {
        public void paintBorder(Component c, Graphics g, int x, int y,
                                int width, int height) {
            g.setColor(UnknownTagBorderColor);
            x += padding;
            width -= (padding * 2);
            g.drawLine(x, y + circleR,
                       x, y + height - circleR);
            g.drawArc(x, y + height - circleD - 1,
                      circleD, circleD, 180, 90);
            g.drawArc(x, y, circleD, circleD, 90, 90);
            g.drawLine(x + circleR, y, x + width - tagSize, y);
            g.drawLine(x + circleR, y + height - 1,
                       x + width - tagSize, y + height - 1);

            g.drawLine(x + width - tagSize, y,
                       x + width - 1, y + height / 2);
            g.drawLine(x + width - tagSize, y + height,
                       x + width - 1, y + height / 2);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(2, 2 + padding, 2, tagSize + 2 + padding);
        }

        public boolean isBorderOpaque() {
            return false;
        }
    } // End of class HiddenTagView.StartTagBorder

    @SuppressWarnings("serial") // Same-version serialization only
    static class EndTagBorder implements Border, Serializable {
        public void paintBorder(Component c, Graphics g, int x, int y,
                                int width, int height) {
            g.setColor(UnknownTagBorderColor);
            x += padding;
            width -= (padding * 2);
            g.drawLine(x + width - 1, y + circleR,
                       x + width - 1, y + height - circleR);
            g.drawArc(x + width - circleD - 1, y + height - circleD - 1,
                      circleD, circleD, 270, 90);
            g.drawArc(x + width - circleD - 1, y, circleD, circleD, 0, 90);
            g.drawLine(x + tagSize, y, x + width - circleR, y);
            g.drawLine(x + tagSize, y + height - 1,
                       x + width - circleR, y + height - 1);

            g.drawLine(x + tagSize, y,
                       x, y + height / 2);
            g.drawLine(x + tagSize, y + height,
                       x, y + height / 2);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(2, tagSize + 2 + padding, 2, 2 + padding);
        }

        public boolean isBorderOpaque() {
            return false;
        }
    } // End of class HiddenTagView.EndTagBorder


} // End of HiddenTagView
