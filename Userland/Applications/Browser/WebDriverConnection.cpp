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
#include <LibWeb/Cookie/ParsedCookie.h>

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
    dbgln("WebDriverConnection: get_named_cookie {}", name);
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
    dbgln("WebDriverConnection: add_cookie {}", cookie.name);
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
    dbgln("WebDriverConnection: update_cookie {}", cookie.name);
    if (auto browser_window = m_browser_window.strong_ref()) {
        auto& tab = browser_window->active_tab();
        if (tab.on_update_cookie) {
            tab.on_update_cookie(tab.url(), cookie);
        }
    }
}

Messages::WebDriverSessionClient::GetDocumentElementResponse WebDriverConnection::get_document_element()
{
    dbgln("WebDriverConnection: get_document_element");
    if (auto browser_window = m_browser_window.strong_ref()) {
        auto& tab = browser_window->active_tab();
        if (tab.on_get_document_element)
            return { tab.on_get_document_element() };
    }
    return { {} };
}

Messages::WebDriverSessionClient::QuerySelectorAllResponse WebDriverConnection::query_selector_all(i32 start_node_id, String const& selector)
{
    dbgln("WebDriverConnection: query_selector_all");
    if (auto browser_window = m_browser_window.strong_ref()) {
        auto& tab = browser_window->active_tab();
        if (tab.on_query_selector_all)
            return { tab.on_query_selector_all(start_node_id, selector) };
    }
    return { {} };
}

Messages::WebDriverSessionClient::GetElementAttributeResponse WebDriverConnection::get_element_attribute(i32 element_id, String const& name)
{
    dbgln("WebDriverConnection: get_element_attribute");
    if (auto browser_window = m_browser_window.strong_ref()) {
        auto& tab = browser_window->active_tab();
        if (tab.on_get_element_attribute)
            return { tab.on_get_element_attribute(element_id, name) };
    }
    return { {} };
}

Messages::WebDriverSessionClient::GetElementPropertyResponse WebDriverConnection::get_element_property(i32 element_id, String const& name)
{
    dbgln("WebDriverConnection: get_element_property");
    if (auto browser_window = m_browser_window.strong_ref()) {
        auto& tab = browser_window->active_tab();
        if (tab.on_get_element_property)
            return { tab.on_get_element_property(element_id, name) };
    }
    return { {} };
}

}
