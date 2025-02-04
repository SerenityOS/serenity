/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/API/Ioctl.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Memory/SharedFramebufferVMObject.h>
#include <LibEDID/EDID.h>

namespace Kernel {

class GraphicsManagement;
class DisplayConnector : public CharacterDevice {
    friend class GraphicsManagement;
    friend class Device;

public:
    struct ModeSetting {
        size_t horizontal_blanking_start() const
        {
            return horizontal_active;
        }
        size_t horizontal_sync_start() const
        {
            return horizontal_active + horizontal_front_porch_pixels;
        }
        size_t horizontal_sync_end() const
        {
            return horizontal_active + horizontal_front_porch_pixels + horizontal_sync_time_pixels;
        }
        size_t horizontal_total() const
        {
            return horizontal_active + horizontal_blank_pixels;
        }

        size_t vertical_blanking_start() const
        {
            return vertical_active;
        }
        size_t vertical_sync_start() const
        {
            return vertical_active + vertical_front_porch_lines;
        }
        size_t vertical_sync_end() const
        {
            return vertical_active + vertical_front_porch_lines + vertical_sync_time_lines;
        }
        size_t vertical_total() const
        {
            return vertical_active + vertical_blank_lines;
        }

        size_t horizontal_stride; // Note: This is commonly known as "pitch"
        size_t pixel_clock_in_khz;

        size_t horizontal_active;
        size_t horizontal_front_porch_pixels;
        size_t horizontal_sync_time_pixels;
        size_t horizontal_blank_pixels;

        size_t vertical_active;
        size_t vertical_front_porch_lines;
        size_t vertical_sync_time_lines;
        size_t vertical_blank_lines;

        size_t horizontal_offset; // Note: This is commonly known as "x offset"
        size_t vertical_offset;   // Note: This is commonly known as "y offset"
    };

public:
    enum class DisplayMode {
        Graphical,
        Console,
    };

public:
    virtual ~DisplayConnector() = default;

    virtual bool mutable_mode_setting_capable() const = 0;
    virtual bool double_framebuffering_capable() const = 0;
    virtual bool flush_support() const = 0;
    virtual bool partial_flush_support() const = 0;
    // Note: This can indicate to userland if the underlying hardware requires
    // a defined refresh rate being supplied when modesetting the screen resolution.
    // Paravirtualized hardware don't need such setting and can safely ignore this.
    virtual bool refresh_rate_support() const = 0;

    bool console_mode() const;
    ErrorOr<ByteBuffer> get_edid() const;
    virtual ErrorOr<void> set_mode_setting(ModeSetting const&) = 0;
    virtual ErrorOr<void> set_safe_mode_setting() = 0;
    ModeSetting current_mode_setting() const;
    virtual ErrorOr<void> set_y_offset(size_t y) = 0;
    virtual ErrorOr<void> unblank() = 0;

    void set_display_mode(Badge<GraphicsManagement>, DisplayMode);

    Memory::Region const& framebuffer_region() const { return *m_framebuffer_region; }

protected:
    void set_edid_bytes(Array<u8, 128> const& edid_bytes, bool might_be_invalid = false);

    DisplayConnector(PhysicalAddress framebuffer_address, size_t framebuffer_resource_size, Memory::MemoryType);
    DisplayConnector(size_t framebuffer_resource_size, Memory::MemoryType);
    virtual void enable_console() = 0;
    virtual void disable_console() = 0;
    virtual ErrorOr<void> flush_first_surface() = 0;
    virtual ErrorOr<void> flush_rectangle(size_t buffer_index, FBRect const& rect);

    ErrorOr<void> initialize_edid_for_generic_monitor(Optional<Array<u8, 3>> manufacturer_id_string);

    mutable Spinlock<LockRank::None> m_control_lock {};
    mutable Mutex m_flushing_lock;

    bool m_console_mode { false };

    bool m_vertical_offsetted { false };

    mutable Spinlock<LockRank::None> m_modeset_lock {};
    ModeSetting m_current_mode_setting {};

    Optional<EDID::Parser> m_edid_parser;
    EDID::Parser::RawBytes m_edid_bytes {};
    bool m_edid_valid { false };

    u8* framebuffer_data() { return m_framebuffer_data; }

private:
    // ^File
    virtual bool is_seekable() const override { return true; }
    virtual bool can_read(OpenFileDescription const&, u64) const final override { return true; }
    virtual bool can_write(OpenFileDescription const&, u64) const final override { return true; }
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override final;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override final;
    virtual ErrorOr<VMObjectAndMemoryType> vmobject_and_memory_type_for_mmap(Process&, Memory::VirtualRange const&, u64&, bool) override final;
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override final;
    virtual StringView class_name() const override final { return "DisplayConnector"sv; }

    DisplayConnector& operator=(DisplayConnector const&) = delete;
    DisplayConnector& operator=(DisplayConnector&&) = delete;
    DisplayConnector(DisplayConnector&&) = delete;

    virtual void will_be_destroyed() override;
    virtual ErrorOr<void> after_inserting() override;

    ErrorOr<void> allocate_framebuffer_resources(size_t rounded_size);

    ErrorOr<bool> ioctl_requires_ownership(unsigned request) const;

    OwnPtr<Memory::Region> m_framebuffer_region;
    OwnPtr<Memory::Region> m_fake_writes_framebuffer_region;
    u8* m_framebuffer_data {};

    Memory::MemoryType m_memory_type { Memory::MemoryType::NonCacheable };
    bool const m_framebuffer_at_arbitrary_physical_range { false };

protected:
    Optional<PhysicalAddress> const m_framebuffer_address;
    size_t m_framebuffer_resource_size;

private:
    LockRefPtr<Memory::SharedFramebufferVMObject> m_shared_framebuffer_vmobject;

    LockWeakPtr<Process> m_responsible_process;
    Spinlock<LockRank::None> m_responsible_process_lock {};

    IntrusiveListNode<DisplayConnector, LockRefPtr<DisplayConnector>> m_list_node;
};
}
