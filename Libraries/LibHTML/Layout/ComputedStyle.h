#pragma once

#include <LibDraw/Size.h>
#include <LibHTML/CSS/LengthBox.h>

class ComputedStyle {
public:
    ComputedStyle();
    ~ComputedStyle();

    LengthBox& margin() { return m_margin; }
    LengthBox& padding() { return m_padding; }
    LengthBox& border() { return m_border; }

    const LengthBox& margin() const { return m_margin; }
    const LengthBox& padding() const { return m_padding; }
    const LengthBox& border() const { return m_border; }

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
    LengthBox m_margin;
    LengthBox m_padding;
    LengthBox m_border;

    Size m_size;
};
