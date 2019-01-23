#include "Font.h"
#include "Peanut8x10.h"

static Font* s_default_font;

void Font::initialize()
{
    s_default_font = nullptr;
}

Font& Font::default_font()
{
    if (!s_default_font)
        s_default_font = adopt(*new Font(Peanut8x10::glyphs, Peanut8x10::glyph_width, Peanut8x10::glyph_height, Peanut8x10::first_glyph, Peanut8x10::last_glyph)).leakRef();
    return *s_default_font;
}

Font::Font(const char* const* glyphs, byte glyph_width, byte glyph_height, byte first_glyph, byte last_glyph)
    : m_glyphs(glyphs)
    , m_glyph_width(glyph_width)
    , m_glyph_height(glyph_height)
    , m_first_glyph(first_glyph)
    , m_last_glyph(last_glyph)
{
    m_error_bitmap = CharacterBitmap::create_from_ascii(Peanut8x10::error_glyph, m_glyph_width, m_glyph_height);
}

Font::~Font()
{
}

const CharacterBitmap* Font::glyph_bitmap(byte ch) const
{
    if (!m_bitmaps[ch]) {
        if (ch < m_first_glyph || ch > m_last_glyph)
            return nullptr;
        const char* data = m_glyphs[(unsigned)ch - m_first_glyph];
        m_bitmaps[ch] = CharacterBitmap::create_from_ascii(data, m_glyph_width, m_glyph_height);
    }
    ASSERT(ch >= m_first_glyph && ch <= m_last_glyph);
    return m_bitmaps[ch].ptr();
}
