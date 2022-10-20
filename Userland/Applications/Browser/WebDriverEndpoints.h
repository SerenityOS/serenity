/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Function.h>
#include <LibWeb/Forward.h>

namespace Browser {

class WebDriverEndpoints {
public:
    WebDriverEndpoints() = default;
    ~WebDriverEndpoints() = default;

    Function<Optional<i32>()> on_get_document_element;
    Function<Optional<Vector<i32>>(i32 start_node_id, String const&)> on_query_selector_all;
    Function<Optional<String>(i32 element_id, String const&)> on_get_element_attribute;
    Function<Optional<String>(i32 element_id, String const&)> on_get_element_property;
    Function<String()> on_get_active_documents_type;
    Function<String(i32 element_id, String const&)> on_get_computed_value_for_element;
    Function<String(i32 element_id)> on_get_element_tag_name;
};

}
