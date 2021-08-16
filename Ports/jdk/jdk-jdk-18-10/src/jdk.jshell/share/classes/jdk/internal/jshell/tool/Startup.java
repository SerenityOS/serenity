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

package jdk.internal.jshell.tool;

import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import static java.util.stream.Collectors.joining;
import static jdk.internal.jshell.tool.JShellTool.RECORD_SEPARATOR;
import static jdk.internal.jshell.tool.JShellTool.getResource;
import static jdk.internal.jshell.tool.JShellTool.readResource;
import static jdk.internal.jshell.tool.JShellTool.toPathResolvingUserHome;

/**
 * Processing start-up "script" information.  The startup may consist of several
 * entries, each of which may have been read from a user file or be a built-in
 * resource.  The startup may also be empty ("-none"); Which is stored as the
 * empty string different from unset (null).  Built-in resources come from
 * resource files.  Startup is stored as named elements rather than concatenated
 * text, for display purposes but also importantly so that when resources update
 * with new releases the built-in will update.
 * @author Robert Field
 */
class Startup {

    // Store one entry in the start-up list
    private static class StartupEntry {

        // is this a JShell built-in?
        private final boolean isBuiltIn;

        // the file or resource name
        private final String name;

        // the commands/snippets as text
        private final String content;

        // for files, the date/time read in -- makes clear it is a snapshot
        private final String timeStamp;

        StartupEntry(boolean isBuiltIn, String name, String content) {
            this(isBuiltIn, name, content, "");
        }

        StartupEntry(boolean isBuiltIn, String name, String content, String timeStamp) {
            this.isBuiltIn = isBuiltIn;
            this.name = name;
            this.content = content;
            this.timeStamp = timeStamp;
        }

        // string form to store in storage (e.g. Preferences)
        String storedForm() {
            return (isBuiltIn ? "*" : "-") + RECORD_SEPARATOR +
                    name + RECORD_SEPARATOR +
                    timeStamp + RECORD_SEPARATOR +
                    content + RECORD_SEPARATOR;
        }

        // the content
        @Override
        public String toString() {
            return content;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + (this.isBuiltIn ? 1 : 0);
            hash = 41 * hash + Objects.hashCode(this.name);
            if (!isBuiltIn) {
                hash = 41 * hash + Objects.hashCode(this.content);
            }
            return hash;
        }

        // built-ins match on name only.  Time stamp isn't considered
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof StartupEntry)) {
                return false;
            }
            StartupEntry sue = (StartupEntry) o;
            return isBuiltIn == sue.isBuiltIn &&
                     name.equals(sue.name) &&
                     (isBuiltIn || content.equals(sue.content));
        }
    }

    private static final String DEFAULT_STARTUP_NAME = "DEFAULT";

    // cached DEFAULT start-up
    private static Startup defaultStartup = null;

    // the list of entries
    private List<StartupEntry> entries;

    // the concatenated content of the list of entries
    private String content;

    // created only with factory methods (below)
    private Startup(List<StartupEntry> entries) {
        this.entries = entries;
        this.content = entries.stream()
                .map(sue -> sue.toString())
                .collect(joining());
    }

    private Startup(StartupEntry entry) {
        this(Collections.singletonList(entry));
    }

    // retrieve the content
    @Override
    public String toString() {
        return content;
    }

    @Override
    public int hashCode() {
        return 9  + Objects.hashCode(this.entries);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Startup)
                && entries.equals(((Startup) o).entries);
    }

    // are there no entries ("-none")?
    boolean isEmpty() {
        return entries.isEmpty();
    }

    // is this the "-default" setting -- one entry which is DEFAULT
    boolean isDefault() {
        if (entries.size() == 1) {
            StartupEntry sue = entries.get(0);
            if (sue.isBuiltIn && sue.name.equals(DEFAULT_STARTUP_NAME)) {
                return true;
            }
        }
        return false;
    }

    // string form to store in storage (e.g. Preferences)
    String storedForm() {
        return entries.stream()
                .map(sue -> sue.storedForm())
                .collect(joining());
    }

    // show commands to re-create
    String show(boolean isRetained) {
        String cmd = "/set start " + (isRetained ? "-retain " : "");
        if (isDefault()) {
            return cmd + "-default\n";
        } else if (isEmpty()) {
            return cmd + "-none\n";
        } else {
            return entries.stream()
                    .map(sue -> sue.name)
                    .collect(joining(" ", cmd, "\n"));
        }
    }

    // show corresponding contents for show()
    String showDetail() {
        if (isDefault() || isEmpty()) {
            return "";
        } else {
            return entries.stream()
                    .map(sue -> "---- " + sue.name
                            + (sue.timeStamp.isEmpty()
                                    ? ""
                                    : " @ " + sue.timeStamp)
                            + " ----\n" + sue.content)
                    .collect(joining());
        }
    }

    /**
     * Factory method: Unpack from stored form.
     *
     * @param storedForm the Startup in the form as stored on persistent
     * storage (e.g. Preferences)
     * @param mh handler for error messages
     * @return Startup, or default startup when error (message has been printed)
     */
    static Startup unpack(String storedForm, MessageHandler mh) {
        if (storedForm != null) {
            if (storedForm.isEmpty()) {
                return noStartup();
            }
            try {
                String[] all = storedForm.split(RECORD_SEPARATOR);
                if (all.length == 1) {
                    // legacy (content only)
                    return new Startup(new StartupEntry(false, "user.jsh", storedForm));
                } else if (all.length % 4 == 0) {
                    List<StartupEntry> e = new ArrayList<>(all.length / 4);
                    for (int i = 0; i < all.length; i += 4) {
                        final boolean isBuiltIn = switch (all[i]) {
                            case "*" -> true;
                            case "-" -> false;
                            default -> throw new IllegalArgumentException("Unexpected StartupEntry kind: " + all[i]);
                        };
                        String name = all[i + 1];
                        String timeStamp = all[i + 2];
                        String content = all[i + 3];
                        if (isBuiltIn) {
                            // update to current definition, use stored if removed/error
                            String resource = getResource(name);
                            if (resource != null) {
                                content = resource;
                            }
                        }
                        e.add(new StartupEntry(isBuiltIn, name, content, timeStamp));
                    }
                    return new Startup(e);
                } else {
                    throw new IllegalArgumentException("Unexpected StartupEntry entry count: " + all.length);
                }
            } catch (Exception ex) {
                mh.errormsg("jshell.err.corrupted.stored.startup", ex.getMessage());
            }
        }
        return defaultStartup(mh);
    }

    /**
     * Factory method: Read Startup from a list of external files or resources.
     *
     * @param fns list of file/resource names to access
     * @param context printable non-natural language context for errors
     * @param mh handler for error messages
     * @return files as Startup, or null when error (message has been printed)
     */
    static Startup fromFileList(List<String> fns, String context, MessageHandler mh) {
        List<StartupEntry> entries = fns.stream()
                .map(fn -> readFile(fn, context, mh))
                .toList();
        if (entries.stream().anyMatch(sue -> sue == null)) {
            return null;
        }
        return new Startup(entries);
    }

    /**
     * Read a external file or a resource.
     *
     * @param filename file/resource to access
     * @param context printable non-natural language context for errors
     * @param mh handler for error messages
     * @return file as startup entry, or null when error (message has been printed)
     */
    private static StartupEntry readFile(String filename, String context, MessageHandler mh) {
        if (filename != null) {
            try {
                byte[] encoded = Files.readAllBytes(toPathResolvingUserHome(filename));
                return new StartupEntry(false, filename, new String(encoded),
                        LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
            } catch (AccessDeniedException e) {
                mh.errormsg("jshell.err.file.not.accessible", context, filename, e.getMessage());
            } catch (NoSuchFileException e) {
                String resource = getResource(filename);
                if (resource != null) {
                    // Not found as file, but found as resource
                    return new StartupEntry(true, filename, resource);
                }
                mh.errormsg("jshell.err.file.not.found", context, filename);
            } catch (Exception e) {
                mh.errormsg("jshell.err.file.exception", context, filename, e);
            }
        } else {
            mh.errormsg("jshell.err.file.filename", context);
        }
        return null;

    }

    /**
     * Factory method: The empty Startup ("-none").
     *
     * @return the empty Startup
     */
    static Startup noStartup() {
        return new Startup(Collections.emptyList());
    }

    /**
     * Factory method: The default Startup ("-default.").
     *
     * @param mh handler for error messages
     * @return The default Startup, or empty startup when error (message has been printed)
     */
    static Startup defaultStartup(MessageHandler mh) {
        if (defaultStartup != null) {
            return defaultStartup;
        }
        try {
            String content = readResource(DEFAULT_STARTUP_NAME);
            return defaultStartup = new Startup(
                    new StartupEntry(true, DEFAULT_STARTUP_NAME, content));
        } catch (AccessDeniedException e) {
            mh.errormsg("jshell.err.file.not.accessible", "jshell", DEFAULT_STARTUP_NAME, e.getMessage());
        } catch (NoSuchFileException e) {
            mh.errormsg("jshell.err.file.not.found", "jshell", DEFAULT_STARTUP_NAME);
        } catch (Exception e) {
            mh.errormsg("jshell.err.file.exception", "jshell", DEFAULT_STARTUP_NAME, e);
        }
        return defaultStartup = noStartup();
    }

}
