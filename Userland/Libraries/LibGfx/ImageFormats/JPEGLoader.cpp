/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/Error.h>
#include <AK/FixedArray.h>
#include <AK/HashMap.h>
#include <AK/Math.h>
#include <AK/MemoryStream.h>
#include <AK/String.h>
#include <AK/Try.h>
#include <AK/Vector.h>
#include <LibGfx/ImageFormats/JPEGLoader.h>

#define JPEG_INVALID 0X0000

// These names are defined in B.1.1.3 - Marker assignments

#define JPEG_APPN0 0XFFE0
#define JPEG_APPN1 0XFFE1
#define JPEG_APPN2 0XFFE2
#define JPEG_APPN3 0XFFE3
#define JPEG_APPN4 0XFFE4
#define JPEG_APPN5 0XFFE5
#define JPEG_APPN6 0XFFE6
#define JPEG_APPN7 0XFFE7
#define JPEG_APPN8 0XFFE8
#define JPEG_APPN9 0XFFE9
#define JPEG_APPN10 0XFFEA
#define JPEG_APPN11 0XFFEB
#define JPEG_APPN12 0XFFEC
#define JPEG_APPN13 0XFFED
#define JPEG_APPN14 0xFFEE
#define JPEG_APPN15 0xFFEF

#define JPEG_RESERVED1 0xFFF1
#define JPEG_RESERVED2 0xFFF2
#define JPEG_RESERVED3 0xFFF3
#define JPEG_RESERVED4 0xFFF4
#define JPEG_RESERVED5 0xFFF5
#define JPEG_RESERVED6 0xFFF6
#define JPEG_RESERVED7 0xFFF7
#define JPEG_RESERVED8 0xFFF8
#define JPEG_RESERVED9 0xFFF9
#define JPEG_RESERVEDA 0xFFFA
#define JPEG_RESERVEDB 0xFFFB
#define JPEG_RESERVEDC 0xFFFC
#define JPEG_RESERVEDD 0xFFFD

#define JPEG_RST0 0xFFD0
#define JPEG_RST1 0xFFD1
#define JPEG_RST2 0xFFD2
#define JPEG_RST3 0xFFD3
#define JPEG_RST4 0xFFD4
#define JPEG_RST5 0xFFD5
#define JPEG_RST6 0xFFD6
#define JPEG_RST7 0xFFD7

#define JPEG_ZRL 0xF0

#define JPEG_DHP 0xFFDE
#define JPEG_EXP 0xFFDF

#define JPEG_DAC 0XFFCC
#define JPEG_DHT 0XFFC4
#define JPEG_DQT 0XFFDB
#define JPEG_EOI 0xFFD9
#define JPEG_DRI 0XFFDD
#define JPEG_SOF0 0XFFC0
#define JPEG_SOF2 0xFFC2
#define JPEG_SOF15 0xFFCF
#define JPEG_SOI 0XFFD8
#define JPEG_SOS 0XFFDA
#define JPEG_COM 0xFFFE

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
        i16 y[64] = { 0 };
        i16 r[64];
    };

    union {
        i16 cb[64] = { 0 };
        i16 g[64];
    };

    union {
        i16 cr[64] = { 0 };
        i16 b[64];
    };

    i16 k[64] = { 0 };
};

struct MacroblockMeta {
    u32 total { 0 };
    u32 padded_total { 0 };
    u32 hcount { 0 };
    u32 vcount { 0 };
    u32 hpadded_count { 0 };
    u32 vpadded_count { 0 };
};

// In the JPEG format, components are defined first at the frame level, then
// referenced in each scan and aggregated with scan-specific information. The
// two following structs mimic this hierarchy.

struct Component {
    // B.2.2 - Frame header syntax
    u8 id { 0 };             // Ci, Component identifier
    u8 hsample_factor { 1 }; // Hi, Horizontal sampling factor
    u8 vsample_factor { 1 }; // Vi, Vertical sampling factor
    u8 qtable_id { 0 };      // Tqi, Quantization table destination selector

    // The JPEG specification does not specify which component corresponds to
    // Y, Cb or Cr. This field (actually the index in the parent Vector) will
    // act as an authority to determine the *real* component.
    // Please note that this is implementation specific.
    u8 index { 0 };
};

struct ScanComponent {
    // B.2.3 - Scan header syntax
    Component& component;
    u8 dc_destination_id { 0 }; // Tdj, DC entropy coding table destination selector
    u8 ac_destination_id { 0 }; // Taj, AC entropy coding table destination selector
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

struct ICCMultiChunkState {
    u8 seen_number_of_icc_chunks { 0 };
    FixedArray<ByteBuffer> chunks;
};

struct Scan {
    // B.2.3 - Scan header syntax
    Vector<ScanComponent, 4> components;

    u8 spectral_selection_start {};      // Ss
    u8 spectral_selection_end {};        // Se
    u8 successive_approximation_high {}; // Ah
    u8 successive_approximation_low {};  // Al

    HuffmanStreamState huffman_stream;

    u64 end_of_bands_run_count { 0 };

    // See the note on Figure B.4 - Scan header syntax
    bool are_components_interleaved() const
    {
        return components.size() != 1;
    }
};

enum class ColorTransform {
    // https://www.itu.int/rec/dologin_pub.asp?lang=e&id=T-REC-T.872-201206-I!!PDF-E&type=items
    // 6.5.3 - APP14 marker segment for colour encoding
    CmykOrRgb = 0,
    YCbCr = 1,
    YCCK = 2,
};

struct JPEGLoadingContext {
    enum State {
        NotDecoded = 0,
        Error,
        FrameDecoded,
        HeaderDecoded,
        BitmapDecoded
    };

    State state { State::NotDecoded };

    u32 luma_table[64] = { 0 };
    u32 chroma_table[64] = { 0 };
    StartOfFrame frame;
    u8 hsample_factor { 0 };
    u8 vsample_factor { 0 };

    Scan current_scan;

    Vector<Component, 4> components;
    RefPtr<Gfx::Bitmap> bitmap;
    u16 dc_restart_interval { 0 };
    HashMap<u8, HuffmanTableSpec> dc_tables;
    HashMap<u8, HuffmanTableSpec> ac_tables;
    Array<i32, 4> previous_dc_values {};
    MacroblockMeta mblock_meta;
    OwnPtr<FixedMemoryStream> stream;

    Optional<ColorTransform> color_transform {};

    Optional<ICCMultiChunkState> icc_multi_chunk_state;
    Optional<ByteBuffer> icc_data;
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

static ErrorOr<size_t> read_huffman_bits(HuffmanStreamState& hstream, size_t count = 1)
{
    if (count > (8 * sizeof(size_t))) {
        dbgln_if(JPEG_DEBUG, "Can't read {} bits at once!", count);
        return Error::from_string_literal("Reading too much huffman bits at once");
    }
    size_t value = 0;
    while (count--) {
        if (hstream.byte_offset >= hstream.stream.size()) {
            dbgln_if(JPEG_DEBUG, "Huffman stream exhausted. This could be an error!");
            return Error::from_string_literal("Huffman stream exhausted.");
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

static ErrorOr<u8> get_next_symbol(HuffmanStreamState& hstream, HuffmanTableSpec const& table)
{
    unsigned code = 0;
    size_t code_cursor = 0;
    for (int i = 0; i < 16; i++) { // Codes can't be longer than 16 bits.
        auto result = TRY(read_huffman_bits(hstream));
        code = (code << 1) | (i32)result;
        for (int j = 0; j < table.code_counts[i]; j++) {
            if (code == table.codes[code_cursor])
                return table.symbols[code_cursor];
            code_cursor++;
        }
    }

    dbgln_if(JPEG_DEBUG, "If you're seeing this...the jpeg decoder needs to support more kinds of JPEGs!");
    return Error::from_string_literal("This kind of JPEG is not yet supported by the decoder");
}

static inline auto* get_component(Macroblock& block, unsigned component)
{
    switch (component) {
    case 0:
        return block.y;
    case 1:
        return block.cb;
    case 2:
        return block.cr;
    case 3:
        return block.k;
    default:
        VERIFY_NOT_REACHED();
    }
}

static ErrorOr<void> refine_coefficient(Scan& scan, auto& coefficient)
{
    // G.1.2.3 - Coding model for subsequent scans of successive approximation
    // See the correction bit from rule b.
    u8 const bit = TRY(read_huffman_bits(scan.huffman_stream, 1));
    if (bit == 1)
        coefficient |= 1 << scan.successive_approximation_low;

    return {};
}

static ErrorOr<void> add_dc(JPEGLoadingContext& context, Macroblock& macroblock, ScanComponent const& scan_component)
{
    auto maybe_table = context.dc_tables.get(scan_component.dc_destination_id);
    if (!maybe_table.has_value()) {
        dbgln_if(JPEG_DEBUG, "Unable to find a DC table with id: {}", scan_component.dc_destination_id);
        return Error::from_string_literal("Unable to find corresponding DC table");
    }

    auto& dc_table = maybe_table.value();
    auto& scan = context.current_scan;

    auto* select_component = get_component(macroblock, scan_component.component.index);
    auto& coefficient = select_component[0];

    if (context.current_scan.successive_approximation_high > 0) {
        TRY(refine_coefficient(scan, coefficient));
        return {};
    }

    // For DC coefficients, symbol encodes the length of the coefficient.
    auto dc_length = TRY(get_next_symbol(scan.huffman_stream, dc_table));
    if (dc_length > 11) {
        dbgln_if(JPEG_DEBUG, "DC coefficient too long: {}!", dc_length);
        return Error::from_string_literal("DC coefficient too long");
    }

    // DC coefficients are encoded as the difference between previous and current DC values.
    i32 dc_diff = TRY(read_huffman_bits(scan.huffman_stream, dc_length));

    // If MSB in diff is 0, the difference is -ve. Otherwise +ve.
    if (dc_length != 0 && dc_diff < (1 << (dc_length - 1)))
        dc_diff -= (1 << dc_length) - 1;

    auto& previous_dc = context.previous_dc_values[scan_component.component.index];
    previous_dc += dc_diff;
    coefficient = previous_dc << scan.successive_approximation_low;

    return {};
}

static ErrorOr<bool> read_eob(Scan& scan, u32 symbol)
{
    // G.1.2.2 - Progressive encoding of AC coefficients with Huffman coding
    // Note: We also use it for non-progressive encoding as it supports both EOB and ZRL

    if (auto const eob = symbol & 0x0F; eob == 0 && symbol != JPEG_ZRL) {
        // We encountered an EOB marker
        auto const eob_base = symbol >> 4;
        auto const additional_value = TRY(read_huffman_bits(scan.huffman_stream, eob_base));

        scan.end_of_bands_run_count = additional_value + (1 << eob_base) - 1;

        // end_of_bands_run_count is decremented at the end of `build_macroblocks`.
        // And we need to now that we reached End of Block in `add_ac`.
        ++scan.end_of_bands_run_count;

        return true;
    }

    return false;
}

static bool is_progressive(StartOfFrame::FrameType frame_type)
{
    return frame_type == StartOfFrame::FrameType::Progressive_DCT
        || frame_type == StartOfFrame::FrameType::Progressive_DCT_Arithmetic
        || frame_type == StartOfFrame::FrameType::Differential_Progressive_DCT
        || frame_type == StartOfFrame::FrameType::Differential_Progressive_DCT_Arithmetic;
}

static ErrorOr<void> add_ac(JPEGLoadingContext& context, Macroblock& macroblock, ScanComponent const& scan_component)
{
    auto maybe_table = context.ac_tables.get(scan_component.ac_destination_id);
    if (!maybe_table.has_value()) {
        dbgln_if(JPEG_DEBUG, "Unable to find a AC table with id: {}", scan_component.ac_destination_id);
        return Error::from_string_literal("Unable to find corresponding AC table");
    }

    auto& ac_table = maybe_table.value();
    auto* select_component = get_component(macroblock, scan_component.component.index);

    auto& scan = context.current_scan;

    // Compute the AC coefficients.

    // 0th coefficient is the dc, which is already handled
    auto first_coefficient = max(1, scan.spectral_selection_start);

    u32 to_skip = 0;
    Optional<u8> saved_symbol;
    Optional<u8> saved_bit_for_rule_a;
    bool in_zrl = false;

    for (int j = first_coefficient; j <= scan.spectral_selection_end; ++j) {
        auto& coefficient = select_component[zigzag_map[j]];

        // AC symbols encode 2 pieces of information, the high 4 bits represent
        // number of zeroes to be stuffed before reading the coefficient. Low 4
        // bits represent the magnitude of the coefficient.
        if (!in_zrl && scan.end_of_bands_run_count == 0 && !saved_symbol.has_value()) {
            saved_symbol = TRY(get_next_symbol(scan.huffman_stream, ac_table));

            if (!TRY(read_eob(scan, *saved_symbol))) {
                to_skip = *saved_symbol >> 4;

                in_zrl = *saved_symbol == JPEG_ZRL;
                if (in_zrl) {
                    to_skip++;
                    saved_symbol.clear();
                }

                if (!in_zrl && is_progressive(context.frame.type) && scan.successive_approximation_high != 0) {
                    // G.1.2.3 - Coding model for subsequent scans of successive approximation
                    // Bit sign from rule a
                    saved_bit_for_rule_a = TRY(read_huffman_bits(scan.huffman_stream, 1));
                }
            }
        }

        if (coefficient != 0) {
            TRY(refine_coefficient(scan, coefficient));
            continue;
        }

        if (to_skip > 0) {
            --to_skip;
            if (to_skip == 0)
                in_zrl = false;
            continue;
        }

        if (scan.end_of_bands_run_count > 0)
            continue;

        if (is_progressive(context.frame.type) && scan.successive_approximation_high != 0) {
            // G.1.2.3 - Coding model for subsequent scans of successive approximation
            if (auto const low_bits = *saved_symbol & 0x0F; low_bits != 1) {
                dbgln_if(JPEG_DEBUG, "AC coefficient low bits isn't equal to 1: {}!", low_bits);
                return Error::from_string_literal("AC coefficient low bits isn't equal to 1");
            }

            coefficient = (*saved_bit_for_rule_a == 0 ? -1 : 1) << scan.successive_approximation_low;
            saved_bit_for_rule_a.clear();
        } else {
            // F.1.2.2 - Huffman encoding of AC coefficients
            u8 const coeff_length = *saved_symbol & 0x0F;

            if (coeff_length > 10) {
                dbgln_if(JPEG_DEBUG, "AC coefficient too long: {}!", coeff_length);
                return Error::from_string_literal("AC coefficient too long");
            }

            if (coeff_length != 0) {
                i32 ac_coefficient = TRY(read_huffman_bits(scan.huffman_stream, coeff_length));
                if (ac_coefficient < (1 << (coeff_length - 1)))
                    ac_coefficient -= (1 << coeff_length) - 1;

                coefficient = ac_coefficient * (1 << scan.successive_approximation_low);
            }
        }

        saved_symbol.clear();
    }

    if (to_skip > 0) {
        dbgln_if(JPEG_DEBUG, "Run-length exceeded boundaries. Cursor: {}, Skipping: {}!", scan.spectral_selection_end + to_skip, to_skip);
        return Error::from_string_literal("Run-length exceeded boundaries");
    }

    return {};
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
 * of one of the components of block at position `macroblock_index`. When the outermost
 * loop finishes first iteration, we'll have all the luminance coefficients for all the
 * macroblocks that share the chrominance data. Next two iterations (assuming that
 * we are dealing with three components) will fill up the blocks with chroma data.
 */
static ErrorOr<void> build_macroblocks(JPEGLoadingContext& context, Vector<Macroblock>& macroblocks, u32 hcursor, u32 vcursor)
{
    for (auto const& scan_component : context.current_scan.components) {
        for (u8 vfactor_i = 0; vfactor_i < scan_component.component.vsample_factor; vfactor_i++) {
            for (u8 hfactor_i = 0; hfactor_i < scan_component.component.hsample_factor; hfactor_i++) {
                // A.2.3 - Interleaved order
                u32 macroblock_index = (vcursor + vfactor_i) * context.mblock_meta.hpadded_count + (hfactor_i + hcursor);
                if (!context.current_scan.are_components_interleaved()) {
                    macroblock_index = vcursor * context.mblock_meta.hpadded_count + (hfactor_i + (hcursor * scan_component.component.vsample_factor) + (vfactor_i * scan_component.component.hsample_factor));

                    // A.2.4 Completion of partial MCU
                    // If the component is [and only if!] to be interleaved, the encoding process
                    // shall also extend the number of samples by one or more additional blocks.

                    // Horizontally
                    if (macroblock_index >= context.mblock_meta.hcount && macroblock_index % context.mblock_meta.hpadded_count >= context.mblock_meta.hcount)
                        continue;
                    // Vertically
                    if (macroblock_index >= context.mblock_meta.hpadded_count * context.mblock_meta.vcount)
                        continue;
                }

                Macroblock& block = macroblocks[macroblock_index];

                if (context.current_scan.spectral_selection_start == 0)
                    TRY(add_dc(context, block, scan_component));
                if (context.current_scan.spectral_selection_end != 0)
                    TRY(add_ac(context, block, scan_component));

                // G.1.2.2 - Progressive encoding of AC coefficients with Huffman coding
                if (context.current_scan.end_of_bands_run_count > 0) {
                    --context.current_scan.end_of_bands_run_count;
                    continue;
                }
            }
        }
    }

    return {};
}

static bool is_dct_based(StartOfFrame::FrameType frame_type)
{
    return frame_type == StartOfFrame::FrameType::Baseline_DCT
        || frame_type == StartOfFrame::FrameType::Extended_Sequential_DCT
        || frame_type == StartOfFrame::FrameType::Progressive_DCT
        || frame_type == StartOfFrame::FrameType::Differential_Sequential_DCT
        || frame_type == StartOfFrame::FrameType::Differential_Progressive_DCT
        || frame_type == StartOfFrame::FrameType::Progressive_DCT_Arithmetic
        || frame_type == StartOfFrame::FrameType::Differential_Sequential_DCT_Arithmetic
        || frame_type == StartOfFrame::FrameType::Differential_Progressive_DCT_Arithmetic;
}

static void reset_decoder(JPEGLoadingContext& context)
{
    // G.1.2.2 - Progressive encoding of AC coefficients with Huffman coding
    context.current_scan.end_of_bands_run_count = 0;

    // E.2.4 Control procedure for decoding a restart interval
    if (is_dct_based(context.frame.type)) {
        context.previous_dc_values = {};
        return;
    }

    VERIFY_NOT_REACHED();
}

static ErrorOr<void> decode_huffman_stream(JPEGLoadingContext& context, Vector<Macroblock>& macroblocks)
{
    // Compute huffman codes for DC and AC tables.
    for (auto it = context.dc_tables.begin(); it != context.dc_tables.end(); ++it)
        generate_huffman_codes(it->value);

    for (auto it = context.ac_tables.begin(); it != context.ac_tables.end(); ++it)
        generate_huffman_codes(it->value);

    for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.vsample_factor) {
        for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.hsample_factor) {
            u32 i = vcursor * context.mblock_meta.hpadded_count + hcursor;

            auto& huffman_stream = context.current_scan.huffman_stream;

            if (context.dc_restart_interval > 0) {
                if (i != 0 && i % (context.dc_restart_interval * context.vsample_factor * context.hsample_factor) == 0) {
                    reset_decoder(context);

                    // Restart markers are stored in byte boundaries. Advance the huffman stream cursor to
                    //  the 0th bit of the next byte.
                    if (huffman_stream.byte_offset < huffman_stream.stream.size()) {
                        if (huffman_stream.bit_offset > 0) {
                            huffman_stream.bit_offset = 0;
                            huffman_stream.byte_offset++;
                        }

                        // Skip the restart marker (RSTn).
                        huffman_stream.byte_offset++;
                    }
                }
            }

            if (auto result = build_macroblocks(context, macroblocks, hcursor, vcursor); result.is_error()) {
                if constexpr (JPEG_DEBUG) {
                    dbgln("Failed to build Macroblock {}: {}", i, result.error());
                    dbgln("Huffman stream byte offset {}", huffman_stream.byte_offset);
                    dbgln("Huffman stream bit offset {}", huffman_stream.bit_offset);
                }
                return result.release_error();
            }
        }
    }
    return {};
}

static bool is_frame_marker(Marker const marker)
{
    // B.1.1.3 - Marker assignments
    bool const is_sof_marker = marker >= JPEG_SOF0 && marker <= JPEG_SOF15;

    // Start of frame markers are valid for JPEG_SOF0 to JPEG_SOF15 except number 4, 8 (reserved) and 12.
    bool const is_defined_marker = marker != JPEG_DHT && marker != 0xFFC8 && marker != JPEG_DAC;

    return is_sof_marker && is_defined_marker;
}

static inline bool is_supported_marker(Marker const marker)
{
    if (marker >= JPEG_APPN0 && marker <= JPEG_APPN15) {

        if (marker != JPEG_APPN0 && marker != JPEG_APPN14)
            dbgln_if(JPEG_DEBUG, "{:#04x} not supported yet. The decoder may fail!", marker);
        return true;
    }
    if (marker >= JPEG_RESERVED1 && marker <= JPEG_RESERVEDD)
        return true;
    if (marker >= JPEG_RST0 && marker <= JPEG_RST7)
        return true;
    switch (marker) {
    case JPEG_COM:
    case JPEG_DHP:
    case JPEG_EXP:
    case JPEG_DHT:
    case JPEG_DQT:
    case JPEG_DRI:
    case JPEG_EOI:
    case JPEG_SOF0:
    case JPEG_SOF2:
    case JPEG_SOI:
    case JPEG_SOS:
        return true;
    }

    if (is_frame_marker(marker))
        dbgln_if(JPEG_DEBUG, "Decoding this frame-type (SOF{}) is not currently supported. Decoder will fail!", marker & 0xf);

    return false;
}

static inline ErrorOr<Marker> read_marker_at_cursor(Stream& stream)
{
    u16 marker = TRY(stream.read_value<BigEndian<u16>>());
    if (is_supported_marker(marker))
        return marker;
    if (marker != 0xFFFF)
        return JPEG_INVALID;
    u8 next;
    do {
        next = TRY(stream.read_value<u8>());
        if (next == 0x00)
            return JPEG_INVALID;
    } while (next == 0xFF);
    marker = 0xFF00 | (u16)next;
    return is_supported_marker(marker) ? marker : JPEG_INVALID;
}

static ErrorOr<void> read_start_of_scan(Stream& stream, JPEGLoadingContext& context)
{
    // B.2.3 - Scan header syntax

    if (context.state < JPEGLoadingContext::State::FrameDecoded)
        return Error::from_string_literal("SOS found before reading a SOF");

    [[maybe_unused]] u16 const bytes_to_read = TRY(stream.read_value<BigEndian<u16>>()) - 2;
    u8 const component_count = TRY(stream.read_value<u8>());

    Scan current_scan;
    current_scan.huffman_stream.stream.ensure_capacity(50 * KiB);

    Optional<u8> last_read;
    u8 component_read = 0;
    for (auto& component : context.components) {
        // See the Csj paragraph:
        // [...] the ordering in the scan header shall follow the ordering in the frame header.
        if (component_read == component_count)
            break;

        if (!last_read.has_value())
            last_read = TRY(stream.read_value<u8>());

        if (component.id != *last_read)
            continue;

        u8 table_ids = TRY(stream.read_value<u8>());

        current_scan.components.empend(component, static_cast<u8>(table_ids >> 4), static_cast<u8>(table_ids & 0x0F));

        component_read++;
        last_read.clear();
    }

    if constexpr (JPEG_DEBUG) {
        StringBuilder builder;
        TRY(builder.try_append("Components in scan: "sv));
        for (auto const& scan_component : current_scan.components) {
            TRY(builder.try_append(TRY(String::number(scan_component.component.id))));
            TRY(builder.try_append(' '));
        }
        dbgln(builder.string_view());
    }

    current_scan.spectral_selection_start = TRY(stream.read_value<u8>());
    current_scan.spectral_selection_end = TRY(stream.read_value<u8>());
    auto const successive_approximation = TRY(stream.read_value<u8>());
    current_scan.successive_approximation_high = successive_approximation >> 4;
    current_scan.successive_approximation_low = successive_approximation & 0x0F;

    dbgln_if(JPEG_DEBUG, "Start of Selection: {}, End of Selection: {}, Successive Approximation High: {}, Successive Approximation Low: {}",
        current_scan.spectral_selection_start,
        current_scan.spectral_selection_end,
        current_scan.successive_approximation_high,
        current_scan.successive_approximation_low);

    if (current_scan.spectral_selection_start > 63 || current_scan.spectral_selection_end > 63 || current_scan.successive_approximation_high > 13 || current_scan.successive_approximation_low > 13) {
        dbgln_if(JPEG_DEBUG, "ERROR! Start of Selection: {}, End of Selection: {}, Successive Approximation High: {}, Successive Approximation Low: {}!",
            current_scan.spectral_selection_start,
            current_scan.spectral_selection_end,
            current_scan.successive_approximation_high,
            current_scan.successive_approximation_low);
        return Error::from_string_literal("Spectral selection is not [0,63] or successive approximation is not null");
    }

    context.current_scan = move(current_scan);

    return {};
}

static ErrorOr<void> read_restart_interval(Stream& stream, JPEGLoadingContext& context)
{
    // B.2.4.4 - Restart interval definition syntax
    u16 bytes_to_read = TRY(stream.read_value<BigEndian<u16>>()) - 2;
    if (bytes_to_read != 2) {
        dbgln_if(JPEG_DEBUG, "Malformed DRI marker found!");
        return Error::from_string_literal("Malformed DRI marker found");
    }
    context.dc_restart_interval = TRY(stream.read_value<BigEndian<u16>>());
    return {};
}

static ErrorOr<void> read_huffman_table(Stream& stream, JPEGLoadingContext& context)
{
    i32 bytes_to_read = TRY(stream.read_value<BigEndian<u16>>());
    bytes_to_read -= 2;
    while (bytes_to_read > 0) {
        HuffmanTableSpec table;
        u8 table_info = TRY(stream.read_value<u8>());
        u8 table_type = table_info >> 4;
        u8 table_destination_id = table_info & 0x0F;
        if (table_type > 1) {
            dbgln_if(JPEG_DEBUG, "Unrecognized huffman table: {}!", table_type);
            return Error::from_string_literal("Unrecognized huffman table");
        }
        if (table_destination_id > 1) {
            dbgln_if(JPEG_DEBUG, "Invalid huffman table destination id: {}!", table_destination_id);
            return Error::from_string_literal("Invalid huffman table destination id");
        }

        table.type = table_type;
        table.destination_id = table_destination_id;
        u32 total_codes = 0;

        // Read code counts. At each index K, the value represents the number of K+1 bit codes in this header.
        for (int i = 0; i < 16; i++) {
            u8 count = TRY(stream.read_value<u8>());
            total_codes += count;
            table.code_counts[i] = count;
        }

        table.codes.ensure_capacity(total_codes);

        // Read symbols. Read X bytes, where X is the sum of the counts of codes read in the previous step.
        for (u32 i = 0; i < total_codes; i++) {
            u8 symbol = TRY(stream.read_value<u8>());
            table.symbols.append(symbol);
        }

        auto& huffman_table = table.type == 0 ? context.dc_tables : context.ac_tables;
        huffman_table.set(table.destination_id, table);
        VERIFY(huffman_table.size() <= 2);

        bytes_to_read -= 1 + 16 + total_codes;
    }

    if (bytes_to_read != 0) {
        dbgln_if(JPEG_DEBUG, "Extra bytes detected in huffman header!");
        return Error::from_string_literal("Extra bytes detected in huffman header");
    }
    return {};
}

static ErrorOr<void> read_icc_profile(Stream& stream, JPEGLoadingContext& context, int bytes_to_read)
{
    // https://www.color.org/technotes/ICC-Technote-ProfileEmbedding.pdf, page 5, "JFIF".
    if (bytes_to_read <= 2)
        return Error::from_string_literal("icc marker too small");

    auto chunk_sequence_number = TRY(stream.read_value<u8>()); // 1-based
    auto number_of_chunks = TRY(stream.read_value<u8>());
    bytes_to_read -= 2;

    if (!context.icc_multi_chunk_state.has_value())
        context.icc_multi_chunk_state.emplace(ICCMultiChunkState { 0, TRY(FixedArray<ByteBuffer>::create(number_of_chunks)) });
    auto& chunk_state = context.icc_multi_chunk_state;

    if (chunk_state->seen_number_of_icc_chunks >= number_of_chunks)
        return Error::from_string_literal("Too many ICC chunks");

    if (chunk_state->chunks.size() != number_of_chunks)
        return Error::from_string_literal("Inconsistent number of total ICC chunks");

    if (chunk_sequence_number == 0)
        return Error::from_string_literal("ICC chunk sequence number not 1 based");
    u8 index = chunk_sequence_number - 1;

    if (index >= chunk_state->chunks.size())
        return Error::from_string_literal("ICC chunk sequence number larger than number of chunks");

    if (!chunk_state->chunks[index].is_empty())
        return Error::from_string_literal("Duplicate ICC chunk at sequence number");

    chunk_state->chunks[index] = TRY(ByteBuffer::create_zeroed(bytes_to_read));
    TRY(stream.read_until_filled(chunk_state->chunks[index]));

    chunk_state->seen_number_of_icc_chunks++;

    if (chunk_state->seen_number_of_icc_chunks != chunk_state->chunks.size())
        return {};

    if (number_of_chunks == 1) {
        context.icc_data = move(chunk_state->chunks[0]);
        return {};
    }

    size_t total_size = 0;
    for (auto const& chunk : chunk_state->chunks)
        total_size += chunk.size();

    auto icc_bytes = TRY(ByteBuffer::create_zeroed(total_size));
    size_t start = 0;
    for (auto const& chunk : chunk_state->chunks) {
        memcpy(icc_bytes.data() + start, chunk.data(), chunk.size());
        start += chunk.size();
    }

    context.icc_data = move(icc_bytes);

    return {};
}

static ErrorOr<void> read_colour_encoding(Stream& stream, [[maybe_unused]] JPEGLoadingContext& context, int bytes_to_read)
{
    // The App 14 segment is application specific in the first JPEG standard.
    // However, the Adobe implementation is globally accepted and the value of the color transform
    // was latter standardized as a JPEG-1 extension.

    // For the structure of the App 14 segment, see:
    // https://www.pdfa.org/norm-refs/5116.DCT_Filter.pdf
    // 18 Adobe Application-Specific JPEG Marker

    // For the value of color_transform, see:
    // https://www.itu.int/rec/dologin_pub.asp?lang=e&id=T-REC-T.872-201206-I!!PDF-E&type=items
    // 6.5.3 - APP14 marker segment for colour encoding

    if (bytes_to_read < 6)
        return Error::from_string_literal("App14 segment too small");

    [[maybe_unused]] auto const version = TRY(stream.read_value<u8>());
    [[maybe_unused]] u16 const flag0 = TRY(stream.read_value<BigEndian<u16>>());
    [[maybe_unused]] u16 const flag1 = TRY(stream.read_value<BigEndian<u16>>());
    auto const color_transform = TRY(stream.read_value<u8>());

    if (bytes_to_read > 6) {
        dbgln_if(JPEG_DEBUG, "Unread bytes in App14 segment: {}", bytes_to_read - 1);
        TRY(stream.discard(bytes_to_read - 1));
    }

    switch (color_transform) {
    case 0:
        context.color_transform = ColorTransform::CmykOrRgb;
        break;
    case 1:
        context.color_transform = ColorTransform::YCbCr;
        break;
    case 2:
        context.color_transform = ColorTransform::YCCK;
        break;
    default:
        dbgln("0x{:x} is not a specified transform flag value, ignoring", color_transform);
    }

    return {};
}

static ErrorOr<void> read_app_marker(Stream& stream, JPEGLoadingContext& context, int app_marker_number)
{
    // B.2.4.6 - Application data syntax
    i32 bytes_to_read = TRY(stream.read_value<BigEndian<u16>>());

    if (bytes_to_read <= 2)
        return Error::from_string_literal("app marker size too small");
    bytes_to_read -= 2;

    StringBuilder builder;
    for (;;) {
        if (bytes_to_read == 0) {
            dbgln_if(JPEG_DEBUG, "app marker {} does not start with zero-terminated string", app_marker_number);
            return {};
        }

        auto c = TRY(stream.read_value<char>());
        bytes_to_read--;

        if (c == '\0')
            break;

        TRY(builder.try_append(c));
    }

    auto app_id = TRY(builder.to_string());

    if (app_marker_number == 2 && app_id == "ICC_PROFILE"sv)
        return read_icc_profile(stream, context, bytes_to_read);
    if (app_marker_number == 14 && app_id == "Adobe"sv)
        return read_colour_encoding(stream, context, bytes_to_read);

    return stream.discard(bytes_to_read);
}

static inline bool validate_luma_and_modify_context(Component const& luma, JPEGLoadingContext& context)
{
    if ((luma.hsample_factor == 1 || luma.hsample_factor == 2) && (luma.vsample_factor == 1 || luma.vsample_factor == 2)) {
        context.mblock_meta.hpadded_count += luma.hsample_factor == 1 ? 0 : context.mblock_meta.hcount % 2;
        context.mblock_meta.vpadded_count += luma.vsample_factor == 1 ? 0 : context.mblock_meta.vcount % 2;
        context.mblock_meta.padded_total = context.mblock_meta.hpadded_count * context.mblock_meta.vpadded_count;
        // For easy reference to relevant sample factors.
        context.hsample_factor = luma.hsample_factor;
        context.vsample_factor = luma.vsample_factor;

        if constexpr (JPEG_DEBUG) {
            dbgln("Horizontal Subsampling Factor: {}", luma.hsample_factor);
            dbgln("Vertical Subsampling Factor: {}", luma.vsample_factor);
        }

        return true;
    }
    return false;
}

static inline void set_macroblock_metadata(JPEGLoadingContext& context)
{
    context.mblock_meta.hcount = (context.frame.width + 7) / 8;
    context.mblock_meta.vcount = (context.frame.height + 7) / 8;
    context.mblock_meta.hpadded_count = context.mblock_meta.hcount;
    context.mblock_meta.vpadded_count = context.mblock_meta.vcount;
    context.mblock_meta.total = context.mblock_meta.hcount * context.mblock_meta.vcount;
}

static ErrorOr<void> read_start_of_frame(Stream& stream, JPEGLoadingContext& context)
{
    if (context.state == JPEGLoadingContext::FrameDecoded) {
        dbgln_if(JPEG_DEBUG, "SOF repeated!");
        return Error::from_string_literal("SOF repeated");
    }

    [[maybe_unused]] u16 const bytes_to_read = TRY(stream.read_value<BigEndian<u16>>());

    context.frame.precision = TRY(stream.read_value<u8>());
    if (context.frame.precision != 8) {
        dbgln_if(JPEG_DEBUG, "SOF precision != 8!");
        return Error::from_string_literal("SOF precision != 8");
    }

    context.frame.height = TRY(stream.read_value<BigEndian<u16>>());
    context.frame.width = TRY(stream.read_value<BigEndian<u16>>());
    if (!context.frame.width || !context.frame.height) {
        dbgln_if(JPEG_DEBUG, "ERROR! Image height: {}, Image width: {}!", context.frame.height, context.frame.width);
        return Error::from_string_literal("Image frame height of width null");
    }

    if (context.frame.width > maximum_width_for_decoded_images || context.frame.height > maximum_height_for_decoded_images) {
        dbgln("This JPEG is too large for comfort: {}x{}", context.frame.width, context.frame.height);
        return Error::from_string_literal("JPEG too large for comfort");
    }

    set_macroblock_metadata(context);

    auto component_count = TRY(stream.read_value<u8>());
    if (component_count != 1 && component_count != 3 && component_count != 4) {
        dbgln_if(JPEG_DEBUG, "Unsupported number of components in SOF: {}!", component_count);
        return Error::from_string_literal("Unsupported number of components in SOF");
    }

    for (u8 i = 0; i < component_count; i++) {
        Component component;
        component.id = TRY(stream.read_value<u8>());
        component.index = i;

        u8 subsample_factors = TRY(stream.read_value<u8>());
        component.hsample_factor = subsample_factors >> 4;
        component.vsample_factor = subsample_factors & 0x0F;

        if (i == 0) {
            // By convention, downsampling is applied only on chroma components. So we should
            //  hope to see the maximum sampling factor in the luma component.
            if (!validate_luma_and_modify_context(component, context)) {
                dbgln_if(JPEG_DEBUG, "Unsupported luma subsampling factors: horizontal: {}, vertical: {}",
                    component.hsample_factor,
                    component.vsample_factor);
                return Error::from_string_literal("Unsupported luma subsampling factors");
            }
        } else {
            if (component.hsample_factor != 1 || component.vsample_factor != 1) {
                dbgln_if(JPEG_DEBUG, "Unsupported chroma subsampling factors: horizontal: {}, vertical: {}",
                    component.hsample_factor,
                    component.vsample_factor);
                return Error::from_string_literal("Unsupported chroma subsampling factors");
            }
        }

        component.qtable_id = TRY(stream.read_value<u8>());
        if (component.qtable_id > 1) {
            dbgln_if(JPEG_DEBUG, "Unsupported quantization table id: {}!", component.qtable_id);
            return Error::from_string_literal("Unsupported quantization table id");
        }

        context.components.append(move(component));
    }

    return {};
}

static ErrorOr<void> read_quantization_table(Stream& stream, JPEGLoadingContext& context)
{
    i32 bytes_to_read = TRY(stream.read_value<BigEndian<u16>>()) - 2;
    while (bytes_to_read > 0) {
        u8 info_byte = TRY(stream.read_value<u8>());
        u8 element_unit_hint = info_byte >> 4;
        if (element_unit_hint > 1) {
            dbgln_if(JPEG_DEBUG, "Unsupported unit hint in quantization table: {}!", element_unit_hint);
            return Error::from_string_literal("Unsupported unit hint in quantization table");
        }
        u8 table_id = info_byte & 0x0F;
        if (table_id > 1) {
            dbgln_if(JPEG_DEBUG, "Unsupported quantization table id: {}!", table_id);
            return Error::from_string_literal("Unsupported quantization table id");
        }
        u32* table = table_id == 0 ? context.luma_table : context.chroma_table;
        for (int i = 0; i < 64; i++) {
            if (element_unit_hint == 0) {
                u8 tmp = TRY(stream.read_value<u8>());
                table[zigzag_map[i]] = tmp;
            } else {
                table[zigzag_map[i]] = TRY(stream.read_value<BigEndian<u16>>());
            }
        }

        bytes_to_read -= 1 + (element_unit_hint == 0 ? 64 : 128);
    }
    if (bytes_to_read != 0) {
        dbgln_if(JPEG_DEBUG, "Invalid length for one or more quantization tables!");
        return Error::from_string_literal("Invalid length for one or more quantization tables");
    }

    return {};
}

static ErrorOr<void> skip_segment(Stream& stream)
{
    u16 bytes_to_skip = TRY(stream.read_value<BigEndian<u16>>()) - 2;
    TRY(stream.discard(bytes_to_skip));
    return {};
}

static void dequantize(JPEGLoadingContext& context, Vector<Macroblock>& macroblocks)
{
    for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.vsample_factor) {
        for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.hsample_factor) {
            for (u32 i = 0; i < context.components.size(); i++) {
                auto& component = context.components[i];
                u32 const* table = component.qtable_id == 0 ? context.luma_table : context.chroma_table;
                for (u32 vfactor_i = 0; vfactor_i < component.vsample_factor; vfactor_i++) {
                    for (u32 hfactor_i = 0; hfactor_i < component.hsample_factor; hfactor_i++) {
                        u32 macroblock_index = (vcursor + vfactor_i) * context.mblock_meta.hpadded_count + (hfactor_i + hcursor);
                        Macroblock& block = macroblocks[macroblock_index];
                        auto* block_component = get_component(block, i);
                        for (u32 k = 0; k < 64; k++)
                            block_component[k] *= table[k];
                    }
                }
            }
        }
    }
}

static void inverse_dct(JPEGLoadingContext const& context, Vector<Macroblock>& macroblocks)
{
    static float const m0 = 2.0f * AK::cos(1.0f / 16.0f * 2.0f * AK::Pi<float>);
    static float const m1 = 2.0f * AK::cos(2.0f / 16.0f * 2.0f * AK::Pi<float>);
    static float const m3 = 2.0f * AK::cos(2.0f / 16.0f * 2.0f * AK::Pi<float>);
    static float const m5 = 2.0f * AK::cos(3.0f / 16.0f * 2.0f * AK::Pi<float>);
    static float const m2 = m0 - m5;
    static float const m4 = m0 + m5;
    static float const s0 = AK::cos(0.0f / 16.0f * AK::Pi<float>) * AK::rsqrt(8.0f);
    static float const s1 = AK::cos(1.0f / 16.0f * AK::Pi<float>) / 2.0f;
    static float const s2 = AK::cos(2.0f / 16.0f * AK::Pi<float>) / 2.0f;
    static float const s3 = AK::cos(3.0f / 16.0f * AK::Pi<float>) / 2.0f;
    static float const s4 = AK::cos(4.0f / 16.0f * AK::Pi<float>) / 2.0f;
    static float const s5 = AK::cos(5.0f / 16.0f * AK::Pi<float>) / 2.0f;
    static float const s6 = AK::cos(6.0f / 16.0f * AK::Pi<float>) / 2.0f;
    static float const s7 = AK::cos(7.0f / 16.0f * AK::Pi<float>) / 2.0f;

    for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.vsample_factor) {
        for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.hsample_factor) {
            for (u32 component_i = 0; component_i < context.components.size(); component_i++) {
                auto& component = context.components[component_i];
                for (u8 vfactor_i = 0; vfactor_i < component.vsample_factor; vfactor_i++) {
                    for (u8 hfactor_i = 0; hfactor_i < component.hsample_factor; hfactor_i++) {
                        u32 macroblock_index = (vcursor + vfactor_i) * context.mblock_meta.hpadded_count + (hfactor_i + hcursor);
                        Macroblock& block = macroblocks[macroblock_index];
                        auto* block_component = get_component(block, component_i);
                        for (u32 k = 0; k < 8; ++k) {
                            float const g0 = block_component[0 * 8 + k] * s0;
                            float const g1 = block_component[4 * 8 + k] * s4;
                            float const g2 = block_component[2 * 8 + k] * s2;
                            float const g3 = block_component[6 * 8 + k] * s6;
                            float const g4 = block_component[5 * 8 + k] * s5;
                            float const g5 = block_component[1 * 8 + k] * s1;
                            float const g6 = block_component[7 * 8 + k] * s7;
                            float const g7 = block_component[3 * 8 + k] * s3;

                            float const f0 = g0;
                            float const f1 = g1;
                            float const f2 = g2;
                            float const f3 = g3;
                            float const f4 = g4 - g7;
                            float const f5 = g5 + g6;
                            float const f6 = g5 - g6;
                            float const f7 = g4 + g7;

                            float const e0 = f0;
                            float const e1 = f1;
                            float const e2 = f2 - f3;
                            float const e3 = f2 + f3;
                            float const e4 = f4;
                            float const e5 = f5 - f7;
                            float const e6 = f6;
                            float const e7 = f5 + f7;
                            float const e8 = f4 + f6;

                            float const d0 = e0;
                            float const d1 = e1;
                            float const d2 = e2 * m1;
                            float const d3 = e3;
                            float const d4 = e4 * m2;
                            float const d5 = e5 * m3;
                            float const d6 = e6 * m4;
                            float const d7 = e7;
                            float const d8 = e8 * m5;

                            float const c0 = d0 + d1;
                            float const c1 = d0 - d1;
                            float const c2 = d2 - d3;
                            float const c3 = d3;
                            float const c4 = d4 + d8;
                            float const c5 = d5 + d7;
                            float const c6 = d6 - d8;
                            float const c7 = d7;
                            float const c8 = c5 - c6;

                            float const b0 = c0 + c3;
                            float const b1 = c1 + c2;
                            float const b2 = c1 - c2;
                            float const b3 = c0 - c3;
                            float const b4 = c4 - c8;
                            float const b5 = c8;
                            float const b6 = c6 - c7;
                            float const b7 = c7;

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
                            float const g0 = block_component[l * 8 + 0] * s0;
                            float const g1 = block_component[l * 8 + 4] * s4;
                            float const g2 = block_component[l * 8 + 2] * s2;
                            float const g3 = block_component[l * 8 + 6] * s6;
                            float const g4 = block_component[l * 8 + 5] * s5;
                            float const g5 = block_component[l * 8 + 1] * s1;
                            float const g6 = block_component[l * 8 + 7] * s7;
                            float const g7 = block_component[l * 8 + 3] * s3;

                            float const f0 = g0;
                            float const f1 = g1;
                            float const f2 = g2;
                            float const f3 = g3;
                            float const f4 = g4 - g7;
                            float const f5 = g5 + g6;
                            float const f6 = g5 - g6;
                            float const f7 = g4 + g7;

                            float const e0 = f0;
                            float const e1 = f1;
                            float const e2 = f2 - f3;
                            float const e3 = f2 + f3;
                            float const e4 = f4;
                            float const e5 = f5 - f7;
                            float const e6 = f6;
                            float const e7 = f5 + f7;
                            float const e8 = f4 + f6;

                            float const d0 = e0;
                            float const d1 = e1;
                            float const d2 = e2 * m1;
                            float const d3 = e3;
                            float const d4 = e4 * m2;
                            float const d5 = e5 * m3;
                            float const d6 = e6 * m4;
                            float const d7 = e7;
                            float const d8 = e8 * m5;

                            float const c0 = d0 + d1;
                            float const c1 = d0 - d1;
                            float const c2 = d2 - d3;
                            float const c3 = d3;
                            float const c4 = d4 + d8;
                            float const c5 = d5 + d7;
                            float const c6 = d6 - d8;
                            float const c7 = d7;
                            float const c8 = c5 - c6;

                            float const b0 = c0 + c3;
                            float const b1 = c1 + c2;
                            float const b2 = c1 - c2;
                            float const b3 = c0 - c3;
                            float const b4 = c4 - c8;
                            float const b5 = c8;
                            float const b6 = c6 - c7;
                            float const b7 = c7;

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

    // F.2.1.5 - Inverse DCT (IDCT)

    for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.vsample_factor) {
        for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.hsample_factor) {
            for (u8 vfactor_i = 0; vfactor_i < context.vsample_factor; ++vfactor_i) {
                for (u8 hfactor_i = 0; hfactor_i < context.hsample_factor; ++hfactor_i) {
                    u32 mb_index = (vcursor + vfactor_i) * context.mblock_meta.hpadded_count + (hcursor + hfactor_i);
                    for (u8 i = 0; i < 8; ++i) {
                        for (u8 j = 0; j < 8; ++j) {
                            macroblocks[mb_index].r[i * 8 + j] = clamp(macroblocks[mb_index].r[i * 8 + j] + 128, 0, 255);
                            macroblocks[mb_index].g[i * 8 + j] = clamp(macroblocks[mb_index].g[i * 8 + j] + 128, 0, 255);
                            macroblocks[mb_index].b[i * 8 + j] = clamp(macroblocks[mb_index].b[i * 8 + j] + 128, 0, 255);
                            macroblocks[mb_index].k[i * 8 + j] = clamp(macroblocks[mb_index].b[i * 8 + j] + 128, 0, 255);
                        }
                    }
                }
            }
        }
    }
}

static void ycbcr_to_rgb(JPEGLoadingContext const& context, Vector<Macroblock>& macroblocks)
{
    // Conversion from YCbCr to RGB isn't specified in the first JPEG specification but in the JFIF extension:
    // See: https://www.itu.int/rec/dologin_pub.asp?lang=f&id=T-REC-T.871-201105-I!!PDF-E&type=items
    // 7 - Conversion to and from RGB
    for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.vsample_factor) {
        for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.hsample_factor) {
            const u32 chroma_block_index = vcursor * context.mblock_meta.hpadded_count + hcursor;
            Macroblock const& chroma = macroblocks[chroma_block_index];
            // Overflows are intentional.
            for (u8 vfactor_i = context.vsample_factor - 1; vfactor_i < context.vsample_factor; --vfactor_i) {
                for (u8 hfactor_i = context.hsample_factor - 1; hfactor_i < context.hsample_factor; --hfactor_i) {
                    u32 macroblock_index = (vcursor + vfactor_i) * context.mblock_meta.hpadded_count + (hcursor + hfactor_i);
                    auto* y = macroblocks[macroblock_index].y;
                    auto* cb = macroblocks[macroblock_index].cb;
                    auto* cr = macroblocks[macroblock_index].cr;
                    for (u8 i = 7; i < 8; --i) {
                        for (u8 j = 7; j < 8; --j) {
                            const u8 pixel = i * 8 + j;
                            const u32 chroma_pxrow = (i / context.vsample_factor) + 4 * vfactor_i;
                            const u32 chroma_pxcol = (j / context.hsample_factor) + 4 * hfactor_i;
                            const u32 chroma_pixel = chroma_pxrow * 8 + chroma_pxcol;
                            int r = y[pixel] + 1.402f * (chroma.cr[chroma_pixel] - 128);
                            int g = y[pixel] - 0.3441f * (chroma.cb[chroma_pixel] - 128) - 0.7141f * (chroma.cr[chroma_pixel] - 128);
                            int b = y[pixel] + 1.772f * (chroma.cb[chroma_pixel] - 128);
                            y[pixel] = clamp(r, 0, 255);
                            cb[pixel] = clamp(g, 0, 255);
                            cr[pixel] = clamp(b, 0, 255);
                        }
                    }
                }
            }
        }
    }
}

static void invert_colors_for_adobe_images(JPEGLoadingContext const& context, Vector<Macroblock>& macroblocks)
{
    if (!context.color_transform.has_value())
        return;

    // From libjpeg-turbo's libjpeg.txt:
    // https://github.com/libjpeg-turbo/libjpeg-turbo/blob/main/libjpeg.txt
    // CAUTION: it appears that Adobe Photoshop writes inverted data in CMYK JPEG
    // files: 0 represents 100% ink coverage, rather than 0% ink as you'd expect.
    // This is arguably a bug in Photoshop, but if you need to work with Photoshop
    // CMYK files, you will have to deal with it in your application.
    for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.vsample_factor) {
        for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.hsample_factor) {
            for (u8 vfactor_i = 0; vfactor_i < context.vsample_factor; ++vfactor_i) {
                for (u8 hfactor_i = 0; hfactor_i < context.hsample_factor; ++hfactor_i) {
                    u32 mb_index = (vcursor + vfactor_i) * context.mblock_meta.hpadded_count + (hcursor + hfactor_i);
                    for (u8 i = 0; i < 8; ++i) {
                        for (u8 j = 0; j < 8; ++j) {
                            macroblocks[mb_index].r[i * 8 + j] = NumericLimits<u8>::max() - macroblocks[mb_index].r[i * 8 + j];
                            macroblocks[mb_index].g[i * 8 + j] = NumericLimits<u8>::max() - macroblocks[mb_index].g[i * 8 + j];
                            macroblocks[mb_index].b[i * 8 + j] = NumericLimits<u8>::max() - macroblocks[mb_index].b[i * 8 + j];
                            macroblocks[mb_index].k[i * 8 + j] = NumericLimits<u8>::max() - macroblocks[mb_index].k[i * 8 + j];
                        }
                    }
                }
            }
        }
    }
}

static void cmyk_to_rgb(JPEGLoadingContext const& context, Vector<Macroblock>& macroblocks)
{
    invert_colors_for_adobe_images(context, macroblocks);

    for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.vsample_factor) {
        for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.hsample_factor) {
            for (u8 vfactor_i = context.vsample_factor - 1; vfactor_i < context.vsample_factor; --vfactor_i) {
                for (u8 hfactor_i = context.hsample_factor - 1; hfactor_i < context.hsample_factor; --hfactor_i) {
                    u32 mb_index = (vcursor + vfactor_i) * context.mblock_meta.hpadded_count + (hcursor + hfactor_i);
                    auto* c = macroblocks[mb_index].y;
                    auto* m = macroblocks[mb_index].cb;
                    auto* y = macroblocks[mb_index].cr;
                    auto* k = macroblocks[mb_index].k;
                    for (u8 i = 0; i < 8; ++i) {
                        for (u8 j = 0; j < 8; ++j) {
                            u8 const pixel = i * 8 + j;

                            static constexpr auto max_value = NumericLimits<u8>::max();

                            auto const black_component = max_value - k[pixel];
                            int const r = ((max_value - c[pixel]) * black_component) / max_value;
                            int const g = ((max_value - m[pixel]) * black_component) / max_value;
                            int const b = ((max_value - y[pixel]) * black_component) / max_value;

                            c[pixel] = clamp(r, 0, max_value);
                            m[pixel] = clamp(g, 0, max_value);
                            y[pixel] = clamp(b, 0, max_value);
                        }
                    }
                }
            }
        }
    }
}

static void ycck_to_rgb(JPEGLoadingContext const& context, Vector<Macroblock>& macroblocks)
{
    // 7 - Conversions between colour encodings
    // YCCK is obtained from CMYK by converting the CMY channels to YCC channel.

    // To convert back into RGB, we only need the 3 first components, which are baseline YCbCr
    ycbcr_to_rgb(context, macroblocks);

    // RGB to CMYK, as mentioned in https://www.smcm.iqfr.csic.es/docs/intel/ipp/ipp_manual/IPPI/ippi_ch15/functn_YCCKToCMYK_JPEG.htm#functn_YCCKToCMYK_JPEG
    for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.vsample_factor) {
        for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.hsample_factor) {
            for (u8 vfactor_i = 0; vfactor_i < context.vsample_factor; ++vfactor_i) {
                for (u8 hfactor_i = 0; hfactor_i < context.hsample_factor; ++hfactor_i) {
                    u32 mb_index = (vcursor + vfactor_i) * context.mblock_meta.hpadded_count + (hcursor + hfactor_i);
                    for (u8 i = 0; i < 8; ++i) {
                        for (u8 j = 0; j < 8; ++j) {
                            macroblocks[mb_index].r[i * 8 + j] = NumericLimits<u8>::max() - macroblocks[mb_index].r[i * 8 + j];
                            macroblocks[mb_index].g[i * 8 + j] = NumericLimits<u8>::max() - macroblocks[mb_index].g[i * 8 + j];
                            macroblocks[mb_index].b[i * 8 + j] = NumericLimits<u8>::max() - macroblocks[mb_index].b[i * 8 + j];
                        }
                    }
                }
            }
        }
    }

    cmyk_to_rgb(context, macroblocks);
}

static ErrorOr<void> handle_color_transform(JPEGLoadingContext const& context, Vector<Macroblock>& macroblocks)
{
    if (context.color_transform.has_value()) {
        // https://www.itu.int/rec/dologin_pub.asp?lang=e&id=T-REC-T.872-201206-I!!PDF-E&type=items
        // 6.5.3 - APP14 marker segment for colour encoding

        switch (*context.color_transform) {
        case ColorTransform::CmykOrRgb:
            if (context.components.size() == 4) {
                cmyk_to_rgb(context, macroblocks);
            } else if (context.components.size() == 3) {
                // Note: components.size() == 3 means that we have an RGB image, so no color transformation is needed.
            } else {
                return Error::from_string_literal("Wrong number of components for CMYK or RGB, aborting.");
            }
            break;
        case ColorTransform::YCbCr:
            ycbcr_to_rgb(context, macroblocks);
            break;
        case ColorTransform::YCCK:
            ycck_to_rgb(context, macroblocks);
            break;
        }

        return {};
    }

    // No App14 segment is present, assuming :
    //      - 1 components means grayscale
    //      - 3 components means YCbCr
    //      - 4 components means CMYK
    if (context.components.size() == 4)
        cmyk_to_rgb(context, macroblocks);
    if (context.components.size() == 3)
        ycbcr_to_rgb(context, macroblocks);

    if (context.components.size() == 1) {
        // With Cb and Cr being equal to zero, this function assign the Y
        // value (luminosity) to R, G and B. Providing a proper conversion
        // from grayscale to RGB.
        ycbcr_to_rgb(context, macroblocks);
    }

    return {};
}

static ErrorOr<void> compose_bitmap(JPEGLoadingContext& context, Vector<Macroblock> const& macroblocks)
{
    context.bitmap = TRY(Bitmap::create(BitmapFormat::BGRx8888, { context.frame.width, context.frame.height }));

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

    return {};
}

static bool is_app_marker(Marker const marker)
{
    return marker >= JPEG_APPN0 && marker <= JPEG_APPN15;
}

static bool is_miscellaneous_or_table_marker(Marker const marker)
{
    // B.2.4 - Table-specification and miscellaneous marker segment syntax
    // See also B.6 - Summary: Figure B.17 – Flow of marker segment

    bool const is_misc = marker == JPEG_COM || marker == JPEG_DRI || is_app_marker(marker);
    bool const is_table = marker == JPEG_DQT || marker == JPEG_DAC || marker == JPEG_DHT;

    return is_misc || is_table;
}

static ErrorOr<void> handle_miscellaneous_or_table(Stream& stream, JPEGLoadingContext& context, Marker const marker)
{
    if (is_app_marker(marker)) {
        TRY(read_app_marker(stream, context, marker - JPEG_APPN0));
        return {};
    }

    switch (marker) {
    case JPEG_COM:
    case JPEG_DAC:
        dbgln_if(JPEG_DEBUG, "TODO: implement marker \"{:x}\"", marker);
        if (auto result = skip_segment(stream); result.is_error()) {
            dbgln_if(JPEG_DEBUG, "Error skipping marker: {:x}!", marker);
            return result.release_error();
        }
        break;
    case JPEG_DHT:
        TRY(read_huffman_table(stream, context));
        break;
    case JPEG_DQT:
        TRY(read_quantization_table(stream, context));
        break;
    case JPEG_DRI:
        TRY(read_restart_interval(stream, context));
        break;
    default:
        dbgln("Unexpected marker: {:x}", marker);
        VERIFY_NOT_REACHED();
    }

    return {};
}

static ErrorOr<void> parse_header(Stream& stream, JPEGLoadingContext& context)
{
    auto marker = TRY(read_marker_at_cursor(stream));
    if (marker != JPEG_SOI) {
        dbgln_if(JPEG_DEBUG, "SOI not found: {:x}!", marker);
        return Error::from_string_literal("SOI not found");
    }
    for (;;) {
        marker = TRY(read_marker_at_cursor(stream));

        if (is_miscellaneous_or_table_marker(marker)) {
            TRY(handle_miscellaneous_or_table(stream, context, marker));
            continue;
        }

        // Set frame type if the marker marks a new frame.
        if (is_frame_marker(marker))
            context.frame.type = static_cast<StartOfFrame::FrameType>(marker & 0xF);

        switch (marker) {
        case JPEG_INVALID:
        case JPEG_RST0:
        case JPEG_RST1:
        case JPEG_RST2:
        case JPEG_RST3:
        case JPEG_RST4:
        case JPEG_RST5:
        case JPEG_RST6:
        case JPEG_RST7:
        case JPEG_SOI:
        case JPEG_EOI:
            dbgln_if(JPEG_DEBUG, "Unexpected marker {:x}!", marker);
            return Error::from_string_literal("Unexpected marker");
        case JPEG_SOF0:
        case JPEG_SOF2:
            TRY(read_start_of_frame(stream, context));
            context.state = JPEGLoadingContext::FrameDecoded;
            return {};
        default:
            if (auto result = skip_segment(stream); result.is_error()) {
                dbgln_if(JPEG_DEBUG, "Error skipping marker: {:x}!", marker);
                return result.release_error();
            }
            break;
        }
    }

    VERIFY_NOT_REACHED();
}

static ErrorOr<void> scan_huffman_stream(AK::SeekableStream& stream, HuffmanStreamState& huffman_stream)
{
    u8 last_byte;
    u8 current_byte = TRY(stream.read_value<u8>());

    for (;;) {
        last_byte = current_byte;
        current_byte = TRY(stream.read_value<u8>());

        if (last_byte == 0xFF) {
            if (current_byte == 0xFF)
                continue;
            if (current_byte == 0x00) {
                current_byte = TRY(stream.read_value<u8>());
                huffman_stream.stream.append(last_byte);
                continue;
            }
            Marker marker = 0xFF00 | current_byte;
            if (marker >= JPEG_RST0 && marker <= JPEG_RST7) {
                huffman_stream.stream.append(marker);
                current_byte = TRY(stream.read_value<u8>());
                continue;
            }

            // Rollback the marker we just read
            TRY(stream.seek(-2, AK::SeekMode::FromCurrentPosition));
            return {};
        } else {
            huffman_stream.stream.append(last_byte);
        }
    }

    VERIFY_NOT_REACHED();
}

static ErrorOr<void> decode_header(JPEGLoadingContext& context)
{
    if (context.state < JPEGLoadingContext::State::HeaderDecoded) {
        if (auto result = parse_header(*context.stream, context); result.is_error()) {
            context.state = JPEGLoadingContext::State::Error;
            return result.release_error();
        }

        if constexpr (JPEG_DEBUG) {
            dbgln("Image width: {}", context.frame.width);
            dbgln("Image height: {}", context.frame.height);
            dbgln("Macroblocks in a row: {}", context.mblock_meta.hpadded_count);
            dbgln("Macroblocks in a column: {}", context.mblock_meta.vpadded_count);
            dbgln("Macroblock meta padded total: {}", context.mblock_meta.padded_total);
        }

        context.state = JPEGLoadingContext::State::HeaderDecoded;
    }
    return {};
}

static ErrorOr<Vector<Macroblock>> construct_macroblocks(JPEGLoadingContext& context)
{
    // B.6 - Summary
    // See: Figure B.16 – Flow of compressed data syntax
    // This function handles the "Multi-scan" loop.

    Vector<Macroblock> macroblocks;
    TRY(macroblocks.try_resize(context.mblock_meta.padded_total));

    Marker marker = TRY(read_marker_at_cursor(*context.stream));
    while (true) {
        if (is_miscellaneous_or_table_marker(marker)) {
            TRY(handle_miscellaneous_or_table(*context.stream, context, marker));
        } else if (marker == JPEG_SOS) {
            TRY(read_start_of_scan(*context.stream, context));
            TRY(scan_huffman_stream(*context.stream, context.current_scan.huffman_stream));
            TRY(decode_huffman_stream(context, macroblocks));
        } else if (marker == JPEG_EOI) {
            return macroblocks;
        } else {
            dbgln_if(JPEG_DEBUG, "Unexpected marker {:x}!", marker);
            return Error::from_string_literal("Unexpected marker");
        }

        marker = TRY(read_marker_at_cursor(*context.stream));
    }
}

static ErrorOr<void> decode_jpeg(JPEGLoadingContext& context)
{
    TRY(decode_header(context));
    auto macroblocks = TRY(construct_macroblocks(context));
    dequantize(context, macroblocks);
    inverse_dct(context, macroblocks);
    TRY(handle_color_transform(context, macroblocks));
    TRY(compose_bitmap(context, macroblocks));
    context.stream.clear();
    return {};
}

JPEGImageDecoderPlugin::JPEGImageDecoderPlugin(NonnullOwnPtr<FixedMemoryStream> stream)
{
    m_context = make<JPEGLoadingContext>();
    m_context->stream = move(stream);
}

JPEGImageDecoderPlugin::~JPEGImageDecoderPlugin() = default;

IntSize JPEGImageDecoderPlugin::size()
{
    if (m_context->state == JPEGLoadingContext::State::Error)
        return {};
    if (m_context->state >= JPEGLoadingContext::State::FrameDecoded)
        return { m_context->frame.width, m_context->frame.height };

    return {};
}

void JPEGImageDecoderPlugin::set_volatile()
{
    if (m_context->bitmap)
        m_context->bitmap->set_volatile();
}

bool JPEGImageDecoderPlugin::set_nonvolatile(bool& was_purged)
{
    if (!m_context->bitmap)
        return false;
    return m_context->bitmap->set_nonvolatile(was_purged);
}

bool JPEGImageDecoderPlugin::initialize()
{
    return true;
}

bool JPEGImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    return data.size() > 3
        && data.data()[0] == 0xFF
        && data.data()[1] == 0xD8
        && data.data()[2] == 0xFF;
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> JPEGImageDecoderPlugin::create(ReadonlyBytes data)
{
    auto stream = TRY(try_make<FixedMemoryStream>(data));
    return adopt_nonnull_own_or_enomem(new (nothrow) JPEGImageDecoderPlugin(move(stream)));
}

bool JPEGImageDecoderPlugin::is_animated()
{
    return false;
}

size_t JPEGImageDecoderPlugin::loop_count()
{
    return 0;
}

size_t JPEGImageDecoderPlugin::frame_count()
{
    return 1;
}

ErrorOr<ImageFrameDescriptor> JPEGImageDecoderPlugin::frame(size_t index)
{
    if (index > 0)
        return Error::from_string_literal("JPEGImageDecoderPlugin: Invalid frame index");

    if (m_context->state == JPEGLoadingContext::State::Error)
        return Error::from_string_literal("JPEGImageDecoderPlugin: Decoding failed");

    if (m_context->state < JPEGLoadingContext::State::BitmapDecoded) {
        if (auto result = decode_jpeg(*m_context); result.is_error()) {
            m_context->state = JPEGLoadingContext::State::Error;
            return result.release_error();
        }
        m_context->state = JPEGLoadingContext::State::BitmapDecoded;
    }

    return ImageFrameDescriptor { m_context->bitmap, 0 };
}

ErrorOr<Optional<ReadonlyBytes>> JPEGImageDecoderPlugin::icc_data()
{
    TRY(decode_header(*m_context));

    if (m_context->icc_data.has_value())
        return *m_context->icc_data;
    return OptionalNone {};
}

}
