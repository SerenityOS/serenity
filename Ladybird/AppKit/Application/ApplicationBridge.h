/*
 * Copyright (c) 2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/Vector.h>
#include <LibIPC/Forward.h>
#include <LibSQL/SQLClient.h>
#include <LibWebView/Forward.h>

namespace Ladybird {

struct ApplicationBridgeImpl;
class WebViewBridge;

class ApplicationBridge {
public:
    ApplicationBridge();
    ~ApplicationBridge();

    ErrorOr<void> launch_request_server(Vector<ByteString> const& certificates);
    ErrorOr<NonnullRefPtr<SQL::SQLClient>> launch_sql_server();
    ErrorOr<NonnullRefPtr<WebView::WebContentClient>> launch_web_content(WebViewBridge&);
    ErrorOr<IPC::File> launch_web_worker();

private:
    NonnullOwnPtr<ApplicationBridgeImpl> m_impl;
};

}
