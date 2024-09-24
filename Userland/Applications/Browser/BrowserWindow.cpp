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
#include "InspectorWidget.h"
#include "Tab.h"
#include "TaskManagerWidget.h"
#include <Applications/Browser/BrowserWindowGML.h>
#include <Applications/BrowserSettings/Defaults.h>
#include <LibConfig/Client.h>
#include <LibCore/StandardPaths.h>
#include <LibDesktop/Launcher.h>
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
#include <LibWeb/CSS/PreferredColorScheme.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/AudioPlayState.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Loader/UserAgent.h>
#include <LibWebView/CookieJar.h>
#include <LibWebView/OutOfProcessWebView.h>
#include <LibWebView/SearchEngine.h>
#include <LibWebView/UserAgent.h>
#include <LibWebView/WebContentClient.h>

namespace Browser {

static ByteString bookmarks_file_path()
{
    StringBuilder builder;
    builder.append(Core::StandardPaths::config_directory());
    builder.append("/bookmarks.json"sv);
    return builder.to_byte_string();
}

BrowserWindow::BrowserWindow(WebView::CookieJar& cookie_jar, Vector<URL::URL> const& initial_urls, StringView const man_file)
    : m_cookie_jar(cookie_jar)
    , m_window_actions(*this)
{
    auto app_icon = GUI::Icon::default_icon("app-browser"sv);
    m_bookmarks_bar = Browser::BookmarksBarWidget::construct(Browser::bookmarks_file_path(), true);

    restore_size_and_position("Browser"sv, "Window"sv, { { 730, 560 } });
    save_size_and_position_on_close("Browser"sv, "Window"sv);
    set_icon(app_icon.bitmap_for_size(16));
    set_title("Browser");

    auto widget = set_main_widget<GUI::Widget>();
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
        update_displayed_zoom_level();
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
        create_new_tab(Browser::g_new_tab_url, Web::HTML::ActivateTab::Yes);
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

    bool show_bookmarks_bar = Config::read_bool("Browser"sv, "Preferences"sv, "ShowBookmarksBar"sv, Browser::default_show_bookmarks_bar);
    m_window_actions.show_bookmarks_bar_action().set_checked(show_bookmarks_bar);
    Browser::BookmarksBarWidget::the().set_visible(show_bookmarks_bar);

    m_window_actions.on_vertical_tabs = [this](auto& action) {
        m_tab_widget->set_tab_position(action.is_checked() ? TabPosition::Left : TabPosition::Top);
        Config::write_bool("Browser"sv, "Preferences"sv, "VerticalTabs"sv, action.is_checked());
    };

    bool vertical_tabs = Config::read_bool("Browser"sv, "Preferences"sv, "VerticalTabs"sv, false);
    m_window_actions.vertical_tabs_action().set_checked(vertical_tabs);
    m_tab_widget->set_tab_position(vertical_tabs ? TabPosition::Left : TabPosition::Top);

    build_menus(man_file);

    for (size_t i = 0; i < initial_urls.size(); ++i)
        create_new_tab(initial_urls[i], (i == 0) ? Web::HTML::ActivateTab::Yes : Web::HTML::ActivateTab::No);
}

void BrowserWindow::build_menus(StringView const man_file)
{
    auto file_menu = add_menu("&File"_string);
    file_menu->add_action(WindowActions::the().create_new_tab_action());
    file_menu->add_action(WindowActions::the().create_new_window_action());

    auto close_tab_action = GUI::CommonActions::make_close_tab_action([this](auto&) {
        active_tab().on_tab_close_request(active_tab());
    },
        this);
    file_menu->add_action(close_tab_action);

    file_menu->add_separator();
    file_menu->add_action(GUI::CommonActions::make_quit_action(
        [](auto&) {
            GUI::Application::the()->quit();
        },
        GUI::CommonActions::QuitAltShortcut::None));

    auto view_menu = add_menu("&View"_string);
    view_menu->add_action(WindowActions::the().show_bookmarks_bar_action());
    view_menu->add_action(WindowActions::the().vertical_tabs_action());
    view_menu->add_separator();
    m_zoom_menu = view_menu->add_submenu("&Zoom"_string);
    m_zoom_menu->add_action(GUI::CommonActions::make_zoom_in_action(
        [this](auto&) {
            auto& tab = active_tab();
            tab.view().zoom_in();
            update_displayed_zoom_level();
        },
        this));
    m_zoom_menu->add_action(GUI::CommonActions::make_zoom_out_action(
        [this](auto&) {
            auto& tab = active_tab();
            tab.view().zoom_out();
            update_displayed_zoom_level();
        },
        this));
    m_zoom_menu->add_action(GUI::CommonActions::make_reset_zoom_action(
        [this](auto&) {
            auto& tab = active_tab();
            tab.view().reset_zoom();
            update_displayed_zoom_level();
        },
        this));
    view_menu->add_separator();
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action(
        [this](auto&) {
            auto& tab = active_tab();
            set_fullscreen(!is_fullscreen());

            auto is_fullscreen = this->is_fullscreen();
            tab_widget().set_bar_visible(!is_fullscreen && tab_widget().children().size() > 1);
            tab.m_toolbar_container->set_visible(!is_fullscreen);
            tab.m_statusbar->set_visible(!is_fullscreen);

            if (is_fullscreen)
                tab.view().set_frame_style(Gfx::FrameStyle::NoFrame);
            else
                tab.view().set_frame_style(Gfx::FrameStyle::SunkenContainer);
        },
        this));

    m_go_back_action = GUI::CommonActions::make_go_back_action([this](auto&) { active_tab().go_back(); }, this);
    m_go_forward_action = GUI::CommonActions::make_go_forward_action([this](auto&) { active_tab().go_forward(); }, this);
    m_go_home_action = GUI::CommonActions::make_go_home_action([this](auto&) { active_tab().load(g_home_url); }, this);
    m_go_home_action->set_status_tip("Go to home page"_string);
    m_reload_action = GUI::CommonActions::make_reload_action([this](auto&) { active_tab().reload(); }, this);
    m_reload_action->set_status_tip("Reload current page"_string);

    auto go_menu = add_menu("&Go"_string);
    go_menu->add_action(*m_go_back_action);
    go_menu->add_action(*m_go_forward_action);
    go_menu->add_action(*m_go_home_action);
    go_menu->add_separator();
    go_menu->add_action(*m_reload_action);

    m_copy_selection_action = GUI::CommonActions::make_copy_action([this](auto&) {
        auto& tab = active_tab();
        auto selected_text = tab.view().selected_text();
        if (!selected_text.is_empty())
            GUI::Clipboard::the().set_plain_text(selected_text);
    });

    m_paste_action = GUI::CommonActions::make_paste_action([this](auto&) {
        auto& tab = active_tab();

        auto [data, mime_type, metadata] = GUI::Clipboard::the().fetch_data_and_type();
        if (data.is_empty() || !mime_type.starts_with("text/"sv))
            return;

        tab.view().paste(MUST(String::from_utf8(StringView { data })));
    });

    m_select_all_action = GUI::CommonActions::make_select_all_action([this](auto&) {
        active_tab().view().select_all();
    });

    m_view_source_action = GUI::Action::create(
        "View &Source", { Mod_Ctrl, Key_U }, g_icon_bag.code, [this](auto&) {
            active_tab().view().get_source();
        },
        this);
    m_view_source_action->set_status_tip("View source code of the current page"_string);

    m_inspect_dom_tree_action = GUI::Action::create(
        "Inspect &DOM Tree", { Mod_Ctrl | Mod_Shift, Key_I }, { Mod_None, Key_F12 }, g_icon_bag.dom_tree, [this](auto&) {
            active_tab().show_inspector_window(Tab::InspectorTarget::Document);
        },
        this);
    m_inspect_dom_tree_action->set_status_tip("Open inspector window for this page"_string);

    m_inspect_dom_node_action = GUI::Action::create(
        "&Inspect Element", g_icon_bag.inspect, [this](auto&) {
            active_tab().show_inspector_window(Tab::InspectorTarget::HoveredElement);
        },
        this);
    m_inspect_dom_node_action->set_status_tip("Open inspector for this element"_string);

    m_task_manager_action = GUI::Action::create(
        "Task &Manager", g_icon_bag.task_manager, [this](auto&) {
            show_task_manager_window();
        },
        this);

    auto inspect_menu = add_menu("&Inspect"_string);
    inspect_menu->add_action(*m_view_source_action);
    inspect_menu->add_action(*m_inspect_dom_tree_action);
    inspect_menu->add_action(*m_task_manager_action);

    auto storage_window_action = GUI::Action::create(
        "Open S&torage Inspector", g_icon_bag.cookie, [this](auto&) {
            active_tab().show_storage_inspector();
        },
        this);
    storage_window_action->set_status_tip("Show Storage inspector for this page"_string);
    inspect_menu->add_action(storage_window_action);

    auto history_window_action = GUI::Action::create(
        "Open &History Window", g_icon_bag.history, [this](auto&) {
            active_tab().show_history_inspector();
        },
        this);
    storage_window_action->set_status_tip("Show History inspector for this tab"_string);
    inspect_menu->add_action(history_window_action);

    auto settings_menu = add_menu("&Settings"_string);

    m_change_homepage_action = GUI::Action::create(
        "Set Homepage URL...", g_icon_bag.go_home, [this](auto&) {
            String homepage_url = String::from_byte_string(Config::read_string("Browser"sv, "Preferences"sv, "Home"sv, Browser::default_homepage_url)).release_value_but_fixme_should_propagate_errors();
            if (GUI::InputBox::show(this, homepage_url, "Enter a URL:"sv, "Change Homepage"sv) == GUI::InputBox::ExecResult::OK) {
                if (URL::URL(homepage_url).is_valid()) {
                    Config::write_string("Browser"sv, "Preferences"sv, "Home"sv, homepage_url);
                    Browser::g_home_url = homepage_url.to_byte_string();
                } else {
                    GUI::MessageBox::show_error(this, "The URL you have entered is not valid"sv);
                }
            }
        },
        this);

    settings_menu->add_action(*m_change_homepage_action);

    auto load_search_engines_result = load_search_engines(settings_menu);
    if (load_search_engines_result.is_error()) {
        dbgln("Failed to open search-engines file: {}", load_search_engines_result.error());
    }

    auto color_scheme_menu = settings_menu->add_submenu("&Color Scheme"_string);
    color_scheme_menu->set_icon(g_icon_bag.color_chooser);
    {
        auto current_setting = Web::CSS::preferred_color_scheme_from_string(Config::read_string("Browser"sv, "Preferences"sv, "ColorScheme"sv, Browser::default_color_scheme));
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
            color_scheme_menu->add_action(action);
            m_color_scheme_actions.add_action(action);
        };

        add_color_scheme_action("Follow System Theme", Web::CSS::PreferredColorScheme::Auto);
        add_color_scheme_action("Light", Web::CSS::PreferredColorScheme::Light);
        add_color_scheme_action("Dark", Web::CSS::PreferredColorScheme::Dark);
    }

    settings_menu->add_separator();
    auto open_settings_action = GUI::Action::create("Browser &Settings", Gfx::Bitmap::load_from_file("/res/icons/16x16/settings.png"sv).release_value_but_fixme_should_propagate_errors(),
        [this](auto&) {
            GUI::Process::spawn_or_show_error(this, "/bin/BrowserSettings"sv);
        });
    settings_menu->add_action(move(open_settings_action));

    auto debug_menu = add_menu("&Debug"_string);
    debug_menu->add_action(GUI::Action::create(
        "Dump &DOM Tree", g_icon_bag.dom_tree, [this](auto&) {
            active_tab().view().debug_request("dump-dom-tree");
        },
        this));
    debug_menu->add_action(GUI::Action::create(
        "Dump &Layout Tree", g_icon_bag.layout, [this](auto&) {
            active_tab().view().debug_request("dump-layout-tree");
        },
        this));
    debug_menu->add_action(GUI::Action::create(
        "Dump &Paint Tree", g_icon_bag.layout, [this](auto&) {
            active_tab().view().debug_request("dump-paint-tree");
        },
        this));
    debug_menu->add_action(GUI::Action::create(
        "Dump S&tacking Context Tree", g_icon_bag.layers, [this](auto&) {
            active_tab().view().debug_request("dump-stacking-context-tree");
        },
        this));
    debug_menu->add_action(GUI::Action::create(
        "Dump &Style Sheets", g_icon_bag.filetype_css, [this](auto&) {
            active_tab().view().debug_request("dump-style-sheets");
        },
        this));
    debug_menu->add_action(GUI::Action::create(
        "Dump &All Resolved Styles", g_icon_bag.filetype_css, [this](auto&) {
            active_tab().view().debug_request("dump-all-resolved-styles");
        },
        this));
    debug_menu->add_action(GUI::Action::create("Dump &History", { Mod_Ctrl, Key_H }, g_icon_bag.history, [this](auto&) {
        active_tab().view().debug_request("dump-session-history");
    }));
    debug_menu->add_action(GUI::Action::create("Dump C&ookies", g_icon_bag.cookie, [this](auto&) {
        m_cookie_jar.dump_cookies();
    }));
    debug_menu->add_action(GUI::Action::create("Dump Loc&al Storage", g_icon_bag.local_storage, [this](auto&) {
        active_tab().view().debug_request("dump-local-storage");
    }));
    debug_menu->add_separator();
    auto line_box_borders_action = GUI::Action::create_checkable(
        "Line &Box Borders", [this](auto& action) {
            active_tab().view().debug_request("set-line-box-borders", action.is_checked() ? "on" : "off");
        },
        this);
    line_box_borders_action->set_checked(false);
    debug_menu->add_action(line_box_borders_action);

    debug_menu->add_separator();
    debug_menu->add_action(GUI::Action::create("Collect &Garbage", { Mod_Ctrl | Mod_Shift, Key_G }, g_icon_bag.trash_can, [this](auto&) {
        active_tab().view().debug_request("collect-garbage");
    }));
    debug_menu->add_action(GUI::Action::create("Clear &Cache", { Mod_Ctrl | Mod_Shift, Key_C }, g_icon_bag.clear_cache, [this](auto&) {
        active_tab().view().debug_request("clear-cache");
    }));

    m_user_agent_spoof_actions.set_exclusive(true);
    auto spoof_user_agent_menu = debug_menu->add_submenu("Spoof &User Agent"_string);
    m_disable_user_agent_spoofing = GUI::Action::create_checkable("Disabled", [this](auto&) {
        active_tab().view().debug_request("spoof-user-agent", Web::default_user_agent);
    });
    m_disable_user_agent_spoofing->set_status_tip(String::from_utf8(Web::default_user_agent).release_value_but_fixme_should_propagate_errors());
    spoof_user_agent_menu->add_action(*m_disable_user_agent_spoofing);
    spoof_user_agent_menu->set_icon(g_icon_bag.spoof);
    m_user_agent_spoof_actions.add_action(*m_disable_user_agent_spoofing);
    m_disable_user_agent_spoofing->set_checked(true);

    auto add_user_agent = [this, &spoof_user_agent_menu](auto& name, auto user_agent) {
        auto action = GUI::Action::create_checkable(name, [this, user_agent](auto&) {
            active_tab().view().debug_request("spoof-user-agent", user_agent);
        });
        action->set_status_tip(String::from_utf8(user_agent).release_value_but_fixme_should_propagate_errors());
        spoof_user_agent_menu->add_action(action);
        m_user_agent_spoof_actions.add_action(action);
    };
    for (auto const& user_agent : WebView::user_agents)
        add_user_agent(user_agent.key, user_agent.value);

    auto custom_user_agent = GUI::Action::create_checkable("Custom...", [this](auto& action) {
        String user_agent;
        if (GUI::InputBox::show(this, user_agent, "Enter User Agent:"sv, "Custom User Agent"sv, GUI::InputType::NonemptyText) != GUI::InputBox::ExecResult::OK) {
            m_disable_user_agent_spoofing->activate();
            return;
        }
        active_tab().view().debug_request("spoof-user-agent", user_agent.to_byte_string());
        action.set_status_tip(user_agent);
    });
    spoof_user_agent_menu->add_action(custom_user_agent);
    m_user_agent_spoof_actions.add_action(custom_user_agent);

    debug_menu->add_separator();
    auto scripting_enabled_action = GUI::Action::create_checkable(
        "Enable Scripting", [this](auto& action) {
            active_tab().view().debug_request("scripting", action.is_checked() ? "on" : "off");
        },
        this);
    scripting_enabled_action->set_checked(true);
    debug_menu->add_action(scripting_enabled_action);

    auto block_pop_ups_action = GUI::Action::create_checkable(
        "Block Pop-ups", [this](auto& action) {
            active_tab().view().debug_request("block-pop-ups", action.is_checked() ? "on" : "off");
        },
        this);
    block_pop_ups_action->set_checked(true);
    debug_menu->add_action(block_pop_ups_action);

    auto same_origin_policy_action = GUI::Action::create_checkable(
        "Enable Same-Origin &Policy", [this](auto& action) {
            active_tab().view().debug_request("same-origin-policy", action.is_checked() ? "on" : "off");
        },
        this);
    same_origin_policy_action->set_checked(false);
    debug_menu->add_action(same_origin_policy_action);

    auto help_menu = add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(this));
    help_menu->add_action(GUI::CommonActions::make_help_action([man_file](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme(man_file), "/bin/Help");
    }));
    help_menu->add_action(WindowActions::the().about_action());
}

ErrorOr<void> BrowserWindow::load_search_engines(GUI::Menu& settings_menu)
{
    m_search_engine_actions.set_exclusive(true);
    auto search_engine_menu = settings_menu.add_submenu("&Search Engine"_string);
    search_engine_menu->set_icon(g_icon_bag.find);
    bool search_engine_set = false;

    m_disable_search_engine_action = GUI::Action::create_checkable(
        "Disable", [](auto&) {
            g_search_engine = {};
            Config::write_string("Browser"sv, "Preferences"sv, "SearchEngine"sv, g_search_engine);
        },
        this);
    search_engine_menu->add_action(*m_disable_search_engine_action);
    m_search_engine_actions.add_action(*m_disable_search_engine_action);
    m_disable_search_engine_action->set_checked(true);

    for (auto [name, url_format] : WebView::search_engines()) {
        auto action = GUI::Action::create_checkable(
            name, [&, url_format](auto&) {
                g_search_engine = url_format;
                Config::write_string("Browser"sv, "Preferences"sv, "SearchEngine"sv, g_search_engine);
            },
            this);
        search_engine_menu->add_action(action);
        m_search_engine_actions.add_action(action);

        if (g_search_engine == url_format) {
            action->set_checked(true);
            search_engine_set = true;
        }

        action->set_status_tip(TRY(String::from_utf8(url_format)));
    }

    auto custom_search_engine_action = GUI::Action::create_checkable("Custom...", [&](auto& action) {
        String search_engine;
        if (GUI::InputBox::show(this, search_engine, "Enter URL template:"sv, "Custom Search Engine"sv, GUI::InputType::NonemptyText, "https://host/search?q={}"sv) != GUI::InputBox::ExecResult::OK) {
            m_disable_search_engine_action->activate();
            return;
        }

        auto argument_count = AK::StringUtils::count(search_engine, "{}"sv);
        if (argument_count != 1) {
            GUI::MessageBox::show(this, "Invalid format, must contain '{}' once!"sv, "Error"sv, GUI::MessageBox::Type::Error);
            m_disable_search_engine_action->activate();
            return;
        }

        g_search_engine = search_engine.to_byte_string();
        Config::write_string("Browser"sv, "Preferences"sv, "SearchEngine"sv, g_search_engine);
        action.set_status_tip(search_engine);
    });
    search_engine_menu->add_action(custom_search_engine_action);
    m_search_engine_actions.add_action(custom_search_engine_action);

    if (!search_engine_set && !g_search_engine.is_empty()) {
        custom_search_engine_action->set_checked(true);
        custom_search_engine_action->set_status_tip(TRY(String::from_byte_string(g_search_engine)));
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
    set_title(ByteString::formatted("{} - Browser", title.is_empty() ? url.to_byte_string() : title));
}

Tab& BrowserWindow::create_new_tab(URL::URL const& url, Web::HTML::ActivateTab activate)
{
    auto& new_tab = m_tab_widget->add_tab<Browser::Tab>("New tab"_string, *this);

    m_tab_widget->set_bar_visible(!is_fullscreen() && m_tab_widget->children().size() > 1);
    m_tab_widget->set_tab_icon(new_tab, new_tab.icon());

    new_tab.on_title_change = [this, &new_tab](auto& title) {
        m_tab_widget->set_tab_title(new_tab, String::from_byte_string(title).release_value_but_fixme_should_propagate_errors());
        if (m_tab_widget->active_widget() == &new_tab)
            set_window_title_for_tab(new_tab);
    };

    new_tab.on_favicon_change = [this, &new_tab](auto& bitmap) {
        m_tab_widget->set_tab_icon(new_tab, &bitmap);
    };

    new_tab.view().on_audio_play_state_changed = [this, &new_tab](auto play_state) {
        switch (play_state) {
        case Web::HTML::AudioPlayState::Paused:
            m_tab_widget->set_tab_action_icon(new_tab, nullptr);
            break;

        case Web::HTML::AudioPlayState::Playing:
            m_tab_widget->set_tab_action_icon(new_tab, g_icon_bag.unmute);
            break;
        }
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

    new_tab.view().on_get_all_cookies = [this](auto& url) {
        return m_cookie_jar.get_all_cookies(url);
    };

    new_tab.view().on_get_named_cookie = [this](auto& url, auto& name) {
        return m_cookie_jar.get_named_cookie(url, name);
    };

    new_tab.view().on_get_cookie = [this](auto& url, auto source) {
        return m_cookie_jar.get_cookie(url, source);
    };

    new_tab.view().on_set_cookie = [this](auto& url, auto& cookie, auto source) {
        m_cookie_jar.set_cookie(url, cookie, source);
    };

    new_tab.view().on_update_cookie = [this](auto const& cookie) {
        m_cookie_jar.update_cookie(cookie);
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

    new_tab.load(url);

    dbgln_if(SPAM_DEBUG, "Added new tab {:p}, loading {}", &new_tab, url);

    if (activate == Web::HTML::ActivateTab::Yes)
        m_tab_widget->set_active_widget(&new_tab);

    return new_tab;
}

void BrowserWindow::create_new_window(URL::URL const& url)
{
    GUI::Process::spawn_or_show_error(this, "/bin/Browser"sv, Array { url.to_byte_string() });
}

void BrowserWindow::content_filters_changed()
{
    tab_widget().for_each_child_of_type<Browser::Tab>([](auto& tab) {
        tab.content_filters_changed();
        return IterationDecision::Continue;
    });
}

void BrowserWindow::autoplay_allowlist_changed()
{
    tab_widget().for_each_child_of_type<Browser::Tab>([](auto& tab) {
        tab.autoplay_allowlist_changed();
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

void BrowserWindow::config_string_did_change(StringView domain, StringView group, StringView key, StringView value)
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

void BrowserWindow::config_bool_did_change(StringView domain, StringView group, StringView key, bool value)
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
    } else if (key == "AllowAutoplayOnAllWebsites") {
        Browser::g_autoplay_allowed_on_all_websites = value;
        autoplay_allowlist_changed();
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
    case GUI::Event::WindowCloseRequest:
        // FIXME: If we have multiple browser windows, this won't be correct anymore
        //     For now, this makes sure that we close the TaskManagerWindow when the user clicks the (X) button
        close_task_manager_window();
        break;
    default:
        break;
    }

    Window::event(event);
}

void BrowserWindow::update_zoom_menu()
{
    VERIFY(m_zoom_menu);
    auto zoom_level_text = String::formatted("&Zoom ({}%)", round_to<int>(active_tab().view().zoom_level() * 100)).release_value_but_fixme_should_propagate_errors();
    m_zoom_menu->set_name(zoom_level_text);
}

void BrowserWindow::update_displayed_zoom_level()
{
    active_tab().update_reset_zoom_button();
    update_zoom_menu();
}

void BrowserWindow::show_task_manager_window()
{
    if (!m_task_manager_window) {
        m_task_manager_window = GUI::Window::construct();
        m_task_manager_window->set_window_mode(GUI::WindowMode::Modeless);
        m_task_manager_window->resize(600, 400);
        m_task_manager_window->set_title("Task Manager");

        (void)m_task_manager_window->set_main_widget<TaskManagerWidget>();
    }

    m_task_manager_window->show();
    m_task_manager_window->move_to_front();
}

void BrowserWindow::close_task_manager_window()
{
    if (m_task_manager_window) {
        m_task_manager_window->close();
    }
}

}
