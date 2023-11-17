/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "JPEGWriter.h"
#include "JPEGShared.h"
#include "JPEGWriterTables.h"
#include <AK/BitStream.h>
#include <AK/Endian.h>
#include <AK/Function.h>
#include <LibGfx/Bitmap.h>

namespace Gfx {

namespace {

// This is basically a BigEndianOutputBitStream, the only difference
// is that it appends 0x00 after each 0xFF when it writes bits.
class JPEGBigEndianOutputBitStream : public Stream {
public:
    explicit JPEGBigEndianOutputBitStream(Stream& stream)
        : m_stream(stream)
    {
    }

    virtual ErrorOr<Bytes> read_some(Bytes) override
    {
        return Error::from_errno(EBADF);
    }

    virtual ErrorOr<size_t> write_some(ReadonlyBytes bytes) override
    {
        VERIFY(m_bit_offset == 0);
        return m_stream.write_some(bytes);
    }

    template<Unsigned T>
    ErrorOr<void> write_bits(T value, size_t bit_count)
    {
        VERIFY(m_bit_offset <= 7);

        while (bit_count > 0) {
            u8 const next_bit = (value >> (bit_count - 1)) & 1;
            bit_count--;

            m_current_byte <<= 1;
            m_current_byte |= next_bit;
            m_bit_offset++;

            if (m_bit_offset > 7) {
                TRY(m_stream.write_value(m_current_byte));
                if (m_current_byte == 0xFF)
                    TRY(m_stream.write_value<u8>(0));

                m_bit_offset = 0;
                m_current_byte = 0;
            }
        }

        return {};
    }

    virtual bool is_eof() const override
    {
        return true;
    }

    virtual bool is_open() const override
    {
        return m_stream.is_open();
    }

    virtual void close() override
    {
    }

    ErrorOr<void> align_to_byte_boundary(u8 filler = 0x0)
    {
        if (m_bit_offset == 0)
            return {};

        TRY(write_bits(filler, 8 - m_bit_offset));
        VERIFY(m_bit_offset == 0);
        return {};
    }

private:
    Stream& m_stream;
    u8 m_current_byte { 0 };
    size_t m_bit_offset { 0 };
};

class JPEGEncodingContext {
public:
    JPEGEncodingContext(JPEGBigEndianOutputBitStream output_stream)
        : m_bit_stream(move(output_stream))
    {
    }

    ErrorOr<void> initialize_mcu(Bitmap const& bitmap)
    {
        u64 const horizontal_macroblocks = bitmap.width() / 8 + (bitmap.width() % 8 == 0 ? 0 : 1);
        m_vertical_macroblocks = bitmap.height() / 8 + (bitmap.height() % 8 == 0 ? 0 : 1);

        TRY(m_macroblocks.try_resize(horizontal_macroblocks * m_vertical_macroblocks));

        for (u16 y {}; y < bitmap.height(); ++y) {
            u16 const vertical_macroblock_index = y / 8;
            u16 const vertical_pixel_offset = y - vertical_macroblock_index * 8;

            for (u16 x {}; x < bitmap.width(); ++x) {
                u16 const horizontal_macroblock_index = x / 8;
                u16 const horizontal_pixel_offset = x - horizontal_macroblock_index * 8;

                auto& macroblock = m_macroblocks[vertical_macroblock_index * horizontal_macroblocks + horizontal_macroblock_index];
                auto const pixel_offset = vertical_pixel_offset * 8 + horizontal_pixel_offset;

                auto const original_pixel = bitmap.get_pixel(x, y);

                // Conversion from YCbCr to RGB isn't specified in the first JPEG specification but in the JFIF extension:
                // See: https://www.itu.int/rec/dologin_pub.asp?lang=f&id=T-REC-T.871-201105-I!!PDF-E&type=items
                // 7 - Conversion to and from RGB
                auto const y_ = clamp(0.299 * original_pixel.red() + 0.587 * original_pixel.green() + 0.114 * original_pixel.blue(), 0, 255);
                auto const cb = clamp(-0.1687 * original_pixel.red() - 0.3313 * original_pixel.green() + 0.5 * original_pixel.blue() + 128, 0, 255);
                auto const cr = clamp(0.5 * original_pixel.red() - 0.4187 * original_pixel.green() - 0.0813 * original_pixel.blue() + 128, 0, 255);

                // A.3.1 - Level shift
                macroblock.r[pixel_offset] = y_ - 128;
                macroblock.g[pixel_offset] = cb - 128;
                macroblock.b[pixel_offset] = cr - 128;
            }
        }

        return {};
    }

    static Array<double, 64> create_cosine_lookup_table()
    {
        static constexpr double pi_over_16 = AK::Pi<double> / 16;

        Array<double, 64> table;

        for (u8 u = 0; u < 8; ++u) {
            for (u8 x = 0; x < 8; ++x)
                table[u * 8 + x] = cos((2 * x + 1) * u * pi_over_16);
        }

        return table;
    }

    void fdct_and_quantization()
    {
        static auto cosine_table = create_cosine_lookup_table();

        for (auto& macroblock : m_macroblocks) {
            constexpr double inverse_sqrt_2 = M_SQRT1_2;

            auto const convert_one_component = [&](i16 component[], QuantizationTable const& table) {
                Array<i16, 64> result {};

                auto const sum_xy = [&](u8 u, u8 v) {
                    double sum {};
                    for (u8 x {}; x < 8; ++x) {
                        for (u8 y {}; y < 8; ++y)
                            sum += component[x * 8 + y] * cosine_table[u * 8 + x] * cosine_table[v * 8 + y];
                    }
                    return sum;
                };

                for (u8 u {}; u < 7; ++u) {
                    double const cu = u == 0 ? inverse_sqrt_2 : 1;
                    for (u8 v {}; v < 7; ++v) {
                        auto const table_index = u * 8 + v;

                        double const cv = v == 0 ? inverse_sqrt_2 : 1;

                        // A.3.3 - FDCT and IDCT
                        double const fdct = cu * cv * sum_xy(u, v) / 4;

                        // A.3.4 - DCT coefficient quantization
                        i16 const quantized = round(fdct / table.table[table_index]);

                        result[table_index] = quantized;
                    }
                }

                for (u8 i {}; i < result.size(); ++i)
                    component[i] = result[i];
            };

            convert_one_component(macroblock.y, m_luminance_quantization_table);
            convert_one_component(macroblock.cb, m_chrominance_quantization_table);
            convert_one_component(macroblock.cr, m_chrominance_quantization_table);
        }
    }

    ErrorOr<void> write_huffman_stream()
    {
        for (auto& macroblock : m_macroblocks) {
            TRY(encode_dc(dc_luminance_huffman_table, macroblock.y, 0));
            TRY(encode_ac(ac_luminance_huffman_table, macroblock.y));

            TRY(encode_dc(dc_chrominance_huffman_table, macroblock.cb, 1));
            TRY(encode_ac(ac_chrominance_huffman_table, macroblock.cb));

            TRY(encode_dc(dc_chrominance_huffman_table, macroblock.cr, 2));
            TRY(encode_ac(ac_chrominance_huffman_table, macroblock.cr));
        }

        TRY(m_bit_stream.align_to_byte_boundary(0xFF));

        return {};
    }

    void set_luminance_quantization_table(QuantizationTable const& table, int quality)
    {
        set_quantization_table(m_luminance_quantization_table, table, quality);
    }

    void set_chrominance_quantization_table(QuantizationTable const& table, int quality)
    {
        set_quantization_table(m_chrominance_quantization_table, table, quality);
    }

    QuantizationTable const& luminance_quantization_table() const
    {
        return m_luminance_quantization_table;
    }

    QuantizationTable const& chrominance_quantization_table() const
    {
        return m_chrominance_quantization_table;
    }

    OutputHuffmanTable dc_luminance_huffman_table;
    OutputHuffmanTable dc_chrominance_huffman_table;

    OutputHuffmanTable ac_luminance_huffman_table;
    OutputHuffmanTable ac_chrominance_huffman_table;

private:
    static void set_quantization_table(QuantizationTable& destination, QuantizationTable const& source, int quality)
    {
        // In order to be compatible with libjpeg-turbo, we use the same coefficients as them.

        quality = clamp(quality, 1, 100);

        if (quality < 50)
            quality = 5000 / quality;
        else
            quality = 200 - quality * 2;

        destination = source;
        for (u8 i {}; i < 64; ++i) {
            auto const shifted_value = (destination.table[i] * quality + 50) / 100;
            destination.table[i] = clamp(shifted_value, 1, 255);
        }
    }

    ErrorOr<void> write_symbol(OutputHuffmanTable::Symbol symbol)
    {
        return m_bit_stream.write_bits(symbol.word, symbol.code_length);
    }

    ErrorOr<void> encode_dc(OutputHuffmanTable const& dc_table, i16 const component[], u8 component_id)
    {
        // F.1.2.1.3 - Huffman encoding procedures for DC coefficients
        auto diff = component[0] - m_last_dc_values[component_id];
        m_last_dc_values[component_id] = component[0];

        auto const size = csize(diff);
        TRY(write_symbol(dc_table.from_input_byte(size)));

        if (diff < 0)
            diff -= 1;

        TRY(m_bit_stream.write_bits<u16>(diff, size));
        return {};
    }

    ErrorOr<void> encode_ac(OutputHuffmanTable const& ac_table, i16 const component[])
    {
        {
            // F.2 - Procedure for sequential encoding of AC coefficients with Huffman coding
            u32 k {};
            u32 r {};

            while (k < 63) {
                k++;

                auto coefficient = component[zigzag_map[k]];
                if (coefficient == 0) {
                    if (k == 63) {
                        TRY(write_symbol(ac_table.from_input_byte(0x00)));
                        break;
                    }
                    r += 1;
                    continue;
                }

                while (r > 15) {
                    TRY(write_symbol(ac_table.from_input_byte(0xF0)));
                    r -= 16;
                }

                {
                    // F.3 - Sequential encoding of a non-zero AC coefficient
                    auto const ssss = csize(coefficient);
                    auto const rs = (r << 4) + ssss;
                    TRY(write_symbol(ac_table.from_input_byte(rs)));

                    if (coefficient < 0)
                        coefficient -= 1;

                    TRY(m_bit_stream.write_bits<u16>(coefficient, ssss));
                }

                r = 0;
            }
        }
        return {};
    }

    static u8 csize(i16 coefficient)
    {
        VERIFY(coefficient >= -2047 && coefficient <= 2047);

        if (coefficient == 0)
            return 0;

        return floor(log2(abs(coefficient))) + 1;
    }

    QuantizationTable m_luminance_quantization_table {};
    QuantizationTable m_chrominance_quantization_table {};

    Vector<Macroblock> m_macroblocks {};
    Array<i16, 3> m_last_dc_values {};

    u64 m_vertical_macroblocks {};

    JPEGBigEndianOutputBitStream m_bit_stream;
};

ErrorOr<void> add_start_of_image(Stream& stream)
{
    TRY(stream.write_value<BigEndian<Marker>>(JPEG_SOI));
    return {};
}

ErrorOr<void> add_end_of_image(Stream& stream)
{
    TRY(stream.write_value<BigEndian<Marker>>(JPEG_EOI));
    return {};
}

ErrorOr<void> add_frame_header(Stream& stream, JPEGEncodingContext const& context, Bitmap const& bitmap)
{
    // B.2.2 - Frame header syntax
    TRY(stream.write_value<BigEndian<Marker>>(JPEG_SOF0));

    // Lf = 8 + 3 × Nf, we only support a single image per frame so Nf = 3
    TRY(stream.write_value<BigEndian<u16>>(17));

    // P
    TRY(stream.write_value<u8>(8));

    // Y
    TRY(stream.write_value<BigEndian<u16>>(bitmap.height()));

    // X
    TRY(stream.write_value<BigEndian<u16>>(bitmap.width()));

    // Nf, as mentioned earlier, we only support Nf = 3
    TRY(stream.write_value<u8>(3));

    // Encode 3 components
    for (u8 i {}; i < 3; ++i) {
        // Ci
        TRY(stream.write_value<u8>(i + 1));

        // Hi and Vi
        TRY(stream.write_value<u8>((1 << 4) | 1));

        // Tqi
        TRY(stream.write_value<u8>((i == 0 ? context.luminance_quantization_table() : context.chrominance_quantization_table()).id));
    }

    return {};
}

ErrorOr<void> add_quantization_table(Stream& stream, QuantizationTable const& table)
{
    // B.2.4.1 - Quantization table-specification syntax
    TRY(stream.write_value<BigEndian<Marker>>(JPEG_DQT));

    // Lq = 2 + 1 * 65
    TRY(stream.write_value<BigEndian<u16>>(2 + 65));

    // Pq and Tq
    TRY(stream.write_value<u8>((0 << 4) | table.id));

    for (u8 i = 0; i < 64; ++i)
        TRY(stream.write_value<u8>(table.table[zigzag_map[i]]));

    return {};
}

ErrorOr<Vector<Vector<u8>, 16>> sort_symbols_per_size(OutputHuffmanTable const& table)
{
    // JPEG only allows symbol with a size less than or equal to 16.
    Vector<Vector<u8>, 16> output {};
    TRY(output.try_resize(16));

    for (auto const& symbol : table.table)
        TRY(output[symbol.code_length - 1].try_append(symbol.input_byte));

    return output;
}

ErrorOr<void> add_huffman_table(Stream& stream, OutputHuffmanTable const& table)
{
    // B.2.4.2 - Huffman table-specification syntax
    TRY(stream.write_value<BigEndian<Marker>>(JPEG_DHT));

    // Lh
    TRY(stream.write_value<BigEndian<u16>>(2 + 17 + table.table.size()));

    // Tc and Th
    TRY(stream.write_value<u8>(table.id));

    auto const vectorized_table = TRY(sort_symbols_per_size(table));
    for (auto const& symbol_vector : vectorized_table)
        TRY(stream.write_value<u8>(symbol_vector.size()));

    for (auto const& symbol_vector : vectorized_table) {
        for (auto symbol : symbol_vector)
            TRY(stream.write_value<u8>(symbol));
    }

    return {};
}

ErrorOr<void> add_scan_header(Stream& stream)
{
    // B.2.3 - Scan header syntax
    TRY(stream.write_value<BigEndian<Marker>>(JPEG_SOS));

    // Ls - 6 + 2 × Ns
    TRY(stream.write_value<BigEndian<u16>>(6 + 2 * 3));

    // Ns
    TRY(stream.write_value<u8>(3));

    // Encode 3 components
    for (u8 i {}; i < 3; ++i) {
        // Csj
        TRY(stream.write_value<u8>(i + 1));

        // Tdj and Taj
        // We're using 0 for luminance and 1 for chrominance
        u8 const huffman_identifier = i > 0 ? 1 : 0;
        TRY(stream.write_value<u8>((huffman_identifier << 4) | huffman_identifier));
    }

    // Ss
    TRY(stream.write_value<u8>(0));

    // Se
    TRY(stream.write_value<u8>(63));

    // Ah and Al
    TRY(stream.write_value<u8>((0 << 4) | 0));

    return {};
}

}

ErrorOr<void> JPEGWriter::encode(Stream& stream, Bitmap const& bitmap, Options const& options)
{
    JPEGEncodingContext context { JPEGBigEndianOutputBitStream { stream } };

    context.set_luminance_quantization_table(s_default_luminance_quantization_table, options.quality);
    context.set_chrominance_quantization_table(s_default_chrominance_quantization_table, options.quality);

    context.dc_luminance_huffman_table = s_default_dc_luminance_huffman_table;
    context.dc_chrominance_huffman_table = s_default_dc_chrominance_huffman_table;

    context.ac_luminance_huffman_table = s_default_ac_luminance_huffman_table;
    context.ac_chrominance_huffman_table = s_default_ac_chrominance_huffman_table;

    TRY(add_start_of_image(stream));

    TRY(add_frame_header(stream, context, bitmap));

    TRY(add_quantization_table(stream, context.luminance_quantization_table()));
    TRY(add_quantization_table(stream, context.chrominance_quantization_table()));

    TRY(add_huffman_table(stream, context.dc_luminance_huffman_table));
    TRY(add_huffman_table(stream, context.dc_chrominance_huffman_table));
    TRY(add_huffman_table(stream, context.ac_luminance_huffman_table));
    TRY(add_huffman_table(stream, context.ac_chrominance_huffman_table));

    TRY(add_scan_header(stream));

    TRY(context.initialize_mcu(bitmap));
    context.fdct_and_quantization();

    TRY(context.write_huffman_stream());

    TRY(add_end_of_image(stream));
    return {};
}

}
