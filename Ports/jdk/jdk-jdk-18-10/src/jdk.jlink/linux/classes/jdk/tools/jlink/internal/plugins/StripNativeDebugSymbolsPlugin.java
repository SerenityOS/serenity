/*
 * Copyright (c) 2019, 2020, Red Hat, Inc.
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
package jdk.tools.jlink.internal.plugins;

import java.io.InputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import jdk.tools.jlink.plugin.PluginException;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.plugin.ResourcePoolEntry;

/**
 * Platform specific jlink plugin for stripping debug symbols from native
 * libraries and binaries.
 *
 */
public final class StripNativeDebugSymbolsPlugin extends AbstractPlugin {

    public static final String NAME = "strip-native-debug-symbols";
    private static final boolean DEBUG = Boolean.getBoolean("jlink.debug");
    private static final String DEFAULT_STRIP_CMD = "objcopy";
    private static final String STRIP_CMD_ARG = DEFAULT_STRIP_CMD;
    private static final String KEEP_DEBUG_INFO_ARG = "keep-debuginfo-files";
    private static final String EXCLUDE_DEBUG_INFO_ARG = "exclude-debuginfo-files";
    private static final String DEFAULT_DEBUG_EXT = "debuginfo";
    private static final String STRIP_DEBUG_SYMS_OPT = "-g";
    private static final String ONLY_KEEP_DEBUG_SYMS_OPT = "--only-keep-debug";
    private static final String ADD_DEBUG_LINK_OPT = "--add-gnu-debuglink";
    private static final ResourceBundle resourceBundle;
    private static final String SHARED_LIBS_EXT = ".so"; // for Linux/Unix

    static {
        Locale locale = Locale.getDefault();
        try {
            resourceBundle = ResourceBundle.getBundle("jdk.tools.jlink."
                    + "resources.strip_native_debug_symbols_plugin", locale);
        } catch (MissingResourceException e) {
            throw new InternalError("Cannot find jlink plugin resource bundle (" +
                        NAME + ") for locale " + locale);
        }
    }

    private final ObjCopyCmdBuilder cmdBuilder;
    private boolean includeDebugSymbols;
    private String stripBin;
    private String debuginfoExt;

    public StripNativeDebugSymbolsPlugin() {
        this(new DefaultObjCopyCmdBuilder());
    }

    public StripNativeDebugSymbolsPlugin(ObjCopyCmdBuilder cmdBuilder) {
        super(NAME, resourceBundle);
        this.cmdBuilder = cmdBuilder;
    }

    @Override
    public ResourcePool transform(ResourcePool in, ResourcePoolBuilder out) {
        StrippedDebugInfoBinaryBuilder builder = new StrippedDebugInfoBinaryBuilder(
                                                        includeDebugSymbols,
                                                        debuginfoExt,
                                                        cmdBuilder,
                                                        stripBin);
        in.transformAndCopy((resource) -> {
            ResourcePoolEntry res = resource;
            if ((resource.type() == ResourcePoolEntry.Type.NATIVE_LIB &&
                 resource.path().endsWith(SHARED_LIBS_EXT)) ||
                resource.type() == ResourcePoolEntry.Type.NATIVE_CMD) {
                Optional<StrippedDebugInfoBinary> strippedBin = builder.build(resource);
                if (strippedBin.isPresent()) {
                    StrippedDebugInfoBinary sb = strippedBin.get();
                    res = sb.strippedBinary();
                    if (includeDebugSymbols) {
                        Optional<ResourcePoolEntry> debugInfo = sb.debugSymbols();
                        if (debugInfo.isEmpty()) {
                            String key = NAME + ".error.debugfile";
                            logError(resource, key);
                        } else {
                            out.add(debugInfo.get());
                        }
                    }
                } else {
                    String key = NAME + ".error.file";
                    logError(resource, key);
                }
            }
            return res;
        }, out);

        return out.build();
    }

    private void logError(ResourcePoolEntry resource, String msgKey) {
        String msg = getMessage(msgKey,
                                NAME,
                                resource.path());
        System.err.println(msg);
    }

    @Override
    public Category getType() {
        return Category.TRANSFORMER;
    }

    @Override
    public boolean hasArguments() {
        return true;
    }

    @Override
    public void configure(Map<String, String> config) {
        doConfigure(true, config);
    }

    // For testing so that validation can be turned off
    public void doConfigure(boolean withChecks, Map<String, String> orig) {
        Map<String, String> config = new HashMap<>(orig);
        String arg = config.remove(NAME);

        stripBin = DEFAULT_STRIP_CMD;
        debuginfoExt = DEFAULT_DEBUG_EXT;

        // argument must never be null as it requires at least one
        // argument, since hasArguments() == true. This might change once
        // 8218761 is implemented.
        if (arg == null) {
            throw new InternalError();
        }
        boolean hasOmitDebugInfo = false;
        boolean hasKeepDebugInfo = false;

        if (KEEP_DEBUG_INFO_ARG.equals(arg)) {
            // Case: --strip-native-debug-symbols keep-debuginfo-files
            hasKeepDebugInfo = true;
        } else if (arg.startsWith(KEEP_DEBUG_INFO_ARG)) {
            // Case: --strip-native-debug-symbols keep-debuginfo-files=foo
            String[] tokens = arg.split("=");
            if (tokens.length != 2 || !KEEP_DEBUG_INFO_ARG.equals(tokens[0])) {
                throw new IllegalArgumentException(
                        getMessage(NAME + ".iae", NAME, arg));
            }
            hasKeepDebugInfo = true;
            debuginfoExt = tokens[1];
        }
        if (EXCLUDE_DEBUG_INFO_ARG.equals(arg) || arg.startsWith(EXCLUDE_DEBUG_INFO_ARG + "=")) {
            // Case: --strip-native-debug-symbols exclude-debuginfo-files[=something]
            hasOmitDebugInfo = true;
        }
        if (arg.startsWith(STRIP_CMD_ARG)) {
            // Case: --strip-native-debug-symbols objcopy=<path/to/objcopy
            String[] tokens = arg.split("=");
            if (tokens.length != 2 || !STRIP_CMD_ARG.equals(tokens[0])) {
                throw new IllegalArgumentException(
                        getMessage(NAME + ".iae", NAME, arg));
            }
            if (withChecks) {
                validateStripArg(tokens[1]);
            }
            stripBin = tokens[1];
        }
        // Cases (combination of options):
        //   --strip-native-debug-symbols keep-debuginfo-files:objcopy=</objcpy/path>
        //   --strip-native-debug-symbols keep-debuginfo-files=ext:objcopy=</objcpy/path>
        //   --strip-native-debug-symbols exclude-debuginfo-files:objcopy=</objcpy/path>
        String stripArg = config.remove(STRIP_CMD_ARG);
        if (stripArg != null && withChecks) {
            validateStripArg(stripArg);
        }
        if (stripArg != null) {
            stripBin = stripArg;
        }
        // Case (reversed combination)
        //   --strip-native-debug-symbols objcopy=</objcpy/path>:keep-debuginfo-files=ext
        // Note: cases like the following are not allowed by the parser
        //   --strip-native-debug-symbols objcopy=</objcpy/path>:keep-debuginfo-files
        //   --strip-native-debug-symbols objcopy=</objcpy/path>:exclude-debuginfo-files
        String keepDebugInfo = config.remove(KEEP_DEBUG_INFO_ARG);
        if (keepDebugInfo != null) {
            hasKeepDebugInfo = true;
            debuginfoExt = keepDebugInfo;
        }
        if ((hasKeepDebugInfo || includeDebugSymbols) && hasOmitDebugInfo) {
            // Cannot keep and omit debug info at the same time. Note that
            // includeDebugSymbols might already be true if configure is being run
            // on the same plugin instance multiple times. Plugin option can
            // repeat.
            throw new IllegalArgumentException(
                    getMessage(NAME + ".iae.conflict",
                               NAME,
                               EXCLUDE_DEBUG_INFO_ARG,
                               KEEP_DEBUG_INFO_ARG));
        }
        if (!arg.startsWith(STRIP_CMD_ARG) &&
            !arg.startsWith(KEEP_DEBUG_INFO_ARG) &&
            !arg.startsWith(EXCLUDE_DEBUG_INFO_ARG)) {
            // unknown arg value; case --strip-native-debug-symbols foobar
            throw new IllegalArgumentException(
                    getMessage(NAME + ".iae", NAME, arg));
        }
        if (!config.isEmpty()) {
            // extraneous values; --strip-native-debug-symbols keep-debuginfo-files:foo=bar
            throw new IllegalArgumentException(
                    getMessage(NAME + ".iae", NAME,
                               config.toString()));
        }
        includeDebugSymbols = hasKeepDebugInfo;
    }

    private void validateStripArg(String stripArg) throws IllegalArgumentException {
        try {
            Path strip = Paths.get(stripArg); // verify it's a resonable path
            if (!Files.isExecutable(strip)) {
                throw new IllegalArgumentException(
                        getMessage(NAME + ".invalidstrip",
                                   stripArg));
            }
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException(
                    getMessage(NAME + ".invalidstrip",
                               e.getInput()));
        }
    }

    private static class StrippedDebugInfoBinaryBuilder {

        private final boolean includeDebug;
        private final String debugExt;
        private final ObjCopyCmdBuilder cmdBuilder;
        private final String strip;

        private StrippedDebugInfoBinaryBuilder(boolean includeDebug,
                                               String debugExt,
                                               ObjCopyCmdBuilder cmdBuilder,
                                               String strip) {
            this.includeDebug = includeDebug;
            this.debugExt = debugExt;
            this.cmdBuilder = cmdBuilder;
            this.strip = strip;
        }

        private Optional<StrippedDebugInfoBinary> build(ResourcePoolEntry resource) {
            Path tempDir = null;
            Optional<ResourcePoolEntry> debugInfo = Optional.empty();
            try {
                Path resPath = Paths.get(resource.path());
                String relativeFileName = resPath.getFileName().toString();
                tempDir = Files.createTempDirectory(NAME + relativeFileName);
                Path resourceFileBinary = tempDir.resolve(relativeFileName);
                String relativeDbgFileName = relativeFileName + "." + debugExt;

                try (InputStream in = resource.content()) {
                    Files.copy(in, resourceFileBinary);
                }

                Path resourceFileDebugSymbols;
                if (includeDebug) {
                    resourceFileDebugSymbols = tempDir.resolve(Paths.get(relativeDbgFileName));
                    String debugEntryPath = resource.path() + "." + debugExt;
                    byte[] debugInfoBytes = createDebugSymbolsFile(resourceFileBinary,
                                           resourceFileDebugSymbols,
                                           relativeDbgFileName);
                    if (debugInfoBytes != null) {
                        ResourcePoolEntry debugEntry = ResourcePoolEntry.create(
                                                                debugEntryPath,
                                                                resource.type(),
                                                                debugInfoBytes);
                        debugInfo = Optional.of(debugEntry);
                    }
                }
                if (!stripBinary(resourceFileBinary)) {
                    if (DEBUG) {
                        System.err.println("DEBUG: Stripping debug info failed.");
                    }
                    return Optional.empty();
                }
                if (includeDebug && !addGnuDebugLink(tempDir,
                                                     relativeFileName,
                                                     relativeDbgFileName)) {
                    if (DEBUG) {
                        System.err.println("DEBUG: Creating debug link failed.");
                    }
                    return Optional.empty();
                }
                byte[] strippedBytes = Files.readAllBytes(resourceFileBinary);
                ResourcePoolEntry strippedResource = resource.copyWithContent(strippedBytes);
                return Optional.of(new StrippedDebugInfoBinary(strippedResource, debugInfo));
            } catch (IOException | InterruptedException e) {
                throw new PluginException(e);
            } finally {
                if (tempDir != null) {
                    deleteDirRecursivelyIgnoreResult(tempDir);
                }
            }
        }

        /*
         *  Equivalent of 'objcopy -g binFile'. Returning true iff stripping of the binary
         *  succeeded.
         */
        private boolean stripBinary(Path binFile)
                throws InterruptedException, IOException {
            String filePath = binFile.toAbsolutePath().toString();
            List<String> stripCmdLine = cmdBuilder.build(strip, STRIP_DEBUG_SYMS_OPT,
                                                     filePath);
            ProcessBuilder builder = createProcessBuilder(stripCmdLine);
            Process stripProc = builder.start();
            int retval = stripProc.waitFor();
            return retval == 0;
        }

        /*
         *  Equivalent of 'objcopy --add-gnu-debuglink=relativeDbgFileName binFile'.
         *  Returning true iff adding the debug link succeeded.
         */
        private boolean addGnuDebugLink(Path currDir,
                                        String binFile,
                                        String relativeDbgFileName)
                                                throws InterruptedException, IOException {
            List<String> addDbgLinkCmdLine = cmdBuilder.build(strip, ADD_DEBUG_LINK_OPT +
                                                     "=" + relativeDbgFileName,
                                                     binFile);
            ProcessBuilder builder = createProcessBuilder(addDbgLinkCmdLine);
            builder.directory(currDir.toFile());
            Process stripProc = builder.start();
            int retval = stripProc.waitFor();
            return retval == 0;

        }

        /*
         *  Equivalent of 'objcopy --only-keep-debug binPath debugPath'.
         *  Returning the bytes of the file containing debug symbols.
         */
        private byte[] createDebugSymbolsFile(Path binPath,
                                              Path debugPath,
                                              String dbgFileName) throws InterruptedException,
                                                                         IOException {
            String filePath = binPath.toAbsolutePath().toString();
            String dbgPath = debugPath.toAbsolutePath().toString();
            List<String> createLinkCmdLine = cmdBuilder.build(strip,
                                                     ONLY_KEEP_DEBUG_SYMS_OPT,
                                                     filePath,
                                                     dbgPath);
            ProcessBuilder builder = createProcessBuilder(createLinkCmdLine);
            Process stripProc = builder.start();
            int retval = stripProc.waitFor();
            if (retval != 0) {
                if (DEBUG) {
                    System.err.println("DEBUG: Creating debuginfo file failed.");
                }
                return null;
            } else {
                return Files.readAllBytes(debugPath);
            }
        }

        private ProcessBuilder createProcessBuilder(List<String> cmd) {
            ProcessBuilder builder = new ProcessBuilder(cmd);
            builder.redirectError(Redirect.INHERIT);
            builder.redirectOutput(Redirect.INHERIT);
            return builder;
        }

        private void deleteDirRecursivelyIgnoreResult(Path tempDir) {
            try {
                Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file,
                            BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir,
                            IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                // ignore deleting the temp dir
            }
        }

    }

    private static class StrippedDebugInfoBinary {
        private final ResourcePoolEntry strippedBinary;
        private final Optional<ResourcePoolEntry> debugSymbols;

        private StrippedDebugInfoBinary(ResourcePoolEntry strippedBinary,
                                        Optional<ResourcePoolEntry> debugSymbols) {
            this.strippedBinary = Objects.requireNonNull(strippedBinary);
            this.debugSymbols = Objects.requireNonNull(debugSymbols);
        }

        public ResourcePoolEntry strippedBinary() {
            return strippedBinary;
        }

        public Optional<ResourcePoolEntry> debugSymbols() {
            return debugSymbols;
        }
    }

    // For better testing using mocked objcopy
    public static interface ObjCopyCmdBuilder {
        List<String> build(String objCopy, String...options);
    }

    private static final class DefaultObjCopyCmdBuilder implements ObjCopyCmdBuilder {

        @Override
        public List<String> build(String objCopy, String...options) {
            List<String> cmdList = new ArrayList<>();
            cmdList.add(objCopy);
            if (options.length > 0) {
                cmdList.addAll(Arrays.asList(options));
            }
            return cmdList;
        }

    }

}
