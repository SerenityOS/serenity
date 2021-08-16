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

package com.sun.org.apache.xpath.internal.compiler;

/**
 * This is used to represent names of nodes that may not be named, like a
 * comment node.
 */
public class PsuedoNames
{

  /**
   * Psuedo name for a wild card pattern ('*').
   */
  public static final String PSEUDONAME_ANY = "*";

  /**
   * Psuedo name for the root node.
   */
  public static final String PSEUDONAME_ROOT = "/";

  /**
   * Psuedo name for a text node.
   */
  public static final String PSEUDONAME_TEXT = "#text";

  /**
   * Psuedo name for a comment node.
   */
  public static final String PSEUDONAME_COMMENT = "#comment";

  /**
   * Psuedo name for a processing instruction node.
   */
  public static final String PSEUDONAME_PI = "#pi";

  /**
   * Psuedo name for an unknown type value.
   */
  public static final String PSEUDONAME_OTHER = "*";
}
