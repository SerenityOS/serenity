set(SOURCES
    Connection.cpp
    Decoder.cpp
    Encoder.cpp
)

serenity_lib(LibIPC ipc)
target_link_libraries(LibIPC PRIVATE LibCore)

if (NOT BUILD_LAGOM AND NOT BUILD_LAGOM_TOOLS)
    serenity_lib_static(LibIPCStatic ipc)
    target_link_libraries(LibIPCStatic PRIVATE LibCoreStatic)
endif()
