/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.event;

import java.util.EventObject;
import javax.swing.tree.TreePath;


/**
 * Encapsulates information describing changes to a tree model, and
 * used to notify tree model listeners of the change.
 * For more information and examples see
 * <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/events/treemodellistener.html">How to Write a Tree Model Listener</a>,
 * a section in <em>The Java Tutorial.</em>
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
 * @author Rob Davis
 * @author Ray Ryan
 * @author Scott Violet
 */
@SuppressWarnings("serial") // Same-version serialization only
public class TreeModelEvent extends EventObject {
    /** Path to the parent of the nodes that have changed. */
    protected TreePath  path;
    /** Indices identifying the position of where the children were. */
    protected int[]     childIndices;
    /** Children that have been removed. */
    protected Object[]  children;

    /**
     * Used to create an event when nodes have been changed, inserted, or
     * removed, identifying the path to the parent of the modified items as
     * an array of Objects. All of the modified objects are siblings which are
     * direct descendents (not grandchildren) of the specified parent.
     * The positions at which the inserts, deletes, or changes occurred are
     * specified by an array of <code>int</code>. The indexes in that array
     * must be in order, from lowest to highest.
     * <p>
     * For changes, the indexes in the model correspond exactly to the indexes
     * of items currently displayed in the UI. As a result, it is not really
     * critical if the indexes are not in their exact order. But after multiple
     * inserts or deletes, the items currently in the UI no longer correspond
     * to the items in the model. It is therefore critical to specify the
     * indexes properly for inserts and deletes.
     * <p>
     * For inserts, the indexes represent the <i>final</i> state of the tree,
     * after the inserts have occurred. Since the indexes must be specified in
     * order, the most natural processing methodology is to do the inserts
     * starting at the lowest index and working towards the highest. Accumulate
     * a Vector of <code>Integer</code> objects that specify the
     * insert-locations as you go, then convert the Vector to an
     * array of <code>int</code> to create the event. When the postition-index
     * equals zero, the node is inserted at the beginning of the list. When the
     * position index equals the size of the list, the node is "inserted" at
     * (appended to) the end of the list.
     * <p>
     * For deletes, the indexes represent the <i>initial</i> state of the tree,
     * before the deletes have occurred. Since the indexes must be specified in
     * order, the most natural processing methodology is to use a delete-counter.
     * Start by initializing the counter to zero and start work through the
     * list from lowest to highest. Every time you do a delete, add the current
     * value of the delete-counter to the index-position where the delete occurred,
     * and append the result to a Vector of delete-locations, using
     * <code>addElement()</code>. Then increment the delete-counter. The index
     * positions stored in the Vector therefore reflect the effects of all previous
     * deletes, so they represent each object's position in the initial tree.
     * (You could also start at the highest index and working back towards the
     * lowest, accumulating a Vector of delete-locations as you go using the
     * <code>insertElementAt(Integer, 0)</code>.) However you produce the Vector
     * of initial-positions, you then need to convert the Vector of <code>Integer</code>
     * objects to an array of <code>int</code> to create the event.
     * <p>
     * <b>Notes:</b><ul style="list-style-type:none">
     * <li>Like the <code>insertNodeInto</code> method in the
     *    <code>DefaultTreeModel</code> class, <code>insertElementAt</code>
     *    appends to the <code>Vector</code> when the index matches the size
     *    of the vector. So you can use <code>insertElementAt(Integer, 0)</code>
     *    even when the vector is empty.</li>
     * <li>To create a node changed event for the root node, specify the parent
     *     and the child indices as <code>null</code>.</li>
     * </ul>
     *
     * @param source the Object responsible for generating the event (typically
     *               the creator of the event object passes <code>this</code>
     *               for its value)
     * @param path   an array of Object identifying the path to the
     *               parent of the modified item(s), where the first element
     *               of the array is the Object stored at the root node and
     *               the last element is the Object stored at the parent node
     * @param childIndices an array of <code>int</code> that specifies the
     *               index values of the removed items. The indices must be
     *               in sorted order, from lowest to highest
     * @param children an array of Object containing the inserted, removed, or
     *                 changed objects
     * @see TreePath
     */
    public TreeModelEvent(Object source, Object[] path, int[] childIndices,
                          Object[] children)
    {
        this(source, (path == null) ? null : new TreePath(path), childIndices, children);
    }

    /**
     * Used to create an event when nodes have been changed, inserted, or
     * removed, identifying the path to the parent of the modified items as
     * a TreePath object. For more information on how to specify the indexes
     * and objects, see
     * <code>TreeModelEvent(Object,Object[],int[],Object[])</code>.
     *
     * @param source the Object responsible for generating the event (typically
     *               the creator of the event object passes <code>this</code>
     *               for its value)
     * @param path   a TreePath object that identifies the path to the
     *               parent of the modified item(s)
     * @param childIndices an array of <code>int</code> that specifies the
     *               index values of the modified items
     * @param children an array of Object containing the inserted, removed, or
     *                 changed objects
     *
     * @see #TreeModelEvent(Object,Object[],int[],Object[])
     */
    public TreeModelEvent(Object source, TreePath path, int[] childIndices,
                          Object[] children)
    {
        super(source);
        this.path = path;
        this.childIndices = childIndices;
        this.children = children;
    }

    /**
     * Used to create an event when the node structure has changed in some way,
     * identifying the path to the root of a modified subtree as an array of
     * Objects. A structure change event might involve nodes swapping position,
     * for example, or it might encapsulate multiple inserts and deletes in the
     * subtree stemming from the node, where the changes may have taken place at
     * different levels of the subtree.
     * <blockquote>
     *   <b>Note:</b><br>
     *   JTree collapses all nodes under the specified node, so that only its
     *   immediate children are visible.
     * </blockquote>
     *
     * @param source the Object responsible for generating the event (typically
     *               the creator of the event object passes <code>this</code>
     *               for its value)
     * @param path   an array of Object identifying the path to the root of the
     *               modified subtree, where the first element of the array is
     *               the object stored at the root node and the last element
     *               is the object stored at the changed node
     * @see TreePath
     */
    public TreeModelEvent(Object source, Object[] path)
    {
        this(source, (path == null) ? null : new TreePath(path));
    }

    /**
     * Used to create an event when the node structure has changed in some way,
     * identifying the path to the root of the modified subtree as a TreePath
     * object. For more information on this event specification, see
     * <code>TreeModelEvent(Object,Object[])</code>.
     *
     * @param source the Object responsible for generating the event (typically
     *               the creator of the event object passes <code>this</code>
     *               for its value)
     * @param path   a TreePath object that identifies the path to the
     *               change. In the DefaultTreeModel,
     *               this object contains an array of user-data objects,
     *               but a subclass of TreePath could use some totally
     *               different mechanism -- for example, a node ID number
     *
     * @see #TreeModelEvent(Object,Object[])
     */
    public TreeModelEvent(Object source, TreePath path)
    {
        super(source);
        this.path = path;
        this.childIndices = new int[0];
    }

    /**
     * For all events, except treeStructureChanged,
     * returns the parent of the changed nodes.
     * For treeStructureChanged events, returns the ancestor of the
     * structure that has changed. This and
     * <code>getChildIndices</code> are used to get a list of the effected
     * nodes.
     * <p>
     * The one exception to this is a treeNodesChanged event that is to
     * identify the root, in which case this will return the root
     * and <code>getChildIndices</code> will return null.
     *
     * @return the TreePath used in identifying the changed nodes.
     * @see TreePath#getLastPathComponent
     */
    public TreePath getTreePath() { return path; }

    /**
     * Convenience method to get the array of objects from the TreePath
     * instance that this event wraps.
     *
     * @return an array of Objects, where the first Object is the one
     *         stored at the root and the last object is the one
     *         stored at the node identified by the path
     */
    public Object[] getPath() {
        if(path != null)
            return path.getPath();
        return null;
    }

    /**
     * Returns the objects that are children of the node identified by
     * <code>getPath</code> at the locations specified by
     * <code>getChildIndices</code>. If this is a removal event the
     * returned objects are no longer children of the parent node.
     *
     * @return an array of Object containing the children specified by
     *         the event
     * @see #getPath
     * @see #getChildIndices
     */
    public Object[] getChildren() {
        if(children != null) {
            int            cCount = children.length;
            Object[]       retChildren = new Object[cCount];

            System.arraycopy(children, 0, retChildren, 0, cCount);
            return retChildren;
        }
        return null;
    }

    /**
     * Returns the values of the child indexes. If this is a removal event
     * the indexes point to locations in the initial list where items
     * were removed. If it is an insert, the indices point to locations
     * in the final list where the items were added. For node changes,
     * the indices point to the locations of the modified nodes.
     *
     * @return an array of <code>int</code> containing index locations for
     *         the children specified by the event
     */
    public int[] getChildIndices() {
        if(childIndices != null) {
            int            cCount = childIndices.length;
            int[]          retArray = new int[cCount];

            System.arraycopy(childIndices, 0, retArray, 0, cCount);
            return retArray;
        }
        return null;
    }

    /**
     * Returns a string that displays and identifies this object's
     * properties.
     *
     * @return a String representation of this object
     */
    public String toString() {
        StringBuilder   sb = new StringBuilder();

        sb.append(getClass().getName() + " " +
                  Integer.toString(hashCode()));
        if(path != null)
            sb.append(" path " + path);
        if(childIndices != null) {
            sb.append(" indices [ ");
            for(int counter = 0; counter < childIndices.length; counter++)
                sb.append(Integer.toString(childIndices[counter])+ " ");
            sb.append("]");
        }
        if(children != null) {
            sb.append(" children [ ");
            for(int counter = 0; counter < children.length; counter++)
                sb.append(children[counter] + " ");
            sb.append("]");
        }
        return sb.toString();
    }
}
