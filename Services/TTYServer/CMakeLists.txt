set(SOURCES
    main.cpp
)

serenity_bin(TTYServer)
target_link_libraries(TTYServer LibC)
