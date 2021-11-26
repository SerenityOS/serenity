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
    encoder << (u64)response.partial_input_length;
    encoder << (u32)response.language;
    encoder << response.display_text;
    encoder << (u32)response.hide_autocomplete_after_applying;
    return true;
}

template<>
inline bool decode(IPC::Decoder& decoder, GUI::AutocompleteProvider::Entry& response)
{
    u32 language = 0;
    u64 partial_input_length = 0;
    u32 hide_autocomplete_after_applying = 0;
    bool ok = decoder.decode(response.completion)
        && decoder.decode(partial_input_length)
        && decoder.decode(language)
        && decoder.decode(response.display_text)
        && decoder.decode(hide_autocomplete_after_applying);

    if (ok) {
        response.language = static_cast<GUI::AutocompleteProvider::Language>(language);
        response.partial_input_length = partial_input_length;
        response.hide_autocomplete_after_applying = static_cast<GUI::AutocompleteProvider::Entry::HideAutocompleteAfterApplying>(hide_autocomplete_after_applying);
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
    encoder << declaration.scope;
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
    if (!decoder.decode(type))
        return false;

    if (!decoder.decode(declaration.scope))
        return false;

    declaration.type = static_cast<GUI::AutocompleteProvider::DeclarationType>(type);
    return true;
}

template<>
inline bool encode(Encoder& encoder, Cpp::Parser::TodoEntry const& entry)
{
    encoder << entry.content;
    encoder << entry.filename;
    encoder << (u64)entry.line;
    encoder << (u64)entry.column;
    return true;
}

template<>
inline bool decode(Decoder& decoder, Cpp::Parser::TodoEntry& entry)
{
    u64 line = 0;
    u64 column = 0;
    if (!decoder.decode(entry.content))
        return false;
    if (!decoder.decode(entry.filename))
        return false;
    if (!decoder.decode(line))
        return false;
    if (!decoder.decode(column))
        return false;

    entry.line = line;
    entry.column = column;
    return true;
}

}
