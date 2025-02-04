/*
 * Copyright (c) 2023, Edwin Rijkee <edwin@virtualparadise.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/GPU/3dfx/VoodooDisplayConnector.h>
#include <Kernel/Devices/GPU/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Devices/GPU/Management.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel::VoodooGraphics {

ErrorOr<NonnullRefPtr<VoodooDisplayConnector>> VoodooDisplayConnector::create(PhysicalAddress framebuffer_address, size_t framebuffer_resource_size, Memory::TypedMapping<RegisterMap volatile> registers_mapping, NonnullOwnPtr<IOWindow> io_window)
{
    auto connector = TRY(Device::try_create_device<VoodooDisplayConnector>(framebuffer_address, framebuffer_resource_size, move(registers_mapping), move(io_window)));
    TRY(connector->create_attached_framebuffer_console());
    TRY(connector->fetch_and_initialize_edid());
    return connector;
}

ErrorOr<void> VoodooDisplayConnector::fetch_and_initialize_edid()
{
    // TODO: actually fetch the EDID.
    return initialize_edid_for_generic_monitor({});
}

ErrorOr<void> VoodooDisplayConnector::create_attached_framebuffer_console()
{
    m_framebuffer_console = Graphics::ContiguousFramebufferConsole::initialize(m_framebuffer_address.value(), 1024, 768, 1024 * sizeof(u32));
    GraphicsManagement::the().set_console(*m_framebuffer_console);
    return {};
}

VoodooDisplayConnector::VoodooDisplayConnector(PhysicalAddress framebuffer_address, size_t framebuffer_resource_size, Memory::TypedMapping<RegisterMap volatile> registers_mapping, NonnullOwnPtr<IOWindow> io_window)
    : DisplayConnector(framebuffer_address, framebuffer_resource_size, Memory::MemoryType::NonCacheable)
    , m_registers(move(registers_mapping))
    , m_io_window(move(io_window))
{
}

ErrorOr<IterationDecision> VoodooDisplayConnector::for_each_dmt_timing_in_edid(Function<IterationDecision(EDID::DMT::MonitorTiming const&)> callback) const
{
    IterationDecision iteration_decision = TRY(m_edid_parser->for_each_standard_timing([&](auto& standard_timing) {
        auto timing = EDID::DMT::find_timing_by_dmt_id(standard_timing.dmt_id());
        if (!timing) {
            return IterationDecision::Continue;
        }

        return callback(*timing);
    }));

    if (iteration_decision == IterationDecision::Break) {
        return iteration_decision;
    }

    iteration_decision = TRY(m_edid_parser->for_each_established_timing([&](auto& established_timing) {
        auto timing = EDID::DMT::find_timing_by_dmt_id(established_timing.dmt_id());
        if (!timing) {
            return IterationDecision::Continue;
        }

        return callback(*timing);
    }));

    return iteration_decision;
}

auto VoodooDisplayConnector::find_suitable_mode(ModeSetting const& requested_mode) const -> ErrorOr<ModeSetting>
{
    u32 width = requested_mode.horizontal_active;
    u32 height = requested_mode.vertical_active;
    ModeSetting result = requested_mode;

    if (requested_mode.horizontal_stride == 0) {
        result.horizontal_stride = sizeof(u32) * width;
    }

    if (requested_mode.pixel_clock_in_khz != 0) {
        dbgln_if(TDFX_DEBUG, "3dfx: Requested mode {}x{} includes timing information", width, height);
        return result;
    }

    dbgln_if(TDFX_DEBUG, "3dfx: Looking for suitable mode with resolution {}x{}", width, height);

    IterationDecision iteration_decision = TRY(m_edid_parser->for_each_detailed_timing([&](auto& timing, unsigned) {
        dbgln_if(TDFX_DEBUG, "3dfx: Considering detailed timing {}x{} @ {}", timing.horizontal_addressable_pixels(), timing.vertical_addressable_lines(), timing.refresh_rate());

        if (timing.is_interlaced() || timing.horizontal_addressable_pixels() != width || timing.vertical_addressable_lines() != height) {
            return IterationDecision::Continue;
        }

        result.pixel_clock_in_khz = timing.pixel_clock_khz();
        result.horizontal_front_porch_pixels = timing.horizontal_front_porch_pixels();
        result.horizontal_sync_time_pixels = timing.horizontal_sync_pulse_width_pixels();
        result.horizontal_blank_pixels = timing.horizontal_blanking_pixels();
        result.vertical_front_porch_lines = timing.vertical_front_porch_lines();
        result.vertical_sync_time_lines = timing.vertical_sync_pulse_width_lines();
        result.vertical_blank_lines = timing.vertical_blanking_lines();
        return IterationDecision::Break;
    }));

    if (iteration_decision == IterationDecision::Break) {
        return result;
    }

    iteration_decision = TRY(for_each_dmt_timing_in_edid([&](auto& timing) {
        dbgln_if(TDFX_DEBUG, "3dfx: Considering DMT timing {}x{} @ {}", timing.horizontal_pixels, timing.vertical_lines, timing.vertical_frequency_hz());

        if (timing.scan_type != EDID::DMT::MonitorTiming::ScanType::NonInterlaced || timing.horizontal_pixels != width || timing.vertical_lines != height) {
            return IterationDecision::Continue;
        }

        result.pixel_clock_in_khz = timing.pixel_clock_khz;
        result.horizontal_front_porch_pixels = timing.horizontal_front_porch_pixels;
        result.horizontal_sync_time_pixels = timing.horizontal_sync_time_pixels;
        result.horizontal_blank_pixels = timing.horizontal_blank_pixels;
        result.vertical_front_porch_lines = timing.vertical_front_porch_lines;
        result.vertical_sync_time_lines = timing.vertical_sync_time_lines;
        result.vertical_blank_lines = timing.vertical_blank_lines;
        return IterationDecision::Break;
    }));

    if (iteration_decision == IterationDecision::Break) {
        return result;
    }

    dbgln_if(TDFX_DEBUG, "3dfx: No timing information available for display mode {}x{}", width, height);
    return EINVAL;
}

ErrorOr<void>
VoodooDisplayConnector::set_mode_setting(ModeSetting const& requested_mode_setting)
{
    SpinlockLocker locker(m_modeset_lock);
    VERIFY(m_framebuffer_console);

    ModeSetting mode_setting = TRY(find_suitable_mode(requested_mode_setting));
    dbgln_if(TDFX_DEBUG, "VoodooDisplayConnector resolution registers set to - {}x{}", mode_setting.horizontal_active, mode_setting.vertical_active);

    auto regs = TRY(prepare_mode_switch(mode_setting));
    TRY(perform_mode_switch(regs));

    m_framebuffer_console->set_resolution(mode_setting.horizontal_active, mode_setting.vertical_active, mode_setting.horizontal_stride);
    m_current_mode_setting = mode_setting;
    return {};
}

ErrorOr<void> VoodooDisplayConnector::set_y_offset(size_t)
{
    return ENOTIMPL;
}

ErrorOr<void> VoodooDisplayConnector::set_safe_mode_setting()
{
    DisplayConnector::ModeSetting safe_mode_set {
        .horizontal_stride = 1024 * sizeof(u32),
        .pixel_clock_in_khz = 65000,
        .horizontal_active = 1024,
        .horizontal_front_porch_pixels = 24,
        .horizontal_sync_time_pixels = 136,
        .horizontal_blank_pixels = 320,
        .vertical_active = 768,
        .vertical_front_porch_lines = 3,
        .vertical_sync_time_lines = 6,
        .vertical_blank_lines = 38,
        .horizontal_offset = 0,
        .vertical_offset = 0,
    };
    return set_mode_setting(safe_mode_set);
}

ErrorOr<void> VoodooDisplayConnector::unblank()
{
    return ENOTIMPL;
}

ErrorOr<void> VoodooDisplayConnector::flush_first_surface()
{
    return ENOTSUP;
}

void VoodooDisplayConnector::enable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->enable();
}

void VoodooDisplayConnector::disable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->disable();
}

u8 VoodooDisplayConnector::read_vga(VGAPort port)
{
    return m_io_window->read8(static_cast<u16>(port) - 0x300);
}

u8 VoodooDisplayConnector::read_vga_indexed(VGAPort index_port, VGAPort data_port, u8 index)
{
    m_io_window->write8(static_cast<u16>(index_port) - 0x300, index);
    return m_io_window->read8(static_cast<u16>(data_port) - 0x300);
}

void VoodooDisplayConnector::write_vga(VGAPort port, u8 value)
{
    m_io_window->write8(static_cast<u16>(port) - 0x300, value - 0x300);
}

void VoodooDisplayConnector::write_vga_indexed(VGAPort index_port, VGAPort data_port, u8 index, u8 value)
{
    m_io_window->write8(static_cast<u16>(index_port) - 0x300, index);
    m_io_window->write8(static_cast<u16>(data_port) - 0x300, value);
}

ErrorOr<void> VoodooDisplayConnector::wait_for_fifo_space(u32 entries)
{
    VERIFY(entries < 32);

    auto deadline = TimeManagement::the().monotonic_time() + Duration::from_seconds(1);
    do {
        if ((m_registers->status & 0x1f) >= entries) {
            return {};
        }
        (void)Thread::current()->sleep(Duration::from_milliseconds(1));
    } while (TimeManagement::the().monotonic_time() < deadline);

    dbgln_if(TDFX_DEBUG, "3dfx: timed out waiting for fifo space");
    return EIO;
}

PLLSettings VoodooDisplayConnector::calculate_pll(i32 desired_frequency_in_khz)
{
    VoodooGraphics::PLLSettings current;
    VoodooGraphics::PLLSettings best;
    i32 best_difference;

    best_difference = desired_frequency_in_khz;

    for (current.m = 0; current.m < 64; current.m++) {
        for (current.n = 0; current.n < 256; current.n++) {
            for (current.k = 0; current.k < 4; current.k++) {
                auto frequency_in_khz = current.frequency_in_khz();

                auto error = AK::abs(frequency_in_khz - desired_frequency_in_khz);
                if (error < best_difference) {
                    best_difference = error;
                    best = current;
                }
            }
        }
    }

    return best;
}

ErrorOr<ModeRegisters> VoodooDisplayConnector::prepare_mode_switch(ModeSetting const& mode_setting)
{
    u32 width = mode_setting.horizontal_active;
    u32 height = mode_setting.vertical_active;

    ModeRegisters regs;

    regs.vga_init0 = EnableVgaExtensions | WakeUpSelect3C3 | EnableAltReadback | FIFODepth8Bit | ExtendedShiftOut;
    regs.vid_proc_cfg |= VideoProcessorEnable | DesktopSurfaceEnable | DesktopPixelFormat32Bit | DesktopCLUTBypass;

    // We only want to touch the 2X flag of the DAC Mode register, the other flags are preserved
    regs.dac_mode = m_registers->dac_mode & ~DacMode2x;

    u32 const max_pixel_clock_in_khz = 270000;
    if (mode_setting.pixel_clock_in_khz > max_pixel_clock_in_khz) {
        return ENOTSUP;
    }

    u32 horizontal_divisor = 8;
    if (mode_setting.pixel_clock_in_khz > max_pixel_clock_in_khz / 2) {
        horizontal_divisor = 16;
        regs.dac_mode |= DacMode2x;
        regs.vid_proc_cfg |= TwoXMode;
    }

    dbgln_if(TDFX_DEBUG, "3dfx: Calculating best PLL settings for pixel clock {} KHz", mode_setting.pixel_clock_in_khz);
    auto pll = calculate_pll(mode_setting.pixel_clock_in_khz);
    dbgln_if(TDFX_DEBUG, "3dfx: Best matching PLL settings: m={}, n={}, k={}. Frequency: {} KHz", pll.m, pll.n, pll.k, pll.frequency_in_khz());
    regs.pll_ctrl0 = pll.register_value();
    regs.vid_screen_size = width | (height << 12);
    regs.vid_desktop_overlay_stride = mode_setting.horizontal_stride;
    regs.misc_out_reg = ClockSelectPLL | CRTCAddressColor;
    if (height < 768) {
        regs.misc_out_reg |= VerticalSyncPositive | HorizontalSyncPositive;
    }

    u32 hor_total = mode_setting.horizontal_total() / horizontal_divisor - 5;
    u32 hor_disp_en_end = width / horizontal_divisor - 1;
    u32 hor_sync_start = mode_setting.horizontal_sync_start() / horizontal_divisor;
    u32 hor_sync_end = (mode_setting.horizontal_sync_end() / horizontal_divisor) & 0x1f;
    u32 hor_blank_start = hor_disp_en_end;
    u32 hor_blank_end = hor_total & 0x7f;

    u32 vert_total = mode_setting.vertical_total() - 2;
    u32 vert_disp_en_end = height - 1;
    u32 vert_sync_start = mode_setting.vertical_sync_start();
    u32 vert_sync_end = mode_setting.vertical_sync_end() & 0xf;
    u32 vert_blank_start = mode_setting.vertical_blanking_start() - 1;
    u32 vert_blank_end = (mode_setting.vertical_total() - 1) & 0xff;

    if (hor_total > 0x1ff ||        // 9-bit field
        hor_disp_en_end > 0x1ff ||  // 9-bit field
        hor_sync_start > 0x1ff ||   // 9-bit field
        hor_sync_end > 0x1f ||      // 5-bit field
        hor_blank_start > 0x1ff ||  // 9-bit field
        hor_blank_end > 0x7f ||     // 7-bit field
        vert_total > 0x7ff ||       // 11-bit field
        vert_disp_en_end > 0x7ff || // 11-bit field
        vert_sync_start > 0x7ff ||  // 11-bit field
        vert_sync_end > 0x0f ||     // 4-bit field
        vert_blank_start > 0x7ff || // 11-bit field
        vert_blank_end > 0xff       // 8-bit field
    ) {
        dbgln_if(TDFX_DEBUG, "3dfx: One of the timing values is too large to fit in its register");
        return EOVERFLOW;
    }

    // CRT Controller Registers
    regs.cr.horizontal_total = hor_total;                    // bit 0-7 of hor_total
    regs.cr.horizontal_display_enable_end = hor_disp_en_end; // bit 0-7 of hor_disp_en_end
    regs.cr.horizontal_blanking_start = hor_blank_start;     // bit 0-7 of hor_blank_start
    regs.cr.horizontal_blanking_end = (hor_blank_end & 0x1f) // bit 0-4 of hor_blank_end
        | CompatibilityRead;
    regs.cr.horizontal_sync_start = hor_sync_start;                // bit 0-7 of hor_sync_start
    regs.cr.horizontal_sync_end = ((hor_sync_end & 0x1f)           // bit 0-4 of hor_sync_end
        | ((hor_blank_end & 0x20) << 2));                          // bit 5 of hor_blank_end
    regs.cr.vertical_total = vert_total;                           // bit 0-7 of vert_total
    regs.cr.overflow = (((vert_total & 0x100) >> 8)                // bit 8 of vert_total
        | ((vert_disp_en_end & 0x100) >> 7)                        // bit 8 of vert_disp_en_end
        | ((vert_sync_start & 0x100) >> 6)                         // bit 8 of vert_sync_start
        | ((vert_blank_start & 0x100) >> 5)                        // bit 8 of vert_blank_start
        | ((vert_total & 0x200) >> 4)                              // bit 9 of vert_disp_en_end
        | ((vert_disp_en_end & 0x200) >> 3)                        // bit 9 of vert_disp_en_end
        | ((vert_sync_start & 0x200) >> 2));                       // bit 9 of vert_sync_start
    regs.cr.maximum_scan_line = ((vert_blank_start & 0x200) >> 4); // bit 9 of vert_blank_start
    regs.cr.vertical_sync_start = vert_sync_start;                 // bit 0-7 of vert_sync_start
    regs.cr.vertical_sync_end = (vert_sync_end & 0x0f)             // bit 0-3 of vert_sync_end
        | EnableVertInt;
    regs.cr.vertical_display_enable_end = vert_disp_en_end; // bit 0-7 of vert_disp_en_end
    regs.cr.vertical_blanking_start = vert_blank_start;     // bit 0-7 of vert_blank_start
    regs.cr.vertical_blanking_end = vert_blank_end;         // bit 0-7 of vert_blank_end
    regs.cr.mode_control = TimingEnable | ByteWordMode;
    regs.cr.horizontal_extensions = (hor_total & 0x100) >> 8 // bit 8 of hor_total
        | (hor_disp_en_end & 0x100) >> 6                     // bit 8 of hor_disp_en_end
        | (hor_blank_start & 0x100) >> 4                     // bit 8 of hor_blank_start
        | (hor_blank_end & 0x40) >> 1                        // bit 6 of hor_blank_end
        | (hor_sync_start & 0x100) >> 2                      // bit 8 of hor_sync_start
        | (hor_sync_end & 0x20) << 2;                        // bit 5 of hor_sync_end
    regs.cr.vertical_extensions = (vert_total & 0x400) >> 10 // bit 10 of vert_total
        | (vert_disp_en_end & 0x400) >> 8                    // bit 10 of vert_disp_en_endx
        | (vert_blank_start & 0x400) >> 6                    // bit 10 of vert_blank_start
        | (vert_blank_end & 0x400) >> 4                      // bit 10 of vert_blank_end
        | (vert_sync_start & 0x400) >> 4;                    // bit 10 of vert_sync_start
    // Graphics Controller Registers
    regs.gr.graphics_controller_miscellaneous = MemoryMapEGAVGAExtended;

    // Attribute Controller Registers
    regs.ar.attribute_controller_mode = GraphicsMode | PixelWidth;

    // Sequencer Registers
    regs.sr.sequencer_reset = AsynchronousReset | SynchronousReset;
    regs.sr.sequencer_clocking_mode = DotClock8;

    return regs;
}

ErrorOr<void> VoodooDisplayConnector::perform_mode_switch(ModeRegisters const& regs)
{
    TRY(wait_for_fifo_space(2));
    m_registers->vid_proc_cfg = 0;
    m_registers->pll_ctrl0 = regs.pll_ctrl0;

    write_vga(VGAPort::MiscOutputWrite, regs.misc_out_reg);
    for (u8 i = 0; i < regs.sr_data.size(); i++) {
        write_vga_indexed(VGAPort::SequencerIndex, VGAPort::SequencerData, i, regs.sr_data[i]);
    }

    // first unprotect CR0 - CR7
    write_vga_indexed(VGAPort::CrtcIndex, VGAPort::CrtcData, 0x11, read_vga_indexed(VGAPort::CrtcIndex, VGAPort::CrtcData, 0x11) & ~CRTCRegsWriteProt);
    for (u8 i = 0; i < regs.cr_data.size(); i++) {
        write_vga_indexed(VGAPort::CrtcIndex, VGAPort::CrtcData, i, regs.cr_data[i]);
    }

    for (u8 i = 0; i < regs.gr_data.size(); i++) {
        write_vga_indexed(VGAPort::GraphicsControllerIndex, VGAPort::GraphicsControllerData, i, regs.gr_data[i]);
    }

    // The AttributeController IO port flips between the index and the data register on every write.
    // Reading InputStatus1 has the side effect of resetting this, this way we know it is in the state we expect
    read_vga(VGAPort::InputStatus1);
    for (u8 i = 0; i < regs.ar_data.size(); i++) {
        write_vga_indexed(VGAPort::AttributeController, VGAPort::AttributeController, i, regs.ar_data[i]);
    }

    TRY(wait_for_fifo_space(6));
    m_registers->vga_init0 = regs.vga_init0;
    m_registers->dac_mode = regs.dac_mode;
    m_registers->vid_desktop_overlay_stride = regs.vid_desktop_overlay_stride;
    m_registers->vid_screen_size = regs.vid_screen_size;
    m_registers->vid_desktop_start_addr = 0;
    m_registers->vid_proc_cfg = regs.vid_proc_cfg;

    return {};
}
}
