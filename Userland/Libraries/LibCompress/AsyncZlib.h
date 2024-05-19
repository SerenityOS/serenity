/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AsyncStreamTransform.h>
#include <AK/OwnPtr.h>
#include <LibCompress/AsyncDeflate.h>

namespace Compress::Async {

class ZlibDecompressor final : public AsyncStreamTransform<AsyncInputStream> {
public:
    ZlibDecompressor(NonnullOwnPtr<AsyncInputStream>&& input);

    ReadonlyBytes buffered_data_unchecked(Badge<AsyncInputStream>) const override;
    void dequeue(Badge<AsyncInputStream>, size_t bytes) override;

private:
    Generator decompress();

    OwnPtr<DeflateDecompressor> m_decompressor;
};

}
