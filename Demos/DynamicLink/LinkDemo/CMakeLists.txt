set(SOURCES
    main.cpp
)

serenity_bin(LinkDemo)
target_link_libraries(LinkDemo LibC)
