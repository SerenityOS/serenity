/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Statistics.h>
#include <LibTest/TestSuite.h>

TEST_CASE(Statistics)
{
    // Setup Test Data
    AK::Statistics<double> odd_number_elements;
    AK::Statistics<double> even_number_elements;
    AK::Statistics<double> odd_number_elements_large;
    AK::Statistics<double> even_number_elements_large;

    odd_number_elements.add(5.0);
    odd_number_elements.add(4.0);
    odd_number_elements.add(3.0);
    odd_number_elements.add(2.0);
    odd_number_elements.add(1.0);

    even_number_elements.add(6.0);
    even_number_elements.add(5.0);
    even_number_elements.add(4.0);
    even_number_elements.add(3.0);
    even_number_elements.add(2.0);
    even_number_elements.add(1.0);

    for (int i = 201; i > 0; i--) {
        odd_number_elements_large.add(i);
    }

    for (int i = 360; i > 0; i--) {
        even_number_elements_large.add(i);
    }

    // Sum
    EXPECT_APPROXIMATE(odd_number_elements.sum(), 15.0);
    EXPECT_APPROXIMATE(even_number_elements.sum(), 21.0);

    // Average
    EXPECT_APPROXIMATE(odd_number_elements.average(), 3.0);
    EXPECT_APPROXIMATE(even_number_elements.average(), 3.5);

    // Min
    EXPECT_APPROXIMATE(odd_number_elements.min(), 1.0);
    EXPECT_APPROXIMATE(even_number_elements.min(), 1.0);

    // Max
    EXPECT_APPROXIMATE(odd_number_elements.max(), 5.0);
    EXPECT_APPROXIMATE(even_number_elements.max(), 6.0);

    // Median
    EXPECT_APPROXIMATE(odd_number_elements.median(), 3.0);
    EXPECT_APPROXIMATE(even_number_elements.median(), 3.5);
    EXPECT_APPROXIMATE(odd_number_elements_large.median(), 101.0);
    EXPECT_APPROXIMATE(even_number_elements_large.median(), 180.5);

    // The expected values for standard deviation and variance were calculated by my school issued scientific calculator

    // Standard Deviation
    EXPECT_APPROXIMATE(odd_number_elements.standard_deviation(), 1.4142135623731);
    EXPECT_APPROXIMATE(even_number_elements.standard_deviation(), 1.7078251276599);

    // Variance
    EXPECT_APPROXIMATE(odd_number_elements.variance(), 2.0);
    EXPECT_APPROXIMATE(even_number_elements.variance(), 2.9166666666667);
}
