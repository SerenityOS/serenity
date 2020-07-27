set(SOURCES
    Emulator.cpp
    MallocTracer.cpp
    MmapRegion.cpp
    SharedBufferRegion.cpp
    SimpleRegion.cpp
    SoftCPU.cpp
    SoftMMU.cpp
    main.cpp
)

serenity_bin(UserspaceEmulator)
target_link_libraries(UserspaceEmulator LibX86 LibDebug LibCore LibPthread)
