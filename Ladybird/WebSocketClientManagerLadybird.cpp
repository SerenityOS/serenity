/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebSocketClientManagerLadybird.h"
#include "WebSocketLadybird.h"

namespace Ladybird {

NonnullRefPtr<WebSocketClientManagerLadybird> WebSocketClientManagerLadybird::create()
{
    return adopt_ref(*new WebSocketClientManagerLadybird());
}

WebSocketClientManagerLadybird::WebSocketClientManagerLadybird() = default;
WebSocketClientManagerLadybird::~WebSocketClientManagerLadybird() = default;

RefPtr<Web::WebSockets::WebSocketClientSocket> WebSocketClientManagerLadybird::connect(AK::URL const& url, String const& origin)
{
    WebSocket::ConnectionInfo connection_info(url);
    connection_info.set_origin(origin);

    auto connection = WebSocketLadybird::create(WebSocket::WebSocket::create(move(connection_info)));
    return connection;
}

}
