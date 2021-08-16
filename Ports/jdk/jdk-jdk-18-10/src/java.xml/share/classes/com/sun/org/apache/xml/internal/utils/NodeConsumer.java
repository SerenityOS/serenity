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

package com.sun.org.apache.xml.internal.utils;

import org.w3c.dom.Node;

/**
 * The tree walker will test for this interface, and call
 * setOriginatingNode before calling the SAX event.  For creating
 * DOM backpointers for things that are normally created via
 * SAX events.
 */
public interface NodeConsumer
{

  /**
   * Set the node that is originating the SAX event.
   *
   * @param n Reference to node that originated the current event.
   */
  public void setOriginatingNode(Node n);
}
