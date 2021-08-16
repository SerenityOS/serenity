/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "memory/allocation.hpp"
#include "runtime/synchronizer.hpp"
#include "runtime/vm_version.hpp"
#include "unittest.hpp"

class SynchronizerTest : public ::testing::Test {
  public:
    static u_char* get_gvars_addr() { return ObjectSynchronizer::get_gvars_addr(); }
    static u_char* get_gvars_hc_sequence_addr() { return ObjectSynchronizer::get_gvars_hc_sequence_addr(); }
    static size_t get_gvars_size() { return ObjectSynchronizer::get_gvars_size(); }
    static u_char* get_gvars_stw_random_addr() { return ObjectSynchronizer::get_gvars_stw_random_addr(); }
};

TEST_VM(SynchronizerTest, sanity) {
  uint cache_line_size = VM_Version::L1_data_cache_line_size();
  if (cache_line_size != 0) {
    // We were able to determine the L1 data cache line size so
    // do some cache line specific sanity checks

    u_char *addr_begin = SynchronizerTest::get_gvars_addr();
    u_char *addr_stw_random = SynchronizerTest::get_gvars_stw_random_addr();
    u_char *addr_hc_sequence = SynchronizerTest::get_gvars_hc_sequence_addr();
    size_t gvars_size = SynchronizerTest::get_gvars_size();

    uint offset_stw_random = (uint) (addr_stw_random - addr_begin);
    uint offset_hc_sequence = (uint) (addr_hc_sequence - addr_begin);
    uint offset_hc_sequence_stw_random = offset_hc_sequence - offset_stw_random;
    uint offset_hc_sequence_struct_end = (uint) gvars_size - offset_hc_sequence;

    EXPECT_GE(offset_stw_random, cache_line_size)
            << "the SharedGlobals.stw_random field is closer "
            << "to the struct beginning than a cache line which permits "
            << "false sharing.";

    EXPECT_GE(offset_hc_sequence_stw_random, cache_line_size)
            << "the SharedGlobals.stw_random and "
            << "SharedGlobals.hc_sequence fields are closer than a cache "
            << "line which permits false sharing.";

    EXPECT_GE(offset_hc_sequence_struct_end, cache_line_size)
            << "the SharedGlobals.hc_sequence field is closer "
            << "to the struct end than a cache line which permits false "
            << "sharing.";
  }
}
