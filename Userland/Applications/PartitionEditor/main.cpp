/*
 * Copyright (c) 2022, Samuel Bowman <sam@sambowman.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Applications/PartitionEditor/PartitionEditorWindowGML.h>
#include <Applications/PartitionEditor/PartitionModel.h>
#include <LibCore/Directory.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Application.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/TableView.h>
#include <unistd.h>

static Vector<ByteString> get_device_paths()
{
    auto device_paths = Vector<ByteString>();
    // FIXME: Propagate errors.
    (void)Core::Directory::for_each_entry("/dev"sv, Core::DirIterator::Flags::SkipParentAndBaseDir, [&](auto const& entry, auto const& directory) -> ErrorOr<IterationDecision> {
        auto full_path = LexicalPath::join(directory.path().string(), entry.name).string();
        if (FileSystem::is_block_device(full_path))
            device_paths.append(full_path);
        return IterationDecision::Continue;
    });
    return device_paths;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::unveil("/dev", "r"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/proc", "r"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/clipboard", "rw"));
    TRY(Core::System::unveil("/tmp/portal/window", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix"));

    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-partition-editor"sv));

    auto window = GUI::Window::construct();
    window->set_title("Partition Editor");
    window->resize(640, 400);
    window->set_icon(app_icon.bitmap_for_size(16));

    if (getuid() != 0) {
        auto error_message = "PartitionEditor must be run as root in order to open raw block devices and read partition tables."sv;
        GUI::MessageBox::show_error(window, error_message);
        return Error::from_string_view(error_message);
    }

    auto widget = window->set_main_widget<GUI::Widget>();
    TRY(widget->load_from_gml(partition_editor_window_gml));

    auto device_paths = get_device_paths();

    auto partition_model = PartitionEditor::PartitionModel::create();
    auto found_it = device_paths.find_if([&](auto const& path) {
        auto result = partition_model->set_device_path(path);
        return !result.is_error();
    });
    // FIXME: Give the user some way to create a partition table
    if (found_it == device_paths.end())
        GUI::MessageBox::show_error(window, ByteString::formatted("No partition table found on any valid block device"));

    auto& device_combobox = *widget->find_descendant_of_type_named<GUI::ComboBox>("device_combobox");
    device_combobox.set_model(GUI::ItemListModel<ByteString>::create(device_paths));
    device_combobox.set_only_allow_values_from_model(true);
    device_combobox.set_selected_index(0);
    device_combobox.on_change = [&](auto const& path, auto const&) {
        auto result = partition_model->set_device_path(path);
        if (result.is_error())
            GUI::MessageBox::show_error(window, ByteString::formatted("No partition table found for device {}", path));
    };

    auto& partition_table_view = *widget->find_descendant_of_type_named<GUI::TableView>("partition_table_view");
    partition_table_view.set_model(partition_model);
    partition_table_view.set_focus(true);

    auto file_menu = window->add_menu("&File"_string);
    file_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_about_action("Partition Editor"_string, app_icon, window));

    window->show();
    return app->exec();
}
