/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Object.h>
#include <LibCore/TCPSocket.h>
#include <LibHTTP/Forward.h>

namespace WebServer {

class Client final : public Core::Object {
    C_OBJECT(Client);

public:
    void start();

private:
    Client(NonnullRefPtr<Core::TCPSocket>, Core::Object* parent);

    void handle_request(ReadonlyBytes);
    void send_response(InputStream&, HTTP::HttpRequest const&, String const& content_type);
    void send_redirect(StringView redirect, HTTP::HttpRequest const&);
    void send_error_response(unsigned code, HTTP::HttpRequest const&, Vector<String> const& headers = {});
    void die();
    void log_response(unsigned code, HTTP::HttpRequest const&);
    void handle_directory_listing(String const& requested_path, String const& real_path, HTTP::HttpRequest const&);
    bool verify_credentials(Vector<HTTP::HttpRequest::Header> const&);

    NonnullRefPtr<Core::TCPSocket> m_socket;
};

}
