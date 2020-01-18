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
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/CArgsParser.h>
#include <LibCore/CDirIterator.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GDesktop.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static int handle_show_all()
{
    CDirIterator di("/res/wallpapers", CDirIterator::SkipDots);
    if (di.has_error()) {
        fprintf(stderr, "CDirIterator: %s\n", di.error_string());
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
    printf("%s\n", GDesktop::the().wallpaper().characters());
    return 0;
}

static int handle_set_pape(const String& name)
{
    StringBuilder builder;
    builder.append("/res/wallpapers/");
    builder.append(name);
    String path = builder.to_string();
    if (!GDesktop::the().set_wallpaper(path)) {
        fprintf(stderr, "pape: Failed to set wallpaper %s\n", path.characters());
        return 1;
    }
    return 0;
};

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    CArgsParser args_parser("pape");

    args_parser.add_arg("a", "show all wallpapers");
    args_parser.add_arg("c", "show current wallpaper");
    args_parser.add_single_value("name");

    CArgsParserResult args = args_parser.parse(argc, argv);

    if (args.is_present("a"))
        return handle_show_all();
    else if (args.is_present("c"))
        return handle_show_current();

    Vector<String> values = args.get_single_values();
    if (values.size() != 1) {
        args_parser.print_usage();
        return 0;
    }

    return handle_set_pape(values[0]);
}
