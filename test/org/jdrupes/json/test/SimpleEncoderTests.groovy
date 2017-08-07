package org.jdrupes.json.test;

import org.jdrupes.json.JsonEncoder

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import spock.lang.*

class SimpleEncoderTests extends Specification {

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
	
	void "Basic Test"() {
		setup: "Prepare data"
		Person person = new Person();
		person.setName("Simon Sample");
		person.setAge(42);
		ArrayList numbers = new ArrayList();
		PhoneNumber[] phoneNumbers = [new PhoneNumber("Home", "06751 51 56 57"),
			new SpecialNumber("Work", "030 77 35 44")];
		person.setNumbers(phoneNumbers);
		
		when:
		String json = JsonEncoder.create().writeObject(person).toJson();
		
		then:
		json == '{"age":42,"name":"Simon Sample","numbers":[{"name":"Home","number":"06751 51 56 57"},{"class":"org.jdrupes.json.test.SimpleEncoderTests$SpecialNumber","name":"Work","number":"030 77 35 44"}]}'
	}
}