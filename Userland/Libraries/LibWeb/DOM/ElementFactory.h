/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <LibWeb/DOM/Element.h>

namespace Web::DOM {

ErrorOr<FixedArray<DeprecatedFlyString>> valid_local_names_for_given_html_element_interface(StringView html_element_interface_name);
bool is_unknown_html_element(DeprecatedFlyString const& tag_name);

// FIXME: The spec doesn't say what the default value of synchronous_custom_elements_flag should be.
WebIDL::ExceptionOr<JS::NonnullGCPtr<Element>> create_element(Document&, DeprecatedFlyString local_name, DeprecatedFlyString namespace_, DeprecatedFlyString prefix = {}, Optional<String> is = Optional<String> {}, bool synchronous_custom_elements_flag = false);

}
