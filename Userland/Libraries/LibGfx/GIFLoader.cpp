/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Debug.h>
#include <AK/Error.h>
#include <AK/IntegralMath.h>
#include <AK/Memory.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Try.h>
#include <LibCore/MemoryStream.h>
#include <LibGfx/GIFLoader.h>
#include <string.h>

namespace Gfx {

// Row strides and offsets for each interlace pass.
static constexpr Array<int, 4> INTERLACE_ROW_STRIDES = { 8, 8, 4, 2 };
static constexpr Array<int, 4> INTERLACE_ROW_OFFSETS = { 0, 4, 2, 1 };

struct GIFImageDescriptor {
    u16 x { 0 };
    u16 y { 0 };
    u16 width { 0 };
    u16 height { 0 };
    bool use_global_color_map { true };
    bool interlaced { false };
    Color color_map[256];
    u8 lzw_min_code_size { 0 };
    Vector<u8> lzw_encoded_bytes;

    // Fields from optional graphic control extension block
    enum DisposalMethod : u8 {
        None = 0,
        InPlace = 1,
        RestoreBackground = 2,
        RestorePrevious = 3,
    };
    DisposalMethod disposal_method { None };
    u8 transparency_index { 0 };
    u16 duration { 0 };
    bool transparent { false };
    bool user_input { false };

    const IntRect rect() const
    {
        return { this->x, this->y, this->width, this->height };
    }
};

struct LogicalScreen {
    u16 width;
    u16 height;
    Color color_map[256];
};

struct GIFLoadingContext {
    enum State {
        NotDecoded = 0,
        FrameDescriptorsLoaded,
        FrameComplete,
    };
    State state { NotDecoded };
    enum ErrorState {
        NoError = 0,
        FailedToDecodeAllFrames,
        FailedToDecodeAnyFrame,
        FailedToLoadFrameDescriptors,
    };
    ErrorState error_state { NoError };
    u8 const* data { nullptr };
    size_t data_size { 0 };
    LogicalScreen logical_screen {};
    u8 background_color_index { 0 };
    NonnullOwnPtrVector<GIFImageDescriptor> images {};
    size_t loops { 1 };
    RefPtr<Gfx::Bitmap> frame_buffer;
    size_t current_frame { 0 };
    RefPtr<Gfx::Bitmap> prev_frame_buffer;
};

enum class GIFFormat {
    GIF87a,
    GIF89a,
};

static ErrorOr<GIFFormat> decode_gif_header(Core::Stream::Stream& stream)
{
    static auto valid_header_87 = "GIF87a"sv;
    static auto valid_header_89 = "GIF89a"sv;

    Array<u8, 6> header;
    TRY(stream.read_entire_buffer(header));

    if (header.span() == valid_header_87.bytes())
        return GIFFormat::GIF87a;
    if (header.span() == valid_header_89.bytes())
        return GIFFormat::GIF89a;

    return Error::from_string_literal("GIF header unknown");
}

class LZWDecoder {
private:
    static constexpr int max_code_size = 12;

public:
    explicit LZWDecoder(Vector<u8> const& lzw_bytes, u8 min_code_size)
        : m_lzw_bytes(lzw_bytes)
        , m_code_size(min_code_size)
        , m_original_code_size(min_code_size)
        , m_table_capacity(AK::exp2<u32>(min_code_size))
    {
        init_code_table();
    }

    u16 add_control_code()
    {
        const u16 control_code = m_code_table.size();
        m_code_table.append(Vector<u8> {});
        m_original_code_table.append(Vector<u8> {});
        if (m_code_table.size() >= m_table_capacity && m_code_size < max_code_size) {

            ++m_code_size;
            ++m_original_code_size;
            m_table_capacity *= 2;
        }
        return control_code;
    }

    void reset()
    {
        m_code_table.clear();
        m_code_table.extend(m_original_code_table);
        m_code_size = m_original_code_size;
        m_table_capacity = AK::exp2<u32>(m_code_size);
        m_output.clear();
    }

    ErrorOr<u16> next_code()
    {
        size_t current_byte_index = m_current_bit_index / 8;
        if (current_byte_index >= m_lzw_bytes.size()) {
            return Error::from_string_literal("LZWDecoder tries to read ouf of bounds");
        }

        // Extract the code bits using a 32-bit mask to cover the possibility that if
        // the current code size > 9 bits then the code can span 3 bytes.
        u8 current_bit_offset = m_current_bit_index % 8;
        u32 mask = (u32)(m_table_capacity - 1) << current_bit_offset;

        // Make a padded copy of the final bytes in the data to ensure we don't read past the end.
        if (current_byte_index + sizeof(mask) > m_lzw_bytes.size()) {
            u8 padded_last_bytes[sizeof(mask)] = { 0 };
            for (int i = 0; current_byte_index + i < m_lzw_bytes.size(); ++i) {
                padded_last_bytes[i] = m_lzw_bytes[current_byte_index + i];
            }
            u32 const* addr = (u32 const*)&padded_last_bytes;
            m_current_code = (*addr & mask) >> current_bit_offset;
        } else {
            u32 tmp_word;
            memcpy(&tmp_word, &m_lzw_bytes.at(current_byte_index), sizeof(u32));
            m_current_code = (tmp_word & mask) >> current_bit_offset;
        }

        if (m_current_code > m_code_table.size()) {
            dbgln_if(GIF_DEBUG, "Corrupted LZW stream, invalid code: {} at bit index {}, code table size: {}",
                m_current_code,
                m_current_bit_index,
                m_code_table.size());
            return Error::from_string_literal("Corrupted LZW stream, invalid code");
        } else if (m_current_code == m_code_table.size() && m_output.is_empty()) {
            dbgln_if(GIF_DEBUG, "Corrupted LZW stream, valid new code but output buffer is empty: {} at bit index {}, code table size: {}",
                m_current_code,
                m_current_bit_index,
                m_code_table.size());
            return Error::from_string_literal("Corrupted LZW stream, valid new code but output buffer is empty");
        }

        m_current_bit_index += m_code_size;

        return m_current_code;
    }

    Vector<u8>& get_output()
    {
        VERIFY(m_current_code <= m_code_table.size());
        if (m_current_code < m_code_table.size()) {
            Vector<u8> new_entry = m_output;
            m_output = m_code_table.at(m_current_code);
            new_entry.append(m_output[0]);
            extend_code_table(new_entry);
        } else if (m_current_code == m_code_table.size()) {
            VERIFY(!m_output.is_empty());
            m_output.append(m_output[0]);
            extend_code_table(m_output);
        }
        return m_output;
    }

private:
    void init_code_table()
    {
        m_code_table.ensure_capacity(m_table_capacity);
        for (u16 i = 0; i < m_table_capacity; ++i) {
            m_code_table.unchecked_append({ (u8)i });
        }
        m_original_code_table = m_code_table;
    }

    void extend_code_table(Vector<u8> const& entry)
    {
        if (entry.size() > 1 && m_code_table.size() < 4096) {
            m_code_table.append(entry);
            if (m_code_table.size() >= m_table_capacity && m_code_size < max_code_size) {
                ++m_code_size;
                m_table_capacity *= 2;
            }
        }
    }

    Vector<u8> const& m_lzw_bytes;

    int m_current_bit_index { 0 };

    Vector<Vector<u8>> m_code_table {};
    Vector<Vector<u8>> m_original_code_table {};

    u8 m_code_size { 0 };
    u8 m_original_code_size { 0 };

    u32 m_table_capacity { 0 };

    u16 m_current_code { 0 };
    Vector<u8> m_output {};
};

static void copy_frame_buffer(Bitmap& dest, Bitmap const& src)
{
    VERIFY(dest.size_in_bytes() == src.size_in_bytes());
    memcpy(dest.scanline(0), src.scanline(0), dest.size_in_bytes());
}

static void clear_rect(Bitmap& bitmap, IntRect const& rect, Color color)
{
    auto intersection_rect = rect.intersected(bitmap.rect());
    if (intersection_rect.is_empty())
        return;

    ARGB32* dst = bitmap.scanline(intersection_rect.top()) + intersection_rect.left();
    const size_t dst_skip = bitmap.pitch() / sizeof(ARGB32);

    for (int i = intersection_rect.height() - 1; i >= 0; --i) {
        fast_u32_fill(dst, color.value(), intersection_rect.width());
        dst += dst_skip;
    }
}

static ErrorOr<void> decode_frame(GIFLoadingContext& context, size_t frame_index)
{
    if (frame_index >= context.images.size()) {
        return Error::from_string_literal("frame_index size too high");
    }

    if (context.state >= GIFLoadingContext::State::FrameComplete && frame_index == context.current_frame) {
        return {};
    }

    size_t start_frame = context.current_frame + 1;
    if (context.state < GIFLoadingContext::State::FrameComplete) {
        start_frame = 0;
        context.frame_buffer = TRY(Bitmap::try_create(BitmapFormat::BGRA8888, { context.logical_screen.width, context.logical_screen.height }));
        context.prev_frame_buffer = TRY(Bitmap::try_create(BitmapFormat::BGRA8888, { context.logical_screen.width, context.logical_screen.height }));

    } else if (frame_index < context.current_frame) {
        start_frame = 0;
    }

    for (size_t i = start_frame; i <= frame_index; ++i) {
        auto& image = context.images.at(i);

        auto const previous_image_disposal_method = i > 0 ? context.images.at(i - 1).disposal_method : GIFImageDescriptor::DisposalMethod::None;

        if (i == 0) {
            context.frame_buffer->fill(Color::Transparent);
        } else if (i > 0 && image.disposal_method == GIFImageDescriptor::DisposalMethod::RestorePrevious
            && previous_image_disposal_method != GIFImageDescriptor::DisposalMethod::RestorePrevious) {
            // This marks the start of a run of frames that once disposed should be restored to the
            // previous underlying image contents. Therefore we make a copy of the current frame
            // buffer so that it can be restored later.
            copy_frame_buffer(*context.prev_frame_buffer, *context.frame_buffer);
        }

        if (previous_image_disposal_method == GIFImageDescriptor::DisposalMethod::RestoreBackground) {
            // Note: RestoreBackground could be interpreted either as restoring the underlying
            // background of the entire image (e.g. container element's background-color), or the
            // background color of the GIF itself. It appears that all major browsers and most other
            // GIF decoders adhere to the former interpretation, therefore we will do the same by
            // clearing the entire frame buffer to transparent.
            clear_rect(*context.frame_buffer, context.images.at(i - 1).rect(), Color::Transparent);
        } else if (i > 0 && previous_image_disposal_method == GIFImageDescriptor::DisposalMethod::RestorePrevious) {
            // Previous frame indicated that once disposed, it should be restored to *its* previous
            // underlying image contents, therefore we restore the saved previous frame buffer.
            copy_frame_buffer(*context.frame_buffer, *context.prev_frame_buffer);
        }

        if (image.lzw_min_code_size > 8)
            return Error::from_string_literal("LZW minimum code size is greater than 8");

        LZWDecoder decoder(image.lzw_encoded_bytes, image.lzw_min_code_size);

        // Add GIF-specific control codes
        int const clear_code = decoder.add_control_code();
        int const end_of_information_code = decoder.add_control_code();

        auto const& color_map = image.use_global_color_map ? context.logical_screen.color_map : image.color_map;

        int pixel_index = 0;
        int row = 0;
        int interlace_pass = 0;
        while (true) {
            ErrorOr<u16> code = decoder.next_code();
            if (code.is_error()) {
                dbgln_if(GIF_DEBUG, "Unexpectedly reached end of gif frame data");
                return code.release_error();
            }

            if (code.value() == clear_code) {
                decoder.reset();
                continue;
            }
            if (code.value() == end_of_information_code)
                break;
            if (!image.width)
                continue;

            auto colors = decoder.get_output();
            for (auto const& color : colors) {
                auto c = color_map[color];

                int x = pixel_index % image.width + image.x;
                int y = row + image.y;

                if (context.frame_buffer->rect().contains(x, y) && (!image.transparent || color != image.transparency_index)) {
                    context.frame_buffer->set_pixel(x, y, c);
                }

                ++pixel_index;
                if (pixel_index % image.width == 0) {
                    if (image.interlaced) {
                        if (interlace_pass < 4) {
                            if (row + INTERLACE_ROW_STRIDES[interlace_pass] >= image.height) {
                                ++interlace_pass;
                                if (interlace_pass < 4)
                                    row = INTERLACE_ROW_OFFSETS[interlace_pass];
                            } else {
                                row += INTERLACE_ROW_STRIDES[interlace_pass];
                            }
                        }
                    } else {
                        ++row;
                    }
                }
            }
        }

        context.current_frame = i;
        context.state = GIFLoadingContext::State::FrameComplete;
    }

    return {};
}

static ErrorOr<void> load_gif_frame_descriptors(GIFLoadingContext& context)
{
    if (context.data_size < 32)
        return Error::from_string_literal("Size too short for GIF frame descriptors");

    auto stream = TRY(Core::Stream::FixedMemoryStream::construct(ReadonlyBytes { context.data, context.data_size }));

    TRY(decode_gif_header(*stream));

    context.logical_screen.width = TRY(stream->read_value<LittleEndian<u16>>());
    context.logical_screen.height = TRY(stream->read_value<LittleEndian<u16>>());

    if (context.logical_screen.width > maximum_width_for_decoded_images || context.logical_screen.height > maximum_height_for_decoded_images) {
        dbgln("This GIF is too large for comfort: {}x{}", context.logical_screen.width, context.logical_screen.height);
        return Error::from_string_literal("This GIF is too large for comfort");
    }

    auto gcm_info = TRY(stream->read_value<u8>());
    context.background_color_index = TRY(stream->read_value<u8>());
    [[maybe_unused]] auto pixel_aspect_ratio = TRY(stream->read_value<u8>());

    u8 bits_per_pixel = (gcm_info & 7) + 1;
    int color_map_entry_count = 1;
    for (int i = 0; i < bits_per_pixel; ++i)
        color_map_entry_count *= 2;

    for (int i = 0; i < color_map_entry_count; ++i) {
        u8 r = TRY(stream->read_value<u8>());
        u8 g = TRY(stream->read_value<u8>());
        u8 b = TRY(stream->read_value<u8>());
        context.logical_screen.color_map[i] = { r, g, b };
    }

    NonnullOwnPtr<GIFImageDescriptor> current_image = make<GIFImageDescriptor>();
    for (;;) {
        u8 sentinel = TRY(stream->read_value<u8>());

        if (sentinel == '!') {
            u8 extension_type = TRY(stream->read_value<u8>());

            u8 sub_block_length = 0;

            Vector<u8> sub_block {};
            for (;;) {
                sub_block_length = TRY(stream->read_value<u8>());
                if (sub_block_length == 0)
                    break;

                TRY(sub_block.try_resize(sub_block.size() + sub_block_length));
                TRY(stream->read_entire_buffer(sub_block.span().slice_from_end(sub_block_length)));
            }

            if (extension_type == 0xF9) {
                if (sub_block.size() != 4) {
                    dbgln_if(GIF_DEBUG, "Unexpected graphic control size");
                    continue;
                }

                u8 disposal_method = (sub_block[0] & 0x1C) >> 2;
                current_image->disposal_method = (GIFImageDescriptor::DisposalMethod)disposal_method;

                u8 user_input = (sub_block[0] & 0x2) >> 1;
                current_image->user_input = user_input == 1;

                u8 transparent = sub_block[0] & 1;
                current_image->transparent = transparent == 1;

                u16 duration = sub_block[1] + ((u16)sub_block[2] << 8);
                current_image->duration = duration;

                current_image->transparency_index = sub_block[3];
            }

            if (extension_type == 0xFF) {
                if (sub_block.size() != 14) {
                    dbgln_if(GIF_DEBUG, "Unexpected application extension size: {}", sub_block.size());
                    continue;
                }

                if (sub_block[11] != 1) {
                    dbgln_if(GIF_DEBUG, "Unexpected application extension format");
                    continue;
                }

                u16 loops = sub_block[12] + (sub_block[13] << 8);
                context.loops = loops;
            }

            continue;
        }

        if (sentinel == ',') {
            context.images.append(move(current_image));
            auto& image = context.images.last();

            image.x = TRY(stream->read_value<LittleEndian<u16>>());
            image.y = TRY(stream->read_value<LittleEndian<u16>>());
            image.width = TRY(stream->read_value<LittleEndian<u16>>());
            image.height = TRY(stream->read_value<LittleEndian<u16>>());

            auto packed_fields = TRY(stream->read_value<u8>());

            image.use_global_color_map = !(packed_fields & 0x80);
            image.interlaced = (packed_fields & 0x40) != 0;

            if (!image.use_global_color_map) {
                size_t local_color_table_size = AK::exp2<size_t>((packed_fields & 7) + 1);

                for (size_t i = 0; i < local_color_table_size; ++i) {
                    u8 r = TRY(stream->read_value<u8>());
                    u8 g = TRY(stream->read_value<u8>());
                    u8 b = TRY(stream->read_value<u8>());
                    image.color_map[i] = { r, g, b };
                }
            }

            image.lzw_min_code_size = TRY(stream->read_value<u8>());

            u8 lzw_encoded_bytes_expected = 0;

            for (;;) {
                lzw_encoded_bytes_expected = TRY(stream->read_value<u8>());
                if (lzw_encoded_bytes_expected == 0)
                    break;

                Array<u8, 256> buffer;
                TRY(stream->read_entire_buffer(buffer.span().trim(lzw_encoded_bytes_expected)));

                for (int i = 0; i < lzw_encoded_bytes_expected; ++i) {
                    image.lzw_encoded_bytes.append(buffer[i]);
                }
            }

            current_image = make<GIFImageDescriptor>();
            continue;
        }

        if (sentinel == ';') {
            break;
        }

        return Error::from_string_literal("Unexpected sentinel");
    }

    context.state = GIFLoadingContext::State::FrameDescriptorsLoaded;
    return {};
}

GIFImageDecoderPlugin::GIFImageDecoderPlugin(u8 const* data, size_t size)
{
    m_context = make<GIFLoadingContext>();
    m_context->data = data;
    m_context->data_size = size;
}

GIFImageDecoderPlugin::~GIFImageDecoderPlugin() = default;

IntSize GIFImageDecoderPlugin::size()
{
    if (m_context->error_state == GIFLoadingContext::ErrorState::FailedToLoadFrameDescriptors) {
        return {};
    }

    if (m_context->state < GIFLoadingContext::State::FrameDescriptorsLoaded) {
        if (load_gif_frame_descriptors(*m_context).is_error()) {
            m_context->error_state = GIFLoadingContext::ErrorState::FailedToLoadFrameDescriptors;
            return {};
        }
    }

    return { m_context->logical_screen.width, m_context->logical_screen.height };
}

void GIFImageDecoderPlugin::set_volatile()
{
    if (m_context->frame_buffer) {
        m_context->frame_buffer->set_volatile();
    }
}

bool GIFImageDecoderPlugin::set_nonvolatile(bool& was_purged)
{
    if (!m_context->frame_buffer)
        return false;
    return m_context->frame_buffer->set_nonvolatile(was_purged);
}

bool GIFImageDecoderPlugin::initialize()
{
    auto stream_or_error = Core::Stream::FixedMemoryStream::construct(ReadonlyBytes { m_context->data, m_context->data_size });
    if (stream_or_error.is_error())
        return false;
    return !decode_gif_header(*stream_or_error.value()).is_error();
}

ErrorOr<bool> GIFImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    auto stream = TRY(Core::Stream::FixedMemoryStream::construct(data));
    return !decode_gif_header(*stream).is_error();
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> GIFImageDecoderPlugin::create(ReadonlyBytes data)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) GIFImageDecoderPlugin(data.data(), data.size()));
}

bool GIFImageDecoderPlugin::is_animated()
{
    if (m_context->error_state != GIFLoadingContext::ErrorState::NoError) {
        return false;
    }

    if (m_context->state < GIFLoadingContext::State::FrameDescriptorsLoaded) {
        if (load_gif_frame_descriptors(*m_context).is_error()) {
            m_context->error_state = GIFLoadingContext::ErrorState::FailedToLoadFrameDescriptors;
            return false;
        }
    }

    return m_context->images.size() > 1;
}

size_t GIFImageDecoderPlugin::loop_count()
{
    if (m_context->error_state != GIFLoadingContext::ErrorState::NoError) {
        return 0;
    }

    if (m_context->state < GIFLoadingContext::State::FrameDescriptorsLoaded) {
        if (load_gif_frame_descriptors(*m_context).is_error()) {
            m_context->error_state = GIFLoadingContext::ErrorState::FailedToLoadFrameDescriptors;
            return 0;
        }
    }

    return m_context->loops;
}

size_t GIFImageDecoderPlugin::frame_count()
{
    if (m_context->error_state != GIFLoadingContext::ErrorState::NoError) {
        return 1;
    }

    if (m_context->state < GIFLoadingContext::State::FrameDescriptorsLoaded) {
        if (load_gif_frame_descriptors(*m_context).is_error()) {
            m_context->error_state = GIFLoadingContext::ErrorState::FailedToLoadFrameDescriptors;
            return 1;
        }
    }

    return m_context->images.size();
}

ErrorOr<ImageFrameDescriptor> GIFImageDecoderPlugin::frame(size_t index)
{
    if (m_context->error_state >= GIFLoadingContext::ErrorState::FailedToDecodeAnyFrame) {
        return Error::from_string_literal("GIFImageDecoderPlugin: Decoding failed");
    }

    if (m_context->state < GIFLoadingContext::State::FrameDescriptorsLoaded) {
        if (auto result = load_gif_frame_descriptors(*m_context); result.is_error()) {
            m_context->error_state = GIFLoadingContext::ErrorState::FailedToLoadFrameDescriptors;
            return result.release_error();
        }
    }

    if (m_context->error_state == GIFLoadingContext::ErrorState::NoError) {
        if (auto result = decode_frame(*m_context, index); result.is_error()) {
            if (m_context->state < GIFLoadingContext::State::FrameComplete) {
                m_context->error_state = GIFLoadingContext::ErrorState::FailedToDecodeAnyFrame;
                return result.release_error();
            }
            if (auto result = decode_frame(*m_context, 0); result.is_error()) {
                m_context->error_state = GIFLoadingContext::ErrorState::FailedToDecodeAnyFrame;
                return result.release_error();
            }
            m_context->error_state = GIFLoadingContext::ErrorState::FailedToDecodeAllFrames;
        }
    }

    ImageFrameDescriptor frame {};
    frame.image = TRY(m_context->frame_buffer->clone());
    frame.duration = m_context->images.at(index).duration * 10;

    if (frame.duration <= 10) {
        frame.duration = 100;
    }

    return frame;
}

}
