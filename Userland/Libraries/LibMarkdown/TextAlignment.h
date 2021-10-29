/*
 * Copyright (c) 2021, Dana Burkart <dana.burkart@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>

namespace Markdown {

class TextAlignment final {
public:
    static String justify(String text, int justification_width = 80, bool ignore_terminal_sequences = true);

private:
    struct WordNode : public RefCounted<WordNode> {
        String value;
        RefPtr<WordNode> next;

        explicit WordNode(String word)
            : value(word)
        {
        }
    };

    static size_t unadorned_text_length(String text);
};

}
