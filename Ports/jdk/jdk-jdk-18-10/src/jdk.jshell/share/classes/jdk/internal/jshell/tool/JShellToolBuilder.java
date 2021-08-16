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

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import jdk.jshell.tool.JavaShellToolBuilder;

/**
 * Builder for programmatically building the jshell tool.
 */
public class JShellToolBuilder implements JavaShellToolBuilder {

    private static final String PREFERENCES_NODE = "tool/JShell";
    private InputStream cmdIn = System.in;
    private InputStream userIn = null;
    private PrintStream cmdOut = System.out;
    private PrintStream console = System.out;
    private PrintStream userOut = System.out;
    private PrintStream cmdErr = System.err;
    private PrintStream userErr = System.err;
    private PersistentStorage prefs = null;
    private Map<String, String> vars = null;
    private Locale locale = Locale.getDefault();
    private boolean interactiveTerminal;
    private boolean capturePrompt = false;

    /**
     * Set the input channels.
     * Default, if not set, {@code in(System.in, null)}.
     *
     * @param cmdIn source of command input
     * @param userIn source of input for running user code, or {@code null} to
     * be extracted from cmdIn
     * @return the {@code JavaShellToolBuilder} instance
     */
    @Override
    public JavaShellToolBuilder in(InputStream cmdIn, InputStream userIn) {
        this.cmdIn = cmdIn;
        this.userIn = userIn;
        return this;
    }

    /**
     * Set the output channels. Same as {@code out(output, output, output)}.
     * Default, if not set, {@code out(System.out)}.
     *
     * @param output destination of command feedback, console interaction, and
     * user code output
     * @return the {@code JavaShellToolBuilder} instance
     */
    @Override
    public JavaShellToolBuilder out(PrintStream output) {
        this.cmdOut = output;
        this.console = output;
        this.userOut = output;
        return this;
    }

    /**
     * Set the output channels.
     * Default, if not set, {@code out(System.out, System.out, System.out)}.
     *
     * @param cmdOut destination of command feedback including error messages
     * for users
     * @param console destination of console interaction
     * @param userOut destination of user code output.  For example, user snippet
     * {@code System.out.println("Hello")} when executed {@code Hello} goes to
     * userOut.
     * @return the {@code JavaShellToolBuilder} instance
     */
    @Override
    public JavaShellToolBuilder out(PrintStream cmdOut, PrintStream console, PrintStream userOut) {
        this.cmdOut = cmdOut;
        this.console = console;
        this.userOut = userOut;
        return this;
    }

    /**
     * Set the error channels. Same as {@code err(error, error)}.
     * Default, if not set, {@code err(System.err)}.
     *
     * @param error destination of tool errors, and
     * user code errors
     * @return the {@code JavaShellToolBuilder} instance
     */
    @Override
    public JavaShellToolBuilder err(PrintStream error) {
        this.cmdErr = error;
        this.userErr = error;
        return this;
    }

    /**
     * Set the error channels.
     * Default, if not set, {@code err(System.err, System.err, System.err)}.
     *
     * @param cmdErr destination of tool start-up and fatal errors
     * @param userErr destination of user code error output.
     * For example, user snippet  {@code System.err.println("Oops")}
     * when executed {@code Oops} goes to userErr.
     * @return the {@code JavaShellToolBuilder} instance
     */
    @Override
    public JavaShellToolBuilder err(PrintStream cmdErr, PrintStream userErr) {
        this.cmdErr = cmdErr;
        this.userErr = userErr;
        return this;
    }

    /**
     * Set the storage mechanism for persistent information which includes
     * input history and retained settings. Default if not set is the
     * tool's standard persistence mechanism.
     *
     * @param prefs an instance of {@link java.util.prefs.Preferences} that
     * is used to retrieve and store persistent information
     * @return the {@code JavaShellToolBuilder} instance
     */
    @Override
    public JavaShellToolBuilder persistence(Preferences prefs) {
        this.prefs = new PreferencesStorage(prefs);
        return this;
    }

    /**
     * Set the storage mechanism for persistent information which includes
     * input history and retained settings.   Default if not set is the
     * tool's standard persistence mechanism.
     *
     * @param prefsMap  an instance of {@link java.util.Map} that
     * is used to retrieve and store persistent information
     * @return the {@code JavaShellToolBuilder} instance
     */
    @Override
    public JavaShellToolBuilder persistence(Map<String, String> prefsMap) {
        this.prefs = new MapStorage(prefsMap);
        return this;
    }

    /**
     * Set the source for environment variables.
     * Default, if not set, {@code env(System.getenv())}.
     *
     * @param vars the Map of environment variable names to values
     * @return the {@code JavaShellToolBuilder} instance
     */
    @Override
    public JavaShellToolBuilder env(Map<String, String> vars) {
        this.vars = vars;
        return this;
    }

    /**
     * Set the locale.
     * Default, if not set, {@code locale(Locale.getDefault())}.
     *
     * @param locale the locale
     * @return the {@code JavaShellToolBuilder} instance
     */
    @Override
    public JavaShellToolBuilder locale(Locale locale) {
        this.locale = locale;
        return this;
    }

    /**
     * Set if the special command capturing prompt override should be used.
     * Default, if not set, {@code promptCapture(false)}.
     *
     * @param capture if {@code true}, basic prompt is the {@code ENQ}
     * character and continuation prompt is the {@code ACK} character.
     * If false, prompts are as set with set-up or user {@code /set} commands.
     * @return the {@code JavaShellToolBuilder} instance
     */
    @Override
    public JavaShellToolBuilder promptCapture(boolean capture) {
        this.capturePrompt = capture;
        return this;
    }

    @Override
    public JavaShellToolBuilder interactiveTerminal(boolean terminal) {
        this.interactiveTerminal = terminal;
        return this;
    }

    /**
     * Create a tool instance for testing. Not in JavaShellToolBuilder.
     *
     * @return the tool instance
     */
    public JShellTool rawTool() {
        if (prefs == null) {
            prefs = new PreferencesStorage(Preferences.userRoot().node(PREFERENCES_NODE));
        }
        if (vars == null) {
            vars = System.getenv();
        }
        JShellTool sh = new JShellTool(cmdIn, cmdOut, cmdErr, console, userIn,
                userOut, userErr, prefs, vars, locale, interactiveTerminal);
        sh.testPrompt = capturePrompt;
        return sh;
    }

    /**
     * Run an instance of the Java shell tool as configured by the other methods
     * in this interface.  This call is not destructive, more than one call of
     * this method may be made from a configured builder. The  exit code from
     * the Java shell tool is ignored.
     *
     * @param arguments the command-line arguments (including options), if any
     * @throws Exception an unexpected fatal exception
     */
    @Override
    public void run(String... arguments) throws Exception {
        rawTool().start(arguments);
    }

    /**
     * Run an instance of the Java shell tool as configured by the other methods
     * in this interface.  This call is not destructive, more than one call of
     * this method may be made from a configured builder.
     *
     * @param arguments the command-line arguments (including options), if any
     * @throws Exception an unexpected fatal exception
     * @return the exit code
     */
    @Override
    public int start(String... arguments) throws Exception {
        return rawTool().start(arguments);
    }

    /**
     * Persistence stored in Preferences.
     */
    private static class PreferencesStorage implements PersistentStorage {

        final Preferences p;

        PreferencesStorage(Preferences p) {
            this.p = p;
        }

        @Override
        public void clear() {
            try {
                p.clear();
            } catch (BackingStoreException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        public String[] keys() {
            try {
                return p.keys();
            } catch (BackingStoreException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        public String get(String key) {
            return p.get(key, null);
        }

        @Override
        public void put(String key, String value) {
            p.put(key, value);
        }

        @Override
        public void remove(String key) {
            p.remove(key);
        }

        @Override
        public void flush() {
            try {
                p.flush();
            } catch (BackingStoreException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    /**
     * Persistence stored in a Map.
     */
    private static class MapStorage implements PersistentStorage {

        final Map<String, String> map;

        MapStorage(Map<String, String> map) {
            this.map = map;
        }

        @Override
        public void clear() {

            try {
                map.clear();
            } catch (UnsupportedOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        public String[] keys() {
            Set<String> ks = map.keySet();
            return ks.toArray(new String[ks.size()]);
        }

        @Override
        public String get(String key) {
            Objects.requireNonNull(key);
            return map.get(key);
        }

        @Override
        public void put(String key, String value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            map.put(key, value);
        }

        @Override
        public void remove(String key) {
            Objects.requireNonNull(key);
            map.remove(key);
        }

        @Override
        public void flush() {
            // no-op always up-to-date
        }
    }

}
