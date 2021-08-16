/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl.dv;

import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import jdk.xml.internal.SecuritySupport;

/**
 * Base class for datatype exceptions. For DTD types, the exception can be
 * created from an error message. For Schema types, it needs an error code
 * (as defined in Appendix C of the structure spec), plus an array of arguents,
 * for error message substitution.
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 *
 * @LastModified: Sep 2017
 */
public class DatatypeException extends Exception {

    /** Serialization version. */
    static final long serialVersionUID = 1940805832730465578L;

    // used to store error code and error substitution arguments
    protected String key;
    protected Object[] args;

    /**
     * Create a new datatype exception by providing an error code and a list
     * of error message substitution arguments.
     *
     * @param key  error code
     * @param args error arguments
     */
    public DatatypeException(String key, Object[] args) {
        super(key);
        this.key = key;
        this.args = args;
    }

    /**
     * Return the error code
     *
     * @return  error code
     */
    public String getKey() {
        return key;
    }

    /**
     * Return the list of error arguments
     *
     * @return  error arguments
     */
    public Object[] getArgs() {
        return args;
    }

    /**
     * Overrides this method to get the formatted&localized error message.
     *
     * REVISIT: the system locale is used to load the property file.
     *          do we want to allow the appilcation to specify a
     *          different locale?
     */
    public String getMessage() {
        ResourceBundle resourceBundle = null;
        resourceBundle = SecuritySupport.getResourceBundle("com.sun.org.apache.xerces.internal.impl.msg.XMLSchemaMessages");
        if (resourceBundle == null)
            throw new MissingResourceException("Property file not found!", "com.sun.org.apache.xerces.internal.impl.msg.XMLSchemaMessages", key);

        String msg = resourceBundle.getString(key);
        if (msg == null) {
            msg = resourceBundle.getString("BadMessageKey");
            throw new MissingResourceException(msg, "com.sun.org.apache.xerces.internal.impl.msg.XMLSchemaMessages", key);
        }

        if (args != null) {
            try {
                msg = java.text.MessageFormat.format(msg, args);
            } catch (Exception e) {
                msg = resourceBundle.getString("FormatFailed");
                msg += " " + resourceBundle.getString(key);
            }
        }

        return msg;
    }
}
