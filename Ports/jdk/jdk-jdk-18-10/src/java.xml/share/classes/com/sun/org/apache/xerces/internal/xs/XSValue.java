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
 * Represents an actual value of a simple type.
 */
public interface XSValue {

    /**
     * The schema normalized value.
     * @return The normalized value.
     */
    public String getNormalizedValue();

    /**
     * The actual value. <code>null</code> if the value is in error.
     * @return The actual value.
     */
    public Object getActualValue();

    /**
     * The declared simple type definition used to validate this value.
     * It can be a union type.
     * @return The declared simple type definition
     */
    public XSSimpleTypeDefinition getTypeDefinition();

    /**
     * If the declared simple type definition is a union, return the member
     * type actually used to validate the value. Otherwise null.
     * @return The member type
     */
    public XSSimpleTypeDefinition getMemberTypeDefinition();

    /**
     * If <code>getTypeDefinition()</code> returns a list type whose item type
     * is a union type, then this method returns a list with the same length
     * as the value list, for simple types that actually validated
     * the corresponding item in the value.
     * @return A list of type definitions
     */
    public XSObjectList getMemberTypeDefinitions();

    /**
     * The actual value built-in datatype, e.g.
     * <code>STRING_DT, SHORT_DT</code>. If the type definition of this
     * value is a list type definition, this method returns
     * <code>LIST_DT</code>. If the type definition of this value is a list
     * type definition whose item type is a union type definition, this
     * method returns <code>LISTOFUNION_DT</code>. To query the actual value
     * of the list or list of union type definitions use
     * <code>itemValueTypes()</code>.
     * @return The actual value type
     */
    public short getActualValueType();

    /**
     * In the case the actual value represents a list, i.e. the
     * <code>actualNormalizedValueType</code> is <code>LIST_DT</code>, the
     * returned array consists of one type kind which represents the itemType
     * . For example:
     * <pre> &lt;simpleType name="listtype"&gt; &lt;list
     * itemType="positiveInteger"/&gt; &lt;/simpleType&gt; &lt;element
     * name="list" type="listtype"/&gt; ... &lt;list&gt;1 2 3&lt;/list&gt; </pre>
     *
     * The <code>schemaNormalizedValue</code> value is "1 2 3", the
     * <code>actualNormalizedValueType</code> value is <code>LIST_DT</code>,
     * and the <code>itemValueTypes</code> is an array of size 1 with the
     * value <code>POSITIVEINTEGER_DT</code>.
     * <br> If the actual value represents a list type definition whose item
     * type is a union type definition, i.e. <code>LISTOFUNION_DT</code>,
     * for each actual value in the list the array contains the
     * corresponding memberType kind. For example:
     * <pre> &lt;simpleType
     * name='union_type' memberTypes="integer string"/&gt; &lt;simpleType
     * name='listOfUnion'&gt; &lt;list itemType='union_type'/&gt;
     * &lt;/simpleType&gt; &lt;element name="list" type="listOfUnion"/&gt;
     * ... &lt;list&gt;1 2 foo&lt;/list&gt; </pre>
     *  The
     * <code>schemaNormalizedValue</code> value is "1 2 foo", the
     * <code>actualNormalizedValueType</code> is <code>LISTOFUNION_DT</code>
     * , and the <code>itemValueTypes</code> is an array of size 3 with the
     * following values: <code>INTEGER_DT, INTEGER_DT, STRING_DT</code>.
     * @return The list value types
     */
    public ShortList getListValueTypes();

}
