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
package test.sql.othervm;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

public class DriverManagerInitTests {

    /**
     * Validate that when DriverManager loads the initial JDBC drivers, that the
     * output from DriverManager.println is available by verifying that the
     * String "JDBC DriverManager initialized" is found
     */
    @Test
    public void test() {

        CharArrayWriter cw = new CharArrayWriter();
        PrintWriter pw = new PrintWriter(cw);
        DriverManager.setLogWriter(pw);
        Enumeration<Driver> drivers = DriverManager.getDrivers();

        try (BufferedReader reader = new BufferedReader(new CharArrayReader(cw.toCharArray()))) {
            boolean result
                    = reader.lines().anyMatch(
                            line -> line.matches(".*JDBC DriverManager initialized.*"));
            assertTrue(result);

        } catch (IOException ex) {
            Logger.getLogger(DriverManagerInitTests.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }

        // Check to verify that we are not initializing a 2nd time
        cw = new CharArrayWriter();
        pw = new PrintWriter(cw);
        DriverManager.setLogWriter(pw);
        drivers = DriverManager.getDrivers();

        try (BufferedReader reader = new BufferedReader(new CharArrayReader(cw.toCharArray()))) {
            boolean result
                    = reader.lines().noneMatch(
                            line -> line.matches(".*JDBC DriverManager initialized.*"));
            assertTrue(result);

        } catch (IOException ex) {
            Logger.getLogger(DriverManagerInitTests.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }

    }

}
