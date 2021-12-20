/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/QuickSort.h>
#include <stdlib.h>
#include <sys/types.h>

class SizedObject {
public:
    SizedObject() = delete;
    SizedObject(void* data, size_t size)
        : m_data(data)
        , m_size(size)
    {
    }
    void* data() const { return m_data; }
    size_t size() const { return m_size; }

private:
    void* m_data;
    size_t m_size;
};

namespace AK {

template<>
inline void swap(const SizedObject& a, const SizedObject& b)
{
    VERIFY(a.size() == b.size());
    const size_t size = a.size();
    const auto a_data = reinterpret_cast<char*>(a.data());
    const auto b_data = reinterpret_cast<char*>(b.data());
    for (auto i = 0u; i < size; ++i) {
        swap(a_data[i], b_data[i]);
    }
}

}

class SizedObjectSlice {
public:
    SizedObjectSlice() = delete;
    SizedObjectSlice(void* data, size_t element_size)
        : m_data(data)
        , m_element_size(element_size)
    {
    }
    const SizedObject operator[](size_t index)
    {
        return { static_cast<char*>(m_data) + index * m_element_size, m_element_size };
    }

private:
    void* m_data;
    size_t m_element_size;
};

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/qsort.html
void qsort(void* bot, size_t nmemb, size_t size, int (*compar)(const void*, const void*))
{
    if (nmemb <= 1) {
        return;
    }

    SizedObjectSlice slice { bot, size };

    AK::dual_pivot_quick_sort(slice, 0, nmemb - 1, [=](const SizedObject& a, const SizedObject& b) { return compar(a.data(), b.data()) < 0; });
}

void qsort_r(void* bot, size_t nmemb, size_t size, int (*compar)(const void*, const void*, void*), void* arg)
{
    if (nmemb <= 1) {
        return;
    }

    SizedObjectSlice slice { bot, size };

    AK::dual_pivot_quick_sort(slice, 0, nmemb - 1, [=](const SizedObject& a, const SizedObject& b) { return compar(a.data(), b.data(), arg) < 0; });
}
