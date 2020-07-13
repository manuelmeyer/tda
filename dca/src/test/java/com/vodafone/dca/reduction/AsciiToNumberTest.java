package com.vodafone.dca.reduction;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AsciiToNumberTest {
	
	@Test
	public void canTransformAsciiToNumber() {
		assertEquals(12345L, AsciiToNumber.atol("12345"));
		assertEquals(1590069299000L, AsciiToNumber.atol("1590069299000"));
		assertEquals(1590069299L, AsciiToNumber.atotime("1590069299000"));
	}
}
