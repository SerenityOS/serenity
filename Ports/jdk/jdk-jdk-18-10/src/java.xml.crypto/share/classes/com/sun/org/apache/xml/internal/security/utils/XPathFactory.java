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
package com.sun.org.apache.xml.internal.security.utils;


/**
 * A Factory to return an XPathAPI instance. If Xalan is available it returns XalanXPathAPI. If not, then
 * it returns JDKXPathAPI.
 */
public abstract class XPathFactory {

    private static final boolean xalanInstalled;

    static {
        boolean installed = false;
        try {
            Class<?> funcTableClass =
                ClassLoaderUtils.loadClass("com.sun.org.apache.xpath.internal.compiler.FunctionTable", XPathFactory.class);
            if (funcTableClass != null) {
                installed = true;
            }
        } catch (Exception e) { //NOPMD
            //ignore
        }
        xalanInstalled = installed;
    }

    /**
     * Get a new XPathFactory instance
     */
    public static XPathFactory newInstance() {
        // Xalan is available
        if (xalanInstalled && XalanXPathAPI.isInstalled()) {
            return new XalanXPathFactory();
        }
        // Some problem was encountered in fixing up the Xalan FunctionTable so fall back to the
        // JDK implementation
        return new JDKXPathFactory();
    }

    /**
     * Get a new XPathAPI instance
     */
    public abstract XPathAPI newXPathAPI();

}
