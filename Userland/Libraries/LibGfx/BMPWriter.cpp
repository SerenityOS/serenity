/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibGfx/BMPWriter.h>
#include <LibGfx/Bitmap.h>

namespace Gfx {

constexpr int bytes_per_pixel = 3;

#define FILE_HEADER_SIZE 14
#define IMAGE_INFORMATION_SIZE 40
#define PIXEL_DATA_OFFSET FILE_HEADER_SIZE + IMAGE_INFORMATION_SIZE

class OutputStreamer {
public:
    OutputStreamer(u8* data)
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

    VERIFY_NOT_REACHED();
}

ByteBuffer BMPWriter::dump(const RefPtr<Bitmap> bitmap)
{
    int pixel_row_data_size = (bytes_per_pixel * 8 * bitmap->width() + 31) / 32 * 4;
    int image_size = pixel_row_data_size * bitmap->height();
    auto buffer = ByteBuffer::create_uninitialized(PIXEL_DATA_OFFSET);

    auto pixel_data = write_pixel_data(bitmap, pixel_row_data_size);
    pixel_data = compress_pixel_data(pixel_data, m_compression);

    int file_size = PIXEL_DATA_OFFSET + pixel_data.size();
    OutputStreamer streamer(buffer.data());
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
