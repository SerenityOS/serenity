/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Platform {
    public  static final String vmName      = privilegedGetProperty("java.vm.name");
    public  static final String vmInfo      = privilegedGetProperty("java.vm.info");
    private static final String osVersion   = privilegedGetProperty("os.version");
    private static       int osVersionMajor = -1;
    private static       int osVersionMinor = -1;
    private static final String osName      = privilegedGetProperty("os.name");
    private static final String dataModel   = privilegedGetProperty("sun.arch.data.model");
    private static final String vmVersion   = privilegedGetProperty("java.vm.version");
    private static final String jdkDebug    = privilegedGetProperty("jdk.debug");
    private static final String osArch      = privilegedGetProperty("os.arch");
    private static final String userName    = privilegedGetProperty("user.name");
    private static final String compiler    = privilegedGetProperty("sun.management.compiler");
    private static final String testJdk     = privilegedGetProperty("test.jdk");

    @SuppressWarnings("removal")
    private static String privilegedGetProperty(String key) {
        return AccessController.doPrivileged((
                PrivilegedAction<String>) () -> System.getProperty(key));
    }

    public static boolean isClient() {
        return vmName.endsWith(" Client VM");
    }

    public static boolean isServer() {
        return vmName.endsWith(" Server VM");
    }

    public static boolean isZero() {
        return vmName.endsWith(" Zero VM");
    }

    public static boolean isMinimal() {
        return vmName.endsWith(" Minimal VM");
    }

    public static boolean isEmbedded() {
        return vmName.contains("Embedded");
    }

    public static boolean isEmulatedClient() {
        return vmInfo.contains(" emulated-client");
    }

    public static boolean isTieredSupported() {
        return compiler.contains("Tiered Compilers");
    }

    public static boolean isInt() {
        return vmInfo.contains("interpreted");
    }

    public static boolean isMixed() {
        return vmInfo.contains("mixed");
    }

    public static boolean isComp() {
        return vmInfo.contains("compiled");
    }

    public static boolean is32bit() {
        return dataModel.equals("32");
    }

    public static boolean is64bit() {
        return dataModel.equals("64");
    }

    public static boolean isAix() {
        return isOs("aix");
    }

    public static boolean isLinux() {
        return isOs("linux");
    }

    public static boolean isBusybox(String tool) {
        try {
            Path toolpath = Paths.get(tool);
            return !isWindows()
                    && Files.isSymbolicLink(toolpath)
                    && Paths.get("/bin/busybox")
                        .equals(Files.readSymbolicLink(toolpath));
        } catch (IOException ignore) {
            return false;
        }
    }

    public static boolean isOSX() {
        return isOs("mac");
    }

    public static boolean isWindows() {
        return isOs("win");
    }

    private static boolean isOs(String osname) {
        return osName.toLowerCase().startsWith(osname.toLowerCase());
    }

    public static String getOsName() {
        return osName;
    }

    // Os version support.
    private static void init_version() {
        String[] osVersionTokens = osVersion.split("\\.");
        try {
            if (osVersionTokens.length > 0) {
                osVersionMajor = Integer.parseInt(osVersionTokens[0]);
                if (osVersionTokens.length > 1) {
                    osVersionMinor = Integer.parseInt(osVersionTokens[1]);
                }
            }
        } catch (NumberFormatException e) {
            osVersionMajor = osVersionMinor = 0;
        }
    }

    public static String getOsVersion() {
        return osVersion;
    }

    // Returns major version number from os.version system property.
    // E.g. 3 on SLES 11.3 (for the linux kernel version).
    public static int getOsVersionMajor() {
        if (osVersionMajor == -1) init_version();
        return osVersionMajor;
    }

    // Returns minor version number from os.version system property.
    // E.g. 0 on SLES 11.3 (for the linux kernel version).
    public static int getOsVersionMinor() {
        if (osVersionMinor == -1) init_version();
        return osVersionMinor;
    }

    public static boolean isDebugBuild() {
        return (jdkDebug.toLowerCase().contains("debug"));
    }

    public static boolean isSlowDebugBuild() {
        return (jdkDebug.toLowerCase().equals("slowdebug"));
    }

    public static boolean isFastDebugBuild() {
        return (jdkDebug.toLowerCase().equals("fastdebug"));
    }

    public static String getVMVersion() {
        return vmVersion;
    }

    public static boolean isAArch64() {
        return isArch("aarch64");
    }

    public static boolean isARM() {
        return isArch("arm.*");
    }

    public static boolean isPPC() {
        return isArch("ppc.*");
    }

    // Returns true for IBM z System running linux.
    public static boolean isS390x() {
        return isArch("s390.*") || isArch("s/390.*") || isArch("zArch_64");
    }

    public static boolean isX64() {
        // On OSX it's 'x86_64' and on other (Linux and Windows) platforms it's 'amd64'
        return isArch("(amd64)|(x86_64)");
    }

    public static boolean isX86() {
        // On Linux it's 'i386', Windows 'x86' without '_64' suffix.
        return isArch("(i386)|(x86(?!_64))");
    }

    public static String getOsArch() {
        return osArch;
    }

    public static boolean isRoot() {
        return userName.equals("root");
    }

    /**
     * Return a boolean for whether SA and jhsdb are ported/available
     * on this platform.
     */
    public static boolean hasSA() {
        if (isZero()) {
            return false; // SA is not enabled.
        }
        if (isAix()) {
            return false; // SA not implemented.
        } else if (isLinux()) {
            if (isS390x() || isARM()) {
                return false; // SA not implemented.
            }
        }
        // Other platforms expected to work:
        return true;
    }

    /**
     * Return true if the test JDK is signed, otherwise false. Only valid on OSX.
     */
    public static boolean isSignedOSX() throws IOException {
        // We only care about signed binaries for 10.14 and later (actually 10.14.5, but
        // for simplicity we'll also include earlier 10.14 versions).
        if (getOsVersionMajor() == 10 && getOsVersionMinor() < 14) {
            return false; // assume not signed
        }

        // Find the path to the java binary.
        String jdkPath = System.getProperty("java.home");
        Path javaPath = Paths.get(jdkPath + "/bin/java");
        String javaFileName = javaPath.toAbsolutePath().toString();
        if (Files.notExists(javaPath)) {
            throw new FileNotFoundException("Could not find file " + javaFileName);
        }

        // Run codesign on the java binary.
        ProcessBuilder pb = new ProcessBuilder("codesign", "-d", "-v", javaFileName);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        Process codesignProcess = pb.start();
        try {
            if (codesignProcess.waitFor(10, TimeUnit.SECONDS) == false) {
                System.err.println("Timed out waiting for the codesign process to complete. Assuming not signed.");
                codesignProcess.destroyForcibly();
                return false; // assume not signed
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Check codesign result to see if java binary is signed. Here are the
        // exit code meanings:
        //    0: signed
        //    1: not signed
        //    2: invalid arguments
        //    3: only has meaning with the -R argument.
        // So we should always get 0 or 1 as an exit value.
        if (codesignProcess.exitValue() == 0) {
            System.out.println("Target JDK is signed. Some tests may be skipped.");
            return true; // signed
        } else if (codesignProcess.exitValue() == 1) {
            System.out.println("Target JDK is not signed.");
            return false; // not signed
        } else {
            System.err.println("Executing codesign failed. Assuming unsigned: " +
                               codesignProcess.exitValue());
            return false; // not signed
        }
    }

    private static boolean isArch(String archnameRE) {
        return Pattern.compile(archnameRE, Pattern.CASE_INSENSITIVE)
                      .matcher(osArch)
                      .matches();
    }

    /**
     * Returns file extension of shared library, e.g. "so" on linux, "dll" on windows.
     * @return file extension
     */
    public static String sharedLibraryExt() {
        if (isWindows()) {
            return "dll";
        } else if (isOSX()) {
            return "dylib";
        } else {
            return "so";
        }
    }

    /*
     * Returns name of system variable containing paths to shared native libraries.
     */
    public static String sharedLibraryPathVariableName() {
        if (isWindows()) {
            return "PATH";
        } else if (isOSX()) {
            return "DYLD_LIBRARY_PATH";
        } else if (isAix()) {
            return "LIBPATH";
        } else {
            return "LD_LIBRARY_PATH";
        }
    }

    /**
     * Returns absolute path to directory containing shared libraries in the tested JDK.
     */
    public static Path libDir() {
        return libDir(Paths.get(testJdk)).toAbsolutePath();
    }

    /**
     * Resolves a given path, to a JDK image, to the directory containing shared libraries.
     *
     * @param image the path to a JDK image
     * @return the resolved path to the directory containing shared libraries
     */
    public static Path libDir(Path image) {
        if (Platform.isWindows()) {
            return image.resolve("bin");
        } else {
            return image.resolve("lib");
        }
    }

    /**
     * Returns absolute path to directory containing JVM shared library.
     */
    public static Path jvmLibDir() {
        return libDir().resolve(variant());
    }

    private static String variant() {
        if (Platform.isServer()) {
            return "server";
        } else if (Platform.isClient()) {
            return "client";
        } else if (Platform.isMinimal()) {
            return "minimal";
        } else {
            throw new Error("TESTBUG: unsupported vm variant");
        }
    }


    public static boolean isDefaultCDSArchiveSupported() {
        return (is64bit()  &&
                isServer() &&
                (isLinux()   ||
                 isOSX()     ||
                 isWindows()) &&
                !isZero()    &&
                !isMinimal() &&
                !isARM());
    }

    /*
     * This should match the #if condition in ClassListParser::load_class_from_source().
     */
    public static boolean areCustomLoadersSupportedForCDS() {
        return (is64bit() && (isLinux() || isOSX()));
    }
}
