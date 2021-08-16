/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "endian.hpp"
#include "inttypes.hpp"

// Most modern compilers optimize the bswap routines to native instructions.
inline static u2 bswap_16(u2 x) {
    return ((x & 0xFF) << 8) |
           ((x >> 8) & 0xFF);
}

inline static u4 bswap_32(u4 x) {
    return ((x & 0xFF) << 24) |
           ((x & 0xFF00) << 8) |
           ((x >> 8) & 0xFF00) |
           ((x >> 24) & 0xFF);
}

inline static u8 bswap_64(u8 x) {
    return (u8)bswap_32((u4)x) << 32 |
           (u8)bswap_32((u4)(x >> 32));
}

u2 NativeEndian::get(u2 x) { return x; }
u4 NativeEndian::get(u4 x) { return x; }
u8 NativeEndian::get(u8 x) { return x; }
s2 NativeEndian::get(s2 x) { return x; }
s4 NativeEndian::get(s4 x) { return x; }
s8 NativeEndian::get(s8 x) { return x; }

void NativeEndian::set(u2& x, u2 y) { x = y; }
void NativeEndian::set(u4& x, u4 y) { x = y; }
void NativeEndian::set(u8& x, u8 y) { x = y; }
void NativeEndian::set(s2& x, s2 y) { x = y; }
void NativeEndian::set(s4& x, s4 y) { x = y; }
void NativeEndian::set(s8& x, s8 y) { x = y; }

NativeEndian NativeEndian::_native;

u2 SwappingEndian::get(u2 x) { return bswap_16(x); }
u4 SwappingEndian::get(u4 x) { return bswap_32(x); }
u8 SwappingEndian::get(u8 x) { return bswap_64(x); }
s2 SwappingEndian::get(s2 x) { return bswap_16(x); }
s4 SwappingEndian::get(s4 x) { return bswap_32(x); }
s8 SwappingEndian::get(s8 x) { return bswap_64(x); }

void SwappingEndian::set(u2& x, u2 y) { x = bswap_16(y); }
void SwappingEndian::set(u4& x, u4 y) { x = bswap_32(y); }
void SwappingEndian::set(u8& x, u8 y) { x = bswap_64(y); }
void SwappingEndian::set(s2& x, s2 y) { x = bswap_16(y); }
void SwappingEndian::set(s4& x, s4 y) { x = bswap_32(y); }
void SwappingEndian::set(s8& x, s8 y) { x = bswap_64(y); }

SwappingEndian SwappingEndian::_swapping;

Endian* Endian::get_handler(bool big_endian) {
    // If requesting little endian on a little endian machine or
    // big endian on a big endian machine use native handler
    if (big_endian == is_big_endian()) {
        return NativeEndian::get_native();
    } else {
        // Use swapping handler.
        return SwappingEndian::get_swapping();
    }
}

// Return a platform u2 from an array in which Big Endian is applied.
u2 Endian::get_java(u1* x) {
    return (u2) (x[0]<<8 | x[1]);
}

// Add a platform u2 to the array as a Big Endian u2
void Endian::set_java(u1* p, u2 x) {
    p[0] = (x >> 8) & 0xff;
    p[1] = x & 0xff;
}

Endian* Endian::get_native_handler() {
    return NativeEndian::get_native();
}
