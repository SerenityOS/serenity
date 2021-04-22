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
    Client(NonnullRefPtr<Core::TCPSocket>, const String&, Core::Object* parent);

    void handle_request(ReadonlyBytes);
    void send_response(InputStream&, const HTTP::HttpRequest&, const String& content_type);
    void send_redirect(StringView redirect, const HTTP::HttpRequest& request);
    void send_error_response(unsigned code, const StringView& message, const HTTP::HttpRequest&);
    void die();
    void log_response(unsigned code, const HTTP::HttpRequest&);
    void handle_directory_listing(const String& requested_path, const String& real_path, const HTTP::HttpRequest&);

    NonnullRefPtr<Core::TCPSocket> m_socket;
    String m_root_path;
};

}
