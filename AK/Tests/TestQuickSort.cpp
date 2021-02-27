/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/TestSuite.h>

#include <AK/Checked.h>
#include <AK/Noncopyable.h>
#include <AK/QuickSort.h>
#include <AK/StdLibExtras.h>

TEST_CASE(sorts_without_copy)
{
    struct NoCopy {
        AK_MAKE_NONCOPYABLE(NoCopy);

    public:
        NoCopy() = default;
        NoCopy(NoCopy&&) = default;

        NoCopy& operator=(NoCopy&&) = default;

        int value { 0 };
    };

    Array<NoCopy, 64> array;

    // Test the dual pivot quick sort.
    for (size_t i = 0; i < 64; ++i)
        array[i].value = (64 - i) % 32 + 32;

    dual_pivot_quick_sort(array, 0, array.size() - 1, [](auto& a, auto& b) { return a.value < b.value; });

    for (size_t i = 0; i < 63; ++i)
        EXPECT(array[i].value <= array[i + 1].value);

    // Test the single pivot quick sort.
    for (size_t i = 0; i < 64; ++i)
        array[i].value = (64 - i) % 32 + 32;

    AK::single_pivot_quick_sort(array.begin(), array.end(), [](auto& a, auto& b) { return a.value < b.value; });

    for (size_t i = 0; i < 63; ++i)
        EXPECT(array[i].value <= array[i + 1].value);
}

// This test case may fail to construct a worst-case input if the pivot choice
// of the underlying quick_sort no longer matches the one used here.
// So it provides no strong guarantees about the properties of quick_sort.
TEST_CASE(maximum_stack_depth)
{
    const int size = 256;
    int* data = new int[size];

    for (int i = 0; i < size; i++) {
        data[i] = i;
    }

    // Construct the data in such a way that the assumed pivot choice
    // of (size / 2) causes the partitions to be of worst case size.
    for (int i = 0; i < size / 2; i++) {
        swap(data[i], data[i + (size - i) / 2]);
    }

    // Measure the depth of the call stack through the less_than argument
    // of quick_sort as it gets copied for each recursive call.
    struct DepthMeasurer {
        int& max_depth;
        int depth { 0 };
        DepthMeasurer(int& max_depth)
            : max_depth(max_depth)
        {
        }
        DepthMeasurer(const DepthMeasurer& obj)
            : max_depth(obj.max_depth)
        {
            depth = obj.depth + 1;
            if (depth > max_depth) {
                max_depth = depth;
            }
        }
        bool operator()(int& a, int& b)
        {
            return a < b;
        }
    };

    int max_depth = 0;
    DepthMeasurer measurer(max_depth);
    AK::single_pivot_quick_sort(data, data + size, measurer);

    EXPECT(max_depth <= 64);

    for (int i = 0; i < size; i++)
        EXPECT(data[i] == i);

    delete[] data;
}

TEST_MAIN(QuickSort)
