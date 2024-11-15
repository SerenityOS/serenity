## Name

![Icon](/res/icons/16x16/app-brickgame.png) BrickGame

[Open](file:///bin/BrickGame)

## Synopsis

```**sh
$ BrickGame
```

## Description

BrickGame is a classic game. Pieces consisting of four small squares each fall from the top of the screen, being fixed in place once they land at the very bottom or on top of other pieces. Once a piece has landed, the next piece will start falling; you can preview this piece on the right side of the screen. By filling an entire row (or line) of squares, that row will be removed and the rows above will shift down. It is also possible to clear multiple lines at once.

You can control where and how a piece falls, by moving it left or right, making it move down faster, dropping it down instantly, or rotating it left and right. There are multiple control schemes available for these six basic actions; and the `space bar` always serves as the instant drop key.

-   `Left`: Move left, `Right`: Move right, `Up`: Rotate right, `Down`: Move down
-   `A`: Move left, `D`: Move right, `W`: Rotate right, `E`: Rotate left, `S`: Move down
-   `H`: Move left, `L`: Move right, `K`: Rotate right, `Z`: Rotate left

The `Escape` and `P` keys pause and unpause the game.

The seven pieces are commonly named "T", "J", "L", "O", "S", "Z", and "I". Note that while "J" and "L" as well as "S" and "Z" are mirrors of each other, they cannot be used interchangeably since you can only rotate pieces.

```
T:  x x x
      x

J:  x x x
        x

L:  x x x
    x

O:  x x
    x x

S:    x x
    x x

Z:  x x
      x x

I:  x x x x
```

The game will award you points for clearing lines, and the number depends both on the number of lines cleared at once, as well as your current level. The base points are 40, 100, 300, and 1200 for one, two, three, and four lines respectively, each multiplied by your current level number.

The game will transition to a higher level once you reach a certain score: 1000 points distance during the first ten levels, then 10,000 points distance. For example, you reach level 9 at 9000 points, level 10 at 10,000 points, but level 11 at 20,000 points. Levels determine how fast pieces drop down the screen, making it harder to play on higher levels. While the drop time is over half a second during level 1, it reduces to a tenth of a second at level 14.

A game ends once the entire screen is filled up and you cannot drop pieces anymore. BrickGame will permanently keep track of your high score.
