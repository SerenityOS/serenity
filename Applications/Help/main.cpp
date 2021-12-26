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
#include <AK/ByteBuffer.h>
#include <AK/URL.h>
#include <LibCore/File.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/ToolBar.h>
#include <LibGUI/ToolBarContainer.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Window.h>
#include <LibMarkdown/Document.h>
#include <LibWeb/Layout/LayoutNode.h>
#include <LibWeb/PageView.h>
#include <libgen.h>
#include <stdio.h>
#include <string.h>

int main(int argc, char* argv[])
{
    if (pledge("stdio shared_buffer accept rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio shared_buffer accept rpath", nullptr) < 0) {
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

    unveil(nullptr, nullptr);

    auto window = GUI::Window::construct();
    window->set_title("Help");
    window->set_rect(300, 200, 570, 500);

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.set_layout<GUI::VerticalBoxLayout>();
    widget.set_fill_with_background_color(true);
    widget.layout()->set_spacing(2);

    auto& toolbar_container = widget.add<GUI::ToolBarContainer>();
    auto& toolbar = toolbar_container.add<GUI::ToolBar>();

    auto& splitter = widget.add<GUI::HorizontalSplitter>();

    auto model = ManualModel::create();

    auto& tree_view = splitter.add<GUI::TreeView>();
    tree_view.set_model(model);
    tree_view.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    tree_view.set_preferred_size(200, 500);

    auto& page_view = splitter.add<Web::PageView>();

    History history;

    RefPtr<GUI::Action> go_back_action;
    RefPtr<GUI::Action> go_forward_action;

    auto update_actions = [&]() {
        go_back_action->set_enabled(history.can_go_back());
        go_forward_action->set_enabled(history.can_go_forward());
    };

    auto open_page = [&](const String& path) {
        if (path.is_null()) {
            page_view.set_document(nullptr);
            return;
        }

        dbg() << "Opening page at " << path;

        auto file = Core::File::construct();
        file->set_filename(path);

        if (!file->open(Core::IODevice::OpenMode::ReadOnly)) {
            int saved_errno = errno;
            GUI::MessageBox::show(strerror(saved_errno), "Failed to open man page", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, window);
            return;
        }
        auto buffer = file->read_all();
        StringView source { (const char*)buffer.data(), buffer.size() };

        String html;
        {
            auto md_document = Markdown::Document::parse(source);
            ASSERT(md_document);
            html = md_document->render_to_html();
        }

        page_view.load_html(html, URL::create_with_file_protocol(path));

        String page_and_section = model->page_and_section(tree_view.selection().first());
        window->set_title(String::format("%s - Help", page_and_section.characters()));
    };

    tree_view.on_selection_change = [&] {
        String path = model->page_path(tree_view.selection().first());
        if (path.is_null()) {
            page_view.set_document(nullptr);
            return;
        }
        history.push(path);
        update_actions();
        open_page(path);
    };

    page_view.on_link_click = [&](const String& href, auto&, unsigned) {
        char* current_path = strdup(history.current().characters());
        char* dir_path = dirname(current_path);
        char* path = realpath(String::format("%s/%s", dir_path, href.characters()).characters(), nullptr);
        free(current_path);
        auto tree_view_index = model->index_from_path(path);
        if (tree_view_index.has_value()) {
            dbg() << "Found path _" << path << "_ in model at index " << tree_view_index.value();
            tree_view.selection().set(tree_view_index.value());
            return;
        }
        history.push(path);
        update_actions();
        open_page(path);
        free(path);
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

    toolbar.add_action(*go_back_action);
    toolbar.add_action(*go_forward_action);

    auto menubar = GUI::MenuBar::construct();

    auto& app_menu = menubar->add_menu("Help");
    app_menu.add_action(GUI::Action::create("About", [&](const GUI::Action&) {
        GUI::AboutDialog::show("Help", Gfx::Bitmap::load_from_file("/res/icons/16x16/book.png"), window);
    }));
    app_menu.add_separator();
    app_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& go_menu = menubar->add_menu("Go");
    go_menu.add_action(*go_back_action);
    go_menu.add_action(*go_forward_action);

    app->set_menubar(move(menubar));

    window->set_focused_widget(&tree_view);
    window->show();

    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/book.png"));

    return app->exec();
}
