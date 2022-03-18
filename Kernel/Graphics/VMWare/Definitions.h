/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel {

static constexpr size_t vmware_svga_version_2_id = (0x900000UL << 8 | (2));

enum class VMWareDisplayRegistersOffset {
    ID = 0,
    ENABLE = 1,
    WIDTH = 2,
    HEIGHT = 3,
    MAX_WIDTH = 4,
    MAX_HEIGHT = 5,
    DEPTH = 6,
    BITS_PER_PIXEL = 7, /* Current bpp in the guest */
    PSEUDOCOLOR = 8,
    RED_MASK = 9,
    GREEN_MASK = 10,
    BLUE_MASK = 11,
    BYTES_PER_LINE = 12,
    FB_OFFSET = 14,
    VRAM_SIZE = 15,
    FB_SIZE = 16,

    CAPABILITIES = 17,
    MEM_SIZE = 19,
    CONFIG_DONE = 20,  /* Set when memory area configured */
    SYNC = 21,         /* See "FIFO Synchronization Registers" */
    BUSY = 22,         /* See "FIFO Synchronization Registers" */
    SCRATCH_SIZE = 29, /* Number of scratch registers */
    MEM_REGS = 30,     /* Number of FIFO registers */
    PITCHLOCK = 32,    /* Fixed pitch for all modes */
    IRQMASK = 33,      /* Interrupt mask */

    GMR_ID = 41,
    GMR_DESCRIPTOR = 42,
    GMR_MAX_IDS = 43,
    GMR_MAX_DESCRIPTOR_LENGTH = 44,

    TRACES = 45,         /* Enable trace-based updates even when FIFO is on */
    GMRS_MAX_PAGES = 46, /* Maximum number of 4KB pages for all GMRs */
    MEMORY_SIZE = 47,    /* Total dedicated device memory excluding FIFO */
};

struct [[gnu::packed]] VMWareDisplayFIFORegisters {
    u32 start;
    u32 size;
    u32 next_command;
    u32 stop;
    u32 commands[];
};

}
