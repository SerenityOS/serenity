/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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
    const char* start_x = nullptr;
    const char* start_y = nullptr;
    const char* width = nullptr;
    const char* height = nullptr;
    bool output_to_clipboard = false;
    int delay = 0;

    args_parser.add_positional_argument(output_path, "Output filename", "output", Core::ArgsParser::Required::No);
    args_parser.add_option(start_x, "Pixels from where the screenshot gets taken (x)", "starting-x", 'x', "pixels");
    args_parser.add_option(start_y, "Pixels from where the screenshot gets taken (y)", "starting-y", 'y', "pixels");
    args_parser.add_option(height, "Screenshot height in pixels", "height", 'h', "pixels");
    args_parser.add_option(width, "Screenshot width in pixels", "width", 'w', "pixels");
    args_parser.add_option(output_to_clipboard, "Output to clipboard", "clipboard", 'c');
    args_parser.add_option(delay, "Seconds to wait before taking a screenshot", "delay", 'd', "seconds");

    args_parser.parse(argc, argv);

    if (output_path.is_empty()) {
        output_path = Core::DateTime::now().to_string("screenshot-%Y-%m-%d-%H-%M-%S.png");
    }

    auto app = GUI::Application::construct(argc, argv);
    sleep(delay);
    auto response = GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::GetScreenBitmap>();

    auto* bitmap = response->bitmap().bitmap();
    if (!bitmap) {
        warnln("Failed to grab screenshot");
        return 1;
    }

    //FIXME: There must be a cleaner way to do this. Maybe have initials value set? Like width being the screen width.
    if (start_x != nullptr && start_y != nullptr && width != nullptr && height != nullptr) {
        int x = atoi(start_x);
        int y = atoi(start_y);
        int w = atoi(width);
        int h = atoi(height);
        bitmap = bitmap->cropped({ x, y, w, h }).leak_ref();
    }

    if (output_to_clipboard) {
        GUI::Clipboard::the().set_bitmap(*bitmap);
        return 0;
    }

    Gfx::PNGWriter writer;
    auto encoded_bitmap = writer.write(bitmap);
    if (encoded_bitmap.is_empty()) {
        warnln("Failed to encode PNG");
        return 1;
    }

    auto file_or_error = Core::File::open(output_path, Core::IODevice::ReadWrite);
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
