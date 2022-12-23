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
inline ErrorOr<CodeComprehension::AutocompleteResultEntry> decode(Decoder& decoder)
{
    auto completion = TRY(decoder.decode<DeprecatedString>());
    auto partial_input_length = TRY(decoder.decode<size_t>());
    auto language = TRY(decoder.decode<CodeComprehension::Language>());
    auto display_text = TRY(decoder.decode<DeprecatedString>());
    auto hide_autocomplete_after_applying = TRY(decoder.decode<CodeComprehension::AutocompleteResultEntry::HideAutocompleteAfterApplying>());

    return CodeComprehension::AutocompleteResultEntry { move(completion), partial_input_length, language, move(display_text), hide_autocomplete_after_applying };
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
inline ErrorOr<CodeComprehension::ProjectLocation> decode(Decoder& decoder)
{
    auto file = TRY(decoder.decode<DeprecatedString>());
    auto line = TRY(decoder.decode<size_t>());
    auto column = TRY(decoder.decode<size_t>());

    return CodeComprehension::ProjectLocation { move(file), line, column };
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
inline ErrorOr<CodeComprehension::Declaration> decode(Decoder& decoder)
{
    auto name = TRY(decoder.decode<DeprecatedString>());
    auto position = TRY(decoder.decode<CodeComprehension::ProjectLocation>());
    auto type = TRY(decoder.decode<CodeComprehension::DeclarationType>());
    auto scope = TRY(decoder.decode<DeprecatedString>());

    return CodeComprehension::Declaration { move(name), position, type, move(scope) };
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
inline ErrorOr<CodeComprehension::TodoEntry> decode(Decoder& decoder)
{
    auto content = TRY(decoder.decode<DeprecatedString>());
    auto filename = TRY(decoder.decode<DeprecatedString>());
    auto line = TRY(decoder.decode<size_t>());
    auto column = TRY(decoder.decode<size_t>());

    return CodeComprehension::TodoEntry { move(content), move(filename), line, column };
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
inline ErrorOr<CodeComprehension::TokenInfo> decode(Decoder& decoder)
{
    auto type = TRY(decoder.decode<CodeComprehension::TokenInfo::SemanticType>());
    auto start_line = TRY(decoder.decode<size_t>());
    auto start_column = TRY(decoder.decode<size_t>());
    auto end_line = TRY(decoder.decode<size_t>());
    auto end_column = TRY(decoder.decode<size_t>());

    return CodeComprehension::TokenInfo { type, start_line, start_column, end_line, end_column };
}

}
