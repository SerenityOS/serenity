/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.script.shell;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.script.*;

/**
 * This is the main class for Java script shell.
 */
public class Main {
    /**
     * main entry point to the command line tool
     * @param args command line argument array
     */
    public static void main(String[] args) {
        // parse command line options
        String[] scriptArgs = processOptions(args);

        // process each script command
        for (Command cmd : scripts) {
            cmd.run(scriptArgs);
        }

        System.exit(EXIT_SUCCESS);
    }

    // Each -e or -f or interactive mode is represented
    // by an instance of Command.
    private static interface Command {
        public void run(String[] arguments);
    }

    /**
     * Parses and processes command line options.
     * @param args command line argument array
     */
    private static String[] processOptions(String[] args) {
        // current scripting language selected
        String currentLanguage = DEFAULT_LANGUAGE;
        // current script file encoding selected
        String currentEncoding = null;

        // check for -classpath or -cp first
        checkClassPath(args);

        // have we seen -e or -f ?
        boolean seenScript = false;
        // have we seen -f - already?
        boolean seenStdin = false;
        for (int i=0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-classpath") ||
                    arg.equals("-cp")) {
                // handled already, just continue
                i++;
                continue;
            }

            // collect non-option arguments and pass these as script arguments
            if (!arg.startsWith("-")) {
                int numScriptArgs;
                int startScriptArg;
                if (seenScript) {
                    // if we have seen -e or -f already all non-option arguments
                    // are passed as script arguments
                    numScriptArgs = args.length - i;
                    startScriptArg = i;
                } else {
                    // if we have not seen -e or -f, first non-option argument
                    // is treated as script file name and rest of the non-option
                    // arguments are passed to script as script arguments
                    numScriptArgs = args.length - i - 1;
                    startScriptArg = i + 1;
                    ScriptEngine se = getScriptEngine(currentLanguage);
                    addFileSource(se, args[i], currentEncoding);
                }
                // collect script arguments and return to main
                String[] result = new String[numScriptArgs];
                System.arraycopy(args, startScriptArg, result, 0, numScriptArgs);
                return result;
            }

            if (arg.startsWith("-D")) {
                String value = arg.substring(2);
                int eq = value.indexOf('=');
                if (eq != -1) {
                    System.setProperty(value.substring(0, eq),
                            value.substring(eq + 1));
                } else {
                    if (!value.isEmpty()) {
                        System.setProperty(value, "");
                    } else {
                        // do not allow empty property name
                        usage(EXIT_CMD_NO_PROPNAME);
                    }
                }
                continue;
            } else if (arg.equals("-?") ||
                       arg.equals("-h") ||
                       arg.equals("--help") ||
                       // -help: legacy.
                       arg.equals("-help")) {
                usage(EXIT_SUCCESS);
            } else if (arg.equals("-e")) {
                seenScript = true;
                if (++i == args.length)
                    usage(EXIT_CMD_NO_SCRIPT);

                ScriptEngine se = getScriptEngine(currentLanguage);
                addStringSource(se, args[i]);
                continue;
            } else if (arg.equals("-encoding")) {
                if (++i == args.length)
                    usage(EXIT_CMD_NO_ENCODING);
                currentEncoding = args[i];
                continue;
            } else if (arg.equals("-f")) {
                seenScript = true;
                if (++i == args.length)
                    usage(EXIT_CMD_NO_FILE);
                ScriptEngine se = getScriptEngine(currentLanguage);
                if (args[i].equals("-")) {
                    if (seenStdin) {
                        usage(EXIT_MULTIPLE_STDIN);
                    } else {
                        seenStdin = true;
                    }
                    addInteractiveMode(se);
                } else {
                    addFileSource(se, args[i], currentEncoding);
                }
                continue;
            } else if (arg.equals("-l")) {
                if (++i == args.length)
                    usage(EXIT_CMD_NO_LANG);
                currentLanguage = args[i];
                continue;
            } else if (arg.equals("-q")) {
                listScriptEngines();
            }
            // some unknown option...
            usage(EXIT_UNKNOWN_OPTION);
        }

        if (! seenScript) {
            ScriptEngine se = getScriptEngine(currentLanguage);
            addInteractiveMode(se);
        }
        return new String[0];
    }

    /**
     * Adds interactive mode Command
     * @param se ScriptEngine to use in interactive mode.
     */
    private static void addInteractiveMode(final ScriptEngine se) {
        scripts.add(new Command() {
            public void run(String[] args) {
                setScriptArguments(se, args);
                processSource(se, "-", null);
            }
        });
    }

    /**
     * Adds script source file Command
     * @param se ScriptEngine used to evaluate the script file
     * @param fileName script file name
     * @param encoding script file encoding
     */
    private static void addFileSource(final ScriptEngine se,
            final String fileName,
            final String encoding) {
        scripts.add(new Command() {
            public void run(String[] args) {
                setScriptArguments(se, args);
                processSource(se, fileName, encoding);
            }
        });
    }

    /**
     * Adds script string source Command
     * @param se ScriptEngine to be used to evaluate the script string
     * @param source Script source string
     */
    private static void addStringSource(final ScriptEngine se,
            final String source) {
        scripts.add(new Command() {
            public void run(String[] args) {
                setScriptArguments(se, args);
                String oldFile = setScriptFilename(se, "<string>");
                try {
                    evaluateString(se, source);
                } finally {
                    setScriptFilename(se, oldFile);
                }
            }
        });
    }

    /**
     * Prints list of script engines available and exits.
     */
    private static void listScriptEngines() {
        List<ScriptEngineFactory> factories = engineManager.getEngineFactories();
        for (ScriptEngineFactory factory: factories) {
            getError().println(getMessage("engine.info",
                    new Object[] { factory.getLanguageName(),
                            factory.getLanguageVersion(),
                            factory.getEngineName(),
                            factory.getEngineVersion()
            }));
        }
        System.exit(EXIT_SUCCESS);
    }

    /**
     * Processes a given source file or standard input.
     * @param se ScriptEngine to be used to evaluate
     * @param filename file name, can be null
     * @param encoding script file encoding, can be null
     */
    private static void processSource(ScriptEngine se, String filename,
            String encoding) {
        if (filename.equals("-")) {
            BufferedReader in = new BufferedReader
                    (new InputStreamReader(getIn()));
            boolean hitEOF = false;
            String prompt = getPrompt(se);
            se.put(ScriptEngine.FILENAME, "<STDIN>");
            while (!hitEOF) {
                getError().print(prompt);
                String source = "";
                try {
                    source = in.readLine();
                } catch (IOException ioe) {
                    getError().println(ioe.toString());
                }
                if (source == null) {
                    hitEOF = true;
                    break;
                }
                Object res = evaluateString(se, source, false);
                if (res != null) {
                    res = res.toString();
                    if (res == null) {
                        res = "null";
                    }
                    getError().println(res);
                }
            }
        } else {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(filename);
            } catch (FileNotFoundException fnfe) {
                getError().println(getMessage("file.not.found",
                        new Object[] { filename }));
                        System.exit(EXIT_FILE_NOT_FOUND);
            }
            evaluateStream(se, fis, filename, encoding);
        }
    }

    /**
     * Evaluates given script source
     * @param se ScriptEngine to evaluate the string
     * @param script Script source string
     * @param exitOnError whether to exit the process on script error
     */
    private static Object evaluateString(ScriptEngine se,
            String script, boolean exitOnError) {
        try {
            return se.eval(script);
        } catch (ScriptException sexp) {
            getError().println(getMessage("string.script.error",
                    new Object[] { sexp.getMessage() }));
                    if (exitOnError)
                        System.exit(EXIT_SCRIPT_ERROR);
        } catch (Exception exp) {
            exp.printStackTrace(getError());
            if (exitOnError)
                System.exit(EXIT_SCRIPT_ERROR);
        }

        return null;
    }

    /**
     * Evaluate script string source and exit on script error
     * @param se ScriptEngine to evaluate the string
     * @param script Script source string
     */
    private static void evaluateString(ScriptEngine se, String script) {
        evaluateString(se, script, true);
    }

    /**
     * Evaluates script from given reader
     * @param se ScriptEngine to evaluate the string
     * @param reader Reader from which is script is read
     * @param name file name to report in error.
     */
    private static Object evaluateReader(ScriptEngine se,
            Reader reader, String name) {
        String oldFilename = setScriptFilename(se, name);
        try {
            return se.eval(reader);
        } catch (ScriptException sexp) {
            getError().println(getMessage("file.script.error",
                    new Object[] { name, sexp.getMessage() }));
                    System.exit(EXIT_SCRIPT_ERROR);
        } catch (Exception exp) {
            exp.printStackTrace(getError());
            System.exit(EXIT_SCRIPT_ERROR);
        } finally {
            setScriptFilename(se, oldFilename);
        }
        return null;
    }

    /**
     * Evaluates given input stream
     * @param se ScriptEngine to evaluate the string
     * @param is InputStream from which script is read
     * @param name file name to report in error
     */
    private static Object evaluateStream(ScriptEngine se,
            InputStream is, String name,
            String encoding) {
        BufferedReader reader = null;
        if (encoding != null) {
            try {
                reader = new BufferedReader(new InputStreamReader(is,
                        encoding));
            } catch (UnsupportedEncodingException uee) {
                getError().println(getMessage("encoding.unsupported",
                        new Object[] { encoding }));
                        System.exit(EXIT_NO_ENCODING_FOUND);
            }
        } else {
            reader = new BufferedReader(new InputStreamReader(is));
        }
        return evaluateReader(se, reader, name);
    }

    /**
     * Prints usage message and exits
     * @param exitCode process exit code
     */
    private static void usage(int exitCode) {
        getError().println(getMessage("main.usage",
                new Object[] { PROGRAM_NAME }));
                System.exit(exitCode);
    }

    /**
     * Gets prompt for interactive mode
     * @return prompt string to use
     */
    private static String getPrompt(ScriptEngine se) {
        List<String> names = se.getFactory().getNames();
        return names.get(0) + "> ";
    }

    /**
     * Get formatted, localized error message
     */
    private static String getMessage(String key, Object[] params) {
        return MessageFormat.format(msgRes.getString(key), params);
    }

    // input stream from where we will read
    private static InputStream getIn() {
        return System.in;
    }

    // stream to print error messages
    private static PrintStream getError() {
        return System.err;
    }

    // get current script engine
    private static ScriptEngine getScriptEngine(String lang) {
        ScriptEngine se = engines.get(lang);
        if (se == null) {
            se = engineManager.getEngineByName(lang);
            if (se == null) {
                getError().println(getMessage("engine.not.found",
                        new Object[] { lang }));
                        System.exit(EXIT_ENGINE_NOT_FOUND);
            }

            // initialize the engine
            initScriptEngine(se);
            // to avoid re-initialization of engine, store it in a map
            engines.put(lang, se);
        }
        return se;
    }

    // initialize a given script engine
    private static void initScriptEngine(ScriptEngine se) {
        // put engine global variable
        se.put("engine", se);

        // load init.<ext> file from resource
        List<String> exts = se.getFactory().getExtensions();
        InputStream sysIn = null;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (String ext : exts) {
            try {
                sysIn = Main.class.getModule().getResourceAsStream("com/sun/tools/script/shell/init." + ext);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            if (sysIn != null) break;
        }
        if (sysIn != null) {
            evaluateStream(se, sysIn, "<system-init>", null);
        }
    }

    /**
     * Checks for -classpath, -cp in command line args. Creates a ClassLoader
     * and sets it as Thread context loader for current thread.
     *
     * @param args command line argument array
     */
    private static void checkClassPath(String[] args) {
        String classPath = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-classpath") ||
                    args[i].equals("-cp")) {
                if (++i == args.length) {
                    // just -classpath or -cp with no value
                    usage(EXIT_CMD_NO_CLASSPATH);
                } else {
                    classPath = args[i];
                }
            }
        }

        if (classPath != null) {
            /* We create a class loader, configure it with specified
             * classpath values and set the same as context loader.
             * Note that ScriptEngineManager uses context loader to
             * load script engines. So, this ensures that user defined
             * script engines will be loaded. For classes referred
             * from scripts, Rhino engine uses thread context loader
             * but this is script engine dependent. We don't have
             * script engine independent solution anyway. Unless we
             * know the class loader used by a specific engine, we
             * can't configure correct loader.
             */
            URL[] urls = pathToURLs(classPath);
            URLClassLoader loader = new URLClassLoader(urls);
            Thread.currentThread().setContextClassLoader(loader);
        }

        // now initialize script engine manager. Note that this has to
        // be done after setting the context loader so that manager
        // will see script engines from user specified classpath
        engineManager = new ScriptEngineManager();
    }

    /**
     * Utility method for converting a search path string to an array
     * of directory and JAR file URLs.
     *
     * @param path the search path string
     * @return the resulting array of directory and JAR file URLs
     */
    private static URL[] pathToURLs(String path) {
        String[] components = path.split(File.pathSeparator);
        URL[] urls = new URL[components.length];
        int count = 0;
        while(count < components.length) {
            URL url = fileToURL(new File(components[count]));
            if (url != null) {
                urls[count++] = url;
            }
        }
        if (urls.length != count) {
            URL[] tmp = new URL[count];
            System.arraycopy(urls, 0, tmp, 0, count);
            urls = tmp;
        }
        return urls;
    }

    /**
     * Returns the directory or JAR file URL corresponding to the specified
     * local file name.
     *
     * @param file the File object
     * @return the resulting directory or JAR file URL, or null if unknown
     */
    private static URL fileToURL(File file) {
        String name;
        try {
            name = file.getCanonicalPath();
        } catch (IOException e) {
            name = file.getAbsolutePath();
        }
        name = name.replace(File.separatorChar, '/');
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        // If the file does not exist, then assume that it's a directory
        if (!file.isFile()) {
            name = name + "/";
        }
        try {
            return new URL("file", "", name);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("file");
        }
    }

    private static void setScriptArguments(ScriptEngine se, String[] args) {
        se.put("arguments", args);
        se.put(ScriptEngine.ARGV, args);
    }

    private static String setScriptFilename(ScriptEngine se, String name) {
        String oldName = (String) se.get(ScriptEngine.FILENAME);
        se.put(ScriptEngine.FILENAME, name);
        return oldName;
    }

    // exit codes
    private static final int EXIT_SUCCESS            = 0;
    private static final int EXIT_CMD_NO_CLASSPATH   = 1;
    private static final int EXIT_CMD_NO_FILE        = 2;
    private static final int EXIT_CMD_NO_SCRIPT      = 3;
    private static final int EXIT_CMD_NO_LANG        = 4;
    private static final int EXIT_CMD_NO_ENCODING    = 5;
    private static final int EXIT_CMD_NO_PROPNAME    = 6;
    private static final int EXIT_UNKNOWN_OPTION     = 7;
    private static final int EXIT_ENGINE_NOT_FOUND   = 8;
    private static final int EXIT_NO_ENCODING_FOUND  = 9;
    private static final int EXIT_SCRIPT_ERROR       = 10;
    private static final int EXIT_FILE_NOT_FOUND     = 11;
    private static final int EXIT_MULTIPLE_STDIN     = 12;

    // default scripting language
    private static final String DEFAULT_LANGUAGE = "js";
    // list of scripts to process
    private static List<Command> scripts;
    // the script engine manager
    private static ScriptEngineManager engineManager;
    // map of engines we loaded
    private static Map<String, ScriptEngine> engines;
    // error messages resource
    private static ResourceBundle msgRes;
    private static String BUNDLE_NAME = "com.sun.tools.script.shell.messages";
    private static String PROGRAM_NAME = "jrunscript";

    static {
        scripts = new ArrayList<Command>();
        engines = new HashMap<String, ScriptEngine>();
        msgRes = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault());
    }
}
