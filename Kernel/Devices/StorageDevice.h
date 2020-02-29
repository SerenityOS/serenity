/*
* Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
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
#pragma once

#include <Kernel/Devices/BlockDevice.h>

namespace Kernel {

class StorageDevice : public BlockDevice {
public:
    StorageDevice(unsigned major, unsigned minor,
        size_t block_size = PAGE_SIZE);

    virtual ssize_t read(FileDescription&, u8*, ssize_t) override;
    virtual bool can_read(const FileDescription&) const override;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override;
    virtual bool can_write(const FileDescription&) const override;

    void set_drive_geometry(u16, u16, u16);
    u32 capacity() const;

private:
    u16 m_cylinders { 0 };
    u16 m_heads { 0 };
    u16 m_sectors_per_track { 0 };
};

}
