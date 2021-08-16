/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @bug 4274267
  @summary Tests that externalized DataFlavor is restored properly
  @author prs@sparc.spb.su: area=
  @modules java.datatransfer
  @run main ExternalizeTest
*/

import java.awt.datatransfer.DataFlavor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ExternalizeTest {

   public static void main(String[] args) {
       DataFlavor df = new DataFlavor("text/enriched; charset=ascii", "Enrich Flavor");

       storeDataFlavor(df);
       DataFlavor df1 = retrieveDataFlavor();

       if (!df.equals(df1)) {
           throw new RuntimeException("FAILED: restored DataFlavor is not equal to externalized one");
       }

   }

   public static void storeDataFlavor(DataFlavor dfs){
       // To store the dataflavor into a file using writeExternal()
       try {
           FileOutputStream ostream = new FileOutputStream("t.tmp");
           ObjectOutputStream p = new ObjectOutputStream(ostream);
           dfs.writeExternal(p);
           ostream.close();

       } catch (Exception ex){
           throw new RuntimeException("FAIL: problem occured while storing DataFlavor");
       }
   }


   public static DataFlavor retrieveDataFlavor(){
       DataFlavor df=DataFlavor.stringFlavor;
       try {
           FileInputStream istream = new FileInputStream("t.tmp");
           ObjectInputStream p = new ObjectInputStream(istream);
           df.readExternal(p);
           istream.close();
       } catch (Exception ex){
           throw new RuntimeException("FAIL: problem occured while retrieving DataFlavor");
       }

       return df;
   }
}

