/*
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 * Copyright (c) 2022, Thomas Keppler <serenity@tkeppler.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Optional.h>
#include <LibHTTP/HttpRequest.h>

namespace WebServer {

class Configuration {
public:
    Configuration(String document_root_path, Optional<HTTP::HttpRequest::BasicAuthenticationCredentials> credentials = {});

    String const& document_root_path() const { return m_document_root_path; }
    Optional<HTTP::HttpRequest::BasicAuthenticationCredentials> const& credentials() const { return m_credentials; }

    static Configuration const& the();

private:
    String m_document_root_path;
    Optional<HTTP::HttpRequest::BasicAuthenticationCredentials> m_credentials;
};

}
