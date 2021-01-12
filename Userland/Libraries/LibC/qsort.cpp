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
    ASSERT(a.size() == b.size());
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
