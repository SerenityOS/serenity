/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include "InspectorWidget.h"
#include <LibCore/File.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/StatusBar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/ToolBar.h>
#include <LibGUI/Window.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/DOMTreeModel.h>
#include <LibHTML/Dump.h>
#include <LibHTML/HtmlView.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutDocument.h>
#include <LibHTML/Layout/LayoutInline.h>
#include <LibHTML/Layout/LayoutNode.h>
#include <LibHTML/Parser/CSSParser.h>
#include <LibHTML/Parser/HTMLParser.h>
#include <LibHTML/ResourceLoader.h>
#include <stdio.h>
#include <stdlib.h>

static const char* home_url = "file:///home/anon/www/welcome.html";

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer accept unix cpath rpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GUI::Application app(argc, argv);

    // Connect to the ProtocolServer immediately so we can drop the "unix" pledge.
    Web::ResourceLoader::the();

    if (pledge("stdio shared_buffer accept rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }


    auto window = GUI::Window::construct();
    window->set_rect(100, 100, 640, 480);

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);
    widget.set_layout<GUI::VerticalBoxLayout>();
    widget.layout()->set_spacing(0);

    auto& toolbar = widget.add<GUI::ToolBar>();
    auto& html_widget = widget.add<Web::HtmlView>();

    History<URL> history;

    RefPtr<GUI::Action> go_back_action;
    RefPtr<GUI::Action> go_forward_action;

    auto update_actions = [&]() {
        go_back_action->set_enabled(history.can_go_back());
        go_forward_action->set_enabled(history.can_go_forward());
    };

    bool should_push_loads_to_history = true;

    go_back_action = GUI::CommonActions::make_go_back_action([&](auto&) {
        history.go_back();
        update_actions();
        TemporaryChange<bool> change(should_push_loads_to_history, false);
        html_widget.load(history.current());
    });

    go_forward_action = GUI::CommonActions::make_go_forward_action([&](auto&) {
        history.go_forward();
        update_actions();
        TemporaryChange<bool> change(should_push_loads_to_history, false);
        html_widget.load(history.current());
    });

    toolbar.add_action(*go_back_action);
    toolbar.add_action(*go_forward_action);

    toolbar.add_action(GUI::CommonActions::make_go_home_action([&](auto&) {
        html_widget.load(home_url);
    }));

    toolbar.add_action(GUI::CommonActions::make_reload_action([&](auto&) {
        TemporaryChange<bool> change(should_push_loads_to_history, false);
        html_widget.reload();
    }));

    auto& location_box = toolbar.add<GUI::TextBox>();

    location_box.on_return_pressed = [&] {
        html_widget.load(location_box.text());
    };

    html_widget.on_load_start = [&](auto& url) {
        location_box.set_text(url.to_string());
        if (should_push_loads_to_history)
            history.push(url);
        update_actions();
    };

    html_widget.on_link_click = [&](auto& url) {
        if (url.starts_with("#")) {
            html_widget.scroll_to_anchor(url.substring_view(1, url.length() - 1));
        } else {
            html_widget.load(html_widget.document()->complete_url(url));
        }
    };

    html_widget.on_title_change = [&](auto& title) {
        window->set_title(String::format("%s - Browser", title.characters()));
    };

    auto focus_location_box_action = GUI::Action::create("Focus location box", { Mod_Ctrl, Key_L }, [&](auto&) {
        location_box.select_all();
        location_box.set_focus(true);
    });

    auto& statusbar = widget.add<GUI::StatusBar>();

    html_widget.on_link_hover = [&](auto& href) {
        statusbar.set_text(href);
    };

    Web::ResourceLoader::the().on_load_counter_change = [&] {
        if (Web::ResourceLoader::the().pending_loads() == 0) {
            statusbar.set_text("");
            return;
        }
        statusbar.set_text(String::format("Loading (%d pending resources...)", Web::ResourceLoader::the().pending_loads()));
    };

    auto menubar = make<GUI::MenuBar>();

    auto app_menu = GUI::Menu::construct("Browser");
    app_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app.quit();
    }));
    menubar->add_menu(move(app_menu));

    RefPtr<GUI::Window> dom_inspector_window;

    auto inspect_menu = GUI::Menu::construct("Inspect");
    inspect_menu->add_action(GUI::Action::create("View source", { Mod_Ctrl, Key_U }, [&](auto&) {
        String filename_to_open;
        char tmp_filename[] = "/tmp/view-source.XXXXXX";
        ASSERT(html_widget.document());
        if (html_widget.document()->url().protocol() == "file") {
            filename_to_open = html_widget.document()->url().path();
        } else {
            int fd = mkstemp(tmp_filename);
            ASSERT(fd >= 0);
            auto source = html_widget.document()->source();
            write(fd, source.characters(), source.length());
            close(fd);
            filename_to_open = tmp_filename;
        }
        if (fork() == 0) {
            execl("/bin/TextEditor", "TextEditor", filename_to_open.characters(), nullptr);
            ASSERT_NOT_REACHED();
        }
    }));
    inspect_menu->add_action(GUI::Action::create("Inspect DOM tree", { Mod_None, Key_F12 }, [&](auto&) {
        if (!dom_inspector_window) {
            dom_inspector_window = GUI::Window::construct();
            dom_inspector_window->set_rect(100, 100, 300, 500);
            dom_inspector_window->set_title("DOM inspector");
            dom_inspector_window->set_main_widget<InspectorWidget>();
        }
        auto* inspector_widget = static_cast<InspectorWidget*>(dom_inspector_window->main_widget());
        inspector_widget->set_document(html_widget.document());
        dom_inspector_window->show();
        dom_inspector_window->move_to_front();
    }));
    menubar->add_menu(move(inspect_menu));

    auto debug_menu = GUI::Menu::construct("Debug");
    debug_menu->add_action(GUI::Action::create("Dump DOM tree", [&](auto&) {
        dump_tree(*html_widget.document());
    }));
    debug_menu->add_action(GUI::Action::create("Dump Layout tree", [&](auto&) {
        dump_tree(*html_widget.document()->layout_node());
    }));
    debug_menu->add_action(GUI::Action::create("Dump Style sheets", [&](auto&) {
        for (auto& sheet : html_widget.document()->stylesheets()) {
            dump_sheet(sheet);
        }
    }));
    debug_menu->add_separator();
    auto line_box_borders_action = GUI::Action::create("Line box borders", [&](auto& action) {
        action.set_checked(!action.is_checked());
        html_widget.set_should_show_line_box_borders(action.is_checked());
        html_widget.update();
    });
    line_box_borders_action->set_checkable(true);
    line_box_borders_action->set_checked(false);
    debug_menu->add_action(line_box_borders_action);
    menubar->add_menu(move(debug_menu));

    auto help_menu = GUI::Menu::construct("Help");
    help_menu->add_action(GUI::Action::create("About", [&](const GUI::Action&) {
        GUI::AboutDialog::show("Browser", Gfx::Bitmap::load_from_file("/res/icons/32x32/filetype-html.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-html.png"));

    window->set_title("Browser");
    window->show();

    URL url_to_load = home_url;

    if (app.args().size() >= 1) {
        url_to_load = URL();
        url_to_load.set_protocol("file");
        url_to_load.set_path(app.args()[0]);
    }

    html_widget.load(url_to_load);

    return app.exec();
}
