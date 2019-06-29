#pragma once

#include <AK/AKString.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/CSS/StyleSheet.h>
#include <LibHTML/DOM/ParentNode.h>

class LayoutNode;
class StyleResolver;
class StyleSheet;

class Document : public ParentNode {
public:
    Document();
    virtual ~Document() override;

    StyleResolver& style_resolver();

    void add_sheet(const StyleSheet& sheet) { m_sheets.append(sheet); }
    const NonnullRefPtrVector<StyleSheet>& stylesheets() const { return m_sheets; }

private:
    OwnPtr<StyleResolver> m_style_resolver;
    NonnullRefPtrVector<StyleSheet> m_sheets;
};
