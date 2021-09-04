/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <AK/Vector.h>

namespace Line {

struct StringMetrics {
    struct MaskedChar {
        size_t position { 0 };
        size_t original_length { 0 };
        size_t masked_length { 0 };
    };
    struct LineMetrics {
        Vector<MaskedChar> masked_chars;
        size_t length { 0 };

        size_t total_length(ssize_t offset = -1) const
        {
            size_t length = this->length;
            for (auto& mask : masked_chars) {
                if (offset < 0 || mask.position <= (size_t)offset) {
                    length -= mask.original_length;
                    length += mask.masked_length;
                }
            }
            return length;
        }
    };

    Vector<LineMetrics> line_metrics;
    size_t total_length { 0 };
    size_t max_line_length { 0 };

    size_t lines_with_addition(StringMetrics const& offset, size_t column_width) const;
    size_t offset_with_addition(StringMetrics const& offset, size_t column_width) const;
    void reset()
    {
        line_metrics.clear();
        total_length = 0;
        max_line_length = 0;
        line_metrics.append({ {}, 0 });
    }
};

}
