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

package com.sun.org.apache.xalan.internal.lib;

import com.sun.org.apache.xalan.internal.extensions.ExpressionContext;
import com.sun.org.apache.xml.internal.dtm.DTMIterator;
import com.sun.org.apache.xml.internal.dtm.ref.DTMNodeIterator;
import com.sun.org.apache.xpath.internal.NodeSet;

/**
 * This class contains EXSLT common extension functions.
 * It is accessed by specifying a namespace URI as follows:
 * <pre>
 *    xmlns:exslt="http://exslt.org/common"
 * </pre>
 *
 * The documentation for each function has been copied from the relevant
 * EXSLT Implementer page.
 *
 * @see <a href="http://www.exslt.org/">EXSLT</a>
 * @xsl.usage general
 */
public class ExsltCommon
{
  /**
   * The exsl:object-type function returns a string giving the type of the object passed
   * as the argument. The possible object types are: 'string', 'number', 'boolean',
   * 'node-set', 'RTF', or 'external'.
   *
   * Most XSLT object types can be coerced to each other without error. However, there are
   * certain coercions that raise errors, most importantly treating anything other than a
   * node set as a node set. Authors of utilities such as named templates or user-defined
   * extension functions may wish to give some flexibility in the parameter and argument values
   * that are accepted by the utility; the exsl:object-type function enables them to do so.
   *
   * The Xalan extensions MethodResolver converts 'object-type' to 'objectType'.
   *
   * @param obj The object to be typed.
   * @return objectType 'string', 'number', 'boolean', 'node-set', 'RTF', or 'external'.
   *
   * @see <a href="http://www.exslt.org/">EXSLT</a>
   */
  public static String objectType (Object obj)
  {
    if (obj instanceof String)
      return "string";
    else if (obj instanceof Boolean)
      return "boolean";
    else if (obj instanceof Number)
      return "number";
    else if (obj instanceof DTMNodeIterator)
    {
      DTMIterator dtmI = ((DTMNodeIterator)obj).getDTMIterator();
      if (dtmI instanceof com.sun.org.apache.xpath.internal.axes.RTFIterator)
        return "RTF";
      else
        return "node-set";
    }
    else
      return "unknown";
  }

  /**
   * The exsl:node-set function converts a result tree fragment (which is what you get
   * when you use the content of xsl:variable rather than its select attribute to give
   * a variable value) into a node set. This enables you to process the XML that you create
   * within a variable, and therefore do multi-step processing.
   *
   * You can also use this function to turn a string into a text node, which is helpful
   * if you want to pass a string to a function that only accepts a node set.
   *
   * The Xalan extensions MethodResolver converts 'node-set' to 'nodeSet'.
   *
   * @param myProcessor is passed in by the Xalan extension processor
   * @param rtf The result tree fragment to be converted to a node-set.
   *
   * @return node-set with the contents of the result tree fragment.
   *
   * Note: Already implemented in the xalan namespace as nodeset.
   *
   * @see <a href="http://www.exslt.org/">EXSLT</a>
   */
  public static NodeSet nodeSet(ExpressionContext myProcessor, Object rtf)
  {
    return Extensions.nodeset(myProcessor, rtf);
  }

}
