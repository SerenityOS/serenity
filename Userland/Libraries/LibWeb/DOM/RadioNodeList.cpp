/*
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/RadioNodeListPrototype.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/RadioNodeList.h>

namespace Web::DOM {

JS::NonnullGCPtr<RadioNodeList> RadioNodeList::create(JS::Realm& realm, Node& root, Scope scope, Function<bool(Node const&)> filter)
{
    return realm.heap().allocate<RadioNodeList>(realm, realm, root, scope, move(filter));
}

RadioNodeList::RadioNodeList(JS::Realm& realm, Node& root, Scope scope, Function<bool(Node const&)> filter)
    : LiveNodeList(realm, root, scope, move(filter))
{
}

RadioNodeList::~RadioNodeList() = default;

void RadioNodeList::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::RadioNodeListPrototype>(realm, "RadioNodeList"));
}

}
