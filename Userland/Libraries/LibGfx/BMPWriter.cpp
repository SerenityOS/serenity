/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
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

#include <AK/Vector.h>
#include <LibGfx/BMPWriter.h>
#include <LibGfx/Bitmap.h>

namespace Gfx {

constexpr int bytes_per_pixel = 3;

#define FILE_HEADER_SIZE 14
#define IMAGE_INFORMATION_SIZE 40
#define PIXEL_DATA_OFFSET FILE_HEADER_SIZE + IMAGE_INFORMATION_SIZE

class Streamer {
public:
    Streamer(u8* data)
        : m_data(data)
    {
    }

    void write_u8(u8 i)
    {
        *(m_data++) = i;
    }

    void write_u16(u16 i)
    {
        *(m_data++) = i & 0xFF;
        *(m_data++) = (i >> 8) & 0xFF;
    }

    void write_u32(u32 i)
    {
        write_u16(i & 0xFFFF);
        write_u16((i >> 16) & 0xFFFF);
    }

    void write_i32(i32 i)
    {
        write_u32(static_cast<u32>(i));
    }

private:
    u8* m_data;
};

static ByteBuffer write_pixel_data(const RefPtr<Bitmap> bitmap, int pixel_row_data_size)
{
    int image_size = pixel_row_data_size * bitmap->height();
    auto buffer = ByteBuffer::create_uninitialized(image_size);

    int current_row = 0;
    for (int y = bitmap->physical_height() - 1; y >= 0; --y) {
        auto* row = buffer.data() + (pixel_row_data_size * current_row++);
        for (int x = 0; x < bitmap->physical_width(); x++) {
            auto pixel = bitmap->get_pixel(x, y);
            row[x * bytes_per_pixel + 0] = pixel.blue();
            row[x * bytes_per_pixel + 1] = pixel.green();
            row[x * bytes_per_pixel + 2] = pixel.red();
        }
    }

    return buffer;
}

static ByteBuffer compress_pixel_data(const ByteBuffer& pixel_data, BMPWriter::Compression compression)
{
    switch (compression) {
    case BMPWriter::Compression::RGB:
        return pixel_data;
    }

    ASSERT_NOT_REACHED();
}

ByteBuffer BMPWriter::dump(const RefPtr<Bitmap> bitmap)
{
    int pixel_row_data_size = (bytes_per_pixel * 8 * bitmap->width() + 31) / 32 * 4;
    int image_size = pixel_row_data_size * bitmap->height();
    auto buffer = ByteBuffer::create_uninitialized(PIXEL_DATA_OFFSET);

    auto pixel_data = write_pixel_data(bitmap, pixel_row_data_size);
    pixel_data = compress_pixel_data(pixel_data, m_compression);

    int file_size = PIXEL_DATA_OFFSET + pixel_data.size();
    Streamer streamer(buffer.data());
    streamer.write_u8('B');
    streamer.write_u8('M');
    streamer.write_u32(file_size);
    streamer.write_u32(0);
    streamer.write_u32(PIXEL_DATA_OFFSET);

    streamer.write_u32(IMAGE_INFORMATION_SIZE); // Header size
    streamer.write_i32(bitmap->width());        // ImageWidth
    streamer.write_i32(bitmap->height());       // ImageHeight
    streamer.write_u16(1);                      // Planes
    streamer.write_u16(bytes_per_pixel * 8);    // BitsPerPixel
    streamer.write_u32((u32)m_compression);     // Compression
    streamer.write_u32(image_size);             // ImageSize
    streamer.write_i32(0);                      // XpixelsPerMeter
    streamer.write_i32(0);                      // YpixelsPerMeter
    streamer.write_u32(0);                      // TotalColors
    streamer.write_u32(0);                      // ImportantColors

    buffer.append(pixel_data.data(), pixel_data.size());
    return buffer;
}

}
