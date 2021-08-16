/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.share.options;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.io.PrintStream;
import nsk.share.TestBug;
import nsk.share.log.LogSupport;

class OptionsSetup {
    private LogSupport log = new LogSupport();
    private boolean verbose = true;
    private Object test;
    private String[] args;
    private OptionHandler unknownOptionHandler;

    private int argIndex = 0;
    private Map<String, OptionDefinition> optionDefs = new LinkedHashMap<String, OptionDefinition>(); // Use LinkedHashMap to ensure order of options
    private List<OptionDefinition> unconfiguredOptionsList = new ArrayList<OptionDefinition>();
    private List<OptionDefinition> unconfiguredOptionList = new ArrayList<OptionDefinition>();
    private Map<String, Object> optionValues = new LinkedHashMap<String, Object>();

    public OptionsSetup(Object test, String[] args, OptionHandler unknownOptionHandler) {
        this.test = test;
        this.args = args;
        this.unknownOptionHandler = unknownOptionHandler;
        log.setDebugEnabled(verbose);
    }

    public void run() {
        searchAnnotations(test, null);
        while (argIndex < args.length) {
            process1Arg();
        }
        setDefaultValues();
        checkMandatoryOptions();
        if (unconfiguredOptionsList.size() > 0) {
            for (OptionDefinition optDef : unconfiguredOptionsList)
                log.info("Unconfigured option: " + optDef);
            throw new TestBug("Some options are unconfigured");
        }
    }

    private void checkMandatoryOptions() {
        for (Map.Entry<String, OptionDefinition> e : optionDefs.entrySet()) {
            String name = e.getKey();
            OptionDefinition optDef = e.getValue();
            if (optDef.getDefaultValue() == null && !optionValues.containsKey(name))
                throw new TestBug("Mandatory option is not specified: -" + name);
        }
    }

    private void setDefaultValues() {
        for (Iterator<OptionDefinition> it = unconfiguredOptionList.iterator(); it.hasNext(); ) {
            OptionDefinition optDef = it.next();
            String value = optDef.getDefaultValue();
            if (value == null)
                continue;
            setOptionValue(optDef, value);
            it.remove();
            if (unconfiguredOptionsList.contains(optDef))
                unconfiguredOptionsList.remove(optDef);
        }

        for (Iterator<OptionDefinition> it = unconfiguredOptionsList.iterator(); it.hasNext(); ) {
            OptionDefinition optDef = it.next();
            if (optionsAnnotation(optDef.getOwner(), optDef.getField(), null, optDef, true))
                it.remove();
        }
    }

    private void process1Arg() {
        String arg = args[argIndex++];
        //log.debug("Processing argument: " + arg);
        if (!arg.startsWith("-")) {
            processUnknownArg(arg);
            return;
        }
        String opt = arg.substring(1);
        String value = null;
        int i = opt.indexOf('=');
        if (i != -1) {
            value = opt.substring(i + 1);
            opt = opt.substring(0, i);
        }
        if (opt.equals("help")) {
            printHelp();
            throw new TestBug("-help was specified");
        }
        if (!optionDefs.containsKey(opt)) {
            if (value == null && argIndex < args.length)
                value = args[argIndex++];
            // We need to try to resolve default values of all unconfigured fields because one of them may potentially have this option
            setDefaultValues();
            if (!optionDefs.containsKey(opt)) {
                processUnknownOpt(opt, value);
                return;
            }
        }
        OptionDefinition optDef = optionDefs.get(opt);
        Field f = optDef.getField();
        // Handle boolean omitted value
        if (value == null && (argIndex >= args.length || args[argIndex].startsWith("-"))) {
            if (f.getType() == boolean.class || f.getType() == Boolean.class) {
                value = "true";
            }
        }
        if (value == null) {
            if (argIndex >= args.length)
                throw new TestBug("Missing value for option -" + opt);
            value = args[argIndex++];
        }
        setOptionValue(optDef, value);
        if (unconfiguredOptionList.contains(optDef)){
            unconfiguredOptionList.remove(optDef);
        }
    }

    private void setOptionValue(OptionDefinition optDef, String value) {
        Object ovalue = null;
        if (optDef.hasFactory()) {
            ovalue = optDef.getFactory().getObject(value);
        } else {
            ovalue = PrimitiveParser.parse(value, optDef.getField().getType());
        }
        optionValues.put(optDef.getName(), ovalue);
        try {
            Field f = optDef.getField();
            Object o = optDef.getOwner();
            f.set(o, ovalue);
            if (f.isAnnotationPresent(Options.class)) {
                if (!optionsAnnotation(o, f, optDef.getPrefix(), optDef, false))
                    throw new TestBug("Unexpected (bug in framework?): optionsAnnotation returned null: " + optDef);
                if (unconfiguredOptionsList.contains(optDef))
                    unconfiguredOptionsList.remove(optDef);
            }
        } catch (IllegalArgumentException e) {
            throw new TestBug("Exception setting field value for option " + optDef.getName(), e);
        } catch (IllegalAccessException e) {
            throw new TestBug("Exception setting field value for option " + optDef.getName(), e);
        }
    }

    private void processUnknownArg(String arg) {
        if (unknownOptionHandler != null)
            unknownOptionHandler.argument(arg);
        else
            throw new TestBug("Invalid argument: " + arg);
    }

    private void processUnknownOpt(String opt, String value) {
        if (unknownOptionHandler != null)
            unknownOptionHandler.option(opt, value);
        else
            throw new TestBug("Invalid option: '" + opt + "', value: '" + value + "'");
    }

    private void searchAnnotations(Object o, String prefix) {
        Class<?> cl0 = o.getClass();
        //log.debug("Looking for annotations for object " + o + ", class " + cl0);
        List<Class> classes = new LinkedList<Class>();
        while (cl0.getSuperclass() != null) {
            classes.add(0, cl0); // Add to the beginning to ensure the option order is from superclass to subclass
            cl0 = cl0.getSuperclass();
        }
        for (Class<?> cl : classes) {
            for (Field f : cl.getDeclaredFields()) {
                OptionDefinition optDef = null;
                if (f.isAnnotationPresent(Option.class)) {
                    optDef = optionAnnotation(o, f, prefix);
                    if (optDef != null) {
                        unconfiguredOptionList.add(optDef);
                    }
                }
                if (f.isAnnotationPresent(Options.class)) {
                    if (!optionsAnnotation(o, f, prefix, optDef, false)) {
                        if (!unconfiguredOptionsList.contains(optDef))
                            unconfiguredOptionsList.add(optDef);
                    }
                }
            }
        }
    }

    private boolean optionsAnnotation(Object o, Field f, String prefix, OptionDefinition optDef, boolean useDefault) {
        if (Modifier.isStatic(f.getModifiers()))
            throw new OptionError("@Options annotation is not allowed at static field", optDef);
        if (!Object.class.isAssignableFrom(f.getDeclaringClass()))
            throw new OptionError("@Options annotation is only allowed on object types", optDef);
        //log.debug("Processing @Options annotation: object " + o + ", field " + f + ", prefix " + prefix);
        Object v = null;
        try {
            f.setAccessible(true);
            v = f.get(o);
        } catch (IllegalAccessException e) {
            throw new OptionError("Exception getting value of field ", e, optDef);
        }
        if (v == null) {
            if (optDef == null)
                throw new OptionError("Value of field is null and no @Option annotation is present", optDef);
            if (!optDef.hasFactory())
                throw new OptionError("Value of field is null and no @Option annotation does not have factory", optDef);
            if (useDefault) {
                setOptionValue(optDef, optDef.getDefaultValue());
                try {
                    v = f.get(o);
                } catch (IllegalAccessException e) {
                    throw new OptionError("Exception getting value of field ", e, optDef);
                }
            }
            if (v == null) {
                // We cannot setup it right away, so it is stored until value is set
                return false;
            } else
                return true; // setOption Value already searched annotations
        }
        Options opts = f.getAnnotation(Options.class);
        String vprefix = opts.prefix();
        if (vprefix.equals(Options.defPrefix))
            vprefix = null;
        if (vprefix != null) {
            if (prefix != null)
                prefix = prefix + "." + vprefix;
            else
                prefix = vprefix;
        }
        searchAnnotations(v, prefix);
        return true;
    }

    private OptionDefinition optionAnnotation(Object o, Field f, String prefix) {
        //log.debug("Processing @Option annotation: object " + o + ", field " + f + ", prefix " + prefix);
        f.setAccessible(true);
        Option opt = f.getAnnotation(Option.class);
        String name = opt.name();
        if (name.equals(Option.defName))
            name = f.getName();  // option name defaults to field name
        if (prefix != null)
            name = prefix + "." + name;
        if (optionDefs.containsKey(name))
            throw new TestBug("Option is already defined: " + name);
        String defaultValue = opt.default_value();
        if (defaultValue.equals(Option.defDefaultValue))
            defaultValue = null; // default value defaults to null
        String description = opt.description();
        if (description.equals(Option.defDescription))
            description = null;
        if (description == null) {
            if (name.equals("log") || name.endsWith(".log")) {
                try {
                    f.set(o, log);
                } catch (IllegalAccessException e) {
                    throw new TestBug("Exception setting log field of " + o, e);
                }
                return null;
            } else
                throw new TestBug("@Option annotation should always have description set: " + name + ", field: " + f);
        }
        Class<? extends OptionObjectFactory> factory = opt.factory();
        //log.debug("Factory: " + factory);
        if (factory.equals(OptionObjectFactory.class))
            factory = null;
        OptionDefinition optDef = new OptionDefinition(
                prefix,
                name,
                description,
                defaultValue,
                factory,
                f,
                o
        );
        optionDefs.put(name, optDef);
        //log.debug("Added option definition: " + optDef);
        return optDef;
    }

    private void printHelp() {
        PrintStream out = System.out;
        out.println(" Supported options:");
        out.println("    -help");
        out.println("          Show this help screen");
        for (Map.Entry<String, OptionDefinition> entry : optionDefs.entrySet()) {
            String opt = entry.getKey();
            OptionDefinition optDef = entry.getValue();
            out.println("    -" + opt + " <" + optDef.getPlaceHolder() + ">");
            out.print("          " + optDef.getDescription() + " ");
            if (optDef.getDefaultValue() != null) {
                out.println("(default: " + optDef.getDefaultValue() + ")");
            } else {
                out.println("(mandatory)");
            }
            if (optDef.hasFactory()) {
                OptionObjectFactory factory = optDef.getFactory();
                for (String key : factory.getPossibleValues()) {
                    out.println("             " + key + ": " + factory.getParameterDescription(key));
                }
            }
        }
    }
}
