# concurrency-3
Implementation of assignment 3 for COP 4520

## Problem 1: Minotaur's Birthday Presents <br>
4 servants (threads) must write thank you notes for the Minotaur's 500,000 birthday presents tracked by a concurrent linked list.

### How to run:
- Make sure you have navigated to the directory storing all the java files for this assignment.
- To compile and run: at the command line, type "javac PresentList.java && java PresentList" and press enter. <br>
Output:
- Console output: runtime.
- present_list_log.txt: (VERY LONG) prints each addition to and removal from the list.

### Correctness
Correct results mean that all 500,000 presents are added to the list and removed from it eventually. An arraylist with synchronized access tracks which presents have been processed and which haven't. 

Correctnesss also ensures that no present is added twice or removed twice and that no segments of the linked list are orphaned.The solution uses hand-over-hand locking on individual nodes to prevent race conditions. Threads traversing the list to add or remove a node will lock the current and previous nodes as they go. This ensures that no two or more threads can modify a single node's next reference at the same time.

### Efficiency
For the program to run efficiently, there should be no excess of sequential operations. Using fine-grained locks on nodes means that multiple threads that wish to, for example, call the add method don't have to wait for other threads to finish adding.

Another area of efficiency comes from ensuring there are no failed contains() calls. Any given thread knows which present they have added, so that thread and only that thread is responsible for removing that present. This actually eliminates the need for contains calls altogether.

### Progress Guarantee
This approach is deadlock free since locks are acquired in the order of list traversal. If some thread attemps to lock some node, it will eventually succeed since other threads release node locks as they traverse and complete list operations. A thread may have to "wait its turn" to acquire a node, but it will eventually be successful.

### Experimental Evaluation
To ensure correctness, I used a debugging ArrayList to track which presents were appropriately processed. After threads concluded, I checked whether the list contained all the presents as expected and no duplicates. I also checked the detailed logs to double check that any present added was also removed. At the end of processing, I checked the size of the list to make sure it contained only the two sentinel nodes.

## Problem 2: Atmospheric Temperature Reading Module
8 sensors (threads) must take temperature readings and store those readings in a shared data structure for the current minute. A report must be generated after every 60 minutes that includes information about the readings.

### Important Note
Simulation mode is enabled by default. This means, after taking readings, the program will wait for the current "minute" (represented as 60 ms in this implementation) to end before taking more readings. This will cause the runtime to be a bit longer. To disable simulation mode for faster report generation, set the boolean SIMULATING to false.

### How to run:
- Make sure you have navigated to the directory storing all the java files for this assignment.
- To compile and run: at the command line, type "javac Rover.java && java Rover" and press enter. <br>
Output:
- Console output: None for this problem! Please check the generated txt files for outputs.
- report.txt: Displays the hourly reports over the course of a single day of readings.
- logs directory: rover_debug_log.txt stores a more detailed log of sensor activity and tracked values over the course of all the readings. rover_time_elapsed.txt stores a log of how long the tasks for each minute take. These output logs can be disabled by setting DEBUGGING to false in the Rover class.

### Correctness
Correct execution ensures that no sensor (thread) can overwrite a temperature that another thread recorded. This approach uses a shared array to store temperatures. This correctness requirement is guaranteed because each thread contains a unique ID, administered by an AtomicInteger counter, that corresponds to a unique index into the global array. Threads only overwrite their own past measurements.

Correct execution also ensures that values for each minute are not overwritten before they can be evaluated to keep track of top 5 highest and lowest temperatures as well as large temperature differences as per the report requirements. This implementation uses a CountDownLatch to make sure all sensors are able to signal when they are finished taking readings before the readings for the current minute are evaluated. This way, no readings are overlooked.

### Efficiency
A simple array was chosen as the shared memory storage structure since indexing into it happens in constant time. TreeSets are used to track the 5 highest and lowest temperatures since they automatically keep track of order. Operations on these TreeSets are logarithmic, which is is sufficiently fast enough, considering each only contains at most six values (before one value is pruned).

Temperature readings happen almost entirely concurrently. The only locking mechanism is when threads decrement the CountDownLatch to signify that they have completed their reading for the current minute.

### Progress Guarantee
This execution is fairly straightforward. Deadlocking does not occur since there are no complicated operations that can cause threads to enter race conditions. Java's CountDownLatch is thread safe, and it is the only agent that enforces waiting.

### Experimental Evaluation
This program prints additional logs beyond the required hourly reports. The in-depth log, rover_debug_log.txt, records readings and the largest difference observed for each minute. I compared these logs against the results in the reports to verify accuracy.

This program also prints additional information about how long both readings and subsequent evaluations take. The requirements specify that these operations cannot take longer than a minute. Based on the output in rover_time_elapsed.txt, this implementation's runtime is well within that threshold, never exceeding 60 milliseconds on my machine.
