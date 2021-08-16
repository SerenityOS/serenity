/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Buffer.h"
#include "FlacTypes.h"
#include "Loader.h"
#include <AK/BitStream.h>
#include <AK/Stream.h>
#include <AK/Types.h>
#include <AK/Variant.h>
#include <LibCore/FileStream.h>

namespace Audio {

class FlacInputStream : public Variant<Core::InputFileStream, InputMemoryStream> {

public:
    using Variant<Core::InputFileStream, InputMemoryStream>::Variant;

    bool seek(size_t pos)
    {
        return this->visit(
            [&](Core::InputFileStream& stream) {
                return stream.seek(pos);
            },
            [&](InputMemoryStream& stream) {
                if (pos >= stream.bytes().size()) {
                    return false;
                }
                stream.seek(pos);
                return true;
            });
    }

    bool handle_any_error()
    {
        return this->visit(
            [&](auto& stream) {
                return stream.handle_any_error();
            });
    }

    InputBitStream bit_stream()
    {
        return this->visit(
            [&](auto& stream) {
                return InputBitStream(stream);
            });
    }
};

ALWAYS_INLINE u8 frame_channel_type_to_channel_count(FlacFrameChannelType channel_type);
// Sign-extend an arbitrary-size signed number to 64 bit signed
ALWAYS_INLINE i64 sign_extend(u32 n, u8 size);
// Decodes the sign representation method used in Rice coding.
// Numbers alternate between positive and negative: 0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, ...
ALWAYS_INLINE i32 rice_to_signed(u32 x);

// decoders
// read a UTF-8 encoded number, even if it is not a valid codepoint
ALWAYS_INLINE u64 read_utf8_char(InputStream& input);
// decode a single number encoded with exponential golomb encoding of the specified order
ALWAYS_INLINE i32 decode_unsigned_exp_golomb(u8 order, InputBitStream& bit_input);

class FlacLoaderPlugin : public LoaderPlugin {
public:
    FlacLoaderPlugin(const StringView& path);
    FlacLoaderPlugin(const ByteBuffer& buffer);
    ~FlacLoaderPlugin()
    {
        if (m_stream)
            m_stream->handle_any_error();
    }

    virtual bool sniff() override;

    virtual bool has_error() override { return !m_error_string.is_null(); }
    virtual const String& error_string() override { return m_error_string; }

    virtual RefPtr<Buffer> get_more_samples(size_t max_bytes_to_read_from_input = 128 * KiB) override;

    virtual void reset() override;
    virtual void seek(const int position) override;

    virtual int loaded_samples() override { return m_loaded_samples; }
    virtual int total_samples() override { return m_total_samples; }
    virtual u32 sample_rate() override { return m_sample_rate; }
    virtual u16 num_channels() override { return m_num_channels; }
    virtual PcmSampleFormat pcm_format() override { return m_sample_format; }
    virtual RefPtr<Core::File> file() override { return m_file; }

    bool is_fixed_blocksize_stream() const { return m_min_block_size == m_max_block_size; }
    bool sample_count_unknown() const { return m_total_samples == 0; }

private:
    bool parse_header();
    // Either returns the metadata block or sets error message.
    // Additionally, increments m_data_start_location past the read meta block.
    FlacRawMetadataBlock next_meta_block(InputBitStream& bit_input);
    // Fetches and sets the next FLAC frame
    void next_frame();
    // Helper of next_frame that fetches a sub frame's header
    FlacSubframeHeader next_subframe_header(InputBitStream& bit_input, u8 channel_index);
    // Helper of next_frame that decompresses a subframe
    Vector<i32> parse_subframe(FlacSubframeHeader& subframe_header, InputBitStream& bit_input);
    // Subframe-internal data decoders (heavy lifting)
    Vector<i32> decode_fixed_lpc(FlacSubframeHeader& subframe, InputBitStream& bit_input);
    Vector<i32> decode_verbatim(FlacSubframeHeader& subframe, InputBitStream& bit_input);
    Vector<i32> decode_custom_lpc(FlacSubframeHeader& subframe, InputBitStream& bit_input);
    Vector<i32> decode_residual(Vector<i32>& decoded, FlacSubframeHeader& subframe, InputBitStream& bit_input);
    // decode a single rice partition that has its own rice parameter
    ALWAYS_INLINE Vector<i32> decode_rice_partition(u8 partition_type, u32 partitions, u32 partition_index, FlacSubframeHeader& subframe, InputBitStream& bit_input);

    // Converters for special coding used in frame headers
    ALWAYS_INLINE u32 convert_sample_count_code(u8 sample_count_code);
    ALWAYS_INLINE u32 convert_sample_rate_code(u8 sample_rate_code);
    ALWAYS_INLINE PcmSampleFormat convert_bit_depth_code(u8 bit_depth_code);

    bool m_valid { false };
    RefPtr<Core::File> m_file;
    String m_error_string;
    OwnPtr<ResampleHelper<i32>> m_resampler;

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
    OwnPtr<FlacInputStream> m_stream;
    Optional<FlacFrameHeader> m_current_frame;
    Vector<Frame> m_current_frame_data;
    u64 m_current_sample_or_frame { 0 };
};

}
