set(SOURCES
    Main.cpp
)

serenity_lib(LibMain main)
target_link_libraries(LibMain LibC)
