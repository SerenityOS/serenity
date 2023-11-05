/*
 * Copyright (c) 2023, Tim Schumacher <timschumi@gmx.de>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibCompress/Lzma.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    AK::set_debug_enabled(false);

    AllocatingMemoryStream stream {};

    auto compressor = MUST(Compress::LzmaCompressor::create_container(MaybeOwned<Stream> { stream }, {}));
    MUST(compressor->write_until_depleted({ data, size }));
    MUST(compressor->flush());

    auto decompressor = MUST(Compress::LzmaDecompressor::create_from_container(MaybeOwned<Stream> { stream }));
    auto result = MUST(decompressor->read_until_eof());

    VERIFY((ReadonlyBytes { data, size }) == result.span());

    return 0;
}
