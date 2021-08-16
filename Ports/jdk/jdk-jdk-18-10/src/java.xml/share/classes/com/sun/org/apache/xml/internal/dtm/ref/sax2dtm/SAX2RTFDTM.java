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

package com.sun.org.apache.xml.internal.dtm.ref.sax2dtm;

import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.dtm.DTMManager;
import com.sun.org.apache.xml.internal.dtm.DTMWSFilter;
import com.sun.org.apache.xml.internal.utils.IntStack;
import com.sun.org.apache.xml.internal.utils.IntVector;
import com.sun.org.apache.xml.internal.utils.StringVector;
import com.sun.org.apache.xml.internal.utils.XMLStringFactory;
import java.util.Vector;
import javax.xml.transform.Source;
import org.xml.sax.SAXException;

/**
 * This is a subclass of SAX2DTM which has been modified to meet the needs of
 * Result Tree Frameworks (RTFs). The differences are:
 *
 * 1) Multiple XML trees may be appended to the single DTM. This means
 * that the root node of each document is _not_ node 0. Some code has
 * had to be deoptimized to support this mode of operation, and an
 * explicit mechanism for obtaining the Node Handle of the root node
 * has been provided.
 *
 * 2) A stack of these documents is maintained, allowing us to "tail-prune" the
 * most recently added trees off the end of the DTM as stylesheet elements
 * (and thus variable contexts) are exited.
 *
 * PLEASE NOTE that this class may be _heavily_ dependent upon the
 * internals of the SAX2DTM superclass, and must be maintained in
 * parallel with that code.  Arguably, they should be conditionals
 * within a single class... but they have deen separated for
 * performance reasons. (In fact, one could even argue about which is
 * the superclass and which is the subclass; the current arrangement
 * is as much about preserving stability of existing code during
 * development as anything else.)
 *
 * %REVIEW% In fact, since the differences are so minor, I think it
 * may be possible/practical to fold them back into the base
 * SAX2DTM. Consider that as a future code-size optimization.
 *
 * @LastModified: Oct 2017
 */
public class SAX2RTFDTM extends SAX2DTM
{
  /** Set true to monitor SAX events and similar diagnostic info. */
  private static final boolean DEBUG = false;

  /** Most recently started Document, or null if the DTM is empty.  */
  private int m_currentDocumentNode=NULL;

  /** Tail-pruning mark: Number of nodes in use */
  IntStack mark_size=new IntStack();
  /** Tail-pruning mark: Number of data items in use */
  IntStack mark_data_size=new IntStack();
  /** Tail-pruning mark: Number of size-of-data fields in use */
  IntStack mark_char_size=new IntStack();
  /** Tail-pruning mark: Number of dataOrQName slots in use */
  IntStack mark_doq_size=new IntStack();
  /** Tail-pruning mark: Number of namespace declaration sets in use
   * %REVIEW% I don't think number of NS sets is ever different from number
   * of NS elements. We can probabably reduce these to a single stack and save
   * some storage.
   * */
  IntStack mark_nsdeclset_size=new IntStack();
  /** Tail-pruning mark: Number of naespace declaration elements in use
   * %REVIEW% I don't think number of NS sets is ever different from number
   * of NS elements. We can probabably reduce these to a single stack and save
   * some storage.
   */
  IntStack mark_nsdeclelem_size=new IntStack();

  /**
   * Tail-pruning mark:  initial number of nodes in use
   */
  int m_emptyNodeCount;

  /**
   * Tail-pruning mark:  initial number of namespace declaration sets
   */
  int m_emptyNSDeclSetCount;

  /**
   * Tail-pruning mark:  initial number of namespace declaration elements
   */
  int m_emptyNSDeclSetElemsCount;

  /**
   * Tail-pruning mark:  initial number of data items in use
   */
  int m_emptyDataCount;

  /**
   * Tail-pruning mark:  initial number of characters in use
   */
  int m_emptyCharsCount;

  /**
   * Tail-pruning mark:  default initial number of dataOrQName slots in use
   */
  int m_emptyDataQNCount;

  public SAX2RTFDTM(DTMManager mgr, Source source, int dtmIdentity,
                 DTMWSFilter whiteSpaceFilter,
                 XMLStringFactory xstringfactory,
                 boolean doIndexing)
  {
    super(mgr, source, dtmIdentity, whiteSpaceFilter,
          xstringfactory, doIndexing);

    // NEVER track source locators for RTFs; they aren't meaningful. I think.
    // (If we did track them, we'd need to tail-prune these too.)
    //com.sun.org.apache.xalan.internal.processor.TransformerFactoryImpl.m_source_location;
    m_useSourceLocationProperty=false;
    m_sourceSystemId = (m_useSourceLocationProperty) ? new StringVector()
                                                     : null;
    m_sourceLine = (m_useSourceLocationProperty) ? new IntVector() : null;
    m_sourceColumn = (m_useSourceLocationProperty) ? new IntVector() : null;

    // Record initial sizes of fields that are pushed and restored
    // for RTF tail-pruning.  More entries can be popped than pushed, so
    // we need this to mark the primordial state of the DTM.
    m_emptyNodeCount = m_size;
    m_emptyNSDeclSetCount = (m_namespaceDeclSets == null)
                                 ? 0 : m_namespaceDeclSets.size();
    m_emptyNSDeclSetElemsCount = (m_namespaceDeclSetElements == null)
                                      ? 0 : m_namespaceDeclSetElements.size();
    m_emptyDataCount = m_data.size();
    m_emptyCharsCount = m_chars.size();
    m_emptyDataQNCount = m_dataOrQName.size();
  }

  /**
   * Given a DTM, find the owning document node. In the case of
   * SAX2RTFDTM, which may contain multiple documents, this returns
   * the <b>most recently started</b> document, or null if the DTM is
   * empty or no document is currently under construction.
   *
   * %REVIEW% Should we continue to report the most recent after
   * construction has ended? I think not, given that it may have been
   * tail-pruned.
   *
   *  @return int Node handle of Document node, or null if this DTM does not
   *  contain an "active" document.
   * */
  public int getDocument()
  {
    return makeNodeHandle(m_currentDocumentNode);
  }

  /**
   * Given a node handle, find the owning document node, using DTM semantics
   * (Document owns itself) rather than DOM semantics (Document has no owner).
   *
   * (I'm counting on the fact that getOwnerDocument() is implemented on top
   * of this call, in the superclass, to avoid having to rewrite that one.
   * Be careful if that code changes!)
   *
   * @param nodeHandle the id of the node.
   * @return int Node handle of owning document
   */
  public int getDocumentRoot(int nodeHandle)
  {
    for (int id=makeNodeIdentity(nodeHandle); id!=NULL; id=_parent(id)) {
      if (_type(id)==DTM.DOCUMENT_NODE) {
        return makeNodeHandle(id);
      }
    }

    return DTM.NULL; // Safety net; should never happen
  }

  /**
   * Given a node identifier, find the owning document node.  Unlike the DOM,
   * this considers the owningDocument of a Document to be itself. Note that
   * in shared DTMs this may not be zero.
   *
   * @param nodeIdentifier the id of the starting node.
   * @return int Node identifier of the root of this DTM tree
   */
  protected int _documentRoot(int nodeIdentifier)
  {
    if(nodeIdentifier==NULL) return NULL;

    for (int parent=_parent(nodeIdentifier);
         parent!=NULL;
         nodeIdentifier=parent,parent=_parent(nodeIdentifier))
      ;

    return nodeIdentifier;
  }

  /**
   * Receive notification of the beginning of a new RTF document.
   *
   * %REVIEW% Y'know, this isn't all that much of a deoptimization. We
   * might want to consider folding the start/endDocument changes back
   * into the main SAX2DTM so we don't have to expose so many fields
   * (even as Protected) and carry the additional code.
   *
   * @throws SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see org.xml.sax.ContentHandler#startDocument
   * */
  public void startDocument() throws SAXException
  {
    // Re-initialize the tree append process
    m_endDocumentOccured = false;
    m_prefixMappings = new Vector<>();
    m_contextIndexes = new IntStack();
    m_parents = new IntStack();

    m_currentDocumentNode=m_size;
    super.startDocument();
  }

  /**
   * Receive notification of the end of the document.
   *
   * %REVIEW% Y'know, this isn't all that much of a deoptimization. We
   * might want to consider folding the start/endDocument changes back
   * into the main SAX2DTM so we don't have to expose so many fields
   * (even as Protected).
   *
   * @throws SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see org.xml.sax.ContentHandler#endDocument
   * */
  public void endDocument() throws SAXException
  {
    charactersFlush();

    m_nextsib.setElementAt(NULL,m_currentDocumentNode);

    if (m_firstch.elementAt(m_currentDocumentNode) == NOTPROCESSED)
      m_firstch.setElementAt(NULL,m_currentDocumentNode);

    if (DTM.NULL != m_previous)
      m_nextsib.setElementAt(DTM.NULL,m_previous);

    m_parents = null;
    m_prefixMappings = null;
    m_contextIndexes = null;

    m_currentDocumentNode= NULL; // no longer open
    m_endDocumentOccured = true;
  }


  /** "Tail-pruning" support for RTFs.
   *
   * This function pushes information about the current size of the
   * DTM's data structures onto a stack, for use by popRewindMark()
   * (which see).
   *
   * %REVIEW% I have no idea how to rewind m_elemIndexes. However,
   * RTFs will not be indexed, so I can simply panic if that case
   * arises. Hey, it works...
   * */
  public void pushRewindMark()
  {
    if(m_indexing || m_elemIndexes!=null)
      throw new java.lang.NullPointerException("Coding error; Don't try to mark/rewind an indexed DTM");

    // Values from DTMDefaultBase
    // %REVIEW% Can the namespace stack sizes ever differ? If not, save space!
    mark_size.push(m_size);
    mark_nsdeclset_size.push((m_namespaceDeclSets==null)
                                   ? 0
                                   : m_namespaceDeclSets.size());
    mark_nsdeclelem_size.push((m_namespaceDeclSetElements==null)
                                   ? 0
                                   : m_namespaceDeclSetElements.size());

    // Values from SAX2DTM
    mark_data_size.push(m_data.size());
    mark_char_size.push(m_chars.size());
    mark_doq_size.push(m_dataOrQName.size());
  }

  /** "Tail-pruning" support for RTFs.
   *
   * This function pops the information previously saved by
   * pushRewindMark (which see) and uses it to discard all nodes added
   * to the DTM after that time. We expect that this will allow us to
   * reuse storage more effectively.
   *
   * This is _not_ intended to be called while a document is still being
   * constructed -- only between endDocument and the next startDocument
   *
   * %REVIEW% WARNING: This is the first use of some of the truncation
   * methods.  If Xalan blows up after this is called, that's a likely
   * place to check.
   *
   * %REVIEW% Our original design for DTMs permitted them to share
   * string pools.  If there any risk that this might be happening, we
   * can _not_ rewind and recover the string storage. One solution
   * might to assert that DTMs used for RTFs Must Not take advantage
   * of that feature, but this seems excessively fragile. Another, much
   * less attractive, would be to just let them leak... Nah.
   *
   * @return true if and only if the pop completely emptied the
   * RTF. That response is used when determining how to unspool
   * RTF-started-while-RTF-open situations.
   * */
  public boolean popRewindMark()
  {
    boolean top=mark_size.empty();

    m_size=top ? m_emptyNodeCount : mark_size.pop();
    m_exptype.setSize(m_size);
    m_firstch.setSize(m_size);
    m_nextsib.setSize(m_size);
    m_prevsib.setSize(m_size);
    m_parent.setSize(m_size);

    m_elemIndexes=null;

    int ds= top ? m_emptyNSDeclSetCount : mark_nsdeclset_size.pop();
    if (m_namespaceDeclSets!=null) {
      m_namespaceDeclSets.setSize(ds);
    }

    int ds1= top ? m_emptyNSDeclSetElemsCount : mark_nsdeclelem_size.pop();
    if (m_namespaceDeclSetElements!=null) {
      m_namespaceDeclSetElements.setSize(ds1);
    }

    // Values from SAX2DTM - m_data always has a reserved entry
    m_data.setSize(top ? m_emptyDataCount : mark_data_size.pop());
    m_chars.setLength(top ? m_emptyCharsCount : mark_char_size.pop());
    m_dataOrQName.setSize(top ? m_emptyDataQNCount : mark_doq_size.pop());

    // Return true iff DTM now empty
    return m_size==0;
  }

  /** @return true if a DTM tree is currently under construction.
   * */
  public boolean isTreeIncomplete()
  {
    return !m_endDocumentOccured;
  }
}
