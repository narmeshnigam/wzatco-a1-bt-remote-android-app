package com.narmeshnigam.a1remote.service

import com.narmeshnigam.a1remote.service.RegistrationPolicy.AfterReturn
import com.narmeshnigam.a1remote.service.RegistrationPolicy.Next
import org.junit.Assert.assertEquals
import org.junit.Test

class RegistrationPolicyTest {
    @Test
    fun `without a proxy the service must acquire one`() {
        assertEquals(Next.ACQUIRE_PROXY, RegistrationPolicy.next(proxyHeld = false, appRegistered = false))
        assertEquals(Next.ACQUIRE_PROXY, RegistrationPolicy.next(proxyHeld = false, appRegistered = true))
    }

    @Test
    fun `with a proxy and no registration the service registers`() {
        assertEquals(Next.REGISTER, RegistrationPolicy.next(proxyHeld = true, appRegistered = false))
    }

    @Test
    fun `an already registered app is never re-registered`() {
        assertEquals(Next.ALREADY_REGISTERED, RegistrationPolicy.next(proxyHeld = true, appRegistered = true))
    }

    @Test
    fun `a true return waits for the callback`() {
        assertEquals(
            AfterReturn.WAIT_FOR_CALLBACK,
            RegistrationPolicy.afterReturn(returned = true, appRegistered = false, attempt = 1),
        )
    }

    @Test
    fun `a false return while registered keeps the registration`() {
        for (attempt in 1..RegistrationPolicy.MAX_ATTEMPTS) {
            assertEquals(
                AfterReturn.KEEP_REGISTERED,
                RegistrationPolicy.afterReturn(returned = false, appRegistered = true, attempt = attempt),
            )
        }
    }

    @Test
    fun `a false return while unregistered retries until the last attempt`() {
        for (attempt in 1 until RegistrationPolicy.MAX_ATTEMPTS) {
            assertEquals(
                "attempt $attempt",
                AfterReturn.RETRY_LATER,
                RegistrationPolicy.afterReturn(returned = false, appRegistered = false, attempt = attempt),
            )
        }
        assertEquals(
            AfterReturn.REFUSED,
            RegistrationPolicy.afterReturn(
                returned = false,
                appRegistered = false,
                attempt = RegistrationPolicy.MAX_ATTEMPTS,
            ),
        )
    }
}
