set(SOURCES
    main.cpp
    Service.cpp
)

serenity_app(SystemServer)
target_link_libraries(SystemServer LibCore)
