/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/FileSystem/SysFS.h>

namespace Kernel::PCI {

class PCIBusSysFSDirectory final : public SysFSDirectory {
public:
    static void initialize();

private:
    PCIBusSysFSDirectory();
};

class PCIDeviceSysFSDirectory final : public SysFSDirectory {
public:
    static NonnullRefPtr<PCIDeviceSysFSDirectory> create(SysFSDirectory const&, Address);
    Address const& address() const { return m_address; }

private:
    PCIDeviceSysFSDirectory(SysFSDirectory const&, Address);

    Address m_address;
};

class PCIDeviceAttributeSysFSComponent : public SysFSComponent {
public:
    static NonnullRefPtr<PCIDeviceAttributeSysFSComponent> create(String name, PCIDeviceSysFSDirectory const& device, size_t offset, size_t field_bytes_width);

    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, FileDescription*) const override;
    virtual ~PCIDeviceAttributeSysFSComponent() {};

protected:
    virtual OwnPtr<KBuffer> try_to_generate_buffer() const;
    PCIDeviceAttributeSysFSComponent(String name, PCIDeviceSysFSDirectory const& device, size_t offset, size_t field_bytes_width);
    NonnullRefPtr<PCIDeviceSysFSDirectory> m_device;
    size_t m_offset;
    size_t m_field_bytes_width;
};

class Access {
public:
    void enumerate(Function<void(Address, ID)>&) const;

    void enumerate_bus(int type, u8 bus, Function<void(Address, ID)>&, bool recursive);
    void enumerate_functions(int type, u8 bus, u8 device, u8 function, Function<void(Address, ID)>& callback, bool recursive);
    void enumerate_device(int type, u8 bus, u8 device, Function<void(Address, ID)>& callback, bool recursive);

    static Access& the();
    static bool is_initialized();
    virtual uint32_t segment_count() const = 0;
    virtual uint8_t segment_start_bus(u32 segment) const = 0;
    virtual uint8_t segment_end_bus(u32 segment) const = 0;

    virtual void write8_field(Address address, u32 field, u8 value) = 0;
    virtual void write16_field(Address address, u32 field, u16 value) = 0;
    virtual void write32_field(Address address, u32 field, u32 value) = 0;

    virtual u8 read8_field(Address address, u32 field) = 0;
    virtual u16 read16_field(Address address, u32 field) = 0;
    virtual u32 read32_field(Address address, u32 field) = 0;

    PhysicalID get_physical_id(Address address) const;

protected:
    virtual void enumerate_hardware(Function<void(Address, ID)>) = 0;

    u8 early_read8_field(Address address, u32 field);
    u16 early_read16_field(Address address, u32 field);
    u32 early_read32_field(Address address, u32 field);
    u16 early_read_type(Address address);

    Access();
    virtual ~Access() = default;

    Vector<PhysicalID> m_physical_ids;
    Bitmap m_enumerated_buses;
};

}
