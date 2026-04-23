## How to run:
**Start Leader:**  
gradle runLeader --args="9000"  

**Start Worker:**  
gradle runWorker --args="Worker1 localhost 9000"  
**Note:** The Leader needs a minimum of 3 workers to begin execution

## Protocol Description
This system implements a simple distributed consensus protocol using a hierarchical Leader-Worker model. 

The Leader acts as the coordinator, while multiple Worker nodes operate independently and communicate with the Leader.
The Workers do not communicate with each other.

Each consensus round follows this sequence:

1. The Leader broadcasts an arithmetic task to all active workers using: TASK < expression>  

2. Each worker receives the task, prompts the user for a result and sends the result back using: RESULT < value>

3. The Leader collects responses concurrently using threads and applies a majority voting to determine consensus.
4. The Leader then broadcast the final decision:  
CONSENSUS < value> < agreement_ratio> or if no consensus is reached, the Leader prints a message and proceeds to the next round. The workers also reflect when a consensus could not be reached. 

## Consensus Algorithm Design
The system uses a majority voting algorithm to determine consensus among worker responses. Each worker independently
submits a result to the Leader.  
The Leader spawns a separate thread per worker to collect responses concurrently. A helper class called WorkerListener synchronizes these threads and ensures the leader waits until all workers respond or a timeout is reached.  

This approach improves accuracy and efficiently by ensuring the Leader proceeds as soon as all the responses are available, while
still handling delayed or failed workers gracefully.  

## Decision Rules  
- A strict majority (greater than 50%) is required to reach consensus.
- The value with the highest number of votes is elected. 

There are three possible outcomes:
1. **Consensus:**  
One value receives more than half of the votes
2. **Tie**  
Multiple values share the highest vote count
3. **No Consensus**  
A value has the most votes but does not meet the majority threshold. 

## Worker Failures  
The system handles worker failures gracefully:  
**If a worker disconnects:**  
- The Leader detects it via socket failure or null repsonse. 
- The worker is removed from the active worker list.
- The Leader continues with the remaining workers

**If a worker does not respond in time**  
- The Leader proceeds after a timeout. 
- If no sufficient repsonses are received, it retries or moves on to the next task.


## Tie Handling  
**If there's a tie:**  
- The Leader  retries the same task once.  
**If the tie persists:**  
- The Leader reports no consensus
- The system moves to the next task. 

## Handling delays and synchronization

Workers can take time to respond due to user input. To handle this, the workers continuously check for new messages while
waiting for input.  
If a new task or shutdown message arrives:  
- The worker interrupts the current input
- Processes the new message immediately. 

This prevents workers from becoming stuck on outdated tasks. 

## Leader shutdown behavior

When the Leader shuts down, it sends a message to all connected workers. The workers then detect the shutdown signal and exit gracefully. 

Additionally, the workers also detect disconnection when readLine() returns null. This ensures proper shutdown even if the connection 
closes unexpectedly. 

## Edge cases and Limitations
The main limitation is that consensus is based on majority voting and not correctness. If the majority of workers agree on an incorrect
answer, it is still accepted as consensus. This is assumed to be acceptable since demonstrating computational correctness is not the main purpose of the assignment. 
Another limitation is that, since workers depend on user input, delays may happen that can lead to retries or missed rounds.

As for edge cases, input validation is handled on both the Leader and the Worker. Invalid arithmetic expressions are rejected before being broadcast, and if a
worker enters an invalid input such as a string, the system prompts them to enter a valid number. 

## Issues encountered and resolution 

One major issue encountered was infinite retry loops when no workers responded. The problem was that the Leader would repeatedly retry the same task without exiting. 
To fix this, I introduced a retry limit. After one retry, the Leader moves to the next task if no responses are received. 

Another issue was the workers becoming stuck waiting for input when a new task was issued. To address this, I updated the workers to check for incoming messages while waiting for 
input and interrupt outdated tasks when necessary.
This ensured proper synchronization between Leader and Workers. 

