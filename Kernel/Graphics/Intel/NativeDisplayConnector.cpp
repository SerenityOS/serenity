/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/IO.h>
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

NonnullOwnPtr<IntelNativeDisplayConnector> IntelNativeDisplayConnector::must_create(PhysicalAddress framebuffer_address, PhysicalAddress registers_region_address, size_t registers_region_length)
{
    auto registers_region = MUST(MM.allocate_kernel_region(PhysicalAddress(registers_region_address), registers_region_length, "Intel Native Graphics Registers", Memory::Region::Access::ReadWrite));
    // FIXME: Try to put the address as parameter to this function to allow creating this DisplayConnector for many generations...
    auto gmbus_connector = MUST(GMBusConnector::create_with_physical_address(registers_region_address.offset(to_underlying(IntelGraphics::RegisterIndex::GMBusClock))));
    auto connector = adopt_own_if_nonnull(new (nothrow) IntelNativeDisplayConnector(framebuffer_address, move(gmbus_connector), move(registers_region))).release_nonnull();
    MUST(connector->initialize_gmbus_settings_and_read_edid());
    // Note: This is very important to set the resolution to something safe so we
    // can create a framebuffer console with valid resolution.
    MUST(connector->set_safe_resolution());
    MUST(connector->create_attached_framebuffer_console(framebuffer_address));
    return connector;
}

ErrorOr<void> IntelNativeDisplayConnector::initialize_gmbus_settings_and_read_edid()
{
    gmbus_read_edid();
    return {};
}

ErrorOr<void> IntelNativeDisplayConnector::create_attached_framebuffer_console(PhysicalAddress framebuffer_address)
{
    m_framebuffer_console = Graphics::ContiguousFramebufferConsole::initialize(framebuffer_address, m_framebuffer_width, m_framebuffer_height, m_framebuffer_pitch);
    GraphicsManagement::the().set_console(*m_framebuffer_console);
    return {};
}

IntelNativeDisplayConnector::IntelNativeDisplayConnector(PhysicalAddress framebuffer_address, NonnullOwnPtr<GMBusConnector> gmbus_connector, NonnullOwnPtr<Memory::Region> registers_region)
    : VGAGenericDisplayConnector(framebuffer_address)
    , m_registers_region(move(registers_region))
    , m_gmbus_connector(move(gmbus_connector))
{
}

ErrorOr<ByteBuffer> IntelNativeDisplayConnector::get_edid() const
{
    if (m_crt_edid.has_value())
        return ByteBuffer::copy(m_crt_edid_bytes, sizeof(m_crt_edid_bytes));
    return ByteBuffer {};
}

ErrorOr<void> IntelNativeDisplayConnector::set_resolution(DisplayConnector::Resolution const&)
{
    return Error::from_errno(ENOTIMPL);
}

ErrorOr<void> IntelNativeDisplayConnector::set_y_offset(size_t)
{
    return Error::from_errno(ENOTIMPL);
}

ErrorOr<DisplayConnector::Resolution> IntelNativeDisplayConnector::get_resolution()
{
    // FIXME: Get the refresh rate as this is real hardware...
    return Resolution { m_framebuffer_width, m_framebuffer_height, 32, {} };
}

ErrorOr<void> IntelNativeDisplayConnector::unblank()
{
    return Error::from_errno(ENOTIMPL);
}

ErrorOr<void> IntelNativeDisplayConnector::set_safe_resolution()
{
    auto modesetting = calculate_modesetting_from_edid(m_crt_edid.value(), 0);
    dmesgln("Intel Native Display Connector - safe resolution is {:d}x{:d}", modesetting.horizontal.active, modesetting.vertical.active);
    set_crt_resolution(modesetting.horizontal.active, modesetting.vertical.active);
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

void IntelNativeDisplayConnector::gmbus_read_edid()
{
    {
        SpinlockLocker control_lock(m_control_lock);
        m_gmbus_connector->write(DDC2_I2C_ADDRESS, 0);
        MUST(m_gmbus_connector->read(DDC2_I2C_ADDRESS, (u8*)&m_crt_edid_bytes, sizeof(m_crt_edid_bytes)));
        // FIXME: It seems like the returned EDID is almost correct,
        // but the first byte is set to 0xD0 instead of 0x00.
        // For now, this "hack" works well enough.
        m_crt_edid_bytes[0] = 0x0;
    }
    if (auto parsed_edid = EDID::Parser::from_bytes({ m_crt_edid_bytes, sizeof(m_crt_edid_bytes) }); !parsed_edid.is_error()) {
        m_crt_edid = parsed_edid.release_value();
    } else {
        for (size_t x = 0; x < 128; x = x + 16) {
            dmesgln("IntelNativeGraphicsAdapter: Print offending EDID");
            dmesgln("{:02x} {:02x} {:02x} {:02x} {:02x} {:02x} {:02x} {:02x} {:02x} {:02x} {:02x} {:02x} {:02x} {:02x} {:02x} {:02x}",
                m_crt_edid_bytes[x], m_crt_edid_bytes[x + 1], m_crt_edid_bytes[x + 2], m_crt_edid_bytes[x + 3],
                m_crt_edid_bytes[x + 4], m_crt_edid_bytes[x + 5], m_crt_edid_bytes[x + 6], m_crt_edid_bytes[x + 7],
                m_crt_edid_bytes[x + 8], m_crt_edid_bytes[x + 9], m_crt_edid_bytes[x + 10], m_crt_edid_bytes[x + 11],
                m_crt_edid_bytes[x + 12], m_crt_edid_bytes[x + 13], m_crt_edid_bytes[x + 14], m_crt_edid_bytes[x + 15]);
        }
        dmesgln("IntelNativeGraphicsAdapter: Parsing EDID failed: {}", parsed_edid.error());
        m_crt_edid = {};
    }
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

bool IntelNativeDisplayConnector::set_crt_resolution(size_t width, size_t height)
{
    SpinlockLocker control_lock(m_control_lock);
    SpinlockLocker modeset_lock(m_modeset_lock);
    if (!is_resolution_valid(width, height)) {
        return false;
    }

    // Note: Just in case we still allow access to VGA IO ports, disable it now.
    GraphicsManagement::the().disable_vga_emulation_access_permanently();

    // FIXME: Get the requested resolution from the EDID!!
    auto modesetting = calculate_modesetting_from_edid(m_crt_edid.value(), 0);

    disable_output();

    auto dac_multiplier = compute_dac_multiplier(modesetting.pixel_clock_in_khz);
    auto pll_settings = create_pll_settings((1000 * modesetting.pixel_clock_in_khz * dac_multiplier), 96'000'000, IntelGraphics::G35Limits);
    if (!pll_settings.has_value())
        VERIFY_NOT_REACHED();
    auto settings = pll_settings.value();
    dbgln_if(INTEL_GRAPHICS_DEBUG, "PLL settings for {} {} {} {} {}", settings.n, settings.m1, settings.m2, settings.p1, settings.p2);
    enable_dpll_without_vga(pll_settings.value(), dac_multiplier);
    set_display_timings(modesetting);
    VERIFY(m_framebuffer_address.has_value());
    enable_output(m_framebuffer_address.value(), width);

    m_framebuffer_width = width;
    m_framebuffer_height = height;
    m_framebuffer_pitch = width * 4;

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

    IO::delay(200);
}

bool IntelNativeDisplayConnector::wait_for_enabled_pipe_a(size_t milliseconds_timeout) const
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
bool IntelNativeDisplayConnector::wait_for_disabled_pipe_a(size_t milliseconds_timeout) const
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

bool IntelNativeDisplayConnector::wait_for_disabled_pipe_b(size_t milliseconds_timeout) const
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

    IO::delay(200);

    write_to_register(IntelGraphics::RegisterIndex::DPLLControlA, (6 << 9) | (settings.p1) << 16 | (1 << 26) | (1 << 28) | (1 << 31));
    write_to_register(IntelGraphics::RegisterIndex::DPLLMultiplierA, (dac_multiplier - 1) | ((dac_multiplier - 1) << 8));

    // The specification says we should wait (at least) about 150 microseconds
    // after enabling the DPLL to allow the clock to stabilize
    IO::delay(200);
    VERIFY(read_from_register(IntelGraphics::RegisterIndex::DPLLControlA) & (1 << 31));
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
