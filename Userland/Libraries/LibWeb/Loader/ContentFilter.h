/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/URL.h>
#include <YAK/Vector.h>

namespace Web {

class ContentFilter {
public:
    static ContentFilter& the();

    bool is_filtered(const URL&) const;
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
