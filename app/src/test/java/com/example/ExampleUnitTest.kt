package com.example

import com.example.util.JalaliCalendar
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testJalaliDateRollovers() {
    // 31-day month rollover: 1403/01/31 -> 1403/02/01
    val (nextAfter31Farvardin, _) = JalaliCalendar.nextDay("1403/01/31")
    assertEquals("1403/02/01", nextAfter31Farvardin)

    // 31-day month end: 1403/06/31 -> 1403/07/01
    val (nextAfterShahrivar, _) = JalaliCalendar.nextDay("1403/06/31")
    assertEquals("1403/07/01", nextAfterShahrivar)

    // 30-day month rollover: 1403/07/30 -> 1403/08/01 (never 31)
    val (nextAfterMehr, _) = JalaliCalendar.nextDay("1403/07/30")
    assertEquals("1403/08/01", nextAfterMehr)

    // 30-day month rollover: 1403/11/30 -> 1403/12/01 (never 31)
    val (nextAfterBahman, _) = JalaliCalendar.nextDay("1403/11/30")
    assertEquals("1403/12/01", nextAfterBahman)

    // Year rollover: 1403 is leap or non-leap, test moving across year boundary
    val (nextYear, _) = JalaliCalendar.nextDay("1402/12/29")
    assertEquals("1403/01/01", nextYear)
  }
}
