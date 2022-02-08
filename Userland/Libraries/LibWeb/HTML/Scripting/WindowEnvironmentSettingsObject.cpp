/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/Scripting/WindowEnvironmentSettingsObject.h>

namespace Web::HTML {

WindowEnvironmentSettingsObject::WindowEnvironmentSettingsObject(DOM::Window& window, JS::ExecutionContext& execution_context)
    : EnvironmentSettingsObject(execution_context)
    , m_window(window)
{
}

// https://html.spec.whatwg.org/multipage/window-object.html#set-up-a-window-environment-settings-object
void WindowEnvironmentSettingsObject::setup(AK::URL& creation_url, JS::ExecutionContext& execution_context)
{
    // 1. Let realm be the value of execution context's Realm component.
    auto* realm = execution_context.realm;
    VERIFY(realm);

    // 2. Let window be realm's global object.
    // NOTE: We want to store the Window impl rather than the WindowObject.
    auto& window = verify_cast<Bindings::WindowObject>(realm->global_object()).impl();

    // 3. Let settings object be a new environment settings object whose algorithms are defined as follows:
    // NOTE: See the functions defined for this class.
    auto settings_object = adopt_own(*new WindowEnvironmentSettingsObject(window, execution_context));

    // FIXME: 4. If reservedEnvironment is non-null, then:
    // FIXME:    1. Set settings object's id to reservedEnvironment's id, target browsing context to reservedEnvironment's target browsing context, and active service worker to reservedEnvironment's active service worker.
    // FIXME:    2. Set reservedEnvironment's id to the empty string.

    // FIXME: 5. Otherwise, set settings object's id to a new unique opaque string, settings object's target browsing context to null, and settings object's active service worker to null.
    settings_object->target_browsing_context = nullptr;

    // FIXME: 6. Set settings object's creation URL to creationURL, settings object's top-level creation URL to topLevelCreationURL, and settings object's top-level origin to topLevelOrigin.
    settings_object->creation_url = creation_url;

    // 7. Set realm's [[HostDefined]] field to settings object.
    realm->set_host_defined(move(settings_object));
}

// https://html.spec.whatwg.org/multipage/window-object.html#script-settings-for-window-objects:responsible-document
RefPtr<DOM::Document> WindowEnvironmentSettingsObject::responsible_document()
{
    // Return window's associated Document.
    return m_window->associated_document();
}

// https://html.spec.whatwg.org/multipage/window-object.html#script-settings-for-window-objects:api-url-character-encoding
String WindowEnvironmentSettingsObject::api_url_character_encoding()
{
    // Return the current character encoding of window's associated Document.
    return m_window->associated_document().encoding_or_default();
}

// https://html.spec.whatwg.org/multipage/window-object.html#script-settings-for-window-objects:api-base-url
AK::URL WindowEnvironmentSettingsObject::api_base_url()
{
    // FIXME: Return the current base URL of window's associated Document.
    //        (This currently just returns the current document URL, not accounting for <base> elements and such)
    return m_window->associated_document().url();
}

// https://html.spec.whatwg.org/multipage/window-object.html#script-settings-for-window-objects:concept-settings-object-origin
Origin WindowEnvironmentSettingsObject::origin()
{
    // Return the origin of window's associated Document.
    return m_window->associated_document().origin();
}

// https://html.spec.whatwg.org/multipage/window-object.html#script-settings-for-window-objects:concept-settings-object-cross-origin-isolated-capability
CanUseCrossOriginIsolatedAPIs WindowEnvironmentSettingsObject::cross_origin_isolated_capability()
{
    // FIXME: Return true if both of the following hold, and false otherwise:
    //          1. realm's agent cluster's cross-origin-isolation mode is "concrete", and
    //          2. window's associated Document is allowed to use the "cross-origin-isolated" feature.
    TODO();
}

}
