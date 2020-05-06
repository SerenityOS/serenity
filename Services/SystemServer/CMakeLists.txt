set(SOURCES
    main.cpp
    Service.cpp
)

serenity_bin(SystemServer)
target_link_libraries(SystemServer LibCore)
