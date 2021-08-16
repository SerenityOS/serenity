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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.UIResource;

/**
 * A component that lets the user graphically select a value by sliding
 * a knob within a bounded interval. The knob is always positioned
 * at the points that match integer values within the specified interval.
 * <p>
 * The slider can show both
 * major tick marks, and minor tick marks between the major ones.  The number of
 * values between the tick marks is controlled with
 * <code>setMajorTickSpacing</code> and <code>setMinorTickSpacing</code>.
 * Painting of tick marks is controlled by {@code setPaintTicks}.
 * <p>
 * Sliders can also print text labels at regular intervals (or at
 * arbitrary locations) along the slider track.  Painting of labels is
 * controlled by {@code setLabelTable} and {@code setPaintLabels}.
 * <p>
 * For further information and examples see
 * <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/components/slider.html">How to Use Sliders</a>,
 * a section in <em>The Java Tutorial.</em>
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
 * @author David Kloba
 * @since 1.2
 */
@JavaBean(defaultProperty = "UI", description = "A component that supports selecting a integer value from a range.")
@SwingContainer(false)
@SuppressWarnings("serial") // Same-version serialization only
public class JSlider extends JComponent implements SwingConstants, Accessible {
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "SliderUI";

    private boolean paintTicks = false;
    private boolean paintTrack = true;
    private boolean paintLabels = false;
    private boolean isInverted = false;

    /**
     * The data model that handles the numeric maximum value,
     * minimum value, and current-position value for the slider.
     */
    protected BoundedRangeModel sliderModel;

    /**
     * The number of values between the major tick marks -- the
     * larger marks that break up the minor tick marks.
     */
    protected int majorTickSpacing;

    /**
     * The number of values between the minor tick marks -- the
     * smaller marks that occur between the major tick marks.
     * @see #setMinorTickSpacing
     */
    protected int minorTickSpacing;

    /**
     * If true, the knob (and the data value it represents)
     * resolve to the closest tick mark next to where the user
     * positioned the knob.  The default is false.
     * @see #setSnapToTicks
     */
    protected boolean snapToTicks = false;

    /**
     * If true, the knob (and the data value it represents)
     * resolve to the closest slider value next to where the user
     * positioned the knob.
     */
    boolean snapToValue = true;

    /**
     * Whether the slider is horizontal or vertical
     * The default is horizontal.
     *
     * @see #setOrientation
     */
    protected int orientation;


    /**
     * {@code Dictionary} of what labels to draw at which values
     */
    @SuppressWarnings("rawtypes")
    private Dictionary labelTable;
    // For better source compatibility, the labelTable field and
    // associated getter and setter methods are being left as raw
    // types.

    /**
     * The changeListener (no suffix) is the listener we add to the
     * slider's model.  This listener is initialized to the
     * {@code ChangeListener} returned from {@code createChangeListener},
     * which by default just forwards events
     * to {@code ChangeListener}s (if any) added directly to the slider.
     *
     * @see #addChangeListener
     * @see #createChangeListener
     */
    protected ChangeListener changeListener = createChangeListener();


    /**
     * Only one <code>ChangeEvent</code> is needed per slider instance since the
     * event's only (read-only) state is the source property.  The source
     * of events generated here is always "this". The event is lazily
     * created the first time that an event notification is fired.
     *
     * @see #fireStateChanged
     */
    protected transient ChangeEvent changeEvent = null;


    private void checkOrientation(int orientation) {
        switch (orientation) {
        case VERTICAL:
        case HORIZONTAL:
            break;
        default:
            throw new IllegalArgumentException("orientation must be one of: VERTICAL, HORIZONTAL");
        }
    }


    /**
     * Creates a horizontal slider with the range 0 to 100 and
     * an initial value of 50.
     */
    public JSlider() {
        this(HORIZONTAL, 0, 100, 50);
    }


    /**
     * Creates a slider using the specified orientation with the
     * range {@code 0} to {@code 100} and an initial value of {@code 50}.
     * The orientation can be
     * either <code>SwingConstants.VERTICAL</code> or
     * <code>SwingConstants.HORIZONTAL</code>.
     *
     * @param  orientation  the orientation of the slider
     * @throws IllegalArgumentException if orientation is not one of {@code VERTICAL}, {@code HORIZONTAL}
     * @see #setOrientation
     */
    public JSlider(int orientation) {
        this(orientation, 0, 100, 50);
    }


    /**
     * Creates a horizontal slider using the specified min and max
     * with an initial value equal to the average of the min plus max.
     * <p>
     * The <code>BoundedRangeModel</code> that holds the slider's data
     * handles any issues that may arise from improperly setting the
     * minimum and maximum values on the slider.  See the
     * {@code BoundedRangeModel} documentation for details.
     *
     * @param min  the minimum value of the slider
     * @param max  the maximum value of the slider
     *
     * @see BoundedRangeModel
     * @see #setMinimum
     * @see #setMaximum
     */
    public JSlider(int min, int max) {
        this(HORIZONTAL, min, max, (min + max) / 2);
    }


    /**
     * Creates a horizontal slider using the specified min, max and value.
     * <p>
     * The <code>BoundedRangeModel</code> that holds the slider's data
     * handles any issues that may arise from improperly setting the
     * minimum, initial, and maximum values on the slider.  See the
     * {@code BoundedRangeModel} documentation for details.
     *
     * @param min  the minimum value of the slider
     * @param max  the maximum value of the slider
     * @param value  the initial value of the slider
     *
     * @see BoundedRangeModel
     * @see #setMinimum
     * @see #setMaximum
     * @see #setValue
     */
    public JSlider(int min, int max, int value) {
        this(HORIZONTAL, min, max, value);
    }


    /**
     * Creates a slider with the specified orientation and the
     * specified minimum, maximum, and initial values.
     * The orientation can be
     * either <code>SwingConstants.VERTICAL</code> or
     * <code>SwingConstants.HORIZONTAL</code>.
     * <p>
     * The <code>BoundedRangeModel</code> that holds the slider's data
     * handles any issues that may arise from improperly setting the
     * minimum, initial, and maximum values on the slider.  See the
     * {@code BoundedRangeModel} documentation for details.
     *
     * @param orientation  the orientation of the slider
     * @param min  the minimum value of the slider
     * @param max  the maximum value of the slider
     * @param value  the initial value of the slider
     *
     * @throws IllegalArgumentException if orientation is not one of {@code VERTICAL}, {@code HORIZONTAL}
     *
     * @see BoundedRangeModel
     * @see #setOrientation
     * @see #setMinimum
     * @see #setMaximum
     * @see #setValue
     */
    public JSlider(int orientation, int min, int max, int value)
    {
        checkOrientation(orientation);
        this.orientation = orientation;
        setModel(new DefaultBoundedRangeModel(value, 0, min, max));
        updateUI();
    }


    /**
     * Creates a horizontal slider using the specified
     * BoundedRangeModel.
     *
     * @param brm a {@code BoundedRangeModel} for the slider
     */
    public JSlider(BoundedRangeModel brm)
    {
        this.orientation = JSlider.HORIZONTAL;
        setModel(brm);
        updateUI();
    }


    /**
     * Gets the UI object which implements the L&amp;F for this component.
     *
     * @return the SliderUI object that implements the Slider L&amp;F
     */
    public SliderUI getUI() {
        return(SliderUI)ui;
    }


    /**
     * Sets the UI object which implements the L&amp;F for this component.
     *
     * @param ui the SliderUI L&amp;F object
     * @see UIDefaults#getUI
     */
    @BeanProperty(hidden = true, visualUpdate = true, description
            = "The UI object that implements the slider's LookAndFeel.")
    public void setUI(SliderUI ui) {
        super.setUI(ui);
    }


    /**
     * Resets the UI property to a value from the current look and feel.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        setUI((SliderUI)UIManager.getUI(this));
        // The labels preferred size may be derived from the font
        // of the slider, so we must update the UI of the slider first, then
        // that of labels.  This way when setSize is called the right
        // font is used.
        updateLabelUIs();
    }


    /**
     * Returns the name of the L&amp;F class that renders this component.
     *
     * @return the string "SliderUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
    }


    /**
     * We pass Change events along to the listeners with the
     * the slider (instead of the model itself) as the event source.
     */
    private class ModelListener implements ChangeListener, Serializable {
        public void stateChanged(ChangeEvent e) {
            fireStateChanged();
        }
    }


    /**
     * Subclasses that want to handle {@code ChangeEvent}s
     * from the model differently
     * can override this to return
     * an instance of a custom <code>ChangeListener</code> implementation.
     * The default {@code ChangeListener} simply calls the
     * {@code fireStateChanged} method to forward {@code ChangeEvent}s
     * to the {@code ChangeListener}s that have been added directly to the
     * slider.
     *
     * @return a instance of new {@code ChangeListener}
     * @see #changeListener
     * @see #fireStateChanged
     * @see javax.swing.event.ChangeListener
     * @see javax.swing.BoundedRangeModel
     */
    protected ChangeListener createChangeListener() {
        return new ModelListener();
    }


    /**
     * Adds a ChangeListener to the slider.
     *
     * @param l the ChangeListener to add
     * @see #fireStateChanged
     * @see #removeChangeListener
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }


    /**
     * Removes a ChangeListener from the slider.
     *
     * @param l the ChangeListener to remove
     * @see #fireStateChanged
     * @see #addChangeListener

     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }


    /**
     * Returns an array of all the <code>ChangeListener</code>s added
     * to this JSlider with addChangeListener().
     *
     * @return all of the <code>ChangeListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    @BeanProperty(bound = false)
    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }


    /**
     * Send a {@code ChangeEvent}, whose source is this {@code JSlider}, to
     * all {@code ChangeListener}s that have registered interest in
     * {@code ChangeEvent}s.
     * This method is called each time a {@code ChangeEvent} is received from
     * the model.
     * <p>
     * The event instance is created if necessary, and stored in
     * {@code changeEvent}.
     *
     * @see #addChangeListener
     * @see EventListenerList
     */
    protected void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i]==ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener)listeners[i+1]).stateChanged(changeEvent);
            }
        }
    }


    /**
     * Returns the {@code BoundedRangeModel} that handles the slider's three
     * fundamental properties: minimum, maximum, value.
     *
     * @return the data model for this component
     * @see #setModel
     * @see    BoundedRangeModel
     */
    public BoundedRangeModel getModel() {
        return sliderModel;
    }


    /**
     * Sets the {@code BoundedRangeModel} that handles the slider's three
     * fundamental properties: minimum, maximum, value.
     *<p>
     * Attempts to pass a {@code null} model to this method result in
     * undefined behavior, and, most likely, exceptions.
     *
     * @param  newModel the new, {@code non-null} <code>BoundedRangeModel</code> to use
     *
     * @see #getModel
     * @see    BoundedRangeModel
     */
    @BeanProperty(description
            = "The sliders BoundedRangeModel.")
    public void setModel(BoundedRangeModel newModel)
    {
        BoundedRangeModel oldModel = getModel();

        if (oldModel != null) {
            oldModel.removeChangeListener(changeListener);
        }

        sliderModel = newModel;

        if (newModel != null) {
            newModel.addChangeListener(changeListener);
        }

        if (accessibleContext != null) {
            accessibleContext.firePropertyChange(
                                                AccessibleContext.ACCESSIBLE_VALUE_PROPERTY,
                                                (oldModel == null
                                                 ? null : Integer.valueOf(oldModel.getValue())),
                                                (newModel == null
                                                 ? null : Integer.valueOf(newModel.getValue())));
        }

        firePropertyChange("model", oldModel, sliderModel);
    }


    /**
     * Returns the slider's current value
     * from the {@code BoundedRangeModel}.
     *
     * @return  the current value of the slider
     * @see     #setValue
     * @see     BoundedRangeModel#getValue
     */
    public int getValue() {
        return getModel().getValue();
    }

    /**
     * Sets the slider's current value to {@code n}.  This method
     * forwards the new value to the model.
     * <p>
     * The data model (an instance of {@code BoundedRangeModel})
     * handles any mathematical
     * issues arising from assigning faulty values.  See the
     * {@code BoundedRangeModel} documentation for details.
     * <p>
     * If the new value is different from the previous value,
     * all change listeners are notified.
     *
     * @param   n       the new value
     * @see     #getValue
     * @see     #addChangeListener
     * @see     BoundedRangeModel#setValue
     */
    @BeanProperty(bound = false, preferred = true, description
            = "The sliders current value.")
    public void setValue(int n) {
        BoundedRangeModel m = getModel();
        int oldValue = m.getValue();
        if (oldValue == n) {
            return;
        }
        m.setValue(n);
    }


    /**
     * Returns the minimum value supported by the slider
     * from the <code>BoundedRangeModel</code>.
     *
     * @return the value of the model's minimum property
     * @see #setMinimum
     * @see     BoundedRangeModel#getMinimum
     */
    public int getMinimum() {
        return getModel().getMinimum();
    }


    /**
     * Sets the slider's minimum value to {@code minimum}.  This method
     * forwards the new minimum value to the model.
     * <p>
     * The data model (an instance of {@code BoundedRangeModel})
     * handles any mathematical
     * issues arising from assigning faulty values.  See the
     * {@code BoundedRangeModel} documentation for details.
     * <p>
     * If the new minimum value is different from the previous minimum value,
     * all change listeners are notified.
     *
     * @param minimum  the new minimum
     * @see #getMinimum
     * @see    #addChangeListener
     * @see BoundedRangeModel#setMinimum
     */
    @BeanProperty(preferred = true, description
            = "The sliders minimum value.")
    public void setMinimum(int minimum) {
        int oldMin = getModel().getMinimum();
        getModel().setMinimum(minimum);
        firePropertyChange( "minimum", Integer.valueOf( oldMin ), Integer.valueOf( minimum ) );
    }


    /**
     * Returns the maximum value supported by the slider
     * from the <code>BoundedRangeModel</code>.
     *
     * @return the value of the model's maximum property
     * @see #setMaximum
     * @see BoundedRangeModel#getMaximum
     */
    public int getMaximum() {
        return getModel().getMaximum();
    }


    /**
     * Sets the slider's maximum value to {@code maximum}.  This method
     * forwards the new maximum value to the model.
     * <p>
     * The data model (an instance of {@code BoundedRangeModel})
     * handles any mathematical
     * issues arising from assigning faulty values.  See the
     * {@code BoundedRangeModel} documentation for details.
     * <p>
     * If the new maximum value is different from the previous maximum value,
     * all change listeners are notified.
     *
     * @param maximum  the new maximum
     * @see #getMaximum
     * @see #addChangeListener
     * @see BoundedRangeModel#setMaximum
     */
    @BeanProperty(preferred = true, description
            = "The sliders maximum value.")
    public void setMaximum(int maximum) {
        int oldMax = getModel().getMaximum();
        getModel().setMaximum(maximum);
        firePropertyChange( "maximum", Integer.valueOf( oldMax ), Integer.valueOf( maximum ) );
    }


    /**
     * Returns the {@code valueIsAdjusting} property from the model.  For
     * details on how this is used, see the {@code setValueIsAdjusting}
     * documentation.
     *
     * @return the value of the model's {@code valueIsAdjusting} property
     * @see #setValueIsAdjusting
     */
    public boolean getValueIsAdjusting() {
        return getModel().getValueIsAdjusting();
    }


    /**
     * Sets the model's {@code valueIsAdjusting} property.  Slider look and
     * feel implementations should set this property to {@code true} when
     * a knob drag begins, and to {@code false} when the drag ends.
     *
     * @param b the new value for the {@code valueIsAdjusting} property
     * @see   #getValueIsAdjusting
     * @see   BoundedRangeModel#setValueIsAdjusting
     */
    @BeanProperty(bound = false, expert = true, description
            = "True if the slider knob is being dragged.")
    public void setValueIsAdjusting(boolean b) {
        BoundedRangeModel m = getModel();
        boolean oldValue = m.getValueIsAdjusting();
        m.setValueIsAdjusting(b);

        if ((oldValue != b) && (accessibleContext != null)) {
            accessibleContext.firePropertyChange(
                                                AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                ((oldValue) ? AccessibleState.BUSY : null),
                                                ((b) ? AccessibleState.BUSY : null));
        }
    }


    /**
     * Returns the "extent" from the <code>BoundedRangeModel</code>.
     * This represents the range of values "covered" by the knob.
     *
     * @return an int representing the extent
     * @see #setExtent
     * @see BoundedRangeModel#getExtent
     */
    public int getExtent() {
        return getModel().getExtent();
    }


    /**
     * Sets the size of the range "covered" by the knob.  Most look
     * and feel implementations will change the value by this amount
     * if the user clicks on either side of the knob.  This method just
     * forwards the new extent value to the model.
     * <p>
     * The data model (an instance of {@code BoundedRangeModel})
     * handles any mathematical
     * issues arising from assigning faulty values.  See the
     * {@code BoundedRangeModel} documentation for details.
     * <p>
     * If the new extent value is different from the previous extent value,
     * all change listeners are notified.
     *
     * @param extent the new extent
     * @see   #getExtent
     * @see   BoundedRangeModel#setExtent
     */
    @BeanProperty(bound = false, expert = true, description
            = "Size of the range covered by the knob.")
    public void setExtent(int extent) {
        getModel().setExtent(extent);
    }


    /**
     * Return this slider's vertical or horizontal orientation.
     * @return {@code SwingConstants.VERTICAL} or
     *  {@code SwingConstants.HORIZONTAL}
     * @see #setOrientation
     */
    public int getOrientation() {
        return orientation;
    }


    /**
     * Set the slider's orientation to either {@code SwingConstants.VERTICAL} or
     * {@code SwingConstants.HORIZONTAL}.
     *
     * @param orientation {@code HORIZONTAL} or {@code VERTICAL}
     * @throws IllegalArgumentException if orientation is not one of {@code VERTICAL}, {@code HORIZONTAL}
     * @see #getOrientation
     */
    @BeanProperty(preferred = true, visualUpdate = true, enumerationValues = {
            "JSlider.VERTICAL",
            "JSlider.HORIZONTAL"}, description
            = "Set the scrollbars orientation to either VERTICAL or HORIZONTAL.")
    public void setOrientation(int orientation)
    {
        checkOrientation(orientation);
        int oldValue = this.orientation;
        this.orientation = orientation;
        firePropertyChange("orientation", oldValue, orientation);

        if ((oldValue != orientation) && (accessibleContext != null)) {
            accessibleContext.firePropertyChange(
                                                AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                ((oldValue == VERTICAL)
                                                 ? AccessibleState.VERTICAL : AccessibleState.HORIZONTAL),
                                                ((orientation == VERTICAL)
                                                 ? AccessibleState.VERTICAL : AccessibleState.HORIZONTAL));
        }
        if (orientation != oldValue) {
            revalidate();
        }
    }


    /**
     * {@inheritDoc}
     *
     * @since 1.6
     */
    public void setFont(Font font) {
        super.setFont(font);
        updateLabelSizes();
    }

    /**
     * {@inheritDoc}
     * @since 1.7
     */
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
        if (!isShowing()) {
            return false;
        }

        // Check that there is a label with such image
        Enumeration<?> elements = labelTable.elements();

        while (elements.hasMoreElements()) {
            Component component = (Component) elements.nextElement();

            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;

                if (SwingUtilities.doesIconReferenceImage(label.getIcon(), img) ||
                        SwingUtilities.doesIconReferenceImage(label.getDisabledIcon(), img)) {
                    return super.imageUpdate(img, infoflags, x, y, w, h);
                }
            }
        }

        return false;
    }

    /**
     * Returns the dictionary of what labels to draw at which values.
     *
     * @return the <code>Dictionary</code> containing labels and
     *    where to draw them
     */
    @SuppressWarnings("rawtypes")
    public Dictionary getLabelTable() {
/*
        if ( labelTable == null && getMajorTickSpacing() > 0 ) {
            setLabelTable( createStandardLabels( getMajorTickSpacing() ) );
        }
*/
        return labelTable;
    }


    /**
     * Used to specify what label will be drawn at any given value.
     * The key-value pairs are of this format:
     * <code>{ Integer value, java.swing.JComponent label }</code>.
     * <p>
     * An easy way to generate a standard table of value labels is by using the
     * {@code createStandardLabels} method.
     * <p>
     * Once the labels have been set, this method calls {@link #updateLabelUIs}.
     * Note that the labels are only painted if the {@code paintLabels}
     * property is {@code true}.
     *
     * @param labels new {@code Dictionary} of labels, or {@code null} to
     *        remove all labels
     * @see #createStandardLabels(int)
     * @see #getLabelTable
     * @see #setPaintLabels
     */
    @BeanProperty(hidden = true, visualUpdate = true, description
            = "Specifies what labels will be drawn for any given value.")
    @SuppressWarnings("rawtypes")
    public void setLabelTable( Dictionary labels ) {
        Dictionary oldTable = labelTable;
        labelTable = labels;
        updateLabelUIs();
        firePropertyChange("labelTable", oldTable, labelTable );
        if (labels != oldTable) {
            revalidate();
            repaint();
        }
    }


    /**
     * Updates the UIs for the labels in the label table by calling
     * {@code updateUI} on each label.  The UIs are updated from
     * the current look and feel.  The labels are also set to their
     * preferred size.
     *
     * @see #setLabelTable
     * @see JComponent#updateUI
     */
    protected void updateLabelUIs() {
        @SuppressWarnings("rawtypes")
        Dictionary labelTable = getLabelTable();

        if (labelTable == null) {
            return;
        }
        Enumeration<?> labels = labelTable.keys();
        while ( labels.hasMoreElements() ) {
            JComponent component = (JComponent) labelTable.get(labels.nextElement());
            component.updateUI();
            component.setSize(component.getPreferredSize());
        }
    }

    private void updateLabelSizes() {
        @SuppressWarnings("rawtypes")
        Dictionary labelTable = getLabelTable();
        if (labelTable != null) {
            Enumeration<?> labels = labelTable.elements();
            while (labels.hasMoreElements()) {
                JComponent component = (JComponent) labels.nextElement();
                component.setSize(component.getPreferredSize());
            }
        }
    }


    /**
     * Creates a {@code Hashtable} of numerical text labels, starting at the
     * slider minimum, and using the increment specified.
     * For example, if you call <code>createStandardLabels( 10 )</code>
     * and the slider minimum is zero,
     * then labels will be created for the values 0, 10, 20, 30, and so on.
     * <p>
     * For the labels to be drawn on the slider, the returned {@code Hashtable}
     * must be passed into {@code setLabelTable}, and {@code setPaintLabels}
     * must be set to {@code true}.
     * <p>
     * For further details on the makeup of the returned {@code Hashtable}, see
     * the {@code setLabelTable} documentation.
     *
     * @param  increment  distance between labels in the generated hashtable
     * @return a new {@code Hashtable} of labels
     * @see #setLabelTable
     * @see #setPaintLabels
     * @throws IllegalArgumentException if {@code increment} is less than or
     *          equal to zero
     */
    public Hashtable<Integer, JComponent> createStandardLabels( int increment ) {
        return createStandardLabels( increment, getMinimum() );
    }


    /**
     * Creates a {@code Hashtable} of numerical text labels, starting at the
     * starting point specified, and using the increment specified.
     * For example, if you call
     * <code>createStandardLabels( 10, 2 )</code>,
     * then labels will be created for the values 2, 12, 22, 32, and so on.
     * <p>
     * For the labels to be drawn on the slider, the returned {@code Hashtable}
     * must be passed into {@code setLabelTable}, and {@code setPaintLabels}
     * must be set to {@code true}.
     * <p>
     * For further details on the makeup of the returned {@code Hashtable}, see
     * the {@code setLabelTable} documentation.
     *
     * @param  increment  distance between labels in the generated hashtable
     * @param  start      value at which the labels will begin
     * @return a new {@code Hashtable} of labels
     * @see #setLabelTable
     * @see #setPaintLabels
     * @exception IllegalArgumentException if {@code start} is
     *          out of range, or if {@code increment} is less than or equal
     *          to zero
     */
    public Hashtable<Integer, JComponent> createStandardLabels( int increment, int start ) {
        if ( start > getMaximum() || start < getMinimum() ) {
            throw new IllegalArgumentException( "Slider label start point out of range." );
        }

        if ( increment <= 0 ) {
            throw new IllegalArgumentException( "Label incremement must be > 0" );
        }

        class SmartHashtable extends Hashtable<Integer, JComponent> implements PropertyChangeListener {
            int increment = 0;
            int start = 0;
            boolean startAtMin = false;

            class LabelUIResource extends JLabel implements UIResource {
                public LabelUIResource( String text, int alignment ) {
                    super( text, alignment );
                    setName("Slider.label");
                }

                public Font getFont() {
                    Font font = super.getFont();
                    if (font != null && !(font instanceof UIResource)) {
                        return font;
                    }
                    return JSlider.this.getFont();
                }

                public Color getForeground() {
                    Color fg = super.getForeground();
                    if (fg != null && !(fg instanceof UIResource)) {
                        return fg;
                    }
                    if (!(JSlider.this.getForeground() instanceof UIResource)) {
                        return JSlider.this.getForeground();
                    }
                    return fg;
                }
            }

            public SmartHashtable( int increment, int start ) {
                super();
                this.increment = increment;
                this.start = start;
                startAtMin = start == getMinimum();
                createLabels();
            }

            public void propertyChange( PropertyChangeEvent e ) {
                if ( e.getPropertyName().equals( "minimum" ) && startAtMin ) {
                    start = getMinimum();
                }

                if ( e.getPropertyName().equals( "minimum" ) ||
                     e.getPropertyName().equals( "maximum" ) ) {

                    Enumeration<?> keys = getLabelTable().keys();
                    Hashtable<Integer, JComponent> hashtable = new Hashtable<>();

                    // Save the labels that were added by the developer
                    while ( keys.hasMoreElements() ) {
                        Integer key = (Integer) keys.nextElement();
                        JComponent value = (JComponent) labelTable.get(key);
                        if ( !(value instanceof LabelUIResource) ) {
                            hashtable.put( key, value );
                        }
                    }

                    clear();
                    createLabels();

                    // Add the saved labels
                    keys = hashtable.keys();
                    while ( keys.hasMoreElements() ) {
                        Integer key = (Integer) keys.nextElement();
                        put( key, hashtable.get( key ) );
                    }

                    ((JSlider)e.getSource()).setLabelTable( this );
                }
            }

            void createLabels() {
                for ( int labelIndex = start; labelIndex <= getMaximum(); labelIndex += increment ) {
                    put( Integer.valueOf( labelIndex ), new LabelUIResource( ""+labelIndex, JLabel.CENTER ) );
                }
            }
        }

        SmartHashtable table = new SmartHashtable( increment, start );

        @SuppressWarnings("rawtypes")
        Dictionary labelTable = getLabelTable();

        if (labelTable != null && (labelTable instanceof PropertyChangeListener)) {
            removePropertyChangeListener((PropertyChangeListener) labelTable);
        }

        addPropertyChangeListener( table );

        return table;
    }


    /**
     * Returns true if the value-range shown for the slider is reversed,
     *
     * @return true if the slider values are reversed from their normal order
     * @see #setInverted
     */
    public boolean getInverted() {
        return isInverted;
    }


    /**
     * Specify true to reverse the value-range shown for the slider and false to
     * put the value range in the normal order.  The order depends on the
     * slider's <code>ComponentOrientation</code> property.  Normal (non-inverted)
     * horizontal sliders with a <code>ComponentOrientation</code> value of
     * <code>LEFT_TO_RIGHT</code> have their maximum on the right.
     * Normal horizontal sliders with a <code>ComponentOrientation</code> value of
     * <code>RIGHT_TO_LEFT</code> have their maximum on the left.  Normal vertical
     * sliders have their maximum on the top.  These labels are reversed when the
     * slider is inverted.
     * <p>
     * By default, the value of this property is {@code false}.
     *
     * @param b  true to reverse the slider values from their normal order
     */
    @BeanProperty(visualUpdate = true, description
            = "If true reverses the slider values from their normal order")
    public void setInverted( boolean b ) {
        boolean oldValue = isInverted;
        isInverted = b;
        firePropertyChange("inverted", oldValue, isInverted);
        if (b != oldValue) {
            repaint();
        }
    }


    /**
     * This method returns the major tick spacing.  The number that is returned
     * represents the distance, measured in values, between each major tick mark.
     * If you have a slider with a range from 0 to 50 and the major tick spacing
     * is set to 10, you will get major ticks next to the following values:
     * 0, 10, 20, 30, 40, 50.
     *
     * @return the number of values between major ticks
     * @see #setMajorTickSpacing
     */
    public int getMajorTickSpacing() {
        return majorTickSpacing;
    }


    /**
     * This method sets the major tick spacing.  The number that is passed in
     * represents the distance, measured in values, between each major tick mark.
     * If you have a slider with a range from 0 to 50 and the major tick spacing
     * is set to 10, you will get major ticks next to the following values:
     * 0, 10, 20, 30, 40, 50.
     * <p>
     * In order for major ticks to be painted, {@code setPaintTicks} must be
     * set to {@code true}.
     * <p>
     * This method will also set up a label table for you.
     * If there is not already a label table, and the major tick spacing is
     * {@code > 0}, and {@code getPaintLabels} returns
     * {@code true}, a standard label table will be generated (by calling
     * {@code createStandardLabels}) with labels at the major tick marks.
     * For the example above, you would get text labels: "0",
     * "10", "20", "30", "40", "50".
     * The label table is then set on the slider by calling
     * {@code setLabelTable}.
     *
     * @param  n  new value for the {@code majorTickSpacing} property
     * @see #getMajorTickSpacing
     * @see #setPaintTicks
     * @see #setLabelTable
     * @see #createStandardLabels(int)
     */
    @BeanProperty(visualUpdate = true, description
            = "Sets the number of values between major tick marks.")
    public void setMajorTickSpacing(int n) {
        int oldValue = majorTickSpacing;
        majorTickSpacing = n;
        if ( labelTable == null && getMajorTickSpacing() > 0 && getPaintLabels() ) {
            setLabelTable( createStandardLabels( getMajorTickSpacing() ) );
        }
        firePropertyChange("majorTickSpacing", oldValue, majorTickSpacing);
        if (majorTickSpacing != oldValue && getPaintTicks()) {
            repaint();
        }
    }



    /**
     * This method returns the minor tick spacing.  The number that is returned
     * represents the distance, measured in values, between each minor tick mark.
     * If you have a slider with a range from 0 to 50 and the minor tick spacing
     * is set to 10, you will get minor ticks next to the following values:
     * 0, 10, 20, 30, 40, 50.
     *
     * @return the number of values between minor ticks
     * @see #getMinorTickSpacing
     */
    public int getMinorTickSpacing() {
        return minorTickSpacing;
    }


    /**
     * This method sets the minor tick spacing.  The number that is passed in
     * represents the distance, measured in values, between each minor tick mark.
     * If you have a slider with a range from 0 to 50 and the minor tick spacing
     * is set to 10, you will get minor ticks next to the following values:
     * 0, 10, 20, 30, 40, 50.
     * <p>
     * In order for minor ticks to be painted, {@code setPaintTicks} must be
     * set to {@code true}.
     *
     * @param  n  new value for the {@code minorTickSpacing} property
     * @see #getMinorTickSpacing
     * @see #setPaintTicks
     */
    @BeanProperty(visualUpdate = true, description
            = "Sets the number of values between minor tick marks.")
    public void setMinorTickSpacing(int n) {
        int oldValue = minorTickSpacing;
        minorTickSpacing = n;
        firePropertyChange("minorTickSpacing", oldValue, minorTickSpacing);
        if (minorTickSpacing != oldValue && getPaintTicks()) {
            repaint();
        }
    }


    /**
     * Returns true if the knob (and the data value it represents)
     * resolve to the closest tick mark next to where the user
     * positioned the knob.
     *
     * @return true if the value snaps to the nearest tick mark, else false
     * @see #setSnapToTicks
     */
    public boolean getSnapToTicks() {
        return snapToTicks;
    }


    /**
     * Returns true if the knob (and the data value it represents)
     * resolve to the closest slider value next to where the user
     * positioned the knob.
     *
     * @return true if the value snaps to the nearest slider value, else false
     * @see #setSnapToValue
     */
    boolean getSnapToValue() {
        return snapToValue;
    }


    /**
     * Specifying true makes the knob (and the data value it represents)
     * resolve to the closest tick mark next to where the user
     * positioned the knob.
     * By default, this property is {@code false}.
     *
     * @param b  true to snap the knob to the nearest tick mark
     * @see #getSnapToTicks
     */
    @BeanProperty(description
            = "If true snap the knob to the nearest tick mark.")
    public void setSnapToTicks(boolean b) {
        boolean oldValue = snapToTicks;
        snapToTicks = b;
        firePropertyChange("snapToTicks", oldValue, snapToTicks);
    }


    /**
     * Specifying true makes the knob (and the data value it represents)
     * resolve to the closest slider value next to where the user
     * positioned the knob. If the {@code snapToTicks} property has also been
     * set to {@code true}, the snap-to-ticks behavior will prevail.
     * By default, the snapToValue property is {@code true}.
     *
     * @param b  true to snap the knob to the nearest slider value
     * @see #getSnapToValue
     * @see #setSnapToTicks
     */
    @BeanProperty(description
            = "If true snap the knob to the nearest slider value.")
    void setSnapToValue(boolean b) {
        boolean oldValue = snapToValue;
        snapToValue = b;
        firePropertyChange("snapToValue", oldValue, snapToValue);
    }


    /**
     * Tells if tick marks are to be painted.
     * @return true if tick marks are painted, else false
     * @see #setPaintTicks
     */
    public boolean getPaintTicks() {
        return paintTicks;
    }


    /**
     * Determines whether tick marks are painted on the slider.
     * By default, this property is {@code false}.
     *
     * @param  b  whether or not tick marks should be painted
     * @see #getPaintTicks
     */
    @BeanProperty(visualUpdate = true, description
            = "If true tick marks are painted on the slider.")
    public void setPaintTicks(boolean b) {
        boolean oldValue = paintTicks;
        paintTicks = b;
        firePropertyChange("paintTicks", oldValue, paintTicks);
        if (paintTicks != oldValue) {
            revalidate();
            repaint();
        }
    }

    /**
     * Tells if the track (area the slider slides in) is to be painted.
     * @return true if track is painted, else false
     * @see #setPaintTrack
     */
    public boolean getPaintTrack() {
        return paintTrack;
    }

    /**
     * Determines whether the track is painted on the slider. By default, this
     * property is {@code true}. It is up to the look and feel to honor this
     * property, some may choose to ignore it.
     *
     * @param  b  whether or not to paint the slider track
     * @see #getPaintTrack
     */
    @BeanProperty(visualUpdate = true, description
            = "If true, the track is painted on the slider.")
    public void setPaintTrack(boolean b) {
        boolean oldValue = paintTrack;
        paintTrack = b;
        firePropertyChange("paintTrack", oldValue, paintTrack);
        if (paintTrack != oldValue) {
            repaint();
        }
    }


    /**
     * Tells if labels are to be painted.
     * @return true if labels are painted, else false
     * @see #setPaintLabels
     */
    public boolean getPaintLabels() {
        return paintLabels;
    }


    /**
     * Determines whether labels are painted on the slider.
     * <p>
     * This method will also set up a label table for you.
     * If there is not already a label table, and the major tick spacing is
     * {@code > 0},
     * a standard label table will be generated (by calling
     * {@code createStandardLabels}) with labels at the major tick marks.
     * The label table is then set on the slider by calling
     * {@code setLabelTable}.
     * <p>
     * By default, this property is {@code false}.
     *
     * @param  b  whether or not to paint labels
     * @see #getPaintLabels
     * @see #getLabelTable
     * @see #createStandardLabels(int)
     */
    @BeanProperty(visualUpdate = true, description
            = "If true labels are painted on the slider.")
    public void setPaintLabels(boolean b) {
        boolean oldValue = paintLabels;
        paintLabels = b;
        if ( labelTable == null && getMajorTickSpacing() > 0 ) {
            setLabelTable( createStandardLabels( getMajorTickSpacing() ) );
        }
        firePropertyChange("paintLabels", oldValue, paintLabels);
        if (paintLabels != oldValue) {
            revalidate();
            repaint();
        }
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
     * Returns a string representation of this JSlider. This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this JSlider.
     */
    protected String paramString() {
        String paintTicksString = (paintTicks ?
                                   "true" : "false");
        String paintTrackString = (paintTrack ?
                                   "true" : "false");
        String paintLabelsString = (paintLabels ?
                                    "true" : "false");
        String isInvertedString = (isInverted ?
                                   "true" : "false");
        String snapToTicksString = (snapToTicks ?
                                    "true" : "false");
        String snapToValueString = (snapToValue ?
                                    "true" : "false");
        String orientationString = (orientation == HORIZONTAL ?
                                    "HORIZONTAL" : "VERTICAL");

        return super.paramString() +
        ",isInverted=" + isInvertedString +
        ",majorTickSpacing=" + majorTickSpacing +
        ",minorTickSpacing=" + minorTickSpacing +
        ",orientation=" + orientationString +
        ",paintLabels=" + paintLabelsString +
        ",paintTicks=" + paintTicksString +
        ",paintTrack=" + paintTrackString +
        ",snapToTicks=" + snapToTicksString +
        ",snapToValue=" + snapToValueString;
    }


/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this JSlider.
     * For sliders, the AccessibleContext takes the form of an
     * AccessibleJSlider.
     * A new AccessibleJSlider instance is created if necessary.
     *
     * @return an AccessibleJSlider that serves as the
     *         AccessibleContext of this JSlider
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJSlider();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JSlider</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to slider user-interface elements.
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
    @SuppressWarnings("serial") // Same-version serialization only
    protected class AccessibleJSlider extends AccessibleJComponent
    implements AccessibleValue, ChangeListener, AccessibleAction {


        private int oldModelValue;

        /**
         * constructs an AccessibleJSlider
         */
        protected AccessibleJSlider() {
            // model is guaranteed to be non-null
            oldModelValue = getModel().getValue();
            JSlider.this.addChangeListener(this);
        }
        /**
         * Get the state set of this object.
         *
         * @return an instance of AccessibleState containing the current state
         * of the object
         * @see AccessibleState
         */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = super.getAccessibleStateSet();
            if (getValueIsAdjusting()) {
                states.add(AccessibleState.BUSY);
            }
            if (getOrientation() == VERTICAL) {
                states.add(AccessibleState.VERTICAL);
            }
            else {
                states.add(AccessibleState.HORIZONTAL);
            }
            return states;
        }

        /**
         * Invoked when the target of the listener has changed its state.
         *
         * @param e  a {@code ChangeEvent} object. Must not be {@code null}
         * @throws NullPointerException if the parameter is {@code null}
         */
        public void stateChanged(ChangeEvent e) {
            if (e == null) {
                throw new NullPointerException();
            }
            int newModelValue = getModel().getValue();
            firePropertyChange(
                    AccessibleContext.ACCESSIBLE_VALUE_PROPERTY,
                    Integer.valueOf(oldModelValue),
                    Integer.valueOf(newModelValue));
            oldModelValue = newModelValue;
        }

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the object
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SLIDER;
        }

        /**
         * Get the AccessibleValue associated with this object.  In the
         * implementation of the Java Accessibility API for this class,
         * return this object, which is responsible for implementing the
         * AccessibleValue interface on behalf of itself.
         *
         * @return this object
         */
        public AccessibleValue getAccessibleValue() {
            return this;
        }

        /**
         * Get the accessible value of this object.
         *
         * @return The current value of this object.
         */
        public Number getCurrentAccessibleValue() {
            return Integer.valueOf(getValue());
        }

        /**
         * Set the value of this object as a Number.
         *
         * @return True if the value was set.
         */
        public boolean setCurrentAccessibleValue(Number n) {
            // TIGER - 4422535
            if (n == null) {
                return false;
            }
            setValue(n.intValue());
            return true;
        }

        /**
         * Get the minimum accessible value of this object.
         *
         * @return The minimum value of this object.
         */
        public Number getMinimumAccessibleValue() {
            return Integer.valueOf(getMinimum());
        }

        /**
         * Get the maximum accessible value of this object.
         *
         * @return The maximum value of this object.
         */
        public Number getMaximumAccessibleValue() {
            // TIGER - 4422362
            BoundedRangeModel model = JSlider.this.getModel();
            return Integer.valueOf(model.getMaximum() - model.getExtent());
        }

        /**
         * Gets the AccessibleAction associated with this object that supports
         * one or more actions.
         *
         * @return AccessibleAction if supported by object; else return null
         * @see AccessibleAction
         */
        public AccessibleAction getAccessibleAction() {
            return this;
        }

        /* ===== Begin AccessibleAction impl ===== */

        /**
         * Returns the number of accessible actions available in this object
         * If there are more than one, the first one is considered the "default"
         * action of the object.
         *
         * Two actions are supported: AccessibleAction.INCREMENT which
         * increments the slider value and AccessibleAction.DECREMENT
         * which decrements the slider value
         *
         * @return the zero-based number of Actions in this object
         */
        public int getAccessibleActionCount() {
            return 2;
        }

        /**
         * Returns a description of the specified action of the object.
         *
         * @param i zero-based index of the actions
         * @return a String description of the action
         * @see #getAccessibleActionCount
         */
        public String getAccessibleActionDescription(int i) {
            if (i == 0) {
                return AccessibleAction.INCREMENT;
            } else if (i == 1) {
                return AccessibleAction.DECREMENT;
            }
            return null;
        }

        /**
         * Performs the specified Action on the object
         *
         * @param i zero-based index of actions. The first action
         * (index 0) is AccessibleAction.INCREMENT and the second
         * action (index 1) is AccessibleAction.DECREMENT.
         * @return true if the action was performed, otherwise false
         * @see #getAccessibleActionCount
         */
        public boolean doAccessibleAction(int i) {
            if (i < 0 || i > 1) {
                return false;
            }
            //0 is increment, 1 is decrrement
            int delta = ((i > 0) ? -1 : 1);
            JSlider.this.setValue(oldModelValue + delta);
            return true;
        }

        /* ===== End AccessibleAction impl ===== */

    } // AccessibleJSlider
}
