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

#pragma once

#include <AK/HashMap.h>
#include <AK/MappedFile.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>

namespace PCIDB {

struct Subsystem {
    u16 vendor_id;
    u16 device_id;
    StringView name;
};

struct Device {
    u16 id;
    StringView name;
    HashMap<int, NonnullOwnPtr<Subsystem>> subsystems;
};

struct Vendor {
    u16 id;
    StringView name;
    HashMap<int, NonnullOwnPtr<Device>> devices;
};

struct ProgrammingInterface {
    u8 id { 0 };
    StringView name {};
};

struct Subclass {
    u8 id { 0 };
    StringView name {};
    HashMap<int, NonnullOwnPtr<ProgrammingInterface>> programming_interfaces;
};

struct Class {
    u8 id { 0 };
    StringView name {};
    HashMap<int, NonnullOwnPtr<Subclass>> subclasses;
};

class Database : public RefCounted<Database> {
public:
    static RefPtr<Database> open(const StringView& file_name);
    static RefPtr<Database> open() { return open("/res/pci.ids"); };

    const StringView get_vendor(u16 vendor_id) const;
    const StringView get_device(u16 vendor_id, u16 device_id) const;
    const StringView get_subsystem(u16 vendor_id, u16 device_id, u16 subvendor_id, u16 subdevice_id) const;
    const StringView get_class(u8 class_id) const;
    const StringView get_subclass(u8 class_id, u8 subclass_id) const;
    const StringView get_programming_interface(u8 class_id, u8 subclass_id, u8 programming_interface_id) const;

private:
    Database(const StringView& file_name)
        : m_file(file_name) {};

    int init();

    enum ParseMode {
        UnknownMode,
        VendorMode,
        ClassMode,
    };

    MappedFile m_file {};
    StringView m_view {};
    HashMap<int, NonnullOwnPtr<Vendor>> m_vendors;
    HashMap<int, NonnullOwnPtr<Class>> m_classes;
    bool m_ready { false };
};

}
