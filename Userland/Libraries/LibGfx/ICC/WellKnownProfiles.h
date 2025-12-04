/*
 * Copyright (c) 2023-2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>

namespace Gfx::ICC {

class Profile;
class ParametricCurveTagData;

ErrorOr<NonnullRefPtr<Profile>> IdentityLAB();

ErrorOr<NonnullRefPtr<Profile>> sRGB();

ErrorOr<NonnullRefPtr<ParametricCurveTagData>> sRGB_curve();

}
