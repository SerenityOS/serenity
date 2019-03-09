#pragma once

namespace AK {

template<typename IteratorType, typename LessThan>
IteratorType partition(IteratorType first, IteratorType last, LessThan less_than)
{
    auto pivot = last;
    auto smallest = first - 1;
    auto it = first;
    for (; it < last; ++it) {
        if (less_than(*it, *pivot)) {
            ++smallest;
            swap(*it, *smallest);
        }
    }
    swap(*(smallest + 1), *last);
    return smallest + 1;
}

template<typename IteratorType, typename LessThan>
void quick_sort_impl(IteratorType begin, IteratorType first, IteratorType last, LessThan less_than)
{
    if (first < last) {
        auto pivot = partition(first, last, less_than);
        quick_sort_impl(begin, first, pivot - 1, less_than);
        quick_sort_impl(begin, pivot + 1, last, less_than);
    }
}

template<typename T>
bool is_less_than(const T& a, const T& b)
{
    return a < b;
}

template<typename IteratorType, typename LessThan>
void quick_sort(IteratorType begin, IteratorType end, LessThan less_than = is_less_than)
{
    if (begin == end)
        return;
    quick_sort_impl(begin, begin, end - 1, less_than);
}

}

using AK::quick_sort;
