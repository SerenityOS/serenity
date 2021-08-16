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

package com.sun.org.apache.xerces.internal.util;

import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import org.w3c.dom.Node;

/**
 * <p>An <code>XMLInputSource</code> analogue to <code>javax.xml.transform.dom.DOMSource</code>.</p>
 *
 */
public final class DOMInputSource extends XMLInputSource {

    private Node fNode;

    public DOMInputSource() {
        this(null);
    }

    public DOMInputSource(Node node) {
        super(null, getSystemIdFromNode(node), null, false);
        fNode = node;
    }

    public DOMInputSource(Node node, String systemId) {
        super(null, systemId, null, false);
        fNode = node;
    }

    public Node getNode() {
        return fNode;
    }

    public void setNode(Node node) {
        fNode = node;
    }

    private static String getSystemIdFromNode(Node node) {
        if (node != null) {
            try {
                return node.getBaseURI();
            }
            // If the DOM implementation is DOM Level 2
            // then a NoSuchMethodError will be thrown.
            // Just ignore it.
            catch (NoSuchMethodError e) {
                return null;
            }
            // There was a failure for some other reason
            // Ignore it as well.
            catch (Exception e) {
                return null;
            }
        }
        return null;
    }

} // DOMInputSource
