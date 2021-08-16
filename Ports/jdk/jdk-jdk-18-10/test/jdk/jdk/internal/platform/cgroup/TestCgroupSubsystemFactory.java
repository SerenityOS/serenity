/*
 * Copyright (c) 2020, Red Hat Inc.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jdk.internal.platform.CgroupInfo;
import jdk.internal.platform.CgroupSubsystemFactory;
import jdk.internal.platform.CgroupSubsystemFactory.CgroupTypeResult;
import jdk.internal.platform.CgroupV1MetricsImpl;
import jdk.internal.platform.cgroupv1.CgroupV1Subsystem;
import jdk.internal.platform.Metrics;
import jdk.test.lib.Utils;
import jdk.test.lib.util.FileUtils;


/*
 * @test
 * @key cgroups
 * @requires os.family == "linux"
 * @modules java.base/jdk.internal.platform
 *          java.base/jdk.internal.platform.cgroupv1
 * @library /test/lib
 * @run junit/othervm TestCgroupSubsystemFactory
 */
public class TestCgroupSubsystemFactory {

    private Path existingDirectory;
    private Path cgroupv1CgroupsJoinControllers;
    private Path cgroupv1MountInfoJoinControllers;
    private Path cgroupv1CgInfoZeroHierarchy;
    private Path cgroupv1MntInfoZeroHierarchy;
    private Path cgroupv2CgInfoZeroHierarchy;
    private Path cgroupv2MntInfoZeroHierarchy;
    private Path cgroupv1CgInfoNonZeroHierarchy;
    private Path cgroupv1MntInfoNonZeroHierarchy;
    private Path cgroupv1MntInfoSystemdOnly;
    private Path cgroupv1MntInfoDoubleCpusets;
    private Path cgroupv1MntInfoDoubleCpusets2;
    private Path cgroupv1SelfCgroup;
    private Path cgroupv2SelfCgroup;
    private Path cgroupv1SelfCgroupJoinCtrl;
    private Path cgroupv1CgroupsOnlyCPUCtrl;
    private Path cgroupv1SelfCgroupsOnlyCPUCtrl;
    private Path cgroupv1MountInfoCgroupsOnlyCPUCtrl;
    private String mntInfoEmpty = "";
    private String cgroupsNonZeroJoinControllers =
            "#subsys_name hierarchy num_cgroups enabled\n" +
            "cpuset\t3\t1\t1\n" +
            "cpu\t4\t153\t1\n" +
            "cpuacct\t4\t153\t1\n" +
            "blkio\t7\t87\t1\n" +
            "memory\t4\t153\t1\n" +
            "devices\t6\t87\t1\n" +
            "freezer\t9\t1\t1\n" +
            "net_cls\t4\t153\t1\n" +
            "perf_event\t2\t1\t1\n" +
            "net_prio\t4\t153\t1\n" +
            "hugetlb\t4\t153\t1\n" +
            "pids\t5\t95\t1\n" +
            "rdma\t8\t1\t1\n";
    private String cgroupsNonZeroCpuControllerOnly =
            "#subsys_name hierarchy num_cgroups enabled\n" +
            "cpu\t4\t153\t1\n" +
            "cpuacct\t4\t153\t1\n";
    private String selfCgroupNonZeroCpuControllerOnly =
            "4:cpu,cpuacct:/user.slice/user-1000.slice/session-3.scope\n";
    private String selfCgroupNonZeroJoinControllers =
            "9:cpuset:/\n" +
            "8:perf_event:/\n" +
            "7:rdma:/\n" +
            "6:freezer:/\n" +
            "5:blkio:/user.slice\n" +
            "4:pids:/user.slice/user-1000.slice/session-3.scope\n" +
            "3:devices:/user.slice\n" +
            "2:cpu,cpuacct,memory,net_cls,net_prio,hugetlb:/user.slice/user-1000.slice/session-3.scope\n" +
            "1:name=systemd:/user.slice/user-1000.slice/session-3.scope\n" +
            "0::/user.slice/user-1000.slice/session-3.scope\n";
    private String cgroupsZeroHierarchy =
            "#subsys_name hierarchy num_cgroups enabled\n" +
            "cpuset 0 1 1\n" +
            "cpu 0 1 1\n" +
            "cpuacct 0 1 1\n" +
            "memory 0 1 1\n" +
            "devices 0 1 1\n" +
            "freezer 0 1 1\n" +
            "net_cls 0 1 1\n" +
            "blkio 0 1 1\n" +
            "perf_event 0 1 1 ";
    private String mntInfoHybrid =
            "30 23 0:26 / /sys/fs/cgroup ro,nosuid,nodev,noexec shared:4 - tmpfs tmpfs ro,seclabel,mode=755\n" +
            "31 30 0:27 / /sys/fs/cgroup/unified rw,nosuid,nodev,noexec,relatime shared:5 - cgroup2 none rw,seclabel,nsdelegate\n" +
            "32 30 0:28 / /sys/fs/cgroup/systemd rw,nosuid,nodev,noexec,relatime shared:6 - cgroup none rw,seclabel,xattr,name=systemd\n" +
            "35 30 0:31 / /sys/fs/cgroup/memory rw,nosuid,nodev,noexec,relatime shared:7 - cgroup none rw,seclabel,memory\n" +
            "36 30 0:32 / /sys/fs/cgroup/pids rw,nosuid,nodev,noexec,relatime shared:8 - cgroup none rw,seclabel,pids\n" +
            "37 30 0:33 / /sys/fs/cgroup/perf_event rw,nosuid,nodev,noexec,relatime shared:9 - cgroup none rw,seclabel,perf_event\n" +
            "38 30 0:34 / /sys/fs/cgroup/net_cls,net_prio rw,nosuid,nodev,noexec,relatime shared:10 - cgroup none rw,seclabel,net_cls,net_prio\n" +
            "39 30 0:35 / /sys/fs/cgroup/hugetlb rw,nosuid,nodev,noexec,relatime shared:11 - cgroup none rw,seclabel,hugetlb\n" +
            "40 30 0:36 / /sys/fs/cgroup/cpu,cpuacct rw,nosuid,nodev,noexec,relatime shared:12 - cgroup none rw,seclabel,cpu,cpuacct\n" +
            "41 30 0:37 / /sys/fs/cgroup/devices rw,nosuid,nodev,noexec,relatime shared:13 - cgroup none rw,seclabel,devices\n" +
            "42 30 0:38 / /sys/fs/cgroup/cpuset rw,nosuid,nodev,noexec,relatime shared:14 - cgroup none rw,seclabel,cpuset\n" +
            "43 30 0:39 / /sys/fs/cgroup/blkio rw,nosuid,nodev,noexec,relatime shared:15 - cgroup none rw,seclabel,blkio\n" +
            "44 30 0:40 / /sys/fs/cgroup/freezer rw,nosuid,nodev,noexec,relatime shared:16 - cgroup none rw,seclabel,freezer\n";
    private String mntInfoCpuOnly =
            "30 23 0:26 / /sys/fs/cgroup ro,nosuid,nodev,noexec shared:4 - tmpfs tmpfs ro,seclabel,mode=755\n" +
            "40 30 0:36 / /sys/fs/cgroup/cpu,cpuacct rw,nosuid,nodev,noexec,relatime shared:12 - cgroup none rw,seclabel,cpu,cpuacct\n";
    private String mntInfoCgroupv1JoinControllers =
            "31 22 0:26 / /sys/fs/cgroup ro,nosuid,nodev,noexec shared:9 - tmpfs tmpfs ro,mode=755\n" +
            "32 31 0:27 / /sys/fs/cgroup/unified rw,nosuid,nodev,noexec,relatime shared:10 - cgroup2 cgroup2 rw,nsdelegate\n" +
            "33 31 0:28 / /sys/fs/cgroup/systemd rw,nosuid,nodev,noexec,relatime shared:11 - cgroup cgroup rw,xattr,name=systemd\n" +
            "36 31 0:31 / /sys/fs/cgroup/perf_event rw,nosuid,nodev,noexec,relatime shared:15 - cgroup cgroup rw,perf_event\n" +
            "37 31 0:32 / /sys/fs/cgroup/cpuset rw,nosuid,nodev,noexec,relatime shared:16 - cgroup cgroup rw,cpuset\n" +
            "38 31 0:33 / /sys/fs/cgroup/cpu,cpuacct,net_cls,net_prio,hugetlb,memory rw,nosuid,nodev,noexec,relatime shared:17 - cgroup cgroup rw,cpu,cpuacct,memory,net_cls,net_prio,hugetlb\n" +
            "39 31 0:34 / /sys/fs/cgroup/pids rw,nosuid,nodev,noexec,relatime shared:18 - cgroup cgroup rw,pids\n" +
            "40 31 0:35 / /sys/fs/cgroup/devices rw,nosuid,nodev,noexec,relatime shared:19 - cgroup cgroup rw,devices\n" +
            "41 31 0:36 / /sys/fs/cgroup/blkio rw,nosuid,nodev,noexec,relatime shared:20 - cgroup cgroup rw,blkio\n" +
            "42 31 0:37 / /sys/fs/cgroup/rdma rw,nosuid,nodev,noexec,relatime shared:21 - cgroup cgroup rw,rdma\n" +
            "43 31 0:38 / /sys/fs/cgroup/freezer rw,nosuid,nodev,noexec,relatime shared:22 - cgroup cgroup rw,freezer\n";
    private String cgroupsNonZeroHierarchy =
            "#subsys_name hierarchy   num_cgroups enabled\n" +
            "cpuset  9   1   1\n" +
            "cpu 7   1   1\n" +
            "cpuacct 7   1   1\n" +
            "blkio   10  1   1\n" +
            "memory  2   90  1\n" +
            "devices 8   74  1\n" +
            "freezer 11  1   1\n" +
            "net_cls 5   1   1\n" +
            "perf_event  4   1   1\n" +
            "net_prio    5   1   1\n" +
            "hugetlb 6   1   1\n" +
            "pids    3   80  1";
    private String mntInfoCgroupsV2Only =
            "28 21 0:25 / /sys/fs/cgroup rw,nosuid,nodev,noexec,relatime shared:4 - cgroup2 none rw,seclabel,nsdelegate";
    private String mntInfoCgroupsV1SystemdOnly =
            "35 26 0:26 / /sys/fs/cgroup/systemd rw,nosuid,nodev,noexec,relatime - cgroup systemd rw,name=systemd\n" +
            "26 18 0:19 / /sys/fs/cgroup rw,relatime - tmpfs none rw,size=4k,mode=755\n";
    private String mntInfoCgroupv1MoreCpusetLine = "121 32 0:37 / /cpuset rw,relatime shared:69 - cgroup none rw,cpuset\n";
    private String mntInfoCgroupsV1DoubleCpuset = mntInfoHybrid + mntInfoCgroupv1MoreCpusetLine;
    private String mntInfoCgroupsV1DoubleCpuset2 = mntInfoCgroupv1MoreCpusetLine + mntInfoHybrid;
    private String cgroupv1SelfCgroupContent = "11:memory:/user.slice/user-1000.slice/user@1000.service\n" +
            "10:hugetlb:/\n" +
            "9:cpuset:/\n" +
            "8:pids:/user.slice/user-1000.slice/user@1000.service\n" +
            "7:freezer:/\n" +
            "6:blkio:/\n" +
            "5:net_cls,net_prio:/\n" +
            "4:devices:/user.slice\n" +
            "3:perf_event:/\n" +
            "2:cpu,cpuacct:/\n" +
            "1:name=systemd:/user.slice/user-1000.slice/user@1000.service/apps.slice/apps-org.gnome.Terminal.slice/vte-spawn-3c00b338-5b65-439f-8e97-135e183d135d.scope\n" +
            "0::/user.slice/user-1000.slice/user@1000.service/apps.slice/apps-org.gnome.Terminal.slice/vte-spawn-3c00b338-5b65-439f-8e97-135e183d135d.scope\n";
    private String cgroupv2SelfCgroupContent = "0::/user.slice/user-1000.slice/session-2.scope";

    @Before
    public void setup() {
        try {
            existingDirectory = Utils.createTempDirectory(TestCgroupSubsystemFactory.class.getSimpleName());
            Path cgroupsZero = Paths.get(existingDirectory.toString(), "cgroups_zero");
            Files.writeString(cgroupsZero, cgroupsZeroHierarchy, StandardCharsets.UTF_8);
            cgroupv1CgInfoZeroHierarchy = cgroupsZero;
            cgroupv2CgInfoZeroHierarchy = cgroupsZero;
            cgroupv1MntInfoZeroHierarchy = Paths.get(existingDirectory.toString(), "mountinfo_empty");
            Files.writeString(cgroupv1MntInfoZeroHierarchy, mntInfoEmpty);

            cgroupv2MntInfoZeroHierarchy = Paths.get(existingDirectory.toString(), "mountinfo_cgroupv2");
            Files.writeString(cgroupv2MntInfoZeroHierarchy, mntInfoCgroupsV2Only);

            cgroupv1CgInfoNonZeroHierarchy = Paths.get(existingDirectory.toString(), "cgroups_non_zero");
            Files.writeString(cgroupv1CgInfoNonZeroHierarchy, cgroupsNonZeroHierarchy);

            cgroupv1MntInfoNonZeroHierarchy = Paths.get(existingDirectory.toString(), "mountinfo_non_zero");
            Files.writeString(cgroupv1MntInfoNonZeroHierarchy, mntInfoHybrid);

            cgroupv1MntInfoSystemdOnly = Paths.get(existingDirectory.toString(), "mountinfo_cgroupv1_systemd_only");
            Files.writeString(cgroupv1MntInfoSystemdOnly, mntInfoCgroupsV1SystemdOnly);

            cgroupv1MntInfoDoubleCpusets = Paths.get(existingDirectory.toString(), "mountinfo_cgroupv1_double_cpuset");
            Files.writeString(cgroupv1MntInfoDoubleCpusets, mntInfoCgroupsV1DoubleCpuset);

            cgroupv1MntInfoDoubleCpusets2 = Paths.get(existingDirectory.toString(), "mountinfo_cgroupv1_double_cpuset2");
            Files.writeString(cgroupv1MntInfoDoubleCpusets2, mntInfoCgroupsV1DoubleCpuset2);

            cgroupv1CgroupsJoinControllers = Paths.get(existingDirectory.toString(), "cgroups_cgv1_join_controllers");
            Files.writeString(cgroupv1CgroupsJoinControllers, cgroupsNonZeroJoinControllers);

            cgroupv1MountInfoJoinControllers = Paths.get(existingDirectory.toString(), "mntinfo_cgv1_join_controllers");
            Files.writeString(cgroupv1MountInfoJoinControllers, mntInfoCgroupv1JoinControllers);

            cgroupv1SelfCgroup = Paths.get(existingDirectory.toString(), "self_cgroup_cgv1");
            Files.writeString(cgroupv1SelfCgroup, cgroupv1SelfCgroupContent);

            cgroupv2SelfCgroup = Paths.get(existingDirectory.toString(), "self_cgroup_cgv2");
            Files.writeString(cgroupv2SelfCgroup, cgroupv2SelfCgroupContent);

            cgroupv1SelfCgroupJoinCtrl = Paths.get(existingDirectory.toString(), "self_cgroup_cgv1_join_controllers");
            Files.writeString(cgroupv1SelfCgroupJoinCtrl, selfCgroupNonZeroJoinControllers);

            cgroupv1CgroupsOnlyCPUCtrl = Paths.get(existingDirectory.toString(), "cgroups_cpu_only_controller");
            Files.writeString(cgroupv1CgroupsOnlyCPUCtrl, cgroupsNonZeroCpuControllerOnly);

            cgroupv1SelfCgroupsOnlyCPUCtrl = Paths.get(existingDirectory.toString(), "self_cgroup_cpu_only_controller");
            Files.writeString(cgroupv1SelfCgroupsOnlyCPUCtrl, selfCgroupNonZeroCpuControllerOnly);

            cgroupv1MountInfoCgroupsOnlyCPUCtrl = Paths.get(existingDirectory.toString(), "self_mountinfo_cpu_only_controller");
            Files.writeString(cgroupv1MountInfoCgroupsOnlyCPUCtrl, mntInfoCpuOnly);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void teardown() {
        try {
            FileUtils.deleteFileTreeWithRetry(existingDirectory);
        } catch (IOException e) {
            System.err.println("Teardown failed. " + e.getMessage());
        }
    }

    @Test
    public void testCgroupv1CpuControllerOnly() throws IOException {
        String cgroups = cgroupv1CgroupsOnlyCPUCtrl.toString();
        String mountInfo = cgroupv1MountInfoCgroupsOnlyCPUCtrl.toString();
        String selfCgroup = cgroupv1SelfCgroupsOnlyCPUCtrl.toString();
        Optional<CgroupTypeResult> result = CgroupSubsystemFactory.determineType(mountInfo, cgroups, selfCgroup);

        assertTrue("Expected non-empty cgroup result", result.isPresent());
        CgroupTypeResult res = result.get();
        assertFalse("Expected cgroup v1", res.isCgroupV2());
        Map<String, CgroupInfo> infos = res.getInfos();
        assertNull("Memory controller expected null", infos.get("memory"));
        assertNotNull("Cpu controller expected non-null", infos.get("cpu"));

        // cgroup v1 tests only as this isn't possible with unified hierarchy
        // where all controllers have the same mount point
        CgroupV1Subsystem subsystem = CgroupV1Subsystem.getInstance(infos);
        // This throws NPEs prior JDK-8257746
        long val = subsystem.getMemoryAndSwapLimit();
        assertEquals("expected unlimited, and no NPE", -1, val);
        val = subsystem.getMemoryAndSwapFailCount();
        assertEquals("expected unlimited, and no NPE", -1, val);
        val = subsystem.getMemoryAndSwapMaxUsage();
        assertEquals("expected unlimited, and no NPE", -1, val);
        val = subsystem.getMemoryAndSwapUsage();
        assertEquals("expected unlimited, and no NPE", -1, val);
    }

    @Test
    public void testCgroupv1JoinControllerCombo() throws IOException {
        String cgroups = cgroupv1CgroupsJoinControllers.toString();
        String mountInfo = cgroupv1MountInfoJoinControllers.toString();
        String selfCgroup = cgroupv1SelfCgroupJoinCtrl.toString();
        Optional<CgroupTypeResult> result = CgroupSubsystemFactory.determineType(mountInfo, cgroups, selfCgroup);

        assertTrue("Expected non-empty cgroup result", result.isPresent());
        CgroupTypeResult res = result.get();
        assertFalse("Join controller combination expected as cgroups v1", res.isCgroupV2());
        CgroupInfo memoryInfo = res.getInfos().get("memory");
        assertEquals("/user.slice/user-1000.slice/session-3.scope", memoryInfo.getCgroupPath());
    }

    @Test
    public void testCgroupv1SystemdOnly() throws IOException {
        String cgroups = cgroupv1CgInfoZeroHierarchy.toString();
        String mountInfo = cgroupv1MntInfoSystemdOnly.toString();
        String selfCgroup = cgroupv1SelfCgroup.toString(); // Content doesn't matter
        Optional<CgroupTypeResult> result = CgroupSubsystemFactory.determineType(mountInfo, cgroups, selfCgroup);

        assertTrue("zero hierarchy ids with no *relevant* controllers mounted", result.isEmpty());
    }

    @Test
    public void testCgroupv1MultipleCpusetMounts() throws IOException {
        doMultipleCpusetMountsTest(cgroupv1MntInfoDoubleCpusets);
        doMultipleCpusetMountsTest(cgroupv1MntInfoDoubleCpusets2);
    }

    private void doMultipleCpusetMountsTest(Path info) throws IOException {
        String cgroups = cgroupv1CgInfoNonZeroHierarchy.toString();
        String mountInfo = info.toString();
        String selfCgroup = cgroupv1SelfCgroup.toString();
        Optional<CgroupTypeResult> result = CgroupSubsystemFactory.determineType(mountInfo, cgroups, selfCgroup);

        assertTrue("Expected non-empty cgroup result", result.isPresent());
        CgroupTypeResult res = result.get();
        assertFalse("Duplicate cpusets should not influence detection heuristic", res.isCgroupV2());
        CgroupInfo cpuSetInfo = res.getInfos().get("cpuset");
        assertEquals("/sys/fs/cgroup/cpuset", cpuSetInfo.getMountPoint());
        assertEquals("/", cpuSetInfo.getMountRoot());
    }

    @Test
    public void testHybridCgroupsV1() throws IOException {
        String cgroups = cgroupv1CgInfoNonZeroHierarchy.toString();
        String mountInfo = cgroupv1MntInfoNonZeroHierarchy.toString();
        String selfCgroup = cgroupv1SelfCgroup.toString();
        Optional<CgroupTypeResult> result = CgroupSubsystemFactory.determineType(mountInfo, cgroups, selfCgroup);

        assertTrue("Expected non-empty cgroup result", result.isPresent());
        CgroupTypeResult res = result.get();
        assertFalse("hybrid hierarchy expected as cgroups v1", res.isCgroupV2());
        CgroupInfo memoryInfo = res.getInfos().get("memory");
        assertEquals("/user.slice/user-1000.slice/user@1000.service", memoryInfo.getCgroupPath());
        assertEquals("/", memoryInfo.getMountRoot());
        assertEquals("/sys/fs/cgroup/memory", memoryInfo.getMountPoint());
    }

    @Test
    public void testZeroHierarchyCgroupsV1() throws IOException {
        String cgroups = cgroupv1CgInfoZeroHierarchy.toString();
        String mountInfo = cgroupv1MntInfoZeroHierarchy.toString();
        String selfCgroup = cgroupv1SelfCgroup.toString(); // Content doesn't matter
        Optional<CgroupTypeResult> result = CgroupSubsystemFactory.determineType(mountInfo, cgroups, selfCgroup);

        assertTrue("zero hierarchy ids with no mounted controllers => empty result", result.isEmpty());
    }

    @Test
    public void testZeroHierarchyCgroupsV2() throws IOException {
        String cgroups = cgroupv2CgInfoZeroHierarchy.toString();
        String mountInfo = cgroupv2MntInfoZeroHierarchy.toString();
        String selfCgroup = cgroupv2SelfCgroup.toString();
        Optional<CgroupTypeResult> result = CgroupSubsystemFactory.determineType(mountInfo, cgroups, selfCgroup);

        assertTrue("Expected non-empty cgroup result", result.isPresent());
        CgroupTypeResult res = result.get();

        assertTrue("zero hierarchy ids with mounted controllers expected cgroups v2", res.isCgroupV2());
        CgroupInfo memoryInfo = res.getInfos().get("memory");
        assertEquals("/user.slice/user-1000.slice/session-2.scope", memoryInfo.getCgroupPath());
        CgroupInfo cpuInfo = res.getInfos().get("cpu");
        assertEquals(memoryInfo.getCgroupPath(), cpuInfo.getCgroupPath());
        assertEquals(memoryInfo.getMountPoint(), cpuInfo.getMountPoint());
        assertEquals(memoryInfo.getMountRoot(), cpuInfo.getMountRoot());
        assertEquals("/sys/fs/cgroup", cpuInfo.getMountPoint());
    }

    @Test(expected = IOException.class)
    public void mountInfoFileNotFound() throws IOException {
        String cgroups = cgroupv1CgInfoZeroHierarchy.toString(); // any existing file
        String selfCgroup = cgroupv1SelfCgroup.toString(); // any existing file
        String mountInfo = Paths.get(existingDirectory.toString(), "not-existing-mountinfo").toString();

        CgroupSubsystemFactory.determineType(mountInfo, cgroups, selfCgroup);
    }

    @Test(expected = IOException.class)
    public void cgroupsFileNotFound() throws IOException {
        String cgroups = Paths.get(existingDirectory.toString(), "not-existing-cgroups").toString();
        String mountInfo = cgroupv2MntInfoZeroHierarchy.toString(); // any existing file
        String selfCgroup = cgroupv2SelfCgroup.toString(); // any existing file
        CgroupSubsystemFactory.determineType(mountInfo, cgroups, selfCgroup);
    }

    @Test(expected = IOException.class)
    public void selfCgroupsFileNotFound() throws IOException {
        String cgroups = cgroupv1CgInfoZeroHierarchy.toString(); // any existing file
        String mountInfo = cgroupv2MntInfoZeroHierarchy.toString(); // any existing file
        String selfCgroup = Paths.get(existingDirectory.toString(), "not-existing-self-cgroups").toString();
        CgroupSubsystemFactory.determineType(mountInfo, cgroups, selfCgroup);
    }
}
