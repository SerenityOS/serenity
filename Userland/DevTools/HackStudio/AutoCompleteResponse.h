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
#include <LibGUI/AutocompleteProvider.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace IPC {

template<>
inline bool encode(IPC::Encoder& encoder, const GUI::AutocompleteProvider::Entry& response)
{
    encoder << response.completion;
    encoder << (u64)response.partial_input_length;
    encoder << (u32)response.kind;
    encoder << (u32)response.language;
    return true;
}

template<>
inline bool decode(IPC::Decoder& decoder, GUI::AutocompleteProvider::Entry& response)
{
    u32 kind = 0;
    u32 language = 0;
    u64 partial_input_length = 0;
    bool ok = decoder.decode(response.completion)
        && decoder.decode(partial_input_length)
        && decoder.decode(kind)
        && decoder.decode(language);

    if (ok) {
        response.kind = static_cast<GUI::AutocompleteProvider::CompletionKind>(kind);
        response.language = static_cast<GUI::AutocompleteProvider::Language>(language);
        response.partial_input_length = partial_input_length;
    }

    return ok;
}

template<>
inline bool encode(Encoder& encoder, const GUI::AutocompleteProvider::ProjectLocation& location)
{
    encoder << location.file;
    encoder << (u64)location.line;
    encoder << (u64)location.column;
    return true;
}

template<>
inline bool decode(Decoder& decoder, GUI::AutocompleteProvider::ProjectLocation& location)
{
    u64 line = 0;
    u64 column = 0;
    if (!decoder.decode(location.file))
        return false;
    if (!decoder.decode(line))
        return false;
    if (!decoder.decode(column))
        return false;

    location.line = line;
    location.column = column;

    return true;
}

template<>
inline bool encode(Encoder& encoder, const GUI::AutocompleteProvider::Declaration& declaration)
{
    encoder << declaration.name;
    if (!encode(encoder, declaration.position))
        return false;
    encoder << (u32)declaration.type;
    return true;
}

template<>
inline bool decode(Decoder& decoder, GUI::AutocompleteProvider::Declaration& declaration)
{
    if (!decoder.decode(declaration.name))
        return false;

    if (!decode(decoder, declaration.position))
        return false;
    u32 type;
    if (!decoder.decode( type))
        return false;

    declaration.type = static_cast<GUI::AutocompleteProvider::DeclarationType>(type);
    return true;
}

}
