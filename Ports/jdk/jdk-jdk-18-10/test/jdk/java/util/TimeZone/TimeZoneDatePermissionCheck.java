/* Testcase for PR381 Stackoverflow error with security manager, signed jars
 * and -Djava.security.debug set.
 * Copyright (c) 2009, Red Hat Inc.
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

import java.util.Date;

/**
 * Test class. Create a test keystore and dummy cert, create a jar file to
 * sign with the test class in it. Sign it run it with the security manager
 * on, plus accesscontroller debugging, will go into infinite recursion
 * trying to get enough permissions for printing Date of failing
 * certificate, unless fix is applied.
 */
public class TimeZoneDatePermissionCheck
{
  public static void main(String[] args)
  {
    System.out.println(new Date());
  }
}
