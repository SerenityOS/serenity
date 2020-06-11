set(SOURCES
	main.cpp
)

serenity_bin(Threading)
target_link_libraries(Threading LibThread LibC LibCore)
