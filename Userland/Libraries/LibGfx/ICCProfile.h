/*
 * Copyright (c) 2022, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Format.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Span.h>

namespace Gfx::ICC {

// ICC v4, 7.2.4 Profile version field
class Version {
public:
    Version() = default;
    Version(u8 major, u8 minor_and_bugfix)
        : m_major_version(major)
        , m_minor_and_bugfix_version(minor_and_bugfix)
    {
    }

    u8 major_version() const { return m_major_version; }
    u8 minor_version() const { return m_minor_and_bugfix_version >> 4; }
    u8 bugfix_version() const { return m_minor_and_bugfix_version & 0xf; }

private:
    u8 m_major_version = 0;
    u8 m_minor_and_bugfix_version = 0;
};

// ICC v4, 7.2.5 Profile/device class field
enum class DeviceClass : u32 {
    InputDevce = 0x73636E72,    // 'scnr'
    DisplayDevice = 0x6D6E7472, // 'mntr'
    OutputDevice = 0x70727472,  // 'prtr'
    DeviceLink = 0x6C696E6B,    // 'link'
    ColorSpace = 0x73706163,    // 'spac'
    Abstract = 0x61627374,      // 'abst'
    NamedColor = 0x6E6D636C,    // 'nmcl'
};
char const* device_class_name(DeviceClass);

class Profile : public RefCounted<Profile> {
public:
    static ErrorOr<NonnullRefPtr<Profile>> try_load_from_externally_owned_memory(ReadonlyBytes bytes);

    Version version() const { return m_version; }
    DeviceClass device_class() const { return m_device_class; }

private:
    Version m_version;
    DeviceClass m_device_class;
};

}

namespace AK {
template<>
struct Formatter<Gfx::ICC::Version> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::ICC::Version const& version)
    {
        return Formatter<FormatString>::format(builder, "{}.{}.{}"sv, version.major_version(), version.minor_version(), version.bugfix_version());
    }
};
}
