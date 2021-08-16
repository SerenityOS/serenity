/*
 * Copyright (c) 2020, Red Hat Inc.
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

package jdk.internal.platform;

/**
 * Data structure to hold info from /proc/self/cgroup,
 * /proc/cgroups and /proc/self/mountinfo
 *
 * man 7 cgroups
 *
 * @see CgroupSubsystemFactory
 */
public class CgroupInfo {

    private final String name;
    private final int hierarchyId;
    private final boolean enabled;
    private String mountPoint;
    private String mountRoot;
    private String cgroupPath;

    private CgroupInfo(String name, int hierarchyId, boolean enabled) {
        this.name = name;
        this.hierarchyId = hierarchyId;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public int getHierarchyId() {
        return hierarchyId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getMountPoint() {
        return mountPoint;
    }

    public void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint;
    }

    public String getMountRoot() {
        return mountRoot;
    }

    public void setMountRoot(String mountRoot) {
        this.mountRoot = mountRoot;
    }

    public String getCgroupPath() {
        return cgroupPath;
    }

    public void setCgroupPath(String cgroupPath) {
        this.cgroupPath = cgroupPath;
    }

    /*
     * Creates a CgroupInfo instance from a line in /proc/cgroups.
     * Comment token (hash) is handled by the caller.
     *
     * Example (annotated):
     *
     * #subsys_name     hierarchy       num_cgroups     enabled
     * cpuset           10              1               1         (a)
     * cpu              7               8               1         (b)
     * [...]
     *
     * Line (a) would yield:
     *   info = new CgroupInfo("cpuset", 10, true);
     *   return info;
     * Line (b) results in:
     *   info = new CgroupInfo("cpu", 7, true);
     *   return info;
     *
     *
     * See CgroupSubsystemFactory.determineType()
     *
     */
    static CgroupInfo fromCgroupsLine(String line) {
        String[] tokens = line.split("\\s+");
        if (tokens.length != 4) {
            return null;
        }
        // discard 3'rd field, num_cgroups
        return new CgroupInfo(tokens[0] /* name */,
                              Integer.parseInt(tokens[1]) /* hierarchyId */,
                              (Integer.parseInt(tokens[3]) == 1) /* enabled */);
    }

}
