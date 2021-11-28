/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Types.h>
#include <LibCpp/Parser.h>
#include <LibGUI/AutocompleteProvider.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace IPC {

template<>
inline bool encode(IPC::Encoder& encoder, const GUI::AutocompleteProvider::Entry& response)
{
    encoder << response.completion;
    encoder << response.partial_input_length;
    encoder << response.language;
    encoder << response.display_text;
    encoder << response.hide_autocomplete_after_applying;
    return true;
}

template<>
inline ErrorOr<void> decode(IPC::Decoder& decoder, GUI::AutocompleteProvider::Entry& response)
{
    TRY(decoder.decode(response.completion));
    TRY(decoder.decode(response.partial_input_length));
    TRY(decoder.decode(response.language));
    TRY(decoder.decode(response.display_text));
    TRY(decoder.decode(response.hide_autocomplete_after_applying));
    return {};
}

template<>
inline bool encode(Encoder& encoder, const GUI::AutocompleteProvider::ProjectLocation& location)
{
    encoder << location.file;
    encoder << location.line;
    encoder << location.column;
    return true;
}

template<>
inline ErrorOr<void> decode(Decoder& decoder, GUI::AutocompleteProvider::ProjectLocation& location)
{
    TRY(decoder.decode(location.file));
    TRY(decoder.decode(location.line));
    TRY(decoder.decode(location.column));
    return {};
}

template<>
inline bool encode(Encoder& encoder, const GUI::AutocompleteProvider::Declaration& declaration)
{
    encoder << declaration.name;
    if (!encode(encoder, declaration.position))
        return false;
    encoder << declaration.type;
    encoder << declaration.scope;
    return true;
}

template<>
inline ErrorOr<void> decode(Decoder& decoder, GUI::AutocompleteProvider::Declaration& declaration)
{
    TRY(decoder.decode(declaration.name));
    TRY(decoder.decode(declaration.position));
    TRY(decoder.decode(declaration.type));
    TRY(decoder.decode(declaration.scope));
    return {};
}

template<>
inline bool encode(Encoder& encoder, Cpp::Parser::TodoEntry const& entry)
{
    encoder << entry.content;
    encoder << entry.filename;
    encoder << entry.line;
    encoder << entry.column;
    return true;
}

template<>
inline ErrorOr<void> decode(Decoder& decoder, Cpp::Parser::TodoEntry& entry)
{
    TRY(decoder.decode(entry.content));
    TRY(decoder.decode(entry.filename));
    TRY(decoder.decode(entry.line));
    TRY(decoder.decode(entry.column));
    return {};
}

}
