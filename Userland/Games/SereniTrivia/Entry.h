/*
 * Copyright (c) 2021, xSlendiX <gamingxslendix@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/Random.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace SereniTrivia {

class Entry {
public:
    static Optional<Entry> try_parse(const JsonValue& value);

    const String& prompt() const { return m_prompt; }
    const Vector<String>& answers() const { return m_answers; }
    u32 answer() const { return m_answer; }

private:
    Entry() = default;

    String m_prompt;
    Vector<String> m_answers;
    u32 m_answer;
};

}
