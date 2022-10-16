/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Matthew Costa <ucosty@gmail.com>
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrowserWindow.h"
#include "Settings.h"
#include "SettingsDialog.h"
#include "Utilities.h"
#include "WebContentView.h"
#include <AK/TypeCasts.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <QAction>
#include <QActionGroup>
#include <QInputDialog>
#include <QPlainTextEdit>

extern String s_serenity_resource_root;
extern Browser::Settings* s_settings;

BrowserWindow::BrowserWindow()
{
    m_tabs_container = new QTabWidget(this);
    m_tabs_container->setElideMode(Qt::TextElideMode::ElideRight);
    m_tabs_container->setMovable(true);
    m_tabs_container->setTabsClosable(true);
    m_tabs_container->setDocumentMode(true);
    m_tabs_container->setTabBarAutoHide(true);

    auto* menu = menuBar()->addMenu("&File");

    auto* new_tab_action = new QAction("New &Tab", this);
    new_tab_action->setShortcut(QKeySequence(Qt::CTRL | Qt::Key_T));
    menu->addAction(new_tab_action);

    auto* settings_action = new QAction("&Settings", this);
    settings_action->setShortcut(QKeySequence(Qt::CTRL | Qt::Key_Comma));
    menu->addAction(settings_action);

    auto* close_current_tab_action = new QAction("Close Current Tab", this);
    close_current_tab_action->setShortcut(QKeySequence(Qt::CTRL | Qt::Key_W));
    menu->addAction(close_current_tab_action);

    auto* quit_action = new QAction("&Quit", this);
    quit_action->setShortcut(QKeySequence(Qt::CTRL | Qt::Key_Q));
    menu->addAction(quit_action);

    auto* view_menu = menuBar()->addMenu("&View");

    auto* open_next_tab_action = new QAction("Open &Next Tab", this);
    open_next_tab_action->setShortcut(QKeySequence(Qt::CTRL | Qt::Key_PageDown));
    view_menu->addAction(open_next_tab_action);
    QObject::connect(open_next_tab_action, &QAction::triggered, this, &BrowserWindow::open_next_tab);

    auto* open_previous_tab_action = new QAction("Open &Previous Tab", this);
    open_previous_tab_action->setShortcut(QKeySequence(Qt::CTRL | Qt::Key_PageUp));
    view_menu->addAction(open_previous_tab_action);
    QObject::connect(open_previous_tab_action, &QAction::triggered, this, &BrowserWindow::open_previous_tab);

    view_menu->addSeparator();

    auto* color_scheme_menu = view_menu->addMenu("&Color Scheme");

    auto* color_scheme_group = new QActionGroup(this);

    auto* auto_color_scheme = new QAction("&Auto", this);
    auto_color_scheme->setCheckable(true);
    color_scheme_group->addAction(auto_color_scheme);
    color_scheme_menu->addAction(auto_color_scheme);
    QObject::connect(auto_color_scheme, &QAction::triggered, this, &BrowserWindow::enable_auto_color_scheme);

    auto* light_color_scheme = new QAction("&Light", this);
    light_color_scheme->setCheckable(true);
    color_scheme_group->addAction(light_color_scheme);
    color_scheme_menu->addAction(light_color_scheme);
    QObject::connect(light_color_scheme, &QAction::triggered, this, &BrowserWindow::enable_light_color_scheme);

    auto* dark_color_scheme = new QAction("&Dark", this);
    dark_color_scheme->setCheckable(true);
    color_scheme_group->addAction(dark_color_scheme);
    color_scheme_menu->addAction(dark_color_scheme);
    QObject::connect(dark_color_scheme, &QAction::triggered, this, &BrowserWindow::enable_dark_color_scheme);

    auto_color_scheme->setChecked(true);

    auto* inspect_menu = menuBar()->addMenu("&Inspect");

    auto* view_source_action = new QAction("View &Source", this);
    view_source_action->setIcon(QIcon(QString("%1/res/icons/16x16/filetype-html.png").arg(s_serenity_resource_root.characters())));
    view_source_action->setShortcut(QKeySequence(Qt::CTRL | Qt::Key_U));
    inspect_menu->addAction(view_source_action);
    QObject::connect(view_source_action, &QAction::triggered, this, [this] {
        if (m_current_tab) {
            m_current_tab->view().get_source();
        }
    });

    auto* js_console_action = new QAction("Show &JS Console", this);
    js_console_action->setIcon(QIcon(QString("%1/res/icons/16x16/filetype-javascript.png").arg(s_serenity_resource_root.characters())));
    js_console_action->setShortcut(QKeySequence(Qt::CTRL | Qt::SHIFT | Qt::Key_J));
    inspect_menu->addAction(js_console_action);
    QObject::connect(js_console_action, &QAction::triggered, this, [this] {
        if (m_current_tab) {
            m_current_tab->view().show_js_console();
        }
    });

    auto* inspector_action = new QAction("Open &Inspector");
    inspector_action->setIcon(QIcon(QString("%1/res/icons/browser/dom-tree.png").arg(s_serenity_resource_root.characters())));
    inspector_action->setShortcut(QKeySequence("Ctrl+Shift+I"));
    inspect_menu->addAction(inspector_action);
    QObject::connect(inspector_action, &QAction::triggered, this, [this] {
        if (m_current_tab) {
            m_current_tab->view().show_inspector();
        }
    });

    auto* debug_menu = menuBar()->addMenu("&Debug");

    auto* dump_dom_tree_action = new QAction("Dump DOM Tree", this);
    dump_dom_tree_action->setIcon(QIcon(QString("%1/res/icons/browser/dom-tree.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(dump_dom_tree_action);
    QObject::connect(dump_dom_tree_action, &QAction::triggered, this, [this] {
        debug_request("dump-dom-tree");
    });

    auto* dump_layout_tree_action = new QAction("Dump Layout Tree", this);
    dump_layout_tree_action->setIcon(QIcon(QString("%1/res/icons/16x16/layout.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(dump_layout_tree_action);
    QObject::connect(dump_layout_tree_action, &QAction::triggered, this, [this] {
        debug_request("dump-layout-tree");
    });

    auto* dump_stacking_context_tree_action = new QAction("Dump Stacking Context Tree", this);
    dump_stacking_context_tree_action->setIcon(QIcon(QString("%1/res/icons/16x16/layers.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(dump_stacking_context_tree_action);
    QObject::connect(dump_stacking_context_tree_action, &QAction::triggered, this, [this] {
        debug_request("dump-stacking-context-tree");
    });

    auto* dump_style_sheets_action = new QAction("Dump Style Sheets", this);
    dump_style_sheets_action->setIcon(QIcon(QString("%1/res/icons/16x16/filetype-css.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(dump_style_sheets_action);
    QObject::connect(dump_style_sheets_action, &QAction::triggered, this, [this] {
        debug_request("dump-style-sheets");
    });

    auto* dump_history_action = new QAction("Dump History", this);
    dump_history_action->setIcon(QIcon(QString("%1/res/icons/16x16/history.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(dump_history_action);
    QObject::connect(dump_history_action, &QAction::triggered, this, [this] {
        debug_request("dump-history");
    });

    auto* dump_cookies_action = new QAction("Dump Cookies", this);
    dump_cookies_action->setIcon(QIcon(QString("%1/res/icons/browser/cookie.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(dump_cookies_action);
    QObject::connect(dump_cookies_action, &QAction::triggered, this, [this] {
        m_cookie_jar.dump_cookies();
    });

    auto* dump_local_storage_action = new QAction("Dump Local Storage", this);
    dump_local_storage_action->setIcon(QIcon(QString("%1/res/icons/browser/local-storage.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(dump_local_storage_action);
    QObject::connect(dump_local_storage_action, &QAction::triggered, this, [this] {
        debug_request("dump-local-storage");
    });

    debug_menu->addSeparator();

    auto* show_line_box_borders_action = new QAction("Show Line Box Borders", this);
    show_line_box_borders_action->setCheckable(true);
    debug_menu->addAction(show_line_box_borders_action);
    QObject::connect(show_line_box_borders_action, &QAction::triggered, this, [this, show_line_box_borders_action] {
        bool state = show_line_box_borders_action->isChecked();
        debug_request("set-line-box-borders", state ? "on" : "off");
    });

    debug_menu->addSeparator();

    auto* collect_garbage_action = new QAction("Collect Garbage", this);
    collect_garbage_action->setShortcut(QKeySequence(Qt::CTRL | Qt::SHIFT | Qt::Key_G));
    collect_garbage_action->setIcon(QIcon(QString("%1/res/icons/16x16/trash-can.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(collect_garbage_action);
    QObject::connect(collect_garbage_action, &QAction::triggered, this, [this] {
        debug_request("collect-garbage");
    });

    auto* clear_cache_action = new QAction("Clear Cache", this);
    clear_cache_action->setIcon(QIcon(QString("%1/res/icons/browser/clear-cache.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(clear_cache_action);
    QObject::connect(clear_cache_action, &QAction::triggered, this, [this] {
        debug_request("clear-cache");
    });

    auto* spoof_user_agent_menu = debug_menu->addMenu("Spoof User Agent");
    spoof_user_agent_menu->setIcon(QIcon(QString("%1/res/icons/16x16/spoof.png").arg(s_serenity_resource_root.characters())));

    auto* user_agent_group = new QActionGroup(this);

    auto add_user_agent = [this, &user_agent_group, &spoof_user_agent_menu](auto& name, auto& user_agent) {
        auto* action = new QAction(name);
        action->setCheckable(true);
        user_agent_group->addAction(action);
        spoof_user_agent_menu->addAction(action);
        QObject::connect(action, &QAction::triggered, this, [this, user_agent] {
            debug_request("spoof-user-agent", user_agent);
            debug_request("clear-cache"); // clear the cache to ensure requests are re-done with the new user agent
        });
        return action;
    };

    auto* disable_spoofing = add_user_agent("Disabled", Web::default_user_agent);
    disable_spoofing->setChecked(true);
    add_user_agent("Chrome Linux Desktop", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.128 Safari/537.36");
    add_user_agent("Firefox Linux Desktop", "Mozilla/5.0 (X11; Linux i686; rv:87.0) Gecko/20100101 Firefox/87.0");
    add_user_agent("Safari macOS Desktop", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_2_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.3 Safari/605.1.15");
    add_user_agent("Chrome Android Mobile", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.66 Mobile Safari/537.36");
    add_user_agent("Firefox Android Mobile", "Mozilla/5.0 (Android 11; Mobile; rv:68.0) Gecko/68.0 Firefox/86.0");
    add_user_agent("Safari iOS Mobile", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_4_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1");

    auto* custom_user_agent_action = new QAction("Custom...");
    custom_user_agent_action->setCheckable(true);
    user_agent_group->addAction(custom_user_agent_action);
    spoof_user_agent_menu->addAction(custom_user_agent_action);
    QObject::connect(custom_user_agent_action, &QAction::triggered, this, [this, disable_spoofing] {
        auto user_agent = QInputDialog::getText(this, "Custom User Agent", "Enter User Agent:");
        if (!user_agent.isEmpty()) {
            debug_request("spoof-user-agent", akstring_from_qstring(user_agent));
            debug_request("clear-cache"); // clear the cache to ensure requests are re-done with the new user agent
        } else {
            disable_spoofing->activate(QAction::Trigger);
        }
    });

    debug_menu->addSeparator();

    auto* enable_scripting_action = new QAction("Enable Scripting", this);
    enable_scripting_action->setCheckable(true);
    enable_scripting_action->setChecked(true);
    debug_menu->addAction(enable_scripting_action);
    QObject::connect(enable_scripting_action, &QAction::triggered, this, [this, enable_scripting_action] {
        bool state = enable_scripting_action->isChecked();
        debug_request("scripting", state ? "on" : "off");
    });

    auto* enable_same_origin_policy_action = new QAction("Enable Same-Origin Policy", this);
    enable_same_origin_policy_action->setCheckable(true);
    debug_menu->addAction(enable_same_origin_policy_action);
    QObject::connect(enable_same_origin_policy_action, &QAction::triggered, this, [this, enable_same_origin_policy_action] {
        bool state = enable_same_origin_policy_action->isChecked();
        debug_request("same-origin-policy", state ? "on" : "off");
    });

    QObject::connect(new_tab_action, &QAction::triggered, this, &BrowserWindow::new_tab);
    QObject::connect(settings_action, &QAction::triggered, this, [this] {
        new SettingsDialog(this);
    });
    QObject::connect(quit_action, &QAction::triggered, this, &QMainWindow::close);
    QObject::connect(m_tabs_container, &QTabWidget::currentChanged, [this](int index) {
        setWindowTitle(QString("%1 - Ladybird").arg(m_tabs_container->tabText(index)));
        setWindowIcon(m_tabs_container->tabIcon(index));
        m_current_tab = verify_cast<Tab>(m_tabs_container->widget(index));
    });
    QObject::connect(m_tabs_container, &QTabWidget::tabCloseRequested, this, &BrowserWindow::close_tab);
    QObject::connect(close_current_tab_action, &QAction::triggered, this, &BrowserWindow::close_current_tab);

    new_tab();

    setCentralWidget(m_tabs_container);
}

void BrowserWindow::debug_request(String const& request, String const& argument)
{
    if (!m_current_tab)
        return;
    m_current_tab->debug_request(request, argument);
}

void BrowserWindow::new_tab()
{
    auto tab = make<Tab>(this);
    auto tab_ptr = tab.ptr();
    m_tabs.append(std::move(tab));

    if (m_current_tab == nullptr) {
        m_current_tab = tab_ptr;
    }

    m_tabs_container->addTab(tab_ptr, "New Tab");
    m_tabs_container->setCurrentWidget(tab_ptr);

    QObject::connect(tab_ptr, &Tab::title_changed, this, &BrowserWindow::tab_title_changed);
    QObject::connect(tab_ptr, &Tab::favicon_changed, this, &BrowserWindow::tab_favicon_changed);

    tab_ptr->view().on_get_cookie = [this](auto& url, auto source) -> String {
        return m_cookie_jar.get_cookie(url, source);
    };

    tab_ptr->view().on_set_cookie = [this](auto& url, auto& cookie, auto source) {
        m_cookie_jar.set_cookie(url, cookie, source);
    };

    tab_ptr->focus_location_editor();
}

void BrowserWindow::close_tab(int index)
{
    auto* tab = m_tabs_container->widget(index);
    m_tabs_container->removeTab(index);
    m_tabs.remove_first_matching([&](auto& entry) {
        return entry == tab;
    });
}

void BrowserWindow::close_current_tab()
{
    auto count = m_tabs_container->count() - 1;
    if (!count)
        close();
    else
        close_tab(m_tabs_container->currentIndex());
}

int BrowserWindow::tab_index(Tab* tab)
{
    return m_tabs_container->indexOf(tab);
}

void BrowserWindow::tab_title_changed(int index, QString const& title)
{
    if (title.isEmpty()) {
        m_tabs_container->setTabText(index, "...");
        setWindowTitle("Ladybird");
    } else {
        m_tabs_container->setTabText(index, title);
        setWindowTitle(QString("%1 - Ladybird").arg(title));
    }
}

void BrowserWindow::tab_favicon_changed(int index, QIcon icon)
{
    m_tabs_container->setTabIcon(index, icon);
    setWindowIcon(icon);
}

void BrowserWindow::open_next_tab()
{
    if (m_tabs_container->count() <= 1)
        return;

    auto next_index = m_tabs_container->currentIndex() + 1;
    if (next_index >= m_tabs_container->count())
        next_index = 0;
    m_tabs_container->setCurrentIndex(next_index);
}

void BrowserWindow::open_previous_tab()
{
    if (m_tabs_container->count() <= 1)
        return;

    auto next_index = m_tabs_container->currentIndex() - 1;
    if (next_index < 0)
        next_index = m_tabs_container->count() - 1;
    m_tabs_container->setCurrentIndex(next_index);
}

void BrowserWindow::enable_auto_color_scheme()
{
    for (auto& tab : m_tabs) {
        tab.view().set_color_scheme(ColorScheme::Auto);
    }
}

void BrowserWindow::enable_light_color_scheme()
{
    for (auto& tab : m_tabs) {
        tab.view().set_color_scheme(ColorScheme::Light);
    }
}

void BrowserWindow::enable_dark_color_scheme()
{
    for (auto& tab : m_tabs) {
        tab.view().set_color_scheme(ColorScheme::Dark);
    }
}
