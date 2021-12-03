/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "History.h"
#include "ManualModel.h"
#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/FilteringProxyModel.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <LibMarkdown/Document.h>
#include <LibWeb/OutOfProcessWebView.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix"));
    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/usr/share/man", "r"));
    TRY(Core::System::unveil("/tmp/portal/launch", "rw"));
    TRY(Core::System::unveil("/tmp/portal/webcontent", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    const char* start_page = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(start_page, "Page to open at launch", "page", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app_icon = GUI::Icon::default_icon("app-help");

    auto window = TRY(GUI::Window::try_create());
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_title("Help");
    window->resize(570, 500);

    auto widget = TRY(window->try_set_main_widget<GUI::Widget>());
    (void)TRY(widget->try_set_layout<GUI::VerticalBoxLayout>());
    widget->set_fill_with_background_color(true);
    widget->layout()->set_spacing(2);

    auto toolbar_container = TRY(widget->try_add<GUI::ToolbarContainer>());
    auto toolbar = TRY(toolbar_container->try_add<GUI::Toolbar>());

    auto splitter = TRY(widget->try_add<GUI::HorizontalSplitter>());
    splitter->layout()->set_spacing(5);

    auto manual_model = ManualModel::create();

    auto left_tab_bar = TRY(splitter->try_add<GUI::TabWidget>());
    auto tree_view_container = TRY(left_tab_bar->try_add_tab<GUI::Widget>("Browse"));
    (void)TRY(tree_view_container->try_set_layout<GUI::VerticalBoxLayout>());
    tree_view_container->layout()->set_margins(4);
    auto tree_view = TRY(tree_view_container->try_add<GUI::TreeView>());
    auto search_view = TRY(left_tab_bar->try_add_tab<GUI::Widget>("Search"));
    (void)TRY(search_view->try_set_layout<GUI::VerticalBoxLayout>());
    search_view->layout()->set_margins(4);
    auto search_box = TRY(search_view->try_add<GUI::TextBox>());
    auto search_list_view = TRY(search_view->try_add<GUI::ListView>());
    search_box->set_fixed_height(20);
    search_box->set_placeholder("Search...");
    search_box->on_change = [&] {
        if (auto* model = search_list_view->model()) {
            auto& search_model = *static_cast<GUI::FilteringProxyModel*>(model);
            search_model.set_filter_term(search_box->text());
            search_model.invalidate();
        }
    };
    search_list_view->set_model(TRY(GUI::FilteringProxyModel::create(manual_model)));
    search_list_view->model()->invalidate();

    tree_view->set_model(manual_model);
    left_tab_bar->set_fixed_width(200);

    auto page_view = TRY(splitter->try_add<Web::OutOfProcessWebView>());

    History history;

    RefPtr<GUI::Action> go_back_action;
    RefPtr<GUI::Action> go_forward_action;

    auto update_actions = [&]() {
        go_back_action->set_enabled(history.can_go_back());
        go_forward_action->set_enabled(history.can_go_forward());
    };

    auto open_page = [&](const String& path) {
        if (path.is_null()) {
            window->set_title("Help");
            page_view->load_empty_document();
            return;
        }

        auto source_result = manual_model->page_view(path);
        if (source_result.is_error()) {
            GUI::MessageBox::show(window, String::formatted("{}", source_result.error()), "Failed to open man page", GUI::MessageBox::Type::Error);
            return;
        }

        auto source = source_result.value();
        String html;
        {
            auto md_document = Markdown::Document::parse(source);
            VERIFY(md_document);
            html = md_document->render_to_html();
        }

        auto url = URL::create_with_file_protocol(path);
        page_view->load_html(html, url);

        app->deferred_invoke([&, path] {
            auto tree_view_index = manual_model->index_from_path(path);
            if (tree_view_index.has_value()) {
                tree_view->expand_tree(tree_view_index.value().parent());
                tree_view->selection().set(tree_view_index.value());

                String page_and_section = manual_model->page_and_section(tree_view_index.value());
                window->set_title(String::formatted("{} - Help", page_and_section));
            } else {
                window->set_title("Help");
            }
        });
    };

    tree_view->on_selection_change = [&] {
        String path = manual_model->page_path(tree_view->selection().first());
        if (path.is_null())
            return;

        history.push(path);
        update_actions();
        open_page(path);
    };

    tree_view->on_toggle = [&](GUI::ModelIndex const& index, bool open) {
        manual_model->update_section_node_on_toggle(index, open);
    };

    auto open_external = [&](auto& url) {
        if (!Desktop::Launcher::open(url)) {
            GUI::MessageBox::show(window,
                String::formatted("The link to '{}' could not be opened.", url),
                "Failed to open link",
                GUI::MessageBox::Type::Error);
        }
    };
    search_list_view->on_selection_change = [&] {
        auto const& index = search_list_view->selection().first();
        if (!index.is_valid())
            return;

        auto* view_model = search_list_view->model();
        if (!view_model) {
            page_view->load_empty_document();
            return;
        }
        auto& search_model = *static_cast<GUI::FilteringProxyModel*>(view_model);
        const auto& mapped_index = search_model.map(index);
        String path = manual_model->page_path(mapped_index);
        if (path.is_null()) {
            page_view->load_empty_document();
            return;
        }
        tree_view->selection().clear();
        tree_view->selection().add(mapped_index);
        history.push(path);
        update_actions();
        open_page(path);
    };

    page_view->on_link_click = [&](auto& url, auto&, unsigned) {
        if (url.protocol() != "file") {
            open_external(url);
            return;
        }
        auto path = Core::File::real_path_for(url.path());
        if (!path.starts_with("/usr/share/man/")) {
            open_external(url);
            return;
        }
        auto tree_view_index = manual_model->index_from_path(path);
        if (tree_view_index.has_value()) {
            dbgln("Found path _{}_ in manual_model at index {}", path, tree_view_index.value());
            tree_view->selection().set(tree_view_index.value());
            return;
        }
        history.push(path);
        update_actions();
        open_page(path);
    };

    go_back_action = GUI::CommonActions::make_go_back_action([&](auto&) {
        history.go_back();
        update_actions();
        open_page(history.current());
    });

    go_forward_action = GUI::CommonActions::make_go_forward_action([&](auto&) {
        history.go_forward();
        update_actions();
        open_page(history.current());
    });

    go_back_action->set_enabled(false);
    go_forward_action->set_enabled(false);

    auto go_home_action = GUI::CommonActions::make_go_home_action([&](auto&) {
        String path = "/usr/share/man/man7/Help-index.md";
        history.push(path);
        update_actions();
        open_page(path);
    });

    (void)TRY(toolbar->try_add_action(*go_back_action));
    (void)TRY(toolbar->try_add_action(*go_forward_action));
    (void)TRY(toolbar->try_add_action(*go_home_action));

    auto file_menu = TRY(window->try_add_menu("&File"));
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    })));

    auto go_menu = TRY(window->try_add_menu("&Go"));
    TRY(go_menu->try_add_action(*go_back_action));
    TRY(go_menu->try_add_action(*go_forward_action));
    TRY(go_menu->try_add_action(*go_home_action));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Help", app_icon, window)));

    auto context_menu = TRY(GUI::Menu::try_create());
    TRY(context_menu->try_add_action(*go_back_action));
    TRY(context_menu->try_add_action(*go_forward_action));
    TRY(context_menu->try_add_action(*go_home_action));
    TRY(context_menu->try_add_separator());

    RefPtr<GUI::Action> copy_action = GUI::CommonActions::make_copy_action([&](auto&) {
        auto selected_text = page_view->selected_text();
        if (!selected_text.is_empty())
            GUI::Clipboard::the().set_plain_text(selected_text);
    });
    TRY(context_menu->try_add_action(*copy_action));

    RefPtr<GUI::Action> select_all_function = GUI::CommonActions::make_select_all_action([&](auto&) {
        page_view->select_all();
    });
    TRY(context_menu->try_add_action(*select_all_function));

    page_view->on_context_menu_request = [&](auto& screen_position) {
        copy_action->set_enabled(!page_view->selected_text().is_empty());
        context_menu->popup(screen_position);
    };

    if (start_page) {
        URL url = URL::create_with_url_or_path(start_page);
        if (url.is_valid() && url.path().ends_with(".md")) {
            history.push(url.path());
            update_actions();
            open_page(url.path());
        } else {
            left_tab_bar->set_active_widget(search_view);
            search_box->set_text(start_page);
            if (auto* model = search_list_view->model()) {
                auto& search_model = *static_cast<GUI::FilteringProxyModel*>(model);
                search_model.set_filter_term(search_box->text());
            }
        }
    } else {
        go_home_action->activate();
    }

    auto statusbar = TRY(widget->try_add<GUI::Statusbar>());
    app->on_action_enter = [&statusbar](GUI::Action const& action) {
        statusbar->set_override_text(action.status_tip());
    };
    app->on_action_leave = [&statusbar](GUI::Action const&) {
        statusbar->set_override_text({});
    };

    page_view->on_link_hover = [&](URL const& url) {
        if (url.is_valid())
            statusbar->set_text(url.to_string());
        else
            statusbar->set_text({});
    };

    window->set_focused_widget(left_tab_bar);
    window->show();

    return app->exec();
}
