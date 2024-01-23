/*
 * Copyright (c) 2021, Valtteri Koskivuori <vkoskiv@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MagnifierWidget.h"
#include <AK/LexicalPath.h>
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/ColorPicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibGfx/Filters/ColorBlindnessFilter.h>
#include <LibGfx/ImageFormats/BMPWriter.h>
#include <LibGfx/ImageFormats/PNGWriter.h>
#include <LibGfx/ImageFormats/QOIWriter.h>
#include <LibMain/Main.h>

static ErrorOr<ByteBuffer> dump_bitmap(RefPtr<Gfx::Bitmap> bitmap, AK::StringView extension)
{
    if (extension == "bmp") {
        return Gfx::BMPWriter::encode(*bitmap);
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
    auto app = TRY(GUI::Application::create(arguments));

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man1/Applications/Magnifier.md") }));
    TRY(Desktop::Launcher::seal_allowlist());
    Config::pledge_domain("Magnifier");

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
    auto magnifier = window->set_main_widget<MagnifierWidget>();

    auto file_menu = window->add_menu("&File"_string);
    file_menu->add_action(GUI::CommonActions::make_save_as_action([&](auto&) {
        ByteString filename = "file for saving";
        auto do_save = [&]() -> ErrorOr<void> {
            auto response = FileSystemAccessClient::Client::the().save_file(window, "Capture", "png");
            if (response.is_error())
                return {};
            auto file = response.value().release_stream();
            auto path = LexicalPath(response.value().filename());
            filename = path.basename();
            auto encoded = TRY(dump_bitmap(magnifier->current_bitmap(), path.extension()));

            TRY(file->write_until_depleted(encoded));
            return {};
        };

        auto result = do_save();
        if (result.is_error()) {
            GUI::MessageBox::show(window, "Unable to save file.\n"sv, "Error"sv, GUI::MessageBox::Type::Error);
            warnln("Error saving bitmap to {}: {}", filename, result.error().string_literal());
        }
    }));
    file_menu->add_separator();
    file_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

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

    auto lock_location_action = GUI::Action::create_checkable(
        "&Lock Location", { Key_L }, [&](auto& action) {
            magnifier->lock_location(action.is_checked());
        });

    auto show_grid_action = GUI::Action::create_checkable(
        "Show &Grid", { Key_G }, [&](auto& action) {
            magnifier->show_grid(action.is_checked());
        });

    auto choose_grid_color_action = GUI::Action::create(
        "Choose Grid &Color", [&](auto& action [[maybe_unused]]) {
            auto dialog = GUI::ColorPicker::construct(magnifier->grid_color(), window, "Magnifier: choose grid color");
            dialog->on_color_changed = [&magnifier](Gfx::Color color) {
                magnifier->set_grid_color(color);
            };
            dialog->set_color_has_alpha_channel(true);
            if (dialog->exec() == GUI::Dialog::ExecResult::OK) {
                Config::write_string("Magnifier"sv, "Grid"sv, "Color"sv, dialog->color().to_byte_string());
            }
        });
    {
        auto color_string = Config::read_string("Magnifier"sv, "Grid"sv, "Color"sv, "#ff00ff64"sv);
        auto maybe_color = Gfx::Color::from_string(color_string);
        magnifier->set_grid_color(maybe_color.value_or(Gfx::Color::Magenta));
    }

    size_action_group->add_action(two_x_action);
    size_action_group->add_action(four_x_action);
    size_action_group->add_action(eight_x_action);
    size_action_group->set_exclusive(true);

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(two_x_action);
    view_menu->add_action(four_x_action);
    view_menu->add_action(eight_x_action);
    two_x_action->set_checked(true);

    view_menu->add_separator();
    view_menu->add_action(pause_action);
    view_menu->add_action(lock_location_action);
    view_menu->add_action(show_grid_action);
    view_menu->add_action(choose_grid_color_action);

    auto timeline_menu = window->add_menu("&Timeline"_string);
    auto previous_frame_action = GUI::Action::create(
        "&Previous frame", { Key_Left }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png"sv)), [&](auto&) {
            pause_action->set_checked(true);
            magnifier->pause_capture(true);
            magnifier->display_previous_frame();
        });
    auto next_frame_action = GUI::Action::create(
        "&Next frame", { Key_Right }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"sv)), [&](auto&) {
            pause_action->set_checked(true);
            magnifier->pause_capture(true);
            magnifier->display_next_frame();
        });
    timeline_menu->add_action(previous_frame_action);
    timeline_menu->add_action(next_frame_action);

    window->add_menu(GUI::CommonMenus::make_accessibility_menu(magnifier));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man1/Applications/Magnifier.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Magnifier"_string, app_icon, window));

    window->show();
    window->set_always_on_top(true);

    return app->exec();
}
