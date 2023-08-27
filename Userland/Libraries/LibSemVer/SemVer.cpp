/*
 * Copyright (c) 2023, Gurkirat Singh <tbhaxor@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericLexer.h>
#include <AK/StringBuilder.h>

#include <LibRegex/RegexMatcher.h>
#include <LibRegex/RegexParser.h>
#include <LibSemVer/Semver.h>

namespace SemVer {
String SemVer::suffix() const
{
    StringBuilder sb;
    if (m_prerelease.bytes().size() > 0) {
        sb.append('-');
        sb.append(m_prerelease);
    }
    if (m_buildmetadata.bytes().size() > 0) {
        sb.append('+');
        sb.append(m_buildmetadata);
    }
    return MUST(sb.to_string());
}

String SemVer::to_string() const
{
    return MUST(String::formatted("{}.{}.{}{}", m_major, m_minor, m_patch, suffix()));
}

ReleaseType SemVer::release_type() const
{
    auto prerel_identifiers = MUST(MUST(m_prerelease.to_lowercase()).split('.'));
    auto is_alpha = prerel_identifiers.contains_slow("alpha"_string);
    auto is_beta = prerel_identifiers.contains_slow("beta"_string);

    if (is_alpha && is_beta) {
        return ReleaseType::AlphaBeta;
    }

    if (is_alpha) {
        return ReleaseType::Alpha;
    }

    if (is_beta) {
        return ReleaseType::Beta;
    }

    if (prerel_identifiers.contains_slow("prerelease"_string)) {
        return ReleaseType::PreRelease;
    }

    // strict release candidate check
    if (prerel_identifiers.contains_slow("rc"_string)) {
        return ReleaseType::ReleaseCandidate;
    }

    return ReleaseType::Stable;
}

ErrorOr<SemVer> SemVer::bump(BumpType type, BumpOpts opt) const
{
    String prerelease;
    String buildmetadata;

    switch (opt) {
    case BumpOpts::KeepSuffix:
        prerelease = m_prerelease;
        buildmetadata = m_buildmetadata;
        break;
    case BumpOpts::ClearBuildMeta:
        prerelease = m_prerelease;
        break;
    case BumpOpts::ClearPreRelease:
        buildmetadata = m_buildmetadata;
        break;
    default:
        return Error::from_string_view("Reached impasse"sv);
    }

    switch (type) {
    case BumpType::Major:
        return SemVer(m_major + 1, m_minor, m_patch, prerelease, buildmetadata);
    case BumpType::Minor:
        return SemVer(m_major, m_minor + 1, m_patch, prerelease, buildmetadata);
    case BumpType::Patch:
        return SemVer(m_major, m_minor, m_patch + 1, prerelease, buildmetadata);
    }

    return Error::from_string_view("Reached impasse"sv);
}

bool SemVer::is_same(SemVer const& other, CompareType compare_type) const
{
    switch (compare_type) {
    case CompareType::Major:
        return m_major == other.m_major;
    case CompareType::Minor:
        return m_major == other.m_major && m_minor == other.m_minor;
    case CompareType::Patch:
        return m_major == other.m_major && m_minor == other.m_minor && m_patch == other.m_patch;
    default:
        return to_string() == other.to_string();
    }
}

bool SemVer::is_greater_than(SemVer const& other) const { return m_major > other.m_major || m_minor > other.m_minor || m_patch > other.m_patch; }

bool SemVer::is_lesser_than(SemVer const& other) const { return m_major < other.m_major || m_minor < other.m_minor || m_patch < other.m_patch; }

bool SemVer::satisfies(StringView const& spec) const
{
    GenericLexer lexer(spec.trim_whitespace());
    if (lexer.tell_remaining() == 0) {
        return false;
    }

    auto compare_op = lexer.consume_until<Function<bool(char)>>([](auto const& ch) {
        return ch >= '0' && ch <= '9';
    });

    auto version = MUST(from_string_view(lexer.consume_all()));
    if (compare_op.length() == 0) {
        return is_same(version, CompareType::Minor);
    }
    if (compare_op == "!=") {
        return !is_same(version);
    }
    if (compare_op == "=="sv) {
        return is_same(version);
    }

    // spec is greater than current semver
    if (compare_op == ">"sv) {
        return is_greater_than(version);
    }
    if (compare_op == "<"sv) {
        return is_lesser_than(version);
    }
    if (compare_op == ">="sv) {
        return is_same(version) || is_greater_than(version);
    }
    if (compare_op == "<="sv) {
        return is_same(version) || is_lesser_than(version);
    }

    return false;
}

ErrorOr<SemVer> from_string_view(StringView const& version)
{
    using namespace regex;
    if (version.count('.') < 2) {
        return AK::Error::from_string_view("Insufficient dot separator"sv);
    }

    if (version.count('+') > 1) {
        return AK::Error::from_string_view("Build metadata must be defined once"sv);
    }

    GenericLexer lexer(version.trim_whitespace());
    if (lexer.tell_remaining() == 0) {
        return AK::Error::from_string_view("Version string is empty"sv);
    }

    auto version_part = lexer.consume_until('.').to_uint<u64>();
    if (!version_part.has_value()) {
        return AK::Error::from_string_view("Major version is not numeric"sv);
    }
    auto v_major = version_part.value();

    lexer.consume();

    version_part = lexer.consume_until('.').to_uint<u64>();
    if (!version_part.has_value()) {
        return AK::Error::from_string_view("Minor version is not numeric"sv);
    }
    auto v_minor = version_part.value();

    lexer.consume();

    version_part = lexer.consume_while<Function<bool(char)>>([](char ch) {
                            return ch >= '0' && ch <= '9';
                        })
                       .to_uint<u64>();
    if (!version_part.has_value()) {
        return AK::Error::from_string_view("Patch version is not numeric"sv);
    }
    auto v_patch = version_part.value();

    if (lexer.is_eof()) {
        return SemVer(v_major, v_minor, v_patch);
    }

    auto const& suffix_ch = lexer.consume();
    StringView buildmetadata;
    StringView prerelease;

    switch (suffix_ch) {
    case '+':
        buildmetadata = lexer.consume_all();
        break;
    case '-':
        prerelease = lexer.consume_until('+');

        if (!lexer.is_eof()) {
            lexer.consume();
            if (lexer.is_eof()) {
                return AK::Error::from_string_view("Build metadata is required"sv);
            }
            buildmetadata = lexer.consume_all();
        }
        break;
    default:
        // TODO: Add context information like actual character (peek) and its index
        // "Expected prerelease (-) or build metadata (+) character at {}. Found {}"
        return AK::Error::from_string_view("Malformed version syntax. Expected + or - characters"sv);
    }

    // Validating consumed prerelease and buildmetadata
    Regex<ECMA262Parser> only_digits_prerelease("^(\\.?\\d+\\.?)(\\d+\\.?)*$");
    if (prerelease.ends_with('.')
        || only_digits_prerelease.has_match(prerelease)
        || prerelease.contains(".."sv) // two more more dots are not allowed
        || prerelease.contains('_')) /* black list character */ {
        return AK::Error::from_string_view("Invalid prerelease part"sv);
    }

    if (buildmetadata.starts_with('.')) {
        return AK::Error::from_string_view("Invalid build metadata part"sv);
    }

    return SemVer(v_major, v_minor, v_patch, TRY(String::from_utf8(prerelease)), TRY(String::from_utf8(buildmetadata)));
}

bool is_valid(StringView const& version)
{
    auto result = from_string_view(version);
    return !result.is_error() && result.release_value().to_string() == version;
}

}
