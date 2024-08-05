/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibMain/Main.h>
#include <LibManual/Node.h>
#include <LibManual/PageNode.h>
#include <LibManual/Path.h>
#include <LibManual/SectionNode.h>
#include <LibMarkdown/Document.h>
#include <LibURL/URL.h>

namespace Help {

MainWidget::MainWidget()
{
}

ErrorOr<void> MainWidget::set_start_page(Vector<StringView, 2> query_parameters)
{
    auto result = Manual::Node::try_create_from_query(query_parameters);
    if (result.is_error()) {
        // No match, so treat the input as a search query
        m_tab_widget->set_active_widget(m_search_container);
        m_search_box->set_focus(true);
        m_search_box->set_text(query_parameters.first_matching([](auto&) { return true; }).value_or(""sv));
        m_search_box->select_all();
        m_filter_model->set_filter_term(m_search_box->text());
        m_go_home_action->activate();
    } else {
        auto const page = TRY(result.value()->path());
        m_history.push(page);
        open_page(page);
    }
    return {};
}

ErrorOr<void> MainWidget::initialize(GUI::Window& window)
{
    m_toolbar = find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    m_tab_widget = find_descendant_of_type_named<GUI::TabWidget>("tab_widget");
    m_search_container = find_descendant_of_type_named<GUI::Widget>("search_container");

    m_search_box = find_descendant_of_type_named<GUI::TextBox>("search_box");
    m_search_box->on_change = [this] {
        m_filter_model->set_filter_term(m_search_box->text());
    };
    m_search_box->on_down_pressed = [this] {
        m_search_view->move_cursor(GUI::AbstractView::CursorMovement::Down, GUI::AbstractView::SelectionUpdate::Set);
    };
    m_search_box->on_up_pressed = [this] {
        m_search_view->move_cursor(GUI::AbstractView::CursorMovement::Up, GUI::AbstractView::SelectionUpdate::Set);
    };

    m_search_view = find_descendant_of_type_named<GUI::ListView>("search_view");
    m_search_view->set_should_hide_unnecessary_scrollbars(true);
    m_search_view->on_selection_change = [this] {
        auto const& index = m_search_view->selection().first();
        if (!index.is_valid())
            return;

        auto* view_model = m_search_view->model();
        if (!view_model) {
            m_web_view->load_empty_document();
            return;
        }
        auto& search_model = *static_cast<GUI::FilteringProxyModel*>(view_model);
        auto const& mapped_index = search_model.map(index);
        auto path = m_manual_model->page_path(mapped_index);
        if (!path.has_value()) {
            m_web_view->load_empty_document();
            return;
        }
        m_browse_view->selection().clear();
        m_browse_view->selection().add(mapped_index);
        m_history.push(path.value());
        open_page(path.value());
    };

    m_browse_view = find_descendant_of_type_named<GUI::TreeView>("browse_view");
    m_browse_view->on_selection_change = [this] {
        auto path = m_manual_model->page_path(m_browse_view->selection().first());
        if (!path.has_value())
            return;

        m_history.push(path.value());
        open_page(path.value());
    };
    m_browse_view->on_toggle = [this](GUI::ModelIndex const& index, bool open) {
        m_manual_model->update_section_node_on_toggle(index, open);
    };

    m_web_view = find_descendant_of_type_named<WebView::OutOfProcessWebView>("web_view");
    m_web_view->use_native_user_style_sheet();
    m_web_view->on_link_click = [this](auto& url, auto&, unsigned) {
        if (url.scheme() == "file") {
            auto path = LexicalPath { URL::percent_decode(url.serialize_path()) };
            if (!path.is_child_of(Manual::manual_base_path)) {
                open_external(url);
                return;
            }
            auto browse_view_index = m_manual_model->index_from_path(path.string());
            if (browse_view_index.has_value()) {
                dbgln("Found path _{}_ in m_manual_model at index {}", path, browse_view_index.value());
                m_browse_view->selection().set(browse_view_index.value());
                return;
            }
            m_history.push(path.string());
            auto string_path = String::from_byte_string(path.string());
            if (string_path.is_error())
                return;
            open_page(string_path.value());
        } else if (url.scheme() == "help") {
            auto maybe_page = Manual::Node::try_find_from_help_url(url);
            if (maybe_page.is_error()) {
                dbgln("Error opening page: {}", maybe_page.error());
                return;
            }
            auto maybe_path = maybe_page.value()->path();
            if (maybe_path.is_error())
                return;
            open_page(maybe_path.release_value());
        } else {
            open_external(url);
        }
    };
    m_web_view->on_context_menu_request = [this](auto screen_position) {
        m_copy_action->set_enabled(!m_web_view->selected_text().is_empty());
        m_context_menu->popup(screen_position);
    };
    m_web_view->on_link_hover = [this](URL::URL const& url) {
        if (url.is_valid())
            m_statusbar->set_text(String::from_byte_string(url.to_byte_string()).release_value_but_fixme_should_propagate_errors());
        else
            m_statusbar->set_text({});
    };
    m_web_view->on_link_unhover = [this] {
        m_statusbar->set_text({});
    };

    m_go_back_action = GUI::CommonActions::make_go_back_action([this](auto&) {
        m_history.go_back();
        open_page(MUST(String::from_byte_string(m_history.current())));
    });

    m_go_forward_action = GUI::CommonActions::make_go_forward_action([this](auto&) {
        m_history.go_forward();
        open_page(MUST(String::from_byte_string(m_history.current())));
    });

    m_go_back_action->set_enabled(false);
    m_go_forward_action->set_enabled(false);

    m_copy_action = GUI::CommonActions::make_copy_action([this](auto&) {
        auto selected_text = m_web_view->selected_text();
        if (!selected_text.is_empty())
            GUI::Clipboard::the().set_plain_text(selected_text);
    });

    m_select_all_action = GUI::CommonActions::make_select_all_action([this](auto&) {
        m_web_view->select_all();
    });

    m_statusbar = find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    GUI::Application::the()->on_action_enter = [this](GUI::Action const& action) {
        m_statusbar->set_override_text(action.status_tip());
    };
    GUI::Application::the()->on_action_leave = [this](GUI::Action const&) {
        m_statusbar->set_override_text({});
    };

    static String const help_index_path = TRY(TRY(Manual::PageNode::help_index_page())->path());
    m_go_home_action = GUI::CommonActions::make_go_home_action([this](auto&) {
        m_history.push(help_index_path);
        open_page(help_index_path);
    });

    m_toolbar->add_action(*m_go_back_action);
    m_toolbar->add_action(*m_go_forward_action);
    m_toolbar->add_action(*m_go_home_action);

    auto file_menu = window.add_menu("&File"_string);
    file_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto view_menu = window.add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window.set_fullscreen(!window.is_fullscreen());
    }));

    auto go_menu = window.add_menu("&Go"_string);
    go_menu->add_action(*m_go_back_action);
    go_menu->add_action(*m_go_forward_action);
    go_menu->add_action(*m_go_home_action);

    auto help_menu = window.add_menu("&Help"_string);
    String help_page_path = TRY(TRY(try_make_ref_counted<Manual::PageNode>(Manual::sections[1 - 1], "Applications/Help"_string))->path());
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(&window));
    help_menu->add_action(GUI::Action::create("&Contents", { Key_F1 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-unknown.png"sv)), [this, help_page_path = move(help_page_path)](auto&) {
        open_page(help_page_path);
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Help"_string, TRY(GUI::Icon::try_create_default_icon("app-help"sv)), &window));

    m_context_menu = GUI::Menu::construct();
    m_context_menu->add_action(*m_go_back_action);
    m_context_menu->add_action(*m_go_forward_action);
    m_context_menu->add_action(*m_go_home_action);
    m_context_menu->add_separator();
    m_context_menu->add_action(*m_copy_action);
    m_context_menu->add_action(*m_select_all_action);

    m_manual_model = TRY(ManualModel::create());
    m_browse_view->set_model(*m_manual_model);
    m_filter_model = TRY(GUI::FilteringProxyModel::create(*m_manual_model, GUI::FilteringProxyModel::FilteringOptions::SortByScore));
    m_search_view->set_model(*m_filter_model);
    m_filter_model->set_filter_term(""sv);

    return {};
}

void MainWidget::open_url(URL::URL const& url)
{
    m_go_back_action->set_enabled(m_history.can_go_back());
    m_go_forward_action->set_enabled(m_history.can_go_forward());

    if (url.scheme() == "file") {
        m_web_view->load(url);

        auto browse_view_index = m_manual_model->index_from_path(URL::percent_decode(url.serialize_path()));
        if (browse_view_index.has_value()) {
            if (browse_view_index.value() != m_browse_view->selection_start_index()) {
                m_browse_view->expand_all_parents_of(browse_view_index.value());
                m_browse_view->set_cursor(browse_view_index.value(), GUI::AbstractView::SelectionUpdate::Set);
            }

            auto page_and_section = m_manual_model->page_and_section(browse_view_index.value());
            if (!page_and_section.has_value())
                return;
            auto title = String::formatted("{} - Help", page_and_section.value());
            if (!title.is_error())
                window()->set_title(title.release_value().to_byte_string());
        } else {
            window()->set_title("Help");
        }
    }
}

void MainWidget::open_external(URL::URL const& url)
{
    if (!Desktop::Launcher::open(url))
        GUI::MessageBox::show(window(), ByteString::formatted("The link to '{}' could not be opened.", url), "Failed to open link"sv, GUI::MessageBox::Type::Error);
}

void MainWidget::open_page(Optional<String> const& path)
{
    m_go_back_action->set_enabled(m_history.can_go_back());
    m_go_forward_action->set_enabled(m_history.can_go_forward());

    if (!path.has_value()) {
        window()->set_title("Help");
        m_web_view->load_empty_document();
        return;
    }
    dbgln("open page: {}", path.value());
    open_url(URL::create_with_url_or_path(path.value().to_byte_string()));
}

}
