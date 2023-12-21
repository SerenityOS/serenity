/*
 * Copyright (c) 2023, Gurkirat Singh <tbhaxor@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <LibSemVer/SemVer.h>

constexpr auto normal_version_separators = ".-"sv;

class Port {

public:
    [[nodiscard]] String name() const { return m_name; }
    [[nodiscard]] String version_string() const { return m_version.has<String>() ? m_version.get<String>() : m_version.get<SemVer::SemVer>().to_string(); }
    [[nodiscard]] ErrorOr<SemVer::SemVer> version_semver() const
    {
        if (m_version.has<String>())
            return Error::from_string_view("This does not have semver"sv);
        return m_version.get<SemVer::SemVer>();
    }

    Port(String const& name, String const& version)
        : m_name(name)
        , m_version(version)
    {
        set_version(version);
    }
    Port(String const& name, String const& version, char normal_version_separator)
        : m_name(name)
        , m_version(version)
    {
        set_version(version, normal_version_separator);
    }

    void set_name(String const& name)
    {
        if (!name.is_empty())
            m_name = name;
    }

    void set_version(StringView const& version)
    {
        for (auto const& normal_version_separator : normal_version_separators) {
            auto semver_parsed = SemVer::from_string_view(version, normal_version_separator);
            if (!semver_parsed.is_error()) {
                m_version = semver_parsed.value();
                return;
            }
        }

        m_version = MUST(String::from_utf8(version));
    }

    void set_version(StringView const& version, char normal_version_separator)
    {
        // If the user has provided the separator, it is safe to assume that they are certain about it.
        // Therefore, it is ideal to crash, indicating that their assumption is incorrect.
        m_version = MUST(SemVer::from_string_view(version, normal_version_separator));
    }

private:
    String m_name;
    Variant<String, SemVer::SemVer> m_version;
};
