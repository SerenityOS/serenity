/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.xml.internal.stream.dtd.nonvalidating;
/**
 */
public class XMLSimpleType {

    //
    // Constants
    //

    /** TYPE_CDATA */
    public static final short TYPE_CDATA = 0;

    /** TYPE_ENTITY */
    public static final short TYPE_ENTITY = 1;

    /** TYPE_ENUMERATION */
    public static final short TYPE_ENUMERATION = 2;

    /** TYPE_ID */
    public static final short TYPE_ID = 3;

    /** TYPE_IDREF */
    public static final short TYPE_IDREF = 4;

    /** TYPE_NMTOKEN */
    public static final short TYPE_NMTOKEN = 5;

    /** TYPE_NOTATION */
    public static final short TYPE_NOTATION = 6;

    /** TYPE_NAMED */
    public static final short TYPE_NAMED = 7;

    /** DEFAULT_TYPE_DEFAULT */
    public static final short DEFAULT_TYPE_DEFAULT = 3;

    /** DEFAULT_TYPE_FIXED */
    public static final short DEFAULT_TYPE_FIXED = 1;

    /** DEFAULT_TYPE_IMPLIED */
    public static final short DEFAULT_TYPE_IMPLIED = 0;

    /** DEFAULT_TYPE_REQUIRED */
    public static final short DEFAULT_TYPE_REQUIRED = 2;

    //
    // Data
    //

    /** type */
    public short type;

    /** name */
    public String name;

    /** enumeration */
    public String[] enumeration;

    /** list */
    public boolean list;

    /** defaultType */
    public short defaultType;

    /** defaultValue */
    public String defaultValue;

    /** non-normalized defaultValue */
    public String nonNormalizedDefaultValue;


    //
    // Methods
    //

    /**
     * setValues
     *
     * @param type
     * @param name
     * @param enumeration
     * @param list
     * @param defaultType
     * @param defaultValue
     * @param nonNormalizedDefaultValue
     * @param datatypeValidator
     */
    public void setValues(short type, String name, String[] enumeration,
    boolean list, short defaultType,
    String defaultValue, String nonNormalizedDefaultValue){

        this.type              = type;
        this.name              = name;
        // REVISIT: Should this be a copy? -Ac
        if (enumeration != null && enumeration.length > 0) {
            this.enumeration = new String[enumeration.length];
            System.arraycopy(enumeration, 0, this.enumeration, 0, this.enumeration.length);
        }
        else {
            this.enumeration = null;
        }
        this.list              = list;
        this.defaultType       = defaultType;
        this.defaultValue      = defaultValue;
        this.nonNormalizedDefaultValue      = nonNormalizedDefaultValue;

    } // setValues(short,String,String[],boolean,short,String,String,DatatypeValidator)

    /** Set values. */
    public void setValues(XMLSimpleType simpleType) {

        type = simpleType.type;
        name = simpleType.name;
        // REVISIT: Should this be a copy? -Ac
        if (simpleType.enumeration != null && simpleType.enumeration.length > 0) {
            enumeration = new String[simpleType.enumeration.length];
            System.arraycopy(simpleType.enumeration, 0, enumeration, 0, enumeration.length);
        }
        else {
            enumeration = null;
        }
        list = simpleType.list;
        defaultType = simpleType.defaultType;
        defaultValue = simpleType.defaultValue;
        nonNormalizedDefaultValue = simpleType.nonNormalizedDefaultValue;

    } // setValues(XMLSimpleType)

    /**
     * clear
     */
    public void clear() {
        this.type              = -1;
        this.name              = null;
        this.enumeration       = null;
        this.list              = false;
        this.defaultType       = -1;
        this.defaultValue      = null;
        this.nonNormalizedDefaultValue = null;
    }

} // class XMLSimpleType
