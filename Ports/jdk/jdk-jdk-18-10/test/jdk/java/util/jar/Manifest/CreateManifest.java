/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7148584
 * @summary Jar tools fails to generate manifest correctly when boundary condition hit
 * @modules jdk.jartool/sun.tools.jar
 * @compile -XDignore.symbol.file=true CreateManifest.java
 * @run main CreateManifest
 */

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.*;

public class CreateManifest {

public static void main(String arg[]) throws Exception {

    String jarFileName = "test.jar";
    String ManifestName = "MANIFEST.MF";

    // create the MANIFEST.MF file
    Files.write(Paths.get(ManifestName), FILE_CONTENTS.getBytes());

    String [] args = new String [] { "cvfm", jarFileName, ManifestName};
    sun.tools.jar.Main jartool =
            new sun.tools.jar.Main(System.out, System.err, "jar");
    jartool.run(args);

    try (JarFile jf = new JarFile(jarFileName)) {
        Manifest m = jf.getManifest();
        String result = m.getMainAttributes().getValue("Class-path");
        if (result == null)
            throw new RuntimeException("Failed to add Class-path attribute to manifest");
    } finally {
        Files.deleteIfExists(Paths.get(jarFileName));
        Files.deleteIfExists(Paths.get(ManifestName));
    }

}

private static final String FILE_CONTENTS =
 "Class-path: \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-testconsole-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-testconsole-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-bmp-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-bmp-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-host-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-host-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-agent-patching-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-agent-patching-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-connector-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-connector-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-discovery-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-gccompliance-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-mos-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-mos-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-security-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-security-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-topology-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-topology-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-event-pojo.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-event-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-event-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-mext-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-mext-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-discovery-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-discovery-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ecm-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ecm-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ecm-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-event-console-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-event-console-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-event-rules-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-event-rules-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-gccompliance-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ip-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ip-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-probanalysis-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-probanalysis-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-swlib-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-installmediacomponent-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-uifwk-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-uifwk-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-discovery-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-gccompliance-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-bmp-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-host-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-agent-patching-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-connector-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-mos-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-event-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-discovery-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-gccompliance-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ip-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-probanalysis-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-testconsole-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-uifwk-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-mext-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-security-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-agentpush-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-agentpush-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-agentpush-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-selfupdate-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-selfupdate-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-selfupdate-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-agentpush-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-groups-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-groups-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-groups-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-topology-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-jobs-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-jobs-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-jobs-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-templ-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-templ-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-templ-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-metricalertserrors-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-metricalertserrors-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-metricalertserrors-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-metrics-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-metrics-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-metrics-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-tc-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-tc-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-tc-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-agentmgmt-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-agentmgmt-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-agentmgmt-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-gcharvester-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-gcharvester-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-gcharvester-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-patching-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-patching-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-patching-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ohinv-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ohinv-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ohinv-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ohagent-pojo.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ohcoherence-pojo.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ohjrockit-pojo.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-extensibility-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-mpcustom-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-selfmonitor-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ocheck-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-udmmig-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-multioms-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-postupgrade-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-postupgrade-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-postupgrade-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ppc-pojo.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ppc-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ppc-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ppc-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ppc-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-mextjmx-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-mextjmx-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-mextjmx-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ocheck-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-services-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-services-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-services-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-eventmobile-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-uifwkmobile-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-logmgmt-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-omsproperties-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-ohel-pojo.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-agentupgrade-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-lm-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-lm-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-core-lm-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-samples-regiontest-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-samples-uipatterns-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-samples-uipatterns-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-samples-uipatterns-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-samples-uielements-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-samples-uielements-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-samples-sandbox-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-samples-sandbox-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-samples-sdkcore-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-samples-sdkcore-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-samples-sdkcore-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-samples-core-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-samples-core-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-samples-core-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-adfext-bc-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-aslm-services-public-pojo.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-avail-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-charge-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-config-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-connect-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-db-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-discovery-public-entity.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-discovery-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-event-console-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-event-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-event-rules-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-extens-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-filebrowser-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-filebrowser-public-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-gccompliance-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-gccompliance-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-gccompliance-public-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-ip-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-job-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-me-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-metric-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-ecm-public-pojo.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-ecm-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-ecm-public-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-ecm-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-paf-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-security-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-swlib-public-pojo.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-swlib-public-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-templ-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-uifwk-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-uifwk-public-pojo.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-uifwk-public-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-bmp-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-bmp-public-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-bmp-public-entity.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-agent-patching-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-agent-patching-public-pojo.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-mext-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-mext-public-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-mext-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-testconsole-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-testconsole-public-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-testconsole-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-mos-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-mos-public-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-mos-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-topology-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-topology-public-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-regions-uimodel.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-regions-public-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-event-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-uifwk-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-adfext-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-agentpatching-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-avail-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-bmp-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-charge-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-config-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-connect-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-db-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-discovery-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-ecm-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-extens-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-gccompliance-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-ip-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-job-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-me-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-metric-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-paf-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-regions-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-security-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-swlib-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-templ-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-groups-public-pojo.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-groups-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-topology-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-resources-public-pojo.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-clonecomponents-public-pojo.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-clonecomponents-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-clonecomponents-public-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-clonecomponents-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-patching-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-patching-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-ohinv-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-ohinv-test.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-ppc-public-pojo.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-ppc-public-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-agentpush-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-uifwkmobile-public-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-lm-public-model.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-lm-public-ui.jar \n" +
 " /ade/dtsao_re/oracle/emcore//lib/em-sdkcore-lm-test.jar \n";
}
