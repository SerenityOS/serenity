/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.sun.org.apache.xpath.internal.compiler;

import com.sun.org.apache.xpath.internal.functions.Function;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.TransformerException;

/**
 * The function table for XPath.
 *
 * @LastModified: Oct 2017
 */
public class FunctionTable
{

  /** The 'current()' id. */
  public static final int FUNC_CURRENT = 0;

  /** The 'last()' id. */
  public static final int FUNC_LAST = 1;

  /** The 'position()' id. */
  public static final int FUNC_POSITION = 2;

  /** The 'count()' id. */
  public static final int FUNC_COUNT = 3;

  /** The 'id()' id. */
  public static final int FUNC_ID = 4;

  /** The 'key()' id (XSLT). */
  public static final int FUNC_KEY = 5;

  /** The 'local-name()' id. */
  public static final int FUNC_LOCAL_PART = 7;

  /** The 'namespace-uri()' id. */
  public static final int FUNC_NAMESPACE = 8;

  /** The 'name()' id. */
  public static final int FUNC_QNAME = 9;

  /** The 'generate-id()' id. */
  public static final int FUNC_GENERATE_ID = 10;

  /** The 'not()' id. */
  public static final int FUNC_NOT = 11;

  /** The 'true()' id. */
  public static final int FUNC_TRUE = 12;

  /** The 'false()' id. */
  public static final int FUNC_FALSE = 13;

  /** The 'boolean()' id. */
  public static final int FUNC_BOOLEAN = 14;

  /** The 'number()' id. */
  public static final int FUNC_NUMBER = 15;

  /** The 'floor()' id. */
  public static final int FUNC_FLOOR = 16;

  /** The 'ceiling()' id. */
  public static final int FUNC_CEILING = 17;

  /** The 'round()' id. */
  public static final int FUNC_ROUND = 18;

  /** The 'sum()' id. */
  public static final int FUNC_SUM = 19;

  /** The 'string()' id. */
  public static final int FUNC_STRING = 20;

  /** The 'starts-with()' id. */
  public static final int FUNC_STARTS_WITH = 21;

  /** The 'contains()' id. */
  public static final int FUNC_CONTAINS = 22;

  /** The 'substring-before()' id. */
  public static final int FUNC_SUBSTRING_BEFORE = 23;

  /** The 'substring-after()' id. */
  public static final int FUNC_SUBSTRING_AFTER = 24;

  /** The 'normalize-space()' id. */
  public static final int FUNC_NORMALIZE_SPACE = 25;

  /** The 'translate()' id. */
  public static final int FUNC_TRANSLATE = 26;

  /** The 'concat()' id. */
  public static final int FUNC_CONCAT = 27;

  /** The 'substring()' id. */
  public static final int FUNC_SUBSTRING = 29;

  /** The 'string-length()' id. */
  public static final int FUNC_STRING_LENGTH = 30;

  /** The 'system-property()' id. */
  public static final int FUNC_SYSTEM_PROPERTY = 31;

  /** The 'lang()' id. */
  public static final int FUNC_LANG = 32;

  /** The 'function-available()' id (XSLT). */
  public static final int FUNC_EXT_FUNCTION_AVAILABLE = 33;

  /** The 'element-available()' id (XSLT). */
  public static final int FUNC_EXT_ELEM_AVAILABLE = 34;

  /** The 'unparsed-entity-uri()' id (XSLT). */
  public static final int FUNC_UNPARSED_ENTITY_URI = 36;

  /** The 'here()' id (XML Signature). */
  public static final int FUNC_HERE = 37;

  // Proprietary

  /** The 'document-location()' id (Proprietary). */
  public static final int FUNC_DOCLOCATION = 35;

  /**
   * The function table.
   */
  private static Class<?> m_functions[];

  /** Table of function name to function ID associations. */
  private static final Map<String, Integer> m_functionID = new HashMap<>();

  /**
   * The function table contains customized functions
   */
  private Class<?> m_functions_customer[] = new Class<?>[NUM_ALLOWABLE_ADDINS];

  /**
   * Table of function name to function ID associations for customized functions
   */
  private Map<String, Integer> m_functionID_customer = new HashMap<>();

  /**
   * Number of built in functions.  Be sure to update this as
   * built-in functions are added.
   */
  private static final int NUM_BUILT_IN_FUNCS = 38;

  /**
   * Number of built-in functions that may be added.
   */
  private static final int NUM_ALLOWABLE_ADDINS = 30;

  /**
   * The index to the next free function index.
   */
  private int m_funcNextFreeIndex = NUM_BUILT_IN_FUNCS;

  static
  {
    m_functions = new Class<?>[NUM_BUILT_IN_FUNCS];
    m_functions[FUNC_CURRENT] = com.sun.org.apache.xpath.internal.functions.FuncCurrent.class;
    m_functions[FUNC_LAST] = com.sun.org.apache.xpath.internal.functions.FuncLast.class;
    m_functions[FUNC_POSITION] = com.sun.org.apache.xpath.internal.functions.FuncPosition.class;
    m_functions[FUNC_COUNT] = com.sun.org.apache.xpath.internal.functions.FuncCount.class;
    m_functions[FUNC_ID] = com.sun.org.apache.xpath.internal.functions.FuncId.class;
    // J2SE does not support Xalan interpretive
    // m_functions[FUNC_KEY] =
    //   com.sun.org.apache.xalan.internal.templates.FuncKey.class;
    m_functions[FUNC_LOCAL_PART] =
      com.sun.org.apache.xpath.internal.functions.FuncLocalPart.class;
    m_functions[FUNC_NAMESPACE] =
      com.sun.org.apache.xpath.internal.functions.FuncNamespace.class;
    m_functions[FUNC_QNAME] = com.sun.org.apache.xpath.internal.functions.FuncQname.class;
    m_functions[FUNC_GENERATE_ID] =
      com.sun.org.apache.xpath.internal.functions.FuncGenerateId.class;
    m_functions[FUNC_NOT] = com.sun.org.apache.xpath.internal.functions.FuncNot.class;
    m_functions[FUNC_TRUE] = com.sun.org.apache.xpath.internal.functions.FuncTrue.class;
    m_functions[FUNC_FALSE] = com.sun.org.apache.xpath.internal.functions.FuncFalse.class;
    m_functions[FUNC_BOOLEAN] = com.sun.org.apache.xpath.internal.functions.FuncBoolean.class;
    m_functions[FUNC_LANG] = com.sun.org.apache.xpath.internal.functions.FuncLang.class;
    m_functions[FUNC_NUMBER] = com.sun.org.apache.xpath.internal.functions.FuncNumber.class;
    m_functions[FUNC_FLOOR] = com.sun.org.apache.xpath.internal.functions.FuncFloor.class;
    m_functions[FUNC_CEILING] = com.sun.org.apache.xpath.internal.functions.FuncCeiling.class;
    m_functions[FUNC_ROUND] = com.sun.org.apache.xpath.internal.functions.FuncRound.class;
    m_functions[FUNC_SUM] = com.sun.org.apache.xpath.internal.functions.FuncSum.class;
    m_functions[FUNC_STRING] = com.sun.org.apache.xpath.internal.functions.FuncString.class;
    m_functions[FUNC_STARTS_WITH] =
      com.sun.org.apache.xpath.internal.functions.FuncStartsWith.class;
    m_functions[FUNC_CONTAINS] = com.sun.org.apache.xpath.internal.functions.FuncContains.class;
    m_functions[FUNC_SUBSTRING_BEFORE] =
      com.sun.org.apache.xpath.internal.functions.FuncSubstringBefore.class;
    m_functions[FUNC_SUBSTRING_AFTER] =
      com.sun.org.apache.xpath.internal.functions.FuncSubstringAfter.class;
    m_functions[FUNC_NORMALIZE_SPACE] =
      com.sun.org.apache.xpath.internal.functions.FuncNormalizeSpace.class;
    m_functions[FUNC_TRANSLATE] =
      com.sun.org.apache.xpath.internal.functions.FuncTranslate.class;
    m_functions[FUNC_CONCAT] = com.sun.org.apache.xpath.internal.functions.FuncConcat.class;
    m_functions[FUNC_SYSTEM_PROPERTY] =
      com.sun.org.apache.xpath.internal.functions.FuncSystemProperty.class;
    m_functions[FUNC_EXT_FUNCTION_AVAILABLE] =
      com.sun.org.apache.xpath.internal.functions.FuncExtFunctionAvailable.class;
    m_functions[FUNC_EXT_ELEM_AVAILABLE] =
      com.sun.org.apache.xpath.internal.functions.FuncExtElementAvailable.class;
    m_functions[FUNC_SUBSTRING] =
      com.sun.org.apache.xpath.internal.functions.FuncSubstring.class;
    m_functions[FUNC_STRING_LENGTH] =
      com.sun.org.apache.xpath.internal.functions.FuncStringLength.class;
    m_functions[FUNC_DOCLOCATION] =
      com.sun.org.apache.xpath.internal.functions.FuncDoclocation.class;
    m_functions[FUNC_UNPARSED_ENTITY_URI] =
      com.sun.org.apache.xpath.internal.functions.FuncUnparsedEntityURI.class;
    m_functions[FUNC_HERE] =
      com.sun.org.apache.xpath.internal.functions.FuncHere.class;
  }

  static{
          m_functionID.put(Keywords.FUNC_CURRENT_STRING,
                          FunctionTable.FUNC_CURRENT);
          m_functionID.put(Keywords.FUNC_LAST_STRING,
                          FunctionTable.FUNC_LAST);
          m_functionID.put(Keywords.FUNC_POSITION_STRING,
                          FunctionTable.FUNC_POSITION);
          m_functionID.put(Keywords.FUNC_COUNT_STRING,
                          FunctionTable.FUNC_COUNT);
          m_functionID.put(Keywords.FUNC_ID_STRING,
                          FunctionTable.FUNC_ID);
          m_functionID.put(Keywords.FUNC_KEY_STRING,
                          FunctionTable.FUNC_KEY);
          m_functionID.put(Keywords.FUNC_LOCAL_PART_STRING,
                          FunctionTable.FUNC_LOCAL_PART);
          m_functionID.put(Keywords.FUNC_NAMESPACE_STRING,
                          FunctionTable.FUNC_NAMESPACE);
          m_functionID.put(Keywords.FUNC_NAME_STRING,
                          FunctionTable.FUNC_QNAME);
          m_functionID.put(Keywords.FUNC_GENERATE_ID_STRING,
                          FunctionTable.FUNC_GENERATE_ID);
          m_functionID.put(Keywords.FUNC_NOT_STRING,
                          FunctionTable.FUNC_NOT);
          m_functionID.put(Keywords.FUNC_TRUE_STRING,
                          FunctionTable.FUNC_TRUE);
          m_functionID.put(Keywords.FUNC_FALSE_STRING,
                          FunctionTable.FUNC_FALSE);
          m_functionID.put(Keywords.FUNC_BOOLEAN_STRING,
                          FunctionTable.FUNC_BOOLEAN);
          m_functionID.put(Keywords.FUNC_LANG_STRING,
                          FunctionTable.FUNC_LANG);
          m_functionID.put(Keywords.FUNC_NUMBER_STRING,
                          FunctionTable.FUNC_NUMBER);
          m_functionID.put(Keywords.FUNC_FLOOR_STRING,
                          FunctionTable.FUNC_FLOOR);
          m_functionID.put(Keywords.FUNC_CEILING_STRING,
                          FunctionTable.FUNC_CEILING);
          m_functionID.put(Keywords.FUNC_ROUND_STRING,
                          FunctionTable.FUNC_ROUND);
          m_functionID.put(Keywords.FUNC_SUM_STRING,
                          FunctionTable.FUNC_SUM);
          m_functionID.put(Keywords.FUNC_STRING_STRING,
                          FunctionTable.FUNC_STRING);
          m_functionID.put(Keywords.FUNC_STARTS_WITH_STRING,
                          FunctionTable.FUNC_STARTS_WITH);
          m_functionID.put(Keywords.FUNC_CONTAINS_STRING,
                          FunctionTable.FUNC_CONTAINS);
          m_functionID.put(Keywords.FUNC_SUBSTRING_BEFORE_STRING,
                          FunctionTable.FUNC_SUBSTRING_BEFORE);
          m_functionID.put(Keywords.FUNC_SUBSTRING_AFTER_STRING,
                          FunctionTable.FUNC_SUBSTRING_AFTER);
          m_functionID.put(Keywords.FUNC_NORMALIZE_SPACE_STRING,
                          FunctionTable.FUNC_NORMALIZE_SPACE);
          m_functionID.put(Keywords.FUNC_TRANSLATE_STRING,
                          FunctionTable.FUNC_TRANSLATE);
          m_functionID.put(Keywords.FUNC_CONCAT_STRING,
                          FunctionTable.FUNC_CONCAT);
          m_functionID.put(Keywords.FUNC_SYSTEM_PROPERTY_STRING,
                          FunctionTable.FUNC_SYSTEM_PROPERTY);
          m_functionID.put(Keywords.FUNC_EXT_FUNCTION_AVAILABLE_STRING,
                        FunctionTable.FUNC_EXT_FUNCTION_AVAILABLE);
          m_functionID.put(Keywords.FUNC_EXT_ELEM_AVAILABLE_STRING,
                          FunctionTable.FUNC_EXT_ELEM_AVAILABLE);
          m_functionID.put(Keywords.FUNC_SUBSTRING_STRING,
                          FunctionTable.FUNC_SUBSTRING);
          m_functionID.put(Keywords.FUNC_STRING_LENGTH_STRING,
                          FunctionTable.FUNC_STRING_LENGTH);
          m_functionID.put(Keywords.FUNC_UNPARSED_ENTITY_URI_STRING,
                          FunctionTable.FUNC_UNPARSED_ENTITY_URI);
          m_functionID.put(Keywords.FUNC_DOCLOCATION_STRING,
                          FunctionTable.FUNC_DOCLOCATION);
          m_functionID.put(Keywords.FUNC_HERE_STRING,
                          FunctionTable.FUNC_HERE);
  }

  public FunctionTable(){
  }

  /**
   * Return the name of the a function in the static table. Needed to avoid
   * making the table publicly available.
   */
  String getFunctionName(int funcID) {
      if (funcID < NUM_BUILT_IN_FUNCS) return m_functions[funcID].getName();
      else return m_functions_customer[funcID - NUM_BUILT_IN_FUNCS].getName();
  }

  /**
   * Obtain a new Function object from a function ID.
   *
   * @param which  The function ID, which may correspond to one of the FUNC_XXX
   *    values found in {@link com.sun.org.apache.xpath.internal.compiler.FunctionTable}, but may
   *    be a value installed by an external module.
   *
   * @return a a new Function instance.
   *
   * @throws javax.xml.transform.TransformerException if ClassNotFoundException,
   *    IllegalAccessException, or InstantiationException is thrown.
   */
  Function getFunction(int which)
          throws javax.xml.transform.TransformerException
  {
          try{
              if (which < NUM_BUILT_IN_FUNCS) {
                  return (Function) m_functions[which].getConstructor().newInstance();
              } else {
                  Class<?> c =  m_functions_customer[which-NUM_BUILT_IN_FUNCS];
                  return (Function) c.getConstructor().newInstance();
              }
          }catch (InstantiationException | IllegalAccessException | SecurityException |
              IllegalArgumentException | InvocationTargetException | NoSuchMethodException ex){
              throw new TransformerException(ex.getMessage());
          }
  }

  /**
   * Obtain a function ID from a given function name
   * @param key the function name in a java.lang.String format.
   * @return a function ID, which may correspond to one of the FUNC_XXX values
   * found in {@link com.sun.org.apache.xpath.internal.compiler.FunctionTable}, but may be a
   * value installed by an external module.
   */
  Integer getFunctionID(String key){
          Integer id = m_functionID_customer.get(key);
          if (null == id) id = m_functionID.get(key);
          return id;
  }

  /**
   * Install a built-in function.
   * @param name The unqualified name of the function, must not be null
   * @param func A Implementation of an XPath Function object.
   * @return the position of the function in the internal index.
   */
  public int installFunction(String name, Class<?> func)
  {

    int funcIndex;
    Integer funcIndexObj = getFunctionID(name);

    if (func != null && !Function.class.isAssignableFrom(func)) {
        throw new ClassCastException(func.getName()
                  + " cannot be cast to "
                  + Function.class.getName());
    }

    if (null != funcIndexObj)
    {
      funcIndex = funcIndexObj;

      if (funcIndex < NUM_BUILT_IN_FUNCS){
              funcIndex = m_funcNextFreeIndex++;
              m_functionID_customer.put(name, funcIndex);
      }
      m_functions_customer[funcIndex - NUM_BUILT_IN_FUNCS] = func;
    }
    else
    {
            funcIndex = m_funcNextFreeIndex++;
            m_functions_customer[funcIndex-NUM_BUILT_IN_FUNCS] = func;
            m_functionID_customer.put(name, funcIndex);
    }
    return funcIndex;
  }

  /**
   * Tell if a built-in, non-namespaced function is available.
   *
   * @param methName The local name of the function.
   *
   * @return True if the function can be executed.
   */
  public boolean functionAvailable(String methName)
  {
      Integer tblEntry = m_functionID.get(methName);
      if (null != tblEntry) return true;
      else{
              tblEntry = m_functionID_customer.get(methName);
              return (null != tblEntry);
      }
  }
}
