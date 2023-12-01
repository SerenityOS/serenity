/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Realm.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/InspectorPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Internals/Inspector.h>
#include <LibWeb/Page/Page.h>

namespace Web::Internals {

JS_DEFINE_ALLOCATOR(Inspector);

Inspector::Inspector(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

Inspector::~Inspector() = default;

void Inspector::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    Object::set_prototype(&Bindings::ensure_web_prototype<Bindings::InspectorPrototype>(realm, "Inspector"_fly_string));
}

void Inspector::inspector_loaded()
{
    if (auto* page = global_object().browsing_context()->page())
        page->client().inspector_did_load();
}

void Inspector::inspect_dom_node(i32 node_id, Optional<i32> const& pseudo_element)
{
    if (auto* page = global_object().browsing_context()->page()) {
        page->client().inspector_did_select_dom_node(node_id, pseudo_element.map([](auto value) {
            VERIFY(value < to_underlying(Web::CSS::Selector::PseudoElement::PseudoElementCount));
            return static_cast<Web::CSS::Selector::PseudoElement>(value);
        }));
    }
}

void Inspector::execute_console_script(String const& script)
{
    if (auto* page = global_object().browsing_context()->page())
        page->client().inspector_did_execute_console_script(script);
}

}
