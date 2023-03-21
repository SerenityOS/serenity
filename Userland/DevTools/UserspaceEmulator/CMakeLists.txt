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
    SoftVPU.cpp
    main.cpp
)

add_compile_options(-mmmx -Wno-psabi -frounding-math)

serenity_bin(UserspaceEmulator)
target_link_libraries(UserspaceEmulator PRIVATE LibX86 LibDebug LibCore LibFileSystem LibLine LibSystem)
