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

package javax.swing.tree;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.EventListener;
import java.util.Vector;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

/**
 * A simple tree data model that uses TreeNodes.
 * For further information and examples that use DefaultTreeModel,
 * see <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/tree.html">How to Use Trees</a>
 * in <em>The Java Tutorial.</em>
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
public class DefaultTreeModel implements Serializable, TreeModel {
    /** Root of the tree. */
    protected TreeNode root;
    /** Listeners. */
    protected EventListenerList listenerList = new EventListenerList();
    /**
      * Determines how the <code>isLeaf</code> method figures
      * out if a node is a leaf node. If true, a node is a leaf
      * node if it does not allow children. (If it allows
      * children, it is not a leaf node, even if no children
      * are present.) That lets you distinguish between <i>folder</i>
      * nodes and <i>file</i> nodes in a file system, for example.
      * <p>
      * If this value is false, then any node which has no
      * children is a leaf node, and any node may acquire
      * children.
      *
      * @see TreeNode#getAllowsChildren
      * @see TreeModel#isLeaf
      * @see #setAsksAllowsChildren
      */
    protected boolean asksAllowsChildren;


    /**
      * Creates a tree in which any node can have children.
      *
      * @param root a TreeNode object that is the root of the tree
      * @see #DefaultTreeModel(TreeNode, boolean)
      */
     @ConstructorProperties({"root"})
     public DefaultTreeModel(TreeNode root) {
        this(root, false);
    }

    /**
      * Creates a tree specifying whether any node can have children,
      * or whether only certain nodes can have children.
      *
      * @param root a TreeNode object that is the root of the tree
      * @param asksAllowsChildren a boolean, false if any node can
      *        have children, true if each node is asked to see if
      *        it can have children
      * @see #asksAllowsChildren
      */
    public DefaultTreeModel(TreeNode root, boolean asksAllowsChildren) {
        super();
        this.root = root;
        this.asksAllowsChildren = asksAllowsChildren;
    }

    /**
      * Sets whether or not to test leafness by asking getAllowsChildren()
      * or isLeaf() to the TreeNodes.  If newvalue is true, getAllowsChildren()
      * is messaged, otherwise isLeaf() is messaged.
      *
      * @param newValue if true, getAllowsChildren() is messaged, otherwise
      *                 isLeaf() is messaged
      */
    public void setAsksAllowsChildren(boolean newValue) {
        asksAllowsChildren = newValue;
    }

    /**
      * Tells how leaf nodes are determined.
      *
      * @return true if only nodes which do not allow children are
      *         leaf nodes, false if nodes which have no children
      *         (even if allowed) are leaf nodes
      * @see #asksAllowsChildren
      */
    public boolean asksAllowsChildren() {
        return asksAllowsChildren;
    }

    /**
     * Sets the root to <code>root</code>. A null <code>root</code> implies
     * the tree is to display nothing, and is legal.
     *
     * @param root new value of tree root
     */
    public void setRoot(TreeNode root) {
        Object oldRoot = this.root;
        this.root = root;
        if (root == null && oldRoot != null) {
            fireTreeStructureChanged(this, null);
        }
        else {
            nodeStructureChanged(root);
        }
    }

    /**
     * Returns the root of the tree.  Returns null only if the tree has
     * no nodes.
     *
     * @return  the root of the tree
     */
    public Object getRoot() {
        return root;
    }

    /**
     * Returns the index of child in parent.
     * If either the parent or child is <code>null</code>, returns -1.
     * @param parent a note in the tree, obtained from this data source
     * @param child the node we are interested in
     * @return the index of the child in the parent, or -1
     *    if either the parent or the child is <code>null</code>
     */
    public int getIndexOfChild(Object parent, Object child) {
        if(parent == null || child == null)
            return -1;
        return ((TreeNode)parent).getIndex((TreeNode)child);
    }

    /**
     * Returns the child of <I>parent</I> at index <I>index</I> in the parent's
     * child array.  <I>parent</I> must be a node previously obtained from
     * this data source. This should not return null if <i>index</i>
     * is a valid index for <i>parent</i> (that is <i>index</i> &gt;= 0 &amp;&amp;
     * <i>index</i> &lt; getChildCount(<i>parent</i>)).
     *
     * @param   parent  a node in the tree, obtained from this data source
     * @return  the child of <I>parent</I> at index <I>index</I>
     */
    public Object getChild(Object parent, int index) {
        return ((TreeNode)parent).getChildAt(index);
    }

    /**
     * Returns the number of children of <I>parent</I>.  Returns 0 if the node
     * is a leaf or if it has no children.  <I>parent</I> must be a node
     * previously obtained from this data source.
     *
     * @param   parent  a node in the tree, obtained from this data source
     * @return  the number of children of the node <I>parent</I>
     */
    public int getChildCount(Object parent) {
        return ((TreeNode)parent).getChildCount();
    }

    /**
     * Returns whether the specified node is a leaf node.
     * The way the test is performed depends on the
     * <code>askAllowsChildren</code> setting.
     *
     * @param node the node to check
     * @return true if the node is a leaf node
     *
     * @see #asksAllowsChildren
     * @see TreeModel#isLeaf
     */
    public boolean isLeaf(Object node) {
        if(asksAllowsChildren)
            return !((TreeNode)node).getAllowsChildren();
        return ((TreeNode)node).isLeaf();
    }

    /**
     * Invoke this method if you've modified the {@code TreeNode}s upon which
     * this model depends. The model will notify all of its listeners that the
     * model has changed.
     */
    public void reload() {
        reload(root);
    }

    /**
      * This sets the user object of the TreeNode identified by path
      * and posts a node changed.  If you use custom user objects in
      * the TreeModel you're going to need to subclass this and
      * set the user object of the changed node to something meaningful.
      */
    public void valueForPathChanged(TreePath path, Object newValue) {
        MutableTreeNode   aNode = (MutableTreeNode)path.getLastPathComponent();

        aNode.setUserObject(newValue);
        nodeChanged(aNode);
    }

    /**
     * Invoked this to insert newChild at location index in parents children.
     * This will then message nodesWereInserted to create the appropriate
     * event. This is the preferred way to add children as it will create
     * the appropriate event.
     *
     * @param newChild  child node to be inserted
     * @param parent    node to which children new node will be added
     * @param index     index of parent's children
     */
    public void insertNodeInto(MutableTreeNode newChild,
                               MutableTreeNode parent, int index){
        parent.insert(newChild, index);

        int[]           newIndexs = new int[1];

        newIndexs[0] = index;
        nodesWereInserted(parent, newIndexs);
    }

    /**
     * Message this to remove node from its parent. This will message
     * nodesWereRemoved to create the appropriate event. This is the
     * preferred way to remove a node as it handles the event creation
     * for you.
     *
     * @param node the node to be removed from it's parrent
     */
    public void removeNodeFromParent(MutableTreeNode node) {
        MutableTreeNode         parent = (MutableTreeNode)node.getParent();

        if(parent == null)
            throw new IllegalArgumentException("node does not have a parent.");

        int[]            childIndex = new int[1];
        Object[]         removedArray = new Object[1];

        childIndex[0] = parent.getIndex(node);
        parent.remove(childIndex[0]);
        removedArray[0] = node;
        nodesWereRemoved(parent, childIndex, removedArray);
    }

    /**
      * Invoke this method after you've changed how node is to be
      * represented in the tree.
      *
      * @param node the changed node
      */
    public void nodeChanged(TreeNode node) {
        if(listenerList != null && node != null) {
            TreeNode         parent = node.getParent();

            if(parent != null) {
                int        anIndex = parent.getIndex(node);
                if(anIndex != -1) {
                    int[]        cIndexs = new int[1];

                    cIndexs[0] = anIndex;
                    nodesChanged(parent, cIndexs);
                }
            }
            else if (node == getRoot()) {
                nodesChanged(node, null);
            }
        }
    }

    /**
     * Invoke this method if you've modified the {@code TreeNode}s upon which
     * this model depends. The model will notify all of its listeners that the
     * model has changed below the given node.
     *
     * @param node the node below which the model has changed
     */
    public void reload(TreeNode node) {
        if(node != null) {
            fireTreeStructureChanged(this, getPathToRoot(node), null, null);
        }
    }

    /**
      * Invoke this method after you've inserted some TreeNodes into
      * node.  childIndices should be the index of the new elements and
      * must be sorted in ascending order.
      *
      * @param node         parent node which children count been incremented
      * @param childIndices indexes of inserted children
      */
    public void nodesWereInserted(TreeNode node, int[] childIndices) {
        if(listenerList != null && node != null && childIndices != null
           && childIndices.length > 0) {
            int               cCount = childIndices.length;
            Object[]          newChildren = new Object[cCount];

            for(int counter = 0; counter < cCount; counter++)
                newChildren[counter] = node.getChildAt(childIndices[counter]);
            fireTreeNodesInserted(this, getPathToRoot(node), childIndices,
                                  newChildren);
        }
    }

    /**
      * Invoke this method after you've removed some TreeNodes from
      * node.  childIndices should be the index of the removed elements and
      * must be sorted in ascending order. And removedChildren should be
      * the array of the children objects that were removed.
      *
      * @param node             parent node which childred were removed
      * @param childIndices     indexes of removed childs
      * @param removedChildren  array of the children objects that were removed
      */
    public void nodesWereRemoved(TreeNode node, int[] childIndices,
                                 Object[] removedChildren) {
        if(node != null && childIndices != null) {
            fireTreeNodesRemoved(this, getPathToRoot(node), childIndices,
                                 removedChildren);
        }
    }

    /**
      * Invoke this method after you've changed how the children identified by
      * childIndicies are to be represented in the tree.
      *
      * @param node         changed node
      * @param childIndices indexes of changed children
      */
    public void nodesChanged(TreeNode node, int[] childIndices) {
        if(node != null) {
            if (childIndices != null) {
                int            cCount = childIndices.length;

                if(cCount > 0) {
                    Object[]       cChildren = new Object[cCount];

                    for(int counter = 0; counter < cCount; counter++)
                        cChildren[counter] = node.getChildAt
                            (childIndices[counter]);
                    fireTreeNodesChanged(this, getPathToRoot(node),
                                         childIndices, cChildren);
                }
            }
            else if (node == getRoot()) {
                fireTreeNodesChanged(this, getPathToRoot(node), null, null);
            }
        }
    }

    /**
      * Invoke this method if you've totally changed the children of
      * node and its children's children...  This will post a
      * treeStructureChanged event.
      *
      * @param node changed node
      */
    public void nodeStructureChanged(TreeNode node) {
        if(node != null) {
           fireTreeStructureChanged(this, getPathToRoot(node), null, null);
        }
    }

    /**
     * Builds the parents of node up to and including the root node,
     * where the original node is the last element in the returned array.
     * The length of the returned array gives the node's depth in the
     * tree.
     *
     * @param aNode the TreeNode to get the path for
     * @return an array of TreeNodes giving the path from the root
     */
    public TreeNode[] getPathToRoot(TreeNode aNode) {
        return getPathToRoot(aNode, 0);
    }

    /**
     * Builds the parents of node up to and including the root node,
     * where the original node is the last element in the returned array.
     * The length of the returned array gives the node's depth in the
     * tree.
     *
     * @param aNode  the TreeNode to get the path for
     * @param depth  an int giving the number of steps already taken towards
     *        the root (on recursive calls), used to size the returned array
     * @return an array of TreeNodes giving the path from the root to the
     *         specified node
     */
    protected TreeNode[] getPathToRoot(TreeNode aNode, int depth) {
        TreeNode[]              retNodes;
        // This method recurses, traversing towards the root in order
        // size the array. On the way back, it fills in the nodes,
        // starting from the root and working back to the original node.

        /* Check for null, in case someone passed in a null node, or
           they passed in an element that isn't rooted at root. */
        if(aNode == null) {
            if(depth == 0)
                return null;
            else
                retNodes = new TreeNode[depth];
        }
        else {
            depth++;
            if(aNode == root)
                retNodes = new TreeNode[depth];
            else
                retNodes = getPathToRoot(aNode.getParent(), depth);
            retNodes[retNodes.length - depth] = aNode;
        }
        return retNodes;
    }

    //
    //  Events
    //

    /**
     * Adds a listener for the TreeModelEvent posted after the tree changes.
     *
     * @see     #removeTreeModelListener
     * @param   l       the listener to add
     */
    public void addTreeModelListener(TreeModelListener l) {
        listenerList.add(TreeModelListener.class, l);
    }

    /**
     * Removes a listener previously added with <B>addTreeModelListener()</B>.
     *
     * @see     #addTreeModelListener
     * @param   l       the listener to remove
     */
    public void removeTreeModelListener(TreeModelListener l) {
        listenerList.remove(TreeModelListener.class, l);
    }

    /**
     * Returns an array of all the tree model listeners
     * registered on this model.
     *
     * @return all of this model's <code>TreeModelListener</code>s
     *         or an empty
     *         array if no tree model listeners are currently registered
     *
     * @see #addTreeModelListener
     * @see #removeTreeModelListener
     *
     * @since 1.4
     */
    public TreeModelListener[] getTreeModelListeners() {
        return listenerList.getListeners(TreeModelListener.class);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.
     *
     * @param source the source of the {@code TreeModelEvent};
     *               typically {@code this}
     * @param path the path to the parent of the nodes that changed; use
     *             {@code null} to identify the root has changed
     * @param childIndices the indices of the changed elements
     * @param children the changed elements
     */
    protected void fireTreeNodesChanged(Object source, Object[] path,
                                        int[] childIndices,
                                        Object[] children) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==TreeModelListener.class) {
                // Lazily create the event:
                if (e == null)
                    e = new TreeModelEvent(source, path,
                                           childIndices, children);
                ((TreeModelListener)listeners[i+1]).treeNodesChanged(e);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.
     *
     * @param source the source of the {@code TreeModelEvent};
     *               typically {@code this}
     * @param path the path to the parent the nodes were added to
     * @param childIndices the indices of the new elements
     * @param children the new elements
     */
    protected void fireTreeNodesInserted(Object source, Object[] path,
                                        int[] childIndices,
                                        Object[] children) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==TreeModelListener.class) {
                // Lazily create the event:
                if (e == null)
                    e = new TreeModelEvent(source, path,
                                           childIndices, children);
                ((TreeModelListener)listeners[i+1]).treeNodesInserted(e);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.
     *
     * @param source the source of the {@code TreeModelEvent};
     *               typically {@code this}
     * @param path the path to the parent the nodes were removed from
     * @param childIndices the indices of the removed elements
     * @param children the removed elements
     */
    protected void fireTreeNodesRemoved(Object source, Object[] path,
                                        int[] childIndices,
                                        Object[] children) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==TreeModelListener.class) {
                // Lazily create the event:
                if (e == null)
                    e = new TreeModelEvent(source, path,
                                           childIndices, children);
                ((TreeModelListener)listeners[i+1]).treeNodesRemoved(e);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.
     *
     * @param source the source of the {@code TreeModelEvent};
     *               typically {@code this}
     * @param path the path to the parent of the structure that has changed;
     *             use {@code null} to identify the root has changed
     * @param childIndices the indices of the affected elements
     * @param children the affected elements
     */
    protected void fireTreeStructureChanged(Object source, Object[] path,
                                        int[] childIndices,
                                        Object[] children) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==TreeModelListener.class) {
                // Lazily create the event:
                if (e == null)
                    e = new TreeModelEvent(source, path,
                                           childIndices, children);
                ((TreeModelListener)listeners[i+1]).treeStructureChanged(e);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.
     *
     * @param source the source of the {@code TreeModelEvent};
     *               typically {@code this}
     * @param path the path to the parent of the structure that has changed;
     *             use {@code null} to identify the root has changed
     */
    private void fireTreeStructureChanged(Object source, TreePath path) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==TreeModelListener.class) {
                // Lazily create the event:
                if (e == null)
                    e = new TreeModelEvent(source, path);
                ((TreeModelListener)listeners[i+1]).treeStructureChanged(e);
            }
        }
    }

    /**
     * Returns an array of all the objects currently registered
     * as <code><em>Foo</em>Listener</code>s
     * upon this model.
     * <code><em>Foo</em>Listener</code>s are registered using the
     * <code>add<em>Foo</em>Listener</code> method.
     *
     * <p>
     *
     * You can specify the <code>listenerType</code> argument
     * with a class literal,
     * such as
     * <code><em>Foo</em>Listener.class</code>.
     * For example, you can query a
     * <code>DefaultTreeModel</code> <code>m</code>
     * for its tree model listeners with the following code:
     *
     * <pre>TreeModelListener[] tmls = (TreeModelListener[])(m.getListeners(TreeModelListener.class));</pre>
     *
     * If no such listeners exist, this method returns an empty array.
     *
     * @param <T> the listener type
     * @param listenerType the type of listeners requested
     * @return an array of all objects registered as
     *          <code><em>Foo</em>Listener</code>s on this component,
     *          or an empty array if no such
     *          listeners have been added
     * @exception ClassCastException if <code>listenerType</code>
     *          doesn't specify a class or interface that implements
     *          <code>java.util.EventListener</code>
     *
     * @see #getTreeModelListeners
     *
     * @since 1.3
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    // Serialization support.
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        Vector<Object> values = new Vector<Object>();

        s.defaultWriteObject();
        // Save the root, if its Serializable.
        if(root != null && root instanceof Serializable) {
            values.addElement("root");
            values.addElement(root);
        }
        s.writeObject(values);
    }

    @Serial
    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField f = s.readFields();
        EventListenerList newListenerList = (EventListenerList) f.get("listenerList", null);
        if (newListenerList == null) {
            throw new InvalidObjectException("Null listenerList");
        }
        listenerList = newListenerList;
        asksAllowsChildren = f.get("asksAllowsChildren", false);

        Vector<?>       values = (Vector)s.readObject();
        int             indexCounter = 0;
        int             maxCounter = values.size();

        if(indexCounter < maxCounter && values.elementAt(indexCounter).
           equals("root")) {
            TreeNode newRoot  = (TreeNode)values.elementAt(++indexCounter);
            if (newRoot == null) {
                throw new InvalidObjectException("Null root");
            }
            root = newRoot;
            indexCounter++;
        }
    }


} // End of class DefaultTreeModel
