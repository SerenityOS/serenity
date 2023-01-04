/*
 * Copyright (c) 2022, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
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
    outln("data color space: {}", Gfx::ICC::data_color_space_name(profile->data_color_space()));
    outln("connection space: {}", Gfx::ICC::profile_connection_space_name(profile->connection_space()));
    outln("creation date and time: {}", Core::DateTime::from_timestamp(profile->creation_timestamp()).to_deprecated_string());

    auto flags = profile->flags();
    outln("flags: 0x{:08x}", flags.bits());
    outln("  embedded in file: {}", flags.is_embedded_in_file() ? "yes" : "no");
    outln("  can be used independently of embedded color data: {}", flags.can_be_used_independently_of_embedded_color_data() ? "yes" : "no");
    if (auto unknown_icc_bits = flags.icc_bits() & ~Gfx::ICC::Flags::KnownBitsMask)
        outln("  other unknown ICC bits: 0x{:04x}", unknown_icc_bits);
    if (auto color_management_module_bits = flags.color_management_module_bits())
        outln("  CMM bits: 0x{:04x}", color_management_module_bits);

    outln("rendering intent: {}", Gfx::ICC::rendering_intent_name(profile->rendering_intent()));
    outln("pcs illuminant: {}", profile->pcs_illuminant());

    out("id: ");
    if (auto id = profile->id(); id.has_value())
        outln("{}", *id);
    else
        outln("(not set)");

    return 0;
}
