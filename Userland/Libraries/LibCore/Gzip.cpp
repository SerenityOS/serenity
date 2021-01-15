/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <AK/Optional.h>
#include <LibCore/Gzip.h>
#include <LibCore/puff.h>
#include <limits.h>
#include <stddef.h>

namespace Core {

bool Gzip::is_compressed(const ByteBuffer& data)
{
    return data.size() > 2 && data[0] == 0x1F && data[1] == 0x8b;
}

// skips the gzip header
// see: https://tools.ietf.org/html/rfc1952#page-5
static Optional<ByteBuffer> get_gzip_payload(const ByteBuffer& data)
{
    size_t current = 0;
    auto read_byte = [&]() {
        if (current >= data.size()) {
            ASSERT_NOT_REACHED();
            return (u8)0;
        }
        return data[current++];
    };

#ifdef DEBUG_GZIP
    dbgln("get_gzip_payload: Skipping over gzip header.");
#endif

    // Magic Header
    if (read_byte() != 0x1F || read_byte() != 0x8B) {
        dbgln("get_gzip_payload: Wrong magic number.");
        return Optional<ByteBuffer>();
    }

    // Compression method
    auto method = read_byte();
    if (method != 8) {
        dbgln("get_gzip_payload: Wrong compression method={}", method);
        return Optional<ByteBuffer>();
    }

    u8 flags = read_byte();

    // Timestamp, Extra flags, OS
    current += 6;

    // FEXTRA
    if (flags & 4) {
        u16 length = read_byte() & read_byte() << 8;
        dbgln("get_gzip_payload: Header has FEXTRA flag set. length={}", length);
        current += length;
    }

    // FNAME
    if (flags & 8) {
        dbgln("get_gzip_payload: Header has FNAME flag set.");
        while (read_byte() != '\0')
            ;
    }

    // FCOMMENT
    if (flags & 16) {
        dbgln("get_gzip_payload: Header has FCOMMENT flag set.");
        while (read_byte() != '\0')
            ;
    }

    // FHCRC
    if (flags & 2) {
        dbgln("get_gzip_payload: Header has FHCRC flag set.");
        current += 2;
    }

    auto new_size = data.size() - current;
    dbgln<debug_gzip>("get_gzip_payload: Returning slice from {} with size {}", current, new_size);
    return data.slice(current, new_size);
}

Optional<ByteBuffer> Gzip::decompress(const ByteBuffer& data)
{
    ASSERT(is_compressed(data));

    dbgln<debug_gzip>("Gzip::decompress: Decompressing gzip compressed data. size={}", data.size());
    auto optional_payload = get_gzip_payload(data);
    if (!optional_payload.has_value()) {
        return Optional<ByteBuffer>();
    }

    auto source = optional_payload.value();
    unsigned long source_len = source.size();
    auto destination = ByteBuffer::create_uninitialized(1024);
    while (true) {
        unsigned long destination_len = destination.size();

        if constexpr (debug_gzip) {
            dbgln("Gzip::decompress: Calling puff()");
            dbgln("  destination_data = {}", destination.data());
            dbgln("  destination_len = {}", destination_len);
            dbgln("  source_data = {}", source.data());
            dbgln("  source_len = {}", source_len);
        }

        auto puff_ret = puff(
            destination.data(), &destination_len,
            source.data(), &source_len);

        if (puff_ret == 0) {
#ifdef DEBUG_GZIP
            dbgln("Gzip::decompress: Decompression success.");
#endif
            destination.trim(destination_len);
            break;
        }

        if (puff_ret == 1) {
            // FIXME: Find a better way of decompressing without needing to try over and over again.
#ifdef DEBUG_GZIP
            dbgln("Gzip::decompress: Output buffer exhausted. Growing.");
#endif
            destination.grow(destination.size() * 2);
        } else {
            dbgln("Gzip::decompress: Error. puff() returned: {}", puff_ret);
            ASSERT_NOT_REACHED();
        }
    }

    return destination;
}

}
