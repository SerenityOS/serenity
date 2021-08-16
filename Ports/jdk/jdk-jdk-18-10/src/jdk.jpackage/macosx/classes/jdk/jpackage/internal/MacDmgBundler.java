/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import static jdk.jpackage.internal.MacAppImageBuilder.ICON_ICNS;
import static jdk.jpackage.internal.MacAppImageBuilder.MAC_CF_BUNDLE_IDENTIFIER;
import static jdk.jpackage.internal.OverridableResource.createResource;

import static jdk.jpackage.internal.StandardBundlerParam.APP_NAME;
import static jdk.jpackage.internal.StandardBundlerParam.CONFIG_ROOT;
import static jdk.jpackage.internal.StandardBundlerParam.LICENSE_FILE;
import static jdk.jpackage.internal.StandardBundlerParam.TEMP_ROOT;
import static jdk.jpackage.internal.StandardBundlerParam.VERBOSE;

public class MacDmgBundler extends MacBaseInstallerBundler {

    private static final ResourceBundle I18N = ResourceBundle.getBundle(
            "jdk.jpackage.internal.resources.MacResources");

    // Background image name in resources
    static final String DEFAULT_BACKGROUND_IMAGE = "background_dmg.tiff";
    // Backround image name and folder under which it will be stored in DMG
    static final String BACKGROUND_IMAGE_FOLDER =".background";
    static final String BACKGROUND_IMAGE = "background.tiff";
    static final String DEFAULT_DMG_SETUP_SCRIPT = "DMGsetup.scpt";
    static final String TEMPLATE_BUNDLE_ICON = "JavaApp.icns";

    static final String DEFAULT_LICENSE_PLIST="lic_template.plist";

    public static final BundlerParamInfo<String> INSTALLER_SUFFIX =
            new StandardBundlerParam<> (
            "mac.dmg.installerName.suffix",
            String.class,
            params -> "",
            (s, p) -> s);

    public Path bundle(Map<String, ? super Object> params,
            Path outdir) throws PackagerException {
        Log.verbose(MessageFormat.format(I18N.getString("message.building-dmg"),
                APP_NAME.fetchFrom(params)));

        IOUtils.writableOutputDir(outdir);

        try {
            Path appLocation = prepareAppBundle(params);

            if (appLocation != null && prepareConfigFiles(params)) {
                Path configScript = getConfig_Script(params);
                if (IOUtils.exists(configScript)) {
                    IOUtils.run("bash", configScript);
                }

                return buildDMG(params, appLocation, outdir);
            }
            return null;
        } catch (IOException | PackagerException ex) {
            Log.verbose(ex);
            throw new PackagerException(ex);
        }
    }

    private static final String hdiutil = "/usr/bin/hdiutil";

    private void prepareDMGSetupScript(Map<String, ? super Object> params)
                                                                    throws IOException {
        Path dmgSetup = getConfig_VolumeScript(params);
        Log.verbose(MessageFormat.format(
                I18N.getString("message.preparing-dmg-setup"),
                dmgSetup.toAbsolutePath().toString()));

        // We need to use URL for DMG to find it. We cannot use volume name, since
        // user might have open DMG with same volume name already. Url should end with
        // '/' and it should be real path (no symbolic links).
        Path imageDir = IMAGES_ROOT.fetchFrom(params);
        if (!Files.exists(imageDir)) {
             // Create it, since it does not exist
             Files.createDirectories(imageDir);
        }
        Path rootPath = Path.of(imageDir.toString()).toRealPath();
        Path volumePath = rootPath.resolve(APP_NAME.fetchFrom(params));
        String volumeUrl = volumePath.toUri().toString() + File.separator;

        // Provide full path to backround image, so we can find it.
        Path bgFile = Path.of(rootPath.toString(), APP_NAME.fetchFrom(params),
                              BACKGROUND_IMAGE_FOLDER, BACKGROUND_IMAGE);

        //prepare config for exe
        Map<String, String> data = new HashMap<>();
        data.put("DEPLOY_VOLUME_URL", volumeUrl);
        data.put("DEPLOY_BG_FILE", bgFile.toString());
        data.put("DEPLOY_VOLUME_PATH", volumePath.toString());
        data.put("DEPLOY_APPLICATION_NAME", APP_NAME.fetchFrom(params));

        data.put("DEPLOY_INSTALL_LOCATION", getInstallDir(params, true));

        createResource(DEFAULT_DMG_SETUP_SCRIPT, params)
                .setCategory(I18N.getString("resource.dmg-setup-script"))
                .setSubstitutionData(data)
                .saveToFile(dmgSetup);
    }

    private Path getConfig_VolumeScript(Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params).resolve(
                APP_NAME.fetchFrom(params) + "-dmg-setup.scpt");
    }

    private Path getConfig_VolumeBackground(
            Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params).resolve(
                APP_NAME.fetchFrom(params) + "-background.tiff");
    }

    private Path getConfig_VolumeIcon(Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params).resolve(
                APP_NAME.fetchFrom(params) + "-volume.icns");
    }

    private Path getConfig_LicenseFile(Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params).resolve(
                APP_NAME.fetchFrom(params) + "-license.plist");
    }

    private void prepareLicense(Map<String, ? super Object> params) {
        try {
            String licFileStr = LICENSE_FILE.fetchFrom(params);
            if (licFileStr == null) {
                return;
            }

            Path licFile = Path.of(licFileStr);
            byte[] licenseContentOriginal =
                    Files.readAllBytes(licFile);
            String licenseInBase64 =
                    Base64.getEncoder().encodeToString(licenseContentOriginal);

            Map<String, String> data = new HashMap<>();
            data.put("APPLICATION_LICENSE_TEXT", licenseInBase64);

            createResource(DEFAULT_LICENSE_PLIST, params)
                    .setCategory(I18N.getString("resource.license-setup"))
                    .setSubstitutionData(data)
                    .saveToFile(getConfig_LicenseFile(params));

        } catch (IOException ex) {
            Log.verbose(ex);
        }
    }

    private boolean prepareConfigFiles(Map<String, ? super Object> params)
            throws IOException {

        createResource(DEFAULT_BACKGROUND_IMAGE, params)
                    .setCategory(I18N.getString("resource.dmg-background"))
                    .saveToFile(getConfig_VolumeBackground(params));

        createResource(TEMPLATE_BUNDLE_ICON, params)
                .setCategory(I18N.getString("resource.volume-icon"))
                .setExternal(ICON_ICNS.fetchFrom(params))
                .saveToFile(getConfig_VolumeIcon(params));

        createResource(null, params)
                .setCategory(I18N.getString("resource.post-install-script"))
                .saveToFile(getConfig_Script(params));

        prepareLicense(params);

        prepareDMGSetupScript(params);

        return true;
    }

    // name of post-image script
    private Path getConfig_Script(Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params).resolve(
                APP_NAME.fetchFrom(params) + "-post-image.sh");
    }

    // Location of SetFile utility may be different depending on MacOS version
    // We look for several known places and if none of them work will
    // try ot find it
    private String findSetFileUtility() {
        String typicalPaths[] = {"/Developer/Tools/SetFile",
                "/usr/bin/SetFile", "/Developer/usr/bin/SetFile"};

        String setFilePath = null;
        for (String path : typicalPaths) {
            Path f = Path.of(path);
            if (Files.exists(f) && Files.isExecutable(f)) {
                setFilePath = path;
                break;
            }
        }

        // Validate SetFile, if Xcode is not installed it will run, but exit with error
        // code
        if (setFilePath != null) {
            try {
                ProcessBuilder pb = new ProcessBuilder(setFilePath, "-h");
                Process p = pb.start();
                int code = p.waitFor();
                if (code == 0) {
                    return setFilePath;
                }
            } catch (Exception ignored) {}

            // No need for generic find attempt. We found it, but it does not work.
            // Probably due to missing xcode.
            return null;
        }

        // generic find attempt
        try {
            ProcessBuilder pb = new ProcessBuilder("/usr/bin/xcrun", "-find", "SetFile");
            Process p = pb.start();
            InputStreamReader isr = new InputStreamReader(p.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String lineRead = br.readLine();
            if (lineRead != null) {
                Path f = Path.of(lineRead);
                if (Files.exists(f) && Files.isExecutable(f)) {
                    return f.toAbsolutePath().toString();
                }
            }
        } catch (IOException ignored) {}

        return null;
    }

    private Path buildDMG( Map<String, ? super Object> params,
            Path appLocation, Path outdir) throws IOException {
        boolean copyAppImage = false;
        Path imagesRoot = IMAGES_ROOT.fetchFrom(params);
        if (!Files.exists(imagesRoot)) {
            Files.createDirectories(imagesRoot);
        }

        Path protoDMG = imagesRoot.resolve(APP_NAME.fetchFrom(params) +"-tmp.dmg");
        Path finalDMG = outdir.resolve(MAC_INSTALLER_NAME.fetchFrom(params)
                + INSTALLER_SUFFIX.fetchFrom(params) + ".dmg");

        Path srcFolder = APP_IMAGE_TEMP_ROOT.fetchFrom(params);
        Path predefinedImage = StandardBundlerParam.getPredefinedAppImage(params);
        if (predefinedImage != null) {
            srcFolder = predefinedImage;
        } else if (StandardBundlerParam.isRuntimeInstaller(params)) {
            Path newRoot = Files.createTempDirectory(TEMP_ROOT.fetchFrom(params),
                    "root-");

            // first, is this already a runtime with
            // <runtime>/Contents/Home - if so we need the Home dir
            Path home = appLocation.resolve("Contents/Home");
            Path source = (Files.exists(home)) ? home : appLocation;

            // Then we need to put back the <NAME>/Content/Home
            Path root = newRoot.resolve(
                    MAC_CF_BUNDLE_IDENTIFIER.fetchFrom(params));
            Path dest = root.resolve("Contents/Home");

            IOUtils.copyRecursive(source, dest);

            srcFolder = newRoot;
        }

        Log.verbose(MessageFormat.format(I18N.getString(
                "message.creating-dmg-file"), finalDMG.toAbsolutePath()));

        Files.deleteIfExists(protoDMG);
        try {
            Files.deleteIfExists(finalDMG);
        } catch (IOException ex) {
            throw new IOException(MessageFormat.format(I18N.getString(
                    "message.dmg-cannot-be-overwritten"),
                    finalDMG.toAbsolutePath()));
        }

        Files.createDirectories(protoDMG.getParent());
        Files.createDirectories(finalDMG.getParent());

        String hdiUtilVerbosityFlag = VERBOSE.fetchFrom(params) ?
                "-verbose" : "-quiet";

        // create temp image
        ProcessBuilder pb = new ProcessBuilder(
                hdiutil,
                "create",
                hdiUtilVerbosityFlag,
                "-srcfolder", srcFolder.toAbsolutePath().toString(),
                "-volname", APP_NAME.fetchFrom(params),
                "-ov", protoDMG.toAbsolutePath().toString(),
                "-fs", "HFS+",
                "-format", "UDRW");
        try {
            IOUtils.exec(pb, false, null, true, Executor.INFINITE_TIMEOUT);
        } catch (IOException ex) {
            Log.verbose(ex); // Log exception

            // Creating DMG from entire app image failed, so lets try to create empty
            // DMG and copy files manually. See JDK-8248059.
            copyAppImage = true;

            long size = new PathGroup(Map.of(new Object(), srcFolder)).sizeInBytes();
            size += 50 * 1024 * 1024; // Add extra 50 megabytes. Actually DMG size will
            // not be bigger, but it will able to hold additional 50 megabytes of data.
            // We need extra room for icons and background image. When we providing
            // actual files to hdiutil, it will create DMG with ~50 megabytes extra room.
            pb = new ProcessBuilder(
                hdiutil,
                "create",
                hdiUtilVerbosityFlag,
                "-size", String.valueOf(size),
                "-volname", APP_NAME.fetchFrom(params),
                "-ov", protoDMG.toAbsolutePath().toString(),
                "-fs", "HFS+");
            new RetryExecutor()
                .setMaxAttemptsCount(10)
                .setAttemptTimeoutMillis(3000)
                .setWriteOutputToFile(true)
                .execute(pb);
        }

        // mount temp image
        pb = new ProcessBuilder(
                hdiutil,
                "attach",
                protoDMG.toAbsolutePath().toString(),
                hdiUtilVerbosityFlag,
                "-mountroot", imagesRoot.toAbsolutePath().toString());
        IOUtils.exec(pb, false, null, true, Executor.INFINITE_TIMEOUT);

        Path mountedRoot = imagesRoot.resolve(APP_NAME.fetchFrom(params));

        // Copy app image, since we did not create DMG with it, but instead we created
        // empty one.
        if (copyAppImage) {
            // In case of predefine app image srcFolder will point to app bundle, so if
            // we use it as is we will copy content of app bundle, but we need app bundle
            // folder as well.
            if (srcFolder.toString().toLowerCase().endsWith(".app")) {
                Path destPath = mountedRoot
                        .resolve(srcFolder.getFileName());
                Files.createDirectory(destPath);
                IOUtils.copyRecursive(srcFolder, destPath);
            } else {
                IOUtils.copyRecursive(srcFolder, mountedRoot);
            }
        }

        try {
            // background image
            Path bgdir = mountedRoot.resolve(BACKGROUND_IMAGE_FOLDER);
            Files.createDirectories(bgdir);
            IOUtils.copyFile(getConfig_VolumeBackground(params),
                    bgdir.resolve(BACKGROUND_IMAGE));

            // We will not consider setting background image and creating link
            // to install-dir in DMG as critical error, since it can fail in
            // headless enviroment.
            try {
                pb = new ProcessBuilder("/usr/bin/osascript",
                        getConfig_VolumeScript(params).toAbsolutePath().toString());
                IOUtils.exec(pb, 180); // Wait 3 minutes. See JDK-8248248.
            } catch (IOException ex) {
                Log.verbose(ex);
            }

            // volume icon
            Path volumeIconFile = mountedRoot.resolve(".VolumeIcon.icns");
            IOUtils.copyFile(getConfig_VolumeIcon(params),
                    volumeIconFile);

            // Indicate that we want a custom icon
            // NB: attributes of the root directory are ignored
            // when creating the volume
            // Therefore we have to do this after we mount image
            String setFileUtility = findSetFileUtility();
            if (setFileUtility != null) {
                //can not find utility => keep going without icon
                try {
                    volumeIconFile.toFile().setWritable(true);
                    // The "creator" attribute on a file is a legacy attribute
                    // but it seems Finder excepts these bytes to be
                    // "icnC" for the volume icon
                    // (might not work on Mac 10.13 with old XCode)
                    pb = new ProcessBuilder(
                            setFileUtility,
                            "-c", "icnC",
                            volumeIconFile.toAbsolutePath().toString());
                    IOUtils.exec(pb);
                    volumeIconFile.toFile().setReadOnly();

                    pb = new ProcessBuilder(
                            setFileUtility,
                            "-a", "C",
                            mountedRoot.toAbsolutePath().toString());
                    IOUtils.exec(pb);
                } catch (IOException ex) {
                    Log.error(ex.getMessage());
                    Log.verbose("Cannot enable custom icon using SetFile utility");
                }
            } else {
                Log.verbose(I18N.getString("message.setfile.dmg"));
            }

        } finally {
            // Detach the temporary image
            pb = new ProcessBuilder(
                    hdiutil,
                    "detach",
                    hdiUtilVerbosityFlag,
                    mountedRoot.toAbsolutePath().toString());
            // "hdiutil detach" might not work right away due to resource busy error, so
            // repeat detach several times.
            RetryExecutor retryExecutor = new RetryExecutor();
            // Image can get detach even if we got resource busy error, so stop
            // trying to detach it if it is no longer attached.
            retryExecutor.setExecutorInitializer(exec -> {
                if (!Files.exists(mountedRoot)) {
                    retryExecutor.abort();
                }
            });
            try {
                // 10 times with 6 second delays.
                retryExecutor.setMaxAttemptsCount(10).setAttemptTimeoutMillis(6000)
                        .execute(pb);
            } catch (IOException ex) {
                if (!retryExecutor.isAborted()) {
                    // Now force to detach if it still attached
                    if (Files.exists(mountedRoot)) {
                        pb = new ProcessBuilder(
                                hdiutil,
                                "detach",
                                "-force",
                                hdiUtilVerbosityFlag,
                                mountedRoot.toAbsolutePath().toString());
                        IOUtils.exec(pb, false, null, true, Executor.INFINITE_TIMEOUT);
                    }
                }
            }
        }

        // Compress it to a new image
        pb = new ProcessBuilder(
                hdiutil,
                "convert",
                protoDMG.toAbsolutePath().toString(),
                hdiUtilVerbosityFlag,
                "-format", "UDZO",
                "-o", finalDMG.toAbsolutePath().toString());
        try {
            new RetryExecutor()
                .setMaxAttemptsCount(10)
                .setAttemptTimeoutMillis(3000)
                .execute(pb);
        } catch (Exception ex) {
            // Convert might failed if something holds file. Try to convert copy.
            Path protoDMG2 = imagesRoot
                    .resolve(APP_NAME.fetchFrom(params) + "-tmp2.dmg");
            Files.copy(protoDMG, protoDMG2);
            try {
                pb = new ProcessBuilder(
                        hdiutil,
                        "convert",
                        protoDMG2.toAbsolutePath().toString(),
                        hdiUtilVerbosityFlag,
                        "-format", "UDZO",
                        "-o", finalDMG.toAbsolutePath().toString());
                IOUtils.exec(pb, false, null, true, Executor.INFINITE_TIMEOUT);
            } finally {
                Files.deleteIfExists(protoDMG2);
            }
        }

        //add license if needed
        if (Files.exists(getConfig_LicenseFile(params))) {
            pb = new ProcessBuilder(
                    hdiutil,
                    "udifrez",
                    finalDMG.toAbsolutePath().toString(),
                    "-xml",
                    getConfig_LicenseFile(params).toAbsolutePath().toString()
            );
            new RetryExecutor()
                .setMaxAttemptsCount(10)
                .setAttemptTimeoutMillis(3000)
                .execute(pb);
        }

        //Delete the temporary image
        Files.deleteIfExists(protoDMG);

        Log.verbose(MessageFormat.format(I18N.getString(
                "message.output-to-location"),
                APP_NAME.fetchFrom(params), finalDMG.toAbsolutePath().toString()));

        return finalDMG;
    }


    //////////////////////////////////////////////////////////////////////////
    // Implement Bundler
    //////////////////////////////////////////////////////////////////////////

    @Override
    public String getName() {
        return I18N.getString("dmg.bundler.name");
    }

    @Override
    public String getID() {
        return "dmg";
    }

    @Override
    public boolean validate(Map<String, ? super Object> params)
            throws ConfigException {
        try {
            Objects.requireNonNull(params);

            //run basic validation to ensure requirements are met
            //we are not interested in return code, only possible exception
            validateAppImageAndBundeler(params);

            return true;
        } catch (RuntimeException re) {
            if (re.getCause() instanceof ConfigException) {
                throw (ConfigException) re.getCause();
            } else {
                throw new ConfigException(re);
            }
        }
    }

    @Override
    public Path execute(Map<String, ? super Object> params,
            Path outputParentDir) throws PackagerException {
        return bundle(params, outputParentDir);
    }

    @Override
    public boolean supported(boolean runtimeInstaller) {
        return isSupported();
    }

    public static final String[] required =
            {"/usr/bin/hdiutil", "/usr/bin/osascript"};
    public static boolean isSupported() {
        try {
            for (String s : required) {
                Path f = Path.of(s);
                if (!Files.exists(f) || !Files.isExecutable(f)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}
