/*
 * Copyright (c) 2020-2021, The SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

    size_t lines_with_addition(const StringMetrics& offset, size_t column_width) const;
    size_t offset_with_addition(const StringMetrics& offset, size_t column_width) const;
    void reset()
    {
        line_metrics.clear();
        total_length = 0;
        max_line_length = 0;
        line_metrics.append({ {}, 0 });
    }
};

}
