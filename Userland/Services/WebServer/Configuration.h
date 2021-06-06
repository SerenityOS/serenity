/*
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace WebServer {

class Configuration {
public:
    Configuration(String root_path);

    String const& root_path() const { return m_root_path; }

    void set_root_path(String root_path) { m_root_path = move(root_path); }

    static Configuration const& the();

private:
    String m_root_path;
};

}
