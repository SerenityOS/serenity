/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/String.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/DOM/Attribute.h>
#include <LibWeb/DOM/ChildNode.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/NamedNodeMap.h>
#include <LibWeb/DOM/NonDocumentTypeChildNode.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/DOM/QualifiedName.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/HTML/EventLoop/Task.h>
#include <LibWeb/HTML/TagNames.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/TreeBuilder.h>

namespace Web::DOM {

class Element
    : public ParentNode
    , public ChildNode<Element>
    , public NonDocumentTypeChildNode<Element> {

public:
    using WrapperType = Bindings::ElementWrapper;

    Element(Document&, DOM::QualifiedName);
    virtual ~Element() override;

    String const& qualified_name() const { return m_qualified_name.as_string(); }
    String const& html_uppercased_qualified_name() const { return m_html_uppercased_qualified_name; }
    virtual FlyString node_name() const final { return html_uppercased_qualified_name(); }
    FlyString const& local_name() const { return m_qualified_name.local_name(); }

    // NOTE: This is for the JS bindings
    String const& tag_name() const { return html_uppercased_qualified_name(); }

    FlyString const& prefix() const { return m_qualified_name.prefix(); }
    FlyString const& namespace_() const { return m_qualified_name.namespace_(); }

    // NOTE: This is for the JS bindings
    FlyString const& namespace_uri() const { return namespace_(); }

    bool has_attribute(FlyString const& name) const;
    bool has_attributes() const { return !m_attributes->is_empty(); }
    String attribute(FlyString const& name) const { return get_attribute(name); }
    String get_attribute(FlyString const& name) const;
    ExceptionOr<void> set_attribute(FlyString const& name, String const& value);
    ExceptionOr<void> set_attribute_ns(FlyString const& namespace_, FlyString const& qualified_name, String const& value);
    void remove_attribute(FlyString const& name);
    DOM::ExceptionOr<bool> toggle_attribute(FlyString const& name, Optional<bool> force);
    size_t attribute_list_size() const { return m_attributes->length(); }
    NamedNodeMap const* attributes() const { return m_attributes.cell(); }
    Vector<String> get_attribute_names() const;

    DOMTokenList* class_list();

    DOM::ExceptionOr<bool> matches(StringView selectors) const;
    DOM::ExceptionOr<DOM::Element const*> closest(StringView selectors) const;

    int client_top() const;
    int client_left() const;
    int client_width() const;
    int client_height() const;

    template<typename Callback>
    void for_each_attribute(Callback callback) const
    {
        for (size_t i = 0; i < m_attributes->length(); ++i) {
            auto const* attribute = m_attributes->item(i);
            callback(attribute->name(), attribute->value());
        }
    }

    bool has_class(FlyString const&, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    Vector<FlyString> const& class_names() const { return m_classes; }

    virtual void apply_presentational_hints(CSS::StyleProperties&) const { }
    virtual void parse_attribute(FlyString const& name, String const& value);
    virtual void did_remove_attribute(FlyString const&);

    enum class NeedsRelayout {
        No = 0,
        Yes = 1,
    };
    NeedsRelayout recompute_style();

    Layout::NodeWithStyle* layout_node() { return static_cast<Layout::NodeWithStyle*>(Node::layout_node()); }
    Layout::NodeWithStyle const* layout_node() const { return static_cast<Layout::NodeWithStyle const*>(Node::layout_node()); }

    String name() const { return attribute(HTML::AttributeNames::name); }

    CSS::StyleProperties const* computed_css_values() const { return m_computed_css_values.ptr(); }
    void set_computed_css_values(RefPtr<CSS::StyleProperties> style) { m_computed_css_values = move(style); }
    NonnullRefPtr<CSS::StyleProperties> resolved_css_values();

    CSS::CSSStyleDeclaration const* inline_style() const;

    CSS::CSSStyleDeclaration* style_for_bindings();

    String inner_html() const;
    ExceptionOr<void> set_inner_html(String const&);

    bool is_focused() const;
    bool is_active() const;

    NonnullRefPtr<HTMLCollection> get_elements_by_class_name(FlyString const&);

    ShadowRoot* shadow_root() { return m_shadow_root; }
    ShadowRoot const* shadow_root() const { return m_shadow_root; }
    void set_shadow_root(RefPtr<ShadowRoot>);

    void set_custom_properties(HashMap<FlyString, CSS::StyleProperty> custom_properties) { m_custom_properties = move(custom_properties); }
    HashMap<FlyString, CSS::StyleProperty> const& custom_properties() const { return m_custom_properties; }

    void queue_an_element_task(HTML::Task::Source, Function<void()>);

    bool is_void_element() const;
    bool serializes_as_void() const;

    NonnullRefPtr<Geometry::DOMRect> get_bounding_client_rect() const;
    NonnullRefPtr<Geometry::DOMRectList> get_client_rects() const;

    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>);

    virtual void did_receive_focus() { }
    virtual void did_lose_focus() { }

    static RefPtr<Layout::Node> create_layout_node_for_display_type(DOM::Document&, CSS::Display const&, NonnullRefPtr<CSS::StyleProperties>, Element*);

    void set_pseudo_element_node(Badge<Layout::TreeBuilder>, CSS::Selector::PseudoElement, RefPtr<Layout::Node>);
    RefPtr<Layout::Node> get_pseudo_element_node(CSS::Selector::PseudoElement) const;
    void clear_pseudo_element_nodes(Badge<Layout::TreeBuilder>);
    void serialize_pseudo_elements_as_json(JsonArraySerializer<StringBuilder>& children_array) const;

protected:
    virtual void children_changed() override;

private:
    void make_html_uppercased_qualified_name();

    QualifiedName m_qualified_name;
    String m_html_uppercased_qualified_name;
    JS::Handle<NamedNodeMap> m_attributes;

    JS::Handle<CSS::ElementInlineCSSStyleDeclaration> m_inline_style;

    RefPtr<CSS::StyleProperties> m_computed_css_values;
    HashMap<FlyString, CSS::StyleProperty> m_custom_properties;

    JS::Handle<DOMTokenList> m_class_list;
    Vector<FlyString> m_classes;

    RefPtr<ShadowRoot> m_shadow_root;

    Array<RefPtr<Layout::Node>, CSS::Selector::PseudoElementCount> m_pseudo_element_nodes;
};

template<>
inline bool Node::fast_is<Element>() const { return is_element(); }

ExceptionOr<QualifiedName> validate_and_extract(FlyString namespace_, FlyString qualified_name);

}
