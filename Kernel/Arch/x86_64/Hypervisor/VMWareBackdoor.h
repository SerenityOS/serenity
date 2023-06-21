/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <AK/kmalloc.h>
#include <Kernel/API/MousePacket.h>

namespace Kernel {

#define VMMOUSE_GETVERSION 10
#define VMMOUSE_DATA 39
#define VMMOUSE_STATUS 40
#define VMMOUSE_COMMAND 41

struct VMWareCommand {
    union {
        u32 ax;
        u32 magic;
    };
    union {
        u32 bx;
        u32 size;
    };
    union {
        u32 cx;
        u32 command;
    };
    union {
        u32 dx;
        u32 port;
    };
    u32 si;
    u32 di;
};

class VMWareBackdoor {

public:
    VMWareBackdoor();
    static VMWareBackdoor* the();

    bool vmmouse_is_absolute() const;
    void enable_absolute_vmmouse();
    void disable_absolute_vmmouse();
    void send(VMWareCommand& command);

    u16 read_mouse_status_queue_size();
    MousePacket receive_mouse_packet();

private:
    void send_high_bandwidth(VMWareCommand& command);
    void get_high_bandwidth(VMWareCommand& command);
    bool detect_vmmouse();
    bool m_vmmouse_absolute { false };
};

}
