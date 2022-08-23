set(SOURCES
    Engine.cpp
)

serenity_lib(LibTextEditing textediting)
target_link_libraries(LibTextEditing LibCore)
