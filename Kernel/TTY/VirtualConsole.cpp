/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/HIDManagement.h>
#include <Kernel/Devices/PCSpeaker.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/Sections.h>
#include <Kernel/StdLib.h>
#include <Kernel/TTY/ConsoleManagement.h>
#include <Kernel/TTY/VirtualConsole.h>
#include <LibVT/Color.h>

namespace Kernel {

ConsoleImpl::ConsoleImpl(VirtualConsole& client)
    : Terminal(client)
{
}

void ConsoleImpl::invalidate_cursor()
{
}
void ConsoleImpl::clear()
{
    m_client.clear();
}
void ConsoleImpl::clear_history()
{
}

void ConsoleImpl::set_size(u16 determined_columns, u16 determined_rows)
{
    VERIFY(determined_columns);
    VERIFY(determined_rows);

    if (determined_columns == columns() && determined_rows == rows())
        return;

    m_columns = determined_columns;
    m_rows = determined_rows;

    m_scroll_region_top = 0;
    m_scroll_region_bottom = determined_rows - 1;

    m_current_state.cursor.clamp(rows() - 1, columns() - 1);
    m_normal_saved_state.cursor.clamp(rows() - 1, columns() - 1);
    m_alternate_saved_state.cursor.clamp(rows() - 1, columns() - 1);
    m_saved_cursor_position.clamp(rows() - 1, columns() - 1);
    m_horizontal_tabs.resize(determined_columns);
    for (unsigned i = 0; i < determined_columns; ++i)
        m_horizontal_tabs[i] = (i % 8) == 0;
    // Rightmost column is always last tab on line.
    m_horizontal_tabs[determined_columns - 1] = 1;
    m_client.terminal_did_resize(m_columns, m_rows);
}
void ConsoleImpl::scroll_up(u16 region_top, u16 region_bottom, size_t count)
{
    // NOTE: We have to invalidate the cursor first.
    m_client.invalidate_cursor(cursor_row());
    m_client.scroll_up(region_top, region_bottom, count);
}

void ConsoleImpl::scroll_down(u16 region_top, u16 region_bottom, size_t count)
{
    m_client.invalidate_cursor(cursor_row());
    m_client.scroll_down(region_top, region_bottom, count);
}

void ConsoleImpl::put_character_at(unsigned row, unsigned column, u32 ch)
{
    m_client.put_character_at(row, column, ch, m_current_state.attribute);
    m_last_code_point = ch;
}

void ConsoleImpl::clear_in_line(u16 row, u16 first_column, u16 last_column)
{
    m_client.clear_in_line(row, first_column, last_column);
}

void ConsoleImpl::scroll_left(u16 row, u16 column, size_t count)
{
    m_client.scroll_left(row, column, count);
}

void ConsoleImpl::scroll_right(u16 row, u16 column, size_t count)
{
    m_client.scroll_right(row, column, count);
}

void VirtualConsole::set_graphical(bool graphical)
{
    m_graphical = graphical;
}

ErrorOr<NonnullOwnPtr<KString>> VirtualConsole::pseudo_name() const
{
    return KString::formatted("tty:{}", m_index);
}

UNMAP_AFTER_INIT NonnullRefPtr<VirtualConsole> VirtualConsole::create(size_t index)
{
    auto virtual_console_or_error = DeviceManagement::try_create_device<VirtualConsole>(index);
    // FIXME: Find a way to propagate errors
    VERIFY(!virtual_console_or_error.is_error());
    return virtual_console_or_error.release_value();
}

UNMAP_AFTER_INIT NonnullRefPtr<VirtualConsole> VirtualConsole::create_with_preset_log(size_t index, CircularQueue<char, 16384> const& log)
{
    auto virtual_console = VirtualConsole::create(index);
    // HACK: We have to go through the TTY layer for correct newline handling.
    // It would be nice to not have to make all these calls, but we can't get the underlying data pointer
    // and head index. If we did that, we could reduce this to at most 2 calls.
    for (auto ch : log) {
        virtual_console->emit_char(ch);
    }
    return virtual_console;
}

UNMAP_AFTER_INIT void VirtualConsole::initialize()
{
    VERIFY(GraphicsManagement::the().console());
    set_size(GraphicsManagement::the().console()->max_column(), GraphicsManagement::the().console()->max_row());
    m_console_impl.set_size(GraphicsManagement::the().console()->max_column(), GraphicsManagement::the().console()->max_row());

    // Allocate twice of the max row * max column * sizeof(Cell) to ensure we can have some sort of history mechanism...
    auto size = GraphicsManagement::the().console()->max_column() * GraphicsManagement::the().console()->max_row() * sizeof(Cell) * 2;
    m_cells = MM.allocate_kernel_region(Memory::page_round_up(size).release_value_but_fixme_should_propagate_errors(), "Virtual Console Cells", Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow).release_value();

    // Add the lines, so we also ensure they will be flushed now
    for (size_t row = 0; row < rows(); row++) {
        m_lines.append({ true, 0 });
    }
    VERIFY(m_cells);
}

void VirtualConsole::refresh_after_resolution_change()
{
    auto old_rows_count = rows();
    auto old_columns_count = columns();
    set_size(GraphicsManagement::the().console()->max_column(), GraphicsManagement::the().console()->max_row());
    m_console_impl.set_size(GraphicsManagement::the().console()->max_column(), GraphicsManagement::the().console()->max_row());

    // Note: From now on, columns() and rows() are updated with the new settings.

    auto size = GraphicsManagement::the().console()->max_column() * GraphicsManagement::the().console()->max_row() * sizeof(Cell) * 2;
    auto new_cells = MM.allocate_kernel_region(Memory::page_round_up(size).release_value_but_fixme_should_propagate_errors(), "Virtual Console Cells", Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow).release_value();

    if (rows() < old_rows_count) {
        m_lines.shrink(rows());
    } else {
        for (size_t row = 0; row < (size_t)(rows() - old_rows_count); row++) {
            m_lines.append({ true, 0 });
        }
    }

    // Note: A potential loss of displayed data occur when resolution width shrinks.
    auto common_rows_count = min(old_rows_count, rows());
    auto common_columns_count = min(old_columns_count, columns());
    for (size_t row = 0; row < common_rows_count; row++) {
        auto& line = m_lines[row];
        memcpy(new_cells->vaddr().offset(row * columns() * sizeof(Cell)).as_ptr(), m_cells->vaddr().offset(row * old_columns_count * sizeof(Cell)).as_ptr(), common_columns_count * sizeof(Cell));
        line.dirty = true;
    }

    // Update the new cells Region
    m_cells = move(new_cells);
    m_console_impl.m_need_full_flush = true;
    flush_dirty_lines();
}

UNMAP_AFTER_INIT VirtualConsole::VirtualConsole(unsigned const index)
    : TTY(4, index)
    , m_index(index)
    , m_console_impl(*this)
{
    initialize();
}

UNMAP_AFTER_INIT VirtualConsole::~VirtualConsole()
{
    VERIFY_NOT_REACHED();
}

static inline Graphics::Console::Color ansi_color_to_standard_vga_color(VT::Color::ANSIColor color)
{
    switch (color) {
    case VT::Color::ANSIColor::DefaultBackground:
    case VT::Color::ANSIColor::Black:
        return Graphics::Console::Color::Black;
    case VT::Color::ANSIColor::Red:
        return Graphics::Console::Color::Red;
    case VT::Color::ANSIColor::Green:
        return Graphics::Console::Color::Green;
    case VT::Color::ANSIColor::Yellow:
        // VGA only has bright yellow, and treats normal yellow as a brownish orange color.
        return Graphics::Console::Color::Brown;
    case VT::Color::ANSIColor::Blue:
        return Graphics::Console::Color::Blue;
    case VT::Color::ANSIColor::Magenta:
        return Graphics::Console::Color::Magenta;
    case VT::Color::ANSIColor::Cyan:
        return Graphics::Console::Color::Cyan;
    case VT::Color::ANSIColor::DefaultForeground:
    case VT::Color::ANSIColor::White:
        return Graphics::Console::Color::LightGray;
    case VT::Color::ANSIColor::BrightBlack:
        return Graphics::Console::Color::DarkGray;
    case VT::Color::ANSIColor::BrightRed:
        return Graphics::Console::Color::BrightRed;
    case VT::Color::ANSIColor::BrightGreen:
        return Graphics::Console::Color::BrightGreen;
    case VT::Color::ANSIColor::BrightYellow:
        return Graphics::Console::Color::Yellow;
    case VT::Color::ANSIColor::BrightBlue:
        return Graphics::Console::Color::BrightBlue;
    case VT::Color::ANSIColor::BrightMagenta:
        return Graphics::Console::Color::BrightMagenta;
    case VT::Color::ANSIColor::BrightCyan:
        return Graphics::Console::Color::BrightCyan;
    case VT::Color::ANSIColor::BrightWhite:
        return Graphics::Console::Color::White;
    }
    VERIFY_NOT_REACHED();
}

static inline Graphics::Console::Color terminal_to_standard_color(VT::Color color)
{
    switch (color.kind()) {
    case VT::Color::Kind::Named:
        return ansi_color_to_standard_vga_color(color.as_named());
    default:
        return Graphics::Console::Color::LightGray;
    }
}

void VirtualConsole::on_key_pressed(KeyEvent event)
{
    // Ignore keyboard in graphical mode.
    if (m_graphical)
        return;

    if (!event.is_press())
        return;

    Processor::deferred_call_queue([this, event]() {
        m_console_impl.handle_key_press(event.key, event.code_point, event.flags);
    });
}

ErrorOr<size_t> VirtualConsole::on_tty_write(UserOrKernelBuffer const& data, size_t size)
{
    SpinlockLocker global_lock(ConsoleManagement::the().tty_write_lock());
    auto result = data.read_buffered<512>(size, [&](ReadonlyBytes buffer) {
        for (const auto& byte : buffer)
            m_console_impl.on_input(byte);
        return buffer.size();
    });
    if (m_active)
        flush_dirty_lines();
    return result;
}

void VirtualConsole::set_active(bool active)
{
    VERIFY(ConsoleManagement::the().m_lock.is_locked());
    VERIFY(m_active != active);
    m_active = active;

    if (active) {
        HIDManagement::the().set_client(this);

        m_console_impl.m_need_full_flush = true;
        flush_dirty_lines();
    } else {
        HIDManagement::the().set_client(nullptr);
    }
}

void VirtualConsole::emit_char(char ch)
{
    // Since we are standards-compliant by not moving to column 1 on '\n', we have to add an extra carriage return to
    // do newlines properly. The `TTY` layer handles adding it.
    echo_with_processing(static_cast<u8>(ch));
}

void VirtualConsole::flush_dirty_lines()
{
    if (!m_active)
        return;
    VERIFY(GraphicsManagement::is_initialized());
    VERIFY(GraphicsManagement::the().console());
    for (u16 visual_row = 0; visual_row < rows(); ++visual_row) {
        auto& line = m_lines[visual_row];
        if (!line.dirty && !m_console_impl.m_need_full_flush)
            continue;
        for (size_t column = 0; column < columns(); ++column) {
            auto& cell = cell_at(column, visual_row);

            auto foreground_color = terminal_to_standard_color(cell.attribute.effective_foreground_color());
            if (has_flag(cell.attribute.flags, VT::Attribute::Flags::Bold))
                foreground_color = (Graphics::Console::Color)((u8)foreground_color | 0x08);
            GraphicsManagement::the().console()->write(column,
                visual_row,
                ((u8)cell.ch < 128 ? cell.ch : '?'),
                terminal_to_standard_color(cell.attribute.effective_background_color()),
                foreground_color);
        }
        line.dirty = false;
    }
    GraphicsManagement::the().console()->set_cursor(m_console_impl.cursor_column(), m_console_impl.cursor_row());
    m_console_impl.m_need_full_flush = false;
}

void VirtualConsole::beep()
{
    if (!kernel_command_line().is_pc_speaker_enabled())
        return;
    PCSpeaker::tone_on(440);
    IO::delay(10000);
    PCSpeaker::tone_off();
}

void VirtualConsole::set_window_title(StringView)
{
    // Do nothing.
}

void VirtualConsole::set_window_progress(int, int)
{
    // Do nothing.
}

void VirtualConsole::terminal_did_resize(u16 columns, u16 rows)
{
    // FIXME: Allocate more Region(s) or deallocate them if needed...
    dbgln("VC {}: Resized to {} x {}", index(), columns, rows);
}

void VirtualConsole::terminal_history_changed(int)
{
    // Do nothing, I guess?
}

void VirtualConsole::emit(u8 const* data, size_t size)
{
    for (size_t i = 0; i < size; i++)
        TTY::emit(data[i], true);
}

void VirtualConsole::set_cursor_style(VT::CursorStyle)
{
    // Do nothing
}

void VirtualConsole::echo(u8 ch)
{
    m_console_impl.on_input(ch);
    if (m_active)
        flush_dirty_lines();
}

VirtualConsole::Cell& VirtualConsole::cell_at(size_t x, size_t y)
{
    auto* ptr = (VirtualConsole::Cell*)(m_cells->vaddr().as_ptr());
    ptr += (y * columns()) + x;
    return *ptr;
}

void VirtualConsole::clear()
{
    auto* cell = (Cell*)m_cells->vaddr().as_ptr();
    for (size_t y = 0; y < rows(); y++) {
        m_lines[y].dirty = true;
        for (size_t x = 0; x < columns(); x++) {
            cell[x].clear();
        }
        cell += columns();
    }
    m_console_impl.set_cursor(0, 0);
}

void VirtualConsole::scroll_up(u16 region_top, u16 region_bottom, size_t count)
{
    VERIFY(region_top <= region_bottom);
    size_t region_size = region_bottom - region_top + 1;
    count = min(count, region_size);
    size_t line_bytes = (columns() * sizeof(Cell));
    memmove(m_cells->vaddr().offset(line_bytes * region_top).as_ptr(), m_cells->vaddr().offset(line_bytes * (region_top + count)).as_ptr(), line_bytes * (region_size - count));
    for (size_t i = 0; i < count; ++i)
        clear_line(region_bottom - i);
    for (u16 row = region_top; row <= region_bottom; ++row)
        m_lines[row].dirty = true;
}

void VirtualConsole::scroll_down(u16 region_top, u16 region_bottom, size_t count)
{
    VERIFY(region_top <= region_bottom);
    size_t region_size = region_bottom - region_top + 1;
    count = min(count, region_size);
    size_t line_bytes = (columns() * sizeof(Cell));
    memmove(m_cells->vaddr().offset(line_bytes * (region_top + count)).as_ptr(), m_cells->vaddr().offset(line_bytes * region_top).as_ptr(), line_bytes * (region_size - count));
    for (size_t i = 0; i < count; ++i)
        clear_line(region_top + i);
    for (u16 row = region_top; row <= region_bottom; ++row)
        m_lines[row].dirty = true;
}

void VirtualConsole::scroll_left(u16 row, u16 column, size_t count)
{
    VERIFY(row < rows());
    VERIFY(column < columns());
    count = min<size_t>(count, columns() - column);
    memmove(&cell_at(column, row), &cell_at(column + count, row), sizeof(Cell) * (columns() - column - count));
    for (size_t i = column + count; i < columns(); ++i)
        cell_at(i, row).clear();
    m_lines[row].dirty = true;
}

void VirtualConsole::scroll_right(u16 row, u16 column, size_t count)
{
    VERIFY(row < rows());
    VERIFY(column < columns());
    count = min<size_t>(count, columns() - column);
    memmove(&cell_at(column + count, row), &cell_at(column, row), sizeof(Cell) * (columns() - column - count));
    for (size_t i = column; i < column + count; ++i)
        cell_at(i, row).clear();
    m_lines[row].dirty = true;
}

void VirtualConsole::clear_in_line(u16 row, u16 first_column, u16 last_column)
{
    VERIFY(row < rows());
    VERIFY(first_column <= last_column);
    VERIFY(last_column < columns());
    m_lines[row].dirty = true;
    for (size_t x = first_column; x <= last_column; x++)
        cell_at(x, row).clear();
}

void VirtualConsole::put_character_at(unsigned row, unsigned column, u32 code_point, const VT::Attribute& attribute)
{
    VERIFY(row < rows());
    VERIFY(column < columns());
    auto& line = m_lines[row];
    auto& cell = cell_at(column, row);
    cell.attribute.foreground_color = attribute.foreground_color;
    cell.attribute.background_color = attribute.background_color;
    cell.attribute.flags = attribute.flags;
    if (code_point > 128)
        cell.ch = ' ';
    else
        cell.ch = code_point;
    cell.attribute.flags |= VT::Attribute::Flags::Touched;
    line.dirty = true;
    // FIXME: Maybe we should consider to change length after printing a special char in a column
    if (code_point <= 20)
        return;
    line.length = max<size_t>(line.length, column);
}

void VirtualConsole::invalidate_cursor(size_t row)
{
    m_lines[row].dirty = true;
}

}
