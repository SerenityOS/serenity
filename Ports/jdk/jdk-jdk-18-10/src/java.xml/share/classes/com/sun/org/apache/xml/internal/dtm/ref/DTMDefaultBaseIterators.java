/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xml.internal.dtm.ref;

import com.sun.org.apache.xml.internal.dtm.*;

import javax.xml.transform.Source;

import com.sun.org.apache.xml.internal.utils.XMLStringFactory;

import com.sun.org.apache.xml.internal.res.XMLErrorResources;
import com.sun.org.apache.xml.internal.res.XMLMessages;
import com.sun.org.apache.xalan.internal.xsltc.dom.NodeCounter;

/**
 * This class implements the traversers for DTMDefaultBase.
 */
public abstract class DTMDefaultBaseIterators extends DTMDefaultBaseTraversers
{

  /**
   * Construct a DTMDefaultBaseTraversers object from a DOM node.
   *
   * @param mgr The DTMManager who owns this DTM.
   * @param source The object that is used to specify the construction source.
   * @param dtmIdentity The DTM identity ID for this DTM.
   * @param whiteSpaceFilter The white space filter for this DTM, which may
   *                         be null.
   * @param xstringfactory The factory to use for creating XMLStrings.
   * @param doIndexing true if the caller considers it worth it to use
   *                   indexing schemes.
   */
  public DTMDefaultBaseIterators(DTMManager mgr, Source source,
                                 int dtmIdentity,
                                 DTMWSFilter whiteSpaceFilter,
                                 XMLStringFactory xstringfactory,
                                 boolean doIndexing)
  {
    super(mgr, source, dtmIdentity, whiteSpaceFilter,
          xstringfactory, doIndexing);
  }

  /**
   * Construct a DTMDefaultBaseTraversers object from a DOM node.
   *
   * @param mgr The DTMManager who owns this DTM.
   * @param source The object that is used to specify the construction source.
   * @param dtmIdentity The DTM identity ID for this DTM.
   * @param whiteSpaceFilter The white space filter for this DTM, which may
   *                         be null.
   * @param xstringfactory The factory to use for creating XMLStrings.
   * @param doIndexing true if the caller considers it worth it to use
   *                   indexing schemes.
   * @param blocksize The block size of the DTM.
   * @param usePrevsib true if we want to build the previous sibling node array.
   * @param newNameTable true if we want to use a new ExpandedNameTable for this DTM.
   */
  public DTMDefaultBaseIterators(DTMManager mgr, Source source,
                                 int dtmIdentity,
                                 DTMWSFilter whiteSpaceFilter,
                                 XMLStringFactory xstringfactory,
                                 boolean doIndexing,
                                 int blocksize,
                                 boolean usePrevsib,
                                 boolean newNameTable)
  {
    super(mgr, source, dtmIdentity, whiteSpaceFilter,
          xstringfactory, doIndexing, blocksize, usePrevsib, newNameTable);
  }

  /**
   * Get an iterator that can navigate over an XPath Axis, predicated by
   * the extended type ID.
   * Returns an iterator that must be initialized
   * with a start node (using iterator.setStartNode()).
   *
   * @param axis One of Axes.ANCESTORORSELF, etc.
   * @param type An extended type ID.
   *
   * @return A DTMAxisIterator, or null if the given axis isn't supported.
   */
  public DTMAxisIterator getTypedAxisIterator(int axis, int type)
  {

    DTMAxisIterator iterator = null;

    /* This causes an error when using patterns for elements that
       do not exist in the DOM (translet types which do not correspond
       to a DOM type are mapped to the DOM.ELEMENT type).
    */

    //        if (type == NO_TYPE) {
    //            return(EMPTYITERATOR);
    //        }
    //        else if (type == ELEMENT) {
    //            iterator = new FilterIterator(getAxisIterator(axis),
    //                                          getElementFilter());
    //        }
    //        else
    {
      switch (axis)
      {
      case Axis.SELF :
        iterator = new TypedSingletonIterator(type);
        break;
      case Axis.CHILD :
        iterator = new TypedChildrenIterator(type);
        break;
      case Axis.PARENT :
        return (new ParentIterator().setNodeType(type));
      case Axis.ANCESTOR :
        return (new TypedAncestorIterator(type));
      case Axis.ANCESTORORSELF :
        return ((new TypedAncestorIterator(type)).includeSelf());
      case Axis.ATTRIBUTE :
        return (new TypedAttributeIterator(type));
      case Axis.DESCENDANT :
        iterator = new TypedDescendantIterator(type);
        break;
      case Axis.DESCENDANTORSELF :
        iterator = (new TypedDescendantIterator(type)).includeSelf();
        break;
      case Axis.FOLLOWING :
        iterator = new TypedFollowingIterator(type);
        break;
      case Axis.PRECEDING :
        iterator = new TypedPrecedingIterator(type);
        break;
      case Axis.FOLLOWINGSIBLING :
        iterator = new TypedFollowingSiblingIterator(type);
        break;
      case Axis.PRECEDINGSIBLING :
        iterator = new TypedPrecedingSiblingIterator(type);
        break;
      case Axis.NAMESPACE :
        iterator = new TypedNamespaceIterator(type);
        break;
      case Axis.ROOT :
        iterator = new TypedRootIterator(type);
        break;
      default :
        throw new DTMException(XMLMessages.createXMLMessage(
            XMLErrorResources.ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED,
            new Object[]{Axis.getNames(axis)}));
            //"Error: typed iterator for axis "
                               //+ Axis.names[axis] + "not implemented");
      }
    }

    return (iterator);
  }

  /**
   * This is a shortcut to the iterators that implement the
   * XPath axes.
   * Returns a bare-bones iterator that must be initialized
   * with a start node (using iterator.setStartNode()).
   *
   * @param axis One of Axes.ANCESTORORSELF, etc.
   *
   * @return A DTMAxisIterator, or null if the given axis isn't supported.
   */
  public DTMAxisIterator getAxisIterator(final int axis)
  {

    DTMAxisIterator iterator = null;

    switch (axis)
    {
    case Axis.SELF :
      iterator = new SingletonIterator();
      break;
    case Axis.CHILD :
      iterator = new ChildrenIterator();
      break;
    case Axis.PARENT :
      return (new ParentIterator());
    case Axis.ANCESTOR :
      return (new AncestorIterator());
    case Axis.ANCESTORORSELF :
      return ((new AncestorIterator()).includeSelf());
    case Axis.ATTRIBUTE :
      return (new AttributeIterator());
    case Axis.DESCENDANT :
      iterator = new DescendantIterator();
      break;
    case Axis.DESCENDANTORSELF :
      iterator = (new DescendantIterator()).includeSelf();
      break;
    case Axis.FOLLOWING :
      iterator = new FollowingIterator();
      break;
    case Axis.PRECEDING :
      iterator = new PrecedingIterator();
      break;
    case Axis.FOLLOWINGSIBLING :
      iterator = new FollowingSiblingIterator();
      break;
    case Axis.PRECEDINGSIBLING :
      iterator = new PrecedingSiblingIterator();
      break;
    case Axis.NAMESPACE :
      iterator = new NamespaceIterator();
      break;
    case Axis.ROOT :
      iterator = new RootIterator();
      break;
    default :
      throw new DTMException(XMLMessages.createXMLMessage(
        XMLErrorResources.ER_ITERATOR_AXIS_NOT_IMPLEMENTED,
        new Object[]{Axis.getNames(axis)}));
        //"Error: iterator for axis '" + Axis.names[axis]
                             //+ "' not implemented");
    }

    return (iterator);
  }

  /**
   * Abstract superclass defining behaviors shared by all DTMDefault's
   * internal implementations of DTMAxisIterator. Subclass this (and
   * override, if necessary) to implement the specifics of an
   * individual axis iterator.
   *
   * Currently there isn't a lot here
   */
  public abstract class InternalAxisIteratorBase extends DTMAxisIteratorBase
  {

    // %REVIEW% We could opt to share _nodeType and setNodeType() as
    // well, and simply ignore them in iterators which don't use them.
    // But Scott's worried about the overhead involved in cloning
    // these, and wants them to have as few fields as possible. Note
    // that we can't create a TypedInternalAxisIteratorBase because
    // those are often based on the untyped versions and Java doesn't
    // support multiple inheritance. <sigh/>

    /**
     * Current iteration location. Usually this is the last location
     * returned (starting point for the next() search); for single-node
     * iterators it may instead be initialized to point to that single node.
     */
    protected int _currentNode;

    /**
     * Remembers the current node for the next call to gotoMark().
     *
     * %REVIEW% Should this save _position too?
     */
    public void setMark()
    {
      _markedNode = _currentNode;
    }

    /**
     * Restores the current node remembered by setMark().
     *
     * %REVEIW% Should this restore _position too?
     */
    public void gotoMark()
    {
      _currentNode = _markedNode;
    }

  }  // end of InternalAxisIteratorBase

  /**
   * Iterator that returns all immediate children of a given node
   */
  public final class ChildrenIterator extends InternalAxisIteratorBase
  {

    /**
     * Setting start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * If the iterator is not restartable, this has no effect.
     * %REVIEW% Should it return/throw something in that case,
     * or set current node to END, to indicate request-not-honored?
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {
//%HZ%: Added reference to DTMDefaultBase.ROOTNODE back in, temporarily
      if (node == DTMDefaultBase.ROOTNODE)
        node = getDocument();
      if (_isRestartable)
      {
        _startNode = node;
        _currentNode = (node == DTM.NULL) ? DTM.NULL
                                          : _firstch(makeNodeIdentity(node));

        return resetPosition();
      }

      return this;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END if no more
     * are available.
     */
    public int next()
    {
      if (_currentNode != NULL) {
        int node = _currentNode;
        _currentNode = _nextsib(node);
        return returnNode(makeNodeHandle(node));
      }

      return END;
    }
  }  // end of ChildrenIterator

  /**
   * Iterator that returns the parent of a given node. Note that
   * this delivers only a single node; if you want all the ancestors,
   * see AncestorIterator.
   */
  public final class ParentIterator extends InternalAxisIteratorBase
  {

    /** The extended type ID that was requested. */
    private int _nodeType = -1;

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {
//%HZ%: Added reference to DTMDefaultBase.ROOTNODE back in, temporarily
      if (node == DTMDefaultBase.ROOTNODE)
        node = getDocument();
      if (_isRestartable)
      {
        _startNode = node;
        _currentNode = getParent(node);

        return resetPosition();
      }

      return this;
    }

    /**
     * Set the node type of the parent that we're looking for.
     * Note that this does _not_ mean "find the nearest ancestor of
     * this type", but "yield the parent if it is of this type".
     *
     *
     * @param type extended type ID.
     *
     * @return ParentIterator configured with the type filter set.
     */
    public DTMAxisIterator setNodeType(final int type)
    {

      _nodeType = type;

      return this;
    }

    /**
     * Get the next node in the iteration. In this case, we return
     * only the immediate parent, _if_ it matches the requested nodeType.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {
      int result = _currentNode;

      if (_nodeType >= DTM.NTYPES) {
        if (_nodeType != getExpandedTypeID(_currentNode)) {
          result = END;
        }
      } else if (_nodeType != NULL) {
        if (_nodeType != getNodeType(_currentNode)) {
          result = END;
        }
      }

      _currentNode = END;

      return returnNode(result);
    }
  }  // end of ParentIterator

  /**
   * Iterator that returns children of a given type for a given node.
   * The functionality chould be achieved by putting a filter on top
   * of a basic child iterator, but a specialised iterator is used
   * for efficiency (both speed and size of translet).
   */
  public final class TypedChildrenIterator extends InternalAxisIteratorBase
  {

    /** The extended type ID that was requested. */
    private final int _nodeType;

    /**
     * Constructor TypedChildrenIterator
     *
     *
     * @param nodeType The extended type ID being requested.
     */
    public TypedChildrenIterator(int nodeType)
    {
      _nodeType = nodeType;
    }

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {
//%HZ%: Added reference to DTMDefaultBase.ROOTNODE back in, temporarily
      if (node == DTMDefaultBase.ROOTNODE)
        node = getDocument();
      if (_isRestartable)
      {
        _startNode = node;
        _currentNode = (node == DTM.NULL)
                                   ? DTM.NULL
                                   : _firstch(makeNodeIdentity(_startNode));

        return resetPosition();
      }

      return this;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {
      int eType;
      int node = _currentNode;

      int nodeType = _nodeType;

      if (nodeType >= DTM.NTYPES) {
        while (node != DTM.NULL && _exptype(node) != nodeType) {
          node = _nextsib(node);
        }
      } else {
        while (node != DTM.NULL) {
          eType = _exptype(node);
          if (eType < DTM.NTYPES) {
            if (eType == nodeType) {
              break;
            }
          } else if (m_expandedNameTable.getType(eType) == nodeType) {
            break;
          }
          node = _nextsib(node);
        }
      }

      if (node == DTM.NULL) {
        _currentNode = DTM.NULL;
        return DTM.NULL;
      } else {
        _currentNode = _nextsib(node);
        return returnNode(makeNodeHandle(node));
      }

    }
  }  // end of TypedChildrenIterator

  /**
   * Iterator that returns children within a given namespace for a
   * given node. The functionality chould be achieved by putting a
   * filter on top of a basic child iterator, but a specialised
   * iterator is used for efficiency (both speed and size of translet).
   */
  public final class NamespaceChildrenIterator
          extends InternalAxisIteratorBase
  {

    /** The extended type ID being requested. */
    private final int _nsType;

    /**
     * Constructor NamespaceChildrenIterator
     *
     *
     * @param type The extended type ID being requested.
     */
    public NamespaceChildrenIterator(final int type)
    {
      _nsType = type;
    }

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {
//%HZ%: Added reference to DTMDefaultBase.ROOTNODE back in, temporarily
      if (node == DTMDefaultBase.ROOTNODE)
        node = getDocument();
      if (_isRestartable)
      {
        _startNode = node;
        _currentNode = (node == DTM.NULL) ? DTM.NULL : NOTPROCESSED;

        return resetPosition();
      }

      return this;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {
      if (_currentNode != DTM.NULL) {
        for (int node = (NOTPROCESSED == _currentNode)
                                  ? _firstch(makeNodeIdentity(_startNode))
                                  : _nextsib(_currentNode);
             node != END;
             node = _nextsib(node)) {
          if (m_expandedNameTable.getNamespaceID(_exptype(node)) == _nsType) {
            _currentNode = node;

            return returnNode(node);
          }
        }
      }

      return END;
    }
  }  // end of NamespaceChildrenIterator

  /**
   * Iterator that returns the namespace nodes as defined by the XPath data model
   * for a given node.
   */
  public class NamespaceIterator
          extends InternalAxisIteratorBase
  {

    /**
     * Constructor NamespaceAttributeIterator
     */
    public NamespaceIterator()
    {

      super();
    }

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {
//%HZ%: Added reference to DTMDefaultBase.ROOTNODE back in, temporarily
      if (node == DTMDefaultBase.ROOTNODE)
        node = getDocument();
      if (_isRestartable)
      {
        _startNode = node;
        _currentNode = getFirstNamespaceNode(node, true);

        return resetPosition();
      }

      return this;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {

      int node = _currentNode;

      if (DTM.NULL != node)
        _currentNode = getNextNamespaceNode(_startNode, node, true);

      return returnNode(node);
    }
  }  // end of NamespaceIterator

  /**
   * Iterator that returns the namespace nodes as defined by the XPath data model
   * for a given node, filtered by extended type ID.
   */
  public class TypedNamespaceIterator extends NamespaceIterator
  {

    /** The extended type ID that was requested. */
    private final int _nodeType;

    /**
     * Constructor TypedNamespaceIterator
     *
     *
     * @param nodeType The extended type ID being requested.
     */
    public TypedNamespaceIterator(int nodeType)
    {
      super();
      _nodeType = nodeType;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {
        int node;

      for (node = _currentNode;
           node != END;
           node = getNextNamespaceNode(_startNode, node, true)) {
        if (getExpandedTypeID(node) == _nodeType
            || getNodeType(node) == _nodeType
            || getNamespaceType(node) == _nodeType) {
          _currentNode = node;

          return returnNode(node);
        }
      }

      return (_currentNode =END);
    }
  }  // end of TypedNamespaceIterator

  /**
   * Iterator that returns the the root node as defined by the XPath data model
   * for a given node.
   */
  public class RootIterator
          extends InternalAxisIteratorBase
  {

    /**
     * Constructor RootIterator
     */
    public RootIterator()
    {

      super();
    }

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {

      if (_isRestartable)
      {
        _startNode = getDocumentRoot(node);
        _currentNode = NULL;

        return resetPosition();
      }

      return this;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {
      if(_startNode == _currentNode)
        return NULL;

      _currentNode = _startNode;

      return returnNode(_startNode);
    }
  }  // end of RootIterator

  /**
   * Iterator that returns the namespace nodes as defined by the XPath data model
   * for a given node, filtered by extended type ID.
   */
  public class TypedRootIterator extends RootIterator
  {

    /** The extended type ID that was requested. */
    private final int _nodeType;

    /**
     * Constructor TypedRootIterator
     *
     * @param nodeType The extended type ID being requested.
     */
    public TypedRootIterator(int nodeType)
    {
      super();
      _nodeType = nodeType;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {
        if(_startNode == _currentNode)
        return NULL;

      int nodeType = _nodeType;
      int node = _startNode;
      int expType = getExpandedTypeID(node);

      _currentNode = node;

      if (nodeType >= DTM.NTYPES) {
        if (nodeType == expType) {
          return returnNode(node);
        }
      } else {
        if (expType < DTM.NTYPES) {
          if (expType == nodeType) {
            return returnNode(node);
          }
        } else {
          if (m_expandedNameTable.getType(expType) == nodeType) {
            return returnNode(node);
          }
        }
      }

      return END;
    }
  }  // end of TypedRootIterator

  /**
   * Iterator that returns attributes within a given namespace for a node.
   */
  public final class NamespaceAttributeIterator
          extends InternalAxisIteratorBase
  {

    /** The extended type ID being requested. */
    private final int _nsType;

    /**
     * Constructor NamespaceAttributeIterator
     *
     *
     * @param nsType The extended type ID being requested.
     */
    public NamespaceAttributeIterator(int nsType)
    {

      super();

      _nsType = nsType;
    }

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {
//%HZ%: Added reference to DTMDefaultBase.ROOTNODE back in, temporarily
      if (node == DTMDefaultBase.ROOTNODE)
        node = getDocument();
      if (_isRestartable)
      {
        _startNode = node;
        _currentNode = getFirstNamespaceNode(node, false);

        return resetPosition();
      }

      return this;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {

      int node = _currentNode;

      if (DTM.NULL != node)
        _currentNode = getNextNamespaceNode(_startNode, node, false);

      return returnNode(node);
    }
  }  // end of NamespaceAttributeIterator

  /**
   * Iterator that returns all siblings of a given node.
   */
  public class FollowingSiblingIterator extends InternalAxisIteratorBase
  {

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {
//%HZ%: Added reference to DTMDefaultBase.ROOTNODE back in, temporarily
      if (node == DTMDefaultBase.ROOTNODE)
        node = getDocument();
      if (_isRestartable)
      {
        _startNode = node;
        _currentNode = makeNodeIdentity(node);

        return resetPosition();
      }

      return this;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {
      _currentNode = (_currentNode == DTM.NULL) ? DTM.NULL
                                                : _nextsib(_currentNode);
      return returnNode(makeNodeHandle(_currentNode));
    }
  }  // end of FollowingSiblingIterator

  /**
   * Iterator that returns all following siblings of a given node.
   */
  public final class TypedFollowingSiblingIterator
          extends FollowingSiblingIterator
  {

    /** The extended type ID that was requested. */
    private final int _nodeType;

    /**
     * Constructor TypedFollowingSiblingIterator
     *
     *
     * @param type The extended type ID being requested.
     */
    public TypedFollowingSiblingIterator(int type)
    {
      _nodeType = type;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {
      if (_currentNode == DTM.NULL) {
        return DTM.NULL;
      }

      int node = _currentNode;
      int eType;
      int nodeType = _nodeType;

      if (nodeType >= DTM.NTYPES) {
        do {
          node = _nextsib(node);
        } while (node != DTM.NULL && _exptype(node) != nodeType);
      } else {
        while ((node = _nextsib(node)) != DTM.NULL) {
          eType = _exptype(node);
          if (eType < DTM.NTYPES) {
            if (eType == nodeType) {
              break;
            }
          } else if (m_expandedNameTable.getType(eType) == nodeType) {
            break;
          }
        }
      }

      _currentNode = node;

      return (_currentNode == DTM.NULL)
                      ? DTM.NULL
                      : returnNode(makeNodeHandle(_currentNode));
    }
  }  // end of TypedFollowingSiblingIterator

  /**
   * Iterator that returns attribute nodes (of what nodes?)
   */
  public final class AttributeIterator extends InternalAxisIteratorBase
  {

    // assumes caller will pass element nodes

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {
//%HZ%: Added reference to DTMDefaultBase.ROOTNODE back in, temporarily
      if (node == DTMDefaultBase.ROOTNODE)
        node = getDocument();
      if (_isRestartable)
      {
        _startNode = node;
        _currentNode = getFirstAttributeIdentity(makeNodeIdentity(node));

        return resetPosition();
      }

      return this;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {

      final int node = _currentNode;

      if (node != NULL) {
        _currentNode = getNextAttributeIdentity(node);
        return returnNode(makeNodeHandle(node));
      }

      return NULL;
    }
  }  // end of AttributeIterator

  /**
   * Iterator that returns attribute nodes of a given type
   */
  public final class TypedAttributeIterator extends InternalAxisIteratorBase
  {

    /** The extended type ID that was requested. */
    private final int _nodeType;

    /**
     * Constructor TypedAttributeIterator
     *
     *
     * @param nodeType The extended type ID that is requested.
     */
    public TypedAttributeIterator(int nodeType)
    {
      _nodeType = nodeType;
    }

    // assumes caller will pass element nodes

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {
      if (_isRestartable)
      {
        _startNode = node;

        _currentNode = getTypedAttribute(node, _nodeType);

        return resetPosition();
      }

      return this;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {

      final int node = _currentNode;

      // singleton iterator, since there can only be one attribute of
      // a given type.
      _currentNode = NULL;

      return returnNode(node);
    }
  }  // end of TypedAttributeIterator

  /**
   * Iterator that returns preceding siblings of a given node
   */
  public class PrecedingSiblingIterator extends InternalAxisIteratorBase
  {

    /**
     * The node identity of _startNode for this iterator
     */
    protected int _startNodeID;

    /**
     * True if this iterator has a reversed axis.
     *
     * @return true.
     */
    public boolean isReverse()
    {
      return true;
    }

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {
//%HZ%: Added reference to DTMDefaultBase.ROOTNODE back in, temporarily
      if (node == DTMDefaultBase.ROOTNODE)
        node = getDocument();
      if (_isRestartable)
      {
        _startNode = node;
        node = _startNodeID = makeNodeIdentity(node);

        if(node == NULL)
        {
          _currentNode = node;
          return resetPosition();
        }

        int type = m_expandedNameTable.getType(_exptype(node));
        if(ExpandedNameTable.ATTRIBUTE == type
           || ExpandedNameTable.NAMESPACE == type )
        {
          _currentNode = node;
        }
        else
        {
          // Be careful to handle the Document node properly
          _currentNode = _parent(node);
          if(NULL!=_currentNode)
            _currentNode = _firstch(_currentNode);
          else
            _currentNode = node;
        }

        return resetPosition();
      }

      return this;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {

      if (_currentNode == _startNodeID || _currentNode == DTM.NULL)
      {
        return NULL;
      }
      else
      {
        final int node = _currentNode;
        _currentNode = _nextsib(node);

        return returnNode(makeNodeHandle(node));
      }
    }
  }  // end of PrecedingSiblingIterator

  /**
   * Iterator that returns preceding siblings of a given type for
   * a given node
   */
  public final class TypedPrecedingSiblingIterator
          extends PrecedingSiblingIterator
  {

    /** The extended type ID that was requested. */
    private final int _nodeType;

    /**
     * Constructor TypedPrecedingSiblingIterator
     *
     *
     * @param type The extended type ID being requested.
     */
    public TypedPrecedingSiblingIterator(int type)
    {
      _nodeType = type;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {
      int node = _currentNode;
      int expType;

      int nodeType = _nodeType;
      int startID = _startNodeID;

      if (nodeType >= DTM.NTYPES) {
        while (node != NULL && node != startID && _exptype(node) != nodeType) {
          node = _nextsib(node);
        }
      } else {
        while (node != NULL && node != startID) {
          expType = _exptype(node);
          if (expType < DTM.NTYPES) {
            if (expType == nodeType) {
              break;
            }
          } else {
            if (m_expandedNameTable.getType(expType) == nodeType) {
              break;
            }
          }
          node = _nextsib(node);
        }
      }

      if (node == DTM.NULL || node == _startNodeID) {
        _currentNode = NULL;
        return NULL;
      } else {
        _currentNode = _nextsib(node);
        return returnNode(makeNodeHandle(node));
      }
    }
  }  // end of TypedPrecedingSiblingIterator

  /**
   * Iterator that returns preceding nodes of a given node.
   * This includes the node set {root+1, start-1}, but excludes
   * all ancestors, attributes, and namespace nodes.
   */
  public class PrecedingIterator extends InternalAxisIteratorBase
  {

    /** The max ancestors, but it can grow... */
    private final int _maxAncestors = 8;

    /**
     * The stack of start node + ancestors up to the root of the tree,
     *  which we must avoid.
     */
    protected int[] _stack = new int[_maxAncestors];

    /** (not sure yet... -sb) */
    protected int _sp, _oldsp;

    protected int _markedsp, _markedNode, _markedDescendant;

    /* _currentNode precedes candidates.  This is the identity, not the handle! */

    /**
     * True if this iterator has a reversed axis.
     *
     * @return true since this iterator is a reversed axis.
     */
    public boolean isReverse()
    {
      return true;
    }

    /**
     * Returns a deep copy of this iterator.   The cloned iterator is not reset.
     *
     * @return a deep copy of this iterator.
     */
    public DTMAxisIterator cloneIterator()
    {
      _isRestartable = false;

      try
      {
        final PrecedingIterator clone = (PrecedingIterator) super.clone();
        final int[] stackCopy = new int[_stack.length];
        System.arraycopy(_stack, 0, stackCopy, 0, _stack.length);

        clone._stack = stackCopy;

        // return clone.reset();
        return clone;
      }
      catch (CloneNotSupportedException e)
      {
        throw new DTMException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ITERATOR_CLONE_NOT_SUPPORTED, null)); //"Iterator clone not supported.");
      }
    }

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {
//%HZ%: Added reference to DTMDefaultBase.ROOTNODE back in, temporarily
      if (node == DTMDefaultBase.ROOTNODE)
        node = getDocument();
      if (_isRestartable)
      {
        node = makeNodeIdentity(node);

        // iterator is not a clone
        int parent, index;

       if (_type(node) == DTM.ATTRIBUTE_NODE)
        node = _parent(node);

        _startNode = node;
        _stack[index = 0] = node;



                parent=node;
                while ((parent = _parent(parent)) != NULL)
                {
                        if (++index == _stack.length)
                        {
                                final int[] stack = new int[index + 4];
                                System.arraycopy(_stack, 0, stack, 0, index);
                                _stack = stack;
                        }
                        _stack[index] = parent;
        }
        if(index>0)
                --index; // Pop actual root node (if not start) back off the stack

        _currentNode=_stack[index]; // Last parent before root node

        _oldsp = _sp = index;

        return resetPosition();
      }

      return this;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {
        // Bugzilla 8324: We were forgetting to skip Attrs and NS nodes.
        // Also recoded the loop controls for clarity and to flatten out
        // the tail-recursion.
                for(++_currentNode;
                        _sp>=0;
                        ++_currentNode)
                {
                        if(_currentNode < _stack[_sp])
                        {
                                if(_type(_currentNode) != ATTRIBUTE_NODE &&
                                        _type(_currentNode) != NAMESPACE_NODE)
                                        return returnNode(makeNodeHandle(_currentNode));
                        }
                        else
                                --_sp;
                }
                return NULL;
    }

    // redefine DTMAxisIteratorBase's reset

    /**
     * Resets the iterator to the last start node.
     *
     * @return A DTMAxisIterator, which may or may not be the same as this
     *         iterator.
     */
    public DTMAxisIterator reset()
    {

      _sp = _oldsp;

      return resetPosition();
    }

    public void setMark() {
        _markedsp = _sp;
        _markedNode = _currentNode;
        _markedDescendant = _stack[0];
    }

    public void gotoMark() {
        _sp = _markedsp;
        _currentNode = _markedNode;
    }
  }  // end of PrecedingIterator

  /**
   * Iterator that returns preceding nodes of agiven type for a
   * given node. This includes the node set {root+1, start-1}, but
   * excludes all ancestors.
   */
  public final class TypedPrecedingIterator extends PrecedingIterator
  {

    /** The extended type ID that was requested. */
    private final int _nodeType;

    /**
     * Constructor TypedPrecedingIterator
     *
     *
     * @param type The extended type ID being requested.
     */
    public TypedPrecedingIterator(int type)
    {
      _nodeType = type;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {
      int node = _currentNode;
      int nodeType = _nodeType;

      if (nodeType >= DTM.NTYPES) {
        while (true) {
          node = node + 1;

          if (_sp < 0) {
            node = NULL;
            break;
          } else if (node >= _stack[_sp]) {
            if (--_sp < 0) {
              node = NULL;
              break;
            }
          } else if (_exptype(node) == nodeType) {
            break;
          }
        }
      } else {
        int expType;

        while (true) {
          node = node + 1;

          if (_sp < 0) {
            node = NULL;
            break;
          } else if (node >= _stack[_sp]) {
            if (--_sp < 0) {
              node = NULL;
              break;
            }
          } else {
            expType = _exptype(node);
            if (expType < DTM.NTYPES) {
              if (expType == nodeType) {
                break;
              }
            } else {
              if (m_expandedNameTable.getType(expType) == nodeType) {
                break;
              }
            }
          }
        }
      }

      _currentNode = node;

      return (node == NULL) ? NULL : returnNode(makeNodeHandle(node));
    }
  }  // end of TypedPrecedingIterator

  /**
   * Iterator that returns following nodes of for a given node.
   */
  public class FollowingIterator extends InternalAxisIteratorBase
  {
    DTMAxisTraverser m_traverser; // easier for now

    public FollowingIterator()
    {
      m_traverser = getAxisTraverser(Axis.FOLLOWING);
    }

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {
//%HZ%: Added reference to DTMDefaultBase.ROOTNODE back in, temporarily
      if (node == DTMDefaultBase.ROOTNODE)
        node = getDocument();
      if (_isRestartable)
      {
        _startNode = node;

        // ?? -sb
        // find rightmost descendant (or self)
        // int current;
        // while ((node = getLastChild(current = node)) != NULL){}
        // _currentNode = current;
        _currentNode = m_traverser.first(node);

        // _currentNode precedes possible following(node) nodes
        return resetPosition();
      }

      return this;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {

      int node = _currentNode;

      _currentNode = m_traverser.next(_startNode, _currentNode);

      return returnNode(node);
    }
  }  // end of FollowingIterator

  /**
   * Iterator that returns following nodes of a given type for a given node.
   */
  public final class TypedFollowingIterator extends FollowingIterator
  {

    /** The extended type ID that was requested. */
    private final int _nodeType;

    /**
     * Constructor TypedFollowingIterator
     *
     *
     * @param type The extended type ID being requested.
     */
    public TypedFollowingIterator(int type)
    {
      _nodeType = type;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {

      int node;

      do{
       node = _currentNode;

      _currentNode = m_traverser.next(_startNode, _currentNode);

      }
      while (node != DTM.NULL
             && (getExpandedTypeID(node) != _nodeType && getNodeType(node) != _nodeType));

      return (node == DTM.NULL ? DTM.NULL :returnNode(node));
    }
  }  // end of TypedFollowingIterator

  /**
   * Iterator that returns the ancestors of a given node in document
   * order.  (NOTE!  This was changed from the XSLTC code!)
   */
  public class AncestorIterator extends InternalAxisIteratorBase
  {
    com.sun.org.apache.xml.internal.utils.NodeVector m_ancestors =
         new com.sun.org.apache.xml.internal.utils.NodeVector();

    int m_ancestorsPos;

    int m_markedPos;

    /** The real start node for this axes, since _startNode will be adjusted. */
    int m_realStartNode;

    /**
     * Get start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @return The root node of the iteration.
     */
    public int getStartNode()
    {
      return m_realStartNode;
    }

    /**
     * True if this iterator has a reversed axis.
     *
     * @return true since this iterator is a reversed axis.
     */
    public final boolean isReverse()
    {
      return true;
    }

    /**
     * Returns a deep copy of this iterator.  The cloned iterator is not reset.
     *
     * @return a deep copy of this iterator.
     */
    public DTMAxisIterator cloneIterator()
    {
      _isRestartable = false;  // must set to false for any clone

      try
      {
        final AncestorIterator clone = (AncestorIterator) super.clone();

        clone._startNode = _startNode;

        // return clone.reset();
        return clone;
      }
      catch (CloneNotSupportedException e)
      {
        throw new DTMException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ITERATOR_CLONE_NOT_SUPPORTED, null)); //"Iterator clone not supported.");
      }
    }

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {
//%HZ%: Added reference to DTMDefaultBase.ROOTNODE back in, temporarily
      if (node == DTMDefaultBase.ROOTNODE)
        node = getDocument();
      m_realStartNode = node;

      if (_isRestartable)
      {
        int nodeID = makeNodeIdentity(node);

        if (!_includeSelf && node != DTM.NULL) {
          nodeID = _parent(nodeID);
          node = makeNodeHandle(nodeID);
        }

        _startNode = node;

        while (nodeID != END) {
          m_ancestors.addElement(node);
          nodeID = _parent(nodeID);
          node = makeNodeHandle(nodeID);
        }
        m_ancestorsPos = m_ancestors.size()-1;

        _currentNode = (m_ancestorsPos>=0)
                               ? m_ancestors.elementAt(m_ancestorsPos)
                               : DTM.NULL;

        return resetPosition();
      }

      return this;
    }

    /**
     * Resets the iterator to the last start node.
     *
     * @return A DTMAxisIterator, which may or may not be the same as this
     *         iterator.
     */
    public DTMAxisIterator reset()
    {

      m_ancestorsPos = m_ancestors.size()-1;

      _currentNode = (m_ancestorsPos>=0) ? m_ancestors.elementAt(m_ancestorsPos)
                                         : DTM.NULL;

      return resetPosition();
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {

      int next = _currentNode;

      int pos = --m_ancestorsPos;

      _currentNode = (pos >= 0) ? m_ancestors.elementAt(m_ancestorsPos)
                                : DTM.NULL;

      return returnNode(next);
    }

    public void setMark() {
        m_markedPos = m_ancestorsPos;
    }

    public void gotoMark() {
        m_ancestorsPos = m_markedPos;
        _currentNode = m_ancestorsPos>=0 ? m_ancestors.elementAt(m_ancestorsPos)
                                         : DTM.NULL;
    }
  }  // end of AncestorIterator

  /**
   * Typed iterator that returns the ancestors of a given node.
   */
  public final class TypedAncestorIterator extends AncestorIterator
  {

    /** The extended type ID that was requested. */
    private final int _nodeType;

    /**
     * Constructor TypedAncestorIterator
     *
     *
     * @param type The extended type ID being requested.
     */
    public TypedAncestorIterator(int type)
    {
      _nodeType = type;
    }

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {
//%HZ%: Added reference to DTMDefaultBase.ROOTNODE back in, temporarily
      if (node == DTMDefaultBase.ROOTNODE)
        node = getDocument();
      m_realStartNode = node;

      if (_isRestartable)
      {
        int nodeID = makeNodeIdentity(node);
        int nodeType = _nodeType;

        if (!_includeSelf && node != DTM.NULL) {
          nodeID = _parent(nodeID);
        }

        _startNode = node;

        if (nodeType >= DTM.NTYPES) {
          while (nodeID != END) {
            int eType = _exptype(nodeID);

            if (eType == nodeType) {
              m_ancestors.addElement(makeNodeHandle(nodeID));
            }
            nodeID = _parent(nodeID);
          }
        } else {
          while (nodeID != END) {
            int eType = _exptype(nodeID);

            if ((eType >= DTM.NTYPES
                    && m_expandedNameTable.getType(eType) == nodeType)
                || (eType < DTM.NTYPES && eType == nodeType)) {
              m_ancestors.addElement(makeNodeHandle(nodeID));
            }
            nodeID = _parent(nodeID);
          }
        }
        m_ancestorsPos = m_ancestors.size()-1;

        _currentNode = (m_ancestorsPos>=0)
                               ? m_ancestors.elementAt(m_ancestorsPos)
                               : DTM.NULL;

        return resetPosition();
      }

      return this;
    }
  }  // end of TypedAncestorIterator

  /**
   * Iterator that returns the descendants of a given node.
   */
  public class DescendantIterator extends InternalAxisIteratorBase
  {

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {
//%HZ%: Added reference to DTMDefaultBase.ROOTNODE back in, temporarily
      if (node == DTMDefaultBase.ROOTNODE)
        node = getDocument();
      if (_isRestartable)
      {
        node = makeNodeIdentity(node);
        _startNode = node;

        if (_includeSelf)
          node--;

        _currentNode = node;

        return resetPosition();
      }

      return this;
    }

    /**
     * Tell if this node identity is a descendant.  Assumes that
     * the node info for the element has already been obtained.
     *
     * This one-sided test works only if the parent has been
     * previously tested and is known to be a descendent. It fails if
     * the parent is the _startNode's next sibling, or indeed any node
     * that follows _startNode in document order.  That may suffice
     * for this iterator, but it's not really an isDescendent() test.
     * %REVIEW% rename?
     *
     * @param identity The index number of the node in question.
     * @return true if the index is a descendant of _startNode.
     */
    protected boolean isDescendant(int identity)
    {
      return (_parent(identity) >= _startNode) || (_startNode == identity);
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {
      if (_startNode == NULL) {
        return NULL;
      }

      if (_includeSelf && (_currentNode + 1) == _startNode)
          return returnNode(makeNodeHandle(++_currentNode)); // | m_dtmIdent);

      int node = _currentNode;
      int type;

      do {
        node++;
        type = _type(node);

        if (NULL == type ||!isDescendant(node)) {
          _currentNode = NULL;
          return END;
        }
      } while(ATTRIBUTE_NODE == type || TEXT_NODE == type
                 || NAMESPACE_NODE == type);

      _currentNode = node;
      return returnNode(makeNodeHandle(node));  // make handle.
    }

    /**
     * Reset.
     *
     */
  public DTMAxisIterator reset()
  {

    final boolean temp = _isRestartable;

    _isRestartable = true;

    setStartNode(makeNodeHandle(_startNode));

    _isRestartable = temp;

    return this;
  }

  }  // end of DescendantIterator

  /**
   * Typed iterator that returns the descendants of a given node.
   */
  public final class TypedDescendantIterator extends DescendantIterator
  {

    /** The extended type ID that was requested. */
    private final int _nodeType;

    /**
     * Constructor TypedDescendantIterator
     *
     *
     * @param nodeType Extended type ID being requested.
     */
    public TypedDescendantIterator(int nodeType)
    {
      _nodeType = nodeType;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {
      int node;
      int type;

      if (_startNode == NULL) {
        return NULL;
      }

      node = _currentNode;

      do
      {
        node++;
        type = _type(node);

        if (NULL == type ||!isDescendant(node)) {
          _currentNode = NULL;
          return END;
        }
      }
      while (type != _nodeType && _exptype(node) != _nodeType);

      _currentNode = node;
      return returnNode(makeNodeHandle(node));
    }
  }  // end of TypedDescendantIterator

  /**
   * Iterator that returns the descendants of a given node.
   * I'm not exactly clear about this one... -sb
   */
  public class NthDescendantIterator extends DescendantIterator
  {

    /** The current nth position. */
    int _pos;

    /**
     * Constructor NthDescendantIterator
     *
     *
     * @param pos The nth position being requested.
     */
    public NthDescendantIterator(int pos)
    {
      _pos = pos;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {

      // I'm not exactly clear yet what this is doing... -sb
      int node;

      while ((node = super.next()) != END)
      {
        node = makeNodeIdentity(node);

        int parent = _parent(node);
        int child = _firstch(parent);
        int pos = 0;

        do
        {
          int type = _type(child);

          if (ELEMENT_NODE == type)
            pos++;
        }
        while ((pos < _pos) && (child = _nextsib(child)) != END);

        if (node == child)
          return node;
      }

      return (END);
    }
  }  // end of NthDescendantIterator

  /**
   * Class SingletonIterator.
   */
  public class SingletonIterator extends InternalAxisIteratorBase
  {

    /** (not sure yet what this is.  -sb)  (sc & sb remove final to compile in JDK 1.1.8) */
    private boolean _isConstant;

    /**
     * Constructor SingletonIterator
     *
     */
    public SingletonIterator()
    {
      this(Integer.MIN_VALUE, false);
    }

    /**
     * Constructor SingletonIterator
     *
     *
     * @param node The node handle to return.
     */
    public SingletonIterator(int node)
    {
      this(node, false);
    }

    /**
     * Constructor SingletonIterator
     *
     *
     * @param node the node handle to return.
     * @param constant (Not sure what this is yet.  -sb)
     */
    public SingletonIterator(int node, boolean constant)
    {
      _currentNode = _startNode = node;
      _isConstant = constant;
    }

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     *
     * @param node Sets the root of the iteration.
     *
     * @return A DTMAxisIterator set to the start of the iteration.
     */
    public DTMAxisIterator setStartNode(int node)
    {
//%HZ%: Added reference to DTMDefaultBase.ROOTNODE back in, temporarily
      if (node == DTMDefaultBase.ROOTNODE)
        node = getDocument();
      if (_isConstant)
      {
        _currentNode = _startNode;

        return resetPosition();
      }
      else if (_isRestartable)
      {
          _currentNode = _startNode = node;

        return resetPosition();
      }

      return this;
    }

    /**
     * Resets the iterator to the last start node.
     *
     * @return A DTMAxisIterator, which may or may not be the same as this
     *         iterator.
     */
    public DTMAxisIterator reset()
    {

      if (_isConstant)
      {
        _currentNode = _startNode;

        return resetPosition();
      }
      else
      {
        final boolean temp = _isRestartable;

        _isRestartable = true;

        setStartNode(_startNode);

        _isRestartable = temp;
      }

      return this;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {

      final int result = _currentNode;

      _currentNode = END;

      return returnNode(result);
    }
  }  // end of SingletonIterator

  /**
   * Iterator that returns a given node only if it is of a given type.
   */
  public final class TypedSingletonIterator extends SingletonIterator
  {

    /** The extended type ID that was requested. */
    private final int _nodeType;

    /**
     * Constructor TypedSingletonIterator
     *
     *
     * @param nodeType The extended type ID being requested.
     */
    public TypedSingletonIterator(int nodeType)
    {
      _nodeType = nodeType;
    }

    /**
     * Get the next node in the iteration.
     *
     * @return The next node handle in the iteration, or END.
     */
    public int next()
    {

      //final int result = super.next();
      final int result = _currentNode;
      int nodeType = _nodeType;

      _currentNode = END;

      if (nodeType >= DTM.NTYPES) {
        if (getExpandedTypeID(result) == nodeType) {
          return returnNode(result);
        }
      } else {
        if (getNodeType(result) == nodeType) {
          return returnNode(result);
        }
      }

      return NULL;
    }
  }  // end of TypedSingletonIterator
}
