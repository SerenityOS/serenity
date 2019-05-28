#pragma once

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
