/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/Navigator.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Page/Page.h>

namespace Web::HTML {

JS::NonnullGCPtr<Navigator> Navigator::create(JS::Realm& realm)
{
    return realm.heap().allocate<Navigator>(realm, realm);
}

Navigator::Navigator(JS::Realm& realm)
    : PlatformObject(realm)
{
}

Navigator::~Navigator() = default;

void Navigator::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::NavigatorPrototype>(realm, "Navigator"));
}

// https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-pdfviewerenabled
bool Navigator::pdf_viewer_enabled() const
{
    // The NavigatorPlugins mixin's pdfViewerEnabled getter steps are to return the user agent's PDF viewer supported.
    // NOTE: The NavigatorPlugins mixin should only be exposed on the Window object.
    auto const& window = verify_cast<HTML::Window>(HTML::current_global_object());
    return window.page()->pdf_viewer_supported();
}

// https://w3c.github.io/webdriver/#dfn-webdriver
bool Navigator::webdriver() const
{
    // Returns true if webdriver-active flag is set, false otherwise.

    // NOTE: The NavigatorAutomationInformation interface should not be exposed on WorkerNavigator.
    auto const& window = verify_cast<HTML::Window>(HTML::current_global_object());
    return window.page()->is_webdriver_active();
}

void Navigator::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_mime_type_array);
    visitor.visit(m_plugin_array);
}

JS::NonnullGCPtr<MimeTypeArray> Navigator::mime_types()
{
    if (!m_mime_type_array)
        m_mime_type_array = heap().allocate<MimeTypeArray>(realm(), realm());
    return *m_mime_type_array;
}

JS::NonnullGCPtr<PluginArray> Navigator::plugins()
{
    if (!m_plugin_array)
        m_plugin_array = heap().allocate<PluginArray>(realm(), realm());
    return *m_plugin_array;
}

}
