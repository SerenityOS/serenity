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
    virtual void refresh() override;
    virtual void back() override;
    virtual void forward() override;
    virtual Messages::WebDriverSessionClient::SerializeSourceResponse serialize_source() override;
    virtual Messages::WebDriverSessionClient::ExecuteScriptResponse execute_script(String const& body, Vector<String> const& json_arguments, Optional<u64> const& timeout, bool async) override;
    virtual Messages::WebDriverSessionClient::GetAllCookiesResponse get_all_cookies() override;
    virtual Messages::WebDriverSessionClient::GetNamedCookieResponse get_named_cookie(String const& name) override;
    virtual void add_cookie(Web::Cookie::ParsedCookie const&) override;
    virtual void update_cookie(Web::Cookie::Cookie const&) override;
    virtual void scroll_element_into_view(i32 element_id) override;
    virtual Messages::WebDriverSessionClient::GetElementTextResponse get_element_text(i32 element_id) override;
    virtual Messages::WebDriverSessionClient::GetElementTagNameResponse get_element_tag_name(i32 element_id) override;
    virtual Messages::WebDriverSessionClient::GetElementRectResponse get_element_rect(i32 element_id) override;
    virtual Messages::WebDriverSessionClient::IsElementEnabledResponse is_element_enabled(i32 element_id) override;
    virtual Messages::WebDriverSessionClient::TakeScreenshotResponse take_screenshot() override;
    virtual Messages::WebDriverSessionClient::TakeElementScreenshotResponse take_element_screenshot(i32 element_id) override;

private:
    WebDriverConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket, NonnullRefPtr<BrowserWindow> browser_window);

    WeakPtr<BrowserWindow> m_browser_window;
};

}
