/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibTextCodec/Decoder.h>

namespace TextCodec {

static Latin1Decoder& latin1_decoder()
{
    static Latin1Decoder* decoder;
    if (!decoder)
        decoder = new Latin1Decoder;
    return *decoder;
}

static UTF8Decoder& utf8_decoder()
{
    static UTF8Decoder* decoder;
    if (!decoder)
        decoder = new UTF8Decoder;
    return *decoder;
}

Decoder* decoder_for(const String& encoding)
{
    if (encoding.equals_ignoring_case("iso-8859-1"))
        return &latin1_decoder();
    if (encoding.equals_ignoring_case("utf-8"))
        return &utf8_decoder();
    return nullptr;
}

String UTF8Decoder::to_utf8(const StringView& input)
{
    return input;
}

String Latin1Decoder::to_utf8(const StringView& input)
{
    StringBuilder builder(input.length());
    for (size_t i = 0; i < input.length(); ++i) {
        u8 ch = input[i];
        // Latin1 is the same as the first 256 Unicode code_points, so no mapping is needed, just utf-8 encoding.
        builder.append_code_point(ch);
    }
    return builder.to_string();
}

}
