/*
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Arch/aarch64/RPi/Mailbox.h>

namespace Kernel::RPi {

class FramebufferSetPhysicalSizeMboxMessage : public Mailbox::Message {
public:
    u32 width;
    u32 height;

    FramebufferSetPhysicalSizeMboxMessage()
        : Mailbox::Message(0x48003, 8)
    {
        width = 0;
        height = 0;
    }
};
static_assert(sizeof(FramebufferSetPhysicalSizeMboxMessage) == 20);

class FramebufferSetVirtualSizeMboxMessage : public Mailbox::Message {
public:
    u32 width;
    u32 height;

    FramebufferSetVirtualSizeMboxMessage()
        : Mailbox::Message(0x48004, 8)
    {
        width = 0;
        height = 0;
    }
};
static_assert(sizeof(FramebufferSetVirtualSizeMboxMessage) == 20);

class FramebufferSetVirtualOffsetMboxMessage : public Mailbox::Message {
public:
    u32 x;
    u32 y;

    FramebufferSetVirtualOffsetMboxMessage()
        : Mailbox::Message(0x48009, 8)
    {
        x = 0;
        y = 0;
    }
};
static_assert(sizeof(FramebufferSetVirtualOffsetMboxMessage) == 20);

class FramebufferSetDepthMboxMessage : public Mailbox::Message {
public:
    u32 depth_bits;

    FramebufferSetDepthMboxMessage()
        : Mailbox::Message(0x48005, 4)
    {
        depth_bits = 0;
    }
};
static_assert(sizeof(FramebufferSetDepthMboxMessage) == 16);

class FramebufferSetPixelOrderMboxMessage : public Mailbox::Message {
public:
    enum PixelOrder : u32 {
        BGR = 0,
        RGB = 1
    };

    PixelOrder pixel_order;

    FramebufferSetPixelOrderMboxMessage()
        : Mailbox::Message(0x48006, 4)
    {
        pixel_order = PixelOrder::BGR;
    }
};
static_assert(sizeof(FramebufferSetPixelOrderMboxMessage) == 16);

class FramebufferAllocateBufferMboxMessage : public Mailbox::Message {
public:
    union {
        u32 alignment;
        u32 address;
    };
    u32 size = 0;

    FramebufferAllocateBufferMboxMessage()
        : Mailbox::Message(0x40001, 8)
    {
        alignment = 0;
        size = 0;
    }
};
static_assert(sizeof(FramebufferAllocateBufferMboxMessage) == 20);

class FramebufferGetPitchMboxMessage : public Mailbox::Message {
public:
    u32 pitch;

    FramebufferGetPitchMboxMessage()
        : Mailbox::Message(0x40008, 4)
    {
        pitch = 0;
    }
};
static_assert(sizeof(FramebufferGetPitchMboxMessage) == 16);

}
