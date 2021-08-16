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

package com.sun.org.apache.xpath.internal.operations;

import com.sun.org.apache.xalan.internal.res.XSLMessages;
import com.sun.org.apache.xml.internal.utils.QName;
import com.sun.org.apache.xpath.internal.Expression;
import com.sun.org.apache.xpath.internal.ExpressionOwner;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.XPathVisitor;
import com.sun.org.apache.xpath.internal.axes.PathComponent;
import com.sun.org.apache.xpath.internal.axes.WalkerFactory;
import com.sun.org.apache.xpath.internal.objects.XNodeSet;
import com.sun.org.apache.xpath.internal.objects.XObject;
import com.sun.org.apache.xpath.internal.res.XPATHErrorResources;
import java.util.List;
import javax.xml.transform.TransformerException;


/**
 * The variable reference expression executer.
 *
 * @LastModified: Oct 2017
 */
public class Variable extends Expression implements PathComponent
{
    static final long serialVersionUID = -4334975375609297049L;
  /** Tell if fixupVariables was called.
   *  @serial   */
  private boolean m_fixUpWasCalled = false;

  /** The qualified name of the variable.
   *  @serial   */
  protected QName m_qname;

  /**
   * The index of the variable, which is either an absolute index to a
   * global, or, if higher than the globals area, must be adjusted by adding
   * the offset to the current stack frame.
   */
  protected int m_index;

  /**
   * Set the index for the variable into the stack.  For advanced use only. You
   * must know what you are doing to use this.
   *
   * @param index a global or local index.
   */
  public void setIndex(int index)
  {
        m_index = index;
  }

  /**
   * Set the index for the variable into the stack.  For advanced use only.
   *
   * @return index a global or local index.
   */
  public int getIndex()
  {
        return m_index;
  }

  /**
   * Set whether or not this is a global reference.  For advanced use only.
   *
   * @param isGlobal true if this should be a global variable reference.
   */
  public void setIsGlobal(boolean isGlobal)
  {
        m_isGlobal = isGlobal;
  }

  /**
   * Set the index for the variable into the stack.  For advanced use only.
   *
   * @return true if this should be a global variable reference.
   */
  public boolean getGlobal()
  {
        return m_isGlobal;
  }





  protected boolean m_isGlobal = false;

  /**
   * This function is used to fixup variables from QNames to stack frame
   * indexes at stylesheet build time.
   * @param vars List of QNames that correspond to variables.  This list
   * should be searched backwards for the first qualified name that
   * corresponds to the variable reference qname.  The position of the
   * QName in the vector from the start of the vector will be its position
   * in the stack frame (but variables above the globalsTop value will need
   * to be offset to the current stack frame).
   */
  public void fixupVariables(List<QName> vars, int globalsSize)
  {
    m_fixUpWasCalled = true;
    int sz = vars.size();

    for (int i = vars.size()-1; i >= 0; i--)
    {
      QName qn = vars.get(i);
      // System.out.println("qn: "+qn);
      if(qn.equals(m_qname))
      {

        if(i < globalsSize)
        {
          m_isGlobal = true;
          m_index = i;
        }
        else
        {
          m_index = i-globalsSize;
        }

        return;
      }
    }

    java.lang.String msg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_COULD_NOT_FIND_VAR,
                                             new Object[]{m_qname.toString()});

    TransformerException te = new TransformerException(msg, this);

    throw new com.sun.org.apache.xml.internal.utils.WrappedRuntimeException(te);

  }


  /**
   * Set the qualified name of the variable.
   *
   * @param qname Must be a non-null reference to a qualified name.
   */
  public void setQName(QName qname)
  {
    m_qname = qname;
  }

  /**
   * Get the qualified name of the variable.
   *
   * @return A non-null reference to a qualified name.
   */
  public QName getQName()
  {
    return m_qname;
  }

  /**
   * Execute an expression in the XPath runtime context, and return the
   * result of the expression.
   *
   *
   * @param xctxt The XPath runtime context.
   *
   * @return The result of the expression in the form of a <code>XObject</code>.
   *
   * @throws javax.xml.transform.TransformerException if a runtime exception
   *         occurs.
   */
  public XObject execute(XPathContext xctxt)
    throws javax.xml.transform.TransformerException
  {
        return execute(xctxt, false);
  }


  /**
   * Dereference the variable, and return the reference value.  Note that lazy
   * evaluation will occur.  If a variable within scope is not found, a warning
   * will be sent to the error listener, and an empty nodeset will be returned.
   *
   *
   * @param xctxt The runtime execution context.
   *
   * @return The evaluated variable, or an empty nodeset if not found.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public XObject execute(XPathContext xctxt, boolean destructiveOK) throws javax.xml.transform.TransformerException
  {
    com.sun.org.apache.xml.internal.utils.PrefixResolver xprefixResolver = xctxt.getNamespaceContext();

    XObject result;
    // Is the variable fetched always the same?
    // XObject result = xctxt.getVariable(m_qname);
    if(m_fixUpWasCalled)
    {
      if(m_isGlobal)
        result = xctxt.getVarStack().getGlobalVariable(xctxt, m_index, destructiveOK);
      else
        result = xctxt.getVarStack().getLocalVariable(xctxt, m_index, destructiveOK);
    }
    else {
        result = xctxt.getVarStack().getVariableOrParam(xctxt,m_qname);
    }

      if (null == result)
      {
        // This should now never happen...
        warn(xctxt, XPATHErrorResources.WG_ILLEGAL_VARIABLE_REFERENCE,
             new Object[]{ m_qname.getLocalPart() });  //"VariableReference given for variable out "+
  //      (new RuntimeException()).printStackTrace();
  //      error(xctxt, XPATHErrorResources.ER_COULDNOT_GET_VAR_NAMED,
  //            new Object[]{ m_qname.getLocalPart() });  //"Could not get variable named "+varName);

        result = new XNodeSet(xctxt.getDTMManager());
      }

      return result;
//    }
//    else
//    {
//      // Hack city... big time.  This is needed to evaluate xpaths from extensions,
//      // pending some bright light going off in my head.  Some sort of callback?
//      synchronized(this)
//      {
//              com.sun.org.apache.xalan.internal.templates.ElemVariable vvar= getElemVariable();
//              if(null != vvar)
//              {
//          m_index = vvar.getIndex();
//          m_isGlobal = vvar.getIsTopLevel();
//          m_fixUpWasCalled = true;
//          return execute(xctxt);
//              }
//      }
//      throw new javax.xml.transform.TransformerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_VAR_NOT_RESOLVABLE, new Object[]{m_qname.toString()})); //"Variable not resolvable: "+m_qname);
//    }
  }

  /**
   * Get the XSLT ElemVariable that this sub-expression references.  In order for
   * this to work, the SourceLocator must be the owning ElemTemplateElement.
   * @return The dereference to the ElemVariable, or null if not found.
   */
  // J2SE does not support Xalan interpretive
  /*
  public com.sun.org.apache.xalan.internal.templates.ElemVariable getElemVariable()
  {

    // Get the current ElemTemplateElement, and then walk backwards in
    // document order, searching
    // for an xsl:param element or xsl:variable element that matches our
    // qname.  If we reach the top level, use the StylesheetRoot's composed
    // list of top level variables and parameters.

    com.sun.org.apache.xalan.internal.templates.ElemVariable vvar = null;
    com.sun.org.apache.xpath.internal.ExpressionNode owner = getExpressionOwner();

    if (null != owner && owner instanceof com.sun.org.apache.xalan.internal.templates.ElemTemplateElement)
    {

      com.sun.org.apache.xalan.internal.templates.ElemTemplateElement prev =
        (com.sun.org.apache.xalan.internal.templates.ElemTemplateElement) owner;

      if (!(prev instanceof com.sun.org.apache.xalan.internal.templates.Stylesheet))
      {
        while ( prev != null && !(prev.getParentNode() instanceof com.sun.org.apache.xalan.internal.templates.Stylesheet) )
        {
          com.sun.org.apache.xalan.internal.templates.ElemTemplateElement savedprev = prev;

          while (null != (prev = prev.getPreviousSiblingElem()))
          {
            if(prev instanceof com.sun.org.apache.xalan.internal.templates.ElemVariable)
            {
              vvar = (com.sun.org.apache.xalan.internal.templates.ElemVariable) prev;

              if (vvar.getName().equals(m_qname))
              {
                return vvar;
              }
              vvar = null;
            }
          }
          prev = savedprev.getParentElem();
        }
      }
      if (prev != null)
        vvar = prev.getStylesheetRoot().getVariableOrParamComposed(m_qname);
    }
    return vvar;

  }
  */
  /**
   * Tell if this expression returns a stable number that will not change during
   * iterations within the expression.  This is used to determine if a proximity
   * position predicate can indicate that no more searching has to occur.
   *
   *
   * @return true if the expression represents a stable number.
   */
  public boolean isStableNumber()
  {
    return true;
  }

  /**
   * Get the analysis bits for this walker, as defined in the WalkerFactory.
   * @return One of WalkerFactory#BIT_DESCENDANT, etc.
   */
  public int getAnalysisBits()
  {

    // J2SE does not support Xalan interpretive
    /*
        com.sun.org.apache.xalan.internal.templates.ElemVariable vvar = getElemVariable();
        if(null != vvar)
        {
                XPath xpath = vvar.getSelect();
                if(null != xpath)
                {
                        Expression expr = xpath.getExpression();
                        if(null != expr && expr instanceof PathComponent)
                        {
                                return ((PathComponent)expr).getAnalysisBits();
                        }
                }
        }
    */

    return WalkerFactory.BIT_FILTER;
  }


  /**
   * @see com.sun.org.apache.xpath.internal.XPathVisitable#callVisitors(ExpressionOwner, XPathVisitor)
   */
  public void callVisitors(ExpressionOwner owner, XPathVisitor visitor)
  {
        visitor.visitVariableRef(owner, this);
  }
  /**
   * @see Expression#deepEquals(Expression)
   */
  public boolean deepEquals(Expression expr)
  {
        if(!isSameClass(expr))
                return false;

        if(!m_qname.equals(((Variable)expr).m_qname))
                return false;

    // J2SE does not support Xalan interpretive
    /*
        // We have to make sure that the qname really references
        // the same variable element.
    if(getElemVariable() != ((Variable)expr).getElemVariable())
        return false;
        */

        return true;
  }

  static final java.lang.String PSUEDOVARNAMESPACE = "http://xml.apache.org/xalan/psuedovar";

  /**
   * Tell if this is a psuedo variable reference, declared by Xalan instead
   * of by the user.
   */
  public boolean isPsuedoVarRef()
  {
        java.lang.String ns = m_qname.getNamespaceURI();
        if((null != ns) && ns.equals(PSUEDOVARNAMESPACE))
        {
                if(m_qname.getLocalName().startsWith("#"))
                        return true;
        }
        return false;
  }


}
