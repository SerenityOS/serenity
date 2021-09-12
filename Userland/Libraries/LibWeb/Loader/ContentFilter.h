/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <AK/Vector.h>

namespace Web {

class ContentFilter {
public:
    static ContentFilter& the();

    bool is_filtered(const AK::URL&) const;
    void add_pattern(const String&);

private:
    ContentFilter();
    ~ContentFilter();

    struct Pattern {
        String text;
    };
    Vector<Pattern> m_patterns;
};

}
