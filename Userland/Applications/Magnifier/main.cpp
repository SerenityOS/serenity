/*
 * Copyright (c) 2021, Valtteri Koskivuori <vkoskiv@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MagnifierWidget.h"
#include <AK/LexicalPath.h>
#include <LibCore/System.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibGfx/BMPWriter.h>
#include <LibGfx/Filters/ColorBlindnessFilter.h>
#include <LibGfx/PNGWriter.h>
#include <LibGfx/QOIWriter.h>
#include <LibMain/Main.h>

static ErrorOr<ByteBuffer> dump_bitmap(RefPtr<Gfx::Bitmap> bitmap, AK::StringView extension)
{
    if (extension == "bmp") {
        Gfx::BMPWriter dumper;
        return dumper.dump(bitmap);
    } else if (extension == "png") {
        return Gfx::PNGWriter::encode(*bitmap);
    } else if (extension == "qoi") {
        return Gfx::QOIWriter::encode(*bitmap);
    } else {
        return Error::from_string_literal("invalid image format");
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio cpath rpath recvfd sendfd unix"));
    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::unveil("/sys/kernel/processes", "r"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-magnifier"sv);

    // 4px on each side for padding
    constexpr int window_dimensions = 240 + 4 + 4;
    auto window = GUI::Window::construct();
    window->set_title("Magnifier");
    window->resize(window_dimensions, window_dimensions);
    window->set_minimizable(false);
    window->set_icon(app_icon.bitmap_for_size(16));
    auto magnifier = TRY(window->try_set_main_widget<MagnifierWidget>());

    auto file_menu = TRY(window->try_add_menu("&File"));
    TRY(file_menu->try_add_action(GUI::CommonActions::make_save_as_action([&](auto&) {
        AK::DeprecatedString filename = "file for saving";
        auto do_save = [&]() -> ErrorOr<void> {
            auto response = FileSystemAccessClient::Client::the().try_save_file(window, "Capture", "png");
            if (response.is_error())
                return {};
            auto file = response.release_value();
            auto path = AK::LexicalPath(file->filename());
            filename = path.basename();
            auto encoded = TRY(dump_bitmap(magnifier->current_bitmap(), path.extension()));

            if (!file->write(encoded.data(), encoded.size())) {
                return Error::from_errno(file->error());
            }
            return {};
        };

        auto result = do_save();
        if (result.is_error()) {
            GUI::MessageBox::show(window, "Unable to save file.\n"sv, "Error"sv, GUI::MessageBox::Type::Error);
            warnln("Error saving bitmap to {}: {}", filename, result.error().string_literal());
        }
    })));
    TRY(file_menu->try_add_separator());
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    })));

    auto size_action_group = make<GUI::ActionGroup>();

    auto two_x_action = GUI::Action::create_checkable(
        "&2x", { Key_2 }, [&](auto&) {
            magnifier->set_scale_factor(2);
        });

    auto four_x_action = GUI::Action::create_checkable(
        "&4x", { Key_4 }, [&](auto&) {
            magnifier->set_scale_factor(4);
        });

    auto eight_x_action = GUI::Action::create_checkable(
        "&8x", { Key_8 }, [&](auto&) {
            magnifier->set_scale_factor(8);
        });

    auto pause_action = GUI::Action::create_checkable(
        "&Pause Capture", { Key_Space }, [&](auto& action) {
            magnifier->pause_capture(action.is_checked());
        });

    size_action_group->add_action(two_x_action);
    size_action_group->add_action(four_x_action);
    size_action_group->add_action(eight_x_action);
    size_action_group->set_exclusive(true);

    auto view_menu = TRY(window->try_add_menu("&View"));
    TRY(view_menu->try_add_action(two_x_action));
    TRY(view_menu->try_add_action(four_x_action));
    TRY(view_menu->try_add_action(eight_x_action));
    two_x_action->set_checked(true);

    TRY(view_menu->try_add_separator());
    TRY(view_menu->try_add_action(pause_action));

    auto timeline_menu = TRY(window->try_add_menu("&Timeline"));
    auto previous_frame_action = GUI::Action::create(
        "&Previous frame", { Key_Left }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-back.png"sv)), [&](auto&) {
            pause_action->set_checked(true);
            magnifier->pause_capture(true);
            magnifier->display_previous_frame();
        });
    auto next_frame_action = GUI::Action::create(
        "&Next frame", { Key_Right }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png"sv)), [&](auto&) {
            pause_action->set_checked(true);
            magnifier->pause_capture(true);
            magnifier->display_next_frame();
        });
    TRY(timeline_menu->try_add_action(previous_frame_action));
    TRY(timeline_menu->try_add_action(next_frame_action));

    TRY(window->try_add_menu(TRY(GUI::CommonMenus::make_accessibility_menu(magnifier))));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_about_action("Magnifier", app_icon, window));

    window->show();
    window->set_always_on_top(true);

    return app->exec();
}
