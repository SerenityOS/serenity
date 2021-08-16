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

package com.sun.org.apache.xerces.internal.impl.xs.models;

/**
 * A compound content model leaf node which carries occurence information.
 *
 * @xerces.internal
 *
 * @author Michael Glavassevich, IBM
 */
public final class XSCMRepeatingLeaf extends XSCMLeaf {

    private final int fMinOccurs;
    private final int fMaxOccurs;

    public XSCMRepeatingLeaf(int type, Object leaf,
            int minOccurs, int maxOccurs, int id, int position) {
        super(type, leaf, id, position);
        fMinOccurs = minOccurs;
        fMaxOccurs = maxOccurs;
    }

    final int getMinOccurs() {
        return fMinOccurs;
    }

    final int getMaxOccurs() {
        return fMaxOccurs;
    }
}
