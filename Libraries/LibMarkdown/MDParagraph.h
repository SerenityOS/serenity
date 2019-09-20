#pragma once

#include <LibMarkdown/MDBlock.h>
#include <LibMarkdown/MDText.h>

class MDParagraph final : public MDBlock {
public:
    virtual ~MDParagraph() override {}

    virtual String render_to_html() const override;
    virtual String render_for_terminal() const override;
    virtual bool parse(Vector<StringView>::ConstIterator& lines) override;

private:
    MDText m_text;
};
