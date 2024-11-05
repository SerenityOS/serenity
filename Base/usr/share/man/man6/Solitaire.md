## Name

![Icon](/res/icons/16x16/app-solitaire.png) Solitaire - The Solitaire card game

[Open](file:///bin/Solitaire)

## Synopsis

```**sh
$ Solitaire
```

## Description

Solitaire is an implementation of the classic solitaire card game Klondike, which became very popular in digital form during the 90s after being included in Microsoft Windows OS.

The game is played with a standard 52-card deck. Objective of the game is completing four distinct ordered sequences of cards (_foundation_), one per suit, from Ace to King.

After shuffling the deck, the game starts, with a setup (_tableau_) of seven vertical piles of cards, increasingly longer: the first pile is one card long, the seventh is seven cards long. Only the top card of each pile is upturned and accessible. The unused cards (_stock_) are kept aside, covered.

Cards can be moved from one pile of the tableau to another only by building contiguous descending sequences of alternate color: for example, King of Diamonds / Queen of Clubs, or 5 of Spades / 4 of Diamonds / 3 of Clubs. Within that rule, multiple cards can be moved at the same time. If one of the seven piles is empty, a new pile can be built there, starting with a King.

The top card from any pile can be detached and added to the foundation, if it is the next card in the sequence for that suit. The foundation sequence goes from the Ace up.

When a new card reaches the top of the pile, it is turned face up. A card which is upturned is never turned face down, even if it is not at the top of the pile anymore.

When no more moves are available, one (or, in a different variant, three) card(s) is taken from the stock and turned up. It can be added either to the tableau piles or to the foundation; if neither is possible, it goes to the waste.

The game can conclude in one of two different ways: (a) the game is won if all 52 cards are moved to the foundation (b) the game is lost if no more progress can be achieved towards the foundation.

## Examples

     foundation
       ♠ A    ♥ 2    ♦ 3    ♣ A


     pile 1      pile 2    ...
       ♥ 3         ♦ K
       ♠ Q

Basic move: the ♠ Q can be shifted from pile 1 to pile 2, since (♦ K - ♠ Q) is a valid descending sequence. Now the ♥ 3 can be taken from pile 1 and moved to the foundation, because it is the next card in the Hearts sequence after the Two.

     pile 1      pile 2    ...
       ♥ 3         ♦ K
       ♠ Q
       ♥ J

Multiple-card move: same as before, except the Q and J on pile 1 must be moved together to pile 2 to produce the sequence (♦ K - ♠ Q - ♥ J).

     pile 1      pile 2    ...
       ♥ 3         ♦ K
       ♠ Q
       ♠ J

No move: no alternating-color sequence can be produced here.
