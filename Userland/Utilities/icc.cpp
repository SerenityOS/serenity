/*
 * Copyright (c) 2022-2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/ICC/BinaryWriter.h>
#include <LibGfx/ICC/Profile.h>
#include <LibGfx/ICC/Tags.h>
#include <LibGfx/ICC/WellKnownProfiles.h>
#include <LibGfx/ImageDecoder.h>
#include <LibVideo/Color/CodingIndependentCodePoints.h>

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

static void out_curve(Gfx::ICC::CurveTagData const& curve, int indent_amount)
{
    auto indent = MUST(String::repeated(' ', indent_amount));
    if (curve.values().is_empty()) {
        outln("{}identity curve", indent);
    } else if (curve.values().size() == 1) {
        outln("{}gamma: {}", indent, FixedPoint<8, u16>::create_raw(curve.values()[0]));
    } else {
        // FIXME: Maybe print the actual points if -v is passed?
        outln("{}curve with {} points", indent, curve.values().size());
    }
}

static void out_parametric_curve(Gfx::ICC::ParametricCurveTagData const& parametric_curve, int indent_amount)
{
    auto indent = MUST(String::repeated(' ', indent_amount));
    switch (parametric_curve.function_type()) {
    case Gfx::ICC::ParametricCurveTagData::FunctionType::Type0:
        outln("{}Y = X**{}", indent, parametric_curve.g());
        break;
    case Gfx::ICC::ParametricCurveTagData::FunctionType::Type1:
        outln("{}Y = ({}*X + {})**{}   if X >= -{}/{}", indent,
            parametric_curve.a(), parametric_curve.b(), parametric_curve.g(), parametric_curve.b(), parametric_curve.a());
        outln("{}Y = 0                                else", indent);
        break;
    case Gfx::ICC::ParametricCurveTagData::FunctionType::Type2:
        outln("{}Y = ({}*X + {})**{} + {}   if X >= -{}/{}", indent,
            parametric_curve.a(), parametric_curve.b(), parametric_curve.g(), parametric_curve.c(), parametric_curve.b(), parametric_curve.a());
        outln("{}Y =  {}                                    else", indent, parametric_curve.c());
        break;
    case Gfx::ICC::ParametricCurveTagData::FunctionType::Type3:
        outln("{}Y = ({}*X + {})**{}   if X >= {}", indent,
            parametric_curve.a(), parametric_curve.b(), parametric_curve.g(), parametric_curve.d());
        outln("{}Y =  {}*X                         else", indent, parametric_curve.c());
        break;
    case Gfx::ICC::ParametricCurveTagData::FunctionType::Type4:
        outln("{}Y = ({}*X + {})**{} + {}   if X >= {}", indent,
            parametric_curve.a(), parametric_curve.b(), parametric_curve.g(), parametric_curve.e(), parametric_curve.d());
        outln("{}Y =  {}*X + {}                             else", indent, parametric_curve.c(), parametric_curve.f());
        break;
    }
}

static void out_curves(Vector<Gfx::ICC::LutCurveType> const& curves)
{
    for (auto const& curve : curves) {
        VERIFY(curve->type() == Gfx::ICC::CurveTagData::Type || curve->type() == Gfx::ICC::ParametricCurveTagData::Type);
        outln("        type {}, relative offset {}, size {}", curve->type(), curve->offset(), curve->size());
        if (curve->type() == Gfx::ICC::CurveTagData::Type)
            out_curve(static_cast<Gfx::ICC::CurveTagData&>(*curve), /*indent=*/12);
        if (curve->type() == Gfx::ICC::ParametricCurveTagData::Type)
            out_parametric_curve(static_cast<Gfx::ICC::ParametricCurveTagData&>(*curve), /*indent=*/12);
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    StringView path;
    args_parser.add_positional_argument(path, "Path to ICC profile or to image containing ICC profile", "FILE", Core::ArgsParser::Required::No);

    StringView name;
    args_parser.add_option(name, "Name of a built-in profile, such as 'sRGB'", "name", 'n', "NAME");

    StringView dump_out_path;
    args_parser.add_option(dump_out_path, "Dump unmodified ICC profile bytes to this path", "dump-to", 0, "FILE");

    StringView reencode_out_path;
    args_parser.add_option(reencode_out_path, "Reencode ICC profile to this path", "reencode-to", 0, "FILE");

    bool force_print = false;
    args_parser.add_option(force_print, "Print profile even when writing ICC files", "print", 0);

    args_parser.parse(arguments);

    if (path.is_empty() && name.is_empty()) {
        warnln("need either a path or a profile name");
        return 1;
    }
    if (!path.is_empty() && !name.is_empty()) {
        warnln("can't have both a path and a profile name");
        return 1;
    }
    if (path.is_empty() && !dump_out_path.is_empty()) {
        warnln("--dump-to only valid with path, not with profile name; use --reencode-to instead");
        return 1;
    }

    ReadonlyBytes icc_bytes;
    NonnullRefPtr<Gfx::ICC::Profile> profile = TRY([&]() -> ErrorOr<NonnullRefPtr<Gfx::ICC::Profile>> {
        if (!name.is_empty()) {
            if (name == "sRGB")
                return Gfx::ICC::sRGB();
            return Error::from_string_literal("unknown profile name");
        }
        auto file = TRY(Core::MappedFile::map(path));

        auto decoder = Gfx::ImageDecoder::try_create_for_raw_bytes(file->bytes());
        if (decoder) {
            if (auto embedded_icc_bytes = TRY(decoder->icc_data()); embedded_icc_bytes.has_value()) {
                icc_bytes = *embedded_icc_bytes;
            } else {
                outln("image contains no embedded ICC profile");
                exit(1);
            }
        } else {
            icc_bytes = file->bytes();
        }

        if (!dump_out_path.is_empty()) {
            auto output_stream = TRY(Core::File::open(dump_out_path, Core::File::OpenMode::Write));
            TRY(output_stream->write_until_depleted(icc_bytes));
        }
        return Gfx::ICC::Profile::try_load_from_externally_owned_memory(icc_bytes);
    }());

    if (!reencode_out_path.is_empty()) {
        auto reencoded_bytes = TRY(Gfx::ICC::encode(profile));
        auto output_stream = TRY(Core::File::open(reencode_out_path, Core::File::OpenMode::Write));
        TRY(output_stream->write_until_depleted(reencoded_bytes));
    }

    bool do_print = (dump_out_path.is_empty() && reencode_out_path.is_empty()) || force_print;
    if (!do_print)
        return 0;

    outln("                  size: {} bytes", profile->on_disk_size());
    out_optional("    preferred CMM type", profile->preferred_cmm_type());
    outln("               version: {}", profile->version());
    outln("          device class: {}", Gfx::ICC::device_class_name(profile->device_class()));
    outln("      data color space: {}", Gfx::ICC::data_color_space_name(profile->data_color_space()));
    outln("      connection space: {}", Gfx::ICC::profile_connection_space_name(profile->connection_space()));
    outln("creation date and time: {}", Core::DateTime::from_timestamp(profile->creation_timestamp()));
    out_optional("      primary platform", profile->primary_platform().map([](auto platform) { return primary_platform_name(platform); }));

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

    size_t profile_disk_size = icc_bytes.size();
    if (profile_disk_size != profile->on_disk_size()) {
        VERIFY(profile_disk_size > profile->on_disk_size());
        outln("{} trailing bytes after profile data", profile_disk_size - profile->on_disk_size());
    }

    outln("");

    outln("tags:");
    HashMap<Gfx::ICC::TagData*, Gfx::ICC::TagSignature> tag_data_to_first_signature;
    TRY(profile->try_for_each_tag([&tag_data_to_first_signature](auto tag_signature, auto tag_data) -> ErrorOr<void> {
        if (auto name = tag_signature_spec_name(tag_signature); name.has_value())
            out("{} ({}): ", *name, tag_signature);
        else
            out("Unknown tag ({}): ", tag_signature);
        outln("type {}, offset {}, size {}", tag_data->type(), tag_data->offset(), tag_data->size());

        // Print tag data only the first time it's seen.
        // (Different sigatures can refer to the same data.)
        auto it = tag_data_to_first_signature.find(tag_data);
        if (it != tag_data_to_first_signature.end()) {
            outln("    (see {} above)", it->value);
            return {};
        }
        tag_data_to_first_signature.set(tag_data, tag_signature);

        if (tag_data->type() == Gfx::ICC::ChromaticityTagData::Type) {
            auto& chromaticity = static_cast<Gfx::ICC::ChromaticityTagData&>(*tag_data);
            outln("    phosphor or colorant type: {}", Gfx::ICC::ChromaticityTagData::phosphor_or_colorant_type_name(chromaticity.phosphor_or_colorant_type()));
            for (auto const& xy : chromaticity.xy_coordinates())
                outln("    x, y: {}, {}", xy.x, xy.y);
        } else if (tag_data->type() == Gfx::ICC::CicpTagData::Type) {
            auto& cicp = static_cast<Gfx::ICC::CicpTagData&>(*tag_data);
            outln("    color primaries: {} - {}", cicp.color_primaries(),
                Video::color_primaries_to_string((Video::ColorPrimaries)cicp.color_primaries()));
            outln("    transfer characteristics: {} - {}", cicp.transfer_characteristics(),
                Video::transfer_characteristics_to_string((Video::TransferCharacteristics)cicp.transfer_characteristics()));
            outln("    matrix coefficients: {} - {}", cicp.matrix_coefficients(),
                Video::matrix_coefficients_to_string((Video::MatrixCoefficients)cicp.matrix_coefficients()));
            outln("    video full range flag: {} - {}", cicp.video_full_range_flag(),
                Video::video_full_range_flag_to_string((Video::VideoFullRangeFlag)cicp.video_full_range_flag()));
        } else if (tag_data->type() == Gfx::ICC::CurveTagData::Type) {
            out_curve(static_cast<Gfx::ICC::CurveTagData&>(*tag_data), /*indent=*/4);
        } else if (tag_data->type() == Gfx::ICC::Lut16TagData::Type) {
            auto& lut16 = static_cast<Gfx::ICC::Lut16TagData&>(*tag_data);
            outln("    input table: {} channels x {} entries", lut16.number_of_input_channels(), lut16.number_of_input_table_entries());
            outln("    output table: {} channels x {} entries", lut16.number_of_output_channels(), lut16.number_of_output_table_entries());
            outln("    color lookup table: {} grid points, {} total entries", lut16.number_of_clut_grid_points(), lut16.clut_values().size());

            auto const& e = lut16.e_matrix();
            outln("    e = [ {}, {}, {},", e[0], e[1], e[2]);
            outln("          {}, {}, {},", e[3], e[4], e[5]);
            outln("          {}, {}, {} ]", e[6], e[7], e[8]);
        } else if (tag_data->type() == Gfx::ICC::Lut8TagData::Type) {
            auto& lut8 = static_cast<Gfx::ICC::Lut8TagData&>(*tag_data);
            outln("    input table: {} channels x {} entries", lut8.number_of_input_channels(), lut8.number_of_input_table_entries());
            outln("    output table: {} channels x {} entries", lut8.number_of_output_channels(), lut8.number_of_output_table_entries());
            outln("    color lookup table: {} grid points, {} total entries", lut8.number_of_clut_grid_points(), lut8.clut_values().size());

            auto const& e = lut8.e_matrix();
            outln("    e = [ {}, {}, {},", e[0], e[1], e[2]);
            outln("          {}, {}, {},", e[3], e[4], e[5]);
            outln("          {}, {}, {} ]", e[6], e[7], e[8]);
        } else if (tag_data->type() == Gfx::ICC::LutAToBTagData::Type) {
            auto& a_to_b = static_cast<Gfx::ICC::LutAToBTagData&>(*tag_data);
            outln("    {} input channels, {} output channels", a_to_b.number_of_input_channels(), a_to_b.number_of_output_channels());

            if (auto const& optional_a_curves = a_to_b.a_curves(); optional_a_curves.has_value()) {
                outln("    a curves: {} curves", optional_a_curves->size());
                out_curves(optional_a_curves.value());
            } else {
                outln("    a curves: (not set)");
            }

            if (auto const& optional_clut = a_to_b.clut(); optional_clut.has_value()) {
                auto const& clut = optional_clut.value();
                outln("    color lookup table: {} grid points, {}",
                    TRY(String::join(" x "sv, clut.number_of_grid_points_in_dimension)),
                    TRY(clut.values.visit(
                        [](Vector<u8> const& v) { return String::formatted("{} u8 entries", v.size()); },
                        [](Vector<u16> const& v) { return String::formatted("{} u16 entries", v.size()); })));
            } else {
                outln("    color lookup table: (not set)");
            }

            if (auto const& optional_m_curves = a_to_b.m_curves(); optional_m_curves.has_value()) {
                outln("    m curves: {} curves", optional_m_curves->size());
                out_curves(optional_m_curves.value());
            } else {
                outln("    m curves: (not set)");
            }

            if (auto const& optional_e = a_to_b.e_matrix(); optional_e.has_value()) {
                auto const& e = optional_e.value();
                outln("    e = [ {}, {}, {}, {},", e[0], e[1], e[2], e[9]);
                outln("          {}, {}, {}, {},", e[3], e[4], e[5], e[10]);
                outln("          {}, {}, {}, {} ]", e[6], e[7], e[8], e[11]);
            } else {
                outln("    e = (not set)");
            }

            outln("    b curves: {} curves", a_to_b.b_curves().size());
            out_curves(a_to_b.b_curves());
        } else if (tag_data->type() == Gfx::ICC::LutBToATagData::Type) {
            auto& b_to_a = static_cast<Gfx::ICC::LutBToATagData&>(*tag_data);
            outln("    {} input channels, {} output channels", b_to_a.number_of_input_channels(), b_to_a.number_of_output_channels());

            outln("    b curves: {} curves", b_to_a.b_curves().size());
            out_curves(b_to_a.b_curves());

            if (auto const& optional_e = b_to_a.e_matrix(); optional_e.has_value()) {
                auto const& e = optional_e.value();
                outln("    e = [ {}, {}, {}, {},", e[0], e[1], e[2], e[9]);
                outln("          {}, {}, {}, {},", e[3], e[4], e[5], e[10]);
                outln("          {}, {}, {}, {} ]", e[6], e[7], e[8], e[11]);
            } else {
                outln("    e = (not set)");
            }

            if (auto const& optional_m_curves = b_to_a.m_curves(); optional_m_curves.has_value()) {
                outln("    m curves: {} curves", optional_m_curves->size());
                out_curves(optional_m_curves.value());
            } else {
                outln("    m curves: (not set)");
            }

            if (auto const& optional_clut = b_to_a.clut(); optional_clut.has_value()) {
                auto const& clut = optional_clut.value();
                outln("    color lookup table: {} grid points, {}",
                    TRY(String::join(" x "sv, clut.number_of_grid_points_in_dimension)),
                    TRY(clut.values.visit(
                        [](Vector<u8> const& v) { return String::formatted("{} u8 entries", v.size()); },
                        [](Vector<u16> const& v) { return String::formatted("{} u16 entries", v.size()); })));
            } else {
                outln("    color lookup table: (not set)");
            }

            if (auto const& optional_a_curves = b_to_a.a_curves(); optional_a_curves.has_value()) {
                outln("    a curves: {} curves", optional_a_curves->size());
                out_curves(optional_a_curves.value());
            } else {
                outln("    a curves: (not set)");
            }
        } else if (tag_data->type() == Gfx::ICC::MeasurementTagData::Type) {
            auto& measurement = static_cast<Gfx::ICC::MeasurementTagData&>(*tag_data);
            outln("    standard observer: {}", Gfx::ICC::MeasurementTagData::standard_observer_name(measurement.standard_observer()));
            outln("    tristimulus value for measurement backing: {}", measurement.tristimulus_value_for_measurement_backing());
            outln("    measurement geometry: {}", Gfx::ICC::MeasurementTagData::measurement_geometry_name(measurement.measurement_geometry()));
            outln("    measurement flare: {} %", measurement.measurement_flare() * 100);
            outln("    standard illuminant: {}", Gfx::ICC::MeasurementTagData::standard_illuminant_name(measurement.standard_illuminant()));
        } else if (tag_data->type() == Gfx::ICC::MultiLocalizedUnicodeTagData::Type) {
            auto& multi_localized_unicode = static_cast<Gfx::ICC::MultiLocalizedUnicodeTagData&>(*tag_data);
            for (auto& record : multi_localized_unicode.records()) {
                outln("    {:c}{:c}/{:c}{:c}: \"{}\"",
                    record.iso_639_1_language_code >> 8, record.iso_639_1_language_code & 0xff,
                    record.iso_3166_1_country_code >> 8, record.iso_3166_1_country_code & 0xff,
                    record.text);
            }
        } else if (tag_data->type() == Gfx::ICC::NamedColor2TagData::Type) {
            auto& named_colors = static_cast<Gfx::ICC::NamedColor2TagData&>(*tag_data);
            outln("    vendor specific flag: 0x{:08x}", named_colors.vendor_specific_flag());
            outln("    common name prefix: \"{}\"", named_colors.prefix());
            outln("    common name suffix: \"{}\"", named_colors.suffix());
            outln("    {} colors:", named_colors.size());
            for (size_t i = 0; i < min(named_colors.size(), 5u); ++i) {
                const auto& pcs = named_colors.pcs_coordinates(i);

                // FIXME: Display decoded values? (See ICC v4 6.3.4.2 and 10.8.)
                out("        \"{}\", PCS coordinates: 0x{:04x} 0x{:04x} 0x{:04x}", TRY(named_colors.color_name(i)), pcs.xyz.x, pcs.xyz.y, pcs.xyz.z);
                if (auto number_of_device_coordinates = named_colors.number_of_device_coordinates(); number_of_device_coordinates > 0) {
                    out(", device coordinates:");
                    for (size_t j = 0; j < number_of_device_coordinates; ++j)
                        out(" 0x{:04x}", named_colors.device_coordinates(i)[j]);
                }
                outln();
            }
            if (named_colors.size() > 5u)
                outln("        ...");
        } else if (tag_data->type() == Gfx::ICC::ParametricCurveTagData::Type) {
            out_parametric_curve(static_cast<Gfx::ICC::ParametricCurveTagData&>(*tag_data), /*indent=*/4);
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
        } else if (tag_data->type() == Gfx::ICC::SignatureTagData::Type) {
            auto& signature = static_cast<Gfx::ICC::SignatureTagData&>(*tag_data);

            if (auto name = signature.name_for_tag(tag_signature); name.has_value()) {
                outln("    signature: {}", name.value());
            } else {
                outln("    signature: Unknown ('{:c}{:c}{:c}{:c}' / 0x{:08x})",
                    signature.signature() >> 24, (signature.signature() >> 16) & 0xff, (signature.signature() >> 8) & 0xff, signature.signature() & 0xff,
                    signature.signature());
            }
        } else if (tag_data->type() == Gfx::ICC::TextDescriptionTagData::Type) {
            auto& text_description = static_cast<Gfx::ICC::TextDescriptionTagData&>(*tag_data);
            outln("    ascii: \"{}\"", text_description.ascii_description());
            out_optional("    unicode", TRY(text_description.unicode_description().map([](auto description) { return String::formatted("\"{}\"", description); })));
            outln("    unicode language code: 0x{}", text_description.unicode_language_code());
            out_optional("    macintosh", TRY(text_description.macintosh_description().map([](auto description) { return String::formatted("\"{}\"", description); })));
        } else if (tag_data->type() == Gfx::ICC::TextTagData::Type) {
            outln("    text: \"{}\"", static_cast<Gfx::ICC::TextTagData&>(*tag_data).text());
        } else if (tag_data->type() == Gfx::ICC::ViewingConditionsTagData::Type) {
            auto& viewing_conditions = static_cast<Gfx::ICC::ViewingConditionsTagData&>(*tag_data);
            outln("    unnormalized CIEXYZ values for illuminant (in which Y is in cd/m²): {}", viewing_conditions.unnormalized_ciexyz_values_for_illuminant());
            outln("    unnormalized CIEXYZ values for surround (in which Y is in cd/m²): {}", viewing_conditions.unnormalized_ciexyz_values_for_surround());
            outln("    illuminant type: {}", Gfx::ICC::MeasurementTagData::standard_illuminant_name(viewing_conditions.illuminant_type()));
        } else if (tag_data->type() == Gfx::ICC::XYZTagData::Type) {
            for (auto& xyz : static_cast<Gfx::ICC::XYZTagData&>(*tag_data).xyzs())
                outln("    {}", xyz);
        }
        return {};
    }));

    return 0;
}
