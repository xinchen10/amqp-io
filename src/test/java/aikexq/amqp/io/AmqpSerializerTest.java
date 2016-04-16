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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AmqpSerializerTest {
    double fieldSetThreshold = 0.5;
    
    byte byteValue = (byte)123;
    short shortValue = (short)33333;
    int intValue = 888888;
    long longValue = -887323;
    float floatValue = 1234567.8899f;
    double doubleValue = -99.32;
    Date dateValue = new Date(System.currentTimeMillis());
    char charValue = 'H';
    String stringValue = "testEventProvider";
    List listValue = Arrays.asList("ddd", 89, -9.08f);
    Map mapValue = new HashMap() {{ put("k1", "value1"); put("k2", null); put("k3", 123456789L); }};
    
    public AmqpSerializerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    private Event createEvent() {
        Event e = new Event();
        if (Math.random() >= fieldSetThreshold) {
            e.provider = this.stringValue;
        }
        if (Math.random() >= fieldSetThreshold) {
            e.params = this.listValue;
        }
        if (Math.random() >= fieldSetThreshold) {
            e.priority = this.byteValue;
        }
        if (Math.random() >= fieldSetThreshold) {
            e.id = this.shortValue;
        }
        if (Math.random() >= fieldSetThreshold) {
            e.count = this.intValue;
        }
        if (Math.random() >= fieldSetThreshold) {
            e.code = this.longValue;
        }
        if (Math.random() >= fieldSetThreshold) {
            e.badget = this.floatValue;
        }
        if (Math.random() >= fieldSetThreshold) {
            e.gain = this.doubleValue;
        }
        if (Math.random() >= fieldSetThreshold) {
            e.timestamp = this.dateValue;
        }
        if (Math.random() >= fieldSetThreshold) {
            e.onTime = true;
        }
        if (Math.random() >= fieldSetThreshold) {
            e.shortName = this.charValue;
        }
        if (Math.random() >= fieldSetThreshold) {
            e.propertes = this.mapValue;
        }
        return e;
    }
    
    private void validate(Event e1, Event e2) {
        Assert.assertEquals("timestamp", e1.timestamp, e2.timestamp);
        Assert.assertEquals("provider", e1.provider, e2.provider);
        Assert.assertEquals("priority", e1.priority, e2.priority);
        Assert.assertEquals("id", e1.id, e2.id);
        Assert.assertEquals("id", e1.id, e2.id);
        Assert.assertEquals("count", e1.count, e2.count);
        Assert.assertEquals("code", e1.code, e2.code);
        Assert.assertEquals("badget", e1.badget, e2.badget);
        Assert.assertEquals("gain", e1.gain, e2.gain);
        Assert.assertEquals("onTime", e1.onTime, e2.onTime);
        Assert.assertEquals("shortName", e1.shortName, e2.shortName);
        assertListEquals(e1.params, e2.params);
        assertMapEquals(e1.propertes, e2.propertes);
    }
    
    private void assertListEquals(List l1, List l2) {
        Assert.assertTrue("null check", (l1 == null && l2 == null) || (l1 != null && l2 != null));
        if (l1 == null || l2 == null) {
            return;
        }
        
        Assert.assertEquals("size", l1.size(), l2.size());
        for (int i = 0; i < l1.size(); i++) {
            Assert.assertEquals("item" + i, l1.get(i), l2.get(i));
        }
    }

    private void assertMapEquals(Map m1, Map m2) {
        Assert.assertTrue("null check", (m1 == null && m2 == null) || (m1 != null && m2 != null));
        if (m1 == null || m2 == null) {
            return;
        }
        
        Assert.assertEquals("size", m1.size(), m2.size());
        for (Object k : m1.keySet()) {
            Object v1 = m1.get(k);
            Object v2 = m1.get(k);
            Assert.assertEquals(k + " value", v1, v2);
        }
    }
    
    @Test
    public void testPrimitiveType() throws Exception {
        this.runPrimitiveTest(null);
        this.runPrimitiveTest(byteValue);
        this.runPrimitiveTest(shortValue);
        this.runPrimitiveTest(intValue);
        this.runPrimitiveTest(longValue);
        this.runPrimitiveTest(floatValue);
        this.runPrimitiveTest(doubleValue);
        this.runPrimitiveTest(dateValue);
        this.runPrimitiveTest(charValue);
        this.runPrimitiveTest(stringValue);
        this.runPrimitiveTest(listValue);
        this.runPrimitiveTest(mapValue);
    }
    
    @Test
    public void testJavaCustomType() throws Exception {
        System.out.println("type user custom");
        AtomicLong a = new AtomicLong(12345L);
        ByteBuffer buffer = ByteBuffer.allocate(512);
        AmqpSerializer.serialize(buffer, a);
        buffer.flip();
        
        AtomicLong a2 = AmqpSerializer.deserialize(AtomicLong.class, buffer);
        Assert.assertEquals(a.get(), a2.get());
    }
    
    @Test
    public void testUserCustomType() throws Exception {
        System.out.println("type user custom");
        Event e = this.createEvent();
        ByteBuffer buffer = ByteBuffer.allocate(512);
        AmqpSerializer.serialize(buffer, e);
        buffer.flip();
        
        Event e2 = AmqpSerializer.deserialize(Event.class, buffer);
        validate(e, e2);
    }
    
    @Test
    public void testNestedCustomType() throws Exception {
        System.out.println("type nested custom");
        Administrator a = new Administrator();
        a.name = "Fred";
        a.reportTo = new Teacher();
        a.reportTo.name = "Mike";
        
        ByteBuffer buffer = ByteBuffer.allocate(512);
        AmqpSerializer.serialize(buffer, a);
        buffer.flip();
        
        Administrator a2 = AmqpSerializer.deserialize(
                Administrator.class, buffer);
        Assert.assertEquals(a.name, a2.name);
        Assert.assertEquals(a.reportTo.name, a2.reportTo.name);
    }
    
    @Test
    public void testCyclicCustomType() throws Exception {
        System.out.println("type cyclic custom");
        Administrator a = new Administrator();
        a.name = "Fred";
        Administrator b = new Administrator();
        b.name = "Mike";
        a.reportTo = b;
        b.reportTo = a;
        
        ByteBuffer buffer = ByteBuffer.allocate(512);
        Exception error = null;
        try {
            AmqpSerializer.serialize(buffer, a);
        } catch (AmqpIoException e) {
            error = e;
        }
        Assert.assertTrue("encoding should fail", error != null);
        Assert.assertTrue("incorrect error message", error.getMessage().indexOf("Cyclic") >= 0);
    }
    
    @Test
    public void testContractListType() throws Exception {
        System.out.println("type amqp contract list");
        Person p = new Person();
        p.name = "Fred";
        p.age = 31;
        p.properties = new HashMap() {{ put("p", "test"); }};
        p.ver = 100;    // this should not be serialized
        
        ByteBuffer buffer = ByteBuffer.allocate(512);
        AmqpSerializer.serialize(buffer, p);
        buffer.flip();
        
        Person p2 = AmqpSerializer.deserialize(Person.class, buffer);
        Assert.assertEquals(p.name, p2.name);
        Assert.assertEquals(p.age, p2.age);
        Assert.assertEquals(0, p2.ver);
        assertMapEquals(p.properties, p2.properties);
    }
    
    @Test
    public void testContractDerivedType() throws Exception {
        System.out.println("type amqp contract derived class 1");
        Student p = new Student();
        p.name = "Fred";
        p.ver = 100;    // this should not be serialized
        p.address = new Address();
        p.address.hourseStreet = "100 Main St.";
        p.address.city = "Big Tree";
        p.address.state = "XY";
        p.address.zip = "ABC012";
        p.grades = Arrays.asList(3.5f, 4.0f, 3.5f, 3.0f);
        
        ByteBuffer buffer = ByteBuffer.allocate(512);
        AmqpSerializer.serialize(buffer, p);
        buffer.flip();
        
        Person p2 = AmqpSerializer.deserialize(Person.class, buffer);
        Assert.assertTrue(Student.class == p2.getClass());
        Student s = (Student)p2;
        Assert.assertEquals(p.name, s.name);
        Assert.assertEquals(p.address.hourseStreet, s.address.hourseStreet);
        Assert.assertEquals(p.address.city, s.address.city);
        Assert.assertEquals(p.address.state, s.address.state);
        Assert.assertEquals(p.address.zip, s.address.zip);
        assertListEquals(p.grades, s.grades);
    }
    
    @Test
    public void testContractDerivedType2() throws Exception {
        System.out.println("type amqp contract derived class 2");
        Teacher p = new Teacher();
        p.name = "Fred";
        p.office = "EMC4-206";
        p.classes = new HashMap() {{ put(101, "CS 101"); put(401, "MS exp" ); }};
        
        ByteBuffer buffer = ByteBuffer.allocate(512);
        AmqpSerializer.serialize(buffer, p);
        buffer.flip();
        
        Person p2 = AmqpSerializer.deserialize(Person.class, buffer);
        Assert.assertTrue(Teacher.class == p2.getClass());
        Teacher t = (Teacher)p2;
        Assert.assertEquals(p.name, t.name);
        Assert.assertEquals(p.office, t.office);
        assertMapEquals(p.classes, t.classes);
    }
    
    @Test
    public void testContractMapType() throws Exception {
        System.out.println("type amqp contract map");
        Product p = new Product();
        p.name = "cheese";
        p.category = Category.Food;
        p.price = 18.98;
        
        ByteBuffer buffer = ByteBuffer.allocate(512);
        AmqpSerializer.serialize(buffer, p);
        buffer.flip();
        
        Product p2 = AmqpSerializer.deserialize(Product.class, buffer);
        Assert.assertEquals(p.name, p2.name);
        Assert.assertEquals(p.category, p2.category);
        Assert.assertEquals(p.price, p2.price);
    }
    
    private void runPrimitiveTest(Object value) throws AmqpIoException {
        System.out.println("type " + (value == null ? "nil" : value.getClass().getName()));
        ByteBuffer buffer = ByteBuffer.allocate(512);
        AmqpSerializer.serialize(buffer, value);
        buffer.flip();
        Object v2 = AmqpSerializer.deserialize(Object.class, buffer);
        if (value == null) {
            Assert.assertTrue("value not null", v2 == null);
        } else  if (List.class.isAssignableFrom(value.getClass())) {
            assertListEquals((List)value, (List)v2);
        } else if (Map.class.isAssignableFrom(value.getClass())) {
            assertMapEquals((Map)value, (Map)v2);
        } else {
            Assert.assertEquals(value, v2);
        }
    }
}
