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

const char* Font::glyph(char ch) const
{
    if (ch < m_firstGlyph || ch > m_lastGlyph)
        return nullptr;
    return m_glyphs[(unsigned)ch - m_firstGlyph];
}

