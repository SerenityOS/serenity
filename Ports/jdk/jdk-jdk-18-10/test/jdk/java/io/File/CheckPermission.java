/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8017212
 * @summary Examine methods in File.java that access the file system do the
 *          right permission check when a security manager exists.
 * @run main/othervm -Djava.security.manager=allow CheckPermission
 * @author Dan Xu
 */

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileFilter;
import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CheckPermission {

    private static final String CHECK_PERMISSION_TEST = "CheckPermissionTest";

    public enum FileOperation {
        READ, WRITE, DELETE, EXEC
    }

    static class Checks {
        private List<Permission> permissionsChecked = new ArrayList<>();
        private Set<String> propertiesChecked = new HashSet<>();

        private Map<FileOperation, List<String>> fileOperationChecked =
            new EnumMap<>(FileOperation.class);

        List<Permission> permissionsChecked() {
            return permissionsChecked;
        }

        Set<String> propertiesChecked() {
            return propertiesChecked;
        }

        List<String> fileOperationChecked(FileOperation op) {
            return fileOperationChecked.get(op);
        }

        void addFileOperation(FileOperation op, String file) {
            List<String> opList = fileOperationChecked.get(op);
            if (opList == null) {
                opList = new ArrayList<>();
                fileOperationChecked.put(op, opList);
            }
            opList.add(file);
        }
    }

    static ThreadLocal<Checks> myChecks = new ThreadLocal<>();

    static void prepare() {
        myChecks.set(new Checks());
    }

    static class LoggingSecurityManager extends SecurityManager {
        static void install() {
            System.setSecurityManager(new LoggingSecurityManager());
        }

        private void checkFileOperation(FileOperation op, String file) {
            Checks checks = myChecks.get();
            if (checks != null)
                checks.addFileOperation(op, file);
        }

        @Override
        public void checkRead(String file) {
            checkFileOperation(FileOperation.READ, file);
        }

        @Override
        public void checkWrite(String file) {
            checkFileOperation(FileOperation.WRITE, file);
        }

        @Override
        public void checkDelete(String file) {
            checkFileOperation(FileOperation.DELETE, file);
        }

        @Override
        public void checkExec(String file) {
            checkFileOperation(FileOperation.EXEC, file);
        }

        @Override
        public void checkPermission(Permission perm) {
            Checks checks = myChecks.get();
            if (checks != null)
                checks.permissionsChecked().add(perm);
        }

        @Override
        public void checkPropertyAccess(String key) {
            Checks checks = myChecks.get();
            if (checks != null)
                checks.propertiesChecked().add(key);
        }
    }

    static void assertCheckPermission(Class<? extends Permission> type,
            String name)
    {
        for (Permission perm : myChecks.get().permissionsChecked()) {
            if (type.isInstance(perm) && perm.getName().equals(name))
                return;
        }
        throw new RuntimeException(type.getName() + "(\"" + name
            + "\") not checked");
    }

    static void assertCheckPropertyAccess(String key) {
        if (!myChecks.get().propertiesChecked().contains(key))
            throw new RuntimeException("Property " + key + " not checked");
    }

    static void assertChecked(File file, List<String> list) {
        if (list != null && !list.isEmpty()) {
            for (String path : list) {
                if (path.equals(file.getPath()))
                    return;
            }
        }
        throw new RuntimeException("Access not checked");
    }

    static void assertNotChecked(File file, List<String> list) {
        if (list != null && !list.isEmpty()) {
            for (String path : list) {
                if (path.equals(file.getPath()))
                    throw new RuntimeException("Access checked");
            }
        }
    }

    static void assertCheckOperation(File file, Set<FileOperation> ops) {
        for (FileOperation op : ops)
            assertChecked(file, myChecks.get().fileOperationChecked(op));
    }

    static void assertNotCheckOperation(File file, Set<FileOperation> ops) {
        for (FileOperation op : ops)
            assertNotChecked(file, myChecks.get().fileOperationChecked(op));
    }

    static void assertOnlyCheckOperation(File file,
            EnumSet<FileOperation> ops)
    {
        assertCheckOperation(file, ops);
        assertNotCheckOperation(file, EnumSet.complementOf(ops));
    }

    static File testFile, another;

    static void setup() {
        testFile = new File(CHECK_PERMISSION_TEST + System.currentTimeMillis());
        if (testFile.exists()) {
            testFile.delete();
        }

        another = new File(CHECK_PERMISSION_TEST + "Another"
                           + System.currentTimeMillis());
        if (another.exists()) {
            another.delete();
        }

        LoggingSecurityManager.install();
    }

    public static void main(String[] args) throws IOException {
        setup();

        prepare();
        testFile.canRead();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.READ));

        prepare();
        testFile.canWrite();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.WRITE));

        prepare();
        testFile.exists();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.READ));

        prepare();
        testFile.isDirectory();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.READ));

        prepare();
        testFile.isFile();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.READ));

        prepare();
        testFile.isHidden();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.READ));

        prepare();
        testFile.lastModified();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.READ));

        prepare();
        testFile.length();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.READ));

        prepare();
        testFile.createNewFile();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.WRITE));

        prepare();
        testFile.list();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.READ));

        prepare();
        testFile.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return false;
            }
        });
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.READ));

        prepare();
        testFile.listFiles();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.READ));

        prepare();
        testFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return false;
            }
        });
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.READ));

        prepare();
        testFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return false;
            }
        });
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.READ));

        prepare();
        testFile.mkdir();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.WRITE));

        if (testFile.exists()) {
            prepare();
            testFile.mkdirs();
            assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.READ));
        }

        if (!another.exists()) {
            prepare();
            another.mkdirs();
            assertOnlyCheckOperation(another,
                    EnumSet.of(FileOperation.READ, FileOperation.WRITE));
        }

        prepare();
        another.delete();
        assertOnlyCheckOperation(another, EnumSet.of(FileOperation.DELETE));

        prepare();
        boolean renRst = testFile.renameTo(another);
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.WRITE));
        assertOnlyCheckOperation(another, EnumSet.of(FileOperation.WRITE));

        if (renRst) {
            if (testFile.exists())
                throw new RuntimeException(testFile + " is already renamed to "
                    + another);
            testFile = another;
        }

        prepare();
        testFile.setLastModified(0);
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.WRITE));

        prepare();
        testFile.setReadOnly();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.WRITE));

        prepare();
        testFile.setWritable(true, true);
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.WRITE));

        prepare();
        testFile.setWritable(true);
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.WRITE));

        prepare();
        testFile.setReadable(true, true);
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.WRITE));

        prepare();
        testFile.setReadable(true);
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.WRITE));

        prepare();
        testFile.setExecutable(true, true);
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.WRITE));

        prepare();
        testFile.setExecutable(true);
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.WRITE));

        prepare();
        testFile.canExecute();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.EXEC));

        prepare();
        testFile.getTotalSpace();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.READ));
        assertCheckPermission(RuntimePermission.class,
                "getFileSystemAttributes");

        prepare();
        testFile.getFreeSpace();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.READ));
        assertCheckPermission(RuntimePermission.class,
                "getFileSystemAttributes");

        prepare();
        testFile.getUsableSpace();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.READ));
        assertCheckPermission(RuntimePermission.class,
                "getFileSystemAttributes");

        prepare();
        File tmpFile = File.createTempFile(CHECK_PERMISSION_TEST, null);
        assertOnlyCheckOperation(tmpFile, EnumSet.of(FileOperation.WRITE));
        tmpFile.delete();
        assertCheckOperation(tmpFile, EnumSet.of(FileOperation.DELETE));

        prepare();
        tmpFile = File.createTempFile(CHECK_PERMISSION_TEST, null, null);
        assertOnlyCheckOperation(tmpFile, EnumSet.of(FileOperation.WRITE));
        tmpFile.delete();
        assertCheckOperation(tmpFile, EnumSet.of(FileOperation.DELETE));

        prepare();
        testFile.deleteOnExit();
        assertOnlyCheckOperation(testFile, EnumSet.of(FileOperation.DELETE));
    }
}
