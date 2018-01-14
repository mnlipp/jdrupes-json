package org.jdrupes.json.test;

import org.jdrupes.json.JsonBeanDecoder
import org.jdrupes.json.JsonBeanEncoder
import org.jdrupes.json.test.SimpleEncoderTests.PhoneNumber

import spock.lang.*

class SimpleDecoderTests extends Specification {

	void "Empty Object Test"() {
		when:
		String json = '{}';
		Map result = JsonBeanDecoder.create(json).readObject();
		
		then:
		result.size() == 0
	}


	void "Basic Array Test"() {		
		when:
		String json = '[[11,12,13],[21,22]]';
		int[][] result = JsonBeanDecoder.create(json).readArray(int[][]);
		
		then:
		result.length == 2
		result[0].length == 3
		result[1].length == 2
		result[0][0] == 11
	}

	enum State { ON, OFF };
		
	void "Enum Test"() {
		when:
		State[] data = [ State.ON, State.OFF ];
		String json = JsonBeanEncoder.create().writeObject(data).toJson();

		then:
		json == '["ON","OFF"]';
		
		when:
		State[] result = JsonBeanDecoder.create(json).readArray(State[]);

		then:
		result[0] == State.ON;
		result[1] == State.OFF;	
	}
}