/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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
#include <Applications/Browser/BrowserWindowGML.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/Widget.h>
#include <LibJS/Interpreter.h>
#include <LibWeb/Dump.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/OutOfProcessWebView.h>

namespace Browser {

static String bookmarks_file_path()
{
    StringBuilder builder;
    builder.append(Core::StandardPaths::config_directory());
    builder.append("/bookmarks.json");
    return builder.to_string();
}

BrowserWindow::BrowserWindow(CookieJar& cookie_jar, URL url)
    : m_cookie_jar(cookie_jar)
    , m_window_actions(*this)
{
    auto app_icon = GUI::Icon::default_icon("app-browser");
    m_bookmarks_bar = Browser::BookmarksBarWidget::construct(Browser::bookmarks_file_path(), true);

    resize(640, 480);
    set_icon(app_icon.bitmap_for_size(16));
    set_title("Browser");

    auto& widget = set_main_widget<GUI::Widget>();
    widget.load_from_gml(browser_window_gml);

    auto& top_line = *widget.find_descendant_of_type_named<GUI::HorizontalSeparator>("top_line");

    m_tab_widget = *widget.find_descendant_of_type_named<GUI::TabWidget>("tab_widget");

    m_tab_widget->on_tab_count_change = [&top_line](size_t tab_count) {
        top_line.set_visible(tab_count > 1);
    };

    m_tab_widget->on_change = [this](auto& active_widget) {
        auto& tab = static_cast<Browser::Tab&>(active_widget);
        set_window_title_for_tab(tab);
        tab.did_become_active();
    };

    m_tab_widget->on_middle_click = [](auto& clicked_widget) {
        auto& tab = static_cast<Browser::Tab&>(clicked_widget);
        tab.on_tab_close_request(tab);
    };

    m_tab_widget->on_context_menu_request = [](auto& clicked_widget, const GUI::ContextMenuEvent& context_menu_event) {
        auto& tab = static_cast<Browser::Tab&>(clicked_widget);
        tab.context_menu_requested(context_menu_event.screen_position());
    };

    m_window_actions.on_create_new_tab = [this] {
        create_new_tab(Browser::g_home_url, true);
    };

    m_window_actions.on_next_tab = [this] {
        m_tab_widget->activate_next_tab();
    };

    m_window_actions.on_previous_tab = [this] {
        m_tab_widget->activate_previous_tab();
    };

    m_window_actions.on_about = [this] {
        auto app_icon = GUI::Icon::default_icon("app-browser");
        GUI::AboutDialog::show("Browser", app_icon.bitmap_for_size(32), this);
    };

    m_window_actions.on_show_bookmarks_bar = [](auto& action) {
        Browser::BookmarksBarWidget::the().set_visible(action.is_checked());
    };

    m_window_actions.show_bookmarks_bar_action().set_checked(true);

    build_menus();

    create_new_tab(move(url), true);
}

BrowserWindow::~BrowserWindow()
{
}

void BrowserWindow::build_menus()
{
    auto menubar = GUI::Menubar::construct();

    auto& file_menu = menubar->add_menu("&File");
    file_menu.add_action(WindowActions::the().create_new_tab_action());

    auto close_tab_action = GUI::Action::create(
        "&Close Tab", { Mod_Ctrl, Key_W }, Gfx::Bitmap::load_from_file("/res/icons/16x16/close-tab.png"), [this](auto&) {
            active_tab().on_tab_close_request(active_tab());
        },
        this);
    close_tab_action->set_status_tip("Close current tab");
    file_menu.add_action(close_tab_action);

    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& view_menu = menubar->add_menu("&View");
    view_menu.add_action(WindowActions::the().show_bookmarks_bar_action());
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
    m_go_home_action = GUI::CommonActions::make_go_home_action([this](auto&) { active_tab().load(g_home_url); }, this);
    m_go_home_action->set_status_tip("Go to home page");
    m_reload_action = GUI::CommonActions::make_reload_action([this](auto&) { active_tab().reload(); }, this);
    m_reload_action->set_status_tip("Reload current page");

    auto& go_menu = menubar->add_menu("&Go");
    go_menu.add_action(*m_go_back_action);
    go_menu.add_action(*m_go_forward_action);
    go_menu.add_action(*m_go_home_action);
    go_menu.add_separator();
    go_menu.add_action(*m_reload_action);

    m_view_source_action = GUI::Action::create(
        "View &Source", { Mod_Ctrl, Key_U }, [this](auto&) {
            auto& tab = active_tab();
            if (tab.m_type == Tab::Type::InProcessWebView) {
                VERIFY(tab.m_page_view->document());
                auto url = tab.m_page_view->document()->url();
                auto source = tab.m_page_view->document()->source();
                tab.view_source(url, source);
            } else {
                tab.m_web_content_view->get_source();
            }
        },
        this);
    m_view_source_action->set_status_tip("View source code of the current page");

    m_inspect_dom_tree_action = GUI::Action::create(
        "Inspect &DOM Tree", { Mod_None, Key_F12 }, [this](auto&) {
            auto& tab = active_tab();
            if (tab.m_type == Tab::Type::InProcessWebView) {
                if (!tab.m_dom_inspector_window) {
                    tab.m_dom_inspector_window = GUI::Window::construct(this);
                    tab.m_dom_inspector_window->resize(300, 500);
                    tab.m_dom_inspector_window->set_title("DOM inspector");
                    tab.m_dom_inspector_window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object.png"));
                    tab.m_dom_inspector_window->set_main_widget<InspectorWidget>();
                }
                auto* inspector_widget = static_cast<InspectorWidget*>(tab.m_dom_inspector_window->main_widget());
                inspector_widget->set_document(tab.m_page_view->document());
                tab.m_dom_inspector_window->show();
                tab.m_dom_inspector_window->move_to_front();
            } else {
                TODO();
            }
        },
        this);
    m_inspect_dom_tree_action->set_status_tip("Open DOM inspector window for this page");

    auto& inspect_menu = menubar->add_menu("&Inspect");
    inspect_menu.add_action(*m_view_source_action);
    inspect_menu.add_action(*m_inspect_dom_tree_action);

    auto js_console_action = GUI::Action::create(
        "Open &JS Console", { Mod_Ctrl, Key_I }, [this](auto&) {
            auto& tab = active_tab();
            if (tab.m_type == Tab::Type::InProcessWebView) {
                if (!tab.m_console_window) {
                    tab.m_console_window = GUI::Window::construct(this);
                    tab.m_console_window->resize(500, 300);
                    tab.m_console_window->set_title("JS Console");
                    tab.m_console_window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-javascript.png"));
                    tab.m_console_window->set_main_widget<ConsoleWidget>();
                }
                auto* console_widget = static_cast<ConsoleWidget*>(tab.m_console_window->main_widget());
                console_widget->set_interpreter(tab.m_page_view->document()->interpreter().make_weak_ptr());
                tab.m_console_window->show();
                tab.m_console_window->move_to_front();
            } else {
                if (!tab.m_console_window) {
                    tab.m_console_window = GUI::Window::construct(this);
                    tab.m_console_window->resize(500, 300);
                    tab.m_console_window->set_title("JS Console");
                    tab.m_console_window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-javascript.png"));
                    tab.m_console_window->set_main_widget<ConsoleWidget>();
                }
                auto* console_widget = static_cast<ConsoleWidget*>(tab.m_console_window->main_widget());
                console_widget->on_js_input = [&tab](const String& js_source) {
                    tab.m_web_content_view->js_console_input(js_source);
                };
                console_widget->clear_output();
                tab.m_web_content_view->js_console_initialize();
                tab.m_console_window->show();
                tab.m_console_window->move_to_front();
            }
        },
        this);
    js_console_action->set_status_tip("Open JavaScript console for this page");
    inspect_menu.add_action(js_console_action);

    auto& settings_menu = menubar->add_menu("&Settings");

    m_search_engine_actions.set_exclusive(true);
    auto& search_engine_menu = settings_menu.add_submenu("&Search Engine");

    bool search_engine_set = false;
    auto add_search_engine = [&](auto& name, auto& url_format) {
        auto action = GUI::Action::create_checkable(
            name, [&](auto&) {
                g_search_engine = url_format;
                auto config = Core::ConfigFile::get_for_app("Browser");
                config->write_entry("Preferences", "SearchEngine", g_search_engine);
            },
            this);
        search_engine_menu.add_action(action);
        m_search_engine_actions.add_action(action);

        if (g_search_engine == url_format) {
            action->set_checked(true);
            search_engine_set = true;
        }
        action->set_status_tip(url_format);
    };

    m_disable_search_engine_action = GUI::Action::create_checkable(
        "Disable", [this](auto&) {
            g_search_engine = {};
            auto config = Core::ConfigFile::get_for_app("Browser");
            config->write_entry("Preferences", "SearchEngine", g_search_engine);
        },
        this);
    search_engine_menu.add_action(*m_disable_search_engine_action);
    m_search_engine_actions.add_action(*m_disable_search_engine_action);
    m_disable_search_engine_action->set_checked(true);

    add_search_engine("Bing", "https://www.bing.com/search?q={}");
    add_search_engine("DuckDuckGo", "https://duckduckgo.com/?q={}");
    add_search_engine("FrogFind", "http://frogfind.com/?q={}");
    add_search_engine("GitHub", "https://github.com/search?q={}");
    add_search_engine("Google", "https://google.com/search?q={}");
    add_search_engine("Yandex", "https://yandex.com/search/?text={}");

    auto custom_search_engine_action = GUI::Action::create_checkable("Custom", [&](auto& action) {
        String search_engine;
        if (GUI::InputBox::show(this, search_engine, "Enter URL template:", "Custom Search Engine") != GUI::InputBox::ExecOK || search_engine.is_empty()) {
            m_disable_search_engine_action->activate();
            return;
        }

        int argument_count = search_engine.replace("{}", "{}", true);
        if (argument_count != 1) {
            GUI::MessageBox::show(this, "Invalid format, must contain '{}' once!", "Error", GUI::MessageBox::Type::Error);
            m_disable_search_engine_action->activate();
            return;
        }

        g_search_engine = search_engine;
        auto config = Core::ConfigFile::get_for_app("Browser");
        config->write_entry("Preferences", "SearchEngine", g_search_engine);
        action.set_status_tip(search_engine);
    });
    search_engine_menu.add_action(custom_search_engine_action);
    m_search_engine_actions.add_action(custom_search_engine_action);

    if (!search_engine_set && !g_search_engine.is_empty()) {
        custom_search_engine_action->set_checked(true);
        custom_search_engine_action->set_status_tip(g_search_engine);
    }

    auto& debug_menu = menubar->add_menu("&Debug");
    debug_menu.add_action(GUI::Action::create(
        "Dump &DOM Tree", [this](auto&) {
            auto& tab = active_tab();
            if (tab.m_type == Tab::Type::InProcessWebView) {
                Web::dump_tree(*tab.m_page_view->document());
            } else {
                tab.m_web_content_view->debug_request("dump-dom-tree");
            }
        },
        this));
    debug_menu.add_action(GUI::Action::create(
        "Dump &Layout Tree", [this](auto&) {
            auto& tab = active_tab();
            if (tab.m_type == Tab::Type::InProcessWebView) {
                Web::dump_tree(*tab.m_page_view->document()->layout_node());
            } else {
                tab.m_web_content_view->debug_request("dump-layout-tree");
            }
        },
        this));
    debug_menu.add_action(GUI::Action::create(
        "Dump &Style Sheets", [this](auto&) {
            auto& tab = active_tab();
            if (tab.m_type == Tab::Type::InProcessWebView) {
                for (auto& sheet : tab.m_page_view->document()->style_sheets().sheets()) {
                    Web::dump_sheet(sheet);
                }
            } else {
                tab.m_web_content_view->debug_request("dump-style-sheets");
            }
        },
        this));
    debug_menu.add_action(GUI::Action::create("Dump &History", { Mod_Ctrl, Key_H }, [this](auto&) {
        active_tab().m_history.dump();
    }));
    debug_menu.add_action(GUI::Action::create("Dump C&ookies", [this](auto&) {
        auto& tab = active_tab();
        if (tab.on_dump_cookies)
            tab.on_dump_cookies();
    }));
    debug_menu.add_separator();
    auto line_box_borders_action = GUI::Action::create_checkable(
        "Line &Box Borders", [this](auto& action) {
            auto& tab = active_tab();
            if (tab.m_type == Tab::Type::InProcessWebView) {
                tab.m_page_view->set_should_show_line_box_borders(action.is_checked());
                tab.m_page_view->update();
            } else {
                tab.m_web_content_view->debug_request("set-line-box-borders", action.is_checked() ? "on" : "off");
            }
        },
        this);
    line_box_borders_action->set_checked(false);
    debug_menu.add_action(line_box_borders_action);

    debug_menu.add_separator();
    debug_menu.add_action(GUI::Action::create("Collect &Garbage", { Mod_Ctrl | Mod_Shift, Key_G }, [this](auto&) {
        auto& tab = active_tab();
        if (tab.m_type == Tab::Type::InProcessWebView) {
            if (auto* document = tab.m_page_view->document()) {
                document->interpreter().heap().collect_garbage(JS::Heap::CollectionType::CollectGarbage, true);
            }
        } else {
            tab.m_web_content_view->debug_request("collect-garbage");
        }
    }));
    debug_menu.add_action(GUI::Action::create("Clear &Cache", { Mod_Ctrl | Mod_Shift, Key_C }, [this](auto&) {
        auto& tab = active_tab();
        if (tab.m_type == Tab::Type::InProcessWebView) {
            Web::ResourceLoader::the().clear_cache();
        } else {
            tab.m_web_content_view->debug_request("clear-cache");
        }
    }));

    m_user_agent_spoof_actions.set_exclusive(true);
    auto& spoof_user_agent_menu = debug_menu.add_submenu("Spoof &User Agent");
    m_disable_user_agent_spoofing = GUI::Action::create_checkable("Disabled", [this](auto&) {
        auto& tab = active_tab();
        if (tab.m_type == Tab::Type::InProcessWebView) {
            Web::ResourceLoader::the().set_user_agent(Web::default_user_agent);
        } else {
            tab.m_web_content_view->debug_request("spoof-user-agent", Web::default_user_agent);
        }
    });
    m_disable_user_agent_spoofing->set_status_tip(Web::default_user_agent);
    spoof_user_agent_menu.add_action(*m_disable_user_agent_spoofing);
    m_user_agent_spoof_actions.add_action(*m_disable_user_agent_spoofing);
    m_disable_user_agent_spoofing->set_checked(true);

    auto add_user_agent = [this, &spoof_user_agent_menu](auto& name, auto& user_agent) {
        auto action = GUI::Action::create_checkable(name, [this, user_agent](auto&) {
            auto& tab = active_tab();
            if (tab.m_type == Tab::Type::InProcessWebView) {
                Web::ResourceLoader::the().set_user_agent(user_agent);
            } else {
                tab.m_web_content_view->debug_request("spoof-user-agent", user_agent);
            }
        });
        action->set_status_tip(user_agent);
        spoof_user_agent_menu.add_action(action);
        m_user_agent_spoof_actions.add_action(action);
    };
    add_user_agent("Chrome Linux Desktop", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.128 Safari/537.36");
    add_user_agent("Firefox Linux Desktop", "Mozilla/5.0 (X11; Linux i686; rv:87.0) Gecko/20100101 Firefox/87.0");
    add_user_agent("Safari macOS Desktop", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_2_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.3 Safari/605.1.15");
    add_user_agent("Chrome Android Mobile", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.66 Mobile Safari/537.36");
    add_user_agent("Firefox Android Mobile", "Mozilla/5.0 (Android 11; Mobile; rv:68.0) Gecko/68.0 Firefox/86.0");
    add_user_agent("Safari iOS Mobile", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_4_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1");

    auto custom_user_agent = GUI::Action::create_checkable("Custom", [this](auto& action) {
        auto& tab = active_tab();
        String user_agent;
        if (GUI::InputBox::show(this, user_agent, "Enter User Agent:", "Custom User Agent") != GUI::InputBox::ExecOK || user_agent.is_empty() || user_agent.is_null()) {
            m_disable_user_agent_spoofing->activate();
            return;
        }
        if (tab.m_type == Tab::Type::InProcessWebView) {
            Web::ResourceLoader::the().set_user_agent(user_agent);
        } else {
            tab.m_web_content_view->debug_request("spoof-user-agent", user_agent);
        }
        action.set_status_tip(user_agent);
    });
    spoof_user_agent_menu.add_action(custom_user_agent);
    m_user_agent_spoof_actions.add_action(custom_user_agent);

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(WindowActions::the().about_action());

    set_menubar(move(menubar));
}

GUI::TabWidget& BrowserWindow::tab_widget()
{
    return *m_tab_widget;
}

Tab& BrowserWindow::active_tab()
{
    return downcast<Tab>(*tab_widget().active_widget());
}

void BrowserWindow::set_window_title_for_tab(Tab const& tab)
{
    auto& title = tab.title();
    auto url = tab.url();
    set_title(String::formatted("{} - Browser", title.is_empty() ? url.to_string() : title));
}

void BrowserWindow::create_new_tab(URL url, bool activate)
{
    auto type = Browser::g_single_process ? Browser::Tab::Type::InProcessWebView : Browser::Tab::Type::OutOfProcessWebView;
    auto& new_tab = m_tab_widget->add_tab<Browser::Tab>("New tab", *this, type);

    m_tab_widget->set_bar_visible(!is_fullscreen() && m_tab_widget->children().size() > 1);

    auto default_favicon = Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-html.png");
    VERIFY(default_favicon);
    m_tab_widget->set_tab_icon(new_tab, default_favicon);

    new_tab.on_title_change = [this, &new_tab](auto& title) {
        m_tab_widget->set_tab_title(new_tab, title);
        if (m_tab_widget->active_widget() == &new_tab)
            set_window_title_for_tab(new_tab);
    };

    new_tab.on_favicon_change = [this, &new_tab](auto& bitmap) {
        m_tab_widget->set_tab_icon(new_tab, &bitmap);
    };

    new_tab.on_tab_open_request = [this](auto& url) {
        create_new_tab(url, true);
    };

    new_tab.on_tab_close_request = [this](auto& tab) {
        m_tab_widget->deferred_invoke([this, &tab](auto&) {
            m_tab_widget->remove_tab(tab);
            m_tab_widget->set_bar_visible(!is_fullscreen() && m_tab_widget->children().size() > 1);
            if (m_tab_widget->children().is_empty())
                close();
        });
    };

    new_tab.on_get_cookie = [this](auto& url, auto source) -> String {
        return m_cookie_jar.get_cookie(url, source);
    };

    new_tab.on_set_cookie = [this](auto& url, auto& cookie, auto source) {
        m_cookie_jar.set_cookie(url, cookie, source);
    };

    new_tab.on_dump_cookies = [this]() {
        m_cookie_jar.dump_cookies();
    };

    new_tab.load(url);

    dbgln("Added new tab {:p}, loading {}", &new_tab, url);

    if (activate)
        m_tab_widget->set_active_widget(&new_tab);
}

}
