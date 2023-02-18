/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf16View.h>
#include <LibGfx/ICC/BinaryFormat.h>
#include <LibGfx/ICC/BinaryWriter.h>
#include <LibGfx/ICC/Profile.h>
#include <time.h>

#pragma GCC diagnostic ignored "-Warray-bounds"

namespace Gfx::ICC {

static ErrorOr<ByteBuffer> encode_chromaticity(ChromaticityTagData const& tag_data)
{
    // ICC v4, 10.2 chromaticityType
    auto bytes = TRY(ByteBuffer::create_uninitialized(2 * sizeof(u32) + 2 * sizeof(u16) + tag_data.xy_coordinates().size() * 2 * sizeof(u16Fixed16Number)));

    *bit_cast<BigEndian<u32>*>(bytes.data()) = (u32)ChromaticityTagData::Type;
    *bit_cast<BigEndian<u32>*>(bytes.data() + 4) = 0;

    *bit_cast<BigEndian<u16>*>(bytes.data() + 8) = tag_data.xy_coordinates().size();
    *bit_cast<BigEndian<u16>*>(bytes.data() + 10) = (u16)tag_data.phosphor_or_colorant_type();

    auto* coordinates = bit_cast<BigEndian<u16Fixed16Number>*>(bytes.data() + 12);
    for (size_t i = 0; i < tag_data.xy_coordinates().size(); ++i) {
        coordinates[2 * i] = tag_data.xy_coordinates()[i].x.raw();
        coordinates[2 * i + 1] = tag_data.xy_coordinates()[i].y.raw();
    }

    return bytes;
}

static ErrorOr<ByteBuffer> encode_cipc(CicpTagData const& tag_data)
{
    // ICC v4, 10.3 cicpType
    auto bytes = TRY(ByteBuffer::create_uninitialized(2 * sizeof(u32) + 4));
    *bit_cast<BigEndian<u32>*>(bytes.data()) = (u32)CicpTagData::Type;
    *bit_cast<BigEndian<u32>*>(bytes.data() + 4) = 0;
    bytes.data()[8] = tag_data.color_primaries();
    bytes.data()[9] = tag_data.transfer_characteristics();
    bytes.data()[10] = tag_data.matrix_coefficients();
    bytes.data()[11] = tag_data.video_full_range_flag();
    return bytes;
}

static ErrorOr<ByteBuffer> encode_curve(CurveTagData const& tag_data)
{
    // ICC v4, 10.6 curveType
    auto bytes = TRY(ByteBuffer::create_uninitialized(3 * sizeof(u32) + tag_data.values().size() * sizeof(u16)));
    *bit_cast<BigEndian<u32>*>(bytes.data()) = (u32)CurveTagData::Type;
    *bit_cast<BigEndian<u32>*>(bytes.data() + 4) = 0;
    *bit_cast<BigEndian<u32>*>(bytes.data() + 8) = tag_data.values().size();

    auto* values = bit_cast<BigEndian<u16>*>(bytes.data() + 12);
    for (size_t i = 0; i < tag_data.values().size(); ++i)
        values[i] = tag_data.values()[i];

    return bytes;
}

static ErrorOr<ByteBuffer> encode_multi_localized_unicode(MultiLocalizedUnicodeTagData const& tag_data)
{
    // ICC v4, 10.15 multiLocalizedUnicodeType
    // "The Unicode strings in storage should be encoded as 16-bit big-endian, UTF-16BE,
    //  and should not be NULL terminated."
    size_t number_of_records = tag_data.records().size();
    size_t header_and_record_size = 4 * sizeof(u32) + number_of_records * sizeof(MultiLocalizedUnicodeRawRecord);

    size_t number_of_codepoints = 0;
    Vector<Utf16Data> utf16_strings;
    TRY(utf16_strings.try_ensure_capacity(number_of_records));
    for (auto const& record : tag_data.records()) {
        TRY(utf16_strings.try_append(TRY(utf8_to_utf16(record.text))));
        number_of_codepoints += utf16_strings.last().size();
    }

    size_t string_table_size = number_of_codepoints * sizeof(u16);

    auto bytes = TRY(ByteBuffer::create_uninitialized(header_and_record_size + string_table_size));

    auto* header = bit_cast<BigEndian<u32>*>(bytes.data());
    header[0] = (u32)MultiLocalizedUnicodeTagData::Type;
    header[1] = 0;
    header[2] = number_of_records;
    header[3] = sizeof(MultiLocalizedUnicodeRawRecord);

    size_t offset = header_and_record_size;
    auto* records = bit_cast<MultiLocalizedUnicodeRawRecord*>(bytes.data() + 16);
    for (size_t i = 0; i < number_of_records; ++i) {
        records[i].language_code = tag_data.records()[i].iso_639_1_language_code;
        records[i].country_code = tag_data.records()[i].iso_3166_1_country_code;
        records[i].string_length_in_bytes = utf16_strings[i].size() * sizeof(u16);
        records[i].string_offset_in_bytes = offset;
        offset += records[i].string_offset_in_bytes;
    }

    auto* string_table = bit_cast<BigEndian<u16>*>(bytes.data() + header_and_record_size);
    for (auto const& utf16_string : utf16_strings) {
        for (size_t i = 0; i < utf16_string.size(); ++i)
            string_table[i] = utf16_string[i];
        string_table += utf16_string.size();
    }

    return bytes;
}

static ErrorOr<ByteBuffer> encode_parametric_curve(ParametricCurveTagData const& tag_data)
{
    // ICC v4, 10.18 parametricCurveType
    auto bytes = TRY(ByteBuffer::create_uninitialized(2 * sizeof(u32) + 2 * sizeof(u16) + tag_data.parameter_count() * sizeof(s15Fixed16Number)));
    *bit_cast<BigEndian<u32>*>(bytes.data()) = (u32)ParametricCurveTagData::Type;
    *bit_cast<BigEndian<u32>*>(bytes.data() + 4) = 0;

    *bit_cast<BigEndian<u16>*>(bytes.data() + 8) = (u16)tag_data.function_type();
    *bit_cast<BigEndian<u16>*>(bytes.data() + 10) = 0;

    auto* parameters = bit_cast<BigEndian<s15Fixed16Number>*>(bytes.data() + 12);
    for (size_t i = 0; i < tag_data.parameter_count(); ++i)
        parameters[i] = tag_data.parameter(i).raw();

    return bytes;
}

static ErrorOr<ByteBuffer> encode_s15_fixed_array(S15Fixed16ArrayTagData const& tag_data)
{
    // ICC v4, 10.22 s15Fixed16ArrayType
    auto bytes = TRY(ByteBuffer::create_uninitialized(2 * sizeof(u32) + tag_data.values().size() * sizeof(s15Fixed16Number)));
    *bit_cast<BigEndian<u32>*>(bytes.data()) = (u32)S15Fixed16ArrayTagData::Type;
    *bit_cast<BigEndian<u32>*>(bytes.data() + 4) = 0;

    auto* values = bit_cast<BigEndian<s15Fixed16Number>*>(bytes.data() + 8);
    for (size_t i = 0; i < tag_data.values().size(); ++i)
        values[i] = tag_data.values()[i].raw();

    return bytes;
}

static ErrorOr<ByteBuffer> encode_xyz(XYZTagData const& tag_data)
{
    // ICC v4, 10.31 XYZType
    auto bytes = TRY(ByteBuffer::create_uninitialized(2 * sizeof(u32) + tag_data.xyzs().size() * sizeof(XYZNumber)));
    *bit_cast<BigEndian<u32>*>(bytes.data()) = (u32)XYZTagData::Type;
    *bit_cast<BigEndian<u32>*>(bytes.data() + 4) = 0;

    auto* xyzs = bit_cast<XYZNumber*>(bytes.data() + 8);
    for (size_t i = 0; i < tag_data.xyzs().size(); ++i)
        xyzs[i] = tag_data.xyzs()[i];

    return bytes;
}

static ErrorOr<ByteBuffer> encode_tag_data(TagData const& tag_data)
{
    switch (tag_data.type()) {
    case ChromaticityTagData::Type:
        return encode_chromaticity(static_cast<ChromaticityTagData const&>(tag_data));
    case CicpTagData::Type:
        return encode_cipc(static_cast<CicpTagData const&>(tag_data));
    case CurveTagData::Type:
        return encode_curve(static_cast<CurveTagData const&>(tag_data));
    case MultiLocalizedUnicodeTagData::Type:
        return encode_multi_localized_unicode(static_cast<MultiLocalizedUnicodeTagData const&>(tag_data));
    case ParametricCurveTagData::Type:
        return encode_parametric_curve(static_cast<ParametricCurveTagData const&>(tag_data));
    case S15Fixed16ArrayTagData::Type:
        return encode_s15_fixed_array(static_cast<S15Fixed16ArrayTagData const&>(tag_data));
    case XYZTagData::Type:
        return encode_xyz(static_cast<XYZTagData const&>(tag_data));
    }
    return ByteBuffer {};
}

static ErrorOr<Vector<ByteBuffer>> encode_tag_datas(Profile const& profile)
{
    Vector<ByteBuffer> tag_data_bytes;

    // FIXME: If two tags refer to the same TagData object, write it just once to the output.
    TRY(tag_data_bytes.try_resize(profile.tag_count()));
    size_t i = 0;
    profile.for_each_tag([&](auto, auto tag_data) {
        // FIXME: Come up with a way to allow TRY instead of MUST here.
        tag_data_bytes[i++] = MUST(encode_tag_data(tag_data));
    });
    return tag_data_bytes;
}

static ErrorOr<void> encode_tag_table(ByteBuffer& bytes, Profile const& profile, Vector<size_t> const& offsets, Vector<ByteBuffer> const& tag_data_bytes)
{
    VERIFY(bytes.size() >= sizeof(ICCHeader) + sizeof(u32) + profile.tag_count() * sizeof(TagTableEntry));

    *bit_cast<BigEndian<u32>*>(bytes.data() + sizeof(ICCHeader)) = profile.tag_count();

    TagTableEntry* tag_table_entries = bit_cast<TagTableEntry*>(bytes.data() + sizeof(ICCHeader) + sizeof(u32));
    int i = 0;
    profile.for_each_tag([&](auto tag_signature, auto) {
        tag_table_entries[i].tag_signature = tag_signature;
        tag_table_entries[i].offset_to_beginning_of_tag_data_element = offsets[i];
        tag_table_entries[i].size_of_tag_data_element = tag_data_bytes[i].size();
        ++i;
    });

    return {};
}

static ErrorOr<void> encode_header(ByteBuffer& bytes, Profile const& profile)
{
    VERIFY(bytes.size() >= sizeof(ICCHeader));
    auto& raw_header = *bit_cast<ICCHeader*>(bytes.data());

    raw_header.profile_size = bytes.size();
    raw_header.preferred_cmm_type = profile.preferred_cmm_type().value_or(PreferredCMMType { 0 });

    raw_header.profile_version_major = profile.version().major_version();
    raw_header.profile_version_minor_bugfix = profile.version().minor_and_bugfix_version();
    raw_header.profile_version_zero = 0;

    raw_header.profile_device_class = profile.device_class();
    raw_header.data_color_space = profile.data_color_space();
    raw_header.profile_connection_space = profile.connection_space();

    time_t profile_timestamp = profile.creation_timestamp();
    struct tm tm;
    if (!gmtime_r(&profile_timestamp, &tm))
        return Error::from_errno(errno);
    raw_header.profile_creation_time.year = tm.tm_year + 1900;
    raw_header.profile_creation_time.month = tm.tm_mon + 1;
    raw_header.profile_creation_time.day = tm.tm_mday;
    raw_header.profile_creation_time.hours = tm.tm_hour;
    raw_header.profile_creation_time.minutes = tm.tm_min;
    raw_header.profile_creation_time.seconds = tm.tm_sec;

    raw_header.profile_file_signature = ProfileFileSignature;
    raw_header.primary_platform = profile.primary_platform().value_or(PrimaryPlatform { 0 });

    raw_header.profile_flags = profile.flags().bits();
    raw_header.device_manufacturer = profile.device_manufacturer().value_or(DeviceManufacturer { 0 });
    raw_header.device_model = profile.device_model().value_or(DeviceModel { 0 });
    raw_header.device_attributes = profile.device_attributes().bits();
    raw_header.rendering_intent = profile.rendering_intent();

    raw_header.pcs_illuminant = profile.pcs_illuminant();

    raw_header.profile_creator = profile.creator().value_or(Creator { 0 });

    memset(raw_header.reserved, 0, sizeof(raw_header.reserved));

    auto id = Profile::compute_id(bytes);
    static_assert(sizeof(id.data) == sizeof(raw_header.profile_id));
    memcpy(raw_header.profile_id, id.data, sizeof(id.data));

    return {};
}

ErrorOr<ByteBuffer> encode(Profile const& profile)
{
    // Valid profiles always have tags. Profile only represents valid profiles.
    VERIFY(profile.tag_count() > 0);

    Vector<ByteBuffer> tag_data_bytes = TRY(encode_tag_datas(profile));

    size_t tag_table_size = sizeof(u32) + profile.tag_count() * sizeof(TagTableEntry);
    size_t offset = sizeof(ICCHeader) + tag_table_size;
    Vector<size_t> offsets;
    for (auto const& bytes : tag_data_bytes) {
        TRY(offsets.try_append(offset));
        offset += align_up_to(bytes.size(), 4);
    }

    // Omit padding after last element.
    // FIXME: Is that correct?
    size_t total_size = offsets.last() + tag_data_bytes.last().size();

    // Leave enough room for the profile header and the tag table count.
    auto bytes = TRY(ByteBuffer::create_zeroed(total_size));

    for (size_t i = 0; i < tag_data_bytes.size(); ++i)
        memcpy(bytes.data() + offsets[i], tag_data_bytes[i].data(), tag_data_bytes[i].size());

    TRY(encode_tag_table(bytes, profile, offsets, tag_data_bytes));
    TRY(encode_header(bytes, profile));

    return bytes;
}

}
