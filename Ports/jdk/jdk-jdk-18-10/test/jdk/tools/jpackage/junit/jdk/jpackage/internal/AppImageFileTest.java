/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jpackage.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class AppImageFileTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testIdentity() throws IOException {
        Map<String, ? super Object> params = new LinkedHashMap<>();
        params.put(Arguments.CLIOptions.NAME.getId(), "Foo");
        params.put(Arguments.CLIOptions.VERSION.getId(), "2.3");
        params.put(Arguments.CLIOptions.DESCRIPTION.getId(), "Duck is the King");
        AppImageFile aif = create(params);

        Assert.assertEquals("Foo", aif.getLauncherName());
    }

    @Test
    public void testInvalidCommandLine() throws IOException {
        // Just make sure AppImageFile will tolerate jpackage params that would
        // never create app image at both load/save phases.
        // People would edit this file just because they can.
        // We should be ready to handle curious minds.
        Map<String, ? super Object> params = new LinkedHashMap<>();
        params.put("invalidParamName", "randomStringValue");
        params.put(Arguments.CLIOptions.APPCLASS.getId(), "TestClass");
        params.put(Arguments.CLIOptions.MAIN_JAR.getId(), "test.jar");
        create(params);

        params = new LinkedHashMap<>();
        params.put(Arguments.CLIOptions.NAME.getId(), "foo");
        params.put(Arguments.CLIOptions.VERSION.getId(), "");
        create(params);
    }

    @Test
    public void testInavlidXml() throws IOException {
        assertInvalid(createFromXml("<foo/>"));
        assertInvalid(createFromXml("<jpackage-state/>"));
        assertInvalid(createFromXml(
                "<jpackage-state>",
                    "<main-launcher></main-launcher>",
                "</jpackage-state>"));
        assertInvalid(createFromXml(
                "<jpackage-state>",
                    "<launcher>A</launcher>",
                    "<launcher>B</launcher>",
                "</jpackage-state>"));
    }

    @Test
    public void testValidXml() throws IOException {
        Assert.assertEquals("Foo", (createFromXml(
                "<jpackage-state>",
                    "<main-launcher>Foo</main-launcher>",
                "</jpackage-state>")).getLauncherName());

        Assert.assertEquals("Boo", (createFromXml(
                "<jpackage-state>",
                    "<main-launcher>Boo</main-launcher>",
                    "<main-launcher>Bar</main-launcher>",
                "</jpackage-state>")).getLauncherName());

        var file = createFromXml(
                "<jpackage-state>",
                    "<main-launcher>Foo</main-launcher>",
                    "<launcher></launcher>",
                "</jpackage-state>");
        Assert.assertEquals("Foo", file.getLauncherName());

        Assert.assertEquals(0, file.getAddLaunchers().size());
    }

    @Test
    public void testMainLauncherName() throws IOException {
        Map<String, ? super Object> params = new LinkedHashMap<>();
        params.put("name", "Foo");
        params.put("description", "Duck App Description");
        AppImageFile aif = create(params);

        Assert.assertEquals("Foo", aif.getLauncherName());
    }

    @Test
    public void testAddLaunchers() throws IOException {
        Map<String, ? super Object> params = new LinkedHashMap<>();
        List<Map<String, ? super Object>> launchersAsMap = new ArrayList<>();

        Map<String, ? super Object> addLauncher2Params = new LinkedHashMap();
        addLauncher2Params.put("name", "Launcher2Name");
        launchersAsMap.add(addLauncher2Params);

        Map<String, ? super Object> addLauncher3Params = new LinkedHashMap();
        addLauncher3Params.put("name", "Launcher3Name");
        launchersAsMap.add(addLauncher3Params);

        params.put("name", "Duke App");
        params.put("description", "Duke App Description");
        params.put("add-launcher", launchersAsMap);
        AppImageFile aif = create(params);

        List<AppImageFile.LauncherInfo> addLaunchers = aif.getAddLaunchers();
        Assert.assertEquals(2, addLaunchers.size());
        List<String> names = new ArrayList<String>();
        names.add(addLaunchers.get(0).getName());
        names.add(addLaunchers.get(1).getName());

        Assert.assertTrue(names.contains("Launcher2Name"));
        Assert.assertTrue(names.contains("Launcher3Name"));

    }

    private AppImageFile create(Map<String, Object> params) throws IOException {
        AppImageFile.save(tempFolder.getRoot().toPath(), params);
        return AppImageFile.load(tempFolder.getRoot().toPath());
    }

    private void assertInvalid(AppImageFile file) {
        Assert.assertNull(file.getLauncherName());
        Assert.assertNull(file.getAddLaunchers());
    }

    private AppImageFile createFromXml(String... xmlData) throws IOException {
        Path directory = tempFolder.getRoot().toPath();
        Path path = AppImageFile.getPathInAppImage(directory);
        path.toFile().mkdirs();
        Files.delete(path);

        ArrayList<String> data = new ArrayList();
        data.add("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");
        data.addAll(List.of(xmlData));

        Files.write(path, data, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

        AppImageFile image = AppImageFile.load(directory);
        return image;
    }

}
