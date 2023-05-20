/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "InstalledPort.h"
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>

class AvailablePort {
public:
    static ErrorOr<int> query_details_for_package(HashMap<String, AvailablePort>& available_ports, HashMap<String, InstalledPort>& installed_ports, StringView package_name, bool verbose);
    static ErrorOr<HashMap<String, AvailablePort>> read_available_ports_list();
    static ErrorOr<int> update_available_ports_list_file();

    AvailablePort(String name, String version, String website)
        : m_name(name)
        , m_website(move(website))
        , m_version(move(version))
    {
    }

    StringView name() const { return m_name.bytes_as_string_view(); }
    StringView version() const { return m_version.bytes_as_string_view(); }
    StringView website() const { return m_website.bytes_as_string_view(); }

private:
    String m_name;
    String m_website;
    String m_version;
};
