#pragma once

#include "CharacterBitmap.h"
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <AK/AKString.h>
#include <AK/Types.h>

// FIXME: Make a MutableGlyphBitmap buddy class for FontEditor instead?
class GlyphBitmap {
    friend class Font;
public:
    const unsigned* rows() const { return m_rows; }
    unsigned row(unsigned index) const { return m_rows[index]; }

    bool bit_at(int x, int y) const { return row(y) & (1 << x); }
    void set_bit_at(int x, int y, bool b)
    {
        auto& mutable_row = const_cast<unsigned*>(m_rows)[y];
        if (b)
            mutable_row |= 1 << x;
        else
            mutable_row &= ~(1 << x);
    }

    Size size() const { return m_size; }
    int width() const { return m_size.width(); }
    int height() const { return m_size.height(); }

private:
    GlyphBitmap(const unsigned* rows, Size size)
        : m_rows(rows)
        , m_size(size)
    {
    }

    const unsigned* m_rows { nullptr };
    Size m_size;
};

class Font : public Retainable<Font> {
public:
    static Font& default_font();
    static Font& default_bold_font();

    RetainPtr<Font> clone() const;

    static RetainPtr<Font> load_from_memory(const byte*);

    static RetainPtr<Font> load_from_file(const String& path);
    bool write_to_file(const String& path);

    ~Font();

    GlyphBitmap glyph_bitmap(char ch) const { return GlyphBitmap(&m_rows[(byte)ch * m_glyph_height], { m_glyph_width, m_glyph_height }); }

    byte glyph_width() const { return m_glyph_width; }
    byte glyph_height() const { return m_glyph_height; }
    int width(const String& string) const { return string.length() * glyph_width(); }

    String name() const { return m_name; }
    void set_name(const String& name) { m_name = name; }

private:
    Font(const String& name, unsigned* rows, byte glyph_width, byte glyph_height);

    String m_name;

    unsigned* m_rows { nullptr };
    void* m_mmap_ptr { nullptr };

    RetainPtr<CharacterBitmap> m_error_bitmap;

    byte m_glyph_width { 0 };
    byte m_glyph_height { 0 };
};
