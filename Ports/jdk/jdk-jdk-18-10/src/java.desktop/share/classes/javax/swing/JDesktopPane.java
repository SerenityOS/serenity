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
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.plaf.DesktopPaneUI;

/**
 * A container used to create a multiple-document interface or a virtual desktop.
 * You create <code>JInternalFrame</code> objects and add them to the
 * <code>JDesktopPane</code>. <code>JDesktopPane</code> extends
 * <code>JLayeredPane</code> to manage the potentially overlapping internal
 * frames. It also maintains a reference to an instance of
 * <code>DesktopManager</code> that is set by the UI
 * class for the current look and feel (L&amp;F).  Note that <code>JDesktopPane</code>
 * does not support borders.
 * <p>
 * This class is normally used as the parent of <code>JInternalFrames</code>
 * to provide a pluggable <code>DesktopManager</code> object to the
 * <code>JInternalFrames</code>. The <code>installUI</code> of the
 * L&amp;F specific implementation is responsible for setting the
 * <code>desktopManager</code> variable appropriately.
 * When the parent of a <code>JInternalFrame</code> is a <code>JDesktopPane</code>,
 * it should delegate most of its behavior to the <code>desktopManager</code>
 * (closing, resizing, etc).
 * <p>
 * For further documentation and examples see
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/internalframe.html">How to Use Internal Frames</a>,
 * a section in <em>The Java Tutorial</em>.
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
 * @see JInternalFrame
 * @see JInternalFrame.JDesktopIcon
 * @see DesktopManager
 *
 * @author David Kloba
 * @since 1.2
 */
@JavaBean(defaultProperty = "UI")
@SuppressWarnings("serial") // Same-version serialization only
public class JDesktopPane extends JLayeredPane implements Accessible
{
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "DesktopPaneUI";

    transient DesktopManager desktopManager;

    private transient JInternalFrame selectedFrame = null;

    /**
      * Indicates that the entire contents of the item being dragged
      * should appear inside the desktop pane.
      *
      * @see #OUTLINE_DRAG_MODE
      * @see #setDragMode
      */
    public static final int LIVE_DRAG_MODE = 0;

    /**
      * Indicates that an outline only of the item being dragged
      * should appear inside the desktop pane.
      *
      * @see #LIVE_DRAG_MODE
      * @see #setDragMode
      */
    public static final int OUTLINE_DRAG_MODE = 1;

    private int dragMode = LIVE_DRAG_MODE;
    private boolean dragModeSet = false;
    private transient List<JInternalFrame> framesCache;
    private boolean componentOrderCheckingEnabled = true;
    private boolean componentOrderChanged = false;

    /**
     * Creates a new <code>JDesktopPane</code>.
     */
    public JDesktopPane() {
        setUIProperty("opaque", Boolean.TRUE);
        setFocusCycleRoot(true);

        setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
            public Component getDefaultComponent(Container c) {
                JInternalFrame[] jifArray = getAllFrames();
                Component comp = null;
                for (JInternalFrame jif : jifArray) {
                    comp = jif.getFocusTraversalPolicy().getDefaultComponent(jif);
                    if (comp != null) {
                        break;
                    }
                }
                return comp;
            }
        });
        updateUI();
    }

    /**
     * Returns the L&amp;F object that renders this component.
     *
     * @return the <code>DesktopPaneUI</code> object that
     *   renders this component
     */
    public DesktopPaneUI getUI() {
        return (DesktopPaneUI)ui;
    }

    /**
     * Sets the L&amp;F object that renders this component.
     *
     * @param ui  the DesktopPaneUI L&amp;F object
     * @see UIDefaults#getUI
     */
    @BeanProperty(hidden = true, visualUpdate = true, description
            = "The UI object that implements the Component's LookAndFeel.")
    public void setUI(DesktopPaneUI ui) {
        super.setUI(ui);
    }

    /**
     * Sets the "dragging style" used by the desktop pane.
     * You may want to change to one mode or another for
     * performance or aesthetic reasons.
     *
     * @param dragMode the style of drag to use for items in the Desktop
     *
     * @see #LIVE_DRAG_MODE
     * @see #OUTLINE_DRAG_MODE
     *
     * @since 1.3
     */
    @BeanProperty(enumerationValues = {
            "JDesktopPane.LIVE_DRAG_MODE",
            "JDesktopPane.OUTLINE_DRAG_MODE"}, description
            = "Dragging style for internal frame children.")
    public void setDragMode(int dragMode) {
        int oldDragMode = this.dragMode;
        this.dragMode = dragMode;
        firePropertyChange("dragMode", oldDragMode, this.dragMode);
        dragModeSet = true;
     }

    /**
     * Gets the current "dragging style" used by the desktop pane.
     * @return either <code>Live_DRAG_MODE</code> or
     *   <code>OUTLINE_DRAG_MODE</code>
     * @see #setDragMode
     * @since 1.3
     */
     public int getDragMode() {
         return dragMode;
     }

    /**
     * Returns the {@code DesktopManger} that handles
     * desktop-specific UI actions.
     *
     * @return the {@code DesktopManger} that handles desktop-specific
     *         UI actions
     */
    public DesktopManager getDesktopManager() {
        return desktopManager;
    }

    /**
     * Sets the <code>DesktopManger</code> that will handle
     * desktop-specific UI actions. This may be overridden by
     * {@code LookAndFeel}.
     *
     * @param d the <code>DesktopManager</code> to use
     */
    @BeanProperty(description
            = "Desktop manager to handle the internal frames in the desktop pane.")
    public void setDesktopManager(DesktopManager d) {
        DesktopManager oldValue = desktopManager;
        desktopManager = d;
        firePropertyChange("desktopManager", oldValue, desktopManager);
    }

    /**
     * Notification from the <code>UIManager</code> that the L&amp;F has changed.
     * Replaces the current UI object with the latest version from the
     * <code>UIManager</code>.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        setUI((DesktopPaneUI)UIManager.getUI(this));
    }


    /**
     * Returns the name of the L&amp;F class that renders this component.
     *
     * @return the string "DesktopPaneUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
    }

    /**
     * Returns all <code>JInternalFrames</code> currently displayed in the
     * desktop. Returns iconified frames as well as expanded frames.
     *
     * @return an array of <code>JInternalFrame</code> objects
     */
    @BeanProperty(bound = false)
    public JInternalFrame[] getAllFrames() {
        return getAllFrames(this).toArray(new JInternalFrame[0]);
    }

    private static Collection<JInternalFrame> getAllFrames(Container parent) {
        int i, count;
        Collection<JInternalFrame> results = new LinkedHashSet<>();
        count = parent.getComponentCount();
        for (i = 0; i < count; i++) {
            Component next = parent.getComponent(i);
            if (next instanceof JInternalFrame) {
                results.add((JInternalFrame) next);
            } else if (next instanceof JInternalFrame.JDesktopIcon) {
                JInternalFrame tmp = ((JInternalFrame.JDesktopIcon) next).getInternalFrame();
                if (tmp != null) {
                    results.add(tmp);
                }
            } else if (next instanceof Container) {
                results.addAll(getAllFrames((Container) next));
            }
        }
        return results;
    }

    /** Returns the currently active <code>JInternalFrame</code>
      * in this <code>JDesktopPane</code>, or <code>null</code>
      * if no <code>JInternalFrame</code> is currently active.
      *
      * @return the currently active <code>JInternalFrame</code> or
      *   <code>null</code>
      * @since 1.3
      */

    public JInternalFrame getSelectedFrame() {
      return selectedFrame;
    }

    /** Sets the currently active <code>JInternalFrame</code>
     *  in this <code>JDesktopPane</code>. This method is used to bridge
     *  the package gap between JDesktopPane and the platform implementation
     *  code and should not be called directly. To visually select the frame
     *  the client must call JInternalFrame.setSelected(true) to activate
     *  the frame.
     *  @see JInternalFrame#setSelected(boolean)
     *
     * @param f the internal frame that's currently selected
     * @since 1.3
     */

    public void setSelectedFrame(JInternalFrame f) {
      selectedFrame = f;
    }

    /**
     * Returns all <code>JInternalFrames</code> currently displayed in the
     * specified layer of the desktop. Returns iconified frames as well
     * expanded frames.
     *
     * @param layer  an int specifying the desktop layer
     * @return an array of <code>JInternalFrame</code> objects
     * @see JLayeredPane
     */
    public JInternalFrame[] getAllFramesInLayer(int layer) {
        Collection<JInternalFrame> allFrames = getAllFrames(this);
        Iterator<JInternalFrame> iterator = allFrames.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getLayer() != layer) {
                iterator.remove();
            }
        }
        return allFrames.toArray(new JInternalFrame[0]);
    }

    private List<JInternalFrame> getFrames() {
        Component c;
        Set<ComponentPosition> set = new TreeSet<ComponentPosition>();
        for (int i = 0; i < getComponentCount(); i++) {
            c = getComponent(i);
            if (c instanceof JInternalFrame) {
                set.add(new ComponentPosition((JInternalFrame)c, getLayer(c),
                    i));
            }
            else if (c instanceof JInternalFrame.JDesktopIcon)  {
                c = ((JInternalFrame.JDesktopIcon)c).getInternalFrame();
                set.add(new ComponentPosition((JInternalFrame)c, getLayer(c),
                    i));
            }
        }
        List<JInternalFrame> frames = new ArrayList<JInternalFrame>(
                set.size());
        for (ComponentPosition position : set) {
            frames.add(position.component);
        }
        return frames;
   }

    private static class ComponentPosition implements
        Comparable<ComponentPosition> {
        private final JInternalFrame component;
        private final int layer;
        private final int zOrder;

        ComponentPosition(JInternalFrame component, int layer, int zOrder) {
            this.component = component;
            this.layer = layer;
            this.zOrder = zOrder;
        }

        public int compareTo(ComponentPosition o) {
            int delta = o.layer - layer;
            if (delta == 0) {
                return zOrder - o.zOrder;
            }
            return delta;
        }
    }

    private JInternalFrame getNextFrame(JInternalFrame f, boolean forward) {
        verifyFramesCache();
        if (f == null) {
            return getTopInternalFrame();
        }
        int i = framesCache.indexOf(f);
        if (i == -1 || framesCache.size() == 1) {
            /* error */
            return null;
        }
        if (forward) {
            // navigate to the next frame
            if (++i == framesCache.size()) {
                /* wrap */
                i = 0;
            }
        }
        else {
            // navigate to the previous frame
            if (--i == -1) {
                /* wrap */
                i = framesCache.size() - 1;
            }
        }
        return framesCache.get(i);
    }

    JInternalFrame getNextFrame(JInternalFrame f) {
        return getNextFrame(f, true);
    }

    private JInternalFrame getTopInternalFrame() {
        if (framesCache.size() == 0) {
            return null;
        }
        return framesCache.get(0);
    }

    private void updateFramesCache() {
        framesCache = getFrames();
    }

    private void verifyFramesCache() {
        // If framesCache is dirty, then recreate it.
        if (componentOrderChanged) {
            componentOrderChanged = false;
            updateFramesCache();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Component comp) {
        super.remove(comp);
        updateFramesCache();
    }

    /**
     * Selects the next <code>JInternalFrame</code> in this desktop pane.
     *
     * @param forward a boolean indicating which direction to select in;
     *        <code>true</code> for forward, <code>false</code> for
     *        backward
     * @return the JInternalFrame that was selected or <code>null</code>
     *         if nothing was selected
     * @since 1.6
     */
    public JInternalFrame selectFrame(boolean forward) {
        JInternalFrame selectedFrame = getSelectedFrame();
        JInternalFrame frameToSelect = getNextFrame(selectedFrame, forward);
        if (frameToSelect == null) {
            return null;
        }
        // Maintain navigation traversal order until an
        // external stack change, such as a click on a frame.
        setComponentOrderCheckingEnabled(false);
        if (forward && selectedFrame != null) {
            selectedFrame.moveToBack();  // For Windows MDI fidelity.
        }
        try { frameToSelect.setSelected(true);
        } catch (PropertyVetoException pve) {}
        setComponentOrderCheckingEnabled(true);
        return frameToSelect;
    }

    /*
     * Sets whether component order checking is enabled.
     * @param enable a boolean value, where <code>true</code> means
     * a change in component order will cause a change in the keyboard
     * navigation order.
     * @since 1.6
     */
    void setComponentOrderCheckingEnabled(boolean enable) {
        componentOrderCheckingEnabled = enable;
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        checkComponentAttributes(comp);
        super.addImpl(comp, constraints, index);
        if (componentOrderCheckingEnabled) {
            if (comp instanceof JInternalFrame ||
                comp instanceof JInternalFrame.JDesktopIcon) {
                componentOrderChanged = true;
            }
        }
    }

    private void checkComponentAttributes(Component comp) {
        if (comp instanceof JInternalFrame && ((JInternalFrame) comp).isIcon()) {
            ((JInternalFrame) comp).putClientProperty("wasIconOnce", Boolean.FALSE);
        }
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    public void remove(int index) {
        if (componentOrderCheckingEnabled) {
            Component comp = getComponent(index);
            if (comp instanceof JInternalFrame ||
                comp instanceof JInternalFrame.JDesktopIcon) {
                componentOrderChanged = true;
            }
        }
        super.remove(index);
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    public void removeAll() {
        if (componentOrderCheckingEnabled) {
            int count = getComponentCount();
            for (int i = 0; i < count; i++) {
                Component comp = getComponent(i);
                if (comp instanceof JInternalFrame ||
                    comp instanceof JInternalFrame.JDesktopIcon) {
                    componentOrderChanged = true;
                    break;
                }
            }
        }
        super.removeAll();
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    public void setComponentZOrder(Component comp, int index) {
        super.setComponentZOrder(comp, index);
        if (componentOrderCheckingEnabled) {
            if (comp instanceof JInternalFrame ||
                comp instanceof JInternalFrame.JDesktopIcon) {
                componentOrderChanged = true;
            }
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

    void setUIProperty(String propertyName, Object value) {
        if (propertyName == "dragMode") {
            if (!dragModeSet) {
                setDragMode(((Integer)value).intValue());
                dragModeSet = false;
            }
        } else {
            super.setUIProperty(propertyName, value);
        }
    }

    /**
     * Returns a string representation of this <code>JDesktopPane</code>.
     * This method is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JDesktopPane</code>
     */
    protected String paramString() {
        String desktopManagerString = (desktopManager != null ?
                                       desktopManager.toString() : "");

        return super.paramString() +
        ",desktopManager=" + desktopManagerString;
    }

/////////////////
// Accessibility support
////////////////

    /**
     * Gets the <code>AccessibleContext</code> associated with this
     * <code>JDesktopPane</code>. For desktop panes, the
     * <code>AccessibleContext</code> takes the form of an
     * <code>AccessibleJDesktopPane</code>.
     * A new <code>AccessibleJDesktopPane</code> instance is created if necessary.
     *
     * @return an <code>AccessibleJDesktopPane</code> that serves as the
     *         <code>AccessibleContext</code> of this <code>JDesktopPane</code>
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJDesktopPane();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JDesktopPane</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to desktop pane user-interface
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
    protected class AccessibleJDesktopPane extends AccessibleJComponent {

        /**
         * Constructs an {@code AccessibleJDesktopPane}.
         */
        protected AccessibleJDesktopPane() {}

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.DESKTOP_PANE;
        }
    }
}
