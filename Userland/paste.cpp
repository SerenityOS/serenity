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

#include <AK/String.h>
#include <LibCore/CArgsParser.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GClipboard.h>
#include <stdio.h>
#include <stdlib.h>

int main(int argc, char* argv[])
{
    bool print_type = false;
    bool no_newline = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(print_type, "Display the copied type", "print-type", 0);
    args_parser.add_option(no_newline, "Do not append a newline", "no-newline", 'n');
    args_parser.parse(argc, argv);

    GApplication app(argc, argv);

    GClipboard& clipboard = GClipboard::the();
    auto data_and_type = clipboard.data_and_type();

    if (!print_type) {
        printf("%s", data_and_type.data.characters());
        // Append a newline to text contents, but
        // only if we're not asked not to do this.
        if (data_and_type.type == "text" && !no_newline)
            putchar('\n');
    } else {
        printf("%s\n", data_and_type.type.characters());
    }

    return 0;
}
