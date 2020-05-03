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
#include <AK/FileSystemPath.h>
#include <AK/MappedFile.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibGfx/GIFLoader.h>
#include <LibM/math.h>
#include <stdio.h>
#include <string.h>

namespace Gfx {

static bool load_gif_impl(GIFLoadingContext&);

struct GIFLoadingContext {
    enum State {
        NotDecoded = 0,
        Error,
        HeaderDecoded,
        BitmapDecoded,
    };
    State state { NotDecoded };
    const u8* data { nullptr };
    size_t data_size { 0 };
    int width { -1 };
    int height { -1 };
    Vector<RefPtr<Gfx::Bitmap>> frames {};
};

RefPtr<Gfx::Bitmap> load_gif(const StringView& path)
{
    MappedFile mapped_file(path);
    if (!mapped_file.is_valid())
        return nullptr;
    GIFImageDecoderPlugin gif_decoder((const u8*)mapped_file.data(), mapped_file.size());
    auto bitmap = gif_decoder.bitmap();
    if (bitmap)
        bitmap->set_mmap_name(String::format("Gfx::Bitmap [%dx%d] - Decoded GIF: %s", bitmap->width(), bitmap->height(), canonicalized_path(path).characters()));
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

struct RGB {
    u8 r;
    u8 g;
    u8 b;
};

struct LogicalScreen {
    u16 width;
    u16 height;
    RGB color_map[256];
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
public:
    struct CodeTableEntry {
        Vector<u8> colors;
        u16 code;
    };

    explicit LZWDecoder(const Vector<u8>& lzw_bytes, u8 min_code_size)
        : m_lzw_bytes(lzw_bytes)
        , m_code_size(min_code_size)
        , m_original_code_size(min_code_size)
    {
        init_code_table();
    }

    u16 add_control_code()
    {
        const u16 control_code = m_code_table.size();
        m_code_table.append({ {}, control_code });
        m_original_code_table.append({ {}, control_code });
        if (m_code_table.size() >= pow(2, m_code_size) && m_code_size < 12) {
            ++m_code_size;
            ++m_original_code_size;
        }
        return control_code;
    }

    void reset()
    {
        m_code_table.clear();
        m_code_table.append(m_original_code_table);
        m_code_size = m_original_code_size;
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
        u32 mask = (u32)(pow(2, m_code_size) - 1) << current_bit_offset;

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

    Vector<u8> get_output()
    {
        ASSERT(m_current_code <= m_code_table.size());
        if (m_current_code < m_code_table.size()) {
            Vector<u8> new_entry = m_output;
            m_output = m_code_table.at(m_current_code).colors;
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
        const int initial_table_size = pow(2, m_code_size);
        m_code_table.clear();
        for (u16 i = 0; i < initial_table_size; ++i) {
            m_code_table.append({ { (u8)i }, i });
        }
        m_original_code_table = m_code_table;
    }

    void extend_code_table(Vector<u8> entry)
    {
        if (entry.size() > 1 && m_code_table.size() < 4096) {
            m_code_table.append({ entry, (u16)m_code_table.size() });
            if (m_code_table.size() >= pow(2, m_code_size) && m_code_size < 12) {
                ++m_code_size;
            }
        }
    }

    const Vector<u8>& m_lzw_bytes;

    int m_current_bit_index { 0 };

    Vector<CodeTableEntry> m_code_table {};
    Vector<CodeTableEntry> m_original_code_table {};

    u8 m_code_size { 0 };
    u8 m_original_code_size { 0 };

    u16 m_current_code { 0 };
    Vector<u8> m_output {};
};

bool load_gif_impl(GIFLoadingContext& context)
{
    if (context.data_size < 32)
        return false;

    auto buffer = ByteBuffer::wrap(context.data, context.data_size);
    BufferStream stream(buffer);

    Optional<GIFFormat> format = decode_gif_header(stream);
    if (!format.has_value()) {
        return false;
    }

    printf("Format is %s\n", format.value() == GIFFormat::GIF89a ? "GIF89a" : "GIF87a");

    LogicalScreen logical_screen;
    stream >> logical_screen.width;
    stream >> logical_screen.height;
    if (stream.handle_read_failure())
        return false;

    context.width = logical_screen.width;
    context.height = logical_screen.height;

    u8 gcm_info = 0;
    stream >> gcm_info;

    if (stream.handle_read_failure())
        return false;

    bool global_color_map_follows_descriptor = gcm_info & 0x80;
    u8 bits_per_pixel = (gcm_info & 7) + 1;
    u8 bits_of_color_resolution = (gcm_info >> 4) & 7;

    printf("LogicalScreen: %dx%d\n", logical_screen.width, logical_screen.height);
    printf("global_color_map_follows_descriptor: %u\n", global_color_map_follows_descriptor);
    printf("bits_per_pixel: %u\n", bits_per_pixel);
    printf("bits_of_color_resolution: %u\n", bits_of_color_resolution);

    u8 background_color = 0;
    stream >> background_color;
    if (stream.handle_read_failure())
        return false;

    printf("background_color: %u\n", background_color);

    u8 pixel_aspect_ratio = 0;
    stream >> pixel_aspect_ratio;
    if (stream.handle_read_failure())
        return false;

    int color_map_entry_count = 1;
    for (int i = 0; i < bits_per_pixel; ++i)
        color_map_entry_count *= 2;

    printf("color_map_entry_count: %d\n", color_map_entry_count);

    for (int i = 0; i < color_map_entry_count; ++i) {
        stream >> logical_screen.color_map[i].r;
        stream >> logical_screen.color_map[i].g;
        stream >> logical_screen.color_map[i].b;
    }

    if (stream.handle_read_failure())
        return false;

    for (int i = 0; i < color_map_entry_count; ++i) {
        auto& rgb = logical_screen.color_map[i];
        printf("[%02x]: %s\n", i, Color(rgb.r, rgb.g, rgb.b).to_string().characters());
    }

    NonnullOwnPtrVector<ImageDescriptor> images;

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

            for (;;) {
                stream >> sub_block_length;

                if (stream.handle_read_failure())
                    return false;

                if (sub_block_length == 0)
                    break;

                u8 dummy;
                for (u16 i = 0; i < sub_block_length; ++i)
                    stream >> dummy;

                if (stream.handle_read_failure())
                    return false;
            }
            continue;
        }

        if (sentinel == 0x2c) {
            images.append(make<ImageDescriptor>());
            auto& image = images.last();
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
            continue;
        }

        if (sentinel == 0x3b) {
            printf("Trailer! Awesome :)\n");
            break;
        }

        return false;
    }

    // We exited the block loop after finding a trailer. We should have everything needed.
    printf("Image count: %zu\n", images.size());
    if (images.is_empty())
        return false;

    for (size_t i = 0; i < images.size(); ++i) {
        auto& image = images.at(i);
        printf("Image %zu: %d,%d %dx%d  %zu bytes LZW-encoded\n", i, image.x, image.y, image.width, image.height, image.lzw_encoded_bytes.size());

        LZWDecoder decoder(image.lzw_encoded_bytes, image.lzw_min_code_size);

        // Add GIF-specific control codes
        const int clear_code = decoder.add_control_code();
        const int end_of_information_code = decoder.add_control_code();

        auto bitmap = Bitmap::create_purgeable(BitmapFormat::RGBA32, { image.width, image.height });

        int pixel_index = 0;
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
                auto rgb = logical_screen.color_map[color];

                int x = pixel_index % image.width;
                int y = pixel_index / image.width;

                Color c = Color(rgb.r, rgb.g, rgb.b);
                bitmap->set_pixel(x, y, c);
                ++pixel_index;
            }
        }

        context.frames.append(bitmap);

        // FIXME: for now only decode the first frame.
        break;
    }

    context.state = GIFLoadingContext::State::BitmapDecoded;
    return true;
}

GIFImageDecoderPlugin::GIFImageDecoderPlugin(const u8* data, size_t size)
{
    m_context = make<GIFLoadingContext>();
    m_context->data = data;
    m_context->data_size = size;
}

GIFImageDecoderPlugin::~GIFImageDecoderPlugin() {}

Size GIFImageDecoderPlugin::size()
{
    if (m_context->state == GIFLoadingContext::State::Error) {
        return {};
    }

    if (m_context->state < GIFLoadingContext::State::BitmapDecoded) {
        if (!load_gif_impl(*m_context)) {
            m_context->state = GIFLoadingContext::State::Error;
            return {};
        }
    }

    return { m_context->width, m_context->height };
}

RefPtr<Gfx::Bitmap> GIFImageDecoderPlugin::bitmap()
{
    if (m_context->state == GIFLoadingContext::State::Error) {
        return nullptr;
    }

    if (m_context->state < GIFLoadingContext::State::BitmapDecoded) {
        if (!load_gif_impl(*m_context)) {
            m_context->state = GIFLoadingContext::State::Error;
            return nullptr;
        }
    }

    // FIXME: for now only return the first frame.
    if (m_context->frames.is_empty()) {
        return nullptr;
    }
    return m_context->frames.first();
}

void GIFImageDecoderPlugin::set_volatile()
{
    for (auto& frame : m_context->frames) {
        frame->set_volatile();
    }
}

bool GIFImageDecoderPlugin::set_nonvolatile()
{
    if (m_context->frames.is_empty()) {
        return false;
    }

    bool success = true;
    for (auto& frame : m_context->frames) {
        success &= frame->set_nonvolatile();
    }
    return success;
}

bool GIFImageDecoderPlugin::sniff()
{
    auto buffer = ByteBuffer::wrap(m_context->data, m_context->data_size);
    BufferStream stream(buffer);
    return decode_gif_header(stream).has_value();
}

bool GIFImageDecoderPlugin::is_animated()
{
    return false;
}

size_t GIFImageDecoderPlugin::loop_count()
{
    return 0;
}

size_t GIFImageDecoderPlugin::frame_count()
{
    return 1;
}

ImageFrameDescriptor GIFImageDecoderPlugin::frame(size_t i)
{
    if (i > 0) {
        return { bitmap(), 0 };
    }
    return {};
}

}
