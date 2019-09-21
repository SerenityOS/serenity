#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <LibHTML/CSS/StyleProperties.h>

class Document;
class Element;
class ParentNode;
class StyleRule;
class StyleSheet;

class StyleResolver {
public:
    explicit StyleResolver(Document&);
    ~StyleResolver();

    Document& document() { return m_document; }
    const Document& document() const { return m_document; }

    StyleProperties resolve_style(const Element&);

    NonnullRefPtrVector<StyleRule> collect_matching_rules(const Element&) const;

private:
    Document& m_document;
};
