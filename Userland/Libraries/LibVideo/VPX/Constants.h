/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

namespace Video {

#define PARTITION_CONTEXTS 16
#define PARTITION_TYPES 4
#define INTRA_MODES 10
#define BLOCK_SIZE_GROUPS 4
#define SKIP_CONTEXTS 3
#define IS_INTER_CONTEXTS 4
#define COMP_MODE_CONTEXTS 5
#define REF_CONTEXTS 5
#define MV_OFFSET_BITS 10
#define TX_SIZES 4
#define TX_SIZE_CONTEXTS 2
#define INTER_MODES 4
#define INTER_MODE_CONTEXTS 7
#define INTERP_FILTER_CONTEXTS 4
#define SWITCHABLE_FILTERS 3
#define MV_CLASSES 11
#define CLASS0_SIZE 2
#define BLOCK_TYPES 2
#define COEF_BANDS 6
#define PREV_COEF_CONTEXTS 6
#define REF_TYPES 2
#define UNCONSTRAINED_NODES 3
#define MIN_TILE_WIDTH_B64 4
#define MAX_TILE_WIDTH_B64 64
#define MAX_SEGMENTS 8
#define SEG_LVL_MAX 4
#define MV_JOINTS 4
#define MV_FR_SIZE 4
#define MAX_PROB 255
#define TX_MODES 5
#define REFS_PER_FRAME 3
#define BLOCK_SIZES 13
#define BLOCK_INVALID 14

}
