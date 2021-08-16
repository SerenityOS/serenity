/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package propertiesparser.parser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Class to facilitate manipulating compiler.properties.
 */
public class MessageFile {

    MessageLine firstLine;
    public Map<String, Message> messages = new TreeMap<>();
    public File file;
    public String keyPrefix;

    public MessageFile(File file, String keyPrefix) throws IOException {
        this.file = file;
        this.keyPrefix = keyPrefix;
        read(file);
    }

    final void read(File in) throws IOException {
        MessageLine currLine = null;
        for (String line : Files.readAllLines(in.toPath())) {
            if (currLine == null)
                firstLine = currLine = new MessageLine(line);
            else
                currLine = currLine.append(line);
            if (line.startsWith(keyPrefix + ".")) {
                int eq = line.indexOf("=");
                if (eq > 0)
                    messages.put(line.substring(0, eq).trim(), new Message(currLine));
            }
        }
    }
}
