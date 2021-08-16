/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.rowset.internal;

import org.xml.sax.*;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * An implementation of the <code>EntityResolver</code> interface, which
 * reads and parses an XML formatted <code>WebRowSet</code> object.
 * This is an implementation of org.xml.sax
 *
 */
public class XmlResolver implements EntityResolver {

        public InputSource resolveEntity(String publicId, String systemId) {
           String schemaName = systemId.substring(systemId.lastIndexOf('/'));

           if(systemId.startsWith("http://java.sun.com/xml/ns/jdbc")) {
               return new InputSource(this.getClass().getResourceAsStream(schemaName));

           } else {
              // use the default behaviour
              return null;
           }




       }
}
