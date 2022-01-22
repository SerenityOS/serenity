/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Buffer.h"
#include "FlacTypes.h"
#include "Loader.h"
#include <AK/Error.h>
#include <AK/Span.h>
#include <AK/Types.h>
#include <LibCore/InputBitStream.h>
#include <LibCore/MemoryStream.h>
#include <LibCore/Stream.h>

namespace Audio {

using Core::Stream::BigEndianInputBitStream;

// Experimentally determined to be a decent buffer size on i686:
// 4K (the default) is slightly worse, and 64K is much worse.
// At sufficiently large buffer sizes, the advantage of infrequent read() calls is outweighed by the memmove() overhead.
// There was no intensive fine-tuning done to determine this value, so improvements may definitely be possible.
constexpr size_t FLAC_BUFFER_SIZE = 8 * KiB;

ALWAYS_INLINE u8 frame_channel_type_to_channel_count(FlacFrameChannelType channel_type);
// Sign-extend an arbitrary-size signed number to 64 bit signed
ALWAYS_INLINE i64 sign_extend(u32 n, u8 size);
// Decodes the sign representation method used in Rice coding.
// Numbers alternate between positive and negative: 0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, ...
ALWAYS_INLINE i32 rice_to_signed(u32 x);

// decoders
// read a UTF-8 encoded number, even if it is not a valid codepoint
ALWAYS_INLINE ErrorOr<u64> read_utf8_char(BigEndianInputBitStream& input);
// decode a single number encoded with exponential golomb encoding of the specified order
ALWAYS_INLINE ErrorOr<i32> decode_unsigned_exp_golomb(u8 order, BigEndianInputBitStream& bit_input);

class FlacLoaderPlugin : public LoaderPlugin {
public:
    explicit FlacLoaderPlugin(StringView path);
    explicit FlacLoaderPlugin(Bytes& buffer);
    ~FlacLoaderPlugin()
    {
    }

    virtual MaybeLoaderError initialize() override;

    virtual LoaderSamples get_more_samples(size_t max_bytes_to_read_from_input = 128 * KiB) override;

    virtual MaybeLoaderError reset() override;
    virtual MaybeLoaderError seek(const int position) override;

    virtual int loaded_samples() override { return static_cast<int>(m_loaded_samples); }
    virtual int total_samples() override { return static_cast<int>(m_total_samples); }
    virtual u32 sample_rate() override { return m_sample_rate; }
    virtual u16 num_channels() override { return m_num_channels; }
    virtual String format_name() override { return "FLAC (.flac)"; }
    virtual PcmSampleFormat pcm_format() override { return m_sample_format; }
    virtual RefPtr<Core::File> file() override { return m_file; }

    bool is_fixed_blocksize_stream() const { return m_min_block_size == m_max_block_size; }
    bool sample_count_unknown() const { return m_total_samples == 0; }

private:
    MaybeLoaderError parse_header();
    // Either returns the metadata block or sets error message.
    // Additionally, increments m_data_start_location past the read meta block.
    ErrorOr<FlacRawMetadataBlock, LoaderError> next_meta_block(BigEndianInputBitStream& bit_input);
    // Fetches and writes the next FLAC frame
    MaybeLoaderError next_frame(Span<Sample>);
    // Helper of next_frame that fetches a sub frame's header
    ErrorOr<FlacSubframeHeader, LoaderError> next_subframe_header(BigEndianInputBitStream& bit_input, u8 channel_index);
    // Helper of next_frame that decompresses a subframe
    ErrorOr<Vector<i32>, LoaderError> parse_subframe(FlacSubframeHeader& subframe_header, BigEndianInputBitStream& bit_input);
    // Subframe-internal data decoders (heavy lifting)
    ErrorOr<Vector<i32>, LoaderError> decode_fixed_lpc(FlacSubframeHeader& subframe, BigEndianInputBitStream& bit_input);
    ErrorOr<Vector<i32>, LoaderError> decode_verbatim(FlacSubframeHeader& subframe, BigEndianInputBitStream& bit_input);
    ErrorOr<Vector<i32>, LoaderError> decode_custom_lpc(FlacSubframeHeader& subframe, BigEndianInputBitStream& bit_input);
    MaybeLoaderError decode_residual(Vector<i32>& decoded, FlacSubframeHeader& subframe, BigEndianInputBitStream& bit_input);
    // decode a single rice partition that has its own rice parameter
    ALWAYS_INLINE ErrorOr<Vector<i32>, LoaderError> decode_rice_partition(u8 partition_type, u32 partitions, u32 partition_index, FlacSubframeHeader& subframe, BigEndianInputBitStream& bit_input);

    // Converters for special coding used in frame headers
    ALWAYS_INLINE ErrorOr<u32, LoaderError> convert_sample_count_code(u8 sample_count_code);
    ALWAYS_INLINE ErrorOr<u32, LoaderError> convert_sample_rate_code(u8 sample_rate_code);
    ALWAYS_INLINE ErrorOr<PcmSampleFormat, LoaderError> convert_bit_depth_code(u8 bit_depth_code);

    RefPtr<Core::File> m_file;
    Optional<LoaderError> m_error {};

    // Data obtained directly from the FLAC metadata: many values have specific bit counts
    u32 m_sample_rate { 0 };         // 20 bit
    u8 m_num_channels { 0 };         // 3 bit
    PcmSampleFormat m_sample_format; // 5 bits for the integer bit depth
    // Blocks are units of decoded audio data
    u16 m_min_block_size { 0 };
    u16 m_max_block_size { 0 };
    // Frames are units of encoded audio data, both of these are 24-bit
    u32 m_min_frame_size { 0 }; //24 bit
    u32 m_max_frame_size { 0 }; // 24 bit
    u64 m_total_samples { 0 };  // 36 bit
    u8 m_md5_checksum[128 / 8]; // 128 bit (!)
    size_t m_loaded_samples { 0 };

    // keep track of the start of the data in the FLAC stream to seek back more easily
    u64 m_data_start_location { 0 };
    OwnPtr<Core::Stream::SeekableStream> m_stream;
    Optional<FlacFrameHeader> m_current_frame;
    // Whatever the last get_more_samples() call couldn't return gets stored here.
    Vector<Sample, FLAC_BUFFER_SIZE> m_unread_data;
    u64 m_current_sample_or_frame { 0 };
};

}
