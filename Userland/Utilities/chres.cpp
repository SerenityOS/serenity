/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibGUI/Application.h>
#include <LibGUI/WindowServerConnection.h>

int main(int argc, char** argv)
{
    int screen = -1;
    int width = -1;
    int height = -1;
    int scale = 1;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Change the screen resolution.");
    args_parser.add_positional_argument(screen, "Screen", "screen");
    args_parser.add_positional_argument(width, "Width", "width");
    args_parser.add_positional_argument(height, "Height", "height");
    args_parser.add_positional_argument(scale, "Scale Factor", "scale", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    // A Core::EventLoop is all we need, but WindowServerConnection needs a full Application object.
    char* dummy_argv[] = { argv[0] };
    auto app = GUI::Application::construct(1, dummy_argv);
    auto screen_layout = GUI::WindowServerConnection::the().get_screen_layout();
    if (screen < 0 || (size_t)screen >= screen_layout.screens.size()) {
        warnln("invalid screen index: {}", screen);
        return 1;
    }
    auto& main_screen = screen_layout.screens[screen];
    main_screen.resolution = { width, height };
    auto set_result = GUI::WindowServerConnection::the().set_screen_layout(screen_layout, true);
    if (!set_result.success()) {
        warnln("failed to set resolution: {}", set_result.error_msg());
        return 1;
    }
}
