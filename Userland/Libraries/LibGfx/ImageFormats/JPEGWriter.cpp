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
#include <AK/Enumerate.h>
#include <AK/Function.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/CMYKBitmap.h>

namespace Gfx {

namespace {

enum class Mode {
    RGB,
    CMYK,
};

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

void interpolate(f32* component, f32 max_value, i8 start, i8 stop)
{
    // We're creating a uniform (ɑ = 1) Catmull–Rom curve for the missing points.
    // That means that tᵢ₊₁ = tᵢ + 1.
    // Note that component[start] should be interpolated but component[stop] should not.

    // p1 and p2 are set to the ceil value.
    // p0 is set to the last non-max value if possible, otherwise the value of p3 for symmetry.
    // The same logic is applied to p3.
    f32 const p0 = start == 0 ? component[zigzag_map[stop]] : component[zigzag_map[start - 1]];
    f32 const p1 = max_value;
    f32 const p2 = max_value;
    f32 const p3 = stop > 63 ? p0 : component[zigzag_map[stop]];

    f32 const t0 = 0.0f;
    f32 const t1 = 1;
    f32 const t2 = 2;
    f32 const t3 = 3;

    f32 const step = 1. / (stop - start + 1);
    f32 t = t1;
    for (i8 i = start; i < stop; ++i) {
        t += step;
        f32 const A1 = p0 * (t1 - t) / (t1 - t0) + p1 * (t - t0) / (t1 - t0);
        f32 const A2 = p1 * (t2 - t) / (t2 - t1) + p2 * (t - t1) / (t2 - t1);
        f32 const A3 = p2 * (t3 - t) / (t3 - t2) + p3 * (t - t2) / (t3 - t2);
        f32 const B1 = A1 * (t2 - t) / (t2 - t0) + A2 * (t - t0) / (t2 - t0);
        f32 const B2 = A2 * (t3 - t) / (t3 - t1) + A3 * (t - t1) / (t3 - t1);
        f32 const C = B1 * (t2 - t) / (t2 - t1) + B2 * (t - t1) / (t2 - t1);

        component[zigzag_map[i]] = C;
    }
}

class JPEGEncodingContext {
public:
    JPEGEncodingContext(JPEGBigEndianOutputBitStream output_stream)
        : m_bit_stream(move(output_stream))
    {
    }

    ErrorOr<void> initialize_mcu(Bitmap const& bitmap)
    {
        u64 const horizontal_macroblocks = ceil_div(bitmap.width(), 8);
        u64 const vertical_macroblocks = ceil_div(bitmap.height(), 8);
        TRY(m_macroblocks.try_resize(horizontal_macroblocks * vertical_macroblocks));

        for (u16 y {}; y < bitmap.height(); ++y) {
            u16 const vertical_macroblock_index = y / 8;
            u16 const vertical_pixel_offset = y - vertical_macroblock_index * 8;

            for (u16 x {}; x < bitmap.width(); ++x) {
                u16 const horizontal_macroblock_index = x / 8;
                u16 const horizontal_pixel_offset = x - horizontal_macroblock_index * 8;

                auto& macroblock = m_macroblocks[vertical_macroblock_index * horizontal_macroblocks + horizontal_macroblock_index];
                auto const pixel_offset = vertical_pixel_offset * 8 + horizontal_pixel_offset;

                auto const original_pixel = bitmap.get_pixel(x, y);
                macroblock.r[pixel_offset] = original_pixel.red();
                macroblock.g[pixel_offset] = original_pixel.green();
                macroblock.b[pixel_offset] = original_pixel.blue();
            }
        }

        return {};
    }

    ErrorOr<void> initialize_mcu(CMYKBitmap const& bitmap)
    {
        u64 const horizontal_macroblocks = ceil_div(bitmap.size().width(), 8);
        u64 const vertical_macroblocks = ceil_div(bitmap.size().height(), 8);
        TRY(m_macroblocks.try_resize(horizontal_macroblocks * vertical_macroblocks));

        for (u16 y {}; y < bitmap.size().height(); ++y) {
            u16 const vertical_macroblock_index = y / 8;
            u16 const vertical_pixel_offset = y - vertical_macroblock_index * 8;

            for (u16 x {}; x < bitmap.size().width(); ++x) {
                u16 const horizontal_macroblock_index = x / 8;
                u16 const horizontal_pixel_offset = x - horizontal_macroblock_index * 8;

                auto& macroblock = m_macroblocks[vertical_macroblock_index * horizontal_macroblocks + horizontal_macroblock_index];
                auto const pixel_offset = vertical_pixel_offset * 8 + horizontal_pixel_offset;

                auto const original_pixel = bitmap.scanline(y)[x];

                macroblock.r[pixel_offset] = original_pixel.c;
                macroblock.g[pixel_offset] = original_pixel.m;
                macroblock.b[pixel_offset] = original_pixel.y;
                macroblock.k[pixel_offset] = original_pixel.k;
            }
        }

        return {};
    }

    static Array<f32, 64> create_cosine_lookup_table()
    {
        static constexpr f32 pi_over_16 = AK::Pi<f32> / 16;

        Array<f32, 64> table;

        for (u8 u = 0; u < 8; ++u) {
            for (u8 x = 0; x < 8; ++x)
                table[u * 8 + x] = cos((2 * x + 1) * u * pi_over_16);
        }

        return table;
    }

    void convert_to_ycbcr(Mode mode)
    {
        for (auto& macroblock : m_macroblocks) {
            for (u8 i = 0; i < 64; ++i) {
                auto r = macroblock.r[i];
                auto g = macroblock.g[i];
                auto b = macroblock.b[i];

                // Conversion from YCbCr to RGB isn't specified in the first JPEG specification but in the JFIF extension:
                // See: https://www.itu.int/rec/dologin_pub.asp?lang=f&id=T-REC-T.871-201105-I!!PDF-E&type=items
                // 7 - Conversion to and from RGB
                auto const y_ = 0.299f * r + 0.587f * g + 0.114f * b;
                auto const cb = -0.1687f * r - 0.3313f * g + 0.5f * b + 128;
                auto const cr = 0.5f * r - 0.4187f * g - 0.0813f * b + 128;

                // A.3.1 - Level shift
                macroblock.y[i] = y_ - 128;
                macroblock.cb[i] = cb - 128;
                macroblock.cr[i] = cr - 128;

                if (mode == Mode::CMYK) {
                    // To get YCCK, the CMY part is converted to RGB (ignoring the K component), and then the RGB is converted to YCbCr.
                    // r is `255 - c` (and similar for g/m b/y), but with the Adobe YCCK color transform marker, the CMY
                    // channels are stored inverted, which cancels out: 255 - (255 - x) == x.
                    // K is stored as-is (meaning it's inverted once for the color transform).
                    macroblock.k[i] = 255 - macroblock.k[i];
                    // A.3.1 - Level shift
                    macroblock.k[i] -= 128;
                }
            }
        }
    }

    void fdct_and_quantization(Mode mode)
    {
        static auto cosine_table = create_cosine_lookup_table();

        for (auto& macroblock : m_macroblocks) {
            constexpr double inverse_sqrt_2 = M_SQRT1_2;

            auto const convert_one_component = [&](f32 component[], QuantizationTable const& table) {
                Array<f32, 64> result {};

                auto const sum_xy = [&](u8 u, u8 v) {
                    f32 sum {};
                    for (u8 y {}; y < 8; ++y) {
                        for (u8 x {}; x < 8; ++x)
                            sum += component[y * 8 + x] * cosine_table[u * 8 + x] * cosine_table[v * 8 + y];
                    }
                    return sum;
                };

                for (u8 v {}; v < 8; ++v) {
                    f32 const cv = v == 0 ? inverse_sqrt_2 : 1;
                    for (u8 u {}; u < 8; ++u) {
                        auto const table_index = v * 8 + u;

                        f32 const cu = u == 0 ? inverse_sqrt_2 : 1;

                        // A.3.3 - FDCT and IDCT
                        f32 const fdct = cu * cv * sum_xy(u, v) / 4;

                        // A.3.4 - DCT coefficient quantization
                        f32 const quantized = fdct / table.table[table_index];

                        result[table_index] = quantized;
                    }
                }

                for (u8 i {}; i < result.size(); ++i)
                    component[i] = result[i];
            };

            convert_one_component(macroblock.y, m_luminance_quantization_table);
            convert_one_component(macroblock.cb, m_chrominance_quantization_table);
            convert_one_component(macroblock.cr, m_chrominance_quantization_table);
            if (mode == Mode::CMYK)
                convert_one_component(macroblock.k, m_luminance_quantization_table);
        }
    }

    void apply_deringing()
    {
        // The method used here is described at: https://kornel.ski/deringing/.

        for (auto& macroblock : m_macroblocks) {
            for (auto component : { macroblock.r, macroblock.g, macroblock.b }) {
                static constexpr auto maximum_value = NumericLimits<u8>::max();
                Optional<u8> start;
                u8 i = 0;
                for (; i < 64; ++i) {
                    if (component[zigzag_map[i]] == maximum_value) {
                        if (!start.has_value())
                            start = i;
                        else
                            continue;
                    } else {
                        if (start.has_value() && i - *start > 2) {
                            interpolate(component, maximum_value, *start, i);
                        }
                        start.clear();
                    }
                }

                if (start != 0 && component[zigzag_map[63]] == maximum_value)
                    interpolate(component, maximum_value, *start, 64);
            }
        }
    }

    ErrorOr<void> huffman_encode_macroblocks(Mode mode)
    {
        for (auto& float_macroblock : m_macroblocks) {
            auto macroblock = float_macroblock.as_i16();

            for (auto [i, component] : enumerate(to_array({ macroblock.y, macroblock.cb, macroblock.cr, macroblock.k }))) {
                if (mode == Mode::CMYK || i < 3) {
                    TRY(encode_dc(component, i));
                    TRY(encode_ac(component, i));
                }
            }
        }
        m_macroblocks.clear();

        TRY(find_optimal_huffman_tables());
        return {};
    }

    ErrorOr<void> write_huffman_stream()
    {
        TRY(write_symbols_to_stream());
        TRY(m_bit_stream.align_to_byte_boundary(0xFF));
        return {};
    }

    void set_quantization_tables(int quality)
    {
        set_quantization_table(m_luminance_quantization_table, s_default_luminance_quantization_table, quality);
        set_quantization_table(m_chrominance_quantization_table, s_default_chrominance_quantization_table, quality);
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
    struct RawBits {
        u16 bits {};
        u8 length {};
    };
    struct Symbol {
        u8 byte {};
        u8 component_id {};
        bool is_dc {};
    };
    using SymbolOrRawBits = Variant<Symbol, RawBits>;

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

    ErrorOr<void> append_symbol(Symbol symbol)
    {
        auto& stat_table = [&]() -> auto& {
            if (symbol.component_id == 0 || symbol.component_id == 3) {
                return symbol.is_dc ? m_symbol_stats[0] : m_symbol_stats[1];
            }
            return symbol.is_dc ? m_symbol_stats[2] : m_symbol_stats[3];
        }();
        stat_table[symbol.byte] += 1;
        TRY(m_symbols_and_bits.try_append(symbol));
        return {};
    }

    ErrorOr<void> encode_dc(i16 const component[], u8 component_id)
    {
        // F.1.2.1.3 - Huffman encoding procedures for DC coefficients
        auto diff = component[0] - m_last_dc_values[component_id];
        m_last_dc_values[component_id] = component[0];

        auto const size = csize(diff);
        TRY(append_symbol({ .byte = size, .component_id = component_id, .is_dc = true }));

        if (diff < 0)
            diff -= 1;

        TRY(m_symbols_and_bits.try_append(RawBits(diff, size)));
        return {};
    }

    ErrorOr<void> encode_ac(i16 const component[], u8 component_id)
    {
        // F.2 - Procedure for sequential encoding of AC coefficients with Huffman coding
        u32 k {};
        u32 r {};

        while (k < 63) {
            k++;

            auto coefficient = component[zigzag_map[k]];
            if (coefficient == 0) {
                if (k == 63) {
                    TRY(append_symbol({ .byte = 0x00, .component_id = component_id, .is_dc = false }));
                    break;
                }
                r += 1;
                continue;
            }

            while (r > 15) {
                TRY(append_symbol({ .byte = 0xF0, .component_id = component_id, .is_dc = false }));
                r -= 16;
            }

            {
                // F.3 - Sequential encoding of a non-zero AC coefficient
                auto const ssss = csize(coefficient);
                u8 const rs = (r << 4) + ssss;
                TRY(append_symbol({ .byte = rs, .component_id = component_id, .is_dc = false }));

                if (coefficient < 0)
                    coefficient -= 1;

                TRY(m_symbols_and_bits.try_append(RawBits(coefficient, ssss)));
            }

            r = 0;
        }
        return {};
    }

    ErrorOr<void> write_symbols_to_stream()
    {
        for (auto const& symbol_or_bits : m_symbols_and_bits) {
            if (symbol_or_bits.has<Symbol>()) {
                auto symbol = symbol_or_bits.get<Symbol>();
                auto const& huffman_table = [&]() {
                    if (symbol.component_id == 0 || symbol.component_id == 3)
                        return symbol.is_dc ? dc_luminance_huffman_table : ac_luminance_huffman_table;
                    return symbol.is_dc ? dc_chrominance_huffman_table : ac_chrominance_huffman_table;
                }();
                TRY(write_symbol(huffman_table.from_input_byte(symbol.byte)));
            } else {
                auto bits = symbol_or_bits.get<RawBits>();
                TRY(m_bit_stream.write_bits(bits.bits, bits.length));
            }
        }

        return {};
    }

    static void find_smallest_frequencies(Array<u32, 257> const& frequencies, u16& v1, Optional<u16>& v2)
    {
        // FIXME: A min-heap with a custom comparator should be able to do the trick.

        // "The procedure “Find V1 for least value of FREQ(V1) > 0” always selects the value
        // with the largest value of V1 when more than one V1 with the same frequency occurs.
        // The reserved code point is then guaranteed to be in the longest code word category."

        u16 index_min {};
        u16 second_index_min {};
        u32 freq_min = NumericLimits<u32>::max();
        u32 second_freq_min = NumericLimits<u32>::max();

        for (auto [i, freq] : enumerate(frequencies)) {
            if (freq == 0)
                continue;
            if (freq <= freq_min) {
                second_index_min = index_min;
                second_freq_min = freq_min;
                index_min = i;
                freq_min = freq;
            } else if (freq <= second_freq_min) {
                second_index_min = i;
                second_freq_min = freq;
            }
        }

        v1 = index_min;
        if (second_freq_min != NumericLimits<u32>::max())
            v2 = second_index_min;
        else
            v2.clear();
    }

    static Array<u8, 257> find_huffman_code_size(Array<u32, 257> frequencies)
    {
        // "Before starting the procedure, the values of FREQ are collected for V = 0 to 255
        // and the FREQ value for V = 256 is set to 1."
        frequencies[256] = 1;

        // "the entries in CODESIZE are all set to 0"
        Array<u8, 257> code_size {};

        // "the indices in OTHERS are set to –1"
        Array<i16, 257> others {};
        others.fill(-1);

        // Figure K.1 – Procedure to find Huffman code sizes
        while (true) {
            u16 v1 {};
            Optional<u16> maybe_v2 {};
            find_smallest_frequencies(frequencies, v1, maybe_v2);
            if (!maybe_v2.has_value())
                break;

            auto v2 = maybe_v2.value();

            frequencies[v1] += frequencies[v2];
            frequencies[v2] = 0;

        increment_v1_code_size:
            code_size[v1] += 1;

            if (others[v1] != -1) {
                v1 = others[v1];
                goto increment_v1_code_size;
            }

            others[v1] = v2;

        increment_v2_code_size:
            code_size[v2] += 1;
            if (others[v2] != -1) {
                v2 = others[v2];
                goto increment_v2_code_size;
            }
        }

        return code_size;
    }

    static void adjust_bits(Array<u8, 257>& bits)
    {
        // Figure K.3 – Procedure for limiting code lengths to 16 bits
        u16 i = 32;
        while (true) {
            if (bits[i] > 0) {
                auto j = i - 1;
                do {
                    j--;
                } while (bits[j] == 0);

                bits[i] = bits[i] - 2;
                bits[i - 1] = bits[i - 1] + 1;
                bits[j + 1] = bits[j + 1] + 2;
                bits[j] = bits[j] - 1;
            } else {
                i -= 1;
                if (i != 16)
                    continue;

                while (bits[i] == 0)
                    --i;
                bits[i] -= 1;
                break;
            }
        }
    }

    static Array<u8, 257> count_bits(Array<u8, 257> const& code_size)
    {
        // "The count for each size is contained in the list, BITS. The counts in BITS are zero
        // at the start of the procedure."
        Array<u8, 257> bits {};

        // Figure K.2 – Procedure to find the number of codes of each size
        for (u16 i = 0; i < 257; ++i) {
            if (code_size[i] == 0)
                continue;
            bits[code_size[i]] += 1;
        }
        adjust_bits(bits);

        return bits;
    }

    static Vector<u8, 256> sort_input(Array<u8, 257> const& code_size)
    {
        // "Figure K.4 – Sorting of input values according to code size"
        Vector<u8, 256> huffval {};
        for (u8 i = 1; i <= 32; ++i) {
            for (u16 j = 0; j <= 255; ++j) {
                if (code_size[j] == i)
                    huffval.append(j);
            }
        }
        return huffval;
    }

    static ErrorOr<OutputHuffmanTable> compute_optimal_table(Array<u32, 257> const& distribution)
    {
        // K.2 A procedure for generating the lists which specify a Huffman code table

        auto code_size = find_huffman_code_size(distribution);

        auto bits = count_bits(code_size);

        // "The input values are sorted according to code size"
        auto huffval = sort_input(code_size);

        // "At this point, the list of code lengths (BITS) and the list of values
        // (HUFFVAL) can be used to generate the code tables."

        Vector<OutputHuffmanTable::Symbol, 16> symbols;
        u16 code = 0;
        u32 symbol_index = 0;
        for (auto [encoded_size, number_of_codes] : enumerate(bits)) {
            for (u8 i = 0; i < number_of_codes; i++) {
                TRY(symbols.try_append({ .input_byte = huffval[symbol_index], .code_length = static_cast<u8>(encoded_size), .word = code }));
                code++;
                symbol_index++;
            }
            code <<= 1;
        }

        return OutputHuffmanTable { move(symbols) };
    }

    ErrorOr<void> find_optimal_huffman_tables()
    {
        dc_luminance_huffman_table = TRY(compute_optimal_table(m_symbol_stats[0]));
        dc_luminance_huffman_table.id = (0 << 4) | 0;
        ac_luminance_huffman_table = TRY(compute_optimal_table(m_symbol_stats[1]));
        ac_luminance_huffman_table.id = (1 << 4) | 0;
        dc_chrominance_huffman_table = TRY(compute_optimal_table(m_symbol_stats[2]));
        dc_chrominance_huffman_table.id = (0 << 4) | 1;
        ac_chrominance_huffman_table = TRY(compute_optimal_table(m_symbol_stats[3]));
        ac_chrominance_huffman_table.id = (1 << 4) | 1;
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

    Vector<FloatMacroblock> m_macroblocks {};
    Array<i16, 4> m_last_dc_values {};

    Array<Array<u32, 257>, 4> m_symbol_stats {};
    Vector<SymbolOrRawBits> m_symbols_and_bits {};

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

ErrorOr<void> add_icc_data(Stream& stream, ReadonlyBytes icc_data)
{
    // https://www.color.org/technotes/ICC-Technote-ProfileEmbedding.pdf, JFIF section
    constexpr StringView icc_chunk_name = "ICC_PROFILE\0"sv;

    // One JPEG chunk is at most 65535 bytes long, which includes the size of the 2-byte
    // "length" field. This leaves 65533 bytes for the actual data. One ICC chunk needs
    // 12 bytes for the "ICC_PROFILE\0" app id and then one byte each for the current
    // sequence number and the number of ICC chunks. This leaves 65519 bytes for the
    // ICC data.
    constexpr size_t icc_chunk_header_size = 2 + icc_chunk_name.length() + 1 + 1;
    constexpr size_t max_chunk_size = 65535 - icc_chunk_header_size;
    static_assert(max_chunk_size == 65519);

    constexpr size_t max_number_of_icc_chunks = 255; // Chunk IDs are stored in an u8 and start at 1.
    constexpr size_t max_icc_data_size = max_chunk_size * max_number_of_icc_chunks;

    // "The 1-byte chunk count limits the size of embeddable profiles to 16 707 345 bytes.""
    static_assert(max_icc_data_size == 16'707'345);

    if (icc_data.size() > max_icc_data_size)
        return Error::from_string_view("JPEGWriter: icc data too large for jpeg format"sv);

    size_t const number_of_icc_chunks = AK::ceil_div(icc_data.size(), max_chunk_size);
    for (size_t chunk_id = 1; chunk_id <= number_of_icc_chunks; ++chunk_id) {
        size_t const chunk_size = min(icc_data.size(), max_chunk_size);

        TRY(stream.write_value<BigEndian<Marker>>(JPEG_APPN2));
        TRY(stream.write_value<BigEndian<u16>>(icc_chunk_header_size + chunk_size));
        TRY(stream.write_until_depleted(icc_chunk_name.bytes()));
        TRY(stream.write_value<u8>(chunk_id));
        TRY(stream.write_value<u8>(number_of_icc_chunks));
        TRY(stream.write_until_depleted(icc_data.slice(0, chunk_size)));
        icc_data = icc_data.slice(chunk_size);
    }
    VERIFY(icc_data.is_empty());
    return {};
}

ErrorOr<void> add_frame_header(Stream& stream, JPEGEncodingContext const& context, IntSize size, Mode mode)
{
    // B.2.2 - Frame header syntax
    TRY(stream.write_value<BigEndian<Marker>>(JPEG_SOF0));

    u16 const Nf = mode == Mode::CMYK ? 4 : 3;

    // Lf = 8 + 3 × Nf
    TRY(stream.write_value<BigEndian<u16>>(8 + 3 * Nf));

    // P
    TRY(stream.write_value<u8>(8));

    // Y
    TRY(stream.write_value<BigEndian<u16>>(size.height()));

    // X
    TRY(stream.write_value<BigEndian<u16>>(size.width()));

    // Nf
    TRY(stream.write_value<u8>(Nf));

    // Encode Nf components
    for (u8 i {}; i < Nf; ++i) {
        // Ci
        TRY(stream.write_value<u8>(i + 1));

        // Hi and Vi
        TRY(stream.write_value<u8>((1 << 4) | 1));

        // Tqi
        TRY(stream.write_value<u8>((i == 0 || i == 3 ? context.luminance_quantization_table() : context.chrominance_quantization_table()).id));
    }

    return {};
}

ErrorOr<void> add_ycck_color_transform_header(Stream& stream)
{
    // T-REC-T.872-201206-I!!PDF-E.pdf, 6.5.3 APP14 marker segment for colour encoding
    TRY(stream.write_value<BigEndian<Marker>>(JPEG_APPN14));
    TRY(stream.write_value<BigEndian<u16>>(14));

    TRY(stream.write_until_depleted("Adobe\0"sv.bytes()));

    // These values are ignored.
    TRY(stream.write_value<u8>(0x64));
    TRY(stream.write_value<BigEndian<u16>>(0x0000));
    TRY(stream.write_value<BigEndian<u16>>(0x0000));

    // YCCK
    TRY(stream.write_value<u8>(0x2));
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

ErrorOr<void> add_scan_header(Stream& stream, Mode mode)
{
    // B.2.3 - Scan header syntax
    TRY(stream.write_value<BigEndian<Marker>>(JPEG_SOS));

    u16 const Ns = mode == Mode::CMYK ? 4 : 3;

    // Ls - 6 + 2 × Ns
    TRY(stream.write_value<BigEndian<u16>>(6 + 2 * Ns));

    // Ns
    TRY(stream.write_value<u8>(Ns));

    // Encode Ns components
    for (u8 i {}; i < Ns; ++i) {
        // Csj
        TRY(stream.write_value<u8>(i + 1));

        // Tdj and Taj
        // We're using 0 for luminance and 1 for chrominance
        u8 const huffman_identifier = i == 0 || i == 3 ? 0 : 1;
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

ErrorOr<void> add_headers(Stream& stream, JPEGEncodingContext const& context, JPEGWriter::Options const& options, IntSize size, Mode mode)
{
    TRY(add_start_of_image(stream));

    if (options.icc_data.has_value())
        TRY(add_icc_data(stream, options.icc_data.value()));

    if (mode == Mode::CMYK)
        TRY(add_ycck_color_transform_header(stream));
    TRY(add_frame_header(stream, context, size, mode));

    TRY(add_quantization_table(stream, context.luminance_quantization_table()));
    TRY(add_quantization_table(stream, context.chrominance_quantization_table()));

    TRY(add_huffman_table(stream, context.dc_luminance_huffman_table));
    TRY(add_huffman_table(stream, context.dc_chrominance_huffman_table));
    TRY(add_huffman_table(stream, context.ac_luminance_huffman_table));
    TRY(add_huffman_table(stream, context.ac_chrominance_huffman_table));

    TRY(add_scan_header(stream, mode));
    return {};
}

ErrorOr<void> add_image(Stream& stream, JPEGEncodingContext& context, JPEGEncoderOptions const& options, IntSize size, Mode mode)
{
    if (options.use_deringing == JPEGEncoderOptions::UseDeringing::Yes)
        context.apply_deringing();
    context.set_quantization_tables(options.quality);
    context.convert_to_ycbcr(mode);
    context.fdct_and_quantization(mode);
    TRY(context.huffman_encode_macroblocks(mode));
    TRY(add_headers(stream, context, options, size, mode));
    TRY(context.write_huffman_stream());
    TRY(add_end_of_image(stream));
    return {};
}

}

ErrorOr<void> JPEGWriter::encode(Stream& stream, Bitmap const& bitmap, Options const& options)
{
    JPEGEncodingContext context { JPEGBigEndianOutputBitStream { stream } };
    TRY(context.initialize_mcu(bitmap));
    TRY(add_image(stream, context, options, bitmap.size(), Mode::RGB));
    return {};
}

ErrorOr<void> JPEGWriter::encode(Stream& stream, CMYKBitmap const& bitmap, Options const& options)
{
    JPEGEncodingContext context { JPEGBigEndianOutputBitStream { stream } };
    TRY(context.initialize_mcu(bitmap));
    TRY(add_image(stream, context, options, bitmap.size(), Mode::CMYK));
    return {};
}

}
