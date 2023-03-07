/*
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Fetch/Request.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/webappapis.html#windoworworkerglobalscope
class WindowOrWorkerGlobalScopeMixin {
public:
    virtual ~WindowOrWorkerGlobalScopeMixin();

    virtual Bindings::PlatformObject& this_impl() = 0;
    virtual Bindings::PlatformObject const& this_impl() const = 0;

    // JS API functions
    WebIDL::ExceptionOr<String> origin() const;
    bool is_secure_context() const;
    bool cross_origin_isolated() const;
    WebIDL::ExceptionOr<String> btoa(String const& data) const;
    WebIDL::ExceptionOr<String> atob(String const& data) const;
    JS::NonnullGCPtr<JS::Promise> fetch(Fetch::RequestInfo const&, Fetch::RequestInit const&) const;
};

}
