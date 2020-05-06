set(SOURCES
    Editor.cpp
)

serenity_lib(LibLine line)
target_link_libraries(LibLine LibC LibCore)
