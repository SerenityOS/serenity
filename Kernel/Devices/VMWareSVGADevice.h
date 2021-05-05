/*
 * Copyright (c) 2021, the SerenityOS Developers
 * Copyright (c) 2021, Alexander Richards <electrodeyt@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/IO.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/PCI/Device.h>

namespace Kernel {
class VMWareSVGADevice final : public BlockDevice
    , public PCI::Device {
public:
    VMWareSVGADevice(PCI::Address addr);

    virtual int ioctl(FileDescription&, unsigned request, FlatPtr arg) override;
    virtual KResultOr<Region*> mmap(Process&, FileDescription&, const Range&, u64 offset, int prot, bool shared) override;

    virtual mode_t required_mode() const override { return 0660; }
    virtual String device_name() const override;

private:
    virtual const char* class_name() const override { return "VMWareSVGA"; }
    virtual bool can_read(const FileDescription&, size_t) const override { return true; }
    virtual bool can_write(const FileDescription&, size_t) const override { return true; }
    virtual void start_request(AsyncBlockDeviceRequest& request) override { request.complete(AsyncDeviceRequest::Failure); }
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override { return -EINVAL; }
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return -EINVAL; }

    virtual void handle_irq(const RegisterState&) override;

    enum class IORegister : u32 {
        ID = 0,
        Enable,
        Width,
        Height,
        MaxWidth,
        MaxHeight,
        Depth,
        BitsPerPixel,
        PseudoColor,
        RedMask,
        GreenMask,
        BlueMask,
        BytesPerLine,
        FBStart,
        FBOffset,
        VRamSize,
        FBSize,
        Capabilities,
        MemStart,
        MemSize,
        ConfigDone,
        Sync,
        Busy,
        GuestID,
        CursorID,
        CursorX,
        CursorY,
        CursorOn,
        HostBitsPerPixel,
        ScratchSize,
        MemRegs,
        NumDisplays,
        Pitchlock,
        IRQMask,
        NumGuestDisplays,
        DisplayID,
        DisplayIsPrimary,
        DisplayPositionX,
        DisplayPositionY,
        DisplayWidth,
        DisplayHeight,
        GMRID,
        GMRDescriptor,
        GMRMaxIDs,
        GMRMaxDescriptorLength,
        Traces
    };

    enum class FifoRegister : u32 {
        Min = 0,
        Max,
        NextCMD,
        Stop,
        Capabilities = 4,
        Flags,
        Fence,
        HWVersion,
        Pitchlock,
        CursorOn,
        CursorX,
        CursorY,
        CursorCount,
        CursorLastUpdated,
        ReservedBytes,
        CursorScreenID,
        _3DCaps = 32,
        _3DCapsLast = 32 + 255,
        Guest3DHWVersion,
        FenceGoal,
        Busy,
        NumRegs
    };

    enum class Capabilities : u32 {
        RectCopy = 0x2,
        Cursor = 0x20,
        CursorBypass = 0x40,
        CursorBypass2 = 0x80,
        _8BitEmulation = 0x100,
        AlphaCursor = 0x200,
        _3D = 0x4000,
        ExtendedFifo = 0x8000,
        MultiMon = 0x10000,
        Pitchlock = 0x20000,
        IRQMask = 0x40000,
        DisplayTopology = 0x80000,
        GMR = 0x100000,
        Traces = 0x200000
    };

    enum class FifoCapabilites : u32 {
        Fence = 0x1,
        AccelFront = 0x2,
        Pitchlock = 0x4,
        Video = 0x8,
        CursorBypass3 = 0x10,
        Escape = 0x20,
        Reserve = 0x40,
        ScreenObject = 0x80
    };

    enum class Commands : u32 {
        Invalid = 0,
        Update,
        RectCopy = 3,
        DefineCursor = 19,
        DefineAlphaCursor = 22,
        UpdateVerbose = 25,
        FrontRopFill = 29,
        Fence = 30,
        Escape = 33,
        DefineScreen = 34,
        DestroyScreen,
        DefineGMRFB,
        BlitGMRFBToScreen,
        BlitScreenToGMRFB,
        AnnotationFill,
        AnnotationCopy,
        Max
    };

    enum class Ports : u32 {
        Index = 0,
        Value,
        Bios,
        IRQStatus
    };

    struct Rect {
        Rect()
            : x(0)
            , y(0)
            , w(0)
            , h(0)
        {
        }
        Rect(u32 _x, u32 _y, u32 _w, u32 _h)
            : x(_x)
            , y(_y)
            , w(_w)
            , h(_h)
        {
        }
        u32 x;
        u32 y;
        u32 w;
        u32 h;
    };

    // Set a video mode
    // Also enables the card itself, and sets up the fifo
    void set_mode(u32 width, u32 height, u32 bpp);

    // Create the version ID as the card wants it
    constexpr u32 make_version_id(u32 v) { return (0x900000 << 8 | v); }

    // Reserve space in the fifo
    // Returns a virtual pointer to which can be written
    // Must be paired by a commit
    // Returns a pointer that can be written into. Does not have
    // to be the bounce buffer and could also just be a pointer
    // into the Fifo
    u32* reserve_fifo(size_t size);

    // Commit reserved data into the fifo
    void commit_fifo(size_t size);

    // Add a fence into the fifo.
    // Returns the Fence ID
    u32 add_fence();

    // Force a "vblank"
    // The card has no concept of buffers (except for GMRs),
    // so we just send a Update packet with the entire screen
    void force_vblank();

    bool get_capability(Capabilities mask) { return m_device_capabilities & static_cast<u32>(mask); }
    bool get_fifo_reg_validity(FifoRegister reg) { return reinterpret_cast<u32*>(m_fifo_addr.as_ptr())[static_cast<u32>(FifoRegister::Min)] > (static_cast<u32>(reg) << 2); }
    bool get_fifo_capablity(FifoCapabilites capability) { return reinterpret_cast<u32*>(m_fifo_addr.as_ptr())[static_cast<u32>(FifoRegister::Capabilities)] & static_cast<u32>(capability); }

    inline void write_fifo(u32 offset, u32 val) { reinterpret_cast<u32*>(m_fifo_addr.as_ptr())[offset] = val; }
    inline void write_fifo(FifoRegister reg, u32 val) { write_fifo(static_cast<u32>(reg), val); }

    inline u32 read_fifo(u32 offset) { return reinterpret_cast<u32*>(m_fifo_addr.as_ptr())[offset]; }
    inline u32 read_fifo(FifoRegister reg) { return read_fifo(static_cast<u32>(reg)); }

    inline void set_register(IORegister index, u32 value)
    {
        IO::out32(m_io_base + static_cast<u32>(Ports::Index), static_cast<u32>(index));
        IO::out32(m_io_base + static_cast<u32>(Ports::Value), value);
    }

    inline u32 get_register(IORegister index)
    {
        IO::out32(m_io_base + static_cast<u32>(Ports::Index), static_cast<u32>(index));
        return IO::in32(m_io_base + static_cast<u32>(Ports::Value));
    }

    inline void copy_rect(const Rect src, const Rect dst)
    {
        u32* buffer = reserve_fifo(7);
        buffer[0] = static_cast<u32>(Commands::RectCopy);
        buffer[1] = src.x;
        buffer[2] = src.y;
        buffer[3] = dst.x;
        buffer[4] = dst.y;
        buffer[5] = dst.w;
        buffer[6] = dst.h;
        commit_fifo(7);
    }

    inline void update(const Rect area)
    {
        u32* buffer = reserve_fifo(5);
        buffer[0] = static_cast<u32>(Commands::Update);
        buffer[1] = area.x;
        buffer[2] = area.y;
        buffer[3] = area.w;
        buffer[4] = area.h;
        commit_fifo(5);
    }

    u32 next_fence { 1 };

    u32 m_io_base { 0 };

    OwnPtr<Region> m_fifo_mapping;
    VirtualAddress m_fifo_addr;
    // The maximum size of the fifo
    size_t m_fifo_size { 0 };

    AK::Vector<u32> m_fifo_bounce_buffer;

    u32 m_device_version { 0 };
    u32 m_device_capabilities { 0 };

    u8 m_interrupt_line { 0 };

    PhysicalAddress m_framebuffer_address_physical;
    // Maximum size of the BAR framebuffer in bytes
    size_t m_framebuffer_max_size { 0 };
    size_t m_framebuffer_pitch { 0 };
    size_t m_framebuffer_width { 0 };
    size_t m_framebuffer_height { 0 };
    size_t m_framebuffer_bpp { 0 };
    // Framebuffer size of the current mode in bytes
    size_t m_framebuffer_size { 0 };

    bool m_mode_set { false };
};
}
