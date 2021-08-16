/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jpackage.internal;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Arguments
 *
 * This class encapsulates and processes the command line arguments,
 * in effect, implementing all the work of jpackage tool.
 *
 * The primary entry point, processArguments():
 * Processes and validates command line arguments, constructing DeployParams.
 * Validates the DeployParams, and generate the BundleParams.
 * Generates List of Bundlers from BundleParams valid for this platform.
 * Executes each Bundler in the list.
 */
public class Arguments {
    private static final ResourceBundle I18N = ResourceBundle.getBundle(
            "jdk.jpackage.internal.resources.MainResources");

    private static final String FA_EXTENSIONS = "extension";
    private static final String FA_CONTENT_TYPE = "mime-type";
    private static final String FA_DESCRIPTION = "description";
    private static final String FA_ICON = "icon";

    // Mac specific file association keys
    // String
    public static final String MAC_CFBUNDLETYPEROLE = "mac.CFBundleTypeRole";
    public static final String MAC_LSHANDLERRANK = "mac.LSHandlerRank";
    public static final String MAC_NSSTORETYPEKEY = "mac.NSPersistentStoreTypeKey";
    public static final String MAC_NSDOCUMENTCLASS = "mac.NSDocumentClass";
    // Boolean
    public static final String MAC_LSTYPEISPACKAGE = "mac.LSTypeIsPackage";
    public static final String MAC_LSDOCINPLACE = "mac.LSSupportsOpeningDocumentsInPlace";
    public static final String MAC_UIDOCBROWSER = "mac.UISupportsDocumentBrowser";
     // Array of strings
    public static final String MAC_NSEXPORTABLETYPES = "mac.NSExportableTypes";
    public static final String MAC_UTTYPECONFORMSTO = "mac.UTTypeConformsTo";

    // regexp for parsing args (for example, for additional launchers)
    private static Pattern pattern = Pattern.compile(
          "(?:(?:([\"'])(?:\\\\\\1|.)*?(?:\\1|$))|(?:\\\\[\"'\\s]|[^\\s]))++");

    private DeployParams deployParams = null;

    private int pos = 0;
    private List<String> argList = null;

    private List<CLIOptions> allOptions = null;

    private String input = null;
    private Path output = null;

    private boolean hasMainJar = false;
    private boolean hasMainClass = false;
    private boolean hasMainModule = false;
    public boolean userProvidedBuildRoot = false;

    private String buildRoot = null;
    private String mainJarPath = null;

    private boolean runtimeInstaller = false;

    private List<AddLauncherArguments> addLaunchers = null;

    private static final Map<String, CLIOptions> argIds = new HashMap<>();
    private static final Map<String, CLIOptions> argShortIds = new HashMap<>();

    static {
        // init maps for parsing arguments
        (EnumSet.allOf(CLIOptions.class)).forEach(option -> {
            argIds.put(option.getIdWithPrefix(), option);
            if (option.getShortIdWithPrefix() != null) {
                argShortIds.put(option.getShortIdWithPrefix(), option);
            }
        });
    }

    private static final InheritableThreadLocal<Arguments> instance =
            new InheritableThreadLocal<Arguments>();

    public Arguments(String[] args) {
        instance.set(this);

        argList = new ArrayList<String>(args.length);
        for (String arg : args) {
            argList.add(arg);
        }
        Log.verbose ("\njpackage argument list: \n" + argList + "\n");
        pos = 0;

        deployParams = new DeployParams();

        allOptions = new ArrayList<>();

        addLaunchers = new ArrayList<>();

        output = Paths.get("").toAbsolutePath();
        deployParams.setOutput(output);
    }

    // CLIOptions is public for DeployParamsTest
    public enum CLIOptions {
        PACKAGE_TYPE("type", "t", OptionCategories.PROPERTY, () -> {
            context().deployParams.setTargetFormat(popArg());
        }),

        INPUT ("input", "i", OptionCategories.PROPERTY, () -> {
            context().input = popArg();
            setOptionValue("input", context().input);
        }),

        OUTPUT ("dest", "d", OptionCategories.PROPERTY, () -> {
            context().output = Path.of(popArg());
            context().deployParams.setOutput(context().output);
        }),

        DESCRIPTION ("description", OptionCategories.PROPERTY),

        VENDOR ("vendor", OptionCategories.PROPERTY),

        APPCLASS ("main-class", OptionCategories.PROPERTY, () -> {
            context().hasMainClass = true;
            setOptionValue("main-class", popArg());
        }),

        NAME ("name", "n", OptionCategories.PROPERTY),

        VERBOSE ("verbose", OptionCategories.PROPERTY, () -> {
            setOptionValue("verbose", true);
            Log.setVerbose();
        }),

        RESOURCE_DIR("resource-dir",
                OptionCategories.PROPERTY, () -> {
            String resourceDir = popArg();
            setOptionValue("resource-dir", resourceDir);
        }),

        ARGUMENTS ("arguments", OptionCategories.PROPERTY, () -> {
            List<String> arguments = getArgumentList(popArg());
            setOptionValue("arguments", arguments);
        }),

        JLINK_OPTIONS ("jlink-options", OptionCategories.PROPERTY, () -> {
            List<String> options = getArgumentList(popArg());
            setOptionValue("jlink-options", options);
        }),

        ICON ("icon", OptionCategories.PROPERTY),

        COPYRIGHT ("copyright", OptionCategories.PROPERTY),

        LICENSE_FILE ("license-file", OptionCategories.PROPERTY),

        VERSION ("app-version", OptionCategories.PROPERTY),

        RELEASE ("linux-app-release", OptionCategories.PROPERTY),

        ABOUT_URL ("about-url", OptionCategories.PROPERTY),

        JAVA_OPTIONS ("java-options", OptionCategories.PROPERTY, () -> {
            List<String> args = getArgumentList(popArg());
            args.forEach(a -> setOptionValue("java-options", a));
        }),

        FILE_ASSOCIATIONS ("file-associations",
                OptionCategories.PROPERTY, () -> {
            Map<String, ? super Object> args = new HashMap<>();

            // load .properties file
            Map<String, String> initialMap = getPropertiesFromFile(popArg());

            putUnlessNull(args, StandardBundlerParam.FA_EXTENSIONS.getID(),
                    initialMap.get(FA_EXTENSIONS));

            putUnlessNull(args, StandardBundlerParam.FA_CONTENT_TYPE.getID(),
                    initialMap.get(FA_CONTENT_TYPE));

            putUnlessNull(args, StandardBundlerParam.FA_DESCRIPTION.getID(),
                    initialMap.get(FA_DESCRIPTION));

            putUnlessNull(args, StandardBundlerParam.FA_ICON.getID(),
                    initialMap.get(FA_ICON));

            // Mac extended file association arguments
            putUnlessNull(args, MAC_CFBUNDLETYPEROLE,
                    initialMap.get(MAC_CFBUNDLETYPEROLE));

            putUnlessNull(args, MAC_LSHANDLERRANK,
                    initialMap.get(MAC_LSHANDLERRANK));

            putUnlessNull(args, MAC_NSSTORETYPEKEY,
                    initialMap.get(MAC_NSSTORETYPEKEY));

            putUnlessNull(args, MAC_NSDOCUMENTCLASS,
                    initialMap.get(MAC_NSDOCUMENTCLASS));

            putUnlessNull(args, MAC_LSTYPEISPACKAGE,
                    initialMap.get(MAC_LSTYPEISPACKAGE));

            putUnlessNull(args, MAC_LSDOCINPLACE,
                    initialMap.get(MAC_LSDOCINPLACE));

            putUnlessNull(args, MAC_UIDOCBROWSER,
                    initialMap.get(MAC_UIDOCBROWSER));

            putUnlessNull(args, MAC_NSEXPORTABLETYPES,
                    initialMap.get(MAC_NSEXPORTABLETYPES));

            putUnlessNull(args, MAC_UTTYPECONFORMSTO,
                    initialMap.get(MAC_UTTYPECONFORMSTO));

            ArrayList<Map<String, ? super Object>> associationList =
                new ArrayList<Map<String, ? super Object>>();

            associationList.add(args);

            // check that we really add _another_ value to the list
            setOptionValue("file-associations", associationList);

        }),

        ADD_LAUNCHER ("add-launcher",
                    OptionCategories.PROPERTY, () -> {
            String spec = popArg();
            String name = null;
            String filename = spec;
            if (spec.contains("=")) {
                String[] values = spec.split("=", 2);
                name = values[0];
                filename = values[1];
            }
            context().addLaunchers.add(
                new AddLauncherArguments(name, filename));
        }),

        TEMP_ROOT ("temp", OptionCategories.PROPERTY, () -> {
            context().buildRoot = popArg();
            context().userProvidedBuildRoot = true;
            setOptionValue("temp", context().buildRoot);
        }),

        INSTALL_DIR ("install-dir", OptionCategories.PROPERTY),

        PREDEFINED_APP_IMAGE ("app-image", OptionCategories.PROPERTY),

        PREDEFINED_RUNTIME_IMAGE ("runtime-image", OptionCategories.PROPERTY),

        MAIN_JAR ("main-jar",  OptionCategories.PROPERTY, () -> {
            context().mainJarPath = popArg();
            context().hasMainJar = true;
            setOptionValue("main-jar", context().mainJarPath);
        }),

        MODULE ("module", "m", OptionCategories.MODULAR, () -> {
            context().hasMainModule = true;
            setOptionValue("module", popArg());
        }),

        ADD_MODULES ("add-modules", OptionCategories.MODULAR),

        MODULE_PATH ("module-path", "p", OptionCategories.MODULAR),

        MAC_SIGN ("mac-sign", "s", OptionCategories.PLATFORM_MAC, () -> {
            setOptionValue("mac-sign", true);
        }),

        MAC_APP_STORE ("mac-app-store", OptionCategories.PLATFORM_MAC, () -> {
            setOptionValue("mac-app-store", true);
        }),

        MAC_CATEGORY ("mac-app-category", OptionCategories.PLATFORM_MAC),

        MAC_BUNDLE_NAME ("mac-package-name", OptionCategories.PLATFORM_MAC),

        MAC_BUNDLE_IDENTIFIER("mac-package-identifier",
                    OptionCategories.PLATFORM_MAC),

        MAC_BUNDLE_SIGNING_PREFIX ("mac-package-signing-prefix",
                    OptionCategories.PLATFORM_MAC),

        MAC_SIGNING_KEY_NAME ("mac-signing-key-user-name",
                    OptionCategories.PLATFORM_MAC),

        MAC_SIGNING_KEYCHAIN ("mac-signing-keychain",
                    OptionCategories.PLATFORM_MAC),

        MAC_ENTITLEMENTS ("mac-entitlements", OptionCategories.PLATFORM_MAC),

        WIN_HELP_URL ("win-help-url", OptionCategories.PLATFORM_WIN),

        WIN_UPDATE_URL ("win-update-url", OptionCategories.PLATFORM_WIN),

        WIN_MENU_HINT ("win-menu", OptionCategories.PLATFORM_WIN, () -> {
            setOptionValue("win-menu", true);
        }),

        WIN_MENU_GROUP ("win-menu-group", OptionCategories.PLATFORM_WIN),

        WIN_SHORTCUT_HINT ("win-shortcut",
                OptionCategories.PLATFORM_WIN, () -> {
            setOptionValue("win-shortcut", true);
        }),

        WIN_SHORTCUT_PROMPT ("win-shortcut-prompt",
                OptionCategories.PLATFORM_WIN, () -> {
            setOptionValue("win-shortcut-prompt", true);
        }),

        WIN_PER_USER_INSTALLATION ("win-per-user-install",
                OptionCategories.PLATFORM_WIN, () -> {
            setOptionValue("win-per-user-install", false);
        }),

        WIN_DIR_CHOOSER ("win-dir-chooser",
                OptionCategories.PLATFORM_WIN, () -> {
            setOptionValue("win-dir-chooser", true);
        }),

        WIN_UPGRADE_UUID ("win-upgrade-uuid",
                OptionCategories.PLATFORM_WIN),

        WIN_CONSOLE_HINT ("win-console", OptionCategories.PLATFORM_WIN, () -> {
            setOptionValue("win-console", true);
        }),

        LINUX_BUNDLE_NAME ("linux-package-name",
                OptionCategories.PLATFORM_LINUX),

        LINUX_DEB_MAINTAINER ("linux-deb-maintainer",
                OptionCategories.PLATFORM_LINUX),

        LINUX_CATEGORY ("linux-app-category",
                OptionCategories.PLATFORM_LINUX),

        LINUX_RPM_LICENSE_TYPE ("linux-rpm-license-type",
                OptionCategories.PLATFORM_LINUX),

        LINUX_PACKAGE_DEPENDENCIES ("linux-package-deps",
                OptionCategories.PLATFORM_LINUX),

        LINUX_SHORTCUT_HINT ("linux-shortcut",
                OptionCategories.PLATFORM_LINUX, () -> {
            setOptionValue("linux-shortcut", true);
        }),

        LINUX_MENU_GROUP ("linux-menu-group", OptionCategories.PLATFORM_LINUX);

        private final String id;
        private final String shortId;
        private final OptionCategories category;
        private final Runnable action;
        private static Arguments argContext;

        private CLIOptions(String id, OptionCategories category) {
            this(id, null, category, null);
        }

        private CLIOptions(String id, String shortId,
                           OptionCategories category) {
            this(id, shortId, category, null);
        }

        private CLIOptions(String id,
                OptionCategories category, Runnable action) {
            this(id, null, category, action);
        }

        private CLIOptions(String id, String shortId,
                           OptionCategories category, Runnable action) {
            this.id = id;
            this.shortId = shortId;
            this.action = action;
            this.category = category;
        }

        public static Arguments context() {
            return instance.get();
        }

        public String getId() {
            return this.id;
        }

        String getIdWithPrefix() {
            return "--" + this.id;
        }

        String getShortIdWithPrefix() {
            return this.shortId == null ? null : "-" + this.shortId;
        }

        void execute() {
            if (action != null) {
                action.run();
            } else {
                defaultAction();
            }
        }

        private void defaultAction() {
            context().deployParams.addBundleArgument(id, popArg());
        }

        private static void setOptionValue(String option, Object value) {
            context().deployParams.addBundleArgument(option, value);
        }

        private static String popArg() {
            nextArg();
            return (context().pos >= context().argList.size()) ?
                            "" : context().argList.get(context().pos);
        }

        private static String getArg() {
            return (context().pos >= context().argList.size()) ?
                        "" : context().argList.get(context().pos);
        }

        private static void nextArg() {
            context().pos++;
        }

        private static boolean hasNextArg() {
            return context().pos < context().argList.size();
        }
    }

    enum OptionCategories {
        MODULAR,
        PROPERTY,
        PLATFORM_MAC,
        PLATFORM_WIN,
        PLATFORM_LINUX;
    }

    public boolean processArguments() {
        try {
            // parse cmd line
            String arg;
            CLIOptions option;
            for (; CLIOptions.hasNextArg(); CLIOptions.nextArg()) {
                arg = CLIOptions.getArg();
                if ((option = toCLIOption(arg)) != null) {
                    // found a CLI option
                    allOptions.add(option);
                    option.execute();
                } else {
                    throw new PackagerException("ERR_InvalidOption", arg);
                }
            }

            if (hasMainJar && !hasMainClass) {
                // try to get main-class from manifest
                String mainClass = getMainClassFromManifest();
                if (mainClass != null) {
                    CLIOptions.setOptionValue(
                            CLIOptions.APPCLASS.getId(), mainClass);
                }
            }

            // display error for arguments that are not supported
            // for current configuration.

            validateArguments();

            List<Map<String, ? super Object>> launchersAsMap =
                    new ArrayList<>();

            for (AddLauncherArguments sl : addLaunchers) {
                launchersAsMap.add(sl.getLauncherMap());
            }

            deployParams.addBundleArgument(
                    StandardBundlerParam.ADD_LAUNCHERS.getID(),
                    launchersAsMap);

            // at this point deployParams should be already configured

            deployParams.validate();

            BundleParams bp = deployParams.getBundleParams();

            // validate name(s)
            ArrayList<String> usedNames = new ArrayList<String>();
            usedNames.add(bp.getName()); // add main app name

            for (AddLauncherArguments sl : addLaunchers) {
                Map<String, ? super Object> slMap = sl.getLauncherMap();
                String slName =
                        (String) slMap.get(Arguments.CLIOptions.NAME.getId());
                if (slName == null) {
                    throw new PackagerException("ERR_NoAddLauncherName");
                }
                // same rules apply to additional launcher names as app name
                DeployParams.validateName(slName, false);
                for (String usedName : usedNames) {
                    if (slName.equals(usedName)) {
                        throw new PackagerException("ERR_NoUniqueName");
                    }
                }
                usedNames.add(slName);
            }
            if (runtimeInstaller && bp.getName() == null) {
                throw new PackagerException("ERR_NoJreInstallerName");
            }

            generateBundle(bp.getBundleParamsAsMap());
            return true;
        } catch (Exception e) {
            if (Log.isVerbose()) {
                Log.verbose(e);
            } else {
                String msg1 = e.getMessage();
                Log.fatalError(msg1);
                if (e.getCause() != null && e.getCause() != e) {
                    String msg2 = e.getCause().getMessage();
                    if (msg2 != null && !msg1.contains(msg2)) {
                        Log.fatalError(msg2);
                    }
                }
            }
            return false;
        }
    }

    private void validateArguments() throws PackagerException {
        String type = deployParams.getTargetFormat();
        String ptype = (type != null) ? type : "default";
        boolean imageOnly = deployParams.isTargetAppImage();
        boolean hasAppImage = allOptions.contains(
                CLIOptions.PREDEFINED_APP_IMAGE);
        boolean hasRuntime = allOptions.contains(
                CLIOptions.PREDEFINED_RUNTIME_IMAGE);
        boolean installerOnly = !imageOnly && hasAppImage;
        runtimeInstaller = !imageOnly && hasRuntime && !hasAppImage &&
                !hasMainModule && !hasMainJar;

        for (CLIOptions option : allOptions) {
            if (!ValidOptions.checkIfSupported(option)) {
                // includes option valid only on different platform
                throw new PackagerException("ERR_UnsupportedOption",
                        option.getIdWithPrefix());
            }
            if (imageOnly) {
                if (!ValidOptions.checkIfImageSupported(option)) {
                    throw new PackagerException("ERR_InvalidTypeOption",
                        option.getIdWithPrefix(), type);
                }
            } else if (installerOnly || runtimeInstaller) {
                if (!ValidOptions.checkIfInstallerSupported(option)) {
                    if (runtimeInstaller) {
                        throw new PackagerException("ERR_NoInstallerEntryPoint",
                            option.getIdWithPrefix());
                    } else {
                        throw new PackagerException("ERR_InvalidTypeOption",
                            option.getIdWithPrefix(), ptype);
                   }
                }
            }
        }
        if (hasRuntime) {
            if (hasAppImage) {
                // note --runtime-image is only for image or runtime installer.
                throw new PackagerException("ERR_MutuallyExclusiveOptions",
                        CLIOptions.PREDEFINED_RUNTIME_IMAGE.getIdWithPrefix(),
                        CLIOptions.PREDEFINED_APP_IMAGE.getIdWithPrefix());
            }
            if (allOptions.contains(CLIOptions.ADD_MODULES)) {
                throw new PackagerException("ERR_MutuallyExclusiveOptions",
                        CLIOptions.PREDEFINED_RUNTIME_IMAGE.getIdWithPrefix(),
                        CLIOptions.ADD_MODULES.getIdWithPrefix());
            }
            if (allOptions.contains(CLIOptions.JLINK_OPTIONS)) {
                throw new PackagerException("ERR_MutuallyExclusiveOptions",
                        CLIOptions.PREDEFINED_RUNTIME_IMAGE.getIdWithPrefix(),
                        CLIOptions.JLINK_OPTIONS.getIdWithPrefix());
            }
        }
        if (hasMainJar && hasMainModule) {
            throw new PackagerException("ERR_BothMainJarAndModule");
        }
        if (imageOnly && !hasMainJar && !hasMainModule) {
            throw new PackagerException("ERR_NoEntryPoint");
        }
    }

    private jdk.jpackage.internal.Bundler getPlatformBundler() {
        boolean appImage = deployParams.isTargetAppImage();
        String type = deployParams.getTargetFormat();
        String bundleType = (appImage ?  "IMAGE" : "INSTALLER");

        for (jdk.jpackage.internal.Bundler bundler :
                Bundlers.createBundlersInstance().getBundlers(bundleType)) {
            if (type == null) {
                 if (bundler.isDefault()
                         && bundler.supported(runtimeInstaller)) {
                     return bundler;
                 }
            } else {
                 if ((appImage || type.equalsIgnoreCase(bundler.getID()))
                         && bundler.supported(runtimeInstaller)) {
                     return bundler;
                 }
            }
        }
        return null;
    }

    private void generateBundle(Map<String,? super Object> params)
            throws PackagerException {

        boolean bundleCreated = false;

        // the temp dir needs to be fetched from the params early,
        // to prevent each copy of the params (such as may be used for
        // additional launchers) from generating a separate temp dir when
        // the default is used (the default is a new temp directory)
        // The bundler.cleanup() below would not otherwise be able to
        // clean these extra (and unneeded) temp directories.
        StandardBundlerParam.TEMP_ROOT.fetchFrom(params);

        // determine what bundler to run
        jdk.jpackage.internal.Bundler bundler = getPlatformBundler();

        if (bundler == null) {
            throw new PackagerException("ERR_InvalidInstallerType",
                      deployParams.getTargetFormat());
        }

        Map<String, ? super Object> localParams = new HashMap<>(params);
        try {
            bundler.validate(localParams);
            Path result = bundler.execute(localParams, deployParams.outdir);
            if (result == null) {
                throw new PackagerException("MSG_BundlerFailed",
                        bundler.getID(), bundler.getName());
            }
            Log.verbose(MessageFormat.format(
                    I18N.getString("message.bundle-created"),
                    bundler.getName()));
        } catch (ConfigException e) {
            Log.verbose(e);
            if (e.getAdvice() != null)  {
                throw new PackagerException(e, "MSG_BundlerConfigException",
                        bundler.getName(), e.getMessage(), e.getAdvice());
            } else {
                throw new PackagerException(e,
                       "MSG_BundlerConfigExceptionNoAdvice",
                        bundler.getName(), e.getMessage());
            }
        } catch (RuntimeException re) {
            Log.verbose(re);
            throw new PackagerException(re, "MSG_BundlerRuntimeException",
                    bundler.getName(), re.toString());
        } finally {
            if (userProvidedBuildRoot) {
                Log.verbose(MessageFormat.format(
                        I18N.getString("message.debug-working-directory"),
                        (Path.of(buildRoot)).toAbsolutePath().toString()));
            } else {
                // always clean up the temporary directory created
                // when --temp option not used.
                bundler.cleanup(localParams);
            }
        }
    }

    static CLIOptions toCLIOption(String arg) {
        CLIOptions option;
        if ((option = argIds.get(arg)) == null) {
            option = argShortIds.get(arg);
        }
        return option;
    }

    static Map<String, String> getPropertiesFromFile(String filename) {
        Map<String, String> map = new HashMap<>();
        // load properties file
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of(filename))) {
            properties.load(reader);
        } catch (IOException e) {
            Log.error("Exception: " + e.getMessage());
        }

        for (final String name: properties.stringPropertyNames()) {
            map.put(name, properties.getProperty(name));
        }

        return map;
    }

    static List<String> getArgumentList(String inputString) {
        List<String> list = new ArrayList<>();
        if (inputString == null || inputString.isEmpty()) {
             return list;
        }

        // The "pattern" regexp attempts to abide to the rule that
        // strings are delimited by whitespace unless surrounded by
        // quotes, then it is anything (including spaces) in the quotes.
        Matcher m = pattern.matcher(inputString);
        while (m.find()) {
            String s = inputString.substring(m.start(), m.end()).trim();
            // Ensure we do not have an empty string. trim() will take care of
            // whitespace only strings. The regex preserves quotes and escaped
            // chars so we need to clean them before adding to the List
            if (!s.isEmpty()) {
                list.add(unquoteIfNeeded(s));
            }
        }
        return list;
    }

    static void putUnlessNull(Map<String, ? super Object> params,
            String param, Object value) {
        if (value != null) {
            params.put(param, value);
        }
    }

    private static String unquoteIfNeeded(String in) {
        if (in == null) {
            return null;
        }

        if (in.isEmpty()) {
            return "";
        }

        // Use code points to preserve non-ASCII chars
        StringBuilder sb = new StringBuilder();
        int codeLen = in.codePointCount(0, in.length());
        int quoteChar = -1;
        for (int i = 0; i < codeLen; i++) {
            int code = in.codePointAt(i);
            if (code == '"' || code == '\'') {
                // If quote is escaped make sure to copy it
                if (i > 0 && in.codePointAt(i - 1) == '\\') {
                    sb.deleteCharAt(sb.length() - 1);
                    sb.appendCodePoint(code);
                    continue;
                }
                if (quoteChar != -1) {
                    if (code == quoteChar) {
                        // close quote, skip char
                        quoteChar = -1;
                    } else {
                        sb.appendCodePoint(code);
                    }
                } else {
                    // opening quote, skip char
                    quoteChar = code;
                }
            } else {
                sb.appendCodePoint(code);
            }
        }
        return sb.toString();
    }

    private String getMainClassFromManifest() {
        if (mainJarPath == null ||
            input == null ) {
            return null;
        }

        JarFile jf;
        try {
            Path file = Path.of(input, mainJarPath);
            if (!Files.exists(file)) {
                return null;
            }
            jf = new JarFile(file.toFile());
            Manifest m = jf.getManifest();
            Attributes attrs = (m != null) ? m.getMainAttributes() : null;
            if (attrs != null) {
                return attrs.getValue(Attributes.Name.MAIN_CLASS);
            }
        } catch (IOException ignore) {}
        return null;
    }

}
