/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import jdk.jpackage.internal.IOUtils.XmlConsumer;
import static jdk.jpackage.internal.OverridableResource.createResource;
import static jdk.jpackage.internal.StandardBundlerParam.LICENSE_FILE;
import jdk.jpackage.internal.WixAppImageFragmentBuilder.ShortcutsFolder;

/**
 * Creates UI WiX fragment.
 */
final class WixUiFragmentBuilder extends WixFragmentBuilder {

    @Override
    void initFromParams(Map<String, ? super Object> params) {
        super.initFromParams(params);

        String licenseFile = LICENSE_FILE.fetchFrom(params);
        withLicenseDlg = licenseFile != null;
        if (withLicenseDlg) {
            Path licenseFileName = IOUtils.getFileName(Path.of(licenseFile));
            Path destFile = getConfigRoot().resolve(licenseFileName);
            setWixVariable("JpLicenseRtf", destFile.toAbsolutePath().toString());
        }

        withInstallDirChooserDlg = INSTALLDIR_CHOOSER.fetchFrom(params);

        List<ShortcutsFolder> shortcutFolders = Stream.of(
                ShortcutsFolder.values()).filter(shortcutFolder -> {
            return shortcutFolder.requested(params)
                    && SHORTCUT_PROMPT.fetchFrom(params);
        }).toList();

        withShortcutPromptDlg = !shortcutFolders.isEmpty();

        customDialogs = new ArrayList<>();

        if (withShortcutPromptDlg) {
            CustomDialog dialog = new CustomDialog(params, I18N.getString(
                    "resource.shortcutpromptdlg-wix-file"),
                    "ShortcutPromptDlg.wxs");
            for (var shortcutFolder : shortcutFolders) {
                dialog.wixVariables.defineWixVariable(
                        shortcutFolder.getWixVariableName());
            }
            customDialogs.add(dialog);
        }

        if (withInstallDirChooserDlg) {
            CustomDialog dialog = new CustomDialog(params, I18N.getString(
                    "resource.installdirnotemptydlg-wix-file"),
                    "InstallDirNotEmptyDlg.wxs");
            List<Dialog> dialogIds = getUI().dialogIdsSupplier.apply(this);
            dialog.wixVariables.setWixVariable("JpAfterInstallDirDlg",
                    dialogIds.get(dialogIds.indexOf(Dialog.InstallDirDlg) + 1).id);
            customDialogs.add(dialog);
        }
    }

    @Override
    void configureWixPipeline(WixPipeline wixPipeline) {
        super.configureWixPipeline(wixPipeline);

        if (withShortcutPromptDlg || withInstallDirChooserDlg || withLicenseDlg) {
            wixPipeline.addLightOptions("-ext", "WixUIExtension");
        }

        // Only needed if we using CA dll, so Wix can find it
        if (withInstallDirChooserDlg) {
            wixPipeline.addLightOptions("-b",
                    getConfigRoot().toAbsolutePath().toString());
        }

        for (var customDialog : customDialogs) {
            customDialog.addToWixPipeline(wixPipeline);
        }
    }

    @Override
    void addFilesToConfigRoot() throws IOException {
        super.addFilesToConfigRoot();

        if (withInstallDirChooserDlg) {
            String fname = "wixhelper.dll"; // CA dll
            try (InputStream is = OverridableResource.readDefault(fname)) {
                Files.copy(is, getConfigRoot().resolve(fname));
            }
        }
    }

    @Override
    protected Collection<XmlConsumer> getFragmentWriters() {
        return List.of(this::addUI);
    }

    private void addUI(XMLStreamWriter xml) throws XMLStreamException,
            IOException {

        if (withInstallDirChooserDlg) {
            xml.writeStartElement("Property");
            xml.writeAttribute("Id", "WIXUI_INSTALLDIR");
            xml.writeAttribute("Value", "INSTALLDIR");
            xml.writeEndElement(); // Property
        }

        if (withLicenseDlg) {
            xml.writeStartElement("WixVariable");
            xml.writeAttribute("Id", "WixUILicenseRtf");
            xml.writeAttribute("Value", "$(var.JpLicenseRtf)");
            xml.writeEndElement(); // WixVariable
        }

        xml.writeStartElement("UI");
        xml.writeAttribute("Id", "JpUI");

        var ui = getUI();
        if (ui != null) {
            ui.write(this, xml);
        }

        xml.writeEndElement(); // UI
    }

    private UI getUI() {
        if (withInstallDirChooserDlg || withShortcutPromptDlg) {
            // WixUI_InstallDir for shortcut prompt dialog too because in
            // WixUI_Minimal UI sequence WelcomeEulaDlg dialog doesn't have "Next"
            // button, but has "Install" button. So inserting shortcut prompt dialog
            // after welcome dialog in WixUI_Minimal UI sequence would be confusing
            return UI.InstallDir;
        } else if (withLicenseDlg) {
            return UI.Minimal;
        } else {
            return null;
        }
    }

    private enum UI {
        InstallDir("WixUI_InstallDir",
                WixUiFragmentBuilder::dialogSequenceForWixUI_InstallDir,
                Dialog::createPairsForWixUI_InstallDir),
        Minimal("WixUI_Minimal", null, null);

        UI(String wixUIRef,
                Function<WixUiFragmentBuilder, List<Dialog>> dialogIdsSupplier,
                Supplier<Map<DialogPair, List<Publish>>> dialogPairsSupplier) {
            this.wixUIRef = wixUIRef;
            this.dialogIdsSupplier = dialogIdsSupplier;
            this.dialogPairsSupplier = dialogPairsSupplier;
        }

        void write(WixUiFragmentBuilder outer, XMLStreamWriter xml) throws
                XMLStreamException, IOException {
            xml.writeStartElement("UIRef");
            xml.writeAttribute("Id", wixUIRef);
            xml.writeEndElement(); // UIRef

            if (dialogIdsSupplier != null) {
                List<Dialog> dialogIds = dialogIdsSupplier.apply(outer);
                Map<DialogPair, List<Publish>> dialogPairs = dialogPairsSupplier.get();

                if (dialogIds.contains(Dialog.InstallDirDlg)) {
                    xml.writeStartElement("DialogRef");
                    xml.writeAttribute("Id", "InstallDirNotEmptyDlg");
                    xml.writeEndElement(); // DialogRef
                }

                var it = dialogIds.iterator();
                Dialog firstId = it.next();
                while (it.hasNext()) {
                    Dialog secondId = it.next();
                    DialogPair pair = new DialogPair(firstId, secondId);
                    for (var curPair : List.of(pair, pair.flip())) {
                        for (var publish : dialogPairs.get(curPair)) {
                            writePublishDialogPair(xml, publish, curPair);
                        }
                    }
                    firstId = secondId;
                }
            }
        }

        private final String wixUIRef;
        private final Function<WixUiFragmentBuilder, List<Dialog>> dialogIdsSupplier;
        private final Supplier<Map<DialogPair, List<Publish>>> dialogPairsSupplier;
    }

    private List<Dialog> dialogSequenceForWixUI_InstallDir() {
        List<Dialog> dialogIds = new ArrayList<>(
                List.of(Dialog.WixUI_WelcomeDlg));
        if (withLicenseDlg) {
            dialogIds.add(Dialog.WixUI_LicenseAgreementDlg);
        }

        if (withInstallDirChooserDlg) {
            dialogIds.add(Dialog.InstallDirDlg);
        }

        if (withShortcutPromptDlg) {
            dialogIds.add(Dialog.ShortcutPromptDlg);
        }

        dialogIds.add(Dialog.WixUI_VerifyReadyDlg);

        return dialogIds;
    }

    private enum Dialog {
        WixUI_WelcomeDlg,
        WixUI_LicenseAgreementDlg,
        InstallDirDlg,
        ShortcutPromptDlg,
        WixUI_VerifyReadyDlg;

        Dialog() {
            if (name().startsWith("WixUI_")) {
                id = name().substring("WixUI_".length());
            } else {
                id = name();
            }
        }

        static Map<DialogPair, List<Publish>> createPair(Dialog firstId,
                Dialog secondId, List<PublishBuilder> nextBuilders,
                List<PublishBuilder> prevBuilders) {
            var pair = new DialogPair(firstId, secondId);
            return Map.of(pair, nextBuilders.stream().map(b -> {
                return buildPublish(b.create()).next().create();
            }).toList(), pair.flip(),
                    prevBuilders.stream().map(b -> {
                        return buildPublish(b.create()).back().create();
                    }).toList());
        }

        static Map<DialogPair, List<Publish>> createPair(Dialog firstId,
                Dialog secondId, List<PublishBuilder> builders) {
            return createPair(firstId, secondId, builders, builders);
        }

        static Map<DialogPair, List<Publish>> createPairsForWixUI_InstallDir() {
            Map<DialogPair, List<Publish>> map = new HashMap<>();

            // Order is a "weight" of action. If there are multiple
            // "NewDialog" action for the same dialog Id, MSI would pick the one
            // with higher order value. In WixUI_InstallDir dialog sequence the
            // highest order value is 4. InstallDirNotEmptyDlg adds NewDialog
            // action with order 5. Setting order to 6 for all
            // actions configured in this function would make them executed
            // instead of corresponding default actions defined in
            // WixUI_InstallDir dialog sequence.
            var order = 6;

            // Based on WixUI_InstallDir.wxs
            var backFromVerifyReadyDlg = List.of(buildPublish().condition(
                    "NOT Installed").order(order));
            var uncondinal = List.of(buildPublish().condition("1"));
            var ifNotIstalled = List.of(
                    buildPublish().condition("NOT Installed").order(order));
            var ifLicenseAccepted = List.of(buildPublish().condition(
                    "LicenseAccepted = \"1\"").order(order));

            // Empty condition list for the default dialog sequence
            map.putAll(createPair(WixUI_WelcomeDlg, WixUI_LicenseAgreementDlg,
                    List.of()));
            map.putAll(
                    createPair(WixUI_WelcomeDlg, InstallDirDlg, ifNotIstalled));
            map.putAll(createPair(WixUI_WelcomeDlg, ShortcutPromptDlg,
                    ifNotIstalled));

            map.putAll(createPair(WixUI_LicenseAgreementDlg, InstallDirDlg,
                    List.of()));
            map.putAll(createPair(WixUI_LicenseAgreementDlg, ShortcutPromptDlg,
                    ifLicenseAccepted, uncondinal));
            map.putAll(createPair(WixUI_LicenseAgreementDlg,
                    WixUI_VerifyReadyDlg, ifLicenseAccepted,
                    backFromVerifyReadyDlg));

            map.putAll(createPair(InstallDirDlg, ShortcutPromptDlg, List.of(),
                    uncondinal));
            map.putAll(createPair(InstallDirDlg, WixUI_VerifyReadyDlg, List.of()));

            map.putAll(createPair(ShortcutPromptDlg, WixUI_VerifyReadyDlg,
                    uncondinal, backFromVerifyReadyDlg));

            return map;
        }

        private final String id;
    }

    private static final class DialogPair {

        DialogPair(Dialog first, Dialog second) {
            this(first.id, second.id);
        }

        DialogPair(String firstId, String secondId) {
            this.firstId = firstId;
            this.secondId = secondId;
        }

        DialogPair flip() {
            return new DialogPair(secondId, firstId);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + Objects.hashCode(this.firstId);
            hash = 97 * hash + Objects.hashCode(this.secondId);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final DialogPair other = (DialogPair) obj;
            if (!Objects.equals(this.firstId, other.firstId)) {
                return false;
            }
            if (!Objects.equals(this.secondId, other.secondId)) {
                return false;
            }
            return true;
        }

        private final String firstId;
        private final String secondId;
    }

    private static final class Publish {

        Publish(String control, String condition, int order) {
            this.control = control;
            this.condition = condition;
            this.order = order;
        }

        private final String control;
        private final String condition;
        private final int order;
    }

    private static final class PublishBuilder {

        PublishBuilder() {
            order(0);
            next();
            condition("1");
        }

        PublishBuilder(Publish publish) {
            order(publish.order);
            control(publish.control);
            condition(publish.condition);
        }

        public PublishBuilder control(String v) {
            control = v;
            return this;
        }

        public PublishBuilder next() {
            return control("Next");
        }

        public PublishBuilder back() {
            return control("Back");
        }

        public PublishBuilder condition(String v) {
            condition = v;
            return this;
        }

        public PublishBuilder order(int v) {
            order = v;
            return this;
        }

        Publish create() {
            return new Publish(control, condition, order);
        }

        private String control;
        private String condition;
        private int order;
    }

    private static PublishBuilder buildPublish() {
        return new PublishBuilder();
    }

    private static PublishBuilder buildPublish(Publish publish) {
        return new PublishBuilder(publish);
    }

    private static void writePublishDialogPair(XMLStreamWriter xml,
            Publish publish, DialogPair dialogPair) throws IOException,
            XMLStreamException {
        xml.writeStartElement("Publish");
        xml.writeAttribute("Dialog", dialogPair.firstId);
        xml.writeAttribute("Control", publish.control);
        xml.writeAttribute("Event", "NewDialog");
        xml.writeAttribute("Value", dialogPair.secondId);
        if (publish.order != 0) {
            xml.writeAttribute("Order", String.valueOf(publish.order));
        }
        xml.writeCharacters(publish.condition);
        xml.writeEndElement();
    }

    private final class CustomDialog {

        CustomDialog(Map<String, ? super Object> params, String category,
                String wxsFileName) {
            this.wxsFileName = wxsFileName;
            this.wixVariables = new WixVariables();

            addResource(
                    createResource(wxsFileName, params).setCategory(category),
                    wxsFileName);
        }

        void addToWixPipeline(WixPipeline wixPipeline) {
            wixPipeline.addSource(getConfigRoot().toAbsolutePath().resolve(
                    wxsFileName), wixVariables.getValues());
        }

        private final WixVariables wixVariables;
        private final String wxsFileName;
    }

    private boolean withInstallDirChooserDlg;
    private boolean withShortcutPromptDlg;
    private boolean withLicenseDlg;
    private List<CustomDialog> customDialogs;

    private static final BundlerParamInfo<Boolean> INSTALLDIR_CHOOSER
            = new StandardBundlerParam<>(
                    Arguments.CLIOptions.WIN_DIR_CHOOSER.getId(),
                    Boolean.class,
                    params -> false,
                    (s, p) -> Boolean.valueOf(s)
            );

    private static final StandardBundlerParam<Boolean> SHORTCUT_PROMPT
            = new StandardBundlerParam<>(
                    Arguments.CLIOptions.WIN_SHORTCUT_PROMPT.getId(),
                    Boolean.class,
                    params -> false,
                    (s, p) -> Boolean.valueOf(s)
            );
}
