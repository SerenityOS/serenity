/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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
/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.org.apache.xml.internal.security.signature.reference;

import java.util.Iterator;

import org.w3c.dom.Node;

/**
 * An abstract representation of a {@code ReferenceData} type containing a node-set.
 */
public interface ReferenceNodeSetData extends ReferenceData {

    /**
     * Returns a read-only iterator over the nodes contained in this
     * {@code NodeSetData} in
     * <a href="http://www.w3.org/TR/1999/REC-xpath-19991116#dt-document-order">
     * document order</a>. Attempts to modify the returned iterator
     * via the {@code remove} method throw
     * {@code UnsupportedOperationException}.
     *
     * @return an {@code Iterator} over the nodes in this
     *    {@code NodeSetData} in document order
     */
    Iterator<Node> iterator();

}
