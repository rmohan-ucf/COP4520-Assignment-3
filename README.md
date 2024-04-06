# COP4520-Assignment-3

For problem one, I used reentrant locks while handling presents to ensure that
whenever I servant moves a present, the other servants are denied access to that
present, preventing errors such as duplicate presents on the chain. I also added
sanity checking before and after locking to ensure that no mishaps were possible
(for example if I found the position to add a present, after locking, I double
check to see if that position is still intact or if another thread has already
added a present in between cycles)

For problem two, I had a thread for each sensor and made sure that the memory area
wouldn't have thread issues by using Java's builtin synchronizedList as well as
making sure that each thread only stores sensor data in its respective column,
ensuring that the same data point isn't being modified by multiple threads.


## Execution Instructions

To compile & execute (using `openjdk 21.0.2`):
```bash
# Problem 1
java Birthday.java

# Problem 2
java Temperature.java
```
