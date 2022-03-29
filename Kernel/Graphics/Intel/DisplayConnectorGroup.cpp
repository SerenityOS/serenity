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
#include <Kernel/Graphics/Intel/DisplayConnectorGroup.h>
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

Optional<IntelGraphics::PLLSettings> IntelDisplayConnectorGroup::create_pll_settings(u64 target_frequency, u64 reference_clock, IntelGraphics::PLLMaxSettings const& limits)
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

ErrorOr<NonnullLockRefPtr<IntelDisplayConnectorGroup>> IntelDisplayConnectorGroup::try_create(MMIORegion const& first_region, MMIORegion const& second_region)
{
    auto registers_region = TRY(MM.allocate_kernel_region(first_region.pci_bar_paddr, first_region.pci_bar_space_length, "Intel Native Graphics Registers"sv, Memory::Region::Access::ReadWrite));
    // FIXME: Try to put the address as parameter to this function to allow creating this DisplayConnector for many generations...
    auto gmbus_connector = TRY(GMBusConnector::create_with_physical_address(first_region.pci_bar_paddr.offset(to_underlying(IntelGraphics::RegisterIndex::GMBusClock))));
    auto connector_group = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) IntelDisplayConnectorGroup(move(gmbus_connector), move(registers_region), first_region, second_region)));
    TRY(connector_group->initialize_connectors());
    return connector_group;
}

IntelDisplayConnectorGroup::IntelDisplayConnectorGroup(NonnullOwnPtr<GMBusConnector> gmbus_connector, NonnullOwnPtr<Memory::Region> registers_region, MMIORegion const& first_region, MMIORegion const& second_region)
    : m_mmio_first_region(first_region)
    , m_mmio_second_region(second_region)
    , m_assigned_mmio_registers_region(m_mmio_first_region)
    , m_registers_region(move(registers_region))
    , m_gmbus_connector(move(gmbus_connector))
{
}

ErrorOr<void> IntelDisplayConnectorGroup::initialize_connectors()
{
    m_connectors[0] = TRY(IntelNativeDisplayConnector::try_create(*this, m_mmio_second_region.pci_bar_paddr, m_mmio_second_region.pci_bar_space_length));
    Array<u8, 128> crt_edid_bytes {};
    {
        SpinlockLocker control_lock(m_control_lock);
        MUST(m_gmbus_connector->write(DDC2_I2C_ADDRESS, 0));
        MUST(m_gmbus_connector->read(DDC2_I2C_ADDRESS, crt_edid_bytes.data(), sizeof(crt_edid_bytes)));

        // FIXME: It seems like the returned EDID is almost correct,
        // but the first byte is set to 0xD0 instead of 0x00.
        // For now, this "hack" works well enough.
        crt_edid_bytes[0] = 0x0;
    }
    m_connectors[0]->set_edid_bytes({}, crt_edid_bytes);
    TRY(m_connectors[0]->set_safe_mode_setting());
    TRY(m_connectors[0]->create_attached_framebuffer_console({}));
    return {};
}

ErrorOr<void> IntelDisplayConnectorGroup::set_safe_mode_setting(Badge<IntelNativeDisplayConnector>, IntelNativeDisplayConnector& connector)
{
    if (!m_connectors[0]->m_edid_parser.has_value())
        return Error::from_errno(ENOTSUP);
    if (!m_connectors[0]->m_edid_parser.value().detailed_timing(0).has_value())
        return Error::from_errno(ENOTSUP);
    auto details = m_connectors[0]->m_edid_parser.value().detailed_timing(0).release_value();

    DisplayConnector::ModeSetting modesetting {
        // Note: We assume that we always use 32 bit framebuffers.
        .horizontal_stride = details.horizontal_addressable_pixels() * sizeof(u32),
        .pixel_clock_in_khz = details.pixel_clock_khz(),
        .horizontal_active = details.horizontal_addressable_pixels(),
        .horizontal_front_porch_pixels = details.horizontal_front_porch_pixels(),
        .horizontal_sync_time_pixels = details.horizontal_sync_pulse_width_pixels(),
        .horizontal_blank_pixels = details.horizontal_blanking_pixels(),
        .vertical_active = details.vertical_addressable_lines(),
        .vertical_front_porch_lines = details.vertical_front_porch_lines(),
        .vertical_sync_time_lines = details.vertical_sync_pulse_width_lines(),
        .vertical_blank_lines = details.vertical_blanking_lines(),
        .horizontal_offset = 0,
        .vertical_offset = 0,
    };

    return set_mode_setting(connector, modesetting);
}

ErrorOr<void> IntelDisplayConnectorGroup::set_mode_setting(Badge<IntelNativeDisplayConnector>, IntelNativeDisplayConnector& connector, DisplayConnector::ModeSetting const& mode_setting)
{
    return set_mode_setting(connector, mode_setting);
}

ErrorOr<void> IntelDisplayConnectorGroup::set_mode_setting(IntelNativeDisplayConnector& connector, DisplayConnector::ModeSetting const& mode_setting)
{
    SpinlockLocker locker(connector.m_modeset_lock);
    VERIFY(const_cast<IntelNativeDisplayConnector*>(&connector) == m_connectors[0].ptr());
    DisplayConnector::ModeSetting actual_mode_setting = mode_setting;
    actual_mode_setting.horizontal_stride = actual_mode_setting.horizontal_active * sizeof(u32);
    VERIFY(actual_mode_setting.horizontal_stride != 0);
    if (!set_crt_resolution(actual_mode_setting))
        return Error::from_errno(ENOTSUP);
    connector.m_current_mode_setting = actual_mode_setting;
    if (!connector.m_framebuffer_console.is_null())
        static_cast<Graphics::GenericFramebufferConsoleImpl*>(connector.m_framebuffer_console.ptr())->set_resolution(actual_mode_setting.horizontal_active, actual_mode_setting.vertical_active, actual_mode_setting.horizontal_stride);
    return {};
}

void IntelDisplayConnectorGroup::enable_vga_plane()
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

void IntelDisplayConnectorGroup::write_to_register(IntelGraphics::RegisterIndex index, u32 value) const
{
    VERIFY(m_control_lock.is_locked());
    SpinlockLocker lock(m_registers_lock);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Intel Graphics Display Connector:: Write to {} value of {:x}", convert_register_index_to_string(index), value);
    auto* reg = (u32 volatile*)m_registers_region->vaddr().offset(to_underlying(index)).as_ptr();
    *reg = value;
}
u32 IntelDisplayConnectorGroup::read_from_register(IntelGraphics::RegisterIndex index) const
{
    VERIFY(m_control_lock.is_locked());
    SpinlockLocker lock(m_registers_lock);
    auto* reg = (u32 volatile*)m_registers_region->vaddr().offset(to_underlying(index)).as_ptr();
    u32 value = *reg;
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Intel Graphics Display Connector: Read from {} value of {:x}", convert_register_index_to_string(index), value);
    return value;
}

bool IntelDisplayConnectorGroup::pipe_a_enabled() const
{
    VERIFY(m_control_lock.is_locked());
    return read_from_register(IntelGraphics::RegisterIndex::PipeAConf) & (1 << 30);
}

bool IntelDisplayConnectorGroup::pipe_b_enabled() const
{
    VERIFY(m_control_lock.is_locked());
    return read_from_register(IntelGraphics::RegisterIndex::PipeBConf) & (1 << 30);
}

void IntelDisplayConnectorGroup::disable_output()
{
    VERIFY(m_control_lock.is_locked());
    disable_dac_output();
    disable_all_planes();
    disable_pipe_a();
    disable_pipe_b();
    disable_dpll();
    disable_vga_emulation();
}

void IntelDisplayConnectorGroup::enable_output(size_t width)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(!pipe_a_enabled());
    enable_pipe_a();
    enable_primary_plane(width);
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

bool IntelDisplayConnectorGroup::set_crt_resolution(DisplayConnector::ModeSetting const& mode_setting)
{
    SpinlockLocker control_lock(m_control_lock);
    SpinlockLocker modeset_lock(m_modeset_lock);

    // Note: Just in case we still allow access to VGA IO ports, disable it now.
    GraphicsManagement::the().disable_vga_emulation_access_permanently();

    auto dac_multiplier = compute_dac_multiplier(mode_setting.pixel_clock_in_khz);
    auto pll_settings = create_pll_settings((1000 * mode_setting.pixel_clock_in_khz * dac_multiplier), 96'000'000, IntelGraphics::G35Limits);
    if (!pll_settings.has_value())
        return false;
    auto settings = pll_settings.value();

    disable_output();
    dbgln_if(INTEL_GRAPHICS_DEBUG, "PLL settings for {} {} {} {} {}", settings.n, settings.m1, settings.m2, settings.p1, settings.p2);
    enable_dpll_without_vga(settings, dac_multiplier);
    set_display_timings(mode_setting);
    enable_output(mode_setting.horizontal_active);

    return true;
}

void IntelDisplayConnectorGroup::set_display_timings(DisplayConnector::ModeSetting const& mode_setting)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    VERIFY(!(read_from_register(IntelGraphics::RegisterIndex::PipeAConf) & (1 << 31)));
    VERIFY(!(read_from_register(IntelGraphics::RegisterIndex::PipeAConf) & (1 << 30)));

    dbgln_if(INTEL_GRAPHICS_DEBUG, "htotal - {}, {}", (mode_setting.horizontal_active - 1), (mode_setting.horizontal_total() - 1));
    write_to_register(IntelGraphics::RegisterIndex::HTotalA, (mode_setting.horizontal_active - 1) | (mode_setting.horizontal_total() - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "hblank - {}, {}", (mode_setting.horizontal_blanking_start() - 1), (mode_setting.horizontal_blanking_start() + mode_setting.horizontal_blank_pixels - 1));
    write_to_register(IntelGraphics::RegisterIndex::HBlankA, (mode_setting.horizontal_blanking_start() - 1) | (mode_setting.horizontal_blanking_start() + mode_setting.horizontal_blank_pixels - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "hsync - {}, {}", (mode_setting.horizontal_sync_start() - 1), (mode_setting.horizontal_sync_end() - 1));
    write_to_register(IntelGraphics::RegisterIndex::HSyncA, (mode_setting.horizontal_sync_start() - 1) | (mode_setting.horizontal_sync_end() - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "vtotal - {}, {}", (mode_setting.vertical_active - 1), (mode_setting.vertical_blanking_start() + mode_setting.vertical_blank_lines - 1));
    write_to_register(IntelGraphics::RegisterIndex::VTotalA, (mode_setting.vertical_active - 1) | (mode_setting.vertical_blanking_start() + mode_setting.vertical_blank_lines - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "vblank - {}, {}", (mode_setting.vertical_blanking_start() - 1), (mode_setting.vertical_blanking_start() + mode_setting.vertical_blank_lines - 1));
    write_to_register(IntelGraphics::RegisterIndex::VBlankA, (mode_setting.vertical_blanking_start() - 1) | (mode_setting.vertical_blanking_start() + mode_setting.vertical_blank_lines - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "vsync - {}, {}", (mode_setting.vertical_sync_start() - 1), (mode_setting.vertical_sync_end() - 1));
    write_to_register(IntelGraphics::RegisterIndex::VSyncA, (mode_setting.vertical_sync_start() - 1) | (mode_setting.vertical_sync_end() - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "sourceSize - {}, {}", (mode_setting.vertical_active - 1), (mode_setting.horizontal_active - 1));
    write_to_register(IntelGraphics::RegisterIndex::PipeASource, (mode_setting.vertical_active - 1) | (mode_setting.horizontal_active - 1) << 16);

    microseconds_delay(200);
}

bool IntelDisplayConnectorGroup::wait_for_enabled_pipe_a(size_t milliseconds_timeout) const
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
bool IntelDisplayConnectorGroup::wait_for_disabled_pipe_a(size_t milliseconds_timeout) const
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

bool IntelDisplayConnectorGroup::wait_for_disabled_pipe_b(size_t milliseconds_timeout) const
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

void IntelDisplayConnectorGroup::disable_dpll()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::DPLLControlA, 0);
    write_to_register(IntelGraphics::RegisterIndex::DPLLControlB, 0);
}

void IntelDisplayConnectorGroup::disable_pipe_a()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::PipeAConf, 0);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Disabling Pipe A");
    wait_for_disabled_pipe_a(100);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Disabling Pipe A - done.");
}

void IntelDisplayConnectorGroup::disable_pipe_b()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::PipeAConf, 0);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Disabling Pipe B");
    wait_for_disabled_pipe_b(100);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Disabling Pipe B - done.");
}

void IntelDisplayConnectorGroup::enable_pipe_a()
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

void IntelDisplayConnectorGroup::enable_primary_plane(size_t width)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    VERIFY(((width * 4) % 64 == 0));

    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneAStride, width * 4);
    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneALinearOffset, 0);
    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneASurface, m_mmio_second_region.pci_bar_paddr.get());

    // FIXME: Serenity uses BGR 32 bit pixel format, but maybe we should try to determine it somehow!
    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneAControl, (0b0110 << 26) | (1 << 31));
}

void IntelDisplayConnectorGroup::set_dpll_registers(IntelGraphics::PLLSettings const& settings)
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::DPLLDivisorA0, (settings.m2 - 2) | ((settings.m1 - 2) << 8) | ((settings.n - 2) << 16));
    write_to_register(IntelGraphics::RegisterIndex::DPLLDivisorA1, (settings.m2 - 2) | ((settings.m1 - 2) << 8) | ((settings.n - 2) << 16));

    write_to_register(IntelGraphics::RegisterIndex::DPLLControlA, 0);
}

void IntelDisplayConnectorGroup::enable_dpll_without_vga(IntelGraphics::PLLSettings const& settings, size_t dac_multiplier)
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

void IntelDisplayConnectorGroup::disable_dac_output()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::AnalogDisplayPort, 0b11 << 10);
}

void IntelDisplayConnectorGroup::enable_dac_output()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::AnalogDisplayPort, (1 << 31));
}

void IntelDisplayConnectorGroup::disable_vga_emulation()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::VGADisplayPlaneControl, (1 << 31));
    read_from_register(IntelGraphics::RegisterIndex::VGADisplayPlaneControl);
}

void IntelDisplayConnectorGroup::disable_all_planes()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_modeset_lock.is_locked());
    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneAControl, 0);
    write_to_register(IntelGraphics::RegisterIndex::DisplayPlaneBControl, 0);
}

}
