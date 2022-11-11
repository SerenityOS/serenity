/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BrowserWindow.h"
#include <AK/Error.h>
#include <AK/String.h>
#include <Applications/Browser/WebDriverSessionClientEndpoint.h>
#include <Applications/Browser/WebDriverSessionServerEndpoint.h>
#include <LibCore/LocalServer.h>
#include <LibGUI/Application.h>
#include <LibIPC/ConnectionToServer.h>
#include <unistd.h>

namespace Browser {

class WebDriverConnection final
    : public IPC::ConnectionToServer<WebDriverSessionClientEndpoint, WebDriverSessionServerEndpoint> {
    C_OBJECT_ABSTRACT(WebDriverConnection)
public:
    static ErrorOr<NonnullRefPtr<WebDriverConnection>> connect_to_webdriver(NonnullRefPtr<BrowserWindow> browser_window, String path)
    {
        dbgln_if(WEBDRIVER_DEBUG, "Trying to connect to {}", path);
        auto result = TRY(Core::Stream::LocalSocket::connect(path));
        dbgln_if(WEBDRIVER_DEBUG, "Connected to WebDriver");
        return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) WebDriverConnection(move(result), browser_window)));
    }

    virtual ~WebDriverConnection() = default;

    virtual void die() override { }

    virtual void quit() override;
    virtual Messages::WebDriverSessionClient::GetTitleResponse get_title() override;

private:
    WebDriverConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket, NonnullRefPtr<BrowserWindow> browser_window);

    WeakPtr<BrowserWindow> m_browser_window;
};

}
