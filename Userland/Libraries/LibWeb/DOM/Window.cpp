/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/DisplayLink.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/Timer.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HighResolutionTime/Performance.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Page/BrowsingContext.h>
#include <LibWeb/Page/Page.h>

namespace Web::DOM {

NonnullRefPtr<Window> Window::create_with_document(Document& document)
{
    return adopt_ref(*new Window(document));
}

Window::Window(Document& document)
    : EventTarget(static_cast<Bindings::ScriptExecutionContext&>(document))
    , m_document(document)
    , m_performance(make<HighResolutionTime::Performance>(*this))
    , m_screen(CSS::Screen::create(*this))
{
}

Window::~Window()
{
}

void Window::set_wrapper(Badge<Bindings::WindowObject>, Bindings::WindowObject& wrapper)
{
    m_wrapper = wrapper.make_weak_ptr();
}

void Window::alert(const String& message)
{
    if (auto* page = m_document.page())
        page->client().page_did_request_alert(message);
}

bool Window::confirm(const String& message)
{
    if (auto* page = m_document.page())
        return page->client().page_did_request_confirm(message);
    return false;
}

String Window::prompt(const String& message, const String& default_)
{
    if (auto* page = m_document.page())
        return page->client().page_did_request_prompt(message, default_);
    return {};
}

i32 Window::set_interval(JS::FunctionObject& callback, i32 interval)
{
    auto timer = Timer::create_interval(*this, interval, callback);
    m_timers.set(timer->id(), timer);
    return timer->id();
}

i32 Window::set_timeout(JS::FunctionObject& callback, i32 interval)
{
    auto timer = Timer::create_timeout(*this, interval, callback);
    m_timers.set(timer->id(), timer);
    return timer->id();
}

void Window::timer_did_fire(Badge<Timer>, Timer& timer)
{
    // We should not be here if there's no JS wrapper for the Window object.
    VERIFY(wrapper());
    auto& vm = wrapper()->vm();

    // NOTE: This protector pointer keeps the timer alive until the end of this function no matter what.
    NonnullRefPtr protector(timer);

    if (timer.type() == Timer::Type::Timeout) {
        m_timers.remove(timer.id());
    }

    [[maybe_unused]] auto rc = vm.call(timer.callback(), wrapper());
    if (vm.exception())
        vm.clear_exception();
}

i32 Window::allocate_timer_id(Badge<Timer>)
{
    return m_timer_id_allocator.allocate();
}

void Window::deallocate_timer_id(Badge<Timer>, i32 id)
{
    m_timer_id_allocator.deallocate(id);
}

void Window::clear_timeout(i32 timer_id)
{
    m_timers.remove(timer_id);
}

void Window::clear_interval(i32 timer_id)
{
    m_timers.remove(timer_id);
}

i32 Window::request_animation_frame(JS::FunctionObject& callback)
{
    // FIXME: This is extremely fake!
    static double fake_timestamp = 0;

    i32 link_id = GUI::DisplayLink::register_callback([handle = make_handle(&callback)](i32 link_id) {
        auto& function = const_cast<JS::FunctionObject&>(static_cast<const JS::FunctionObject&>(*handle.cell()));
        auto& vm = function.vm();
        fake_timestamp += 10;
        [[maybe_unused]] auto rc = vm.call(function, JS::js_undefined(), JS::Value(fake_timestamp));
        if (vm.exception())
            vm.clear_exception();
        GUI::DisplayLink::unregister_callback(link_id);
    });

    // FIXME: Don't hand out raw DisplayLink ID's to JavaScript!
    return link_id;
}

void Window::cancel_animation_frame(i32 id)
{
    // FIXME: We should not be passing untrusted numbers to DisplayLink::unregister_callback()!
    GUI::DisplayLink::unregister_callback(id);
}

void Window::did_set_location_href(Badge<Bindings::LocationObject>, const URL& new_href)
{
    auto* frame = document().browsing_context();
    if (!frame)
        return;
    frame->loader().load(new_href, FrameLoader::Type::Navigation);
}

void Window::did_call_location_reload(Badge<Bindings::LocationObject>)
{
    auto* frame = document().browsing_context();
    if (!frame)
        return;
    frame->loader().load(document().url(), FrameLoader::Type::Reload);
}

bool Window::dispatch_event(NonnullRefPtr<Event> event)
{
    return EventDispatcher::dispatch(*this, event, true);
}

JS::Object* Window::create_wrapper(JS::GlobalObject& global_object)
{
    return &global_object;
}

int Window::inner_width() const
{
    if (!document().layout_node())
        return 0;
    return document().layout_node()->width();
}

int Window::inner_height() const
{
    if (!document().layout_node())
        return 0;
    return document().layout_node()->height();
}

}
