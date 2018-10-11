#pragma once

#include <AK/Retainable.h>
#include <AK/Types.h>

class Font : public Retainable<Font> {
public:
    static Font& defaultFont();

    ~Font();

    const char* glyph(char) const;

    unsigned glyphWidth() const { return m_glyphWidth; }
    unsigned glyphHeight() const { return m_glyphHeight; }

private:
    Font(const char* const* glyphs, unsigned glyphWidth, unsigned glyphHeight, byte firstGlyph, byte lastGlyph);

    const char* const* m_glyphs { nullptr };

    unsigned m_glyphWidth { 0 };
    unsigned m_glyphHeight { 0 };

    byte m_firstGlyph { 0 };
    byte m_lastGlyph { 0 };
};
