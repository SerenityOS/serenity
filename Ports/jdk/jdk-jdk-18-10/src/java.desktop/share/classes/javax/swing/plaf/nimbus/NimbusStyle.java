/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.Painter;

import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.synth.ColorType;
import static javax.swing.plaf.synth.SynthConstants.*;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthPainter;
import javax.swing.plaf.synth.SynthStyle;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>A SynthStyle implementation used by Nimbus. Each Region that has been
 * registered with the NimbusLookAndFeel will have an associated NimbusStyle.
 * Third party components that are registered with the NimbusLookAndFeel will
 * therefore be handed a NimbusStyle from the look and feel from the
 * #getStyle(JComponent, Region) method.</p>
 *
 * <p>This class properly reads and retrieves values placed in the UIDefaults
 * according to the standard Nimbus naming conventions. It will create and
 * retrieve painters, fonts, colors, and other data stored there.</p>
 *
 * <p>NimbusStyle also supports the ability to override settings on a per
 * component basis. NimbusStyle checks the component's client property map for
 * "Nimbus.Overrides". If the value associated with this key is an instance of
 * UIDefaults, then the values in that defaults table will override the standard
 * Nimbus defaults in UIManager, but for that component instance only.</p>
 *
 * <p>Optionally, you may specify the client property
 * "Nimbus.Overrides.InheritDefaults". If true, this client property indicates
 * that the defaults located in UIManager should first be read, and then
 * replaced with defaults located in the component client properties. If false,
 * then only the defaults located in the component client property map will
 * be used. If not specified, it is assumed to be true.</p>
 *
 * <p>You must specify "Nimbus.Overrides" for "Nimbus.Overrides.InheritDefaults"
 * to have any effect. "Nimbus.Overrides" indicates whether there are any
 * overrides, while "Nimbus.Overrides.InheritDefaults" indicates whether those
 * overrides should first be initialized with the defaults from UIManager.</p>
 *
 * <p>The NimbusStyle is reloaded whenever a property change event is fired
 * for a component for "Nimbus.Overrides" or "Nimbus.Overrides.InheritDefaults".
 * So for example, setting a new UIDefaults on a component would cause the
 * style to be reloaded.</p>
 *
 * <p>The values are only read out of UIManager once, and then cached. If
 * you need to read the values again (for example, if the UI is being reloaded),
 * then discard this NimbusStyle and read a new one from NimbusLookAndFeel
 * using NimbusLookAndFeel.getStyle.</p>
 *
 * <p>The primary API of interest in this class for 3rd party component authors
 * are the three methods which retrieve painters: #getBackgroundPainter,
 * #getForegroundPainter, and #getBorderPainter.</p>
 *
 * <p>NimbusStyle allows you to specify custom states, or modify the order of
 * states. Synth (and thus Nimbus) has the concept of a "state". For example,
 * a JButton might be in the "MOUSE_OVER" state, or the "ENABLED" state, or the
 * "DISABLED" state. These are all "standard" states which are defined in synth,
 * and which apply to all synth Regions.</p>
 *
 * <p>Sometimes, however, you need to have a custom state. For example, you
 * want JButton to render differently if it's parent is a JToolbar. In Nimbus,
 * you specify these custom states by including a special key in UIDefaults.
 * The following UIDefaults entries define three states for this button:</p>
 *
 * <pre><code>
 *     JButton.States = Enabled, Disabled, Toolbar
 *     JButton[Enabled].backgroundPainter = somePainter
 *     JButton[Disabled].background = BLUE
 *     JButton[Toolbar].backgroundPainter = someOtherPaint
 * </code></pre>
 *
 * <p>As you can see, the <code>JButton.States</code> entry lists the states
 * that the JButton style will support. You then specify the settings for
 * each state. If you do not specify the <code>JButton.States</code> entry,
 * then the standard Synth states will be assumed. If you specify the entry
 * but the list of states is empty or null, then the standard synth states
 * will be assumed.</p>
 *
 * @author Richard Bair
 * @author Jasper Potts
 */
public final class NimbusStyle extends SynthStyle {
    /* Keys and scales for large/small/mini components, based on Apples sizes */
    /** Large key */
    public static final String LARGE_KEY = "large";
    /** Small key */
    public static final String SMALL_KEY = "small";
    /** Mini key */
    public static final String MINI_KEY = "mini";
    /** Large scale */
    public static final double LARGE_SCALE = 1.15;
    /** Small scale */
    public static final double SMALL_SCALE = 0.857;
    /** Mini scale */
    public static final double MINI_SCALE = 0.714;

    /**
     * Special constant used for performance reasons during the get() method.
     * If get() runs through all of the search locations and determines that
     * there is no value, then NULL will be placed into the values map. This way
     * on subsequent lookups it will simply extract NULL, see it, and return
     * null rather than continuing the lookup procedure.
     */
    private static final Object NULL = '\0';
    /**
     * <p>The Color to return from getColorForState if it would otherwise have
     * returned null.</p>
     *
     * <p>Returning null from getColorForState is a very bad thing, as it causes
     * the AWT peer for the component to install a SystemColor, which is not a
     * UIResource. As a result, if <code>null</code> is returned from
     * getColorForState, then thereafter the color is not updated for other
     * states or on LAF changes or updates. This DEFAULT_COLOR is used to
     * ensure that a ColorUIResource is always returned from
     * getColorForState.</p>
     */
    private static final Color DEFAULT_COLOR = new ColorUIResource(Color.BLACK);
    /**
     * Simple Comparator for ordering the RuntimeStates according to their
     * rank.
     */
    private static final Comparator<RuntimeState> STATE_COMPARATOR =
        new Comparator<RuntimeState>() {
            @Override
            public int compare(RuntimeState a, RuntimeState b) {
                return a.state - b.state;
            }
        };
    /**
     * The prefix for the component or region that this NimbusStyle
     * represents. This prefix is used to lookup state in the UIManager.
     * It should be something like Button or Slider.Thumb or "MyButton" or
     * ComboBox."ComboBox.arrowButton" or "MyComboBox"."ComboBox.arrowButton"
     */
    private String prefix;
    /**
     * The SynthPainter that will be returned from this NimbusStyle. The
     * SynthPainter returned will be a SynthPainterImpl, which will in turn
     * delegate back to this NimbusStyle for the proper Painter (not
     * SynthPainter) to use for painting the foreground, background, or border.
     */
    private SynthPainter painter;
    /**
     * Data structure containing all of the defaults, insets, states, and other
     * values associated with this style. This instance refers to default
     * values, and are used when no overrides are discovered in the client
     * properties of a component. These values are lazily created on first
     * access.
     */
    private Values values;

    /**
     * A temporary CacheKey used to perform lookups. This pattern avoids
     * creating useless garbage keys, or concatenating strings, etc.
     */
    private CacheKey tmpKey = new CacheKey("", 0);

    /**
     * Some NimbusStyles are created for a specific component only. In Nimbus,
     * this happens whenever the component has as a client property a
     * UIDefaults which overrides (or supplements) those defaults found in
     * UIManager.
     */
    private WeakReference<JComponent> component;

    /**
     * Create a new NimbusStyle. Only the prefix must be supplied. At the
     * appropriate time, installDefaults will be called. At that point, all of
     * the state information will be pulled from UIManager and stored locally
     * within this style.
     *
     * @param prefix Something like Button or Slider.Thumb or
     *        org.jdesktop.swingx.JXStatusBar or ComboBox."ComboBox.arrowButton"
     * @param c an optional reference to a component that this NimbusStyle
     *        should be associated with. This is only used when the component
     *        has Nimbus overrides registered in its client properties and
     *        should be null otherwise.
     */
    NimbusStyle(String prefix, JComponent c) {
        if (c != null) {
            this.component = new WeakReference<JComponent>(c);
        }
        this.prefix = prefix;
        this.painter = new SynthPainterImpl(this);
    }

    /**
     * {@inheritDoc}
     *
     * Overridden to cause this style to populate itself with data from
     * UIDefaults, if necessary.
     */
    @Override public void installDefaults(SynthContext ctx) {
        validate();

        //delegate to the superclass to install defaults such as background,
        //foreground, font, and opaque onto the swing component.
        super.installDefaults(ctx);
    }

    /**
     * Pulls data out of UIDefaults, if it has not done so already, and sets
     * up the internal state.
     */
    private void validate() {
        // a non-null values object is the flag we use to determine whether
        // to reparse from UIManager.
        if (values != null) return;

        // reconstruct this NimbusStyle based on the entries in the UIManager
        // and possibly based on any overrides within the component's
        // client properties (assuming such a component exists and contains
        // any Nimbus.Overrides)
        values = new Values();

        Map<String, Object> defaults =
                ((NimbusLookAndFeel) UIManager.getLookAndFeel()).
                        getDefaultsForPrefix(prefix);

        // inspect the client properties for the key "Nimbus.Overrides". If the
        // value is an instance of UIDefaults, then these defaults are used
        // in place of, or in addition to, the defaults in UIManager.
        if (component != null) {
            // We know component.get() is non-null here, as if the component
            // were GC'ed, we wouldn't be processing its style.
            Object o = component.get().getClientProperty("Nimbus.Overrides");
            if (o instanceof UIDefaults) {
                Object i = component.get().getClientProperty(
                        "Nimbus.Overrides.InheritDefaults");
                boolean inherit = i instanceof Boolean ? (Boolean)i : true;
                UIDefaults d = (UIDefaults)o;
                TreeMap<String, Object> map = new TreeMap<String, Object>();
                for (Object obj : d.keySet()) {
                    if (obj instanceof String) {
                        String key = (String)obj;
                        if (key.startsWith(prefix)) {
                            map.put(key, d.get(key));
                        }
                    }
                }
                if (inherit) {
                    defaults.putAll(map);
                } else {
                    defaults = map;
                }
            }
        }

        //a list of the different types of states used by this style. This
        //list may contain only "standard" states (those defined by Synth),
        //or it may contain custom states, or it may contain only "standard"
        //states but list them in a non-standard order.
        List<State<?>> states = new ArrayList<>();
        //a map of state name to code
        Map<String,Integer> stateCodes = new HashMap<>();
        //This is a list of runtime "state" context objects. These contain
        //the values associated with each state.
        List<RuntimeState> runtimeStates = new ArrayList<>();

        //determine whether there are any custom states, or custom state
        //order. If so, then read all those custom states and define the
        //"values" stateTypes to be a non-null array.
        //Otherwise, let the "values" stateTypes be null to indicate that
        //there are no custom states or custom state ordering
        String statesString = (String)defaults.get(prefix + ".States");
        if (statesString != null) {
            String[] s = statesString.split(",");
            for (int i=0; i<s.length; i++) {
                s[i] = s[i].trim();
                if (!State.isStandardStateName(s[i])) {
                    //this is a non-standard state name, so look for the
                    //custom state associated with it
                    String stateName = prefix + "." + s[i];
                    State<?> customState = (State)defaults.get(stateName);
                    if (customState != null) {
                        states.add(customState);
                    }
                } else {
                    states.add(State.getStandardState(s[i]));
                }
            }

            //if there were any states defined, then set the stateTypes array
            //to be non-null. Otherwise, leave it null (meaning, use the
            //standard synth states).
            if (states.size() > 0) {
                values.stateTypes = states.toArray(new State<?>[states.size()]);
            }

            //assign codes for each of the state types
            int code = 1;
            for (State<?> state : states) {
                stateCodes.put(state.getName(), code);
                code <<= 1;
            }
        } else {
            //since there were no custom states defined, setup the list of
            //standard synth states. Note that the "v.stateTypes" is not
            //being set here, indicating that at runtime the state selection
            //routines should use standard synth states instead of custom
            //states. I do need to popuplate this temp list now though, so that
            //the remainder of this method will function as expected.
            states.add(State.Enabled);
            states.add(State.MouseOver);
            states.add(State.Pressed);
            states.add(State.Disabled);
            states.add(State.Focused);
            states.add(State.Selected);
            states.add(State.Default);

            //assign codes for the states
            stateCodes.put("Enabled", ENABLED);
            stateCodes.put("MouseOver", MOUSE_OVER);
            stateCodes.put("Pressed", PRESSED);
            stateCodes.put("Disabled", DISABLED);
            stateCodes.put("Focused", FOCUSED);
            stateCodes.put("Selected", SELECTED);
            stateCodes.put("Default", DEFAULT);
        }

        //Now iterate over all the keys in the defaults table
        for (String key : defaults.keySet()) {
            //The key is something like JButton.Enabled.backgroundPainter,
            //or JButton.States, or JButton.background.
            //Remove the "JButton." portion of the key
            String temp = key.substring(prefix.length());
            //if there is a " or : then we skip it because it is a subregion
            //of some kind
            if (temp.indexOf('"') != -1 || temp.indexOf(':') != -1) continue;
            //remove the separator
            temp = temp.substring(1);
            //At this point, temp may be any of the following:
            //background
            //[Enabled].background
            //[Enabled+MouseOver].background
            //property.foo

            //parse out the states and the property
            String stateString = null;
            String property = null;
            int bracketIndex = temp.indexOf(']');
            if (bracketIndex < 0) {
                //there is not a state string, so property = temp
                property = temp;
            } else {
                stateString = temp.substring(0, bracketIndex);
                property = temp.substring(bracketIndex + 2);
            }

            //now that I have the state (if any) and the property, get the
            //value for this property and install it where it belongs
            if (stateString == null) {
                //there was no state, just a property. Check for the custom
                //"contentMargins" property (which is handled specially by
                //Synth/Nimbus). Also check for the property being "States",
                //in which case it is not a real property and should be ignored.
                //otherwise, assume it is a property and install it on the
                //values object
                if ("contentMargins".equals(property)) {
                    values.contentMargins = (Insets)defaults.get(key);
                } else if ("States".equals(property)) {
                    //ignore
                } else {
                    values.defaults.put(property, defaults.get(key));
                }
            } else {
                //it is possible that the developer has a malformed UIDefaults
                //entry, such that something was specified in the place of
                //the State portion of the key but it wasn't a state. In this
                //case, skip will be set to true
                boolean skip = false;
                //this variable keeps track of the int value associated with
                //the state. See SynthState for details.
                int componentState = 0;
                //Multiple states may be specified in the string, such as
                //Enabled+MouseOver
                String[] stateParts = stateString.split("\\+");
                //For each state, we need to find the State object associated
                //with it, or skip it if it cannot be found.
                for (String s : stateParts) {
                    if (stateCodes.containsKey(s)) {
                        componentState |= stateCodes.get(s);
                    } else {
                        //Was not a state. Maybe it was a subregion or something
                        //skip it.
                        skip = true;
                        break;
                    }
                }

                if (skip) continue;

                //find the RuntimeState for this State
                RuntimeState rs = null;
                for (RuntimeState s : runtimeStates) {
                    if (s.state == componentState) {
                        rs = s;
                        break;
                    }
                }

                //couldn't find the runtime state, so create a new one
                if (rs == null) {
                    rs = new RuntimeState(componentState, stateString);
                    runtimeStates.add(rs);
                }

                //check for a couple special properties, such as for the
                //painters. If these are found, then set the specially on
                //the runtime state. Else, it is just a normal property,
                //so put it in the UIDefaults associated with that runtime
                //state
                if ("backgroundPainter".equals(property)) {
                    rs.backgroundPainter = getPainter(defaults, key);
                } else if ("foregroundPainter".equals(property)) {
                    rs.foregroundPainter = getPainter(defaults, key);
                } else if ("borderPainter".equals(property)) {
                    rs.borderPainter = getPainter(defaults, key);
                } else {
                    rs.defaults.put(property, defaults.get(key));
                }
            }
        }

        //now that I've collected all the runtime states, I'll sort them based
        //on their integer "state" (see SynthState for how this works).
        Collections.sort(runtimeStates, STATE_COMPARATOR);

        //finally, set the array of runtime states on the values object
        values.states = runtimeStates.toArray(new RuntimeState[runtimeStates.size()]);
    }

    private Painter<Object> getPainter(Map<String, Object> defaults, String key) {
        Object p = defaults.get(key);
        if (p instanceof UIDefaults.LazyValue) {
            p = ((UIDefaults.LazyValue)p).createValue(UIManager.getDefaults());
        }
        @SuppressWarnings("unchecked")
        Painter<Object> tmp = (p instanceof Painter ? (Painter)p : null);
        return tmp;
    }

    /**
     * {@inheritDoc}
     *
     * Overridden to cause this style to populate itself with data from
     * UIDefaults, if necessary.
     */
    @Override public Insets getInsets(SynthContext ctx, Insets in) {
        if (in == null) {
            in = new Insets(0, 0, 0, 0);
        }

        Values v = getValues(ctx);

        if (v.contentMargins == null) {
            in.bottom = in.top = in.left = in.right = 0;
            return in;
        } else {
            in.bottom = v.contentMargins.bottom;
            in.top = v.contentMargins.top;
            in.left = v.contentMargins.left;
            in.right = v.contentMargins.right;
            // Account for scale
            // The key "JComponent.sizeVariant" is used to match Apple's LAF
            String scaleKey = (String)ctx.getComponent().getClientProperty(
                    "JComponent.sizeVariant");
            if (scaleKey != null){
                if (LARGE_KEY.equals(scaleKey)){
                    in.bottom *= LARGE_SCALE;
                    in.top *= LARGE_SCALE;
                    in.left *= LARGE_SCALE;
                    in.right *= LARGE_SCALE;
                } else if (SMALL_KEY.equals(scaleKey)){
                    in.bottom *= SMALL_SCALE;
                    in.top *= SMALL_SCALE;
                    in.left *= SMALL_SCALE;
                    in.right *= SMALL_SCALE;
                } else if (MINI_KEY.equals(scaleKey)){
                    in.bottom *= MINI_SCALE;
                    in.top *= MINI_SCALE;
                    in.left *= MINI_SCALE;
                    in.right *= MINI_SCALE;
                }
            }
            return in;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overridden to cause this style to populate itself with data from
     * UIDefaults, if necessary.</p>
     *
     * <p>In addition, NimbusStyle handles ColorTypes slightly differently from
     * Synth.</p>
     * <ul>
     *  <li>ColorType.BACKGROUND will equate to the color stored in UIDefaults
     *      named "background".</li>
     *  <li>ColorType.TEXT_BACKGROUND will equate to the color stored in
     *      UIDefaults named "textBackground".</li>
     *  <li>ColorType.FOREGROUND will equate to the color stored in UIDefaults
     *      named "textForeground".</li>
     *  <li>ColorType.TEXT_FOREGROUND will equate to the color stored in
     *      UIDefaults named "textForeground".</li>
     * </ul>
     */
    @Override protected Color getColorForState(SynthContext ctx, ColorType type) {
        String key = null;
        if (type == ColorType.BACKGROUND) {
            key = "background";
        } else if (type == ColorType.FOREGROUND) {
            //map FOREGROUND as TEXT_FOREGROUND
            key = "textForeground";
        } else if (type == ColorType.TEXT_BACKGROUND) {
            key = "textBackground";
        } else if (type == ColorType.TEXT_FOREGROUND) {
            key = "textForeground";
        } else if (type == ColorType.FOCUS) {
            key = "focus";
        } else if (type != null) {
            key = type.toString();
        } else {
            return DEFAULT_COLOR;
        }
        Color c = (Color) get(ctx, key);
        //if all else fails, return a default color (which is a ColorUIResource)
        if (c == null) c = DEFAULT_COLOR;
        return c;
    }

    /**
     * {@inheritDoc}
     *
     * Overridden to cause this style to populate itself with data from
     * UIDefaults, if necessary. If a value named "font" is not found in
     * UIDefaults, then the "defaultFont" font in UIDefaults will be returned
     * instead.
     */
    @Override protected Font getFontForState(SynthContext ctx) {
        Font f = (Font)get(ctx, "font");
        if (f == null) f = UIManager.getFont("defaultFont");

        // Account for scale
        // The key "JComponent.sizeVariant" is used to match Apple's LAF
        String scaleKey = (String)ctx.getComponent().getClientProperty(
                "JComponent.sizeVariant");
        if (scaleKey != null){
            if (LARGE_KEY.equals(scaleKey)){
                f = f.deriveFont(Math.round(f.getSize2D()*LARGE_SCALE));
            } else if (SMALL_KEY.equals(scaleKey)){
                f = f.deriveFont(Math.round(f.getSize2D()*SMALL_SCALE));
            } else if (MINI_KEY.equals(scaleKey)){
                f = f.deriveFont(Math.round(f.getSize2D()*MINI_SCALE));
            }
        }
        return f;
    }

    /**
     * {@inheritDoc}
     *
     * Returns the SynthPainter for this style, which ends up delegating to
     * the Painters installed in this style.
     */
    @Override public SynthPainter getPainter(SynthContext ctx) {
        return painter;
    }

    /**
     * {@inheritDoc}
     *
     * Overridden to cause this style to populate itself with data from
     * UIDefaults, if necessary. If opacity is not specified in UI defaults,
     * then it defaults to being non-opaque.
     */
    @Override public boolean isOpaque(SynthContext ctx) {
        // Force Table CellRenderers to be opaque
        if ("Table.cellRenderer".equals(ctx.getComponent().getName())) {
            return true;
        }
        Boolean opaque = (Boolean)get(ctx, "opaque");
        return opaque == null ? false : opaque;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overridden to cause this style to populate itself with data from
     * UIDefaults, if necessary.</p>
     *
     * <p>Properties in UIDefaults may be specified in a chained manner. For
     * example:
     * <pre>
     * background
     * Button.opacity
     * Button.Enabled.foreground
     * Button.Enabled+Selected.background
     * </pre>
     *
     * <p>In this example, suppose you were in the Enabled+Selected state and
     * searched for "foreground". In this case, we first check for
     * Button.Enabled+Selected.foreground, but no such color exists. We then
     * fall back to the next valid state, in this case,
     * Button.Enabled.foreground, and have a match. So we return it.</p>
     *
     * <p>Again, if we were in the state Enabled and looked for "background", we
     * wouldn't find it in Button.Enabled, or in Button, but would at the top
     * level in UIManager. So we return that value.</p>
     *
     * <p>One special note: the "key" passed to this method could be of the form
     * "background" or "Button.background" where "Button" equals the prefix
     * passed to the NimbusStyle constructor. In either case, it looks for
     * "background".</p>
     *
     * @param ctx SynthContext identifying requester
     * @param key must not be null
     */
    @Override public Object get(SynthContext ctx, Object key) {
        Values v = getValues(ctx);

        // strip off the prefix, if there is one.
        String fullKey = key.toString();
        String partialKey = fullKey.substring(fullKey.indexOf('.') + 1);

        Object obj = null;
        int xstate = getExtendedState(ctx, v);

        // check the cache
        tmpKey.init(partialKey, xstate);
        obj = v.cache.get(tmpKey);
        boolean wasInCache = obj != null;
        if (!wasInCache){
            // Search exact matching states and then lesser matching states
            RuntimeState s = null;
            int[] lastIndex = new int[] {-1};
            while (obj == null &&
                    (s = getNextState(v.states, lastIndex, xstate)) != null) {
                obj = s.defaults.get(partialKey);
            }
            // Search Region Defaults
            if (obj == null && v.defaults != null) {
                obj = v.defaults.get(partialKey);
            }
            // return found object
            // Search UIManager Defaults
            if (obj == null) obj = UIManager.get(fullKey);
            // Search Synth Defaults for InputMaps
            if (obj == null && partialKey.equals("focusInputMap")) {
                obj = super.get(ctx, fullKey);
            }
            // if all we got was a null, store this fact for later use
            v.cache.put(new CacheKey(partialKey, xstate),
                    obj == null ? NULL : obj);
        }
        // return found object
        return obj == NULL ? null : obj;
    }

    @SuppressWarnings("unchecked")
    private static Painter<Object> paintFilter(@SuppressWarnings("rawtypes") Painter painter) {
        return (Painter<Object>) painter;
    }


    /**
     * Gets the appropriate background Painter, if there is one, for the state
     * specified in the given SynthContext. This method does appropriate
     * fallback searching, as described in #get.
     *
     * @param ctx The SynthContext. Must not be null.
     * @return The background painter associated for the given state, or null if
     * none could be found.
     */
    public Painter<Object> getBackgroundPainter(SynthContext ctx) {
        Values v = getValues(ctx);
        int xstate = getExtendedState(ctx, v);
        Painter<Object> p = null;

        // check the cache
        tmpKey.init("backgroundPainter$$instance", xstate);
        p = paintFilter((Painter)v.cache.get(tmpKey));
        if (p != null) return p;

        // not in cache, so lookup and store in cache
        RuntimeState s = null;
        int[] lastIndex = new int[] {-1};
        while ((s = getNextState(v.states, lastIndex, xstate)) != null) {
            if (s.backgroundPainter != null) {
                p = paintFilter(s.backgroundPainter);
                break;
            }
        }
        if (p == null) p = paintFilter((Painter)get(ctx, "backgroundPainter"));
        if (p != null) {
            v.cache.put(new CacheKey("backgroundPainter$$instance", xstate), p);
        }
        return p;
    }

    /**
     * Gets the appropriate foreground Painter, if there is one, for the state
     * specified in the given SynthContext. This method does appropriate
     * fallback searching, as described in #get.
     *
     * @param ctx The SynthContext. Must not be null.
     * @return The foreground painter associated for the given state, or null if
     * none could be found.
     */
    public Painter<Object> getForegroundPainter(SynthContext ctx) {
        Values v = getValues(ctx);
        int xstate = getExtendedState(ctx, v);
        Painter<Object> p = null;

        // check the cache
        tmpKey.init("foregroundPainter$$instance", xstate);
        p = paintFilter((Painter)v.cache.get(tmpKey));
        if (p != null) return p;

        // not in cache, so lookup and store in cache
        RuntimeState s = null;
        int[] lastIndex = new int[] {-1};
        while ((s = getNextState(v.states, lastIndex, xstate)) != null) {
            if (s.foregroundPainter != null) {
                p = paintFilter(s.foregroundPainter);
                break;
            }
        }
        if (p == null) p = paintFilter((Painter)get(ctx, "foregroundPainter"));
        if (p != null) {
            v.cache.put(new CacheKey("foregroundPainter$$instance", xstate), p);
        }
        return p;
    }

    /**
     * Gets the appropriate border Painter, if there is one, for the state
     * specified in the given SynthContext. This method does appropriate
     * fallback searching, as described in #get.
     *
     * @param ctx The SynthContext. Must not be null.
     * @return The border painter associated for the given state, or null if
     * none could be found.
     */
    public Painter<Object> getBorderPainter(SynthContext ctx) {
        Values v = getValues(ctx);
        int xstate = getExtendedState(ctx, v);
        Painter<Object> p = null;

        // check the cache
        tmpKey.init("borderPainter$$instance", xstate);
        p = paintFilter((Painter)v.cache.get(tmpKey));
        if (p != null) return p;

        // not in cache, so lookup and store in cache
        RuntimeState s = null;
        int[] lastIndex = new int[] {-1};
        while ((s = getNextState(v.states, lastIndex, xstate)) != null) {
            if (s.borderPainter != null) {
                p = paintFilter(s.borderPainter);
                break;
            }
        }
        if (p == null) p = paintFilter((Painter)get(ctx, "borderPainter"));
        if (p != null) {
            v.cache.put(new CacheKey("borderPainter$$instance", xstate), p);
        }
        return p;
    }

    /**
     * Utility method which returns the proper Values based on the given
     * SynthContext. Ensures that parsing of the values has occurred, or
     * reoccurs as necessary.
     *
     * @param ctx The SynthContext
     * @return a non-null values reference
     */
    private Values getValues(SynthContext ctx) {
        validate();
        return values;
    }

    /**
     * Simple utility method that searches the given array of Strings for the
     * given string. This method is only called from getExtendedState if
     * the developer has specified a specific state for the component to be
     * in (ie, has "wedged" the component in that state) by specifying
     * they client property "Nimbus.State".
     *
     * @param names a non-null array of strings
     * @param name the name to look for in the array
     * @return true or false based on whether the given name is in the array
     */
    private boolean contains(String[] names, String name) {
        assert name != null;
        for (int i=0; i<names.length; i++) {
            if (name.equals(names[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Gets the extended state for a given synth context. Nimbus supports the
     * ability to define custom states. The algorithm used for choosing what
     * style information to use for a given state requires a single integer
     * bit string where each bit in the integer represents a different state
     * that the component is in. This method uses the componentState as
     * reported in the SynthContext, in addition to custom states, to determine
     * what this extended state is.</p>
     *
     * <p>In addition, this method checks the component in the given context
     * for a client property called "Nimbus.State". If one exists, then it will
     * decompose the String associated with that property to determine what
     * state to return. In this way, the developer can force a component to be
     * in a specific state, regardless of what the "real" state of the component
     * is.</p>
     *
     * <p>The string associated with "Nimbus.State" would be of the form:
     * <pre>Enabled+CustomState+MouseOver</pre></p>
     *
     * @param ctx
     * @param v
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private int getExtendedState(SynthContext ctx, Values v) {
        JComponent c = ctx.getComponent();
        int xstate = 0;
        int mask = 1;
        //check for the Nimbus.State client property
        //Performance NOTE: getClientProperty ends up inside a synchronized
        //block, so there is some potential for performance issues here, however
        //I'm not certain that there is one on a modern VM.
        Object property = c.getClientProperty("Nimbus.State");
        if (property != null) {
            String stateNames = property.toString();
            String[] states = stateNames.split("\\+");
            if (v.stateTypes == null){
                // standard states only
                for (String stateStr : states) {
                    State.StandardState s = State.getStandardState(stateStr);
                    if (s != null) xstate |= s.getState();
                }
            } else {
                // custom states
                for (State<?> s : v.stateTypes) {
                    if (contains(states, s.getName())) {
                        xstate |= mask;
                    }
                    mask <<= 1;
                }
            }
        } else {
            //if there are no custom states defined, then simply return the
            //state that Synth reported
            if (v.stateTypes == null) return ctx.getComponentState();

            //there are custom states on this values, so I'll have to iterate
            //over them all and return a custom extended state
            int state = ctx.getComponentState();
            for (State s : v.stateTypes) {
                if (s.isInState(c, state)) {
                    xstate |= mask;
                }
                mask <<= 1;
            }
        }
        return xstate;
    }

    /**
     * <p>Gets the RuntimeState that most closely matches the state in the given
     * context, but is less specific than the given "lastState". Essentially,
     * this allows you to search for the next best state.</p>
     *
     * <p>For example, if you had the following three states:
     * <pre>
     * Enabled
     * Enabled+Pressed
     * Disabled
     * </pre>
     * And you wanted to find the state that best represented
     * ENABLED+PRESSED+FOCUSED and <code>lastState</code> was null (or an
     * empty array, or an array with a single int with index == -1), then
     * Enabled+Pressed would be returned. If you then call this method again but
     * pass the index of Enabled+Pressed as the "lastState", then
     * Enabled would be returned. If you call this method a third time and pass
     * the index of Enabled in as the <code>lastState</code>, then null would be
     * returned.</p>
     *
     * <p>The actual code path for determining the proper state is the same as
     * in Synth.</p>
     *
     * @param lastState a 1 element array, allowing me to do pass-by-reference.
     */
    private RuntimeState getNextState(RuntimeState[] states,
                                      int[] lastState,
                                      int xstate) {
        // Use the StateInfo with the most bits that matches that of state.
        // If there are none, then fallback to
        // the StateInfo with a state of 0, indicating it'll match anything.

        // Consider if we have 3 StateInfos a, b and c with states:
        // SELECTED, SELECTED | ENABLED, 0
        //
        // Input                          Return Value
        // -----                          ------------
        // SELECTED                       a
        // SELECTED | ENABLED             b
        // MOUSE_OVER                     c
        // SELECTED | ENABLED | FOCUSED   b
        // ENABLED                        c

        if (states != null && states.length > 0) {
            int bestCount = 0;
            int bestIndex = -1;
            int wildIndex = -1;

            //if xstate is 0, then search for the runtime state with component
            //state of 0. That is, find the exact match and return it.
            if (xstate == 0) {
                for (int counter = states.length - 1; counter >= 0; counter--) {
                    if (states[counter].state == 0) {
                        lastState[0] = counter;
                        return states[counter];
                    }
                }
                //an exact match couldn't be found, so there was no match.
                lastState[0] = -1;
                return null;
            }

            //xstate is some value != 0

            //determine from which index to start looking. If lastState[0] is -1
            //then we know to start from the end of the state array. Otherwise,
            //we start at the lastIndex - 1.
            int lastStateIndex = lastState == null || lastState[0] == -1 ?
                states.length : lastState[0];

            for (int counter = lastStateIndex - 1; counter >= 0; counter--) {
                int oState = states[counter].state;

                if (oState == 0) {
                    if (wildIndex == -1) {
                        wildIndex = counter;
                    }
                } else if ((xstate & oState) == oState) {
                    // This is key, we need to make sure all bits of the
                    // StateInfo match, otherwise a StateInfo with
                    // SELECTED | ENABLED would match ENABLED, which we
                    // don't want.
                    int bitCount = Integer.bitCount(oState);
                    if (bitCount > bestCount) {
                        bestIndex = counter;
                        bestCount = bitCount;
                    }
                }
            }
            if (bestIndex != -1) {
                lastState[0] = bestIndex;
                return states[bestIndex];
            }
            if (wildIndex != -1) {
                lastState[0] = wildIndex;
                return states[wildIndex];
            }
        }
        lastState[0] = -1;
        return null;
    }

    /**
     * Contains values such as the UIDefaults and painters associated with
     * a state. Whereas <code>State</code> represents a distinct state that a
     * component can be in (such as Enabled), this class represents the colors,
     * fonts, painters, etc associated with some state for this
     * style.
     */
    private final class RuntimeState implements Cloneable {
        int state;
        Painter<Object> backgroundPainter;
        Painter<Object> foregroundPainter;
        Painter<Object> borderPainter;
        String stateName;
        UIDefaults defaults = new UIDefaults(10, .7f);

        private RuntimeState(int state, String stateName) {
            this.state = state;
            this.stateName = stateName;
        }

        @Override
        public String toString() {
            return stateName;
        }

        @Override
        public RuntimeState clone() {
            RuntimeState clone = new RuntimeState(state, stateName);
            clone.backgroundPainter = backgroundPainter;
            clone.foregroundPainter = foregroundPainter;
            clone.borderPainter = borderPainter;
            clone.defaults.putAll(defaults);
            return clone;
        }
    }

    /**
     * Essentially a struct of data for a style. A default instance of this
     * class is used by NimbusStyle. Additional instances exist for each
     * component that has overrides.
     */
    private static final class Values {
        /**
         * The list of State types. A State represents a type of state, such
         * as Enabled, Default, WindowFocused, etc. These can be custom states.
         */
        State<?>[] stateTypes = null;
        /**
         * The list of actual runtime state representations. These can represent things such
         * as Enabled + Focused. Thus, they differ from States in that they contain
         * several states together, and have associated properties, data, etc.
         */
        RuntimeState[] states = null;
        /**
         * The content margins for this region.
         */
        Insets contentMargins;
        /**
         * Defaults on the region/component level.
         */
        UIDefaults defaults = new UIDefaults(10, .7f);
        /**
         * Simple cache. After a value has been looked up, it is stored
         * in this cache for later retrieval. The key is a concatenation of
         * the property being looked up, two dollar signs, and the extended
         * state. So for example:
         *
         * foo.bar$$2353
         */
        Map<CacheKey,Object> cache = new HashMap<CacheKey,Object>();
    }

    /**
     * This implementation presupposes that key is never null and that
     * the two keys being checked for equality are never null
     */
    private static final class CacheKey {
        private String key;
        private int xstate;

        CacheKey(Object key, int xstate) {
            init(key, xstate);
        }

        void init(Object key, int xstate) {
            this.key = key.toString();
            this.xstate = xstate;
        }

        @Override
        public boolean equals(Object obj) {
            final CacheKey other = (CacheKey) obj;
            if (obj == null) return false;
            if (this.xstate != other.xstate) return false;
            if (!this.key.equals(other.key)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 29 * hash + this.key.hashCode();
            hash = 29 * hash + this.xstate;
            return hash;
        }
    }
}
