/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/Utf8View.h>
#include <LibIPC/File.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Shape.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/WindowExposedInterfaces.h>
#include <LibWeb/Bindings/WindowPrototype.h>
#include <LibWeb/CSS/MediaQueryList.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/ResolvedCSSStyleDeclaration.h>
#include <LibWeb/CSS/Screen.h>
#include <LibWeb/Crypto/Crypto.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/DOMURL/DOMURL.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/CloseWatcherManager.h>
#include <LibWeb/HTML/CustomElements/CustomElementRegistry.h>
#include <LibWeb/HTML/DocumentState.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/Focus.h>
#include <LibWeb/HTML/HTMLEmbedElement.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/HTML/HTMLObjectElement.h>
#include <LibWeb/HTML/Location.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/Navigation.h>
#include <LibWeb/HTML/Navigator.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/PageTransitionEvent.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Scripting/ExceptionReporter.h>
#include <LibWeb/HTML/Scripting/TemporaryExecutionContext.h>
#include <LibWeb/HTML/Storage.h>
#include <LibWeb/HTML/TokenizedFeatures.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/HTML/WindowProxy.h>
#include <LibWeb/HighResolutionTime/TimeOrigin.h>
#include <LibWeb/Infra/Base64.h>
#include <LibWeb/Infra/CharacterTypes.h>
#include <LibWeb/Internals/Inspector.h>
#include <LibWeb/Internals/Internals.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/RequestIdleCallback/IdleDeadline.h>
#include <LibWeb/Selection/Selection.h>
#include <LibWeb/WebIDL/AbstractOperations.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(Window);

// https://html.spec.whatwg.org/#run-the-animation-frame-callbacks
void run_animation_frame_callbacks(DOM::Document& document, double now)
{
    // FIXME: Bring this closer to the spec.
    document.window()->animation_frame_callback_driver().run(now);
}

class IdleCallback : public RefCounted<IdleCallback> {
public:
    explicit IdleCallback(ESCAPING Function<JS::Completion(JS::NonnullGCPtr<RequestIdleCallback::IdleDeadline>)> handler, u32 handle)
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
    return realm.heap().allocate<Window>(realm, realm);
}

Window::Window(JS::Realm& realm)
    : DOM::EventTarget(realm)
{
    m_legacy_platform_object_flags = LegacyPlatformObjectFlags {
        .supports_named_properties = true,
        .has_legacy_unenumerable_named_properties_interface_extended_attribute = true,
        .has_global_interface_extended_attribute = true,
    };
}

void Window::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    WindowOrWorkerGlobalScopeMixin::visit_edges(visitor);

    visitor.visit(m_associated_document);
    visitor.visit(m_current_event);
    visitor.visit(m_screen);
    visitor.visit(m_location);
    visitor.visit(m_crypto);
    visitor.visit(m_navigator);
    visitor.visit(m_navigation);
    visitor.visit(m_custom_element_registry);
    visitor.visit(m_pdf_viewer_plugin_objects);
    visitor.visit(m_pdf_viewer_mime_type_objects);
    visitor.visit(m_count_queuing_strategy_size_function);
    visitor.visit(m_byte_length_queuing_strategy_size_function);
    visitor.visit(m_close_watcher_manager);
}

void Window::finalize()
{
    Base::finalize();
    WindowOrWorkerGlobalScopeMixin::finalize();
}

Window::~Window() = default;

// https://html.spec.whatwg.org/multipage/window-object.html#window-open-steps
WebIDL::ExceptionOr<JS::GCPtr<WindowProxy>> Window::open_impl(StringView url, StringView target, StringView features)
{
    // 1. If the event loop's termination nesting level is nonzero, return null.
    if (main_thread_event_loop().termination_nesting_level() != 0)
        return nullptr;

    // 2. Let sourceDocument be the entry global object's associated Document.
    auto& source_document = verify_cast<Window>(entry_global_object()).associated_document();

    // 3. If target is the empty string, then set target to "_blank".
    if (target.is_empty())
        target = "_blank"sv;

    // 4. Let tokenizedFeatures be the result of tokenizing features.
    auto tokenized_features = tokenize_open_features(features);

    // 5. Let noopener and noreferrer be false.
    auto no_opener = TokenizedFeature::NoOpener::No;
    auto no_referrer = TokenizedFeature::NoReferrer::No;

    // 6. If tokenizedFeatures["noopener"] exists, then:
    if (auto no_opener_feature = tokenized_features.get("noopener"sv); no_opener_feature.has_value()) {
        // 1. Set noopener to the result of parsing tokenizedFeatures["noopener"] as a boolean feature.
        no_opener = parse_boolean_feature<TokenizedFeature::NoOpener>(*no_opener_feature);

        // 2. Remove tokenizedFeatures["noopener"].
        tokenized_features.remove("noopener"sv);
    }

    // 7. If tokenizedFeatures["noreferrer"] exists, then:
    if (auto no_referrer_feature = tokenized_features.get("noreferrer"sv); no_referrer_feature.has_value()) {
        // 1. Set noreferrer to the result of parsing tokenizedFeatures["noreferrer"] as a boolean feature.
        no_referrer = parse_boolean_feature<TokenizedFeature::NoReferrer>(*no_referrer_feature);

        // 2. Remove tokenizedFeatures["noreferrer"].
        tokenized_features.remove("noreferrer"sv);
    }

    // 8. Let referrerPolicy be the empty string.
    auto referrer_policy = ReferrerPolicy::ReferrerPolicy::EmptyString;

    // 9. If noreferrer is true, then set noopener to true and set referrerPolicy to "no-referrer".
    if (no_referrer == TokenizedFeature::NoReferrer::Yes) {
        no_opener = TokenizedFeature::NoOpener::Yes;
        referrer_policy = ReferrerPolicy::ReferrerPolicy::NoReferrer;
    }

    // 10. Let targetNavigable and windowType be the result of applying the rules for choosing a navigable given target, sourceDocument's node navigable, and noopener.
    VERIFY(source_document.navigable());
    auto [target_navigable, window_type] = source_document.navigable()->choose_a_navigable(target, no_opener, ActivateTab::Yes, tokenized_features);

    // 11. If targetNavigable is null, then return null.
    if (target_navigable == nullptr)
        return nullptr;

    // 12. If windowType is either "new and unrestricted" or "new with no opener", then:
    if (window_type == Navigable::WindowType::NewAndUnrestricted || window_type == Navigable::WindowType::NewWithNoOpener) {
        // 1. Set the target browsing context's is popup to the result of checking if a popup window is requested, given tokenizedFeatures.
        target_navigable->set_is_popup(check_if_a_popup_window_is_requested(tokenized_features));

        // 2. Set up browsing context features for target browsing context given tokenizedFeatures. [CSSOMVIEW]
        // NOTE: This is implemented in choose_a_navigable when creating the top level traversable.

        // 3. Let urlRecord be the URL record about:blank.
        auto url_record = URL::URL("about:blank"sv);

        // 4. If url is not the empty string, then set urlRecord to the result of encoding-parsing a URL given url, relative to the entry settings object.
        if (!url.is_empty()) {
            url_record = entry_settings_object().parse_url(url);
            // 5. If urlRecord is failure, then throw a "SyntaxError" DOMException.
            if (!url_record.is_valid())
                return WebIDL::SyntaxError::create(realm(), "URL is not valid"_fly_string);
        }

        // 6. If urlRecord matches about:blank, then perform the URL and history update steps given targetNavigable's active document and urlRecord.
        if (url_matches_about_blank(url_record)) {
            // AD-HOC: Mark the initial about:blank for the new window as load complete
            // FIXME: We do this other places too when creating a new about:blank document. Perhaps it's worth a spec issue?
            HTML::HTMLParser::the_end(*target_navigable->active_document());

            perform_url_and_history_update_steps(*target_navigable->active_document(), url_record);
        }

        // 7. Otherwise, navigate targetNavigable to urlRecord using sourceDocument, with referrerPolicy set to referrerPolicy and exceptionsEnabled set to true.
        else {
            TRY(target_navigable->navigate({ .url = url_record, .source_document = source_document, .exceptions_enabled = true, .referrer_policy = referrer_policy }));
        }
    }

    // 13. Otherwise:
    else {
        // 1. If url is not the empty string, then:
        if (!url.is_empty()) {
            // 1. Let urlRecord be the result of encoding-parsing a URL url, relative to the entry settings object.
            auto url_record = entry_settings_object().parse_url(url);

            // 2. If urlRecord is failure, then throw a "SyntaxError" DOMException.
            if (!url_record.is_valid())
                return WebIDL::SyntaxError::create(realm(), "URL is not valid"_fly_string);

            // 3. Navigate targetNavigable to urlRecord using sourceDocument, with referrerPolicy set to referrerPolicy and exceptionsEnabled set to true.
            TRY(target_navigable->navigate({ .url = url_record, .source_document = source_document, .exceptions_enabled = true, .referrer_policy = referrer_policy }));
        }

        // 2. If noopener is false, then set targetNavigable's active browsing context's opener browsing context to sourceDocument's browsing context.
        if (no_opener == TokenizedFeature::NoOpener::No)
            target_navigable->active_browsing_context()->set_opener_browsing_context(source_document.browsing_context());
    }

    // 14. If noopener is true or windowType is "new with no opener", then return null.
    if (no_opener == TokenizedFeature::NoOpener::Yes || window_type == Navigable::WindowType::NewWithNoOpener)
        return nullptr;

    // 15. Return targetNavigable's active WindowProxy.
    return target_navigable->active_window_proxy();
}

bool Window::dispatch_event(DOM::Event& event)
{
    return DOM::EventDispatcher::dispatch(*this, event, true);
}

Page& Window::page()
{
    return associated_document().page();
}

Page const& Window::page() const
{
    return associated_document().page();
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
    case CSS::MediaFeatureID::DeviceHeight:
        return CSS::MediaFeatureValue(CSS::Length::make_px(page().web_exposed_screen_area().height()));
    case CSS::MediaFeatureID::DeviceWidth:
        return CSS::MediaFeatureValue(CSS::Length::make_px(page().web_exposed_screen_area().width()));
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
        switch (page().preferred_color_scheme()) {
        case CSS::PreferredColorScheme::Light:
            return CSS::MediaFeatureValue(CSS::ValueID::Light);
        case CSS::PreferredColorScheme::Dark:
            return CSS::MediaFeatureValue(CSS::ValueID::Dark);
        case CSS::PreferredColorScheme::Auto:
        default:
            return CSS::MediaFeatureValue(page().palette().is_dark() ? CSS::ValueID::Dark : CSS::ValueID::Light);
        }
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
    case CSS::MediaFeatureID::Resolution:
        return CSS::MediaFeatureValue(CSS::Resolution(device_pixel_ratio(), CSS::Resolution::Type::Dppx));
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

// https://html.spec.whatwg.org/#fire-a-page-transition-event
void Window::fire_a_page_transition_event(FlyString const& event_name, bool persisted)
{
    // To fire a page transition event named eventName at a Window window with a boolean persisted,
    // fire an event named eventName at window, using PageTransitionEvent,
    // with the persisted attribute initialized to persisted,
    PageTransitionEventInit event_init {};
    event_init.persisted = persisted;
    auto event = PageTransitionEvent::create(associated_document().realm(), event_name, event_init);

    // ...the cancelable attribute initialized to true,
    event->set_cancelable(true);

    // the bubbles attribute initialized to true,
    event->set_bubbles(true);

    // and legacy target override flag set.
    dispatch_event(event);
}

// https://html.spec.whatwg.org/multipage/webstorage.html#dom-localstorage
WebIDL::ExceptionOr<JS::NonnullGCPtr<Storage>> Window::local_storage()
{
    // FIXME: Implement according to spec.
    static HashMap<Origin, JS::Handle<Storage>> local_storage_per_origin;
    auto storage = local_storage_per_origin.ensure(associated_document().origin(), [this]() -> JS::Handle<Storage> {
        return Storage::create(realm());
    });
    return JS::NonnullGCPtr { *storage };
}

// https://html.spec.whatwg.org/multipage/webstorage.html#dom-sessionstorage
WebIDL::ExceptionOr<JS::NonnullGCPtr<Storage>> Window::session_storage()
{
    // FIXME: Implement according to spec.
    static HashMap<Origin, JS::Handle<Storage>> session_storage_per_origin;
    auto storage = session_storage_per_origin.ensure(associated_document().origin(), [this]() -> JS::Handle<Storage> {
        return Storage::create(realm());
    });
    return JS::NonnullGCPtr { *storage };
}

// https://html.spec.whatwg.org/multipage/interaction.html#sticky-activation
bool Window::has_sticky_activation() const
{
    // When the current high resolution time given W
    auto current_time = HighResolutionTime::current_high_resolution_time(*this);

    // is greater than or equal to the last activation timestamp in W
    if (current_time >= m_last_activation_timestamp) {
        // W is said to have sticky activation.
        return true;
    }

    return false;
}

// https://html.spec.whatwg.org/multipage/interaction.html#transient-activation
bool Window::has_transient_activation() const
{
    // The transient activation duration is expected be at most a few seconds, so that the user can possibly
    // perceive the link between an interaction with the page and the page calling the activation-gated API.
    static constexpr HighResolutionTime::DOMHighResTimeStamp transient_activation_duration_ms = 5000;

    // When the current high resolution time given W
    auto current_time = HighResolutionTime::current_high_resolution_time(*this);

    // is greater than or equal to the last activation timestamp in W
    if (current_time >= m_last_activation_timestamp) {
        // and less than the last activation timestamp in W plus the transient activation duration
        if (current_time < m_last_activation_timestamp + transient_activation_duration_ms) {
            // then W is said to have transient activation.
            return true;
        }
    }

    return false;
}

// https://html.spec.whatwg.org/multipage/interaction.html#history-action-activation
bool Window::has_history_action_activation() const
{
    // When the last history-action activation timestamp of W is not equal to the last activation timestamp of W, then W is said to have history-action activation.
    return m_last_history_action_activation_timestamp != m_last_activation_timestamp;
}

// https://html.spec.whatwg.org/multipage/interaction.html#consume-history-action-user-activation
void Window::consume_history_action_user_activation()
{
    auto navigable = this->navigable();

    // 1. If W's navigable is null, then return.
    if (navigable == nullptr)
        return;

    // 2. Let top be W's navigable's top-level traversable.
    auto top = navigable->top_level_traversable();

    // 3. Let navigables be the inclusive descendant navigables of top's active document.
    auto navigables = top->active_document()->inclusive_descendant_navigables();

    // 4. Let windows be the list of Window objects constructed by taking the active window of each item in navigables.
    JS::MarkedVector<JS::GCPtr<Window>> windows(heap());
    for (auto& n : navigables)
        windows.append(n->active_window());

    // 5. For each window in windows, set window's last history-action activation timestamp to window's last activation timestamp.
    for (auto& window : windows)
        window->set_last_history_action_activation_timestamp(window->last_activation_timestamp());
}

// https://html.spec.whatwg.org/multipage/interaction.html#consume-user-activation
void Window::consume_user_activation()
{
    auto navigable = this->navigable();

    // 1. If W's navigable is null, then return.
    if (navigable == nullptr)
        return;

    // 2. Let top be W's navigable's top-level traversable.
    auto top = navigable->top_level_traversable();

    // 3. Let navigables be the inclusive descendant navigables of top's active document.
    auto navigables = top->active_document()->inclusive_descendant_navigables();

    // 4. Let windows be the list of Window objects constructed by taking the active window of each item in navigables.
    JS::MarkedVector<JS::GCPtr<Window>> windows(heap());
    for (auto& n : navigables)
        windows.append(n->active_window());

    // 5. For each window in windows, if window's last activation timestamp is not positive infinity, then set window's last activation timestamp to negative infinity.
    for (auto& window : windows) {
        if (window->last_activation_timestamp() != AK::Infinity<HighResolutionTime::DOMHighResTimeStamp>)
            window->set_last_activation_timestamp(-AK::Infinity<HighResolutionTime::DOMHighResTimeStamp>);
    }
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
    queue_global_task(Task::Source::IdleTask, *this, JS::create_heap_function(heap(), [this] {
        invoke_idle_callbacks();
    }));
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
            report_exception(result, realm());
        // 4. If window's list of runnable idle callbacks is not empty, queue a task which performs the steps
        //    in the invoke idle callbacks algorithm with getDeadline and window as a parameters and return from this algorithm
        queue_global_task(Task::Source::IdleTask, *this, JS::create_heap_function(heap(), [this] {
            invoke_idle_callbacks();
        }));
    }
}

void Window::set_associated_document(DOM::Document& document)
{
    m_associated_document = &document;
}

void Window::set_current_event(DOM::Event* event)
{
    m_current_event = event;
}

BrowsingContext const* Window::browsing_context() const
{
    return m_associated_document->browsing_context();
}

BrowsingContext* Window::browsing_context()
{
    return m_associated_document->browsing_context();
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#window-navigable
JS::GCPtr<Navigable> Window::navigable() const
{
    // A Window's navigable is the navigable whose active document is the Window's associated Document's, or null if there is no such navigable.
    return Navigable::navigable_with_active_document(*m_associated_document);
}

// https://html.spec.whatwg.org/multipage/system-state.html#pdf-viewer-plugin-objects
Vector<JS::NonnullGCPtr<Plugin>> Window::pdf_viewer_plugin_objects()
{
    // Each Window object has a PDF viewer plugin objects list. If the user agent's PDF viewer supported is false, then it is the empty list.
    // Otherwise, it is a list containing five Plugin objects, whose names are, respectively:
    // 0.   "PDF Viewer"
    // 1.   "Chrome PDF Viewer"
    // 2.   "Chromium PDF Viewer"
    // 3.   "Microsoft Edge PDF Viewer"
    // 4.   "WebKit built-in PDF"
    // The values of the above list form the PDF viewer plugin names list. https://html.spec.whatwg.org/multipage/system-state.html#pdf-viewer-plugin-names
    if (!page().pdf_viewer_supported())
        return {};

    if (m_pdf_viewer_plugin_objects.is_empty()) {
        // FIXME: Propagate errors.
        m_pdf_viewer_plugin_objects.append(realm().heap().allocate<Plugin>(realm(), realm(), "PDF Viewer"_string));
        m_pdf_viewer_plugin_objects.append(realm().heap().allocate<Plugin>(realm(), realm(), "Chrome PDF Viewer"_string));
        m_pdf_viewer_plugin_objects.append(realm().heap().allocate<Plugin>(realm(), realm(), "Chromium PDF Viewer"_string));
        m_pdf_viewer_plugin_objects.append(realm().heap().allocate<Plugin>(realm(), realm(), "Microsoft Edge PDF Viewer"_string));
        m_pdf_viewer_plugin_objects.append(realm().heap().allocate<Plugin>(realm(), realm(), "WebKit built-in PDF"_string));
    }

    return m_pdf_viewer_plugin_objects;
}

// https://html.spec.whatwg.org/multipage/system-state.html#pdf-viewer-mime-type-objects
Vector<JS::NonnullGCPtr<MimeType>> Window::pdf_viewer_mime_type_objects()
{
    // Each Window object has a PDF viewer mime type objects list. If the user agent's PDF viewer supported is false, then it is the empty list.
    // Otherwise, it is a list containing two MimeType objects, whose types are, respectively:
    // 0.   "application/pdf"
    // 1.   "text/pdf"
    // The values of the above list form the PDF viewer mime types list. https://html.spec.whatwg.org/multipage/system-state.html#pdf-viewer-mime-types
    if (!page().pdf_viewer_supported())
        return {};

    if (m_pdf_viewer_mime_type_objects.is_empty()) {
        m_pdf_viewer_mime_type_objects.append(realm().heap().allocate<MimeType>(realm(), realm(), "application/pdf"_string));
        m_pdf_viewer_mime_type_objects.append(realm().heap().allocate<MimeType>(realm(), realm(), "text/pdf"_string));
    }

    return m_pdf_viewer_mime_type_objects;
}

// https://streams.spec.whatwg.org/#count-queuing-strategy-size-function
JS::NonnullGCPtr<WebIDL::CallbackType> Window::count_queuing_strategy_size_function()
{
    auto& realm = this->realm();

    if (!m_count_queuing_strategy_size_function) {
        // 1. Let steps be the following steps:
        auto steps = [](auto const&) {
            // 1. Return 1.
            return 1.0;
        };

        // 2. Let F be ! CreateBuiltinFunction(steps, 0, "size", « », globalObject’s relevant Realm).
        auto function = JS::NativeFunction::create(realm, move(steps), 0, "size", &realm);

        // 3. Set globalObject’s count queuing strategy size function to a Function that represents a reference to F, with callback context equal to globalObject’s relevant settings object.
        m_count_queuing_strategy_size_function = heap().allocate<WebIDL::CallbackType>(realm, *function, relevant_settings_object(*this));
    }

    return JS::NonnullGCPtr { *m_count_queuing_strategy_size_function };
}

// https://streams.spec.whatwg.org/#byte-length-queuing-strategy-size-function
JS::NonnullGCPtr<WebIDL::CallbackType> Window::byte_length_queuing_strategy_size_function()
{
    auto& realm = this->realm();

    if (!m_byte_length_queuing_strategy_size_function) {
        // 1. Let steps be the following steps, given chunk:
        auto steps = [](JS::VM& vm) {
            auto chunk = vm.argument(0);

            // 1. Return ? GetV(chunk, "byteLength").
            return chunk.get(vm, vm.names.byteLength);
        };

        // 2. Let F be ! CreateBuiltinFunction(steps, 1, "size", « », globalObject’s relevant Realm).
        auto function = JS::NativeFunction::create(realm, move(steps), 1, "size", &realm);

        // 3. Set globalObject’s byte length queuing strategy size function to a Function that represents a reference to F, with callback context equal to globalObject’s relevant settings object.
        m_byte_length_queuing_strategy_size_function = heap().allocate<WebIDL::CallbackType>(realm, *function, relevant_settings_object(*this));
    }

    return JS::NonnullGCPtr { *m_byte_length_queuing_strategy_size_function };
}

static bool s_inspector_object_exposed = false;
static bool s_internals_object_exposed = false;

void Window::set_inspector_object_exposed(bool exposed)
{
    s_inspector_object_exposed = exposed;
}

void Window::set_internals_object_exposed(bool exposed)
{
    s_internals_object_exposed = exposed;
}

WebIDL::ExceptionOr<void> Window::initialize_web_interfaces(Badge<WindowEnvironmentSettingsObject>)
{
    auto& realm = this->realm();
    add_window_exposed_interfaces(*this);

    WEB_SET_PROTOTYPE_FOR_INTERFACE(Window);

    Bindings::WindowGlobalMixin::initialize(realm, *this);
    WindowOrWorkerGlobalScopeMixin::initialize(realm);

    if (s_inspector_object_exposed)
        define_direct_property("inspector", heap().allocate<Internals::Inspector>(realm, realm), JS::default_attributes);
    if (s_internals_object_exposed)
        define_direct_property("internals", heap().allocate<Internals::Internals>(realm, realm), JS::default_attributes);

    return {};
}

// https://webidl.spec.whatwg.org/#platform-object-setprototypeof
JS::ThrowCompletionOr<bool> Window::internal_set_prototype_of(JS::Object* prototype)
{
    // 1. Return ? SetImmutablePrototype(O, V).
    return set_immutable_prototype(prototype);
}

// https://html.spec.whatwg.org/multipage/window-object.html#dom-window
JS::NonnullGCPtr<WindowProxy> Window::window() const
{
    // The window, frames, and self getter steps are to return this's relevant realm.[[GlobalEnv]].[[GlobalThisValue]].
    return verify_cast<WindowProxy>(relevant_realm(*this).global_environment().global_this_value());
}

// https://html.spec.whatwg.org/multipage/window-object.html#dom-self
JS::NonnullGCPtr<WindowProxy> Window::self() const
{
    // The window, frames, and self getter steps are to return this's relevant realm.[[GlobalEnv]].[[GlobalThisValue]].
    return verify_cast<WindowProxy>(relevant_realm(*this).global_environment().global_this_value());
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-document-2
JS::NonnullGCPtr<DOM::Document const> Window::document() const
{
    // The document getter steps are to return this's associated Document.
    return associated_document();
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-name
String Window::name() const
{
    // 1. If this's navigable is null, then return the empty string.
    if (!navigable())
        return String {};

    // 2. Return this's navigable's target name.
    return navigable()->target_name();
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#apis-for-creating-and-navigating-browsing-contexts-by-name:dom-name
void Window::set_name(String const& name)
{
    // 1. If this's navigable is null, then return.
    if (!navigable())
        return;

    // 2. Set this's navigable's active session history entry's document state's navigable target name to the given value.
    navigable()->active_session_history_entry()->document_state()->set_navigable_target_name(name);
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-window-status
String Window::status() const
{
    // the status attribute on the Window object must, on getting, return the last string it was set to
    return m_status;
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-window-close
void Window::close()
{
    // FIXME: Implement this properly
    dbgln("(STUBBED) Window::close()");
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-window-closed
bool Window::closed() const
{
    // FIXME: Implement this properly
    dbgln("(STUBBED) Window::closed");
    return false;
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-window-status
void Window::set_status(String const& status)
{
    // on setting, must set itself to the new value.
    m_status = status;
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-location
JS::NonnullGCPtr<Location> Window::location()
{
    auto& realm = this->realm();

    // The Window object's location getter steps are to return this's Location object.
    if (!m_location)
        m_location = heap().allocate<Location>(realm, realm);
    return JS::NonnullGCPtr { *m_location };
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-history
JS::NonnullGCPtr<History> Window::history() const
{
    // The history getter steps are to return this's associated Document's history object.
    return associated_document().history();
}

// https://html.spec.whatwg.org/multipage/interaction.html#dom-window-focus
void Window::focus()
{
    // 1. Let current be this Window object's navigable.
    auto current = navigable();

    // 2. If current is null, then return.
    if (!current)
        return;

    // 3. Run the focusing steps with current.
    // FIXME: We should pass in the browsing context itself instead of the active document, however the focusing steps don't currently accept browsing contexts.
    //        Passing in a browsing context always makes it resolve to its active document for focus, so this is fine for now.
    run_focusing_steps(current->active_document());

    // FIXME: 4. If current is a top-level traversable, user agents are encouraged to trigger some sort of notification to
    //           indicate to the user that the page is attempting to gain focus.
}

// https://html.spec.whatwg.org/multipage/interaction.html#dom-window-blur
void Window::blur()
{
    // The blur() method steps are to do nothing.
}

// https://html.spec.whatwg.org/multipage/window-object.html#dom-frames
JS::NonnullGCPtr<WindowProxy> Window::frames() const
{
    // The window, frames, and self getter steps are to return this's relevant realm.[[GlobalEnv]].[[GlobalThisValue]].
    return verify_cast<WindowProxy>(relevant_realm(*this).global_environment().global_this_value());
}

// https://html.spec.whatwg.org/multipage/window-object.html#dom-length
u32 Window::length()
{
    // The length getter steps are to return this's associated Document's document-tree child navigables's size.
    return associated_document().document_tree_child_navigables().size();
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-top
JS::GCPtr<WindowProxy const> Window::top() const
{
    // 1. If this's navigable is null, then return null.
    auto navigable = this->navigable();
    if (!navigable)
        return {};

    // 2. Return this's navigable's top-level traversable's active WindowProxy.
    return navigable->top_level_traversable()->active_window_proxy();
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-opener
JS::GCPtr<WindowProxy const> Window::opener() const
{
    // 1. Let current be this's browsing context.
    auto const* current = browsing_context();

    // 2. If current is null, then return null.
    if (!current)
        return {};

    // 3. If current's opener browsing context is null, then return null.
    auto opener_browsing_context = current->opener_browsing_context();
    if (!opener_browsing_context)
        return {};

    // 4. Return current's opener browsing context's WindowProxy object.
    return opener_browsing_context->window_proxy();
}

WebIDL::ExceptionOr<void> Window::set_opener(JS::Value value)
{
    // 1. If the given value is null and this's browsing context is non-null, then set this's browsing context's opener browsing context to null.
    auto* browsing_context = this->browsing_context();
    if (value.is_null() && browsing_context)
        browsing_context->set_opener_browsing_context(nullptr);

    // 2. If the given value is non-null, then perform ? DefinePropertyOrThrow(this, "opener", { [[Value]]: the given value, [[Writable]]: true, [[Enumerable]]: true, [[Configurable]]: true }).
    if (!value.is_null()) {
        static JS::PropertyKey opener_property_key { "opener", JS::PropertyKey::StringMayBeNumber::No };
        TRY(define_property_or_throw(opener_property_key, { .value = value, .writable = true, .enumerable = true, .configurable = true }));
    }

    return {};
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-parent
JS::GCPtr<WindowProxy const> Window::parent() const
{
    // 1. Let navigable be this's navigable.
    auto navigable = this->navigable();

    // 2. If navigable is null, then return null.
    if (!navigable)
        return {};

    // 3. If navigable's parent is not null, then set navigable to navigable's parent.
    if (auto parent = navigable->parent())
        navigable = parent;

    // 4. Return navigable's active WindowProxy.
    return navigable->active_window_proxy();
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-frameelement
JS::GCPtr<DOM::Element const> Window::frame_element() const
{
    // 1. Let current be this's node navigable.
    auto current = navigable();

    // 2. If current is null, then return null.
    if (!current)
        return {};

    // 3. Let container be current's container.
    auto container = current->container();

    // 4. If container is null, then return null.
    if (!container)
        return {};

    // 5. If container's node document's origin is not same origin-domain with the current settings object's origin, then return null.
    if (!container->document().origin().is_same_origin_domain(current_settings_object().origin()))
        return {};

    // 6. Return container.
    return container;
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-open
WebIDL::ExceptionOr<JS::GCPtr<WindowProxy>> Window::open(Optional<String> const& url, Optional<String> const& target, Optional<String> const& features)
{
    // The open(url, target, features) method steps are to run the window open steps with url, target, and features.
    return open_impl(*url, *target, *features);
}

// https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator
JS::NonnullGCPtr<Navigator> Window::navigator()
{
    auto& realm = this->realm();

    // The navigator and clientInformation getter steps are to return this's associated Navigator.
    if (!m_navigator)
        m_navigator = heap().allocate<Navigator>(realm, realm);
    return JS::NonnullGCPtr { *m_navigator };
}

// https://html.spec.whatwg.org/multipage/interaction.html#close-watcher-manager
JS::NonnullGCPtr<CloseWatcherManager> Window::close_watcher_manager()
{
    auto& realm = this->realm();

    if (!m_close_watcher_manager)
        m_close_watcher_manager = heap().allocate<CloseWatcherManager>(realm, realm);
    return JS::NonnullGCPtr { *m_close_watcher_manager };
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-alert
void Window::alert(String const& message)
{
    // https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#simple-dialogs
    // Note: This method is defined using two overloads, instead of using an optional argument,
    //       for historical reasons. The practical impact of this is that alert(undefined) is
    //       treated as alert("undefined"), but alert() is treated as alert("").
    // FIXME: Make this fully spec compliant.
    page().did_request_alert(message);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-confirm
bool Window::confirm(Optional<String> const& message)
{
    // FIXME: Make this fully spec compliant.
    // NOTE: `message` has an IDL-provided default value and is never empty.
    return page().did_request_confirm(*message);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-prompt
Optional<String> Window::prompt(Optional<String> const& message, Optional<String> const& default_)
{
    // FIXME: Make this fully spec compliant.
    return page().did_request_prompt(*message, *default_);
}

// https://html.spec.whatwg.org/multipage/web-messaging.html#window-post-message-steps
WebIDL::ExceptionOr<void> Window::window_post_message_steps(JS::Value message, WindowPostMessageOptions const& options)
{
    // 1. Let targetRealm be targetWindow's realm.
    auto& target_realm = this->realm();

    // 2. Let incumbentSettings be the incumbent settings object.
    auto& incumbent_settings = incumbent_settings_object();

    // 3. Let targetOrigin be options["targetOrigin"].
    Variant<String, Origin> target_origin = options.target_origin;

    // 4. If targetOrigin is a single U+002F SOLIDUS character (/), then set targetOrigin to incumbentSettings's origin.
    if (options.target_origin == "/"sv) {
        target_origin = incumbent_settings.origin();
    }
    // 5. Otherwise, if targetOrigin is not a single U+002A ASTERISK character (*), then:
    else if (options.target_origin != "*"sv) {
        // 1. Let parsedURL be the result of running the URL parser on targetOrigin.
        auto parsed_url = DOMURL::parse(options.target_origin);

        // 2. If parsedURL is failure, then throw a "SyntaxError" DOMException.
        if (!parsed_url.is_valid())
            return WebIDL::SyntaxError::create(target_realm, MUST(String::formatted("Invalid URL for targetOrigin: '{}'", options.target_origin)));

        // 3. Set targetOrigin to parsedURL's origin.
        target_origin = DOMURL::url_origin(parsed_url);
    }

    // 6. Let transfer be options["transfer"].
    auto& transfer = options.transfer;

    // 7. Let serializeWithTransferResult be StructuredSerializeWithTransfer(message, transfer). Rethrow any exceptions.
    auto serialize_with_transfer_result = TRY(structured_serialize_with_transfer(target_realm.vm(), message, transfer));

    // 8. Queue a global task on the posted message task source given targetWindow to run the following steps:
    queue_global_task(Task::Source::PostedMessage, *this, JS::create_heap_function(heap(), [this, serialize_with_transfer_result = move(serialize_with_transfer_result), target_origin = move(target_origin), &incumbent_settings, &target_realm]() mutable {
        // 1. If the targetOrigin argument is not a single literal U+002A ASTERISK character (*) and targetWindow's
        //    associated Document's origin is not same origin with targetOrigin, then return.
        // NOTE: Due to step 4 and 5 above, the only time it's not '*' is if target_origin contains an Origin.
        if (!target_origin.has<String>()) {
            auto const& actual_target_origin = target_origin.get<Origin>();
            if (!document()->origin().is_same_origin(actual_target_origin))
                return;
        }

        // 2. Let origin be the serialization of incumbentSettings's origin.
        auto origin = incumbent_settings.origin().serialize();

        // 3. Let source be the WindowProxy object corresponding to incumbentSettings's global object (a Window object).
        auto& source = verify_cast<WindowProxy>(incumbent_settings.realm().global_environment().global_this_value());

        // 4. Let deserializeRecord be StructuredDeserializeWithTransfer(serializeWithTransferResult, targetRealm).
        auto& settings_object = Bindings::host_defined_environment_settings_object(target_realm);
        auto temporary_execution_context = TemporaryExecutionContext { settings_object };
        auto deserialize_record_or_error = structured_deserialize_with_transfer(vm(), serialize_with_transfer_result);

        // If this throws an exception, catch it, fire an event named messageerror at targetWindow, using MessageEvent,
        // with the origin attribute initialized to origin and the source attribute initialized to source, and then return.
        if (deserialize_record_or_error.is_exception()) {
            MessageEventInit message_event_init {};
            message_event_init.origin = MUST(String::from_byte_string(origin));
            message_event_init.source = JS::make_handle(source);

            auto message_error_event = MessageEvent::create(target_realm, EventNames::messageerror, message_event_init);
            dispatch_event(message_error_event);
            return;
        }

        // 5. Let messageClone be deserializeRecord.[[Deserialized]].
        auto deserialize_record = deserialize_record_or_error.release_value();
        auto message_clone = deserialize_record.deserialized;

        // 6. Let newPorts be a new frozen array consisting of all MessagePort objects in deserializeRecord.[[TransferredValues]],
        //    if any, maintaining their relative order.
        // FIXME: Use a FrozenArray
        Vector<JS::Handle<MessagePort>> new_ports;
        for (auto const& object : deserialize_record.transferred_values) {
            if (is<HTML::MessagePort>(*object)) {
                new_ports.append(verify_cast<MessagePort>(*object));
            }
        }

        // 7. Fire an event named message at targetWindow, using MessageEvent, with the origin attribute initialized to origin,
        //    the source attribute initialized to source, the data attribute initialized to messageClone, and the ports attribute
        //    initialized to newPorts.
        MessageEventInit message_event_init {};
        message_event_init.origin = MUST(String::from_byte_string(origin));
        message_event_init.source = JS::make_handle(source);
        message_event_init.data = message_clone;
        message_event_init.ports = move(new_ports);

        auto message_event = MessageEvent::create(target_realm, EventNames::message, message_event_init);
        dispatch_event(message_event);
    }));

    return {};
}

// https://html.spec.whatwg.org/multipage/web-messaging.html#dom-window-postmessage-options
WebIDL::ExceptionOr<void> Window::post_message(JS::Value message, WindowPostMessageOptions const& options)
{
    // The Window interface's postMessage(message, options) method steps are to run the window post message steps given
    // this, message, and options.
    return window_post_message_steps(message, options);
}

// https://html.spec.whatwg.org/multipage/web-messaging.html#dom-window-postmessage
WebIDL::ExceptionOr<void> Window::post_message(JS::Value message, String const& target_origin, Vector<JS::Handle<JS::Object>> const& transfer)
{
    // The Window interface's postMessage(message, targetOrigin, transfer) method steps are to run the window post message
    // steps given this, message, and «[ "targetOrigin" → targetOrigin, "transfer" → transfer ]».
    return window_post_message_steps(message, WindowPostMessageOptions { { .transfer = transfer }, target_origin });
}

// https://dom.spec.whatwg.org/#dom-window-event
Variant<JS::Handle<DOM::Event>, JS::Value> Window::event() const
{
    // The event getter steps are to return this’s current event.
    if (auto* current_event = this->current_event())
        return make_handle(const_cast<DOM::Event&>(*current_event));
    return JS::js_undefined();
}

// https://w3c.github.io/csswg-drafts/cssom/#dom-window-getcomputedstyle
JS::NonnullGCPtr<CSS::CSSStyleDeclaration> Window::get_computed_style(DOM::Element& element, Optional<String> const& pseudo_element) const
{
    // FIXME: Make this fully spec compliant.
    (void)pseudo_element;
    return heap().allocate<CSS::ResolvedCSSStyleDeclaration>(realm(), element);
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-matchmedia
WebIDL::ExceptionOr<JS::NonnullGCPtr<CSS::MediaQueryList>> Window::match_media(String const& query)
{
    // 1. Let parsed media query list be the result of parsing query.
    auto parsed_media_query_list = parse_media_query_list(CSS::Parser::ParsingContext(associated_document()), query);

    // 2. Return a new MediaQueryList object, with this's associated Document as the document, with parsed media query list as its associated media query list.
    auto media_query_list = heap().allocate<CSS::MediaQueryList>(realm(), associated_document(), move(parsed_media_query_list));
    associated_document().add_media_query_list(media_query_list);
    return media_query_list;
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-screen
JS::NonnullGCPtr<CSS::Screen> Window::screen()
{
    // The screen attribute must return the Screen object associated with the Window object.
    if (!m_screen)
        m_screen = heap().allocate<CSS::Screen>(realm(), *this);
    return JS::NonnullGCPtr { *m_screen };
}

JS::GCPtr<CSS::VisualViewport> Window::visual_viewport()
{
    // If the associated document is fully active, the visualViewport attribute must return
    // the VisualViewport object associated with the Window object’s associated document.
    if (associated_document().is_fully_active())
        return associated_document().visual_viewport();

    // Otherwise, it must return null.
    return nullptr;
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-innerwidth
i32 Window::inner_width() const
{
    // The innerWidth attribute must return the viewport width including the size of a rendered scroll bar (if any),
    // or zero if there is no viewport.
    if (auto const navigable = associated_document().navigable())
        return navigable->viewport_rect().width().to_int();
    return 0;
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-innerheight
i32 Window::inner_height() const
{
    // The innerHeight attribute must return the viewport height including the size of a rendered scroll bar (if any),
    // or zero if there is no viewport.
    if (auto const navigable = associated_document().navigable())
        return navigable->viewport_rect().height().to_int();
    return 0;
}

// https://drafts.csswg.org/cssom-view/#dom-window-moveto
void Window::move_to(long, long) const
{
    // 1. Optionally, return.
    return;

    // NOTE: For now, the other steps are unimplemented because we always return in step 1.
    // FIXME: Implement these for popup windows?
    // 2. Let target be this's relevant global object's browsing context.
    // 3. If target is not an auxiliary browsing context that was created by a script (as opposed to by an action of the user), then return.
    // 4. Optionally, clamp x and y in a user-agent-defined manner so that the window does not move outside the available space.
    // 5. Move target’s window such that the window’s top left corner is at coordinates (x, y) relative to the top left corner of the output device, measured in CSS pixels of target. The positive axes are rightward and downward.
}

// https://drafts.csswg.org/cssom-view/#dom-window-moveby
void Window::move_by(long, long) const
{
    // 1. Optionally, return.
    return;

    // NOTE: For now, the other steps are unimplemented because we always return in step 1.
    // FIXME: Implement these for popup windows?
    // 2. Let target be this's relevant global object's browsing context.
    // 3. If target is not an auxiliary browsing context that was created by a script (as opposed to by an action of the user), then return.
    // 4. Optionally, clamp x and y in a user-agent-defined manner so that the window does not move outside the available space.
    // 5. Move target’s window x CSS pixels of target rightward and y CSS pixels of target downward.
}

// https://drafts.csswg.org/cssom-view/#dom-window-resizeto
void Window::resize_to(long, long) const
{
    // 1. Optionally, return.
    return;

    // NOTE: For now, the other steps are unimplemented because we always return in step 1.
    // FIXME: Implement these for popup windows?
    // 2. Let target be this's relevant global object's browsing context.
    // 3. If target is not an auxiliary browsing context that was created by a script (as opposed to by an action of the user), then return.
    // 4. Optionally, clamp width and height in a user-agent-defined manner so that the window does not get too small or bigger than the available space.
    // 5. Resize target’s window by moving its right and bottom edges such that the distance between the left and right edges of the viewport are width CSS pixels of target and the distance between the top and bottom edges of the viewport are height CSS pixels of target.
    // 6. Optionally, move target’s window in a user-agent-defined manner so that it does not grow outside the available space.
}

// https://drafts.csswg.org/cssom-view/#dom-window-resizeby
void Window::resize_by(long, long) const
{
    // 1. Optionally, return.
    return;

    // NOTE: For now, the other steps are unimplemented because we always return in step 1.
    // FIXME: Implement these for popup windows?
    // 2. Let target be this's relevant global object's browsing context.
    // 3. If target is not an auxiliary browsing context that was created by a script (as opposed to by an action of the user), then return.
    // 4. Optionally, clamp x and y in a user-agent-defined manner so that the window does not get too small or bigger than the available space.
    // 5. Resize target’s window by moving its right edge x CSS pixels of target rightward and its bottom edge y CSS pixels of target downward.
    // 6. Optionally, move target’s window in a user-agent-defined manner so that it does not grow outside the available space.
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-scrollx
double Window::scroll_x() const
{
    // The scrollX attribute must return the x-coordinate, relative to the initial containing block origin,
    // of the left of the viewport, or zero if there is no viewport.
    if (auto const navigable = associated_document().navigable())
        return navigable->viewport_rect().x().to_double();
    return 0;
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-scrolly
double Window::scroll_y() const
{
    // The scrollY attribute must return the y-coordinate, relative to the initial containing block origin,
    // of the top of the viewport, or zero if there is no viewport.
    if (auto const navigable = associated_document().navigable())
        return navigable->viewport_rect().y().to_double();
    return 0;
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-scroll
void Window::scroll(ScrollToOptions const& options)
{
    // 4. If there is no viewport, abort these steps.
    auto navigable = associated_document().navigable();
    if (!navigable)
        return;

    // 1. If invoked with one argument, follow these substeps:

    // 1. Let options be the argument.
    auto viewport_rect = navigable->viewport_rect().to_type<float>();

    // 2. Let x be the value of the left dictionary member of options, if present, or the viewport’s current scroll
    //    position on the x axis otherwise.
    auto x = options.left.value_or(viewport_rect.x());

    // 3. Let y be the value of the top dictionary member of options, if present, or the viewport’s current scroll
    //    position on the y axis otherwise.
    auto y = options.top.value_or(viewport_rect.y());

    // 3. Normalize non-finite values for x and y.
    x = HTML::normalize_non_finite_values(x);
    y = HTML::normalize_non_finite_values(y);

    // 5. Let viewport width be the width of the viewport excluding the width of the scroll bar, if any.
    auto viewport_width = viewport_rect.width();

    // 6. Let viewport height be the height of the viewport excluding the height of the scroll bar, if any.
    auto viewport_height = viewport_rect.height();

    auto const document = navigable->active_document();
    VERIFY(document);

    // Make sure layout is up-to-date before looking at scrollable overflow metrics.
    document->update_layout();

    VERIFY(document->paintable_box());
    auto scrolling_area = document->paintable_box()->scrollable_overflow_rect()->to_type<float>();

    // 7. FIXME: For now we always assume overflow direction is rightward
    // -> If the viewport has rightward overflow direction
    //    Let x be max(0, min(x, viewport scrolling area width - viewport width)).
    x = max(0.0f, min(x, scrolling_area.width() - viewport_width));
    // -> If the viewport has leftward overflow direction
    //    Let x be min(0, max(x, viewport width - viewport scrolling area width)).

    // 8. FIXME: For now we always assume overflow direction is downward
    // -> If the viewport has downward overflow direction
    //    Let y be max(0, min(y, viewport scrolling area height - viewport height)).
    y = max(0.0f, min(y, scrolling_area.height() - viewport_height));
    // -> If the viewport has upward overflow direction
    //    Let y be min(0, max(y, viewport height - viewport scrolling area height)).

    // FIXME: 9. Let position be the scroll position the viewport would have by aligning the x-coordinate x of the viewport
    //           scrolling area with the left of the viewport and aligning the y-coordinate y of the viewport scrolling area
    //           with the top of the viewport.
    auto position = Gfx::FloatPoint { x, y };

    // 10. If position is the same as the viewport’s current scroll position, and the viewport does not have an ongoing
    //     smooth scroll, abort these steps.
    if (position == viewport_rect.location())
        return;

    // 11. Let document be the viewport’s associated Document.
    //     NOTE: document is already defined above.

    // 12. Perform a scroll of the viewport to position, document’s root element as the associated element, if there is
    //     one, or null otherwise, and the scroll behavior being the value of the behavior dictionary member of options.
    navigable->perform_scroll_of_viewport({ x, y });
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-scroll
void Window::scroll(double x, double y)
{
    // 2. If invoked with two arguments, follow these substeps:

    // 1. Let options be null converted to a ScrollToOptions dictionary. [WEBIDL]
    auto options = ScrollToOptions {};

    // 2. Let x and y be the arguments, respectively.

    options.left = x;
    options.top = y;

    scroll(options);
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-scrollby
void Window::scroll_by(ScrollToOptions options)
{
    // 2. Normalize non-finite values for the left and top dictionary members of options.
    auto left = HTML::normalize_non_finite_values(options.left);
    auto top = HTML::normalize_non_finite_values(options.top);

    // 3. Add the value of scrollX to the left dictionary member.
    options.left = left + scroll_x();

    // 4. Add the value of scrollY to the top dictionary member.
    options.top = top + scroll_y();

    // 5. Act as if the scroll() method was invoked with options as the only argument.
    scroll(options);
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-scrollby
void Window::scroll_by(double x, double y)
{
    // 1. If invoked with two arguments, follow these substeps:

    // 1. Let options be null converted to a ScrollToOptions dictionary. [WEBIDL]
    auto options = ScrollToOptions {};

    // 2. Let x and y be the arguments, respectively.

    // 3. Let the left dictionary member of options have the value x.
    options.left = x;

    // 4. Let the top dictionary member of options have the value y.
    options.top = y;

    scroll_by(options);
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-screenx
i32 Window::screen_x() const
{
    // The screenX and screenLeft attributes must return the x-coordinate, relative to the origin of the Web-exposed
    // screen area, of the left of the client window as number of CSS pixels, or zero if there is no such thing.
    return page().window_position().x().value();
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-screeny
i32 Window::screen_y() const
{
    // The screenY and screenTop attributes must return the y-coordinate, relative to the origin of the screen of the
    // Web-exposed screen area, of the top of the client window as number of CSS pixels, or zero if there is no such thing.
    return page().window_position().y().value();
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-outerwidth
i32 Window::outer_width() const
{
    // The outerWidth attribute must return the width of the client window. If there is no client window this
    // attribute must return zero.
    return page().window_size().width().value();
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-outerheight
i32 Window::outer_height() const
{
    // The outerHeight attribute must return the height of the client window. If there is no client window this
    // attribute must return zero.
    return page().window_size().height().value();
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-devicepixelratio
double Window::device_pixel_ratio() const
{
    // 1. If there is no output device, return 1 and abort these steps.
    // 2. Let CSS pixel size be the size of a CSS pixel at the current page zoom and using a scale factor of 1.0.
    // 3. Let device pixel size be the vertical size of a device pixel of the output device.
    // 4. Return the result of dividing CSS pixel size by device pixel size.
    return page().client().device_pixels_per_css_pixel();
}

// https://html.spec.whatwg.org/multipage/imagebitmap-and-animations.html#dom-animationframeprovider-requestanimationframe
i32 Window::request_animation_frame(WebIDL::CallbackType& callback)
{
    // FIXME: Make this fully spec compliant. Currently implements a mix of 'requestAnimationFrame()' and 'run the animation frame callbacks'.
    return m_animation_frame_callback_driver.add([this, callback = JS::make_handle(callback)](double now) {
        // 3. Invoke callback, passing now as the only argument, and if an exception is thrown, report the exception.
        auto result = WebIDL::invoke_callback(*callback, {}, JS::Value(now));
        if (result.is_error())
            report_exception(result, realm());
    });
}

// https://html.spec.whatwg.org/multipage/imagebitmap-and-animations.html#animationframeprovider-cancelanimationframe
void Window::cancel_animation_frame(i32 handle)
{
    // 1. If this is not supported, then throw a "NotSupportedError" DOMException.
    // NOTE: Doesn't apply in this Window-specific implementation.

    // 2. Let callbacks be this's target object's map of animation frame callbacks.
    // 3. Remove callbacks[handle].
    m_animation_frame_callback_driver.remove(handle);
}

// https://w3c.github.io/requestidlecallback/#dom-window-requestidlecallback
u32 Window::request_idle_callback(WebIDL::CallbackType& callback, RequestIdleCallback::IdleRequestOptions const& options)
{
    // 1. Let window be this Window object.

    // 2. Increment the window's idle callback identifier by one.
    m_idle_callback_identifier++;

    // 3. Let handle be the current value of window's idle callback identifier.
    auto handle = m_idle_callback_identifier;

    // 4. Push callback to the end of window's list of idle request callbacks, associated with handle.
    auto handler = [callback = JS::make_handle(callback)](JS::NonnullGCPtr<RequestIdleCallback::IdleDeadline> deadline) -> JS::Completion {
        return WebIDL::invoke_callback(*callback, {}, deadline.ptr());
    };
    m_idle_request_callbacks.append(adopt_ref(*new IdleCallback(move(handler), handle)));

    // 5. Return handle and then continue running this algorithm asynchronously.
    return handle;

    // FIXME: 6. If the timeout property is present in options and has a positive value:
    // FIXME:    1. Wait for timeout milliseconds.
    // FIXME:    2. Wait until all invocations of this algorithm, whose timeout added to their posted time occurred before this one's, have completed.
    // FIXME:    3. Optionally, wait a further user-agent defined length of time.
    // FIXME:    4. Queue a task on the queue associated with the idle-task task source, which performs the invoke idle callback timeout algorithm, passing handle and window as arguments.
    (void)options;
}

// https://w3c.github.io/requestidlecallback/#dom-window-cancelidlecallback
void Window::cancel_idle_callback(u32 handle)
{
    // 1. Let window be this Window object.

    // 2. Find the entry in either the window's list of idle request callbacks or list of runnable idle callbacks
    //    that is associated with the value handle.
    // 3. If there is such an entry, remove it from both window's list of idle request callbacks and the list of runnable idle callbacks.
    m_idle_request_callbacks.remove_first_matching([&](auto& callback) {
        return callback->handle() == handle;
    });
    m_runnable_idle_callbacks.remove_first_matching([&](auto& callback) {
        return callback->handle() == handle;
    });
}

// https://w3c.github.io/selection-api/#dom-window-getselection
JS::GCPtr<Selection::Selection> Window::get_selection() const
{
    // The method must invoke and return the result of getSelection() on this's Window.document attribute.
    return associated_document().get_selection();
}

// https://w3c.github.io/webcrypto/#dom-windoworworkerglobalscope-crypto
JS::NonnullGCPtr<Crypto::Crypto> Window::crypto()
{
    auto& realm = this->realm();

    if (!m_crypto)
        m_crypto = heap().allocate<Crypto::Crypto>(realm, realm);
    return JS::NonnullGCPtr { *m_crypto };
}

// https://html.spec.whatwg.org/multipage/obsolete.html#dom-window-captureevents
void Window::capture_events()
{
    // Do nothing.
}

// https://html.spec.whatwg.org/multipage/obsolete.html#dom-document-releaseevents
void Window::release_events()
{
    // Do nothing.
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigation
JS::NonnullGCPtr<Navigation> Window::navigation()
{
    // Upon creation of the Window object, its navigation API must be set
    // to a new Navigation object created in the Window object's relevant realm.
    if (!m_navigation) {
        auto& realm = relevant_realm(*this);
        m_navigation = heap().allocate<Navigation>(realm, realm);
    }

    // The navigation getter steps are to return this's navigation API.
    return *m_navigation;
}

// https://html.spec.whatwg.org/multipage/custom-elements.html#dom-window-customelements
JS::NonnullGCPtr<CustomElementRegistry> Window::custom_elements()
{
    auto& realm = this->realm();

    // The customElements attribute of the Window interface must return the CustomElementRegistry object for that Window object.
    if (!m_custom_element_registry)
        m_custom_element_registry = heap().allocate<CustomElementRegistry>(realm, realm);
    return JS::NonnullGCPtr { *m_custom_element_registry };
}

// https://html.spec.whatwg.org/#document-tree-child-navigable-target-name-property-set
OrderedHashMap<FlyString, JS::NonnullGCPtr<Navigable>> Window::document_tree_child_navigable_target_name_property_set()
{
    // The document-tree child navigable target name property set of a Window object window is the return value of running these steps:

    // 1. Let children be the document-tree child navigables of window's associated Document.
    auto children = associated_document().document_tree_child_navigables();

    // 2. Let firstNamedChildren be an empty ordered set.
    OrderedHashMap<FlyString, JS::NonnullGCPtr<Navigable>> first_named_children;

    // 3. For each navigable of children:
    for (auto const& navigable : children) {
        // 1. Let name be navigable's target name.
        auto const& name = navigable->target_name();

        // 2. If name is the empty string, then continue.
        if (name.is_empty())
            continue;

        // 3. If firstNamedChildren contains a navigable whose target name is name, then continue.
        if (first_named_children.contains(name))
            continue;

        // 4. Append navigable to firstNamedChildren.
        (void)first_named_children.set(name, *navigable);
    }

    // 4. Let names be an empty ordered set.
    OrderedHashMap<FlyString, JS::NonnullGCPtr<Navigable>> names;

    // 5. For each navigable of firstNamedChildren:
    for (auto const& [name, navigable] : first_named_children) {
        // 1. Let name be navigable's target name.
        // 2. If navigable's active document's origin is same origin with window's relevant settings object's origin, then append name to names.
        if (navigable->active_document()->origin().is_same_origin(relevant_settings_object(*this).origin()))
            names.set(name, *navigable);
    }

    return names;
}

// https://html.spec.whatwg.org/#named-access-on-the-window-object
Vector<FlyString> Window::supported_property_names() const
{
    // FIXME: Make the const-correctness of the methods this method calls less cowboy.
    auto& mutable_this = const_cast<Window&>(*this);

    // The Window object supports named properties.
    // The supported property names of a Window object window at any moment consist of the following,
    // in tree order according to the element that contributed them, ignoring later duplicates:

    HashTable<FlyString> property_names;

    // - window's document-tree child navigable target name property set;
    auto child_navigable_property_set = mutable_this.document_tree_child_navigable_target_name_property_set();
    for (auto& entry : child_navigable_property_set)
        property_names.set(entry.key, AK::HashSetExistingEntryBehavior::Keep);

    // - the value of the name content attribute for all embed, form, img, and object elements
    //   that have a non-empty name content attribute and are in a document tree with window's associated Document as their root; and
    // - the value of the id content attribute for all HTML elements that have a non-empty id content attribute
    //   and are in a document tree with window's associated Document as their root.
    associated_document().for_each_in_subtree_of_type<DOM::Element>([&property_names](auto& element) -> TraversalDecision {
        if (is<HTMLEmbedElement>(element) || is<HTMLFormElement>(element) || is<HTMLImageElement>(element) || is<HTMLObjectElement>(element)) {
            if (element.name().has_value())
                property_names.set(element.name().value(), AK::HashSetExistingEntryBehavior::Keep);
        }
        if (auto const& name = element.id(); name.has_value())
            property_names.set(name.value().to_string(), AK::HashSetExistingEntryBehavior::Keep);
        return TraversalDecision::Continue;
    });

    return property_names.values();
}

// https://html.spec.whatwg.org/#named-access-on-the-window-object
WebIDL::ExceptionOr<JS::Value> Window::named_item_value(FlyString const& name) const
{
    // FIXME: Make the const-correctness of the methods this method calls less cowboy.
    auto& mutable_this = const_cast<Window&>(*this);

    // To determine the value of a named property name in a Window object window, the user agent must return the value obtained using the following steps:

    // 1. Let objects be the list of named objects of window with the name name.
    // NOTE: There will be at least one such object, since the algorithm would otherwise not have been invoked by Web IDL.
    auto objects = mutable_this.named_objects(name);

    // 2. If objects contains a navigable, then:
    if (!objects.navigables.is_empty()) {
        // 1. Let container be the first navigable container in window's associated Document's descendants whose content navigable is in objects.
        JS::GCPtr<NavigableContainer> container = nullptr;
        mutable_this.associated_document().for_each_in_subtree_of_type<HTML::NavigableContainer>([&](HTML::NavigableContainer& navigable_container) {
            if (!navigable_container.content_navigable())
                return TraversalDecision::Continue;
            if (objects.navigables.contains_slow(JS::NonnullGCPtr { *navigable_container.content_navigable() })) {
                container = navigable_container;
                return TraversalDecision::Break;
            }
            return TraversalDecision::Continue;
        });
        // 2. Return container's content navigable's active WindowProxy.
        VERIFY(container);
        return container->content_navigable()->active_window_proxy();
    }

    // 3. Otherwise, if objects has only one element, return that element.
    if (objects.elements.size() == 1)
        return objects.elements[0];

    // 4. Otherwise return an HTMLCollection rooted at window's associated Document,
    //    whose filter matches only named objects of window with the name name. (By definition, these will all be elements.)
    return DOM::HTMLCollection::create(mutable_this.associated_document(), DOM::HTMLCollection::Scope::Descendants, [name](auto& element) -> bool {
        if ((is<HTMLEmbedElement>(element) || is<HTMLFormElement>(element) || is<HTMLImageElement>(element) || is<HTMLObjectElement>(element))
            && (element.name() == name))
            return true;
        return element.id() == name;
    });
}

// https://html.spec.whatwg.org/#dom-window-nameditem-filter
Window::NamedObjects Window::named_objects(StringView name)
{
    // NOTE: Since the Window interface has the [Global] extended attribute, its named properties
    //       follow the rules for named properties objects rather than legacy platform objects.

    // Named objects of Window object window with the name name, for the purposes of the above algorithm, consist of the following:
    NamedObjects objects;

    // document-tree child navigables of window's associated Document whose target name is name;
    auto children = associated_document().document_tree_child_navigables();
    for (auto& navigable : children) {
        if (navigable->target_name() == name) {
            objects.navigables.append(*navigable);
        }
    }

    // embed, form, img, or object elements that have a name content attribute whose value is name
    // and are in a document tree with window's associated Document as their root; and
    // HTML elements that have an id content attribute whose value is name and are in a document tree with window's associated Document as their root.
    associated_document().for_each_in_subtree_of_type<DOM::Element>([&objects, &name](auto& element) -> TraversalDecision {
        if ((is<HTMLEmbedElement>(element) || is<HTMLFormElement>(element) || is<HTMLImageElement>(element) || is<HTMLObjectElement>(element))
            && (element.name() == name))
            objects.elements.append(element);
        else if (element.id() == name)
            objects.elements.append(element);
        return TraversalDecision::Continue;
    });

    return objects;
}

}
