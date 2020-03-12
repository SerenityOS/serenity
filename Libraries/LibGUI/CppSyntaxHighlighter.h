#pragma once

#include <LibGUI/SyntaxHighlighter.h>

namespace GUI {

class CppSyntaxHighlighter final : public SyntaxHighlighter {
public:
    CppSyntaxHighlighter() {}
    virtual ~CppSyntaxHighlighter() override;

    virtual bool is_identifier(void*) const override;
    virtual bool is_navigatable(void*) const override;

    virtual SyntaxLanguage language() const override { return SyntaxLanguage::Cpp; }
    virtual void rehighlight() override;

protected:
    virtual Vector<MatchingTokenPair> matching_token_pairs() const override;
    virtual bool token_types_equal(void*, void*) const override;
};

}
