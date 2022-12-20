/*
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <WebServer/Configuration.h>

namespace WebServer {

static Configuration* s_configuration = nullptr;

Configuration::Configuration(DeprecatedString document_root_path)
    : m_document_root_path(move(document_root_path))
{
    VERIFY(!s_configuration);
    s_configuration = this;
}

Configuration const& Configuration::the()
{
    VERIFY(s_configuration);
    return *s_configuration;
}

}
