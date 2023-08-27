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

TEST_CASE(to_string)
{
    Vector<StringView> versions {
        "1.0.4"sv,
        "1.2.3"sv,
        "10.20.30"sv,
        "1.1.2-prerelease+meta"sv,
        "1.1.2+meta"sv,
        "1.1.2+meta-valid"sv,
        "1.0.0-alpha"sv,
        "1.0.0-beta"sv,
        "1.0.0-alpha.beta"sv,
        "1.0.0-alpha.beta.1"sv,
        "1.0.0-alpha.1"sv,
        "1.0.0-alpha0.valid"sv,
        "1.0.0-alpha.0valid"sv,
        "1.0.0-rc.1+build.1"sv,
        "2.0.0-rc.1+build.123"sv,
        "1.2.3-beta"sv,
        "10.2.3-DEV-SNAPSHOT"sv,
        "1.2.3-SNAPSHOT-123"sv,
        "1.0.0"sv,
        "2.0.0"sv,
        "1.1.7"sv,
        "2.0.0+build.1848"sv,
        "2.0.1-alpha.1227"sv,
        "1.0.0-alpha+beta"sv,
        "1.0.0-alpha-a.b-c-somethinglong+build.1-aef.1-its-okay"sv,
        "1.2.3----RC-SNAPSHOT.12.9.1--.12+788"sv,
        "1.2.3----R-S.12.9.1--.12+meta"sv,
        "1.2.3----RC-SNAPSHOT.12.9.1--.12"sv,
        "1.0.0+0.build.1-rc.10000aaa-kk-0.1"sv,
        "1.0.0-0A.is.legal"sv,
    };

    for (auto const& version : versions) {
        EXPECT_EQ(version, MUST(SemVer::from_string_view(version)).to_string());
    }
}

TEST_CASE(bump)
{
    auto version = MUST(SemVer::from_string_view("1.1.2-prerelease+meta"sv));

    auto major_bump = MUST(version.bump(SemVer::BumpType::Major));
    auto minor_bump = MUST(version.bump(SemVer::BumpType::Minor));
    auto patch_bump = MUST(version.bump(SemVer::BumpType::Patch));

    EXPECT_EQ(major_bump.m_major, version.m_major + 1);
    EXPECT_EQ(minor_bump.m_minor, version.m_minor + 1);
    EXPECT_EQ(patch_bump.m_patch, version.m_patch + 1);

    EXPECT_EQ(major_bump.suffix(), version.suffix());

    auto bump_clear_prerelease = MUST(version.bump(SemVer::BumpType::Major, SemVer::BumpOpts::ClearPreRelease));
    auto bump_clear_buildmetadata = MUST(version.bump(SemVer::BumpType::Major, SemVer::BumpOpts::ClearBuildMeta));

    EXPECT_EQ(bump_clear_prerelease.suffix(), "+meta"sv);
    EXPECT_EQ(bump_clear_buildmetadata.suffix(), "-prerelease"sv);

    auto invalid_value_reaches_empass = version.bump((SemVer::BumpType)10);
    EXPECT(invalid_value_reaches_empass.is_error());

    invalid_value_reaches_empass = version.bump(SemVer::BumpType::Major, (SemVer::BumpOpts)10);
    EXPECT(invalid_value_reaches_empass.is_error());
}

TEST_CASE(release_type)
{
    Vector<Tuple<StringView, SemVer::ReleaseType>> test_cases {
        // no suffix
        Tuple { "1.2.3"sv, SemVer::ReleaseType::Stable },
        // alpha or beta
        Tuple { "1.2.3-alpha"sv, SemVer::ReleaseType::Alpha },
        Tuple { "1.2.3-beta"sv, SemVer::ReleaseType::Beta },
        // alpha and beta order doesn't matter
        Tuple { "1.2.3-alpha.beta"sv, SemVer::ReleaseType::AlphaBeta },
        Tuple { "1.2.3-beta.alpha"sv, SemVer::ReleaseType::AlphaBeta },
        // prerelease
        Tuple { "1.2.3-prerelease"sv, SemVer::ReleaseType::PreRelease },
        // release candidate
        Tuple { "1.2.3-rc"sv, SemVer::ReleaseType::ReleaseCandidate },
        // alpha/beta has high priority
        Tuple { "1.2.3-prerelease.alpha"sv, SemVer::ReleaseType::Alpha },
        Tuple { "1.2.3-alpha.prerelease"sv, SemVer::ReleaseType::Alpha },
        Tuple { "1.2.3-prerelease.beta"sv, SemVer::ReleaseType::Beta },
        Tuple { "1.2.3-beta.prerelease"sv, SemVer::ReleaseType::Beta },
        Tuple { "1.2.3-beta.prerelease.alpha"sv, SemVer::ReleaseType::AlphaBeta },
        Tuple { "1.2.3-alpha.prerelease.beta"sv, SemVer::ReleaseType::AlphaBeta },
        Tuple { "1.2.3-rc.alpha"sv, SemVer::ReleaseType::Alpha },
        Tuple { "1.2.3-alpha.rc"sv, SemVer::ReleaseType::Alpha },
        Tuple { "1.2.3-rc.beta"sv, SemVer::ReleaseType::Beta },
        Tuple { "1.2.3-beta.rc"sv, SemVer::ReleaseType::Beta },
        Tuple { "1.2.3-beta.rc.alpha"sv, SemVer::ReleaseType::AlphaBeta },
        Tuple { "1.2.3-alpha.rc.beta"sv, SemVer::ReleaseType::AlphaBeta },
        // prerelease has high priority than release candidate
        Tuple { "1.2.3-prerelease.rc"sv, SemVer::ReleaseType::PreRelease },
        Tuple { "1.2.3-rc.prerelease"sv, SemVer::ReleaseType::PreRelease },
        // fallback to stable version
        Tuple { "1.2.3-is.also.legal"sv, SemVer::ReleaseType::Stable },
    };

    for (auto const& test_case : test_cases.span()) {
        auto version = MUST(SemVer::from_string_view(test_case.get<0>()));
        EXPECT_EQ(version.release_type(), test_case.get<1>());
    }
}

TEST_CASE(is_same)
{
    Vector<Tuple<StringView, StringView, SemVer::CompareType, bool>> test_cases {
        // exact match
        Tuple { "1.1.2-prerelease+meta"sv, "1.1.2-prerelease+meta"sv, SemVer::CompareType::Exact, true },
        Tuple { "1.1.2-prerelease+meta"sv, "1.1.3-prerelease+meta"sv, SemVer::CompareType::Exact, false },
        Tuple { "1.1.2-prerelease+meta"sv, "1.2.2-prerelease+meta"sv, SemVer::CompareType::Exact, false },
        Tuple { "1.1.2-prerelease+meta"sv, "2.1.2-prerelease+meta"sv, SemVer::CompareType::Exact, false },
        Tuple { "1.1.2-prerelease+meta"sv, "1.1.3-someother"sv, SemVer::CompareType::Exact, false },
        // major part match
        Tuple { "1.1.2"sv, "1.1.2"sv, SemVer::CompareType::Major, true },
        Tuple { "1.1.2"sv, "1.2.2"sv, SemVer::CompareType::Major, true },
        Tuple { "1.1.2"sv, "1.1.3"sv, SemVer::CompareType::Major, true },
        Tuple { "1.1.2"sv, "2.1.2"sv, SemVer::CompareType::Major, false },
        // minor part match
        Tuple { "1.1.2"sv, "1.1.2"sv, SemVer::CompareType::Minor, true },
        Tuple { "1.1.2"sv, "1.1.3"sv, SemVer::CompareType::Minor, true },
        Tuple { "1.1.2"sv, "1.2.2"sv, SemVer::CompareType::Minor, false },
        Tuple { "1.1.2"sv, "2.1.2"sv, SemVer::CompareType::Minor, false },
        Tuple { "1.1.2"sv, "2.2.2"sv, SemVer::CompareType::Minor, false },
        // patch part match
        Tuple { "1.1.2"sv, "1.1.2"sv, SemVer::CompareType::Patch, true },
        Tuple { "1.1.2"sv, "1.1.3"sv, SemVer::CompareType::Patch, false },
        Tuple { "1.1.2"sv, "1.2.2"sv, SemVer::CompareType::Patch, false },
        Tuple { "1.1.2"sv, "2.1.2"sv, SemVer::CompareType::Patch, false },
        Tuple { "1.1.2"sv, "1.2.2"sv, SemVer::CompareType::Patch, false },
        Tuple { "1.1.2"sv, "2.1.2"sv, SemVer::CompareType::Patch, false },
        Tuple { "1.1.2"sv, "2.2.2"sv, SemVer::CompareType::Patch, false },
    };

    for (auto const& test_case : test_cases) {
        auto v1 = MUST(SemVer::from_string_view(test_case.get<0>()));
        auto v2 = MUST(SemVer::from_string_view(test_case.get<1>()));
        EXPECT_EQ(v1.is_same(v2, test_case.get<2>()), test_case.get<3>());
    }
}

TEST_CASE(is_greater_than)
{
    Vector<Tuple<StringView, StringView, bool>> test_cases {
        Tuple { "1.1.3"sv, "1.1.2"sv, true },
        Tuple { "1.2.2"sv, "1.1.2"sv, true },
        Tuple { "2.1.2"sv, "1.1.2"sv, true },
        Tuple { "2.1.3"sv, "1.1.2"sv, true },
        Tuple { "1.2.3"sv, "1.1.2"sv, true },
        Tuple { "1.2.2"sv, "1.1.2"sv, true },
        Tuple { "1.1.2"sv, "1.1.2"sv, false },
    };

    for (auto const& test_case : test_cases) {
        auto v1 = MUST(SemVer::from_string_view(test_case.get<0>()));
        auto v2 = MUST(SemVer::from_string_view(test_case.get<1>()));
        EXPECT_EQ(v1.is_greater_than(v2), test_case.get<2>());
    }
}

TEST_CASE(is_lesser_than)
{
    Vector<Tuple<StringView, StringView, bool>> test_cases {
        Tuple { "1.1.2"sv, "1.1.3"sv, true },
        Tuple { "1.1.2"sv, "1.2.2"sv, true },
        Tuple { "1.1.2"sv, "2.1.2"sv, true },
        Tuple { "1.1.2"sv, "2.1.3"sv, true },
        Tuple { "1.1.2"sv, "1.2.3"sv, true },
        Tuple { "1.1.2"sv, "1.2.2"sv, true },
        Tuple { "1.1.2"sv, "1.1.2"sv, false },
    };

    for (auto const& test_case : test_cases) {
        auto v1 = MUST(SemVer::from_string_view(test_case.get<0>()));
        auto v2 = MUST(SemVer::from_string_view(test_case.get<1>()));
        EXPECT_EQ(v1.is_lesser_than(v2), test_case.get<2>());
    }
}

TEST_CASE(satisfies)
{
    auto version = MUST(SemVer::from_string_view("1.1.2-prerelease+meta"sv));

    Vector<Tuple<StringView, bool>> test_cases {
        { "1.1.2-prerelease+meta"sv, true },
        { "1.2.2-prerelease+meta"sv, false },

        { "!=1.1.2-prerelease+meta"sv, false },
        { "!=1.2.2-prerelease+meta"sv, true },

        { "==1.1.3-prerelease+meta"sv, false },
        { "==1.1.2-prerelease"sv, false },
        { "==1.1.2-prerelease+meta"sv, true },

        { "<1.1.1-prerelease+meta"sv, false },
        { "<1.1.2-prerelease+meta"sv, false },
        { "<1.1.3-prerelease+meta"sv, true },

        { ">1.1.1-prerelease+meta"sv, true },
        { ">1.1.2-prerelease+meta"sv, false },
        { ">1.1.3-prerelease+meta"sv, false },

        { ">=1.1.1-prerelease+meta"sv, true },
        { ">=1.1.2-prerelease+meta"sv, true },
        { ">=1.1.3-prerelease+meta"sv, false },

        { "<=1.1.1-prerelease+meta"sv, false },
        { "<=1.1.2-prerelease+meta"sv, true },
        { "<=1.1.3-prerelease+meta"sv, true },

        { "HELLO1.1.2-prerelease+meta"sv, false },
    };

    for (auto const& test_case : test_cases) {
        EXPECT_EQ(version.satisfies(test_case.get<0>()), test_case.get<1>());
    }
}
