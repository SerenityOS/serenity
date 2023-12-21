/*
 * Copyright (c) 2023, Gurkirat Singh <tbhaxor@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Find.h>
#include <AK/GenericLexer.h>
#include <AK/ReverseIterator.h>
#include <AK/StringBuilder.h>
#include <LibSemVer/SemVer.h>

namespace SemVer {
String SemVer::suffix() const
{
    StringBuilder sb;
    if (!m_prerelease_identifiers.is_empty())
        sb.appendff("-{}", prerelease());
    if (!m_build_metadata_identifiers.is_empty())
        sb.appendff("+{}", build_metadata());
    return sb.to_string().release_value_but_fixme_should_propagate_errors();
}

String SemVer::to_string() const
{
    return String::formatted("{}{}{}{}{}{}", m_major, m_number_separator, m_minor, m_number_separator, m_patch, suffix()).release_value_but_fixme_should_propagate_errors();
}

SemVer SemVer::bump(BumpType type) const
{
    switch (type) {
    case BumpType::Major:
        return SemVer(m_major + 1, 0, 0, m_number_separator);
    case BumpType::Minor:
        return SemVer(m_major, m_minor + 1, 0, m_number_separator);
    case BumpType::Patch:
        return SemVer(m_major, m_minor, m_patch + 1, m_number_separator);
    case BumpType::Prerelease: {
        Vector<String> prerelease_identifiers = m_prerelease_identifiers;
        bool is_found = false;

        // Unlike comparision, prerelease bumps take from RTL.
        for (auto& identifier : AK::ReverseWrapper::in_reverse(prerelease_identifiers)) {
            auto numeric_identifier = identifier.to_number<u32>();
            if (numeric_identifier.has_value()) {
                is_found = true;
                identifier = String::formatted("{}", numeric_identifier.value() + 1).release_value_but_fixme_should_propagate_errors();
                break;
            }
        }

        // Append 0 identifier if there is no numeric found to be bumped.
        if (!is_found)
            prerelease_identifiers.append("0"_string);

        return SemVer(m_major, m_minor, m_patch, m_number_separator, prerelease_identifiers, {});
    }
    default:
        VERIFY_NOT_REACHED();
    }
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
        // Build metadata MUST be ignored when determining version precedence.
        return m_major == other.m_major && m_minor == other.m_minor && m_patch == other.m_patch && prerelease() == other.prerelease();
    }
}

bool SemVer::is_greater_than(SemVer const& other) const
{
    // Priortize the normal version string.
    // Precedence is determined by the first difference when comparing them from left to right.
    // Major > Minor > Patch
    if (m_major > other.m_major || m_minor > other.m_minor || m_patch > other.m_patch)
        return true;

    // When major, minor, and patch are equal, a pre-release version has lower precedence than a normal version.
    // Example: 1.0.0-alpha < 1.0.0
    if (prerelease() == other.prerelease() || other.prerelease().is_empty())
        return false;
    if (prerelease().is_empty())
        return true;

    // Both the versions have non-zero length of pre-release identifiers.
    for (size_t i = 0; i < min(prerelease_identifiers().size(), other.prerelease_identifiers().size()); ++i) {
        auto const this_numerical_identifier = m_prerelease_identifiers[i].to_number<u32>();
        auto const other_numerical_identifier = other.m_prerelease_identifiers[i].to_number<u32>();

        // 1. Identifiers consisting of only digits are compared numerically.
        if (this_numerical_identifier.has_value() && other_numerical_identifier.has_value()) {
            auto const this_value = this_numerical_identifier.value();
            auto const other_value = other_numerical_identifier.value();

            if (this_value == other_value) {
                continue;
            }
            return this_value > other_value;
        }

        // 2. Identifiers with letters or hyphens are compared lexically in ASCII sort order.
        if (!this_numerical_identifier.has_value() && !other_numerical_identifier.has_value()) {
            if (m_prerelease_identifiers[i] == other.m_prerelease_identifiers[i]) {
                continue;
            }
            return m_prerelease_identifiers[i] > other.m_prerelease_identifiers[i];
        }

        // 3. Numeric identifiers always have lower precedence than non-numeric identifiers.
        if (this_numerical_identifier.has_value() && !other_numerical_identifier.has_value())
            return false;
        if (!this_numerical_identifier.has_value() && other_numerical_identifier.has_value())
            return true;
    }

    // 4. If all of the preceding identifiers are equal, larger set of pre-release fields has a higher precedence than a smaller set.
    return m_prerelease_identifiers.size() > other.m_prerelease_identifiers.size();
}

bool SemVer::satisfies(StringView const& semver_spec) const
{
    GenericLexer lexer(semver_spec.trim_whitespace());
    if (lexer.tell_remaining() == 0)
        return false;

    auto compare_op = lexer.consume_until([](auto const& ch) { return ch >= '0' && ch <= '9'; });

    auto spec_version = MUST(from_string_view(lexer.consume_all()));
    // Lenient compare, tolerance for any patch and pre-release.
    if (compare_op.is_empty())
        return is_same(spec_version, CompareType::Minor);
    if (compare_op == "!="sv)
        return !is_same(spec_version);

    // Adds strictness based on number of equal sign.
    if (compare_op == "="sv)
        return is_same(spec_version, CompareType::Patch);
    // Exact version string match.
    if (compare_op == "=="sv)
        return is_same(spec_version);

    // Current version is greater than spec.
    if (compare_op == ">"sv)
        return is_greater_than(spec_version);
    if (compare_op == "<"sv)
        return is_lesser_than(spec_version);
    if (compare_op == ">="sv)
        return is_same(spec_version) || is_greater_than(spec_version);
    if (compare_op == "<="sv)
        return is_same(spec_version) || !is_greater_than(spec_version);

    return false;
}

ErrorOr<SemVer> from_string_view(StringView const& version, char normal_version_separator)
{
    if (is_ascii_space(normal_version_separator) || is_ascii_digit(normal_version_separator)) {
        return Error::from_string_view("Version separator can't be a space or digit character"sv);
    }

    if (version.count(normal_version_separator) < 2)
        return Error::from_string_view("Insufficient occurrences of version separator"sv);

    if (version.count('+') > 1)
        return Error::from_string_view("Build metadata must be defined at most once"sv);

    // Checks for the bad charaters
    // Spec: https://semver.org/#backusnaur-form-grammar-for-valid-semver-versions
    auto trimmed_version = version.trim_whitespace();
    for (auto const& code_point : trimmed_version.bytes()) {
        if (is_ascii_space(code_point) || code_point == '_') {
            return Error::from_string_view("Bad characters found in the version string"sv);
        }
    }

    GenericLexer lexer(trimmed_version);
    if (lexer.tell_remaining() == 0)
        return Error::from_string_view("Version string is empty"sv);

    // Parse the normal version parts.
    // https://semver.org/#spec-item-2
    auto version_part = lexer.consume_until(normal_version_separator).to_number<u64>();
    if (!version_part.has_value())
        return Error::from_string_view("Major version is not numeric"sv);
    auto version_major = version_part.value();

    lexer.consume();

    version_part = lexer.consume_until(normal_version_separator).to_number<u64>();
    if (!version_part.has_value())
        return Error::from_string_view("Minor version is not numeric"sv);
    auto version_minor = version_part.value();

    lexer.consume();

    version_part = lexer.consume_while([](char ch) { return ch >= '0' && ch <= '9'; }).to_number<u64>();
    if (!version_part.has_value())
        return Error::from_string_view("Patch version is not numeric"sv);
    auto version_patch = version_part.value();

    if (lexer.is_eof())
        return SemVer(version_major, version_minor, version_patch, normal_version_separator);

    Vector<String> build_metadata_identifiers;
    Vector<String> prerelease_identifiers;

    auto process_build_metadata = [&lexer, &build_metadata_identifiers]() -> ErrorOr<void> {
        // Function body strictly adheres to the spec
        // Spec: https://semver.org/#spec-item-10
        if (lexer.is_eof()) {
            return Error::from_string_view("Build metadata can't be empty"sv);
        }

        auto build_metadata = TRY(String::from_utf8(lexer.consume_all()));
        build_metadata_identifiers = TRY(build_metadata.split('.'));

        // Because there is no mention about leading zero in the spec, only empty check is used
        for (auto& identifier : build_metadata_identifiers) {
            if (identifier.is_empty()) {
                return Error::from_string_view("Build metadata identifier must be non empty string"sv);
            }
        }

        return {};
    };

    switch (lexer.consume()) {
    case '+': {
        // Build metadata always starts with the + symbol after normal version string.
        TRY(process_build_metadata());
        break;
    }
    case '-': {
        // Pre-releases always start with the - symbol after normal version string.
        // Spec: https://semver.org/#spec-item-9
        if (lexer.is_eof())
            return Error::from_string_view("Pre-release can't be empty"sv);

        auto prerelease = TRY(String::from_utf8(lexer.consume_until('+')));

        constexpr auto is_valid_identifier = [](String const& identifier) {
            for (auto const& code_point : identifier.code_points()) {
                if (!is_ascii_alphanumeric(code_point) && code_point != '-') {
                    return false;
                }
            }
            return true;
        };
        // Parts of prerelease (identitifers) are separated by dot (.)
        prerelease_identifiers = TRY(prerelease.split('.'));
        for (auto const& prerelease_identifier : prerelease_identifiers) {
            // Empty identifiers are not allowed.
            if (prerelease_identifier.is_empty())
                return Error::from_string_view("Prerelease identifier can't be empty"sv);

            // If there are multiple digits, it can't start with 0 digit.
            // 1.2.3-0 or 1.2.3-0is.legal are valid, but not 1.2.3-00 or 1.2.3-01
            auto identifier_bytes = prerelease_identifier.bytes();
            if (identifier_bytes.size() > 1 && prerelease_identifier.starts_with('0') && is_ascii_digit(identifier_bytes[1]))
                return Error::from_string_view("Prerelease identifier has leading redundant zeroes"sv);

            // Validate identifier against charset
            if (!is_valid_identifier(prerelease_identifier))
                return Error::from_string_view("Characters in prerelease identifier must be either hyphen (-), dot (.) or alphanumeric"sv);
        }

        if (!lexer.is_eof()) {
            // This would invalidate the following versions.
            // 1.2.3-pre$ss 1.2.3-pre.1.0*build-meta
            if (lexer.consume() != '+') {
                return Error::from_string_view("After processing pre-release, only + character is allowed for build metadata information"sv);
            }

            // Process the pending build metadata information, ignoring invalids like following.
            // 1.2.3-pre+ is not a valid version.
            TRY(process_build_metadata());
        }
        break;
    }
    default:
        // TODO: Add context information like actual character (peek) and its index, use the following format.
        // "Expected prerelease (-) or build metadata (+) character at {}. Found {}"
        return Error::from_string_view("Malformed version syntax. Expected + or - characters"sv);
    }

    return SemVer(version_major, version_minor, version_patch, normal_version_separator, prerelease_identifiers, build_metadata_identifiers);
}

bool is_valid(StringView const& version, char normal_version_separator)
{
    auto result = from_string_view(version, normal_version_separator);
    return !result.is_error() && result.release_value().to_string() == version;
}
}
