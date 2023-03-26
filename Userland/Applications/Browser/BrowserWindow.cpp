/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, networkException <networkexception@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrowserWindow.h"
#include "BookmarksBarWidget.h"
#include "Browser.h"
#include "ConsoleWidget.h"
#include "CookieJar.h"
#include "InspectorWidget.h"
#include "Tab.h"
#include <AK/LexicalPath.h>
#include <Applications/Browser/BrowserWindowGML.h>
#include <LibConfig/Client.h>
#include <LibCore/DateTime.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Icon.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Process.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/Widget.h>
#include <LibGfx/ImageFormats/PNGWriter.h>
#include <LibJS/Interpreter.h>
#include <LibWeb/CSS/PreferredColorScheme.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWebView/OutOfProcessWebView.h>
#include <LibWebView/WebContentClient.h>

namespace Browser {

static DeprecatedString bookmarks_file_path()
{
    StringBuilder builder;
    builder.append(Core::StandardPaths::config_directory());
    builder.append("/bookmarks.json"sv);
    return builder.to_deprecated_string();
}

static DeprecatedString search_engines_file_path()
{
    StringBuilder builder;
    builder.append(Core::StandardPaths::config_directory());
    builder.append("/SearchEngines.json"sv);
    return builder.to_deprecated_string();
}

BrowserWindow::BrowserWindow(CookieJar& cookie_jar, URL url)
    : m_cookie_jar(cookie_jar)
    , m_window_actions(*this)
{
    auto app_icon = GUI::Icon::default_icon("app-browser"sv);
    m_bookmarks_bar = Browser::BookmarksBarWidget::construct(Browser::bookmarks_file_path(), true);

    resize(730, 560);
    set_icon(app_icon.bitmap_for_size(16));
    set_title("Browser");

    auto widget = set_main_widget<GUI::Widget>().release_value_but_fixme_should_propagate_errors();
    widget->load_from_gml(browser_window_gml).release_value_but_fixme_should_propagate_errors();

    auto& top_line = *widget->find_descendant_of_type_named<GUI::HorizontalSeparator>("top_line");

    m_tab_widget = *widget->find_descendant_of_type_named<GUI::TabWidget>("tab_widget");
    m_tab_widget->on_tab_count_change = [&top_line](size_t tab_count) {
        top_line.set_visible(tab_count > 1);
    };

    m_tab_widget->on_change = [this](auto& active_widget) {
        auto& tab = static_cast<Browser::Tab&>(active_widget);
        set_window_title_for_tab(tab);
        tab.did_become_active();
        update_zoom_menu_text();
    };

    m_tab_widget->on_middle_click = [](auto& clicked_widget) {
        auto& tab = static_cast<Browser::Tab&>(clicked_widget);
        tab.on_tab_close_request(tab);
    };

    m_tab_widget->on_tab_close_click = [](auto& clicked_widget) {
        auto& tab = static_cast<Browser::Tab&>(clicked_widget);
        tab.on_tab_close_request(tab);
    };

    m_tab_widget->on_context_menu_request = [](auto& clicked_widget, const GUI::ContextMenuEvent& context_menu_event) {
        auto& tab = static_cast<Browser::Tab&>(clicked_widget);
        tab.context_menu_requested(context_menu_event.screen_position());
    };

    m_window_actions.on_create_new_tab = [this] {
        create_new_tab(Browser::url_from_user_input(Browser::g_new_tab_url), Web::HTML::ActivateTab::Yes);
    };

    m_window_actions.on_create_new_window = [this] {
        create_new_window(g_home_url);
    };

    m_window_actions.on_next_tab = [this] {
        m_tab_widget->activate_next_tab();
    };

    m_window_actions.on_previous_tab = [this] {
        m_tab_widget->activate_previous_tab();
    };

    for (size_t i = 0; i <= 7; ++i) {
        m_window_actions.on_tabs.append([this, i] {
            if (i >= m_tab_widget->tab_count())
                return;
            m_tab_widget->set_tab_index(i);
        });
    }
    m_window_actions.on_tabs.append([this] {
        m_tab_widget->activate_last_tab();
    });

    m_window_actions.on_show_bookmarks_bar = [](auto& action) {
        Browser::BookmarksBarWidget::the().set_visible(action.is_checked());
        Config::write_bool("Browser"sv, "Preferences"sv, "ShowBookmarksBar"sv, action.is_checked());
    };

    bool show_bookmarks_bar = Config::read_bool("Browser"sv, "Preferences"sv, "ShowBookmarksBar"sv, true);
    m_window_actions.show_bookmarks_bar_action().set_checked(show_bookmarks_bar);
    Browser::BookmarksBarWidget::the().set_visible(show_bookmarks_bar);

    m_window_actions.on_vertical_tabs = [this](auto& action) {
        m_tab_widget->set_tab_position(action.is_checked() ? GUI::TabWidget::TabPosition::Left : GUI::TabWidget::TabPosition::Top);
        Config::write_bool("Browser"sv, "Preferences"sv, "VerticalTabs"sv, action.is_checked());
    };

    bool vertical_tabs = Config::read_bool("Browser"sv, "Preferences"sv, "VerticalTabs"sv, false);
    m_window_actions.vertical_tabs_action().set_checked(vertical_tabs);
    m_tab_widget->set_tab_position(vertical_tabs ? GUI::TabWidget::TabPosition::Left : GUI::TabWidget::TabPosition::Top);

    build_menus();

    create_new_tab(move(url), Web::HTML::ActivateTab::Yes);
}

void BrowserWindow::build_menus()
{
    auto& file_menu = add_menu("&File");
    file_menu.add_action(WindowActions::the().create_new_tab_action());
    file_menu.add_action(WindowActions::the().create_new_window_action());

    auto close_tab_action = GUI::CommonActions::make_close_tab_action([this](auto&) {
        active_tab().on_tab_close_request(active_tab());
    },
        this);
    file_menu.add_action(close_tab_action);

    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& view_menu = add_menu("&View");
    view_menu.add_action(WindowActions::the().show_bookmarks_bar_action());
    view_menu.add_action(WindowActions::the().vertical_tabs_action());
    view_menu.add_separator();
    m_zoom_menu = view_menu.add_submenu("&Zoom");
    m_zoom_menu->add_action(GUI::CommonActions::make_zoom_in_action(
        [this](auto&) {
            auto& tab = active_tab();
            tab.view().zoom_in();
            update_zoom_menu_text();
        },
        this));
    m_zoom_menu->add_action(GUI::CommonActions::make_zoom_out_action(
        [this](auto&) {
            auto& tab = active_tab();
            tab.view().zoom_out();
            update_zoom_menu_text();
        },
        this));
    m_zoom_menu->add_action(GUI::CommonActions::make_reset_zoom_action(
        [this](auto&) {
            auto& tab = active_tab();
            tab.view().reset_zoom();
            update_zoom_menu_text();
        },
        this));
    view_menu.add_separator();
    view_menu.add_action(GUI::CommonActions::make_fullscreen_action(
        [this](auto&) {
            auto& tab = active_tab();
            set_fullscreen(!is_fullscreen());

            auto is_fullscreen = this->is_fullscreen();
            tab_widget().set_bar_visible(!is_fullscreen && tab_widget().children().size() > 1);
            tab.m_toolbar_container->set_visible(!is_fullscreen);
            tab.m_statusbar->set_visible(!is_fullscreen);

            if (is_fullscreen) {
                tab.view().set_frame_thickness(0);
            } else {
                tab.view().set_frame_thickness(2);
            }
        },
        this));

    m_go_back_action = GUI::CommonActions::make_go_back_action([this](auto&) { active_tab().go_back(); }, this);
    m_go_forward_action = GUI::CommonActions::make_go_forward_action([this](auto&) { active_tab().go_forward(); }, this);
    m_go_home_action = GUI::CommonActions::make_go_home_action([this](auto&) { active_tab().load(Browser::url_from_user_input(g_home_url)); }, this);
    m_go_home_action->set_status_tip("Go to home page");
    m_reload_action = GUI::CommonActions::make_reload_action([this](auto&) { active_tab().reload(); }, this);
    m_reload_action->set_status_tip("Reload current page");

    auto& go_menu = add_menu("&Go");
    go_menu.add_action(*m_go_back_action);
    go_menu.add_action(*m_go_forward_action);
    go_menu.add_action(*m_go_home_action);
    go_menu.add_separator();
    go_menu.add_action(*m_reload_action);

    m_copy_selection_action = GUI::CommonActions::make_copy_action([this](auto&) {
        auto& tab = active_tab();
        auto selected_text = tab.view().selected_text();
        if (!selected_text.is_empty())
            GUI::Clipboard::the().set_plain_text(selected_text);
    });

    m_select_all_action = GUI::CommonActions::make_select_all_action([this](auto&) {
        active_tab().view().select_all();
    });

    m_view_source_action = GUI::Action::create(
        "View &Source", { Mod_Ctrl, Key_U }, g_icon_bag.code, [this](auto&) {
            active_tab().view().get_source();
        },
        this);
    m_view_source_action->set_status_tip("View source code of the current page");

    m_inspect_dom_tree_action = GUI::Action::create(
        "Inspect &DOM Tree", { Mod_None, Key_F12 }, g_icon_bag.dom_tree, [this](auto&) {
            active_tab().show_inspector_window(Tab::InspectorTarget::Document);
        },
        this);
    m_inspect_dom_tree_action->set_status_tip("Open inspector window for this page");

    m_inspect_dom_node_action = GUI::Action::create(
        "&Inspect Element", g_icon_bag.inspect, [this](auto&) {
            active_tab().show_inspector_window(Tab::InspectorTarget::HoveredElement);
        },
        this);
    m_inspect_dom_node_action->set_status_tip("Open inspector for this element");

    m_take_visible_screenshot_action = GUI::Action::create(
        "Take &Visible Screenshot"sv, g_icon_bag.filetype_image, [this](auto&) {
            if (auto result = take_screenshot(ScreenshotType::Visible); result.is_error())
                GUI::MessageBox::show_error(this, DeprecatedString::formatted("{}", result.error()));
        },
        this);
    m_take_visible_screenshot_action->set_status_tip("Save a screenshot of the visible portion of the current tab to the Downloads directory"sv);

    m_take_full_screenshot_action = GUI::Action::create(
        "Take &Full Screenshot"sv, g_icon_bag.filetype_image, [this](auto&) {
            if (auto result = take_screenshot(ScreenshotType::Full); result.is_error())
                GUI::MessageBox::show_error(this, DeprecatedString::formatted("{}", result.error()));
        },
        this);
    m_take_full_screenshot_action->set_status_tip("Save a screenshot of the entirety of the current tab to the Downloads directory"sv);

    auto& inspect_menu = add_menu("&Inspect");
    inspect_menu.add_action(*m_view_source_action);
    inspect_menu.add_action(*m_inspect_dom_tree_action);

    auto js_console_action = GUI::Action::create(
        "Open &JS Console", { Mod_Ctrl, Key_I }, g_icon_bag.filetype_javascript, [this](auto&) {
            active_tab().show_console_window();
        },
        this);
    js_console_action->set_status_tip("Open JavaScript console for this page");
    inspect_menu.add_action(js_console_action);

    auto storage_window_action = GUI::Action::create(
        "Open S&torage Inspector", g_icon_bag.cookie, [this](auto&) {
            active_tab().show_storage_inspector();
        },
        this);
    storage_window_action->set_status_tip("Show Storage inspector for this page");
    inspect_menu.add_action(storage_window_action);

    auto history_window_action = GUI::Action::create(
        "Open &History Window", g_icon_bag.history, [this](auto&) {
            active_tab().show_history_inspector();
        },
        this);
    storage_window_action->set_status_tip("Show History inspector for this tab");
    inspect_menu.add_action(history_window_action);

    auto& settings_menu = add_menu("&Settings");

    m_change_homepage_action = GUI::Action::create(
        "Set Homepage URL...", g_icon_bag.go_home, [this](auto&) {
            auto homepage_url = Config::read_string("Browser"sv, "Preferences"sv, "Home"sv, "about:blank"sv);
            if (GUI::InputBox::show(this, homepage_url, "Enter URL"sv, "Change homepage URL"sv) == GUI::InputBox::ExecResult::OK) {
                if (URL(homepage_url).is_valid()) {
                    Config::write_string("Browser"sv, "Preferences"sv, "Home"sv, homepage_url);
                    Browser::g_home_url = homepage_url;
                } else {
                    GUI::MessageBox::show_error(this, "The URL you have entered is not valid"sv);
                }
            }
        },
        this);

    settings_menu.add_action(*m_change_homepage_action);

    auto load_search_engines_result = load_search_engines(settings_menu);
    if (load_search_engines_result.is_error()) {
        dbgln("Failed to open search-engines file: {}", load_search_engines_result.error());
    }

    auto& color_scheme_menu = settings_menu.add_submenu("&Color Scheme");
    color_scheme_menu.set_icon(g_icon_bag.color_chooser);
    {
        auto current_setting = Web::CSS::preferred_color_scheme_from_string(Config::read_string("Browser"sv, "Preferences"sv, "ColorScheme"sv, "auto"sv));
        m_color_scheme_actions.set_exclusive(true);

        auto add_color_scheme_action = [&](auto& name, Web::CSS::PreferredColorScheme preference_value) {
            auto action = GUI::Action::create_checkable(
                name, [=, this](auto&) {
                    Config::write_string("Browser"sv, "Preferences"sv, "ColorScheme"sv, Web::CSS::preferred_color_scheme_to_string(preference_value));
                    active_tab().view().set_preferred_color_scheme(preference_value);
                },
                this);
            if (current_setting == preference_value)
                action->set_checked(true);
            color_scheme_menu.add_action(action);
            m_color_scheme_actions.add_action(action);
        };

        add_color_scheme_action("Follow system theme", Web::CSS::PreferredColorScheme::Auto);
        add_color_scheme_action("Light", Web::CSS::PreferredColorScheme::Light);
        add_color_scheme_action("Dark", Web::CSS::PreferredColorScheme::Dark);
    }

    settings_menu.add_separator();
    auto open_settings_action = GUI::Action::create("Browser &Settings", Gfx::Bitmap::load_from_file("/res/icons/16x16/settings.png"sv).release_value_but_fixme_should_propagate_errors(),
        [this](auto&) {
            GUI::Process::spawn_or_show_error(this, "/bin/BrowserSettings"sv);
        });
    settings_menu.add_action(move(open_settings_action));

    auto& debug_menu = add_menu("&Debug");
    debug_menu.add_action(GUI::Action::create(
        "Dump &DOM Tree", g_icon_bag.dom_tree, [this](auto&) {
            active_tab().view().debug_request("dump-dom-tree");
        },
        this));
    debug_menu.add_action(GUI::Action::create(
        "Dump &Layout Tree", g_icon_bag.layout, [this](auto&) {
            active_tab().view().debug_request("dump-layout-tree");
        },
        this));
    debug_menu.add_action(GUI::Action::create(
        "Dump &Paint Tree", g_icon_bag.layout, [this](auto&) {
            active_tab().view().debug_request("dump-paint-tree");
        },
        this));
    debug_menu.add_action(GUI::Action::create(
        "Dump S&tacking Context Tree", g_icon_bag.layers, [this](auto&) {
            active_tab().view().debug_request("dump-stacking-context-tree");
        },
        this));
    debug_menu.add_action(GUI::Action::create(
        "Dump &Style Sheets", g_icon_bag.filetype_css, [this](auto&) {
            active_tab().view().debug_request("dump-style-sheets");
        },
        this));
    debug_menu.add_action(GUI::Action::create("Dump &History", { Mod_Ctrl, Key_H }, g_icon_bag.history, [this](auto&) {
        active_tab().m_history.dump();
    }));
    debug_menu.add_action(GUI::Action::create("Dump C&ookies", g_icon_bag.cookie, [this](auto&) {
        auto& tab = active_tab();
        if (tab.on_dump_cookies)
            tab.on_dump_cookies();
    }));
    debug_menu.add_action(GUI::Action::create("Dump Loc&al Storage", g_icon_bag.local_storage, [this](auto&) {
        active_tab().view().debug_request("dump-local-storage");
    }));
    debug_menu.add_separator();
    auto line_box_borders_action = GUI::Action::create_checkable(
        "Line &Box Borders", [this](auto& action) {
            active_tab().view().debug_request("set-line-box-borders", action.is_checked() ? "on" : "off");
        },
        this);
    line_box_borders_action->set_checked(false);
    debug_menu.add_action(line_box_borders_action);

    debug_menu.add_separator();
    debug_menu.add_action(GUI::Action::create("Collect &Garbage", { Mod_Ctrl | Mod_Shift, Key_G }, g_icon_bag.trash_can, [this](auto&) {
        active_tab().view().debug_request("collect-garbage");
    }));
    debug_menu.add_action(GUI::Action::create("Clear &Cache", { Mod_Ctrl | Mod_Shift, Key_C }, g_icon_bag.clear_cache, [this](auto&) {
        active_tab().view().debug_request("clear-cache");
    }));

    m_user_agent_spoof_actions.set_exclusive(true);
    auto& spoof_user_agent_menu = debug_menu.add_submenu("Spoof &User Agent");
    m_disable_user_agent_spoofing = GUI::Action::create_checkable("Disabled", [this](auto&) {
        active_tab().view().debug_request("spoof-user-agent", Web::default_user_agent);
    });
    m_disable_user_agent_spoofing->set_status_tip(Web::default_user_agent);
    spoof_user_agent_menu.add_action(*m_disable_user_agent_spoofing);
    spoof_user_agent_menu.set_icon(g_icon_bag.spoof);
    m_user_agent_spoof_actions.add_action(*m_disable_user_agent_spoofing);
    m_disable_user_agent_spoofing->set_checked(true);

    auto add_user_agent = [this, &spoof_user_agent_menu](auto& name, auto& user_agent) {
        auto action = GUI::Action::create_checkable(name, [this, user_agent](auto&) {
            active_tab().view().debug_request("spoof-user-agent", user_agent);
        });
        action->set_status_tip(user_agent);
        spoof_user_agent_menu.add_action(action);
        m_user_agent_spoof_actions.add_action(action);
    };
    add_user_agent("Chrome Linux Desktop", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.128 Safari/537.36");
    add_user_agent("Firefox Linux Desktop", "Mozilla/5.0 (X11; Linux x86_64; rv:87.0) Gecko/20100101 Firefox/87.0");
    add_user_agent("Safari macOS Desktop", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_2_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.3 Safari/605.1.15");
    add_user_agent("Chrome Android Mobile", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.66 Mobile Safari/537.36");
    add_user_agent("Firefox Android Mobile", "Mozilla/5.0 (Android 11; Mobile; rv:68.0) Gecko/68.0 Firefox/86.0");
    add_user_agent("Safari iOS Mobile", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_4_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1");

    auto custom_user_agent = GUI::Action::create_checkable("Custom...", [this](auto& action) {
        DeprecatedString user_agent;
        if (GUI::InputBox::show(this, user_agent, "Enter User Agent:"sv, "Custom User Agent"sv, GUI::InputType::NonemptyText) != GUI::InputBox::ExecResult::OK) {
            m_disable_user_agent_spoofing->activate();
            return;
        }
        active_tab().view().debug_request("spoof-user-agent", user_agent);
        action.set_status_tip(user_agent);
    });
    spoof_user_agent_menu.add_action(custom_user_agent);
    m_user_agent_spoof_actions.add_action(custom_user_agent);

    debug_menu.add_separator();
    auto scripting_enabled_action = GUI::Action::create_checkable(
        "Enable Scripting", [this](auto& action) {
            active_tab().view().debug_request("scripting", action.is_checked() ? "on" : "off");
        },
        this);
    scripting_enabled_action->set_checked(true);
    debug_menu.add_action(scripting_enabled_action);

    auto block_pop_ups_action = GUI::Action::create_checkable(
        "Block Pop-ups", [this](auto& action) {
            active_tab().view().debug_request("block-pop-ups", action.is_checked() ? "on" : "off");
        },
        this);
    block_pop_ups_action->set_checked(true);
    debug_menu.add_action(block_pop_ups_action);

    auto same_origin_policy_action = GUI::Action::create_checkable(
        "Enable Same Origin &Policy", [this](auto& action) {
            active_tab().view().debug_request("same-origin-policy", action.is_checked() ? "on" : "off");
        },
        this);
    same_origin_policy_action->set_checked(false);
    debug_menu.add_action(same_origin_policy_action);

    auto& help_menu = add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_command_palette_action(this));
    help_menu.add_action(WindowActions::the().about_action());
}

ErrorOr<void> BrowserWindow::load_search_engines(GUI::Menu& settings_menu)
{
    m_search_engine_actions.set_exclusive(true);
    auto& search_engine_menu = settings_menu.add_submenu("&Search Engine");
    search_engine_menu.set_icon(g_icon_bag.find);
    bool search_engine_set = false;

    m_disable_search_engine_action = GUI::Action::create_checkable(
        "Disable", [](auto&) {
            g_search_engine = {};
            Config::write_string("Browser"sv, "Preferences"sv, "SearchEngine"sv, g_search_engine);
        },
        this);
    search_engine_menu.add_action(*m_disable_search_engine_action);
    m_search_engine_actions.add_action(*m_disable_search_engine_action);
    m_disable_search_engine_action->set_checked(true);

    auto search_engines_file = TRY(Core::File::open(Browser::search_engines_file_path(), Core::File::OpenMode::Read));
    auto file_size = TRY(search_engines_file->size());
    auto buffer = TRY(ByteBuffer::create_uninitialized(file_size));
    if (!search_engines_file->read_until_filled(buffer).is_error()) {
        StringView buffer_contents { buffer.bytes() };
        if (auto json = TRY(JsonValue::from_string(buffer_contents)); json.is_array()) {
            auto json_array = json.as_array();
            for (auto& json_item : json_array.values()) {
                if (!json_item.is_object())
                    continue;
                auto search_engine = json_item.as_object();
                auto name = search_engine.get_deprecated_string("title"sv).value();
                auto url_format = search_engine.get_deprecated_string("url_format"sv).value();

                auto action = GUI::Action::create_checkable(
                    name, [&, url_format](auto&) {
                        g_search_engine = url_format;
                        Config::write_string("Browser"sv, "Preferences"sv, "SearchEngine"sv, g_search_engine);
                    },
                    this);
                search_engine_menu.add_action(action);
                m_search_engine_actions.add_action(action);

                if (g_search_engine == url_format) {
                    action->set_checked(true);
                    search_engine_set = true;
                }
                action->set_status_tip(url_format);
            }
        }
    }

    auto custom_search_engine_action = GUI::Action::create_checkable("Custom...", [&](auto& action) {
        DeprecatedString search_engine;
        if (GUI::InputBox::show(this, search_engine, "Enter URL template:"sv, "Custom Search Engine"sv, GUI::InputType::NonemptyText, "https://host/search?q={}"sv) != GUI::InputBox::ExecResult::OK) {
            m_disable_search_engine_action->activate();
            return;
        }

        auto argument_count = search_engine.count("{}"sv);
        if (argument_count != 1) {
            GUI::MessageBox::show(this, "Invalid format, must contain '{}' once!"sv, "Error"sv, GUI::MessageBox::Type::Error);
            m_disable_search_engine_action->activate();
            return;
        }

        g_search_engine = search_engine;
        Config::write_string("Browser"sv, "Preferences"sv, "SearchEngine"sv, g_search_engine);
        action.set_status_tip(search_engine);
    });
    search_engine_menu.add_action(custom_search_engine_action);
    m_search_engine_actions.add_action(custom_search_engine_action);

    if (!search_engine_set && !g_search_engine.is_empty()) {
        custom_search_engine_action->set_checked(true);
        custom_search_engine_action->set_status_tip(g_search_engine);
    }

    return {};
}

GUI::TabWidget& BrowserWindow::tab_widget()
{
    return *m_tab_widget;
}

Tab& BrowserWindow::active_tab()
{
    return verify_cast<Tab>(*tab_widget().active_widget());
}

void BrowserWindow::set_window_title_for_tab(Tab const& tab)
{
    auto& title = tab.title();
    auto url = tab.url();
    set_title(DeprecatedString::formatted("{} - Browser", title.is_empty() ? url.to_deprecated_string() : title));
}

Tab& BrowserWindow::create_new_tab(URL url, Web::HTML::ActivateTab activate)
{
    auto& new_tab = m_tab_widget->add_tab<Browser::Tab>("New tab"_short_string, *this);

    m_tab_widget->set_bar_visible(!is_fullscreen() && m_tab_widget->children().size() > 1);

    new_tab.on_title_change = [this, &new_tab](auto& title) {
        m_tab_widget->set_tab_title(new_tab, String::from_deprecated_string(title).release_value_but_fixme_should_propagate_errors());
        if (m_tab_widget->active_widget() == &new_tab)
            set_window_title_for_tab(new_tab);
    };

    new_tab.on_favicon_change = [this, &new_tab](auto& bitmap) {
        m_tab_widget->set_tab_icon(new_tab, &bitmap);
    };

    new_tab.on_tab_open_request = [this](auto& url) {
        create_new_tab(url, Web::HTML::ActivateTab::Yes);
    };

    new_tab.on_activate_tab_request = [this](auto& tab) {
        m_tab_widget->set_active_widget(&tab);
    };

    new_tab.on_tab_close_request = [this](auto& tab) {
        m_tab_widget->deferred_invoke([this, &tab] {
            m_tab_widget->remove_tab(tab);
            m_tab_widget->set_bar_visible(!is_fullscreen() && m_tab_widget->children().size() > 1);
            if (m_tab_widget->children().is_empty())
                close();
        });
    };

    new_tab.on_tab_close_other_request = [this](auto& tab) {
        m_tab_widget->deferred_invoke([this, &tab] {
            m_tab_widget->remove_all_tabs_except(tab);
            VERIFY(m_tab_widget->children().size() == 1);
            m_tab_widget->set_bar_visible(false);
        });
    };

    new_tab.on_window_open_request = [this](auto& url) {
        create_new_window(url);
    };

    new_tab.on_get_all_cookies = [this](auto& url) {
        return m_cookie_jar.get_all_cookies(url);
    };

    new_tab.on_get_named_cookie = [this](auto& url, auto& name) {
        return m_cookie_jar.get_named_cookie(url, name);
    };

    new_tab.on_get_cookie = [this](auto& url, auto source) -> DeprecatedString {
        return m_cookie_jar.get_cookie(url, source);
    };

    new_tab.on_set_cookie = [this](auto& url, auto& cookie, auto source) {
        m_cookie_jar.set_cookie(url, cookie, source);
    };

    new_tab.on_dump_cookies = [this]() {
        m_cookie_jar.dump_cookies();
    };

    new_tab.on_update_cookie = [this](auto cookie) {
        m_cookie_jar.update_cookie(move(cookie));
    };

    new_tab.on_get_cookies_entries = [this]() {
        return m_cookie_jar.get_all_cookies();
    };

    new_tab.on_get_local_storage_entries = [this]() {
        return active_tab().view().get_local_storage_entries();
    };

    new_tab.on_get_session_storage_entries = [this]() {
        return active_tab().view().get_session_storage_entries();
    };

    new_tab.on_take_screenshot = [this]() {
        return active_tab().view().take_screenshot();
    };

    new_tab.load(url);

    dbgln_if(SPAM_DEBUG, "Added new tab {:p}, loading {}", &new_tab, url);

    if (activate == Web::HTML::ActivateTab::Yes)
        m_tab_widget->set_active_widget(&new_tab);

    return new_tab;
}

void BrowserWindow::create_new_window(URL url)
{
    GUI::Process::spawn_or_show_error(this, "/bin/Browser"sv, Array { url.to_deprecated_string() });
}

void BrowserWindow::content_filters_changed()
{
    tab_widget().for_each_child_of_type<Browser::Tab>([](auto& tab) {
        tab.content_filters_changed();
        return IterationDecision::Continue;
    });
}

void BrowserWindow::proxy_mappings_changed()
{
    tab_widget().for_each_child_of_type<Browser::Tab>([](auto& tab) {
        tab.proxy_mappings_changed();
        return IterationDecision::Continue;
    });
}

void BrowserWindow::config_string_did_change(DeprecatedString const& domain, DeprecatedString const& group, DeprecatedString const& key, DeprecatedString const& value)
{
    if (domain != "Browser")
        return;

    if (group == "Preferences") {
        if (key == "SearchEngine")
            Browser::g_search_engine = value;
        else if (key == "Home")
            Browser::g_home_url = value;
        else if (key == "NewTab")
            Browser::g_new_tab_url = value;
    } else if (group.starts_with("Proxy:"sv)) {
        dbgln("Proxy mapping changed: {}/{} = {}", group, key, value);
        auto proxy_spec = group.substring_view(6);
        auto existing_proxy = Browser::g_proxies.find(proxy_spec);
        if (existing_proxy.is_end())
            Browser::g_proxies.append(proxy_spec);

        Browser::g_proxy_mappings.set(key, existing_proxy.index());
        proxy_mappings_changed();
    }

    // TODO: ColorScheme
}

void BrowserWindow::config_bool_did_change(DeprecatedString const& domain, DeprecatedString const& group, DeprecatedString const& key, bool value)
{
    dbgln("{} {} {} {}", domain, group, key, value);
    if (domain != "Browser" || group != "Preferences")
        return;

    if (key == "ShowBookmarksBar") {
        m_window_actions.show_bookmarks_bar_action().set_checked(value);
        Browser::BookmarksBarWidget::the().set_visible(value);
    } else if (key == "EnableContentFilters") {
        Browser::g_content_filters_enabled = value;
        content_filters_changed();
    }

    // NOTE: CloseDownloadWidgetOnFinish is read each time in DownloadWindow
}

void BrowserWindow::broadcast_window_position(Gfx::IntPoint position)
{
    tab_widget().for_each_child_of_type<Browser::Tab>([&](auto& tab) {
        tab.window_position_changed(position);
        return IterationDecision::Continue;
    });
}

void BrowserWindow::broadcast_window_size(Gfx::IntSize size)
{
    tab_widget().for_each_child_of_type<Browser::Tab>([&](auto& tab) {
        tab.window_size_changed(size);
        return IterationDecision::Continue;
    });
}

void BrowserWindow::event(Core::Event& event)
{
    switch (event.type()) {
    case GUI::Event::Move:
        broadcast_window_position(static_cast<GUI::MoveEvent&>(event).position());
        break;
    case GUI::Event::Resize:
        broadcast_window_size(static_cast<GUI::ResizeEvent&>(event).size());
        break;
    default:
        break;
    }

    Window::event(event);
}

ErrorOr<void> BrowserWindow::take_screenshot(ScreenshotType type)
{
    if (!active_tab().on_take_screenshot)
        return {};

    Gfx::ShareableBitmap bitmap;

    switch (type) {
    case ScreenshotType::Visible:
        bitmap = active_tab().on_take_screenshot();
        break;
    case ScreenshotType::Full:
        bitmap = active_tab().view().take_document_screenshot();
        break;
    }

    if (!bitmap.is_valid())
        return Error::from_string_view("Failed to take a screenshot of the current tab"sv);

    LexicalPath path { Core::StandardPaths::downloads_directory() };
    path = path.append(Core::DateTime::now().to_deprecated_string("screenshot-%Y-%m-%d-%H-%M-%S.png"sv));

    auto encoded = TRY(Gfx::PNGWriter::encode(*bitmap.bitmap()));

    auto screenshot_file = TRY(Core::File::open(path.string(), Core::File::OpenMode::Write));
    TRY(screenshot_file->write_until_depleted(encoded));

    return {};
}

void BrowserWindow::update_zoom_menu_text()
{
    VERIFY(m_zoom_menu);
    auto zoom_level_text = DeprecatedString::formatted("&Zoom ({}%)", round_to<int>(active_tab().view().zoom_level() * 100));
    m_zoom_menu->set_name(zoom_level_text);
}

}
