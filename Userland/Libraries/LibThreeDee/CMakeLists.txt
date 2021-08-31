set(SOURCES
    Mesh.cpp
    WavefrontOBJLoader.cpp
)

serenity_lib(LibThreeDee threedee)
target_link_libraries(LibThreeDee LibGL)
