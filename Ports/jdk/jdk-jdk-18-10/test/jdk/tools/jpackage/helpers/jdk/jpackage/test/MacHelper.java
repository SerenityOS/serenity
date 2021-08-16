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
package jdk.jpackage.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import jdk.jpackage.test.Functional.ThrowingConsumer;
import jdk.jpackage.test.Functional.ThrowingSupplier;
import jdk.jpackage.test.PackageTest.PackageHandlers;
import jdk.jpackage.internal.RetryExecutor;
import org.xml.sax.SAXException;
import org.w3c.dom.NodeList;

public class MacHelper {

    public static void withExplodedDmg(JPackageCommand cmd,
            ThrowingConsumer<Path> consumer) {
        cmd.verifyIsOfType(PackageType.MAC_DMG);

        // Explode DMG assuming this can require interaction, thus use `yes`.
        var plist = readPList(Executor.of("sh", "-c",
                String.join(" ", "yes", "|", "/usr/bin/hdiutil", "attach",
                        JPackageCommand.escapeAndJoin(
                                cmd.outputBundle().toString()), "-plist"))
                .dumpOutput()
                .executeAndGetOutput());

        final Path mountPoint = Path.of(plist.queryValue("mount-point"));
        try {
            Path dmgImage = mountPoint.resolve(cmd.name() +
                    (cmd.isRuntime() ? "" : ".app"));
            TKit.trace(String.format("Exploded [%s] in [%s] directory",
                    cmd.outputBundle(), dmgImage));
            ThrowingConsumer.toConsumer(consumer).accept(dmgImage);
        } finally {
            String cmdline[] = {
                "/usr/bin/hdiutil",
                "detach",
                "-verbose",
                mountPoint.toAbsolutePath().toString()};
            // "hdiutil detach" might not work right away due to resource busy error, so
            // repeat detach several times.
            RetryExecutor retryExecutor = new RetryExecutor();
            // Image can get detach even if we got resource busy error, so stop
            // trying to detach it if it is no longer attached.
            retryExecutor.setExecutorInitializer(exec -> {
                if (!Files.exists(mountPoint)) {
                    retryExecutor.abort();
                }
            });
            try {
                // 10 times with 6 second delays.
                retryExecutor.setMaxAttemptsCount(10)
                        .setAttemptTimeoutMillis(6000)
                        .execute(cmdline);
            } catch (IOException ex) {
                if (!retryExecutor.isAborted()) {
                    // Now force to detach if it still attached
                    if (Files.exists(mountPoint)) {
                        Executor.of("/usr/bin/hdiutil", "detach",
                                    "-force", "-verbose")
                                 .addArgument(mountPoint).execute();
                    }
                }
            }
        }
    }

    public static PListWrapper readPListFromAppImage(Path appImage) {
        return readPList(appImage.resolve("Contents/Info.plist"));
    }

    public static PListWrapper readPList(Path path) {
        TKit.assertReadableFileExists(path);
        return ThrowingSupplier.toSupplier(() -> readPList(Files.readAllLines(
                path))).get();
    }

    public static PListWrapper readPList(List<String> lines) {
        return readPList(lines.stream());
    }

    public static PListWrapper readPList(Stream<String> lines) {
        return ThrowingSupplier.toSupplier(() -> new PListWrapper(lines
                // Skip leading lines before xml declaration
                .dropWhile(Pattern.compile("\\s?<\\?xml\\b.+\\?>").asPredicate().negate())
                .collect(Collectors.joining()))).get();
    }

    static PackageHandlers createDmgPackageHandlers() {
        PackageHandlers dmg = new PackageHandlers();

        dmg.installHandler = cmd -> {
            withExplodedDmg(cmd, dmgImage -> {
                Executor.of("sudo", "cp", "-r")
                .addArgument(dmgImage)
                .addArgument(getInstallationDirectory(cmd).getParent())
                .execute();
            });
        };
        dmg.unpackHandler = (cmd, destinationDir) -> {
            Path unpackDir = destinationDir.resolve(
                    TKit.removeRootFromAbsolutePath(
                            getInstallationDirectory(cmd)).getParent());
            try {
                Files.createDirectories(unpackDir);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            withExplodedDmg(cmd, dmgImage -> {
                Executor.of("cp", "-r")
                .addArgument(dmgImage)
                .addArgument(unpackDir)
                .execute();
            });
            return destinationDir;
        };
        dmg.uninstallHandler = cmd -> {
            cmd.verifyIsOfType(PackageType.MAC_DMG);
            Executor.of("sudo", "rm", "-rf")
            .addArgument(cmd.appInstallationDirectory())
            .execute();
        };

        return dmg;
    }

    static PackageHandlers createPkgPackageHandlers() {
        PackageHandlers pkg = new PackageHandlers();

        pkg.installHandler = cmd -> {
            cmd.verifyIsOfType(PackageType.MAC_PKG);
            Executor.of("sudo", "/usr/sbin/installer", "-allowUntrusted", "-pkg")
            .addArgument(cmd.outputBundle())
            .addArguments("-target", "/")
            .execute();
        };
        pkg.unpackHandler = (cmd, destinationDir) -> {
            cmd.verifyIsOfType(PackageType.MAC_PKG);
            Executor.of("pkgutil", "--expand")
            .addArgument(cmd.outputBundle())
            .addArgument(destinationDir.resolve("data")) // We need non-existing folder
            .execute();

            final Path unpackRoot = destinationDir.resolve("unpacked");

            Path installDir = TKit.removeRootFromAbsolutePath(
                    getInstallationDirectory(cmd)).getParent();
            final Path unpackDir = unpackRoot.resolve(installDir);
            try {
                Files.createDirectories(unpackDir);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            Executor.of("tar", "-C")
            .addArgument(unpackDir)
            .addArgument("-xvf")
            .addArgument(Path.of(destinationDir.toString(), "data",
                                 cmd.name() + "-app.pkg", "Payload"))
            .execute();
            return unpackRoot;
        };
        pkg.uninstallHandler = cmd -> {
            cmd.verifyIsOfType(PackageType.MAC_PKG);
            Executor.of("sudo", "rm", "-rf")
            .addArgument(cmd.appInstallationDirectory())
            .execute();
        };

        return pkg;
    }

    static String getBundleName(JPackageCommand cmd) {
        cmd.verifyIsOfType(PackageType.MAC);
        return String.format("%s-%s%s", getPackageName(cmd), cmd.version(),
                cmd.packageType().getSuffix());
    }

    static Path getInstallationDirectory(JPackageCommand cmd) {
        cmd.verifyIsOfType(PackageType.MAC);
        return Path.of(cmd.getArgumentValue("--install-dir", () -> "/Applications"))
                .resolve(cmd.name() + (cmd.isRuntime() ? "" : ".app"));
    }

    private static String getPackageName(JPackageCommand cmd) {
        return cmd.getArgumentValue("--mac-package-name",
                () -> cmd.installerName());
    }

    public static final class PListWrapper {
        public String queryValue(String keyName) {
            XPath xPath = XPathFactory.newInstance().newXPath();
            // Query for the value of <string> element preceding <key> element
            // with value equal to `keyName`
            String query = String.format(
                    "//string[preceding-sibling::key = \"%s\"][1]", keyName);
            return ThrowingSupplier.toSupplier(() -> (String) xPath.evaluate(
                    query, doc, XPathConstants.STRING)).get();
        }

        public Boolean queryBoolValue(String keyName) {
            XPath xPath = XPathFactory.newInstance().newXPath();
            // Query boolean element preceding <key> element
            // with value equal to `keyName`
            String query = String.format(
                    "name(//*[preceding-sibling::key = \"%s\"])", keyName);
            String value = ThrowingSupplier.toSupplier(() -> (String) xPath.evaluate(
                    query, doc, XPathConstants.STRING)).get();
            return Boolean.valueOf(value);
        }

        public List<String> queryArrayValue(String keyName) {
            XPath xPath = XPathFactory.newInstance().newXPath();
            // Query string array preceding <key> element with value equal to `keyName`
            String query = String.format(
                    "//array[preceding-sibling::key = \"%s\"]", keyName);
            NodeList list = ThrowingSupplier.toSupplier(() -> (NodeList) xPath.evaluate(
                    query, doc, XPathConstants.NODESET)).get();
            if (list.getLength() != 1) {
                throw new RuntimeException(
                        String.format("Unable to find <array> element for key = \"%s\"]",
                                keyName));
            }

            NodeList childList = list.item(0).getChildNodes();
            List<String> values = new ArrayList(childList.getLength());
            for (int i = 0; i < childList.getLength(); i++) {
                if (childList.item(i).getNodeName().equals("string")) {
                    values.add(childList.item(i).getTextContent());
                }
            }
            return values;
        }

        PListWrapper(String xml) throws ParserConfigurationException,
                SAXException, IOException {
            doc = createDocumentBuilder().parse(new ByteArrayInputStream(
                    xml.getBytes(StandardCharsets.UTF_8)));
        }

        private static DocumentBuilder createDocumentBuilder() throws
                ParserConfigurationException {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultInstance();
            dbf.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);
            return dbf.newDocumentBuilder();
        }

        private final org.w3c.dom.Document doc;
    }

    static final Set<Path> CRITICAL_RUNTIME_FILES = Set.of(Path.of(
            "Contents/Home/lib/server/libjvm.dylib"));

}
