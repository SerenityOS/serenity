/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
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

#include <LibCore/ArgsParser.h>
#include <LibGUI/Application.h>
#include <LibGUI/WindowServerConnection.h>

int main(int argc, char** argv)
{
    int width = -1;
    int height = -1;
    int scale = 1;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Change the screen resolution.");
    args_parser.add_positional_argument(width, "Width", "width");
    args_parser.add_positional_argument(height, "Height", "height");
    args_parser.add_positional_argument(scale, "Scale Factor", "scale", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    // A Core::EventLoop is all we need, but WindowServerConnection needs a full Application object.
    char* dummy_argv[] = { argv[0] };
    auto app = GUI::Application::construct(1, dummy_argv);
    auto result = GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetResolution>(Gfx::IntSize { width, height }, scale);
    if (!result->success()) {
        warnln("failed to set resolution");
        return 1;
    }
}
