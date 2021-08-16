/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.internal.tool;

import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.internal.SecuritySupport;
import jdk.jfr.internal.SecuritySupport.SafePath;
import jdk.jfr.internal.jfc.JFC;
import jdk.jfr.internal.jfc.model.AbortException;
import jdk.jfr.internal.jfc.model.JFCModel;
import jdk.jfr.internal.jfc.model.SettingsLog;
import jdk.jfr.internal.jfc.model.UserInterface;
import jdk.jfr.internal.jfc.model.XmlInput;

final class Configure extends Command {
    private final List<SafePath> inputFiles = new ArrayList<>();

    @Override
    public List<String> getOptionSyntax() {
        List<String> list = new ArrayList<>();
        list.add("[--interactive] [--verbose]");
        list.add("[--input <files>] [--output <file>]");
        list.add("[option=value]* [event-setting=value]*");
        return list;
    }

    @Override
    protected String getTitle() {
        return "Configures a .jfc file";
    }

    @Override
    public String getName() {
        return "configure";
    }

    @Override
    public String getDescription() {
        return "Creates a custom .jfc-file for event configuration";
    }

    @Override
    public void displayOptionUsage(PrintStream stream) {
        stream.println("  --interactive           Interactive mode where the configuration is");
        stream.println("                          determined by a set of questions.");
        stream.println();
        stream.println("  --verbose               Displays the modified settings.");
        stream.println();
        stream.println("  --input <files>         A comma-separated list of .jfc files from which");
        stream.println("                          the new configuration is based. If no file is");
        stream.println("                          specified, the default file in the JDK is used");
        stream.println("                          (default.jfc). If 'none' is specified, the new");
        stream.println("                          configuration starts empty.");
        stream.println();
        stream.println("  --ouput <file>          The filename of the generated output file. If not");
        stream.println("                          specified, the filename custom.jfc will be used.");
        stream.println();
        stream.println("  option=value            The option value to modify. For available options,");
        stream.println("                          see listed input files below.");
        stream.println();
        stream.println("  event-setting=value     The event setting value to modify. Use the form:");
        stream.println("                          <event-name>#<setting-name>=<value>");
        stream.println("                          To add a new event setting, prefix the event name");
        stream.println("                          with '+'.");
        stream.println();
        stream.println("The whitespace delimiter can be omitted for timespan values, i.e. 20ms. For");
        stream.println("more information about the settings syntax, see Javadoc of the jdk.jfr package.");
        ensureInputFiles();
        for (SafePath path : inputFiles) {
            try {
                String name = path.toPath().getFileName().toString();
                displayParameters(stream, path, name);
            } catch (InvalidPathException | ParseException | IOException e) {
                stream.println("Unable read options for " + path + " " + e.getMessage());
            }
        }
        stream.println();
        stream.println("To run interactive configuration wizard:");
        stream.println();
        stream.println("  jfr configure --interactive");
        stream.println();
        stream.println("Example usage:");
        stream.println();
        stream.println("  jfr configure gc=high method-profiling=high --output high.jfc");
        stream.println();
        stream.println("  jfr configure jdk.JavaMonitorEnter#threshold=1ms --output locks.jfc");
        stream.println();
        stream.println("  jfr configure +HelloWorld#enabled=true +HelloWorld#stackTrace=true");
        stream.println();
        stream.println("  jfr configure --input default.jfc,third-party.jfc --output unified.jfc");
        stream.println();
        stream.println("  jfr configure --input none +Hello#enabled=true --output minimal.jfc");
    }

    private void displayParameters(PrintStream stream, SafePath path, String name) throws ParseException, IOException {
        JFCModel parameters = new JFCModel(path);
        stream.println();
        stream.println("Options for " + name + ":");
        stream.println();
        for (XmlInput input : parameters.getInputs()) {
            stream.println("  " + input.getOptionSyntax());
            stream.println();
        }
    }

    @Override
    public void execute(Deque<String> options) throws UserSyntaxException, UserDataException {
        boolean interactive = false;
        boolean log = false;
        SafePath output = null;
        Map<String, String> keyValues = new LinkedHashMap<>();
        int optionCount = options.size();
        while (optionCount > 0) {
            if (acceptSwitch(options, "--interactive")) {
                interactive = true;
            }
            if (acceptSwitch(options, "--verbose")) {
                log = true;
            }
            if (acceptOption(options, "--input")) {
                String value = options.pop();
                inputFiles.addAll(makeSafePathList(value));
            }
            if (acceptOption(options, "--output")) {
                if (output != null) {
                    throw new UserDataException("only one output file can be specified");
                }
                String value = options.pop();
                output = makeJFCPath(value);
            }
            if (acceptKeyValue(options)) {
                String value = options.pop();
                var keyValue = value.split("=");
                keyValues.put(keyValue[0], keyValue[1]);
            }
            if (optionCount == options.size()) {
                // No progress
                throw new UserSyntaxException("unknown option " + options.peek());
            }
            optionCount = options.size();
        }
        if (!interactive && output == null && keyValues.isEmpty()) {
            throw new UserSyntaxException("missing argument");
        }
        ensureInputFiles();
        configure(interactive, log, output, keyValues);
    }

    private boolean acceptKeyValue(Deque<String> options) {
        if (!options.isEmpty()) {
            String keyValue = options.peek();
            int index = keyValue.indexOf("=");
            return index > 0 && index < keyValue.length() - 1;
        }
        return false;
    }

    private void configure(boolean interactive, boolean log, SafePath output, Map<String, String> options) throws UserDataException {
        try {
            if (output == null) {
                output = new SafePath(Path.of("custom.jfc"));
            }
            UserInterface ui = new UserInterface();
            JFCModel model = new JFCModel(inputFiles);
            model.setLabel("Custom");
            if (log) {
                SettingsLog.enable();
            }
            for (var option : options.entrySet()) {
                model.configure(option.getKey(), option.getValue());
            }
            SettingsLog.flush();
            try {
                if (interactive) {
                    int q = model.getInputs().size() + 1;
                    ui.println("============== .jfc Configuration Wizard ============");
                    ui.println("This wizard will generate a JFR configuration file by");
                    ui.println("asking " + q + " questions. Press ENTER to use the default");
                    ui.println("value, or type Q to abort the wizard.");
                    model.configure(ui);
                    output = filename(ui, output);
                }
            } catch (AbortException e) {
                ui.println("Abort.");
                return;
            }
            model.saveToFile(output);
            ui.println("Configuration written successfully to:");
            ui.println(output.toPath().toAbsolutePath().toString());
        } catch (IllegalArgumentException iae) {
            throw new UserDataException(iae.getMessage());
        } catch (FileNotFoundException ffe) {
            throw new UserDataException("could not find file: " + ffe.getMessage());
        } catch (IOException ioe) {
            throw new UserDataException("i/o error: " + ioe.getMessage());
        } catch (ParseException pe) {
            pe.printStackTrace();
            throw new UserDataException("parsing error: " + pe.getMessage());
        }
    }

    private List<SafePath> makeSafePathList(String value) {
        List<SafePath> paths = new ArrayList<>();
        for (String name : value.split(",")) {
            paths.add(JFC.createSafePath(name));
        }
        return paths;
    }


    private void ensureInputFiles() throws InternalError {
        if (inputFiles.isEmpty()) {
            for (SafePath predefined : SecuritySupport.getPredefinedJFCFiles()) {
                if (predefined.toString().endsWith("default.jfc")) {
                    inputFiles.add(predefined);
                }
            }
        }
    }

    private static SafePath filename(UserInterface ui, SafePath file) throws AbortException {
        ui.println();
        ui.println("Filename: " + file + " (default)");
        while (true) {
            String line = ui.readLine();
            try {
                if (line.isBlank()) {
                    return file;
                }
                if (line.endsWith(".jfc")) {
                    return new SafePath(line);
                }
                ui.println("Filename must end with .jfc.");
            } catch (InvalidPathException ipe) {
                ui.println("Not a valid filename. " + ipe.getMessage());
            }
        }
    }

    private SafePath makeJFCPath(String file) throws UserDataException, UserSyntaxException {
        if (file.startsWith("--")) {
            throw new UserSyntaxException("missing file");
        }
        try {
            Path path = Path.of(file).toAbsolutePath();
            ensureFileExtension(path, ".jfc");
            return new SafePath(path);
        } catch (IOError ioe) {
            throw new UserDataException("i/o error reading file '" + file + "', " + ioe.getMessage());
        } catch (InvalidPathException ipe) {
            throw new UserDataException("invalid path '" + file + "'");
        }
    }
}
