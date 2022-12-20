/*
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Optional.h>
#include <LibHTTP/HttpRequest.h>

namespace WebServer {

class Configuration {
public:
    Configuration(DeprecatedString document_root_path);

    DeprecatedString const& document_root_path() const { return m_document_root_path; }
    Optional<HTTP::HttpRequest::BasicAuthenticationCredentials> const& credentials() const { return m_credentials; }

    void set_document_root_path(DeprecatedString root_path) { m_document_root_path = move(root_path); }
    void set_credentials(Optional<HTTP::HttpRequest::BasicAuthenticationCredentials> credentials) { m_credentials = move(credentials); }

    static Configuration const& the();

private:
    DeprecatedString m_document_root_path;
    Optional<HTTP::HttpRequest::BasicAuthenticationCredentials> m_credentials;
};

}
