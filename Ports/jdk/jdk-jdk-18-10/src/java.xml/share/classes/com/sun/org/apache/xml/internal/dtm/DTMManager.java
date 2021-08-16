/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xml.internal.dtm;

import com.sun.org.apache.xml.internal.utils.PrefixResolver;
import com.sun.org.apache.xml.internal.utils.XMLStringFactory;

/**
 * A DTMManager instance can be used to create DTM and
 * DTMIterator objects, and manage the DTM objects in the system.
 *
 * <p>The system property that determines which Factory implementation
 * to create is named "com.sun.org.apache.xml.internal.utils.DTMFactory". This
 * property names a concrete subclass of the DTMFactory abstract
 *  class. If the property is not defined, a platform default is be used.</p>
 *
 * <p>An instance of this class <emph>must</emph> be safe to use across
 * thread instances.  It is expected that a client will create a single instance
 * of a DTMManager to use across multiple threads.  This will allow sharing
 * of DTMs across multiple processes.</p>
 *
 * <p>Note: this class is incomplete right now.  It will be pretty much
 * modeled after javax.xml.transform.TransformerFactory in terms of its
 * factory support.</p>
 *
 * <p>State: In progress!!</p>
 */
public abstract class DTMManager
{

  /**
   * Factory for creating XMLString objects.
   *  %TBD% Make this set by the caller.
   */
  protected XMLStringFactory m_xsf = null;

  private boolean _overrideDefaultParser;
  /**
   * Default constructor is protected on purpose.
   */
  protected DTMManager(){}

  /**
   * Get the XMLStringFactory used for the DTMs.
   *
   *
   * @return a valid XMLStringFactory object, or null if it hasn't been set yet.
   */
  public XMLStringFactory getXMLStringFactory()
  {
    return m_xsf;
  }

  /**
   * Set the XMLStringFactory used for the DTMs.
   *
   *
   * @param xsf a valid XMLStringFactory object, should not be null.
   */
  public void setXMLStringFactory(XMLStringFactory xsf)
  {
    m_xsf = xsf;
  }

  /**
   * Obtain a new instance of a <code>DTMManager</code>.
   * This static method creates a new factory instance
   * using the default <code>DTMManager</code> implementation, which is
   * <code>com.sun.org.apache.xml.internal.dtm.ref.DTMManagerDefault</code>.
   * </li>
   * </ul>
   *
   * Once an application has obtained a reference to a <code>
   * DTMManager</code> it can use the factory to configure
   * and obtain parser instances.
   *
   * @return new DTMManager instance, never null.
   *
   * @throws DTMException
   * if the implementation is not available or cannot be instantiated.
   */
  public static DTMManager newInstance(XMLStringFactory xsf)
           throws DTMException
  {
      final DTMManager factoryImpl = new com.sun.org.apache.xml.internal.dtm.ref.DTMManagerDefault();
      factoryImpl.setXMLStringFactory(xsf);

      return factoryImpl;
  }

  /**
   * Get an instance of a DTM, loaded with the content from the
   * specified source.  If the unique flag is true, a new instance will
   * always be returned.  Otherwise it is up to the DTMManager to return a
   * new instance or an instance that it already created and may be being used
   * by someone else.
   *
   * (More parameters may eventually need to be added for error handling
   * and entity resolution, and to better control selection of implementations.)
   *
   * @param source the specification of the source object, which may be null,
   *               in which case it is assumed that node construction will take
   *               by some other means.
   * @param unique true if the returned DTM must be unique, probably because it
   * is going to be mutated.
   * @param whiteSpaceFilter Enables filtering of whitespace nodes, and may
   *                         be null.
   * @param incremental true if the DTM should be built incrementally, if
   *                    possible.
   * @param doIndexing true if the caller considers it worth it to use
   *                   indexing schemes.
   *
   * @return a non-null DTM reference.
   */
  public abstract DTM getDTM(javax.xml.transform.Source source,
                             boolean unique, DTMWSFilter whiteSpaceFilter,
                             boolean incremental, boolean doIndexing);

  /**
   * Get the instance of DTM that "owns" a node handle.
   *
   * @param nodeHandle the nodeHandle.
   *
   * @return a non-null DTM reference.
   */
  public abstract DTM getDTM(int nodeHandle);

  /**
   * Given a W3C DOM node, try and return a DTM handle.
   * Note: calling this may be non-optimal.
   *
   * @param node Non-null reference to a DOM node.
   *
   * @return a valid DTM handle.
   */
  public abstract int getDTMHandleFromNode(org.w3c.dom.Node node);

  /**
   * Creates a DTM representing an empty <code>DocumentFragment</code> object.
   * @return a non-null DTM reference.
   */
  public abstract DTM createDocumentFragment();

  /**
   * Release a DTM either to a lru pool, or completely remove reference.
   * DTMs without system IDs are always hard deleted.
   * State: experimental.
   *
   * @param dtm The DTM to be released.
   * @param shouldHardDelete True if the DTM should be removed no matter what.
   * @return true if the DTM was removed, false if it was put back in a lru pool.
   */
  public abstract boolean release(DTM dtm, boolean shouldHardDelete);

  /**
   * Create a new <code>DTMIterator</code> based on an XPath
   * <a href="http://www.w3.org/TR/xpath#NT-LocationPath>LocationPath</a> or
   * a <a href="http://www.w3.org/TR/xpath#NT-UnionExpr">UnionExpr</a>.
   *
   * @param xpathCompiler ??? Somehow we need to pass in a subpart of the
   * expression.  I hate to do this with strings, since the larger expression
   * has already been parsed.
   *
   * @param pos The position in the expression.
   * @return The newly created <code>DTMIterator</code>.
   */
  public abstract DTMIterator createDTMIterator(Object xpathCompiler,
          int pos);

  /**
   * Create a new <code>DTMIterator</code> based on an XPath
   * <a href="http://www.w3.org/TR/xpath#NT-LocationPath>LocationPath</a> or
   * a <a href="http://www.w3.org/TR/xpath#NT-UnionExpr">UnionExpr</a>.
   *
   * @param xpathString Must be a valid string expressing a
   * <a href="http://www.w3.org/TR/xpath#NT-LocationPath>LocationPath</a> or
   * a <a href="http://www.w3.org/TR/xpath#NT-UnionExpr">UnionExpr</a>.
   *
   * @param presolver An object that can resolve prefixes to namespace URLs.
   *
   * @return The newly created <code>DTMIterator</code>.
   */
  public abstract DTMIterator createDTMIterator(String xpathString,
          PrefixResolver presolver);

  /**
   * Create a new <code>DTMIterator</code> based only on a whatToShow
   * and a DTMFilter.  The traversal semantics are defined as the
   * descendant access.
   * <p>
   * Note that DTMIterators may not be an exact match to DOM
   * NodeIterators. They are initialized and used in much the same way
   * as a NodeIterator, but their response to document mutation is not
   * currently defined.
   *
   * @param whatToShow This flag specifies which node types may appear in
   *   the logical view of the tree presented by the iterator. See the
   *   description of <code>NodeFilter</code> for the set of possible
   *   <code>SHOW_</code> values.These flags can be combined using
   *   <code>OR</code>.
   * @param filter The <code>NodeFilter</code> to be used with this
   *   <code>DTMFilter</code>, or <code>null</code> to indicate no filter.
   * @param entityReferenceExpansion The value of this flag determines
   *   whether entity reference nodes are expanded.
   *
   * @return The newly created <code>DTMIterator</code>.
   */
  public abstract DTMIterator createDTMIterator(int whatToShow,
          DTMFilter filter, boolean entityReferenceExpansion);

  /**
   * Create a new <code>DTMIterator</code> that holds exactly one node.
   *
   * @param node The node handle that the DTMIterator will iterate to.
   *
   * @return The newly created <code>DTMIterator</code>.
   */
  public abstract DTMIterator createDTMIterator(int node);

  /* Flag indicating whether an incremental transform is desired */
  public boolean m_incremental = false;

  /*
   * Flag set by FEATURE_SOURCE_LOCATION.
   * This feature specifies whether the transformation phase should
   * keep track of line and column numbers for the input source
   * document.
   */
  public boolean m_source_location = false;

  /**
   * Get a flag indicating whether an incremental transform is desired
   * @return incremental boolean.
   *
   */
  public boolean getIncremental()
  {
    return m_incremental;
  }

  /**
   * Set a flag indicating whether an incremental transform is desired
   * This flag should have the same value as the FEATURE_INCREMENTAL feature
   * which is set by the TransformerFactory.setAttribut() method before a
   * DTMManager is created
   * @param incremental boolean to use to set m_incremental.
   *
   */
  public void setIncremental(boolean incremental)
  {
    m_incremental = incremental;
  }

  /**
   * Get a flag indicating whether the transformation phase should
   * keep track of line and column numbers for the input source
   * document.
   * @return source location boolean
   *
   */
  public boolean getSource_location()
  {
    return m_source_location;
  }

  /**
   * Set a flag indicating whether the transformation phase should
   * keep track of line and column numbers for the input source
   * document.
   * This flag should have the same value as the FEATURE_SOURCE_LOCATION feature
   * which is set by the TransformerFactory.setAttribut() method before a
   * DTMManager is created
   * @param sourceLocation boolean to use to set m_source_location
   */
  public void setSource_location(boolean sourceLocation){
    m_source_location = sourceLocation;
  }

    /**
     * Return the state of the services mechanism feature.
     */
    public boolean overrideDefaultParser() {
        return _overrideDefaultParser;
    }

    /**
     * Set the state of the services mechanism feature.
     */
    public void setOverrideDefaultParser(boolean flag) {
        _overrideDefaultParser = flag;
    }

  // -------------------- private methods --------------------

  /** This value, set at compile time, controls how many bits of the
   * DTM node identifier numbers are used to identify a node within a
   * document, and thus sets the maximum number of nodes per
   * document. The remaining bits are used to identify the DTM
   * document which contains this node.
   *
   * If you change IDENT_DTM_NODE_BITS, be sure to rebuild _ALL_ the
   * files which use it... including the IDKey testcases.
   *
   * (FuncGenerateKey currently uses the node identifier directly and
   * thus is affected when this changes. The IDKEY results will still be
   * _correct_ (presuming no other breakage), but simple equality
   * comparison against the previous "golden" files will probably
   * complain.)
   * */
  public static final int IDENT_DTM_NODE_BITS = 16;


  /** When this bitmask is ANDed with a DTM node handle number, the result
   * is the low bits of the node's index number within that DTM. To obtain
   * the high bits, add the DTM ID portion's offset as assigned in the DTM
   * Manager.
   */
  public static final int IDENT_NODE_DEFAULT = (1<<IDENT_DTM_NODE_BITS)-1;


  /** When this bitmask is ANDed with a DTM node handle number, the result
   * is the DTM's document identity number.
   */
  public static final int IDENT_DTM_DEFAULT = ~IDENT_NODE_DEFAULT;

  /** This is the maximum number of DTMs available.  The highest DTM is
    * one less than this.
   */
  public static final int IDENT_MAX_DTMS = (IDENT_DTM_DEFAULT >>> IDENT_DTM_NODE_BITS) + 1;


  /**
   * %TBD% Doc
   *
   * NEEDSDOC @param dtm
   *
   * NEEDSDOC ($objectName$) @return
   */
  public abstract int getDTMIdentity(DTM dtm);

  /**
   * %TBD% Doc
   *
   * NEEDSDOC ($objectName$) @return
   */
  public int getDTMIdentityMask()
  {
    return IDENT_DTM_DEFAULT;
  }

  /**
   * %TBD% Doc
   *
   * NEEDSDOC ($objectName$) @return
   */
  public int getNodeIdentityMask()
  {
    return IDENT_NODE_DEFAULT;
  }
}
