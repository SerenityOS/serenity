#pragma once

#include <LibGUI/SyntaxHighlighter.h>

namespace GUI {

class CppSyntaxHighlighter final : public SyntaxHighlighter {
public:
    CppSyntaxHighlighter() {}
    virtual ~CppSyntaxHighlighter() override;

    virtual SyntaxLanguage language() const override { return SyntaxLanguage::Cpp; }
    virtual void rehighlight() override;
    virtual void highlight_matching_token_pair() override;
};

}
