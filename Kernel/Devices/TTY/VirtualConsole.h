/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/Vector.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/Devices/GPU/Console/Console.h>
#include <Kernel/Devices/Generic/ConsoleDevice.h>
#include <Kernel/Devices/Input/Management.h>
#include <Kernel/Devices/TTY/TTY.h>
#include <LibVT/Attribute.h>
#include <LibVT/Color.h>
#include <LibVT/Position.h>
#include <LibVT/Terminal.h>

namespace Kernel {

class VirtualConsole;
// FIXME: This implementation has no knowledge about keeping terminal history...
class ConsoleImpl final : public VT::Terminal {
public:
    explicit ConsoleImpl(VirtualConsole&);

    virtual void set_size(u16 columns, u16 rows) override;

private:
    virtual void invalidate_cursor() override;
    virtual void clear() override;
    virtual void clear_history() override;

    virtual void scroll_up(u16 region_top, u16 region_bottom, size_t count) override;
    virtual void scroll_down(u16 region_top, u16 region_bottom, size_t count) override;
    virtual void scroll_left(u16 row, u16 column, size_t count) override;
    virtual void scroll_right(u16 row, u16 column, size_t count) override;
    virtual void put_character_at(unsigned row, unsigned column, u32 ch) override;
    virtual void clear_in_line(u16 row, u16 first_column, u16 last_column) override;
};

class VirtualConsole final : public TTY
    , public KeyboardClient
    , public VT::TerminalClient {
    friend class Device;
    friend class ConsoleImpl;
    friend class VT::Terminal;

public:
    struct Line {
        bool dirty;
        size_t length;
    };

    struct Cell {
        void clear()
        {
            ch = ' ';
            attribute.reset();
        }
        char ch;
        VT::Attribute attribute;
    };

public:
    static constexpr size_t s_max_virtual_consoles = 6;

    static void switch_to(unsigned index);
    static void initialize_consoles();
    static void switch_to_debug_console() { switch_to(1); }
    static void resolution_was_changed();
    static bool emit_char_on_debug_console(char ch);
    static NonnullRefPtr<VirtualConsole> create(size_t index);
    static NonnullRefPtr<VirtualConsole> create_with_preset_log(size_t index, CircularQueue<char, 16384> const&);

    virtual ~VirtualConsole() override;

    size_t index() const { return m_index; }
    bool is_graphical() const { return m_graphical; }
    void set_graphical(bool graphical);

    void emit_char(char);

private:
    explicit VirtualConsole(unsigned const index);
    // ^KeyboardClient
    virtual void on_key_pressed(KeyEvent) override;

    void refresh_after_resolution_change();

    // ^TTY
    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_name() const override;
    virtual ErrorOr<size_t> on_tty_write(UserOrKernelBuffer const&, size_t) override;
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual void echo(u8) override;

    // ^TerminalClient
    virtual void beep() override;
    virtual void set_window_title(StringView) override;
    virtual void set_window_progress(int, int) override;
    virtual void terminal_did_resize(u16 columns, u16 rows) override;
    virtual void terminal_history_changed(int) override;
    virtual void terminal_did_perform_possibly_partial_clear() override;
    virtual void emit(u8 const*, size_t) override;
    virtual void set_cursor_shape(VT::CursorShape) override;
    virtual void set_cursor_blinking(bool) override;

    // ^CharacterDevice
    virtual StringView class_name() const override { return "VirtualConsole"sv; }

    void set_active(bool);
    void flush_dirty_lines();

    unsigned m_index;
    bool m_active { false };
    bool m_graphical { false };

private:
    void initialize();

    void invalidate_cursor(size_t row);

    void clear();

    Cell& cell_at(size_t column, size_t row);

    using ParamVector = Vector<unsigned, 4>;

    void scroll_down(u16 region_top, u16 region_bottom, size_t count);
    void scroll_up(u16 region_top, u16 region_bottom, size_t count);
    void scroll_left(u16 row, u16 column, size_t count);
    void scroll_right(u16 row, u16 column, size_t count);
    void clear_line(size_t index)
    {
        clear_in_line(index, 0, m_console_impl.columns() - 1);
    }
    void clear_in_line(u16 row, u16 first_column, u16 last_column);
    void put_character_at(unsigned row, unsigned column, u32 ch, const VT::Attribute&);

    OwnPtr<Memory::Region> m_cells;
    Vector<VirtualConsole::Line> m_lines;
    ConsoleImpl m_console_impl;
};

}
