#pragma once

#include <VirtualFileSystem/CharacterDevice.h>

class TTY : public CharacterDevice {
public:
    virtual ~TTY() override;

    virtual ssize_t read(byte*, size_t) override;
    virtual ssize_t write(const byte*, size_t) override;
    virtual bool hasDataAvailableForRead() const override;

    virtual String ttyName() const = 0;

protected:
    virtual bool isTTY() const final { return true; }

    TTY(unsigned major, unsigned minor);
    void emit(byte);

    virtual void onTTYWrite(byte) = 0;

private:
    Vector<byte> m_buffer;
};

