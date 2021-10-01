if (NOT EXISTS "${CMAKE_CURRENT_BINARY_DIR}/config.ini")
    configure_file("${CMAKE_SOURCE_DIR}/Kernel/config.ini.template" config.ini COPYONLY)
endif()
set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${CMAKE_CURRENT_BINARY_DIR}/config.ini")


# Default values for default setup
set(COMPILE_IDE ON)
set(COMPILE_AHCI ON)
set(COMPILE_RAMDISK ON)

set(COMPILE_E1000 ON)
set(COMPILE_E1000E ON)
set(COMPILE_RTL8139 ON)
set(COMPILE_RTL8168 ON)
set(COMPILE_NE2000 ON)

set(COMPILE_BOCHS_GRAPHICS ON)
set(COMPILE_INTEL_GRAPHICS ON)
set(COMPILE_VIRTIO_GRAPHICS ON)

file(READ "${CMAKE_CURRENT_BINARY_DIR}/config.ini" contents )
string(REGEX REPLACE ";" "\\\\;" contents "${contents}")
string(REGEX REPLACE "\n" ";" contents "${contents}")
foreach(line IN LISTS contents)
    if (line MATCHES "^$|^;|^#")
        continue()
    endif()
    string(REPLACE "=" ";" line_list ${line})
    list(GET line_list 0 var)
    list(GET line_list 1 value)
    if (NOT var STREQUAL "" AND NOT value STREQUAL "")
        string(TOUPPER "${value}" value)
        set("${var}" "${value}")
    endif()
endforeach()

message(VERBOSE "Compiling ATA IDE support was set to " "${COMPILE_IDE}" )

if (COMPILE_IDE OR COMPILE_AHCI)
    add_library(ATA_STORAGE_DEVICE OBJECT
    Storage/ATADevice.cpp
    Storage/ATADiskDevice.cpp
    Storage/ATAPIDiscDevice.cpp
    )
    target_link_libraries(Kernel PUBLIC ATA_STORAGE_DEVICE)
endif()  

if (COMPILE_IDE)
    add_library(ATA_IDE OBJECT
        Storage/IDEController.cpp
        Storage/IDEChannel.cpp
        Storage/BMIDEChannel.cpp
    )
    target_compile_definitions(Kernel PRIVATE COMPILE_IDE)
    target_link_libraries(Kernel PUBLIC ATA_IDE)
endif()

message(VERBOSE "Compiling SATA AHCI support was set to " "${COMPILE_AHCI}" )

if (COMPILE_AHCI)
    add_library(SATA_AHCI OBJECT
        Storage/AHCIController.cpp
        Storage/AHCIPort.cpp
        Storage/AHCIPortHandler.cpp
    )
    target_compile_definitions(Kernel PRIVATE COMPILE_AHCI)
    target_link_libraries(Kernel PUBLIC SATA_AHCI)
endif()

message(VERBOSE "Compiling Ramdisk support was set to " "${COMPILE_RAMDISK}" )

if (COMPILE_RAMDISK)
    add_library(RAMDISK_IO OBJECT
        Storage/RamdiskController.cpp
        Storage/RamdiskDevice.cpp
    )
    target_compile_definitions(Kernel PRIVATE COMPILE_RAMDISK)
    target_link_libraries(Kernel PUBLIC RAMDISK_IO)
endif()

message(VERBOSE "Compiling e1000e support was set to " "${COMPILE_E1000E}" )
if (COMPILE_E1000E)
    add_library(E1000E_NET OBJECT
        Net/E1000ENetworkAdapter.cpp

    )
    target_compile_definitions(Kernel PRIVATE COMPILE_E1000E)
    target_link_libraries(Kernel PUBLIC E1000E_NET)
endif()

message(VERBOSE "Compiling e1000 support was set to " "${COMPILE_E1000}" )
if (COMPILE_E1000)
    add_library(E1000_NET OBJECT
        Net/E1000NetworkAdapter.cpp
    )
    target_compile_definitions(Kernel PRIVATE COMPILE_E1000)
    target_link_libraries(Kernel PUBLIC E1000_NET)
endif()

message(VERBOSE "Compiling ne2000 support was set to " "${COMPILE_NE2000}" )
if (COMPILE_NE2000)
    add_library(NE2000_NET OBJECT
        Net/NE2000NetworkAdapter.cpp
    )
    target_compile_definitions(Kernel PRIVATE COMPILE_NE2000)
    target_link_libraries(Kernel PUBLIC NE2000_NET)
endif()

message(VERBOSE "Compiling rtl8139 support was set to " "${COMPILE_RTL8139}" )
if (COMPILE_RTL8139)
    add_library(RTL8139_NET OBJECT
        Net/RTL8139NetworkAdapter.cpp
    )
    target_compile_definitions(Kernel PRIVATE COMPILE_RTL8139)
    target_link_libraries(Kernel PUBLIC RTL8139_NET)
endif()

message(VERBOSE "Compiling rtl8168 support was set to " "${COMPILE_RTL8168}" )
if (COMPILE_RTL8168)
    add_library(RTL8168_NET OBJECT
        Net/RTL8168NetworkAdapter.cpp
    )
    target_compile_definitions(Kernel PRIVATE COMPILE_RTL8168)
    target_link_libraries(Kernel PUBLIC RTL8168_NET)
endif()

message(VERBOSE "Compiling bochs graphics support was set to " "${COMPILE_BOCHS_GRAPHICS}" )
if (COMPILE_BOCHS_GRAPHICS)
    add_library(BOCHS_GRAPHICS OBJECT
        Graphics/Bochs/GraphicsAdapter.cpp
    )
    target_compile_definitions(Kernel PRIVATE COMPILE_BOCHS_GRAPHICS)
    target_link_libraries(Kernel PUBLIC BOCHS_GRAPHICS)
endif()

message(VERBOSE "Compiling Intel graphics support was set to " "${COMPILE_INTEL_GRAPHICS}" )
if (COMPILE_INTEL_GRAPHICS)
    add_library(INTEL_GRAPHICS OBJECT
        Graphics/Intel/NativeGraphicsAdapter.cpp
    )
    target_compile_definitions(Kernel PRIVATE COMPILE_INTEL_GRAPHICS)
    target_link_libraries(Kernel PUBLIC INTEL_GRAPHICS)
endif()

message(VERBOSE "Compiling virtio graphics support was set to " "${COMPILE_VIRTIO_GRAPHICS}" )
if (COMPILE_VIRTIO_GRAPHICS)
    target_compile_definitions(Kernel PRIVATE COMPILE_VIRTIO_GRAPHICS)
    add_library(VIRTIO_GRAPHICS OBJECT
        Graphics/VirtIOGPU/FrameBufferDevice.cpp
        Graphics/VirtIOGPU/Console.cpp
        Graphics/VirtIOGPU/GPU.cpp
        Graphics/VirtIOGPU/GraphicsAdapter.cpp
    )
    target_link_libraries(Kernel PUBLIC VIRTIO_GRAPHICS)
endif()
