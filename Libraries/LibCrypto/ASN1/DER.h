/*
 * Copyright (c) 2020, Ali Mohammad Pur <ali.mpfard@gmail.com>
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
#include <LibCrypto/ASN1/ASN1.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>

namespace Crypto {

static bool der_decode_integer(const u8* in, size_t length, UnsignedBigInteger& number)
{
    if (length < 3) {
        dbg() << "invalid header size";
        return false;
    }

    size_t x { 0 };
    // must start with 0x02
    if ((in[x++] & 0x1f) != 0x02) {
        dbg() << "not an integer " << in[x - 1] << " (" << in[x] << " follows)";
        return false;
    }

    // decode length
    size_t z = in[x++];
    if ((x & 0x80) == 0) {
        // overflow
        if (x + z > length) {
            dbg() << "would overflow " << z + x << " > " << length;
            return false;
        }

        number = UnsignedBigInteger::import_data(in + x, z);
        return true;
    } else {
        // actual big integer
        z &= 0x7f;

        // overflow
        if ((x + z) > length || z > 4 || z == 0) {
            dbg() << "would overflow " << z + x << " > " << length;
            return false;
        }

        size_t y = 0;
        while (z--) {
            y = ((size_t)(in[x++])) | (y << 8);
        }

        // overflow
        if (x + y > length) {
            dbg() << "would overflow " << y + x << " > " << length;
            return false;
        }

        number = UnsignedBigInteger::import_data(in + x, y);
        return true;
    }

    // see if it's negative
    if (in[x] & 0x80) {
        dbg() << "negative bigint unsupported in der_decode_integer";
        return false;
    }

    return true;
}
static bool der_length_integer(UnsignedBigInteger* num, size_t* out_length)
{
    auto& bigint = *num;
    size_t value_length = bigint.trimmed_length() * sizeof(u32);
    auto length = value_length;
    if (length == 0) {
        ++length;
    } else {
        // the number comes with a 0 padding to make it positive in 2's comp
        // add that zero if the msb is 1, but only if we haven't padded it
        // ourselves
        auto ms2b = (u16)(bigint.words()[bigint.trimmed_length() - 1] >> 16);

        if ((ms2b & 0xff00) == 0) {
            if (!(((u8)ms2b) & 0x80))
                --length;
        } else if (ms2b & 0x8000) {
            ++length;
        }
    }
    if (value_length < 128) {
        ++length;
    } else {
        ++length;
        while (value_length) {
            ++length;
            value_length >>= 8;
        }
    }
    // kind
    ++length;
    *out_length = length;
    return true;
}
constexpr static bool der_decode_object_identifier(const u8* in, size_t in_length, u8* words, u8* out_length)
{
    if (in_length < 3)
        return false; // invalid header

    if (*out_length < 2)
        return false; // need at least two words

    size_t x { 0 };
    if ((in[x++] & 0x1f) != 0x06) {
        return false; // invalid header value
    }

    size_t length { 0 };
    if (in[x] < 128) {
        length = in[x++];
    } else {
        if ((in[x] < 0x81) | (in[x] > 0x82))
            return false; // invalid header

        size_t y = in[x++] & 0x7f;
        while (y--)
            length = (length << 8) | (size_t)in[x++];
    }

    if (length < 1 || length + x > in_length)
        return false; // invalid length or overflow

    size_t y { 0 }, t { 0 };
    while (length--) {
        t = (t << 7) | (in[x] & 0x7f);
        if (!(in[x++] & 0x80)) {
            if (y >= *out_length)
                return false; // overflow

            if (y == 0) {
                words[0] = t / 40;
                words[1] = t % 40;
                y = 2;
            } else {
                words[y++] = t;
            }
            t = 0;
        }
    }
    *out_length = y;
    return true;
}

static constexpr size_t der_object_identifier_bits(size_t x)
{
    x &= 0xffffffff;
    size_t c { 0 };
    while (x) {
        ++c;
        x >>= 1;
    }
    return c;
}

constexpr static bool der_length_object_identifier(u8* words, size_t num_words, size_t* out_length)
{
    if (num_words < 2)
        return false;

    if (words[0] > 3 || (words[0] < 2 && words[1] > 39))
        return false;

    size_t z { 0 };
    size_t wordbuf = words[0] * 40 + words[1];
    for (size_t y = 0; y < num_words; ++y) {
        auto t = der_object_identifier_bits(wordbuf);
        z = t / 7 + (!!(t % 7)) + (!!(wordbuf == 0));
        if (y < num_words - 1)
            wordbuf = words[y + 1];
    }

    if (z < 128) {
        z += 2;
    } else if (z < 256) {
        z += 3;
    } else {
        z += 4;
    }
    *out_length = z;
    return true;
}

constexpr static bool der_length_sequence(ASN1::List* list, size_t in_length, size_t* out_length)
{
    size_t y { 0 }, x { 0 };
    for (size_t i = 0; i < in_length; ++i) {
        auto type = list[i].kind;
        auto size = list[i].size;
        auto data = list[i].data;

        if (type == ASN1::Kind::Eol)
            break;

        switch (type) {
        case ASN1::Kind::Integer:
            if (!der_length_integer((UnsignedBigInteger*)data, &x)) {
                return false;
            }
            y += x;
            break;
        case ASN1::Kind::ObjectIdentifier:
            if (!der_length_object_identifier((u8*)data, size, &x)) {
                return false;
            }
            y += x;
            break;
        case ASN1::Kind::Sequence:
            if (!der_length_sequence((ASN1::List*)data, size, &x)) {
                return false;
            }
            y += x;
            break;
        default:
            dbg() << "Unhandled Kind " << ASN1::kind_name(type);
            ASSERT_NOT_REACHED();
            break;
        }
    }

    if (y < 128) {
        y += 2;
    } else if (y < 256) {
        y += 3;
    } else if (y < 65536) {
        y += 4;
    } else if (y < 16777216ul) {
        y += 5;
    } else {
        dbg() << "invalid length " << y;
        return false;
    }
    *out_length = y;
    return true;
}

static inline bool der_decode_sequence(const u8* in, size_t in_length, ASN1::List* list, size_t out_length, bool ordered = true)
{
    if (in_length < 2) {
        dbg() << "header too small";
        return false; // invalid header
    }
    size_t x { 0 };
    if (in[x++] != 0x30) {
        dbg() << "not a sequence: " << in[x - 1];
        return false; // not a sequence
    }
    size_t block_size { 0 };
    size_t y { 0 };
    if (in[x] < 128) {
        block_size = in[x++];
    } else if (in[x] & 0x80) {
        if ((in[x] < 0x81) || (in[x] > 0x83)) {
            dbg() << "invalid length element " << in[x];
            return false;
        }

        y = in[x++] & 0x7f;

        if (x + y > in_length) {
            dbg() << "would overflow " << x + y << " -> " << in_length;
            return false; // overflow
        }
        block_size = 0;
        while (y--)
            block_size = (block_size << 8) | (size_t)in[x++];
    }

    // overflow
    if (x + block_size > in_length) {
        dbg() << "would overflow " << x + block_size << " -> " << in_length;
        return false;
    }

    for (size_t i = 0; i < out_length; ++i)
        list[i].used = false;

    in_length = block_size;
    for (size_t i = 0; i < out_length; ++i) {
        size_t z = 0;
        auto kind = list[i].kind;
        auto size = list[i].size;
        auto data = list[i].data;

        if (!ordered && list[i].used) {
            continue;
        }

        switch (kind) {
        case ASN1::Kind::Integer:
            z = in_length;
            if (!der_decode_integer(in + x, z, *(UnsignedBigInteger*)data)) {
                dbg() << "could not decode an integer";
                return false;
            }
            if (!der_length_integer((UnsignedBigInteger*)data, &z)) {
                dbg() << "could not figure out the length";
                return false;
            }
            break;
        case ASN1::Kind::ObjectIdentifier:
            z = in_length;
            if (!der_decode_object_identifier(in + x, z, (u8*)data, (u8*)&size)) {
                if (!ordered)
                    continue;
                return false;
            }
            list[i].size = size;
            if (!der_length_object_identifier((u8*)data, size, &z)) {
                return false;
            }
            break;
        case ASN1::Kind::Sequence:
            if ((in[x] & 0x3f) != 0x30) {
                dbg() << "Not a sequence: " << (in[x] & 0x3f);
                return false;
            }
            z = in_length;
            if (!der_decode_sequence(in + x, z, (ASN1::List*)data, size)) {
                if (!ordered)
                    continue;
                return false;
            }
            if (!der_length_sequence((ASN1::List*)data, size, &z)) {
                return false;
            }
            break;
        default:
            dbg() << "Unhandled ASN1 kind " << ASN1::kind_name(kind);
            ASSERT_NOT_REACHED();
            break;
        }
        x += z;
        in_length -= z;
        list[i].used = true;
        if (!ordered)
            i = -1;
    }
    for (size_t i = 0; i < out_length; ++i)
        if (!list[i].used) {
            dbg() << "index " << i << " was not read";
            return false;
        }

    return true;
}

template<size_t element_count>
struct der_decode_sequence_many_base {
    constexpr void set(size_t index, ASN1::Kind kind, size_t size, void* data)
    {
        ASN1::set(m_list[index], kind, data, size);
    }

    constexpr der_decode_sequence_many_base(const u8* in, size_t in_length)
        : m_in(in)
        , m_in_length(in_length)
    {
    }

    ASN1::List* list() { return m_list; }
    const u8* in() { return m_in; }
    size_t in_length() { return m_in_length; }

protected:
    ASN1::List m_list[element_count];
    const u8* m_in;
    size_t m_in_length;
};

template<size_t element_count>
struct der_decode_sequence_many : public der_decode_sequence_many_base<element_count> {

    template<typename ElementType, typename... Args>
    constexpr void construct(size_t index, ASN1::Kind kind, size_t size, ElementType data, Args... args)
    {
        der_decode_sequence_many_base<element_count>::set(index, kind, size, (void*)data);
        construct(index + 1, args...);
    }

    constexpr void construct(size_t index)
    {
        ASSERT(index == element_count);
    }

    template<typename... Args>
    constexpr der_decode_sequence_many(const u8* in, size_t in_length, Args... args)
        : der_decode_sequence_many_base<element_count>(in, in_length)
    {
        construct(0, args...);
    }

    constexpr operator bool()
    {
        return der_decode_sequence(this->m_in, this->m_in_length, this->m_list, element_count);
    }
};

// FIXME: Move these terrible constructs into their own place
constexpr static void decode_b64_block(const u8 in[4], u8 out[3])
{
    out[0] = (u8)(in[0] << 2 | in[1] >> 4);
    out[1] = (u8)(in[1] << 4 | in[2] >> 2);
    out[2] = (u8)(((in[2] << 6) & 0xc0) | in[3]);
}

constexpr static char base64_chars[] { "|$$$}rstuvwxyz{$$$$$$$>?@ABCDEFGHIJKLMNOPQRSTUVW$$$$$$XYZ[\\]^_`abcdefghijklmnopq" };
constexpr static size_t decode_b64(const u8* in_buffer, size_t in_length, ByteBuffer& out_buffer)
{
    u8 in[4] { 0 }, out[3] { 0 }, v { 0 };
    size_t i { 0 }, length { 0 };
    size_t output_offset { 0 };

    const u8* ptr = in_buffer;

    while (ptr <= in_buffer + in_length) {
        for (length = 0, i = 0; i < 4 && (ptr <= in_buffer + in_length); ++i) {
            v = 0;
            while ((ptr <= in_buffer + in_length) && !v) {
                v = ptr[0];
                ++ptr;
                v = (u8)((v < 43 || v > 122) ? 0 : base64_chars[v - 43]);
                if (v)
                    v = (u8)(v == '$' ? 0 : v - 61);
            }
            if (ptr <= in_buffer + in_length) {
                ++length;
                if (v)
                    in[i] = v - 1;

            } else {
                in[i] = 0;
            }
        }
        if (length) {
            decode_b64_block(in, out);
            out_buffer.overwrite(output_offset, out, length - 1);
            output_offset += length - 1;
        }
    }
    return output_offset;
}
}
