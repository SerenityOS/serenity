/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLOptGroupElement.h>
#include <LibWeb/HTML/HTMLOptionElement.h>
#include <LibWeb/HTML/HTMLSelectElement.h>

namespace Web::HTML {

HTMLSelectElement::HTMLSelectElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&Bindings::cached_web_prototype(realm(), "HTMLSelectElement"));
}

HTMLSelectElement::~HTMLSelectElement() = default;

void HTMLSelectElement::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_options.ptr());
}

// https://html.spec.whatwg.org/multipage/form-elements.html#dom-select-options
JS::GCPtr<HTMLOptionsCollection> const& HTMLSelectElement::options()
{
    if (!m_options) {
        m_options = HTMLOptionsCollection::create(*this, [](DOM::Element const& element) {
            // https://html.spec.whatwg.org/multipage/form-elements.html#concept-select-option-list
            // The list of options for a select element consists of all the option element children of
            // the select element, and all the option element children of all the optgroup element children
            // of the select element, in tree order.
            return is<HTMLOptionElement>(element);
        });
    }
    return m_options;
}

// https://html.spec.whatwg.org/multipage/form-elements.html#dom-select-length
size_t HTMLSelectElement::length()
{
    // The length IDL attribute must return the number of nodes represented by the options collection. On setting, it must act like the attribute of the same name on the options collection.
    return const_cast<HTMLOptionsCollection&>(*options()).length();
}

// https://html.spec.whatwg.org/multipage/form-elements.html#dom-select-item
DOM::Element* HTMLSelectElement::item(size_t index)
{
    // The item(index) method must return the value returned by the method of the same name on the options collection, when invoked with the same argument.
    return const_cast<HTMLOptionsCollection&>(*options()).item(index);
}

// https://html.spec.whatwg.org/multipage/form-elements.html#dom-select-nameditem
DOM::Element* HTMLSelectElement::named_item(FlyString const& name)
{
    // The namedItem(name) method must return the value returned by the method of the same name on the options collection, when invoked with the same argument.
    return const_cast<HTMLOptionsCollection&>(*options()).named_item(name);
}

// https://html.spec.whatwg.org/multipage/form-elements.html#dom-select-add
WebIDL::ExceptionOr<void> HTMLSelectElement::add(HTMLOptionOrOptGroupElement element, Optional<HTMLElementOrElementIndex> before)
{
    // Similarly, the add(element, before) method must act like its namesake method on that same options collection.
    return const_cast<HTMLOptionsCollection&>(*options()).add(move(element), move(before));
}

// https://html.spec.whatwg.org/multipage/form-elements.html#concept-select-option-list
Vector<JS::Handle<HTMLOptionElement>> HTMLSelectElement::list_of_options() const
{
    // The list of options for a select element consists of all the option element children of the select element,
    // and all the option element children of all the optgroup element children of the select element, in tree order.
    Vector<JS::Handle<HTMLOptionElement>> list;

    for_each_child_of_type<HTMLOptionElement>([&](HTMLOptionElement const& option_element) {
        list.append(JS::make_handle(const_cast<HTMLOptionElement&>(option_element)));
    });

    for_each_child_of_type<HTMLOptGroupElement>([&](HTMLOptGroupElement const& optgroup_element) {
        optgroup_element.for_each_child_of_type<HTMLOptionElement>([&](HTMLOptionElement const& option_element) {
            list.append(JS::make_handle(const_cast<HTMLOptionElement&>(option_element)));
        });
    });

    return list;
}

// https://html.spec.whatwg.org/multipage/form-elements.html#dom-select-selectedindex
int HTMLSelectElement::selected_index() const
{
    // The selectedIndex IDL attribute, on getting, must return the index of the first option element in the list of options
    // in tree order that has its selectedness set to true, if any. If there isn't one, then it must return −1.

    int index = 0;
    for (auto const& option_element : list_of_options()) {
        if (option_element->selected())
            return index;
        ++index;
    }
    return -1;
}

void HTMLSelectElement::set_selected_index(int index)
{
    // On setting, the selectedIndex attribute must set the selectedness of all the option elements in the list of options to false,
    // and then the option element in the list of options whose index is the given new value,
    // if any, must have its selectedness set to true and its dirtiness set to true.
    auto options = list_of_options();
    for (auto& option : options)
        option->m_selected = false;

    if (index < 0 || index >= static_cast<int>(options.size()))
        return;

    auto& selected_option = options[index];
    selected_option->m_selected = true;
    selected_option->m_dirty = true;
}

}
