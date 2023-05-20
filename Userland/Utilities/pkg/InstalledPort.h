/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>

class InstalledPort {
public:
    enum class Type {
        Auto,
        Dependency,
        Manual,
    };

    static ErrorOr<HashMap<String, InstalledPort>> read_ports_database();
    static ErrorOr<void> for_each_by_type(HashMap<String, InstalledPort>&, Type type, Function<ErrorOr<void>(InstalledPort const&)> callback);

    InstalledPort(String name, Type type, String version)
        : m_name(name)
        , m_type(type)
        , m_version(move(version))
    {
    }

    Type type() const { return m_type; }
    StringView type_as_string_view() const
    {
        if (m_type == Type::Auto)
            return "Automatic"sv;
        if (m_type == Type::Dependency)
            return "Dependency"sv;
        if (m_type == Type::Manual)
            return "Manual"sv;
        VERIFY_NOT_REACHED();
    }

    StringView name() const { return m_name.bytes_as_string_view(); }
    StringView version() const { return m_version.bytes_as_string_view(); }

private:
    String m_name;
    Type m_type;
    String m_version;
};
