/*
 * Copyright (c) 2022, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

struct VirGL3DResourceSpec {
    u32 target;
    u32 format;
    u32 bind;
    u32 width;
    u32 height;
    u32 depth;
    u32 array_size;
    u32 last_level;
    u32 nr_samples;
    u32 flags;
    u32 created_resource_id;
};

struct VirGLCommandBuffer {
    u32 const* data;
    u32 num_elems;
};

#define VIRGL_DATA_DIR_GUEST_TO_HOST 1
#define VIRGL_DATA_DIR_HOST_TO_GUEST 2

struct VirGLTransferDescriptor {
    void* data;
    size_t offset_in_region;
    size_t num_bytes;
    int direction;
};
