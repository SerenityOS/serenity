/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebDriverConnection.h"
#include "BrowserWindow.h"
#include <AK/Vector.h>
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWebView/WebContentClient.h>

namespace Browser {

WebDriverConnection::WebDriverConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket, NonnullRefPtr<BrowserWindow> browser_window)
    : IPC::ConnectionToServer<WebDriverSessionClientEndpoint, WebDriverSessionServerEndpoint>(*this, move(socket))
    , m_browser_window(move(browser_window))
{
}

void WebDriverConnection::quit()
{
    dbgln_if(WEBDRIVER_DEBUG, "WebDriverConnection: quit");
    if (auto browser_window = m_browser_window.strong_ref())
        browser_window->close();
}

Messages::WebDriverSessionClient::GetTitleResponse WebDriverConnection::get_title()
{
    dbgln_if(WEBDRIVER_DEBUG, "WebDriverConnection: get_title");
    if (auto browser_window = m_browser_window.strong_ref())
        return { browser_window->active_tab().title() };
    return { "" };
}

void WebDriverConnection::refresh()
{
    dbgln_if(WEBDRIVER_DEBUG, "WebDriverConnection: refresh");
    if (auto browser_window = m_browser_window.strong_ref())
        browser_window->active_tab().reload();
}

void WebDriverConnection::back()
{
    dbgln_if(WEBDRIVER_DEBUG, "WebDriverConnection: back");
    if (auto browser_window = m_browser_window.strong_ref())
        browser_window->active_tab().go_back();
}

void WebDriverConnection::forward()
{
    dbgln_if(WEBDRIVER_DEBUG, "WebDriverConnection: forward");
    if (auto browser_window = m_browser_window.strong_ref())
        browser_window->active_tab().go_forward();
}

Messages::WebDriverSessionClient::GetAllCookiesResponse WebDriverConnection::get_all_cookies()
{
    dbgln_if(WEBDRIVER_DEBUG, "WebDriverConnection: get_cookies");
    if (auto browser_window = m_browser_window.strong_ref()) {
        if (browser_window->active_tab().on_get_cookies_entries) {
            return { browser_window->active_tab().on_get_cookies_entries() };
        }
    }
    return { {} };
}

Messages::WebDriverSessionClient::GetNamedCookieResponse WebDriverConnection::get_named_cookie(String const& name)
{
    dbgln_if(WEBDRIVER_DEBUG, "WebDriverConnection: get_named_cookie {}", name);
    if (auto browser_window = m_browser_window.strong_ref()) {
        if (browser_window->active_tab().on_get_cookies_entries) {
            for (auto cookie : browser_window->active_tab().on_get_cookies_entries()) {
                if (cookie.name == name)
                    return { cookie };
            }
            return Optional<Web::Cookie::Cookie> {};
        }
    }
    return { {} };
}

void WebDriverConnection::add_cookie(Web::Cookie::ParsedCookie const& cookie)
{
    dbgln_if(WEBDRIVER_DEBUG, "WebDriverConnection: add_cookie {}", cookie.name);
    if (auto browser_window = m_browser_window.strong_ref()) {
        auto& tab = browser_window->active_tab();
        if (tab.on_set_cookie) {
            // FIXME: The spec doesn't say anything about the source
            //  but can we assume a cookie created through a HTTP-request to the WebDriver
            //  to be (source) from an HTTP-API?
            tab.on_set_cookie(tab.url(), cookie, Web::Cookie::Source::Http);
        }
    }
}

void WebDriverConnection::update_cookie(Web::Cookie::Cookie const& cookie)
{
    dbgln_if(WEBDRIVER_DEBUG, "WebDriverConnection: update_cookie {}", cookie.name);
    if (auto browser_window = m_browser_window.strong_ref()) {
        auto& tab = browser_window->active_tab();
        if (tab.on_update_cookie) {
            tab.on_update_cookie(tab.url(), cookie);
        }
    }
}

}
