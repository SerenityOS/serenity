/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "History.h"
#include "ManualModel.h"
#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/FilteringProxyModel.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/ToolBar.h>
#include <LibGUI/ToolBarContainer.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Window.h>
#include <LibMarkdown/Document.h>
#include <LibWeb/OutOfProcessWebView.h>
#include <libgen.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char* argv[])
{
    if (pledge("stdio recvfd sendfd accept rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd accept rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/usr/share/man", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/launch", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/webcontent", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    const char* start_page = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(start_page, "Page to open at launch", "page", Core::ArgsParser::Required::No);

    args_parser.parse(argc, argv);

    auto app_icon = GUI::Icon::default_icon("app-help");

    auto window = GUI::Window::construct();
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_title("Help");
    window->resize(570, 500);

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.set_layout<GUI::VerticalBoxLayout>();
    widget.set_fill_with_background_color(true);
    widget.layout()->set_spacing(2);

    auto& toolbar_container = widget.add<GUI::ToolBarContainer>();
    auto& toolbar = toolbar_container.add<GUI::ToolBar>();

    auto& splitter = widget.add<GUI::HorizontalSplitter>();

    auto model = ManualModel::create();

    auto& left_tab_bar = splitter.add<GUI::TabWidget>();
    auto& tree_view_container = left_tab_bar.add_tab<GUI::Widget>("Browse");
    tree_view_container.set_layout<GUI::VerticalBoxLayout>();
    tree_view_container.layout()->set_margins({ 4, 4, 4, 4 });
    auto& tree_view = tree_view_container.add<GUI::TreeView>();
    auto& search_view = left_tab_bar.add_tab<GUI::Widget>("Search");
    search_view.set_layout<GUI::VerticalBoxLayout>();
    search_view.layout()->set_margins({ 4, 4, 4, 4 });
    auto& search_box = search_view.add<GUI::TextBox>();
    auto& search_list_view = search_view.add<GUI::ListView>();
    search_box.set_fixed_height(20);
    search_box.set_placeholder("Search...");
    search_box.on_change = [&] {
        if (auto model = search_list_view.model()) {
            auto& search_model = *static_cast<GUI::FilteringProxyModel*>(model);
            search_model.set_filter_term(search_box.text());
            search_model.update();
        }
    };
    search_list_view.set_model(GUI::FilteringProxyModel::construct(model));
    search_list_view.model()->update();

    tree_view.set_model(model);
    left_tab_bar.set_fixed_width(200);

    auto& page_view = splitter.add<Web::OutOfProcessWebView>();

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
            page_view.load_empty_document();
            return;
        }

        auto source_result = model->page_view(path);
        if (source_result.is_error()) {
            GUI::MessageBox::show(window, source_result.error().string(), "Failed to open man page", GUI::MessageBox::Type::Error);
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
        page_view.load_html(html, url);

        auto tree_view_index = model->index_from_path(path);
        if (tree_view_index.has_value())
            tree_view.expand_tree(tree_view_index.value().parent());

        String page_and_section = model->page_and_section(tree_view_index.value());
        window->set_title(String::formatted("{} - Help", page_and_section));
    };

    tree_view.on_selection_change = [&] {
        String path = model->page_path(tree_view.selection().first());
        history.push(path);
        update_actions();
        open_page(path);
    };

    tree_view.on_toggle = [&](const GUI::ModelIndex& index, const bool open) {
        model->update_section_node_on_toggle(index, open);
    };

    auto open_external = [&](auto& url) {
        if (!Desktop::Launcher::open(url)) {
            GUI::MessageBox::show(window,
                String::formatted("The link to '{}' could not be opened.", url),
                "Failed to open link",
                GUI::MessageBox::Type::Error);
        }
    };
    search_list_view.on_selection = [&](auto index) {
        if (!index.is_valid())
            return;

        if (auto model = search_list_view.model()) {
            auto& search_model = *static_cast<GUI::FilteringProxyModel*>(model);
            index = search_model.map(index);
        } else {
            page_view.load_empty_document();
            return;
        }
        String path = model->page_path(index);
        if (path.is_null()) {
            page_view.load_empty_document();
            return;
        }
        tree_view.selection().clear();
        tree_view.selection().add(index);
        history.push(path);
        update_actions();
        open_page(path);
    };

    page_view.on_link_click = [&](auto& url, auto&, unsigned) {
        if (url.protocol() != "file") {
            open_external(url);
            return;
        }
        auto path = Core::File::real_path_for(url.path());
        if (!path.starts_with("/usr/share/man/")) {
            open_external(url);
            return;
        }
        auto tree_view_index = model->index_from_path(path);
        if (tree_view_index.has_value()) {
            dbgln("Found path _{}_ in model at index {}", path, tree_view_index.value());
            tree_view.selection().set(tree_view_index.value());
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

    toolbar.add_action(*go_back_action);
    toolbar.add_action(*go_forward_action);
    toolbar.add_action(*go_home_action);

    auto menubar = GUI::MenuBar::construct();

    auto& app_menu = menubar->add_menu("File");
    app_menu.add_action(GUI::CommonActions::make_about_action("Help", app_icon, window));
    app_menu.add_separator();
    app_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& go_menu = menubar->add_menu("Go");
    go_menu.add_action(*go_back_action);
    go_menu.add_action(*go_forward_action);
    go_menu.add_action(*go_home_action);

    window->set_menubar(move(menubar));

    if (start_page) {
        URL url = URL::create_with_url_or_path(start_page);
        if (url.is_valid() && url.path().ends_with(".md")) {
            history.push(url.path());
            update_actions();
            open_page(url.path());
        } else {
            left_tab_bar.set_active_widget(&search_view);
            search_box.set_text(start_page);
            if (auto model = search_list_view.model()) {
                auto& search_model = *static_cast<GUI::FilteringProxyModel*>(model);
                search_model.set_filter_term(search_box.text());
            }
        }
    } else {
        go_home_action->activate();
    }

    window->set_focused_widget(&left_tab_bar);
    window->show();

    return app->exec();
}
