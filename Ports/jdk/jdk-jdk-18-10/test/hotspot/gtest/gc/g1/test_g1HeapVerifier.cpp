/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "gc/g1/g1Arguments.hpp"
#include "gc/g1/g1HeapVerifier.hpp"
#include "logging/logConfiguration.hpp"
#include "logging/logTag.hpp"
#include "logging/logTestFixture.hpp"
#include "unittest.hpp"

class G1HeapVerifierTest : public LogTestFixture {
  protected:
    static void parse_verification_type(const char* type) {
      G1Arguments::parse_verification_type(type);
    }
};

TEST_VM_F(G1HeapVerifierTest, parse) {
  LogConfiguration::configure_stdout(LogLevel::Off, true, LOG_TAGS(gc, verify));

  // Default is to verify everything.
  ASSERT_TRUE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyYoungNormal));
  ASSERT_TRUE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyConcurrentStart));
  ASSERT_TRUE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyMixed));
  ASSERT_TRUE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyYoungEvacFail));
  ASSERT_TRUE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyRemark));
  ASSERT_TRUE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyCleanup));
  ASSERT_TRUE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyFull));

  // Setting one will disable all other.
  G1HeapVerifierTest::parse_verification_type("full");
  ASSERT_FALSE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyYoungNormal));
  ASSERT_FALSE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyConcurrentStart));
  ASSERT_FALSE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyMixed));
  ASSERT_FALSE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyYoungEvacFail));
  ASSERT_FALSE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyRemark));
  ASSERT_FALSE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyCleanup));
  ASSERT_TRUE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyFull));

  // Verify case sensitivity.
  G1HeapVerifierTest::parse_verification_type("YOUNG-NORMAL");
  ASSERT_FALSE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyYoungNormal));
  G1HeapVerifierTest::parse_verification_type("young-normal");
  ASSERT_TRUE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyYoungNormal));

  // Verify perfect match
  G1HeapVerifierTest::parse_verification_type("mixedgc");
  ASSERT_FALSE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyMixed));
  G1HeapVerifierTest::parse_verification_type("mixe");
  ASSERT_FALSE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyMixed));
  G1HeapVerifierTest::parse_verification_type("mixed");
  ASSERT_TRUE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyMixed));

  // Verify the last three
  G1HeapVerifierTest::parse_verification_type("concurrent-start");
  G1HeapVerifierTest::parse_verification_type("remark");
  G1HeapVerifierTest::parse_verification_type("cleanup");
  ASSERT_TRUE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyRemark));
  ASSERT_TRUE(G1HeapVerifier::should_verify(G1HeapVerifier::G1VerifyCleanup));
}
