/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Types.h>
#include <LibCpp/Parser.h>
#include <LibGUI/AutocompleteProvider.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace IPC {

template<>
inline ErrorOr<void> encode(Encoder& encoder, CodeComprehension::AutocompleteResultEntry const& response)
{
    TRY(encoder.encode(response.completion));
    TRY(encoder.encode(response.partial_input_length));
    TRY(encoder.encode(response.language));
    TRY(encoder.encode(response.display_text));
    TRY(encoder.encode(response.hide_autocomplete_after_applying));
    return {};
}

template<>
inline ErrorOr<CodeComprehension::AutocompleteResultEntry> decode(Decoder& decoder)
{
    auto completion = TRY(decoder.decode<ByteString>());
    auto partial_input_length = TRY(decoder.decode<size_t>());
    auto language = TRY(decoder.decode<CodeComprehension::Language>());
    auto display_text = TRY(decoder.decode<ByteString>());
    auto hide_autocomplete_after_applying = TRY(decoder.decode<CodeComprehension::AutocompleteResultEntry::HideAutocompleteAfterApplying>());

    return CodeComprehension::AutocompleteResultEntry { move(completion), partial_input_length, language, move(display_text), hide_autocomplete_after_applying };
}

template<>
inline ErrorOr<void> encode(Encoder& encoder, CodeComprehension::ProjectLocation const& location)
{
    TRY(encoder.encode(location.file));
    TRY(encoder.encode(location.line));
    TRY(encoder.encode(location.column));
    return {};
}

template<>
inline ErrorOr<CodeComprehension::ProjectLocation> decode(Decoder& decoder)
{
    auto file = TRY(decoder.decode<ByteString>());
    auto line = TRY(decoder.decode<size_t>());
    auto column = TRY(decoder.decode<size_t>());

    return CodeComprehension::ProjectLocation { move(file), line, column };
}

template<>
inline ErrorOr<void> encode(Encoder& encoder, CodeComprehension::Declaration const& declaration)
{
    TRY(encoder.encode(declaration.name));
    TRY(encoder.encode(declaration.position));
    TRY(encoder.encode(declaration.type));
    TRY(encoder.encode(declaration.scope));
    return {};
}

template<>
inline ErrorOr<CodeComprehension::Declaration> decode(Decoder& decoder)
{
    auto name = TRY(decoder.decode<ByteString>());
    auto position = TRY(decoder.decode<CodeComprehension::ProjectLocation>());
    auto type = TRY(decoder.decode<CodeComprehension::DeclarationType>());
    auto scope = TRY(decoder.decode<ByteString>());

    return CodeComprehension::Declaration { move(name), position, type, move(scope) };
}

template<>
inline ErrorOr<void> encode(Encoder& encoder, CodeComprehension::TodoEntry const& entry)
{
    TRY(encoder.encode(entry.content));
    TRY(encoder.encode(entry.filename));
    TRY(encoder.encode(entry.line));
    TRY(encoder.encode(entry.column));
    return {};
}

template<>
inline ErrorOr<CodeComprehension::TodoEntry> decode(Decoder& decoder)
{
    auto content = TRY(decoder.decode<ByteString>());
    auto filename = TRY(decoder.decode<ByteString>());
    auto line = TRY(decoder.decode<size_t>());
    auto column = TRY(decoder.decode<size_t>());

    return CodeComprehension::TodoEntry { move(content), move(filename), line, column };
}

template<>
inline ErrorOr<void> encode(Encoder& encoder, CodeComprehension::TokenInfo const& location)
{
    TRY(encoder.encode(location.type));
    TRY(encoder.encode(location.start_line));
    TRY(encoder.encode(location.start_column));
    TRY(encoder.encode(location.end_line));
    TRY(encoder.encode(location.end_column));
    return {};
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
