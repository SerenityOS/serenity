/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/DeprecatedString.h>
#include <AK/GenericLexer.h>
#include <AK/Utf8View.h>
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
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Requests.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/CustomElements/CustomElementRegistry.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/Focus.h>
#include <LibWeb/HTML/Location.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/Navigation.h>
#include <LibWeb/HTML/Navigator.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/PageTransitionEvent.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Scripting/ExceptionReporter.h>
#include <LibWeb/HTML/Storage.h>
#include <LibWeb/HTML/TokenizedFeatures.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/HTML/WindowProxy.h>
#include <LibWeb/HighResolutionTime/Performance.h>
#include <LibWeb/HighResolutionTime/TimeOrigin.h>
#include <LibWeb/Infra/Base64.h>
#include <LibWeb/Infra/CharacterTypes.h>
#include <LibWeb/Internals/Internals.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/RequestIdleCallback/IdleDeadline.h>
#include <LibWeb/Selection/Selection.h>
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
    return realm.heap().allocate<Window>(realm, realm);
}

Window::Window(JS::Realm& realm)
    : DOM::EventTarget(realm)
{
}

void Window::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    WindowOrWorkerGlobalScopeMixin::visit_edges(visitor);

    visitor.visit(m_associated_document.ptr());
    visitor.visit(m_current_event.ptr());
    visitor.visit(m_performance.ptr());
    visitor.visit(m_screen.ptr());
    visitor.visit(m_location);
    visitor.visit(m_crypto);
    visitor.visit(m_navigator);
    visitor.visit(m_navigation);
    visitor.visit(m_custom_element_registry);
    for (auto& plugin_object : m_pdf_viewer_plugin_objects)
        visitor.visit(plugin_object);
    for (auto& mime_type_object : m_pdf_viewer_mime_type_objects)
        visitor.visit(mime_type_object);
    visitor.visit(m_count_queuing_strategy_size_function);
    visitor.visit(m_byte_length_queuing_strategy_size_function);
}

Window::~Window() = default;

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#normalizing-the-feature-name
static StringView normalize_feature_name(StringView name)
{
    // For legacy reasons, there are some aliases of some feature names. To normalize a feature name name, switch on name:

    // "screenx"
    if (name == "screenx"sv) {
        // Return "left".
        return "left"sv;
    }
    // "screeny"
    else if (name == "screeny"sv) {
        // Return "top".
        return "top"sv;
    }
    // "innerwidth"
    else if (name == "innerwidth"sv) {
        // Return "width".
        return "width"sv;
    }
    // "innerheight"
    else if (name == "innerheight") {
        // Return "height".
        return "height"sv;
    }
    // Anything else
    else {
        // Return name.
        return name;
    }
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#concept-window-open-features-tokenize
static OrderedHashMap<DeprecatedString, DeprecatedString> tokenize_open_features(StringView features)
{
    // 1. Let tokenizedFeatures be a new ordered map.
    OrderedHashMap<DeprecatedString, DeprecatedString> tokenized_features;

    // 2. Let position point at the first code point of features.
    GenericLexer lexer(features);

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#feature-separator
    auto is_feature_separator = [](auto character) {
        return Infra::is_ascii_whitespace(character) || character == '=' || character == ',';
    };

    // 3. While position is not past the end of features:
    while (!lexer.is_eof()) {
        // 1. Let name be the empty string.
        DeprecatedString name;

        // 2. Let value be the empty string.
        DeprecatedString value;

        // 3. Collect a sequence of code points that are feature separators from features given position. This skips past leading separators before the name.
        lexer.ignore_while(is_feature_separator);

        // 4. Collect a sequence of code points that are not feature separators from features given position. Set name to the collected characters, converted to ASCII lowercase.
        name = lexer.consume_until(is_feature_separator).to_lowercase_string();

        // 5. Set name to the result of normalizing the feature name name.
        name = normalize_feature_name(name);

        // 6. While position is not past the end of features and the code point at position in features is not U+003D (=):
        //    1. If the code point at position in features is U+002C (,), or if it is not a feature separator, then break.
        //    2. Advance position by 1.
        lexer.ignore_while(Infra::is_ascii_whitespace);

        // 7. If the code point at position in features is a feature separator:
        //    1. While position is not past the end of features and the code point at position in features is a feature separator:
        //       1. If the code point at position in features is U+002C (,), then break.
        //       2. Advance position by 1.
        lexer.ignore_while([](auto character) { return Infra::is_ascii_whitespace(character) || character == '='; });

        // 2. Collect a sequence of code points that are not feature separators code points from features given position. Set value to the collected code points, converted to ASCII lowercase.
        value = lexer.consume_until(is_feature_separator).to_lowercase_string();

        // 8. If name is not the empty string, then set tokenizedFeatures[name] to value.
        if (!name.is_empty())
            tokenized_features.set(move(name), move(value));
    }

    // 4. Return tokenizedFeatures.
    return tokenized_features;
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#concept-window-open-features-parse-boolean
template<Enum T>
static T parse_boolean_feature(StringView value)
{
    // 1. If value is the empty string, then return true.
    if (value.is_empty())
        return T::Yes;

    // 2. If value is "yes", then return true.
    if (value == "yes"sv)
        return T::Yes;

    // 3. If value is "true", then return true.
    if (value == "true"sv)
        return T::Yes;

    // 4. Let parsed be the result of parsing value as an integer.
    auto parsed = value.to_int<i64>();

    // 5. If parsed is an error, then set it to 0.
    if (!parsed.has_value())
        parsed = 0;

    // 6. Return false if parsed is 0, and true otherwise.
    return parsed == 0 ? T::No : T::Yes;
}

//  https://html.spec.whatwg.org/multipage/window-object.html#popup-window-is-requested
static TokenizedFeature::Popup check_if_a_popup_window_is_requested(OrderedHashMap<DeprecatedString, DeprecatedString> const& tokenized_features)
{
    // 1. If tokenizedFeatures is empty, then return false.
    if (tokenized_features.is_empty())
        return TokenizedFeature::Popup::No;

    // 2. If tokenizedFeatures["popup"] exists, then return the result of parsing tokenizedFeatures["popup"] as a boolean feature.
    if (auto popup_feature = tokenized_features.get("popup"sv); popup_feature.has_value())
        return parse_boolean_feature<TokenizedFeature::Popup>(*popup_feature);

    // https://html.spec.whatwg.org/multipage/window-object.html#window-feature-is-set
    auto check_if_a_window_feature_is_set = [&]<Enum T>(StringView feature_name, T default_value) {
        // 1. If tokenizedFeatures[featureName] exists, then return the result of parsing tokenizedFeatures[featureName] as a boolean feature.
        if (auto feature = tokenized_features.get(feature_name); feature.has_value())
            return parse_boolean_feature<T>(*feature);

        // 2. Return defaultValue.
        return default_value;
    };

    // 3. Let location be the result of checking if a window feature is set, given tokenizedFeatures, "location", and false.
    auto location = check_if_a_window_feature_is_set("location"sv, TokenizedFeature::Location::No);

    // 4. Let toolbar be the result of checking if a window feature is set, given tokenizedFeatures, "toolbar", and false.
    auto toolbar = check_if_a_window_feature_is_set("toolbar"sv, TokenizedFeature::Toolbar::No);

    // 5. If location and toolbar are both false, then return true.
    if (location == TokenizedFeature::Location::No && toolbar == TokenizedFeature::Toolbar::No)
        return TokenizedFeature::Popup::Yes;

    // 6. Let menubar be the result of checking if a window feature is set, given tokenizedFeatures, menubar", and false.
    auto menubar = check_if_a_window_feature_is_set("menubar"sv, TokenizedFeature::Menubar::No);

    // 7. If menubar is false, then return true.
    if (menubar == TokenizedFeature::Menubar::No)
        return TokenizedFeature::Popup::Yes;

    // 8. Let resizable be the result of checking if a window feature is set, given tokenizedFeatures, "resizable", and true.
    auto resizable = check_if_a_window_feature_is_set("resizable"sv, TokenizedFeature::Resizable::Yes);

    // 9. If resizable is false, then return true.
    if (resizable == TokenizedFeature::Resizable::No)
        return TokenizedFeature::Popup::Yes;

    // 10. Let scrollbars be the result of checking if a window feature is set, given tokenizedFeatures, "scrollbars", and false.
    auto scrollbars = check_if_a_window_feature_is_set("scrollbars"sv, TokenizedFeature::Scrollbars::No);

    // 11. If scrollbars is false, then return true.
    if (scrollbars == TokenizedFeature::Scrollbars::No)
        return TokenizedFeature::Popup::Yes;

    // 12. Let status be the result of checking if a window feature is set, given tokenizedFeatures, "status", and false.
    auto status = check_if_a_window_feature_is_set("status"sv, TokenizedFeature::Status::No);

    // 13. If status is false, then return true.
    if (status == TokenizedFeature::Status::No)
        return TokenizedFeature::Popup::Yes;

    // 14. Return false.
    return TokenizedFeature::Popup::No;
}

// FIXME: This is based on the old 'browsing context' concept, which was replaced with 'navigable'
// https://html.spec.whatwg.org/multipage/window-object.html#window-open-steps
WebIDL::ExceptionOr<JS::GCPtr<WindowProxy>> Window::open_impl(StringView url, StringView target, StringView features)
{
    auto& vm = this->vm();

    // 1. If the event loop's termination nesting level is nonzero, return null.
    if (main_thread_event_loop().termination_nesting_level() != 0)
        return nullptr;

    // 2. Let source browsing context be the entry global object's browsing context.
    auto* source_browsing_context = verify_cast<Window>(entry_global_object()).browsing_context();

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

    // 8. If noreferrer is true, then set noopener to true.
    if (no_referrer == TokenizedFeature::NoReferrer::Yes)
        no_opener = TokenizedFeature::NoOpener::Yes;

    // 9. Let target browsing context and windowType be the result of applying the rules for choosing a browsing context given target, source browsing context, and noopener.
    auto [target_browsing_context, window_type] = source_browsing_context->choose_a_browsing_context(target, no_opener);

    // 10. If target browsing context is null, then return null.
    if (target_browsing_context == nullptr)
        return nullptr;

    // 11. If windowType is either "new and unrestricted" or "new with no opener", then:
    if (window_type == BrowsingContext::WindowType::NewAndUnrestricted || window_type == BrowsingContext::WindowType::NewWithNoOpener) {
        // 1. Set the target browsing context's is popup to the result of checking if a popup window is requested, given tokenizedFeatures.
        target_browsing_context->set_is_popup(check_if_a_popup_window_is_requested(tokenized_features));

        // FIXME: 2. Set up browsing context features for target browsing context given tokenizedFeatures. [CSSOMVIEW]
        // NOTE: While this is not implemented yet, all of observable actions taken by this operation are optional (implementation-defined).

        // 3. Let urlRecord be the URL record about:blank.
        auto url_record = AK::URL("about:blank"sv);

        // 4. If url is not the empty string, then parse url relative to the entry settings object, and set urlRecord to the resulting URL record, if any. If the parse a URL algorithm failed, then throw a "SyntaxError" DOMException.
        if (!url.is_empty()) {
            url_record = entry_settings_object().parse_url(url);
            if (!url_record.is_valid())
                return WebIDL::SyntaxError::create(realm(), "URL is not valid");
        }

        // FIXME: 5. If urlRecord matches about:blank, then perform the URL and history update steps given target browsing context's active document and urlRecord.

        // 6. Otherwise:
        else {
            // 1. Let request be a new request whose URL is urlRecord.
            auto request = Fetch::Infrastructure::Request::create(vm);
            request->set_url(url_record);

            // 2. If noreferrer is true, then set request's referrer to "no-referrer".
            if (no_referrer == TokenizedFeature::NoReferrer::Yes)
                request->set_referrer(Fetch::Infrastructure::Request::Referrer::NoReferrer);

            // 3. Navigate target browsing context to request, with exceptionsEnabled set to true and the source browsing context set to source browsing context.
            TRY(target_browsing_context->navigate(request, *source_browsing_context, true));
        }
    }

    // 12. Otherwise:
    else {
        // 1. If url is not the empty string, then:
        if (!url.is_empty()) {
            // 1. Let urlRecord be the URL record about:blank.
            auto url_record = AK::URL("about:blank"sv);

            // 2. Parse url relative to the entry settings object, and set urlRecord to the resulting URL record, if any. If the parse a URL algorithm failed, then throw a "SyntaxError" DOMException.
            url_record = entry_settings_object().parse_url(url);
            if (!url_record.is_valid())
                return WebIDL::SyntaxError::create(realm(), "URL is not valid");

            // 3. Let request be a new request whose URL is urlRecord.
            auto request = Fetch::Infrastructure::Request::create(vm);
            request->set_url(url_record);

            // 4. If noreferrer is true, then set request's referrer to "noreferrer".
            if (no_referrer == TokenizedFeature::NoReferrer::Yes)
                request->set_referrer(Fetch::Infrastructure::Request::Referrer::NoReferrer);

            // 5. Navigate target browsing context to request, with exceptionsEnabled set to true and the source browsing context set to source browsing context.
            TRY(target_browsing_context->navigate(request, *source_browsing_context, true));
        }

        // 2. If noopener is false, then set target browsing context's opener browsing context to source browsing context.
        if (no_opener == TokenizedFeature::NoOpener::No)
            target_browsing_context->set_opener_browsing_context(source_browsing_context);
    }

    // 13. If noopener is true or windowType is "new with no opener", then return null.
    if (no_opener == TokenizedFeature::NoOpener::Yes || window_type == BrowsingContext::WindowType::NewWithNoOpener)
        return nullptr;

    // 14. Return target browsing context's WindowProxy object.
    return target_browsing_context->window_proxy();
}

void Window::did_set_location_href(Badge<Location>, AK::URL const& new_href)
{
    auto* browsing_context = associated_document().browsing_context();
    if (!browsing_context)
        return;
    browsing_context->loader().load(new_href, FrameLoader::Type::Navigation);
}

void Window::did_call_location_reload(Badge<Location>)
{
    auto* browsing_context = associated_document().browsing_context();
    if (!browsing_context)
        return;
    browsing_context->loader().load(associated_document().url(), FrameLoader::Type::Reload);
}

void Window::did_call_location_replace(Badge<Location>, DeprecatedString url)
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

Page* Window::page()
{
    return associated_document().page();
}

Page const* Window::page() const
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
        if (auto* page = this->page()) {
            return CSS::MediaFeatureValue(CSS::Length::make_px(page->web_exposed_screen_area().height().to_double()));
        }
        return CSS::MediaFeatureValue(0);
    case CSS::MediaFeatureID::DeviceWidth:
        if (auto* page = this->page()) {
            return CSS::MediaFeatureValue(CSS::Length::make_px(page->web_exposed_screen_area().width().to_double()));
        }
        return CSS::MediaFeatureValue(0);
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

// https://html.spec.whatwg.org/multipage/interaction.html#transient-activation
bool Window::has_transient_activation() const
{
    // The transient activation duration is expected be at most a few seconds, so that the user can possibly
    // perceive the link between an interaction with the page and the page calling the activation-gated API.
    auto transient_activation_duration = 5;

    // When the current high resolution time given W
    auto unsafe_shared_time = HighResolutionTime::unsafe_shared_current_time();
    auto current_time = HighResolutionTime::relative_high_resolution_time(unsafe_shared_time, realm().global_object());

    // is greater than or equal to the last activation timestamp in W
    if (current_time >= m_last_activation_timestamp) {
        // and less than the last activation timestamp in W plus the transient activation duration
        if (current_time < m_last_activation_timestamp + transient_activation_duration) {
            // then W is said to have transient activation.
            return true;
        }
    }

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
    queue_global_task(Task::Source::IdleTask, *this, [this] {
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
            report_exception(result, realm());
        // 4. If window's list of runnable idle callbacks is not empty, queue a task which performs the steps
        //    in the invoke idle callbacks algorithm with getDeadline and window as a parameters and return from this algorithm
        queue_global_task(Task::Source::IdleTask, *this, [this] {
            invoke_idle_callbacks();
        });
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
    VERIFY(page());
    if (!page()->pdf_viewer_supported())
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
    VERIFY(page());
    if (!page()->pdf_viewer_supported())
        return {};

    if (m_pdf_viewer_mime_type_objects.is_empty()) {
        m_pdf_viewer_mime_type_objects.append(realm().heap().allocate<MimeType>(realm(), realm(), "application/pdf"_string));
        m_pdf_viewer_mime_type_objects.append(realm().heap().allocate<MimeType>(realm(), realm(), "text/pdf"_string));
    }

    return m_pdf_viewer_mime_type_objects;
}

// https://streams.spec.whatwg.org/#count-queuing-strategy-size-function
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::CallbackType>> Window::count_queuing_strategy_size_function()
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
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::CallbackType>> Window::byte_length_queuing_strategy_size_function()
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

static bool s_internals_object_exposed = false;

void Window::set_internals_object_exposed(bool exposed)
{
    s_internals_object_exposed = exposed;
}

WebIDL::ExceptionOr<void> Window::initialize_web_interfaces(Badge<WindowEnvironmentSettingsObject>)
{
    auto& realm = this->realm();
    add_window_exposed_interfaces(*this);

    Object::set_prototype(&Bindings::ensure_web_prototype<Bindings::WindowPrototype>(realm, "Window"));

    Bindings::WindowGlobalMixin::initialize(realm, *this);
    WindowOrWorkerGlobalScopeMixin::initialize(realm);

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
    if (!browsing_context())
        return String {};

    // 2. Return this's navigable's target name.
    return browsing_context()->name();
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#apis-for-creating-and-navigating-browsing-contexts-by-name:dom-name
void Window::set_name(String const& name)
{
    // 1. If this's navigable is null, then return.
    if (!browsing_context())
        return;

    // 2. Set this's navigable's active session history entry's document state's navigable target name to the given value.
    browsing_context()->set_name(name);
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
    auto* current = browsing_context();

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

// https://html.spec.whatwg.org/multipage/window-object.html#dom-frames
JS::NonnullGCPtr<WindowProxy> Window::frames() const
{
    // The window, frames, and self getter steps are to return this's relevant realm.[[GlobalEnv]].[[GlobalThisValue]].
    return verify_cast<WindowProxy>(relevant_realm(*this).global_environment().global_this_value());
}

// https://html.spec.whatwg.org/multipage/window-object.html#dom-length
u32 Window::length() const
{
    // The length getter steps are to return this's associated Document's document-tree child navigables's size.
    return static_cast<u32>(document_tree_child_browsing_context_count());
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-top
JS::GCPtr<WindowProxy const> Window::top() const
{
    // 1. If this's navigable is null, then return null.
    auto const* browsing_context = this->browsing_context();
    if (!browsing_context)
        return {};

    // 2. Return this's navigable's top-level traversable's active WindowProxy.
    return browsing_context->top_level_browsing_context().window_proxy();
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-parent
JS::GCPtr<WindowProxy const> Window::parent() const
{
    // 1. Let navigable be this's navigable.
    auto* navigable = browsing_context();

    // 2. If navigable is null, then return null.
    if (!navigable)
        return {};

    // 3. If navigable's parent is not null, then set navigable to navigable's parent.
    if (auto parent = navigable->parent())
        navigable = parent;

    // 4. Return navigable's active WindowProxy.
    return navigable->window_proxy();
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-frameelement
JS::GCPtr<DOM::Element const> Window::frame_element() const
{
    // 1. Let current be this's node navigable.
    auto* current = browsing_context();

    // 2. If current is null, then return null.
    if (!current)
        return {};

    // 3. Let container be current's container.
    auto* container = current->container();

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

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-alert
void Window::alert(String const& message)
{
    // https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#simple-dialogs
    // Note: This method is defined using two overloads, instead of using an optional argument,
    //       for historical reasons. The practical impact of this is that alert(undefined) is
    //       treated as alert("undefined"), but alert() is treated as alert("").
    // FIXME: Make this fully spec compliant.
    if (auto* page = this->page())
        page->did_request_alert(message);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-confirm
bool Window::confirm(Optional<String> const& message)
{
    // FIXME: Make this fully spec compliant.
    // NOTE: `message` has an IDL-provided default value and is never empty.
    if (auto* page = this->page())
        return page->did_request_confirm(*message);
    return false;
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-prompt
Optional<String> Window::prompt(Optional<String> const& message, Optional<String> const& default_)
{
    // FIXME: Make this fully spec compliant.
    if (auto* page = this->page())
        return page->did_request_prompt(*message, *default_);
    return {};
}

// https://html.spec.whatwg.org/multipage/web-messaging.html#dom-window-postmessage
void Window::post_message(JS::Value message, String const&)
{
    // FIXME: This is an ad-hoc hack implementation instead, since we don't currently
    //        have serialization and deserialization of messages.
    queue_global_task(Task::Source::PostedMessage, *this, [this, message] {
        MessageEventInit event_init {};
        event_init.data = message;
        event_init.origin = "<origin>"_string;
        dispatch_event(MessageEvent::create(realm(), EventNames::message, event_init));
    });
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
    if (auto const* browsing_context = associated_document().browsing_context())
        return browsing_context->viewport_rect().width().to_int();
    return 0;
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-innerheight
i32 Window::inner_height() const
{
    // The innerHeight attribute must return the viewport height including the size of a rendered scroll bar (if any),
    // or zero if there is no viewport.
    if (auto const* browsing_context = associated_document().browsing_context())
        return browsing_context->viewport_rect().height().to_int();
    return 0;
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-scrollx
double Window::scroll_x() const
{
    // The scrollX attribute must return the x-coordinate, relative to the initial containing block origin,
    // of the left of the viewport, or zero if there is no viewport.
    if (auto* page = this->page())
        return page->top_level_browsing_context().viewport_scroll_offset().x().to_double();
    return 0;
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-scrolly
double Window::scroll_y() const
{
    // The scrollY attribute must return the y-coordinate, relative to the initial containing block origin,
    // of the top of the viewport, or zero if there is no viewport.
    if (auto* page = this->page())
        return page->top_level_browsing_context().viewport_scroll_offset().y().to_double();
    return 0;
}

// https://w3c.github.io/csswg-drafts/cssom-view/#perform-a-scroll
static void perform_a_scroll(Page& page, double x, double y, JS::GCPtr<DOM::Node const> element, Bindings::ScrollBehavior behavior)
{
    // FIXME: 1. Abort any ongoing smooth scroll for box.
    // 2. If the user agent honors the scroll-behavior property and one of the following are true:
    // - behavior is "auto" and element is not null and its computed value of the scroll-behavior property is smooth
    // - behavior is smooth
    // ...then perform a smooth scroll of box to position. Once the position has finished updating, emit the scrollend
    // event. Otherwise, perform an instant scroll of box to position. After an instant scroll emit the scrollend event.
    // FIXME: Support smooth scrolling.
    (void)element;
    (void)behavior;
    page.client().page_did_request_scroll_to({ x, y });
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-scroll
void Window::scroll(ScrollToOptions const& options)
{
    // 4. If there is no viewport, abort these steps.
    auto* page = this->page();
    if (!page)
        return;
    auto const& top_level_browsing_context = page->top_level_browsing_context();

    // 1. If invoked with one argument, follow these substeps:

    // 1. Let options be the argument.
    auto viewport_rect = top_level_browsing_context.viewport_rect().to_type<float>();

    // 2. Let x be the value of the left dictionary member of options, if present, or the viewport’s current scroll
    //    position on the x axis otherwise.
    auto x = options.left.value_or(viewport_rect.x());

    // 3. Let y be the value of the top dictionary member of options, if present, or the viewport’s current scroll
    //    position on the y axis otherwise.
    auto y = options.top.value_or(viewport_rect.y());

    // 3. Normalize non-finite values for x and y.
    x = JS::Value(x).is_finite_number() ? x : 0;
    y = JS::Value(y).is_finite_number() ? y : 0;

    // 5. Let viewport width be the width of the viewport excluding the width of the scroll bar, if any.
    auto viewport_width = viewport_rect.width();

    // 6. Let viewport height be the height of the viewport excluding the height of the scroll bar, if any.
    auto viewport_height = viewport_rect.height();

    (void)viewport_width;
    (void)viewport_height;

    // FIXME: 7.
    // -> If the viewport has rightward overflow direction
    //    Let x be max(0, min(x, viewport scrolling area width - viewport width)).
    // -> If the viewport has leftward overflow direction
    //    Let x be min(0, max(x, viewport width - viewport scrolling area width)).

    // FIXME: 8.
    // -> If the viewport has downward overflow direction
    //    Let y be max(0, min(y, viewport scrolling area height - viewport height)).
    // -> If the viewport has upward overflow direction
    //    Let y be min(0, max(y, viewport height - viewport scrolling area height)).

    // FIXME: 9. Let position be the scroll position the viewport would have by aligning the x-coordinate x of the viewport
    //           scrolling area with the left of the viewport and aligning the y-coordinate y of the viewport scrolling area
    //           with the top of the viewport.

    // FIXME: 10. If position is the same as the viewport’s current scroll position, and the viewport does not have an ongoing
    //            smooth scroll, abort these steps.

    // 11. Let document be the viewport’s associated Document.
    auto const* document = top_level_browsing_context.active_document();

    // 12. Perform a scroll of the viewport to position, document’s root element as the associated element, if there is
    //     one, or null otherwise, and the scroll behavior being the value of the behavior dictionary member of options.
    auto element = JS::GCPtr<DOM::Node const> { document ? &document->root() : nullptr };
    perform_a_scroll(*page, x, y, element, options.behavior);
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
    auto x = options.left.value_or(0);
    auto y = options.top.value_or(0);
    x = JS::Value(x).is_finite_number() ? x : 0;
    y = JS::Value(y).is_finite_number() ? y : 0;

    // 3. Add the value of scrollX to the left dictionary member.
    options.left = x + scroll_x();

    // 4. Add the value of scrollY to the top dictionary member.
    options.top = y + scroll_y();

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
    if (auto* page = this->page())
        return page->window_position().x().value();
    return 0;
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-screeny
i32 Window::screen_y() const
{
    // The screenY and screenTop attributes must return the y-coordinate, relative to the origin of the screen of the
    // Web-exposed screen area, of the top of the client window as number of CSS pixels, or zero if there is no such thing.
    if (auto* page = this->page())
        return page->window_position().y().value();
    return 0;
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-outerwidth
i32 Window::outer_width() const
{
    // The outerWidth attribute must return the width of the client window. If there is no client window this
    // attribute must return zero.
    if (auto* page = this->page())
        return page->window_size().width().value();
    return 0;
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-outerheight
i32 Window::outer_height() const
{
    // The outerHeight attribute must return the height of the client window. If there is no client window this
    // attribute must return zero.
    if (auto* page = this->page())
        return page->window_size().height().value();
    return 0;
}

// https://w3c.github.io/csswg-drafts/cssom-view/#dom-window-devicepixelratio
double Window::device_pixel_ratio() const
{
    // 1. If there is no output device, return 1 and abort these steps.
    // 2. Let CSS pixel size be the size of a CSS pixel at the current page zoom and using a scale factor of 1.0.
    // 3. Let device pixel size be the vertical size of a device pixel of the output device.
    // 4. Return the result of dividing CSS pixel size by device pixel size.
    if (auto* page = this->page())
        return page->client().device_pixels_per_css_pixel();
    return 1;
}

// https://html.spec.whatwg.org/multipage/imagebitmap-and-animations.html#dom-animationframeprovider-requestanimationframe
i32 Window::request_animation_frame(WebIDL::CallbackType& callback)
{
    // FIXME: Make this fully spec compliant. Currently implements a mix of 'requestAnimationFrame()' and 'run the animation frame callbacks'.
    auto now = HighResolutionTime::unsafe_shared_current_time();
    return m_animation_frame_callback_driver.add([this, now, callback = JS::make_handle(callback)](auto) {
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

// https://w3c.github.io/hr-time/#dom-windoworworkerglobalscope-performance
JS::NonnullGCPtr<HighResolutionTime::Performance> Window::performance()
{
    if (!m_performance)
        m_performance = heap().allocate<HighResolutionTime::Performance>(realm(), *this);
    return JS::NonnullGCPtr { *m_performance };
}

// https://w3c.github.io/webcrypto/#dom-windoworworkerglobalscope-crypto
JS::NonnullGCPtr<Crypto::Crypto> Window::crypto()
{
    auto& realm = this->realm();

    if (!m_crypto)
        m_crypto = heap().allocate<Crypto::Crypto>(realm, realm);
    return JS::NonnullGCPtr { *m_crypto };
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

// https://html.spec.whatwg.org/multipage/window-object.html#number-of-document-tree-child-browsing-contexts
size_t Window::document_tree_child_browsing_context_count() const
{
    // 1. If W's browsing context is null, then return 0.
    auto* this_browsing_context = associated_document().browsing_context();
    if (!this_browsing_context)
        return 0;

    // 2. Return the number of document-tree child browsing contexts of W's browsing context.
    return this_browsing_context->document_tree_child_browsing_context_count();
}

}
