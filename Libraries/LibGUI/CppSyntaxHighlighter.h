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
    virtual void highlight_matching_token_pair() override;
};

}
