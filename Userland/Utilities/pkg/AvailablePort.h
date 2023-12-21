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

class AvailablePort : public Port {
public:
    static void query_details_for_package(HashMap<String, AvailablePort>& available_ports, HashMap<String, InstalledPort> const& installed_ports, StringView package_name, bool verbose);
    static ErrorOr<HashMap<String, AvailablePort>> read_available_ports_list();
    static ErrorOr<int> update_available_ports_list_file();

    AvailablePort(String const& name, String const& version, String const& website)
        : Port(name, version)
        , m_website(website)
    {
    }

    StringView website() const { return m_website.bytes_as_string_view(); }

private:
    String m_website;
};
