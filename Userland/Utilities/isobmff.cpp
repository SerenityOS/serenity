/*
 * Copyright (c) 2023, Gregory Bertilson <Zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/ImageFormats/ISOBMFF/Reader.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    StringView path;
    args_parser.add_positional_argument(path, "Path to ISO Base Media File Format file", "FILE");

    args_parser.parse(arguments);

    auto file = TRY(Core::MappedFile::map(path));
    auto reader = TRY(Gfx::ISOBMFF::Reader::create(TRY(try_make<FixedMemoryStream>(file->bytes()))));
    auto boxes = TRY(reader.read_entire_file());

    for (auto& box : boxes)
        box->dump();
    return 0;
}
