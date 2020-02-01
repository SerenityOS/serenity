set(SOURCES
    main.cpp
)

serenity_app(Terminal)
target_link_libraries(Terminal LibVT)
