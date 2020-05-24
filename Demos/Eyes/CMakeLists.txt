set(SOURCES
    main.cpp
    EyesWidget.cpp
)

serenity_bin(Eyes)
target_link_libraries(Eyes LibGUI LibGfx)
