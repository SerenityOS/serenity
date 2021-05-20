set(SOURCES
    Card.cpp
    CardStack.cpp
)

serenity_lib(LibCards cards)
target_link_libraries(LibCards LibC LibCore)
