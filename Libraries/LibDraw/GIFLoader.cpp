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
#include <AK/NonnullOwnPtrVector.h>
#include <LibDraw/GIFLoader.h>
#include <stdio.h>

static RefPtr<GraphicsBitmap> load_gif_impl(const u8*, size_t);

RefPtr<GraphicsBitmap> load_gif(const StringView& path)
{
    MappedFile mapped_file(path);
    if (!mapped_file.is_valid())
        return nullptr;
    auto bitmap = load_gif_impl((const u8*)mapped_file.data(), mapped_file.size());
    if (bitmap)
        bitmap->set_mmap_name(String::format("GraphicsBitmap [%dx%d] - Decoded GIF: %s", bitmap->width(), bitmap->height(), canonicalized_path(path).characters()));
    return bitmap;
}

RefPtr<GraphicsBitmap> load_gif_from_memory(const u8* data, size_t length)
{
    auto bitmap = load_gif_impl(data, length);
    if (bitmap)
        bitmap->set_mmap_name(String::format("GraphicsBitmap [%dx%d] - Decoded GIF: <memory>", bitmap->width(), bitmap->height()));
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

RefPtr<GraphicsBitmap> load_gif_impl(const u8* data, size_t data_size)
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
            u8 packed_fields;
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
    printf("Image count: %d\n", images.size());
    if (images.is_empty())
        return nullptr;

    for (int i = 0; i < images.size(); ++i) {
        auto& image = images.at(i);
        printf("Image %d: %d,%d %dx%d  %d bytes LZW-encoded\n", i, image.x, image.y, image.width, image.height, image.lzw_encoded_bytes.size());

        // FIXME: Decode the LZW-encoded bytes and turn them into an image.
    }

    return nullptr;
}
