/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "History.h"
#include <AK/Optional.h>
#include <AK/URL.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Widget.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibHTTP/Job.h>
#include <LibWeb/Forward.h>
#include <LibWebView/ViewImplementation.h>

namespace WebView {
class OutOfProcessWebView;
}

namespace Browser {

class BrowserWindow;
class InspectorWidget;
class ConsoleWidget;
class HistoryWidget;
class StorageWidget;

class Tab final : public GUI::Widget {
    C_OBJECT(Tab);

    // FIXME: This should go away eventually.
    friend class BrowserWindow;

public:
    virtual ~Tab() override;

    URL url() const;

    enum class LoadType {
        Normal,
        HistoryNavigation,
    };

    void load(const URL&, LoadType = LoadType::Normal);
    void reload();
    void go_back(int steps = 1);
    void go_forward(int steps = 1);

    void did_become_active();
    void context_menu_requested(Gfx::IntPoint screen_position);
    void content_filters_changed();
    void autoplay_allowlist_changed();
    void proxy_mappings_changed();

    void action_entered(GUI::Action&);
    void action_left(GUI::Action&);

    void window_position_changed(Gfx::IntPoint);
    void window_size_changed(Gfx::IntSize);

    Function<void(DeprecatedString const&)> on_title_change;
    Function<void(const URL&)> on_tab_open_request;
    Function<void(Tab&)> on_activate_tab_request;
    Function<void(Tab&)> on_tab_close_request;
    Function<void(Tab&)> on_tab_close_other_request;
    Function<void(const URL&)> on_window_open_request;
    Function<void(Gfx::Bitmap const&)> on_favicon_change;
    Function<Vector<Web::Cookie::Cookie>(AK::URL const& url)> on_get_all_cookies;
    Function<Optional<Web::Cookie::Cookie>(AK::URL const& url, DeprecatedString const& name)> on_get_named_cookie;
    Function<DeprecatedString(const URL&, Web::Cookie::Source source)> on_get_cookie;
    Function<void(const URL&, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source)> on_set_cookie;
    Function<void()> on_dump_cookies;
    Function<void(Web::Cookie::Cookie)> on_update_cookie;
    Function<Vector<Web::Cookie::Cookie>()> on_get_cookies_entries;
    Function<OrderedHashMap<DeprecatedString, DeprecatedString>()> on_get_local_storage_entries;
    Function<OrderedHashMap<DeprecatedString, DeprecatedString>()> on_get_session_storage_entries;

    void enable_webdriver_mode();

    enum class InspectorTarget {
        Document,
        HoveredElement
    };
    void show_inspector_window(InspectorTarget);

    void show_console_window();
    void show_storage_inspector();
    void show_history_inspector();

    void update_reset_zoom_button();

    DeprecatedString const& title() const { return m_title; }
    Gfx::Bitmap const* icon() const { return m_icon; }

    WebView::OutOfProcessWebView& view() { return *m_web_content_view; }

private:
    Tab(BrowserWindow&, WebView::UseJavaScriptBytecode);

    virtual void show_event(GUI::ShowEvent&) override;
    virtual void hide_event(GUI::HideEvent&) override;

    BrowserWindow const& window() const;
    BrowserWindow& window();

    void update_actions();
    ErrorOr<void> bookmark_current_url();
    void update_bookmark_button(StringView url);
    void start_download(const URL& url);
    void view_source(const URL& url, DeprecatedString const& source);
    void update_status(Optional<String> text_override = {}, i32 count_waiting = 0);
    void close_sub_widgets();

    enum class MayAppendTLD {
        No,
        Yes
    };

    Optional<URL> url_from_location_bar(MayAppendTLD = MayAppendTLD::No);

    History m_history;

    RefPtr<WebView::OutOfProcessWebView> m_web_content_view;

    RefPtr<GUI::UrlBox> m_location_box;
    RefPtr<GUI::Button> m_reset_zoom_button;
    RefPtr<GUI::Button> m_bookmark_button;
    RefPtr<InspectorWidget> m_dom_inspector_widget;
    RefPtr<ConsoleWidget> m_console_widget;
    RefPtr<StorageWidget> m_storage_widget;
    RefPtr<HistoryWidget> m_history_widget;
    RefPtr<GUI::Statusbar> m_statusbar;
    RefPtr<GUI::ToolbarContainer> m_toolbar_container;

    RefPtr<GUI::Menu> m_link_context_menu;
    RefPtr<GUI::Action> m_link_context_menu_default_action;
    URL m_link_context_menu_url;

    RefPtr<GUI::Menu> m_image_context_menu;
    Gfx::ShareableBitmap m_image_context_menu_bitmap;
    URL m_image_context_menu_url;

    RefPtr<GUI::Menu> m_audio_context_menu;
    RefPtr<GUI::Menu> m_video_context_menu;
    RefPtr<GUI::Action> m_media_context_menu_play_pause_action;
    RefPtr<GUI::Action> m_media_context_menu_mute_unmute_action;
    RefPtr<GUI::Action> m_media_context_menu_controls_action;
    RefPtr<GUI::Action> m_media_context_menu_loop_action;
    URL m_media_context_menu_url;

    RefPtr<GUI::Menu> m_tab_context_menu;
    RefPtr<GUI::Menu> m_page_context_menu;
    RefPtr<GUI::Menu> m_go_back_context_menu;
    RefPtr<GUI::Menu> m_go_forward_context_menu;
    DeprecatedString m_title;
    RefPtr<const Gfx::Bitmap> m_icon;

    Optional<URL> m_navigating_url;

    bool m_loaded { false };
    bool m_is_history_navigation { false };
};

URL url_from_user_input(DeprecatedString const& input);

}
