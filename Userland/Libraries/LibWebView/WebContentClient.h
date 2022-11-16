/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ConnectionToServer.h>
#include <WebContent/WebContentClientEndpoint.h>
#include <WebContent/WebContentServerEndpoint.h>

namespace WebView {

class ViewImplementation;

class WebContentClient final
    : public IPC::ConnectionToServer<WebContentClientEndpoint, WebContentServerEndpoint>
    , public WebContentClientEndpoint {
    IPC_CLIENT_CONNECTION(WebContentClient, "/tmp/session/%sid/portal/webcontent"sv);

public:
    WebContentClient(NonnullOwnPtr<Core::Stream::LocalSocket>, ViewImplementation&);

    Function<void()> on_web_content_process_crash;

private:
    virtual void die() override;

    virtual void did_paint(Gfx::IntRect const&, i32) override;
    virtual void did_finish_loading(AK::URL const&) override;
    virtual void did_request_navigate_back() override;
    virtual void did_request_navigate_forward() override;
    virtual void did_request_refresh() override;
    virtual void did_invalidate_content_rect(Gfx::IntRect const&) override;
    virtual void did_change_selection() override;
    virtual void did_request_cursor_change(i32) override;
    virtual void did_layout(Gfx::IntSize const&) override;
    virtual void did_change_title(String const&) override;
    virtual void did_request_scroll(i32, i32) override;
    virtual void did_request_scroll_to(Gfx::IntPoint const&) override;
    virtual void did_request_scroll_into_view(Gfx::IntRect const&) override;
    virtual void did_enter_tooltip_area(Gfx::IntPoint const&, String const&) override;
    virtual void did_leave_tooltip_area() override;
    virtual void did_hover_link(AK::URL const&) override;
    virtual void did_unhover_link() override;
    virtual void did_click_link(AK::URL const&, String const&, unsigned) override;
    virtual void did_middle_click_link(AK::URL const&, String const&, unsigned) override;
    virtual void did_start_loading(AK::URL const&) override;
    virtual void did_request_context_menu(Gfx::IntPoint const&) override;
    virtual void did_request_link_context_menu(Gfx::IntPoint const&, AK::URL const&, String const&, unsigned) override;
    virtual void did_request_image_context_menu(Gfx::IntPoint const&, AK::URL const&, String const&, unsigned, Gfx::ShareableBitmap const&) override;
    virtual void did_get_source(AK::URL const&, String const&) override;
    virtual void did_get_dom_tree(String const&) override;
    virtual void did_get_dom_node_properties(i32 node_id, String const& specified_style, String const& computed_style, String const& custom_properties, String const& node_box_sizing) override;
    virtual void did_output_js_console_message(i32 message_index) override;
    virtual void did_get_js_console_messages(i32 start_index, Vector<String> const& message_types, Vector<String> const& messages) override;
    virtual void did_change_favicon(Gfx::ShareableBitmap const&) override;
    virtual void did_request_alert(String const&) override;
    virtual void did_request_confirm(String const&) override;
    virtual void did_request_prompt(String const&, String const&) override;
    virtual void did_request_accept_dialog() override;
    virtual void did_request_dismiss_dialog() override;
    virtual Messages::WebContentClient::DidRequestAllCookiesResponse did_request_all_cookies(AK::URL const&) override;
    virtual Messages::WebContentClient::DidRequestNamedCookieResponse did_request_named_cookie(AK::URL const&, String const&) override;
    virtual Messages::WebContentClient::DidRequestCookieResponse did_request_cookie(AK::URL const&, u8) override;
    virtual void did_set_cookie(AK::URL const&, Web::Cookie::ParsedCookie const&, u8) override;
    virtual void did_update_cookie(AK::URL const&, Web::Cookie::Cookie const&) override;
    virtual void did_update_resource_count(i32 count_waiting) override;
    virtual void did_request_restore_window() override;
    virtual Messages::WebContentClient::DidRequestRepositionWindowResponse did_request_reposition_window(Gfx::IntPoint const&) override;
    virtual Messages::WebContentClient::DidRequestResizeWindowResponse did_request_resize_window(Gfx::IntSize const&) override;
    virtual Messages::WebContentClient::DidRequestMaximizeWindowResponse did_request_maximize_window() override;
    virtual Messages::WebContentClient::DidRequestMinimizeWindowResponse did_request_minimize_window() override;
    virtual Messages::WebContentClient::DidRequestFullscreenWindowResponse did_request_fullscreen_window() override;
    virtual void did_request_file(String const& path, i32) override;

    ViewImplementation& m_view;
};

}
