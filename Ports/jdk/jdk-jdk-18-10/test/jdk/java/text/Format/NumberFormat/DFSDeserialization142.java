/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * No at-test for this test, because it needs to be run on older version JDK than 1.6 to test.
 * It was tested using 1.4.2. The file object was created using JDK1.6.
 */



import java.awt.*;
import java.text.*;
import java.util.*;
import java.io.*;

public class DFSDeserialization142{

    public static void main(String[] args)
    {
        try {

           File file = new File("DecimalFormatSymbols.current");
           FileInputStream istream = new FileInputStream(file);
           ObjectInputStream p = new ObjectInputStream(istream);
           DecimalFormatSymbols dfs = (DecimalFormatSymbols)p.readObject();
           if (dfs.getCurrencySymbol().equals("*SpecialCurrencySymbol*")){
                System.out.println("Serialization/Deserialization Test Passed.");
           }else{
                throw new Exception("Serialization/Deserialization Test Failed:"+dfs.getCurrencySymbol());
           }
           istream.close();
       } catch (Exception e) {
            e.printStackTrace();
          }
     }
}
