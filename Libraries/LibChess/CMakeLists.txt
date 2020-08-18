set(SOURCES
    Chess.cpp
)

serenity_lib(LibChess chess)
target_link_libraries(LibChess LibC)
