package org.jdrupes.json.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdrupes.json.JsonBeanDecoder;
import org.jdrupes.json.JsonDecodeException;

import static org.junit.Assert.*;

import org.junit.Test;

public class DecodeBeanTests {

	public static class Person {

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

	public static class PhoneNumber {
		private String name;
		private String number;

		public PhoneNumber() {
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

	public static class SpecialNumber extends PhoneNumber {
	}

	@Test
	public void readBean() throws JsonDecodeException {
		String json = "{\"age\":42,\"name\":\"Simon Sample\",\""
				+ "numbers\":[{\"name\":\"Home\",\"number\":\"06751 51 56 57\"},"
				+ "{\"class\":\"SpecialNumber\",\"name\":\"Work\",\"number\":\"030 77 35 44\"}]}";
		Person result = JsonBeanDecoder.create(json)
				.addAlias(SpecialNumber.class, "SpecialNumber")
				.readObject(Person.class);
		assertNotEquals(null, result);
		assertEquals(result.age, 42);
		assertEquals(result.numbers.length, 2);
		assertTrue(result.numbers[0] instanceof PhoneNumber);
		assertTrue(result.numbers[1] instanceof SpecialNumber);
		assertEquals("Home", result.numbers[0].name);
	}

	@Test
	public void readAsMap() throws JsonDecodeException {
		String json = "{\"age\":42,\"name\":\"Simon Sample\",\""
				+ "numbers\":[{\"name\":\"Home\",\"number\":\"06751 51 56 57\"},"
				+ "{\"class\":\"SpecialNumber\",\"name\":\"Work\",\"number\":\"030 77 35 44\"}]}";
		@SuppressWarnings("unchecked")
		Map<String,?> result = JsonBeanDecoder.create(json)
				.addAlias(SpecialNumber.class, "SpecialNumber")
				.readObject(HashMap.class);
		assertNotEquals(null, result);
		assertEquals(42L, result.get("age"));
		assertEquals(((List<?>)result.get("numbers")).size(), 2);
		assertTrue(((List<?>)result.get("numbers")).get(0) instanceof Map);
		assertTrue(((List<?>)result.get("numbers")).get(1) instanceof SpecialNumber);
	}

}
