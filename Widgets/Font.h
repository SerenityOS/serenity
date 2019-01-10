#pragma once

#include "CharacterBitmap.h"
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <AK/Types.h>

class Font : public Retainable<Font> {
public:
    static Font& defaultFont();

    ~Font();

    const CharacterBitmap* glyphBitmap(byte) const;

    byte glyphWidth() const { return m_glyphWidth; }
    byte glyphHeight() const { return m_glyphHeight; }

    static void initialize();

private:
    Font(const char* const* glyphs, byte glyphWidth, byte glyphHeight, byte firstGlyph, byte lastGlyph);

    const char* const* m_glyphs { nullptr };
    mutable RetainPtr<CharacterBitmap> m_bitmaps[256];

    byte m_glyphWidth { 0 };
    byte m_glyphHeight { 0 };

    byte m_firstGlyph { 0 };
    byte m_lastGlyph { 0 };
};
