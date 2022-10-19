/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
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
        dbgln("Trying to connect to {}", path);
        auto result = TRY(Core::Stream::LocalSocket::connect(path));
        dbgln("Connected to WebDriver");
        return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) WebDriverConnection(move(result), browser_window)));
    }

    virtual ~WebDriverConnection() = default;

    virtual void die() override { }

    virtual void quit() override;
    virtual Messages::WebDriverSessionClient::GetUrlResponse get_url() override;
    virtual void set_url(AK::URL const& url) override;
    virtual Messages::WebDriverSessionClient::GetTitleResponse get_title() override;
    virtual void refresh() override;
    virtual void back() override;
    virtual void forward() override;
    virtual Messages::WebDriverSessionClient::GetAllCookiesResponse get_all_cookies() override;
    virtual Messages::WebDriverSessionClient::GetNamedCookieResponse get_named_cookie(String const& name) override;
    virtual void add_cookie(Web::Cookie::ParsedCookie const&) override;
    virtual void update_cookie(Web::Cookie::Cookie const&) override;
    virtual Messages::WebDriverSessionClient::GetDocumentElementResponse get_document_element() override;
    virtual Messages::WebDriverSessionClient::QuerySelectorAllResponse query_selector_all(i32 start_node_id, String const& selector) override;
    virtual Messages::WebDriverSessionClient::GetElementAttributeResponse get_element_attribute(i32 element_id, String const& name) override;
    virtual Messages::WebDriverSessionClient::GetElementPropertyResponse get_element_property(i32 element_id, String const& name) override;

private:
    WebDriverConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket, NonnullRefPtr<BrowserWindow> browser_window);

    WeakPtr<BrowserWindow> m_browser_window;
};

}
