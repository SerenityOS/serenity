/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/Checked.h>
#include <AK/Singleton.h>
#include <Kernel/Debug.h>
#include <Kernel/Graphics/QXLGraphicsAdapter.h>
#include <Kernel/Graphics/Console/FramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/Spice.h>
#include <Kernel/IO.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/Process.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/errno_numbers.h>
#include <LibC/sys/ioctl_numbers.h>
#include <LibCrypto/Checksum/CRC32.h>

namespace Kernel {

typedef u64 QXLPhysical;

struct QXLRingHeader {
    u32 num_items;
    u32 prod;
    u32 notify_on_prod;
    u32 cons;
    u32 notify_on_cons;
};

struct QXLCommand {
    QXLPhysical data;
    u32 type;
    u32 padding;
};

struct QXLRect {
    i32 top;
    i32 left;
    i32 bottom;
    i32 right;
};

struct QXLURect {
    u32 top;
    u32 left;
    u32 bottom;
    u32 right;
};

struct QXLMonitorConfig {
    u16 count;
    u16 padding;
    QXLURect heads[64];
};

struct QXLRom {
    u32 magic;
    u32 id;
    u32 update_id;
    u32 compression_level;
    u32 log_level;
    u32 mode;
    u32 modes_offset;
    u32 num_io_pages;
    u32 pages_offset;
    u32 draw_area_offset;
    u32 surface0_area_size;
    u32 ram_header_offset;
    u32 mm_clock;

    // qxl 2
    u32 n_surfaces;
    u64 flags;
    u8 slots_start;
    u8 slots_end;
    u8 slot_gen_bits;
    u8 slot_id_bits;
    u8 slot_generation;

    // qxl 4
    u8 client_present;
    u8 client_capabilities[58];
    u32 client_monitors_config_crc;
    QXLMonitorConfig client_monitors_config;
};

struct QXLMemSlot {
    u64 mem_start;
    u64 mem_end;
};

struct QXLSurfaceCreate {
    u32 width;
    u32 height;
    i32 stride;
    u32 format;
    u32 position;
    u32 mouse_mode;
    u32 flags;
    u32 type;
    QXLPhysical mem;
};

struct QXLRamHeader {
    u32 magic;
    u32 int_pending;
    u32 int_mask;
    u8 log_buf[4096];
    QXLRingHeader cmd_ring_hdr;
    QXLCommand cmd_ring[32];
    QXLRingHeader cursor_ring_hdr;
    QXLCommand cursor_ring[32];
    QXLRingHeader release_ring_hdr;
    u64 release_ring[8];
    QXLRect update_area;

    // qxl 2
    u32 update_surface;
    QXLMemSlot mem_slot;
    QXLSurfaceCreate create_surface;
    u64 flags;

    // qxl 4
    QXLPhysical  monitors_config;
    u8 guest_capabilities[64];
};

enum QXLIOCommand : u16 {
    NOTIFY_CMD,
    NOTIFY_CURSOR,
    UPDATE_AREA,
    UPDATE_IRQ,
    NOTIFY_OOM,
    RESET,
    SET_MODE,
    LOG,
    // qxl 2
    MEMSLOT_ADD,
    MEMSLOT_DEL,
    DETACH_PRIMARY,
    ATTACH_PRIMARY,
    CREATE_PRIMARY,
    DESTROY_PRIMARY,
    DESTROY_SURFACE_WAIT,
    DESTROY_ALL_SURFACES,
    // qxl 3
    UPDATE_AREA_ASYNC,
    MEMSLOT_ADD_ASYNC,
    CREATE_PRIMARY_ASYNC,
    DESTROY_PRIMARY_ASYNC,
    DESTROY_SURFACE_ASYNC,
    DESTROY_ALL_SURFACES_ASYNC,
    FLUSH_SURFACES_ASYNC,
    FLUSH_RELEASE,
    // qxl 4
    MONITORS_CONFIG_ASYNC,
    RANGE_SIZE
};

class QXLDevice {
    friend class Ring;
    friend class MemSlot;
public:
    QXLDevice(QXLGraphicsAdapter& adapter)
        : m_adapter(adapter)
    {
    }

    bool initialize(PCI::Address const&, RefPtr<FramebufferDevice>&, RefPtr<Graphics::FramebufferConsole>&);
    void send_io_command(QXLIOCommand, u8 value);
    bool read_monitor_config(QXLMonitorConfig&);
    PhysicalAddress framebuffer_address() const;
    bool can_accept_resolution(size_t output_port_index, size_t width, size_t height);

private:
    class Ring {
    public:
        Ring(volatile QXLRingHeader& header, u32 size, QXLIOCommand notify_cmd, bool set_notify)
            : m_header(header)
            , m_size(size)
            , m_notify_cmd(notify_cmd)
        {
            if (set_notify)
                m_header.notify_on_prod = size;
        }
    private:
        volatile QXLRingHeader& m_header;
        const u32 m_size;
        const QXLIOCommand m_notify_cmd;
    };

    class MemSlot {
    public:
        MemSlot(QXLDevice&, u32, PhysicalAddress, size_t);
    private:
        u64 m_high_bits { 0 };
        u8 m_generation { 0 };
    };

    void reset();

    QXLGraphicsAdapter& m_adapter;
    TypedMapping<volatile QXLRom> m_rom;
    TypedMapping<volatile QXLRamHeader> m_ram_header;
    PhysicalAddress m_vram_addr;
    PhysicalAddress m_surface_addr;
    size_t m_surface_size { 0 };
    u16 m_io_base { 0 };
    OwnPtr<Ring> m_command_ring;
    OwnPtr<Ring> m_cursor_ring;
    OwnPtr<Ring> m_release_ring;
    OwnPtr<MemSlot> m_vram_slot;
    OwnPtr<MemSlot> m_surface_slot;
};

UNMAP_AFTER_INIT bool QXLDevice::initialize(PCI::Address const& pci_address, RefPtr<FramebufferDevice>& framebuffer_device, RefPtr<Graphics::FramebufferConsole>& console)
{
    dbgln("Initialize qxl device {}.{}", pci_address.device(), pci_address.function());
    auto rom_addr = PhysicalAddress(PCI::get_BAR2(pci_address) & 0xfffffffc);
    auto rom_size = PCI::get_BAR_space_size(pci_address, 2);
    m_vram_addr = PhysicalAddress(PCI::get_BAR0(pci_address) & 0xfffffffc);
    m_rom = map_typed<volatile QXLRom>(rom_addr);
    if (m_rom->magic != 0x4f525851)
        return false;
    m_ram_header = map_typed_writable<volatile QXLRamHeader>(m_vram_addr.offset(m_rom->ram_header_offset));
    if (m_ram_header->magic != 0x41525851)
        return false;
    m_io_base = PCI::get_BAR3(pci_address) & 0xfff0;

    bool surface_64_bits = false;
    if (PCI::get_BAR_space_size(pci_address, 4) > 0) {
        m_surface_addr = PhysicalAddress(PCI::get_BAR4(pci_address) & 0xfffffffc);
        m_surface_size = PCI::get_BAR_space_size(pci_address, 4);
        surface_64_bits = true;
    } else {
        m_surface_addr = PhysicalAddress(PCI::get_BAR1(pci_address) & 0xfffffffc);
        m_surface_size = PCI::get_BAR_space_size(pci_address, 1);
    }

    dbgln("QXL: Device version {}.{}", (u32)m_rom->id, (u32)m_rom->update_id);
    dbgln("QXL: Compression level: {}", (u32)m_rom->compression_level);
    dbgln("QXL: Log level: {}", (u32)m_rom->log_level);
    dbgln("QXL: ROM at {:} with {} bytes, VRAM at {:} with {} bytes", rom_addr, rom_size, m_vram_addr, (u32)m_rom->surface0_area_size);
    dbgln("QXL: IO Base: 0x{:x}", m_io_base);
    dbgln("QXL: Surface: {} with {} bytes ({} bits)", m_surface_addr, m_surface_size, surface_64_bits ? "64" : "32");

    // This is a little weird, but we *must* map the framebuffer, otherwise setting up the memslot later will fail!
    framebuffer_device = FramebufferDevice::create(m_adapter, 0, framebuffer_address(), 1024, 768, 1024 * sizeof(u32));
    framebuffer_device->initialize();
    console = Graphics::FramebufferConsole::initialize(framebuffer_address(), 1024, 768, 1024 * sizeof(u32));

    m_command_ring = adopt_own_if_nonnull(new Ring(m_ram_header->cmd_ring_hdr, sizeof(m_ram_header->cmd_ring) / sizeof(m_ram_header->cmd_ring[0]), QXLIOCommand::NOTIFY_CMD, false));
    if (!m_command_ring)
        return false;
    m_cursor_ring = adopt_own_if_nonnull(new Ring(m_ram_header->cursor_ring_hdr, sizeof(m_ram_header->cursor_ring) / sizeof(m_ram_header->cursor_ring[0]), QXLIOCommand::NOTIFY_CURSOR, false));
    if (!m_cursor_ring)
        return false;
    m_release_ring = adopt_own_if_nonnull(new Ring(m_ram_header->release_ring_hdr, sizeof(m_ram_header->release_ring) / sizeof(m_ram_header->release_ring[0]), QXLIOCommand::NOTIFY_CMD, true));
    if (!m_release_ring)
        return false;

    // Reset device to a known state
    reset();

    // TODO: setup IRQ using the _PRT?

    m_vram_slot = adopt_own_if_nonnull(new MemSlot(*this, 0, m_vram_addr, m_rom->ram_header_offset));
    if (!m_vram_slot)
        return false;
    m_surface_slot = adopt_own_if_nonnull(new MemSlot(*this, 1, m_surface_addr, m_surface_size));
    if (!m_surface_slot)
        return false;

    dbgln("QXL: Requesting monitor configuration");
    send_io_command(QXLIOCommand::MONITORS_CONFIG_ASYNC, 0);

    QXLMonitorConfig monitor_config;
    if (!read_monitor_config(monitor_config)) {
        dbgln("QXL: Failed to read monitor configuration!");
        return false;
    }


    return true;
}

void QXLDevice::reset()
{
    send_io_command(QXLIOCommand::RESET, 0);
}

QXLDevice::MemSlot::MemSlot(QXLDevice& device, u32 slot_index, PhysicalAddress paddr, size_t size)
{
    device.m_ram_header->mem_slot.mem_start = paddr.get();
    device.m_ram_header->mem_slot.mem_end = paddr.offset(size).get();
    u8 slot = device.m_rom->slots_start + slot_index;
    device.send_io_command(QXLIOCommand::MEMSLOT_ADD, slot);
    m_generation = device.m_rom->slot_generation;
    m_high_bits = (((u64)slot << device.m_rom->slot_gen_bits) | m_generation)
        << (64 - (device.m_rom->slot_gen_bits + device.m_rom->slot_id_bits));
    dbgln("QXL: Slot #{} base: {} size: {} generation: {} high bits: 0x{:x}", slot_index, paddr, size, m_generation, m_high_bits);
}

PhysicalAddress QXLDevice::framebuffer_address() const
{
    return m_vram_addr.offset(m_rom->draw_area_offset);
}

bool QXLDevice::read_monitor_config(QXLMonitorConfig& monitor_config)
{
    for (int retry = 0; retry < 5; retry++) {
        if (retry > 0)
            IO::delay(5);

        u32 expected_crc = m_rom->client_monitors_config_crc;

        auto& rom_config = m_rom->client_monitors_config;
        monitor_config.count = rom_config.count;
        monitor_config.padding = rom_config.padding;
        for (u32 i = 0; i < sizeof(rom_config.heads) / sizeof(rom_config.heads[0]); i++) {
            monitor_config.heads[i].left = rom_config.heads[i].left;
            monitor_config.heads[i].top = rom_config.heads[i].top;
            monitor_config.heads[i].bottom = rom_config.heads[i].bottom;
            monitor_config.heads[i].right = rom_config.heads[i].right;
        }

        u32 actual_crc = Crypto::Checksum::CRC32({ (const u8*)&monitor_config, sizeof(monitor_config) }).digest();
        if (actual_crc == expected_crc)
            return true;

        dbgln("QXL: Bad monitor configuration, crc expected: {:x}, actual: {:x} (attempt: {})", expected_crc, actual_crc, retry);
    }
    return false;
}

void QXLDevice::send_io_command(QXLIOCommand command, u8 value)
{
    IO::out8(m_io_base + (u16)command, value);
}

bool QXLDevice::can_accept_resolution(size_t, size_t width, size_t height)
{
    if (Checked<size_t>::multiplication_would_overflow(width, height, sizeof(u32)))
        return false;

    auto bytes = width * height * sizeof(u32);
    if (bytes > m_rom->surface0_area_size) {
        dbgln("QXL: Not enough memory for resolution {}x{}, need: {} have: {}", width, height, bytes, (u32)m_rom->surface0_area_size);
        return false;
    }
    return true;
}

UNMAP_AFTER_INIT RefPtr<QXLGraphicsAdapter> QXLGraphicsAdapter::initialize(PCI::Address address)
{
    auto adapter = adopt_ref(*new QXLGraphicsAdapter(address));
    if (adapter->initialize())
        return adapter;
    return {};
}

UNMAP_AFTER_INIT QXLGraphicsAdapter::QXLGraphicsAdapter(PCI::Address pci_address)
    : GraphicsDevice(pci_address)
    , PCI::DeviceController(pci_address)
    , m_device(adopt_own_if_nonnull(new QXLDevice(*this)))
{
}

UNMAP_AFTER_INIT bool QXLGraphicsAdapter::initialize()
{
    if (!m_device->initialize(pci_address(), m_framebuffer_device, m_framebuffer_console))
        return false;
    // FIXME: This is a very wrong way to do this...
    GraphicsManagement::the().m_console = m_framebuffer_console;

    dbgln("QXL: Device initialized");
    // We assume safe resolutio is 1024x768x32

    set_safe_resolution();
    return true;
}

UNMAP_AFTER_INIT void QXLGraphicsAdapter::initialize_framebuffer_devices()
{
    // FIXME: Find a better way to determine default resolution...

}

GraphicsDevice::Type QXLGraphicsAdapter::type() const
{
    return Type::QXL;
}

void QXLGraphicsAdapter::set_safe_resolution()
{
    VERIFY(m_framebuffer_console);
    auto result = try_to_set_resolution(0, 1024, 768);
    VERIFY(result);
}

void QXLGraphicsAdapter::set_resolution_registers(size_t width, size_t height)
{
    dbgln("QXLGraphicsAdapter resolution registers set to - {}x{}", width, height);
}

bool QXLGraphicsAdapter::try_to_set_resolution(size_t output_port_index, size_t width, size_t height)
{
    // Note: There's only one output port for this adapter
    VERIFY(output_port_index == 0);
    VERIFY(m_framebuffer_console);
    if (!m_device->can_accept_resolution(output_port_index, width, height))
        return false;
    set_resolution_registers(width, height);
    dbgln("QXLGraphicsAdapter resolution test - {}x{}", width, height);

    if (!validate_setup_resolution(width, height))
        return false;

    dbgln("QXLGraphicsAdapter: resolution set to {}x{}", width, height);
    m_framebuffer_console->set_resolution(width, height, width * sizeof(u32));
    return true;
}

bool QXLGraphicsAdapter::validate_setup_resolution(size_t width, size_t height)
{
    dbgln("validate_setup_resolution {}x{}", width, height);
    return true;
}

bool QXLGraphicsAdapter::set_y_offset(size_t output_port_index, size_t y)
{
    dbgln("set_y_offset output: {} y: {}", output_port_index, y);
    return false;
}

void QXLGraphicsAdapter::enable_consoles()
{
    ScopedSpinLock lock(m_console_mode_switch_lock);
    VERIFY(m_framebuffer_console);
    m_console_enabled = true;
    // TODO: switch to console mode?
    if (m_framebuffer_device)
        m_framebuffer_device->deactivate_writes();
    m_framebuffer_console->enable();
}
void QXLGraphicsAdapter::disable_consoles()
{
    ScopedSpinLock lock(m_console_mode_switch_lock);
    VERIFY(m_framebuffer_console);
    VERIFY(m_framebuffer_device);
    m_console_enabled = false;
    // TODO: switch to graphics mode?
    m_framebuffer_console->disable();
    m_framebuffer_device->activate_writes();
}

}
