/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package util;

import java.io.File;
import java.io.IOException;
import java.sql.DriverAction;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple java.sql.Driver stub class that registers the driver via a static
 * block with a DriverAction Implementation
 * @author ljanders
 */
public class StubDriverDA extends StubDriver {

    public static final String DriverActionCalled = "DriverActionCalled.txt";

    static DriverAction da;

    static {
        try {
            DriverManager.registerDriver(new StubDriverDA(), da);
        } catch (SQLException ex) {
            Logger.getLogger(StubDriverDA.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public StubDriverDA() {
        da = new DriverActionImpl(this);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.matches("^jdbc:luckydog:.*");
    }

    /**
     * This method will write out a text file when called by the
     * DriverActionImpl.release method when DriverManager.deregisterDriver
     * is called. This is used by DriverManagerTests to validate that
     * DriverAction.release was called
     */
    protected void release() {
        File file = new File(DriverActionCalled);
        try {
            file.createNewFile();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
