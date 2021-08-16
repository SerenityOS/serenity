/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing;

import java.awt.Component;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.beans.Transient;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.text.BreakIterator;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleExtendedComponent;
import javax.accessibility.AccessibleIcon;
import javax.accessibility.AccessibleKeyBinding;
import javax.accessibility.AccessibleRelation;
import javax.accessibility.AccessibleRelationSet;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleText;
import javax.swing.plaf.LabelUI;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.StyledDocument;
import javax.swing.text.View;

/**
 * A display area for a short text string or an image,
 * or both.
 * A label does not react to input events.
 * As a result, it cannot get the keyboard focus.
 * A label can, however, display a keyboard alternative
 * as a convenience for a nearby component
 * that has a keyboard alternative but can't display it.
 * <p>
 * A <code>JLabel</code> object can display
 * either text, an image, or both.
 * You can specify where in the label's display area
 * the label's contents are aligned
 * by setting the vertical and horizontal alignment.
 * By default, labels are vertically centered
 * in their display area.
 * Text-only labels are leading edge aligned, by default;
 * image-only labels are horizontally centered, by default.
 * <p>
 * You can also specify the position of the text
 * relative to the image.
 * By default, text is on the trailing edge of the image,
 * with the text and image vertically aligned.
 * <p>
 * A label's leading and trailing edge are determined from the value of its
 * {@link java.awt.ComponentOrientation} property.  At present, the default
 * ComponentOrientation setting maps the leading edge to left and the trailing
 * edge to right.
 *
 * <p>
 * Finally, you can use the <code>setIconTextGap</code> method
 * to specify how many pixels
 * should appear between the text and the image.
 * The default is 4 pixels.
 * <p>
 * See <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/label.html">How to Use Labels</a>
 * in <em>The Java Tutorial</em>
 * for further documentation.
 * <p>
 * <strong>Warning:</strong> Swing is not thread safe. For more
 * information see <a
 * href="package-summary.html#threading">Swing's Threading
 * Policy</a>.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author Hans Muller
 * @since 1.2
 */
@JavaBean(defaultProperty = "UI", description = "A component that displays a short string and an icon.")
@SwingContainer(false)
@SuppressWarnings("serial")
public class JLabel extends JComponent implements SwingConstants, Accessible
{
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "LabelUI";

    private int mnemonic = '\0';
    private int mnemonicIndex = -1;

    private String text = "";         // "" rather than null, for BeanBox
    private Icon defaultIcon = null;
    private Icon disabledIcon = null;
    private boolean disabledIconSet = false;

    private int verticalAlignment = CENTER;
    private int horizontalAlignment = LEADING;
    private int verticalTextPosition = CENTER;
    private int horizontalTextPosition = TRAILING;
    private int iconTextGap = 4;

    /**
     * The Component this label is for; null if the label
     * is not the label for a component
     */
    protected Component labelFor = null;

    /**
     * Client property key used to determine what label is labeling the
     * component.  This is generally not used by labels, but is instead
     * used by components such as text areas that are being labeled by
     * labels.  When the labelFor property of a label is set, it will
     * automatically set the LABELED_BY_PROPERTY of the component being
     * labelled.
     *
     * @see #setLabelFor
     */
    static final String LABELED_BY_PROPERTY = "labeledBy";

    /**
     * Creates a <code>JLabel</code> instance with the specified
     * text, image, and horizontal alignment.
     * The label is centered vertically in its display area.
     * The text is on the trailing edge of the image.
     *
     * @param text  The text to be displayed by the label.
     * @param icon  The image to be displayed by the label.
     * @param horizontalAlignment  One of the following constants
     *           defined in <code>SwingConstants</code>:
     *           <code>LEFT</code>,
     *           <code>CENTER</code>,
     *           <code>RIGHT</code>,
     *           <code>LEADING</code> or
     *           <code>TRAILING</code>.
     */
    public JLabel(String text, Icon icon, int horizontalAlignment) {
        setText(text);
        setIcon(icon);
        setHorizontalAlignment(horizontalAlignment);
        updateUI();
        setAlignmentX(LEFT_ALIGNMENT);
    }

    /**
     * Creates a <code>JLabel</code> instance with the specified
     * text and horizontal alignment.
     * The label is centered vertically in its display area.
     *
     * @param text  The text to be displayed by the label.
     * @param horizontalAlignment  One of the following constants
     *           defined in <code>SwingConstants</code>:
     *           <code>LEFT</code>,
     *           <code>CENTER</code>,
     *           <code>RIGHT</code>,
     *           <code>LEADING</code> or
     *           <code>TRAILING</code>.
     */
    public JLabel(String text, int horizontalAlignment) {
        this(text, null, horizontalAlignment);
    }

    /**
     * Creates a <code>JLabel</code> instance with the specified text.
     * The label is aligned against the leading edge of its display area,
     * and centered vertically.
     *
     * @param text  The text to be displayed by the label.
     */
    public JLabel(String text) {
        this(text, null, LEADING);
    }

    /**
     * Creates a <code>JLabel</code> instance with the specified
     * image and horizontal alignment.
     * The label is centered vertically in its display area.
     *
     * @param image  The image to be displayed by the label.
     * @param horizontalAlignment  One of the following constants
     *           defined in <code>SwingConstants</code>:
     *           <code>LEFT</code>,
     *           <code>CENTER</code>,
     *           <code>RIGHT</code>,
     *           <code>LEADING</code> or
     *           <code>TRAILING</code>.
     */
    public JLabel(Icon image, int horizontalAlignment) {
        this(null, image, horizontalAlignment);
    }

    /**
     * Creates a <code>JLabel</code> instance with the specified image.
     * The label is centered vertically and horizontally
     * in its display area.
     *
     * @param image  The image to be displayed by the label.
     */
    public JLabel(Icon image) {
        this(null, image, CENTER);
    }

    /**
     * Creates a <code>JLabel</code> instance with
     * no image and with an empty string for the title.
     * The label is centered vertically
     * in its display area.
     * The label's contents, once set, will be displayed on the leading edge
     * of the label's display area.
     */
    public JLabel() {
        this("", null, LEADING);
    }


    /**
     * Returns the L&amp;F object that renders this component.
     *
     * @return LabelUI object
     */
    public LabelUI getUI() {
        return (LabelUI)ui;
    }


    /**
     * Sets the L&amp;F object that renders this component.
     *
     * @param ui  the LabelUI L&amp;F object
     * @see UIDefaults#getUI
     */
    @BeanProperty(hidden = true, visualUpdate = true, description
            = "The UI object that implements the Component's LookAndFeel.")
    public void setUI(LabelUI ui) {
        super.setUI(ui);
        // disabled icon is generated by LF so it should be unset here
        if (!disabledIconSet && disabledIcon != null) {
            setDisabledIcon(null);
        }
    }


    /**
     * Resets the UI property to a value from the current look and feel.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        setUI((LabelUI) UIManager.getUI(this));
    }


    /**
     * Returns a string that specifies the name of the l&amp;f class
     * that renders this component.
     *
     * @return the string "LabelUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
    }


    /**
     * Returns the text string that the label displays.
     *
     * @return a String
     * @see #setText
     */
    public String getText() {
        return text;
    }


    /**
     * Defines the single line of text this component will display.  If
     * the value of text is null or empty string, nothing is displayed.
     * <p>
     * The default value of this property is null.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param text  the single line of text this component will display
     * @see #setVerticalTextPosition
     * @see #setHorizontalTextPosition
     * @see #setIcon
     */
    @BeanProperty(preferred = true, visualUpdate = true, description
            = "Defines the single line of text this component will display.")
    public void setText(String text) {

        String oldAccessibleName = null;
        if (accessibleContext != null) {
            oldAccessibleName = accessibleContext.getAccessibleName();
        }

        String oldValue = this.text;
        this.text = text;
        firePropertyChange("text", oldValue, text);

        setDisplayedMnemonicIndex(
                      SwingUtilities.findDisplayedMnemonicIndex(
                                          text, getDisplayedMnemonic()));

        if ((accessibleContext != null)
            && (accessibleContext.getAccessibleName() != oldAccessibleName)) {
                accessibleContext.firePropertyChange(
                        AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY,
                        oldAccessibleName,
                        accessibleContext.getAccessibleName());
        }
        if (text == null || oldValue == null || !text.equals(oldValue)) {
            revalidate();
            repaint();
        }
    }


    /**
     * Returns the graphic image (glyph, icon) that the label displays.
     *
     * @return an Icon
     * @see #setIcon
     */
    public Icon getIcon() {
        return defaultIcon;
    }

    /**
     * Defines the icon this component will display.  If
     * the value of icon is null, nothing is displayed.
     * <p>
     * The default value of this property is null.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param icon  the default icon this component will display
     * @see #setVerticalTextPosition
     * @see #setHorizontalTextPosition
     * @see #getIcon
     */
    @BeanProperty(preferred = true, visualUpdate = true, description
            = "The icon this component will display.")
    public void setIcon(Icon icon) {
        Icon oldValue = defaultIcon;
        defaultIcon = icon;

        /* If the default icon has really changed and we had
         * generated the disabled icon for this component
         * (in other words, setDisabledIcon() was never called), then
         * clear the disabledIcon field.
         */
        if ((defaultIcon != oldValue) && !disabledIconSet) {
            disabledIcon = null;
        }

        firePropertyChange("icon", oldValue, defaultIcon);

        if ((accessibleContext != null) && (oldValue != defaultIcon)) {
                accessibleContext.firePropertyChange(
                        AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY,
                        oldValue, defaultIcon);
        }

        /* If the default icon has changed and the new one is
         * a different size, then revalidate.   Repaint if the
         * default icon has changed.
         */
        if (defaultIcon != oldValue) {
            if ((defaultIcon == null) ||
                (oldValue == null) ||
                (defaultIcon.getIconWidth() != oldValue.getIconWidth()) ||
                (defaultIcon.getIconHeight() != oldValue.getIconHeight())) {
                revalidate();
            }
            repaint();
        }
    }


    /**
     * Returns the icon used by the label when it's disabled.
     * If no disabled icon has been set this will forward the call to
     * the look and feel to construct an appropriate disabled Icon.
     * <p>
     * Some look and feels might not render the disabled Icon, in which
     * case they will ignore this.
     *
     * @return the <code>disabledIcon</code> property
     * @see #setDisabledIcon
     * @see javax.swing.LookAndFeel#getDisabledIcon
     * @see ImageIcon
     */
    @Transient
    public Icon getDisabledIcon() {
        if (!disabledIconSet && disabledIcon == null && defaultIcon != null) {
            disabledIcon = UIManager.getLookAndFeel().getDisabledIcon(this, defaultIcon);
            if (disabledIcon != null) {
                firePropertyChange("disabledIcon", null, disabledIcon);
            }
        }
        return disabledIcon;
    }


    /**
     * Set the icon to be displayed if this JLabel is "disabled"
     * (JLabel.setEnabled(false)).
     * <p>
     * The default value of this property is null.
     *
     * @param disabledIcon the Icon to display when the component is disabled
     * @see #getDisabledIcon
     * @see #setEnabled
     */
    @BeanProperty(visualUpdate = true, description
            = "The icon to display if the label is disabled.")
    public void setDisabledIcon(Icon disabledIcon) {
        Icon oldValue = this.disabledIcon;
        this.disabledIcon = disabledIcon;
        disabledIconSet = (disabledIcon != null);
        firePropertyChange("disabledIcon", oldValue, disabledIcon);
        if (disabledIcon != oldValue) {
            if (disabledIcon == null || oldValue == null ||
                disabledIcon.getIconWidth() != oldValue.getIconWidth() ||
                disabledIcon.getIconHeight() != oldValue.getIconHeight()) {
                revalidate();
            }
            if (!isEnabled()) {
                repaint();
            }
        }
    }


    /**
     * Specify a keycode that indicates a mnemonic key.
     * This property is used when the label is part of a larger component.
     * If the labelFor property of the label is not null, the label will
     * call the requestFocus method of the component specified by the
     * labelFor property when the mnemonic is activated.
     *
     * @param key  a keycode that indicates a mnemonic key
     * @see #getLabelFor
     * @see #setLabelFor
     */
    @BeanProperty(visualUpdate = true, description
            = "The mnemonic keycode.")
    public void setDisplayedMnemonic(int key) {
        int oldKey = mnemonic;
        mnemonic = key;
        firePropertyChange("displayedMnemonic", oldKey, mnemonic);

        setDisplayedMnemonicIndex(
            SwingUtilities.findDisplayedMnemonicIndex(getText(), mnemonic));

        if (key != oldKey) {
            revalidate();
            repaint();
        }
    }


    /**
     * Specifies the displayedMnemonic as a char value.
     *
     * @param aChar  a char specifying the mnemonic to display
     * @see #setDisplayedMnemonic(int)
     */
    public void setDisplayedMnemonic(char aChar) {
        int vk = java.awt.event.KeyEvent.getExtendedKeyCodeForChar(aChar);
        if (vk != java.awt.event.KeyEvent.VK_UNDEFINED) {
            setDisplayedMnemonic(vk);
        }
    }


    /**
     * Return the keycode that indicates a mnemonic key.
     * This property is used when the label is part of a larger component.
     * If the labelFor property of the label is not null, the label will
     * call the requestFocus method of the component specified by the
     * labelFor property when the mnemonic is activated.
     *
     * @return int value for the mnemonic key
     *
     * @see #getLabelFor
     * @see #setLabelFor
     */
    public int getDisplayedMnemonic() {
        return mnemonic;
    }

    /**
     * Provides a hint to the look and feel as to which character in the
     * text should be decorated to represent the mnemonic. Not all look and
     * feels may support this. A value of -1 indicates either there is no
     * mnemonic, the mnemonic character is not contained in the string, or
     * the developer does not wish the mnemonic to be displayed.
     * <p>
     * The value of this is updated as the properties relating to the
     * mnemonic change (such as the mnemonic itself, the text...).
     * You should only ever have to call this if
     * you do not wish the default character to be underlined. For example, if
     * the text was 'Save As', with a mnemonic of 'a', and you wanted the 'A'
     * to be decorated, as 'Save <u>A</u>s', you would have to invoke
     * <code>setDisplayedMnemonicIndex(5)</code> after invoking
     * <code>setDisplayedMnemonic(KeyEvent.VK_A)</code>.
     *
     * @since 1.4
     * @param index Index into the String to underline
     * @exception IllegalArgumentException will be thrown if <code>index</code>
     *            is &gt;= length of the text, or &lt; -1
     */
    @BeanProperty(visualUpdate = true, description
            = "the index into the String to draw the keyboard character mnemonic at")
    public void setDisplayedMnemonicIndex(int index)
                                             throws IllegalArgumentException {
        int oldValue = mnemonicIndex;
        if (index == -1) {
            mnemonicIndex = -1;
        } else {
            String text = getText();
            int textLength = (text == null) ? 0 : text.length();
            if (index < -1 || index >= textLength) {  // index out of range
                throw new IllegalArgumentException("index == " + index);
            }
        }
        mnemonicIndex = index;
        firePropertyChange("displayedMnemonicIndex", oldValue, index);
        if (index != oldValue) {
            revalidate();
            repaint();
        }
    }

    /**
     * Returns the character, as an index, that the look and feel should
     * provide decoration for as representing the mnemonic character.
     *
     * @since 1.4
     * @return index representing mnemonic character
     * @see #setDisplayedMnemonicIndex
     */
    public int getDisplayedMnemonicIndex() {
        return mnemonicIndex;
    }

    /**
     * Verify that key is a legal value for the horizontalAlignment properties.
     *
     * @param key the property value to check
     * @param message the IllegalArgumentException detail message
     * @return the key value if {@code key} is a a legal value for the
     *         horizontalAlignment properties
     * @exception IllegalArgumentException if key isn't LEFT, CENTER, RIGHT,
     * LEADING or TRAILING.
     * @see #setHorizontalTextPosition
     * @see #setHorizontalAlignment
     */
    protected int checkHorizontalKey(int key, String message) {
        if ((key == LEFT) ||
            (key == CENTER) ||
            (key == RIGHT) ||
            (key == LEADING) ||
            (key == TRAILING)) {
            return key;
        }
        else {
            throw new IllegalArgumentException(message);
        }
    }


    /**
     * Verify that key is a legal value for the
     * verticalAlignment or verticalTextPosition properties.
     *
     * @param key the property value to check
     * @param message the IllegalArgumentException detail message
     * @return the key value if {@code key} is a legal value for the
     *         verticalAlignment or verticalTextPosition properties
     * @exception IllegalArgumentException if key isn't TOP, CENTER, or BOTTOM.
     * @see #setVerticalAlignment
     * @see #setVerticalTextPosition
     */
    protected int checkVerticalKey(int key, String message) {
        if ((key == TOP) || (key == CENTER) || (key == BOTTOM)) {
            return key;
        }
        else {
            throw new IllegalArgumentException(message);
        }
    }


    /**
     * Returns the amount of space between the text and the icon
     * displayed in this label.
     *
     * @return an int equal to the number of pixels between the text
     *         and the icon.
     * @see #setIconTextGap
     */
    public int getIconTextGap() {
        return iconTextGap;
    }


    /**
     * If both the icon and text properties are set, this property
     * defines the space between them.
     * <p>
     * The default value of this property is 4 pixels.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param iconTextGap  the space between the icon and text properties
     * @see #getIconTextGap
     */
    @BeanProperty(visualUpdate = true, description
            = "If both the icon and text properties are set, this property defines the space between them.")
    public void setIconTextGap(int iconTextGap) {
        int oldValue = this.iconTextGap;
        this.iconTextGap = iconTextGap;
        firePropertyChange("iconTextGap", oldValue, iconTextGap);
        if (iconTextGap != oldValue) {
            revalidate();
            repaint();
        }
    }



    /**
     * Returns the alignment of the label's contents along the Y axis.
     *
     * @return   The value of the verticalAlignment property, one of the
     *           following constants defined in <code>SwingConstants</code>:
     *           <code>TOP</code>,
     *           <code>CENTER</code>, or
     *           <code>BOTTOM</code>.
     *
     * @see SwingConstants
     * @see #setVerticalAlignment
     */
    public int getVerticalAlignment() {
        return verticalAlignment;
    }


    /**
     * Sets the alignment of the label's contents along the Y axis.
     * <p>
     * The default value of this property is CENTER.
     *
     * @param alignment One of the following constants
     *           defined in <code>SwingConstants</code>:
     *           <code>TOP</code>,
     *           <code>CENTER</code> (the default), or
     *           <code>BOTTOM</code>.
     *
     * @see SwingConstants
     * @see #getVerticalAlignment
     */
    @BeanProperty(visualUpdate = true, enumerationValues = {
            "SwingConstants.TOP",
            "SwingConstants.CENTER",
            "SwingConstants.BOTTOM"},
            description = "The alignment of the label's contents along the Y axis.")
    public void setVerticalAlignment(int alignment) {
        if (alignment == verticalAlignment) return;
        int oldValue = verticalAlignment;
        verticalAlignment = checkVerticalKey(alignment, "verticalAlignment");
        firePropertyChange("verticalAlignment", oldValue, verticalAlignment);
        repaint();
    }


    /**
     * Returns the alignment of the label's contents along the X axis.
     *
     * @return   The value of the horizontalAlignment property, one of the
     *           following constants defined in <code>SwingConstants</code>:
     *           <code>LEFT</code>,
     *           <code>CENTER</code>,
     *           <code>RIGHT</code>,
     *           <code>LEADING</code> or
     *           <code>TRAILING</code>.
     *
     * @see #setHorizontalAlignment
     * @see SwingConstants
     */
    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    /**
     * Sets the alignment of the label's contents along the X axis.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param alignment  One of the following constants
     *           defined in <code>SwingConstants</code>:
     *           <code>LEFT</code>,
     *           <code>CENTER</code> (the default for image-only labels),
     *           <code>RIGHT</code>,
     *           <code>LEADING</code> (the default for text-only labels) or
     *           <code>TRAILING</code>.
     *
     * @see SwingConstants
     * @see #getHorizontalAlignment
     */
    @BeanProperty(visualUpdate = true, enumerationValues = {
            "SwingConstants.LEFT",
            "SwingConstants.CENTER",
            "SwingConstants.RIGHT",
            "SwingConstants.LEADING",
            "SwingConstants.TRAILING"}, description
            = "The alignment of the label's content along the X axis.")
    public void setHorizontalAlignment(int alignment) {
        if (alignment == horizontalAlignment) return;
        int oldValue = horizontalAlignment;
        horizontalAlignment = checkHorizontalKey(alignment,
                                                 "horizontalAlignment");
        firePropertyChange("horizontalAlignment",
                           oldValue, horizontalAlignment);
        repaint();
    }


    /**
     * Returns the vertical position of the label's text,
     * relative to its image.
     *
     * @return   One of the following constants
     *           defined in <code>SwingConstants</code>:
     *           <code>TOP</code>,
     *           <code>CENTER</code>, or
     *           <code>BOTTOM</code>.
     *
     * @see #setVerticalTextPosition
     * @see SwingConstants
     */
    public int getVerticalTextPosition() {
        return verticalTextPosition;
    }


    /**
     * Sets the vertical position of the label's text,
     * relative to its image.
     * <p>
     * The default value of this property is CENTER.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param textPosition  One of the following constants
     *           defined in <code>SwingConstants</code>:
     *           <code>TOP</code>,
     *           <code>CENTER</code> (the default), or
     *           <code>BOTTOM</code>.
     *
     * @see SwingConstants
     * @see #getVerticalTextPosition
     */
    @BeanProperty(expert = true, visualUpdate = true, enumerationValues = {
            "SwingConstants.TOP",
            "SwingConstants.CENTER",
            "SwingConstants.BOTTOM"},
            description = "The vertical position of the text relative to it's image.")
    public void setVerticalTextPosition(int textPosition) {
        if (textPosition == verticalTextPosition) return;
        int old = verticalTextPosition;
        verticalTextPosition = checkVerticalKey(textPosition,
                                                "verticalTextPosition");
        firePropertyChange("verticalTextPosition", old, verticalTextPosition);
        revalidate();
        repaint();
    }


    /**
     * Returns the horizontal position of the label's text,
     * relative to its image.
     *
     * @return   One of the following constants
     *           defined in <code>SwingConstants</code>:
     *           <code>LEFT</code>,
     *           <code>CENTER</code>,
     *           <code>RIGHT</code>,
     *           <code>LEADING</code> or
     *           <code>TRAILING</code>.
     *
     * @see SwingConstants
     */
    public int getHorizontalTextPosition() {
        return horizontalTextPosition;
    }


    /**
     * Sets the horizontal position of the label's text,
     * relative to its image.
     *
     * @param textPosition  One of the following constants
     *           defined in <code>SwingConstants</code>:
     *           <code>LEFT</code>,
     *           <code>CENTER</code>,
     *           <code>RIGHT</code>,
     *           <code>LEADING</code>, or
     *           <code>TRAILING</code> (the default).
     *
     * @see SwingConstants
     */
    @BeanProperty(expert = true, visualUpdate = true, enumerationValues = {
            "SwingConstants.LEFT",
            "SwingConstants.CENTER",
            "SwingConstants.RIGHT",
            "SwingConstants.LEADING",
            "SwingConstants.TRAILING"}, description
            = "The horizontal position of the label's text, relative to its image.")
    public void setHorizontalTextPosition(int textPosition) {
        int old = horizontalTextPosition;
        this.horizontalTextPosition = checkHorizontalKey(textPosition,
                                                "horizontalTextPosition");
        firePropertyChange("horizontalTextPosition",
                           old, horizontalTextPosition);
        revalidate();
        repaint();
    }


    /**
     * This is overridden to return false if the current Icon's Image is
     * not equal to the passed in Image <code>img</code>.
     *
     * @see     java.awt.image.ImageObserver
     * @see     java.awt.Component#imageUpdate(java.awt.Image, int, int, int, int, int)
     */
    public boolean imageUpdate(Image img, int infoflags,
                               int x, int y, int w, int h) {
        // Don't use getDisabledIcon, will trigger creation of icon if icon
        // not set.
        if (!isShowing() ||
            !SwingUtilities.doesIconReferenceImage(getIcon(), img) &&
            !SwingUtilities.doesIconReferenceImage(disabledIcon, img)) {

            return false;
        }
        return super.imageUpdate(img, infoflags, x, y, w, h);
    }


    /**
     * See readObject() and writeObject() in JComponent for more
     * information about serialization in Swing.
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if (getUIClassID().equals(uiClassID)) {
            byte count = JComponent.getWriteObjCounter(this);
            JComponent.setWriteObjCounter(this, --count);
            if (count == 0 && ui != null) {
                ui.installUI(this);
            }
        }
    }


    /**
     * Returns a string representation of this JLabel. This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this JLabel.
     */
    protected String paramString() {
        String textString = (text != null ?
                             text : "");
        String defaultIconString = ((defaultIcon != null)
                                    && (defaultIcon != this)  ?
                                    defaultIcon.toString() : "");
        String disabledIconString = ((disabledIcon != null)
                                     && (disabledIcon != this) ?
                                     disabledIcon.toString() : "");
        String labelForString = (labelFor  != null ?
                                 labelFor.toString() : "");
        String verticalAlignmentString;
        if (verticalAlignment == TOP) {
            verticalAlignmentString = "TOP";
        } else if (verticalAlignment == CENTER) {
            verticalAlignmentString = "CENTER";
        } else if (verticalAlignment == BOTTOM) {
            verticalAlignmentString = "BOTTOM";
        } else verticalAlignmentString = "";
        String horizontalAlignmentString;
        if (horizontalAlignment == LEFT) {
            horizontalAlignmentString = "LEFT";
        } else if (horizontalAlignment == CENTER) {
            horizontalAlignmentString = "CENTER";
        } else if (horizontalAlignment == RIGHT) {
            horizontalAlignmentString = "RIGHT";
        } else if (horizontalAlignment == LEADING) {
            horizontalAlignmentString = "LEADING";
        } else if (horizontalAlignment == TRAILING) {
            horizontalAlignmentString = "TRAILING";
        } else horizontalAlignmentString = "";
        String verticalTextPositionString;
        if (verticalTextPosition == TOP) {
            verticalTextPositionString = "TOP";
        } else if (verticalTextPosition == CENTER) {
            verticalTextPositionString = "CENTER";
        } else if (verticalTextPosition == BOTTOM) {
            verticalTextPositionString = "BOTTOM";
        } else verticalTextPositionString = "";
        String horizontalTextPositionString;
        if (horizontalTextPosition == LEFT) {
            horizontalTextPositionString = "LEFT";
        } else if (horizontalTextPosition == CENTER) {
            horizontalTextPositionString = "CENTER";
        } else if (horizontalTextPosition == RIGHT) {
            horizontalTextPositionString = "RIGHT";
        } else if (horizontalTextPosition == LEADING) {
            horizontalTextPositionString = "LEADING";
        } else if (horizontalTextPosition == TRAILING) {
            horizontalTextPositionString = "TRAILING";
        } else horizontalTextPositionString = "";

        return super.paramString() +
        ",defaultIcon=" + defaultIconString +
        ",disabledIcon=" + disabledIconString +
        ",horizontalAlignment=" + horizontalAlignmentString +
        ",horizontalTextPosition=" + horizontalTextPositionString +
        ",iconTextGap=" + iconTextGap +
        ",labelFor=" + labelForString +
        ",text=" + textString +
        ",verticalAlignment=" + verticalAlignmentString +
        ",verticalTextPosition=" + verticalTextPositionString;
    }

    /**
     * --- Accessibility Support ---
     */

    /**
     * Get the component this is labelling.
     *
     * @return the Component this is labelling.  Can be null if this
     * does not label a Component.  If the displayedMnemonic
     * property is set and the labelFor property is also set, the label
     * will call the requestFocus method of the component specified by the
     * labelFor property when the mnemonic is activated.
     *
     * @see #getDisplayedMnemonic
     * @see #setDisplayedMnemonic
     */
    public Component getLabelFor() {
        return labelFor;
    }

    /**
     * Set the component this is labelling.  Can be null if this does not
     * label a Component.  If the displayedMnemonic property is set
     * and the labelFor property is also set, the label will
     * call the requestFocus method of the component specified by the
     * labelFor property when the mnemonic is activated.
     *
     * @param c  the Component this label is for, or null if the label is
     *           not the label for a component
     *
     * @see #getDisplayedMnemonic
     * @see #setDisplayedMnemonic
     */
    @BeanProperty(description
            = "The component this is labelling.")
    public void setLabelFor(Component c) {
        Component oldC = labelFor;
        labelFor = c;
        firePropertyChange("labelFor", oldC, c);

        if (oldC instanceof JComponent) {
            ((JComponent)oldC).putClientProperty(LABELED_BY_PROPERTY, null);
        }
        if (c instanceof JComponent) {
            ((JComponent)c).putClientProperty(LABELED_BY_PROPERTY, this);
        }
    }

    /**
     * Get the AccessibleContext of this object
     *
     * @return the AccessibleContext of this object
     */
    @BeanProperty(bound = false, expert = true, description
            = "The AccessibleContext associated with this Label.")
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJLabel();
        }
        return accessibleContext;
    }

    /**
     * The class used to obtain the accessible role for this object.
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    @SuppressWarnings("serial")
    protected class AccessibleJLabel extends AccessibleJComponent
        implements AccessibleText, AccessibleExtendedComponent {

        /**
         * Constructs an {@code AccessibleJLabel}.
         */
        protected AccessibleJLabel() {}

        /**
         * Get the accessible name of this object.
         *
         * @return the localized name of the object -- can be null if this
         * object does not have a name
         * @see AccessibleContext#setAccessibleName
         */
        public String getAccessibleName() {
            String name = accessibleName;

            if (name == null) {
                name = (String)getClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY);
            }
            if (name == null) {
                name = JLabel.this.getText();
            }
            if (name == null) {
                name = super.getAccessibleName();
            }
            return name;
        }

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.LABEL;
        }

        /**
         * Get the AccessibleIcons associated with this object if one
         * or more exist.  Otherwise return null.
         * @since 1.3
         */
        public AccessibleIcon [] getAccessibleIcon() {
            Icon icon = getIcon();
            if (icon instanceof Accessible) {
                AccessibleContext ac =
                ((Accessible)icon).getAccessibleContext();
                if (ac != null && ac instanceof AccessibleIcon) {
                    return new AccessibleIcon[] { (AccessibleIcon)ac };
                }
            }
            return null;
        }

        /**
         * Get the AccessibleRelationSet associated with this object if one
         * exists.  Otherwise return null.
         * @see AccessibleRelation
         * @since 1.3
         */
        public AccessibleRelationSet getAccessibleRelationSet() {
            // Check where the AccessibleContext's relation
            // set already contains a LABEL_FOR relation.
            AccessibleRelationSet relationSet
                = super.getAccessibleRelationSet();

            if (!relationSet.contains(AccessibleRelation.LABEL_FOR)) {
                Component c = JLabel.this.getLabelFor();
                if (c != null) {
                    AccessibleRelation relation
                        = new AccessibleRelation(AccessibleRelation.LABEL_FOR);
                    relation.setTarget(c);
                    relationSet.add(relation);
                }
            }
            return relationSet;
        }


        /* AccessibleText ---------- */

        public AccessibleText getAccessibleText() {
            View view = (View)JLabel.this.getClientProperty("html");
            if (view != null) {
                return this;
            } else {
                return null;
            }
        }

        /**
         * Given a point in local coordinates, return the zero-based index
         * of the character under that Point.  If the point is invalid,
         * this method returns -1.
         *
         * @param p the Point in local coordinates
         * @return the zero-based index of the character under Point p; if
         * Point is invalid returns -1.
         * @since 1.3
         */
        public int getIndexAtPoint(Point p) {
            View view = (View) JLabel.this.getClientProperty("html");
            if (view != null) {
                Rectangle r = getTextRectangle();
                if (r == null) {
                    return -1;
                }
                Rectangle2D.Float shape =
                    new Rectangle2D.Float(r.x, r.y, r.width, r.height);
                Position.Bias[] bias = new Position.Bias[1];
                return view.viewToModel(p.x, p.y, shape, bias);
            } else {
                return -1;
            }
        }

        /**
         * Returns the bounding box of the character at the given
         * index in the string.  The bounds are returned in local
         * coordinates. If the index is invalid, <code>null</code> is returned.
         *
         * @param i the index into the String
         * @return the screen coordinates of the character's bounding box.
         * If the index is invalid, <code>null</code> is returned.
         * @since 1.3
         */
        public Rectangle getCharacterBounds(int i) {
            View view = (View) JLabel.this.getClientProperty("html");
            if (view != null) {
                Rectangle r = getTextRectangle();
        if (r == null) {
            return null;
        }
                Rectangle2D.Float shape =
                    new Rectangle2D.Float(r.x, r.y, r.width, r.height);
                try {
                    Shape charShape =
                        view.modelToView(i, shape, Position.Bias.Forward);
                    return charShape.getBounds();
                } catch (BadLocationException e) {
                    return null;
                }
            } else {
                return null;
            }
        }

        /**
         * Return the number of characters (valid indicies)
         *
         * @return the number of characters
         * @since 1.3
         */
        public int getCharCount() {
            View view = (View) JLabel.this.getClientProperty("html");
            if (view != null) {
                Document d = view.getDocument();
                if (d instanceof StyledDocument) {
                    StyledDocument doc = (StyledDocument)d;
                    return doc.getLength();
                }
            }
            return accessibleContext.getAccessibleName().length();
        }

        /**
         * Return the zero-based offset of the caret.
         *
         * Note: That to the right of the caret will have the same index
         * value as the offset (the caret is between two characters).
         * @return the zero-based offset of the caret.
         * @since 1.3
         */
        public int getCaretPosition() {
            // There is no caret.
            return -1;
        }

        /**
         * Returns the String at a given index.
         *
         * @param part the AccessibleText.CHARACTER, AccessibleText.WORD,
         * or AccessibleText.SENTENCE to retrieve
         * @param index an index within the text &gt;= 0
         * @return the letter, word, or sentence,
         *   null for an invalid index or part
         * @since 1.3
         */
        public String getAtIndex(int part, int index) {
            if (index < 0 || index >= getCharCount()) {
                return null;
            }
            switch (part) {
            case AccessibleText.CHARACTER:
                try {
                    return getText(index, 1);
                } catch (BadLocationException e) {
                    return null;
                }
            case AccessibleText.WORD:
                try {
                    String s = getText(0, getCharCount());
                    BreakIterator words = BreakIterator.getWordInstance(getLocale());
                    words.setText(s);
                    int end = words.following(index);
                    return s.substring(words.previous(), end);
                } catch (BadLocationException e) {
                    return null;
                }
            case AccessibleText.SENTENCE:
                try {
                    String s = getText(0, getCharCount());
                    BreakIterator sentence =
                        BreakIterator.getSentenceInstance(getLocale());
                    sentence.setText(s);
                    int end = sentence.following(index);
                    return s.substring(sentence.previous(), end);
                } catch (BadLocationException e) {
                    return null;
                }
            default:
                return null;
            }
        }

        /**
         * Returns the String after a given index.
         *
         * @param part the AccessibleText.CHARACTER, AccessibleText.WORD,
         * or AccessibleText.SENTENCE to retrieve
         * @param index an index within the text &gt;= 0
         * @return the letter, word, or sentence, null for an invalid
         *  index or part
         * @since 1.3
         */
        public String getAfterIndex(int part, int index) {
            if (index < 0 || index >= getCharCount()) {
                return null;
            }
            switch (part) {
            case AccessibleText.CHARACTER:
                if (index+1 >= getCharCount()) {
                   return null;
                }
                try {
                    return getText(index+1, 1);
                } catch (BadLocationException e) {
                    return null;
                }
            case AccessibleText.WORD:
                try {
                    String s = getText(0, getCharCount());
                    BreakIterator words = BreakIterator.getWordInstance(getLocale());
                    words.setText(s);
                    int start = words.following(index);
                    if (start == BreakIterator.DONE || start >= s.length()) {
                        return null;
                    }
                    int end = words.following(start);
                    if (end == BreakIterator.DONE || end >= s.length()) {
                        return null;
                    }
                    return s.substring(start, end);
                } catch (BadLocationException e) {
                    return null;
                }
            case AccessibleText.SENTENCE:
                try {
                    String s = getText(0, getCharCount());
                    BreakIterator sentence =
                        BreakIterator.getSentenceInstance(getLocale());
                    sentence.setText(s);
                    int start = sentence.following(index);
                    if (start == BreakIterator.DONE || start > s.length()) {
                        return null;
                    }
                    int end = sentence.following(start);
                    if (end == BreakIterator.DONE || end > s.length()) {
                        return null;
                    }
                    return s.substring(start, end);
                } catch (BadLocationException e) {
                    return null;
                }
            default:
                return null;
            }
        }

        /**
         * Returns the String before a given index.
         *
         * @param part the AccessibleText.CHARACTER, AccessibleText.WORD,
         *   or AccessibleText.SENTENCE to retrieve
         * @param index an index within the text &gt;= 0
         * @return the letter, word, or sentence, null for an invalid index
         *  or part
         * @since 1.3
         */
        public String getBeforeIndex(int part, int index) {
            if (index < 0 || index > getCharCount()-1) {
                return null;
            }
            switch (part) {
            case AccessibleText.CHARACTER:
                if (index == 0) {
                    return null;
                }
                try {
                    return getText(index-1, 1);
                } catch (BadLocationException e) {
                    return null;
                }
            case AccessibleText.WORD:
                try {
                    String s = getText(0, getCharCount());
                    BreakIterator words = BreakIterator.getWordInstance(getLocale());
                    words.setText(s);
                    int end = words.following(index);
                    end = words.previous();
                    int start = words.previous();
                    if (start == BreakIterator.DONE) {
                        return null;
                    }
                    return s.substring(start, end);
                } catch (BadLocationException e) {
                    return null;
                }
            case AccessibleText.SENTENCE:
                try {
                    String s = getText(0, getCharCount());
                    BreakIterator sentence =
                        BreakIterator.getSentenceInstance(getLocale());
                    sentence.setText(s);
                    int end = sentence.following(index);
                    end = sentence.previous();
                    int start = sentence.previous();
                    if (start == BreakIterator.DONE) {
                        return null;
                    }
                    return s.substring(start, end);
                } catch (BadLocationException e) {
                    return null;
                }
            default:
                return null;
            }
        }

        /**
         * Return the AttributeSet for a given character at a given index
         *
         * @param i the zero-based index into the text
         * @return the AttributeSet of the character
         * @since 1.3
         */
        public AttributeSet getCharacterAttribute(int i) {
            View view = (View) JLabel.this.getClientProperty("html");
            if (view != null) {
                Document d = view.getDocument();
                if (d instanceof StyledDocument) {
                    StyledDocument doc = (StyledDocument)d;
                    Element elem = doc.getCharacterElement(i);
                    if (elem != null) {
                        return elem.getAttributes();
                    }
                }
            }
            return null;
        }

        /**
         * Returns the start offset within the selected text.
         * If there is no selection, but there is
         * a caret, the start and end offsets will be the same.
         *
         * @return the index into the text of the start of the selection
         * @since 1.3
         */
        public int getSelectionStart() {
            // Text cannot be selected.
            return -1;
        }

        /**
         * Returns the end offset within the selected text.
         * If there is no selection, but there is
         * a caret, the start and end offsets will be the same.
         *
         * @return the index into the text of the end of the selection
         * @since 1.3
         */
        public int getSelectionEnd() {
            // Text cannot be selected.
            return -1;
        }

        /**
         * Returns the portion of the text that is selected.
         *
         * @return the String portion of the text that is selected
         * @since 1.3
         */
        public String getSelectedText() {
            // Text cannot be selected.
            return null;
        }

        /*
         * Returns the text substring starting at the specified
         * offset with the specified length.
         */
        private String getText(int offset, int length)
            throws BadLocationException {

            View view = (View) JLabel.this.getClientProperty("html");
            if (view != null) {
                Document d = view.getDocument();
                if (d instanceof StyledDocument) {
                    StyledDocument doc = (StyledDocument)d;
                    return doc.getText(offset, length);
                }
            }
            return null;
        }

        /*
         * Returns the bounding rectangle for the component text.
         */
        private Rectangle getTextRectangle() {

            String text = JLabel.this.getText();
            Icon icon = (JLabel.this.isEnabled()) ? JLabel.this.getIcon() : JLabel.this.getDisabledIcon();

            if ((icon == null) && (text == null)) {
                return null;
            }

            Rectangle paintIconR = new Rectangle();
            Rectangle paintTextR = new Rectangle();
            Rectangle paintViewR = new Rectangle();
            Insets paintViewInsets = new Insets(0, 0, 0, 0);

            paintViewInsets = JLabel.this.getInsets(paintViewInsets);
            paintViewR.x = paintViewInsets.left;
            paintViewR.y = paintViewInsets.top;
            paintViewR.width = JLabel.this.getWidth() - (paintViewInsets.left + paintViewInsets.right);
            paintViewR.height = JLabel.this.getHeight() - (paintViewInsets.top + paintViewInsets.bottom);

            String clippedText = SwingUtilities.layoutCompoundLabel(
                (JComponent)JLabel.this,
                getFontMetrics(getFont()),
                text,
                icon,
                JLabel.this.getVerticalAlignment(),
                JLabel.this.getHorizontalAlignment(),
                JLabel.this.getVerticalTextPosition(),
                JLabel.this.getHorizontalTextPosition(),
                paintViewR,
                paintIconR,
                paintTextR,
                JLabel.this.getIconTextGap());

            return paintTextR;
        }

        // ----- AccessibleExtendedComponent

        /**
         * Returns the AccessibleExtendedComponent
         *
         * @return the AccessibleExtendedComponent
         */
        AccessibleExtendedComponent getAccessibleExtendedComponent() {
            return this;
        }

        /**
         * Returns the tool tip text
         *
         * @return the tool tip text, if supported, of the object;
         * otherwise, null
         * @since 1.4
         */
        public String getToolTipText() {
            return JLabel.this.getToolTipText();
        }

        /**
         * Returns the titled border text
         *
         * @return the titled border text, if supported, of the object;
         * otherwise, null
         * @since 1.4
         */
        public String getTitledBorderText() {
            return super.getTitledBorderText();
        }

        /**
         * Returns key bindings associated with this object
         *
         * @return the key bindings, if supported, of the object;
         * otherwise, null
         * @see AccessibleKeyBinding
         * @since 1.4
         */
        public AccessibleKeyBinding getAccessibleKeyBinding() {
            int mnemonic = JLabel.this.getDisplayedMnemonic();
            if (mnemonic == 0) {
                return null;
            }
            return new LabelKeyBinding(mnemonic);
        }

        class LabelKeyBinding implements AccessibleKeyBinding {
            int mnemonic;

            LabelKeyBinding(int mnemonic) {
                this.mnemonic = mnemonic;
            }

            /**
             * Returns the number of key bindings for this object
             *
             * @return the zero-based number of key bindings for this object
             */
            public int getAccessibleKeyBindingCount() {
                return 1;
            }

            /**
             * Returns a key binding for this object.  The value returned is an
             * java.lang.Object which must be cast to appropriate type depending
             * on the underlying implementation of the key.  For example, if the
             * Object returned is a javax.swing.KeyStroke, the user of this
             * method should do the following:
             * <nf><code>
             * Component c = <get the component that has the key bindings>
             * AccessibleContext ac = c.getAccessibleContext();
             * AccessibleKeyBinding akb = ac.getAccessibleKeyBinding();
             * for (int i = 0; i < akb.getAccessibleKeyBindingCount(); i++) {
             *     Object o = akb.getAccessibleKeyBinding(i);
             *     if (o instanceof javax.swing.KeyStroke) {
             *         javax.swing.KeyStroke keyStroke = (javax.swing.KeyStroke)o;
             *         <do something with the key binding>
             *     }
             * }
             * </code></nf>
             *
             * @param i zero-based index of the key bindings
             * @return a javax.lang.Object which specifies the key binding
             * @exception IllegalArgumentException if the index is
             * out of bounds
             * @see #getAccessibleKeyBindingCount
             */
            public java.lang.Object getAccessibleKeyBinding(int i) {
                if (i != 0) {
                    throw new IllegalArgumentException();
                }
                return KeyStroke.getKeyStroke(mnemonic, 0);
            }
        }

    }  // AccessibleJComponent
}
