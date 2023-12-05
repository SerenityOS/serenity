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
#include <LibWeb/DOM/NamedNodeMap.h>
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
    global_object().browsing_context()->page().client().inspector_did_load();
}

void Inspector::inspect_dom_node(i32 node_id, Optional<i32> const& pseudo_element)
{
    auto& page = global_object().browsing_context()->page();
    page.client().inspector_did_select_dom_node(node_id, pseudo_element.map([](auto value) {
        VERIFY(value < to_underlying(Web::CSS::Selector::PseudoElement::PseudoElementCount));
        return static_cast<Web::CSS::Selector::PseudoElement>(value);
    }));
}

void Inspector::set_dom_node_text(i32 node_id, String const& text)
{
    global_object().browsing_context()->page().client().inspector_did_set_dom_node_text(node_id, text);
}

void Inspector::set_dom_node_tag(i32 node_id, String const& tag)
{
    global_object().browsing_context()->page().client().inspector_did_set_dom_node_tag(node_id, tag);
}

void Inspector::add_dom_node_attributes(i32 node_id, JS::NonnullGCPtr<DOM::NamedNodeMap> attributes)
{
    global_object().browsing_context()->page().client().inspector_did_add_dom_node_attributes(node_id, attributes);
}

void Inspector::replace_dom_node_attribute(i32 node_id, String const& name, JS::NonnullGCPtr<DOM::NamedNodeMap> replacement_attributes)
{
    global_object().browsing_context()->page().client().inspector_did_replace_dom_node_attribute(node_id, name, replacement_attributes);
}

void Inspector::request_dom_tree_context_menu(i32 node_id, i32 client_x, i32 client_y, String const& type, Optional<String> const& tag_or_attribute_name)
{
    global_object().browsing_context()->page().client().inspector_did_request_dom_tree_context_menu(node_id, { client_x, client_y }, type, tag_or_attribute_name);
}

void Inspector::execute_console_script(String const& script)
{
    global_object().browsing_context()->page().client().inspector_did_execute_console_script(script);
}

}
