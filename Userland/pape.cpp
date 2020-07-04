/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/Application.h>
#include <LibGUI/Desktop.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static int handle_show_all()
{
    Core::DirIterator di("/res/wallpapers", Core::DirIterator::SkipDots);
    if (di.has_error()) {
        fprintf(stderr, "DirIterator: %s\n", di.error_string());
        return 1;
    }

    while (di.has_next()) {
        String name = di.next_path();
        printf("%s\n", name.characters());
    }
    return 0;
}

static int handle_show_current()
{
    printf("%s\n", GUI::Desktop::the().wallpaper().characters());
    return 0;
}

static int handle_set_pape(const String& name)
{
    StringBuilder builder;
    builder.append("/res/wallpapers/");
    builder.append(name);
    String path = builder.to_string();
    if (!GUI::Desktop::the().set_wallpaper(path)) {
        fprintf(stderr, "pape: Failed to set wallpaper %s\n", path.characters());
        return 1;
    }
    return 0;
};

int main(int argc, char** argv)
{
    bool show_all = false;
    bool show_current = false;
    const char* name = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(show_all, "Show all wallpapers", "show-all", 'a');
    args_parser.add_option(show_current, "Show current wallpaper", "show-current", 'c');
    args_parser.add_positional_argument(name, "Wallpaper to set", "name", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto app = GUI::Application::construct(argc, argv);

    if (show_all)
        return handle_show_all();
    else if (show_current)
        return handle_show_current();

    return handle_set_pape(name);
}
