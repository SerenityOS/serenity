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

package com.sun.org.apache.xerces.internal.xs;

/**
 * This interface represents the Identity-constraint Definition schema
 * component.
 */
public interface XSIDCDefinition extends XSObject {
    // Identity Constraints
    /**
     * See the definition of <code>key</code> in the identity-constraint
     * category.
     */
    public static final short IC_KEY                    = 1;
    /**
     * See the definition of <code>keyref</code> in the identity-constraint
     * category.
     */
    public static final short IC_KEYREF                 = 2;
    /**
     * See the definition of <code>unique</code> in the identity-constraint
     * category.
     */
    public static final short IC_UNIQUE                 = 3;

    /**
     * [identity-constraint category]: one of key, keyref or unique.
     */
    public short getCategory();

    /**
     * [selector]: a restricted XPath 1.0 expression.
     */
    public String getSelectorStr();

    /**
     * [fields]: a non-empty list of restricted  XPath 1.0 expressions.
     */
    public StringList getFieldStrs();

    /**
     * [referenced key]: required if [identity-constraint category] is keyref,
     * <code>null</code> otherwise. An identity-constraint definition with [
     * identity-constraint category] equal to key or unique.
     */
    public XSIDCDefinition getRefKey();

    /**
     * A sequence of [annotations] or an empty  <code>XSObjectList</code>.
     */
    public XSObjectList getAnnotations();

}
