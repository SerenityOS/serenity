/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

package pkg;

import java.io.File;

/*****************************************************
 * {@linkplain java.lang.String Link-Plain to String Class}.
 ****************************************************/
public class B {
    /**
     * A method with html tag the method {@link ClassLoader#getSystemClassLoader()
     * <b>getSystemClassLoader()</b>} as the parent class loader.
     */
    public void  method1() {}

    /**
     * is equivalent to invoking <code>{@link #createTempFile(java.lang.String,
     * java.lang.String, java.io.File)
     * createTempFile(prefix,&nbsp;suffix,&nbsp;null)}</code>.
     * */
    public void method2() {}

    /**
     * A required method to satisfy other tests.
     * @param s1 parameter
     * @param s2 parameter
     * @param f  parameter
     */
    public void createTempFile(String s1, String s2, File f){}

   /**
    * A external links, complicated by whitespace
    * @see <a href="http://www.ietf.org/rfc/rfc2279.txt"><i>RFC&nbsp;2279: UTF-8, a
    * transformation format of ISO 10646</i></a>
    * @see <a href="http://www.ietf.org/rfc/rfc2373.txt"><i>RFC&nbsp;2373: IPv6 Addressing
    * Architecture</i></a>
    * @see <a href="http://www.ietf.org/rfc/rfc2396.txt"><i>RFC&nbsp;2396: Uniform
    * Resource Identifiers (URI): Generic Syntax</i></a>
    * @see <a href="http://www.ietf.org/rfc/rfc2732.txt"><i>RFC&nbsp;2732: Format for
    * Literal IPv6 Addresses in URLs</i></a>
    * @see <a href="C.html">A nearby file</a>
    */
   public void externalLink() {}
}
