//
// AdLib Sound Card Driver
// Author: Jesse Buhagiar [quaker762]
// Datasheet:   http://bochs.sourceforge.net/techspec/adlib_sb.txt (RIP no real documentation)
//              http://www.shipbrook.net/jeff/sb.html
//              http://www.vgmpf.com/Wiki/images/4/48/AdLib_-_Programming_Guide.pdf
//
// Email me at jooster669@gmail.com if you have any questions/suggestions :)
//
// Note: This card is NOT attached to any IRQ!
//
//
#pragma once

#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/IRQHandler.h>

class AdLib final : public CharacterDevice {
    AK_MAKE_ETERNAL;

public:
    explicit AdLib();
    virtual ~AdLib() override;

    static AdLib& the();

    // ^CharacterDevice
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override;
    virtual bool can_read(FileDescription&) const override;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override;
    virtual bool can_write(FileDescription&) const override;

private:
    // ^CharacterDevice
    virtual const char* class_name() const override { return "AdLib"; }

    void write_status(u8);
    void write_data(u8);
    void write_register(u8, u8);

    u8 read_status();

    bool detect();

    bool detected = false;
};
