/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Span.h>
#include <Kernel/Bus/PCI/Driver.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Bus/VirtIO/Transport/PCIe/TransportLink.h>
#include <Kernel/Devices/Input/Management.h>
#include <Kernel/Devices/Input/VirtIO/EvDevDefinitions.h>
#include <Kernel/Devices/Input/VirtIO/Input.h>
#include <Kernel/Devices/Input/VirtIO/KeyboardKeymap.h>
#include <Kernel/Sections.h>

namespace Kernel::VirtIO {

struct VirtIOInputEvent {
    LittleEndian<EvDevEventType> type;
    LittleEndian<EvDevEventCode> code;
    LittleEndian<u32> value;
};
static_assert(AssertSize<VirtIOInputEvent, 8>());

struct VirtIOInputConfig {
    enum class Select : u8 {
        Unset = 0x00,
        IDName = 0x01,
        IDSerial = 0x02,
        IDDevIDs = 0x03,
        PropBits = 0x10,
        EvBits = 0x11,
        AbsInfo = 0x12,
    };

    struct AbsInfo {
        LittleEndian<u32> min;
        LittleEndian<u32> max;
        LittleEndian<u32> fuzz;
        LittleEndian<u32> flat;
        LittleEndian<u32> res;
    };
    static_assert(AssertSize<AbsInfo, 20>());

    struct DevIDs {
        LittleEndian<u16> bustype;
        LittleEndian<u16> vendor;
        LittleEndian<u16> product;
        LittleEndian<u16> version;
    };
    static_assert(AssertSize<DevIDs, 8>());

    Select select;
    u8 subsel;
    u8 size;
    u8 reserved[5];
    union {
        char string[128];
        u8 bitmap[128];
        AbsInfo abs;
        DevIDs ids;
    } u;
};
static_assert(AssertSize<VirtIOInputConfig, 136>());

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<Input>> Input::create_for_pci_instance(PCI::DeviceIdentifier const& device_identifier)
{
    auto pci_transport_link = TRY(PCIeTransportLink::create(device_identifier));
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Input(move(pci_transport_link))));
}

UNMAP_AFTER_INIT ErrorOr<void> Input::initialize_virtio_resources()
{
    TRY(Device::initialize_virtio_resources());
    auto const* cfg = TRY(transport_entity().get_config(VirtIO::ConfigurationType::Device));
    TRY(negotiate_features([&](auto) { return 0; }));

    transport_entity().config_write8(*cfg, offsetof(VirtIOInputConfig, subsel), 0);

    OwnPtr<KString> name;

    transport_entity().config_write8(*cfg, offsetof(VirtIOInputConfig, select), to_underlying(VirtIOInputConfig::Select::IDName));
    transport_entity().read_config_atomic([&]() {
        auto size = transport_entity().config_read8(*cfg, offsetof(VirtIOInputConfig, size));
        if (size == 0)
            return;

        VERIFY(size <= sizeof(VirtIOInputConfig::u.string));

        char* name_chars = nullptr;
        name = MUST(KString::try_create_uninitialized(size, name_chars));

        for (size_t i = 0; i < size; i++)
            name_chars[i] = static_cast<char>(transport_entity().config_read8(*cfg, offsetof(VirtIOInputConfig, u.string) + i));
    });

    if (name)
        dbgln("VirtIO::Input: Device name: {}", name);

    transport_entity().config_write8(*cfg, offsetof(VirtIOInputConfig, select), to_underlying(VirtIOInputConfig::Select::AbsInfo));
    transport_entity().read_config_atomic([&]() {
        auto size = transport_entity().config_read8(*cfg, offsetof(VirtIOInputConfig, size));
        if (size == 0)
            return;

        VERIFY(size == sizeof(VirtIOInputConfig::u.abs));

        m_abs_min = bit_cast<LittleEndian<u32>>(transport_entity().config_read32(*cfg, offsetof(VirtIOInputConfig, u.abs.min)));
        m_abs_max = bit_cast<LittleEndian<u32>>(transport_entity().config_read32(*cfg, offsetof(VirtIOInputConfig, u.abs.max)));
    });

    TRY(setup_queues(2));
    finish_init();

    auto& event_queue = get_queue(EVENTQ);
    SpinlockLocker event_queue_lock(event_queue.lock());

    m_event_buffer_region = TRY(MM.allocate_contiguous_kernel_region(TRY(Memory::page_round_up(event_queue.size() * sizeof(VirtIOInputEvent))), "VirtIO::Input eventq"sv, Memory::Region::Access::ReadWrite));

    QueueChain event_queue_chain(event_queue);

    for (size_t queue_idx = 0; queue_idx < event_queue.size(); ++queue_idx) {
        auto buffer_start = m_event_buffer_region->physical_page(0)->paddr().offset(queue_idx * sizeof(VirtIOInputEvent));
        auto did_add_buffer = event_queue_chain.add_buffer_to_chain(buffer_start, sizeof(VirtIOInputEvent), BufferType::DeviceWritable);
        VERIFY(did_add_buffer);
        supply_chain_and_notify(EVENTQ, event_queue_chain);
    }

    InputManagement::the().attach_standalone_input_device(*m_mouse_device);
    InputManagement::the().attach_standalone_input_device(*m_keyboard_device);

    return {};
}

UNMAP_AFTER_INIT Input::Input(NonnullOwnPtr<TransportEntity> transport_entity)
    : VirtIO::Device(move(transport_entity))
    , m_mouse_device(MouseDevice::try_to_initialize().release_value_but_fixme_should_propagate_errors())
    , m_keyboard_device(KeyboardDevice::try_to_initialize().release_value_but_fixme_should_propagate_errors())
{
}

ErrorOr<void> Input::handle_device_config_change()
{
    return {};
}

void Input::handle_queue_update(u16 queue_index)
{
    VERIFY(queue_index == EVENTQ);

    auto& queue = get_queue(EVENTQ);
    SpinlockLocker queue_lock(queue.lock());

    size_t used;
    QueueChain popped_chain = queue.pop_used_buffer_chain(used);
    while (!popped_chain.is_empty()) {
        popped_chain.for_each([this](PhysicalAddress paddr, size_t) {
            size_t offset = paddr.get() - m_event_buffer_region->physical_page(0)->paddr().get();
            auto const& event = *reinterpret_cast<VirtIOInputEvent const*>(m_event_buffer_region->vaddr().offset(offset).as_ptr());
            handle_event(event);
        });

        supply_chain_and_notify(EVENTQ, popped_chain);
        popped_chain = queue.pop_used_buffer_chain(used);
    }
}

void Input::handle_event(VirtIOInputEvent const& event)
{
    // TODO: Set lock key LEDs

    switch (event.type) {
    case EvDevEventType::Syn:
        switch (event.code) {
        case EvDevEventCode::SynReport:
            m_mouse_device->handle_mouse_packet_input_event(m_current_mouse_packet);

            // Don't reset the x/y values if the last event was an absolute event, as otherwise the mouse would jump to the top left corner on events other than mouse movement.
            if (m_current_mouse_packet.is_relative) {
                m_current_mouse_packet.x = 0;
                m_current_mouse_packet.y = 0;
            }

            m_current_mouse_packet.z = 0;
            m_current_mouse_packet.w = 0;

            break;

        default:
            dbgln_if(VIRTIO_DEBUG, "VirtIO::Input: Unknown EV_SYN event code: {:#x}", to_underlying(static_cast<EvDevEventCode>(event.code)));
            break;
        }
        break;

    case EvDevEventType::Key:
        switch (event.code) {
        case EvDevEventCode::ButtonLeft:
            if (event.value == 1)
                m_current_mouse_packet.buttons |= MousePacket::Button::LeftButton;
            else
                m_current_mouse_packet.buttons &= ~MousePacket::Button::LeftButton;
            break;

        case EvDevEventCode::ButtonRight:
            if (event.value == 1)
                m_current_mouse_packet.buttons |= MousePacket::Button::RightButton;
            else
                m_current_mouse_packet.buttons &= ~MousePacket::Button::RightButton;
            break;

        case EvDevEventCode::ButtonMiddle:
            if (event.value == 1)
                m_current_mouse_packet.buttons |= MousePacket::Button::MiddleButton;
            else
                m_current_mouse_packet.buttons &= ~MousePacket::Button::MiddleButton;
            break;

        default:
            // NOTE: We only supply entropy from the keyboard device, as each MouseDevice already has a EntropySource attached to it.
            m_entropy_source.add_random_event(event.code);

            m_keyboard_device->update_modifier(Mod_Keypad, false);

            RawKeyEvent raw_key_event;
            raw_key_event.is_press_down = event.value == 1;
            raw_key_event.scancode = to_underlying(static_cast<EvDevEventCode>(event.code));

            switch (event.code) {
            case EvDevEventCode::KeyLeftAlt:
                m_keyboard_device->update_modifier(Mod_Alt, raw_key_event.is_press());
                break;

            case EvDevEventCode::KeyLeftControl:
            case EvDevEventCode::KeyRightControl:
                m_keyboard_device->update_modifier(Mod_Ctrl, raw_key_event.is_press());
                break;

            case EvDevEventCode::KeyLeftShift:
            case EvDevEventCode::KeyRightShift:
                m_keyboard_device->update_modifier(Mod_Shift, raw_key_event.is_press());
                break;

            case EvDevEventCode::KeyLeftMeta:
            case EvDevEventCode::KeyRightMeta:
                m_keyboard_device->update_modifier(Mod_Super, raw_key_event.is_press());
                break;

            case EvDevEventCode::KeyRightAlt:
                m_keyboard_device->update_modifier(Mod_AltGr, raw_key_event.is_press());
                break;

            default:
                break;
            }

            if ((event.code >= EvDevEventCode::KeyKeypad7 && event.code <= EvDevEventCode::KeyKeypadDot)
                || event.code == EvDevEventCode::KeyKeypadAsterisk
                || event.code == EvDevEventCode::KeyKeypadEnter
                || event.code == EvDevEventCode::KeyKeypadEqual
                || event.code == EvDevEventCode::KeyKeypadSlash) {
                m_keyboard_device->update_modifier(Mod_Keypad, true);
            }

            // The shift key only applies to small key codes, so only use the shifted key map if the event code is small enough.
            bool use_shifted_key_map = (m_keyboard_device->modifiers() & Mod_Shift) != 0 && raw_key_event.scancode < shifted_evdev_key_map.size();

            auto key_map = use_shifted_key_map ? Span<KeyCodeEntry const>(shifted_evdev_key_map) : Span<KeyCodeEntry const>(unshifted_evdev_key_map);

            if (raw_key_event.scancode >= key_map.size()) {
                dbgln_if(VIRTIO_DEBUG, "VirtIO::Input: Unknown EV_KEY event code: {:#x}", to_underlying(static_cast<EvDevEventCode>(event.code)));
                return;
            }

            raw_key_event.code_entry = key_map[raw_key_event.scancode];

            KeyEvent key_event {
                .key = raw_key_event.code_entry.key_code,
                .map_entry_index = raw_key_event.code_entry.map_entry_index,
                .scancode = raw_key_event.scancode,
                .flags = raw_key_event.is_press() ? static_cast<u8>(Is_Press) : static_cast<u8>(0),
            };

            if (m_keyboard_device->num_lock_on() && (m_keyboard_device->modifiers() & Mod_Shift) == 0) {
                if (key_event.scancode >= to_underlying(EvDevEventCode::KeyKeypad7) && raw_key_event.scancode <= to_underlying(EvDevEventCode::KeyKeypadDot)) {
                    auto index = key_event.scancode - to_underlying(EvDevEventCode::KeyKeypad7);
                    static constexpr auto numpad_key_map = to_array<KeyCodeEntry>({
                        { Key_7, 0x08 },
                        { Key_8, 0x09 },
                        { Key_9, 0x0a },
                        { Key_Invalid, 0xff },
                        { Key_4, 0x05 },
                        { Key_5, 0x06 },
                        { Key_6, 0x07 },
                        { Key_Invalid, 0xff },
                        { Key_1, 0x02 },
                        { Key_2, 0x03 },
                        { Key_3, 0x04 },
                        { Key_0, 0x0b },
                        { Key_Period, 0x34 },
                    });

                    if (numpad_key_map[index].key_code != Key_Invalid) {
                        key_event.key = numpad_key_map[index].key_code;
                        key_event.map_entry_index = numpad_key_map[index].map_entry_index;
                    }
                }
            }

            m_keyboard_device->handle_input_event(key_event);
            break;
        }
        break;

    case EvDevEventType::Rel: {
        if (event.code == EvDevEventCode::RelX) {
            m_current_mouse_packet.is_relative = true;
            m_current_mouse_packet.x = static_cast<int>(event.value);
        } else if (event.code == EvDevEventCode::RelY) {
            m_current_mouse_packet.is_relative = true;
            m_current_mouse_packet.y = -static_cast<int>(event.value);
        } else if (event.code == EvDevEventCode::RelWheel) {
            m_current_mouse_packet.z = -static_cast<int>(event.value);
        } else {
            dbgln_if(VIRTIO_DEBUG, "VirtIO::Input: Unknown EV_REL event code: {:#x}", to_underlying(static_cast<EvDevEventCode>(event.code)));
        }
        break;
    }

    case EvDevEventType::Abs: {
        if (event.code == EvDevEventCode::AbsX) {
            m_current_mouse_packet.is_relative = false;
            m_current_mouse_packet.x = static_cast<int>((event.value - m_abs_min) * 0xffff / (m_abs_max - m_abs_min));
        } else if (event.code == EvDevEventCode::AbsY) {
            m_current_mouse_packet.is_relative = false;
            m_current_mouse_packet.y = static_cast<int>((event.value - m_abs_min) * 0xffff / (m_abs_max - m_abs_min));
        } else {
            dbgln_if(VIRTIO_DEBUG, "VirtIO::Input: Unknown EV_ABS event code: {:#x}", to_underlying(static_cast<EvDevEventCode>(event.code)));
        }
        break;
    }

    default:
        dbgln_if(VIRTIO_DEBUG, "VirtIO::Input: Unknown event type: {:#x}", to_underlying(static_cast<EvDevEventType>(event.type)));
        break;
    }
}

PCI_DRIVER(VirtIOInputDriver);

ErrorOr<void> VirtIOInputDriver::probe(PCI::DeviceIdentifier const& pci_device_identifier) const
{
    if (pci_device_identifier.hardware_id().vendor_id != PCI::VendorID::VirtIO
        || pci_device_identifier.hardware_id().device_id != PCI::DeviceID::VirtIOInput)
        return ENOTSUP;

    auto input = TRY(Input::create_for_pci_instance(pci_device_identifier));
    TRY(input->initialize_virtio_resources());

    (void)input.leak_ref();

    return {};
}

}
