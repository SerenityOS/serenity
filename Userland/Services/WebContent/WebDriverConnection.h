/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/ConnectionToServer.h>
#include <LibWeb/Forward.h>
#include <LibWeb/WebDriver/ElementLocationStrategies.h>
#include <LibWeb/WebDriver/Response.h>
#include <WebContent/Forward.h>
#include <WebContent/WebDriverClientEndpoint.h>
#include <WebContent/WebDriverServerEndpoint.h>

namespace WebContent {

class WebDriverConnection final
    : public IPC::ConnectionToServer<WebDriverClientEndpoint, WebDriverServerEndpoint> {
    C_OBJECT_ABSTRACT(WebDriverConnection)

public:
    static ErrorOr<NonnullRefPtr<WebDriverConnection>> connect(ConnectionFromClient& web_content_client, PageHost& page_host, String const& webdriver_ipc_path);
    virtual ~WebDriverConnection() = default;

private:
    WebDriverConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket, ConnectionFromClient& web_content_client, PageHost& page_host);

    virtual void die() override { }

    virtual void close_session() override;
    virtual void set_is_webdriver_active(bool) override;
    virtual Messages::WebDriverClient::NavigateToResponse navigate_to(JsonValue const& payload) override;
    virtual Messages::WebDriverClient::GetCurrentUrlResponse get_current_url() override;
    virtual Messages::WebDriverClient::GetWindowRectResponse get_window_rect() override;
    virtual Messages::WebDriverClient::SetWindowRectResponse set_window_rect(JsonValue const& payload) override;
    virtual Messages::WebDriverClient::MaximizeWindowResponse maximize_window() override;
    virtual Messages::WebDriverClient::MinimizeWindowResponse minimize_window() override;
    virtual Messages::WebDriverClient::FindElementResponse find_element(JsonValue const& payload) override;
    virtual Messages::WebDriverClient::FindElementsResponse find_elements(JsonValue const& payload) override;
    virtual Messages::WebDriverClient::FindElementFromElementResponse find_element_from_element(JsonValue const& payload, String const& element_id) override;
    virtual Messages::WebDriverClient::FindElementsFromElementResponse find_elements_from_element(JsonValue const& payload, String const& element_id) override;
    virtual Messages::WebDriverClient::IsElementSelectedResponse is_element_selected(String const& element_id) override;
    virtual Messages::WebDriverClient::GetElementAttributeResponse get_element_attribute(String const& element_id, String const& name) override;

    ErrorOr<void, Web::WebDriver::Error> ensure_open_top_level_browsing_context();
    void restore_the_window();
    Gfx::IntRect maximize_the_window();
    Gfx::IntRect iconify_the_window();
    ErrorOr<JsonArray, Web::WebDriver::Error> find(Web::DOM::ParentNode& start_node, Web::WebDriver::LocationStrategy using_, StringView value);

    ConnectionFromClient& m_web_content_client;
    PageHost& m_page_host;
};

}
