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

import javax.naming.directory.DirContext;
import java.util.Hashtable;

/**
 * The test base class for DNS related tests, extends TestBase.
 *
 * This class override some methods of supper class to provide DNS tests shared
 * base logic.
 *
 * @see TestBase
 */
public abstract class DNSTestBase extends TestBase {
    private Hashtable<Object, Object> env;
    private DirContext ctx;
    private boolean localServer;

    public DNSTestBase() {
        // set default, we will run with local server playback by default
        setLocalServer(true);
    }

    @Override public void initTest(String[] args) {
        // init env which will used later to initial dir context
        env = DNSTestUtils
                .initEnv(localServer, this.getClass().getName(), args);
    }

    @Override public void cleanupTest() {
        // cleanup dir context
        DNSTestUtils.cleanup(ctx);
        // stop local server if there is
        Server server = (Server) env.get(DNSTestUtils.TEST_DNS_SERVER_THREAD);
        if (server != null) {
            server.stopServer();
        }
    }

    public Hashtable<Object, Object> env() {
        if (env == null) {
            throw new RuntimeException("Test had not been initialized");
        }

        return env;
    }

    public DirContext context() {
        if (ctx == null) {
            throw new RuntimeException("Context had not been initialized");
        }

        return ctx;
    }

    public void setContext(DirContext context) {
        ctx = context;
    }

    public void setLocalServer(boolean isRequired) {
        this.localServer = isRequired;
    }
}
