/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Complex.h>
#include <AK/Vector.h>

namespace LibDSP {

void fft(Vector<Complex<double>>& sample_data, bool invert = false);

}
