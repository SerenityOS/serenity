/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Queue.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Widget.h>
#include <LibURL/URL.h>
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
    : public GUI::Frame
    , public ViewImplementation {
    C_OBJECT(OutOfProcessWebView);

    using Super = GUI::Frame;

public:
    virtual ~OutOfProcessWebView() override;

    OrderedHashMap<String, String> get_local_storage_entries();
    OrderedHashMap<String, String> get_session_storage_entries();

    void set_content_filters(Vector<String>);
    void set_autoplay_allowed_on_all_websites();
    void set_autoplay_allowlist(Vector<String>);
    void set_proxy_mappings(Vector<ByteString> proxies, HashMap<ByteString, size_t> mappings);
    void connect_to_webdriver(ByteString const& webdriver_ipc_path);

    void set_window_position(Gfx::IntPoint);
    void set_window_size(Gfx::IntSize);

    void set_system_visibility_state(bool visible);

    // This is a hint that tells OOPWV that the content will scale to the viewport size.
    // In practice, this means that OOPWV may render scaled stale versions of the content while resizing.
    void set_content_scales_to_viewport(bool);

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
    virtual void drag_enter_event(GUI::DragEvent&) override;
    virtual void drag_move_event(GUI::DragEvent&) override;
    virtual void drag_leave_event(GUI::Event&) override;
    virtual void drop_event(GUI::DropEvent&) override;

    // ^WebView::ViewImplementation
    virtual void initialize_client(CreateNewClient) override;
    virtual void update_zoom() override;

    virtual Web::DevicePixelSize viewport_size() const override;
    virtual Gfx::IntPoint to_content_position(Gfx::IntPoint widget_position) const override;
    virtual Gfx::IntPoint to_widget_position(Gfx::IntPoint content_position) const override;

    void enqueue_native_event(Web::MouseEvent::Type, GUI::MouseEvent const& event);

    void enqueue_native_event(Web::DragEvent::Type, GUI::DropEvent const& event);
    void finish_handling_drag_event(Web::DragEvent const&);

    void enqueue_native_event(Web::KeyEvent::Type, GUI::KeyEvent const& event);
    void finish_handling_key_event(Web::KeyEvent const&);

    bool m_content_scales_to_viewport { false };
};

}
