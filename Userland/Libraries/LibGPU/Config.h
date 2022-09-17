/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace GPU {

// FIXME: These 3 types were originally introduced in LibSoftGPU and are currently needed by the device interface.
//        Once we refactor the interface these should move back into LibSoftGPU.
using ColorType = u32; // BGRA:8888
using DepthType = float;
using StencilType = u8;

// FIXME: This constant was originally introduced in LibSoftGPU and is currently used in the Vertex struct.
//        Once we refactor the interface this should move back into LibSoftGPU.
static constexpr int NUM_TEXTURE_UNITS = 2;

}
