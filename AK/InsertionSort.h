/*
 * Copyright (c) 2022, Marc Luqué <marc.luque@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>

namespace AK {

/* Standard Insertion Sort.
 * NOTE: `end` is inclusive.
 */
template<typename Collection, typename Compare>
void insertion_sort(Collection& col, int start, int end, Compare compare)
{
    for (int i = start; i <= end; ++i) {
        for (int j = i; j > 0 && compare(col[j], col[j - 1]); --j) {
            swap(col[j], col[j - 1]);
        }
    }
}

// NOTE: `end` is exclusive.
template<typename Iterator, typename Compare>
void insertion_sort(Iterator start, Iterator end, Compare compare)
{
    for (Iterator i = start; i < end; ++i) {
        for (Iterator j = i; j > start && compare(*j, *(j - 1)); --j) {
            swap(*j, *(j - 1));
        }
    }
}

template<typename Iterator>
void insertion_sort(Iterator start, Iterator end)
{
    insertion_sort(start, end, [](auto& a, auto& b) { return a < b; });
}

template<typename Collection, typename Compare>
void insertion_sort(Collection& collection, Compare compare)
{
    insertion_sort(collection, 0, collection.size() - 1, move(compare));
}

template<typename Collection>
void insertion_sort(Collection& collection)
{
    insertion_sort(collection, 0, collection.size() - 1, [](auto& a, auto& b) { return a < b; });
}

}

using AK::insertion_sort;
