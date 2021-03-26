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
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/PNGWriter.h>

int main(int argc, char** argv)
{
    Core::ArgsParser args_parser;

    String output_path;
    args_parser.add_positional_argument(output_path, "Output filename", "output", Core::ArgsParser::Required::No);

    args_parser.parse(argc, argv);

    if (output_path.is_empty()) {
        output_path = Core::DateTime::now().to_string("screenshot-%Y-%m-%d-%H-%M-%S.png");
    }

    auto app = GUI::Application::construct(argc, argv);
    auto response = GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::GetScreenBitmap>();

    auto* bitmap = response->bitmap().bitmap();
    if (!bitmap) {
        warnln("Failed to grab screenshot");
        return 1;
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
