set(SOURCES
    main.cpp
)

serenity_bin(CrashDaemon)
target_link_libraries(CrashDaemon ${LIBRARY_LIBC} LibCompress LibCore LibCoreDump)
