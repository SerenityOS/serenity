/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/ConnectionToServer.h>
#include <LibWeb/WebDriver/Response.h>
#include <WebContent/Forward.h>
#include <WebContent/WebDriverClientEndpoint.h>
#include <WebContent/WebDriverServerEndpoint.h>

namespace WebContent {

class WebDriverConnection final
    : public IPC::ConnectionToServer<WebDriverClientEndpoint, WebDriverServerEndpoint> {
    C_OBJECT_ABSTRACT(WebDriverConnection)

public:
    static ErrorOr<NonnullRefPtr<WebDriverConnection>> connect(PageHost& page_host, String const& webdriver_ipc_path);
    virtual ~WebDriverConnection() = default;

private:
    WebDriverConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket, PageHost& page_host);

    virtual void die() override { }
    virtual void set_is_webdriver_active(bool) override;

    PageHost& m_page_host;
};

}
