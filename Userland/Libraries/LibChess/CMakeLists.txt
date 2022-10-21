set(SOURCES
    Chess.cpp
    UCICommand.cpp
    UCIEndpoint.cpp
)

serenity_lib(LibChess chess)
target_link_libraries(LibChess PRIVATE LibCore)
