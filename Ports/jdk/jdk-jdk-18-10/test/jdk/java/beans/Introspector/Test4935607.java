/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test %I% %G%
 * @bug 4935607
 * @summary Tests transient properties
 * @author Sergey Malenkov
 */

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.awt.geom.RectangularShape;
import java.awt.im.InputContext;

import java.beans.FeatureDescriptor;
import java.beans.Transient;

import java.util.EventListener;

import javax.swing.AbstractButton;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;

public class Test4935607 {
    public static void main(String[] args) {
        // test all possible cases
        test(Null.class);
        test(True.class);
        test(False.class);
        test(NullNull.class);
        test(TrueNull.class);
        test(FalseNull.class);
        test(NullTrue.class);
        test(TrueTrue.class);
        test(FalseTrue.class);
        test(NullFalse.class);
        test(TrueFalse.class);
        test(FalseFalse.class);
        // test transient properties in AWT
        test(RectangularShape.class, "frame"); // NON-NLS: getFrame()
        test(Rectangle.class, "bounds"); // NON-NLS: getBounds()
        test(Dimension.class, "size"); // NON-NLS: getSize()
        test(Point.class, "location"); // NON-NLS: getLocation()
        test(Component.class, "foreground"); // NON-NLS: getForeground()
        test(Component.class, "background"); // NON-NLS: getBackground()
        test(Component.class, "font"); // NON-NLS: getFont()
        test(Component.class, "visible"); // NON-NLS: getVisible()
        test(ScrollPane.class, "scrollPosition"); // NON-NLS: getScrollPosition()
        test(InputContext.class, "compositionEnabled"); // NON-NLS: getCompositionEnabled()
        // test transient properties in Swing
        test(JComponent.class, "minimumSize"); // NON-NLS: getMinimumSize()
        test(JComponent.class, "preferredSize"); // NON-NLS: getPreferredSize()
        test(JComponent.class, "maximumSize"); // NON-NLS: getMaximumSize()
        test(ImageIcon.class, "image"); // NON-NLS: getImage()
        test(ImageIcon.class, "imageObserver"); // NON-NLS: getImageObserver()
        test(JMenuBar.class, "helpMenu"); // NON-NLS: getHelpMenu()
        test(JScrollPane.class, "verticalScrollBar"); // NON-NLS: getVerticalScrollBar()
        test(JScrollPane.class, "horizontalScrollBar"); // NON-NLS: getHorizontalScrollBar()
        test(JScrollPane.class, "rowHeader"); // NON-NLS: getRowHeader()
        test(JScrollPane.class, "columnHeader"); // NON-NLS: getColumnHeader()
        test(JViewport.class, "extentSize"); // NON-NLS: getExtentSize()
        test(JTableHeader.class, "defaultRenderer"); // NON-NLS: getDefaultRenderer()
        test(JList.class, "cellRenderer"); // NON-NLS: getCellRenderer()
        test(JList.class, "selectedIndices"); // NON-NLS: getSelectedIndices()
        test(DefaultListSelectionModel.class, "leadSelectionIndex"); // NON-NLS: getLeadSelectionIndex()
        test(DefaultListSelectionModel.class, "anchorSelectionIndex"); // NON-NLS: getAnchorSelectionIndex()
        test(JComboBox.class, "selectedIndex"); // NON-NLS: getSelectedIndex()
        test(JTabbedPane.class, "selectedIndex"); // NON-NLS: getSelectedIndex()
        test(JTabbedPane.class, "selectedComponent"); // NON-NLS: getSelectedComponent()
        test(AbstractButton.class, "disabledIcon"); // NON-NLS: getDisabledIcon()
        test(JLabel.class, "disabledIcon"); // NON-NLS: getDisabledIcon()
        test(JTextComponent.class, "caret"); // NON-NLS: getCaret()
        test(JTextComponent.class, "caretPosition"); // NON-NLS: getCaretPosition()
        test(JTextComponent.class, "selectionStart"); // NON-NLS: getSelectionStart()
        test(JTextComponent.class, "selectionEnd"); // NON-NLS: getSelectionEnd()
    }

    private static void test(Class type) {
        Object value = getExpectedValue(type);
        test(value, BeanUtils.getPropertyDescriptor(type, "property")); // NON-NLS: the property to check
        test(value, BeanUtils.getEventSetDescriptor(type, "eventSet")); // NON-NLS: the event set to check
        System.out.println();
    }

    private static void test(Class type, String property) {
        System.out.print(type.getName() + ": ");
        test(Boolean.TRUE, BeanUtils.getPropertyDescriptor(type, property));
    }

    private static void test(Object expected, FeatureDescriptor fd) {
        System.out.println(fd.getName());
        Object actual = fd.getValue("transient"); // NON-NLS: the attribute name
        if ((actual == null) ? (expected != null) : !actual.equals(expected))
            throw new Error("expected " + expected + " value, but actual value is " + actual);
    }

    private static Object getExpectedValue(Class type) {
        try {
            return type.getField("VALUE").get(type); // NON-NLS: the field name with expected value
        } catch (NoSuchFieldException exception) {
            return null;
        } catch (IllegalAccessException exception) {
            throw new Error("unexpected error", exception);
        }
    }


    public static class Null {
        public Object getProperty() {
            return this;
        }

        public void setProperty(Object object) {
        }

        public void addEventSetListener(EventSetListener listener) {
        }

        public void removeEventSetListener(EventSetListener listener) {
        }
    }

    public static class True {
        public static final Boolean VALUE = Boolean.TRUE;

        @Transient
        public Object getProperty() {
            return this;
        }

        @Transient
        public void setProperty(Object object) {
        }

        @Transient
        public void addEventSetListener(EventSetListener listener) {
        }

        @Transient
        public void removeEventSetListener(EventSetListener listener) {
        }
    }

    public static class False {
        public static final Boolean VALUE = Boolean.FALSE;

        @Transient(false)
        public Object getProperty() {
            return this;
        }

        @Transient(false)
        public void setProperty(Object object) {
        }

        @Transient(false)
        public void addEventSetListener(EventSetListener listener) {
        }

        @Transient(false)
        public void removeEventSetListener(EventSetListener listener) {
        }
    }

    public static class NullNull extends Null {
        @Override
        public Object getProperty() {
            return this;
        }

        @Override
        public void setProperty(Object object) {
        }

        @Override
        public void addEventSetListener(EventSetListener listener) {
        }

        @Override
        public void removeEventSetListener(EventSetListener listener) {
        }
    }

    public static class TrueNull extends Null {
        public static final Boolean VALUE = Boolean.TRUE;

        @Override
        @Transient
        public Object getProperty() {
            return this;
        }

        @Override
        @Transient
        public void setProperty(Object object) {
        }

        @Override
        @Transient
        public void addEventSetListener(EventSetListener listener) {
        }

        @Override
        @Transient
        public void removeEventSetListener(EventSetListener listener) {
        }
    }

    public static class FalseNull extends Null {
        public static final Boolean VALUE = Boolean.FALSE;

        @Override
        @Transient(false)
        public Object getProperty() {
            return this;
        }

        @Override
        @Transient(false)
        public void setProperty(Object object) {
        }

        @Override
        @Transient(false)
        public void addEventSetListener(EventSetListener listener) {
        }

        @Override
        @Transient(false)
        public void removeEventSetListener(EventSetListener listener) {
        }
    }

    public static class NullTrue extends True {
        @Override
        public Object getProperty() {
            return this;
        }

        @Override
        public void setProperty(Object object) {
        }

        @Override
        public void addEventSetListener(EventSetListener listener) {
        }

        @Override
        public void removeEventSetListener(EventSetListener listener) {
        }
    }

    public static class TrueTrue extends True {
        @Override
        @Transient
        public Object getProperty() {
            return this;
        }

        @Override
        @Transient
        public void setProperty(Object object) {
        }

        @Override
        @Transient
        public void addEventSetListener(EventSetListener listener) {
        }

        @Override
        @Transient
        public void removeEventSetListener(EventSetListener listener) {
        }
    }

    public static class FalseTrue extends True {
        public static final Boolean VALUE = Boolean.FALSE;

        @Override
        @Transient(false)
        public Object getProperty() {
            return this;
        }

        @Override
        @Transient(false)
        public void setProperty(Object object) {
        }

        @Override
        @Transient(false)
        public void addEventSetListener(EventSetListener listener) {
        }

        @Override
        @Transient(false)
        public void removeEventSetListener(EventSetListener listener) {
        }
    }

    public static class NullFalse extends False {
        @Override
        public Object getProperty() {
            return this;
        }

        @Override
        public void setProperty(Object object) {
        }

        @Override
        public void addEventSetListener(EventSetListener listener) {
        }

        @Override
        public void removeEventSetListener(EventSetListener listener) {
        }
    }

    public static class TrueFalse extends False {
        public static final Boolean VALUE = Boolean.TRUE;

        @Override
        @Transient
        public Object getProperty() {
            return this;
        }

        @Override
        @Transient
        public void setProperty(Object object) {
        }

        @Override
        @Transient
        public void addEventSetListener(EventSetListener listener) {
        }

        @Override
        @Transient
        public void removeEventSetListener(EventSetListener listener) {
        }
    }

    public static class FalseFalse extends False {
        @Override
        @Transient(false)
        public Object getProperty() {
            return this;
        }

        @Override
        @Transient(false)
        public void setProperty(Object object) {
        }

        @Override
        @Transient(false)
        public void addEventSetListener(EventSetListener listener) {
        }

        @Override
        @Transient(false)
        public void removeEventSetListener(EventSetListener listener) {
        }
    }

    public static final class EventSetListener implements EventListener {
    }
}
