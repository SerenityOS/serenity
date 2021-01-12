set(SOURCES
    BackgroundAction.cpp
    Thread.cpp
)

serenity_lib(LibThread thread)
target_link_libraries(LibThread LibC LibCore LibPthread)
