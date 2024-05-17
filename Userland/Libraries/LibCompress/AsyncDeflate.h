/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AsyncBitStream.h>
#include <AK/AsyncStreamTransform.h>
#include <AK/StreamBuffer.h>

namespace Compress::Async {

class DeflateDecompressor final : public AsyncStreamTransform<AsyncInputLittleEndianBitStream> {
public:
    DeflateDecompressor(NonnullOwnPtr<AsyncInputStream>&& input);

    ReadonlyBytes buffered_data_unchecked(Badge<AsyncInputStream>) const override;
    void dequeue(Badge<AsyncInputStream>, size_t bytes) override;

private:
    Generator decompress();

    StreamSeekbackBuffer m_buffer;
};

}
