#pragma once

#include <AK/OwnPtr.h>
#include <AK/NonnullRefPtrVector.h>

class Document;
class Element;
class ParentNode;
class StyleRule;
class StyleSheet;
class StyledNode;

class StyleResolver {
public:
    explicit StyleResolver(Document&);
    ~StyleResolver();

    Document& document() { return m_document; }
    const Document& document() const { return m_document; }

    NonnullRefPtr<StyledNode> create_styled_node(const Element&);
    NonnullRefPtr<StyledNode> create_styled_node(const Document&);

    NonnullRefPtrVector<StyleRule> collect_matching_rules(const Element&) const;


private:
    Document& m_document;
};
