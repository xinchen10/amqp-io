//  ------------------------------------------------------------------------------------
//  Copyright (c) xinchen
//  All rights reserved. 
//  
//  Licensed under the Apache License, Version 2.0 (the ""License""); you may not use this 
//  file except in compliance with the License. You may obtain a copy of the License at 
//  http://www.apache.org/licenses/LICENSE-2.0  
//  
//  THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
//  EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR 
//  CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR 
//  NON-INFRINGEMENT. 
// 
//  See the Apache Version 2.0 License for specific language governing permissions and 
//  limitations under the License.
//  ------------------------------------------------------------------------------------

package aikexq.amqp.io;

import java.nio.ByteBuffer;
import java.util.HashSet;

/**
 * <p>AmqpSerializer encodes Java objects into bytes and decodes Java object
 * from bytes using the AMQP type system. Examples of usage,
 * </p>
 * <ul>
 * <li>encode: {@code AmqpSerializer.serialize(ByteBuffer, Object)}</li>
 * <li>decode: {@code AmqpSerializer.deserialize(Class<?>, ByteBuffer)}</li>
 * </ul><br>
 * <p>Java primitive types are encoded as AMQP types directly.
 * <ul>
 * <li>boolean: a boolean value</li>
 * <li>byte: a signed 8-bit integer</li>
 * <li>short: a signed 16-bit integer</li>
 * <li>int: a signed 32-bit integer</li>
 * <li>long: a signed 64-bit integer</li>
 * <li>float: a 32-bit precision IEEE-754 floating point</li>
 * <li>double: a 64-bit precision IEEE-754 floating point</li>
 * <li>char: utf32 BE encoded Unicode character</li>
 * <li>Date: 64-bit signed integer representing milliseconds since the Unix epoch</li>
 * <li>UUID: UUID as defined in section 4.1.2 of RFC-4122</li>
 * <li>byte[]: array of bytes</li>
 * <li>string: UTF8 Unicode string</li>
 * <li>List: a list. Value must be primitive types</li>
 * <li>Map: a map. Key and value must be primitive types</li>
 * </ul>
 * </p>
 * <p>A custom type is serialized as an AMQP described type. By default,
 * the descriptor, which is an AMQP symbol, is the full name of the class,
 * and the value is an AMQP list containing values of the non-static
 * non-transient fields. Each field is identified by its relative position
 * defined in the class. </p>
 * <p>Custom types can be Java built-in types or user-defined types. Since
 * each field is implicitly identified by its position, any changes in
 * ordering can cause breaks.</p>
 * <p>For example, an AtomicLong object is represented as the following AMQP
 * described list.</p>
 * <pre>{@code
 * <type name="atomic-long" source="list" provides="Number">
 *   <descriptor name="java.util.concurrent.atomic.AtomicLong"/>
 *   <field name="value" type="long" mandatory="true"/>
 * </type>
 * }</pre>
 * <p>Not all Java classes can be safely serialized by this serializer,
 * even for those that implement Serializable. They may contain non-compatible
 * data types or implement their own writeObject and readObject methods.
 * The serializer does not invoke the writeObject and readObject because
 * they produce or consume non-AMQP bytes.</p>
 * <p>A class without default constructor is not supported.</p>
 * <p>A class that does special initialization in constructor or readObject
 * method may either fail to be deserialized or not fully initialized after
 * decoding. One such example is the java.net.URI class. After decoding,
 * the "string" field is correctly set but all other transient fields are
 * not initialized.</p>
 * <p>AMQP annotation classes are introduced to address these limitations and
 * provide more control on object serialization without user writing custom
 * readObject/writeObject methods.</p>
 * <p>AmqpContract annotation can be used to customize the descriptor name
 * and the encoding type. AmqpMember annotation specifies which fields to
 * be included in serialization. Fields without AmqpMember annotation are
 * excluded. When the encoding type is set to DescribedMap, the object is
 * encoded as an AMQP described map. The key of each field is the field name
 * by default, and can be changed in AmqpMember annotation.</p>
 * <p>The type name specified in AmqpContract is scoped to the AmqpSerializer
 * instance by which the object is encoded or decoded. When the static 
 * methods are called, the name must be unique globally.</P>
 * <p>The following class definition maps a Book object to an AMQP described
 * map with custom class name and field names.</p>
 * <pre><code>
 *   {@literal @}AmqpContract(name="ns:book" type=EncodingType.DescribedMap)
 *   public class Book {
 *     {@literal @}AmqpMember(name="ns:title")
 *     public string title;
 *     {@literal @}AmqpMember(name="ns:authors")
 *     public string authors;
 *     {@literal @}AmqpMember(name="ns:webiste")
 *     public string website;
 *   }
 * </code></pre>
 * <p>Class inheritance is also supported. If annotated, derived classes
 * must have the same EncodingType as the base class. In cases where
 * a reader knows only the base type but the buffer may contain instances
 * of different types on the inheritance hierarchy, the base class can be
 * annotated with AmqpProvide to define a list of known derived classes.
 * The AmqpProvide annotation  enables the decoder to resolve the correct
 * type of an object in the buffer based on its descriptor name.
 * The following example shows the Book class has two derived classes.</p>
 * <pre><code>
 *   {@literal @}AmqpContract(name="ns:book" type=EncodingType.DescribedMap)
 *   {@literal @}AmqpProvide(types={ChildrenBook.class, FictionBook.class})
 *   public class Book {
 *     //...
 *   }
 * 
 *   {@literal @}AmqpContract(name="ns:children-book" type=EncodingType.DescribedMap)
 *   public class ChildrenBook extends Book {
 *     public int age;
 *   }
 * 
 *   {@literal @}AmqpContract(name="ns:finction-book" type=EncodingType.DescribedMap)
 *   public class FictionBook extends Book {
 *     public string category;
 *   }
 * </code></pre>
 * <p>Given a ByteBuffer, the caller now can call
 * {@code AmqpSerializer.deserialize(Book.class, buffer)} to decode either
 * ChildrenBook or FictionBook instances from the buffer.</p>
 * <p>Cyclic class reference is allowed, but cyclic object reference in
 * encoding is not allowed.</p>
 */
public class AmqpSerializer {
    private static final AmqpSerializer instance;
    private final CustomType customType = new CustomType();
    
    static {
        instance = new AmqpSerializer();
    }
    
    /**
     * Encodes an object graph into bytes.
     * @param buffer Buffer to save the bytes. The buffer's position is
     * advanced after bytes are written.
     * @param graph Object to be encoded.
     * @throws AmqpIoException
     */
    public static void serialize(ByteBuffer buffer, Object graph) throws AmqpIoException {
        instance.writeObject(buffer, graph);
    }
    
    /**
     * Decodes an object of the specified type from the buffer.
     * @param <T> Expected type of the object. The buffer's position is
     * advanced after bytes are read.
     * @param c Class of the type.
     * @param buffer Buffer to read bytes.
     * @return Object of type T.
     * @throws AmqpIoException
     */
    public static <T> T deserialize(Class<T> c, ByteBuffer buffer) throws AmqpIoException {
        return (T)instance.readObject(c, buffer);
    }
    
    /**
     * Encodes an object graph into bytes.
     * @param buffer Buffer to save the bytes. The buffer's position is
     * advanced after bytes are written.
     * @param graph Object to be encoded.
     * @throws AmqpIoException
     */
    public void writeObject(ByteBuffer buffer, Object graph) throws AmqpIoException {
        try {
            this.customType.writeObject(buffer, graph, new HashSet());
        } catch (AmqpIoException ioe) {
            throw ioe;
        } catch (Exception e) {
            throw new AmqpIoException(e.getMessage(), e);
        }
    }
    
    /**
     * Decodes an object of the specified type from the buffer.
     * @param <T> Expected type of the object. The buffer's position is
     * advanced after bytes are read.
     * @param c Class of the type.
     * @param buffer Buffer to read bytes.
     * @return Object of type T.
     * @throws AmqpIoException
     */
    public <T> T readObject(Class c, ByteBuffer buffer) throws AmqpIoException {
        try {
            return (T)this.customType.readObject(c, buffer);
        } catch (AmqpIoException ioe) {
            throw ioe;
        } catch (Exception e) {
            throw new AmqpIoException(e.getMessage(), e);
        }
    }
}
