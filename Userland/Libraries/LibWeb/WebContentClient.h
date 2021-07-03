/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ServerConnection.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <WebContent/WebContentClientEndpoint.h>
#include <WebContent/WebContentServerEndpoint.h>

namespace Web {

class OutOfProcessWebView;

class WebContentClient final
    : public IPC::ServerConnection<WebContentClientEndpoint, WebContentServerEndpoint>
    , public WebContentClientEndpoint {
    C_OBJECT(WebContentClient);

public:
    Function<void()> on_web_content_process_crash;

private:
    WebContentClient(OutOfProcessWebView&);

    virtual void die() override;

    virtual void did_paint(Gfx::IntRect const&, i32) override;
    virtual void did_finish_loading(URL const&) override;
    virtual void did_invalidate_content_rect(Gfx::IntRect const&) override;
    virtual void did_change_selection() override;
    virtual void did_request_cursor_change(i32) override;
    virtual void did_layout(Gfx::IntSize const&) override;
    virtual void did_change_title(String const&) override;
    virtual void did_request_scroll(int) override;
    virtual void did_request_scroll_into_view(Gfx::IntRect const&) override;
    virtual void did_enter_tooltip_area(Gfx::IntPoint const&, String const&) override;
    virtual void did_leave_tooltip_area() override;
    virtual void did_hover_link(URL const&) override;
    virtual void did_unhover_link() override;
    virtual void did_click_link(URL const&, String const&, unsigned) override;
    virtual void did_middle_click_link(URL const&, String const&, unsigned) override;
    virtual void did_start_loading(URL const&) override;
    virtual void did_request_context_menu(Gfx::IntPoint const&) override;
    virtual void did_request_link_context_menu(Gfx::IntPoint const&, URL const&, String const&, unsigned) override;
    virtual void did_request_image_context_menu(Gfx::IntPoint const&, URL const&, String const&, unsigned, Gfx::ShareableBitmap const&) override;
    virtual void did_get_source(URL const&, String const&) override;
    virtual void did_js_console_output(String const&, String const&) override;
    virtual void did_change_favicon(Gfx::ShareableBitmap const&) override;
    virtual void did_request_alert(String const&) override;
    virtual Messages::WebContentClient::DidRequestConfirmResponse did_request_confirm(String const&) override;
    virtual Messages::WebContentClient::DidRequestPromptResponse did_request_prompt(String const&, String const&) override;
    virtual Messages::WebContentClient::DidRequestCookieResponse did_request_cookie(URL const&, u8) override;
    virtual void did_set_cookie(URL const&, Web::Cookie::ParsedCookie const&, u8) override;

    OutOfProcessWebView& m_view;
};

}
