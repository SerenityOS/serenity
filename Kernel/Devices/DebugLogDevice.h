#include <Kernel/Devices/CharacterDevice.h>

class DebugLogDevice final : public CharacterDevice {
public:
    DebugLogDevice();
    virtual ~DebugLogDevice() override;

    static DebugLogDevice& the();

private:
    // ^CharacterDevice
    virtual ssize_t read(FileDescription&, byte*, ssize_t) override { return 0; }
    virtual ssize_t write(FileDescription&, const byte*, ssize_t) override;
    virtual bool can_write(FileDescription&) const override { return true; }
    virtual bool can_read(FileDescription&) const override { return true; }
    virtual const char* class_name() const override { return "DebugLogDevice"; }
};
