package com.example.client;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;

@BusInterface(name="org.alljoyn.bus.samples.simple.SimpleInterface")
public interface SimpleInterface {
	@BusMethod
	String Ping(String inStr) throws BusException;
}
