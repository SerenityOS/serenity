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

#include "Profile.h"
#include "ProfileTimelineWidget.h"
#include "RunningProcessesModel.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Timer.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Model.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Window.h>
#include <serenity.h>
#include <stdio.h>
#include <string.h>

static bool generate_profile(pid_t specified_pid);

int main(int argc, char** argv)
{
    Core::ArgsParser args_parser;
    int pid = 0;
    args_parser.add_option(pid, "PID to profile", "pid", 'p', "PID");
    args_parser.parse(argc, argv, false);

    GUI::Application app(argc, argv);

    const char* path = nullptr;
    if (argc != 2) {
        if (!generate_profile(pid))
            return 0;
        path = "/proc/profile";
    } else {
        path = argv[1];
    }

    auto profile = Profile::load_from_perfcore_file(path);

    if (!profile) {
        fprintf(stderr, "Unable to load profile '%s'\n", path);
        return 1;
    }

    auto window = GUI::Window::construct();
    window->set_title("Profiler");
    window->set_rect(100, 100, 800, 600);

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();

    main_widget.add<ProfileTimelineWidget>(*profile);

    auto& bottom_splitter = main_widget.add<GUI::VerticalSplitter>();

    auto& tree_view = bottom_splitter.add<GUI::TreeView>();
    tree_view.set_headers_visible(true);
    tree_view.set_model(profile->model());

    auto& disassembly_view = bottom_splitter.add<GUI::TableView>();

    tree_view.on_selection = [&](auto& index) {
        profile->set_disassembly_index(index);
        disassembly_view.set_model(profile->disassembly_model());
    };

    auto menubar = GUI::MenuBar::construct();
    auto& app_menu = menubar->add_menu("Profiler");
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app.quit(); }));

    auto& view_menu = menubar->add_menu("View");
    auto invert_action = GUI::Action::create_checkable("Invert tree", { Mod_Ctrl, Key_I }, [&](auto& action) {
        profile->set_inverted(action.is_checked());
    });
    invert_action->set_checked(false);
    view_menu.add_action(invert_action);

    auto percent_action = GUI::Action::create_checkable("Show percentages", { Mod_Ctrl, Key_P }, [&](auto& action) {
        profile->set_show_percentages(action.is_checked());
        tree_view.update();
        disassembly_view.update();
    });
    percent_action->set_checked(false);
    view_menu.add_action(percent_action);

    app.set_menubar(move(menubar));

    window->show();
    return app.exec();
}

pid_t prompt_for_process_to_profile()
{
    pid_t pid = 0;
    auto window = GUI::Window::construct();
    window->set_title("Profiler");
    Gfx::IntRect window_rect { 0, 0, 480, 360 };
    window_rect.center_within(GUI::Desktop::the().rect());
    window->set_rect(window_rect);
    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);
    widget.set_layout<GUI::VerticalBoxLayout>();
    auto& table_view = widget.add<GUI::TableView>();
    table_view.set_model(GUI::SortingProxyModel::create(Profiler::RunningProcessesModel::create()));
    table_view.model()->set_key_column_and_sort_order(Profiler::RunningProcessesModel::Column::PID, GUI::SortOrder::Descending);
    auto& button_container = widget.add<GUI::Widget>();
    button_container.set_preferred_size(0, 30);
    button_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    button_container.set_layout<GUI::HorizontalBoxLayout>();
    auto& profile_button = button_container.add<GUI::Button>("Profile");
    profile_button.on_click = [&](auto) {
        if (table_view.selection().is_empty()) {
            GUI::MessageBox::show("No process selected!", "Profiler", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, window);
            return;
        }
        auto index = table_view.selection().first();
        auto pid_as_variant = table_view.model()->data(index, GUI::Model::Role::Custom);
        pid = pid_as_variant.as_i32();
        GUI::Application::the().quit(0);
    };
    auto& cancel_button = button_container.add<GUI::Button>("Cancel");
    cancel_button.on_click = [](auto) {
        GUI::Application::the().quit();
    };

    table_view.model()->update();
    window->show();
    GUI::Application::the().exec();
    return pid;
}

bool prompt_to_stop_profiling()
{
    auto window = GUI::Window::construct();
    window->set_title("Profiling");
    Gfx::IntRect window_rect { 0, 0, 320, 200 };
    window_rect.center_within(GUI::Desktop::the().rect());
    window->set_rect(window_rect);
    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);
    widget.set_layout<GUI::VerticalBoxLayout>();

    auto& timer_label = widget.add<GUI::Label>("...");
    Core::ElapsedTimer clock;
    clock.start();
    auto update_timer = Core::Timer::construct(100, [&] {
        timer_label.set_text(String::format("%.1f seconds", (float)clock.elapsed() / 1000.0f));
    });

    auto& stop_button = widget.add<GUI::Button>("Stop");
    stop_button.on_click = [&](auto) {
        GUI::Application::the().quit();
    };

    window->show();
    return GUI::Application::the().exec() == 0;
}

bool generate_profile(pid_t pid)
{
    if (!pid) {
        pid = prompt_for_process_to_profile();
        if (!pid)
            return false;
    }

    if (profiling_enable(pid) < 0) {
        int saved_errno = errno;
        GUI::MessageBox::show(String::format("Unable to profile PID %d: %s", pid, strerror(saved_errno)), "Profiler", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK);
        return false;
    }

    if (!prompt_to_stop_profiling())
        return false;

    return true;
}
