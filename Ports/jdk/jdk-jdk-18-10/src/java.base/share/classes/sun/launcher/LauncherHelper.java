/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.launcher;

/*
 *
 *  <p><b>This is NOT part of any API supported by Sun Microsystems.
 *  If you write code that depends on this, you do so at your own
 *  risk.  This code and its internal interfaces are subject to change
 *  or deletion without notice.</b>
 *
 */

/**
 * A utility package for the java(1), javaw(1) launchers.
 * The following are helper methods that the native launcher uses
 * to perform checks etc. using JNI, see src/share/bin/java.c
 */
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.module.ModuleDescriptor.Provides;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Locale.Category;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.internal.misc.VM;
import jdk.internal.module.ModuleBootstrap;
import jdk.internal.module.Modules;
import jdk.internal.platform.Container;
import jdk.internal.platform.Metrics;


public final class LauncherHelper {

    // No instantiation
    private LauncherHelper() {}

    // used to identify JavaFX applications
    private static final String JAVAFX_APPLICATION_MARKER =
            "JavaFX-Application-Class";
    private static final String JAVAFX_APPLICATION_CLASS_NAME =
            "javafx.application.Application";
    private static final String JAVAFX_FXHELPER_CLASS_NAME_SUFFIX =
            "sun.launcher.LauncherHelper$FXHelper";
    private static final String LAUNCHER_AGENT_CLASS = "Launcher-Agent-Class";
    private static final String MAIN_CLASS = "Main-Class";
    private static final String ADD_EXPORTS = "Add-Exports";
    private static final String ADD_OPENS = "Add-Opens";

    private static StringBuilder outBuf = new StringBuilder();

    private static final String INDENT = "    ";
    private static final String VM_SETTINGS     = "VM settings:";
    private static final String PROP_SETTINGS   = "Property settings:";
    private static final String LOCALE_SETTINGS = "Locale settings:";

    // sync with java.c and jdk.internal.misc.VM
    private static final String diagprop = "sun.java.launcher.diag";
    static final boolean trace = VM.getSavedProperty(diagprop) != null;

    private static final String defaultBundleName =
            "sun.launcher.resources.launcher";

    private static class ResourceBundleHolder {
        private static final ResourceBundle RB =
                ResourceBundle.getBundle(defaultBundleName);
    }
    private static PrintStream ostream;
    private static Class<?> appClass; // application class, for GUI/reporting purposes

    /*
     * A method called by the launcher to print out the standard settings,
     * by default -XshowSettings is equivalent to -XshowSettings:all,
     * Specific information may be gotten by using suboptions with possible
     * values vm, properties and locale.
     *
     * printToStderr: choose between stdout and stderr
     *
     * optionFlag: specifies which options to print default is all other
     *    possible values are vm, properties, locale.
     *
     * initialHeapSize: in bytes, as set by the launcher, a zero-value indicates
     *    this code should determine this value, using a suitable method or
     *    the line could be omitted.
     *
     * maxHeapSize: in bytes, as set by the launcher, a zero-value indicates
     *    this code should determine this value, using a suitable method.
     *
     * stackSize: in bytes, as set by the launcher, a zero-value indicates
     *    this code determine this value, using a suitable method or omit the
     *    line entirely.
     */
    @SuppressWarnings("fallthrough")
    static void showSettings(boolean printToStderr, String optionFlag,
            long initialHeapSize, long maxHeapSize, long stackSize) {

        initOutput(printToStderr);
        String opts[] = optionFlag.split(":");
        String optStr = (opts.length > 1 && opts[1] != null)
                ? opts[1].trim()
                : "all";
        switch (optStr) {
            case "vm":
                printVmSettings(initialHeapSize, maxHeapSize, stackSize);
                break;
            case "properties":
                printProperties();
                break;
            case "locale":
                printLocale();
                break;
            case "system":
                if (System.getProperty("os.name").contains("Linux")) {
                    printSystemMetrics();
                    break;
                }
            default:
                printVmSettings(initialHeapSize, maxHeapSize, stackSize);
                printProperties();
                printLocale();
                if (System.getProperty("os.name").contains("Linux")) {
                    printSystemMetrics();
                }
                break;
        }
    }

    /*
     * prints the main vm settings subopt/section
     */
    private static void printVmSettings(
            long initialHeapSize, long maxHeapSize,
            long stackSize) {

        ostream.println(VM_SETTINGS);
        if (stackSize != 0L) {
            ostream.println(INDENT + "Stack Size: " +
                    SizePrefix.scaleValue(stackSize));
        }
        if (initialHeapSize != 0L) {
             ostream.println(INDENT + "Min. Heap Size: " +
                    SizePrefix.scaleValue(initialHeapSize));
        }
        if (maxHeapSize != 0L) {
            ostream.println(INDENT + "Max. Heap Size: " +
                    SizePrefix.scaleValue(maxHeapSize));
        } else {
            ostream.println(INDENT + "Max. Heap Size (Estimated): "
                    + SizePrefix.scaleValue(Runtime.getRuntime().maxMemory()));
        }
        ostream.println(INDENT + "Using VM: "
                + System.getProperty("java.vm.name"));
        ostream.println();
    }

    /*
     * prints the properties subopt/section
     */
    private static void printProperties() {
        Properties p = System.getProperties();
        ostream.println(PROP_SETTINGS);
        List<String> sortedPropertyKeys = new ArrayList<>();
        sortedPropertyKeys.addAll(p.stringPropertyNames());
        Collections.sort(sortedPropertyKeys);
        for (String x : sortedPropertyKeys) {
            printPropertyValue(x, p.getProperty(x));
        }
        ostream.println();
    }

    private static boolean isPath(String key) {
        return key.endsWith(".dirs") || key.endsWith(".path");
    }

    private static void printPropertyValue(String key, String value) {
        ostream.print(INDENT + key + " = ");
        if (key.equals("line.separator")) {
            for (byte b : value.getBytes()) {
                switch (b) {
                    case 0xd:
                        ostream.print("\\r ");
                        break;
                    case 0xa:
                        ostream.print("\\n ");
                        break;
                    default:
                        // print any bizzare line separators in hex, but really
                        // shouldn't happen.
                        ostream.printf("0x%02X", b & 0xff);
                        break;
                }
            }
            ostream.println();
            return;
        }
        if (!isPath(key)) {
            ostream.println(value);
            return;
        }
        String[] values = value.split(System.getProperty("path.separator"));
        boolean first = true;
        for (String s : values) {
            if (first) { // first line treated specially
                ostream.println(s);
                first = false;
            } else { // following lines prefix with indents
                ostream.println(INDENT + INDENT + s);
            }
        }
    }

    /*
     * prints the locale subopt/section
     */
    private static void printLocale() {
        Locale locale = Locale.getDefault();
        ostream.println(LOCALE_SETTINGS);
        ostream.println(INDENT + "default locale = " +
                locale.getDisplayName());
        ostream.println(INDENT + "default display locale = " +
                Locale.getDefault(Category.DISPLAY).getDisplayName());
        ostream.println(INDENT + "default format locale = " +
                Locale.getDefault(Category.FORMAT).getDisplayName());
        printLocales();
        ostream.println();
    }

    private static void printLocales() {
        Locale[] tlocales = Locale.getAvailableLocales();
        final int len = tlocales == null ? 0 : tlocales.length;
        if (len < 1 ) {
            return;
        }
        // Locale does not implement Comparable so we convert it to String
        // and sort it for pretty printing.
        Set<String> sortedSet = new TreeSet<>();
        for (Locale l : tlocales) {
            sortedSet.add(l.toString());
        }

        ostream.print(INDENT + "available locales = ");
        Iterator<String> iter = sortedSet.iterator();
        final int last = len - 1;
        for (int i = 0 ; iter.hasNext() ; i++) {
            String s = iter.next();
            ostream.print(s);
            if (i != last) {
                ostream.print(", ");
            }
            // print columns of 8
            if ((i + 1) % 8 == 0) {
                ostream.println();
                ostream.print(INDENT + INDENT);
            }
        }
    }

    public static void printSystemMetrics() {
        Metrics c = Container.metrics();

        ostream.println("Operating System Metrics:");

        if (c == null) {
            ostream.println(INDENT + "No metrics available for this platform");
            return;
        }

        final long longRetvalNotSupported = -2;

        ostream.println(INDENT + "Provider: " + c.getProvider());
        ostream.println(INDENT + "Effective CPU Count: " + c.getEffectiveCpuCount());
        ostream.println(formatCpuVal(c.getCpuPeriod(), INDENT + "CPU Period: ", longRetvalNotSupported));
        ostream.println(formatCpuVal(c.getCpuQuota(), INDENT + "CPU Quota: ", longRetvalNotSupported));
        ostream.println(formatCpuVal(c.getCpuShares(), INDENT + "CPU Shares: ", longRetvalNotSupported));

        int cpus[] = c.getCpuSetCpus();
        if (cpus != null) {
            ostream.println(INDENT + "List of Processors, "
                    + cpus.length + " total: ");

            ostream.print(INDENT);
            for (int i = 0; i < cpus.length; i++) {
                ostream.print(cpus[i] + " ");
            }
            if (cpus.length > 0) {
                ostream.println("");
            }
        } else {
            ostream.println(INDENT + "List of Processors: N/A");
        }

        cpus = c.getEffectiveCpuSetCpus();
        if (cpus != null) {
            ostream.println(INDENT + "List of Effective Processors, "
                    + cpus.length + " total: ");

            ostream.print(INDENT);
            for (int i = 0; i < cpus.length; i++) {
                ostream.print(cpus[i] + " ");
            }
            if (cpus.length > 0) {
                ostream.println("");
            }
        } else {
            ostream.println(INDENT + "List of Effective Processors: N/A");
        }

        int mems[] = c.getCpuSetMems();
        if (mems != null) {
            ostream.println(INDENT + "List of Memory Nodes, "
                    + mems.length + " total: ");

            ostream.print(INDENT);
            for (int i = 0; i < mems.length; i++) {
                ostream.print(mems[i] + " ");
            }
            if (mems.length > 0) {
                ostream.println("");
            }
        } else {
            ostream.println(INDENT + "List of Memory Nodes: N/A");
        }

        mems = c.getEffectiveCpuSetMems();
        if (mems != null) {
            ostream.println(INDENT + "List of Available Memory Nodes, "
                    + mems.length + " total: ");

            ostream.print(INDENT);
            for (int i = 0; i < mems.length; i++) {
                ostream.print(mems[i] + " ");
            }
            if (mems.length > 0) {
                ostream.println("");
            }
        } else {
            ostream.println(INDENT + "List of Available Memory Nodes: N/A");
        }

        long limit = c.getMemoryLimit();
        ostream.println(formatLimitString(limit, INDENT + "Memory Limit: ", longRetvalNotSupported));

        limit = c.getMemorySoftLimit();
        ostream.println(formatLimitString(limit, INDENT + "Memory Soft Limit: ", longRetvalNotSupported));

        limit = c.getMemoryAndSwapLimit();
        ostream.println(formatLimitString(limit, INDENT + "Memory & Swap Limit: ", longRetvalNotSupported));

        limit = c.getPidsMax();
        ostream.println(formatLimitString(limit, INDENT + "Maximum Processes Limit: ",
                                          longRetvalNotSupported, false));
        ostream.println("");
    }

    private static String formatLimitString(long limit, String prefix, long unavailable) {
        return formatLimitString(limit, prefix, unavailable, true);
    }

    private static String formatLimitString(long limit, String prefix, long unavailable, boolean scale) {
        if (limit >= 0) {
            if (scale) {
                return prefix + SizePrefix.scaleValue(limit);
            } else {
                return prefix + limit;
            }
        } else if (limit == unavailable) {
            return prefix + "N/A";
        } else {
            return prefix + "Unlimited";
        }
    }

    private static String formatCpuVal(long cpuVal, String prefix, long unavailable) {
        if (cpuVal >= 0) {
            return prefix + cpuVal + "us";
        } else if (cpuVal == unavailable) {
            return prefix + "N/A";
        } else {
            return prefix + cpuVal;
        }
    }

    private enum SizePrefix {

        KILO(1024, "K"),
        MEGA(1024 * 1024, "M"),
        GIGA(1024 * 1024 * 1024, "G"),
        TERA(1024L * 1024L * 1024L * 1024L, "T");
        long size;
        String abbrev;

        SizePrefix(long size, String abbrev) {
            this.size = size;
            this.abbrev = abbrev;
        }

        private static String scale(long v, SizePrefix prefix) {
            return BigDecimal.valueOf(v).divide(BigDecimal.valueOf(prefix.size),
                    2, RoundingMode.HALF_EVEN).toPlainString() + prefix.abbrev;
        }
        /*
         * scale the incoming values to a human readable form, represented as
         * K, M, G and T, see java.c parse_size for the scaled values and
         * suffixes. The lowest possible scaled value is Kilo.
         */
        static String scaleValue(long v) {
            if (v < MEGA.size) {
                return scale(v, KILO);
            } else if (v < GIGA.size) {
                return scale(v, MEGA);
            } else if (v < TERA.size) {
                return scale(v, GIGA);
            } else {
                return scale(v, TERA);
            }
        }
    }

    /**
     * A private helper method to get a localized message and also
     * apply any arguments that we might pass.
     */
    private static String getLocalizedMessage(String key, Object... args) {
        String msg = ResourceBundleHolder.RB.getString(key);
        return (args != null) ? MessageFormat.format(msg, args) : msg;
    }

    /**
     * The java -help message is split into 3 parts, an invariant, followed
     * by a set of platform dependent variant messages, finally an invariant
     * set of lines.
     * This method initializes the help message for the first time, and also
     * assembles the invariant header part of the message.
     */
    static void initHelpMessage(String progname) {
        outBuf = outBuf.append(getLocalizedMessage("java.launcher.opt.header",
                (progname == null) ? "java" : progname ));
    }

    /**
     * Appends the vm selection messages to the header, already created.
     * initHelpSystem must already be called.
     */
    static void appendVmSelectMessage(String vm1, String vm2) {
        outBuf = outBuf.append(getLocalizedMessage("java.launcher.opt.vmselect",
                vm1, vm2));
    }

    /**
     * Appends the vm synoym message to the header, already created.
     * initHelpSystem must be called before using this method.
     */
    static void appendVmSynonymMessage(String vm1, String vm2) {
        outBuf = outBuf.append(getLocalizedMessage("java.launcher.opt.hotspot",
                vm1, vm2));
    }

    /**
     * Appends the last invariant part to the previously created messages,
     * and finishes up the printing to the desired output stream.
     * initHelpSystem must be called before using this method.
     */
    static void printHelpMessage(boolean printToStderr) {
        initOutput(printToStderr);
        outBuf = outBuf.append(getLocalizedMessage("java.launcher.opt.footer",
                File.pathSeparator));
        ostream.println(outBuf.toString());
    }

    /**
     * Prints the Xusage text to the desired output stream.
     */
    static void printXUsageMessage(boolean printToStderr) {
        initOutput(printToStderr);
        ostream.println(getLocalizedMessage("java.launcher.X.usage",
                File.pathSeparator));
        if (System.getProperty("os.name").contains("OS X")) {
            ostream.println(getLocalizedMessage("java.launcher.X.macosx.usage",
                        File.pathSeparator));
        }
    }

    static void initOutput(boolean printToStderr) {
        ostream =  (printToStderr) ? System.err : System.out;
    }

    static void initOutput(PrintStream ps) {
        ostream = ps;
    }

    static String getMainClassFromJar(String jarname) {
        String mainValue;
        try (JarFile jarFile = new JarFile(jarname)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                abort(null, "java.launcher.jar.error2", jarname);
            }
            Attributes mainAttrs = manifest.getMainAttributes();
            if (mainAttrs == null) {
                abort(null, "java.launcher.jar.error3", jarname);
            }

            // Main-Class
            mainValue = mainAttrs.getValue(MAIN_CLASS);
            if (mainValue == null) {
                abort(null, "java.launcher.jar.error3", jarname);
            }

            // Launcher-Agent-Class (only check for this when Main-Class present)
            String agentClass = mainAttrs.getValue(LAUNCHER_AGENT_CLASS);
            if (agentClass != null) {
                ModuleLayer.boot().findModule("java.instrument").ifPresent(m -> {
                    try {
                        String cn = "sun.instrument.InstrumentationImpl";
                        Class<?> clazz = Class.forName(cn, false, null);
                        Method loadAgent = clazz.getMethod("loadAgent", String.class);
                        loadAgent.invoke(null, jarname);
                    } catch (Throwable e) {
                        if (e instanceof InvocationTargetException) e = e.getCause();
                        abort(e, "java.launcher.jar.error4", jarname);
                    }
                });
            }

            // Add-Exports and Add-Opens
            String exports = mainAttrs.getValue(ADD_EXPORTS);
            if (exports != null) {
                addExportsOrOpens(exports, false);
            }
            String opens = mainAttrs.getValue(ADD_OPENS);
            if (opens != null) {
                addExportsOrOpens(opens, true);
            }

            /*
             * Hand off to FXHelper if it detects a JavaFX application
             * This must be done after ensuring a Main-Class entry
             * exists to enforce compliance with the jar specification
             */
            if (mainAttrs.containsKey(
                    new Attributes.Name(JAVAFX_APPLICATION_MARKER))) {
                FXHelper.setFXLaunchParameters(jarname, LM_JAR);
                return FXHelper.class.getName();
            }

            return mainValue.trim();
        } catch (IOException ioe) {
            abort(ioe, "java.launcher.jar.error1", jarname);
        }
        return null;
    }

    /**
     * Process the Add-Exports or Add-Opens value. The value is
     * {@code <module>/<package> ( <module>/<package>)*}.
     */
    static void addExportsOrOpens(String value, boolean open) {
        for (String moduleAndPackage : value.split(" ")) {
            String[] s = moduleAndPackage.trim().split("/");
            if (s.length == 2) {
                String mn = s[0];
                String pn = s[1];
                ModuleLayer.boot()
                    .findModule(mn)
                    .filter(m -> m.getDescriptor().packages().contains(pn))
                    .ifPresent(m -> {
                        if (open) {
                            Modules.addOpensToAllUnnamed(m, pn);
                        } else {
                            Modules.addExportsToAllUnnamed(m, pn);
                        }
                    });
            }
        }
    }

    // From src/share/bin/java.c:
    //   enum LaunchMode { LM_UNKNOWN = 0, LM_CLASS, LM_JAR, LM_MODULE, LM_SOURCE }

    private static final int LM_UNKNOWN = 0;
    private static final int LM_CLASS   = 1;
    private static final int LM_JAR     = 2;
    private static final int LM_MODULE  = 3;
    private static final int LM_SOURCE  = 4;

    static void abort(Throwable t, String msgKey, Object... args) {
        if (msgKey != null) {
            ostream.println(getLocalizedMessage(msgKey, args));
        }
        if (trace) {
            if (t != null) {
                t.printStackTrace();
            } else {
                Thread.dumpStack();
            }
        }
        System.exit(1);
    }

    /**
     * This method:
     * 1. Loads the main class from the module or class path
     * 2. Checks the public static void main method.
     * 3. If the main class extends FX Application then call on FXHelper to
     * perform the launch.
     *
     * @param printToStderr if set, all output will be routed to stderr
     * @param mode LaunchMode as determined by the arguments passed on the
     *             command line
     * @param what the module name[/class], JAR file, or the main class
     *             depending on the mode
     *
     * @return the application's main class
     */
    @SuppressWarnings("fallthrough")
    public static Class<?> checkAndLoadMain(boolean printToStderr,
                                            int mode,
                                            String what) {
        initOutput(printToStderr);

        Class<?> mainClass = null;
        switch (mode) {
            case LM_MODULE: case LM_SOURCE:
                mainClass = loadModuleMainClass(what);
                break;
            default:
                mainClass = loadMainClass(mode, what);
                break;
        }

        // record the real main class for UI purposes
        // neither method above can return null, they will abort()
        appClass = mainClass;

        /*
         * Check if FXHelper can launch it using the FX launcher. In an FX app,
         * the main class may or may not have a main method, so do this before
         * validating the main class.
         */
        if (JAVAFX_FXHELPER_CLASS_NAME_SUFFIX.equals(mainClass.getName()) ||
            doesExtendFXApplication(mainClass)) {
            // Will abort() if there are problems with FX runtime
            FXHelper.setFXLaunchParameters(what, mode);
            mainClass = FXHelper.class;
        }

        validateMainClass(mainClass);
        return mainClass;
    }

    /**
     * Returns the main class for a module. The query is either a module name
     * or module-name/main-class. For the former then the module's main class
     * is obtained from the module descriptor (MainClass attribute).
     */
    private static Class<?> loadModuleMainClass(String what) {
        int i = what.indexOf('/');
        String mainModule;
        String mainClass;
        if (i == -1) {
            mainModule = what;
            mainClass = null;
        } else {
            mainModule = what.substring(0, i);
            mainClass = what.substring(i+1);
        }

        // main module is in the boot layer
        ModuleLayer layer = ModuleLayer.boot();
        Optional<Module> om = layer.findModule(mainModule);
        if (!om.isPresent()) {
            // should not happen
            throw new InternalError("Module " + mainModule + " not in boot Layer");
        }
        Module m = om.get();

        // get main class
        if (mainClass == null) {
            Optional<String> omc = m.getDescriptor().mainClass();
            if (!omc.isPresent()) {
                abort(null, "java.launcher.module.error1", mainModule);
            }
            mainClass = omc.get();
        }

        // load the class from the module
        Class<?> c = null;
        try {
            c = Class.forName(m, mainClass);
            if (c == null && System.getProperty("os.name", "").contains("OS X")
                    && Normalizer.isNormalized(mainClass, Normalizer.Form.NFD)) {

                String cn = Normalizer.normalize(mainClass, Normalizer.Form.NFC);
                c = Class.forName(m, cn);
            }
        } catch (LinkageError le) {
            abort(null, "java.launcher.module.error3", mainClass, m.getName(),
                    le.getClass().getName() + ": " + le.getLocalizedMessage());
        }
        if (c == null) {
            abort(null, "java.launcher.module.error2", mainClass, mainModule);
        }

        System.setProperty("jdk.module.main.class", c.getName());
        return c;
    }

    /**
     * Loads the main class from the class path (LM_CLASS or LM_JAR).
     */
    private static Class<?> loadMainClass(int mode, String what) {
        // get the class name
        String cn;
        switch (mode) {
            case LM_CLASS:
                cn = what;
                break;
            case LM_JAR:
                cn = getMainClassFromJar(what);
                break;
            default:
                // should never happen
                throw new InternalError("" + mode + ": Unknown launch mode");
        }

        // load the main class
        cn = cn.replace('/', '.');
        Class<?> mainClass = null;
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        try {
            try {
                mainClass = Class.forName(cn, false, scl);
            } catch (NoClassDefFoundError | ClassNotFoundException cnfe) {
                if (System.getProperty("os.name", "").contains("OS X")
                        && Normalizer.isNormalized(cn, Normalizer.Form.NFD)) {
                    try {
                        // On Mac OS X since all names with diacritical marks are
                        // given as decomposed it is possible that main class name
                        // comes incorrectly from the command line and we have
                        // to re-compose it
                        String ncn = Normalizer.normalize(cn, Normalizer.Form.NFC);
                        mainClass = Class.forName(ncn, false, scl);
                    } catch (NoClassDefFoundError | ClassNotFoundException cnfe1) {
                        abort(cnfe1, "java.launcher.cls.error1", cn,
                                cnfe1.getClass().getCanonicalName(), cnfe1.getMessage());
                    }
                } else {
                    abort(cnfe, "java.launcher.cls.error1", cn,
                            cnfe.getClass().getCanonicalName(), cnfe.getMessage());
                }
            }
        } catch (LinkageError le) {
            abort(le, "java.launcher.cls.error6", cn,
                    le.getClass().getName() + ": " + le.getLocalizedMessage());
        }
        return mainClass;
    }

    /*
     * Accessor method called by the launcher after getting the main class via
     * checkAndLoadMain(). The "application class" is the class that is finally
     * executed to start the application and in this case is used to report
     * the correct application name, typically for UI purposes.
     */
    public static Class<?> getApplicationClass() {
        return appClass;
    }

    /*
     * Check if the given class is a JavaFX Application class. This is done
     * in a way that does not cause the Application class to load or throw
     * ClassNotFoundException if the JavaFX runtime is not available.
     */
    private static boolean doesExtendFXApplication(Class<?> mainClass) {
        for (Class<?> sc = mainClass.getSuperclass(); sc != null;
                sc = sc.getSuperclass()) {
            if (sc.getName().equals(JAVAFX_APPLICATION_CLASS_NAME)) {
                return true;
            }
        }
        return false;
    }

    // Check the existence and signature of main and abort if incorrect
    static void validateMainClass(Class<?> mainClass) {
        Method mainMethod = null;
        try {
            mainMethod = mainClass.getMethod("main", String[].class);
        } catch (NoSuchMethodException nsme) {
            // invalid main or not FX application, abort with an error
            abort(null, "java.launcher.cls.error4", mainClass.getName(),
                  JAVAFX_APPLICATION_CLASS_NAME);
        } catch (Throwable e) {
            if (mainClass.getModule().isNamed()) {
                abort(e, "java.launcher.module.error5",
                      mainClass.getName(), mainClass.getModule().getName(),
                      e.getClass().getName(), e.getLocalizedMessage());
            } else {
                abort(e, "java.launcher.cls.error7", mainClass.getName(),
                      e.getClass().getName(), e.getLocalizedMessage());
            }
        }

        /*
         * getMethod (above) will choose the correct method, based
         * on its name and parameter type, however, we still have to
         * ensure that the method is static and returns a void.
         */
        int mod = mainMethod.getModifiers();
        if (!Modifier.isStatic(mod)) {
            abort(null, "java.launcher.cls.error2", "static",
                  mainMethod.getDeclaringClass().getName());
        }
        if (mainMethod.getReturnType() != java.lang.Void.TYPE) {
            abort(null, "java.launcher.cls.error3",
                  mainMethod.getDeclaringClass().getName());
        }
    }

    private static final String encprop = "sun.jnu.encoding";
    private static String encoding = null;
    private static boolean isCharsetSupported = false;

    /*
     * converts a c or a byte array to a platform specific string,
     * previously implemented as a native method in the launcher.
     */
    static String makePlatformString(boolean printToStderr, byte[] inArray) {
        initOutput(printToStderr);
        if (encoding == null) {
            encoding = System.getProperty(encprop);
            isCharsetSupported = Charset.isSupported(encoding);
        }
        try {
            String out = isCharsetSupported
                    ? new String(inArray, encoding)
                    : new String(inArray);
            return out;
        } catch (UnsupportedEncodingException uee) {
            abort(uee, null);
        }
        return null; // keep the compiler happy
    }

    static String[] expandArgs(String[] argArray) {
        List<StdArg> aList = new ArrayList<>();
        for (String x : argArray) {
            aList.add(new StdArg(x));
        }
        return expandArgs(aList);
    }

    static String[] expandArgs(List<StdArg> argList) {
        ArrayList<String> out = new ArrayList<>();
        if (trace) {
            System.err.println("Incoming arguments:");
        }
        for (StdArg a : argList) {
            if (trace) {
                System.err.println(a);
            }
            if (a.needsExpansion) {
                File x = new File(a.arg);
                File parent = x.getParentFile();
                String glob = x.getName();
                if (parent == null) {
                    parent = new File(".");
                }
                try (DirectoryStream<Path> dstream =
                        Files.newDirectoryStream(parent.toPath(), glob)) {
                    int entries = 0;
                    for (Path p : dstream) {
                        out.add(p.normalize().toString());
                        entries++;
                    }
                    if (entries == 0) {
                        out.add(a.arg);
                    }
                } catch (Exception e) {
                    out.add(a.arg);
                    if (trace) {
                        System.err.println("Warning: passing argument as-is " + a);
                        System.err.print(e);
                    }
                }
            } else {
                out.add(a.arg);
            }
        }
        String[] oarray = new String[out.size()];
        out.toArray(oarray);

        if (trace) {
            System.err.println("Expanded arguments:");
            for (String x : oarray) {
                System.err.println(x);
            }
        }
        return oarray;
    }

    /* duplicate of the native StdArg struct */
    private static class StdArg {
        final String arg;
        final boolean needsExpansion;
        StdArg(String arg, boolean expand) {
            this.arg = arg;
            this.needsExpansion = expand;
        }
        // protocol: first char indicates whether expansion is required
        // 'T' = true ; needs expansion
        // 'F' = false; needs no expansion
        StdArg(String in) {
            this.arg = in.substring(1);
            needsExpansion = in.charAt(0) == 'T';
        }
        public String toString() {
            return "StdArg{" + "arg=" + arg + ", needsExpansion=" + needsExpansion + '}';
        }
    }

    static final class FXHelper {

        private static final String JAVAFX_GRAPHICS_MODULE_NAME =
                "javafx.graphics";

        private static final String JAVAFX_LAUNCHER_CLASS_NAME =
                "com.sun.javafx.application.LauncherImpl";

        /*
         * The launch method used to invoke the JavaFX launcher. These must
         * match the strings used in the launchApplication method.
         *
         * Command line                 JavaFX-App-Class  Launch mode  FX Launch mode
         * java -cp fxapp.jar FXClass   N/A               LM_CLASS     "LM_CLASS"
         * java -cp somedir FXClass     N/A               LM_CLASS     "LM_CLASS"
         * java -jar fxapp.jar          Present           LM_JAR       "LM_JAR"
         * java -jar fxapp.jar          Not Present       LM_JAR       "LM_JAR"
         * java -m module/class [1]     N/A               LM_MODULE    "LM_MODULE"
         * java -m module               N/A               LM_MODULE    "LM_MODULE"
         *
         * [1] - JavaFX-Application-Class is ignored when modular args are used, even
         * if present in a modular jar
         */
        private static final String JAVAFX_LAUNCH_MODE_CLASS = "LM_CLASS";
        private static final String JAVAFX_LAUNCH_MODE_JAR = "LM_JAR";
        private static final String JAVAFX_LAUNCH_MODE_MODULE = "LM_MODULE";

        /*
         * FX application launcher and launch method, so we can launch
         * applications with no main method.
         */
        private static String fxLaunchName = null;
        private static String fxLaunchMode = null;

        private static Class<?> fxLauncherClass    = null;
        private static Method   fxLauncherMethod   = null;

        /*
         * Set the launch params according to what was passed to LauncherHelper
         * so we can use the same launch mode for FX. Abort if there is any
         * issue with loading the FX runtime or with the launcher method.
         */
        private static void setFXLaunchParameters(String what, int mode) {

            // find the module with the FX launcher
            Optional<Module> om = ModuleLayer.boot().findModule(JAVAFX_GRAPHICS_MODULE_NAME);
            if (!om.isPresent()) {
                abort(null, "java.launcher.cls.error5");
            }

            // load the FX launcher class
            fxLauncherClass = Class.forName(om.get(), JAVAFX_LAUNCHER_CLASS_NAME);
            if (fxLauncherClass == null) {
                abort(null, "java.launcher.cls.error5");
            }

            try {
                /*
                 * signature must be:
                 * public static void launchApplication(String launchName,
                 *     String launchMode, String[] args);
                 */
                fxLauncherMethod = fxLauncherClass.getMethod("launchApplication",
                        String.class, String.class, String[].class);

                // verify launcher signature as we do when validating the main method
                int mod = fxLauncherMethod.getModifiers();
                if (!Modifier.isStatic(mod)) {
                    abort(null, "java.launcher.javafx.error1");
                }
                if (fxLauncherMethod.getReturnType() != java.lang.Void.TYPE) {
                    abort(null, "java.launcher.javafx.error1");
                }
            } catch (NoSuchMethodException ex) {
                abort(ex, "java.launcher.cls.error5", ex);
            }

            fxLaunchName = what;
            switch (mode) {
                case LM_CLASS:
                    fxLaunchMode = JAVAFX_LAUNCH_MODE_CLASS;
                    break;
                case LM_JAR:
                    fxLaunchMode = JAVAFX_LAUNCH_MODE_JAR;
                    break;
                case LM_MODULE:
                    fxLaunchMode = JAVAFX_LAUNCH_MODE_MODULE;
                    break;
                default:
                    // should not have gotten this far...
                    throw new InternalError(mode + ": Unknown launch mode");
            }
        }

        public static void main(String... args) throws Exception {
            if (fxLauncherMethod == null
                    || fxLaunchMode == null
                    || fxLaunchName == null) {
                throw new RuntimeException("Invalid JavaFX launch parameters");
            }
            // launch appClass via fxLauncherMethod
            fxLauncherMethod.invoke(null,
                    new Object[] {fxLaunchName, fxLaunchMode, args});
        }
    }

    /**
     * Called by the launcher to list the observable modules.
     */
    static void listModules() {
        initOutput(System.out);

        ModuleBootstrap.limitedFinder().findAll().stream()
            .sorted(new JrtFirstComparator())
            .forEach(LauncherHelper::showModule);
    }

    /**
     * Called by the launcher to show the resolved modules
     */
    static void showResolvedModules() {
        initOutput(System.out);

        ModuleLayer bootLayer = ModuleLayer.boot();
        Configuration cf = bootLayer.configuration();

        cf.modules().stream()
            .map(ResolvedModule::reference)
            .sorted(new JrtFirstComparator())
            .forEach(LauncherHelper::showModule);
    }

    /**
     * Called by the launcher to describe a module
     */
    static void describeModule(String moduleName) {
        initOutput(System.out);

        ModuleFinder finder = ModuleBootstrap.limitedFinder();
        ModuleReference mref = finder.find(moduleName).orElse(null);
        if (mref == null) {
            abort(null, "java.launcher.module.error4", moduleName);
        }
        ModuleDescriptor md = mref.descriptor();

        // one-line summary
        showModule(mref);

        // unqualified exports (sorted by package)
        md.exports().stream()
            .filter(e -> !e.isQualified())
            .sorted(Comparator.comparing(Exports::source))
            .map(e -> Stream.concat(Stream.of(e.source()),
                                    toStringStream(e.modifiers()))
                    .collect(Collectors.joining(" ")))
            .forEach(sourceAndMods -> ostream.format("exports %s%n", sourceAndMods));

        // dependences
        for (Requires r : md.requires()) {
            String nameAndMods = Stream.concat(Stream.of(r.name()),
                                               toStringStream(r.modifiers()))
                    .collect(Collectors.joining(" "));
            ostream.format("requires %s", nameAndMods);
            finder.find(r.name())
                .map(ModuleReference::descriptor)
                .filter(ModuleDescriptor::isAutomatic)
                .ifPresent(any -> ostream.print(" automatic"));
            ostream.println();
        }

        // service use and provides
        for (String s : md.uses()) {
            ostream.format("uses %s%n", s);
        }
        for (Provides ps : md.provides()) {
            String names = ps.providers().stream().collect(Collectors.joining(" "));
            ostream.format("provides %s with %s%n", ps.service(), names);

        }

        // qualified exports
        for (Exports e : md.exports()) {
            if (e.isQualified()) {
                String who = e.targets().stream().collect(Collectors.joining(" "));
                ostream.format("qualified exports %s to %s%n", e.source(), who);
            }
        }

        // open packages
        for (Opens opens: md.opens()) {
            if (opens.isQualified())
                ostream.print("qualified ");
            String sourceAndMods = Stream.concat(Stream.of(opens.source()),
                                                 toStringStream(opens.modifiers()))
                    .collect(Collectors.joining(" "));
            ostream.format("opens %s", sourceAndMods);
            if (opens.isQualified()) {
                String who = opens.targets().stream().collect(Collectors.joining(" "));
                ostream.format(" to %s", who);
            }
            ostream.println();
        }

        // non-exported/non-open packages
        Set<String> concealed = new TreeSet<>(md.packages());
        md.exports().stream().map(Exports::source).forEach(concealed::remove);
        md.opens().stream().map(Opens::source).forEach(concealed::remove);
        concealed.forEach(p -> ostream.format("contains %s%n", p));
    }

    /**
     * Prints a single line with the module name, version and modifiers
     */
    private static void showModule(ModuleReference mref) {
        ModuleDescriptor md = mref.descriptor();
        ostream.print(md.toNameAndVersion());
        mref.location()
                .filter(uri -> !isJrt(uri))
                .ifPresent(uri -> ostream.format(" %s", uri));
        if (md.isOpen())
            ostream.print(" open");
        if (md.isAutomatic())
            ostream.print(" automatic");
        ostream.println();
    }

    /**
     * A ModuleReference comparator that considers modules in the run-time
     * image to be less than modules than not in the run-time image.
     */
    private static class JrtFirstComparator implements Comparator<ModuleReference> {
        private final Comparator<ModuleReference> real;

        JrtFirstComparator() {
            this.real = Comparator.comparing(ModuleReference::descriptor);
        }

        @Override
        public int compare(ModuleReference a, ModuleReference b) {
            if (isJrt(a)) {
                return isJrt(b) ? real.compare(a, b) : -1;
            } else {
                return isJrt(b) ? 1 : real.compare(a, b);
            }
        }
    }

    private static <T> Stream<String> toStringStream(Set<T> s) {
        return s.stream().map(e -> e.toString().toLowerCase());
    }

    private static boolean isJrt(ModuleReference mref) {
        return isJrt(mref.location().orElse(null));
    }

    private static boolean isJrt(URI uri) {
        return (uri != null && uri.getScheme().equalsIgnoreCase("jrt"));
    }

}
