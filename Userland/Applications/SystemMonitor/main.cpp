/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Undefine <cqundefine@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GraphWidget.h"
#include "MemoryStatsWidget.h"
#include "NetworkStatisticsWidget.h"
#include "ProcessFileDescriptorMapWidget.h"
#include "ProcessMemoryMapWidget.h"
#include "ProcessModel.h"
#include "ProcessStateWidget.h"
#include "ProcessUnveiledPathsWidget.h"
#include "ThreadStackWidget.h"
#include <AK/NumberFormat.h>
#include <Applications/SystemMonitor/ProcessWindowGML.h>
#include <Applications/SystemMonitor/SystemMonitorGML.h>
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Object.h>
#include <LibCore/System.h>
#include <LibCore/Timer.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Icon.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/LazyWidget.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/StackWidget.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibMain/Main.h>
#include <LibPCIDB/Database.h>
#include <LibThreading/BackgroundAction.h>
#include <serenity.h>
#include <signal.h>
#include <spawn.h>
#include <stdio.h>
#include <unistd.h>

static ErrorOr<NonnullRefPtr<GUI::Window>> build_process_window(pid_t);
static void build_performance_tab(GUI::Widget&);

static RefPtr<GUI::Statusbar> statusbar;

namespace SystemMonitor {

class ProgressbarPaintingDelegate final : public GUI::TableCellPaintingDelegate {
public:
    virtual ~ProgressbarPaintingDelegate() override = default;

    virtual void paint(GUI::Painter& painter, Gfx::IntRect const& a_rect, Palette const& palette, GUI::ModelIndex const& index) override
    {
        auto rect = a_rect.shrunken(2, 2);
        auto percentage = index.data(GUI::ModelRole::Custom).to_i32();

        auto data = index.data();
        String text;
        if (data.is_string())
            text = data.as_string();
        Gfx::StylePainter::paint_progressbar(painter, rect, palette, 0, 100, percentage, text);
        painter.draw_rect(rect, Color::Black);
    }
};

class UnavailableProcessWidget final : public GUI::Frame {
    C_OBJECT(UnavailableProcessWidget)
public:
    virtual ~UnavailableProcessWidget() override = default;

    String const& text() const { return m_text; }
    void set_text(String text)
    {
        m_text = move(text);
        update();
    }

private:
    UnavailableProcessWidget()
    {
        REGISTER_STRING_PROPERTY("text", text, set_text);
    }

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        Frame::paint_event(event);
        if (text().is_empty())
            return;
        GUI::Painter painter(*this);
        painter.add_clip_rect(event.rect());
        painter.draw_text(frame_inner_rect(), text(), Gfx::TextAlignment::Center, palette().window_text(), Gfx::TextElision::Right);
    }

    String m_text;
};

class HardwareTabWidget final : public GUI::LazyWidget {
    C_OBJECT(HardwareTabWidget)

public:
    HardwareTabWidget()
    {
        auto db_creator = Threading::BackgroundAction<int>::construct([this](auto&) {
            auto db = PCIDB::Database::open();
            if (!db)
                warnln("Couldn't open PCI ID database!");
            m_db = db;
            m_loader_complete.store(true);
            return 0;
        },
            nullptr);

        this->on_first_show = [this](GUI::LazyWidget& self) {
            {
                Vector<GUI::JsonArrayModel::FieldSpec> processors_field;
                processors_field.empend("processor", "Processor", Gfx::TextAlignment::CenterRight);
                processors_field.empend("vendor_id", "Vendor ID", Gfx::TextAlignment::CenterLeft);
                processors_field.empend("brand", "Brand", Gfx::TextAlignment::CenterLeft);
                processors_field.empend("Features", Gfx::TextAlignment::CenterLeft, [](auto& object) {
                    StringBuilder builder;
                    auto features = object.get("features").as_array();
                    for (auto& feature : features.values()) {
                        builder.append(feature.to_string());
                        builder.append(' ');
                    }
                    return GUI::Variant(builder.to_string());
                });
                processors_field.empend("family", "Family", Gfx::TextAlignment::CenterRight);
                processors_field.empend("model", "Model", Gfx::TextAlignment::CenterRight);
                processors_field.empend("stepping", "Stepping", Gfx::TextAlignment::CenterRight);
                processors_field.empend("type", "Type", Gfx::TextAlignment::CenterRight);

                auto& processors_table_view = *self.find_descendant_of_type_named<GUI::TableView>("cpus_table");
                auto json_model = GUI::JsonArrayModel::create("/proc/cpuinfo", move(processors_field));
                processors_table_view.set_model(json_model);
                json_model->invalidate();
            }

            {
                while (!m_loader_complete.load())
                    ;
                Vector<GUI::JsonArrayModel::FieldSpec> pci_fields;
                pci_fields.empend(
                    "Address", Gfx::TextAlignment::CenterLeft,
                    [](const JsonObject& object) {
                        auto seg = object.get("seg").to_u32();
                        auto bus = object.get("bus").to_u32();
                        auto device = object.get("device").to_u32();
                        auto function = object.get("function").to_u32();
                        return String::formatted("{:04x}:{:02x}:{:02x}.{}", seg, bus, device, function);
                    });
                pci_fields.empend(
                    "Class", Gfx::TextAlignment::CenterLeft,
                    [this](const JsonObject& object) {
                        auto class_id = object.get("class").to_u32();
                        String class_name = m_db ? m_db->get_class(class_id) : nullptr;
                        return class_name.is_empty() ? String::formatted("Unknown class: {:04x}", class_id) : class_name;
                    });
                pci_fields.empend(
                    "Vendor", Gfx::TextAlignment::CenterLeft,
                    [this](const JsonObject& object) {
                        auto vendor_id = object.get("vendor_id").to_u32();
                        String vendor_name = m_db ? m_db->get_vendor(vendor_id) : nullptr;
                        return vendor_name.is_empty() ? String::formatted("Unknown vendor: {:02x}", vendor_id) : vendor_name;
                    });
                pci_fields.empend(
                    "Device", Gfx::TextAlignment::CenterLeft,
                    [this](const JsonObject& object) {
                        auto vendor_id = object.get("vendor_id").to_u32();
                        auto device_id = object.get("device_id").to_u32();
                        String device_name = m_db ? m_db->get_device(vendor_id, device_id) : nullptr;
                        return device_name.is_empty() ? String::formatted("Unknown device: {:02x}", device_id) : device_name;
                    });
                pci_fields.empend(
                    "Revision", Gfx::TextAlignment::CenterRight,
                    [](const JsonObject& object) {
                        auto revision_id = object.get("revision_id").to_u32();
                        return String::formatted("{:02x}", revision_id);
                    });

                auto& pci_table_view = *self.find_descendant_of_type_named<GUI::TableView>("pci_dev_table");
                pci_table_view.set_model(MUST(GUI::SortingProxyModel::create(GUI::JsonArrayModel::create("/proc/pci", move(pci_fields)))));
                pci_table_view.model()->invalidate();
            }
        };
    }

private:
    RefPtr<PCIDB::Database> m_db;
    Atomic<bool> m_loader_complete { false };
};

class StorageTabWidget final : public GUI::LazyWidget {
    C_OBJECT(StorageTabWidget)
public:
    StorageTabWidget()
    {
        this->on_first_show = [](GUI::LazyWidget& self) {
            auto& fs_table_view = *self.find_child_of_type_named<GUI::TableView>("storage_table");

            Vector<GUI::JsonArrayModel::FieldSpec> df_fields;
            df_fields.empend("mount_point", "Mount point", Gfx::TextAlignment::CenterLeft);
            df_fields.empend("class_name", "Class", Gfx::TextAlignment::CenterLeft);
            df_fields.empend("source", "Source", Gfx::TextAlignment::CenterLeft);
            df_fields.empend(
                "Size", Gfx::TextAlignment::CenterRight,
                [](const JsonObject& object) {
                    StringBuilder size_builder;
                    size_builder.append(" ");
                    size_builder.append(human_readable_size(object.get("total_block_count").to_u64() * object.get("block_size").to_u64()));
                    size_builder.append(" ");
                    return size_builder.to_string();
                },
                [](const JsonObject& object) {
                    return object.get("total_block_count").to_u64() * object.get("block_size").to_u64();
                },
                [](const JsonObject& object) {
                    auto total_blocks = object.get("total_block_count").to_u64();
                    if (total_blocks == 0)
                        return 0;
                    auto free_blocks = object.get("free_block_count").to_u64();
                    auto used_blocks = total_blocks - free_blocks;
                    int percentage = (static_cast<double>(used_blocks) / static_cast<double>(total_blocks) * 100.0);
                    return percentage;
                });
            df_fields.empend(
                "Used", Gfx::TextAlignment::CenterRight,
                [](const JsonObject& object) {
            auto total_blocks = object.get("total_block_count").to_u64();
            auto free_blocks = object.get("free_block_count").to_u64();
            auto used_blocks = total_blocks - free_blocks;
            return human_readable_size(used_blocks * object.get("block_size").to_u64()); },
                [](const JsonObject& object) {
                    auto total_blocks = object.get("total_block_count").to_u64();
                    auto free_blocks = object.get("free_block_count").to_u64();
                    auto used_blocks = total_blocks - free_blocks;
                    return used_blocks * object.get("block_size").to_u64();
                });
            df_fields.empend(
                "Available", Gfx::TextAlignment::CenterRight,
                [](const JsonObject& object) {
                    return human_readable_size(object.get("free_block_count").to_u64() * object.get("block_size").to_u64());
                },
                [](const JsonObject& object) {
                    return object.get("free_block_count").to_u64() * object.get("block_size").to_u64();
                });
            df_fields.empend("Access", Gfx::TextAlignment::CenterLeft, [](const JsonObject& object) {
                bool readonly = object.get("readonly").to_bool();
                int mount_flags = object.get("mount_flags").to_int();
                return readonly || (mount_flags & MS_RDONLY) ? "Read-only" : "Read/Write";
            });
            df_fields.empend("Mount flags", Gfx::TextAlignment::CenterLeft, [](const JsonObject& object) {
                int mount_flags = object.get("mount_flags").to_int();
                StringBuilder builder;
                bool first = true;
                auto check = [&](int flag, const char* name) {
                    if (!(mount_flags & flag))
                        return;
                    if (!first)
                        builder.append(',');
                    builder.append(name);
                    first = false;
                };
                check(MS_NODEV, "nodev");
                check(MS_NOEXEC, "noexec");
                check(MS_NOSUID, "nosuid");
                check(MS_BIND, "bind");
                check(MS_RDONLY, "ro");
                check(MS_WXALLOWED, "wxallowed");
                if (builder.string_view().is_empty())
                    return String("defaults");
                return builder.to_string();
            });
            df_fields.empend("free_block_count", "Free blocks", Gfx::TextAlignment::CenterRight);
            df_fields.empend("total_block_count", "Total blocks", Gfx::TextAlignment::CenterRight);
            df_fields.empend("free_inode_count", "Free inodes", Gfx::TextAlignment::CenterRight);
            df_fields.empend("total_inode_count", "Total inodes", Gfx::TextAlignment::CenterRight);
            df_fields.empend("block_size", "Block size", Gfx::TextAlignment::CenterRight);

            fs_table_view.set_model(MUST(GUI::SortingProxyModel::create(GUI::JsonArrayModel::create("/proc/df", move(df_fields)))));

            fs_table_view.set_column_painting_delegate(3, make<ProgressbarPaintingDelegate>());

            fs_table_view.model()->invalidate();
        };
    }
};

}

REGISTER_WIDGET(SystemMonitor, HardwareTabWidget)
REGISTER_WIDGET(SystemMonitor, StorageTabWidget)
REGISTER_WIDGET(SystemMonitor, UnavailableProcessWidget)

static bool can_access_pid(pid_t pid)
{
    int rc = kill(pid, 0);
    return rc == 0;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    {
        // Before we do anything else, boost our process priority to the maximum allowed.
        // It's very frustrating when the system is bogged down under load and you just want
        // System Monitor to work.
        sched_param param {
            .sched_priority = THREAD_PRIORITY_MAX,
        };
        sched_setparam(0, &param);
    }

    TRY(Core::System::pledge("stdio thread proc recvfd sendfd rpath exec unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    Config::pledge_domain("SystemMonitor");

    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/proc", "r"));
    TRY(Core::System::unveil("/dev", "r"));
    TRY(Core::System::unveil("/bin", "r"));
    TRY(Core::System::unveil("/usr/lib", "r"));

    // This directory only exists if ports are installed
    if (auto result = Core::System::unveil("/usr/local/bin", "r"); result.is_error() && result.error().code() != ENOENT)
        return result.release_error();

    if (auto result = Core::System::unveil("/usr/local/lib", "r"); result.is_error() && result.error().code() != ENOENT)
        return result.release_error();

    // This file is only accesible when running as root
    if (auto result = Core::System::unveil("/boot/Kernel.debug", "r"); result.is_error() && result.error().code() != EACCES)
        return result.release_error();

    TRY(Core::System::unveil("/bin/Profiler", "rx"));
    TRY(Core::System::unveil("/bin/Inspector", "rx"));
    TRY(Core::System::unveil(nullptr, nullptr));

    StringView args_tab = "processes"sv;
    Core::ArgsParser parser;
    parser.add_option(args_tab, "Tab, one of 'processes', 'graphs', 'fs', 'hardware', or 'network'", "open-tab", 't', "tab");
    parser.parse(arguments);
    StringView args_tab_view = args_tab;

    auto app_icon = GUI::Icon::default_icon("app-system-monitor");

    auto window = GUI::Window::construct();
    window->set_title("System Monitor");
    window->resize(560, 430);

    auto main_widget = TRY(window->try_set_main_widget<GUI::Widget>());
    main_widget->load_from_gml(system_monitor_gml);
    auto& tabwidget = *main_widget->find_descendant_of_type_named<GUI::TabWidget>("main_tabs");
    statusbar = main_widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    auto& process_table_container = *tabwidget.find_descendant_of_type_named<GUI::Widget>("processes");

    auto process_model = ProcessModel::create();
    process_model->on_state_update = [&](int process_count, int thread_count) {
        statusbar->set_text(0, String::formatted("Processes: {}", process_count));
        statusbar->set_text(1, String::formatted("Threads: {}", thread_count));
    };

    auto& performance_widget = *tabwidget.find_descendant_of_type_named<GUI::Widget>("performance");
    build_performance_tab(performance_widget);

    auto& process_table_view = *process_table_container.find_child_of_type_named<GUI::TreeView>("process_table");
    process_table_view.set_model(TRY(GUI::SortingProxyModel::create(process_model)));
    for (auto column = 0; column < ProcessModel::Column::__Count; ++column)
        process_table_view.set_column_visible(column, false);
    process_table_view.set_column_visible(ProcessModel::Column::PID, true);
    process_table_view.set_column_visible(ProcessModel::Column::TID, true);
    process_table_view.set_column_visible(ProcessModel::Column::Name, true);
    process_table_view.set_column_visible(ProcessModel::Column::CPU, true);
    process_table_view.set_column_visible(ProcessModel::Column::User, true);
    process_table_view.set_column_visible(ProcessModel::Column::Virtual, true);
    process_table_view.set_column_visible(ProcessModel::Column::DirtyPrivate, true);

    process_table_view.set_key_column_and_sort_order(ProcessModel::Column::CPU, GUI::SortOrder::Descending);
    process_model->update();

    i32 frequency = Config::read_i32("SystemMonitor", "Monitor", "Frequency", 3);
    if (frequency != 1 && frequency != 3 && frequency != 5) {
        frequency = 3;
        Config::write_i32("SystemMonitor", "Monitor", "Frequency", frequency);
    }

    auto& refresh_timer = window->add<Core::Timer>(
        frequency * 1000, [&] {
            // FIXME: remove the primitive re-toggling code once persistent model indices work.
            auto toggled_indices = process_table_view.selection().indices();
            toggled_indices.remove_all_matching([&](auto const& index) { return !process_table_view.is_toggled(index); });
            process_model->update();
            if (!process_table_view.selection().is_empty())
                process_table_view.selection().for_each_index([&](auto& selection) {
                    if (toggled_indices.contains_slow(selection))
                        process_table_view.expand_all_parents_of(selection);
                });

            if (auto* memory_stats_widget = SystemMonitor::MemoryStatsWidget::the())
                memory_stats_widget->refresh();
        });

    auto selected_id = [&](ProcessModel::Column column) -> pid_t {
        if (process_table_view.selection().is_empty())
            return -1;
        auto pid_index = process_table_view.model()->index(process_table_view.selection().first().row(), column, process_table_view.selection().first().parent());
        return pid_index.data().to_i32();
    };

    auto selected_name = [&](ProcessModel::Column column) -> String {
        if (process_table_view.selection().is_empty())
            return {};
        auto pid_index = process_table_view.model()->index(process_table_view.selection().first().row(), column, process_table_view.selection().first().parent());
        return pid_index.data().to_string();
    };

    auto kill_action = GUI::Action::create(
        "&Kill Process", { Mod_Ctrl, Key_K }, { Key_Delete }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/kill.png").release_value_but_fixme_should_propagate_errors(), [&](const GUI::Action&) {
            pid_t pid = selected_id(ProcessModel::Column::PID);
            if (pid == -1)
                return;
            auto rc = GUI::MessageBox::show(window, String::formatted("Do you really want to kill \"{}\" (PID {})?", selected_name(ProcessModel::Column::Name), pid), "System Monitor", GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
            if (rc == GUI::Dialog::ExecResult::Yes)
                kill(pid, SIGKILL);
        },
        &process_table_view);

    auto stop_action = GUI::Action::create(
        "&Stop Process", { Mod_Ctrl, Key_S }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/stop-hand.png").release_value_but_fixme_should_propagate_errors(), [&](const GUI::Action&) {
            pid_t pid = selected_id(ProcessModel::Column::PID);
            if (pid == -1)
                return;
            auto rc = GUI::MessageBox::show(window, String::formatted("Do you really want to stop \"{}\" (PID {})?", selected_name(ProcessModel::Column::Name), pid), "System Monitor", GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
            if (rc == GUI::Dialog::ExecResult::Yes)
                kill(pid, SIGSTOP);
        },
        &process_table_view);

    auto continue_action = GUI::Action::create(
        "&Continue Process", { Mod_Ctrl, Key_C }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/continue.png").release_value_but_fixme_should_propagate_errors(), [&](const GUI::Action&) {
            pid_t pid = selected_id(ProcessModel::Column::PID);
            if (pid != -1)
                kill(pid, SIGCONT);
        },
        &process_table_view);

    auto profile_action = GUI::Action::create(
        "&Profile Process", { Mod_Ctrl, Key_P },
        Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-profiler.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
            pid_t pid = selected_id(ProcessModel::Column::PID);
            if (pid != -1) {
                auto pid_string = String::number(pid);
                pid_t child;
                const char* argv[] = { "/bin/Profiler", "--pid", pid_string.characters(), nullptr };
                if ((errno = posix_spawn(&child, "/bin/Profiler", nullptr, nullptr, const_cast<char**>(argv), environ))) {
                    perror("posix_spawn");
                } else {
                    if (disown(child) < 0)
                        perror("disown");
                }
            }
        },
        &process_table_view);

    HashMap<pid_t, NonnullRefPtr<GUI::Window>> process_windows;

    auto process_properties_action = GUI::CommonActions::make_properties_action(
        [&](auto&) {
            auto pid = selected_id(ProcessModel::Column::PID);
            if (pid == -1)
                return;

            RefPtr<GUI::Window> process_window;
            auto it = process_windows.find(pid);
            if (it == process_windows.end()) {
                auto process_window_or_error = build_process_window(pid);
                if (process_window_or_error.is_error())
                    return;
                process_window = process_window_or_error.release_value();
                process_window->on_close_request = [pid, &process_windows] {
                    process_windows.remove(pid);
                    return GUI::Window::CloseRequestDecision::Close;
                };
                process_windows.set(pid, *process_window);
            } else {
                process_window = it->value;
            }
            process_window->show();
            process_window->move_to_front();
        },
        &process_table_view);

    auto& file_menu = window->add_menu("&File");
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto process_context_menu = GUI::Menu::construct();
    process_context_menu->add_action(kill_action);
    process_context_menu->add_action(stop_action);
    process_context_menu->add_action(continue_action);
    process_context_menu->add_separator();
    process_context_menu->add_action(profile_action);
    process_context_menu->add_separator();
    process_context_menu->add_action(process_properties_action);
    process_table_view.on_context_menu_request = [&]([[maybe_unused]] const GUI::ModelIndex& index, const GUI::ContextMenuEvent& event) {
        if (index.is_valid())
            process_context_menu->popup(event.screen_position(), process_properties_action);
    };

    auto& frequency_menu = window->add_menu("F&requency");
    GUI::ActionGroup frequency_action_group;
    frequency_action_group.set_exclusive(true);

    auto make_frequency_action = [&](int seconds) {
        auto action = GUI::Action::create_checkable(String::formatted("&{} Sec", seconds), [&refresh_timer, seconds](auto&) {
            Config::write_i32("SystemMonitor", "Monitor", "Frequency", seconds);
            refresh_timer.restart(seconds * 1000);
        });
        action->set_status_tip(String::formatted("Refresh every {} seconds", seconds));
        action->set_checked(frequency == seconds);
        frequency_action_group.add_action(*action);
        frequency_menu.add_action(*action);
    };

    make_frequency_action(1);
    make_frequency_action(3);
    make_frequency_action(5);

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("System Monitor", app_icon, window));

    process_table_view.on_activation = [&](auto&) {
        if (process_properties_action->is_enabled())
            process_properties_action->activate();
    };

    static pid_t last_selected_pid;

    process_table_view.on_selection_change = [&] {
        pid_t pid = selected_id(ProcessModel::Column::PID);
        if (pid == last_selected_pid || pid < 1)
            return;
        last_selected_pid = pid;
        bool has_access = can_access_pid(pid);
        kill_action->set_enabled(has_access);
        stop_action->set_enabled(has_access);
        continue_action->set_enabled(has_access);
        profile_action->set_enabled(has_access);
        process_properties_action->set_enabled(has_access);
    };

    app->on_action_enter = [](GUI::Action const& action) {
        statusbar->set_override_text(action.status_tip());
    };
    app->on_action_leave = [](GUI::Action const&) {
        statusbar->set_override_text({});
    };

    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    if (args_tab_view == "processes")
        tabwidget.set_active_widget(&process_table_container);
    else if (args_tab_view == "graphs")
        tabwidget.set_active_widget(&performance_widget);
    else if (args_tab_view == "fs")
        tabwidget.set_active_widget(tabwidget.find_descendant_of_type_named<SystemMonitor::StorageTabWidget>("storage"));
    else if (args_tab_view == "hardware")
        tabwidget.set_active_widget(tabwidget.find_descendant_of_type_named<SystemMonitor::HardwareTabWidget>("hardware"));
    else if (args_tab_view == "network")
        tabwidget.set_active_widget(tabwidget.find_descendant_of_type_named<GUI::Widget>("network"));

    return app->exec();
}

ErrorOr<NonnullRefPtr<GUI::Window>> build_process_window(pid_t pid)
{
    auto window = GUI::Window::construct();
    window->resize(480, 360);
    window->set_title(String::formatted("PID {} - System Monitor", pid));

    auto app_icon = GUI::Icon::default_icon("app-system-monitor");
    window->set_icon(app_icon.bitmap_for_size(16));

    auto main_widget = TRY(window->try_set_main_widget<GUI::Widget>());
    main_widget->load_from_gml(process_window_gml);

    GUI::ModelIndex process_index;
    for (int row = 0; row < ProcessModel::the().row_count({}); ++row) {
        auto index = ProcessModel::the().index(row, ProcessModel::Column::PID);
        if (index.data().to_i32() == pid) {
            process_index = index;
            break;
        }
    }

    VERIFY(process_index.is_valid());
    if (auto icon_data = process_index.sibling_at_column(ProcessModel::Column::Icon).data(); icon_data.is_icon()) {
        main_widget->find_descendant_of_type_named<GUI::Label>("icon_label")->set_icon(icon_data.as_icon().bitmap_for_size(32));
    }

    main_widget->find_descendant_of_type_named<GUI::Label>("process_name")->set_text(String::formatted("{} (PID {})", process_index.sibling_at_column(ProcessModel::Column::Name).data().to_string(), pid));

    main_widget->find_descendant_of_type_named<SystemMonitor::ProcessStateWidget>("process_state")->set_pid(pid);
    main_widget->find_descendant_of_type_named<SystemMonitor::ProcessFileDescriptorMapWidget>("open_files")->set_pid(pid);
    main_widget->find_descendant_of_type_named<SystemMonitor::ThreadStackWidget>("thread_stack")->set_ids(pid, pid);
    main_widget->find_descendant_of_type_named<SystemMonitor::ProcessMemoryMapWidget>("memory_map")->set_pid(pid);
    main_widget->find_descendant_of_type_named<SystemMonitor::ProcessUnveiledPathsWidget>("unveiled_paths")->set_pid(pid);

    auto& widget_stack = *main_widget->find_descendant_of_type_named<GUI::StackWidget>("widget_stack");
    auto& unavailable_process_widget = *widget_stack.find_descendant_of_type_named<SystemMonitor::UnavailableProcessWidget>("unavailable_process");
    unavailable_process_widget.set_text(String::formatted("Unable to access PID {}", pid));

    if (can_access_pid(pid))
        widget_stack.set_active_widget(widget_stack.find_descendant_of_type_named<GUI::TabWidget>("available_process"));
    else
        widget_stack.set_active_widget(&unavailable_process_widget);

    return window;
}

void build_performance_tab(GUI::Widget& graphs_container)
{
    auto& cpu_graph_group_box = *graphs_container.find_descendant_of_type_named<GUI::GroupBox>("cpu_graph");

    size_t cpu_graphs_per_row = min(4, ProcessModel::the().cpus().size());
    auto cpu_graph_rows = ceil_div(ProcessModel::the().cpus().size(), cpu_graphs_per_row);
    cpu_graph_group_box.set_fixed_height(120u * cpu_graph_rows);

    Vector<SystemMonitor::GraphWidget&> cpu_graphs;
    for (auto row = 0u; row < cpu_graph_rows; ++row) {
        auto& cpu_graph_row = cpu_graph_group_box.add<GUI::Widget>();
        cpu_graph_row.set_layout<GUI::HorizontalBoxLayout>();
        cpu_graph_row.layout()->set_margins(6);
        cpu_graph_row.set_fixed_height(108);
        for (auto i = 0u; i < cpu_graphs_per_row; ++i) {
            auto& cpu_graph = cpu_graph_row.add<SystemMonitor::GraphWidget>();
            cpu_graph.set_max(100);
            cpu_graph.set_value_format(0, {
                                              .graph_color_role = ColorRole::SyntaxPreprocessorStatement,
                                              .text_formatter = [](u64 value) {
                                                  return String::formatted("Total: {}%", value);
                                              },
                                          });
            cpu_graph.set_value_format(1, {
                                              .graph_color_role = ColorRole::SyntaxPreprocessorValue,
                                              .text_formatter = [](u64 value) {
                                                  return String::formatted("Kernel: {}%", value);
                                              },
                                          });
            cpu_graphs.append(cpu_graph);
        }
    }
    ProcessModel::the().on_cpu_info_change = [cpu_graphs](NonnullOwnPtrVector<ProcessModel::CpuInfo> const& cpus) mutable {
        float sum_cpu = 0;
        for (size_t i = 0; i < cpus.size(); ++i) {
            cpu_graphs[i].add_value({ static_cast<size_t>(cpus[i].total_cpu_percent), static_cast<size_t>(cpus[i].total_cpu_percent_kernel) });
            sum_cpu += cpus[i].total_cpu_percent;
        }
        float cpu_usage = sum_cpu / (float)cpus.size();
        statusbar->set_text(2, String::formatted("CPU usage: {}%", (int)roundf(cpu_usage)));
    };

    auto& memory_graph = *graphs_container.find_descendant_of_type_named<SystemMonitor::GraphWidget>("memory_graph");
    memory_graph.set_value_format(0, {
                                         .graph_color_role = ColorRole::SyntaxComment,
                                         .text_formatter = [](u64 bytes) {
                                             return String::formatted("Committed: {}", human_readable_size(bytes));
                                         },
                                     });
    memory_graph.set_value_format(1, {
                                         .graph_color_role = ColorRole::SyntaxPreprocessorStatement,
                                         .text_formatter = [](u64 bytes) {
                                             return String::formatted("Allocated: {}", human_readable_size(bytes));
                                         },
                                     });
    memory_graph.set_value_format(2, {
                                         .graph_color_role = ColorRole::SyntaxPreprocessorValue,
                                         .text_formatter = [](u64 bytes) {
                                             return String::formatted("Kernel heap: {}", human_readable_size(bytes));
                                         },
                                     });
}
