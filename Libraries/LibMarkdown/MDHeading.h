#pragma once

#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibMarkdown/MDBlock.h>
#include <LibMarkdown/MDText.h>

class MDHeading final : public MDBlock {
public:
    virtual ~MDHeading() override {}

    virtual String render_to_html() const override;
    virtual String render_for_terminal() const override;
    virtual bool parse(Vector<StringView>::ConstIterator& lines) override;

private:
    MDText m_text;
    int m_level { -1 };
};
