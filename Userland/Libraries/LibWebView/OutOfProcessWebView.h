/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Queue.h>
#include <AK/URL.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGUI/Widget.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/HTML/ActivateTab.h>
#include <LibWeb/Page/Page.h>
#include <LibWebView/ViewImplementation.h>

namespace Messages::WebContentServer {
class WebdriverExecuteScriptResponse;
}

namespace WebView {

class WebContentClient;

class OutOfProcessWebView final
    : public GUI::AbstractScrollableWidget
    , public ViewImplementation {
    C_OBJECT(OutOfProcessWebView);

    using Super = GUI::AbstractScrollableWidget;

public:
    virtual ~OutOfProcessWebView() override;

    DeprecatedString dump_layout_tree();

    OrderedHashMap<DeprecatedString, DeprecatedString> get_local_storage_entries();
    OrderedHashMap<DeprecatedString, DeprecatedString> get_session_storage_entries();

    void set_content_filters(Vector<String>);
    void set_autoplay_allowed_on_all_websites();
    void set_autoplay_allowlist(Vector<String>);
    void set_proxy_mappings(Vector<DeprecatedString> proxies, HashMap<DeprecatedString, size_t> mappings);
    void connect_to_webdriver(DeprecatedString const& webdriver_ipc_path);

    void set_window_position(Gfx::IntPoint);
    void set_window_size(Gfx::IntSize);

    void set_system_visibility_state(bool visible);

    // This is a hint that tells OOPWV that the content will scale to the viewport size.
    // In practice, this means that OOPWV may render scaled stale versions of the content while resizing.
    void set_content_scales_to_viewport(bool);

    void set_user_style_sheet(String source);

private:
    OutOfProcessWebView();

    // ^Widget
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void keyup_event(GUI::KeyEvent&) override;
    virtual void theme_change_event(GUI::ThemeChangeEvent&) override;
    virtual void screen_rects_change_event(GUI::ScreenRectsChangeEvent&) override;
    virtual void focusin_event(GUI::FocusEvent&) override;
    virtual void focusout_event(GUI::FocusEvent&) override;
    virtual void show_event(GUI::ShowEvent&) override;
    virtual void hide_event(GUI::HideEvent&) override;

    // ^AbstractScrollableWidget
    virtual void did_scroll() override;

    // ^WebView::ViewImplementation
    virtual void create_client(EnableCallgrindProfiling = EnableCallgrindProfiling::No) override;
    virtual void update_zoom() override;

    virtual Gfx::IntRect viewport_rect() const override;
    virtual Gfx::IntPoint to_content_position(Gfx::IntPoint widget_position) const override;
    virtual Gfx::IntPoint to_widget_position(Gfx::IntPoint content_position) const override;

    using InputEvent = Variant<GUI::KeyEvent, GUI::MouseEvent>;
    void enqueue_input_event(InputEvent const&);
    void process_next_input_event();
    void did_finish_handling_input_event(bool event_was_accepted);

    bool m_is_awaiting_response_for_input_event { false };
    Queue<InputEvent> m_pending_input_events;

    bool m_content_scales_to_viewport { false };
};

}
