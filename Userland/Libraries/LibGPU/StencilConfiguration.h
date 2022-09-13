/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGPU/Config.h>
#include <LibGPU/Enums.h>

namespace GPU {

struct StencilConfiguration {
    StencilTestFunction test_function;
    StencilType reference_value;
    StencilType test_mask;

    StencilOperation on_stencil_test_fail;
    StencilOperation on_depth_test_fail;
    StencilOperation on_pass;
    StencilType write_mask;
};

}
