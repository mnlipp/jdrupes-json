package org.jdrupes.json.test;

import org.jdrupes.json.JsonBeanDecoder
import org.jdrupes.json.JsonBeanEncoder
import org.jdrupes.json.JsonRpc
import org.jdrupes.json.JsonRpc.DefaultJsonRpc
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
	
	void "RPC Test"() {
		setup: "Prepare RPC"
		String json = '{"method":"test","params":["param1",42],"jsonrpc":"2.0"}'
		
		when:
		JsonRpc rpc = JsonBeanDecoder.create(json).readObject(DefaultJsonRpc.class)

		then:
		rpc.method() == "test"
		rpc.params().asString(0) == "param1"
		rpc.params().asInt(1) == 42
	}
}