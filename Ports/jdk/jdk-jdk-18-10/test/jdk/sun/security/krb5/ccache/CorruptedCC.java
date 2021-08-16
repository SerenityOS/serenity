/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8028780
 * @summary JDK KRB5 module throws OutOfMemoryError when CCache is corrupt
 * @modules java.security.jgss/sun.security.krb5
 *          java.security.jgss/sun.security.krb5.internal.ccache
 * @run main/othervm -Xmx8m CorruptedCC
 */
import java.nio.file.Files;
import java.nio.file.Paths;
import sun.security.krb5.internal.ccache.CredentialsCache;

public class CorruptedCC {
    public static void main(String[] args) throws Exception {
        for (int i=0; i<TimeInCCache.ccache.length; i++) {
            byte old = TimeInCCache.ccache[i];
            TimeInCCache.ccache[i] = 0x7f;
            Files.write(Paths.get("tmpcc"), TimeInCCache.ccache);
            // The next line will return null for I/O issues. That's OK.
            CredentialsCache.getInstance("tmpcc");
            TimeInCCache.ccache[i] = old;
        }
    }
}
