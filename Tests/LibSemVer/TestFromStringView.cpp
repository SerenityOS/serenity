/*
 * Copyright (c) 2023, Gurkirat Singh <tbhaxor@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <AK/Tuple.h>
#include <AK/Vector.h>
#include <LibSemVer/Semver.h>
#include <LibTest/TestCase.h>

TEST_CASE(parsing)
{
    Vector<Tuple<StringView, bool>> test_cases = {
        Tuple { "1"sv, false },
        Tuple { "1.2"sv, false },
        Tuple { "1.2.3-0123"sv, false },
        Tuple { "1.2.3-0123.0123"sv, false },
        Tuple { "1.1.2+.123"sv, false },
        Tuple { "+invalid"sv, false },
        Tuple { "-invalid"sv, false },
        Tuple { "-invalid+invalid"sv, false },
        Tuple { "-invalid.01"sv, false },
        Tuple { "alpha"sv, false },
        Tuple { "alpha.beta"sv, false },
        Tuple { "alpha.beta.1"sv, false },
        Tuple { "alpha.1"sv, false },
        Tuple { "alpha+beta"sv, false },
        Tuple { "alpha_beta"sv, false },
        Tuple { "alpha."sv, false },
        Tuple { "alpha.."sv, false },
        Tuple { "beta"sv, false },
        Tuple { "1.0.0-alpha_beta"sv, false },
        Tuple { "-alpha."sv, false },
        Tuple { "1.0.0-alpha.."sv, false },
        Tuple { "1.0.0-alpha..1"sv, false },
        Tuple { "1.0.0-alpha...1"sv, false },
        Tuple { "1.0.0-alpha....1"sv, false },
        Tuple { "1.0.0-alpha.....1"sv, false },
        Tuple { "1.0.0-alpha......1"sv, false },
        Tuple { "1.0.0-alpha.......1"sv, false },
        Tuple { "01.1.1"sv, false },
        Tuple { "1.01.1"sv, false },
        Tuple { "1.1.01"sv, false },
        Tuple { "1.2"sv, false },
        Tuple { "1.2.3.DEV"sv, false },
        Tuple { "1.2-SNAPSHOT"sv, false },
        Tuple { "1.2.31.2.3----RC-SNAPSHOT.12.09.1--..12+788"sv, false },
        Tuple { "1.2-RC-SNAPSHOT"sv, false },
        Tuple { "-1.0.3-gamma+b7718"sv, false },
        Tuple { "+justmeta"sv, false },
        Tuple { "9.8.7+meta+meta"sv, false },
        Tuple { "9.8.7-whatever+meta+meta"sv, false },
        Tuple { "99999999999999999999999.999999999999999999.99999999999999999"sv, false },
        Tuple { "1.0.4"sv, true },
        Tuple { "1.2.3"sv, true },
        Tuple { "10.20.30"sv, true },
        Tuple { "1.1.2-prerelease+meta"sv, true },
        Tuple { "1.1.2+meta"sv, true },
        Tuple { "1.1.2+meta-valid"sv, true },
        Tuple { "1.0.0-alpha"sv, true },
        Tuple { "1.0.0-beta"sv, true },
        Tuple { "1.0.0-alpha.beta"sv, true },
        Tuple { "1.0.0-alpha.beta.1"sv, true },
        Tuple { "1.0.0-alpha.1"sv, true },
        Tuple { "1.0.0-alpha0.valid"sv, true },
        Tuple { "1.0.0-alpha.0valid"sv, true },
        Tuple { "1.0.0-rc.1+build.1"sv, true },
        Tuple { "2.0.0-rc.1+build.123"sv, true },
        Tuple { "1.2.3-beta"sv, true },
        Tuple { "10.2.3-DEV-SNAPSHOT"sv, true },
        Tuple { "1.2.3-SNAPSHOT-123"sv, true },
        Tuple { "1.0.0"sv, true },
        Tuple { "2.0.0"sv, true },
        Tuple { "1.1.7"sv, true },
        Tuple { "2.0.0+build.1848"sv, true },
        Tuple { "2.0.1-alpha.1227"sv, true },
        Tuple { "1.0.0-alpha+beta"sv, true },
        Tuple { "1.0.0-alpha-a.b-c-somethinglong+build.1-aef.1-its-okay"sv, true },
        Tuple { "1.2.3----RC-SNAPSHOT.12.9.1--.12+788"sv, true },
        Tuple { "1.2.3----R-S.12.9.1--.12+meta"sv, true },
        Tuple { "1.2.3----RC-SNAPSHOT.12.9.1--.12"sv, true },
        Tuple { "1.0.0+0.build.1-rc.10000aaa-kk-0.1"sv, true },
        Tuple { "1.0.0-0A.is.legal"sv, true },
    };

    for (auto const& test_case : test_cases) {
        EXPECT_EQ(SemVer::is_valid(test_case.get<StringView>()), test_case.get<bool>());
    }
}

TEST_CASE(no_error_on_invalid_strings)
{
}
