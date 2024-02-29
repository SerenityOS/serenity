/*
 * Copyright (c) 2023, Gurkirat Singh <tbhaxor@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibSemVer/SemVer.h>
#include <LibTest/TestCase.h>

TEST_CASE(parsing) // NOLINT(readability-function-cognitive-complexity)
{
    EXPECT(!SemVer::is_valid("1"sv));
    EXPECT(!SemVer::is_valid("1.2"sv));
    EXPECT(!SemVer::is_valid("1.1.2+.123"sv));
    EXPECT(!SemVer::is_valid("1.2.3-0123"sv));
    EXPECT(!SemVer::is_valid("1.2.3-0123.0123"sv));
    EXPECT(!SemVer::is_valid("+invalid"sv));
    EXPECT(!SemVer::is_valid("-invalid"sv));
    EXPECT(!SemVer::is_valid("-invalid+invalid"sv));
    EXPECT(!SemVer::is_valid("-invalid.01"sv));
    EXPECT(!SemVer::is_valid("1 .2.3-this.is.invalid"sv));
    EXPECT(!SemVer::is_valid("1.2.3-this .is. also .invalid"sv));
    EXPECT(!SemVer::is_valid("1.2.3"sv, ' '));
    EXPECT(!SemVer::is_valid("alpha"sv));
    EXPECT(!SemVer::is_valid("alpha.beta"sv));
    EXPECT(!SemVer::is_valid("alpha.beta.1"sv));
    EXPECT(!SemVer::is_valid("alpha.1"sv));
    EXPECT(!SemVer::is_valid("alpha+beta"sv));
    EXPECT(!SemVer::is_valid("alpha_beta"sv));
    EXPECT(!SemVer::is_valid("alpha."sv));
    EXPECT(!SemVer::is_valid("alpha.."sv));
    EXPECT(!SemVer::is_valid("beta"sv));
    EXPECT(!SemVer::is_valid("1.0.0-alpha_beta"sv));
    EXPECT(!SemVer::is_valid("-alpha."sv));
    EXPECT(!SemVer::is_valid("1.0.0-alpha.."sv));
    EXPECT(!SemVer::is_valid("1.0.0-alpha..1"sv));
    EXPECT(!SemVer::is_valid("1.0.0-alpha...1"sv));
    EXPECT(!SemVer::is_valid("1.0.0-alpha....1"sv));
    EXPECT(!SemVer::is_valid("1.0.0-alpha.....1"sv));
    EXPECT(!SemVer::is_valid("1.0.0-alpha......1"sv));
    EXPECT(!SemVer::is_valid("1.0.0-alpha.......1"sv));
    EXPECT(!SemVer::is_valid("01.1.1"sv));
    EXPECT(!SemVer::is_valid("1.01.1"sv));
    EXPECT(!SemVer::is_valid("1.1.01"sv));
    EXPECT(!SemVer::is_valid("1.2"sv));
    EXPECT(!SemVer::is_valid("1.2.3.DEV"sv));
    EXPECT(!SemVer::is_valid("1.2-SNAPSHOT"sv));
    EXPECT(!SemVer::is_valid("1.2.31.2.3----RC-SNAPSHOT.12.09.1--..12+788"sv));
    EXPECT(!SemVer::is_valid("1.2-RC-SNAPSHOT"sv));
    EXPECT(!SemVer::is_valid("-1.0.3-gamma+b7718"sv));
    EXPECT(!SemVer::is_valid("+justmeta"sv));
    EXPECT(!SemVer::is_valid("9.8.7+meta+meta"sv));
    EXPECT(!SemVer::is_valid("9.8.7-whatever+meta+meta"sv));
    // Because of size_t overflow, it won't work work version such as 99999999999999999999999
    EXPECT(!SemVer::is_valid("99999999999999999999999.999999999999999999.99999999999999999"sv));
    EXPECT(SemVer::is_valid("1.0.4"sv));
    EXPECT(SemVer::is_valid("1.2.3"sv));
    EXPECT(SemVer::is_valid("10.20.30"sv));
    EXPECT(SemVer::is_valid("1.1.2-prerelease+meta"sv));
    EXPECT(SemVer::is_valid("1.1.2+meta"sv));
    EXPECT(SemVer::is_valid("1.1.2+meta-valid"sv));
    EXPECT(SemVer::is_valid("1.0.0-alpha"sv));
    EXPECT(SemVer::is_valid("1.0.0-beta"sv));
    EXPECT(SemVer::is_valid("1.0.0-alpha.beta"sv));
    EXPECT(SemVer::is_valid("1.0.0-alpha.beta.1"sv));
    EXPECT(SemVer::is_valid("1.0.0-alpha.1"sv));
    EXPECT(SemVer::is_valid("1.0.0-alpha0.valid"sv));
    EXPECT(SemVer::is_valid("1.0.0-alpha.0valid"sv));
    EXPECT(SemVer::is_valid("1.0.0-rc.1+build.1"sv));
    EXPECT(SemVer::is_valid("2.0.0-rc.1+build.123"sv));
    EXPECT(SemVer::is_valid("1.2.3-beta"sv));
    EXPECT(SemVer::is_valid("10.2.3-DEV-SNAPSHOT"sv));
    EXPECT(SemVer::is_valid("1.2.3-SNAPSHOT-123"sv));
    EXPECT(SemVer::is_valid("1.0.0"sv));
    EXPECT(SemVer::is_valid("2.0.0"sv));
    EXPECT(SemVer::is_valid("1.1.7"sv));
    EXPECT(SemVer::is_valid("2.0.0+build.1848"sv));
    EXPECT(SemVer::is_valid("2.0.1-alpha.1227"sv));
    EXPECT(SemVer::is_valid("1.0.0-alpha+beta"sv));
    EXPECT(SemVer::is_valid("1.0.0-alpha-a.b-c-somethinglong+build.1-aef.1-its-okay"sv));
    EXPECT(SemVer::is_valid("1.2.3----RC-SNAPSHOT.12.9.1--.12+788"sv));
    EXPECT(SemVer::is_valid("1.2.3----R-S.12.9.1--.12+meta"sv));
    EXPECT(SemVer::is_valid("1.2.3----RC-SNAPSHOT.12.9.1--.12"sv));
    EXPECT(SemVer::is_valid("1.0.0+0.build.1-rc.10000aaa-kk-0.1"sv));
    EXPECT(SemVer::is_valid("1.0.0-0A.is.legal"sv));
}

TEST_CASE(parse_with_different_mmp_sep)
{
    // insufficient separators
    EXPECT(!SemVer::is_valid("1.2-3"sv));
    EXPECT(!SemVer::is_valid("1.2-3"sv, '-'));

    // conflicting separators
    EXPECT(!SemVer::is_valid("11213"sv, '1'));

    // sufficient separators
    EXPECT(SemVer::is_valid("1.2.3"sv, '.'));
    EXPECT(SemVer::is_valid("1-2-3"sv, '-'));
    EXPECT(SemVer::is_valid("1-3-3-pre+build"sv, '-'));
}
