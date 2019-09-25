#pragma once

#include <LibDraw/Color.h>
#include <LibDraw/Size.h>
#include <LibHTML/CSS/LengthBox.h>

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

    LengthBox& margin() { return m_margin; }
    LengthBox& padding() { return m_padding; }
    LengthBox& border() { return m_border; }

    const LengthBox& margin() const { return m_margin; }
    const LengthBox& padding() const { return m_padding; }
    const LengthBox& border() const { return m_border; }

    FontStyle font_style() const { return m_font_style; }

    const Size& size() const { return m_size; }
    Size& size() { return m_size; }

    struct PixelBox {
        int top;
        int right;
        int bottom;
        int left;
    };

    PixelBox full_margin() const;

private:
    Color m_text_color;
    Color m_background_color;

    LengthBox m_margin;
    LengthBox m_padding;
    LengthBox m_border;

    Size m_size;

    FontStyle m_font_style { FontStyle::Normal };
};
