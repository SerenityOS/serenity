#pragma once

#include <AK/Function.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <AK/WeakPtr.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/CSS/StyleSheet.h>
#include <LibHTML/DOM/ParentNode.h>

class Frame;
class HTMLBodyElement;
class HTMLHtmlElement;
class HTMLHeadElement;
class LayoutDocument;
class LayoutNode;
class StyleResolver;
class StyleSheet;

class Document : public ParentNode {
public:
    Document();
    virtual ~Document() override;

    void set_url(const URL& url) { m_url = url; }
    const URL& url() const { return m_url; }

    URL complete_url(const String&) const;

    void fixup();

    StyleResolver& style_resolver();

    void add_sheet(const StyleSheet& sheet) { m_sheets.append(sheet); }
    const NonnullRefPtrVector<StyleSheet>& stylesheets() const { return m_sheets; }

    virtual String tag_name() const override { return "#document"; }

    void set_hovered_node(Node*);
    Node* hovered_node() { return m_hovered_node; }
    const Node* hovered_node() const { return m_hovered_node; }

    const HTMLHtmlElement* document_element() const;
    const HTMLHeadElement* head() const;
    const HTMLBodyElement* body() const;

    String title() const;

    void attach_to_frame(Badge<Frame>, Frame&);
    void detach_from_frame(Badge<Frame>, Frame&);

    Frame* frame() { return m_frame.ptr(); }
    const Frame* frame() const { return m_frame.ptr(); }

    Color background_color() const;

    Color link_color() const { return m_link_color; }
    void set_link_color(Color);

    Color active_link_color() const { return m_active_link_color; }
    void set_active_link_color(Color);

    Color visited_link_color() const { return m_visited_link_color; }
    void set_visited_link_color(Color);

    void layout();

    void update_style();
    void update_layout();
    Function<void()> on_layout_updated;

    virtual bool is_child_allowed(const Node&) const override;

    const LayoutDocument* layout_node() const;

private:
    virtual RefPtr<LayoutNode> create_layout_node(const StyleResolver&, const StyleProperties* parent_style) const override;

    OwnPtr<StyleResolver> m_style_resolver;
    NonnullRefPtrVector<StyleSheet> m_sheets;
    RefPtr<Node> m_hovered_node;
    WeakPtr<Frame> m_frame;
    URL m_url;

    RefPtr<LayoutDocument> m_layout_root;

    Color m_link_color { Color::Blue };
    Color m_active_link_color { Color::Red };
    Color m_visited_link_color { Color::Magenta };
};

template<>
inline bool is<Document>(const Node& node)
{
    return node.is_document();
}
