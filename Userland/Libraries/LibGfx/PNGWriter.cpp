/*
 * Copyright (c) 2021, Pierre Hoffmeister
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Aziz Berkay Yesilyurt <abyesilyurt@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Concepts.h>
#include <AK/String.h>
#include <LibCrypto/Checksum/CRC32.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/PNGWriter.h>

namespace Gfx {

class PNGChunk {
    using data_length_type = u32;

public:
    explicit PNGChunk(String);
    auto const& data() const { return m_data; };
    String const& type() const { return m_type; };
    void reserve(size_t bytes) { m_data.ensure_capacity(bytes); }

    template<typename T>
    void add_as_big_endian(T);

    template<typename T>
    void add_as_little_endian(T);

    void add_u8(u8);

    template<typename T>
    void add(T*, size_t);

    void store_type();
    void store_data_length();
    u32 crc();

private:
    template<typename T>
    requires(IsUnsigned<T>) void add(T);

    ByteBuffer m_data;
    String m_type;
};

class NonCompressibleBlock {
public:
    void finalize(PNGChunk&);
    void add_byte_to_block(u8 data, PNGChunk&);

    u32 adler_s1() const { return m_adler_s1; }
    u32 adler_s2() const { return m_adler_s2; }

private:
    void add_block_to_chunk(PNGChunk&, bool);
    void update_adler(u8);
    bool full() { return m_non_compressible_data.size() == 65535; }
    Vector<u8> m_non_compressible_data;
    u16 m_adler_s1 { 1 };
    u16 m_adler_s2 { 0 };
};

PNGChunk::PNGChunk(String type)
    : m_type(move(type))
{
    add<data_length_type>(0);
    store_type();
}

void PNGChunk::store_type()
{
    m_data.append(type().bytes());
}

void PNGChunk::store_data_length()
{
    auto data_length = BigEndian<u32>(m_data.size() - sizeof(data_length_type) - m_type.length());
    __builtin_memcpy(m_data.offset_pointer(0), &data_length, sizeof(u32));
}

u32 PNGChunk::crc()
{
    u32 crc = Crypto::Checksum::CRC32({ m_data.offset_pointer(sizeof(data_length_type)), m_data.size() - sizeof(data_length_type) }).digest();
    return crc;
}

template<typename T>
requires(IsUnsigned<T>) void PNGChunk::add(T data)
{
    m_data.append(&data, sizeof(T));
}

template<typename T>
void PNGChunk::add(T* data, size_t size)
{
    m_data.append(data, size);
}

template<typename T>
void PNGChunk::add_as_little_endian(T data)
{
    auto data_out = AK::convert_between_host_and_little_endian(data);
    add(data_out);
}

template<typename T>
void PNGChunk::add_as_big_endian(T data)
{
    auto data_out = AK::convert_between_host_and_big_endian(data);
    add(data_out);
}

void PNGChunk::add_u8(u8 data)
{
    add(data);
}

void NonCompressibleBlock::add_byte_to_block(u8 data, PNGChunk& chunk)
{
    m_non_compressible_data.append(data);
    update_adler(data);
    if (full()) {
        add_block_to_chunk(chunk, false);
        m_non_compressible_data.clear_with_capacity();
    }
}

void NonCompressibleBlock::add_block_to_chunk(PNGChunk& png_chunk, bool last)
{
    png_chunk.add_u8(last);

    u16 len = m_non_compressible_data.size();
    u16 nlen = ~len;

    png_chunk.add_as_little_endian(len);
    png_chunk.add_as_little_endian(nlen);

    png_chunk.add(m_non_compressible_data.data(), m_non_compressible_data.size());
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

void PNGWriter::add_chunk(PNGChunk& png_chunk)
{
    png_chunk.store_data_length();
    u32 crc = png_chunk.crc();
    png_chunk.add_as_big_endian(crc);
    m_data.append(png_chunk.data().data(), png_chunk.data().size());
}

void PNGWriter::add_png_header()
{
    const u8 png_header[8] = { 0x89, 'P', 'N', 'G', 13, 10, 26, 10 };
    m_data.append(png_header, sizeof(png_header));
}

void PNGWriter::add_IHDR_chunk(u32 width, u32 height, u8 bit_depth, u8 color_type, u8 compression_method, u8 filter_method, u8 interlace_method)
{
    PNGChunk png_chunk { "IHDR" };
    png_chunk.add_as_big_endian(width);
    png_chunk.add_as_big_endian(height);
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

void PNGWriter::add_IDAT_chunk(Gfx::Bitmap const& bitmap)
{
    PNGChunk png_chunk { "IDAT" };
    png_chunk.reserve(bitmap.size_in_bytes());

    u16 CMF_FLG = 0x81d;
    png_chunk.add_as_big_endian(CMF_FLG);

    NonCompressibleBlock non_compressible_block;

    for (int y = 0; y < bitmap.height(); ++y) {
        non_compressible_block.add_byte_to_block(0, png_chunk);

        for (int x = 0; x < bitmap.width(); ++x) {
            auto pixel = bitmap.get_pixel(x, y);
            non_compressible_block.add_byte_to_block(pixel.red(), png_chunk);
            non_compressible_block.add_byte_to_block(pixel.green(), png_chunk);
            non_compressible_block.add_byte_to_block(pixel.blue(), png_chunk);
            non_compressible_block.add_byte_to_block(pixel.alpha(), png_chunk);
        }
    }
    non_compressible_block.finalize(png_chunk);

    png_chunk.add_as_big_endian(non_compressible_block.adler_s2());
    png_chunk.add_as_big_endian(non_compressible_block.adler_s1());

    add_chunk(png_chunk);
}

ByteBuffer PNGWriter::encode(Gfx::Bitmap const& bitmap)
{
    PNGWriter writer;
    writer.add_png_header();
    writer.add_IHDR_chunk(bitmap.width(), bitmap.height(), 8, 6, 0, 0, 0);
    writer.add_IDAT_chunk(bitmap);
    writer.add_IEND_chunk();
    // FIXME: Handle OOM failure.
    return ByteBuffer::copy(writer.m_data).release_value();
}

}
