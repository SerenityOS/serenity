/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Endian.h>

static_assert(BigEndian<u32> {} == 0, "Big endian values should be default constructed in a constexpr context.");

static_assert(BigEndian<u32> { 42 } == 42, "Big endian values should be value constructed in a constexpr context.");

static_assert(LittleEndian<u32> {} == 0, "Little endian values should be default constructed in a constexpr context.");

static_assert(LittleEndian<u32> { 42 } == 42, "Little endian values should be value constructed in a constexpr context.");
