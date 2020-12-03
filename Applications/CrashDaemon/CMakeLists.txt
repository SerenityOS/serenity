set(SOURCES
    main.cpp
)

serenity_bin(CrashDaemon)
target_link_libraries(CrashDaemon LibC LibCore LibCoreDump LibDebug)
