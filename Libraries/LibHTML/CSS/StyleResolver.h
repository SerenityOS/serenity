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

    NonnullRefPtr<StyleProperties> resolve_style(const Element&, const StyleProperties* parent_properties) const;

    NonnullRefPtrVector<StyleRule> collect_matching_rules(const Element&) const;

private:
    template<typename Callback>
    void for_each_stylesheet(Callback) const;

    Document& m_document;
};
