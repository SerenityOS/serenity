/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCompress/AsyncZlib.h>
#include <LibCompress/Zlib.h>

namespace Compress::Async {

ZlibDecompressor::ZlibDecompressor(NonnullOwnPtr<AsyncInputStream>&& input)
    : AsyncStreamTransform(move(input), decompress())
{
}

ReadonlyBytes ZlibDecompressor::buffered_data_unchecked(Badge<AsyncInputStream>) const
{
    return m_decompressor ? m_decompressor->buffered_data_unchecked(badge()) : ReadonlyBytes {};
}

void ZlibDecompressor::dequeue(Badge<AsyncInputStream>, size_t bytes)
{
    m_decompressor->dequeue(badge(), bytes);
}

auto ZlibDecompressor::decompress() -> Generator
{
    auto cmf = CO_TRY(co_await m_stream->read_object<ZlibHeader>());
    if (cmf.compression_method != ZlibCompressionMethod::Deflate || cmf.compression_info > 7) {
        m_stream->reset();
        co_return Error::from_string_literal("Non-DEFLATE compression inside Zlib is not supported");
    }
    if (cmf.present_dictionary) {
        m_stream->reset();
        co_return Error::from_string_literal("Zlib compression with a pre-defined dictionary is currently not supported");
    }
    if (cmf.as_u16 % 31 != 0) {
        m_stream->reset();
        co_return Error::from_string_literal("Zlib error correction code does not match");
    }

    Crypto::Checksum::Adler32 checksum;

    m_decompressor = make<DeflateDecompressor>(MaybeOwned { *m_stream });
    while (true) {
        auto previous_buffer_size = m_decompressor->buffered_data_unchecked(badge()).size();
        bool is_not_eof = CO_TRY(co_await m_decompressor->enqueue_some(badge()));
        if (!is_not_eof)
            break;
        auto added_bytes = m_decompressor->buffered_data_unchecked(badge()).slice(previous_buffer_size);
        checksum.update(added_bytes);
        co_yield {};
    }

    CO_TRY(co_await m_decompressor->close());

    auto stored_checksum = CO_TRY(co_await m_stream->read_object<NetworkOrdered<u32>>());
    if (stored_checksum != checksum.digest()) {
        m_stream->reset();
        co_return Error::from_string_literal("Calculated and stored checksums do not match");
    }

    co_return {};
}

}
