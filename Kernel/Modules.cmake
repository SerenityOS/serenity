if (NOT EXISTS "${CMAKE_CURRENT_BINARY_DIR}/config.ini")
    configure_file("${CMAKE_SOURCE_DIR}/Kernel/config.ini.template" config.ini COPYONLY)
endif()
set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${CMAKE_CURRENT_BINARY_DIR}/config.ini")


# Default values for default setup
set(COMPILE_IDE ON)
set(COMPILE_AHCI ON)
set(COMPILE_RAMDISK ON)

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
