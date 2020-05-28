/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#include <Kernel/PCI/Access.h>

namespace Kernel {
namespace PCI {

class IOAccess final : public PCI::Access {
public:
    static void initialize();

protected:
    IOAccess();

private:
    virtual void enumerate_hardware(Function<void(Address, ID)>) override;
    virtual const char* access_type() const override { return "IO-Access"; };
    virtual uint32_t segment_count() const override { return 1; };
    virtual void write8_field(Address address, u32, u8) override final;
    virtual void write16_field(Address address, u32, u16) override final;
    virtual void write32_field(Address address, u32, u32) override final;
    virtual u8 read8_field(Address address, u32) override;
    virtual u16 read16_field(Address address, u32) override;
    virtual u32 read32_field(Address address, u32) override;

    virtual uint8_t segment_start_bus(u32) const override { return 0x0; }
    virtual uint8_t segment_end_bus(u32) const override { return 0xFF; }
};

}
}
