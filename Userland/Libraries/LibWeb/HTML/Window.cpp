/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibWeb/Bindings/IDLAbstractOperations.h>
#include <LibWeb/Bindings/IdleDeadlineWrapper.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/ResolvedCSSStyleDeclaration.h>
#include <LibWeb/Crypto/Crypto.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/PageTransitionEvent.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Scripting/ExceptionReporter.h>
#include <LibWeb/HTML/Storage.h>
#include <LibWeb/HTML/Timer.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/HighResolutionTime/Performance.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/RequestIdleCallback/IdleDeadline.h>
#include <LibWeb/Selection/Selection.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/#run-the-animation-frame-callbacks
void run_animation_frame_callbacks(DOM::Document& document, double)
{
    // FIXME: Bring this closer to the spec.
    document.window().animation_frame_callback_driver().run();
}

class IdleCallback : public RefCounted<IdleCallback> {
public:
    explicit IdleCallback(Function<JS::Completion(NonnullRefPtr<RequestIdleCallback::IdleDeadline>)> handler, u32 handle)
        : m_handler(move(handler))
        , m_handle(handle)
    {
    }
    ~IdleCallback() = default;

    JS::Completion invoke(NonnullRefPtr<RequestIdleCallback::IdleDeadline> deadline) { return m_handler(move(deadline)); }
    u32 handle() const { return m_handle; }

private:
    Function<JS::Completion(NonnullRefPtr<RequestIdleCallback::IdleDeadline>)> m_handler;
    u32 m_handle { 0 };
};

NonnullRefPtr<Window> Window::create()
{
    return adopt_ref(*new Window);
}

NonnullRefPtr<Window> Window::create_with_document(DOM::Document& document)
{
    return adopt_ref(*new Window(document));
}

Window::Window()
    : DOM::EventTarget()
    , m_performance(make<HighResolutionTime::Performance>(*this))
    , m_crypto(Crypto::Crypto::create())
    , m_screen(CSS::Screen::create({}, *this))
{
}

Window::Window(DOM::Document& document)
    : DOM::EventTarget()
    , m_associated_document(document)
    , m_performance(make<HighResolutionTime::Performance>(*this))
    , m_crypto(Crypto::Crypto::create())
    , m_screen(CSS::Screen::create({}, *this))
{
}

Window::~Window() = default;

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

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-settimeout
i32 Window::set_timeout(Bindings::TimerHandler handler, i32 timeout, JS::MarkedVector<JS::Value> arguments)
{
    return run_timer_initialization_steps(move(handler), timeout, move(arguments), Repeat::No);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-setinterval
i32 Window::set_interval(Bindings::TimerHandler handler, i32 timeout, JS::MarkedVector<JS::Value> arguments)
{
    return run_timer_initialization_steps(move(handler), timeout, move(arguments), Repeat::Yes);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-cleartimeout
void Window::clear_timeout(i32 id)
{
    m_timers.remove(id);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-clearinterval
void Window::clear_interval(i32 id)
{
    m_timers.remove(id);
}

void Window::deallocate_timer_id(Badge<Timer>, i32 id)
{
    m_timer_id_allocator.deallocate(id);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#timer-initialisation-steps
i32 Window::run_timer_initialization_steps(Bindings::TimerHandler handler, i32 timeout, JS::MarkedVector<JS::Value> arguments, Repeat repeat, Optional<i32> previous_id)
{
    // 1. Let thisArg be global if that is a WorkerGlobalScope object; otherwise let thisArg be the WindowProxy that corresponds to global.

    // 2. If previousId was given, let id be previousId; otherwise, let id be an implementation-defined integer that is greater than zero and does not already exist in global's map of active timers.
    auto id = previous_id.has_value() ? previous_id.value() : m_timer_id_allocator.allocate();

    // FIXME: 3. If the surrounding agent's event loop's currently running task is a task that was created by this algorithm, then let nesting level be the task's timer nesting level. Otherwise, let nesting level be zero.

    // 4. If timeout is less than 0, then set timeout to 0.
    if (timeout < 0)
        timeout = 0;

    // FIXME: 5. If nesting level is greater than 5, and timeout is less than 4, then set timeout to 4.

    // 6. Let callerRealm be the current Realm Record, and calleeRealm be global's relevant Realm.
    // FIXME: Implement this when step 9.2 is implemented.

    // 7. Let initiating script be the active script.
    // 8. Assert: initiating script is not null, since this algorithm is always called from some script.

    // 9. Let task be a task that runs the following substeps:
    auto task = [weak_window = make_weak_ptr(), handler = move(handler), timeout, arguments = move(arguments), repeat, id]() mutable {
        auto window = weak_window.strong_ref();
        if (!window)
            return;

        // 1. If id does not exist in global's map of active timers, then abort these steps.
        if (!window->m_timers.contains(id))
            return;

        handler.visit(
            // 2. If handler is a Function, then invoke handler given arguments with the callback this value set to thisArg. If this throws an exception, catch it, and report the exception.
            [&](JS::Handle<Bindings::CallbackType> callback) {
                if (auto result = Bindings::IDL::invoke_callback(*callback, window->wrapper(), arguments); result.is_error())
                    HTML::report_exception(result);
            },
            // 3. Otherwise:
            [&](String const& source) {
                // 1. Assert: handler is a string.
                // FIXME: 2. Perform HostEnsureCanCompileStrings(callerRealm, calleeRealm). If this throws an exception, catch it, report the exception, and abort these steps.

                // 3. Let settings object be global's relevant settings object.
                auto& settings_object = window->associated_document().relevant_settings_object();

                // 4. Let base URL be initiating script's base URL.
                auto url = window->associated_document().url();

                // 5. Assert: base URL is not null, as initiating script is a classic script or a JavaScript module script.

                // 6. Let fetch options be a script fetch options whose cryptographic nonce is initiating script's fetch options's cryptographic nonce, integrity metadata is the empty string, parser metadata is "not-parser-inserted", credentials mode is initiating script's fetch options's credentials mode, and referrer policy is initiating script's fetch options's referrer policy.
                // 7. Let script be the result of creating a classic script given handler, settings object, base URL, and fetch options.
                auto script = HTML::ClassicScript::create(url.basename(), source, settings_object, url);

                // 8. Run the classic script script.
                (void)script->run();
            });

        // 4. If id does not exist in global's map of active timers, then abort these steps.
        if (!window->m_timers.contains(id))
            return;

        switch (repeat) {
        // 5. If repeat is true, then perform the timer initialization steps again, given global, handler, timeout, arguments, true, and id.
        case Repeat::Yes:
            window->run_timer_initialization_steps(handler, timeout, move(arguments), repeat, id);
            break;

        // 6. Otherwise, remove global's map of active timers[id].
        case Repeat::No:
            window->m_timers.remove(id);
            break;
        }
    };

    // FIXME: 10. Increment nesting level by one.
    // FIXME: 11. Set task's timer nesting level to nesting level.

    // 12. Let completionStep be an algorithm step which queues a global task on the timer task source given global to run task.
    auto completion_step = [weak_window = make_weak_ptr(), task = move(task)]() mutable {
        auto window = weak_window.strong_ref();
        if (!window)
            return;

        HTML::queue_global_task(HTML::Task::Source::TimerTask, *window->wrapper(), move(task));
    };

    // 13. Run steps after a timeout given global, "setTimeout/setInterval", timeout, completionStep, and id.
    auto timer = Timer::create(*this, timeout, move(completion_step), id);
    m_timers.set(id, timer);
    timer->start();

    // 14. Return id.
    return id;
}

// https://html.spec.whatwg.org/multipage/imagebitmap-and-animations.html#run-the-animation-frame-callbacks
i32 Window::request_animation_frame(Bindings::CallbackType& js_callback)
{
    return m_animation_frame_callback_driver.add([this, js_callback = JS::make_handle(js_callback)](auto) mutable {
        // 3. Invoke callback, passing now as the only argument,
        auto result = Bindings::IDL::invoke_callback(*js_callback, {}, JS::Value(performance().now()));

        // and if an exception is thrown, report the exception.
        if (result.is_error())
            HTML::report_exception(result);
    });
}

void Window::cancel_animation_frame(i32 id)
{
    m_animation_frame_callback_driver.remove(id);
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

bool Window::dispatch_event(NonnullRefPtr<DOM::Event> event)
{
    return DOM::EventDispatcher::dispatch(*this, event, true);
}

JS::Object* Window::create_wrapper(JS::Realm& realm)
{
    return &realm.global_object();
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

CSS::CSSStyleDeclaration* Window::get_computed_style(DOM::Element& element) const
{
    return CSS::ResolvedCSSStyleDeclaration::create(element);
}

NonnullRefPtr<CSS::MediaQueryList> Window::match_media(String media)
{
    auto media_query_list = CSS::MediaQueryList::create(associated_document(), parse_media_query_list(CSS::Parser::ParsingContext(associated_document()), media));
    associated_document().add_media_query_list(media_query_list);
    return media_query_list;
}

Optional<CSS::MediaFeatureValue> Window::query_media_feature(CSS::MediaFeatureID media_feature) const
{
    // FIXME: Many of these should be dependent on the hardware

    // https://www.w3.org/TR/mediaqueries-5/#media-descriptor-table
    switch (media_feature) {
    case CSS::MediaFeatureID::AnyHover:
        return CSS::MediaFeatureValue(CSS::ValueID::Hover);
    case CSS::MediaFeatureID::AnyPointer:
        return CSS::MediaFeatureValue(CSS::ValueID::Fine);
    case CSS::MediaFeatureID::AspectRatio:
        return CSS::MediaFeatureValue(CSS::Ratio(inner_width(), inner_height()));
    case CSS::MediaFeatureID::Color:
        return CSS::MediaFeatureValue(8);
    case CSS::MediaFeatureID::ColorGamut:
        return CSS::MediaFeatureValue(CSS::ValueID::Srgb);
    case CSS::MediaFeatureID::ColorIndex:
        return CSS::MediaFeatureValue(0);
    // FIXME: device-aspect-ratio
    // FIXME: device-height
    // FIXME: device-width
    case CSS::MediaFeatureID::DisplayMode:
        // FIXME: Detect if window is fullscreen
        return CSS::MediaFeatureValue(CSS::ValueID::Browser);
    case CSS::MediaFeatureID::DynamicRange:
        return CSS::MediaFeatureValue(CSS::ValueID::Standard);
    case CSS::MediaFeatureID::EnvironmentBlending:
        return CSS::MediaFeatureValue(CSS::ValueID::Opaque);
    case CSS::MediaFeatureID::ForcedColors:
        return CSS::MediaFeatureValue(CSS::ValueID::None);
    case CSS::MediaFeatureID::Grid:
        return CSS::MediaFeatureValue(0);
    case CSS::MediaFeatureID::Height:
        return CSS::MediaFeatureValue(CSS::Length::make_px(inner_height()));
    case CSS::MediaFeatureID::HorizontalViewportSegments:
        return CSS::MediaFeatureValue(1);
    case CSS::MediaFeatureID::Hover:
        return CSS::MediaFeatureValue(CSS::ValueID::Hover);
    case CSS::MediaFeatureID::InvertedColors:
        return CSS::MediaFeatureValue(CSS::ValueID::None);
    case CSS::MediaFeatureID::Monochrome:
        return CSS::MediaFeatureValue(0);
    case CSS::MediaFeatureID::NavControls:
        return CSS::MediaFeatureValue(CSS::ValueID::Back);
    case CSS::MediaFeatureID::Orientation:
        return CSS::MediaFeatureValue(inner_height() >= inner_width() ? CSS::ValueID::Portrait : CSS::ValueID::Landscape);
    case CSS::MediaFeatureID::OverflowBlock:
        return CSS::MediaFeatureValue(CSS::ValueID::Scroll);
    case CSS::MediaFeatureID::OverflowInline:
        return CSS::MediaFeatureValue(CSS::ValueID::Scroll);
    case CSS::MediaFeatureID::Pointer:
        return CSS::MediaFeatureValue(CSS::ValueID::Fine);
    case CSS::MediaFeatureID::PrefersColorScheme: {
        if (auto* page = this->page()) {
            switch (page->preferred_color_scheme()) {
            case CSS::PreferredColorScheme::Light:
                return CSS::MediaFeatureValue(CSS::ValueID::Light);
            case CSS::PreferredColorScheme::Dark:
                return CSS::MediaFeatureValue(CSS::ValueID::Dark);
            case CSS::PreferredColorScheme::Auto:
            default:
                return CSS::MediaFeatureValue(page->palette().is_dark() ? CSS::ValueID::Dark : CSS::ValueID::Light);
            }
        }
        return CSS::MediaFeatureValue(CSS::ValueID::Light);
    }
    case CSS::MediaFeatureID::PrefersContrast:
        // FIXME: Make this a preference
        return CSS::MediaFeatureValue(CSS::ValueID::NoPreference);
    case CSS::MediaFeatureID::PrefersReducedData:
        // FIXME: Make this a preference
        return CSS::MediaFeatureValue(CSS::ValueID::NoPreference);
    case CSS::MediaFeatureID::PrefersReducedMotion:
        // FIXME: Make this a preference
        return CSS::MediaFeatureValue(CSS::ValueID::NoPreference);
    case CSS::MediaFeatureID::PrefersReducedTransparency:
        // FIXME: Make this a preference
        return CSS::MediaFeatureValue(CSS::ValueID::NoPreference);
    // FIXME: resolution
    case CSS::MediaFeatureID::Scan:
        return CSS::MediaFeatureValue(CSS::ValueID::Progressive);
    case CSS::MediaFeatureID::Scripting:
        if (associated_document().is_scripting_enabled())
            return CSS::MediaFeatureValue(CSS::ValueID::Enabled);
        return CSS::MediaFeatureValue(CSS::ValueID::None);
    case CSS::MediaFeatureID::Update:
        return CSS::MediaFeatureValue(CSS::ValueID::Fast);
    case CSS::MediaFeatureID::VerticalViewportSegments:
        return CSS::MediaFeatureValue(1);
    case CSS::MediaFeatureID::VideoColorGamut:
        return CSS::MediaFeatureValue(CSS::ValueID::Srgb);
    case CSS::MediaFeatureID::VideoDynamicRange:
        return CSS::MediaFeatureValue(CSS::ValueID::Standard);
    case CSS::MediaFeatureID::Width:
        return CSS::MediaFeatureValue(CSS::Length::make_px(inner_width()));

    default:
        break;
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
void Window::queue_microtask(Bindings::CallbackType& callback)
{
    // The queueMicrotask(callback) method must queue a microtask to invoke callback,
    HTML::queue_a_microtask(&associated_document(), [callback = JS::make_handle(callback)]() mutable {
        auto result = Bindings::IDL::invoke_callback(*callback, {});
        // and if callback throws an exception, report the exception.
        if (result.is_error())
            HTML::report_exception(result);
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

// https://html.spec.whatwg.org/multipage/webstorage.html#dom-localstorage
RefPtr<HTML::Storage> Window::local_storage()
{
    // FIXME: Implement according to spec.

    static HashMap<Origin, NonnullRefPtr<HTML::Storage>> local_storage_per_origin;
    return local_storage_per_origin.ensure(associated_document().origin(), [] {
        return HTML::Storage::create();
    });
}

// https://html.spec.whatwg.org/multipage/webstorage.html#dom-sessionstorage
RefPtr<HTML::Storage> Window::session_storage()
{
    // FIXME: Implement according to spec.

    static HashMap<Origin, NonnullRefPtr<HTML::Storage>> session_storage_per_origin;
    return session_storage_per_origin.ensure(associated_document().origin(), [] {
        return HTML::Storage::create();
    });
}

// https://html.spec.whatwg.org/multipage/browsers.html#dom-parent
Window* Window::parent()
{
    // 1. Let current be this Window object's browsing context.
    auto* current = associated_document().browsing_context();

    // 2. If current is null, then return null.
    if (!current)
        return nullptr;

    // 3. If current is a child browsing context of another browsing context parent,
    //    then return parent's WindowProxy object.
    if (current->parent()) {
        VERIFY(current->parent()->active_document());
        return &current->parent()->active_document()->window();
    }

    // 4. Assert: current is a top-level browsing context.
    VERIFY(current->is_top_level());

    // FIXME: 5. Return current's WindowProxy object.
    VERIFY(current->active_document());
    return &current->active_document()->window();
}

// https://html.spec.whatwg.org/multipage/web-messaging.html#window-post-message-steps
DOM::ExceptionOr<void> Window::post_message(JS::Value message, String const&)
{
    // FIXME: This is an ad-hoc hack implementation instead, since we don't currently
    //        have serialization and deserialization of messages.
    HTML::queue_global_task(HTML::Task::Source::PostedMessage, *wrapper(), [strong_this = NonnullRefPtr(*this), message]() mutable {
        HTML::MessageEventInit event_init {};
        event_init.data = message;
        event_init.origin = "<origin>";
        strong_this->dispatch_event(HTML::MessageEvent::create(HTML::EventNames::message, event_init));
    });
    return {};
}

// https://html.spec.whatwg.org/multipage/window-object.html#dom-name
String Window::name() const
{
    // 1. If this's browsing context is null, then return the empty string.
    if (!browsing_context())
        return String::empty();
    // 2. Return this's browsing context's name.
    return browsing_context()->name();
}

// https://html.spec.whatwg.org/multipage/window-object.html#dom-name
void Window::set_name(String const& name)
{
    // 1. If this's browsing context is null, then return.
    if (!browsing_context())
        return;
    // 2. Set this's browsing context's name to the given value.
    browsing_context()->set_name(name);
}

// https://w3c.github.io/requestidlecallback/#start-an-idle-period-algorithm
void Window::start_an_idle_period()
{
    // 1. Optionally, if the user agent determines the idle period should be delayed, return from this algorithm.
    if (!wrapper())
        return;
    // 2. Let pending_list be window's list of idle request callbacks.
    auto& pending_list = m_idle_request_callbacks;
    // 3. Let run_list be window's list of runnable idle callbacks.
    auto& run_list = m_runnable_idle_callbacks;
    run_list.extend(pending_list);
    // 4. Clear pending_list.
    pending_list.clear();

    // FIXME: This might not agree with the spec, but currently we use 100% CPU if we keep queueing tasks
    if (run_list.is_empty())
        return;

    // 5. Queue a task on the queue associated with the idle-task task source,
    //    which performs the steps defined in the invoke idle callbacks algorithm with window and getDeadline as parameters.
    HTML::queue_global_task(HTML::Task::Source::IdleTask, *wrapper(), [window = NonnullRefPtr(*this)]() mutable {
        window->invoke_idle_callbacks();
    });
}

// https://w3c.github.io/requestidlecallback/#invoke-idle-callbacks-algorithm
void Window::invoke_idle_callbacks()
{
    auto& event_loop = main_thread_event_loop();
    // 1. If the user-agent believes it should end the idle period early due to newly scheduled high-priority work, return from the algorithm.
    // 2. Let now be the current time.
    auto now = event_loop.unsafe_shared_current_time();
    // 3. If now is less than the result of calling getDeadline and the window's list of runnable idle callbacks is not empty:
    if (now < event_loop.compute_deadline() && !m_runnable_idle_callbacks.is_empty()) {
        // 1. Pop the top callback from window's list of runnable idle callbacks.
        auto callback = m_runnable_idle_callbacks.take_first();
        // 2. Let deadlineArg be a new IdleDeadline whose [get deadline time algorithm] is getDeadline.
        auto deadline_arg = RequestIdleCallback::IdleDeadline::create();
        // 3. Call callback with deadlineArg as its argument. If an uncaught runtime script error occurs, then report the exception.
        auto result = callback->invoke(deadline_arg);
        if (result.is_error())
            HTML::report_exception(result);
        // 4. If window's list of runnable idle callbacks is not empty, queue a task which performs the steps
        //    in the invoke idle callbacks algorithm with getDeadline and window as a parameters and return from this algorithm
        HTML::queue_global_task(HTML::Task::Source::IdleTask, *wrapper(), [window = NonnullRefPtr(*this)]() mutable {
            window->invoke_idle_callbacks();
        });
    }
}

// https://w3c.github.io/requestidlecallback/#the-requestidlecallback-method
u32 Window::request_idle_callback(Bindings::CallbackType& callback)
{
    // 1. Let window be this Window object.
    auto& window = *this;
    // 2. Increment the window's idle callback identifier by one.
    window.m_idle_callback_identifier++;
    // 3. Let handle be the current value of window's idle callback identifier.
    auto handle = window.m_idle_callback_identifier;
    // 4. Push callback to the end of window's list of idle request callbacks, associated with handle.
    auto handler = [callback = JS::make_handle(callback)](NonnullRefPtr<RequestIdleCallback::IdleDeadline> deadline) -> JS::Completion {
        auto& realm = callback->callback.shape().realm();
        auto* wrapped_deadline = Bindings::wrap(realm, *deadline);
        return Bindings::IDL::invoke_callback(const_cast<Bindings::CallbackType&>(*callback), {}, JS::Value(wrapped_deadline));
    };
    window.m_idle_request_callbacks.append(adopt_ref(*new IdleCallback(move(handler), handle)));
    // 5. Return handle and then continue running this algorithm asynchronously.
    return handle;
    // FIXME: 6. If the timeout property is present in options and has a positive value:
    // FIXME:    1. Wait for timeout milliseconds.
    // FIXME:    2. Wait until all invocations of this algorithm, whose timeout added to their posted time occurred before this one's, have completed.
    // FIXME:    3. Optionally, wait a further user-agent defined length of time.
    // FIXME:    4. Queue a task on the queue associated with the idle-task task source, which performs the invoke idle callback timeout algorithm, passing handle and window as arguments.
}

// https://w3c.github.io/requestidlecallback/#the-cancelidlecallback-method
void Window::cancel_idle_callback(u32 handle)
{
    // 1. Let window be this Window object.
    auto& window = *this;
    // 2. Find the entry in either the window's list of idle request callbacks or list of runnable idle callbacks
    //    that is associated with the value handle.
    // 3. If there is such an entry, remove it from both window's list of idle request callbacks and the list of runnable idle callbacks.
    window.m_idle_request_callbacks.remove_first_matching([handle](auto& callback) {
        return callback->handle() == handle;
    });
    window.m_runnable_idle_callbacks.remove_first_matching([handle](auto& callback) {
        return callback->handle() == handle;
    });
}

void Window::set_associated_document(DOM::Document& document)
{
    m_associated_document = document;
}

}
