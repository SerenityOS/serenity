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

template<class T>
static void out_optional(char const* label, Optional<T> optional)
{
    out("{}: ", label);
    if (optional.has_value())
        outln("{}", *optional);
    else
        outln("(not set)");
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    static StringView icc_path;
    args_parser.add_positional_argument(icc_path, "Path to ICC profile", "FILE");
    args_parser.parse(arguments);

    auto icc_file = TRY(Core::MappedFile::map(icc_path));
    auto profile = TRY(Gfx::ICC::Profile::try_load_from_externally_owned_memory(icc_file->bytes()));

    out_optional("preferred CMM type", profile->preferred_cmm_type());
    outln("version: {}", profile->version());
    outln("device class: {}", Gfx::ICC::device_class_name(profile->device_class()));
    outln("data color space: {}", Gfx::ICC::data_color_space_name(profile->data_color_space()));
    outln("connection space: {}", Gfx::ICC::profile_connection_space_name(profile->connection_space()));
    outln("creation date and time: {}", Core::DateTime::from_timestamp(profile->creation_timestamp()).to_deprecated_string());
    outln("primary platform: {}", Gfx::ICC::primary_platform_name(profile->primary_platform()));

    auto flags = profile->flags();
    outln("flags: 0x{:08x}", flags.bits());
    outln("  embedded in file: {}", flags.is_embedded_in_file() ? "yes" : "no");
    outln("  can be used independently of embedded color data: {}", flags.can_be_used_independently_of_embedded_color_data() ? "yes" : "no");
    if (auto unknown_icc_bits = flags.icc_bits() & ~Gfx::ICC::Flags::KnownBitsMask)
        outln("  other unknown ICC bits: 0x{:04x}", unknown_icc_bits);
    if (auto color_management_module_bits = flags.color_management_module_bits())
        outln("  CMM bits: 0x{:04x}", color_management_module_bits);

    out_optional("device manufacturer", profile->device_manufacturer());
    out_optional("device model", profile->device_model());

    auto device_attributes = profile->device_attributes();
    outln("device attributes: 0x{:016x}", device_attributes.bits());
    outln("  media is {}, {}, {}, {}",
        device_attributes.media_reflectivity() == Gfx::ICC::DeviceAttributes::MediaReflectivity::Reflective ? "reflective" : "transparent",
        device_attributes.media_glossiness() == Gfx::ICC::DeviceAttributes::MediaGlossiness::Glossy ? "glossy" : "matte",
        device_attributes.media_polarity() == Gfx::ICC::DeviceAttributes::MediaPolarity::Positive ? "of positive polarity" : "of negative polarity",
        device_attributes.media_color() == Gfx::ICC::DeviceAttributes::MediaColor::Colored ? "colored" : "black and white");
    VERIFY((flags.icc_bits() & ~Gfx::ICC::DeviceAttributes::KnownBitsMask) == 0);
    if (auto vendor_bits = device_attributes.vendor_bits())
        outln("  vendor bits: 0x{:08x}", vendor_bits);

    outln("rendering intent: {}", Gfx::ICC::rendering_intent_name(profile->rendering_intent()));
    outln("pcs illuminant: {}", profile->pcs_illuminant());
    out_optional("creator", profile->creator());
    out_optional("id", profile->id());

    size_t profile_disk_size = icc_file->size();
    if (profile_disk_size != profile->on_disk_size()) {
        VERIFY(profile_disk_size > profile->on_disk_size());
        outln("{} trailing bytes after profile data", profile_disk_size - profile->on_disk_size());
    }

    return 0;
}
