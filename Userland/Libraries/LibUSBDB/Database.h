/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibCore/MappedFile.h>

namespace USBDB {

struct Interface {
    u16 interface;
    StringView name;
};

struct Device {
    u16 id;
    StringView name;
    HashMap<int, NonnullOwnPtr<Interface>> interfaces;
};

struct Vendor {
    u16 id;
    StringView name;
    HashMap<int, NonnullOwnPtr<Device>> devices;
};

struct Protocol {
    u8 id { 0 };
    StringView name {};
};

struct Subclass {
    u8 id { 0 };
    StringView name {};
    HashMap<int, NonnullOwnPtr<Protocol>> protocols;
};

struct Class {
    u8 id { 0 };
    StringView name {};
    HashMap<int, NonnullOwnPtr<Subclass>> subclasses;
};

class Database : public RefCounted<Database> {
public:
    static RefPtr<Database> open(String const& filename);
    static RefPtr<Database> open() { return open("/res/usb.ids"); };

    const StringView get_vendor(u16 vendor_id) const;
    const StringView get_device(u16 vendor_id, u16 device_id) const;
    const StringView get_interface(u16 vendor_id, u16 device_id, u16 interface_id) const;
    const StringView get_class(u8 class_id) const;
    const StringView get_subclass(u8 class_id, u8 subclass_id) const;
    const StringView get_protocol(u8 class_id, u8 subclass_id, u8 protocol_id) const;

private:
    explicit Database(NonnullRefPtr<Core::MappedFile> file)
        : m_file(move(file))
    {
    }

    int init();

    enum ParseMode {
        UnknownMode,
        VendorMode,
        ClassMode,
    };

    NonnullRefPtr<Core::MappedFile> m_file;
    StringView m_view {};
    HashMap<int, NonnullOwnPtr<Vendor>> m_vendors;
    HashMap<int, NonnullOwnPtr<Class>> m_classes;
    bool m_ready { false };
};

}
