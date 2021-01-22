/*
 * Copyright (c) 2021, Pierre Hoffmeister
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

#include "PNGWriter.h"
#include <AK/String.h>
#include <LibCrypto/Checksum/CRC32.h>

namespace Gfx {

class PNGChunk {
public:
    PNGChunk(const String&);
    const Vector<u8>& data() const { return m_data; };
    const String& type() const { return m_type; };
    void add_u8(u8);
    void add_u16_big(u16);
    void add_u32_big(u32);
    void add_u16_little(u16);
    void add_u32_little(u32);

private:
    Vector<u8> m_data;
    String m_type;
};

class NonCompressibleBlock {
public:
    void finalize(PNGChunk&);
    void add_byte_to_block(u8 data, PNGChunk&);

    u32 adler_s1() { return m_adler_s1; }
    u32 adler_s2() { return m_adler_s2; }

private:
    void add_block_to_chunk(PNGChunk&, bool);
    void update_adler(u8);
    bool full() { return m_non_compressible_data.size() == 65535; }
    Vector<u8> m_non_compressible_data;
    u32 m_adler_s1 { 1 };
    u32 m_adler_s2 { 0 };
};

PNGChunk::PNGChunk(const String& type)
    : m_type(move(type))
{
}

void PNGChunk::add_u8(u8 data)
{
    m_data.append(data);
}

void PNGChunk::add_u16_little(u16 data)
{
    m_data.append(data & 0xff);
    m_data.append((data >> 8) & 0xff);
}

void PNGChunk::add_u32_little(u32 data)
{
    m_data.append(data & 0xff);
    m_data.append((data >> 8) & 0xff);
    m_data.append((data >> 16) & 0xff);
    m_data.append((data >> 24) & 0xff);
}

void PNGChunk::add_u32_big(u32 data)
{
    m_data.append((data >> 24) & 0xff);
    m_data.append((data >> 16) & 0xff);
    m_data.append((data >> 8) & 0xff);
    m_data.append(data & 0xff);
}

void PNGChunk::add_u16_big(u16 data)
{
    m_data.append((data >> 8) & 0xff);
    m_data.append(data & 0xff);
}

void NonCompressibleBlock::add_byte_to_block(u8 data, PNGChunk& chunk)
{
    m_non_compressible_data.append(data);
    update_adler(data);
    if (full()) {
        add_block_to_chunk(chunk, false);
        m_non_compressible_data.clear();
    }
}

void NonCompressibleBlock::add_block_to_chunk(PNGChunk& png_chunk, bool last)
{
    if (last) {
        png_chunk.add_u8(1);
    } else {
        png_chunk.add_u8(0);
    }

    auto len = m_non_compressible_data.size();
    auto nlen = ~len;

    png_chunk.add_u16_little(len);
    png_chunk.add_u16_little(nlen);

    for (auto non_compressed_byte : m_non_compressible_data) {
        png_chunk.add_u8(non_compressed_byte);
    }
}

void NonCompressibleBlock::finalize(PNGChunk& chunk)
{
    add_block_to_chunk(chunk, true);
}

void NonCompressibleBlock::update_adler(u8 data)
{
    m_adler_s1 = (m_adler_s1 + data) % 65521;
    m_adler_s2 = (m_adler_s2 + m_adler_s1) % 65521;
}

void PNGWriter::add_chunk(const PNGChunk& png_chunk)
{
    Vector<u8> combined;
    for (auto character : png_chunk.type()) {
        combined.append(character);
    }
    combined.append(png_chunk.data());

    auto crc = BigEndian(Crypto::Checksum::CRC32({ (const u8*)combined.data(), combined.size() }).digest());
    auto data_len = BigEndian(png_chunk.data().size());

    ByteBuffer buf;
    buf.append(&data_len, sizeof(u32));
    buf.append(combined.data(), combined.size());
    buf.append(&crc, sizeof(u32));

    m_data.append(buf.data(), buf.size());
}

void PNGWriter::add_png_header()
{
    const u8 png_header[8] = { 0x89, 'P', 'N', 'G', 13, 10, 26, 10 };
    m_data.append(png_header, sizeof(png_header));
}

void PNGWriter::add_IHDR_chunk(u32 width, u32 height, u8 bit_depth, u8 color_type, u8 compression_method, u8 filter_method, u8 interlace_method)
{
    PNGChunk png_chunk { "IHDR" };
    png_chunk.add_u32_big(width);
    png_chunk.add_u32_big(height);
    png_chunk.add_u8(bit_depth);
    png_chunk.add_u8(color_type);
    png_chunk.add_u8(compression_method);
    png_chunk.add_u8(filter_method);
    png_chunk.add_u8(interlace_method);
    add_chunk(png_chunk);
}

void PNGWriter::add_IEND_chunk()
{
    PNGChunk png_chunk { "IEND" };
    add_chunk(png_chunk);
}

void PNGWriter::add_IDAT_chunk(const RefPtr<Bitmap> bitmap)
{
    PNGChunk png_chunk { "IDAT" };

    u16 CMF_FLG = 0x81d;
    png_chunk.add_u16_big(CMF_FLG);

    NonCompressibleBlock non_compressible_block;

    for (int y = 0; y < bitmap->height(); ++y) {
        non_compressible_block.add_byte_to_block(0, png_chunk);

        for (int x = 0; x < bitmap->width(); ++x) {
            auto pixel = bitmap->get_pixel(x, y);
            non_compressible_block.add_byte_to_block(pixel.red(), png_chunk);
            non_compressible_block.add_byte_to_block(pixel.green(), png_chunk);
            non_compressible_block.add_byte_to_block(pixel.blue(), png_chunk);
            non_compressible_block.add_byte_to_block(pixel.alpha(), png_chunk);
        }
    }
    non_compressible_block.finalize(png_chunk);

    png_chunk.add_u16_big(non_compressible_block.adler_s2());
    png_chunk.add_u16_big(non_compressible_block.adler_s1());

    add_chunk(png_chunk);
}

ByteBuffer PNGWriter::write(const RefPtr<Bitmap> bitmap)
{
    add_png_header();
    add_IHDR_chunk(bitmap->width(), bitmap->height(), 8, 6, 0, 0, 0);
    add_IDAT_chunk(bitmap);
    add_IEND_chunk();

    ByteBuffer buf;
    buf.append(m_data.data(), m_data.size());
    return buf;
}

}
