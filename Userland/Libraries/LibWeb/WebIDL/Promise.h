/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibJS/SafeFunction.h>
#include <LibWeb/Forward.h>

namespace Web::WebIDL {

using ReactionSteps = JS::SafeFunction<WebIDL::ExceptionOr<JS::Value>(JS::Value)>;

JS::PromiseCapability create_promise(JS::Realm&);
JS::PromiseCapability create_resolved_promise(JS::Realm&, JS::Value);
JS::PromiseCapability create_rejected_promise(JS::Realm&, JS::Value);
void resolve_promise(JS::VM&, JS::PromiseCapability const&, JS::Value = JS::js_undefined());
void reject_promise(JS::VM&, JS::PromiseCapability const&, JS::Value);
JS::NonnullGCPtr<JS::Promise> react_to_promise(JS::PromiseCapability const&, Optional<ReactionSteps> on_fulfilled_callback, Optional<ReactionSteps> on_rejected_callback);
JS::NonnullGCPtr<JS::Promise> upon_fulfillment(JS::PromiseCapability const&, ReactionSteps);
JS::NonnullGCPtr<JS::Promise> upon_rejection(JS::PromiseCapability const&, ReactionSteps);
void mark_promise_as_handled(JS::PromiseCapability const&);

}
