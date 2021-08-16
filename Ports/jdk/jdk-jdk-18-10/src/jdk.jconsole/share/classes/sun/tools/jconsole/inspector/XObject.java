/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jconsole.inspector;

// java import
import javax.swing.*;
import java.util.Objects;

/**
 * This provides a wrapper to the Object class to allow it to be
 displayed/manipulated as a GUI object.
*/
@SuppressWarnings("serial")
public class XObject extends JLabel {
    private Object object;
    private static boolean useHashCodeRepresentation = true;
    public static final XObject NULL_OBJECT = new XObject("null");
    public XObject (Object object, Icon icon) {
        this(object);
        setIcon(icon);
    }

    public XObject (Object object) {
        setObject(object);
        setHorizontalAlignment(SwingConstants.LEFT);
    }

    public boolean equals(Object o) {
        if (o instanceof XObject) {
            return Objects.equals(object, ((XObject)o).getObject());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return object.hashCode();
    }


    public Object getObject() {
        return object;
    }

    //if true the object.hashcode is added to the label
    public static void
        useHashCodeRepresentation(boolean useHashCodeRepresentation) {
        XObject.useHashCodeRepresentation = useHashCodeRepresentation;
    }

    public static boolean hashCodeRepresentation() {
        return useHashCodeRepresentation;
    }

    public void setObject(Object object) {
        this.object = object;
        // if the object is not  a swing component,
        // use default icon
        try {
            String text = null;
            if (object instanceof JLabel) {
                setIcon(((JLabel)object).getIcon());
                if (getText() != null) {
                    text = ((JLabel)object).getText();

                }
            }
            else if (object instanceof JButton) {
                setIcon(((JButton)object).getIcon());
                if (getText() != null) {
                    text = ((JButton)object).getText();
                }
            }
            else if (getText() != null) {
                text = object.toString();
                setIcon(IconManager.DEFAULT_XOBJECT);
            }
            if (text != null) {
                if (useHashCodeRepresentation && (this != NULL_OBJECT)) {
                    text = text + "     ("+object.hashCode()+")";
                }
                setText(text);
            }
        }
        catch (Exception e) {
             System.out.println("Error setting XObject object :"+
                                e.getMessage());
        }
    }
}
