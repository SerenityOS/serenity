#pragma once

#include <VirtualFileSystem/CharacterDevice.h>
#include <VirtualFileSystem/UnixTypes.h>

class TTY : public CharacterDevice {
public:
    virtual ~TTY() override;

    virtual ssize_t read(byte*, size_t) override;
    virtual ssize_t write(const byte*, size_t) override;
    virtual bool hasDataAvailableForRead() const override;

    virtual String ttyName() const = 0;

    void set_pgid(pid_t pgid) { m_pgid = pgid; }
    pid_t pgid() const { return m_pgid; }

    const Unix::termios& termios() const { return m_termios; }
    void set_termios(const Unix::termios&);
    bool should_generate_signals() const { return m_termios.c_lflag & ISIG; }
    bool should_echo_input() const { return m_termios.c_lflag & ECHO; }

protected:
    virtual bool isTTY() const final override { return true; }

    TTY(unsigned major, unsigned minor);
    void emit(byte);

    virtual void onTTYWrite(const byte*, size_t) = 0;

    void interrupt();

private:
    Vector<byte> m_buffer;
    pid_t m_pgid { 0 };
    Unix::termios m_termios;
};

