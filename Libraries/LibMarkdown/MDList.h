#pragma once

#include <AK/Vector.h>
#include <LibMarkdown/MDBlock.h>
#include <LibMarkdown/MDText.h>

class MDList final : public MDBlock {
public:
    virtual ~MDList() override {}

    virtual String render_to_html() const override;
    virtual String render_for_terminal() const override;
    virtual bool parse(Vector<StringView>::ConstIterator& lines) override;

private:
    // TODO: List items should be considered blocks of their own kind.
    Vector<MDText> m_items;
    bool m_is_ordered { false };
};
