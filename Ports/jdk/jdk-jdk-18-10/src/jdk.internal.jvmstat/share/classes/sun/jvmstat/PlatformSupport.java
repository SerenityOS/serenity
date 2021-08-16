/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package sun.jvmstat;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.List;
import jdk.internal.vm.VMSupport;

/*
 * Support routines handling temp directory locating
 * and process ID extraction.
 */
public class PlatformSupport {
    private static final String tmpDirName;
    static {
        /*
         * For this to work, the target VM and this code need to use
         * the same directory. Instead of guessing which directory the
         * VM is using, we will ask.
         */
        String tmpdir = VMSupport.getVMTemporaryDirectory();

        /*
         * Assure that the string returned has a trailing File.separator
         * character. This check was added because the Linux implementation
         * changed such that the java.io.tmpdir string no longer terminates
         * with a File.separator character.
         */
        if (tmpdir.lastIndexOf(File.separator) != (tmpdir.length()-1)) {
            tmpdir = tmpdir + File.separator;
        }
        tmpDirName = tmpdir;
    }

    public static PlatformSupport getInstance() {
        try {
            Class<?> c = Class.forName("sun.jvmstat.PlatformSupportImpl");
            @SuppressWarnings("unchecked")
            Constructor<PlatformSupport> cntr = (Constructor<PlatformSupport>) c.getConstructor();
            return cntr.newInstance();
        } catch (ClassNotFoundException e) {
            return new PlatformSupport();
        } catch (ReflectiveOperationException e) {
            throw new InternalError(e);
        }
    }

    // package-private
    PlatformSupport() {}

    /*
     * Return the OS specific temporary directory
     */
    public static String getTemporaryDirectory() {
        return tmpDirName;
    }

    /*
     * Return a list of the temporary directories that the VM uses
     * for the attach and perf data files.  This function returns
     * the traditional temp directory in addition to any paths
     * accessible by the host which map to temp directories used
     * by containers. The container functionality is only currently
     * supported on Linux platforms.
     *
     * It is important that this directory is well-known and the
     * same for all VM instances. It cannot be affected by configuration
     * variables such as java.io.tmpdir.
     */
    public List<String> getTemporaryDirectories(int vmid) {
        // Add the default temporary directory only
        return List.of(tmpDirName);
    }

    /*
     * Extract the host PID from a file path.
     */
    public int getLocalVmId(File file) throws NumberFormatException {
        return Integer.parseInt(file.getName());
    }


    /*
     * Return the inner most namespaced PID if there is one,
     * otherwise return the original PID.
     */
    public int getNamespaceVmId(int pid) {
        return pid;
    }
}
