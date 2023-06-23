/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022-2023, Lucas Chollet <lucas.chollet@serenityos.org>
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
#include <AK/NumericLimits.h>
#include <AK/String.h>
#include <AK/Try.h>
#include <AK/Vector.h>
#include <LibGfx/ImageFormats/JPEGLoader.h>
#include <LibGfx/ImageFormats/JPEGShared.h>
#include <LibGfx/ImageFormats/TIFFLoader.h>
#include <LibGfx/ImageFormats/TIFFMetadata.h>

namespace Gfx {

struct MacroblockMeta {
    u32 total { 0 };
    u32 padded_total { 0 };
    u32 hcount { 0 };
    u32 vcount { 0 };
    u32 hpadded_count { 0 };
    u32 vpadded_count { 0 };
};

struct SamplingFactors {
    u8 horizontal {};
    u8 vertical {};

    bool operator==(SamplingFactors const&) const = default;
};

// In the JPEG format, components are defined first at the frame level, then
// referenced in each scan and aggregated with scan-specific information. The
// two following structs mimic this hierarchy.

struct Component {
    // B.2.2 - Frame header syntax
    u8 id { 0 };                               // Ci, Component identifier
    SamplingFactors sampling_factors { 1, 1 }; // Hi, Horizontal sampling factor and Vi, Vertical sampling factor
    u8 quantization_table_id { 0 };            // Tqi, Quantization table destination selector

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

struct HuffmanTable {
    u8 type { 0 };
    u8 destination_id { 0 };
    u8 code_counts[16] = { 0 };
    Vector<u8> symbols;
    Vector<u16> codes;

    // Note: The value 8 is chosen quite arbitrarily, the only current constraint
    //       is that both the symbol and the size fit in an u16. I've tested more
    //       values but none stand out, and 8 is the value used by libjpeg-turbo.
    static constexpr u8 bits_per_cached_code = 8;
    static constexpr u8 maximum_bits_per_code = 16;
    u8 first_non_cached_code_index {};

    ErrorOr<void> generate_codes()
    {
        unsigned code = 0;
        for (auto number_of_codes : code_counts) {
            for (int i = 0; i < number_of_codes; i++)
                codes.append(code++);
            code <<= 1;
        }

        TRY(generate_lookup_table());
        return {};
    }

    struct SymbolAndSize {
        u8 symbol {};
        u8 size {};
    };

    ErrorOr<SymbolAndSize> symbol_from_code(u16 code) const
    {
        static constexpr u8 shift_for_cache = maximum_bits_per_code - bits_per_cached_code;

        if (lookup_table[code >> shift_for_cache] != invalid_entry) {
            u8 const code_length = lookup_table[code >> shift_for_cache] >> bits_per_cached_code;
            return SymbolAndSize { static_cast<u8>(lookup_table[code >> shift_for_cache]), code_length };
        }

        u64 code_cursor = first_non_cached_code_index;

        for (u8 i = HuffmanTable::bits_per_cached_code; i < 16; i++) {
            auto const result = code >> (maximum_bits_per_code - 1 - i);
            for (u32 j = 0; j < code_counts[i]; j++) {
                if (result == codes[code_cursor])
                    return SymbolAndSize { symbols[code_cursor], static_cast<u8>(i + 1) };

                code_cursor++;
            }
        }

        return Error::from_string_literal("This kind of JPEG is not yet supported by the decoder");
    }

private:
    static constexpr u16 invalid_entry = 0xFF;

    ErrorOr<void> generate_lookup_table()
    {
        lookup_table.fill(invalid_entry);

        u32 code_offset = 0;
        for (u8 code_length = 1; code_length <= bits_per_cached_code; code_length++) {
            for (u32 i = 0; i < code_counts[code_length - 1]; i++, code_offset++) {
                u32 code_key = codes[code_offset] << (bits_per_cached_code - code_length);
                u8 duplicate_count = 1 << (bits_per_cached_code - code_length);
                if (code_key + duplicate_count >= lookup_table.size())
                    return Error::from_string_literal("Malformed Huffman table");

                for (; duplicate_count > 0; duplicate_count--) {
                    lookup_table[code_key] = (code_length << bits_per_cached_code) | symbols[code_offset];
                    code_key++;
                }
            }
        }
        return {};
    }

    Array<u16, 1 << bits_per_cached_code> lookup_table {};
};

class HuffmanStream;

class JPEGStream {
public:
    static ErrorOr<JPEGStream> create(NonnullOwnPtr<Stream> stream)
    {
        Vector<u8> buffer;
        TRY(buffer.try_resize(buffer_size));
        JPEGStream jpeg_stream { move(stream), move(buffer) };

        TRY(jpeg_stream.refill_buffer());
        jpeg_stream.m_offset_from_start = 0;
        return jpeg_stream;
    }

    ALWAYS_INLINE ErrorOr<u8> read_u8()
    {
        if (m_byte_offset == m_current_size)
            TRY(refill_buffer());
        return m_buffer[m_byte_offset++];
    }

    ALWAYS_INLINE ErrorOr<u16> read_u16()
    {
        if (m_saved_marker.has_value())
            return m_saved_marker.release_value();

        return (static_cast<u16>(TRY(read_u8())) << 8) | TRY(read_u8());
    }

    ALWAYS_INLINE ErrorOr<void> discard(u64 bytes)
    {
        auto const discarded_from_buffer = min(m_current_size - m_byte_offset, bytes);
        m_byte_offset += discarded_from_buffer;

        if (discarded_from_buffer < bytes) {
            m_offset_from_start += bytes - discarded_from_buffer;
            TRY(m_stream->discard(bytes - discarded_from_buffer));
        }

        return {};
    }

    ErrorOr<void> read_until_filled(Bytes bytes)
    {
        auto const copied = m_buffer.span().slice(m_byte_offset).copy_trimmed_to(bytes);
        m_byte_offset += copied;

        if (copied < bytes.size()) {
            m_offset_from_start += bytes.size() - copied;
            TRY(m_stream->read_until_filled(bytes.slice(copied)));
        }

        return {};
    }

    Optional<u16>& saved_marker(Badge<HuffmanStream>)
    {
        return m_saved_marker;
    }

    u64 byte_offset() const
    {
        return m_offset_from_start + m_byte_offset;
    }

private:
    JPEGStream(NonnullOwnPtr<Stream> stream, Vector<u8> buffer)
        : m_stream(move(stream))
        , m_buffer(move(buffer))
    {
    }

    ErrorOr<void> refill_buffer()
    {
        VERIFY(m_byte_offset == m_current_size);

        m_offset_from_start += m_byte_offset;

        m_current_size = TRY(m_stream->read_some(m_buffer.span())).size();
        if (m_current_size == 0)
            return Error::from_string_literal("Unexpected end of file");

        m_byte_offset = 0;

        return {};
    }

    static constexpr auto buffer_size = 4096;

    NonnullOwnPtr<Stream> m_stream;

    Optional<u16> m_saved_marker {};

    Vector<u8> m_buffer {};
    u64 m_offset_from_start { 0 };
    u64 m_byte_offset { buffer_size };
    u64 m_current_size { buffer_size };
};

class HuffmanStream {
public:
    ALWAYS_INLINE ErrorOr<u8> next_symbol(HuffmanTable const& table)
    {
        u16 const code = TRY(peek_bits(HuffmanTable::maximum_bits_per_code));

        auto const symbol_and_size = TRY(table.symbol_from_code(code));

        TRY(discard_bits(symbol_and_size.size));
        return symbol_and_size.symbol;
    }

    ALWAYS_INLINE ErrorOr<u16> read_bits(u8 count = 1)
    {
        if (count > NumericLimits<u16>::digits()) {
            dbgln_if(JPEG_DEBUG, "Can't read {} bits at once!", count);
            return Error::from_string_literal("Reading too much huffman bits at once");
        }

        u16 const value = TRY(peek_bits(count));
        TRY(discard_bits(count));
        return value;
    }

    ALWAYS_INLINE ErrorOr<u16> peek_bits(u8 count)
    {
        if (count == 0)
            return 0;

        if (count + m_bit_offset > bits_in_reservoir)
            TRY(refill_reservoir());

        auto const mask = NumericLimits<u16>::max() >> (NumericLimits<u16>::digits() - count);

        return static_cast<u16>((m_bit_reservoir >> (bits_in_reservoir - m_bit_offset - count)) & mask);
    }

    ALWAYS_INLINE ErrorOr<void> discard_bits(u8 count)
    {
        m_bit_offset += count;

        if (m_bit_offset > bits_in_reservoir)
            TRY(refill_reservoir());

        return {};
    }

    ErrorOr<void> advance_to_byte_boundary()
    {
        if (auto remainder = m_bit_offset % 8; remainder != 0)
            TRY(discard_bits(bits_per_byte - remainder));

        return {};
    }

    HuffmanStream(JPEGStream& stream)
        : jpeg_stream(stream)
    {
    }

private:
    ALWAYS_INLINE ErrorOr<void> refill_reservoir()
    {
        auto const bytes_needed = m_bit_offset / bits_per_byte;

        u8 bytes_added {};

        auto const append_byte = [&](u8 byte) {
            m_last_byte_was_ff = false;
            m_bit_reservoir <<= 8;
            m_bit_reservoir |= byte;
            m_bit_offset -= 8;
            bytes_added++;
        };

        do {
            // Note: We fake zeroes when we have reached another segment
            //       It allows us to continue peeking seamlessly.
            u8 const next_byte = jpeg_stream.saved_marker({}).has_value() ? 0 : TRY(jpeg_stream.read_u8());

            if (m_last_byte_was_ff) {
                if (next_byte == 0xFF)
                    continue;

                if (next_byte == 0x00) {
                    append_byte(0xFF);
                    continue;
                }

                Marker const marker = 0xFF00 | next_byte;
                if (marker < JPEG_RST0 || marker > JPEG_RST7) {
                    // Note: The only way to know that we reached the end of a segment is to read
                    //       the marker of the following one. So we store it for later use.
                    jpeg_stream.saved_marker({}) = marker;
                    m_last_byte_was_ff = false;
                    continue;
                }
            }

            if (next_byte == 0xFF) {
                m_last_byte_was_ff = true;
                continue;
            }

            append_byte(next_byte);
        } while (bytes_added < bytes_needed);

        return {};
    }

    JPEGStream& jpeg_stream;

    using Reservoir = u64;
    static constexpr auto bits_per_byte = 8;
    static constexpr auto bits_in_reservoir = sizeof(Reservoir) * bits_per_byte;

    Reservoir m_bit_reservoir {};
    u8 m_bit_offset { bits_in_reservoir };

    bool m_last_byte_was_ff { false };
};

struct ICCMultiChunkState {
    u8 seen_number_of_icc_chunks { 0 };
    FixedArray<ByteBuffer> chunks;
};

struct Scan {
    Scan(HuffmanStream stream)
        : huffman_stream(stream)
    {
    }

    // B.2.3 - Scan header syntax
    Vector<ScanComponent, 4> components;

    u8 spectral_selection_start {};      // Ss
    u8 spectral_selection_end {};        // Se
    u8 successive_approximation_high {}; // Ah
    u8 successive_approximation_low {};  // Al

    HuffmanStream huffman_stream;

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
    JPEGLoadingContext(JPEGStream jpeg_stream, JPEGDecoderOptions options)
        : stream(move(jpeg_stream))
        , options(options)
    {
    }

    static ErrorOr<NonnullOwnPtr<JPEGLoadingContext>> create(NonnullOwnPtr<Stream> stream, JPEGDecoderOptions options)
    {
        auto jpeg_stream = TRY(JPEGStream::create(move(stream)));
        return make<JPEGLoadingContext>(move(jpeg_stream), options);
    }

    enum State {
        NotDecoded = 0,
        Error,
        FrameDecoded,
        HeaderDecoded,
        BitmapDecoded
    };

    State state { State::NotDecoded };

    Array<Array<u16, 64>, 4> quantization_tables {};
    Array<bool, 4> registered_quantization_tables {};

    StartOfFrame frame;
    SamplingFactors sampling_factors {};

    Optional<Scan> current_scan {};

    Vector<Component, 4> components;

    RefPtr<Gfx::Bitmap> bitmap;
    RefPtr<Gfx::CMYKBitmap> cmyk_bitmap;

    u16 dc_restart_interval { 0 };
    HashMap<u8, HuffmanTable> dc_tables;
    HashMap<u8, HuffmanTable> ac_tables;
    Array<i16, 4> previous_dc_values {};
    MacroblockMeta mblock_meta;
    JPEGStream stream;
    JPEGDecoderOptions options;

    Optional<ColorTransform> color_transform {};

    OwnPtr<ExifMetadata> exif_metadata {};

    Optional<ICCMultiChunkState> icc_multi_chunk_state;
    Optional<ByteBuffer> icc_data;
};

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
    u8 const bit = TRY(scan.huffman_stream.read_bits(1));
    if (bit == 1)
        coefficient |= 1 << scan.successive_approximation_low;

    return {};
}

enum class JPEGDecodingMode {
    Sequential,
    Progressive
};

template<JPEGDecodingMode DecodingMode>
static ErrorOr<void> add_dc(JPEGLoadingContext& context, Macroblock& macroblock, ScanComponent const& scan_component)
{
    auto maybe_table = context.dc_tables.get(scan_component.dc_destination_id);
    if (!maybe_table.has_value()) {
        dbgln_if(JPEG_DEBUG, "Unable to find a DC table with id: {}", scan_component.dc_destination_id);
        return Error::from_string_literal("Unable to find corresponding DC table");
    }

    auto& dc_table = maybe_table.value();
    auto& scan = *context.current_scan;

    auto* select_component = get_component(macroblock, scan_component.component.index);
    auto& coefficient = select_component[0];

    if (DecodingMode == JPEGDecodingMode::Progressive && scan.successive_approximation_high > 0) {
        TRY(refine_coefficient(scan, coefficient));
        return {};
    }

    // For DC coefficients, symbol encodes the length of the coefficient.
    auto dc_length = TRY(scan.huffman_stream.next_symbol(dc_table));

    // F.1.2.1.2 - Defining Huffman tables for the DC coefficients
    // F.1.5.1 - Structure of DC code table for 12-bit sample precision
    if ((context.frame.precision == 8 && dc_length > 11)
        || (context.frame.precision == 12 && dc_length > 15)) {
        dbgln_if(JPEG_DEBUG, "DC coefficient too long: {}!", dc_length);
        return Error::from_string_literal("DC coefficient too long");
    }

    // DC coefficients are encoded as the difference between previous and current DC values.
    i16 dc_diff = TRY(scan.huffman_stream.read_bits(dc_length));

    // If MSB in diff is 0, the difference is -ve. Otherwise +ve.
    if (dc_length != 0 && dc_diff < (1 << (dc_length - 1)))
        dc_diff -= (1 << dc_length) - 1;

    auto& previous_dc = context.previous_dc_values[scan_component.component.index];
    previous_dc += dc_diff;
    coefficient = previous_dc << scan.successive_approximation_low;

    return {};
}

template<JPEGDecodingMode DecodingMode>
static ALWAYS_INLINE ErrorOr<bool> read_eob(Scan& scan, u32 symbol)
{
    // OPTIMIZATION: This is a fast path for sequential JPEGs, these
    //               only supports EOB with a value of one block.
    if constexpr (DecodingMode == JPEGDecodingMode::Sequential)
        return symbol == 0x00;

    // G.1.2.2 - Progressive encoding of AC coefficients with Huffman coding
    // Note: We also use it for non-progressive encoding as it supports both EOB and ZRL

    if (auto const eob = symbol & 0x0F; eob == 0 && symbol != JPEG_ZRL) {
        // We encountered an EOB marker
        auto const eob_base = symbol >> 4;
        auto const additional_value = TRY(scan.huffman_stream.read_bits(eob_base));

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

template<JPEGDecodingMode DecodingMode>
static ErrorOr<void> add_ac(JPEGLoadingContext& context, Macroblock& macroblock, ScanComponent const& scan_component)
{
    auto maybe_table = context.ac_tables.get(scan_component.ac_destination_id);
    if (!maybe_table.has_value()) {
        dbgln_if(JPEG_DEBUG, "Unable to find a AC table with id: {}", scan_component.ac_destination_id);
        return Error::from_string_literal("Unable to find corresponding AC table");
    }

    auto& ac_table = maybe_table.value();
    auto* select_component = get_component(macroblock, scan_component.component.index);

    auto& scan = *context.current_scan;

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
            saved_symbol = TRY(scan.huffman_stream.next_symbol(ac_table));

            if (!TRY(read_eob<DecodingMode>(scan, *saved_symbol))) {
                to_skip = *saved_symbol >> 4;

                in_zrl = *saved_symbol == JPEG_ZRL;
                if (in_zrl) {
                    to_skip++;
                    saved_symbol.clear();
                }

                if constexpr (DecodingMode == JPEGDecodingMode::Sequential) {
                    j += to_skip - 1;
                    to_skip = 0;
                    in_zrl = false;
                    continue;
                }

                if constexpr (DecodingMode == JPEGDecodingMode::Progressive) {
                    if (!in_zrl && scan.successive_approximation_high != 0) {
                        // G.1.2.3 - Coding model for subsequent scans of successive approximation
                        // Bit sign from rule a
                        saved_bit_for_rule_a = TRY(scan.huffman_stream.read_bits(1));
                    }
                }
            } else if constexpr (DecodingMode == JPEGDecodingMode::Sequential) {
                break;
            }
        }

        if constexpr (DecodingMode == JPEGDecodingMode::Progressive) {
            if (coefficient != 0) {
                TRY(refine_coefficient(scan, coefficient));
                continue;
            }
        }

        if (to_skip > 0) {
            --to_skip;
            if (to_skip == 0)
                in_zrl = false;
            continue;
        }

        if (scan.end_of_bands_run_count > 0)
            continue;

        if (DecodingMode == JPEGDecodingMode::Progressive && scan.successive_approximation_high != 0) {
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

            // F.1.2.2.1 - Structure of AC code table
            // F.1.5.2 - Structure of AC code table for 12-bit sample precision
            if ((context.frame.precision == 8 && coeff_length > 10)
                || (context.frame.precision == 12 && coeff_length > 14)) {
                dbgln_if(JPEG_DEBUG, "AC coefficient too long: {}!", coeff_length);
                return Error::from_string_literal("AC coefficient too long");
            }

            if (coeff_length != 0) {
                i32 ac_coefficient = TRY(scan.huffman_stream.read_bits(coeff_length));
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
template<JPEGDecodingMode DecodingMode>
static ErrorOr<void> build_macroblocks(JPEGLoadingContext& context, Vector<Macroblock>& macroblocks, u32 hcursor, u32 vcursor)
{
    for (auto const& scan_component : context.current_scan->components) {
        for (u8 vfactor_i = 0; vfactor_i < scan_component.component.sampling_factors.vertical; vfactor_i++) {
            for (u8 hfactor_i = 0; hfactor_i < scan_component.component.sampling_factors.horizontal; hfactor_i++) {
                // A.2.3 - Interleaved order
                u32 macroblock_index = (vcursor + vfactor_i) * context.mblock_meta.hpadded_count + (hfactor_i + hcursor);
                if (!context.current_scan->are_components_interleaved()) {
                    macroblock_index = vcursor * context.mblock_meta.hpadded_count + (hfactor_i + (hcursor * scan_component.component.sampling_factors.vertical) + (vfactor_i * scan_component.component.sampling_factors.horizontal));

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

                if constexpr (DecodingMode == JPEGDecodingMode::Sequential) {
                    TRY(add_dc<DecodingMode>(context, block, scan_component));
                    TRY(add_ac<DecodingMode>(context, block, scan_component));
                } else {
                    if (context.current_scan->spectral_selection_start == 0)
                        TRY(add_dc<DecodingMode>(context, block, scan_component));
                    if (context.current_scan->spectral_selection_end != 0)
                        TRY(add_ac<DecodingMode>(context, block, scan_component));

                    // G.1.2.2 - Progressive encoding of AC coefficients with Huffman coding
                    if (context.current_scan->end_of_bands_run_count > 0) {
                        --context.current_scan->end_of_bands_run_count;
                        continue;
                    }
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
    context.current_scan->end_of_bands_run_count = 0;

    // E.2.4 Control procedure for decoding a restart interval
    if (is_dct_based(context.frame.type)) {
        context.previous_dc_values = {};
        return;
    }

    VERIFY_NOT_REACHED();
}

static ErrorOr<void> decode_huffman_stream(JPEGLoadingContext& context, Vector<Macroblock>& macroblocks)
{
    for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.sampling_factors.vertical) {
        for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.sampling_factors.horizontal) {
            // FIXME: This is likely wrong for non-interleaved scans.
            VERIFY(context.mblock_meta.hpadded_count % context.sampling_factors.horizontal == 0);
            u32 number_of_mcus_decoded_so_far = ((vcursor / context.sampling_factors.vertical) * context.mblock_meta.hpadded_count + hcursor) / context.sampling_factors.horizontal;

            auto& huffman_stream = context.current_scan->huffman_stream;

            if (context.dc_restart_interval > 0) {
                if (number_of_mcus_decoded_so_far != 0 && number_of_mcus_decoded_so_far % context.dc_restart_interval == 0) {
                    reset_decoder(context);

                    // Restart markers are stored in byte boundaries. Advance the huffman stream cursor to
                    //  the 0th bit of the next byte.
                    TRY(huffman_stream.advance_to_byte_boundary());

                    // Skip the restart marker (RSTn).
                    TRY(huffman_stream.discard_bits(8));
                }
            }

            auto result = [&]() {
                if (is_progressive(context.frame.type))
                    return build_macroblocks<JPEGDecodingMode::Progressive>(context, macroblocks, hcursor, vcursor);
                return build_macroblocks<JPEGDecodingMode::Sequential>(context, macroblocks, hcursor, vcursor);
            }();

            if (result.is_error()) {
                if constexpr (JPEG_DEBUG) {
                    dbgln("Failed to build Macroblock {}: {}", number_of_mcus_decoded_so_far, result.error());
                    dbgln("Huffman stream byte offset {:#x}", context.stream.byte_offset());
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
    case JPEG_SOF1:
    case JPEG_SOF2:
    case JPEG_SOI:
    case JPEG_SOS:
        return true;
    }

    if (is_frame_marker(marker))
        dbgln_if(JPEG_DEBUG, "Decoding this frame-type (SOF{}) is not currently supported. Decoder will fail!", marker & 0xf);

    return false;
}

static inline ErrorOr<Marker> read_until_marker(JPEGStream& stream)
{
    u16 marker = TRY(stream.read_u16());

    while (!is_supported_marker(marker))
        marker = marker << 8 | TRY(stream.read_u8());

    return marker;
}

static ErrorOr<u16> read_effective_chunk_size(JPEGStream& stream)
{
    // The stored chunk size includes the size of `stored_size` itself.
    u16 const stored_size = TRY(stream.read_u16());
    if (stored_size < 2)
        return Error::from_string_literal("Stored chunk size is too small");
    return stored_size - 2;
}

static ErrorOr<void> ensure_quantization_tables_are_present(JPEGLoadingContext& context)
{
    for (auto const& component : context.current_scan->components) {
        if (!context.registered_quantization_tables[component.component.quantization_table_id])
            return Error::from_string_literal("Unknown quantization table id");
    }
    return {};
}

static ErrorOr<void> read_start_of_scan(JPEGStream& stream, JPEGLoadingContext& context)
{
    // B.2.3 - Scan header syntax

    if (context.state < JPEGLoadingContext::State::FrameDecoded)
        return Error::from_string_literal("SOS found before reading a SOF");

    [[maybe_unused]] u16 const bytes_to_read = TRY(read_effective_chunk_size(stream));
    u8 const component_count = TRY(stream.read_u8());

    Scan current_scan(HuffmanStream { context.stream });

    Optional<u8> last_read;
    u8 component_read = 0;
    for (auto& component : context.components) {
        // See the Csj paragraph:
        // [...] the ordering in the scan header shall follow the ordering in the frame header.
        if (component_read == component_count)
            break;

        if (!last_read.has_value())
            last_read = TRY(stream.read_u8());

        if (component.id != *last_read)
            continue;

        u8 const table_ids = TRY(stream.read_u8());

        current_scan.components.empend(component, static_cast<u8>(table_ids >> 4), static_cast<u8>(table_ids & 0x0F));

        component_read++;
        last_read.clear();
    }

    if constexpr (JPEG_DEBUG) {
        StringBuilder builder;
        TRY(builder.try_append("Components in scan: "sv));
        for (auto const& scan_component : current_scan.components) {
            TRY(builder.try_append(String::number(scan_component.component.id)));
            TRY(builder.try_append(' '));
        }
        dbgln(builder.string_view());
    }

    current_scan.spectral_selection_start = TRY(stream.read_u8());
    current_scan.spectral_selection_end = TRY(stream.read_u8());
    auto const successive_approximation = TRY(stream.read_u8());
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

    TRY(ensure_quantization_tables_are_present(context));

    return {};
}

static ErrorOr<void> read_restart_interval(JPEGStream& stream, JPEGLoadingContext& context)
{
    // B.2.4.4 - Restart interval definition syntax
    u16 bytes_to_read = TRY(read_effective_chunk_size(stream));
    if (bytes_to_read != 2) {
        dbgln_if(JPEG_DEBUG, "Malformed DRI marker found!");
        return Error::from_string_literal("Malformed DRI marker found");
    }
    context.dc_restart_interval = TRY(stream.read_u16());
    dbgln_if(JPEG_DEBUG, "Restart marker: {}", context.dc_restart_interval);
    return {};
}

static ErrorOr<void> read_huffman_table(JPEGStream& stream, JPEGLoadingContext& context)
{
    // B.2.4.2 - Huffman table-specification syntax

    u16 bytes_to_read = TRY(read_effective_chunk_size(stream));

    while (bytes_to_read > 0) {
        HuffmanTable table;
        u8 const table_info = TRY(stream.read_u8());
        u8 const table_type = table_info >> 4;
        u8 const table_destination_id = table_info & 0x0F;
        if (table_type > 1) {
            dbgln_if(JPEG_DEBUG, "Unrecognized huffman table: {}!", table_type);
            return Error::from_string_literal("Unrecognized huffman table");
        }

        if ((context.frame.type == StartOfFrame::FrameType::Baseline_DCT && table_destination_id > 1)
            || (context.frame.type != StartOfFrame::FrameType::Baseline_DCT && table_destination_id > 3)) {
            dbgln_if(JPEG_DEBUG, "Invalid huffman table destination id: {}!", table_destination_id);
            return Error::from_string_literal("Invalid huffman table destination id");
        }

        table.type = table_type;
        table.destination_id = table_destination_id;
        u32 total_codes = 0;

        // Read code counts. At each index K, the value represents the number of K+1 bit codes in this header.
        for (int i = 0; i < 16; i++) {
            if (i == HuffmanTable::bits_per_cached_code)
                table.first_non_cached_code_index = total_codes;
            u8 const count = TRY(stream.read_u8());
            total_codes += count;
            table.code_counts[i] = count;
        }

        table.codes.ensure_capacity(total_codes);
        table.symbols.ensure_capacity(total_codes);

        // Read symbols. Read X bytes, where X is the sum of the counts of codes read in the previous step.
        for (u32 i = 0; i < total_codes; i++) {
            u8 symbol = TRY(stream.read_u8());
            table.symbols.append(symbol);
        }

        TRY(table.generate_codes());

        auto& huffman_table = table.type == 0 ? context.dc_tables : context.ac_tables;
        huffman_table.set(table.destination_id, table);

        bytes_to_read -= 1 + 16 + total_codes;
    }

    if (bytes_to_read != 0) {
        dbgln_if(JPEG_DEBUG, "Extra bytes detected in huffman header!");
        return Error::from_string_literal("Extra bytes detected in huffman header");
    }
    return {};
}

static ErrorOr<void> read_icc_profile(JPEGStream& stream, JPEGLoadingContext& context, int bytes_to_read)
{
    // https://www.color.org/technotes/ICC-Technote-ProfileEmbedding.pdf, page 5, "JFIF".
    if (bytes_to_read <= 2) {
        dbgln_if(JPEG_DEBUG, "icc marker too small");
        TRY(stream.discard(bytes_to_read));
        return {};
    }

    auto chunk_sequence_number = TRY(stream.read_u8()); // 1-based
    auto number_of_chunks = TRY(stream.read_u8());
    bytes_to_read -= 2;

    if (!context.icc_multi_chunk_state.has_value())
        context.icc_multi_chunk_state.emplace(ICCMultiChunkState { 0, TRY(FixedArray<ByteBuffer>::create(number_of_chunks)) });
    auto& chunk_state = context.icc_multi_chunk_state;

    u8 index {};

    auto const ensure_correctness = [&]() -> ErrorOr<void> {
        if (chunk_state->seen_number_of_icc_chunks >= number_of_chunks)
            return Error::from_string_literal("Too many ICC chunks");

        if (chunk_state->chunks.size() != number_of_chunks)
            return Error::from_string_literal("Inconsistent number of total ICC chunks");

        if (chunk_sequence_number == 0)
            return Error::from_string_literal("ICC chunk sequence number not 1 based");

        index = chunk_sequence_number - 1;

        if (index >= chunk_state->chunks.size())
            return Error::from_string_literal("ICC chunk sequence number larger than number of chunks");

        if (!chunk_state->chunks[index].is_empty())
            return Error::from_string_literal("Duplicate ICC chunk at sequence number");

        return {};
    };

    if (auto result = ensure_correctness(); result.is_error()) {
        dbgln_if(JPEG_DEBUG, "JPEG: {}", result.release_error());
        TRY(stream.discard(bytes_to_read));
        return {};
    }

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

static ErrorOr<void> read_colour_encoding(JPEGStream& stream, [[maybe_unused]] JPEGLoadingContext& context, int bytes_to_read)
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

    [[maybe_unused]] auto const version = TRY(stream.read_u8());
    [[maybe_unused]] u16 const flag0 = TRY(stream.read_u16());
    [[maybe_unused]] u16 const flag1 = TRY(stream.read_u16());
    auto const color_transform = TRY(stream.read_u8());

    if (bytes_to_read > 6) {
        dbgln_if(JPEG_DEBUG, "Unread bytes in App14 segment: {}", bytes_to_read - 6);
        TRY(stream.discard(bytes_to_read - 6));
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
        dbgln("{:#x} is not a specified transform flag value, ignoring", color_transform);
    }

    return {};
}

static ErrorOr<void> read_exif(JPEGStream& stream, JPEGLoadingContext& context, int bytes_to_read)
{
    // This refers to Exif's specification, see TIFFLoader for more information.
    // 4.7.2.2. - APP1 internal structure
    if (bytes_to_read <= 1) {
        TRY(stream.discard(bytes_to_read));
        return {};
    }

    // Discard padding byte
    TRY(stream.discard(1));

    auto exif_buffer = TRY(ByteBuffer::create_uninitialized(bytes_to_read - 1));
    TRY(stream.read_until_filled(exif_buffer));

    context.exif_metadata = TRY(TIFFImageDecoderPlugin::read_exif_metadata(exif_buffer));

    return {};
}

static ErrorOr<void> read_app_marker(JPEGStream& stream, JPEGLoadingContext& context, int app_marker_number)
{
    // B.2.4.6 - Application data syntax

    u16 bytes_to_read = TRY(read_effective_chunk_size(stream));

    StringBuilder builder;
    for (;;) {
        if (bytes_to_read == 0) {
            dbgln_if(JPEG_DEBUG, "app marker {} does not start with zero-terminated string", app_marker_number);
            return {};
        }

        auto c = TRY(stream.read_u8());
        bytes_to_read--;

        if (c == '\0')
            break;

        TRY(builder.try_append(c));
    }

    auto app_id = TRY(builder.to_string());

    if (app_marker_number == 1 && app_id == "Exif"sv)
        return read_exif(stream, context, bytes_to_read);
    if (app_marker_number == 2 && app_id == "ICC_PROFILE"sv)
        return read_icc_profile(stream, context, bytes_to_read);
    if (app_marker_number == 14 && app_id == "Adobe"sv)
        return read_colour_encoding(stream, context, bytes_to_read);

    return stream.discard(bytes_to_read);
}

static inline bool validate_sampling_factors_and_modify_context(SamplingFactors const& sampling_factors, JPEGLoadingContext& context)
{
    if ((sampling_factors.horizontal == 1 || sampling_factors.horizontal == 2) && (sampling_factors.vertical == 1 || sampling_factors.vertical == 2)) {
        context.mblock_meta.hpadded_count += sampling_factors.horizontal == 1 ? 0 : context.mblock_meta.hcount % 2;
        context.mblock_meta.vpadded_count += sampling_factors.vertical == 1 ? 0 : context.mblock_meta.vcount % 2;
        context.mblock_meta.padded_total = context.mblock_meta.hpadded_count * context.mblock_meta.vpadded_count;
        // For easy reference to relevant sample factors.
        context.sampling_factors = sampling_factors;

        return true;
    }
    return false;
}

static inline void set_macroblock_metadata(JPEGLoadingContext& context)
{
    context.mblock_meta.hcount = ceil_div<u32>(context.frame.width, 8);
    context.mblock_meta.vcount = ceil_div<u32>(context.frame.height, 8);
    context.mblock_meta.hpadded_count = context.mblock_meta.hcount;
    context.mblock_meta.vpadded_count = context.mblock_meta.vcount;
    context.mblock_meta.total = context.mblock_meta.hcount * context.mblock_meta.vcount;
}

static ErrorOr<void> ensure_standard_precision(StartOfFrame const& frame)
{
    // B.2.2 - Frame header syntax
    // Table B.2 - Frame header parameter sizes and values

    if (frame.precision == 8)
        return {};

    if (frame.type == StartOfFrame::FrameType::Extended_Sequential_DCT && frame.precision == 12)
        return {};

    if (frame.type == StartOfFrame::FrameType::Progressive_DCT && frame.precision == 12)
        return {};

    dbgln_if(JPEG_DEBUG, "Unsupported precision: {}, for SOF type: {}!", frame.precision, static_cast<int>(frame.type));
    return Error::from_string_literal("Unsupported SOF precision.");
}

static ErrorOr<void> read_start_of_frame(JPEGStream& stream, JPEGLoadingContext& context)
{
    if (context.state == JPEGLoadingContext::FrameDecoded) {
        dbgln_if(JPEG_DEBUG, "SOF repeated!");
        return Error::from_string_literal("SOF repeated");
    }

    // B.2.2 Frame header syntax

    [[maybe_unused]] u16 const bytes_to_read = TRY(read_effective_chunk_size(stream));

    context.frame.precision = TRY(stream.read_u8());

    TRY(ensure_standard_precision(context.frame));

    context.frame.height = TRY(stream.read_u16());
    context.frame.width = TRY(stream.read_u16());
    if (!context.frame.width || !context.frame.height) {
        dbgln_if(JPEG_DEBUG, "ERROR! Image height: {}, Image width: {}!", context.frame.height, context.frame.width);
        return Error::from_string_literal("Image frame height of width null");
    }

    set_macroblock_metadata(context);

    auto component_count = TRY(stream.read_u8());
    if (component_count != 1 && component_count != 3 && component_count != 4) {
        dbgln_if(JPEG_DEBUG, "Unsupported number of components in SOF: {}!", component_count);
        return Error::from_string_literal("Unsupported number of components in SOF");
    }

    for (u8 i = 0; i < component_count; i++) {
        Component component;
        component.id = TRY(stream.read_u8());
        component.index = i;

        u8 subsample_factors = TRY(stream.read_u8());
        component.sampling_factors.horizontal = subsample_factors >> 4;
        component.sampling_factors.vertical = subsample_factors & 0x0F;

        if (component_count == 1) {
            // 4.8.2 Minimum coded unit: "If the compressed image data is non-interleaved, the MCU is defined to be one data unit."
            component.sampling_factors = { 1, 1 };
        }

        dbgln_if(JPEG_DEBUG, "Component subsampling: {}, {}", component.sampling_factors.horizontal, component.sampling_factors.vertical);

        if (component.sampling_factors.horizontal == 0 || component.sampling_factors.horizontal > 4
            || component.sampling_factors.vertical == 0 || component.sampling_factors.vertical > 4) {
            return Error::from_string_literal("Invalid subsampling factor values");
        }

        if (i == 0) {
            // By convention, downsampling is applied only on chroma components. So we should
            //  hope to see the maximum sampling factor in the luma component.
            if (!validate_sampling_factors_and_modify_context(component.sampling_factors, context)) {
                dbgln_if(JPEG_DEBUG, "Unsupported luma subsampling factors: horizontal: {}, vertical: {}",
                    component.sampling_factors.horizontal,
                    component.sampling_factors.vertical);
                return Error::from_string_literal("Unsupported luma subsampling factors");
            }
        } else {
            auto const& y_component = context.components[0];
            if (y_component.sampling_factors.horizontal % component.sampling_factors.horizontal != 0
                || y_component.sampling_factors.vertical % component.sampling_factors.vertical != 0) {
                dbgln_if(JPEG_DEBUG, "Unsupported chroma subsampling factors: horizontal: {}, vertical: {}",
                    component.sampling_factors.horizontal,
                    component.sampling_factors.vertical);
                return Error::from_string_literal("Unsupported chroma subsampling factors");
            }
        }

        component.quantization_table_id = TRY(stream.read_u8());

        context.components.append(move(component));
    }

    return {};
}

static ErrorOr<void> read_quantization_table(JPEGStream& stream, JPEGLoadingContext& context)
{
    // B.2.4.1 - Quantization table-specification syntax

    u16 bytes_to_read = TRY(read_effective_chunk_size(stream));

    while (bytes_to_read > 0) {
        u8 const info_byte = TRY(stream.read_u8());
        u8 const element_unit_hint = info_byte >> 4;
        if (element_unit_hint > 1) {
            dbgln_if(JPEG_DEBUG, "Unsupported unit hint in quantization table: {}!", element_unit_hint);
            return Error::from_string_literal("Unsupported unit hint in quantization table");
        }
        u8 const table_id = info_byte & 0x0F;

        if (table_id > 3) {
            dbgln_if(JPEG_DEBUG, "Unsupported quantization table id: {}!", table_id);
            return Error::from_string_literal("Unsupported quantization table id");
        }

        context.registered_quantization_tables[table_id] = true;

        auto& table = context.quantization_tables[table_id];

        for (int i = 0; i < 64; i++) {
            if (element_unit_hint == 0)
                table[zigzag_map[i]] = TRY(stream.read_u8());
            else
                table[zigzag_map[i]] = TRY(stream.read_u16());
        }

        bytes_to_read -= 1 + (element_unit_hint == 0 ? 64 : 128);
    }
    if (bytes_to_read != 0) {
        dbgln_if(JPEG_DEBUG, "Invalid length for one or more quantization tables!");
        return Error::from_string_literal("Invalid length for one or more quantization tables");
    }

    return {};
}

static ErrorOr<void> skip_segment(JPEGStream& stream)
{
    u16 bytes_to_skip = TRY(read_effective_chunk_size(stream));
    TRY(stream.discard(bytes_to_skip));
    return {};
}

static void dequantize(JPEGLoadingContext& context, Vector<Macroblock>& macroblocks)
{
    for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.sampling_factors.vertical) {
        for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.sampling_factors.horizontal) {
            for (u32 i = 0; i < context.components.size(); i++) {
                auto const& component = context.components[i];

                auto const& table = context.quantization_tables[component.quantization_table_id];

                for (u32 vfactor_i = 0; vfactor_i < component.sampling_factors.vertical; vfactor_i++) {
                    for (u32 hfactor_i = 0; hfactor_i < component.sampling_factors.horizontal; hfactor_i++) {
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

static void inverse_dct_8x8(i16* block_component)
{
    // Does a 2-D IDCT by doing two 1-D IDCTs as described in https://unix4lyfe.org/dct/
    // The 1-D DCT idea is described at https://unix4lyfe.org/dct-1d/, read aan.cc from bottom to top.
    static float const m0 = 2.0f * AK::cos(1.0f / 16.0f * 2.0f * AK::Pi<float>);
    static float const m1 = 2.0f * AK::cos(2.0f / 16.0f * 2.0f * AK::Pi<float>);
    static float const m3 = 2.0f * AK::cos(2.0f / 16.0f * 2.0f * AK::Pi<float>);
    static float const m5 = 2.0f * AK::cos(3.0f / 16.0f * 2.0f * AK::Pi<float>);
    static float const m2 = m0 - m5;
    static float const m4 = m0 + m5;
    static float const s0 = AK::cos(0.0f / 16.0f * AK::Pi<float>) / AK::sqrt(8.0f);
    static float const s1 = AK::cos(1.0f / 16.0f * AK::Pi<float>) / 2.0f;
    static float const s2 = AK::cos(2.0f / 16.0f * AK::Pi<float>) / 2.0f;
    static float const s3 = AK::cos(3.0f / 16.0f * AK::Pi<float>) / 2.0f;
    static float const s4 = AK::cos(4.0f / 16.0f * AK::Pi<float>) / 2.0f;
    static float const s5 = AK::cos(5.0f / 16.0f * AK::Pi<float>) / 2.0f;
    static float const s6 = AK::cos(6.0f / 16.0f * AK::Pi<float>) / 2.0f;
    static float const s7 = AK::cos(7.0f / 16.0f * AK::Pi<float>) / 2.0f;

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

static void inverse_dct(JPEGLoadingContext const& context, Vector<Macroblock>& macroblocks)
{
    for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.sampling_factors.vertical) {
        for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.sampling_factors.horizontal) {
            for (u32 component_i = 0; component_i < context.components.size(); component_i++) {
                auto& component = context.components[component_i];
                for (u8 vfactor_i = 0; vfactor_i < component.sampling_factors.vertical; vfactor_i++) {
                    for (u8 hfactor_i = 0; hfactor_i < component.sampling_factors.horizontal; hfactor_i++) {
                        u32 macroblock_index = (vcursor + vfactor_i) * context.mblock_meta.hpadded_count + (hfactor_i + hcursor);
                        Macroblock& block = macroblocks[macroblock_index];
                        auto* block_component = get_component(block, component_i);
                        inverse_dct_8x8(block_component);
                    }
                }
            }
        }
    }

    // F.2.1.5 - Inverse DCT (IDCT)
    auto const level_shift = 1 << (context.frame.precision - 1);
    auto const max_value = (1 << context.frame.precision) - 1;
    for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.sampling_factors.vertical) {
        for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.sampling_factors.horizontal) {
            for (u8 vfactor_i = 0; vfactor_i < context.sampling_factors.vertical; ++vfactor_i) {
                for (u8 hfactor_i = 0; hfactor_i < context.sampling_factors.horizontal; ++hfactor_i) {
                    u32 mb_index = (vcursor + vfactor_i) * context.mblock_meta.hpadded_count + (hcursor + hfactor_i);
                    for (u8 i = 0; i < 8; ++i) {
                        for (u8 j = 0; j < 8; ++j) {

                            // FIXME: This just truncate all coefficients, it's an easy way to support (read hack)
                            //        12 bits JPEGs without rewriting all color transformations.
                            auto const clamp_to_8_bits = [&](u16 color) -> u8 {
                                if (context.frame.precision == 8)
                                    return static_cast<u8>(color);
                                return static_cast<u8>(color >> 4);
                            };

                            macroblocks[mb_index].r[i * 8 + j] = clamp_to_8_bits(clamp(macroblocks[mb_index].r[i * 8 + j] + level_shift, 0, max_value));
                            macroblocks[mb_index].g[i * 8 + j] = clamp_to_8_bits(clamp(macroblocks[mb_index].g[i * 8 + j] + level_shift, 0, max_value));
                            macroblocks[mb_index].b[i * 8 + j] = clamp_to_8_bits(clamp(macroblocks[mb_index].b[i * 8 + j] + level_shift, 0, max_value));
                            macroblocks[mb_index].k[i * 8 + j] = clamp_to_8_bits(clamp(macroblocks[mb_index].k[i * 8 + j] + level_shift, 0, max_value));
                        }
                    }
                }
            }
        }
    }
}

static void undo_subsampling(JPEGLoadingContext const& context, Vector<Macroblock>& macroblocks)
{
    // The first component has sampling factors of context.sampling_factors, while the others
    // divide the first component's sampling factors. This is enforced by read_start_of_frame().
    // This function undoes the subsampling by duplicating the values of the smaller components.
    // See https://www.w3.org/Graphics/JPEG/itu-t81.pdf, A.2 Order of source image data encoding.
    //
    // FIXME: Allow more combinations of sampling factors.
    // See https://calendar.perfplanet.com/2015/why-arent-your-images-using-chroma-subsampling/ for
    // subsampling factors visble on the web. In PDF files, YCCK 2111 and 2112 and CMYK 2111 and 2112 are also present.
    for (u32 component_i = 0; component_i < context.components.size(); component_i++) {
        auto& component = context.components[component_i];
        if (component.sampling_factors == context.sampling_factors)
            continue;

        for (u32 vcursor = 0; vcursor < context.mblock_meta.vcount; vcursor += context.sampling_factors.vertical) {
            for (u32 hcursor = 0; hcursor < context.mblock_meta.hcount; hcursor += context.sampling_factors.horizontal) {
                u32 const component_block_index = vcursor * context.mblock_meta.hpadded_count + hcursor;
                Macroblock& component_block = macroblocks[component_block_index];
                auto* block_component_source = get_component(component_block, component_i);

                // Overflows are intentional.
                for (u8 vfactor_i = context.sampling_factors.vertical - 1; vfactor_i < context.sampling_factors.vertical; --vfactor_i) {
                    for (u8 hfactor_i = context.sampling_factors.horizontal - 1; hfactor_i < context.sampling_factors.horizontal; --hfactor_i) {
                        u32 macroblock_index = (vcursor + vfactor_i) * context.mblock_meta.hpadded_count + (hfactor_i + hcursor);
                        Macroblock& block = macroblocks[macroblock_index];
                        auto* block_component_destination = get_component(block, component_i);
                        for (u8 i = 7; i < 8; --i) {
                            for (u8 j = 7; j < 8; --j) {
                                u8 const pixel = i * 8 + j;
                                // The component is 8x8 subsampled 2x2. Upsample its 2x2 4x4 tiles.
                                u32 const component_pxrow = (i / context.sampling_factors.vertical) + 4 * vfactor_i;
                                u32 const component_pxcol = (j / context.sampling_factors.horizontal) + 4 * hfactor_i;
                                u32 const component_pixel = component_pxrow * 8 + component_pxcol;
                                block_component_destination[pixel] = block_component_source[component_pixel];
                            }
                        }
                    }
                }
            }
        }
    }
}

static void ycbcr_to_rgb(Vector<Macroblock>& macroblocks)
{
    // Conversion from YCbCr to RGB isn't specified in the first JPEG specification but in the JFIF extension:
    // See: https://www.itu.int/rec/dologin_pub.asp?lang=f&id=T-REC-T.871-201105-I!!PDF-E&type=items
    // 7 - Conversion to and from RGB
    for (auto& macroblock : macroblocks) {
        auto* y = macroblock.y;
        auto* cb = macroblock.cb;
        auto* cr = macroblock.cr;
        for (u8 i = 0; i < 64; ++i) {
            int r = y[i] + 1.402f * (cr[i] - 128);
            int g = y[i] - 0.3441f * (cb[i] - 128) - 0.7141f * (cr[i] - 128);
            int b = y[i] + 1.772f * (cb[i] - 128);
            y[i] = clamp(r, 0, 255);
            cb[i] = clamp(g, 0, 255);
            cr[i] = clamp(b, 0, 255);
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
    for (auto& macroblock : macroblocks) {
        for (u8 i = 0; i < 64; ++i) {
            macroblock.r[i] = 255 - macroblock.r[i];
            macroblock.g[i] = 255 - macroblock.g[i];
            macroblock.b[i] = 255 - macroblock.b[i];
            macroblock.k[i] = 255 - macroblock.k[i];
        }
    }
}

static void ycck_to_cmyk(Vector<Macroblock>& macroblocks)
{
    // 7 - Conversions between colour encodings
    // YCCK is obtained from CMYK by converting the CMY channels to YCC channel.

    // To convert back into RGB, we only need the 3 first components, which are baseline YCbCr
    ycbcr_to_rgb(macroblocks);

    // RGB to CMY, as mentioned in https://www.smcm.iqfr.csic.es/docs/intel/ipp/ipp_manual/IPPI/ippi_ch15/functn_YCCKToCMYK_JPEG.htm#functn_YCCKToCMYK_JPEG
    for (auto& macroblock : macroblocks) {
        for (u8 i = 0; i < 64; ++i) {
            macroblock.r[i] = 255 - macroblock.r[i];
            macroblock.g[i] = 255 - macroblock.g[i];
            macroblock.b[i] = 255 - macroblock.b[i];
        }
    }
}

static ErrorOr<void> handle_color_transform(JPEGLoadingContext const& context, Vector<Macroblock>& macroblocks)
{
    // Note: This is non-standard but some encoder still add the App14 segment for grayscale images.
    //       So let's ignore the color transform value if we only have one component.
    if (context.color_transform.has_value() && context.components.size() != 1) {
        // https://www.itu.int/rec/dologin_pub.asp?lang=e&id=T-REC-T.872-201206-I!!PDF-E&type=items
        // 6.5.3 - APP14 marker segment for colour encoding

        switch (*context.color_transform) {
        case ColorTransform::CmykOrRgb:
            if (context.components.size() == 4) {
                // Nothing to do here.
            } else if (context.components.size() == 3) {
                // Note: components.size() == 3 means that we have an RGB image, so no color transformation is needed.
            } else {
                return Error::from_string_literal("Wrong number of components for CMYK or RGB, aborting.");
            }
            break;
        case ColorTransform::YCbCr:
            ycbcr_to_rgb(macroblocks);
            break;
        case ColorTransform::YCCK:
            ycck_to_cmyk(macroblocks);
            break;
        }

        return {};
    }

    // No App14 segment is present, assuming :
    //      - 1 components means grayscale
    //      - 3 components means YCbCr
    //      - 4 components means CMYK (Nothing to do here).
    if (context.components.size() == 3)
        ycbcr_to_rgb(macroblocks);

    if (context.components.size() == 1) {
        // With Cb and Cr being equal to zero, this function assign the Y
        // value (luminosity) to R, G and B. Providing a proper conversion
        // from grayscale to RGB.
        ycbcr_to_rgb(macroblocks);
    }

    return {};
}

static ErrorOr<void> compose_bitmap(JPEGLoadingContext& context, Vector<Macroblock> const& macroblocks)
{
    context.bitmap = TRY(Bitmap::create(BitmapFormat::BGRx8888, { context.frame.width, context.frame.height }));

    for (u32 y = context.frame.height - 1; y < context.frame.height; y--) {
        u32 const block_row = y / 8;
        u32 const pixel_row = y % 8;
        for (u32 x = 0; x < context.frame.width; x++) {
            u32 const block_column = x / 8;
            auto& block = macroblocks[block_row * context.mblock_meta.hpadded_count + block_column];
            u32 const pixel_column = x % 8;
            u32 const pixel_index = pixel_row * 8 + pixel_column;
            Color const color { (u8)block.y[pixel_index], (u8)block.cb[pixel_index], (u8)block.cr[pixel_index] };
            context.bitmap->set_pixel(x, y, color);
        }
    }

    return {};
}

static ErrorOr<void> compose_cmyk_bitmap(JPEGLoadingContext& context, Vector<Macroblock>& macroblocks)
{
    if (context.options.cmyk == JPEGDecoderOptions::CMYK::Normal)
        invert_colors_for_adobe_images(context, macroblocks);

    context.cmyk_bitmap = TRY(Gfx::CMYKBitmap::create_with_size({ context.frame.width, context.frame.height }));

    for (u32 y = context.frame.height - 1; y < context.frame.height; y--) {
        u32 const block_row = y / 8;
        u32 const pixel_row = y % 8;
        for (u32 x = 0; x < context.frame.width; x++) {
            u32 const block_column = x / 8;
            auto& block = macroblocks[block_row * context.mblock_meta.hpadded_count + block_column];
            u32 const pixel_column = x % 8;
            u32 const pixel_index = pixel_row * 8 + pixel_column;
            context.cmyk_bitmap->scanline(y)[x] = { (u8)block.y[pixel_index], (u8)block.cb[pixel_index], (u8)block.cr[pixel_index], (u8)block.k[pixel_index] };
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

static ErrorOr<void> handle_miscellaneous_or_table(JPEGStream& stream, JPEGLoadingContext& context, Marker const marker)
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

static ErrorOr<void> parse_header(JPEGStream& stream, JPEGLoadingContext& context)
{
    auto marker = TRY(read_until_marker(stream));
    if (marker != JPEG_SOI) {
        dbgln_if(JPEG_DEBUG, "SOI not found: {:x}!", marker);
        return Error::from_string_literal("SOI not found");
    }
    for (;;) {
        marker = TRY(read_until_marker(stream));

        if (is_miscellaneous_or_table_marker(marker)) {
            TRY(handle_miscellaneous_or_table(stream, context, marker));
            continue;
        }

        // Set frame type if the marker marks a new frame.
        if (is_frame_marker(marker))
            context.frame.type = static_cast<StartOfFrame::FrameType>(marker & 0xF);

        switch (marker) {
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
        case JPEG_SOF1:
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

static ErrorOr<void> decode_header(JPEGLoadingContext& context)
{
    VERIFY(context.state < JPEGLoadingContext::State::HeaderDecoded);
    TRY(parse_header(context.stream, context));

    if constexpr (JPEG_DEBUG) {
        dbgln("Image width: {}", context.frame.width);
        dbgln("Image height: {}", context.frame.height);
        dbgln("Macroblocks in a row: {}", context.mblock_meta.hpadded_count);
        dbgln("Macroblocks in a column: {}", context.mblock_meta.vpadded_count);
        dbgln("Macroblock meta padded total: {}", context.mblock_meta.padded_total);
    }

    context.state = JPEGLoadingContext::State::HeaderDecoded;
    return {};
}

static ErrorOr<Vector<Macroblock>> construct_macroblocks(JPEGLoadingContext& context)
{
    // B.6 - Summary
    // See: Figure B.16 – Flow of compressed data syntax
    // This function handles the "Multi-scan" loop.

    Vector<Macroblock> macroblocks;
    TRY(macroblocks.try_resize(context.mblock_meta.padded_total));

    Marker marker = TRY(read_until_marker(context.stream));
    while (true) {
        if (is_miscellaneous_or_table_marker(marker)) {
            TRY(handle_miscellaneous_or_table(context.stream, context, marker));
        } else if (marker == JPEG_SOS) {
            TRY(read_start_of_scan(context.stream, context));
            TRY(decode_huffman_stream(context, macroblocks));
        } else if (marker == JPEG_EOI) {
            return macroblocks;
        } else {
            dbgln_if(JPEG_DEBUG, "Unexpected marker {:x}!", marker);
            return Error::from_string_literal("Unexpected marker");
        }

        marker = TRY(read_until_marker(context.stream));
    }
}

static ErrorOr<void> decode_jpeg(JPEGLoadingContext& context)
{
    auto macroblocks = TRY(construct_macroblocks(context));
    dequantize(context, macroblocks);
    inverse_dct(context, macroblocks);
    undo_subsampling(context, macroblocks);
    TRY(handle_color_transform(context, macroblocks));
    if (context.components.size() == 4)
        TRY(compose_cmyk_bitmap(context, macroblocks));
    else
        TRY(compose_bitmap(context, macroblocks));
    return {};
}

JPEGImageDecoderPlugin::JPEGImageDecoderPlugin(NonnullOwnPtr<JPEGLoadingContext> context)
    : m_context(move(context))
{
}

JPEGImageDecoderPlugin::~JPEGImageDecoderPlugin() = default;

IntSize JPEGImageDecoderPlugin::size()
{
    return { m_context->frame.width, m_context->frame.height };
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
    return create_with_options(data, {});
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> JPEGImageDecoderPlugin::create_with_options(ReadonlyBytes data, JPEGDecoderOptions options)
{
    auto stream = TRY(try_make<FixedMemoryStream>(data));
    auto context = TRY(JPEGLoadingContext::create(move(stream), options));
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) JPEGImageDecoderPlugin(move(context))));
    TRY(decode_header(*plugin->m_context));
    return plugin;
}

ErrorOr<ImageFrameDescriptor> JPEGImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
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

    if (m_context->cmyk_bitmap && !m_context->bitmap)
        return ImageFrameDescriptor { TRY(m_context->cmyk_bitmap->to_low_quality_rgb()), 0 };

    return ImageFrameDescriptor { m_context->bitmap, 0 };
}

Optional<Metadata const&> JPEGImageDecoderPlugin::metadata()
{
    if (m_context->exif_metadata)
        return *m_context->exif_metadata;
    return OptionalNone {};
}

ErrorOr<Optional<ReadonlyBytes>> JPEGImageDecoderPlugin::icc_data()
{
    if (m_context->icc_data.has_value())
        return *m_context->icc_data;
    return OptionalNone {};
}

NaturalFrameFormat JPEGImageDecoderPlugin::natural_frame_format() const
{
    if (m_context->state == JPEGLoadingContext::State::Error)
        return NaturalFrameFormat::RGB;
    VERIFY(m_context->state >= JPEGLoadingContext::State::HeaderDecoded);
    if (m_context->components.size() == 1)
        return NaturalFrameFormat::Grayscale;
    if (m_context->components.size() == 4)
        return NaturalFrameFormat::CMYK;
    return NaturalFrameFormat::RGB;
}

ErrorOr<NonnullRefPtr<CMYKBitmap>> JPEGImageDecoderPlugin::cmyk_frame()
{
    VERIFY(natural_frame_format() == NaturalFrameFormat::CMYK);

    if (m_context->state < JPEGLoadingContext::State::BitmapDecoded) {
        if (auto result = decode_jpeg(*m_context); result.is_error()) {
            m_context->state = JPEGLoadingContext::State::Error;
            return result.release_error();
        }
        m_context->state = JPEGLoadingContext::State::BitmapDecoded;
    }

    return *m_context->cmyk_bitmap;
}

}
