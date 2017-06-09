# Oak Node Stats

This command line utility computes basic statistics about nodes and properties in a JCR repository created by Apache Jackrabbit Oak's Segment Store.

## Build

You can build this project by executing the following command:

    mvn clean package
    
## Run

You can run the built JAR by executing the following command:

    java -jar target/oak-node-stats-*.jar directory
    
where `directory` is the path on the file system of the repository.

You can specify a path in the repository as a second argument. 
In this case, only statistics relevant to that subtree of the repository are computed.

    java -jar target/oak-node-stats-*.jar directory /content

## Output

After a successful execution, the program will output on the command line a certain amount of statistics, indexed by type.
The output has been designed to be easier to consume with standard command line utilities.
An example of output is the following (some parts are omitted for clarity):

    properties.n 266104501
    properties.max 4999
    properties.sum 885316929
    properties.mean 3.326952102174326
    properties.histogram 0 100 266104377
    properties.histogram 100 200 68
    properties.histogram 200 300 6
    properties.histogram 300 400 2
    properties.histogram 600 700 13
    properties.histogram 800 900 1
    properties.histogram 1000 1100 3
    properties.histogram 2900 3000 3
    properties.histogram 4900 5000 28
    property.names.length.n 266104501
    property.names.length.max 18930
    property.names.length.sum 11060586969
    property.names.length.mean 41.564824824214455
    property.names.length.histogram 0 100 213243682
    property.names.length.histogram 100 200 35637071
    property.names.length.histogram 200 300 15966272
    property.names.length.histogram 300 400 561311
    ...

## License

This code is released under an MIT license.
