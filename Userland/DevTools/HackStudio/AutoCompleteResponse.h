/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Types.h>
#include <LibCpp/Parser.h>
#include <LibGUI/AutocompleteProvider.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace IPC {

template<>
inline bool encode(Encoder& encoder, CodeComprehension::AutocompleteResultEntry const& response)
{
    encoder << response.completion;
    encoder << response.partial_input_length;
    encoder << response.language;
    encoder << response.display_text;
    encoder << response.hide_autocomplete_after_applying;
    return true;
}

template<>
inline ErrorOr<void> decode(Decoder& decoder, CodeComprehension::AutocompleteResultEntry& response)
{
    TRY(decoder.decode(response.completion));
    TRY(decoder.decode(response.partial_input_length));
    TRY(decoder.decode(response.language));
    TRY(decoder.decode(response.display_text));
    TRY(decoder.decode(response.hide_autocomplete_after_applying));
    return {};
}

template<>
inline bool encode(Encoder& encoder, CodeComprehension::ProjectLocation const& location)
{
    encoder << location.file;
    encoder << location.line;
    encoder << location.column;
    return true;
}

template<>
inline ErrorOr<void> decode(Decoder& decoder, CodeComprehension::ProjectLocation& location)
{
    TRY(decoder.decode(location.file));
    TRY(decoder.decode(location.line));
    TRY(decoder.decode(location.column));
    return {};
}

template<>
inline bool encode(Encoder& encoder, CodeComprehension::Declaration const& declaration)
{
    encoder << declaration.name;
    if (!encode(encoder, declaration.position))
        return false;
    encoder << declaration.type;
    encoder << declaration.scope;
    return true;
}

template<>
inline ErrorOr<void> decode(Decoder& decoder, CodeComprehension::Declaration& declaration)
{
    TRY(decoder.decode(declaration.name));
    TRY(decoder.decode(declaration.position));
    TRY(decoder.decode(declaration.type));
    TRY(decoder.decode(declaration.scope));
    return {};
}

template<>
inline bool encode(Encoder& encoder, CodeComprehension::TodoEntry const& entry)
{
    encoder << entry.content;
    encoder << entry.filename;
    encoder << entry.line;
    encoder << entry.column;
    return true;
}

template<>
inline ErrorOr<void> decode(Decoder& decoder, CodeComprehension::TodoEntry& entry)
{
    TRY(decoder.decode(entry.content));
    TRY(decoder.decode(entry.filename));
    TRY(decoder.decode(entry.line));
    TRY(decoder.decode(entry.column));
    return {};
}

template<>
inline bool encode(Encoder& encoder, CodeComprehension::TokenInfo const& location)
{
    encoder << (u32)location.type;
    static_assert(sizeof(location.type) == sizeof(u32));
    encoder << location.start_line;
    encoder << location.start_column;
    encoder << location.end_line;
    encoder << location.end_column;
    return true;
}

template<>
inline ErrorOr<void> decode(Decoder& decoder, CodeComprehension::TokenInfo& entry)
{
    u32 semantic_type { 0 };
    static_assert(sizeof(semantic_type) == sizeof(entry.type));

    TRY(decoder.decode(semantic_type));
    entry.type = static_cast<CodeComprehension::TokenInfo::SemanticType>(semantic_type);
    TRY(decoder.decode(entry.start_line));
    TRY(decoder.decode(entry.start_column));
    TRY(decoder.decode(entry.end_line));
    TRY(decoder.decode(entry.end_column));
    return {};
}

}
