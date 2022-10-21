set(SOURCES
    Card.cpp
    CardGame.cpp
    CardPainter.cpp
    CardStack.cpp
)

serenity_lib(LibCards cards)
target_link_libraries(LibCards PRIVATE LibCore LibConfig LibGfx LibGUI)
