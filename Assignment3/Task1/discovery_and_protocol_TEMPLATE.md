# Task 1.2: Mystery Service Discovery and Protocol Documentation

**Your Name:** Virginia Appleton 
**How I tested:** Extended client 

---

## Part 1: Discovery Log

Document at least 8 test attempts showing your systematic investigation.

### Attempt 1
**Request Sent:**

```json
{
  "type": "analyzer"
}
```

**Response Received:**
```json
{
  "ok": false,
  "message": "Field 'action' does not exist in request. Hint: what action do you want to perform?"
}
```
**What I Learned:**
I learned that the analyzer service requires an action field

---

### Attempt 2
**Request Sent:**
```json
{
  "type" : "analyzer",
  "action" : "test"
}
```

**Response Received:**
```json
{
  "ok": false,
  "message" : "Field 'text' does not exist in request"

}
```

**What I Learned:**
The analyzer service also requires a text field . 
---

### Attempt 3
**Request Sent:**
```json
{
  "type" : "analyzer",
  "action" : "test",
  "text" : "I am playing detective"
}
```

**Response Received:**
```json
{
  "ok" : false,
  "message" : "Action 'test' not supported. Valid actions: wordcount, charcount, search"

}
```

**What I Learned:**
 The analyzer service supports three actions: "wordcount", "charcount", and "search"


---

### Attempt 4
**Request Sent:**
```json
{
  "type" : "analyzer",
  "action" : "wordcount",
  "text" : "I am playing detective"
}
```

**Response Received:**
```json
{
  "count" : 4,
  "action" : "wordcount",
  "type": "analyzer",
  "ok" : true
}
```

**What I Learned:**
The wordcount action returns the number of words in the input text using the "count" field. 

---
### Attempt 5
**Request Sent:**
```json
{
  "type" : "analyzer",
  "action" : "charcount",
  "text" : "I am playing detective"
}
```

**Response Received:**
```json
{
  "count":22,
  "action":"charcount",
  "type":"analyzer",
  "ok":true
}
```

**What I Learned:**
The charcount action returns the number of characters in a string including whitespaces


---

### Attempt 6
**Request Sent:**
```json
{
  "type" : "analyzer",
  "action" : "search",
  "text" : "I am playing detective"
}
```

**Response Received:**
```json
{
  "ok": false,
  "message": "Field 'find' does not exist in request"
}
```

**What I Learned:**
The search action requires an additional "find" field specifying what to search for. 


---
### Attempt 7
**Request Sent:**
```json
{
  "type" : "analyzer",
  "action" : "search",
  "text" : "I am playing detective",
  "find" : "e"
}
```

**Response Received:**
```json
{
  "found": true,
  "find":"e",
  "count":3,
  "action":"search",
  "positions":[14,16,21],
  "type":"analyzer",
  "ok": true
}
```

**What I Learned:**
The search action finds a target character in a string and returns whether that value was found, the number of matches ("count")
and the positions of each match ("positions")

---
---
### Attempt 8
**Request Sent:**
```json
{
  "type" : "analyzer",
  "action" : "search",
  "text" : "I am playing detective",
  "find" : "playing"
}
```

**Response Received:**
```json
{
  "found":true,
  "find":"playing",
  "count":1,
  "action":"search",
  "positions":[5],
  "type":"analyzer",
  "ok":true""
}
```

**What I Learned:**
The search action also supports searching for full substrings, not just single characters. The "positions" field
returns the index where the match begins.

---

---

## Part 2: Complete Protocol Specification

Follow the same format as Task 1.1 README protocols.

### Analyzer Service

This service performs text analysis using three operations: wordcount, charcount and search. 

### General Error responses


#### [wordcount]

**Request:**
```json
{
  "type" : "analyzer",
  "action" : "wordcount",  
  "text" : <String>

}
```

**Success Response:**
```json
{
  "count": <int>,
  "action":"wordcount",
  "type":"analyzer",
  "ok": true
}
```

**Error Responses:**
Same General error responses apply


#### [charcount]

**Request:**
```json
{
  "type" : "analyzer",
  "action" : "charcount", 
  "text" : <String>

}
```

**Success Response:**
```json
{
  "count": <int>,
  "action":"charcount",
  "type":"analyzer",
  "ok": true
}
```

**Error Responses:**
Same General error responses apply

#### [search]

**Request:**
```json
{
  "type" : "analyzer",
  "action" : "search", 
  "text" : <String>,
  "find" : <String>

}
```

**Success Response:**
```json
{
  "type": "analyzer",
  "ok": true,
  "action": "search",
  "find": <String>,
  "found": <bool>,
  "count": <int>,
  "positions": <Array of int>
}
```

**Error Responses:**

```json
{
  "ok" : false,
  "message" :"Field 'find' does not exist in request" 

}
```
```json
{
  "ok": false,
  "message": "Field 'action' does not exist in request"
}
```
```json
{
  "ok": false,
  "message": "Field 'text' does not exist in request"
}

```
```json
{
  "ok": false,
  "message": "Action '<value>' not supported. Valid actions: wordcount, charcount, search"
}
```
---

## Part 3: Summary

**Total Operations Discovered:**  

3 (wordcount, charcount and search)  

**How I approached discovery:**  

I started with a minimal request and used the error messages returned by the service to guide my next steps. 
I then added required fields and tested different action values. Each response helped me identify the required fields, 
supported operations, and the structure of the output.  

**Most challenging part:**  

The most challenging part was interpreting the error messages and understanding how each message corresponded to missing fields or incorrect inputs.