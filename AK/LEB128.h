/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Stream.h>
#include <AK/Types.h>

namespace AK {

struct LEB128 {
    template<typename StreamT, typename ValueType = size_t>
    static bool read_unsigned(StreamT& stream, ValueType& result)
    {
        [[maybe_unused]] size_t backup_offset = 0;
        if constexpr (requires { stream.offset(); })
            backup_offset = stream.offset();
        InputStream& input_stream { stream };

        result = 0;
        size_t num_bytes = 0;
        while (true) {
            if (input_stream.unreliable_eof()) {
                if constexpr (requires { stream.seek(backup_offset); })
                    stream.seek(backup_offset);
                input_stream.set_fatal_error();
                return false;
            }

            u8 byte = 0;
            input_stream >> byte;
            if (input_stream.has_any_error())
                return false;

            result = (result) | (static_cast<ValueType>(byte & ~(1 << 7)) << (num_bytes * 7));
            if (!(byte & (1 << 7)))
                break;
            ++num_bytes;
        }

        return true;
    }

    template<typename StreamT, typename ValueType = ssize_t>
    static bool read_signed(StreamT& stream, ValueType& result)
    {
        using UValueType = MakeUnsigned<ValueType>;
        [[maybe_unused]] size_t backup_offset = 0;
        if constexpr (requires { stream.offset(); })
            backup_offset = stream.offset();
        InputStream& input_stream { stream };

        result = 0;
        size_t num_bytes = 0;
        u8 byte = 0;

        do {
            if (input_stream.unreliable_eof()) {
                if constexpr (requires { stream.seek(backup_offset); })
                    stream.seek(backup_offset);
                input_stream.set_fatal_error();
                return false;
            }

            input_stream >> byte;
            if (input_stream.has_any_error())
                return false;
            result = (result) | (static_cast<UValueType>(byte & ~(1 << 7)) << (num_bytes * 7));
            ++num_bytes;
        } while (byte & (1 << 7));

        if (num_bytes * 7 < sizeof(UValueType) * 4 && (byte & 0x40)) {
            // sign extend
            result |= ((UValueType)(-1) << (num_bytes * 7));
        }

        return true;
    }
};

}

using AK::LEB128;
