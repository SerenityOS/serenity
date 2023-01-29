/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/DeprecatedString.h>
#include <LibWeb/ARIA/ARIAMixin.h>
#include <LibWeb/Bindings/ElementPrototype.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/DOM/Attr.h>
#include <LibWeb/DOM/ChildNode.h>
#include <LibWeb/DOM/NamedNodeMap.h>
#include <LibWeb/DOM/NonDocumentTypeChildNode.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/DOM/QualifiedName.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/HTML/EventLoop/Task.h>
#include <LibWeb/HTML/TagNames.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/TreeBuilder.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::DOM {

// https://w3c.github.io/csswg-drafts/cssom-view-1/#dictdef-scrolloptions
struct ScrollOptions {
    Bindings::ScrollBehavior behavior { Bindings::ScrollBehavior::Auto };
};

// https://w3c.github.io/csswg-drafts/cssom-view-1/#dictdef-scrollintoviewoptions
struct ScrollIntoViewOptions : public ScrollOptions {
    Bindings::ScrollLogicalPosition block { Bindings::ScrollLogicalPosition::Start };
    Bindings::ScrollLogicalPosition inline_ { Bindings::ScrollLogicalPosition::Nearest };
};

class Element
    : public ParentNode
    , public ChildNode<Element>
    , public NonDocumentTypeChildNode<Element>
    , public ARIA::ARIAMixin {
    WEB_PLATFORM_OBJECT(Element, ParentNode);

public:
    virtual ~Element() override;

    DeprecatedString const& qualified_name() const { return m_qualified_name.as_string(); }
    DeprecatedString const& html_uppercased_qualified_name() const { return m_html_uppercased_qualified_name; }
    virtual DeprecatedFlyString node_name() const final { return html_uppercased_qualified_name(); }
    DeprecatedFlyString const& local_name() const { return m_qualified_name.local_name(); }

    // NOTE: This is for the JS bindings
    DeprecatedString const& tag_name() const { return html_uppercased_qualified_name(); }

    DeprecatedFlyString const& prefix() const { return m_qualified_name.prefix(); }
    DeprecatedFlyString const& namespace_() const { return m_qualified_name.namespace_(); }

    // NOTE: This is for the JS bindings
    DeprecatedFlyString const& namespace_uri() const { return namespace_(); }

    bool has_attribute(DeprecatedFlyString const& name) const;
    bool has_attributes() const { return !m_attributes->is_empty(); }
    DeprecatedString attribute(DeprecatedFlyString const& name) const { return get_attribute(name); }
    DeprecatedString get_attribute(DeprecatedFlyString const& name) const;
    WebIDL::ExceptionOr<void> set_attribute(DeprecatedFlyString const& name, DeprecatedString const& value);
    WebIDL::ExceptionOr<void> set_attribute_ns(DeprecatedFlyString const& namespace_, DeprecatedFlyString const& qualified_name, DeprecatedString const& value);
    void remove_attribute(DeprecatedFlyString const& name);
    WebIDL::ExceptionOr<bool> toggle_attribute(DeprecatedFlyString const& name, Optional<bool> force);
    size_t attribute_list_size() const { return m_attributes->length(); }
    NamedNodeMap const* attributes() const { return m_attributes.ptr(); }
    Vector<DeprecatedString> get_attribute_names() const;

    JS::GCPtr<Attr> get_attribute_node(DeprecatedFlyString const& name) const;

    DOMTokenList* class_list();

    WebIDL::ExceptionOr<bool> matches(StringView selectors) const;
    WebIDL::ExceptionOr<DOM::Element const*> closest(StringView selectors) const;

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

    bool has_class(DeprecatedFlyString const&, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    Vector<DeprecatedFlyString> const& class_names() const { return m_classes; }

    virtual void apply_presentational_hints(CSS::StyleProperties&) const { }
    virtual void parse_attribute(DeprecatedFlyString const& name, DeprecatedString const& value);
    virtual void did_remove_attribute(DeprecatedFlyString const&);

    enum class NeedsRelayout {
        No = 0,
        Yes = 1,
    };
    NeedsRelayout recompute_style();

    Layout::NodeWithStyle* layout_node() { return static_cast<Layout::NodeWithStyle*>(Node::layout_node()); }
    Layout::NodeWithStyle const* layout_node() const { return static_cast<Layout::NodeWithStyle const*>(Node::layout_node()); }

    DeprecatedString name() const { return attribute(HTML::AttributeNames::name); }

    CSS::StyleProperties const* computed_css_values() const { return m_computed_css_values.ptr(); }
    void set_computed_css_values(RefPtr<CSS::StyleProperties> style) { m_computed_css_values = move(style); }
    NonnullRefPtr<CSS::StyleProperties> resolved_css_values();

    CSS::CSSStyleDeclaration const* inline_style() const;

    CSS::CSSStyleDeclaration* style_for_bindings();

    WebIDL::ExceptionOr<DeprecatedString> inner_html() const;
    WebIDL::ExceptionOr<void> set_inner_html(DeprecatedString const&);

    WebIDL::ExceptionOr<void> insert_adjacent_html(DeprecatedString position, DeprecatedString text);

    bool is_focused() const;
    bool is_active() const;

    JS::NonnullGCPtr<HTMLCollection> get_elements_by_class_name(DeprecatedFlyString const&);

    bool is_shadow_host() const;
    ShadowRoot* shadow_root_internal() { return m_shadow_root.ptr(); }
    ShadowRoot const* shadow_root_internal() const { return m_shadow_root.ptr(); }
    void set_shadow_root(JS::GCPtr<ShadowRoot>);

    void set_custom_properties(HashMap<DeprecatedFlyString, CSS::StyleProperty> custom_properties) { m_custom_properties = move(custom_properties); }
    HashMap<DeprecatedFlyString, CSS::StyleProperty> const& custom_properties() const { return m_custom_properties; }

    void queue_an_element_task(HTML::Task::Source, JS::SafeFunction<void()>);

    bool is_void_element() const;
    bool serializes_as_void() const;

    JS::NonnullGCPtr<Geometry::DOMRect> get_bounding_client_rect() const;
    JS::NonnullGCPtr<Geometry::DOMRectList> get_client_rects() const;

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>);

    virtual void did_receive_focus() { }
    virtual void did_lose_focus() { }

    static JS::GCPtr<Layout::Node> create_layout_node_for_display_type(DOM::Document&, CSS::Display const&, NonnullRefPtr<CSS::StyleProperties>, Element*);

    void set_pseudo_element_node(Badge<Layout::TreeBuilder>, CSS::Selector::PseudoElement, JS::GCPtr<Layout::Node>);
    JS::GCPtr<Layout::Node> get_pseudo_element_node(CSS::Selector::PseudoElement) const;
    void clear_pseudo_element_nodes(Badge<Layout::TreeBuilder>);
    void serialize_pseudo_elements_as_json(JsonArraySerializer<StringBuilder>& children_array) const;

    i32 tab_index() const;
    void set_tab_index(i32 tab_index);

    bool is_potentially_scrollable() const;

    double scroll_top() const;
    double scroll_left() const;
    void set_scroll_top(double y);
    void set_scroll_left(double x);

    int scroll_width() const;
    int scroll_height() const;

    bool is_actually_disabled() const;

    WebIDL::ExceptionOr<JS::GCPtr<Element>> insert_adjacent_element(DeprecatedString const& where, JS::NonnullGCPtr<Element> element);
    WebIDL::ExceptionOr<void> insert_adjacent_text(DeprecatedString const& where, DeprecatedString const& data);

    // https://w3c.github.io/csswg-drafts/cssom-view-1/#dom-element-scrollintoview
    ErrorOr<void> scroll_into_view(Optional<Variant<bool, ScrollIntoViewOptions>> = {});

    // https://www.w3.org/TR/wai-aria-1.2/#ARIAMixin
#define ARIA_IMPL(name, attribute)                                               \
    DeprecatedString name() const override                                       \
    {                                                                            \
        return get_attribute(attribute);                                         \
    }                                                                            \
                                                                                 \
    WebIDL::ExceptionOr<void> set_##name(DeprecatedString const& value) override \
    {                                                                            \
        TRY(set_attribute(attribute, value));                                    \
        return {};                                                               \
    }

    // https://www.w3.org/TR/wai-aria-1.2/#accessibilityroleandproperties-correspondence
    ARIA_IMPL(role, "role");
    ARIA_IMPL(aria_active_descendant, "aria-activedescendant");
    ARIA_IMPL(aria_atomic, "aria-atomic");
    ARIA_IMPL(aria_auto_complete, "aria-autocomplete");
    ARIA_IMPL(aria_busy, "aria-busy");
    ARIA_IMPL(aria_checked, "aria-checked");
    ARIA_IMPL(aria_col_count, "aria-colcount");
    ARIA_IMPL(aria_col_index, "aria-colindex");
    ARIA_IMPL(aria_col_span, "aria-colspan");
    ARIA_IMPL(aria_controls, "aria-controls");
    ARIA_IMPL(aria_current, "aria-current");
    ARIA_IMPL(aria_described_by, "aria-describedby");
    ARIA_IMPL(aria_details, "aria-details");
    ARIA_IMPL(aria_drop_effect, "aria-dropeffect");
    ARIA_IMPL(aria_error_message, "aria-errormessage");
    ARIA_IMPL(aria_disabled, "aria-disabled");
    ARIA_IMPL(aria_expanded, "aria-expanded");
    ARIA_IMPL(aria_flow_to, "aria-flowto");
    ARIA_IMPL(aria_grabbed, "aria-grabbed");
    ARIA_IMPL(aria_has_popup, "aria-haspopup");
    ARIA_IMPL(aria_hidden, "aria-hidden");
    ARIA_IMPL(aria_invalid, "aria-invalid");
    ARIA_IMPL(aria_key_shortcuts, "aria-keyshortcuts");
    ARIA_IMPL(aria_label, "aria-label");
    ARIA_IMPL(aria_labelled_by, "aria-labelledby");
    ARIA_IMPL(aria_level, "aria-level");
    ARIA_IMPL(aria_live, "aria-live");
    ARIA_IMPL(aria_modal, "aria-modal");
    ARIA_IMPL(aria_multi_line, "aria-multiline");
    ARIA_IMPL(aria_multi_selectable, "aria-multiselectable");
    ARIA_IMPL(aria_orientation, "aria-orientation");
    ARIA_IMPL(aria_owns, "aria-owns");
    ARIA_IMPL(aria_placeholder, "aria-placeholder");
    ARIA_IMPL(aria_pos_in_set, "aria-posinset");
    ARIA_IMPL(aria_pressed, "aria-pressed");
    ARIA_IMPL(aria_read_only, "aria-readonly");
    ARIA_IMPL(aria_relevant, "aria-relevant");
    ARIA_IMPL(aria_required, "aria-required");
    ARIA_IMPL(aria_role_description, "aria-roledescription");
    ARIA_IMPL(aria_row_count, "aria-rowcount");
    ARIA_IMPL(aria_row_index, "aria-rowindex");
    ARIA_IMPL(aria_row_span, "aria-rowspan");
    ARIA_IMPL(aria_selected, "aria-selected");
    ARIA_IMPL(aria_set_size, "aria-setsize");
    ARIA_IMPL(aria_sort, "aria-sort");
    ARIA_IMPL(aria_value_max, "aria-valuemax");
    ARIA_IMPL(aria_value_min, "aria-valuemin");
    ARIA_IMPL(aria_value_now, "aria-valuenow");
    ARIA_IMPL(aria_value_text, "aria-valuetext");

#undef ARIA_IMPL

    virtual bool exclude_from_accessibility_tree() const override;

    virtual bool include_in_accessibility_tree() const override;

protected:
    Element(Document&, DOM::QualifiedName);
    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;

    virtual void children_changed() override;
    virtual i32 default_tab_index_value() const;

    virtual void visit_edges(Cell::Visitor&) override;

private:
    void make_html_uppercased_qualified_name();

    void invalidate_style_after_attribute_change(DeprecatedFlyString const& attribute_name);

    WebIDL::ExceptionOr<JS::GCPtr<Node>> insert_adjacent(DeprecatedString const& where, JS::NonnullGCPtr<Node> node);

    QualifiedName m_qualified_name;
    DeprecatedString m_html_uppercased_qualified_name;

    JS::GCPtr<NamedNodeMap> m_attributes;
    JS::GCPtr<CSS::ElementInlineCSSStyleDeclaration> m_inline_style;
    JS::GCPtr<DOMTokenList> m_class_list;
    JS::GCPtr<ShadowRoot> m_shadow_root;

    RefPtr<CSS::StyleProperties> m_computed_css_values;
    HashMap<DeprecatedFlyString, CSS::StyleProperty> m_custom_properties;

    Vector<DeprecatedFlyString> m_classes;

    Array<JS::GCPtr<Layout::Node>, to_underlying(CSS::Selector::PseudoElement::PseudoElementCount)> m_pseudo_element_nodes;
};

template<>
inline bool Node::fast_is<Element>() const { return is_element(); }

WebIDL::ExceptionOr<QualifiedName> validate_and_extract(JS::Realm&, DeprecatedFlyString namespace_, DeprecatedFlyString qualified_name);

}
