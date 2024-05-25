/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AvailablePort.h"
#include "InstalledPort.h"
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibCore/File.h>

class AvailablePortDatabase {
public:
    static constexpr StringView default_path = "/usr/Ports/AvailablePorts.md"sv;

    static ErrorOr<NonnullOwnPtr<AvailablePortDatabase>> instantiate_ports_database(StringView path);
    static ErrorOr<int> download_available_ports_list_file(StringView path);

    void query_details_for_package(HashMap<String, InstalledPort> const& installed_ports_map, StringView package_name, bool verbose);

    HashMap<String, AvailablePort> const& map() const { return m_available_ports; }
    String const& path() const { return m_path; }

private:
    ErrorOr<HashMap<String, AvailablePort>> read_available_ports_list();

    // NOTE: Because the available ports list is updated in a sequential way
    // we don't preserve a file descriptor for it, but rather it's preferable
    // to simply drop the database and re-read it after an update.
    AvailablePortDatabase(HashMap<String, AvailablePort> available_ports, String path)
        : m_available_ports(available_ports)
        , m_path(path)
    {
    }

    HashMap<String, AvailablePort> m_available_ports;
    String m_path;
};
