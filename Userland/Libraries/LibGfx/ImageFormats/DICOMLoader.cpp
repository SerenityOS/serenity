/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DICOMLoader.h"
#include <AK/Array.h>
#include <AK/BitStream.h>
#include <AK/CountingStream.h>
#include <AK/String.h>

namespace Gfx {

namespace {

auto constexpr MAGIC_HEADER = "DICM"sv;

// https://dicom.nema.org/medical/dicom/current/output/chtml/part05/chapter_7.html#sect_7.1.1
struct DataElement {
    u16 group_number {};
    u16 element_number {};

    // Values and their representations are defined here:
    // https://dicom.nema.org/medical/dicom/current/output/chtml/part05/sect_6.2.html
    Array<u8, 2> value_representation {};
    StringView value_representation_as_string { value_representation.span() };

    u32 value_length {};

    Variant<u32, String> value { 0 };
};

constexpr Array vr_with_small_length = { "AE"sv, "AS"sv, "AT"sv, "CS"sv, "DA"sv, "DS"sv, "DT"sv, "FL"sv, "FD"sv, "IS"sv, "LO"sv, "LT"sv, "PN"sv, "SH"sv, "SL"sv, "SS"sv, "ST"sv, "TM"sv, "UI"sv, "UL"sv, "US"sv };

enum class ShouldInterpretValue : u8 {
    No = 0,
    Yes = 1,
};

// https://dicom.nema.org/medical/dicom/current/output/chtml/part05/chapter_7.html#sect_7.1.2
ErrorOr<DataElement> read_data_element(Stream& stream, ShouldInterpretValue should_interpret_value)
{
    DataElement element {};

    element.group_number = TRY(stream.read_value<u16>());
    element.element_number = TRY(stream.read_value<u16>());

    TRY(stream.read_until_filled(element.value_representation));

    if (vr_with_small_length.contains_slow(element.value_representation_as_string)) {
        // "for VRs of AE, [...], UL and US the Value Length Field is the 16-bit unsigned integer
        // following the two byte VR Field (Table 7.1-2)."
        element.value_length = TRY(stream.read_value<u16>());
    } else {
        // "for all other VRs the 16 bits following the two byte VR Field are reserved for use by
        // later versions of the DICOM Standard. These reserved bytes shall be set to 0000H and shall not
        // be used or decoded (Table 7.1-1)."
        TRY(stream.discard(2));

        // "The Value Length Field is a 32-bit unsigned integer."
        element.value_length = TRY(stream.read_value<u32>());
    }

    auto read_string = [&]() -> ErrorOr<String> {
        ByteBuffer buffer;
        TRY(buffer.try_resize(element.value_length));
        TRY(stream.read_until_filled(buffer));
        return String::from_utf8(StringView { buffer.span() });
    };

    if (should_interpret_value == ShouldInterpretValue::Yes) {
        if (element.value_representation_as_string == "UL") {
            element.value = TRY(stream.read_value<u32>());
        } else if (element.value_representation_as_string == "US") {
            element.value = TRY(stream.read_value<u16>());
        } else if (element.value_representation_as_string == "PN") {
            element.value = TRY(TRY(read_string()).replace("^"sv, " "sv, ReplaceMode::All));
        } else if (element.value_representation_as_string == "LO" || element.value_representation_as_string == "CS") {
            element.value = TRY(read_string());
        } else if (element.value_representation_as_string == "DA") {
            // Format YYYYMMDD
            auto raw_date = TRY(read_string());
            StringBuilder builder;
            builder.append(raw_date.bytes_as_string_view().substring_view(6, 2));
            builder.append('/');
            builder.append(raw_date.bytes_as_string_view().substring_view(4, 2));
            builder.append('/');
            builder.append(raw_date.bytes_as_string_view().substring_view(0, 4));
            element.value = TRY(builder.to_string());
        } else {
            // This is not a known type, let's skip the bytes for now.
            TRY(stream.discard(element.value_length));
        }
    }

    return element;
}

// https://dicom.nema.org/medical/dicom/current/output/chtml/part10/chapter_7.html#sect_7.1
ErrorOr<void> read_file_meta_information(Stream& stream)
{
    // Skipping the file preamble
    TRY(stream.discard(128));

    Array<u8, 4> magic_bytes;
    TRY(stream.read_until_filled(magic_bytes));

    if (magic_bytes.span() != MAGIC_HEADER.bytes())
        return Error::from_string_literal("DICOMImageDecoderPlugin: Invalid DICOM Prefix");

    auto file_meta_information_group_length = TRY(read_data_element(stream, ShouldInterpretValue::Yes));
    if (file_meta_information_group_length.group_number != 2 && file_meta_information_group_length.element_number != 0)
        return Error::from_string_literal("DICOMImageDecoderPlugin: First element has to be 'File Meta Information Group Length'");

    // This simple decoder doesn't care about the other elements of the header, so let's skip it!
    TRY(stream.discard(file_meta_information_group_length.value.get<u32>()));

    return {};
}

struct DICOMMetadata : public Metadata {
    String institution_name {};
    String study_date {};
    String patient_name {};
    String patient_birth_date {};
    String body_part_examined {};

private:
    virtual void fill_main_tags() const override
    {
        if (!institution_name.is_empty())
            m_main_tags.set("Institution Name"sv, institution_name);
        if (!study_date.is_empty())
            m_main_tags.set("Study Date"sv, study_date);
        if (!patient_name.is_empty())
            m_main_tags.set("Patient's Name"sv, patient_name);
        if (!patient_birth_date.is_empty())
            m_main_tags.set("Patient Birth Date"sv, patient_birth_date);
        if (!body_part_examined.is_empty())
            m_main_tags.set("Body Part Examined"sv, body_part_examined);
    }
};

}

class DICOMLoadingContext {
public:
    DICOMLoadingContext(NonnullOwnPtr<FixedMemoryStream> stream)
        : m_stream(*move(stream))
    {
    }

    ErrorOr<void> decode_image_header()
    {
        TRY(read_file_meta_information(m_stream));

        // Data like image size and pixel type is stored in the "Data Set", so let's decode some elements as well.
        TRY(read_useful_elements());

        m_state = State::HeaderDecoded;
        return {};
    }

    ErrorOr<void> read_optional_metadata(DataElement const& element)
    {
        // "Study Date"
        if (element.group_number == 0x08 && element.element_number == 0x20) {
            if (element.value.has<String>())
                m_metadata.study_date = element.value.get<String>();
        }

        // "Institution Name"
        if (element.group_number == 0x08 && element.element_number == 0x80) {
            if (element.value.has<String>())
                m_metadata.institution_name = element.value.get<String>();
        }

        // "Patient's Name"
        // https://dicom.nema.org/medical/dicom/current/output/chtml/part03/sect_C.2.2.html#table_C.2-2
        if (element.group_number == 0x10 && element.element_number == 0x10) {
            if (element.value.has<String>())
                m_metadata.patient_name = element.value.get<String>();
        }

        // "Patient's Birth Date"
        if (element.group_number == 0x10 && element.element_number == 0x30) {
            if (element.value.has<String>())
                m_metadata.patient_birth_date = element.value.get<String>();
        }

        // "Body Part Examined"
        if (element.group_number == 0x18 && element.element_number == 0x15) {
            if (element.value.has<String>())
                m_metadata.body_part_examined = element.value.get<String>();
        }

        return {};
    }

    ErrorOr<void> read_useful_elements()
    {
        // We try to find some useful data for decoding like the image size or the bit depth.
        // All this information is stored in the 'US Image Module' group, described here in the spec:
        // https://dicom.nema.org/medical/dicom/current/output/chtml/part03/sect_C.8.5.6.html
        static constexpr u32 us_image_module_group = 40;

        Optional<DataElement> last_element;
        while (!last_element.has_value() || last_element->group_number <= us_image_module_group) {
            last_element = TRY(read_data_element(m_stream, ShouldInterpretValue::Yes));

            TRY(read_optional_metadata(*last_element));

            if (last_element->group_number == us_image_module_group && last_element->element_number == 2) {
                // SamplesPerPixel
                if (last_element->value != 1)
                    return Error::from_string_literal("DICOMImageDecoderPlugin: Unsupported value of SamplesPerPixel");
            } else if (last_element->group_number == us_image_module_group && last_element->element_number == 16) {
                // Rows
                m_size.set_height(last_element->value.get<u32>());
            } else if (last_element->group_number == us_image_module_group && last_element->element_number == 17) {
                // Columns
                m_size.set_width(last_element->value.get<u32>());
            } else if (last_element->group_number == us_image_module_group && last_element->element_number == 257) {
                // BitsStored
                if (last_element->value != 8 && last_element->value != 16)
                    return Error::from_string_literal("DICOMImageDecoderPlugin: Unsupported value of BitsStored");
                m_bit_depth = last_element->value.get<u32>();
            }
        }

        if (m_size.is_empty())
            return Error::from_string_literal("DICOMImageDecoderPlugin: Unable to find the image's dimensions");

        return {};
    }

    ErrorOr<void> decode()
    {
        // https://dicom.nema.org/medical/dicom/current/output/chtml/part03/sect_C.7.6.3.html#table_C.7-11a
        static constexpr u32 image_pixel_module_group = 32763;
        static constexpr u32 pixel_data_element = 16;

        while (true) {
            auto element = TRY(read_data_element(m_stream, ShouldInterpretValue::No));
            if (element.group_number != image_pixel_module_group && element.element_number != pixel_data_element) {
                TRY(m_stream.discard(element.value_length));
                continue;
            }

            m_bitmap = TRY(Bitmap::create(BitmapFormat::BGRx8888, m_size));
            for (i32 y = 0; y < m_size.height(); ++y) {
                for (i32 x = 0; x < m_size.width(); ++x) {
                    u8 luma = m_bit_depth == 8 ? TRY(m_stream.read_value<u8>()) : TRY(m_stream.read_value<u16>()) >> 8;
                    auto gray = Color { luma, luma, luma };
                    m_bitmap->set_pixel(x, y, gray);
                }
            }

            break;
        }

        m_state = State::FrameDecoded;
        return {};
    }

    enum class State {
        NotDecoded = 0,
        Error,
        HeaderDecoded,
        FrameDecoded,
    };

    State state() const
    {
        return m_state;
    }

    IntSize size() const
    {
        return m_size;
    }

    RefPtr<Bitmap> bitmap() const
    {
        return m_bitmap;
    }

    DICOMMetadata const& metadata() const
    {
        return m_metadata;
    }

private:
    FixedMemoryStream m_stream;

    State m_state {};
    IntSize m_size {};
    u32 m_bit_depth {};
    RefPtr<Bitmap> m_bitmap {};

    DICOMMetadata m_metadata;
};

DICOMImageDecoderPlugin::DICOMImageDecoderPlugin(NonnullOwnPtr<FixedMemoryStream> stream)
    : m_context(make<DICOMLoadingContext>(move(stream)))
{
}

DICOMImageDecoderPlugin::~DICOMImageDecoderPlugin() = default;

IntSize DICOMImageDecoderPlugin::size()
{
    return m_context->size();
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> DICOMImageDecoderPlugin::create(ReadonlyBytes data)
{
    auto stream = TRY(try_make<FixedMemoryStream>(data));
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) DICOMImageDecoderPlugin(move(stream))));
    TRY(plugin->m_context->decode_image_header());
    return plugin;
}

bool DICOMImageDecoderPlugin::sniff(ReadonlyBytes bytes)
{
    if (bytes.size() < 132)
        return false;
    if (!bytes.starts_with(Array<u8, 128> {}.span()))
        return false;
    if (bytes.slice(128).trim(4) != MAGIC_HEADER.bytes())

        return false;
    return true;
}

ErrorOr<ImageFrameDescriptor> DICOMImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
{
    if (index > 0)
        return Error::from_string_literal("DICOMImageDecoderPlugin: Invalid frame index");

    if (m_context->state() == DICOMLoadingContext::State::Error)
        return Error::from_string_literal("DICOMImageDecoderPlugin: Decoding failed");

    if (m_context->state() < DICOMLoadingContext::State::FrameDecoded)
        TRY(m_context->decode());

    return ImageFrameDescriptor { m_context->bitmap(), 0 };
}

Optional<Metadata const&> DICOMImageDecoderPlugin::metadata()
{
    return m_context->metadata();
}

}
