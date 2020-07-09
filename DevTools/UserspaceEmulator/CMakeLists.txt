set(SOURCES
    SoftCPU.cpp
    SoftMMU.cpp
    Emulator.cpp
    main.cpp
)

serenity_bin(UserspaceEmulator)
target_link_libraries(UserspaceEmulator LibX86 LibCore)
