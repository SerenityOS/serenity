/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Thomas Keppler <serenity@tkeppler.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibCore/EventReceiver.h>
#include <LibCore/Socket.h>
#include <LibHTTP/Forward.h>
#include <LibHTTP/HttpRequest.h>

namespace WebServer {

class Client final : public Core::EventReceiver {
    C_OBJECT(Client);

public:
    void start();

private:
    Client(NonnullOwnPtr<Core::BufferedTCPSocket>, Core::EventReceiver* parent);

    using WrappedError = Variant<AK::Error, HTTP::HttpRequest::ParseError>;

    struct ContentInfo {
        String type;
        u64 length {};
    };

    ErrorOr<void, WrappedError> on_ready_to_read();
    ErrorOr<bool> handle_request(HTTP::HttpRequest const&);
    ErrorOr<void> send_response(Stream&, HTTP::HttpRequest const&, ContentInfo);
    ErrorOr<void> send_redirect(StringView redirect, HTTP::HttpRequest const&);
    ErrorOr<void> send_error_response(unsigned code, HTTP::HttpRequest const&, Vector<String> const& headers = {});
    void die();
    void log_response(unsigned code, HTTP::HttpRequest const&);
    ErrorOr<void> handle_directory_listing(String const& requested_path, String const& real_path, HTTP::HttpRequest const&);
    bool verify_credentials(Vector<HTTP::Header> const&);

    NonnullOwnPtr<Core::BufferedTCPSocket> m_socket;
    StringBuilder m_remaining_request;
};

}
