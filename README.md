# amqp-io
Serialize Java objects using the AMQP type system

This library allows you to encode/decode Java objects to/from AMQP encoded bytes.
It supports Java primitive types as well as user defined custom types. It also
allows using annotations to further control what/how classes and fields should
be encoded.

This library is not meant to be another Java serializer that supports all Java
types. The major goal is to encode Java objects in a standard type system, so
the application can interoperate with any another AMQP clients.

For example, given the following Java class,
```
@AmqpContract(name = "test.amqp:person")
public class Person {
    @AmqpMember(order = 1)
    public String name;
    
    @AmqpMember(order = 2)
    public int age;

    @AmqpMember(order = 3)
    public Date dateOfBirth;

    @AmqpMember(order = 8)
    public Map properties;
    
    public long ver;
}
```

You can serialize an instance of it by doing,
```
    AmqpSerializer.serialize(buffer, person);
```

The buffer will now contain the serialized object in AMQP type system.
You can send it over the network with an AMQP client, or persist it
on disk for storage purposes. When the buffer is availabe to another
AMQP client, it will be able to decode the data and present it in a way
defined by that client.

For example, with [amqpnetlite](https://github.com/azure/amqpnetlite), you can define a C# class as follows,
```
[AmqpContract(Name = "test.amqp:person")]
public class Person {
    [AmqpMember(Order = 1)]
    public String Name;
    
    [AmqpMember(order = 2)]
    public int Age;

    [AmqpMember(order = 3)]
    public Date DateOfBirth;

    [AmqpMember(order = 8)]
    public IDictionary Properties;
}
```

and decode the object by calling,
```
    Person person = AmqpSerializer.Deserialize<Person>(buffer);
```

API documentation: http://xinchen10.github.io/amqp-io