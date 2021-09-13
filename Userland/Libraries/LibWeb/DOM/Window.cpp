/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/DisplayLink.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibWeb/CSS/ComputedCSSStyleDeclaration.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/Timer.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HighResolutionTime/Performance.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Page/BrowsingContext.h>
#include <LibWeb/Page/Page.h>

namespace Web::DOM {

class RequestAnimationFrameCallback : public RefCounted<RequestAnimationFrameCallback> {
public:
    explicit RequestAnimationFrameCallback(i32 id, Function<void(i32)> handler)
        : m_id(id)
        , m_handler(move(handler))
    {
    }
    ~RequestAnimationFrameCallback() { }

    i32 id() const { return m_id; }
    bool is_cancelled() const { return !m_handler; }

    void cancel() { m_handler = nullptr; }
    void invoke() { m_handler(m_id); }

private:
    i32 m_id { 0 };
    Function<void(i32)> m_handler;
};

struct RequestAnimationFrameDriver {
    RequestAnimationFrameDriver()
    {
        m_timer = Core::Timer::create_single_shot(16, [this] {
            auto taken_callbacks = move(m_callbacks);
            for (auto& it : taken_callbacks) {
                if (!it.value->is_cancelled())
                    it.value->invoke();
            }
        });
    }

    NonnullRefPtr<RequestAnimationFrameCallback> add(Function<void(i32)> handler)
    {
        auto id = m_id_allocator.allocate();
        auto callback = adopt_ref(*new RequestAnimationFrameCallback { id, move(handler) });
        m_callbacks.set(id, callback);
        if (!m_timer->is_active())
            m_timer->start();
        return callback;
    }

    bool remove(i32 id)
    {
        auto it = m_callbacks.find(id);
        if (it == m_callbacks.end())
            return false;
        m_callbacks.remove(it);
        m_id_allocator.deallocate(id);
        return true;
    }

private:
    HashMap<i32, NonnullRefPtr<RequestAnimationFrameCallback>> m_callbacks;
    IDAllocator m_id_allocator;
    RefPtr<Core::Timer> m_timer;
};

static RequestAnimationFrameDriver& request_animation_frame_driver()
{
    static RequestAnimationFrameDriver driver;
    return driver;
}

NonnullRefPtr<Window> Window::create_with_document(Document& document)
{
    return adopt_ref(*new Window(document));
}

Window::Window(Document& document)
    : EventTarget(static_cast<Bindings::ScriptExecutionContext&>(document))
    , m_associated_document(document)
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

void Window::alert(String const& message)
{
    if (auto* page = this->page())
        page->client().page_did_request_alert(message);
}

bool Window::confirm(String const& message)
{
    if (auto* page = this->page())
        return page->client().page_did_request_confirm(message);
    return false;
}

String Window::prompt(String const& message, String const& default_)
{
    if (auto* page = this->page())
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

i32 Window::request_animation_frame(JS::FunctionObject& js_callback)
{
    auto callback = request_animation_frame_driver().add([this, handle = JS::make_handle(&js_callback)](i32 id) mutable {
        auto& function = *handle.cell();
        auto& vm = function.vm();
        (void)vm.call(function, JS::js_undefined(), JS::Value((double)Core::DateTime::now().timestamp()));
        if (vm.exception())
            vm.clear_exception();
        m_request_animation_frame_callbacks.remove(id);
    });
    m_request_animation_frame_callbacks.set(callback->id(), callback);
    return callback->id();
}

void Window::cancel_animation_frame(i32 id)
{
    auto it = m_request_animation_frame_callbacks.find(id);
    if (it == m_request_animation_frame_callbacks.end())
        return;
    it->value->cancel();
    m_request_animation_frame_callbacks.remove(it);
}

void Window::did_set_location_href(Badge<Bindings::LocationObject>, AK::URL const& new_href)
{
    auto* frame = associated_document().browsing_context();
    if (!frame)
        return;
    frame->loader().load(new_href, FrameLoader::Type::Navigation);
}

void Window::did_call_location_reload(Badge<Bindings::LocationObject>)
{
    auto* frame = associated_document().browsing_context();
    if (!frame)
        return;
    frame->loader().load(associated_document().url(), FrameLoader::Type::Reload);
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
    if (!associated_document().layout_node())
        return 0;
    return associated_document().layout_node()->width();
}

int Window::inner_height() const
{
    if (!associated_document().layout_node())
        return 0;
    return associated_document().layout_node()->height();
}

Page* Window::page()
{
    return associated_document().page();
}

Page const* Window::page() const
{
    return associated_document().page();
}

NonnullRefPtr<CSS::CSSStyleDeclaration> Window::get_computed_style(DOM::Element& element) const
{
    return CSS::ComputedCSSStyleDeclaration::create(element);
}

NonnullRefPtr<CSS::MediaQueryList> Window::match_media(String media)
{
    return CSS::MediaQueryList::create(associated_document(), move(media));
}

}
