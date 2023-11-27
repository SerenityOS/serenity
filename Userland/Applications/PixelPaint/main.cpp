/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/DynamicWidgetContainer.h>
#include <LibGUI/Icon.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio thread recvfd sendfd rpath unix wpath cpath"));

    auto app = TRY(GUI::Application::create(arguments));
    Config::pledge_domain("PixelPaint");
    app->set_config_domain("PixelPaint"_string);

    StringView image_file;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(image_file, "Image file to open", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/clipboard", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/image", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/etc/FileIconProvider.ini", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-pixel-paint"sv);

    PixelPaint::g_icon_bag = TRY(PixelPaint::IconBag::create());

    auto window = GUI::Window::construct();
    window->set_title("Pixel Paint");
    window->restore_size_and_position("PixelPaint"sv, "Window"sv, { { 800, 520 } });
    window->save_size_and_position_on_close("PixelPaint"sv, "Window"sv);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto main_widget = window->set_main_widget<PixelPaint::MainWidget>();

    TRY(main_widget->initialize_menubar(*window));

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (main_widget->request_close()) {
            GUI::DynamicWidgetContainer::close_all_detached_windows();
            return GUI::Window::CloseRequestDecision::Close;
        }
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    auto& statusbar = *main_widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    app->on_action_enter = [&statusbar](GUI::Action& action) {
        statusbar.set_override_text(action.status_tip());
    };

    app->on_action_leave = [&statusbar](GUI::Action&) {
        statusbar.set_override_text({});
    };

    window->show();

    if (!image_file.is_empty()) {
        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(window, image_file);
        if (!response.is_error())
            main_widget->open_image(response.release_value());
        else
            TRY(main_widget->create_default_image());
    } else {
        TRY(main_widget->create_default_image());
    }

    return app->exec();
}
