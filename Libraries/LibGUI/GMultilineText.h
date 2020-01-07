#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibDraw/TextAlignment.h>
#include <LibGUI/GFrame.h>

class GMultilineText : public GFrame {
    C_OBJECT(GMultilineText)
public:
    virtual ~GMultilineText() override;

    String text() const { return m_text; }
    void set_text(const StringView&);

    TextAlignment text_alignment() const { return m_text_alignment; }
    void set_text_alignment(TextAlignment text_alignment) { m_text_alignment = text_alignment; }

    int line_spacing() const { return m_line_spacing; }
    void set_line_spacing(int line_spacing) { m_line_spacing = line_spacing; }

protected:
    explicit GMultilineText(GWidget* parent = nullptr);
    GMultilineText(const StringView& text, GWidget* parent = nullptr);

    virtual void paint_event(GPaintEvent&) override;
    virtual void resize_event(GResizeEvent&) override;

private:
    void wrap_and_set_height(int max_width);

    String m_text;
    Vector<String> m_lines;
    TextAlignment m_text_alignment { TextAlignment::Center };
    int m_line_spacing { 4 };
};
