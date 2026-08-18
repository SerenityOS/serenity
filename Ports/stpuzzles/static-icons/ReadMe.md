# Puzzle icons

These are the `stpuzzles` launcher icons. Normally they get generated while
building the game, but that means running the puzzle binaries on the build
host - which we can't do when cross-compiling, so instead we commit them.

```sh
# Host with GTK3, ImageMagick and Perl installed.
cmake -S <puzzles-src> -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build --target icons

for base in build/icons/*-16d24.png; do
    name=${base%-16d24.png}; name=${name##*/}
    [ "$name" = nullgame ] && continue   # not a playable puzzle
    magick "$base" "${base%-16d24.png}-32d24.png" "$name.ico"
    mv "$name.ico" /path/to/static-icons
done
```
