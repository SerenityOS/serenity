/*
 * Copyright (c) 2021, xSlendiX <gamingxslendix@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Entry.h"

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>

namespace SereniTrivia {

Optional<Entry> Entry::try_parse(const JsonValue& value)
{
    if (!value.is_object())
        return {};
    auto& entry = value.as_object();
    Entry e;
    if (!entry.has("prompt") || !entry.has("answer") || !entry.has("answers"))
        return {};
    e.m_prompt = entry.get("prompt").as_string();
    e.m_answer = entry.get("answer").as_u32();
    auto& answers_array = entry.get("answers").as_array();
    for (size_t i = 0; i < answers_array.size(); ++i) {
        if (!answers_array[i].is_string()) {
            warnln("Couldn't parse entry answer #{}!", i);
            continue;
        }
        e.m_answers.append(answers_array[i].as_string());
    }

    return e;
}

}
