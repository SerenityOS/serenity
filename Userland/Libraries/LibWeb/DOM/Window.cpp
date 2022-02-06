/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/DisplayLink.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/ResolvedCSSStyleDeclaration.h>
#include <LibWeb/Crypto/Crypto.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/Timer.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/PageTransitionEvent.h>
#include <LibWeb/HighResolutionTime/Performance.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Selection/Selection.h>

namespace Web::DOM {

class RequestAnimationFrameCallback : public RefCounted<RequestAnimationFrameCallback> {
public:
    explicit RequestAnimationFrameCallback(i32 id, Function<void(i32)> handler)
        : m_id(id)
        , m_handler(move(handler))
    {
    }
    ~RequestAnimationFrameCallback() = default;

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
        m_timer = Core::Timer::create_single_shot(16, [] {
            HTML::main_thread_event_loop().schedule();
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

    void run()
    {
        auto taken_callbacks = move(m_callbacks);
        for (auto& it : taken_callbacks) {
            if (!it.value->is_cancelled())
                it.value->invoke();
        }
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

// https://html.spec.whatwg.org/#run-the-animation-frame-callbacks
void run_animation_frame_callbacks(DOM::Document&, double)
{
    // FIXME: Bring this closer to the spec.
    request_animation_frame_driver().run();
}

NonnullRefPtr<Window> Window::create_with_document(Document& document)
{
    return adopt_ref(*new Window(document));
}

Window::Window(Document& document)
    : EventTarget(static_cast<Bindings::ScriptExecutionContext&>(document))
    , m_associated_document(document)
    , m_performance(make<HighResolutionTime::Performance>(*this))
    , m_crypto(Crypto::Crypto::create())
    , m_screen(CSS::Screen::create({}, *this))
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
    NonnullRefPtr<Timer> strong_timer { timer };

    if (timer.type() == Timer::Type::Timeout) {
        m_timers.remove(timer.id());
    }

    HTML::queue_global_task(HTML::Task::Source::TimerTask, associated_document(), [this, strong_this = NonnullRefPtr(*this), strong_timer = NonnullRefPtr(timer)]() mutable {
        // We should not be here if there's no JS wrapper for the Window object.
        VERIFY(wrapper());
        auto& vm = wrapper()->vm();

        [[maybe_unused]] auto rc = JS::call(wrapper()->global_object(), strong_timer->callback(), wrapper());
        if (vm.exception())
            vm.clear_exception();
    });
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
        (void)JS::call(function.global_object(), function, JS::js_undefined(), JS::Value(performance().now()));
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
    auto* browsing_context = associated_document().browsing_context();
    if (!browsing_context)
        return;
    browsing_context->loader().load(new_href, FrameLoader::Type::Navigation);
}

void Window::did_call_location_reload(Badge<Bindings::LocationObject>)
{
    auto* browsing_context = associated_document().browsing_context();
    if (!browsing_context)
        return;
    browsing_context->loader().load(associated_document().url(), FrameLoader::Type::Reload);
}

void Window::did_call_location_replace(Badge<Bindings::LocationObject>, String url)
{
    auto* browsing_context = associated_document().browsing_context();
    if (!browsing_context)
        return;
    auto new_url = associated_document().parse_url(url);
    browsing_context->loader().load(move(new_url), FrameLoader::Type::Navigation);
}

bool Window::dispatch_event(NonnullRefPtr<Event> event)
{
    return EventDispatcher::dispatch(*this, event, true);
}

JS::Object* Window::create_wrapper(JS::GlobalObject& global_object)
{
    return &global_object;
}

// https://www.w3.org/TR/cssom-view-1/#dom-window-innerwidth
int Window::inner_width() const
{
    // The innerWidth attribute must return the viewport width including the size of a rendered scroll bar (if any),
    // or zero if there is no viewport.
    if (auto const* browsing_context = associated_document().browsing_context())
        return browsing_context->viewport_rect().width();
    return 0;
}

// https://www.w3.org/TR/cssom-view-1/#dom-window-innerheight
int Window::inner_height() const
{
    // The innerHeight attribute must return the viewport height including the size of a rendered scroll bar (if any),
    // or zero if there is no viewport.
    if (auto const* browsing_context = associated_document().browsing_context())
        return browsing_context->viewport_rect().height();
    return 0;
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
    return CSS::ResolvedCSSStyleDeclaration::create(element);
}

NonnullRefPtr<CSS::MediaQueryList> Window::match_media(String media)
{
    auto media_query_list = CSS::MediaQueryList::create(associated_document(), parse_media_query_list(CSS::ParsingContext(associated_document()), media));
    associated_document().add_media_query_list(media_query_list);
    return media_query_list;
}

Optional<CSS::MediaFeatureValue> Window::query_media_feature(FlyString const& name) const
{
    // FIXME: Many of these should be dependent on the hardware

    // MEDIAQUERIES-4 properties - https://www.w3.org/TR/mediaqueries-4/#media-descriptor-table
    if (name.equals_ignoring_case("any-hover"sv))
        return CSS::MediaFeatureValue("hover");
    if (name.equals_ignoring_case("any-pointer"sv))
        return CSS::MediaFeatureValue("fine");
    // FIXME: aspect-ratio
    if (name.equals_ignoring_case("color"sv))
        return CSS::MediaFeatureValue(32);
    if (name.equals_ignoring_case("color-gamut"sv))
        return CSS::MediaFeatureValue("srgb");
    if (name.equals_ignoring_case("color-index"sv))
        return CSS::MediaFeatureValue(0);
    // FIXME: device-aspect-ratio
    // FIXME: device-height
    // FIXME: device-width
    if (name.equals_ignoring_case("grid"sv))
        return CSS::MediaFeatureValue(0);
    if (name.equals_ignoring_case("height"sv))
        return CSS::MediaFeatureValue(CSS::Length::make_px(inner_height()));
    if (name.equals_ignoring_case("hover"sv))
        return CSS::MediaFeatureValue("hover");
    if (name.equals_ignoring_case("monochrome"sv))
        return CSS::MediaFeatureValue(0);
    if (name.equals_ignoring_case("hover"sv))
        return CSS::MediaFeatureValue(inner_height() >= inner_width() ? "portrait" : "landscape");
    if (name.equals_ignoring_case("overflow-block"sv))
        return CSS::MediaFeatureValue("scroll");
    // FIXME: overflow-inline
    if (name.equals_ignoring_case("pointer"sv))
        return CSS::MediaFeatureValue("fine");
    // FIXME: resolution
    if (name.equals_ignoring_case("scan"sv))
        return CSS::MediaFeatureValue("progressive");
    if (name.equals_ignoring_case("update"sv))
        return CSS::MediaFeatureValue("fast");
    if (name.equals_ignoring_case("width"sv))
        return CSS::MediaFeatureValue(CSS::Length::make_px(inner_width()));

    // MEDIAQUERIES-5 properties - https://www.w3.org/TR/mediaqueries-5/#media-descriptor-table
    if (name.equals_ignoring_case("prefers-color-scheme")) {
        if (auto* page = this->page()) {
            switch (page->preferred_color_scheme()) {
            case CSS::PreferredColorScheme::Light:
                return CSS::MediaFeatureValue("light");
            case CSS::PreferredColorScheme::Dark:
                return CSS::MediaFeatureValue("dark");
            case CSS::PreferredColorScheme::Auto:
            default:
                return CSS::MediaFeatureValue(page->palette().is_dark() ? "dark" : "light");
            }
        }
    }

    return {};
}

// https://www.w3.org/TR/cssom-view/#dom-window-scrollx
float Window::scroll_x() const
{
    if (auto* page = this->page())
        return page->top_level_browsing_context().viewport_scroll_offset().x();
    return 0;
}

// https://www.w3.org/TR/cssom-view/#dom-window-scrolly
float Window::scroll_y() const
{
    if (auto* page = this->page())
        return page->top_level_browsing_context().viewport_scroll_offset().y();
    return 0;
}

// https://html.spec.whatwg.org/#fire-a-page-transition-event
void Window::fire_a_page_transition_event(FlyString const& event_name, bool persisted)
{
    // To fire a page transition event named eventName at a Window window with a boolean persisted,
    // fire an event named eventName at window, using PageTransitionEvent,
    // with the persisted attribute initialized to persisted,
    HTML::PageTransitionEventInit event_init {};
    event_init.persisted = persisted;
    auto event = HTML::PageTransitionEvent::create(event_name, event_init);

    // ...the cancelable attribute initialized to true,
    event->set_cancelable(true);

    // the bubbles attribute initialized to true,
    event->set_bubbles(true);

    // and legacy target override flag set.
    dispatch_event(move(event));
}

// https://html.spec.whatwg.org/#dom-queuemicrotask
void Window::queue_microtask(JS::FunctionObject& callback)
{
    // The queueMicrotask(callback) method must queue a microtask to invoke callback,
    HTML::queue_a_microtask(associated_document(), [&callback, handle = JS::make_handle(&callback)]() {
        auto& vm = callback.vm();
        [[maybe_unused]] auto rc = JS::call(callback.global_object(), callback, JS::js_null());
        // FIXME: ...and if callback throws an exception, report the exception.
        if (vm.exception())
            vm.clear_exception();
    });
}

float Window::device_pixel_ratio() const
{
    // FIXME: Return 2.0f if we're in HiDPI mode!
    return 1.0f;
}

// https://drafts.csswg.org/cssom-view/#dom-window-screenx
int Window::screen_x() const
{
    // The screenX and screenLeft attributes must return the x-coordinate, relative to the origin of the Web-exposed screen area,
    // of the left of the client window as number of CSS pixels, or zero if there is no such thing.
    return 0;
}

// https://drafts.csswg.org/cssom-view/#dom-window-screeny
int Window::screen_y() const
{
    // The screenY and screenTop attributes must return the y-coordinate, relative to the origin of the screen of the Web-exposed screen area,
    // of the top of the client window as number of CSS pixels, or zero if there is no such thing.
    return 0;
}

// https://w3c.github.io/selection-api/#dom-window-getselection
Selection::Selection* Window::get_selection()
{
    // FIXME: Implement.
    return nullptr;
}

}
