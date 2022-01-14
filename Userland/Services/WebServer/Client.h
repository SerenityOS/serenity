/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Object.h>
#include <LibCore/Stream.h>
#include <LibHTTP/Forward.h>
#include <LibHTTP/HttpRequest.h>

namespace WebServer {

class Client final : public Core::Object {
    C_OBJECT(Client);

public:
    void start();

private:
    Client(NonnullOwnPtr<Core::Stream::BufferedTCPSocket>, Core::Object* parent);

    ErrorOr<bool> handle_request(ReadonlyBytes);
    ErrorOr<void> send_response(InputStream&, HTTP::HttpRequest const&, String const& content_type);
    ErrorOr<void> send_redirect(StringView redirect, HTTP::HttpRequest const&);
    ErrorOr<void> send_error_response(unsigned code, HTTP::HttpRequest const&, Vector<String> const& headers = {});
    void die();
    void log_response(unsigned code, HTTP::HttpRequest const&);
    ErrorOr<void> handle_directory_listing(String const& requested_path, String const& real_path, HTTP::HttpRequest const&);
    bool verify_credentials(Vector<HTTP::HttpRequest::Header> const&);

    NonnullOwnPtr<Core::Stream::BufferedTCPSocket> m_socket;
};

}
