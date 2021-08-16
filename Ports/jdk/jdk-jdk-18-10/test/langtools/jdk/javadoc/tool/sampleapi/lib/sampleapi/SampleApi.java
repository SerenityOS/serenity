/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package sampleapi;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static java.util.stream.Collectors.toList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sampleapi.generator.ModuleGenerator;

import sampleapi.generator.PackageGenerator;

public class SampleApi {

    private final Context ctx;
    private final List<ModuleGenerator> modules = new ArrayList<>();

    public SampleApi() {
        JavacTool jt = JavacTool.create();
        JavacTask task = jt.getTask(null, null, null, null, null, null);
        ctx = ((JavacTaskImpl) task).getContext();
    }

    public static SampleApi load(Path resDir)
            throws ParserConfigurationException, IOException, SAXException {
        SampleApi result = new SampleApi();
        System.out.println("Loading resources from " + resDir);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Files.list(resDir)
                .peek(f -> System.out.println(f.getFileName()))
                .filter(f -> f.getFileName().toString().endsWith(".xml"))
                .peek(f -> System.out.println(f.getFileName()))
                .forEach(resFile -> {
                    try (InputStream is = Files.newInputStream(resFile)) {
                        Document document = builder.parse(is);
                        NodeList moduleElements = document.getElementsByTagName("module");
                        for (int i = 0; i < moduleElements.getLength(); i++) {
                            result.modules.add(ModuleGenerator
                                    .load((Element) moduleElements.item(i)));
                        }
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    } catch (SAXException ex) {
                        throw new RuntimeException(ex);
                    }
                });
        return result;
    }

    public Context getContext() {
        return ctx;
    }

    public List<ModuleGenerator> getModules() {
        return modules;
    }


    public void generate(Path outDir) {
        //resolveIDs(modules);
        modules.forEach(m -> {
            try {
                m.generate(outDir, this);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
    }

    public void generate(String dir)
            throws ParserConfigurationException, IOException, SAXException {
        generate(Paths.get(dir));
    }

    public ModuleGenerator moduleById(String id) {
        String real_id = getId(id);
        return modules.stream()
                            .filter(m -> m.id.equals(real_id))
                            .findAny().orElseThrow(() -> new IllegalStateException("No module with id: " + real_id));
    }

    public PackageGenerator packageById(String id) {
        String real_id = getId(id);
        return modules.stream()
                .flatMap(m -> m.packages.stream())
                .filter(p -> p.id.equals(real_id)).findAny()
                .orElseThrow(() -> new IllegalStateException("No package with id: " + real_id));
    }

    public String classById(String id) {
        String real_id = getId(id);
        return modules.stream()
                .flatMap(m -> m.packages.stream())
                .peek(p -> System.out.println(p.packageName + " " + p.idBases.size()))
                .flatMap(p -> p.idBases.entrySet().stream()
                    .filter(e -> e.getKey().equals(real_id))
                    .map(e -> p.packageName + "." + e.getValue().name.toString())
                    .peek(System.out::println))
                .findAny().orElseThrow(() -> new IllegalStateException("No class with id: " + id));
    }

    public boolean isId(String name) {
        return name.startsWith("$");
    }

    public boolean isIdEqual(String name, String id) {
        return isId(name) && getId(name).equals(id);
    }

    public String getId(String name) {
        if(!isId(name)) {
            throw new IllegalStateException("Not an id: " + name);
        }
        return name.substring(1);
    }
}
