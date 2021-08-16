/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.basic;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.accessibility.*;
import javax.swing.plaf.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import sun.awt.AppContext;
import sun.swing.DefaultLookup;
import sun.swing.SwingUtilities2;
import sun.swing.UIAction;

/**
 * Basic UI implementation for JComboBox.
 * <p>
 * The combo box is a compound component which means that it is an aggregate of
 * many simpler components. This class creates and manages the listeners
 * on the combo box and the combo box model. These listeners update the user
 * interface in response to changes in the properties and state of the combo box.
 * <p>
 * All event handling is handled by listener classes created with the
 * <code>createxxxListener()</code> methods and internal classes.
 * You can change the behavior of this class by overriding the
 * <code>createxxxListener()</code> methods and supplying your own
 * event listeners or subclassing from the ones supplied in this class.
 * <p>
 * For adding specific actions,
 * overide <code>installKeyboardActions</code> to add actions in response to
 * KeyStroke bindings. See the article <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/keybinding.html">How to Use Key Bindings</a>
 *
 * @author Arnaud Weber
 * @author Tom Santos
 * @author Mark Davidson
 */
public class BasicComboBoxUI extends ComboBoxUI {

    /**
     * The instance of {@code JComboBox}.
     */
    protected JComboBox<Object> comboBox;
    /**
     * This protected field is implementation specific. Do not access directly
     * or override.
     */
    protected boolean   hasFocus = false;

    // Control the selection behavior of the JComboBox when it is used
    // in the JTable DefaultCellEditor.
    private boolean isTableCellEditor = false;
    private static final String IS_TABLE_CELL_EDITOR = "JComboBox.isTableCellEditor";

    /**
     * This list is for drawing the current item in the combo box.
     */
    protected JList<Object>   listBox;

    /**
     * Used to render the currently selected item in the combo box.
     * It doesn't have anything to do with the popup's rendering.
     */
    protected CellRendererPane currentValuePane = new CellRendererPane();

    /**
     * The implementation of {@code ComboPopup} that is used to show the popup.
     */
    protected ComboPopup popup;

    /**
     * The Component that the @{code ComboBoxEditor} uses for editing.
     */
    protected Component editor;

    /**
     * The arrow button that invokes the popup.
     */
    protected JButton   arrowButton;

    // Listeners that are attached to the JComboBox
    /**
     * This protected field is implementation specific. Do not access directly
     * or override. Override the listener construction method instead.
     *
     * @see #createKeyListener
     */
    protected KeyListener keyListener;
    /**
     * This protected field is implementation specific. Do not access directly
     * or override. Override the listener construction method instead.
     *
     * @see #createFocusListener
     */
    protected FocusListener focusListener;
    /**
     * This protected field is implementation specific. Do not access directly
     * or override. Override the listener construction method instead.
     *
     * @see #createPropertyChangeListener
     */
    protected PropertyChangeListener propertyChangeListener;

    /**
     * This protected field is implementation specific. Do not access directly
     * or override. Override the listener construction method instead.
     *
     * @see #createItemListener
     */
    protected ItemListener itemListener;

    // Listeners that the ComboPopup produces.
    /**
     * The {@code MouseListener} listens to events.
     */
    protected MouseListener popupMouseListener;

    /**
     * The {@code MouseMotionListener} listens to events.
     */
    protected MouseMotionListener popupMouseMotionListener;

    /**
     * The {@code KeyListener} listens to events.
     */
    protected KeyListener popupKeyListener;

    // This is used for knowing when to cache the minimum preferred size.
    // If the data in the list changes, the cached value get marked for recalc.
    // Added to the current JComboBox model
    /**
     * This protected field is implementation specific. Do not access directly
     * or override. Override the listener construction method instead.
     *
     * @see #createListDataListener
     */
    protected ListDataListener listDataListener;

    /**
     * Implements all the Listeners needed by this class, all existing
     * listeners redirect to it.
     */
    private Handler handler;

    /**
     * The time factor to treate the series of typed alphanumeric key
     * as prefix for first letter navigation.
     */
    private long timeFactor = 1000L;

    /**
     * This is tricky, this variables is needed for DefaultKeySelectionManager
     * to take into account time factor.
     */
    private long lastTime = 0L;
    private long time = 0L;

    /**
     * The default key selection manager
     */
    JComboBox.KeySelectionManager keySelectionManager;

    /**
     * The flag for recalculating the minimum preferred size.
     */
    protected boolean isMinimumSizeDirty = true;

    /**
     * The cached minimum preferred size.
     */
    protected Dimension cachedMinimumSize = new Dimension( 0, 0 );

    // Flag for calculating the display size
    private boolean isDisplaySizeDirty = true;

    // Cached the size that the display needs to render the largest item
    private Dimension cachedDisplaySize = new Dimension( 0, 0 );

    // Key used for lookup of the DefaultListCellRenderer in the AppContext.
    private static final Object COMBO_UI_LIST_CELL_RENDERER_KEY =
                        new StringBuffer("DefaultListCellRendererKey");

    static final StringBuffer HIDE_POPUP_KEY
                  = new StringBuffer("HidePopupKey");

    /**
     * Whether or not all cells have the same baseline.
     */
    private boolean sameBaseline;

    /**
     * Indicates whether or not the combo box button should be square.
     * If square, then the width and height are equal, and are both set to
     * the height of the combo minus appropriate insets.
     *
     * @since 1.7
     */
    protected boolean squareButton = true;

    /**
     * If specified, these insets act as padding around the cell renderer when
     * laying out and painting the "selected" item in the combo box. These
     * insets add to those specified by the cell renderer.
     *
     * @since 1.7
     */
    protected Insets padding;

    /**
     * Constructs a {@code BasicComboBoxUI}.
     */
    public BasicComboBoxUI() {}

    // Used for calculating the default size.
    private static ListCellRenderer<Object> getDefaultListCellRenderer() {
        @SuppressWarnings("unchecked")
        ListCellRenderer<Object> renderer = (ListCellRenderer)AppContext.
                         getAppContext().get(COMBO_UI_LIST_CELL_RENDERER_KEY);

        if (renderer == null) {
            renderer = new DefaultListCellRenderer();
            AppContext.getAppContext().put(COMBO_UI_LIST_CELL_RENDERER_KEY,
                                           new DefaultListCellRenderer());
        }
        return renderer;
    }

    /**
     * Populates ComboBox's actions.
     */
    static void loadActionMap(LazyActionMap map) {
        map.put(new Actions(Actions.HIDE));
        map.put(new Actions(Actions.PAGE_DOWN));
        map.put(new Actions(Actions.PAGE_UP));
        map.put(new Actions(Actions.HOME));
        map.put(new Actions(Actions.END));
        map.put(new Actions(Actions.DOWN));
        map.put(new Actions(Actions.DOWN_2));
        map.put(new Actions(Actions.TOGGLE));
        map.put(new Actions(Actions.TOGGLE_2));
        map.put(new Actions(Actions.UP));
        map.put(new Actions(Actions.UP_2));
        map.put(new Actions(Actions.ENTER));
    }

    //========================
    // begin UI Initialization
    //

    /**
     * Constructs a new instance of {@code BasicComboBoxUI}.
     *
     * @param c a component
     * @return a new instance of {@code BasicComboBoxUI}
     */
    public static ComponentUI createUI(JComponent c) {
        return new BasicComboBoxUI();
    }

    @Override
    public void installUI( JComponent c ) {
        isMinimumSizeDirty = true;

        @SuppressWarnings("unchecked")
        JComboBox<Object> tmp = (JComboBox)c;
        comboBox = tmp;
        installDefaults();
        popup = createPopup();
        listBox = popup.getList();

        // Is this combo box a cell editor?
        Boolean inTable = (Boolean)c.getClientProperty(IS_TABLE_CELL_EDITOR );
        if (inTable != null) {
            isTableCellEditor = inTable.equals(Boolean.TRUE) ? true : false;
        }

        if ( comboBox.getRenderer() == null || comboBox.getRenderer() instanceof UIResource ) {
            comboBox.setRenderer( createRenderer() );
        }

        if ( comboBox.getEditor() == null || comboBox.getEditor() instanceof UIResource ) {
            comboBox.setEditor( createEditor() );
        }

        installListeners();
        installComponents();

        comboBox.setLayout( createLayoutManager() );

        comboBox.setRequestFocusEnabled( true );

        installKeyboardActions();

        comboBox.putClientProperty("doNotCancelPopup", HIDE_POPUP_KEY);

        if (keySelectionManager == null || keySelectionManager instanceof UIResource) {
            keySelectionManager = new DefaultKeySelectionManager();
        }
        comboBox.setKeySelectionManager(keySelectionManager);
    }

    @Override
    public void uninstallUI( JComponent c ) {
        setPopupVisible( comboBox, false);
        popup.uninstallingUI();

        uninstallKeyboardActions();

        comboBox.setLayout( null );

        uninstallComponents();
        uninstallListeners();
        uninstallDefaults();

        if ( comboBox.getRenderer() == null || comboBox.getRenderer() instanceof UIResource ) {
            comboBox.setRenderer( null );
        }

        ComboBoxEditor comboBoxEditor = comboBox.getEditor();
        if (comboBoxEditor instanceof UIResource ) {
            if (comboBoxEditor.getEditorComponent().hasFocus()) {
                // Leave focus in JComboBox.
                comboBox.requestFocusInWindow();
            }
            comboBox.setEditor( null );
        }

        if (keySelectionManager instanceof UIResource) {
            comboBox.setKeySelectionManager(null);
        }

        handler = null;
        keyListener = null;
        focusListener = null;
        listDataListener = null;
        propertyChangeListener = null;
        popup = null;
        listBox = null;
        comboBox = null;
    }

    /**
     * Installs the default colors, default font, default renderer, and default
     * editor into the JComboBox.
     */
    protected void installDefaults() {
        LookAndFeel.installColorsAndFont( comboBox,
                                          "ComboBox.background",
                                          "ComboBox.foreground",
                                          "ComboBox.font" );
        LookAndFeel.installBorder( comboBox, "ComboBox.border" );
        LookAndFeel.installProperty( comboBox, "opaque", Boolean.TRUE);

        Long l = (Long)UIManager.get("ComboBox.timeFactor");
        timeFactor = l == null ? 1000L : l.longValue();

        //NOTE: this needs to default to true if not specified
        Boolean b = (Boolean)UIManager.get("ComboBox.squareButton");
        squareButton = b == null ? true : b;

        padding = UIManager.getInsets("ComboBox.padding");
    }

    /**
     * Creates and installs listeners for the combo box and its model.
     * This method is called when the UI is installed.
     */
    protected void installListeners() {
        if ( (itemListener = createItemListener()) != null) {
            comboBox.addItemListener( itemListener );
        }
        if ( (propertyChangeListener = createPropertyChangeListener()) != null ) {
            comboBox.addPropertyChangeListener( propertyChangeListener );
        }
        if ( (keyListener = createKeyListener()) != null ) {
            comboBox.addKeyListener( keyListener );
        }
        if ( (focusListener = createFocusListener()) != null ) {
            comboBox.addFocusListener( focusListener );
        }
        if ((popupMouseListener = popup.getMouseListener()) != null) {
            comboBox.addMouseListener( popupMouseListener );
        }
        if ((popupMouseMotionListener = popup.getMouseMotionListener()) != null) {
            comboBox.addMouseMotionListener( popupMouseMotionListener );
        }
        if ((popupKeyListener = popup.getKeyListener()) != null) {
            comboBox.addKeyListener(popupKeyListener);
        }

        if ( comboBox.getModel() != null ) {
            if ( (listDataListener = createListDataListener()) != null ) {
                comboBox.getModel().addListDataListener( listDataListener );
            }
        }
    }

    /**
     * Uninstalls the default colors, default font, default renderer,
     * and default editor from the combo box.
     */
    protected void uninstallDefaults() {
        LookAndFeel.installColorsAndFont( comboBox,
                                          "ComboBox.background",
                                          "ComboBox.foreground",
                                          "ComboBox.font" );
        LookAndFeel.uninstallBorder( comboBox );
    }

    /**
     * Removes the installed listeners from the combo box and its model.
     * The number and types of listeners removed and in this method should be
     * the same that was added in <code>installListeners</code>
     */
    protected void uninstallListeners() {
        if ( keyListener != null ) {
            comboBox.removeKeyListener( keyListener );
        }
        if ( itemListener != null) {
            comboBox.removeItemListener( itemListener );
        }
        if ( propertyChangeListener != null ) {
            comboBox.removePropertyChangeListener( propertyChangeListener );
        }
        if ( focusListener != null) {
            comboBox.removeFocusListener( focusListener );
        }
        if ( popupMouseListener != null) {
            comboBox.removeMouseListener( popupMouseListener );
        }
        if ( popupMouseMotionListener != null) {
            comboBox.removeMouseMotionListener( popupMouseMotionListener );
        }
        if (popupKeyListener != null) {
            comboBox.removeKeyListener(popupKeyListener);
        }
        if ( comboBox.getModel() != null ) {
            if ( listDataListener != null ) {
                comboBox.getModel().removeListDataListener( listDataListener );
            }
        }
    }

    /**
     * Creates the popup portion of the combo box.
     *
     * @return an instance of <code>ComboPopup</code>
     * @see ComboPopup
     */
    protected ComboPopup createPopup() {
        return new BasicComboPopup( comboBox );
    }

    /**
     * Creates a <code>KeyListener</code> which will be added to the
     * combo box. If this method returns null then it will not be added
     * to the combo box.
     *
     * @return an instance <code>KeyListener</code> or null
     */
    protected KeyListener createKeyListener() {
        return getHandler();
    }

    /**
     * Creates a <code>FocusListener</code> which will be added to the combo box.
     * If this method returns null then it will not be added to the combo box.
     *
     * @return an instance of a <code>FocusListener</code> or null
     */
    protected FocusListener createFocusListener() {
        return getHandler();
    }

    /**
     * Creates a list data listener which will be added to the
     * <code>ComboBoxModel</code>. If this method returns null then
     * it will not be added to the combo box model.
     *
     * @return an instance of a <code>ListDataListener</code> or null
     */
    protected ListDataListener createListDataListener() {
        return getHandler();
    }

    /**
     * Creates an <code>ItemListener</code> which will be added to the
     * combo box. If this method returns null then it will not
     * be added to the combo box.
     * <p>
     * Subclasses may override this method to return instances of their own
     * ItemEvent handlers.
     *
     * @return an instance of an <code>ItemListener</code> or null
     */
    protected ItemListener createItemListener() {
        return null;
    }

    /**
     * Creates a <code>PropertyChangeListener</code> which will be added to
     * the combo box. If this method returns null then it will not
     * be added to the combo box.
     *
     * @return an instance of a <code>PropertyChangeListener</code> or null
     */
    protected PropertyChangeListener createPropertyChangeListener() {
        return getHandler();
    }

    /**
     * Creates a layout manager for managing the components which make up the
     * combo box.
     *
     * @return an instance of a layout manager
     */
    protected LayoutManager createLayoutManager() {
        return getHandler();
    }

    /**
     * Creates the default renderer that will be used in a non-editiable combo
     * box. A default renderer will used only if a renderer has not been
     * explicitly set with <code>setRenderer</code>.
     *
     * @return a <code>ListCellRender</code> used for the combo box
     * @see javax.swing.JComboBox#setRenderer
     */
    protected ListCellRenderer<Object> createRenderer() {
        return new BasicComboBoxRenderer.UIResource();
    }

    /**
     * Creates the default editor that will be used in editable combo boxes.
     * A default editor will be used only if an editor has not been
     * explicitly set with <code>setEditor</code>.
     *
     * @return a <code>ComboBoxEditor</code> used for the combo box
     * @see javax.swing.JComboBox#setEditor
     */
    protected ComboBoxEditor createEditor() {
        return new BasicComboBoxEditor.UIResource();
    }

    /**
     * Returns the shared listener.
     */
    private Handler getHandler() {
        if (handler == null) {
            handler = new Handler();
        }
        return handler;
    }

    //
    // end UI Initialization
    //======================


    //======================
    // begin Inner classes
    //

    /**
     * This listener checks to see if the key event isn't a navigation key.  If
     * it finds a key event that wasn't a navigation key it dispatches it to
     * JComboBox.selectWithKeyChar() so that it can do type-ahead.
     *
     * This public inner class should be treated as protected.
     * Instantiate it only within subclasses of
     * <code>BasicComboBoxUI</code>.
     */
    public class KeyHandler extends KeyAdapter {
        /**
         * Constructs a {@code KeyHandler}.
         */
        public KeyHandler() {}

        @Override
        public void keyPressed( KeyEvent e ) {
            getHandler().keyPressed(e);
        }
    }

    /**
     * This listener hides the popup when the focus is lost.  It also repaints
     * when focus is gained or lost.
     *
     * This public inner class should be treated as protected.
     * Instantiate it only within subclasses of
     * <code>BasicComboBoxUI</code>.
     */
    public class FocusHandler implements FocusListener {
        /**
         * Constructs a {@code FocusHandler}.
         */
        public FocusHandler() {}

        public void focusGained( FocusEvent e ) {
            getHandler().focusGained(e);
        }

        public void focusLost( FocusEvent e ) {
            getHandler().focusLost(e);
        }
    }

    /**
     * This listener watches for changes in the
     * <code>ComboBoxModel</code>.
     * <p>
     * This public inner class should be treated as protected.
     * Instantiate it only within subclasses of
     * <code>BasicComboBoxUI</code>.
     *
     * @see #createListDataListener
     */
    public class ListDataHandler implements ListDataListener {
        /**
         * Constructs a {@code ListDataHandler}.
         */
        public ListDataHandler() {}

        public void contentsChanged( ListDataEvent e ) {
            getHandler().contentsChanged(e);
        }

        public void intervalAdded( ListDataEvent e ) {
            getHandler().intervalAdded(e);
        }

        public void intervalRemoved( ListDataEvent e ) {
            getHandler().intervalRemoved(e);
        }
    }

    /**
     * This listener watches for changes to the selection in the
     * combo box.
     * <p>
     * This public inner class should be treated as protected.
     * Instantiate it only within subclasses of
     * <code>BasicComboBoxUI</code>.
     *
     * @see #createItemListener
     */
    public class ItemHandler implements ItemListener {
        /**
         * Constructs a {@code ItemHandler}.
         */
        public ItemHandler() {}

        // This class used to implement behavior which is now redundant.
        public void itemStateChanged(ItemEvent e) {}
    }

    /**
     * This listener watches for bound properties that have changed in the
     * combo box.
     * <p>
     * Subclasses which wish to listen to combo box property changes should
     * call the superclass methods to ensure that the combo box ui correctly
     * handles property changes.
     * <p>
     * This public inner class should be treated as protected.
     * Instantiate it only within subclasses of
     * <code>BasicComboBoxUI</code>.
     *
     * @see #createPropertyChangeListener
     */
    public class PropertyChangeHandler implements PropertyChangeListener {
        /**
         * Constructs a {@code PropertyChangeHandler}.
         */
        public PropertyChangeHandler() {}

        public void propertyChange(PropertyChangeEvent e) {
            getHandler().propertyChange(e);
        }
    }


    // Syncronizes the ToolTip text for the components within the combo box to be the
    // same value as the combo box ToolTip text.
    private void updateToolTipTextForChildren() {
        Component[] children = comboBox.getComponents();
        for ( int i = 0; i < children.length; ++i ) {
            if ( children[i] instanceof JComponent ) {
                ((JComponent)children[i]).setToolTipText( comboBox.getToolTipText() );
            }
        }
    }

    /**
     * This layout manager handles the 'standard' layout of combo boxes.  It puts
     * the arrow button to the right and the editor to the left.  If there is no
     * editor it still keeps the arrow button to the right.
     *
     * This public inner class should be treated as protected.
     * Instantiate it only within subclasses of
     * <code>BasicComboBoxUI</code>.
     */
    public class ComboBoxLayoutManager implements LayoutManager {
        /**
         * Constructs a {@code ComboBoxLayoutManager}.
         */
        public ComboBoxLayoutManager() {}

        public void addLayoutComponent(String name, Component comp) {}

        public void removeLayoutComponent(Component comp) {}

        public Dimension preferredLayoutSize(Container parent) {
            return getHandler().preferredLayoutSize(parent);
        }

        public Dimension minimumLayoutSize(Container parent) {
            return getHandler().minimumLayoutSize(parent);
        }

        public void layoutContainer(Container parent) {
            getHandler().layoutContainer(parent);
        }
    }

    //
    // end Inner classes
    //====================


    //===============================
    // begin Sub-Component Management
    //

    /**
     * Creates and initializes the components which make up the
     * aggregate combo box. This method is called as part of the UI
     * installation process.
     */
    protected void installComponents() {
        arrowButton = createArrowButton();

        if (arrowButton != null)  {
            comboBox.add(arrowButton);
            configureArrowButton();
        }

        if ( comboBox.isEditable() ) {
            addEditor();
        }

        comboBox.add( currentValuePane );
    }

    /**
     * The aggregate components which comprise the combo box are
     * unregistered and uninitialized. This method is called as part of the
     * UI uninstallation process.
     */
    protected void uninstallComponents() {
        if ( arrowButton != null ) {
            unconfigureArrowButton();
        }
        if ( editor != null ) {
            unconfigureEditor();
        }
        comboBox.removeAll(); // Just to be safe.
        arrowButton = null;
    }

    /**
     * This public method is implementation specific and should be private.
     * do not call or override. To implement a specific editor create a
     * custom <code>ComboBoxEditor</code>
     *
     * @see #createEditor
     * @see javax.swing.JComboBox#setEditor
     * @see javax.swing.ComboBoxEditor
     */
    public void addEditor() {
        removeEditor();
        editor = comboBox.getEditor().getEditorComponent();
        if ( editor != null ) {
            configureEditor();
            comboBox.add(editor);
            if(comboBox.isFocusOwner()) {
                // Switch focus to the editor component
                editor.requestFocusInWindow();
            }
        }
    }

    /**
     * This public method is implementation specific and should be private.
     * do not call or override.
     *
     * @see #addEditor
     */
    public void removeEditor() {
        if ( editor != null ) {
            unconfigureEditor();
            comboBox.remove( editor );
            editor = null;
        }
    }

    /**
     * This protected method is implementation specific and should be private.
     * do not call or override.
     *
     * @see #addEditor
     */
    protected void configureEditor() {
        // Should be in the same state as the combobox
        editor.setEnabled(comboBox.isEnabled());

        editor.setFocusable(comboBox.isFocusable());

        editor.setFont( comboBox.getFont() );

        if (focusListener != null) {
            editor.addFocusListener(focusListener);
        }

        editor.addFocusListener( getHandler() );

        comboBox.getEditor().addActionListener(getHandler());

        if(editor instanceof JComponent) {
            ((JComponent)editor).putClientProperty("doNotCancelPopup",
                                                   HIDE_POPUP_KEY);
            ((JComponent)editor).setInheritsPopupMenu(true);
        }

        comboBox.configureEditor(comboBox.getEditor(),comboBox.getSelectedItem());

        editor.addPropertyChangeListener(propertyChangeListener);
    }

    /**
     * This protected method is implementation specific and should be private.
     * Do not call or override.
     *
     * @see #addEditor
     */
    protected void unconfigureEditor() {
        if (focusListener != null) {
            editor.removeFocusListener(focusListener);
        }

        editor.removePropertyChangeListener(propertyChangeListener);
        editor.removeFocusListener(getHandler());
        comboBox.getEditor().removeActionListener(getHandler());
    }

    /**
     * This public method is implementation specific and should be private. Do
     * not call or override.
     *
     * @see #createArrowButton
     */
    public void configureArrowButton() {
        if ( arrowButton != null ) {
            arrowButton.setEnabled( comboBox.isEnabled() );
            arrowButton.setFocusable(comboBox.isFocusable());
            arrowButton.setRequestFocusEnabled(false);
            arrowButton.addMouseListener( popup.getMouseListener() );
            arrowButton.addMouseMotionListener( popup.getMouseMotionListener() );
            arrowButton.resetKeyboardActions();
            arrowButton.putClientProperty("doNotCancelPopup", HIDE_POPUP_KEY);
            arrowButton.setInheritsPopupMenu(true);
        }
    }

    /**
     * This public method is implementation specific and should be private. Do
     * not call or override.
     *
     * @see #createArrowButton
     */
    public void unconfigureArrowButton() {
        if ( arrowButton != null ) {
            arrowButton.removeMouseListener( popup.getMouseListener() );
            arrowButton.removeMouseMotionListener( popup.getMouseMotionListener() );
        }
    }

    /**
     * Creates a button which will be used as the control to show or hide
     * the popup portion of the combo box.
     *
     * @return a button which represents the popup control
     */
    protected JButton createArrowButton() {
        JButton button = new BasicArrowButton(BasicArrowButton.SOUTH,
                                    UIManager.getColor("ComboBox.buttonBackground"),
                                    UIManager.getColor("ComboBox.buttonShadow"),
                                    UIManager.getColor("ComboBox.buttonDarkShadow"),
                                    UIManager.getColor("ComboBox.buttonHighlight"));
        button.setName("ComboBox.arrowButton");
        return button;
    }

    //
    // end Sub-Component Management
    //===============================


    //================================
    // begin ComboBoxUI Implementation
    //

    /**
     * Tells if the popup is visible or not.
     */
    public boolean isPopupVisible( JComboBox<?> c ) {
        return popup != null && popup.isVisible();
    }

    /**
     * Hides the popup.
     */
    public void setPopupVisible( JComboBox<?> c, boolean v ) {
        if (popup != null) {
            if (v) {
                popup.show();
            } else {
                popup.hide();
            }
        }
    }

    /**
     * Determines if the JComboBox is focus traversable.  If the JComboBox is editable
     * this returns false, otherwise it returns true.
     */
    public boolean isFocusTraversable( JComboBox<?> c ) {
        return !comboBox.isEditable();
    }

    //
    // end ComboBoxUI Implementation
    //==============================


    //=================================
    // begin ComponentUI Implementation
    @Override
    public void paint( Graphics g, JComponent c ) {
        hasFocus = comboBox.hasFocus();
        if ( !comboBox.isEditable() ) {
            Rectangle r = rectangleForCurrentValue();
            paintCurrentValueBackground(g,r,hasFocus);
            paintCurrentValue(g,r,hasFocus);
        }
        // Empty out the renderer pane, allowing renderers to be gc'ed.
        currentValuePane.removeAll();
    }

    @Override
    public Dimension getPreferredSize( JComponent c ) {
        return getMinimumSize(c);
    }

    /**
     * The minimum size is the size of the display area plus insets plus the button.
     */
    @Override
    public Dimension getMinimumSize( JComponent c ) {
        if ( !isMinimumSizeDirty ) {
            return new Dimension(cachedMinimumSize);
        }
        Dimension size = getDisplaySize();
        Insets insets = getInsets();
        //calculate the width and height of the button
        int buttonHeight = size.height;
        int buttonWidth = squareButton ? buttonHeight : arrowButton.getPreferredSize().width;
        //adjust the size based on the button width
        size.height += insets.top + insets.bottom;
        size.width +=  insets.left + insets.right + buttonWidth;

        cachedMinimumSize.setSize( size.width, size.height );
        isMinimumSizeDirty = false;

        return new Dimension(size);
    }

    @Override
    public Dimension getMaximumSize( JComponent c ) {
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    /**
     * Returns the baseline.
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @see javax.swing.JComponent#getBaseline(int, int)
     * @since 1.6
     */
    @Override
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        int baseline = -1;
        // force sameBaseline to be updated.
        getDisplaySize();
        if (sameBaseline) {
            Insets insets = c.getInsets();
            height = Math.max(height - insets.top - insets.bottom, 0);
            if (!comboBox.isEditable()) {
                ListCellRenderer<Object> renderer = comboBox.getRenderer();
                if (renderer == null)  {
                    renderer = new DefaultListCellRenderer();
                }
                Object value = null;
                Object prototypeValue = comboBox.getPrototypeDisplayValue();
                if (prototypeValue != null)  {
                    value = prototypeValue;
                }
                else if (comboBox.getModel().getSize() > 0) {
                    // Note, we're assuming the baseline is the same for all
                    // cells, if not, this needs to loop through all.
                    value = comboBox.getModel().getElementAt(0);
                }
                Component component = renderer.
                        getListCellRendererComponent(listBox, value, -1,
                                                     false, false);
                if (component instanceof JLabel) {
                    JLabel label = (JLabel) component;
                    String text = label.getText();
                    if ((text == null) || text.isEmpty()) {
                        label.setText(" ");
                    }
                }
                if (component instanceof JComponent) {
                    component.setFont(comboBox.getFont());
                }
                baseline = component.getBaseline(width, height);
            }
            else {
                baseline = editor.getBaseline(width, height);
            }
            if (baseline > 0) {
                baseline += insets.top;
            }
        }
        return baseline;
    }

    /**
     * Returns an enum indicating how the baseline of the component
     * changes as the size changes.
     *
     * @throws NullPointerException {@inheritDoc}
     * @see javax.swing.JComponent#getBaseline(int, int)
     * @since 1.6
     */
    @Override
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(
            JComponent c) {
        super.getBaselineResizeBehavior(c);
        // Force sameBaseline to be updated.
        getDisplaySize();
        if (comboBox.isEditable()) {
            return editor.getBaselineResizeBehavior();
        }
        else if (sameBaseline) {
            ListCellRenderer<Object> renderer = comboBox.getRenderer();
            if (renderer == null)  {
                renderer = new DefaultListCellRenderer();
            }
            Object value = null;
            Object prototypeValue = comboBox.getPrototypeDisplayValue();
            if (prototypeValue != null)  {
                value = prototypeValue;
            }
            else if (comboBox.getModel().getSize() > 0) {
                // Note, we're assuming the baseline is the same for all
                // cells, if not, this needs to loop through all.
                value = comboBox.getModel().getElementAt(0);
            }
            if (value != null) {
                Component component = renderer.
                        getListCellRendererComponent(listBox, value, -1,
                                                     false, false);
                return component.getBaselineResizeBehavior();
            }
        }
        return Component.BaselineResizeBehavior.OTHER;
    }

    // This is currently hacky...
    @Override
    public int getAccessibleChildrenCount(JComponent c) {
        if ( comboBox.isEditable() ) {
            return 2;
        }
        else {
            return 1;
        }
    }

    // This is currently hacky...
    @Override
    public Accessible getAccessibleChild(JComponent c, int i) {
        // 0 = the popup
        // 1 = the editor
        switch ( i ) {
        case 0:
            if ( popup instanceof Accessible ) {
                AccessibleContext ac = ((Accessible) popup).getAccessibleContext();
                ac.setAccessibleParent(comboBox);
                return(Accessible) popup;
            }
            break;
        case 1:
            if ( comboBox.isEditable()
                 && (editor instanceof Accessible) ) {
                AccessibleContext ac = ((Accessible) editor).getAccessibleContext();
                ac.setAccessibleParent(comboBox);
                return(Accessible) editor;
            }
            break;
        }
        return null;
    }

    //
    // end ComponentUI Implementation
    //===============================


    //======================
    // begin Utility Methods
    //

    /**
     * Returns whether or not the supplied keyCode maps to a key that is used for
     * navigation.  This is used for optimizing key input by only passing non-
     * navigation keys to the type-ahead mechanism.  Subclasses should override this
     * if they change the navigation keys.
     *
     * @param keyCode a key code
     * @return {@code true} if the supplied {@code keyCode} maps to a navigation key
     */
    protected boolean isNavigationKey( int keyCode ) {
        return keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN ||
               keyCode == KeyEvent.VK_KP_UP || keyCode == KeyEvent.VK_KP_DOWN;
    }

    private boolean isNavigationKey(int keyCode, int modifiers) {
        InputMap inputMap = comboBox.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        KeyStroke key = KeyStroke.getKeyStroke(keyCode, modifiers);

        if (inputMap != null && inputMap.get(key) != null) {
            return true;
        }
        return false;
    }

    /**
     * Selects the next item in the list.  It won't change the selection if the
     * currently selected item is already the last item.
     */
    protected void selectNextPossibleValue() {
        int si;

        if ( comboBox.isPopupVisible() ) {
            si = listBox.getSelectedIndex();
        }
        else {
            si = comboBox.getSelectedIndex();
        }

        if ( si < comboBox.getModel().getSize() - 1 ) {
            listBox.setSelectedIndex( si + 1 );
            listBox.ensureIndexIsVisible( si + 1 );
            if ( !isTableCellEditor ) {
                if (!(UIManager.getBoolean("ComboBox.noActionOnKeyNavigation") && comboBox.isPopupVisible())) {
                    comboBox.setSelectedIndex(si+1);
                }
            }
            comboBox.repaint();
        }
    }

    /**
     * Selects the previous item in the list.  It won't change the selection if the
     * currently selected item is already the first item.
     */
    protected void selectPreviousPossibleValue() {
        int si;

        if ( comboBox.isPopupVisible() ) {
            si = listBox.getSelectedIndex();
        }
        else {
            si = comboBox.getSelectedIndex();
        }

        if ( si > 0 ) {
            listBox.setSelectedIndex( si - 1 );
            listBox.ensureIndexIsVisible( si - 1 );
            if ( !isTableCellEditor ) {
                if (!(UIManager.getBoolean("ComboBox.noActionOnKeyNavigation") && comboBox.isPopupVisible())) {
                    comboBox.setSelectedIndex(si-1);
                }
            }
            comboBox.repaint();
        }
    }

    /**
     * Hides the popup if it is showing and shows the popup if it is hidden.
     */
    protected void toggleOpenClose() {
        setPopupVisible(comboBox, !isPopupVisible(comboBox));
    }

    /**
     * Returns the area that is reserved for drawing the currently selected item.
     *
     * @return the area that is reserved for drawing the currently selected item
     */
    protected Rectangle rectangleForCurrentValue() {
        int width = comboBox.getWidth();
        int height = comboBox.getHeight();
        Insets insets = getInsets();
        int buttonSize = height - (insets.top + insets.bottom);
        if ( arrowButton != null ) {
            buttonSize = arrowButton.getWidth();
        }
        if(BasicGraphicsUtils.isLeftToRight(comboBox)) {
            return new Rectangle(insets.left, insets.top,
                             width - (insets.left + insets.right + buttonSize),
                             height - (insets.top + insets.bottom));
        }
        else {
            return new Rectangle(insets.left + buttonSize, insets.top,
                             width - (insets.left + insets.right + buttonSize),
                             height - (insets.top + insets.bottom));
        }
    }

    /**
     * Gets the insets from the JComboBox.
     *
     * @return the insets
     */
    protected Insets getInsets() {
        return comboBox.getInsets();
    }

    //
    // end Utility Methods
    //====================


    //===============================
    // begin Painting Utility Methods
    //

    /**
     * Paints the currently selected item.
     *
     * @param g an instance of {@code Graphics}
     * @param bounds a bounding rectangle to render to
     * @param hasFocus is focused
     */
    public void paintCurrentValue(Graphics g,Rectangle bounds,boolean hasFocus) {
        ListCellRenderer<Object> renderer = comboBox.getRenderer();
        Component c;

        if ( hasFocus && !isPopupVisible(comboBox) ) {
            c = renderer.getListCellRendererComponent( listBox,
                                                       comboBox.getSelectedItem(),
                                                       -1,
                                                       true,
                                                       false );
        }
        else {
            c = renderer.getListCellRendererComponent( listBox,
                                                       comboBox.getSelectedItem(),
                                                       -1,
                                                       false,
                                                       false );
            c.setBackground(UIManager.getColor("ComboBox.background"));
        }
        c.setFont(comboBox.getFont());
        if ( hasFocus && !isPopupVisible(comboBox) ) {
            c.setForeground(listBox.getSelectionForeground());
            c.setBackground(listBox.getSelectionBackground());
        }
        else {
            if ( comboBox.isEnabled() ) {
                c.setForeground(comboBox.getForeground());
                c.setBackground(comboBox.getBackground());
            }
            else {
                c.setForeground(DefaultLookup.getColor(
                         comboBox, this, "ComboBox.disabledForeground", null));
                c.setBackground(DefaultLookup.getColor(
                         comboBox, this, "ComboBox.disabledBackground", null));
            }
        }

        // Fix for 4238829: should lay out the JPanel.
        boolean shouldValidate = false;
        if (c instanceof JPanel)  {
            shouldValidate = true;
        }

        int x = bounds.x, y = bounds.y, w = bounds.width, h = bounds.height;
        if (padding != null) {
            x = bounds.x + padding.left;
            y = bounds.y + padding.top;
            w = bounds.width - (padding.left + padding.right);
            h = bounds.height - (padding.top + padding.bottom);
        }

        currentValuePane.paintComponent(g,c,comboBox,x,y,w,h,shouldValidate);
    }

    /**
     * Paints the background of the currently selected item.
     *
     * @param g an instance of {@code Graphics}
     * @param bounds a bounding rectangle to render to
     * @param hasFocus is focused
     */
    public void paintCurrentValueBackground(Graphics g,Rectangle bounds,boolean hasFocus) {
        Color t = g.getColor();
        if ( comboBox.isEnabled() )
            g.setColor(DefaultLookup.getColor(comboBox, this,
                                              "ComboBox.background", null));
        else
            g.setColor(DefaultLookup.getColor(comboBox, this,
                                     "ComboBox.disabledBackground", null));
        g.fillRect(bounds.x,bounds.y,bounds.width,bounds.height);
        g.setColor(t);
    }

    /**
     * Repaint the currently selected item.
     */
    void repaintCurrentValue() {
        Rectangle r = rectangleForCurrentValue();
        comboBox.repaint(r.x,r.y,r.width,r.height);
    }

    //
    // end Painting Utility Methods
    //=============================


    //===============================
    // begin Size Utility Methods
    //

    /**
     * Return the default size of an empty display area of the combo box using
     * the current renderer and font.
     *
     * @return the size of an empty display area
     * @see #getDisplaySize
     */
    protected Dimension getDefaultSize() {
        // Calculates the height and width using the default text renderer
        Dimension d = getSizeForComponent(getDefaultListCellRenderer().getListCellRendererComponent(listBox, " ", -1, false, false));

        return new Dimension(d.width, d.height);
    }

    /**
     * Returns the calculated size of the display area. The display area is the
     * portion of the combo box in which the selected item is displayed. This
     * method will use the prototype display value if it has been set.
     * <p>
     * For combo boxes with a non trivial number of items, it is recommended to
     * use a prototype display value to significantly speed up the display
     * size calculation.
     *
     * @return the size of the display area calculated from the combo box items
     * @see javax.swing.JComboBox#setPrototypeDisplayValue
     */
    protected Dimension getDisplaySize() {
        if (!isDisplaySizeDirty)  {
            return new Dimension(cachedDisplaySize);
        }
        Dimension result = new Dimension();

        ListCellRenderer<Object> renderer = comboBox.getRenderer();
        if (renderer == null)  {
            renderer = new DefaultListCellRenderer();
        }

        sameBaseline = true;

        Object prototypeValue = comboBox.getPrototypeDisplayValue();
        if (prototypeValue != null)  {
            // Calculates the dimension based on the prototype value
            result = getSizeForComponent(renderer.getListCellRendererComponent(listBox,
                                                                               prototypeValue,
                                                                               -1, false, false));
        } else {
            // Calculate the dimension by iterating over all the elements in the combo
            // box list.
            ComboBoxModel<Object> model = comboBox.getModel();
            int modelSize = model.getSize();
            int baseline = -1;
            Dimension d;

            Component cpn;

            if (modelSize > 0 ) {
                for (int i = 0; i < modelSize ; i++ ) {
                    // Calculates the maximum height and width based on the largest
                    // element
                    Object value = model.getElementAt(i);
                    Component c = renderer.getListCellRendererComponent(
                            listBox, value, -1, false, false);
                    d = getSizeForComponent(c);
                    if (sameBaseline && value != null &&
                            (!(value instanceof String) || !"".equals(value))) {
                        int newBaseline = c.getBaseline(d.width, d.height);
                        if (newBaseline == -1) {
                            sameBaseline = false;
                        }
                        else if (baseline == -1) {
                            baseline = newBaseline;
                        }
                        else if (baseline != newBaseline) {
                            sameBaseline = false;
                        }
                    }
                    result.width = Math.max(result.width,d.width);
                    result.height = Math.max(result.height,d.height);
                }
            } else {
                result = getDefaultSize();
                if (comboBox.isEditable()) {
                    result.width = 100;
                }
            }
        }

        if ( comboBox.isEditable() ) {
            Dimension d = editor.getPreferredSize();
            result.width = Math.max(result.width,d.width);
            result.height = Math.max(result.height,d.height);
        }

        // calculate in the padding
        if (padding != null) {
            result.width += padding.left + padding.right;
            result.height += padding.top + padding.bottom;
        }

        // Set the cached value
        cachedDisplaySize.setSize(result.width, result.height);
        isDisplaySizeDirty = false;

        return result;
    }

    /**
     * Returns the size a component would have if used as a cell renderer.
     *
     * @param comp a {@code Component} to check
     * @return size of the component
     * @since 1.7
     */
    protected Dimension getSizeForComponent(Component comp) {
        // This has been refactored out in hopes that it may be investigated and
        // simplified for the next major release. adding/removing
        // the component to the currentValuePane and changing the font may be
        // redundant operations.
        currentValuePane.add(comp);
        comp.setFont(comboBox.getFont());
        Dimension d = comp.getPreferredSize();
        currentValuePane.remove(comp);
        return d;
    }


    //
    // end Size Utility Methods
    //=============================


    //=================================
    // begin Keyboard Action Management
    //

    /**
     * Adds keyboard actions to the JComboBox.  Actions on enter and esc are already
     * supplied.  Add more actions as you need them.
     */
    protected void installKeyboardActions() {
        InputMap km = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        SwingUtilities.replaceUIInputMap(comboBox, JComponent.
                             WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, km);


        LazyActionMap.installLazyActionMap(comboBox, BasicComboBoxUI.class,
                                           "ComboBox.actionMap");
    }

    InputMap getInputMap(int condition) {
        if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) {
            return (InputMap)DefaultLookup.get(comboBox, this,
                                               "ComboBox.ancestorInputMap");
        }
        return null;
    }

    boolean isTableCellEditor() {
        return isTableCellEditor;
    }

    /**
     * Removes the focus InputMap and ActionMap.
     */
    protected void uninstallKeyboardActions() {
        SwingUtilities.replaceUIInputMap(comboBox, JComponent.
                                 WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);
        SwingUtilities.replaceUIActionMap(comboBox, null);
    }


    //
    // Actions
    //
    private static class Actions extends UIAction {
        private static final String HIDE = "hidePopup";
        private static final String DOWN = "selectNext";
        private static final String DOWN_2 = "selectNext2";
        private static final String TOGGLE = "togglePopup";
        private static final String TOGGLE_2 = "spacePopup";
        private static final String UP = "selectPrevious";
        private static final String UP_2 = "selectPrevious2";
        private static final String ENTER = "enterPressed";
        private static final String PAGE_DOWN = "pageDownPassThrough";
        private static final String PAGE_UP = "pageUpPassThrough";
        private static final String HOME = "homePassThrough";
        private static final String END = "endPassThrough";

        Actions(String name) {
            super(name);
        }

        public void actionPerformed( ActionEvent e ) {
            String key = getName();
            @SuppressWarnings("unchecked")
            JComboBox<Object> comboBox = (JComboBox)e.getSource();
            BasicComboBoxUI ui = (BasicComboBoxUI)BasicLookAndFeel.getUIOfType(
                                  comboBox.getUI(), BasicComboBoxUI.class);
            if (key == HIDE) {
                comboBox.firePopupMenuCanceled();
                comboBox.setPopupVisible(false);
            }
            else if (key == PAGE_DOWN || key == PAGE_UP ||
                     key == HOME || key == END) {
                int index = getNextIndex(comboBox, key);
                if (index >= 0 && index < comboBox.getItemCount()) {
                    if (UIManager.getBoolean("ComboBox.noActionOnKeyNavigation") && comboBox.isPopupVisible()) {
                        ui.listBox.setSelectedIndex(index);
                        ui.listBox.ensureIndexIsVisible(index);
                        comboBox.repaint();
                    } else {
                        comboBox.setSelectedIndex(index);
                    }
                }
            }
            else if (key == DOWN) {
                if (comboBox.isShowing() ) {
                    if ( comboBox.isPopupVisible() ) {
                        if (ui != null) {
                            ui.selectNextPossibleValue();
                        }
                    } else {
                        comboBox.setPopupVisible(true);
                    }
                }
            }
            else if (key == DOWN_2) {
                // Special case in which pressing the arrow keys will not
                // make the popup appear - except for editable combo boxes
                // and combo boxes inside a table.
                if (comboBox.isShowing() ) {
                    if ( (comboBox.isEditable() ||
                            (ui != null && ui.isTableCellEditor()))
                         && !comboBox.isPopupVisible() ) {
                        comboBox.setPopupVisible(true);
                    } else {
                        if (ui != null) {
                            ui.selectNextPossibleValue();
                        }
                    }
                }
            }
            else if (key == TOGGLE || key == TOGGLE_2) {
                if (ui != null && (key == TOGGLE || !comboBox.isEditable())) {
                    if ( ui.isTableCellEditor() ) {
                        // Forces the selection of the list item if the
                        // combo box is in a JTable.
                        comboBox.setSelectedIndex(ui.popup.getList().
                                                  getSelectedIndex());
                    }
                    else {
                        comboBox.setPopupVisible(!comboBox.isPopupVisible());
                    }
                }
            }
            else if (key == UP) {
                if (ui != null) {
                    if (ui.isPopupVisible(comboBox)) {
                        ui.selectPreviousPossibleValue();
                    }
                    else if (DefaultLookup.getBoolean(comboBox, ui,
                                    "ComboBox.showPopupOnNavigation", false)) {
                        ui.setPopupVisible(comboBox, true);
                    }
                }
            }
            else if (key == UP_2) {
                 // Special case in which pressing the arrow keys will not
                 // make the popup appear - except for editable combo boxes.
                 if (comboBox.isShowing() && ui != null) {
                     if ( comboBox.isEditable() && !comboBox.isPopupVisible()) {
                         comboBox.setPopupVisible(true);
                     } else {
                         ui.selectPreviousPossibleValue();
                     }
                 }
             }

            else if (key == ENTER) {
                if (comboBox.isPopupVisible()) {
                    // If ComboBox.noActionOnKeyNavigation is set,
                    // forse selection of list item
                    if (UIManager.getBoolean("ComboBox.noActionOnKeyNavigation")) {
                        Object listItem = ui.popup.getList().getSelectedValue();
                        if (listItem != null) {
                            comboBox.getEditor().setItem(listItem);
                            comboBox.setSelectedItem(listItem);
                        }
                        comboBox.setPopupVisible(false);
                    } else {
                        // Forces the selection of the list item
                        boolean isEnterSelectablePopup =
                                UIManager.getBoolean("ComboBox.isEnterSelectablePopup");
                        if (!comboBox.isEditable() || isEnterSelectablePopup
                                || ui.isTableCellEditor) {
                            Object listItem = ui.popup.getList().getSelectedValue();
                            if (listItem != null) {
                                // Use the selected value from popup
                                // to set the selected item in combo box,
                                // but ensure before that JComboBox.actionPerformed()
                                // won't use editor's value to set the selected item
                                comboBox.getEditor().setItem(listItem);
                                comboBox.setSelectedItem(listItem);
                            }
                        }
                        comboBox.setPopupVisible(false);
                    }
                }
                else {
                    // Hide combo box if it is a table cell editor
                    if (ui.isTableCellEditor && !comboBox.isEditable()) {
                        comboBox.setSelectedItem(comboBox.getSelectedItem());
                    }
                    // Call the default button binding.
                    // This is a pretty messy way of passing an event through
                    // to the root pane.
                    JRootPane root = SwingUtilities.getRootPane(comboBox);
                    if (root != null) {
                        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
                        ActionMap am = root.getActionMap();
                        if (im != null && am != null) {
                            Object obj = im.get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0));
                            if (obj != null) {
                                Action action = am.get(obj);
                                if (action != null) {
                                    action.actionPerformed(new ActionEvent(
                                     root, e.getID(), e.getActionCommand(),
                                     e.getWhen(), e.getModifiers()));
                                }
                            }
                        }
                    }
                }
            }
        }

        private int getNextIndex(JComboBox<?> comboBox, String key) {
            int listHeight = comboBox.getMaximumRowCount();

            int selectedIndex = comboBox.getSelectedIndex();
            if (UIManager.getBoolean("ComboBox.noActionOnKeyNavigation")
                    && (comboBox.getUI() instanceof BasicComboBoxUI)) {
                selectedIndex = ((BasicComboBoxUI) comboBox.getUI()).listBox.getSelectedIndex();
            }

            if (key == PAGE_UP) {
                int index = selectedIndex - listHeight;
                return (index < 0 ? 0: index);
            }
            else if (key == PAGE_DOWN) {
                int index = selectedIndex + listHeight;
                int max = comboBox.getItemCount();
                return (index < max ? index: max-1);
            }
            else if (key == HOME) {
                return 0;
            }
            else if (key == END) {
                return comboBox.getItemCount() - 1;
            }
            return comboBox.getSelectedIndex();
        }

        @Override
        public boolean accept(Object c) {
            if (getName() == HIDE ) {
                return (c != null && ((JComboBox)c).isPopupVisible());
            } else if (getName() == ENTER) {
                JRootPane root = SwingUtilities.getRootPane((JComboBox)c);
                if (root != null && (c != null && !((JComboBox)c).isPopupVisible())) {
                    InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
                    ActionMap am = root.getActionMap();
                    if (im != null && am != null) {
                        Object obj = im.get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
                        if (obj == null) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    }
    //
    // end Keyboard Action Management
    //===============================


    //
    // Shared Handler, implements all listeners
    //
    private class Handler implements ActionListener, FocusListener,
                                     KeyListener, LayoutManager,
                                     ListDataListener, PropertyChangeListener {
        //
        // PropertyChangeListener
        //
        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if (e.getSource() == editor){
                // If the border of the editor changes then this can effect
                // the size of the editor which can cause the combo's size to
                // become invalid so we need to clear size caches
                if ("border".equals(propertyName)){
                    isMinimumSizeDirty = true;
                    isDisplaySizeDirty = true;
                    comboBox.revalidate();
                }
            } else {
                @SuppressWarnings("unchecked")
                JComboBox<?> comboBox = (JComboBox)e.getSource();
                if ( propertyName == "model" ) {
                    @SuppressWarnings("unchecked")
                    ComboBoxModel<?> newModel = (ComboBoxModel)e.getNewValue();
                    @SuppressWarnings("unchecked")
                    ComboBoxModel<?> oldModel = (ComboBoxModel)e.getOldValue();

                    if ( oldModel != null && listDataListener != null ) {
                        oldModel.removeListDataListener( listDataListener );
                    }

                    if ( newModel != null && listDataListener != null ) {
                        newModel.addListDataListener( listDataListener );
                    }

                    if ( editor != null ) {
                        comboBox.configureEditor( comboBox.getEditor(), comboBox.getSelectedItem() );
                    }
                    isMinimumSizeDirty = true;
                    isDisplaySizeDirty = true;
                    comboBox.revalidate();
                    comboBox.repaint();
                }
                else if ( propertyName == "editor" && comboBox.isEditable() ) {
                    addEditor();
                    comboBox.revalidate();
                }
                else if ( propertyName == "editable" ) {
                    if ( comboBox.isEditable() ) {
                        comboBox.setRequestFocusEnabled( false );
                        addEditor();
                    } else {
                        comboBox.setRequestFocusEnabled( true );
                        removeEditor();
                    }
                    updateToolTipTextForChildren();
                    comboBox.revalidate();
                }
                else if ( propertyName == "enabled" ) {
                    boolean enabled = comboBox.isEnabled();
                    if ( editor != null )
                        editor.setEnabled(enabled);
                    if ( arrowButton != null )
                        arrowButton.setEnabled(enabled);
                    comboBox.repaint();
                }
                else if ( propertyName == "focusable" ) {
                    boolean focusable = comboBox.isFocusable();
                    if ( editor != null )
                        editor.setFocusable(focusable);
                    if ( arrowButton != null )
                        arrowButton.setFocusable(focusable);
                    comboBox.repaint();
                }
                else if ( propertyName == "maximumRowCount" ) {
                    if ( isPopupVisible( comboBox ) ) {
                        setPopupVisible(comboBox, false);
                        setPopupVisible(comboBox, true);
                    }
                }
                else if ( propertyName == "font" ) {
                    listBox.setFont( comboBox.getFont() );
                    if ( editor != null ) {
                        editor.setFont( comboBox.getFont() );
                    }
                    isMinimumSizeDirty = true;
                    isDisplaySizeDirty = true;
                    comboBox.validate();
                } else if (SwingUtilities2.isScaleChanged(e)) {
                    isMinimumSizeDirty = true;
                    isDisplaySizeDirty = true;
                    comboBox.validate();
                }
                else if ( propertyName == JComponent.TOOL_TIP_TEXT_KEY ) {
                    updateToolTipTextForChildren();
                }
                else if ( propertyName == BasicComboBoxUI.IS_TABLE_CELL_EDITOR ) {
                    Boolean inTable = (Boolean)e.getNewValue();
                    isTableCellEditor = inTable.equals(Boolean.TRUE) ? true : false;
                }
                else if (propertyName == "prototypeDisplayValue") {
                    isMinimumSizeDirty = true;
                    isDisplaySizeDirty = true;
                    comboBox.revalidate();
                }
                else if (propertyName == "renderer") {
                    isMinimumSizeDirty = true;
                    isDisplaySizeDirty = true;
                    comboBox.revalidate();
                }
            }
        }


        //
        // KeyListener
        //

        // This listener checks to see if the key event isn't a navigation
        // key.  If it finds a key event that wasn't a navigation key it
        // dispatches it to JComboBox.selectWithKeyChar() so that it can do
        // type-ahead.
        @SuppressWarnings("deprecation")
        public void keyPressed( KeyEvent e ) {
            if ( isNavigationKey(e.getKeyCode(), e.getModifiers()) ) {
                lastTime = 0L;
            } else if ( comboBox.isEnabled() && comboBox.getModel().getSize()!=0 &&
                        isTypeAheadKey( e ) && e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
                time = e.getWhen();
                if ( comboBox.selectWithKeyChar(e.getKeyChar()) ) {
                    e.consume();
                }
            }
        }

        public void keyTyped(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
        }

        private boolean isTypeAheadKey( KeyEvent e ) {
            return !e.isAltDown() && !BasicGraphicsUtils.isMenuShortcutKeyDown(e);
        }

        //
        // FocusListener
        //
        // NOTE: The class is added to both the Editor and ComboBox.
        // The combo box listener hides the popup when the focus is lost.
        // It also repaints when focus is gained or lost.

        public void focusGained( FocusEvent e ) {
            ComboBoxEditor comboBoxEditor = comboBox.getEditor();

            if ( (comboBoxEditor != null) &&
                 (e.getSource() == comboBoxEditor.getEditorComponent()) ) {
                return;
            }
            hasFocus = true;
            comboBox.repaint();

            if (comboBox.isEditable() && editor != null) {
                editor.requestFocus();
            }
        }

        public void focusLost( FocusEvent e ) {
            ComboBoxEditor editor = comboBox.getEditor();
            if ( (editor != null) &&
                 (e.getSource() == editor.getEditorComponent()) ) {
                Object item = editor.getItem();

                Object selectedItem = comboBox.getSelectedItem();
                if (!e.isTemporary() && item != null &&
                    !item.equals((selectedItem == null) ? "" : selectedItem )) {
                    comboBox.actionPerformed
                        (new ActionEvent(editor, 0, "",
                                      EventQueue.getMostRecentEventTime(), 0));
                }
            }

            hasFocus = false;
            if (!e.isTemporary()) {
                setPopupVisible(comboBox, false);
            }
            comboBox.repaint();
        }

        //
        // ListDataListener
        //

        // This listener watches for changes in the ComboBoxModel
        public void contentsChanged( ListDataEvent e ) {
            if ( !(e.getIndex0() == -1 && e.getIndex1() == -1) ) {
                isMinimumSizeDirty = true;
                comboBox.revalidate();
            }

            // set the editor with the selected item since this
            // is the event handler for a selected item change.
            if (comboBox.isEditable() && editor != null) {
                comboBox.configureEditor( comboBox.getEditor(),
                                          comboBox.getSelectedItem() );
            }

            isDisplaySizeDirty = true;
            comboBox.repaint();
        }

        public void intervalAdded( ListDataEvent e ) {
            contentsChanged( e );
        }

        public void intervalRemoved( ListDataEvent e ) {
            contentsChanged( e );
        }

        //
        // LayoutManager
        //

        // This layout manager handles the 'standard' layout of combo boxes.
        // It puts the arrow button to the right and the editor to the left.
        // If there is no editor it still keeps the arrow button to the right.
        public void addLayoutComponent(String name, Component comp) {}

        public void removeLayoutComponent(Component comp) {}

        public Dimension preferredLayoutSize(Container parent) {
            return parent.getPreferredSize();
        }

        public Dimension minimumLayoutSize(Container parent) {
            return parent.getMinimumSize();
        }

        public void layoutContainer(Container parent) {
            @SuppressWarnings("unchecked")
            JComboBox<?> cb = (JComboBox)parent;
            int width = cb.getWidth();
            int height = cb.getHeight();

            Insets insets = getInsets();
            int buttonHeight = height - (insets.top + insets.bottom);
            int buttonWidth = buttonHeight;
            if (arrowButton != null) {
                Insets arrowInsets = arrowButton.getInsets();
                buttonWidth = squareButton ?
                    buttonHeight :
                    arrowButton.getPreferredSize().width + arrowInsets.left + arrowInsets.right;
            }
            Rectangle cvb;

            if (arrowButton != null) {
                if (BasicGraphicsUtils.isLeftToRight(cb)) {
                    arrowButton.setBounds(width - (insets.right + buttonWidth),
                            insets.top, buttonWidth, buttonHeight);
                } else {
                    arrowButton.setBounds(insets.left, insets.top,
                            buttonWidth, buttonHeight);
                }
            }
            if ( editor != null ) {
                cvb = rectangleForCurrentValue();
                editor.setBounds(cvb);
            }
        }

        //
        // ActionListener
        //
        // Fix for 4515752: Forward the Enter pressed on the
        // editable combo box to the default button

        // Note: This could depend on event ordering. The first ActionEvent
        // from the editor may be handled by the JComboBox in which case, the
        // enterPressed action will always be invoked.
        public void actionPerformed(ActionEvent evt) {
            Object item = comboBox.getEditor().getItem();
            if (item != null) {
                if (!comboBox.isPopupVisible() && !item.equals(comboBox.getSelectedItem())) {
                    comboBox.setSelectedItem(comboBox.getEditor().getItem());
                }
                ActionMap am = comboBox.getActionMap();
                if (am != null) {
                    Action action = am.get("enterPressed");
                    if (action != null) {
                        action.actionPerformed(new ActionEvent(comboBox, evt.getID(),
                                evt.getActionCommand(),
                                evt.getModifiers()));
                    }
                }
            }
        }
   }

    class DefaultKeySelectionManager implements JComboBox.KeySelectionManager, UIResource {
        private String prefix = "";
        private String typedString = "";

        public int selectionForKey(char aKey,ComboBoxModel<?> aModel) {
            if (lastTime == 0L) {
                prefix = "";
                typedString = "";
            }
            boolean startingFromSelection = true;

            int startIndex = comboBox.getSelectedIndex();
            if (time - lastTime < timeFactor) {
                typedString += aKey;
                if((prefix.length() == 1) && (aKey == prefix.charAt(0))) {
                    // Subsequent same key presses move the keyboard focus to the next
                    // object that starts with the same letter.
                    startIndex++;
                } else {
                    prefix = typedString;
                }
            } else {
                startIndex++;
                typedString = "" + aKey;
                prefix = typedString;
            }
            lastTime = time;

            if (startIndex < 0 || startIndex >= aModel.getSize()) {
                startingFromSelection = false;
                startIndex = 0;
            }
            int index = listBox.getNextMatch(prefix, startIndex,
                                             Position.Bias.Forward);
            if (index < 0 && startingFromSelection) { // wrap
                index = listBox.getNextMatch(prefix, 0,
                                             Position.Bias.Forward);
            }
            return index;
        }
    }

}
