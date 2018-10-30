#pragma once

#include <VirtualFileSystem/CharacterDevice.h>

class TTY : public CharacterDevice {
public:
    virtual ~TTY() override;

    virtual ssize_t read(byte*, size_t) override;
    virtual ssize_t write(const byte*, size_t) override;
    virtual bool hasDataAvailableForRead() const override;

protected:
    TTY(unsigned major, unsigned minor);
};

