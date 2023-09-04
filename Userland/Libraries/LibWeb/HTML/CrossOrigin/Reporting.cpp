/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/PropertyKey.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/CrossOrigin/AbstractOperations.h>
#include <LibWeb/HTML/CrossOrigin/Reporting.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/origin.html#coop-check-access-report
void check_if_access_between_two_browsing_contexts_should_be_reported(BrowsingContext const& accessor, BrowsingContext const& accessed, JS::PropertyKey const& property_key, EnvironmentSettingsObject const& environment)
{
    // 1. If P is not a cross-origin accessible window property name, then return.
    if (!is_cross_origin_accessible_window_property_name(property_key))
        return;

    // FIXME: 2. If accessor's active document's origin or any of its ancestors' active document's origins are not same origin with accessor's top-level browsing context's active document's origin, or if accessed's active document's origin or any of its ancestors' active document's origins are not same origin with accessed's top-level browsing context's active document's origin, then return.
    // NOTE: This avoids leaking information about cross-origin iframes to a top level frame with cross-origin opener policy reporting.

    // FIXME: 3. If accessor's top-level browsing context's virtual browsing context group ID is accessed's top-level browsing context's virtual browsing context group ID, then return.

    // 4. Let accessorAccessedRelationship be a new accessor-accessed relationship with value none.
    auto accessor_accessed_relationship = AccessorAccessedRelationship::None;

    // 5. If accessed's top-level browsing context's opener browsing context is accessor or an ancestor of accessor, then set accessorAccessedRelationship to accessor is opener.
    if (auto opener = accessed.top_level_browsing_context()->opener_browsing_context(); opener && (opener == &accessor || opener->is_ancestor_of(accessor)))
        accessor_accessed_relationship = AccessorAccessedRelationship::AccessorIsOpener;

    // 6. If accessor's top-level browsing context's opener browsing context is accessed or an ancestor of accessed, then set accessorAccessedRelationship to accessor is openee.
    if (auto opener = accessor.top_level_browsing_context()->opener_browsing_context(); opener && (opener == &accessed || opener->is_ancestor_of(accessed)))
        accessor_accessed_relationship = AccessorAccessedRelationship::AccessorIsOpenee;

    // FIXME: 7. Queue violation reports for accesses, given accessorAccessedRelationship, accessor's top-level browsing context's active document's cross-origin opener policy, accessed's top-level browsing context's active document's cross-origin opener policy, accessor's active document's URL, accessed's active document's URL, accessor's top-level browsing context's initial URL, accessed's top-level browsing context's initial URL, accessor's active document's origin, accessed's active document's origin, accessor's top-level browsing context's opener origin at creation, accessed's top-level browsing context's opener origin at creation, accessor's top-level browsing context's active document's referrer, accessed's top-level browsing context's active document's referrer, P, and environment.
    (void)accessor;
    (void)accessed;
    (void)environment;
    (void)accessor_accessed_relationship;
}

}
