/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8153955
 * @summary test the FileHandler's new property
 *  "java.util.logging.FileHandler.maxLocks" which will be present in
 *  "logging.properties" file with default value of 100. This property can be
 *  overriden by specifying this property in the custom config file.
 * @library /test/lib
 * @build jdk.test.lib.Platform
 *        jdk.test.lib.util.FileUtils
 * @author rpatil
 * @run main/othervm FileHandlerMaxLocksTest
 */
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import jdk.test.lib.util.FileUtils;

public class FileHandlerMaxLocksTest {

    private static final String LOGGER_DIR = "logger-dir";
    private static final String MAX_LOCK_PROPERTY = "java.util.logging.FileHandler.maxLocks = 200";
    private static final String CONFIG_FILE_NAME = "logging.properties";

    public static void main(String[] args) throws Exception {
        File loggerDir = createLoggerDir();
        String configFilePath = loggerDir.getPath() + File.separator + CONFIG_FILE_NAME;
        File configFile = new File(configFilePath);
        createFile(configFile, false);
        System.setProperty("java.util.logging.config.file", configFilePath);
        List<FileHandler> fileHandlers = new ArrayList<>();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(MAX_LOCK_PROPERTY);
            writer.flush();
            // 200 raises the default limit of 100, we try 102 times
            for (int i = 0; i < 102; i++) {
                fileHandlers.add(new FileHandler(loggerDir.getPath() + File.separator + "test_%u.log"));
            }
        } catch (IOException ie) {
            throw new RuntimeException("Test Failed: " + ie.getMessage());
        } finally {
            for (FileHandler fh : fileHandlers) {
                fh.close();
            }
            FileUtils.deleteFileTreeWithRetry(Paths.get(loggerDir.getPath()));
        }
    }

    /**
     * Create a writable directory in user directory for the test
     *
     * @return writable directory created that needs to be deleted when done
     * @throws RuntimeException
     */
    private static File createLoggerDir() throws RuntimeException {
        String userDir = System.getProperty("user.dir", ".");
        File loggerDir = new File(userDir, LOGGER_DIR);
        if (!createFile(loggerDir, true)) {
            throw new RuntimeException("Test failed: unable to create"
                    + " writable working directory "
                    + loggerDir.getAbsolutePath());
        }
        // System.out.println("Created Logger Directory: " + loggerDir.getPath());
        return loggerDir;
    }

    /**
     * @param newFile  File to be created
     * @param makeDirectory  is File to be created is directory
     * @return true if file already exists or creation succeeded
     */
    private static boolean createFile(File newFile, boolean makeDirectory) {
        if (newFile.exists()) {
            return true;
        }
        if (makeDirectory) {
            return newFile.mkdir();
        } else {
            try {
                return newFile.createNewFile();
            } catch (IOException ie) {
                System.err.println("Not able to create file: " + newFile
                        + ", IOException: " + ie.getMessage());
                return false;
            }
        }
    }
}
