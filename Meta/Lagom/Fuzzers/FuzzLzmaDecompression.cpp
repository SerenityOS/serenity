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

    // LibFuzzer has a default memory limit of 2048 MB, so limit the dictionary size to a
    // reasonable number to make sure that we don't actually run into it by allocating a
    // huge dictionary. The chosen value is double of what the largest dictionary in the
    // specifications test files is, so it should be more than enough for fuzzing everything
    // that we would want to fuzz.
    constexpr size_t largest_reasonable_dictionary_size = 16 * MiB;

    if (size >= sizeof(Compress::LzmaHeader)) {
        auto const* header = reinterpret_cast<Compress::LzmaHeader const*>(data);
        if (header->dictionary_size() > largest_reasonable_dictionary_size)
            return -1;
    }

    auto stream = make<FixedMemoryStream>(ReadonlyBytes { data, size });
    auto decompressor_or_error = Compress::LzmaDecompressor::create_from_container(move(stream));
    if (decompressor_or_error.is_error())
        return 0;
    auto decompressor = decompressor_or_error.release_value();
    while (!decompressor->is_eof()) {
        auto maybe_error = decompressor->discard(4096);
        if (maybe_error.is_error())
            break;
    }
    return 0;
}
