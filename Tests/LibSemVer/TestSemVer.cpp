/*
 * Copyright (c) 2023, Gurkirat Singh <tbhaxor@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibSemVer/SemVer.h>
#include <LibTest/TestCase.h>

#define GET_SEMVER(expression)                           \
    ({                                                   \
        auto r = (SemVer::from_string_view(expression)); \
        EXPECT(!r.is_error());                           \
        r.value();                                       \
    })

#define GET_STRING(expression)                    \
    ({                                            \
        auto r = (String::from_utf8(expression)); \
        EXPECT(!r.is_error());                    \
        r.value();                                \
    })

#define IS_SAME_SCENARIO(x, y, op) \
    GET_SEMVER(x).is_same(GET_SEMVER(y), op)

#define IS_GREATER_THAN_SCENARIO(x, y) \
    GET_SEMVER(x).is_greater_than(GET_SEMVER(y))

#define IS_LESSER_THAN_SCENARIO(x, y) \
    GET_SEMVER(x).is_lesser_than(GET_SEMVER(y))

TEST_CASE(to_string) // NOLINT(readability-function-cognitive-complexity, readability-function-size)
{
    EXPECT_EQ(GET_SEMVER("1.2.3"sv).to_string(), GET_STRING("1.2.3"sv));
    EXPECT_EQ(GET_SEMVER("1.2.3"sv).to_string(), GET_STRING("1.2.3"sv));
    EXPECT_EQ(GET_SEMVER("10.20.30"sv).to_string(), GET_STRING("10.20.30"sv));
    EXPECT_EQ(GET_SEMVER("1.1.2-prerelease+meta"sv).to_string(), GET_STRING("1.1.2-prerelease+meta"sv));
    EXPECT_EQ(GET_SEMVER("1.1.2+meta"sv).to_string(), GET_STRING("1.1.2+meta"sv));
    EXPECT_EQ(GET_SEMVER("1.1.2+meta-valid"sv).to_string(), GET_STRING("1.1.2+meta-valid"sv));
    EXPECT_EQ(GET_SEMVER("1.0.0-alpha"sv).to_string(), GET_STRING("1.0.0-alpha"sv));
    EXPECT_EQ(GET_SEMVER("1.0.0-beta"sv).to_string(), GET_STRING("1.0.0-beta"sv));
    EXPECT_EQ(GET_SEMVER("1.0.0-alpha.beta"sv).to_string(), GET_STRING("1.0.0-alpha.beta"sv));
    EXPECT_EQ(GET_SEMVER("1.0.0-alpha.beta.1"sv).to_string(), GET_STRING("1.0.0-alpha.beta.1"sv));
    EXPECT_EQ(GET_SEMVER("1.0.0-alpha.1"sv).to_string(), GET_STRING("1.0.0-alpha.1"sv));
    EXPECT_EQ(GET_SEMVER("1.0.0-alpha0.valid"sv).to_string(), GET_STRING("1.0.0-alpha0.valid"sv));
    EXPECT_EQ(GET_SEMVER("1.0.0-alpha.0valid"sv).to_string(), GET_STRING("1.0.0-alpha.0valid"sv));
    EXPECT_EQ(GET_SEMVER("1.0.0-rc.1+build.1"sv).to_string(), GET_STRING("1.0.0-rc.1+build.1"sv));
    EXPECT_EQ(GET_SEMVER("2.0.0-rc.1+build.123"sv).to_string(), GET_STRING("2.0.0-rc.1+build.123"sv));
    EXPECT_EQ(GET_SEMVER("1.2.3-beta"sv).to_string(), GET_STRING("1.2.3-beta"sv));
    EXPECT_EQ(GET_SEMVER("10.2.3-DEV-SNAPSHOT"sv).to_string(), GET_STRING("10.2.3-DEV-SNAPSHOT"sv));
    EXPECT_EQ(GET_SEMVER("1.2.3-SNAPSHOT-123"sv).to_string(), GET_STRING("1.2.3-SNAPSHOT-123"sv));
    EXPECT_EQ(GET_SEMVER("1.0.0"sv).to_string(), GET_STRING("1.0.0"sv));
    EXPECT_EQ(GET_SEMVER("2.0.0"sv).to_string(), GET_STRING("2.0.0"sv));
    EXPECT_EQ(GET_SEMVER("1.1.7"sv).to_string(), GET_STRING("1.1.7"sv));
    EXPECT_EQ(GET_SEMVER("2.0.0+build.1848"sv).to_string(), GET_STRING("2.0.0+build.1848"sv));
    EXPECT_EQ(GET_SEMVER("2.0.1-alpha.1227"sv).to_string(), GET_STRING("2.0.1-alpha.1227"sv));
    EXPECT_EQ(GET_SEMVER("1.0.0-alpha+beta"sv).to_string(), GET_STRING("1.0.0-alpha+beta"sv));
    EXPECT_EQ(GET_SEMVER("1.0.0-alpha-a.b-c-somethinglong+build.1-aef.1-its-okay"sv).to_string(), GET_STRING("1.0.0-alpha-a.b-c-somethinglong+build.1-aef.1-its-okay"sv));
    EXPECT_EQ(GET_SEMVER("1.2.3----RC-SNAPSHOT.12.9.1--.12+788"sv).to_string(), GET_STRING("1.2.3----RC-SNAPSHOT.12.9.1--.12+788"sv));
    EXPECT_EQ(GET_SEMVER("1.2.3----RC-SNAPSHOT.12.9.1--"sv).to_string(), GET_STRING("1.2.3----RC-SNAPSHOT.12.9.1--"sv));
    EXPECT_EQ(GET_SEMVER("1.2.3----R-S.12.9.1--.12+meta"sv).to_string(), GET_STRING("1.2.3----R-S.12.9.1--.12+meta"sv));
    EXPECT_EQ(GET_SEMVER("1.2.3----RC-SNAPSHOT.12.9.1--.12"sv).to_string(), GET_STRING("1.2.3----RC-SNAPSHOT.12.9.1--.12"sv));
    EXPECT_EQ(GET_SEMVER("1.0.0+0.build.1-rc.10000aaa-kk-0.1"sv).to_string(), GET_STRING("1.0.0+0.build.1-rc.10000aaa-kk-0.1"sv));
    EXPECT_EQ(GET_SEMVER("1.0.0-0A.is.legal"sv).to_string(), GET_STRING("1.0.0-0A.is.legal"sv));
}

TEST_CASE(normal_bump) // NOLINT(readability-function-cognitive-complexity)
{
    auto version = GET_SEMVER("1.1.2-prerelease+meta"sv);

    // normal bumps
    auto major_bump = version.bump(SemVer::BumpType::Major);
    EXPECT_EQ(major_bump.major(), version.major() + 1);
    EXPECT_EQ(major_bump.minor(), 0ul);
    EXPECT_EQ(major_bump.patch(), 0ul);
    EXPECT(major_bump.suffix().is_empty());

    auto minor_bump = version.bump(SemVer::BumpType::Minor);
    EXPECT_EQ(minor_bump.major(), version.major());
    EXPECT_EQ(minor_bump.minor(), version.minor() + 1);
    EXPECT_EQ(minor_bump.patch(), 0ul);
    EXPECT(minor_bump.suffix().is_empty());

    auto patch_bump = version.bump(SemVer::BumpType::Patch);
    EXPECT_EQ(patch_bump.major(), version.major());
    EXPECT_EQ(patch_bump.minor(), version.minor());
    EXPECT_EQ(patch_bump.patch(), version.patch() + 1);
    EXPECT(minor_bump.suffix().is_empty());
}

TEST_CASE(prerelease_bump_increment_numeric)
{
    auto version = GET_SEMVER("1.1.2-0"sv);

    auto prerelease_bump = version.bump(SemVer::BumpType::Prerelease);
    EXPECT_EQ(prerelease_bump.major(), version.major());
    EXPECT_EQ(prerelease_bump.minor(), version.minor());
    EXPECT_EQ(prerelease_bump.patch(), version.patch());
    EXPECT_NE(prerelease_bump.prerelease(), version.prerelease());
    EXPECT(prerelease_bump.build_metadata().is_empty());

    auto version_prerelease_parts = version.prerelease_identifiers();
    auto bumped_prerelease_parts = prerelease_bump.prerelease_identifiers();
    EXPECT_EQ(bumped_prerelease_parts.size(), version_prerelease_parts.size());
    EXPECT_EQ(bumped_prerelease_parts[0], "1"_string);
}

TEST_CASE(prerelease_bump_rightmost_numeric_part)
{
    auto version = GET_SEMVER("1.1.2-a.1.0.c"sv);

    auto prerelease_bump = version.bump(SemVer::BumpType::Prerelease);
    EXPECT_EQ(prerelease_bump.major(), version.major());
    EXPECT_EQ(prerelease_bump.minor(), version.minor());
    EXPECT_EQ(prerelease_bump.patch(), version.patch());
    EXPECT_NE(prerelease_bump.prerelease(), version.prerelease());
    EXPECT(prerelease_bump.build_metadata().is_empty());

    auto version_prerelease_parts = version.prerelease_identifiers();
    auto bumped_prerelease_parts = prerelease_bump.prerelease_identifiers();
    EXPECT_EQ(bumped_prerelease_parts.size(), version_prerelease_parts.size());
    EXPECT_EQ(bumped_prerelease_parts[2], "1"_string);
}

TEST_CASE(prerelease_bump_add_zero_if_no_numeric)
{
    auto version = GET_SEMVER("1.1.2-only.strings"sv);

    auto prerelease_bump = version.bump(SemVer::BumpType::Prerelease);
    EXPECT_EQ(prerelease_bump.major(), version.major());
    EXPECT_EQ(prerelease_bump.minor(), version.minor());
    EXPECT_EQ(prerelease_bump.patch(), version.patch());
    EXPECT_NE(prerelease_bump.prerelease(), version.prerelease());
    EXPECT(prerelease_bump.build_metadata().is_empty());

    auto version_prerelease_parts = version.prerelease_identifiers();
    auto bumped_prerelease_parts = prerelease_bump.prerelease_identifiers();
    EXPECT(bumped_prerelease_parts.size() > version_prerelease_parts.size());
    EXPECT_EQ(bumped_prerelease_parts[2], "0"_string);
}

TEST_CASE(is_same) // NOLINT(readability-function-cognitive-complexity)
{
    // exact match
    EXPECT(IS_SAME_SCENARIO("1.1.2-prerelease+meta"sv, "1.1.2-prerelease+meta"sv, SemVer::CompareType::Exact));
    EXPECT(!IS_SAME_SCENARIO("1.1.2-prerelease+meta"sv, "1.1.3-prerelease+meta"sv, SemVer::CompareType::Exact));
    EXPECT(!IS_SAME_SCENARIO("1.1.2-prerelease+meta"sv, "1.2.2-prerelease+meta"sv, SemVer::CompareType::Exact));
    EXPECT(!IS_SAME_SCENARIO("1.1.2-prerelease+meta"sv, "2.1.2-prerelease+meta"sv, SemVer::CompareType::Exact));
    EXPECT(!IS_SAME_SCENARIO("1.1.2-prerelease+meta"sv, "1.1.3-someother"sv, SemVer::CompareType::Exact));
    // major part match
    EXPECT(IS_SAME_SCENARIO("1.1.2"sv, "1.1.2"sv, SemVer::CompareType::Major));
    EXPECT(IS_SAME_SCENARIO("1.1.2"sv, "1.2.2"sv, SemVer::CompareType::Major));
    EXPECT(IS_SAME_SCENARIO("1.1.2"sv, "1.1.3"sv, SemVer::CompareType::Major));
    EXPECT(!IS_SAME_SCENARIO("1.1.2"sv, "2.1.2"sv, SemVer::CompareType::Major));
    // minor part match
    EXPECT(IS_SAME_SCENARIO("1.1.2"sv, "1.1.2"sv, SemVer::CompareType::Minor));
    EXPECT(IS_SAME_SCENARIO("1.1.2"sv, "1.1.3"sv, SemVer::CompareType::Minor));
    EXPECT(!IS_SAME_SCENARIO("1.1.2"sv, "1.2.2"sv, SemVer::CompareType::Minor));
    EXPECT(!IS_SAME_SCENARIO("1.1.2"sv, "2.1.2"sv, SemVer::CompareType::Minor));
    EXPECT(!IS_SAME_SCENARIO("1.1.2"sv, "2.2.2"sv, SemVer::CompareType::Minor));
    // patch part match
    EXPECT(IS_SAME_SCENARIO("1.1.2"sv, "1.1.2"sv, SemVer::CompareType::Patch));
    EXPECT(!IS_SAME_SCENARIO("1.1.2"sv, "1.1.3"sv, SemVer::CompareType::Patch));
    EXPECT(!IS_SAME_SCENARIO("1.1.2"sv, "1.2.2"sv, SemVer::CompareType::Patch));
    EXPECT(!IS_SAME_SCENARIO("1.1.2"sv, "2.1.2"sv, SemVer::CompareType::Patch));
    EXPECT(!IS_SAME_SCENARIO("1.1.2"sv, "1.2.2"sv, SemVer::CompareType::Patch));
    EXPECT(!IS_SAME_SCENARIO("1.1.2"sv, "2.1.2"sv, SemVer::CompareType::Patch));
    EXPECT(!IS_SAME_SCENARIO("1.1.2"sv, "2.2.2"sv, SemVer::CompareType::Patch));
}

TEST_CASE(is_greater_than) // NOLINT(readability-function-cognitive-complexity)
{
    // Just normal versions
    EXPECT(IS_GREATER_THAN_SCENARIO("1.1.3"sv, "1.1.2"sv));
    EXPECT(IS_GREATER_THAN_SCENARIO("1.2.2"sv, "1.1.2"sv));
    EXPECT(IS_GREATER_THAN_SCENARIO("2.1.2"sv, "1.1.2"sv));
    EXPECT(IS_GREATER_THAN_SCENARIO("2.1.3"sv, "1.1.2"sv));
    EXPECT(IS_GREATER_THAN_SCENARIO("1.2.3"sv, "1.1.2"sv));
    EXPECT(IS_GREATER_THAN_SCENARIO("1.2.2"sv, "1.1.2"sv));
    EXPECT(!IS_GREATER_THAN_SCENARIO("1.1.2"sv, "1.1.2"sv));

    // Basic, imbalanced prereleased testing
    EXPECT(!IS_GREATER_THAN_SCENARIO("1.0.0-alpha"sv, "1.0.0-alpha"sv));
    EXPECT(!IS_GREATER_THAN_SCENARIO("1.0.0-alpha"sv, "1.0.0"sv));
    EXPECT(IS_GREATER_THAN_SCENARIO("1.0.0"sv, "1.0.0-0"sv));

    // Both versions have more than one identifiers
    // 1. All numeric
    EXPECT(IS_GREATER_THAN_SCENARIO("1.0.0-0.1.2"sv, "1.0.0-0.1.1"sv));
    EXPECT(IS_GREATER_THAN_SCENARIO("1.0.0-0.2.0"sv, "1.0.0-0.1.2"sv));
    EXPECT(!IS_GREATER_THAN_SCENARIO("1.0.0-0.1.2"sv, "1.0.0-0.1.2"sv));

    // 2. For non-numeric, lexical compare
    EXPECT(IS_GREATER_THAN_SCENARIO("1.0.0-beta"sv, "1.0.0-alpha"sv));
    EXPECT(IS_GREATER_THAN_SCENARIO("1.0.0-0.beta"sv, "1.0.0-0.alpha"sv));

    // 3. Either one is numeric, but not both, then numeric given low precedence
    EXPECT(IS_GREATER_THAN_SCENARIO("1.0.0-0.alpha"sv, "1.0.0-0.0"sv));
    EXPECT(!IS_GREATER_THAN_SCENARIO("1.0.0-0.0"sv, "1.0.0-0.alpha"sv));

    // 4. Prefix identifiers are same, larger has high precedence
    EXPECT(IS_GREATER_THAN_SCENARIO("1.0.0-alpha.beta.gamma"sv, "1.0.0-alpha"sv));
}

TEST_CASE(is_lesser_than) // NOLINT(readability-function-cognitive-complexity)
{
    // This function depends on is_greater_than, so basic testing is OK
    EXPECT(IS_LESSER_THAN_SCENARIO("1.1.2"sv, "1.1.3"sv));
    EXPECT(IS_LESSER_THAN_SCENARIO("1.1.2"sv, "1.2.2"sv));
    EXPECT(IS_LESSER_THAN_SCENARIO("1.1.2"sv, "2.1.2"sv));
    EXPECT(IS_LESSER_THAN_SCENARIO("1.1.2"sv, "2.1.3"sv));
    EXPECT(IS_LESSER_THAN_SCENARIO("1.1.2"sv, "1.2.3"sv));
    EXPECT(IS_LESSER_THAN_SCENARIO("1.1.2"sv, "1.2.2"sv));
    EXPECT(!IS_LESSER_THAN_SCENARIO("1.1.2"sv, "1.1.2"sv));
}

TEST_CASE(satisfies) // NOLINT(readability-function-cognitive-complexity)
{
    auto version = GET_SEMVER("1.1.2-prerelease+meta"sv);

    EXPECT(version.satisfies("1.1.2-prerelease+meta"sv));
    EXPECT(!version.satisfies("1.2.2-prerelease+meta"sv));
    EXPECT(!version.satisfies("!=1.1.2-prerelease+meta"sv));
    EXPECT(version.satisfies("!=1.2.2-prerelease+meta"sv));
    EXPECT(version.satisfies("=1.1.2"sv));
    EXPECT(version.satisfies("=1.1.2-prerelease+meta"sv));
    EXPECT(!version.satisfies("=1.1.3"sv));
    EXPECT(!version.satisfies("==1.1.3-prerelease+meta"sv));
    EXPECT(version.satisfies("==1.1.2-prerelease"sv));
    EXPECT(version.satisfies("==1.1.2-prerelease+meta"sv));
    EXPECT(!version.satisfies("<1.1.1-prerelease+meta"sv));
    EXPECT(!version.satisfies("<1.1.2-prerelease+meta"sv));
    EXPECT(version.satisfies("<1.1.3-prerelease+meta"sv));
    EXPECT(version.satisfies(">1.1.1-prerelease+meta"sv));
    EXPECT(!version.satisfies(">1.1.2-prerelease+meta"sv));
    EXPECT(!version.satisfies(">1.1.3-prerelease+meta"sv));
    EXPECT(version.satisfies(">=1.1.1-prerelease+meta"sv));
    EXPECT(version.satisfies(">=1.1.2-prerelease+meta"sv));
    EXPECT(!version.satisfies(">=1.1.3-prerelease+meta"sv));
    EXPECT(!version.satisfies("<=1.1.1-prerelease+meta"sv));
    EXPECT(version.satisfies("<=1.1.2-prerelease+meta"sv));
    EXPECT(version.satisfies("<=1.1.3-prerelease+meta"sv));
    EXPECT(!version.satisfies("HELLO1.1.2-prerelease+meta"sv));
}
