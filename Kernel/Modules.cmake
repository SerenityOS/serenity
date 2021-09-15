if (NOT EXISTS "${CMAKE_CURRENT_BINARY_DIR}/config.ini")
    configure_file("${CMAKE_SOURCE_DIR}/Kernel/config.ini.template" config.ini COPYONLY)
endif()
set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${CMAKE_CURRENT_BINARY_DIR}/config.ini")


# Default values for default setup
set(COMPILE_IDE ON)

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

if (COMPILE_IDE)
    add_library(ATA_IDE OBJECT
        Storage/IDEController.cpp
        Storage/IDEChannel.cpp
        Storage/BMIDEChannel.cpp
    )
    target_compile_definitions(Kernel PRIVATE COMPILE_IDE)
    target_link_libraries(Kernel PUBLIC ATA_IDE)
endif()
