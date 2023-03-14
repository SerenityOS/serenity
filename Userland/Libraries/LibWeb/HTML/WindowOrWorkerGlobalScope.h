/*
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/HashMap.h>
#include <AK/IDAllocator.h>
#include <AK/Variant.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Fetch/Request.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/MessagePort.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/#timerhandler
using TimerHandler = Variant<JS::Handle<WebIDL::CallbackType>, DeprecatedString>;

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
    void queue_microtask(WebIDL::CallbackType&);
    WebIDL::ExceptionOr<JS::Value> structured_clone(JS::Value, StructuredSerializeOptions const&) const;
    JS::NonnullGCPtr<JS::Promise> fetch(Fetch::RequestInfo const&, Fetch::RequestInit const&) const;

    i32 set_timeout(TimerHandler, i32 timeout, JS::MarkedVector<JS::Value> arguments);
    i32 set_interval(TimerHandler, i32 timeout, JS::MarkedVector<JS::Value> arguments);
    void clear_timeout(i32);
    void clear_interval(i32);

protected:
    void visit_edges(JS::Cell::Visitor&);

private:
    enum class Repeat {
        Yes,
        No,
    };
    i32 run_timer_initialization_steps(TimerHandler handler, i32 timeout, JS::MarkedVector<JS::Value> arguments, Repeat repeat, Optional<i32> previous_id = {}, Optional<AK::URL> base_url = {});

    IDAllocator m_timer_id_allocator;
    HashMap<int, JS::NonnullGCPtr<Timer>> m_timers;
};

}
