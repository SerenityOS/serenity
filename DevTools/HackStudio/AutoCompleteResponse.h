/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/String.h>
#include <AK/Types.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace HackStudio {

enum class CompletionKind {
    Identifier,
};

struct AutoCompleteResponse {
    String completion;
    size_t partial_input_length { 0 };
    CompletionKind kind { CompletionKind::Identifier };
};

}

namespace IPC {

template<>
inline bool encode(IPC::Encoder& encoder, const HackStudio::AutoCompleteResponse& response)
{
    encoder << response.completion;
    encoder << (u64)response.partial_input_length;
    encoder << (u32)response.kind;
    return true;
}

template<>
inline bool decode(IPC::Decoder& decoder, HackStudio::AutoCompleteResponse& response)
{
    u32 kind = 0;
    u64 partial_input_length = 0;
    bool ok = decoder.decode(response.completion)
        && decoder.decode(partial_input_length)
        && decoder.decode(kind);

    if (ok) {
        response.kind = static_cast<HackStudio::CompletionKind>(kind);
        response.partial_input_length = partial_input_length;
    }

    return ok;
}

}
