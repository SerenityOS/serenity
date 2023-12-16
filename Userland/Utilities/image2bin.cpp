/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/Random.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Desktop.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath unix"));
    ByteString path;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path to image", "path");
    args_parser.parse(arguments);

    auto bitmap = TRY(Gfx::Bitmap::load_from_file(path));

    TRY(Core::System::pledge("stdio"));
    Vector<u8> data;
    for (auto height = 0; height < bitmap->size().height(); height++) {
        auto* scanline = bitmap->scanline_u8(height);
        for (auto byte_index_in_row = 0u; byte_index_in_row < bitmap->pitch(); byte_index_in_row++) {
            TRY(data.try_append(scanline[byte_index_in_row]));
        }
    }
    VERIFY(data.size() == bitmap->size_in_bytes());
    TRY(Core::System::write(STDOUT_FILENO, data.span()));
    return 0;
}
