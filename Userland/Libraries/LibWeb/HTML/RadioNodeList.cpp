/*
 * Copyright (c) 2022, Johan Dahlin <jdahlin@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/NodeList.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/RadioNodeList.h>

namespace Web::HTML {

RadioNodeList::RadioNodeList(JS::Realm& realm)
    : DOM::NodeList(realm)
{
}

RadioNodeList::~RadioNodeList() = default;

// https://html.spec.whatwg.org/multipage/common-dom-interfaces.html#dom-radionodelist-value
String RadioNodeList::value() const
{
    // 1. Let element be the first element in tree order represented by the RadioNodeList object
    // that is an input element whose type attribute is in the Radio Button state and whose
    // checkedness is true. Otherwise, let it be null.

    // 2. If element is null, return the empty string.

    // 3. If element is an element with no value attribute, return the string "on".

    // 4. Otherwise, return the value of element's value attribute.
    TODO();
    return "";
}

// https://html.spec.whatwg.org/multipage/common-dom-interfaces.html#dom-radionodelist-value
WebIDL::ExceptionOr<void> RadioNodeList::set_value(String const& name)
{
    (void)name;
    // 1. If the new value is the string "on": let element be the first element in tree order represented
    // by the RadioNodeList object that is an input element whose type attribute is in the Radio Button state and
    // whose value content attribute is either absent, or present and equal to the new value, if any.
    // If no such element exists, then instead let element be null.

    // Otherwise: let element be the first element in tree order represented by the RadioNodeList object that is
    // an input element whose type attribute is in the Radio Button state and whose value content attribute is
    // present and equal to the new value, if any. If no such element exists, then instead let element be null.

    // 2. If element is not null, then set its checkedness to true.
    TODO();
    return {};
};

}
