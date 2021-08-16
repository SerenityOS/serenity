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

package com.sun.org.apache.xalan.internal.lib;

import com.sun.org.apache.xpath.internal.NodeSet;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class contains EXSLT math extension functions.
 * It is accessed by specifying a namespace URI as follows:
 * <pre>
 *    xmlns:math="http://exslt.org/math"
 * </pre>
 *
 * The documentation for each function has been copied from the relevant
 * EXSLT Implementer page.
 *
 * @see <a href="http://www.exslt.org/">EXSLT</a>

 * @xsl.usage general
 */
public class ExsltMath extends ExsltBase
{
  // Constants
  private static String PI = "3.1415926535897932384626433832795028841971693993751";
  private static String E  = "2.71828182845904523536028747135266249775724709369996";
  private static String SQRRT2 = "1.41421356237309504880168872420969807856967187537694";
  private static String LN2 = "0.69314718055994530941723212145817656807550013436025";
  private static String LN10 = "2.302585092994046";
  private static String LOG2E = "1.4426950408889633";
  private static String SQRT1_2 = "0.7071067811865476";

  /**
   * The math:max function returns the maximum value of the nodes passed as the argument.
   * The maximum value is defined as follows. The node set passed as an argument is sorted
   * in descending order as it would be by xsl:sort with a data type of number. The maximum
   * is the result of converting the string value of the first node in this sorted list to
   * a number using the number function.
   * <p>
   * If the node set is empty, or if the result of converting the string values of any of the
   * nodes to a number is NaN, then NaN is returned.
   *
   * @param nl The NodeList for the node-set to be evaluated.
   *
   * @return the maximum value found, NaN if any node cannot be converted to a number.
   *
   * @see <a href="http://www.exslt.org/">EXSLT</a>
   */
  public static double max (NodeList nl)
  {
    if (nl == null || nl.getLength() == 0)
      return Double.NaN;

    double m = - Double.MAX_VALUE;
    for (int i = 0; i < nl.getLength(); i++)
    {
      Node n = nl.item(i);
      double d = toNumber(n);
      if (Double.isNaN(d))
        return Double.NaN;
      else if (d > m)
        m = d;
    }

    return m;
  }

  /**
   * The math:min function returns the minimum value of the nodes passed as the argument.
   * The minimum value is defined as follows. The node set passed as an argument is sorted
   * in ascending order as it would be by xsl:sort with a data type of number. The minimum
   * is the result of converting the string value of the first node in this sorted list to
   * a number using the number function.
   * <p>
   * If the node set is empty, or if the result of converting the string values of any of
   * the nodes to a number is NaN, then NaN is returned.
   *
   * @param nl The NodeList for the node-set to be evaluated.
   *
   * @return the minimum value found, NaN if any node cannot be converted to a number.
   *
   * @see <a href="http://www.exslt.org/">EXSLT</a>
   */
  public static double min (NodeList nl)
  {
    if (nl == null || nl.getLength() == 0)
      return Double.NaN;

    double m = Double.MAX_VALUE;
    for (int i = 0; i < nl.getLength(); i++)
    {
      Node n = nl.item(i);
      double d = toNumber(n);
      if (Double.isNaN(d))
        return Double.NaN;
      else if (d < m)
        m = d;
    }

    return m;
  }

  /**
   * The math:highest function returns the nodes in the node set whose value is the maximum
   * value for the node set. The maximum value for the node set is the same as the value as
   * calculated by math:max. A node has this maximum value if the result of converting its
   * string value to a number as if by the number function is equal to the maximum value,
   * where the equality comparison is defined as a numerical comparison using the = operator.
   * <p>
   * If any of the nodes in the node set has a non-numeric value, the math:max function will
   * return NaN. The definition numeric comparisons entails that NaN != NaN. Therefore if any
   * of the nodes in the node set has a non-numeric value, math:highest will return an empty
   * node set.
   *
   * @param nl The NodeList for the node-set to be evaluated.
   *
   * @return node-set with nodes containing the maximum value found, an empty node-set
   * if any node cannot be converted to a number.
   */
  public static NodeList highest (NodeList nl)
  {
    double maxValue = max(nl);

    NodeSet highNodes = new NodeSet();
    highNodes.setShouldCacheNodes(true);

    if (Double.isNaN(maxValue))
      return highNodes;  // empty Nodeset

    for (int i = 0; i < nl.getLength(); i++)
    {
      Node n = nl.item(i);
      double d = toNumber(n);
      if (d == maxValue)
        highNodes.addElement(n);
    }
    return highNodes;
  }

  /**
   * The math:lowest function returns the nodes in the node set whose value is the minimum value
   * for the node set. The minimum value for the node set is the same as the value as calculated
   * by math:min. A node has this minimum value if the result of converting its string value to
   * a number as if by the number function is equal to the minimum value, where the equality
   * comparison is defined as a numerical comparison using the = operator.
   * <p>
   * If any of the nodes in the node set has a non-numeric value, the math:min function will return
   * NaN. The definition numeric comparisons entails that NaN != NaN. Therefore if any of the nodes
   * in the node set has a non-numeric value, math:lowest will return an empty node set.
   *
   * @param nl The NodeList for the node-set to be evaluated.
   *
   * @return node-set with nodes containing the minimum value found, an empty node-set
   * if any node cannot be converted to a number.
   *
   */
  public static NodeList lowest (NodeList nl)
  {
    double minValue = min(nl);

    NodeSet lowNodes = new NodeSet();
    lowNodes.setShouldCacheNodes(true);

    if (Double.isNaN(minValue))
      return lowNodes;  // empty Nodeset

    for (int i = 0; i < nl.getLength(); i++)
    {
      Node n = nl.item(i);
      double d = toNumber(n);
      if (d == minValue)
        lowNodes.addElement(n);
    }
    return lowNodes;
  }

  /**
   * The math:abs function returns the absolute value of a number.
   *
   * @param num A number
   * @return The absolute value of the number
   */
   public static double abs(double num)
   {
     return Math.abs(num);
   }

  /**
   * The math:acos function returns the arccosine value of a number.
   *
   * @param num A number
   * @return The arccosine value of the number
   */
   public static double acos(double num)
   {
     return Math.acos(num);
   }

  /**
   * The math:asin function returns the arcsine value of a number.
   *
   * @param num A number
   * @return The arcsine value of the number
   */
   public static double asin(double num)
   {
     return Math.asin(num);
   }

  /**
   * The math:atan function returns the arctangent value of a number.
   *
   * @param num A number
   * @return The arctangent value of the number
   */
   public static double atan(double num)
   {
     return Math.atan(num);
   }

  /**
   * The math:atan2 function returns the angle ( in radians ) from the X axis to a point (y,x).
   *
   * @param num1 The X axis value
   * @param num2 The Y axis value
   * @return The angle (in radians) from the X axis to a point (y,x)
   */
   public static double atan2(double num1, double num2)
   {
     return Math.atan2(num1, num2);
   }

  /**
   * The math:cos function returns cosine of the passed argument.
   *
   * @param num A number
   * @return The cosine value of the number
   */
   public static double cos(double num)
   {
     return Math.cos(num);
   }

  /**
   * The math:exp function returns e (the base of natural logarithms) raised to a power.
   *
   * @param num A number
   * @return The value of e raised to the given power
   */
   public static double exp(double num)
   {
     return Math.exp(num);
   }

  /**
   * The math:log function returns the natural logarithm of a number.
   *
   * @param num A number
   * @return The natural logarithm of the number
   */
   public static double log(double num)
   {
     return Math.log(num);
   }

  /**
   * The math:power function returns the value of a base expression taken to a specified power.
   *
   * @param num1 The base
   * @param num2 The power
   * @return The value of the base expression taken to the specified power
   */
   public static double power(double num1, double num2)
   {
     return Math.pow(num1, num2);
   }

  /**
   * The math:random function returns a random number from 0 to 1.
   *
   * @return A random double from 0 to 1
   */
   public static double random()
   {
     return Math.random();
   }

  /**
   * The math:sin function returns the sine of the number.
   *
   * @param num A number
   * @return The sine value of the number
   */
   public static double sin(double num)
   {
     return Math.sin(num);
   }

  /**
   * The math:sqrt function returns the square root of a number.
   *
   * @param num A number
   * @return The square root of the number
   */
   public static double sqrt(double num)
   {
     return Math.sqrt(num);
   }

  /**
   * The math:tan function returns the tangent of the number passed as an argument.
   *
   * @param num A number
   * @return The tangent value of the number
   */
   public static double tan(double num)
   {
     return Math.tan(num);
   }

  /**
   * The math:constant function returns the specified constant to a set precision.
   * The possible constants are:
   * <pre>
   *  PI
   *  E
   *  SQRRT2
   *  LN2
   *  LN10
   *  LOG2E
   *  SQRT1_2
   * </pre>
   * @param name The name of the constant
   * @param precision The precision
   * @return The value of the specified constant to the given precision
   */
   public static double constant(String name, double precision)
   {
     String value = null;
     if (name.equals("PI"))
       value = PI;
     else if (name.equals("E"))
       value = E;
     else if (name.equals("SQRRT2"))
       value = SQRRT2;
     else if (name.equals("LN2"))
       value = LN2;
     else if (name.equals("LN10"))
       value = LN10;
     else if (name.equals("LOG2E"))
       value = LOG2E;
     else if (name.equals("SQRT1_2"))
       value = SQRT1_2;

     if (value != null)
     {
       int bits = (int)precision;

       if (bits <= value.length())
         value = value.substring(0, bits);

       return Double.parseDouble(value);
     }
     else
       return Double.NaN;

   }

}
