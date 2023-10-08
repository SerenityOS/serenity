/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/DeprecatedString.h>
#include <LibWeb/ARIA/ARIAMixin.h>
#include <LibWeb/Bindings/ElementPrototype.h>
#include <LibWeb/Bindings/ShadowRootPrototype.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/CSS/StyleProperty.h>
#include <LibWeb/DOM/ChildNode.h>
#include <LibWeb/DOM/NonDocumentTypeChildNode.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/DOM/QualifiedName.h>
#include <LibWeb/DOM/Slottable.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/HTML/EventLoop/Task.h>
#include <LibWeb/HTML/ScrollOptions.h>
#include <LibWeb/HTML/TagNames.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/IntersectionObserver/IntersectionObserver.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::DOM {

struct ShadowRootInit {
    Bindings::ShadowRootMode mode;
    bool delegates_focus = false;
    Bindings::SlotAssignmentMode slot_assignment { Bindings::SlotAssignmentMode::Named };
};

// https://w3c.github.io/csswg-drafts/cssom-view-1/#dictdef-scrollintoviewoptions
struct ScrollIntoViewOptions : public HTML::ScrollOptions {
    Bindings::ScrollLogicalPosition block { Bindings::ScrollLogicalPosition::Start };
    Bindings::ScrollLogicalPosition inline_ { Bindings::ScrollLogicalPosition::Nearest };
};

// https://html.spec.whatwg.org/multipage/custom-elements.html#upgrade-reaction
// An upgrade reaction, which will upgrade the custom element and contains a custom element definition; or
struct CustomElementUpgradeReaction {
    JS::Handle<HTML::CustomElementDefinition> custom_element_definition;
};

// https://html.spec.whatwg.org/multipage/custom-elements.html#callback-reaction
// A callback reaction, which will call a lifecycle callback, and contains a callback function as well as a list of arguments.
struct CustomElementCallbackReaction {
    JS::Handle<WebIDL::CallbackType> callback;
    JS::MarkedVector<JS::Value> arguments;
};

// https://dom.spec.whatwg.org/#concept-element-custom-element-state
// An element’s custom element state is one of "undefined", "failed", "uncustomized", "precustomized", or "custom".
enum class CustomElementState {
    Undefined,
    Failed,
    Uncustomized,
    Precustomized,
    Custom,
};

class Element
    : public ParentNode
    , public ChildNode<Element>
    , public NonDocumentTypeChildNode<Element>
    , public SlottableMixin
    , public ARIA::ARIAMixin {
    WEB_PLATFORM_OBJECT(Element, ParentNode);

public:
    virtual ~Element() override;

    FlyString const& qualified_name() const { return m_qualified_name.as_string(); }
    FlyString const& html_uppercased_qualified_name() const { return m_html_uppercased_qualified_name; }

    virtual FlyString node_name() const final { return html_uppercased_qualified_name(); }
    DeprecatedFlyString deprecated_local_name() const { return m_qualified_name.local_name().to_deprecated_fly_string(); }
    FlyString const& local_name() const { return m_qualified_name.local_name(); }

    // NOTE: This is for the JS bindings
    FlyString const& tag_name() const { return html_uppercased_qualified_name(); }
    DeprecatedString deprecated_tag_name() const { return html_uppercased_qualified_name().to_deprecated_fly_string(); }

    Optional<FlyString> const& prefix() const { return m_qualified_name.prefix(); }
    DeprecatedFlyString deprecated_prefix() const { return m_qualified_name.deprecated_prefix(); }

    void set_prefix(Optional<FlyString> value);

    DeprecatedFlyString namespace_() const { return m_qualified_name.deprecated_namespace_(); }

    // NOTE: This is for the JS bindings
    Optional<FlyString> const& namespace_uri() const { return m_qualified_name.namespace_(); }

    // FIXME: This should be taking a 'FlyString const&' / 'Optional<FlyString> const&'
    bool has_attribute(StringView name) const;
    bool has_attribute_ns(Optional<String> const& namespace_, StringView name) const;
    bool has_attributes() const;

    // FIXME: This should be taking a 'FlyString const&'
    DeprecatedString deprecated_attribute(StringView name) const { return deprecated_get_attribute(name); }
    Optional<String> attribute(StringView name) const { return get_attribute(name); }

    // FIXME: This should be taking a 'FlyString const&' / 'Optional<FlyString> const&'
    Optional<String> get_attribute(StringView name) const;
    DeprecatedString deprecated_get_attribute(StringView name) const;
    DeprecatedString get_attribute_value(StringView local_name, DeprecatedFlyString const& namespace_ = {}) const;

    WebIDL::ExceptionOr<void> set_attribute(DeprecatedFlyString const& name, DeprecatedString const& value);
    WebIDL::ExceptionOr<void> set_attribute(DeprecatedFlyString const& name, Optional<String> const& value);
    WebIDL::ExceptionOr<void> set_attribute(FlyString const& name, Optional<String> const& value)
    {
        return set_attribute(name.to_deprecated_fly_string(), value);
    }

    // FIXME: This should be taking an Optional<FlyString>
    WebIDL::ExceptionOr<void> set_attribute_ns(Optional<String> const& namespace_, FlyString const& qualified_name, FlyString const& value);
    void set_attribute_value(DeprecatedFlyString const& local_name, DeprecatedString const& value, DeprecatedFlyString const& prefix = {}, DeprecatedFlyString const& namespace_ = {});
    WebIDL::ExceptionOr<JS::GCPtr<Attr>> set_attribute_node(Attr&);
    WebIDL::ExceptionOr<JS::GCPtr<Attr>> set_attribute_node_ns(Attr&);

    // FIXME: This should take a 'FlyString cosnt&'
    void remove_attribute(StringView name);

    WebIDL::ExceptionOr<bool> toggle_attribute(FlyString const& name, Optional<bool> force);
    size_t attribute_list_size() const;
    NamedNodeMap const* attributes() const { return m_attributes.ptr(); }
    Vector<String> get_attribute_names() const;

    JS::GCPtr<Attr> get_attribute_node(FlyString const& name) const;

    DOMTokenList* class_list();

    WebIDL::ExceptionOr<JS::NonnullGCPtr<ShadowRoot>> attach_shadow(ShadowRootInit init);
    JS::GCPtr<ShadowRoot> shadow_root() const;

    WebIDL::ExceptionOr<bool> matches(StringView selectors) const;
    WebIDL::ExceptionOr<DOM::Element const*> closest(StringView selectors) const;

    int client_top() const;
    int client_left() const;
    int client_width() const;
    int client_height() const;

    void for_each_attribute(Function<void(FlyString const&, DeprecatedString const&)>) const;

    bool has_class(FlyString const&, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    Vector<FlyString> const& class_names() const { return m_classes; }

    virtual void apply_presentational_hints(CSS::StyleProperties&) const { }

    // https://dom.spec.whatwg.org/#concept-element-attributes-change-ext
    using AttributeChangeSteps = Function<void(FlyString const& /*local_name*/, DeprecatedString const& /*old_value*/, DeprecatedString const& /*value*/, DeprecatedFlyString const& /*namespace_*/)>;

    void add_attribute_change_steps(AttributeChangeSteps steps);
    void run_attribute_change_steps(FlyString const& local_name, DeprecatedString const& old_value, DeprecatedString const& value, DeprecatedFlyString const& namespace_);
    virtual void attribute_changed(FlyString const& name, DeprecatedString const& value);

    struct [[nodiscard]] RequiredInvalidationAfterStyleChange {
        bool repaint { false };
        bool rebuild_stacking_context_tree { false };
        bool relayout { false };
        bool rebuild_layout_tree { false };

        void operator|=(RequiredInvalidationAfterStyleChange const& other)
        {
            repaint |= other.repaint;
            rebuild_stacking_context_tree |= other.rebuild_stacking_context_tree;
            relayout |= other.relayout;
            rebuild_layout_tree |= other.rebuild_layout_tree;
        }

        [[nodiscard]] bool is_none() const { return !repaint && !rebuild_stacking_context_tree && !relayout && !rebuild_layout_tree; }
        static RequiredInvalidationAfterStyleChange full() { return { true, true, true, true }; }
    };

    RequiredInvalidationAfterStyleChange recompute_style();

    virtual Optional<CSS::Selector::PseudoElement> pseudo_element() const { return {}; }

    Layout::NodeWithStyle* layout_node();
    Layout::NodeWithStyle const* layout_node() const;

    DeprecatedString name() const { return deprecated_attribute(HTML::AttributeNames::name); }

    CSS::StyleProperties* computed_css_values() { return m_computed_css_values.ptr(); }
    CSS::StyleProperties const* computed_css_values() const { return m_computed_css_values.ptr(); }
    void set_computed_css_values(RefPtr<CSS::StyleProperties>);
    NonnullRefPtr<CSS::StyleProperties> resolved_css_values();

    CSS::CSSStyleDeclaration const* inline_style() const;

    CSS::CSSStyleDeclaration* style_for_bindings();

    WebIDL::ExceptionOr<String> inner_html() const;
    WebIDL::ExceptionOr<void> set_inner_html(StringView);

    WebIDL::ExceptionOr<void> insert_adjacent_html(String const& position, String const& text);

    bool is_focused() const;
    bool is_active() const;
    bool is_target() const;
    bool is_document_element() const;

    JS::NonnullGCPtr<HTMLCollection> get_elements_by_class_name(StringView);

    bool is_shadow_host() const;
    ShadowRoot* shadow_root_internal() { return m_shadow_root.ptr(); }
    ShadowRoot const* shadow_root_internal() const { return m_shadow_root.ptr(); }
    void set_shadow_root(JS::GCPtr<ShadowRoot>);

    void set_custom_properties(Optional<CSS::Selector::PseudoElement>, HashMap<DeprecatedFlyString, CSS::StyleProperty> custom_properties);
    [[nodiscard]] HashMap<DeprecatedFlyString, CSS::StyleProperty> const& custom_properties(Optional<CSS::Selector::PseudoElement>) const;

    int queue_an_element_task(HTML::Task::Source, JS::SafeFunction<void()>);

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

    WebIDL::ExceptionOr<JS::GCPtr<Element>> insert_adjacent_element(String const& where, JS::NonnullGCPtr<Element> element);
    WebIDL::ExceptionOr<void> insert_adjacent_text(String const& where, String const& data);

    // https://w3c.github.io/csswg-drafts/cssom-view-1/#dom-element-scrollintoview
    ErrorOr<void> scroll_into_view(Optional<Variant<bool, ScrollIntoViewOptions>> = {});

    // https://www.w3.org/TR/wai-aria-1.2/#ARIAMixin
#define ARIA_IMPL(name, attribute)                                               \
    Optional<String> name() const override                                       \
    {                                                                            \
        return get_attribute(attribute);                                         \
    }                                                                            \
                                                                                 \
    WebIDL::ExceptionOr<void> set_##name(Optional<String> const& value) override \
    {                                                                            \
        TRY(set_attribute(attribute, value));                                    \
        return {};                                                               \
    }

    // https://www.w3.org/TR/wai-aria-1.2/#accessibilityroleandproperties-correspondence
    ARIA_IMPL(role, "role"sv);
    ARIA_IMPL(aria_active_descendant, "aria-activedescendant"sv);
    ARIA_IMPL(aria_atomic, "aria-atomic"sv);
    ARIA_IMPL(aria_auto_complete, "aria-autocomplete"sv);
    ARIA_IMPL(aria_busy, "aria-busy"sv);
    ARIA_IMPL(aria_checked, "aria-checked"sv);
    ARIA_IMPL(aria_col_count, "aria-colcount"sv);
    ARIA_IMPL(aria_col_index, "aria-colindex"sv);
    ARIA_IMPL(aria_col_span, "aria-colspan"sv);
    ARIA_IMPL(aria_controls, "aria-controls"sv);
    ARIA_IMPL(aria_current, "aria-current"sv);
    ARIA_IMPL(aria_described_by, "aria-describedby"sv);
    ARIA_IMPL(aria_details, "aria-details"sv);
    ARIA_IMPL(aria_drop_effect, "aria-dropeffect"sv);
    ARIA_IMPL(aria_error_message, "aria-errormessage"sv);
    ARIA_IMPL(aria_disabled, "aria-disabled"sv);
    ARIA_IMPL(aria_expanded, "aria-expanded"sv);
    ARIA_IMPL(aria_flow_to, "aria-flowto"sv);
    ARIA_IMPL(aria_grabbed, "aria-grabbed"sv);
    ARIA_IMPL(aria_has_popup, "aria-haspopup"sv);
    ARIA_IMPL(aria_hidden, "aria-hidden"sv);
    ARIA_IMPL(aria_invalid, "aria-invalid"sv);
    ARIA_IMPL(aria_key_shortcuts, "aria-keyshortcuts"sv);
    ARIA_IMPL(aria_label, "aria-label"sv);
    ARIA_IMPL(aria_labelled_by, "aria-labelledby"sv);
    ARIA_IMPL(aria_level, "aria-level"sv);
    ARIA_IMPL(aria_live, "aria-live"sv);
    ARIA_IMPL(aria_modal, "aria-modal"sv);
    ARIA_IMPL(aria_multi_line, "aria-multiline"sv);
    ARIA_IMPL(aria_multi_selectable, "aria-multiselectable"sv);
    ARIA_IMPL(aria_orientation, "aria-orientation"sv);
    ARIA_IMPL(aria_owns, "aria-owns"sv);
    ARIA_IMPL(aria_placeholder, "aria-placeholder"sv);
    ARIA_IMPL(aria_pos_in_set, "aria-posinset"sv);
    ARIA_IMPL(aria_pressed, "aria-pressed"sv);
    ARIA_IMPL(aria_read_only, "aria-readonly"sv);
    ARIA_IMPL(aria_relevant, "aria-relevant"sv);
    ARIA_IMPL(aria_required, "aria-required"sv);
    ARIA_IMPL(aria_role_description, "aria-roledescription"sv);
    ARIA_IMPL(aria_row_count, "aria-rowcount"sv);
    ARIA_IMPL(aria_row_index, "aria-rowindex"sv);
    ARIA_IMPL(aria_row_span, "aria-rowspan"sv);
    ARIA_IMPL(aria_selected, "aria-selected"sv);
    ARIA_IMPL(aria_set_size, "aria-setsize"sv);
    ARIA_IMPL(aria_sort, "aria-sort"sv);
    ARIA_IMPL(aria_value_max, "aria-valuemax"sv);
    ARIA_IMPL(aria_value_min, "aria-valuemin"sv);
    ARIA_IMPL(aria_value_now, "aria-valuenow"sv);
    ARIA_IMPL(aria_value_text, "aria-valuetext"sv);

#undef ARIA_IMPL

    virtual bool exclude_from_accessibility_tree() const override;

    virtual bool include_in_accessibility_tree() const override;

    void enqueue_a_custom_element_upgrade_reaction(HTML::CustomElementDefinition& custom_element_definition);
    void enqueue_a_custom_element_callback_reaction(FlyString const& callback_name, JS::MarkedVector<JS::Value> arguments);

    Vector<Variant<CustomElementUpgradeReaction, CustomElementCallbackReaction>>& custom_element_reaction_queue() { return m_custom_element_reaction_queue; }
    Vector<Variant<CustomElementUpgradeReaction, CustomElementCallbackReaction>> const& custom_element_reaction_queue() const { return m_custom_element_reaction_queue; }

    JS::ThrowCompletionOr<void> upgrade_element(JS::NonnullGCPtr<HTML::CustomElementDefinition> custom_element_definition);
    void try_to_upgrade();

    bool is_defined() const;
    bool is_custom() const;

    Optional<String> const& is_value() const { return m_is_value; }
    void set_is_value(Optional<String> const& is) { m_is_value = is; }

    void set_custom_element_state(CustomElementState value) { m_custom_element_state = value; }
    void setup_custom_element_from_constructor(HTML::CustomElementDefinition& custom_element_definition, Optional<String> const& is_value);

    void scroll(HTML::ScrollToOptions const&);
    void scroll(double x, double y);

    void register_intersection_observer(Badge<IntersectionObserver::IntersectionObserver>, IntersectionObserver::IntersectionObserverRegistration);
    void unregister_intersection_observer(Badge<IntersectionObserver::IntersectionObserver>, JS::NonnullGCPtr<IntersectionObserver::IntersectionObserver>);
    IntersectionObserver::IntersectionObserverRegistration& get_intersection_observer_registration(Badge<DOM::Document>, IntersectionObserver::IntersectionObserver const&);

    enum class ScrollOffsetFor {
        Self,
        PseudoBefore,
        PseudoAfter
    };
    CSSPixelPoint scroll_offset(ScrollOffsetFor type) const { return m_scroll_offset[to_underlying(type)]; }
    void set_scroll_offset(ScrollOffsetFor type, CSSPixelPoint offset) { m_scroll_offset[to_underlying(type)] = offset; }

    enum class Dir {
        Ltr,
        Rtl,
        Auto,
    };
    Optional<Dir> dir() const { return m_dir; }

    enum class Directionality {
        Ltr,
        Rtl,
    };
    Directionality directionality() const;

protected:
    Element(Document&, DOM::QualifiedName);
    virtual void initialize(JS::Realm&) override;

    virtual void children_changed() override;
    virtual i32 default_tab_index_value() const;

    virtual void visit_edges(Cell::Visitor&) override;

    virtual bool id_reference_exists(DeprecatedString const&) const override;

private:
    void make_html_uppercased_qualified_name();

    void invalidate_style_after_attribute_change(FlyString const& attribute_name);

    WebIDL::ExceptionOr<JS::GCPtr<Node>> insert_adjacent(DeprecatedString const& where, JS::NonnullGCPtr<Node> node);

    void enqueue_an_element_on_the_appropriate_element_queue();

    QualifiedName m_qualified_name;
    FlyString m_html_uppercased_qualified_name;

    JS::GCPtr<NamedNodeMap> m_attributes;
    Vector<AttributeChangeSteps> m_attribute_change_steps;
    JS::GCPtr<CSS::ElementInlineCSSStyleDeclaration> m_inline_style;
    JS::GCPtr<DOMTokenList> m_class_list;
    JS::GCPtr<ShadowRoot> m_shadow_root;

    RefPtr<CSS::StyleProperties> m_computed_css_values;
    HashMap<DeprecatedFlyString, CSS::StyleProperty> m_custom_properties;
    Array<HashMap<DeprecatedFlyString, CSS::StyleProperty>, to_underlying(CSS::Selector::PseudoElement::PseudoElementCount)> m_pseudo_element_custom_properties;

    Vector<FlyString> m_classes;
    Optional<Dir> m_dir;

    Array<JS::GCPtr<Layout::Node>, to_underlying(CSS::Selector::PseudoElement::PseudoElementCount)> m_pseudo_element_nodes;

    // https://html.spec.whatwg.org/multipage/custom-elements.html#custom-element-reaction-queue
    // All elements have an associated custom element reaction queue, initially empty. Each item in the custom element reaction queue is of one of two types:
    // NOTE: See the structs at the top of this header.
    Vector<Variant<CustomElementUpgradeReaction, CustomElementCallbackReaction>> m_custom_element_reaction_queue;

    // https://dom.spec.whatwg.org/#concept-element-custom-element-state
    CustomElementState m_custom_element_state { CustomElementState::Undefined };

    // https://dom.spec.whatwg.org/#concept-element-custom-element-definition
    JS::GCPtr<HTML::CustomElementDefinition> m_custom_element_definition;

    // https://dom.spec.whatwg.org/#concept-element-is-value
    Optional<String> m_is_value;

    // https://www.w3.org/TR/intersection-observer/#dom-element-registeredintersectionobservers-slot
    // Element objects have an internal [[RegisteredIntersectionObservers]] slot, which is initialized to an empty list.
    Vector<IntersectionObserver::IntersectionObserverRegistration> m_registered_intersection_observers;

    Array<CSSPixelPoint, 3> m_scroll_offset;
};

template<>
inline bool Node::fast_is<Element>() const { return is_element(); }

WebIDL::ExceptionOr<QualifiedName> validate_and_extract(JS::Realm&, DeprecatedFlyString namespace_, DeprecatedFlyString qualified_name);

}
