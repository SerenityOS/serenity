#pragma once

#include "CharacterBitmap.h"
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <AK/Types.h>

class Font : public Retainable<Font> {
public:
    static Font& default_font();

    ~Font();

    const CharacterBitmap* glyph_bitmap(byte) const;

    byte glyph_width() const { return m_glyph_width; }
    byte glyph_height() const { return m_glyph_height; }

    static void initialize();

private:
    Font(const char* const* glyphs, byte glyph_width, byte glyph_height, byte firstGlyph, byte lastGlyph);

    const char* const* m_glyphs { nullptr };
    mutable RetainPtr<CharacterBitmap> m_bitmaps[256];

    byte m_glyph_width { 0 };
    byte m_glyph_height { 0 };

    byte m_first_glyph { 0 };
    byte m_last_glyph { 0 };
};
