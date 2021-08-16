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

package com.sun.org.apache.xpath.internal;

import javax.xml.transform.TransformerException;

import com.sun.org.apache.xalan.internal.res.XSLMessages;
import com.sun.org.apache.xpath.internal.objects.XObject;
import com.sun.org.apache.xpath.internal.res.XPATHErrorResources;

/**
 * Defines a class to keep track of a stack for
 * template arguments and variables.
 *
 * <p>This has been changed from the previous incarnations of this
 * class to be fairly low level.</p>
 * @xsl.usage internal
 * @LastModified: Nov 2017
 */
public class VariableStack implements Cloneable
{
  /**
   * limitation for 1K
   */
  public static final int CLEARLIMITATION= 1024;

  /**
   * Constructor for a variable stack.
   */
  public VariableStack()
  {
    reset();
  }

  /**
   * Returns a clone of this variable stack.
   *
   * @return  a clone of this variable stack.
   *
   * @throws CloneNotSupportedException
   */
  public synchronized Object clone() throws CloneNotSupportedException
  {

    VariableStack vs = (VariableStack) super.clone();

    // I *think* I can get away with a shallow clone here?
    vs._stackFrames = _stackFrames.clone();
    vs._links = _links.clone();

    return vs;
  }

  /**
   * The stack frame where all variables and params will be kept.
   * @serial
   */
  XObject[] _stackFrames = new XObject[XPathContext.RECURSIONLIMIT * 2];

  /**
   * The top of the stack frame (<code>_stackFrames</code>).
   * @serial
   */
  int _frameTop;

  /**
   * The bottom index of the current frame (relative to <code>_stackFrames</code>).
   * @serial
   */
  private int _currentFrameBottom;

  /**
   * The stack of frame positions.  I call 'em links because of distant
   * <a href="http://math.millikin.edu/mprogers/Courses/currentCourses/CS481-ComputerArchitecture/cs481.Motorola68000.html">
   * Motorola 68000 assembler</a> memories.  :-)
   * @serial
   */
  int[] _links = new int[XPathContext.RECURSIONLIMIT];

  /**
   * The top of the links stack.
   */
  int _linksTop;

  /**
   * Get the element at the given index, regardless of stackframe.
   *
   * @param i index from zero.
   *
   * @return The item at the given index.
   */
  public XObject elementAt(final int i)
  {
    return _stackFrames[i];
  }

  /**
   * Get size of the stack.
   *
   * @return the total size of the execution stack.
   */
  public int size()
  {
    return _frameTop;
  }

  /**
   * Reset the stack to a start position.
   *
   * @return the total size of the execution stack.
   */
  public void reset()
  {

    _frameTop = 0;
    _linksTop = 0;

    // Adding one here to the stack of frame positions will allow us always
    // to look one under without having to check if we're at zero.
    // (As long as the caller doesn't screw up link/unlink.)
    _links[_linksTop++] = 0;
    _stackFrames = new XObject[_stackFrames.length];
  }

  /**
   * Set the current stack frame.
   *
   * @param sf The new stack frame position.
   */
  public void setStackFrame(int sf)
  {
    _currentFrameBottom = sf;
  }

  /**
   * Get the position from where the search should start,
   * which is either the searchStart property, or the top
   * of the stack if that value is -1.
   *
   * @return The current stack frame position.
   */
  public int getStackFrame()
  {
    return _currentFrameBottom;
  }

  /**
   * Allocates memory (called a stackframe) on the stack; used to store
   * local variables and parameter arguments.
   *
   * <p>I use the link/unlink concept because of distant
   * <a href="http://math.millikin.edu/mprogers/Courses/currentCourses/CS481-ComputerArchitecture/cs481.Motorola68000.html">
   * Motorola 68000 assembler</a> memories.</p>
   *
   * @param size The size of the stack frame allocation.  This ammount should
   * normally be the maximum number of variables that you can have allocated
   * at one time in the new stack frame.
   *
   * @return The bottom of the stack frame, from where local variable addressing
   * should start from.
   */
  public int link(final int size)
  {

    _currentFrameBottom = _frameTop;
    _frameTop += size;

    if (_frameTop >= _stackFrames.length)
    {
      XObject newsf[] = new XObject[_stackFrames.length + XPathContext.RECURSIONLIMIT + size];

      System.arraycopy(_stackFrames, 0, newsf, 0, _stackFrames.length);

      _stackFrames = newsf;
    }

    if (_linksTop + 1 >= _links.length)
    {
      int newlinks[] = new int[_links.length + (CLEARLIMITATION * 2)];

      System.arraycopy(_links, 0, newlinks, 0, _links.length);

      _links = newlinks;
    }

    _links[_linksTop++] = _currentFrameBottom;

    return _currentFrameBottom;
  }

  /**
   * Free up the stack frame that was last allocated with
   * {@link #link(int size)}.
   */
  public  void unlink()
  {
    _frameTop = _links[--_linksTop];
    _currentFrameBottom = _links[_linksTop - 1];
  }

  /**
   * Free up the stack frame that was last allocated with
   * {@link #link(int size)}.
   * @param currentFrame The current frame to set to
   * after the unlink.
   */
  public  void unlink(int currentFrame)
  {
    _frameTop = _links[--_linksTop];
    _currentFrameBottom = currentFrame;
  }

  /**
   * Set a local variable or parameter in the current stack frame.
   *
   *
   * @param index Local variable index relative to the current stack
   * frame bottom.
   *
   * @param val The value of the variable that is being set.
   */
  public void setLocalVariable(int index, XObject val)
  {
    _stackFrames[index + _currentFrameBottom] = val;
  }

  /**
   * Set a local variable or parameter in the specified stack frame.
   *
   *
   * @param index Local variable index relative to the current stack
   * frame bottom.
   * NEEDSDOC @param stackFrame
   *
   * @param val The value of the variable that is being set.
   */
  public void setLocalVariable(int index, XObject val, int stackFrame)
  {
    _stackFrames[index + stackFrame] = val;
  }

  /**
   * Get a local variable or parameter in the current stack frame.
   *
   *
   * @param xctxt The XPath context, which must be passed in order to
   * lazy evaluate variables.
   *
   * @param index Local variable index relative to the current stack
   * frame bottom.
   *
   * @return The value of the variable.
   *
   * @throws TransformerException
   */
  public XObject getLocalVariable(XPathContext xctxt, int index)
          throws TransformerException
  {

    index += _currentFrameBottom;

    XObject val = _stackFrames[index];

    if(null == val)
      throw new TransformerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_VARIABLE_ACCESSED_BEFORE_BIND, null),
                     xctxt.getSAXLocator());
      // "Variable accessed before it is bound!", xctxt.getSAXLocator());

    // Lazy execution of variables.
    if (val.getType() == XObject.CLASS_UNRESOLVEDVARIABLE)
      return (_stackFrames[index] = val.execute(xctxt));

    return val;
  }

  /**
   * Get a local variable or parameter in the current stack frame.
   *
   *
   * @param index Local variable index relative to the given
   * frame bottom.
   * NEEDSDOC @param frame
   *
   * @return The value of the variable.
   *
   * @throws TransformerException
   */
  public XObject getLocalVariable(int index, int frame)
          throws TransformerException
  {

    index += frame;

    XObject val = _stackFrames[index];

    return val;
  }

  /**
   * Get a local variable or parameter in the current stack frame.
   *
   *
   * @param xctxt The XPath context, which must be passed in order to
   * lazy evaluate variables.
   *
   * @param index Local variable index relative to the current stack
   * frame bottom.
   *
   * @return The value of the variable.
   *
   * @throws TransformerException
   */
  public XObject getLocalVariable(XPathContext xctxt, int index, boolean destructiveOK)
          throws TransformerException
  {

    index += _currentFrameBottom;

    XObject val = _stackFrames[index];

    if(null == val)
      throw new TransformerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_VARIABLE_ACCESSED_BEFORE_BIND, null),
                     xctxt.getSAXLocator());
      // "Variable accessed before it is bound!", xctxt.getSAXLocator());

    // Lazy execution of variables.
    if (val.getType() == XObject.CLASS_UNRESOLVEDVARIABLE)
      return (_stackFrames[index] = val.execute(xctxt));

    return destructiveOK ? val : val.getFresh();
  }

  /**
   * Tell if a local variable has been set or not.
   *
   * @param index Local variable index relative to the current stack
   * frame bottom.
   *
   * @return true if the value at the index is not null.
   *
   * @throws TransformerException
   */
  public boolean isLocalSet(int index) throws TransformerException
  {
    return (_stackFrames[index + _currentFrameBottom] != null);
  }

  /** NEEDSDOC Field m_nulls          */
  private static XObject[] m_nulls = new XObject[CLEARLIMITATION];

  /**
   * Use this to clear the variables in a section of the stack.  This is
   * used to clear the parameter section of the stack, so that default param
   * values can tell if they've already been set.  It is important to note that
   * this function has a 1K limitation.
   *
   * @param start The start position, relative to the current local stack frame.
   * @param len The number of slots to be cleared.
   */
  public void clearLocalSlots(int start, int len)
  {

    start += _currentFrameBottom;

    System.arraycopy(m_nulls, 0, _stackFrames, start, len);
  }

  /**
   * Set a global variable or parameter in the global stack frame.
   *
   *
   * @param index Local variable index relative to the global stack frame
   * bottom.
   *
   * @param val The value of the variable that is being set.
   */
  public void setGlobalVariable(final int index, final XObject val)
  {
    _stackFrames[index] = val;
  }

  /**
   * Get a global variable or parameter from the global stack frame.
   *
   *
   * @param xctxt The XPath context, which must be passed in order to
   * lazy evaluate variables.
   *
   * @param index Global variable index relative to the global stack
   * frame bottom.
   *
   * @return The value of the variable.
   *
   * @throws TransformerException
   */
  public XObject getGlobalVariable(XPathContext xctxt, final int index)
          throws TransformerException
  {

    XObject val = _stackFrames[index];

    // Lazy execution of variables.
    if (val.getType() == XObject.CLASS_UNRESOLVEDVARIABLE)
      return (_stackFrames[index] = val.execute(xctxt));

    return val;
  }

  /**
   * Get a global variable or parameter from the global stack frame.
   *
   *
   * @param xctxt The XPath context, which must be passed in order to
   * lazy evaluate variables.
   *
   * @param index Global variable index relative to the global stack
   * frame bottom.
   *
   * @return The value of the variable.
   *
   * @throws TransformerException
   */
  public XObject getGlobalVariable(XPathContext xctxt, final int index, boolean destructiveOK)
          throws TransformerException
  {

    XObject val = _stackFrames[index];

    // Lazy execution of variables.
    if (val.getType() == XObject.CLASS_UNRESOLVEDVARIABLE)
      return (_stackFrames[index] = val.execute(xctxt));

    return destructiveOK ? val : val.getFresh();
  }

  /**
   * Get a variable based on it's qualified name.
   * This is for external use only.
   *
   * @param xctxt The XPath context, which must be passed in order to
   * lazy evaluate variables.
   *
   * @param qname The qualified name of the variable.
   *
   * @return The evaluated value of the variable.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public XObject getVariableOrParam(
          XPathContext xctxt, com.sun.org.apache.xml.internal.utils.QName qname)
            throws javax.xml.transform.TransformerException
  {

    // J2SE does not support Xalan interpretive
        /*
    com.sun.org.apache.xml.internal.utils.PrefixResolver prefixResolver =
      xctxt.getNamespaceContext();

    // Get the current ElemTemplateElement, which must be pushed in as the
    // prefix resolver, and then walk backwards in document order, searching
    // for an xsl:param element or xsl:variable element that matches our
    // qname.  If we reach the top level, use the StylesheetRoot's composed
    // list of top level variables and parameters.

    if (prefixResolver instanceof com.sun.org.apache.xalan.internal.templates.ElemTemplateElement)
    {

      com.sun.org.apache.xalan.internal.templates.ElemVariable vvar;

      com.sun.org.apache.xalan.internal.templates.ElemTemplateElement prev =
        (com.sun.org.apache.xalan.internal.templates.ElemTemplateElement) prefixResolver;

      if (!(prev instanceof com.sun.org.apache.xalan.internal.templates.Stylesheet))
      {
        while ( !(prev.getParentNode() instanceof com.sun.org.apache.xalan.internal.templates.Stylesheet) )
        {
          com.sun.org.apache.xalan.internal.templates.ElemTemplateElement savedprev = prev;

          while (null != (prev = prev.getPreviousSiblingElem()))
          {
            if (prev instanceof com.sun.org.apache.xalan.internal.templates.ElemVariable)
            {
              vvar = (com.sun.org.apache.xalan.internal.templates.ElemVariable) prev;

              if (vvar.getName().equals(qname))
                return getLocalVariable(xctxt, vvar.getIndex());
            }
          }
          prev = savedprev.getParentElem();
        }
      }

      vvar = prev.getStylesheetRoot().getVariableOrParamComposed(qname);
      if (null != vvar)
        return getGlobalVariable(xctxt, vvar.getIndex());
    }
    */

    throw new javax.xml.transform.TransformerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_VAR_NOT_RESOLVABLE, new Object[]{qname.toString()})); //"Variable not resolvable: " + qname);
  }
}  // end VariableStack
