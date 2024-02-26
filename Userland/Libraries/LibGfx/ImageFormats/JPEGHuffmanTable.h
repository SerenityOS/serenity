/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Vector.h>

namespace Gfx::JPEG {

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

}
