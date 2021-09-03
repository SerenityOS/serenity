/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Complex.h>
#include <YAK/Vector.h>

void fft(Vector<Complex<double>>& sample_data, bool invert);
