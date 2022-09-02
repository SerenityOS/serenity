/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/Intel/NativeDisplayConnector.h>
#include <Kernel/Memory/Region.h>

namespace Kernel {

namespace IntelGraphics {

#define DDC2_I2C_ADDRESS 0x50

struct PLLSettings {
    bool is_valid() const { return (n != 0 && m1 != 0 && m2 != 0 && p1 != 0 && p2 != 0); }
    u64 compute_dot_clock(u64 refclock) const
    {
        return (refclock * (5 * m1 + m2) / n) / (p1 * p2);
    }

    u64 compute_vco(u64 refclock) const
    {
        return refclock * (5 * m1 + m2) / n;
    }

    u64 compute_m() const
    {
        return 5 * m1 + m2;
    }

    u64 compute_p() const
    {
        return p1 * p2;
    }
    u64 n { 0 };
    u64 m1 { 0 };
    u64 m2 { 0 };
    u64 p1 { 0 };
    u64 p2 { 0 };
};

static constexpr PLLMaxSettings G35Limits {
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

static bool check_pll_settings(IntelGraphics::PLLSettings const& settings, size_t reference_clock, IntelGraphics::PLLMaxSettings const& limits)
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

Optional<IntelGraphics::PLLSettings> IntelNativeDisplayConnector::create_pll_settings(u64 target_frequency, u64 reference_clock, IntelGraphics::PLLMaxSettings const& limits)
{
    IntelGraphics::PLLSettings settings;
    IntelGraphics::PLLSettings best_settings;
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

NonnullLockRefPtr<IntelNativeDisplayConnector> IntelNativeDisplayConnector::must_create(PhysicalAddress framebuffer_address, size_t framebuffer_resource_size, PhysicalAddress registers_region_address, size_t registers_region_length)
{
    auto registers_region = MUST(MM.allocate_kernel_region(PhysicalAddress(registers_region_address), registers_region_length, "Intel Native Graphics Registers"sv, Memory::Region::Access::ReadWrite));
    auto device_or_error = DeviceManagement::try_create_device<IntelNativeDisplayConnector>(framebuffer_address, framebuffer_resource_size, move(registers_region));
    VERIFY(!device_or_error.is_error());
    auto connector = device_or_error.release_value();
    MUST(connector->initialize_gmbus_settings_and_read_edid());
    // Note: This is very important to set the resolution to something safe so we
    // can create a framebuffer console with valid resolution.
    {
        SpinlockLocker control_lock(connector->m_control_lock);
        MUST(connector->set_safe_mode_setting());
    }
    MUST(connector->create_attached_framebuffer_console());
    return connector;
}

ErrorOr<void> IntelNativeDisplayConnector::initialize_gmbus_settings_and_read_edid()
{
    gmbus_read_edid();
    return {};
}

ErrorOr<void> IntelNativeDisplayConnector::set_y_offset(size_t)
{
    return Error::from_errno(ENOTIMPL);
}

ErrorOr<void> IntelNativeDisplayConnector::unblank()
{
    return Error::from_errno(ENOTIMPL);
}

void IntelNativeDisplayConnector::enable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->enable();
}

void IntelNativeDisplayConnector::disable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->disable();
}

ErrorOr<void> IntelNativeDisplayConnector::flush_first_surface()
{
    return Error::from_errno(ENOTSUP);
}

ErrorOr<void> IntelNativeDisplayConnector::create_attached_framebuffer_console()
{
    m_framebuffer_console = Graphics::ContiguousFramebufferConsole::initialize(m_framebuffer_address.value(), m_current_mode_setting.horizontal_active, m_current_mode_setting.vertical_active, m_current_mode_setting.horizontal_stride);
    GraphicsManagement::the().set_console(*m_framebuffer_console);
    return {};
}

IntelNativeDisplayConnector::IntelNativeDisplayConnector(PhysicalAddress framebuffer_address, size_t framebuffer_resource_size, NonnullOwnPtr<Memory::Region> registers_region)
    : DisplayConnector(framebuffer_address, framebuffer_resource_size, true)
    , m_registers_region(move(registers_region))
{
    {
        SpinlockLocker control_lock(m_control_lock);
        set_gmbus_default_rate();
        set_gmbus_pin_pair(IntelGraphics::GMBusPinPair::DedicatedAnalog);
    }
}

ErrorOr<void> IntelNativeDisplayConnector::set_mode_setting(DisplayConnector::ModeSetting const&)
{
    return Error::from_errno(ENOTIMPL);
}

ErrorOr<void> IntelNativeDisplayConnector::set_safe_mode_setting()
{
    set_safe_crt_resolution();
    return {};
}

void IntelNativeDisplayConnector::enable_vga_plane()
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

void IntelNativeDisplayConnector::write_to_register(IntelGraphics::RegisterIndex index, u32 value) const
{
    VERIFY(m_control_lock.is_locked());
    SpinlockLocker lock(m_registers_lock);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Intel Graphics Display Connector:: Write to {} value of {:x}", convert_register_index_to_string(index), value);
    auto* reg = (u32 volatile*)m_registers_region->vaddr().offset(to_underlying(index)).as_ptr();
    *reg = value;
}
u32 IntelNativeDisplayConnector::read_from_register(IntelGraphics::RegisterIndex index) const
{
    VERIFY(m_control_lock.is_locked());
    SpinlockLocker lock(m_registers_lock);
    auto* reg = (u32 volatile*)m_registers_region->vaddr().offset(to_underlying(index)).as_ptr();
    u32 value = *reg;
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Intel Graphics Display Connector: Read from {} value of {:x}", convert_register_index_to_string(index), value);
    return value;
}

bool IntelNativeDisplayConnector::pipe_a_enabled() const
{
    VERIFY(m_control_lock.is_locked());
    return read_from_register(IntelGraphics::RegisterIndex::PipeAConf) & (1 << 30);
}

bool IntelNativeDisplayConnector::pipe_b_enabled() const
{
    VERIFY(m_control_lock.is_locked());
    return read_from_register(IntelGraphics::RegisterIndex::PipeBConf) & (1 << 30);
}

bool IntelNativeDisplayConnector::gmbus_wait_for(IntelGraphics::GMBusStatus desired_status, Optional<size_t> milliseconds_timeout)
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
        case IntelGraphics::GMBusStatus::HardwareReady:
            if (status & (1 << 11))
                return true;
            break;
        case IntelGraphics::GMBusStatus::TransactionCompletion:
            if (status & (1 << 14))
                return true;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        microseconds_delay(1000);
        milliseconds_passed++;
    }
}

void IntelNativeDisplayConnector::gmbus_write(unsigned address, u32 byte)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(address < 256);
    full_memory_barrier();
    write_to_register(IntelGraphics::RegisterIndex::GMBusData, byte);
    full_memory_barrier();
    write_to_register(IntelGraphics::RegisterIndex::GMBusCommand, ((address << 1) | (1 << 16) | (IntelGraphics::GMBusCycle::Wait << 25) | (1 << 30)));
    full_memory_barrier();
    gmbus_wait_for(IntelGraphics::GMBusStatus::TransactionCompletion, {});
}
void IntelNativeDisplayConnector::gmbus_read(unsigned address, u8* buf, size_t length)
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
    write_to_register(IntelGraphics::RegisterIndex::GMBusCommand, (1 | (address << 1) | (length << 16) | (IntelGraphics::GMBusCycle::Wait << 25) | (1 << 30)));
    full_memory_barrier();
    while (nread < length) {
        gmbus_wait_for(IntelGraphics::GMBusStatus::HardwareReady, {});
        read_set();
    }
    gmbus_wait_for(IntelGraphics::GMBusStatus::TransactionCompletion, {});
}

void IntelNativeDisplayConnector::gmbus_read_edid()
{
    Array<u8, 128> crt_edid_bytes {};
    {
        SpinlockLocker control_lock(m_control_lock);
        gmbus_write(DDC2_I2C_ADDRESS, 0);
        gmbus_read(DDC2_I2C_ADDRESS, (u8*)&crt_edid_bytes, sizeof(crt_edid_bytes));
        // FIXME: It seems like the returned EDID is almost correct,
        // but the first byte is set to 0xD0 instead of 0x00.
        // For now, this "hack" works well enough.
        crt_edid_bytes[0] = 0x0;
    }
    set_edid_bytes(crt_edid_bytes);
}

bool IntelNativeDisplayConnector::is_resolution_valid(size_t, size_t)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    // FIXME: Check that we are able to modeset to the requested resolution!
    return true;
}

void IntelNativeDisplayConnector::disable_output()
{
    VERIFY(m_control_lock.is_locked());
    disable_dac_output();
    disable_all_planes();
    disable_pipe_a();
    disable_pipe_b();
    disable_dpll();
    disable_vga_emulation();
}

void IntelNativeDisplayConnector::enable_output(PhysicalAddress fb_address, size_t width)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(!pipe_a_enabled());
    enable_pipe_a();
    enable_primary_plane(fb_address, width);
    enable_dac_output();
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

bool IntelNativeDisplayConnector::set_safe_crt_resolution()
{
    VERIFY(m_control_lock.is_locked());
    SpinlockLocker modeset_lock(m_modeset_lock);

    // Note: Just in case we still allow access to VGA IO ports, disable it now.
    GraphicsManagement::the().disable_vga_emulation_access_permanently();

    // FIXME: Get the requested resolution from the EDID!!
    auto modesetting = calculate_modesetting_from_edid(m_edid_parser.value(), 0);

    disable_output();

    auto dac_multiplier = compute_dac_multiplier(modesetting.pixel_clock_in_khz);
    auto pll_settings = create_pll_settings((1000 * modesetting.pixel_clock_in_khz * dac_multiplier), 96'000'000, IntelGraphics::G35Limits);
    if (!pll_settings.has_value())
        VERIFY_NOT_REACHED();
    auto settings = pll_settings.value();
    dbgln_if(INTEL_GRAPHICS_DEBUG, "PLL settings for {} {} {} {} {}", settings.n, settings.m1, settings.m2, settings.p1, settings.p2);
    enable_dpll_without_vga(pll_settings.value(), dac_multiplier);
    set_display_timings(modesetting);
    enable_output(m_framebuffer_address.value(), modesetting.horizontal.blanking_start());

    DisplayConnector::ModeSetting mode_set {
        .horizontal_stride = modesetting.horizontal.blanking_start() * sizeof(u32),
        .pixel_clock_in_khz = 0,
        .horizontal_active = modesetting.horizontal.blanking_start(),
        .horizontal_front_porch_pixels = 0,
        .horizontal_sync_time_pixels = 0,
        .horizontal_blank_pixels = 0,
        .vertical_active = modesetting.vertical.blanking_start(),
        .vertical_front_porch_lines = 0,
        .vertical_sync_time_lines = 0,
        .vertical_blank_lines = 0,
        .horizontal_offset = 0,
        .vertical_offset = 0,
    };

    m_current_mode_setting = mode_set;

    if (m_framebuffer_console)
        m_framebuffer_console->set_resolution(m_current_mode_setting.horizontal_active, m_current_mode_setting.vertical_active, m_current_mode_setting.horizontal_stride);

    return true;
}

void IntelNativeDisplayConnector::set_display_timings(Graphics::Modesetting const& modesetting)
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

    microseconds_delay(200);
}

bool IntelNativeDisplayConnector::wait_for_enabled_pipe_a(size_t milliseconds_timeout) const
{
    size_t current_time = 0;
    while (current_time < milliseconds_timeout) {
        if (pipe_a_enabled())
            return true;
        microseconds_delay(1000);
        current_time++;
    }
    return false;
}
bool IntelNativeDisplayConnector::wait_for_disabled_pipe_a(size_t milliseconds_timeout) const
{
    size_t current_time = 0;
    while (current_time < milliseconds_timeout) {
        if (!pipe_a_enabled())
            return true;
        microseconds_delay(1000);
        current_time++;
    }
    return false;
}

bool IntelNativeDisplayConnector::wait_for_disabled_pipe_b(size_t milliseconds_timeout) const
{
    size_t current_time = 0;
    while (current_time < milliseconds_timeout) {
        if (!pipe_b_enabled())
            return true;
        microseconds_delay(1000);
        current_time++;
    }
    return false;
}

void IntelNativeDisplayConnector::disable_dpll()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::DPLLControlA, 0);
    write_to_register(IntelGraphics::RegisterIndex::DPLLControlB, 0);
}

void IntelNativeDisplayConnector::disable_pipe_a()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::PipeAConf, 0);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Disabling Pipe A");
    wait_for_disabled_pipe_a(100);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Disabling Pipe A - done.");
}

void IntelNativeDisplayConnector::disable_pipe_b()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::PipeAConf, 0);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Disabling Pipe B");
    wait_for_disabled_pipe_b(100);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Disabling Pipe B - done.");
}

void IntelNativeDisplayConnector::set_gmbus_default_rate()
{
    // FIXME: Verify GMBUS Rate Select is set only when GMBUS is idle
    VERIFY(m_control_lock.is_locked());
    // Set the rate to 100KHz
    write_to_register(IntelGraphics::RegisterIndex::GMBusClock, read_from_register(IntelGraphics::RegisterIndex::GMBusClock) & ~(0b111 << 8));
}

void IntelNativeDisplayConnector::enable_pipe_a()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    VERIFY(!(read_from_register(IntelGraphics::RegisterIndex::PipeAConf) & (1 << 31)));
    VERIFY(!(read_from_register(IntelGraphics::RegisterIndex::PipeAConf) & (1 << 30)));
    write_to_register(IntelGraphics::RegisterIndex::PipeAConf, (1 << 31) | (1 << 24));
    dbgln_if(INTEL_GRAPHICS_DEBUG, "enabling Pipe A");
    // FIXME: Seems like my video card is buggy and doesn't set the enabled bit (bit 30)!!
    wait_for_enabled_pipe_a(100);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "enabling Pipe A - done.");
}

void IntelNativeDisplayConnector::enable_primary_plane(PhysicalAddress fb_address, size_t width)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    VERIFY(((width * 4) % 64 == 0));

    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneAStride, width * 4);
    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneALinearOffset, 0);
    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneASurface, fb_address.get());

    // FIXME: Serenity uses BGR 32 bit pixel format, but maybe we should try to determine it somehow!
    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneAControl, (0b0110 << 26) | (1 << 31));
}

void IntelNativeDisplayConnector::set_dpll_registers(IntelGraphics::PLLSettings const& settings)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::DPLLDivisorA0, (settings.m2 - 2) | ((settings.m1 - 2) << 8) | ((settings.n - 2) << 16));
    write_to_register(IntelGraphics::RegisterIndex::DPLLDivisorA1, (settings.m2 - 2) | ((settings.m1 - 2) << 8) | ((settings.n - 2) << 16));

    write_to_register(IntelGraphics::RegisterIndex::DPLLControlA, 0);
}

void IntelNativeDisplayConnector::enable_dpll_without_vga(IntelGraphics::PLLSettings const& settings, size_t dac_multiplier)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());

    set_dpll_registers(settings);

    microseconds_delay(200);

    write_to_register(IntelGraphics::RegisterIndex::DPLLControlA, (6 << 9) | (settings.p1) << 16 | (1 << 26) | (1 << 28) | (1 << 31));
    write_to_register(IntelGraphics::RegisterIndex::DPLLMultiplierA, (dac_multiplier - 1) | ((dac_multiplier - 1) << 8));

    // The specification says we should wait (at least) about 150 microseconds
    // after enabling the DPLL to allow the clock to stabilize
    microseconds_delay(200);
    VERIFY(read_from_register(IntelGraphics::RegisterIndex::DPLLControlA) & (1 << 31));
}

void IntelNativeDisplayConnector::set_gmbus_pin_pair(IntelGraphics::GMBusPinPair pin_pair)
{
    // FIXME: Verify GMBUS is idle
    VERIFY(m_control_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::GMBusClock, (read_from_register(IntelGraphics::RegisterIndex::GMBusClock) & (~0b111)) | (pin_pair & 0b111));
}

void IntelNativeDisplayConnector::disable_dac_output()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::AnalogDisplayPort, 0b11 << 10);
}

void IntelNativeDisplayConnector::enable_dac_output()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::AnalogDisplayPort, (1 << 31));
}

void IntelNativeDisplayConnector::disable_vga_emulation()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::VGADisplayPlaneControl, (1 << 31));
    read_from_register(IntelGraphics::RegisterIndex::VGADisplayPlaneControl);
}

void IntelNativeDisplayConnector::disable_all_planes()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneAControl, 0);
    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneBControl, 0);
}

}
