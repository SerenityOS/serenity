/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/ICC/BinaryFormat.h>
#include <LibGfx/ICC/BinaryWriter.h>
#include <LibGfx/ICC/Profile.h>
#include <time.h>

#pragma GCC diagnostic ignored "-Warray-bounds"

namespace Gfx::ICC {

static ErrorOr<void> encode_tag_table(ByteBuffer& bytes, Profile const& profile)
{
    VERIFY(bytes.size() >= sizeof(ICCHeader) + sizeof(u32) + profile.tag_count() * sizeof(TagTableEntry));

    *bit_cast<BigEndian<u32>*>(bytes.data() + sizeof(ICCHeader)) = profile.tag_count();

    TagTableEntry* tag_table_entry = bit_cast<TagTableEntry*>(bytes.data() + sizeof(ICCHeader) + sizeof(u32));
    profile.for_each_tag([&tag_table_entry](auto tag_signature, auto) {
        tag_table_entry->tag_signature = tag_signature;
        tag_table_entry->offset_to_beginning_of_tag_data_element = 0; // FIXME
        tag_table_entry->size_of_tag_data_element = 0; // FIXME
        ++tag_table_entry;
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
    // Leaves enough room for the profile header and the tag table count.
    // FIXME: Serialize tag data and write tag data too.
    size_t tag_table_size = sizeof(u32) + profile.tag_count() * sizeof(TagTableEntry);
    auto bytes = TRY(ByteBuffer::create_zeroed(sizeof(ICCHeader) + tag_table_size));

    TRY(encode_tag_table(bytes, profile));
    TRY(encode_header(bytes, profile));

    return bytes;
}

}
