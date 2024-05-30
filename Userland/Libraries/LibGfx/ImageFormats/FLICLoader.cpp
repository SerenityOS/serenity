/*
 * Copyright (c) 2024, Nicolas Ramz <nicolas.ramz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <AK/MemoryStream.h>
#include <LibGfx/ImageFormats/FLICLoader.h>

namespace Gfx {

static constexpr size_t const flic_header_size = 128;
static constexpr size_t const chunk_header_size = 6;

struct Format {
    enum : u16 {
        FLI = 0xAF11,
        FLC = 0xAF12,
    };
};

struct ChunkType {
    enum : u16 {
        // sub_chunks
        COLOR_256 = 0x4,
        COLOR_64 = 0xB,
        FLI_COPY = 0x10,
        DELTA_FLI = 0xC,
        // main chunks
        PREFIX_TYPE = 0xF100,
        SCRIPT_CHUNK = 0xF1E0,
        FRAME_TYPE = 0xF1FA,
        SEGMENT_TABLE = 0xF1FB,
        HUFFMAN_TABLE = 0xF1FC
    };
};

struct Chunk {
    ByteBuffer data;
    u16 type;
};

struct FLICFrameDescriptor {
    Color color_map[256];
    Vector<NonnullOwnPtr<Chunk>> chunks;
};

struct FLICLoadingContext {
    FLICLoadingContext(FixedMemoryStream stream)
        : stream(move(stream))
    {
    }

    enum State {
        NotDecoded = 0,
        FrameDescriptorsLoaded,
        FrameComplete,
    };

    State state { NotDecoded };
    enum ErrorState {
        NoError = 0,
        FailedToDecodeAllFrames,
        FailedToLoadFrameDescriptors,
    };
    ErrorState error_state { NoError };
    u16 format;
    u16 width { 0 };
    u16 height { 0 };
    u16 color_depth { 0 };
    u16 frame_count { 0 };
    u16 flags { 0 };
    u32 duration { 0 };

    Vector<NonnullOwnPtr<FLICFrameDescriptor>> frames {};
    RefPtr<Gfx::Bitmap> frame_buffer;
    FixedMemoryStream stream;
};

static ErrorOr<u16> decode_flic_type(FixedMemoryStream& stream)
{
    TRY(stream.seek(4, SeekMode::FromCurrentPosition));
    auto type = TRY(stream.read_value<LittleEndian<u16>>());

    if (type == Format::FLI || type == Format::FLC) {
        return type;
    }

    dbgln_if(FLIC_DEBUG, "decode_flic_header not detected :( type={:#x}", type);

    return Error::from_string_literal("FLIC header unknown");
}

static ErrorOr<void> decode_flic_header(FLICLoadingContext& context)
{
    dbgln_if(FLIC_DEBUG, "Decoding flic header");
    if (TRY(context.stream.size()) < flic_header_size)
        return Error::from_string_literal("Size too short for FLIC header");

    context.format = TRY(decode_flic_type(context.stream));
    context.frame_count = TRY(context.stream.read_value<LittleEndian<u16>>());
    context.width = TRY(context.stream.read_value<LittleEndian<u16>>());
    context.height = TRY(context.stream.read_value<LittleEndian<u16>>());
    context.color_depth = TRY(context.stream.read_value<LittleEndian<u16>>());
    context.flags = TRY(context.stream.read_value<LittleEndian<u16>>());
    context.duration = TRY(context.stream.read_value<LittleEndian<u32>>());
    // In FLI files, frame duration is expressed in increments of 1/70 sec:
    // probably because VGA's common refresh rate was 70hz.
    // In this case a frame's duration is 1/70 ~ 14ms
    if (context.format == Format::FLI)
        context.duration *= 14;

    // The next 108 bytes are only used in later version of the format:
    // we can skip them for now.
    TRY(context.stream.seek(108, SeekMode::FromCurrentPosition));

    dbgln_if(FLIC_DEBUG, "frame_count={} width={} height={} depth={} flags={} duration={}", context.frame_count, context.width, context.height, context.color_depth, context.flags, context.duration);

    return {};
}

static ErrorOr<void> decode_frame(FLICLoadingContext& context, size_t frame_index)
{
    auto& current_frame = context.frames.at(frame_index);
    dbgln_if(FLIC_DEBUG, "Decode_frame width={} frame_index={} chunks={}", context.width, frame_index, current_frame->chunks.size());

    // Not very optimal: copy color_table from previous frame
    if (frame_index > 0) {
        auto& previous_frame = context.frames.at(frame_index - 1);
        for (size_t c = 0; c < 255; c++) {
            current_frame->color_map[c] = previous_frame->color_map[c];
        }
    }

    for (size_t i = 0; i < current_frame->chunks.size(); ++i) {
        auto const& chunk = current_frame->chunks.at(i);
        auto stream = make<FixedMemoryStream>(ReadonlyBytes { chunk->data });

        switch (chunk->type) {
        case ChunkType::COLOR_64: {
            dbgln_if(FLIC_DEBUG, "Decoding color_map_64");
            auto num_packets = TRY(stream->read_value<LittleEndian<u16>>());
            u8 color_index = 0;

            for (size_t packet = 0; packet < num_packets; ++packet) {
                auto skip_count = TRY(stream->read_value<u8>());
                u16 copy_count = TRY(stream->read_value<u8>());
                if (copy_count == 0)
                    copy_count = 256;
                color_index += skip_count;

                for (size_t color = 0; color < copy_count; color++) {
                    u8 r = TRY(stream->read_value<u8>()) << 2;
                    u8 g = TRY(stream->read_value<u8>()) << 2;
                    u8 b = TRY(stream->read_value<u8>()) << 2;
                    current_frame->color_map[color_index++] = { r, g, b };
                }
            }
            break;
        }

        case ChunkType::FLI_COPY: {
            dbgln_if(FLIC_DEBUG, "Decoding copy_buffer {}x{} buffer_size={}", context.width, context.height, stream->size());
            context.frame_buffer = TRY(Bitmap::create(BitmapFormat::BGRA8888, { context.width, context.height }));
            for (size_t y = 0; y < context.height; ++y) {
                for (size_t x = 0; x < context.width; ++x) {
                    context.frame_buffer->set_pixel(x, y, current_frame->color_map[TRY(stream->read_value<u8>())]);
                }
            }
            break;
        }

        case ChunkType::DELTA_FLI: {
            dbgln_if(FLIC_DEBUG, "Decoding delta_fli {}x{} buffer_size={}", context.width, context.height, stream->size());
            auto lines_to_skip = TRY(stream->read_value<LittleEndian<u16>>());
            auto number_of_lines = TRY(stream->read_value<LittleEndian<u16>>());
            auto& previous_frame = context.frames.at(frame_index - 1);

            for (size_t line = lines_to_skip; line < lines_to_skip + number_of_lines; line++) {
                size_t x = 0;
                auto packets = TRY(stream->read_value<u8>());

                for (u8 packet = 0; packet < packets; packet++) {
                    auto col_skip_count = TRY(stream->read_value<u8>());
                    auto type = TRY(stream->read_value<i8>());
                    x += col_skip_count;

                    // copy next pixels
                    if (type > 0) {
                        auto dest_x = x;
                        for (i8 n = 0; n < type; n++) {
                            auto color = TRY(stream->read_value<u8>());
                            context.frame_buffer->set_pixel(dest_x++, line, previous_frame->color_map[color]);
                        }
                    } else if (type < 0) {
                        type = -type;
                        // repeat next pixels
                        auto color = TRY(stream->read_value<u8>());
                        auto dest_x = x;
                        for (i8 n = 0; n < type; n++) {
                            context.frame_buffer->set_pixel(dest_x++, line, previous_frame->color_map[color]);
                        }
                    }

                    x += type;
                }
            }
            break;
        }

        default: {
            dbgln_if(FLIC_DEBUG, "Unknown main chunk {:#x}", chunk->type);
            break;
        }
        }
    }

    context.state = FLICLoadingContext::State::FrameComplete;

    return {};
}

static ErrorOr<void> read_subchunk(FLICLoadingContext& context)
{
    auto& current_frame = context.frames.last();
    auto offset = context.stream.offset();
    auto size = TRY(context.stream.read_value<LittleEndian<u32>>());
    auto type = TRY(context.stream.read_value<LittleEndian<u16>>());
    dbgln_if(FLIC_DEBUG, "Subchunk type={:#x} size={}", type, size);
    NonnullOwnPtr<Chunk> chunk = make<Chunk>();
    chunk->type = type;
    // Some FLI encoders save the wrong size on the subchunk header.
    // For example in FLI_COPY subchunks: let's make sure the size is is enough to hold
    // an uncompressed frame.
    size_t data_size = type == 0x10 ? AK::max(size - chunk_header_size, context.width * context.height) : (u16)size;
    dbgln_if(FLIC_DEBUG, "Using subchunk size {}", data_size);
    auto const chunk_data = TRY(chunk->data.get_bytes_for_writing(data_size));
    TRY(context.stream.read_until_filled(chunk_data));
    current_frame->chunks.append(move(chunk));

    TRY(context.stream.seek(offset + size, SeekMode::SetPosition));

    return {};
}

static ErrorOr<void> load_flic_frame_chunks(FLICLoadingContext& context)
{
    while (context.frames.size() < context.frame_count) {
        auto offset = context.stream.offset();
        auto size = TRY(context.stream.read_value<LittleEndian<u32>>());
        auto chunk_type = TRY(context.stream.read_value<LittleEndian<u16>>());
        auto num_chunks = TRY(context.stream.read_value<LittleEndian<u16>>());

        switch (chunk_type) {
        case ChunkType::FRAME_TYPE: {
            dbgln_if(FLIC_DEBUG, "Found frame chunk size={} type={:#x} num_chunks={}", size, chunk_type, num_chunks);
            NonnullOwnPtr<FLICFrameDescriptor> current_frame = make<FLICFrameDescriptor>();
            context.frames.append(move(current_frame));

            // The next 8 bytes are reserved bytes.
            TRY(context.stream.seek(8, SeekMode::FromCurrentPosition));

            for (auto i = 0; i < num_chunks; ++i) {
                TRY(read_subchunk(context));
                dbgln_if(FLIC_DEBUG, "Added subchunk offset={}", context.stream.offset());
            }
            break;
        }

        default: {
            dbgln_if(FLIC_DEBUG, "Skipping unknown main chunk type={:#x}", chunk_type);
            break;
        }
        }

        // Skip unknown chunks / pad bytes
        TRY(context.stream.seek(offset + size, SeekMode::SetPosition));
    }

    return {};
}

bool FLICImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    FixedMemoryStream stream { data };
    return !decode_flic_type(stream).is_error();
}

FLICImageDecoderPlugin::FLICImageDecoderPlugin(FixedMemoryStream stream)
{
    m_context = make<FLICLoadingContext>(move(stream));
}

FLICImageDecoderPlugin::~FLICImageDecoderPlugin() = default;

IntSize FLICImageDecoderPlugin::size()
{
    return { m_context->width, m_context->height };
}

size_t FLICImageDecoderPlugin::first_animated_frame_index()
{
    return 0;
}

bool FLICImageDecoderPlugin::is_animated()
{
    if (m_context->error_state != FLICLoadingContext::ErrorState::NoError) {
        return false;
    }

    if (m_context->state < FLICLoadingContext::State::FrameDescriptorsLoaded) {
        if (load_flic_frame_chunks(*m_context).is_error()) {
            m_context->error_state = FLICLoadingContext::ErrorState::FailedToLoadFrameDescriptors;
            return false;
        }
    }

    return m_context->frames.size() > 1;
}

size_t FLICImageDecoderPlugin::loop_count()
{
    if (m_context->error_state != FLICLoadingContext::ErrorState::NoError) {
        return 0;
    }

    if (m_context->state < FLICLoadingContext::State::FrameDescriptorsLoaded) {
        if (load_flic_frame_chunks(*m_context).is_error()) {
            m_context->error_state = FLICLoadingContext::ErrorState::FailedToLoadFrameDescriptors;
            return 0;
        }
    }

    return 1;
}

size_t FLICImageDecoderPlugin::frame_count()
{
    if (m_context->error_state != FLICLoadingContext::ErrorState::NoError) {
        return 1;
    }

    if (m_context->state < FLICLoadingContext::State::FrameDescriptorsLoaded) {
        if (load_flic_frame_chunks(*m_context).is_error()) {
            m_context->error_state = FLICLoadingContext::ErrorState::FailedToLoadFrameDescriptors;
            return 1;
        }
    }

    return m_context->frames.size();
}

ErrorOr<ImageFrameDescriptor> FLICImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
{
    if (m_context->state < FLICLoadingContext::State::FrameDescriptorsLoaded) {
        dbgln_if(FLIC_DEBUG, "Reading frame chunks");
        if (auto result = load_flic_frame_chunks(*m_context); result.is_error()) {
            m_context->error_state = FLICLoadingContext::ErrorState::FailedToLoadFrameDescriptors;
            return result.release_error();
        }
    }

    if (m_context->error_state == FLICLoadingContext::ErrorState::NoError) {
        dbgln_if(FLIC_DEBUG, "Decoding frame {}", index);
        TRY(decode_frame(*m_context, index));
    }

    if (m_context->state < FLICLoadingContext::State::FrameComplete) {
        return Error::from_string_literal("FLICImageDecoderPlugin: Frame could not be decoded");
    }

    ImageFrameDescriptor frame {};
    frame.image = TRY(m_context->frame_buffer->clone());
    frame.duration = m_context->duration;

    return frame;
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> FLICImageDecoderPlugin::create(ReadonlyBytes data)
{
    FixedMemoryStream stream { data };
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) FLICImageDecoderPlugin(move(stream))));
    TRY(decode_flic_header(*plugin->m_context));
    return plugin;
}
}
