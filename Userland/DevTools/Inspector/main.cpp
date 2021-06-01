/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RemoteObject.h"
#include "RemoteObjectGraphModel.h"
#include "RemoteObjectPropertyModel.h"
#include "RemoteProcess.h"
#include <AK/URL.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/ModelEditingDelegate.h>
#include <LibGUI/ProcessChooser.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Window.h>
#include <stdio.h>
#include <unistd.h>

using namespace Inspector;

[[noreturn]] static void print_usage_and_exit()
{
    outln("usage: Inspector <pid>");
    exit(0);
}

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp", "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/proc/all", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    bool gui_mode = argc != 2;
    pid_t pid;

    auto app = GUI::Application::construct(argc, argv);
    auto app_icon = GUI::Icon::default_icon("app-inspector");
    if (gui_mode) {
    choose_pid:
        auto process_chooser = GUI::ProcessChooser::construct("Inspector", "Inspect", app_icon.bitmap_for_size(16));
        if (process_chooser->exec() == GUI::Dialog::ExecCancel)
            return 0;
        pid = process_chooser->pid();
    } else {
        auto pid_opt = String(argv[1]).to_int();
        if (!pid_opt.has_value())
            print_usage_and_exit();
        pid = pid_opt.value();
    }

    auto window = GUI::Window::construct();

    if (pid == getpid()) {
        GUI::MessageBox::show(window, "Cannot inspect Inspector itself!", "Error", GUI::MessageBox::Type::Error);
        return 1;
    }

    RemoteProcess remote_process(pid);
    if (!remote_process.is_inspectable()) {
        GUI::MessageBox::show(window, String::formatted("Process pid={} is not inspectable", remote_process.pid()), "Error", GUI::MessageBox::Type::Error);
        if (gui_mode) {
            goto choose_pid;
        } else {
            return 1;
        }
    }

    if (!Desktop::Launcher::add_allowed_handler_with_only_specific_urls(
            "/bin/Help",
            { URL::create_with_file_protocol("/usr/share/man/man1/Inspector.md") })
        || !Desktop::Launcher::seal_allowlist()) {
        warnln("Failed to set up allowed launch URLs");
        return 1;
    }

    window->set_title("Inspector");
    window->resize(685, 500);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto menubar = GUI::Menubar::construct();

    auto& file_menu = menubar->add_menu("&File");
    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/Inspector.md"), "/bin/Help");
    }));
    help_menu.add_action(GUI::CommonActions::make_about_action("Inspector", app_icon, window));

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);
    widget.set_layout<GUI::VerticalBoxLayout>();

    auto& splitter = widget.add<GUI::HorizontalSplitter>();

    remote_process.on_update = [&] {
        if (!remote_process.process_name().is_null())
            window->set_title(String::formatted("{} ({}) - Inspector", remote_process.process_name(), remote_process.pid()));
    };

    auto& tree_view = splitter.add<GUI::TreeView>();
    tree_view.set_model(remote_process.object_graph_model());
    tree_view.set_activates_on_selection(true);
    tree_view.set_fixed_width(286);

    auto& properties_tree_view = splitter.add<GUI::TreeView>();
    properties_tree_view.set_should_fill_selected_rows(true);
    properties_tree_view.set_editable(true);
    properties_tree_view.aid_create_editing_delegate = [](auto&) {
        return make<GUI::StringModelEditingDelegate>();
    };

    tree_view.on_activation = [&](auto& index) {
        auto* remote_object = static_cast<RemoteObject*>(index.internal_data());
        properties_tree_view.set_model(remote_object->property_model());
        remote_process.set_inspected_object(remote_object->address);
    };

    auto properties_tree_view_context_menu = GUI::Menu::construct("Properties Tree View");

    auto copy_bitmap = Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-copy.png");
    auto copy_property_name_action = GUI::Action::create("Copy Property Name", copy_bitmap, [&](auto&) {
        GUI::Clipboard::the().set_plain_text(properties_tree_view.selection().first().data().to_string());
    });
    auto copy_property_value_action = GUI::Action::create("Copy Property Value", copy_bitmap, [&](auto&) {
        GUI::Clipboard::the().set_plain_text(properties_tree_view.selection().first().sibling_at_column(1).data().to_string());
    });

    properties_tree_view_context_menu->add_action(copy_property_name_action);
    properties_tree_view_context_menu->add_action(copy_property_value_action);

    properties_tree_view.on_context_menu_request = [&](const GUI::ModelIndex& index, const GUI::ContextMenuEvent& event) {
        if (index.is_valid()) {
            properties_tree_view_context_menu->popup(event.screen_position());
        }
    };

    window->set_menubar(move(menubar));
    window->show();
    remote_process.update();

    if (pledge("stdio recvfd sendfd rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    return app->exec();
}
