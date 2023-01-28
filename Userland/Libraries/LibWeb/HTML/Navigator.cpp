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

JS::ThrowCompletionOr<void> Navigator::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::NavigatorPrototype>(realm, "Navigator"));

    return {};
}

// https://w3c.github.io/webdriver/#dfn-webdriver
bool Navigator::webdriver() const
{
    // Returns true if webdriver-active flag is set, false otherwise.

    // NOTE: The NavigatorAutomationInformation interface should not be exposed on WorkerNavigator.
    auto const& window = verify_cast<HTML::Window>(HTML::current_global_object());
    return window.page()->is_webdriver_active();
}

}
