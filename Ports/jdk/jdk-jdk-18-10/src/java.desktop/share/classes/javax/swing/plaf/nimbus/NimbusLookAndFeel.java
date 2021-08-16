/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.plaf.nimbus;

import java.awt.BorderLayout;
import static java.awt.BorderLayout.*;
import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.synth.Region;
import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.plaf.synth.SynthStyle;
import javax.swing.plaf.synth.SynthStyleFactory;
import javax.swing.plaf.UIResource;
import java.security.AccessController;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.JToolBar;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import sun.swing.ImageIconUIResource;
import javax.swing.plaf.synth.SynthIcon;
import sun.swing.plaf.GTKKeybindings;
import sun.swing.plaf.WindowsKeybindings;
import sun.security.action.GetPropertyAction;

/**
 * <p>The NimbusLookAndFeel class.</p>
 *
 * @author Jasper Potts
 * @author Richard Bair
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
public class NimbusLookAndFeel extends SynthLookAndFeel {

    /** Set of standard region names for UIDefaults Keys */
    private static final String[] COMPONENT_KEYS = new String[]{"ArrowButton", "Button",
                    "CheckBox", "CheckBoxMenuItem", "ColorChooser", "ComboBox",
                    "DesktopPane", "DesktopIcon", "EditorPane", "FileChooser",
                    "FormattedTextField", "InternalFrame",
                    "InternalFrameTitlePane", "Label", "List", "Menu",
                    "MenuBar", "MenuItem", "OptionPane", "Panel",
                    "PasswordField", "PopupMenu", "PopupMenuSeparator",
                    "ProgressBar", "RadioButton", "RadioButtonMenuItem",
                    "RootPane", "ScrollBar", "ScrollBarTrack", "ScrollBarThumb",
                    "ScrollPane", "Separator", "Slider", "SliderTrack",
                    "SliderThumb", "Spinner", "SplitPane", "TabbedPane",
                    "Table", "TableHeader", "TextArea", "TextField", "TextPane",
                    "ToggleButton", "ToolBar", "ToolTip", "Tree", "Viewport"};

    /**
     * A reference to the auto-generated file NimbusDefaults. This file contains
     * the default mappings and values for the look and feel as specified in the
     * visual designer.
     */
    private NimbusDefaults defaults;

    /**
     * Reference to populated LAD uidefaults
     */
    private UIDefaults uiDefaults;

    private DefaultsListener defaultsListener = new DefaultsListener();

    /**
     * Create a new NimbusLookAndFeel.
     */
    public NimbusLookAndFeel() {
        super();
        defaults = new NimbusDefaults();
    }

    /** Called by UIManager when this look and feel is installed. */
    @Override public void initialize() {
        super.initialize();
        defaults.initialize();
        // create synth style factory
        setStyleFactory(new SynthStyleFactory() {
            @Override
            public SynthStyle getStyle(JComponent c, Region r) {
                return defaults.getStyle(c, r);
            }
        });
    }


    /** Called by UIManager when this look and feel is uninstalled. */
    @Override public void uninitialize() {
        super.uninitialize();
        defaults.uninitialize();
        // clear all cached images to free memory
        ImageCache.getInstance().flush();
        UIManager.getDefaults().removePropertyChangeListener(defaultsListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override public UIDefaults getDefaults() {
        if (uiDefaults == null){
            // Detect platform
            String osName = getSystemProperty("os.name");
            boolean isWindows = osName != null && osName.contains("Windows");

            // We need to call super for basic's properties file.
            uiDefaults = super.getDefaults();
            defaults.initializeDefaults(uiDefaults);

            // Install Keybindings
            if (isWindows) {
                WindowsKeybindings.installKeybindings(uiDefaults);
            } else {
                GTKKeybindings.installKeybindings(uiDefaults);
            }

            // Add Titled Border
            uiDefaults.put("TitledBorder.titlePosition",
                    TitledBorder.ABOVE_TOP);
            uiDefaults.put("TitledBorder.border", new BorderUIResource(
                    new LoweredBorder()));
            uiDefaults.put("TitledBorder.titleColor",
                    getDerivedColor("text",0.0f,0.0f,0.23f,0,true));
            uiDefaults.put("TitledBorder.font",
                    new NimbusDefaults.DerivedFont("defaultFont",
                            1f, true, null));

            // Choose Dialog button positions
            uiDefaults.put("OptionPane.isYesLast", !isWindows);

            // Store Table ScrollPane Corner Component
            uiDefaults.put("Table.scrollPaneCornerComponent",
                    new UIDefaults.ActiveValue() {
                        @Override
                        public Object createValue(UIDefaults table) {
                            return new TableScrollPaneCorner();
                        }
                    });

            // Setup the settings for ToolBarSeparator which is custom
            // installed for Nimbus
            uiDefaults.put("ToolBarSeparator[Enabled].backgroundPainter",
                    new ToolBarSeparatorPainter());

            // Populate UIDefaults with a standard set of properties
            for (String componentKey : COMPONENT_KEYS) {
                String key = componentKey+".foreground";
                if (!uiDefaults.containsKey(key)){
                    uiDefaults.put(key,
                            new NimbusProperty(componentKey,"textForeground"));
                }
                key = componentKey+".background";
                if (!uiDefaults.containsKey(key)){
                    uiDefaults.put(key,
                            new NimbusProperty(componentKey,"background"));
                }
                key = componentKey+".font";
                if (!uiDefaults.containsKey(key)){
                    uiDefaults.put(key,
                            new NimbusProperty(componentKey,"font"));
                }
                key = componentKey+".disabledText";
                if (!uiDefaults.containsKey(key)){
                    uiDefaults.put(key,
                            new NimbusProperty(componentKey,"Disabled",
                                   "textForeground"));
                }
                key = componentKey+".disabled";
                if (!uiDefaults.containsKey(key)){
                    uiDefaults.put(key,
                            new NimbusProperty(componentKey,"Disabled",
                                    "background"));
                }
            }

            // FileView icon keys are used by some applications, we don't have
            // a computer icon at the moment so using home icon for now
            uiDefaults.put("FileView.computerIcon",
                    new LinkProperty("FileChooser.homeFolderIcon"));
            uiDefaults.put("FileView.directoryIcon",
                    new LinkProperty("FileChooser.directoryIcon"));
            uiDefaults.put("FileView.fileIcon",
                    new LinkProperty("FileChooser.fileIcon"));
            uiDefaults.put("FileView.floppyDriveIcon",
                    new LinkProperty("FileChooser.floppyDriveIcon"));
            uiDefaults.put("FileView.hardDriveIcon",
                    new LinkProperty("FileChooser.hardDriveIcon"));
        }
        return uiDefaults;
    }

    /**
     * Gets the style associated with the given component and region. This
     * will never return null. If an appropriate component and region cannot
     * be determined, then a default style is returned.
     *
     * @param c a non-null reference to a JComponent
     * @param r a non-null reference to the region of the component c
     * @return a non-null reference to a NimbusStyle.
     */
    public static NimbusStyle getStyle(JComponent c, Region r) {
        return (NimbusStyle)SynthLookAndFeel.getStyle(c, r);
    }

    /**
     * Return a short string that identifies this look and feel. This
     * String will be the unquoted String "Nimbus".
     *
     * @return a short string identifying this look and feel.
     */
    @Override public String getName() {
        return "Nimbus";
    }

    /**
     * Return a string that identifies this look and feel. This String will
     * be the unquoted String "Nimbus".
     *
     * @return a short string identifying this look and feel.
     */
    @Override public String getID() {
        return "Nimbus";
    }

    /**
     * Returns a textual description of this look and feel.
     *
     * @return textual description of this look and feel.
     */
    @Override public String getDescription() {
        return "Nimbus Look and Feel";
    }

    /**
     * {@inheritDoc}
     * @return {@code true}
     */
    @Override public boolean shouldUpdateStyleOnAncestorChanged() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overridden to return {@code true} when one of the following
     * properties change:
     * <ul>
     *   <li>{@code "Nimbus.Overrides"}
     *   <li>{@code "Nimbus.Overrides.InheritDefaults"}
     *   <li>{@code "JComponent.sizeVariant"}
     * </ul>
     *
     * @since 1.7
     */
    @Override
    protected boolean shouldUpdateStyleOnEvent(PropertyChangeEvent ev) {
        String eName = ev.getPropertyName();

        // These properties affect style cached inside NimbusDefaults (6860433)
        if ("name" == eName ||
            "ancestor" == eName ||
            "Nimbus.Overrides" == eName ||
            "Nimbus.Overrides.InheritDefaults" == eName ||
            "JComponent.sizeVariant" == eName) {

            JComponent c = (JComponent) ev.getSource();
            defaults.clearOverridesCache(c);
            return true;
        }

        return super.shouldUpdateStyleOnEvent(ev);
    }

    /**
     * <p>Registers a third party component with the NimbusLookAndFeel.</p>
     *
     * <p>Regions represent Components and areas within Components that act as
     * independent painting areas. Once registered with the NimbusLookAndFeel,
     * NimbusStyles for these Regions can be retrieved via the
     * <code>getStyle</code> method.</p>
     *
     * <p>The NimbusLookAndFeel uses a standard naming scheme for entries in the
     * UIDefaults table. The key for each property, state, painter, and other
     * default registered in UIDefaults for a specific Region will begin with
     * the specified <code>prefix</code></p>
     *
     * <p>For example, suppose I had a component named JFoo. Suppose I then registered
     * this component with the NimbusLookAndFeel in this manner:</p>
     *
     * <pre><code>
     *     laf.register(NimbusFooUI.FOO_REGION, "Foo");
     * </code></pre>
     *
     * <p>In this case, I could then register properties for this component with
     * UIDefaults in the following manner:</p>
     *
     * <pre><code>
     *     UIManager.put("Foo.background", new ColorUIResource(Color.BLACK));
     *     UIManager.put("Foo.Enabled.backgroundPainter", new FooBackgroundPainter());
     * </code></pre>
     *
     * <p>It is also possible to register a named component with Nimbus.
     * For example, suppose you wanted to style the background of a JPanel
     * named "MyPanel" differently from other JPanels. You could accomplish this
     * by doing the following:</p>
     *
     * <pre><code>
     *     laf.register(Region.PANEL, "\"MyPanel\"");
     *     UIManager.put("\"MyPanel\".background", new ColorUIResource(Color.RED));
     * </code></pre>
     *
     * @param region The Synth Region that is being registered. Such as Button, or
     *        ScrollBarThumb, or NimbusFooUI.FOO_REGION.
     * @param prefix The UIDefault prefix. For example, could be ComboBox, or if
     *        a named components, "MyComboBox", or even something like
     *        ToolBar."MyComboBox"."ComboBox.arrowButton"
     */
    public void register(Region region, String prefix) {
        defaults.register(region, prefix);
    }

    /**
     * Simple utility method that reads system keys.
     */
    @SuppressWarnings("removal")
    private String getSystemProperty(String key) {
        return AccessController.doPrivileged(new GetPropertyAction(key));
    }

    @Override
    public Icon getDisabledIcon(JComponent component, Icon icon) {
        if (icon instanceof SynthIcon) {
            SynthIcon si = (SynthIcon)icon;
            BufferedImage img = EffectUtils.createCompatibleTranslucentImage(
                    si.getIconWidth(), si.getIconHeight());
            Graphics2D gfx = img.createGraphics();
            si.paintIcon(component, gfx, 0, 0);
            gfx.dispose();
            return new ImageIconUIResource(GrayFilter.createDisabledImage(img));
        } else {
            return super.getDisabledIcon(component, icon);
        }
    }

    /**
     * Get a derived color, derived colors are shared instances and is color
     * value will change when its parent UIDefault color changes.
     *
     * @param uiDefaultParentName The parent UIDefault key
     * @param hOffset             The hue offset
     * @param sOffset             The saturation offset
     * @param bOffset             The brightness offset
     * @param aOffset             The alpha offset
     * @param uiResource          True if the derived color should be a
     *                            UIResource, false if it should not be
     * @return The stored derived color
     */
    public Color getDerivedColor(String uiDefaultParentName,
                                 float hOffset, float sOffset,
                                 float bOffset, int aOffset,
                                 boolean uiResource) {
        return defaults.getDerivedColor(uiDefaultParentName, hOffset, sOffset,
                bOffset, aOffset, uiResource);
    }

    /**
     * Decodes and returns a color, which is derived from an offset between two
     * other colors.
     *
     * @param color1   The first color
     * @param color2   The second color
     * @param midPoint The offset between color 1 and color 2, a value of 0.0 is
     *                 color 1 and 1.0 is color 2;
     * @param uiResource True if the derived color should be a UIResource
     * @return The derived color
     */
    protected final Color getDerivedColor(Color color1, Color color2,
                                      float midPoint, boolean uiResource) {
        int argb = deriveARGB(color1, color2, midPoint);
        if (uiResource) {
            return new ColorUIResource(argb);
        } else {
            return new Color(argb);
        }
    }

    /**
     * Decodes and returns a color, which is derived from a offset between two
     * other colors.
     *
     * @param color1   The first color
     * @param color2   The second color
     * @param midPoint The offset between color 1 and color 2, a value of 0.0 is
     *                 color 1 and 1.0 is color 2;
     * @return The derived color, which will be a UIResource
     */
    protected final Color getDerivedColor(Color color1, Color color2,
                                      float midPoint) {
        return getDerivedColor(color1, color2, midPoint, true);
    }

    /**
     * Package private method which returns either BorderLayout.NORTH,
     * BorderLayout.SOUTH, BorderLayout.EAST, or BorderLayout.WEST depending
     * on the location of the toolbar in its parent. The toolbar might be
     * in PAGE_START, PAGE_END, CENTER, or some other position, but will be
     * resolved to either NORTH,SOUTH,EAST, or WEST based on where the toolbar
     * actually IS, with CENTER being NORTH.
     *
     * This code is used to determine where the border line should be drawn
     * by the custom toolbar states, and also used by NimbusIcon to determine
     * whether the handle icon needs to be shifted to look correct.
     *
     * Toollbars are unfortunately odd in the way these things are handled,
     * and so this code exists to unify the logic related to toolbars so it can
     * be shared among the static files such as NimbusIcon and generated files
     * such as the ToolBar state classes.
     */
    static Object resolveToolbarConstraint(JToolBar toolbar) {
        //NOTE: we don't worry about component orientation or PAGE_END etc
        //because the BasicToolBarUI always uses an absolute position of
        //NORTH/SOUTH/EAST/WEST.
        if (toolbar != null) {
            Container parent = toolbar.getParent();
            if (parent != null) {
                LayoutManager m = parent.getLayout();
                if (m instanceof BorderLayout) {
                    BorderLayout b = (BorderLayout)m;
                    Object con = b.getConstraints(toolbar);
                    if (con == SOUTH || con == EAST || con == WEST) {
                        return con;
                    }
                    return NORTH;
                }
            }
        }
        return NORTH;
    }

    /**
     * Derives the ARGB value for a color based on an offset between two
     * other colors.
     *
     * @param color1   The first color
     * @param color2   The second color
     * @param midPoint The offset between color 1 and color 2, a value of 0.0 is
     *                 color 1 and 1.0 is color 2;
     * @return the ARGB value for a new color based on this derivation
     */
    static int deriveARGB(Color color1, Color color2, float midPoint) {
        int r = color1.getRed() +
                Math.round((color2.getRed() - color1.getRed()) * midPoint);
        int g = color1.getGreen() +
                Math.round((color2.getGreen() - color1.getGreen()) * midPoint);
        int b = color1.getBlue() +
                Math.round((color2.getBlue() - color1.getBlue()) * midPoint);
        int a = color1.getAlpha() +
                Math.round((color2.getAlpha() - color1.getAlpha()) * midPoint);
        return ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                (b & 0xFF);
    }

    /**
     * Simple Symbolic Link style UIDefalts Property
     */
    private class LinkProperty implements UIDefaults.ActiveValue, UIResource{
        private String dstPropName;

        private LinkProperty(String dstPropName) {
            this.dstPropName = dstPropName;
        }

        @Override
        public Object createValue(UIDefaults table) {
            return UIManager.get(dstPropName);
        }
    }

    /**
     * Nimbus Property that looks up Nimbus keys for standard key names. For
     * example "Button.background" --> "Button[Enabled].backgound"
     */
    private class NimbusProperty implements UIDefaults.ActiveValue, UIResource {
        private String prefix;
        private String state = null;
        private String suffix;
        private boolean isFont;

        private NimbusProperty(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
            isFont = "font".equals(suffix);
        }

        private NimbusProperty(String prefix, String state, String suffix) {
            this(prefix,suffix);
            this.state = state;
        }

        /**
         * Creates the value retrieved from the <code>UIDefaults</code> table.
         * The object is created each time it is accessed.
         *
         * @param table a <code>UIDefaults</code> table
         * @return the created <code>Object</code>
         */
        @Override
        public Object createValue(UIDefaults table) {
            Object obj = null;
            // check specified state
            if (state!=null){
                obj = uiDefaults.get(prefix+"["+state+"]."+suffix);
            }
            // check enabled state
            if (obj==null){
                obj = uiDefaults.get(prefix+"[Enabled]."+suffix);
            }
            // check for defaults
            if (obj==null){
                if (isFont) {
                    obj = uiDefaults.get("defaultFont");
                } else {
                    obj = uiDefaults.get(suffix);
                }
            }
            return obj;
        }
    }

    private Map<String, Map<String, Object>> compiledDefaults = null;
    private boolean defaultListenerAdded = false;

    static String parsePrefix(String key) {
        if (key == null) {
            return null;
        }
        boolean inquotes = false;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == '"') {
                inquotes = !inquotes;
            } else if ((c == '[' || c == '.') && !inquotes) {
                return key.substring(0, i);
            }
        }
        return null;
    }

    Map<String, Object> getDefaultsForPrefix(String prefix) {
        if (compiledDefaults == null) {
            compiledDefaults = new HashMap<String, Map<String, Object>>();
            for (Map.Entry<Object, Object> entry: UIManager.getDefaults().entrySet()) {
                if (entry.getKey() instanceof String) {
                    addDefault((String) entry.getKey(), entry.getValue());
                }
            }
            if (! defaultListenerAdded) {
                UIManager.getDefaults().addPropertyChangeListener(defaultsListener);
                defaultListenerAdded = true;
            }
        }
        return compiledDefaults.get(prefix);
    }

    private void addDefault(String key, Object value) {
        if (compiledDefaults == null) {
            return;
        }

        String prefix = parsePrefix(key);
        if (prefix != null) {
            Map<String, Object> keys = compiledDefaults.get(prefix);
            if (keys == null) {
                keys = new HashMap<String, Object>();
                compiledDefaults.put(prefix, keys);
            }
            keys.put(key, value);
        }
    }

    private class DefaultsListener implements PropertyChangeListener {
        @Override public void propertyChange(PropertyChangeEvent ev) {
            String key = ev.getPropertyName();
            if ("UIDefaults".equals(key)) {
                compiledDefaults = null;
            } else {
                addDefault(key, ev.getNewValue());
            }
        }
    }
}
