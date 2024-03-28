/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Document.h>

namespace Web {

bool build_xml_document(DOM::Document& document, ByteBuffer const& data, Optional<String> content_encoding);
JS::GCPtr<DOM::Document> load_document(HTML::NavigationParams const& navigation_params);

// https://html.spec.whatwg.org/multipage/document-lifecycle.html#read-ua-inline
template<typename MutateDocument>
JS::NonnullGCPtr<DOM::Document> create_document_for_inline_content(JS::GCPtr<HTML::Navigable> navigable, Optional<String> navigation_id, MutateDocument mutate_document)
{
    auto& vm = navigable->vm();

    // 1. Let origin be a new opaque origin.
    HTML::Origin origin {};

    // 2. Let coop be a new cross-origin opener policy.
    auto coop = HTML::CrossOriginOpenerPolicy {};

    // 3. Let coopEnforcementResult be a new cross-origin opener policy enforcement result with
    //    url: response's URL
    //    origin: origin
    //    cross-origin opener policy: coop
    HTML::CrossOriginOpenerPolicyEnforcementResult coop_enforcement_result {
        .url = URL::URL("about:error"), // AD-HOC
        .origin = origin,
        .cross_origin_opener_policy = coop
    };

    // 4. Let navigationParams be a new navigation params with
    //    id: navigationId
    //    navigable: navigable
    //    request: null
    //    response: a new response
    //    origin: origin
    //    fetch controller: null
    //    commit early hints: null
    //    COOP enforcement result: coopEnforcementResult
    //    reserved environment: null
    //    policy container: a new policy container
    //    final sandboxing flag set: an empty set
    //    cross-origin opener policy: coop
    //    FIXME: navigation timing type: navTimingType
    //    about base URL: null
    auto response = Fetch::Infrastructure::Response::create(vm);
    response->url_list().append(URL::URL("about:error")); // AD-HOC: https://github.com/whatwg/html/issues/9122
    HTML::NavigationParams navigation_params {
        .id = navigation_id,
        .navigable = navigable,
        .request = {},
        .response = *response,
        .fetch_controller = nullptr,
        .commit_early_hints = nullptr,
        .coop_enforcement_result = move(coop_enforcement_result),
        .reserved_environment = {},
        .origin = move(origin),
        .policy_container = HTML::PolicyContainer {},
        .final_sandboxing_flag_set = HTML::SandboxingFlagSet {},
        .cross_origin_opener_policy = move(coop),
        .about_base_url = {},
    };

    // 5. Let document be the result of creating and initializing a Document object given "html", "text/html", and navigationParams.
    auto document = DOM::Document::create_and_initialize(DOM::Document::Type::HTML, "text/html"_string, navigation_params).release_value_but_fixme_should_propagate_errors();

    // 6. Either associate document with a custom rendering that is not rendered using the normal Document rendering rules, or mutate document until it represents the content the
    //    user agent wants to render.
    mutate_document(*document);

    // 7. Return document.
    return document;
}

}
