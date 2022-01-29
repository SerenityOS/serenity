/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/FixedArray.h>
#include <AK/FlyString.h>
#include <AK/Format.h>
#include <AK/IntegralMath.h>
#include <AK/Math.h>
#include <AK/ScopeGuard.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Try.h>
#include <AK/TypedTransfer.h>
#include <AK/UFixedBigInt.h>
#include <LibAudio/Buffer.h>
#include <LibAudio/FlacLoader.h>
#include <LibAudio/FlacTypes.h>
#include <LibAudio/LoaderError.h>
#include <LibCore/MemoryStream.h>
#include <LibCore/Stream.h>

namespace Audio {

FlacLoaderPlugin::FlacLoaderPlugin(StringView path)
    : m_file(Core::File::construct(path))
{
    if (!m_file->open(Core::OpenMode::ReadOnly)) {
        m_error = LoaderError { String::formatted("Can't open file: {}", m_file->error_string()) };
        return;
    }

    auto maybe_stream = Core::Stream::BufferedFile::create(MUST(Core::Stream::File::open(path, Core::Stream::OpenMode::Read)), FLAC_BUFFER_SIZE);
    if (maybe_stream.is_error())
        m_error = LoaderError { "Can't open file stream" };
    else
        m_stream = maybe_stream.release_value();
}

FlacLoaderPlugin::FlacLoaderPlugin(Bytes& buffer)
{
    auto maybe_stream = Core::Stream::MemoryStream::construct(buffer);
    if (maybe_stream.is_error())
        m_error = LoaderError { "Can't open memory stream" };
    else
        m_stream = maybe_stream.release_value();
}

MaybeLoaderError FlacLoaderPlugin::initialize()
{
    if (m_error.has_value())
        return m_error.release_value();

    TRY(parse_header());
    TRY(reset());
    return {};
}

MaybeLoaderError FlacLoaderPlugin::parse_header()
{
    auto bit_input = LOADER_TRY(BigEndianInputBitStream::construct(*m_stream));

    // A mixture of VERIFY and the non-crashing TRY().
#define FLAC_VERIFY(check, category, msg)                                                                                           \
    do {                                                                                                                            \
        if (!(check)) {                                                                                                             \
            return LoaderError { category, static_cast<size_t>(m_data_start_location), String::formatted("FLAC header: {}", msg) }; \
        }                                                                                                                           \
    } while (0)

    // Magic number
    u32 flac = LOADER_TRY(bit_input->read_bits<u32>(32));
    m_data_start_location += 4;
    FLAC_VERIFY(flac == 0x664C6143, LoaderError::Category::Format, "Magic number must be 'flaC'"); // "flaC"

    // Receive the streaminfo block
    auto streaminfo = TRY(next_meta_block(*bit_input));
    FLAC_VERIFY(streaminfo.type == FlacMetadataBlockType::STREAMINFO, LoaderError::Category::Format, "First block must be STREAMINFO");
    auto streaminfo_data_memory = LOADER_TRY(Core::Stream::MemoryStream::construct(streaminfo.data.bytes()));
    auto streaminfo_data = LOADER_TRY(BigEndianInputBitStream::construct(*streaminfo_data_memory));

    // STREAMINFO block
    m_min_block_size = LOADER_TRY(streaminfo_data->read_bits<u16>(16));
    FLAC_VERIFY(m_min_block_size >= 16, LoaderError::Category::Format, "Minimum block size must be 16");
    m_max_block_size = LOADER_TRY(streaminfo_data->read_bits<u16>(16));
    FLAC_VERIFY(m_max_block_size >= 16, LoaderError::Category::Format, "Maximum block size");
    m_min_frame_size = LOADER_TRY(streaminfo_data->read_bits<u32>(24));
    m_max_frame_size = LOADER_TRY(streaminfo_data->read_bits<u32>(24));
    m_sample_rate = LOADER_TRY(streaminfo_data->read_bits<u32>(20));
    FLAC_VERIFY(m_sample_rate <= 655350, LoaderError::Category::Format, "Sample rate");
    m_num_channels = LOADER_TRY(streaminfo_data->read_bits<u8>(3)) + 1; // 0 = one channel

    u8 bits_per_sample = LOADER_TRY(streaminfo_data->read_bits<u8>(5)) + 1;
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
        FLAC_VERIFY(false, LoaderError::Category::Format, "Sample bit depth invalid");
    }

    m_total_samples = LOADER_TRY(streaminfo_data->read_bits<u64>(36));
    FLAC_VERIFY(m_total_samples > 0, LoaderError::Category::Format, "Number of samples is zero");
    // Parse checksum into a buffer first
    [[maybe_unused]] u128 md5_checksum;
    VERIFY(streaminfo_data->is_aligned_to_byte_boundary());
    auto md5_bytes_read = LOADER_TRY(streaminfo_data->read(md5_checksum.bytes()));
    FLAC_VERIFY(md5_bytes_read == md5_checksum.my_size(), LoaderError::Category::IO, "MD5 Checksum size");
    md5_checksum.bytes().copy_to({ m_md5_checksum, sizeof(m_md5_checksum) });

    // Parse other blocks
    // TODO: For a simple first implementation, all other blocks are skipped as allowed by the FLAC specification.
    // Especially the SEEKTABLE block may become useful in a more sophisticated version.
    [[maybe_unused]] u16 meta_blocks_parsed = 1;
    [[maybe_unused]] u16 total_meta_blocks = meta_blocks_parsed;
    FlacRawMetadataBlock block = streaminfo;
    while (!block.is_last_block) {
        block = TRY(next_meta_block(*bit_input));
        ++total_meta_blocks;
    }

    dbgln_if(AFLACLOADER_DEBUG, "Parsed FLAC header: blocksize {}-{}{}, framesize {}-{}, {}Hz, {}bit, {} channels, {} samples total ({:.2f}s), MD5 {}, data start at {:x} bytes, {} headers total (skipped {})", m_min_block_size, m_max_block_size, is_fixed_blocksize_stream() ? " (constant)" : "", m_min_frame_size, m_max_frame_size, m_sample_rate, pcm_bits_per_sample(m_sample_format), m_num_channels, m_total_samples, static_cast<double>(m_total_samples) / static_cast<double>(m_sample_rate), md5_checksum, m_data_start_location, total_meta_blocks, total_meta_blocks - meta_blocks_parsed);

    return {};
}

ErrorOr<FlacRawMetadataBlock, LoaderError> FlacLoaderPlugin::next_meta_block(BigEndianInputBitStream& bit_input)
{

    bool is_last_block = LOADER_TRY(bit_input.read_bit());
    // The block type enum constants agree with the specification
    FlacMetadataBlockType type = (FlacMetadataBlockType)LOADER_TRY(bit_input.read_bits<u8>(7));
    m_data_start_location += 1;
    FLAC_VERIFY(type != FlacMetadataBlockType::INVALID, LoaderError::Category::Format, "Invalid metadata block");

    u32 block_length = LOADER_TRY(bit_input.read_bits<u32>(24));
    m_data_start_location += 3;
    auto block_data_result = ByteBuffer::create_uninitialized(block_length);
    FLAC_VERIFY(!block_data_result.is_error(), LoaderError::Category::IO, "Out of memory");
    auto block_data = block_data_result.release_value();
    // Reads exactly the bytes necessary into the Bytes container
    LOADER_TRY(bit_input.read(block_data));
    m_data_start_location += block_length;
    return FlacRawMetadataBlock {
        is_last_block,
        type,
        block_length,
        block_data,
    };
}
#undef FLAC_VERIFY

MaybeLoaderError FlacLoaderPlugin::reset()
{
    TRY(seek(m_data_start_location));
    m_current_frame.clear();
    return {};
}

MaybeLoaderError FlacLoaderPlugin::seek(const int position)
{
    if (m_stream->seek(position, Core::Stream::SeekMode::SetPosition).is_error())
        return LoaderError { LoaderError::IO, m_loaded_samples, String::formatted("Invalid seek position {}", position) };
    return {};
}

LoaderSamples FlacLoaderPlugin::get_more_samples(size_t max_bytes_to_read_from_input)
{
    ssize_t remaining_samples = static_cast<ssize_t>(m_total_samples - m_loaded_samples);
    if (remaining_samples <= 0)
        return Buffer::create_empty();

    size_t samples_to_read = min(max_bytes_to_read_from_input, remaining_samples);
    auto samples = FixedArray<Sample>::must_create_but_fixme_should_propagate_errors(samples_to_read);
    size_t sample_index = 0;

    if (m_unread_data.size() > 0) {
        size_t to_transfer = min(m_unread_data.size(), samples_to_read);
        dbgln_if(AFLACLOADER_DEBUG, "Reading {} samples from unread sample buffer (size {})", to_transfer, m_unread_data.size());
        AK::TypedTransfer<Sample>::move(samples.data(), m_unread_data.data(), to_transfer);
        if (to_transfer < m_unread_data.size())
            m_unread_data.remove(0, to_transfer);
        else
            m_unread_data.clear_with_capacity();

        sample_index += to_transfer;
    }

    while (sample_index < samples_to_read) {
        TRY(next_frame(samples.span().slice(sample_index)));
        sample_index += m_current_frame->sample_count;
    }

    m_loaded_samples += sample_index;
    auto maybe_buffer = Buffer::create_with_samples(move(samples));
    if (maybe_buffer.is_error())
        return LoaderError { LoaderError::Category::Internal, m_loaded_samples, "Couldn't allocate sample buffer" };
    return maybe_buffer.release_value();
}

MaybeLoaderError FlacLoaderPlugin::next_frame(Span<Sample> target_vector)
{
#define FLAC_VERIFY(check, category, msg)                                                                                               \
    do {                                                                                                                                \
        if (!(check)) {                                                                                                                 \
            return LoaderError { category, static_cast<size_t>(m_current_sample_or_frame), String::formatted("FLAC header: {}", msg) }; \
        }                                                                                                                               \
    } while (0)

    auto bit_stream = LOADER_TRY(BigEndianInputBitStream::construct(*m_stream));

    // TODO: Check the CRC-16 checksum (and others) by keeping track of read data

    // FLAC frame sync code starts header
    u16 sync_code = LOADER_TRY(bit_stream->read_bits<u16>(14));
    FLAC_VERIFY(sync_code == 0b11111111111110, LoaderError::Category::Format, "Sync code");
    bool reserved_bit = LOADER_TRY(bit_stream->read_bit());
    FLAC_VERIFY(reserved_bit == 0, LoaderError::Category::Format, "Reserved frame header bit");
    [[maybe_unused]] bool blocking_strategy = LOADER_TRY(bit_stream->read_bit());

    u32 sample_count = TRY(convert_sample_count_code(LOADER_TRY(bit_stream->read_bits<u8>(4))));

    u32 frame_sample_rate = TRY(convert_sample_rate_code(LOADER_TRY(bit_stream->read_bits<u8>(4))));

    u8 channel_type_num = LOADER_TRY(bit_stream->read_bits<u8>(4));
    FLAC_VERIFY(channel_type_num < 0b1011, LoaderError::Format, "Channel assignment");
    FlacFrameChannelType channel_type = (FlacFrameChannelType)channel_type_num;

    PcmSampleFormat bit_depth = TRY(convert_bit_depth_code(LOADER_TRY(bit_stream->read_bits<u8>(3))));

    reserved_bit = LOADER_TRY(bit_stream->read_bit());
    FLAC_VERIFY(reserved_bit == 0, LoaderError::Category::Format, "Reserved frame header end bit");

    // FIXME: sample number can be 8-56 bits, frame number can be 8-48 bits
    m_current_sample_or_frame = LOADER_TRY(read_utf8_char(*bit_stream));

    // Conditional header variables
    if (sample_count == FLAC_BLOCKSIZE_AT_END_OF_HEADER_8) {
        sample_count = LOADER_TRY(bit_stream->read_bits<u32>(8)) + 1;
    } else if (sample_count == FLAC_BLOCKSIZE_AT_END_OF_HEADER_16) {
        sample_count = LOADER_TRY(bit_stream->read_bits<u32>(16)) + 1;
    }

    if (frame_sample_rate == FLAC_SAMPLERATE_AT_END_OF_HEADER_8) {
        frame_sample_rate = LOADER_TRY(bit_stream->read_bits<u32>(8)) * 1000;
    } else if (frame_sample_rate == FLAC_SAMPLERATE_AT_END_OF_HEADER_16) {
        frame_sample_rate = LOADER_TRY(bit_stream->read_bits<u32>(16));
    } else if (frame_sample_rate == FLAC_SAMPLERATE_AT_END_OF_HEADER_16X10) {
        frame_sample_rate = LOADER_TRY(bit_stream->read_bits<u32>(16)) * 10;
    }

    // TODO: check header checksum, see above
    [[maybe_unused]] u8 checksum = LOADER_TRY(bit_stream->read_bits<u8>(8));

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
        FlacSubframeHeader new_subframe = TRY(next_subframe_header(*bit_stream, i));
        Vector<i32> subframe_samples = TRY(parse_subframe(new_subframe, *bit_stream));
        current_subframes.unchecked_append(move(subframe_samples));
    }

    bit_stream->align_to_byte_boundary();

    // TODO: check checksum, see above
    [[maybe_unused]] u16 footer_checksum = LOADER_TRY(bit_stream->read_bits<u16>(16));
    dbgln_if(AFLACLOADER_DEBUG, "Subframe footer checksum: {}", footer_checksum);

    Vector<i32> left;
    Vector<i32> right;

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

    VERIFY(left.size() == right.size() && left.size() == m_current_frame->sample_count);

    double sample_rescale = static_cast<double>(1 << (pcm_bits_per_sample(m_current_frame->bit_depth) - 1));
    dbgln_if(AFLACLOADER_DEBUG, "Sample rescaled from {} bits: factor {:.1f}", pcm_bits_per_sample(m_current_frame->bit_depth), sample_rescale);

    // zip together channels
    auto samples_to_directly_copy = min(target_vector.size(), m_current_frame->sample_count);
    for (size_t i = 0; i < samples_to_directly_copy; ++i) {
        Sample frame = { left[i] / sample_rescale, right[i] / sample_rescale };
        target_vector[i] = frame;
    }
    // move superfluous data into the class buffer instead
    auto result = m_unread_data.try_grow_capacity(m_current_frame->sample_count - samples_to_directly_copy);
    if (result.is_error())
        return LoaderError { LoaderError::Category::Internal, static_cast<size_t>(samples_to_directly_copy + m_current_sample_or_frame), "Couldn't allocate sample buffer for superfluous data" };

    for (size_t i = samples_to_directly_copy; i < m_current_frame->sample_count; ++i) {
        Sample frame = { left[i] / sample_rescale, right[i] / sample_rescale };
        m_unread_data.unchecked_append(frame);
    }

    return {};
#undef FLAC_VERIFY
}

ErrorOr<u32, LoaderError> FlacLoaderPlugin::convert_sample_count_code(u8 sample_count_code)
{
    // single codes
    switch (sample_count_code) {
    case 0:
        return LoaderError { LoaderError::Category::Format, static_cast<size_t>(m_current_sample_or_frame), "Reserved block size" };
    case 1:
        return 192;
    case 6:
        return FLAC_BLOCKSIZE_AT_END_OF_HEADER_8;
    case 7:
        return FLAC_BLOCKSIZE_AT_END_OF_HEADER_16;
    }
    if (sample_count_code >= 2 && sample_count_code <= 5) {
        return 576 * AK::exp2(sample_count_code - 2);
    }
    return 256 * AK::exp2(sample_count_code - 8);
}

ErrorOr<u32, LoaderError> FlacLoaderPlugin::convert_sample_rate_code(u8 sample_rate_code)
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
        return LoaderError { LoaderError::Category::Format, static_cast<size_t>(m_current_sample_or_frame), "Invalid sample rate code" };
    }
}

ErrorOr<PcmSampleFormat, LoaderError> FlacLoaderPlugin::convert_bit_depth_code(u8 bit_depth_code)
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
        return LoaderError { LoaderError::Category::Format, static_cast<size_t>(m_current_sample_or_frame), "Reserved sample size" };
    default:
        return LoaderError { LoaderError::Category::Format, static_cast<size_t>(m_current_sample_or_frame), String::formatted("Unsupported sample size {}", bit_depth_code) };
    }
}

u8 frame_channel_type_to_channel_count(FlacFrameChannelType channel_type)
{
    if (channel_type <= 7)
        return channel_type + 1;
    return 2;
}

ErrorOr<FlacSubframeHeader, LoaderError> FlacLoaderPlugin::next_subframe_header(BigEndianInputBitStream& bit_stream, u8 channel_index)
{
    u8 bits_per_sample = static_cast<u16>(pcm_bits_per_sample(m_current_frame->bit_depth));

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
    if (LOADER_TRY(bit_stream.read_bit()) != 0)
        return LoaderError { LoaderError::Category::Format, static_cast<size_t>(m_current_sample_or_frame), "Zero bit padding" };

    // subframe type (encoding)
    u8 subframe_code = LOADER_TRY(bit_stream.read_bits<u8>(6));
    if ((subframe_code >= 0b000010 && subframe_code <= 0b000111) || (subframe_code > 0b001100 && subframe_code < 0b100000))
        return LoaderError { LoaderError::Category::Format, static_cast<size_t>(m_current_sample_or_frame), "Subframe type" };

    FlacSubframeType subframe_type;
    u8 order = 0;
    // LPC has the highest bit set
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
    bool has_wasted_bits = LOADER_TRY(bit_stream.read_bit());
    u8 k = 0;
    if (has_wasted_bits) {
        bool current_k_bit = 0;
        do {
            current_k_bit = LOADER_TRY(bit_stream.read_bit());
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

ErrorOr<Vector<i32>, LoaderError> FlacLoaderPlugin::parse_subframe(FlacSubframeHeader& subframe_header, BigEndianInputBitStream& bit_input)
{
    Vector<i32> samples;

    switch (subframe_header.type) {
    case FlacSubframeType::Constant: {
        u64 constant_value = LOADER_TRY(bit_input.read_bits<u64>(subframe_header.bits_per_sample - subframe_header.wasted_bits_per_sample));
        dbgln_if(AFLACLOADER_DEBUG, "Constant subframe: {}", constant_value);

        samples.ensure_capacity(m_current_frame->sample_count);
        VERIFY(subframe_header.bits_per_sample - subframe_header.wasted_bits_per_sample != 0);
        i32 constant = sign_extend(static_cast<u32>(constant_value), subframe_header.bits_per_sample - subframe_header.wasted_bits_per_sample);
        for (u32 i = 0; i < m_current_frame->sample_count; ++i) {
            samples.unchecked_append(constant);
        }
        break;
    }
    case FlacSubframeType::Fixed: {
        dbgln_if(AFLACLOADER_DEBUG, "Fixed LPC subframe order {}", subframe_header.order);
        samples = TRY(decode_fixed_lpc(subframe_header, bit_input));
        break;
    }
    case FlacSubframeType::Verbatim: {
        dbgln_if(AFLACLOADER_DEBUG, "Verbatim subframe");
        samples = TRY(decode_verbatim(subframe_header, bit_input));
        break;
    }
    case FlacSubframeType::LPC: {
        dbgln_if(AFLACLOADER_DEBUG, "Custom LPC subframe order {}", subframe_header.order);
        samples = TRY(decode_custom_lpc(subframe_header, bit_input));
        break;
    }
    default:
        return LoaderError { LoaderError::Category::Unimplemented, static_cast<size_t>(m_current_sample_or_frame), "Unhandled FLAC subframe type" };
    }

    for (size_t i = 0; i < samples.size(); ++i) {
        samples[i] <<= subframe_header.wasted_bits_per_sample;
    }

    ResampleHelper<i32> resampler(m_current_frame->sample_rate, m_sample_rate);
    return resampler.resample(samples);
}

// Decode a subframe that isn't actually encoded, usually seen in random data
ErrorOr<Vector<i32>, LoaderError> FlacLoaderPlugin::decode_verbatim(FlacSubframeHeader& subframe, BigEndianInputBitStream& bit_input)
{
    Vector<i32> decoded;
    decoded.ensure_capacity(m_current_frame->sample_count);

    VERIFY(subframe.bits_per_sample - subframe.wasted_bits_per_sample != 0);
    for (size_t i = 0; i < m_current_frame->sample_count; ++i) {
        decoded.unchecked_append(sign_extend(
            LOADER_TRY(bit_input.read_bits<u32>(subframe.bits_per_sample - subframe.wasted_bits_per_sample)),
            subframe.bits_per_sample - subframe.wasted_bits_per_sample));
    }

    return decoded;
}

// Decode a subframe encoded with a custom linear predictor coding, i.e. the subframe provides the polynomial order and coefficients
ErrorOr<Vector<i32>, LoaderError> FlacLoaderPlugin::decode_custom_lpc(FlacSubframeHeader& subframe, BigEndianInputBitStream& bit_input)
{
    Vector<i32> decoded;
    decoded.ensure_capacity(m_current_frame->sample_count);

    VERIFY(subframe.bits_per_sample - subframe.wasted_bits_per_sample != 0);
    // warm-up samples
    for (auto i = 0; i < subframe.order; ++i) {
        decoded.unchecked_append(sign_extend(
            LOADER_TRY(bit_input.read_bits<u32>(subframe.bits_per_sample - subframe.wasted_bits_per_sample)),
            subframe.bits_per_sample - subframe.wasted_bits_per_sample));
    }

    // precision of the coefficients
    u8 lpc_precision = LOADER_TRY(bit_input.read_bits<u8>(4));
    if (lpc_precision == 0b1111)
        return LoaderError { LoaderError::Category::Format, static_cast<size_t>(m_current_sample_or_frame), "Invalid linear predictor coefficient precision" };
    lpc_precision += 1;

    // shift needed on the data (signed!)
    i8 lpc_shift = sign_extend(LOADER_TRY(bit_input.read_bits<u8>(5)), 5);

    Vector<i32> coefficients;
    coefficients.ensure_capacity(subframe.order);
    // read coefficients
    for (auto i = 0; i < subframe.order; ++i) {
        u32 raw_coefficient = LOADER_TRY(bit_input.read_bits<u32>(lpc_precision));
        i32 coefficient = static_cast<i32>(sign_extend(raw_coefficient, lpc_precision));
        coefficients.unchecked_append(coefficient);
    }

    dbgln_if(AFLACLOADER_DEBUG, "{}-bit {} shift coefficients: {}", lpc_precision, lpc_shift, coefficients);

    TRY(decode_residual(decoded, subframe, bit_input));

    // approximate the waveform with the predictor
    for (size_t i = subframe.order; i < m_current_frame->sample_count; ++i) {
        // (see below)
        i64 sample = 0;
        for (size_t t = 0; t < subframe.order; ++t) {
            // It's really important that we compute in 64-bit land here.
            // Even though FLAC operates at a maximum bit depth of 32 bits, modern encoders use super-large coefficients for maximum compression.
            // These will easily overflow 32 bits and cause strange white noise that abruptly stops intermittently (at the end of a frame).
            // The simple fix of course is to do intermediate computations in 64 bits.
            sample += static_cast<i64>(coefficients[t]) * static_cast<i64>(decoded[i - t - 1]);
        }
        decoded[i] += sample >> lpc_shift;
    }

    return decoded;
}

// Decode a subframe encoded with one of the fixed linear predictor codings
ErrorOr<Vector<i32>, LoaderError> FlacLoaderPlugin::decode_fixed_lpc(FlacSubframeHeader& subframe, BigEndianInputBitStream& bit_input)
{
    Vector<i32> decoded;
    decoded.ensure_capacity(m_current_frame->sample_count);

    VERIFY(subframe.bits_per_sample - subframe.wasted_bits_per_sample != 0);
    // warm-up samples
    for (auto i = 0; i < subframe.order; ++i) {
        decoded.unchecked_append(sign_extend(
            LOADER_TRY(bit_input.read_bits<u32>(subframe.bits_per_sample - subframe.wasted_bits_per_sample)),
            subframe.bits_per_sample - subframe.wasted_bits_per_sample));
    }

    TRY(decode_residual(decoded, subframe, bit_input));

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
        return LoaderError { LoaderError::Category::Format, static_cast<size_t>(m_current_sample_or_frame), String::formatted("Unrecognized predictor order {}", subframe.order) };
    }
    return decoded;
}

// Decode the residual, the "error" between the function approximation and the actual audio data
MaybeLoaderError FlacLoaderPlugin::decode_residual(Vector<i32>& decoded, FlacSubframeHeader& subframe, BigEndianInputBitStream& bit_input)
{
    u8 residual_mode = LOADER_TRY(bit_input.read_bits<u8>(2));
    u8 partition_order = LOADER_TRY(bit_input.read_bits<u8>(4));
    size_t partitions = 1 << partition_order;

    if (residual_mode == FlacResidualMode::Rice4Bit) {
        // decode a single Rice partition with four bits for the order k
        for (size_t i = 0; i < partitions; ++i) {
            auto rice_partition = TRY(decode_rice_partition(4, partitions, i, subframe, bit_input));
            decoded.extend(move(rice_partition));
        }
    } else if (residual_mode == FlacResidualMode::Rice5Bit) {
        // five bits equivalent
        for (size_t i = 0; i < partitions; ++i) {
            auto rice_partition = TRY(decode_rice_partition(5, partitions, i, subframe, bit_input));
            decoded.extend(move(rice_partition));
        }
    } else
        return LoaderError { LoaderError::Category::Format, static_cast<size_t>(m_current_sample_or_frame), "Reserved residual coding method" };

    return {};
}

// Decode a single Rice partition as part of the residual, every partition can have its own Rice parameter k
ALWAYS_INLINE ErrorOr<Vector<i32>, LoaderError> FlacLoaderPlugin::decode_rice_partition(u8 partition_type, u32 partitions, u32 partition_index, FlacSubframeHeader& subframe, BigEndianInputBitStream& bit_input)
{
    // Rice parameter / Exp-Golomb order
    u8 k = LOADER_TRY(bit_input.read_bits<u8>(partition_type));

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
        u8 unencoded_bps = LOADER_TRY(bit_input.read_bits<u8>(5));
        for (size_t r = 0; r < residual_sample_count; ++r) {
            rice_partition[r] = LOADER_TRY(bit_input.read_bits<u8>(unencoded_bps));
        }
    } else {
        for (size_t r = 0; r < residual_sample_count; ++r) {
            rice_partition[r] = LOADER_TRY(decode_unsigned_exp_golomb(k, bit_input));
        }
    }

    return rice_partition;
}

// Decode a single number encoded with Rice/Exponential-Golomb encoding (the unsigned variant)
ALWAYS_INLINE ErrorOr<i32> decode_unsigned_exp_golomb(u8 k, BigEndianInputBitStream& bit_input)
{
    u8 q = 0;
    while (TRY(bit_input.read_bit()) == 0)
        ++q;

    // least significant bits (remainder)
    u32 rem = TRY(bit_input.read_bits<u32>(k));
    u32 value = q << k | rem;

    return rice_to_signed(value);
}

ErrorOr<u64> read_utf8_char(BigEndianInputBitStream& input)
{
    u64 character;
    u8 buffer = 0;
    Bytes buffer_bytes { &buffer, 1 };
    TRY(input.read(buffer_bytes));
    u8 start_byte = buffer_bytes[0];
    // Signal byte is zero: ASCII character
    if ((start_byte & 0b10000000) == 0) {
        return start_byte;
    } else if ((start_byte & 0b11000000) == 0b10000000) {
        return Error::from_string_literal("Illegal continuation byte"sv);
    }
    // This algorithm is too good and supports the theoretical max 0xFF start byte
    u8 length = 1;
    while (((start_byte << length) & 0b10000000) == 0b10000000)
        ++length;
    u8 bits_from_start_byte = 8 - (length + 1);
    u8 start_byte_bitmask = AK::exp2(bits_from_start_byte) - 1;
    character = start_byte_bitmask & start_byte;
    for (u8 i = length - 1; i > 0; --i) {
        TRY(input.read(buffer_bytes));
        u8 current_byte = buffer_bytes[0];
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
    i32 sign = -static_cast<i32>(x & 1);
    // copies the sign's sign onto the actual magnitude of x
    return static_cast<i32>(sign ^ (x >> 1));
}
}
