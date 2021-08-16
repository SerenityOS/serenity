/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test 1.1, 06/11/07
 * @author Xuelei Fan
 * @bug 6466247
 * @summary java.security.debug permission=<classname> and codebase=<URL>
 *          options do not work
 * @modules java.base/sun.security.util
 * @run main/othervm -Djava.security.debug="stacknothing--=-30logincontextacCess:stack-domain,combiner;access:fAilure-jarpermission=sun.dummy.DummyPermission;peRmiSsion=sun.Dummy.DummyPermission2=permission=sun.dummy.DummyPermission3:codEbAse=/dir1/DIR2/Dir3/File.java,codebase=http://www.sun.com/search?q=SunMicro,codEbAse=/dir1/DIR2/Dir3/File.java;coDebase=www.sun.com;codebase=file:///C:/temp/foo%20more/a.txt" MultiOptions
 */
import sun.security.util.Debug;

public class MultiOptions
{
    public static void main(String args[]) throws Exception {

        if (!Debug.isOn("access") ||
                !Debug.isOn("stack") ||
                !Debug.isOn("logincontext") ||
                !Debug.isOn("domain") ||
                !Debug.isOn("combiner") ||
                !Debug.isOn("failure") ||
                !Debug.isOn("jar") ||
                !Debug.isOn("permission=sun.dummy.DummyPermission") ||
                Debug.isOn("permission=sun.dummy.dummypermission") ||
                !Debug.isOn("permission=sun.Dummy.DummyPermission2") ||
                !Debug.isOn("permission=sun.dummy.DummyPermission3") ||
                !Debug.isOn("codebase=/dir1/DIR2/Dir3/File.java") ||
                Debug.isOn("codebase=/dir1/dir2/dir3/file.java") ||
                !Debug.isOn("codebase=www.sun.com") ||
                !Debug.isOn("codebase=file:///C:/temp/foo%20more/a.txt") ||
                !Debug.isOn("codebase=http://www.sun.com/search?q=SunMicro") ) {
            throw new Exception("sun.security.Debug failed to parse options");
        }
    }

}
