/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/String.h>
#include <Applications/Browser/WebDriverSessionClientEndpoint.h>
#include <Applications/Browser/WebDriverSessionServerEndpoint.h>
#include <LibCore/LocalServer.h>
#include <LibGUI/Application.h>
#include <LibIPC/ConnectionToServer.h>
#include <unistd.h>

namespace Browser {
class ConnectionToServer final
    : public IPC::ConnectionToServer<WebDriverSessionClientEndpoint, WebDriverSessionServerEndpoint> {
    C_OBJECT_ABSTRACT(ConnectionToServer)
public:
    static ErrorOr<void> connect_to_webdriver(AK::NonnullRefPtr<GUI::Application> app, String path)
    {
        dbgln("Trying to connect to {}", path);
        auto result = TRY(Core::Stream::LocalSocket::connect(path));
        dbgln("Connected to WebDriver");
        s_the = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ConnectionToServer(move(result), app)));

        return {};
    }

    virtual ~ConnectionToServer() = default;

    virtual void die() override { }

    virtual void quit() override;
    virtual Messages::WebDriverSessionClient::GetUrlResponse get_url() override;
    virtual void set_url(AK::URL const& url) override;
    virtual Messages::WebDriverSessionClient::GetTitleResponse get_title() override;

private:
    ConnectionToServer(NonnullOwnPtr<Core::Stream::LocalSocket> socket, NonnullRefPtr<GUI::Application> app);

    NonnullRefPtr<GUI::Application> m_app;

    static RefPtr<ConnectionToServer> s_the;
};
}
