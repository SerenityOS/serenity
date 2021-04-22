/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <stdio.h>

int main(int, char**)
{
    auto value = JsonValue::from_string("{\"property\": \"value\"}");
    VERIFY(value.has_value());
    printf("parsed: _%s_\n", value.value().to_string().characters());
    printf("object.property = '%s'\n", value.value().as_object().get("property").to_string().characters());
    return 0;
}
