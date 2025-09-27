# Set_Card_Game
A Java implementation of the classic Set card game, built with Maven and designed with concurrency and synchronization. The project supports different play modes and demonstrates efficient use of multithreading for game logic.

# System Requirements
Java

Maven

# How to Run

compile with: mvn compile

run with: mvn exec:java

# Gameplay
The game contains a deck of 81 cards. Each card contains a drawing with four features (color,
number, shape, shading).

The game starts with drawn cards from the deck that are placed on a grid on the table.

The goal of each player is to find a combination of three cards from the cards on the table that
are said to make up a “legal set”.

A “legal set” is defined as a set of 3 cards, that for each one of the four features — color,
number, shape, and shading — the three cards must display that feature as either: (a) all the
same, or: (b) all different (in other words, for each feature the three cards must avoid having
two cards showing one version of the feature and the remaining card showing a different
version).

The game's active components contain the dealer and the players.

The players play together simultaneously on the table, trying to find a legal set of 3 cards. 
If the set is not legal, the player gets a penalty, freezing for a specified time period.

If the set is a legal set, the dealer replace the 3 cards with 3 new cards from the deck and give the successful player one point.
In this case the player also gets frozen although for a shorter time period.

In case no legal sets are currently available on the table, once every minute the dealer collects all the cards from the table, reshuffles the
deck and draws them anew.

The game will continue as long as there is a legal set to be found in the remaining cards (that are
either on table or in the deck). When there is no legal set left, the game will end and the player
with the most points will be declared as the winner!

Each player controls 12 unique keys on the keyboard as follows. The default keys are:

<img width="835" height="182" alt="image" src="https://github.com/user-attachments/assets/c7a784ad-fcac-48ea-bc18-8d3579c319e7" />

The keys layout is the same as the table's cards slots (3x4), and each key press dispatches the
respective player’s action, which is either to place or remove a token from the card in that slot.

The game supports 2 player types: human and non-human.

The input from the human players is taken from the physical keyboard as an input.

The non-human players are simulated by threads that continually produce random key presses.

