#include "Font.h"
#include "Peanut8x10.h"
#include "Liza8x10.h"

#define DEFAULT_FONT_NAME Liza8x10

static Font* s_default_font;

void Font::initialize()
{
    s_default_font = nullptr;
}

Font& Font::default_font()
{
    if (!s_default_font)
        s_default_font = adopt(*new Font(DEFAULT_FONT_NAME::glyphs, DEFAULT_FONT_NAME::glyph_width, DEFAULT_FONT_NAME::glyph_height, DEFAULT_FONT_NAME::first_glyph, DEFAULT_FONT_NAME::last_glyph)).leakRef();
    return *s_default_font;
}

Font::Font(const char* const* glyphs, byte glyph_width, byte glyph_height, byte first_glyph, byte last_glyph)
    : m_glyphs(glyphs)
    , m_glyph_width(glyph_width)
    , m_glyph_height(glyph_height)
    , m_first_glyph(first_glyph)
    , m_last_glyph(last_glyph)
{
    m_error_bitmap = CharacterBitmap::create_from_ascii(DEFAULT_FONT_NAME::error_glyph, m_glyph_width, m_glyph_height);
    for (unsigned ch = 0; ch < 256; ++ch) {
        if (ch < m_first_glyph || ch > m_last_glyph) {
            m_bitmaps[ch] = m_error_bitmap.copyRef();
            continue;
        }
        const char* data = m_glyphs[(unsigned)ch - m_first_glyph];
        m_bitmaps[ch] = CharacterBitmap::create_from_ascii(data, m_glyph_width, m_glyph_height);
    }
}

Font::~Font()
{
}
