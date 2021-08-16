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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.plaf.ToolBarUI;
import javax.swing.plaf.UIResource;

/**
 * <code>JToolBar</code> provides a component that is useful for
 * displaying commonly used <code>Action</code>s or controls.
 * For examples and information on using tool bars see
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/toolbar.html">How to Use Tool Bars</a>,
 * a section in <em>The Java Tutorial</em>.
 *
 * <p>
 * With most look and feels,
 * the user can drag out a tool bar into a separate window
 * (unless the <code>floatable</code> property is set to <code>false</code>).
 * For drag-out to work correctly, it is recommended that you add
 * <code>JToolBar</code> instances to one of the four "sides" of a
 * container whose layout manager is a <code>BorderLayout</code>,
 * and do not add children to any of the other four "sides".
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
 * @author Georges Saab
 * @author Jeff Shapiro
 * @see Action
 * @since 1.2
 */
@JavaBean(defaultProperty = "UI", description = "A component which displays commonly used controls or Actions.")
@SwingContainer
@SuppressWarnings("serial") // Same-version serialization only
public class JToolBar extends JComponent implements SwingConstants, Accessible
{
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "ToolBarUI";

    private    boolean   paintBorder              = true;
    private    Insets    margin                   = null;
    private    boolean   floatable                = true;
    private    int       orientation              = HORIZONTAL;

    /**
     * Creates a new tool bar; orientation defaults to <code>HORIZONTAL</code>.
     */
    public JToolBar()
    {
        this( HORIZONTAL );
    }

    /**
     * Creates a new tool bar with the specified <code>orientation</code>.
     * The <code>orientation</code> must be either <code>HORIZONTAL</code>
     * or <code>VERTICAL</code>.
     *
     * @param orientation  the orientation desired
     */
    public JToolBar( int orientation )
    {
        this(null, orientation);
    }

    /**
     * Creates a new tool bar with the specified <code>name</code>.  The
     * name is used as the title of the undocked tool bar.  The default
     * orientation is <code>HORIZONTAL</code>.
     *
     * @param name the name of the tool bar
     * @since 1.3
     */
    public JToolBar( String name ) {
        this(name, HORIZONTAL);
    }

    /**
     * Creates a new tool bar with a specified <code>name</code> and
     * <code>orientation</code>.
     * All other constructors call this constructor.
     * If <code>orientation</code> is an invalid value, an exception will
     * be thrown.
     *
     * @param name  the name of the tool bar
     * @param orientation  the initial orientation -- it must be
     *          either <code>HORIZONTAL</code> or <code>VERTICAL</code>
     * @exception IllegalArgumentException if orientation is neither
     *          <code>HORIZONTAL</code> nor <code>VERTICAL</code>
     * @since 1.3
     */
    public JToolBar( String name , int orientation) {
        setName(name);
        checkOrientation( orientation );

        this.orientation = orientation;
        DefaultToolBarLayout layout =  new DefaultToolBarLayout( orientation );
        setLayout( layout );

        addPropertyChangeListener( layout );

        updateUI();
    }

    /**
     * Returns the tool bar's current UI.
     *
     * @return the tool bar's current UI.
     * @see #setUI
     */
    public ToolBarUI getUI() {
        return (ToolBarUI)ui;
    }

    /**
     * Sets the L&amp;F object that renders this component.
     *
     * @param ui  the <code>ToolBarUI</code> L&amp;F object
     * @see UIDefaults#getUI
     */
    @BeanProperty(hidden = true, visualUpdate = true, description
            = "The UI object that implements the Component's LookAndFeel.")
    public void setUI(ToolBarUI ui) {
        super.setUI(ui);
    }

    /**
     * Notification from the <code>UIFactory</code> that the L&amp;F has changed.
     * Called to replace the UI with the latest version from the
     * <code>UIFactory</code>.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        setUI((ToolBarUI)UIManager.getUI(this));
        // GTKLookAndFeel installs a different LayoutManager, and sets it
        // to null after changing the look and feel, so, install the default
        // if the LayoutManager is null.
        if (getLayout() == null) {
            setLayout(new DefaultToolBarLayout(getOrientation()));
        }
        invalidate();
    }



    /**
     * Returns the name of the L&amp;F class that renders this component.
     *
     * @return the string "ToolBarUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
    }


    /**
     * Returns the index of the specified component.
     * (Note: Separators occupy index positions.)
     *
     * @param c  the <code>Component</code> to find
     * @return an integer indicating the component's position,
     *          where 0 is first
     */
    public int getComponentIndex(Component c) {
        int ncomponents = this.getComponentCount();
        Component[] component = this.getComponents();
        for (int i = 0 ; i < ncomponents ; i++) {
            Component comp = component[i];
            if (comp == c)
                return i;
        }
        return -1;
    }

    /**
     * Returns the component at the specified index.
     *
     * @param i  the component's position, where 0 is first
     * @return   the <code>Component</code> at that position,
     *          or <code>null</code> for an invalid index
     *
     */
    public Component getComponentAtIndex(int i) {
        int ncomponents = this.getComponentCount();
        if ( i >= 0 && i < ncomponents) {
            Component[] component = this.getComponents();
            return component[i];
        }
        return null;
    }

     /**
      * Sets the margin between the tool bar's border and
      * its buttons. Setting to <code>null</code> causes the tool bar to
      * use the default margins. The tool bar's default <code>Border</code>
      * object uses this value to create the proper margin.
      * However, if a non-default border is set on the tool bar,
      * it is that <code>Border</code> object's responsibility to create the
      * appropriate margin space (otherwise this property will
      * effectively be ignored).
      *
      * @param m an <code>Insets</code> object that defines the space
      *         between the border and the buttons
      * @see Insets
      */
     @BeanProperty(expert = true, description
             = "The margin between the tool bar's border and contents")
     public void setMargin(Insets m)
     {
         Insets old = margin;
         margin = m;
         firePropertyChange("margin", old, m);
         revalidate();
         repaint();
     }

     /**
      * Returns the margin between the tool bar's border and
      * its buttons.
      *
      * @return an <code>Insets</code> object containing the margin values
      * @see Insets
      */
     public Insets getMargin()
     {
         if(margin == null) {
             return new Insets(0,0,0,0);
         } else {
             return margin;
         }
     }

     /**
      * Gets the <code>borderPainted</code> property.
      *
      * @return the value of the <code>borderPainted</code> property
      * @see #setBorderPainted
      */
     public boolean isBorderPainted()
     {
         return paintBorder;
     }


     /**
      * Sets the <code>borderPainted</code> property, which is
      * <code>true</code> if the border should be painted.
      * The default value for this property is <code>true</code>.
      * Some look and feels might not implement painted borders;
      * they will ignore this property.
      *
      * @param b if true, the border is painted
      * @see #isBorderPainted
      */
     @BeanProperty(expert = true, description
             = "Does the tool bar paint its borders?")
     public void setBorderPainted(boolean b)
     {
         if ( paintBorder != b )
         {
             boolean old = paintBorder;
             paintBorder = b;
             firePropertyChange("borderPainted", old, b);
             revalidate();
             repaint();
         }
     }

     /**
      * Paints the tool bar's border if the <code>borderPainted</code> property
      * is <code>true</code>.
      *
      * @param g  the <code>Graphics</code> context in which the painting
      *         is done
      * @see JComponent#paint
      * @see JComponent#setBorder
      */
     protected void paintBorder(Graphics g)
     {
         if (isBorderPainted())
         {
             super.paintBorder(g);
         }
     }

    /**
     * Gets the <code>floatable</code> property.
     *
     * @return the value of the <code>floatable</code> property
     *
     * @see #setFloatable
     */
    public boolean isFloatable()
    {
        return floatable;
    }

     /**
      * Sets the <code>floatable</code> property,
      * which must be <code>true</code> for the user to move the tool bar.
      * Typically, a floatable tool bar can be
      * dragged into a different position within the same container
      * or out into its own window.
      * The default value of this property is <code>true</code>.
      * Some look and feels might not implement floatable tool bars;
      * they will ignore this property.
      *
      * @param b if <code>true</code>, the tool bar can be moved;
      *          <code>false</code> otherwise
      * @see #isFloatable
      */
     @BeanProperty(preferred = true, description
             = "Can the tool bar be made to float by the user?")
    public void setFloatable( boolean b )
    {
        if ( floatable != b )
        {
            boolean old = floatable;
            floatable = b;

            firePropertyChange("floatable", old, b);
            revalidate();
            repaint();
        }
    }

    /**
     * Returns the current orientation of the tool bar.  The value is either
     * <code>HORIZONTAL</code> or <code>VERTICAL</code>.
     *
     * @return an integer representing the current orientation -- either
     *          <code>HORIZONTAL</code> or <code>VERTICAL</code>
     * @see #setOrientation
     */
    public int getOrientation()
    {
        return this.orientation;
    }

    /**
     * Sets the orientation of the tool bar.  The orientation must have
     * either the value <code>HORIZONTAL</code> or <code>VERTICAL</code>.
     * If <code>orientation</code> is
     * an invalid value, an exception will be thrown.
     *
     * @param o  the new orientation -- either <code>HORIZONTAL</code> or
     *                  <code>VERTICAL</code>
     * @exception IllegalArgumentException if orientation is neither
     *          <code>HORIZONTAL</code> nor <code>VERTICAL</code>
     * @see #getOrientation
     */
    @BeanProperty(preferred = true, enumerationValues = {
            "SwingConstants.HORIZONTAL",
            "SwingConstants.VERTICAL"}, description
            = "The current orientation of the tool bar")
    public void setOrientation( int o )
    {
        checkOrientation( o );

        if ( orientation != o )
        {
            int old = orientation;
            orientation = o;

            firePropertyChange("orientation", old, o);
            revalidate();
            repaint();
        }
    }

    /**
     * Sets the rollover state of this toolbar. If the rollover state is true
     * then the border of the toolbar buttons will be drawn only when the
     * mouse pointer hovers over them. The default value of this property
     * is false.
     * <p>
     * The implementation of a look and feel may choose to ignore this
     * property.
     *
     * @param rollover true for rollover toolbar buttons; otherwise false
     * @since 1.4
     */
    @BeanProperty(preferred = true, visualUpdate = true, description
            = "Will draw rollover button borders in the toolbar.")
    public void setRollover(boolean rollover) {
        putClientProperty("JToolBar.isRollover",
                          rollover ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Returns the rollover state.
     *
     * @return true if rollover toolbar buttons are to be drawn; otherwise false
     * @see #setRollover(boolean)
     * @since 1.4
     */
    public boolean isRollover() {
        Boolean rollover = (Boolean)getClientProperty("JToolBar.isRollover");
        if (rollover != null) {
            return rollover.booleanValue();
        }
        return false;
    }

    private void checkOrientation( int orientation )
    {
        switch ( orientation )
        {
            case VERTICAL:
            case HORIZONTAL:
                break;
            default:
                throw new IllegalArgumentException( "orientation must be one of: VERTICAL, HORIZONTAL" );
        }
    }

    /**
     * Appends a separator of default size to the end of the tool bar.
     * The default size is determined by the current look and feel.
     */
    public void addSeparator()
    {
        addSeparator(null);
    }

    /**
     * Appends a separator of a specified size to the end
     * of the tool bar.
     *
     * @param size the <code>Dimension</code> of the separator
     */
    public void addSeparator( Dimension size )
    {
        JToolBar.Separator s = new JToolBar.Separator( size );
        add(s);
    }

    /**
     * Adds a new <code>JButton</code> which dispatches the action.
     *
     * @param a the <code>Action</code> object to add as a new menu item
     * @return the new button which dispatches the action
     */
    public JButton add(Action a) {
        JButton b = createActionComponent(a);
        b.setAction(a);
        add(b);
        return b;
    }

    /**
     * Factory method which creates the <code>JButton</code> for
     * <code>Action</code>s added to the <code>JToolBar</code>.
     * The default name is empty if a <code>null</code> action is passed.
     *
     * @param a the <code>Action</code> for the button to be added
     * @return the newly created button
     * @see Action
     * @since 1.3
     */
    protected JButton createActionComponent(Action a) {
        JButton b = new JButton() {
            protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
                PropertyChangeListener pcl = createActionChangeListener(this);
                if (pcl==null) {
                    pcl = super.createActionPropertyChangeListener(a);
                }
                return pcl;
            }
        };
        if (a != null && (a.getValue(Action.SMALL_ICON) != null ||
                          a.getValue(Action.LARGE_ICON_KEY) != null)) {
            b.setHideActionText(true);
        }
        b.setHorizontalTextPosition(JButton.CENTER);
        b.setVerticalTextPosition(JButton.BOTTOM);
        return b;
    }

    /**
     * Returns a properly configured <code>PropertyChangeListener</code>
     * which updates the control as changes to the <code>Action</code> occur,
     * or <code>null</code> if the default
     * property change listener for the control is desired.
     *
     * @param b a {@code JButton}
     * @return {@code null}
     */
    protected PropertyChangeListener createActionChangeListener(JButton b) {
        return null;
    }

    /**
     * If a <code>JButton</code> is being added, it is initially
     * set to be disabled.
     *
     * @param comp  the component to be enhanced
     * @param constraints  the constraints to be enforced on the component
     * @param index the index of the component
     *
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        if (comp instanceof Separator) {
            if (getOrientation() == VERTICAL) {
                ( (Separator)comp ).setOrientation(JSeparator.HORIZONTAL);
            } else {
                ( (Separator)comp ).setOrientation(JSeparator.VERTICAL);
            }
        }
        super.addImpl(comp, constraints, index);
        if (comp instanceof JButton) {
            ((JButton)comp).setDefaultCapable(false);
        }
    }


    /**
     * A toolbar-specific separator. An object with dimension but
     * no contents used to divide buttons on a tool bar into groups.
     */
    public static class Separator extends JSeparator
    {
        private Dimension separatorSize;

        /**
         * Creates a new toolbar separator with the default size
         * as defined by the current look and feel.
         */
        public Separator()
        {
            this( null );  // let the UI define the default size
        }

        /**
         * Creates a new toolbar separator with the specified size.
         *
         * @param size the <code>Dimension</code> of the separator
         */
        public Separator( Dimension size )
        {
            super( JSeparator.HORIZONTAL );
            setSeparatorSize(size);
        }

        /**
         * Returns the name of the L&amp;F class that renders this component.
         *
         * @return the string "ToolBarSeparatorUI"
         * @see JComponent#getUIClassID
         * @see UIDefaults#getUI
         */
        public String getUIClassID()
        {
            return "ToolBarSeparatorUI";
        }

        /**
         * Sets the size of the separator.
         *
         * @param size the new <code>Dimension</code> of the separator
         */
        public void setSeparatorSize( Dimension size )
        {
            if (size != null) {
                separatorSize = size;
            } else {
                super.updateUI();
            }
            this.invalidate();
        }

        /**
         * Returns the size of the separator
         *
         * @return the <code>Dimension</code> object containing the separator's
         *         size (This is a reference, NOT a copy!)
         */
        public Dimension getSeparatorSize()
        {
            return separatorSize;
        }

        /**
         * Returns the minimum size for the separator.
         *
         * @return the <code>Dimension</code> object containing the separator's
         *         minimum size
         */
        public Dimension getMinimumSize()
        {
            if (separatorSize != null) {
                return separatorSize.getSize();
            } else {
                return super.getMinimumSize();
            }
        }

        /**
         * Returns the maximum size for the separator.
         *
         * @return the <code>Dimension</code> object containing the separator's
         *         maximum size
         */
        public Dimension getMaximumSize()
        {
            if (separatorSize != null) {
                return separatorSize.getSize();
            } else {
                return super.getMaximumSize();
            }
        }

        /**
         * Returns the preferred size for the separator.
         *
         * @return the <code>Dimension</code> object containing the separator's
         *         preferred size
         */
        public Dimension getPreferredSize()
        {
            if (separatorSize != null) {
                return separatorSize.getSize();
            } else {
                return super.getPreferredSize();
            }
        }
    }


    /**
     * See <code>readObject</code> and <code>writeObject</code> in
     * <code>JComponent</code> for more
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
     * Returns a string representation of this <code>JToolBar</code>.
     * This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JToolBar</code>.
     */
    protected String paramString() {
        String paintBorderString = (paintBorder ?
                                    "true" : "false");
        String marginString = (margin != null ?
                               margin.toString() : "");
        String floatableString = (floatable ?
                                  "true" : "false");
        String orientationString = (orientation == HORIZONTAL ?
                                    "HORIZONTAL" : "VERTICAL");

        return super.paramString() +
        ",floatable=" + floatableString +
        ",margin=" + marginString +
        ",orientation=" + orientationString +
        ",paintBorder=" + paintBorderString;
    }


    private class DefaultToolBarLayout
        implements LayoutManager2, Serializable, PropertyChangeListener, UIResource {

        BoxLayout lm;

        DefaultToolBarLayout(int orientation) {
            if (orientation == JToolBar.VERTICAL) {
                lm = new BoxLayout(JToolBar.this, BoxLayout.PAGE_AXIS);
            } else {
                lm = new BoxLayout(JToolBar.this, BoxLayout.LINE_AXIS);
            }
        }

        public void addLayoutComponent(String name, Component comp) {
            lm.addLayoutComponent(name, comp);
        }

        public void addLayoutComponent(Component comp, Object constraints) {
            lm.addLayoutComponent(comp, constraints);
        }

        public void removeLayoutComponent(Component comp) {
            lm.removeLayoutComponent(comp);
        }

        public Dimension preferredLayoutSize(Container target) {
            return lm.preferredLayoutSize(target);
        }

        public Dimension minimumLayoutSize(Container target) {
            return lm.minimumLayoutSize(target);
        }

        public Dimension maximumLayoutSize(Container target) {
            return lm.maximumLayoutSize(target);
        }

        public void layoutContainer(Container target) {
            lm.layoutContainer(target);
        }

        public float getLayoutAlignmentX(Container target) {
            return lm.getLayoutAlignmentX(target);
        }

        public float getLayoutAlignmentY(Container target) {
            return lm.getLayoutAlignmentY(target);
        }

        public void invalidateLayout(Container target) {
            lm.invalidateLayout(target);
        }

        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();
            if( name.equals("orientation") ) {
                int o = ((Integer)e.getNewValue()).intValue();

                if (o == JToolBar.VERTICAL)
                    lm = new BoxLayout(JToolBar.this, BoxLayout.PAGE_AXIS);
                else {
                    lm = new BoxLayout(JToolBar.this, BoxLayout.LINE_AXIS);
                }
            }
        }
    }


    public void setLayout(LayoutManager mgr) {
        LayoutManager oldMgr = getLayout();
        if (oldMgr instanceof PropertyChangeListener) {
            removePropertyChangeListener((PropertyChangeListener)oldMgr);
        }
        super.setLayout(mgr);
    }

/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this JToolBar.
     * For tool bars, the AccessibleContext takes the form of an
     * AccessibleJToolBar.
     * A new AccessibleJToolBar instance is created if necessary.
     *
     * @return an AccessibleJToolBar that serves as the
     *         AccessibleContext of this JToolBar
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJToolBar();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JToolBar</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to toolbar user-interface elements.
     */
    protected class AccessibleJToolBar extends AccessibleJComponent {

        /**
         * Constructs an {@code AccessibleJToolBar}.
         */
        protected AccessibleJToolBar() {}

        /**
         * Get the state of this object.
         *
         * @return an instance of AccessibleStateSet containing the current
         * state set of the object
         * @see AccessibleState
         */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = super.getAccessibleStateSet();
            // FIXME:  [[[WDW - need to add orientation from BoxLayout]]]
            // FIXME:  [[[WDW - need to do SELECTABLE if SelectionModel is added]]]
            return states;
        }

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the object
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.TOOL_BAR;
        }
    } // inner class AccessibleJToolBar
}
