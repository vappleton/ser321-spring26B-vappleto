# Assignment 3 Task 2: Hangman Game Protocol

**Author:** Virginia Appleton
**Date:** 04/09/2026

---

## How to Run
You can use Gradle to run things, running with ./gradlew is of course also an option
**Server:**
Default
```bash
gradle Server
```

With arguments
```bash
gradle Server -Pport=8888
```

**Client:**
Default but running more quietly on Gradle
```bash
gradle Client --console=plain -q
```

With arguments
```bash
gradle Client -Phost=localhost -Pport=8888
```

---

## Video Demonstration

**Link:** https://youtu.be/RahLOAmQQro 

The video demonstrates:
- Starting server and client
- Complete game playthrough
- All implemented features

---

## Implemented Features Checklist

### Core Features (Required)
- [x] Set Player Name (provided as example)
- [x] Start New Game
- [x] Guess Letter
- [x] Game State
- [x] Win/Lose Detection
- [x] Graceful Quit

### Medium Features (Enhanced Gameplay)
- [x] Hint feature
- [x] Word Guessing
- [x] Guessed Letters Command
- [x] Give Up

### Advanced Features (Competition)
- [x] Scoring System
- [x] Leaderboard

**Note:** Mark [x] for completed features, [ ] for not implemented.

---

## Protocol Specification

### Overview
This protocol follows a request-response pattern using JSON messages over a socket connection. The Client send user actions as JSON requests,
and the server processes them and returns JSON responses. 

### General Request

```json
{
  "type" : "hangman",
  "action" : <String>
}
```
### General Response
```json
{
  "type": "hangman",
  "ok": <bool>,
  "message" : <String> // only present on error responses
}
```
---

### 1. Set Player Name

**Request:**
```json
{
    "type": "name",
    "name": "<string>"
}
```

**Success Response:**
```json
{
    "type": "name",
    "ok": true,
    "message": "Welcome <name>! ..."
}
```

**Error Response:**
```json
{
    "ok": false,
    "message": "Name cannot be empty"
}
```

---

### 2. Start New Game

**Request:**
```json
{
  "type" : "hangman",
  "action" : "start"
}
```

**Success Response:**
```json
{
  "ok" : true,
  "type" : "hangman",
  "word" : "_ _ _ _ _",
  "attemptsLeft" : <int>,
  "score" : 0,
  "misses" : 0
  
}
```
**Error Response:**
```json
{
  "ok" : false,
  "message" : "Game could not be started"
}
```
---

### 3. Guess Letter

**Request:**

```json
{
  "type": "hangman",
  "action" : "guess",
  "letter" : <String>
}
```
**Success Response:**
```json
{
  "type": "hangman",
  "ok": true,
  "word": "_ a _ _ _",
  "correct": true,
  "attemptsLeft": <int>,
  "misses" : <int>,
  "score" : <int>
  
}
```
**Error Response(s):**
```json
{
  "ok" : false,
  "message" : "Numbers are not allowed. You must provide a letter"
}
```
```json
{
  "ok" : false,
  "message" : "Letter cannot be empty. Please provide a letter"
  
}
```
```json
{
  "ok" : false,
  "message" : "Letter must be a single character"
}
```
```json
{
  "ok" : false,
  "message" : "Letter already guessed"
}
```
---

### 4. Game state

**Request:**
```json
{
  "type" : "hangman",
  "action" : "state"
}
```
**Success Response**
```json
{
  "type": "hangman",
  "ok": true,
  "word": "_ a _ _",
  "misses": <int>,
  "attemptsLeft": <int>,
  "score": <int>,
  "hangman": <String> 
}
```
---

### Win/Lose Detection // happens after guess or word guess

**Success Response**
```json
{
  "type": "hangman",
  "ok" : true,
  "status" : "win",
  "word" : <String>,
  "score": <int>
}
```
```json
{
  "type": "hangman",
  "ok" : true,
  "status" : "lose",
  "word" : <String>
}
```
---

### 5. Hint
**Request:**
```json
{
  "type": "hangman",
  "action": "hint"
}
```
**Success Response:**
```json
{
  "type": "hangman",
  "ok": true,
  "hint": <String>,
  "word": <String>,
  "score": <int>
}
```
**Error response:** 
```json
{
  "ok" : false,
  "message" : "No letters left to reveal"
}
```
---
### 6. Word Guessing
**Request:**
```json
{
  "type": "hangman",
  "action": "guessword",
  "word": <String>
}
```
**Success Response (correct):**
```json
{
  "type": "hangman",
  "ok": true,
  "status": "win",
  "word": <String>,
  "score": <int>
}
```
**Success Response (incorrect):**
```json
{
  "type": "hangman",
  "ok": true,
  "correct": false,
  "attemptsLeft": <int>,
  "word": <String>
}
```
**Error response:**
```json
{
  "ok" : false,
  "message" : "Invalid word guess"
}
```
---

### 7. Guessed Letters
**Request:**
```json
{
  "type": "hangman",
  "action": "guessedLetters"
}
```
**Success Response:**
```json
{
  "type": "hangman",
  "ok": true,
  "letters": <Array of String>  // example "letters": ["a", "e,", "t"]
}
```
---


### 8. Give Up
**Request:**
```json
{
  "type": "hangman",
  "action": "giveup"
}
```
**Success Response:**
```json
{
  "type": "hangman",
  "ok": true,
  "message": "Game ended. You gave up.",
  "word": <String>
}
```
---

### 9. LeaderBoard
**Request:**
```json
{
  "type": "hangman",
  "action": "leaderboard"
}
```
**Success Response:**
```json
{
  "type": "hangman",
  "ok": true,
  "leaderboard": [
    {
      "name": <String>,
      "bestScore": <int>,
      "avgScore": <int>,
      "winRate": <int>,
      "games": <int>
    }
  ]
}
```

### 10. quit
**Request:**
```json
{
  "type": "quit"
}
```
**Success Response:**
```json
{
  "type": "quit",
  "ok": true,
  "message": "Goodbye <playerName>! Thanks for playing!" 
  
}

```

---


## Error Handling Strategy

[Explain your approach to error handling:]

**Server-side validation:**
- [What validations does your server perform?]
  The server validates all incoming requests for the required fields such as "type", "action", and other parameters

- [How do you handle missing fields?]
  Missing fields return a JSON response with ok = false and a descriptive error message that specifies what went wrong,
- and prompting the user to enter the required field.

- [How do you handle invalid data types?]
  Invalid data types such as non-string letter inputs are handled with the appropriate error message

- [How do you handle game state errors?]
  Game state errors are handled by validating the current state of the game before processing requests.
  For example, the server checks whether a game is currently active before allowing actions such as
  guessing letters, requesting hints, or guessing the word.

---

## Robustness

[Explain how you ensured robustness:]

**Server robustness:**
- [How does server handle invalid input without crashing?]
- The server validates all inputs before processing and never crashes on invalid input.
- Exceptions are handlded gracefully and converted into JSON responses. 


**Client robustness:**
- [How does client handle unexpected responses?]
- The client checks the "ok" field in every response before accessingg the values

- [What happens if server is unavailable?]
- If the server is unavailable or disconnects, the client displays an error message ("Error communicating with server"),
- instead of crashing.

---

## Assumptions (if applicable)

[List any assumptions you made about the protocol or game rules]

1. [Assumption 1]
2. [Assumption 2]
3. [etc.]

---

## Known Issues

[List any known bugs or limitations]

1. Input handling ambiguity: 
The game uses numeric inputs (1-4, 0) for menu commands while also accepting free-form input for guesses. This creates a
limitation where numeric inputs could be interpreted as commands rather than invalid guesses. I addressed this by prioritizing
command handling first on the client side and having the server to perform additional validation to reject non-letter inputs.
However, this dual use of numeric inputs can be unintuitive for the user. 
2. Give UP control flow bug (Fixed after recording):
After recording the screencast, I noticed that when typing "yes" in the "GiveUp" option, the program would exi the game loop
instead of returning to main menu. I pin pointed the problem in the incorrect return in the client's showGameMenu method. 
I fixed the issue by properly handling the inGame state and loop control. However, I recorded the video demonstration before I 
implemented this fix. 

---
