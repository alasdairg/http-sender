# HttpSender

HttpSender is an Http Client for Java. Features of note: 
* No dependencies outside the JDK - it just uses Java's venerable Http(s)UrlConnection under the hood
* Intuitive "fluent" interface
* Built-in timing of requests
* Asynchronous and synchronous operation
* Easy to configure use of a proxy
* Simple templating mechanism supporting string interpolation in url, headers and query params
* Convenience class to simplify Form POSTs
* Fallback mechanism, allowing sophisticated patterns of retries and alternative urls to be specified easily
* Client-side certificate support for Https (either from a Keystore or directly from PEM files)

#### No dependencies
This is what originally motivated me to write this in the first place - I was fed up with the Maven equivalent of "DLL hell", with multiple incompatible versions of the Apache Http Client pulled into projects through third party libraries. This library depends on nothing outside of the JDK itself. It became a small pet project and grew arms and legs since then ...

#### Fluent interface
```java
Response response = 
   new Get("https://httpbin.org/x/y/z?a=1&b=2&c=3")
      .queryParam("d", "4")
      .queryParam("e", "5.1", "5.2", "5.3")
      .header("User-Agent", "HttpSender")
      .header("Cache-Control", "no-cache")
      .timeout(5000)
      .execute();

Response response = 
   new Post("https://httpbin.org/x/y/z")
      .contentType("application/json")
      .requestBody("{ \"someJson\": true }")
      .execute();
   
Response response = 
   new Put("https://httpbin.org/x/y/z")
      .contentType("application/json")
      .requestBody("{ \"someJson\": true }")
      .execute(); 
   
Response response = new Delete("https://httpbin.org/x/y/z").execute();
```          
#### Built-in timing of requests
```java
Response response = new Get("http://www.google.com").execute();
//Fully read the response
String str = response.bodyAsString();
//Elapsed time is from submission of request to close of Response OutputStream
long timeTakenInMilliseconds = response.getElapsed(); 
```
#### Async support
```java
//Async handlers
new Get("https://httpbin.org/x/y/z?a=1&b=2&c=3")
   .executeAsync(response -> {}, throwable -> {});
   
//Async handlers - use a specified Executor to send request
new Get("https://httpbin.org/x/y/z?a=1&b=2&c=3")
   .executeAsync(response -> {}, throwable -> {}, Executors.newCachedThreadPool());
   
//Return a future
CompletableFuture<Response> future = 
   new Get("https://httpbin.org/x/y/z?a=1&b=2&c=3")
      .executeAsync();

//Return a future - use a specified Executor to send request
CompletableFuture<Response> future = 
   new Get("https://httpbin.org/x/y/z?a=1&b=2&c=3")
      .executeAsync(Executors.newCachedThreadPool());
```
#### Send requests through a proxy
```java
Response response = 
   new Get("https://httpbin.org/x/y/z?a=1&b=2&c=3")
      .proxy(HttpProxy.at("10.10.10.123", 8118))
      .execute();
   
//... with credentials
Response response = 
   new Get("https://httpbin.org/x/y/z?a=1&b=2&c=3")
      .proxy(HttpProxy.at("10.10.10.123", 8118), "user", "password")
      .execute();
```
#### Simple templating mechanism supporting string interpolation
```java
Get template = 
   new Get("https://{hostname}/x/y/z?a={AA}&b=2&c=3")
      .queryParam("d", "{DD}")
      .queryParam("e", "5.1", "5.2", "5.3")
      .header("User-Agent", "{agent}")
      .header("Cache-Control", "no-cache")
      .timeout(5000);
   
Get request1 = 
   template.copy()
     .placeholder("hostname", "httpbin.org")
     .placeholder("AA", "request1_A")
     .placheolder("DD", "request1_D")
     .placeholder("agent", "request1_agent");
                     
Get request2 = 
   template.copy()
     .placeholder("hostname", "somewhere-else.org")
     .placeholder("AA", "request2_A")
     .placheolder("DD", "request2_D")
     .placeholder("agent", "request2_agent");
   
Response response1 = request1.execute();
Response response2 = request2.execute();
```
#### Convenience class to simplify Form POSTs
```java
Response response = 
   new FormPost("https://httpbin.org/x/y/z")
      .formField("field1", "value1")
      .formField("field2", "value2")
      .formField("field3", "value3")
      .execute();
       
//Also supports placeholders
FormPost template = new FormPost("https://httpbin.org/x/y/z")
   .formField("{X}", "value1")
   .formField("field2", "{Y}")
   .formField("field3", "value3");
   
Response response = 
   template.copy()
      .placeholder("X", "field1")
      .placeholder("Y", "value2")
     .execute();
```
#### Fallback mechanism
```java
Get primary = new Get("http://mainserver@somewhere.com/blah");
Get secondary = new Get("http://backupserver@somewhere.com/blah");
   
FallbackRequest fr1 = new FallbackRequest();
//Request to the primary server then immediately request to the secondary server if that fails.
Response response = 
   fr1.tryRequest(primary, true)
      .tryRequest(secondary, true)
      .execute();
   
FallbackRequest fr2 = new FallbackRequest();
//Try the primary server up to 3 times then try the secondary server up to 3 times
Response response = 
   fr2.tryRequest(primary, true, RetryStrategy.maxTotalTries(3))
      .tryRequest(secondary, true, RetryStrategy.maxTotalTries(3))
      .execute();
      
FallbackRequest fr3 = new FallbackRequest();
//Try the primary server up to 3 times then try the secondary server up to 3 times
//Pause between retries for 10, 20 seconds for primary and 30 seconds for secondary
Response response = 
   fr3.tryRequest(primary, true, RetryStrategy.maxTotalTries(3), BackoffStrategy.specified(10000, 20000))
      .tryRequest(secondary, true, RetryStrategy.maxTotalTries(3), BackoffStrategy.specified(30000))
      .execute();

//FallbackRequests can be nested just like ordinary requests ...
//e.g. try the previous fallback request indefinitely until successful:
FallbackRequest fr4 = new FallbackRequest();
Response response = 
   fr4.tryRequest(fr3, true, RetryStrategy.forever())
      .execute();
```
#### Client-side certificates
```java
//From a loaded KeyStore (password arguments optional)
//Overloads also provided for loading KeyStore from Inputstream
KeyStore ks = .....
Response response = 
   new Get("https://httpbin.org/x/y/z?a=1&b=2&c=3")
      .useClientCerts(ClientCerts.fromKeyStore(ks, "ksPassword", "entryPassword"))
      .execute();
     
//From one or more PEM files
//Overloads also provided for loading PEM files from InputStreams,
//File objects or byte[]s         
Response response = 
   new Get("https://httpbin.org/x/y/z?a=1&b=2&c=3")
      .useClientCerts(ClientCerts.fromPEM("/a/b/c/mycerts.pem", "/x/y/z/another.pem"))
      .execute();
```
