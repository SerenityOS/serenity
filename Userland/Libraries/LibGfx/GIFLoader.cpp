/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Debug.h>
#include <AK/Math.h>
#include <AK/Memory.h>
#include <AK/MemoryStream.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibGfx/GIFLoader.h>
#include <string.h>

namespace Gfx {

// Row strides and offsets for each interlace pass.
static const int INTERLACE_ROW_STRIDES[] = { 8, 8, 4, 2 };
static const int INTERLACE_ROW_OFFSETS[] = { 0, 4, 2, 1 };

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
    const u8* data { nullptr };
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

static Optional<GIFFormat> decode_gif_header(InputMemoryStream& stream)
{
    static auto valid_header_87 = "GIF87a"sv;
    static auto valid_header_89 = "GIF89a"sv;

    Array<u8, 6> header;
    stream >> header;

    if (stream.handle_any_error())
        return {};

    if (header.span() == valid_header_87.bytes())
        return GIFFormat::GIF87a;
    if (header.span() == valid_header_89.bytes())
        return GIFFormat::GIF89a;

    return {};
}

class LZWDecoder {
private:
    static constexpr int max_code_size = 12;

public:
    explicit LZWDecoder(const Vector<u8>& lzw_bytes, u8 min_code_size)
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

    Optional<u16> next_code()
    {
        size_t current_byte_index = m_current_bit_index / 8;
        if (current_byte_index >= m_lzw_bytes.size()) {
            return {};
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
            const u32* addr = (const u32*)&padded_last_bytes;
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
            return {};
        } else if (m_current_code == m_code_table.size() && m_output.is_empty()) {
            dbgln_if(GIF_DEBUG, "Corrupted LZW stream, valid new code but output buffer is empty: {} at bit index {}, code table size: {}",
                m_current_code,
                m_current_bit_index,
                m_code_table.size());
            return {};
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

    void extend_code_table(const Vector<u8>& entry)
    {
        if (entry.size() > 1 && m_code_table.size() < 4096) {
            m_code_table.append(entry);
            if (m_code_table.size() >= m_table_capacity && m_code_size < max_code_size) {
                ++m_code_size;
                m_table_capacity *= 2;
            }
        }
    }

    const Vector<u8>& m_lzw_bytes;

    int m_current_bit_index { 0 };

    Vector<Vector<u8>> m_code_table {};
    Vector<Vector<u8>> m_original_code_table {};

    u8 m_code_size { 0 };
    u8 m_original_code_size { 0 };

    u32 m_table_capacity { 0 };

    u16 m_current_code { 0 };
    Vector<u8> m_output {};
};

static void copy_frame_buffer(Bitmap& dest, const Bitmap& src)
{
    VERIFY(dest.size_in_bytes() == src.size_in_bytes());
    memcpy(dest.scanline(0), src.scanline(0), dest.size_in_bytes());
}

static void clear_rect(Bitmap& bitmap, const IntRect& rect, Color color)
{
    auto intersection_rect = rect.intersected(bitmap.rect());
    if (intersection_rect.is_empty())
        return;

    RGBA32* dst = bitmap.scanline(intersection_rect.top()) + intersection_rect.left();
    const size_t dst_skip = bitmap.pitch() / sizeof(RGBA32);

    for (int i = intersection_rect.height() - 1; i >= 0; --i) {
        fast_u32_fill(dst, color.value(), intersection_rect.width());
        dst += dst_skip;
    }
}

static bool decode_frame(GIFLoadingContext& context, size_t frame_index)
{
    if (frame_index >= context.images.size()) {
        return false;
    }

    if (context.state >= GIFLoadingContext::State::FrameComplete && frame_index == context.current_frame) {
        return true;
    }

    size_t start_frame = context.current_frame + 1;
    if (context.state < GIFLoadingContext::State::FrameComplete) {
        start_frame = 0;
        {
            auto bitmap_or_error = Bitmap::try_create(BitmapFormat::BGRA8888, { context.logical_screen.width, context.logical_screen.height });
            if (bitmap_or_error.is_error())
                return false;
            context.frame_buffer = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
        }
        {
            auto bitmap_or_error = Bitmap::try_create(BitmapFormat::BGRA8888, { context.logical_screen.width, context.logical_screen.height });
            if (bitmap_or_error.is_error())
                return false;
            context.prev_frame_buffer = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
        }
    } else if (frame_index < context.current_frame) {
        start_frame = 0;
    }

    for (size_t i = start_frame; i <= frame_index; ++i) {
        auto& image = context.images.at(i);

        const auto previous_image_disposal_method = i > 0 ? context.images.at(i - 1).disposal_method : GIFImageDescriptor::DisposalMethod::None;

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
            return false;

        LZWDecoder decoder(image.lzw_encoded_bytes, image.lzw_min_code_size);

        // Add GIF-specific control codes
        const int clear_code = decoder.add_control_code();
        const int end_of_information_code = decoder.add_control_code();

        const auto& color_map = image.use_global_color_map ? context.logical_screen.color_map : image.color_map;

        int pixel_index = 0;
        int row = 0;
        int interlace_pass = 0;
        while (true) {
            Optional<u16> code = decoder.next_code();
            if (!code.has_value()) {
                dbgln_if(GIF_DEBUG, "Unexpectedly reached end of gif frame data");
                return false;
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
            for (const auto& color : colors) {
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

    return true;
}

static bool load_gif_frame_descriptors(GIFLoadingContext& context)
{
    if (context.data_size < 32)
        return false;

    InputMemoryStream stream { { context.data, context.data_size } };

    Optional<GIFFormat> format = decode_gif_header(stream);
    if (!format.has_value()) {
        return false;
    }

    LittleEndian<u16> value;

    stream >> value;
    context.logical_screen.width = value;

    stream >> value;
    context.logical_screen.height = value;

    if (stream.handle_any_error())
        return false;

    if (context.logical_screen.width > maximum_width_for_decoded_images || context.logical_screen.height > maximum_height_for_decoded_images) {
        dbgln("This GIF is too large for comfort: {}x{}", context.logical_screen.width, context.logical_screen.height);
        return false;
    }

    u8 gcm_info = 0;
    stream >> gcm_info;

    if (stream.handle_any_error())
        return false;

    stream >> context.background_color_index;
    if (stream.handle_any_error())
        return false;

    u8 pixel_aspect_ratio = 0;
    stream >> pixel_aspect_ratio;
    if (stream.handle_any_error())
        return false;

    u8 bits_per_pixel = (gcm_info & 7) + 1;
    int color_map_entry_count = 1;
    for (int i = 0; i < bits_per_pixel; ++i)
        color_map_entry_count *= 2;

    for (int i = 0; i < color_map_entry_count; ++i) {
        u8 r = 0;
        u8 g = 0;
        u8 b = 0;
        stream >> r >> g >> b;
        context.logical_screen.color_map[i] = { r, g, b };
    }

    if (stream.handle_any_error())
        return false;

    NonnullOwnPtr<GIFImageDescriptor> current_image = make<GIFImageDescriptor>();
    for (;;) {
        u8 sentinel = 0;
        stream >> sentinel;

        if (stream.handle_any_error())
            return false;

        if (sentinel == '!') {
            u8 extension_type = 0;
            stream >> extension_type;
            if (stream.handle_any_error())
                return false;

            u8 sub_block_length = 0;

            Vector<u8> sub_block {};
            for (;;) {
                stream >> sub_block_length;

                if (stream.handle_any_error())
                    return false;

                if (sub_block_length == 0)
                    break;

                u8 dummy = 0;
                for (u16 i = 0; i < sub_block_length; ++i) {
                    stream >> dummy;
                    sub_block.append(dummy);
                }

                if (stream.handle_any_error())
                    return false;
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

            LittleEndian<u16> tmp;

            u8 packed_fields { 0 };

            stream >> tmp;
            image.x = tmp;

            stream >> tmp;
            image.y = tmp;

            stream >> tmp;
            image.width = tmp;

            stream >> tmp;
            image.height = tmp;

            stream >> packed_fields;
            if (stream.handle_any_error())
                return false;

            image.use_global_color_map = !(packed_fields & 0x80);
            image.interlaced = (packed_fields & 0x40) != 0;

            if (!image.use_global_color_map) {
                size_t local_color_table_size = AK::exp2<size_t>((packed_fields & 7) + 1);

                for (size_t i = 0; i < local_color_table_size; ++i) {
                    u8 r = 0;
                    u8 g = 0;
                    u8 b = 0;
                    stream >> r >> g >> b;
                    image.color_map[i] = { r, g, b };
                }
            }

            stream >> image.lzw_min_code_size;
            if (stream.handle_any_error())
                return false;

            u8 lzw_encoded_bytes_expected = 0;

            for (;;) {
                stream >> lzw_encoded_bytes_expected;

                if (stream.handle_any_error())
                    return false;

                if (lzw_encoded_bytes_expected == 0)
                    break;

                Array<u8, 256> buffer;
                stream >> buffer.span().trim(lzw_encoded_bytes_expected);

                if (stream.handle_any_error())
                    return false;

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

        return false;
    }

    context.state = GIFLoadingContext::State::FrameDescriptorsLoaded;
    return true;
}

GIFImageDecoderPlugin::GIFImageDecoderPlugin(const u8* data, size_t size)
{
    m_context = make<GIFLoadingContext>();
    m_context->data = data;
    m_context->data_size = size;
}

GIFImageDecoderPlugin::~GIFImageDecoderPlugin() { }

IntSize GIFImageDecoderPlugin::size()
{
    if (m_context->error_state == GIFLoadingContext::ErrorState::FailedToLoadFrameDescriptors) {
        return {};
    }

    if (m_context->state < GIFLoadingContext::State::FrameDescriptorsLoaded) {
        if (!load_gif_frame_descriptors(*m_context)) {
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

bool GIFImageDecoderPlugin::sniff()
{
    InputMemoryStream stream { { m_context->data, m_context->data_size } };
    return decode_gif_header(stream).has_value();
}

bool GIFImageDecoderPlugin::is_animated()
{
    if (m_context->error_state != GIFLoadingContext::ErrorState::NoError) {
        return false;
    }

    if (m_context->state < GIFLoadingContext::State::FrameDescriptorsLoaded) {
        if (!load_gif_frame_descriptors(*m_context)) {
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
        if (!load_gif_frame_descriptors(*m_context)) {
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
        if (!load_gif_frame_descriptors(*m_context)) {
            m_context->error_state = GIFLoadingContext::ErrorState::FailedToLoadFrameDescriptors;
            return 1;
        }
    }

    return m_context->images.size();
}

ErrorOr<ImageFrameDescriptor> GIFImageDecoderPlugin::frame(size_t index)
{
    if (m_context->error_state >= GIFLoadingContext::ErrorState::FailedToDecodeAnyFrame) {
        return Error::from_string_literal("GIFImageDecoderPlugin: Decoding failed"sv);
    }

    if (m_context->state < GIFLoadingContext::State::FrameDescriptorsLoaded) {
        if (!load_gif_frame_descriptors(*m_context)) {
            m_context->error_state = GIFLoadingContext::ErrorState::FailedToLoadFrameDescriptors;
            return Error::from_string_literal("GIFImageDecoderPlugin: Decoding failed"sv);
        }
    }

    if (m_context->error_state == GIFLoadingContext::ErrorState::NoError && !decode_frame(*m_context, index)) {
        if (m_context->state < GIFLoadingContext::State::FrameComplete || !decode_frame(*m_context, 0)) {
            m_context->error_state = GIFLoadingContext::ErrorState::FailedToDecodeAnyFrame;
            return Error::from_string_literal("GIFImageDecoderPlugin: Decoding failed"sv);
        }
        m_context->error_state = GIFLoadingContext::ErrorState::FailedToDecodeAllFrames;
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
