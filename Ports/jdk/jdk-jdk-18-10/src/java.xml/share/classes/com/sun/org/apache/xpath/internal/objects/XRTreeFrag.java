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

package com.sun.org.apache.xpath.internal.objects;

import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.dtm.DTMIterator;
import com.sun.org.apache.xml.internal.utils.XMLString;
import com.sun.org.apache.xpath.internal.Expression;
import com.sun.org.apache.xpath.internal.ExpressionNode;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.axes.RTFIterator;

import org.w3c.dom.NodeList;

/**
 * This class represents an XPath result tree fragment object, and is capable of
 * converting the RTF to other types, such as a string.
 * @xsl.usage general
 */
public class XRTreeFrag extends XObject implements Cloneable
{
    static final long serialVersionUID = -3201553822254911567L;
  private DTMXRTreeFrag m_DTMXRTreeFrag;
  private int m_dtmRoot = DTM.NULL;
  protected boolean m_allowRelease = false;


  /**
   * Create an XRTreeFrag Object.
   *
   */
  public XRTreeFrag(int root, XPathContext xctxt, ExpressionNode parent)
  {
    super(null);
    exprSetParent(parent);
    initDTM(root, xctxt);
  }

  /**
   * Create an XRTreeFrag Object.
   *
   */
  public XRTreeFrag(int root, XPathContext xctxt)
  {
    super(null);
   initDTM(root, xctxt);
  }

  private final void initDTM(int root, XPathContext xctxt){
    m_dtmRoot = root;
    final DTM dtm = xctxt.getDTM(root);
    if(dtm != null){
      m_DTMXRTreeFrag = xctxt.getDTMXRTreeFrag(xctxt.getDTMIdentity(dtm));
    }
  }

  /**
   * Return a java object that's closest to the representation
   * that should be handed to an extension.
   *
   * @return The object that this class wraps
   */
  public Object object()
  {
    if (m_DTMXRTreeFrag.getXPathContext() != null)
      return new com.sun.org.apache.xml.internal.dtm.ref.DTMNodeIterator((DTMIterator)(new com.sun.org.apache.xpath.internal.NodeSetDTM(m_dtmRoot, m_DTMXRTreeFrag.getXPathContext().getDTMManager())));
    else
      return super.object();
  }

  /**
   * Create an XRTreeFrag Object.
   *
   */
  public XRTreeFrag(Expression expr)
  {
    super(expr);
  }

  /**
   * Specify if it's OK for detach to release the iterator for reuse.
   *
   * @param allowRelease true if it is OK for detach to release this iterator
   * for pooling.
   */
  public void allowDetachToRelease(boolean allowRelease)
  {
    m_allowRelease = allowRelease;
  }

  /**
   * Detaches the <code>DTMIterator</code> from the set which it iterated
   * over, releasing any computational resources and placing the iterator
   * in the INVALID state. After <code>detach</code> has been invoked,
   * calls to <code>nextNode</code> or <code>previousNode</code> will
   * raise a runtime exception.
   *
   * In general, detach should only be called once on the object.
   */
  public void detach(){
    if(m_allowRelease){
        m_DTMXRTreeFrag.destruct();
      setObject(null);
    }
  }

  /**
   * Tell what kind of class this is.
   *
   * @return type CLASS_RTREEFRAG
   */
  public int getType()
  {
    return CLASS_RTREEFRAG;
  }

  /**
   * Given a request type, return the equivalent string.
   * For diagnostic purposes.
   *
   * @return type string "#RTREEFRAG"
   */
  public String getTypeString()
  {
    return "#RTREEFRAG";
  }

  /**
   * Cast result object to a number.
   *
   * @return The result tree fragment as a number or NaN
   */
  public double num()
    throws javax.xml.transform.TransformerException
  {

    XMLString s = xstr();

    return s.toDouble();
  }

  /**
   * Cast result object to a boolean.  This always returns true for a RTreeFrag
   * because it is treated like a node-set with a single root node.
   *
   * @return true
   */
  public boolean bool()
  {
    return true;
  }

  private XMLString m_xmlStr = null;

  /**
   * Cast result object to an XMLString.
   *
   * @return The document fragment node data or the empty string.
   */
  public XMLString xstr()
  {
    if(null == m_xmlStr)
      m_xmlStr = m_DTMXRTreeFrag.getDTM().getStringValue(m_dtmRoot);

    return m_xmlStr;
  }

  /**
   * Cast result object to a string.
   *
   * @return The string this wraps or the empty string if null
   */
  public void appendToFsb(com.sun.org.apache.xml.internal.utils.FastStringBuffer fsb)
  {
    XString xstring = (XString)xstr();
    xstring.appendToFsb(fsb);
  }


  /**
   * Cast result object to a string.
   *
   * @return The document fragment node data or the empty string.
   */
  public String str()
  {
    String str = m_DTMXRTreeFrag.getDTM().getStringValue(m_dtmRoot).toString();

    return (null == str) ? "" : str;
  }

  /**
   * Cast result object to a result tree fragment.
   *
   * @return The document fragment this wraps
   */
  public int rtf()
  {
    return m_dtmRoot;
  }

  /**
   * Cast result object to a DTMIterator.
   * dml - modified to return an RTFIterator for
   * benefit of EXSLT object-type function in
   * {@link com.sun.org.apache.xalan.internal.lib.ExsltCommon}.
   * @return The document fragment as a DTMIterator
   */
  public DTMIterator asNodeIterator()
  {
    return new RTFIterator(m_dtmRoot, m_DTMXRTreeFrag.getXPathContext().getDTMManager());
  }

  /**
   * Cast result object to a nodelist. (special function).
   *
   * @return The document fragment as a nodelist
   */
  public NodeList convertToNodeset()
  {

    if (m_obj instanceof NodeList)
      return (NodeList) m_obj;
    else
      return new com.sun.org.apache.xml.internal.dtm.ref.DTMNodeList(asNodeIterator());
  }

  /**
   * Tell if two objects are functionally equal.
   *
   * @param obj2 Object to compare this to
   *
   * @return True if the two objects are equal
   *
   * @throws javax.xml.transform.TransformerException
   */
  public boolean equals(XObject obj2)
  {

    try
    {
      if (XObject.CLASS_NODESET == obj2.getType())
      {

        // In order to handle the 'all' semantics of
        // nodeset comparisons, we always call the
        // nodeset function.
        return obj2.equals(this);
      }
      else if (XObject.CLASS_BOOLEAN == obj2.getType())
      {
        return bool() == obj2.bool();
      }
      else if (XObject.CLASS_NUMBER == obj2.getType())
      {
        return num() == obj2.num();
      }
      else if (XObject.CLASS_NODESET == obj2.getType())
      {
        return xstr().equals(obj2.xstr());
      }
      else if (XObject.CLASS_STRING == obj2.getType())
      {
        return xstr().equals(obj2.xstr());
      }
      else if (XObject.CLASS_RTREEFRAG == obj2.getType())
      {

        // Probably not so good.  Think about this.
        return xstr().equals(obj2.xstr());
      }
      else
      {
        return super.equals(obj2);
      }
    }
    catch(javax.xml.transform.TransformerException te)
    {
      throw new com.sun.org.apache.xml.internal.utils.WrappedRuntimeException(te);
    }
  }

}
