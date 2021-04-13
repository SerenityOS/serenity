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

#include "RemoteObject.h"
#include "RemoteObjectGraphModel.h"
#include "RemoteObjectPropertyModel.h"
#include "RemoteProcess.h"
#include <AK/URL.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/ModelEditingDelegate.h>
#include <LibGUI/ProcessChooser.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Window.h>
#include <stdio.h>

using namespace Inspector;

[[noreturn]] static void print_usage_and_exit()
{
    outln("usage: Inspector <pid>");
    exit(0);
}

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd rpath accept unix cpath fattr", nullptr) < 0) {
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

    pid_t pid;

    auto app = GUI::Application::construct(argc, argv);
    auto app_icon = GUI::Icon::default_icon("app-inspector");
    if (argc != 2) {
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

    if (!Desktop::Launcher::add_allowed_handler_with_only_specific_urls(
            "/bin/Help",
            { URL::create_with_file_protocol("/usr/share/man/man1/Inspector.md") })
        || !Desktop::Launcher::seal_allowlist()) {
        warnln("Failed to set up allowed launch URLs");
        return 1;
    }

    auto all_processes = Core::ProcessStatisticsReader::get_all();
    for (auto& it : all_processes.value()) {
        if (it.value.pid != pid)
            continue;
        if (it.value.pledge.is_empty())
            break;
        if (!it.value.pledge.contains("accept")) {
            GUI::MessageBox::show(window, String::formatted("{} ({}) has not pledged accept!", it.value.name, pid), "Error", GUI::MessageBox::Type::Error);
            return 1;
        }
        break;
    }

    window->set_title("Inspector");
    window->resize(685, 500);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto menubar = GUI::Menubar::construct();
    auto& app_menu = menubar->add_menu("File");
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/Inspector.md"), "/bin/Help");
    }));
    help_menu.add_action(GUI::CommonActions::make_about_action("Inspector", app_icon, window));

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);
    widget.set_layout<GUI::VerticalBoxLayout>();

    auto& splitter = widget.add<GUI::HorizontalSplitter>();

    RemoteProcess remote_process(pid);

    remote_process.on_update = [&] {
        if (!remote_process.process_name().is_null())
            window->set_title(String::formatted("{} ({}) - Inspector", remote_process.process_name(), remote_process.pid()));
    };

    auto& tree_view = splitter.add<GUI::TreeView>();
    tree_view.set_model(remote_process.object_graph_model());
    tree_view.set_activates_on_selection(true);
    tree_view.set_fixed_width(286);

    auto& properties_tree_view = splitter.add<GUI::TreeView>();
    properties_tree_view.set_editable(true);
    properties_tree_view.aid_create_editing_delegate = [](auto&) {
        return make<GUI::StringModelEditingDelegate>();
    };

    tree_view.on_activation = [&](auto& index) {
        auto* remote_object = static_cast<RemoteObject*>(index.internal_data());
        properties_tree_view.set_model(remote_object->property_model());
        remote_process.set_inspected_object(remote_object->address);
    };

    window->set_menubar(move(menubar));
    window->show();
    remote_process.update();

    if (pledge("stdio recvfd sendfd rpath accept unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    return app->exec();
}
