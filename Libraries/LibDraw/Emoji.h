#pragma once

#include <AK/Types.h>
#include <AK/NonnullRefPtr.h>

class GraphicsBitmap;

class Emoji {
 public:
    ~Emoji() {}

    static const Emoji* emoji_for_codepoint(u32 codepoint);
    const GraphicsBitmap& bitmap() const { return m_bitmap; }

private:
    explicit Emoji(NonnullRefPtr<GraphicsBitmap>);

    NonnullRefPtr<GraphicsBitmap> m_bitmap;
};
