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

import java.awt.Graphics;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.text.Format;
import java.text.NumberFormat;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.ProgressBarUI;

/**
 * A component that visually displays the progress of some task.  As the task
 * progresses towards completion, the progress bar displays the
 * task's percentage of completion.
 * This percentage is typically represented visually by a rectangle which
 * starts out empty and gradually becomes filled in as the task progresses.
 * In addition, the progress bar can display a textual representation of this
 * percentage.
 * <p>
 * {@code JProgressBar} uses a {@code BoundedRangeModel} as its data model,
 * with the {@code value} property representing the "current" state of the task,
 * and the {@code minimum} and {@code maximum} properties representing the
 * beginning and end points, respectively.
 * <p>
 * To indicate that a task of unknown length is executing,
 * you can put a progress bar into indeterminate mode.
 * While the bar is in indeterminate mode,
 * it animates constantly to show that work is occurring.
 * As soon as you can determine the task's length and amount of progress,
 * you should update the progress bar's value
 * and switch it back to determinate mode.
 *
 * <p>
 *
 * Here is an example of creating a progress bar,
 * where <code>task</code> is an object (representing some piece of work)
 * which returns information about the progress of the task:
 *
 *<pre>
 *progressBar = new JProgressBar(0, task.getLengthOfTask());
 *progressBar.setValue(0);
 *progressBar.setStringPainted(true);
 *</pre>
 *
 * Here is an example of querying the current state of the task, and using
 * the returned value to update the progress bar:
 *
 *<pre>
 *progressBar.setValue(task.getCurrent());
 *</pre>
 *
 * Here is an example of putting a progress bar into
 * indeterminate mode,
 * and then switching back to determinate mode
 * once the length of the task is known:
 *
 *<pre>
 *progressBar = new JProgressBar();
 *<em>...//when the task of (initially) unknown length begins:</em>
 *progressBar.setIndeterminate(true);
 *<em>...//do some work; get length of task...</em>
 *progressBar.setMaximum(newLength);
 *progressBar.setValue(newValue);
 *progressBar.setIndeterminate(false);
 *</pre>
 *
 * <p>
 *
 * For complete examples and further documentation see
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/progress.html" target="_top">How to Monitor Progress</a>,
 * a section in <em>The Java Tutorial.</em>
 *
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
 * @see javax.swing.plaf.basic.BasicProgressBarUI
 * @see javax.swing.BoundedRangeModel
 * @see javax.swing.SwingWorker
 *
 * @author Michael C. Albers
 * @author Kathy Walrath
 * @since 1.2
 */
@JavaBean(defaultProperty = "UI", description = "A component that displays an integer value.")
@SwingContainer(false)
@SuppressWarnings("serial") // Same-version serialization only
public class JProgressBar extends JComponent implements SwingConstants, Accessible
{
    /**
     * @see #getUIClassID
     */
    private static final String uiClassID = "ProgressBarUI";

    /**
     * Whether the progress bar is horizontal or vertical.
     * The default is <code>HORIZONTAL</code>.
     *
     * @see #setOrientation
     */
    protected int orientation;

    /**
     * Whether to display a border around the progress bar.
     * The default is <code>true</code>.
     *
     * @see #setBorderPainted
     */
    protected boolean paintBorder;

    /**
     * The object that holds the data for the progress bar.
     *
     * @see #setModel
     */
    protected BoundedRangeModel model;

    /**
     * An optional string that can be displayed on the progress bar.
     * The default is <code>null</code>. Setting this to a non-<code>null</code>
     * value does not imply that the string will be displayed.
     * To display the string, {@code paintString} must be {@code true}.
     *
     * @see #setString
     * @see #setStringPainted
     */
    protected String progressString;

    /**
     * Whether to display a string of text on the progress bar.
     * The default is <code>false</code>.
     * Setting this to <code>true</code> causes a textual
     * display of the progress to be rendered on the progress bar. If
     * the <code>progressString</code> is <code>null</code>,
     * the percentage of completion is displayed on the progress bar.
     * Otherwise, the <code>progressString</code> is
     * rendered on the progress bar.
     *
     * @see #setStringPainted
     * @see #setString
     */
    protected boolean paintString;

    /**
     * The default minimum for a progress bar is 0.
     */
    private static final int defaultMinimum = 0;
    /**
     * The default maximum for a progress bar is 100.
     */
    private static final int defaultMaximum = 100;
    /**
     * The default orientation for a progress bar is <code>HORIZONTAL</code>.
     */
    private static final int defaultOrientation = HORIZONTAL;

    /**
     * Only one <code>ChangeEvent</code> is needed per instance since the
     * event's only interesting property is the immutable source, which
     * is the progress bar.
     * The event is lazily created the first time that an
     * event notification is fired.
     *
     * @see #fireStateChanged
     */
    protected transient ChangeEvent changeEvent = null;

    /**
     * Listens for change events sent by the progress bar's model,
     * redispatching them
     * to change-event listeners registered upon
     * this progress bar.
     *
     * @see #createChangeListener
     */
    protected ChangeListener changeListener = null;

    /**
     * Format used when displaying percent complete.
     */
    private transient Format format;

    /**
     * Whether the progress bar is indeterminate (<code>true</code>) or
     * normal (<code>false</code>); the default is <code>false</code>.
     *
     * @see #setIndeterminate
     * @since 1.4
     */
    private boolean indeterminate;


   /**
     * Creates a horizontal progress bar
     * that displays a border but no progress string.
     * The initial and minimum values are 0,
     * and the maximum is 100.
     *
     * @see #setOrientation
     * @see #setBorderPainted
     * @see #setStringPainted
     * @see #setString
     * @see #setIndeterminate
     */
    public JProgressBar()
    {
        this(defaultOrientation);
    }

   /**
     * Creates a progress bar with the specified orientation,
     * which can be
     * either {@code SwingConstants.VERTICAL} or
     * {@code SwingConstants.HORIZONTAL}.
     * By default, a border is painted but a progress string is not.
     * The initial and minimum values are 0,
     * and the maximum is 100.
     *
     * @param orient  the desired orientation of the progress bar
     * @throws IllegalArgumentException if {@code orient} is an illegal value
     *
     * @see #setOrientation
     * @see #setBorderPainted
     * @see #setStringPainted
     * @see #setString
     * @see #setIndeterminate
     */
    public JProgressBar(int orient)
    {
        this(orient, defaultMinimum, defaultMaximum);
    }


    /**
     * Creates a horizontal progress bar
     * with the specified minimum and maximum.
     * Sets the initial value of the progress bar to the specified minimum.
     * By default, a border is painted but a progress string is not.
     * <p>
     * The <code>BoundedRangeModel</code> that holds the progress bar's data
     * handles any issues that may arise from improperly setting the
     * minimum, initial, and maximum values on the progress bar.
     * See the {@code BoundedRangeModel} documentation for details.
     *
     * @param min  the minimum value of the progress bar
     * @param max  the maximum value of the progress bar
     *
     * @see BoundedRangeModel
     * @see #setOrientation
     * @see #setBorderPainted
     * @see #setStringPainted
     * @see #setString
     * @see #setIndeterminate
     */
    public JProgressBar(int min, int max)
    {
        this(defaultOrientation, min, max);
    }


    /**
     * Creates a progress bar using the specified orientation,
     * minimum, and maximum.
     * By default, a border is painted but a progress string is not.
     * Sets the initial value of the progress bar to the specified minimum.
     * <p>
     * The <code>BoundedRangeModel</code> that holds the progress bar's data
     * handles any issues that may arise from improperly setting the
     * minimum, initial, and maximum values on the progress bar.
     * See the {@code BoundedRangeModel} documentation for details.
     *
     * @param orient  the desired orientation of the progress bar
     * @param min  the minimum value of the progress bar
     * @param max  the maximum value of the progress bar
     * @throws IllegalArgumentException if {@code orient} is an illegal value
     *
     * @see BoundedRangeModel
     * @see #setOrientation
     * @see #setBorderPainted
     * @see #setStringPainted
     * @see #setString
     * @see #setIndeterminate
     */
    public JProgressBar(int orient, int min, int max)
    {
        // Creating the model this way is a bit simplistic, but
        //  I believe that it is the most common usage of this
        //  component - it's what people will expect.
        setModel(new DefaultBoundedRangeModel(min, 0, min, max));
        updateUI();

        setOrientation(orient);      // documented with set/getOrientation()
        setBorderPainted(true);      // documented with is/setBorderPainted()
        setStringPainted(false);     // see setStringPainted
        setString(null);             // see getString
        setIndeterminate(false);     // see setIndeterminate
    }


    /**
     * Creates a horizontal progress bar
     * that uses the specified model
     * to hold the progress bar's data.
     * By default, a border is painted but a progress string is not.
     *
     * @param newModel  the data model for the progress bar
     *
     * @see #setOrientation
     * @see #setBorderPainted
     * @see #setStringPainted
     * @see #setString
     * @see #setIndeterminate
     */
    public JProgressBar(BoundedRangeModel newModel)
    {
        setModel(newModel);
        updateUI();

        setOrientation(defaultOrientation);  // see setOrientation()
        setBorderPainted(true);              // see setBorderPainted()
        setStringPainted(false);             // see setStringPainted
        setString(null);                     // see getString
        setIndeterminate(false);             // see setIndeterminate
    }


    /**
     * Returns {@code SwingConstants.VERTICAL} or
     * {@code SwingConstants.HORIZONTAL}, depending on the orientation
     * of the progress bar. The default orientation is
     * {@code SwingConstants.HORIZONTAL}.
     *
     * @return <code>HORIZONTAL</code> or <code>VERTICAL</code>
     * @see #setOrientation
     */
    public int getOrientation() {
        return orientation;
    }


   /**
     * Sets the progress bar's orientation to <code>newOrientation</code>,
     * which must be {@code SwingConstants.VERTICAL} or
     * {@code SwingConstants.HORIZONTAL}. The default orientation
     * is {@code SwingConstants.HORIZONTAL}.
     *
     * @param  newOrientation  <code>HORIZONTAL</code> or <code>VERTICAL</code>
     * @exception      IllegalArgumentException    if <code>newOrientation</code>
     *                                              is an illegal value
     * @see #getOrientation
     */
    @BeanProperty(preferred = true, visualUpdate = true, description
            = "Set the progress bar's orientation.")
    public void setOrientation(int newOrientation) {
        if (orientation != newOrientation) {
            switch (newOrientation) {
            case VERTICAL:
            case HORIZONTAL:
                int oldOrientation = orientation;
                orientation = newOrientation;
                firePropertyChange("orientation", oldOrientation, newOrientation);
                if (accessibleContext != null) {
                    accessibleContext.firePropertyChange(
                            AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                            ((oldOrientation == VERTICAL)
                             ? AccessibleState.VERTICAL
                             : AccessibleState.HORIZONTAL),
                            ((orientation == VERTICAL)
                             ? AccessibleState.VERTICAL
                             : AccessibleState.HORIZONTAL));
                }
                break;
            default:
                throw new IllegalArgumentException(newOrientation +
                                             " is not a legal orientation");
            }
            revalidate();
        }
    }


    /**
     * Returns the value of the <code>stringPainted</code> property.
     *
     * @return the value of the <code>stringPainted</code> property
     * @see    #setStringPainted
     * @see    #setString
     */
    public boolean isStringPainted() {
        return paintString;
    }


    /**
     * Sets the value of the <code>stringPainted</code> property,
     * which determines whether the progress bar
     * should render a progress string.
     * The default is <code>false</code>, meaning
     * no string is painted.
     * Some look and feels might not support progress strings
     * or might support them only when the progress bar is in determinate mode.
     *
     * @param   b       <code>true</code> if the progress bar should render a string
     * @see     #isStringPainted
     * @see     #setString
     */
    @BeanProperty(visualUpdate = true, description
            = "Whether the progress bar should render a string.")
    public void setStringPainted(boolean b) {
        //PENDING: specify that string not painted when in indeterminate mode?
        //         or just leave that to the L&F?
        boolean oldValue = paintString;
        paintString = b;
        firePropertyChange("stringPainted", oldValue, paintString);
        if (paintString != oldValue) {
            revalidate();
            repaint();
        }
    }


    /**
     * Returns a {@code String} representation of the current progress.
     * By default, this returns a simple percentage {@code String} based on
     * the value returned from {@code getPercentComplete}.  An example
     * would be the "42%".  You can change this by calling {@code setString}.
     *
     * @return the value of the progress string, or a simple percentage string
     *         if the progress string is {@code null}
     * @see    #setString
     */
    public String getString(){
        if (progressString != null) {
            return progressString;
        } else {
            if (format == null) {
                format = NumberFormat.getPercentInstance();
            }
            return format.format(Double.valueOf(getPercentComplete()));
        }
    }

    /**
     * Sets the value of the progress string. By default,
     * this string is <code>null</code>, implying the built-in behavior of
     * using a simple percent string.
     * If you have provided a custom progress string and want to revert to
     * the built-in behavior, set the string back to <code>null</code>.
     * <p>
     * The progress string is painted only if
     * the <code>isStringPainted</code> method returns <code>true</code>.
     *
     * @param  s       the value of the progress string
     * @see    #getString
     * @see    #setStringPainted
     * @see    #isStringPainted
     */
    @BeanProperty(visualUpdate = true, description
            = "Specifies the progress string to paint")
    public void setString(String s){
        String oldValue = progressString;
        progressString = s;
        firePropertyChange("string", oldValue, progressString);
        if (progressString == null || oldValue == null || !progressString.equals(oldValue)) {
            repaint();
        }
    }

    /**
     * Returns the percent complete for the progress bar.
     * Note that this number is between 0.0 and 1.0.
     *
     * @return the percent complete for this progress bar
     */
    @BeanProperty(bound = false)
    public double getPercentComplete() {
        long span = model.getMaximum() - model.getMinimum();
        double currentValue = model.getValue();
        double pc = (currentValue - model.getMinimum()) / span;
        return pc;
    }

    /**
     * Returns the <code>borderPainted</code> property.
     *
     * @return the value of the <code>borderPainted</code> property
     * @see    #setBorderPainted
     */
    public boolean isBorderPainted() {
        return paintBorder;
    }

    /**
     * Sets the <code>borderPainted</code> property, which is
     * <code>true</code> if the progress bar should paint its border.
     * The default value for this property is <code>true</code>.
     * Some look and feels might not implement painted borders;
     * they will ignore this property.
     *
     * @param   b       <code>true</code> if the progress bar
     *                  should paint its border;
     *                  otherwise, <code>false</code>
     * @see     #isBorderPainted
     */
    @BeanProperty(visualUpdate = true, description
            = "Whether the progress bar should paint its border.")
    public void setBorderPainted(boolean b) {
        boolean oldValue = paintBorder;
        paintBorder = b;
        firePropertyChange("borderPainted", oldValue, paintBorder);
        if (paintBorder != oldValue) {
            repaint();
        }
    }

    /**
     * Paints the progress bar's border if the <code>borderPainted</code>
     * property is <code>true</code>.
     *
     * @param g  the <code>Graphics</code> context within which to paint the border
     * @see #paint
     * @see #setBorder
     * @see #isBorderPainted
     * @see #setBorderPainted
     */
    protected void paintBorder(Graphics g) {
        if (isBorderPainted()) {
            super.paintBorder(g);
        }
    }


    /**
     * Returns the look-and-feel object that renders this component.
     *
     * @return the <code>ProgressBarUI</code> object that renders this component
     */
    public ProgressBarUI getUI() {
        return (ProgressBarUI)ui;
    }

    /**
     * Sets the look-and-feel object that renders this component.
     *
     * @param ui  a <code>ProgressBarUI</code> object
     * @see UIDefaults#getUI
     */
    @BeanProperty(hidden = true, visualUpdate = true, description
            = "The UI object that implements the Component's LookAndFeel.")
    public void setUI(ProgressBarUI ui) {
        super.setUI(ui);
    }


    /**
     * Resets the UI property to a value from the current look and feel.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        setUI((ProgressBarUI)UIManager.getUI(this));
    }


    /**
     * Returns the name of the look-and-feel class that renders this component.
     *
     * @return the string "ProgressBarUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false, expert = true, description
            = "A string that specifies the name of the look-and-feel class.")
    public String getUIClassID() {
        return uiClassID;
    }


    /* We pass each Change event to the listeners with the
     * the progress bar as the event source.
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
    private class ModelListener implements ChangeListener, Serializable {
        public void stateChanged(ChangeEvent e) {
            fireStateChanged();
        }
    }

    /**
     * Subclasses that want to handle change events
     * from the model differently
     * can override this to return
     * an instance of a custom <code>ChangeListener</code> implementation.
     * The default {@code ChangeListener} simply calls the
     * {@code fireStateChanged} method to forward {@code ChangeEvent}s
     * to the {@code ChangeListener}s that have been added directly to the
     * progress bar.
     *
     * @return the instance of a custom {@code ChangeListener} implementation.
     * @see #changeListener
     * @see #fireStateChanged
     * @see javax.swing.event.ChangeListener
     * @see javax.swing.BoundedRangeModel
     */
    protected ChangeListener createChangeListener() {
        return new ModelListener();
    }

    /**
     * Adds the specified <code>ChangeListener</code> to the progress bar.
     *
     * @param l the <code>ChangeListener</code> to add
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * Removes a <code>ChangeListener</code> from the progress bar.
     *
     * @param l the <code>ChangeListener</code> to remove
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    /**
     * Returns an array of all the <code>ChangeListener</code>s added
     * to this progress bar with <code>addChangeListener</code>.
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
     * Send a {@code ChangeEvent}, whose source is this {@code JProgressBar}, to
     * all {@code ChangeListener}s that have registered interest in
     * {@code ChangeEvent}s.
     * This method is called each time a {@code ChangeEvent} is received from
     * the model.
     * <p>
     *
     * The event instance is created if necessary, and stored in
     * {@code changeEvent}.
     *
     * @see #addChangeListener
     * @see EventListenerList
     */
    protected void fireStateChanged() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ChangeListener.class) {
                // Lazily create the event:
                if (changeEvent == null)
                    changeEvent = new ChangeEvent(this);
                ((ChangeListener)listeners[i+1]).stateChanged(changeEvent);
            }
        }
    }

    /**
     * Returns the data model used by this progress bar.
     *
     * @return the <code>BoundedRangeModel</code> currently in use
     * @see #setModel
     * @see    BoundedRangeModel
     */
    public BoundedRangeModel getModel() {
        return model;
    }

    /**
     * Sets the data model used by the <code>JProgressBar</code>.
     * Note that the {@code BoundedRangeModel}'s {@code extent} is not used,
     * and is set to {@code 0}.
     *
     * @param  newModel the <code>BoundedRangeModel</code> to use
     */
    @BeanProperty(bound = false, expert = true, description
            = "The data model used by the JProgressBar.")
    public void setModel(BoundedRangeModel newModel) {
        // PENDING(???) setting the same model to multiple bars is broken; listeners
        BoundedRangeModel oldModel = getModel();

        if (newModel != oldModel) {
            if (oldModel != null) {
                oldModel.removeChangeListener(changeListener);
                changeListener = null;
            }

            model = newModel;

            if (newModel != null) {
                changeListener = createChangeListener();
                newModel.addChangeListener(changeListener);
            }

            if (accessibleContext != null) {
                accessibleContext.firePropertyChange(
                        AccessibleContext.ACCESSIBLE_VALUE_PROPERTY,
                        (oldModel== null
                         ? null : Integer.valueOf(oldModel.getValue())),
                        (newModel== null
                         ? null : Integer.valueOf(newModel.getValue())));
            }

            if (model != null) {
                model.setExtent(0);
            }
            repaint();
        }
    }


    /* All of the model methods are implemented by delegation. */

    /**
     * Returns the progress bar's current {@code value}
     * from the <code>BoundedRangeModel</code>.
     * The value is always between the
     * minimum and maximum values, inclusive.
     *
     * @return  the current value of the progress bar
     * @see     #setValue
     * @see     BoundedRangeModel#getValue
     */
    public int getValue() { return getModel().getValue(); }

    /**
     * Returns the progress bar's {@code minimum} value
     * from the <code>BoundedRangeModel</code>.
     *
     * @return  the progress bar's minimum value
     * @see     #setMinimum
     * @see     BoundedRangeModel#getMinimum
     */
    public int getMinimum() { return getModel().getMinimum(); }

    /**
     * Returns the progress bar's {@code maximum} value
     * from the <code>BoundedRangeModel</code>.
     *
     * @return  the progress bar's maximum value
     * @see     #setMaximum
     * @see     BoundedRangeModel#getMaximum
     */
    public int getMaximum() { return getModel().getMaximum(); }

    /**
     * Sets the progress bar's current value to {@code n}.  This method
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
            = "The progress bar's current value.")
    public void setValue(int n) {
        BoundedRangeModel brm = getModel();
        int oldValue = brm.getValue();
        brm.setValue(n);

        if (accessibleContext != null) {
            accessibleContext.firePropertyChange(
                    AccessibleContext.ACCESSIBLE_VALUE_PROPERTY,
                    Integer.valueOf(oldValue),
                    Integer.valueOf(brm.getValue()));
        }
    }

    /**
     * Sets the progress bar's minimum value
     * (stored in the progress bar's data model) to <code>n</code>.
     * <p>
     * The data model (a <code>BoundedRangeModel</code> instance)
     * handles any mathematical
     * issues arising from assigning faulty values.
     * See the {@code BoundedRangeModel} documentation for details.
     * <p>
     * If the minimum value is different from the previous minimum,
     * all change listeners are notified.
     *
     * @param  n       the new minimum
     * @see    #getMinimum
     * @see    #addChangeListener
     * @see    BoundedRangeModel#setMinimum
     */
    @BeanProperty(bound = false, preferred = true, description
            = "The progress bar's minimum value.")
    public void setMinimum(int n) { getModel().setMinimum(n); }

    /**
     * Sets the progress bar's maximum value
     * (stored in the progress bar's data model) to <code>n</code>.
     * <p>
     * The underlying <code>BoundedRangeModel</code> handles any mathematical
     * issues arising from assigning faulty values.
     * See the {@code BoundedRangeModel} documentation for details.
     * <p>
     * If the maximum value is different from the previous maximum,
     * all change listeners are notified.
     *
     * @param  n       the new maximum
     * @see    #getMaximum
     * @see    #addChangeListener
     * @see    BoundedRangeModel#setMaximum
     */
    @BeanProperty(bound = false, preferred = true, description
            = "The progress bar's maximum value.")
    public void setMaximum(int n) { getModel().setMaximum(n); }

    /**
     * Sets the <code>indeterminate</code> property of the progress bar,
     * which determines whether the progress bar is in determinate
     * or indeterminate mode.
     * An indeterminate progress bar continuously displays animation
     * indicating that an operation of unknown length is occurring.
     * By default, this property is <code>false</code>.
     * Some look and feels might not support indeterminate progress bars;
     * they will ignore this property.
     *
     * <p>
     *
     * See
     * <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/progress.html" target="_top">How to Monitor Progress</a>
     * for examples of using indeterminate progress bars.
     *
     * @param newValue  <code>true</code> if the progress bar
     *                  should change to indeterminate mode;
     *                  <code>false</code> if it should revert to normal.
     *
     * @see #isIndeterminate
     * @see javax.swing.plaf.basic.BasicProgressBarUI
     *
     * @since 1.4
     */
    public void setIndeterminate(boolean newValue) {
        boolean oldValue = indeterminate;
        indeterminate = newValue;
        firePropertyChange("indeterminate", oldValue, indeterminate);
    }

    /**
     * Returns the value of the <code>indeterminate</code> property.
     *
     * @return the value of the <code>indeterminate</code> property
     * @see    #setIndeterminate
     *
     * @since 1.4
     */
    @BeanProperty(bound = false, description
            = "Is the progress bar indeterminate (true) or normal (false)?")
    public boolean isIndeterminate() {
        return indeterminate;
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
     * Returns a string representation of this <code>JProgressBar</code>.
     * This method is intended to be used only for debugging purposes. The
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JProgressBar</code>
     */
    protected String paramString() {
        String orientationString = (orientation == HORIZONTAL ?
                                    "HORIZONTAL" : "VERTICAL");
        String paintBorderString = (paintBorder ?
                                    "true" : "false");
        String progressStringString = (progressString != null ?
                                       progressString : "");
        String paintStringString = (paintString ?
                                    "true" : "false");
        String indeterminateString = (indeterminate ?
                                    "true" : "false");

        return super.paramString() +
        ",orientation=" + orientationString +
        ",paintBorder=" + paintBorderString +
        ",paintString=" + paintStringString +
        ",progressString=" + progressStringString +
        ",indeterminateString=" + indeterminateString;
    }

/////////////////
// Accessibility support
////////////////

    /**
     * Gets the <code>AccessibleContext</code> associated with this
     * <code>JProgressBar</code>. For progress bars, the
     * <code>AccessibleContext</code> takes the form of an
     * <code>AccessibleJProgressBar</code>.
     * A new <code>AccessibleJProgressBar</code> instance is created if necessary.
     *
     * @return an <code>AccessibleJProgressBar</code> that serves as the
     *         <code>AccessibleContext</code> of this <code>JProgressBar</code>
     */
    @BeanProperty(bound = false, expert = true, description
            = "The AccessibleContext associated with this ProgressBar.")
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJProgressBar();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JProgressBar</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to progress bar user-interface
     * elements.
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
    protected class AccessibleJProgressBar extends AccessibleJComponent
        implements AccessibleValue {

        /**
         * Constructs an {@code AccessibleJProgressBar}.
         */
        protected AccessibleJProgressBar() {}

        /**
         * Gets the state set of this object.
         *
         * @return an instance of AccessibleState containing the current state
         * of the object
         * @see AccessibleState
         */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = super.getAccessibleStateSet();
            if (getModel().getValueIsAdjusting()) {
                states.add(AccessibleState.BUSY);
            }
            if (getOrientation() == VERTICAL) {
                states.add(AccessibleState.VERTICAL);
            } else {
                states.add(AccessibleState.HORIZONTAL);
            }
            return states;
        }

        /**
         * Gets the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PROGRESS_BAR;
        }

        /**
         * Gets the <code>AccessibleValue</code> associated with this object.  In the
         * implementation of the Java Accessibility API for this class,
         * returns this object, which is responsible for implementing the
         * <code>AccessibleValue</code> interface on behalf of itself.
         *
         * @return this object
         */
        public AccessibleValue getAccessibleValue() {
            return this;
        }

        /**
         * Gets the accessible value of this object.
         *
         * @return the current value of this object
         */
        public Number getCurrentAccessibleValue() {
            return Integer.valueOf(getValue());
        }

        /**
         * Sets the value of this object as a <code>Number</code>.
         *
         * @return <code>true</code> if the value was set
         */
        public boolean setCurrentAccessibleValue(Number n) {
            // TIGER- 4422535
            if (n == null) {
                return false;
            }
            setValue(n.intValue());
            return true;
        }

        /**
         * Gets the minimum accessible value of this object.
         *
         * @return the minimum value of this object
         */
        public Number getMinimumAccessibleValue() {
            return Integer.valueOf(getMinimum());
        }

        /**
         * Gets the maximum accessible value of this object.
         *
         * @return the maximum value of this object
         */
        public Number getMaximumAccessibleValue() {
            // TIGER - 4422362
            return Integer.valueOf(model.getMaximum() - model.getExtent());
        }

    } // AccessibleJProgressBar
}
