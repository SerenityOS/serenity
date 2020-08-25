/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#include <AK/Optional.h>
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
    AK_MAKE_ETERNAL;

public:
    VMWareBackdoor();
    static VMWareBackdoor* the();

    bool vmmouse_is_absolute() const;
    void enable_absolute_vmmouse();
    void disable_absolute_vmmouse();
    void send(VMWareCommand& command);

    Optional<MousePacket> receive_mouse_packet();

private:
    void send_high_bandwidth(VMWareCommand& command);
    void get_high_bandwidth(VMWareCommand& command);
    bool detect_vmmouse();
    bool m_vmmouse_absolute { false };
};

}
