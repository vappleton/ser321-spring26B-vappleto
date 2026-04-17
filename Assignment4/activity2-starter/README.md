# Activity 2: Auction Game vs Bot Opponents

## Overview

In this activity, you will implement a multi-threaded auction game server where players compete against bot opponents to win fantasy items. Players bid on 5 items, trying to maximize their final score (remaining gold + value of items won).

You will learn about:
- **Thread pools** - Managing multiple concurrent clients
- **Protocol Buffers** - Efficient binary serialization
- **Basic synchronization** - Protecting shared data (leaderboard)

## Screencast link: https://youtu.be/dhKvQt9C6tk 

## How to compile and run the server and client
gradle build 

**Server:**  
gradle runServer --args="--grading"

**Client:**  
gradle runClient 



## Starter Code

### What's Provided

1. **AuctionServer.java** - Server skeleton with examples (YOU COMPLETE THIS)
2. **AuctionClient.java** - Complete menu-driven client
3. **Item.java** - Item data structure
4. **ItemLoader.java** - Loads items from file
5. **BotOpponent.java** - Bot bidding logic (you don't modify)
6. **LeaderboardManager.java** - Leaderboard persistence (make thread-safe)
7. **auction.proto** - Protocol Buffers definition
8. **items.txt** - Fantasy items for auction
9. **PROTO_PROTOCOL.md** - Complete protocol specification
10. **build.gradle** - Build configuration
11. **ProtocolTest.java** - 4 starter tests

## What was implemented
**In AuctionServer.java**  
[x] handleJoin() - Initializes a new game for the player, creates bots, loads items and sends the first item with player status.   
[x] handleBid() - Processes the player bids,validates input, generates the bot bids, determines the winner of a specific item, updates the game state and returns the auction results.    
[x] handleLeaderboard() - Retrieves the top 10 scores from the leaderboard and returns them in the response.
[x] handleGameOver() - Calculates  final scores for the player and the bots, determines the winner for the whole game, updates the global leaderboard and returns complete game results including the items won 


**In LeaderBoardManager.java**  
Made the leaderboard thread-safe by adding "synchronized" to the methods definitions  to ensure safe access accross the multiple client threads.   

**In ProtocolTest.java**   
Additional tests included are :  
[x] testLeaderboardRequest() - This test verifies leaderboard request ans returns the correct response and structure  
[x] testSkipBidRequest() - Tests the skip functionality using -1 bid to ensure the auction still completes.  
[x] testInvalidBidRequest() - ensures that invalid bids return ERROR and don't advance the game state  
[x] testValidBidRequest() - Confirms that valid bids are accepted and return BID_RESULT with the update state  
[x] testJoinRequest() - verifies that JOIN initializes the game correctly with player status and first item.   

### Running the Starter Code

**Build without running tests:**
```bash
gradle build -x test
```

**Start the server:**
```bash
gradle runServer
# Or with custom port:
gradle runServer --args="9000"
```

**Start the client (in a new terminal):**
```bash
gradle runClient
# Or with custom host/port:
gradle runClient -Phost=localhost -Pport=9000
```

## Game Mechanics
See given PDF.

## Protocol Details

See `PROTO_PROTOCOL.md` for complete specification.

### Request Types
- REGISTER - Set player name
- JOIN - Start game against bots
- BID - Submit bid on current item (-1 to skip)
- LEADERBOARD - Query top scores
- QUIT - Disconnect

### Response Types
- WELCOME - Greeting
- GAME_JOINED - Game started with first item
- BID_RESULT - Auction complete, show winner
- GAME_OVER - Final scores + leaderboard rank
- LEADERBOARD_RESPONSE - Top 10 scores
- ERROR - Validation error
- FAREWELL - Disconnect acknowledged

## Testing

### Protocol Tests
Start your server in grading mode and run the provided tests:
```bash
# Terminal 1: Start server
gradle runServer --args="--grading"

# Terminal 2: Run tests
gradle test
```

### Manual Testing

**Single Player Test:**
1. Start server: `gradle runServer`
2. Start client: `gradle runClient`
3. Register name, join game, bid on all items
4. Try skipping (-1) and bidding below reserve price
5. See final score and leaderboard position

**Multiple Concurrent Players Test:**
1. Start server
2. Start 3 clients in separate terminals
3. Each registers unique name and joins
4. All play independent games simultaneously
5. Check leaderboard to see all scores

**Leaderboard Persistence Test:**
1. Play a game and note your score
2. Stop server (Ctrl+C)
3. Restart server
4. Query leaderboard - your score should still be there

## Example Game Session

```
Server: Welcome to the Auction Game! Please set your name.
Client: Warrior
Server: Welcome, Warrior! You have 150 gold. Type 'join' to start playing!

Client: join
Server: Game started! You're playing against Alaric, Brynn, and Cedric.
        Item #1: Sword of Flames (weapon, 30-50 gold, reserve: 15)
Client: 40

Server: Auction complete!
        Winner: Warrior (bid: 40)
        All Bids: Warrior=40, Alaric=35, Brynn=32, Cedric=28
        Actual Value: 42 gold
        Item #2: Magic Shield (armor, 25-45 gold, reserve: 12)
Client: -1  (skip)

Server: Auction complete!
        Winner: Alaric (bid: 30)
        ...

... continues for all items ...

Server: Game over! Final results:
        Warrior: 165 total (70 gold + 95 items)
        Alaric: 110 total (40 gold + 70 items)
        Brynn: 105 total (55 gold + 50 items)
        Cedric: 100 total (60 gold + 40 items)
        Winner: Warrior
        Your Leaderboard Position: #3
```

## Tips
- Follow the protocol specification exactly
- Test with multiple concurrent clients
- Make sure leaderboard persists across server restarts (uses `scores.txt`)

## Design decisions or challenges 
One of the main design decisions was how to handle the game state for each player. Since each client runs its own thread, I kept
all game-related data inside a PLayerGameState object so that each player's game is completely independent. This made it easier 
to manage things like the items, gold, and bot opponents without having to worry about any interference between threads. 

A challenge I ran into was handling the bot opponents since the BotOpponent class was not meant to be modified. My workaround 
was tracking the bots' gold and items separately inside a PlayerGameState object. This allowed me to correctly calculate final scores for both 
the player and the bots without changing the given code. 

Another part that took some thought was the BID logic, as there are several steps involved ( validation, bids, determining the winner, updating state),
so I broke it down into smaller steps to keep it organized and made sure that invalid bids did not advance the game. 

Handling the protocol flow for when the  game is over was also a bit tricky, so I decided to have the main connection handle the transition to GAME_OVER
after the final item. 

Finally, I added "synchronized" to the methods in the leaderboard manager to ensure thread-safety. This prevents the race conditions when
the multiple clients are connected at the same time. 

## Known issues/Limitations 
One limitation to keep in mind is that the server must be running before executing the protocol tests, as the tests assume an 
active connection to the server. 

The server uses a thread pool (ExecutorService) to manage client connections efficiently, which works fine for this assignment but may not scale effciently with a large number of concurrent clients. 

Not a limitation per se, but the leaderboard currently stores all global scores, including multiple entries for the same player if they play multiple games, 
rather than just keeping the highest score per player. This is assumed to be acceptable since the requirement states that the leaderboard should return 
the top 10 scores from global leaderboard. It's also worth mentioning that the leaderboard persist across server restarts using a file ('scores.txt'), which is generated at runtime. 