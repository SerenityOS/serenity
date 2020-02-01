set(SOURCES
    main.cpp
)

serenity_app(TTYServer)
target_link_libraries(TTYServer LibC)
