/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <LibGUI/AutocompleteProvider.h>
#include <LibIPC/Forward.h>

namespace HackStudio {

struct Diagnostic {
    enum class Level {
        Error,
        Warning,
        Note,
        Info,
    };

    GUI::AutocompleteProvider::ProjectLocation start_position;
    GUI::AutocompleteProvider::ProjectLocation end_position;
    String text;
    Level level;
};

}

namespace IPC {
template<>
inline bool encode(Encoder& encoder, HackStudio::Diagnostic const& entry)
{
    encoder << entry.start_position;
    encoder << entry.end_position;
    encoder << entry.text;
    encoder << entry.level;
    return true;
}

template<>
inline ErrorOr<void> decode(Decoder& decoder, HackStudio::Diagnostic& entry)
{
    TRY(decoder.decode(entry.start_position));
    TRY(decoder.decode(entry.end_position));
    TRY(decoder.decode(entry.text));
    TRY(decoder.decode(entry.level));
    return {};
}
}
