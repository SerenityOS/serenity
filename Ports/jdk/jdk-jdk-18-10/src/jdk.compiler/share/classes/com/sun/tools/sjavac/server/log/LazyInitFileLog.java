/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.tools.sjavac.server.log;

import com.sun.tools.sjavac.Log;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LazyInitFileLog extends Log {

    String baseFilename;
    Path destination = null;

    public LazyInitFileLog(String baseFilename) {
        super(null, null);
        this.baseFilename = baseFilename;
    }

    protected void printLogMsg(Level msgLevel, String msg) {
        try {
            // Lazily initialize out/err
            if (out == null && isLevelLogged(msgLevel)) {
                destination = getAvailableDestination();
                out = err = new PrintWriter(new FileWriter(destination.toFile()), true);
            }
            // Proceed to log the message
            super.printLogMsg(msgLevel, msg);
        } catch (IOException e) {
            // This could be bad. We might have run into an error and we can't
            // log it. Resort to printing on stdout.
            System.out.println("IO error occurred: " + e.getMessage());
            System.out.println("Original message: [" + msgLevel + "] " + msg);
        }
    }

    /**
     * @return The first available path of baseFilename, baseFilename.1,
     * basefilename.2, ...
     */
    private Path getAvailableDestination() {
        Path p = Paths.get(baseFilename);
        int i = 1;
        while (Files.exists(p)) {
            p = Paths.get(baseFilename + "." + i++);
        }
        return p;
    }

    public Path getLogDestination() {
        return destination;
    }
}
