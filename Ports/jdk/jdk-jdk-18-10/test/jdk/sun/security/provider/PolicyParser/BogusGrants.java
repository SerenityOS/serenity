/*
 * Copyright (c) 1999, 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @author Gary Ellison
 * @bug 4233900
 * @summary Catch anomalies in Policy parsing
 * @modules java.base/sun.security.provider
 * @run main BogusGrants p001.policy p002.policy p003.policy p004.policy
 */

import java.io.*;
import java.util.Enumeration;
import sun.security.provider.*;

public class BogusGrants {

   public static void main(String args[]) throws Exception {
       String dir = System.getProperty("test.src", ".");
       for (int i=0; i < args.length; i++) {
           try {
               PolicyParser pp = new PolicyParser(true);
               String pfile =  new File(dir, args[i]).getPath();

               pp.read(new FileReader(pfile));
               Enumeration ge = pp.grantElements();
               if (ge.hasMoreElements()) {
                   throw new Exception("PolicyFile " + pfile + " grant entry should not parse but it did");
               }
           } catch
               (sun.security.provider.PolicyParser.ParsingException p) {
               System.out.println("Passed test " + i +
                                  ": Bogus grant entry caught " +
                                  p.getMessage());
           }
       }
   }
}
