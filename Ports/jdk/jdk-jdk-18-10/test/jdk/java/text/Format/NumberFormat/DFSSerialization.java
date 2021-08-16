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
 * @test
 * @bug 4068067
 * @library /java/text/testlib
 * @build DFSSerialization IntlTest HexDumpReader
 * @run main DFSSerialization
 * @summary Three different tests are done.
 *    1. read from the object created using jdk1.4.2
 *    2. create a valid DecimalFormatSymbols object with current JDK, then read the object
 *    3. Try to create an valid DecimalFormatSymbols object by passing null to set null
 *       for the exponent separator symbol. Expect the NullPointerException.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class DFSSerialization extends IntlTest{
    public static void main(String[] args) throws Exception {
        new DFSSerialization().run(args);
    }
    public void TestDFSSerialization(){
        /*
         * 1. read from the object created using jdk1.4.2
         */
        File oldFile = new File(System.getProperty("test.src", "."), "DecimalFormatSymbols.142.txt");
        DecimalFormatSymbols dfs142 = readTestObject(oldFile);
        if (dfs142 != null){
            if (dfs142.getExponentSeparator().equals("E") && dfs142.getCurrencySymbol().equals("*SpecialCurrencySymbol*")){
                System.out.println("\n  Deserialization of JDK1.4.2 Object from the current JDK: Passed.");
                logln(" Deserialization of JDK1.4.2 Object from the current JDK: Passed.");
            } else {
                errln(" Deserialization of JDK1.4.2 Object from the current JDK was Failed:"
                      +dfs142.getCurrencySymbol()+" "+dfs142.getExponentSeparator());
                /*
                 * logically should not throw this exception as errln throws exception
                 * if not thrown yet - but in case errln got changed
                 */
                throw new RuntimeException(" Deserialization of JDK1.4.2 Object from the current JDK was Failed:"
                                           +dfs142.getCurrencySymbol()+" "+dfs142.getExponentSeparator());
            }
        }
        /*
         * 2. create a valid DecimalFormatSymbols object with current JDK, then read the object
         */
        String validObject = "DecimalFormatSymbols.current";
        File currentFile = createTestObject(validObject, "*SpecialExponentSeparator*");

        DecimalFormatSymbols dfsValid = readTestObject(currentFile);
        if (dfsValid != null){
            if (dfsValid.getExponentSeparator().equals("*SpecialExponentSeparator*") &&
                dfsValid.getCurrencySymbol().equals("*SpecialCurrencySymbol*")){
                System.out.println("  Deserialization of current JDK Object from the current JDK: Passed.");
                logln(" Deserialization of current JDK Object from the current JDK: Passed.");
            } else {
                errln(" Deserialization of current JDK Object from the current JDK was Failed:"
                      +dfsValid.getCurrencySymbol()+" "+dfsValid.getExponentSeparator());
                /*
                 * logically should not throw this exception as errln throws exception
                 * if not thrown yet - but in case errln got changed
                 */
                throw new RuntimeException(" Deserialization of current Object from the current JDK was Failed:"
                                           +dfsValid.getCurrencySymbol()+" "+dfsValid.getExponentSeparator());
            }
        }
        /*
         * 3. Try to create an valid DecimalFormatSymbols object by passing null
         *    to set null for the exponent separator symbol. Expect the NullPointerException.
         */
        DecimalFormatSymbols symNPE = new DecimalFormatSymbols(Locale.US);
        boolean npePassed = false;
        try {
            symNPE.setExponentSeparator(null);
        } catch (NullPointerException npe){
            npePassed = true;
            System.out.println("  Trying to set exponent separator with null: Passed.");
            logln(" Trying to set exponent separator with null: Passed.");
        }
        if (!npePassed){
            System.out.println(" Trying to set exponent separator with null:Failed.");
            errln("  Trying to set exponent separator with null:Failed.");
            /*
             * logically should not throw this exception as errln throws exception
             * if not thrown yet - but in case errln got changed
             */
            throw new RuntimeException(" Trying to set exponent separator with null:Failed.");
        }

    }

    private DecimalFormatSymbols readTestObject(File inputFile){
        try (InputStream istream = inputFile.getName().endsWith(".txt") ?
                                       HexDumpReader.getStreamFromHexDump(inputFile) :
                                       new FileInputStream(inputFile)) {
            ObjectInputStream p = new ObjectInputStream(istream);
            DecimalFormatSymbols dfs = (DecimalFormatSymbols)p.readObject();
            return dfs;
        } catch (Exception e) {
            errln("Test Malfunction in DFSSerialization: Exception while reading the object");
            /*
             * logically should not throw this exception as errln throws exception
             * if not thrown yet - but in case errln got changed
             */
            throw new RuntimeException("Test Malfunction: re-throwing the exception", e);
        }
    }

    private File createTestObject(String objectName, String expString){
        DecimalFormatSymbols dfs= new DecimalFormatSymbols();
        dfs.setExponentSeparator(expString);
        dfs.setCurrencySymbol("*SpecialCurrencySymbol*");
        logln(" The special exponent separator is set : "  + dfs.getExponentSeparator());
        logln(" The special currency symbol is set : "  + dfs.getCurrencySymbol());

        // 6345659: create a test object in the test.class dir where test user has a write permission.
        File file = new File(System.getProperty("test.class", "."), objectName);
        try (FileOutputStream ostream = new FileOutputStream(file)) {
            ObjectOutputStream p = new ObjectOutputStream(ostream);
            p.writeObject(dfs);
            //System.out.println(" The special currency symbol is set : "  + dfs.getCurrencySymbol());
            return file;
        } catch (Exception e){
            errln("Test Malfunction in DFSSerialization: Exception while creating an object");
            /*
             * logically should not throw this exception as errln throws exception
             * if not thrown yet - but in case errln got changed
             */
            throw new RuntimeException("Test Malfunction: re-throwing the exception", e);
        }
    }
}
