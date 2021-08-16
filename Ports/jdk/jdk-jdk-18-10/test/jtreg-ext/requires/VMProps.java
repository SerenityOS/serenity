/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package requires;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.test.whitebox.code.Compiler;
import jdk.test.whitebox.cpuinfo.CPUInfo;
import jdk.test.whitebox.gc.GC;
import jdk.test.whitebox.WhiteBox;
import jdk.test.lib.Platform;
import jdk.test.lib.Container;

/**
 * The Class to be invoked by jtreg prior Test Suite execution to
 * collect information about VM.
 * Do not use any APIs that may not be available in all target VMs.
 * Properties set by this Class will be available in the @requires expressions.
 */
public class VMProps implements Callable<Map<String, String>> {
    // value known to jtreg as an indicator of error state
    private static final String ERROR_STATE = "__ERROR__";

    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    private static class SafeMap {
        private final Map<String, String> map = new HashMap<>();

        public void put(String key, Supplier<String> s) {
            String value;
            try {
                value = s.get();
            } catch (Throwable t) {
                System.err.println("failed to get value for " + key);
                t.printStackTrace(System.err);
                value = ERROR_STATE + t;
            }
            map.put(key, value);
        }
    }

    /**
     * Collects information about VM properties.
     * This method will be invoked by jtreg.
     *
     * @return Map of property-value pairs.
     */
    @Override
    public Map<String, String> call() {
        SafeMap map = new SafeMap();
        map.put("vm.flavor", this::vmFlavor);
        map.put("vm.compMode", this::vmCompMode);
        map.put("vm.bits", this::vmBits);
        map.put("vm.flightRecorder", this::vmFlightRecorder);
        map.put("vm.simpleArch", this::vmArch);
        map.put("vm.debug", this::vmDebug);
        map.put("vm.jvmci", this::vmJvmci);
        map.put("vm.emulatedClient", this::vmEmulatedClient);
        // vm.hasSA is "true" if the VM contains the serviceability agent
        // and jhsdb.
        map.put("vm.hasSA", this::vmHasSA);
        // vm.hasJFR is "true" if JFR is included in the build of the VM and
        // so tests can be executed.
        map.put("vm.hasJFR", this::vmHasJFR);
        map.put("vm.jvmti", this::vmHasJVMTI);
        map.put("vm.cpu.features", this::cpuFeatures);
        map.put("vm.pageSize", this::vmPageSize);
        map.put("vm.rtm.cpu", this::vmRTMCPU);
        map.put("vm.rtm.compiler", this::vmRTMCompiler);
        // vm.cds is true if the VM is compiled with cds support.
        map.put("vm.cds", this::vmCDS);
        map.put("vm.cds.custom.loaders", this::vmCDSForCustomLoaders);
        map.put("vm.cds.archived.java.heap", this::vmCDSForArchivedJavaHeap);
        // vm.graal.enabled is true if Graal is used as JIT
        map.put("vm.graal.enabled", this::isGraalEnabled);
        map.put("vm.compiler1.enabled", this::isCompiler1Enabled);
        map.put("vm.compiler2.enabled", this::isCompiler2Enabled);
        map.put("docker.support", this::dockerSupport);
        map.put("vm.musl", this::isMusl);
        map.put("release.implementor", this::implementor);
        map.put("jdk.containerized", this::jdkContainerized);
        map.put("vm.flagless", this::isFlagless);
        vmGC(map); // vm.gc.X = true/false
        vmOptFinalFlags(map);

        dump(map.map);
        return map.map;
    }

    /**
     * Print a stack trace before returning error state;
     * Used by the various helper functions which parse information from
     * VM properties in the case where they don't find an expected property
     * or a property doesn't conform to an expected format.
     *
     * @return {@link #ERROR_STATE}
     */
    private String errorWithMessage(String message) {
        new Exception(message).printStackTrace();
        return ERROR_STATE + message;
    }

    /**
     * @return vm.simpleArch value of "os.simpleArch" property of tested JDK.
     */
    protected String vmArch() {
        String arch = System.getProperty("os.arch");
        if (arch.equals("x86_64") || arch.equals("amd64")) {
            return "x64";
        } else if (arch.contains("86")) {
            return "x86";
        } else {
            return arch;
        }
    }

    /**
     * @return VM type value extracted from the "java.vm.name" property.
     */
    protected String vmFlavor() {
        // E.g. "Java HotSpot(TM) 64-Bit Server VM"
        String vmName = System.getProperty("java.vm.name");
        if (vmName == null) {
            return errorWithMessage("Can't get 'java.vm.name' property");
        }

        Pattern startP = Pattern.compile(".* (\\S+) VM");
        Matcher m = startP.matcher(vmName);
        if (m.matches()) {
            return m.group(1).toLowerCase();
        }
        return errorWithMessage("Can't get VM flavor from 'java.vm.name'");
    }

    /**
     * @return VM compilation mode extracted from the "java.vm.info" property.
     */
    protected String vmCompMode() {
        // E.g. "mixed mode"
        String vmInfo = System.getProperty("java.vm.info");
        if (vmInfo == null) {
            return errorWithMessage("Can't get 'java.vm.info' property");
        }
        vmInfo = vmInfo.toLowerCase();
        if (vmInfo.contains("mixed mode")) {
            return "Xmixed";
        } else if (vmInfo.contains("compiled mode")) {
            return "Xcomp";
        } else if (vmInfo.contains("interpreted mode")) {
            return "Xint";
        } else {
            return errorWithMessage("Can't get compilation mode from 'java.vm.info'");
        }
    }

    /**
     * @return VM bitness, the value of the "sun.arch.data.model" property.
     */
    protected String vmBits() {
        String dataModel = System.getProperty("sun.arch.data.model");
        if (dataModel != null) {
            return dataModel;
        } else {
            return errorWithMessage("Can't get 'sun.arch.data.model' property");
        }
    }

    /**
     * @return "true" if Flight Recorder is enabled, "false" if is disabled.
     */
    protected String vmFlightRecorder() {
        Boolean isFlightRecorder = WB.getBooleanVMFlag("FlightRecorder");
        String startFROptions = WB.getStringVMFlag("StartFlightRecording");
        if (isFlightRecorder != null && isFlightRecorder) {
            return "true";
        }
        if (startFROptions != null && !startFROptions.isEmpty()) {
            return "true";
        }
        return "false";
    }

    /**
     * @return debug level value extracted from the "jdk.debug" property.
     */
    protected String vmDebug() {
        String debug = System.getProperty("jdk.debug");
        if (debug != null) {
            return "" + debug.contains("debug");
        } else {
            return errorWithMessage("Can't get 'jdk.debug' property");
        }
    }

    /**
     * @return true if VM supports JVMCI and false otherwise
     */
    protected String vmJvmci() {
        // builds with jvmci have this flag
        if (WB.getBooleanVMFlag("EnableJVMCI") == null) {
            return "false";
        }

        // Not all GCs have full JVMCI support
        if (!WB.isJVMCISupportedByGC()) {
          return "false";
        }

        // Interpreted mode cannot enable JVMCI
        if (vmCompMode().equals("Xint")) {
          return "false";
        }

        return "true";
    }

    /**
     * @return true if VM runs in emulated-client mode and false otherwise.
     */
    protected String vmEmulatedClient() {
        String vmInfo = System.getProperty("java.vm.info");
        if (vmInfo == null) {
            return errorWithMessage("Can't get 'java.vm.info' property");
        }
        return "" + vmInfo.contains(" emulated-client");
    }

    /**
     * @return supported CPU features
     */
    protected String cpuFeatures() {
        return CPUInfo.getFeatures().toString();
    }

    /**
     * For all existing GC sets vm.gc.X property.
     * Example vm.gc.G1=true means:
     *    VM supports G1
     *    User either set G1 explicitely (-XX:+UseG1GC) or did not set any GC
     *    G1 can be selected, i.e. it doesn't conflict with other VM flags
     *
     * @param map - property-value pairs
     */
    protected void vmGC(SafeMap map) {
        var isJVMCIEnabled = Compiler.isJVMCIEnabled();
        for (GC gc: GC.values()) {
            map.put("vm.gc." + gc.name(),
                    () -> "" + (gc.isSupported()
                            && (!isJVMCIEnabled || gc.isSupportedByJVMCICompiler())
                            && (gc.isSelected() || GC.isSelectedErgonomically())));
        }
    }

    /**
     * Selected final flag.
     *
     * @param map - property-value pairs
     * @param flagName - flag name
     */
    private void vmOptFinalFlag(SafeMap map, String flagName) {
        map.put("vm.opt.final." + flagName,
                () -> String.valueOf(WB.getBooleanVMFlag(flagName)));
    }

    /**
     * Selected sets of final flags.
     *
     * @param map - property-value pairs
     */
    protected void vmOptFinalFlags(SafeMap map) {
        vmOptFinalFlag(map, "ClassUnloading");
        vmOptFinalFlag(map, "ClassUnloadingWithConcurrentMark");
        vmOptFinalFlag(map, "UseCompressedOops");
        vmOptFinalFlag(map, "UseVectorizedMismatchIntrinsic");
        vmOptFinalFlag(map, "EnableJVMCI");
        vmOptFinalFlag(map, "EliminateAllocations");
        vmOptFinalFlag(map, "UseVtableBasedCHA");
    }

    /**
     * @return "true" if VM has a serviceability agent.
     */
    protected String vmHasSA() {
        return "" + Platform.hasSA();
    }

    /**
     * @return "true" if the VM is compiled with Java Flight Recorder (JFR)
     * support.
     */
    protected String vmHasJFR() {
        return "" + WB.isJFRIncluded();
    }

    /**
     * @return "true" if the VM is compiled with JVMTI
     */
    protected String vmHasJVMTI() {
        return "" + WB.isJVMTIIncluded();
    }

    /**
     * @return true if compiler in use supports RTM and false otherwise.
     */
    protected String vmRTMCompiler() {
        boolean isRTMCompiler = false;

        if (Compiler.isC2Enabled() &&
            (Platform.isX86() || Platform.isX64() || Platform.isPPC())) {
            isRTMCompiler = true;
        }
        return "" + isRTMCompiler;
    }

    /**
     * @return true if VM runs RTM supported CPU and false otherwise.
     */
    protected String vmRTMCPU() {
        return "" + CPUInfo.hasFeature("rtm");
    }

    /**
     * Check for CDS support.
     *
     * @return true if CDS is supported by the VM to be tested.
     */
    protected String vmCDS() {
        return "" + WB.isCDSIncluded();
    }

    /**
     * Check for CDS support for custom loaders.
     *
     * @return true if CDS provides support for customer loader in the VM to be tested.
     */
    protected String vmCDSForCustomLoaders() {
        return "" + ("true".equals(vmCDS()) && Platform.areCustomLoadersSupportedForCDS());
    }

    /**
     * Check for CDS support for archived Java heap regions.
     *
     * @return true if CDS provides support for archive Java heap regions in the VM to be tested.
     */
    protected String vmCDSForArchivedJavaHeap() {
        return "" + ("true".equals(vmCDS()) && WB.isJavaHeapArchiveSupported());
    }

    /**
     * @return System page size in bytes.
     */
    protected String vmPageSize() {
        return "" + WB.getVMPageSize();
    }

    /**
     * Check if Graal is used as JIT compiler.
     *
     * @return true if Graal is used as JIT compiler.
     */
    protected String isGraalEnabled() {
        return "" + Compiler.isGraalEnabled();
    }

    /**
     * Check if Compiler1 is present.
     *
     * @return true if Compiler1 is used as JIT compiler, either alone or as part of the tiered system.
     */
    protected String isCompiler1Enabled() {
        return "" + Compiler.isC1Enabled();
    }

    /**
     * Check if Compiler2 is present.
     *
     * @return true if Compiler2 is used as JIT compiler, either alone or as part of the tiered system.
     */
    protected String isCompiler2Enabled() {
        return "" + Compiler.isC2Enabled();
    }

   /**
     * A simple check for docker support
     *
     * @return true if docker is supported in a given environment
     */
    protected String dockerSupport() {
        boolean isSupported = false;
        if (Platform.isLinux()) {
           // currently docker testing is only supported for Linux,
           // on certain platforms

           String arch = System.getProperty("os.arch");

           if (Platform.isX64()) {
              isSupported = true;
           } else if (Platform.isAArch64()) {
              isSupported = true;
           } else if (Platform.isS390x()) {
              isSupported = true;
           } else if (arch.equals("ppc64le")) {
              isSupported = true;
           }
        }

        if (isSupported) {
           try {
              isSupported = checkDockerSupport();
           } catch (Exception e) {
              isSupported = false;
           }
         }

        return "" + isSupported;
    }

    private boolean checkDockerSupport() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(Container.ENGINE_COMMAND, "ps");
        Process p = pb.start();
        p.waitFor(10, TimeUnit.SECONDS);

        return (p.exitValue() == 0);
    }

    /**
     * Checks musl libc.
     *
     * @return true if musl libc is used.
     */
    protected String isMusl() {
        return Boolean.toString(WB.getLibcName().contains("musl"));
    }

    private String implementor() {
        try (InputStream in = new BufferedInputStream(new FileInputStream(
                System.getProperty("java.home") + "/release"))) {
            Properties properties = new Properties();
            properties.load(in);
            String implementorProperty = properties.getProperty("IMPLEMENTOR");
            if (implementorProperty != null) {
                return implementorProperty.replace("\"", "");
            }
            return errorWithMessage("Can't get 'IMPLEMENTOR' property from 'release' file");
        } catch (IOException e) {
            e.printStackTrace();
            return errorWithMessage("Failed to read 'release' file " + e);
        }
    }

    private String jdkContainerized() {
        String isEnabled = System.getenv("TEST_JDK_CONTAINERIZED");
        return "" + "true".equalsIgnoreCase(isEnabled);
    }

    /**
     * Checks if we are in <i>almost</i> out-of-box configuration, i.e. the flags
     * which JVM is started with don't affect its behavior "significantly".
     * {@code TEST_VM_FLAGLESS} enviroment variable can be used to force this
     * method to return true and allow any flags.
     *
     * @return true if there are no JVM flags
     */
    private String isFlagless() {
        boolean result = true;
        if (System.getenv("TEST_VM_FLAGLESS") != null) {
            return "" + result;
        }

        List<String> allFlags = new ArrayList<String>();
        Collections.addAll(allFlags, System.getProperty("test.vm.opts", "").trim().split("\\s+"));
        Collections.addAll(allFlags, System.getProperty("test.java.opts", "").trim().split("\\s+"));

        // check -XX flags
        var ignoredXXFlags = Set.of(
                // added by run-test framework
                "MaxRAMPercentage",
                // added by test environment
                "CreateCoredumpOnCrash"
        );
        result &= allFlags.stream()
                          .filter(s -> s.startsWith("-XX:"))
                          // map to names:
                              // remove -XX:
                              .map(s -> s.substring(4))
                              // remove +/- from bool flags
                              .map(s -> s.charAt(0) == '+' || s.charAt(0) == '-' ? s.substring(1) : s)
                              // remove =.* from others
                              .map(s -> s.contains("=") ? s.substring(0, s.indexOf('=')) : s)
                          // skip known-to-be-there flags
                          .filter(s -> !ignoredXXFlags.contains(s))
                          .findAny()
                          .isEmpty();

        // check -X flags
        var ignoredXFlags = Set.of(
                // default, yet still seen to be explicitly set
                "mixed"
        );
        result &= allFlags.stream()
                          .filter(s -> s.startsWith("-X") && !s.startsWith("-XX:"))
                          // map to names:
                              // remove -X
                              .map(s -> s.substring(2))
                              // remove :.* from flags with values
                              .map(s -> s.contains(":") ? s.substring(0, s.indexOf(':')) : s)
                          // skip known-to-be-there flags
                          .filter(s -> !ignoredXFlags.contains(s))
                          .findAny()
                          .isEmpty();

        return "" + result;
    }

    /**
     * Dumps the map to the file if the file name is given as the property.
     * This functionality could be helpful to know context in the real
     * execution.
     *
     * @param map
     */
    protected static void dump(Map<String, String> map) {
        String dumpFileName = System.getProperty("vmprops.dump");
        if (dumpFileName == null) {
            return;
        }
        List<String> lines = new ArrayList<>();
        map.forEach((k, v) -> lines.add(k + ":" + v));
        try {
            Files.write(Paths.get(dumpFileName), lines,
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to dump properties into '"
                    + dumpFileName + "'", e);
        }
    }

    /**
     * This method is for the testing purpose only.
     *
     * @param args
     */
    public static void main(String args[]) {
        Map<String, String> map = new VMProps().call();
        map.forEach((k, v) -> System.out.println(k + ": '" + v + "'"));
    }
}
