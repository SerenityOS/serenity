/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

import static jdk.jpackage.internal.StandardBundlerParam.VERSION;
import static jdk.jpackage.internal.StandardBundlerParam.ADD_LAUNCHERS;
import static jdk.jpackage.internal.StandardBundlerParam.APP_NAME;
import static jdk.jpackage.internal.StandardBundlerParam.SHORTCUT_HINT;
import static jdk.jpackage.internal.StandardBundlerParam.MENU_HINT;
import static jdk.jpackage.internal.StandardBundlerParam.SIGN_BUNDLE;

public class AppImageFile {

    // These values will be loaded from AppImage xml file.
    private final String creatorVersion;
    private final String creatorPlatform;
    private final String launcherName;
    private final List<LauncherInfo> addLauncherInfos;
    private final boolean signed;

    private static final String FILENAME = ".jpackage.xml";

    private static final Map<Platform, String> PLATFORM_LABELS = Map.of(
            Platform.LINUX, "linux", Platform.WINDOWS, "windows", Platform.MAC,
            "macOS");


    private AppImageFile() {
        this(null, null, null, null, null);
    }

    private AppImageFile(String launcherName, List<LauncherInfo> launcherInfos,
            String creatorVersion, String creatorPlatform, String signedStr) {
        this.launcherName = launcherName;
        this.addLauncherInfos = launcherInfos;
        this.creatorVersion = creatorVersion;
        this.creatorPlatform = creatorPlatform;
        this.signed = "true".equals(signedStr);
    }

    /**
     * Returns list of additional launchers configured for the application.
     * Each item in the list is not null or empty string.
     * Returns empty list for application without additional launchers.
     */
    List<LauncherInfo> getAddLaunchers() {
        return addLauncherInfos;
    }

    /**
     * Returns main application launcher name. Never returns null or empty value.
     */
    String getLauncherName() {
        return launcherName;
    }

    boolean isSigned() {
        return signed;
    }

    void verifyCompatible() throws ConfigException {
        // Just do nothing for now.
    }

    /**
     * Returns path to application image info file.
     * @param appImageDir - path to application image
     */
    public static Path getPathInAppImage(Path appImageDir) {
        return ApplicationLayout.platformAppImage()
                .resolveAt(appImageDir)
                .appDirectory()
                .resolve(FILENAME);
    }

    /**
     * Saves file with application image info in application image.
     * @param appImageDir - path to application image
     * @throws IOException
     */
    static void save(Path appImageDir, Map<String, Object> params)
            throws IOException {
        IOUtils.createXml(getPathInAppImage(appImageDir), xml -> {
            xml.writeStartElement("jpackage-state");
            xml.writeAttribute("version", getVersion());
            xml.writeAttribute("platform", getPlatform());

            xml.writeStartElement("app-version");
            xml.writeCharacters(VERSION.fetchFrom(params));
            xml.writeEndElement();

            xml.writeStartElement("main-launcher");
            xml.writeCharacters(APP_NAME.fetchFrom(params));
            xml.writeEndElement();

            xml.writeStartElement("signed");
            xml.writeCharacters(SIGN_BUNDLE.fetchFrom(params).toString());
            xml.writeEndElement();

            List<Map<String, ? super Object>> addLaunchers =
                ADD_LAUNCHERS.fetchFrom(params);

            for (int i = 0; i < addLaunchers.size(); i++) {
                Map<String, ? super Object> sl = addLaunchers.get(i);
                xml.writeStartElement("add-launcher");
                xml.writeAttribute("name", APP_NAME.fetchFrom(sl));
                xml.writeAttribute("shortcut",
                        SHORTCUT_HINT.fetchFrom(sl).toString());
                xml.writeAttribute("menu", MENU_HINT.fetchFrom(sl).toString());
                xml.writeEndElement();
            }
        });
    }

    /**
     * Loads application image info from application image.
     * @param appImageDir - path to application image
     * @return valid info about application image or null
     * @throws IOException
     */
    static AppImageFile load(Path appImageDir) throws IOException {
        try {
            Document doc = readXml(appImageDir);

            XPath xPath = XPathFactory.newInstance().newXPath();

            String mainLauncher = xpathQueryNullable(xPath,
                    "/jpackage-state/main-launcher/text()", doc);
            if (mainLauncher == null) {
                // No main launcher, this is fatal.
                return new AppImageFile();
            }

            List<LauncherInfo> launcherInfos = new ArrayList<>();

            String platform = xpathQueryNullable(xPath,
                    "/jpackage-state/@platform", doc);

            String version = xpathQueryNullable(xPath,
                    "/jpackage-state/@version", doc);

            String signedStr = xpathQueryNullable(xPath,
                    "/jpackage-state/@signed", doc);

            NodeList launcherNodes = (NodeList) xPath.evaluate(
                    "/jpackage-state/add-launcher", doc,
                    XPathConstants.NODESET);

            for (int i = 0; i != launcherNodes.getLength(); i++) {
                 Node item = launcherNodes.item(i);
                 String name = getAttribute(item, "name");
                 String shortcut = getAttribute(item, "shortcut");
                 String menu = getAttribute(item, "menu");

                 launcherInfos.add(new LauncherInfo(name,
                         !("false".equals(shortcut)),
                         !("false".equals(menu))));
            }

            AppImageFile file = new AppImageFile(
                    mainLauncher, launcherInfos, version, platform, signedStr);
            if (!file.isValid()) {
                file = new AppImageFile();
            }
            return file;
        } catch (XPathExpressionException ex) {
            // This should never happen as XPath expressions should be correct
            throw new RuntimeException(ex);
        }
    }

    private static String getAttribute(Node item, String attr) {
        NamedNodeMap attrs = item.getAttributes();
        Node attrNode = attrs.getNamedItem(attr);
        return ((attrNode == null) ? null : attrNode.getNodeValue());
    }

    public static Document readXml(Path appImageDir) throws IOException {
        try {
            Path path = getPathInAppImage(appImageDir);

            DocumentBuilderFactory dbf =
                    DocumentBuilderFactory.newDefaultInstance();
            dbf.setFeature(
                   "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);
            DocumentBuilder b = dbf.newDocumentBuilder();
            return b.parse(Files.newInputStream(path));
        } catch (ParserConfigurationException | SAXException ex) {
            // Let caller sort this out
            throw new IOException(ex);
        }
    }

    /**
     * Returns list of LauncherInfo objects configured for the application.
     * The first item in the returned list is main launcher.
     * Following items in the list are names of additional launchers.
     */
    static List<LauncherInfo> getLaunchers(Path appImageDir,
            Map<String, ? super Object> params) {
        List<LauncherInfo> launchers = new ArrayList<>();
        try {
            AppImageFile appImageInfo = AppImageFile.load(appImageDir);
            if (appImageInfo != null) {
                launchers.add(new LauncherInfo(
                        appImageInfo.getLauncherName(), true, true));
                launchers.addAll(appImageInfo.getAddLaunchers());
                return launchers;
            }
        } catch (NoSuchFileException nsfe) {
            // non jpackage generated app-image (no app/.jpackage.xml)
            Log.info(MessageFormat.format(I18N.getString(
                    "warning.foreign-app-image"), appImageDir));
        } catch (IOException ioe) {
            Log.verbose(ioe);
            Log.info(MessageFormat.format(I18N.getString(
                    "warning.invalid-app-image"), appImageDir));

        }
        // this should never be the case, but maintaining behavior of
        // creating default launchers without AppImageFile present

        ADD_LAUNCHERS.fetchFrom(params).stream().map(APP_NAME::fetchFrom).map(
                name -> new LauncherInfo(name, true, true)).forEach(launchers::add);
        return launchers;
    }

    public static String extractAppName(Path appImageDir) {
        try {
            return AppImageFile.load(appImageDir).getLauncherName();
        } catch (IOException ioe) {
            Log.verbose(MessageFormat.format(I18N.getString(
                    "warning.foreign-app-image"), appImageDir));
            return null;
        }
    }

    private static String xpathQueryNullable(XPath xPath, String xpathExpr,
            Document xml) throws XPathExpressionException {
        NodeList nodes = (NodeList) xPath.evaluate(xpathExpr, xml,
                XPathConstants.NODESET);
        if (nodes != null && nodes.getLength() > 0) {
            return nodes.item(0).getNodeValue();
        }
        return null;
    }

    private static String getVersion() {
        return System.getProperty("java.version");
    }

    private static String getPlatform() {
        return PLATFORM_LABELS.get(Platform.getPlatform());
    }

    private boolean isValid() {
        if (launcherName == null || launcherName.length() == 0) {
            return false;
        }
        for (var launcher : addLauncherInfos) {
            if ("".equals(launcher.getName())) {
                return false;
            }
        }

        return true;
    }

    static class LauncherInfo {
        private String name;
        private boolean shortcut;
        private boolean menu;

        public LauncherInfo(String name, boolean shortcut, boolean menu) {
            this.name = name;
            this.shortcut = shortcut;
            this.menu = menu;
        }
        public String getName() {
            return name;
        }
        public boolean isShortcut() {
            return shortcut;
        }
        public boolean isMenu() {
            return menu;
        }
    }

}
