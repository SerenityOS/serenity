/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/QOALoader.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    auto qoa_data = ByteBuffer::copy(data, size).release_value();
    auto qoa_or_error = Audio::QOALoaderPlugin::create(qoa_data.bytes());

    if (qoa_or_error.is_error())
        return 0;

    auto qoa = qoa_or_error.release_value();

    for (;;) {
        auto samples = qoa->load_chunks(5 * KiB);
        if (samples.is_error())
            return 0;
        if (samples.value().size() == 0)
            break;
    }

    return 0;
}
