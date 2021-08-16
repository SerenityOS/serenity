/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.synth;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.basic.BasicLookAndFeel;

import sun.awt.AppContext;
import sun.awt.SunToolkit;
import sun.swing.DefaultLookup;
import sun.swing.SwingAccessor;
import sun.swing.SwingUtilities2;
import sun.swing.plaf.synth.SynthFileChooserUI;

/**
 * SynthLookAndFeel provides the basis for creating a customized look and
 * feel. SynthLookAndFeel does not directly provide a look, all painting is
 * delegated.
 * You need to either provide a configuration file, by way of the
 * {@link #load} method, or provide your own {@link SynthStyleFactory}
 * to {@link #setStyleFactory}. Refer to the
 * <a href="package-summary.html">package summary</a> for an example of
 * loading a file, and {@link javax.swing.plaf.synth.SynthStyleFactory} for
 * an example of providing your own <code>SynthStyleFactory</code> to
 * <code>setStyleFactory</code>.
 * <p>
 * {@link SynthIcon} interface provides
 * {@code paintIcon(synthContext, graphics, x, y, width, height)} method that
 * allows to draw the icon with the given {@code SynthContext}.
 * <p>
 * <strong>Warning:</strong>
 * This class implements {@link Serializable} as a side effect of it
 * extending {@link BasicLookAndFeel}. It is not intended to be serialized.
 * An attempt to serialize it will
 * result in {@link NotSerializableException}.
 *
 * @serial exclude
 * @since 1.5
 * @author Scott Violet
 */
@SuppressWarnings("serial") // Per above comment, not actually serializable
public class SynthLookAndFeel extends BasicLookAndFeel {
    /**
     * Used in a handful of places where we need an empty Insets.
     */
    static final Insets EMPTY_UIRESOURCE_INSETS = new InsetsUIResource(
                                                            0, 0, 0, 0);

    /**
     * AppContext key to get the current SynthStyleFactory.
     */
    private static final Object STYLE_FACTORY_KEY =
                  new StringBuffer("com.sun.java.swing.plaf.gtk.StyleCache");

    /**
     * AppContext key to get selectedUI.
     */
    private static final Object SELECTED_UI_KEY = new StringBuilder("selectedUI");

    /**
     * AppContext key to get selectedUIState.
     */
    private static final Object SELECTED_UI_STATE_KEY = new StringBuilder("selectedUIState");

    /**
     * The last SynthStyleFactory that was asked for from AppContext
     * <code>lastContext</code>.
     */
    private static SynthStyleFactory lastFactory;
    /**
     * AppContext lastLAF came from.
     */
    private static AppContext lastContext;

    /**
     * SynthStyleFactory for the this SynthLookAndFeel.
     */
    private SynthStyleFactory factory;

    /**
     * Map of defaults table entries. This is populated via the load
     * method.
     */
    private Map<String, Object> defaultsMap;

    private Handler _handler;

    static ComponentUI getSelectedUI() {
        return (ComponentUI) AppContext.getAppContext().get(SELECTED_UI_KEY);
    }

    /**
     * Used by the renderers. For the most part the renderers are implemented
     * as Labels, which is problematic in so far as they are never selected.
     * To accommodate this SynthLabelUI checks if the current
     * UI matches that of <code>selectedUI</code> (which this methods sets), if
     * it does, then a state as set by this method is returned. This provides
     * a way for labels to have a state other than selected.
     */
    static void setSelectedUI(ComponentUI uix, boolean selected,
                              boolean focused, boolean enabled,
                              boolean rollover) {
        int selectedUIState = 0;

        if (selected) {
            selectedUIState = SynthConstants.SELECTED;
            if (focused) {
                selectedUIState |= SynthConstants.FOCUSED;
            }
        }
        else if (rollover && enabled) {
            selectedUIState |=
                    SynthConstants.MOUSE_OVER | SynthConstants.ENABLED;
            if (focused) {
                selectedUIState |= SynthConstants.FOCUSED;
            }
        }
        else {
            if (enabled) {
                selectedUIState |= SynthConstants.ENABLED;
                if (focused) {
                    selectedUIState |= SynthConstants.FOCUSED;
                }
            }
            else {
                selectedUIState |= SynthConstants.DISABLED;
            }
        }

        AppContext context = AppContext.getAppContext();

        context.put(SELECTED_UI_KEY, uix);
        context.put(SELECTED_UI_STATE_KEY, Integer.valueOf(selectedUIState));
    }

    static int getSelectedUIState() {
        Integer result = (Integer) AppContext.getAppContext().get(SELECTED_UI_STATE_KEY);

        return result == null ? 0 : result.intValue();
    }

    /**
     * Clears out the selected UI that was last set in setSelectedUI.
     */
    static void resetSelectedUI() {
        AppContext.getAppContext().remove(SELECTED_UI_KEY);
    }


    /**
     * Sets the SynthStyleFactory that the UI classes provided by
     * synth will use to obtain a SynthStyle.
     *
     * @param cache SynthStyleFactory the UIs should use.
     */
    public static void setStyleFactory(SynthStyleFactory cache) {
        // We assume the setter is called BEFORE the getter has been invoked
        // for a particular AppContext.
        synchronized(SynthLookAndFeel.class) {
            AppContext context = AppContext.getAppContext();
            lastFactory = cache;
            lastContext = context;
            context.put(STYLE_FACTORY_KEY, cache);
        }
    }

    /**
     * Returns the current SynthStyleFactory.
     *
     * @return SynthStyleFactory
     */
    public static SynthStyleFactory getStyleFactory() {
        synchronized(SynthLookAndFeel.class) {
            AppContext context = AppContext.getAppContext();

            if (lastContext == context) {
                return lastFactory;
            }
            lastContext = context;
            lastFactory = (SynthStyleFactory) context.get(STYLE_FACTORY_KEY);
            return lastFactory;
        }
    }

    /**
     * Returns the component state for the specified component. This should
     * only be used for Components that don't have any special state beyond
     * that of ENABLED, DISABLED or FOCUSED. For example, buttons shouldn't
     * call into this method.
     */
    static int getComponentState(Component c) {
        if (c.isEnabled()) {
            if (c.isFocusOwner()) {
                return SynthUI.ENABLED | SynthUI.FOCUSED;
            }
            return SynthUI.ENABLED;
        }
        return SynthUI.DISABLED;
    }

    /**
     * Gets a SynthStyle for the specified region of the specified component.
     * This is not for general consumption, only custom UIs should call this
     * method.
     *
     * @param c JComponent to get the SynthStyle for
     * @param region Identifies the region of the specified component
     * @return SynthStyle to use.
     */
    public static SynthStyle getStyle(JComponent c, Region region) {
        return getStyleFactory().getStyle(c, region);
    }

    /**
     * Returns true if the Style should be updated in response to the
     * specified PropertyChangeEvent. This forwards to
     * <code>shouldUpdateStyleOnAncestorChanged</code> as necessary.
     */
    static boolean shouldUpdateStyle(PropertyChangeEvent event) {
        LookAndFeel laf = UIManager.getLookAndFeel();
        return (laf instanceof SynthLookAndFeel &&
                ((SynthLookAndFeel) laf).shouldUpdateStyleOnEvent(event));
    }

    /**
     * A convience method that will reset the Style of StyleContext if
     * necessary.
     *
     * @return newStyle
     */
    static SynthStyle updateStyle(SynthContext context, SynthUI ui) {
        SynthStyle newStyle = getStyle(context.getComponent(),
                                       context.getRegion());
        SynthStyle oldStyle = context.getStyle();

        if (newStyle != oldStyle) {
            if (oldStyle != null) {
                oldStyle.uninstallDefaults(context);
            }
            context.setStyle(newStyle);
            newStyle.installDefaults(context, ui);
        }
        return newStyle;
    }

    /**
     * Updates the style associated with <code>c</code>, and all its children.
     * This is a lighter version of
     * <code>SwingUtilities.updateComponentTreeUI</code>.
     *
     * @param c Component to update style for.
     */
    public static void updateStyles(Component c) {
        if (c instanceof JComponent) {
            // Yes, this is hacky. A better solution is to get the UI
            // and cast, but JComponent doesn't expose a getter for the UI
            // (each of the UIs do), making that approach impractical.
            String name = c.getName();
            c.setName(null);
            if (name != null) {
                c.setName(name);
            }
            ((JComponent)c).revalidate();
        }
        Component[] children = null;
        if (c instanceof JMenu) {
            children = ((JMenu)c).getMenuComponents();
        }
        else if (c instanceof Container) {
            children = ((Container)c).getComponents();
        }
        if (children != null) {
            for (Component child : children) {
                updateStyles(child);
            }
        }
        c.repaint();
    }

    /**
     * Returns the Region for the JComponent <code>c</code>.
     *
     * @param c JComponent to fetch the Region for
     * @return Region corresponding to <code>c</code>
     */
    public static Region getRegion(JComponent c) {
        return Region.getRegion(c);
    }

    /**
     * A convenience method to return where the foreground should be
     * painted for the Component identified by the passed in
     * AbstractSynthContext.
     */
    static Insets getPaintingInsets(SynthContext state, Insets insets) {
        if (state.isSubregion()) {
            insets = state.getStyle().getInsets(state, insets);
        }
        else {
            insets = state.getComponent().getInsets(insets);
        }
        return insets;
    }

    /**
     * A convenience method that handles painting of the background.
     * All SynthUI implementations should override update and invoke
     * this method.
     */
    static void update(SynthContext state, Graphics g) {
        paintRegion(state, g, null);
    }

    /**
     * A convenience method that handles painting of the background for
     * subregions. All SynthUI's that have subregions should invoke
     * this method, than paint the foreground.
     */
    static void updateSubregion(SynthContext state, Graphics g,
                                Rectangle bounds) {
        paintRegion(state, g, bounds);
    }

    private static void paintRegion(SynthContext state, Graphics g,
                                    Rectangle bounds) {
        JComponent c = state.getComponent();
        SynthStyle style = state.getStyle();
        int x, y, width, height;

        if (bounds == null) {
            x = 0;
            y = 0;
            width = c.getWidth();
            height = c.getHeight();
        }
        else {
            x = bounds.x;
            y = bounds.y;
            width = bounds.width;
            height = bounds.height;
        }

        // Fill in the background, if necessary.
        boolean subregion = state.isSubregion();
        if ((subregion && style.isOpaque(state)) ||
                          (!subregion && c.isOpaque())) {
            g.setColor(style.getColor(state, ColorType.BACKGROUND));
            g.fillRect(x, y, width, height);
        }
    }

    static boolean isLeftToRight(Component c) {
        return c.getComponentOrientation().isLeftToRight();
    }

    /**
     * Returns the ui that is of type <code>klass</code>, or null if
     * one can not be found.
     */
    static Object getUIOfType(ComponentUI ui, Class<?> klass) {
        if (klass.isInstance(ui)) {
            return ui;
        }
        return null;
    }

    /**
     * Creates the Synth look and feel <code>ComponentUI</code> for
     * the passed in <code>JComponent</code>.
     *
     * @param c JComponent to create the <code>ComponentUI</code> for
     * @return ComponentUI to use for <code>c</code>
     */
    public static ComponentUI createUI(JComponent c) {
        String key = c.getUIClassID().intern();

        if (key == "ButtonUI") {
            return SynthButtonUI.createUI(c);
        }
        else if (key == "CheckBoxUI") {
            return SynthCheckBoxUI.createUI(c);
        }
        else if (key == "CheckBoxMenuItemUI") {
            return SynthCheckBoxMenuItemUI.createUI(c);
        }
        else if (key == "ColorChooserUI") {
            return SynthColorChooserUI.createUI(c);
        }
        else if (key == "ComboBoxUI") {
            return SynthComboBoxUI.createUI(c);
        }
        else if (key == "DesktopPaneUI") {
            return SynthDesktopPaneUI.createUI(c);
        }
        else if (key == "DesktopIconUI") {
            return SynthDesktopIconUI.createUI(c);
        }
        else if (key == "EditorPaneUI") {
            return SynthEditorPaneUI.createUI(c);
        }
        else if (key == "FileChooserUI") {
            return SynthFileChooserUI.createUI(c);
        }
        else if (key == "FormattedTextFieldUI") {
            return SynthFormattedTextFieldUI.createUI(c);
        }
        else if (key == "InternalFrameUI") {
            return SynthInternalFrameUI.createUI(c);
        }
        else if (key == "LabelUI") {
            return SynthLabelUI.createUI(c);
        }
        else if (key == "ListUI") {
            return SynthListUI.createUI(c);
        }
        else if (key == "MenuBarUI") {
            return SynthMenuBarUI.createUI(c);
        }
        else if (key == "MenuUI") {
            return SynthMenuUI.createUI(c);
        }
        else if (key == "MenuItemUI") {
            return SynthMenuItemUI.createUI(c);
        }
        else if (key == "OptionPaneUI") {
            return SynthOptionPaneUI.createUI(c);
        }
        else if (key == "PanelUI") {
            return SynthPanelUI.createUI(c);
        }
        else if (key == "PasswordFieldUI") {
            return SynthPasswordFieldUI.createUI(c);
        }
        else if (key == "PopupMenuSeparatorUI") {
            return SynthSeparatorUI.createUI(c);
        }
        else if (key == "PopupMenuUI") {
            return SynthPopupMenuUI.createUI(c);
        }
        else if (key == "ProgressBarUI") {
            return SynthProgressBarUI.createUI(c);
        }
        else if (key == "RadioButtonUI") {
            return SynthRadioButtonUI.createUI(c);
        }
        else if (key == "RadioButtonMenuItemUI") {
            return SynthRadioButtonMenuItemUI.createUI(c);
        }
        else if (key == "RootPaneUI") {
            return SynthRootPaneUI.createUI(c);
        }
        else if (key == "ScrollBarUI") {
            return SynthScrollBarUI.createUI(c);
        }
        else if (key == "ScrollPaneUI") {
            return SynthScrollPaneUI.createUI(c);
        }
        else if (key == "SeparatorUI") {
            return SynthSeparatorUI.createUI(c);
        }
        else if (key == "SliderUI") {
            return SynthSliderUI.createUI(c);
        }
        else if (key == "SpinnerUI") {
            return SynthSpinnerUI.createUI(c);
        }
        else if (key == "SplitPaneUI") {
            return SynthSplitPaneUI.createUI(c);
        }
        else if (key == "TabbedPaneUI") {
            return SynthTabbedPaneUI.createUI(c);
        }
        else if (key == "TableUI") {
            return SynthTableUI.createUI(c);
        }
        else if (key == "TableHeaderUI") {
            return SynthTableHeaderUI.createUI(c);
        }
        else if (key == "TextAreaUI") {
            return SynthTextAreaUI.createUI(c);
        }
        else if (key == "TextFieldUI") {
            return SynthTextFieldUI.createUI(c);
        }
        else if (key == "TextPaneUI") {
            return SynthTextPaneUI.createUI(c);
        }
        else if (key == "ToggleButtonUI") {
            return SynthToggleButtonUI.createUI(c);
        }
        else if (key == "ToolBarSeparatorUI") {
            return SynthSeparatorUI.createUI(c);
        }
        else if (key == "ToolBarUI") {
            return SynthToolBarUI.createUI(c);
        }
        else if (key == "ToolTipUI") {
            return SynthToolTipUI.createUI(c);
        }
        else if (key == "TreeUI") {
            return SynthTreeUI.createUI(c);
        }
        else if (key == "ViewportUI") {
            return SynthViewportUI.createUI(c);
        }
        return null;
    }


    /**
     * Creates a SynthLookAndFeel.
     * <p>
     * For the returned <code>SynthLookAndFeel</code> to be useful you need to
     * invoke <code>load</code> to specify the set of
     * <code>SynthStyle</code>s, or invoke <code>setStyleFactory</code>.
     *
     * @see #load
     * @see #setStyleFactory
     */
    public SynthLookAndFeel() {
        factory = new DefaultSynthStyleFactory();
        _handler = new Handler();
    }

    /**
     * Loads the set of <code>SynthStyle</code>s that will be used by
     * this <code>SynthLookAndFeel</code>. <code>resourceBase</code> is
     * used to resolve any path based resources, for example an
     * <code>Image</code> would be resolved by
     * <code>resourceBase.getResource(path)</code>. Refer to
     * <a href="doc-files/synthFileFormat.html">Synth File Format</a>
     * for more information.
     *
     * @param input InputStream to load from
     * @param resourceBase used to resolve any images or other resources
     * @throws ParseException if there is an error in parsing
     * @throws IllegalArgumentException if input or resourceBase is <code>null</code>
     */
    public void load(InputStream input, Class<?> resourceBase) throws
                       ParseException {
        if (resourceBase == null) {
            throw new IllegalArgumentException(
                "You must supply a valid resource base Class");
        }

        if (defaultsMap == null) {
            defaultsMap = new HashMap<String, Object>();
        }

        new SynthParser().parse(input, (DefaultSynthStyleFactory) factory,
                                null, resourceBase, defaultsMap);
    }

    /**
     * Loads the set of <code>SynthStyle</code>s that will be used by
     * this <code>SynthLookAndFeel</code>. Path based resources are resolved
     * relatively to the specified <code>URL</code> of the style. For example
     * an <code>Image</code> would be resolved by
     * <code>new URL(synthFile, path)</code>. Refer to
     * <a href="doc-files/synthFileFormat.html">Synth File Format</a> for more
     * information.
     *
     * @param url the <code>URL</code> to load the set of
     *     <code>SynthStyle</code> from
     * @throws ParseException if there is an error in parsing
     * @throws IllegalArgumentException if synthSet is <code>null</code>
     * @throws IOException if synthSet cannot be opened as an <code>InputStream</code>
     * @since 1.6
     */
    public void load(URL url) throws ParseException, IOException {
        if (url == null) {
            throw new IllegalArgumentException(
                "You must supply a valid Synth set URL");
        }

        if (defaultsMap == null) {
            defaultsMap = new HashMap<String, Object>();
        }

        InputStream input = url.openStream();
        new SynthParser().parse(input, (DefaultSynthStyleFactory) factory,
                                url, null, defaultsMap);
    }

    /**
     * Called by UIManager when this look and feel is installed.
     */
    @Override
    public void initialize() {
        super.initialize();
        DefaultLookup.setDefaultLookup(new SynthDefaultLookup());
        setStyleFactory(factory);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().
            addPropertyChangeListener(_handler);
    }

    /**
     * Called by UIManager when this look and feel is uninstalled.
     */
    @Override
    public void uninitialize() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().
            removePropertyChangeListener(_handler);
        // We should uninstall the StyleFactory here, but unfortunately
        // there are a handful of things that retain references to the
        // LookAndFeel and expect things to work
        super.uninitialize();
    }

    /**
     * Returns the defaults for this SynthLookAndFeel.
     *
     * @return Defaults table.
     */
    @Override
    public UIDefaults getDefaults() {
        UIDefaults table = new UIDefaults(60, 0.75f);

        Region.registerUIs(table);
        table.setDefaultLocale(Locale.getDefault());
        SwingAccessor.getUIDefaultsAccessor()
                     .addInternalBundle(table,
                             "com.sun.swing.internal.plaf.basic.resources.basic");
        SwingAccessor.getUIDefaultsAccessor()
                     .addInternalBundle(table,
                             "com.sun.swing.internal.plaf.synth.resources.synth");

        // SynthTabbedPaneUI supports rollover on tabs, GTK does not
        table.put("TabbedPane.isTabRollover", Boolean.TRUE);

        // These need to be defined for JColorChooser to work.
        table.put("ColorChooser.swatchesRecentSwatchSize",
                  new Dimension(10, 10));
        table.put("ColorChooser.swatchesDefaultRecentColor", Color.RED);
        table.put("ColorChooser.swatchesSwatchSize", new Dimension(10, 10));

        // These need to be defined for ImageView.
        table.put("html.pendingImage", SwingUtilities2.makeIcon(getClass(),
                                BasicLookAndFeel.class,
                                "icons/image-delayed.png"));
        table.put("html.missingImage", SwingUtilities2.makeIcon(getClass(),
                                BasicLookAndFeel.class,
                                "icons/image-failed.png"));

        // These are needed for PopupMenu.
        table.put("PopupMenu.selectedWindowInputMapBindings", new Object[] {
                  "ESCAPE", "cancel",
                    "DOWN", "selectNext",
                 "KP_DOWN", "selectNext",
                      "UP", "selectPrevious",
                   "KP_UP", "selectPrevious",
                    "LEFT", "selectParent",
                 "KP_LEFT", "selectParent",
                   "RIGHT", "selectChild",
                "KP_RIGHT", "selectChild",
                   "ENTER", "return",
                   "ctrl ENTER", "return",
                   "SPACE", "return"
        });
        table.put("PopupMenu.selectedWindowInputMapBindings.RightToLeft",
                  new Object[] {
                    "LEFT", "selectChild",
                 "KP_LEFT", "selectChild",
                   "RIGHT", "selectParent",
                "KP_RIGHT", "selectParent",
                  });

        // enabled antialiasing depending on desktop settings
        flushUnreferenced();
        SwingUtilities2.putAATextInfo(useLAFConditions(), table);
        new AATextListener(this);

        if (defaultsMap != null) {
            table.putAll(defaultsMap);
        }
        return table;
    }

    /**
     * Returns true, SynthLookAndFeel is always supported.
     *
     * @return true.
     */
    @Override
    public boolean isSupportedLookAndFeel() {
        return true;
    }

    /**
     * Returns false, SynthLookAndFeel is not a native look and feel.
     *
     * @return false
     */
    @Override
    public boolean isNativeLookAndFeel() {
        return false;
    }

    /**
     * Returns a textual description of SynthLookAndFeel.
     *
     * @return textual description of synth.
     */
    @Override
    public String getDescription() {
        return "Synth look and feel";
    }

    /**
     * Return a short string that identifies this look and feel.
     *
     * @return a short string identifying this look and feel.
     */
    @Override
    public String getName() {
        return "Synth look and feel";
    }

    /**
     * Return a string that identifies this look and feel.
     *
     * @return a short string identifying this look and feel.
     */
    @Override
    public String getID() {
        return "Synth";
    }

    /**
     * Returns whether or not the UIs should update their
     * <code>SynthStyles</code> from the <code>SynthStyleFactory</code>
     * when the ancestor of the <code>JComponent</code> changes. A subclass
     * that provided a <code>SynthStyleFactory</code> that based the
     * return value from <code>getStyle</code> off the containment hierarchy
     * would override this method to return true.
     *
     * @return whether or not the UIs should update their
     * <code>SynthStyles</code> from the <code>SynthStyleFactory</code>
     * when the ancestor changed.
     */
    public boolean shouldUpdateStyleOnAncestorChanged() {
        return false;
    }

    /**
     * Returns whether or not the UIs should update their styles when a
     * particular event occurs.
     *
     * @param ev a {@code PropertyChangeEvent}
     * @return whether or not the UIs should update their styles
     * @since 1.7
     */
    protected boolean shouldUpdateStyleOnEvent(PropertyChangeEvent ev) {
        String eName = ev.getPropertyName();
        if ("name" == eName || "componentOrientation" == eName) {
            return true;
        }
        if ("ancestor" == eName && ev.getNewValue() != null) {
            // Only update on an ancestor change when getting a valid
            // parent and the LookAndFeel wants this.
            return shouldUpdateStyleOnAncestorChanged();
        }
        return false;
    }

    /**
     * Returns the antialiasing information as specified by the host desktop.
     * Antialiasing might be forced off if the desktop is GNOME and the user
     * has set his locale to Chinese, Japanese or Korean. This is consistent
     * with what GTK does. See com.sun.java.swing.plaf.gtk.GtkLookAndFeel
     * for more information about CJK and antialiased fonts.
     *
     * @return the text antialiasing information associated to the desktop
     */
    private static boolean useLAFConditions() {
        String language = Locale.getDefault().getLanguage();
        Toolkit tk = Toolkit.getDefaultToolkit();
        String desktop =
            (tk instanceof SunToolkit) ? ((SunToolkit)tk).getDesktop() : null;

        boolean isCjkLocale = (Locale.CHINESE.getLanguage().equals(language) ||
                Locale.JAPANESE.getLanguage().equals(language) ||
                Locale.KOREAN.getLanguage().equals(language));
        boolean isGnome = "gnome".equals(desktop);
        boolean isLocal = SwingUtilities2.isLocalDisplay();

        return isLocal && (!isGnome || !isCjkLocale);
    }

    private static ReferenceQueue<LookAndFeel> queue = new ReferenceQueue<LookAndFeel>();

    private static void flushUnreferenced() {
        AATextListener aatl;
        while ((aatl = (AATextListener) queue.poll()) != null) {
            aatl.dispose();
        }
    }

    private static class AATextListener
        extends WeakReference<LookAndFeel> implements PropertyChangeListener {
        private String key = SunToolkit.DESKTOPFONTHINTS;

        AATextListener(LookAndFeel laf) {
            super(laf, queue);
            Toolkit tk = Toolkit.getDefaultToolkit();
            tk.addPropertyChangeListener(key, this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent pce) {
            UIDefaults defaults = UIManager.getLookAndFeelDefaults();
            if (defaults.getBoolean("Synth.doNotSetTextAA")) {
                dispose();
                return;
            }

            LookAndFeel laf = get();
            if (laf == null || laf != UIManager.getLookAndFeel()) {
                dispose();
                return;
            }

            SwingUtilities2.putAATextInfo(useLAFConditions(), defaults);

            updateUI();
        }

        void dispose() {
            Toolkit tk = Toolkit.getDefaultToolkit();
            tk.removePropertyChangeListener(key, this);
        }

        /**
         * Updates the UI of the passed in window and all its children.
         */
        private static void updateWindowUI(Window window) {
            updateStyles(window);
            Window[] ownedWins = window.getOwnedWindows();
            for (Window w : ownedWins) {
                updateWindowUI(w);
            }
        }

        /**
         * Updates the UIs of all the known Frames.
         */
        private static void updateAllUIs() {
            Frame[] appFrames = Frame.getFrames();
            for (Frame frame : appFrames) {
                updateWindowUI(frame);
            }
        }

        /**
         * Indicates if an updateUI call is pending.
         */
        private static boolean updatePending;

        /**
         * Sets whether or not an updateUI call is pending.
         */
        private static synchronized void setUpdatePending(boolean update) {
            updatePending = update;
        }

        /**
         * Returns true if a UI update is pending.
         */
        private static synchronized boolean isUpdatePending() {
            return updatePending;
        }

        protected void updateUI() {
            if (!isUpdatePending()) {
                setUpdatePending(true);
                Runnable uiUpdater = new Runnable() {
                    @Override
                    public void run() {
                        updateAllUIs();
                        setUpdatePending(false);
                    }
                };
                SwingUtilities.invokeLater(uiUpdater);
            }
        }
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        throw new NotSerializableException(this.getClass().getName());
    }

    private class Handler implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            Object newValue = evt.getNewValue();
            Object oldValue = evt.getOldValue();

            if ("focusOwner" == propertyName) {
                if (oldValue instanceof JComponent) {
                    repaintIfBackgroundsDiffer((JComponent)oldValue);

                }

                if (newValue instanceof JComponent) {
                    repaintIfBackgroundsDiffer((JComponent)newValue);
                }
            }
            else if ("managingFocus" == propertyName) {
                // De-register listener on old keyboard focus manager and
                // register it on the new one.
                KeyboardFocusManager manager =
                    (KeyboardFocusManager)evt.getSource();
                if (newValue.equals(Boolean.FALSE)) {
                    manager.removePropertyChangeListener(_handler);
                }
                else {
                    manager.addPropertyChangeListener(_handler);
                }
            }
        }

        /**
         * This is a support method that will check if the background colors of
         * the specified component differ between focused and unfocused states.
         * If the color differ the component will then repaint itself.
         *
         * @comp the component to check
         */
        private void repaintIfBackgroundsDiffer(JComponent comp) {
            ComponentUI ui = comp.getUI();
            if (ui instanceof SynthUI) {
                SynthUI synthUI = (SynthUI)ui;
                SynthContext context = synthUI.getContext(comp);
                SynthStyle style = context.getStyle();
                int state = context.getComponentState();

                // Get the current background color.
                Color currBG = style.getColor(context, ColorType.BACKGROUND);

                // Get the last background color.
                state ^= SynthConstants.FOCUSED;
                context.setComponentState(state);
                Color lastBG = style.getColor(context, ColorType.BACKGROUND);

                // Reset the component state back to original.
                state ^= SynthConstants.FOCUSED;
                context.setComponentState(state);

                // Repaint the component if the backgrounds differed.
                if (currBG != null && !currBG.equals(lastBG)) {
                    comp.repaint();
                }
            }
        }
    }
}
