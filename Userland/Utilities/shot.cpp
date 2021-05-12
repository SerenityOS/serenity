/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/PNGWriter.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    Core::ArgsParser args_parser;

    String output_path;
    bool output_to_clipboard = false;
    int delay = 0;

    args_parser.add_positional_argument(output_path, "Output filename", "output", Core::ArgsParser::Required::No);
    args_parser.add_option(output_to_clipboard, "Output to clipboard", "clipboard", 'c');
    args_parser.add_option(delay, "Seconds to wait before taking a screenshot", "delay", 'd', "seconds");

    args_parser.parse(argc, argv);

    if (output_path.is_empty()) {
        output_path = Core::DateTime::now().to_string("screenshot-%Y-%m-%d-%H-%M-%S.png");
    }

    auto app = GUI::Application::construct(argc, argv);
    sleep(delay);
    auto shared_bitmap = GUI::WindowServerConnection::the().get_screen_bitmap({});

    auto* bitmap = shared_bitmap.bitmap();
    if (!bitmap) {
        warnln("Failed to grab screenshot");
        return 1;
    }

    if (output_to_clipboard) {
        GUI::Clipboard::the().set_bitmap(*bitmap);
        return 0;
    }

    auto encoded_bitmap = Gfx::PNGWriter::encode(*bitmap);
    if (encoded_bitmap.is_empty()) {
        warnln("Failed to encode PNG");
        return 1;
    }

    auto file_or_error = Core::File::open(output_path, Core::OpenMode::ReadWrite);
    if (file_or_error.is_error()) {
        warnln("Could not open '{}' for writing: {}", output_path, file_or_error.error());
        return 1;
    }

    auto& file = *file_or_error.value();
    if (!file.write(encoded_bitmap.data(), encoded_bitmap.size())) {
        warnln("Failed to write PNG");
        return 1;
    }

    outln("{}", output_path);
    return 0;
}
