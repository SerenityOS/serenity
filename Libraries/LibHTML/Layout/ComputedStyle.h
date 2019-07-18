#pragma once

#include <LibHTML/CSS/LengthBox.h>
#include <LibDraw/Color.h>
#include <LibDraw/Size.h>

enum FontStyle {
    Normal,
    Bold,
};

class ComputedStyle {
public:
    ComputedStyle();
    ~ComputedStyle();

    Color text_color() const { return m_text_color; }
    Color background_color() const { return m_background_color; }

    LengthBox& offset() { return m_offset; }
    LengthBox& margin() { return m_margin; }
    LengthBox& padding() { return m_padding; }
    LengthBox& border() { return m_border; }

    const LengthBox& offset() const { return m_offset; }
    const LengthBox& margin() const { return m_margin; }
    const LengthBox& padding() const { return m_padding; }
    const LengthBox& border() const { return m_border; }

    FontStyle font_style() const { return m_font_style; }

    const Size& size() const { return m_size; }
    Size& size() { return m_size; }

private:
    Color m_text_color;
    Color m_background_color;

    LengthBox m_offset;
    LengthBox m_margin;
    LengthBox m_padding;
    LengthBox m_border;

    Size m_size;

    FontStyle m_font_style { FontStyle::Normal };
};
