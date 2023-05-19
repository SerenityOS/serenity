/*
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 * Copyright (c) 2022, Thomas Keppler <serenity@tkeppler.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <WebServer/Configuration.h>

namespace WebServer {

static Configuration* s_configuration = nullptr;

Configuration::Configuration(String document_root_path, Optional<HTTP::HttpRequest::BasicAuthenticationCredentials> credentials)
    : m_document_root_path(move(document_root_path))
    , m_credentials(move(credentials))
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
