/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <stdio.h>
#include <time.h>
#include <unistd.h>
#include <AK/StdLibExtras.h>

namespace Constants
{
    static inline String AppDomainName { "Fifteen"sv };
    static inline String AppIconName { "app-fifteen"sv };
    static inline String ConfigGroupSettings { "Settings"sv };
    static inline String ConfigGroupScore { "Settings"sv };
    static inline String ConfigNumberOfRows { "rows"sv };
    static inline String ConfigNumberOfColumns { "columns"sv };
    static inline String ConfigCellSizeInPixels { "cell_size"sv };
    static inline String ConfigCellColor { "cell_color"sv };
    static inline String ConfigCellTextColor { "cell_text_color"sv };
}

template<typename ValueType>
void shuffle(ValueType *array, size_t array_size)
{
    if (!array_size || array_size < 2) return;

    srand (time(nullptr));
 
    for (int idx = int(array_size) - 1; idx > 0; --idx)
    {
        int cp { idx }, patience { 10 };
        while (cp == idx && --patience > 0) cp = rand() % (idx + 1);

        swap(array[idx], array[cp]);
    }
}
