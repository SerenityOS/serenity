/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrowserWindow.h"
#include "BookmarksBarWidget.h"
#include "CookieJar.h"
#include "Tab.h"
#include <Applications/Browser/BrowserWindowGML.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/Widget.h>

namespace Browser {

extern bool g_single_process;
extern String g_home_url;

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

    create_new_tab(move(url), true);
}

BrowserWindow::~BrowserWindow()
{
}

GUI::TabWidget& BrowserWindow::tab_widget()
{
    return *m_tab_widget;
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
    auto& new_tab = m_tab_widget->add_tab<Browser::Tab>("New tab", type);

    m_tab_widget->set_bar_visible(!is_fullscreen() && m_tab_widget->children().size() > 1);

    auto default_favicon = Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-html.png");
    VERIFY(default_favicon);
    m_tab_widget->set_tab_icon(new_tab, default_favicon);

    new_tab.on_title_change = [this, &new_tab](auto title) {
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
