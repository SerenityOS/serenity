/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.editor.external;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Wrapper for controlling an external editor.
 */
public class ExternalEditor {
    private final Consumer<String> errorHandler;
    private final Consumer<String> saveHandler;
    private final boolean wait;

    private final Runnable suspendInteractiveInput;
    private final Runnable resumeInteractiveInput;
    private final Runnable promptForNewLineToEndWait;

    private WatchService watcher;
    private Thread watchedThread;
    private Path dir;
    private Path tmpfile;

    /**
     * Launch an external editor.
     *
     * @param cmd the command to launch (with parameters)
     * @param initialText initial text in the editor buffer
     * @param errorHandler handler for error messages
     * @param saveHandler handler sent the buffer contents on save
     * @param suspendInteractiveInput a callback to suspend caller (shell) input
     * @param resumeInteractiveInput a callback to resume caller input
     * @param wait true, if editor process termination cannot be used to
     * determine when done
     * @param promptForNewLineToEndWait a callback to prompt for newline if
     * wait==true
     */
    public static void edit(String[] cmd, String initialText,
            Consumer<String> errorHandler,
            Consumer<String> saveHandler,
            Runnable suspendInteractiveInput,
            Runnable resumeInteractiveInput,
            boolean wait,
            Runnable promptForNewLineToEndWait) {
        ExternalEditor ed = new ExternalEditor(errorHandler, saveHandler, suspendInteractiveInput,
             resumeInteractiveInput, wait, promptForNewLineToEndWait);
        ed.edit(cmd, initialText);
    }

    ExternalEditor(Consumer<String> errorHandler,
            Consumer<String> saveHandler,
            Runnable suspendInteractiveInput,
            Runnable resumeInteractiveInput,
            boolean wait,
            Runnable promptForNewLineToEndWait) {
        this.errorHandler = errorHandler;
        this.saveHandler = saveHandler;
        this.wait = wait;
        this.suspendInteractiveInput = suspendInteractiveInput;
        this.resumeInteractiveInput = resumeInteractiveInput;
        this.promptForNewLineToEndWait = promptForNewLineToEndWait;
    }

    private void edit(String[] cmd, String initialText) {
        try {
            setupWatch(initialText);
            launch(cmd);
        } catch (IOException ex) {
            errorHandler.accept(ex.getMessage());
        } finally {
            deleteDirectory();
        }
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    private void setupWatch(String initialText) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.dir = Files.createTempDirectory("extedit");
        this.tmpfile = Files.createTempFile(dir, null, ".java");
        Files.write(tmpfile, initialText.getBytes(Charset.forName("UTF-8")));
        dir.register(watcher,
                ENTRY_CREATE,
                ENTRY_DELETE,
                ENTRY_MODIFY);
        watchedThread = new Thread(() -> {
            for (;;) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (ClosedWatchServiceException ex) {
                    // The watch service has been closed, we are done
                    break;
                } catch (InterruptedException ex) {
                    // tolerate an interrupt
                    continue;
                }

                if (!key.pollEvents().isEmpty()) {
                    saveFile();
                }

                boolean valid = key.reset();
                if (!valid) {
                    // The watch service has been closed, we are done
                    break;
                }
            }
        });
        watchedThread.start();
    }

    private void launch(String[] cmd) throws IOException {
        String[] params = Arrays.copyOf(cmd, cmd.length + 1);
        params[cmd.length] = tmpfile.toString();
        ProcessBuilder pb = new ProcessBuilder(params);
        pb = pb.inheritIO();

        try {
            suspendInteractiveInput.run();
            Process process = pb.start();
            // wait to exit edit mode in one of these ways...
            if (wait) {
                // -wait option -- ignore process exit, wait for carriage-return
                Scanner scanner = new Scanner(System.in);
                promptForNewLineToEndWait.run();
                scanner.nextLine();
            } else {
                // wait for process to exit
                process.waitFor();
            }
        } catch (IOException ex) {
            errorHandler.accept("process IO failure: " + ex.getMessage());
        } catch (InterruptedException ex) {
            errorHandler.accept("process interrupt: " + ex.getMessage());
        } finally {
            try {
                watcher.close();
                watchedThread.join(); //so that saveFile() is finished.
                saveFile();
            } catch (InterruptedException ex) {
                errorHandler.accept("process interrupt: " + ex.getMessage());
            } finally {
                resumeInteractiveInput.run();
            }
        }
    }

    private void saveFile() {
        try (Stream<String> lines = Files.lines(tmpfile)) {
            saveHandler.accept(lines.collect(Collectors.joining("\n", "", "\n")));
        } catch (IOException ex) {
            errorHandler.accept("Failure in read edit file: " + ex.getMessage());
        }
    }

    private void deleteDirectory() {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException fail)
                        throws IOException {
                    if (fail == null) {
                        Files.delete(directory);
                        return FileVisitResult.CONTINUE;
                    }
                    throw fail;
                }
            });
        } catch (IOException exc) {
            // ignore: The end-user will not want to see this, it is in a temp
            // directory so it will go away eventually, and tests verify that
            // the deletion is occurring.
        }
    }
}
