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

import java.util.Map;

/*
 * A specific SSL/TLS communication test case on two peers.
 */
public class TestCase<U extends UseCase> {

    // The server side use case.
    public final U serverCase;

    // The client side use case.
    public final U clientCase;

    private Status status;

    public TestCase(U serverCase, U clientCase) {
        this.serverCase = serverCase;
        this.clientCase = clientCase;
    }

    public TestCase<U> addServerProp(String prop, String value) {
        serverCase.addProp(prop, value);
        return this;
    }

    public String getServerProp(String prop) {
        return serverCase.getProp(prop);
    }

    public TestCase<U> addAllServerProps(Map<String, String> props) {
        serverCase.addAllProps(props);
        return this;
    }

    public Map<String, String> getAllServerProps() {
        return serverCase.getAllProps();
    }

    public TestCase<U> removeServerProp(String prop) {
        serverCase.removeProp(prop);
        return this;
    }

    public TestCase<U> removeAllServerProp() {
        serverCase.removeAllProps();
        return this;
    }

    public TestCase<U> addClientProp(String prop, String value) {
        clientCase.addProp(prop, value);
        return this;
    }

    public String getClientProp(String prop) {
        return clientCase.getProp(prop);
    }

    public TestCase<U> addAllClientProps(Map<String, String> props) {
        clientCase.addAllProps(props);
        return this;
    }

    public Map<String, String> getAllClientProps() {
        return clientCase.getAllProps();
    }

    public TestCase<U> removeClientProp(String prop) {
        clientCase.removeProp(prop);
        return this;
    }

    public TestCase<U> removeAllClientProp() {
        clientCase.removeAllProps();
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Server: " + serverCase + "\n" + "Client: " + clientCase;
    }
}
