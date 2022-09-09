serenity_component(
    SingleAppDaemon
    REQUIRED
    TARGETS SingleAppDaemon
)

set(SOURCES
    main.cpp
)

serenity_bin(SingleAppDaemon)
target_link_libraries(SingleAppDaemon LibC LibCore LibMain)
