/*
 * Copyright (c) 2022-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/TypeCasts.h>
#include <LibJS/Runtime/PromiseCapability.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/HostDefined.h>
#include <LibWeb/DOM/AbortSignal.h>
#include <LibWeb/Fetch/FetchMethod.h>
#include <LibWeb/Fetch/Fetching/Fetching.h>
#include <LibWeb/Fetch/Fetching/RefCountedFlag.h>
#include <LibWeb/Fetch/Infrastructure/FetchAlgorithms.h>
#include <LibWeb/Fetch/Infrastructure/FetchController.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Requests.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Responses.h>
#include <LibWeb/Fetch/Request.h>
#include <LibWeb/Fetch/Response.h>
#include <LibWeb/WebIDL/ExceptionOr.h>
#include <LibWeb/WebIDL/Promise.h>

namespace Web::Fetch {

// https://fetch.spec.whatwg.org/#dom-global-fetch
JS::NonnullGCPtr<JS::Promise> fetch(JS::VM& vm, RequestInfo const& input, RequestInit const& init)
{
    auto& realm = *vm.current_realm();

    // 1. Let p be a new promise.
    auto promise_capability = WebIDL::create_promise(realm);

    // 2. Let requestObject be the result of invoking the initial value of Request as constructor with input and init
    //    as arguments. If this throws an exception, reject p with it and return p.
    auto exception_or_request_object = Request::construct_impl(realm, input, init);
    if (exception_or_request_object.is_exception()) {
        auto throw_completion = Bindings::dom_exception_to_throw_completion(vm, exception_or_request_object.exception());
        WebIDL::reject_promise(realm, promise_capability, *throw_completion.value());
        return verify_cast<JS::Promise>(*promise_capability->promise().ptr());
    }
    auto request_object = exception_or_request_object.release_value();

    // 3. Let request be requestObject’s request.
    auto request = request_object->request();

    // 4. If requestObject’s signal is aborted, then:
    if (request_object->signal()->aborted()) {
        // 1. Abort the fetch() call with p, request, null, and requestObject’s signal’s abort reason.
        abort_fetch(realm, promise_capability, request, nullptr, request_object->signal()->reason());

        // 2. Return p.
        return verify_cast<JS::Promise>(*promise_capability->promise().ptr());
    }

    // 5. Let globalObject be request’s client’s global object.
    auto& global_object = request->client()->global_object();

    // FIXME: 6. If globalObject is a ServiceWorkerGlobalScope object, then set request’s service-workers mode to "none".
    (void)global_object;

    // 7. Let responseObject be null.
    JS::Handle<Response> response_object_handle;

    // 8. Let relevantRealm be this’s relevant Realm.
    // NOTE: This assumes that the running execution context is for the fetch() function call.
    auto& relevant_realm = HTML::relevant_realm(*vm.running_execution_context().function);

    // 9. Let locallyAborted be false.
    // NOTE: This lets us reject promises with predictable timing, when the request to abort comes from the same thread
    //       as the call to fetch.
    auto locally_aborted = Fetching::RefCountedFlag::create(false);

    // 10. Let controller be null.
    JS::GCPtr<Infrastructure::FetchController> controller;

    // NOTE: Step 11 is done out of order so that the controller is non-null when we capture the GCPtr by copy in the abort algorithm lambda.
    //       This is not observable, AFAICT.

    // 12. Set controller to the result of calling fetch given request and processResponse given response being these
    //     steps:
    auto process_response = [locally_aborted, promise_capability, request, response_object_handle, &relevant_realm](JS::NonnullGCPtr<Infrastructure::Response> response) mutable {
        // 1. If locallyAborted is true, then abort these steps.
        if (locally_aborted->value())
            return;

        // NOTE: Not part of the spec, but we need to have an execution context on the stack to call native functions.
        //       (In this case, Promise functions)
        auto& environment_settings_object = Bindings::host_defined_environment_settings_object(relevant_realm);
        environment_settings_object.prepare_to_run_script();

        ScopeGuard guard = [&]() {
            // See above NOTE.
            environment_settings_object.clean_up_after_running_script();
        };

        // 2. If response’s aborted flag is set, then:
        if (response->aborted()) {
            // FIXME: 1. Let deserializedError be the result of deserialize a serialized abort reason given controller’s
            //           serialized abort reason and relevantRealm.
            auto deserialized_error = JS::js_undefined();

            // 2. Abort the fetch() call with p, request, responseObject, and deserializedError.
            abort_fetch(relevant_realm, promise_capability, request, response_object_handle.cell(), deserialized_error);

            // 3. Abort these steps.
            return;
        }

        // 3. If response is a network error, then reject p with a TypeError and abort these steps.
        if (response->is_network_error()) {
            auto message = response->network_error_message().value_or("Response is a network error"sv);
            WebIDL::reject_promise(relevant_realm, promise_capability, JS::TypeError::create(relevant_realm, message).release_allocated_value_but_fixme_should_propagate_errors());
            return;
        }

        // 4. Set responseObject to the result of creating a Response object, given response, "immutable", and
        //    relevantRealm.
        auto response_object = Response::create(relevant_realm, response, Headers::Guard::Immutable).release_value_but_fixme_should_propagate_errors();
        response_object_handle = JS::make_handle(response_object);

        // 5. Resolve p with responseObject.
        WebIDL::resolve_promise(relevant_realm, promise_capability, response_object);
    };
    controller = MUST(Fetching::fetch(
        realm,
        request,
        Infrastructure::FetchAlgorithms::create(vm,
            {
                .process_request_body_chunk_length = {},
                .process_request_end_of_body = {},
                .process_early_hints_response = {},
                .process_response = move(process_response),
                .process_response_end_of_body = {},
                .process_response_consume_body = {},
            })));

    // 11. Add the following abort steps to requestObject’s signal:
    request_object->signal()->add_abort_algorithm([locally_aborted, request, controller, promise_capability_handle = JS::make_handle(*promise_capability), request_object_handle = JS::make_handle(*request_object), response_object_handle, &relevant_realm] {
        dbgln_if(WEB_FETCH_DEBUG, "Fetch: Request object signal's abort algorithm called");

        auto& promise_capability = *promise_capability_handle;
        auto& request_object = *request_object_handle;
        JS::GCPtr<Response> response_object = response_object_handle.ptr();

        // 1. Set locallyAborted to true.
        locally_aborted->set_value(true);

        // 2. Assert: controller is non-null.
        VERIFY(controller);

        // 3. Abort controller with requestObject’s signal’s abort reason.
        controller->abort(relevant_realm, request_object.signal()->reason());

        // 4. Abort the fetch() call with p, request, responseObject, and requestObject’s signal’s abort reason.
        abort_fetch(relevant_realm, promise_capability, request, response_object, request_object.signal()->reason());
    });

    // 13. Return p.
    return verify_cast<JS::Promise>(*promise_capability->promise().ptr());
}

// https://fetch.spec.whatwg.org/#abort-fetch
void abort_fetch(JS::Realm& realm, WebIDL::Promise const& promise, JS::NonnullGCPtr<Infrastructure::Request> request, JS::GCPtr<Response> response_object, JS::Value error)
{
    dbgln_if(WEB_FETCH_DEBUG, "Fetch: Aborting fetch with: request @ {}, error = {}", request.ptr(), error);

    // 1. Reject promise with error.
    // NOTE: This is a no-op if promise has already fulfilled.
    WebIDL::reject_promise(realm, promise, error);

    // 2. If request’s body is non-null and is readable, then cancel request’s body with error.
    if (auto* body = request->body().get_pointer<Infrastructure::Body>(); body != nullptr && body->stream()->is_readable()) {
        // TODO: Implement cancelling streams
        (void)error;
    }

    // 3. If responseObject is null, then return.
    if (response_object == nullptr)
        return;

    // 4. Let response be responseObject’s response.
    auto response = response_object->response();

    // 5. If response’s body is non-null and is readable, then error response’s body with error.
    if (response->body().has_value()) {
        auto stream = response->body()->stream();
        if (stream->is_readable()) {
            // TODO: Implement erroring streams
            (void)error;
        }
    }
}

}
