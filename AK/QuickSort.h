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

/* This is a dual pivot quick sort. It is quite a bit faster than the single
 * pivot quick_sort below. The other quick_sort below should only be used when
 * you are stuck with simple iterators to a container and you don't have access
 * to the container itself.
 */
template<typename Collection, typename LessThan>
void dual_pivot_quick_sort(Collection& col, int start, int end, LessThan less_than)
{
    if (start >= end) {
        return;
    }

    int left_pointer, right_pointer;
    if (!less_than(col[start], col[end])) {
        swap(col[start], col[end]);
    }

    int j = start + 1;
    int k = start + 1;
    int g = end - 1;

    auto left_pivot = col[start];
    auto right_pivot = col[end];

    while (k <= g) {
        if (less_than(col[k], left_pivot)) {
            swap(col[k], col[j]);
            j++;
        } else if (!less_than(col[k], right_pivot)) {
            while (!less_than(col[g], right_pivot) && k < g) {
                g--;
            }
            swap(col[k], col[g]);
            g--;
            if (less_than(col[k], left_pivot)) {
                swap(col[k], col[j]);
                j++;
            }
        }
        k++;
    }
    j--;
    g++;

    swap(col[start], col[j]);
    swap(col[end], col[g]);

    left_pointer = j;
    right_pointer = g;

    dual_pivot_quick_sort(col, start, left_pointer - 1, less_than);
    dual_pivot_quick_sort(col, left_pointer + 1, right_pointer - 1, less_than);
    dual_pivot_quick_sort(col, right_pointer + 1, end, less_than);
}

template<typename Iterator, typename LessThan>
void quick_sort(Iterator start, Iterator end, LessThan less_than)
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

template<typename Iterator>
void quick_sort(Iterator start, Iterator end)
{
    quick_sort(start, end, [](auto& a, auto& b) { return a < b; });
}

template<typename Collection, typename LessThan>
void quick_sort(Collection& collection, LessThan less_than)
{
    dual_pivot_quick_sort(collection, 0, collection.size() - 1, move(less_than));
}

template<typename Collection>
void quick_sort(Collection& collection)
{
    dual_pivot_quick_sort(collection, 0, collection.size() - 1,
        [](auto& a, auto& b) { return a < b; });
}

}

using AK::quick_sort;
