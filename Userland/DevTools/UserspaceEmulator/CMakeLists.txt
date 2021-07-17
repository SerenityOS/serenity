serenity_component(
    UserspaceEmulator
    RECOMMENDED
    TARGETS UserspaceEmulator
)

set(SOURCES
    Emulator.cpp
    Emulator_syscalls.cpp
    MallocTracer.cpp
    MmapRegion.cpp
    Range.cpp
    RangeAllocator.cpp
    Region.cpp
    SimpleRegion.cpp
    SoftCPU.cpp
    SoftFPU.cpp
    SoftMMU.cpp
    main.cpp
)

add_compile_options(-mmmx)

serenity_bin(UserspaceEmulator)
target_link_libraries(UserspaceEmulator LibX86 LibDebug LibCore LibPthread LibLine)
