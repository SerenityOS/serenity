#pragma once

#include <VirtualFileSystem/CharacterDevice.h>
#include <VirtualFileSystem/UnixTypes.h>

class Process;

class DoubleBuffer {
public:
    DoubleBuffer()
        : m_write_buffer(&m_buffer1)
        , m_read_buffer(&m_buffer2)
    {
    }

    ssize_t write(const byte*, size_t);
    ssize_t read(byte*, size_t);

    bool is_empty() const { return m_read_buffer_index >= m_read_buffer->size() && m_write_buffer->isEmpty(); }

private:
    void flip();

    Vector<byte>* m_write_buffer { nullptr };
    Vector<byte>* m_read_buffer { nullptr };
    Vector<byte> m_buffer1;
    Vector<byte> m_buffer2;
    size_t m_read_buffer_index { 0 };
};

class TTY : public CharacterDevice {
public:
    virtual ~TTY() override;

    virtual ssize_t read(byte*, size_t) override;
    virtual ssize_t write(const byte*, size_t) override;
    virtual bool hasDataAvailableForRead() const override;
    virtual int ioctl(Process&, unsigned request, unsigned arg) override final;

    virtual String ttyName() const = 0;

    unsigned short rows() const { return m_rows; }
    unsigned short columns() const { return m_columns; }

    void set_pgid(pid_t pgid) { m_pgid = pgid; }
    pid_t pgid() const { return m_pgid; }

    const Unix::termios& termios() const { return m_termios; }
    void set_termios(const Unix::termios&);
    bool should_generate_signals() const { return m_termios.c_lflag & ISIG; }
    bool should_echo_input() const { return m_termios.c_lflag & ECHO; }
    bool in_canonical_mode() const { return m_termios.c_lflag & ICANON; }

protected:
    virtual void onTTYWrite(const byte*, size_t) = 0;
    void set_size(unsigned short columns, unsigned short rows);

    TTY(unsigned major, unsigned minor);
    void emit(byte);

private:
    // ^CharacterDevice
    virtual bool isTTY() const final override { return true; }

    void generate_signal(int signal);

    DoubleBuffer m_buffer;
    pid_t m_pgid { 0 };
    Unix::termios m_termios;
    unsigned short m_rows { 0 };
    unsigned short m_columns { 0 };
};

