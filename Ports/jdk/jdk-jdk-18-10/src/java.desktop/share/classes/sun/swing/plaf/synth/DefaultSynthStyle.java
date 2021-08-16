/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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
package sun.swing.plaf.synth;

import javax.swing.plaf.synth.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.plaf.*;

/**
 * Default implementation of SynthStyle. Has setters for the various
 * SynthStyle methods. Many of the properties can be specified for all states,
 * using SynthStyle directly, or a specific state using one of the StateInfo
 * methods.
 * <p>
 * Beyond the constructor a subclass should override the <code>addTo</code>
 * and <code>clone</code> methods, these are used when the Styles are being
 * merged into a resulting style.
 *
 * @author Scott Violet
 */
public class DefaultSynthStyle extends SynthStyle implements Cloneable {

    private static final Object PENDING = new Object();

    /**
     * Should the component be opaque?
     */
    private boolean opaque;
    /**
     * Insets.
     */
    private Insets insets;
    /**
     * Information specific to ComponentState.
     */
    private StateInfo[] states;
    /**
     * User specific data.
     */
    private Map<Object, Object> data;

    /**
     * Font to use if there is no matching StateInfo, or the StateInfo doesn't
     * define one.
     */
    private Font font;

    /**
     * SynthGraphics, may be null.
     */
    private SynthGraphicsUtils synthGraphics;

    /**
     * Painter to use if the StateInfo doesn't have one.
     */
    private SynthPainter painter;


    /**
     * Nullary constructor, intended for subclassers.
     */
    public DefaultSynthStyle() {
    }

    /**
     * Creates a new DefaultSynthStyle that is a copy of the passed in
     * style. Any StateInfo's of the passed in style are clonsed as well.
     *
     * @param style Style to duplicate
     */
    public DefaultSynthStyle(DefaultSynthStyle style) {
        opaque = style.opaque;
        if (style.insets != null) {
            insets = new Insets(style.insets.top, style.insets.left,
                                style.insets.bottom, style.insets.right);
        }
        if (style.states != null) {
            states = new StateInfo[style.states.length];
            for (int counter = style.states.length - 1; counter >= 0;
                     counter--) {
                states[counter] = (StateInfo)style.states[counter].clone();
            }
        }
        if (style.data != null) {
            data = new HashMap<>();
            data.putAll(style.data);
        }
        font = style.font;
        synthGraphics = style.synthGraphics;
        painter = style.painter;
    }

    /**
     * Creates a new DefaultSynthStyle.
     *
     * @param insets Insets for the Style
     * @param opaque Whether or not the background is completely painted in
     *        an opaque color
     * @param states StateInfos describing properties per state
     * @param data Style specific data.
     */
    public DefaultSynthStyle(Insets insets, boolean opaque,
                             StateInfo[] states, Map<Object, Object> data) {
        this.insets = insets;
        this.opaque = opaque;
        this.states = states;
        this.data = data;
    }

    public Color getColor(SynthContext context, ColorType type) {
        return getColor(context.getComponent(), context.getRegion(),
                        context.getComponentState(), type);
    }

    public Color getColor(JComponent c, Region id, int state,
                          ColorType type) {
        // For the enabled state, prefer the widget's colors
        if (!id.isSubregion() && state == SynthConstants.ENABLED) {
            if (type == ColorType.BACKGROUND) {
                return c.getBackground();
            }
            else if (type == ColorType.FOREGROUND) {
                return c.getForeground();
            }
            else if (type == ColorType.TEXT_FOREGROUND) {
                // If getForeground returns a non-UIResource it means the
                // developer has explicitly set the foreground, use it over
                // that of TEXT_FOREGROUND as that is typically the expected
                // behavior.
                Color color = c.getForeground();
                if (!(color instanceof UIResource)) {
                    return color;
                }
            }
        }
        // Then use what we've locally defined
        Color color = getColorForState(c, id, state, type);
        if (color == null) {
            // No color, fallback to that of the widget.
            if (type == ColorType.BACKGROUND ||
                        type == ColorType.TEXT_BACKGROUND) {
                return c.getBackground();
            }
            else if (type == ColorType.FOREGROUND ||
                     type == ColorType.TEXT_FOREGROUND) {
                return c.getForeground();
            }
        }
        return color;
    }

    protected Color getColorForState(SynthContext context, ColorType type) {
        return getColorForState(context.getComponent(), context.getRegion(),
                                context.getComponentState(), type);
    }

    /**
     * Returns the color for the specified state.
     *
     * @param c JComponent the style is associated with
     * @param id Region identifier
     * @param state State of the region.
     * @param type Type of color being requested.
     * @return Color to render with
     */
    protected Color getColorForState(JComponent c, Region id, int state,
                                     ColorType type) {
        // Use the best state.
        StateInfo si = getStateInfo(state);
        Color color;
        if (si != null && (color = si.getColor(type)) != null) {
            return color;
        }
        if (si == null || si.getComponentState() != 0) {
            si = getStateInfo(0);
            if (si != null) {
                return si.getColor(type);
            }
        }
        return null;
    }

    /**
     * Sets the font that is used if there is no matching StateInfo, or
     * it does not define a font.
     *
     * @param font Font to use for rendering
     */
    public void setFont(Font font) {
        this.font = font;
    }

    public Font getFont(SynthContext state) {
        return getFont(state.getComponent(), state.getRegion(),
                       state.getComponentState());
    }

    public Font getFont(JComponent c, Region id, int state) {
        if (!id.isSubregion() && state == SynthConstants.ENABLED) {
            return c.getFont();
        }
        Font cFont = c.getFont();
        if (cFont != null && !(cFont instanceof UIResource)) {
            return cFont;
        }
        return getFontForState(c, id, state);
    }

    /**
     * Returns the font for the specified state. This should NOT callback
     * to the JComponent.
     *
     * @param c JComponent the style is associated with
     * @param id Region identifier
     * @param state State of the region.
     * @return Font to render with
     */
    protected Font getFontForState(JComponent c, Region id, int state) {
        if (c == null) {
            return this.font;
        }
        // First pass, look for the best match
        StateInfo si = getStateInfo(state);
        Font font;
        if (si != null && (font = si.getFont()) != null) {
            return font;
        }
        if (si == null || si.getComponentState() != 0) {
            si = getStateInfo(0);
            if (si != null && (font = si.getFont()) != null) {
                return font;
            }
        }
        // Fallback font.
        return this.font;
    }

    protected Font getFontForState(SynthContext context) {
        return getFontForState(context.getComponent(), context.getRegion(),
                               context.getComponentState());
    }

    /**
     * Sets the SynthGraphicsUtils that will be used for rendering.
     *
     * @param graphics SynthGraphics
     */
    public void setGraphicsUtils(SynthGraphicsUtils graphics) {
        this.synthGraphics = graphics;
    }

    /**
     * Returns a SynthGraphicsUtils.
     *
     * @param context SynthContext identifying requestor
     * @return SynthGraphicsUtils
     */
    public SynthGraphicsUtils getGraphicsUtils(SynthContext context) {
        if (synthGraphics == null) {
            return super.getGraphicsUtils(context);
        }
        return synthGraphics;
    }

    /**
     * Sets the insets.
     *
     * @param insets the new insets.
     */
    public void setInsets(Insets insets) {
        this.insets = insets;
    }

    /**
     * Returns the Insets. If <code>to</code> is non-null the resulting
     * insets will be placed in it, otherwise a new Insets object will be
     * created and returned.
     *
     * @param state SynthContext identifying requestor
     * @param to Where to place Insets
     * @return Insets.
     */
    public Insets getInsets(SynthContext state, Insets to) {
        if (to == null) {
            to = new Insets(0, 0, 0, 0);
        }
        if (insets != null) {
            to.left = insets.left;
            to.right = insets.right;
            to.top = insets.top;
            to.bottom = insets.bottom;
        }
        else {
            to.left = to.right = to.top = to.bottom = 0;
        }
        return to;
    }

    /**
     * Sets the Painter to use for the border.
     *
     * @param painter Painter for the Border.
     */
    public void setPainter(SynthPainter painter) {
        this.painter = painter;
    }

    /**
     * Returns the Painter for the passed in Component. This may return null.
     *
     * @param ss SynthContext identifying requestor
     * @return Painter for the border
     */
    public SynthPainter getPainter(SynthContext ss) {
        return painter;
    }

    /**
     * Sets whether or not the JComponent should be opaque.
     *
     * @param opaque Whether or not the JComponent should be opaque.
     */
    public void setOpaque(boolean opaque) {
        this.opaque = opaque;
    }

    /**
     * Returns the value to initialize the opacity property of the Component
     * to. A Style should NOT assume the opacity will remain this value, the
     * developer may reset it or override it.
     *
     * @param ss SynthContext identifying requestor
     * @return opaque Whether or not the JComponent is opaque.
     */
    public boolean isOpaque(SynthContext ss) {
        return opaque;
    }

    /**
     * Sets style specific values. This does NOT copy the data, it
     * assigns it directly to this Style.
     *
     * @param data Style specific values
     */
    public void setData(Map<Object, Object> data) {
        this.data = data;
    }

    /**
     * Returns the style specific data.
     *
     * @return Style specific data.
     */
    public Map<Object, Object> getData() {
        return data;
    }

    /**
     * Getter for a region specific style property.
     *
     * @param state SynthContext identifying requestor
     * @param key Property being requested.
     * @return Value of the named property
     */
    public Object get(SynthContext state, Object key) {
        // Look for the best match
        StateInfo si = getStateInfo(state.getComponentState());
        if (si != null && si.getData() != null && getKeyFromData(si.getData(), key) != null) {
            return getKeyFromData(si.getData(), key);
        }
        si = getStateInfo(0);
        if (si != null && si.getData() != null && getKeyFromData(si.getData(), key) != null) {
            return getKeyFromData(si.getData(), key);
        }
        if(getKeyFromData(data, key) != null)
          return getKeyFromData(data, key);
        return getDefaultValue(state, key);
    }


    private Object getKeyFromData(Map<Object, Object> stateData, Object key) {
          Object value = null;
          if (stateData != null) {

            synchronized(stateData) {
                value = stateData.get(key);
            }
            while (value == PENDING) {
                synchronized(stateData) {
                    try {
                        stateData.wait();
                    } catch (InterruptedException ie) {}
                    value = stateData.get(key);
                }
            }
            if (value instanceof UIDefaults.LazyValue) {
                synchronized(stateData) {
                    stateData.put(key, PENDING);
                }
                value = ((UIDefaults.LazyValue)value).createValue(null);
                synchronized(stateData) {
                    stateData.put(key, value);
                    stateData.notifyAll();
                }
            }
        }
        return value;
    }

    /**
     * Returns the default value for a particular property.  This is only
     * invoked if this style doesn't define a property for <code>key</code>.
     *
     * @param context SynthContext identifying requestor
     * @param key Property being requested.
     * @return Value of the named property
     */
    public Object getDefaultValue(SynthContext context, Object key) {
        return super.get(context, key);
    }

    /**
     * Creates a clone of this style.
     *
     * @return Clone of this style
     */
    public Object clone() {
        DefaultSynthStyle style;
        try {
            style = (DefaultSynthStyle)super.clone();
        } catch (CloneNotSupportedException cnse) {
            return null;
        }
        if (states != null) {
            style.states = new StateInfo[states.length];
            for (int counter = states.length - 1; counter >= 0; counter--) {
                style.states[counter] = (StateInfo)states[counter].clone();
            }
        }
        if (data != null) {
            style.data = new HashMap<>();
            style.data.putAll(data);
        }
        return style;
    }

    /**
     * Merges the contents of this Style with that of the passed in Style,
     * returning the resulting merged syle. Properties of this
     * <code>DefaultSynthStyle</code> will take precedence over those of the
     * passed in <code>DefaultSynthStyle</code>. For example, if this
     * style specifics a non-null font, the returned style will have its
     * font so to that regardless of the <code>style</code>'s font.
     *
     * @param style Style to add our styles to
     * @return Merged style.
     */
    public DefaultSynthStyle addTo(DefaultSynthStyle style) {
        if (insets != null) {
            style.insets = this.insets;
        }
        if (font != null) {
            style.font = this.font;
        }
        if (painter != null) {
            style.painter = this.painter;
        }
        if (synthGraphics != null) {
            style.synthGraphics = this.synthGraphics;
        }
        style.opaque = opaque;
        if (states != null) {
            if (style.states == null) {
                style.states = new StateInfo[states.length];
                for (int counter = states.length - 1; counter >= 0; counter--){
                    if (states[counter] != null) {
                        style.states[counter] = (StateInfo)states[counter].
                                                clone();
                    }
                }
            }
            else {
                // Find the number of new states in unique, merging any
                // matching states as we go. Also, move any merge styles
                // to the end to give them precedence.
                int unique = 0;
                // Number of StateInfos that match.
                int matchCount = 0;
                int maxOStyles = style.states.length;
                for (int thisCounter = states.length - 1; thisCounter >= 0;
                         thisCounter--) {
                    int state = states[thisCounter].getComponentState();
                    boolean found = false;

                    for (int oCounter = maxOStyles - 1 - matchCount;
                             oCounter >= 0; oCounter--) {
                        if (state == style.states[oCounter].
                                           getComponentState()) {
                            style.states[oCounter] = states[thisCounter].
                                        addTo(style.states[oCounter]);
                            // Move StateInfo to end, giving it precedence.
                            StateInfo tmp = style.states[maxOStyles - 1 -
                                                         matchCount];
                            style.states[maxOStyles - 1 - matchCount] =
                                  style.states[oCounter];
                            style.states[oCounter] = tmp;
                            matchCount++;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        unique++;
                    }
                }
                if (unique != 0) {
                    // There are states that exist in this Style that
                    // don't exist in the other style, recreate the array
                    // and add them.
                    StateInfo[] newStates = new StateInfo[
                                   unique + maxOStyles];
                    int newIndex = maxOStyles;

                    System.arraycopy(style.states, 0, newStates, 0,maxOStyles);
                    for (int thisCounter = states.length - 1; thisCounter >= 0;
                             thisCounter--) {
                        int state = states[thisCounter].getComponentState();
                        boolean found = false;

                        for (int oCounter = maxOStyles - 1; oCounter >= 0;
                                 oCounter--) {
                            if (state == style.states[oCounter].
                                               getComponentState()) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            newStates[newIndex++] = (StateInfo)states[
                                      thisCounter].clone();
                        }
                    }
                    style.states = newStates;
                }
            }
        }
        if (data != null) {
            if (style.data == null) {
                style.data = new HashMap<>();
            }
            style.data.putAll(data);
        }
        return style;
    }

    /**
     * Sets the array of StateInfo's which are used to specify properties
     * specific to a particular style.
     *
     * @param states StateInfos
     */
    public void setStateInfo(StateInfo[] states) {
        this.states = states;
    }

    /**
     * Returns the array of StateInfo's that that are used to specify
     * properties specific to a particular style.
     *
     * @return Array of StateInfos.
     */
    public StateInfo[] getStateInfo() {
        return states;
    }

    /**
     * Returns the best matching StateInfo for a particular state.
     *
     * @param state Component state.
     * @return Best matching StateInfo, or null
     */
    public StateInfo getStateInfo(int state) {
        // Use the StateInfo with the most bits that matches that of state.
        // If there is none, than fallback to
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

        if (states != null) {
            int bestCount = 0;
            int bestIndex = -1;
            int wildIndex = -1;

            if (state == 0) {
                for (int counter = states.length - 1; counter >= 0;counter--) {
                    if (states[counter].getComponentState() == 0) {
                        return states[counter];
                    }
                }
                return null;
            }
            for (int counter = states.length - 1; counter >= 0; counter--) {
                int oState = states[counter].getComponentState();

                if (oState == 0) {
                    if (wildIndex == -1) {
                        wildIndex = counter;
                    }
                }
                else if ((state & oState) == oState) {
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
                return states[bestIndex];
            }
            if (wildIndex != -1) {
                return states[wildIndex];
            }
          }
          return null;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(super.toString()).append(',');

        sb.append("data=").append(data).append(',');

        sb.append("font=").append(font).append(',');

        sb.append("insets=").append(insets).append(',');

        sb.append("synthGraphics=").append(synthGraphics).append(',');

        sb.append("painter=").append(painter).append(',');

        StateInfo[] states = getStateInfo();
        if (states != null) {
            sb.append("states[");
            for (StateInfo state : states) {
                sb.append(state.toString()).append(',');
            }
            sb.append(']').append(',');
        }

        // remove last newline
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }


    /**
     * StateInfo represents Style information specific to the state of
     * a component.
     */
    public static class StateInfo {
        private Map<Object, Object> data;
        private Font font;
        private Color[] colors;
        private int state;

        /**
         * Creates a new StateInfo.
         */
        public StateInfo() {
        }

        /**
         * Creates a new StateInfo with the specified properties
         *
         * @param state Component state(s) that this StateInfo should be used
         * for
         * @param font Font for this state
         * @param colors Colors for this state
         */
        public StateInfo(int state, Font font, Color[] colors) {
            this.state = state;
            this.font = font;
            this.colors = colors;
        }

        /**
         * Creates a new StateInfo that is a copy of the passed in
         * StateInfo.
         *
         * @param info StateInfo to copy.
         */
        public StateInfo(StateInfo info) {
            this.state = info.state;
            this.font = info.font;
            if(info.data != null) {
               if(data == null) {
                  data = new HashMap<>();
               }
               data.putAll(info.data);
            }
            if (info.colors != null) {
                this.colors = new Color[info.colors.length];
                System.arraycopy(info.colors, 0, colors, 0,info.colors.length);
            }
        }

        public Map<Object, Object> getData() {
            return data;
        }

        public void setData(Map<Object, Object> data) {
            this.data = data;
        }

        /**
         * Sets the font for this state.
         *
         * @param font Font to use for rendering
         */
        public void setFont(Font font) {
            this.font = font;
        }

        /**
         * Returns the font for this state.
         *
         * @return Returns the font to use for rendering this state
         */
        public Font getFont() {
            return font;
        }

        /**
         * Sets the array of colors to use for rendering this state. This
         * is indexed by <code>ColorType.getID()</code>.
         *
         * @param colors Array of colors
         */
        public void setColors(Color[] colors) {
            this.colors = colors;
        }

        /**
         * Returns the array of colors to use for rendering this state. This
         * is indexed by <code>ColorType.getID()</code>.
         *
         * @return Array of colors
         */
        public Color[] getColors() {
            return colors;
        }

        /**
         * Returns the Color to used for the specified ColorType.
         *
         * @return Color.
         */
        public Color getColor(ColorType type) {
            if (colors != null) {
                int id = type.getID();

                if (id < colors.length) {
                    return colors[id];
                }
            }
            return null;
        }

        /**
         * Merges the contents of this StateInfo with that of the passed in
         * StateInfo, returning the resulting merged StateInfo. Properties of
         * this <code>StateInfo</code> will take precedence over those of the
         * passed in <code>StateInfo</code>. For example, if this
         * StateInfo specifics a non-null font, the returned StateInfo will
         * have its font so to that regardless of the <code>StateInfo</code>'s
         * font.
         *
         * @param info StateInfo to add our styles to
         * @return Merged StateInfo.
         */
        public StateInfo addTo(StateInfo info) {
            if (font != null) {
                info.font = font;
            }
            if(data != null) {
                if(info.data == null) {
                    info.data = new HashMap<>();
                }
                info.data.putAll(data);
            }
            if (colors != null) {
                if (info.colors == null) {
                    info.colors = new Color[colors.length];
                    System.arraycopy(colors, 0, info.colors, 0,
                                     colors.length);
                }
                else {
                    if (info.colors.length < colors.length) {
                        Color[] old = info.colors;

                        info.colors = new Color[colors.length];
                        System.arraycopy(old, 0, info.colors, 0, old.length);
                    }
                    for (int counter = colors.length - 1; counter >= 0;
                             counter--) {
                        if (colors[counter] != null) {
                            info.colors[counter] = colors[counter];
                        }
                    }
                }
            }
            return info;
        }

        /**
         * Sets the state this StateInfo corresponds to.
         *
         * @see SynthConstants
         * @param state info.
         */
        public void setComponentState(int state) {
            this.state = state;
        }

        /**
         * Returns the state this StateInfo corresponds to.
         *
         * @see SynthConstants
         * @return state info.
         */
        public int getComponentState() {
            return state;
        }

        /**
         * Creates and returns a copy of this StateInfo.
         *
         * @return Copy of this StateInfo.
         */
        public Object clone() {
            return new StateInfo(this);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append(super.toString()).append(',');

            sb.append("state=").append(Integer.toString(state)).append(',');

            sb.append("font=").append(font).append(',');

            if (colors != null) {
                sb.append("colors=").append(Arrays.asList(colors)).
                    append(',');
            }
            return sb.toString();
        }
    }
}
