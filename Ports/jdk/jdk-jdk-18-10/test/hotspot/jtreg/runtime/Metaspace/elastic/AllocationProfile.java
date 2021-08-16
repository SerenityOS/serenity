/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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
 *
 */

import java.util.*;

public class AllocationProfile {

    final String name;

    // Allocation word size spread
    public final long minimumSingleAllocationSize;
    public final long maximumSingleAllocationSize;

    // dealloc probability [0.0 .. 1.0]
    public final double randomDeallocProbability;

    public AllocationProfile(String name, long minimumSingleAllocationSize, long maximumSingleAllocationSize, double randomDeallocProbability) {
        this.minimumSingleAllocationSize = minimumSingleAllocationSize;
        this.maximumSingleAllocationSize = maximumSingleAllocationSize;
        this.randomDeallocProbability = randomDeallocProbability;
        this.name = name;
    }

    public long randomAllocationSize() {
        Random r = new Random();
        return r.nextInt((int)(maximumSingleAllocationSize - minimumSingleAllocationSize + 1)) + minimumSingleAllocationSize;
    }

    // Some standard profiles
    static final List<AllocationProfile> standardProfiles = new ArrayList<>();

    static {
        standardProfiles.add(new AllocationProfile("medium-range",1, 2048, 0.15));
        standardProfiles.add(new AllocationProfile("small-blocks",1, 512, 0.15));
        standardProfiles.add(new AllocationProfile("micro-blocks",1, 32, 0.15));
    }

    static AllocationProfile randomProfile() {
        return standardProfiles.get(RandomHelper.random().nextInt(standardProfiles.size()));
    }

    @Override
    public String toString() {
        return "MetaspaceTestAllocationProfile{" +
                "name='" + name + '\'' +
                ", minimumSingleAllocationSize=" + minimumSingleAllocationSize +
                ", maximumSingleAllocationSize=" + maximumSingleAllocationSize +
                ", randomDeallocProbability=" + randomDeallocProbability +
                '}';
    }

}
