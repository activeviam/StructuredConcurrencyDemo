Structured Game Of Life Demo
===
This is a demo project showing the concept of
[Structured Concurrency](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful)
and its implementation in [Project Loom](https://jdk.java.net/loom/) using a
[Game Of Life](https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life) simulation as an example.

Concepts
---
### Structured Concurrency

The main idea of Structured Concurrency is that using the `go` operator (or the `new Thread(...).start()`,
`executor.submit()` constructs, etc.) makes the code more complex. From the code, it becomes impossible to
understand how many parallel tasks are being executed at the same time. This statement is similar to the `goto`
operator. Just like `goto`, the `go` statement can create a new branch of control flow anywhere in the program,
which is extremely difficult to trace syntactically. It also makes debugging and error handling much more
difficult. Using callback chains breaks logically sequential code into unrelated parts.

In order to cope with these problems, it is proposed to switch to another design pattern, linking the moments of
creation and completion of parallel subtasks to the block structure of the code. For this, the concept of *scope*
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

### Virtual Threads

Another important concept introduced in Project Loom is *virtual threads*. For the user, a virtual thread is
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

### Extent Locals

Extent locals is a new concept introduced in Java 19 as an alternative to thread locals. Virtual threads support
working with ThreadLocal variables, but their usage can be associated with a significant memory consumption (a hash
table is created in each thread containing all thread-local variables associated with this thread).

A common reason to use ThreadLocal variables is to pass some context between tasks. To do this, the following
wrapper code is often used:

```java
class Task {

	private static final ThreadLocal<Task> CURRENT_TASK = new ThreadLocal<>();
	private Task currentTask;

	public void compute() {
		pushExecutionContext();
		try {
			computeSafely();
		} finally {
			popExecutionContext();
		}
	}

	void pushExecutionContext() {
		this.previousTask = CURRENT_TASK.get();
		CURRENT_TASK.set(this);
	}

	void popExecutionContext() {
		CURRENT_TASK.set(this.previousTask);
		this.previousTask = null;
	}
}
```

This approach is justified when working with a pool of threads. Nevertheless, this approach can cause various
errors and vulnerabilities, ranging from incorrect values of variables (if some part of the context was forgotten
to be set) and up to leaks of secret variables (for example, if as a result of the work-stealing algorithm one task
gains access to the data of another task).

If we use virtual threads, then such a context has another important property: it is set only once during the
lifetime of the thread (because one thread executes only one task). In some sense, a context variable must be
declared final.

ExtentLocal is a concept designed to solve this problem. Like ThreadLocal, an extent-local variable is most often
declared as a static final field of some class. The value that code can read from the extent-local variable depends
on which thread the code is executing on. The key difference from a thread-local variable is how an extent-local
variable is bound to a value.

In order to assign the value of extent-local to a variable, you must declare a scope. Within this scope, the
extent-local variable has the constant value specified when the scope was created. This is done as follows:

```java
class Foo {

	final static ExtentLocal<T> V = ExtentLocal.newInstance();

	void bar() {
		// caller code
		ExtentLocal
				.where(V, someValue)
				.run(() -> {
					// ... some code here ...
					var foo = V.get();
					// ... some more code ...
				});
	}
}
```

This code structure makes it much easier to understand what value the code can read. The immutability of an
extent-local variable allows you to get the required value much faster than in the case of thread-local variables (
scopes create a tree structure, and each node of such a tree stores a cache of extent-local variables, which cannot
be invalidated, since the variables are immutable).

The problem with leakage of secret values is solved as follows. Consider the following code snippet (example from
[https://openjdk.org/jeps/8263012](https://openjdk.org/jeps/8263012), section "Rebinding of extent-local variables"
):

```java
class Foo {

	void foo() {
		String password = PASSWORD.get();
		doSomethingWithPassword(password);

		Logger.log(() -> {
			return "Password: " + PASSWORD.get();
			// Security leak:     ^^^^^^^^^^^^^^
		});
	}
}
```

Inside the `foo()` method, we need access to `PASSWORD`. But in the logger code, the password must be hidden. To do
this, you can change the logger as follows:

```java
class Logger {

	void log(Supplier<String> supplier) {
		if (loggingEnabled) {
			var message = ExtentLocal
					.where(PASSWORD, null)
					.call(supplier::get);
			doLog(message);
		}
	}
}
```

In the `log()` method itself, the code will be able to read the password, but the supplier code will be executed in
a new scope, and in it the value of the `PASSWORD` variable will be hidden behind the new binding.

Extent-locals and structured concurrency scopes are compatible with each other. Using extent-locals is the
recommended way to pass context variables to the parallel subtasks (see
[GameOfLifeContext class](./src/main/java/com/activeviam/experiments/gameoflife/biz/GameOfLifeContext.java)).

Implementation
---

### Task

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

### Watcher pattern

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

Compile & Run
---

In order to compile the project, you need to download OpenJDK Early-Access build with Project Loom support:
[https://jdk.java.net/loom/](https://jdk.java.net/loom/). The code is tested on the Build 19-loom+6-625
(2022/4/29). Since the API is unstable, further updates my break this code.

This build includes the following [bug (JDK-8286859)](https://bugs.openjdk.org/browse/JDK-8286859). It seems that
this bug doesn't affect the code, but I cannot guarantee it. 

To run the project, use the following command:

```bash
java \
  -cp target/classes \
  --enable-preview \
  --add-modules jdk.incubator.concurrent \
  com.activeviam.experiments.gameoflife.Main 
```
