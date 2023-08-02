/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/WebSockets/WebSocket.h>
#include <LibWebSocket/ConnectionInfo.h>
#include <LibWebSocket/Message.h>
#include <LibWebSocket/WebSocket.h>

#pragma once

namespace Ladybird {

class WebSocketClientManagerQt : public Web::WebSockets::WebSocketClientManager {
public:
    static NonnullRefPtr<WebSocketClientManagerQt> create();

    virtual ~WebSocketClientManagerQt() override;
    virtual RefPtr<Web::WebSockets::WebSocketClientSocket> connect(AK::URL const&, DeprecatedString const& origin, Vector<DeprecatedString> const& protocols) override;

private:
    WebSocketClientManagerQt();
};

}
