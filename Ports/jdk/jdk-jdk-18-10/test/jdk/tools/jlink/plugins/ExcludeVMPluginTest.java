/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

 /*
 * @test
 * @summary Test exclude VM plugin
 * @author Jean-Francois Denise
 * @modules jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jlink.internal.plugins
 *          jdk.jlink/jdk.tools.jlink.plugin
 * @run main ExcludeVMPluginTest
 */
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import jdk.tools.jlink.internal.ResourcePoolManager;

import jdk.tools.jlink.internal.plugins.ExcludeVMPlugin;
import jdk.tools.jlink.plugin.Plugin;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolEntry;

public class ExcludeVMPluginTest {

    private static final String TAG = "# orig in test\n";

    private static final String[] ARCHITECTURES = {"/", "/amd64/", "/i386/", "/arm/",
        "/aarch64/", "/toto/"};

    private static final String[] CLIENT = {"client/" + jvmlib(),};
    private static final String[] SERVER = {"server/" + jvmlib()};
    private static final String[] MINIMAL = {"minimal/" + jvmlib()};
    private static final String[] ALL = {CLIENT[0], SERVER[0], MINIMAL[0]};
    private static final String JVM_CFG_ALL = TAG + "-server KNOWN\n-client KNOWN\n-minimal KNOWN\n";
    private static final String JVM_CFG_CLIENT = TAG + "-client KNOWN\n";
    private static final String JVM_CFG_SERVER = TAG + "-server KNOWN\n";
    private static final String JVM_CFG_SERVER_ALIAS_OTHERS = TAG + "-server KNOWN\n-client ALIASED_TO -server\n-minimal ALIASED_TO -server\n";
    private static final String JVM_CFG_CLIENT_ALIAS_OTHERS = TAG + "-client KNOWN\n-server ALIASED_TO -client\n-minimal ALIASED_TO -client\n";
    private static final String JVM_CFG_MINIMAL_ALIAS_OTHERS = TAG + "-minimal KNOWN\n-server ALIASED_TO -minimal\n-client ALIASED_TO -minimal\n";
    private static final String JVM_CFG_MINIMAL = TAG + "-minimal KNOWN\n";

    public static void main(String[] args) throws Exception {
        new ExcludeVMPluginTest().test();
    }

    public void test() throws Exception {
        boolean failed = false;

        try {
            checkVM("toto", ALL, JVM_CFG_ALL, ALL, JVM_CFG_ALL);
            failed = true;
            throw new Exception("Should have failed");
        } catch (Exception ex) {
            if (failed) {
                throw ex;
            }
        }

        checkVM("all", ALL, JVM_CFG_ALL, ALL, JVM_CFG_ALL);
        checkVM("all", CLIENT, JVM_CFG_CLIENT, CLIENT, JVM_CFG_CLIENT);
        checkVM("all", SERVER, JVM_CFG_SERVER, SERVER, JVM_CFG_SERVER);
        checkVM("all", MINIMAL, JVM_CFG_MINIMAL, MINIMAL, JVM_CFG_MINIMAL);

        checkVM("server", ALL, JVM_CFG_ALL, SERVER, JVM_CFG_SERVER_ALIAS_OTHERS);
        checkVM("server", SERVER, JVM_CFG_SERVER, SERVER, JVM_CFG_SERVER);
        try {
            checkVM("server", CLIENT, JVM_CFG_CLIENT, SERVER, JVM_CFG_SERVER);
            failed = true;
            throw new Exception("Should have failed");
        } catch (Exception ex) {
            if (failed) {
                throw ex;
            }
        }
        try {
            checkVM("server", MINIMAL, JVM_CFG_MINIMAL, SERVER, JVM_CFG_SERVER);
            failed = true;
            throw new Exception("Should have failed");
        } catch (Exception ex) {
            if (failed) {
                throw ex;
            }
        }

        checkVM("client", ALL, JVM_CFG_ALL, CLIENT, JVM_CFG_CLIENT_ALIAS_OTHERS);
        checkVM("client", CLIENT, JVM_CFG_CLIENT, CLIENT, JVM_CFG_CLIENT);
        try {
            checkVM("client", SERVER, JVM_CFG_SERVER, CLIENT, JVM_CFG_CLIENT);
            failed = true;
            throw new Exception("Should have failed");
        } catch (Exception ex) {
            if (failed) {
                throw ex;
            }
        }
        try {
            checkVM("client", MINIMAL, JVM_CFG_MINIMAL, CLIENT, JVM_CFG_CLIENT);
            failed = true;
            throw new Exception("Should have failed");
        } catch (Exception ex) {
            if (failed) {
                throw ex;
            }
        }

        checkVM("minimal", ALL, JVM_CFG_ALL, MINIMAL, JVM_CFG_MINIMAL_ALIAS_OTHERS);
        checkVM("minimal", MINIMAL, JVM_CFG_MINIMAL, MINIMAL, JVM_CFG_MINIMAL);
        try {
            checkVM("minimal", SERVER, JVM_CFG_SERVER, MINIMAL, JVM_CFG_MINIMAL);
            failed = true;
            throw new Exception("Should have failed");
        } catch (Exception ex) {
            if (failed) {
                throw ex;
            }
        }
        try {
            checkVM("minimal", CLIENT, JVM_CFG_CLIENT, MINIMAL, JVM_CFG_MINIMAL);
            failed = true;
            throw new Exception("Should have failed");
        } catch (Exception ex) {
            if (failed) {
                throw ex;
            }
        }

    }

    public void checkVM(String vm, String[] input, String jvmcfg, String[] expectedOutput, String expectdJvmCfg) throws Exception {

        for (String arch : ARCHITECTURES) {
            String[] winput = new String[input.length];
            String[] woutput = new String[expectedOutput.length];
            for (int i = 0; i < input.length; i++) {
                winput[i] = "/java.base/lib" + arch + input[i];
            }
            for (int i = 0; i < expectedOutput.length; i++) {
                woutput[i] = "/java.base/lib" + arch + expectedOutput[i];
            }
            doCheckVM(vm, winput, jvmcfg, woutput, expectdJvmCfg);
        }
    }

    private void doCheckVM(String vm, String[] input, String jvmcfg, String[] expectedOutput, String expectdJvmCfg) throws Exception {
        // Create a pool with jvm.cfg and the input paths.
        byte[] jvmcfgContent = jvmcfg.getBytes();
        ResourcePoolManager poolMgr = new ResourcePoolManager();
        poolMgr.add(
            ResourcePoolEntry.create("/java.base/lib/jvm.cfg",
                ResourcePoolEntry.Type.NATIVE_LIB, jvmcfgContent));

        // java.base/module-info.class is used by exclude vm plugin
        // to get current osName(). We read it from jrt-fs and add a
        // ResourcePoolEntry
        poolMgr.add(
            ResourcePoolEntry.create("/java.base/module-info.class",
                ResourcePoolEntry.Type.CLASS_OR_RESOURCE, getJavaBaseModuleInfo()));
        for (String in : input) {
            poolMgr.add(ResourcePoolEntry.create(in,
                    ResourcePoolEntry.Type.NATIVE_LIB, new byte[0]));
        }
        ResourcePoolManager outMgr = new ResourcePoolManager();

        Plugin p = new ExcludeVMPlugin();
        Map<String, String> config = new HashMap<>();
        if (vm != null) {
            config.put(p.getName(), vm);
        }
        p.configure(config);
        ResourcePool out = p.transform(poolMgr.resourcePool(), outMgr.resourcePoolBuilder());

        String newContent = new String(out.findEntry("/java.base/lib/jvm.cfg").get().contentBytes());

        if (!expectdJvmCfg.equals(newContent)) {
            throw new Exception("Got content " + newContent + " expected " + expectdJvmCfg);
        }

        // Apart from native resources, we should find jvm.cfg and
        // java.base/module-info.class. So, we add 2 here to the
        // expected count!
        if (out.entryCount() != (expectedOutput.length + 2)) {
            out.entries().forEach(m -> {
                System.err.println(m.path());
            });
            throw new Exception("Invalid output size " + out.entryCount() + " expected " + (expectedOutput.length + 2));
        }

        out.entries().forEach(md -> {
            if (md.path().equals("/java.base/lib/jvm.cfg") ||
                md.path().equals("/java.base/module-info.class")) {
                return;
            }
            boolean contained = false;
            for (String o : expectedOutput) {
                if (md.path().equals(o)) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                throw new RuntimeException(md.path() + " not expected");
            }
        });
    }

    // read java.base/module-info.class from jrt-fs
    private static Path getJavaBaseModuleInfo() {
        return Paths.get(URI.create("jrt:/java.base/module-info.class"));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    private static boolean isMac() {
        return System.getProperty("os.name").startsWith("Mac OS");
    }

    private static String jvmlib() {
        String lib = "libjvm.so";
        if (isWindows()) {
            lib = "jvm.dll";
        } else if (isMac()) {
            lib = "libjvm.dylib";
        }
        return lib;
    }
}
