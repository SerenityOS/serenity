serenity_component(
    CrashDaemon
    REQUIRED
    TARGETS CrashDaemon
)

set(SOURCES
    main.cpp
)

serenity_bin(CrashDaemon)
target_link_libraries(CrashDaemon LibCompress LibCore LibCoredump LibMain)
