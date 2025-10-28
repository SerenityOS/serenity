/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Enumerate.h>
#include <AK/IntegralMath.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/MemoryStream.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibCore/MimeData.h>
#include <LibCore/System.h>
#include <LibGfx/ImageFormats/BilevelImage.h>
#include <LibGfx/ImageFormats/JBIG2Loader.h>
#include <LibGfx/ImageFormats/JBIG2Shared.h>
#include <LibGfx/ImageFormats/JBIG2Writer.h>

struct ToJSONOptions {
    StringView input_path;
};

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

    TRY(header_object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "number_of_pages"sv) {
            if (auto number_of_pages = value.get_u32(); number_of_pages.has_value()) {
                header.number_of_pages = number_of_pages.value();
                return {};
            }
            if (value.is_null()) {
                header.number_of_pages = {};
                return {};
            }
            return Error::from_string_literal("expected u32 or `null` for \"number_of_pages\"");
        }

        if (key == "organization"sv) {
            header.organization = TRY(jbig2_organization_from_json(value));
            return {};
        }

        dbgln("key {}", key);
        return Error::from_string_literal("unknown key");
    }));

    return header;
}

static Optional<Vector<i8>> jbig2_adaptive_template_pixels_from_json(JsonValue const& value)
{
    if (!value.is_array())
        return OptionalNone {};

    Vector<i8> adaptive_template_pixels;
    for (auto const& value : value.as_array().values()) {
        auto element = value.get_i32();
        if (!element.has_value() || (element.value() < -128 || element.value() > 127))
            return OptionalNone {};
        adaptive_template_pixels.append(static_cast<i8>(element.value()));
    }
    return adaptive_template_pixels;
}

static Vector<i8> default_adaptive_template_pixels(u8 gb_template, bool use_extended_template)
{
    // Default to Table 5 – The nominal values of the AT pixel locations
    if (gb_template == 0) {
        if (use_extended_template) {
            return {
                // clang-format off
                -2, 0,
                0, -2,
                -2, -1,
                -1, -2,
                1, -2,
                2, -1,
                -3, 0,
                -4, 0,
                2, -2,
                3, -1,
                -2, -2,
                -3, -1,
                // clang-format on
            };
        }
        return {
            // clang-format off
            3, -1,
            -3, -1,
            2, -2,
            -2, -2,
            // clang-format on
        };
    }
    if (gb_template == 1)
        return { 3, -1 };
    return { 2, -1 };
}

static Vector<i8> default_refinement_adaptive_template_pixels(u8 gr_template)
{
    // Default to Figure 12 – 13-pixel refinement template showing the AT pixels at their nominal locations
    if (gr_template == 0)
        return { -1, -1, -1, -1 };
    return {};
}

static ErrorOr<Gfx::MQArithmeticEncoder::Trailing7FFFHandling> jbig2_trailing_7fff_handling_from_json(JsonValue const& value)
{
    if (auto strip_trailing_7fffs = value.get_bool(); strip_trailing_7fffs.has_value()) {
        if (strip_trailing_7fffs.value())
            return Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Remove;
        return Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Keep;
    }
    return Error::from_string_literal("expected bool for \"strip_trailing_7fffs\"");
}

struct JSONRect {
    Optional<u32> x;
    Optional<u32> y;
    Optional<u32> width;
    Optional<u32> height;
};

static ErrorOr<JSONRect> jbig2_rect_from_json(JsonObject const& object)
{
    JSONRect rect;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "x"sv) {
            if (auto x = value.get_u32(); x.has_value()) {
                rect.x = x.value();
                return {};
            }
            return Error::from_string_literal("expected u32 for \"x\"");
        }

        if (key == "y"sv) {
            if (auto y = value.get_u32(); y.has_value()) {
                rect.y = y.value();
                return {};
            }
            return Error::from_string_literal("expected u32 for \"y\"");
        }

        if (key == "width"sv) {
            if (auto width = value.get_u32(); width.has_value()) {
                rect.width = width.value();
                return {};
            }
            return Error::from_string_literal("expected u32 for \"width\"");
        }

        if (key == "height"sv) {
            if (auto height = value.get_u32(); height.has_value()) {
                rect.height = height.value();
                return {};
            }
            return Error::from_string_literal("expected u32 for \"height\"");
        }

        dbgln("rect key {}", key);
        return Error::from_string_literal("unknown rect key");
    }));

    return rect;
}

static ErrorOr<NonnullRefPtr<Gfx::BilevelImage>> jbig2_image_from_json(ToJSONOptions const& options, JsonObject const& object)
{
    RefPtr<Gfx::BilevelImage> image;
    JSONRect crop_rect;
    bool invert = false;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "from_file") {
            if (value.is_string()) {
                ByteString base_directory = LexicalPath { options.input_path }.dirname();
                auto path = LexicalPath::absolute_path(base_directory, value.as_string());
                auto file_or_error = Core::MappedFile::map(path);
                if (file_or_error.is_error()) {
                    dbgln("could not open {}", path);
                    return file_or_error.release_error();
                }
                auto file = file_or_error.release_value();
                auto guessed_mime_type = Core::guess_mime_type_based_on_filename(path);
                auto decoder = TRY(Gfx::ImageDecoder::try_create_for_raw_bytes(file->bytes(), guessed_mime_type));
                if (!decoder)
                    return Error::from_string_literal("could not find decoder for input file");
                auto bitmap = TRY(decoder->frame(0)).image;
                image = TRY(Gfx::BilevelImage::create_from_bitmap(*bitmap, Gfx::DitheringAlgorithm::FloydSteinberg));
                return {};
            }
            return Error::from_string_literal("expected string for \"from_file\"");
        }

        if (key == "crop") {
            if (value.is_object()) {
                crop_rect = TRY(jbig2_rect_from_json(value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"crop\"");
        }

        if (key == "invert") {
            if (auto invert_value = value.get_bool(); invert_value.has_value()) {
                invert = invert_value.value();
                return {};
            }
            return Error::from_string_literal("expected bool for \"invert\"");
        }

        dbgln("image_data key {}", key);
        return Error::from_string_literal("unknown image_data key");
    }));

    if (!image)
        return Error::from_string_literal("no image data in image_data; add \"from_file\" key");

    if (crop_rect.x.has_value() || crop_rect.y.has_value() || crop_rect.width.has_value() || crop_rect.height.has_value()) {
        u32 crop_x = crop_rect.x.value_or(0);
        u32 crop_y = crop_rect.y.value_or(0);
        u32 crop_width = crop_rect.width.value_or(image->width() - crop_x);
        u32 crop_height = crop_rect.height.value_or(image->height() - crop_y);
        if (crop_x + crop_width > image->width() || crop_y + crop_height > image->height())
            return Error::from_string_literal("crop rectangle out of bounds");

        auto cropped_image = TRY(Gfx::BilevelImage::create(crop_width, crop_height));
        for (u32 y = 0; y < crop_height; ++y)
            for (u32 x = 0; x < crop_width; ++x)
                cropped_image->set_bit(x, y, image->get_bit(x + crop_x, y + crop_y));

        image = move(cropped_image);
    }

    if (invert) {
        for (u32 y = 0; y < image->height(); ++y)
            for (u32 x = 0; x < image->width(); ++x)
                image->set_bit(x, y, !image->get_bit(x, y));
    }

    return image.release_nonnull();
}

static ErrorOr<u8> jbig2_region_segment_information_flags_from_json(JsonObject const& object)
{
    u8 flags = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "external_combination_operator"sv) {
            if (value.is_string()) {
                auto const& s = value.as_string();
                if (s == "or"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::Or);
                else if (s == "and"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::And);
                else if (s == "xor"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::Xor);
                else if (s == "xnor"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::XNor);
                else if (s == "replace"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::Replace);
                else
                    return Error::from_string_literal("expected \"or\", \"and\", \"xor\", \"xnor\", or \"replace\" for \"external_combination_operator\"");
                return {};
            }
            return Error::from_string_literal("expected \"or\", \"and\", \"xor\", \"xnor\", or \"replace\" for \"external_combination_operator\"");
        }

        dbgln("region_segment_information flag key {}", key);
        return Error::from_string_literal("unknown region_segment_information flag key");
    }));

    return flags;
}

struct RegionSegmentInformatJSON {
    Gfx::JBIG2::RegionSegmentInformationField region_segment_information {};
    bool use_width_from_image { false };
    bool use_height_from_image { false };
};

static ErrorOr<RegionSegmentInformatJSON> jbig2_region_segment_information_from_json(JsonObject const& object)
{
    RegionSegmentInformatJSON result;
    result.use_width_from_image = true;
    result.use_height_from_image = true;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "width"sv) {
            if (auto width = value.get_u32(); width.has_value()) {
                result.region_segment_information.width = width.value();
                result.use_width_from_image = false;
                return {};
            }
            if (value.is_string()) {
                if (value.as_string() == "from_image_data"sv) {
                    result.use_width_from_image = true;
                    return {};
                }
                return Error::from_string_literal("expected \"from_image_data\" for \"width\" when it is a string");
            }
            return Error::from_string_literal("expected u32 or string for \"width\"");
        }

        if (key == "height"sv) {
            if (auto height = value.get_u32(); height.has_value()) {
                result.region_segment_information.height = height.value();
                result.use_height_from_image = false;
                return {};
            }
            if (value.is_string()) {
                if (value.as_string() == "from_image_data"sv) {
                    result.use_height_from_image = true;
                    return {};
                }
                return Error::from_string_literal("expected \"from_image_data\" for \"height\" when it is a string");
            }
            return Error::from_string_literal("expected u32 or string for \"height\"");
        }

        if (key == "x"sv) {
            if (auto x = value.get_u32(); x.has_value()) {
                result.region_segment_information.x_location = x.value();
                return {};
            }
            return Error::from_string_literal("expected u32 for \"x\"");
        }

        if (key == "y"sv) {
            if (auto y = value.get_u32(); y.has_value()) {
                result.region_segment_information.y_location = y.value();
                return {};
            }
            return Error::from_string_literal("expected u32 for \"y\"");
        }

        if (key == "flags"sv) {
            if (value.is_object()) {
                u8 flags = TRY(jbig2_region_segment_information_flags_from_json(value.as_object()));
                result.region_segment_information.flags = flags;
                return {};
            }
            return Error::from_string_literal("expected object for \"flags\"");
        }

        dbgln("region_segment_information key {}", key);
        return Error::from_string_literal("unknown region_segment_information key");
    }));
    return result;
}

static ErrorOr<u16> jbig2_symbol_dictionary_flags_from_json(JsonObject const& object)
{
    u16 flags = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "uses_huffman_encoding"sv) {
            if (auto uses_huffman_encoding = value.get_bool(); uses_huffman_encoding.has_value()) {
                if (uses_huffman_encoding.value())
                    flags |= 1u;
                return {};
            }
            return Error::from_string_literal("expected bool for \"uses_huffman_encoding\"");
        }

        if (key == "uses_refinement_or_aggregate_coding"sv) {
            if (auto uses_refinement_or_aggregate_coding = value.get_bool(); uses_refinement_or_aggregate_coding.has_value()) {
                if (uses_refinement_or_aggregate_coding.value())
                    flags |= 1u << 1;
                return {};
            }
            return Error::from_string_literal("expected bool for \"uses_refinement_or_aggregate_coding\"");
        }

        if (key == "huffman_table_selection_for_height_differences"sv) {
            if (auto huffman_table_selection_for_height_differences = value.get_uint(); huffman_table_selection_for_height_differences.has_value()) {
                if (huffman_table_selection_for_height_differences.value() <= 3) {
                    flags |= huffman_table_selection_for_height_differences.value() << 2;
                    return {};
                }
            }
            // FIXME: Also allow names "standard_table_4", "standard_table_5", "custom" for values 0, 1, 3.
            return Error::from_string_literal("expected 0, 1, or 3 for \"huffman_table_selection_for_height_differences\"");
        }

        if (key == "huffman_table_selection_for_width_differences"sv) {
            if (auto huffman_table_selection_for_width_differences = value.get_uint(); huffman_table_selection_for_width_differences.has_value()) {
                if (huffman_table_selection_for_width_differences.value() <= 3) {
                    flags |= huffman_table_selection_for_width_differences.value() << 4;
                    return {};
                }
            }
            // FIXME: Also allow names "standard_table_2", "standard_table_3", "custom" for values 0, 1, 3.
            return Error::from_string_literal("expected 0, 1, or 3 for \"huffman_table_selection_for_width_differences\"");
        }

        if (key == "huffman_table_selection_for_bitmap_sizes"sv) {
            if (auto huffman_table_selection_for_bitmap_sizes = value.get_uint(); huffman_table_selection_for_bitmap_sizes.has_value()) {
                if (huffman_table_selection_for_bitmap_sizes.value() <= 1) {
                    flags |= huffman_table_selection_for_bitmap_sizes.value() << 6;
                    return {};
                }
            }
            // FIXME: Also allow names "standard_table_1", "custom" for values 0, 1.
            return Error::from_string_literal("expected 0 or 1 for \"huffman_table_selection_for_bitmap_sizes\"");
        }

        if (key == "huffman_table_selection_for_number_of_symbol_instances"sv) {
            if (auto huffman_table_selection_for_number_of_symbol_instances = value.get_uint(); huffman_table_selection_for_number_of_symbol_instances.has_value()) {
                if (huffman_table_selection_for_number_of_symbol_instances.value() <= 1) {
                    flags |= huffman_table_selection_for_number_of_symbol_instances.value() << 7;
                    return {};
                }
            }
            // FIXME: Also allow names "standard_table_1", "custom" for values 0, 1.
            return Error::from_string_literal("expected 0 or 1 for \"huffman_table_selection_for_number_of_symbol_instances\"");
        }

        if (key == "is_bitmap_coding_context_used"sv) {
            if (auto is_bitmap_coding_context_used = value.get_bool(); is_bitmap_coding_context_used.has_value()) {
                if (is_bitmap_coding_context_used.value())
                    flags |= 1u << 8;
                return {};
            }
            return Error::from_string_literal("expected bool for \"is_bitmap_coding_context_used\"");
        }

        if (key == "is_bitmap_coding_context_retained"sv) {
            if (auto is_bitmap_coding_context_retained = value.get_bool(); is_bitmap_coding_context_retained.has_value()) {
                if (is_bitmap_coding_context_retained.value())
                    flags |= 1u << 9;
                return {};
            }
            return Error::from_string_literal("expected bool for \"is_bitmap_coding_context_used\"");
        }

        if (key == "template"sv) {
            if (auto template_ = value.get_uint(); template_.has_value()) {
                if (template_.value() <= 3) {
                    flags |= template_.value() << 10;
                    return {};
                }
            }
            return Error::from_string_literal("expected 0, 1, 2, or 3 for \"template\"");
        }

        if (key == "refinement_template"sv) {
            if (auto refinement_template = value.get_uint(); refinement_template.has_value()) {
                if (refinement_template.value() <= 1) {
                    flags |= refinement_template.value() << 12;
                    return {};
                }
            }
            return Error::from_string_literal("expected 0 or 1 for \"refinement_template\"");
        }

        dbgln("symbol_dictionary flag key {}", key);
        return Error::from_string_literal("unknown symbol_dictionary flag key");
    }));

    return flags;
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_symbol_dictionary_from_json(ToJSONOptions const&, Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    if (!object.has_value())
        return Error::from_string_literal("symbol_dictionary segment should have \"data\" object");

    u16 flags = 0;
    Vector<i8> adaptive_template_pixels;
    Vector<i8> refinement_adaptive_template_pixels;
    Gfx::MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Keep };
    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "flags"sv) {
            if (value.is_object()) {
                flags = TRY(jbig2_symbol_dictionary_flags_from_json(value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"flags\"");
        }

        if (key == "adaptive_template_pixels"sv) {
            if (auto adaptive_template_pixels_json = jbig2_adaptive_template_pixels_from_json(value); adaptive_template_pixels_json.has_value()) {
                adaptive_template_pixels = adaptive_template_pixels_json.value();
                return {};
            }
            return Error::from_string_literal("expected array of i8 for \"adaptive_template_pixels\"");
        }

        if (key == "refinement_adaptive_template_pixels"sv) {
            if (auto adaptive_template_pixels_json = jbig2_adaptive_template_pixels_from_json(value); adaptive_template_pixels_json.has_value()) {
                refinement_adaptive_template_pixels = adaptive_template_pixels_json.value();
                return {};
            }
            return Error::from_string_literal("expected array of i8 for \"refinement_adaptive_template_pixels\"");
        }

        if (key == "strip_trailing_7fffs"sv) {
            trailing_7fff_handling = TRY(jbig2_trailing_7fff_handling_from_json(value));
            return {};
        }

        dbgln("symbol_dictionary key {}", key);
        return Error::from_string_literal("unknown symbol_dictionary key");
    }));

    bool uses_huffman_encoding = (flags & 1) != 0;
    u8 symbol_template = (flags >> 10) & 3;
    if (adaptive_template_pixels.is_empty() && !uses_huffman_encoding)
        adaptive_template_pixels = default_adaptive_template_pixels(symbol_template, false);

    size_t number_of_adaptive_template_pixels = 0;
    if (!uses_huffman_encoding)
        number_of_adaptive_template_pixels = symbol_template == 0 ? 4 : 1;
    if (adaptive_template_pixels.size() != number_of_adaptive_template_pixels * 2) {
        dbgln("expected {} entries, got {}", number_of_adaptive_template_pixels * 2, adaptive_template_pixels.size());
        return Error::from_string_literal("symbol_dictionary \"data\" object has wrong number of \"adaptive_template_pixels\"");
    }
    Array<Gfx::JBIG2::AdaptiveTemplatePixel, 4> template_pixels {};
    for (size_t i = 0; i < number_of_adaptive_template_pixels; ++i) {
        template_pixels[i].x = adaptive_template_pixels[2 * i];
        template_pixels[i].y = adaptive_template_pixels[2 * i + 1];
    }

    u8 symbol_refinement_template = (flags >> 12) & 1;
    if (refinement_adaptive_template_pixels.is_empty())
        adaptive_template_pixels = default_refinement_adaptive_template_pixels(symbol_refinement_template);

    bool uses_refinement_or_aggregate_coding = (flags & 2) != 0;
    size_t number_of_refinement_adaptive_template_pixels = uses_refinement_or_aggregate_coding && symbol_refinement_template == 0 ? 2 : 0;
    if (refinement_adaptive_template_pixels.size() != number_of_refinement_adaptive_template_pixels * 2) {
        dbgln("expected {} entries, got {}", number_of_refinement_adaptive_template_pixels * 2, refinement_adaptive_template_pixels.size());
        return Error::from_string_literal("symbol_dictionary \"data\" object has wrong number of \"refinement_adaptive_template_pixels\"");
    }
    Array<Gfx::JBIG2::AdaptiveTemplatePixel, 2> refinement_template_pixels {};
    for (size_t i = 0; i < number_of_refinement_adaptive_template_pixels; ++i) {
        refinement_template_pixels[i].x = refinement_adaptive_template_pixels[2 * i];
        refinement_template_pixels[i].y = refinement_adaptive_template_pixels[2 * i + 1];
    }

    return Gfx::JBIG2::SegmentData {
        header,
        Gfx::JBIG2::SymbolDictionarySegmentData {
            flags,
            template_pixels,
            refinement_template_pixels,
            trailing_7fff_handling,
        }
    };
}

static ErrorOr<u16> jbig2_text_region_flags_from_json(JsonObject const& object)
{
    u16 flags = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "uses_huffman_encoding"sv) {
            if (auto uses_huffman_encoding = value.get_bool(); uses_huffman_encoding.has_value()) {
                if (uses_huffman_encoding.value())
                    flags |= 1u;
                return {};
            }
            return Error::from_string_literal("expected bool for \"uses_huffman_encoding\"");
        }

        if (key == "uses_refinement_coding"sv) {
            if (auto uses_refinement_coding = value.get_bool(); uses_refinement_coding.has_value()) {
                if (uses_refinement_coding.value())
                    flags |= 1u << 1;
                return {};
            }
            return Error::from_string_literal("expected bool for \"uses_refinement_coding\"");
        }

        if (key == "strip_size"sv) {
            if (auto strip_size = value.get_uint(); strip_size.has_value()) {
                switch (strip_size.value()) {
                case 1:
                case 2:
                case 4:
                case 8:
                    flags |= AK::log2(strip_size.value()) << 2;
                    return {};
                }
            }
            return Error::from_string_literal("expected 1, 2, 4, or 8 for \"strip_size\"");
        }

        if (key == "reference_corner"sv) {
            if (value.is_string()) {
                auto const& s = value.as_string();
                if (s == "bottom_left"sv)
                    flags |= to_underlying(Gfx::JBIG2::ReferenceCorner::BottomLeft) << 4;
                else if (s == "top_left"sv)
                    flags |= to_underlying(Gfx::JBIG2::ReferenceCorner::TopLeft) << 4;
                else if (s == "bottom_right"sv)
                    flags |= to_underlying(Gfx::JBIG2::ReferenceCorner::BottomRight) << 4;
                else if (s == "top_right"sv)
                    flags |= to_underlying(Gfx::JBIG2::ReferenceCorner::TopRight) << 4;
                else
                    return Error::from_string_literal("expected \"bottom_left\", \"top_left\", \"bottom_right\", or \"top_right\" for \"reference_corner\"");
                return {};
            }
            return Error::from_string_literal("expected \"bottom_left\", \"top_left\", \"bottom_right\", or \"top_right\" for \"reference_corner\"");
        }

        if (key == "is_transposed"sv) {
            if (auto is_transposed = value.get_bool(); is_transposed.has_value()) {
                if (is_transposed.value())
                    flags |= 1u << 6;
                return {};
            }
            return Error::from_string_literal("expected bool for \"is_transposed\"");
        }

        if (key == "combination_operator"sv) {
            if (value.is_string()) {
                // "replace" is only valid in a region segment information's external_combination_operator, not here.
                auto const& s = value.as_string();
                if (s == "or"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::Or) << 7;
                else if (s == "and"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::And) << 7;
                else if (s == "xor"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::Xor) << 7;
                else if (s == "xnor"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::XNor) << 7;
                else
                    return Error::from_string_literal("expected \"or\", \"and\", \"xor\", or \"xnor\" for \"combination_operator\"");
                return {};
            }
            return Error::from_string_literal("expected \"or\", \"and\", \"xor\", or \"xnor\" for \"combination_operator\"");
        }

        if (key == "default_pixel_value"sv) {
            if (value.is_string()) {
                auto const& s = value.as_string();
                if (s == "white"sv)
                    flags |= 0;
                else if (s == "black"sv)
                    flags |= 1u << 9;
                else
                    return Error::from_string_literal("expected \"white\" or \"black\" for \"default_pixel_value\"");
                return {};
            }
            return Error::from_string_literal("expected \"white\" or \"black\" for \"default_pixel_value\"");
        }

        if (key == "delta_s_offset"sv) {
            if (auto delta_s_offset = value.get_i32(); delta_s_offset.has_value() && delta_s_offset.value() >= -16 && delta_s_offset.value() <= 15) {
                flags |= (delta_s_offset.value() & 0x1F) << 10;
                return {};
            }
            return Error::from_string_literal("expected value in [-16, 15] for \"delta_s_offset\"");
        }

        if (key == "refinement_template"sv) {
            if (auto refinement_template = value.get_uint(); refinement_template.has_value()) {
                if (refinement_template.value() <= 1) {
                    flags |= refinement_template.value() << 15;
                    return {};
                }
            }
            return Error::from_string_literal("expected 0 or 1 for \"refinement_template\"");
        }

        dbgln("text_region flag key {}", key);
        return Error::from_string_literal("unknown text_region flag key");
    }));

    return flags;
}

static ErrorOr<u16> jbig2_text_region_huffman_flags_from_json(JsonObject const& object)
{
    u16 flags = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "huffman_table_selection_for_first_s"sv) {
            if (auto huffman_table_selection_for_first_s = value.get_uint(); huffman_table_selection_for_first_s.has_value()) {
                if (huffman_table_selection_for_first_s.value() <= 3) {
                    flags |= huffman_table_selection_for_first_s.value();
                    return {};
                }
            }
            // FIXME: Also allow names "standard_table_6", "standard_table_7", "custom" for values 0, 1, 3.
            return Error::from_string_literal("expected 0, 1, or 2 for \"huffman_table_selection_for_first_s\"");
        }

        if (key == "huffman_table_selection_for_subsequent_s"sv) {
            if (auto huffman_table_selection_for_subsequent_s = value.get_uint(); huffman_table_selection_for_subsequent_s.has_value()) {
                if (huffman_table_selection_for_subsequent_s.value() <= 3) {
                    flags |= huffman_table_selection_for_subsequent_s.value() << 2;
                    return {};
                }
            }
            // FIXME: Also allow names "standard_table_8", "standard_table_9", "standard_table_10", "custom" for values 0, 1, 2, 3.
            return Error::from_string_literal("expected 0, 1, 2, or 3 for \"huffman_table_selection_for_subsequent_s\"");
        }

        if (key == "huffman_table_selection_for_t"sv) {
            if (auto huffman_table_selection_for_t = value.get_uint(); huffman_table_selection_for_t.has_value()) {
                if (huffman_table_selection_for_t.value() <= 3) {
                    flags |= huffman_table_selection_for_t.value() << 4;
                    return {};
                }
            }
            // FIXME: Also allow names "standard_table_11", "standard_table_12", "standard_table_13", "custom" for values 0, 1, 2, 3.
            return Error::from_string_literal("expected 0, 1, 2, or 3 for \"huffman_table_selection_for_t\"");
        }

        if (key == "huffman_table_selection_for_refinement_delta_width"sv) {
            if (auto huffman_table_selection_for_refinement_delta_width = value.get_uint(); huffman_table_selection_for_refinement_delta_width.has_value()) {
                if (huffman_table_selection_for_refinement_delta_width.value() <= 3) {
                    flags |= huffman_table_selection_for_refinement_delta_width.value() << 6;
                    return {};
                }
            }
            // FIXME: Also allow names "standard_table_14", "standard_table_15", "custom" for values 0, 1, 3.
            return Error::from_string_literal("expected 0, 1, or 3 for \"huffman_table_selection_for_refinement_delta_width\"");
        }

        if (key == "huffman_table_selection_for_refinement_delta_height"sv) {
            if (auto huffman_table_selection_for_refinement_delta_height = value.get_uint(); huffman_table_selection_for_refinement_delta_height.has_value()) {
                if (huffman_table_selection_for_refinement_delta_height.value() <= 3) {
                    flags |= huffman_table_selection_for_refinement_delta_height.value() << 8;
                    return {};
                }
            }
            // FIXME: Also allow names "standard_table_14", "standard_table_15", "custom" for values 0, 1, 3.
            return Error::from_string_literal("expected 0, 1, or 3 for \"huffman_table_selection_for_refinement_delta_height\"");
        }

        if (key == "huffman_table_selection_for_refinement_delta_x_offset"sv) {
            if (auto huffman_table_selection_for_refinement_delta_x_offset = value.get_uint(); huffman_table_selection_for_refinement_delta_x_offset.has_value()) {
                if (huffman_table_selection_for_refinement_delta_x_offset.value() <= 3) {
                    flags |= huffman_table_selection_for_refinement_delta_x_offset.value() << 10;
                    return {};
                }
            }
            // FIXME: Also allow names "standard_table_14", "standard_table_15", "custom" for values 0, 1, 3.
            return Error::from_string_literal("expected 0, 1, or 3 for \"huffman_table_selection_for_refinement_delta_x_offset\"");
        }

        if (key == "huffman_table_selection_for_refinement_delta_y_offset"sv) {
            if (auto huffman_table_selection_for_refinement_delta_y_offset = value.get_uint(); huffman_table_selection_for_refinement_delta_y_offset.has_value()) {
                if (huffman_table_selection_for_refinement_delta_y_offset.value() <= 3) {
                    flags |= huffman_table_selection_for_refinement_delta_y_offset.value() << 12;
                    return {};
                }
            }
            // FIXME: Also allow names "standard_table_14", "standard_table_15", "custom" for values 0, 1, 3.
            return Error::from_string_literal("expected 0, 1, or 3 for \"huffman_table_selection_for_refinement_delta_y_offset\"");
        }

        if (key == "huffman_table_selection_for_refinement_size_table"sv) {
            if (auto huffman_table_selection_for_refinement_size_table = value.get_uint(); huffman_table_selection_for_refinement_size_table.has_value()) {
                if (huffman_table_selection_for_refinement_size_table.value() <= 1) {
                    flags |= huffman_table_selection_for_refinement_size_table.value() << 14;
                    return {};
                }
            }
            // FIXME: Also allow names "standard_table_1", "custom" for values 0, 1.
            return Error::from_string_literal("expected 0 or 1 for \"huffman_table_selection_for_refinement_size_table\"");
        }

        dbgln("text_region huffman_flags key {}", key);
        return Error::from_string_literal("unknown text_region huffman_flags key");
    }));

    return flags;
}

static ErrorOr<Gfx::JBIG2::TextRegionSegmentData> jbig2_text_region_from_json(ToJSONOptions const&, Optional<JsonObject const&> object)
{
    if (!object.has_value())
        return Error::from_string_literal("text_region segment should have \"data\" object");

    Vector<i8> refinement_adaptive_template_pixels;
    Gfx::JBIG2::TextRegionSegmentData text_region;

    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "region_segment_information"sv) {
            if (value.is_object()) {
                auto region_segment_information = TRY(jbig2_region_segment_information_from_json(value.as_object()));
                if (region_segment_information.use_width_from_image || region_segment_information.use_height_from_image)
                    return Error::from_string_literal("can't use \"from_image\" with text_region");
                text_region.region_segment_information = region_segment_information.region_segment_information;
                return {};
            }
            return Error::from_string_literal("expected object for \"region_segment_information\"");
        }

        if (key == "flags"sv) {
            if (value.is_object()) {
                text_region.flags = TRY(jbig2_text_region_flags_from_json(value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"flags\"");
        }

        if (key == "huffman_flags"sv) {
            if (value.is_object()) {
                text_region.huffman_flags = TRY(jbig2_text_region_huffman_flags_from_json(value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"huffman_flags\"");
        }

        if (key == "refinement_adaptive_template_pixels"sv) {
            if (auto adaptive_template_pixels_json = jbig2_adaptive_template_pixels_from_json(value); adaptive_template_pixels_json.has_value()) {
                refinement_adaptive_template_pixels = adaptive_template_pixels_json.value();
                return {};
            }
            return Error::from_string_literal("expected array of i8 for \"refinement_adaptive_template_pixels\"");
        }

        if (key == "strip_trailing_7fffs"sv)
            text_region.trailing_7fff_handling = TRY(jbig2_trailing_7fff_handling_from_json(value));

        dbgln("text_region key {}", key);
        return Error::from_string_literal("unknown text_region key");
    }));

    bool uses_refinement_coding = (text_region.flags & 2) != 0;
    u8 refinement_template = (text_region.flags >> 15);
    size_t number_of_refinement_adaptive_template_pixels = uses_refinement_coding && refinement_template == 0 ? 2 : 0;
    if (refinement_adaptive_template_pixels.size() != number_of_refinement_adaptive_template_pixels * 2) {
        dbgln("expected {} entries, got {}", number_of_refinement_adaptive_template_pixels * 2, refinement_adaptive_template_pixels.size());
        return Error::from_string_literal("text_region \"data\" object has wrong number of \"refinement_adaptive_template_pixels\"");
    }
    for (size_t i = 0; i < number_of_refinement_adaptive_template_pixels; ++i) {
        text_region.refinement_adaptive_template_pixels[i].x = refinement_adaptive_template_pixels[2 * i];
        text_region.refinement_adaptive_template_pixels[i].y = refinement_adaptive_template_pixels[2 * i + 1];
    }

    return text_region;
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_immediate_text_region_from_json(ToJSONOptions const& options, Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    auto result = TRY(jbig2_text_region_from_json(options, object));
    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::ImmediateTextRegionSegmentData { move(result) } };
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_immediate_lossless_text_region_from_json(ToJSONOptions const& options, Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::ImmediateLosslessTextRegionSegmentData { TRY(jbig2_text_region_from_json(options, object)) } };
}

static ErrorOr<u8> jbig2_pattern_dictionary_flags_from_json(JsonObject const& object)
{
    u8 flags = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "is_modified_modified_read"sv) {
            if (auto is_modified_modified_read = value.get_bool(); is_modified_modified_read.has_value()) {
                if (is_modified_modified_read.value())
                    flags |= 1u;
                return {};
            }
            return Error::from_string_literal("expected bool for \"is_modified_modified_read\"");
        }

        if (key == "pd_template"sv) {
            if (auto pd_template = value.get_uint(); pd_template.has_value()) {
                if (pd_template.value() > 3)
                    return Error::from_string_literal("expected 0, 1, 2, or 3 for \"pd_template\"");
                flags |= pd_template.value() << 1;
                return {};
            }
            return Error::from_string_literal("expected uint for \"pd_template\"");
        }

        dbgln("pattern_dictionary flag key {}", key);
        return Error::from_string_literal("unknown pattern_dictionary flag key");
    }));

    return flags;
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_pattern_dictionary_from_json(ToJSONOptions const& options, Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    if (!object.has_value())
        return Error::from_string_literal("pattern_dictionary segment should have \"data\" object");

    u8 flags = 0;
    u8 pattern_width = 0;
    u8 pattern_height = 0;
    u32 gray_max = 0;
    bool gray_max_from_tiles = false;
    Gfx::MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Keep };
    RefPtr<Gfx::BilevelImage> image;
    enum class Method {
        None,
        DistinctImageTiles,
        UniqueImageTiles,
    };
    Method method = Method::None;

    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "flags"sv) {
            if (value.is_object()) {
                flags = TRY(jbig2_pattern_dictionary_flags_from_json(value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"flags\"");
        }

        if (key == "pattern_width"sv) {
            if (auto pattern_width_json = value.get_u32(); pattern_width_json.has_value()) {
                if (pattern_width_json.value() == 0 || pattern_width_json.value() > 255)
                    return Error::from_string_literal("expected non-zero u8 for \"pattern_width\"");
                pattern_width = pattern_width_json.value();
                return {};
            }
            return Error::from_string_literal("expected u8 for \"pattern_width\"");
        }

        if (key == "pattern_height"sv) {
            if (auto pattern_height_json = value.get_u32(); pattern_height_json.has_value()) {
                if (pattern_height_json.value() == 0 || pattern_height_json.value() > 255)
                    return Error::from_string_literal("expected non-zero u8 for \"pattern_height\"");
                pattern_height = pattern_height_json.value();
                return {};
            }
            return Error::from_string_literal("expected u8 for \"pattern_height\"");
        }

        if (key == "gray_max"sv) {
            if (auto gray_max_json = value.get_u32(); gray_max_json.has_value()) {
                gray_max = gray_max_json.value();
                return {};
            }
            if (value.is_string()) {
                if (value.as_string() == "from_tiles"sv) {
                    gray_max_from_tiles = true;
                    return {};
                }
            }
            return Error::from_string_literal("expected u32 or \"from_tiles\" for \"gray_max\"");
        }

        if (key == "strip_trailing_7fffs"sv) {
            trailing_7fff_handling = TRY(jbig2_trailing_7fff_handling_from_json(value));
            return {};
        }

        // FIXME: Make this more flexible.
        if (key == "image_data"sv) {
            if (value.is_object()) {
                image = TRY(jbig2_image_from_json(options, value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"image_data\"");
        }

        if (key == "method"sv) {
            if (value.is_string()) {
                auto const& method_json = value.as_string();
                if (method_json == "distinct_image_tiles"sv) {
                    method = Method::DistinctImageTiles;
                    return {};
                }
                if (method_json == "unique_image_tiles"sv) {
                    method = Method::UniqueImageTiles;
                    return {};
                }
            }
            return Error::from_string_literal("expected \"distinct_image_tiles\" for \"method\"");
        }

        dbgln("pattern_dictionary key {}", key);
        return Error::from_string_literal("unknown pattern_dictionary key");
    }));

    if (gray_max_from_tiles && method == Method::None)
        return Error::from_string_literal("can't use \"from_tiles\" for gray_max without using a tiling method");

    if (method == Method::DistinctImageTiles || method == Method::UniqueImageTiles) {
        auto number_of_tiles_in_x = ceil_div(image->width(), static_cast<size_t>(pattern_width));
        auto number_of_tiles_in_y = ceil_div(image->height(), static_cast<size_t>(pattern_height));

        // FIXME: For UniqueImageTiles at the edge, we could use a custom hasher/comparator to match existing full tiles
        //        by ignoring pixels outside the clipped tile rect.
        Vector<Gfx::BilevelSubImage> tiles;
        HashTable<Gfx::BilevelSubImage> saw_tile;
        Gfx::IntRect bitmap_rect { 0, 0, static_cast<int>(image->width()), static_cast<int>(image->height()) };
        for (size_t tile_y = 0, tile_index = 0; tile_y < number_of_tiles_in_y; ++tile_y) {
            for (size_t tile_x = 0; tile_x < number_of_tiles_in_x; ++tile_x, ++tile_index) {
                Gfx::IntPoint source_position { static_cast<int>(tile_x * pattern_width), static_cast<int>(tile_y * pattern_height) };
                Gfx::IntRect source_rect { source_position, { pattern_width, pattern_height } };
                source_rect = source_rect.intersected(bitmap_rect);
                auto source = image->subbitmap(source_rect);
                if (method == Method::DistinctImageTiles || saw_tile.set(source) == HashSetResult::InsertedNewEntry)
                    TRY(tiles.try_append(source));
            }
        }

        auto tiled_image = TRY(Gfx::BilevelImage::create(pattern_width * tiles.size(), pattern_height));
        tiled_image->fill(false);
        for (auto const& [i, tile] : enumerate(tiles)) {
            Gfx::IntPoint destination_position { static_cast<int>(i * pattern_width), 0 };
            tile.composite_onto(*tiled_image, destination_position, Gfx::BilevelImage::CompositionType::Replace);
        }

        if (gray_max_from_tiles)
            gray_max = tiles.size() - 1;

        image = move(tiled_image);
    }

    return Gfx::JBIG2::SegmentData {
        header,
        Gfx::JBIG2::PatternDictionarySegmentData {
            flags,
            pattern_width,
            pattern_height,
            gray_max,
            image.release_nonnull(),
            trailing_7fff_handling,
        }
    };
}

static ErrorOr<u8> jbig2_halftone_region_flags_from_json(JsonObject const& object)
{
    u8 flags = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "is_modified_modified_read"sv) {
            if (auto is_modified_modified_read = value.get_bool(); is_modified_modified_read.has_value()) {
                if (is_modified_modified_read.value())
                    flags |= 1u;
                return {};
            }
            return Error::from_string_literal("expected bool for \"is_modified_modified_read\"");
        }

        if (key == "ht_template"sv) {
            if (auto ht_template = value.get_uint(); ht_template.has_value()) {
                if (ht_template.value() > 3)
                    return Error::from_string_literal("expected 0, 1, 2, or 3 for \"ht_template\"");
                flags |= ht_template.value() << 1;
                return {};
            }
            return Error::from_string_literal("expected uint for \"gb_template\"");
        }

        if (key == "enable_skip"sv) {
            if (auto enable_skip = value.get_bool(); enable_skip.has_value()) {
                if (enable_skip.value())
                    flags |= 1u << 3;
                return {};
            }
            return Error::from_string_literal("expected bool for \"enable_skip\"");
        }

        if (key == "combination_operator"sv) {
            if (value.is_string()) {
                auto const& s = value.as_string();
                if (s == "or"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::Or) << 4;
                else if (s == "and"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::And) << 4;
                else if (s == "xor"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::Xor) << 4;
                else if (s == "xnor"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::XNor) << 4;
                else if (s == "replace"sv)
                    flags |= to_underlying(Gfx::JBIG2::CombinationOperator::Replace) << 4;
                else
                    return Error::from_string_literal("expected \"or\", \"and\", \"xor\", \"xnor\", or \"replace\" for \"combination_operator\"");
                return {};
            }
            return Error::from_string_literal("expected \"or\", \"and\", \"xor\", \"xnor\", or \"replace\" for \"combination_operator\"");
        }

        if (key == "default_pixel_value"sv) {
            if (value.is_string()) {
                auto const& s = value.as_string();
                if (s == "white"sv)
                    flags |= 0;
                else if (s == "black"sv)
                    flags |= 1u << 7;
                else
                    return Error::from_string_literal("expected \"white\" or \"black\" for \"default_pixel_value\"");
                return {};
            }
            return Error::from_string_literal("expected \"white\" or \"black\" for \"default_pixel_value\"");
        }

        dbgln("halftone_region flag key {}", key);
        return Error::from_string_literal("unknown halftone_region flag key");
    }));

    bool uses_mmr = flags & 1;
    u8 ht_template = (flags >> 1) & 3;
    if (uses_mmr && ht_template != 0)
        return Error::from_string_literal("if is_modified_modified_read is true, ht_template must be 0");

    return flags;
}

static ErrorOr<Vector<u64>> jbig2_halftone_graymap_from_json(ToJSONOptions const&, JsonObject const& object)
{
    Vector<u64> graymap;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "array") {
            if (value.is_array()) {
                for (auto const& row : value.as_array().values()) {
                    if (!row.is_array())
                        return Error::from_string_literal("expected array for \"array\" entries");

                    for (auto const& element : row.as_array().values()) {
                        if (auto value = element.get_u64(); value.has_value()) {
                            TRY(graymap.try_append(value.value()));
                            continue;
                        }
                        return Error::from_string_literal("expected u64 for \"graymap_data\" elements");
                    }
                }
                return {};
            }
            return Error::from_string_literal("expected array for \"array\"");
        }

        dbgln("graymap_data key {}", key);
        return Error::from_string_literal("unknown graymap_data key");
    }));

    return graymap;
}

static ErrorOr<Gfx::JBIG2::HalftoneRegionSegmentData> jbig2_halftone_region_from_json(ToJSONOptions const& options, Optional<JsonObject const&> object)
{
    if (!object.has_value())
        return Error::from_string_literal("halftone_region segment should have \"data\" object");

    Gfx::JBIG2::HalftoneRegionSegmentData halftone_region;

    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "region_segment_information"sv) {
            if (value.is_object()) {
                auto region_segment_information = TRY(jbig2_region_segment_information_from_json(value.as_object()));
                if (region_segment_information.use_width_from_image || region_segment_information.use_height_from_image)
                    return Error::from_string_literal("can't use \"from_image\" with halftone_region");
                halftone_region.region_segment_information = region_segment_information.region_segment_information;
                return {};
            }
            return Error::from_string_literal("expected object for \"region_segment_information\"");
        }

        if (key == "flags"sv) {
            if (value.is_object()) {
                halftone_region.flags = TRY(jbig2_halftone_region_flags_from_json(value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"flags\"");
        }

        if (key == "grayscale_width"sv) {
            if (auto grayscale_width = value.get_u32(); grayscale_width.has_value()) {
                halftone_region.grayscale_width = grayscale_width.value();
                return {};
            }
            return Error::from_string_literal("expected u32 for \"grayscale_width\"");
        }

        if (key == "grayscale_height"sv) {
            if (auto grayscale_height = value.get_u32(); grayscale_height.has_value()) {
                halftone_region.grayscale_height = grayscale_height.value();
                return {};
            }
            return Error::from_string_literal("expected u32 for \"grayscale_height\"");
        }

        if (key == "grid_offset_x_times_256"sv) {
            if (auto grid_offset_x = value.get_i32(); grid_offset_x.has_value()) {
                halftone_region.grid_offset_x_times_256 = grid_offset_x.value();
                return {};
            }
            return Error::from_string_literal("expected i32 for \"grid_offset_x\"");
        }

        if (key == "grid_offset_y_times_256"sv) {
            if (auto grid_offset_y = value.get_i32(); grid_offset_y.has_value()) {
                halftone_region.grid_offset_y_times_256 = grid_offset_y.value();
                return {};
            }
            return Error::from_string_literal("expected i32 for \"grid_offset_y\"");
        }

        if (key == "grid_vector_x_times_256"sv) {
            if (auto grid_vector_x = value.get_u32(); grid_vector_x.has_value()) {
                if (grid_vector_x.value() > 0xffff)
                    return Error::from_string_literal("expected u16 for \"grid_vector_x\"");
                halftone_region.grid_vector_x_times_256 = grid_vector_x.value();
                return {};
            }
            return Error::from_string_literal("expected u16 for \"grid_vector_x\"");
        }

        if (key == "grid_vector_y_times_256"sv) {
            if (auto grid_vector_y = value.get_u32(); grid_vector_y.has_value()) {
                if (grid_vector_y.value() > 0xffff)
                    return Error::from_string_literal("expected u16 for \"grid_vector_y\"");
                halftone_region.grid_vector_y_times_256 = grid_vector_y.value();
                return {};
            }
            return Error::from_string_literal("expected u16 for \"grid_vector_y\"");
        }

        if (key == "strip_trailing_7fffs"sv) {
            halftone_region.trailing_7fff_handling = TRY(jbig2_trailing_7fff_handling_from_json(value));
            return {};
        }

        if (key == "graymap_data"sv) {
            if (value.is_object()) {
                halftone_region.grayscale_image = TRY(jbig2_halftone_graymap_from_json(options, value.as_object()));
                return {};
            }
            if (value.is_string()) {
                if (value.as_string() == "identity_tile_indices"sv) {
                    halftone_region.grayscale_image.clear();
                    u32 num_pixels = halftone_region.grayscale_width * halftone_region.grayscale_height;
                    for (u32 i = 0; i < num_pixels; ++i)
                        TRY(halftone_region.grayscale_image.try_append(i));
                    return {};
                }
            }
            return Error::from_string_literal("expected object or \"identity_tile_indices\" for \"graymap_data\"");
        }

        dbgln("halftone_region key {}", key);
        return Error::from_string_literal("unknown halftone_region key");
    }));

    return halftone_region;
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_immediate_halftone_region_from_json(ToJSONOptions const& options, Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    auto result = TRY(jbig2_halftone_region_from_json(options, object));
    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::ImmediateHalftoneRegionSegmentData { move(result) } };
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_immediate_lossless_halftone_region_from_json(ToJSONOptions const& options, Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::ImmediateLosslessHalftoneRegionSegmentData { TRY(jbig2_halftone_region_from_json(options, object)) } };
}

static ErrorOr<u8> jbig2_generic_region_flags_from_json(JsonObject const& object)
{
    u8 flags = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "is_modified_modified_read"sv) {
            if (auto is_modified_modified_read = value.get_bool(); is_modified_modified_read.has_value()) {
                if (is_modified_modified_read.value())
                    flags |= 1u;
                return {};
            }
            return Error::from_string_literal("expected bool for \"is_modified_modified_read\"");
        }

        if (key == "gb_template"sv) {
            if (auto gb_template = value.get_uint(); gb_template.has_value()) {
                if (gb_template.value() > 3)
                    return Error::from_string_literal("expected 0, 1, 2, or 3 for \"gb_template\"");
                flags |= gb_template.value() << 1;
                return {};
            }
            return Error::from_string_literal("expected uint for \"gb_template\"");
        }

        if (key == "use_typical_prediction"sv) {
            if (auto use_typical_prediction = value.get_bool(); use_typical_prediction.has_value()) {
                if (use_typical_prediction.value())
                    flags |= 1u << 3;
                return {};
            }
            return Error::from_string_literal("expected bool for \"use_typical_prediction\"");
        }

        if (key == "use_extended_template"sv) {
            if (auto use_extended_template = value.get_bool(); use_extended_template.has_value()) {
                if (use_extended_template.value())
                    flags |= 1u << 4;
                return {};
            }
            return Error::from_string_literal("expected bool for \"use_extended_template\"");
        }

        dbgln("generic_region flag key {}", key);
        return Error::from_string_literal("unknown generic_region flag key");
    }));

    bool uses_mmr = flags & 1;
    if (uses_mmr && (flags & ~1) != 0)
        return Error::from_string_literal("if is_modified_modified_read is true, other flags must be false");

    return flags;
}

static ErrorOr<Gfx::JBIG2::GenericRegionSegmentData> jbig2_generic_region_from_json(ToJSONOptions const& options, Optional<JsonObject const&> object)
{
    if (!object.has_value())
        return Error::from_string_literal("generic_region segment should have \"data\" object");

    RegionSegmentInformatJSON region_segment_information;
    region_segment_information.use_width_from_image = true;
    region_segment_information.use_height_from_image = true;
    Optional<u32> real_height_for_generic_region_of_initially_unknown_size;
    u8 flags = 0;
    Vector<i8> adaptive_template_pixels;
    Gfx::MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Keep };
    RefPtr<Gfx::BilevelImage> image;
    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "region_segment_information"sv) {
            if (value.is_object()) {
                region_segment_information = TRY(jbig2_region_segment_information_from_json(value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"region_segment_information\"");
        }

        if (key == "real_height_for_generic_region_of_initially_unknown_size"sv) {
            if (auto real_height_for_generic_region_of_initially_unknown_size_json = value.get_u32(); real_height_for_generic_region_of_initially_unknown_size_json.has_value()) {
                real_height_for_generic_region_of_initially_unknown_size = real_height_for_generic_region_of_initially_unknown_size_json.value();
                return {};
            }
            return Error::from_string_literal("expected u32 for \"real_height_for_generic_region_of_initially_unknown_size\"");
        }

        if (key == "flags"sv) {
            if (value.is_object()) {
                flags = TRY(jbig2_generic_region_flags_from_json(value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"flags\"");
        }

        if (key == "adaptive_template_pixels"sv) {
            if (auto adaptive_template_pixels_json = jbig2_adaptive_template_pixels_from_json(value); adaptive_template_pixels_json.has_value()) {
                adaptive_template_pixels = adaptive_template_pixels_json.value();
                return {};
            }
            return Error::from_string_literal("expected array of i8 for \"adaptive_template_pixels\"");
        }

        if (key == "strip_trailing_7fffs"sv) {
            trailing_7fff_handling = TRY(jbig2_trailing_7fff_handling_from_json(value));
            return {};
        }

        if (key == "image_data"sv) {
            if (value.is_object()) {
                image = TRY(jbig2_image_from_json(options, value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"image_data\"");
        }

        dbgln("generic_region key {}", key);
        return Error::from_string_literal("unknown generic_region key");
    }));

    if (!image)
        return Error::from_string_literal("generic_region \"data\" object missing required key \"image_data\"");

    if (region_segment_information.use_width_from_image)
        region_segment_information.region_segment_information.width = image->width();
    if (region_segment_information.use_height_from_image)
        region_segment_information.region_segment_information.height = image->height();

    if (region_segment_information.region_segment_information.width != image->width()
        || real_height_for_generic_region_of_initially_unknown_size.value_or(region_segment_information.region_segment_information.height) != image->height()) {
        dbgln("generic_region's region_segment_information width/height: {}x{}{}, image dimensions: {}x{}",
            region_segment_information.region_segment_information.width, region_segment_information.region_segment_information.height,
            real_height_for_generic_region_of_initially_unknown_size.has_value() ? MUST(String::formatted("(overridden with {})", real_height_for_generic_region_of_initially_unknown_size.value())) : ""sv,
            image->width(), image->height());
        return Error::from_string_literal("generic_region's region_segment_information width/height do not match image dimensions");
    }

    bool uses_mmr = flags & 1;
    bool use_extended_template = (flags >> 4) & 1;
    u8 gb_template = (flags >> 1) & 3;
    if (adaptive_template_pixels.is_empty() && !uses_mmr)
        adaptive_template_pixels = default_adaptive_template_pixels(gb_template, use_extended_template);

    size_t number_of_adaptive_template_pixels = 0;
    if (!uses_mmr) {
        if (gb_template == 0)
            number_of_adaptive_template_pixels = use_extended_template ? 12 : 4;
        else
            number_of_adaptive_template_pixels = 1;
    }
    if (adaptive_template_pixels.size() != number_of_adaptive_template_pixels * 2) {
        dbgln("expected {} entries, got {}", number_of_adaptive_template_pixels * 2, adaptive_template_pixels.size());
        return Error::from_string_literal("generic_region \"data\" object has wrong number of \"adaptive_template_pixels\"");
    }
    Array<Gfx::JBIG2::AdaptiveTemplatePixel, 12> template_pixels {};
    for (size_t i = 0; i < number_of_adaptive_template_pixels; ++i) {
        template_pixels[i].x = adaptive_template_pixels[2 * i];
        template_pixels[i].y = adaptive_template_pixels[2 * i + 1];
    }

    return Gfx::JBIG2::GenericRegionSegmentData {
        region_segment_information.region_segment_information,
        flags,
        template_pixels,
        image.release_nonnull(),
        real_height_for_generic_region_of_initially_unknown_size,
        trailing_7fff_handling,
    };
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_immediate_generic_region_from_json(ToJSONOptions const& options, Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    auto result = TRY(jbig2_generic_region_from_json(options, object));
    if (header.is_immediate_generic_region_of_initially_unknown_size != result.real_height_for_generic_region_of_initially_unknown_size.has_value())
        return Error::from_string_literal("is_immediate_generic_region_of_initially_unknown_size and data.real_height_for_generic_region_of_initially_unknown_size must be set together");
    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::ImmediateGenericRegionSegmentData { move(result) } };
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_immediate_lossless_generic_region_from_json(ToJSONOptions const& options, Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::ImmediateLosslessGenericRegionSegmentData { TRY(jbig2_generic_region_from_json(options, object)) } };
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_intermediate_generic_region_from_json(ToJSONOptions const& options, Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::IntermediateGenericRegionSegmentData { TRY(jbig2_generic_region_from_json(options, object)) } };
}

static ErrorOr<u8> jbig2_refinement_region_flags_from_json(JsonObject const& object)
{
    u8 flags = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "gr_template"sv) {
            if (auto gr_template = value.get_uint(); gr_template.has_value()) {
                if (gr_template.value() > 1)
                    return Error::from_string_literal("expected 0 or 1 for \"gr_template\"");
                flags |= gr_template.value();
                return {};
            }
            return Error::from_string_literal("expected uint for \"gr_template\"");
        }

        if (key == "use_typical_prediction"sv) {
            if (auto use_typical_prediction = value.get_bool(); use_typical_prediction.has_value()) {
                if (use_typical_prediction.value())
                    flags |= 1u << 1;
                return {};
            }
            return Error::from_string_literal("expected bool for \"use_typical_prediction\"");
        }

        dbgln("generic_refinement_region flag key {}", key);
        return Error::from_string_literal("unknown generic_refinement_region flag key");
    }));

    return flags;
}

static ErrorOr<Gfx::JBIG2::GenericRefinementRegionSegmentData> jbig2_generic_refinement_region_from_json(ToJSONOptions const& options, Optional<JsonObject const&> object)
{
    if (!object.has_value())
        return Error::from_string_literal("generic_refinement_region segment should have \"data\" object");

    RegionSegmentInformatJSON region_segment_information;
    region_segment_information.use_width_from_image = true;
    region_segment_information.use_height_from_image = true;
    u8 flags = 0;
    Vector<i8> adaptive_template_pixels;
    Gfx::MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Keep };
    RefPtr<Gfx::BilevelImage> image;
    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "region_segment_information"sv) {
            if (value.is_object()) {
                region_segment_information = TRY(jbig2_region_segment_information_from_json(value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"region_segment_information\"");
        }

        if (key == "flags"sv) {
            if (value.is_object()) {
                flags = TRY(jbig2_refinement_region_flags_from_json(value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"flags\"");
        }

        if (key == "adaptive_template_pixels"sv) {
            if (auto adaptive_template_pixels_json = jbig2_adaptive_template_pixels_from_json(value); adaptive_template_pixels_json.has_value()) {
                adaptive_template_pixels = adaptive_template_pixels_json.value();
                return {};
            }
            return Error::from_string_literal("expected array of i8 for \"adaptive_template_pixels\"");
        }

        if (key == "strip_trailing_7fffs"sv) {
            trailing_7fff_handling = TRY(jbig2_trailing_7fff_handling_from_json(value));
            return {};
        }

        if (key == "image_data"sv) {
            if (value.is_object()) {
                image = TRY(jbig2_image_from_json(options, value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"image_data\"");
        }

        dbgln("generic_refinement_region key {}", key);
        return Error::from_string_literal("unknown generic_refinement_region key");
    }));

    if (!image)
        return Error::from_string_literal("generic_refinement_region \"data\" object missing required key \"image_data\"");

    if (region_segment_information.use_width_from_image)
        region_segment_information.region_segment_information.width = image->width();
    if (region_segment_information.use_height_from_image)
        region_segment_information.region_segment_information.height = image->height();

    if (region_segment_information.region_segment_information.width != image->width()
        || region_segment_information.region_segment_information.height != image->height()) {
        dbgln("generic_refinement_region's region_segment_information width/height: {}x{}, image dimensions: {}x{}",
            region_segment_information.region_segment_information.width, region_segment_information.region_segment_information.height,
            image->width(), image->height());
        return Error::from_string_literal("generic_refinement_region's region_segment_information width/height do not match image dimensions");
    }

    u8 gr_template = flags & 1;
    if (adaptive_template_pixels.is_empty())
        adaptive_template_pixels = default_refinement_adaptive_template_pixels(gr_template);

    size_t number_of_adaptive_template_pixels = gr_template == 0 ? 2 : 0;
    if (adaptive_template_pixels.size() != number_of_adaptive_template_pixels * 2) {
        dbgln("expected {} entries, got {}", number_of_adaptive_template_pixels * 2, adaptive_template_pixels.size());
        return Error::from_string_literal("generic_refinement_region \"data\" object has wrong number of \"adaptive_template_pixels\"");
    }
    Array<Gfx::JBIG2::AdaptiveTemplatePixel, 2> template_pixels {};
    for (size_t i = 0; i < number_of_adaptive_template_pixels; ++i) {
        template_pixels[i].x = adaptive_template_pixels[2 * i];
        template_pixels[i].y = adaptive_template_pixels[2 * i + 1];
    }

    return Gfx::JBIG2::GenericRefinementRegionSegmentData {
        region_segment_information.region_segment_information,
        flags,
        template_pixels,
        image.release_nonnull(),
        trailing_7fff_handling,
    };
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_immediate_generic_refinement_region_from_json(ToJSONOptions const& options, Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::ImmediateGenericRefinementRegionSegmentData { TRY(jbig2_generic_refinement_region_from_json(options, object)) } };
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_immediate_lossless_generic_refinement_region_from_json(ToJSONOptions const& options, Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::ImmediateLosslessGenericRefinementRegionSegmentData { TRY(jbig2_generic_refinement_region_from_json(options, object)) } };
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_intermediate_generic_refinement_region_from_json(ToJSONOptions const& options, Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::IntermediateGenericRefinementRegionSegmentData { TRY(jbig2_generic_refinement_region_from_json(options, object)) } };
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

static ErrorOr<u16> jbig2_page_information_striping_information_from_json(JsonObject const& object)
{
    u16 striping_information = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "is_striped"sv) {
            if (auto is_striped = value.get_bool(); is_striped.has_value()) {
                if (is_striped.value())
                    striping_information |= 0x8000u;
                return {};
            }
            return Error::from_string_literal("expected bool for \"is_striped\"");
        }

        if (key == "maximum_stripe_size"sv) {
            if (auto maximum_stripe_size = value.get_u32(); maximum_stripe_size.has_value()) {
                if (maximum_stripe_size.value() > 0x7FFF)
                    return Error::from_string_literal("maximum_stripe_size should be <= 32767");
                striping_information |= maximum_stripe_size.value();
                return {};
            }
            return Error::from_string_literal("expected u32 for \"maximum_stripe_size\"");
        }

        dbgln("page_information striping_information key {}", key);
        return Error::from_string_literal("unknown page_information striping_information key");
    }));

    return striping_information;
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_page_information_from_json(Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    if (!object.has_value())
        return Error::from_string_literal("page_information segment should have \"data\" object");

    Gfx::JBIG2::PageInformationSegment data {};

    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "page_width"sv) {
            if (auto page_width = value.get_u32(); page_width.has_value()) {
                data.bitmap_width = page_width.value();
                return {};
            }
            return Error::from_string_literal("expected u32 for \"page_width\"");
        }

        if (key == "page_height"sv) {
            if (auto page_height = value.get_u32(); page_height.has_value()) {
                data.bitmap_height = page_height.value();
                return {};
            }
            if (value.is_null()) {
                data.bitmap_height = 0xffff'ffff;
                return {};
            }
            return Error::from_string_literal("expected u32 or null for \"page_height\"");
        }

        if (key == "page_x_resolution"sv) {
            if (auto page_x_resolution = value.get_u32(); page_x_resolution.has_value()) {
                data.page_x_resolution = page_x_resolution.value();
                return {};
            }
            return Error::from_string_literal("expected u32 for \"page_x_resolution\"");
        }

        if (key == "page_y_resolution"sv) {
            if (auto page_y_resolution = value.get_u32(); page_y_resolution.has_value()) {
                data.page_y_resolution = page_y_resolution.value();
                return {};
            }
            return Error::from_string_literal("expected u32 for \"page_y_resolution\"");
        }
        if (key == "flags"sv) {
            if (value.is_object()) {
                data.flags = TRY(jbig2_page_information_flags_from_json(value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"flags\"");
        }

        if (key == "striping_information"sv) {
            if (value.is_object()) {
                data.striping_information = TRY(jbig2_page_information_striping_information_from_json(value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"striping_information\"");
        }

        dbgln("page_information key {}", key);
        return Error::from_string_literal("unknown page_information key");
    }));

    return Gfx::JBIG2::SegmentData { header, data };
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_end_of_page_from_json(Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    if (object.has_value())
        return Error::from_string_literal("end_of_page segment should have no \"data\" object");
    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::EndOfPageSegmentData {} };
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_end_of_stripe_from_json(Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    if (!object.has_value())
        return Error::from_string_literal("end_of_stripe segment needs a \"data\" object");

    Optional<u32> y_coordinate;

    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "y_coordinate"sv) {
            if (auto y = value.get_u32(); y.has_value()) {
                y_coordinate = y.value();
                return {};
            }
            return Error::from_string_literal("expected u32 for \"y_coordinate\"");
        }

        dbgln("end_of_stripe key {}", key);
        return Error::from_string_literal("unknown end_of_stripe key");
    }));

    if (!y_coordinate.has_value())
        return Error::from_string_literal("end_of_stripe segment missing required \"y_coordinate\" key");

    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::EndOfStripeSegment { y_coordinate.value() } };
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_end_of_file_from_json(Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    if (object.has_value())
        return Error::from_string_literal("end_of_file segment should have no \"data\" object");
    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::EndOfFileSegmentData {} };
}

static ErrorOr<u8> jbig2_tables_flags_from_json(JsonObject const& object)
{
    u8 flags = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "has_out_of_band_symbol"sv) {
            if (auto has_out_of_band_symbol = value.get_bool(); has_out_of_band_symbol.has_value()) {
                if (has_out_of_band_symbol.value())
                    flags |= 1u;
                return {};
            }
            return Error::from_string_literal("expected bool for \"has_out_of_band_symbol\"");
        }

        if (key == "prefix_bit_count"sv) {
            if (auto prefix_bit_count = value.get_u32(); prefix_bit_count.has_value()) {
                if (prefix_bit_count.value() == 0 || prefix_bit_count.value() > 8)
                    return Error::from_string_literal("expected 1..8 for \"prefix_bit_count\"");
                flags |= (prefix_bit_count.value() - 1) << 1;
                return {};
            }
            return Error::from_string_literal("expected 1..8 for \"prefix_bit_count\"");
        }

        if (key == "range_bit_count"sv) {
            if (auto range_bit_count = value.get_u32(); range_bit_count.has_value()) {
                if (range_bit_count.value() == 0 || range_bit_count.value() > 8)
                    return Error::from_string_literal("expected 1..8 for \"range_bit_count\"");
                flags |= (range_bit_count.value() - 1) << 4;
                return {};
            }
            return Error::from_string_literal("expected 1..8 for \"range_bit_count\"");
        }

        dbgln("tables flag key {}", key);
        return Error::from_string_literal("unknown tables flag key");
    }));

    return flags;
}

static ErrorOr<Vector<Gfx::JBIG2::TablesData::Entry>> jbig2_tables_entries_from_json(JsonArray const& array)
{
    Vector<Gfx::JBIG2::TablesData::Entry> entries;

    for (auto const& value : array.values()) {
        if (value.is_object()) {
            Gfx::JBIG2::TablesData::Entry entry;

            TRY(value.as_object().try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
                if (key == "prefix_length"sv) {
                    if (auto prefix_length = value.get_u32(); prefix_length.has_value()) {
                        entry.prefix_length = prefix_length.value();
                        return {};
                    }
                    return Error::from_string_literal("expected u32 for \"prefix_length\"");
                }

                if (key == "range_length"sv) {
                    if (auto range_length = value.get_u32(); range_length.has_value()) {
                        entry.range_length = range_length.value();
                        return {};
                    }
                    return Error::from_string_literal("expected u32 for \"range_length\"");
                }

                dbgln("tables entry key {}", key);
                return Error::from_string_literal("unknown tables entry key");
            }));

            entries.append(entry);
            continue;
        }
        return Error::from_string_literal("tables entries should be objects");
    }

    return entries;
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_tables_from_json(Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    if (!object.has_value())
        return Error::from_string_literal("page_information segment should have \"data\" object");

    Gfx::JBIG2::TablesData data {};

    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "flags"sv) {
            if (value.is_object()) {
                data.flags = TRY(jbig2_tables_flags_from_json(value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"flags\"");
        }

        if (key == "lowest_value"sv) {
            if (auto lowest_value = value.get_i32(); lowest_value.has_value()) {
                data.lowest_value = lowest_value.value();
                return {};
            }
            return Error::from_string_literal("expected i32 for \"lowest_value\"");
        }

        if (key == "highest_value"sv) {
            if (auto highest_value = value.get_i32(); highest_value.has_value()) {
                data.highest_value = highest_value.value();
                return {};
            }
            return Error::from_string_literal("expected i32 for \"highest_value\"");
        }

        if (key == "entries"sv) {
            if (value.is_array()) {
                data.entries = TRY(jbig2_tables_entries_from_json(value.as_array()));
                return {};
            }
            return Error::from_string_literal("expected array for \"entries\"");
        }

        if (key == "lower_range_prefix_length"sv) {
            if (auto lower_range_prefix_length = value.get_u32(); lower_range_prefix_length.has_value()) {
                if (lower_range_prefix_length.value() > 255)
                    return Error::from_string_literal("expected u8 for \"lower_range_prefix_length\"");
                data.lower_range_prefix_length = lower_range_prefix_length.value();
                return {};
            }
            return Error::from_string_literal("expected u8 for \"lower_range_prefix_length\"");
        }

        if (key == "upper_range_prefix_length"sv) {
            if (auto upper_range_prefix_length = value.get_u32(); upper_range_prefix_length.has_value()) {
                if (upper_range_prefix_length.value() > 255)
                    return Error::from_string_literal("expected u8 for \"upper_range_prefix_length\"");
                data.upper_range_prefix_length = upper_range_prefix_length.value();
                return {};
            }
            return Error::from_string_literal("expected u8 for \"upper_range_prefix_length\"");
        }

        if (key == "out_of_band_prefix_length"sv) {
            if (auto out_of_band_prefix_length = value.get_u32(); out_of_band_prefix_length.has_value()) {
                if (out_of_band_prefix_length.value() > 255)
                    return Error::from_string_literal("expected u8 for \"out_of_band_prefix_length\"");
                data.out_of_band_prefix_length = out_of_band_prefix_length.value();
                return {};
            }
            return Error::from_string_literal("expected u8 for \"out_of_band_prefix_length\"");
        }

        dbgln("tables key {}", key);
        return Error::from_string_literal("unknown tables key");
    }));

    if (data.out_of_band_prefix_length != 0 && (data.flags & 1) == 0)
        return Error::from_string_literal("out_of_band_prefix_length is non-zero, but has_out_of_band_symbol is false in flags");

    return Gfx::JBIG2::SegmentData { header, data };
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_extension_from_json(Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    if (!object.has_value())
        return Error::from_string_literal("extension segment should have \"data\" object");

    Gfx::JBIG2::ExtensionData data {};

    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "type"sv) {
            if (value.is_string()) {
                auto type = value.as_string();
                if (type == "single_byte_coded_comment"sv) {
                    data.type = Gfx::JBIG2::ExtensionType::SingleByteCodedComment;
                    return {};
                }
                if (type == "multi_byte_coded_comment"sv) {
                    data.type = Gfx::JBIG2::ExtensionType::MultiByteCodedComment;
                    return {};
                }
            }
            return Error::from_string_literal("expected \"single_byte_coded_comment\" or \"multi_byte_coded_comment\" for \"type\"");
        }

        if (key == "entries"sv) {
            if (value.is_array()) {
                for (auto const& entry : value.as_array().values()) {
                    if (!entry.is_array())
                        return Error::from_string_literal("expected array for \"entries\" elements");
                    auto const& entry_array = entry.as_array();
                    if (entry_array.values().size() != 2)
                        return Error::from_string_literal("expected 2 elements in \"entries\" elements");
                    if (!entry_array.values()[0].is_string())
                        return Error::from_string_literal("expected string for \"entries\" element 0");
                    if (!entry_array.values()[1].is_string())
                        return Error::from_string_literal("expected string for \"entries\" element 1");
                    TRY(data.entries.try_append({
                        TRY(String::from_byte_string(entry_array.values()[0].as_string())),
                        TRY(String::from_byte_string(entry_array.values()[1].as_string())),
                    }));
                }
                return {};
            }
            return Error::from_string_literal("expected array for \"entries\"");
        }

        dbgln("extension key {}", key);
        return Error::from_string_literal("unknown extension key");
    }));

    return Gfx::JBIG2::SegmentData { header, data };
}

static ErrorOr<Gfx::JBIG2::SegmentHeaderData::Reference> jbig2_referred_to_segment_from_json(JsonObject const& object)
{
    Gfx::JBIG2::SegmentHeaderData::Reference reference;
    bool has_retention_flag = false;
    bool has_segment_number = false;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "retained"sv) {
            if (auto retained = value.get_bool(); retained.has_value()) {
                reference.retention_flag = retained.value();
                has_retention_flag = true;
                return {};
            }
            return Error::from_string_literal("expected bool for \"retained\"");
        }

        if (key == "segment_number"sv) {
            if (auto segment_number = value.get_u32(); segment_number.has_value()) {
                reference.segment_number = segment_number.value();
                has_segment_number = true;
                return {};
            }
            return Error::from_string_literal("expected u32 for \"segment_number\"");
        }

        dbgln("referred_to_segment key {}", key);
        return Error::from_string_literal("unknown referred_to_segments entry key");
    }));

    if (!has_retention_flag)
        return Error::from_string_literal("referred_to_segment missing \"retained\"");
    if (!has_segment_number)
        return Error::from_string_literal("referred_to_segment missing \"segment_number\"");

    return reference;
}

static ErrorOr<Vector<Gfx::JBIG2::SegmentHeaderData::Reference>> jbig2_referred_to_segments_from_json(JsonArray const& array)
{
    Vector<Gfx::JBIG2::SegmentHeaderData::Reference> referred_to_segments;

    for (auto const& value : array.values()) {
        if (!value.is_object())
            return Error::from_string_literal("referred_to_segments elements should be objects");
        TRY(referred_to_segments.try_append(TRY(jbig2_referred_to_segment_from_json(value.as_object()))));
    }

    return referred_to_segments;
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_segment_from_json(ToJSONOptions const& options, JsonObject const& segment_object)
{
    Gfx::JBIG2::SegmentHeaderData header;

    Optional<ByteString> type_string;
    Optional<JsonObject const&> segment_data_object;

    TRY(segment_object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "segment_number"sv) {
            if (auto segment_number = value.get_u32(); segment_number.has_value()) {
                header.segment_number = segment_number.value();
                return {};
            }
            return Error::from_string_literal("expected u32 for \"segment_number\"");
        }

        if (key == "type"sv) {
            if (value.is_string()) {
                type_string = value.as_string();
                return {};
            }
            return Error::from_string_literal("expected string for \"type\"");
        }

        if (key == "force_32_bit_page_association"sv) {
            if (auto force_32_bit_page_association = value.get_bool(); force_32_bit_page_association.has_value()) {
                header.force_32_bit_page_association = force_32_bit_page_association.value();
                return {};
            }
            return Error::from_string_literal("expected bool for \"force_32_bit_page_association\"");
        }

        if (key == "is_immediate_generic_region_of_initially_unknown_size"sv) {
            if (auto is_immediate_generic_region_of_initially_unknown_size = value.get_bool(); is_immediate_generic_region_of_initially_unknown_size.has_value()) {
                header.is_immediate_generic_region_of_initially_unknown_size = is_immediate_generic_region_of_initially_unknown_size.value();
                return {};
            }
            return Error::from_string_literal("expected bool for \"is_immediate_generic_region_of_initially_unknown_size\"");
        }

        if (key == "page_association"sv) {
            if (auto page_association = value.get_u32(); page_association.has_value()) {
                header.page_association = page_association.value();
                return {};
            }
            return Error::from_string_literal("expected u32 for \"page_association\"");
        }

        if (key == "referred_to_segments"sv) {
            if (value.is_array()) {
                header.referred_to_segments = TRY(jbig2_referred_to_segments_from_json(value.as_array()));
                return {};
            }
            return Error::from_string_literal("expected array for \"referred_to_segments\"");
        }

        if (key == "retained"sv) {
            if (auto retained = value.get_bool(); retained.has_value()) {
                header.retention_flag = retained.value();
                return {};
            }
            return Error::from_string_literal("expected bool for \"retained\"");
        }

        if (key == "data"sv) {
            if (value.is_object()) {
                segment_data_object = value.as_object();
                return {};
            }
            return Error::from_string_literal("expected object for \"data\"");
        }

        dbgln("segment key {}", key);
        return Error::from_string_literal("unknown segment key");
    }));

    if (!type_string.has_value())
        return Error::from_string_literal("segment missing \"type\"");

    if (header.is_immediate_generic_region_of_initially_unknown_size && type_string != "generic_region"sv)
        return Error::from_string_literal("is_immediate_generic_region_of_initially_unknown_size can only be set for type \"generic_region\"");

    if (type_string == "symbol_dictionary")
        return jbig2_symbol_dictionary_from_json(options, header, segment_data_object);
    if (type_string == "text_region")
        return jbig2_immediate_text_region_from_json(options, header, segment_data_object);
    if (type_string == "lossless_text_region")
        return jbig2_immediate_lossless_text_region_from_json(options, header, segment_data_object);
    if (type_string == "pattern_dictionary")
        return jbig2_pattern_dictionary_from_json(options, header, segment_data_object);
    if (type_string == "halftone_region")
        return jbig2_immediate_halftone_region_from_json(options, header, segment_data_object);
    if (type_string == "lossless_halftone_region")
        return jbig2_immediate_lossless_halftone_region_from_json(options, header, segment_data_object);
    if (type_string == "generic_region")
        return jbig2_immediate_generic_region_from_json(options, header, segment_data_object);
    if (type_string == "lossless_generic_region")
        return jbig2_immediate_lossless_generic_region_from_json(options, header, segment_data_object);
    if (type_string == "intermediate_generic_region")
        return jbig2_intermediate_generic_region_from_json(options, header, segment_data_object);
    if (type_string == "generic_refinement_region")
        return jbig2_immediate_generic_refinement_region_from_json(options, header, segment_data_object);
    if (type_string == "lossless_generic_refinement_region")
        return jbig2_immediate_lossless_generic_refinement_region_from_json(options, header, segment_data_object);
    if (type_string == "intermediate_generic_refinement_region")
        return jbig2_intermediate_generic_refinement_region_from_json(options, header, segment_data_object);
    if (type_string == "page_information")
        return jbig2_page_information_from_json(header, segment_data_object);
    if (type_string == "end_of_page")
        return jbig2_end_of_page_from_json(header, segment_data_object);
    if (type_string == "end_of_stripe")
        return jbig2_end_of_stripe_from_json(header, segment_data_object);
    if (type_string == "end_of_file")
        return jbig2_end_of_file_from_json(header, segment_data_object);
    if (type_string == "tables")
        return jbig2_tables_from_json(header, segment_data_object);
    if (type_string == "extension")
        return jbig2_extension_from_json(header, segment_data_object);

    dbgln("segment type {}", type_string);
    return Error::from_string_literal("segment has unknown type");
}

static ErrorOr<Vector<Gfx::JBIG2::SegmentData>> jbig2_segments_from_json(ToJSONOptions const& options, JsonArray const& segments_array)
{
    Vector<Gfx::JBIG2::SegmentData> segments;

    for (auto const& segment_value : segments_array.values()) {
        if (!segment_value.is_object())
            return Error::from_string_literal("segment should be object");
        segments.append(TRY(jbig2_segment_from_json(options, segment_value.as_object())));
    }

    return segments;
}

static ErrorOr<Gfx::JBIG2::FileData> jbig2_data_from_json(ToJSONOptions const& options, JsonValue const& json)
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
        jbig2.segments = TRY(jbig2_segments_from_json(options, segments.value()));
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

    ToJSONOptions options { .input_path = in_path };
    auto jbig2 = TRY(jbig2_data_from_json(options, json));

    AllocatingMemoryStream stream;
    TRY(Gfx::JBIG2Writer::encode_with_explicit_data(stream, jbig2));
    auto jbig2_data = TRY(stream.read_until_eof());

    // Only write images that decode correctly.
    TRY(TRY(Gfx::JBIG2ImageDecoderPlugin::create(jbig2_data))->frame(0));

    auto output_stream = TRY(Core::File::open(out_path, Core::File::OpenMode::Write));
    auto buffered_output = TRY(Core::OutputBufferedFile::create(move(output_stream)));

    TRY(buffered_output->write_until_depleted(jbig2_data));

    return 0;
}
