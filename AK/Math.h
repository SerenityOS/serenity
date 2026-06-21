/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Math/Cbrt.h>
#include <AK/Math/Constants.h>
#include <AK/Math/Copysign.h>
#include <AK/Math/Division.h>
#include <AK/Math/Exponentials.h>
#include <AK/Math/Fabs.h>
#include <AK/Math/Hyperbolic.h>
#include <AK/Math/Radians.h>
#include <AK/Math/Rounding.h>
#include <AK/Math/Sqrt.h>
#include <AK/Math/Trigonometry.h>
#include <math.h>

#ifdef KERNEL
#    error "Including AK/Math.h from the Kernel is never correct! Floating point is disabled."
#endif
