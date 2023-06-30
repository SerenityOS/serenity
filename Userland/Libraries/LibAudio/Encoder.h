/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Span.h>
#include <LibAudio/Sample.h>

namespace Audio {

class Encoder {
public:
    virtual ~Encoder() = default;

    // Encodes the given samples and writes them to the output stream.
    // Note that due to format restrictions, not all samples might be written immediately, this is only guaranteed after a call to finalize().
    virtual ErrorOr<void> write_samples(ReadonlySpan<Sample> samples) = 0;

    // Finalizes the stream, future calls to write_samples() will cause an error.
    // This method makes sure that all samples are encoded and written out.
    // This method is called in the destructor, but since this can error, you should call this function yourself before disposing of the decoder.
    virtual ErrorOr<void> finalize() = 0;
};

}
