#pragma once

#include <LibGUI/SyntaxHighlighter.h>

namespace GUI {

class JSSyntaxHighlighter final : public SyntaxHighlighter {
public:
    JSSyntaxHighlighter() {}
    virtual ~JSSyntaxHighlighter() override;

    virtual bool is_identifier(void*) const override;
    virtual bool is_navigatable(void*) const override;

    virtual SyntaxLanguage language() const override { return SyntaxLanguage::Javascript; }
    virtual void rehighlight() override;

protected:
    virtual Vector<MatchingTokenPair> matching_token_pairs() const override;
    virtual bool token_types_equal(void*, void*) const override;
};

}
