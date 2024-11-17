/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/InternalsPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/DOMURL/DOMURL.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Internals/Internals.h>
#include <LibWeb/Page/InputEvent.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/ViewportPaintable.h>

namespace Web::Internals {

JS_DEFINE_ALLOCATOR(Internals);

Internals::Internals(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

Internals::~Internals() = default;

void Internals::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(Internals);
}

HTML::Window& Internals::internals_window() const
{
    return verify_cast<HTML::Window>(HTML::relevant_global_object(*this));
}

Page& Internals::internals_page() const
{
    return internals_window().page();
}

void Internals::signal_text_test_is_done()
{
    internals_page().client().page_did_finish_text_test();
}

void Internals::gc()
{
    vm().heap().collect_garbage();
}

JS::Object* Internals::hit_test(double x, double y)
{
    auto& active_document = internals_window().associated_document();
    // NOTE: Force a layout update just before hit testing. This is because the current layout tree, which is required
    //       for stacking context traversal, might not exist if this call occurs between the tear_down_layout_tree()
    //       and update_layout() calls
    active_document.update_layout();
    auto result = active_document.paintable_box()->hit_test({ x, y }, Painting::HitTestType::Exact);
    if (result.has_value()) {
        auto hit_tеsting_result = JS::Object::create(realm(), nullptr);
        hit_tеsting_result->define_direct_property("node", result->dom_node(), JS::default_attributes);
        hit_tеsting_result->define_direct_property("indexInNode", JS::Value(result->index_in_node), JS::default_attributes);
        return hit_tеsting_result;
    }
    return nullptr;
}

void Internals::send_text(HTML::HTMLElement& target, String const& text)
{
    auto& page = internals_page();
    target.focus();

    for (auto code_point : text.code_points())
        page.handle_keydown(UIEvents::code_point_to_key_code(code_point), 0, code_point);
}

void Internals::send_key(HTML::HTMLElement& target, String const& key_name)
{
    auto key_code = UIEvents::key_code_from_string(key_name);
    target.focus();

    internals_page().handle_keydown(key_code, 0, 0);
}

void Internals::commit_text()
{
    internals_page().handle_keydown(UIEvents::Key_Return, 0, 0);
}

void Internals::click(double x, double y)
{
    click(x, y, UIEvents::MouseButton::Primary);
}

void Internals::middle_click(double x, double y)
{
    click(x, y, UIEvents::MouseButton::Middle);
}

void Internals::click(double x, double y, UIEvents::MouseButton button)
{
    auto& page = internals_page();

    auto position = page.css_to_device_point({ x, y });
    page.handle_mousedown(position, position, button, 0, 0);
    page.handle_mouseup(position, position, button, 0, 0);
}

void Internals::move_pointer_to(double x, double y)
{
    auto& page = internals_page();

    auto position = page.css_to_device_point({ x, y });
    page.handle_mousemove(position, position, 0, 0);
}

void Internals::wheel(double x, double y, double delta_x, double delta_y)
{
    auto& page = internals_page();

    auto position = page.css_to_device_point({ x, y });
    page.handle_mousewheel(position, position, 0, 0, 0, delta_x, delta_y);
}

WebIDL::ExceptionOr<bool> Internals::dispatch_user_activated_event(DOM::EventTarget& target, DOM::Event& event)
{
    event.set_is_trusted(true);
    return target.dispatch_event(event);
}

void Internals::spoof_current_url(String const& url_string)
{
    auto url = DOMURL::parse(url_string);

    VERIFY(url.is_valid());

    auto origin = DOMURL::url_origin(url);

    auto& window = internals_window();
    window.associated_document().set_url(url);
    window.associated_document().set_origin(origin);
}

JS::NonnullGCPtr<InternalAnimationTimeline> Internals::create_internal_animation_timeline()
{
    auto& realm = this->realm();
    return realm.heap().allocate<InternalAnimationTimeline>(realm, realm);
}

void Internals::simulate_drag_start(double x, double y, String const& name, String const& contents)
{
    Vector<HTML::SelectedFile> files;
    files.empend(name.to_byte_string(), MUST(ByteBuffer::copy(contents.bytes())));

    auto& page = internals_page();

    auto position = page.css_to_device_point({ x, y });
    page.handle_drag_and_drop_event(DragEvent::Type::DragStart, position, position, UIEvents::MouseButton::Primary, 0, 0, move(files));
}

void Internals::simulate_drag_move(double x, double y)
{
    auto& page = internals_page();

    auto position = page.css_to_device_point({ x, y });
    page.handle_drag_and_drop_event(DragEvent::Type::DragMove, position, position, UIEvents::MouseButton::Primary, 0, 0, {});
}

void Internals::simulate_drop(double x, double y)
{
    auto& page = internals_page();

    auto position = page.css_to_device_point({ x, y });
    page.handle_drag_and_drop_event(DragEvent::Type::Drop, position, position, UIEvents::MouseButton::Primary, 0, 0, {});
}

}
