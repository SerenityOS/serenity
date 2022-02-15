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
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/HTML/EventLoop/Task.h>
#include <LibWeb/HTML/TagNames.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/QualifiedName.h>

namespace Web::DOM {

class Element
    : public ParentNode
    , public ChildNode<Element>
    , public NonDocumentTypeChildNode<Element> {

public:
    using WrapperType = Bindings::ElementWrapper;

    Element(Document&, QualifiedName);
    virtual ~Element() override;

    const String& qualified_name() const { return m_qualified_name.as_string(); }
    const String& html_uppercased_qualified_name() const { return m_html_uppercased_qualified_name; }
    virtual FlyString node_name() const final { return html_uppercased_qualified_name(); }
    const FlyString& local_name() const { return m_qualified_name.local_name(); }

    // NOTE: This is for the JS bindings
    const String& tag_name() const { return html_uppercased_qualified_name(); }

    const FlyString& prefix() const { return m_qualified_name.prefix(); }
    const FlyString& namespace_() const { return m_qualified_name.namespace_(); }

    // NOTE: This is for the JS bindings
    const FlyString& namespace_uri() const { return namespace_(); }

    bool has_attribute(const FlyString& name) const;
    bool has_attributes() const { return !m_attributes->is_empty(); }
    String attribute(const FlyString& name) const { return get_attribute(name); }
    String get_attribute(const FlyString& name) const;
    ExceptionOr<void> set_attribute(const FlyString& name, const String& value);
    ExceptionOr<void> set_attribute_ns(FlyString const& namespace_, FlyString const& qualified_name, String const& value);
    void remove_attribute(const FlyString& name);
    size_t attribute_list_size() const { return m_attributes->length(); }
    NonnullRefPtr<NamedNodeMap> const& attributes() const { return m_attributes; }
    Vector<String> get_attribute_names() const;

    RefPtr<DOMTokenList> const& class_list();

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

    bool has_class(const FlyString&, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    const Vector<FlyString>& class_names() const { return m_classes; }

    virtual void apply_presentational_hints(CSS::StyleProperties&) const { }
    virtual void parse_attribute(const FlyString& name, const String& value);
    virtual void did_remove_attribute(FlyString const&) { }

    void recompute_style();

    Layout::NodeWithStyle* layout_node() { return static_cast<Layout::NodeWithStyle*>(Node::layout_node()); }
    const Layout::NodeWithStyle* layout_node() const { return static_cast<const Layout::NodeWithStyle*>(Node::layout_node()); }

    String name() const { return attribute(HTML::AttributeNames::name); }

    CSS::StyleProperties const* specified_css_values() const { return m_specified_css_values.ptr(); }
    void set_specified_css_values(RefPtr<CSS::StyleProperties> style) { m_specified_css_values = move(style); }
    NonnullRefPtr<CSS::StyleProperties> computed_style();

    const CSS::CSSStyleDeclaration* inline_style() const { return m_inline_style; }

    NonnullRefPtr<CSS::CSSStyleDeclaration> style_for_bindings();

    String inner_html() const;
    ExceptionOr<void> set_inner_html(String const&);

    bool is_focused() const;
    bool is_active() const;

    NonnullRefPtr<HTMLCollection> get_elements_by_class_name(FlyString const&);

    ShadowRoot* shadow_root() { return m_shadow_root; }
    const ShadowRoot* shadow_root() const { return m_shadow_root; }
    void set_shadow_root(RefPtr<ShadowRoot>);

    void add_custom_property(String custom_property_name, CSS::StyleProperty style_property)
    {
        m_custom_properties.set(move(custom_property_name), move(style_property));
    }

    HashMap<String, CSS::StyleProperty> const& custom_properties() const { return m_custom_properties; }
    HashMap<String, CSS::StyleProperty>& custom_properties() { return m_custom_properties; }

    void queue_an_element_task(HTML::Task::Source, Function<void()>);

    bool is_void_element() const;
    bool serializes_as_void() const;

    NonnullRefPtr<Geometry::DOMRect> get_bounding_client_rect() const;
    NonnullRefPtr<Geometry::DOMRectList> get_client_rects() const;

    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>);

    virtual void did_receive_focus() { }
    virtual void did_lose_focus() { }

protected:
    virtual void children_changed() override;

private:
    void make_html_uppercased_qualified_name();

    QualifiedName m_qualified_name;
    String m_html_uppercased_qualified_name;
    NonnullRefPtr<NamedNodeMap> m_attributes;

    RefPtr<CSS::CSSStyleDeclaration> m_inline_style;

    RefPtr<CSS::StyleProperties> m_specified_css_values;
    HashMap<String, CSS::StyleProperty> m_custom_properties;

    RefPtr<DOMTokenList> m_class_list;
    Vector<FlyString> m_classes;

    RefPtr<ShadowRoot> m_shadow_root;
};

template<>
inline bool Node::fast_is<Element>() const { return is_element(); }

}
