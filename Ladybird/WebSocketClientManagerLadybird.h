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

class WebSocketClientManagerLadybird : public Web::WebSockets::WebSocketClientManager {
public:
    static NonnullRefPtr<WebSocketClientManagerLadybird> create();

    virtual ~WebSocketClientManagerLadybird() override;
    virtual RefPtr<Web::WebSockets::WebSocketClientSocket> connect(AK::URL const&, String const& origin) override;

private:
    WebSocketClientManagerLadybird();
};

}
