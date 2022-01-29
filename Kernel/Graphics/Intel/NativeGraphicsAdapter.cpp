/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/Definitions.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/Intel/NativeGraphicsAdapter.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

static constexpr IntelNativeGraphicsAdapter::PLLMaxSettings G35Limits {
    { 20'000'000, 400'000'000 },      // values in Hz, dot_clock
    { 1'400'000'000, 2'800'000'000 }, // values in Hz, VCO
    { 3, 8 },                         // n
    { 70, 120 },                      // m
    { 10, 20 },                       // m1
    { 5, 9 },                         // m2
    { 5, 80 },                        // p
    { 1, 8 },                         // p1
    { 5, 10 }                         // p2
};

static constexpr u16 supported_models[] {
    0x29c2, // Intel G35 Adapter
};

static bool is_supported_model(u16 device_id)
{
    for (auto& id : supported_models) {
        if (id == device_id)
            return true;
    }
    return false;
}

#define DDC2_I2C_ADDRESS 0x50

RefPtr<IntelNativeGraphicsAdapter> IntelNativeGraphicsAdapter::initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    VERIFY(pci_device_identifier.hardware_id().vendor_id == 0x8086);
    if (!is_supported_model(pci_device_identifier.hardware_id().device_id))
        return {};
    return adopt_ref(*new IntelNativeGraphicsAdapter(pci_device_identifier.address()));
}

static size_t compute_dac_multiplier(size_t pixel_clock_in_khz)
{
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Intel native graphics: Pixel clock is {} KHz", pixel_clock_in_khz);
    VERIFY(pixel_clock_in_khz >= 25000);
    if (pixel_clock_in_khz >= 100000) {
        return 1;
    } else if (pixel_clock_in_khz >= 50000) {
        return 2;
    } else {
        return 4;
    }
}

static Graphics::Modesetting calculate_modesetting_from_edid(EDID::Parser& edid, size_t index)
{
    auto details = edid.detailed_timing(index).release_value();

    Graphics::Modesetting mode;
    VERIFY(details.pixel_clock_khz());
    mode.pixel_clock_in_khz = details.pixel_clock_khz();

    size_t horizontal_active = details.horizontal_addressable_pixels();
    size_t horizontal_sync_offset = details.horizontal_front_porch_pixels();

    mode.horizontal.active = horizontal_active;
    mode.horizontal.sync_start = horizontal_active + horizontal_sync_offset;
    mode.horizontal.sync_end = horizontal_active + horizontal_sync_offset + details.horizontal_sync_pulse_width_pixels();
    mode.horizontal.total = horizontal_active + details.horizontal_blanking_pixels();

    size_t vertical_active = details.vertical_addressable_lines();
    size_t vertical_sync_offset = details.vertical_front_porch_lines();

    mode.vertical.active = vertical_active;
    mode.vertical.sync_start = vertical_active + vertical_sync_offset;
    mode.vertical.sync_end = vertical_active + vertical_sync_offset + details.vertical_sync_pulse_width_lines();
    mode.vertical.total = vertical_active + details.vertical_blanking_lines();
    return mode;
}

static bool check_pll_settings(const IntelNativeGraphicsAdapter::PLLSettings& settings, size_t reference_clock, const IntelNativeGraphicsAdapter::PLLMaxSettings& limits)
{
    if (settings.n < limits.n.min || settings.n > limits.n.max) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "N is invalid {}", settings.n);
        return false;
    }
    if (settings.m1 < limits.m1.min || settings.m1 > limits.m1.max) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "m1 is invalid {}", settings.m1);
        return false;
    }
    if (settings.m2 < limits.m2.min || settings.m2 > limits.m2.max) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "m2 is invalid {}", settings.m2);
        return false;
    }
    if (settings.p1 < limits.p1.min || settings.p1 > limits.p1.max) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "p1 is invalid {}", settings.p1);
        return false;
    }

    if (settings.m1 <= settings.m2) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "m2 is invalid {} as it is bigger than m1 {}", settings.m2, settings.m1);
        return false;
    }

    auto m = settings.compute_m();
    auto p = settings.compute_p();

    if (m < limits.m.min || m > limits.m.max) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "m invalid {}", m);
        return false;
    }
    if (p < limits.p.min || p > limits.p.max) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "p invalid {}", p);
        return false;
    }

    auto dot = settings.compute_dot_clock(reference_clock);
    auto vco = settings.compute_vco(reference_clock);

    if (dot < limits.dot_clock.min || dot > limits.dot_clock.max) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "Dot clock invalid {}", dot);
        return false;
    }
    if (vco < limits.vco.min || vco > limits.vco.max) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "VCO clock invalid {}", vco);
        return false;
    }
    return true;
}

static size_t find_absolute_difference(u64 target_frequency, u64 checked_frequency)
{
    if (target_frequency >= checked_frequency)
        return target_frequency - checked_frequency;
    return checked_frequency - target_frequency;
}

Optional<IntelNativeGraphicsAdapter::PLLSettings> IntelNativeGraphicsAdapter::create_pll_settings(u64 target_frequency, u64 reference_clock, const PLLMaxSettings& limits)
{
    IntelNativeGraphicsAdapter::PLLSettings settings;
    IntelNativeGraphicsAdapter::PLLSettings best_settings;
    // FIXME: Is this correct for all Intel Native graphics cards?
    settings.p2 = 10;
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Check PLL settings for ref clock of {} Hz, for target of {} Hz", reference_clock, target_frequency);
    u64 best_difference = 0xffffffff;
    for (settings.n = limits.n.min; settings.n <= limits.n.max; ++settings.n) {
        for (settings.m1 = limits.m1.max; settings.m1 >= limits.m1.min; --settings.m1) {
            for (settings.m2 = limits.m2.max; settings.m2 >= limits.m2.min; --settings.m2) {
                for (settings.p1 = limits.p1.max; settings.p1 >= limits.p1.min; --settings.p1) {
                    dbgln_if(INTEL_GRAPHICS_DEBUG, "Check PLL settings for {} {} {} {} {}", settings.n, settings.m1, settings.m2, settings.p1, settings.p2);
                    if (!check_pll_settings(settings, reference_clock, limits))
                        continue;
                    auto current_dot_clock = settings.compute_dot_clock(reference_clock);
                    if (current_dot_clock == target_frequency)
                        return settings;
                    auto difference = find_absolute_difference(target_frequency, current_dot_clock);
                    if (difference < best_difference && (current_dot_clock > target_frequency)) {
                        best_settings = settings;
                        best_difference = difference;
                    }
                }
            }
        }
    }
    if (best_settings.is_valid())
        return best_settings;
    return {};
}

IntelNativeGraphicsAdapter::IntelNativeGraphicsAdapter(PCI::Address address)
    : VGACompatibleAdapter(address)
    , m_registers(PCI::get_BAR0(address) & 0xfffffffc)
    , m_framebuffer_addr(PCI::get_BAR2(address) & 0xfffffffc)
{
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Intel Native Graphics Adapter @ {}", address);
    auto bar0_space_size = PCI::get_BAR_space_size(address, 0);
    VERIFY(bar0_space_size == 0x80000);
    dmesgln("Intel Native Graphics Adapter @ {}, MMIO @ {}, space size is {:x} bytes", address, PhysicalAddress(PCI::get_BAR0(address)), bar0_space_size);
    dmesgln("Intel Native Graphics Adapter @ {}, framebuffer @ {}", address, PhysicalAddress(PCI::get_BAR2(address)));
    auto region_or_error = MM.allocate_kernel_region(PhysicalAddress(PCI::get_BAR0(address)).page_base(), bar0_space_size, "Intel Native Graphics Registers", Memory::Region::Access::ReadWrite);
    if (region_or_error.is_error()) {
        TODO();
    }
    m_registers_region = region_or_error.release_value();
    PCI::enable_bus_mastering(address);
    {
        SpinlockLocker control_lock(m_control_lock);
        set_gmbus_default_rate();
        set_gmbus_pin_pair(GMBusPinPair::DedicatedAnalog);
    }
    gmbus_read_edid();

    auto modesetting = calculate_modesetting_from_edid(m_crt_edid.value(), 0);
    dmesgln("Intel Native Graphics Adapter @ {}, preferred resolution is {:d}x{:d}", pci_address(), modesetting.horizontal.active, modesetting.vertical.active);
    set_crt_resolution(modesetting.horizontal.active, modesetting.vertical.active);
    auto framebuffer_address = PhysicalAddress(PCI::get_BAR2(pci_address()) & 0xfffffff0);
    VERIFY(!framebuffer_address.is_null());
    VERIFY(m_framebuffer_pitch != 0);
    VERIFY(m_framebuffer_height != 0);
    VERIFY(m_framebuffer_width != 0);
    m_framebuffer_console = Graphics::ContiguousFramebufferConsole::initialize(framebuffer_address, m_framebuffer_width, m_framebuffer_height, m_framebuffer_pitch);
    GraphicsManagement::the().set_console(*m_framebuffer_console);
}

void IntelNativeGraphicsAdapter::enable_vga_plane()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
}

[[maybe_unused]] static StringView convert_register_index_to_string(IntelGraphics::RegisterIndex index)
{
    switch (index) {
    case IntelGraphics::RegisterIndex::PipeAConf:
        return "PipeAConf"sv;
    case IntelGraphics::RegisterIndex::PipeBConf:
        return "PipeBConf"sv;
    case IntelGraphics::RegisterIndex::GMBusData:
        return "GMBusData"sv;
    case IntelGraphics::RegisterIndex::GMBusStatus:
        return "GMBusStatus"sv;
    case IntelGraphics::RegisterIndex::GMBusCommand:
        return "GMBusCommand"sv;
    case IntelGraphics::RegisterIndex::GMBusClock:
        return "GMBusClock"sv;
    case IntelGraphics::RegisterIndex::DisplayPlaneAControl:
        return "DisplayPlaneAControl"sv;
    case IntelGraphics::RegisterIndex::DisplayPlaneALinearOffset:
        return "DisplayPlaneALinearOffset"sv;
    case IntelGraphics::RegisterIndex::DisplayPlaneAStride:
        return "DisplayPlaneAStride"sv;
    case IntelGraphics::RegisterIndex::DisplayPlaneASurface:
        return "DisplayPlaneASurface"sv;
    case IntelGraphics::RegisterIndex::DPLLDivisorA0:
        return "DPLLDivisorA0"sv;
    case IntelGraphics::RegisterIndex::DPLLDivisorA1:
        return "DPLLDivisorA1"sv;
    case IntelGraphics::RegisterIndex::DPLLControlA:
        return "DPLLControlA"sv;
    case IntelGraphics::RegisterIndex::DPLLControlB:
        return "DPLLControlB"sv;
    case IntelGraphics::RegisterIndex::DPLLMultiplierA:
        return "DPLLMultiplierA"sv;
    case IntelGraphics::RegisterIndex::HTotalA:
        return "HTotalA"sv;
    case IntelGraphics::RegisterIndex::HBlankA:
        return "HBlankA"sv;
    case IntelGraphics::RegisterIndex::HSyncA:
        return "HSyncA"sv;
    case IntelGraphics::RegisterIndex::VTotalA:
        return "VTotalA"sv;
    case IntelGraphics::RegisterIndex::VBlankA:
        return "VBlankA"sv;
    case IntelGraphics::RegisterIndex::VSyncA:
        return "VSyncA"sv;
    case IntelGraphics::RegisterIndex::PipeASource:
        return "PipeASource"sv;
    case IntelGraphics::RegisterIndex::AnalogDisplayPort:
        return "AnalogDisplayPort"sv;
    case IntelGraphics::RegisterIndex::VGADisplayPlaneControl:
        return "VGADisplayPlaneControl"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void IntelNativeGraphicsAdapter::write_to_register(IntelGraphics::RegisterIndex index, u32 value) const
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_registers_region);
    SpinlockLocker lock(m_registers_lock);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Intel Graphics {}: Write to {} value of {:x}", pci_address(), convert_register_index_to_string(index), value);
    auto* reg = (volatile u32*)m_registers_region->vaddr().offset(index).as_ptr();
    *reg = value;
}
u32 IntelNativeGraphicsAdapter::read_from_register(IntelGraphics::RegisterIndex index) const
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_registers_region);
    SpinlockLocker lock(m_registers_lock);
    auto* reg = (volatile u32*)m_registers_region->vaddr().offset(index).as_ptr();
    u32 value = *reg;
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Intel Graphics {}: Read from {} value of {:x}", pci_address(), convert_register_index_to_string(index), value);
    return value;
}

bool IntelNativeGraphicsAdapter::pipe_a_enabled() const
{
    VERIFY(m_control_lock.is_locked());
    return read_from_register(IntelGraphics::RegisterIndex::PipeAConf) & (1 << 30);
}

bool IntelNativeGraphicsAdapter::pipe_b_enabled() const
{
    VERIFY(m_control_lock.is_locked());
    return read_from_register(IntelGraphics::RegisterIndex::PipeBConf) & (1 << 30);
}

bool IntelNativeGraphicsAdapter::gmbus_wait_for(GMBusStatus desired_status, Optional<size_t> milliseconds_timeout)
{
    VERIFY(m_control_lock.is_locked());
    size_t milliseconds_passed = 0;
    while (1) {
        if (milliseconds_timeout.has_value() && milliseconds_timeout.value() < milliseconds_passed)
            return false;
        full_memory_barrier();
        u32 status = read_from_register(IntelGraphics::RegisterIndex::GMBusStatus);
        full_memory_barrier();
        VERIFY(!(status & (1 << 10))); // error happened
        switch (desired_status) {
        case GMBusStatus::HardwareReady:
            if (status & (1 << 11))
                return true;
            break;
        case GMBusStatus::TransactionCompletion:
            if (status & (1 << 14))
                return true;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        IO::delay(1000);
        milliseconds_passed++;
    }
}

void IntelNativeGraphicsAdapter::gmbus_write(unsigned address, u32 byte)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(address < 256);
    full_memory_barrier();
    write_to_register(IntelGraphics::RegisterIndex::GMBusData, byte);
    full_memory_barrier();
    write_to_register(IntelGraphics::RegisterIndex::GMBusCommand, ((address << 1) | (1 << 16) | (GMBusCycle::Wait << 25) | (1 << 30)));
    full_memory_barrier();
    gmbus_wait_for(GMBusStatus::TransactionCompletion, {});
}
void IntelNativeGraphicsAdapter::gmbus_read(unsigned address, u8* buf, size_t length)
{
    VERIFY(address < 256);
    VERIFY(m_control_lock.is_locked());
    size_t nread = 0;
    auto read_set = [&] {
        full_memory_barrier();
        u32 data = read_from_register(IntelGraphics::RegisterIndex::GMBusData);
        full_memory_barrier();
        for (size_t index = 0; index < 4; index++) {
            if (nread == length)
                break;
            buf[nread] = (data >> (8 * index)) & 0xFF;
            nread++;
        }
    };

    full_memory_barrier();
    write_to_register(IntelGraphics::RegisterIndex::GMBusCommand, (1 | (address << 1) | (length << 16) | (GMBusCycle::Wait << 25) | (1 << 30)));
    full_memory_barrier();
    while (nread < length) {
        gmbus_wait_for(GMBusStatus::HardwareReady, {});
        read_set();
    }
    gmbus_wait_for(GMBusStatus::TransactionCompletion, {});
}

void IntelNativeGraphicsAdapter::gmbus_read_edid()
{
    {
        SpinlockLocker control_lock(m_control_lock);
        gmbus_write(DDC2_I2C_ADDRESS, 0);
        gmbus_read(DDC2_I2C_ADDRESS, (u8*)&m_crt_edid_bytes, sizeof(m_crt_edid_bytes));
    }
    if (auto parsed_edid = EDID::Parser::from_bytes({ m_crt_edid_bytes, sizeof(m_crt_edid_bytes) }); !parsed_edid.is_error()) {
        m_crt_edid = parsed_edid.release_value();
    } else {
        dbgln("IntelNativeGraphicsAdapter: Parsing EDID failed: {}", parsed_edid.error());
        m_crt_edid = {};
    }
}

bool IntelNativeGraphicsAdapter::is_resolution_valid(size_t, size_t)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    // FIXME: Check that we are able to modeset to the requested resolution!
    return true;
}

void IntelNativeGraphicsAdapter::disable_output()
{
    VERIFY(m_control_lock.is_locked());
    disable_dac_output();
    disable_all_planes();
    disable_pipe_a();
    disable_pipe_b();
    disable_vga_emulation();
    disable_dpll();
}

void IntelNativeGraphicsAdapter::enable_output(PhysicalAddress fb_address, size_t width)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(!pipe_a_enabled());

    enable_pipe_a();
    enable_primary_plane(fb_address, width);
    enable_dac_output();
}

bool IntelNativeGraphicsAdapter::set_crt_resolution(size_t width, size_t height)
{
    SpinlockLocker control_lock(m_control_lock);
    SpinlockLocker modeset_lock(m_modeset_lock);
    if (!is_resolution_valid(width, height)) {
        return false;
    }

    // FIXME: Get the requested resolution from the EDID!!
    auto modesetting = calculate_modesetting_from_edid(m_crt_edid.value(), 0);

    disable_output();

    auto dac_multiplier = compute_dac_multiplier(modesetting.pixel_clock_in_khz);
    auto pll_settings = create_pll_settings((1000 * modesetting.pixel_clock_in_khz * dac_multiplier), 96'000'000, G35Limits);
    if (!pll_settings.has_value())
        VERIFY_NOT_REACHED();
    auto settings = pll_settings.value();
    dbgln_if(INTEL_GRAPHICS_DEBUG, "PLL settings for {} {} {} {} {}", settings.n, settings.m1, settings.m2, settings.p1, settings.p2);
    enable_dpll_without_vga(pll_settings.value(), dac_multiplier);
    set_display_timings(modesetting);
    auto address = PhysicalAddress(PCI::get_BAR2(pci_address()) & 0xfffffff0);
    VERIFY(!address.is_null());
    enable_output(address, width);

    m_framebuffer_width = width;
    m_framebuffer_height = height;
    m_framebuffer_pitch = width * 4;

    return true;
}

void IntelNativeGraphicsAdapter::set_display_timings(const Graphics::Modesetting& modesetting)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    VERIFY(!(read_from_register(IntelGraphics::RegisterIndex::PipeAConf) & (1 << 31)));
    VERIFY(!(read_from_register(IntelGraphics::RegisterIndex::PipeAConf) & (1 << 30)));

    dbgln_if(INTEL_GRAPHICS_DEBUG, "htotal - {}, {}", (modesetting.horizontal.active - 1), (modesetting.horizontal.total - 1));
    write_to_register(IntelGraphics::RegisterIndex::HTotalA, (modesetting.horizontal.active - 1) | (modesetting.horizontal.total - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "hblank - {}, {}", (modesetting.horizontal.blanking_start() - 1), (modesetting.horizontal.blanking_end() - 1));
    write_to_register(IntelGraphics::RegisterIndex::HBlankA, (modesetting.horizontal.blanking_start() - 1) | (modesetting.horizontal.blanking_end() - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "hsync - {}, {}", (modesetting.horizontal.sync_start - 1), (modesetting.horizontal.sync_end - 1));
    write_to_register(IntelGraphics::RegisterIndex::HSyncA, (modesetting.horizontal.sync_start - 1) | (modesetting.horizontal.sync_end - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "vtotal - {}, {}", (modesetting.vertical.active - 1), (modesetting.vertical.total - 1));
    write_to_register(IntelGraphics::RegisterIndex::VTotalA, (modesetting.vertical.active - 1) | (modesetting.vertical.total - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "vblank - {}, {}", (modesetting.vertical.blanking_start() - 1), (modesetting.vertical.blanking_end() - 1));
    write_to_register(IntelGraphics::RegisterIndex::VBlankA, (modesetting.vertical.blanking_start() - 1) | (modesetting.vertical.blanking_end() - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "vsync - {}, {}", (modesetting.vertical.sync_start - 1), (modesetting.vertical.sync_end - 1));
    write_to_register(IntelGraphics::RegisterIndex::VSyncA, (modesetting.vertical.sync_start - 1) | (modesetting.vertical.sync_end - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "sourceSize - {}, {}", (modesetting.vertical.active - 1), (modesetting.horizontal.active - 1));
    write_to_register(IntelGraphics::RegisterIndex::PipeASource, (modesetting.vertical.active - 1) | (modesetting.horizontal.active - 1) << 16);

    IO::delay(200);
}

bool IntelNativeGraphicsAdapter::wait_for_enabled_pipe_a(size_t milliseconds_timeout) const
{
    size_t current_time = 0;
    while (current_time < milliseconds_timeout) {
        if (pipe_a_enabled())
            return true;
        IO::delay(1000);
        current_time++;
    }
    return false;
}
bool IntelNativeGraphicsAdapter::wait_for_disabled_pipe_a(size_t milliseconds_timeout) const
{
    size_t current_time = 0;
    while (current_time < milliseconds_timeout) {
        if (!pipe_a_enabled())
            return true;
        IO::delay(1000);
        current_time++;
    }
    return false;
}

bool IntelNativeGraphicsAdapter::wait_for_disabled_pipe_b(size_t milliseconds_timeout) const
{
    size_t current_time = 0;
    while (current_time < milliseconds_timeout) {
        if (!pipe_b_enabled())
            return true;
        IO::delay(1000);
        current_time++;
    }
    return false;
}

void IntelNativeGraphicsAdapter::disable_dpll()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::DPLLControlA, read_from_register(IntelGraphics::RegisterIndex::DPLLControlA) & ~0x80000000);
    write_to_register(IntelGraphics::RegisterIndex::DPLLControlB, read_from_register(IntelGraphics::RegisterIndex::DPLLControlB) & ~0x80000000);
}

void IntelNativeGraphicsAdapter::disable_pipe_a()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::PipeAConf, read_from_register(IntelGraphics::RegisterIndex::PipeAConf) & ~0x80000000);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Disabling Pipe A");
    wait_for_disabled_pipe_a(100);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Disabling Pipe A - done.");
}

void IntelNativeGraphicsAdapter::disable_pipe_b()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::PipeAConf, read_from_register(IntelGraphics::RegisterIndex::PipeBConf) & ~0x80000000);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Disabling Pipe B");
    wait_for_disabled_pipe_b(100);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Disabling Pipe B - done.");
}

void IntelNativeGraphicsAdapter::set_gmbus_default_rate()
{
    // FIXME: Verify GMBUS Rate Select is set only when GMBUS is idle
    VERIFY(m_control_lock.is_locked());
    // Set the rate to 100KHz
    write_to_register(IntelGraphics::RegisterIndex::GMBusClock, read_from_register(IntelGraphics::RegisterIndex::GMBusClock) & ~(0b111 << 8));
}

void IntelNativeGraphicsAdapter::enable_pipe_a()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    VERIFY(!(read_from_register(IntelGraphics::RegisterIndex::PipeAConf) & (1 << 31)));
    VERIFY(!(read_from_register(IntelGraphics::RegisterIndex::PipeAConf) & (1 << 30)));
    write_to_register(IntelGraphics::RegisterIndex::PipeAConf, read_from_register(IntelGraphics::RegisterIndex::PipeAConf) | 0x80000000);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "enabling Pipe A");
    // FIXME: Seems like my video card is buggy and doesn't set the enabled bit (bit 30)!!
    wait_for_enabled_pipe_a(100);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "enabling Pipe A - done.");
}

void IntelNativeGraphicsAdapter::enable_primary_plane(PhysicalAddress fb_address, size_t width)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    VERIFY(((width * 4) % 64 == 0));

    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneAStride, width * 4);
    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneALinearOffset, 0);
    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneASurface, fb_address.get());

    // FIXME: Serenity uses BGR 32 bit pixel format, but maybe we should try to determine it somehow!
    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneAControl, (read_from_register(IntelGraphics::RegisterIndex::DisplayPlaneAControl) & (~(0b1111 << 26))) | (0b0110 << 26) | (1 << 31));
}

void IntelNativeGraphicsAdapter::set_dpll_registers(const PLLSettings& settings)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::DPLLDivisorA0, (settings.m2 - 2) | ((settings.m1 - 2) << 8) | ((settings.n - 2) << 16));
    write_to_register(IntelGraphics::RegisterIndex::DPLLDivisorA1, (settings.m2 - 2) | ((settings.m1 - 2) << 8) | ((settings.n - 2) << 16));

    write_to_register(IntelGraphics::RegisterIndex::DPLLControlA, read_from_register(IntelGraphics::RegisterIndex::DPLLControlA) & ~0x80000000);
}

void IntelNativeGraphicsAdapter::enable_dpll_without_vga(const PLLSettings& settings, size_t dac_multiplier)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());

    set_dpll_registers(settings);

    IO::delay(200);

    write_to_register(IntelGraphics::RegisterIndex::DPLLControlA, (6 << 9) | (settings.p1) << 16 | (1 << 26) | (1 << 28) | (1 << 31));
    write_to_register(IntelGraphics::RegisterIndex::DPLLMultiplierA, (dac_multiplier - 1) | ((dac_multiplier - 1) << 8));

    // The specification says we should wait (at least) about 150 microseconds
    // after enabling the DPLL to allow the clock to stabilize
    IO::delay(200);
    VERIFY(read_from_register(IntelGraphics::RegisterIndex::DPLLControlA) & (1 << 31));
}

void IntelNativeGraphicsAdapter::set_gmbus_pin_pair(GMBusPinPair pin_pair)
{
    // FIXME: Verify GMBUS is idle
    VERIFY(m_control_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::GMBusClock, (read_from_register(IntelGraphics::RegisterIndex::GMBusClock) & (~0b111)) | (pin_pair & 0b111));
}

void IntelNativeGraphicsAdapter::disable_dac_output()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::AnalogDisplayPort, 0b11 << 10);
}

void IntelNativeGraphicsAdapter::enable_dac_output()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::AnalogDisplayPort, (read_from_register(IntelGraphics::RegisterIndex::AnalogDisplayPort) & (~(0b11 << 10))) | 0x80000000);
}

void IntelNativeGraphicsAdapter::disable_vga_emulation()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::VGADisplayPlaneControl, (read_from_register(IntelGraphics::RegisterIndex::VGADisplayPlaneControl) & (~(1 << 30))) | 0x80000000);
}

void IntelNativeGraphicsAdapter::disable_all_planes()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneAControl, read_from_register(IntelGraphics::RegisterIndex::DisplayPlaneAControl) & ~(1 << 31));
}

void IntelNativeGraphicsAdapter::initialize_framebuffer_devices()
{
    auto address = PhysicalAddress(PCI::get_BAR2(pci_address()) & 0xfffffff0);
    VERIFY(!address.is_null());
    VERIFY(m_framebuffer_pitch != 0);
    VERIFY(m_framebuffer_height != 0);
    VERIFY(m_framebuffer_width != 0);
    m_framebuffer_device = FramebufferDevice::create(*this, address, m_framebuffer_width, m_framebuffer_height, m_framebuffer_pitch);
    // FIXME: Would be nice to be able to return a ErrorOr<void> here.
    auto framebuffer_result = m_framebuffer_device->try_to_initialize();
    VERIFY(!framebuffer_result.is_error());
}

ErrorOr<ByteBuffer> IntelNativeGraphicsAdapter::get_edid(size_t output_port_index) const
{
    if (output_port_index != 0) {
        dbgln("IntelNativeGraphicsAdapter: get_edid: Only one output supported");
        return Error::from_errno(ENODEV);
    }

    if (m_crt_edid.has_value())
        return ByteBuffer::copy(m_crt_edid_bytes, sizeof(m_crt_edid_bytes));

    return ByteBuffer {};
}

}
