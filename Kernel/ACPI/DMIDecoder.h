#pragma once

#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/VM/PhysicalAddress.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/VirtualAddress.h>

namespace SMBIOS {
struct [[gnu::packed]] LegacyEntryPoint32bit
{
    char legacy_sig[5];
    u8 checksum2;
    u16 smboios_table_length;
    u32 smbios_table_ptr;
    u16 smbios_tables_count;
    u8 smbios_bcd_revision;
};

struct [[gnu::packed]] EntryPoint32bit
{
    char sig[4];
    u8 checksum;
    u8 length;
    u8 major_version;
    u8 minor_version;
    u16 maximum_structure_size;
    u8 implementation_revision;
    char formatted_area[5];
    LegacyEntryPoint32bit legacy_structure;
};

struct [[gnu::packed]] EntryPoint64bit
{
    char sig[5];
    u8 checksum;
    u8 length;
    u8 major_version;
    u8 minor_version;
    u8 document_revision;
    u8 revision;
    u8 reserved;
    u32 table_maximum_size;
    u64 table_ptr;
};

struct [[gnu::packed]] TableHeader
{
    u8 type;
    u8 length;
    u16 handle;
};

enum class TableType {
    BIOSInfo = 0,
    SysInfo = 1,
    ModuleInfo = 2,
    SysEnclosure = 3,
    ProcessorInfo = 4,
    CacheInfo = 7,
    PortConnectorInfo = 8,
    SystemSlots = 9,
    OEMStrings = 11,
    SysConfigOptions = 12,
    BIOSLanguageInfo = 13,
    GroupAssociations = 14,
    SysEventLog = 15,
    PhysicalMemoryArray = 16,
    MemoryDevice = 17,
    MemoryErrorInfo32Bit = 18,
    MemoryArrayMappedAddress = 19,
    MemoryDeviceMappedAddress = 20,
    BuiltinPointingDevice = 21,
    PortableBattery = 22,
    SysReset = 23,
    HardwareSecurity = 24,
    SysPowerControls = 25,
    VoltageProbe = 26,
    CoolingDevice = 27,
    TemperatureProbe = 28,
    ElectricalCurrentProbe = 29,
    OutOfBandRemoteAccess = 30,
    SystemBootInfo = 32,
    MemoryErrorInfo64Bit = 33,
    ManagementDevice = 34,
    ManagementDeviceComponent = 35,
    ManagementDeviceThresholdData = 36,
    MemoryChannel = 37,
    IPMIDeviceInfo = 38,
    SysPowerSupply = 39,
    AdditionalInfo = 40,
    OnboardDevicesExtendedInfo = 41,
    ManagementControllerHostInterface = 42,
    TPMDevice = 43,
    ProcessorAdditionalInfo = 44,
    Inactive = 126,
    EndOfTable = 127
};

struct [[gnu::packed]] BIOSInfo
{ // Type 0
    TableHeader h;
    u8 bios_vendor_str_number;
    u8 bios_version_str_number;
    u16 bios_segment;
    u8 bios_release_date_str_number;
    u8 bios_rom_size;
    u64 bios_characteristics;
    u8 ext_bios_characteristics[];
};

enum class BIOSCharacteristics {
    Unknown = (1 << 2),
    NotSupported = (1 << 3),
    ISA_support = (1 << 4),
    MCA_support = (1 << 5),
    EISA_support = (1 << 6),
    PCI_support = (1 << 7),
    PCMCIA_support = (1 << 8),
    PnP_support = (1 << 9),
    APM_support = (1 << 10),
    UpgradeableBIOS = (1 << 11),
    Shadowing_BIOS = (1 << 12),
    VL_VESA_support = (1 << 13),
    ESCD_support = (1 << 14),
    CD_boot_support = (1 << 15),
    select_boot_support = (1 << 16),
    BIOS_ROM_socketed = (1 << 17),
    PCMCIA_boot_support = (1 << 18),
    EDD_spec_support = (1 << 19),
    floppy_nec98_1200k_support = (1 << 20),
    floppy_toshiba_1200k_support = (1 << 21),
    floppy_360k_support = (1 << 22),
    floppy_1200k_services_support = (1 << 23),
    floppy_720k_services_support = (1 << 24),
    floppy_2880k_services_support = (1 << 25),
    int5_print_screen_support = (1 << 26),
    int9_8042_keyboard_support = (1 << 27),
    int14_serial_support = (1 << 28),
    int17_printer_support = (1 << 29),
    int10_video_support = (1 << 30),
    nec_pc98 = (1 << 31)
};

struct [[gnu::packed]] ExtBIOSInfo
{
    u8 bios_major_release;
    u8 bios_minor_release;
    u8 embedded_controller_firmware_major_release;
    u8 embedded_controller_firmware_minor_release;
    u16 ext_bios_rom_size;
};

struct [[gnu::packed]] SysInfo
{ // Type 1
    TableHeader h;
    u8 manufacturer_str_number;
    u8 product_name_str_number;
    u8 version_str_number;
    u8 serial_number_str_number;
    u64 uuid[2];
    u8 wake_up_type;
    u8 sku_str_number;
    u8 family_str_number;
};

enum class WakeUpType {
    Reserved = 0,
    Other = 1,
    Unknown = 2,
    APM_TIMER = 3,
    MODEM_RING = 4,
    LAN_REMOTE = 5,
    POWER_SWTCH = 6,
    PCI_PME = 7,
    AC_RESTORE = 8,
};

struct [[gnu::packed]] ModuleInfo
{ // Type 2
    TableHeader h;
    u8 manufacturer_str_number;
    u8 product_name_str_number;
    u8 version_str_number;
    u8 serial_number_str_number;
    u8 asset_tag_str_number;
    u8 feature_flags;
    u8 chassis_location;
    u16 chassis_handle;
    u8 board_type;
    u8 contained_object_handles_count;
    u16 contained_object_handles[];
};

enum class BoardType {
    Unkown = 0x1,
    Other = 0x2,
    Server_Blade = 0x3,
    Connectivity_Switch = 0x4,
    System_Management_Module = 0x5,
    Processor_Module = 0x6,
    IO_Module = 0x7,
    Memory_Module = 0x8,
    Daughter_Board = 0x9,
    Motherboard = 0xA,
    Processor_Memory_Module = 0xB,
    Processor_IO_Module = 0xC,
    Interconnect_Board = 0xD,
};

struct [[gnu::packed]] SysEnclosure
{ // Type 3
    TableHeader h;
    u8 manufacturer_str_number;
    u8 type;
    u8 version_str_number;
    u8 serial_number_str_number;
    u8 asset_tag_str_number;
    u8 boot_up_state;
    u8 power_supply_state;
    u8 thermal_state;
    u8 security_status;
    u32 vendor_specific_info;
    u8 height;
    u8 power_cords_number;
    u8 contained_element_count;
    u8 contained_element_record_length;
};

struct [[gnu::packed]] ExtSysEnclosure
{
    u8 sku_str_number;
};

enum class SysEnclosureType {
    Other = 0x1,
    Unknown = 0x2,
    Desktop = 0x3,
    Low_Profile_Desktop = 0x4,
    Pizza_Box = 0x5,
    Mini_Tower = 0x6,
    Tower = 0x7,
    Portable = 0x8,
    Laptop = 0x9,
    Notebook = 0xA,
    Hand_Held = 0xB,
    Docking_Station = 0xC,
    AIO = 0xD,
    Sub_Notebook = 0xE,
    Space_Saving = 0xF,
    Lunch_Box = 0x10,
    Main_Server_Chassis = 0x11,
    Expansion_Chassis = 0x12,
    Sub_Chassis = 0x13,
    Bus_Expansion_Chassis = 0x14,
    Peripheral_Chassis = 0x15,
    RAID_Chassis = 0x16,
    Rack_MOunt_Chassis = 0x17,
    Sealed_case_PC = 0x18,
    Multi_System_Chasis = 0x19,
    Compact_PCI = 0x1A,
    Advanced_TCA = 0x1B,
    Blade = 0x1C,
    Blade_Enclosure = 0x1D,
    Tablet = 0x1E,
    Convertible = 0x1F,
    Detachable = 0x20,
    IoT_Gateway = 0x21,
    Embedded_PC = 0x22,
    Mini_PC = 0x23,
    Stick_PC = 0x24,
};

enum class SysEnclosureState {
    Other = 0x1,
    Unknown = 0x2,
    Safe = 0x3,
    Warning = 0x4,
    Critical = 0x5,
    Non_Recoverable = 0x6,
};

enum class SysEnclosureSecurityStatus {
    Other = 0x1,
    Unknown = 0x2,
    None = 0x3,
    External_Interface_Locked_Out = 0x4,
    External_Interface_Enabled = 0x5,
};

struct [[gnu::packed]] SysEnclosureContainedElement
{
    u8 type;
    u8 min_contained_element_count;
    u8 max_contained_element_count;
};

struct [[gnu::packed]] ProcessorInfo
{ // Type 4
    TableHeader h;
    u8 socket_designation_str_number;
    u8 processor_type;
    u8 processor_family;
    u8 processor_manufacturer_str_number;
    u64 processor_id;
    u8 processor_version_str_number;
    u8 voltage;
    u16 external_clock;
    u16 max_speed;
    u16 current_speed;
    u8 status;
    u8 processor_upgrade;
    u16 l1_cache_handle;
    u16 l2_cache_handle;
    u16 l3_cache_handle;
    u8 serial_number_str_number;
    u8 asset_tag_str_number;
    u8 part_number_str_number;
    u8 core_count;
    u8 core_enabled;
    u8 thread_count;
    u16 processor_characteristics;
    u16 processor_family2;
    u16 core_count2;
    u16 core_enabled2;
    u16 thread_count2;
};

enum class ProcessorType {
    Other = 0x1,
    Unknown = 0x2,
    Central_Processor = 0x3,
    Math_Processor = 0x4,
    DSP_Processor = 0x5,
    Video_Processor = 0x6,
};

enum class ProcessorUpgrade {
    Other = 0x1,
    Unknown = 0x2,
    Daughter_Board = 0x3,
    ZIF_Socket = 0x4,
    Replaceable_Piggy_Back = 0x5,
    None = 0x6,
    LIF_Sokcet = 0x7,
    Slot_1 = 0x8,
    Slot_2 = 0x9,
    Socket_370_pin = 0xA,
    Slot_A = 0xB,
    Slot_M = 0xC,
    Socket_423 = 0xD,
    Socket_A_462 = 0xE,
    Socket_478 = 0xF,
    Socket_754 = 0x10,
    Socket_940 = 0x11,
    Socket_939 = 0x12,
    Socket_mPGA604 = 0x13,
    Socket_LGA771 = 0x14,
    Socket_LGA775 = 0x15,
    Socket_S1 = 0x16,
    Socket_AM2 = 0x17,
    Socket_F_1207 = 0x18,
    Socket_LGA1366 = 0x19,
    Socket_G34 = 0x1A,
    Socket_AM3 = 0x1B,
    Socket_C32 = 0x1C,
    Socket_LGA1156 = 0x1D,
    Socket_LGA1567 = 0x1E,
    Socket_PGA988A = 0x1F,
    Socket_BGA1288 = 0x20,
    Socket_rPGA988B = 0x21,
    Socket_BGA1023 = 0x22,
    Socket_BGA1224 = 0x23,
    Socket_LGA1155 = 0x24,
    Socket_LGA1356 = 0x25,
    Socket_LGA2011 = 0x26,
    Socket_FS1 = 0x27,
    Socket_FS2 = 0x28,
    Socket_FM1 = 0x29,
    Socket_FM2 = 0x2A,
    Socket_LGA2011_3 = 0x2B,
    Socket_LGA1356_3 = 0x2C,
    Socket_LGA1150 = 0x2D,
    Socket_BGA1168 = 0x2E,
    Socket_BGA1234 = 0x2F,
    Socket_BGA1364 = 0x30,
    Socket_AM4 = 0x31,
    Socket_LGA1151 = 0x32,
    Socket_BGA1356 = 0x33,
    Socket_BGA1440 = 0x34,
    Socket_BGA1515 = 0x35,
    Socket_LGA3647_1 = 0x36,
    Socket_SP3 = 0x37,
    Socket_SP3r2 = 0x38,
    Socket_LGA2066 = 0x39,
    Socket_BGA1392 = 0x3A,
    Socket_BGA1510 = 0x3B,
    Socket_BGA1528 = 0x3C
};

struct [[gnu::packed]] CacheInfo
{ // Type 7
    TableHeader h;
    u8 socket_designation_str_number;
    u16 cache_config;
    u16 max_cache_size;
    u16 installed_size;
    u16 supported_sram_type;
    u16 current_sram_type;
    u8 cache_speed;
    u8 error_correction_type;
    u8 system_cache_type;
    u8 associativity;
    u32 max_cache_size2;
    u32 installed_size2;
};

struct [[gnu::packed]] PortConnectorInfo
{ // Type 8
    TableHeader h;
    u8 internal_reference_designator_str_number;
    u8 internal_connector_type;
    u8 external_reference_designator_str_number;
    u8 external_connector_type;
    u8 port_type;
};

enum class ConnectorType {
    None = 0x0,
    Centronics = 0x1,
    Mini_Centronics = 0x2,
    Proprietary = 0x3,
    DB_25_pin_male = 0x4,
    DB_25_pin_female = 0x5,
    DB_15_pin_male = 0x6,
    DB_15_pin_female = 0x7,
    DB_9_pin_male = 0x8,
    DB_9_pin_female = 0x9,
    RJ_11 = 0xA,
    RJ_45 = 0xB,
    MiniSCSI_50_pin = 0xC,
    MiniDIN = 0xD,
    MicroDIN = 0xE,
    PS2 = 0xF,
    Infrared = 0x10,
    HP_HIL = 0x11,
    AccessBus_USB = 0x12,
    SSA_SCSI = 0x13,
    Circular_DIN8_male = 0x14,
    Circular_DIN8_female = 0x15,
    OnBoard_IDE = 0x16,
    OnBoard_Floppy = 0x17,
    Dual_Inline_9pin = 0x18,
    Dual_Inline_25pin = 0x19,
    Dual_Inline_50pin = 0x1A,
    Dual_Inline_68pin = 0x1B,
    OnBoard_SoundInput_CDROM = 0x1C,
    Mini_Centronics_Type14 = 0x1D,
    Mini_Centronics_Type26 = 0x1E,
    Mini_Jack_Headphones = 0x1F,
    BNC = 0x20,
    Connector_1394 = 0x21,
    SAS_SATA_Plug_Receptacle = 0x22,
    USB_TypeC_Receptacle = 0x23,
    PC98 = 0xA0,
    PC98_Hireso = 0xA1,
    PC_H98 = 0xA2,
    PC98_Note = 0xA3,
    PC98_Full = 0xA4,
    Other = 0xFF
};

enum class PortType {
    None = 0x0,
    Parallel_Port_XT_AT_Compatible = 0x1,
    Parallel_Port_PS2 = 0x2,
    Parallel_Port_ECP = 0x3,
    Parallel_Port_EPP = 0x4,
    Parallel_Port_ECP_EPP = 0x5,
    Serial_Port_XT_AT_Compatible = 0x6,
    Serial_Port_16450_Compatible = 0x7,
    Serial_Port_16550_Compatible = 0x8,
    Serial_Port_16550A_Compatible = 0x9,
    SCSI_Port = 0xA,
    MIDI_Port = 0xB,
    Joy_Stick_Port = 0xC,
    Keyboard_Port = 0xD,
    Mouse_Port = 0xE,
    SSA_SCSI = 0xF,
    USB = 0x10,
    FireWire = 0x11,
    PCMCIA_Type1 = 0x12,
    PCMCIA_Type2 = 0x13,
    PCMCIA_Type3 = 0x14,
    Cardbus = 0x15,
    AccessBus_Port = 0x16,
    SCSI_2 = 0x17,
    SCSI_Wide = 0x18,
    PC98 = 0x19,
    PC98_Hireso = 0x1A,
    PC_H98 = 0x1B,
    Video_Port = 0x1C,
    Audio_Port = 0x1D,
    Modem_Port = 0x1E,
    Network_Port = 0x1F,
    SATA = 0x20,
    SAS = 0x21,
    MFDP = 0x22,
    Thunderbolt = 0x23,
    Intel_8251_Compatible = 0xA0,
    Intel_8251_FIFO_Compatible = 0xA1,
    Other = 0xFF
};

struct [[gnu::packed]] SystemSlotPeerGroup
{
    u16 segment_group_number;
    u8 bus_number;
    u8 device_function_number;
    u8 data_bus_width;
};

struct [[gnu::packed]] SystemSlots
{ // Type 9
    TableHeader h;
    u8 slot_designation_str_number;
    u8 slot_type;
    u8 slot_data_bus_width;
    u8 current_stage;
    u8 slot_length;
    u16 slot_id;
    u8 slot_characteristics_1;
    u8 slot_characteristics_2;
    u16 segment_group_number;
    u8 bus_number;
    u8 device_function_number;
    u8 data_bus_width;
    u8 peer_grouping_count;
    SystemSlotPeerGroup peer_groups[];
};

enum class SlotType {
    Other = 0x1,
    Unknown = 0x2,
    ISA = 0x3,
    MCA = 0x4,
    EISA = 0x5,
    PCI = 0x6,
    PCMCIA = 0x7,
    VL_VESA = 0x8,
    Proprietary = 0x9,
    Processor_Card_Slot = 0xA,
    Proprietary_Memory_Card_Slot = 0xB,
    IO_Riser_Card_Slot = 0xC,
    NuBus = 0xD,
    PCI_66MHZ_Capable = 0xE,
    AGP = 0xF,
    AGP_2X = 0x10,
    AGP_4X = 0x11,
    PCI_X = 0x12,
    AGP_8X = 0x13,
    M_Dot_2_Socket_1_DP = 0x14,
    M_Dot_2_Socket_1_SD = 0x15,
    M_Dot_2_Socket_2 = 0x16,
    M_Dot_2_Socket_3 = 0x17,
    MXM_Type1 = 0x18,
    MXM_Type2 = 0x19,
    MXM_Type3_Standard = 0x1A,
    MXM_Type3_HE = 0x1B,
    MXM_Type4 = 0x1C,
    MXM_3_Type_A = 0x1D,
    MXM_3_Type_B = 0x1E,
    PCI_Express_Gen2 = 0x1F,
    PCI_Express_Gen3 = 0x20,
    PCI_Express_Mini_52pin_Type1 = 0x21,
    PCI_Express_Mini_52pin_Type2 = 0x22,
    PCI_Express_Mini_76pin = 0x23,
    CXL_Flexbus_1_0 = 0x30,
    PC98_C20 = 0xA0,
    PC98_C24 = 0xA1,
    PC98_E = 0xA2,
    PC98_Local_Bus = 0xA3,
    PC98_Card = 0xA4,
    PCI_Express = 0xA5,
    PCI_Express_x1 = 0xA6,
    PCI_Express_x2 = 0xA7,
    PCI_Express_x4 = 0xA8,
    PCI_Express_x8 = 0xA9,
    PCI_Express_x16 = 0xAA,
    PCI_Express_Gen_2 = 0xAB,
    PCI_Express_Gen_2_x1 = 0xAC,
    PCI_Express_Gen_2_x2 = 0xAD,
    PCI_Express_Gen_2_x4 = 0xAE,
    PCI_Express_Gen_2_x8 = 0xAF,
    PCI_Express_Gen_2_x16 = 0xB0,
    PCI_Express_Gen_3 = 0xB1,
    PCI_Express_Gen_3_x1 = 0xB2,
    PCI_Express_Gen_3_x2 = 0xB3,
    PCI_Express_Gen_3_x4 = 0xB4,
    PCI_Express_Gen_3_x8 = 0xB5,
    PCI_Express_Gen_3_x16 = 0xB6,
    PCI_Express_Gen_4 = 0xB8,
    PCI_Express_Gen_4_x1 = 0xB9,
    PCI_Express_Gen_4_x2 = 0xBA,
    PCI_Express_Gen_4_x4 = 0xBB,
    PCI_Express_Gen_4_x8 = 0xBC,
    PCI_Express_Gen_4_x16 = 0xBD
};

enum class SlotDataBusWidth {
    Other = 0x1,
    Unknown = 0x2,
    _8_bit = 0x3,
    _16_bit = 0x4,
    _32_bit = 0x5,
    _64_bit = 0x6,
    _128_bit = 0x7,
    _1x_x1 = 0x8,
    _2x_x2 = 0x9,
    _4x_x4 = 0xA,
    _8x_x8 = 0xB,
    _12x_x12 = 0xC,
    _16x_x16 = 0xD,
    _32x_x32 = 0xE
};

enum class SlotCurrentUsage {
    Other = 0x1,
    Unknown = 0x2,
    Available = 0x3,
    In_Use = 0x4,
    Unavailable = 0x5
};

enum class SlotLength {
    Other = 0x1,
    Unknown = 0x2,
    Short_Length = 0x3,
    Long_Length = 0x4,
    _2_5_Drive_Form_Factor = 0x5,
    _3_5_Drive_Form_Factor = 0x6
};

enum class SlotCharacteristics1 {
    Unknown = (1 << 0),
    Provides_5volt = (1 << 1),
    Provides_3_3volt = (1 << 2),
    Shared_Slot = (1 << 3),
    Support_PC_Card_16 = (1 << 4),
    Support_CardBus = (1 << 5),
    Support_Zoom_Video = (1 << 6),
    Support_Modem_Ring_Resume = (1 << 7)
};

enum class SlotCharacteristics2 {
    Support_PCI_PME = (1 << 0),
    Support_Hot_Plug = (1 << 1),
    Support_SMBus = (1 << 2),
    Support_Bifurcation = (1 << 3),
};

struct [[gnu::packed]] OEMStrings
{ // Type 11
    TableHeader h;
    u8 strings_count;
};

struct [[gnu::packed]] SysConfigOptions
{ // Type 12
    TableHeader h;
    u8 strings_count;
};

struct [[gnu::packed]] BIOSLanguageInfo
{ // Type 13
    TableHeader h;
    u8 installable_langs_counts;
    u8 flags;
    u8 reserved[15];
    u8 current_lang_str_number; // String number (one-based) of the currently installed language
};

struct [[gnu::packed]] GroupAssociations
{ // Type 14
    TableHeader h;
    u8 group_name_str_number;
    u8 item_type;
    u16 item_handle;
};

struct [[gnu::packed]] SysEventLog
{ // Type 15
    TableHeader h;
    u16 log_area_length;
    u16 log_header_start_offset;
    u16 log_data_start_offset;
    u8 access_method;
    u8 log_status;
    u32 log_change_token;
    u32 access_method_address;
    u8 log_header_format;
    u8 supported_log_type_descriptors_count;
    u8 log_type_descriptor_length;
    u8 supported_event_log_type_descriptor_list[];
};

struct [[gnu::packed]] PhysicalMemoryArray
{ // Type 16
    TableHeader h;
    u8 location;
    u8 use;
    u8 memory_error_correction;
    u32 max_capacity;
    u16 memory_error_info_handle;
    u16 memory_devices_count;
    u64 ext_max_capacity;
};

enum class MemoryArrayLocation {
    Other = 0x1,
    Unknown = 0x2,
    Motherboard = 0x3,
    ISA_addon_card = 0x4,
    EISA_addon_card = 0x5,
    PCI_addon_card = 0x6,
    MCA_addon_card = 0x7,
    PCMCIA_addon_card = 0x8,
    Proprietary_addon_card = 0x9,
    NuBus = 0xA,
    PC98_C20_addon_card = 0xA0,
    PC98_C24_addon_card = 0xA1,
    PC98_E_addon_card = 0xA2,
    PC98_Local_Bus_addon_card = 0xA3,
    CXL_Flexbus_1_0_addon_card = 0xA4
};

enum class MemoryArrayUse {
    Other = 0x1,
    Unknown = 0x2,
    System_Memory = 0x3,
    Video_Memory = 0x4,
    Flash_Memory = 0x5,
    Non_Volatile_RAM = 0x6,
    Cache_Memory = 0x7
};

enum class MemoryArrayErrorCorrectionType {
    Other = 0x1,
    Unknown = 0x2,
    None = 0x3,
    Parity = 0x4,
    SingleBit_ECC = 0x5,
    MultiBit_ECC = 0x6,
    CRC = 0x7
};

struct [[gnu::packed]] MemoryDevice
{ // Type 17
    TableHeader h;
    u16 physical_memory_array_handle;
    u16 memory_error_info_handle;
    u16 total_width;
    u16 data_width;
    u16 size;
    u8 form_factor;
    u8 device_set;
    u8 device_locator_str_number;
    u8 bank_locator_str_number;
    u8 memory_type;
    u16 type_detail;
    u16 speed;
    u8 manufacturer_str_number;
    u8 serial_number_str_number;
    u8 asset_tag_str_number;
    u8 part_number_str_number;
    u8 attributes;
    u32 ext_size;
    u16 configured_memory_speed;
    u16 min_voltage;
    u16 max_voltage;
    u16 configured_voltage;
    u8 memory_technology;
    u16 memory_operating_mode_capability;
    u8 firmware_version_str_number;
    u16 module_manufacturer_id;
    u16 module_product_id;
    u16 memory_subsystem_controller_manufacturer_id;
    u16 memory_subsystem_controller_product_id;
    u64 non_volatile_size;
    u64 volatile_size;
    u64 cache_size;
    u64 logical_size;
    u32 ext_speed;
    u32 ext_configured_memory_speed;
};

enum class MemoryDeviceFormFactor {
    Other = 0x1,
    Unknown = 0x2,
    SIMM = 0x3,
    SIP = 0x4,
    Chip = 0x5,
    DIP = 0x6,
    ZIP = 0x7,
    ProprietaryCard = 0x8,
    DIMM = 0x9,
    TSOP = 0xA,
    Chips_Row = 0xB,
    RIMM = 0xC,
    SODIMM = 0xD,
    SRIMM = 0xE,
    FB_DIMM = 0xF,
    Die = 0x10
};

enum class MemoryDeviceType {
    Other = 0x1,
    Unknown = 0x2,
    DRAM = 0x3,
    EDRAM = 0x4,
    VRAM = 0x5,
    SRAM = 0x6,
    RAM = 0x7,
    ROM = 0x8,
    FLASH = 0x9,
    EEPROM = 0xA,
    FEPROM = 0xB,
    EPROM = 0xC,
    CDRAM = 0xD,
    _3DRAM = 0xE,
    SDRAM = 0xF,
    SGRAM = 0x10,
    RDRAM = 0x11,
    DDR = 0x12,
    DDR2 = 0x13,
    DDR2_FB_DIMM = 0x14,
    DDR3 = 0x18,
    FBD2 = 0x19,
    DDR4 = 0x1A,
    LPDDR = 0x1B,
    LPDDR2 = 0x1C,
    LPDDR3 = 0x1D,
    LPDDR4 = 0x1E,
    Logical_Non_Volatile_Device = 0x1F,
    HBM = 0x20,  // (High Bandwidth Memory)
    HBM2 = 0x21, // (High Bandwidth Memory Generation 2)
};

enum class MemoryDeviceTypeDetail {
    Other = (1 << 1),
    Unknown = (1 << 2),
    Fast_paged = (1 << 3),
    Static_Column = (1 << 4),
    Pseudo_Static = (1 << 5),
    RAMBUS = (1 << 6),
    Synchronous = (1 << 7),
    CMOS = (1 << 8),
    EDO = (1 << 9),
    Window_DRAM = (1 << 10),
    Cache_DRAM = (1 << 11),
    Non_volatile = (1 << 12),
    Registered_Buffered = (1 << 13),
    Unbuffered_Unregistered = (1 << 14),
    LRDIMM = (1 << 15)
};

enum class MemoryDeviceTechnology {
    Other = 0x1,
    Unknown = 0x2,
    DRAM = 0x3,
    NVDIMM_N = 0x4,
    NVDIMM_F = 0x5,
    NVDIMM_P = 0x6,
    Intel_Optane_DC_Persistent_Memory = 0x7
};

enum class MemoryDeviceOperatingModeCapability {
    Other = (1 << 1),
    Unknown = (1 << 2),
    Volatile_Memory = (1 << 3),
    Byte_accessible_persistent_memory = (1 << 4),
    Block_accessible_persistent_memory = (1 << 5),
};

struct MemoryErrorInfo32Bit { // Type 18
    TableHeader h;
    u8 error_type;
    u8 error_granularity;
    u8 error_operation;
    u32 vendor_syndrome;
    u32 memory_array_error_address;
    u32 device_error_address;
    u32 error_resolution;
};

enum class MemoryErrorType {
    Other = 0x1,
    Unknown = 0x2,
    OK = 0x3,
    Bad_read = 0x4,
    Parity_error = 0x5,
    SingleBit_error = 0x6,
    DoubleBit_error = 0x7,
    MultiBit_error = 0x8,
    Nibble_error = 0x9,
    Checksum_error = 0xA,
    CRC_error = 0xB,
    Corrected_SingleBit_error = 0xC,
    Corrected_error = 0xD,
    Uncorrectable_error = 0xE
};

enum class MemoryErrorGranularity {
    Other = 0x1,
    Unknown = 0x2,
    Device_level = 0x3,
    Memory_partition_level = 0x4
};

enum class MemoryErrorOperation {
    Other = 0x1,
    Unknown = 0x2,
    Read = 0x3,
    Write = 0x4,
    Partial_Write = 0x5
};

struct [[gnu::packed]] MemoryArrayMappedAddress
{ // Type 19
    TableHeader h;
    u32 starting_address;
    u32 ending_address;
    u16 memory_array_handle;
    u8 partition_width;
    u64 ext_starting_address;
    u64 ext_ending_address;
};

struct [[gnu::packed]] MemoryDeviceMappedAddress
{ // Type 20
    TableHeader h;
    u32 starting_address;
    u32 ending_address;
    u16 memory_device_handle;
    u16 memory_array_mapped_handle;
    u8 partition_row_position;
    u8 interleave_position;
    u8 interleaved_data_depth;
    u64 ext_starting_address;
    u64 ext_ending_address;
};

struct [[gnu::packed]] BuiltinPointingDevice
{ // Type 21
    TableHeader h;
    u8 type;
    u8 interface;
    u8 buttons_count;
};

enum class PointingDeviceType {
    Other = 0x1,
    Unknown = 0x2,
    Mouse = 0x3,
    Track_Ball = 0x4,
    Track_Point = 0x5,
    Glide_Point = 0x6,
    Touch_Pad = 0x7,
    Touch_Screen = 0x8,
    Optical_Sensor = 0x9
};

enum class PointingDeviceInterface {
    Other = 0x1,
    Unknown = 0x2,
    Serial = 0x3,
    PS2 = 0x4,
    Infrared = 0x5,
    HP_HIL = 0x6,
    Bus_mouse = 0x7,
    AppleDesktopBus = 0x8,
    Bus_mouse_DB9 = 0xA0,
    Bus_mouse_microDIN = 0xA1,
    USB = 0xA2
};

struct [[gnu::packed]] PortableBattery
{ // Type 22
    TableHeader h;
    u8 location_str_number;
    u8 manufacturer_str_number;
    u8 manufacture_date_str_number;
    u8 serial_number_str_number;
    u8 device_name_str_number;
    u8 device_chemistry;
    u16 design_capacity;
    u16 design_voltage;
    u8 sbds_version_number;
    u8 max_error_battery_data;
    u16 sbds_serial_number;
    u16 sbds_manufacture_date;
    u8 sbds_device_chemistry_str_number;
    u8 design_capacity_multiplier;
    u32 oem_specific;
};

enum class PortableBatteryChemistry {
    Other = 0x1,
    Unknown = 0x2,
    Lead_Acid = 0x3,
    Nickel_Cadmium = 0x4,
    Nickel_metal_hydride = 0x5,
    Lithium_ion = 0x6,
    Zinc_air = 0x7,
    Lithium_polymer = 0x8
};

struct [[gnu::packed]] SysReset
{ // Type 23
    TableHeader h;
    u8 capabilities;
    u16 reset_count;
    u16 reset_limit;
    u16 timer_interval;
    u16 timeout;
};

struct [[gnu::packed]] HardwareSecurity
{ // Type 24
    TableHeader h;
    u8 hardware_security_settings;
};

struct [[gnu::packed]] SysPowerControls
{ // Type 25
    TableHeader h;
    u8 next_scheduled_power_on_month;
    u8 next_scheduled_power_on_day_of_month;
    u8 next_scheduled_power_on_hour;
    u8 next_scheduled_power_on_minute;
    u8 next_scheduled_power_on_second;
};

struct [[gnu::packed]] VoltageProbe
{ // Type 26
    TableHeader h;
    u8 description_str_number;
    u8 location_and_status;
    u16 max_value;
    u16 min_value;
    u16 resolution;
    u16 tolerance;
    u16 accuracy;
    u32 oem_defined;
    u16 nominal_value;
};

struct [[gnu::packed]] CoolingDevice
{ // Type 27
    TableHeader h;
    u16 temperature_probe_handle;
    u8 device_type_and_status;
    u8 cooling_unit_group;
    u32 oem_defined;
    u16 nominal_speed;
    u8 description_str_number;
};

struct [[gnu::packed]] TemperatureProbe
{ // Type 28
    TableHeader h;
    u8 description_str_number;
    u8 location_and_status;
    u16 max_value;
    u16 min_value;
    u16 resolution;
    u16 tolerance;
    u16 accuracy;
    u32 oem_defined;
    u16 nominal_value;
};

struct [[gnu::packed]] ElectricalCurrentProbe
{ // Type 29
    TableHeader h;
    u8 description_str_number;
    u8 location_and_status;
    u16 max_value;
    u16 min_value;
    u16 resolution;
    u16 tolerance;
    u16 accuracy;
    u32 oem_defined;
    u16 nominal_value;
};

struct [[gnu::packed]] OutOfBandRemoteAccess
{ // Type 30
    TableHeader h;
    u8 manufacturer_name_str_number;
    u8 connections;
};

struct [[gnu::packed]] SystemBootInfo
{ // Type 32
    TableHeader h;
    u8 reserved[6];
    u8 boot_status[10];
};

struct [[gnu::packed]] MemoryErrorInfo64Bit
{ // Type 33
    TableHeader h;
    u8 error_type;
    u8 error_granularity;
    u8 error_operation;
    u32 vendor_syndrome;
    u64 memory_array_error_address;
    u64 device_error_address;
    u32 error_resolution;
};

struct [[gnu::packed]] ManagementDevice
{ // Type 34
    TableHeader h;
    u8 description_str_number;
    u8 type;
    u32 address;
    u8 address_type;
};

enum class ManagementDeviceType {
    Other = 0x1,
    Unknown = 0x2,
    LM75 = 0x3,
    LM78 = 0x4,
    LM79 = 0x5,
    LM80 = 0x6,
    LM81 = 0x7,
    ADM9240 = 0x8,
    DS1780 = 0x9,
    Maxim_1617 = 0xA,
    GL518SM = 0xB, // Genesys GL518SM
    W83781D = 0xC, // Winbond W83781D
    HT82H791 = 0xD // Holtek HT82H791
};

enum class ManagementDeviceAddressType {
    Other = 0x1,
    Unknown = 0x2,
    IO_Port = 0x3,
    Memory = 0x4,
    SMBus = 0x5
};

struct [[gnu::packed]] ManagementDeviceComponent
{ // Type 35
    TableHeader h;
    u8 description_str_number;
    u16 management_device_handle;
    u16 component_handle;
    u16 threshold_handle;
};

struct [[gnu::packed]] ManagementDeviceThresholdData
{ // Type 36
    TableHeader h;
    u16 lower_threshold_non_critical;
    u16 upper_threshold_non_critical;
    u16 lower_threshold_critical;
    u16 upper_threshold_critical;
    u16 lower_threshold_non_recoverable;
    u16 upper_threshold_non_recoverable;
};

struct [[gnu::packed]] MemoryDeviceDescriptor
{
    u8 device_load;
    u16 device_handle;
};

struct [[gnu::packed]] MemoryChannel
{ // Type 37
    TableHeader h;
    u8 channel_type;
    u8 memory_device_count;
    MemoryDeviceDescriptor memory_devices_descriptors[];
};

enum class MemroryChannelType {
    Other = 0x1,
    Unknown = 0x2,
    RamBus = 0x3,
    SyncLink = 0x4
};

struct [[gnu::packed]] IPMIDeviceInfo
{ // Type 38
    TableHeader h;
    u8 interface_type;
    u8 ipmi_spec_revision;
    u8 i2c_slave_address;
    u8 nv_storage_device_address;
    u64 base_address;
    u8 base_address_modifier;
    u8 interrupt_number;
};

enum class IPMIDeviceInfoBMCInterfaceType {
    Unknown = 0x1,
    KCS = 0x2,  // KCS: Keyboard Controller Style
    SMIC = 0x3, // SMIC: Server Management Interface Chip
    BT = 0x4,   // BT: Block Transfer
    SSIF = 0x5  // SSIF: SMBus System Interface
};

struct [[gnu::packed]] SysPowerSupply
{ // Type 39
    TableHeader h;
    u8 power_unit_group;
    u8 location_str_number;
    u8 device_name_str_number;
    u8 manufacturer_str_number;
    u8 serial_number_str_number;
    u8 asset_tag_number_str_number;
    u8 model_part_number_str_number;
    u8 revision_level_str_number;
    u16 max_power_capacity;
    u16 power_supply_characteristics;
    u16 input_voltage_probe_handle;
    u16 cooling_device_handle;
    u16 input_current_probe_handle;
};

struct [[gnu::packed]] AdditionalInfoEntry
{
    u8 entry_length;
    u16 referenced_handle;
    u8 referenced_offset;
    u8 string_number;
    u8 value[];
};

struct [[gnu::packed]] AdditionalInfo
{ // Type 40
    TableHeader h;
    u8 additional_info_entries_count;
    AdditionalInfoEntry entries[];
};

struct [[gnu::packed]] OnboardDevicesExtendedInfo
{ // Type 41
    TableHeader h;
    u8 reference_designation_str_number;
    u8 device_type;
    u8 device_type_instance;
    u16 segment_group_number;
    u8 bus_number;
    u8 device_function_number;
};

enum class OnboardDeviceType {
    Other = 0x1,
    Unknown = 0x2,
    Video = 0x3,
    SCSI_Controller = 0x4,
    Ethernet = 0x5,
    Token_Ring = 0x6,
    Sound = 0x7,
    PATA_Controller = 0x8,
    SATA_Controller = 0x9,
    SAS_Controller = 0xA
};

struct [[gnu::packed]] ManagementControllerHostInterface
{ // Type 42
    TableHeader h;
    u8 interface_type;
    u8 interface_type_specific_data_length;
    u8 interface_type_specific_data[];
};

struct [[gnu::packed]] ProtocolRecordData
{
    u8 protocol_type;
    u8 protocol_type_specific_data_length;
    u8 protocol_type_specific_data[];
};

struct [[gnu::packed]] ExtManagementControllerHostInterface
{ // Type 42 Ext
    u8 protocol_records_count;
    ProtocolRecordData protocol_records[];
};

enum class ManagementControllerHostInterfaceProtocolType {
    IPMI = 0x2,
    MCTP = 0x3,
    RedfishOverIP = 0x4
};

struct [[gnu::packed]] TPMDevice
{ // Type 43
    TableHeader h;
    char vendor_id[4];
    u8 major_spec_version;
    u8 minor_spec_version;
    u32 firmware_version_1;
    u32 firmware_version_2;
    u8 description_str_number;
    u64 characteristics;
    u32 oem_defined;
};

enum class TPMDeviceCharacteristics {
    Characteristics_not_supported = (1 << 2),
    Family_Configurable_1 = (1 << 3), // Family configurable via firmware update; for example, switching between TPM 1.2 and TPM 2.0.
    Family_Configurable_2 = (1 << 4), // Family configurable via platform software support, such as BIOS Setup; for example, switching between TPM 1.2 and TPM 2.0.
    Family_Configurable_3 = (1 << 5), // Family configurable via OEM proprietary mechanism; for example, switching between TPM 1.2 and TPM 2.0.
};

struct [[gnu::packed]] ProcessorSpecificBlock
{
    u8 block_length;
    u8 processor_type;
    u8 processor_specific_data[];
};

struct [[gnu::packed]] ProcessorAdditionalInfo
{ // Type 44
    TableHeader h;
    u16 referenced_handle;
    ProcessorSpecificBlock blocks[];
};

enum class ProcessorArchitectureType {
    IA32 = 0x1,
    x86_64 = 0x2,
    Itanium = 0x3,
    ARM32bit = 0x4,
    ARM64bit = 0x5,
    RISC_V_32bit = 0x6,
    RISC_V_64bit = 0x7,
    RISC_V_128bit = 0x8
};

struct [[gnu::packed]] Inactive
{ // Type 126
    TableHeader h;
};

struct [[gnu::packed]] EndOfTable
{ // Type 127
    TableHeader h;
};
}

class DMIDecoder {
public:
    static DMIDecoder& the();
    static void initialize();
    static void initialize_untrusted();
    Vector<SMBIOS::PhysicalMemoryArray*>& get_physical_memory_areas();
    bool is_reliable();
    u64 get_bios_characteristics();

private:
    void enumerate_smbios_tables();
    SMBIOS::TableHeader* get_next_physical_table(SMBIOS::TableHeader& p_table);
    SMBIOS::TableHeader* get_smbios_physical_table_by_handle(u16 handle);
    SMBIOS::TableHeader* get_smbios_physical_table_by_type(u8 table_type);
    char* get_smbios_string(SMBIOS::TableHeader& p_table, u8 string_number);
    size_t get_table_size(SMBIOS::TableHeader& table);

    explicit DMIDecoder(bool trusted);
    void initialize_parser();

    SMBIOS::EntryPoint32bit* find_entry32bit_point();
    SMBIOS::EntryPoint64bit* find_entry64bit_point();

    SMBIOS::EntryPoint32bit* m_entry32bit_point;
    SMBIOS::EntryPoint64bit* m_entry64bit_point;
    SMBIOS::TableHeader* m_structure_table;
    u32 m_structures_count;
    u32 m_table_length;
    bool m_use_64bit_entry;
    bool m_operable;
    bool m_untrusted;

    SinglyLinkedList<SMBIOS::TableHeader*> m_smbios_tables;
};
