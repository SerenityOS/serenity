#pragma once

#include <VirtualFileSystem/CharacterDevice.h>

class GUIEventDevice final : public CharacterDevice {
public:
    GUIEventDevice();
    virtual ~GUIEventDevice() override;

private:
    virtual bool has_data_available_for_reading(Process&) const override;
    virtual ssize_t read(byte* buffer, size_t bufferSize) override;
    virtual ssize_t write(const byte* buffer, size_t bufferSize) override;
};
