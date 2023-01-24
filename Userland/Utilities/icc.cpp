/*
 * Copyright (c) 2022-2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/ICCProfile.h>

template<class T>
static ErrorOr<String> hyperlink(URL const& target, T const& label)
{
    return String::formatted("\033]8;;{}\033\\{}\033]8;;\033\\", target, label);
}

template<class T>
static void out_optional(char const* label, Optional<T> const& optional)
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

    out_optional("    preferred CMM type", profile->preferred_cmm_type());
    outln("               version: {}", profile->version());
    outln("          device class: {}", Gfx::ICC::device_class_name(profile->device_class()));
    outln("      data color space: {}", Gfx::ICC::data_color_space_name(profile->data_color_space()));
    outln("      connection space: {}", Gfx::ICC::profile_connection_space_name(profile->connection_space()));
    outln("creation date and time: {}", Core::DateTime::from_timestamp(profile->creation_timestamp()));
    outln("      primary platform: {}", Gfx::ICC::primary_platform_name(profile->primary_platform()));

    auto flags = profile->flags();
    outln("                 flags: 0x{:08x}", flags.bits());
    outln("                        - {}embedded in file", flags.is_embedded_in_file() ? "" : "not ");
    outln("                        - can{} be used independently of embedded color data", flags.can_be_used_independently_of_embedded_color_data() ? "" : "not");
    if (auto unknown_icc_bits = flags.icc_bits() & ~Gfx::ICC::Flags::KnownBitsMask)
        outln("                        other unknown ICC bits: 0x{:04x}", unknown_icc_bits);
    if (auto color_management_module_bits = flags.color_management_module_bits())
        outln("                            CMM bits: 0x{:04x}", color_management_module_bits);

    out_optional("   device manufacturer", TRY(profile->device_manufacturer().map([](auto device_manufacturer) {
        return hyperlink(device_manufacturer_url(device_manufacturer), device_manufacturer);
    })));
    out_optional("          device model", TRY(profile->device_model().map([](auto device_model) {
        return hyperlink(device_model_url(device_model), device_model);
    })));

    auto device_attributes = profile->device_attributes();
    outln("     device attributes: 0x{:016x}", device_attributes.bits());
    outln("                        media is:");
    outln("                        - {}",
        device_attributes.media_reflectivity() == Gfx::ICC::DeviceAttributes::MediaReflectivity::Reflective ? "reflective" : "transparent");
    outln("                        - {}",
        device_attributes.media_glossiness() == Gfx::ICC::DeviceAttributes::MediaGlossiness::Glossy ? "glossy" : "matte");
    outln("                        - {}",
        device_attributes.media_polarity() == Gfx::ICC::DeviceAttributes::MediaPolarity::Positive ? "of positive polarity" : "of negative polarity");
    outln("                        - {}",
        device_attributes.media_color() == Gfx::ICC::DeviceAttributes::MediaColor::Colored ? "colored" : "black and white");
    VERIFY((flags.icc_bits() & ~Gfx::ICC::DeviceAttributes::KnownBitsMask) == 0);
    if (auto vendor_bits = device_attributes.vendor_bits())
        outln("                        vendor bits: 0x{:08x}", vendor_bits);

    outln("      rendering intent: {}", Gfx::ICC::rendering_intent_name(profile->rendering_intent()));
    outln("        pcs illuminant: {}", profile->pcs_illuminant());
    out_optional("               creator", profile->creator());
    out_optional("                    id", profile->id());

    size_t profile_disk_size = icc_file->size();
    if (profile_disk_size != profile->on_disk_size()) {
        VERIFY(profile_disk_size > profile->on_disk_size());
        outln("{} trailing bytes after profile data", profile_disk_size - profile->on_disk_size());
    }

    outln("");

    outln("tags:");
    HashMap<Gfx::ICC::TagData*, Gfx::ICC::TagSignature> tag_data_to_first_signature;
    profile->for_each_tag([&tag_data_to_first_signature](auto tag_signature, auto tag_data) {
        outln("{}: {}, offset {}, size {}", tag_signature, tag_data->type(), tag_data->offset(), tag_data->size());

        // Print tag data only the first time it's seen.
        // (Different sigatures can refer to the same data.)
        auto it = tag_data_to_first_signature.find(tag_data);
        if (it != tag_data_to_first_signature.end()) {
            outln("    (see {} above)", it->value);
            return;
        }
        tag_data_to_first_signature.set(tag_data, tag_signature);

        if (tag_data->type() == Gfx::ICC::CurveTagData::Type) {
            auto& curve = static_cast<Gfx::ICC::CurveTagData&>(*tag_data);
            if (curve.values().is_empty()) {
                outln("    identity curve");
            } else if (curve.values().size() == 1) {
                outln("    gamma: {}", FixedPoint<8, u16>::create_raw(curve.values()[0]));
            } else {
                // FIXME: Maybe print the actual points if -v is passed?
                outln("    curve with {} points", curve.values().size());
            }
        } else if (tag_data->type() == Gfx::ICC::MultiLocalizedUnicodeTagData::Type) {
            auto& multi_localized_unicode = static_cast<Gfx::ICC::MultiLocalizedUnicodeTagData&>(*tag_data);
            for (auto& record : multi_localized_unicode.records()) {
                outln("    {:c}{:c}/{:c}{:c}: \"{}\"",
                    record.iso_639_1_language_code >> 8, record.iso_639_1_language_code & 0xff,
                    record.iso_3166_1_country_code >> 8, record.iso_3166_1_country_code & 0xff,
                    record.text);
            }
        } else if (tag_data->type() == Gfx::ICC::ParametricCurveTagData::Type) {
            auto& parametric_curve = static_cast<Gfx::ICC::ParametricCurveTagData&>(*tag_data);
            switch (parametric_curve.function_type()) {
            case Gfx::ICC::ParametricCurveTagData::FunctionType::Type0:
                outln("  Y = X**{}", parametric_curve.g());
                break;
            case Gfx::ICC::ParametricCurveTagData::FunctionType::Type1:
                outln("  Y = ({}*X + {})**{}   if X >= -{}/{}",
                    parametric_curve.a(), parametric_curve.b(), parametric_curve.g(), parametric_curve.b(), parametric_curve.a());
                outln("  Y = 0                                else");
                break;
            case Gfx::ICC::ParametricCurveTagData::FunctionType::Type2:
                outln("  Y = ({}*X + {})**{} + {}   if X >= -{}/{}",
                    parametric_curve.a(), parametric_curve.b(), parametric_curve.g(), parametric_curve.c(), parametric_curve.b(), parametric_curve.a());
                outln("  Y =  {}                                    else", parametric_curve.c());
                break;
            case Gfx::ICC::ParametricCurveTagData::FunctionType::Type3:
                outln("  Y = ({}*X + {})**{}   if X >= {}",
                    parametric_curve.a(), parametric_curve.b(), parametric_curve.g(), parametric_curve.d());
                outln("  Y =  {}*X                        else", parametric_curve.c());
                break;
            case Gfx::ICC::ParametricCurveTagData::FunctionType::Type4:
                outln("  Y = ({}*X + {})**{} + {}   if X >= {}",
                    parametric_curve.a(), parametric_curve.b(), parametric_curve.g(), parametric_curve.e(), parametric_curve.d());
                outln("  Y =  {}*X + {}                             else", parametric_curve.c(), parametric_curve.f());
                break;
            }
        } else if (tag_data->type() == Gfx::ICC::S15Fixed16ArrayTagData::Type) {
            // This tag can contain arbitrarily many fixed-point numbers, but in practice it's
            // exclusively used for the 'chad' tag, where it always contains 9 values that
            // represent a 3x3 matrix.  So print the values in groups of 3.
            auto& fixed_array = static_cast<Gfx::ICC::S15Fixed16ArrayTagData&>(*tag_data);
            out("    [");
            int i = 0;
            for (auto value : fixed_array.values()) {
                if (i > 0) {
                    out(",");
                    if (i % 3 == 0) {
                        outln();
                        out("     ");
                    }
                }
                out(" {}", value);
                i++;
            }
            outln(" ]");
        } else if (tag_data->type() == Gfx::ICC::TextDescriptionTagData::Type) {
            auto& text_description = static_cast<Gfx::ICC::TextDescriptionTagData&>(*tag_data);
            outln("    ascii: \"{}\"", text_description.ascii_description());
            out_optional("    unicode", MUST(text_description.unicode_description().map([](auto description) { return String::formatted("\"{}\"", description); })));
            outln("    unicode language code: 0x{}", text_description.unicode_language_code());
            out_optional("    macintosh", MUST(text_description.macintosh_description().map([](auto description) { return String::formatted("\"{}\"", description); })));
        } else if (tag_data->type() == Gfx::ICC::TextTagData::Type) {
            outln("    text: \"{}\"", static_cast<Gfx::ICC::TextTagData&>(*tag_data).text());
        } else if (tag_data->type() == Gfx::ICC::XYZTagData::Type) {
            for (auto& xyz : static_cast<Gfx::ICC::XYZTagData&>(*tag_data).xyzs())
                outln("    {}", xyz);
        }
    });

    return 0;
}
