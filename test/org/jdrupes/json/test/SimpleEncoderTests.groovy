package org.jdrupes.json.test;

import javax.management.ObjectName

import org.jdrupes.json.JsonBeanEncoder
import org.jdrupes.json.JsonRpc

import groovy.transform.InheritConstructors
import spock.lang.*

class SimpleEncoderTests extends Specification {

    void "Date Test"() {
        setup: "Create data"
        Date date = new Date(Date.UTC(83, 11, 17, 20, 07, 12));

        when:
        String json = JsonBeanEncoder.create().writeObject(date).toJson();

        then:
        json == '"1983-12-17T20:07:12Z"'
    }

    void "Character Fallback Test"() {
        setup: "Create data"
        Character c = '#';

        when:
        String json = JsonBeanEncoder.create().writeObject(c).toJson();

        then:
        json == '"#"'
    }

    void "ObjectName Fallback Test"() {
        setup: "Create data"
        ObjectName data = new ObjectName("org.test:type=data,name=item");

        when:
        String json = JsonBeanEncoder.create().writeObject(data).toJson();

        then:
        json == '"org.test:name=item,type=data"'
    }

    void "Basic List Test"() {
        setup: "Create data"
        List<List<Integer>> list = new ArrayList();
        List<Integer> l1 = new ArrayList();
        list.add(l1);
        l1.add(11);
        l1.add(12);
        l1.add(13);
        List<Integer> l2 = new ArrayList();
        list.add(l2);
        l2.add(21);
        l2.add(22);

        when:
        String json = JsonBeanEncoder.create().writeObject(list).toJson();

        then:
        json == '[[11,12,13],[21,22]]'
    }

    void "Basic Array Test"() {
        setup: "Create data"
        String[][] a = [
            ["11", "12", "13"],
            ["21", "22"]
        ];

        when:
        String json = JsonBeanEncoder.create().writeObject(a).toJson();

        then:
        json == '[["11","12","13"],["21","22"]]'
    }

    void "Primitives Array Test"() {
        setup: "Create data"
        int[] a = [1, 2, 3, 4, 5, 6];

        when:
        String json = JsonBeanEncoder.create().writeObject(a).toJson();

        then:
        json == '[1,2,3,4,5,6]'
    }

    void "Basic Map Test"() {
        setup: "Create data"
        Map data = new HashMap();
        data.put("entry1", "42");
        data.put("entry2", "yes");

        when:
        String json = JsonBeanEncoder.create().writeObject(data).toJson();

        then:
        json == '{"entry1":"42","entry2":"yes"}'
    }

    public class Person {

        private String name;
        private int age;
        private PhoneNumber[] numbers;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public int getAge() {
            return age;
        }
        public void setAge(int age) {
            this.age = age;
        }
        public PhoneNumber[] getNumbers() {
            return numbers;
        }
        public void setNumbers(PhoneNumber[] numbers) {
            this.numbers = numbers;
        }
    }

    public class PhoneNumber {
        private String name;
        private String number;

        public PhoneNumber(String name, String number) {
            this.name = name;
            this.number = number;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getNumber() {
            return number;
        }
        public void setNumber(String number) {
            this.number = number;
        }
    }

    @InheritConstructors
    public class SpecialNumber extends PhoneNumber {
    }

    void "Bean Test"() {
        setup: "Prepare data"
        Person person = new Person();
        person.setName("Simon Sample");
        person.setAge(42);
        ArrayList numbers = new ArrayList();
        PhoneNumber[] phoneNumbers = [
            new PhoneNumber("Home", "06751 51 56 57"),
            new SpecialNumber("Work", "030 77 35 44")
        ];
        person.setNumbers(phoneNumbers);

        when:
        String json = JsonBeanEncoder.create().
                addAlias(SpecialNumber.class, "SpecialNumber").
                writeObject(person).toJson();

        then:
        json == '{"age":42,"name":"Simon Sample","numbers":[{"name":"Home","number":"06751 51 56 57"},{"class":"SpecialNumber","name":"Work","number":"030 77 35 44"}]}'
    }

    void "Bean Test 2"() {
        setup: "Prepare data"
        TestJavaBean bean = new TestJavaBean(42,
                new Date(Date.UTC(83, 11, 17, 20, 07, 12)),
                new ObjectName("org.test:type=data,name=item"));

        when:
        String json = JsonBeanEncoder.create().writeObject(bean).toJson();

        then:
        json == '{"dateProperty":"1983-12-17T20:07:12Z","intProperty":42,"objectNameProperty":"org.test:name=item,type=data"}'
    }

    void "RPC Test"() {
        setup: "Prepare RPC"
        JsonRpc rpc = JsonRpc.create();
        rpc.setMethod("test");
        rpc.addParam("param1");
        rpc.addParam(42);

        when:
        String json = JsonBeanEncoder.create().
                writeObject(rpc).toJson();

        then:
        json == '{"method":"test","params":["param1",42],"jsonrpc":"2.0"}'
    }
}