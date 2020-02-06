/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GClipboard.h>
#include <stdio.h>
#include <stdlib.h>

struct Options {
    String data;
    StringView type { "text" };
};

Options parse_options(int argc, char* argv[])
{
    const char* type = nullptr;
    Vector<const char*> text;

    Core::ArgsParser args_parser;
    args_parser.add_option(type, "Pick a type", "type", 't', "type");
    args_parser.add_positional_argument(text, "Text to copy", "text", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    Options options;
    options.type = type;

    if (text.is_empty()) {
        // Copy our stdin.
        auto c_stdin = Core::File::construct();
        bool success = c_stdin->open(
            STDIN_FILENO,
            Core::IODevice::OpenMode::ReadOnly,
            Core::File::ShouldCloseFileDescription::No);
        ASSERT(success);
        auto buffer = c_stdin->read_all();
        dbg() << "Read size " << buffer.size();
        options.data = String((char*)buffer.data(), buffer.size());
    } else {
        // Copy the rest of our command-line args.
        StringBuilder builder;
        bool first = true;
        for (auto& word : text) {
            if (!first)
                builder.append(' ');
            first = false;
            builder.append(word);
        }
        options.data = builder.to_string();
    }

    return options;
}

int main(int argc, char* argv[])
{
    GUI::Application app(argc, argv);

    Options options = parse_options(argc, argv);

    auto& clipboard = GUI::Clipboard::the();
    clipboard.set_data(options.data, options.type);

    return 0;
}
