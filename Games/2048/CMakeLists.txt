set(SOURCES
    main.cpp
    2048.cpp
)

serenity_bin(2048)
target_link_libraries(2048 LibGUI)
