/*
 * Copyright (c) 2022-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Matthew Costa <ucosty@gmail.com>
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrowserWindow.h"
#include "ConsoleWidget.h"
#include "Settings.h"
#include "SettingsDialog.h"
#include "Utilities.h"
#include "WebContentView.h"
#include <AK/TypeCasts.h>
#include <Browser/CookieJar.h>
#include <LibWeb/CSS/PreferredColorScheme.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <QAction>
#include <QActionGroup>
#include <QClipboard>
#include <QGuiApplication>
#include <QInputDialog>
#include <QPlainTextEdit>
#include <QTabBar>

extern DeprecatedString s_serenity_resource_root;

namespace Ladybird {
extern Settings* s_settings;

static QIcon const& app_icon()
{
    static QIcon icon;
    if (icon.isNull()) {
        QPixmap pixmap;
        pixmap.load(":/Icons/ladybird.png");
        icon = QIcon(pixmap);
    }
    return icon;
}

BrowserWindow::BrowserWindow(Browser::CookieJar& cookie_jar, StringView webdriver_content_ipc_path, WebView::EnableCallgrindProfiling enable_callgrind_profiling, WebView::UseJavaScriptBytecode use_javascript_bytecode, UseLagomNetworking use_lagom_networking)
    : m_cookie_jar(cookie_jar)
    , m_webdriver_content_ipc_path(webdriver_content_ipc_path)
    , m_enable_callgrind_profiling(enable_callgrind_profiling)
    , m_use_javascript_bytecode(use_javascript_bytecode)
    , m_use_lagom_networking(use_lagom_networking)
{
    setWindowIcon(app_icon());
    m_tabs_container = new QTabWidget(this);
    m_tabs_container->installEventFilter(this);
    m_tabs_container->setElideMode(Qt::TextElideMode::ElideRight);
    m_tabs_container->setMovable(true);
    m_tabs_container->setTabsClosable(true);
    m_tabs_container->setDocumentMode(true);
    m_tabs_container->setTabBarAutoHide(true);

    auto* menu = menuBar()->addMenu("&File");

    auto* new_tab_action = new QAction("New &Tab", this);
    new_tab_action->setIcon(QIcon(QString("%1/res/icons/16x16/new-tab.png").arg(s_serenity_resource_root.characters())));
    new_tab_action->setShortcuts(QKeySequence::keyBindings(QKeySequence::StandardKey::AddTab));
    menu->addAction(new_tab_action);

    auto* close_current_tab_action = new QAction("&Close Current Tab", this);
    close_current_tab_action->setIcon(QIcon(QString("%1/res/icons/16x16/close-tab.png").arg(s_serenity_resource_root.characters())));
    close_current_tab_action->setShortcuts(QKeySequence::keyBindings(QKeySequence::StandardKey::Close));
    menu->addAction(close_current_tab_action);

    auto* open_file_action = new QAction("&Open File...", this);
    open_file_action->setIcon(QIcon(QString("%1/res/icons/16x16/filetype-folder-open.png").arg(s_serenity_resource_root.characters())));
    open_file_action->setShortcut(QKeySequence(QKeySequence::StandardKey::Open));
    menu->addAction(open_file_action);

    menu->addSeparator();

    auto* quit_action = new QAction("&Quit", this);
    quit_action->setShortcuts(QKeySequence::keyBindings(QKeySequence::StandardKey::Quit));
    menu->addAction(quit_action);

    auto* edit_menu = menuBar()->addMenu("&Edit");

    m_copy_selection_action = make<QAction>("&Copy", this);
    m_copy_selection_action->setIcon(QIcon(QString("%1/res/icons/16x16/edit-copy.png").arg(s_serenity_resource_root.characters())));
    m_copy_selection_action->setShortcuts(QKeySequence::keyBindings(QKeySequence::StandardKey::Copy));
    edit_menu->addAction(m_copy_selection_action);
    QObject::connect(m_copy_selection_action, &QAction::triggered, this, &BrowserWindow::copy_selected_text);

    m_select_all_action = make<QAction>("Select &All", this);
    m_select_all_action->setIcon(QIcon(QString("%1/res/icons/16x16/select-all.png").arg(s_serenity_resource_root.characters())));
    m_select_all_action->setShortcuts(QKeySequence::keyBindings(QKeySequence::StandardKey::SelectAll));
    edit_menu->addAction(m_select_all_action);
    QObject::connect(m_select_all_action, &QAction::triggered, this, &BrowserWindow::select_all);

    edit_menu->addSeparator();

    auto* settings_action = new QAction("&Settings", this);
    settings_action->setIcon(QIcon(QString("%1/res/icons/16x16/settings.png").arg(s_serenity_resource_root.characters())));
    settings_action->setShortcuts(QKeySequence::keyBindings(QKeySequence::StandardKey::Preferences));
    edit_menu->addAction(settings_action);

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

    m_zoom_menu = view_menu->addMenu("&Zoom");

    auto* zoom_in_action = new QAction("Zoom &In", this);
    zoom_in_action->setIcon(QIcon(QString("%1/res/icons/16x16/zoom-in.png").arg(s_serenity_resource_root.characters())));
    auto zoom_in_shortcuts = QKeySequence::keyBindings(QKeySequence::StandardKey::ZoomIn);
    zoom_in_shortcuts.append(QKeySequence(Qt::CTRL | Qt::Key_Equal));
    zoom_in_action->setShortcuts(zoom_in_shortcuts);
    m_zoom_menu->addAction(zoom_in_action);
    QObject::connect(zoom_in_action, &QAction::triggered, this, &BrowserWindow::zoom_in);

    auto* zoom_out_action = new QAction("Zoom &Out", this);
    zoom_out_action->setIcon(QIcon(QString("%1/res/icons/16x16/zoom-out.png").arg(s_serenity_resource_root.characters())));
    zoom_out_action->setShortcuts(QKeySequence::keyBindings(QKeySequence::StandardKey::ZoomOut));
    m_zoom_menu->addAction(zoom_out_action);
    QObject::connect(zoom_out_action, &QAction::triggered, this, &BrowserWindow::zoom_out);

    auto* reset_zoom_action = new QAction("&Reset Zoom", this);
    reset_zoom_action->setIcon(QIcon(QString("%1/res/icons/16x16/zoom-reset.png").arg(s_serenity_resource_root.characters())));
    reset_zoom_action->setShortcut(QKeySequence(Qt::CTRL | Qt::Key_0));
    m_zoom_menu->addAction(reset_zoom_action);
    QObject::connect(reset_zoom_action, &QAction::triggered, this, &BrowserWindow::reset_zoom);

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

    m_view_source_action = make<QAction>("View &Source", this);
    m_view_source_action->setIcon(QIcon(QString("%1/res/icons/16x16/filetype-html.png").arg(s_serenity_resource_root.characters())));
    m_view_source_action->setShortcut(QKeySequence(Qt::CTRL | Qt::Key_U));
    inspect_menu->addAction(m_view_source_action);
    QObject::connect(m_view_source_action, &QAction::triggered, this, [this] {
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
            m_current_tab->show_console_window();
        }
    });

    auto* inspector_action = new QAction("Open &Inspector", this);
    inspector_action->setIcon(QIcon(QString("%1/res/icons/browser/dom-tree.png").arg(s_serenity_resource_root.characters())));
    inspector_action->setShortcut(QKeySequence("Ctrl+Shift+I"));
    inspect_menu->addAction(inspector_action);
    QObject::connect(inspector_action, &QAction::triggered, this, [this] {
        if (m_current_tab) {
            m_current_tab->show_inspector_window();
        }
    });

    auto* debug_menu = menuBar()->addMenu("&Debug");

    auto* dump_dom_tree_action = new QAction("Dump &DOM Tree", this);
    dump_dom_tree_action->setIcon(QIcon(QString("%1/res/icons/browser/dom-tree.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(dump_dom_tree_action);
    QObject::connect(dump_dom_tree_action, &QAction::triggered, this, [this] {
        debug_request("dump-dom-tree");
    });

    auto* dump_layout_tree_action = new QAction("Dump &Layout Tree", this);
    dump_layout_tree_action->setIcon(QIcon(QString("%1/res/icons/16x16/layout.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(dump_layout_tree_action);
    QObject::connect(dump_layout_tree_action, &QAction::triggered, this, [this] {
        debug_request("dump-layout-tree");
    });

    auto* dump_paint_tree_action = new QAction("Dump &Paint Tree", this);
    dump_paint_tree_action->setIcon(QIcon(QString("%1/res/icons/16x16/layout.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(dump_paint_tree_action);
    QObject::connect(dump_paint_tree_action, &QAction::triggered, this, [this] {
        debug_request("dump-paint-tree");
    });

    auto* dump_stacking_context_tree_action = new QAction("Dump S&tacking Context Tree", this);
    dump_stacking_context_tree_action->setIcon(QIcon(QString("%1/res/icons/16x16/layers.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(dump_stacking_context_tree_action);
    QObject::connect(dump_stacking_context_tree_action, &QAction::triggered, this, [this] {
        debug_request("dump-stacking-context-tree");
    });

    auto* dump_style_sheets_action = new QAction("Dump &Style Sheets", this);
    dump_style_sheets_action->setIcon(QIcon(QString("%1/res/icons/16x16/filetype-css.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(dump_style_sheets_action);
    QObject::connect(dump_style_sheets_action, &QAction::triggered, this, [this] {
        debug_request("dump-style-sheets");
    });

    auto* dump_styles_action = new QAction("Dump &All Resolved Styles", this);
    dump_styles_action->setIcon(QIcon(QString("%1/res/icons/16x16/filetype-css.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(dump_styles_action);
    QObject::connect(dump_styles_action, &QAction::triggered, this, [this] {
        debug_request("dump-all-resolved-styles");
    });

    auto* dump_history_action = new QAction("Dump &History", this);
    dump_history_action->setShortcut(QKeySequence(Qt::CTRL | Qt::Key_H));
    dump_history_action->setIcon(QIcon(QString("%1/res/icons/16x16/history.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(dump_history_action);
    QObject::connect(dump_history_action, &QAction::triggered, this, [this] {
        debug_request("dump-history");
    });

    auto* dump_cookies_action = new QAction("Dump C&ookies", this);
    dump_cookies_action->setIcon(QIcon(QString("%1/res/icons/browser/cookie.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(dump_cookies_action);
    QObject::connect(dump_cookies_action, &QAction::triggered, this, [this] {
        m_cookie_jar.dump_cookies();
    });

    auto* dump_local_storage_action = new QAction("Dump Loc&al Storage", this);
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

    auto* collect_garbage_action = new QAction("Collect &Garbage", this);
    collect_garbage_action->setShortcut(QKeySequence(Qt::CTRL | Qt::SHIFT | Qt::Key_G));
    collect_garbage_action->setIcon(QIcon(QString("%1/res/icons/16x16/trash-can.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(collect_garbage_action);
    QObject::connect(collect_garbage_action, &QAction::triggered, this, [this] {
        debug_request("collect-garbage");
    });

    auto* clear_cache_action = new QAction("Clear &Cache", this);
    clear_cache_action->setShortcut(QKeySequence(Qt::CTRL | Qt::SHIFT | Qt::Key_C));
    clear_cache_action->setIcon(QIcon(QString("%1/res/icons/browser/clear-cache.png").arg(s_serenity_resource_root.characters())));
    debug_menu->addAction(clear_cache_action);
    QObject::connect(clear_cache_action, &QAction::triggered, this, [this] {
        debug_request("clear-cache");
    });

    auto* spoof_user_agent_menu = debug_menu->addMenu("Spoof &User Agent");
    spoof_user_agent_menu->setIcon(QIcon(QString("%1/res/icons/16x16/spoof.png").arg(s_serenity_resource_root.characters())));

    auto* user_agent_group = new QActionGroup(this);

    auto add_user_agent = [this, &user_agent_group, &spoof_user_agent_menu](auto& name, auto& user_agent) {
        auto* action = new QAction(name, this);
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

    auto* custom_user_agent_action = new QAction("Custom...", this);
    custom_user_agent_action->setCheckable(true);
    user_agent_group->addAction(custom_user_agent_action);
    spoof_user_agent_menu->addAction(custom_user_agent_action);
    QObject::connect(custom_user_agent_action, &QAction::triggered, this, [this, disable_spoofing] {
        auto user_agent = QInputDialog::getText(this, "Custom User Agent", "Enter User Agent:");
        if (!user_agent.isEmpty()) {
            debug_request("spoof-user-agent", ak_deprecated_string_from_qstring(user_agent));
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

    auto* block_pop_ups_action = new QAction("Block Pop-ups", this);
    block_pop_ups_action->setCheckable(true);
    block_pop_ups_action->setChecked(true);
    debug_menu->addAction(block_pop_ups_action);
    QObject::connect(block_pop_ups_action, &QAction::triggered, this, [this, block_pop_ups_action] {
        bool state = block_pop_ups_action->isChecked();
        debug_request("block-pop-ups", state ? "on" : "off");
    });

    auto* enable_same_origin_policy_action = new QAction("Enable Same-Origin Policy", this);
    enable_same_origin_policy_action->setCheckable(true);
    debug_menu->addAction(enable_same_origin_policy_action);
    QObject::connect(enable_same_origin_policy_action, &QAction::triggered, this, [this, enable_same_origin_policy_action] {
        bool state = enable_same_origin_policy_action->isChecked();
        debug_request("same-origin-policy", state ? "on" : "off");
    });

    QObject::connect(new_tab_action, &QAction::triggered, this, [this] {
        new_tab(s_settings->new_tab_page(), Web::HTML::ActivateTab::Yes);
    });
    QObject::connect(open_file_action, &QAction::triggered, this, &BrowserWindow::open_file);
    QObject::connect(settings_action, &QAction::triggered, this, [this] {
        new SettingsDialog(this);
    });
    QObject::connect(quit_action, &QAction::triggered, this, &QMainWindow::close);
    QObject::connect(m_tabs_container, &QTabWidget::currentChanged, [this](int index) {
        setWindowTitle(QString("%1 - Ladybird").arg(m_tabs_container->tabText(index)));
        set_current_tab(verify_cast<Tab>(m_tabs_container->widget(index)));
    });
    QObject::connect(m_tabs_container, &QTabWidget::tabCloseRequested, this, &BrowserWindow::close_tab);
    QObject::connect(close_current_tab_action, &QAction::triggered, this, &BrowserWindow::close_current_tab);

    m_inspect_dom_node_action = make<QAction>("&Inspect Element", this);
    connect(m_inspect_dom_node_action, &QAction::triggered, this, [this] {
        if (m_current_tab)
            m_current_tab->show_inspector_window(Tab::InspectorTarget::HoveredElement);
    });
    m_go_back_action = make<QAction>("Go Back");
    connect(m_go_back_action, &QAction::triggered, this, [this] {
        if (m_current_tab)
            m_current_tab->back();
    });
    m_go_forward_action = make<QAction>("Go Forward");
    connect(m_go_forward_action, &QAction::triggered, this, [this] {
        if (m_current_tab)
            m_current_tab->forward();
    });
    m_reload_action = make<QAction>("&Reload");
    connect(m_reload_action, &QAction::triggered, this, [this] {
        if (m_current_tab)
            m_current_tab->reload();
    });

    m_go_back_action->setShortcuts(QKeySequence::keyBindings(QKeySequence::StandardKey::Back));
    m_go_forward_action->setShortcuts(QKeySequence::keyBindings(QKeySequence::StandardKey::Forward));
    m_reload_action->setShortcuts(QKeySequence::keyBindings(QKeySequence::StandardKey::Refresh));
    m_go_back_action->setEnabled(false);
    m_go_forward_action->setEnabled(false);

    new_tab(s_settings->new_tab_page(), Web::HTML::ActivateTab::Yes);

    setCentralWidget(m_tabs_container);
    setContextMenuPolicy(Qt::PreventContextMenu);
}

void BrowserWindow::set_current_tab(Tab* tab)
{
    m_current_tab = tab;
    if (tab)
        update_displayed_zoom_level();
}

void BrowserWindow::debug_request(DeprecatedString const& request, DeprecatedString const& argument)
{
    if (!m_current_tab)
        return;
    m_current_tab->debug_request(request, argument);
}

Tab& BrowserWindow::new_tab(QString const& url, Web::HTML::ActivateTab activate_tab)
{
    auto tab = make<Tab>(this, m_webdriver_content_ipc_path, m_enable_callgrind_profiling, m_use_javascript_bytecode, m_use_lagom_networking);
    auto tab_ptr = tab.ptr();
    m_tabs.append(std::move(tab));

    if (m_current_tab == nullptr) {
        set_current_tab(tab_ptr);
    }

    m_tabs_container->addTab(tab_ptr, "New Tab");
    if (activate_tab == Web::HTML::ActivateTab::Yes)
        m_tabs_container->setCurrentWidget(tab_ptr);

    QObject::connect(tab_ptr, &Tab::title_changed, this, &BrowserWindow::tab_title_changed);
    QObject::connect(tab_ptr, &Tab::favicon_changed, this, &BrowserWindow::tab_favicon_changed);

    QObject::connect(&tab_ptr->view(), &WebContentView::urls_dropped, this, [this](auto& urls) {
        VERIFY(urls.size());
        m_current_tab->navigate(urls[0].toString());

        for (qsizetype i = 1; i < urls.size(); ++i)
            new_tab(urls[i].toString(), Web::HTML::ActivateTab::No);
    });

    tab_ptr->view().on_new_tab = [this](auto activate_tab) {
        auto& tab = new_tab("about:blank", activate_tab);
        return tab.view().handle();
    };

    tab_ptr->view().on_tab_open_request = [this](auto url, auto activate_tab) {
        auto& tab = new_tab(qstring_from_ak_deprecated_string(url.to_deprecated_string()), activate_tab);
        return tab.view().handle();
    };

    tab_ptr->view().on_link_click = [this](auto url, auto target, unsigned modifiers) {
        // TODO: maybe activate tabs according to some configuration, this is just normal current browser behavior
        if (modifiers == Mod_Ctrl) {
            m_current_tab->view().on_tab_open_request(url, Web::HTML::ActivateTab::No);
        } else if (target == "_blank") {
            m_current_tab->view().on_tab_open_request(url, Web::HTML::ActivateTab::Yes);
        } else {
            m_current_tab->view().load(url);
        }
    };

    tab_ptr->view().on_link_middle_click = [this](auto url, auto target, unsigned modifiers) {
        m_current_tab->view().on_link_click(url, target, Mod_Ctrl);
        (void)modifiers;
    };

    tab_ptr->view().on_get_all_cookies = [this](auto const& url) {
        return m_cookie_jar.get_all_cookies(url);
    };

    tab_ptr->view().on_get_named_cookie = [this](auto const& url, auto const& name) {
        return m_cookie_jar.get_named_cookie(url, name);
    };

    tab_ptr->view().on_get_cookie = [this](auto& url, auto source) -> DeprecatedString {
        return m_cookie_jar.get_cookie(url, source);
    };

    tab_ptr->view().on_set_cookie = [this](auto& url, auto& cookie, auto source) {
        m_cookie_jar.set_cookie(url, cookie, source);
    };

    tab_ptr->view().on_update_cookie = [this](auto const& cookie) {
        m_cookie_jar.update_cookie(cookie);
    };

    tab_ptr->focus_location_editor();

    // We *don't* load the initial page if we are connected to a WebDriver, as the Set URL command may come in very
    // quickly, and become replaced by this load.
    if (m_webdriver_content_ipc_path.is_empty()) {
        // We make it HistoryNavigation so that the initial page doesn't get added to the history.
        tab_ptr->navigate(url, Tab::LoadType::HistoryNavigation);
    }

    return *tab_ptr;
}

void BrowserWindow::activate_tab(int index)
{
    m_tabs_container->setCurrentIndex(index);
}

void BrowserWindow::close_tab(int index)
{
    auto* tab = m_tabs_container->widget(index);
    m_tabs_container->removeTab(index);
    m_tabs.remove_first_matching([&](auto& entry) {
        return entry == tab;
    });
}

void BrowserWindow::open_file()
{
    m_current_tab->open_file();
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
    m_tabs_container->setTabText(index, title);
    if (m_tabs_container->currentIndex() == index)
        setWindowTitle(QString("%1 - Ladybird").arg(title));
}

void BrowserWindow::tab_favicon_changed(int index, QIcon icon)
{
    m_tabs_container->setTabIcon(index, icon);
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
        tab->view().set_preferred_color_scheme(Web::CSS::PreferredColorScheme::Auto);
    }
}

void BrowserWindow::enable_light_color_scheme()
{
    for (auto& tab : m_tabs) {
        tab->view().set_preferred_color_scheme(Web::CSS::PreferredColorScheme::Light);
    }
}

void BrowserWindow::enable_dark_color_scheme()
{
    for (auto& tab : m_tabs) {
        tab->view().set_preferred_color_scheme(Web::CSS::PreferredColorScheme::Dark);
    }
}

void BrowserWindow::zoom_in()
{
    if (!m_current_tab)
        return;
    m_current_tab->view().zoom_in();
    update_displayed_zoom_level();
}

void BrowserWindow::zoom_out()
{
    if (!m_current_tab)
        return;
    m_current_tab->view().zoom_out();
    update_displayed_zoom_level();
}

void BrowserWindow::reset_zoom()
{
    if (!m_current_tab)
        return;
    m_current_tab->view().reset_zoom();
    update_displayed_zoom_level();
}

void BrowserWindow::select_all()
{
    if (!m_current_tab)
        return;

    if (auto* console = m_current_tab->console(); console && console->isActiveWindow())
        console->view().select_all();
    else
        m_current_tab->view().select_all();
}

void BrowserWindow::update_displayed_zoom_level()
{
    VERIFY(m_zoom_menu && m_current_tab);
    auto zoom_level_text = MUST(String::formatted("&Zoom ({}%)", round_to<int>(m_current_tab->view().zoom_level() * 100)));
    m_zoom_menu->setTitle(qstring_from_ak_string(zoom_level_text));
    m_current_tab->update_reset_zoom_button();
}

void BrowserWindow::copy_selected_text()
{
    if (!m_current_tab)
        return;

    DeprecatedString text;

    if (auto* console = m_current_tab->console(); console && console->isActiveWindow())
        text = console->view().selected_text();
    else
        text = m_current_tab->view().selected_text();

    auto* clipboard = QGuiApplication::clipboard();
    clipboard->setText(qstring_from_ak_deprecated_string(text));
}

void BrowserWindow::resizeEvent(QResizeEvent* event)
{
    QWidget::resizeEvent(event);
    for (auto& tab : m_tabs) {
        tab->view().set_window_size({ frameSize().width(), frameSize().height() });
    }
}

void BrowserWindow::moveEvent(QMoveEvent* event)
{
    QWidget::moveEvent(event);
    for (auto& tab : m_tabs) {
        tab->view().set_window_position({ event->pos().x(), event->pos().y() });
    }
}

void BrowserWindow::wheelEvent(QWheelEvent* event)
{
    if ((event->modifiers() & Qt::ControlModifier) != 0) {
        if (event->angleDelta().y() > 0)
            zoom_in();
        else if (event->angleDelta().y() < 0)
            zoom_out();
    }
}

bool BrowserWindow::eventFilter(QObject* obj, QEvent* event)
{
    if (event->type() == QEvent::MouseButtonRelease) {
        auto const* const mouse_event = static_cast<QMouseEvent*>(event);
        if (mouse_event->button() == Qt::MouseButton::MiddleButton) {
            if (obj == m_tabs_container) {
                auto const tab_index = m_tabs_container->tabBar()->tabAt(mouse_event->pos());
                close_tab(tab_index);
                return true;
            }
        }
    }

    return QMainWindow::eventFilter(obj, event);
}

}
