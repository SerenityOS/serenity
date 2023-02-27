/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Thomas Keppler <serenity@tkeppler.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibCore/Object.h>
#include <LibCore/Socket.h>
#include <LibHTTP/Forward.h>
#include <LibHTTP/HttpRequest.h>

namespace WebServer {

class Client final : public Core::Object {
    C_OBJECT(Client);

public:
    void start();

private:
    Client(NonnullOwnPtr<Core::BufferedTCPSocket>, Core::Object* parent);

    struct ContentInfo {
        String type;
        size_t length {};
    };

    ErrorOr<bool> handle_request(ReadonlyBytes);
    ErrorOr<void> send_response(Stream&, HTTP::HttpRequest const&, ContentInfo);
    ErrorOr<void> send_redirect(StringView redirect, HTTP::HttpRequest const&);
    ErrorOr<void> send_error_response(unsigned code, HTTP::HttpRequest const&, Vector<String> const& headers = {});
    void die();
    void log_response(unsigned code, HTTP::HttpRequest const&);
    ErrorOr<void> handle_directory_listing(String const& requested_path, String const& real_path, HTTP::HttpRequest const&);
    bool verify_credentials(Vector<HTTP::HttpRequest::Header> const&);

    NonnullOwnPtr<Core::BufferedTCPSocket> m_socket;
};

}
