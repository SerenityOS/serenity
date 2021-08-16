/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1HEAPVERIFIER_HPP
#define SHARE_GC_G1_G1HEAPVERIFIER_HPP

#include "gc/g1/heapRegionSet.hpp"
#include "gc/shared/verifyOption.hpp"
#include "memory/allocation.hpp"
#include "utilities/macros.hpp"
#include "utilities/ticks.hpp"

class G1CollectedHeap;

class G1HeapVerifier : public CHeapObj<mtGC> {
private:
  static int _enabled_verification_types;

  G1CollectedHeap* _g1h;

  void verify_region_sets();

public:
  enum G1VerifyType {
    G1VerifyYoungNormal     =  1, // -XX:VerifyGCType=young-normal
    G1VerifyConcurrentStart =  2, // -XX:VerifyGCType=concurrent-start
    G1VerifyMixed           =  4, // -XX:VerifyGCType=mixed
    G1VerifyYoungEvacFail   =  8, // -XX:VerifyGCType=young-evac-fail
    G1VerifyRemark          = 16, // -XX:VerifyGCType=remark
    G1VerifyCleanup         = 32, // -XX:VerifyGCType=cleanup
    G1VerifyFull            = 64, // -XX:VerifyGCType=full
    G1VerifyAll             = -1
  };

  G1HeapVerifier(G1CollectedHeap* heap) : _g1h(heap) {}

  static void enable_verification_type(G1VerifyType type);
  static bool should_verify(G1VerifyType type);

  // Perform verification.

  // vo == UsePrevMarking -> use "prev" marking information,
  // vo == UseNextMarking -> use "next" marking information
  // vo == UseFullMarking -> use "next" marking bitmap but no TAMS
  //
  // NOTE: Only the "prev" marking information is guaranteed to be
  // consistent most of the time, so most calls to this should use
  // vo == UsePrevMarking.
  // Currently, there is only one case where this is called with
  // vo == UseNextMarking, which is to verify the "next" marking
  // information at the end of remark.
  // Currently there is only one place where this is called with
  // vo == UseFullMarking, which is to verify the marking during a
  // full GC.
  void verify(VerifyOption vo);

  // verify_region_sets_optional() is planted in the code for
  // list verification in debug builds.
  void verify_region_sets_optional() { DEBUG_ONLY(verify_region_sets();) }

  void prepare_for_verify();
  void verify(G1VerifyType type, VerifyOption vo, const char* msg);
  void verify_before_gc(G1VerifyType type);
  void verify_after_gc(G1VerifyType type);

#ifndef PRODUCT
  // Make sure that the given bitmap has no marked objects in the
  // range [from,limit). If it does, print an error message and return
  // false. Otherwise, just return true. bitmap_name should be "prev"
  // or "next".
  bool verify_no_bits_over_tams(const char* bitmap_name, const G1CMBitMap* const bitmap,
                                HeapWord* from, HeapWord* limit);

  // Verify that the prev / next bitmap range [tams,end) for the given
  // region has no marks. Return true if all is well, false if errors
  // are detected.
  bool verify_bitmaps(const char* caller, HeapRegion* hr);
#endif // PRODUCT

  // If G1VerifyBitmaps is set, verify that the marking bitmaps for
  // the given region do not have any spurious marks. If errors are
  // detected, print appropriate error messages and crash.
  void check_bitmaps(const char* caller, HeapRegion* hr) PRODUCT_RETURN;

  // If G1VerifyBitmaps is set, verify that the marking bitmaps do not
  // have any spurious marks. If errors are detected, print
  // appropriate error messages and crash.
  void check_bitmaps(const char* caller) PRODUCT_RETURN;

  // Do sanity check on the contents of the in-cset fast test table.
  bool check_region_attr_table() PRODUCT_RETURN_( return true; );

  void verify_card_table_cleanup() PRODUCT_RETURN;

  void verify_not_dirty_region(HeapRegion* hr) PRODUCT_RETURN;
  void verify_dirty_region(HeapRegion* hr) PRODUCT_RETURN;
  void verify_dirty_young_regions() PRODUCT_RETURN;

  static void verify_ready_for_archiving();
  static void verify_archive_regions();
};

#endif // SHARE_GC_G1_G1HEAPVERIFIER_HPP
