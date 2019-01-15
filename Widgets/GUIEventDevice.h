#pragma once

#include <VirtualFileSystem/CharacterDevice.h>

class GUIEventDevice final : public CharacterDevice {
public:
    GUIEventDevice();
    virtual ~GUIEventDevice() override;

private:
    // ^CharacterDevice
    virtual ssize_t read(byte* buffer, size_t bufferSize) override;
    virtual ssize_t write(const byte* buffer, size_t bufferSize) override;
    virtual bool can_read(Process&) const override;
    virtual bool can_write(Process&) const override { return true; }
};
