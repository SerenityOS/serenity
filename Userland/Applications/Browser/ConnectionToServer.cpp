/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Applications/Browser/BrowserWindow.h>
#include <Applications/Browser/ConnectionToServer.h>

namespace Browser {

RefPtr<ConnectionToServer> ConnectionToServer::s_the;

ConnectionToServer::ConnectionToServer(NonnullOwnPtr<Core::Stream::LocalSocket> socket, NonnullRefPtr<GUI::Application> app)
    : IPC::ConnectionToServer<WebDriverSessionClientEndpoint, WebDriverSessionServerEndpoint>(*this, move(socket))
    , m_app(app)
{
}

void ConnectionToServer::quit()
{
    dbgln("WebDriverSession: quit");
    m_app->quit();
}

Messages::WebDriverSessionClient::GetUrlResponse ConnectionToServer::get_url()
{
    dbgln("WebDriverSession: get_url");
    if (auto* browser_window = dynamic_cast<Browser::BrowserWindow*>(m_app->active_window())) {
        return { browser_window->active_tab().url() };
    }
    return { nullptr };
}

void ConnectionToServer::set_url(AK::URL const& url)
{
    dbgln("WebDriverSession: set_url {}", url);

    if (auto* browser_window = dynamic_cast<Browser::BrowserWindow*>(m_app->active_window())) {
        browser_window->active_tab().load(url);
    }
}

Messages::WebDriverSessionClient::GetTitleResponse ConnectionToServer::get_title()
{
    dbgln("WebDriverSession: get_title");
    if (auto* browser_window = dynamic_cast<Browser::BrowserWindow*>(m_app->active_window())) {
        return { browser_window->active_tab().title() };
    }
    return { nullptr };
}

}
