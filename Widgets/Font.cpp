#include "Font.h"
#include "Peanut8x10.h"
#include <AK/RetainPtr.h>
#include <cstdio>

Font& Font::defaultFont()
{
    static auto* f = adopt(*new Font(Peanut8x10::glyphs, Peanut8x10::glyphWidth, Peanut8x10::glyphHeight, Peanut8x10::firstGlyph, Peanut8x10::lastGlyph)).leakRef();
    return *f;
}

Font::Font(const char* const* glyphs, byte glyphWidth, byte glyphHeight, byte firstGlyph, byte lastGlyph)
    : m_glyphs(glyphs)
    , m_glyphWidth(glyphWidth)
    , m_glyphHeight(glyphHeight)
    , m_firstGlyph(firstGlyph)
    , m_lastGlyph(lastGlyph)
{
}

Font::~Font()
{
}

const CharacterBitmap* Font::glyphBitmap(byte ch) const
{
    if (!m_bitmaps[ch]) {
        if (ch < m_firstGlyph || ch > m_lastGlyph)
            return nullptr;
        const char* data = m_glyphs[(unsigned)ch - m_firstGlyph];
        m_bitmaps[ch] = CharacterBitmap::createFromASCII(data, m_glyphWidth, m_glyphHeight);
    }
    ASSERT(ch >= m_firstGlyph && ch <= m_lastGlyph);
    return m_bitmaps[ch].ptr();
}
