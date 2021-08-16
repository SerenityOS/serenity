/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text;

import java.util.Vector;
import java.awt.*;
import javax.swing.event.*;

/**
 * ZoneView is a View implementation that creates zones for which
 * the child views are not created or stored until they are needed
 * for display or model/view translations.  This enables a substantial
 * reduction in memory consumption for situations where the model
 * being represented is very large, by building view objects only for
 * the region being actively viewed/edited.  The size of the children
 * can be estimated in some way, or calculated asynchronously with
 * only the result being saved.
 * <p>
 * ZoneView extends BoxView to provide a box that implements
 * zones for its children.  The zones are special View implementations
 * (the children of an instance of this class) that represent only a
 * portion of the model that an instance of ZoneView is responsible
 * for.  The zones don't create child views until an attempt is made
 * to display them. A box shaped view is well suited to this because:
 *   <ul>
 *   <li>
 *   Boxes are a heavily used view, and having a box that
 *   provides this behavior gives substantial opportunity
 *   to plug the behavior into a view hierarchy from the
 *   view factory.
 *   <li>
 *   Boxes are tiled in one direction, so it is easy to
 *   divide them into zones in a reliable way.
 *   <li>
 *   Boxes typically have a simple relationship to the model (i.e. they
 *   create child views that directly represent the child elements).
 *   <li>
 *   Boxes are easier to estimate the size of than some other shapes.
 *   </ul>
 * <p>
 * The default behavior is controlled by two properties, maxZoneSize
 * and maxZonesLoaded.  Setting maxZoneSize to Integer.MAX_VALUE would
 * have the effect of causing only one zone to be created.  This would
 * effectively turn the view into an implementation of the decorator
 * pattern.  Setting maxZonesLoaded to a value of Integer.MAX_VALUE would
 * cause zones to never be unloaded.  For simplicity, zones are created on
 * boundaries represented by the child elements of the element the view is
 * responsible for.  The zones can be any View implementation, but the
 * default implementation is based upon AsyncBoxView which supports fairly
 * large zones efficiently.
 *
 * @author  Timothy Prinzing
 * @see     View
 * @since   1.3
 */
public class ZoneView extends BoxView {

    int maxZoneSize = 8 * 1024;
    int maxZonesLoaded = 3;
    Vector<View> loadedZones;

    /**
     * Constructs a ZoneView.
     *
     * @param elem the element this view is responsible for
     * @param axis either View.X_AXIS or View.Y_AXIS
     */
    public ZoneView(Element elem, int axis) {
        super(elem, axis);
        loadedZones = new Vector<View>();
    }

    /**
     * Get the current maximum zone size.
     * @return the current maximum zone size
     */
    public int getMaximumZoneSize() {
        return maxZoneSize;
    }

    /**
     * Set the desired maximum zone size.  A
     * zone may get larger than this size if
     * a single child view is larger than this
     * size since zones are formed on child view
     * boundaries.
     *
     * @param size the number of characters the zone
     * may represent before attempting to break
     * the zone into a smaller size.
     */
    public void setMaximumZoneSize(int size) {
        maxZoneSize = size;
    }

    /**
     * Get the current setting of the number of zones
     * allowed to be loaded at the same time.
     * @return current setting of the number of zones
     * allowed to be loaded at the same time
     */
    public int getMaxZonesLoaded() {
        return maxZonesLoaded;
    }

    /**
     * Sets the current setting of the number of zones
     * allowed to be loaded at the same time. This will throw an
     * <code>IllegalArgumentException</code> if <code>mzl</code> is less
     * than 1.
     *
     * @param mzl the desired maximum number of zones
     *  to be actively loaded, must be greater than 0
     * @exception IllegalArgumentException if <code>mzl</code> is &lt; 1
     */
    public void setMaxZonesLoaded(int mzl) {
        if (mzl < 1) {
            throw new IllegalArgumentException("ZoneView.setMaxZonesLoaded must be greater than 0.");
        }
        maxZonesLoaded = mzl;
        unloadOldZones();
    }

    /**
     * Called by a zone when it gets loaded.  This happens when
     * an attempt is made to display or perform a model/view
     * translation on a zone that was in an unloaded state.
     * This is implemented to check if the maximum number of
     * zones was reached and to unload the oldest zone if so.
     *
     * @param zone the child view that was just loaded.
     */
    protected void zoneWasLoaded(View zone) {
        //System.out.println("loading: " + zone.getStartOffset() + "," + zone.getEndOffset());
        loadedZones.addElement(zone);
        unloadOldZones();
    }

    void unloadOldZones() {
        while (loadedZones.size() > getMaxZonesLoaded()) {
            View zone = loadedZones.elementAt(0);
            loadedZones.removeElementAt(0);
            unloadZone(zone);
        }
    }

    /**
     * Unload a zone (Convert the zone to its memory saving state).
     * The zones are expected to represent a subset of the
     * child elements of the element this view is responsible for.
     * Therefore, the default implementation is to simple remove
     * all the children.
     *
     * @param zone the child view desired to be set to an
     *  unloaded state.
     */
    protected void unloadZone(View zone) {
        //System.out.println("unloading: " + zone.getStartOffset() + "," + zone.getEndOffset());
        zone.removeAll();
    }

    /**
     * Determine if a zone is in the loaded state.
     * The zones are expected to represent a subset of the
     * child elements of the element this view is responsible for.
     * Therefore, the default implementation is to return
     * true if the view has children.
     * param zone the child view
     * @param zone the zone
     * @return whether or not the zone is in the loaded state.
     */
    protected boolean isZoneLoaded(View zone) {
        return (zone.getViewCount() > 0);
    }

    /**
     * Create a view to represent a zone for the given
     * range within the model (which should be within
     * the range of this objects responsibility).  This
     * is called by the zone management logic to create
     * new zones.  Subclasses can provide a different
     * implementation for a zone by changing this method.
     *
     * @param p0 the start of the desired zone.  This should
     *  be &gt;= getStartOffset() and &lt; getEndOffset().  This
     *  value should also be &lt; p1.
     * @param p1 the end of the desired zone.  This should
     *  be &gt; getStartOffset() and &lt;= getEndOffset().  This
     *  value should also be &gt; p0.
     * @return a view to represent a zone for the given range within
     * the model
     */
    protected View createZone(int p0, int p1) {
        Document doc = getDocument();
        View zone;
        try {
            zone = new Zone(getElement(),
                            doc.createPosition(p0),
                            doc.createPosition(p1));
        } catch (BadLocationException ble) {
            // this should puke in some way.
            throw new StateInvariantError(ble.getMessage());
        }
        return zone;
    }

    /**
     * Loads all of the children to initialize the view.
     * This is called by the <code>setParent</code> method.
     * This is reimplemented to not load any children directly
     * (as they are created by the zones).  This method creates
     * the initial set of zones.  Zones don't actually get
     * populated however until an attempt is made to display
     * them or to do model/view coordinate translation.
     *
     * @param f the view factory
     */
    protected void loadChildren(ViewFactory f) {
        // build the first zone.
        Document doc = getDocument();
        int offs0 = getStartOffset();
        int offs1 = getEndOffset();
        append(createZone(offs0, offs1));
        handleInsert(offs0, offs1 - offs0);
    }

    /**
     * Returns the child view index representing the given position in
     * the model.
     *
     * @param pos the position &gt;= 0
     * @return  index of the view representing the given position, or
     *   -1 if no view represents that position
     */
    protected int getViewIndexAtPosition(int pos) {
        // PENDING(prinz) this could be done as a binary
        // search, and probably should be.
        int n = getViewCount();
        if (pos == getEndOffset()) {
            return n - 1;
        }
        for(int i = 0; i < n; i++) {
            View v = getView(i);
            if(pos >= v.getStartOffset() &&
               pos < v.getEndOffset()) {
                return i;
            }
        }
        return -1;
    }

    void handleInsert(int pos, int length) {
        int index = getViewIndex(pos, Position.Bias.Forward);
        View v = getView(index);
        int offs0 = v.getStartOffset();
        int offs1 = v.getEndOffset();
        if ((offs1 - offs0) > maxZoneSize) {
            splitZone(index, offs0, offs1);
        }
    }

    void handleRemove(int pos, int length) {
        // IMPLEMENT
    }

    /**
     * Break up the zone at the given index into pieces
     * of an acceptable size.
     */
    void splitZone(int index, int offs0, int offs1) {
        // divide the old zone into a new set of bins
        Element elem = getElement();
        Document doc = elem.getDocument();
        Vector<View> zones = new Vector<View>();
        int offs = offs0;
        do {
            offs0 = offs;
            offs = Math.min(getDesiredZoneEnd(offs0), offs1);
            zones.addElement(createZone(offs0, offs));
        } while (offs < offs1);
        View oldZone = getView(index);
        View[] newZones = new View[zones.size()];
        zones.copyInto(newZones);
        replace(index, 1, newZones);
    }

    /**
     * Returns the zone position to use for the
     * end of a zone that starts at the given
     * position.  By default this returns something
     * close to half the max zone size.
     */
    int getDesiredZoneEnd(int pos) {
        Element elem = getElement();
        int index = elem.getElementIndex(pos + (maxZoneSize / 2));
        Element child = elem.getElement(index);
        int offs0 = child.getStartOffset();
        int offs1 = child.getEndOffset();
        if ((offs1 - pos) > maxZoneSize) {
            if (offs0 > pos) {
                return offs0;
            }
        }
        return offs1;
    }

    // ---- View methods ----------------------------------------------------

    /**
     * The superclass behavior will try to update the child views
     * which is not desired in this case, since the children are
     * zones and not directly effected by the changes to the
     * associated element.  This is reimplemented to do nothing
     * and return false.
     */
    protected boolean updateChildren(DocumentEvent.ElementChange ec,
                                     DocumentEvent e, ViewFactory f) {
        return false;
    }

    /**
     * Gives notification that something was inserted into the document
     * in a location that this view is responsible for.  This is largely
     * delegated to the superclass, but is reimplemented to update the
     * relevant zone (i.e. determine if a zone needs to be split into a
     * set of 2 or more zones).
     *
     * @param changes the change information from the associated document
     * @param a the current allocation of the view
     * @param f the factory to use to rebuild if the view has children
     * @see View#insertUpdate
     */
    public void insertUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        handleInsert(changes.getOffset(), changes.getLength());
        super.insertUpdate(changes, a, f);
    }

    /**
     * Gives notification that something was removed from the document
     * in a location that this view is responsible for.  This is largely
     * delegated to the superclass, but is reimplemented to update the
     * relevant zones (i.e. determine if zones need to be removed or
     * joined with another zone).
     *
     * @param changes the change information from the associated document
     * @param a the current allocation of the view
     * @param f the factory to use to rebuild if the view has children
     * @see View#removeUpdate
     */
    public void removeUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        handleRemove(changes.getOffset(), changes.getLength());
        super.removeUpdate(changes, a, f);
    }

    /**
     * Internally created view that has the purpose of holding
     * the views that represent the children of the ZoneView
     * that have been arranged in a zone.
     */
    class Zone extends AsyncBoxView {

        private Position start;
        private Position end;

        public Zone(Element elem, Position start, Position end) {
            super(elem, ZoneView.this.getAxis());
            this.start = start;
            this.end = end;
        }

        /**
         * Creates the child views and populates the
         * zone with them.  This is done by translating
         * the positions to child element index locations
         * and building views to those elements.  If the
         * zone is already loaded, this does nothing.
         */
        public void load() {
            if (! isLoaded()) {
                setEstimatedMajorSpan(true);
                Element e = getElement();
                ViewFactory f = getViewFactory();
                int index0 = e.getElementIndex(getStartOffset());
                int index1 = e.getElementIndex(getEndOffset());
                View[] added = new View[index1 - index0 + 1];
                for (int i = index0; i <= index1; i++) {
                    added[i - index0] = f.create(e.getElement(i));
                }
                replace(0, 0, added);

                zoneWasLoaded(this);
            }
        }

        /**
         * Removes the child views and returns to a
         * state of unloaded.
         */
        public void unload() {
            setEstimatedMajorSpan(true);
            removeAll();
        }

        /**
         * Determines if the zone is in the loaded state
         * or not.
         */
        public boolean isLoaded() {
            return (getViewCount() != 0);
        }

        /**
         * This method is reimplemented to not build the children
         * since the children are created when the zone is loaded
         * rather then when it is placed in the view hierarchy.
         * The major span is estimated at this point by building
         * the first child (but not storing it), and calling
         * setEstimatedMajorSpan(true) followed by setSpan for
         * the major axis with the estimated span.
         */
        protected void loadChildren(ViewFactory f) {
            // mark the major span as estimated
            setEstimatedMajorSpan(true);

            // estimate the span
            Element elem = getElement();
            int index0 = elem.getElementIndex(getStartOffset());
            int index1 = elem.getElementIndex(getEndOffset());
            int nChildren = index1 - index0;

            // replace this with something real
            //setSpan(getMajorAxis(), nChildren * 10);

            View first = f.create(elem.getElement(index0));
            first.setParent(this);
            float w = first.getPreferredSpan(X_AXIS);
            float h = first.getPreferredSpan(Y_AXIS);
            if (getMajorAxis() == X_AXIS) {
                w *= nChildren;
            } else {
                h += nChildren;
            }

            setSize(w, h);
        }

        /**
         * Publish the changes in preferences upward to the parent
         * view.
         * <p>
         * This is reimplemented to stop the superclass behavior
         * if the zone has not yet been loaded.  If the zone is
         * unloaded for example, the last seen major span is the
         * best estimate and a calculated span for no children
         * is undesirable.
         */
        protected void flushRequirementChanges() {
            if (isLoaded()) {
                super.flushRequirementChanges();
            }
        }

        /**
         * Returns the child view index representing the given position in
         * the model.  Since the zone contains a cluster of the overall
         * set of child elements, we can determine the index fairly
         * quickly from the model by subtracting the index of the
         * start offset from the index of the position given.
         *
         * @param pos the position >= 0
         * @return  index of the view representing the given position, or
         *   -1 if no view represents that position
         * @since 1.3
         */
        public int getViewIndex(int pos, Position.Bias b) {
            boolean isBackward = (b == Position.Bias.Backward);
            pos = (isBackward) ? Math.max(0, pos - 1) : pos;
            Element elem = getElement();
            int index1 = elem.getElementIndex(pos);
            int index0 = elem.getElementIndex(getStartOffset());
            return index1 - index0;
        }

        protected boolean updateChildren(DocumentEvent.ElementChange ec,
                                         DocumentEvent e, ViewFactory f) {
            // the structure of this element changed.
            Element[] removedElems = ec.getChildrenRemoved();
            Element[] addedElems = ec.getChildrenAdded();
            Element elem = getElement();
            int index0 = elem.getElementIndex(getStartOffset());
            int index1 = elem.getElementIndex(getEndOffset()-1);
            int index = ec.getIndex();
            if ((index >= index0) && (index <= index1)) {
                // The change is in this zone
                int replaceIndex = index - index0;
                int nadd = Math.min(index1 - index0 + 1, addedElems.length);
                int nremove = Math.min(index1 - index0 + 1, removedElems.length);
                View[] added = new View[nadd];
                for (int i = 0; i < nadd; i++) {
                    added[i] = f.create(addedElems[i]);
                }
                replace(replaceIndex, nremove, added);
            }
            return true;
        }

        // --- View methods ----------------------------------

        /**
         * Fetches the attributes to use when rendering.  This view
         * isn't directly responsible for an element so it returns
         * the outer classes attributes.
         */
        public AttributeSet getAttributes() {
            return ZoneView.this.getAttributes();
        }

        /**
         * Renders using the given rendering surface and area on that
         * surface.  This is implemented to load the zone if its not
         * already loaded, and then perform the superclass behavior.
         *
         * @param g the rendering surface to use
         * @param a the allocated region to render into
         * @see View#paint
         */
        public void paint(Graphics g, Shape a) {
            load();
            super.paint(g, a);
        }

        /**
         * Provides a mapping from the view coordinate space to the logical
         * coordinate space of the model.  This is implemented to first
         * make sure the zone is loaded before providing the superclass
         * behavior.
         *
         * @param x   x coordinate of the view location to convert >= 0
         * @param y   y coordinate of the view location to convert >= 0
         * @param a the allocated region to render into
         * @return the location within the model that best represents the
         *  given point in the view >= 0
         * @see View#viewToModel
         */
        public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
            load();
            return super.viewToModel(x, y, a, bias);
        }

        /**
         * Provides a mapping from the document model coordinate space
         * to the coordinate space of the view mapped to it.  This is
         * implemented to provide the superclass behavior after first
         * making sure the zone is loaded (The zone must be loaded to
         * make this calculation).
         *
         * @param pos the position to convert
         * @param a the allocated region to render into
         * @return the bounding box of the given position
         * @exception BadLocationException  if the given position does not represent a
         *   valid location in the associated document
         * @see View#modelToView
         */
        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            load();
            return super.modelToView(pos, a, b);
        }

        /**
         * Start of the zones range.
         *
         * @see View#getStartOffset
         */
        public int getStartOffset() {
            return start.getOffset();
        }

        /**
         * End of the zones range.
         */
        public int getEndOffset() {
            return end.getOffset();
        }

        /**
         * Gives notification that something was inserted into
         * the document in a location that this view is responsible for.
         * If the zone has been loaded, the superclass behavior is
         * invoked, otherwise this does nothing.
         *
         * @param e the change information from the associated document
         * @param a the current allocation of the view
         * @param f the factory to use to rebuild if the view has children
         * @see View#insertUpdate
         */
        public void insertUpdate(DocumentEvent e, Shape a, ViewFactory f) {
            if (isLoaded()) {
                super.insertUpdate(e, a, f);
            }
        }

        /**
         * Gives notification that something was removed from the document
         * in a location that this view is responsible for.
         * If the zone has been loaded, the superclass behavior is
         * invoked, otherwise this does nothing.
         *
         * @param e the change information from the associated document
         * @param a the current allocation of the view
         * @param f the factory to use to rebuild if the view has children
         * @see View#removeUpdate
         */
        public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
            if (isLoaded()) {
                super.removeUpdate(e, a, f);
            }
        }

        /**
         * Gives notification from the document that attributes were changed
         * in a location that this view is responsible for.
         * If the zone has been loaded, the superclass behavior is
         * invoked, otherwise this does nothing.
         *
         * @param e the change information from the associated document
         * @param a the current allocation of the view
         * @param f the factory to use to rebuild if the view has children
         * @see View#removeUpdate
         */
        public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
            if (isLoaded()) {
                super.changedUpdate(e, a, f);
            }
        }

    }
}
