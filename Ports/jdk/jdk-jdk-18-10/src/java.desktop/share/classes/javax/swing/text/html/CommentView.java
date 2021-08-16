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
 * CommentView subclasses HiddenTagView to contain a JTextArea showing
 * a comment. When the textarea is edited the comment is
 * reset. As this inherits from EditableView if the JTextComponent is
 * not editable, the textarea will not be visible.
 *
 * @author  Scott Violet
 */
class CommentView extends HiddenTagView {
    CommentView(Element e) {
        super(e);
    }

    protected Component createComponent() {
        Container host = getContainer();
        if (host != null && !((JTextComponent)host).isEditable()) {
            return null;
        }
        JTextArea ta = new JTextArea(getRepresentedText());
        Document doc = getDocument();
        Font font;
        if (doc instanceof StyledDocument) {
            font = ((StyledDocument)doc).getFont(getAttributes());
            ta.setFont(font);
        }
        else {
            font = ta.getFont();
        }
        updateYAlign(font);
        ta.setBorder(CBorder);
        ta.getDocument().addDocumentListener(this);
        ta.setFocusable(isVisible());
        return ta;
    }

    void resetBorder() {
    }

    /**
     * This is subclassed to put the text on the Comment attribute of
     * the Element's AttributeSet.
     */
    void _updateModelFromText() {
        JTextComponent textC = getTextComponent();
        Document doc = getDocument();
        if (textC != null && doc != null) {
            String text = textC.getText();
            SimpleAttributeSet sas = new SimpleAttributeSet();
            isSettingAttributes = true;
            try {
                sas.addAttribute(HTML.Attribute.COMMENT, text);
                ((StyledDocument)doc).setCharacterAttributes
                    (getStartOffset(), getEndOffset() -
                     getStartOffset(), sas, false);
            }
            finally {
                isSettingAttributes = false;
            }
        }
    }

    JTextComponent getTextComponent() {
        return (JTextComponent)getComponent();
    }

    String getRepresentedText() {
        AttributeSet as = getElement().getAttributes();
        if (as != null) {
            Object comment = as.getAttribute(HTML.Attribute.COMMENT);
            if (comment instanceof String) {
                return (String)comment;
            }
        }
        return "";
    }

    static final Border CBorder = new CommentBorder();
    static final int commentPadding = 3;
    static final int commentPaddingD = commentPadding * 3;

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    static class CommentBorder extends LineBorder {
        CommentBorder() {
            super(Color.black, 1);
        }

        public void paintBorder(Component c, Graphics g, int x, int y,
                                int width, int height) {
            super.paintBorder(c, g, x + commentPadding, y,
                              width - commentPaddingD, height);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            Insets retI = super.getBorderInsets(c, insets);

            retI.left += commentPadding;
            retI.right += commentPadding;
            return retI;
        }

        public boolean isBorderOpaque() {
            return false;
        }
    } // End of class CommentView.CommentBorder
} // End of CommentView
