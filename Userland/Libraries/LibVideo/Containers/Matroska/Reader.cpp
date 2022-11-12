/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <Zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/Optional.h>
#include <AK/Utf8View.h>
#include <LibCore/MappedFile.h>

#include "Reader.h"

namespace Video::Matroska {

#define TRY_READ(expression) DECODER_TRY(DecoderErrorCategory::Corrupted, expression)

constexpr u32 EBML_MASTER_ELEMENT_ID = 0x1A45DFA3;
constexpr u32 SEGMENT_ELEMENT_ID = 0x18538067;
constexpr u32 DOCTYPE_ELEMENT_ID = 0x4282;
constexpr u32 DOCTYPE_VERSION_ELEMENT_ID = 0x4287;
constexpr u32 SEGMENT_INFORMATION_ELEMENT_ID = 0x1549A966;
constexpr u32 TRACK_ELEMENT_ID = 0x1654AE6B;
constexpr u32 CLUSTER_ELEMENT_ID = 0x1F43B675;
constexpr u32 TIMESTAMP_SCALE_ID = 0x2AD7B1;
constexpr u32 MUXING_APP_ID = 0x4D80;
constexpr u32 WRITING_APP_ID = 0x5741;
constexpr u32 DURATION_ID = 0x4489;

// Tracks
constexpr u32 TRACK_ENTRY_ID = 0xAE;
constexpr u32 TRACK_NUMBER_ID = 0xD7;
constexpr u32 TRACK_UID_ID = 0x73C5;
constexpr u32 TRACK_TYPE_ID = 0x83;
constexpr u32 TRACK_LANGUAGE_ID = 0x22B59C;
constexpr u32 TRACK_CODEC_ID = 0x86;
constexpr u32 TRACK_VIDEO_ID = 0xE0;
constexpr u32 TRACK_AUDIO_ID = 0xE1;

// Video
constexpr u32 PIXEL_WIDTH_ID = 0xB0;
constexpr u32 PIXEL_HEIGHT_ID = 0xBA;
constexpr u32 COLOR_ENTRY_ID = 0x55B0;
constexpr u32 PRIMARIES_ID = 0x55BB;
constexpr u32 TRANSFER_CHARACTERISTICS_ID = 0x55BA;
constexpr u32 MATRIX_COEFFICIENTS_ID = 0x55B1;
constexpr u32 BITS_PER_CHANNEL_ID = 0x55B2;

// Audio
constexpr u32 CHANNELS_ID = 0x9F;
constexpr u32 BIT_DEPTH_ID = 0x6264;

// Clusters
constexpr u32 SIMPLE_BLOCK_ID = 0xA3;
constexpr u32 TIMESTAMP_ID = 0xE7;

DecoderErrorOr<NonnullOwnPtr<MatroskaDocument>> Reader::parse_matroska_from_file(StringView path)
{
    auto mapped_file = DECODER_TRY(DecoderErrorCategory::IO, Core::MappedFile::map(path));
    return parse_matroska_from_data(mapped_file->bytes());
}

DecoderErrorOr<NonnullOwnPtr<MatroskaDocument>> Reader::parse_matroska_from_data(ReadonlyBytes data)
{
    Reader reader(data);
    return reader.parse();
}

static DecoderErrorOr<void> parse_master_element(Streamer& streamer, [[maybe_unused]] StringView element_name, Function<DecoderErrorOr<void>(u64)> element_consumer)
{
    auto element_data_size = TRY_READ(streamer.read_variable_size_integer());
    dbgln_if(MATROSKA_DEBUG, "{} has {} octets of data.", element_name, element_data_size);

    streamer.push_octets_read();
    while (streamer.octets_read() < element_data_size) {
        dbgln_if(MATROSKA_TRACE_DEBUG, "====== Reading  element ======");
        auto element_id = TRY_READ(streamer.read_variable_size_integer(false));
        dbgln_if(MATROSKA_TRACE_DEBUG, "{:s} element ID is {:#010x}\n", element_name, element_id);

        TRY(element_consumer(element_id));
        dbgln_if(MATROSKA_TRACE_DEBUG, "Read {} octets of the {} so far.", streamer.octets_read(), element_name);
    }
    streamer.pop_octets_read();

    return {};
}

static DecoderErrorOr<EBMLHeader> parse_ebml_header(Streamer& streamer)
{
    EBMLHeader header;
    TRY(parse_master_element(streamer, "Header"sv, [&](u64 element_id) -> DecoderErrorOr<void> {
        switch (element_id) {
        case DOCTYPE_ELEMENT_ID:
            header.doc_type = TRY_READ(streamer.read_string());
            dbgln_if(MATROSKA_DEBUG, "Read DocType attribute: {}", header.doc_type);
            break;
        case DOCTYPE_VERSION_ELEMENT_ID:
            header.doc_type_version = TRY_READ(streamer.read_u64());
            dbgln_if(MATROSKA_DEBUG, "Read DocTypeVersion attribute: {}", header.doc_type_version);
            break;
        default:
            TRY_READ(streamer.read_unknown_element());
        }

        return {};
    }));

    return header;
}

static DecoderErrorOr<NonnullOwnPtr<SegmentInformation>> parse_information(Streamer& streamer);
static DecoderErrorOr<void> parse_tracks(Streamer& streamer, MatroskaDocument& matroska_document);
static DecoderErrorOr<NonnullOwnPtr<Cluster>> parse_cluster(Streamer& streamer);

static DecoderErrorOr<void> parse_segment_elements(Streamer& streamer, MatroskaDocument& matroska_document)
{
    dbgln_if(MATROSKA_DEBUG, "Parsing segment elements");
    return parse_master_element(streamer, "Segment"sv, [&](u64 element_id) -> DecoderErrorOr<void> {
        switch (element_id) {
        case SEGMENT_INFORMATION_ELEMENT_ID:
            matroska_document.set_segment_information(TRY(parse_information(streamer)));
            break;
        case TRACK_ELEMENT_ID:
            TRY(parse_tracks(streamer, matroska_document));
            break;
        case CLUSTER_ELEMENT_ID:
            matroska_document.clusters().append(TRY(parse_cluster(streamer)));
            break;
        default:
            TRY_READ(streamer.read_unknown_element());
        }

        return {};
    });
}

DecoderErrorOr<NonnullOwnPtr<MatroskaDocument>> Reader::parse()
{
    auto first_element_id = TRY_READ(m_streamer.read_variable_size_integer(false));
    dbgln_if(MATROSKA_TRACE_DEBUG, "First element ID is {:#010x}\n", first_element_id);
    if (first_element_id != EBML_MASTER_ELEMENT_ID)
        return DecoderError::corrupted("First element was not an EBML header"sv);

    auto header = TRY(parse_ebml_header(m_streamer));
    dbgln_if(MATROSKA_DEBUG, "Parsed EBML header");

    auto root_element_id = TRY_READ(m_streamer.read_variable_size_integer(false));
    if (root_element_id != SEGMENT_ELEMENT_ID)
        return DecoderError::corrupted("Second element was not a segment element"sv);

    auto matroska_document = make<MatroskaDocument>(header);
    TRY(parse_segment_elements(m_streamer, *matroska_document));
    return matroska_document;
}

static DecoderErrorOr<NonnullOwnPtr<SegmentInformation>> parse_information(Streamer& streamer)
{
    auto segment_information = make<SegmentInformation>();
    TRY(parse_master_element(streamer, "Segment Information"sv, [&](u64 element_id) -> DecoderErrorOr<void> {
        switch (element_id) {
        case TIMESTAMP_SCALE_ID:
            segment_information->set_timestamp_scale(TRY_READ(streamer.read_u64()));
            dbgln_if(MATROSKA_DEBUG, "Read TimestampScale attribute: {}", segment_information->timestamp_scale());
            break;
        case MUXING_APP_ID:
            segment_information->set_muxing_app(TRY_READ(streamer.read_string()));
            dbgln_if(MATROSKA_DEBUG, "Read MuxingApp attribute: {}", segment_information->muxing_app().as_string());
            break;
        case WRITING_APP_ID:
            segment_information->set_writing_app(TRY_READ(streamer.read_string()));
            dbgln_if(MATROSKA_DEBUG, "Read WritingApp attribute: {}", segment_information->writing_app().as_string());
            break;
        case DURATION_ID:
            segment_information->set_duration(TRY_READ(streamer.read_float()));
            dbgln_if(MATROSKA_DEBUG, "Read Duration attribute: {}", segment_information->duration().value());
            break;
        default:
            TRY_READ(streamer.read_unknown_element());
        }

        return {};
    }));

    return segment_information;
}

static DecoderErrorOr<TrackEntry::ColorFormat> parse_video_color_information(Streamer& streamer)
{
    TrackEntry::ColorFormat color_format {};

    TRY(parse_master_element(streamer, "Colour"sv, [&](u64 element_id) -> DecoderErrorOr<void> {
        switch (element_id) {
        case PRIMARIES_ID:
            color_format.color_primaries = static_cast<ColorPrimaries>(TRY_READ(streamer.read_u64()));
            dbgln_if(MATROSKA_TRACE_DEBUG, "Read Colour's Primaries attribute: {}", color_primaries_to_string(color_format.color_primaries));
            break;
        case TRANSFER_CHARACTERISTICS_ID:
            color_format.transfer_characteristics = static_cast<TransferCharacteristics>(TRY_READ(streamer.read_u64()));
            dbgln_if(MATROSKA_TRACE_DEBUG, "Read Colour's TransferCharacteristics attribute: {}", transfer_characteristics_to_string(color_format.transfer_characteristics));
            break;
        case MATRIX_COEFFICIENTS_ID:
            color_format.matrix_coefficients = static_cast<MatrixCoefficients>(TRY_READ(streamer.read_u64()));
            dbgln_if(MATROSKA_TRACE_DEBUG, "Read Colour's MatrixCoefficients attribute: {}", matrix_coefficients_to_string(color_format.matrix_coefficients));
            break;
        case BITS_PER_CHANNEL_ID:
            color_format.bits_per_channel = TRY_READ(streamer.read_u64());
            dbgln_if(MATROSKA_TRACE_DEBUG, "Read Colour's BitsPerChannel attribute: {}", color_format.bits_per_channel);
            break;
        default:
            TRY_READ(streamer.read_unknown_element());
        }

        return {};
    }));

    return color_format;
}

static DecoderErrorOr<TrackEntry::VideoTrack> parse_video_track_information(Streamer& streamer)
{
    TrackEntry::VideoTrack video_track {};

    TRY(parse_master_element(streamer, "VideoTrack"sv, [&](u64 element_id) -> DecoderErrorOr<void> {
        switch (element_id) {
        case PIXEL_WIDTH_ID:
            video_track.pixel_width = TRY_READ(streamer.read_u64());
            dbgln_if(MATROSKA_TRACE_DEBUG, "Read VideoTrack's PixelWidth attribute: {}", video_track.pixel_width);
            break;
        case PIXEL_HEIGHT_ID:
            video_track.pixel_height = TRY_READ(streamer.read_u64());
            dbgln_if(MATROSKA_TRACE_DEBUG, "Read VideoTrack's PixelHeight attribute: {}", video_track.pixel_height);
            break;
        case COLOR_ENTRY_ID:
            video_track.color_format = TRY(parse_video_color_information(streamer));
            break;
        default:
            TRY_READ(streamer.read_unknown_element());
        }

        return {};
    }));

    return video_track;
}

static DecoderErrorOr<TrackEntry::AudioTrack> parse_audio_track_information(Streamer& streamer)
{
    TrackEntry::AudioTrack audio_track {};

    TRY(parse_master_element(streamer, "AudioTrack"sv, [&](u64 element_id) -> DecoderErrorOr<void> {
        switch (element_id) {
        case CHANNELS_ID:
            audio_track.channels = TRY_READ(streamer.read_u64());
            dbgln_if(MATROSKA_TRACE_DEBUG, "Read AudioTrack's Channels attribute: {}", audio_track.channels);
            break;
        case BIT_DEPTH_ID:
            audio_track.bit_depth = TRY_READ(streamer.read_u64());
            dbgln_if(MATROSKA_TRACE_DEBUG, "Read AudioTrack's BitDepth attribute: {}", audio_track.bit_depth);
            break;
        default:
            TRY_READ(streamer.read_unknown_element());
        }

        return {};
    }));

    return audio_track;
}

static DecoderErrorOr<NonnullOwnPtr<TrackEntry>> parse_track_entry(Streamer& streamer)
{
    auto track_entry = make<TrackEntry>();
    TRY(parse_master_element(streamer, "Track"sv, [&](u64 element_id) -> DecoderErrorOr<void> {
        switch (element_id) {
        case TRACK_NUMBER_ID:
            track_entry->set_track_number(TRY_READ(streamer.read_u64()));
            dbgln_if(MATROSKA_TRACE_DEBUG, "Read TrackNumber attribute: {}", track_entry->track_number());
            break;
        case TRACK_UID_ID:
            track_entry->set_track_uid(TRY_READ(streamer.read_u64()));
            dbgln_if(MATROSKA_TRACE_DEBUG, "Read TrackUID attribute: {}", track_entry->track_uid());
            break;
        case TRACK_TYPE_ID:
            track_entry->set_track_type(static_cast<TrackEntry::TrackType>(TRY_READ(streamer.read_u64())));
            dbgln_if(MATROSKA_TRACE_DEBUG, "Read TrackType attribute: {}", track_entry->track_type());
            break;
        case TRACK_LANGUAGE_ID:
            track_entry->set_language(TRY_READ(streamer.read_string()));
            dbgln_if(MATROSKA_TRACE_DEBUG, "Read Track's Language attribute: {}", track_entry->language());
            break;
        case TRACK_CODEC_ID:
            track_entry->set_codec_id(TRY_READ(streamer.read_string()));
            dbgln_if(MATROSKA_TRACE_DEBUG, "Read Track's CodecID attribute: {}", track_entry->codec_id());
            break;
        case TRACK_VIDEO_ID:
            track_entry->set_video_track(TRY(parse_video_track_information(streamer)));
            break;
        case TRACK_AUDIO_ID:
            track_entry->set_audio_track(TRY(parse_audio_track_information(streamer)));
            break;
        default:
            TRY_READ(streamer.read_unknown_element());
        }

        return {};
    }));

    return track_entry;
}

static DecoderErrorOr<void> parse_tracks(Streamer& streamer, MatroskaDocument& matroska_document)
{
    return parse_master_element(streamer, "Tracks"sv, [&](u64 element_id) -> DecoderErrorOr<void> {
        if (element_id == TRACK_ENTRY_ID) {
            dbgln_if(MATROSKA_DEBUG, "Parsing track");
            auto track_entry = TRY(parse_track_entry(streamer));
            auto track_number = track_entry->track_number();
            dbgln_if(MATROSKA_DEBUG, "Adding track {} to document", track_number);
            matroska_document.add_track(track_number, track_entry.release_nonnull<TrackEntry>());
        } else {
            TRY_READ(streamer.read_unknown_element());
        }

        return {};
    });
}

static DecoderErrorOr<NonnullOwnPtr<Block>> parse_simple_block(Streamer& streamer);

static DecoderErrorOr<NonnullOwnPtr<Cluster>> parse_cluster(Streamer& streamer)
{
    auto cluster = make<Cluster>();

    TRY(parse_master_element(streamer, "Cluster"sv, [&](u64 element_id) -> DecoderErrorOr<void> {
        switch (element_id) {
        case SIMPLE_BLOCK_ID:
            cluster->blocks().append(TRY(parse_simple_block(streamer)));
            break;
        case TIMESTAMP_ID:
            cluster->set_timestamp(TRY_READ(streamer.read_u64()));
            break;
        default:
            TRY_READ(streamer.read_unknown_element());
        }

        return {};
    }));

    return cluster;
}

static DecoderErrorOr<NonnullOwnPtr<Block>> parse_simple_block(Streamer& streamer)
{
    auto block = make<Block>();

    auto content_size = TRY_READ(streamer.read_variable_size_integer());

    auto octets_read_before_track_number = streamer.octets_read();
    auto track_number = TRY_READ(streamer.read_variable_size_integer());
    block->set_track_number(track_number);

    block->set_timestamp(TRY_READ(streamer.read_i16()));

    auto flags = TRY_READ(streamer.read_octet());
    block->set_only_keyframes((flags & (1u << 7u)) != 0);
    block->set_invisible((flags & (1u << 3u)) != 0);
    block->set_lacing(static_cast<Block::Lacing>((flags & 0b110u) >> 1u));
    block->set_discardable((flags & 1u) != 0);

    auto total_frame_content_size = content_size - (streamer.octets_read() - octets_read_before_track_number);
    if (block->lacing() == Block::Lacing::EBML) {
        auto octets_read_before_frame_sizes = streamer.octets_read();
        auto frame_count = TRY_READ(streamer.read_octet()) + 1;
        Vector<u64> frame_sizes;
        frame_sizes.ensure_capacity(frame_count);

        u64 frame_size_sum = 0;
        u64 previous_frame_size;
        auto first_frame_size = TRY_READ(streamer.read_variable_size_integer());
        frame_sizes.append(first_frame_size);
        frame_size_sum += first_frame_size;
        previous_frame_size = first_frame_size;

        for (int i = 0; i < frame_count - 2; i++) {
            auto frame_size_difference = TRY_READ(streamer.read_variable_size_signed_integer());
            u64 frame_size;
            // FIXME: x - (-y) == x + y??
            if (frame_size_difference < 0)
                frame_size = previous_frame_size - (-frame_size_difference);
            else
                frame_size = previous_frame_size + frame_size_difference;
            frame_sizes.append(frame_size);
            frame_size_sum += frame_size;
            previous_frame_size = frame_size;
        }
        frame_sizes.append(total_frame_content_size - frame_size_sum - (streamer.octets_read() - octets_read_before_frame_sizes));

        for (int i = 0; i < frame_count; i++) {
            // FIXME: ReadonlyBytes instead of copying the frame data?
            auto current_frame_size = frame_sizes.at(i);
            block->add_frame(DECODER_TRY_ALLOC(ByteBuffer::copy(streamer.data(), current_frame_size)));
            TRY_READ(streamer.drop_octets(current_frame_size));
        }
    } else if (block->lacing() == Block::Lacing::FixedSize) {
        auto frame_count = TRY_READ(streamer.read_octet()) + 1;
        auto individual_frame_size = total_frame_content_size / frame_count;
        for (int i = 0; i < frame_count; i++) {
            block->add_frame(DECODER_TRY_ALLOC(ByteBuffer::copy(streamer.data(), individual_frame_size)));
            TRY_READ(streamer.drop_octets(individual_frame_size));
        }
    } else {
        block->add_frame(DECODER_TRY_ALLOC(ByteBuffer::copy(streamer.data(), total_frame_content_size)));
        TRY_READ(streamer.drop_octets(total_frame_content_size));
    }
    return block;
}

ErrorOr<String> Streamer::read_string()
{
    auto string_length = TRY(read_variable_size_integer());
    if (remaining() < string_length)
        return Error::from_string_literal("String length extends past the end of the stream");
    auto string_value = String(data_as_chars(), string_length);
    TRY(drop_octets(string_length));
    return string_value;
}

ErrorOr<u8> Streamer::read_octet()
{
    if (!has_octet()) {
        dbgln_if(MATROSKA_TRACE_DEBUG, "Ran out of stream data");
        return Error::from_string_literal("Stream is out of data");
    }
    u8 byte = *data();
    m_octets_read.last()++;
    m_position++;
    return byte;
}

ErrorOr<i16> Streamer::read_i16()
{
    return (TRY(read_octet()) << 8) | TRY(read_octet());
}

ErrorOr<u64> Streamer::read_variable_size_integer(bool mask_length)
{
    dbgln_if(MATROSKA_TRACE_DEBUG, "Reading from offset {:p}", data());
    auto length_descriptor = TRY(read_octet());
    dbgln_if(MATROSKA_TRACE_DEBUG, "Reading VINT, first byte is {:#02x}", length_descriptor);
    if (length_descriptor == 0)
        return Error::from_string_literal("read_variable_size_integer: Length descriptor has no terminating set bit");
    size_t length = 0;
    while (length < 8) {
        if (((length_descriptor >> (8 - length)) & 1) == 1)
            break;
        length++;
    }
    dbgln_if(MATROSKA_TRACE_DEBUG, "Reading VINT of total length {}", length);
    if (length > 8)
        return Error::from_string_literal("read_variable_size_integer: Length is too large");

    u64 result;
    if (mask_length)
        result = length_descriptor & ~(1u << (8 - length));
    else
        result = length_descriptor;
    dbgln_if(MATROSKA_TRACE_DEBUG, "Beginning of VINT is {:#02x}", result);
    for (size_t i = 1; i < length; i++) {
        u8 next_octet = TRY(read_octet());
        dbgln_if(MATROSKA_TRACE_DEBUG, "Read octet of {:#02x}", next_octet);
        result = (result << 8u) | next_octet;
        dbgln_if(MATROSKA_TRACE_DEBUG, "New result is {:#010x}", result);
    }
    return result;
}

ErrorOr<i64> Streamer::read_variable_size_signed_integer()
{
    auto length_descriptor = TRY(read_octet());
    if (length_descriptor == 0)
        return Error::from_string_literal("read_variable_sized_signed_integer: Length descriptor has no terminating set bit");
    i64 length = 0;
    while (length < 8) {
        if (((length_descriptor >> (8 - length)) & 1) == 1)
            break;
        length++;
    }
    if (length > 8)
        return Error::from_string_literal("read_variable_size_integer: Length is too large");

    i64 result = length_descriptor & ~(1u << (8 - length));
    for (i64 i = 1; i < length; i++) {
        u8 next_octet = TRY(read_octet());
        result = (result << 8u) | next_octet;
    }
    result -= AK::exp2<i64>(length * 7 - 1) - 1;
    return result;
}

ErrorOr<void> Streamer::drop_octets(size_t num_octets)
{
    if (remaining() < num_octets)
        return Error::from_string_literal("Tried to drop octets past the end of the stream");
    m_position += num_octets;
    m_octets_read.last() += num_octets;
    return {};
}

ErrorOr<u64> Streamer::read_u64()
{
    auto integer_length = TRY(read_variable_size_integer());
    u64 result = 0;
    for (size_t i = 0; i < integer_length; i++) {
        result = (result << 8u) + TRY(read_octet());
    }
    return result;
}

ErrorOr<double> Streamer::read_float()
{
    auto length = TRY(read_variable_size_integer());
    if (length != 4u && length != 8u)
        return Error::from_string_literal("Float size must be 4 or 8 bytes");

    union {
        u64 value;
        float float_value;
        double double_value;
    } read_data;
    read_data.value = 0;
    for (size_t i = 0; i < length; i++) {
        read_data.value = (read_data.value << 8u) + TRY(read_octet());
    }
    if (length == 4u)
        return read_data.float_value;
    return read_data.double_value;
}

ErrorOr<void> Streamer::read_unknown_element()
{
    auto element_length = TRY(read_variable_size_integer());
    return drop_octets(element_length);
}

}
