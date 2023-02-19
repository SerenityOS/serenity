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

    *bit_cast<BigEndian<u32>*>(bytes.data()) = static_cast<u32>(ChromaticityTagData::Type);
    *bit_cast<BigEndian<u32>*>(bytes.data() + 4) = 0;

    *bit_cast<BigEndian<u16>*>(bytes.data() + 8) = tag_data.xy_coordinates().size();
    *bit_cast<BigEndian<u16>*>(bytes.data() + 10) = static_cast<u16>(tag_data.phosphor_or_colorant_type());

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
    *bit_cast<BigEndian<u32>*>(bytes.data()) = static_cast<u32>(CicpTagData::Type);
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
    *bit_cast<BigEndian<u32>*>(bytes.data()) = static_cast<u32>(CurveTagData::Type);
    *bit_cast<BigEndian<u32>*>(bytes.data() + 4) = 0;
    *bit_cast<BigEndian<u32>*>(bytes.data() + 8) = tag_data.values().size();

    auto* values = bit_cast<BigEndian<u16>*>(bytes.data() + 12);
    for (size_t i = 0; i < tag_data.values().size(); ++i)
        values[i] = tag_data.values()[i];

    return bytes;
}

static ErrorOr<ByteBuffer> encode_measurement(MeasurementTagData const& tag_data)
{
    // ICC v4, 10.14 measurementType
    auto bytes = TRY(ByteBuffer::create_uninitialized(2 * sizeof(u32) + sizeof(MeasurementHeader)));
    *bit_cast<BigEndian<u32>*>(bytes.data()) = static_cast<u32>(MeasurementTagData::Type);
    *bit_cast<BigEndian<u32>*>(bytes.data() + 4) = 0;

    auto& header = *bit_cast<MeasurementHeader*>(bytes.data() + 8);
    header.standard_observer = tag_data.standard_observer();
    header.tristimulus_value_for_measurement_backing = tag_data.tristimulus_value_for_measurement_backing();
    header.measurement_geometry = tag_data.measurement_geometry();
    header.measurement_flare = tag_data.measurement_flare().raw();
    header.standard_illuminant = tag_data.standard_illuminant();

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
    header[0] = static_cast<u32>(MultiLocalizedUnicodeTagData::Type);
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
        offset += records[i].string_length_in_bytes;
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
    *bit_cast<BigEndian<u32>*>(bytes.data()) = static_cast<u32>(ParametricCurveTagData::Type);
    *bit_cast<BigEndian<u32>*>(bytes.data() + 4) = 0;

    *bit_cast<BigEndian<u16>*>(bytes.data() + 8) = static_cast<u16>(tag_data.function_type());
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
    *bit_cast<BigEndian<u32>*>(bytes.data()) = static_cast<u32>(S15Fixed16ArrayTagData::Type);
    *bit_cast<BigEndian<u32>*>(bytes.data() + 4) = 0;

    auto* values = bit_cast<BigEndian<s15Fixed16Number>*>(bytes.data() + 8);
    for (size_t i = 0; i < tag_data.values().size(); ++i)
        values[i] = tag_data.values()[i].raw();

    return bytes;
}

static ErrorOr<ByteBuffer> encode_signature(SignatureTagData const& tag_data)
{
    // ICC v4, 10.23 signatureType
    auto bytes = TRY(ByteBuffer::create_uninitialized(3 * sizeof(u32)));
    *bit_cast<BigEndian<u32>*>(bytes.data()) = static_cast<u32>(SignatureTagData::Type);
    *bit_cast<BigEndian<u32>*>(bytes.data() + 4) = 0;
    *bit_cast<BigEndian<u32>*>(bytes.data() + 8) = tag_data.signature();
    return bytes;
}

static ErrorOr<ByteBuffer> encode_text_description(TextDescriptionTagData const& tag_data)
{
    // ICC v2, 6.5.17 textDescriptionType
    // All lengths include room for a trailing nul character.
    // See also the many comments in TextDescriptionTagData::from_bytes().
    u32 ascii_size = sizeof(u32) + tag_data.ascii_description().bytes().size() + 1;

    // FIXME: Include tag_data.unicode_description() if it's set.
    u32 unicode_size = 2 * sizeof(u32);

    // FIXME: Include tag_data.macintosh_description() if it's set.
    u32 macintosh_size = sizeof(u16) + sizeof(u8) + 67;

    auto bytes = TRY(ByteBuffer::create_uninitialized(2 * sizeof(u32) + ascii_size + unicode_size + macintosh_size));
    *bit_cast<BigEndian<u32>*>(bytes.data()) = static_cast<u32>(TextDescriptionTagData::Type);
    *bit_cast<BigEndian<u32>*>(bytes.data() + 4) = 0;

    // ASCII
    *bit_cast<BigEndian<u32>*>(bytes.data() + 8) = tag_data.ascii_description().bytes().size() + 1;
    memcpy(bytes.data() + 12, tag_data.ascii_description().bytes().data(), tag_data.ascii_description().bytes().size());
    bytes.data()[12 + tag_data.ascii_description().bytes().size()] = '\0';

    // Unicode
    // "Because the Unicode language code and Unicode count immediately follow the ASCII description,
    //  their alignment is not correct when the ASCII count is not a multiple of four"
    // So we can't use BigEndian<u32> here.
    u8* cursor = bytes.data() + 8 + ascii_size;
    u32 unicode_language_code = 0; // FIXME: Set to tag_data.unicode_language_code() once this writes unicode data.
    cursor[0] = unicode_language_code >> 24;
    cursor[1] = (unicode_language_code >> 16) & 0xff;
    cursor[2] = (unicode_language_code >> 8) & 0xff;
    cursor[3] = unicode_language_code & 0xff;
    cursor += 4;

    // FIXME: Include tag_data.unicode_description() if it's set.
    u32 ucs2_count = 0; // FIXME: If tag_data.unicode_description() is set, set this to its length plus room for one nul character.
    cursor[0] = ucs2_count >> 24;
    cursor[1] = (ucs2_count >> 16) & 0xff;
    cursor[2] = (ucs2_count >> 8) & 0xff;
    cursor[3] = ucs2_count & 0xff;
    cursor += 4;

    // Macintosh scriptcode
    u16 scriptcode_code = 0; // MacRoman
    cursor[0] = (scriptcode_code >> 8) & 0xff;
    cursor[1] = scriptcode_code & 0xff;
    cursor += 2;

    u8 macintosh_description_length = 0; // FIXME: If tag_data.macintosh_description() is set, set this to tis length plus room for one nul character.
    cursor[0] = macintosh_description_length;
    cursor += 1;
    memset(cursor, 0, 67);

    return bytes;
}

static ErrorOr<ByteBuffer> encode_text(TextTagData const& tag_data)
{
    // ICC v4, 10.24 textType
    // "The textType is a simple text structure that contains a 7-bit ASCII text string. The length of the string is obtained
    //  by subtracting 8 from the element size portion of the tag itself. This string shall be terminated with a 00h byte."
    auto text_bytes = tag_data.text().bytes();
    auto bytes = TRY(ByteBuffer::create_uninitialized(2 * sizeof(u32) + text_bytes.size() + 1));
    *bit_cast<BigEndian<u32>*>(bytes.data()) = static_cast<u32>(TextTagData::Type);
    *bit_cast<BigEndian<u32>*>(bytes.data() + 4) = 0;
    memcpy(bytes.data() + 8, text_bytes.data(), text_bytes.size());
    *(bytes.data() + 8 + text_bytes.size()) = '\0';
    return bytes;
}

static ErrorOr<ByteBuffer> encode_viewing_conditions(ViewingConditionsTagData const& tag_data)
{
    // ICC v4, 10.30 viewingConditionsType
    auto bytes = TRY(ByteBuffer::create_uninitialized(2 * sizeof(u32) + sizeof(ViewingConditionsHeader)));
    *bit_cast<BigEndian<u32>*>(bytes.data()) = static_cast<u32>(ViewingConditionsTagData::Type);
    *bit_cast<BigEndian<u32>*>(bytes.data() + 4) = 0;

    auto& header = *bit_cast<ViewingConditionsHeader*>(bytes.data() + 8);
    header.unnormalized_ciexyz_values_for_illuminant = tag_data.unnormalized_ciexyz_values_for_illuminant();
    header.unnormalized_ciexyz_values_for_surround = tag_data.unnormalized_ciexyz_values_for_surround();
    header.illuminant_type = tag_data.illuminant_type();

    return bytes;
}

static ErrorOr<ByteBuffer> encode_xyz(XYZTagData const& tag_data)
{
    // ICC v4, 10.31 XYZType
    auto bytes = TRY(ByteBuffer::create_uninitialized(2 * sizeof(u32) + tag_data.xyzs().size() * sizeof(XYZNumber)));
    *bit_cast<BigEndian<u32>*>(bytes.data()) = static_cast<u32>(XYZTagData::Type);
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
    case MeasurementTagData::Type:
        return encode_measurement(static_cast<MeasurementTagData const&>(tag_data));
    case MultiLocalizedUnicodeTagData::Type:
        return encode_multi_localized_unicode(static_cast<MultiLocalizedUnicodeTagData const&>(tag_data));
    case ParametricCurveTagData::Type:
        return encode_parametric_curve(static_cast<ParametricCurveTagData const&>(tag_data));
    case S15Fixed16ArrayTagData::Type:
        return encode_s15_fixed_array(static_cast<S15Fixed16ArrayTagData const&>(tag_data));
    case SignatureTagData::Type:
        return encode_signature(static_cast<SignatureTagData const&>(tag_data));
    case TextDescriptionTagData::Type:
        return encode_text_description(static_cast<TextDescriptionTagData const&>(tag_data));
    case TextTagData::Type:
        return encode_text(static_cast<TextTagData const&>(tag_data));
    case ViewingConditionsTagData::Type:
        return encode_viewing_conditions(static_cast<ViewingConditionsTagData const&>(tag_data));
    case XYZTagData::Type:
        return encode_xyz(static_cast<XYZTagData const&>(tag_data));
    }

    // FIXME: If this gets hit, we always write an invalid icc output file.
    // Make this return an Optional and don't write tags that have types we can't encode.
    // Not ideal, but better than writing invalid outputs.
    return ByteBuffer {};
}

static ErrorOr<Vector<ByteBuffer>> encode_tag_datas(Profile const& profile, HashMap<TagData*, size_t>& tag_data_map)
{
    Vector<ByteBuffer> tag_data_bytes;
    TRY(tag_data_bytes.try_ensure_capacity(profile.tag_count()));

    profile.for_each_tag([&](auto, auto tag_data) {
        if (tag_data_map.contains(tag_data.ptr()))
            return;

        // FIXME: Come up with a way to allow TRY instead of MUST here.
        tag_data_bytes.append(MUST(encode_tag_data(tag_data)));
        MUST(tag_data_map.try_set(tag_data.ptr(), tag_data_bytes.size() - 1));
    });
    return tag_data_bytes;
}

static ErrorOr<void> encode_tag_table(ByteBuffer& bytes, Profile const& profile, Vector<size_t> const& offsets, Vector<ByteBuffer> const& tag_data_bytes, HashMap<TagData*, size_t> const& tag_data_map)
{
    // ICC v4, 7.3 Tag table
    // ICC v4, 7.3.1 Overview
    VERIFY(bytes.size() >= sizeof(ICCHeader) + sizeof(u32) + profile.tag_count() * sizeof(TagTableEntry));

    *bit_cast<BigEndian<u32>*>(bytes.data() + sizeof(ICCHeader)) = profile.tag_count();

    TagTableEntry* tag_table_entries = bit_cast<TagTableEntry*>(bytes.data() + sizeof(ICCHeader) + sizeof(u32));
    int i = 0;
    profile.for_each_tag([&](auto tag_signature, auto tag_data) {
        tag_table_entries[i].tag_signature = tag_signature;

        auto index = tag_data_map.get(tag_data.ptr()).value();
        tag_table_entries[i].offset_to_beginning_of_tag_data_element = offsets[index];
        tag_table_entries[i].size_of_tag_data_element = tag_data_bytes[index].size();
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

    HashMap<TagData*, size_t> tag_data_map;
    Vector<ByteBuffer> tag_data_bytes = TRY(encode_tag_datas(profile, tag_data_map));

    size_t tag_table_size = sizeof(u32) + profile.tag_count() * sizeof(TagTableEntry);
    size_t offset = sizeof(ICCHeader) + tag_table_size;
    Vector<size_t> offsets;
    for (auto const& bytes : tag_data_bytes) {
        TRY(offsets.try_append(offset));
        offset += align_up_to(bytes.size(), 4);
    }

    // Omit padding after last element.
    size_t total_size = offsets.last() + tag_data_bytes.last().size();

    auto bytes = TRY(ByteBuffer::create_zeroed(total_size));

    for (size_t i = 0; i < tag_data_bytes.size(); ++i)
        memcpy(bytes.data() + offsets[i], tag_data_bytes[i].data(), tag_data_bytes[i].size());

    TRY(encode_tag_table(bytes, profile, offsets, tag_data_bytes, tag_data_map));
    TRY(encode_header(bytes, profile));

    return bytes;
}

}
