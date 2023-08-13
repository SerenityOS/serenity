/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Attr.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/MutationType.h>
#include <LibWeb/DOM/StaticNodeList.h>
#include <LibWeb/HTML/CustomElements/CustomElementReactionNames.h>

namespace Web::DOM {

JS::NonnullGCPtr<Attr> Attr::create(Document& document, DeprecatedFlyString local_name, DeprecatedString value, Element* owner_element)
{
    return document.heap().allocate<Attr>(document.realm(), document, QualifiedName(move(local_name), {}, {}), move(value), owner_element);
}

JS::NonnullGCPtr<Attr> Attr::create(Document& document, QualifiedName qualified_name, DeprecatedString value, Element* owner_element)
{
    return document.heap().allocate<Attr>(document.realm(), document, move(qualified_name), move(value), owner_element);
}

JS::NonnullGCPtr<Attr> Attr::clone(Document& document)
{
    return *heap().allocate<Attr>(realm(), document, m_qualified_name, m_value, nullptr);
}

Attr::Attr(Document& document, QualifiedName qualified_name, DeprecatedString value, Element* owner_element)
    : Node(document, NodeType::ATTRIBUTE_NODE)
    , m_qualified_name(move(qualified_name))
    , m_value(move(value))
    , m_owner_element(owner_element)
{
}

void Attr::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::AttrPrototype>(realm, "Attr"));
}

void Attr::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_owner_element.ptr());
}

Element* Attr::owner_element()
{
    return m_owner_element.ptr();
}

Element const* Attr::owner_element() const
{
    return m_owner_element.ptr();
}

void Attr::set_owner_element(Element* owner_element)
{
    m_owner_element = owner_element;
}

// https://dom.spec.whatwg.org/#set-an-existing-attribute-value
void Attr::set_value(DeprecatedString value)
{
    // 1. If attribute’s element is null, then set attribute’s value to value.
    if (!owner_element()) {
        m_value = move(value);
        return;
    }

    // 2. Otherwise, change attribute to value.
    // https://dom.spec.whatwg.org/#concept-element-attributes-change
    // 1. Handle attribute changes for attribute with attribute’s element, attribute’s value, and value.
    handle_attribute_changes(*owner_element(), m_value, value);

    // 2. Set attribute’s value to value.
    m_value = move(value);
}

// https://dom.spec.whatwg.org/#handle-attribute-changes
void Attr::handle_attribute_changes(Element& element, DeprecatedString const& old_value, DeprecatedString const& new_value)
{
    // 1. Queue a mutation record of "attributes" for element with attribute’s local name, attribute’s namespace, oldValue, « », « », null, and null.
    element.queue_mutation_record(MutationType::attributes, local_name(), namespace_uri(), old_value, {}, {}, nullptr, nullptr);

    // 2. If element is custom, then enqueue a custom element callback reaction with element, callback name "attributeChangedCallback", and an argument list containing attribute’s local name, oldValue, newValue, and attribute’s namespace.
    if (element.is_custom()) {
        auto& vm = this->vm();

        JS::MarkedVector<JS::Value> arguments { vm.heap() };
        arguments.append(JS::PrimitiveString::create(vm, local_name()));
        arguments.append(old_value.is_null() ? JS::js_null() : JS::PrimitiveString::create(vm, old_value));
        arguments.append(new_value.is_null() ? JS::js_null() : JS::PrimitiveString::create(vm, new_value));
        arguments.append(namespace_uri().is_null() ? JS::js_null() : JS::PrimitiveString::create(vm, namespace_uri()));

        element.enqueue_a_custom_element_callback_reaction(HTML::CustomElementReactionNames::attributeChangedCallback, move(arguments));
    }

    // FIXME: 3. Run the attribute change steps with element, attribute’s local name, oldValue, newValue, and attribute’s namespace.
}

}
