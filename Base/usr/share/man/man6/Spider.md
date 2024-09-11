## Name

![Icon](/res/icons/16x16/app-spider.png) Spider - The Spider card game

[Open](file:///bin/Spider)

## Synopsis

```**sh
$ Spider
```

## Description

Spider is an implementation of the classic solitaire card game with the same name, which became popular in digital form after its inclusion in Microsoft Windows XP.

In this version, the game is played with 104 cards, forming 8 complete (A to K) instances of either 2 different suits or a single suit, as chosen from the in-game menu.

After shuffling together the cards, the game starts, with a setup (_tableau_) of 54 cards over 10 vertical piles. Only the top card of each pile is upturned and accessible. The unused 50 cards (_stock_) are kept aside, covered, and dealt 10 at a time (1 per pile), when no more moves are possible on the current tableau.

Objective of the game is removing all the cards from the tableau and stock.

A single card can be moved from the top of one pile of the tableau to another, regardless of the suit, but only if its value is one less than the current top card on the destination pile: for example, a Queen of Diamonds can be moved onto a King of Spades; multiple cards can be moved at the same time only if they constitute a contiguous descending sequence of a same suit: for example, King of Diamonds / Queen of Diamonds / Jack of Diamonds.

A complete King-to-Ace sequence of the same suit can be removed from the tableau. Any card(s) can be moved on an empty pile, respecting the aforementioned rule for multiple cards.

When a new card reaches the top of the pile, it is turned face up. A card which is upturned is never turned face down, even if it is not at the top of the pile anymore.

When no more moves are available, 10 more cards are moved from the stock to the piles, 1 per pile, and turned face up.

The game can conclude in one of two different ways: (a) the game is won if all cards are removed from the tableau, and the stock is empty (b) the game is lost if no more progress can be achieved.

## Examples

     pile 1      pile 2    ...
       ♥ 3         ♦ K
       ♠ Q

Basic move: the ♠ Q can be shifted from pile 1 to pile 2, since (K - Q) is a valid sequence.

     pile 1      pile 2    ...
       ♥ 3         ♦ K
       ♠ Q
       ♠ J

Multiple-card move: same as before, except the Q and J on pile 1, constituting a valid in-suit sequence, can be moved together to pile 2 to produce (K - Q - J) sequence.

     pile 1      pile 2    ...
       ♥ 3         ♥ K
       ♠ K
       ♥ J

No move: no contiguous descending sequence can be produced here.
