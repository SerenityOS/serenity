#pragma once

#include <AK/Types.h>

class GraphicsBitmap;

class Emoji {
public:
    static const GraphicsBitmap* emoji_for_codepoint(u32 codepoint);
};
