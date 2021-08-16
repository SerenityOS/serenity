/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy.operators;

import java.awt.Color;
import java.awt.Container;
import java.util.Hashtable;

import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.colorchooser.ColorSelectionModel;
import javax.swing.plaf.ColorChooserUI;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.ComponentSearcher;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;

/**
 *
 * Class provides methods to cover main JColorChooser component functionality.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class JColorChooserOperator extends JComponentOperator
        implements Outputable {

    /**
     * Identifier for a "color" property.
     *
     * @see #getDump
     */
    public static final String COLOR_DPROP = "Color";

    /**
     * Identifier for a "selected page" property.
     *
     * @see #getDump
     */
    public static final String SELECTED_PAGE_DPROP = "Selected page";

    private static final String RGB_TITLE = "RGB";
    private static final String HSV_TITLE = "HSV";
    private static final String HSL_TITLE = "HSL";
    private static final String CMYK_TITLE = "CMYK";

    private static final int HSV_HUE_INDEX = 0;
    private static final int HSV_SATURATION_INDEX = 1;
    private static final int HSV_VALUE_INDEX = 2;
    private static final int HSV_TRANSPARENCY_INDEX = 3;
    private static final int HSL_HUE_INDEX = 0;
    private static final int HSL_SATURATION_INDEX = 1;
    private static final int HSL_LIGHTNESS_INDEX = 2;
    private static final int HSL_TRANSPARENCY_INDEX = 3;
    private static final int RGB_RED_INDEX = 0;
    private static final int RGB_GREEN_INDEX = 1;
    private static final int RGB_BLUE_INDEX = 2;
    private static final int RGB_ALPHA_INDEX = 3;
    private static final int RGB_COLORCODE_TEXT_FIELD_INDEX = 4;
    private static final int CMYK_CYAN_INDEX = 0;
    private static final int CMYK_MAGENTA_INDEX = 1;
    private static final int CMYK_YELLOW_INDEX = 2;
    private static final int CMYK_BLACK_INDEX = 3;
    private static final int CMYK_ALPHA_INDEX = 4;

    private TestOut output;
    private JTabbedPaneOperator tabbed;
    private JTextFieldOperator red;
    private JTextFieldOperator green;
    private JTextFieldOperator blue;

    /**
     * Constructor.
     *
     * @param comp a component
     */
    public JColorChooserOperator(JColorChooser comp) {
        super(comp);
        setTimeouts(JemmyProperties.getProperties().getTimeouts());
        setOutput(JemmyProperties.getProperties().getOutput());
        tabbed = new JTabbedPaneOperator(this);
    }

    /**
     * Constructs a JColorChooserOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JColorChooserOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((JColorChooser) cont.
                waitSubComponent(new JColorChooserFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JColorChooserOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JColorChooserOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont Operator pointing a container to search component in.
     * @param index Ordinal component index.
     *
     */
    public JColorChooserOperator(ContainerOperator<?> cont, int index) {
        this((JColorChooser) waitComponent(cont,
                new JColorChooserFinder(),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont Operator pointing a container to search component in.
     *
     */
    public JColorChooserOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches JColorChooser in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JColorChooser instance or null if component was not found.
     */
    public static JColorChooser findJColorChooser(Container cont, ComponentChooser chooser, int index) {
        return (JColorChooser) findComponent(cont, new JColorChooserFinder(chooser), index);
    }

    /**
     * Searches 0'th JColorChooser in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JColorChooser instance or null if component was not found.
     */
    public static JColorChooser findJColorChooser(Container cont, ComponentChooser chooser) {
        return findJColorChooser(cont, chooser, 0);
    }

    /**
     * Searches JColorChooser in container.
     *
     * @param cont Container to search component in.
     * @param index Ordinal component index.
     * @return JColorChooser instance or null if component was not found.
     */
    public static JColorChooser findJColorChooser(Container cont, int index) {
        return findJColorChooser(cont, ComponentSearcher.getTrueChooser(Integer.toString(index) + "'th JColorChooser instance"), index);
    }

    /**
     * Searches 0'th JColorChooser in container.
     *
     * @param cont Container to search component in.
     * @return JColorChooser instance or null if component was not found.
     */
    public static JColorChooser findJColorChooser(Container cont) {
        return findJColorChooser(cont, 0);
    }

    /**
     * Waits JColorChooser in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JColorChooser instance or null if component was not displayed.
     *
     */
    public static JColorChooser waitJColorChooser(Container cont, ComponentChooser chooser, int index) {
        return (JColorChooser) waitComponent(cont, new JColorChooserFinder(chooser), index);
    }

    /**
     * Waits 0'th JColorChooser in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JColorChooser instance or null if component was not displayed.
     *
     */
    public static JColorChooser waitJColorChooser(Container cont, ComponentChooser chooser) {
        return waitJColorChooser(cont, chooser, 0);
    }

    /**
     * Waits JColorChooser in container.
     *
     * @param cont Container to search component in.
     * @param index Ordinal component index.
     * @return JColorChooser instance or null if component was not displayed.
     *
     */
    public static JColorChooser waitJColorChooser(Container cont, int index) {
        return waitJColorChooser(cont, ComponentSearcher.getTrueChooser(Integer.toString(index) + "'th JColorChooser instance"), index);
    }

    /**
     * Waits 0'th JColorChooser in container.
     *
     * @param cont Container to search component in.
     * @return JColorChooser instance or null if component was not displayed.
     *
     */
    public static JColorChooser waitJColorChooser(Container cont) {
        return waitJColorChooser(cont, 0);
    }

    @Override
    public void setOutput(TestOut out) {
        output = out;
        super.setOutput(output.createErrorOutput());
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    /**
     * Switches tab to "RGB" page.
     */
    public void switchToRGB() {
        if (!tabbed.getTitleAt(tabbed.getSelectedIndex()).
                equals(RGB_TITLE)) {
            tabbed.selectPage(RGB_TITLE);
        }
        blue = new JTextFieldOperator(this, 2);
        green = new JTextFieldOperator(this, 1);
        red = new JTextFieldOperator(this, 0);
    }

    /**
     * Enters red color component value. Switches to "RGB" page first.
     *
     * @param value red color component
     * @see #switchToRGB()
     * @see #enterColor(int, int, int)
     * @see #enterColor(java.awt.Color)
     * @see #enterColor(int)
     */
    public void enterRed(int value) {
        switchToRGB();
        red.setText(Integer.toString(value));
    }

    /**
     * Enters green color component value. Switches to "RGB" page first.
     *
     * @param value green color component
     * @see #switchToRGB()
     * @see #enterColor(int, int, int)
     * @see #enterColor(java.awt.Color)
     * @see #enterColor(int)
     */
    public void enterGreen(int value) {
        switchToRGB();
        green.setText(Integer.toString(value));
    }

    /**
     * Enters blue color component value. Switches to "RGB" page first.
     *
     * @param value blue color component
     * @see #switchToRGB()
     * @see #enterColor(int, int, int)
     * @see #enterColor(java.awt.Color)
     * @see #enterColor(int)
     */
    public void enterBlue(int value) {
        switchToRGB();
        blue.setText(Integer.toString(value));
    }

    /**
     * Enters all color components values. Switches to "RGB" page first.
     *
     * @param red red color component
     * @param green green color component
     * @param blue blue color component
     * @see #switchToRGB()
     * @see #enterColor(java.awt.Color)
     * @see #enterColor(int)
     */
    public void enterColor(int red, int green, int blue) {
        switchToRGB();
        enterRed(red);
        enterGreen(green);
        enterBlue(blue);
    }

    /**
     * Enters color. Switches to "RGB" page first.
     *
     * @param color a color
     * @see #switchToRGB()
     * @see #enterColor(int, int, int)
     * @see #enterColor(int)
     */
    public void enterColor(Color color) {
        enterColor(color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Enters color. Switches to "RGB" page first.
     *
     * @param color a color
     * @see #switchToRGB()
     * @see #enterColor(int, int, int)
     * @see #enterColor(java.awt.Color)
     */
    public void enterColor(int color) {
        enterColor(new Color(color));
    }

    /**
     * Returns information about component.
     */
    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        result.put(COLOR_DPROP, ((JColorChooser) getSource()).getColor().toString());
        JTabbedPane tb = (JTabbedPane) tabbed.getSource();
        result.put(SELECTED_PAGE_DPROP, tb.getTitleAt(tb.getSelectedIndex()));
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps
     * {@code JColorChooser.addChooserPanel(AbstractColorChooserPanel)}
     * through queue
     */
    public void addChooserPanel(final AbstractColorChooserPanel abstractColorChooserPanel) {
        runMapping(new MapVoidAction("addChooserPanel") {
            @Override
            public void map() {
                ((JColorChooser) getSource()).addChooserPanel(abstractColorChooserPanel);
            }
        });
    }

    /**
     * Maps {@code JColorChooser.getChooserPanels()} through queue
     */
    public AbstractColorChooserPanel[] getChooserPanels() {
        return ((AbstractColorChooserPanel[]) runMapping(new MapAction<Object>("getChooserPanels") {
            @Override
            public Object map() {
                return ((JColorChooser) getSource()).getChooserPanels();
            }
        }));
    }

    /**
     * Maps {@code JColorChooser.getColor()} through queue
     */
    public Color getColor() {
        return (runMapping(new MapAction<Color>("getColor") {
            @Override
            public Color map() {
                return ((JColorChooser) getSource()).getColor();
            }
        }));
    }

    /**
     * Maps {@code JColorChooser.getPreviewPanel()} through queue
     */
    public JComponent getPreviewPanel() {
        return (runMapping(new MapAction<JComponent>("getPreviewPanel") {
            @Override
            public JComponent map() {
                return ((JColorChooser) getSource()).getPreviewPanel();
            }
        }));
    }

    /**
     * Returns a spinner operator used for modifying Hue value if HSV or HSL tab
     * is already selected, else returns null.
     *
     * @return an operator for Hue <code>JSpinner<code> inside HSV or HSL
     *         tab
     */
    public JSpinnerOperator getHueSpinnerOperator() {
        return getSpinnerOperator(new String[]{HSV_TITLE, HSL_TITLE},
                new int[]{HSV_HUE_INDEX, HSL_HUE_INDEX});
    }

    /**
     * Returns a spinner operator used for modifying Saturation value if HSV or
     * HSL tab is already selected, else returns null.
     *
     * @return an operator for Saturation <code>JSpinner<code> inside HSV or HSL tab
     */
    public JSpinnerOperator getSaturationSpinnerOperator() {
        return getSpinnerOperator(new String[]{HSV_TITLE, HSL_TITLE},
                new int[]{HSV_SATURATION_INDEX, HSL_SATURATION_INDEX});
    }

    /**
     * Returns a spinner operator used for modifying value field if HSV tab is
     * already selected, else returns null.
     *
     * @return an operator for value <code>JSpinner<code> inside HSV tab
     */
    public JSpinnerOperator getValueSpinnerOperator() {
        return getSpinnerOperator(new String[]{HSV_TITLE}, new int[]{HSV_VALUE_INDEX});
    }

    /**
     * Returns a spinner operator used for modifying transparency value if HSV
     * or HSL tab is already selected, else returns null.
     *
     * @return an operator for transparency <code>JSpinner<code> inside HSV or HSL tab
     */
    public JSpinnerOperator getTransparencySpinnerOperator() {
        return getSpinnerOperator(new String[]{HSL_TITLE, HSV_TITLE},
                new int[]{HSL_TRANSPARENCY_INDEX, HSV_TRANSPARENCY_INDEX});
    }

    /**
     * Returns a spinner operator used for modifying Lightness value if HSL tab
     * is already selected, else returns null.
     *
     * @return an operator for Lightness <code>JSpinner<code> inside HSL tab
     */
    public JSpinnerOperator getLightnessSpinnerOperator() {
        return getSpinnerOperator(new String[]{HSL_TITLE}, new int[]{HSL_LIGHTNESS_INDEX});
    }

    /**
     * Returns a spinner operator used for modifying Red value if RGB tab is
     * already selected, else returns null.
     *
     * @return an operator for Red <code>JSpinner<code> inside RGB tab
     */
    public JSpinnerOperator getRedSpinnerOperator() {
        return getSpinnerOperator(new String[]{RGB_TITLE}, new int[]{RGB_RED_INDEX});
    }

    /**
     * Returns a spinner operator used for modifying Green value if RGB tab is
     * already selected, else returns null.
     *
     * @return an operator for Green <code>JSpinner<code> inside RGB tab
     */
    public JSpinnerOperator getGreenSpinnerOperator() {
        return getSpinnerOperator(new String[]{RGB_TITLE}, new int[]{RGB_GREEN_INDEX});
    }

    /**
     * Returns a spinner operator used for modifying Blue value if RGB tab is
     * already selected, else returns null.
     *
     * @return an operator for Blue <code>JSpinner<code> inside RGB tab
     */
    public JSpinnerOperator getBlueSpinnerOperator() {
        return getSpinnerOperator(new String[]{RGB_TITLE}, new int[]{RGB_BLUE_INDEX});
    }

    /**
     * Returns a spinner operator used for modifying Alpha value if RGB or CMYK
     * tab is already selected, else returns null.
     *
     * @return an operator for Alpha <code>JSpinner<code> inside RGB/CMYK
     *         tab
     */
    public JSpinnerOperator getAlphaSpinnerOperator() {
        return getSpinnerOperator(new String[]{RGB_TITLE, CMYK_TITLE},
                new int[]{RGB_ALPHA_INDEX, CMYK_ALPHA_INDEX});
    }

    /**
     * Returns a spinner operator used for modifying Cyan value if CMYK tab is
     * already selected, else returns null.
     *
     * @return an operator for Cyan <code>JSpinner<code> inside CMYK tab
     */
    public JSpinnerOperator getCyanSpinnerOperator() {
        return getSpinnerOperator(new String[]{CMYK_TITLE}, new int[]{CMYK_CYAN_INDEX});
    }

    /**
     * Returns a spinner operator used for modifying Magenta value if CMYK tab
     * is already selected, else returns null.
     *
     * @return an operator for Magenta <code>JSpinner<code> inside CMYK tab
     */
    public JSpinnerOperator getMagentaSpinnerOperator() {
        return getSpinnerOperator(new String[]{CMYK_TITLE}, new int[]{CMYK_MAGENTA_INDEX});
    }

    /**
     * Returns a spinner operator used for modifying Yellow value if CMYK tab is
     * already selected, else returns null.
     *
     * @return an operator for Yellow <code>JSpinner<code> inside CMYK tab
     */
    public JSpinnerOperator getYellowSpinnerOperator() {
        return getSpinnerOperator(new String[]{CMYK_TITLE}, new int[]{CMYK_YELLOW_INDEX});
    }

    /**
     * Returns a spinner operator used for modifying Black value if CMYK tab is
     * already selected, else returns null.
     *
     * @return an operator for Black value <code>JSpinner<code> inside CMYK tab
     */
    public JSpinnerOperator getBlackSpinnerOperator() {
        return getSpinnerOperator(new String[]{CMYK_TITLE}, new int[]{CMYK_BLACK_INDEX});
    }

    /**
     * Returns a slider operator used for modifying Hue value if HSV or HSL tab
     * is already selected, else returns null.
     *
     * @return an operator for Hue <code>JSlider<code> inside HSV or HSL
     *         tab
     */
    public JSliderOperator getHueSliderOperator() {
        return getSliderOperator(new String[]{HSV_TITLE, HSL_TITLE},
                new int[]{HSV_HUE_INDEX, HSL_HUE_INDEX});
    }

    /**
     * Returns a slider operator used for modifying Saturation value if HSV or
     * HSL tab is already selected, else returns null.
     *
     * @return an operator for Saturation <code>JSlider<code> inside HSV or HSL tab
     */
    public JSliderOperator getSaturationSliderOperator() {
        return getSliderOperator(new String[]{HSV_TITLE, HSL_TITLE},
                new int[]{HSV_SATURATION_INDEX, HSL_SATURATION_INDEX});
    }

    /**
     * Returns a slider operator used for modifying value field if HSV tab is
     * already selected, else returns null.
     *
     * @return an operator for value <code>JSlider<code> inside HSV tab
     */
    public JSliderOperator getValueSliderOperator() {
        return getSliderOperator(new String[]{HSV_TITLE}, new int[]{HSV_VALUE_INDEX});
    }

    /**
     * Returns a slider operator used for modifying transparency value if HSV or
     * HSL tab is already selected, else returns null.
     *
     * @return an operator for transparency <code>JSlider<code> inside HSV or HSL tab
     */
    public JSliderOperator getTransparencySliderOperator() {
        return getSliderOperator(new String[]{HSV_TITLE, HSL_TITLE},
                new int[]{HSV_TRANSPARENCY_INDEX, HSL_TRANSPARENCY_INDEX});
    }

    /**
     * Returns a slider operator used for modifying Lightness value if HSL tab
     * is already selected, else returns null.
     *
     * @return an operator for Lightness <code>JSlider<code> inside HSL tab
     */
    public JSliderOperator getLightnessSliderOperator() {
        return getSliderOperator(new String[]{HSL_TITLE}, new int[]{HSL_LIGHTNESS_INDEX});
    }

    /**
     * Returns a slider operator used for modifying Red value if RGB tab is
     * already selected, else returns null.
     *
     * @return an operator for Red <code>JSlider<code> inside RGB tab
     */
    public JSliderOperator getRedSliderOperator() {
        return getSliderOperator(new String[]{RGB_TITLE}, new int[]{RGB_RED_INDEX});
    }

    /**
     * Returns a slider operator used for modifying Green value if RGB tab is
     * already selected, else returns null.
     *
     * @return an operator for Green <code>JSlider<code> inside RGB tab
     */
    public JSliderOperator getGreenSliderOperator() {
        return getSliderOperator(new String[]{RGB_TITLE}, new int[]{RGB_GREEN_INDEX});
    }

    /**
     * Returns a slider operator used for modifying Blue value if RGB tab is
     * already selected, else returns null.
     *
     * @return an operator for Blue <code>JSlider<code> inside RGB tab
     */
    public JSliderOperator getBlueSliderOperator() {
        return getSliderOperator(new String[]{RGB_TITLE}, new int[]{RGB_BLUE_INDEX});
    }

    /**
     * Returns a slider operator used for modifying Alpha value if RGB or CMYK
     * tab is already selected, else returns null.
     *
     * @return an operator for Alpha <code>JSlider<code> inside RGB/CMYK
     *         tab
     */
    public JSliderOperator getAlphaSliderOperator() {
        return getSliderOperator(new String[]{RGB_TITLE, CMYK_TITLE},
                new int[]{RGB_ALPHA_INDEX, CMYK_ALPHA_INDEX});
    }

    /**
     * Returns a slider operator used for modifying Cyan value if CMYK tab is
     * already selected, else returns null.
     *
     * @return an operator for Cyan <code>JSlider<code> inside CMYK tab
     */
    public JSliderOperator getCyanSliderOperator() {
        return getSliderOperator(new String[]{CMYK_TITLE}, new int[]{CMYK_CYAN_INDEX});
    }

    /**
     * Returns a slider operator used for modifying Magenta value if CMYK tab is
     * already selected, else returns null.
     *
     * @return an operator for Magenta <code>JSlider<code> inside CMYK tab
     */
    public JSliderOperator getMagentaSliderOperator() {
        return getSliderOperator(new String[]{CMYK_TITLE}, new int[]{CMYK_MAGENTA_INDEX});
    }

    /**
     * Returns a slider operator used for modifying Yellow value if CMYK tab is
     * already selected, else returns null.
     *
     * @return an operator for Yellow <code>JSlider<code> inside CMYK tab
     */
    public JSliderOperator getYellowSliderOperator() {
        return getSliderOperator(new String[]{CMYK_TITLE}, new int[]{CMYK_YELLOW_INDEX});
    }

    /**
     * Returns a slider operator used for modifying Black value if CMYK tab is
     * already selected, else returns null.
     *
     * @return an operator for Black <code>JSlider<code> inside CMYK tab
     */
    public JSliderOperator getBlackSliderOperator() {
        return getSliderOperator(new String[]{CMYK_TITLE}, new int[]{CMYK_BLACK_INDEX});
    }

    /**
     * Returns a textField operator used for modifying Color Code value if RGB
     * tab is already selected, else returns null.
     *
     * @return an operator for Color Code value <code>JTextField<code> inside RGB tab
     */
    public JTextFieldOperator getColorCodeTextFieldOperator() {
        JTextFieldOperator colorCode = null;
        if (tabbed.getTitleAt(tabbed.getSelectedIndex()).equals(RGB_TITLE)) {
            colorCode = new JTextFieldOperator(this, RGB_COLORCODE_TEXT_FIELD_INDEX);
        }
        return colorCode;
    }

    /**
     * Maps {@code JColorChooser.getSelectionModel()} through queue
     */
    public ColorSelectionModel getSelectionModel() {
        return (runMapping(new MapAction<ColorSelectionModel>("getSelectionModel") {
            @Override
            public ColorSelectionModel map() {
                return ((JColorChooser) getSource()).getSelectionModel();
            }
        }));
    }

    /**
     * Maps {@code JColorChooser.getUI()} through queue
     */
    public ColorChooserUI getUI() {
        return (runMapping(new MapAction<ColorChooserUI>("getUI") {
            @Override
            public ColorChooserUI map() {
                return ((JColorChooser) getSource()).getUI();
            }
        }));
    }

    /**
     * Maps
     * {@code JColorChooser.removeChooserPanel(AbstractColorChooserPanel)}
     * through queue
     */
    public AbstractColorChooserPanel removeChooserPanel(final AbstractColorChooserPanel abstractColorChooserPanel) {
        return (runMapping(new MapAction<AbstractColorChooserPanel>("removeChooserPanel") {
            @Override
            public AbstractColorChooserPanel map() {
                return ((JColorChooser) getSource()).removeChooserPanel(abstractColorChooserPanel);
            }
        }));
    }

    /**
     * Maps
     * {@code JColorChooser.setChooserPanels(AbstractColorChooserPanel[])}
     * through queue
     */
    public void setChooserPanels(final AbstractColorChooserPanel[] abstractColorChooserPanel) {
        runMapping(new MapVoidAction("setChooserPanels") {
            @Override
            public void map() {
                ((JColorChooser) getSource()).setChooserPanels(abstractColorChooserPanel);
            }
        });
    }

    /**
     * Maps {@code JColorChooser.setColor(int)} through queue
     */
    public void setColor(final int i) {
        runMapping(new MapVoidAction("setColor") {
            @Override
            public void map() {
                ((JColorChooser) getSource()).setColor(i);
            }
        });
    }

    /**
     * Maps {@code JColorChooser.setColor(int, int, int)} through queue
     */
    public void setColor(final int i, final int i1, final int i2) {
        runMapping(new MapVoidAction("setColor") {
            @Override
            public void map() {
                ((JColorChooser) getSource()).setColor(i, i1, i2);
            }
        });
    }

    /**
     * Maps {@code JColorChooser.setColor(Color)} through queue
     */
    public void setColor(final Color color) {
        runMapping(new MapVoidAction("setColor") {
            @Override
            public void map() {
                ((JColorChooser) getSource()).setColor(color);
            }
        });
    }

    /**
     * Maps {@code JColorChooser.setPreviewPanel(JComponent)} through queue
     */
    public void setPreviewPanel(final JComponent jComponent) {
        runMapping(new MapVoidAction("setPreviewPanel") {
            @Override
            public void map() {
                ((JColorChooser) getSource()).setPreviewPanel(jComponent);
            }
        });
    }

    /**
     * Maps {@code JColorChooser.setSelectionModel(ColorSelectionModel)}
     * through queue
     */
    public void setSelectionModel(final ColorSelectionModel colorSelectionModel) {
        runMapping(new MapVoidAction("setSelectionModel") {
            @Override
            public void map() {
                ((JColorChooser) getSource()).setSelectionModel(colorSelectionModel);
            }
        });
    }

    /**
     * Maps {@code JColorChooser.setUI(ColorChooserUI)} through queue
     */
    public void setUI(final ColorChooserUI colorChooserUI) {
        runMapping(new MapVoidAction("setUI") {
            @Override
            public void map() {
                ((JColorChooser) getSource()).setUI(colorChooserUI);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Checks component type.
     */
    public static class JColorChooserFinder extends Finder {

        /**
         * Constructs JColorChooserFinder.
         *
         * @param sf other searching criteria.
         */
        public JColorChooserFinder(ComponentChooser sf) {
            super(JColorChooser.class, sf);
        }

        /**
         * Constructs JColorChooserFinder.
         */
        public JColorChooserFinder() {
            super(JColorChooser.class);
        }
    }

    private JSliderOperator getSliderOperator(String[] tabs, int[] index) {
        int selectedTabIndex = getSelectedTabIndex(tabs);
        if (selectedTabIndex != -1) {
            return new JSliderOperator(this, index[selectedTabIndex]);
        } else {
            return null;
        }
    }

    private JSpinnerOperator getSpinnerOperator(String[] tabs, int[] index) {
        int selectedTabIndex = getSelectedTabIndex(tabs);
        if (selectedTabIndex != -1) {
            return new JSpinnerOperator(this, index[selectedTabIndex]);
        } else {
            return null;
        }
    }

    private int getSelectedTabIndex(String tabs[]) {
        for (int i = 0; i < tabs.length; i++) {
            if (tabbed.getTitleAt(tabbed.getSelectedIndex()).equals(tabs[i])) {
                return i;
            }
        }
        return -1;
    }
}
