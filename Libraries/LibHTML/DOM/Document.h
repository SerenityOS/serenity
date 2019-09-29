#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/CSS/StyleSheet.h>
#include <LibHTML/DOM/ParentNode.h>

class HTMLHtmlElement;
class HTMLHeadElement;
class LayoutNode;
class StyleResolver;
class StyleSheet;

class Document : public ParentNode {
public:
    Document();
    virtual ~Document() override;

    void normalize();

    StyleResolver& style_resolver();

    void add_sheet(const StyleSheet& sheet) { m_sheets.append(sheet); }
    const NonnullRefPtrVector<StyleSheet>& stylesheets() const { return m_sheets; }

    virtual String tag_name() const override { return "#document"; }

    void set_hovered_node(Node* node) { m_hovered_node = node; }
    Node* hovered_node() { return m_hovered_node; }
    const Node* hovered_node() const { return m_hovered_node; }

    const HTMLHtmlElement* document_element() const;
    const HTMLHeadElement* head() const;

    String title() const;

private:
    OwnPtr<StyleResolver> m_style_resolver;
    NonnullRefPtrVector<StyleSheet> m_sheets;
    RefPtr<Node> m_hovered_node;
};
