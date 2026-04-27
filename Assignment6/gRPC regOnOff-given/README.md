# GRPC Services 

This project implements multiple gRPC services, including both provided and custom functionality. The services demonstrate both stateless and stateful designs, 
 input validation, and persistent storage: 

- ConverterService (stateless unit conversion)
- Library Service (stateful book management with persistence)
- BucketList Service (Custom stateful service for managing personal bucketlist items)

Each service fulfills core assignment requirements:
- Supporting multiple RPC requests
- Accepting structured input from clients
- Returning different response types
- Using repeated fields for list-based responses
- Maintaining persistent data on the server (for stateful services)

## How to run the program
**Step 1: Start the server with:**  

    gradle runNode 

**Step 2: Start the client ina new terminal with:**  

    gradle runClient  

The client will connect to the server at `localhost:8000`. 

## Implemented services:

### Converter Service
The converter service provides unit conversion multiple categories:  
- Length (Kilometers, miles, yards, feet)  
- Weight (Kilograms, pounds)
- Temperature (Celsius, Fahrenheit)

This is a stateless service

### Library Service
The Library service manages a collection of books with the following functionality:
- List all books
- Search books by title or author
- Borrow books
- Return books

This is a stateful service

### Bucketlist Service (Custom service)

The BucketList service allows users to manage a list of personal goals or experiences (bucketlist items). 
This service has the following functionality:
- Add new items
- List all items
- Mark items as completed
- Delete items

This is a stateful service


## How to use the program

When the client starts, the user is prompted to select a service:

1. Converter
2. Library
3. BucketList

### Converter Service
- Enter a numeric value  
- Enter a source unit (e.g., KILOMETER, POUND, CELSIUS)
- Enter a target unit  
The program returns the converted value or an error message

### Library Service
Options:
- List books: Displays all the books
- Search books: Prompts user to enter title or author
- Borrow book: prompts user to enter ISBN and name
- Return book:  prompts user to enter ISBN  
The system validates input and displays appropriate messages. 

### BucketList Service  
Options:
- Add item:  Prompts user to enter description
- List items: displays a numbered list of bucketlist items
- Complete item: prompts user to select the item number to mark as completed.
- Delete item: prompts user to select item number to be dleeted.  

Notes: 
- Items are shown with simple numbering for ease of use
- Internally, UUIDs are used for identification
- Invalid selections are handled gracefully

## Screencast Link


## List of Requirements fulfilled:  
The project satisfies the following requirements:  
- Service allows multiple requests (Converter, Library and BucketList all support multiple RPC methods)
- Each request includes required input (value, units, ISBN, descriptions, etc.)
- Different responses are returned depending on the request (conversion results, lists, status messages)
- Repeated fields are used (e.g., listBooks, listItems)
- Persisten data storage is implemented (Library and BucketList use JSON files)
- Client provides a user-friendly interface with menu-driven interaction
- Server handles invalid input gracefully without crashing


## gradle test

IMPORTANT: Tests expect the server to be running first!
First run in one terminal:  

    gradle runNode  
Then in second terminal:   
    
    gradle test

The tests connect to localhost:8000 by default.

To run in IDE:
- go about it like in the ProtoBuf assignment to get rid of errors
- all mains expect input, so if you want to run them in your IDE you need to provide the inputs for them, see build.gradle