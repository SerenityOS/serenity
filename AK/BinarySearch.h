#pragma once

namespace AK {

template<typename T>
int integral_compare(const T& a, const T& b)
{
    return a - b;
}

template<typename T, typename Compare>
T* binary_search(T* haystack, size_t haystack_size, const T& needle, Compare compare = integral_compare)
{
    int low = 0;
    int high = haystack_size - 1;
    while (low <= high) {
        int middle = (low + high) / 2;
        int comparison = compare(needle, haystack[middle]);
        if (comparison < 0)
            high = middle - 1;
        else if (comparison > 0)
            low = middle + 1;
        else
            return &haystack[middle];
    }

    return nullptr;
}

}

using AK::binary_search;
