/*
 * Copyright (c) 2023, Martin Janiczek <martin@janiczek.cz>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/String.h>

/* Chunk is a description of a RandomRun slice.
Used to say which part of a given RandomRun will be shrunk by some ShrinkCmd.

Example:

    We're trying to shrink RandomRun [5,1,3,18,4,2,3,0] (length: 8).
    Many ShrinkCmds get created, among which are these:
        - ZeroChunk(Chunk{m_size = 4, m_index = 1})
            - results in RandomRun [5,0,0,0,0,2,3,0]
        - SortChunk(Chunk{m_size = 3, m_index = 3})
            - results in RandomRun [5,1,3,2,4,18,3,0]
        - DeleteChunkAndMaybeDecPrevious(Chunk{m_size = 4, m_index = 4})
            - results in RandomRun [5,1,3,18]

*/
struct Chunk {
    uint8_t size;
    size_t index;

    ErrorOr<String> to_string() const
    {
        return String::formatted("Chunk<size={}, i={}>", size, index);
    }
};

template<>
struct AK::Formatter<Chunk> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Chunk chunk)
    {
        return Formatter<StringView>::format(builder, TRY(chunk.to_string()));
    }
};
