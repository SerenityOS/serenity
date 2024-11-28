/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022-2024, Tim Flynn <trflynn89@ladybird.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/Time.h>
#include <AK/Vector.h>
#include <LibCore/File.h>
#include <LibJS/Runtime/JSONObject.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/CSS/CSSStyleValue.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/Crypto/Crypto.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentObserver.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/NodeFilter.h>
#include <LibWeb/DOM/NodeIterator.h>
#include <LibWeb/DOM/Position.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/Geometry/DOMRect.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/Focus.h>
#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLDataListElement.h>
#include <LibWeb/HTML/HTMLFrameElement.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/HTMLOptGroupElement.h>
#include <LibWeb/HTML/HTMLOptionElement.h>
#include <LibWeb/HTML/HTMLSelectElement.h>
#include <LibWeb/HTML/HTMLTextAreaElement.h>
#include <LibWeb/HTML/NavigationObserver.h>
#include <LibWeb/HTML/Scripting/TemporaryExecutionContext.h>
#include <LibWeb/HTML/SelectedFile.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/HTML/WindowProxy.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Platform/EventLoopPlugin.h>
#include <LibWeb/Platform/Timer.h>
#include <LibWeb/UIEvents/EventNames.h>
#include <LibWeb/UIEvents/MouseEvent.h>
#include <LibWeb/WebDriver/Actions.h>
#include <LibWeb/WebDriver/ElementReference.h>
#include <LibWeb/WebDriver/ExecuteScript.h>
#include <LibWeb/WebDriver/HeapTimer.h>
#include <LibWeb/WebDriver/InputState.h>
#include <LibWeb/WebDriver/Properties.h>
#include <LibWeb/WebDriver/Screenshot.h>
#include <WebContent/WebDriverConnection.h>

namespace WebContent {

// https://w3c.github.io/webdriver/#dfn-serialized-cookie
static JsonValue serialize_cookie(Web::Cookie::Cookie const& cookie)
{
    JsonObject serialized_cookie;
    serialized_cookie.set("name"sv, cookie.name.to_byte_string());
    serialized_cookie.set("value"sv, cookie.value.to_byte_string());
    serialized_cookie.set("path"sv, cookie.path.to_byte_string());
    serialized_cookie.set("domain"sv, cookie.domain.to_byte_string());
    serialized_cookie.set("secure"sv, cookie.secure);
    serialized_cookie.set("httpOnly"sv, cookie.http_only);
    serialized_cookie.set("expiry"sv, cookie.expiry_time.seconds_since_epoch());
    serialized_cookie.set("sameSite"sv, Web::Cookie::same_site_to_string(cookie.same_site));

    return serialized_cookie;
}

static JsonValue serialize_rect(Gfx::IntRect const& rect)
{
    JsonObject serialized_rect = {};
    serialized_rect.set("x", rect.x());
    serialized_rect.set("y", rect.y());
    serialized_rect.set("width", rect.width());
    serialized_rect.set("height", rect.height());

    return serialized_rect;
}

static Gfx::IntRect compute_window_rect(Web::Page const& page)
{
    return {
        page.window_position().x(),
        page.window_position().y(),
        page.window_size().width(),
        page.window_size().height()
    };
}

// https://w3c.github.io/webdriver/#dfn-no-longer-open
static ErrorOr<void, Web::WebDriver::Error> ensure_browsing_context_is_open(JS::GCPtr<Web::HTML::BrowsingContext> browsing_context)
{
    // A browsing context is said to be no longer open if its navigable has been destroyed.
    if (!browsing_context || browsing_context->has_navigable_been_destroyed())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchWindow, "Window not found"sv);
    return {};
}

// https://w3c.github.io/webdriver/#dfn-scrolls-into-view
static ErrorOr<void> scroll_element_into_view(Web::DOM::Element& element)
{
    // 1. Let options be the following ScrollIntoViewOptions:
    Web::DOM::ScrollIntoViewOptions options {};
    // Logical scroll position "block"
    //     "end"
    options.block = Web::Bindings::ScrollLogicalPosition::End;
    // Logical scroll position "inline"
    //     "nearest"
    options.inline_ = Web::Bindings::ScrollLogicalPosition::Nearest;

    // 2. Run Function.[[Call]](scrollIntoView, options) with element as the this value.
    TRY(element.scroll_into_view(options));

    return {};
}

// https://w3c.github.io/webdriver/#dfn-container
static Optional<Web::DOM::Element&> container_for_element(Web::DOM::Element& element)
{
    auto first_element_reached_by_traversing_the_tree_in_reverse_order = [](Web::DOM::Element& element, auto filter) -> Optional<Web::DOM::Element&> {
        auto node_iterator = element.document().create_node_iterator(element, to_underlying(Web::DOM::NodeFilter::WhatToShow::SHOW_ALL), nullptr);

        auto current_node = node_iterator->previous_node();
        while (current_node.has_value() && current_node.value() != nullptr && current_node.value()->is_element()) {
            if (filter(current_node.value()))
                return static_cast<Web::DOM::Element&>(*current_node.release_value());
        }

        return {};
    };

    // An element’s container is:
    // -> option element in a valid element context
    // -> optgroup element in a valid element context
    // FIXME: Determine if the element is in a valid element context. (https://html.spec.whatwg.org/#concept-element-contexts)
    if (is<Web::HTML::HTMLOptionElement>(element) || is<Web::HTML::HTMLOptGroupElement>(element)) {
        // The element’s element context, which is determined by:
        // 1. Let datalist parent be the first datalist element reached by traversing the tree in reverse order from element, or undefined if the root of the tree is reached.
        auto datalist_parent = first_element_reached_by_traversing_the_tree_in_reverse_order(element, [](auto& node) { return is<Web::HTML::HTMLDataListElement>(*node); });

        // 2. Let select parent be the first select element reached by traversing the tree in reverse order from element, or undefined if the root of the tree is reached.
        auto select_parent = first_element_reached_by_traversing_the_tree_in_reverse_order(element, [](auto& node) { return is<Web::HTML::HTMLSelectElement>(*node); });

        // 3. If datalist parent is undefined, the element context is select parent. Otherwise, the element context is datalist parent.
        if (!datalist_parent.has_value())
            return select_parent;
        return datalist_parent;
    }
    // -> option element in an invalid element context
    else if (is<Web::HTML::HTMLOptionElement>(element)) {
        // The element does not have a container.
        return {};
    }
    // -> Otherwise
    else {
        // The container is the element itself.
        return element;
    }
}

template<typename T>
static bool fire_an_event(FlyString name, Optional<Web::DOM::Element&> target)
{
    // FIXME: This is supposed to call the https://dom.spec.whatwg.org/#concept-event-fire DOM algorithm,
    //        but that doesn't seem to be implemented elsewhere. So, we'll ad-hack it for now. :^)

    if (!target.has_value())
        return false;

    auto event = T::create(target->realm(), name);
    return target->dispatch_event(event);
}

ErrorOr<NonnullRefPtr<WebDriverConnection>> WebDriverConnection::connect(Web::PageClient& page_client, ByteString const& webdriver_ipc_path)
{
    dbgln_if(WEBDRIVER_DEBUG, "Trying to connect to {}", webdriver_ipc_path);
    auto socket = TRY(Core::LocalSocket::connect(webdriver_ipc_path));

    // Allow pop-ups, or otherwise /window/new won't be able to open a new tab.
    page_client.page().set_should_block_pop_ups(false);

    dbgln_if(WEBDRIVER_DEBUG, "Connected to WebDriver");
    return adopt_nonnull_ref_or_enomem(new (nothrow) WebDriverConnection(move(socket), page_client));
}

WebDriverConnection::WebDriverConnection(NonnullOwnPtr<Core::LocalSocket> socket, Web::PageClient& page_client)
    : IPC::ConnectionToServer<WebDriverClientEndpoint, WebDriverServerEndpoint>(*this, move(socket))
{
    set_current_top_level_browsing_context(page_client.page().top_level_browsing_context());
}

void WebDriverConnection::visit_edges(JS::Cell::Visitor& visitor)
{
    visitor.visit(m_current_browsing_context);
    visitor.visit(m_current_parent_browsing_context);
    visitor.visit(m_current_top_level_browsing_context);
    visitor.visit(m_action_executor);
    visitor.visit(m_document_observer);
    visitor.visit(m_navigation_observer);
    visitor.visit(m_navigation_timer);
}

// https://w3c.github.io/webdriver/#dfn-close-the-session
void WebDriverConnection::close_session()
{
    // 1. Set the webdriver-active flag to false.
    set_is_webdriver_active(false);

    // 2. An endpoint node must close any top-level browsing contexts associated with the session, without prompting to unload.
    if (auto browsing_context = current_top_level_browsing_context())
        browsing_context->top_level_traversable()->close_top_level_traversable();
}

void WebDriverConnection::set_page_load_strategy(Web::WebDriver::PageLoadStrategy const& page_load_strategy)
{
    m_page_load_strategy = page_load_strategy;
}

void WebDriverConnection::set_unhandled_prompt_behavior(Web::WebDriver::UnhandledPromptBehavior const& unhandled_prompt_behavior)
{
    m_unhandled_prompt_behavior = unhandled_prompt_behavior;
}

void WebDriverConnection::set_strict_file_interactability(bool strict_file_interactability)
{
    m_strict_file_interactability = strict_file_interactability;
}

void WebDriverConnection::set_is_webdriver_active(bool is_webdriver_active)
{
    current_browsing_context().page().set_is_webdriver_active(is_webdriver_active);
}

// 9.1 Get Timeouts, https://w3c.github.io/webdriver/#dfn-get-timeouts
Messages::WebDriverClient::GetTimeoutsResponse WebDriverConnection::get_timeouts()
{
    // 1. Let timeouts be the timeouts object for session’s timeouts configuration
    auto timeouts = Web::WebDriver::timeouts_object(m_timeouts_configuration);

    // 2. Return success with data timeouts.
    return timeouts;
}

// 9.2 Set Timeouts, https://w3c.github.io/webdriver/#dfn-set-timeouts
Messages::WebDriverClient::SetTimeoutsResponse WebDriverConnection::set_timeouts(JsonValue const& payload)
{
    // FIXME: Spec issue: As written, the spec replaces the timeouts configuration with the newly provided values. But
    //        all other implementations update the existing configuration with any new values instead. WPT relies on
    //        this behavior, and sends us one timeout value at time.
    //        https://github.com/w3c/webdriver/issues/1596

    // 1. Let timeouts be the result of trying to JSON deserialize as a timeouts configuration the request’s parameters.
    TRY(Web::WebDriver::json_deserialize_as_a_timeouts_configuration_into(payload, m_timeouts_configuration));

    // 2. Make the session timeouts the new timeouts.

    // 3. Return success with data null.
    return JsonValue {};
}

// 10.1 Navigate To, https://w3c.github.io/webdriver/#navigate-to
Messages::WebDriverClient::NavigateToResponse WebDriverConnection::navigate_to(JsonValue const& payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "WebDriverConnection::navigate_to {}", payload);

    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 2. Let url be the result of getting the property url from the parameters argument.
    if (!payload.is_object() || !payload.as_object().has_string("url"sv))
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, "Payload doesn't have a string `url`"sv);
    URL::URL url(payload.as_object().get_byte_string("url"sv).value());

    // FIXME: 3. If url is not an absolute URL or is not an absolute URL with fragment or not a local scheme, return error with error code invalid argument.

    // 4. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 5. Let current URL be the current top-level browsing context’s active document’s URL.
    auto const& current_url = current_top_level_browsing_context()->active_document()->url();

    // FIXME: 6. If current URL and url do not have the same absolute URL:
    // FIXME:     a. If timer has not been started, start a timer. If this algorithm has not completed before timer reaches the session’s session page load timeout in milliseconds, return an error with error code timeout.

    // 7. Navigate the current top-level browsing context to url.
    current_top_level_browsing_context()->page().load(url);

    auto navigation_complete = JS::create_heap_function(current_top_level_browsing_context()->heap(), [this](Web::WebDriver::Response result) {
        // 9. Set the current browsing context with the current top-level browsing context.
        set_current_browsing_context(*current_top_level_browsing_context());

        // FIXME: 10. If the current top-level browsing context contains a refresh state pragma directive of time 1 second or less, wait until the refresh timeout has elapsed, a new navigate has begun, and return to the first step of this algorithm.

        async_navigation_complete(move(result));
    });

    // 8. If url is special except for file and current URL and URL do not have the same absolute URL:
    // AD-HOC: We wait for the navigation to complete regardless of whether the current URL differs from the provided
    //         URL. Even if they're the same, the navigation queues a tasks that we must await, otherwise subsequent
    //         endpoint invocations will attempt to operate on the wrong page.
    if (url.is_special() && url.scheme() != "file"sv) {
        // a. Try to wait for navigation to complete.
        wait_for_navigation_to_complete(navigation_complete);

        // FIXME: b. Try to run the post-navigation checks.
    } else {
        navigation_complete->function()(JsonValue {});
    }

    // 11. Return success with data null.
    return JsonValue {};
}

// 10.2 Get Current URL, https://w3c.github.io/webdriver/#get-current-url
Messages::WebDriverClient::GetCurrentUrlResponse WebDriverConnection::get_current_url()
{
    dbgln_if(WEBDRIVER_DEBUG, "WebDriverConnection::get_current_url");

    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let url be the serialization of the current top-level browsing context’s active document’s document URL.
    auto url = current_top_level_browsing_context()->active_document()->url().to_byte_string();

    // 4. Return success with data url.
    return url;
}

// 10.3 Back, https://w3c.github.io/webdriver/#dfn-back
Messages::WebDriverClient::BackResponse WebDriverConnection::back()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Traverse the history by a delta –1 for the current browsing context.
    current_browsing_context().page().client().page_did_request_navigate_back();

    // FIXME: 4. If the previous step completed results in a pageHide event firing, wait until pageShow event fires or for the session page load timeout milliseconds to pass, whichever occurs sooner.
    // FIXME: 5. If the previous step completed by the session page load timeout being reached, and user prompts have been handled, return error with error code timeout.

    // 6. Return success with data null.
    return JsonValue {};
}

// 10.4 Forward, https://w3c.github.io/webdriver/#dfn-forward
Messages::WebDriverClient::ForwardResponse WebDriverConnection::forward()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Traverse the history by a delta 1 for the current browsing context.
    current_browsing_context().page().client().page_did_request_navigate_forward();

    // FIXME: 4. If the previous step completed results in a pageHide event firing, wait until pageShow event fires or for the session page load timeout milliseconds to pass, whichever occurs sooner.
    // FIXME: 5. If the previous step completed by the session page load timeout being reached, and user prompts have been handled, return error with error code timeout.

    // 6. Return success with data null.
    return JsonValue {};
}

// 10.5 Refresh, https://w3c.github.io/webdriver/#dfn-refresh
Messages::WebDriverClient::RefreshResponse WebDriverConnection::refresh()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Initiate an overridden reload of the current top-level browsing context’s active document.
    current_top_level_browsing_context()->page().client().page_did_request_refresh();

    // FIXME: 4. If url is special except for file:
    // FIXME:     1. Try to wait for navigation to complete.
    // FIXME:     2. Try to run the post-navigation checks.

    // 5. Set the current browsing context with current top-level browsing context.
    set_current_browsing_context(*current_top_level_browsing_context());

    // 6. Return success with data null.
    return JsonValue {};
}

// 10.6 Get Title, https://w3c.github.io/webdriver/#dfn-get-title
Messages::WebDriverClient::GetTitleResponse WebDriverConnection::get_title()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let title be the initial value of the title IDL attribute of the current top-level browsing context's active document.
    auto title = current_top_level_browsing_context()->active_document()->title();

    // 4. Return success with data title.
    return title.to_byte_string();
}

// 11.1 Get Window Handle, https://w3c.github.io/webdriver/#get-window-handle
Messages::WebDriverClient::GetWindowHandleResponse WebDriverConnection::get_window_handle()
{
    // 1. If session's current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 2. Return success with data being the window handle associated with session's current top-level browsing context.
    return JsonValue { current_top_level_browsing_context()->top_level_traversable()->window_handle() };
}

// 11.2 Close Window, https://w3c.github.io/webdriver/#dfn-close-window
Messages::WebDriverClient::CloseWindowResponse WebDriverConnection::close_window()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Close the current top-level browsing context.
    current_top_level_browsing_context()->top_level_traversable()->close_top_level_traversable();

    return JsonValue {};
}

// 11.3 Switch to Window, https://w3c.github.io/webdriver/#dfn-switch-to-window
Messages::WebDriverClient::SwitchToWindowResponse WebDriverConnection::switch_to_window(String const& handle)
{
    // 4. If handle is equal to the associated window handle for some top-level browsing context in the
    //    current session, let context be the that browsing context, and set the current top-level
    //    browsing context with context.
    //    Otherwise, return error with error code no such window.
    bool found_matching_context = false;

    for (auto* navigable : Web::HTML::all_navigables()) {
        auto traversable = navigable->top_level_traversable();
        if (!traversable || !traversable->active_browsing_context())
            continue;

        if (handle == traversable->window_handle()) {
            set_current_top_level_browsing_context(*traversable->active_browsing_context());
            found_matching_context = true;
            break;
        }
    }

    if (!found_matching_context)
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchWindow, "Window not found");

    // 5. Update any implementation-specific state that would result from the user selecting the current
    //    browsing context for interaction, without altering OS-level focus.
    current_browsing_context().page().client().page_did_request_activate_tab();

    return JsonValue {};
}

// 11.5 New Window, https://w3c.github.io/webdriver/#dfn-new-window
Messages::WebDriverClient::NewWindowResponse WebDriverConnection::new_window(JsonValue const& payload)
{
    // 1. If the implementation does not support creating new top-level browsing contexts, return error with error code unsupported operation.

    // 2. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 3. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 4. Let type hint be the result of getting the property "type" from the parameters argument.
    if (!payload.is_object())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, "Payload is not a JSON object");

    // FIXME: Actually use this value to decide between an OS window or tab.
    auto type_hint = payload.as_object().get("type"sv);
    if (type_hint.has_value() && !type_hint->is_null() && !type_hint->is_string())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, "Payload property `type` is not null or a string"sv);

    // 5. Create a new top-level browsing context by running the window open steps with url set to "about:blank",
    //    target set to the empty string, and features set to "noopener" and the user agent configured to create a new
    //    browsing context. This must be done without invoking the focusing steps for the created browsing context. If
    //    type hint has the value "tab", and the implementation supports multiple browsing context in the same OS
    //    window, the new browsing context should share an OS window with the current browsing context. If type hint
    //    is "window", and the implementation supports multiple browsing contexts in separate OS windows, the
    //    created browsing context should be in a new OS window. In all other cases the details of how the browsing
    //    context is presented to the user are implementation defined.
    auto* active_window = current_browsing_context().active_window();
    VERIFY(active_window);

    Web::HTML::TemporaryExecutionContext execution_context { active_window->document()->relevant_settings_object() };
    auto [target_navigable, no_opener, window_type] = MUST(active_window->window_open_steps_internal("about:blank"sv, ""sv, "noopener"sv));

    // 6. Let handle be the associated window handle of the newly created window.
    auto handle = target_navigable->traversable_navigable()->window_handle();

    // 7. Let type be "tab" if the newly created window shares an OS-level window with the current browsing context, or "window" otherwise.
    auto type = "tab"sv;

    // 8. Let result be a new JSON Object initialized with:
    JsonObject result;
    result.set("handle"sv, JsonValue { handle });
    result.set("type"sv, JsonValue { type });

    // 9. Return success with data result.
    return result;
}

// 11.6 Switch To Frame, https://w3c.github.io/webdriver/#dfn-switch-to-frame
Messages::WebDriverClient::SwitchToFrameResponse WebDriverConnection::switch_to_frame(JsonValue const& payload)
{
    // 1. Let id be the result of getting the property "id" from parameters.
    if (!payload.is_object() || !payload.as_object().has("id"sv))
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, "Payload doesn't have property `id`"sv);

    auto id = payload.as_object().get("id"sv).release_value();

    // 2. If id is not null, a Number object, or an Object that represents a web element, return error with error code invalid argument.
    if (!id.is_null() && !id.is_number() && !Web::WebDriver::represents_a_web_element(id))
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, "Payload property `id` is not null, a number, or a web element"sv);

    // 3. Run the substeps of the first matching condition:

    // -> id is null
    if (id.is_null()) {
        // 1. If session's current top-level browsing context is no longer open, return error with error code no such window.
        TRY(ensure_current_top_level_browsing_context_is_open());

        // 2. Try to handle any user prompts with session.
        TRY(handle_any_user_prompts());

        // 3. Set the current browsing context with session and session's current top-level browsing context.
        set_current_browsing_context(*current_top_level_browsing_context());
    }

    // -> id is a Number object
    else if (id.is_number()) {
        // FIXME: 1. If id is less than 0 or greater than 216 – 1, return error with error code invalid argument.
        // FIXME: 2. If session's current browsing context is no longer open, return error with error code no such window.
        // FIXME: 3. Try to handle any user prompts with session.
        // FIXME: 4. Let window be the associated window of session's current browsing context's active document.
        // FIXME: 5. If id is not a supported property index of window, return error with error code no such frame.
        // FIXME: 6. Let child window be the WindowProxy object obtained by calling window.[[GetOwnProperty]] (id).
        // FIXME: 7. Set the current browsing context with session and child window's browsing context.
        dbgln("FIXME: WebDriverConnection::switch_to_frame(id={})", id);
    }

    // -> id represents a web element
    else if (id.is_object()) {
        auto element_id = Web::WebDriver::extract_web_element_reference(id.as_object());

        // 1. If session's current browsing context is no longer open, return error with error code no such window.
        TRY(ensure_current_browsing_context_is_open());

        // 2. Try to handle any user prompts with session.
        TRY(handle_any_user_prompts());

        // 3. Let element be the result of trying to get a known element with session and id.
        auto element = TRY(Web::WebDriver::get_known_element(element_id));

        // 4. If element is not a frame or iframe element, return error with error code no such frame.
        bool is_frame = is<Web::HTML::HTMLFrameElement>(*element);
        bool is_iframe = is<Web::HTML::HTMLIFrameElement>(*element);

        if (!is_frame && !is_iframe)
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchFrame, "element is not a frame"sv);

        // 5. Set the current browsing context with session and element's content navigable's active browsing context.
        if (is_frame) {
            // FIXME: Should HTMLFrameElement also be a NavigableContainer?
            set_current_browsing_context(*element->navigable()->active_browsing_context());
        } else {
            auto& navigable_container = static_cast<Web::HTML::NavigableContainer&>(*element);
            set_current_browsing_context(*navigable_container.content_navigable()->active_browsing_context());
        }
    }

    // FIXME: 4. Update any implementation-specific state that would result from the user selecting session's current browsing context for interaction, without altering OS-level focus.

    // 5. Return success with data null
    return JsonValue {};
}

// 11.7 Switch To Parent Frame, https://w3c.github.io/webdriver/#dfn-switch-to-parent-frame
Messages::WebDriverClient::SwitchToParentFrameResponse WebDriverConnection::switch_to_parent_frame(JsonValue const&)
{
    // 1. If session's current browsing context is already the top-level browsing context:
    if (&current_browsing_context() == current_top_level_browsing_context()) {
        // 1. If session's current browsing context is no longer open, return error with error code no such window.
        TRY(ensure_current_browsing_context_is_open());

        // 2. Return success with data null.
        return JsonValue {};
    }

    auto parent_browsing_context = current_parent_browsing_context();

    // 2. If session's current parent browsing context is no longer open, return error with error code no such window.
    TRY(ensure_browsing_context_is_open(parent_browsing_context));

    // 3. Try to handle any user prompts with session.
    TRY(handle_any_user_prompts());

    // 4. If session's current parent browsing context is not null, set the current browsing context with session and
    //    current parent browsing context.
    if (parent_browsing_context)
        set_current_browsing_context(*current_parent_browsing_context());

    // FIXME: 5. Update any implementation-specific state that would result from the user selecting session's current browsing context for interaction, without altering OS-level focus.

    // 6. Return success with data null.
    return JsonValue {};
}

// 11.8.1 Get Window Rect, https://w3c.github.io/webdriver/#dfn-get-window-rect
Messages::WebDriverClient::GetWindowRectResponse WebDriverConnection::get_window_rect()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Return success with data set to the WindowRect object for the current top-level browsing context.
    return serialize_rect(compute_window_rect(current_top_level_browsing_context()->page()));
}

// 11.8.2 Set Window Rect, https://w3c.github.io/webdriver/#dfn-set-window-rect
Messages::WebDriverClient::SetWindowRectResponse WebDriverConnection::set_window_rect(JsonValue const& payload)
{
    if (!payload.is_object())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, "Payload is not a JSON object");

    auto const& properties = payload.as_object();

    auto resolve_property = [](auto name, auto const& property, i32 min, i32 max) -> ErrorOr<Optional<i32>, Web::WebDriver::Error> {
        if (property.is_null())
            return Optional<i32> {};
        auto value = property.template get_integer<i32>();
        if (!value.has_value())
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, ByteString::formatted("Property '{}' is not a Number", name));
        if (*value < min)
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, ByteString::formatted("Property '{}' value {} exceeds the minimum allowed value {}", name, *value, min));
        if (*value > max)
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, ByteString::formatted("Property '{}' value {} exceeds the maximum allowed value {}", name, *value, max));

        return value;
    };

    // 1. Let width be the result of getting a property named width from the parameters argument, else let it be null.
    auto width_property = properties.get("width"sv).value_or(JsonValue());

    // 2. Let height be the result of getting a property named height from the parameters argument, else let it be null.
    auto height_property = properties.get("height"sv).value_or(JsonValue());

    // 3. Let x be the result of getting a property named x from the parameters argument, else let it be null.
    auto x_property = properties.get("x"sv).value_or(JsonValue());

    // 4. Let y be the result of getting a property named y from the parameters argument, else let it be null.
    auto y_property = properties.get("y"sv).value_or(JsonValue());

    // 5. If width or height is neither null nor a Number from 0 to 2^31 − 1, return error with error code invalid argument.
    auto width = TRY(resolve_property("width"sv, width_property, 0, NumericLimits<i32>::max()));
    auto height = TRY(resolve_property("height"sv, height_property, 0, NumericLimits<i32>::max()));

    // 6. If x or y is neither null nor a Number from −(2^31) to 2^31 − 1, return error with error code invalid argument.
    auto x = TRY(resolve_property("x"sv, x_property, NumericLimits<i32>::min(), NumericLimits<i32>::max()));
    auto y = TRY(resolve_property("y"sv, y_property, NumericLimits<i32>::min(), NumericLimits<i32>::max()));

    // 7. If the remote end does not support the Set Window Rect command for the current top-level browsing context for any reason, return error with error code unsupported operation.

    // 8. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 9. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // FIXME: 10. Fully exit fullscreen.

    // 11. Restore the window.
    restore_the_window();

    Gfx::IntRect window_rect;

    // 11. If width and height are not null:
    if (width.has_value() && height.has_value()) {
        // a. Set the width, in CSS pixels, of the operating system window containing the current top-level browsing context, including any browser chrome and externally drawn window decorations to a value that is as close as possible to width.
        // b. Set the height, in CSS pixels, of the operating system window containing the current top-level browsing context, including any browser chrome and externally drawn window decorations to a value that is as close as possible to height.
        auto size = current_top_level_browsing_context()->page().client().page_did_request_resize_window({ *width, *height });
        window_rect.set_size(size);
    } else {
        window_rect.set_size(current_top_level_browsing_context()->page().window_size().to_type<int>());
    }

    // 12. If x and y are not null:
    if (x.has_value() && y.has_value()) {
        // a. Run the implementation-specific steps to set the position of the operating system level window containing the current top-level browsing context to the position given by the x and y coordinates.
        auto position = current_top_level_browsing_context()->page().client().page_did_request_reposition_window({ *x, *y });
        window_rect.set_location(position);
    } else {
        window_rect.set_location(current_top_level_browsing_context()->page().window_position().to_type<int>());
    }

    // 14. Return success with data set to the WindowRect object for the current top-level browsing context.
    return serialize_rect(window_rect);
}

// 11.8.3 Maximize Window, https://w3c.github.io/webdriver/#dfn-maximize-window
Messages::WebDriverClient::MaximizeWindowResponse WebDriverConnection::maximize_window()
{
    // 1. If the remote end does not support the Maximize Window command for the current top-level browsing context for any reason, return error with error code unsupported operation.

    // 2. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 3. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // FIXME: 4. Fully exit fullscreen.

    // 5. Restore the window.
    restore_the_window();

    // 6. Maximize the window of the current top-level browsing context.
    auto window_rect = maximize_the_window();

    // 7. Return success with data set to the WindowRect object for the current top-level browsing context.
    return serialize_rect(window_rect);
}

// 11.8.4 Minimize Window, https://w3c.github.io/webdriver/#minimize-window
Messages::WebDriverClient::MinimizeWindowResponse WebDriverConnection::minimize_window()
{
    // 1. If the remote end does not support the Minimize Window command for the current top-level browsing context for any reason, return error with error code unsupported operation.

    // 2. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 3. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // FIXME: 4. Fully exit fullscreen.

    // 5. Iconify the window.
    auto window_rect = iconify_the_window();

    // 6. Return success with data set to the WindowRect object for the current top-level browsing context.
    return serialize_rect(window_rect);
}

// 11.8.5 Fullscreen Window, https://w3c.github.io/webdriver/#dfn-fullscreen-window
Messages::WebDriverClient::FullscreenWindowResponse WebDriverConnection::fullscreen_window()
{
    // 1. If the remote end does not support fullscreen return error with error code unsupported operation.

    // 2. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 3. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 4. Restore the window.
    restore_the_window();

    // 5. FIXME: Call fullscreen an element with the current top-level browsing context’s active document’s document element.
    //           As described in https://fullscreen.spec.whatwg.org/#fullscreen-an-element
    //    NOTE: What we do here is basically `requestFullscreen(options)` with options["navigationUI"]="show"
    auto rect = current_top_level_browsing_context()->page().client().page_did_request_fullscreen_window();

    // 6. Return success with data set to the WindowRect object for the current top-level browsing context.
    return serialize_rect(rect);
}

// Extension Consume User Activation, https://html.spec.whatwg.org/multipage/interaction.html#user-activation-user-agent-automation
Messages::WebDriverClient::ConsumeUserActivationResponse WebDriverConnection::consume_user_activation()
{
    // FIXME: This should probably be in the spec steps
    // If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 1. Let window be current browsing context's active window.
    auto* window = current_browsing_context().active_window();

    // 2. Let consume be true if window has transient activation; otherwise false.
    bool consume = window->has_transient_activation();

    // 3. If consume is true, then consume user activation of window.
    if (consume)
        window->consume_user_activation();

    // 4. Return success with data consume.
    return consume;
}

// 12.3.2 Find Element, https://w3c.github.io/webdriver/#dfn-find-element
Messages::WebDriverClient::FindElementResponse WebDriverConnection::find_element(JsonValue const& payload)
{
    // 1. Let location strategy be the result of getting a property called "using".
    auto location_strategy_string = TRY(Web::WebDriver::get_property(payload, "using"sv));
    auto location_strategy = Web::WebDriver::location_strategy_from_string(location_strategy_string);

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!location_strategy.has_value())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, ByteString::formatted("Location strategy '{}' is invalid", location_strategy_string));

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    auto selector = TRY(Web::WebDriver::get_property(payload, "value"sv));

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 6. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    auto start_node_getter = [this]() -> StartNodeGetter::ReturnType {
        // 7. Let start node be the current browsing context’s document element.
        auto* start_node = current_browsing_context().active_document();

        // 8. If start node is null, return error with error code no such element.
        if (!start_node)
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchElement, "document element does not exist"sv);

        return *start_node;
    };

    // 9. Let result be the result of trying to Find with start node, location strategy, and selector.
    auto result = TRY(find(move(start_node_getter), *location_strategy, selector));

    // 10. If result is empty, return error with error code no such element. Otherwise, return the first element of result.
    if (result.is_empty())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchElement, "The requested element does not exist"sv);

    return result.take(0);
}

// 12.3.3 Find Elements, https://w3c.github.io/webdriver/#dfn-find-elements
Messages::WebDriverClient::FindElementsResponse WebDriverConnection::find_elements(JsonValue const& payload)
{
    // 1. Let location strategy be the result of getting a property called "using".
    auto location_strategy_string = TRY(Web::WebDriver::get_property(payload, "using"sv));
    auto location_strategy = Web::WebDriver::location_strategy_from_string(location_strategy_string);

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!location_strategy.has_value())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, ByteString::formatted("Location strategy '{}' is invalid", location_strategy_string));

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    auto selector = TRY(Web::WebDriver::get_property(payload, "value"sv));

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 6. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    auto start_node_getter = [this]() -> StartNodeGetter::ReturnType {
        // 7. Let start node be the current browsing context’s document element.
        auto* start_node = current_browsing_context().active_document();

        // 8. If start node is null, return error with error code no such element.
        if (!start_node)
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchElement, "document element does not exist"sv);

        return *start_node;
    };

    // 9. Return the result of trying to Find with start node, location strategy, and selector.
    return TRY(find(move(start_node_getter), *location_strategy, selector));
}

// 12.3.4 Find Element From Element, https://w3c.github.io/webdriver/#dfn-find-element-from-element
Messages::WebDriverClient::FindElementFromElementResponse WebDriverConnection::find_element_from_element(JsonValue const& payload, String const& element_id)
{
    // 1. Let location strategy be the result of getting a property called "using".
    auto location_strategy_string = TRY(Web::WebDriver::get_property(payload, "using"sv));
    auto location_strategy = Web::WebDriver::location_strategy_from_string(location_strategy_string);

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!location_strategy.has_value())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, ByteString::formatted("Location strategy '{}' is invalid", location_strategy_string));

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    auto selector = TRY(Web::WebDriver::get_property(payload, "value"sv));

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 6. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    auto start_node_getter = [&]() -> StartNodeGetter::ReturnType {
        // 7. Let start node be the result of trying to get a known connected element with url variable element id.
        return TRY(Web::WebDriver::get_known_element(element_id));
    };

    // 8. Let result be the value of trying to Find with start node, location strategy, and selector.
    auto result = TRY(find(move(start_node_getter), *location_strategy, selector));

    // 9. If result is empty, return error with error code no such element. Otherwise, return the first element of result.
    if (result.is_empty())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchElement, "The requested element does not exist"sv);

    return result.take(0);
}

// 12.3.5 Find Elements From Element, https://w3c.github.io/webdriver/#dfn-find-elements-from-element
Messages::WebDriverClient::FindElementsFromElementResponse WebDriverConnection::find_elements_from_element(JsonValue const& payload, String const& element_id)
{
    // 1. Let location strategy be the result of getting a property called "using".
    auto location_strategy_string = TRY(Web::WebDriver::get_property(payload, "using"sv));
    auto location_strategy = Web::WebDriver::location_strategy_from_string(location_strategy_string);

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!location_strategy.has_value())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, ByteString::formatted("Location strategy '{}' is invalid", location_strategy_string));

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    auto selector = TRY(Web::WebDriver::get_property(payload, "value"sv));

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 6. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    auto start_node_getter = [&]() -> StartNodeGetter::ReturnType {
        // 7. Let start node be the result of trying to get a known connected element with url variable element id.
        return TRY(Web::WebDriver::get_known_element(element_id));
    };

    // 8. Return the result of trying to Find with start node, location strategy, and selector.
    return TRY(find(move(start_node_getter), *location_strategy, selector));
}

// 12.3.6 Find Element From Shadow Root, https://w3c.github.io/webdriver/#find-element-from-shadow-root
Messages::WebDriverClient::FindElementFromShadowRootResponse WebDriverConnection::find_element_from_shadow_root(JsonValue const& payload, String const& shadow_id)
{
    // 1. Let location strategy be the result of getting a property called "using".
    auto location_strategy_string = TRY(Web::WebDriver::get_property(payload, "using"sv));
    auto location_strategy = Web::WebDriver::location_strategy_from_string(location_strategy_string);

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!location_strategy.has_value())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, ByteString::formatted("Location strategy '{}' is invalid", location_strategy_string));

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    auto selector = TRY(Web::WebDriver::get_property(payload, "value"sv));

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 6. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    auto start_node_getter = [&]() -> StartNodeGetter::ReturnType {
        // 7. Let start node be the result of trying to get a known shadow root with url variable shadow id.
        return TRY(Web::WebDriver::get_known_shadow_root(shadow_id));
    };

    // 8. Let result be the value of trying to Find with start node, location strategy, and selector.
    auto result = TRY(find(move(start_node_getter), *location_strategy, selector));

    // 9. If result is empty, return error with error code no such element. Otherwise, return the first element of result.
    if (result.is_empty())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchElement, "The requested element does not exist"sv);

    return result.take(0);
}

// 12.3.7 Find Elements From Shadow Root, https://w3c.github.io/webdriver/#find-elements-from-shadow-root
Messages::WebDriverClient::FindElementsFromShadowRootResponse WebDriverConnection::find_elements_from_shadow_root(JsonValue const& payload, String const& shadow_id)
{
    // 1. Let location strategy be the result of getting a property called "using".
    auto location_strategy_string = TRY(Web::WebDriver::get_property(payload, "using"sv));
    auto location_strategy = Web::WebDriver::location_strategy_from_string(location_strategy_string);

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!location_strategy.has_value())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, ByteString::formatted("Location strategy '{}' is invalid", location_strategy_string));

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    auto selector = TRY(Web::WebDriver::get_property(payload, "value"sv));

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 6. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    auto start_node_getter = [&]() -> StartNodeGetter::ReturnType {
        // 7. Let start node be the result of trying to get a known shadow root with url variable shadow id.
        return TRY(Web::WebDriver::get_known_shadow_root(shadow_id));
    };

    // 8. Return the result of trying to Find with start node, location strategy, and selector.
    return TRY(find(move(start_node_getter), *location_strategy, selector));
}

// 12.3.8 Get Active Element, https://w3c.github.io/webdriver/#get-active-element
Messages::WebDriverClient::GetActiveElementResponse WebDriverConnection::get_active_element()
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let active element be the active element of the current browsing context’s document element.
    auto* active_element = current_browsing_context().active_document()->active_element();

    // 4. If active element is a non-null element, return success with data set to web element reference object for active element.
    //    Otherwise, return error with error code no such element.
    if (active_element)
        return ByteString::number(active_element->unique_id());

    return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchElement, "The current document does not have an active element"sv);
}

// 12.3.9 Get Element Shadow Root, https://w3c.github.io/webdriver/#get-element-shadow-root
Messages::WebDriverClient::GetElementShadowRootResponse WebDriverConnection::get_element_shadow_root(String const& element_id)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto element = TRY(Web::WebDriver::get_known_element(element_id));

    // 4. Let shadow root be element's shadow root.
    auto shadow_root = element->shadow_root();

    // 5. If shadow root is null, return error with error code no such shadow root.
    if (!shadow_root)
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchShadowRoot, ByteString::formatted("Element with ID '{}' does not have a shadow root", element_id));

    // 6. Let serialized be the shadow root reference object for shadow root.
    auto serialized = Web::WebDriver::shadow_root_reference_object(*shadow_root);

    // 7. Return success with data serialized.
    return serialized;
}

// 12.4.1 Is Element Selected, https://w3c.github.io/webdriver/#dfn-is-element-selected
Messages::WebDriverClient::IsElementSelectedResponse WebDriverConnection::is_element_selected(String const& element_id)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto element = TRY(Web::WebDriver::get_known_element(element_id));

    // 4. Let selected be the value corresponding to the first matching statement:
    bool selected = false;

    // element is an input element with a type attribute in the Checkbox- or Radio Button state
    if (is<Web::HTML::HTMLInputElement>(*element)) {
        // -> The result of element’s checkedness.
        auto& input = static_cast<Web::HTML::HTMLInputElement&>(*element);
        using enum Web::HTML::HTMLInputElement::TypeAttributeState;

        if (input.type_state() == Checkbox || input.type_state() == RadioButton)
            selected = input.checked();
    }
    // element is an option element
    else if (is<Web::HTML::HTMLOptionElement>(*element)) {
        // -> The result of element’s selectedness.
        selected = static_cast<Web::HTML::HTMLOptionElement&>(*element).selected();
    }
    // Otherwise
    //   -> False.

    // 5. Return success with data selected.
    return selected;
}

// 12.4.2 Get Element Attribute, https://w3c.github.io/webdriver/#dfn-get-element-attribute
Messages::WebDriverClient::GetElementAttributeResponse WebDriverConnection::get_element_attribute(String const& element_id, String const& name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto element = TRY(Web::WebDriver::get_known_element(element_id));

    // 4. Let result be the result of the first matching condition:
    Optional<ByteString> result;

    // -> If name is a boolean attribute
    if (Web::HTML::is_boolean_attribute(name)) {
        // "true" (string) if the element has the attribute, otherwise null.
        if (element->has_attribute(name))
            result = "true"sv;
    }
    // -> Otherwise
    else {
        // The result of getting an attribute by name name.
        if (auto attr = element->get_attribute(name); attr.has_value())
            result = attr->to_byte_string();
    }

    // 5. Return success with data result.
    if (result.has_value())
        return result.release_value();
    return JsonValue {};
}

// 12.4.3 Get Element Property, https://w3c.github.io/webdriver/#dfn-get-element-property
Messages::WebDriverClient::GetElementPropertyResponse WebDriverConnection::get_element_property(String const& element_id, String const& name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto element = TRY(Web::WebDriver::get_known_element(element_id));

    Optional<ByteString> result;

    // 4. Let property be the result of calling the Object.[[GetProperty]](name) on element.
    Web::HTML::TemporaryExecutionContext execution_context { current_browsing_context().active_document()->relevant_settings_object() };

    if (auto property_or_error = element->get(name.to_byte_string()); !property_or_error.is_throw_completion()) {
        auto property = property_or_error.release_value();

        // 5. Let result be the value of property if not undefined, or null.
        if (!property.is_undefined()) {
            if (auto string_or_error = property.to_byte_string(element->vm()); !string_or_error.is_error())
                result = string_or_error.release_value();
        }
    }

    // 6. Return success with data result.
    if (result.has_value())
        return result.release_value();
    return JsonValue {};
}

// 12.4.4 Get Element CSS Value, https://w3c.github.io/webdriver/#dfn-get-element-css-value
Messages::WebDriverClient::GetElementCssValueResponse WebDriverConnection::get_element_css_value(String const& element_id, String const& name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto element = TRY(Web::WebDriver::get_known_element(element_id));

    // 4. Let computed value be the result of the first matching condition:
    ByteString computed_value;

    // -> current browsing context’s active document’s type is not "xml"
    if (!current_browsing_context().active_document()->is_xml_document()) {
        // computed value of parameter property name from element’s style declarations. property name is obtained from url variables.
        if (auto property = Web::CSS::property_id_from_string(name); property.has_value()) {
            if (auto* computed_values = element->computed_css_values())
                computed_value = computed_values->property(property.value())->to_string().to_byte_string();
        }
    }
    // -> Otherwise
    else {
        // "" (empty string)
        computed_value = ByteString::empty();
    }

    // 5. Return success with data computed value.
    return computed_value;
}

// 12.4.5 Get Element Text, https://w3c.github.io/webdriver/#dfn-get-element-text
Messages::WebDriverClient::GetElementTextResponse WebDriverConnection::get_element_text(String const& element_id)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto element = TRY(Web::WebDriver::get_known_element(element_id));

    // 4. Let rendered text be the result of performing implementation-specific steps whose result is exactly the same as the result of a Function.[[Call]](null, element) with bot.dom.getVisibleText as the this value.
    auto rendered_text = element->text_content();

    // 5. Return success with data rendered text.
    return rendered_text.value_or(String {}).to_byte_string();
}

// 12.4.6 Get Element Tag Name, https://w3c.github.io/webdriver/#dfn-get-element-tag-name
Messages::WebDriverClient::GetElementTagNameResponse WebDriverConnection::get_element_tag_name(String const& element_id)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto element = TRY(Web::WebDriver::get_known_element(element_id));

    // 4. Let qualified name be the result of getting element’s tagName IDL attribute.
    auto qualified_name = element->tag_name();

    // 5. Return success with data qualified name.
    return MUST(JsonValue::from_string(qualified_name));
}

// 12.4.7 Get Element Rect, https://w3c.github.io/webdriver/#dfn-get-element-rect
Messages::WebDriverClient::GetElementRectResponse WebDriverConnection::get_element_rect(String const& element_id)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto element = TRY(Web::WebDriver::get_known_element(element_id));

    // 4. Calculate the absolute position of element and let it be coordinates.
    // 5. Let rect be element’s bounding rectangle.
    auto rect = calculate_absolute_rect_of_element(*element);

    // 6. Let body be a new JSON Object initialized with:
    // "x"
    //     The first value of coordinates.
    // "y"
    //     The second value of coordinates.
    // "width"
    //     Value of rect’s width dimension.
    // "height"
    //     Value of rect’s height dimension.
    auto body = serialize_rect(rect);

    // 7. Return success with data body.
    return body;
}

// 12.4.8 Is Element Enabled, https://w3c.github.io/webdriver/#dfn-is-element-enabled
Messages::WebDriverClient::IsElementEnabledResponse WebDriverConnection::is_element_enabled(String const& element_id)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto element = TRY(Web::WebDriver::get_known_element(element_id));

    // 4. Let enabled be a boolean initially set to true if the current browsing context’s active document’s type is not "xml".
    // 5. Otherwise, let enabled to false and jump to the last step of this algorithm.
    bool enabled = !current_browsing_context().active_document()->is_xml_document();

    // 6. Set enabled to false if a form control is disabled.
    if (enabled && is<Web::HTML::FormAssociatedElement>(*element)) {
        auto& form_associated_element = dynamic_cast<Web::HTML::FormAssociatedElement&>(*element);
        enabled = form_associated_element.enabled();
    }

    // 7. Return success with data enabled.
    return enabled;
}

// 12.4.9 Get Computed Role, https://w3c.github.io/webdriver/#dfn-get-computed-role
Messages::WebDriverClient::GetComputedRoleResponse WebDriverConnection::get_computed_role(String const& element_id)
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto element = TRY(Web::WebDriver::get_known_element(element_id));

    // 4. Let role be the result of computing the WAI-ARIA role of element.
    auto role = element->role_or_default();

    // 5. Return success with data role.
    if (role.has_value())
        return Web::ARIA::role_name(*role);
    return ""sv;
}

// 12.4.10 Get Computed Label, https://w3c.github.io/webdriver/#get-computed-label
Messages::WebDriverClient::GetComputedLabelResponse WebDriverConnection::get_computed_label(String const& element_id)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let element be the result of trying to get a known element with url variable element id.
    auto element = TRY(Web::WebDriver::get_known_element(element_id));

    // 4. Let label be the result of a Accessible Name and Description Computation for the Accessible Name of the element.
    auto label = element->accessible_name(element->document()).release_value_but_fixme_should_propagate_errors();

    // 5. Return success with data label.
    return label.to_byte_string();
}

// 12.5.1 Element Click, https://w3c.github.io/webdriver/#element-click
Messages::WebDriverClient::ElementClickResponse WebDriverConnection::element_click(String const& element_id)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let element be the result of trying to get a known element with element id.
    auto element = TRY(Web::WebDriver::get_known_element(element_id));

    // 4. If the element is an input element in the file upload state return error with error code invalid argument.
    if (is<Web::HTML::HTMLInputElement>(*element)) {
        // -> The result of element’s checkedness.
        auto& input = static_cast<Web::HTML::HTMLInputElement&>(*element);
        using enum Web::HTML::HTMLInputElement::TypeAttributeState;

        if (input.type_state() == FileUpload)
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, "Clicking on an input element in the file upload state is not supported"sv);
    }

    // 5. Scroll into view the element’s container.
    auto element_container = container_for_element(*element);
    auto scroll_or_error = scroll_element_into_view(*element_container);
    if (scroll_or_error.is_error())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::UnknownError, scroll_or_error.error().string_literal());

    // FIXME: 6. If element’s container is still not in view, return error with error code element not interactable.
    // FIXME: 7. If element’s container is obscured by another element, return error with error code element click intercepted.

    auto on_complete = JS::create_heap_function(current_browsing_context().heap(), [this](Web::WebDriver::Response result) {
        // 9. Wait until the user agent event loop has spun enough times to process the DOM events generated by the
        //    previous step.
        m_action_executor = nullptr;

        // FIXME: 10. Perform implementation-defined steps to allow any navigations triggered by the click to start.

        // 11. Try to wait for navigation to complete.
        wait_for_navigation_to_complete(JS::create_heap_function(current_browsing_context().heap(), [this, result = move(result)](Web::WebDriver::Response navigation_result) mutable {
            // FIXME: 12. Try to run the post-navigation checks.

            if (navigation_result.is_error())
                async_actions_performed(move(navigation_result));
            else
                async_actions_performed(move(result));
        }));
    });

    // 8. Matching on element:
    // -> option element
    if (is<Web::HTML::HTMLOptionElement>(*element)) {
        auto& option_element = static_cast<Web::HTML::HTMLOptionElement&>(*element);

        // 1. Let parent node be the element’s container.
        auto parent_node = element_container;

        // 2. Fire a mouseOver event at parent node.
        fire_an_event<Web::UIEvents::MouseEvent>(Web::UIEvents::EventNames::mouseover, parent_node);

        // 3. Fire a mouseMove event at parent node.
        fire_an_event<Web::UIEvents::MouseEvent>(Web::UIEvents::EventNames::mousemove, parent_node);

        // 4. Fire a mouseDown event at parent node.
        fire_an_event<Web::UIEvents::MouseEvent>(Web::UIEvents::EventNames::mousedown, parent_node);

        // 5. Run the focusing steps on parent node.
        Web::HTML::run_focusing_steps(parent_node.has_value() ? &*parent_node : nullptr);

        // 6. If element is not disabled:
        if (!option_element.is_actually_disabled()) {
            // 1. Fire an input event at parent node.
            fire_an_event<Web::DOM::Event>(Web::HTML::EventNames::input, parent_node);

            // 2. Let previous selectedness be equal to element selectedness.
            auto previous_selectedness = option_element.selected();

            // 3. If element’s container has the multiple attribute, toggle the element’s selectedness state
            //    by setting it to the opposite value of its current selectedness.
            if (parent_node.has_value() && parent_node->has_attribute(Web::HTML::AttributeNames::multiple)) {
                option_element.set_selected(!option_element.selected());
            }
            //    Otherwise, set the element’s selectedness state to true.
            else {
                option_element.set_selected(true);
            }

            // 4. If previous selectedness is false, fire a change event at parent node.
            if (!previous_selectedness) {
                fire_an_event<Web::DOM::Event>(Web::HTML::EventNames::change, parent_node);
            }
        }

        // 7. Fire a mouseUp event at parent node.
        fire_an_event<Web::UIEvents::MouseEvent>(Web::UIEvents::EventNames::mouseup, parent_node);

        // 8. Fire a click event at parent node.
        fire_an_event<Web::UIEvents::MouseEvent>(Web::UIEvents::EventNames::click, parent_node);

        Web::HTML::queue_a_task(Web::HTML::Task::Source::Unspecified, nullptr, nullptr, JS::create_heap_function(current_browsing_context().heap(), [on_complete]() {
            on_complete->function()(JsonValue {});
        }));
    }
    // -> Otherwise
    else {
        // 1. Let input state be the result of get the input state given current session and current top-level
        //    browsing context.
        auto& input_state = Web::WebDriver::get_input_state(*current_top_level_browsing_context());

        // 2. Let actions options be a new actions options with the is element origin steps set to represents a web
        //    element, and the get element origin steps set to get a WebElement origin.
        Web::WebDriver::ActionsOptions actions_options {
            .is_element_origin = &Web::WebDriver::represents_a_web_element,
            .get_element_origin = &Web::WebDriver::get_web_element_origin,
        };

        // 3. Let input id be a the result of generating a UUID.
        auto input_id = MUST(Web::Crypto::generate_random_uuid());

        // 4. Let source be the result of create an input source with input state, and "pointer".
        auto source = Web::WebDriver::create_input_source(input_state, Web::WebDriver::InputSourceType::Pointer, Web::WebDriver::PointerInputSource::Subtype::Mouse);

        // 5. Add an input source with input state, input id and source.
        Web::WebDriver::add_input_source(input_state, input_id, move(source));

        // 6. Let click point be the element’s in-view center point.
        // FIXME: Spec-issue: This parameter is unused. Note that it would not correct to set the mouse move action
        //        position to this click point. The [0,0] specified below is ultimately interpreted as an offset from
        //        the element's center position.
        //        https://github.com/w3c/webdriver/issues/1563

        // 7. Let pointer move action be an action object constructed with arguments input id, "pointer", and "pointerMove".
        Web::WebDriver::ActionObject pointer_move_action { input_id, Web::WebDriver::InputSourceType::Pointer, Web::WebDriver::ActionObject::Subtype::PointerMove };

        // 8. Set a property x to 0 on pointer move action.
        // 9. Set a property y to 0 on pointer move action.
        pointer_move_action.pointer_move_fields().position = { 0, 0 };

        // 10. Set a property origin to element on pointer move action.
        auto origin = Web::WebDriver::get_or_create_a_web_element_reference(*element);
        pointer_move_action.pointer_move_fields().origin = MUST(String::from_byte_string(origin));

        // 11. Let pointer down action be an action object constructed with arguments input id, "pointer", and "pointerDown".
        Web::WebDriver::ActionObject pointer_down_action { input_id, Web::WebDriver::InputSourceType::Pointer, Web::WebDriver::ActionObject::Subtype::PointerDown };

        // 12. Set a property button to 0 on pointer down action.
        pointer_down_action.pointer_up_down_fields().button = Web::UIEvents::button_code_to_mouse_button(0);

        // 13. Let pointer up action be an action object constructed with arguments input id, "pointer", and "pointerUp" as arguments.
        Web::WebDriver::ActionObject pointer_up_action { input_id, Web::WebDriver::InputSourceType::Pointer, Web::WebDriver::ActionObject::Subtype::PointerUp };

        // 14. Set a property button to 0 on pointer up action.
        pointer_up_action.pointer_up_down_fields().button = Web::UIEvents::button_code_to_mouse_button(0);

        // 15. Let actions be the list «pointer move action, pointer down action, pointer up action».
        Vector actions { move(pointer_move_action), move(pointer_down_action), move(pointer_up_action) };

        // 16. Dispatch a list of actions with input state, actions, current browsing context, and actions options.
        m_action_executor = Web::WebDriver::dispatch_list_of_actions(input_state, move(actions), current_browsing_context(), move(actions_options), JS::create_heap_function(current_browsing_context().heap(), [on_complete, &input_state, input_id = move(input_id)](Web::WebDriver::Response result) {
            // 17. Remove an input source with input state and input id.
            Web::WebDriver::remove_input_source(input_state, input_id);

            on_complete->function()(move(result));
        }));
    }

    // 13. Return success with data null.
    return JsonValue {};
}

// 12.5.2 Element Clear, https://w3c.github.io/webdriver/#dfn-element-clear
Messages::WebDriverClient::ElementClearResponse WebDriverConnection::element_clear(String const& element_id)
{
    // https://w3c.github.io/webdriver/#dfn-clear-a-content-editable-element
    auto clear_content_editable_element = [&](Web::DOM::Element& element) {
        // 1. If element's innerHTML IDL attribute is an empty string do nothing and return.
        if (auto result = element.inner_html(); result.is_error() || result.value().is_empty())
            return;

        // 2. Run the focusing steps for element.
        Web::HTML::run_focusing_steps(&element);

        // 3. Set element's innerHTML IDL attribute to an empty string.
        (void)element.set_inner_html({});

        // 4. Run the unfocusing steps for the element.
        Web::HTML::run_unfocusing_steps(&element);
    };

    // https://w3c.github.io/webdriver/#dfn-clear-a-resettable-element
    auto clear_resettable_element = [&](Web::DOM::Element& element) {
        VERIFY(is<Web::HTML::FormAssociatedElement>(element));
        auto& form_associated_element = dynamic_cast<Web::HTML::FormAssociatedElement&>(element);

        // 1. Let empty be the result of the first matching condition:
        auto empty = [&]() {
            // -> element is an input element whose type attribute is in the File Upload state
            //    True if the list of selected files has a length of 0, and false otherwise
            if (is<Web::HTML::HTMLInputElement>(element)) {
                auto& input_element = static_cast<Web::HTML::HTMLInputElement&>(element);

                if (input_element.type_state() == Web::HTML::HTMLInputElement::TypeAttributeState::FileUpload)
                    return input_element.files()->length() == 0;
            }

            // -> otherwise
            //    True if its value IDL attribute is an empty string, and false otherwise.
            return form_associated_element.value().is_empty();
        }();

        // 2. If element is a candidate for constraint validation it satisfies its constraints, and empty is true,
        //    abort these substeps.
        // FIXME: Implement constraint validation.
        if (empty)
            return;

        // 3. Invoke the focusing steps for element.
        Web::HTML::run_focusing_steps(&element);

        // 4. Invoke the clear algorithm for element.
        form_associated_element.clear_algorithm();

        // 5. Invoke the unfocusing steps for the element.
        Web::HTML::run_unfocusing_steps(&element);
    };

    // 1. If session's current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Try to handle any user prompts with session.
    TRY(handle_any_user_prompts());

    // 3. Let element be the result of trying to get a known element with session and element id.
    auto element = TRY(Web::WebDriver::get_known_element(element_id));

    // 4. If element is not editable, return an error with error code invalid element state.
    if (!Web::WebDriver::is_element_editable(*element))
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidElementState, "Element is not editable"sv);

    // 5. Scroll into view the element.
    TRY(scroll_element_into_view(*element));

    // FIXME: 6. Let timeout be session's session timeouts' implicit wait timeout.
    // FIXME: 7. Let timer be a new timer.
    // FIXME: 8. If timeout is not null:
    {
        // FIXME: 1. Start the timer with timer and timeout.
    }
    // FIXME: 9. Wait for element to become interactable, or timer's timeout fired flag to be set, whichever occurs first.

    // 10. If element is not interactable, return error with error code element not interactable.
    if (!Web::WebDriver::is_element_interactable(current_browsing_context(), *element))
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::ElementNotInteractable, "Element is not interactable"sv);

    // 11. Run the substeps of the first matching statement:
    // -> element is a mutable form control element
    if (Web::WebDriver::is_element_mutable_form_control(*element)) {
        // Invoke the steps to clear a resettable element.
        clear_resettable_element(*element);
    }
    // -> element is a mutable element
    else if (Web::WebDriver::is_element_mutable(*element)) {
        // Invoke the steps to clear a content editable element.
        clear_content_editable_element(*element);
    }
    // -> otherwise
    else {
        // Return error with error code invalid element state.
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidElementState, "Element is not editable"sv);
    }

    // 12. Return success with data null.
    return JsonValue {};
}

// 12.5.3 Element Send Keys, https://w3c.github.io/webdriver/#dfn-element-send-keys
Messages::WebDriverClient::ElementSendKeysResponse WebDriverConnection::element_send_keys(String const& element_id, JsonValue const& payload)
{
    // 1. Let text be the result of getting a property named "text" from parameters.
    // 2. If text is not a String, return an error with error code invalid argument.
    auto text = TRY(Web::WebDriver::get_property(payload, "text"sv));

    // 3. If session's current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 4. Try to handle any user prompts with session.
    TRY(handle_any_user_prompts());

    // 5. Let element be the result of trying to get a known element with session and URL variables[element id].
    auto element = TRY(Web::WebDriver::get_known_element(element_id));

    // 6. Let file be true if element is input element in the file upload state, or false otherwise.
    auto file = is<Web::HTML::HTMLInputElement>(*element) && static_cast<Web::HTML::HTMLInputElement&>(*element).type_state() == Web::HTML::HTMLInputElement::TypeAttributeState::FileUpload;

    // 7. If file is false or the session's strict file interactability, is true run the following substeps:
    if (!file || m_strict_file_interactability) {
        // 1. Scroll into view the element.
        TRY(scroll_element_into_view(*element));

        // FIXME: 2. Let timeout be session's session timeouts' implicit wait timeout.
        // FIXME: 3. Let timer be a new timer.
        // FIXME: 4. If timeout is not null:
        {
            // FIXME: 1. Start the timer with timer and timeout.
        }
        // FIXME: 5. Wait for element to become keyboard-interactable, or timer's timeout fired flag to be set, whichever occurs first.

        // 6. If element is not keyboard-interactable, return error with error code element not interactable.
        if (!Web::WebDriver::is_element_keyboard_interactable(*element))
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::ElementNotInteractable, "Element is not keyboard-interactable"sv);

        // 7. If element is not the active element run the focusing steps for the element.
        if (!element->is_active())
            Web::HTML::run_focusing_steps(element);
    }

    // 8. Run the substeps of the first matching condition:

    // -> file is true
    if (file) {
        auto& input_element = static_cast<Web::HTML::HTMLInputElement&>(*element);

        // 1. Let files be the result of splitting text on the newline (\n) character.
        auto files = text.split('\n');

        // 2. If files is of 0 length, return an error with error code invalid argument.
        if (files.is_empty())
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, "File list is empty"sv);

        // 3. Let multiple equal the result of calling hasAttribute() with "multiple" on element.
        auto multiple = input_element.has_attribute(Web::HTML::AttributeNames::multiple);

        // 4. if multiple is false and the length of files is not equal to 1, return an error with error code invalid argument.
        if (!multiple && files.size() != 1)
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, "Element does not accept multiple files"sv);

        // 5. Verify that each file given by the user exists. If any do not, return error with error code invalid argument.
        // 6. Complete implementation specific steps equivalent to setting the selected files on the input element. If
        //    multiple is true files are be appended to element's selected files.
        auto create_selected_file = [](auto const& path) -> ErrorOr<Web::HTML::SelectedFile> {
            auto file = TRY(Core::File::open(path, Core::File::OpenMode::Read));
            auto contents = TRY(file->read_until_eof());

            return Web::HTML::SelectedFile { LexicalPath::basename(path), move(contents) };
        };

        Vector<Web::HTML::SelectedFile> selected_files;
        selected_files.ensure_capacity(files.size());

        for (auto const& path : files) {
            auto selected_file = create_selected_file(path);
            if (selected_file.is_error())
                return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, ByteString::formatted("'{}' does not exist", path));

            selected_files.unchecked_append(selected_file.release_value());
        }

        input_element.did_select_files(selected_files, Web::HTML::HTMLInputElement::MultipleHandling::Append);

        // 7. Fire these events in order on element:
        //     1. input
        //     2. change
        // NOTE: These events are fired by `did_select_files` as an element task. So instead of firing them here, we spin
        //       the event loop once before informing the client that the action is complete.
        Web::HTML::queue_a_task(Web::HTML::Task::Source::Unspecified, nullptr, nullptr, JS::create_heap_function(current_browsing_context().heap(), [this]() {
            async_actions_performed(JsonValue {});
        }));

        // 8. Return success with data null.
        return JsonValue {};
    }
    // -> element is a non-typeable form control
    else if (Web::WebDriver::is_element_non_typeable_form_control(*element)) {
        // 1. If element does not have an own property named value return an error with error code element not interactable
        if (!is<Web::HTML::HTMLInputElement>(*element))
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::ElementNotInteractable, "Element does not have a property named 'value'"sv);

        auto& input_element = static_cast<Web::HTML::HTMLInputElement&>(*element);

        // 2. If element is not mutable return an error with error code element not interactable.
        if (input_element.is_mutable())
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::ElementNotInteractable, "Element is immutable"sv);

        // 3. Set a property value to text on element.
        MUST(input_element.set_value(MUST(String::from_byte_string(text))));

        // FIXME: 4. If element is suffering from bad input return an error with error code invalid argument.

        // 5. Return success with data null.
        async_actions_performed(JsonValue {});
        return JsonValue {};
    }
    // -> element is content editable
    else if (is<Web::HTML::HTMLElement>(*element) && static_cast<Web::HTML::HTMLElement&>(*element).is_content_editable()) {
        // If element does not currently have focus, set the text insertion caret after any child content.
        if (!element->is_focused())
            element->document().set_cursor_position(Web::DOM::Position::create(element->realm(), *element, element->length()));
    }
    // -> otherwise
    else if (is<Web::HTML::FormAssociatedTextControlElement>(*element)) {
        Optional<Web::HTML::FormAssociatedTextControlElement&> target;

        if (is<Web::HTML::HTMLInputElement>(*element))
            target = static_cast<Web::HTML::HTMLInputElement&>(*element);
        else if (is<Web::HTML::HTMLTextAreaElement>(*element))
            target = static_cast<Web::HTML::HTMLTextAreaElement&>(*element);

        // NOTE: The spec doesn't dictate this, but these steps only make sense for form-associated text elements.
        if (target.has_value()) {
            // 1. If element does not currently have focus, let current text length be the length of element's API value.
            Optional<Web::WebIDL::UnsignedLong> current_text_length;

            if (element->is_focused()) {
                auto api_value = target->relevant_value();

                // FIXME: This should be a UTF-16 code unit length, but `set_the_selection_range` is also currently
                //        implemented in terms of code point length.
                current_text_length = api_value.code_points().length();
            }

            // 2. Set the text insertion caret using set selection range using current text length for both the start
            //    and end parameters.
            (void)target->set_selection_range(current_text_length, current_text_length, {});
        }
    }

    // 9. Let input state be the result of get the input state with session and session's current top-level browsing context.
    auto& input_state = Web::WebDriver::get_input_state(*current_top_level_browsing_context());

    // 10. Let input id be a the result of generating a UUID.
    auto input_id = MUST(Web::Crypto::generate_random_uuid());

    // 11. Let source be the result of create an input source with input state, and "key".
    auto source = Web::WebDriver::create_input_source(input_state, Web::WebDriver::InputSourceType::Key, {});

    // 12. Add an input source with input state, input id and source.
    Web::WebDriver::add_input_source(input_state, input_id, move(source));

    // 13. Dispatch actions for a string with arguments input state, input id, and source, text, and session's current browsing context.
    m_action_executor = Web::WebDriver::dispatch_actions_for_a_string(input_state, input_id, source, text, current_browsing_context(), JS::create_heap_function(current_browsing_context().heap(), [this, &input_state, input_id](Web::WebDriver::Response result) {
        m_action_executor = nullptr;

        // 14. Remove an input source with input state and input id.
        Web::WebDriver::remove_input_source(input_state, input_id);

        async_actions_performed(move(result));
    }));

    // 15. Return success with data null.
    return JsonValue {};
}

// 13.1 Get Page Source, https://w3c.github.io/webdriver/#dfn-get-page-source
Messages::WebDriverClient::GetSourceResponse WebDriverConnection::get_source()
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    auto* document = current_browsing_context().active_document();
    Optional<ByteString> source;

    // 3. Let source be the result of invoking the fragment serializing algorithm on a fictional node whose only child is the document element providing true for the require well-formed flag. If this causes an exception to be thrown, let source be null.
    if (auto result = document->serialize_fragment(Web::DOMParsing::RequireWellFormed::Yes); !result.is_error())
        source = result.release_value().to_byte_string();

    // 4. Let source be the result of serializing to string the current browsing context active document, if source is null.
    if (!source.has_value())
        source = MUST(document->serialize_fragment(Web::DOMParsing::RequireWellFormed::No)).to_byte_string();

    // 5. Return success with data source.
    return source.release_value();
}

// 13.2.1 Execute Script, https://w3c.github.io/webdriver/#dfn-execute-script
Messages::WebDriverClient::ExecuteScriptResponse WebDriverConnection::execute_script(JsonValue const& payload)
{
    auto* window = current_browsing_context().active_window();
    auto& vm = window->vm();

    // 1. Let body and arguments be the result of trying to extract the script arguments from a request with argument parameters.
    auto [body, arguments] = TRY(extract_the_script_arguments_from_a_request(vm, payload));

    // 2. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 3. Handle any user prompts, and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 4. Let timeout be session's session timeouts' script timeout.
    auto timeout_ms = m_timeouts_configuration.script_timeout;

    // This handles steps 5 to 9 and produces the appropriate result type for the following steps.
    Web::WebDriver::execute_script(current_browsing_context(), move(body), move(arguments), timeout_ms, JS::create_heap_function(vm.heap(), [&](Web::WebDriver::ExecuteScriptResultSerialized result) {
        dbgln_if(WEBDRIVER_DEBUG, "Executing script returned: {}", result.value);
        Web::WebDriver::Response response;

        switch (result.type) {
        // 10. If promise is still pending and the session script timeout is reached, return error with error code script timeout.
        case Web::WebDriver::ExecuteScriptResultType::Timeout:
            response = Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::ScriptTimeoutError, "Script timed out");
            break;
        // 11. Upon fulfillment of promise with value v, let result be a JSON clone of v, and return success with data result.
        case Web::WebDriver::ExecuteScriptResultType::PromiseResolved:
            response = move(result.value);
            break;
        // 12. Upon rejection of promise with reason r, let result be a JSON clone of r, and return error with error code javascript error and data result.
        case Web::WebDriver::ExecuteScriptResultType::PromiseRejected:
        case Web::WebDriver::ExecuteScriptResultType::JavaScriptError:
            response = Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::JavascriptError, "Script returned an error", move(result.value));
            break;
        case Web::WebDriver::ExecuteScriptResultType::BrowsingContextDiscarded:
            response = Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::StaleElementReference, "Browsing context has been discarded", move(result.value));
            break;
        case Web::WebDriver::ExecuteScriptResultType::StaleElement:
            response = Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::StaleElementReference, "Referenced element has become stale", move(result.value));
            break;
        }

        async_script_executed(move(response));
    }));

    return JsonValue {};
}

// 13.2.2 Execute Async Script, https://w3c.github.io/webdriver/#dfn-execute-async-script
Messages::WebDriverClient::ExecuteAsyncScriptResponse WebDriverConnection::execute_async_script(JsonValue const& payload)
{
    auto* window = current_browsing_context().active_window();
    auto& vm = window->vm();

    // 1. Let body and arguments by the result of trying to extract the script arguments from a request with argument parameters.
    auto [body, arguments] = TRY(extract_the_script_arguments_from_a_request(vm, payload));

    // 2. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 3. Handle any user prompts, and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 4. Let timeout be session's session timeouts' script timeout.
    auto timeout_ms = m_timeouts_configuration.script_timeout;

    // This handles steps 5 to 9 and produces the appropriate result type for the following steps.
    Web::WebDriver::execute_async_script(current_browsing_context(), move(body), move(arguments), timeout_ms, JS::create_heap_function(vm.heap(), [&](Web::WebDriver::ExecuteScriptResultSerialized result) {
        dbgln_if(WEBDRIVER_DEBUG, "Executing async script returned: {}", result.value);
        Web::WebDriver::Response response;

        switch (result.type) {
        // 10. If promise is still pending and the session script timeout is reached, return error with error code script timeout.
        case Web::WebDriver::ExecuteScriptResultType::Timeout:
            response = Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::ScriptTimeoutError, "Script timed out");
            break;
        // 11. Upon fulfillment of promise with value v, let result be a JSON clone of v, and return success with data result.
        case Web::WebDriver::ExecuteScriptResultType::PromiseResolved:
            response = move(result.value);
            break;
        // 12. Upon rejection of promise with reason r, let result be a JSON clone of r, and return error with error code javascript error and data result.
        case Web::WebDriver::ExecuteScriptResultType::PromiseRejected:
        case Web::WebDriver::ExecuteScriptResultType::JavaScriptError:
            response = Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::JavascriptError, "Script returned an error", move(result.value));
            break;
        case Web::WebDriver::ExecuteScriptResultType::BrowsingContextDiscarded:
            response = Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::StaleElementReference, "Browsing context has been discarded", move(result.value));
            break;
        case Web::WebDriver::ExecuteScriptResultType::StaleElement:
            response = Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::StaleElementReference, "Referenced element has become stale", move(result.value));
            break;
        }

        async_script_executed(move(response));
    }));

    return JsonValue {};
}

// 14.1 Get All Cookies, https://w3c.github.io/webdriver/#dfn-get-all-cookies
Messages::WebDriverClient::GetAllCookiesResponse WebDriverConnection::get_all_cookies()
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts, and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let cookies be a new JSON List.
    JsonArray cookies;

    // 4. For each cookie in all associated cookies of the current browsing context’s active document:
    auto* document = current_browsing_context().active_document();

    for (auto const& cookie : current_browsing_context().page().client().page_did_request_all_cookies(document->url())) {
        // 1. Let serialized cookie be the result of serializing cookie.
        auto serialized_cookie = serialize_cookie(cookie);

        // 2. Append serialized cookie to cookies
        TRY(cookies.append(move(serialized_cookie)));
    }

    // 5. Return success with data cookies.
    return cookies;
}

// 14.2 Get Named Cookie, https://w3c.github.io/webdriver/#dfn-get-named-cookie
Messages::WebDriverClient::GetNamedCookieResponse WebDriverConnection::get_named_cookie(String const& name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts, and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. If the url variable name is equal to a cookie’s cookie name amongst all associated cookies of the current browsing context’s active document, return success with the serialized cookie as data.
    auto* document = current_browsing_context().active_document();

    if (auto cookie = current_browsing_context().page().client().page_did_request_named_cookie(document->url(), name); cookie.has_value()) {
        auto serialized_cookie = serialize_cookie(*cookie);
        return serialized_cookie;
    }

    // 4. Otherwise, return error with error code no such cookie.
    return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchCookie, ByteString::formatted("Cookie '{}' not found", name));
}

// 14.3 Add Cookie, https://w3c.github.io/webdriver/#dfn-adding-a-cookie
Messages::WebDriverClient::AddCookieResponse WebDriverConnection::add_cookie(JsonValue const& payload)
{
    // 1. Let data be the result of getting a property named cookie from the parameters argument.
    auto const& data = *TRY(Web::WebDriver::get_property<JsonObject const*>(payload, "cookie"sv));

    // 2. If data is not a JSON Object with all the required (non-optional) JSON keys listed in the table for cookie conversion, return error with error code invalid argument.
    // NOTE: This validation is performed in subsequent steps.

    // 3. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 4. Handle any user prompts, and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // FIXME: 5. If the current browsing context’s document element is a cookie-averse Document object, return error with error code invalid cookie domain.

    // 6. If cookie name or cookie value is null, cookie domain is not equal to the current browsing context’s active document’s domain, cookie secure only or cookie HTTP only are not boolean types, or cookie expiry time is not an integer type, or it less than 0 or greater than the maximum safe integer, return error with error code invalid argument.
    // NOTE: This validation is either performed in subsequent steps, or is performed by the CookieJar (namely domain matching).

    // 7. Create a cookie in the cookie store associated with the active document’s address using cookie name name, cookie value value, and an attribute-value list of the following cookie concepts listed in the table for cookie conversion from data:
    Web::Cookie::ParsedCookie cookie {};
    cookie.name = MUST(String::from_byte_string(TRY(Web::WebDriver::get_property(data, "name"sv))));
    cookie.value = MUST(String::from_byte_string(TRY(Web::WebDriver::get_property(data, "value"sv))));

    // Cookie path
    //     The value if the entry exists, otherwise "/".
    if (data.has("path"sv))
        cookie.path = MUST(String::from_byte_string(TRY(Web::WebDriver::get_property(data, "path"sv))));
    else
        cookie.path = "/"_string;

    // Cookie domain
    //     The value if the entry exists, otherwise the current browsing context’s active document’s URL domain.
    // NOTE: The otherwise case is handled by the CookieJar
    if (data.has("domain"sv))
        cookie.domain = MUST(String::from_byte_string(TRY(Web::WebDriver::get_property(data, "domain"sv))));

    // Cookie secure only
    //     The value if the entry exists, otherwise false.
    if (data.has("secure"sv))
        cookie.secure_attribute_present = TRY(Web::WebDriver::get_property<bool>(data, "secure"sv));

    // Cookie HTTP only
    //     The value if the entry exists, otherwise false.
    if (data.has("httpOnly"sv))
        cookie.http_only_attribute_present = TRY(Web::WebDriver::get_property<bool>(data, "httpOnly"sv));

    // Cookie expiry time
    //     The value if the entry exists, otherwise leave unset to indicate that this is a session cookie.
    if (data.has("expiry"sv)) {
        // NOTE: less than 0 or greater than safe integer are handled by the JSON parser
        auto expiry = TRY(Web::WebDriver::get_property<u32>(data, "expiry"sv));
        cookie.expiry_time_from_expires_attribute = UnixDateTime::from_seconds_since_epoch(expiry);
    }

    // Cookie same site
    //     The value if the entry exists, otherwise leave unset to indicate that no same site policy is defined.
    if (data.has("sameSite"sv)) {
        auto same_site = TRY(Web::WebDriver::get_property(data, "sameSite"sv));
        cookie.same_site_attribute = Web::Cookie::same_site_from_string(same_site);
    }

    auto* document = current_browsing_context().active_document();
    current_browsing_context().page().client().page_did_set_cookie(document->url(), cookie, Web::Cookie::Source::Http);

    // If there is an error during this step, return error with error code unable to set cookie.
    // NOTE: This probably should only apply to the actual setting of the cookie in the Browser, which cannot fail in our case.

    // 8. Return success with data null.
    return JsonValue {};
}

// 14.4 Delete Cookie, https://w3c.github.io/webdriver/#dfn-delete-cookie
Messages::WebDriverClient::DeleteCookieResponse WebDriverConnection::delete_cookie(String const& name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts, and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Delete cookies using the url variable name parameter as the filter argument.
    delete_cookies(name);

    // 4. Return success with data null.
    return JsonValue {};
}

// 14.5 Delete All Cookies, https://w3c.github.io/webdriver/#dfn-delete-all-cookies
Messages::WebDriverClient::DeleteAllCookiesResponse WebDriverConnection::delete_all_cookies()
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Handle any user prompts, and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Delete cookies, giving no filtering argument.
    delete_cookies();

    // 4. Return success with data null.
    return JsonValue {};
}

// 15.7 Perform Actions, https://w3c.github.io/webdriver/#perform-actions
Messages::WebDriverClient::PerformActionsResponse WebDriverConnection::perform_actions(JsonValue const& payload)
{
    // 4. If session's current browsing context is no longer open, return error with error code no such window.
    // NOTE: We do this first so we can assume the current top-level browsing context below is non-null.
    TRY(ensure_current_browsing_context_is_open());

    // 1. Let input state be the result of get the input state with session and session's current top-level browsing context.
    auto& input_state = Web::WebDriver::get_input_state(*current_top_level_browsing_context());

    // 2. Let actions options be a new actions options with the is element origin steps set to represents a web element,
    //    and the get element origin steps set to get a WebElement origin.
    Web::WebDriver::ActionsOptions actions_options {
        .is_element_origin = &Web::WebDriver::represents_a_web_element,
        .get_element_origin = &Web::WebDriver::get_web_element_origin,
    };

    // 3. Let actions by tick be the result of trying to extract an action sequence with input state, parameters, and
    //    actions options.
    auto actions_by_tick = TRY(Web::WebDriver::extract_an_action_sequence(input_state, payload, actions_options));

    // 5. Try to handle any user prompts with session.
    TRY(handle_any_user_prompts());

    // 6. Dispatch actions with input state, actions by tick, current browsing context, and actions options. If this
    //    results in an error return that error.
    auto on_complete = JS::create_heap_function(current_browsing_context().heap(), [this](Web::WebDriver::Response result) {
        m_action_executor = nullptr;
        async_actions_performed(move(result));
    });

    m_action_executor = Web::WebDriver::dispatch_actions(input_state, move(actions_by_tick), current_browsing_context(), move(actions_options), on_complete);

    // 7. Return success with data null.
    return JsonValue {};
}

// 15.8 Release Actions, https://w3c.github.io/webdriver/#release-actions
Messages::WebDriverClient::ReleaseActionsResponse WebDriverConnection::release_actions()
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_browsing_context_is_open());

    // 2. Let input state be the result of get the input state with current session and current top-level browsing context.
    auto& input_state = Web::WebDriver::get_input_state(*current_top_level_browsing_context());

    // 3. Let actions options be a new actions options with the is element origin steps set to represents a web element,
    //    and the get element origin steps set to get a WebElement origin.
    Web::WebDriver::ActionsOptions actions_options {
        .is_element_origin = &Web::WebDriver::represents_a_web_element,
        .get_element_origin = &Web::WebDriver::get_web_element_origin,
    };

    // 4. Let undo actions be input state’s input cancel list in reverse order.
    auto undo_actions = input_state.input_cancel_list;
    undo_actions.reverse();

    // 5. Try to dispatch tick actions with arguments undo actions, 0, current browsing context, and actions options.
    TRY(Web::WebDriver::dispatch_tick_actions(input_state, undo_actions, AK::Duration::zero(), current_browsing_context(), actions_options));

    // 6. Reset the input state with current session and current top-level browsing context.
    Web::WebDriver::reset_input_state(*current_top_level_browsing_context());

    // 7. Return success with data null.
    return JsonValue {};
}

// 16.1 Dismiss Alert, https://w3c.github.io/webdriver/#dismiss-alert
Messages::WebDriverClient::DismissAlertResponse WebDriverConnection::dismiss_alert()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 2. If there is no current user prompt, return error with error code no such alert.
    if (!current_browsing_context().page().has_pending_dialog())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchAlert, "No user dialog is currently open"sv);

    // 3. Dismiss the current user prompt.
    current_browsing_context().page().dismiss_dialog(JS::create_heap_function(current_browsing_context().heap(), [this]() {
        async_dialog_closed(JsonValue {});
    }));

    // 4. Return success with data null.
    return JsonValue {};
}

// 16.2 Accept Alert, https://w3c.github.io/webdriver/#accept-alert
Messages::WebDriverClient::AcceptAlertResponse WebDriverConnection::accept_alert()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 2. If there is no current user prompt, return error with error code no such alert.
    if (!current_browsing_context().page().has_pending_dialog())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchAlert, "No user dialog is currently open"sv);

    // 3. Accept the current user prompt.
    current_browsing_context().page().accept_dialog(JS::create_heap_function(current_browsing_context().heap(), [this]() {
        async_dialog_closed(JsonValue {});
    }));

    // 4. Return success with data null.
    return JsonValue {};
}

// 16.3 Get Alert Text, https://w3c.github.io/webdriver/#get-alert-text
Messages::WebDriverClient::GetAlertTextResponse WebDriverConnection::get_alert_text()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 2. If there is no current user prompt, return error with error code no such alert.
    if (!current_browsing_context().page().has_pending_dialog())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchAlert, "No user dialog is currently open"sv);

    // 3. Let message be the text message associated with the current user prompt, or otherwise be null.
    auto const& message = current_browsing_context().page().pending_dialog_text();

    // 4. Return success with data message.
    if (message.has_value())
        return message->to_byte_string();
    return JsonValue {};
}

// 16.4 Send Alert Text, https://w3c.github.io/webdriver/#send-alert-text
Messages::WebDriverClient::SendAlertTextResponse WebDriverConnection::send_alert_text(JsonValue const& payload)
{
    // 1. Let text be the result of getting the property "text" from parameters.
    // 2. If text is not a String, return error with error code invalid argument.
    auto text = TRY(Web::WebDriver::get_property(payload, "text"sv));

    // 3. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 4. If there is no current user prompt, return error with error code no such alert.
    if (!current_browsing_context().page().has_pending_dialog())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchAlert, "No user dialog is currently open"sv);

    // 5. Run the substeps of the first matching current user prompt:
    switch (current_browsing_context().page().pending_dialog()) {
    // -> alert
    // -> confirm
    case Web::Page::PendingDialog::Alert:
    case Web::Page::PendingDialog::Confirm:
        // Return error with error code element not interactable.
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::ElementNotInteractable, "Only prompt dialogs may receive text"sv);

    // -> prompt
    case Web::Page::PendingDialog::Prompt:
        // Do nothing.
        break;

    // -> Otherwise
    default:
        // Return error with error code unsupported operation.
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::UnsupportedOperation, "Unknown dialog type"sv);
    }

    // 6. Perform user agent dependent steps to set the value of current user prompt’s text field to text.
    current_browsing_context().page().client().page_did_request_set_prompt_text(TRY(String::from_byte_string(text)));

    // 7. Return success with data null.
    return JsonValue {};
}

// 17.1 Take Screenshot, https://w3c.github.io/webdriver/#take-screenshot
Messages::WebDriverClient::TakeScreenshotResponse WebDriverConnection::take_screenshot()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 2. When the user agent is next to run the animation frame callbacks:
    //     a. Let root rect be the current top-level browsing context’s document element’s rectangle.
    //     b. Let screenshot result be the result of trying to call draw a bounding box from the framebuffer, given root rect as an argument.
    //     c. Let canvas be a canvas element of screenshot result’s data.
    //     d. Let encoding result be the result of trying encoding a canvas as Base64 canvas.
    //     e. Let encoded string be encoding result’s data.
    auto* document = current_top_level_browsing_context()->active_document();
    auto root_rect = calculate_absolute_rect_of_element(*document->document_element());

    auto encoded_string = TRY(Web::WebDriver::capture_element_screenshot(
        [&](auto const& rect, auto& bitmap) { current_top_level_browsing_context()->page().client().paint(rect.template to_type<Web::DevicePixels>(), bitmap); },
        current_top_level_browsing_context()->page(),
        *document->document_element(),
        root_rect));

    // 3. Return success with data encoded string.
    return encoded_string;
}

// 17.2 Take Element Screenshot, https://w3c.github.io/webdriver/#dfn-take-element-screenshot
Messages::WebDriverClient::TakeElementScreenshotResponse WebDriverConnection::take_element_screenshot(String const& element_id)
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_current_top_level_browsing_context_is_open());

    // 2. Handle any user prompts and return its value if it is an error.
    TRY(handle_any_user_prompts());

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto element = TRY(Web::WebDriver::get_known_element(element_id));

    // 4. Scroll into view the element.
    (void)scroll_element_into_view(*element);

    // 5. When the user agent is next to run the animation frame callbacks:
    //     a. Let element rect be element’s rectangle.
    //     b. Let screenshot result be the result of trying to call draw a bounding box from the framebuffer, given element rect as an argument.
    //     c. Let canvas be a canvas element of screenshot result’s data.
    //     d. Let encoding result be the result of trying encoding a canvas as Base64 canvas.
    //     e. Let encoded string be encoding result’s data.
    auto element_rect = calculate_absolute_rect_of_element(*element);

    auto encoded_string = TRY(Web::WebDriver::capture_element_screenshot(
        [&](auto const& rect, auto& bitmap) { current_top_level_browsing_context()->page().client().paint(rect.template to_type<Web::DevicePixels>(), bitmap); },
        current_top_level_browsing_context()->page(),
        *element,
        element_rect));

    // 6. Return success with data encoded string.
    return encoded_string;
}

// 18.1 Print Page, https://w3c.github.io/webdriver/#dfn-print-page
Messages::WebDriverClient::PrintPageResponse WebDriverConnection::print_page(JsonValue const& payload)
{
    dbgln("FIXME: WebDriverConnection::print_page({})", payload);
    return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::UnsupportedOperation, "Print not implemented"sv);
}

// https://w3c.github.io/webdriver/#dfn-set-the-current-browsing-context
void WebDriverConnection::set_current_browsing_context(Web::HTML::BrowsingContext& browsing_context)
{
    // 1. Set session's current browsing context to context.
    m_current_browsing_context = browsing_context;

    // 2. Set the session's current parent browsing context to the parent browsing context of context, if that context
    //    exists, or null otherwise.
    if (auto navigable = browsing_context.active_document()->navigable(); navigable && navigable->parent())
        m_current_parent_browsing_context = navigable->parent()->active_browsing_context();
    else
        m_current_parent_browsing_context = nullptr;
}

// https://w3c.github.io/webdriver/#dfn-set-the-current-browsing-context
void WebDriverConnection::set_current_top_level_browsing_context(Web::HTML::BrowsingContext& browsing_context)
{
    // 1. Assert: context is a top-level browsing context.
    VERIFY(browsing_context.is_top_level());

    // 2. Set session's current top-level browsing context to context.
    m_current_top_level_browsing_context = browsing_context;

    // 3. Set the current browsing context with session and context.
    set_current_browsing_context(browsing_context);
}

Messages::WebDriverClient::EnsureTopLevelBrowsingContextIsOpenResponse WebDriverConnection::ensure_top_level_browsing_context_is_open()
{
    TRY(ensure_current_top_level_browsing_context_is_open());
    return JsonValue {};
}

ErrorOr<void, Web::WebDriver::Error> WebDriverConnection::ensure_current_browsing_context_is_open()
{
    return ensure_browsing_context_is_open(current_browsing_context());
}

ErrorOr<void, Web::WebDriver::Error> WebDriverConnection::ensure_current_top_level_browsing_context_is_open()
{
    return ensure_browsing_context_is_open(current_top_level_browsing_context());
}

// https://w3c.github.io/webdriver/#dfn-handle-any-user-prompts
ErrorOr<void, Web::WebDriver::Error> WebDriverConnection::handle_any_user_prompts()
{
    // 1. If there is no current user prompt, abort these steps and return success.
    if (!current_browsing_context().page().has_pending_dialog())
        return {};

    // 2. Perform the following substeps based on the current session’s user prompt handler:
    switch (m_unhandled_prompt_behavior) {
    // -> dismiss state
    case Web::WebDriver::UnhandledPromptBehavior::Dismiss:
        // Dismiss the current user prompt.
        current_browsing_context().page().dismiss_dialog();
        break;

    // -> accept state
    case Web::WebDriver::UnhandledPromptBehavior::Accept:
        // Accept the current user prompt.
        current_browsing_context().page().accept_dialog();
        break;

    // -> dismiss and notify state
    case Web::WebDriver::UnhandledPromptBehavior::DismissAndNotify:
        // Dismiss the current user prompt.
        current_browsing_context().page().dismiss_dialog();

        // Return an annotated unexpected alert open error.
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::UnexpectedAlertOpen, "A user dialog is open"sv);

    // -> accept and notify state
    case Web::WebDriver::UnhandledPromptBehavior::AcceptAndNotify:
        // Accept the current user prompt.
        current_browsing_context().page().accept_dialog();

        // Return an annotated unexpected alert open error.
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::UnexpectedAlertOpen, "A user dialog is open"sv);

    // -> ignore state
    case Web::WebDriver::UnhandledPromptBehavior::Ignore:
        // Return an annotated unexpected alert open error.
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::UnexpectedAlertOpen, "A user dialog is open"sv);
    }

    // 3. Return success.
    return {};
}

// https://w3c.github.io/webdriver/#dfn-wait-for-navigation-to-complete
// FIXME: Update this AO to the latest spec steps.
void WebDriverConnection::wait_for_navigation_to_complete(OnNavigationComplete on_complete)
{
    // 1. If the current session has a page loading strategy of none, return success with data null.
    if (m_page_load_strategy == Web::WebDriver::PageLoadStrategy::None) {
        on_complete->function()(JsonValue {});
        return;
    }

    // 2. If the current browsing context is no longer open, return success with data null.
    if (ensure_browsing_context_is_open(current_browsing_context()).is_error()) {
        on_complete->function()(JsonValue {});
        return;
    }

    auto& realm = current_browsing_context().active_document()->realm();
    auto navigable = current_browsing_context().active_document()->navigable();

    if (!navigable || navigable->ongoing_navigation().has<Empty>()) {
        on_complete->function()(JsonValue {});
        return;
    }

    auto reset_observers = [](auto& self) {
        if (self.m_navigation_observer) {
            self.m_navigation_observer->set_navigation_complete({});
            self.m_navigation_observer = nullptr;
        }
        if (self.m_document_observer) {
            self.m_document_observer->set_document_readiness_observer({});
            self.m_document_observer = nullptr;
        }
    };

    // 3. Start a timer. If this algorithm has not completed before timer reaches the session’s session page load timeout
    //    in milliseconds, return an error with error code timeout.
    m_navigation_timer = realm.heap().allocate<Web::WebDriver::HeapTimer>(realm);

    // 4. If there is an ongoing attempt to navigate the current browsing context that has not yet matured, wait for
    //    navigation to mature.
    m_navigation_observer = realm.heap().allocate<Web::HTML::NavigationObserver>(realm, realm, *navigable);

    m_navigation_observer->set_navigation_complete([this, &realm, reset_observers]() {
        reset_observers(*this);

        // 5. Let readiness target be the document readiness state associated with the current session’s page loading
        //    strategy, which can be found in the table of page load strategies.
        auto readiness_target = [this]() {
            switch (m_page_load_strategy) {
            case Web::WebDriver::PageLoadStrategy::Normal:
                return Web::HTML::DocumentReadyState::Complete;
            case Web::WebDriver::PageLoadStrategy::Eager:
                return Web::HTML::DocumentReadyState::Interactive;
            default:
                VERIFY_NOT_REACHED();
            };
        }();

        // 6. Wait for the current browsing context’s document readiness state to reach readiness target,
        //    or for the session page load timeout to pass, whichever occurs sooner.
        if (auto* document = current_browsing_context().active_document(); document->readiness() != readiness_target) {
            m_document_observer = realm.heap().allocate<Web::DOM::DocumentObserver>(realm, realm, *document);

            m_document_observer->set_document_readiness_observer([this, readiness_target](Web::HTML::DocumentReadyState readiness) {
                if (readiness == readiness_target)
                    m_navigation_timer->stop_and_fire_timeout_handler();
            });
        } else {
            m_navigation_timer->stop_and_fire_timeout_handler();
        }
    });

    m_navigation_timer->start(m_timeouts_configuration.page_load_timeout.value_or(300'000), JS::create_heap_function(realm.heap(), [this, on_complete, reset_observers]() {
        reset_observers(*this);

        auto did_time_out = m_navigation_timer->is_timed_out();
        m_navigation_timer = nullptr;

        // 7. If the previous step completed by the session page load timeout being reached and the browser does
        //    not have an active user prompt, return error with error code timeout.
        if (did_time_out && !current_browsing_context().active_document()->page().has_pending_dialog()) {
            on_complete->function()(Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::Timeout, "Navigation timed out"sv));
            return;
        }

        // 8. Return success with data null.
        on_complete->function()(JsonValue {});
    }));
}

void WebDriverConnection::page_did_open_dialog(Badge<PageClient>)
{
    // OPTMIZATION: If a dialog is opened while we are awaiting a specific document readiness state, that state will
    //              never be reached, as the dialog will block the HTML event loop from any further processing. Instead
    //              of waiting for the session's page load timeout to expire, unblock the waiter immediately. This also
    //              seems to match how other browsers behave.
    if (m_navigation_timer)
        m_navigation_timer->stop_and_fire_timeout_handler();
}

// https://w3c.github.io/webdriver/#dfn-restore-the-window
void WebDriverConnection::restore_the_window()
{
    // To restore the window, given an operating system level window with an associated top-level browsing context, run implementation-specific steps to restore or unhide the window to the visible screen.
    current_top_level_browsing_context()->page().client().page_did_request_restore_window();

    // Do not return from this operation until the visibility state of the top-level browsing context’s active document has reached the visible state, or until the operation times out.
    // FIXME: It isn't clear which timeout should be used here.
    auto page_load_timeout_fired = false;
    auto timer = Core::Timer::create_single_shot(m_timeouts_configuration.page_load_timeout.value_or(300'000), [&] {
        page_load_timeout_fired = true;
    });
    timer->start();

    Web::Platform::EventLoopPlugin::the().spin_until([&]() {
        auto state = current_top_level_browsing_context()->top_level_traversable()->system_visibility_state();
        return page_load_timeout_fired || state == Web::HTML::VisibilityState::Visible;
    });
}

// https://w3c.github.io/webdriver/#dfn-maximize-the-window
Gfx::IntRect WebDriverConnection::maximize_the_window()
{
    // To maximize the window, given an operating system level window with an associated top-level browsing context, run the implementation-specific steps to transition the operating system level window into the maximized window state.
    auto rect = current_top_level_browsing_context()->page().client().page_did_request_maximize_window();

    // Return when the window has completed the transition, or within an implementation-defined timeout.
    return rect;
}

// https://w3c.github.io/webdriver/#dfn-iconify-the-window
Gfx::IntRect WebDriverConnection::iconify_the_window()
{
    // To iconify the window, given an operating system level window with an associated top-level browsing context, run implementation-specific steps to iconify, minimize, or hide the window from the visible screen.
    auto rect = current_top_level_browsing_context()->page().client().page_did_request_minimize_window();

    // Do not return from this operation until the visibility state of the top-level browsing context’s active document has reached the hidden state, or until the operation times out.
    // FIXME: It isn't clear which timeout should be used here.
    auto page_load_timeout_fired = false;
    auto timer = Core::Timer::create_single_shot(m_timeouts_configuration.page_load_timeout.value_or(300'000), [&] {
        page_load_timeout_fired = true;
    });
    timer->start();

    Web::Platform::EventLoopPlugin::the().spin_until([&]() {
        auto state = current_top_level_browsing_context()->top_level_traversable()->system_visibility_state();
        return page_load_timeout_fired || state == Web::HTML::VisibilityState::Hidden;
    });

    return rect;
}

// https://w3c.github.io/webdriver/#dfn-find
// FIXME: Update this AO to the latest spec steps.
ErrorOr<JsonArray, Web::WebDriver::Error> WebDriverConnection::find(StartNodeGetter&& start_node_getter, Web::WebDriver::LocationStrategy using_, StringView value)
{
    // 1. Let end time be the current time plus the session implicit wait timeout.
    auto end_time = MonotonicTime::now() + AK::Duration::from_milliseconds(static_cast<i64>(m_timeouts_configuration.implicit_wait_timeout.value_or(0)));

    // 2. Let location strategy be equal to using.
    auto location_strategy = using_;

    // 3. Let selector be equal to value.
    auto selector = value;

    ErrorOr<JS::GCPtr<Web::DOM::NodeList>, Web::WebDriver::Error> maybe_elements { nullptr };

    auto try_to_find_element = [&]() -> decltype(maybe_elements) {
        // 4. Let elements returned be the result of trying to call the relevant element location strategy with arguments start node, and selector.
        auto elements = Web::WebDriver::invoke_location_strategy(location_strategy, *TRY(start_node_getter()), selector);

        // 5. If a DOMException, SyntaxError, XPathException, or other error occurs during the execution of the element location strategy, return error invalid selector.
        if (elements.is_error())
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidSelector, ByteString::formatted("The location strategy could not finish: {}", elements.error().message));

        return elements.release_value();
    };

    Web::Platform::EventLoopPlugin::the().spin_until([&]() {
        maybe_elements = try_to_find_element();
        if (maybe_elements.is_error())
            return true;

        // 6. If elements returned is empty and the current time is less than end time return to step 4. Otherwise, continue to the next step.
        return maybe_elements.value()->length() != 0 || MonotonicTime::now() >= end_time;
    });

    auto elements = TRY(maybe_elements);
    VERIFY(elements);

    // 7. Let result be an empty JSON List.
    JsonArray result;
    result.ensure_capacity(elements->length());

    // 8. For each element in elements returned, append the web element reference object for element, to result.
    for (size_t i = 0; i < elements->length(); ++i)
        TRY(result.append(Web::WebDriver::web_element_reference_object(*elements->item(i))));

    // 9. Return success with data result.
    return result;
}

// https://w3c.github.io/webdriver/#dfn-json-deserialize
static ErrorOr<JS::Value, Web::WebDriver::Error> json_deserialize(JS::VM& vm, JsonValue const& value)
{
    // 1. If seen is not provided, let seen be an empty List.
    // 2. Jump to the first appropriate step below:
    // 3. Matching on value:
    // -> undefined
    // -> null
    // -> type Boolean
    // -> type Number
    // -> type String
    if (value.is_null() || value.is_bool() || value.is_number() || value.is_string()) {
        // Return success with data value.
        return JS::JSONObject::parse_json_value(vm, value);
    }

    // -> Object that represents a web element
    if (Web::WebDriver::represents_a_web_element(value)) {
        // Return the deserialized web element of value.
        return Web::WebDriver::deserialize_web_element(value.as_object());
    }

    // FIXME: -> Object that represents a shadow root
    //     Return the deserialized shadow root of value.
    // FIXME: -> Object that represents a web frame
    //     Return the deserialized web frame of value.
    // FIXME: -> Object that represents a web window
    //     Return the deserialized web window of value.
    // FIXME: -> instance of Array
    // FIXME: -> instance of Object
    //     Return clone an object algorithm with session, value and seen, and the JSON deserialize algorithm as the clone algorithm.
    dbgln("FIXME: Implement JSON deserialize for: {}", value);
    return JS::JSONObject::parse_json_value(vm, value);
}

// https://w3c.github.io/webdriver/#dfn-extract-the-script-arguments-from-a-request
ErrorOr<WebDriverConnection::ScriptArguments, Web::WebDriver::Error> WebDriverConnection::extract_the_script_arguments_from_a_request(JS::VM& vm, JsonValue const& payload)
{
    // Creating JSON objects below requires an execution context.
    Web::HTML::TemporaryExecutionContext execution_context { current_browsing_context().active_document()->relevant_settings_object() };

    // 1. Let script be the result of getting a property named script from the parameters.
    // 2. If script is not a String, return error with error code invalid argument.
    auto script = TRY(Web::WebDriver::get_property(payload, "script"sv));

    // 3. Let args be the result of getting a property named args from the parameters.
    // 4. If args is not an Array return error with error code invalid argument.
    auto const& args = *TRY(Web::WebDriver::get_property<JsonArray const*>(payload, "args"sv));

    // 5. Let arguments be the result of calling the JSON deserialize algorithm with arguments args.
    auto arguments = JS::MarkedVector<JS::Value> { vm.heap() };

    TRY(args.try_for_each([&](auto const& arg) -> ErrorOr<void, Web::WebDriver::Error> {
        arguments.append(TRY(json_deserialize(vm, arg)));
        return {};
    }));

    // 6. Return success with data script and arguments.
    return ScriptArguments { move(script), move(arguments) };
}

// https://w3c.github.io/webdriver/#dfn-delete-cookies
void WebDriverConnection::delete_cookies(Optional<StringView> const& name)
{
    // For each cookie among all associated cookies of the current browsing context’s active document, un the substeps of the first matching condition:
    auto* document = current_browsing_context().active_document();

    for (auto& cookie : current_browsing_context().page().client().page_did_request_all_cookies(document->url())) {
        // -> name is undefined
        // -> name is equal to cookie name
        if (!name.has_value() || name.value() == cookie.name) {
            // Set the cookie expiry time to a Unix timestamp in the past.
            cookie.expiry_time = UnixDateTime::earliest();
            current_browsing_context().page().client().page_did_update_cookie(move(cookie));
        }
        // -> Otherwise
        //    Do nothing.
    }
}

// https://w3c.github.io/webdriver/#dfn-calculate-the-absolute-position
Gfx::IntPoint WebDriverConnection::calculate_absolute_position_of_element(JS::NonnullGCPtr<Web::Geometry::DOMRect> rect)
{
    // 1. Let rect be the value returned by calling getBoundingClientRect().

    // 2. Let window be the associated window of current top-level browsing context.
    auto const* window = current_top_level_browsing_context()->active_window();

    // 3. Let x be (scrollX of window + rect’s x coordinate).
    auto x = (window ? static_cast<int>(window->scroll_x()) : 0) + static_cast<int>(rect->x());

    // 4. Let y be (scrollY of window + rect’s y coordinate).
    auto y = (window ? static_cast<int>(window->scroll_y()) : 0) + static_cast<int>(rect->y());

    // 5. Return a pair of (x, y).
    return { x, y };
}

Gfx::IntRect WebDriverConnection::calculate_absolute_rect_of_element(Web::DOM::Element const& element)
{
    auto bounding_rect = element.get_bounding_client_rect();
    auto coordinates = calculate_absolute_position_of_element(bounding_rect);

    return {
        coordinates.x(),
        coordinates.y(),
        static_cast<int>(bounding_rect->width()),
        static_cast<int>(bounding_rect->height())
    };
}
}
