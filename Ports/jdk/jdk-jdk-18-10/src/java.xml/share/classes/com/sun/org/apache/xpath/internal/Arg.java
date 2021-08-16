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

package com.sun.org.apache.xpath.internal;

import com.sun.org.apache.xml.internal.utils.QName;
import com.sun.org.apache.xpath.internal.objects.XObject;
import java.util.Objects;

/**
 * This class holds an instance of an argument on
 * the stack. The value of the argument can be either an
 * XObject or a String containing an expression.
 * @xsl.usage internal
 */
public class Arg
{

  /** Field m_qname: The name of this argument, expressed as a QName
   * (Qualified Name) object.
   * @see getQName
   * @see setQName
   *  */
  private QName m_qname;

  /**
   * Get the qualified name for this argument.
   *
   * @return QName object containing the qualified name
   */
  public final QName getQName()
  {
    return m_qname;
  }

  /**
   * Set the qualified name for this argument.
   *
   * @param name QName object representing the new Qualified Name.
   */
  public final void setQName(QName name)
  {
    m_qname = name;
  }

  /** Field m_val: Stored XObject value of this argument
   * @see #getVal()
   * @see #setVal()
   */
  private XObject m_val;

  /**
   * Get the value for this argument.
   *
   * @return the argument's stored XObject value.
   * @see #setVal(XObject)
   */
  public final XObject getVal()
  {
    return m_val;
  }

  /**
   * Set the value of this argument.
   *
   * @param val an XObject representing the arguments's value.
   * @see #getVal()
   */
  public final void setVal(XObject val)
  {
    m_val = val;
  }

  /**
   * Have the object release it's resources.
   * Call only when the variable or argument is going out of scope.
   */
  public void detach()
  {
    if(null != m_val)
    {
      m_val.allowDetachToRelease(true);
      m_val.detach();
    }
  }


  /** Field m_expression: Stored expression value of this argument.
   * @see #setExpression
   * @see #getExpression
   * */
  private String m_expression;

  /**
   * Get the value expression for this argument.
   *
   * @return String containing the expression previously stored into this
   * argument
   * @see #setExpression
   */
  public String getExpression()
  {
    return m_expression;
  }

  /**
   * Set the value expression for this argument.
   *
   * @param expr String containing the expression to be stored as this
   * argument's value.
   * @see #getExpression
   */
  public void setExpression(String expr)
  {
    m_expression = expr;
  }

  /**
   * True if this variable was added with an xsl:with-param or
   * is added via setParameter.
   */
  private boolean m_isFromWithParam;

  /**
   * Tell if this variable is a parameter passed with a with-param or as
   * a top-level parameter.
   */
   public boolean isFromWithParam()
   {
    return m_isFromWithParam;
   }

  /**
   * True if this variable is currently visible.  To be visible,
   * a variable needs to come either from xsl:variable or be
   * a "received" parameter, ie one for which an xsl:param has
   * been encountered.
   * Set at the time the object is constructed and updated as needed.
   */
  private boolean m_isVisible;

  /**
   * Tell if this variable is currently visible.
   */
   public boolean isVisible()
   {
    return m_isVisible;
   }

  /**
   * Update visibility status of this variable.
   */
   public void setIsVisible(boolean b)
   {
    m_isVisible = b;
   }

  /**
   * Construct a dummy parameter argument, with no QName and no
   * value (either expression string or value XObject). isVisible
   * defaults to true.
   */
  public Arg()
  {

    m_qname = new QName("");
       // so that string compares can be done.
    m_val = null;
    m_expression = null;
    m_isVisible = true;
    m_isFromWithParam = false;
  }

  /**
   * Construct a parameter argument that contains an expression.
   *
   * @param qname Name of the argument, expressed as a QName object.
   * @param expression String to be stored as this argument's value expression.
   * @param isFromWithParam True if this is a parameter variable.
   */
  public Arg(QName qname, String expression, boolean isFromWithParam)
  {

    m_qname = qname;
    m_val = null;
    m_expression = expression;
    m_isFromWithParam = isFromWithParam;
    m_isVisible = !isFromWithParam;
  }

  /**
   * Construct a parameter argument which has an XObject value.
   * isVisible defaults to true.
   *
   * @param qname Name of the argument, expressed as a QName object.
   * @param val Value of the argument, expressed as an XObject
   */
  public Arg(QName qname, XObject val)
  {

    m_qname = qname;
    m_val = val;
    m_isVisible = true;
    m_isFromWithParam = false;
    m_expression = null;
  }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.m_qname);
    }

  /**
   * Equality function specialized for the variable name.  If the argument
   * is not a qname, it will deligate to the super class.
   *
   * @param   obj   the reference object with which to compare.
   * @return  <code>true</code> if this object is the same as the obj
   *          argument; <code>false</code> otherwise.
   */
  @Override
  public boolean equals(Object obj)
  {
    if(obj instanceof QName)
    {
      return m_qname.equals(obj);
    }
    else
      return super.equals(obj);
  }

  /**
   * Construct a parameter argument.
   *
   * @param qname Name of the argument, expressed as a QName object.
   * @param val Value of the argument, expressed as an XObject
   * @param isFromWithParam True if this is a parameter variable.
   */
  public Arg(QName qname, XObject val, boolean isFromWithParam)
  {

    m_qname = qname;
    m_val = val;
    m_isFromWithParam = isFromWithParam;
    m_isVisible = !isFromWithParam;
    m_expression = null;
  }
}
