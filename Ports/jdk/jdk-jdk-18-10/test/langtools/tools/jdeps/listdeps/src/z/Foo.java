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

package z;

import java.sql.Driver;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
/*
 * Dependences on java.sql and java.logging which can be reduced.
 */
public class Foo {
    // dependence to java.logging
    static Logger log = Logger.getLogger("foo");
    static final String isCoalescing = XMLInputFactory.IS_COALESCING;

    // dependence to java.sql
    public Driver getDriver() { return null; }

    // dependence to same package
    public Bar getBar() { return new Bar(); }

    // dependence to module m
    public String isCoalescing() { return lib.Lib.isCoalescing; }


}
