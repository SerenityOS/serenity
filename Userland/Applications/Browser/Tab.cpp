/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Maciej Zygmanowski <sppmacd@pm.me>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tab.h"
#include "BookmarksBarWidget.h"
#include "Browser.h"
#include "BrowserWindow.h"
#include "ConsoleWidget.h"
#include "DownloadWidget.h"
#include "History/HistoryWidget.h"
#include "InspectorWidget.h"
#include "StorageWidget.h"
#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <Applications/Browser/TabGML.h>
#include <Applications/BrowserSettings/Defaults.h>
#include <LibConfig/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/Window.h>
#include <LibPublicSuffix/URL.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/SyntaxHighlighter/SyntaxHighlighter.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWebView/OutOfProcessWebView.h>

namespace Browser {

Tab::~Tab()
{
    close_sub_widgets();
}

URL url_from_user_input(DeprecatedString const& input)
{
    URL url_with_http_schema = URL(DeprecatedString::formatted("https://{}", input));
    if (url_with_http_schema.is_valid() && url_with_http_schema.port().has_value())
        return url_with_http_schema;

    URL url = URL(input);
    if (url.is_valid())
        return url;

    return url_with_http_schema;
}

void Tab::start_download(const URL& url)
{
    auto window = GUI::Window::construct(&this->window());
    window->resize(300, 170);
    window->set_title(DeprecatedString::formatted("0% of {}", url.basename()));
    window->set_resizable(false);
    (void)window->set_main_widget<DownloadWidget>(url).release_value_but_fixme_should_propagate_errors();
    window->show();
}

void Tab::view_source(const URL& url, DeprecatedString const& source)
{
    auto window = GUI::Window::construct(&this->window());
    auto editor = window->set_main_widget<GUI::TextEditor>().release_value_but_fixme_should_propagate_errors();
    editor->set_text(source);
    editor->set_mode(GUI::TextEditor::ReadOnly);
    editor->set_syntax_highlighter(make<Web::HTML::SyntaxHighlighter>());
    editor->set_ruler_visible(true);
    window->resize(640, 480);
    window->set_title(url.to_deprecated_string());
    window->set_icon(g_icon_bag.filetype_text);
    window->set_window_mode(GUI::WindowMode::Modeless);
    window->show();
}

void Tab::update_status(Optional<String> text_override, i32 count_waiting)
{
    if (text_override.has_value()) {
        m_statusbar->set_text(*text_override);
        return;
    }

    if (m_loaded) {
        m_statusbar->set_text({});
        return;
    }

    VERIFY(m_navigating_url.has_value());

    if (count_waiting == 0) {
        // ex: "Loading google.com"
        m_statusbar->set_text(String::formatted("Loading {}", m_navigating_url->serialized_host().release_value_but_fixme_should_propagate_errors()).release_value_but_fixme_should_propagate_errors());
    } else {
        // ex: "google.com is waiting on 5 resources"
        m_statusbar->set_text(String::formatted("{} is waiting on {} resource{}", m_navigating_url->serialized_host().release_value_but_fixme_should_propagate_errors(), count_waiting, count_waiting == 1 ? ""sv : "s"sv).release_value_but_fixme_should_propagate_errors());
    }
}

Tab::Tab(BrowserWindow& window)
{
    load_from_gml(tab_gml).release_value_but_fixme_should_propagate_errors();

    m_toolbar_container = *find_descendant_of_type_named<GUI::ToolbarContainer>("toolbar_container");
    auto& toolbar = *find_descendant_of_type_named<GUI::Toolbar>("toolbar");

    auto& webview_container = *find_descendant_of_type_named<GUI::Widget>("webview_container");

    m_web_content_view = webview_container.add<WebView::OutOfProcessWebView>();

    auto preferred_color_scheme = Web::CSS::preferred_color_scheme_from_string(Config::read_string("Browser"sv, "Preferences"sv, "ColorScheme"sv, Browser::default_color_scheme));
    m_web_content_view->set_preferred_color_scheme(preferred_color_scheme);

    content_filters_changed();
    autoplay_allowlist_changed();

    m_web_content_view->set_proxy_mappings(g_proxies, g_proxy_mappings);
    if (!g_webdriver_content_ipc_path.is_empty())
        enable_webdriver_mode();

    auto& go_back_button = toolbar.add_action(window.go_back_action());
    go_back_button.on_context_menu_request = [&](auto&) {
        if (!m_history.can_go_back())
            return;
        int i = 0;
        m_go_back_context_menu = GUI::Menu::construct();
        for (auto& url : m_history.get_back_title_history()) {
            i++;
            m_go_back_context_menu->add_action(GUI::Action::create(url.to_deprecated_string(), g_icon_bag.filetype_html, [this, i](auto&) { go_back(i); }));
        }
        m_go_back_context_menu->popup(go_back_button.screen_relative_rect().bottom_left().moved_up(1));
    };

    auto& go_forward_button = toolbar.add_action(window.go_forward_action());
    go_forward_button.on_context_menu_request = [&](auto&) {
        if (!m_history.can_go_forward())
            return;
        int i = 0;
        m_go_forward_context_menu = GUI::Menu::construct();
        for (auto& url : m_history.get_forward_title_history()) {
            i++;
            m_go_forward_context_menu->add_action(GUI::Action::create(url.to_deprecated_string(), g_icon_bag.filetype_html, [this, i](auto&) { go_forward(i); }));
        }
        m_go_forward_context_menu->popup(go_forward_button.screen_relative_rect().bottom_left().moved_up(1));
    };

    auto& go_home_button = toolbar.add_action(window.go_home_action());
    go_home_button.set_allowed_mouse_buttons_for_pressing(GUI::MouseButton::Primary | GUI::MouseButton::Middle);
    go_home_button.on_middle_mouse_click = [&](auto) {
        on_tab_open_request(Browser::url_from_user_input(g_home_url));
    };

    toolbar.add_action(window.reload_action());

    m_location_box = toolbar.add<GUI::UrlBox>();
    m_location_box->set_placeholder("Search or enter address"sv);

    m_location_box->on_return_pressed = [this] {
        auto url = url_from_location_bar();
        if (url.has_value())
            load(url.release_value());
    };

    m_location_box->on_ctrl_return_pressed = [this] {
        auto url = url_from_location_bar(MayAppendTLD::Yes);
        if (url.has_value())
            load(url.release_value());
    };

    m_location_box->add_custom_context_menu_action(GUI::Action::create("Paste && Go", [this](auto&) {
        auto [data, mime_type, _] = GUI::Clipboard::the().fetch_data_and_type();
        if (!mime_type.starts_with("text/"sv))
            return;
        auto const& paste_text = data;
        if (paste_text.is_empty())
            return;
        m_location_box->set_text(paste_text);
        m_location_box->on_return_pressed();
    }));

    auto bookmark_action = GUI::Action::create(
        "Bookmark current URL", { Mod_Ctrl, Key_D }, [this](auto&) {
            if (auto result = bookmark_current_url(); result.is_error())
                GUI::MessageBox::show_error(this->window().main_widget()->window(), MUST(String::formatted("Failed to bookmark URL: {}", result.error())));
        },
        this);

    m_reset_zoom_button = toolbar.add<GUI::Button>();
    m_reset_zoom_button->set_tooltip_deprecated("Reset zoom level");
    m_reset_zoom_button->on_click = [&](auto) {
        view().reset_zoom();
        update_reset_zoom_button();
        window.update_zoom_menu();
    };
    m_reset_zoom_button->set_button_style(Gfx::ButtonStyle::Coolbar);
    m_reset_zoom_button->set_visible(false);
    m_reset_zoom_button->set_preferred_width(GUI::SpecialDimension::Shrink);

    m_bookmark_button = toolbar.add<GUI::Button>();
    m_bookmark_button->set_action(bookmark_action);
    m_bookmark_button->set_button_style(Gfx::ButtonStyle::Coolbar);
    m_bookmark_button->set_focus_policy(GUI::FocusPolicy::TabFocus);
    m_bookmark_button->set_icon(g_icon_bag.bookmark_contour);
    m_bookmark_button->set_fixed_size(22, 22);

    view().on_load_start = [this](auto& url, bool is_redirect) {
        m_navigating_url = url;
        m_loaded = false;

        // If we are loading due to a redirect, we replace the current history entry
        // with the loaded URL
        if (is_redirect) {
            m_history.replace_current(url, title());
        }

        update_status();

        m_location_box->set_icon(nullptr);
        m_location_box->set_text(url.to_deprecated_string());

        // don't add to history if back or forward is pressed
        if (!m_is_history_navigation)
            m_history.push(url, title());
        m_is_history_navigation = false;

        update_actions();
        update_bookmark_button(url.to_deprecated_string());

        if (m_dom_inspector_widget)
            m_dom_inspector_widget->clear_dom_json();

        if (m_console_widget)
            m_console_widget->reset();
    };

    view().on_load_finish = [this](auto&) {
        m_navigating_url = {};
        m_loaded = true;

        update_status();

        if (m_dom_inspector_widget) {
            m_web_content_view->inspect_dom_tree();
            m_web_content_view->inspect_accessibility_tree();
        }
    };

    view().on_navigate_back = [this]() {
        go_back(1);
    };

    view().on_navigate_forward = [this]() {
        go_forward(1);
    };

    view().on_refresh = [this]() {
        reload();
    };

    view().on_link_click = [this](auto& url, auto& target, unsigned modifiers) {
        if (target == "_blank" || modifiers == Mod_Ctrl) {
            on_tab_open_request(url);
        } else {
            load(url);
        }
    };

    view().on_resource_status_change = [this](auto count_waiting) {
        update_status({}, count_waiting);
    };

    view().on_restore_window = [this]() {
        this->window().show();
        this->window().move_to_front();
        m_web_content_view->set_system_visibility_state(true);
    };

    view().on_reposition_window = [this](Gfx::IntPoint position) {
        this->window().move_to(position);
        return this->window().position();
    };

    view().on_resize_window = [this](Gfx::IntSize size) {
        this->window().resize(size);
        return this->window().size();
    };

    view().on_maximize_window = [this]() {
        this->window().set_maximized(true);
        return this->window().rect();
    };

    view().on_minimize_window = [this]() {
        this->window().set_minimized(true);
        m_web_content_view->set_system_visibility_state(false);
        return this->window().rect();
    };

    view().on_fullscreen_window = [this]() {
        this->window().set_fullscreen(true);
        return this->window().rect();
    };

    m_link_context_menu = GUI::Menu::construct();
    auto link_default_action = GUI::Action::create("&Open", g_icon_bag.go_to, [this](auto&) {
        view().on_link_click(m_link_context_menu_url, "", 0);
    });
    m_link_context_menu->add_action(link_default_action);
    m_link_context_menu_default_action = link_default_action;
    m_link_context_menu->add_action(GUI::Action::create("Open in New &Tab", g_icon_bag.new_tab, [this](auto&) {
        view().on_link_click(m_link_context_menu_url, "_blank", 0);
    }));
    m_link_context_menu->add_separator();
    m_link_context_menu->add_action(GUI::Action::create("&Copy URL", g_icon_bag.copy, [this](auto&) {
        GUI::Clipboard::the().set_plain_text(m_link_context_menu_url.to_deprecated_string());
    }));
    m_link_context_menu->add_separator();
    m_link_context_menu->add_action(GUI::Action::create("&Download", g_icon_bag.download, [this](auto&) {
        start_download(m_link_context_menu_url);
    }));
    m_link_context_menu->add_separator();
    m_link_context_menu->add_action(window.inspect_dom_node_action());

    view().on_link_context_menu_request = [this](auto& url, auto widget_position) {
        m_link_context_menu_url = url;

        auto screen_position = view().screen_relative_rect().location().translated(widget_position);
        m_link_context_menu->popup(screen_position, m_link_context_menu_default_action);
    };

    m_image_context_menu = GUI::Menu::construct();
    m_image_context_menu->add_action(GUI::Action::create("&Open Image", g_icon_bag.filetype_image, [this](auto&) {
        view().on_link_click(m_image_context_menu_url, "", 0);
    }));
    m_image_context_menu->add_action(GUI::Action::create("Open Image in New &Tab", g_icon_bag.new_tab, [this](auto&) {
        view().on_link_click(m_image_context_menu_url, "_blank", 0);
    }));
    m_image_context_menu->add_separator();
    m_image_context_menu->add_action(GUI::Action::create("&Copy Image", g_icon_bag.copy, [this](auto&) {
        if (m_image_context_menu_bitmap.is_valid())
            GUI::Clipboard::the().set_bitmap(*m_image_context_menu_bitmap.bitmap());
    }));
    m_image_context_menu->add_action(GUI::Action::create("Copy Image &URL", g_icon_bag.copy, [this](auto&) {
        GUI::Clipboard::the().set_plain_text(m_image_context_menu_url.to_deprecated_string());
    }));
    m_image_context_menu->add_separator();
    m_image_context_menu->add_action(GUI::Action::create("&Download", g_icon_bag.download, [this](auto&) {
        start_download(m_image_context_menu_url);
    }));
    m_image_context_menu->add_separator();
    m_image_context_menu->add_action(window.inspect_dom_node_action());

    view().on_image_context_menu_request = [this](auto& image_url, auto widget_position, Gfx::ShareableBitmap const& shareable_bitmap) {
        m_image_context_menu_url = image_url;
        m_image_context_menu_bitmap = shareable_bitmap;

        auto screen_position = view().screen_relative_rect().location().translated(widget_position);
        m_image_context_menu->popup(screen_position);
    };

    m_media_context_menu_play_pause_action = GUI::Action::create("&Play", g_icon_bag.play, [this](auto&) {
        view().toggle_media_play_state();
    });
    m_media_context_menu_mute_unmute_action = GUI::Action::create("&Mute", g_icon_bag.mute, [this](auto&) {
        view().toggle_media_mute_state();
    });
    m_media_context_menu_controls_action = GUI::Action::create_checkable("Show &Controls", [this](auto&) {
        view().toggle_media_controls_state();
    });
    m_media_context_menu_loop_action = GUI::Action::create_checkable("&Loop", [this](auto&) {
        view().toggle_media_loop_state();
    });

    m_audio_context_menu = GUI::Menu::construct();
    m_audio_context_menu->add_action(*m_media_context_menu_play_pause_action);
    m_audio_context_menu->add_action(*m_media_context_menu_mute_unmute_action);
    m_audio_context_menu->add_action(*m_media_context_menu_controls_action);
    m_audio_context_menu->add_action(*m_media_context_menu_loop_action);
    m_audio_context_menu->add_separator();
    m_audio_context_menu->add_action(GUI::Action::create("&Open Audio", g_icon_bag.filetype_audio, [this](auto&) {
        view().on_link_click(m_media_context_menu_url, "", 0);
    }));
    m_audio_context_menu->add_action(GUI::Action::create("Open Audio in New &Tab", g_icon_bag.new_tab, [this](auto&) {
        view().on_link_click(m_media_context_menu_url, "_blank", 0);
    }));
    m_audio_context_menu->add_separator();
    m_audio_context_menu->add_action(GUI::Action::create("Copy Audio &URL", g_icon_bag.copy, [this](auto&) {
        GUI::Clipboard::the().set_plain_text(m_media_context_menu_url.to_deprecated_string());
    }));
    m_audio_context_menu->add_separator();
    m_audio_context_menu->add_action(GUI::Action::create("&Download", g_icon_bag.download, [this](auto&) {
        start_download(m_media_context_menu_url);
    }));
    m_audio_context_menu->add_separator();
    m_audio_context_menu->add_action(window.inspect_dom_node_action());

    m_video_context_menu = GUI::Menu::construct();
    m_video_context_menu->add_action(*m_media_context_menu_play_pause_action);
    m_video_context_menu->add_action(*m_media_context_menu_mute_unmute_action);
    m_video_context_menu->add_action(*m_media_context_menu_controls_action);
    m_video_context_menu->add_action(*m_media_context_menu_loop_action);
    m_video_context_menu->add_separator();
    m_video_context_menu->add_action(GUI::Action::create("&Open Video", g_icon_bag.filetype_video, [this](auto&) {
        view().on_link_click(m_media_context_menu_url, "", 0);
    }));
    m_video_context_menu->add_action(GUI::Action::create("Open Video in New &Tab", g_icon_bag.new_tab, [this](auto&) {
        view().on_link_click(m_media_context_menu_url, "_blank", 0);
    }));
    m_video_context_menu->add_separator();
    m_video_context_menu->add_action(GUI::Action::create("Copy Video &URL", g_icon_bag.copy, [this](auto&) {
        GUI::Clipboard::the().set_plain_text(m_media_context_menu_url.to_deprecated_string());
    }));
    m_video_context_menu->add_separator();
    m_video_context_menu->add_action(GUI::Action::create("&Download", g_icon_bag.download, [this](auto&) {
        start_download(m_media_context_menu_url);
    }));
    m_video_context_menu->add_separator();
    m_video_context_menu->add_action(window.inspect_dom_node_action());

    view().on_media_context_menu_request = [this](auto widget_position, Web::Page::MediaContextMenu const& menu) {
        m_media_context_menu_url = menu.media_url;

        if (menu.is_playing) {
            m_media_context_menu_play_pause_action->set_icon(g_icon_bag.pause);
            m_media_context_menu_play_pause_action->set_text("&Pause"sv);
        } else {
            m_media_context_menu_play_pause_action->set_icon(g_icon_bag.play);
            m_media_context_menu_play_pause_action->set_text("&Play"sv);
        }

        if (menu.is_muted) {
            m_media_context_menu_mute_unmute_action->set_icon(g_icon_bag.unmute);
            m_media_context_menu_mute_unmute_action->set_text("Un&mute"sv);
        } else {
            m_media_context_menu_mute_unmute_action->set_icon(g_icon_bag.mute);
            m_media_context_menu_mute_unmute_action->set_text("&Mute"sv);
        }

        m_media_context_menu_controls_action->set_checked(menu.has_user_agent_controls);
        m_media_context_menu_loop_action->set_checked(menu.is_looping);

        auto screen_position = view().screen_relative_rect().location().translated(widget_position);

        if (menu.is_video)
            m_video_context_menu->popup(screen_position);
        else
            m_audio_context_menu->popup(screen_position);
    };

    view().on_link_middle_click = [this](auto& href, auto&, auto) {
        view().on_link_click(href, "_blank", 0);
    };

    view().on_title_change = [this](auto const& title) {
        m_history.update_title(title);
        m_title = title;

        if (on_title_change)
            on_title_change(m_title);
    };

    view().on_favicon_change = [this](auto& icon) {
        m_icon = icon;
        m_location_box->set_icon(&icon);
        if (on_favicon_change)
            on_favicon_change(icon);
    };

    view().on_get_all_cookies = [this](auto& url) -> Vector<Web::Cookie::Cookie> {
        if (on_get_all_cookies)
            return on_get_all_cookies(url);
        return {};
    };

    view().on_get_named_cookie = [this](auto& url, auto& name) -> Optional<Web::Cookie::Cookie> {
        if (on_get_named_cookie)
            return on_get_named_cookie(url, name);
        return {};
    };

    view().on_get_cookie = [this](auto& url, auto source) -> DeprecatedString {
        if (on_get_cookie)
            return on_get_cookie(url, source);
        return {};
    };

    view().on_set_cookie = [this](auto& url, auto& cookie, auto source) {
        if (on_set_cookie)
            on_set_cookie(url, cookie, source);
    };

    view().on_update_cookie = [this](auto& cookie) {
        if (on_update_cookie)
            on_update_cookie(cookie);
    };

    view().on_get_source = [this](auto& url, auto& source) {
        view_source(url, source);
    };

    view().on_get_dom_tree = [this](auto& dom_tree) {
        if (m_dom_inspector_widget)
            m_dom_inspector_widget->set_dom_json(dom_tree);
    };

    view().on_get_dom_node_properties = [this](auto node_id, auto& specified, auto& computed, auto& custom_properties, auto& node_box_sizing, auto& aria_properties_state) {
        m_dom_inspector_widget->set_dom_node_properties_json({ node_id }, specified, computed, custom_properties, node_box_sizing, aria_properties_state);
    };

    view().on_get_accessibility_tree = [this](auto& accessibility_tree) {
        if (m_dom_inspector_widget)
            m_dom_inspector_widget->set_accessibility_json(accessibility_tree);
    };

    view().on_js_console_new_message = [this](auto message_index) {
        if (m_console_widget)
            m_console_widget->notify_about_new_console_message(message_index);
    };

    view().on_get_js_console_messages = [this](auto start_index, auto& message_types, auto& messages) {
        if (m_console_widget)
            m_console_widget->handle_console_messages(start_index, message_types, messages);
    };

    auto focus_location_box_action = GUI::Action::create(
        "Focus location box", { Mod_Ctrl, Key_L }, Key_F6, [this](auto&) {
            m_location_box->set_focus(true);
            m_location_box->select_current_line();
        },
        this);

    m_statusbar = *find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    view().on_link_hover = [this](auto& url) {
        update_status(String::from_deprecated_string(url.to_deprecated_string()).release_value_but_fixme_should_propagate_errors());
    };

    view().on_link_unhover = [this]() {
        view().set_override_cursor(Gfx::StandardCursor::None);
        update_status();
    };

    view().on_back_button = [this] {
        if (m_history.can_go_back())
            go_back();
    };

    view().on_forward_button = [this] {
        if (m_history.can_go_forward())
            go_forward();
    };

    view().on_new_tab = [this](auto activate_tab) {
        auto& tab = this->window().create_new_tab(URL("about:blank"), activate_tab);
        return tab.view().handle();
    };

    view().on_activate_tab = [this]() {
        on_activate_tab_request(*this);
    };

    view().on_close = [this] {
        on_tab_close_request(*this);
    };

    m_tab_context_menu = GUI::Menu::construct();
    m_tab_context_menu->add_action(GUI::CommonActions::make_reload_action([this](auto&) {
        reload();
    }));
    m_tab_context_menu->add_action(GUI::CommonActions::make_close_tab_action([this](auto&) {
        on_tab_close_request(*this);
    }));
    m_tab_context_menu->add_action(GUI::Action::create("&Duplicate Tab", g_icon_bag.duplicate_tab, [this](auto&) {
        on_tab_open_request(url());
    }));
    m_tab_context_menu->add_action(GUI::Action::create("Close &Other Tabs", g_icon_bag.close_other_tabs, [this](auto&) {
        on_tab_close_other_request(*this);
    }));

    auto take_visible_screenshot_action = GUI::Action::create(
        "Take &Visible Screenshot"sv, g_icon_bag.filetype_image, [this](auto&) {
            if (auto result = view().take_screenshot(WebView::ViewImplementation::ScreenshotType::Visible); result.is_error()) {
                auto error = String::formatted("{}", result.error()).release_value_but_fixme_should_propagate_errors();
                GUI::MessageBox::show_error(&this->window(), error);
            }
        },
        this);
    take_visible_screenshot_action->set_status_tip("Save a screenshot of the visible portion of the current tab to the Downloads directory"_string);

    auto take_full_screenshot_action = GUI::Action::create(
        "Take &Full Screenshot"sv, g_icon_bag.filetype_image, [this](auto&) {
            if (auto result = view().take_screenshot(WebView::ViewImplementation::ScreenshotType::Full); result.is_error()) {
                auto error = String::formatted("{}", result.error()).release_value_but_fixme_should_propagate_errors();
                GUI::MessageBox::show_error(&this->window(), error);
            }
        },
        this);
    take_full_screenshot_action->set_status_tip("Save a screenshot of the entirety of the current tab to the Downloads directory"_string);

    m_page_context_menu = GUI::Menu::construct();
    m_page_context_menu->add_action(window.go_back_action());
    m_page_context_menu->add_action(window.go_forward_action());
    m_page_context_menu->add_action(window.reload_action());
    m_page_context_menu->add_separator();
    m_page_context_menu->add_action(window.copy_selection_action());
    m_page_context_menu->add_action(window.select_all_action());
    m_page_context_menu->add_separator();
    m_page_context_menu->add_action(move(take_visible_screenshot_action));
    m_page_context_menu->add_action(move(take_full_screenshot_action));
    m_page_context_menu->add_separator();
    m_page_context_menu->add_action(window.view_source_action());
    m_page_context_menu->add_action(window.inspect_dom_tree_action());
    m_page_context_menu->add_action(window.inspect_dom_node_action());

    view().on_context_menu_request = [&](auto widget_position) {
        auto screen_position = view().screen_relative_rect().location().translated(widget_position);
        m_page_context_menu->popup(screen_position);
    };
}

void Tab::update_reset_zoom_button()
{
    auto zoom_level = view().zoom_level();
    if (zoom_level != 1.0f) {
        m_reset_zoom_button->set_text(MUST(String::formatted("{}%", round_to<int>(zoom_level * 100))));
        m_reset_zoom_button->set_visible(true);
    } else {
        m_reset_zoom_button->set_visible(false);
    }
}

Optional<URL> Tab::url_from_location_bar(MayAppendTLD may_append_tld)
{
    DeprecatedString text = m_location_box->text();

    StringBuilder builder;
    builder.append(text);
    if (may_append_tld == MayAppendTLD::Yes) {
        // FIXME: Expand the list of top level domains.
        if (!(text.ends_with(".com"sv) || text.ends_with(".net"sv) || text.ends_with(".org"sv))) {
            builder.append(".com"sv);
        }
    }
    auto final_text = builder.to_deprecated_string();

    auto error_or_absolute_url = PublicSuffix::absolute_url(final_text);
    if (error_or_absolute_url.is_error()) {
        if (g_search_engine.is_empty()) {
            GUI::MessageBox::show(&this->window(), "Select a search engine in the Settings menu before searching."sv, "No search engine selected"sv, GUI::MessageBox::Type::Information);
            return {};
        }

        return URL(g_search_engine.replace("{}"sv, URL::percent_encode(final_text), ReplaceMode::FirstOnly));
    }

    return URL(error_or_absolute_url.release_value());
}

void Tab::load(const URL& url, LoadType load_type)
{
    m_is_history_navigation = (load_type == LoadType::HistoryNavigation);
    m_web_content_view->load(url);
    m_location_box->set_focus(false);
}

URL Tab::url() const
{
    return m_web_content_view->url();
}

void Tab::reload()
{
    load(url());
}

void Tab::go_back(int steps)
{
    m_history.go_back(steps);
    update_actions();
    load(m_history.current().url, LoadType::HistoryNavigation);
}

void Tab::go_forward(int steps)
{
    m_history.go_forward(steps);
    update_actions();
    load(m_history.current().url, LoadType::HistoryNavigation);
}

void Tab::update_actions()
{
    auto& window = this->window();
    if (this != &window.active_tab())
        return;
    window.go_back_action().set_enabled(m_history.can_go_back());
    window.go_forward_action().set_enabled(m_history.can_go_forward());
}

ErrorOr<void> Tab::bookmark_current_url()
{
    auto url = this->url().to_deprecated_string();
    if (BookmarksBarWidget::the().contains_bookmark(url)) {
        TRY(BookmarksBarWidget::the().remove_bookmark(url));
    } else {
        TRY(BookmarksBarWidget::the().add_bookmark(url, m_title));
    }

    return {};
}

void Tab::update_bookmark_button(StringView url)
{
    if (BookmarksBarWidget::the().contains_bookmark(url)) {
        m_bookmark_button->set_icon(g_icon_bag.bookmark_filled);
        m_bookmark_button->set_tooltip_deprecated("Remove Bookmark");
    } else {
        m_bookmark_button->set_icon(g_icon_bag.bookmark_contour);
        m_bookmark_button->set_tooltip_deprecated("Add Bookmark");
    }
}

void Tab::did_become_active()
{
    BookmarksBarWidget::the().on_bookmark_click = [this](auto& url, auto open) {
        if (open == BookmarksBarWidget::Open::InNewTab)
            on_tab_open_request(url);
        else if (open == BookmarksBarWidget::Open::InNewWindow)
            on_window_open_request(url);
        else
            load(url);
    };

    BookmarksBarWidget::the().on_bookmark_hover = [this](auto&, auto& url) {
        m_statusbar->set_text(String::from_deprecated_string(url).release_value_but_fixme_should_propagate_errors());
    };

    BookmarksBarWidget::the().on_bookmark_change = [this]() {
        update_bookmark_button(url().to_deprecated_string());
    };

    BookmarksBarWidget::the().remove_from_parent();
    m_toolbar_container->add_child(BookmarksBarWidget::the());

    auto is_fullscreen = window().is_fullscreen();
    m_toolbar_container->set_visible(!is_fullscreen);
    m_statusbar->set_visible(!is_fullscreen);

    update_actions();
}

void Tab::context_menu_requested(Gfx::IntPoint screen_position)
{
    m_tab_context_menu->popup(screen_position);
}

void Tab::content_filters_changed()
{
    if (g_content_filters_enabled)
        m_web_content_view->set_content_filters(g_content_filters);
    else
        m_web_content_view->set_content_filters({});
}

void Tab::autoplay_allowlist_changed()
{
    if (g_autoplay_allowed_on_all_websites)
        m_web_content_view->set_autoplay_allowed_on_all_websites();
    else
        m_web_content_view->set_autoplay_allowlist(g_autoplay_allowlist);
}

void Tab::proxy_mappings_changed()
{
    m_web_content_view->set_proxy_mappings(g_proxies, g_proxy_mappings);
}

void Tab::action_entered(GUI::Action& action)
{
    m_statusbar->set_override_text(action.status_tip());
}

void Tab::action_left(GUI::Action&)
{
    m_statusbar->set_override_text({});
}

void Tab::window_position_changed(Gfx::IntPoint position)
{
    m_web_content_view->set_window_position(position);
}

void Tab::window_size_changed(Gfx::IntSize size)
{
    m_web_content_view->set_window_size(size);
}

BrowserWindow const& Tab::window() const
{
    return static_cast<BrowserWindow const&>(*Widget::window());
}

BrowserWindow& Tab::window()
{
    return static_cast<BrowserWindow&>(*Widget::window());
}

void Tab::show_inspector_window(Browser::Tab::InspectorTarget inspector_target)
{
    if (!m_dom_inspector_widget) {
        auto window = GUI::Window::construct(&this->window());
        window->set_window_mode(GUI::WindowMode::Modeless);
        window->resize(325, 500);
        window->set_title("Inspector");
        window->set_icon(g_icon_bag.inspector_object);
        window->on_close = [&]() {
            m_web_content_view->clear_inspected_dom_node();
        };
        m_dom_inspector_widget = window->set_main_widget<InspectorWidget>().release_value_but_fixme_should_propagate_errors();
        m_dom_inspector_widget->set_web_view(*m_web_content_view);
        m_web_content_view->inspect_dom_tree();
        m_web_content_view->inspect_accessibility_tree();
    }

    if (inspector_target == InspectorTarget::HoveredElement) {
        // FIXME: Handle pseudo-elements
        auto hovered_node = m_web_content_view->get_hovered_node_id();
        m_dom_inspector_widget->set_selection({ hovered_node });
    } else {
        VERIFY(inspector_target == InspectorTarget::Document);
        m_dom_inspector_widget->select_default_node();
    }

    auto* window = m_dom_inspector_widget->window();
    window->show();
    window->move_to_front();
}

void Tab::close_sub_widgets()
{
    auto close_widget_window = [](auto& widget) {
        if (widget) {
            auto* window = widget->window();
            window->close();
        }
    };
    close_widget_window(m_dom_inspector_widget);
    close_widget_window(m_console_widget);
    close_widget_window(m_storage_widget);
}

void Tab::show_console_window()
{
    if (!m_console_widget) {
        auto console_window = GUI::Window::construct(&window());
        console_window->resize(500, 300);
        console_window->set_title("JS Console");
        console_window->set_icon(g_icon_bag.filetype_javascript);
        m_console_widget = console_window->set_main_widget<ConsoleWidget>().release_value_but_fixme_should_propagate_errors();
        m_console_widget->on_js_input = [this](DeprecatedString const& js_source) {
            m_web_content_view->js_console_input(js_source);
        };
        m_console_widget->on_request_messages = [this](i32 start_index) {
            m_web_content_view->js_console_request_messages(start_index);
        };
    }

    auto* window = m_console_widget->window();
    window->show();
    window->move_to_front();
}

void Tab::show_storage_inspector()
{
    if (!m_storage_widget) {
        auto storage_window = GUI::Window::construct(&window());
        storage_window->resize(500, 300);
        storage_window->set_title("Storage Inspector");
        storage_window->set_icon(g_icon_bag.cookie);
        m_storage_widget = storage_window->set_main_widget<StorageWidget>().release_value_but_fixme_should_propagate_errors();
        m_storage_widget->on_update_cookie = [this](Web::Cookie::Cookie cookie) {
            if (on_update_cookie)
                on_update_cookie(move(cookie));
        };
    }

    if (on_get_cookies_entries) {
        auto cookies = on_get_cookies_entries();
        m_storage_widget->clear_cookies();
        m_storage_widget->set_cookies_entries(cookies);
    }

    if (on_get_local_storage_entries) {
        auto local_storage_entries = on_get_local_storage_entries();
        m_storage_widget->clear_local_storage_entries();
        m_storage_widget->set_local_storage_entries(move(local_storage_entries));
    }

    if (on_get_session_storage_entries) {
        auto session_storage_entries = on_get_session_storage_entries();
        m_storage_widget->clear_session_storage_entries();
        m_storage_widget->set_session_storage_entries(move(session_storage_entries));
    }

    auto* window = m_storage_widget->window();
    window->show();
    window->move_to_front();
}

void Tab::show_history_inspector()
{
    if (!m_history_widget) {
        auto history_window = GUI::Window::construct(&window());
        history_window->resize(500, 300);
        history_window->set_title("History");
        history_window->set_icon(g_icon_bag.history);
        m_history_widget = history_window->set_main_widget<HistoryWidget>().release_value_but_fixme_should_propagate_errors();
    }

    m_history_widget->clear_history_entries();
    m_history_widget->set_history_entries(m_history.get_all_history_entries());

    auto* window = m_history_widget->window();
    window->show();
    window->move_to_front();
}

void Tab::show_event(GUI::ShowEvent&)
{
    m_web_content_view->set_visible(true);
}

void Tab::hide_event(GUI::HideEvent&)
{
    m_web_content_view->set_visible(false);
}

void Tab::enable_webdriver_mode()
{
    m_web_content_view->connect_to_webdriver(Browser::g_webdriver_content_ipc_path);
    auto& webdriver_banner = *find_descendant_of_type_named<GUI::Widget>("webdriver_banner");
    webdriver_banner.set_visible(true);
}

}
