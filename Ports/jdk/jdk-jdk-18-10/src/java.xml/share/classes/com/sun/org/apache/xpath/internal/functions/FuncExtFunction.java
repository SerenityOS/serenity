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

package com.sun.org.apache.xpath.internal.functions;

import com.sun.org.apache.xalan.internal.res.XSLMessages;
import com.sun.org.apache.xml.internal.utils.QName;
import com.sun.org.apache.xpath.internal.Expression;
import com.sun.org.apache.xpath.internal.ExpressionNode;
import com.sun.org.apache.xpath.internal.ExpressionOwner;
import com.sun.org.apache.xpath.internal.ExtensionsProvider;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.XPathVisitor;
import com.sun.org.apache.xpath.internal.objects.XNull;
import com.sun.org.apache.xpath.internal.objects.XObject;
import com.sun.org.apache.xpath.internal.res.XPATHErrorResources;
import com.sun.org.apache.xpath.internal.res.XPATHMessages;
import java.util.ArrayList;
import java.util.List;

/**
 * An object of this class represents an extension call expression.  When
 * the expression executes, it calls ExtensionsTable#extFunction, and then
 * converts the result to the appropriate XObject.
 * @xsl.usage advanced
 * @LastModified: Oct 2017
 */
public class FuncExtFunction extends Function
{
    static final long serialVersionUID = 5196115554693708718L;

  /**
   * The namespace for the extension function, which should not normally
   *  be null or empty.
   *  @serial
   */
  String m_namespace;

  /**
   * The local name of the extension.
   *  @serial
   */
  String m_extensionName;

  /**
   * Unique method key, which is passed to ExtensionsTable#extFunction in
   *  order to allow caching of the method.
   *  @serial
   */
  Object m_methodKey;

  /**
   * Array of static expressions which represent the parameters to the
   *  function.
   *  @serial
   */
  List<Expression> m_argVec = new ArrayList<>();

  /**
   * This function is used to fixup variables from QNames to stack frame
   * indexes at stylesheet build time.
   * @param vars List of QNames that correspond to variables.  This list
   * should be searched backwards for the first qualified name that
   * corresponds to the variable reference qname.  The position of the
   * QName in the vector from the start of the vector will be its position
   * in the stack frame (but variables above the globalsTop value will need
   * to be offset to the current stack frame).
   * NEEDSDOC @param globalsSize
   */
  public void fixupVariables(List<QName> vars, int globalsSize)
  {

    if (null != m_argVec)
    {
      int nArgs = m_argVec.size();

      for (int i = 0; i < nArgs; i++)
      {
        Expression arg = m_argVec.get(i);

        arg.fixupVariables(vars, globalsSize);
      }
    }
  }

  /**
   * Return the namespace of the extension function.
   *
   * @return The namespace of the extension function.
   */
  public String getNamespace()
  {
    return m_namespace;
  }

  /**
   * Return the name of the extension function.
   *
   * @return The name of the extension function.
   */
  public String getFunctionName()
  {
    return m_extensionName;
  }

  /**
   * Return the method key of the extension function.
   *
   * @return The method key of the extension function.
   */
  public Object getMethodKey()
  {
    return m_methodKey;
  }

  /**
   * Return the nth argument passed to the extension function.
   *
   * @param n The argument number index.
   * @return The Expression object at the given index.
   */
  public Expression getArg(int n) {
    if (n >= 0 && n < m_argVec.size())
      return m_argVec.get(n);
    else
      return null;
  }

  /**
   * Return the number of arguments that were passed
   * into this extension function.
   *
   * @return The number of arguments.
   */
  public int getArgCount() {
    return m_argVec.size();
  }

  /**
   * Create a new FuncExtFunction based on the qualified name of the extension,
   * and a unique method key.
   *
   * @param namespace The namespace for the extension function, which should
   *                  not normally be null or empty.
   * @param extensionName The local name of the extension.
   * @param methodKey Unique method key, which is passed to
   *                  ExtensionsTable#extFunction in order to allow caching
   *                  of the method.
   */
  public FuncExtFunction(java.lang.String namespace,
                         java.lang.String extensionName, Object methodKey)
  {
    //try{throw new Exception("FuncExtFunction() " + namespace + " " + extensionName);} catch (Exception e){e.printStackTrace();}
    m_namespace = namespace;
    m_extensionName = extensionName;
    m_methodKey = methodKey;
  }

  /**
   * Execute the function.  The function must return
   * a valid object.
   * @param xctxt The current execution context.
   * @return A valid XObject.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public XObject execute(XPathContext xctxt)
          throws javax.xml.transform.TransformerException
  {
    if (xctxt.isSecureProcessing())
      throw new javax.xml.transform.TransformerException(
        XPATHMessages.createXPATHMessage(
          XPATHErrorResources.ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED,
          new Object[] {toString()}));

    XObject result;
    List<XObject> argVec = new ArrayList<>();
    int nArgs = m_argVec.size();

    for (int i = 0; i < nArgs; i++)
    {
      Expression arg = m_argVec.get(i);

      XObject xobj = arg.execute(xctxt);
      /*
       * Should cache the arguments for func:function
       */
      xobj.allowDetachToRelease(false);
      argVec.add(xobj);
    }
    //dml
    ExtensionsProvider extProvider = (ExtensionsProvider)xctxt.getOwnerObject();
    Object val = extProvider.extFunction(this, argVec);

    if (null != val)
    {
      result = XObject.create(val, xctxt);
    }
    else
    {
      result = new XNull();
    }

    return result;
  }

  /**
   * Set an argument expression for a function.  This method is called by the
   * XPath compiler.
   *
   * @param arg non-null expression that represents the argument.
   * @param argNum The argument number index.
   *
   * @throws WrongNumberArgsException If the argNum parameter is beyond what
   * is specified for this function.
   */
  public void setArg(Expression arg, int argNum)
          throws WrongNumberArgsException
  {
    m_argVec.add(arg);
    arg.exprSetParent(this);
  }

  /**
   * Check that the number of arguments passed to this function is correct.
   *
   *
   * @param argNum The number of arguments that is being passed to the function.
   *
   * @throws WrongNumberArgsException
   */
  public void checkNumberArgs(int argNum) throws WrongNumberArgsException{}


  class ArgExtOwner implements ExpressionOwner
  {

    Expression m_exp;

        ArgExtOwner(Expression exp)
        {
                m_exp = exp;
        }

    /**
     * @see ExpressionOwner#getExpression()
     */
    public Expression getExpression()
    {
      return m_exp;
    }


    /**
     * @see ExpressionOwner#setExpression(Expression)
     */
    public void setExpression(Expression exp)
    {
        exp.exprSetParent(FuncExtFunction.this);
        m_exp = exp;
    }
  }


  /**
   * Call the visitors for the function arguments.
   */
  public void callArgVisitors(XPathVisitor visitor)
  {
      for (int i = 0; i < m_argVec.size(); i++)
      {
         Expression exp = m_argVec.get(i);
         exp.callVisitors(new ArgExtOwner(exp), visitor);
      }

  }

  /**
   * Set the parent node.
   * For an extension function, we also need to set the parent
   * node for all argument expressions.
   *
   * @param n The parent node
   */
  public void exprSetParent(ExpressionNode n)
  {

    super.exprSetParent(n);

    int nArgs = m_argVec.size();

    for (int i = 0; i < nArgs; i++)
    {
      Expression arg = m_argVec.get(i);

      arg.exprSetParent(n);
    }
  }

  /**
   * Constructs and throws a WrongNumberArgException with the appropriate
   * message for this function object.  This class supports an arbitrary
   * number of arguments, so this method must never be called.
   *
   * @throws WrongNumberArgsException
   */
  protected void reportWrongNumberArgs() throws WrongNumberArgsException {
    String fMsg = XSLMessages.createXPATHMessage(
        XPATHErrorResources.ER_INCORRECT_PROGRAMMER_ASSERTION,
        new Object[]{ "Programmer's assertion:  the method FunctionMultiArgs.reportWrongNumberArgs() should never be called." });

    throw new RuntimeException(fMsg);
  }

  /**
   * Return the name of the extesion function in string format
   */
  public String toString()
  {
    if (m_namespace != null && m_namespace.length() > 0)
      return "{" + m_namespace + "}" + m_extensionName;
    else
      return m_extensionName;
  }
}
