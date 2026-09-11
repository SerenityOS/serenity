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

static ErrorOr<bool> parse_bool(JsonValue const& value, StringView error)
{
    if (auto b = value.get_bool(); b.has_value())
        return b.value();
    return Error::from_string_view(error);
}

static ErrorOr<u8> parse_bit(JsonValue const& value, StringView error)
{
    return static_cast<u8>(TRY(parse_bool(value, error)));
}

static ErrorOr<i32> parse_i32(JsonValue const& value, StringView error)
{
    if (auto i = value.get_i32(); i.has_value())
        return i.value();
    return Error::from_string_view(error);
}

static ErrorOr<i32> parse_i32_in_range(JsonValue const& value, i32 min, i32 max, StringView error)
{
    i32 i = TRY(parse_i32(value, error));
    if (i < min || i > max)
        return Error::from_string_view(error);
    return i;
}

static ErrorOr<u32> parse_u32(JsonValue const& value, StringView error)
{
    if (auto i = value.get_u32(); i.has_value())
        return i.value();
    return Error::from_string_view(error);
}

static ErrorOr<u32> parse_u32_in_range(JsonValue const& value, u32 min, u32 max, StringView error)
{
    u32 i = TRY(parse_u32(value, error));
    if (i < min || i > max)
        return Error::from_string_view(error);
    return i;
}

static ErrorOr<u32> parse_u32_in_set(JsonValue const& value, Vector<u32>&& values, StringView error)
{
    u32 i = TRY(parse_u32(value, error));
    if (!values.contains_slow(i))
        return Error::from_string_view(error);
    return i;
}

static ErrorOr<JsonArray const*> parse_array(JsonValue const& value, StringView error)
{
    if (value.is_array())
        return &value.as_array();
    return Error::from_string_view(error);
}

static ErrorOr<JsonObject const*> parse_object(JsonValue const& value, StringView error)
{
    if (value.is_object())
        return &value.as_object();
    return Error::from_string_view(error);
}

static ErrorOr<ByteString const*> parse_string(JsonValue const& value, StringView error)
{
    if (value.is_string())
        return &value.as_string();
    return Error::from_string_view(error);
}

static bool is_string_literal(JsonValue const& value, StringView string)
{
    if (!value.is_string())
        return false;
    return value.as_string() == string;
}

template<class T, class V>
static ErrorOr<void> set(T& out, ErrorOr<V>&& in)
{
    out = TRY(in);
    return {};
}

template<class T, class V>
static ErrorOr<void> set_bits(T& out, ErrorOr<V>&& in, u8 shift)
{
    out |= TRY(in) << shift;
    return {};
}

enum class AllowReplace {
    No,
    Yes,
};

static ErrorOr<Gfx::JBIG2::CombinationOperator> parse_jbig2_combination_operator_from_json(JsonValue const& value, AllowReplace allow_replace, StringView error)
{
    if (is_string_literal(value, "or"sv))
        return Gfx::JBIG2::CombinationOperator::Or;
    if (is_string_literal(value, "and"sv))
        return Gfx::JBIG2::CombinationOperator::And;
    if (is_string_literal(value, "xor"sv))
        return Gfx::JBIG2::CombinationOperator::Xor;
    if (is_string_literal(value, "xnor"sv))
        return Gfx::JBIG2::CombinationOperator::XNor;
    if (is_string_literal(value, "replace"sv) && allow_replace == AllowReplace::Yes)
        return Gfx::JBIG2::CombinationOperator::Replace;
    return Error::from_string_view(error);
}

static ErrorOr<u8> parse_jbig2_combination_operator_bits(JsonValue const& value, AllowReplace allow_replace, StringView error)
{
    return to_underlying(TRY(parse_jbig2_combination_operator_from_json(value, allow_replace, error)));
}

static ErrorOr<u8> parse_jbig2_color_from_json(JsonValue const& value, StringView error)
{
    if (is_string_literal(value, "white"sv))
        return 0;
    if (is_string_literal(value, "black"sv))
        return 1;
    return Error::from_string_view(error);
}

static ErrorOr<Gfx::JBIG2::Organization> jbig2_organization_from_json(JsonValue const& value)
{
    if (is_string_literal(value, "sequential"sv))
        return Gfx::JBIG2::Organization::Sequential;
    if (is_string_literal(value, "random_access"sv))
        return Gfx::JBIG2::Organization::RandomAccess;
    return Error::from_string_literal("organization must be \"sequential\" or \"random_access\"");
}

static ErrorOr<Gfx::JBIG2::FileHeaderData> jbig2_header_from_json(JsonObject const& header_object)
{
    Gfx::JBIG2::FileHeaderData header;

    TRY(header_object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "number_of_pages"sv) {
            if (value.is_null()) {
                header.number_of_pages = {};
                return {};
            }
            return set(header.number_of_pages, parse_u32(value, "expected u32 or `null` for \"number_of_pages\""sv));
        }

        if (key == "organization"sv)
            return set(header.organization, jbig2_organization_from_json(value));

        dbgln("global_header key {}", key);
        return Error::from_string_literal("unknown global_header key");
    }));

    return header;
}

static ErrorOr<Vector<i8>> parse_jbig2_adaptive_template_pixels_from_json(JsonValue const& value, StringView error)
{
    Vector<i8> adaptive_template_pixels;
    for (auto const& value : TRY(parse_array(value, error))->values())
        adaptive_template_pixels.append(static_cast<i8>(TRY(parse_i32_in_range(value, -128, 127, error))));
    return adaptive_template_pixels;
}

template<unsigned N>
static ErrorOr<Array<Gfx::JBIG2::AdaptiveTemplatePixel, N>> jbig2_adaptive_template_pixels_to_array(Vector<i8> const& adaptive_template_pixels, size_t number_of_adaptive_template_pixels, StringView error)
{
    VERIFY(number_of_adaptive_template_pixels <= N);
    if (adaptive_template_pixels.size() != number_of_adaptive_template_pixels * 2) {
        dbgln("expected {} entries, got {}", number_of_adaptive_template_pixels * 2, adaptive_template_pixels.size());
        return Error::from_string_view(error);
    }
    Array<Gfx::JBIG2::AdaptiveTemplatePixel, N> template_pixels {};
    for (size_t i = 0; i < number_of_adaptive_template_pixels; ++i) {
        template_pixels[i].x = adaptive_template_pixels[2 * i];
        template_pixels[i].y = adaptive_template_pixels[2 * i + 1];
    }
    return template_pixels;
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

static ErrorOr<Gfx::MQArithmeticEncoder::Trailing7FFFHandling> parse_jbig2_trailing_7fff_handling_from_json(JsonValue const& value)
{
    if (TRY(parse_bool(value, "expected bool for \"strip_trailing_7fffs\""sv)))
        return Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Remove;
    return Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Keep;
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
        if (key == "x"sv)
            return set(rect.x, parse_u32(value, "expected u32 for \"x\""sv));

        if (key == "y"sv)
            return set(rect.y, parse_u32(value, "expected u32 for \"y\""sv));

        if (key == "width"sv)
            return set(rect.width, parse_u32(value, "expected u32 for \"width\""sv));

        if (key == "height"sv)
            return set(rect.height, parse_u32(value, "expected u32 for \"height\""sv));

        dbgln("rect key {}", key);
        return Error::from_string_literal("unknown rect key");
    }));

    return rect;
}

static ErrorOr<NonnullRefPtr<Gfx::Bitmap>> jbig2_load_bitmap(ToJSONOptions const& options, ByteString const& base_name)
{
    ByteString base_directory = LexicalPath { options.input_path }.dirname();
    auto path = LexicalPath::absolute_path(base_directory, base_name);
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
    return TRY(decoder->frame(0)).image.release_nonnull();
}

static ErrorOr<NonnullRefPtr<Gfx::Bitmap>> jbig2_bitmap_from_json(ToJSONOptions const& options, JsonObject const& object)
{
    RefPtr<Gfx::Bitmap> bitmap;
    JSONRect crop_rect;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "from_file")
            return set(bitmap, jbig2_load_bitmap(options, *TRY(parse_string(value, "expected string for \"from_file\""sv))));

        if (key == "crop")
            return set(crop_rect, jbig2_rect_from_json(*TRY(parse_object(value, "expected object for \"crop\""sv))));

        dbgln("match_image key {}", key);
        return Error::from_string_literal("unknown match_image key");
    }));

    if (!bitmap)
        return Error::from_string_literal("no image data in match_image; add \"from_file\" key");

    if (crop_rect.x.has_value() || crop_rect.y.has_value() || crop_rect.width.has_value() || crop_rect.height.has_value()) {
        int crop_x = static_cast<int>(crop_rect.x.value_or(0));
        int crop_y = static_cast<int>(crop_rect.y.value_or(0));
        int crop_width = static_cast<int>(crop_rect.width.value_or(bitmap->width() - crop_x));
        int crop_height = static_cast<int>(crop_rect.height.value_or(bitmap->height() - crop_y));
        if (crop_x + crop_width > bitmap->width() || crop_y + crop_height > bitmap->height())
            return Error::from_string_literal("crop rectangle out of bounds");
        bitmap = TRY(bitmap->cropped({ crop_x, crop_y, crop_width, crop_height }));
    }

    return bitmap.release_nonnull();
}

static ErrorOr<NonnullRefPtr<Gfx::BilevelImage>> jbig2_image_from_json(ToJSONOptions const& options, JsonObject const& object)
{
    RefPtr<Gfx::BilevelImage> image;
    JSONRect crop_rect;
    bool invert = false;
    int repeat_x = 1;
    int repeat_y = 1;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "from_file") {
            auto bitmap = TRY(jbig2_load_bitmap(options, *TRY(parse_string(value, "expected string for \"from_file\""sv))));
            return set(image, Gfx::BilevelImage::create_from_bitmap(*bitmap, Gfx::DitheringAlgorithm::FloydSteinberg));
        }

        if (key == "crop")
            return set(crop_rect, jbig2_rect_from_json(*TRY(parse_object(value, "expected object for \"crop\""sv))));

        if (key == "invert")
            return set(invert, parse_bool(value, "expected bool for \"invert\""sv));

        if (key == "repeat_x")
            return set(repeat_x, parse_i32_in_range(value, 1, NumericLimits<i32>::max(), "expected i32 >= 1 for \"repeat_x\""sv));

        if (key == "repeat_y")
            return set(repeat_y, parse_i32_in_range(value, 1, NumericLimits<i32>::max(), "expected i32 >= 1 for \"repeat_y\""sv));

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

    if (repeat_x > 1 || repeat_y > 1) {
        auto repeated_image = TRY(Gfx::BilevelImage::create(image->width() * repeat_x, image->height() * repeat_y));
        for (u32 y = 0; y < repeated_image->height(); ++y) {
            for (u32 x = 0; x < repeated_image->width(); ++x) {
                repeated_image->set_bit(x, y, image->get_bit(x % image->width(), y % image->height()));
            }
        }
        image = move(repeated_image);
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
        if (key == "external_combination_operator"sv)
            return set_bits(flags, parse_jbig2_combination_operator_bits(value, AllowReplace::Yes, "expected \"or\", \"and\", \"xor\", \"xnor\", or \"replace\" for \"external_combination_operator\""sv), 0);

        dbgln("region_segment_information flag key {}", key);
        return Error::from_string_literal("unknown region_segment_information flag key");
    }));

    return flags;
}

struct RegionSegmentInformationJSON {
    Gfx::JBIG2::RegionSegmentInformationField region_segment_information {};
    bool use_width_from_image { false };
    bool use_height_from_image { false };
};

static ErrorOr<RegionSegmentInformationJSON> jbig2_region_segment_information_from_json(JsonObject const& object)
{
    RegionSegmentInformationJSON result;
    result.use_width_from_image = true;
    result.use_height_from_image = true;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "width"sv) {
            if (is_string_literal(value, "from_image_data"sv)) {
                result.use_width_from_image = true;
                return {};
            }
            result.region_segment_information.width = TRY(parse_u32(value, "expected u32 or \"from_image_data\" for \"width\""sv));
            result.use_width_from_image = false;
            return {};
        }

        if (key == "height"sv) {
            if (is_string_literal(value, "from_image_data"sv)) {
                result.use_height_from_image = true;
                return {};
            }
            result.region_segment_information.height = TRY(parse_u32(value, "expected u32 or \"from_image_data\" for \"height\""sv));
            result.use_height_from_image = false;
            return {};
        }

        if (key == "x"sv)
            return set(result.region_segment_information.x_location, parse_u32(value, "expected u32 for \"x\""sv));

        if (key == "y"sv)
            return set(result.region_segment_information.y_location, parse_u32(value, "expected u32 for \"y\""sv));

        if (key == "flags"sv)
            return set(result.region_segment_information.flags, jbig2_region_segment_information_flags_from_json(*TRY(parse_object(value, "expected object for \"flags\""sv))));

        dbgln("region_segment_information key {}", key);
        return Error::from_string_literal("unknown region_segment_information key");
    }));
    return result;
}

static ErrorOr<u16> jbig2_symbol_dictionary_flags_from_json(JsonObject const& object)
{
    u16 flags = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "uses_huffman_encoding"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"uses_huffman_encoding\""sv), 0);

        if (key == "uses_refinement_or_aggregate_coding"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"uses_refinement_or_aggregate_coding\""sv), 1);

        if (key == "huffman_table_selection_for_height_differences"sv) {
            // FIXME: Also allow names "standard_table_4", "standard_table_5", "custom" for values 0, 1, 3.
            return set_bits(flags, parse_u32_in_set(value, { 0, 1, 3 }, "expected 0, 1, or 3 for \"huffman_table_selection_for_height_differences\""sv), 2);
        }

        if (key == "huffman_table_selection_for_width_differences"sv) {
            // FIXME: Also allow names "standard_table_2", "standard_table_3", "custom" for values 0, 1, 3.
            return set_bits(flags, parse_u32_in_set(value, { 0, 1, 3 }, "expected 0, 1, or 3 for \"huffman_table_selection_for_width_differences\""sv), 4);
        }

        if (key == "huffman_table_selection_for_bitmap_sizes"sv) {
            // FIXME: Also allow names "standard_table_1", "custom" for values 0, 1.
            return set_bits(flags, parse_u32_in_set(value, { 0, 1 }, "expected 0 or 1 for \"huffman_table_selection_for_bitmap_sizes\""sv), 6);
        }

        if (key == "huffman_table_selection_for_number_of_symbol_instances"sv) {
            // FIXME: Also allow names "standard_table_1", "custom" for values 0, 1.
            return set_bits(flags, parse_u32_in_set(value, { 0, 1 }, "expected 0 or 1 for \"huffman_table_selection_for_number_of_symbol_instances\""sv), 7);
        }

        if (key == "is_bitmap_coding_context_used"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"is_bitmap_coding_context_used\""sv), 8);

        if (key == "is_bitmap_coding_context_retained"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"is_bitmap_coding_context_retained\""sv), 9);

        if (key == "template"sv)
            return set_bits(flags, parse_u32_in_range(value, 0, 3, "expected 0, 1, 2, or 3 for \"template\""sv), 10);

        if (key == "refinement_template"sv)
            return set_bits(flags, parse_u32_in_range(value, 0, 1, "expected 0 or 1 for \"refinement_template\""sv), 12);

        dbgln("symbol_dictionary flag key {}", key);
        return Error::from_string_literal("unknown symbol_dictionary flag key");
    }));

    return flags;
}

static ErrorOr<Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass::RefinedSymbol> jbig2_symbol_dictionary_refined_symbol_from_json(ToJSONOptions const& options, JsonObject const& object)
{
    u32 symbol_id = 0;
    i32 delta_x_offset = 0;
    i32 delta_y_offset = 0;
    RefPtr<Gfx::BilevelImage> image;
    Gfx::MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Keep };
    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "symbol_id"sv)
            return set(symbol_id, parse_u32(value, "expected u32 for \"symbol_id\""sv));

        if (key == "delta_x_offset"sv)
            return set(delta_x_offset, parse_i32(value, "expected i32 for \"delta_x_offset\""sv));

        if (key == "delta_y_offset"sv)
            return set(delta_y_offset, parse_i32(value, "expected i32 for \"delta_y_offset\""sv));

        if (key == "image_data"sv)
            return set(image, jbig2_image_from_json(options, *TRY(parse_object(value, "expected object for \"image_data\""sv))));

        if (key == "strip_trailing_7fffs"sv)
            return set(trailing_7fff_handling, parse_jbig2_trailing_7fff_handling_from_json(value));

        dbgln("symbol_dict symbol refines_symbol_to key {}", key);
        return Error::from_string_literal("unknown symbol_dict symbol refines_symbol_to key");
    }));

    if (!image)
        return Error::from_string_literal("\"refines_symbol_to\" missing \"image_data\"");

    return Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass::RefinedSymbol {
        symbol_id,
        delta_x_offset,
        delta_y_offset,
        *image,
        trailing_7fff_handling,
    };
}

static ErrorOr<Vector<Gfx::JBIG2::TextRegionStrip>> jbig2_text_region_strips_from_json(ToJSONOptions const& options, JsonArray const& array);

static ErrorOr<Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass::RefinesUsingStrips> jbig2_symbol_dictionary_refines_using_strips_from_json(ToJSONOptions const& options, JsonObject const& object)
{
    Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass::RefinesUsingStrips refines_using_strips;
    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "initial_strip_t"sv)
            return set(refines_using_strips.initial_strip_t, parse_i32(value, "expected i32 for \"initial_strip_t\""sv));

        if (key == "strips"sv)
            return set(refines_using_strips.strips, jbig2_text_region_strips_from_json(options, *TRY(parse_array(value, "expected array for \"strips\""sv))));

        dbgln("symbol_dict symbol refines_using_strips key {}", key);
        return Error::from_string_literal("unknown symbol_dict symbol refines_using_strips key");
    }));

    return refines_using_strips;
}

static ErrorOr<Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass::Symbol> jbig2_symbol_dictionary_height_class_symbol_from_json(ToJSONOptions const& options, JsonObject const& object)
{
    bool is_exported = true;
    Optional<i32> width;
    Optional<i32> height;
    Optional<Variant<NonnullRefPtr<Gfx::BilevelImage>, Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass::RefinedSymbol, Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass::RefinesUsingStrips>> image;
    Gfx::IntSize size;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "exported"sv)
            return set(is_exported, parse_bool(value, "expected bool for \"exported\""sv));

        if ((key == "image_data"sv || key == "refines_symbol_to"sv) && image.has_value()) {
            return Error::from_string_literal("only one of \"image_data\" or \"refines_symbol_to\" may be specified");
        }

        if (key == "image_data"sv) {
            auto image_json = TRY(jbig2_image_from_json(options, *TRY(parse_object(value, "expected object for \"image_data\""sv))));
            size = { image_json->width(), image_json->height() };
            image = move(image_json);
            return {};
        }

        if (key == "refines_symbol_to"sv) {
            auto refined_symbol = TRY(jbig2_symbol_dictionary_refined_symbol_from_json(options, *TRY(parse_object(value, "expected object for \"refines_symbol_to\""sv))));
            size = { refined_symbol.refines_to->width(), refined_symbol.refines_to->height() };
            image = move(refined_symbol);
            return {};
        }

        if (key == "refines_using_strips"sv) {
            auto refines_using_strips = TRY(jbig2_symbol_dictionary_refines_using_strips_from_json(options, *TRY(parse_object(value, "expected object for \"refines_using_strips\""sv))));
            image = move(refines_using_strips);
            return {};
        }

        if (key == "width"sv)
            return set(width, parse_i32(value, "expected i32 for \"width\""sv));

        if (key == "height"sv)
            return set(height, parse_i32(value, "expected i32 for \"height\""sv));

        dbgln("height_class.symbol key {}", key);
        return Error::from_string_literal("unknown height_class.symbol key");
    }));

    if (!image.has_value())
        return Error::from_string_literal("\"symbol\" missing \"image_data\" or \"refines_symbol_to\"");

    if (width.has_value() != image->has<Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass::RefinesUsingStrips>())
        return Error::from_string_literal("symbol \"width\" should be present exactly for \"refines_using_strips\" entries");
    if (height.has_value() != image->has<Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass::RefinesUsingStrips>())
        return Error::from_string_literal("symbol \"height\" should be present exactly for \"refines_using_strips\" entries");
    if (image->has<Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass::RefinesUsingStrips>())
        size = { width.value(), height.value() };

    return Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass::Symbol {
        .size = size,
        .is_exported = is_exported,
        .image = image.release_value(),
    };
}

static ErrorOr<Vector<Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass::Symbol>> jbig2_symbol_dictionary_height_class_symbols_from_json(ToJSONOptions const& options, JsonArray const& array)
{
    Vector<Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass::Symbol> symbols;

    for (auto const& value : array.values())
        symbols.append(TRY(jbig2_symbol_dictionary_height_class_symbol_from_json(options, *TRY(parse_object(value, "expected object for height class symbol"sv)))));

    return symbols;
}

static ErrorOr<Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass> jbig2_symbol_dictionary_height_class_from_json(ToJSONOptions const& options, JsonObject const& object)
{
    Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass height_class;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "height_class_collective_bitmap_is_compressed"sv)
            return set(height_class.is_collective_bitmap_compressed, parse_bool(value, "expected bool for \"height_class_collective_bitmap_is_compressed\""sv));

        if (key == "symbols"sv)
            return set(height_class.symbols, jbig2_symbol_dictionary_height_class_symbols_from_json(options, *TRY(parse_array(value, "expected array for \"height_class.symbols\""sv))));

        dbgln("height_class key {}", key);
        return Error::from_string_literal("unknown height_class key");
    }));

    return height_class;
}

static ErrorOr<Vector<Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass>> jbig2_symbol_dictionary_height_classes_from_json(ToJSONOptions const& options, JsonArray const& array)
{
    Vector<Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass> height_classes;

    for (auto const& value : array.values())
        height_classes.append(TRY(jbig2_symbol_dictionary_height_class_from_json(options, *TRY(parse_object(value, "expected object for height class"sv)))));

    return height_classes;
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_symbol_dictionary_from_json(ToJSONOptions const& options, Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    if (!object.has_value())
        return Error::from_string_literal("symbol_dictionary segment should have \"data\" object");

    u16 flags = 0;
    Vector<i8> adaptive_template_pixels;
    Vector<i8> refinement_adaptive_template_pixels;
    Vector<bool> export_flags_for_referred_to_symbols;
    Vector<Gfx::JBIG2::SymbolDictionarySegmentData::HeightClass> height_classes;
    Gfx::MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Keep };
    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "flags"sv)
            return set(flags, jbig2_symbol_dictionary_flags_from_json(*TRY(parse_object(value, "expected object for \"flags\""sv))));

        if (key == "adaptive_template_pixels"sv)
            return set(adaptive_template_pixels, parse_jbig2_adaptive_template_pixels_from_json(value, "expected array of i8 for \"adaptive_template_pixels\""sv));

        if (key == "refinement_adaptive_template_pixels"sv)
            return set(refinement_adaptive_template_pixels, parse_jbig2_adaptive_template_pixels_from_json(value, "expected array of i8 for \"refinement_adaptive_template_pixels\""sv));

        if (key == "export_flags_for_referred_to_symbols"sv) {
            for (auto const& flag_value : TRY(parse_array(value, "expected array for \"export_flags_for_referred_to_symbols\""sv))->values())
                export_flags_for_referred_to_symbols.append(TRY(parse_bool(flag_value, "expected bool in array for \"export_flags_for_referred_to_symbols\""sv)));
            return {};
        }

        if (key == "height_classes"sv)
            return set(height_classes, jbig2_symbol_dictionary_height_classes_from_json(options, *TRY(parse_array(value, "expected array for \"height_classes\""sv))));

        if (key == "strip_trailing_7fffs"sv)
            return set(trailing_7fff_handling, parse_jbig2_trailing_7fff_handling_from_json(value));

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
    auto template_pixels = TRY(jbig2_adaptive_template_pixels_to_array<4>(adaptive_template_pixels, number_of_adaptive_template_pixels, "symbol_dictionary \"data\" object has wrong number of \"adaptive_template_pixels\""sv));

    bool uses_refinement_or_aggregate_coding = (flags & 2) != 0;
    u8 symbol_refinement_template = (flags >> 12) & 1;
    if (uses_refinement_or_aggregate_coding && refinement_adaptive_template_pixels.is_empty())
        refinement_adaptive_template_pixels = default_refinement_adaptive_template_pixels(symbol_refinement_template);

    size_t number_of_refinement_adaptive_template_pixels = uses_refinement_or_aggregate_coding && symbol_refinement_template == 0 ? 2 : 0;
    auto refinement_template_pixels = TRY(jbig2_adaptive_template_pixels_to_array<2>(refinement_adaptive_template_pixels, number_of_refinement_adaptive_template_pixels, "symbol_dictionary \"data\" object has wrong number of \"refinement_adaptive_template_pixels\""sv));

    return Gfx::JBIG2::SegmentData {
        header,
        Gfx::JBIG2::SymbolDictionarySegmentData {
            flags,
            template_pixels,
            refinement_template_pixels,
            move(export_flags_for_referred_to_symbols),
            move(height_classes),
            trailing_7fff_handling,
        }
    };
}

static ErrorOr<u16> jbig2_text_region_flags_from_json(JsonObject const& object)
{
    u16 flags = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "uses_huffman_encoding"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"uses_huffman_encoding\""sv), 0);

        if (key == "uses_refinement_coding"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"uses_refinement_coding\""sv), 1);

        if (key == "strip_size"sv) {
            auto strip_size = TRY(parse_u32_in_set(value, { 1, 2, 4, 8 }, "expected 1, 2, 4, or 8 for \"strip_size\""sv));
            flags |= AK::log2(strip_size) << 2;
            return {};
        }

        if (key == "reference_corner"sv) {
            if (is_string_literal(value, "bottom_left"sv))
                flags |= to_underlying(Gfx::JBIG2::ReferenceCorner::BottomLeft) << 4;
            else if (is_string_literal(value, "top_left"sv))
                flags |= to_underlying(Gfx::JBIG2::ReferenceCorner::TopLeft) << 4;
            else if (is_string_literal(value, "bottom_right"sv))
                flags |= to_underlying(Gfx::JBIG2::ReferenceCorner::BottomRight) << 4;
            else if (is_string_literal(value, "top_right"sv))
                flags |= to_underlying(Gfx::JBIG2::ReferenceCorner::TopRight) << 4;
            else
                return Error::from_string_literal("expected \"bottom_left\", \"top_left\", \"bottom_right\", or \"top_right\" for \"reference_corner\"");
            return {};
        }

        if (key == "is_transposed"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"is_transposed\""sv), 6);

        if (key == "combination_operator"sv) {
            // "replace" is only valid in a region segment information's external_combination_operator, not here.
            return set_bits(flags, parse_jbig2_combination_operator_bits(value, AllowReplace::No, "expected \"or\", \"and\", \"xor\", or \"xnor\" for \"combination_operator\""sv), 7);
        }

        if (key == "default_pixel_value"sv)
            return set_bits(flags, parse_jbig2_color_from_json(value, "expected \"white\" or \"black\" for \"default_pixel_value\""sv), 9);

        if (key == "delta_s_offset"sv) {
            auto offset = TRY(parse_i32_in_range(value, -16, 15, "expected value in [-16, 15] for \"delta_s_offset\""sv));
            flags |= (offset & 0x1F) << 10;
            return {};
        }

        if (key == "refinement_template"sv)
            return set_bits(flags, parse_u32_in_range(value, 0, 1, "expected 0 or 1 for \"refinement_template\""sv), 15);

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
            // FIXME: Also allow names "standard_table_6", "standard_table_7", "custom" for values 0, 1, 3.
            return set_bits(flags, parse_u32_in_set(value, { 0, 1, 3 }, "expected 0, 1, or 3 for \"huffman_table_selection_for_first_s\""sv), 0);
        }

        if (key == "huffman_table_selection_for_subsequent_s"sv) {
            // FIXME: Also allow names "standard_table_8", "standard_table_9", "standard_table_10", "custom" for values 0, 1, 2, 3.
            return set_bits(flags, parse_u32_in_set(value, { 0, 1, 2, 3 }, "expected 0, 1, 2, or 3 for \"huffman_table_selection_for_subsequent_s\""sv), 2);
        }

        if (key == "huffman_table_selection_for_t"sv) {
            // FIXME: Also allow names "standard_table_11", "standard_table_12", "standard_table_13", "custom" for values 0, 1, 2, 3.
            return set_bits(flags, parse_u32_in_set(value, { 0, 1, 2, 3 }, "expected 0, 1, 2, or 3 for \"huffman_table_selection_for_t\""sv), 4);
        }

        if (key == "huffman_table_selection_for_refinement_delta_width"sv) {
            // FIXME: Also allow names "standard_table_14", "standard_table_15", "custom" for values 0, 1, 3.
            return set_bits(flags, parse_u32_in_set(value, { 0, 1, 3 }, "expected 0, 1, or 3 for \"huffman_table_selection_for_refinement_delta_width\""sv), 6);
        }

        if (key == "huffman_table_selection_for_refinement_delta_height"sv) {
            // FIXME: Also allow names "standard_table_14", "standard_table_15", "custom" for values 0, 1, 3.
            return set_bits(flags, parse_u32_in_set(value, { 0, 1, 3 }, "expected 0, 1, or 3 for \"huffman_table_selection_for_refinement_delta_height\""sv), 8);
        }

        if (key == "huffman_table_selection_for_refinement_delta_x_offset"sv) {
            // FIXME: Also allow names "standard_table_14", "standard_table_15", "custom" for values 0, 1, 3.
            return set_bits(flags, parse_u32_in_set(value, { 0, 1, 3 }, "expected 0, 1, or 3 for \"huffman_table_selection_for_refinement_delta_x_offset\""sv), 10);
        }

        if (key == "huffman_table_selection_for_refinement_delta_y_offset"sv) {
            // FIXME: Also allow names "standard_table_14", "standard_table_15", "custom" for values 0, 1, 3.
            return set_bits(flags, parse_u32_in_set(value, { 0, 1, 3 }, "expected 0, 1, or 3 for \"huffman_table_selection_for_refinement_delta_y_offset\""sv), 12);
        }

        if (key == "huffman_table_selection_for_refinement_size_table"sv) {
            // FIXME: Also allow names "standard_table_1", "custom" for values 0, 1.
            return set_bits(flags, parse_u32_in_set(value, { 0, 1 }, "expected 0 or 1 for \"huffman_table_selection_for_refinement_size_table\""sv), 14);
        }

        dbgln("text_region huffman_flags key {}", key);
        return Error::from_string_literal("unknown text_region huffman_flags key");
    }));

    return flags;
}

static ErrorOr<Gfx::JBIG2::TextRegionStrip::SymbolInstance::RefinementData> jbig2_text_region_symbol_instance_refinement_data_from_json(ToJSONOptions const& options, JsonObject const& object)
{
    i32 delta_width = 0;
    i32 delta_height = 0;
    i32 delta_x_offset = 0;
    i32 delta_y_offset = 0;
    RefPtr<Gfx::BilevelImage> image;
    Gfx::MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Keep };
    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "delta_width"sv)
            return set(delta_width, parse_i32(value, "expected i32 for \"delta_width\""sv));

        if (key == "delta_height"sv)
            return set(delta_height, parse_i32(value, "expected i32 for \"delta_height\""sv));

        if (key == "delta_x_offset"sv)
            return set(delta_x_offset, parse_i32(value, "expected i32 for \"delta_x_offset\""sv));

        if (key == "delta_y_offset"sv)
            return set(delta_y_offset, parse_i32(value, "expected i32 for \"delta_y_offset\""sv));

        if (key == "image_data"sv)
            return set(image, jbig2_image_from_json(options, *TRY(parse_object(value, "expected object for \"image_data\""sv))));

        if (key == "strip_trailing_7fffs"sv)
            return set(trailing_7fff_handling, parse_jbig2_trailing_7fff_handling_from_json(value));

        dbgln("text_region symbol_instance refinement_data key {}", key);
        return Error::from_string_literal("unknown text_region symbol_instance refinement_data key");
    }));

    if (!image)
        return Error::from_string_literal("\"instance_refines_symbol_to\" missing \"image_data\"");

    return Gfx::JBIG2::TextRegionStrip::SymbolInstance::RefinementData {
        delta_width,
        delta_height,
        delta_x_offset,
        delta_y_offset,
        *image,
        trailing_7fff_handling,
    };
}

static ErrorOr<Gfx::JBIG2::TextRegionStrip::SymbolInstance> jbig2_text_region_symbol_instance_from_json(ToJSONOptions const& options, JsonObject const& object)
{
    Gfx::JBIG2::TextRegionStrip::SymbolInstance instance;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "symbol_id"sv)
            return set(instance.symbol_id, parse_u32(value, "expected u32 for \"symbol_id\""sv));

        if (key == "instance_s"sv)
            return set(instance.s, parse_i32(value, "expected i32 for \"instance_s\""sv));

        if (key == "instance_t"sv)
            return set(instance.t, parse_i32(value, "expected i32 for \"instance_t\""sv));

        if (key == "instance_refines_symbol_to"sv)
            return set(instance.refinement_data, jbig2_text_region_symbol_instance_refinement_data_from_json(options, *TRY(parse_object(value, "expected object for \"instance_refines_symbol_to\""sv))));

        dbgln("text_region symbol_instance key {}", key);
        return Error::from_string_literal("unknown text_region symbol_instance key");
    }));

    return instance;
}

static ErrorOr<Vector<Gfx::JBIG2::TextRegionStrip::SymbolInstance>> jbig2_text_region_instances_from_json(ToJSONOptions const& options, JsonArray const& array)
{
    Vector<Gfx::JBIG2::TextRegionStrip::SymbolInstance> instances;

    for (auto const& item : array.values())
        instances.append(TRY(jbig2_text_region_symbol_instance_from_json(options, *TRY(parse_object(item, "expected object for text_region symbol_instance"sv)))));

    return instances;
}

static ErrorOr<Gfx::JBIG2::TextRegionStrip> jbig2_text_region_strip_from_json(ToJSONOptions const& options, JsonObject const& object)
{
    Gfx::JBIG2::TextRegionStrip strip;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "strip_t"sv)
            return set(strip.strip_t, parse_i32(value, "expected i32 for \"strip_t\""sv));

        if (key == "instances"sv)
            return set(strip.symbol_instances, jbig2_text_region_instances_from_json(options, *TRY(parse_array(value, "expected array for \"instances\""sv))));

        dbgln("text_region strip key {}", key);
        return Error::from_string_literal("unknown text_region strip key");
    }));

    return strip;
}

static ErrorOr<Vector<Gfx::JBIG2::TextRegionStrip>> jbig2_text_region_strips_from_json(ToJSONOptions const& options, JsonArray const& array)
{
    Vector<Gfx::JBIG2::TextRegionStrip> strips;

    for (auto const& item : array.values())
        strips.append(TRY(jbig2_text_region_strip_from_json(options, *TRY(parse_object(item, "expected object for text_region strip"sv)))));

    return strips;
}

static ErrorOr<Gfx::JBIG2::TextRegionSegmentData> jbig2_text_region_from_json(ToJSONOptions const& options, Optional<JsonObject const&> object)
{
    if (!object.has_value())
        return Error::from_string_literal("text_region segment should have \"data\" object");

    Vector<i8> refinement_adaptive_template_pixels;
    Gfx::JBIG2::TextRegionSegmentData text_region;

    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "region_segment_information"sv) {
            auto region_segment_information = TRY(jbig2_region_segment_information_from_json(*TRY(parse_object(value, "expected object for \"region_segment_information\""sv))));
            if (region_segment_information.use_width_from_image || region_segment_information.use_height_from_image)
                return Error::from_string_literal("can't use \"from_image\" with text_region");
            text_region.region_segment_information = region_segment_information.region_segment_information;
            return {};
        }

        if (key == "flags"sv)
            return set(text_region.flags, jbig2_text_region_flags_from_json(*TRY(parse_object(value, "expected object for \"flags\""sv))));

        if (key == "huffman_flags"sv)
            return set(text_region.huffman_flags, jbig2_text_region_huffman_flags_from_json(*TRY(parse_object(value, "expected object for \"huffman_flags\""sv))));

        if (key == "refinement_adaptive_template_pixels"sv)
            return set(refinement_adaptive_template_pixels, parse_jbig2_adaptive_template_pixels_from_json(value, "expected array of i8 for \"refinement_adaptive_template_pixels\""sv));

        if (key == "initial_strip_t"sv)
            return set(text_region.initial_strip_t, parse_i32(value, "expected i32 for \"initial_strip_t\""sv));

        if (key == "strips"sv)
            return set(text_region.strips, jbig2_text_region_strips_from_json(options, *TRY(parse_array(value, "expected array for \"strips\""sv))));

        if (key == "strip_trailing_7fffs"sv)
            return set(text_region.trailing_7fff_handling, parse_jbig2_trailing_7fff_handling_from_json(value));

        dbgln("text_region key {}", key);
        return Error::from_string_literal("unknown text_region key");
    }));

    bool uses_refinement_coding = (text_region.flags & 2) != 0;
    u8 refinement_template = (text_region.flags >> 15);
    if (uses_refinement_coding && refinement_adaptive_template_pixels.is_empty())
        refinement_adaptive_template_pixels = default_refinement_adaptive_template_pixels(refinement_template);

    size_t number_of_refinement_adaptive_template_pixels = uses_refinement_coding && refinement_template == 0 ? 2 : 0;
    text_region.refinement_adaptive_template_pixels = TRY(jbig2_adaptive_template_pixels_to_array<2>(refinement_adaptive_template_pixels, number_of_refinement_adaptive_template_pixels, "text_region \"data\" object has wrong number of \"refinement_adaptive_template_pixels\""sv));

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

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_intermediate_text_region_from_json(ToJSONOptions const& options, Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::IntermediateTextRegionSegmentData { TRY(jbig2_text_region_from_json(options, object)) } };
}

static ErrorOr<u8> jbig2_pattern_dictionary_flags_from_json(JsonObject const& object)
{
    u8 flags = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "is_modified_modified_read"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"is_modified_modified_read\""sv), 0);

        if (key == "pd_template"sv)
            return set_bits(flags, parse_u32_in_range(value, 0, 3, "expected 0, 1, 2, or 3 for \"pd_template\""sv), 1);

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
    u32 grayscale_width { 0 };
    u32 grayscale_height { 0 };
    i32 grid_offset_x_times_256 { 0 };
    i32 grid_offset_y_times_256 { 0 };
    u16 grid_vector_x_times_256 { 0 };
    u16 grid_vector_y_times_256 { 0 };

    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "flags"sv)
            return set(flags, jbig2_pattern_dictionary_flags_from_json(*TRY(parse_object(value, "expected object for \"flags\""sv))));

        if (key == "pattern_width"sv)
            return set(pattern_width, parse_u32_in_range(value, 1, 255, "expected non-zero u8 for \"pattern\""sv));

        if (key == "pattern_height"sv)
            return set(pattern_height, parse_u32_in_range(value, 1, 255, "expected non-zero u8 for \"pattern_height\""sv));

        if (key == "gray_max"sv) {
            if (is_string_literal(value, "from_tiles"sv)) {
                gray_max_from_tiles = true;
                return {};
            }
            return set(gray_max, parse_u32(value, "expected u32 or \"from_tiles\" for \"gray_max\""sv));
        }

        if (key == "strip_trailing_7fffs"sv)
            return set(trailing_7fff_handling, parse_jbig2_trailing_7fff_handling_from_json(value));

        if (key == "grayscale_width"sv)
            return set(grayscale_width, parse_u32(value, "expected u32 for \"grayscale_width\""sv));

        if (key == "grayscale_height"sv)
            return set(grayscale_height, parse_u32(value, "expected u32 for \"grayscale_height\""sv));

        if (key == "grid_offset_x_times_256"sv)
            return set(grid_offset_x_times_256, parse_i32(value, "expected i32 for \"grid_offset_x_times_256\""sv));

        if (key == "grid_offset_y_times_256"sv)
            return set(grid_offset_y_times_256, parse_i32(value, "expected i32 for \"grid_offset_y_times_256\""sv));

        if (key == "grid_vector_x_times_256"sv)
            return set(grid_vector_x_times_256, parse_u32_in_range(value, 0, 0xffff, "expected u16 for \"grid_vector_x_times_256\""sv));

        if (key == "grid_vector_y_times_256"sv)
            return set(grid_vector_y_times_256, parse_u32_in_range(value, 0, 0xffff, "expected u16 for \"grid_vector_y_times_256\""sv));

        // FIXME: Make this more flexible.
        if (key == "image_data"sv) {
            if (value.is_object()) {
                image = TRY(jbig2_image_from_json(options, value.as_object()));
                return {};
            }
            if (value.is_array()) {
                size_t width = 0;
                for (auto const& [i, image_json] : enumerate(value.as_array().values())) {
                    auto tile_image = TRY(jbig2_image_from_json(options, *TRY(parse_object(image_json, "expected object for \"image_data\" array entries"sv))));

                    if (i == 0) {
                        width = tile_image->width();
                        image = TRY(Gfx::BilevelImage::create(width * value.as_array().size(), tile_image->height()));
                    }
                    if (tile_image->width() != width || tile_image->height() != image->height())
                        return Error::from_string_literal("all images in \"image_data\" array must have the same dimensions");
                    Gfx::IntPoint destination_position { static_cast<int>(i * width), 0 };
                    tile_image->composite_onto(*image, destination_position, Gfx::BilevelImage::CompositionType::Replace);
                }
                return {};
            }
            return Error::from_string_literal("expected object or array for \"image_data\"");
        }

        if (key == "method"sv) {
            if (is_string_literal(value, "distinct_image_tiles"sv)) {
                method = Method::DistinctImageTiles;
                return {};
            }
            if (is_string_literal(value, "unique_image_tiles"sv)) {
                method = Method::UniqueImageTiles;
                return {};
            }
            return Error::from_string_literal("expected \"distinct_image_tiles\" or \"unique_image_tiles\" for \"method\"");
        }

        dbgln("pattern_dictionary key {}", key);
        return Error::from_string_literal("unknown pattern_dictionary key");
    }));

    if (gray_max_from_tiles && method == Method::None)
        return Error::from_string_literal("can't use \"from_tiles\" for gray_max without using a tiling method");

    if (!image)
        return Error::from_string_literal("pattern_dictionary \"data\" object missing \"image_data\"");

    if (method == Method::DistinctImageTiles || method == Method::UniqueImageTiles) {
        if (grid_vector_x_times_256 == 0 && grid_vector_y_times_256 == 0) {
            if (grayscale_width == 0 && grayscale_height == 0) {
                grayscale_width = ceil_div(image->width(), static_cast<size_t>(pattern_width));
                grayscale_height = ceil_div(image->height(), static_cast<size_t>(pattern_height));
            }
            grid_vector_x_times_256 = pattern_width * 256;
        }

        if (grayscale_width == 0 || grayscale_height == 0)
            return Error::from_string_literal("grayscale_width and grayscale_height must be set when using custom grid");

        // FIXME: For UniqueImageTiles at the edge, we could use a custom hasher/comparator to match existing full tiles
        //        by ignoring pixels outside the clipped tile rect.
        Vector<Gfx::BilevelSubImage> tiles;
        HashTable<Gfx::BilevelSubImage> saw_tile;
        Gfx::IntRect bitmap_rect { 0, 0, static_cast<int>(image->width()), static_cast<int>(image->height()) };
        for (int tile_y = 0; tile_y < (int)grayscale_height; ++tile_y) {
            for (int tile_x = 0; tile_x < (int)grayscale_width; ++tile_x) {
                auto x = (grid_offset_x_times_256 + tile_y * grid_vector_y_times_256 + tile_x * grid_vector_x_times_256) >> 8;
                auto y = (grid_offset_y_times_256 + tile_y * grid_vector_x_times_256 - tile_x * grid_vector_y_times_256) >> 8;

                Gfx::IntPoint source_position { x, y };
                Gfx::IntRect source_rect { source_position, { pattern_width, pattern_height } };
                source_rect = source_rect.intersected(bitmap_rect);
                if (source_rect.is_empty())
                    continue;
                auto source = image->subbitmap(source_rect);
                if (method == Method::DistinctImageTiles || saw_tile.set(source) == HashSetResult::InsertedNewEntry)
                    TRY(tiles.try_append(source));
            }
        }

        auto tiled_image = TRY(Gfx::BilevelImage::create(pattern_width * tiles.size(), pattern_height));
        tiled_image->fill(false);
        for (auto const& [i, tile] : enumerate(tiles)) {
            // FIXME: The destination_position is wrong for tiles clipped at the left or top edge.
            //        We should remember the original source_position shift after intersection with bitmap_rect,
            //        and add that offset here.
            Gfx::IntPoint destination_position { static_cast<int>(i * pattern_width), 0 };
            tile.composite_onto(*tiled_image, destination_position, Gfx::BilevelImage::CompositionType::Replace);
        }

        if (gray_max_from_tiles)
            gray_max = tiles.size() - 1;

        image = move(tiled_image);
    } else if (grayscale_width != 0 || grayscale_height != 0
        || grid_offset_x_times_256 != 0 || grid_offset_y_times_256 != 0
        || grid_vector_x_times_256 != 0 || grid_vector_y_times_256 != 0) {
        return Error::from_string_literal("grid parameters ignored when \"method\" is not set to a tiling method");
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
        if (key == "is_modified_modified_read"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"is_modified_modified_read\""sv), 0);

        if (key == "ht_template"sv)
            return set_bits(flags, parse_u32_in_range(value, 0, 3, "expected 0, 1, 2, or 3 for \"ht_template\""sv), 1);

        if (key == "enable_skip"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"enable_skip\""sv), 3);

        if (key == "combination_operator"sv)
            return set_bits(flags, parse_jbig2_combination_operator_bits(value, AllowReplace::Yes, "expected \"or\", \"and\", \"xor\", \"xnor\", or \"replace\" for \"combination_operator\""sv), 4);

        if (key == "default_pixel_value"sv)
            return set_bits(flags, parse_jbig2_color_from_json(value, "expected \"white\" or \"black\" for \"default_pixel_value\""sv), 7);

        dbgln("halftone_region flag key {}", key);
        return Error::from_string_literal("unknown halftone_region flag key");
    }));

    bool uses_mmr = flags & 1;
    u8 ht_template = (flags >> 1) & 3;
    if (uses_mmr && ht_template != 0)
        return Error::from_string_literal("if is_modified_modified_read is true, ht_template must be 0");

    return flags;
}

static ErrorOr<Variant<Vector<u64>, NonnullRefPtr<Gfx::Bitmap>>> jbig2_halftone_graymap_from_json(ToJSONOptions const& options, JsonObject const& object)
{
    Optional<Variant<Vector<u64>, NonnullRefPtr<Gfx::Bitmap>>> graymap;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "array") {
            Vector<u64> graymap_data;
            for (auto const& row : TRY(parse_array(value, "expected array for \"array\""sv))->values()) {
                for (auto const& element : TRY(parse_array(row, "expected array for \"array\" entries"sv))->values()) {
                    if (auto value = element.get_u64(); value.has_value()) {
                        TRY(graymap_data.try_append(value.value()));
                        continue;
                    }
                    return Error::from_string_literal("expected u64 for \"graymap_data\" elements");
                }
            }
            graymap = move(graymap_data);
            return {};
        }

        if (key == "match_image") {
            if (value.is_object())
                return set(graymap, jbig2_bitmap_from_json(options, value.as_object()));
            if (value.is_string())
                return set(graymap, jbig2_load_bitmap(options, value.as_string()));
            return Error::from_string_literal("expected string or object for \"match_image\"");
        }

        dbgln("graymap_data key {}", key);
        return Error::from_string_literal("unknown graymap_data key");
    }));

    if (!graymap.has_value())
        return Error::from_string_literal("graymap_data object must have \"array\" or \"match_image\" member");

    return graymap.release_value();
}

static ErrorOr<Gfx::JBIG2::HalftoneRegionSegmentData> jbig2_halftone_region_from_json(ToJSONOptions const& options, Optional<JsonObject const&> object)
{
    if (!object.has_value())
        return Error::from_string_literal("halftone_region segment should have \"data\" object");

    Gfx::JBIG2::RegionSegmentInformationField region_segment_information;
    u8 flags { 0 };
    u32 grayscale_width { 0 };
    u32 grayscale_height { 0 };
    i32 grid_offset_x_times_256 { 0 };
    i32 grid_offset_y_times_256 { 0 };
    u16 grid_vector_x_times_256 { 0 };
    u16 grid_vector_y_times_256 { 0 };
    Optional<Variant<Vector<u64>, NonnullRefPtr<Gfx::Bitmap>>> grayscale_image;
    Gfx::MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Keep };

    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "region_segment_information"sv) {
            auto region_segment_information_json = TRY(jbig2_region_segment_information_from_json(*TRY(parse_object(value, "expected object for \"region_segment_information\""sv))));
            if (region_segment_information_json.use_width_from_image || region_segment_information_json.use_height_from_image)
                return Error::from_string_literal("can't use \"from_image\" with halftone_region");
            region_segment_information = region_segment_information_json.region_segment_information;
            return {};
        }

        if (key == "flags"sv)
            return set(flags, jbig2_halftone_region_flags_from_json(*TRY(parse_object(value, "expected object for \"flags\""sv))));

        if (key == "grayscale_width"sv)
            return set(grayscale_width, parse_u32(value, "expected u32 for \"grayscale_width\""sv));

        if (key == "grayscale_height"sv)
            return set(grayscale_height, parse_u32(value, "expected u32 for \"grayscale_height\""sv));

        if (key == "grid_offset_x_times_256"sv)
            return set(grid_offset_x_times_256, parse_i32(value, "expected i32 for \"grid_offset_x_times_256\""sv));

        if (key == "grid_offset_y_times_256"sv)
            return set(grid_offset_y_times_256, parse_i32(value, "expected i32 for \"grid_offset_y_times_256\""sv));

        if (key == "grid_vector_x_times_256"sv)
            return set(grid_vector_x_times_256, parse_u32_in_range(value, 0, 0xffff, "expected u16 for \"grid_vector_x_times_256\""sv));

        if (key == "grid_vector_y_times_256"sv)
            return set(grid_vector_y_times_256, parse_u32_in_range(value, 0, 0xffff, "expected u16 for \"grid_vector_y_times_256\""sv));

        if (key == "strip_trailing_7fffs"sv)
            return set(trailing_7fff_handling, parse_jbig2_trailing_7fff_handling_from_json(value));

        if (key == "graymap_data"sv) {
            if (value.is_object())
                return set(grayscale_image, jbig2_halftone_graymap_from_json(options, value.as_object()));
            if (is_string_literal(value, "identity_tile_indices"sv)) {
                Vector<u64> graymap;
                for (u32 i = 0; i < grayscale_width * grayscale_height; ++i)
                    TRY(graymap.try_append(i));
                grayscale_image = move(graymap);
                return {};
            }
            return Error::from_string_literal("expected object or \"identity_tile_indices\" for \"graymap_data\"");
        }

        dbgln("halftone_region key {}", key);
        return Error::from_string_literal("unknown halftone_region key");
    }));

    if (!grayscale_image.has_value())
        return Error::from_string_literal("halftone_region \"data\" object missing \"graymap_data\"");

    return Gfx::JBIG2::HalftoneRegionSegmentData {
        region_segment_information,
        flags,
        grayscale_width,
        grayscale_height,
        grid_offset_x_times_256,
        grid_offset_y_times_256,
        grid_vector_x_times_256,
        grid_vector_y_times_256,
        grayscale_image.release_value(),
        trailing_7fff_handling,
    };
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

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_intermediate_halftone_region_from_json(ToJSONOptions const& options, Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::IntermediateHalftoneRegionSegmentData { TRY(jbig2_halftone_region_from_json(options, object)) } };
}

static ErrorOr<u8> jbig2_generic_region_flags_from_json(JsonObject const& object)
{
    u8 flags = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "is_modified_modified_read"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"is_modified_modified_read\""sv), 0);

        if (key == "gb_template"sv)
            return set_bits(flags, parse_u32_in_range(value, 0, 3, "expected 0, 1, 2, or 3 for \"gb_template\""sv), 1);

        if (key == "use_typical_prediction"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"use_typical_prediction\""sv), 3);

        if (key == "use_extended_template"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"use_extended_template\""sv), 4);

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

    RegionSegmentInformationJSON region_segment_information;
    region_segment_information.use_width_from_image = true;
    region_segment_information.use_height_from_image = true;
    Optional<u32> real_height_for_generic_region_of_initially_unknown_size;
    u8 flags = 0;
    Vector<i8> adaptive_template_pixels;
    Gfx::MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Keep };
    RefPtr<Gfx::BilevelImage> image;
    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "region_segment_information"sv)
            return set(region_segment_information, jbig2_region_segment_information_from_json(*TRY(parse_object(value, "expected object for \"region_segment_information\""sv))));

        if (key == "real_height_for_generic_region_of_initially_unknown_size"sv)
            return set(real_height_for_generic_region_of_initially_unknown_size, parse_u32(value, "expected u32 for \"real_height_for_generic_region_of_initially_unknown_size\""sv));

        if (key == "flags"sv)
            return set(flags, jbig2_generic_region_flags_from_json(*TRY(parse_object(value, "expected object for \"flags\""sv))));

        if (key == "adaptive_template_pixels"sv)
            return set(adaptive_template_pixels, parse_jbig2_adaptive_template_pixels_from_json(value, "expected array of i8 for \"adaptive_template_pixels\""sv));

        if (key == "strip_trailing_7fffs"sv)
            return set(trailing_7fff_handling, parse_jbig2_trailing_7fff_handling_from_json(value));

        if (key == "image_data"sv)
            return set(image, jbig2_image_from_json(options, *TRY(parse_object(value, "expected object for \"image_data\""sv))));

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
    auto template_pixels = TRY(jbig2_adaptive_template_pixels_to_array<12>(adaptive_template_pixels, number_of_adaptive_template_pixels, "generic_region \"data\" object has wrong number of \"adaptive_template_pixels\""sv));

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
        if (key == "gr_template"sv)
            return set_bits(flags, parse_u32_in_range(value, 0, 1, "expected 0 or 1 for \"gr_template\""sv), 0);

        if (key == "use_typical_prediction"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"use_typical_prediction\""sv), 1);

        dbgln("generic_refinement_region flag key {}", key);
        return Error::from_string_literal("unknown generic_refinement_region flag key");
    }));

    return flags;
}

static ErrorOr<Gfx::JBIG2::GenericRefinementRegionSegmentData> jbig2_generic_refinement_region_from_json(ToJSONOptions const& options, Optional<JsonObject const&> object)
{
    if (!object.has_value())
        return Error::from_string_literal("generic_refinement_region segment should have \"data\" object");

    RegionSegmentInformationJSON region_segment_information;
    region_segment_information.use_width_from_image = true;
    region_segment_information.use_height_from_image = true;
    u8 flags = 0;
    Vector<i8> adaptive_template_pixels;
    Gfx::MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Keep };
    RefPtr<Gfx::BilevelImage> image;
    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "region_segment_information"sv)
            return set(region_segment_information, jbig2_region_segment_information_from_json(*TRY(parse_object(value, "expected object for \"region_segment_information\""sv))));

        if (key == "flags"sv)
            return set(flags, jbig2_refinement_region_flags_from_json(*TRY(parse_object(value, "expected object for \"flags\""sv))));

        if (key == "adaptive_template_pixels"sv)
            return set(adaptive_template_pixels, parse_jbig2_adaptive_template_pixels_from_json(value, "expected array of i8 for \"adaptive_template_pixels\""sv));

        if (key == "strip_trailing_7fffs"sv)
            return set(trailing_7fff_handling, parse_jbig2_trailing_7fff_handling_from_json(value));

        if (key == "image_data"sv)
            return set(image, jbig2_image_from_json(options, *TRY(parse_object(value, "expected object for \"image_data\""sv))));

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
    auto template_pixels = TRY(jbig2_adaptive_template_pixels_to_array<2>(adaptive_template_pixels, number_of_adaptive_template_pixels, "generic_refinement_region \"data\" object has wrong number of \"adaptive_template_pixels\""sv));

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
        if (key == "is_eventually_lossless"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"is_eventually_lossless\""sv), 0);

        if (key == "might_contain_refinements"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"might_contain_refinements\""sv), 1);

        if (key == "default_color"sv)
            return set_bits(flags, parse_jbig2_color_from_json(value, "expected \"white\" or \"black\" for \"default_color\""sv), 2);

        if (key == "default_combination_operator"sv) {
            // "replace" is only valid in a region segment information's external_combination_operator, not here.
            return set_bits(flags, parse_jbig2_combination_operator_bits(value, AllowReplace::No, "expected \"or\", \"and\", \"xor\", or \"xnor\" for \"default_combination_operator\""sv), 3);
        }

        if (key == "requires_auxiliary_buffers"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"requires_auxiliary_buffers\""sv), 5);

        if (key == "direct_region_segments_override_default_combination_operator"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"direct_region_segments_override_default_combination_operator\""sv), 6);

        if (key == "might_contain_coloured_segments"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"might_contain_coloured_segments\""sv), 7);

        dbgln("page_information flag key {}", key);
        return Error::from_string_literal("unknown page_information flag key");
    }));

    return flags;
}

static ErrorOr<u16> jbig2_page_information_striping_information_from_json(JsonObject const& object)
{
    u16 striping_information = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "is_striped"sv)
            return set_bits(striping_information, parse_bit(value, "expected bool for \"is_striped\""sv), 15);

        if (key == "maximum_stripe_size"sv)
            return set_bits(striping_information, parse_u32_in_range(value, 0, 0x7FFF, "maximum_stripe_size should be <= 32767"sv), 0);

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
        if (key == "page_width"sv)
            return set(data.bitmap_width, parse_u32(value, "expected u32 for \"page_width\""sv));

        if (key == "page_height"sv) {
            if (value.is_null()) {
                data.bitmap_height = 0xffff'ffff;
                return {};
            }
            return set(data.bitmap_height, parse_u32(value, "expected u32 or null for \"page_height\""sv));
        }

        if (key == "page_x_resolution"sv)
            return set(data.page_x_resolution, parse_u32(value, "expected u32 for \"page_x_resolution\""sv));

        if (key == "page_y_resolution"sv)
            return set(data.page_y_resolution, parse_u32(value, "expected u32 for \"page_y_resolution\""sv));

        if (key == "flags"sv)
            return set(data.flags, jbig2_page_information_flags_from_json(*TRY(parse_object(value, "expected object for \"flags\""sv))));

        if (key == "striping_information"sv)
            return set(data.striping_information, jbig2_page_information_striping_information_from_json(*TRY(parse_object(value, "expected object for \"striping_information\""sv))));

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
        if (key == "y_coordinate"sv)
            return set(y_coordinate, parse_u32(value, "expected u32 for \"y_coordinate\""sv));

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
        if (key == "has_out_of_band_symbol"sv)
            return set_bits(flags, parse_bit(value, "expected bool for \"has_out_of_band_symbol\""sv), 0);

        if (key == "prefix_bit_count"sv) {
            flags |= (TRY(parse_u32_in_range(value, 1, 8, "expected 1..8 for \"prefix_bit_count\""sv)) - 1) << 1;
            return {};
        }

        if (key == "range_bit_count"sv) {
            flags |= (TRY(parse_u32_in_range(value, 1, 8, "expected 1..8 for \"range_bit_count\""sv)) - 1) << 4;
            return {};
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
        Gfx::JBIG2::TablesData::Entry entry;

        auto const& table_entry_object = *TRY(parse_object(value, "tables entries should be objects"sv));
        TRY(table_entry_object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
            if (key == "prefix_length"sv)
                return set(entry.prefix_length, parse_u32(value, "expected u32 for \"prefix_length\""sv));

            if (key == "range_length"sv)
                return set(entry.range_length, parse_u32(value, "expected u32 for \"range_length\""sv));

            dbgln("tables entry key {}", key);
            return Error::from_string_literal("unknown tables entry key");
        }));

        entries.append(entry);
    }

    return entries;
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_tables_from_json(Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    if (!object.has_value())
        return Error::from_string_literal("tables segment should have \"data\" object");

    Gfx::JBIG2::TablesData data {};

    TRY(object->try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "flags"sv)
            return set(data.flags, jbig2_tables_flags_from_json(*TRY(parse_object(value, "expected object for \"flags\""sv))));

        if (key == "lowest_value"sv)
            return set(data.lowest_value, parse_i32(value, "expected i32 for \"lowest_value\""sv));

        if (key == "highest_value"sv)
            return set(data.highest_value, parse_i32(value, "expected i32 for \"highest_value\""sv));

        if (key == "entries"sv)
            return set(data.entries, jbig2_tables_entries_from_json(*TRY(parse_array(value, "expected array for \"entries\""sv))));

        if (key == "lower_range_prefix_length"sv)
            return set(data.lower_range_prefix_length, parse_u32_in_range(value, 0, 255, "expected u8 for \"lower_range_prefix_length\""sv));

        if (key == "upper_range_prefix_length"sv)
            return set(data.upper_range_prefix_length, parse_u32_in_range(value, 0, 255, "expected u8 for \"upper_range_prefix_length\""sv));

        if (key == "out_of_band_prefix_length"sv)
            return set(data.out_of_band_prefix_length, parse_u32_in_range(value, 0, 255, "expected u8 for \"out_of_band_prefix_length\""sv));

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
            if (is_string_literal(value, "single_byte_coded_comment"sv)) {
                data.type = Gfx::JBIG2::ExtensionType::SingleByteCodedComment;
                return {};
            }
            if (is_string_literal(value, "multi_byte_coded_comment"sv)) {
                data.type = Gfx::JBIG2::ExtensionType::MultiByteCodedComment;
                return {};
            }
            return Error::from_string_literal("expected \"single_byte_coded_comment\" or \"multi_byte_coded_comment\" for \"type\"");
        }

        if (key == "entries"sv) {
            for (auto const& entry : TRY(parse_array(value, "expected array for \"entries\""sv))->values()) {
                auto const& entry_array = *TRY(parse_array(entry, "expected array for \"entries\" elements"sv));
                if (entry_array.values().size() != 2)
                    return Error::from_string_literal("expected 2 elements in \"entries\" elements");
                TRY(data.entries.try_append({
                    TRY(String::from_byte_string(*TRY(parse_string(entry_array.values()[0], "expected string for \"entries\" element 0"sv)))),
                    TRY(String::from_byte_string(*TRY(parse_string(entry_array.values()[1], "expected string for \"entries\" element 1"sv)))),
                }));
            }
            return {};
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
            has_retention_flag = true;
            return set(reference.retention_flag, parse_bool(value, "expected bool for \"retained\""sv));
        }

        if (key == "segment_number"sv) {
            has_segment_number = true;
            return set(reference.segment_number, parse_u32(value, "expected u32 for \"segment_number\""sv));
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

    for (auto const& value : array.values())
        TRY(referred_to_segments.try_append(TRY(jbig2_referred_to_segment_from_json(*TRY(parse_object(value, "referred_to_segments elements should be objects"sv))))));

    return referred_to_segments;
}

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_segment_from_json(ToJSONOptions const& options, JsonObject const& segment_object)
{
    Gfx::JBIG2::SegmentHeaderData header;

    Optional<ByteString> type_string;
    Optional<JsonObject const&> segment_data_object;

    TRY(segment_object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "segment_number"sv)
            return set(header.segment_number, parse_u32(value, "expected u32 for \"segment_number\""sv));

        if (key == "type"sv) {
            type_string = *TRY(parse_string(value, "expected string for \"type\""sv));
            return {};
        }

        if (key == "force_32_bit_page_association"sv)
            return set(header.force_32_bit_page_association, parse_bool(value, "expected bool for \"force_32_bit_page_association\""sv));

        if (key == "is_immediate_generic_region_of_initially_unknown_size"sv)
            return set(header.is_immediate_generic_region_of_initially_unknown_size, parse_bool(value, "expected bool for \"is_immediate_generic_region_of_initially_unknown_size\""sv));

        if (key == "page_association"sv)
            return set(header.page_association, parse_u32(value, "expected u32 for \"page_association\""sv));

        if (key == "referred_to_segments"sv)
            return set(header.referred_to_segments, jbig2_referred_to_segments_from_json(*TRY(parse_array(value, "expected array for \"referred_to_segments\""sv))));

        if (key == "retained"sv)
            return set(header.retention_flag, parse_bool(value, "expected bool for \"retained\""sv));

        if (key == "data"sv) {
            segment_data_object = *TRY(parse_object(value, "expected object for \"data\""sv));
            return {};
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
    if (type_string == "intermediate_text_region")
        return jbig2_intermediate_text_region_from_json(options, header, segment_data_object);
    if (type_string == "pattern_dictionary")
        return jbig2_pattern_dictionary_from_json(options, header, segment_data_object);
    if (type_string == "halftone_region")
        return jbig2_immediate_halftone_region_from_json(options, header, segment_data_object);
    if (type_string == "lossless_halftone_region")
        return jbig2_immediate_lossless_halftone_region_from_json(options, header, segment_data_object);
    if (type_string == "intermediate_halftone_region")
        return jbig2_intermediate_halftone_region_from_json(options, header, segment_data_object);
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

    for (auto const& segment_value : segments_array.values())
        segments.append(TRY(jbig2_segment_from_json(options, *TRY(parse_object(segment_value, "segment should be object"sv)))));

    return segments;
}

static ErrorOr<Gfx::JBIG2::FileData> jbig2_data_from_json(ToJSONOptions const& options, JsonValue const& json)
{
    Gfx::JBIG2::FileData jbig2;

    auto const& object = *TRY(parse_object(json, "top-level should be object"sv));

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
    args_parser.add_positional_argument(in_path, "Path to input json file", "FILE");
    args_parser.add_option(out_path, "Path to output jbig2 file", "output", 'o', "FILE");
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
    Gfx::JBIG2DecoderOptions decoder_options;
    decoder_options.log_comments = Gfx::JBIG2DecoderOptions::LogComments::No;
    decoder_options.strictness = Gfx::JBIG2DecoderOptions::Strictness::SpecCompliant;
    TRY(TRY(Gfx::JBIG2ImageDecoderPlugin::create_with_options(jbig2_data, decoder_options))->frame(0));

    auto output_stream = TRY(Core::File::open(out_path, Core::File::OpenMode::Write));
    auto buffered_output = TRY(Core::OutputBufferedFile::create(move(output_stream)));

    TRY(buffered_output->write_until_depleted(jbig2_data));

    return 0;
}
