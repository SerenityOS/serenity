/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/StdLibExtras.h>

namespace AK {

template<typename T>
bool is_less_than(const T& a, const T& b)
{
    return a < b;
}

template<typename Iterator, typename LessThan>
void quick_sort(Iterator start, Iterator end, LessThan less_than = is_less_than)
{
    int size = end - start;
    if (size <= 1)
        return;

    int pivot_point = size / 2;
    auto pivot = *(start + pivot_point);

    if (pivot_point)
        swap(*(start + pivot_point), *start);

    int i = 1;
    for (int j = 1; j < size; ++j) {
        if (less_than(*(start + j), pivot)) {
            swap(*(start + j), *(start + i));
            ++i;
        }
    }

    swap(*start, *(start + i - 1));
    quick_sort(start, start + i - 1, less_than);
    quick_sort(start + i, end, less_than);
}

}

using AK::quick_sort;
