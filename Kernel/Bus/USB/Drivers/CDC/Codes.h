/*
 * Copyright (c) 2025, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/StdLibExtras.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/UFixedBigInt.h>
#include <Kernel/Library/Assertions.h>

namespace Kernel::USB::CDC {

// https://www.usb.org/sites/default/files/CDC1.2_WMC1.1_012011_0.zip
#define ENUMERATE_SUBCLASS_CODES(X)                                                          \
    X(0x00, Reserved, "Reserved", "[None]")                                                  \
    X(0x01, DirectLineControlModel, "Direct Line Control Model", "[USBPSTN]")                \
    X(0x02, AbstractControlModel, "Abstract Control Model", "[USBPSTN]")                     \
    X(0x03, TelephoneControlModel, "Telephone Control Model", "[USBPSTN]")                   \
    X(0x04, MultiChannelControlModel, "Multi-Channel Control Model", "[USBISDN]")            \
    X(0x05, CAPIControlModel, "CAPI Control Model", "[USBISDN]")                             \
    X(0x06, EthernetNetworkingControlModel, "Ethernet Networking Control Model", "[USBECM]") \
    X(0x07, ATMNetworkingControlModel, "ATM Networking Control Model", "[USBATM]")           \
    X(0x08, WirelessHandsetControlModel, "Wireless Handset Control Model", "[USBWMC]")       \
    X(0x09, DeviceManagement, "Device Management", "[USBWMC]")                               \
    X(0x0A, MobileDirectLineModel, "Mobile Direct Line Model", "[USBWMC]")                   \
    X(0x0B, OBEX, "OBEX", "[USBWMC]")                                                        \
    X(0x0C, EthernetEmulationModel, "Ethernet Emulation Model", "[USBEEM]")                  \
    X(0x0D, NetworkControlModel, "Network Control Model", "[USBNCM]")                        \
    X(0x0E, MobileBroadbandInterfaceModel, "Mobile Broadband Interface Model", "[USBMBIM]")
// 0x0F-0x7F Reserved
// 0x80-0xFF Vendor Specific

enum class SubclassCode : u8 {
#define __ENUMERATE_SUBCLASS_CODE(code, name, _, __) name = code,
    ENUMERATE_SUBCLASS_CODES(__ENUMERATE_SUBCLASS_CODE)
#undef __ENUMERATE_SUBCLASS_CODE
};

inline StringView subclass_code_to_string(SubclassCode code)
{
    switch (to_underlying(code)) {
#define __ENUMERATE_SUBCLASS_CODE(code, name, str, standard) \
    case to_underlying(SubclassCode::name):                  \
        return standard " " str##sv;
        ENUMERATE_SUBCLASS_CODES(__ENUMERATE_SUBCLASS_CODE)
    case 0x0F ... 0x7F:
        return "Reserved"sv;
    case 0x80 ... 0xFF:
        return "Vendor Specific"sv;
    default:
        VERIFY_NOT_REACHED();
    }
#undef __ENUMERATE_SUBCLASS_CODE
}

#define ENUMERATE_COMMUNICATION_PROTOCOL_CODES(X)                           \
    X(0x00, NoSpecificProtocol, "No Specific Protocol", "[USB]")            \
    X(0x01, ATCommands_V250, "AT Commands", "ITU-T V.250")                  \
    X(0x02, ATCommands_PCCA101, "AT Commands", "PCCA-101")                  \
    X(0x03, ATCommands_PCCA101_AnnexO, "AT Commands", "PCCA-101 Annex O")   \
    X(0x04, ATCommands_GSM07_07, "AT Commands", "GSM 7.07")                 \
    X(0x05, ATCommands_3GPP27_007, "AT Commands", "3GPP 27.007")            \
    X(0x06, ATCommands_TIA_CDMA, "AT Commands (TIA CDMA)", "C-S0017-0")     \
    X(0x07, EthernetEmulationModel, "Ethernet Emulation Model", "[USBEEM]") \
    /* 08h-FDh Reserved (future use) */                                     \
    X(0xFE, ExternalProtocol, "External Protocol", "")                      \
    X(0xFF, Vendor, "Vendor-Specific", "[USB]")

enum class CommunicationProtocolCode : u8 {
#define __ENUMERATE_PROTOCOL_CODE(code, name, _, __) name = code,
    ENUMERATE_COMMUNICATION_PROTOCOL_CODES(__ENUMERATE_PROTOCOL_CODE)
#undef __ENUMERATE_PROTOCOL_CODE
};

inline StringView protocol_code_to_string(CommunicationProtocolCode code)
{
    switch (to_underlying(code)) {
#define __ENUMERATE_PROTOCOL_CODE(code, name, str, standard) \
    case to_underlying(CommunicationProtocolCode::name):     \
        return standard " " str##sv;
        ENUMERATE_COMMUNICATION_PROTOCOL_CODES(__ENUMERATE_PROTOCOL_CODE)
#undef __ENUMERATE_PROTOCOL_CODE
    default:
        return "Reserved"sv;
    }
}

#define ENUMERATE_DATA_PROTOCOL_CODES(X)                                                                                                            \
    X(0x00, NoSpecificProtocol, "No Specific Protocol", "[USB]")                                                                                    \
    X(0x01, NetworkTransferBlock, "Network Transfer Block", "[USBNCM]")                                                                             \
    /* 02h-2Fh Reserved (future use) */                                                                                                             \
    X(0x30, I430, "Physical interface protocol for ISDN BRI", "I.430")                                                                              \
    X(0x31, ISO3309, "HDLC", "[ISO/IEC 3309-1993]")                                                                                                 \
    X(0x32, Transparent, "Transparent", "[None]")                                                                                                   \
    /* 33h-4Fh Reserved (future use) */                                                                                                             \
    X(0x50, Q921M, "Management protocol for Q.921 data link protocol", "Q.921M")                                                                    \
    X(0x51, Q921, "Data link protocol for Q.931", "Q.921")                                                                                          \
    X(0x52, Q921TM, "TEI-multiplexor for Q.921 data link protocol", "Q.921TM")                                                                      \
    /* 53h-8Fh Reserved (future use) */                                                                                                             \
    X(0x90, V42bis, "V.42bis", "[Data compression procedures]")                                                                                     \
    X(0x91, Q931EuroISDN, "Euro-ISDN protocol control", "Q.931/Euro- ISDN")                                                                         \
    X(0x92, V120, "V.24 rate adaptation to ISDN", "V120")                                                                                           \
    X(0x93, CAPI20, "CAPI Commands", "CAPI2.0")                                                                                                     \
    /* 0x94 - 0xFC Reserved (future use) */                                                                                                         \
    X(0xFD, HostBasedDriver, "Host based driver", "[None]")                                                                                         \
    /* Note: This protocol code should only be used in messages between host and device to identify the host driver portion of a protocol stack. */ \
    X(0xFE, FunctionalDescriptor, "Protocol Unit Functional Descriptor Defined", "[USBCDC]")                                                        \
    /* The protocol(s) are described using a Protocol Unit Functional Descriptors on Communications Class Interface */                              \
    X(0xFF, Vendor, "Vendor-Specific", "[USB]")

enum class DataProtocolCode : u8 {
#define __ENUMERATE_PROTOCOL_CODE(code, name, _, __) name = code,
    ENUMERATE_DATA_PROTOCOL_CODES(__ENUMERATE_PROTOCOL_CODE)
#undef __ENUMERATE_PROTOCOL_CODE
};

inline StringView protocol_code_to_string(DataProtocolCode code)
{
    switch (to_underlying(code)) {
#define __ENUMERATE_PROTOCOL_CODE(code, name, str, standard) \
    case to_underlying(DataProtocolCode::name):              \
        return standard " " str##sv;
        ENUMERATE_DATA_PROTOCOL_CODES(__ENUMERATE_PROTOCOL_CODE)
#undef __ENUMERATE_PROTOCOL_CODE
    default:
        return "Reserved"sv;
    }
}

enum class ClassSpecificDescriptorCodes : u8 {
    CS_Interface = 0x24,
    CS_Endpoint = 0x25
};

#define ENUMERATE_CDC_CS_INTERFACE_DESCRIPTORS(X)                                                                                        \
    X(0x00, Header, "Header")                                                                                                            \
    X(0x01, CallManagement, "Call Management")                                                                                           \
    X(0x02, AbstractControlManagement, "Abstract Control Management")                                                                    \
    X(0x03, DirectLineManagement, "Direct Line Management")                                                                              \
    X(0x04, TelephoneRingerManagement, "Telephone Ringer Management")                                                                    \
    X(0x05, TelephoneCallAndLineStateReportingCapabilitiesDescriptor, "Telephone Call and Line State Reporting Capabilities Descriptor") \
    X(0x06, Union, "Union")                                                                                                              \
    X(0x07, CountrySelection, "Country Selection")                                                                                       \
    X(0x08, TelephoneOperationalModes, "Telephone Operational Modes")                                                                    \
    X(0x09, USBTerminal, "USB Terminal")                                                                                                 \
    X(0x0A, NetworkChannelTerminal, "Network Channel Terminal")                                                                          \
    X(0x0B, ProtocolUnit, "Protocol Unit")                                                                                               \
    X(0x0C, ExtensionUnit, "Extension Unit")                                                                                             \
    X(0x0D, MultiChannelManagement, "Multi-Channel Management")                                                                          \
    X(0x0E, CAPIControlManagement, "CAPI Control Management")                                                                            \
    X(0x0F, EthernetNetworking, "Ethernet Networking")                                                                                   \
    X(0x10, ATMNetworking, "ATM Networking")                                                                                             \
    X(0x11, WirelessHandsetControlModel, "Wireless Handset Control Model")                                                               \
    X(0x12, MobileDirectLineModel, "Mobile Direct Line Model")                                                                           \
    X(0x13, MDLMDetail, "MDLM Detail")                                                                                                   \
    X(0x14, DeviceManagementModel, "Device Management Model")                                                                            \
    X(0x15, OBEX, "OBEX")                                                                                                                \
    X(0x16, CommandSet, "Command Set")                                                                                                   \
    X(0x17, CommandSetDetail, "Command Set Detail")                                                                                      \
    X(0x18, TelephoneControlModel, "Telephone Control Model")                                                                            \
    X(0x19, OBEXServiceIdentifier, "OBEX Service Identifier")                                                                            \
    X(0x1A, NCM, "NCM")                                                                                                                  \
    X(0x1B, MBIM, "MBIM")                                                                                                                \
    X(0x1C, ExtendedNBIM, "Extended NBIM")                                                                                               \
    X(0x1D, NCMExtendedCapability, "NCM Extended Capabilities")                                                                          \
    X(0x1E, NCMExtendedFeature, "NCM Extended Feature")                                                                                  \
    /*0x1F-0x7F: Reserved (future use)*/                                                                                                 \
    /*0x80-0xFF: Vendor-specific*/

enum class ClassSpecificInterfaceDescriptorCodes : u8 {
#define __ENUMERATE_CS_INTERFACE_DESCRIPTOR(code, name, _) name = code,
    ENUMERATE_CDC_CS_INTERFACE_DESCRIPTORS(__ENUMERATE_CS_INTERFACE_DESCRIPTOR)
#undef __ENUMERATE_CS_INTERFACE_DESCRIPTOR
};

inline StringView class_specific_interface_descriptor_to_string(ClassSpecificInterfaceDescriptorCodes code)
{
    switch (to_underlying(code)) {
#define __ENUMERATE_CS_INTERFACE_DESCRIPTOR(code, name, str)         \
    case to_underlying(ClassSpecificInterfaceDescriptorCodes::name): \
        return str##sv;
        ENUMERATE_CDC_CS_INTERFACE_DESCRIPTORS(__ENUMERATE_CS_INTERFACE_DESCRIPTOR)
#undef __ENUMERATE_CS_INTERFACE_DESCRIPTOR
    default:
        return "Reserved"sv;
    }
}

}
