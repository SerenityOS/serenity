/*
 * Copyright (c) 2022, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/ICCProfile.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    static StringView icc_path;
    args_parser.add_positional_argument(icc_path, "Path to ICC profile", "FILE");
    args_parser.parse(arguments);

    auto icc_file = TRY(Core::MappedFile::map(icc_path));
    auto profile = TRY(Gfx::ICC::Profile::try_load_from_externally_owned_memory(icc_file->bytes()));

    outln("version: {}", profile->version());
    outln("device class: {}", Gfx::ICC::device_class_name(profile->device_class()));

    return 0;
}
