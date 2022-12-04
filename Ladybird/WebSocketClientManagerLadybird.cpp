/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebSocketClientManagerLadybird.h"
#include "WebSocketImplQt.h"
#include "WebSocketLadybird.h"

namespace Ladybird {

NonnullRefPtr<WebSocketClientManagerLadybird> WebSocketClientManagerLadybird::create()
{
    return adopt_ref(*new WebSocketClientManagerLadybird());
}

WebSocketClientManagerLadybird::WebSocketClientManagerLadybird() = default;
WebSocketClientManagerLadybird::~WebSocketClientManagerLadybird() = default;

RefPtr<Web::WebSockets::WebSocketClientSocket> WebSocketClientManagerLadybird::connect(AK::URL const& url, DeprecatedString const& origin)
{
    WebSocket::ConnectionInfo connection_info(url);
    connection_info.set_origin(origin);

    auto impl = adopt_ref(*new WebSocketImplQt);
    auto web_socket = WebSocket::WebSocket::create(move(connection_info), move(impl));
    web_socket->start();
    return WebSocketLadybird::create(web_socket);
}

}
