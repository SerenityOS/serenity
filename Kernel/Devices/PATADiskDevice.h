/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

//
// A Disk Device Connected to a PATA Channel
//
//
#pragma once

#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/IRQHandler.h>
#include <Kernel/Lock.h>

namespace Kernel {

class PATAChannel;

class PATADiskDevice final : public BlockDevice {
    AK_MAKE_ETERNAL
public:
    // Type of drive this IDEDiskDevice is on the ATA channel.
    //
    // Each PATA channel can contain only two devices, which (I think) are
    // jumper selectable on the drive itself by shorting two pins.
    enum class DriveType : u8 {
        Master,
        Slave
    };

public:
    static NonnullRefPtr<PATADiskDevice> create(PATAChannel&, DriveType, int major, int minor);
    virtual ~PATADiskDevice() override;

    // ^DiskDevice
    virtual bool read_blocks(unsigned index, u16 count, u8*) override;
    virtual bool write_blocks(unsigned index, u16 count, const u8*) override;

    void set_drive_geometry(u16, u16, u16);

    // ^BlockDevice
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override;
    virtual bool can_read(const FileDescription&) const override;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override;
    virtual bool can_write(const FileDescription&) const override;

protected:
    explicit PATADiskDevice(PATAChannel&, DriveType, int, int);

private:
    // ^DiskDevice
    virtual const char* class_name() const override;

    bool wait_for_irq();
    bool read_sectors_with_dma(u32 lba, u16 count, u8*);
    bool write_sectors_with_dma(u32 lba, u16 count, const u8*);
    bool read_sectors(u32 lba, u16 count, u8* buffer);
    bool write_sectors(u32 lba, u16 count, const u8* data);
    bool is_slave() const;

    Lock m_lock { "IDEDiskDevice" };
    u16 m_cylinders { 0 };
    u16 m_heads { 0 };
    u16 m_sectors_per_track { 0 };
    DriveType m_drive_type { DriveType::Master };

    PATAChannel& m_channel;
};

}
