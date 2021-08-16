/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serial;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A {@code CardLayout} object is a layout manager for a
 * container. It treats each component in the container as a card.
 * Only one card is visible at a time, and the container acts as
 * a stack of cards. The first component added to a
 * {@code CardLayout} object is the visible component when the
 * container is first displayed.
 * <p>
 * The ordering of cards is determined by the container's own internal
 * ordering of its component objects. {@code CardLayout}
 * defines a set of methods that allow an application to flip
 * through these cards sequentially, or to show a specified card.
 * The {@link CardLayout#addLayoutComponent}
 * method can be used to associate a string identifier with a given card
 * for fast random access.
 *
 * @author      Arthur van Hoff
 * @see         java.awt.Container
 * @since       1.0
 */

public class CardLayout implements LayoutManager2,
                                   Serializable {
    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -4328196481005934313L;

    /*
     * This creates a Vector to store associated
     * pairs of components and their names.
     * @see java.util.Vector
     */
    Vector<Card> vector = new Vector<>();

    /**
     * A pair of component and string that represents its name.
     */
    class Card implements Serializable {

        /**
         * Use serialVersionUID from JDK 1.4 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 6640330810709497518L;
        public String name;
        public Component comp;
        public Card(String cardName, Component cardComponent) {
            name = cardName;
            comp = cardComponent;
        }
    }

    /*
     * Index of Component currently displayed by CardLayout.
     */
    int currentCard = 0;


    /*
    * A cards horizontal Layout gap (inset). It specifies
    * the space between the left and right edges of a
    * container and the current component.
    * This should be a non negative Integer.
    * @see getHgap()
    * @see setHgap()
    */
    int hgap;

    /*
    * A cards vertical Layout gap (inset). It specifies
    * the space between the top and bottom edges of a
    * container and the current component.
    * This should be a non negative Integer.
    * @see getVgap()
    * @see setVgap()
    */
    int vgap;

    /**
     * @serialField tab         Hashtable
     *      deprecated, for forward compatibility only
     * @serialField hgap        int the horizontal Layout gap
     * @serialField vgap        int the vertical Layout gap
     * @serialField vector      Vector the pairs of components and their names
     * @serialField currentCard int the index of Component currently displayed
     */
    @Serial
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("tab", Hashtable.class),
        new ObjectStreamField("hgap", Integer.TYPE),
        new ObjectStreamField("vgap", Integer.TYPE),
        new ObjectStreamField("vector", Vector.class),
        new ObjectStreamField("currentCard", Integer.TYPE)
    };

    /**
     * Creates a new card layout with gaps of size zero.
     */
    public CardLayout() {
        this(0, 0);
    }

    /**
     * Creates a new card layout with the specified horizontal and
     * vertical gaps. The horizontal gaps are placed at the left and
     * right edges. The vertical gaps are placed at the top and bottom
     * edges.
     * @param     hgap   the horizontal gap.
     * @param     vgap   the vertical gap.
     */
    public CardLayout(int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
    }

    /**
     * Gets the horizontal gap between components.
     * @return    the horizontal gap between components.
     * @see       java.awt.CardLayout#setHgap(int)
     * @see       java.awt.CardLayout#getVgap()
     * @since     1.1
     */
    public int getHgap() {
        return hgap;
    }

    /**
     * Sets the horizontal gap between components.
     * @param hgap the horizontal gap between components.
     * @see       java.awt.CardLayout#getHgap()
     * @see       java.awt.CardLayout#setVgap(int)
     * @since     1.1
     */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /**
     * Gets the vertical gap between components.
     * @return the vertical gap between components.
     * @see       java.awt.CardLayout#setVgap(int)
     * @see       java.awt.CardLayout#getHgap()
     */
    public int getVgap() {
        return vgap;
    }

    /**
     * Sets the vertical gap between components.
     * @param     vgap the vertical gap between components.
     * @see       java.awt.CardLayout#getVgap()
     * @see       java.awt.CardLayout#setHgap(int)
     * @since     1.1
     */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /**
     * Adds the specified component to this card layout's internal
     * table of names. The object specified by {@code constraints}
     * must be a string. The card layout stores this string as a key-value
     * pair that can be used for random access to a particular card.
     * By calling the {@code show} method, an application can
     * display the component with the specified name.
     * @param     comp          the component to be added.
     * @param     constraints   a tag that identifies a particular
     *                                        card in the layout.
     * @see       java.awt.CardLayout#show(java.awt.Container, java.lang.String)
     * @exception  IllegalArgumentException  if the constraint is not a string.
     */
    public void addLayoutComponent(Component comp, Object constraints) {
      synchronized (comp.getTreeLock()) {
          if (constraints == null){
              constraints = "";
          }
        if (constraints instanceof String) {
            addLayoutComponent((String)constraints, comp);
        } else {
            throw new IllegalArgumentException("cannot add to layout: constraint must be a string");
        }
      }
    }

    /**
     * @deprecated   replaced by
     *      {@code addLayoutComponent(Component, Object)}.
     */
    @Deprecated
    public void addLayoutComponent(String name, Component comp) {
        synchronized (comp.getTreeLock()) {
            if (!vector.isEmpty()) {
                comp.setVisible(false);
            }
            for (int i=0; i < vector.size(); i++) {
                if ((vector.get(i)).name.equals(name)) {
                    (vector.get(i)).comp = comp;
                    return;
                }
            }
            vector.add(new Card(name, comp));
        }
    }

    /**
     * Removes the specified component from the layout.
     * If the card was visible on top, the next card underneath it is shown.
     * @param   comp   the component to be removed.
     * @see     java.awt.Container#remove(java.awt.Component)
     * @see     java.awt.Container#removeAll()
     */
    public void removeLayoutComponent(Component comp) {
        synchronized (comp.getTreeLock()) {
            for (int i = 0; i < vector.size(); i++) {
                if ((vector.get(i)).comp == comp) {
                    // if we remove current component we should show next one
                    if (comp.isVisible() && (comp.getParent() != null)) {
                        next(comp.getParent());
                    }

                    vector.remove(i);

                    // correct currentCard if this is necessary
                    if (currentCard > i) {
                        currentCard--;
                    }
                    break;
                }
            }
        }
    }

    /**
     * Determines the preferred size of the container argument using
     * this card layout.
     * @param   parent the parent container in which to do the layout
     * @return  the preferred dimensions to lay out the subcomponents
     *                of the specified container
     * @see     java.awt.Container#getPreferredSize
     * @see     java.awt.CardLayout#minimumLayoutSize
     */
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int ncomponents = parent.getComponentCount();
            int w = 0;
            int h = 0;

            for (int i = 0 ; i < ncomponents ; i++) {
                Component comp = parent.getComponent(i);
                Dimension d = comp.getPreferredSize();
                if (d.width > w) {
                    w = d.width;
                }
                if (d.height > h) {
                    h = d.height;
                }
            }
            return new Dimension(insets.left + insets.right + w + hgap*2,
                                 insets.top + insets.bottom + h + vgap*2);
        }
    }

    /**
     * Calculates the minimum size for the specified panel.
     * @param     parent the parent container in which to do the layout
     * @return    the minimum dimensions required to lay out the
     *                subcomponents of the specified container
     * @see       java.awt.Container#doLayout
     * @see       java.awt.CardLayout#preferredLayoutSize
     */
    public Dimension minimumLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int ncomponents = parent.getComponentCount();
            int w = 0;
            int h = 0;

            for (int i = 0 ; i < ncomponents ; i++) {
                Component comp = parent.getComponent(i);
                Dimension d = comp.getMinimumSize();
                if (d.width > w) {
                    w = d.width;
                }
                if (d.height > h) {
                    h = d.height;
                }
            }
            return new Dimension(insets.left + insets.right + w + hgap*2,
                                 insets.top + insets.bottom + h + vgap*2);
        }
    }

    /**
     * Returns the maximum dimensions for this layout given the components
     * in the specified target container.
     * @param target the component which needs to be laid out
     * @see Container
     * @see #minimumLayoutSize
     * @see #preferredLayoutSize
     */
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Returns the alignment along the x axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     */
    public float getLayoutAlignmentX(Container parent) {
        return 0.5f;
    }

    /**
     * Returns the alignment along the y axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     */
    public float getLayoutAlignmentY(Container parent) {
        return 0.5f;
    }

    /**
     * Invalidates the layout, indicating that if the layout manager
     * has cached information it should be discarded.
     */
    public void invalidateLayout(Container target) {
    }

    /**
     * Lays out the specified container using this card layout.
     * <p>
     * Each component in the {@code parent} container is reshaped
     * to be the size of the container, minus space for surrounding
     * insets, horizontal gaps, and vertical gaps.
     *
     * @param     parent the parent container in which to do the layout
     * @see       java.awt.Container#doLayout
     */
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int ncomponents = parent.getComponentCount();
            Component comp = null;
            boolean currentFound = false;

            for (int i = 0 ; i < ncomponents ; i++) {
                comp = parent.getComponent(i);
                comp.setBounds(hgap + insets.left, vgap + insets.top,
                               parent.width - (hgap*2 + insets.left + insets.right),
                               parent.height - (vgap*2 + insets.top + insets.bottom));
                if (comp.isVisible()) {
                    currentFound = true;
                }
            }

            if (!currentFound && ncomponents > 0) {
                parent.getComponent(0).setVisible(true);
            }
        }
    }

    /**
     * Make sure that the Container really has a CardLayout installed.
     * Otherwise havoc can ensue!
     */
    void checkLayout(Container parent) {
        if (parent.getLayout() != this) {
            throw new IllegalArgumentException("wrong parent for CardLayout");
        }
    }

    /**
     * Flips to the first card of the container.
     * @param     parent   the parent container in which to do the layout
     * @see       java.awt.CardLayout#last
     */
    public void first(Container parent) {
        synchronized (parent.getTreeLock()) {
            checkLayout(parent);
            int ncomponents = parent.getComponentCount();
            for (int i = 0 ; i < ncomponents ; i++) {
                Component comp = parent.getComponent(i);
                if (comp.isVisible()) {
                    comp.setVisible(false);
                    break;
                }
            }
            if (ncomponents > 0) {
                currentCard = 0;
                parent.getComponent(0).setVisible(true);
                parent.validate();
            }
        }
    }

    /**
     * Flips to the next card of the specified container. If the
     * currently visible card is the last one, this method flips to the
     * first card in the layout.
     * @param     parent   the parent container in which to do the layout
     * @see       java.awt.CardLayout#previous
     */
    public void next(Container parent) {
        synchronized (parent.getTreeLock()) {
            checkLayout(parent);
            int ncomponents = parent.getComponentCount();
            for (int i = 0 ; i < ncomponents ; i++) {
                Component comp = parent.getComponent(i);
                if (comp.isVisible()) {
                    comp.setVisible(false);
                    currentCard = (i + 1) % ncomponents;
                    comp = parent.getComponent(currentCard);
                    comp.setVisible(true);
                    parent.validate();
                    return;
                }
            }
            showDefaultComponent(parent);
        }
    }

    /**
     * Flips to the previous card of the specified container. If the
     * currently visible card is the first one, this method flips to the
     * last card in the layout.
     * @param     parent   the parent container in which to do the layout
     * @see       java.awt.CardLayout#next
     */
    public void previous(Container parent) {
        synchronized (parent.getTreeLock()) {
            checkLayout(parent);
            int ncomponents = parent.getComponentCount();
            for (int i = 0 ; i < ncomponents ; i++) {
                Component comp = parent.getComponent(i);
                if (comp.isVisible()) {
                    comp.setVisible(false);
                    currentCard = ((i > 0) ? i-1 : ncomponents-1);
                    comp = parent.getComponent(currentCard);
                    comp.setVisible(true);
                    parent.validate();
                    return;
                }
            }
            showDefaultComponent(parent);
        }
    }

    void showDefaultComponent(Container parent) {
        if (parent.getComponentCount() > 0) {
            currentCard = 0;
            parent.getComponent(0).setVisible(true);
            parent.validate();
        }
    }

    /**
     * Flips to the last card of the container.
     * @param     parent   the parent container in which to do the layout
     * @see       java.awt.CardLayout#first
     */
    public void last(Container parent) {
        synchronized (parent.getTreeLock()) {
            checkLayout(parent);
            int ncomponents = parent.getComponentCount();
            for (int i = 0 ; i < ncomponents ; i++) {
                Component comp = parent.getComponent(i);
                if (comp.isVisible()) {
                    comp.setVisible(false);
                    break;
                }
            }
            if (ncomponents > 0) {
                currentCard = ncomponents - 1;
                parent.getComponent(currentCard).setVisible(true);
                parent.validate();
            }
        }
    }

    /**
     * Flips to the component that was added to this layout with the
     * specified {@code name}, using {@code addLayoutComponent}.
     * If no such component exists, then nothing happens.
     * @param     parent   the parent container in which to do the layout
     * @param     name     the component name
     * @see       java.awt.CardLayout#addLayoutComponent(java.awt.Component, java.lang.Object)
     */
    public void show(Container parent, String name) {
        synchronized (parent.getTreeLock()) {
            checkLayout(parent);
            Component next = null;
            int ncomponents = vector.size();
            for (int i = 0; i < ncomponents; i++) {
                Card card = vector.get(i);
                if (card.name.equals(name)) {
                    next = card.comp;
                    currentCard = i;
                    break;
                }
            }
            if ((next != null) && !next.isVisible()) {
                ncomponents = parent.getComponentCount();
                for (int i = 0; i < ncomponents; i++) {
                    Component comp = parent.getComponent(i);
                    if (comp.isVisible()) {
                        comp.setVisible(false);
                        break;
                    }
                }
                next.setVisible(true);
                parent.validate();
            }
        }
    }

    /**
     * Returns a string representation of the state of this card layout.
     * @return    a string representation of this card layout.
     */
    public String toString() {
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + "]";
    }

    /**
     * Reads serializable fields from stream.
     *
     * @param  s the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws IOException if an I/O error occurs
     */
    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream s)
        throws ClassNotFoundException, IOException
    {
        ObjectInputStream.GetField f = s.readFields();

        hgap = f.get("hgap", 0);
        vgap = f.get("vgap", 0);

        if (f.defaulted("vector")) {
            //  pre-1.4 stream
            Hashtable<String, Component> tab = (Hashtable)f.get("tab", null);
            vector = new Vector<>();
            if (tab != null && !tab.isEmpty()) {
                for (Enumeration<String> e = tab.keys() ; e.hasMoreElements() ; ) {
                    String key = e.nextElement();
                    Component comp = tab.get(key);
                    vector.add(new Card(key, comp));
                    if (comp.isVisible()) {
                        currentCard = vector.size() - 1;
                    }
                }
            }
        } else {
            vector = (Vector)f.get("vector", null);
            currentCard = f.get("currentCard", 0);
        }
    }

    /**
     * Writes serializable fields to stream.
     *
     * @param  s the {@code ObjectOutputStream} to write
     * @throws IOException if an I/O error occurs
     */
    @Serial
    private void writeObject(ObjectOutputStream s)
        throws IOException
    {
        Hashtable<String, Component> tab = new Hashtable<>();
        int ncomponents = vector.size();
        for (int i = 0; i < ncomponents; i++) {
            Card card = vector.get(i);
            tab.put(card.name, card.comp);
        }

        ObjectOutputStream.PutField f = s.putFields();
        f.put("hgap", hgap);
        f.put("vgap", vgap);
        f.put("vector", vector);
        f.put("currentCard", currentCard);
        f.put("tab", tab);
        s.writeFields();
    }
}
