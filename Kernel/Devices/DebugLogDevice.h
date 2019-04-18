#include <Kernel/Devices/CharacterDevice.h>

class DebugLogDevice final : public CharacterDevice {
public:
    DebugLogDevice();
    virtual ~DebugLogDevice() override;

    static DebugLogDevice& the();

private:
    // ^CharacterDevice
    virtual ssize_t read(Process&, byte*, ssize_t) override { return 0; }
    virtual ssize_t write(Process&, const byte*, ssize_t) override;
    virtual bool can_write(Process&) const override { return true; }
    virtual bool can_read(Process&) const override { return true; }
    virtual const char* class_name() const override { return "DebugLogDevice"; }
};
