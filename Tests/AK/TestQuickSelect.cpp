/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/QuickSelect.h>
#include <AK/QuickSort.h>

template<typename Collection>
int naive_select(Collection& a, int k)
{
    quick_sort(a);
    return k;
}

TEST_CASE(quickselect_inplace)
{

    // Test the various Quickselect Pivot methods against the naive select
    Array<int, 64> array;
    Array<int, 64> naive_results;

    auto reset_array = [&]() {
        for (size_t i = 0; i < 64; ++i)
            array[i] = (64 - i) % 32 + 32;
    };

    // Populate naive results
    reset_array();
    for (size_t k = 0; k < 64; ++k) {
        naive_results[k] = array.at(naive_select(array, k));
    }

    // Default configuration of `quick_select`
    reset_array();
    for (size_t k = 0; k < 64; ++k) {
        EXPECT(naive_results[k] == array.at(AK::quickselect_inplace(array, k)));
    }

    // first_element pivot function
    reset_array();
    for (size_t k = 0; k < 64; ++k) {
        EXPECT(naive_results[k] == array.at(AK::quickselect_inplace(array, k, [](auto collection, size_t left, size_t right, auto less_than) { return AK::PivotFunctions::first_element(collection, left, right, less_than); })));
    }

    // middle_element pivot function
    reset_array();
    for (size_t k = 0; k < 64; ++k) {
        EXPECT(naive_results[k] == array.at(AK::quickselect_inplace(array, k, [](auto collection, size_t left, size_t right, auto less_than) { return AK::PivotFunctions::middle_element(collection, left, right, less_than); })));
    }

    // random_element pivot function
    reset_array();
    for (size_t k = 0; k < 64; ++k) {
        EXPECT(naive_results[k] == array.at(AK::quickselect_inplace(array, k, [](auto collection, size_t left, size_t right, auto less_than) { return AK::PivotFunctions::random_element(collection, left, right, less_than); })));
    }

    // median_of_medians pivot function
    reset_array();
    for (size_t k = 0; k < 64; ++k) {
        EXPECT(naive_results[k] == array.at(AK::quickselect_inplace(array, k, [](auto collection, size_t left, size_t right, auto less_than) { return AK::PivotFunctions::median_of_medians(collection, left, right, less_than); })));
    }
}
