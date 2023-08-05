/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebSocketClientManagerQt.h"
#include "WebSocketImplQt.h"
#include "WebSocketQt.h"

namespace Ladybird {

NonnullRefPtr<WebSocketClientManagerQt> WebSocketClientManagerQt::create()
{
    return adopt_ref(*new WebSocketClientManagerQt());
}

WebSocketClientManagerQt::WebSocketClientManagerQt() = default;
WebSocketClientManagerQt::~WebSocketClientManagerQt() = default;

RefPtr<Web::WebSockets::WebSocketClientSocket> WebSocketClientManagerQt::connect(AK::URL const& url, DeprecatedString const& origin, Vector<DeprecatedString> const& protocols)
{
    WebSocket::ConnectionInfo connection_info(url);
    connection_info.set_origin(origin);
    connection_info.set_protocols(protocols);

    auto impl = adopt_ref(*new WebSocketImplQt);
    auto web_socket = WebSocket::WebSocket::create(move(connection_info), move(impl));
    web_socket->start();
    return WebSocketQt::create(web_socket);
}

}
