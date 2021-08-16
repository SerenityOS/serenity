/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/*
 * An abstract server.
 */
public abstract class AbstractServer extends AbstractPeer implements Server {

    @Override
    protected void printLog() throws IOException {
        System.out.println("---------- Server log start ----------");
        super.printLog();
        System.out.println("---------- Server log end ----------");
    }

    /*
     * Generally, server outputs logs to a separated file.
     * For some cases, this file could be used as a synchronizer between
     * server and client.
     */
    @Override
    public Path getLogPath() {
        return Paths.get("server.log");
    }

    @Override
    public void signalStop() throws Exception { }

    public static abstract class Builder extends AbstractPeer.Builder {

        private int port;

        // Indicates if requires client authentication.
        private boolean clientAuth = true;

        public int getPort() {
            return port;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public boolean getClientAuth() {
            return clientAuth;
        }

        public Builder setClientAuth(boolean clientAuth) {
            this.clientAuth = clientAuth;
            return this;
        }

        public abstract AbstractServer build() throws Exception;
    }
}
