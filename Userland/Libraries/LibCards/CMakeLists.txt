set(SOURCES
    Card.cpp
    CardGame.cpp
    CardPainter.cpp
    CardStack.cpp
)

serenity_lib(LibCards cards)
target_link_libraries(LibCards LibC LibCore LibConfig LibGUI)
