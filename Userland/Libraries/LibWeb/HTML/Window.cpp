/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/String.h>
#include <AK/Utf8View.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/Shape.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/Bindings/CSSNamespace.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/LocationObject.h>
#include <LibWeb/Bindings/Replaceable.h>
#include <LibWeb/Bindings/WindowExposedInterfaces.h>
#include <LibWeb/Bindings/WindowPrototype.h>
#include <LibWeb/CSS/MediaQueryList.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/ResolvedCSSStyleDeclaration.h>
#include <LibWeb/CSS/Screen.h>
#include <LibWeb/Crypto/Crypto.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/Navigator.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/PageTransitionEvent.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Scripting/ExceptionReporter.h>
#include <LibWeb/HTML/Storage.h>
#include <LibWeb/HTML/Timer.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/HTML/WindowProxy.h>
#include <LibWeb/HighResolutionTime/Performance.h>
#include <LibWeb/HighResolutionTime/TimeOrigin.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/RequestIdleCallback/IdleDeadline.h>
#include <LibWeb/Selection/Selection.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>
#include <LibWeb/WebIDL/AbstractOperations.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/#run-the-animation-frame-callbacks
void run_animation_frame_callbacks(DOM::Document& document, double)
{
    // FIXME: Bring this closer to the spec.
    document.window().animation_frame_callback_driver().run();
}

class IdleCallback : public RefCounted<IdleCallback> {
public:
    explicit IdleCallback(Function<JS::Completion(JS::NonnullGCPtr<RequestIdleCallback::IdleDeadline>)> handler, u32 handle)
        : m_handler(move(handler))
        , m_handle(handle)
    {
    }
    ~IdleCallback() = default;

    JS::Completion invoke(JS::NonnullGCPtr<RequestIdleCallback::IdleDeadline> deadline) { return m_handler(deadline); }
    u32 handle() const { return m_handle; }

private:
    Function<JS::Completion(JS::NonnullGCPtr<RequestIdleCallback::IdleDeadline>)> m_handler;
    u32 m_handle { 0 };
};

JS::NonnullGCPtr<Window> Window::create(JS::Realm& realm)
{
    return *realm.heap().allocate<Window>(realm, realm);
}

Window::Window(JS::Realm& realm)
    : DOM::EventTarget(realm)
{
}

void Window::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_associated_document.ptr());
    visitor.visit(m_current_event.ptr());
    visitor.visit(m_performance.ptr());
    visitor.visit(m_screen.ptr());
    visitor.visit(m_location_object);
    visitor.visit(m_crypto);
    visitor.visit(m_navigator);
    for (auto& it : m_timers)
        visitor.visit(it.value.ptr());
}

Window::~Window() = default;

HighResolutionTime::Performance& Window::performance()
{
    if (!m_performance)
        m_performance = heap().allocate<HighResolutionTime::Performance>(realm(), *this);
    return *m_performance;
}

CSS::Screen& Window::screen()
{
    if (!m_screen)
        m_screen = heap().allocate<CSS::Screen>(realm(), *this);
    return *m_screen;
}

void Window::alert_impl(String const& message)
{
    if (auto* page = this->page())
        page->client().page_did_request_alert(message);
}

bool Window::confirm_impl(String const& message)
{
    if (auto* page = this->page())
        return page->client().page_did_request_confirm(message);
    return false;
}

String Window::prompt_impl(String const& message, String const& default_)
{
    if (auto* page = this->page())
        return page->client().page_did_request_prompt(message, default_);
    return {};
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-settimeout
i32 Window::set_timeout_impl(TimerHandler handler, i32 timeout, JS::MarkedVector<JS::Value> arguments)
{
    return run_timer_initialization_steps(move(handler), timeout, move(arguments), Repeat::No);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-setinterval
i32 Window::set_interval_impl(TimerHandler handler, i32 timeout, JS::MarkedVector<JS::Value> arguments)
{
    return run_timer_initialization_steps(move(handler), timeout, move(arguments), Repeat::Yes);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-cleartimeout
void Window::clear_timeout_impl(i32 id)
{
    m_timers.remove(id);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-clearinterval
void Window::clear_interval_impl(i32 id)
{
    m_timers.remove(id);
}

void Window::deallocate_timer_id(Badge<Timer>, i32 id)
{
    m_timer_id_allocator.deallocate(id);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#timer-initialisation-steps
i32 Window::run_timer_initialization_steps(TimerHandler handler, i32 timeout, JS::MarkedVector<JS::Value> arguments, Repeat repeat, Optional<i32> previous_id)
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
    JS::SafeFunction<void()> task = [this, handler = move(handler), timeout, arguments = move(arguments), repeat, id]() mutable {
        // 1. If id does not exist in global's map of active timers, then abort these steps.
        if (!m_timers.contains(id))
            return;

        handler.visit(
            // 2. If handler is a Function, then invoke handler given arguments with the callback this value set to thisArg. If this throws an exception, catch it, and report the exception.
            [&](JS::Handle<WebIDL::CallbackType> callback) {
                if (auto result = WebIDL::invoke_callback(*callback, this, arguments); result.is_error())
                    HTML::report_exception(result, realm());
            },
            // 3. Otherwise:
            [&](String const& source) {
                // 1. Assert: handler is a string.
                // FIXME: 2. Perform HostEnsureCanCompileStrings(callerRealm, calleeRealm). If this throws an exception, catch it, report the exception, and abort these steps.

                // 3. Let settings object be global's relevant settings object.
                auto& settings_object = associated_document().relevant_settings_object();

                // 4. Let base URL be initiating script's base URL.
                auto url = associated_document().url();

                // 5. Assert: base URL is not null, as initiating script is a classic script or a JavaScript module script.

                // 6. Let fetch options be a script fetch options whose cryptographic nonce is initiating script's fetch options's cryptographic nonce, integrity metadata is the empty string, parser metadata is "not-parser-inserted", credentials mode is initiating script's fetch options's credentials mode, and referrer policy is initiating script's fetch options's referrer policy.
                // 7. Let script be the result of creating a classic script given handler, settings object, base URL, and fetch options.
                auto script = HTML::ClassicScript::create(url.basename(), source, settings_object, url);

                // 8. Run the classic script script.
                (void)script->run();
            });

        // 4. If id does not exist in global's map of active timers, then abort these steps.
        if (!m_timers.contains(id))
            return;

        switch (repeat) {
        // 5. If repeat is true, then perform the timer initialization steps again, given global, handler, timeout, arguments, true, and id.
        case Repeat::Yes:
            run_timer_initialization_steps(handler, timeout, move(arguments), repeat, id);
            break;

        // 6. Otherwise, remove global's map of active timers[id].
        case Repeat::No:
            m_timers.remove(id);
            break;
        }
    };

    // FIXME: 10. Increment nesting level by one.
    // FIXME: 11. Set task's timer nesting level to nesting level.

    // 12. Let completionStep be an algorithm step which queues a global task on the timer task source given global to run task.
    JS::SafeFunction<void()> completion_step = [this, task = move(task)]() mutable {
        HTML::queue_global_task(HTML::Task::Source::TimerTask, *this, move(task));
    };

    // 13. Run steps after a timeout given global, "setTimeout/setInterval", timeout, completionStep, and id.
    auto timer = Timer::create(*this, timeout, move(completion_step), id);
    m_timers.set(id, timer);
    timer->start();

    // 14. Return id.
    return id;
}

// https://html.spec.whatwg.org/multipage/imagebitmap-and-animations.html#run-the-animation-frame-callbacks
i32 Window::request_animation_frame_impl(WebIDL::CallbackType& js_callback)
{
    return m_animation_frame_callback_driver.add([this, js_callback = JS::make_handle(js_callback)](auto) mutable {
        // 3. Invoke callback, passing now as the only argument,
        auto result = WebIDL::invoke_callback(*js_callback, {}, JS::Value(performance().now()));

        // and if an exception is thrown, report the exception.
        if (result.is_error())
            HTML::report_exception(result, realm());
    });
}

void Window::cancel_animation_frame_impl(i32 id)
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

bool Window::dispatch_event(DOM::Event& event)
{
    return DOM::EventDispatcher::dispatch(*this, event, true);
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

CSS::CSSStyleDeclaration* Window::get_computed_style_impl(DOM::Element& element) const
{
    return CSS::ResolvedCSSStyleDeclaration::create(element);
}

JS::NonnullGCPtr<CSS::MediaQueryList> Window::match_media_impl(String media)
{
    auto media_query_list = CSS::MediaQueryList::create(associated_document(), parse_media_query_list(CSS::Parser::ParsingContext(associated_document()), media));
    associated_document().add_media_query_list(*media_query_list);
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
    auto event = HTML::PageTransitionEvent::create(associated_document().realm(), event_name, event_init);

    // ...the cancelable attribute initialized to true,
    event->set_cancelable(true);

    // the bubbles attribute initialized to true,
    event->set_bubbles(true);

    // and legacy target override flag set.
    dispatch_event(*event);
}

// https://html.spec.whatwg.org/#dom-queuemicrotask
void Window::queue_microtask_impl(WebIDL::CallbackType& callback)
{
    // The queueMicrotask(callback) method must queue a microtask to invoke callback,
    HTML::queue_a_microtask(&associated_document(), [weak_window = make_weak_ptr<Window>(), &callback]() mutable {
        JS::GCPtr<Window> window = weak_window.ptr();
        if (!window)
            return;

        auto result = WebIDL::invoke_callback(callback, {});
        // and if callback throws an exception, report the exception.
        if (result.is_error())
            HTML::report_exception(result, window->realm());
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

// https://html.spec.whatwg.org/multipage/webstorage.html#dom-localstorage
JS::NonnullGCPtr<HTML::Storage> Window::local_storage()
{
    // FIXME: Implement according to spec.

    static HashMap<Origin, JS::Handle<HTML::Storage>> local_storage_per_origin;
    auto storage = local_storage_per_origin.ensure(associated_document().origin(), [this]() -> JS::Handle<HTML::Storage> {
        return *HTML::Storage::create(realm());
    });
    return *storage;
}

// https://html.spec.whatwg.org/multipage/webstorage.html#dom-sessionstorage
JS::NonnullGCPtr<HTML::Storage> Window::session_storage()
{
    // FIXME: Implement according to spec.

    static HashMap<Origin, JS::Handle<HTML::Storage>> session_storage_per_origin;
    auto storage = session_storage_per_origin.ensure(associated_document().origin(), [this]() -> JS::Handle<HTML::Storage> {
        return *HTML::Storage::create(realm());
    });
    return *storage;
}

// https://html.spec.whatwg.org/multipage/browsers.html#dom-parent
WindowProxy* Window::parent()
{
    // 1. Let current be this Window object's browsing context.
    auto* current = browsing_context();

    // 2. If current is null, then return null.
    if (!current)
        return nullptr;

    // 3. If current is a child browsing context of another browsing context parent,
    //    then return parent's WindowProxy object.
    if (current->parent()) {
        return current->parent()->window_proxy();
    }

    // 4. Assert: current is a top-level browsing context.
    VERIFY(current->is_top_level());

    // 5. Return current's WindowProxy object.
    return current->window_proxy();
}

// https://html.spec.whatwg.org/multipage/web-messaging.html#window-post-message-steps
WebIDL::ExceptionOr<void> Window::post_message_impl(JS::Value message, String const&)
{
    // FIXME: This is an ad-hoc hack implementation instead, since we don't currently
    //        have serialization and deserialization of messages.
    HTML::queue_global_task(HTML::Task::Source::PostedMessage, *this, [this, message]() mutable {
        HTML::MessageEventInit event_init {};
        event_init.data = message;
        event_init.origin = "<origin>";
        dispatch_event(*HTML::MessageEvent::create(realm(), HTML::EventNames::message, event_init));
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

// https://html.spec.whatwg.org/multipage/interaction.html#transient-activation
bool Window::has_transient_activation() const
{
    // FIXME: Implement this.
    return false;
}

// https://w3c.github.io/requestidlecallback/#start-an-idle-period-algorithm
void Window::start_an_idle_period()
{
    // 1. Optionally, if the user agent determines the idle period should be delayed, return from this algorithm.

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
    HTML::queue_global_task(HTML::Task::Source::IdleTask, *this, [this]() mutable {
        invoke_idle_callbacks();
    });
}

// https://w3c.github.io/requestidlecallback/#invoke-idle-callbacks-algorithm
void Window::invoke_idle_callbacks()
{
    auto& event_loop = main_thread_event_loop();
    // 1. If the user-agent believes it should end the idle period early due to newly scheduled high-priority work, return from the algorithm.
    // 2. Let now be the current time.
    auto now = HighResolutionTime::unsafe_shared_current_time();
    // 3. If now is less than the result of calling getDeadline and the window's list of runnable idle callbacks is not empty:
    if (now < event_loop.compute_deadline() && !m_runnable_idle_callbacks.is_empty()) {
        // 1. Pop the top callback from window's list of runnable idle callbacks.
        auto callback = m_runnable_idle_callbacks.take_first();
        // 2. Let deadlineArg be a new IdleDeadline whose [get deadline time algorithm] is getDeadline.
        auto deadline_arg = RequestIdleCallback::IdleDeadline::create(realm());
        // 3. Call callback with deadlineArg as its argument. If an uncaught runtime script error occurs, then report the exception.
        auto result = callback->invoke(deadline_arg);
        if (result.is_error())
            HTML::report_exception(result, realm());
        // 4. If window's list of runnable idle callbacks is not empty, queue a task which performs the steps
        //    in the invoke idle callbacks algorithm with getDeadline and window as a parameters and return from this algorithm
        HTML::queue_global_task(HTML::Task::Source::IdleTask, *this, [this]() mutable {
            invoke_idle_callbacks();
        });
    }
}

// https://w3c.github.io/requestidlecallback/#the-requestidlecallback-method
u32 Window::request_idle_callback_impl(WebIDL::CallbackType& callback)
{
    // 1. Let window be this Window object.
    auto& window = *this;
    // 2. Increment the window's idle callback identifier by one.
    window.m_idle_callback_identifier++;
    // 3. Let handle be the current value of window's idle callback identifier.
    auto handle = window.m_idle_callback_identifier;
    // 4. Push callback to the end of window's list of idle request callbacks, associated with handle.
    auto handler = [callback = JS::make_handle(callback)](JS::NonnullGCPtr<RequestIdleCallback::IdleDeadline> deadline) -> JS::Completion {
        return WebIDL::invoke_callback(const_cast<WebIDL::CallbackType&>(*callback), {}, deadline.ptr());
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
void Window::cancel_idle_callback_impl(u32 handle)
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
    m_associated_document = &document;
}

void Window::set_current_event(DOM::Event* event)
{
    m_current_event = event;
}

HTML::BrowsingContext const* Window::browsing_context() const
{
    return m_associated_document->browsing_context();
}

HTML::BrowsingContext* Window::browsing_context()
{
    return m_associated_document->browsing_context();
}

void Window::initialize(JS::Realm& realm)
{
    Base::initialize(realm);

    // FIXME: This is a hack..
    realm.set_global_object(this, this);
}

void Window::initialize_web_interfaces(Badge<WindowEnvironmentSettingsObject>)
{
    auto& realm = this->realm();
    add_window_exposed_interfaces(*this, realm);

    Object::set_prototype(&Bindings::ensure_web_prototype<Bindings::WindowPrototype>(realm, "Window"));

    m_crypto = Crypto::Crypto::create(realm);

    // FIXME: These should be native accessors, not properties
    define_direct_property("window", this, JS::Attribute::Enumerable);
    define_direct_property("frames", this, JS::Attribute::Enumerable);
    define_direct_property("self", this, JS::Attribute::Enumerable);
    define_native_accessor(realm, "top", top_getter, nullptr, JS::Attribute::Enumerable);
    define_native_accessor(realm, "parent", parent_getter, {}, JS::Attribute::Enumerable);
    define_native_accessor(realm, "document", document_getter, {}, JS::Attribute::Enumerable);
    define_native_accessor(realm, "frameElement", frame_element_getter, {}, JS::Attribute::Enumerable);
    define_native_accessor(realm, "name", name_getter, name_setter, JS::Attribute::Enumerable);
    define_native_accessor(realm, "history", history_getter, {}, JS::Attribute::Enumerable);
    define_native_accessor(realm, "performance", performance_getter, performance_setter, JS::Attribute::Enumerable | JS::Attribute::Configurable);
    define_native_accessor(realm, "crypto", crypto_getter, {}, JS::Attribute::Enumerable);
    define_native_accessor(realm, "screen", screen_getter, {}, JS::Attribute::Enumerable);
    define_native_accessor(realm, "innerWidth", inner_width_getter, {}, JS::Attribute::Enumerable);
    define_native_accessor(realm, "innerHeight", inner_height_getter, {}, JS::Attribute::Enumerable);
    define_native_accessor(realm, "devicePixelRatio", device_pixel_ratio_getter, {}, JS::Attribute::Enumerable | JS::Attribute::Configurable);
    u8 attr = JS::Attribute::Writable | JS::Attribute::Enumerable | JS::Attribute::Configurable;
    define_native_function(realm, "alert", alert, 0, attr);
    define_native_function(realm, "confirm", confirm, 0, attr);
    define_native_function(realm, "prompt", prompt, 0, attr);
    define_native_function(realm, "setInterval", set_interval, 1, attr);
    define_native_function(realm, "setTimeout", set_timeout, 1, attr);
    define_native_function(realm, "clearInterval", clear_interval, 1, attr);
    define_native_function(realm, "clearTimeout", clear_timeout, 1, attr);
    define_native_function(realm, "requestAnimationFrame", request_animation_frame, 1, attr);
    define_native_function(realm, "cancelAnimationFrame", cancel_animation_frame, 1, attr);
    define_native_function(realm, "atob", atob, 1, attr);
    define_native_function(realm, "btoa", btoa, 1, attr);

    define_native_function(realm, "queueMicrotask", queue_microtask, 1, attr);

    define_native_function(realm, "requestIdleCallback", request_idle_callback, 1, attr);
    define_native_function(realm, "cancelIdleCallback", cancel_idle_callback, 1, attr);

    define_native_function(realm, "getComputedStyle", get_computed_style, 1, attr);
    define_native_function(realm, "matchMedia", match_media, 1, attr);
    define_native_function(realm, "getSelection", get_selection, 0, attr);

    define_native_function(realm, "postMessage", post_message, 1, attr);

    // FIXME: These properties should be [Replaceable] according to the spec, but [Writable+Configurable] is the closest we have.
    define_native_accessor(realm, "scrollX", scroll_x_getter, {}, attr);
    define_native_accessor(realm, "pageXOffset", scroll_x_getter, {}, attr);
    define_native_accessor(realm, "scrollY", scroll_y_getter, {}, attr);
    define_native_accessor(realm, "pageYOffset", scroll_y_getter, {}, attr);
    define_native_accessor(realm, "length", length_getter, {}, attr);

    define_native_function(realm, "scroll", scroll, 2, attr);
    define_native_function(realm, "scrollTo", scroll, 2, attr);
    define_native_function(realm, "scrollBy", scroll_by, 2, attr);

    define_native_accessor(realm, "screenX", screen_x_getter, {}, attr);
    define_native_accessor(realm, "screenY", screen_y_getter, {}, attr);
    define_native_accessor(realm, "screenLeft", screen_left_getter, {}, attr);
    define_native_accessor(realm, "screenTop", screen_top_getter, {}, attr);

    define_direct_property("CSS", heap().allocate<Bindings::CSSNamespace>(realm, realm), 0);

    define_native_accessor(realm, "localStorage", local_storage_getter, {}, attr);
    define_native_accessor(realm, "sessionStorage", session_storage_getter, {}, attr);
    define_native_accessor(realm, "origin", origin_getter, {}, attr);

    // Legacy
    define_native_accessor(realm, "event", event_getter, event_setter, JS::Attribute::Enumerable);

    m_location_object = heap().allocate<Bindings::LocationObject>(realm, realm);

    m_navigator = heap().allocate<HTML::Navigator>(realm, realm);
    define_direct_property("navigator", m_navigator, JS::Attribute::Enumerable | JS::Attribute::Configurable);
    define_direct_property("clientInformation", m_navigator, JS::Attribute::Enumerable | JS::Attribute::Configurable);

    // NOTE: location is marked as [LegacyUnforgeable], meaning it isn't configurable.
    define_native_accessor(realm, "location", location_getter, location_setter, JS::Attribute::Enumerable);

    // WebAssembly "namespace"
    define_direct_property("WebAssembly", heap().allocate<Bindings::WebAssemblyObject>(realm, realm), JS::Attribute::Enumerable | JS::Attribute::Configurable);

    // HTML::GlobalEventHandlers and HTML::WindowEventHandlers
#define __ENUMERATE(attribute, event_name) \
    define_native_accessor(realm, #attribute, attribute##_getter, attribute##_setter, attr);
    ENUMERATE_GLOBAL_EVENT_HANDLERS(__ENUMERATE);
    ENUMERATE_WINDOW_EVENT_HANDLERS(__ENUMERATE);
#undef __ENUMERATE
}

HTML::Origin Window::origin() const
{
    return associated_document().origin();
}

// https://webidl.spec.whatwg.org/#platform-object-setprototypeof
JS::ThrowCompletionOr<bool> Window::internal_set_prototype_of(JS::Object* prototype)
{
    // 1. Return ? SetImmutablePrototype(O, V).
    return set_immutable_prototype(prototype);
}

static JS::ThrowCompletionOr<HTML::Window*> impl_from(JS::VM& vm)
{
    // Since this is a non built-in function we must treat it as non-strict mode
    // this means that a nullish this_value should be converted to the
    // global_object. Generally this does not matter as we try to convert the
    // this_value to a specific object type in the bindings. But since window is
    // the global object we make an exception here.
    // This allows calls like `setTimeout(f, 10)` to work.
    auto this_value = vm.this_value();
    if (this_value.is_nullish())
        this_value = &vm.current_realm()->global_object();

    auto* this_object = MUST(this_value.to_object(vm));

    if (!is<Window>(*this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "Window");
    return static_cast<Window*>(this_object);
}

JS_DEFINE_NATIVE_FUNCTION(Window::alert)
{
    // https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#simple-dialogs
    // Note: This method is defined using two overloads, instead of using an optional argument,
    //       for historical reasons. The practical impact of this is that alert(undefined) is
    //       treated as alert("undefined"), but alert() is treated as alert("").
    auto* impl = TRY(impl_from(vm));
    String message = "";
    if (vm.argument_count())
        message = TRY(vm.argument(0).to_string(vm));
    impl->alert_impl(message);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(Window::confirm)
{
    auto* impl = TRY(impl_from(vm));
    String message = "";
    if (!vm.argument(0).is_undefined())
        message = TRY(vm.argument(0).to_string(vm));
    return JS::Value(impl->confirm_impl(message));
}

JS_DEFINE_NATIVE_FUNCTION(Window::prompt)
{
    auto* impl = TRY(impl_from(vm));
    String message = "";
    String default_ = "";
    if (!vm.argument(0).is_undefined())
        message = TRY(vm.argument(0).to_string(vm));
    if (!vm.argument(1).is_undefined())
        default_ = TRY(vm.argument(1).to_string(vm));
    auto response = impl->prompt_impl(message, default_);
    if (response.is_null())
        return JS::js_null();
    return JS::js_string(vm, response);
}

static JS::ThrowCompletionOr<TimerHandler> make_timer_handler(JS::VM& vm, JS::Value handler)
{
    if (handler.is_function())
        return JS::make_handle(vm.heap().allocate_without_realm<WebIDL::CallbackType>(handler.as_function(), HTML::incumbent_settings_object()));
    return TRY(handler.to_string(vm));
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-settimeout
JS_DEFINE_NATIVE_FUNCTION(Window::set_timeout)
{
    auto* impl = TRY(impl_from(vm));

    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::BadArgCountAtLeastOne, "setTimeout");

    auto handler = TRY(make_timer_handler(vm, vm.argument(0)));

    i32 timeout = 0;
    if (vm.argument_count() >= 2)
        timeout = TRY(vm.argument(1).to_i32(vm));

    JS::MarkedVector<JS::Value> arguments { vm.heap() };
    for (size_t i = 2; i < vm.argument_count(); ++i)
        arguments.append(vm.argument(i));

    auto id = impl->set_timeout_impl(move(handler), timeout, move(arguments));
    return JS::Value(id);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-setinterval
JS_DEFINE_NATIVE_FUNCTION(Window::set_interval)
{
    auto* impl = TRY(impl_from(vm));

    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::BadArgCountAtLeastOne, "setInterval");

    auto handler = TRY(make_timer_handler(vm, vm.argument(0)));

    i32 timeout = 0;
    if (vm.argument_count() >= 2)
        timeout = TRY(vm.argument(1).to_i32(vm));

    JS::MarkedVector<JS::Value> arguments { vm.heap() };
    for (size_t i = 2; i < vm.argument_count(); ++i)
        arguments.append(vm.argument(i));

    auto id = impl->set_interval_impl(move(handler), timeout, move(arguments));
    return JS::Value(id);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-cleartimeout
JS_DEFINE_NATIVE_FUNCTION(Window::clear_timeout)
{
    auto* impl = TRY(impl_from(vm));

    i32 id = 0;
    if (vm.argument_count())
        id = TRY(vm.argument(0).to_i32(vm));

    impl->clear_timeout_impl(id);
    return JS::js_undefined();
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-clearinterval
JS_DEFINE_NATIVE_FUNCTION(Window::clear_interval)
{
    auto* impl = TRY(impl_from(vm));

    i32 id = 0;
    if (vm.argument_count())
        id = TRY(vm.argument(0).to_i32(vm));

    impl->clear_interval_impl(id);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(Window::request_animation_frame)
{
    auto* impl = TRY(impl_from(vm));
    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::BadArgCountOne, "requestAnimationFrame");
    auto* callback_object = TRY(vm.argument(0).to_object(vm));
    if (!callback_object->is_function())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAFunctionNoParam);
    auto* callback = vm.heap().allocate_without_realm<WebIDL::CallbackType>(*callback_object, HTML::incumbent_settings_object());
    return JS::Value(impl->request_animation_frame_impl(*callback));
}

JS_DEFINE_NATIVE_FUNCTION(Window::cancel_animation_frame)
{
    auto* impl = TRY(impl_from(vm));
    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::BadArgCountOne, "cancelAnimationFrame");
    auto id = TRY(vm.argument(0).to_i32(vm));
    impl->cancel_animation_frame_impl(id);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(Window::queue_microtask)
{
    auto* impl = TRY(impl_from(vm));
    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::BadArgCountAtLeastOne, "queueMicrotask");
    auto* callback_object = TRY(vm.argument(0).to_object(vm));
    if (!callback_object->is_function())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAFunctionNoParam);

    auto* callback = vm.heap().allocate_without_realm<WebIDL::CallbackType>(*callback_object, HTML::incumbent_settings_object());

    impl->queue_microtask_impl(*callback);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(Window::request_idle_callback)
{
    auto* impl = TRY(impl_from(vm));
    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::BadArgCountAtLeastOne, "requestIdleCallback");
    auto* callback_object = TRY(vm.argument(0).to_object(vm));
    if (!callback_object->is_function())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAFunctionNoParam);
    // FIXME: accept options object

    auto* callback = vm.heap().allocate_without_realm<WebIDL::CallbackType>(*callback_object, HTML::incumbent_settings_object());

    return JS::Value(impl->request_idle_callback_impl(*callback));
}

JS_DEFINE_NATIVE_FUNCTION(Window::cancel_idle_callback)
{
    auto* impl = TRY(impl_from(vm));
    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::BadArgCountOne, "cancelIdleCallback");
    auto id = TRY(vm.argument(0).to_u32(vm));
    impl->cancel_idle_callback_impl(id);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(Window::atob)
{
    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::BadArgCountOne, "atob");
    auto string = TRY(vm.argument(0).to_string(vm));
    auto decoded = decode_base64(StringView(string));
    if (decoded.is_error())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::InvalidFormat, "Base64");

    // decode_base64() returns a byte string. LibJS uses UTF-8 for strings. Use Latin1Decoder to convert bytes 128-255 to UTF-8.
    auto decoder = TextCodec::decoder_for("windows-1252");
    VERIFY(decoder);
    return JS::js_string(vm, decoder->to_utf8(decoded.value()));
}

JS_DEFINE_NATIVE_FUNCTION(Window::btoa)
{
    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::BadArgCountOne, "btoa");
    auto string = TRY(vm.argument(0).to_string(vm));

    Vector<u8> byte_string;
    byte_string.ensure_capacity(string.length());
    for (u32 code_point : Utf8View(string)) {
        if (code_point > 0xff)
            return throw_completion(WebIDL::InvalidCharacterError::create(*vm.current_realm(), "Data contains characters outside the range U+0000 and U+00FF"));
        byte_string.append(code_point);
    }

    auto encoded = encode_base64(byte_string.span());
    return JS::js_string(vm, move(encoded));
}

// https://html.spec.whatwg.org/multipage/window-object.html#number-of-document-tree-child-browsing-contexts
JS::ThrowCompletionOr<size_t> Window::document_tree_child_browsing_context_count() const
{
    auto* impl = TRY(impl_from(vm()));

    // 1. If W's browsing context is null, then return 0.
    auto* this_browsing_context = impl->associated_document().browsing_context();
    if (!this_browsing_context)
        return 0;

    // 2. Return the number of document-tree child browsing contexts of W's browsing context.
    return this_browsing_context->document_tree_child_browsing_context_count();
}

// https://html.spec.whatwg.org/multipage/window-object.html#dom-length
JS_DEFINE_NATIVE_FUNCTION(Window::length_getter)
{
    auto* impl = TRY(impl_from(vm));

    // The length getter steps are to return the number of document-tree child browsing contexts of this.
    return TRY(impl->document_tree_child_browsing_context_count());
}

// https://html.spec.whatwg.org/multipage/browsers.html#dom-top
JS_DEFINE_NATIVE_FUNCTION(Window::top_getter)
{
    auto* impl = TRY(impl_from(vm));

    // 1. If this Window object's browsing context is null, then return null.
    auto* browsing_context = impl->browsing_context();
    if (!browsing_context)
        return JS::js_null();

    // 2. Return this Window object's browsing context's top-level browsing context's WindowProxy object.
    return browsing_context->top_level_browsing_context().window_proxy();
}

JS_DEFINE_NATIVE_FUNCTION(Window::parent_getter)
{
    auto* impl = TRY(impl_from(vm));
    auto* parent = impl->parent();
    if (!parent)
        return JS::js_null();
    return parent;
}

JS_DEFINE_NATIVE_FUNCTION(Window::document_getter)
{
    auto* impl = TRY(impl_from(vm));
    return &impl->associated_document();
}

// https://html.spec.whatwg.org/multipage/browsers.html#dom-frameelement
JS_DEFINE_NATIVE_FUNCTION(Window::frame_element_getter)
{
    auto* impl = TRY(impl_from(vm));

    // 1. Let current be this Window object's browsing context.
    auto* current = impl->browsing_context();

    // 2. If current is null, then return null.
    if (!current)
        return JS::js_null();

    // 3. Let container be current's container.
    auto* container = current->container();

    // 4. If container is null, then return null.
    if (!container)
        return JS::js_null();

    // 5. If container's node document's origin is not same origin-domain with the current settings object's origin, then return null.
    if (!container->document().origin().is_same_origin(current_settings_object().origin()))
        return JS::js_null();

    // 6. Return container.
    return container;
}

JS_DEFINE_NATIVE_FUNCTION(Window::performance_getter)
{
    auto* impl = TRY(impl_from(vm));
    return &impl->performance();
}

JS_DEFINE_NATIVE_FUNCTION(Window::performance_setter)
{
    // https://webidl.spec.whatwg.org/#dfn-attribute-setter
    // 4.1. If no arguments were passed, then throw a TypeError.
    if (vm.argument_count() == 0)
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::BadArgCountOne, "set performance");

    auto* impl = TRY(impl_from(vm));

    // 5. If attribute is declared with the [Replaceable] extended attribute, then:
    // 1. Perform ? CreateDataProperty(esValue, id, V).
    TRY(impl->create_data_property("performance", vm.argument(0)));

    // 2. Return undefined.
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(Window::screen_getter)
{
    auto* impl = TRY(impl_from(vm));
    return &impl->screen();
}

JS_DEFINE_NATIVE_FUNCTION(Window::event_getter)
{
    auto* impl = TRY(impl_from(vm));
    if (!impl->current_event())
        return JS::js_undefined();
    return impl->current_event();
}

JS_DEFINE_NATIVE_FUNCTION(Window::event_setter)
{
    REPLACEABLE_PROPERTY_SETTER(Window, event);
}

JS_DEFINE_NATIVE_FUNCTION(Window::location_getter)
{
    auto* impl = TRY(impl_from(vm));
    return impl->m_location_object;
}

JS_DEFINE_NATIVE_FUNCTION(Window::location_setter)
{
    auto* impl = TRY(impl_from(vm));
    TRY(impl->m_location_object->set(JS::PropertyKey("href"), vm.argument(0), JS::Object::ShouldThrowExceptions::Yes));
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(Window::crypto_getter)
{
    auto* impl = TRY(impl_from(vm));
    return &impl->crypto();
}

JS_DEFINE_NATIVE_FUNCTION(Window::inner_width_getter)
{
    auto* impl = TRY(impl_from(vm));
    return JS::Value(impl->inner_width());
}

JS_DEFINE_NATIVE_FUNCTION(Window::inner_height_getter)
{
    auto* impl = TRY(impl_from(vm));
    return JS::Value(impl->inner_height());
}

JS_DEFINE_NATIVE_FUNCTION(Window::device_pixel_ratio_getter)
{
    auto* impl = TRY(impl_from(vm));
    return JS::Value(impl->device_pixel_ratio());
}

JS_DEFINE_NATIVE_FUNCTION(Window::get_computed_style)
{
    auto* impl = TRY(impl_from(vm));
    auto* object = TRY(vm.argument(0).to_object(vm));
    if (!is<DOM::Element>(object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "DOM element");

    return impl->get_computed_style_impl(*static_cast<DOM::Element*>(object));
}

// https://w3c.github.io/selection-api/#dom-window-getselection
JS_DEFINE_NATIVE_FUNCTION(Window::get_selection)
{
    // The method must invoke and return the result of getSelection() on this's Window.document attribute.
    auto* impl = TRY(impl_from(vm));
    return impl->associated_document().get_selection();
}

JS_DEFINE_NATIVE_FUNCTION(Window::match_media)
{
    auto* impl = TRY(impl_from(vm));
    auto media = TRY(vm.argument(0).to_string(vm));
    return impl->match_media_impl(move(media));
}

// https://www.w3.org/TR/cssom-view/#dom-window-scrollx
JS_DEFINE_NATIVE_FUNCTION(Window::scroll_x_getter)
{
    auto* impl = TRY(impl_from(vm));
    return JS::Value(impl->scroll_x());
}

// https://www.w3.org/TR/cssom-view/#dom-window-scrolly
JS_DEFINE_NATIVE_FUNCTION(Window::scroll_y_getter)
{
    auto* impl = TRY(impl_from(vm));
    return JS::Value(impl->scroll_y());
}

enum class ScrollBehavior {
    Auto,
    Smooth
};

// https://www.w3.org/TR/cssom-view/#perform-a-scroll
static void perform_a_scroll(Page& page, double x, double y, ScrollBehavior)
{
    // FIXME: Stop any existing smooth-scrolls
    // FIXME: Implement smooth-scroll
    page.client().page_did_request_scroll_to({ x, y });
}

// https://www.w3.org/TR/cssom-view/#dom-window-scroll
JS_DEFINE_NATIVE_FUNCTION(Window::scroll)
{
    auto* impl = TRY(impl_from(vm));
    if (!impl->page())
        return JS::js_undefined();
    auto& page = *impl->page();

    auto viewport_rect = page.top_level_browsing_context().viewport_rect();
    auto x_value = JS::Value(viewport_rect.x());
    auto y_value = JS::Value(viewport_rect.y());
    String behavior_string = "auto";

    if (vm.argument_count() == 1) {
        auto* options = TRY(vm.argument(0).to_object(vm));
        auto left = TRY(options->get("left"));
        if (!left.is_undefined())
            x_value = left;

        auto top = TRY(options->get("top"));
        if (!top.is_undefined())
            y_value = top;

        auto behavior_string_value = TRY(options->get("behavior"));
        if (!behavior_string_value.is_undefined())
            behavior_string = TRY(behavior_string_value.to_string(vm));
        if (behavior_string != "smooth" && behavior_string != "auto")
            return vm.throw_completion<JS::TypeError>("Behavior is not one of 'smooth' or 'auto'");

    } else if (vm.argument_count() >= 2) {
        // We ignore arguments 2+ in line with behavior of Chrome and Firefox
        x_value = vm.argument(0);
        y_value = vm.argument(1);
    }

    ScrollBehavior behavior = (behavior_string == "smooth") ? ScrollBehavior::Smooth : ScrollBehavior::Auto;

    double x = TRY(x_value.to_double(vm));
    x = JS::Value(x).is_finite_number() ? x : 0.0;

    double y = TRY(y_value.to_double(vm));
    y = JS::Value(y).is_finite_number() ? y : 0.0;

    // FIXME: Are we calculating the viewport in the way this function expects?
    // FIXME: Handle overflow-directions other than top-left to bottom-right

    perform_a_scroll(page, x, y, behavior);
    return JS::js_undefined();
}

// https://www.w3.org/TR/cssom-view/#dom-window-scrollby
JS_DEFINE_NATIVE_FUNCTION(Window::scroll_by)
{
    auto& realm = *vm.current_realm();

    auto* impl = TRY(impl_from(vm));
    if (!impl->page())
        return JS::js_undefined();
    auto& page = *impl->page();

    JS::Object* options = nullptr;

    if (vm.argument_count() == 0) {
        options = JS::Object::create(realm, nullptr);
    } else if (vm.argument_count() == 1) {
        options = TRY(vm.argument(0).to_object(vm));
    } else if (vm.argument_count() >= 2) {
        // We ignore arguments 2+ in line with behavior of Chrome and Firefox
        options = JS::Object::create(realm, nullptr);
        MUST(options->set("left", vm.argument(0), ShouldThrowExceptions::No));
        MUST(options->set("top", vm.argument(1), ShouldThrowExceptions::No));
        MUST(options->set("behavior", JS::js_string(vm, "auto"), ShouldThrowExceptions::No));
    }

    auto left_value = TRY(options->get("left"));
    auto left = TRY(left_value.to_double(vm));

    auto top_value = TRY(options->get("top"));
    auto top = TRY(top_value.to_double(vm));

    left = JS::Value(left).is_finite_number() ? left : 0.0;
    top = JS::Value(top).is_finite_number() ? top : 0.0;

    auto current_scroll_position = page.top_level_browsing_context().viewport_scroll_offset();
    left = left + current_scroll_position.x();
    top = top + current_scroll_position.y();

    auto behavior_string_value = TRY(options->get("behavior"));
    auto behavior_string = behavior_string_value.is_undefined() ? "auto" : TRY(behavior_string_value.to_string(vm));
    if (behavior_string != "smooth" && behavior_string != "auto")
        return vm.throw_completion<JS::TypeError>("Behavior is not one of 'smooth' or 'auto'");
    ScrollBehavior behavior = (behavior_string == "smooth") ? ScrollBehavior::Smooth : ScrollBehavior::Auto;

    // FIXME: Spec wants us to call scroll(options) here.
    //        The only difference is that would invoke the viewport calculations that scroll()
    //        is not actually doing yet, so this is the same for now.
    perform_a_scroll(page, left, top, behavior);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(Window::history_getter)
{
    auto* impl = TRY(impl_from(vm));
    return impl->associated_document().history();
}

JS_DEFINE_NATIVE_FUNCTION(Window::screen_left_getter)
{
    auto* impl = TRY(impl_from(vm));
    return JS::Value(impl->screen_x());
}

JS_DEFINE_NATIVE_FUNCTION(Window::screen_top_getter)
{
    auto* impl = TRY(impl_from(vm));
    return JS::Value(impl->screen_y());
}

JS_DEFINE_NATIVE_FUNCTION(Window::screen_x_getter)
{
    auto* impl = TRY(impl_from(vm));
    return JS::Value(impl->screen_x());
}

JS_DEFINE_NATIVE_FUNCTION(Window::screen_y_getter)
{
    auto* impl = TRY(impl_from(vm));
    return JS::Value(impl->screen_y());
}

JS_DEFINE_NATIVE_FUNCTION(Window::post_message)
{
    auto* impl = TRY(impl_from(vm));
    auto target_origin = TRY(vm.argument(1).to_string(vm));
    impl->post_message_impl(vm.argument(0), target_origin);
    return JS::js_undefined();
}

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-origin
JS_DEFINE_NATIVE_FUNCTION(Window::origin_getter)
{
    auto* impl = TRY(impl_from(vm));
    return JS::js_string(vm, impl->associated_document().origin().serialize());
}

JS_DEFINE_NATIVE_FUNCTION(Window::local_storage_getter)
{
    auto* impl = TRY(impl_from(vm));
    return impl->local_storage();
}

JS_DEFINE_NATIVE_FUNCTION(Window::session_storage_getter)
{
    auto* impl = TRY(impl_from(vm));
    return impl->session_storage();
}

JS_DEFINE_NATIVE_FUNCTION(Window::name_getter)
{
    auto* impl = TRY(impl_from(vm));
    return JS::js_string(vm, impl->name());
}

JS_DEFINE_NATIVE_FUNCTION(Window::name_setter)
{
    auto* impl = TRY(impl_from(vm));
    impl->set_name(TRY(vm.argument(0).to_string(vm)));
    return JS::js_undefined();
}

#define __ENUMERATE(attribute, event_name)                                      \
    JS_DEFINE_NATIVE_FUNCTION(Window::attribute##_getter)                       \
    {                                                                           \
        auto* impl = TRY(impl_from(vm));                                        \
        auto retval = impl->attribute();                                        \
        if (!retval)                                                            \
            return JS::js_null();                                               \
        return &retval->callback;                                               \
    }                                                                           \
    JS_DEFINE_NATIVE_FUNCTION(Window::attribute##_setter)                       \
    {                                                                           \
        auto* impl = TRY(impl_from(vm));                                        \
        auto value = vm.argument(0);                                            \
        WebIDL::CallbackType* cpp_value = nullptr;                              \
        if (value.is_object()) {                                                \
            cpp_value = vm.heap().allocate_without_realm<WebIDL::CallbackType>( \
                value.as_object(), HTML::incumbent_settings_object());          \
        }                                                                       \
        impl->set_##attribute(cpp_value);                                       \
        return JS::js_undefined();                                              \
    }
ENUMERATE_GLOBAL_EVENT_HANDLERS(__ENUMERATE)
ENUMERATE_WINDOW_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

}
