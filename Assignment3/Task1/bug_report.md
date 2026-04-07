# StringConcatenation Debugging Exercise

## Overview
The stringconcatenation service is implemented in both client and server, but has **4 bugs** that prevent it from working correctly according to the protocol specification.

The Correct Protocol is in the README.md

---

## The 4 Bugs

### Bug #1:  Wrong field name on client side
**Location:** `SockClient.java`, line 74

**The Problem:**
```
The protocol expects the first string value to be stored under the key "String1" but the client uses "str1".
Because of this mismatch, the server doesn't receive the expected field. 
```

**The Fix:**
```
Replace "str1" with "string1" in the client request.
```

**Why it matters:** 
```
If the field name doesn't match the protocol, the server cannot find the required value and will reject the request. 
```

**How did you find this:**
```
When running the  server and client, I received the error: "Field string1 does not exist in request", which indicated the mismatch
in field names. 
```

### Bug #2:  Server returns wrong type
**Location:** `SockServer.java`, line 184

**The Problem:**
```
The protocol specifies that the response type should be "stringconcatenation", but the server returns "concat" instead
```

**The Fix:**
```
Update the server to return "type": "stringconcatenation". 
```

**Why it matters:**
```
The client relies on the "type" field to determine how to process the response. If the type is incorrect, the server may 
not handle the response properly. 
```

**How did you find this:**
```
I compared the protocol specification with the server’s response and noticed the mismatch in the "type" field
```

### Bug #3:  Server uses wrong response field name
**Location:** `SockServer.java`, line 188

**The Problem:**
```
The protocol specifies that the concatenated string should be returned in the "result" field, but the server uses "combined" instead. 
```

**The Fix:**
```
Replace "combined" with "result" in the server response. 
```

**Why it matters:**
```
The client expects the result in the "result" field. If a different field name is used, the client will not be able to 
retrive the concatenated striing correctly. 
```

**How did you find this:**
```Same as  bug #2, I compared the protocol's success response with the server implementation and noticed that the field names
did not match.
``` 

### Bug #4:  Missing client-side handling for stringconcatenation
**Location:** `SockClient.java`, line 124

**The Problem:**
```
There wasn't a specific "if-else" branch to handle responses of type "stringconcatenation". As a result, the code fell into the default case 
which attempts to read "result" as an integer using getInt(). 
```

**The Fix:**
```
Add an else-if condition to handle "stringconcatenation" and use getString("result") instead of getInt(). 
```

**Why it matters:**
```
the "result" field for this service is a String, not an integer. Attempting to parse it as an integer causes a runtim exception (JSONException). 
```

**How did you find this:**
```
After fixing the previous bugs, I received the error: "JSONObject["result" is not an int", which indicated that the client was
parsing the repsonse incrrectly. 
```

