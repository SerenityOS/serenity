/*
 * Copyright (c) 2020-2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AddressRanges.h"
#include "DwarfTypes.h"
#include <AK/LEB128.h>
#include <LibCore/Stream.h>

namespace Debug::Dwarf {

AddressRangesV5::AddressRangesV5(NonnullOwnPtr<Core::Stream::Stream> range_lists_stream, CompilationUnit const& compilation_unit)
    : m_range_lists_stream(move(range_lists_stream))
    , m_compilation_unit(compilation_unit)
{
}

ErrorOr<void> AddressRangesV5::for_each_range(Function<void(Range)> callback)
{
    // Dwarf version 5, section 2.17.3 "Non-Contiguous Address Ranges"

    Core::Stream::WrapInAKInputStream wrapped_range_lists_stream { *m_range_lists_stream };

    Optional<FlatPtr> current_base_address;
    while (!m_range_lists_stream->is_eof()) {
        auto entry_type = TRY(m_range_lists_stream->read_value<u8>());
        switch (static_cast<RangeListEntryType>(entry_type)) {
        case RangeListEntryType::BaseAddress: {
            current_base_address = TRY(m_range_lists_stream->read_value<FlatPtr>());
            break;
        }
        case RangeListEntryType::BaseAddressX: {
            FlatPtr index;
            LEB128::read_unsigned(wrapped_range_lists_stream, index);
            current_base_address = m_compilation_unit.get_address(index);
            break;
        }
        case RangeListEntryType::OffsetPair: {
            Optional<FlatPtr> base_address = current_base_address;
            if (!base_address.has_value()) {
                base_address = m_compilation_unit.base_address();
            }

            if (!base_address.has_value())
                return Error::from_string_literal("Expected base_address for rangelist");

            size_t start_offset, end_offset;
            LEB128::read_unsigned(wrapped_range_lists_stream, start_offset);
            LEB128::read_unsigned(wrapped_range_lists_stream, end_offset);
            callback(Range { start_offset + *base_address, end_offset + *base_address });
            break;
        }
        case RangeListEntryType::StartLength: {
            auto start = TRY(m_range_lists_stream->read_value<FlatPtr>());
            size_t length;
            LEB128::read_unsigned(wrapped_range_lists_stream, length);
            callback(Range { start, start + length });
            break;
        }
        case RangeListEntryType::StartXEndX: {
            size_t start, end;
            LEB128::read_unsigned(wrapped_range_lists_stream, start);
            LEB128::read_unsigned(wrapped_range_lists_stream, end);
            callback(Range { m_compilation_unit.get_address(start), m_compilation_unit.get_address(end) });
            break;
        }
        case RangeListEntryType::StartXLength: {
            size_t start, length;
            LEB128::read_unsigned(wrapped_range_lists_stream, start);
            LEB128::read_unsigned(wrapped_range_lists_stream, length);
            auto start_addr = m_compilation_unit.get_address(start);
            callback(Range { start_addr, start_addr + length });
            break;
        }
        case RangeListEntryType::EndOfList:
            return {};
        default:
            dbgln("unsupported range list entry type: 0x{:x}", entry_type);
            return Error::from_string_literal("Unsupported range list entry type");
        }
    }

    return {};
}

AddressRangesV4::AddressRangesV4(NonnullOwnPtr<Core::Stream::Stream> ranges_stream, CompilationUnit const& compilation_unit)
    : m_ranges_stream(move(ranges_stream))
    , m_compilation_unit(compilation_unit)
{
}

ErrorOr<void> AddressRangesV4::for_each_range(Function<void(Range)> callback)
{
    // Dwarf version 4, section 2.17.3 "Non-Contiguous Address Ranges"

    Optional<FlatPtr> current_base_address;
    while (!m_ranges_stream->is_eof()) {
        auto begin = TRY(m_ranges_stream->read_value<FlatPtr>());
        auto end = TRY(m_ranges_stream->read_value<FlatPtr>());

        if (begin == 0 && end == 0) {
            // end of list entry
            return {};
        } else if (begin == explode_byte(0xff)) {
            current_base_address = end;
        } else {
            FlatPtr base = current_base_address.value_or(m_compilation_unit.base_address().value_or(0));
            callback({ base + begin, base + end });
        }
    }

    return {};
}

}
