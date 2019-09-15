#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibDraw/TextAlignment.h>
#include <LibGUI/GFrame.h>

class TextWidget : public GFrame {
    C_OBJECT(TextWidget)
public:
    explicit TextWidget(GWidget* parent = nullptr);
    TextWidget(const StringView& text, GWidget* parent = nullptr);
    virtual ~TextWidget() override;

    String text() const { return m_text; }
    void set_text(const StringView&);

    TextAlignment text_alignment() const { return m_text_alignment; }
    void set_text_alignment(TextAlignment text_alignment) { m_text_alignment = text_alignment; }

    bool should_wrap() const { return m_should_wrap; }
    void set_should_wrap(bool should_wrap) { m_should_wrap = should_wrap; }

    int line_height() const { return m_line_height; }
    void set_line_height(int line_height) { m_line_height = line_height; }

    void wrap_and_set_height();

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void resize_event(GResizeEvent&) override;

    String m_text;
    Vector<String> m_lines;
    TextAlignment m_text_alignment { TextAlignment::Center };
    bool m_should_wrap { false };
    int m_line_height { 0 };
};
