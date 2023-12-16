/*
 * Copyright (c) 2023, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "WebView.h"
#include <Ladybird/Types.h>
#include <LibWebView/ViewImplementation.h>

class LadybirdViewImpl final
    : public WebView::ViewImplementation {
public:
    static ErrorOr<NonnullOwnPtr<LadybirdViewImpl>> create(LadybirdWebView* widget);
    virtual ~LadybirdViewImpl() override;

    void set_viewport_rect(int x, int y, int width, int height);
    void scale_factor_changed();

    void mouse_down(int x, int y, unsigned button, unsigned buttons, unsigned modifiers);
    void mouse_move(int x, int y, unsigned buttons, unsigned modifiers);
    void mouse_up(int x, int y, unsigned button, unsigned buttons, unsigned modifiers);

    void key_down(KeyCode, unsigned modifiers, u32 code_point);
    void key_up(KeyCode, unsigned modifiers, u32 code_point);

    void connect_to_webdriver(char const* path);

private:
    LadybirdViewImpl(LadybirdWebView* widget);

    virtual void update_zoom() override;
    virtual Web::DevicePixelRect viewport_rect() const override;
    virtual Gfx::IntPoint to_content_position(Gfx::IntPoint widget_position) const override;
    virtual Gfx::IntPoint to_widget_position(Gfx::IntPoint content_position) const override;

    virtual void create_client() override;

    void update_cursor(Gfx::StandardCursor);
    void update_theme();
    WebView::CookieJar& cookie_jar();

    Web::DevicePixelRect m_viewport_rect;
    LadybirdWebView* m_widget { nullptr };
    gulong m_update_style_id { 0 };
};
