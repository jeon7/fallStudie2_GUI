The program works, but need to look at theses so far.

1. how to merge chat history to other warnings and errors in textArea.
2. if more clients than the number, connection_pool: there are exceptions in the client, not warning messages. 
3. semaphore is written but not tested.
4. in ConnectionHandler, method *_addToMessageList.  remove writer if possible.
in testClient with console, those can't be removed.
5. refactoring, comment, test sysout, etc