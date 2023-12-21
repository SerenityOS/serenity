/*
 * Copyright (c) 2023, Gurkirat Singh <tbhaxor@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Format.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>

namespace SemVer {
enum class BumpType {
    Major,
    Minor,
    Patch,
    Prerelease,
};

enum class CompareType {
    Exact,
    Major,
    Minor,
    Patch
};

class SemVer {

public:
    SemVer(u64 major, u64 minor, u64 patch, char m_number_separator)
        : m_number_separator(m_number_separator)
        , m_major(major)
        , m_minor(minor)
        , m_patch(patch)
    {
    }

    SemVer(u64 major, u64 minor, u64 patch, char m_number_separator, Vector<String> const& prereleases, Vector<String> const& build_metadata)
        : m_number_separator(m_number_separator)
        , m_major(major)
        , m_minor(minor)
        , m_patch(patch)
        , m_prerelease_identifiers(prereleases)
        , m_build_metadata_identifiers(build_metadata)
    {
    }

    [[nodiscard]] u64 major() const { return m_major; }
    [[nodiscard]] u64 minor() const { return m_minor; }
    [[nodiscard]] u64 patch() const { return m_patch; }
    [[nodiscard]] ReadonlySpan<String> prerelease_identifiers() const { return m_prerelease_identifiers.span(); }
    [[nodiscard]] String prerelease() const
    {
        return String::join('.', m_prerelease_identifiers).release_value_but_fixme_should_propagate_errors();
    }
    [[nodiscard]] ReadonlySpan<String> build_metadata_identifiers() const { return m_build_metadata_identifiers.span(); }
    [[nodiscard]] String build_metadata() const { return String::join('.', m_build_metadata_identifiers).release_value_but_fixme_should_propagate_errors(); }

    [[nodiscard]] SemVer bump(BumpType) const;

    [[nodiscard]] bool is_same(SemVer const&, CompareType = CompareType::Exact) const;
    [[nodiscard]] bool is_greater_than(SemVer const&) const;
    [[nodiscard]] bool is_lesser_than(SemVer const& other) const { return !is_same(other) && !is_greater_than(other); }
    [[nodiscard]] bool operator==(SemVer const& other) const { return is_same(other); }
    [[nodiscard]] bool operator!=(SemVer const& other) const { return !is_same(other); }
    [[nodiscard]] bool operator>(SemVer const& other) const { return is_lesser_than(other); }
    [[nodiscard]] bool operator<(SemVer const& other) const { return is_greater_than(other); }
    [[nodiscard]] bool operator>=(SemVer const& other) const { return *this == other || *this > other; }
    [[nodiscard]] bool operator<=(SemVer const& other) const { return *this == other || *this < other; }

    [[nodiscard]] bool satisfies(StringView const& semver_spec) const;

    [[nodiscard]] String suffix() const;
    [[nodiscard]] String to_string() const;

private:
    char m_number_separator;
    u64 m_major;
    u64 m_minor;
    u64 m_patch;
    Vector<String> m_prerelease_identifiers;
    Vector<String> m_build_metadata_identifiers;
};

ErrorOr<SemVer> from_string_view(StringView const&, char normal_version_separator = '.');

bool is_valid(StringView const&, char normal_version_separator = '.');

}

template<>
struct AK::Formatter<SemVer::SemVer> : Formatter<String> {
    ErrorOr<void> format(FormatBuilder& builder, SemVer::SemVer const& value)
    {
        return Formatter<String>::format(builder, value.to_string());
    }
};
