#pragma once

#include <SharedGraphics/Color.h>

struct Box {
    int top { 0 };
    int right { 0 };
    int bottom { 0 };
    int left { 0 };
};

enum FontStyle {
    Normal,
    Bold,
};

class LayoutStyle {
public:
    LayoutStyle();
    ~LayoutStyle();

    Color text_color() const { return m_text_color; }
    Color background_color() const { return m_background_color; }

    Box& offset() { return m_offset; }
    Box& margin() { return m_margin; }
    Box& padding() { return m_padding; }

    const Box& offset() const { return m_offset; }
    const Box& margin() const { return m_margin; }
    const Box& padding() const { return m_padding; }

    FontStyle font_style() const { return m_font_style; }

private:
    Color m_text_color;
    Color m_background_color;

    Box m_offset;
    Box m_margin;
    Box m_padding;

    FontStyle m_font_style { FontStyle::Normal };
};
