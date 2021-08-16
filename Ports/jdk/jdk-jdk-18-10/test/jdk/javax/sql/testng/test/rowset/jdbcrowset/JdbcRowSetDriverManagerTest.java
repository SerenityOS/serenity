/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package test.rowset.jdbcrowset;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import org.testng.annotations.Test;
import util.BaseTest;
import util.StubDriver;

public class JdbcRowSetDriverManagerTest extends BaseTest {

    // URL that the StubDriver recognizes
    private final static String StubDriverURL = "jdbc:tennis:boy";

    /**
     * Validate that JDBCRowSetImpl can connect to a JDBC driver that is
     * register by DriverManager.
     */
    @Test(enabled = true)
    public void test0000() throws SQLException {

        DriverManager.registerDriver(new StubDriver());

        // Show that the StubDriver is loaded and then call setAutoCommit on
        // the returned Connection
        dumpRegisteredDrivers();
        Connection con = DriverManager.getConnection(StubDriverURL, "userid", "password");
        con.setAutoCommit(true);

        // Have com.sun.rowset.JdbcRowSetImpl create a Connection and
        // then call setAutoCommit
        RowSetFactory rsf = RowSetProvider.newFactory();
        JdbcRowSet jrs = rsf.createJdbcRowSet();
        jrs.setUrl(StubDriverURL);
        jrs.setUsername("userid");
        jrs.setPassword("password");

        jrs.setAutoCommit(true);
    }

    private static void dumpRegisteredDrivers() {
        System.out.println("+++ Loaded Drivers +++");
        System.out.println("++++++++++++++++++++++++");
        DriverManager.drivers()
                .forEach(d
                        -> System.out.println("+++ Driver:" + d + " "
                        + d.getClass().getClassLoader()));
        System.out.println("++++++++++++++++++++++++");

    }
}
