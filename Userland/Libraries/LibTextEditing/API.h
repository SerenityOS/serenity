/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Utf8View.h>
#include <LibTextEditing/Forward.h>

namespace TextEditing {

// In LibTextEditing, all positioning information is on a text character basis, so line and column or variations on those.

struct Position {
    unsigned column;
    // NOT the line in the document, just the row in the viewport!
    unsigned row;
};

struct FileLocation {
    unsigned column;
    unsigned line;
};

using Infinity = Empty;

struct Viewport {
    // Height in text lines.
    unsigned height;
    // If there is no automatic line break (common in GUI text editors), the viewport width is considered infinite.
    AK::Variant<unsigned, Infinity> width;
};

// How scrolling should happen when the viewport is changed.
enum class VerticalScrollBehavior {
    // Keep the lines in the middle of the viewport in the middle.
    Center,
    // Keep the lines at the top of the viewport fixed.
    FixTop,
    // Keep the lines at the bottom of the viewport fixed.
    FixBottom,
};

struct Range {
    // Inclusive.
    Position start;
    // Exclusive.
    Position end;
};

}
