CS 149 Programming Assignment 2 

Botao Hu (botaohu@stanford.edu)

[Implementation]

1. Use the nested @Atomic annotation. 
First of all, we replace the ``syncronized'' keywords with @Atomic annotation for all treap's public interface (add, remove, contains). Each data structure operation performs as a whole or none, i.e., if any conflict happens during the operation, the whole operation will roll back and retry. In order to improve the performance more, we add the @Atomic annotation to all the recursive function (addImpl, removeImpl, rotateLeft, rotateRight). Now, logically, we can still regard the public inferface (add, remove, contains) as an atomic operation. But now we actually reduce granularity because if a conflict happens in a recursive function (say addImpl) at deep position on the call stack, STM only rolls back the current function that involves with the current conflict instead of rolling back the whole call stack, i.e., the whole operation. This trick will dramatically reduce the rollback rate. 

2. Reduce unnecessary write operation. 
In the original implementation of Treap, for each time we call the recursive function on a node, the return value of the function will be written back to that node. e.g., 
x.left = addImpl(x.left, a) 
Because the data of Treap node are shared across multiple threads, the large number of writing-back operation will lead to a large number of read-write and write-write conflicts. 
Actually, few of operations will change the root of the tree. It's unnecessary to write the return value back every time. We only need to write back the node when the return root is different from before. This trick will significantly scalability against the increase of thread number. 

3. Use AtomicLong to generate the random number. 
In order to prevent that two threads get the same random number, we use AtomicLong to implement the random number. We use a while loop to generate the next random number until compareAndSet(oldVal, newVal) executes successfully, i.e., no two threads can break this loop at the same time and get the same returns.

[Results]

We run our program in EC2. From ``results.txt'', we can see these insights. 

1. Speed up scales linearly except for 16 threads. 
2. STMTreap outperforms the single thread CoarseLockTreap for thread 8 and 16.
3. STMTreap outperforms STMTreapNoNested (with not nested @Atomic at private function). 
4. LSA is better than TL2 in my experiment result.


