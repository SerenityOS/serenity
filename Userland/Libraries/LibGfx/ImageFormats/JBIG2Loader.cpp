/*
 * Copyright (c) 2024-2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitStream.h>
#include <AK/Debug.h>
#include <AK/Enumerate.h>
#include <AK/GenericShorthands.h>
#include <AK/IntegralMath.h>
#include <AK/Utf16View.h>
#include <LibGfx/ImageFormats/BilevelImage.h>
#include <LibGfx/ImageFormats/CCITTDecoder.h>
#include <LibGfx/ImageFormats/JBIG2Loader.h>
#include <LibGfx/ImageFormats/JBIG2Shared.h>
#include <LibGfx/ImageFormats/MQArithmeticCoder.h>
#include <LibTextCodec/Decoder.h>

// Spec: ITU-T_T_88__08_2018.pdf in the zip file here:
// https://www.itu.int/rec/T-REC-T.88-201808-I
// Annex H has a datastream example.

// That spec was published in 2018 and contains all previous amendments. Its history is:
// * 2002: Original spec published, describes decoding only. Has generic regions,
//         symbol regions, text regions, halftone regions, and pattern regions.
// * 2003: Amendment 1 approved. Describes encoding. Not interesting for us.
//   * 2004: (Amendment 1 erratum 1 approved. Not interesting for us.)
// * 2003: Amendment 2 approved. Added support for EXTTEMPLATE.
// * 2011: Amendment 3 approved. Added support for color coding
//         (COLEXTFLAG, CPCOMPLEN, CPDEFCOLS, CPEXCOLS, CPNCOMP, CPNVALS, GBCOLS,
//         GBCOMBOP, GBFGCOLID, SBCOLS, SBCOLSECTSIZE and SBFGCOLID).
// This history might explain why EXTTEMPLATE and colors are very rare in practice.

namespace Gfx {

namespace JBIG2 {

// Annex A, Arithmetic integer decoding procedure
class ArithmeticIntegerDecoder {
public:
    ArithmeticIntegerDecoder();

    // A.2 Procedure for decoding values (except IAID)
    // Returns OptionalNone for OOB.
    Optional<i32> decode(MQArithmeticDecoder&);

    // Returns Error for OOB.
    ErrorOr<i32> decode_non_oob(MQArithmeticDecoder&);

private:
    u16 PREV { 0 };
    Vector<MQArithmeticCoderContext> contexts;
};

ArithmeticIntegerDecoder::ArithmeticIntegerDecoder()
{
    contexts.resize(1 << 9);
}

Optional<int> ArithmeticIntegerDecoder::decode(MQArithmeticDecoder& decoder)
{
    // A.2 Procedure for decoding values (except IAID)
    // "1) Set:
    //    PREV = 1"
    u16 PREV = 1;

    // "2) Follow the flowchart in Figure A.1. Decode each bit with CX equal to "IAx + PREV" where "IAx" represents the identifier
    //     of the current arithmetic integer decoding procedure, "+" represents concatenation, and the rightmost 9 bits of PREV are used."
    auto decode_bit = [&]() {
        bool D = decoder.get_next_bit(contexts[PREV & 0x1FF]);
        // "3) After each bit is decoded:
        //     If PREV < 256 set:
        //         PREV = (PREV << 1) OR D
        //     Otherwise set:
        //         PREV = (((PREV << 1) OR D) AND 511) OR 256
        //     where D represents the value of the just-decoded bit.
        if (PREV < 256)
            PREV = (PREV << 1) | (u16)D;
        else
            PREV = (((PREV << 1) | (u16)D) & 511) | 256;
        return D;
    };

    auto decode_bits = [&](int n) {
        u32 result = 0;
        for (int i = 0; i < n; ++i)
            result = (result << 1) | decode_bit();
        return result;
    };

    // Figure A.1 – Flowchart for the integer arithmetic decoding procedures (except IAID)
    u8 S = decode_bit();
    u32 V;
    if (!decode_bit())
        V = decode_bits(2);
    else if (!decode_bit())
        V = decode_bits(4) + 4;
    else if (!decode_bit())
        V = decode_bits(6) + 20;
    else if (!decode_bit())
        V = decode_bits(8) + 84;
    else if (!decode_bit())
        V = decode_bits(12) + 340;
    else
        V = decode_bits(32) + 4436;

    // "4) The sequence of bits decoded, interpreted according to Table A.1, gives the value that is the result of this invocation
    //     of the integer arithmetic decoding procedure."
    if (S == 1 && V == 0)
        return {};
    return S ? -V : V;
}

ErrorOr<i32> ArithmeticIntegerDecoder::decode_non_oob(MQArithmeticDecoder& decoder)
{
    auto result = decode(decoder);
    if (!result.has_value())
        return Error::from_string_literal("ArithmeticIntegerDecoder: Unexpected OOB");
    return result.value();
}

class ArithmeticIntegerIDDecoder {
public:
    explicit ArithmeticIntegerIDDecoder(u32 code_length);

    // A.3 The IAID decoding procedure
    u32 decode(MQArithmeticDecoder&);

private:
    u32 m_code_length { 0 };
    Vector<MQArithmeticCoderContext> contexts;
};

ArithmeticIntegerIDDecoder::ArithmeticIntegerIDDecoder(u32 code_length)
    : m_code_length(code_length)
{
    contexts.resize(1 << (code_length + 1));
}

u32 ArithmeticIntegerIDDecoder::decode(MQArithmeticDecoder& decoder)
{
    // A.3 The IAID decoding procedure
    u32 prev = 1;
    for (u8 i = 0; i < m_code_length; ++i) {
        bool bit = decoder.get_next_bit(contexts[prev]);
        prev = (prev << 1) | bit;
    }
    prev = prev - (1 << m_code_length);
    return prev;
}

struct Code {
    u16 prefix_length {};         // "PREFLEN" in spec. High bit set for lower range table line.
    u8 range_length {};           // "RANGELEN" in spec.
    Optional<i32> first_value {}; // First number in "VAL" in spec.
    u32 code {};                  // "Encoding" in spec.

    constexpr static int LowerRangeBit = 0x8000;
};

// Table B.1 – Standard Huffman table A
constexpr Array standard_huffman_table_A = {
    Code { 1, 4, 0, 0b0 },
    Code { 2, 8, 16, 0b10 },
    Code { 3, 16, 272, 0b110 },
    Code { 3, 32, 65808, 0b111 },
};

// Table B.2 – Standard Huffman table B
constexpr Array standard_huffman_table_B = {
    Code { 1, 0, 0, 0b0 },
    Code { 2, 0, 1, 0b10 },
    Code { 3, 0, 2, 0b110 },
    Code { 4, 3, 3, 0b1110 },
    Code { 5, 6, 11, 0b11110 },
    Code { 6, 32, 75, 0b111110 },
    Code { 6, 0, OptionalNone {}, 0b111111 },
};

// Table B.4 – Standard Huffman table D
constexpr Array standard_huffman_table_D = {
    Code { 1, 0, 1, 0b0 },
    Code { 2, 0, 2, 0b10 },
    Code { 3, 0, 3, 0b110 },
    Code { 4, 3, 4, 0b1110 },
    Code { 5, 6, 12, 0b11110 },
    Code { 5, 32, 76, 0b11111 },
};

// Table B.6 – Standard Huffman table F
constexpr Array standard_huffman_table_F = {
    Code { 5, 10, -2048, 0b11100 },
    Code { 4, 9, -1024, 0b1000 },
    Code { 4, 8, -512, 0b1001 },
    Code { 4, 7, -256, 0b1010 },
    Code { 5, 6, -128, 0b11101 },
    Code { 5, 5, -64, 0b11110 },
    Code { 4, 5, -32, 0b1011 },
    Code { 2, 7, 0, 0b00 },
    Code { 3, 7, 128, 0b010 },
    Code { 3, 8, 256, 0b011 },
    Code { 4, 9, 512, 0b1100 },
    Code { 4, 10, 1024, 0b1101 },
    Code { 6 | Code::LowerRangeBit, 32, -2049, 0b111110 },
    Code { 6, 32, 2048, 0b111111 },
};

// Table B.7 – Standard Huffman table G
constexpr Array standard_huffman_table_G = {
    Code { 4, 9, -1024, 0b1000 },
    Code { 3, 8, -512, 0b000 },
    Code { 4, 7, -256, 0b1001 },
    Code { 5, 6, -128, 0b11010 },
    Code { 5, 5, -64, 0b11011 },
    Code { 4, 5, -32, 0b1010 },
    Code { 4, 5, 0, 0b1011 },
    Code { 5, 5, 32, 0b11100 },
    Code { 5, 6, 64, 0b11101 },
    Code { 4, 7, 128, 0b1100 },
    Code { 3, 8, 256, 0b001 },
    Code { 3, 9, 512, 0b010 },
    Code { 3, 10, 1024, 0b011 },
    Code { 5 | Code::LowerRangeBit, 32, -1025, 0b11110 },
    Code { 5, 32, 2048, 0b11111 },
};

// Table B.8 – Standard Huffman table H
constexpr Array standard_huffman_table_H = {
    Code { 8, 3, -15, 0b11111100 },
    Code { 9, 1, -7, 0b111111100 },
    Code { 8, 1, -5, 0b11111101 },
    Code { 9, 0, -3, 0b111111101 },
    Code { 7, 0, -2, 0b1111100 },
    Code { 4, 0, -1, 0b1010 },
    Code { 2, 1, 0, 0b00 },
    Code { 5, 0, 2, 0b11010 },
    Code { 6, 0, 3, 0b111010 },
    Code { 3, 4, 4, 0b100 },
    Code { 6, 1, 20, 0b111011 },
    Code { 4, 4, 22, 0b1011 },
    Code { 4, 5, 38, 0b1100 },
    Code { 5, 6, 70, 0b11011 },
    Code { 5, 7, 134, 0b11100 },
    Code { 6, 7, 262, 0b111100 },
    Code { 7, 8, 390, 0b1111101 },
    Code { 6, 10, 646, 0b111101 },
    Code { 9 | Code::LowerRangeBit, 32, -16, 0b111111110 },
    Code { 9, 32, 1670, 0b111111111 },
    Code { 2, 0, OptionalNone {}, 0b01 },
};

// Table B.9 – Standard Huffman table I
constexpr Array standard_huffman_table_I = {
    Code { 8, 4, -31, 0b11111100 },
    Code { 9, 2, -15, 0b111111100 },
    Code { 8, 2, -11, 0b11111101 },
    Code { 9, 1, -7, 0b111111101 },
    Code { 7, 1, -5, 0b1111100 },
    Code { 4, 1, -3, 0b1010 },
    Code { 3, 1, -1, 0b010 },
    Code { 3, 1, 1, 0b011 },
    Code { 5, 1, 3, 0b11010 },
    Code { 6, 1, 5, 0b111010 },
    Code { 3, 5, 7, 0b100 },
    Code { 6, 2, 39, 0b111011 },
    Code { 4, 5, 43, 0b1011 },
    Code { 4, 6, 75, 0b1100 },
    Code { 5, 7, 139, 0b11011 },
    Code { 5, 8, 267, 0b11100 },
    Code { 6, 8, 523, 0b111100 },
    Code { 7, 9, 779, 0b1111101 },
    Code { 6, 11, 1291, 0b111101 },
    Code { 9 | Code::LowerRangeBit, 32, -32, 0b111111110 },
    Code { 9, 32, 3339, 0b111111111 },
    Code { 2, 0, OptionalNone {}, 0b00 },
};

// Table B.10 – Standard Huffman table J
constexpr Array standard_huffman_table_J = {
    Code { 7, 4, -21, 0b1111010 },
    Code { 8, 0, -5, 0b11111100 },
    Code { 7, 0, -4, 0b1111011 },
    Code { 5, 0, -3, 0b11000 },
    Code { 2, 2, -2, 0b00 },
    Code { 5, 0, 2, 0b11001 },
    Code { 6, 0, 3, 0b110110 },
    Code { 7, 0, 4, 0b1111100 },
    Code { 8, 0, 5, 0b11111101 },
    Code { 2, 6, 6, 0b01 },
    Code { 5, 5, 70, 0b11010 },
    Code { 6, 5, 102, 0b110111 },
    Code { 6, 6, 134, 0b111000 },
    Code { 6, 7, 198, 0b111001 },
    Code { 6, 8, 326, 0b111010 },
    Code { 6, 9, 582, 0b111011 },
    Code { 6, 10, 1094, 0b111100 },
    Code { 7, 11, 2118, 0b1111101 },
    Code { 8 | Code::LowerRangeBit, 32, -22, 0b11111110 },
    Code { 8, 32, 4166, 0b11111111 },
    Code { 2, 0, OptionalNone {}, 0b10 },
};

// Table B.11 – Standard Huffman table K
constexpr Array standard_huffman_table_K = {
    Code { 1, 0, 1, 0b0 },
    Code { 2, 1, 2, 0b10 },
    Code { 4, 0, 4, 0b1100 },
    Code { 4, 1, 5, 0b1101 },
    Code { 5, 1, 7, 0b11100 },
    Code { 5, 2, 9, 0b11101 },
    Code { 6, 2, 13, 0b111100 },
    Code { 7, 2, 17, 0b1111010 },
    Code { 7, 3, 21, 0b1111011 },
    Code { 7, 4, 29, 0b1111100 },
    Code { 7, 5, 45, 0b1111101 },
    Code { 7, 6, 77, 0b1111110 },
    Code { 7, 32, 141, 0b1111111 },
};

// Table B.12 – Standard Huffman table L
constexpr Array standard_huffman_table_L = {
    Code { 1, 0, 1, 0b0 },
    Code { 2, 0, 2, 0b10 },
    Code { 3, 1, 3, 0b110 },
    Code { 5, 0, 5, 0b11100 },
    Code { 5, 1, 6, 0b11101 },
    Code { 6, 1, 8, 0b111100 },
    Code { 7, 0, 10, 0b1111010 },
    Code { 7, 1, 11, 0b1111011 },
    Code { 7, 2, 13, 0b1111100 },
    Code { 7, 3, 17, 0b1111101 },
    Code { 7, 4, 25, 0b1111110 },
    Code { 8, 5, 41, 0b11111110 },
    Code { 8, 32, 73, 0b11111111 },
};

// Table B.13 – Standard Huffman table M
constexpr Array standard_huffman_table_M = {
    Code { 1, 0, 1, 0b0 },
    Code { 3, 0, 2, 0b100 },
    Code { 4, 0, 3, 0b1100 },
    Code { 5, 0, 4, 0b11100 },
    Code { 4, 1, 5, 0b1101 },
    Code { 3, 3, 7, 0b101 },
    Code { 6, 1, 15, 0b111010 },
    Code { 6, 2, 17, 0b111011 },
    Code { 6, 3, 21, 0b111100 },
    Code { 6, 4, 29, 0b111101 },
    Code { 6, 5, 45, 0b111110 },
    Code { 7, 6, 77, 0b1111110 },
    Code { 7, 32, 141, 0b1111111 },
};

// Table B.14 – Standard Huffman table N
constexpr Array standard_huffman_table_N = {
    Code { 3, 0, -2, 0b100 },
    Code { 3, 0, -1, 0b101 },
    Code { 1, 0, 0, 0b0 },
    Code { 3, 0, 1, 0b110 },
    Code { 3, 0, 2, 0b111 },
};

// Table B.15 – Standard Huffman table O
constexpr Array standard_huffman_table_O = {
    Code { 7, 4, -24, 0b1111100 },
    Code { 6, 2, -8, 0b111100 },
    Code { 5, 1, -4, 0b11100 },
    Code { 4, 0, -2, 0b1100 },
    Code { 3, 0, -1, 0b100 },
    Code { 1, 0, 0, 0b0 },
    Code { 3, 0, 1, 0b101 },
    Code { 4, 0, 2, 0b1101 },
    Code { 5, 1, 3, 0b11101 },
    Code { 6, 2, 5, 0b111101 },
    Code { 7, 4, 9, 0b1111101 },
    Code { 7 | Code::LowerRangeBit, 32, -25, 0b1111110 },
    Code { 7, 32, 25, 0b1111111 },
};

class HuffmanTable {
public:
    enum class StandardTable {
        B_1,  // Standard Huffman table A
        B_2,  // Standard Huffman table B
        B_3,  // Standard Huffman table C
        B_4,  // Standard Huffman table D
        B_5,  // Standard Huffman table E
        B_6,  // Standard Huffman table F
        B_7,  // Standard Huffman table G
        B_8,  // Standard Huffman table H
        B_9,  // Standard Huffman table I
        B_10, // Standard Huffman table J
        B_11, // Standard Huffman table K
        B_12, // Standard Huffman table L
        B_13, // Standard Huffman table M
        B_14, // Standard Huffman table N
        B_15, // Standard Huffman table O
    };
    static ErrorOr<HuffmanTable*> standard_huffman_table(StandardTable);

    bool has_oob_symbol() const { return m_has_oob_symbol; }

    // Returns OptionalNone for OOB.
    ErrorOr<Optional<i32>> read_symbol(BigEndianInputBitStream&) const;

    // Will never return OOB.
    ErrorOr<i32> read_symbol_non_oob(BigEndianInputBitStream&) const;

    HuffmanTable(ReadonlySpan<Code> codes, bool has_oob_symbol = false)
        : m_codes(codes)
        , m_has_oob_symbol(has_oob_symbol)
    {
    }

private:
    ErrorOr<Optional<i32>> read_symbol_internal(BigEndianInputBitStream&) const;

    ReadonlySpan<Code> m_codes;
    bool m_has_oob_symbol { false };
};

ErrorOr<HuffmanTable*> HuffmanTable::standard_huffman_table(StandardTable kind)
{
    switch (kind) {
    case StandardTable::B_1: {
        static HuffmanTable standard_table_A(standard_huffman_table_A);
        return &standard_table_A;
    }
    case StandardTable::B_2: {
        static HuffmanTable standard_table_B(standard_huffman_table_B, true);
        return &standard_table_B;
    }
    case StandardTable::B_3:
        // If you find a file using this, get the table from #26104.
        return Error::from_string_literal("Standard table C not yet supported");
    case StandardTable::B_4: {
        static HuffmanTable standard_table_D(standard_huffman_table_D);
        return &standard_table_D;
    }
    case StandardTable::B_5:
        // If you find a file using this, get the table from #26104.
        return Error::from_string_literal("Standard table E not yet supported");
    case StandardTable::B_6: {
        static HuffmanTable standard_table_F(standard_huffman_table_F);
        return &standard_table_F;
    }
    case StandardTable::B_7: {
        static HuffmanTable standard_table_G(standard_huffman_table_G);
        return &standard_table_G;
    }
    case StandardTable::B_8: {
        static HuffmanTable standard_table_H(standard_huffman_table_H, true);
        return &standard_table_H;
    }
    case StandardTable::B_9: {
        static HuffmanTable standard_table_I(standard_huffman_table_I, true);
        return &standard_table_I;
    }
    case StandardTable::B_10: {
        static HuffmanTable standard_table_J(standard_huffman_table_J, true);
        return &standard_table_J;
    }
    case StandardTable::B_11: {
        static HuffmanTable standard_table_K(standard_huffman_table_K);
        return &standard_table_K;
    }
    case StandardTable::B_12: {
        static HuffmanTable standard_table_L(standard_huffman_table_L);
        return &standard_table_L;
    }
    case StandardTable::B_13: {
        static HuffmanTable standard_table_M(standard_huffman_table_M);
        return &standard_table_M;
    }
    case StandardTable::B_14: {
        static HuffmanTable standard_table_N(standard_huffman_table_N);
        return &standard_table_N;
    }
    case StandardTable::B_15: {
        static HuffmanTable standard_table_O(standard_huffman_table_O);
        return &standard_table_O;
    }
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<Optional<i32>> HuffmanTable::read_symbol_internal(BigEndianInputBitStream& stream) const
{
    // FIXME: Use an approach that doesn't require a full scan for every bit. See Compress::CanonicalCodes.
    u32 code_word = 0;
    u8 code_size = 0;
    while (true) {
        code_word = (code_word << 1) | TRY(stream.read_bit());
        code_size++;
        for (auto const& code : m_codes) {
            if ((code.prefix_length & ~Code::LowerRangeBit) == code_size && code.code == code_word) {
                if (!code.first_value.has_value())
                    return code.first_value; // OOB

                i32 value = 0; // "HTOFFSET" in spec.
                for (u8 i = 0; i < code.range_length; ++i)
                    value = (value << 1) | TRY(stream.read_bit());

                if (code.prefix_length & Code::LowerRangeBit)
                    return code.first_value.value() - value;
                return value + code.first_value.value();
            }
        }
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<Optional<i32>> HuffmanTable::read_symbol(BigEndianInputBitStream& stream) const
{
    VERIFY(m_has_oob_symbol);
    return read_symbol_internal(stream);
}

ErrorOr<i32> HuffmanTable::read_symbol_non_oob(BigEndianInputBitStream& stream) const
{
    VERIFY(!m_has_oob_symbol);
    auto result = TRY(read_symbol_internal(stream));
    return result.value();
}

}

struct SegmentData {
    SegmentData(JBIG2::SegmentHeader header, ReadonlyBytes data)
        : header(header)
        , data(data)
    {
    }

    JBIG2::SegmentHeader header;
    ReadonlyBytes data;

    auto type() const { return header.type; }

    // Valid after complete_decoding_all_segment_headers().
    Vector<SegmentData*> referred_to_segments;

    // Set on dictionary segments after they've been decoded.
    Optional<Vector<BilevelSubImage>> symbols;

    // Set on pattern segments after they've been decoded.
    Optional<Vector<BilevelSubImage>> patterns;

    // Set on code table segments after they've been decoded.
    Optional<Vector<JBIG2::Code>> codes;
    Optional<JBIG2::HuffmanTable> huffman_table;

    // Set on intermediate region segments after they've been decoded.
    RefPtr<BilevelImage> aux_buffer;
    JBIG2::RegionSegmentInformationField aux_buffer_information_field;
};

struct Page {
    IntSize size;

    // This is never CombinationOperator::Replace for Pages.
    JBIG2::CombinationOperator default_combination_operator { JBIG2::CombinationOperator::Or };

    bool direct_region_segments_override_default_combination_operator { false };

    RefPtr<BilevelImage> bits;
};

struct JBIG2LoadingContext {
    enum class State {
        NotDecoded = 0,
        Error,
        Decoded,
    };
    State state { State::NotDecoded };

    JBIG2::Organization organization { JBIG2::Organization::Sequential };
    Page page;
    u32 current_page_number { 1 };

    Optional<u32> number_of_pages;
    Vector<u32> page_numbers;

    Vector<SegmentData> segments;

    // Files from the Power JBIG2 tests have a few quirks.
    // Since they're useful for coverage, detect these files and be more lenient.
    bool is_power_jbig2_file { false };
};

static ErrorOr<void> decode_jbig2_header(JBIG2LoadingContext& context, ReadonlyBytes data)
{
    if (!JBIG2ImageDecoderPlugin::sniff(data))
        return Error::from_string_literal("JBIG2LoadingContext: Invalid JBIG2 header");

    FixedMemoryStream stream(data.slice(sizeof(JBIG2::id_string)));

    // D.4.2 File header flags
    u8 header_flags = TRY(stream.read_value<u8>());
    if (header_flags & 0b11110000)
        return Error::from_string_literal("JBIG2LoadingContext: Invalid header flags");
    context.organization = (header_flags & 1) ? JBIG2::Organization::Sequential : JBIG2::Organization::RandomAccess;
    dbgln_if(JBIG2_DEBUG, "JBIG2 Header: Organization: {} ({})", (int)context.organization, context.organization == JBIG2::Organization::Sequential ? "Sequential" : "Random-access");
    bool has_known_number_of_pages = (header_flags & 2) ? false : true;
    bool uses_templates_with_12_AT_pixels = (header_flags & 4) ? true : false;
    bool contains_colored_region_segments = (header_flags & 8) ? true : false;

    dbgln_if(JBIG2_DEBUG, "    has_known_number_of_pages={}", has_known_number_of_pages);
    dbgln_if(JBIG2_DEBUG, "    uses_templates_with_12_AT_pixels={}", uses_templates_with_12_AT_pixels);
    dbgln_if(JBIG2_DEBUG, "    contains_colored_region_segments={}", contains_colored_region_segments);

    // D.4.3 Number of pages
    if (has_known_number_of_pages) {
        context.number_of_pages = TRY(stream.read_value<BigEndian<u32>>());
        dbgln_if(JBIG2_DEBUG, "    number of pages: {}", context.number_of_pages.value());
    }

    dbgln_if(JBIG2_DEBUG, "");

    return {};
}

static ErrorOr<JBIG2::SegmentType> to_segment_type(u8 type_int)
{
    auto type = static_cast<JBIG2::SegmentType>(type_int);
    switch (type) {
    case JBIG2::SegmentType::SymbolDictionary:
    case JBIG2::SegmentType::IntermediateTextRegion:
    case JBIG2::SegmentType::ImmediateTextRegion:
    case JBIG2::SegmentType::ImmediateLosslessTextRegion:
    case JBIG2::SegmentType::PatternDictionary:
    case JBIG2::SegmentType::IntermediateHalftoneRegion:
    case JBIG2::SegmentType::ImmediateHalftoneRegion:
    case JBIG2::SegmentType::ImmediateLosslessHalftoneRegion:
    case JBIG2::SegmentType::IntermediateGenericRegion:
    case JBIG2::SegmentType::ImmediateGenericRegion:
    case JBIG2::SegmentType::ImmediateLosslessGenericRegion:
    case JBIG2::SegmentType::IntermediateGenericRefinementRegion:
    case JBIG2::SegmentType::ImmediateGenericRefinementRegion:
    case JBIG2::SegmentType::ImmediateLosslessGenericRefinementRegion:
    case JBIG2::SegmentType::PageInformation:
    case JBIG2::SegmentType::EndOfPage:
    case JBIG2::SegmentType::EndOfStripe:
    case JBIG2::SegmentType::EndOfFile:
    case JBIG2::SegmentType::Profiles:
    case JBIG2::SegmentType::Tables:
    case JBIG2::SegmentType::ColorPalette:
    case JBIG2::SegmentType::Extension:
        return type;
    }
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid segment type");
}

static ErrorOr<JBIG2::SegmentHeader> decode_segment_header(SeekableStream& stream)
{
    // 7.2.2 Segment number
    u32 segment_number = TRY(stream.read_value<BigEndian<u32>>());
    dbgln_if(JBIG2_DEBUG, "Segment number: {}", segment_number);

    // 7.2.3 Segment header flags
    u8 flags = TRY(stream.read_value<u8>());
    JBIG2::SegmentType type = TRY(to_segment_type(flags & 0b11'1111));
    dbgln_if(JBIG2_DEBUG, "Segment type: {}", (int)type);
    bool segment_page_association_size_is_32_bits = (flags & 0b100'0000) != 0;
    bool segment_retained_only_by_itself_and_extension_segments = (flags & 0b1000'00000) != 0;

    dbgln_if(JBIG2_DEBUG, "Page association size is 32 bits: {}", segment_page_association_size_is_32_bits);
    dbgln_if(JBIG2_DEBUG, "Page retained only by itself and extension segments: {}", segment_retained_only_by_itself_and_extension_segments);

    // 7.2.4 Referred-to segment count and retention flags
    u8 referred_to_segment_count_and_retention_flags = TRY(stream.read_value<u8>());
    u32 count_of_referred_to_segments = referred_to_segment_count_and_retention_flags >> 5;
    if (count_of_referred_to_segments == 5 || count_of_referred_to_segments == 6)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid count_of_referred_to_segments");

    bool retention_flag = false;
    Vector<bool> referred_to_segment_retention_flags;
    if (count_of_referred_to_segments == 7) {
        TRY(stream.seek(-1, SeekMode::FromCurrentPosition));
        count_of_referred_to_segments = TRY(stream.read_value<BigEndian<u32>>()) & 0x1FFF'FFFF;

        LittleEndianInputBitStream bit_stream { MaybeOwned { stream } };
        u32 bit_count = ceil_div(count_of_referred_to_segments + 1, 8) * 8;
        retention_flag = TRY(bit_stream.read_bit());
        for (u32 i = 0; i < count_of_referred_to_segments; ++i)
            referred_to_segment_retention_flags.append(TRY(bit_stream.read_bit()));
        for (u32 i = count_of_referred_to_segments; i < bit_count; ++i) {
            if (TRY(bit_stream.read_bit()))
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid referred-to segment retention flag");
        }
    } else {
        retention_flag = referred_to_segment_count_and_retention_flags & 1;
        for (u32 i = 1; i < count_of_referred_to_segments + 1; ++i)
            referred_to_segment_retention_flags.append((referred_to_segment_count_and_retention_flags >> i) & 1);
        for (u32 i = count_of_referred_to_segments + 1; i < 5; ++i) {
            if ((referred_to_segment_count_and_retention_flags >> i) & 1)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid referred-to segment retention flag");
        }
    }
    dbgln_if(JBIG2_DEBUG, "Retained: {}", retention_flag);
    dbgln_if(JBIG2_DEBUG, "Referred-to segment count: {}", count_of_referred_to_segments);

    // 7.2.5 Referred-to segment numbers
    Vector<u32> referred_to_segment_numbers;
    for (u32 i = 0; i < count_of_referred_to_segments; ++i) {
        u32 referred_to_segment_number;
        if (segment_number <= 256)
            referred_to_segment_number = TRY(stream.read_value<u8>());
        else if (segment_number <= 65536)
            referred_to_segment_number = TRY(stream.read_value<BigEndian<u16>>());
        else
            referred_to_segment_number = TRY(stream.read_value<BigEndian<u32>>());

        // "If a segment refers to other segments, it must refer to only segments with lower segment numbers."
        if (referred_to_segment_number >= segment_number)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Referred-to segment number too large");

        referred_to_segment_numbers.append(referred_to_segment_number);
        dbgln_if(JBIG2_DEBUG, "Referred-to segment number: {}, retained {}", referred_to_segment_number, referred_to_segment_retention_flags[i]);
    }

    // 7.2.6 Segment page association
    u32 segment_page_association;
    if (segment_page_association_size_is_32_bits) {
        segment_page_association = TRY(stream.read_value<BigEndian<u32>>());
    } else {
        segment_page_association = TRY(stream.read_value<u8>());
    }
    dbgln_if(JBIG2_DEBUG, "Segment page association: {}", segment_page_association);

    // 7.2.7 Segment data length
    u32 data_length = TRY(stream.read_value<BigEndian<u32>>());
    dbgln_if(JBIG2_DEBUG, "Segment data length: {}", data_length);

    Optional<u32> opt_data_length;
    if (data_length != 0xffff'ffff)
        opt_data_length = data_length;
    else if (type != JBIG2::SegmentType::ImmediateGenericRegion)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Unknown data length only allowed for ImmediateGenericRegion");

    dbgln_if(JBIG2_DEBUG, "");

    return JBIG2::SegmentHeader { segment_number, type, retention_flag, move(referred_to_segment_numbers), move(referred_to_segment_retention_flags), segment_page_association, opt_data_length };
}

static ErrorOr<size_t> scan_for_immediate_generic_region_size(ReadonlyBytes data)
{
    // 7.2.7 Segment data length
    // "If the segment's type is "Immediate generic region", then the length field may contain the value 0xFFFFFFFF.
    //  This value is intended to mean that the length of the segment's data part is unknown at the time that the segment header is written (...).
    //  In this case, the true length of the segment's data part shall be determined through examination of the data:
    //  if the segment uses template-based arithmetic coding, then the segment's data part ends with the two-byte sequence 0xFF 0xAC followed by a four-byte row count.
    //  If the segment uses MMR coding, then the segment's data part ends with the two-byte sequence 0x00 0x00 followed by a four-byte row count.
    //  The form of encoding used by the segment may be determined by examining the eighteenth byte of its segment data part,
    //  and the end sequences can occur anywhere after that eighteenth byte."
    // 7.4.6.4 Decoding a generic region segment
    // "NOTE – The sequence 0x00 0x00 cannot occur within MMR-encoded data; the sequence 0xFF 0xAC can occur only at the end of arithmetically-coded data.
    //  Thus, those sequences cannot occur by chance in the data that is decoded to generate the contents of the generic region."
    dbgln_if(JBIG2_DEBUG, "(Unknown data length, computing it)");

    if (data.size() < 19 + sizeof(u32))
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Data too short to contain segment data header and end sequence");

    // Per 7.4.6.1 Generic region segment data header, this starts with the 17 bytes described in
    // 7.4.1 Region segment information field, followed the byte described in 7.4.6.2 Generic region segment flags.
    // That byte's lowest bit stores if the segment uses MMR.
    u8 flags = data[17];
    bool uses_mmr = (flags & 1) != 0;
    auto end_sequence = uses_mmr ? to_array<u8>({ 0x00, 0x00 }) : to_array<u8>({ 0xFF, 0xAC });
    u8 const* end = static_cast<u8 const*>(memmem(data.data() + 19, data.size() - 19 - sizeof(u32), end_sequence.data(), end_sequence.size()));
    if (!end)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Could not find end sequence in segment data");

    size_t size = end - data.data() + end_sequence.size() + sizeof(u32);
    dbgln_if(JBIG2_DEBUG, "(Computed size is {})", size);
    return size;
}

static void identify_power_jbig2_files(JBIG2LoadingContext& context)
{
    for (auto const& segment : context.segments) {
        auto signature_data_1 = "\x20\0\0\0"
                                "Source\0"
                                "Power JBIG-2 Encoder - The University of British Columba and Image Power Inc.\0"
                                "Version\0"
                                "1.0.0\0"
                                "\0"sv;
        auto signature_data_2 = "\x20\0\0\0"
                                "Source\0"
                                "Power JBIG-2 Encoder - The University of British Columbia and Image Power Inc.\0"
                                "Version\0"
                                "1.0.0\0"
                                "\0"sv;
        if (segment.type() == JBIG2::SegmentType::Extension && (segment.data == signature_data_1.bytes() || segment.data == signature_data_2.bytes())) {
            context.is_power_jbig2_file = true;
            return;
        }
    }
}

static ErrorOr<void> validate_segment_order(JBIG2LoadingContext const& context)
{
    // 7.1 General description
    // "In the sequential and random-access organizations (see D.1 and D.2), the segments must appear in the file in increasing order
    //  of their segment numbers. However, in the embedded organization (see D.3), this is not the case"
    // "NOTE – It is possible for there to be gaps in the segment numbering"
    if (context.organization == JBIG2::Organization::Embedded)
        return {};

    for (size_t i = 1; i < context.segments.size(); ++i)
        if (context.segments[i - 1].header.segment_number > context.segments[i].header.segment_number)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Segments out of order");

    return {};
}

static ErrorOr<void> validate_segment_header_retention_flags(JBIG2LoadingContext const& context)
{
    // "If the retain bit for this segment value is 0, then no segment may refer to this segment.
    //  If the retain bit for the first referred-to segment value is 0, then no segment after this one may refer to the first segment
    //  that this segment refers to (i.e., this segment is the last segment that refers to that other segment)"
    HashTable<int> dead_segments;

    for (auto const& segment : context.segments) {
        auto const& header = segment.header;

        if (header.retention_flag) {
            // Guaranteed because decode_segment_header() guarantees referred_to_segment_numbers are larger than segment_number.
            VERIFY(!dead_segments.contains(header.segment_number));
        } else {
            if (dead_segments.set(header.segment_number) != HashSetResult::InsertedNewEntry)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid segment retention flags");
        }

        for (auto const& [i, referred_to_segment_number] : enumerate(header.referred_to_segment_numbers)) {
            // Quirk: t89-halftone/*-stripe.jb2 have one PatternDictionary and then one ImmediateHalftoneRegion per stripe,
            // but each ImmediateHalftoneRegion (incorrectly?) sets the retention flag for the PatternDictionary to 0.
            if (dead_segments.contains(referred_to_segment_number) && !context.is_power_jbig2_file)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Segment refers to dead segment");

            auto const referred_to_segment_retention_flag = header.referred_to_segment_retention_flags[i];
            if (referred_to_segment_retention_flag) {
                if (dead_segments.contains(referred_to_segment_number))
                    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Segment retention flags tried to revive dead segment");
            } else {
                dead_segments.set(referred_to_segment_number);
            }
        }
    }

    // It is not true that all segments are marked as dead at the end of the file.

    return {};
}

static bool is_region_segment(JBIG2::SegmentType type)
{
    // 7.3 Segment types
    // "The segments of types "intermediate text region", "immediate text region", "immediate lossless text region",
    //  "intermediate halftone region", "immediate halftone region", "immediate lossless halftone region", "intermediate
    //  generic region", "immediate generic region" , "immediate lossless generic region", "intermediate generic refinement
    //  region", "immediate generic refinement region", and "immediate lossless generic refinement region" are collectively
    //  referred to as "region segments"."
    switch (type) {
    case JBIG2::SegmentType::IntermediateTextRegion:
    case JBIG2::SegmentType::ImmediateTextRegion:
    case JBIG2::SegmentType::ImmediateLosslessTextRegion:
    case JBIG2::SegmentType::IntermediateHalftoneRegion:
    case JBIG2::SegmentType::ImmediateHalftoneRegion:
    case JBIG2::SegmentType::ImmediateLosslessHalftoneRegion:
    case JBIG2::SegmentType::IntermediateGenericRegion:
    case JBIG2::SegmentType::ImmediateGenericRegion:
    case JBIG2::SegmentType::ImmediateLosslessGenericRegion:
    case JBIG2::SegmentType::IntermediateGenericRefinementRegion:
    case JBIG2::SegmentType::ImmediateGenericRefinementRegion:
    case JBIG2::SegmentType::ImmediateLosslessGenericRefinementRegion:
        return true;
    default:
        return false;
    }
}

static bool is_intermediate_region_segment(JBIG2::SegmentType type)
{
    switch (type) {
    case JBIG2::SegmentType::IntermediateTextRegion:
    case JBIG2::SegmentType::IntermediateHalftoneRegion:
    case JBIG2::SegmentType::IntermediateGenericRegion:
    case JBIG2::SegmentType::IntermediateGenericRefinementRegion:
        return true;
    default:
        return false;
    }
}

static ErrorOr<void> validate_segment_header_references(JBIG2LoadingContext const& context)
{
    // 7.3.1 Rules for segment references

    HashMap<u32, u32> intermediate_region_segment_references;
    for (auto const& segment : context.segments) {
        // "• An intermediate region segment may only be referred to by one other non-extension segment; it may be
        //    referred to by any number of extension segments."
        for (auto const* referred_to_segment : segment.referred_to_segments) {
            if (!is_intermediate_region_segment(referred_to_segment->type()) || segment.type() == JBIG2::SegmentType::Extension)
                continue;
            if (intermediate_region_segment_references.set(referred_to_segment->header.segment_number, segment.header.segment_number) != HashSetResult::InsertedNewEntry)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Intermediate region segment referred to by multiple non-extension segments");
        }

        // "• A segment of type "symbol dictionary" (type 0) may refer to any number of segments of type "symbol
        //    dictionary" and to up to four segments of type "tables"."
        if (segment.type() == JBIG2::SegmentType::SymbolDictionary) {
            u32 table_count = 0;
            for (auto const* referred_to_segment : segment.referred_to_segments) {
                if (!first_is_one_of(referred_to_segment->type(), JBIG2::SegmentType::SymbolDictionary, JBIG2::SegmentType::Tables))
                    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Symbol dictionary segment refers to invalid segment type");
                if (referred_to_segment->type() == JBIG2::SegmentType::Tables)
                    table_count++;
            }
            if (table_count > 4)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Symbol dictionary segment refers to too many tables segments");
        }

        // "• A segment of type "intermediate text region", "immediate text region" or "immediate lossless text
        //    region" (type 4, 6 or 7) may refer to any number of segments of type "symbol dictionary" and to up to
        //    eight segments of type "tables". Additionally, it may refer to any number of segments of type "colour
        //    palette segment", if it has COLEXTFLAG = 1 in its region segment flags."
        // Note: decode_region_segment_information_field() currently rejects COLEXTFLAG = 1, so that part is not implemented.
        if (first_is_one_of(segment.type(),
                JBIG2::SegmentType::IntermediateTextRegion,
                JBIG2::SegmentType::ImmediateTextRegion,
                JBIG2::SegmentType::ImmediateLosslessTextRegion)) {
            u32 table_count = 0;
            for (auto const* referred_to_segment : segment.referred_to_segments) {
                if (!first_is_one_of(referred_to_segment->type(), JBIG2::SegmentType::SymbolDictionary,
                        JBIG2::SegmentType::Tables))
                    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Text region segment refers to invalid segment type");
                if (referred_to_segment->type() == JBIG2::SegmentType::Tables)
                    table_count++;
            }
            if (table_count > 8)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Text region segment refers to too many tables segments");
        }

        // "• A segment of type "pattern dictionary" (type 16) must not refer to any other segment."
        if (segment.type() == JBIG2::SegmentType::PatternDictionary && !segment.header.referred_to_segment_numbers.is_empty())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Pattern dictionary segment refers to other segments");

        // "• A segment of type "intermediate halftone region", "immediate halftone region" or "immediate lossless
        //    halftone region" (type 20, 22 or 23) must refer to exactly one segment, and this segment must be of type
        //    "pattern dictionary"."
        if (first_is_one_of(segment.type(),
                JBIG2::SegmentType::IntermediateHalftoneRegion,
                JBIG2::SegmentType::ImmediateHalftoneRegion,
                JBIG2::SegmentType::ImmediateLosslessHalftoneRegion)) {
            if (segment.referred_to_segments.size() != 1)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Halftone region segment must refer to exactly one pattern dictionary segment");
            if (segment.referred_to_segments[0]->type() != JBIG2::SegmentType::PatternDictionary)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Halftone region segment refers to non-pattern dictionary segment");
        }

        // "• A segment of type "intermediate generic region", "immediate generic region" or "immediate lossless
        //    generic region" (type 36, 38 or 39) must not refer to any other segment. If it has COLEXTFLAG = 1 in
        //    its region segment flags, however, it may refer to any number of segments of the type "colour palette
        //   segment"."
        // Note: decode_region_segment_information_field() currently rejects COLEXTFLAG = 1, so that part is not implemented.
        if (first_is_one_of(segment.type(),
                JBIG2::SegmentType::IntermediateGenericRegion,
                JBIG2::SegmentType::ImmediateGenericRegion,
                JBIG2::SegmentType::ImmediateLosslessGenericRegion)
            && !segment.header.referred_to_segment_numbers.is_empty()) {
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Generic region segment refers to other segments");
        }

        // "• A segment of type "intermediate generic refinement region" (type 40) must refer to exactly one other
        //    segment. This other segment must be an intermediate region segment."
        if (segment.type() == JBIG2::SegmentType::IntermediateGenericRefinementRegion) {
            if (segment.referred_to_segments.size() != 1)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Intermediate generic refinement region must refer to exactly one segment");
            if (!is_intermediate_region_segment(segment.referred_to_segments[0]->type()))
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Intermediate generic refinement region does not refer to intermediate region segment");
        }

        // "• A segment of type "immediate generic refinement region" or "immediate lossless generic refinement
        //    region" (type 42 or 43) may refer to either zero other segments or exactly one other segment. If it refers
        //    to one other segment then that segment must be an intermediate region segment."
        if (first_is_one_of(segment.type(),
                JBIG2::SegmentType::ImmediateGenericRefinementRegion,
                JBIG2::SegmentType::ImmediateLosslessGenericRefinementRegion)) {
            if (segment.referred_to_segments.size() > 1)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Immediate generic refinement region must refer to zero or one segment");
            if (segment.referred_to_segments.size() == 1 && !is_intermediate_region_segment(segment.referred_to_segments[0]->type()))
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Immediate generic refinement region does not refer to intermediate region segment");
        }

        // "• A segment of type "page information" (type 48) must not refer to any other segments."
        if (segment.type() == JBIG2::SegmentType::PageInformation && !segment.header.referred_to_segment_numbers.is_empty())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Page information segment refers to other segments");

        // "• A segment of type "end of page" (type 49) must not refer to any other segments."
        if (segment.type() == JBIG2::SegmentType::EndOfPage && !segment.header.referred_to_segment_numbers.is_empty())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: End of page segment refers to other segments");

        // "• A segment of type "end of stripe" (type 50) must not refer to any other segments."
        if (segment.type() == JBIG2::SegmentType::EndOfStripe && !segment.header.referred_to_segment_numbers.is_empty())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: End of stripe segment refers to other segments");

        // "• A segment of type "end of file" (type 51) must not refer to any other segments."
        if (segment.type() == JBIG2::SegmentType::EndOfFile && !segment.header.referred_to_segment_numbers.is_empty())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: End of file segment refers to other segments");

        // "• A segment of type "profiles" (type 52) must not refer to any other segments."
        if (segment.type() == JBIG2::SegmentType::Profiles && !segment.header.referred_to_segment_numbers.is_empty())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Profiles segment refers to other segments");

        // "• A segment of type "tables" (type 53) must not refer to any other segments."
        if (segment.type() == JBIG2::SegmentType::Tables && !segment.header.referred_to_segment_numbers.is_empty())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Tables segment refers to other segments");

        // "• A segment of type "extension" (type 62) may refer to any number of segments of any type, unless the
        //    extension segment's type imposes some restriction."
        // Nothing to check.

        // "• A segment of type "colour palette" (type 54) must not refer to any other segments."
        if (segment.type() == JBIG2::SegmentType::ColorPalette && !segment.header.referred_to_segment_numbers.is_empty())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Colour palette segment refers to other segments");
    }

    return {};
}

static ErrorOr<void> validate_segment_header_page_associations(JBIG2LoadingContext const& context)
{
    // 7.3.2 Rules for page associations
    for (auto const& segment : context.segments) {
        // "Every region segment must be associated with some page (i.e., have a non-zero page association field). "Page
        //  information",  "end of page" and "end of stripe" segments must be associated with some page. "End of file" segments
        //  must not be associated with any page. Segments of other types may be associated with a page or not."
        if (is_region_segment(segment.type())
            || first_is_one_of(segment.type(), JBIG2::SegmentType::PageInformation, JBIG2::SegmentType::EndOfPage, JBIG2::SegmentType::EndOfStripe)) {
            if (segment.header.page_association == 0)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Region, page information, end of page or end of stripe segment with no page association");
        }
        // Quirk: `042_*.jb2`, `amb_*.jb2` in the Power JBIG2 test suite incorrectly (cf 7.3.2) associate EndOfFile with a page.
        if (segment.type() == JBIG2::SegmentType::EndOfFile && segment.header.page_association != 0 && !context.is_power_jbig2_file)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: End of file segment with page association");

        // "If a segment is not associated with any page, then it must not refer to any segment that is associated with any page."
        if (segment.header.page_association == 0) {
            for (auto const* referred_to_segment : segment.referred_to_segments) {
                if (referred_to_segment->header.page_association != 0)
                    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Segment not associated with a page refers to segment associated with a page");
            }
        }

        // "If a segment is associated with a page, then it may refer to segments that are not associated with any page, and to
        //  segments that are associated with the same page. It must not refer to any segment that is associated with a different
        //  page."
        if (segment.header.page_association != 0) {
            for (auto const* referred_to_segment : segment.referred_to_segments) {
                if (referred_to_segment->header.page_association != 0 && referred_to_segment->header.page_association != segment.header.page_association)
                    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Segment refers to segment associated with a different page");
            }
        }
    }

    return {};
}

static ErrorOr<void> decode_segment_headers(JBIG2LoadingContext& context, ReadonlyBytes data)
{
    FixedMemoryStream stream(data);

    Vector<ReadonlyBytes> segment_datas;
    auto store_and_skip_segment_data = [&](JBIG2::SegmentHeader const& segment_header) -> ErrorOr<void> {
        size_t start_offset = TRY(stream.tell());

        // 7.2.7 Segment data length
        // "If the segment's type is "Immediate generic region", then the length field may contain the value 0xFFFFFFFF."
        // It sounds like this is not even allowed for ImmediateLosslessGenericRegion.
        // It's used in 0000033.pdf pages 1-2, and 0000600.pdf pages 1-3 (only with ImmediateGenericRegion).
        if (!segment_header.data_length.has_value() && segment_header.type != JBIG2::SegmentType::ImmediateGenericRegion)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Segment data length must be known for non-ImmediateGenericRegion segments");

        u32 data_length = TRY(segment_header.data_length.try_value_or_lazy_evaluated([&]() {
            return scan_for_immediate_generic_region_size(data.slice(start_offset));
        }));

        if (start_offset + data_length > data.size()) {
            dbgln_if(JBIG2_DEBUG, "JBIG2ImageDecoderPlugin: start_offset={}, data_length={}, data.size()={}", start_offset, data_length, data.size());
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Segment data length exceeds file size");
        }
        ReadonlyBytes segment_data = data.slice(start_offset, data_length);
        segment_datas.append(segment_data);

        TRY(stream.seek(data_length, SeekMode::FromCurrentPosition));
        return {};
    };

    Vector<JBIG2::SegmentHeader> segment_headers;
    while (!stream.is_eof()) {
        auto segment_header = TRY(decode_segment_header(stream));
        segment_headers.append(segment_header);

        if (context.organization != JBIG2::Organization::RandomAccess)
            TRY(store_and_skip_segment_data(segment_header));

        // Required per spec for files with RandomAccess organization.
        if (segment_header.type == JBIG2::SegmentType::EndOfFile)
            break;
    }

    if (context.organization == JBIG2::Organization::RandomAccess) {
        for (auto const& segment_header : segment_headers)
            TRY(store_and_skip_segment_data(segment_header));
    }

    if (segment_headers.size() != segment_datas.size())
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Segment headers and segment datas have different sizes");

    for (size_t i = 0; i < segment_headers.size(); ++i)
        context.segments.append({ segment_headers[i], segment_datas[i] });

    return {};
}

static ErrorOr<void> complete_decoding_all_segment_headers(JBIG2LoadingContext& context)
{
    HashMap<u32, u32> segments_by_number;
    for (auto const& [i, segment] : enumerate(context.segments)) {
        if (segments_by_number.set(segment.header.segment_number, i) != HashSetResult::InsertedNewEntry)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Duplicate segment number");
    }

    for (auto& segment : context.segments) {
        for (auto referred_to_segment_number : segment.header.referred_to_segment_numbers) {
            auto opt_referred_to_segment = segments_by_number.get(referred_to_segment_number);
            if (!opt_referred_to_segment.has_value())
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Segment refers to non-existing segment");
            segment.referred_to_segments.append(&context.segments[opt_referred_to_segment.value()]);
        }
    }

    identify_power_jbig2_files(context);

    TRY(validate_segment_order(context));
    TRY(validate_segment_header_retention_flags(context));
    TRY(validate_segment_header_references(context));
    TRY(validate_segment_header_page_associations(context));

    return {};
}

static ErrorOr<JBIG2::RegionSegmentInformationField> decode_region_segment_information_field(ReadonlyBytes data)
{
    // 7.4.1 Region segment information field
    if (data.size() < sizeof(JBIG2::RegionSegmentInformationField))
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid region segment information field size");
    auto result = *(JBIG2::RegionSegmentInformationField const*)data.data();
    if ((result.flags & 0b1111'0000) != 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid region segment information field flags");
    if ((result.flags & 0x7) > 4)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid region segment information field operator");

    // NOTE 3 – If the colour extension flag (COLEXTFLAG) is equal to 1, the external combination operator must be REPLACE.
    if (result.is_color_bitmap() && result.external_combination_operator() != JBIG2::CombinationOperator::Replace)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid colored region segment information field operator");

    // FIXME: Support colors one day.
    // Update validate_segment_header_references() when allowing this.
    // Check that is_color_bitmap is only true if contains_colored_region_segments in the JBIG2 file header is set then.
    if (result.is_color_bitmap())
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: COLEXTFLAG=1 not yet implemented");

    return result;
}

static ErrorOr<JBIG2::PageInformationSegment> decode_page_information_segment(ReadonlyBytes data)
{
    // 7.4.8 Page information segment syntax
    if (data.size() != sizeof(JBIG2::PageInformationSegment))
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid page information segment size");
    return *(JBIG2::PageInformationSegment const*)data.data();
}

static ErrorOr<void> validate_segment_combination_operator_consistency(JBIG2LoadingContext& context, JBIG2::RegionSegmentInformationField const& information_field)
{
    // 7.4.8.5 Page segment flags
    // "NOTE 1 – All region segments, except for refinement region segments, are direct region segments. Because of the requirements
    //  in 7.4.7.5 restricting the external combination operators of refinement region segments, if this bit is 0, then refinement region
    //  segments associated with this page that refer to no region segments must have an external combination operator of REPLACE,
    //  and all other region segments associated with this page must have the external combination operator specified by this page's
    //  "Page default combination operator"."

    if (context.page.direct_region_segments_override_default_combination_operator)
        return {};

    if (information_field.external_combination_operator() != context.page.default_combination_operator)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Segment combination operator does not match page default combination operator, despite page information segment claiming it would");

    return {};
}

static ErrorOr<JBIG2::EndOfStripeSegment> decode_end_of_stripe_segment(ReadonlyBytes data)
{
    // 7.4.10 End of stripe segment syntax
    if (data.size() != sizeof(JBIG2::EndOfStripeSegment))
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: End of strip segment has wrong size");
    return *(JBIG2::EndOfStripeSegment const*)data.data();
}

static ErrorOr<void> scan_for_page_size(JBIG2LoadingContext& context)
{
    // This implements just enough of "8.2 Page image composition" to figure out the size of the current page.
    // The spec describes a slightly more complicated approach to make streaming work,
    // but we require all input data to be available anyway, so can just scan through all EndOfStripe segments.

    size_t page_info_count = 0;
    bool has_initially_unknown_height = false;
    bool found_end_of_page = false;
    bool page_is_striped = false;
    u16 max_stripe_height = 0;
    Optional<int> height_at_end_of_last_stripe;
    Optional<size_t> last_end_of_stripe_index;
    Optional<size_t> last_not_end_of_page_segment_index;
    for (auto const& [segment_index, segment] : enumerate(context.segments)) {
        if (segment.header.page_association != context.current_page_number)
            continue;

        // Quirk: `042_*.jb2`, `amb_*.jb2` in the Power JBIG2 test suite incorrectly (cf 7.3.2) associate EndOfFile with a page.
        if (segment.type() == JBIG2::SegmentType::EndOfFile && context.is_power_jbig2_file)
            continue;

        if (found_end_of_page)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Found segment after EndOfPage");

        if (segment.type() != JBIG2::SegmentType::EndOfPage)
            last_not_end_of_page_segment_index = segment_index;

        if (segment.type() == JBIG2::SegmentType::PageInformation) {
            if (++page_info_count > 1)
                return Error::from_string_literal("JBIG2: Multiple PageInformation segments");

            auto page_information = TRY(decode_page_information_segment(segment.data));

            // 7.4.8.6 Page striping information
            // "the maximum size of each stripe (the distance between an end of stripe segment's end row and the end row of the previous
            //  end of stripe segment, or 0 in the case of the first end of stripe segment) must be no more than the page's maximum
            //  stripe size."
            // This means that the first stripe can be one taller than maximum_stripe_size, but all subsequent stripes must not be.
            // FIXME: Be stricter about subsequent stripes.
            page_is_striped = page_information.page_is_striped();
            max_stripe_height = page_information.maximum_stripe_size() + 1;

            context.page.size = { page_information.bitmap_width, page_information.bitmap_height };
            has_initially_unknown_height = page_information.bitmap_height == 0xffff'ffff;

            // "If the page's bitmap height is unknown (indicated by a page bitmap height of 0xFFFFFFFF) then the "page is striped"
            //  bit must be 1."
            if (has_initially_unknown_height && !page_information.page_is_striped())
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Non-striped bitmaps of indeterminate height not allowed");
        } else if (segment.type() == JBIG2::SegmentType::EndOfStripe) {
            if (page_info_count == 0)
                return Error::from_string_literal("JBIG2: EndOfStripe before PageInformation");
            if (!page_is_striped)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Found EndOfStripe for non-striped page");

            // 7.4.10 End of stripe segment syntax
            // "An end of stripe segment states that the encoder has finished coding a portion of the page with which the segment is
            //  associated, and will not revisit it. It specifies the Y coordinate of a row of the page; no segment following the end of
            //  stripe may modify any portion of the page bitmap that lines on or above that row; furthermore, no segment preceding
            //  the end of stripe may modify any portion of the page bitmap that lies below that row. This row is called the "end row"
            //  of the stripe."
            auto end_of_stripe = TRY(decode_end_of_stripe_segment(segment.data));
            int new_height = end_of_stripe.y_coordinate + 1;

            if (has_initially_unknown_height) {
                if (height_at_end_of_last_stripe.has_value() && new_height < height_at_end_of_last_stripe.value())
                    return Error::from_string_literal("JBIG2ImageDecoderPlugin: EndOfStripe Y coordinate is not increasing");
                context.page.size.set_height(new_height);
            } else if (new_height > context.page.size.height()) {
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: EndOfStripe Y coordinate larger than page height");
            }

            // "The end row specified by an end of stripe segment must lie below any previous end row for that page."
            int stripe_height = new_height - height_at_end_of_last_stripe.value_or(0);
            if (stripe_height <= 0)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: EndOfStripe Y coordinate is not increasing");

            dbgln_if(JBIG2_DEBUG, "stripe_height={}, max_stripe_height={}", stripe_height, max_stripe_height);
            if (stripe_height > max_stripe_height)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: EndOfStripe Y coordinate larger than maximum stripe height");

            height_at_end_of_last_stripe = new_height;
            last_end_of_stripe_index = segment_index;
        } else if (segment.type() == JBIG2::SegmentType::EndOfPage) {
            if (segment.data.size() != 0)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: End of page segment has non-zero size");
            found_end_of_page = true;
        }
    }

    if (page_info_count == 0)
        return Error::from_string_literal("JBIG2: Missing PageInformation segment");

    if (page_is_striped) {
        if (has_initially_unknown_height) {
            // "A page whose height was originally unknown must contain at least one end of stripe segment."
            if (!height_at_end_of_last_stripe.has_value())
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Striped page of initially unknown height without EndOfStripe segment");

            if (last_end_of_stripe_index.value() != last_not_end_of_page_segment_index.value())
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Page not ended by end of stripe segment on striped page with initially unknown height");
            context.page.size.set_height(height_at_end_of_last_stripe.value());
        }

        // `!=` is not true, e.g. in ignition.pdf the last stripe is shorter than the page height.
        if (!has_initially_unknown_height && height_at_end_of_last_stripe.has_value() && height_at_end_of_last_stripe.value() > context.page.size.height())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Stripes are higher than page height");
    }

    if (context.organization == JBIG2::Organization::Embedded) {
        // PDF 1.7 spec, 3.3.6 JBIG2Decode Filter
        // "The JBIG2 file header, end-of-page segments, and end-of-file segment are not
        //  used in PDF. These should be removed before the PDF objects described below
        //  are created."
        if (found_end_of_page)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Unexpected EndOfPage segment in embedded stream");
    } else {
        // 7.4.9 End of page segment syntax
        // "Each page must have exactly one end of page segment associated with it."
        if (!found_end_of_page)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Missing EndOfPage segment");
    }

    return {};
}

static ErrorOr<void> scan_for_page_numbers(JBIG2LoadingContext& context)
{
    HashTable<u32> seen_pages;
    Vector<u32> pages;

    for (auto const& segment : context.segments) {
        if (segment.header.page_association == 0)
            continue;
        if (seen_pages.contains(segment.header.page_association))
            continue;
        seen_pages.set(segment.header.page_association);
        pages.append(segment.header.page_association);
    }

    if (context.number_of_pages.has_value() && context.number_of_pages.value() != pages.size())
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Number of pages in file header does not match number of pages found in segments");

    context.page_numbers = move(pages);
    return {};
}

// 6.2.2 Input parameters
// Table 2 – Parameters for the generic region decoding procedure
struct GenericRegionDecodingInputParameters {
    bool is_modified_modified_read { false }; // "MMR" in spec.
    u32 region_width { 0 };                   // "GBW" in spec.
    u32 region_height { 0 };                  // "GBH" in spec.
    u8 gb_template { 0 };
    bool is_typical_prediction_used { false };          // "TPGDON" in spec.
    bool is_extended_reference_template_used { false }; // "EXTTEMPLATE" in spec.
    Optional<BilevelImage const&> skip_pattern;         // "USESKIP", "SKIP" in spec.

    Array<JBIG2::AdaptiveTemplatePixel, 12> adaptive_template_pixels; // "GBATX" / "GBATY" in spec.
    // FIXME: GBCOLS, GBCOMBOP, COLEXTFLAG

    enum RequireEOFBAfterMMR {
        No,
        Yes,
    } require_eof_after_mmr { RequireEOFBAfterMMR::No };

    // If is_modified_modified_read is true, generic_region_decoding_procedure() reads data off this stream.
    Stream* stream { nullptr };

    // If is_modified_modified_read is false, generic_region_decoding_procedure() reads data off this decoder.
    MQArithmeticDecoder* arithmetic_decoder { nullptr };
};

// 6.2 Generic region decoding procedure
static ErrorOr<NonnullRefPtr<BilevelImage>> generic_region_decoding_procedure(GenericRegionDecodingInputParameters const& inputs, Optional<JBIG2::GenericContexts>& maybe_contexts)
{
    if (inputs.is_modified_modified_read) {
        dbgln_if(JBIG2_DEBUG, "JBIG2ImageDecoderPlugin: MMR image data");

        // 6.2.6 Decoding using MMR coding
        // "If the number of bytes contained in the encoded bitmap is known in advance, then it is permissible for the data
        //  stream not to contain an EOFB (000000000001000000000001) at the end of the MMR-encoded data."
        CCITT::Group4Options options;
        if (inputs.require_eof_after_mmr == GenericRegionDecodingInputParameters::RequireEOFBAfterMMR::Yes)
            options.has_end_of_block = CCITT::Group4Options::HasEndOfBlock::Yes;

        // "An invocation of the generic region decoding procedure with MMR equal to 1 shall consume an integral number of
        //  bytes, beginning and ending on a byte boundary."
        // This means we can pass in a stream to CCITT::decode_ccitt_group4() and that can use a bit stream internally.
        auto buffer = TRY(CCITT::decode_ccitt_group4(*inputs.stream, inputs.region_width, inputs.region_height, options));

        size_t bytes_per_row = ceil_div(inputs.region_width, 8);
        if (buffer.size() != bytes_per_row * inputs.region_height)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Decoded MMR data has wrong size");

        auto result = TRY(BilevelImage::create_from_byte_buffer(move(buffer), inputs.region_width, inputs.region_height));
        return result;
    }

    auto& contexts = maybe_contexts.value();

    // 6.2.5 Decoding using a template and arithmetic coding
    if (inputs.is_extended_reference_template_used)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode EXTTEMPLATE yet");

    int number_of_adaptive_template_pixels = inputs.gb_template == 0 ? 4 : 1;
    for (int i = 0; i < number_of_adaptive_template_pixels; ++i)
        TRY(check_valid_adaptive_template_pixel(inputs.adaptive_template_pixels[i]));

    if (inputs.skip_pattern.has_value() && (inputs.skip_pattern->width() != inputs.region_width || inputs.skip_pattern->height() != inputs.region_height))
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid USESKIP dimensions");

    static constexpr auto get_pixel = [](NonnullRefPtr<BilevelImage> const& buffer, int x, int y) -> bool {
        // 6.2.5.2 Coding order and edge conventions
        // "• All pixels lying outside the bounds of the actual bitmap have the value 0."
        // We don't have to check y >= buffer->height() because check_valid_adaptive_template_pixel() rejects y > 0.
        if (x < 0 || x >= (int)buffer->width() || y < 0)
            return false;
        return buffer->get_bit(x, y);
    };

    static constexpr auto get_pixels = [](NonnullRefPtr<BilevelImage> const& buffer, int x, int y, u8 width) -> u8 {
        if (x + width < 0 || x >= (int)buffer->width() || y < 0)
            return 0;
        auto corrected_x = max(x, 0);
        auto right_end = x + width;
        auto corrected_right_end = min(right_end, buffer->width());
        auto in_bounds = corrected_right_end - corrected_x;
        auto res = buffer->get_bits(corrected_x, y, in_bounds);
        res <<= (right_end - corrected_right_end);
        return res;
    };

    // Figure 3(a) – Template when GBTEMPLATE = 0 and EXTTEMPLATE = 0,
    constexpr auto compute_context_0 = [](NonnullRefPtr<BilevelImage> const& buffer, ReadonlySpan<JBIG2::AdaptiveTemplatePixel> adaptive_pixels, int x, int y) -> u16 {
        u16 result = 0;
        for (int i = 0; i < 4; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x + adaptive_pixels[i].x, y + adaptive_pixels[i].y);
        result = (result << 3) | get_pixels(buffer, x - 1, y - 2, 3);
        result = (result << 5) | get_pixels(buffer, x - 2, y - 1, 5);
        result = (result << 4) | get_pixels(buffer, x - 4, y, 4);
        return result;
    };

    // Figure 4 – Template when GBTEMPLATE = 1
    auto compute_context_1 = [](NonnullRefPtr<BilevelImage> const& buffer, ReadonlySpan<JBIG2::AdaptiveTemplatePixel> adaptive_pixels, int x, int y) -> u16 {
        u16 result = 0;
        result = (result << 1) | (u16)get_pixel(buffer, x + adaptive_pixels[0].x, y + adaptive_pixels[0].y);
        result = (result << 4) | get_pixels(buffer, x - 1, y - 2, 4);
        result = (result << 5) | get_pixels(buffer, x - 2, y - 1, 5);
        result = (result << 3) | get_pixels(buffer, x - 3, y, 3);
        return result;
    };

    // Figure 5 – Template when GBTEMPLATE = 2
    auto compute_context_2 = [](NonnullRefPtr<BilevelImage> const& buffer, ReadonlySpan<JBIG2::AdaptiveTemplatePixel> adaptive_pixels, int x, int y) -> u16 {
        u16 result = 0;
        result = (result << 1) | (u16)get_pixel(buffer, x + adaptive_pixels[0].x, y + adaptive_pixels[0].y);
        result = (result << 3) | get_pixels(buffer, x - 1, y - 2, 3);
        result = (result << 4) | get_pixels(buffer, x - 2, y - 1, 4);
        result = (result << 2) | get_pixels(buffer, x - 2, y, 2);
        return result;
    };

    // Figure 6 – Template when GBTEMPLATE = 3
    auto compute_context_3 = [](NonnullRefPtr<BilevelImage> const& buffer, ReadonlySpan<JBIG2::AdaptiveTemplatePixel> adaptive_pixels, int x, int y) -> u16 {
        u16 result = 0;
        result = (result << 1) | (u16)get_pixel(buffer, x + adaptive_pixels[0].x, y + adaptive_pixels[0].y);
        result = (result << 5) | get_pixels(buffer, x - 3, y - 1, 5);
        result = (result << 4) | get_pixels(buffer, x - 4, y, 4);
        return result;
    };

    u16 (*compute_context)(NonnullRefPtr<BilevelImage> const&, ReadonlySpan<JBIG2::AdaptiveTemplatePixel>, int, int);
    if (inputs.gb_template == 0)
        compute_context = compute_context_0;
    else if (inputs.gb_template == 1)
        compute_context = compute_context_1;
    else if (inputs.gb_template == 2)
        compute_context = compute_context_2;
    else {
        VERIFY(inputs.gb_template == 3);
        compute_context = compute_context_3;
    }

    // "The values of the pixels in this neighbourhood define a context. Each context has its own adaptive probability estimate
    //  used by the arithmetic coder (see Annex E)."
    // "* Decode the current pixel by invoking the arithmetic entropy decoding procedure, with CX set to the value formed by
    //    concatenating the label "GB" and the 10-16 pixel values gathered in CONTEXT."
    // Implementor's note: What this is supposed to mean is that we have a bunch of independent contexts, and we pick the
    // context for the current pixel based on pixel values in the neighborhood. The "GB" part just means this context is
    // independent from other contexts in the spec. They are passed in to this function.

    // Figure 8 – Reused context for coding the SLTP value when GBTEMPLATE is 0
    constexpr u16 sltp_context_for_template_0 = 0b10011'0110010'0101;

    // Figure 9 – Reused context for coding the SLTP value when GBTEMPLATE is 1
    constexpr u16 sltp_context_for_template_1 = 0b0011'110010'101;

    // Figure 10 – Reused context for coding the SLTP value when GBTEMPLATE is 2
    constexpr u16 sltp_context_for_template_2 = 0b001'11001'01;

    // Figure 11 – Reused context for coding the SLTP value when GBTEMPLATE is 3
    constexpr u16 sltp_context_for_template_3 = 0b011001'0101;

    u16 sltp_context = [](u8 gb_template) {
        if (gb_template == 0)
            return sltp_context_for_template_0;
        if (gb_template == 1)
            return sltp_context_for_template_1;
        if (gb_template == 2)
            return sltp_context_for_template_2;
        VERIFY(gb_template == 3);
        return sltp_context_for_template_3;
    }(inputs.gb_template);

    // 6.2.5.7 Decoding the bitmap
    MQArithmeticDecoder& decoder = *inputs.arithmetic_decoder;

    // "1) Set:
    //         LTP = 0"
    bool ltp = false; // "Line (uses) Typical Prediction" maybe?

    // " 2) Create a bitmap GBREG of width GBW and height GBH pixels."
    auto result = TRY(BilevelImage::create(inputs.region_width, inputs.region_height));

    // "3) Decode each row as follows:"
    for (size_t y = 0; y < inputs.region_height; ++y) {
        // "a) If all GBH rows have been decoded then the decoding is complete; proceed to step 4)."
        // "b) If TPGDON is 1, then decode a bit using the arithmetic entropy coder..."
        if (inputs.is_typical_prediction_used) {
            // "SLTP" in spec. "Swap LTP" or "Switch LTP" maybe?
            bool sltp = decoder.get_next_bit(contexts.contexts[sltp_context]);
            ltp = ltp ^ sltp;

            // "c) If LTP = 1 then set every pixel of the current row of GBREG equal to the corresponding pixel of the row
            //     immediately above."
            if (ltp) {
                for (size_t x = 0; x < inputs.region_width; ++x)
                    result->set_bit(x, y, get_pixel(result, (int)x, (int)y - 1));
                continue;
            }
        }

        // "d) If LTP = 0 then, from left to right, decode each pixel of the current row of GBREG. The procedure for each
        //     pixel is as follows:"
        for (size_t x = 0; x < inputs.region_width; ++x) {
            // "i) If USESKIP is 1 and the pixel in the bitmap SKIP at the location corresponding to the current pixel is 1,
            //     then set the current pixel to 0."
            if (inputs.skip_pattern.has_value() && inputs.skip_pattern->get_bit(x, y)) {
                result->set_bit(x, y, false);
                continue;
            }

            // "ii) Otherwise:"
            u16 context = compute_context(result, inputs.adaptive_template_pixels, x, y);
            bool bit = decoder.get_next_bit(contexts.contexts[context]);
            result->set_bit(x, y, bit);
        }
    }

    // "4) After all the rows have been decoded, the current contents of the bitmap GBREG are the results that shall be
    //     obtained by every decoder, whether it performs this exact sequence of steps or not."
    return result;
}

// 6.3.2 Input parameters
// Table 6 – Parameters for the generic refinement region decoding procedure
struct GenericRefinementRegionDecodingInputParameters {
    u32 region_width { 0 };                                          // "GRW" in spec.
    u32 region_height { 0 };                                         // "GRH" in spec.
    u8 gr_template { 0 };                                            // "GRTEMPLATE" in spec.
    BilevelSubImage const* reference_bitmap { nullptr };             // "GRREFERENCE" in spec.
    i32 reference_x_offset { 0 };                                    // "GRREFERENCEDX" in spec.
    i32 reference_y_offset { 0 };                                    // "GRREFERENCEDY" in spec.
    bool is_typical_prediction_used { false };                       // "TPGRON" in spec.
    Array<JBIG2::AdaptiveTemplatePixel, 2> adaptive_template_pixels; // "GRATX" / "GRATY" in spec.
};

struct RefinementContexts {
    explicit RefinementContexts(u8 refinement_template)
    {
        contexts.resize(1 << (refinement_template == 0 ? 13 : 10));
    }

    Vector<MQArithmeticCoderContext> contexts; // "GR" (+ binary suffix) in spec.
};

// 6.3 Generic Refinement Region Decoding Procedure
static ErrorOr<NonnullRefPtr<BilevelImage>> generic_refinement_region_decoding_procedure(GenericRefinementRegionDecodingInputParameters& inputs, MQArithmeticDecoder& decoder, RefinementContexts& contexts)
{
    VERIFY(inputs.gr_template == 0 || inputs.gr_template == 1);

    if (inputs.gr_template == 0) {
        TRY(check_valid_adaptive_template_pixel(inputs.adaptive_template_pixels[0]));
        // inputs.adaptive_template_pixels[1] is allowed to contain any value.
    }
    // GRTEMPLATE 1 never uses adaptive pixels.

    // 6.3.5.3 Fixed templates and adaptive templates
    static constexpr auto get_pixel = [](auto const& buffer, int x, int y) -> bool {
        if (x < 0 || x >= (int)buffer.width() || y < 0 || y >= (int)buffer.height())
            return false;
        return buffer.get_bit(x, y);
    };

    // Figure 12 – 13-pixel refinement template showing the AT pixels at their nominal locations
    constexpr auto compute_context_0 = [](ReadonlySpan<JBIG2::AdaptiveTemplatePixel> adaptive_pixels, BilevelSubImage const& reference, int reference_x, int reference_y, BilevelImage const& buffer, int x, int y) -> u16 {
        u16 result = 0;

        for (int dy = -1; dy <= 1; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                if (dy == -1 && dx == -1)
                    result = (result << 1) | (u16)get_pixel(reference, reference_x + adaptive_pixels[1].x, reference_y + adaptive_pixels[1].y);
                else
                    result = (result << 1) | (u16)get_pixel(reference, reference_x + dx, reference_y + dy);
            }
        }

        result = (result << 1) | (u16)get_pixel(buffer, x + adaptive_pixels[0].x, y + adaptive_pixels[0].y);
        for (int i = 0; i < 2; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x + i, y - 1);
        result = (result << 1) | (u16)get_pixel(buffer, x - 1, y);

        return result;
    };

    // Figure 13 – 10-pixel refinement template
    constexpr auto compute_context_1 = [](ReadonlySpan<JBIG2::AdaptiveTemplatePixel>, BilevelSubImage const& reference, int reference_x, int reference_y, BilevelImage const& buffer, int x, int y) -> u16 {
        u16 result = 0;

        for (int dy = -1; dy <= 1; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                if ((dy == -1 && (dx == -1 || dx == 1)) || (dy == 1 && dx == -1))
                    continue;
                result = (result << 1) | (u16)get_pixel(reference, reference_x + dx, reference_y + dy);
            }
        }

        for (int i = 0; i < 3; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x - 1 + i, y - 1);
        result = (result << 1) | (u16)get_pixel(buffer, x - 1, y);

        return result;
    };

    auto compute_context = inputs.gr_template == 0 ? compute_context_0 : compute_context_1;

    // Figure 14 – Reused context for coding the SLTP value when GRTEMPLATE is 0
    constexpr u16 sltp_context_for_template_0 = 0b000'010'000'000'0;

    // Figure 15 – Reused context for coding the SLTP value when GRTEMPLATE is 1
    constexpr u16 sltp_context_for_template_1 = 0b0'010'00'000'0;

    u16 const sltp_context = inputs.gr_template == 0 ? sltp_context_for_template_0 : sltp_context_for_template_1;

    // 6.3.5.6 Decoding the refinement bitmap

    // "1) Set LTP = 0."
    bool ltp = false; // "Line (uses) Typical Prediction" maybe?

    // "2) Create a bitmap GRREG of width GRW and height GRH pixels."
    auto result = TRY(BilevelImage::create(inputs.region_width, inputs.region_height));

    // "3) Decode each row as follows:"
    for (size_t y = 0; y < result->height(); ++y) {
        // "a) If all GRH rows have been decoded, then the decoding is complete; proceed to step 4)."
        // "b) If TPGRON is 1, then decode a bit using the arithmetic entropy coder..."
        if (inputs.is_typical_prediction_used) {
            // "SLTP" in spec. "Swap LTP" or "Switch LTP" maybe?
            bool sltp = decoder.get_next_bit(contexts.contexts[sltp_context]);
            ltp = ltp ^ sltp;
        }

        if (!ltp) {
            // "c) If LTP = 0 then, from left to right, explicitly decode all pixels of the current row of GRREG. The
            //     procedure for each pixel is as follows:"
            for (size_t x = 0; x < result->width(); ++x) {
                u16 context = compute_context(inputs.adaptive_template_pixels, *inputs.reference_bitmap, x - inputs.reference_x_offset, y - inputs.reference_y_offset, *result, x, y);
                bool bit = decoder.get_next_bit(contexts.contexts[context]);
                result->set_bit(x, y, bit);
            }
        } else {
            // "d) If LTP = 1 then, from left to right, implicitly decode certain pixels of the current row of GRREG,
            //     and explicitly decode the rest. The procedure for each pixel is as follows:"
            for (size_t x = 0; x < result->width(); ++x) {
                // "TPGRPIX", "TPGRVAL" in spec.
                auto prediction = [&](size_t x, size_t y) -> Optional<bool> {
                    // "• a 3 × 3 pixel array in the reference bitmap (Figure 16), centred at the location
                    //    corresponding to the current pixel, contains pixels all of the same value."
                    bool prediction = get_pixel(*inputs.reference_bitmap, x - inputs.reference_x_offset - 1, y - inputs.reference_y_offset - 1);
                    for (int dy = -1; dy <= 1; ++dy)
                        for (int dx = -1; dx <= 1; ++dx)
                            if (get_pixel(*inputs.reference_bitmap, x - inputs.reference_x_offset + dx, y - inputs.reference_y_offset + dy) != prediction)
                                return {};
                    return prediction;
                }(x, y);

                // TPGRON must be 1 if LTP is set. (The spec has an explicit "TPGRON is 1 AND" check here, but it is pointless.)
                VERIFY(inputs.is_typical_prediction_used);
                if (prediction.has_value()) {
                    result->set_bit(x, y, prediction.value());
                } else {
                    u16 context = compute_context(inputs.adaptive_template_pixels, *inputs.reference_bitmap, x - inputs.reference_x_offset, y - inputs.reference_y_offset, *result, x, y);
                    bool bit = decoder.get_next_bit(contexts.contexts[context]);
                    result->set_bit(x, y, bit);
                }
            }
        }
    }

    return result;
}

static constexpr BilevelImage::CompositionType to_composition_type(JBIG2::CombinationOperator operator_)
{
    switch (operator_) {
    case JBIG2::CombinationOperator::Or:
        return BilevelImage::CompositionType::Or;
    case JBIG2::CombinationOperator::And:
        return BilevelImage::CompositionType::And;
    case JBIG2::CombinationOperator::Xor:
        return BilevelImage::CompositionType::Xor;
    case JBIG2::CombinationOperator::XNor:
        return BilevelImage::CompositionType::XNor;
    case JBIG2::CombinationOperator::Replace:
        return BilevelImage::CompositionType::Replace;
    }
    VERIFY_NOT_REACHED();
}

// 6.4.2 Input parameters
// Table 9 – Parameters for the text region decoding procedure
struct TextRegionDecodingInputParameters {
    bool uses_huffman_encoding { false };     // "SBHUFF" in spec.
    bool uses_refinement_coding { false };    // "SBREFINE" in spec.
    u32 region_width { 0 };                   // "SBW" in spec.
    u32 region_height { 0 };                  // "SBH" in spec.
    u32 number_of_instances { 0 };            // "SBNUMINSTANCES" in spec.
    u32 size_of_symbol_instance_strips { 0 }; // "SBSTRIPS" in spec.
    // "SBNUMSYMS" is `symbols.size()` below.

    // Only set if uses_huffman_encoding is true.
    JBIG2::HuffmanTable const* symbol_id_table { nullptr }; // "SBSYMCODES" in spec.

    u32 id_symbol_code_length { 0 }; // "SBSYMCODELEN" in spec.
    Vector<BilevelSubImage> symbols; // "SBNUMSYMS" / "SBSYMS" in spec.
    u8 default_pixel { 0 };          // "SBDEFPIXEL" in spec.

    JBIG2::CombinationOperator operator_ { JBIG2::CombinationOperator::Or }; // "SBCOMBOP" in spec.

    bool is_transposed { false }; // "TRANSPOSED" in spec.

    JBIG2::ReferenceCorner reference_corner { JBIG2::ReferenceCorner::TopLeft }; // "REFCORNER" in spec.

    i8 delta_s_offset { 0 }; // "SBDSOFFSET" in spec.

    // Only set if uses_huffman_encoding is true.
    JBIG2::HuffmanTable const* first_s_table { nullptr };                 // "SBHUFFFS" in spec.
    JBIG2::HuffmanTable const* subsequent_s_table { nullptr };            // "SBHUFFDS" in spec.
    JBIG2::HuffmanTable const* delta_t_table { nullptr };                 // "SBHUFFDT" in spec.
    JBIG2::HuffmanTable const* refinement_delta_width_table { nullptr };  // "SBHUFFRDW" in spec.
    JBIG2::HuffmanTable const* refinement_delta_height_table { nullptr }; // "SBHUFFRDH" in spec.
    JBIG2::HuffmanTable const* refinement_x_offset_table { nullptr };     // "SBHUFFRDX" in spec.
    JBIG2::HuffmanTable const* refinement_y_offset_table { nullptr };     // "SBHUFFRDY" in spec.
    JBIG2::HuffmanTable const* refinement_size_table { nullptr };         // "SBHUFFRSIZE" in spec.

    u8 refinement_template { 0 };                                               // "SBRTEMPLATE" in spec.
    Array<JBIG2::AdaptiveTemplatePixel, 2> refinement_adaptive_template_pixels; // "SBRATX" / "SBRATY" in spec.
    // FIXME: COLEXTFLAG, SBCOLS

    // If uses_huffman_encoding is true, generic_region_decoding_procedure() reads data off this stream.
    Stream* stream { nullptr };

    // If uses_huffman_encoding is false, generic_region_decoding_procedure() reads data off this decoder.
    MQArithmeticDecoder* arithmetic_decoder { nullptr };
};

struct TextContexts {
    explicit TextContexts(u32 id_symbol_code_length)
        : id_decoder(id_symbol_code_length)
    {
    }

    JBIG2::ArithmeticIntegerDecoder delta_t_integer_decoder;         // "IADT" in spec.
    JBIG2::ArithmeticIntegerDecoder first_s_integer_decoder;         // "IAFS" in spec.
    JBIG2::ArithmeticIntegerDecoder subsequent_s_integer_decoder;    // "IADS" in spec.
    JBIG2::ArithmeticIntegerDecoder instance_t_integer_decoder;      // "IAIT" in spec.
    JBIG2::ArithmeticIntegerIDDecoder id_decoder;                    // "IAID" in spec.
    JBIG2::ArithmeticIntegerDecoder refinement_delta_width_decoder;  // "IARDW" in spec.
    JBIG2::ArithmeticIntegerDecoder refinement_delta_height_decoder; // "IARDH" in spec.
    JBIG2::ArithmeticIntegerDecoder refinement_x_offset_decoder;     // "IARDX" in spec.
    JBIG2::ArithmeticIntegerDecoder refinement_y_offset_decoder;     // "IARDY" in spec.
    JBIG2::ArithmeticIntegerDecoder has_refinement_image_decoder;    // "IARI" in spec.
};

// 6.4 Text Region Decoding Procedure
static ErrorOr<NonnullRefPtr<BilevelImage>> text_region_decoding_procedure(TextRegionDecodingInputParameters const& inputs, Optional<TextContexts>& text_contexts, Optional<RefinementContexts>& refinement_contexts)
{
    Optional<BigEndianInputBitStream> bit_stream;
    MQArithmeticDecoder* decoder = nullptr;
    if (inputs.uses_huffman_encoding) {
        bit_stream = BigEndianInputBitStream { MaybeOwned { *inputs.stream } };
    } else {
        decoder = inputs.arithmetic_decoder;
    }

    // 6.4.6 Strip delta T
    // "If SBHUFF is 1, decode a value using the Huffman table specified by SBHUFFDT and multiply the resulting value by SBSTRIPS.
    //  If SBHUFF is 0, decode a value using the IADT integer arithmetic decoding procedure (see Annex A) and multiply the resulting value by SBSTRIPS."
    auto read_delta_t = [&]() -> ErrorOr<i32> {
        if (inputs.uses_huffman_encoding)
            return TRY(inputs.delta_t_table->read_symbol_non_oob(*bit_stream)) * inputs.size_of_symbol_instance_strips;
        return TRY(text_contexts->delta_t_integer_decoder.decode_non_oob(*decoder)) * inputs.size_of_symbol_instance_strips;
    };

    // 6.4.7 First symbol instance S coordinate
    // "If SBHUFF is 1, decode a value using the Huffman table specified by SBHUFFFS.
    //  If SBHUFF is 0, decode a value using the IAFS integer arithmetic decoding procedure (see Annex A)."
    auto read_first_s = [&]() -> ErrorOr<i32> {
        if (inputs.uses_huffman_encoding)
            return inputs.first_s_table->read_symbol_non_oob(*bit_stream);
        return text_contexts->first_s_integer_decoder.decode_non_oob(*decoder);
    };

    // 6.4.8 Subsequent symbol instance S coordinate
    // "If SBHUFF is 1, decode a value using the Huffman table specified by SBHUFFDS.
    //  If SBHUFF is 0, decode a value using the IADS integer arithmetic decoding procedure (see Annex A).
    //  In either case it is possible that the result of this decoding is the out-of-band value OOB."
    auto read_subsequent_s = [&]() -> ErrorOr<Optional<i32>> {
        if (inputs.uses_huffman_encoding)
            return inputs.subsequent_s_table->read_symbol(*bit_stream);
        return text_contexts->subsequent_s_integer_decoder.decode(*decoder);
    };

    // 6.4.9 Symbol instance T coordinate
    // "If SBSTRIPS == 1, then the value decoded is always zero. Otherwise:
    //  • If SBHUFF is 1, decode a value by reading ceil(log2(SBSTRIPS)) bits directly from the bitstream.
    //  • If SBHUFF is 0, decode a value using the IAIT integer arithmetic decoding procedure (see Annex A)."
    auto read_instance_t = [&]() -> ErrorOr<i32> {
        if (inputs.size_of_symbol_instance_strips == 1)
            return 0;
        if (inputs.uses_huffman_encoding)
            return TRY(bit_stream->read_bits(ceil(log2(inputs.size_of_symbol_instance_strips))));
        return text_contexts->instance_t_integer_decoder.decode_non_oob(*decoder);
    };

    // 6.4.10 Symbol instance symbol ID
    // "If SBHUFF is 1, decode a value by reading one bit at a time until the resulting bit string is equal to one of the entries in
    //  SBSYMCODES. The resulting value, which is IDI, is the index of the entry in SBSYMCODES that is read.
    //  If SBHUFF is 0, decode a value using the IAID integer arithmetic decoding procedure (see Annex A). Set IDI to the
    //  resulting value."
    auto read_symbol_id = [&]() -> ErrorOr<u32> {
        if (inputs.uses_huffman_encoding)
            return inputs.symbol_id_table->read_symbol_non_oob(*bit_stream);
        return text_contexts->id_decoder.decode(*decoder);
    };

    // 6.4.11.1 Symbol instance refinement delta width
    // "If SBHUFF is 1, decode a value using the Huffman table specified by SBHUFFRDW.
    //  If SBHUFF is 0, decode a value using the IARDW integer arithmetic decoding procedure (see Annex A)."
    auto read_refinement_delta_width = [&]() -> ErrorOr<i32> {
        if (inputs.uses_huffman_encoding)
            return inputs.refinement_delta_width_table->read_symbol_non_oob(*bit_stream);
        return text_contexts->refinement_delta_width_decoder.decode_non_oob(*decoder);
    };

    // 6.4.11.2 Symbol instance refinement delta height
    // "If SBHUFF is 1, decode a value using the Huffman table specified by SBHUFFRDH.
    //  If SBHUFF is 0, decode a value using the IARDH integer arithmetic decoding procedure (see Annex A)."
    auto read_refinement_delta_height = [&]() -> ErrorOr<i32> {
        if (inputs.uses_huffman_encoding)
            return inputs.refinement_delta_height_table->read_symbol_non_oob(*bit_stream);
        return text_contexts->refinement_delta_height_decoder.decode_non_oob(*decoder);
    };

    // 6.4.11.3 Symbol instance refinement X offset
    // "If SBHUFF is 1, decode a value using the Huffman table specified by SBHUFFRDX.
    //  If SBHUFF is 0, decode a value using the IARDX integer arithmetic decoding procedure (see Annex A)."
    auto read_refinement_x_offset = [&]() -> ErrorOr<i32> {
        if (inputs.uses_huffman_encoding)
            return inputs.refinement_x_offset_table->read_symbol_non_oob(*bit_stream);
        return text_contexts->refinement_x_offset_decoder.decode_non_oob(*decoder);
    };

    // 6.4.11.4 Symbol instance refinement Y offset
    // "If SBHUFF is 1, decode a value using the Huffman table specified by SBHUFFRDY.
    //  If SBHUFF is 0, decode a value using the IARDY integer arithmetic decoding procedure (see Annex A)."
    auto read_refinement_y_offset = [&]() -> ErrorOr<i32> {
        if (inputs.uses_huffman_encoding)
            return inputs.refinement_y_offset_table->read_symbol_non_oob(*bit_stream);
        return text_contexts->refinement_y_offset_decoder.decode_non_oob(*decoder);
    };

    // 6.4.11 Symbol instance bitmap
    Optional<BilevelSubImage> refinement_result;
    auto read_bitmap = [&](u32 id) -> ErrorOr<BilevelSubImage const*> {
        if (id >= inputs.symbols.size())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Symbol ID out of range");
        auto const& symbol = inputs.symbols[id];

        bool has_refinement_image = false; // "R_I" in spec.
        if (inputs.uses_refinement_coding) {
            // "• If SBHUFF is 1, then read one bit and set RI to the value of that bit.
            //  • If SBHUFF is 0, then decode one bit using the IARI integer arithmetic decoding procedure and set RI to the value of that bit."
            if (inputs.uses_huffman_encoding)
                has_refinement_image = TRY(bit_stream->read_bit());
            else
                has_refinement_image = TRY(text_contexts->has_refinement_image_decoder.decode_non_oob(*decoder));
        }

        // "If RI is 0 then set the symbol instance bitmap IBI to SBSYMS[IDI]."
        if (!has_refinement_image)
            return &symbol;

        if (inputs.uses_huffman_encoding)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode refinement images with huffman encoding yet");

        auto refinement_delta_width = TRY(read_refinement_delta_width());
        auto refinement_delta_height = TRY(read_refinement_delta_height());
        auto refinement_x_offset = TRY(read_refinement_x_offset());
        auto refinement_y_offset = TRY(read_refinement_y_offset());
        // FIXME: This is missing some steps needed for the SBHUFF = 1 case.

        dbgln_if(JBIG2_DEBUG, "refinement delta width: {}, refinement delta height: {}, refinement x offset: {}, refinement y offset: {}", refinement_delta_width, refinement_delta_height, refinement_x_offset, refinement_y_offset);

        // Table 12 – Parameters used to decode a symbol instance's bitmap using refinement
        if (symbol.width() > static_cast<u32>(INT32_MAX) || static_cast<i32>(symbol.width()) + refinement_delta_width < 0)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Refinement width out of bounds");
        if (symbol.height() > static_cast<u32>(INT32_MAX) || static_cast<i32>(symbol.height()) + refinement_delta_height < 0)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Refinement height out of bounds");

        GenericRefinementRegionDecodingInputParameters refinement_inputs;
        refinement_inputs.region_width = symbol.width() + refinement_delta_width;
        refinement_inputs.region_height = symbol.height() + refinement_delta_height;
        refinement_inputs.gr_template = inputs.refinement_template;
        refinement_inputs.reference_bitmap = &symbol;
        refinement_inputs.reference_x_offset = floor_div(refinement_delta_width, 2) + refinement_x_offset;
        refinement_inputs.reference_y_offset = floor_div(refinement_delta_height, 2) + refinement_y_offset;
        refinement_inputs.is_typical_prediction_used = false;
        refinement_inputs.adaptive_template_pixels = inputs.refinement_adaptive_template_pixels;
        auto result = TRY(generic_refinement_region_decoding_procedure(refinement_inputs, *decoder, refinement_contexts.value()));
        refinement_result = result->as_subbitmap();
        return &refinement_result.value();
    };

    // 6.4.5 Decoding the text region

    // "1) Fill a bitmap SBREG, of the size given by SBW and SBH, with the SBDEFPIXEL value."
    auto result = TRY(BilevelImage::create(inputs.region_width, inputs.region_height));
    if (inputs.default_pixel != 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot handle SBDEFPIXEL not equal to 0 yet");
    result->fill(inputs.default_pixel != 0);

    // "2) Decode the initial STRIPT value as described in 6.4.6. Negate the decoded value and assign this negated value to the variable STRIPT.
    //     Assign the value 0 to FIRSTS. Assign the value 0 to NINSTANCES."
    i32 strip_t = -TRY(read_delta_t());
    i32 first_s = 0;
    u32 n_instances = 0;

    // "3) If COLEXTFLAG is 1, decode the colour section as described in 6.4.12."
    // FIXME: Implement support for colors one day.

    // "4) Decode each strip as follows:
    //      a) If NINSTANCES is equal to SBNUMINSTANCES then there are no more strips to decode,
    //         and the process of decoding the text region is complete; proceed to step 4)."
    // Implementor's note. The spec means "proceed to step 5)" at the end of 4a).
    while (n_instances < inputs.number_of_instances) {
        // "b) Decode the strip's delta T value as described in 6.4.6. Let DT be the decoded value. Set:
        //         STRIPT = STRIPT + DT"
        i32 delta_t = TRY(read_delta_t());
        strip_t += delta_t;

        i32 cur_s;
        bool is_first_symbol = true;
        while (true) {
            // "c) Decode each symbol instance in the strip as follows:
            //      i) If the current symbol instance is the first symbol instance in the strip, then decode the first
            //         symbol instance's S coordinate as described in 6.4.7. Let DFS be the decoded value. Set:
            //              FIRSTS = FIRSTS + DFS
            //              CURS = FIRSTS
            //      ii) Otherwise, if the current symbol instance is not the first symbol instance in the strip, decode
            //          the symbol instance's S coordinate as described in 6.4.8. If the result of this decoding is OOB
            //          then the last symbol instance of the strip has been decoded; proceed to step 3 d). Otherwise, let
            //          IDS be the decoded value. Set:
            //              CURS = CURS + IDS + SBDSOFFSET"
            // Implementor's note: The spec means "proceed to step 4 d)" in 4c ii).
            if (is_first_symbol) {
                i32 delta_first_s = TRY(read_first_s());
                first_s += delta_first_s;
                cur_s = first_s;
                is_first_symbol = false;
            } else {
                auto subsequent_s = TRY(read_subsequent_s());
                if (!subsequent_s.has_value())
                    break;
                i32 instance_delta_s = subsequent_s.value();
                cur_s += instance_delta_s + inputs.delta_s_offset;
            }

            //     "iii) Decode the symbol instance's T coordinate as described in 6.4.9. Let CURT be the decoded value. Set:
            //              TI = STRIPT + CURT"
            i32 cur_t = TRY(read_instance_t());
            i32 t_instance = strip_t + cur_t;

            //     "iv) Decode the symbol instance's symbol ID as described in 6.4.10. Let IDI be the decoded value."
            u32 id = TRY(read_symbol_id());

            //     "v) Determine the symbol instance's bitmap IBI as described in 6.4.11. The width and height of this
            //         bitmap shall be denoted as WI and HI respectively."
            auto const& symbol = *TRY(read_bitmap(id));

            //     "vi) Update CURS as follows:
            //      • If TRANSPOSED is 0, and REFCORNER is TOPRIGHT or BOTTOMRIGHT, set:
            //              CURS = CURS + WI – 1
            //      • If TRANSPOSED is 1, and REFCORNER is BOTTOMLEFT or BOTTOMRIGHT, set:
            //              CURS = CURS + HI – 1
            //      • Otherwise, do not change CURS in this step."
            using enum JBIG2::ReferenceCorner;
            if (!inputs.is_transposed && (inputs.reference_corner == TopRight || inputs.reference_corner == BottomRight))
                cur_s += symbol.width() - 1;
            if (inputs.is_transposed && (inputs.reference_corner == BottomLeft || inputs.reference_corner == BottomRight))
                cur_s += symbol.height() - 1;

            //     "vii) Set:
            //              SI = CURS"
            auto s_instance = cur_s;

            //     "viii) Determine the location of the symbol instance bitmap with respect to SBREG as follows:
            //          • If TRANSPOSED is 0, then:
            //              – If REFCORNER is TOPLEFT then the top left pixel of the symbol instance bitmap
            //                IBI shall be placed at SBREG[SI, TI].
            //              – If REFCORNER is TOPRIGHT then the top right pixel of the symbol instance
            //                bitmap IBI shall be placed at SBREG[SI, TI].
            //              – If REFCORNER is BOTTOMLEFT then the bottom left pixel of the symbol
            //                instance bitmap IBI shall be placed at SBREG[SI, TI].
            //              – If REFCORNER is BOTTOMRIGHT then the bottom right pixel of the symbol
            //                instance bitmap IBI shall be placed at SBREG[SI, TI].
            //          • If TRANSPOSED is 1, then:
            //              – If REFCORNER is TOPLEFT then the top left pixel of the symbol instance bitmap
            //                IBI shall be placed at SBREG[TI, SI].
            //              – If REFCORNER is TOPRIGHT then the top right pixel of the symbol instance
            //                bitmap IBI shall be placed at SBREG[TI, SI].
            //              – If REFCORNER is BOTTOMLEFT then the bottom left pixel of the symbol
            //                instance bitmap IBI shall be placed at SBREG[TI, SI].
            //              – If REFCORNER is BOTTOMRIGHT then the bottom right pixel of the symbol
            //                instance bitmap IBI shall be placed at SBREG[TI, SI].
            //          If any part of IBI, when placed at this location, lies outside the bounds of SBREG, then ignore
            //          this part of IBI in step 3 c) ix)."
            // Implementor's note: The spec means "ignore this part of IBI in step 3 c) x)" in 3c viii)'s last sentence.
            if (inputs.is_transposed)
                swap(s_instance, t_instance);
            if (inputs.reference_corner == TopRight || inputs.reference_corner == BottomRight)
                s_instance -= symbol.width() - 1;
            if (inputs.reference_corner == BottomLeft || inputs.reference_corner == BottomRight)
                t_instance -= symbol.height() - 1;

            //     "ix) If COLEXTFLAG is 1, set the colour specified by SBCOLS[SBFGCOLID[NINSTANCES]]
            //          to the foreground colour of the symbol instance bitmap IBI."
            // FIXME: Implement support for colors one day.

            //     "x) Draw IBI into SBREG. Combine each pixel of IBI with the current value of the corresponding
            //         pixel in SBREG, using the combination operator specified by SBCOMBOP. Write the results
            //         of each combination into that pixel in SBREG."
            dbgln_if(JBIG2_DEBUG, "combining symbol {} ({}x{}) at ({}, {}) with operator {}", id, symbol.width(), symbol.height(), s_instance, t_instance, (int)inputs.operator_);
            symbol.composite_onto(*result, { s_instance, t_instance }, to_composition_type(inputs.operator_));

            //     "xi) Update CURS as follows:
            //          • If TRANSPOSED is 0, and REFCORNER is TOPLEFT or BOTTOMLEFT, set:
            //              CURS = CURS + WI – 1
            //          • If TRANSPOSED is 1, and REFCORNER is TOPLEFT or TOPRIGHT, set:
            //              CURS = CURS + HI – 1
            //          • Otherwise, do not change CURS in this step."
            if (!inputs.is_transposed && (inputs.reference_corner == TopLeft || inputs.reference_corner == BottomLeft))
                cur_s += symbol.width() - 1;
            if (inputs.is_transposed && (inputs.reference_corner == TopLeft || inputs.reference_corner == TopRight))
                cur_s += symbol.height() - 1;

            //      "xii) Set:
            //              NINSTANCES = NINSTANCES + 1"
            ++n_instances;
        }
        //  "d) When the strip has been completely decoded, decode the next strip."
        // (Done in the next loop iteration.)
    }

    //  "5) After all the strips have been decoded, the current contents of SBREG are the results that shall be
    //      obtained by every decoder, whether it performs this exact sequence of steps or not."
    return result;
}

// 6.5.2 Input parameters
// Table 13 – Parameters for the symbol dictionary decoding procedure
struct SymbolDictionaryDecodingInputParameters {

    bool uses_huffman_encoding { false };               // "SDHUFF" in spec.
    bool uses_refinement_or_aggregate_coding { false }; // "SDREFAGG" in spec.

    Vector<BilevelSubImage> input_symbols; // "SDNUMINSYMS", "SDINSYMS" in spec.

    u32 number_of_new_symbols { 0 };      // "SDNUMNEWSYMS" in spec.
    u32 number_of_exported_symbols { 0 }; // "SDNUMEXSYMS" in spec.

    // Only set if uses_huffman_encoding is true.
    JBIG2::HuffmanTable const* delta_height_table;               // "SDHUFFDH" in spec.
    JBIG2::HuffmanTable const* delta_width_table;                // "SDHUFFDW" in spec.
    JBIG2::HuffmanTable const* bitmap_size_table;                // "SDHUFFBMSIZE" in spec.
    JBIG2::HuffmanTable const* number_of_symbol_instances_table; // "SDHUFFAGGINST" in spec.

    u8 symbol_template { 0 };                                        // "SDTEMPLATE" in spec.
    Array<JBIG2::AdaptiveTemplatePixel, 4> adaptive_template_pixels; // "SDATX" / "SDATY" in spec.

    u8 refinement_template { 0 };                                               // "SDRTEMPLATE" in spec;
    Array<JBIG2::AdaptiveTemplatePixel, 2> refinement_adaptive_template_pixels; // "SDRATX" / "SDRATY" in spec.
};

struct SymbolContexts {
    JBIG2::ArithmeticIntegerDecoder delta_height_integer_decoder;       // "IADH" in spec.
    JBIG2::ArithmeticIntegerDecoder delta_width_integer_decoder;        // "IADW" in spec.
    JBIG2::ArithmeticIntegerDecoder number_of_symbol_instances_decoder; // "IAAI" in spec.
    JBIG2::ArithmeticIntegerDecoder export_integer_decoder;             // "IAEX" in spec.
};

// 6.5 Symbol Dictionary Decoding Procedure
static ErrorOr<Vector<BilevelSubImage>> symbol_dictionary_decoding_procedure(SymbolDictionaryDecodingInputParameters const& inputs, ReadonlyBytes data)
{
    Optional<FixedMemoryStream> stream;
    Optional<BigEndianInputBitStream> bit_stream;
    Optional<MQArithmeticDecoder> decoder;
    Optional<JBIG2::GenericContexts> generic_contexts;
    Optional<SymbolContexts> symbol_contexts;
    if (inputs.uses_huffman_encoding) {
        stream = FixedMemoryStream { data };
        bit_stream = BigEndianInputBitStream { MaybeOwned { stream.value() } };
    } else {
        decoder = TRY(MQArithmeticDecoder::initialize(data));
        generic_contexts = JBIG2::GenericContexts { inputs.symbol_template };
        symbol_contexts = SymbolContexts {};
    }

    // 6.5.6 Height class delta height
    // "If SDHUFF is 1, decode a value using the Huffman table specified by SDHUFFDH.
    //  If SDHUFF is 0, decode a value using the IADH integer arithmetic decoding procedure (see Annex A)."
    auto read_delta_height = [&]() -> ErrorOr<i32> {
        if (inputs.uses_huffman_encoding)
            return inputs.delta_height_table->read_symbol_non_oob(*bit_stream);
        return symbol_contexts->delta_height_integer_decoder.decode_non_oob(*decoder);
    };

    // 6.5.7 Delta width
    // "If SDHUFF is 1, decode a value using the Huffman table specified by SDHUFFDW.
    //  If SDHUFF is 0, decode a value using the IADW integer arithmetic decoding procedure (see Annex A).
    //  In either case it is possible that the result of this decoding is the out-of-band value OOB."
    auto read_delta_width = [&]() -> ErrorOr<Optional<i32>> {
        if (inputs.uses_huffman_encoding)
            return inputs.delta_width_table->read_symbol(*bit_stream);
        return symbol_contexts->delta_width_integer_decoder.decode(*decoder);
    };

    // 6.5.8 Symbol bitmap
    // "This field is only present if SDHUFF = 0 or SDREFAGG = 1. This field takes one of two forms; SDREFAGG
    //  determines which form is used."

    // 6.5.8.2.1 Number of symbol instances in aggregation
    // If SDHUFF is 1, decode a value using the Huffman table specified by SDHUFFAGGINST.
    // If SDHUFF is 0, decode a value using the IAAI integer arithmetic decoding procedure (see Annex A).
    Optional<JBIG2::ArithmeticIntegerDecoder> number_of_symbol_instances_decoder; // "IAAI" in spec.
    auto read_number_of_symbol_instances = [&]() -> ErrorOr<i32> {
        if (inputs.uses_huffman_encoding)
            return inputs.number_of_symbol_instances_table->read_symbol_non_oob(*bit_stream);
        return symbol_contexts->number_of_symbol_instances_decoder.decode_non_oob(*decoder);
    };

    // 6.5.8.1 Direct-coded symbol bitmap
    Optional<TextContexts> text_contexts;
    Optional<RefinementContexts> refinement_contexts;

    // This belongs in 6.5.5 1) below, but also needs to be captured by read_bitmap here.
    Vector<BilevelSubImage> new_symbols;

    auto read_symbol_bitmap = [&](u32 width, u32 height) -> ErrorOr<NonnullRefPtr<BilevelImage>> {
        // 6.5.8 Symbol bitmap
        if (inputs.uses_huffman_encoding)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode generic symbol bitmaps with huffman encoding");

        // 6.5.8.1 Direct-coded symbol bitmap
        // "If SDREFAGG is 0, then decode the symbol's bitmap using a generic region decoding procedure as described in 6.2.
        //  Set the parameters to this decoding procedure as shown in Table 16."
        if (!inputs.uses_refinement_or_aggregate_coding) {
            // Table 16 – Parameters used to decode a symbol's bitmap using generic bitmap decoding
            GenericRegionDecodingInputParameters generic_inputs;
            generic_inputs.is_modified_modified_read = false;
            generic_inputs.region_width = width;
            generic_inputs.region_height = height;
            generic_inputs.gb_template = inputs.symbol_template;
            generic_inputs.is_extended_reference_template_used = false; // Missing from spec in table 16.
            for (int i = 0; i < 4; ++i)
                generic_inputs.adaptive_template_pixels[i] = inputs.adaptive_template_pixels[i];
            generic_inputs.arithmetic_decoder = &decoder.value();
            return generic_region_decoding_procedure(generic_inputs, generic_contexts);
        }

        // 6.5.8.2 Refinement/aggregate-coded symbol bitmap
        // "1) Decode the number of symbol instances contained in the aggregation, as specified in 6.5.8.2.1. Let REFAGGNINST be the value decoded."
        auto number_of_symbol_instances = TRY(read_number_of_symbol_instances()); // "REFAGGNINST" in spec.
        dbgln_if(JBIG2_DEBUG, "Number of symbol instances: {}", number_of_symbol_instances);

        // 6.5.8.2.3 Setting SBSYMCODES and SBSYMCODELEN
        // FIXME: Implement support for SDHUFF = 1
        u32 code_length = ceil(log2(inputs.input_symbols.size() + inputs.number_of_new_symbols));

        if (!text_contexts.has_value())
            text_contexts = TextContexts { code_length };
        if (!refinement_contexts.has_value())
            refinement_contexts = RefinementContexts(inputs.refinement_template);

        if (number_of_symbol_instances > 1) {
            // "2) If REFAGGNINST is greater than one, then decode the bitmap itself using a text region decoding procedure
            //     as described in 6.4. Set the parameters to this decoding procedure as shown in Table 17."

            // Table 17 – Parameters used to decode a symbol's bitmap using refinement/aggregate decoding
            TextRegionDecodingInputParameters text_inputs;
            text_inputs.uses_huffman_encoding = inputs.uses_huffman_encoding;
            text_inputs.uses_refinement_coding = true;
            text_inputs.region_width = width;
            text_inputs.region_height = height;
            text_inputs.number_of_instances = number_of_symbol_instances;
            text_inputs.size_of_symbol_instance_strips = 1;
            text_inputs.id_symbol_code_length = code_length;

            // 6.5.8.2.4 Setting SBSYMS
            // "Set SBSYMS to an array of SDNUMINSYMS + NSYMSDECODED symbols, formed by concatenating the array
            //  SDINSYMS and the first NSYMSDECODED entries of the array SDNEWSYMS."
            text_inputs.symbols.extend(inputs.input_symbols);
            text_inputs.symbols.extend(new_symbols);

            text_inputs.default_pixel = 0;
            text_inputs.operator_ = JBIG2::CombinationOperator::Or;
            text_inputs.is_transposed = false;
            text_inputs.reference_corner = JBIG2::ReferenceCorner::TopLeft;
            text_inputs.delta_s_offset = 0;
            text_inputs.first_s_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_6));
            text_inputs.subsequent_s_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_8));
            text_inputs.delta_t_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_11));
            text_inputs.refinement_delta_width_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_15));
            text_inputs.refinement_delta_height_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_15));
            text_inputs.refinement_x_offset_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_15));
            text_inputs.refinement_y_offset_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_15));
            text_inputs.refinement_size_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_1));
            text_inputs.refinement_template = inputs.refinement_template;
            text_inputs.refinement_adaptive_template_pixels = inputs.refinement_adaptive_template_pixels;

            text_inputs.arithmetic_decoder = &decoder.value();
            return text_region_decoding_procedure(text_inputs, text_contexts, refinement_contexts);
        }

        // "3) If REFAGGNINST is equal to one, then decode the bitmap as described in 6.5.8.2.2."

        // 6.5.8.2.2 Decoding a bitmap when REFAGGNINST = 1
        // FIXME: This is missing some steps for the SDHUFF = 1 case.
        if (number_of_symbol_instances != 1)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Unexpected number of symbol instances");

        u32 symbol_id = text_contexts->id_decoder.decode(*decoder);
        i32 refinement_x_offset = TRY(text_contexts->refinement_x_offset_decoder.decode_non_oob(*decoder));
        i32 refinement_y_offset = TRY(text_contexts->refinement_y_offset_decoder.decode_non_oob(*decoder));

        if (symbol_id >= inputs.input_symbols.size() && symbol_id - inputs.input_symbols.size() >= new_symbols.size())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Refinement/aggregate symbol ID out of range");

        auto const& IBO = (symbol_id < inputs.input_symbols.size()) ? inputs.input_symbols[symbol_id] : new_symbols[symbol_id - inputs.input_symbols.size()];
        // Table 18 – Parameters used to decode a symbol's bitmap when REFAGGNINST = 1
        GenericRefinementRegionDecodingInputParameters refinement_inputs;
        refinement_inputs.region_width = width;
        refinement_inputs.region_height = height;
        refinement_inputs.gr_template = inputs.refinement_template;
        refinement_inputs.reference_bitmap = &IBO;
        refinement_inputs.reference_x_offset = refinement_x_offset;
        refinement_inputs.reference_y_offset = refinement_y_offset;
        refinement_inputs.is_typical_prediction_used = false;
        refinement_inputs.adaptive_template_pixels = inputs.refinement_adaptive_template_pixels;
        return generic_refinement_region_decoding_procedure(refinement_inputs, decoder.value(), refinement_contexts.value());
    };

    auto read_height_class_collective_bitmap = [&](u32 total_width, u32 height) -> ErrorOr<NonnullRefPtr<BilevelImage>> {
        // 6.5.9 Height class collective bitmap
        // "1) Read the size in bytes using the SDHUFFBMSIZE Huffman table. Let BMSIZE be the value decoded."
        auto bitmap_size = TRY(inputs.bitmap_size_table->read_symbol_non_oob(*bit_stream));

        // "2) Skip over any bits remaining in the last byte read."
        bit_stream->align_to_byte_boundary();

        NonnullRefPtr<BilevelImage> result = TRY([&]() -> ErrorOr<NonnullRefPtr<BilevelImage>> {
            // "3) If BMSIZE is zero, then the bitmap is stored uncompressed, and the actual size in bytes is:
            //
            //         HCHEIGHT * ceil_div(TOTWIDTH, 8)
            //
            //     Decode the bitmap by reading this many bytes and treating it as HCHEIGHT rows of TOTWIDTH pixels, each
            //     row padded out to a byte boundary with 0-7 0 bits."
            if (bitmap_size == 0) {
                auto result = TRY(BilevelImage::create(total_width, height));
                TRY(bit_stream->read_until_filled(result->bytes()));
                return result;
            }
            // "4) Otherwise, decode the bitmap using a generic bitmap decoding procedure as described in 6.2. Set the
            //     parameters to this decoding procedure as shown in Table 19."
            // Table 19 – Parameters used to decode a height class collective bitmap
            GenericRegionDecodingInputParameters generic_inputs;
            generic_inputs.is_modified_modified_read = true;
            generic_inputs.region_width = total_width;
            generic_inputs.region_height = height;

            ReadonlyBytes bitmap_data = data.slice(stream->offset(), bitmap_size);
            TRY(stream->discard(bitmap_size));
            FixedMemoryStream bitmap_stream { bitmap_data };
            generic_inputs.stream = &bitmap_stream;
            return generic_region_decoding_procedure(generic_inputs, generic_contexts);
        }());

        // "5) Skip over any bits remaining in the last byte read."
        // Already done above. This step allowed us to slice the data in step 4.

        return result;
    };

    // 6.5.5 Decoding the symbol dictionary
    // "1) Create an array SDNEWSYMS of bitmaps, having SDNUMNEWSYMS entries."
    // Done above read_symbol_bitmap's definition.

    // "2) If SDHUFF is 1 and SDREFAGG is 0, create an array SDNEWSYMWIDTHS of integers, having SDNUMNEWSYMS entries."
    Vector<u32> new_symbol_widths;

    // "3) Set:
    //      HCHEIGHT = 0
    //      NSYMSDECODED = 0"
    u32 height_class_height = 0;
    u32 number_of_symbols_decoded = 0;

    // "4) Decode each height class as follows:
    //      a) If NSYMSDECODED == SDNUMNEWSYMS then all the symbols in the dictionary have been decoded; proceed to step 5)."
    while (number_of_symbols_decoded < inputs.number_of_new_symbols) {
        // "b) Decode the height class delta height as described in 6.5.6. Let HCDH be the decoded value. Set:
        //      HCHEIGHT = HCEIGHT + HCDH
        //      SYMWIDTH = 0
        //      TOTWIDTH = 0
        //      HCFIRSTSYM = NSYMSDECODED"
        i32 delta_height = TRY(read_delta_height());
        height_class_height += delta_height;
        u32 symbol_width = 0;
        u32 total_width = 0;
        u32 height_class_first_symbol = number_of_symbols_decoded;
        // "c) Decode each symbol within the height class as follows:"
        while (true) {
            // "i) Decode the delta width for the symbol as described in 6.5.7."
            auto opt_delta_width = TRY(read_delta_width());
            // "   If the result of this decoding is OOB then all the symbols in this height class have been decoded; proceed to step 4 d)."
            if (!opt_delta_width.has_value())
                break;

            VERIFY(number_of_symbols_decoded < inputs.number_of_new_symbols);
            // "   Otherwise let DW be the decoded value and set:"
            //         SYMWIDTH = SYMWIDTH + DW
            //         TOTWIDTH = TOTWIDTH + SYMWIDTH"
            i32 delta_width = opt_delta_width.value();
            symbol_width += delta_width;
            total_width += symbol_width;

            // "ii) If SDHUFF is 0 or SDREFAGG is 1, then decode the symbol's bitmap as described in 6.5.8.
            //      Let BS be the decoded bitmap (this bitmap has width SYMWIDTH and height HCHEIGHT). Set:
            //          SDNEWSYMS[NSYMSDECODED] = BS"
            // FIXME: Doing this eagerly is pretty wasteful. Decode on demand instead?
            if (!inputs.uses_huffman_encoding || inputs.uses_refinement_or_aggregate_coding) {
                auto bitmap = TRY(read_symbol_bitmap(symbol_width, height_class_height));
                new_symbols.append(bitmap->as_subbitmap());
            }

            // "iii) If SDHUFF is 1 and SDREFAGG is 0, then set:
            //      SDNEWSYMWIDTHS[NSYMSDECODED] = SYMWIDTH"
            if (inputs.uses_huffman_encoding && !inputs.uses_refinement_or_aggregate_coding)
                new_symbol_widths.append(symbol_width);

            // "iv) Set:
            //      NSYMSDECODED = NSYMSDECODED + 1"
            number_of_symbols_decoded++;
        }

        // "d) If SDHUFF is 1 and SDREFAGG is 0, then decode the height class collective bitmap as described
        //     in 6.5.9. Let BHC be the decoded bitmap. This bitmap has width TOTWIDTH and height
        //     HCHEIGHT. Break up the bitmap BHC as follows to obtain the symbols
        //     SDNEWSYMS[HCFIRSTSYM] through SDNEWSYMS[NSYMSDECODED – 1].
        //
        //     BHC contains the NSYMSDECODED – HCFIRSTSYM symbols concatenated left-to-right, with no
        //     intervening gaps. For each I between HCFIRSTSYM and NSYMSDECODED – 1:
        //
        //     • the width of SDNEWSYMS[I] is the value of SDNEWSYMWIDTHS[I];
        //     • the height of SDNEWSYMS[I] is HCHEIGHT; and
        //     • the bitmap SDNEWSYMS[I] can be obtained by extracting the columns of BHC from:
        //
        //           sum(J=HCFIRSTSYM to I-1, SDNEWSYMWIDTHS[J]) to sum(J=HCFIRSTSYM to I-1, SDNEWSYMWIDTHS[J])^(-1)"
        // Note: I think the spec means "...to sum(J=HCFIRSTSYM to I, SDNEWSYMWIDTHS[J]) - 1" in the last sentence.
        if (inputs.uses_huffman_encoding && !inputs.uses_refinement_or_aggregate_coding) {
            auto collective_bitmap = TRY(read_height_class_collective_bitmap(total_width, height_class_height));
            u32 current_column = 0;
            for (size_t i = height_class_first_symbol; i < number_of_symbols_decoded; ++i) {
                auto width = new_symbol_widths[i];
                IntRect symbol_rect { static_cast<int>(current_column), 0, static_cast<int>(width), static_cast<int>(height_class_height) };
                new_symbols.append(collective_bitmap->subbitmap(symbol_rect));
                current_column += width;
            }
        }
    }

    // "5) Determine which symbol bitmaps are exported from this symbol dictionary, as described in 6.5.10. These
    //     bitmaps can be drawn from the symbols that are used as input to the symbol dictionary decoding
    //     procedure as well as the new symbols produced by the decoding procedure."
    Optional<JBIG2::HuffmanTable*> export_table;
    if (inputs.uses_huffman_encoding)
        export_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_1));

    // 6.5.10 Exported symbols
    Vector<bool> export_flags;
    export_flags.resize(inputs.input_symbols.size() + inputs.number_of_new_symbols);

    // "1) Set:
    //      EXINDEX = 0
    //      CUREXFLAG = 0"
    u32 exported_index = 0;
    bool current_export_flag = false;

    do {
        // "2) Decode a value using Table B.1 if SDHUFF is 1, or the IAEX integer arithmetic decoding procedure if
        //  SDHUFF is 0. Let EXRUNLENGTH be the decoded value."
        i32 export_run_length;
        if (inputs.uses_huffman_encoding)
            export_run_length = TRY(export_table.value()->read_symbol_non_oob(*bit_stream));
        else
            export_run_length = TRY(symbol_contexts->export_integer_decoder.decode_non_oob(*decoder));

        // "3) Set EXFLAGS[EXINDEX] through EXFLAGS[EXINDEX + EXRUNLENGTH – 1] to CUREXFLAG.
        //  If EXRUNLENGTH = 0, then this step does not change any values."
        for (int i = 0; i < export_run_length; ++i)
            export_flags[exported_index + i] = current_export_flag;

        // "4) Set:
        //      EXINDEX = EXINDEX + EXRUNLENGTH
        //      CUREXFLAG = NOT(CUREXFLAG)"
        exported_index += export_run_length;
        current_export_flag = !current_export_flag;

        //  5) Repeat steps 2) through 4) until EXINDEX == SDNUMINSYMS + SDNUMNEWSYMS.
    } while (exported_index < inputs.input_symbols.size() + inputs.number_of_new_symbols);

    // "6) The array EXFLAGS now contains 1 for each symbol that is exported from the dictionary, and 0 for each
    //  symbol that is not exported."
    Vector<BilevelSubImage> exported_symbols;

    // "7) Set:
    //      I = 0
    //      J = 0
    //  8) For each value of I from 0 to SDNUMINSYMS + SDNUMNEWSYMS – 1,"
    for (size_t i = 0; i < inputs.input_symbols.size() + inputs.number_of_new_symbols; ++i) {
        // "if EXFLAGS[I] == 1 then perform the following steps:"
        if (!export_flags[i])
            continue;
        //  "a) If I < SDNUMINSYMS then set:
        //       SDEXSYMS[J] = SDINSYMS[I]
        //       J = J + 1"
        if (i < inputs.input_symbols.size())
            exported_symbols.append(inputs.input_symbols[i]);

        //  "b) If I >= SDNUMINSYMS then set:
        //       SDEXSYMS[J] = SDNEWSYMS[I – SDNUMINSYMS]
        //       J = J + 1"
        if (i >= inputs.input_symbols.size())
            exported_symbols.append(move(new_symbols[i - inputs.input_symbols.size()]));
    }

    if (exported_symbols.size() != inputs.number_of_exported_symbols)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Unexpected number of exported symbols");

    return exported_symbols;
}

// Annex C Gray-scale image decoding procedure

// C.2 Input parameters
// Table C.1 – Parameters for the gray-scale image decoding procedure
struct GrayscaleInputParameters {
    bool uses_mmr { false }; // "GSMMR" in spec.

    Optional<BilevelImage const&> skip_pattern; // "GSUSESKIP" / "GSKIP" in spec.

    u8 bpp { 0 };         // "GSBPP" in spec.
    u32 width { 0 };      // "GSW" in spec.
    u32 height { 0 };     // "GSH" in spec.
    u8 template_id { 0 }; // "GSTEMPLATE" in spec.

    // If uses_mmr is false, grayscale_image_decoding_procedure() reads data off this decoder.
    MQArithmeticDecoder* arithmetic_decoder { nullptr };
};

// C.5 Decoding the gray-scale image
static ErrorOr<Vector<u64>> grayscale_image_decoding_procedure(GrayscaleInputParameters const& inputs, ReadonlyBytes data, Optional<JBIG2::GenericContexts>& contexts)
{
    VERIFY(inputs.bpp < 64);

    // Table C.4 – Parameters used to decode a bitplane of the gray-scale image
    GenericRegionDecodingInputParameters generic_inputs;
    generic_inputs.is_modified_modified_read = inputs.uses_mmr;
    generic_inputs.region_width = inputs.width;
    generic_inputs.region_height = inputs.height;
    generic_inputs.gb_template = inputs.template_id;
    generic_inputs.is_typical_prediction_used = false;
    generic_inputs.is_extended_reference_template_used = false; // Missing from spec.
    generic_inputs.skip_pattern = inputs.skip_pattern;
    generic_inputs.adaptive_template_pixels[0].x = inputs.template_id <= 1 ? 3 : 2;
    generic_inputs.adaptive_template_pixels[0].y = -1;
    generic_inputs.adaptive_template_pixels[1].x = -3;
    generic_inputs.adaptive_template_pixels[1].y = -1;
    generic_inputs.adaptive_template_pixels[2].x = 2;
    generic_inputs.adaptive_template_pixels[2].y = -2;
    generic_inputs.adaptive_template_pixels[3].x = -2;
    generic_inputs.adaptive_template_pixels[3].y = -2;
    generic_inputs.arithmetic_decoder = inputs.arithmetic_decoder;

    // An MMR graymap is the only case where the size of the a generic region is not known in advance,
    // and where the data is immediately followed by more MMR data. We need to have the MMR decoder
    // skip the EOFB marker at the end, so that the following bitplanes can be decoded.
    // See 6.2.6 Decoding using MMR coding.
    generic_inputs.require_eof_after_mmr = GenericRegionDecodingInputParameters::RequireEOFBAfterMMR::Yes;

    FixedMemoryStream stream { data };
    generic_inputs.stream = &stream;

    // "The gray-scale image is obtained by decoding GSBPP bitplanes. These bitplanes are denoted (from least significant to
    //  most significant) GSPLANES[0], GSPLANES[1], . . . , GSPLANES[GSBPP – 1]. The bitplanes are Gray-coded, so
    //  that each bitplane's true value is equal to its coded value XORed with the next-more-significant bitplane."
    Vector<RefPtr<BilevelImage>> bitplanes;
    bitplanes.resize(inputs.bpp);

    // "1) Decode GSPLANES[GSBPP – 1] using the generic region decoding procedure. The parameters to the
    //     generic region decoding procedure are as shown in Table C.4."
    bitplanes[inputs.bpp - 1] = TRY(generic_region_decoding_procedure(generic_inputs, contexts));

    // "2) Set J = GSBPP – 2."
    int j = inputs.bpp - 2;

    // "3) While J >= 0, perform the following steps:"
    while (j >= 0) {
        // "a) Decode GSPLANES[J] using the generic region decoding procedure. The parameters to the generic
        //     region decoding procedure are as shown in Table C.4."
        bitplanes[j] = TRY(generic_region_decoding_procedure(generic_inputs, contexts));

        // "b) For each pixel (x, y) in GSPLANES[J], set:
        //     GSPLANES[J][x, y] = GSPLANES[J + 1][x, y] XOR GSPLANES[J][x, y]"
        bitplanes[j + 1]->composite_onto(*bitplanes[j], { 0, 0 }, BilevelImage::CompositionType::Xor);

        // "c) Set J = J – 1."
        j = j - 1;
    }

    // "4) For each (x, y), set:
    //     GSVALS [x, y] = sum_{J = 0}^{GSBPP - 1} GSPLANES[J][x,y] × 2**J)"
    Vector<u64> result;
    result.resize(inputs.width * inputs.height);
    for (u32 y = 0; y < inputs.height; ++y) {
        for (u32 x = 0; x < inputs.width; ++x) {
            u64 value = 0;
            for (int j = 0; j < inputs.bpp; ++j) {
                if (bitplanes[j]->get_bit(x, y))
                    value |= 1 << j;
            }
            result[y * inputs.width + x] = value;
        }
    }
    return result;
}

// 6.6.2 Input parameters
// Table 20 – Parameters for the halftone region decoding procedure
struct HalftoneRegionDecodingInputParameters {
    u32 region_width { 0 };                                                             // "HBW" in spec.
    u32 region_height { 0 };                                                            // "HBH" in spec.
    bool uses_mmr { false };                                                            // "HMMR" in spec.
    u8 halftone_template { 0 };                                                         // "HTEMPLATE" in spec.
    Vector<BilevelSubImage> patterns;                                                   // "HNUMPATS" / "HPATS" in spec.
    bool default_pixel_value { false };                                                 // "HDEFPIXEL" in spec.
    JBIG2::CombinationOperator combination_operator { JBIG2::CombinationOperator::Or }; // "HCOMBOP" in spec.
    bool enable_skip { false };                                                         // "HENABLESKIP" in spec.
    u32 grayscale_width { 0 };                                                          // "HGW" in spec.
    u32 grayscale_height { 0 };                                                         // "HGH" in spec.
    i32 grid_origin_x_offset { 0 };                                                     // "HGX" in spec.
    i32 grid_origin_y_offset { 0 };                                                     // "HGY" in spec.
    u16 grid_vector_x { 0 };                                                            // "HRY" in spec.
    u16 grid_vector_y { 0 };                                                            // "HRX" in spec.
    u8 pattern_width { 0 };                                                             // "HPW" in spec.
    u8 pattern_height { 0 };                                                            // "HPH" in spec.
};

// 6.6 Halftone Region Decoding Procedure
static ErrorOr<NonnullRefPtr<BilevelImage>> halftone_region_decoding_procedure(HalftoneRegionDecodingInputParameters const& inputs, ReadonlyBytes data, Optional<JBIG2::GenericContexts>& contexts)
{
    // 6.6.5 Decoding the halftone region
    // "1) Fill a bitmap HTREG, of the size given by HBW and HBH, with the HDEFPIXEL value."
    auto result = TRY(BilevelImage::create(inputs.region_width, inputs.region_height));
    result->fill(inputs.default_pixel_value);

    // "2) If HENABLESKIP equals 1, compute a bitmap HSKIP as shown in 6.6.5.1."
    Optional<BilevelImage const&> skip_pattern;
    RefPtr<BilevelImage> skip_pattern_storage;
    if (inputs.enable_skip) {
        skip_pattern_storage = TRY(BilevelImage::create(inputs.grayscale_width, inputs.grayscale_height));
        skip_pattern = *skip_pattern_storage;

        // 6.6.5.1 Computing HSKIP
        // "1) For each value of mg between 0 and HGH – 1, beginning from 0, perform the following steps:"
        for (int m_g = 0; m_g < (int)inputs.grayscale_height; ++m_g) {
            // "a) For each value of ng between 0 and HGW – 1, beginning from 0, perform the following steps:"
            for (int n_g = 0; n_g < (int)inputs.grayscale_width; ++n_g) {
                // "i) Set:
                //      x = (HGX + m_g × HRY + n_g × HRX) >> 8
                //      y = (HGY + m_g × HRX – n_g × HRY) >> 8"
                auto x = (inputs.grid_origin_x_offset + m_g * inputs.grid_vector_y + n_g * inputs.grid_vector_x) >> 8;
                auto y = (inputs.grid_origin_y_offset + m_g * inputs.grid_vector_x - n_g * inputs.grid_vector_y) >> 8;

                // "ii) If ((x + HPW <= 0) OR (x >= HBW) OR (y + HPH <= 0) OR (y >= HBH)) then set:
                //          HSKIP[n_g, m_g] = 1
                //      Otherwise, set:
                //          HSKIP[n_g, m_g] = 0"
                if (x + inputs.pattern_width <= 0 || x >= (int)inputs.region_width || y + inputs.pattern_height <= 0 || y >= (int)inputs.region_height)
                    skip_pattern_storage->set_bit(n_g, m_g, true);
                else
                    skip_pattern_storage->set_bit(n_g, m_g, false);
            }
        }
    }

    // "3) Set HBPP to ⌈log2 (HNUMPATS)⌉."
    u32 bits_per_pattern = ceil(log2(inputs.patterns.size()));

    // "4) Decode an image GI of size HGW by HGH with HBPP bits per pixel using the gray-scale image decoding
    //     procedure as described in Annex C. Set the parameters to this decoding procedure as shown in Table 23.
    //     Let GI be the results of invoking this decoding procedure."
    GrayscaleInputParameters grayscale_inputs;
    grayscale_inputs.uses_mmr = inputs.uses_mmr;
    grayscale_inputs.width = inputs.grayscale_width;
    grayscale_inputs.height = inputs.grayscale_height;
    // HBPP is a 32-bit word in Table 22, Table 23 says to copy it to GSBPP, and according to Table C.1 GSBPP is 6 bits.
    if (bits_per_pattern >= 64)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Too many patterns for grayscale image decoding");
    grayscale_inputs.bpp = bits_per_pattern;
    grayscale_inputs.skip_pattern = skip_pattern;
    grayscale_inputs.template_id = inputs.halftone_template;

    Optional<MQArithmeticDecoder> decoder;
    if (!inputs.uses_mmr) {
        decoder = TRY(MQArithmeticDecoder::initialize(data));
        grayscale_inputs.arithmetic_decoder = &decoder.value();
    }

    auto grayscale_image = TRY(grayscale_image_decoding_procedure(grayscale_inputs, data, contexts));

    // "5) Place sequentially the patterns corresponding to the values in GI into HTREG by the procedure described in 6.6.5.2.
    //     The rendering procedure is illustrated in Figure 26. The outline of two patterns are marked by dotted boxes."
    {
        // 6.6.5.2 Rendering the patterns
        // "Draw the patterns into HTREG using the following procedure:
        //  1) For each value of m_g between 0 and HGH – 1, beginning from 0, perform the following steps."
        for (int m_g = 0; m_g < (int)inputs.grayscale_height; ++m_g) {
            // "a) For each value of n_g between 0 and HGW – 1, beginning from 0, perform the following steps."
            for (int n_g = 0; n_g < (int)inputs.grayscale_width; ++n_g) {
                // "i) Set:
                //      x = (HGX + m_g × HRY + n_g × HRX) >> 8
                //      y = (HGY + m_g × HRX – n_g × HRY) >> 8"
                auto x = (inputs.grid_origin_x_offset + m_g * inputs.grid_vector_y + n_g * inputs.grid_vector_x) >> 8;
                auto y = (inputs.grid_origin_y_offset + m_g * inputs.grid_vector_x - n_g * inputs.grid_vector_y) >> 8;

                // "ii) Draw the pattern HPATS[GI[n_g, m_g]] into HTREG such that its upper left pixel is at location (x, y) in HTREG.
                //
                //      A pattern is drawn into HTREG as follows. Each pixel of the pattern shall be combined with
                //      the current value of the corresponding pixel in the halftone-coded bitmap, using the
                //      combination operator specified by HCOMBOP. The results of each combination shall be
                //      written into that pixel in the halftone-coded bitmap.
                //
                //      If any part of a decoded pattern, when placed at location (x, y) lies outside the actual halftone-
                //      coded bitmap, then this part of the pattern shall be ignored in the process of combining the
                //      pattern with the bitmap."
                auto grayscale_value = grayscale_image[n_g + m_g * inputs.grayscale_width];
                if (grayscale_value >= inputs.patterns.size())
                    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Grayscale value out of range");
                auto const& pattern = inputs.patterns[grayscale_value];
                pattern.composite_onto(*result, { x, y }, to_composition_type(inputs.combination_operator));
            }
        }
    }

    // "6) After all the patterns have been placed on the bitmap, the current contents of the halftone-coded bitmap are
    //     the results that shall be obtained by every decoder, whether it performs this exact sequence of steps or not."
    return result;
}

// 6.7.2 Input parameters
// Table 24 – Parameters for the pattern dictionary decoding procedure
struct PatternDictionaryDecodingInputParameters {
    bool uses_mmr { false }; // "HDMMR" in spec.
    u32 width { 0 };         // "HDPW" in spec.
    u32 height { 0 };        // "HDPH" in spec.
    u32 gray_max { 0 };      // "GRAYMAX" in spec.
    u8 hd_template { 0 };    // "HDTEMPLATE" in spec.
};

// 6.7 Pattern Dictionary Decoding Procedure
static ErrorOr<Vector<BilevelSubImage>> pattern_dictionary_decoding_procedure(PatternDictionaryDecodingInputParameters const& inputs, ReadonlyBytes data, Optional<JBIG2::GenericContexts>& contexts)
{
    // Table 27 – Parameters used to decode a pattern dictionary's collective bitmap
    GenericRegionDecodingInputParameters generic_inputs;
    generic_inputs.is_modified_modified_read = inputs.uses_mmr;
    generic_inputs.region_width = (inputs.gray_max + 1) * inputs.width;
    generic_inputs.region_height = inputs.height;
    generic_inputs.gb_template = inputs.hd_template;
    generic_inputs.is_typical_prediction_used = false;
    generic_inputs.is_extended_reference_template_used = false; // Missing from spec in table 27.
    generic_inputs.skip_pattern = OptionalNone {};
    generic_inputs.adaptive_template_pixels[0].x = -inputs.width;
    generic_inputs.adaptive_template_pixels[0].y = 0;
    generic_inputs.adaptive_template_pixels[1].x = -3;
    generic_inputs.adaptive_template_pixels[1].y = -1;
    generic_inputs.adaptive_template_pixels[2].x = 2;
    generic_inputs.adaptive_template_pixels[2].y = -2;
    generic_inputs.adaptive_template_pixels[3].x = -2;
    generic_inputs.adaptive_template_pixels[3].y = -2;

    Optional<FixedMemoryStream> stream;
    Optional<MQArithmeticDecoder> decoder;
    if (inputs.uses_mmr) {
        stream = FixedMemoryStream { data };
        generic_inputs.stream = &stream.value();
    } else {
        decoder = TRY(MQArithmeticDecoder::initialize(data));
        generic_inputs.arithmetic_decoder = &decoder.value();
    }

    auto bitmap = TRY(generic_region_decoding_procedure(generic_inputs, contexts));

    Vector<BilevelSubImage> patterns;
    for (u32 gray = 0; gray <= inputs.gray_max; ++gray) {
        int x = gray * inputs.width;
        auto pattern = bitmap->subbitmap({ x, 0, static_cast<int>(inputs.width), static_cast<int>(inputs.height) });
        patterns.append(move(pattern));
    }

    dbgln_if(JBIG2_DEBUG, "Pattern dictionary: {} patterns", patterns.size());

    return patterns;
}

static ErrorOr<void> decode_symbol_dictionary(JBIG2LoadingContext& context, SegmentData& segment)
{
    // 7.4.2 Symbol dictionary segment syntax

    // Retrieve referred-to symbols and tables. The spec does this later,
    // but having the custom tables available is convenient for collecting huffman tables below.
    Vector<BilevelSubImage> symbols;
    Vector<JBIG2::HuffmanTable const*> custom_tables;
    for (auto const* referred_to_segment : segment.referred_to_segments) {
        dbgln_if(JBIG2_DEBUG, "Symbol segment refers to segment id {}", referred_to_segment->header.segment_number);
        if (referred_to_segment->symbols.has_value())
            symbols.extend(referred_to_segment->symbols.value());
        else if (referred_to_segment->huffman_table.has_value())
            custom_tables.append(&referred_to_segment->huffman_table.value());
        else
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Symbol segment referred-to segment without symbols or huffman table");
    }

    u8 custom_table_index = 0;
    auto custom_table = [&custom_tables, &custom_table_index]() -> ErrorOr<JBIG2::HuffmanTable const*> {
        if (custom_table_index >= custom_tables.size())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Custom Huffman table index out of range");
        return custom_tables[custom_table_index++];
    };

    // 7.4.2.1 Symbol dictionary segment data header
    FixedMemoryStream stream(segment.data);

    // 7.4.2.1.1 Symbol dictionary flags
    u16 flags = TRY(stream.read_value<BigEndian<u16>>());
    bool uses_huffman_encoding = (flags & 1) != 0;               // "SDHUFF" in spec.
    bool uses_refinement_or_aggregate_coding = (flags & 2) != 0; // "SDREFAGG" in spec.

    u8 huffman_table_selection_for_height_differences = (flags >> 2) & 0b11; // "SDHUFFDH" in spec.
    if (!uses_huffman_encoding && huffman_table_selection_for_height_differences != 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid huffman_table_selection_for_height_differences");

    JBIG2::HuffmanTable const* delta_height_table = nullptr;
    if (uses_huffman_encoding) {
        if (huffman_table_selection_for_height_differences == 0)
            delta_height_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_4));
        else if (huffman_table_selection_for_height_differences == 1)
            delta_height_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_5));
        else if (huffman_table_selection_for_height_differences == 2)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid huffman_table_selection_for_height_differences");
        else if (huffman_table_selection_for_height_differences == 3)
            delta_height_table = TRY(custom_table());
    }

    u8 huffman_table_selection_for_width_differences = (flags >> 4) & 0b11; // "SDHUFFDW" in spec.
    if (!uses_huffman_encoding && huffman_table_selection_for_width_differences != 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid huffman_table_selection_for_width_differences");

    JBIG2::HuffmanTable const* delta_width_table = nullptr;
    if (uses_huffman_encoding) {
        if (huffman_table_selection_for_width_differences == 0)
            delta_width_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_2));
        else if (huffman_table_selection_for_width_differences == 1)
            delta_width_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_3));
        else if (huffman_table_selection_for_width_differences == 2)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid huffman_table_selection_for_height_differences");
        else if (huffman_table_selection_for_width_differences == 3)
            delta_width_table = TRY(custom_table());
    }

    bool uses_user_supplied_size_table = (flags >> 6) & 1; // "SDHUFFBMSIZE" in spec.
    if (!uses_huffman_encoding && uses_user_supplied_size_table)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid uses_user_supplied_size_table");

    JBIG2::HuffmanTable const* bitmap_size_table = nullptr;
    if (uses_huffman_encoding) {
        if (!uses_user_supplied_size_table)
            bitmap_size_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_1));
        else
            bitmap_size_table = TRY(custom_table());
    }

    bool uses_user_supplied_aggregate_table = (flags >> 7) & 1; // "SDHUFFAGGINST" in spec.
    if (!uses_huffman_encoding && uses_user_supplied_aggregate_table)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid uses_user_supplied_aggregate_table");

    JBIG2::HuffmanTable const* number_of_symbol_instances_table = nullptr;
    if (uses_huffman_encoding) {
        if (!uses_user_supplied_aggregate_table)
            number_of_symbol_instances_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_1));
        else
            number_of_symbol_instances_table = TRY(custom_table());
    }

    if (custom_table_index != custom_tables.size())
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Not all referred custom tables used");

    if (uses_huffman_encoding) {
        if (!delta_width_table->has_oob_symbol())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Custom SDHUFFDW table must have OOB symbol");

        if (delta_height_table->has_oob_symbol()
            || bitmap_size_table->has_oob_symbol()
            || number_of_symbol_instances_table->has_oob_symbol()) {
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Custom Huffman tables must not have OOB symbol");
        }
    }

    bool bitmap_coding_context_used = (flags >> 8) & 1;
    if (uses_huffman_encoding && !uses_refinement_or_aggregate_coding && bitmap_coding_context_used)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid bitmap_coding_context_used");

    bool bitmap_coding_context_retained = (flags >> 9) & 1;
    if (uses_huffman_encoding && !uses_refinement_or_aggregate_coding && bitmap_coding_context_retained)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid bitmap_coding_context_retained");

    u8 template_used = (flags >> 10) & 0b11; // "SDTEMPLATE" in spec.
    if (uses_huffman_encoding && template_used != 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid template_used");

    u8 refinement_template_used = (flags >> 12) & 1; // "SDREFTEMPLATE" in spec.

    // Quirk: 042_22.jb2 does not set SDREFAGG but it does set SDREFTEMPLATE.
    if (!uses_refinement_or_aggregate_coding && refinement_template_used != 0 && !context.is_power_jbig2_file)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid refinement_template_used");

    if (flags & 0b1110'0000'0000'0000)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid symbol dictionary flags");

    // 7.4.2.1.2 Symbol dictionary AT flags
    Array<JBIG2::AdaptiveTemplatePixel, 4> adaptive_template {};
    if (!uses_huffman_encoding) {
        int number_of_adaptive_template_pixels = template_used == 0 ? 4 : 1;
        for (int i = 0; i < number_of_adaptive_template_pixels; ++i) {
            adaptive_template[i].x = TRY(stream.read_value<i8>());
            adaptive_template[i].y = TRY(stream.read_value<i8>());
        }
    }

    // 7.4.2.1.3 Symbol dictionary refinement AT flags
    Array<JBIG2::AdaptiveTemplatePixel, 2> adaptive_refinement_template {};
    if (uses_refinement_or_aggregate_coding && refinement_template_used == 0) {
        for (size_t i = 0; i < adaptive_refinement_template.size(); ++i) {
            adaptive_refinement_template[i].x = TRY(stream.read_value<i8>());
            adaptive_refinement_template[i].y = TRY(stream.read_value<i8>());
        }
    }

    // 7.4.2.1.4 Number of exported symbols (SDNUMEXSYMS)
    u32 number_of_exported_symbols = TRY(stream.read_value<BigEndian<u32>>());

    // 7.4.2.1.5 Number of new symbols (SDNUMNEWSYMS)
    u32 number_of_new_symbols = TRY(stream.read_value<BigEndian<u32>>());

    dbgln_if(JBIG2_DEBUG, "Symbol dictionary: uses_huffman_encoding={}", uses_huffman_encoding);
    dbgln_if(JBIG2_DEBUG, "Symbol dictionary: uses_refinement_or_aggregate_coding={}", uses_refinement_or_aggregate_coding);
    dbgln_if(JBIG2_DEBUG, "Symbol dictionary: huffman_table_selection_for_height_differences={}", huffman_table_selection_for_height_differences);
    dbgln_if(JBIG2_DEBUG, "Symbol dictionary: huffman_table_selection_for_width_differences={}", huffman_table_selection_for_width_differences);
    dbgln_if(JBIG2_DEBUG, "Symbol dictionary: uses_user_supplied_size_table={}", uses_user_supplied_size_table);
    dbgln_if(JBIG2_DEBUG, "Symbol dictionary: uses_user_supplied_aggregate_table={}", uses_user_supplied_aggregate_table);
    dbgln_if(JBIG2_DEBUG, "Symbol dictionary: bitmap_coding_context_used={}", bitmap_coding_context_used);
    dbgln_if(JBIG2_DEBUG, "Symbol dictionary: bitmap_coding_context_retained={}", bitmap_coding_context_retained);
    dbgln_if(JBIG2_DEBUG, "Symbol dictionary: template_used={}", template_used);
    dbgln_if(JBIG2_DEBUG, "Symbol dictionary: refinement_template_used={}", refinement_template_used);
    dbgln_if(JBIG2_DEBUG, "Symbol dictionary: number_of_exported_symbols={}", number_of_exported_symbols);
    dbgln_if(JBIG2_DEBUG, "Symbol dictionary: number_of_new_symbols={}", number_of_new_symbols);

    // 7.4.2.1.6 Symbol dictionary segment Huffman table selection
    // We currently do this as part of handling 7.4.2.1.1 a bit further up.

    // 7.4.2.2 Decoding a symbol dictionary segment
    // "1) Interpret its header, as described in 7.4.2.1."
    // Done!

    // "2) Decode (or retrieve the results of decoding) any referred-to symbol dictionary and tables segments."
    // Done further up already.

    // "3) If the "bitmap coding context used" bit in the header was 1, ..."
    if (bitmap_coding_context_used)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode bitmap coding context segment yet");

    // "4) If the "bitmap coding context used" bit in the header was 0, then, as described in E.3.7,
    //     reset all the arithmetic coding statistics for the generic region and generic refinement region decoding procedures to zero."
    // Nothing to do.

    // "5) Reset the arithmetic coding statistics for all the contexts of all the arithmetic integer coders to zero."
    // We currently do this by keeping the statistics as locals in symbol_dictionary_decoding_procedure().

    // "6) Invoke the symbol dictionary decoding procedure described in 6.5, with the parameters to the symbol dictionary decoding procedure set as shown in Table 31."
    SymbolDictionaryDecodingInputParameters inputs;
    inputs.uses_huffman_encoding = uses_huffman_encoding;
    inputs.uses_refinement_or_aggregate_coding = uses_refinement_or_aggregate_coding;
    inputs.input_symbols = move(symbols);
    inputs.number_of_new_symbols = number_of_new_symbols;
    inputs.number_of_exported_symbols = number_of_exported_symbols;
    inputs.delta_height_table = delta_height_table;
    inputs.delta_width_table = delta_width_table;
    inputs.bitmap_size_table = bitmap_size_table;
    inputs.number_of_symbol_instances_table = number_of_symbol_instances_table;
    inputs.symbol_template = template_used;
    inputs.adaptive_template_pixels = adaptive_template;
    inputs.refinement_template = refinement_template_used;
    inputs.refinement_adaptive_template_pixels = adaptive_refinement_template;
    auto result = TRY(symbol_dictionary_decoding_procedure(inputs, segment.data.slice(TRY(stream.tell()))));

    // "7) If the "bitmap coding context retained" bit in the header was 1, then, as described in E.3.8, preserve the current contents
    //     of the arithmetic coding statistics for the generic region and generic refinement region decoding procedures."
    if (bitmap_coding_context_retained)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot retain bitmap coding context yet");

    segment.symbols = move(result);

    return {};
}

struct RegionResult {
    JBIG2::RegionSegmentInformationField information_field;
    NonnullRefPtr<BilevelImage> bitmap;
};

static void handle_immediate_direct_region(JBIG2LoadingContext& context, RegionResult const& result)
{
    // 8.2 Page image composition, 5a.
    result.bitmap->composite_onto(
        *context.page.bits,
        { result.information_field.x_location, result.information_field.y_location },
        to_composition_type(result.information_field.external_combination_operator()));
}

static ErrorOr<void> handle_intermediate_direct_region(JBIG2LoadingContext&, SegmentData& segment, RegionResult& result)
{
    // 8.2 Page image composition, 5b.
    VERIFY(result.bitmap->width() == result.information_field.width);
    VERIFY(result.bitmap->height() == result.information_field.height);
    segment.aux_buffer = move(result.bitmap);
    segment.aux_buffer_information_field = result.information_field;
    return {};
}

static ErrorOr<Vector<u32>> assign_huffman_codes(ReadonlyBytes code_lengths)
{
    // FIXME: Use shared huffman code, instead of using this algorithm from the spec.

    // B.3 Assigning the prefix codes
    // code_lengths is "PREFLEN" in spec, code_lengths.size is "NTEMP".
    Vector<u32> codes; // "CODES" in spec.
    TRY(codes.try_resize(code_lengths.size()));

    // "1) Build a histogram in the array LENCOUNT counting the number of times each prefix length value
    //     occurs in PREFLEN: LENCOUNT[I] is the number of times that the value I occurs in the array
    //     PREFLEN."
    Array<u32, 32> length_counts {}; // "LENCOUNT" in spec.
    for (auto length : code_lengths) {
        VERIFY(length < 32);
        length_counts[length]++;
    }

    // "2) Let LENMAX be the largest value for which LENCOUNT[LENMAX] > 0. Set:
    //         CURLEN = 1
    //         FIRSTCODE[0] = 0
    //         LENCOUNT[0] = 0"
    size_t highest_length_index = 0; // "LENMAX" in spec.
    for (auto const& [i, count] : enumerate(length_counts)) {
        if (count > 0)
            highest_length_index = i;
    }
    size_t current_length = 1;           // "CURLEN" in spec.
    Array<u32, 32> first_code_at_length; // "FIRSTCODE" in spec.
    first_code_at_length[0] = 0;
    length_counts[0] = 0;

    // "3) While CURLEN ≤ LENMAX, perform the following operations:"
    while (current_length <= highest_length_index) {
        // "a) Set:
        //         FIRSTCODE[CURLEN] = (FIRSTCODE[CURLEN – 1] + LENCOUNT[CURLEN – 1]) × 2
        //         CURCODE = FIRSTCODE[CURLEN]
        //         CURTEMP = 0"
        first_code_at_length[current_length] = (first_code_at_length[current_length - 1] + length_counts[current_length - 1]) * 2;
        u32 current_code = first_code_at_length[current_length]; // "CURCODE" in spec.
        size_t i = 0;                                            // "CURTEMP" in spec.

        // "b) While CURTEMP < NTEMP, perform the following operations:"
        while (i < code_lengths.size()) {
            // "i) If PREFLEN[CURTEMP] = CURLEN, then set:
            //         CODES[CURTEMP] = CURCODE
            //         CURCODE = CURCODE + 1"
            if (code_lengths[i] == current_length) {
                codes[i] = current_code;
                current_code++;
            }

            // "ii) Set CURTEMP = CURTEMP + 1"
            i++;
        }

        // "c) Set:
        //         CURLEN = CURLEN + 1"
        current_length++;
    }

    return codes;
}

static ErrorOr<RegionResult> decode_text_region(JBIG2LoadingContext& context, SegmentData const& segment)
{
    // 7.4.3 Text region segment syntax
    auto data = segment.data;
    auto information_field = TRY(decode_region_segment_information_field(data));
    data = data.slice(sizeof(information_field));

    dbgln_if(JBIG2_DEBUG, "Text region: width={}, height={}, x={}, y={}, flags={:#x}", information_field.width, information_field.height, information_field.x_location, information_field.y_location, information_field.flags);
    TRY(validate_segment_combination_operator_consistency(context, information_field));

    FixedMemoryStream stream(data);

    // 7.4.3.1.1 Text region segment flags
    u16 text_region_segment_flags = TRY(stream.read_value<BigEndian<u16>>());
    bool uses_huffman_encoding = (text_region_segment_flags & 1) != 0;  // "SBHUFF" in spec.
    bool uses_refinement_coding = (text_region_segment_flags >> 1) & 1; // "SBREFINE" in spec.
    u8 log_strip_size = (text_region_segment_flags >> 2) & 3;           // "LOGSBSTRIPS" in spec.
    u8 strip_size = 1u << log_strip_size;
    u8 reference_corner = (text_region_segment_flags >> 4) & 3;     // "REFCORNER"
    bool is_transposed = (text_region_segment_flags >> 6) & 1;      // "TRANSPOSED" in spec.
    u8 combination_operator = (text_region_segment_flags >> 7) & 3; // "SBCOMBOP" in spec.
    if (combination_operator > 4)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid text region combination operator");

    u8 default_pixel_value = (text_region_segment_flags >> 9) & 1; // "SBDEFPIXEL" in spec.

    u8 delta_s_offset_value = (text_region_segment_flags >> 10) & 0x1f; // "SBDSOFFSET" in spec.
    i8 delta_s_offset = AK::sign_extend(delta_s_offset_value, 5);

    u8 refinement_template = (text_region_segment_flags >> 15) != 0; // "SBRTEMPLATE" in spec.
    if (!uses_refinement_coding && refinement_template != 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid refinement_template");

    // Retrieve referred-to symbols and tables. The spec does this later, but the number of symbols is needed to decode the symbol ID Huffman table,
    // and having the custom tables available is convenient for handling 7.4.3.1.2 below.
    Vector<BilevelSubImage> symbols; // `symbols.size()` is "SBNUMSYMS" in spec.
    Vector<JBIG2::HuffmanTable const*> custom_tables;
    for (auto const* referred_to_segment : segment.referred_to_segments) {
        dbgln_if(JBIG2_DEBUG, "Text segment refers to segment id {}", referred_to_segment->header.segment_number);
        if (referred_to_segment->symbols.has_value())
            symbols.extend(referred_to_segment->symbols.value());
        else if (referred_to_segment->huffman_table.has_value())
            custom_tables.append(&referred_to_segment->huffman_table.value());
        else
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Text segment referred-to segment without symbols or huffman table");
    }

    // 7.4.3.1.2 Text region segment Huffman flags
    // "This field is only present if SBHUFF is 1."
    JBIG2::HuffmanTable const* first_s_table = nullptr;
    JBIG2::HuffmanTable const* subsequent_s_table = nullptr;
    JBIG2::HuffmanTable const* delta_t_table = nullptr;
    JBIG2::HuffmanTable const* refinement_delta_width_table = nullptr;
    JBIG2::HuffmanTable const* refinement_delta_height_table = nullptr;
    JBIG2::HuffmanTable const* refinement_x_offset_table = nullptr;
    JBIG2::HuffmanTable const* refinement_y_offset_table = nullptr;
    JBIG2::HuffmanTable const* refinement_size_table = nullptr;
    if (uses_huffman_encoding) {
        u16 huffman_flags = TRY(stream.read_value<BigEndian<u16>>());

        u8 custom_table_index = 0;
        auto custom_table = [&custom_tables, &custom_table_index]() -> ErrorOr<JBIG2::HuffmanTable const*> {
            if (custom_table_index >= custom_tables.size())
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: Custom Huffman table index out of range");
            return custom_tables[custom_table_index++];
        };

        auto first_s_selection = (huffman_flags >> 0) & 0b11; // "SBHUFFFS" in spec.
        if (first_s_selection == 0)
            first_s_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_6));
        else if (first_s_selection == 1)
            first_s_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_7));
        else if (first_s_selection == 2)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid first_s_table");
        else if (first_s_selection == 3)
            first_s_table = TRY(custom_table());

        auto subsequent_s_selection = (huffman_flags >> 2) & 0b11; // "SBHUFFDS" in spec.
        if (subsequent_s_selection == 0)
            subsequent_s_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_8));
        else if (subsequent_s_selection == 1)
            subsequent_s_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_9));
        else if (subsequent_s_selection == 2)
            subsequent_s_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_10));
        else if (subsequent_s_selection == 3)
            subsequent_s_table = TRY(custom_table());

        auto delta_t_selection = (huffman_flags >> 4) & 0b11; // "SBHUFFDT" in spec.
        if (delta_t_selection == 0)
            delta_t_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_11));
        else if (delta_t_selection == 1)
            delta_t_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_12));
        else if (delta_t_selection == 2)
            delta_t_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_13));
        else if (delta_t_selection == 3)
            delta_t_table = TRY(custom_table());

        // Quirk: 042_11.jb2 has refinement huffman table bits set but the SBREFINE bit is not set.
        if (!uses_refinement_coding && (huffman_flags & 0x7fc0) != 0 && !context.is_power_jbig2_file)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Huffman flags have refinement bits set but refinement bit is not set");

        auto refinement_delta_width_selection = (huffman_flags >> 6) & 0b11; // "SBHUFFRDW" in spec.
        if (refinement_delta_width_selection == 0)
            refinement_delta_width_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_14));
        else if (refinement_delta_width_selection == 1)
            refinement_delta_width_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_15));
        else if (refinement_delta_width_selection == 2)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid refinement_delta_width_table");
        else if (refinement_delta_width_selection == 3)
            refinement_delta_width_table = TRY(custom_table());

        auto refinement_delta_height_selection = (huffman_flags >> 8) & 0b11; // "SBHUFFRDH" in spec.
        if (refinement_delta_height_selection == 0)
            refinement_delta_height_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_14));
        else if (refinement_delta_height_selection == 1)
            refinement_delta_height_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_15));
        else if (refinement_delta_height_selection == 2)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid refinement_delta_height_table");
        else if (refinement_delta_height_selection == 3)
            refinement_delta_height_table = TRY(custom_table());

        auto refinement_x_offset_selection = (huffman_flags >> 10) & 0b11; // "SBHUFFRDX" in spec.
        if (refinement_x_offset_selection == 0)
            refinement_x_offset_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_14));
        else if (refinement_x_offset_selection == 1)
            refinement_x_offset_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_15));
        else if (refinement_x_offset_selection == 2)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid refinement_x_offset_table");
        else if (refinement_x_offset_selection == 3)
            refinement_x_offset_table = TRY(custom_table());

        auto refinement_y_offset_selection = (huffman_flags >> 12) & 0b11; // "SBHUFFRDY" in spec.
        if (refinement_y_offset_selection == 0)
            refinement_y_offset_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_14));
        else if (refinement_y_offset_selection == 1)
            refinement_y_offset_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_15));
        else if (refinement_y_offset_selection == 2)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid refinement_y_offset_table");
        else if (refinement_y_offset_selection == 3)
            refinement_y_offset_table = TRY(custom_table());

        auto refinement_size_selection = (huffman_flags >> 14) & 0b1; // "SBHUFFRSIZE" in spec.
        if (refinement_size_selection == 0)
            refinement_size_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_1));
        else if (refinement_size_selection == 1)
            refinement_size_table = TRY(custom_table());

        if (custom_table_index != custom_tables.size())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Not all referred custom tables used");

        if (!subsequent_s_table->has_oob_symbol())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Custom SBHUFFDS table must have OOB symbol");

        if (first_s_table->has_oob_symbol()
            || delta_t_table->has_oob_symbol()
            || refinement_delta_width_table->has_oob_symbol()
            || refinement_delta_height_table->has_oob_symbol()
            || refinement_x_offset_table->has_oob_symbol()
            || refinement_y_offset_table->has_oob_symbol()
            || refinement_size_table->has_oob_symbol()) {
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Custom Huffman tables must not have OOB symbol");
        }

        if (huffman_flags & 0x8000)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid text region segment Huffman flags");
    }

    // 7.4.3.1.3 Text region refinement AT flags
    // "This field is only present if SBREFINE is 1 and SBRTEMPLATE is 0."
    Array<JBIG2::AdaptiveTemplatePixel, 2> adaptive_refinement_template {};
    if (uses_refinement_coding && refinement_template == 0) {
        for (size_t i = 0; i < adaptive_refinement_template.size(); ++i) {
            adaptive_refinement_template[i].x = TRY(stream.read_value<i8>());
            adaptive_refinement_template[i].y = TRY(stream.read_value<i8>());
        }
    }

    // 7.4.3.1.4 Number of symbol instances (SBNUMINSTANCES)
    u32 number_of_symbol_instances = TRY(stream.read_value<BigEndian<u32>>());

    // 7.4.3.1.5 Text region segment symbol ID Huffman decoding table
    // "It is only present if SBHUFF is 1."
    Vector<JBIG2::Code> symbol_id_codes;
    Optional<JBIG2::HuffmanTable> symbol_id_table_storage;
    JBIG2::HuffmanTable const* symbol_id_table = nullptr;
    if (uses_huffman_encoding) {
        // 7.4.3.1.7 Symbol ID Huffman table decoding
        auto bit_stream = BigEndianInputBitStream { MaybeOwned { stream } };

        // "1) Read the code lengths for RUNCODE0 through RUNCODE34; each is stored as a four-bit value."
        Array<u8, 35> code_length_lengths {};
        for (size_t i = 0; i < code_length_lengths.size(); ++i)
            code_length_lengths[i] = TRY(bit_stream.read_bits<u8>(4));

        // "2) Given the lengths, assign Huffman codes for RUNCODE0 through RUNCODE34 using the algorithm
        //     in B.3."
        auto code_length_codes = TRY(assign_huffman_codes(code_length_lengths));

        Vector<JBIG2::Code, 35> code_lengths_entries;
        for (auto const& [i, length] : enumerate(code_length_lengths)) {
            if (length == 0)
                continue;
            JBIG2::Code code { .prefix_length = length, .range_length = 0, .first_value = i, .code = code_length_codes[i] };
            code_lengths_entries.append(code);
        }
        JBIG2::HuffmanTable code_lengths_table { code_lengths_entries };

        Vector<u8> code_lengths;
        do {
            // "3) Read a Huffman code using this assignment. This decodes into one of RUNCODE0 through
            //     RUNCODE34. If it is RUNCODE32, read two additional bits. If it is RUNCODE33, read three
            //     additional bits. If it is RUNCODE34, read seven additional bits."
            auto code = TRY(code_lengths_table.read_symbol_non_oob(bit_stream));
            u8 repeats = 0;
            if (code == 32)
                repeats = TRY(bit_stream.read_bits<u8>(2)) + 3;
            else if (code == 33)
                repeats = TRY(bit_stream.read_bits<u8>(3)) + 3;
            else if (code == 34)
                repeats = TRY(bit_stream.read_bits<u8>(7)) + 11;

            // "4) Interpret the RUNCODE code and the additional bits (if any) according to Table 29. This gives the
            //     symbol ID code lengths for one or more symbols."
            // Note: The spec means "Table 32" here.
            if (code < 32) {
                code_lengths.append(code);
            } else if (code == 32) {
                if (code_lengths.is_empty())
                    return Error::from_string_literal("JBIG2ImageDecoderPlugin: RUNCODE32 without previous code");
                auto last_value = code_lengths.last();
                for (size_t i = 0; i < repeats; ++i)
                    code_lengths.append(last_value);
            } else if (code == 33 || code == 34) {
                for (size_t i = 0; i < repeats; ++i)
                    code_lengths.append(0);
            }

            // "5) Repeat steps 3) and 4) until the symbol ID code lengths for all SBNUMSYMS symbols have been
            //     determined."
        } while (code_lengths.size() < symbols.size());

        // "6) Skip over the remaining bits in the last byte read, so that the actual text region decoding procedure begins
        //     on a byte boundary."
        // Done automatically by the BigEndianInputBitStream wrapping `stream`.

        // "7) Assign a Huffman code to each symbol by applying the algorithm in B.3 to the symbol ID code lengths
        //     just decoded. The result is the symbol ID Huffman table SBSYMCODES."
        auto codes = TRY(assign_huffman_codes(code_lengths));
        for (auto const& [i, length] : enumerate(code_lengths)) {
            if (length == 0)
                continue;
            JBIG2::Code code { .prefix_length = length, .range_length = 0, .first_value = i, .code = codes[i] };
            symbol_id_codes.append(code);
        }
        symbol_id_table_storage = JBIG2::HuffmanTable { symbol_id_codes };
        symbol_id_table = &symbol_id_table_storage.value();
    }

    dbgln_if(JBIG2_DEBUG, "Text region: uses_huffman_encoding={}, uses_refinement_coding={}, strip_size={}, reference_corner={}, is_transposed={}", uses_huffman_encoding, uses_refinement_coding, strip_size, reference_corner, is_transposed);
    dbgln_if(JBIG2_DEBUG, "Text region: combination_operator={}, default_pixel_value={}, delta_s_offset={}, refinement_template={}", combination_operator, default_pixel_value, delta_s_offset, refinement_template);
    dbgln_if(JBIG2_DEBUG, "Text region: number_of_symbol_instances={}", number_of_symbol_instances);

    // 7.4.3.2 Decoding a text region segment
    // "1) Interpret its header, as described in 7.4.3.1."
    // Done!

    // "2) Decode (or retrieve the results of decoding) any referred-to symbol dictionary and tables segments."
    // Done further up, since it's needed to decode the symbol ID Huffman table already.

    // "3) As described in E.3.7, reset all the arithmetic coding statistics to zero."
    u32 id_symbol_code_length = ceil(log2(symbols.size()));
    Optional<TextContexts> text_contexts;
    if (!uses_huffman_encoding)
        text_contexts = TextContexts { id_symbol_code_length };
    Optional<RefinementContexts> refinement_contexts;
    if (uses_refinement_coding)
        refinement_contexts = RefinementContexts { refinement_template };

    // "4) Invoke the text region decoding procedure described in 6.4, with the parameters to the text region decoding procedure set as shown in Table 34."
    TextRegionDecodingInputParameters inputs;
    inputs.uses_huffman_encoding = uses_huffman_encoding;
    inputs.uses_refinement_coding = uses_refinement_coding;
    inputs.default_pixel = default_pixel_value;
    inputs.operator_ = static_cast<JBIG2::CombinationOperator>(combination_operator);
    inputs.is_transposed = is_transposed;
    inputs.reference_corner = static_cast<JBIG2::ReferenceCorner>(reference_corner);
    inputs.delta_s_offset = delta_s_offset;
    inputs.region_width = information_field.width;
    inputs.region_height = information_field.height;
    inputs.number_of_instances = number_of_symbol_instances;
    inputs.size_of_symbol_instance_strips = strip_size;
    inputs.symbol_id_table = symbol_id_table;
    inputs.id_symbol_code_length = id_symbol_code_length;
    inputs.symbols = move(symbols);
    inputs.first_s_table = first_s_table;
    inputs.subsequent_s_table = subsequent_s_table;
    inputs.delta_t_table = delta_t_table;
    inputs.refinement_delta_width_table = refinement_delta_width_table;
    inputs.refinement_delta_height_table = refinement_delta_height_table;
    inputs.refinement_x_offset_table = refinement_x_offset_table;
    inputs.refinement_y_offset_table = refinement_y_offset_table;
    inputs.refinement_size_table = refinement_size_table;
    inputs.refinement_template = refinement_template;
    inputs.refinement_adaptive_template_pixels = adaptive_refinement_template;

    Optional<MQArithmeticDecoder> decoder;
    if (uses_huffman_encoding) {
        inputs.stream = &stream;
    } else {
        decoder = TRY(MQArithmeticDecoder::initialize(data.slice(TRY(stream.tell()))));
        inputs.arithmetic_decoder = &decoder.value();
    }

    auto result = TRY(text_region_decoding_procedure(inputs, text_contexts, refinement_contexts));
    return RegionResult { .information_field = information_field, .bitmap = move(result) };
}

static ErrorOr<void> decode_intermediate_text_region(JBIG2LoadingContext& context, SegmentData& segment)
{
    auto result = TRY(decode_text_region(context, segment));
    return handle_intermediate_direct_region(context, segment, result);
}

static ErrorOr<void> decode_immediate_text_region(JBIG2LoadingContext& context, SegmentData const& segment)
{
    auto result = TRY(decode_text_region(context, segment));
    handle_immediate_direct_region(context, result);
    return {};
}

static ErrorOr<void> decode_immediate_lossless_text_region(JBIG2LoadingContext& context, SegmentData const& segment)
{
    // 7.4.3 Text region segment syntax
    // "The data parts of all three of the text region segment types ("intermediate text region", "immediate text region" and
    //  "immediate lossless text region") are coded identically, but are acted upon differently, see 8.2."
    // But 8.2 only describes a difference between intermediate and immediate regions as far as I can tell,
    // and calling the immediate text region handler for immediate lossless text regions seems to do the right thing (?).
    return decode_immediate_text_region(context, segment);
}

static ErrorOr<void> decode_pattern_dictionary(JBIG2LoadingContext&, SegmentData& segment)
{
    // 7.4.4 Pattern dictionary segment syntax
    FixedMemoryStream stream(segment.data);

    // 7.4.4.1.1 Pattern dictionary flags
    u8 flags = TRY(stream.read_value<u8>());
    bool uses_mmr = flags & 1;
    u8 hd_template = (flags >> 1) & 3;
    if (uses_mmr && hd_template != 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid hd_template");
    if (flags & 0b1111'1000)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid flags");

    // 7.4.4.1.2 Width of the patterns in the pattern dictionary (HDPW)
    u8 width = TRY(stream.read_value<u8>());
    if (width == 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid width");

    // 7.4.4.1.3 Height of the patterns in the pattern dictionary (HDPH)
    u8 height = TRY(stream.read_value<u8>());
    if (height == 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid height");

    // 7.4.4.1.4 Largest gray-scale value (GRAYMAX)
    u32 gray_max = TRY(stream.read_value<BigEndian<u32>>());

    // 7.4.4.2 Decoding a pattern dictionary segment
    dbgln_if(JBIG2_DEBUG, "Pattern dictionary: uses_mmr={}, hd_template={}, width={}, height={}, gray_max={}", uses_mmr, hd_template, width, height, gray_max);
    auto data = segment.data.slice(TRY(stream.tell()));

    // "1) Interpret its header, as described in 7.4.4.1."
    // Done!

    // "2) As described in E.3.7, reset all the arithmetic coding statistics to zero."
    Optional<JBIG2::GenericContexts> contexts;
    if (!uses_mmr)
        contexts = JBIG2::GenericContexts { hd_template };

    // "3) Invoke the pattern dictionary decoding procedure described in 6.7, with the parameters to the pattern
    //     dictionary decoding procedure set as shown in Table 35."
    PatternDictionaryDecodingInputParameters inputs;
    inputs.uses_mmr = uses_mmr;
    inputs.width = width;
    inputs.height = height;
    inputs.gray_max = gray_max;
    inputs.hd_template = hd_template;
    auto result = TRY(pattern_dictionary_decoding_procedure(inputs, data, contexts));

    segment.patterns = move(result);

    return {};
}

static ErrorOr<RegionResult> decode_halftone_region(JBIG2LoadingContext& context, SegmentData const& segment)
{
    // 7.4.5 Halftone region segment syntax
    auto data = segment.data;
    auto information_field = TRY(decode_region_segment_information_field(data));
    data = data.slice(sizeof(information_field));

    dbgln_if(JBIG2_DEBUG, "Halftone region: width={}, height={}, x={}, y={}, flags={:#x}", information_field.width, information_field.height, information_field.x_location, information_field.y_location, information_field.flags);
    TRY(validate_segment_combination_operator_consistency(context, information_field));

    FixedMemoryStream stream(data);

    // 7.4.5.1.1 Halftone region segment flags
    u8 flags = TRY(stream.read_value<u8>());
    bool uses_mmr = flags & 1;           // "HMMR" in spec.
    u8 template_used = (flags >> 1) & 3; // "HTTEMPLATE" in spec.
    if (uses_mmr && template_used != 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid template_used");
    bool enable_skip = (flags >> 3) & 1;        // "HENABLESKIP" in spec.
    u8 combination_operator = (flags >> 4) & 7; // "HCOMBOP" in spec.
    if (combination_operator > 4)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid combination_operator");
    bool default_pixel_value = (flags >> 7) & 1; // "HDEFPIXEL" in spec.

    dbgln_if(JBIG2_DEBUG, "Halftone region: uses_mmr={}, template_used={}, enable_skip={}, combination_operator={}, default_pixel_value={}", uses_mmr, template_used, enable_skip, combination_operator, default_pixel_value);

    // 7.4.5.1.2 Halftone grid position and size
    // 7.4.5.1.2.1 Width of the gray-scale image (HGW)
    u32 gray_width = TRY(stream.read_value<BigEndian<u32>>());

    // 7.4.5.1.2.2 Height of the gray-scale image (HGH)
    u32 gray_height = TRY(stream.read_value<BigEndian<u32>>());

    // 7.4.5.1.2.3 Horizontal offset of the grid (HGX)
    i32 grid_x = TRY(stream.read_value<BigEndian<i32>>());

    // 7.4.5.1.2.4 Vertical offset of the grid (HGY)
    i32 grid_y = TRY(stream.read_value<BigEndian<i32>>());

    // 7.4.5.1.3 Halftone grid vector
    // 7.4.5.1.3.1 Horizontal coordinate of the halftone grid vector (HRX)
    u16 grid_vector_x = TRY(stream.read_value<BigEndian<u16>>());

    // 7.4.5.1.3.2 Vertical coordinate of the halftone grid vector (HRY)
    u16 grid_vector_y = TRY(stream.read_value<BigEndian<u16>>());

    dbgln_if(JBIG2_DEBUG, "Halftone region: gray_width={}, gray_height={}, grid_x={}, grid_y={}, grid_vector_x={}, grid_vector_y={}", gray_width, gray_height, grid_x, grid_y, grid_vector_x, grid_vector_y);

    // 7.4.5.2 Decoding a halftone region segment
    // "1) Interpret its header, as described in 7.4.5.1."
    // Done!

    // "2) Decode (or retrieve the results of decoding) the referred-to pattern dictionary segment."
    VERIFY(segment.referred_to_segments.size() == 1);
    dbgln_if(JBIG2_DEBUG, "Halftone segment refers to segment id {}", segment.referred_to_segments[0]->header.segment_number);
    Vector<BilevelSubImage> patterns = segment.referred_to_segments[0]->patterns.value();
    if (patterns.is_empty())
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Halftone segment without patterns");

    // "3) As described in E.3.7, reset all the arithmetic coding statistics to zero."
    Optional<JBIG2::GenericContexts> contexts;
    if (!uses_mmr)
        contexts = JBIG2::GenericContexts { template_used };

    // "4) Invoke the halftone region decoding procedure described in 6.6, with the parameters to the halftone
    //     region decoding procedure set as shown in Table 36."
    data = data.slice(TRY(stream.tell()));
    HalftoneRegionDecodingInputParameters inputs;
    inputs.region_width = information_field.width;
    inputs.region_height = information_field.height;
    inputs.uses_mmr = uses_mmr;
    inputs.halftone_template = template_used;
    inputs.enable_skip = enable_skip;
    inputs.combination_operator = static_cast<JBIG2::CombinationOperator>(combination_operator);
    inputs.default_pixel_value = default_pixel_value;
    inputs.grayscale_width = gray_width;
    inputs.grayscale_height = gray_height;
    inputs.grid_origin_x_offset = grid_x;
    inputs.grid_origin_y_offset = grid_y;
    inputs.grid_vector_x = grid_vector_x;
    inputs.grid_vector_y = grid_vector_y;
    inputs.patterns = move(patterns);
    inputs.pattern_width = inputs.patterns[0].width();
    inputs.pattern_height = inputs.patterns[0].height();
    auto result = TRY(halftone_region_decoding_procedure(inputs, data, contexts));

    return RegionResult { .information_field = information_field, .bitmap = move(result) };
}

static ErrorOr<void> decode_intermediate_halftone_region(JBIG2LoadingContext& context, SegmentData& segment)
{
    auto result = TRY(decode_halftone_region(context, segment));
    return handle_intermediate_direct_region(context, segment, result);
}

static ErrorOr<void> decode_immediate_halftone_region(JBIG2LoadingContext& context, SegmentData const& segment)
{
    auto result = TRY(decode_halftone_region(context, segment));
    handle_immediate_direct_region(context, result);
    return {};
}

static ErrorOr<void> decode_immediate_lossless_halftone_region(JBIG2LoadingContext& context, SegmentData const& segment)
{
    // 7.4.5 Halftone region segment syntax
    // "The data parts of all three of the halftone region segment types ("intermediate halftone region", "immediate halftone
    //  region" and "immediate lossless halftone region") are coded identically, but are acted upon differently, see 8.2."
    // But 8.2 only describes a difference between intermediate and immediate regions as far as I can tell,
    // and calling the immediate halftone region handler for immediate lossless halftone regions seems to do the right thing (?).
    return decode_immediate_halftone_region(context, segment);
}

static ErrorOr<RegionResult> decode_generic_region(JBIG2LoadingContext& context, SegmentData const& segment)
{
    // 7.4.6 Generic region segment syntax
    auto data = segment.data;
    auto information_field = TRY(decode_region_segment_information_field(data));

    // "As a special case, as noted in 7.2.7, an immediate generic region segment may have an unknown length. In this case, it
    //  is also possible that the segment may contain fewer rows of bitmap data than are indicated in the segment's region
    //  segment information field.
    //  In order for the decoder to correctly decode the segment, it needs to read the four-byte row count field, which is stored
    //  in the last four bytes of the segment's data part. [...] The row count field contains the actual number of rows contained in
    //  this segment; it must be no greater than the region segment bitmap height value in the segment's region segment
    //  information field."
    // scan_for_immediate_generic_region_size() made `data` the right size for this case, just need to get the rows from the end.
    if (!segment.header.data_length.has_value()) {
        auto last_four_bytes = data.slice_from_end(4);
        u32 row_count = (last_four_bytes[0] << 24) | (last_four_bytes[1] << 16) | (last_four_bytes[2] << 8) | last_four_bytes[3];
        if (row_count > information_field.height)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Row count after data for immediate generic region greater than region segment height");
        if (row_count != information_field.height)
            dbgln_if(JBIG2_DEBUG, "JBIG2ImageDecoderPlugin: Changing row count from {} to {}", information_field.height, row_count);
        information_field.height = row_count;
        data = data.slice(0, data.size() - 4);
    }

    data = data.slice(sizeof(information_field));

    dbgln_if(JBIG2_DEBUG, "Generic region: width={}, height={}, x={}, y={}, flags={:#x}", information_field.width, information_field.height, information_field.x_location, information_field.y_location, information_field.flags);
    TRY(validate_segment_combination_operator_consistency(context, information_field));

    // 7.4.6.2 Generic region segment flags
    if (data.is_empty())
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: No segment data");
    u8 flags = data[0];
    bool uses_mmr = (flags & 1) != 0;

    // "GBTEMPLATE"
    // "If MMR is 1 then this field must contain the value zero."
    u8 arithmetic_coding_template = (flags >> 1) & 3;
    if (uses_mmr && arithmetic_coding_template != 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid GBTEMPLATE");

    bool typical_prediction_generic_decoding_on = (flags >> 3) & 1; // "TPGDON"; "TPGD" is short for "Typical Prediction for Generic Direct coding"
    bool uses_extended_reference_template = (flags >> 4) & 1;       // "EXTTEMPLATE"
    if (flags & 0b1110'0000)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid flags");
    data = data.slice(sizeof(flags));

    // 7.4.6.3 Generic region segment AT flags
    Array<JBIG2::AdaptiveTemplatePixel, 12> adaptive_template_pixels {};
    if (!uses_mmr) {
        dbgln_if(JBIG2_DEBUG, "Non-MMR generic region, GBTEMPLATE={} TPGDON={} EXTTEMPLATE={}", arithmetic_coding_template, typical_prediction_generic_decoding_on, uses_extended_reference_template);

        if (arithmetic_coding_template == 0 && uses_extended_reference_template) {
            // This was added in T.88 Amendment 2 (https://www.itu.int/rec/T-REC-T.88-200306-S!Amd2/en) mid-2003.
            // I haven't seen it being used in the wild, and the spec says "32-byte field as shown below" and then shows 24 bytes,
            // so it's not clear how much data to read.
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: GBTEMPLATE=0 EXTTEMPLATE=1 not yet implemented");
        }

        size_t number_of_adaptive_template_pixels = arithmetic_coding_template == 0 ? 4 : 1;
        if (data.size() < 2 * number_of_adaptive_template_pixels)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: No adaptive template data");
        for (size_t i = 0; i < number_of_adaptive_template_pixels; ++i) {
            adaptive_template_pixels[i].x = static_cast<i8>(data[2 * i]);
            adaptive_template_pixels[i].y = static_cast<i8>(data[2 * i + 1]);
            dbgln_if(JBIG2_DEBUG, "GBAT{}: {}, {}", i, adaptive_template_pixels[i].x, adaptive_template_pixels[i].y);
        }
        data = data.slice(2 * number_of_adaptive_template_pixels);
    }

    // 7.4.6.4 Decoding a generic region segment
    // "1) Interpret its header, as described in 7.4.6.1"
    // Done above.
    // "2) As described in E.3.7, reset all the arithmetic coding statistics to zero."
    Optional<JBIG2::GenericContexts> contexts;
    if (!uses_mmr)
        contexts = JBIG2::GenericContexts { arithmetic_coding_template };

    // "3) Invoke the generic region decoding procedure described in 6.2, with the parameters to the generic region decoding procedure set as shown in Table 37."
    GenericRegionDecodingInputParameters inputs;
    inputs.is_modified_modified_read = uses_mmr;
    inputs.region_width = information_field.width;
    inputs.region_height = information_field.height;
    inputs.gb_template = arithmetic_coding_template;
    inputs.is_typical_prediction_used = typical_prediction_generic_decoding_on;
    inputs.is_extended_reference_template_used = uses_extended_reference_template;
    inputs.skip_pattern = OptionalNone {};
    inputs.adaptive_template_pixels = adaptive_template_pixels;

    Optional<FixedMemoryStream> stream;
    Optional<MQArithmeticDecoder> decoder;
    if (uses_mmr) {
        stream = FixedMemoryStream { data };
        inputs.stream = &stream.value();
    } else {
        decoder = TRY(MQArithmeticDecoder::initialize(data));
        inputs.arithmetic_decoder = &decoder.value();
    }

    auto result = TRY(generic_region_decoding_procedure(inputs, contexts));

    // 8.2 Page image composition step 5)
    if (information_field.x_location + information_field.width > (u32)context.page.size.width()
        || information_field.y_location + information_field.height > (u32)context.page.size.height()) {
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Region bounds outsize of page bounds");
    }

    return RegionResult { .information_field = information_field, .bitmap = move(result) };
}

static ErrorOr<void> decode_intermediate_generic_region(JBIG2LoadingContext& context, SegmentData& segment)
{
    auto result = TRY(decode_generic_region(context, segment));
    return handle_intermediate_direct_region(context, segment, result);
}

static ErrorOr<void> decode_immediate_generic_region(JBIG2LoadingContext& context, SegmentData const& segment)
{
    auto result = TRY(decode_generic_region(context, segment));
    handle_immediate_direct_region(context, result);
    return {};
}

static ErrorOr<void> decode_immediate_lossless_generic_region(JBIG2LoadingContext& context, SegmentData const& segment)
{
    // 7.4.6 Generic region segment syntax
    // "The data parts of all three of the generic region segment types ("intermediate generic region", "immediate generic region" and
    //  "immediate lossless generic region") are coded identically, but are acted upon differently, see 8.2."
    // But 8.2 only describes a difference between intermediate and immediate regions as far as I can tell,
    // and calling the immediate generic region handler for immediate lossless generic regions seems to do the right thing (?).
    return decode_immediate_generic_region(context, segment);
}

static ErrorOr<RegionResult> decode_generic_refinement_region(JBIG2LoadingContext& context, SegmentData const& segment)
{
    // 7.4.7 Generic refinement region syntax
    auto data = segment.data;
    auto information_field = TRY(decode_region_segment_information_field(data));
    data = data.slice(sizeof(information_field));

    dbgln_if(JBIG2_DEBUG, "Generic refinement region: width={}, height={}, x={}, y={}, flags={:#x}", information_field.width, information_field.height, information_field.x_location, information_field.y_location, information_field.flags);
    TRY(validate_segment_combination_operator_consistency(context, information_field));

    FixedMemoryStream stream(data);

    // 7.4.7.2 Generic refinement region segment flags
    u8 flags = TRY(stream.read_value<u8>());
    u8 arithmetic_coding_template = flags & 1;                        // "GRTEMPLATE"
    bool typical_prediction_generic_refinement_on = (flags >> 1) & 1; // "TPGRON"; "TPGR" is short for "Typical Prediction for Generic Refinement coding"
    if (flags & 0b1111'1100)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid refinement flags");

    dbgln_if(JBIG2_DEBUG, "GRTEMPLATE={} TPRDON={}", arithmetic_coding_template, typical_prediction_generic_refinement_on);

    // 7.4.7.3 Generic refinement region segment AT flags
    Array<JBIG2::AdaptiveTemplatePixel, 2> adaptive_template_pixels {};
    if (arithmetic_coding_template == 0) {
        for (size_t i = 0; i < 2; ++i) {
            adaptive_template_pixels[i].x = TRY(stream.read_value<i8>());
            adaptive_template_pixels[i].y = TRY(stream.read_value<i8>());
            dbgln_if(JBIG2_DEBUG, "GRAT{}: {}, {}", i, adaptive_template_pixels[i].x, adaptive_template_pixels[i].y);
        }
    }

    // 7.4.7.5 Decoding a generic refinement region segment
    // "1) Interpret its header as described in 7.4.7.1."
    // Done above.

    VERIFY(segment.referred_to_segments.size() <= 1);

    // "If this segment does not refer to another region segment then its external combination operator must be REPLACE."
    if (segment.referred_to_segments.is_empty()) {
        if (information_field.external_combination_operator() != JBIG2::CombinationOperator::Replace)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Generic refinement region without reference segment must use REPLACE operator");
    }
    // "If it does refer to another region segment, then this segment's region bitmap size, location, and external combination operator
    //  must be equal to that other segment's region bitmap size, location, and external combination operator."
    else {
        auto const& other_information_field = segment.referred_to_segments[0]->aux_buffer_information_field;
        if (information_field.width != other_information_field.width
            || information_field.height != other_information_field.height
            || information_field.x_location != other_information_field.x_location
            || information_field.y_location != other_information_field.y_location
            || information_field.external_combination_operator() != other_information_field.external_combination_operator()) {
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Generic refinement region with reference segment must match size, location and combination operator of referenced segment");
        }
    }

    // "2) As described in E.3.7, reset all the arithmetic coding statistics to zero."
    RefinementContexts contexts { arithmetic_coding_template };

    // "3) Determine the buffer associated with the region segment that this segment refers to."
    // Details described in 7.4.7.4 Reference bitmap selection.
    BilevelImage const* reference_bitmap = nullptr;
    if (segment.referred_to_segments.size() == 1) {
        reference_bitmap = segment.referred_to_segments[0]->aux_buffer.ptr();
        VERIFY(reference_bitmap->width() == segment.referred_to_segments[0]->aux_buffer_information_field.width);
        VERIFY(reference_bitmap->height() == segment.referred_to_segments[0]->aux_buffer_information_field.height);
    } else {
        // When adding support for this and for intermediate generic refinement regions, make sure to only allow
        // this case for immediate generic refinement regions.
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Generic refinement region without reference segment not yet implemented");
    }

    // "4) Invoke the generic refinement region decoding procedure described in 6.3, with the parameters to the
    //     generic refinement region decoding procedure set as shown in Table 38."
    data = data.slice(TRY(stream.tell()));
    GenericRefinementRegionDecodingInputParameters inputs;
    inputs.region_width = information_field.width;
    inputs.region_height = information_field.height;
    inputs.gr_template = arithmetic_coding_template;
    auto subbitmap = reference_bitmap->as_subbitmap();
    inputs.reference_bitmap = &subbitmap;
    inputs.reference_x_offset = 0;
    inputs.reference_y_offset = 0;
    inputs.is_typical_prediction_used = typical_prediction_generic_refinement_on;
    inputs.adaptive_template_pixels = adaptive_template_pixels;

    auto decoder = TRY(MQArithmeticDecoder::initialize(data));
    auto result = TRY(generic_refinement_region_decoding_procedure(inputs, decoder, contexts));
    return RegionResult { .information_field = information_field, .bitmap = move(result) };
}

static ErrorOr<void> decode_intermediate_generic_refinement_region(JBIG2LoadingContext& context, SegmentData& segment)
{
    auto result = TRY(decode_generic_refinement_region(context, segment));

    // 8.2 Page image composition, 5e.
    VERIFY(result.bitmap->width() == result.information_field.width);
    VERIFY(result.bitmap->height() == result.information_field.height);
    segment.aux_buffer = move(result.bitmap);
    segment.aux_buffer_information_field = result.information_field;
    return {};
}

static ErrorOr<void> decode_immediate_generic_refinement_region(JBIG2LoadingContext& context, SegmentData const& segment)
{
    auto result = TRY(decode_generic_refinement_region(context, segment));

    // 8.2 Page image composition, 5d.
    result.bitmap->composite_onto(
        *context.page.bits,
        { result.information_field.x_location, result.information_field.y_location },
        to_composition_type(result.information_field.external_combination_operator()));

    return {};
}

static ErrorOr<void> decode_immediate_lossless_generic_refinement_region(JBIG2LoadingContext& context, SegmentData const& segment)
{
    // 7.4.7 Generic refinement region syntax
    // "The data parts of all three of the generic refinement region segment types ("intermediate generic refinement region",
    //  "immediate generic refinement region" and "immediate lossless generic refinement region") are coded identically, but
    //  are acted upon differently, see 8.2."
    // But 8.2 only describes a difference between intermediate and immediate regions as far as I can tell,
    // and calling the immediate generic refinement region handler for immediate lossless generic refinement regions seems to do the right thing (?).
    return decode_immediate_generic_refinement_region(context, segment);
}

static ErrorOr<void> decode_page_information(JBIG2LoadingContext& context, SegmentData const& segment)
{
    // 7.4.8 Page information segment syntax and 8.1 Decoder model steps 1) - 3).

    // "1) Decode the page information segment."
    auto page_information = TRY(decode_page_information_segment(segment.data));

    u8 default_color = page_information.default_color();
    context.page.default_combination_operator = page_information.default_combination_operator();
    context.page.direct_region_segments_override_default_combination_operator = page_information.direct_region_segments_override_default_combination_operator();

    if (page_information.bitmap_height == 0xffff'ffff && !page_information.page_is_striped())
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Non-striped bitmaps of indeterminate height not allowed");

    dbgln_if(JBIG2_DEBUG, "Page information: width={}, height={}, x_resolution={}, y_resolution={}, is_striped={}, max_stripe_size={}", page_information.bitmap_width, page_information.bitmap_height, page_information.page_x_resolution, page_information.page_y_resolution, page_information.page_is_striped(), page_information.maximum_stripe_size());
    dbgln_if(JBIG2_DEBUG, "Page information flags: {:#02x}", page_information.flags);
    dbgln_if(JBIG2_DEBUG, "    is_eventually_lossless={}", page_information.is_eventually_lossless());
    dbgln_if(JBIG2_DEBUG, "    might_contain_refinements={}", page_information.might_contain_refinements());
    dbgln_if(JBIG2_DEBUG, "    default_color={}", default_color);
    dbgln_if(JBIG2_DEBUG, "    default_combination_operator={}", (int)context.page.default_combination_operator);
    dbgln_if(JBIG2_DEBUG, "    requires_auxiliary_buffers={}", page_information.requires_auxiliary_buffers());
    dbgln_if(JBIG2_DEBUG, "    direct_region_segments_override_default_combination_operator={}", context.page.direct_region_segments_override_default_combination_operator);
    dbgln_if(JBIG2_DEBUG, "    might_contain_coloured_segment={}", page_information.might_contain_coloured_segments());

    // "2) Create the page buffer, of the size given in the page information segment.
    //
    //     If the page height is unknown, then this is not possible. However, in this case the page must be striped,
    //     and the maximum stripe height specified, and the initial page buffer can be created with height initially
    //     equal to this maximum stripe height."
    // ...but we don't care about streaming input (yet?), so scan_for_page_size() already looked at all segment headers
    // and filled in context.page.size from page information and end of stripe segments.
    context.page.bits = TRY(BilevelImage::create(context.page.size.width(), context.page.size.height()));

    // "3) Fill the page buffer with the page's default pixel value."
    context.page.bits->fill(default_color != 0);

    return {};
}

static ErrorOr<void> decode_end_of_page(JBIG2LoadingContext&, SegmentData const& segment)
{
    // 7.4.9 End of page segment syntax
    if (segment.data.size() != 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: End of page segment has non-zero size");

    dbgln_if(JBIG2_DEBUG, "End of page");

    // Actual processing of this segment is in scan_for_page_size().
    return {};
}

static ErrorOr<void> decode_end_of_stripe(JBIG2LoadingContext&, SegmentData const& segment)
{
    // 7.4.10 End of stripe segment syntax
    auto end_of_stripe = TRY(decode_end_of_stripe_segment(segment.data));

    // The data in these segments is used in scan_for_page_size().
    dbgln_if(JBIG2_DEBUG, "End of stripe: y={}", end_of_stripe.y_coordinate);

    return {};
}

static ErrorOr<void> decode_end_of_file(JBIG2LoadingContext&, SegmentData const& segment)
{
    // 7.4.11 End of file segment syntax
    if (segment.data.size() != 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: End of file segment has non-zero size");

    dbgln_if(JBIG2_DEBUG, "End of file");

    return {};
}

static ErrorOr<void> decode_profiles(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode profiles yet");
}

static ErrorOr<void> decode_tables(JBIG2LoadingContext&, SegmentData& segment)
{
    // 7.4.13 Code table segment syntax
    // B.2 Code table structure
    FixedMemoryStream stream { segment.data };

    // "1) Decode the code table flags field as described in B.2.1. This sets the values HTOOB, HTPS and HTRS."
    u8 flags = TRY(stream.read_value<u8>());
    if (flags & 0x80)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid code table flags");
    bool has_out_of_band = flags & 1;             // "HTOOB" in spec.
    u8 prefix_bit_count = ((flags >> 1) & 7) + 1; // "HTPS" (hash table prefix size) in spec.
    u8 range_bit_count = ((flags >> 4) & 7) + 1;  // "HTRS" (hash table range size) in spec.
    dbgln_if(JBIG2_DEBUG, "Tables: has_out_of_band={}, prefix_bit_count={}, range_bit_count={}", has_out_of_band, prefix_bit_count, range_bit_count);

    // "2) Decode the code table lowest value field as described in B.2.2. Let HTLOW be the value decoded."
    i32 lowest_value = TRY(stream.read_value<BigEndian<i32>>()); // "HTLOW" in spec.
    dbgln_if(JBIG2_DEBUG, "Tables: lower bound={}", lowest_value);

    // "3) Decode the code table highest value field as described in B.2.3. Let HTHIGH be the value decoded."
    i32 highest_value = TRY(stream.read_value<BigEndian<i32>>()); // "HTHIGH" in spec.
    dbgln_if(JBIG2_DEBUG, "Tables: One more than upper bound={}", highest_value);

    // "4) Set:
    //         CURRANGELOW = HTLOW
    //         NTEMP = 0"
    i32 value = lowest_value; // "CURRANGELOW" in spec.
    auto bit_stream = BigEndianInputBitStream { MaybeOwned { stream } };

    // "5) Decode each table line as follows:"
    Vector<u8> prefix_lengths;
    Vector<u8> range_lengths;
    Vector<Optional<i32>> range_lows;
    do {
        // "a) Read HTPS bits. Set PREFLEN[NTEMP] to the value decoded."
        u8 prefix_length = TRY(bit_stream.read_bits<u8>(prefix_bit_count));
        TRY(prefix_lengths.try_append(prefix_length));

        // "b) Read HTRS bits. Let RANGELEN[NTEMP] be the value decoded."
        u8 range_length = TRY(bit_stream.read_bits<u8>(range_bit_count));
        TRY(range_lengths.try_append(range_length));

        dbgln_if(JBIG2_DEBUG, "Tables[{}]: prefix_length={}, range_length={}, range_low={}", prefix_lengths.size() - 1, prefix_length, range_length, value);

        // "c) Set:
        //         RANGELOW[NTEMP] = CURRANGELOW
        //         CURRANGELOW = CURRANGELOW + 2 ** RANGELEN[NTEMP]
        //         NTEMP = NTEMP + 1"
        TRY(range_lows.try_append(value));
        value += 1 << range_length;

        // "d) If CURRANGELOW ≥ HTHIGH then proceed to step 6)."
    } while (value < highest_value);

    // "6) Read HTPS bits. Let LOWPREFLEN be the value read."
    u8 prefix_length = TRY(bit_stream.read_bits<u8>(prefix_bit_count)); // "LOWPREFLEN" in spec.

    dbgln_if(JBIG2_DEBUG, "lower: prefix_length={}", prefix_length);

    // "7) [...] This is the lower range table line for this table."
    TRY(prefix_lengths.try_append(prefix_length));
    TRY(range_lengths.try_append(32));
    TRY(range_lows.try_append(lowest_value - 1));

    // "8) Read HTPS bits. Let HIGHPREFLEN be the value read."
    prefix_length = TRY(bit_stream.read_bits<u8>(prefix_bit_count)); // "HIGHPREFLEN" in spec.

    dbgln_if(JBIG2_DEBUG, "upper: prefix_length={}", prefix_length);

    // "9) [...] This is the upper range table line for this table."
    TRY(prefix_lengths.try_append(prefix_length));
    TRY(range_lengths.try_append(32));
    TRY(range_lows.try_append(highest_value));

    // "10) If HTOOB is 1, then:"
    if (has_out_of_band) {
        // "a) Read HTPS bits. Let OOBPREFLEN be the value read."
        prefix_length = TRY(bit_stream.read_bits<u8>(prefix_bit_count)); // "OOBPREFLEN" in spec.

        dbgln_if(JBIG2_DEBUG, "oob: prefix_length={}", prefix_length);

        // "b) [...] This is the out-of-band table line for this table. Note that there is no range associated with this value."
        TRY(prefix_lengths.try_append(prefix_length));
        TRY(range_lengths.try_append(0));
        TRY(range_lows.try_append(OptionalNone {}));
    }

    // "11) Create the prefix codes using the algorithm described in B.3."
    auto codes = TRY(assign_huffman_codes(prefix_lengths));

    Vector<JBIG2::Code> table_codes;
    for (auto const& [i, length] : enumerate(prefix_lengths)) {
        if (length == 0)
            continue;

        JBIG2::Code code { .prefix_length = length, .range_length = range_lengths[i], .first_value = range_lows[i], .code = codes[i] };
        if (i == prefix_lengths.size() - (has_out_of_band ? 3 : 2))
            code.prefix_length |= JBIG2::Code::LowerRangeBit;
        table_codes.append(code);
    }

    segment.codes = move(table_codes);
    segment.huffman_table = JBIG2::HuffmanTable { segment.codes->span(), has_out_of_band };

    return {};
}

static ErrorOr<void> decode_color_palette(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode color palette yet");
}

static ErrorOr<void> decode_extension(JBIG2LoadingContext&, SegmentData const& segment)
{
    // 7.4.14 Extension segment syntax
    FixedMemoryStream stream { segment.data };

    u32 type = TRY(stream.read_value<BigEndian<u32>>());

    dbgln_if(JBIG2_DEBUG, "Extension, type {:#x}", type);

    auto read_string = [&]<class T>() -> ErrorOr<Vector<T>> {
        Vector<T> result;
        do {
            result.append(TRY(stream.read_value<BigEndian<T>>()));
        } while (result.last());
        result.take_last();
        return result;
    };

    switch (type) {
    case to_underlying(JBIG2::ExtensionType::SingleByteCodedComment): {
        // 7.4.15.1 Single-byte coded comment
        // Pairs of zero-terminated ISO/IEC 8859-1 (latin1) pairs, terminated by another \0.
        while (true) {
            auto first_bytes = TRY(read_string.template operator()<u8>());
            if (first_bytes.is_empty())
                break;

            auto second_bytes = TRY(read_string.template operator()<u8>());

            auto first = TRY(TextCodec::decoder_for_exact_name("ISO-8859-1"sv)->to_utf8(StringView { first_bytes }));
            auto second = TRY(TextCodec::decoder_for_exact_name("ISO-8859-1"sv)->to_utf8(StringView { second_bytes }));
            dbgln("JBIG2ImageDecoderPlugin: key '{}', value '{}'", first, second);
        }
        if (!stream.is_eof())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Trailing data after SingleByteCodedComment");
        return {};
    }
    case to_underlying(JBIG2::ExtensionType::MultiByteCodedComment): {
        // 7.4.15.2 Multi-byte coded comment
        // Pairs of (two-byte-)zero-terminated UCS-2 pairs, terminated by another \0\0.
        while (true) {
            auto first_ucs2 = TRY(read_string.template operator()<u16>());
            if (first_ucs2.is_empty())
                break;

            auto second_ucs2 = TRY(read_string.template operator()<u16>());

            auto first = TRY(Utf16View(first_ucs2).to_utf8());
            auto second = TRY(Utf16View(second_ucs2).to_utf8());
            dbgln("JBIG2ImageDecoderPlugin: key '{}', value '{}'", first, second);
        }
        if (!stream.is_eof())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Trailing data after MultiByteCodedComment");
        return {};
    }
    }

    // FIXME: If bit 31 in `type` is not set, the extension isn't necessary, and we could ignore it.
    dbgln("JBIG2ImageDecoderPlugin: Unknown extension type {:#x}", type);
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Unknown extension type");
}

static ErrorOr<void> decode_data(JBIG2LoadingContext& context)
{
    for (size_t i = 0; i < context.segments.size(); ++i) {
        auto& segment = context.segments[i];

        if (segment.header.page_association != 0 && segment.header.page_association != context.current_page_number)
            continue;

        switch (segment.type()) {
        case JBIG2::SegmentType::SymbolDictionary:
            TRY(decode_symbol_dictionary(context, segment));
            break;
        case JBIG2::SegmentType::IntermediateTextRegion:
            TRY(decode_intermediate_text_region(context, segment));
            break;
        case JBIG2::SegmentType::ImmediateTextRegion:
            TRY(decode_immediate_text_region(context, segment));
            break;
        case JBIG2::SegmentType::ImmediateLosslessTextRegion:
            TRY(decode_immediate_lossless_text_region(context, segment));
            break;
        case JBIG2::SegmentType::PatternDictionary:
            TRY(decode_pattern_dictionary(context, segment));
            break;
        case JBIG2::SegmentType::IntermediateHalftoneRegion:
            TRY(decode_intermediate_halftone_region(context, segment));
            break;
        case JBIG2::SegmentType::ImmediateHalftoneRegion:
            TRY(decode_immediate_halftone_region(context, segment));
            break;
        case JBIG2::SegmentType::ImmediateLosslessHalftoneRegion:
            TRY(decode_immediate_lossless_halftone_region(context, segment));
            break;
        case JBIG2::SegmentType::IntermediateGenericRegion:
            TRY(decode_intermediate_generic_region(context, segment));
            break;
        case JBIG2::SegmentType::ImmediateGenericRegion:
            TRY(decode_immediate_generic_region(context, segment));
            break;
        case JBIG2::SegmentType::ImmediateLosslessGenericRegion:
            TRY(decode_immediate_lossless_generic_region(context, segment));
            break;
        case JBIG2::SegmentType::IntermediateGenericRefinementRegion:
            TRY(decode_intermediate_generic_refinement_region(context, segment));
            break;
        case JBIG2::SegmentType::ImmediateGenericRefinementRegion:
            TRY(decode_immediate_generic_refinement_region(context, segment));
            break;
        case JBIG2::SegmentType::ImmediateLosslessGenericRefinementRegion:
            TRY(decode_immediate_lossless_generic_refinement_region(context, segment));
            break;
        case JBIG2::SegmentType::PageInformation:
            TRY(decode_page_information(context, segment));
            break;
        case JBIG2::SegmentType::EndOfPage:
            TRY(decode_end_of_page(context, segment));
            break;
        case JBIG2::SegmentType::EndOfStripe:
            TRY(decode_end_of_stripe(context, segment));
            break;
        case JBIG2::SegmentType::EndOfFile:
            TRY(decode_end_of_file(context, segment));
            // "If a file contains an end of file segment, it must be the last segment."
            if (i != context.segments.size() - 1)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: End of file segment not last segment");
            break;
        case JBIG2::SegmentType::Profiles:
            TRY(decode_profiles(context, segment));
            break;
        case JBIG2::SegmentType::Tables:
            TRY(decode_tables(context, segment));
            break;
        case JBIG2::SegmentType::ColorPalette:
            TRY(decode_color_palette(context, segment));
            break;
        case JBIG2::SegmentType::Extension:
            TRY(decode_extension(context, segment));
            break;
        }

        dbgln_if(JBIG2_DEBUG, "");
    }

    return {};
}

JBIG2ImageDecoderPlugin::JBIG2ImageDecoderPlugin()
{
    m_context = make<JBIG2LoadingContext>();
}

JBIG2ImageDecoderPlugin::~JBIG2ImageDecoderPlugin() = default;

IntSize JBIG2ImageDecoderPlugin::size()
{
    return m_context->page.size;
}

bool JBIG2ImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    return data.starts_with(JBIG2::id_string);
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> JBIG2ImageDecoderPlugin::create(ReadonlyBytes data)
{
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) JBIG2ImageDecoderPlugin()));
    TRY(decode_jbig2_header(*plugin->m_context, data));

    data = data.slice(sizeof(JBIG2::id_string) + sizeof(u8) + (plugin->m_context->number_of_pages.has_value() ? sizeof(u32) : 0));
    TRY(decode_segment_headers(*plugin->m_context, data));
    TRY(complete_decoding_all_segment_headers(*plugin->m_context));

    TRY(scan_for_page_size(*plugin->m_context));
    TRY(scan_for_page_numbers(*plugin->m_context));

    return plugin;
}

size_t JBIG2ImageDecoderPlugin::frame_count()
{
    return m_context->page_numbers.size();
}

ErrorOr<ImageFrameDescriptor> JBIG2ImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
{
    if (index >= frame_count())
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid frame index");

    if (m_context->current_page_number != m_context->page_numbers[index]) {
        m_context->current_page_number = m_context->page_numbers[index];
        m_context->state = JBIG2LoadingContext::State::NotDecoded;
        TRY(scan_for_page_size(*m_context));
    }

    if (m_context->state == JBIG2LoadingContext::State::Error)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Decoding failed");

    if (m_context->state < JBIG2LoadingContext::State::Decoded) {
        auto result = decode_data(*m_context);
        if (result.is_error()) {
            m_context->state = JBIG2LoadingContext::State::Error;
            return result.release_error();
        }
        m_context->state = JBIG2LoadingContext::State::Decoded;
    }

    auto bitmap = TRY(m_context->page.bits->to_gfx_bitmap());
    return ImageFrameDescriptor { move(bitmap), 0 };
}

ErrorOr<ByteBuffer> JBIG2ImageDecoderPlugin::decode_embedded(Vector<ReadonlyBytes> data)
{
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) JBIG2ImageDecoderPlugin()));
    plugin->m_context->organization = JBIG2::Organization::Embedded;

    for (auto const& segment_data : data)
        TRY(decode_segment_headers(*plugin->m_context, segment_data));
    TRY(complete_decoding_all_segment_headers(*plugin->m_context));

    TRY(scan_for_page_size(*plugin->m_context));
    TRY(scan_for_page_numbers(*plugin->m_context));

    if (plugin->frame_count() != 1)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Embedded JBIG2 data must have exactly one page");

    TRY(decode_data(*plugin->m_context));

    return plugin->m_context->page.bits->to_byte_buffer();
}

}
