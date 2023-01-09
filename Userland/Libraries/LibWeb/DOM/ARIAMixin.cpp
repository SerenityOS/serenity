/*
 * Copyright (c) 2022, Jonah Shafran <jonahshafran@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/ARIAMixin.h>
#include <LibWeb/DOM/ARIARoleNames.h>
#include <LibWeb/Infra/CharacterTypes.h>

namespace Web::DOM {

// https://www.w3.org/TR/wai-aria-1.2/#introroles
DeprecatedFlyString ARIAMixin::role_or_default() const
{
    // 1. Use the rules of the host language to detect that an element has a role attribute and to identify the attribute value string for it.
    auto role_string = role();

    // 2. Separate the attribute value string for that attribute into a sequence of whitespace-free substrings by separating on whitespace.
    auto role_list = role_string.split_view(Infra::is_ascii_whitespace);

    // 3. Compare the substrings to all the names of the non-abstract WAI-ARIA roles. Case-sensitivity of the comparison inherits from the case-sensitivity of the host language.
    for (auto const& role : role_list) {
        // 4. Use the first such substring in textual order that matches the name of a non-abstract WAI-ARIA role.
        if (ARIARoleNames::is_non_abstract_aria_role(role))
            return role;
    }

    // https://www.w3.org/TR/wai-aria-1.2/#document-handling_author-errors_roles
    // If the role attribute contains no tokens matching the name of a non-abstract WAI-ARIA role, the user agent MUST treat the element as if no role had been provided.
    // https://www.w3.org/TR/wai-aria-1.2/#implicit_semantics
    return default_role();
}

// https://www.w3.org/TR/wai-aria-1.2/#global_states
bool ARIAMixin::has_global_aria_attribute() const
{
    return !aria_atomic().is_null()
        || !aria_busy().is_null()
        || !aria_controls().is_null()
        || !aria_current().is_null()
        || !aria_described_by().is_null()
        || !aria_details().is_null()
        || !aria_disabled().is_null()
        || !aria_drop_effect().is_null()
        || !aria_error_message().is_null()
        || !aria_flow_to().is_null()
        || !aria_grabbed().is_null()
        || !aria_has_popup().is_null()
        || !aria_hidden().is_null()
        || !aria_invalid().is_null()
        || !aria_key_shortcuts().is_null()
        || !aria_label().is_null()
        || !aria_labelled_by().is_null()
        || !aria_live().is_null()
        || !aria_owns().is_null()
        || !aria_relevant().is_null()
        || !aria_role_description().is_null();
}

}
