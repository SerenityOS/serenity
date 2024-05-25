/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "InstalledPort.h"
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibCore/File.h>

class InstalledPortDatabase {
public:
    static constexpr StringView default_path = "/usr/Ports/installed.db"sv;

    static ErrorOr<NonnullOwnPtr<InstalledPortDatabase>> instantiate_ports_database(StringView path);
    ErrorOr<void> insert_new_port_to_ports_database(InstalledPort::Type type, String, InstalledPort, Vector<Port> const& dependencies);
    ErrorOr<void> for_each_by_type(InstalledPort::Type type, Function<ErrorOr<void>(InstalledPort const&)> callback);

    HashMap<String, InstalledPort> const& map() const { return m_installed_ports; }
    String const& path() const { return m_path; }

private:
    InstalledPortDatabase(HashMap<String, InstalledPort> installed_ports, NonnullOwnPtr<Core::File> database_file, String path)
        : m_installed_ports(installed_ports)
        , m_database_file(move(database_file))
        , m_path(path)
    {
    }

    HashMap<String, InstalledPort> m_installed_ports;
    NonnullOwnPtr<Core::File> m_database_file;
    String m_path;
};
