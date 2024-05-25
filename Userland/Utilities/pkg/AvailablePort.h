/*
 * Copyright (c) 2023-2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "InstalledPort.h"
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>

class AvailablePort : public Port {
public:
    AvailablePort(String const& name, String const& version, String const& website)
        : Port(name, version)
        , m_website(website)
    {
    }

    StringView website() const { return m_website.bytes_as_string_view(); }

private:
    String m_website;
};
