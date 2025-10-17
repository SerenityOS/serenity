/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

static ErrorOr<Gfx::JBIG2::SegmentData> jbig2_end_of_file_from_json(Gfx::JBIG2::SegmentHeaderData const& header, Optional<JsonObject const&> object)
{
    if (object.has_value())
        return Error::from_string_literal("end_of_file segment should have no \"data\" object");
    return Gfx::JBIG2::SegmentData { header, Gfx::JBIG2::EndOfFileSegmentData {} };
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

static ErrorOr<u8> jbig2_pattern_dictionary_flags_from_json(JsonObject const& object)
{
    u8 flags = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "is_modified_read_read"sv) {
            if (auto is_modified_read_read = value.get_bool(); is_modified_read_read.has_value()) {
                if (is_modified_read_read.value())
                    flags |= 1u;
                return {};
            }
            return Error::from_string_literal("expected bool for \"is_modified_read_read\"");
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
    Gfx::MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Keep };
    RefPtr<Gfx::BilevelImage> image;

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
            return Error::from_string_literal("expected u32 for \"gray_max_\"");
        }

        if (key == "strip_trailing_7fffs"sv) {
            if (auto strip_trailing_7fffs = value.get_bool(); strip_trailing_7fffs.has_value()) {
                if (strip_trailing_7fffs.value())
                    trailing_7fff_handling = Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Remove;
                else
                    trailing_7fff_handling = Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Keep;
                return {};
            }
            return Error::from_string_literal("expected bool for \"strip_trailing_7fffs\"");
        }

        // FIXME: Make this more flexible.
        if (key == "image_data"sv) {
            if (value.is_object()) {
                image = TRY(jbig2_image_from_json(options, value.as_object()));
                return {};
            }
            return Error::from_string_literal("expected object for \"image_data\"");
        }

        dbgln("pattern_dictionary key {}", key);
        return Error::from_string_literal("unknown pattern_dictionary key");
    }));

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

static ErrorOr<u8> jbig2_generic_region_flags_from_json(JsonObject const& object)
{
    u8 flags = 0;

    TRY(object.try_for_each_member([&](StringView key, JsonValue const& value) -> ErrorOr<void> {
        if (key == "is_modified_read_read"sv) {
            if (auto is_modified_read_read = value.get_bool(); is_modified_read_read.has_value()) {
                if (is_modified_read_read.value())
                    flags |= 1u;
                return {};
            }
            return Error::from_string_literal("expected bool for \"is_modified_read_read\"");
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
        return Error::from_string_literal("if is_modified_read_read is true, other flags must be false");

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
            if (value.is_array()) {
                auto const& adaptive_template_pixels_json = value.as_array();
                for (auto const& value : adaptive_template_pixels_json.values()) {
                    if (auto pixel = value.get_i32(); pixel.has_value()) {
                        if (pixel.value() < -128 || pixel.value() > 127)
                            return Error::from_string_literal("expected i8 for \"adaptive_template_pixels\" elements");
                        adaptive_template_pixels.append(static_cast<i8>(pixel.value()));
                        continue;
                    }
                    return Error::from_string_literal("expected array of i8 for \"adaptive_template_pixels\"");
                }
                return {};
            }
            return Error::from_string_literal("expected array for \"adaptive_template_pixels\"");
        }

        if (key == "strip_trailing_7fffs"sv) {
            if (auto strip_trailing_7fffs = value.get_bool(); strip_trailing_7fffs.has_value()) {
                if (strip_trailing_7fffs.value())
                    trailing_7fff_handling = Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Remove;
                else
                    trailing_7fff_handling = Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Keep;
                return {};
            }
            return Error::from_string_literal("expected bool for \"strip_trailing_7fffs\"");
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
    if (adaptive_template_pixels.is_empty() && !uses_mmr) {
        // Default to Table 5 – The nominal values of the AT pixel locations
        if (gb_template == 0) {
            if (use_extended_template) {
                adaptive_template_pixels = {
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
            } else {
                adaptive_template_pixels = {
                    // clang-format off
                    3, -1,
                    -3, -1,
                    2, -2,
                    -2, -2,
                    // clang-format on
                };
            }
        } else if (gb_template == 1) {
            adaptive_template_pixels = { 3, -1 };
        } else {
            adaptive_template_pixels = { 2, -1 };
        }
    }

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
            if (value.is_array()) {
                auto const& adaptive_template_pixels_json = value.as_array();
                for (auto const& value : adaptive_template_pixels_json.values()) {
                    if (auto pixel = value.get_i32(); pixel.has_value()) {
                        if (pixel.value() < -128 || pixel.value() > 127)
                            return Error::from_string_literal("expected i8 for \"adaptive_template_pixels\" elements");
                        adaptive_template_pixels.append(static_cast<i8>(pixel.value()));
                        continue;
                    }
                    return Error::from_string_literal("expected array of i8 for \"adaptive_template_pixels\"");
                }
                return {};
            }
            return Error::from_string_literal("expected array for \"adaptive_template_pixels\"");
        }

        if (key == "strip_trailing_7fffs"sv) {
            if (auto strip_trailing_7fffs = value.get_bool(); strip_trailing_7fffs.has_value()) {
                if (strip_trailing_7fffs.value())
                    trailing_7fff_handling = Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Remove;
                else
                    trailing_7fff_handling = Gfx::MQArithmeticEncoder::Trailing7FFFHandling::Keep;
                return {};
            }
            return Error::from_string_literal("expected bool for \"strip_trailing_7fffs\"");
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
    if (adaptive_template_pixels.is_empty() && gr_template == 0) {
        // Default to Figure 12 – 13-pixel refinement template showing the AT pixels at their nominal locations
        adaptive_template_pixels = { -1, -1, -1, -1 };
    }

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

    if (type_string == "end_of_file")
        return jbig2_end_of_file_from_json(header, segment_data_object);
    if (type_string == "end_of_page")
        return jbig2_end_of_page_from_json(header, segment_data_object);
    if (type_string == "end_of_stripe")
        return jbig2_end_of_stripe_from_json(header, segment_data_object);
    if (type_string == "pattern_dictionary")
        return jbig2_pattern_dictionary_from_json(options, header, segment_data_object);
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
