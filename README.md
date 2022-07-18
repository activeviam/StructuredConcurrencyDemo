Structured Game Of Life Demo
===
This is a demo project showing the concept of
[Structured Concurrency](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful)
and its implementation in [Project Loom](https://jdk.java.net/loom/) using a
[Game Of Life](https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life) simulation as an example.

Concepts
---
<h3>Structured Concurrency</h3>

The main idea of Structured Concurrency is that using the `go` operator (or the `new Thread(...).start()`,
`executor.submit()` constructs, etc.) makes the code more complex. From the code, it becomes impossible to
understand how many parallel tasks are being executed at the same time. This statement is similar to the `goto`
operator. Just like `goto`, the `go` statement can create a new branch of control flow anywhere in the program,
which is extremely difficult to trace syntactically. It also makes debugging and error handling much more
difficult. Using callback chains breaks logically sequential code into unrelated parts.

In order to cope with these problems, it is proposed to switch to another design pattern, linking the moments of
creation and completion of parallel subtasks to the block structure of the code. For this, the concept of _scope_
is introduced. The code in this paradigm looks something like this:

```java
class Foo {

	void bar() {
		try (var scope = new StructuredTaskScope<?>()) {
			Future<?> f1 = scope.fork(subtask1);
			Future<?> f2 = scope.fork(subtask2);
			// ...
			scope.join();
			// Error handling, result processing
		}
	}
}
// Here all the subtasks are guaranteed to be terminated
```

This approach is somewhat similar to the parallelism in OpenMP, but much more flexible.
Abandoning the `goto` statement in favor of `if`, `for/while` statements and function calls, proposed by
E.&nbsp;Dijkstra in the article
["Go to statement considered harmful"](https://scholar.google.com/scholar?cluster=15335993203437612903&hl=en&as_sdt=0.5)
allowed the emergence of high-level programming languages with a block code structure. The absence of a `goto`
operator ensures that if you call a function, eventually you shall exit it and return to the point where you
started. Likewise, you can't just jump out of an `if` block or out of a loop (actually, loops have `break` and
`continue` which were not in Dijkstra's article, but they are nowhere near as destructive as `goto`). It was this
"black box" format with clear entry and exit points that allowed such constructs as exceptions, try-with-resources,
RAII, and many others to appear. At the same time, at the level of machine code, the `goto` operator has not
disappeared anywhere: it’s just that now it is used not haphazardly, but only according to several pre-negotiated
patterns.

Structured Concurrency suggests doing the same with the `go` operator. Scopes allow the programmer to think of a
piece of code as a black box, which we first enter and then exit, while we know for sure that there are no tasks in
the background. Everything that we launched in the block must be completed in it. Every error raised in a subtask
will be delivered to the caller code. Every resource opened before the scope may be closed after the scope since no
background tasks use it.

<h3>Virtual Threads</h3>
Another important concept introduced in Project Loom is _virtual threads_. For the user, a virtual thread is
practically no different from a normal, "physical" thread. The main difference lies in implementation. Regular Java
threads most often correspond to operating system threads. Their management is done by the OS, and this imposes
significant restrictions. First, the process of creating a system thread takes a lot of time. Secondly, using more
than a couple of hundred threads leads to performance degradation, since a significant part of the time is spent
switching between threads. Switching between system threads occurs when the timer expires.

Unlike physical threads, virtual threads are managed by the Java Virtual Machine. From the JVM point of view, a
virtual thread is a special kind of Runnable executing on a special ForkJoinPool. A virtual thread has its own call
stack, ThreadLocal and ExtentLocal variables. When the system starts executing the virtual thread code, it is
associated with the physical carrier thread and executed on it until it is preempted by another virtual thread. In
this case, there is no timer switching: the points where a switch can occur are blocking operations. Typically, if
a virtual thread starts a blocking operation, it will be unmounted from the carrier thread and queued.

Virtual threads are designed with the expectation that there will be a lot of them. Creating a new virtual thread
is very cheap, and their number does not affect system performance (because there is no forced timer switching).
This allows you to create a separate virtual thread for each task. Moreover, using virtual threads we can return to
the “blocking” code design: code pieces like future.get() or thread.join() will cause the current virtual thread to
be unmounted until the result is ready.

Structured Concurrency combined with Virtual Threads allows you to write easy-to-understand multi-threaded code.

Implementation
---

<h3>Task</h3>
The main building block of this project is the `ATask` abstract class. This class encapsulates functionality that
allows multiple blocks of code to refer to the same subtask. The code for this class looks like this:

```java
abstract class ATask<V> extends Callable<V> {

	private final AtomicBoolean startedFlag = new AtomicBoolean(false);
	private final CountDownLatch done = new CountDownLatch(1);

	public V call() throws Exception {
		if (tryStart()) {
			return unsafeCall();
		} else {
			return waitForResult();
		}
	}


	boolean tryStart() {
		return startedFlag.compareAndSet(false, true);
	}

	V unsafeCall() throws Exception {
		try {
			this.result = compute();
			return this.result;
		} catch (Exception ex) {
			this.ex = ex;
			throw ex;
		} finally {
			try {
				dispose();
			} finally {
				done.countDown();
			}
		}
	}

	V waitForResult() throws Exception {
		done.await();
		if (this.ex != null) {
			throw new RuntimeException(this.ex);
		} else {
			return this.result;
		}
	}

	protected abstract V compute() throws Exception;

	protected abstract void dispose();
}
```

You can use this class like this:

```java
class Foo {

	T bar() {
		try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
			Future<T1> f1 = scope.fork(task1);
			Future<T2> f2 = scope.fork(task2);

			// ...

			scope.join().throwIfFailed();

			return merge(f1.resultNow(), f2.resultNow());
		}
	}
}
```

If any of the tasks is already running, the new virtual thread will simply block and wait for the corresponding
task to complete. At the same time, its termination with an error will cause the error to be thrown in all calling
threads.

<h3>Watcher pattern</h3>

In the following example, we add a simple watcher that traces the computation progress:

```java
class Foo {

	T runWithWatcher(ATask<T> mainTask) {
		try (var scope = new StructuredTaskScope.ShutdownOnSuccess<T>()) {
			scope.fork(mainTask);
			scope.fork(() -> {
				try {
					while (true) {
						traceProgress();
						Thread.sleep(1000);
					}
				} catch (InterruptedException e) {
					traceSummary();
					throw e;
				}
			});
			
			return scope.join().result();
		}
	}
}
```