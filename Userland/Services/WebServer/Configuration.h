/*
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibHTTP/HttpRequest.h>

namespace WebServer {

class Configuration {
public:
    Configuration(String root_path);

    String const& root_path() const { return m_root_path; }
    Optional<HTTP::HttpRequest::BasicAuthenticationCredentials> const& credentials() const { return m_credentials; }

    void set_root_path(String root_path) { m_root_path = move(root_path); }
    void set_credentials(Optional<HTTP::HttpRequest::BasicAuthenticationCredentials> credentials) { m_credentials = move(credentials); }

    static Configuration const& the();

private:
    String m_root_path;
    Optional<HTTP::HttpRequest::BasicAuthenticationCredentials> m_credentials;
};

}
