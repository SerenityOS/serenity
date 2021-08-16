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
 * This interface represents the Particle schema component.
 */
public interface XSParticle extends XSObject {
    /**
     * [min occurs]: determines the minimum number of terms that can occur.
     */
    public int getMinOccurs();

    /**
     *  [max occurs]: determines the maximum number of terms that can occur.
     * To query for the value of unbounded use
     * <code>maxOccursUnbounded</code>. When the value of
     * <code>maxOccursUnbounded</code> is <code>true</code>, the value of
     * <code>maxOccurs</code> is unspecified.
     */
    public int getMaxOccurs();

    /**
     * [max occurs]: whether the maxOccurs value is unbounded.
     */
    public boolean getMaxOccursUnbounded();

    /**
     * [term]: one of a model group, a wildcard, or an element declaration.
     */
    public XSTerm getTerm();

    /**
     * A sequence of [annotations] or an empty <code>XSObjectList</code>.
     */
    public XSObjectList getAnnotations();
}
