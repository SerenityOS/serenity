/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Types.h>
#include <AK/Optional.h>

namespace Video {

class BitStream {
public:
    BitStream(const u8* data, size_t size)
        : m_data_ptr(data)
        , m_bytes_remaining(size)
    {
    }

    u8 read_byte();
    bool read_bit();
    u8 read_f(size_t n);
    i8 read_s(size_t n);
    u8 read_f8();
    u16 read_f16();
    u8 read_literal(size_t n);

    u64 get_position();
    size_t bytes_remaining();
    size_t bits_remaining();

    bool init_bool(size_t bytes);
    bool read_bool(u8 probability);
    bool exit_bool();
private:
    const u8* m_data_ptr { nullptr };
    size_t m_bytes_remaining { 0 };
    Optional<u8> m_current_byte;
    i8 m_current_bit_position { 0 };
    u64 m_bytes_read { 0 };

    u8 m_bool_value { 0 };
    u8 m_bool_range { 0 };
    u64 m_bool_max_bits { 0 };
};

}
