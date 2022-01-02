/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <AK/Math.h>
#include <AK/MemoryStream.h>
#include <AK/Vector.h>
#include <LibGfx/JPGLoader.h>

#define JPG_INVALID 0X0000

#define JPG_APPN0 0XFFE0
#define JPG_APPN1 0XFFE1
#define JPG_APPN2 0XFFE2
#define JPG_APPN3 0XFFE3
#define JPG_APPN4 0XFFE4
#define JPG_APPN5 0XFFE5
#define JPG_APPN6 0XFFE6
#define JPG_APPN7 0XFFE7
#define JPG_APPN8 0XFFE8
#define JPG_APPN9 0XFFE9
#define JPG_APPNA 0XFFEA
#define JPG_APPNB 0XFFEB
#define JPG_APPNC 0XFFEC
#define JPG_APPND 0XFFED
#define JPG_APPNE 0xFFEE
#define JPG_APPNF 0xFFEF

#define JPG_RESERVED1 0xFFF1
#define JPG_RESERVED2 0xFFF2
#define JPG_RESERVED3 0xFFF3
#define JPG_RESERVED4 0xFFF4
#define JPG_RESERVED5 0xFFF5
#define JPG_RESERVED6 0xFFF6
#define JPG_RESERVED7 0xFFF7
#define JPG_RESERVED8 0xFFF8
#define JPG_RESERVED9 0xFFF9
#define JPG_RESERVEDA 0xFFFA
#define JPG_RESERVEDB 0xFFFB
#define JPG_RESERVEDC 0xFFFC
#define JPG_RESERVEDD 0xFFFD

#define JPG_RST0 0xFFD0
#define JPG_RST1 0xFFD1
#define JPG_RST2 0xFFD2
#define JPG_RST3 0xFFD3
#define JPG_RST4 0xFFD4
#define JPG_RST5 0xFFD5
#define JPG_RST6 0xFFD6
#define JPG_RST7 0xFFD7

#define JPG_DHP 0xFFDE
#define JPG_EXP 0xFFDF

#define JPG_DHT 0XFFC4
#define JPG_DQT 0XFFDB
#define JPG_EOI 0xFFD9
#define JPG_RST 0XFFDD
#define JPG_SOF0 0XFFC0
#define JPG_SOF2 0xFFC2
#define JPG_SOI 0XFFD8
#define JPG_SOS 0XFFDA
#define JPG_COM 0xFFFE

namespace Gfx {

constexpr static u8 zigzag_map[64] {
    0, 1, 8, 16, 9, 2, 3, 10,
    17, 24, 32, 25, 18, 11, 4, 5,
    12, 19, 26, 33, 40, 48, 41, 34,
    27, 20, 13, 6, 7, 14, 21, 28,
    35, 42, 49, 56, 57, 50, 43, 36,
    29, 22, 15, 23, 30, 37, 44, 51,
    58, 59, 52, 45, 38, 31, 39, 46,
    53, 60, 61, 54, 47, 55, 62, 63
};

using Marker = u16;

/**
 * MCU means group of data units that are coded together. A data unit is an 8x8
 * block of component data. In interleaved scans, number of non-interleaved data
 * units of a component C is Ch * Cv, where Ch and Cv represent the horizontal &
 * vertical subsampling factors of the component, respectively. A MacroBlock is
 * an 8x8 block of RGB values before encoding, and 8x8 block of YCbCr values when
 * we're done decoding the huffman stream.
 */
struct Macroblock {
    union {
        i32 y[64] = { 0 };
        i32 r[64];
    };

    union {
        i32 cb[64] = { 0 };
        i32 g[64];
    };

    union {
        i32 cr[64] = { 0 };
        i32 b[64];
    };
};

struct MacroblockMeta {
    u32 total { 0 };
    u32 padded_total { 0 };
    u32 hcount { 0 };
    u32 vcount { 0 };
    u32 hpadded_count { 0 };
    u32 vpadded_count { 0 };
};

struct ComponentSpec {
    u8 id { 0 };
    u8 hsample_factor { 1 }; // Horizontal sampling factor.
    u8 vsample_factor { 1 }; // Vertical sampling factor.
    u8 ac_destination_id { 0 };
    u8 dc_destination_id { 0 };
    u8 qtable_id { 0 }; // Quantization table id.
};

struct StartOfFrame {

    // Of these, only the first 3 are in mainstream use, and refers to SOF0-2.
    enum class FrameType {
        Baseline_DCT = 0,
        Extended_Sequential_DCT = 1,
        Progressive_DCT = 2,
        Sequential_Lossless = 3,
        Differential_Sequential_DCT = 5,
        Differential_Progressive_DCT = 6,
        Differential_Sequential_Lossless = 7,
        Extended_Sequential_DCT_Arithmetic = 9,
        Progressive_DCT_Arithmetic = 10,
        Sequential_Lossless_Arithmetic = 11,
        Differential_Sequential_DCT_Arithmetic = 13,
        Differential_Progressive_DCT_Arithmetic = 14,
        Differential_Sequential_Lossless_Arithmetic = 15,
    };

    FrameType type { FrameType::Baseline_DCT };
    u8 precision { 0 };
    u16 height { 0 };
    u16 width { 0 };
};

struct HuffmanTableSpec {
    u8 type { 0 };
    u8 destination_id { 0 };
    u8 code_counts[16] = { 0 };
    Vector<u8> symbols;
    Vector<u16> codes;
};

struct HuffmanStreamState {
    Vector<u8> stream;
    u8 bit_offset { 0 };
    size_t byte_offset { 0 };
};

struct JPGLoadingContext {
    enum State {
        NotDecoded = 0,
        Error,
        FrameDecoded,
        BitmapDecoded
    };

    State state { State::NotDecoded };
    const u8* data { nullptr };
    size_t data_size { 0 };
    u32 luma_table[64] = { 0 };
    u32 chroma_table[64] = { 0 };
    StartOfFrame frame;
    u8 hsample_factor { 0 };
    u8 vsample_factor { 0 };
    u8 component_count { 0 };
    Vector<ComponentSpec, 3> components;
    RefPtr<Gfx::Bitmap> bitmap;
    u16 dc_reset_interval { 0 };
    HashMap<u8, HuffmanTableSpec> dc_tables;
    HashMap<u8, HuffmanTableSpec> ac_tables;
    HuffmanStreamState huffman_stream;
    i32 previous_dc_values[3] = { 0 };
    MacroblockMeta mblock_meta;
};

static void generate_huffman_codes(HuffmanTableSpec& table)
{
    unsigned code = 0;
    for (auto number_of_codes : table.code_counts) {
        for (int i = 0; i < number_of_codes; i++)
            table.codes.append(code++);
        code <<= 1;
    }
}

static Optional<size_t> read_huffman_bits(HuffmanStreamState& hstream, size_t count = 1)
{
    if (count > (8 * sizeof(size_t))) {
        dbgln_if(JPG_DEBUG, "Can't read {} bits at once!", count);
        return {};
    }
    size_t value = 0;
    while (count--) {
        if (hstream.byte_offset >= hstream.stream.size()) {
            dbgln_if(JPG_DEBUG, "Huffman stream exhausted. This could be an error!");
            return {};
        }
        u8 current_byte = hstream.stream[hstream.byte_offset];
        u8 current_bit = 1u & (u32)(current_byte >> (7 - hstream.bit_offset)); // MSB first.
        hstream.bit_offset++;
        value = (value << 1) | (size_t)current_bit;
        if (hstream.bit_offset == 8) {
            hstream.byte_offset++;
            hstream.bit_offset = 0;
        }
    }
    return value;
}

static Optional<u8> get_next_symbol(HuffmanStreamState& hstream, const HuffmanTableSpec& table)
{
    unsigned code = 0;
    size_t code_cursor = 0;
    for (int i = 0; i < 16; i++) { // Codes can't be longer than 16 bits.
        auto result = read_huffman_bits(hstream);
        if (!result.has_value())
            return {};
        code = (code << 1) | (i32)result.release_value();
        for (int j = 0; j < table.code_counts[i]; j++) {
            if (code == table.codes[code_cursor])
                return table.symbols[code_cursor];
            code_cursor++;
        }
    }

    dbgln_if(JPG_DEBUG, "If you're seeing this...the jpeg decoder needs to support more kinds of JPEGs!");
    return {};
}

static inline i32* get_component(Macroblock& block, unsigned component)
{
    switch (component) {
    case 0:
        return block.y;
    case 1:
        return block.cb;
    default:
        return block.cr;
    }
}

/**
 * Build the macroblocks possible by reading single (MCU) subsampled pair of CbCr.
 * Depending on the sampling factors, we may not see triples of y, cb, cr in that
 * order. If sample factors differ from one, we'll read more than one block of y-
 * coefficients before we get to read a cb-cr block.

 * In the function below, `hcursor` and `vcursor` denote the location of the block
 * we're building in the macroblock matrix. `vfactor_i` and `hfactor_i` are cursors
 * that iterate over the vertical and horizontal subsampling factors, respectively.
 * When we finish one iteration of the innermost loop, we'll have the coefficients
 * of one of the components of block at position `mb_index`. When the outermost loop
 * finishes first iteration, we'll have all the luminance coefficients for all the
 * macroblocks that share the chrominance data. Next two iterations (assuming that
 * we are dealing with three components) will fill up the blocks with chroma data.
 */
static bool build_macroblocks(JPGLoadingContext& context, Vector<Macroblock>& macroblocks, u32 hcursor, u32 vcursor)
{
    for (unsigned component_i = 0; component_i < context.component_count; component_i++) {
        auto& component = context.components[component_i];

        if (component.dc_destination_id >= context.dc_tables.size())
            return false;
        if (component.ac_destination_id >= context.ac_tables.size())
            return false;

        for (u8 vfactor_i = 0; vfactor_i < component.vsample_factor; vfactor_i++) {
            for (u8 hfactor_i = 0; hfactor_i < component.hsample_factor; hfactor_i++) {
                u32 mb_index = (vcursor + vfactor_i) * context.mblock_meta.hpadded_count + (hfactor_i + hcursor);
                Macroblock& block = macroblocks[mb_index];

                auto& dc_table = context.dc_tables.find(component.dc_destination_id)->value;
                auto& ac_table = context.ac_tables.find(component.ac_destination_id)->value;

                auto symbol_or_error = get_next_symbol(context.huffman_stream, dc_table);
                if (!symbol_or_error.has_value())
                    return false;

                // For DC coefficients, symbol encodes the length of the coefficient.
                auto dc_length = symbol_or_error.release_value();
                if (dc_length > 11) {
                    dbgln_if(JPG_DEBUG, "DC coefficient too long: {}!", dc_length);
                    return false;
                }

                auto coeff_or_error = read_huffman_bits(context.huffman_stream, dc_length);
                if (!coeff_or_error.has_value())
                    return false;

                // DC coefficients are encoded as the difference between previous and current DC values.
                i32 dc_diff = coeff_or_error.release_value();

                // If MSB in diff is 0, the difference is -ve. Otherwise +ve.
                if (dc_length != 0 && dc_diff < (1 << (dc_length - 1)))
                    dc_diff -= (1 << dc_length) - 1;

                auto select_component = get_component(block, component_i);
                auto& previous_dc = context.previous_dc_values[component_i];
                select_component[0] = previous_dc += dc_diff;

                // Compute the AC coefficients.
                for (int j = 1; j < 64;) {
                    symbol_or_error = get_next_symbol(context.huffman_stream, ac_table);
                    if (!symbol_or_error.has_value())
                        return false;

                    // AC symbols encode 2 pieces of information, the high 4 bits represent
                    // number of zeroes to be stuffed before reading the coefficient. Low 4
                    // bits represent the magnitude of the coefficient.
                    auto ac_symbol = symbol_or_error.release_value();
                    if (ac_symbol == 0)
                        break;

                    // ac_symbol = 0xF0 means we need to skip 16 zeroes.
                    u8 run_length = ac_symbol == 0xF0 ? 16 : ac_symbol >> 4;
                    j += run_length;

                    if (j >= 64) {
                        dbgln_if(JPG_DEBUG, "Run-length exceeded boundaries. Cursor: {}, Skipping: {}!", j, run_length);
                        return false;
                    }

                    u8 coeff_length = ac_symbol & 0x0F;
                    if (coeff_length > 10) {
                        dbgln_if(JPG_DEBUG, "AC coefficient too long: {}!", coeff_length);
                        return false;
                    }

                    if (coeff_length != 0) {
                        coeff_or_error = read_huffman_bits(context.huffman_stream, coeff_length);
                        if (!coeff_or_error.has_value())
                            return false;
                        i32 ac_coefficient = coeff_or_error.release_value();
                        if (ac_coefficient < (1 << (coeff_length - 1)))
                            ac_coefficient -= (1 << coeff_length) - 1;

                        select_component[zigzag_map[j++]] = ac_coefficient;
                    }
                }
            }
        }
    }

    return true;
}

static Optional<Vector<Macroblock>> decode_huffman_stream(JPGLoadingContext& context)
{
    Vector<Macroblock> macroblocks;
    macroblocks.resize(context.mblock_meta.padded_total);

    if constexpr (JPG_DEBUG) {
        dbgln("Image width: {}", context.frame.width);
        dbgln("Image height: {}", context.frame.height);
        dbgln("Macroblocks in a row: {}", context.mblock_meta.hpadded_count);
        dbgln("Macroblocks in a column: {}", context.mblock_meta.vpadded_count);
        dbgln("Macroblock meta padded total: {}", context.mblock_meta.padded_total);
    }

    // Compute huffman codes for DC and AC tables.
    for (auto it = context.dc_tables.begin(); it != context.dc_tables.end(); ++it)
        generate_huffman_codes(it->value);

    for (auto it = context.ac_tables.begin(); it != context.ac_tables.end(); ++it)
        generate_huffman_codes(it->value);

    for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.vsample_factor) {
        for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.hsample_factor) {
            u32 i = vcursor * context.mblock_meta.hpadded_count + hcursor;
            if (context.dc_reset_interval > 0) {
                if (i % context.dc_reset_interval == 0) {
                    context.previous_dc_values[0] = 0;
                    context.previous_dc_values[1] = 0;
                    context.previous_dc_values[2] = 0;

                    // Restart markers are stored in byte boundaries. Advance the huffman stream cursor to
                    //  the 0th bit of the next byte.
                    if (context.huffman_stream.byte_offset < context.huffman_stream.stream.size()) {
                        if (context.huffman_stream.bit_offset > 0) {
                            context.huffman_stream.bit_offset = 0;
                            context.huffman_stream.byte_offset++;
                        }

                        // Skip the restart marker (RSTn).
                        context.huffman_stream.byte_offset++;
                    }
                }
            }

            if (!build_macroblocks(context, macroblocks, hcursor, vcursor)) {
                if constexpr (JPG_DEBUG) {
                    dbgln("Failed to build Macroblock {}", i);
                    dbgln("Huffman stream byte offset {}", context.huffman_stream.byte_offset);
                    dbgln("Huffman stream bit offset {}", context.huffman_stream.bit_offset);
                }
                return {};
            }
        }
    }

    return macroblocks;
}

static inline bool bounds_okay(const size_t cursor, const size_t delta, const size_t bound)
{
    return (delta + cursor) < bound;
}

static inline bool is_valid_marker(const Marker marker)
{
    if (marker >= JPG_APPN0 && marker <= JPG_APPNF) {

        if (marker != JPG_APPN0)
            dbgln_if(JPG_DEBUG, "{:#04x} not supported yet. The decoder may fail!", marker);
        return true;
    }
    if (marker >= JPG_RESERVED1 && marker <= JPG_RESERVEDD)
        return true;
    if (marker >= JPG_RST0 && marker <= JPG_RST7)
        return true;
    switch (marker) {
    case JPG_COM:
    case JPG_DHP:
    case JPG_EXP:
    case JPG_DHT:
    case JPG_DQT:
    case JPG_RST:
    case JPG_SOF0:
    case JPG_SOI:
    case JPG_SOS:
        return true;
    }

    if (marker >= 0xFFC0 && marker <= 0xFFCF) {
        if (marker != 0xFFC4 && marker != 0xFFC8 && marker != 0xFFCC) {
            dbgln_if(JPG_DEBUG, "Decoding this frame-type (SOF{}) is not currently supported. Decoder will fail!", marker & 0xf);
            return false;
        }
    }

    return false;
}

static inline u16 read_be_word(InputMemoryStream& stream)
{
    BigEndian<u16> tmp;
    stream >> tmp;
    return tmp;
}

static inline Marker read_marker_at_cursor(InputMemoryStream& stream)
{
    u16 marker = read_be_word(stream);
    if (stream.handle_any_error())
        return JPG_INVALID;
    if (is_valid_marker(marker))
        return marker;
    if (marker != 0xFFFF)
        return JPG_INVALID;
    u8 next;
    do {
        stream >> next;
        if (stream.handle_any_error() || next == 0x00)
            return JPG_INVALID;
    } while (next == 0xFF);
    marker = 0xFF00 | (u16)next;
    return is_valid_marker(marker) ? marker : JPG_INVALID;
}

static bool read_start_of_scan(InputMemoryStream& stream, JPGLoadingContext& context)
{
    if (context.state < JPGLoadingContext::State::FrameDecoded) {
        dbgln_if(JPG_DEBUG, "{}: SOS found before reading a SOF!", stream.offset());
        return false;
    }

    u16 bytes_to_read = read_be_word(stream);
    if (stream.handle_any_error())
        return false;
    bytes_to_read -= 2;
    if (!bounds_okay(stream.offset(), bytes_to_read, context.data_size))
        return false;
    u8 component_count = 0;
    stream >> component_count;
    if (stream.handle_any_error())
        return false;
    if (component_count != context.component_count) {
        dbgln_if(JPG_DEBUG, "{}: Unsupported number of components: {}!", stream.offset(), component_count);
        return false;
    }

    for (int i = 0; i < component_count; i++) {
        u8 component_id = 0;
        stream >> component_id;
        if (stream.handle_any_error())
            return false;

        auto& component = context.components[i];
        if (component.id != component_id) {
            dbgln("JPEG decode failed (component.id != component_id)");
            return false;
        }

        u8 table_ids = 0;
        stream >> table_ids;
        if (stream.handle_any_error())
            return false;

        component.dc_destination_id = table_ids >> 4;
        component.ac_destination_id = table_ids & 0x0F;

        if (context.dc_tables.size() != context.ac_tables.size()) {
            dbgln_if(JPG_DEBUG, "{}: DC & AC table count mismatch!", stream.offset());
            return false;
        }

        if (!context.dc_tables.contains(component.dc_destination_id)) {
            dbgln_if(JPG_DEBUG, "DC table (id: {}) does not exist!", component.dc_destination_id);
            return false;
        }

        if (!context.ac_tables.contains(component.ac_destination_id)) {
            dbgln_if(JPG_DEBUG, "AC table (id: {}) does not exist!", component.ac_destination_id);
            return false;
        }
    }

    u8 spectral_selection_start = 0;
    stream >> spectral_selection_start;
    if (stream.handle_any_error())
        return false;
    u8 spectral_selection_end = 0;
    stream >> spectral_selection_end;
    if (stream.handle_any_error())
        return false;
    u8 successive_approximation = 0;
    stream >> successive_approximation;
    if (stream.handle_any_error())
        return false;
    // The three values should be fixed for baseline JPEGs utilizing sequential DCT.
    if (spectral_selection_start != 0 || spectral_selection_end != 63 || successive_approximation != 0) {
        dbgln_if(JPG_DEBUG, "{}: ERROR! Start of Selection: {}, End of Selection: {}, Successive Approximation: {}!",
            stream.offset(),
            spectral_selection_start,
            spectral_selection_end,
            successive_approximation);
        return false;
    }
    return true;
}

static bool read_reset_marker(InputMemoryStream& stream, JPGLoadingContext& context)
{
    u16 bytes_to_read = read_be_word(stream);
    if (stream.handle_any_error())
        return false;
    bytes_to_read -= 2;
    if (bytes_to_read != 2) {
        dbgln_if(JPG_DEBUG, "{}: Malformed reset marker found!", stream.offset());
        return false;
    }
    context.dc_reset_interval = read_be_word(stream);
    if (stream.handle_any_error())
        return false;
    return true;
}

static bool read_huffman_table(InputMemoryStream& stream, JPGLoadingContext& context)
{
    i32 bytes_to_read = read_be_word(stream);
    if (stream.handle_any_error())
        return false;
    if (!bounds_okay(stream.offset(), bytes_to_read, context.data_size))
        return false;
    bytes_to_read -= 2;
    while (bytes_to_read > 0) {
        HuffmanTableSpec table;
        u8 table_info = 0;
        stream >> table_info;
        if (stream.handle_any_error())
            return false;
        u8 table_type = table_info >> 4;
        u8 table_destination_id = table_info & 0x0F;
        if (table_type > 1) {
            dbgln_if(JPG_DEBUG, "{}: Unrecognized huffman table: {}!", stream.offset(), table_type);
            return false;
        }
        if (table_destination_id > 1) {
            dbgln_if(JPG_DEBUG, "{}: Invalid huffman table destination id: {}!", stream.offset(), table_destination_id);
            return false;
        }

        table.type = table_type;
        table.destination_id = table_destination_id;
        u32 total_codes = 0;

        // Read code counts. At each index K, the value represents the number of K+1 bit codes in this header.
        for (int i = 0; i < 16; i++) {
            u8 count = 0;
            stream >> count;
            if (stream.handle_any_error())
                return false;
            total_codes += count;
            table.code_counts[i] = count;
        }

        table.codes.ensure_capacity(total_codes);

        // Read symbols. Read X bytes, where X is the sum of the counts of codes read in the previous step.
        for (u32 i = 0; i < total_codes; i++) {
            u8 symbol = 0;
            stream >> symbol;
            if (stream.handle_any_error())
                return false;
            table.symbols.append(symbol);
        }

        if (stream.handle_any_error())
            return false;

        auto& huffman_table = table.type == 0 ? context.dc_tables : context.ac_tables;
        huffman_table.set(table.destination_id, table);
        VERIFY(huffman_table.size() <= 2);

        bytes_to_read -= 1 + 16 + total_codes;
    }

    if (bytes_to_read != 0) {
        dbgln_if(JPG_DEBUG, "{}: Extra bytes detected in huffman header!", stream.offset());
        return false;
    }
    return true;
}

static inline bool validate_luma_and_modify_context(const ComponentSpec& luma, JPGLoadingContext& context)
{
    if ((luma.hsample_factor == 1 || luma.hsample_factor == 2) && (luma.vsample_factor == 1 || luma.vsample_factor == 2)) {
        context.mblock_meta.hpadded_count += luma.hsample_factor == 1 ? 0 : context.mblock_meta.hcount % 2;
        context.mblock_meta.vpadded_count += luma.vsample_factor == 1 ? 0 : context.mblock_meta.vcount % 2;
        context.mblock_meta.padded_total = context.mblock_meta.hpadded_count * context.mblock_meta.vpadded_count;
        // For easy reference to relevant sample factors.
        context.hsample_factor = luma.hsample_factor;
        context.vsample_factor = luma.vsample_factor;

        if constexpr (JPG_DEBUG) {
            dbgln("Horizontal Subsampling Factor: {}", luma.hsample_factor);
            dbgln("Vertical Subsampling Factor: {}", luma.vsample_factor);
        }

        return true;
    }
    return false;
}

static inline void set_macroblock_metadata(JPGLoadingContext& context)
{
    context.mblock_meta.hcount = (context.frame.width + 7) / 8;
    context.mblock_meta.vcount = (context.frame.height + 7) / 8;
    context.mblock_meta.hpadded_count = context.mblock_meta.hcount;
    context.mblock_meta.vpadded_count = context.mblock_meta.vcount;
    context.mblock_meta.total = context.mblock_meta.hcount * context.mblock_meta.vcount;
}

static bool read_start_of_frame(InputMemoryStream& stream, JPGLoadingContext& context)
{
    if (context.state == JPGLoadingContext::FrameDecoded) {
        dbgln_if(JPG_DEBUG, "{}: SOF repeated!", stream.offset());
        return false;
    }

    i32 bytes_to_read = read_be_word(stream);
    if (stream.handle_any_error())
        return false;

    bytes_to_read -= 2;
    if (!bounds_okay(stream.offset(), bytes_to_read, context.data_size))
        return false;

    stream >> context.frame.precision;
    if (stream.handle_any_error())
        return false;
    if (context.frame.precision != 8) {
        dbgln_if(JPG_DEBUG, "{}: SOF precision != 8!", stream.offset());
        return false;
    }

    context.frame.height = read_be_word(stream);
    if (stream.handle_any_error())
        return false;
    context.frame.width = read_be_word(stream);
    if (stream.handle_any_error())
        return false;
    if (!context.frame.width || !context.frame.height) {
        dbgln_if(JPG_DEBUG, "{}: ERROR! Image height: {}, Image width: {}!", stream.offset(), context.frame.height, context.frame.width);
        return false;
    }

    if (context.frame.width > maximum_width_for_decoded_images || context.frame.height > maximum_height_for_decoded_images) {
        dbgln("This JPEG is too large for comfort: {}x{}", context.frame.width, context.frame.height);
        return false;
    }

    set_macroblock_metadata(context);

    stream >> context.component_count;
    if (stream.handle_any_error())
        return false;
    if (context.component_count != 1 && context.component_count != 3) {
        dbgln_if(JPG_DEBUG, "{}: Unsupported number of components in SOF: {}!", stream.offset(), context.component_count);
        return false;
    }

    for (u8 i = 0; i < context.component_count; i++) {
        ComponentSpec component;

        stream >> component.id;
        if (stream.handle_any_error())
            return false;

        u8 subsample_factors = 0;
        stream >> subsample_factors;
        if (stream.handle_any_error())
            return false;
        component.hsample_factor = subsample_factors >> 4;
        component.vsample_factor = subsample_factors & 0x0F;

        if (i == 0) {
            // By convention, downsampling is applied only on chroma components. So we should
            //  hope to see the maximum sampling factor in the luma component.
            if (!validate_luma_and_modify_context(component, context)) {
                dbgln_if(JPG_DEBUG, "{}: Unsupported luma subsampling factors: horizontal: {}, vertical: {}",
                    stream.offset(),
                    component.hsample_factor,
                    component.vsample_factor);
                return false;
            }
        } else {
            if (component.hsample_factor != 1 || component.vsample_factor != 1) {
                dbgln_if(JPG_DEBUG, "{}: Unsupported chroma subsampling factors: horizontal: {}, vertical: {}",
                    stream.offset(),
                    component.hsample_factor,
                    component.vsample_factor);
                return false;
            }
        }

        stream >> component.qtable_id;
        if (stream.handle_any_error())
            return false;
        if (component.qtable_id > 1) {
            dbgln_if(JPG_DEBUG, "{}: Unsupported quantization table id: {}!", stream.offset(), component.qtable_id);
            return false;
        }

        context.components.append(move(component));
    }

    return true;
}

static bool read_quantization_table(InputMemoryStream& stream, JPGLoadingContext& context)
{
    i32 bytes_to_read = read_be_word(stream);
    if (stream.handle_any_error())
        return false;
    bytes_to_read -= 2;
    if (!bounds_okay(stream.offset(), bytes_to_read, context.data_size))
        return false;
    while (bytes_to_read > 0) {
        u8 info_byte = 0;
        stream >> info_byte;
        if (stream.handle_any_error())
            return false;
        u8 element_unit_hint = info_byte >> 4;
        if (element_unit_hint > 1) {
            dbgln_if(JPG_DEBUG, "{}: Unsupported unit hint in quantization table: {}!", stream.offset(), element_unit_hint);
            return false;
        }
        u8 table_id = info_byte & 0x0F;
        if (table_id > 1) {
            dbgln_if(JPG_DEBUG, "{}: Unsupported quantization table id: {}!", stream.offset(), table_id);
            return false;
        }
        u32* table = table_id == 0 ? context.luma_table : context.chroma_table;
        for (int i = 0; i < 64; i++) {
            if (element_unit_hint == 0) {
                u8 tmp = 0;
                stream >> tmp;
                if (stream.handle_any_error())
                    return false;
                table[zigzag_map[i]] = tmp;
            } else {
                table[zigzag_map[i]] = read_be_word(stream);
                if (stream.handle_any_error())
                    return false;
            }
        }
        if (stream.handle_any_error())
            return false;

        bytes_to_read -= 1 + (element_unit_hint == 0 ? 64 : 128);
    }
    if (bytes_to_read != 0) {
        dbgln_if(JPG_DEBUG, "{}: Invalid length for one or more quantization tables!", stream.offset());
        return false;
    }

    return true;
}

static bool skip_marker_with_length(InputMemoryStream& stream)
{
    u16 bytes_to_skip = read_be_word(stream);
    bytes_to_skip -= 2;
    if (stream.handle_any_error())
        return false;
    stream.discard_or_error(bytes_to_skip);
    return !stream.handle_any_error();
}

static void dequantize(JPGLoadingContext& context, Vector<Macroblock>& macroblocks)
{
    for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.vsample_factor) {
        for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.hsample_factor) {
            for (u32 i = 0; i < context.component_count; i++) {
                auto& component = context.components[i];
                const u32* table = component.qtable_id == 0 ? context.luma_table : context.chroma_table;
                for (u32 vfactor_i = 0; vfactor_i < component.vsample_factor; vfactor_i++) {
                    for (u32 hfactor_i = 0; hfactor_i < component.hsample_factor; hfactor_i++) {
                        u32 mb_index = (vcursor + vfactor_i) * context.mblock_meta.hpadded_count + (hfactor_i + hcursor);
                        Macroblock& block = macroblocks[mb_index];
                        int* block_component = get_component(block, i);
                        for (u32 k = 0; k < 64; k++)
                            block_component[k] *= table[k];
                    }
                }
            }
        }
    }
}

static void inverse_dct(const JPGLoadingContext& context, Vector<Macroblock>& macroblocks)
{
    static const float m0 = 2.0 * AK::cos(1.0 / 16.0 * 2.0 * AK::Pi<double>);
    static const float m1 = 2.0 * AK::cos(2.0 / 16.0 * 2.0 * AK::Pi<double>);
    static const float m3 = 2.0 * AK::cos(2.0 / 16.0 * 2.0 * AK::Pi<double>);
    static const float m5 = 2.0 * AK::cos(3.0 / 16.0 * 2.0 * AK::Pi<double>);
    static const float m2 = m0 - m5;
    static const float m4 = m0 + m5;
    static const float s0 = AK::cos(0.0 / 16.0 * AK::Pi<double>) / sqrt(8);
    static const float s1 = AK::cos(1.0 / 16.0 * AK::Pi<double>) / 2.0;
    static const float s2 = AK::cos(2.0 / 16.0 * AK::Pi<double>) / 2.0;
    static const float s3 = AK::cos(3.0 / 16.0 * AK::Pi<double>) / 2.0;
    static const float s4 = AK::cos(4.0 / 16.0 * AK::Pi<double>) / 2.0;
    static const float s5 = AK::cos(5.0 / 16.0 * AK::Pi<double>) / 2.0;
    static const float s6 = AK::cos(6.0 / 16.0 * AK::Pi<double>) / 2.0;
    static const float s7 = AK::cos(7.0 / 16.0 * AK::Pi<double>) / 2.0;

    for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.vsample_factor) {
        for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.hsample_factor) {
            for (u32 component_i = 0; component_i < context.component_count; component_i++) {
                auto& component = context.components[component_i];
                for (u8 vfactor_i = 0; vfactor_i < component.vsample_factor; vfactor_i++) {
                    for (u8 hfactor_i = 0; hfactor_i < component.hsample_factor; hfactor_i++) {
                        u32 mb_index = (vcursor + vfactor_i) * context.mblock_meta.hpadded_count + (hfactor_i + hcursor);
                        Macroblock& block = macroblocks[mb_index];
                        i32* block_component = get_component(block, component_i);
                        for (u32 k = 0; k < 8; ++k) {
                            const float g0 = block_component[0 * 8 + k] * s0;
                            const float g1 = block_component[4 * 8 + k] * s4;
                            const float g2 = block_component[2 * 8 + k] * s2;
                            const float g3 = block_component[6 * 8 + k] * s6;
                            const float g4 = block_component[5 * 8 + k] * s5;
                            const float g5 = block_component[1 * 8 + k] * s1;
                            const float g6 = block_component[7 * 8 + k] * s7;
                            const float g7 = block_component[3 * 8 + k] * s3;

                            const float f0 = g0;
                            const float f1 = g1;
                            const float f2 = g2;
                            const float f3 = g3;
                            const float f4 = g4 - g7;
                            const float f5 = g5 + g6;
                            const float f6 = g5 - g6;
                            const float f7 = g4 + g7;

                            const float e0 = f0;
                            const float e1 = f1;
                            const float e2 = f2 - f3;
                            const float e3 = f2 + f3;
                            const float e4 = f4;
                            const float e5 = f5 - f7;
                            const float e6 = f6;
                            const float e7 = f5 + f7;
                            const float e8 = f4 + f6;

                            const float d0 = e0;
                            const float d1 = e1;
                            const float d2 = e2 * m1;
                            const float d3 = e3;
                            const float d4 = e4 * m2;
                            const float d5 = e5 * m3;
                            const float d6 = e6 * m4;
                            const float d7 = e7;
                            const float d8 = e8 * m5;

                            const float c0 = d0 + d1;
                            const float c1 = d0 - d1;
                            const float c2 = d2 - d3;
                            const float c3 = d3;
                            const float c4 = d4 + d8;
                            const float c5 = d5 + d7;
                            const float c6 = d6 - d8;
                            const float c7 = d7;
                            const float c8 = c5 - c6;

                            const float b0 = c0 + c3;
                            const float b1 = c1 + c2;
                            const float b2 = c1 - c2;
                            const float b3 = c0 - c3;
                            const float b4 = c4 - c8;
                            const float b5 = c8;
                            const float b6 = c6 - c7;
                            const float b7 = c7;

                            block_component[0 * 8 + k] = b0 + b7;
                            block_component[1 * 8 + k] = b1 + b6;
                            block_component[2 * 8 + k] = b2 + b5;
                            block_component[3 * 8 + k] = b3 + b4;
                            block_component[4 * 8 + k] = b3 - b4;
                            block_component[5 * 8 + k] = b2 - b5;
                            block_component[6 * 8 + k] = b1 - b6;
                            block_component[7 * 8 + k] = b0 - b7;
                        }
                        for (u32 l = 0; l < 8; ++l) {
                            const float g0 = block_component[l * 8 + 0] * s0;
                            const float g1 = block_component[l * 8 + 4] * s4;
                            const float g2 = block_component[l * 8 + 2] * s2;
                            const float g3 = block_component[l * 8 + 6] * s6;
                            const float g4 = block_component[l * 8 + 5] * s5;
                            const float g5 = block_component[l * 8 + 1] * s1;
                            const float g6 = block_component[l * 8 + 7] * s7;
                            const float g7 = block_component[l * 8 + 3] * s3;

                            const float f0 = g0;
                            const float f1 = g1;
                            const float f2 = g2;
                            const float f3 = g3;
                            const float f4 = g4 - g7;
                            const float f5 = g5 + g6;
                            const float f6 = g5 - g6;
                            const float f7 = g4 + g7;

                            const float e0 = f0;
                            const float e1 = f1;
                            const float e2 = f2 - f3;
                            const float e3 = f2 + f3;
                            const float e4 = f4;
                            const float e5 = f5 - f7;
                            const float e6 = f6;
                            const float e7 = f5 + f7;
                            const float e8 = f4 + f6;

                            const float d0 = e0;
                            const float d1 = e1;
                            const float d2 = e2 * m1;
                            const float d3 = e3;
                            const float d4 = e4 * m2;
                            const float d5 = e5 * m3;
                            const float d6 = e6 * m4;
                            const float d7 = e7;
                            const float d8 = e8 * m5;

                            const float c0 = d0 + d1;
                            const float c1 = d0 - d1;
                            const float c2 = d2 - d3;
                            const float c3 = d3;
                            const float c4 = d4 + d8;
                            const float c5 = d5 + d7;
                            const float c6 = d6 - d8;
                            const float c7 = d7;
                            const float c8 = c5 - c6;

                            const float b0 = c0 + c3;
                            const float b1 = c1 + c2;
                            const float b2 = c1 - c2;
                            const float b3 = c0 - c3;
                            const float b4 = c4 - c8;
                            const float b5 = c8;
                            const float b6 = c6 - c7;
                            const float b7 = c7;

                            block_component[l * 8 + 0] = b0 + b7;
                            block_component[l * 8 + 1] = b1 + b6;
                            block_component[l * 8 + 2] = b2 + b5;
                            block_component[l * 8 + 3] = b3 + b4;
                            block_component[l * 8 + 4] = b3 - b4;
                            block_component[l * 8 + 5] = b2 - b5;
                            block_component[l * 8 + 6] = b1 - b6;
                            block_component[l * 8 + 7] = b0 - b7;
                        }
                    }
                }
            }
        }
    }
}

static void ycbcr_to_rgb(const JPGLoadingContext& context, Vector<Macroblock>& macroblocks)
{
    for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.vsample_factor) {
        for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.hsample_factor) {
            const u32 chroma_block_index = vcursor * context.mblock_meta.hpadded_count + hcursor;
            const Macroblock& chroma = macroblocks[chroma_block_index];
            // Overflows are intentional.
            for (u8 vfactor_i = context.vsample_factor - 1; vfactor_i < context.vsample_factor; --vfactor_i) {
                for (u8 hfactor_i = context.hsample_factor - 1; hfactor_i < context.hsample_factor; --hfactor_i) {
                    u32 mb_index = (vcursor + vfactor_i) * context.mblock_meta.hpadded_count + (hcursor + hfactor_i);
                    i32* y = macroblocks[mb_index].y;
                    i32* cb = macroblocks[mb_index].cb;
                    i32* cr = macroblocks[mb_index].cr;
                    for (u8 i = 7; i < 8; --i) {
                        for (u8 j = 7; j < 8; --j) {
                            const u8 pixel = i * 8 + j;
                            const u32 chroma_pxrow = (i / context.vsample_factor) + 4 * vfactor_i;
                            const u32 chroma_pxcol = (j / context.hsample_factor) + 4 * hfactor_i;
                            const u32 chroma_pixel = chroma_pxrow * 8 + chroma_pxcol;
                            int r = y[pixel] + 1.402f * chroma.cr[chroma_pixel] + 128;
                            int g = y[pixel] - 0.344f * chroma.cb[chroma_pixel] - 0.714f * chroma.cr[chroma_pixel] + 128;
                            int b = y[pixel] + 1.772f * chroma.cb[chroma_pixel] + 128;
                            y[pixel] = r < 0 ? 0 : (r > 255 ? 255 : r);
                            cb[pixel] = g < 0 ? 0 : (g > 255 ? 255 : g);
                            cr[pixel] = b < 0 ? 0 : (b > 255 ? 255 : b);
                        }
                    }
                }
            }
        }
    }
}

static bool compose_bitmap(JPGLoadingContext& context, const Vector<Macroblock>& macroblocks)
{
    auto bitmap_or_error = Bitmap::try_create(BitmapFormat::BGRx8888, { context.frame.width, context.frame.height });
    if (bitmap_or_error.is_error())
        return false;
    context.bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
    if (bitmap_or_error.is_error())
        return false;

    for (u32 y = context.frame.height - 1; y < context.frame.height; y--) {
        const u32 block_row = y / 8;
        const u32 pixel_row = y % 8;
        for (u32 x = 0; x < context.frame.width; x++) {
            const u32 block_column = x / 8;
            auto& block = macroblocks[block_row * context.mblock_meta.hpadded_count + block_column];
            const u32 pixel_column = x % 8;
            const u32 pixel_index = pixel_row * 8 + pixel_column;
            const Color color { (u8)block.y[pixel_index], (u8)block.cb[pixel_index], (u8)block.cr[pixel_index] };
            context.bitmap->set_pixel(x, y, color);
        }
    }

    return true;
}

static bool parse_header(InputMemoryStream& stream, JPGLoadingContext& context)
{
    auto marker = read_marker_at_cursor(stream);
    if (stream.handle_any_error())
        return false;
    if (marker != JPG_SOI) {
        dbgln_if(JPG_DEBUG, "{}: SOI not found: {:x}!", stream.offset(), marker);
        return false;
    }
    for (;;) {
        marker = read_marker_at_cursor(stream);
        if (stream.handle_any_error())
            return false;

        // Set frame type if the marker marks a new frame.
        if (marker >= 0xFFC0 && marker <= 0xFFCF) {
            // Ignore interleaved markers.
            if (marker != 0xFFC4 && marker != 0xFFC8 && marker != 0xFFCC) {
                context.frame.type = static_cast<StartOfFrame::FrameType>(marker & 0xF);
            }
        }

        switch (marker) {
        case JPG_INVALID:
        case JPG_RST0:
        case JPG_RST1:
        case JPG_RST2:
        case JPG_RST3:
        case JPG_RST4:
        case JPG_RST5:
        case JPG_RST6:
        case JPG_RST7:
        case JPG_SOI:
        case JPG_EOI:
            dbgln_if(JPG_DEBUG, "{}: Unexpected marker {:x}!", stream.offset(), marker);
            return false;
        case JPG_SOF0:
            if (!read_start_of_frame(stream, context))
                return false;
            context.state = JPGLoadingContext::FrameDecoded;
            break;
        case JPG_DQT:
            if (!read_quantization_table(stream, context))
                return false;
            break;
        case JPG_RST:
            if (!read_reset_marker(stream, context))
                return false;
            break;
        case JPG_DHT:
            if (!read_huffman_table(stream, context))
                return false;
            break;
        case JPG_SOS:
            return read_start_of_scan(stream, context);
        default:
            if (!skip_marker_with_length(stream)) {
                dbgln_if(JPG_DEBUG, "{}: Error skipping marker: {:x}!", stream.offset(), marker);
                return false;
            }
            break;
        }
    }

    VERIFY_NOT_REACHED();
}

static bool scan_huffman_stream(InputMemoryStream& stream, JPGLoadingContext& context)
{
    u8 last_byte;
    u8 current_byte = 0;
    stream >> current_byte;
    if (stream.handle_any_error())
        return false;

    for (;;) {
        last_byte = current_byte;
        stream >> current_byte;
        if (stream.handle_any_error()) {
            dbgln_if(JPG_DEBUG, "{}: EOI not found!", stream.offset());
            return false;
        }

        if (last_byte == 0xFF) {
            if (current_byte == 0xFF)
                continue;
            if (current_byte == 0x00) {
                stream >> current_byte;
                if (stream.handle_any_error())
                    return false;
                context.huffman_stream.stream.append(last_byte);
                continue;
            }
            Marker marker = 0xFF00 | current_byte;
            if (marker == JPG_EOI)
                return true;
            if (marker >= JPG_RST0 && marker <= JPG_RST7) {
                context.huffman_stream.stream.append(marker);
                stream >> current_byte;
                if (stream.handle_any_error())
                    return false;
                continue;
            }
            dbgln_if(JPG_DEBUG, "{}: Invalid marker: {:x}!", stream.offset(), marker);
            return false;
        } else {
            context.huffman_stream.stream.append(last_byte);
        }
    }

    VERIFY_NOT_REACHED();
}

static bool decode_jpg(JPGLoadingContext& context)
{
    InputMemoryStream stream { { context.data, context.data_size } };

    if (!parse_header(stream, context))
        return false;
    if (!scan_huffman_stream(stream, context))
        return false;

    auto result = decode_huffman_stream(context);
    if (!result.has_value()) {
        dbgln_if(JPG_DEBUG, "{}: Failed to decode Macroblocks!", stream.offset());
        return false;
    }

    auto macroblocks = result.release_value();
    dequantize(context, macroblocks);
    inverse_dct(context, macroblocks);
    ycbcr_to_rgb(context, macroblocks);
    if (!compose_bitmap(context, macroblocks))
        return false;
    return true;
}

JPGImageDecoderPlugin::JPGImageDecoderPlugin(const u8* data, size_t size)
{
    m_context = make<JPGLoadingContext>();
    m_context->data = data;
    m_context->data_size = size;
    m_context->huffman_stream.stream.ensure_capacity(50 * KiB);
}

JPGImageDecoderPlugin::~JPGImageDecoderPlugin()
{
}

IntSize JPGImageDecoderPlugin::size()
{
    if (m_context->state == JPGLoadingContext::State::Error)
        return {};
    if (m_context->state >= JPGLoadingContext::State::FrameDecoded)
        return { m_context->frame.width, m_context->frame.height };

    return {};
}

void JPGImageDecoderPlugin::set_volatile()
{
    if (m_context->bitmap)
        m_context->bitmap->set_volatile();
}

bool JPGImageDecoderPlugin::set_nonvolatile(bool& was_purged)
{
    if (!m_context->bitmap)
        return false;
    return m_context->bitmap->set_nonvolatile(was_purged);
}

bool JPGImageDecoderPlugin::sniff()
{
    return m_context->data_size > 3
        && m_context->data[0] == 0xFF
        && m_context->data[1] == 0xD8
        && m_context->data[2] == 0xFF;
}

bool JPGImageDecoderPlugin::is_animated()
{
    return false;
}

size_t JPGImageDecoderPlugin::loop_count()
{
    return 0;
}

size_t JPGImageDecoderPlugin::frame_count()
{
    return 1;
}

ErrorOr<ImageFrameDescriptor> JPGImageDecoderPlugin::frame(size_t index)
{
    if (index > 0)
        return Error::from_string_literal("JPGImageDecoderPlugin: Invalid frame index"sv);

    if (m_context->state == JPGLoadingContext::State::Error)
        return Error::from_string_literal("JPGImageDecoderPlugin: Decoding failed"sv);

    if (m_context->state < JPGLoadingContext::State::BitmapDecoded) {
        if (!decode_jpg(*m_context)) {
            m_context->state = JPGLoadingContext::State::Error;
            return Error::from_string_literal("JPGImageDecoderPlugin: Decoding failed"sv);
        }
        m_context->state = JPGLoadingContext::State::BitmapDecoded;
    }

    return ImageFrameDescriptor { m_context->bitmap, 0 };
}

}
