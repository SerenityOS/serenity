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
 * This interface represents the Wildcard schema component.
 */
public interface XSWildcard extends XSTerm {
    // Namespace Constraint
    /**
     * Namespace Constraint: any namespace is allowed.
     */
    public static final short NSCONSTRAINT_ANY          = 1;
    /**
     * Namespace Constraint: namespaces in the list are not allowed.
     */
    public static final short NSCONSTRAINT_NOT          = 2;
    /**
     * Namespace Constraint: namespaces in the list are allowed.
     */
    public static final short NSCONSTRAINT_LIST         = 3;

    // Process contents
    /**
     * There must be a top-level declaration for the item available, or the
     * item must have an xsi:type, and the item must be valid as appropriate.
     */
    public static final short PC_STRICT                 = 1;
    /**
     * No constraints at all: the item must simply be well-formed XML.
     */
    public static final short PC_SKIP                   = 2;
    /**
     * If the item, or any items among its [children] is an element
     * information item, has a uniquely determined declaration available, it
     * must be valid with respect to that definition, that is, validate
     * where you can and do not worry when you cannot.
     */
    public static final short PC_LAX                    = 3;

    /**
     * Namespace constraint: A constraint type: any, not, list.
     */
    public short getConstraintType();

    /**
     * Namespace constraint: For <code>constraintType</code>
     * <code>NSCONSTRAINT_LIST</code>, the list contains allowed namespaces.
     * For <code>constraintType</code> <code>NSCONSTRAINT_NOT</code>, the
     * list contains disallowed namespaces. For <code>constraintType</code>
     * <code>NSCONSTRAINT_ANY</code>, the <code>StringList</code> is empty.
     */
    public StringList getNsConstraintList();

    /**
     * [process contents]: one of skip, lax or strict. Valid constants values
     * are: <code>PC_LAX</code>, <code>PC_SKIP</code> and
     * <code>PC_STRICT</code>.
     */
    public short getProcessContents();

    /**
     * An annotation if it exists, otherwise <code>null</code>. If not null
     * then the first [annotation] from the sequence of annotations.
     */
    public XSAnnotation getAnnotation();

    /**
     * A sequence of [annotations] or an empty <code>XSObjectList</code>.
     */
    public XSObjectList getAnnotations();
}
