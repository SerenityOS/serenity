/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/BufferStream.h>
#include <AK/ByteBuffer.h>
#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibGfx/GIFLoader.h>
#include <LibM/math.h>
#include <stdio.h>
#include <string.h>

namespace Gfx {

static bool load_gif_frame_descriptors(GIFLoadingContext&);

struct RGB {
    u8 r;
    u8 g;
    u8 b;
};

struct ImageDescriptor {
    u16 x;
    u16 y;
    u16 width;
    u16 height;
    bool use_global_color_map;
    RGB color_map[256];
    u8 lzw_min_code_size;
    Vector<u8> lzw_encoded_bytes;
    RefPtr<Gfx::Bitmap> bitmap;

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
};

struct LogicalScreen {
    u16 width;
    u16 height;
    RGB color_map[256];
};

struct GIFLoadingContext {
    enum State {
        NotDecoded = 0,
        Error,
        FrameDescriptorsLoaded,
    };
    State state { NotDecoded };
    size_t frames_decoded { 0 };
    const u8* data { nullptr };
    size_t data_size { 0 };
    LogicalScreen logical_screen {};
    u8 background_color_index { 0 };
    NonnullOwnPtrVector<ImageDescriptor> images {};
    size_t loops { 1 };
};

RefPtr<Gfx::Bitmap> load_gif(const StringView& path)
{
    MappedFile mapped_file(path);
    if (!mapped_file.is_valid())
        return nullptr;
    GIFImageDecoderPlugin gif_decoder((const u8*)mapped_file.data(), mapped_file.size());
    auto bitmap = gif_decoder.bitmap();
    if (bitmap)
        bitmap->set_mmap_name(String::format("Gfx::Bitmap [%dx%d] - Decoded GIF: %s", bitmap->width(), bitmap->height(), LexicalPath::canonicalized_path(path).characters()));
    return bitmap;
}

RefPtr<Gfx::Bitmap> load_gif_from_memory(const u8* data, size_t length)
{
    GIFImageDecoderPlugin gif_decoder(data, length);
    auto bitmap = gif_decoder.bitmap();
    if (bitmap)
        bitmap->set_mmap_name(String::format("Gfx::Bitmap [%dx%d] - Decoded GIF: <memory>", bitmap->width(), bitmap->height()));
    return bitmap;
}

enum class GIFFormat {
    GIF87a,
    GIF89a,
};

Optional<GIFFormat> decode_gif_header(BufferStream& stream)
{
    static const char valid_header_87[] = "GIF87a";
    static const char valid_header_89[] = "GIF89a";

    char header[6];
    for (int i = 0; i < 6; ++i)
        stream >> header[i];

    if (stream.handle_read_failure())
        return {};

    if (!memcmp(header, valid_header_87, sizeof(header)))
        return GIFFormat::GIF87a;
    else if (!memcmp(header, valid_header_89, sizeof(header)))
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
        , m_table_capacity(pow(2, min_code_size))
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
        m_code_table.append(m_original_code_table);
        m_code_size = m_original_code_size;
        m_table_capacity = pow(2, m_code_size);
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
            const u32* addr = (const u32*)&m_lzw_bytes.at(current_byte_index);
            m_current_code = (*addr & mask) >> current_bit_offset;
        }

        if (m_current_code > m_code_table.size()) {
            dbg() << "Corrupted LZW stream, invalid code: " << m_current_code << " at bit index: "
                  << m_current_bit_index << ", code table size: " << m_code_table.size();
            return {};
        }

        m_current_bit_index += m_code_size;

        return m_current_code;
    }

    Vector<u8>& get_output()
    {
        ASSERT(m_current_code <= m_code_table.size());
        if (m_current_code < m_code_table.size()) {
            Vector<u8> new_entry = m_output;
            m_output = m_code_table.at(m_current_code);
            new_entry.append(m_output[0]);
            extend_code_table(new_entry);
        } else if (m_current_code == m_code_table.size()) {
            m_output.append(m_output[0]);
            extend_code_table(m_output);
        }
        return m_output;
    }

private:
    void init_code_table()
    {
        m_code_table.clear();
        for (u16 i = 0; i < m_table_capacity; ++i) {
            m_code_table.append({ (u8)i });
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

bool decode_frames_up_to_index(GIFLoadingContext& context, size_t frame_index)
{
    if (frame_index >= context.images.size()) {
        return false;
    }

    for (size_t i = context.frames_decoded; i <= frame_index; ++i) {
        auto& image = context.images.at(i);
        printf("Image %zu: %d,%d %dx%d  %zu bytes LZW-encoded\n", i, image.x, image.y, image.width, image.height, image.lzw_encoded_bytes.size());

        dbg() << "Decoding frame: " << i + 1 << " of " << context.images.size();

        LZWDecoder decoder(image.lzw_encoded_bytes, image.lzw_min_code_size);

        // Add GIF-specific control codes
        const int clear_code = decoder.add_control_code();
        const int end_of_information_code = decoder.add_control_code();

        auto background_rgb = context.logical_screen.color_map[context.background_color_index];
        Color background_color = Color(background_rgb.r, background_rgb.g, background_rgb.b);

        image.bitmap = Bitmap::create_purgeable(BitmapFormat::RGBA32, { context.logical_screen.width, context.logical_screen.height });
        image.bitmap->fill(background_color);
        if (i > 0 && image.disposal_method == ImageDescriptor::DisposalMethod::InPlace) {
            for (int y = 0; y < image.bitmap->height(); ++y) {
                for (int x = 0; x < image.bitmap->width(); ++x) {
                    image.bitmap->set_pixel(x, y, context.images.at(i - 1).bitmap->get_pixel(x, y));
                }
            }
        }

        int pixel_index = 0;
        RGB transparent_color { 0, 0, 0 };
        if (image.transparent) {
            transparent_color = context.logical_screen.color_map[image.transparency_index];
        }

        while (true) {
            Optional<u16> code = decoder.next_code();
            if (!code.has_value()) {
                dbg() << "Unexpectedly reached end of gif frame data";
                return false;
            }

            if (code.value() == clear_code) {
                decoder.reset();
                continue;
            } else if (code.value() == end_of_information_code) {
                break;
            }

            auto colors = decoder.get_output();

            for (const auto& color : colors) {
                auto rgb = context.logical_screen.color_map[color];

                int x = pixel_index % image.width + image.x;
                int y = pixel_index / image.width + image.y;

                Color c = Color(rgb.r, rgb.g, rgb.b);

                if (image.transparent) {
                    if (rgb.r == transparent_color.r && rgb.g == transparent_color.g && rgb.b == transparent_color.b) {
                        c.set_alpha(0);
                    }
                }

                image.bitmap->set_pixel(x, y, c);
                ++pixel_index;
            }
        }

        ++context.frames_decoded;
    }

    return true;
}

bool load_gif_frame_descriptors(GIFLoadingContext& context)
{
    if (context.data_size < 32)
        return false;

    auto buffer = ByteBuffer::wrap(const_cast<u8*>(context.data), context.data_size);
    BufferStream stream(buffer);

    Optional<GIFFormat> format = decode_gif_header(stream);
    if (!format.has_value()) {
        return false;
    }

    printf("Format is %s\n", format.value() == GIFFormat::GIF89a ? "GIF89a" : "GIF87a");

    stream >> context.logical_screen.width;
    stream >> context.logical_screen.height;
    if (stream.handle_read_failure())
        return false;

    u8 gcm_info = 0;
    stream >> gcm_info;

    if (stream.handle_read_failure())
        return false;

    bool global_color_map_follows_descriptor = gcm_info & 0x80;
    u8 bits_per_pixel = (gcm_info & 7) + 1;
    u8 bits_of_color_resolution = (gcm_info >> 4) & 7;

    printf("LogicalScreen: %dx%d\n", context.logical_screen.width, context.logical_screen.height);
    printf("global_color_map_follows_descriptor: %u\n", global_color_map_follows_descriptor);
    printf("bits_per_pixel: %u\n", bits_per_pixel);
    printf("bits_of_color_resolution: %u\n", bits_of_color_resolution);

    stream >> context.background_color_index;
    if (stream.handle_read_failure())
        return false;

    printf("background_color: %u\n", context.background_color_index);

    u8 pixel_aspect_ratio = 0;
    stream >> pixel_aspect_ratio;
    if (stream.handle_read_failure())
        return false;

    int color_map_entry_count = 1;
    for (int i = 0; i < bits_per_pixel; ++i)
        color_map_entry_count *= 2;

    printf("color_map_entry_count: %d\n", color_map_entry_count);

    for (int i = 0; i < color_map_entry_count; ++i) {
        stream >> context.logical_screen.color_map[i].r;
        stream >> context.logical_screen.color_map[i].g;
        stream >> context.logical_screen.color_map[i].b;
    }

    if (stream.handle_read_failure())
        return false;

    for (int i = 0; i < color_map_entry_count; ++i) {
        auto& rgb = context.logical_screen.color_map[i];
        printf("[%02x]: %s\n", i, Color(rgb.r, rgb.g, rgb.b).to_string().characters());
    }

    NonnullOwnPtr<ImageDescriptor> current_image = make<ImageDescriptor>();
    for (;;) {
        u8 sentinel = 0;
        stream >> sentinel;
        printf("Sentinel: %02x\n", sentinel);

        if (sentinel == 0x21) {
            u8 extension_type = 0;
            stream >> extension_type;
            if (stream.handle_read_failure())
                return false;

            printf("Extension block of type %02x\n", extension_type);

            u8 sub_block_length = 0;

            Vector<u8> sub_block {};
            for (;;) {
                stream >> sub_block_length;

                if (stream.handle_read_failure())
                    return false;

                if (sub_block_length == 0)
                    break;

                u8 dummy = 0;
                for (u16 i = 0; i < sub_block_length; ++i) {
                    stream >> dummy;
                    sub_block.append(dummy);
                }

                if (stream.handle_read_failure())
                    return false;
            }

            if (extension_type == 0xF9) {
                if (sub_block.size() != 4) {
                    dbg() << "Unexpected graphic control size";
                    continue;
                }

                u8 disposal_method = (sub_block[0] & 0x1C) >> 2;
                current_image->disposal_method = (ImageDescriptor::DisposalMethod)disposal_method;

                u8 user_input = (sub_block[0] & 0x2) >> 1;
                current_image->user_input = user_input == 1;

                u8 transparent = sub_block[0] & 1;
                current_image->transparent = transparent == 1;

                u16 duration = sub_block[1] + ((u16)sub_block[2] >> 8);
                current_image->duration = duration;

                current_image->transparency_index = sub_block[3];
            }

            if (extension_type == 0xFF) {
                if (sub_block.size() != 14) {
                    dbg() << "Unexpected application extension size: " << sub_block.size();
                    continue;
                }

                if (sub_block[11] != 1) {
                    dbg() << "Unexpected application extension format";
                    continue;
                }

                u16 loops = sub_block[12] + (sub_block[13] << 8);
                context.loops = loops;
            }

            continue;
        }

        if (sentinel == 0x2c) {
            context.images.append(move(current_image));
            auto& image = context.images.last();

            u8 packed_fields { 0 };
            stream >> image.x;
            stream >> image.y;
            stream >> image.width;
            stream >> image.height;
            stream >> packed_fields;
            if (stream.handle_read_failure())
                return false;
            printf("Image descriptor: %d,%d %dx%d, %02x\n", image.x, image.y, image.width, image.height, packed_fields);

            stream >> image.lzw_min_code_size;

            printf("min code size: %u\n", image.lzw_min_code_size);

            u8 lzw_encoded_bytes_expected = 0;

            for (;;) {
                stream >> lzw_encoded_bytes_expected;

                if (stream.handle_read_failure())
                    return false;

                if (lzw_encoded_bytes_expected == 0)
                    break;

                u8 buffer[256];
                for (int i = 0; i < lzw_encoded_bytes_expected; ++i) {
                    stream >> buffer[i];
                }

                if (stream.handle_read_failure())
                    return false;

                for (int i = 0; i < lzw_encoded_bytes_expected; ++i) {
                    image.lzw_encoded_bytes.append(buffer[i]);
                }
            }

            current_image = make<ImageDescriptor>();
            continue;
        }

        if (sentinel == 0x3b) {
            printf("Trailer! Awesome :)\n");
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
    if (m_context->state == GIFLoadingContext::State::Error) {
        return {};
    }

    if (m_context->state < GIFLoadingContext::State::FrameDescriptorsLoaded) {
        if (!load_gif_frame_descriptors(*m_context)) {
            m_context->state = GIFLoadingContext::State::Error;
            return {};
        }
    }

    return { m_context->logical_screen.width, m_context->logical_screen.height };
}

RefPtr<Gfx::Bitmap> GIFImageDecoderPlugin::bitmap()
{
    return frame(0).image;
}

void GIFImageDecoderPlugin::set_volatile()
{
    for (size_t i = 0; i < m_context->frames_decoded; ++i) {
        m_context->images.at(i).bitmap->set_volatile();
    }
}

bool GIFImageDecoderPlugin::set_nonvolatile()
{
    if (m_context->images.is_empty()) {
        return false;
    }

    bool success = true;
    for (size_t i = 0; i < m_context->frames_decoded; ++i) {
        success &= m_context->images.at(i).bitmap->set_nonvolatile();
    }
    return success;
}

bool GIFImageDecoderPlugin::sniff()
{
    auto buffer = ByteBuffer::wrap(const_cast<u8*>(m_context->data), m_context->data_size);
    BufferStream stream(buffer);
    return decode_gif_header(stream).has_value();
}

bool GIFImageDecoderPlugin::is_animated()
{
    if (m_context->state < GIFLoadingContext::State::FrameDescriptorsLoaded) {
        if (!load_gif_frame_descriptors(*m_context)) {
            m_context->state = GIFLoadingContext::State::Error;
            return false;
        }
    }

    return m_context->images.size() > 1;
}

size_t GIFImageDecoderPlugin::loop_count()
{
    if (m_context->state < GIFLoadingContext::State::FrameDescriptorsLoaded) {
        if (!load_gif_frame_descriptors(*m_context)) {
            m_context->state = GIFLoadingContext::State::Error;
            return 0;
        }
    }

    return m_context->loops;
}

size_t GIFImageDecoderPlugin::frame_count()
{
    if (m_context->state < GIFLoadingContext::State::FrameDescriptorsLoaded) {
        if (!load_gif_frame_descriptors(*m_context)) {
            m_context->state = GIFLoadingContext::State::Error;
            return 1;
        }
    }

    return m_context->images.size();
}

ImageFrameDescriptor GIFImageDecoderPlugin::frame(size_t i)
{
    if (m_context->state == GIFLoadingContext::State::Error) {
        return {};
    }

    if (m_context->state < GIFLoadingContext::State::FrameDescriptorsLoaded) {
        if (!load_gif_frame_descriptors(*m_context)) {
            m_context->state = GIFLoadingContext::State::Error;
            return {};
        }
    }

    if (!decode_frames_up_to_index(*m_context, i)) {
        m_context->state = GIFLoadingContext::State::Error;
        return {};
    }

    ImageFrameDescriptor frame {};
    frame.image = m_context->images.at(i).bitmap;
    frame.duration = m_context->images.at(i).duration * 10;

    if (frame.duration <= 10) {
        frame.duration = 100;
    }

    return frame;
}

}
