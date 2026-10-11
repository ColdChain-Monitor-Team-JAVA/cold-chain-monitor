package com.coldchain.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.coldchain.web.MfaService.Result;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class MfaServiceTest {
    @Test void correctCodeAfterOneMistake() {
        MfaService m = new MfaService("123456", 300, 3); MockHttpSession s = new MockHttpSession(); m.start(s);
        assertEquals(Result.WRONG, m.verify(s, "000000"));
        assertEquals(Result.OK, m.verify(s, "123456"));
    }
    @Test void locksAfterMaxAttempts() {
        MfaService m = new MfaService("123456", 300, 3); MockHttpSession s = new MockHttpSession(); m.start(s);
        assertEquals(Result.WRONG, m.verify(s, "1")); assertEquals(Result.WRONG, m.verify(s, "2"));
        assertEquals(Result.LOCKED, m.verify(s, "3"));
        assertEquals(Result.LOCKED, m.verify(s, "123456"));
    }
    @Test void expiredCodeIsRejected() {
        MfaService m = new MfaService("123456", -1, 3); MockHttpSession s = new MockHttpSession(); m.start(s);
        assertEquals(Result.EXPIRED, m.verify(s, "123456"));
    }
}
