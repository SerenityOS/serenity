#
# Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

#

# This file identifies the root of the test-suite hierarchy.
# It also contains test-suite configuration information.

# The list of keywords supported in this test suite
# stress:               stress/slow test
# headful:              test can be run only on headful host
# intermittent:         flaky test, known to fail intermittently
# randomness:           test uses randomness, test cases differ from run to run
# cgroups:              test uses cgroups
keys=stress headful intermittent randomness cgroups

groups=TEST.groups TEST.quick-groups

# Source files for classes that will be used at the beginning of each test suite run,
# to determine additional characteristics of the system for use with the @requires tag.
# Note: compiled bootlibs classes will be added to BCP.
requires.extraPropDefns = ../../jtreg-ext/requires/VMProps.java
requires.extraPropDefns.bootlibs = ../../lib/jdk/test/whitebox
requires.extraPropDefns.libs = \
    ../../lib/jdk/test/lib/Platform.java \
    ../../lib/jdk/test/lib/Container.java
requires.extraPropDefns.vmOpts = -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
requires.properties= \
    sun.arch.data.model \
    vm.simpleArch \
    vm.bits \
    vm.flightRecorder \
    vm.gc.G1 \
    vm.gc.Serial \
    vm.gc.Parallel \
    vm.gc.Shenandoah \
    vm.gc.Epsilon \
    vm.gc.Z \
    vm.jvmci \
    vm.emulatedClient \
    vm.cpu.features \
    vm.pageSize \
    vm.debug \
    vm.hasSA \
    vm.hasJFR \
    vm.rtm.cpu \
    vm.rtm.compiler \
    vm.cds \
    vm.cds.custom.loaders \
    vm.cds.archived.java.heap \
    vm.jvmti \
    vm.graal.enabled \
    vm.compiler1.enabled \
    vm.compiler2.enabled \
    vm.musl \
    vm.flagless \
    docker.support \
    jdk.containerized

# Minimum jtreg version
requiredVersion=6+1

# Path to libraries in the topmost test directory. This is needed so @library
# does not need ../../../ notation to reach them
external.lib.roots = ../../../

# Use new module options
useNewOptions=true

# Use --patch-module instead of -Xmodule:
useNewPatchModule=true
