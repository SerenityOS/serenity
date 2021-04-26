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
    template<typename StreamT>
    static bool read_unsigned(StreamT& stream, size_t& result)
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

            result = (result) | (static_cast<size_t>(byte & ~(1 << 7)) << (num_bytes * 7));
            if (!(byte & (1 << 7)))
                break;
            ++num_bytes;
        }

        return true;
    }

    template<typename StreamT>
    static bool read_signed(StreamT& stream, ssize_t& result)
    {
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
            result = (result) | (static_cast<size_t>(byte & ~(1 << 7)) << (num_bytes * 7));
            ++num_bytes;
        } while (byte & (1 << 7));

        if (num_bytes * 7 < sizeof(size_t) * 4 && (byte & 0x40)) {
            // sign extend
            result |= ((size_t)(-1) << (num_bytes * 7));
        }

        return true;
    }
};

}

using AK::LEB128;
