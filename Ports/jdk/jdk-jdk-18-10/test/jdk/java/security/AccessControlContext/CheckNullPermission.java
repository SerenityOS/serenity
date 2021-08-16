/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4232768
 * @summary Make sure NullPointerException is raised when permission to check
 * for is null
 */

import java.security.ProtectionDomain;
import java.security.AccessControlContext;

public class CheckNullPermission {

   public static void main (String argv[]) throws Exception {
       ProtectionDomain pd[] = new ProtectionDomain[1];

       try {
           (new AccessControlContext(pd)).checkPermission(null);
           throw new Exception("Expected NullPointerException not thrown");
       } catch (NullPointerException npe) {
       }
   }
}
