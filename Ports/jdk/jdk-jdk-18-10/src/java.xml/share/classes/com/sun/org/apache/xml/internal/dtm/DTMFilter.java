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

package com.sun.org.apache.xml.internal.dtm;

/**
 * Simple filter for doing node tests.  Note the semantics of this are
 * somewhat different that the DOM's NodeFilter.
 */
public interface DTMFilter
{

  // Constants for whatToShow.  These are used to set the node type that will
  // be traversed. These values may be ORed together before being passed to
  // the DTMIterator.

  /**
   * Show all <code>Nodes</code>.
   */
  public static final int SHOW_ALL = 0xFFFFFFFF;

  /**
   * Show <code>Element</code> nodes.
   */
  public static final int SHOW_ELEMENT = 0x00000001;

  /**
   * Show <code>Attr</code> nodes. This is meaningful only when creating an
   * iterator or tree-walker with an attribute node as its
   * <code>root</code>; in this case, it means that the attribute node
   * will appear in the first position of the iteration or traversal.
   * Since attributes are never children of other nodes, they do not
   * appear when traversing over the main document tree.
   */
  public static final int SHOW_ATTRIBUTE = 0x00000002;

  /**
   * Show <code>Text</code> nodes.
   */
  public static final int SHOW_TEXT = 0x00000004;

  /**
   * Show <code>CDATASection</code> nodes.
   */
  public static final int SHOW_CDATA_SECTION = 0x00000008;

  /**
   * Show <code>EntityReference</code> nodes. Note that if Entity References
   * have been fully expanded while the tree was being constructed, these
   * nodes will not appear and this mask has no effect.
   */
  public static final int SHOW_ENTITY_REFERENCE = 0x00000010;

  /**
   * Show <code>Entity</code> nodes. This is meaningful only when creating
   * an iterator or tree-walker with an<code> Entity</code> node as its
   * <code>root</code>; in this case, it means that the <code>Entity</code>
   *  node will appear in the first position of the traversal. Since
   * entities are not part of the document tree, they do not appear when
   * traversing over the main document tree.
   */
  public static final int SHOW_ENTITY = 0x00000020;

  /**
   * Show <code>ProcessingInstruction</code> nodes.
   */
  public static final int SHOW_PROCESSING_INSTRUCTION = 0x00000040;

  /**
   * Show <code>Comment</code> nodes.
   */
  public static final int SHOW_COMMENT = 0x00000080;

  /**
   * Show <code>Document</code> nodes. (Of course, as with Attributes
   * and such, this is meaningful only when the iteration root is the
   * Document itself, since Document has no parent.)
   */
  public static final int SHOW_DOCUMENT = 0x00000100;

  /**
   * Show <code>DocumentType</code> nodes.
   */
  public static final int SHOW_DOCUMENT_TYPE = 0x00000200;

  /**
   * Show <code>DocumentFragment</code> nodes. (Of course, as with
   * Attributes and such, this is meaningful only when the iteration
   * root is the Document itself, since DocumentFragment has no parent.)
   */
  public static final int SHOW_DOCUMENT_FRAGMENT = 0x00000400;

  /**
   * Show <code>Notation</code> nodes. This is meaningful only when creating
   * an iterator or tree-walker with a <code>Notation</code> node as its
   * <code>root</code>; in this case, it means that the
   * <code>Notation</code> node will appear in the first position of the
   * traversal. Since notations are not part of the document tree, they do
   * not appear when traversing over the main document tree.
   */
  public static final int SHOW_NOTATION = 0x00000800;

  /**

   * This bit instructs the iterator to show namespace nodes, which
   * are modeled by DTM but not by the DOM.  Make sure this does not
   * conflict with {@link org.w3c.dom.traversal.NodeFilter}.
   * <p>
   * %REVIEW% Might be safer to start from higher bits and work down,
   * to leave room for the DOM to expand its set of constants... Or,
   * possibly, to create a DTM-specific field for these additional bits.
   */
  public static final int SHOW_NAMESPACE = 0x00001000;

  /**
   * Special bit for filters implementing match patterns starting with
   * a function.  Make sure this does not conflict with
   * {@link org.w3c.dom.traversal.NodeFilter}.
   * <p>
   * %REVIEW% Might be safer to start from higher bits and work down,
   * to leave room for the DOM to expand its set of constants... Or,
   * possibly, to create a DTM-specific field for these additional bits.
   */
  public static final int SHOW_BYFUNCTION = 0x00010000;

  /**
   * Test whether a specified node is visible in the logical view of a
   * <code>DTMIterator</code>. Normally, this function
   * will be called by the implementation of <code>DTMIterator</code>;
   * it is not normally called directly from
   * user code.
   *
   * @param nodeHandle int Handle of the node.
   * @param whatToShow one of SHOW_XXX values.
   * @return one of FILTER_ACCEPT, FILTER_REJECT, or FILTER_SKIP.
   */
  public short acceptNode(int nodeHandle, int whatToShow);

  /**
   * Test whether a specified node is visible in the logical view of a
   * <code>DTMIterator</code>. Normally, this function
   * will be called by the implementation of <code>DTMIterator</code>;
   * it is not normally called directly from
   * user code.
   * <p>
   * TODO: Should this be setNameMatch(expandedName) followed by accept()?
   * Or will we really be testing a different name at every invocation?
   *
   * <p>%REVIEW% Under what circumstances will this be used? The cases
   * I've considered are just as easy and just about as efficient if
   * the name test is performed in the DTMIterator... -- Joe</p>
   *
   * <p>%REVIEW% Should that 0xFFFF have a mnemonic assigned to it?
   * Also: This representation is assuming the expanded name is indeed
   * split into high/low 16-bit halfwords. If we ever change the
   * balance between namespace and localname bits (eg because we
   * decide there are many more localnames than namespaces, which is
   * fairly likely), this is going to break. It might be safer to
   * encapsulate the details with a makeExpandedName method and make
   * that responsible for setting up the wildcard version as well.</p>
   *
   * @param nodeHandle int Handle of the node.
   * @param whatToShow one of SHOW_XXX values.
   * @param expandedName a value defining the exanded name as defined in
   *                     the DTM interface.  Wild cards will be defined
   *                     by 0xFFFF in the namespace and/or localname
   *                     portion of the expandedName.
   * @return one of FILTER_ACCEPT, FILTER_REJECT, or FILTER_SKIP.  */
  public short acceptNode(int nodeHandle, int whatToShow, int expandedName);

}
