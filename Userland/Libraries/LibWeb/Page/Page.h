/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/URL.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <Kernel/API/KeyCode.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibGfx/StandardCursor.h>
#include <LibIPC/Forward.h>
#include <LibJS/Heap/Handle.h>
#include <LibWeb/CSS/PreferredColorScheme.h>
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/ActivateTab.h>
#include <LibWeb/Loader/FileRequest.h>
#include <LibWeb/PixelUnits.h>

namespace Web {

class PageClient;

class Page : public Weakable<Page> {
    AK_MAKE_NONCOPYABLE(Page);
    AK_MAKE_NONMOVABLE(Page);

public:
    explicit Page(PageClient&);
    ~Page();

    PageClient& client() { return m_client; }
    PageClient const& client() const { return m_client; }

    // FIXME: This is a hack.
    bool top_level_browsing_context_is_initialized() const;

    HTML::BrowsingContext& top_level_browsing_context();
    HTML::BrowsingContext const& top_level_browsing_context() const;

    HTML::BrowsingContext& focused_context();
    HTML::BrowsingContext const& focused_context() const { return const_cast<Page*>(this)->focused_context(); }

    void set_focused_browsing_context(Badge<EventHandler>, HTML::BrowsingContext&);

    void load(const AK::URL&);
    void load(LoadRequest&);

    void load_html(StringView, const AK::URL&);

    bool has_ongoing_navigation() const;

    CSSPixelPoint device_to_css_point(DevicePixelPoint) const;
    DevicePixelPoint css_to_device_point(CSSPixelPoint) const;
    CSSPixelRect device_to_css_rect(DevicePixelRect) const;
    DevicePixelRect enclosing_device_rect(CSSPixelRect) const;
    DevicePixelRect rounded_device_rect(CSSPixelRect) const;

    bool handle_mouseup(DevicePixelPoint, unsigned button, unsigned buttons, unsigned modifiers);
    bool handle_mousedown(DevicePixelPoint, unsigned button, unsigned buttons, unsigned modifiers);
    bool handle_mousemove(DevicePixelPoint, unsigned buttons, unsigned modifiers);
    bool handle_mousewheel(DevicePixelPoint, unsigned button, unsigned buttons, unsigned modifiers, int wheel_delta_x, int wheel_delta_y);
    bool handle_doubleclick(DevicePixelPoint, unsigned button, unsigned buttons, unsigned modifiers);

    bool handle_keydown(KeyCode, unsigned modifiers, u32 code_point);
    bool handle_keyup(KeyCode, unsigned modifiers, u32 code_point);

    Gfx::Palette palette() const;
    CSSPixelRect web_exposed_screen_area() const;
    CSS::PreferredColorScheme preferred_color_scheme() const;

    bool is_same_origin_policy_enabled() const { return m_same_origin_policy_enabled; }
    void set_same_origin_policy_enabled(bool b) { m_same_origin_policy_enabled = b; }

    bool is_scripting_enabled() const { return m_is_scripting_enabled; }
    void set_is_scripting_enabled(bool b) { m_is_scripting_enabled = b; }

    bool should_block_pop_ups() const { return m_should_block_pop_ups; }
    void set_should_block_pop_ups(bool b) { m_should_block_pop_ups = b; }

    bool is_webdriver_active() const { return m_is_webdriver_active; }
    void set_is_webdriver_active(bool b) { m_is_webdriver_active = b; }

    DevicePixelPoint window_position() const { return m_window_position; }
    void set_window_position(DevicePixelPoint position) { m_window_position = position; }

    DevicePixelSize window_size() const { return m_window_size; }
    void set_window_size(DevicePixelSize size) { m_window_size = size; }

    void did_request_alert(String const& message);
    void alert_closed();

    bool did_request_confirm(String const& message);
    void confirm_closed(bool accepted);

    Optional<String> did_request_prompt(String const& message, String const& default_);
    void prompt_closed(Optional<String> response);

    enum class PendingDialog {
        None,
        Alert,
        Confirm,
        Prompt,
    };
    bool has_pending_dialog() const { return m_pending_dialog != PendingDialog::None; }
    PendingDialog pending_dialog() const { return m_pending_dialog; }
    Optional<String> const& pending_dialog_text() const { return m_pending_dialog_text; }
    void dismiss_dialog();
    void accept_dialog();

    struct MediaContextMenu {
        AK::URL media_url;
        bool is_video { false };
        bool is_playing { false };
        bool is_muted { false };
        bool has_user_agent_controls { false };
        bool is_looping { false };
    };
    void did_request_media_context_menu(i32 media_id, CSSPixelPoint, DeprecatedString const& target, unsigned modifiers, MediaContextMenu);
    WebIDL::ExceptionOr<void> toggle_media_play_state();
    void toggle_media_mute_state();
    WebIDL::ExceptionOr<void> toggle_media_loop_state();
    WebIDL::ExceptionOr<void> toggle_media_controls_state();

    Optional<String> const& user_style() const { return m_user_style_sheet_source; }
    void set_user_style(String source);

    bool pdf_viewer_supported() const { return m_pdf_viewer_supported; }

private:
    JS::GCPtr<HTML::HTMLMediaElement> media_context_menu_element();

    PageClient& m_client;

    JS::Handle<HTML::BrowsingContext> m_top_level_browsing_context;
    WeakPtr<HTML::BrowsingContext> m_focused_context;

    // FIXME: Enable this by default once CORS preflight checks are supported.
    bool m_same_origin_policy_enabled { false };

    bool m_is_scripting_enabled { true };

    bool m_should_block_pop_ups { true };

    // https://w3c.github.io/webdriver/#dfn-webdriver-active-flag
    // The webdriver-active flag is set to true when the user agent is under remote control. It is initially false.
    bool m_is_webdriver_active { false };

    DevicePixelPoint m_window_position {};
    DevicePixelSize m_window_size {};

    PendingDialog m_pending_dialog { PendingDialog::None };
    Optional<String> m_pending_dialog_text;
    Optional<Empty> m_pending_alert_response;
    Optional<bool> m_pending_confirm_response;
    Optional<Optional<String>> m_pending_prompt_response;

    Optional<int> m_media_context_menu_element_id;

    Optional<String> m_user_style_sheet_source;

    // https://html.spec.whatwg.org/multipage/system-state.html#pdf-viewer-supported
    // Each user agent has a PDF viewer supported boolean, whose value is implementation-defined (and might vary according to user preferences).
    // Spec Note: This value also impacts the navigation processing model.
    // FIXME: Actually support pdf viewing
    bool m_pdf_viewer_supported { false };
};

class PageClient {
public:
    virtual Page& page() = 0;
    virtual Page const& page() const = 0;
    virtual bool is_connection_open() const = 0;
    virtual Gfx::Palette palette() const = 0;
    virtual DevicePixelRect screen_rect() const = 0;
    virtual double device_pixels_per_css_pixel() const = 0;
    virtual CSS::PreferredColorScheme preferred_color_scheme() const = 0;
    virtual void paint(DevicePixelRect const&, Gfx::Bitmap&) = 0;
    virtual void page_did_change_title(DeprecatedString const&) { }
    virtual void page_did_request_navigate_back() { }
    virtual void page_did_request_navigate_forward() { }
    virtual void page_did_request_refresh() { }
    virtual Gfx::IntSize page_did_request_resize_window(Gfx::IntSize) { return {}; }
    virtual Gfx::IntPoint page_did_request_reposition_window(Gfx::IntPoint) { return {}; }
    virtual void page_did_request_restore_window() { }
    virtual Gfx::IntRect page_did_request_maximize_window() { return {}; }
    virtual Gfx::IntRect page_did_request_minimize_window() { return {}; }
    virtual Gfx::IntRect page_did_request_fullscreen_window() { return {}; }
    virtual void page_did_start_loading(const AK::URL&, bool is_redirect) { (void)is_redirect; }
    virtual void page_did_create_new_document(Web::DOM::Document&) { }
    virtual void page_did_finish_loading(const AK::URL&) { }
    virtual void page_did_change_selection() { }
    virtual void page_did_request_cursor_change(Gfx::StandardCursor) { }
    virtual void page_did_request_context_menu(CSSPixelPoint) { }
    virtual void page_did_request_link_context_menu(CSSPixelPoint, AK::URL const&, [[maybe_unused]] DeprecatedString const& target, [[maybe_unused]] unsigned modifiers) { }
    virtual void page_did_request_image_context_menu(CSSPixelPoint, AK::URL const&, [[maybe_unused]] DeprecatedString const& target, [[maybe_unused]] unsigned modifiers, Gfx::Bitmap const*) { }
    virtual void page_did_request_media_context_menu(CSSPixelPoint, [[maybe_unused]] DeprecatedString const& target, [[maybe_unused]] unsigned modifiers, Page::MediaContextMenu) { }
    virtual void page_did_click_link(const AK::URL&, [[maybe_unused]] DeprecatedString const& target, [[maybe_unused]] unsigned modifiers) { }
    virtual void page_did_middle_click_link(const AK::URL&, [[maybe_unused]] DeprecatedString const& target, [[maybe_unused]] unsigned modifiers) { }
    virtual void page_did_enter_tooltip_area(CSSPixelPoint, DeprecatedString const&) { }
    virtual void page_did_leave_tooltip_area() { }
    virtual void page_did_hover_link(const AK::URL&) { }
    virtual void page_did_unhover_link() { }
    virtual void page_did_invalidate(CSSPixelRect const&) { }
    virtual void page_did_change_favicon(Gfx::Bitmap const&) { }
    virtual void page_did_layout() { }
    virtual void page_did_request_scroll(i32, i32) { }
    virtual void page_did_request_scroll_to(CSSPixelPoint) { }
    virtual void page_did_request_scroll_into_view(CSSPixelRect const&) { }
    virtual void page_did_request_alert(String const&) { }
    virtual void page_did_request_confirm(String const&) { }
    virtual void page_did_request_prompt(String const&, String const&) { }
    virtual void page_did_request_set_prompt_text(String const&) { }
    virtual void page_did_request_accept_dialog() { }
    virtual void page_did_request_dismiss_dialog() { }
    virtual Vector<Web::Cookie::Cookie> page_did_request_all_cookies(AK::URL const&) { return {}; }
    virtual Optional<Web::Cookie::Cookie> page_did_request_named_cookie(AK::URL const&, DeprecatedString const&) { return {}; }
    virtual DeprecatedString page_did_request_cookie(const AK::URL&, Cookie::Source) { return {}; }
    virtual void page_did_set_cookie(const AK::URL&, Cookie::ParsedCookie const&, Cookie::Source) { }
    virtual void page_did_update_cookie(Web::Cookie::Cookie) { }
    virtual void page_did_update_resource_count(i32) { }
    virtual String page_did_request_new_tab(HTML::ActivateTab) { return {}; }
    virtual void page_did_request_activate_tab() { }
    virtual void page_did_close_browsing_context(HTML::BrowsingContext const&) { }

    virtual void request_file(FileRequest) = 0;

    // https://html.spec.whatwg.org/multipage/input.html#show-the-picker,-if-applicable
    virtual void page_did_request_file_picker(WeakPtr<DOM::EventTarget>, [[maybe_unused]] bool multiple) {};

    virtual void page_did_finish_text_test() {};

protected:
    virtual ~PageClient() = default;
};

}

namespace IPC {

template<>
ErrorOr<void> encode(Encoder&, Web::Page::MediaContextMenu const&);

template<>
ErrorOr<Web::Page::MediaContextMenu> decode(Decoder&);

}
