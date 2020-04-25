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

static RefPtr<Gfx::Bitmap> load_gif_impl(const u8*, size_t);

RefPtr<Gfx::Bitmap> load_gif(const StringView& path)
{
    MappedFile mapped_file(path);
    if (!mapped_file.is_valid())
        return nullptr;
    auto bitmap = load_gif_impl((const u8*)mapped_file.data(), mapped_file.size());
    if (bitmap)
        bitmap->set_mmap_name(String::format("Gfx::Bitmap [%dx%d] - Decoded GIF: %s", bitmap->width(), bitmap->height(), canonicalized_path(path).characters()));
    return bitmap;
}

RefPtr<Gfx::Bitmap> load_gif_from_memory(const u8* data, size_t length)
{
    auto bitmap = load_gif_impl(data, length);
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
        if ((int)m_code_table.size() >= pow(2, m_code_size) && m_code_size < 12) {
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

        // Read code bits using a 32-bit mask. If current code size > 9 bits
        // then it's possible the code spans 3 bytes.
        u8 current_bit_offset = m_current_bit_index % 8;
        u32 mask = (u32)(pow(2, m_code_size) - 1) << current_bit_offset;

        // Make sure that we don't read bytes past the end of the data.
        int bytes_past_end = current_byte_index + sizeof(mask) - m_lzw_bytes.size();
        u8 bits_past_end = 0;
        if (bytes_past_end > 0) {
            current_byte_index -= bytes_past_end;
            mask <<= bytes_past_end * 8;
            bits_past_end = bytes_past_end * 8;
        }

        ASSERT(current_byte_index + sizeof(mask) - 1 < m_lzw_bytes.size());
        const u32* addr = (const u32*)&m_lzw_bytes.at(current_byte_index);
        m_current_code = (*addr & mask) >> (current_bit_offset + bits_past_end);

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
            if ((int)m_code_table.size() >= pow(2, m_code_size) && m_code_size < 12) {
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

RefPtr<Gfx::Bitmap> load_gif_impl(const u8* data, size_t data_size)
{
    if (data_size < 32)
        return nullptr;

    auto buffer = ByteBuffer::wrap(data, data_size);
    BufferStream stream(buffer);

    static const char valid_header_87[] = "GIF87a";
    static const char valid_header_89[] = "GIF89a";

    char header[6];
    for (int i = 0; i < 6; ++i)
        stream >> header[i];

    GIFFormat format;
    if (!memcmp(header, valid_header_87, sizeof(header)))
        format = GIFFormat::GIF87a;
    else if (!memcmp(header, valid_header_89, sizeof(header)))
        format = GIFFormat::GIF89a;
    else
        return nullptr;

    printf("Format is %s\n", format == GIFFormat::GIF89a ? "GIF89a" : "GIF87a");

    LogicalScreen logical_screen;
    stream >> logical_screen.width;
    stream >> logical_screen.height;
    if (stream.handle_read_failure())
        return nullptr;

    u8 gcm_info = 0;
    stream >> gcm_info;

    if (stream.handle_read_failure())
        return nullptr;

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
        return nullptr;

    printf("background_color: %u\n", background_color);

    u8 pixel_aspect_ratio = 0;
    stream >> pixel_aspect_ratio;
    if (stream.handle_read_failure())
        return nullptr;

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
        return nullptr;

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
                return nullptr;

            printf("Extension block of type %02x\n", extension_type);

            u8 sub_block_length = 0;

            for (;;) {
                stream >> sub_block_length;

                if (stream.handle_read_failure())
                    return nullptr;

                if (sub_block_length == 0)
                    break;

                u8 dummy;
                for (u16 i = 0; i < sub_block_length; ++i)
                    stream >> dummy;

                if (stream.handle_read_failure())
                    return nullptr;
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
                return nullptr;
            printf("Image descriptor: %d,%d %dx%d, %02x\n", image.x, image.y, image.width, image.height, packed_fields);

            stream >> image.lzw_min_code_size;

            printf("min code size: %u\n", image.lzw_min_code_size);

            u8 lzw_encoded_bytes_expected = 0;

            for (;;) {
                stream >> lzw_encoded_bytes_expected;

                if (stream.handle_read_failure())
                    return nullptr;

                if (lzw_encoded_bytes_expected == 0)
                    break;

                u8 buffer[256];
                for (int i = 0; i < lzw_encoded_bytes_expected; ++i) {
                    stream >> buffer[i];
                }

                if (stream.handle_read_failure())
                    return nullptr;

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

        return nullptr;
    }

    // We exited the block loop after finding a trailer. We should have everything needed.
    printf("Image count: %zu\n", images.size());
    if (images.is_empty())
        return nullptr;

    for (size_t i = 0; i < images.size(); ++i) {
        auto& image = images.at(i);
        printf("Image %zu: %d,%d %dx%d  %zu bytes LZW-encoded\n", i, image.x, image.y, image.width, image.height, image.lzw_encoded_bytes.size());

        // FIXME: Decode the LZW-encoded bytes and turn them into an image.
    }

    return nullptr;
}

}
