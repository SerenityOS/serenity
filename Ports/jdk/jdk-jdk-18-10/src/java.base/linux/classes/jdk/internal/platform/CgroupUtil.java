/*
 * Copyright (c) 2020, Red Hat Inc.
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

package jdk.internal.platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.stream.Stream;

public final class CgroupUtil {

    @SuppressWarnings("removal")
    public static Stream<String> readFilePrivileged(Path path) throws IOException {
        try {
            PrivilegedExceptionAction<Stream<String>> pea = () -> Files.lines(path);
            return AccessController.doPrivileged(pea);
        } catch (PrivilegedActionException e) {
            unwrapIOExceptionAndRethrow(e);
            throw new InternalError(e.getCause());
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    static void unwrapIOExceptionAndRethrow(PrivilegedActionException pae) throws IOException {
        Throwable x = pae.getCause();
        if (x instanceof IOException)
            throw (IOException) x;
        if (x instanceof RuntimeException)
            throw (RuntimeException) x;
        if (x instanceof Error)
            throw (Error) x;
    }

    static String readStringValue(CgroupSubsystemController controller, String param) throws IOException {
        PrivilegedExceptionAction<BufferedReader> pea = () ->
                Files.newBufferedReader(Paths.get(controller.path(), param));
        try (@SuppressWarnings("removal") BufferedReader bufferedReader =
                     AccessController.doPrivileged(pea)) {
            String line = bufferedReader.readLine();
            return line;
        } catch (PrivilegedActionException e) {
            unwrapIOExceptionAndRethrow(e);
            throw new InternalError(e.getCause());
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @SuppressWarnings("removal")
    public static List<String> readAllLinesPrivileged(Path path) throws IOException {
        try {
            PrivilegedExceptionAction<List<String>> pea = () -> Files.readAllLines(path);
            return AccessController.doPrivileged(pea);
        } catch (PrivilegedActionException e) {
            unwrapIOExceptionAndRethrow(e);
            throw new InternalError(e.getCause());
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
