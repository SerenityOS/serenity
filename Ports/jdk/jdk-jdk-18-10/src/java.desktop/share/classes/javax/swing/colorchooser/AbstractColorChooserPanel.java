/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.colorchooser;

import java.awt.*;
import java.beans.BeanProperty;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;

/**
 * This is the abstract superclass for color choosers.  If you want to add
 * a new color chooser panel into a <code>JColorChooser</code>, subclass
 * this class.
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
 * @author Tom Santos
 * @author Steve Wilson
 */
@SuppressWarnings("serial") // Same-version serialization only
public abstract class AbstractColorChooserPanel extends JPanel {


    /**
     * Identifies that the transparency of the color (alpha value) can be
     * selected
     */
    public static final String TRANSPARENCY_ENABLED_PROPERTY
            = "TransparencyEnabled";

    private final PropertyChangeListener enabledListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent event) {
            Object value = event.getNewValue();
            if (value instanceof Boolean) {
                setEnabled((Boolean) value);
            }
        }
    };

    /**
     *
     */
    private JColorChooser chooser;

    /**
     * Constructor for subclasses to call.
     */
    protected AbstractColorChooserPanel() {}

    /**
      * Invoked automatically when the model's state changes.
      * It is also called by <code>installChooserPanel</code> to allow
      * you to set up the initial state of your chooser.
      * Override this method to update your <code>ChooserPanel</code>.
      */
    public abstract void updateChooser();

    /**
     * Builds a new chooser panel.
     */
    protected abstract void buildChooser();

    /**
     * Returns a string containing the display name of the panel.
     * @return the name of the display panel
     */
    public abstract String getDisplayName();

    /**
     * Provides a hint to the look and feel as to the
     * <code>KeyEvent.VK</code> constant that can be used as a mnemonic to
     * access the panel. A return value &lt;= 0 indicates there is no mnemonic.
     * <p>
     * The return value here is a hint, it is ultimately up to the look
     * and feel to honor the return value in some meaningful way.
     * <p>
     * This implementation returns 0, indicating the
     * <code>AbstractColorChooserPanel</code> does not support a mnemonic,
     * subclasses wishing a mnemonic will need to override this.
     *
     * @return KeyEvent.VK constant identifying the mnemonic; &lt;= 0 for no
     *         mnemonic
     * @see #getDisplayedMnemonicIndex
     * @since 1.4
     */
    public int getMnemonic() {
        return 0;
    }

    /**
     * Provides a hint to the look and feel as to the index of the character in
     * <code>getDisplayName</code> that should be visually identified as the
     * mnemonic. The look and feel should only use this if
     * <code>getMnemonic</code> returns a value &gt; 0.
     * <p>
     * The return value here is a hint, it is ultimately up to the look
     * and feel to honor the return value in some meaningful way. For example,
     * a look and feel may wish to render each
     * <code>AbstractColorChooserPanel</code> in a <code>JTabbedPane</code>,
     * and further use this return value to underline a character in
     * the <code>getDisplayName</code>.
     * <p>
     * This implementation returns -1, indicating the
     * <code>AbstractColorChooserPanel</code> does not support a mnemonic,
     * subclasses wishing a mnemonic will need to override this.
     *
     * @return Character index to render mnemonic for; -1 to provide no
     *                   visual identifier for this panel.
     * @see #getMnemonic
     * @since 1.4
     */
    public int getDisplayedMnemonicIndex() {
        return -1;
    }

    /**
     * Returns the small display icon for the panel.
     * @return the small display icon
     */
    public abstract Icon getSmallDisplayIcon();

    /**
     * Returns the large display icon for the panel.
     * @return the large display icon
     */
    public abstract Icon getLargeDisplayIcon();

    /**
     * Invoked when the panel is added to the chooser.
     * If you override this, be sure to call <code>super</code>.
     *
     * @param enclosingChooser the chooser to which the panel is to be added
     * @exception RuntimeException  if the chooser panel has already been
     *                          installed
     */
    public void installChooserPanel(JColorChooser enclosingChooser) {
        if (chooser != null) {
            throw new RuntimeException ("This chooser panel is already installed");
        }
        chooser = enclosingChooser;
        chooser.addPropertyChangeListener("enabled", enabledListener);
        setEnabled(chooser.isEnabled());
        buildChooser();
        updateChooser();
    }

    /**
     * Invoked when the panel is removed from the chooser.
     * If override this, be sure to call <code>super</code>.
     *
     * @param enclosingChooser the chooser from which the panel is to be removed
     */
  public void uninstallChooserPanel(JColorChooser enclosingChooser) {
        chooser.removePropertyChangeListener("enabled", enabledListener);
        chooser = null;
    }

    /**
      * Returns the model that the chooser panel is editing.
      * @return the <code>ColorSelectionModel</code> model this panel
      *         is editing
      */
    public ColorSelectionModel getColorSelectionModel() {
        return (this.chooser != null)
                ? this.chooser.getSelectionModel()
                : null;
    }

    /**
     * Returns the color that is currently selected.
     * @return the <code>Color</code> that is selected
     */
    protected Color getColorFromModel() {
        ColorSelectionModel model = getColorSelectionModel();
        return (model != null)
                ? model.getSelectedColor()
                : null;
    }

    void setSelectedColor(Color color) {
        ColorSelectionModel model = getColorSelectionModel();
        if (model != null) {
            model.setSelectedColor(color);
        }
    }

    /**
     * Sets whether color chooser panel allows to select the transparency
     * (alpha value) of a color.
     * This method fires a property-changed event, using the string value of
     * {@code TRANSPARENCY_ENABLED_PROPERTY} as the name
     * of the property.
     *
     * <p>The value is a hint and may not be applicable to all types of chooser
     * panel.
     *
     * <p>The default value is {@code true}.
     *
     * @param b true if the transparency of a color can be selected
     * @see #isColorTransparencySelectionEnabled()
     */
    @BeanProperty(description
            = "Sets the transparency of a color selection on or off.")
    public void setColorTransparencySelectionEnabled(boolean b){
    }

    /**
     * Gets whether color chooser panel allows to select the transparency
     * (alpha value) of a color.
     *
     * @return true if the transparency of a color can be selected
     * @see #setColorTransparencySelectionEnabled(boolean)
     */
    public boolean isColorTransparencySelectionEnabled(){
        return true;
    }

    /**
     * Draws the panel.
     * @param g  the <code>Graphics</code> object
     */
    public void paint(Graphics g) {
        super.paint(g);
    }

    /**
     * Returns an integer from the defaults table. If <code>key</code> does
     * not map to a valid <code>Integer</code>, <code>default</code> is
     * returned.
     *
     * @param key  an <code>Object</code> specifying the int
     * @param defaultValue Returned value if <code>key</code> is not available,
     *                     or is not an Integer
     * @return the int
     */
    int getInt(Object key, int defaultValue) {
        Object value = UIManager.get(key, getLocale());

        if (value instanceof Integer) {
            return ((Integer)value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String)value);
            } catch (NumberFormatException nfe) {}
        }
        return defaultValue;
    }
}
