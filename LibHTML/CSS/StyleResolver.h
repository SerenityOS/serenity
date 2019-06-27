#pragma once

#include <AK/OwnPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <LibHTML/Layout/LayoutStyle.h>

class Document;
class Element;
class StyleSheet;

class StyleResolver {
public:
    explicit StyleResolver(Document&);
    ~StyleResolver();

    Document& document() { return m_document; }
    const Document& document() const { return m_document; }

    void add_sheet(const StyleSheet& sheet) { m_sheets.append(sheet); }

    OwnPtr<LayoutStyle> resolve_element_style(const Element&);
    OwnPtr<LayoutStyle> resolve_document_style(const Document&);

private:
    Document& m_document;

    NonnullRefPtrVector<StyleSheet> m_sheets;
};
