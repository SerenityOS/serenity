/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLDialogElement.h>

namespace Web::HTML {

HTMLDialogElement::HTMLDialogElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLDialogElement::~HTMLDialogElement() = default;

void HTMLDialogElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLDialogElementPrototype>(realm, "HTMLDialogElement"));
}

// https://html.spec.whatwg.org/multipage/interactive-elements.html#dom-dialog-show
void HTMLDialogElement::show()
{
    dbgln("(STUBBED) HTMLDialogElement::show()");
}

// https://html.spec.whatwg.org/multipage/interactive-elements.html#dom-dialog-showmodal
void HTMLDialogElement::show_modal()
{
    dbgln("(STUBBED) HTMLDialogElement::show_modal()");
}

// https://html.spec.whatwg.org/multipage/interactive-elements.html#dom-dialog-close
void HTMLDialogElement::close(Optional<String>)
{
    dbgln("(STUBBED) HTMLDialogElement::close()");
}

// https://html.spec.whatwg.org/multipage/interactive-elements.html#dom-dialog-returnvalue
String HTMLDialogElement::return_value() const
{
    return m_return_value;
}

// https://html.spec.whatwg.org/multipage/interactive-elements.html#dom-dialog-returnvalue
void HTMLDialogElement::set_return_value(String return_value)
{
    m_return_value = move(return_value);
}

}
