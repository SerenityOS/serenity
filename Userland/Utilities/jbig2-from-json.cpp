/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/MemoryStream.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibGfx/ImageFormats/BilevelImage.h>
#include <LibGfx/ImageFormats/JBIG2Loader.h>
#include <LibGfx/ImageFormats/JBIG2Shared.h>
#include <LibGfx/ImageFormats/JBIG2Writer.h>

static ErrorOr<Gfx::JBIG2::Organization> jbig2_organization_from_json(JsonValue const& value)
{
    if (!value.is_string())
        return Error::from_string_literal("expected string for \"organization\"");

    auto string = value.as_string();
    if (string == "sequential")
        return Gfx::JBIG2::Organization::Sequential;
    if (string == "random_access")
        return Gfx::JBIG2::Organization::RandomAccess;

    return Error::from_string_literal("organization must be \"sequential\" or \"random_access\"");
}

static ErrorOr<Gfx::JBIG2::FileHeaderData> jbig2_header_from_json(JsonObject const& header_object)
{
    Gfx::JBIG2::FileHeaderData header;

    TRY(header_object.try_for_each_member([&](StringView key, JsonValue const& object) -> ErrorOr<void> {
        if (key == "number_of_pages"sv) {
            if (auto number_of_pages = object.get_u32(); number_of_pages.has_value()) {
                header.number_of_pages = number_of_pages.value();
                return {};
            }
            if (object.is_null()) {
                header.number_of_pages = {};
                return {};
            }
            return Error::from_string_literal("expected uint or `null` for \"number_of_pages\"");
        }

        if (key == "organization"sv) {
            header.organization = TRY(jbig2_organization_from_json(object));
            return {};
        }

        dbgln("key {}", key);
        return Error::from_string_literal("unknown key");
    }));

    return header;
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_end_of_page_from_json(Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    if (object.has_value())
        return Error::from_string_literal("end_of_page segment should have no \"data\" object");
    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::EndOfPageSegmentData {} };
}

static ErrorOr<u8> jbig2_page_information_flags_from_json(JsonObject const& object)
{
    u8 flags = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "is_eventually_lossless"sv) {
            if (auto is_eventually_lossless = value.get_bool(); is_eventually_lossless.has_value()) {
                if (is_eventually_lossless.value())
                    flags |= 1u;
                return {};
            }
            return Error::from_string_literal("expected bool for \"is_eventually_lossless\"");
        }

        if (key == "might_contain_refinements"sv) {
            if (auto might_contain_refinements = value.get_bool(); might_contain_refinements.has_value()) {
                if (might_contain_refinements.value())
                    flags |= 1u << 1;
                return {};
            }
            return Error::from_string_literal("expected bool for \"might_contain_refinements\"");
        }

        if (key == "default_color"sv) {
            if (value.is_string()) {
                auto const& s = value.as_string();
                if (s == "white"sv)
                    flags |= 0;
                else if (s == "black"sv)
                    flags |= 1u << 2;
                else
                    return Error::from_string_literal("expected \"white\" or \"black\" for \"default_color\"");
                return {};
            }
            return Error::from_string_literal("expected \"white\" or \"black\" for \"default_color\"");
        }

        if (key == "default_combination_operator"sv) {
            if (value.is_string()) {
                // "replace" is only valid in a region segment information's external_combination_operator, not here.
                auto const& s = value.as_string();
                if (s == "or"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::Or) << 3;
                else if (s == "and"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::And) << 3;
                else if (s == "xor"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::Xor) << 3;
                else if (s == "xnor"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::XNor) << 3;
                else
                    return Error::from_string_literal("expected \"or\", \"and\", \"xor\", or \"xnor\" for \"default_combination_operator\"");
                return {};
            }
            return Error::from_string_literal("expected \"or\", \"and\", \"xor\", or \"xnor\" for \"default_combination_operator\"");
        }

        if (key == "requires_auxiliary_buffers"sv) {
            if (auto requires_auxiliary_buffers = value.get_bool(); requires_auxiliary_buffers.has_value()) {
                if (requires_auxiliary_buffers.value())
                    flags |= 1u << 5;
                return {};
            }
            return Error::from_string_literal("expected bool for \"requires_auxiliary_buffers\"");
        }

        if (key == "direct_region_segments_override_default_combination_operator"sv) {
            if (auto direct_region_segments_override_default_combination_operator = value.get_bool(); direct_region_segments_override_default_combination_operator.has_value()) {
                if (direct_region_segments_override_default_combination_operator.value())
                    flags |= 1u << 6;
                return {};
            }
            return Error::from_string_literal("expected bool for \"direct_region_segments_override_default_combination_operator\"");
        }

        if (key == "might_contain_coloured_segments"sv) {
            if (auto might_contain_coloured_segments = value.get_bool(); might_contain_coloured_segments.has_value()) {
                if (might_contain_coloured_segments.value())
                    flags |= 1u << 7;
                return {};
            }
            return Error::from_string_literal("expected bool for \"might_contain_coloured_segments\"");
        }

        dbgln("page_information flag key {}", key);
        return Error::from_string_literal("unknown page_information flag key");
    }));

    return flags;
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_page_information_from_json(Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    if (!object.has_value())
        return Error::from_string_literal("page_information segment should have \"data\" object");

    Gfx::JBIG2::PageInformationSegment data {};

    TRY(object->try_for_each_member([&](StringView key, JsonValue const& object) -> ErrorOr<void> {
        if (key == "page_width"sv) {
            if (auto page_width = object.get_u32(); page_width.has_value()) {
                data.bitmap_width = page_width.value();
                return {};
            }
            return Error::from_string_literal("expected uint for \"page_width\"");
        }

        if (key == "page_height"sv) {
            if (auto page_height = object.get_u32(); page_height.has_value()) {
                data.bitmap_height = page_height.value();
                return {};
            }
            return Error::from_string_literal("expected uint for \"page_height\"");
        }

        if (key == "page_x_resolution"sv) {
            if (auto page_x_resolution = object.get_u32(); page_x_resolution.has_value()) {
                data.page_x_resolution = page_x_resolution.value();
                return {};
            }
            return Error::from_string_literal("expected uint for \"page_x_resolution\"");
        }

        if (key == "page_y_resolution"sv) {
            if (auto page_y_resolution = object.get_u32(); page_y_resolution.has_value()) {
                data.page_y_resolution = page_y_resolution.value();
                return {};
            }
            return Error::from_string_literal("expected uint for \"page_y_resolution\"");
        }
        if (key == "flags"sv) {
            if (object.is_object()) {
                data.flags = TRY(jbig2_page_information_flags_from_json(object.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"flags\"");
        }

        dbgln("page_information key {}", key);
        return Error::from_string_literal("unknown page_information key");
    }));

    return Gfx::JBIG2::SegmentData { header, data };
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_segment_from_json(JsonObject const& segment_object)
{
    Gfx::JBIG2::SegmentHeaderData header;

    Optional<ByteString> type_string;
    Optional<JsonObject const&> segment_data_object;

    TRY(segment_object.try_for_each_member([&](StringView key, JsonValue const& object) -> ErrorOr<void> {
        if (key == "segment_number"sv) {
            if (auto segment_number = object.get_u32(); segment_number.has_value()) {
                header.segment_number = segment_number.value();
                return {};
            }
            return Error::from_string_literal("expected uint for \"segment_number\"");
        }

        if (key == "type"sv) {
            if (object.is_string()) {
                type_string = object.as_string();
                return {};
            }
            return Error::from_string_literal("expected string for \"type\"");
        }

        if (key == "page_association"sv) {
            if (auto page_association = object.get_u32(); page_association.has_value()) {
                header.page_association = page_association.value();
                return {};
            }
            return Error::from_string_literal("expected uint for \"page_association\"");
        }

        if (key == "data"sv) {
            if (object.is_object()) {
                segment_data_object = object.as_object();
                return {};
            }
            return Error::from_string_literal("expected object for \"data\"");
        }

        dbgln("segment key {}", key);
        return Error::from_string_literal("unknown segment key");
    }));

    if (!type_string.has_value())
        return Error::from_string_literal("segment missing \"type\"");

    if (type_string == "end_of_page")
        return jbig2_end_of_page_from_json(header, segment_data_object);
    if (type_string == "page_information")
        return jbig2_page_information_from_json(header, segment_data_object);

    dbgln("segment type {}", type_string);
    return Error::from_string_literal("segment has unknown type");
}

static ErrorOr<Vector<Gfx::JBIG2::SegmentData>> jbig2_segments_from_json(JsonArray const& segments_array)
{
    Vector<Gfx::JBIG2::SegmentData> segments;

    for (auto const& segment_value : segments_array.values()) {
        if (!segment_value.is_object())
            return Error::from_string_literal("segment should be object");
        segments.append(TRY(jbig2_segment_from_json(segment_value.as_object())));
    }

    return segments;
}

static ErrorOr<Gfx::JBIG2::FileData> jbig2_data_from_json(JsonValue const& json)
{
    Gfx::JBIG2::FileData jbig2;

    if (!json.is_object())
        return Error::from_string_literal("top-level should be object");
    auto object = json.as_object();

    if (auto global_header = object.get_object("global_header"sv); global_header.has_value())
        jbig2.header = TRY(jbig2_header_from_json(global_header.value()));
    else
        return Error::from_string_literal("top-level should have \"global_header\" object");

    if (auto segments = object.get_array("segments"sv); segments.has_value())
        jbig2.segments = TRY(jbig2_segments_from_json(segments.value()));
    else
        return Error::from_string_literal("top-level should have \"segments\" array");

    return jbig2;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    StringView in_path;
    StringView out_path;
    Core::ArgsParser args_parser;
    args_parser.set_general_help("Creates JBIG2 test files from JSON descriptions.");
    args_parser.add_positional_argument(in_path, "Path to input image file", "FILE");
    args_parser.add_option(out_path, "Path to output image file", "output", 'o', "FILE");
    args_parser.parse(arguments);
    if (out_path.is_empty())
        return Error::from_string_literal("-o is required");

    auto file = TRY(Core::File::open_file_or_standard_stream(in_path, Core::File::OpenMode::Read));
    TRY(Core::System::pledge("stdio"));

    auto file_contents = TRY(file->read_until_eof());
    auto json = TRY(JsonValue::from_string(file_contents));

    AllocatingMemoryStream stream;
    auto jbig2 = TRY(jbig2_data_from_json(json));
    TRY(Gfx::JBIG2Writer::encode_with_explicit_data(stream, jbig2));

    auto jbig2_data = TRY(stream.read_until_eof());

    // Only write images that decode correctly.
    TRY(TRY(Gfx::JBIG2ImageDecoderPlugin::create(jbig2_data))->frame(0));

    auto output_stream = TRY(Core::File::open(out_path, Core::File::OpenMode::Write));
    auto buffered_output = TRY(Core::OutputBufferedFile::create(move(output_stream)));

    TRY(buffered_output->write_until_depleted(jbig2_data));

    return 0;
}
