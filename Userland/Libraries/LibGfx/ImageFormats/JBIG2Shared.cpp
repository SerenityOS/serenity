/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitStream.h>
#include <LibGfx/ImageFormats/JBIG2Shared.h>

namespace Gfx::JBIG2 {

ErrorOr<SymbolDictionaryHuffmanTables> symbol_dictionary_huffman_tables_from_flags(u16 flags, Vector<JBIG2::HuffmanTable const*> custom_tables)
{
    // 7.4.2.1.1 Symbol dictionary flags
    SymbolDictionaryHuffmanTables tables;

    bool uses_huffman_encoding = (flags & 1) != 0; // "SDHUFF" in spec.

    u8 custom_table_index = 0;
    auto custom_table = [&custom_tables, &custom_table_index]() -> ErrorOr<JBIG2::HuffmanTable const*> {
        if (custom_table_index >= custom_tables.size())
            return Error::from_string_literal("JBIG2: Custom Huffman table index out of range");
        return custom_tables[custom_table_index++];
    };

    u8 huffman_table_selection_for_height_differences = (flags >> 2) & 0b11; // "SDHUFFDH" in spec.
    if (!uses_huffman_encoding && huffman_table_selection_for_height_differences != 0)
        return Error::from_string_literal("JBIG2: Invalid huffman_table_selection_for_height_differences");

    if (uses_huffman_encoding) {
        if (huffman_table_selection_for_height_differences == 0)
            tables.delta_height_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_4));
        else if (huffman_table_selection_for_height_differences == 1)
            tables.delta_height_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_5));
        else if (huffman_table_selection_for_height_differences == 2)
            return Error::from_string_literal("JBIG2: Invalid huffman_table_selection_for_height_differences");
        else if (huffman_table_selection_for_height_differences == 3)
            tables.delta_height_table = TRY(custom_table());
    }

    u8 huffman_table_selection_for_width_differences = (flags >> 4) & 0b11; // "SDHUFFDW" in spec.
    if (!uses_huffman_encoding && huffman_table_selection_for_width_differences != 0)
        return Error::from_string_literal("JBIG2: Invalid huffman_table_selection_for_width_differences");

    if (uses_huffman_encoding) {
        if (huffman_table_selection_for_width_differences == 0)
            tables.delta_width_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_2));
        else if (huffman_table_selection_for_width_differences == 1)
            tables.delta_width_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_3));
        else if (huffman_table_selection_for_width_differences == 2)
            return Error::from_string_literal("JBIG2: Invalid huffman_table_selection_for_height_differences");
        else if (huffman_table_selection_for_width_differences == 3)
            tables.delta_width_table = TRY(custom_table());
    }

    bool uses_user_supplied_size_table = (flags >> 6) & 1; // "SDHUFFBMSIZE" in spec.
    if (!uses_huffman_encoding && uses_user_supplied_size_table)
        return Error::from_string_literal("JBIG2: Invalid uses_user_supplied_size_table");

    if (uses_huffman_encoding) {
        if (!uses_user_supplied_size_table)
            tables.bitmap_size_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_1));
        else
            tables.bitmap_size_table = TRY(custom_table());
    }

    bool uses_user_supplied_aggregate_table = (flags >> 7) & 1; // "SDHUFFAGGINST" in spec.
    if (!uses_huffman_encoding && uses_user_supplied_aggregate_table)
        return Error::from_string_literal("JBIG2: Invalid uses_user_supplied_aggregate_table");

    if (uses_huffman_encoding) {
        if (!uses_user_supplied_aggregate_table)
            tables.number_of_symbol_instances_table = TRY(JBIG2::HuffmanTable::standard_huffman_table(JBIG2::HuffmanTable::StandardTable::B_1));
        else
            tables.number_of_symbol_instances_table = TRY(custom_table());
    }

    if (custom_table_index != custom_tables.size())
        return Error::from_string_literal("JBIG2: Not all referred custom tables used");

    if (uses_huffman_encoding) {
        if (!tables.delta_width_table->has_oob_symbol())
            return Error::from_string_literal("JBIG2: Custom SDHUFFDW table must have OOB symbol");

        if (tables.delta_height_table->has_oob_symbol()
            || tables.bitmap_size_table->has_oob_symbol()
            || tables.number_of_symbol_instances_table->has_oob_symbol()) {
            return Error::from_string_literal("JBIG2: Custom Huffman tables must not have OOB symbol");
        }
    }

    return tables;
}

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

ErrorOr<void> HuffmanTable::write_symbol_internal(BigEndianOutputBitStream& stream, Optional<i32> value_or_oob) const
{
    // FIXME: Use an approach that doesn't require a full scan for every value,
    //        for example by handling OOB, lower range, and upper range first,
    //        and then binary searching the rest.
    for (auto const& code : m_codes) {
        if (value_or_oob.has_value() != code.first_value.has_value())
            continue;

        if (!value_or_oob.has_value()) {
            VERIFY(code.range_length == 0);
            return stream.write_bits(code.code, code.prefix_length);
        }

        auto first_value = code.first_value.value();
        auto value = value_or_oob.value();

        if (code.prefix_length & Code::LowerRangeBit) {
            VERIFY(code.range_length == 32);
            if (value > first_value)
                continue;
            TRY(stream.write_bits(code.code, code.prefix_length & ~Code::LowerRangeBit));
            return stream.write_bits(static_cast<u32>(first_value - value), code.range_length);
        }

        if (value < first_value || (code.range_length != 32 && value >= (first_value + (1 << code.range_length))))
            continue;
        TRY(stream.write_bits(code.code, code.prefix_length));
        return stream.write_bits(static_cast<u32>(value - first_value), code.range_length);
    }
    return Error::from_string_literal("JBIG2Writer: value not representable in this huffman table");
}

ErrorOr<void> HuffmanTable::write_symbol(BigEndianOutputBitStream& stream, Optional<i32> value) const
{
    VERIFY(m_has_oob_symbol);
    return write_symbol_internal(stream, value);
}

ErrorOr<void> HuffmanTable::write_symbol_non_oob(BigEndianOutputBitStream& stream, i32 value) const
{
    VERIFY(!m_has_oob_symbol);
    return write_symbol_internal(stream, value);
}

}
