/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTError;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.desktop.OpenFilesEvent;
import java.awt.desktop.OpenFilesHandler;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.Collections;

public class Hello implements OpenFilesHandler {

    public static void main(String[] args) throws IOException, InterruptedException {
        var faFiles = getFaFiles();
        if (faFiles != null) {
            // Some files got opened through fa mechanizm.
            // They are the arguments then.
            args = faFiles.toArray(String[]::new);
        }

        var lines = printArgs(args);

        Stream.of(args).forEach(arg -> System.out.println(
                arg.codePoints()
                        .mapToObj(codePoint -> String.format("0x%04x", codePoint))
                        .collect(Collectors.joining(",", "[", "]"))));

        lines.forEach(System.out::println);

        var outputFile = getOutputFile(args);
        trace(String.format("Output file: [%s]", outputFile));
        Files.write(outputFile, lines);
    }

    private static List<String> printArgs(String[] args) {
        List<String> lines = new ArrayList<>();
        lines.add(MSG);

        lines.add("args.length: " + args.length);

        for (String arg : args) {
            if (arg.startsWith("jpackage.app")) {
                lines.add(arg + "=" + System.getProperty(arg));
            } else {
                lines.add(arg);
            }
        }

        for (int index = 1; index <= EXPECTED_NUM_OF_PARAMS; index++) {
            String value = System.getProperty("param" + index);
            if (value != null) {
                lines.add("-Dparam" + index + "=" + value);
            }
        }

        return lines;
    }

    private static Path getOutputFile(String[] args) {
        Path outputFilePath = Path.of("appOutput.txt");

        // If first arg is a file (most likely from fa), then put output in the same folder as
        // the file from fa.
        if (args.length >= 1) {
            Path faPath = Path.of(args[0]);
            if (Files.exists(faPath)) {
                return faPath.toAbsolutePath().getParent().resolve(outputFilePath);
            }
        }

        try {
            // Try writing in the default output file.
            Files.write(outputFilePath, Collections.emptyList());
            return outputFilePath;
        } catch (IOException ex) {
            // Log reason of a failure.
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            Stream.of(errors.toString().split("\\R")).forEachOrdered(Hello::trace);
        }

        return Path.of(System.getProperty("user.home")).resolve(outputFilePath);
    }

    @Override
    public void openFiles(OpenFilesEvent e) {
        synchronized(lock) {
            trace("openFiles");
            files = e.getFiles().stream()
                .map(File::toString)
                .collect(Collectors.toList());

            lock.notifyAll();
        }
    }

    private static List<String> getFaFiles() throws InterruptedException {
        if (openFilesHandler == null) {
            return null;
        }

        synchronized(openFilesHandler.lock) {
            trace("getFaFiles: wait");
            openFilesHandler.lock.wait(1000);
            if (openFilesHandler.files == null) {
                trace(String.format("getFaFiles: no files"));
                return null;
            }
            // Return copy of `files` to keep access to `files` field synchronized.
            trace(String.format("getFaFiles: file count %d",
                    openFilesHandler.files.size()));
            return new ArrayList<>(openFilesHandler.files);
        }
    }

    private List<String> files;
    private final Object lock = new Object();
    private final static Hello openFilesHandler = createInstance();

    private static Hello createInstance() {
        if (GraphicsEnvironment.isHeadless()) {
            return null;
        }

        trace("Environment supports a display");

        if (!Desktop.isDesktopSupported()) {
            return null;
        }

        trace("Environment supports a desktop");

        try {
            // Disable JAB.
            // Needed to suppress error:
            // Exception in thread "main" java.awt.AWTError: Assistive Technology not found: com.sun.java.accessibility.AccessBridge
            System.setProperty("javax.accessibility.assistive_technologies", "");
        } catch (SecurityException ex) {
            ex.printStackTrace();
        }

        try {
            var desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) {
                trace("Set file handler");
                Hello instance = new Hello();
                desktop.setOpenFileHandler(instance);
                return instance;
            }
        } catch (AWTError ex) {
            trace("Set file handler failed");
            ex.printStackTrace();
        }

        return null;
    }

    private static final String MSG = "jpackage test application";
    private static final int EXPECTED_NUM_OF_PARAMS = 3; // Starts at 1

    private static void trace(String msg) {
        System.out.println("hello: " + msg);
    }
}
