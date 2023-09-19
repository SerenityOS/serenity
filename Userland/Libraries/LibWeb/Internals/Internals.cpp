/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/InternalsPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Internals/Internals.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Internals {

Internals::Internals(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

Internals::~Internals() = default;

void Internals::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    Object::set_prototype(&Bindings::ensure_web_prototype<Bindings::InternalsPrototype>(realm, "Internals"));
}

void Internals::signal_text_test_is_done()
{
    if (auto* page = global_object().browsing_context()->page()) {
        page->client().page_did_finish_text_test();
    }
}

void Internals::gc()
{
    vm().heap().collect_garbage();
}

JS::Object* Internals::hit_test(double x, double y)
{
    auto* active_document = global_object().browsing_context()->top_level_browsing_context()->active_document();
    // NOTE: Force a layout update just before hit testing. This is because the current layout tree, which is required
    //       for stacking context traversal, might not exist if this call occurs between the tear_down_layout_tree()
    //       and update_layout() calls
    active_document->update_layout();
    auto result = active_document->paintable_box()->hit_test({ x, y }, Painting::HitTestType::Exact);
    if (result.has_value()) {
        auto hit_tеsting_result = JS::Object::create(realm(), nullptr);
        hit_tеsting_result->define_direct_property("node", result->dom_node(), JS::default_attributes);
        hit_tеsting_result->define_direct_property("indexInNode", JS::Value(result->index_in_node), JS::default_attributes);
        return hit_tеsting_result;
    }
    return nullptr;
}

}
