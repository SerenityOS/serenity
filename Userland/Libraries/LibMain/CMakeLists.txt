set(SOURCES
    Main.cpp
)

serenity_lib_static(LibMain main)
target_link_libraries(LibMain LibC)
