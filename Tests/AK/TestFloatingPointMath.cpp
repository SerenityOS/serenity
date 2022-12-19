/*
 * Copyright (c) 2022, Matheus Sousa <msamuel@aluno.puc-rio.br>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Math.h>

TEST_CASE(pow)
{
    EXPECT_EQ(AK::pow(10.0, -100.0), 0.0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001);
}
