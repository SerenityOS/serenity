/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebDriverConnection.h"
#include "BrowserWindow.h"
#include <AK/Vector.h>
#include <LibWeb/Cookie/Cookie.h>

namespace Browser {

WebDriverConnection::WebDriverConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket, NonnullRefPtr<BrowserWindow> browser_window)
    : IPC::ConnectionToServer<WebDriverSessionClientEndpoint, WebDriverSessionServerEndpoint>(*this, move(socket))
    , m_browser_window(move(browser_window))
{
}

void WebDriverConnection::quit()
{
    dbgln("WebDriverConnection: quit");
    if (auto browser_window = m_browser_window.strong_ref())
        browser_window->close();
}

Messages::WebDriverSessionClient::GetUrlResponse WebDriverConnection::get_url()
{
    dbgln("WebDriverConnection: get_url");
    if (auto browser_window = m_browser_window.strong_ref())
        return { browser_window->active_tab().url() };
    return { URL("") };
}

void WebDriverConnection::set_url(AK::URL const& url)
{
    dbgln("WebDriverConnection: set_url {}", url);
    if (auto browser_window = m_browser_window.strong_ref())
        browser_window->active_tab().load(url);
}

Messages::WebDriverSessionClient::GetTitleResponse WebDriverConnection::get_title()
{
    dbgln("WebDriverConnection: get_title");
    if (auto browser_window = m_browser_window.strong_ref())
        return { browser_window->active_tab().title() };
    return { "" };
}

void WebDriverConnection::refresh()
{
    dbgln("WebDriverConnection: refresh");
    if (auto browser_window = m_browser_window.strong_ref())
        browser_window->active_tab().reload();
}

void WebDriverConnection::back()
{
    dbgln("WebDriverConnection: back");
    if (auto browser_window = m_browser_window.strong_ref())
        browser_window->active_tab().go_back();
}

void WebDriverConnection::forward()
{
    dbgln("WebDriverConnection: forward");
    if (auto browser_window = m_browser_window.strong_ref())
        browser_window->active_tab().go_forward();
}

Messages::WebDriverSessionClient::GetAllCookiesResponse WebDriverConnection::get_all_cookies()
{
    dbgln("WebDriverConnection: get_cookies");
    if (auto browser_window = m_browser_window.strong_ref()) {
        if (browser_window->active_tab().on_get_cookies_entries) {
            return { browser_window->active_tab().on_get_cookies_entries() };
        }
    }
    return { {} };
}

Messages::WebDriverSessionClient::GetNamedCookieResponse WebDriverConnection::get_named_cookie(String const& name)
{
    dbgln("WebDriverConnection: get_named_cookie");
    if (auto browser_window = m_browser_window.strong_ref()) {
        if (browser_window->active_tab().on_get_cookies_entries) {
            for (auto cookie : browser_window->active_tab().on_get_cookies_entries()) {
                if (cookie.name == name)
                    return { cookie };
            }
        }
    }
    return { {} };
}

void WebDriverConnection::update_cookie(Web::Cookie::Cookie const& cookie)
{
    if (auto browser_window = m_browser_window.strong_ref()) {
        auto& tab = browser_window->active_tab();
        if (tab.on_update_cookie) {
            tab.on_update_cookie(tab.url(), cookie);
        }
    }
}

}
