#pragma once

#include <LibMarkdown/MDBlock.h>
#include <LibMarkdown/MDText.h>

class MDCodeBlock final : public MDBlock {
public:
    virtual ~MDCodeBlock() override {}

    virtual String render_to_html() const override;
    virtual String render_for_terminal() const override;
    virtual bool parse(Vector<StringView>::ConstIterator& lines) override;

private:
    String style_language() const;
    MDText::Style style() const;

    String m_code;
    MDText m_style_spec;
};
