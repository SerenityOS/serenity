/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Fetch/Infrastructure/HTTP/Requests.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Responses.h>
#include <LibWeb/HTML/CrossOrigin/CrossOriginOpenerPolicy.h>
#include <LibWeb/HTML/CrossOrigin/CrossOriginOpenerPolicyEnforcementResult.h>
#include <LibWeb/HTML/HistoryHandlingBehavior.h>
#include <LibWeb/HTML/Navigable.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/PolicyContainers.h>
#include <LibWeb/HTML/SandboxingFlagSet.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#navigation-params
struct NavigationParams {
    // null or a navigation ID
    Optional<String> id;

    // null or a request that started the navigation
    JS::GCPtr<Fetch::Infrastructure::Request> request;

    // a response that ultimately was navigated to (potentially a network error)
    JS::NonnullGCPtr<Fetch::Infrastructure::Response> response;

    // an origin to use for the new Document
    Origin origin;

    // a policy container to use for the new Document
    PolicyContainer policy_container;

    // a sandboxing flag set to impose on the new Document
    SandboxingFlagSet final_sandboxing_flag_set = {};

    // a cross-origin opener policy to use for the new Document
    CrossOriginOpenerPolicy cross_origin_opener_policy;

    // a cross-origin opener policy enforcement result, used for reporting and potentially for causing a browsing context group switch
    CrossOriginOpenerPolicyEnforcementResult coop_enforcement_result;

    // null or an environment reserved for the new Document
    Optional<Environment> reserved_environment;

    // the browsing context to be navigated (or discarded, if a browsing context group switch occurs)
    JS::Handle<HTML::BrowsingContext> browsing_context;

    // the navigable to be navigated
    JS::Handle<Navigable> navigable;

    // a history handling behavior
    HistoryHandlingBehavior history_handling { HistoryHandlingBehavior::Default };

    // a boolean
    bool has_cross_origin_redirects { false };

    // FIXME: an algorithm expecting a response
    void* process_response_end_of_body { nullptr };

    // null or a fetch controller
    JS::GCPtr<Fetch::Infrastructure::FetchController> fetch_controller { nullptr };

    // FIXME: null or an algorithm accepting a Document, once it has been created
    void* commit_early_hints { nullptr };
};

}
