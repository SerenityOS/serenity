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

class Palette;
class CTimer;
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

    StyleResolver& style_resolver() { return *m_style_resolver; }
    const StyleResolver& style_resolver() const { return *m_style_resolver; }

    void add_sheet(const StyleSheet& sheet) { m_sheets.append(sheet); }
    const NonnullRefPtrVector<StyleSheet>& stylesheets() const { return m_sheets; }

    virtual String tag_name() const override { return "#document"; }

    void set_hovered_node(Node*);
    Node* hovered_node() { return m_hovered_node; }
    const Node* hovered_node() const { return m_hovered_node; }

    void set_inspected_node(Node*);
    Node* inspected_node() { return m_inspected_node; }
    const Node* inspected_node() const { return m_inspected_node; }

    const HTMLHtmlElement* document_element() const;
    const HTMLHeadElement* head() const;
    const HTMLBodyElement* body() const;

    String title() const;

    void attach_to_frame(Badge<Frame>, Frame&);
    void detach_from_frame(Badge<Frame>, Frame&);

    Frame* frame() { return m_frame.ptr(); }
    const Frame* frame() const { return m_frame.ptr(); }

    Color background_color(const Palette&) const;
    RefPtr<GraphicsBitmap> background_image() const;

    Color link_color() const;
    void set_link_color(Color);

    Color active_link_color() const;
    void set_active_link_color(Color);

    Color visited_link_color() const;
    void set_visited_link_color(Color);

    void layout();
    void force_layout();

    void update_style();
    void update_layout();
    Function<void()> on_layout_updated;

    virtual bool is_child_allowed(const Node&) const override;

    const LayoutDocument* layout_node() const;
    LayoutDocument* layout_node();

    void schedule_style_update();

    const Element* get_element_by_id(const String&) const;
    Vector<const Element*> get_elements_by_name(const String&) const;

    const String& source() const { return m_source; }
    void set_source(const String& source) { m_source = source; }

private:
    virtual RefPtr<LayoutNode> create_layout_node(const StyleProperties* parent_style) const override;

    OwnPtr<StyleResolver> m_style_resolver;
    NonnullRefPtrVector<StyleSheet> m_sheets;
    RefPtr<Node> m_hovered_node;
    RefPtr<Node> m_inspected_node;
    WeakPtr<Frame> m_frame;
    URL m_url;

    RefPtr<LayoutDocument> m_layout_root;

    Optional<Color> m_link_color;
    Optional<Color> m_active_link_color;
    Optional<Color> m_visited_link_color;

    RefPtr<CTimer> m_style_update_timer;

    String m_source;
};

template<>
inline bool is<Document>(const Node& node)
{
    return node.is_document();
}
