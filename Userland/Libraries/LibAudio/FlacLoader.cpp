/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FlacLoader.h"
#include "Buffer.h"
#include <AK/BitStream.h>
#include <AK/Debug.h>
#include <AK/FlyString.h>
#include <AK/Format.h>
#include <AK/Stream.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <LibCore/FileStream.h>
#include <math.h>

namespace Audio {

FlacLoaderPlugin::FlacLoaderPlugin(const StringView& path)
    : m_file(Core::File::construct(path))
{
    if (!m_file->open(Core::OpenMode::ReadOnly)) {
        m_error_string = String::formatted("Can't open file: {}", m_file->error_string());
        return;
    }

    m_valid = parse_header();
    if (!m_valid)
        return;

    m_stream = make<FlacInputStream>(Core::InputFileStream(*m_file));
    reset();

    m_resampler = make<ResampleHelper<double>>(m_sample_rate, 44100);
}

FlacLoaderPlugin::FlacLoaderPlugin(const ByteBuffer& buffer)
{
    m_stream = make<FlacInputStream>(InputMemoryStream(buffer));
    if (!m_stream) {
        m_error_string = String::formatted("Can't open memory stream");
        return;
    }

    m_valid = parse_header();
    if (!m_valid)
        return;
    reset();

    m_resampler = make<ResampleHelper<double>>(m_sample_rate, 44100);
}

bool FlacLoaderPlugin::sniff()
{
    return m_valid;
}

bool FlacLoaderPlugin::parse_header()
{
    Optional<Core::InputFileStream> fis;
    bool ok = true;

    InputBitStream bit_input = [&]() -> InputBitStream {
        if (m_file) {
            fis = Core::InputFileStream(*m_file);
            return InputBitStream(*fis);
        }
        return InputBitStream(m_stream->get<InputMemoryStream>());
    }();

#define CHECK_OK(msg)                                                      \
    do {                                                                   \
        if (!ok) {                                                         \
            m_error_string = String::formatted("Parsing failed: {}", msg); \
            return {};                                                     \
        }                                                                  \
    } while (0)

    // Magic number
    u32 flac = bit_input.read_bits_big_endian(32);
    m_data_start_location += 4;
    ok = ok && flac == 0x664C6143; // "flaC"
    CHECK_OK("FLAC magic number");

    // Receive the streaminfo block
    FlacRawMetadataBlock streaminfo = next_meta_block(bit_input);
    // next_meta_block sets the error string if something goes wrong
    ok = ok && m_error_string.is_empty();
    CHECK_OK(m_error_string);
    ok = ok && (streaminfo.type == FlacMetadataBlockType::STREAMINFO);
    CHECK_OK("First block type");
    InputMemoryStream streaminfo_data_memory(streaminfo.data.bytes());
    InputBitStream streaminfo_data(streaminfo_data_memory);

    // STREAMINFO block
    m_min_block_size = streaminfo_data.read_bits_big_endian(16);
    ok = ok && (m_min_block_size >= 16);
    CHECK_OK("Minimum block size");
    m_max_block_size = streaminfo_data.read_bits_big_endian(16);
    ok = ok && (m_max_block_size >= 16);
    CHECK_OK("Maximum block size");
    m_min_frame_size = streaminfo_data.read_bits_big_endian(24);
    m_max_frame_size = streaminfo_data.read_bits_big_endian(24);
    m_sample_rate = streaminfo_data.read_bits_big_endian(20);
    ok = ok && (m_sample_rate <= 655350);
    CHECK_OK("Sample rate");
    m_num_channels = streaminfo_data.read_bits_big_endian(3) + 1; // 0 ^= one channel

    u8 bits_per_sample = streaminfo_data.read_bits_big_endian(5) + 1;
    if (bits_per_sample == 8) {
        // FIXME: Signed/Unsigned issues?
        m_sample_format = PcmSampleFormat::Uint8;
    } else if (bits_per_sample == 16) {
        m_sample_format = PcmSampleFormat::Int16;
    } else if (bits_per_sample == 24) {
        m_sample_format = PcmSampleFormat::Int24;
    } else if (bits_per_sample == 32) {
        m_sample_format = PcmSampleFormat::Int32;
    } else {
        ok = false;
        CHECK_OK("Sample bit depth");
    }

    m_total_samples = streaminfo_data.read_bits_big_endian(36);
    // Parse checksum into a buffer first
    ByteBuffer md5_checksum = ByteBuffer::create_uninitialized(128 / 8);
    streaminfo_data.read(md5_checksum);
    md5_checksum.bytes().copy_to({ m_md5_checksum, sizeof(m_md5_checksum) });

    // Parse other blocks
    // TODO: For a simple first implementation, all other blocks are skipped as allowed by the FLAC specification.
    // Especially the SEEKTABLE block may become useful in a more sophisticated version.
    [[maybe_unused]] u16 meta_blocks_parsed = 1;
    [[maybe_unused]] u16 total_meta_blocks = meta_blocks_parsed;
    FlacRawMetadataBlock block = streaminfo;
    while (!block.is_last_block) {
        block = next_meta_block(bit_input);
        ++total_meta_blocks;
        ok = ok && m_error_string.is_empty();
        CHECK_OK(m_error_string);
    }

    if constexpr (AFLACLOADER_DEBUG) {
        // HACK: u128 should be able to format itself
        StringBuilder checksum_string;
        for (unsigned int i = 0; i < md5_checksum.size(); ++i) {
            checksum_string.appendff("{:0X}", md5_checksum[i]);
        }
        dbgln("Parsed FLAC header: blocksize {}-{}{}, framesize {}-{}, {}Hz, {}bit, {} channels, {} samples total ({:.2f}s), MD5 {}, data start at {:x} bytes, {} headers total (skipped {})", m_min_block_size, m_max_block_size, is_fixed_blocksize_stream() ? " (constant)" : "", m_min_frame_size, m_max_frame_size, m_sample_rate, pcm_bits_per_sample(m_sample_format), m_num_channels, m_total_samples, m_total_samples / static_cast<double>(m_sample_rate), checksum_string.to_string(), m_data_start_location, total_meta_blocks, total_meta_blocks - meta_blocks_parsed);
    }

    return true;
#undef CHECK_OK
}

FlacRawMetadataBlock FlacLoaderPlugin::next_meta_block(InputBitStream& bit_input)
{
#define CHECK_IO_ERROR()                    \
    do {                                    \
        if (bit_input.handle_any_error()) { \
            m_error_string = "Read error";  \
            return FlacRawMetadataBlock {}; \
        }                                   \
    } while (0)

    bool is_last_block = bit_input.read_bit_big_endian();
    CHECK_IO_ERROR();
    // The block type enum constants agree with the specification
    FlacMetadataBlockType type = (FlacMetadataBlockType)bit_input.read_bits_big_endian(7);
    CHECK_IO_ERROR();
    if (type == FlacMetadataBlockType::INVALID) {
        m_error_string = "Invalid metadata block";
        return FlacRawMetadataBlock {};
    }
    m_data_start_location += 1;

    u32 block_length = bit_input.read_bits_big_endian(24);
    m_data_start_location += 3;
    CHECK_IO_ERROR();
    ByteBuffer block_data = ByteBuffer::create_uninitialized(block_length);
    // Reads exactly the bytes necessary into the Bytes container
    bit_input.read(block_data);
    m_data_start_location += block_length;
    CHECK_IO_ERROR();
    return FlacRawMetadataBlock {
        is_last_block,
        type,
        block_length,
        block_data,
    };

#undef CHECK_IO_ERROR
}

void FlacLoaderPlugin::reset()
{
    seek(m_data_start_location);
    m_current_frame.clear();
}

void FlacLoaderPlugin::seek(const int position)
{
    m_stream->seek(position);
}

// TODO implement these
RefPtr<Buffer> FlacLoaderPlugin::get_more_samples([[maybe_unused]] size_t max_bytes_to_read_from_input)
{
    Vector<Frame> samples;
    size_t remaining_samples = max_bytes_to_read_from_input;
    while (remaining_samples > 0) {
        if (!m_current_frame.has_value()) {
            next_frame();
            if (!m_error_string.is_empty()) {
                dbgln("Frame parsing error: {}", m_error_string);
                return nullptr;
            }
            // HACK: Test the start of the next subframe
            // auto input = m_stream->bit_stream();
            // u64 next = input.read_bits_big_endian(64);
            // dbgln("After frame end: {}", next);
        }
        samples.append(m_current_frame_data.take_first());
        if (m_current_frame_data.size() == 0) {
            m_current_frame.clear();
        }
        --remaining_samples;
    }

    return Buffer::create_with_samples(move(samples));
}

void FlacLoaderPlugin::next_frame()
{
    bool ok = true;
    InputBitStream bit_stream = m_stream->bit_stream();
#define CHECK_OK(msg)                                                                                                      \
    do {                                                                                                                   \
        if (!ok) {                                                                                                         \
            m_error_string = String::formatted("Frame parsing failed: {}", msg);                                           \
            bit_stream.align_to_byte_boundary();                                                                           \
            dbgln_if(AFLACLOADER_DEBUG, "Crash in FLAC loader: next bytes are {:x}", bit_stream.read_bits_big_endian(32)); \
            return;                                                                                                        \
        }                                                                                                                  \
    } while (0)

#define CHECK_ERROR_STRING                                             \
    do {                                                               \
        if (!m_error_string.is_null() && !m_error_string.is_empty()) { \
            ok = false;                                                \
            CHECK_OK(m_error_string);                                  \
        }                                                              \
    } while (0)

    // TODO: Check the CRC-16 checksum (and others) by keeping track of read data

    // FLAC frame sync code starts header
    u16 sync_code = bit_stream.read_bits_big_endian(14);
    ok = ok && (sync_code == 0b11111111111110);
    CHECK_OK("Sync code");
    bool reserved_bit = bit_stream.read_bit_big_endian();
    ok = ok && (reserved_bit == 0);
    CHECK_OK("Reserved frame header bit");
    [[maybe_unused]] bool blocking_strategy = bit_stream.read_bit_big_endian();

    u32 sample_count = convert_sample_count_code(bit_stream.read_bits_big_endian(4));
    CHECK_ERROR_STRING;

    u32 frame_sample_rate = convert_sample_rate_code(bit_stream.read_bits_big_endian(4));
    CHECK_ERROR_STRING;

    u8 channel_type_num = bit_stream.read_bits_big_endian(4);
    if (channel_type_num >= 0b1011) {
        ok = false;
        CHECK_OK("Channel assignment");
    }
    FlacFrameChannelType channel_type = (FlacFrameChannelType)channel_type_num;

    PcmSampleFormat bit_depth = convert_bit_depth_code(bit_stream.read_bits_big_endian(3));
    CHECK_ERROR_STRING;

    reserved_bit = bit_stream.read_bit_big_endian();
    ok = ok && (reserved_bit == 0);
    CHECK_OK("Reserved frame header end bit");

    // FIXME: sample number can be 8-56 bits, frame number can be 8-48 bits
    m_current_sample_or_frame = read_utf8_char(bit_stream);

    // Conditional header variables
    if (sample_count == FLAC_BLOCKSIZE_AT_END_OF_HEADER_8) {
        sample_count = bit_stream.read_bits(8) + 1;
    } else if (sample_count == FLAC_BLOCKSIZE_AT_END_OF_HEADER_16) {
        sample_count = bit_stream.read_bits(16) + 1;
    }

    if (frame_sample_rate == FLAC_SAMPLERATE_AT_END_OF_HEADER_8) {
        frame_sample_rate = bit_stream.read_bits(8) * 1000;
    } else if (frame_sample_rate == FLAC_SAMPLERATE_AT_END_OF_HEADER_16) {
        frame_sample_rate = bit_stream.read_bits(16);
    } else if (frame_sample_rate == FLAC_SAMPLERATE_AT_END_OF_HEADER_16X10) {
        frame_sample_rate = bit_stream.read_bits(16) * 10;
    }

    // TODO: check header checksum, see above
    [[maybe_unused]] u8 checksum = bit_stream.read_bits(8);

    dbgln_if(AFLACLOADER_DEBUG, "Frame: {} samples, {}bit {}Hz, channeltype {:x}, {} number {}, header checksum {}", sample_count, pcm_bits_per_sample(bit_depth), frame_sample_rate, channel_type_num, blocking_strategy ? "sample" : "frame", m_current_sample_or_frame, checksum);

    m_current_frame = FlacFrameHeader {
        sample_count,
        frame_sample_rate,
        channel_type,
        bit_depth,
    };

    u8 subframe_count = frame_channel_type_to_channel_count(channel_type);
    Vector<Vector<i32>> current_subframes;
    current_subframes.ensure_capacity(subframe_count);

    for (u8 i = 0; i < subframe_count; ++i) {
        FlacSubframeHeader new_subframe = next_subframe_header(bit_stream, i);
        CHECK_ERROR_STRING;
        Vector<i32> subframe_samples = parse_subframe(new_subframe, bit_stream);
        // HACK: Test the start of the next subframe
        CHECK_ERROR_STRING;
        current_subframes.append(move(subframe_samples));
    }

    bit_stream.align_to_byte_boundary();

    // TODO: check checksum, see above
    [[maybe_unused]] u16 footer_checksum = bit_stream.read_bits_big_endian(16);

    Vector<i32> left, right;

    switch (channel_type) {
    case FlacFrameChannelType::Mono:
        left = right = current_subframes[0];
        break;
    case FlacFrameChannelType::Stereo:
    // TODO mix together surround channels on each side?
    case FlacFrameChannelType::StereoCenter:
    case FlacFrameChannelType::Surround4p0:
    case FlacFrameChannelType::Surround5p0:
    case FlacFrameChannelType::Surround5p1:
    case FlacFrameChannelType::Surround6p1:
    case FlacFrameChannelType::Surround7p1:
        left = current_subframes[0];
        right = current_subframes[1];
        break;
    case FlacFrameChannelType::LeftSideStereo:
        // channels are left (0) and side (1)
        left = current_subframes[0];
        right.ensure_capacity(left.size());
        for (size_t i = 0; i < left.size(); ++i) {
            // right = left - side
            right.unchecked_append(left[i] - current_subframes[1][i]);
        }
        break;
    case FlacFrameChannelType::RightSideStereo:
        // channels are side (0) and right (1)
        right = current_subframes[1];
        left.ensure_capacity(right.size());
        for (size_t i = 0; i < right.size(); ++i) {
            // left = right + side
            left.unchecked_append(right[i] + current_subframes[0][i]);
        }
        break;
    case FlacFrameChannelType::MidSideStereo:
        // channels are mid (0) and side (1)
        left.ensure_capacity(current_subframes[0].size());
        right.ensure_capacity(current_subframes[0].size());
        for (size_t i = 0; i < current_subframes[0].size(); ++i) {
            i64 mid = current_subframes[0][i];
            i64 side = current_subframes[1][i];
            mid *= 2;
            // prevent integer division errors
            left.unchecked_append(static_cast<i32>((mid + side) / 2));
            right.unchecked_append(static_cast<i32>((mid - side) / 2));
        }
        break;
    }

    VERIFY(left.size() == right.size());

    // TODO: find the correct rescale offset
    double sample_rescale = static_cast<double>(1 << pcm_bits_per_sample(m_current_frame->bit_depth));
    dbgln_if(AFLACLOADER_DEBUG, "Sample rescaled from {} bits: factor {:.1f}", pcm_bits_per_sample(m_current_frame->bit_depth), sample_rescale);

    m_current_frame_data.clear_with_capacity();
    m_current_frame_data.ensure_capacity(left.size());
    // zip together channels
    for (size_t i = 0; i < left.size(); ++i) {
        Frame frame = { left[i] / sample_rescale, right[i] / sample_rescale };
        m_current_frame_data.unchecked_append(frame);
    }

#undef CHECK_OK
#undef CHECK_ERROR_STRING
}

u32 FlacLoaderPlugin::convert_sample_count_code(u8 sample_count_code)
{
    // single codes
    switch (sample_count_code) {
    case 0:
        m_error_string = "Reserved block size";
        return 0;
    case 1:
        return 192;
    case 6:
        return FLAC_BLOCKSIZE_AT_END_OF_HEADER_8;
    case 7:
        return FLAC_BLOCKSIZE_AT_END_OF_HEADER_16;
    }
    if (sample_count_code >= 2 && sample_count_code <= 5) {
        return 576 * pow(2, (sample_count_code - 2));
    }
    return 256 * pow(2, (sample_count_code - 8));
}

u32 FlacLoaderPlugin::convert_sample_rate_code(u8 sample_rate_code)
{
    switch (sample_rate_code) {
    case 0:
        return m_sample_rate;
    case 1:
        return 88200;
    case 2:
        return 176400;
    case 3:
        return 192000;
    case 4:
        return 8000;
    case 5:
        return 16000;
    case 6:
        return 22050;
    case 7:
        return 24000;
    case 8:
        return 32000;
    case 9:
        return 44100;
    case 10:
        return 48000;
    case 11:
        return 96000;
    case 12:
        return FLAC_SAMPLERATE_AT_END_OF_HEADER_8;
    case 13:
        return FLAC_SAMPLERATE_AT_END_OF_HEADER_16;
    case 14:
        return FLAC_SAMPLERATE_AT_END_OF_HEADER_16X10;
    default:
        m_error_string = "Invalid sample rate code";
        return 0;
    }
}

PcmSampleFormat FlacLoaderPlugin::convert_bit_depth_code(u8 bit_depth_code)
{
    switch (bit_depth_code) {
    case 0:
        return m_sample_format;
    case 1:
        return PcmSampleFormat::Uint8;
    case 4:
        return PcmSampleFormat::Int16;
    case 6:
        return PcmSampleFormat::Int24;
    case 3:
    case 7:
        m_error_string = "Reserved sample size";
        return PcmSampleFormat::Float64;
    default:
        m_error_string = String::formatted("Unsupported sample size {}", bit_depth_code);
        return PcmSampleFormat::Float64;
    }
}

u8 frame_channel_type_to_channel_count(FlacFrameChannelType channel_type)
{
    if (channel_type <= 7)
        return channel_type + 1;
    return 2;
}

FlacSubframeHeader FlacLoaderPlugin::next_subframe_header(InputBitStream& bit_stream, u8 channel_index)
{
    u8 bits_per_sample = pcm_bits_per_sample(m_current_frame->bit_depth);

    // For inter-channel correlation, the side channel needs an extra bit for its samples
    switch (m_current_frame->channels) {
    case LeftSideStereo:
    case MidSideStereo:
        if (channel_index == 1) {
            ++bits_per_sample;
        }
        break;
    case RightSideStereo:
        if (channel_index == 0) {
            ++bits_per_sample;
        }
        break;
    // "normal" channel types
    default:
        break;
    }

    // zero-bit padding
    bit_stream.read_bit_big_endian();

    // subframe type (encoding)
    u8 subframe_code = bit_stream.read_bits_big_endian(6);
    if ((subframe_code >= 0b000010 && subframe_code <= 0b000111) || (subframe_code > 0b001100 && subframe_code < 0b100000)) {
        m_error_string = "Subframe type";
        return {};
    }

    FlacSubframeType subframe_type;
    u8 order = 0;
    //LPC has the highest bit set
    if ((subframe_code & 0b100000) > 0) {
        subframe_type = FlacSubframeType::LPC;
        order = (subframe_code & 0b011111) + 1;
    } else if ((subframe_code & 0b001000) > 0) {
        // Fixed has the third-highest bit set
        subframe_type = FlacSubframeType::Fixed;
        order = (subframe_code & 0b000111);
    } else {
        subframe_type = (FlacSubframeType)subframe_code;
    }

    // wasted bits per sample (unary encoding)
    bool has_wasted_bits = bit_stream.read_bit_big_endian();
    u8 k = 0;
    if (has_wasted_bits) {
        bool current_k_bit = 0;
        u8 k = 0;
        do {
            current_k_bit = bit_stream.read_bit_big_endian();
            ++k;
        } while (current_k_bit != 1);
    }

    return FlacSubframeHeader {
        subframe_type,
        order,
        k,
        bits_per_sample
    };
}

Vector<i32> FlacLoaderPlugin::parse_subframe(FlacSubframeHeader& subframe_header, InputBitStream& bit_input)
{
    Vector<i32> samples;

    switch (subframe_header.type) {
    case FlacSubframeType::Constant: {
        u64 constant_value = bit_input.read_bits_big_endian(subframe_header.bits_per_sample - subframe_header.wasted_bits_per_sample);
        dbgln_if(AFLACLOADER_DEBUG, "Constant subframe: {}", constant_value);

        samples.ensure_capacity(m_current_frame->sample_count);
        for (u32 i = 0; i < m_current_frame->sample_count; ++i) {
            samples.unchecked_append(constant_value);
        }
        break;
    }
    case FlacSubframeType::Fixed: {
        dbgln_if(AFLACLOADER_DEBUG, "Fixed LPC subframe order {}", subframe_header.order);
        samples = decode_fixed_lpc(subframe_header, bit_input);
        break;
    }
    case FlacSubframeType::Verbatim: {
        dbgln_if(AFLACLOADER_DEBUG, "Verbatim subframe");
        samples = decode_verbatim(subframe_header, bit_input);
        break;
    }
    case FlacSubframeType::LPC: {
        dbgln_if(AFLACLOADER_DEBUG, "Custom LPC subframe order {}", subframe_header.order);
        samples = decode_custom_lpc(subframe_header, bit_input);
        break;
    }
    default:
        m_error_string = "Unhandled FLAC subframe type";
        return {};
    }
    if (!m_error_string.is_empty()) {
        return {};
    }

    for (size_t i = 0; i < samples.size(); ++i) {
        samples[i] <<= subframe_header.wasted_bits_per_sample;
    }

    ResampleHelper<i32> resampler(m_current_frame->sample_rate, m_sample_rate);
    return resampler.resample(samples);
}

// Decode a subframe that isn't actually encoded
Vector<i32> FlacLoaderPlugin::decode_verbatim([[maybe_unused]] FlacSubframeHeader& subframe, [[maybe_unused]] InputBitStream& bit_input)
{
    TODO();
}

// Decode a subframe encoded with a custom linear predictor coding, i.e. the subframe provides the polynomial order and coefficients
Vector<i32> FlacLoaderPlugin::decode_custom_lpc(FlacSubframeHeader& subframe, InputBitStream& bit_input)
{
    Vector<i32> decoded;
    decoded.ensure_capacity(m_current_frame->sample_count);

    // warm-up samples
    for (auto i = 0; i < subframe.order; ++i) {
        decoded.unchecked_append(sign_extend(bit_input.read_bits_big_endian(subframe.bits_per_sample - subframe.wasted_bits_per_sample), subframe.bits_per_sample));
        decoded[i] <<= subframe.wasted_bits_per_sample;
    }

    // precision of the coefficients
    u8 lpc_precision = bit_input.read_bits_big_endian(4);
    if (lpc_precision == 0b1111) {
        m_error_string = "Invalid linear predictor coefficient precision";
        return {};
    }
    lpc_precision += 1;

    // shift needed on the data (signed!)
    i8 lpc_shift = sign_extend(bit_input.read_bits_big_endian(5), 5);

    Vector<i32> coefficients;
    coefficients.ensure_capacity(subframe.order);
    // read coefficients
    for (auto i = 0; i < subframe.order; ++i) {
        u32 raw_coefficient = bit_input.read_bits_big_endian(lpc_precision);
        i32 coefficient = sign_extend(raw_coefficient, lpc_precision);
        coefficients.unchecked_append(coefficient);
    }

    dbgln_if(AFLACLOADER_DEBUG, "{}-bit {} shift coefficients: {}", lpc_precision, lpc_shift, coefficients);

    // decode residual
    // FIXME: This order may be incorrect, the LPC is applied to the residual, probably leading to incorrect results.
    decoded = decode_residual(decoded, subframe, bit_input);

    // approximate the waveform with the predictor
    for (size_t i = subframe.order; i < m_current_frame->sample_count; ++i) {
        i32 sample = 0;
        for (size_t t = 0; t < subframe.order; ++t) {
            sample += coefficients[t] * decoded[i - t - 1];
        }
        decoded[i] += sample >> lpc_shift;
    }

    return decoded;
}

// Decode a subframe encoded with one of the fixed linear predictor codings
Vector<i32> FlacLoaderPlugin::decode_fixed_lpc(FlacSubframeHeader& subframe, InputBitStream& bit_input)
{
    Vector<i32> decoded;
    decoded.ensure_capacity(m_current_frame->sample_count);

    // warm-up samples
    for (auto i = 0; i < subframe.order; ++i) {
        decoded.unchecked_append(bit_input.read_bits_big_endian(subframe.bits_per_sample - subframe.wasted_bits_per_sample));
    }

    decode_residual(decoded, subframe, bit_input);
    if (!m_error_string.is_empty())
        return {};
    dbgln_if(AFLACLOADER_DEBUG, "decoded length {}, {} order predictor", decoded.size(), subframe.order);

    switch (subframe.order) {
    case 0:
        // s_0(t) = 0
        for (u32 i = subframe.order; i < m_current_frame->sample_count; ++i)
            decoded[i] += 0;
        break;
    case 1:
        // s_1(t) = s(t-1)
        for (u32 i = subframe.order; i < m_current_frame->sample_count; ++i)
            decoded[i] += decoded[i - 1];
        break;
    case 2:
        // s_2(t) = 2s(t-1) - s(t-2)
        for (u32 i = subframe.order; i < m_current_frame->sample_count; ++i)
            decoded[i] += 2 * decoded[i - 1] - decoded[i - 2];
        break;
    case 3:
        // s_3(t) = 3s(t-1) - 3s(t-2) + s(t-3)
        for (u32 i = subframe.order; i < m_current_frame->sample_count; ++i)
            decoded[i] += 3 * decoded[i - 1] - 3 * decoded[i - 2] + decoded[i - 3];
        break;
    case 4:
        // s_4(t) = 4s(t-1) - 6s(t-2) + 4s(t-3) - s(t-4)
        for (u32 i = subframe.order; i < m_current_frame->sample_count; ++i)
            decoded[i] += 4 * decoded[i - 1] - 6 * decoded[i - 2] + 4 * decoded[i - 3] - decoded[i - 4];
        break;
    default:
        m_error_string = String::formatted("Unrecognized predictor order {}", subframe.order);
        break;
    }
    return decoded;
}

// Decode the residual, the "error" between the function approximation and the actual audio data
Vector<i32> FlacLoaderPlugin::decode_residual(Vector<i32>& decoded, FlacSubframeHeader& subframe, InputBitStream& bit_input)
{
    u8 residual_mode = bit_input.read_bits_big_endian(2);
    u8 partition_order = bit_input.read_bits_big_endian(4);
    u32 partitions = 1 << partition_order;

    if (residual_mode == FlacResidualMode::Rice4Bit) {
        // decode a single Rice partition with four bits for the order k
        for (u32 i = 0; i < partitions; ++i) {
            auto rice_partition = decode_rice_partition(4, partitions, i, subframe, bit_input);
            decoded.extend(move(rice_partition));
        }
    } else if (residual_mode == FlacResidualMode::Rice5Bit) {
        // five bits equivalent
        for (u32 i = 0; i < partitions; ++i) {
            auto rice_partition = decode_rice_partition(5, partitions, i, subframe, bit_input);
            decoded.extend(move(rice_partition));
        }
    } else {
        m_error_string = "Reserved residual coding method";
        return {};
    }

    return decoded;
}

// Decode a single Rice partition as part of the residual, every partition can have its own Rice parameter k
ALWAYS_INLINE Vector<i32> FlacLoaderPlugin::decode_rice_partition(u8 partition_type, u32 partitions, u32 partition_index, FlacSubframeHeader& subframe, InputBitStream& bit_input)
{
    // Rice parameter / Exp-Golomb order
    u8 k = bit_input.read_bits_big_endian(partition_type);

    u32 residual_sample_count;
    if (partitions == 0)
        residual_sample_count = m_current_frame->sample_count - subframe.order;
    else
        residual_sample_count = m_current_frame->sample_count / partitions;
    if (partition_index == 0)
        residual_sample_count -= subframe.order;

    Vector<i32> rice_partition;
    rice_partition.resize(residual_sample_count);

    // escape code for unencoded binary partition
    if (k == (1 << partition_type) - 1) {
        u8 unencoded_bps = bit_input.read_bits_big_endian(5);
        for (u32 r = 0; r < residual_sample_count; ++r) {
            rice_partition[r] = bit_input.read_bits_big_endian(unencoded_bps);
        }
    } else {
        for (u32 r = 0; r < residual_sample_count; ++r) {
            rice_partition[r] = decode_unsigned_exp_golomb(k, bit_input);
        }
    }

    return rice_partition;
}

// Decode a single number encoded with Rice/Exponential-Golomb encoding (the unsigned variant)
ALWAYS_INLINE i32 decode_unsigned_exp_golomb(u8 k, InputBitStream& bit_input)
{
    u8 q = 0;
    while (bit_input.read_bit_big_endian() == 0)
        ++q;

    // least significant bits (remainder)
    u32 rem = bit_input.read_bits_big_endian(k);
    u32 value = (u32)(q << k | rem);

    return rice_to_signed(value);
}

u64 read_utf8_char(InputStream& input)
{
    u64 character;
    ByteBuffer single_byte_buffer = ByteBuffer::create_uninitialized(1);
    input.read(single_byte_buffer);
    u8 start_byte = single_byte_buffer[0];
    // Signal byte is zero: ASCII character
    if ((start_byte & 0b10000000) == 0) {
        return start_byte;
    } else if ((start_byte & 0b11000000) == 0b10000000) {
        // illegal continuation byte
        return 0;
    }
    // This algorithm is too good and supports the theoretical max 0xFF start byte
    u8 length = 1;
    while (((start_byte << length) & 0b10000000) == 0b10000000)
        ++length;
    u8 bits_from_start_byte = 8 - (length + 1);
    u8 start_byte_bitmask = pow(2, bits_from_start_byte) - 1;
    character = start_byte_bitmask & start_byte;
    for (u8 i = length; i > 0; --i) {
        input.read(single_byte_buffer);
        u8 current_byte = single_byte_buffer[0];
        character = (character << 6) | (current_byte & 0b00111111);
    }
    return character;
}

i64 sign_extend(u32 n, u8 size)
{
    // negative
    if ((n & (1 << (size - 1))) > 0) {
        return static_cast<i64>(n | (0xffffffff << size));
    }
    // positive
    return n;
}

i32 rice_to_signed(u32 x)
{
    // positive numbers are even, negative numbers are odd
    // bitmask for conditionally inverting the entire number, thereby "negating" it
    i32 sign = -(x & 1);
    // copies the sign's sign onto the actual magnitude of x
    return (i32)(sign ^ (x >> 1));
}

}
