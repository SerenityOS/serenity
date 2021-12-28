/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Julius Heijmen <julius.heijmen@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FlameGraphView.h"
#include "IndividualSampleModel.h"
#include "Profile.h"
#include "ProfileModel.h"
#include "TimelineContainer.h"
#include "TimelineHeader.h"
#include "TimelineTrack.h"
#include "TimelineView.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/System.h>
#include <LibCore/Timer.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Model.h>
#include <LibGUI/ProcessChooser.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <serenity.h>
#include <string.h>

using namespace Profiler;

static bool generate_profile(pid_t& pid);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    int pid = 0;
    char const* perfcore_file_arg = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_option(pid, "PID to profile", "pid", 'p', "PID");
    args_parser.add_positional_argument(perfcore_file_arg, "Path of perfcore file", "perfcore-file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (pid && perfcore_file_arg) {
        warnln("-p/--pid option and perfcore-file argument must not be used together!");
        return 1;
    }

    auto app = TRY(GUI::Application::try_create(arguments));
    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-profiler"));

    String perfcore_file;
    if (!perfcore_file_arg) {
        if (!generate_profile(pid))
            return 0;
        perfcore_file = String::formatted("/proc/{}/perf_events", pid);
    } else {
        perfcore_file = perfcore_file_arg;
    }

    auto profile_or_error = Profile::load_from_perfcore_file(perfcore_file);
    if (profile_or_error.is_error()) {
        GUI::MessageBox::show(nullptr, String::formatted("{}", profile_or_error.error()), "Profiler", GUI::MessageBox::Type::Error);
        return 0;
    }

    auto& profile = profile_or_error.value();

    auto window = TRY(GUI::Window::try_create());

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_protocol("/usr/share/man/man1/Profiler.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    window->set_title("Profiler");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(800, 600);

    auto main_widget = TRY(window->try_set_main_widget<GUI::Widget>());
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout<GUI::VerticalBoxLayout>();

    auto timeline_header_container = TRY(GUI::Widget::try_create());
    timeline_header_container->set_layout<GUI::VerticalBoxLayout>();
    timeline_header_container->set_fill_with_background_color(true);
    timeline_header_container->set_shrink_to_fit(true);

    auto timeline_view = TRY(TimelineView::try_create(*profile));
    for (auto const& process : profile->processes()) {
        bool matching_event_found = false;
        for (auto const& event : profile->events()) {
            if (event.pid == process.pid && process.valid_at(event.serial)) {
                matching_event_found = true;
                break;
            }
        }
        if (!matching_event_found)
            continue;
        auto timeline_header = TRY(timeline_header_container->try_add<TimelineHeader>(*profile, process));
        timeline_header->set_shrink_to_fit(true);
        timeline_header->on_selection_change = [&](bool selected) {
            auto end_valid = process.end_valid == EventSerialNumber {} ? EventSerialNumber::max_valid_serial() : process.end_valid;
            if (selected)
                profile->add_process_filter(process.pid, process.start_valid, end_valid);
            else
                profile->remove_process_filter(process.pid, process.start_valid, end_valid);

            timeline_header_container->for_each_child_widget([](auto& other_timeline_header) {
                static_cast<TimelineHeader&>(other_timeline_header).update_selection();
                return IterationDecision::Continue;
            });
        };

        (void)TRY(timeline_view->try_add<TimelineTrack>(*timeline_view, *profile, process));
    }

    auto main_splitter = TRY(main_widget->try_add<GUI::VerticalSplitter>());

    [[maybe_unused]] auto timeline_container = TRY(main_splitter->try_add<TimelineContainer>(*timeline_header_container, *timeline_view));

    auto tab_widget = TRY(main_splitter->try_add<GUI::TabWidget>());

    auto tree_tab = TRY(tab_widget->try_add_tab<GUI::Widget>("Call Tree"));
    tree_tab->set_layout<GUI::VerticalBoxLayout>();
    tree_tab->layout()->set_margins(4);
    auto bottom_splitter = TRY(tree_tab->try_add<GUI::VerticalSplitter>());

    auto tree_view = TRY(bottom_splitter->try_add<GUI::TreeView>());
    tree_view->set_should_fill_selected_rows(true);
    tree_view->set_column_headers_visible(true);
    tree_view->set_selection_behavior(GUI::TreeView::SelectionBehavior::SelectRows);
    tree_view->set_model(profile->model());

    auto disassembly_view = TRY(bottom_splitter->try_add<GUI::TableView>());
    disassembly_view->set_visible(false);

    auto update_disassembly_model = [&] {
        if (disassembly_view->is_visible() && !tree_view->selection().is_empty()) {
            profile->set_disassembly_index(tree_view->selection().first());
            disassembly_view->set_model(profile->disassembly_model());
        } else {
            disassembly_view->set_model(nullptr);
        }
    };

    auto source_view = TRY(bottom_splitter->try_add<GUI::TableView>());
    source_view->set_visible(false);

    auto update_source_model = [&] {
        if (source_view->is_visible() && !tree_view->selection().is_empty()) {
            profile->set_source_index(tree_view->selection().first());
            source_view->set_model(profile->source_model());
        } else {
            source_view->set_model(nullptr);
        }
    };

    tree_view->on_selection_change = [&] {
        update_disassembly_model();
        update_source_model();
    };

    auto disassembly_action = GUI::Action::create_checkable("Show &Disassembly", { Mod_Ctrl, Key_D }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/x86.png").release_value_but_fixme_should_propagate_errors(), [&](auto& action) {
        disassembly_view->set_visible(action.is_checked());
        update_disassembly_model();
    });

    auto source_action = GUI::Action::create_checkable("Show &Source", { Mod_Ctrl, Key_S }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/x86.png").release_value_but_fixme_should_propagate_errors(), [&](auto& action) {
        source_view->set_visible(action.is_checked());
        update_source_model();
    });

    auto samples_tab = TRY(tab_widget->try_add_tab<GUI::Widget>("Samples"));
    samples_tab->set_layout<GUI::VerticalBoxLayout>();
    samples_tab->layout()->set_margins(4);

    auto samples_splitter = TRY(samples_tab->try_add<GUI::HorizontalSplitter>());
    auto samples_table_view = TRY(samples_splitter->try_add<GUI::TableView>());
    samples_table_view->set_model(profile->samples_model());

    auto individual_sample_view = TRY(samples_splitter->try_add<GUI::TableView>());
    samples_table_view->on_selection_change = [&] {
        auto const& index = samples_table_view->selection().first();
        auto model = IndividualSampleModel::create(*profile, index.data(GUI::ModelRole::Custom).to_integer<size_t>());
        individual_sample_view->set_model(move(model));
    };

    auto signposts_tab = TRY(tab_widget->try_add_tab<GUI::Widget>("Signposts"));
    signposts_tab->set_layout<GUI::VerticalBoxLayout>();
    signposts_tab->layout()->set_margins(4);

    auto signposts_splitter = TRY(signposts_tab->try_add<GUI::HorizontalSplitter>());
    auto signposts_table_view = TRY(signposts_splitter->try_add<GUI::TableView>());
    signposts_table_view->set_model(profile->signposts_model());

    auto individual_signpost_view = TRY(signposts_splitter->try_add<GUI::TableView>());
    signposts_table_view->on_selection_change = [&] {
        auto const& index = signposts_table_view->selection().first();
        auto model = IndividualSampleModel::create(*profile, index.data(GUI::ModelRole::Custom).to_integer<size_t>());
        individual_signpost_view->set_model(move(model));
    };

    auto flamegraph_tab = TRY(tab_widget->try_add_tab<GUI::Widget>("Flame Graph"));
    flamegraph_tab->set_layout<GUI::VerticalBoxLayout>();
    flamegraph_tab->layout()->set_margins({ 4, 4, 4, 4 });

    auto flamegraph_view = TRY(flamegraph_tab->try_add<FlameGraphView>(profile->model(), ProfileModel::Column::StackFrame, ProfileModel::Column::SampleCount));

    u64 const start_of_trace = profile->first_timestamp();
    u64 const end_of_trace = start_of_trace + profile->length_in_ms();
    auto const clamp_timestamp = [start_of_trace, end_of_trace](u64 timestamp) -> u64 {
        return min(end_of_trace, max(timestamp, start_of_trace));
    };

    auto statusbar = TRY(main_widget->try_add<GUI::Statusbar>());
    auto statusbar_update = [&] {
        auto& view = *timeline_view;
        StringBuilder builder;

        auto flamegraph_hovered_index = flamegraph_view->hovered_index();
        if (flamegraph_hovered_index.is_valid()) {
            auto stack = profile->model().data(flamegraph_hovered_index.sibling_at_column(ProfileModel::Column::StackFrame)).to_string();
            auto sample_count = profile->model().data(flamegraph_hovered_index.sibling_at_column(ProfileModel::Column::SampleCount)).to_i32();
            auto self_count = profile->model().data(flamegraph_hovered_index.sibling_at_column(ProfileModel::Column::SelfCount)).to_i32();
            builder.appendff("{}, ", stack);
            builder.appendff("Samples: {}{}, ", sample_count, profile->show_percentages() ? "%" : " Samples");
            builder.appendff("Self: {}{}", self_count, profile->show_percentages() ? "%" : " Samples");
        } else {
            u64 normalized_start_time = clamp_timestamp(min(view.select_start_time(), view.select_end_time()));
            u64 normalized_end_time = clamp_timestamp(max(view.select_start_time(), view.select_end_time()));
            u64 normalized_hover_time = clamp_timestamp(view.hover_time());
            builder.appendff("Time: {} ms", normalized_hover_time - start_of_trace);
            if (normalized_start_time != normalized_end_time) {
                auto start = normalized_start_time - start_of_trace;
                auto end = normalized_end_time - start_of_trace;
                builder.appendff(", Selection: {} - {} ms", start, end);
                builder.appendff(", Duration: {} ms", end - start);
            }
        }
        statusbar->set_text(builder.to_string());
    };
    timeline_view->on_selection_change = [&] { statusbar_update(); };
    flamegraph_view->on_hover_change = [&] { statusbar_update(); };

    auto file_menu = TRY(window->try_add_menu("&File"));
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); })));

    auto view_menu = TRY(window->try_add_menu("&View"));

    auto invert_action = GUI::Action::create_checkable("&Invert Tree", { Mod_Ctrl, Key_I }, [&](auto& action) {
        profile->set_inverted(action.is_checked());
    });
    invert_action->set_checked(false);
    TRY(view_menu->try_add_action(invert_action));

    auto top_functions_action = GUI::Action::create_checkable("&Top Functions", { Mod_Ctrl, Key_T }, [&](auto& action) {
        profile->set_show_top_functions(action.is_checked());
    });
    top_functions_action->set_checked(false);
    TRY(view_menu->try_add_action(top_functions_action));

    auto percent_action = GUI::Action::create_checkable("Show &Percentages", { Mod_Ctrl, Key_P }, [&](auto& action) {
        profile->set_show_percentages(action.is_checked());
        tree_view->update();
        disassembly_view->update();
        source_view->update();
    });
    percent_action->set_checked(false);
    TRY(view_menu->try_add_action(percent_action));

    TRY(view_menu->try_add_action(disassembly_action));
    TRY(view_menu->try_add_action(source_action));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/Profiler.md"), "/bin/Help");
    })));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Profiler", app_icon, window)));

    window->show();
    return app->exec();
}

static bool prompt_to_stop_profiling(pid_t pid, String const& process_name)
{
    auto window = GUI::Window::construct();
    window->set_title(String::formatted("Profiling {}({})", process_name, pid));
    window->resize(240, 100);
    window->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-profiler.png").release_value_but_fixme_should_propagate_errors());
    window->center_on_screen();

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);
    auto& layout = widget.set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins({ 0, 0, 16 });

    auto& timer_label = widget.add<GUI::Label>("...");
    Core::ElapsedTimer clock;
    clock.start();
    auto update_timer = Core::Timer::construct(100, [&] {
        timer_label.set_text(String::formatted("{:.1} seconds", clock.elapsed() / 1000.0f));
    });

    auto& stop_button = widget.add<GUI::Button>("Stop");
    stop_button.set_fixed_size(140, 22);
    stop_button.on_click = [&](auto) {
        GUI::Application::the()->quit();
    };

    window->show();
    return GUI::Application::the()->exec() == 0;
}

bool generate_profile(pid_t& pid)
{
    if (!pid) {
        auto process_chooser = GUI::ProcessChooser::construct("Profiler", "Profile", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-profiler.png").release_value_but_fixme_should_propagate_errors());
        if (process_chooser->exec() == GUI::Dialog::ExecCancel)
            return false;
        pid = process_chooser->pid();
    }

    String process_name;

    auto all_processes = Core::ProcessStatisticsReader::get_all();
    if (all_processes.has_value()) {
        auto& processes = all_processes->processes;
        if (auto it = processes.find_if([&](auto& entry) { return entry.pid == pid; }); it != processes.end())
            process_name = it->name;
        else
            process_name = "(unknown)";
    } else {
        process_name = "(unknown)";
    }

    static constexpr u64 event_mask = PERF_EVENT_SAMPLE | PERF_EVENT_MMAP | PERF_EVENT_MUNMAP | PERF_EVENT_PROCESS_CREATE
        | PERF_EVENT_PROCESS_EXEC | PERF_EVENT_PROCESS_EXIT | PERF_EVENT_THREAD_CREATE | PERF_EVENT_THREAD_EXIT;

    if (profiling_enable(pid, event_mask) < 0) {
        int saved_errno = errno;
        GUI::MessageBox::show(nullptr, String::formatted("Unable to profile process {}({}): {}", process_name, pid, strerror(saved_errno)), "Profiler", GUI::MessageBox::Type::Error);
        return false;
    }

    if (!prompt_to_stop_profiling(pid, process_name))
        return false;

    if (profiling_disable(pid) < 0) {
        return false;
    }

    return true;
}
