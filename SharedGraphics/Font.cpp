#include "Font.h"
#include "Peanut8x10.h"
#include "Liza8x10.h"

#define DEFAULT_FONT_NAME Liza8x10

static const byte error_glyph_width = 8;
static const byte error_glyph_height = 10;
static constexpr const char* error_glyph {
    "  ####  "
    " #    # "
    " #    # "
    " # ## # "
    " # ## # "
    "  ####  "
    "   ##   "
    " ###### "
    "   ##   "
    "   ##   ",
};

static Font* s_default_font;

void Font::initialize()
{
    s_default_font = nullptr;
}

Font& Font::default_font()
{
    if (!s_default_font)
        s_default_font = adopt(*new Font(DEFAULT_FONT_NAME::glyphs, DEFAULT_FONT_NAME::glyph_width, DEFAULT_FONT_NAME::glyph_height, DEFAULT_FONT_NAME::first_glyph, DEFAULT_FONT_NAME::last_glyph)).leak_ref();
    return *s_default_font;
}

RetainPtr<Font> Font::clone() const
{
    size_t bytes_per_glyph = glyph_width() * glyph_height();
    // FIXME: This is leaked!
    char** new_glyphs = static_cast<char**>(malloc(sizeof(char*) * 256));
    for (unsigned i = 0; i < 256; ++i) {
        new_glyphs[i] = static_cast<char*>(malloc(bytes_per_glyph));
        if (i >= m_first_glyph && i <= m_last_glyph) {
            memcpy(new_glyphs[i], m_glyphs[i - m_first_glyph], bytes_per_glyph);
        } else {
            memset(new_glyphs[i], ' ', bytes_per_glyph);
        }
    }
    return adopt(*new Font(new_glyphs, m_glyph_width, m_glyph_height, 0, 255));
}

Font::Font(const char* const* glyphs, byte glyph_width, byte glyph_height, byte first_glyph, byte last_glyph)
    : m_glyphs(glyphs)
    , m_glyph_width(glyph_width)
    , m_glyph_height(glyph_height)
    , m_first_glyph(first_glyph)
    , m_last_glyph(last_glyph)
{
    ASSERT(m_glyph_width == error_glyph_width);
    ASSERT(m_glyph_height == error_glyph_height);
    m_error_bitmap = CharacterBitmap::create_from_ascii(error_glyph, error_glyph_width, error_glyph_height);
    for (unsigned ch = 0; ch < 256; ++ch) {
        if (ch < m_first_glyph || ch > m_last_glyph) {
            m_bitmaps[ch] = m_error_bitmap.copy_ref();
            continue;
        }
        const char* data = m_glyphs[(unsigned)ch - m_first_glyph];
        m_bitmaps[ch] = CharacterBitmap::create_from_ascii(data, m_glyph_width, m_glyph_height);
    }
}

Font::~Font()
{
}
