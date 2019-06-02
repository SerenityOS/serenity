#pragma once

#include <SharedGraphics/Rect.h>
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <AK/AKString.h>
#include <AK/MappedFile.h>
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

    static Font& default_fixed_width_font();

    RetainPtr<Font> clone() const;

    static RetainPtr<Font> load_from_file(const StringView& path);
    bool write_to_file(const StringView& path);

    ~Font();

    GlyphBitmap glyph_bitmap(char ch) const { return GlyphBitmap(&m_rows[(byte)ch * m_glyph_height], { glyph_width(ch), m_glyph_height }); }

    byte glyph_width(char ch) const { return m_fixed_width ? m_glyph_width : m_glyph_widths[(byte)ch]; }
    byte glyph_height() const { return m_glyph_height; }
    byte min_glyph_width() const { return m_min_glyph_width; }
    byte max_glyph_width() const { return m_max_glyph_width; }
    byte glyph_spacing() const { return m_fixed_width ? 0 : 1; }
    int width(const StringView& string) const;

    String name() const { return m_name; }
    void set_name(const StringView& name) { m_name = name; }

    bool is_fixed_width() const { return m_fixed_width; }
    void set_fixed_width(bool b) { m_fixed_width = b; }

    void set_glyph_width(char ch, byte width)
    {
        ASSERT(m_glyph_widths);
        m_glyph_widths[(byte)ch] = width;
    }

private:
    Font(const StringView& name, unsigned* rows, byte* widths, bool is_fixed_width, byte glyph_width, byte glyph_height);

    static RetainPtr<Font> load_from_memory(const byte*);

    String m_name;

    unsigned* m_rows { nullptr };
    byte* m_glyph_widths { nullptr };
    MappedFile m_mapped_file;

    byte m_glyph_width { 0 };
    byte m_glyph_height { 0 };
    byte m_min_glyph_width { 0 };
    byte m_max_glyph_width { 0 };

    bool m_fixed_width { false };
};
