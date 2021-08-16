/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package test;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.lang.Class;
import java.lang.String;
import java.lang.System;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.xml.sax.helpers.XMLReaderFactory;

public class XMLFactoryHelper {
    /*
     * instantiate a xml factory by reflection e.g.
     * DocumentBuilderFactory.newInstance()
     */
    public static Object instantiateXMLService(String serviceName) throws Exception {
        ClassLoader backup = Thread.currentThread().getContextClassLoader();
        try {
            // set thread context class loader to module class loader
            Thread.currentThread().setContextClassLoader(XMLFactoryHelper.class.getClassLoader());
            if (serviceName.equals("org.xml.sax.XMLReader"))
                return XMLReaderFactory.createXMLReader();
            else if (serviceName.equals("javax.xml.validation.SchemaFactory"))
                return Class.forName(serviceName).getMethod("newInstance", String.class)
                        .invoke(null, W3C_XML_SCHEMA_NS_URI);
            else
                return Class.forName(serviceName).getMethod("newInstance").invoke(null);
        } finally {
            Thread.currentThread().setContextClassLoader(backup);
        }

    }

}
