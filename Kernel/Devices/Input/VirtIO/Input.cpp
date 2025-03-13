/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Span.h>
#include <AK/String.h>
#include <Kernel/Bus/VirtIO/Transport/PCIe/TransportLink.h>
#include <Kernel/Devices/Input/Management.h>
#include <Kernel/Devices/Input/VirtIO/EvDevDefinitions.h>
#include <Kernel/Devices/Input/VirtIO/Input.h>
#include <Kernel/Sections.h>

namespace Kernel::VirtIO {

struct VirtIOInputEvent {
    LittleEndian<u16> type;
    LittleEndian<u16> code;
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

// clang-format off
static constexpr auto unshifted_evdev_key_map = to_array<KeyCodeEntry const>({
    // 0x00-0x0f
    { Key_Invalid, 0xff },           { Key_Escape, 0x01 },            { Key_1, 0x02 },                 { Key_2, 0x03 },
    { Key_3, 0x04 },                 { Key_4, 0x05 },                 { Key_5, 0x06 },                 { Key_6, 0x07 },
    { Key_7, 0x08 },                 { Key_8, 0x09 },                 { Key_9, 0x0a },                 { Key_0, 0x0b },
    { Key_Minus, 0x0c },             { Key_Equal, 0x0d },             { Key_Backspace, 0x0e },         { Key_Tab, 0x0f },

    // 0x10-0x1f
    { Key_Q, 0x10 },                 { Key_W, 0x11 },                 { Key_E, 0x12 },                 { Key_R, 0x13 },
    { Key_T, 0x14 },                 { Key_Y, 0x15 },                 { Key_U, 0x16 },                 { Key_I, 0x17 },
    { Key_O, 0x18 },                 { Key_P, 0x19 },                 { Key_LeftBracket, 0x1a },       { Key_RightBracket, 0x1b },
    { Key_Return, 0x1c },            { Key_LeftControl, 0xff },       { Key_A, 0x1e },                 { Key_S, 0x1f },

    // 0x20-0x2f
    { Key_D, 0x20 },                 { Key_F, 0x21 },                 { Key_G, 0x22 },                 { Key_H, 0x23 },
    { Key_J, 0x24 },                 { Key_K, 0x25 },                 { Key_L, 0x26 },                 { Key_Semicolon, 0x27 },
    { Key_Apostrophe, 0x28 },        { Key_Backtick, 0x29 },          { Key_LeftShift, 0xff },         { Key_Backslash, 0x2b },
    { Key_Z, 0x2c },                 { Key_X, 0x2d },                 { Key_C, 0x2e },                 { Key_V, 0x2f },

    // 0x30-0x3f
    { Key_B, 0x30 },                 { Key_N, 0x31 },                 { Key_M, 0x32 },                 { Key_Comma, 0x33 },
    { Key_Period, 0x34 },            { Key_Slash, 0x35 },             { Key_RightShift, 0xff },        { Key_Asterisk, 0x37 },
    { Key_LeftAlt, 0xff },           { Key_Space, 0x39 },             { Key_CapsLock, 0xff },          { Key_F1, 0xff },
    { Key_F2, 0xff },                { Key_F3, 0xff },                { Key_F4, 0xff },                { Key_F5, 0xff },

    // 0x40-0x4f
    { Key_F6, 0xff },                { Key_F7, 0xff },                { Key_F8, 0xff },                { Key_F9, 0xff },
    { Key_F10, 0xff },               { Key_NumLock, 0xff },           { Key_ScrollLock, 0xff },        { Key_Home, 0xff },
    { Key_Up, 0xff },                { Key_PageUp, 0xff },            { Key_Minus, 0x4a },             { Key_Left, 0xff },
    { Key_Invalid, 0xff },           { Key_Right, 0xff },             { Key_Plus, 0x4e },              { Key_End, 0xff },

    // 0x50-0x5f
    { Key_Down, 0xff },              { Key_PageDown, 0xff },          { Key_Insert, 0xff },            { Key_Delete, 0xff },
    { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Backslash, 0x56 },         { Key_F11, 0xff },
    { Key_F12, 0xff },               { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },
    { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },

    // 0x60-0x6f
    // FIXME: Add Numpad "/" key to character map for key code 0x62
    { Key_Return, 0x1c },            { Key_RightControl, 0xff },      { Key_Slash, 0xff },             { Key_SysRq, 0xff },
    { Key_RightAlt, 0xff },          { Key_Invalid, 0xff },           { Key_Home, 0xff },              { Key_Up, 0xff },
    { Key_PageUp, 0xff },            { Key_Left, 0xff },              { Key_Right, 0xff },             { Key_End, 0xff },
    { Key_Down, 0xff },              { Key_PageDown, 0xff },          { Key_Insert, 0xff },            { Key_Delete, 0xff },

    // 0x70-0x7f
    { Key_Invalid, 0xff },           { Key_Mute, 0xff },              { Key_VolumeDown, 0xff },        { Key_VolumeUp, 0xff },
    { Key_Power, 0xff },             { Key_Equal, 0xff },             { Key_Invalid, 0xff },           { Key_PauseBreak, 0xff },
    { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },
    { Key_Invalid, 0xff },           { Key_LeftSuper, 0xff },         { Key_RightSuper, 0xff },        { Key_Menu, 0xff },

    // 0x80-0x8f
    { Key_Stop, 0xff },              { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },
    { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },
    { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },
    { Key_Calculator, 0xff },        { Key_Invalid, 0xff },           { Key_Sleep, 0xff },             { Key_Wake, 0xff },
});
// clang-format on

// clang-format off
static constexpr auto shifted_evdev_key_map = to_array<KeyCodeEntry const>({
    // 0x00-0x0f
    { Key_Invalid, 0xff },           { Key_Escape, 0x01 },            { Key_ExclamationPoint, 0x02 },  { Key_AtSign, 0x03 },
    { Key_Hashtag, 0x04 },           { Key_Dollar, 0x05 },            { Key_Percent, 0x06 },           { Key_Circumflex, 0x07 },
    { Key_Ampersand, 0x08 },         { Key_Asterisk, 0x09 },          { Key_LeftParen, 0x0a },         { Key_RightParen, 0x0b },
    { Key_Underscore, 0x0c },        { Key_Plus, 0x0d },              { Key_Backspace, 0x0e },         { Key_Tab, 0x0f },

    // 0x10-0x1f
    { Key_Q, 0x10 },                 { Key_W, 0x11 },                 { Key_E, 0x12 },                 { Key_R, 0x13 },
    { Key_T, 0x14 },                 { Key_Y, 0x15 },                 { Key_U, 0x16 },                 { Key_I, 0x17 },
    { Key_O, 0x18 },                 { Key_P, 0x19 },                 { Key_LeftBrace, 0x1a },         { Key_RightBrace, 0x1b },
    { Key_Return, 0x1c },            { Key_LeftControl, 0xff },       { Key_A, 0x1e },                 { Key_S, 0x1f },

    // 0x20-0x2f
    { Key_D, 0x20 },                 { Key_F, 0x21 },                 { Key_G, 0x22 },                 { Key_H, 0x23 },
    { Key_J, 0x24 },                 { Key_K, 0x25 },                 { Key_L, 0x26 },                 { Key_Colon, 0x27 },
    { Key_DoubleQuote, 0x28 },       { Key_Tilde, 0x29 },             { Key_LeftShift, 0xff },         { Key_Pipe, 0x2b },
    { Key_Z, 0x2c },                 { Key_X, 0x2d },                 { Key_C, 0x2e },                 { Key_V, 0x2f },

    // 0x30-0x3f
    { Key_B, 0x30 },                 { Key_N, 0x31 },                 { Key_M, 0x32 },                 { Key_LessThan, 0x33 },
    { Key_GreaterThan, 0x34 },       { Key_QuestionMark, 0x35 },      { Key_RightShift, 0xff },        { Key_Asterisk, 0x37 },
    { Key_LeftAlt, 0xff },           { Key_Space, 0x39 },             { Key_CapsLock, 0xff },          { Key_F1, 0xff },
    { Key_F2, 0xff },                { Key_F3, 0xff },                { Key_F4, 0xff },                { Key_F5, 0xff },

    // 0x40-0x4f
    { Key_F6, 0xff },                { Key_F7, 0xff },                { Key_F8, 0xff },                { Key_F9, 0xff },
    { Key_F10, 0xff },               { Key_NumLock, 0xff },           { Key_ScrollLock, 0xff },        { Key_Home, 0xff },
    { Key_Up, 0xff },                { Key_PageUp, 0xff },            { Key_Minus, 0x4a },             { Key_Left, 0xff },
    { Key_Invalid, 0xff },           { Key_Right, 0xff },             { Key_Plus, 0x4e },              { Key_End, 0xff },

    // 0x50-0x5f
    { Key_Down, 0xff },              { Key_PageDown, 0xff },          { Key_Insert, 0xff },            { Key_Delete, 0xff },
    { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Pipe, 0x56 },              { Key_F11, 0xff },
    { Key_F12, 0xff },               { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },
    { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },
});
// clang-format on

UNMAP_AFTER_INIT NonnullLockRefPtr<Input> Input::must_create_for_pci_instance(PCI::DeviceIdentifier const& device_identifier)
{
    auto pci_transport_link = MUST(PCIeTransportLink::create(device_identifier));
    return adopt_lock_ref_if_nonnull(new Input(move(pci_transport_link))).release_nonnull();
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
    case EV_SYN:
        switch (event.code) {
        case SYN_REPORT:
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
            dbgln_if(VIRTIO_DEBUG, "VirtIO::Input: Unknown EV_SYN event code: {:#x}", event.code);
            break;
        }
        break;

    case EV_KEY:
        switch (event.code) {
        case BTN_LEFT:
            if (event.value == 1)
                m_current_mouse_packet.buttons |= MousePacket::Button::LeftButton;
            else
                m_current_mouse_packet.buttons &= ~MousePacket::Button::LeftButton;
            break;

        case BTN_RIGHT:
            if (event.value == 1)
                m_current_mouse_packet.buttons |= MousePacket::Button::RightButton;
            else
                m_current_mouse_packet.buttons &= ~MousePacket::Button::RightButton;
            break;

        case BTN_MIDDLE:
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
            raw_key_event.scancode = event.code;

            switch (event.code) {
            case KEY_LEFTALT:
                m_keyboard_device->update_modifier(Mod_Alt, raw_key_event.is_press());
                break;

            case KEY_LEFTCTRL:
            case KEY_RIGHTCTRL:
                m_keyboard_device->update_modifier(Mod_Ctrl, raw_key_event.is_press());
                break;

            case KEY_LEFTSHIFT:
            case KEY_RIGHTSHIFT:
                m_keyboard_device->update_modifier(Mod_Shift, raw_key_event.is_press());
                break;

            case KEY_LEFTMETA:
            case KEY_RIGHTMETA:
                m_keyboard_device->update_modifier(Mod_Super, raw_key_event.is_press());
                break;

            case KEY_RIGHTALT:
                m_keyboard_device->update_modifier(Mod_AltGr, raw_key_event.is_press());
                break;

            default:
                break;
            }

            if ((event.code >= KEY_KP7 && event.code <= KEY_KPDOT)
                || event.code == KEY_KPASTERISK
                || event.code == KEY_KPENTER
                || event.code == KEY_KPEQUAL
                || event.code == KEY_KPSLASH) {
                m_keyboard_device->update_modifier(Mod_Keypad, true);
            }

            // The shift key only applies to small key codes, so only use the shifted key map if the event code is small enough.
            bool use_shifted_key_map = (m_keyboard_device->modifiers() & Mod_Shift) != 0 && event.code < shifted_evdev_key_map.size();

            auto key_map = use_shifted_key_map ? Span<KeyCodeEntry const>(shifted_evdev_key_map) : Span<KeyCodeEntry const>(unshifted_evdev_key_map);

            if (event.code >= key_map.size()) {
                dbgln_if(VIRTIO_DEBUG, "VirtIO::Input: Unknown EV_KEY event code: {:#x}", event.code);
                return;
            }

            raw_key_event.code_entry = key_map[event.code];

            KeyEvent key_event {
                .key = raw_key_event.code_entry.key_code,
                .map_entry_index = raw_key_event.code_entry.map_entry_index,
                .scancode = raw_key_event.scancode,
                .flags = raw_key_event.is_press() ? static_cast<u8>(Is_Press) : static_cast<u8>(0),
            };

            if (m_keyboard_device->num_lock_on() && (m_keyboard_device->modifiers() & Mod_Shift) == 0) {
                if (key_event.scancode >= KEY_KP7 && raw_key_event.scancode <= KEY_KPDOT) {
                    auto index = key_event.scancode - KEY_KP7;
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

    case EV_REL: {
        if (event.code == REL_X) {
            m_current_mouse_packet.is_relative = true;
            m_current_mouse_packet.x = static_cast<int>(event.value);
        } else if (event.code == REL_Y) {
            m_current_mouse_packet.is_relative = true;
            m_current_mouse_packet.y = -static_cast<int>(event.value);
        } else if (event.code == REL_WHEEL) {
            m_current_mouse_packet.z = -static_cast<int>(event.value);
        } else {
            dbgln_if(VIRTIO_DEBUG, "VirtIO::Input: Unknown EV_REL event code: {:#x}", event.code);
        }
        break;
    }

    case EV_ABS: {
        if (event.code == ABS_X) {
            m_current_mouse_packet.is_relative = false;
            m_current_mouse_packet.x = static_cast<int>((event.value - m_abs_min) * 0xffff / (m_abs_max - m_abs_min));
        } else if (event.code == ABS_Y) {
            m_current_mouse_packet.is_relative = false;
            m_current_mouse_packet.y = static_cast<int>((event.value - m_abs_min) * 0xffff / (m_abs_max - m_abs_min));
        } else {
            dbgln_if(VIRTIO_DEBUG, "VirtIO::Input: Unknown EV_ABS event code: {:#x}", event.code);
        }
        break;
    }

    default:
        dbgln_if(VIRTIO_DEBUG, "VirtIO::Input: Unknown event type: {:#x}", event.type);
        break;
    }
}

}
