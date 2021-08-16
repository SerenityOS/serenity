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

package com.sun.org.apache.xerces.internal.dom;

import java.io.Serializable;

/**
 * This class is used, via a pool managed on CoreDocumentImpl, in ParentNode to
 * improve performance of the NodeList accessors, getLength() and item(i).
 *
 * @xerces.internal
 *
 * @author Arnaud  Le Hors, IBM
 *
 */
class NodeListCache implements Serializable {

    /** Serialization version. */
    private static final long serialVersionUID = -7927529254918631002L;

    /** Cached node list length. */
    int fLength = -1;

    /** Last requested node index. */
    int fChildIndex = -1;

    /** Last requested node. */
    ChildNode fChild;

    /** Owner of this cache */
    ParentNode fOwner;

    /** Pointer to the next object on the list,
        only meaningful when actully stored in the free list. */
    NodeListCache next;

    NodeListCache(ParentNode owner) {
        fOwner = owner;
    }
}
