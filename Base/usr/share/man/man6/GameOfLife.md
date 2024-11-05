## Name

![Icon](/res/icons/16x16/app-gameoflife.png) GameOfLife

[Open](file:///bin/GameOfLife)

## Synopsis

```**sh
$ GameOfLife
```

## Description

GameOfLife is an implementation of John Conway's Game of Life.

The game is a cellular automaton where each cell is either dead (grey) or alive (yellow) and will change state in the next tick if any of the following rules are fulfilled:

-   An alive cell will die by underpopulation if it has fewer than two neighbors.
-   An alive cell will die by overpopulation if it has more than three neighbors.
-   A dead cell will come alive by reproduction if it has exactly three neighbors.

Otherwise, it will keep its old state.
