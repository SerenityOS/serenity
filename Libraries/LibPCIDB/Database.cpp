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

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>

#include "Database.h"

namespace PCIDB {

RefPtr<Database> Database::open(const StringView& file_name)
{
    auto res = adopt(*new Database(file_name));
    if (res->init() != 0)
        return nullptr;
    return res;
}

const StringView Database::get_vendor(u16 vendor_id) const
{
    const auto& vendor = m_vendors.get(vendor_id);
    if (!vendor.has_value())
        return "";
    return vendor.value()->name;
}

const StringView Database::get_device(u16 vendor_id, u16 device_id) const
{
    const auto& vendor = m_vendors.get(vendor_id);
    if (!vendor.has_value())
        return "";
    const auto& device = vendor.value()->devices.get(device_id);
    if (!device.has_value())
        return "";
    return device.value()->name;
}

const StringView Database::get_subsystem(u16 vendor_id, u16 device_id, u16 subvendor_id, u16 subdevice_id) const
{
    const auto& vendor = m_vendors.get(vendor_id);
    if (!vendor.has_value())
        return "";
    const auto& device = vendor.value()->devices.get(device_id);
    if (!device.has_value())
        return "";
    const auto& subsystem = device.value()->subsystems.get((subvendor_id << 16) + subdevice_id);
    if (!subsystem.has_value())
        return "";
    return subsystem.value()->name;
}

const StringView Database::get_class(u8 class_id) const
{
    const auto& xclass = m_classes.get(class_id);
    if (!xclass.has_value())
        return "";
    return xclass.value()->name;
}

const StringView Database::get_subclass(u8 class_id, u8 subclass_id) const
{
    const auto& xclass = m_classes.get(class_id);
    if (!xclass.has_value())
        return "";
    const auto& subclass = xclass.value()->subclasses.get(subclass_id);
    if (!subclass.has_value())
        return "";
    return subclass.value()->name;
}

const StringView Database::get_programming_interface(u8 class_id, u8 subclass_id, u8 programming_interface_id) const
{
    const auto& xclass = m_classes.get(class_id);
    if (!xclass.has_value())
        return "";
    const auto& subclass = xclass.value()->subclasses.get(subclass_id);
    if (!subclass.has_value())
        return "";
    const auto& programming_interface = subclass.value()->programming_interfaces.get(programming_interface_id);
    if (!programming_interface.has_value())
        return "";
    return programming_interface.value()->name;
}

u8 parse_hex_digit(char digit)
{
    if (digit >= '0' && digit <= '9')
        return digit - '0';
    ASSERT(digit >= 'a' && digit <= 'f');
    return 10 + (digit - 'a');
}

template<typename T>
T parse_hex(StringView str, size_t count)
{
    ASSERT(str.length() >= count);

    T res = 0;
    for (size_t i = 0; i < count; i++)
        res = (res << 4) + parse_hex_digit(str[i]);

    return res;
}

int Database::init()
{
    if (m_ready)
        return 0;

    if (!m_file.is_valid())
        return -1;

    m_view = StringView((const char*)m_file.data(), m_file.size());

    ParseMode mode = ParseMode::UnknownMode;

    OwnPtr<Vendor> current_vendor = nullptr;
    OwnPtr<Device> current_device = nullptr;
    OwnPtr<Class> current_class = nullptr;
    OwnPtr<Subclass> current_subclass = nullptr;

    auto commit_device = [&]() {
        if (current_device && current_vendor) {
            auto id = current_device->id;
            current_vendor->devices.set(id, current_device.release_nonnull());
        }
    };

    auto commit_vendor = [&]() {
        commit_device();
        if (current_vendor) {
            auto id = current_vendor->id;
            m_vendors.set(id, current_vendor.release_nonnull());
        }
    };

    auto commit_subclass = [&]() {
        if (current_subclass && current_class) {
            auto id = current_subclass->id;
            current_class->subclasses.set(id, current_subclass.release_nonnull());
        }
    };

    auto commit_class = [&]() {
        commit_subclass();
        if (current_class) {
            auto id = current_class->id;
            m_classes.set(id, current_class.release_nonnull());
        }
    };

    auto commit_all = [&]() {
        commit_vendor();
        commit_class();
    };

    auto lines = m_view.split_view('\n');


    for (auto& line : lines) {
        if (line.length() < 2 || line[0] == '#')
            continue;

        if (line[0] == 'C') {
            mode = ParseMode::ClassMode;
            commit_all();
        } else if ((line[0] >= '0' && line[0] <= '9') || (line[0] >= 'a' && line[0] <= 'f')) {
            mode = ParseMode::VendorMode;
            commit_all();
        } else if (line[0] != '\t') {
            mode = ParseMode::UnknownMode;
            continue;
        }

        switch (mode) {
        case ParseMode::VendorMode:
            if (line[0] != '\t') {
                commit_vendor();
                current_vendor = make<Vendor>();
                current_vendor->id = parse_hex<u16>(line, 4);
                current_vendor->name = line.substring_view(6, line.length() - 6);
            } else if (line[0] == '\t' && line[1] != '\t') {
                commit_device();
                current_device = make<Device>();
                current_device->id = parse_hex<u16>(line.substring_view(1, line.length() - 1), 4);
                current_device->name = line.substring_view(7, line.length() - 7);
            } else if (line[0] == '\t' && line[1] == '\t') {
                auto subsystem = make<Subsystem>();
                subsystem->vendor_id = parse_hex<u16>(line.substring_view(2, 4), 4);
                subsystem->device_id = parse_hex<u16>(line.substring_view(7, 4), 4);
                subsystem->name = line.substring_view(13, line.length() - 13);
                current_device->subsystems.set((subsystem->vendor_id << 8) + subsystem->device_id, move(subsystem));
            }
            break;
        case ParseMode::ClassMode:
            if (line[0] != '\t') {
                commit_class();
                current_class = make<Class>();
                current_class->id = parse_hex<u8>(line.substring_view(2, 2), 2);
                current_class->name = line.substring_view(6, line.length() - 6);
            } else if (line[0] == '\t' && line[1] != '\t') {
                commit_subclass();
                current_subclass = make<Subclass>();
                current_subclass->id = parse_hex<u8>(line.substring_view(1, 2), 2);
                current_subclass->name = line.substring_view(5, line.length() - 5);
            } else if (line[0] == '\t' && line[1] == '\t') {
                auto programming_interface = make<ProgrammingInterface>();
                programming_interface->id = parse_hex<u8>(line.substring_view(2, 2), 2);
                programming_interface->name = line.substring_view(6, line.length() - 6);
                current_subclass->programming_interfaces.set(programming_interface->id, move(programming_interface));
            }
            break;
        default:
            break;
        }
    }

    commit_all();

    m_ready = true;


    return 0;
}

}
