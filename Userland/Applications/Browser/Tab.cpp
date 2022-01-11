/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Maciej Zygmanowski <sppmacd@pm.me>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tab.h"
#include "BookmarksBarWidget.h"
#include "Browser.h"
#include "BrowserWindow.h"
#include "ConsoleWidget.h"
#include "DownloadWidget.h"
#include "InspectorWidget.h"
#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <Applications/Browser/TabGML.h>
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
#include <LibJS/Interpreter.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/SyntaxHighlighter/SyntaxHighlighter.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/OutOfProcessWebView.h>

namespace Browser {

URL url_from_user_input(const String& input)
{
    String url_string = input;
    if (input.starts_with("?") && !g_search_engine.is_empty())
        url_string = g_search_engine.replace("{}", URL::percent_encode(input.substring_view(1)));

    URL url = URL(url_string);
    if (url.is_valid())
        return url;

    StringBuilder builder;
    builder.append("http://");
    builder.append(url_string);
    return URL(builder.build());
}

void Tab::start_download(const URL& url)
{
    auto window = GUI::Window::construct(&this->window());
    window->resize(300, 170);
    window->set_title(String::formatted("0% of {}", url.basename()));
    window->set_resizable(false);
    window->set_main_widget<DownloadWidget>(url);
    window->show();
}

void Tab::view_source(const URL& url, const String& source)
{
    auto window = GUI::Window::construct(&this->window());
    auto& editor = window->set_main_widget<GUI::TextEditor>();
    editor.set_text(source);
    editor.set_mode(GUI::TextEditor::ReadOnly);
    editor.set_syntax_highlighter(make<Web::HTML::SyntaxHighlighter>());
    editor.set_ruler_visible(true);
    window->resize(640, 480);
    window->set_title(url.to_string());
    window->set_icon(g_icon_bag.filetype_text);
    window->show();
}

Tab::Tab(BrowserWindow& window)
{
    load_from_gml(tab_gml);

    m_toolbar_container = *find_descendant_of_type_named<GUI::ToolbarContainer>("toolbar_container");
    auto& toolbar = *find_descendant_of_type_named<GUI::Toolbar>("toolbar");

    auto& webview_container = *find_descendant_of_type_named<GUI::Widget>("webview_container");

    m_web_content_view = webview_container.add<Web::OutOfProcessWebView>();
    m_web_content_view->set_content_filters(g_content_filters);

    auto& go_back_button = toolbar.add_action(window.go_back_action());
    go_back_button.on_context_menu_request = [this](auto& context_menu_event) {
        if (!m_history.can_go_back())
            return;
        int i = 0;
        m_go_back_context_menu = GUI::Menu::construct();
        for (auto& url : m_history.get_back_title_history()) {
            i++;
            m_go_back_context_menu->add_action(GUI::Action::create(url.to_string(), g_icon_bag.filetype_html, [this, i](auto&) { go_back(i); }));
        }
        m_go_back_context_menu->popup(context_menu_event.screen_position());
    };

    auto& go_forward_button = toolbar.add_action(window.go_forward_action());
    go_forward_button.on_context_menu_request = [this](auto& context_menu_event) {
        if (!m_history.can_go_forward())
            return;
        int i = 0;
        m_go_forward_context_menu = GUI::Menu::construct();
        for (auto& url : m_history.get_forward_title_history()) {
            i++;
            m_go_forward_context_menu->add_action(GUI::Action::create(url.to_string(), g_icon_bag.filetype_html, [this, i](auto&) { go_forward(i); }));
        }
        m_go_forward_context_menu->popup(context_menu_event.screen_position());
    };

    toolbar.add_action(window.go_home_action());
    toolbar.add_action(window.reload_action());

    m_location_box = toolbar.add<GUI::UrlBox>();
    m_location_box->set_placeholder("Address");

    m_location_box->on_return_pressed = [this] {
        if (m_location_box->text().starts_with('?') && g_search_engine.is_empty()) {
            GUI::MessageBox::show(&this->window(), "Select a search engine in the Settings menu before searching.", "No search engine selected", GUI::MessageBox::Type::Information);
            return;
        }

        auto url = url_from_user_input(m_location_box->text());
        load(url);
    };

    m_location_box->add_custom_context_menu_action(GUI::Action::create("Paste && Go", [this](auto&) {
        auto [data, mime_type, _] = GUI::Clipboard::the().fetch_data_and_type();
        if (!mime_type.starts_with("text/"))
            return;
        auto const& paste_text = data;
        if (paste_text.is_empty())
            return;
        m_location_box->set_text(paste_text);
        m_location_box->on_return_pressed();
    }));

    m_bookmark_button = toolbar.add<GUI::Button>();
    m_bookmark_button->set_button_style(Gfx::ButtonStyle::Coolbar);
    m_bookmark_button->set_focus_policy(GUI::FocusPolicy::TabFocus);
    m_bookmark_button->set_icon(g_icon_bag.bookmark_contour);
    m_bookmark_button->set_fixed_size(22, 22);

    m_bookmark_button->on_click = [this](auto) {
        bookmark_current_url();
    };

    auto bookmark_action = GUI::Action::create(
        "Bookmark current URL", { Mod_Ctrl, Key_D }, [this](auto&) {
            bookmark_current_url();
        },
        this);

    hooks().on_load_start = [this](auto& url) {
        m_location_box->set_icon(nullptr);
        m_location_box->set_text(url.to_string());

        // don't add to history if back or forward is pressed
        if (!m_is_history_navigation)
            m_history.push(url, title());
        m_is_history_navigation = false;

        update_actions();
        update_bookmark_button(url.to_string());

        if (m_dom_inspector_widget)
            m_dom_inspector_widget->clear_dom_json();

        if (m_console_widget)
            m_console_widget->reset();
    };

    hooks().on_load_finish = [this](auto&) {
        if (m_dom_inspector_widget)
            m_web_content_view->inspect_dom_tree();
    };

    hooks().on_link_click = [this](auto& url, auto& target, unsigned modifiers) {
        if (target == "_blank" || modifiers == Mod_Ctrl) {
            on_tab_open_request(url);
        } else {
            load(url);
        }
    };

    m_link_context_menu = GUI::Menu::construct();
    auto link_default_action = GUI::Action::create("&Open", [this](auto&) {
        hooks().on_link_click(m_link_context_menu_url, "", 0);
    });
    m_link_context_menu->add_action(link_default_action);
    m_link_context_menu_default_action = link_default_action;
    m_link_context_menu->add_action(GUI::Action::create("Open in New &Tab", [this](auto&) {
        hooks().on_link_click(m_link_context_menu_url, "_blank", 0);
    }));
    m_link_context_menu->add_separator();
    m_link_context_menu->add_action(GUI::Action::create("&Copy URL", [this](auto&) {
        GUI::Clipboard::the().set_plain_text(m_link_context_menu_url.to_string());
    }));
    m_link_context_menu->add_separator();
    m_link_context_menu->add_action(GUI::Action::create("&Download", [this](auto&) {
        start_download(m_link_context_menu_url);
    }));
    m_link_context_menu->add_separator();
    m_link_context_menu->add_action(window.inspect_dom_node_action());

    hooks().on_link_context_menu_request = [this](auto& url, auto& screen_position) {
        m_link_context_menu_url = url;
        m_link_context_menu->popup(screen_position, m_link_context_menu_default_action);
    };

    m_image_context_menu = GUI::Menu::construct();
    m_image_context_menu->add_action(GUI::Action::create("&Open Image", [this](auto&) {
        hooks().on_link_click(m_image_context_menu_url, "", 0);
    }));
    m_image_context_menu->add_action(GUI::Action::create("Open Image in New &Tab", [this](auto&) {
        hooks().on_link_click(m_image_context_menu_url, "_blank", 0);
    }));
    m_image_context_menu->add_separator();
    m_image_context_menu->add_action(GUI::Action::create("&Copy Image", [this](auto&) {
        if (m_image_context_menu_bitmap.is_valid())
            GUI::Clipboard::the().set_bitmap(*m_image_context_menu_bitmap.bitmap());
    }));
    m_image_context_menu->add_action(GUI::Action::create("Copy Image &URL", [this](auto&) {
        GUI::Clipboard::the().set_plain_text(m_image_context_menu_url.to_string());
    }));
    m_image_context_menu->add_separator();
    m_image_context_menu->add_action(GUI::Action::create("&Download", [this](auto&) {
        start_download(m_image_context_menu_url);
    }));
    m_image_context_menu->add_separator();
    m_image_context_menu->add_action(window.inspect_dom_node_action());

    hooks().on_image_context_menu_request = [this](auto& image_url, auto& screen_position, const Gfx::ShareableBitmap& shareable_bitmap) {
        m_image_context_menu_url = image_url;
        m_image_context_menu_bitmap = shareable_bitmap;
        m_image_context_menu->popup(screen_position);
    };

    hooks().on_link_middle_click = [this](auto& href, auto&, auto) {
        hooks().on_link_click(href, "_blank", 0);
    };

    hooks().on_title_change = [this](auto& title) {
        if (title.is_null()) {
            m_history.update_title(url().to_string());
            m_title = url().to_string();
        } else {
            m_history.update_title(title);
            m_title = title;
        }
        if (on_title_change)
            on_title_change(m_title);
    };

    hooks().on_favicon_change = [this](auto& icon) {
        m_icon = icon;
        m_location_box->set_icon(&icon);
        if (on_favicon_change)
            on_favicon_change(icon);
    };

    hooks().on_get_cookie = [this](auto& url, auto source) -> String {
        if (on_get_cookie)
            return on_get_cookie(url, source);
        return {};
    };

    hooks().on_set_cookie = [this](auto& url, auto& cookie, auto source) {
        if (on_set_cookie)
            on_set_cookie(url, cookie, source);
    };

    hooks().on_get_source = [this](auto& url, auto& source) {
        view_source(url, source);
    };

    hooks().on_get_dom_tree = [this](auto& dom_tree) {
        if (m_dom_inspector_widget)
            m_dom_inspector_widget->set_dom_json(dom_tree);
    };

    hooks().on_get_dom_node_properties = [this](auto node_id, auto& specified, auto& computed, auto& custom_properties) {
        m_dom_inspector_widget->set_dom_node_properties_json(node_id, specified, computed, custom_properties);
    };

    hooks().on_js_console_new_message = [this](auto message_index) {
        if (m_console_widget)
            m_console_widget->notify_about_new_console_message(message_index);
    };

    hooks().on_get_js_console_messages = [this](auto start_index, auto& message_types, auto& messages) {
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

    hooks().on_link_hover = [this](auto& url) {
        if (url.is_valid())
            m_statusbar->set_text(url.to_string());
        else
            m_statusbar->set_text("");
    };

    hooks().on_url_drop = [this](auto& url) {
        load(url);
    };

    m_tab_context_menu = GUI::Menu::construct();
    m_tab_context_menu->add_action(GUI::CommonActions::make_reload_action([this](auto&) {
        this->window().reload_action().activate();
    }));
    m_tab_context_menu->add_action(GUI::CommonActions::make_close_tab_action([this](auto&) {
        on_tab_close_request(*this);
    }));
    m_tab_context_menu->add_action(GUI::Action::create("&Duplicate Tab", g_icon_bag.duplicate_tab, [this](auto&) {
        on_tab_open_request(url());
    }));
    m_tab_context_menu->add_action(GUI::Action::create("Close &Other Tabs", [this](auto&) {
        on_tab_close_other_request(*this);
    }));

    m_page_context_menu = GUI::Menu::construct();
    m_page_context_menu->add_action(window.go_back_action());
    m_page_context_menu->add_action(window.go_forward_action());
    m_page_context_menu->add_action(window.reload_action());
    m_page_context_menu->add_separator();
    m_page_context_menu->add_action(window.copy_selection_action());
    m_page_context_menu->add_action(window.select_all_action());
    m_page_context_menu->add_separator();
    m_page_context_menu->add_action(window.view_source_action());
    m_page_context_menu->add_action(window.inspect_dom_tree_action());
    m_page_context_menu->add_action(window.inspect_dom_node_action());
    hooks().on_context_menu_request = [&](auto& screen_position) {
        m_page_context_menu->popup(screen_position);
    };
}

Tab::~Tab()
{
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

void Tab::bookmark_current_url()
{
    auto url = this->url().to_string();
    if (BookmarksBarWidget::the().contains_bookmark(url)) {
        BookmarksBarWidget::the().remove_bookmark(url);
    } else {
        BookmarksBarWidget::the().add_bookmark(url, m_title);
    }
    update_bookmark_button(url);
}

void Tab::update_bookmark_button(const String& url)
{
    if (BookmarksBarWidget::the().contains_bookmark(url)) {
        m_bookmark_button->set_icon(g_icon_bag.bookmark_filled);
        m_bookmark_button->set_tooltip("Remove Bookmark");
    } else {
        m_bookmark_button->set_icon(g_icon_bag.bookmark_contour);
        m_bookmark_button->set_tooltip("Add Bookmark");
    }
}

void Tab::did_become_active()
{
    BookmarksBarWidget::the().on_bookmark_click = [this](auto& url, unsigned modifiers) {
        if (modifiers & Mod_Ctrl)
            on_tab_open_request(url);
        else
            load(url);
    };

    BookmarksBarWidget::the().on_bookmark_hover = [this](auto&, auto& url) {
        m_statusbar->set_text(url);
    };

    BookmarksBarWidget::the().remove_from_parent();
    m_toolbar_container->add_child(BookmarksBarWidget::the());

    auto is_fullscreen = window().is_fullscreen();
    m_toolbar_container->set_visible(!is_fullscreen);
    m_statusbar->set_visible(!is_fullscreen);

    update_actions();
}

void Tab::context_menu_requested(const Gfx::IntPoint& screen_position)
{
    m_tab_context_menu->popup(screen_position);
}

GUI::AbstractScrollableWidget& Tab::view()
{
    return *m_web_content_view;
}

Web::WebViewHooks& Tab::hooks()
{
    return *m_web_content_view;
}

void Tab::action_entered(GUI::Action& action)
{
    m_statusbar->set_override_text(action.status_tip());
}

void Tab::action_left(GUI::Action&)
{
    m_statusbar->set_override_text({});
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
        window->resize(300, 500);
        window->set_title("Inspector");
        window->set_icon(g_icon_bag.inspector_object);
        window->on_close = [&]() {
            m_web_content_view->clear_inspected_dom_node();
        };
        m_dom_inspector_widget = window->set_main_widget<InspectorWidget>();
        m_dom_inspector_widget->set_web_view(*m_web_content_view);
        m_web_content_view->inspect_dom_tree();
    }

    if (inspector_target == InspectorTarget::HoveredElement) {
        Optional<i32> hovered_node = m_web_content_view->get_hovered_node_id();
        VERIFY(hovered_node.has_value());
        m_dom_inspector_widget->set_inspected_node(hovered_node.value());
    } else {
        VERIFY(inspector_target == InspectorTarget::Document);
        m_dom_inspector_widget->select_default_node();
    }

    auto* window = m_dom_inspector_widget->window();
    window->show();
    window->move_to_front();
}

void Tab::show_console_window()
{
    if (!m_console_widget) {
        auto console_window = GUI::Window::construct(&window());
        console_window->resize(500, 300);
        console_window->set_title("JS Console");
        console_window->set_icon(g_icon_bag.filetype_javascript);
        m_console_widget = console_window->set_main_widget<ConsoleWidget>();
        m_console_widget->on_js_input = [this](String const& js_source) {
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

}
