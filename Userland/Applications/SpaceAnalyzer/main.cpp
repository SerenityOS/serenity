/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include "Tree.h"
#include "TreeMapWidget.h"
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Breadcrumbbar.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGfx/Bitmap.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>

static auto const APP_NAME = "Space Analyzer"_string;

static ByteString get_absolute_path_to_selected_node(SpaceAnalyzer::TreeMapWidget const& tree_map_widget, bool include_last_node = true)
{
    StringBuilder path_builder;
    for (size_t k = 0; k < tree_map_widget.path_size() - (include_last_node ? 0 : 1); k++) {
        if (k != 0) {
            path_builder.append('/');
        }
        TreeNode const* node = tree_map_widget.path_node(k);
        path_builder.append(node->name());
    }
    return path_builder.to_byte_string();
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::create(arguments));

    // Configure application window.
    auto app_icon = GUI::Icon::default_icon("app-space-analyzer"sv);
    auto window = GUI::Window::construct();
    window->set_title(APP_NAME.bytes_as_string_view());
    window->resize(640, 480);
    window->set_icon(app_icon.bitmap_for_size(16));

    // Load widgets.
    auto main_widget = TRY(SpaceAnalyzer::MainWidget::try_create());
    window->set_main_widget(main_widget);

    auto& breadcrumbbar = *main_widget->find_descendant_of_type_named<GUI::Breadcrumbbar>("breadcrumbbar");
    auto& tree_map_widget = *main_widget->find_descendant_of_type_named<SpaceAnalyzer::TreeMapWidget>("tree_map");
    auto& statusbar = *main_widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    GUI::Application::the()->on_action_enter = [&statusbar](GUI::Action& action) {
        statusbar.set_override_text(action.status_tip());
    };
    GUI::Application::the()->on_action_leave = [&statusbar](GUI::Action&) {
        statusbar.set_override_text({});
    };

    tree_map_widget.set_focus(true);

    auto file_menu = window->add_menu("&File"_string);
    file_menu->add_action(GUI::Action::create("&Analyze", { KeyCode::Key_F5 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/reload.png"sv)), [&](auto&) {
        // FIXME: Just modify the tree in memory instead of traversing the entire file system
        if (auto result = tree_map_widget.analyze(statusbar); result.is_error()) {
            GUI::MessageBox::show_error(window, ByteString::formatted("{}", result.error()));
        }
    }));
    file_menu->add_separator();
    file_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_about_action(APP_NAME, app_icon, window));

    auto open_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"sv));
    // Configure the node's context menu.
    auto open_action = GUI::Action::create("Open in File Manager", { Mod_Ctrl, Key_O }, open_icon, [&](auto&) {
        auto path_string = get_absolute_path_to_selected_node(tree_map_widget);
        if (path_string.is_empty())
            return;

        if (FileSystem::is_directory(path_string)) {
            Desktop::Launcher::open(URL::create_with_file_scheme(path_string));
            return;
        }
        LexicalPath path { path_string };
        Desktop::Launcher::open(URL::create_with_file_scheme(path.dirname(), path.basename()));
    });

    auto copy_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-copy.png"sv));
    auto copy_path_action = GUI::Action::create("Copy Path to Clipboard", { Mod_Ctrl, Key_C }, copy_icon, [&](auto&) {
        GUI::Clipboard::the().set_plain_text(get_absolute_path_to_selected_node(tree_map_widget));
    });
    auto delete_action = GUI::CommonActions::make_delete_action([&](auto&) {
        ByteString selected_node_path = get_absolute_path_to_selected_node(tree_map_widget);
        bool try_again = true;
        while (try_again) {
            try_again = false;

            auto deletion_result = FileSystem::remove(selected_node_path, FileSystem::RecursionMode::Allowed);
            if (deletion_result.is_error()) {
                auto retry_message_result = GUI::MessageBox::show(window,
                    ByteString::formatted("Failed to delete \"{}\": {}. Retry?",
                        selected_node_path,
                        deletion_result.error()),
                    "Deletion failed"sv,
                    GUI::MessageBox::Type::Error,
                    GUI::MessageBox::InputType::YesNo);
                if (retry_message_result == GUI::MessageBox::ExecResult::Yes) {
                    try_again = true;
                }
            } else {
                GUI::MessageBox::show(window,
                    ByteString::formatted("Successfully deleted \"{}\".", selected_node_path),
                    "Deletion completed"sv,
                    GUI::MessageBox::Type::Information,
                    GUI::MessageBox::InputType::OK);
            }
        }

        if (auto result = tree_map_widget.analyze(statusbar); result.is_error()) {
            GUI::MessageBox::show_error(window, ByteString::formatted("{}", result.error()));
        }
    });

    auto context_menu = GUI::Menu::construct();
    context_menu->add_action(open_action);
    context_menu->add_action(copy_path_action);
    context_menu->add_action(delete_action);

    // Configure event handlers.
    breadcrumbbar.on_segment_click = [&](size_t index) {
        VERIFY(index < tree_map_widget.path_size());
        tree_map_widget.set_viewpoint(index);
    };
    tree_map_widget.on_path_change = [&]() {
        StringBuilder builder;

        breadcrumbbar.clear_segments();
        for (size_t k = 0; k < tree_map_widget.path_size(); k++) {
            if (k == 0) {
                if (tree_map_widget.viewpoint() == 0)
                    window->set_title("/ - SpaceAnalyzer");

                breadcrumbbar.append_segment("/", GUI::FileIconProvider::icon_for_path("/"sv).bitmap_for_size(16), "/", "/"_string);
                continue;
            }

            TreeNode const* node = tree_map_widget.path_node(k);

            builder.append('/');
            builder.append(node->name());

            // Sneakily set the window title here, while the StringBuilder holds the right amount of the path.
            if (k == tree_map_widget.viewpoint())
                window->set_title(ByteString::formatted("{} - SpaceAnalyzer", builder.string_view()));

            breadcrumbbar.append_segment(node->name(), GUI::FileIconProvider::icon_for_path(builder.string_view()).bitmap_for_size(16), builder.string_view(), MUST(builder.to_string()));
        }
        breadcrumbbar.set_selected_segment(tree_map_widget.viewpoint());
    };
    tree_map_widget.on_context_menu_request = [&](const GUI::ContextMenuEvent& event) {
        ByteString selected_node_path = get_absolute_path_to_selected_node(tree_map_widget);
        if (selected_node_path.is_empty())
            return;
        delete_action->set_enabled(FileSystem::can_delete_or_move(selected_node_path));
        if (FileSystem::is_directory(selected_node_path))
            open_action->set_text("Open in File Manager");
        else
            open_action->set_text("Reveal in File Manager");

        context_menu->popup(event.screen_position());
    };

    // At startup automatically do an analysis of root.
    TRY(tree_map_widget.analyze(statusbar));

    window->show();
    return app->exec();
}
