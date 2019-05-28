#pragma once

#include "DoubleBuffer.h"
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/UnixTypes.h>

class Process;

class TTY : public CharacterDevice {
public:
    virtual ~TTY() override;

    virtual ssize_t read(FileDescriptor&, byte*, ssize_t) override;
    virtual ssize_t write(FileDescriptor&, const byte*, ssize_t) override;
    virtual bool can_read(FileDescriptor&) const override;
    virtual bool can_write(FileDescriptor&) const override;
    virtual int ioctl(FileDescriptor&, unsigned request, unsigned arg) override final;
    virtual String absolute_path(FileDescriptor&) const override { return tty_name(); }

    virtual String tty_name() const = 0;

    unsigned short rows() const { return m_rows; }
    unsigned short columns() const { return m_columns; }

    void set_pgid(pid_t pgid) { m_pgid = pgid; }
    pid_t pgid() const { return m_pgid; }

    void set_termios(const termios&);
    bool should_generate_signals() const { return m_termios.c_lflag & ISIG; }
    bool should_echo_input() const { return m_termios.c_lflag & ECHO; }
    bool in_canonical_mode() const { return m_termios.c_lflag & ICANON; }

    void set_default_termios();
    void hang_up();

protected:
    virtual ssize_t on_tty_write(const byte*, ssize_t) = 0;
    void set_size(unsigned short columns, unsigned short rows);

    TTY(unsigned major, unsigned minor);
    void emit(byte);

    void generate_signal(int signal);

private:
    // ^CharacterDevice
    virtual bool is_tty() const final override { return true; }

    DoubleBuffer m_buffer;
    pid_t m_pgid { 0 };
    termios m_termios;
    unsigned short m_rows { 0 };
    unsigned short m_columns { 0 };
};
